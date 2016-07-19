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

def return_template_as_text():
    return \
b"""def validateInterpretation(field_profile):
    return True

field_profile = locals()['field_profile']
is_valid_interpretation = validateInterpretation(field_profile)
"""

def return_template_encoded():
    return b64encode(return_template_as_text())
