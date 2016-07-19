'''
Created on Mar 29, 2016

@author: leegc
'''

from io import StringIO
from engine.script_executor import ScriptExecutor
from engine.objects import Result
import sys
import logging
import traceback
import time
import base64

'''
Object to handle running the user's Python script and return a boolean value.
'''

class InsecureScriptRunner(ScriptExecutor):

    def __init__(self):
        #self.script = self._format_script(script)
        pass

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
    
    def execute(self, script, metrics_dictionary):
        script = self._format_script(script)
        field_profile = metrics_dictionary
        # pass in a local copy of the metrics_dictionary (seen as 'field_profile' to the user)
        local_script_vars = locals()
        execution_time = '0'
        # run it
        result = False
        error = False
        message = ""
        try:
            exec(script, local_script_vars)
            if (bool(local_script_vars['is_valid_interpretation'])):
                result = True
            else: 
                result = False
        except AssertionError as err:
            logging.debug("User script failed an assertion.  Returning false.")
            result = False
            error = False
            message += "\nUser assertion failed in script."
        except SyntaxError as err:
            result = str(False)
            error = str(True)
            message += "\n" + str(err.__class__.__name__) + ": " + str(err.args[0]) + " on line " + str(err.lineno)
        except Exception as err:
            logging.debug("Invalid Python in user script.  Returning false.")
            logging.debug("Exception: " + str(sys.exc_info()[0]))
            result = str(False)
            error = str(True)
            message += "\n" + str(err.__class__.__name__) + ": " + str(err.args[0]) + " on line " +\
                     str(traceback.extract_tb(sys.exc_info()[2])[-1][1])
        
        overall_result = {'result': bool(result), 'message': message, 'error': bool(error)}
        return Result(overall_result)
    
    def _decode_base_64_(self, base_64_encoded):
        return base64.b64decode(base_64_encoded).decode('UTF-8')