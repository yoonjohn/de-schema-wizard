'''
Created on May 25, 2016

@author: leegc
'''

from engine.script_executor import ScriptExecutor
from engine.objects import Result
from engine.exceptions import ResultTimeout
from engine.exceptions import ScriptExecutorException
import logging
import redis
import os
import json
import uuid
import time

REDIS="REDIS_PORT"
REQUEST_CHANNEL="execution-requests"
RESPONSE_CHANNEL_METADATA_KEY="response-channel-id"
# assumes redis container is linked with alias "redis"

class Bridge(object):

    def __init__(self, redis, request_channel):
        self.r = redis
        self.p = redis.pubsub(ignore_subscribe_messages=True)
        self.request_channel = request_channel

    def request_execution(self, script, metrics_dictionary):
        data = {'iScript':script, 'metrics_dictionary':metrics_dictionary.user_facing_profile_mapping}
        self.receiving_channel_id = str(uuid.uuid1())
        data[RESPONSE_CHANNEL_METADATA_KEY] = self.receiving_channel_id
        self.p.subscribe(self.receiving_channel_id)
        self.r.publish(self.request_channel, json.dumps(data))

    def wait_for_response(self):
        t = time.time()
        message = self.p.get_message()
        while message == None:
            if time.time() - t > 20:
                m = self.p.get_message()
                if m is None:
                    logging.error("Execution request was never read by the sidekick.")
                else:
                    logging.error("Execution request read by sidekick, but no reponse found.")
                raise ResultTimeout()
            time.sleep(.1)
            message = self.p.get_message()
        result = json.loads(message['data'].decode("UTF-8"))
        return result
    
class SecureScriptRunner(ScriptExecutor):
    def __init__(self):
        self.bridge = Bridge(init_redis(), REQUEST_CHANNEL)
        self.internal_error = False
        
    def execute(self, script, metrics_dictionary):
        self.bridge.request_execution(script, metrics_dictionary)
        try:
            result = self.bridge.wait_for_response()
            return Result(result)
        except ResultTimeout as result_err:
            logging.error("Unexpected timeout while waiting for Sidekick.  Disabling script execution.")
            raise ScriptExecutorException from result_err

def init_redis(host='localhost', port=6379):
    try:
        redis_conn_string = os.environ[REDIS]
    except ValueError:
        redis_conn_string = "tcp://localhost:6379"
    redis_conn_string = redis_conn_string[6:]
    index = redis_conn_string.index(':')
    ip = redis_conn_string[:index]
    port = redis_conn_string[index+1:]
    return redis.StrictRedis(ip, port, 0)