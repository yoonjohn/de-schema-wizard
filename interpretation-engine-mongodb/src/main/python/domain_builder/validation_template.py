'''
Created on Mar 30, 2016

@author: leegc
'''

from base64 import b64encode

'''
Imports will be detected and dynamically relocated to the top of the script at validation time
'''

'''
Editable text area
This function should be implemented, it can be edited by the user, but deleting it will result in an invalid script
'''
#def validateInterpretation(field_profile):
#    return True
'''
end Editable text area
'''

'''
Generated method signature and call, the user cannot edit or type anything below this
This keyword argument variable will differ depending on the main type
These are example values for metrics calculations that will be passed to the script
e.g. The field is a number, decimal, so the following keys are added to the map (and visible but not editable to the user)
'''

#field_profile = locals()['field_profile']
#is_valid_interpretation = validateInterpretation(field_profile)

'''
The isValidInterpretation value can be evaluated from outside the script after the exec() call
'''


'''
# The default script (encoded in Mongo)
def validateInterpretation(field_profile):
    return True
field_profile = locals()['field_profile']
is_valid_interpretation = validateInterpretation(field_profile)
'''

'''
Interpretation Script - use this script to customize how the interpretation is validated
field_profile: a Python 'dictionary' of metrics that were gathered for the field being interpreted
field_profile['main_type'] = "number", "string", or "binary"
field_profile['detail_type'] = "integer", "decimal", "exponent", "phrase", "term", "boolean", "date/time", "image", "video", or "audio"
field_profile['example_values'] = set of example values gathered 
field_profile['number_min'] = the minimum of a numeric field
field_profile['number_max'] = the maximum of a numeric field
field_profile['number_average'] = the average of a numeric field
field_profile['number_std_dev'] = the standard deviation of a numeric field
field_profile['number_num_distinct'] = the number of distinct values of a numeric field
field_profile['string_min_length'] = the minimum length of a string field
field_profile['string_max_length'] = the maximum length of a string field
field_profile['string_average_length'] = the average length of a string field
field_profile['string_std_dev_length'] = the standard deviation of the length of a string field
field_profile['string_num_distinct'] = the number of distinct strings of a string fields
field_profile['binary_length'] = the length (in bytes) of a binary field
field_profile['binary_mime_type'] = the mime_type of a binary field
field_profile['binary_hash'] = the SHA256 hash of the binary field
field_profile['binary_entropy'] = the entropy of the byte distribution of a binary field
''' 
'''
'''

def return_template_as_text():
    return \
b"""def validateInterpretation(field_profile):
    return True

field_profile = locals()['field_profile']
is_valid_interpretation = validateInterpretation(field_profile)
"""

def return_template_encoded():
    return b64encode(return_template_as_text())
