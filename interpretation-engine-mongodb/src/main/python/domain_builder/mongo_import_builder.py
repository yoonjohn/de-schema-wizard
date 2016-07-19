'''
Created on Apr 29, 2016

@author: leegc
'''

import sys
import os
import uuid
import json
import yaml
import base64
import time
from domain_builder.validation_template import return_template_encoded

def get_domain_properties(domain_properties_file):
    d_properties = open(domain_properties_file, 'r')
    y_map = yaml.load(d_properties.read())
    d_properties.close()
    return y_map

def get_interpretation_properties(interpretation_properties_file):
    i_propreties = open(interpretation_properties_file, 'r')
    y_map = yaml.load(i_propreties.read())
    raw_type = [str(sample_data) for sample_data in y_map['iSampleData'] ]
    y_map['iSampleData'] = raw_type
    i_propreties.close()
    return y_map

def generate_mongo_imports(resource_base, domain_output, interpretation_output):
    for d_dir in os.listdir(resource_base):
        d_guid = str(uuid.uuid4())
        t1 = int(time.time()*1000)
        for d_file in os.listdir(os.path.join(resource_base, d_dir)):
            splits = d_file.split('#',2)
            if len(splits) != 2:
                if not os.path.basename(d_file) == str(os.path.basename(d_dir)+".yml"):
                    raise Exception("""Unexpected file """+d_file+""" found.  Domain properties must be in the form <domain-name>.yml and interpretation file names must be in the form <name>#<format>.yml""")   
                domain = {"dName":d_dir, "dId":d_guid, "dLastUpdate":t1}
                d_properties_file = os.path.join(resource_base, d_dir, d_file)
                d_properties = get_domain_properties(d_properties_file)
                for dp in d_properties:
                    domain[dp] = d_properties[dp]
                domain_output.write(json.dumps(domain)+"\n")
            else:
                i_guid = str(uuid.uuid4())
                i_name = splits[0]
                file_ext = splits[1].index('.')
                i_format = splits[1][:file_ext]
                interpretation = {"iId":i_guid, "iName":i_name, "iFormat":i_format, "iDomainId":d_guid}
                i_properties = get_interpretation_properties(os.path.join(resource_base, d_dir, d_file))
                for ip in i_properties:
                    interpretation[ip] = i_properties[ip]
                # encode the script 
                if 'iScript' in interpretation:
                    interpretation['iScript'] = base64.b64encode(interpretation['iScript'].encode()).decode()
                else:
                    interpretation['iScript'] = return_template_encoded().decode()
                # verify correct main type and detail type
                main_type = interpretation['iConstraints']['main-type']
                assert main_type == 'number' or main_type == 'string' or main_type == 'binary'
                detail_type = interpretation['iConstraints']['detail-type']
                assert detail_type == 'decimal' or detail_type == 'integer' or detail_type == 'exponent' \
                    or detail_type == 'term' or detail_type == 'phrase' or detail_type == 'boolean' or detail_type == 'date-time' \
                    or detail_type == 'audio' or detail_type == 'video' or detail_type == 'image'
                interpretation_output.write(json.dumps(interpretation)+"\n")
                

if __name__ == '__main__':
    if len(sys.argv) == 4:
        resource_base = sys.argv[1]
        domain_imports_name = sys.argv[2]
        interpretation_imports_name = sys.argv[3]
    else:
        print('Should specify a resource directory, domain file path, and interpretation file path using arguments.')
        print('Using relative defaults.')
        resource_base = os.path.abspath(os.path.join(".", os.pardir, os.pardir, "resources", "domains"))
        domain_imports_name = "domain_imports.json"
        interpretation_imports_name = "interpretation_imports.json"
    domain_imports_file = open(domain_imports_name, 'w')
    interpretation_imports_file = open(interpretation_imports_name, 'w')
    print("Scanning " + os.path.abspath(resource_base) + " for domains.")
    print("Using " + os.path.abspath(domain_imports_name) + " and " + os.path.abspath(interpretation_imports_name) + ".")
    generate_mongo_imports(resource_base, domain_imports_file, interpretation_imports_file)
    domain_imports_file.close()
    interpretation_imports_file.close()
    print('Resources closed.')