'''
Created on Mar 30, 2016

@author: leegc
'''
import logging
import json

'''
Script result object to help standard usage across modules
'''
class Result(object):
    result_key = 'result'
    error_key = 'error'
    message_key = 'message'
    
    def __init__(self, result_json=None):
        if result_json is None:
            self.result = False
            self.error = False
            self.message = ""
        else:
            self.result = result_json[self.result_key]
            self.error = result_json[self.error_key]
            self.message = result_json[self.message_key]
        
    def __str__(self):
        return "Return value " + str(self.result) + " with error value " + str(self.error) + " and message \'" + self.message + "\"."
    
    def to_schema_wizard_object(self):
        if self.result:
            string_result = 'true'
        else:
            string_result = 'false'
        obj = {'script-trace':self.message, 'script-result':string_result} 
        return obj

'''
Profile object that also contains the name for convenience
'''
class Profile(object):
    def __init__(self, name, profile):
        self.name = name
        self.profile = profile
    
    def __getitem__(self, attr):
        return self.profile[attr]
    
    def convert_to_metrics_dictionary(self):
        return MetricsDictionary(self.profile)

'''
Dictionary contianing all properties that the user will be able to interact with.
Will be called 'field_profile' in the customizable script.
'''
class MetricsDictionary(object):
    def __init__(self, profile):
        self.user_facing_profile_mapping = {}
        self.user_facing_profile_mapping['main_type'] = profile['main-type']
        self.user_facing_profile_mapping['detail_type'] = profile['detail']['detail-type']
        self.user_facing_profile_mapping['example_values'] = profile['exampleValues']
        logging.debug(str(len(self.user_facing_profile_mapping['example_values'])) + " example values received.")
        if self.user_facing_profile_mapping['main_type'] == 'number':
            self.user_facing_profile_mapping['number_min'] =  profile['detail']['min']
            self.user_facing_profile_mapping['number_max'] = profile['detail']['max']
            self.user_facing_profile_mapping['number_average'] = profile['detail']['average']
            self.user_facing_profile_mapping['number_std_dev'] = profile['detail']['std-dev']
            num_distinct = str(profile['detail']['num-distinct-values'])
            if(num_distinct.startswith(">=")):
                num_distinct = num_distinct[2:]
            logging.info("num distinct " + str(num_distinct))
            self.user_facing_profile_mapping['num_distinct_values'] = num_distinct 
        elif self.user_facing_profile_mapping['main_type'] == 'string':
            self.user_facing_profile_mapping['string_min_length'] =  profile['detail']['min-length']
            self.user_facing_profile_mapping['string_max_length'] = profile['detail']['max-length']
            self.user_facing_profile_mapping['string_average_length'] = profile['detail']['average-length']
            self.user_facing_profile_mapping['string_std_dev_length'] = profile['detail']['std-dev-length']
            num_distinct = str(profile['detail']['num-distinct-values'])
            if(num_distinct.startswith(">=")):
                num_distinct = num_distinct[2:]
            self.user_facing_profile_mapping['num_distinct_values'] = num_distinct
        elif self.user_facing_profile_mapping['main_type'] == 'binary':
            self.user_facing_profile_mapping['binary_length'] = profile['detail']['length']
            self.user_facing_profile_mapping['binary_mime_type'] = profile['detail']['detail-type']
            self.user_facing_profile_mapping['binary_hash'] = profile['detail']['hash']
            self.user_facing_profile_mapping['binary_entropy'] = profile['detail']['entropy']
        else :
            print("Not a number, string, or binary.")
            
    def __getitem__(self, attr):
        if attr in self.user_facing_profile_mapping:
            return self.user_facing_profile_mapping[attr]
        else:
            return None
        
    def __contains__(self, attr):
        return attr in self.user_facing_profile_mapping
        
    def example_values(self):
        return self.user_facing_profile_mapping['example_values']
      
    def main_type(self):
        return self.user_facing_profile_mapping['main_type']
    
    def detail_type(self):
        return self.user_facing_profile_mapping['detail_type']
    
    def number_min(self):
        return self.user_facing_profile_mapping['number_min']
    
    def number_max(self):
        return self.user_facing_profile_mapping['number_max']
    
    def number_average(self):
        return self.user_facing_profile_mapping['number_average']
    
    def number_std_dev(self):
        return self.user_facing_profile_mapping['number_std_dev']
    
    def number_num_distinct(self):
        return self.user_facing_profile_mapping['number_num_distinct']
    
    def string_min_length(self):
        return self.user_facing_profile_mapping['string_min_length']
    
    def string_max_length(self):
        return self.user_facing_profile_mapping['string_max_length']
    
    def string_average_length(self):
        return self.user_facing_profile_mapping['string_average_length']
    
    def string_std_dev_length(self):
        self.user_facing_profile_mapping['string_std_dev_length']
        
    def string_num_distinct(self):
        self.user_facing_profile_mapping['string_num_distinct']
    
    def binary_length(self):    
        self.user_facing_profile_mapping['binary_length']
        
    def binary_mime_type(self):
        self.user_facing_profile_mapping['binary_mime_type']
        
    def binary_hash(self):
        self.user_facing_profile_mapping['binary_hash']
        
    def binary_entropy(self):
        self.user_facing_profile_mapping['binary_entropy']
    