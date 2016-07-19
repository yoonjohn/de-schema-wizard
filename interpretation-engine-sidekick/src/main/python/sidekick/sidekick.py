from docker import Client
import sidekick.checks as checks
from sidekick.checks import error_messages
import os
import redis
import sys
import time
import logging
from sidekick.manage import ContainerTracker

REDIS="REDIS_PORT"

root = logging.getLogger()
root.setLevel(logging.INFO)

ch = logging.StreamHandler(sys.stdout)
ch.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
ch.setFormatter(formatter)
root.addHandler(ch)

class Sidekick(object):
    run = True

    def __init__(self):
        try:
            self.cli = init_docker()
            self.r = init_redis()
            self.p = self.r.pubsub()
            self.p.subscribe('execution-requests')
            self.container_metadata_mapping = {}
            self.container_tracker = ContainerTracker()
        except OSError as os_err:
            logging.exception(os_err)
            logging.error("Environment not correctly initialized.")
            self.run = False
        except Exception as err:
            logging.exception(err)
            logging.error("Error initializing sidekick.  Interpretation Engine will not be able to run user scripts. \
                Make sure you have mounted /var/run/docker.sock and are linked to a Redis container on port 6379. \
                Also check that the Docker version matches the docker-py version.")
            self.run = False

    def test_docker(self):
        logging.info("Starting container.")
        self.start_untrusted()

    def test_redis(self):
        logging.info("Setting foo->bar")
        logging.info(self.r.set('foo','bar'))

    def join(self):
        if self.run:
            logging.info("Starting sidekick.")
        while self.run:
            containers = self.cli.containers(filters={'label':'untrusted'}, all=True)
            for container in containers:
                details = self.cli.inspect_container(container['Id'])
                status = details['State']['Status']
                logging.debug("Container " + container['Id'] + " is " + status + ".")
                if status == 'exited':
                    container_exit_code = details['State']['ExitCode']
                    logging.info("Container " +container['Id'] + " exit code: " + str(container_exit_code) + ".")
                    try:
                        metadata = self.container_metadata_mapping[container['Id']]
                        if metadata.early_termination_code == checks.UNKNOWN or metadata.early_termination_code == checks.OK:
                            if not int(container_exit_code) == 0:
                                metadata.early_termination_code = checks.WARNING
                        data = self.container_tracker.handle_results(self.cli, self.r, container['Id'], metadata.early_termination_code)
                        self.container_tracker.send_result_to_ie(self.r, metadata, data)
                    except KeyError:
                        logging.error("Untrusted container was not properly started by the sidekick.")
                    except ValueError:
                        logging.error("Undefined exit code from Docker.")
                    self.remove_container(container['Id'])
                elif status == 'running':
                    self.investigate(details)
                elif status == 'created':
                    pass
                else:
                    logging.info("Untrusted container " + container['Id'] 
                                 + " is in unexpected state \"" + str(status) + "\".  Shutting down.")
                    self.remove_container(container['Id'])

            request = self.container_tracker.handle_requests(self.cli, self.r, self.p, self.data_volume())
            if request is not None:
                self.container_metadata_mapping[request[0]] = request[1]
            time.sleep(.1)
        else:
            logging.info("Sidekick shutting down.")
            
    def remove_container(self, container_id):
        self.cli.stop(container=container_id)
        self.cli.remove_container(container=container_id, v=True)
        logging.info("Removed container: " + container_id + ".")
        
    def investigate(self, details):
        for check in self.container_metadata_mapping[details['Id']].status_checkers:
            code = check.check_untrusted(self.cli, details)
            if code > checks.OK:
                logging.info("Container " + details['Id'] + ": " + error_messages[code])
                self.container_metadata_mapping[details['Id']].early_termination_code = code
                if code >= checks.IMMEDIATE_SHUTDOWN:
                    self.cli.kill(container=details['Id'])

    def data_volume(self):
        return self.cli.containers(filters={'name':'shared-volume'},all=True)[0]

    def generate_unique_user(self):
        pass


def init_docker(socket='unix://var/run/docker.sock'):
    client = Client(socket)
    client.ping()
    logging.info("Connected to Docker.")
    return client


def init_redis(host='localhost', port=6379):
    try:
        redis_conn_string = os.environ[REDIS]
    except ValueError:
        redis_conn_string = "tcp://localhost:6379"
    redis_conn_string = redis_conn_string[6:]
    index = redis_conn_string.index(':')
    ip = redis_conn_string[:index]
    port = redis_conn_string[index+1:]
    r = redis.StrictRedis(ip, port, 0)
    r.ping()
    logging.info("Connected to Redis.")
    return r

if __name__ == '__main__':
    sidekick = Sidekick()
    sidekick.join()
