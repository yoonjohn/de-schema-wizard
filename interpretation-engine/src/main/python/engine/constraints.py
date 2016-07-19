'''
Created on Apr 5, 2016

@author: leegc
'''

from re import match
from engine.objects import Result
import logging

main_type = "main_type"
detail_type = "detail_type"

number_min = "number_min"
number_max = "number_max"
number_average = "number_average"
number_std_dev = "number_std_dev"

string_min_len = "string_min_length"
string_max_len = "string_max_length"
string_average_len = "string_average_length"
string_std_dev_len = "string_std_dev_length"

binary_len = "binary_len"
binary_mime_type = "binary_mime_type"
binary_hash = "binary_hash"
binary_entropy = "binary_entropy"

_special_constraint_key = None
num_distinct_values = "num_distinct_values"

quantized = "quantized"
categorical = "categorical"
relational = "relational"
ordinal = "ordinal"

_constraint_keys_to_metrics_dictionary_key_mapping = \
    {
    "main-type":main_type,
    "detail-type":detail_type,
     
    "min":number_min, 
    "max":number_max, 
    "average":number_average, 
    "std-dev":number_std_dev, 
    "num-distinct-values":num_distinct_values,
    
    "min-length":string_min_len, 
    "max-length":string_max_len, 
    "average-length":string_average_len, 
    "std-dev-length":string_std_dev_len, 
    
    "length":binary_len, 
    "mime-type":binary_mime_type, 
    "hash":binary_hash, 
    "entropy":binary_entropy,
    
    "regex":_special_constraint_key,
    
    "quantized":quantized,
    "categorical":categorical,
    "relational":relational,
    "ordinal":ordinal
    }
    
'''
Validate constraints based on the mapping in Mongo.
'''

class ConstraintValidator(object):
    
    def validate_constraints(self, interpretation_name, field_name, constraints_dictionary, metrics_dictionary):
        # note: because this fails at the first violation and
        # generators do not have order, logging results may not be consistent
        result = Result()
        result.result = False
               
        if not constraints_dictionary['main-type'] == metrics_dictionary[_constraint_keys_to_metrics_dictionary_key_mapping['main-type']]:
            message ="Type mismatch for interpretation " + interpretation_name + " and field \'" + field_name + "\'."
            logging.debug(message)
            result.message = message
            return result
         
        for c in self._constraint_generator(constraints_dictionary):
            if c == None:
                logging.warn("Unknown constraint found with key " +c.constraint_key+" and value "+c.constraint_value +".  Ignoring.")
                continue
            
            metrics_key = _constraint_keys_to_metrics_dictionary_key_mapping[c.constraint_key]
            test_value = None
            constraint_result = True
            if metrics_key not in metrics_dictionary:
                # metrics do not contain the appropriate test value; continue
                if not metrics_key == _special_constraint_key:
                    logging.warning("Unexpected metrics key: " + metrics_key + ".")
                    logging.warning("Type " + metrics_dictionary['main_type'] + ".")
                    #logging.warning("Metrics-dict: " + metrics_dictionary.user_facing_profile_mapping)
                    continue
            else:
                test_value = metrics_dictionary[metrics_key]
                logging.info("Validating " + str(test_value) + " for " + str(c.constraint_value))
                constraint_result = c.validate(test_value)
                
            if not constraint_result: 
                # constraint violated
                message = "\'"+interpretation_name + "\' constraint violated for field "+field_name+" - " +c.__class__.__name__ +" value "+str(test_value) +" violated "+str(c.constraint_value)
                logging.debug(message)
                result.message = message
                return result
            if 'example_values' in metrics_dictionary and c.constrain_example_values:
                # loop through and validate example values using the appropriate constraint 
                for example_value in metrics_dictionary['example_values']:
                    if metrics_dictionary['main_type'] == 'string':
                        if c.constraint_key == 'min-length':
                            example_value = len(example_value)
                        elif c.constraint_key == 'max-length':
                            example_value = len(example_value)
                    constraint_result = c.validate(example_value)
                    if not constraint_result: 
                        message = "Constraint violated - " +c.__class__.__name__ +" value "+str(example_value) +" violated "+str(c.constraint_value)
                        logging.debug(message)
                        result.message = message
                        return result
                else:
                    # if all successful, output number of validated example values
                    logging.info("Validated " + str(len(metrics_dictionary['example_values'])) + " example values for field " + field_name + " with "+ c.__class__.__name__ +" for "+interpretation_name+".")
            else:
                if 'example_values' not in metrics_dictionary:
                    logging.warn("No example values passed to interpretation engine for field " + field_name)
        result.result = True
        return result
    
    def _constraint_generator(self, constraints_dictionary):
        for constraint_key in constraints_dictionary:
            if constraint_key not in constraints_dictionary or constraints_dictionary[constraint_key] is None:
                continue
            yield self._instantiate_appropriate_constraint(constraint_key, constraints_dictionary[constraint_key])
    
    def _instantiate_appropriate_constraint(self, constraint_key, constraint_value):
        constraint = Constraint(constraint_key, constraint_value)
        if constraint_key == 'main-type' or constraint_key == 'detail-type':
            constraint = EqualityConstraint(constraint_key, constraint_value)
            constraint.constrain_example_values = False
        elif constraint_key == 'min' or constraint_key == 'min-length':
            constraint = MinimumConstraint(constraint_key, constraint_value)
        elif constraint_key == 'max' or constraint_key == 'max-length':
            constraint = MaximumConstraint(constraint_key, constraint_value)
        elif constraint_key == 'num-distinct-values':
            constraint = MaximumConstraint(constraint_key, constraint_value)
            constraint.constrain_example_values = False
        elif constraint_key == 'categorical' or constraint_key == 'relational' \
            or constraint_key == 'relational' or constraint_key == 'ordinal':
            constraint = VizWizConstraint(constraint_key, constraint_value)
        elif constraint_key == 'regex':
            constraint = RegexConstraint(constraint_key, constraint_value)
        return constraint

'''
Constraint classes that will validate based on an constraint_key (a name), constraint value (the value that the constraint will use to validate),
and a function that is passed a test value.
'''

class Constraint(object):
    constrain_example_values = True
    
    def __init__(self, constraint_key, constraint_value):
        self.constraint_key = constraint_key
        self.constraint_value = constraint_value

    def validate(self, test_value):
        return True

class MinimumConstraint(Constraint):
    def __init__(self, constraint_key, constraint_value):
        self.constraint_key = constraint_key
        self.constraint_value = float(constraint_value)
    
    def validate(self, test_value):
        return float(test_value) >= self.constraint_value
    
class MaximumConstraint(Constraint):
    def __init__(self, constraint_key, constraint_value):
        self.constraint_key = constraint_key
        self.constraint_value = float(constraint_value)
        
    def validate(self, test_value):
        return float(test_value) <= self.constraint_value
    
class EqualityConstraint(Constraint):
    def validate(self, test_value):
        return self.constraint_value == test_value
    
class RegexConstraint(Constraint):
    def validate(self, test_value):
        return match(self.constraint_value, str(test_value))
    
class VizWizConstraint(Constraint):
    def validate(self, test_value):
        pass


