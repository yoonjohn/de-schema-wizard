'''
Created on May 31, 2016

@author: leegc
'''

import logging
import time

# code == 0 -> normal execution
OK = 0

# code == 1 -> internal error
INTERNAL_ERROR = 1

# code > 10 and code < 20 -> warning/on alert
ON_ALERT = 10
WARNING = 11
SUSPICIOUS = 13

# code > 20 -> shutdown immediately 
IMMEDIATE_SHUTDOWN = 20
EXECUTION_TOO_LONG = 23
MALICIOUS = 47
UNKNOWN = 51

error_messages = {
          OK: "Script executed normally.",
          INTERNAL_ERROR:"Internal error executing script.",
          WARNING:"Script did not behave as expected.",
          SUSPICIOUS:"This script behaved suspiciously.  It has been flagged.",
          EXECUTION_TOO_LONG:"Code exceeded execution time limit.",
          MALICIOUS:"Malicious code execution suspected.",
          UNKNOWN:"The status of the script could not be determined."
          }

def get_checks():
    checks = [RunningTimeCheck()]
    return checks

class Check(object):
    def check_untrusted(self, docker_cli, docker_inspection):
        pass
    
class RunningTimeCheck(Check):
    limit = 10
    
    def check_untrusted(self, docker_cli, docker_inspection):
        start_time_str = docker_inspection['State']['StartedAt'] 
        start_time_str = start_time_str[:start_time_str.index(".")]
        start_time_seconds = time.mktime(time.strptime(start_time_str, "%Y-%m-%dT%H:%M:%S"))
        dif = time.time() - start_time_seconds
        if dif > self.limit:
            return EXECUTION_TOO_LONG
        else:
            return OK
        
class RunningProcessesCheck(Check):
    
    def check_untrusted(self, docker_cli, docker_inspection):
        Check.check_untrusted(self, docker_cli, docker_inspection)

class FileDifferenceCheck(Check):
    
    def check_untrusted(self, docker_cli, docker_inspection):
        pass
    
class OutputCheck(Check):
    
    def check_untrusted(self, docker_cli, docker_inspection):
        Check.check_untrusted(self, docker_cli, docker_inspection)
