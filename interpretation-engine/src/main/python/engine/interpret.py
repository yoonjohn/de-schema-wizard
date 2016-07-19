'''
Created on Mar 30, 2016

@author: leegc
'''
from engine.objects import Profile
from engine.objects import Result
from engine.profile import ProfileInterpreter
from engine.exceptions import ResultTimeout
from engine.exceptions import ScriptExecutorException
import logging
from engine.script_executor import ScriptExecutor

'''
The Interpretation Engine that encapsulates interpretation logic.
'''
class InterpretationEngine(object):
        
    def interpret(self, interpretation_list, profile_mapping):
        # maps a key to its interpretation
        logging.debug("Interpretting with: " + ",".join((i['iName'] for i in interpretation_list)))
        interpretation_mapping = {}
        for profile in self._profile_list_generator(profile_mapping):
            #self._load_interpretations_into_mapping(
            #    interpretation_mapping, ProfileInterpreter(interpretation_list, profile))
            interpretation_mapping[profile.name] = ProfileInterpreter(interpretation_list, profile).validate()
                
        return interpretation_mapping
    
    '''def interpret_one(self, script, profile_mapping):
        for profile in self._profile_list_generator(profile_mapping):
            return ScriptRunner().execute_with_output(script, profile.convert_to_metrics_dictionary())'''
   
    def test_interpret(self, interpretation, profile_mapping):
        result = Result()
        logging.debug("Testing " + interpretation['iName'] + ".")
        interpreter = ProfileInterpreter([interpretation], profile_mapping)
        try:
            result = interpreter.phase_1_validation(interpretation)
            if result.result:
                logging.info(interpretation['iName'] + " passed phase 1.")
                result = interpreter.phase_2_validation(interpretation)
                if result.result:
                    logging.info(interpretation['iName'] + " passed phase 2.")
        except ScriptExecutorException:
            logging.exception("Exception while executing.  Returning default results.")
            result.message = "There was an internal error while executing the script."
            result.result = False
            
        return result
    
     
    def interpret_one(self, script, profile_mapping):
        for profile in self._profile_list_generator(profile_mapping):
            return ScriptExecutor().execute_with_output(script, profile.convert_to_metrics_dictionary())
    
    def _profile_list_generator(self, profile_mapping):
        # generate a Profile object for convenience
        for key in profile_mapping:
            yield Profile(key, profile_mapping[key])
            
    #def _load_interpretations_into_mapping(self, interpretation_mapping, profile_interpreter):
        # put the validated interpretation into the interpretation_mapping
        #interpretation_mapping[profile_interpreter.profile.name] = [i for i in profile_interpreter.validate()]
                