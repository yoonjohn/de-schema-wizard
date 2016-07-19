'''
Created on May 31, 2016

@author: leegc
'''
import sidekick.checks as checks
from sidekick.message_handler import MessageHandler
import logging

class ActiveContainerMetadata(object):

    def __init__(self, response_channel, tmp_dir, status_checkers):
        self.response_channel = response_channel
        self.tmp_dir = tmp_dir
        self.early_termination_code = checks.OK
        self.status_checkers = status_checkers
        
class MessageGenerator(object):
    
    def __init__(self):
        self.message = ""
        self.message_handler = MessageHandler()
        
    def add_to_message(self, addition):
        self.message += "\n"
        self.message += addition
        
    def reset_with_new_message(self, new_message):
        self.message = new_message
        
    def report_raw_result(self, raw_exception, raw_line_no):
        tple = self.message_handler.handle_name_and_line(raw_exception, raw_line_no)
        if tple[0] is not None:
            if tple[1]:
                self.reset_with_new_message(tple[0])
            else:
                self.add_to_message(tple[0])
        
    def report_metadata_shutdown_code(self, metadata_shutdown_code):
        logging.info("Reporting shutdown code: " + str(metadata_shutdown_code) + ".")
        if metadata_shutdown_code in checks.error_messages:
            self.message += checks.error_messages[metadata_shutdown_code]
        else:
            self.message += checks.error_messages[checks.UNKNOWN]
        if metadata_shutdown_code > checks.IMMEDIATE_SHUTDOWN:
            logging.warning("Immediate shutdown performed: " + self.message)
            logging.warning("No alert implemented.")
        
    def get_message(self):
        return self.message