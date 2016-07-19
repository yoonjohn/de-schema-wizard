'''
Created on Mar 28, 2016

@author: leegc
'''

from pyjarowinkler.distance import get_jaro_distance
from engine.constraints import ConstraintValidator
from engine.script_executor import ScriptExecutor
from engine.objects import Result
import logging

STRUCTURED_KEY_SEPARATOR = "."

'''
The object that performs Phase 1-3 of the Interpretation Engine
'''
class ProfileInterpreter(object):

    def __init__(self, interpretation_list, field_profile):
        self.interpretation_list = interpretation_list
        self.profile = field_profile
        self.metrics_dictionary = self.profile.convert_to_metrics_dictionary()
        self.script_executor = ScriptExecutor()
    
    # Validate using the constraints defined in the interpretation object
    def phase_1_validation(self, interpretation):
        return ConstraintValidator().validate_constraints(interpretation['iName'], self.profile.name, interpretation["iConstraints"], self.metrics_dictionary)
    
    # Validate using the script that user has written
    def phase_2_validation(self, interpretation):
        result = self.script_executor.execute(interpretation['iScript'], self.metrics_dictionary)
        bool_result = result.result
        if not bool_result:
            logging.debug("Removing " + interpretation['iName'] + " from field " + self.profile.name + " because its script returned false.")
            return result
        logging.debug("Script valid for field " + interpretation['iName'] + ".")
        return result
    
    # Validate using Jaro-Winkler string matching algorithm
    def phase_3_validation(self, interpretation, min_confidence):    
        result = Result() 
        last_underscore_index = self.profile.name.rfind(STRUCTURED_KEY_SEPARATOR)  
        if last_underscore_index >= 0:
            normalized_name = self.profile.name[last_underscore_index:]
        else:
            normalized_name = self.profile.name
        max_confidence = 0.0
        for matching_name in interpretation['iMatchingNames']:
            if len(matching_name) > 0:
                jaroDistance =  get_jaro_distance(normalized_name.lower(), matching_name.lower())
                if jaroDistance > max_confidence:
                    max_confidence = jaroDistance
        interpretation['iConfidence'] = max_confidence
        if max_confidence <= min_confidence:
            message = "Matching confidence not high enough for field " + normalized_name + " with interpretation "+interpretation['iName']+"."
            logging.debug(message)
            result.message = message
            return result
        logging.debug("Highest matching confidence for " + normalized_name + " was " + str(max_confidence) + ".")
        result.result = True
        return result

    # Validate using Phases 1-3.  Edit the valid_interpretations list in place and return only validated entries. 
    def validate(self, min_confidence=.7):
        #log.info("Interpreting field",self.profile.name,".")
        logging.debug("Validating interpretatations for field " + self.profile.name + ".")
        valid_interpretations = [i for i in self.interpretation_list \
                                    if self.phase_1_validation(i).result \
                                    and self.phase_2_validation(i).result \
                                    and self.phase_3_validation(i, min_confidence).result]
        '''if len(valid_interpretations) > 1:
            valid_interpretations = [i for i in valid_interpretations if self.phase_3_validation(i, min_confidence).result]
        elif len(valid_interpretations) == 1:
            logging.debug("Only one interpretation possible after phases 1 and 2 for field " + self.profile.name + ".");
            valid_interpretations[0]['iConfidence'] = min_confidence'''
        valid_interpretations.sort(key=lambda x : x['iConfidence'], reverse=True)
        for valid_interpretation in valid_interpretations:
            logging.info("Field " + self.profile.name + " interpretted as " + str(valid_interpretation['iName']) +".")
        return valid_interpretations
