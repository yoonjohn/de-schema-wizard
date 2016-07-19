'''
Created on May 25, 2016

@author: leegc
'''

from engine.validation_template import return_template_encoded
from engine.objects import Result
from engine.exceptions import ScriptExecutorException
from engine.exceptions import ResultTimeout
import os
import platform
import logging
import time
import json

class ScriptExecutor(object):

    def __init__(self):
        dev_build = False
        if "DEV_BUILD" in os.environ:
            dev_build = True
        system = platform.system()
        if system == 'Linux':
            if dev_build:
                logging.warning("Development environment detected.  Initializing trusted script execution.")
                from dev.development_trusted_runner import InsecureScriptRunner
                self.script_executor = InsecureScriptRunner()
            else:
                logging.info("Initializing secure script runner.")
                from prod.linux_untrusted_runner import SecureScriptRunner
                try :
                    self.script_executor = SecureScriptRunner()
                except ScriptExecutorException:
                    logging.error("Could not instantiate secure script executor.  Script execution disabled.")
        elif system == 'Windows':
            logging.warning("Development environment detected.  Initializing trusted script execution.")
            from dev.development_trusted_runner import InsecureScriptRunner
            self.script_executor = InsecureScriptRunner()
        else:
            logging.error("System not found.  Validation has been disabled.")
        
    def execute(self, script, metrics_dictionary):
        if self.script_executor is None:
            logging.error("Script runner was not initialized correctly.  Execution disabled.")
            return None
        elif script == return_template_encoded().decode():
            logging.info("Default script detected.")
            default_result = Result()
            default_result.result = True
            default_result.message = "Default script returned true."
            return default_result
        else:
            try:
                start_time = int(round(time.time() * 1000))
                result = self.script_executor.execute(script, metrics_dictionary)
                finish_time = int(round(time.time() * 1000)) + 1
                execution_time = str(finish_time - start_time)
                result.message += "\nProgram executed in " + execution_time + " milliseconds."
                logging.debug("Got result " + json.dumps(result.to_schema_wizard_object()) + " from script executor in "+str(execution_time)+" milliseconds.")
                return result
            except ScriptExecutorException as se_exc:
                logging.error("Script Executor Exception caught.  Should indicate a configuration error with the sidekick.")
                logging.exception(se_exc)
                result = Result()
                result.result = False
                result.error = True
                result.message = str("Unable to execute script.  This indicates that the environment is incorrectly configured.")
                return result
            except UnicodeDecodeError as ud_exc:
                logging.error("Unicode error caught.  Script should not have been validated.")
                logging.exception(ud_exc)
                result = Result()
                result.result = False
                result.error = True
                result.message = str("The script contains invalid character(s).")
                return result
            except Exception as exc:
                logging.exception(exc)
                result = Result()
                result.result = False
                result.error = True
                result.message = str("There was an internal error executing the script.")
                return result

    def _format_script(self, script):
        # script is in base64 in Mongo 
        # decode base64, and then decode to UTF-8 to become a string (rather than bytes-like)
        decoded_script = self._decode_base_64_(script)
        formatted_script = ""
        imports = []
        lines = decoded_script.splitlines(True)
        # find all the import lines
        for line in lines:
            if line.startswith("from") or line.startswith("import"):
                logging.warning("Import detected: " + line)
                imports.append(line)
            else:
                formatted_script = formatted_script + line
        
        # put the import lines at the top of the script
        for i in imports:
            formatted_script = i + decoded_script
        return formatted_script
    
    '''def validate(self, script):
        """Decodes a base64 script and validates it by compile
        
        parameter base64 String
        return diction with JSON Array of JSON Objects (discrepancies) and a true/false flag
        based on whether or not the validation was successful
        """
        file = '_interpretation_.py'
        with open(file,"w") as f:
            f.write(self._decode_base_64_(script))
        
        from subprocess import Popen, PIPE
        p = Popen(['flake8', file], stdin=PIPE, stdout=PIPE, stderr=PIPE)
        output, err = p.communicate()
        console_out = output.decode('UTF-8').strip()
        discrepancies_text = console_out.splitlines()
        
        discrepancies_list = []
        clean_list = []
        return_list = {}
        
        for discrepancy in discrepancies_text:
            discrepancy_components = discrepancy.split(':', 4)
            file_name = discrepancy_components[0]
            line_number = discrepancy_components[1]
            column_number = discrepancy_components[2]
            error_msg = discrepancy_components[3].strip()
            
            error_components = error_msg.split(' ', 1)
            error_code = error_components[0]
            annotation = error_components[1]
            
            if (error_code[0] == 'E'):
                if (error_code[1] == '1'):
                    discrepancy_type = 'error'
                elif (error_code[1] == '9'):
                    discrepancy_type = 'error'
                else:
                    discrepancy_type = 'warning'
            else:
                discrepancy_type = 'warning'
            
            discrepancies_list.append(self._create_discrepancy_(discrepancy_type, int(line_number), annotation))
        
        # Compile
        try:
            with open(file) as f:
                script = f.read()
            compile(script, '<script>', 'exec')
            # Only return non-error discrepancies
            for discrepancy in discrepancies_list:
                if(discrepancy['type'] != 'error'):
                    clean_list.append(discrepancy)
            return_list = {"discrepancies":json.dumps(clean_list), "isValid":True}
            return return_list
        except Exception as e:
            import re
            error_msg = str(e)
            error_line = error_msg.split('line ')
            line_num_pattern = re.compile(r'[^\d.]+')
            line_number = line_num_pattern.sub('', error_line[1])
            
            discrepancies_list.append(self._create_discrepancy_(discrepancy_type, int(line_number), annotation))
            clean_list = discrepancies_list
            return_list = {"discrepancies":json.dumps(clean_list), "isValid":False}
            return return_list
    
    def _create_discrepancy_(self, discrepancy_type, line_number, annotation):
        line_number -= 1
        return {'type': discrepancy_type, 'row': line_number, 'text': (annotation[0].upper() + annotation[1:])}
    
    def _decode_base_64_(self, base_64_encoded):
        return base64.b64decode(base_64_encoded).decode('UTF-8')'''