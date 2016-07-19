'''
Created on May 26, 2016

@author: leegc
'''

import json
import base64
import logging

class ScriptValidator(object):
    def validate(self, script):
        """Decodes a base64 script and validates it by compile
        
        parameter base64 String
        return diction with JSON Array of JSON Objects (discrepancies) and a true/false flag
        based on whether or not the validation was successful
        """
        
        try :
            file = '_interpretation_.py'
            with open(file,"w") as f:
                f.write(self._decode_base_64_(script))
            
            from subprocess import Popen, PIPE
            p = Popen(['flake8', file], stdin=PIPE, stdout=PIPE, stderr=PIPE)
            output, err = p.communicate()
            discrepancies_text = ""
            
            discrepancies_list = []
            clean_list = []
            return_list = {}
            
            console_out = output.decode('UTF-8').strip()
            discrepancies_text = console_out.splitlines()
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
            with open(file) as f:
                script = f.read()
            compile(script, '<script>', 'exec')
            # Only return non-error discrepancies
            for discrepancy in discrepancies_list:
                if(discrepancy['type'] != 'error'):
                    clean_list.append(discrepancy)
            return_list = {"discrepancies":json.dumps(clean_list), "isValid":True}
            return return_list
            
        except UnicodeDecodeError as exc:
            logging.warning("Decoding error encountered.")
            clean_list = [self._create_discrepancy_('error', 1, "Invalid character found.")]
            return_list = {"discrepancies":json.dumps(clean_list), "isValid":False}
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
        return base64.b64decode(base_64_encoded).decode('UTF-8')