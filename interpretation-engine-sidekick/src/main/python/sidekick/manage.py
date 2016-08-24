'''
Created on Jun 7, 2016

@author: leegc
'''
import sidekick.checks as checks
from sidekick.checks import error_messages
from sidekick.metadata import ActiveContainerMetadata
from sidekick.metadata import MessageGenerator
import shutil
import os
import logging
import json
import random
from tempfile import mkdtemp

DIV = "__***__"
PULL_TAG_KEY = "PULL_TAG"
UNTRUSTED_IMAGE="der.deleidos.com/digitaledge/schema-wizard/sw-ie-untrusted"
UNTRUSTED_IMAGE_KEY = "U_IMAGE"
UNTRUSTED_USER = 1111
UNTRUSTED_GROUP = 999
UNTRUSTED_PROFILE_KEY = "U_PROFILE"

class ContainerTracker(object):
    
    def __init__(self):
        self.tag = os.environ[PULL_TAG_KEY]
        if UNTRUSTED_IMAGE_KEY in os.environ:
            self.untrusted_image_name = os.environ[UNTRUSTED_IMAGE_KEY]
        else:
            self.untrusted_image_name = UNTRUSTED_IMAGE
        if UNTRUSTED_PROFILE_KEY not in os.environ:
            raise OSError("You must specify the AppArmor profile that untrusted code will use \
                with the \'U_PROFILE\' environment variable.  To use the default Docker profile,\
                 set this variable to \'docker-default\'.")
        self.untrusted_profile = os.environ[UNTRUSTED_PROFILE_KEY]
        logging.info("Using untrusted profile - " + self.untrusted_profile)
    
    def start_untrusted(self, docker_cli, label_list=['untrusted'], image_str='hello-world', volumes_from=[], environment={}, user=UNTRUSTED_USER, entrypoint=None, mem_limit="100m", security_opt=None):
        host_config = docker_cli.create_host_config(volumes_from=volumes_from, network_mode = 'none',
                                                     mem_limit=mem_limit, read_only=True, security_opt=security_opt)
        
        logging.info("Host config: " + str(host_config))
        logging.info("Starting " + image_str+ " as user " + str(user) + ".")
        user_with_group = str(user) + ":" + str(UNTRUSTED_GROUP)
        container = docker_cli.create_container(image=image_str, labels=label_list, host_config=host_config,
                                                 environment=environment, network_disabled=True, user=user_with_group)
        c_id = container.get('Id')
        logging.info("Starting "+image_str+" with id " + c_id + ".")
        docker_cli.start(container=c_id)
        return c_id
    
    def send_result_to_ie(self, redis, container_metadata, data):
        tmp_dir = container_metadata.tmp_dir
        response_channel = container_metadata.response_channel
        redis.publish(response_channel, json.dumps(data))
        logging.info("Sent " + str(data) + " to " + response_channel + ".")
        logging.info("Removing tree: " + tmp_dir + ".")
        shutil.rmtree(tmp_dir)
    
    def send_error_result_to_ie(self, redis, response_channel):
        data = {'result':False,'error':True,'message':checks.error_messages[checks.INTERNAL_ERROR]}
        redis.publish(response_channel, json.dumps(data))
        logging.info("Sent " + str(data) + " to " + response_channel + ".")
        
    def handle_results(self, docker_cli, redis_conn, container_id, shutdown_metadata_code):
        
        std_out = docker_cli.logs(container=container_id, stdout=True, tail=10).decode("utf-8")
        logging.info("Raw script logs: " + std_out)

        message_gen = MessageGenerator()
        message_gen.report_metadata_shutdown_code(shutdown_metadata_code)
        
        data = {'result':False,'error':False,'message':None }
        string_result = None
        lines = std_out.split('\n')
        is_next = False
        for line in reversed(lines):
            if line == DIV:
                is_next = True
            elif is_next:
                string_result = line
                break
        if string_result is None:
            if shutdown_metadata_code == checks.OK:
                message_gen.reset_with_new_message(checks.error_messages[checks.SUSPICIOUS])
            else:
                message_gen.add_to_message("No result detected.  Defaulted to false.")
            logging.warning(message_gen.get_message())
        elif len(string_result) > 1000:
            message_gen.add_to_message("Length of result is suspiciously long.  Defaulted to false.")
            logging.warning(message_gen.get_message())
            # mark as suspicious
        else:
            logging.info("Raw script results: " + string_result)
            try:
                raw_data = json.loads(string_result)
                
                message_gen.report_raw_result(raw_data['exception'], raw_data['line'])
                
                if raw_data['result'] == "True":
                    data['result'] = True
                elif not raw_data['result'] == "False":
                    message_gen.reset_with_new_message("Unexpected result - must return boolean (True or False).")
                    
                if raw_data['error'] == "True":
                    if data['result']:
                        message_gen.add_to_message("Unexpected result - 'true' result with a reported error.  Defaulted to false.")
                        data['result'] = False
                
                
            except Exception as exc:
                logging.warning(message_gen.get_message())
                logging.exception(exc)
                message_gen.reset_with_new_message("Result could not be determined.  Defaulted to false.")
        data['message'] = message_gen.get_message()
        logging.info("Processed script results: " + str(data) + ".")
        return data
    
    def handle_requests(self, docker_cli, redis, pubsub, data_volume):
        
            message = pubsub.get_message()
            if message is not None:
                if message['type'] == 'message':
                    # receive request from interpretation engine via Redis
                    data = message['data'].decode("utf-8")
                    j = json.loads(data)
                    response_channel = j['response-channel-id']
                    try:
                        j.pop('response-channel-id')
        
                        user_id = random.randint(1001, 5000)
                        tmp_dir = mkdtemp(dir="/usr/local/shared/untrusted/")
                        tmp = open(os.path.join(tmp_dir, "metrics-dictionary.json"), 'w')
                        tmp.write(json.dumps(j))
                        tmp.close()
                        logging.info("Updating " + tmp_dir + " and " + tmp.name + ".")
                        os.chown(tmp_dir, user_id, UNTRUSTED_GROUP)
                        os.chown(tmp.name, user_id, UNTRUSTED_GROUP)
                        #out = check_output(["usermod", "-g", str(UNTRUSTED_GROUP), str(user_id)])
                        #logging.info(out)
                        os.chmod(tmp_dir, 0o700)
                        os.chmod(tmp.name, 0o700)
                        env = {'RESOURCE_PATH':tmp.name}
        
                        image_str=self.untrusted_image_name+":"+self.tag
                        if self.untrusted_profile is not None:
                            sec_opt = ["apparmor="+self.untrusted_profile]
                        else:
                            sec_opt = None
                        c_id = self.start_untrusted(docker_cli, volumes_from=[data_volume['Id']], image_str=image_str, environment=env, user=user_id, security_opt=sec_opt)
        
                        container_metadata = ActiveContainerMetadata(response_channel, tmp_dir, checks.get_checks()) 
                        logging.info("Response channel for " + c_id + " is " + response_channel + ".")
                        return c_id, container_metadata
                    except Exception as err:
                        logging.exception(err)
                        logging.error("Container did not successfully start.")
                        self.send_error_result_to_ie(redis, response_channel)
                        return None