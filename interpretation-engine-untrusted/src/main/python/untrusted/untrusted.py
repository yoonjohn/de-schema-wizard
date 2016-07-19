from json import dumps
from json import loads
from sys import exit
from sys import exc_info
from os import environ
from os import remove
from traceback import extract_tb
from base64 import b64decode

DIV = "__***__"
RESOURCE="RESOURCE_PATH"

def r_e(e,l):
    return str(e) + " on line " + str(l) + "."

if __name__ == '__main__':
    resource_file_path = environ[RESOURCE]

    f = open(resource_file_path, 'r')
    j = loads(f.read())
    f.close()
    remove(resource_file_path)

    script = j['iScript']
    script = b64decode(script).decode('UTF-8')

    field_profile = j['metrics_dictionary']
    is_valid_interpretation = False
    local_vars = locals()
    error = str(False)
    exception = None
    line = None

    try:
        exec(script, local_vars)
        result = str(local_vars['is_valid_interpretation'])
    except AssertionError:
        # assertion failed, invalid but return message
        result = str(False)
    except SyntaxError as err:
        result = str(False)
        error = str(True)
        line = err.lineno
        exception = err.__class__.__name__
    except Exception as err:
        # Exception while running script
        result = str(False)
        error = str(True)
        line = extract_tb(exc_info()[2])[-1][1]
        exception = err.__class__.__name__
        
    response = {'result':result, 'error':error, 'exception':exception, 'line':line}

    print(DIV)
    print(dumps(response))
    print(DIV)
    if error == str(False):
        exit(0)
    else:
        exit(1)
