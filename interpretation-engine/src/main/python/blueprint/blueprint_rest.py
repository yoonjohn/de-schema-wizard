'''
Created on Mar 31, 2016

@author: yoonj1
'''
from flask import Blueprint, request, current_app
from flask_api import status
from flask import Response
from blueprint import blueprint_core as bpc
from pymongo import MongoClient
from bson.json_util import dumps
from engine.objects import Profile
import json
import os
import logging
import sys

"""Logger initialization"""
logger = logging.getLogger()
logger.level = logging.INFO
stream_handler = logging.StreamHandler(sys.stdout)
logger.addHandler(stream_handler)
stream_handler.stream = sys.stdout

"""Connection variables"""
bp = Blueprint('blueprint', __name__)
TCP_ADDR = os.getenv('SW_MONGODB_PORT_27017_TCP_ADDR', 'localhost')
TCP_PORT = 27017
client = MongoClient(TCP_ADDR, TCP_PORT)
reverse_geo_db_name = "reverse_geo"
domain_db_name = "domain_manager"
        
@bp.route('/test', methods=['GET'])
def test():
    """Return an arbitrary string
    
    A test method
    """
    current_app.logger.info("Test call received.")
    output = bpc.test()
    current_app.logger.debug(output)
    return output

@bp.route('/', methods=['GET'])
def display_info():
    """Return all domains in the domain collection
    
    Default action for the root URL
    """
    current_app.logger.info("Test get_domain call received.")    
    output = bpc.display_info(_get_domain_collection())
    current_app.logger.debug(output)
    return output

@bp.route('/create/domain', methods=['POST'])
def create_domain():
    """Return the Object Id of the inserted object if successful
    Return "0" if the action was unsuccessful
    
    Creates a domain object in the domain collection
    """
    current_app.logger.info("Received create domain request.")
    if request.headers['Content-Type'] == 'application/json':
        current_app.logger.debug("Content type is application/json")
        domain_object = request.json
        current_app.logger.debug("domain_object: " , domain_object)
        output = bpc.create_domain(_get_domain_collection(), domain_object)
        current_app.logger.debug(output)
        
        
        if output == "-1":
            return output, status.HTTP_403_FORBIDDEN  
        else:
            return output, status.HTTP_201_CREATED
    
@bp.route('/create/interpretation', methods=['POST'])
def create_interpretation():
    """Return the Object Id of the inserted object
    Return "0" if the action was unsuccessful
    
    Creates an interpretation object in the interpretation collection
    """
    current_app.logger.info("Retrieving create interpretation request.")
    if request.headers['Content-Type'] == 'application/json':
        current_app.logger.debug("Content type is application/json")
        interpretation_object = request.json
        current_app.logger.debug("interpretation_object: " , interpretation_object)
        output = bpc.create_interpretation(_get_interpretation_collection(), interpretation_object)
        current_app.logger.debug(output)
        
        if output == "-1":
            return output, status.HTTP_403_FORBIDDEN
        else:
            return output, status.HTTP_201_CREATED
        
@bp.route('/domains', methods=['GET'])
def get_domains_catalog():
    """Return all domains from the domain collection
    
    Gets all of the domains
    """
    current_app.logger.info("Retrieving domain catalog.")
    output = bpc.get_domain_catalog(_get_domain_collection())
    current_app.logger.debug(output)
    
    return Response(output, status=200, mimetype='application/json')

@bp.route('/interpretations', methods=['GET'])
def get_interpretations_catalog():
    """Return all interpretations from the interpretations collection
    
    Gets all of the interpretations
    """
    current_app.logger.info("Retrieving interpretation catalog.")
    output = bpc.get_interpretation_catalog(_get_interpretation_collection())
    current_app.logger.debug(output)
   
    return Response(output, status=200, mimetype='application/json')

@bp.route('/domain/<domain_guid>', methods=['GET'])
def get_domain_interpretations(domain_guid):
    """Return all interpretations of a given domain
    
    Gets all interpretations associated with a given domain_guid
    """
    current_app.logger.info("Retrieving interpretations for domain.")
    current_app.logger.debug("domain_guid: " + domain_guid)
    output = bpc.get_interpretations_by_domain(_get_interpretation_collection(), domain_guid)
    current_app.logger.debug(output)
    
    return Response(output, status=200, mimetype='application/json')

@bp.route('/interpretation/<interpretation_guid>', methods=['GET'])
def get_interpretation(interpretation_guid):
    """Return a single interpretation with the associated guid
    
    Gets a lone interpretation from the interpretation collection
    """
    current_app.logger.info("Retrieving read interpretation request.")
    current_app.logger.debug("interpretation_guid: " + interpretation_guid)
    output = bpc.get_interpretation_by_guid(_get_interpretation_collection(), interpretation_guid)
    current_app.logger.debug(output)
   
    return Response(output, status=200, mimetype='application/json')

@bp.route('/update/domain', methods=['POST'])
def update_domain():
    """Return the modified count of objects
    
    Updates a domain object from the domain collection
    """
    current_app.logger.info("Received update domain request.")
    current_app.logger.debug("Retrieving update domain request.")
    if request.headers['Content-Type'] == 'application/json':
        current_app.logger.debug("Content type is application/json")
        domain_object = request.json
        current_app.logger.debug("domain_object: " , domain_object)
        output = bpc.update_domain(_get_domain_collection(), domain_object)
        current_app.logger.debug(output)
        
        if output == 1:
            return str(output), status.HTTP_202_ACCEPTED
        elif output > 1:
            return str(output), status.HTTP_409_CONFLICT
        else:
            return str(output), status.HTTP_404_NOT_FOUND
    
@bp.route('/update/interpretation', methods=['POST'])
def update_interpretation():
    """Return the modified count of objects
    
    Updates an interpretation from the interpretation collection
    """
    current_app.logger.info("Received update interpretation request.")
    if request.headers['Content-Type'] == 'application/json':
        current_app.logger.debug("Content type is application/json")
        interpretation_object = request.json
        current_app.logger.debug("interpretation_object: " , interpretation_object)
        output = bpc.update_interpretation(_get_interpretation_collection(), interpretation_object)
        current_app.logger.debug(output)
        
    if output == 1:
        return str(output), status.HTTP_202_ACCEPTED
    elif output > 1:
        return str(output), status.HTTP_409_CONFLICT
    else:
        return str(output), status.HTTP_404_NOT_FOUND

@bp.route('/delete/domain/<domain_guid>', methods=['DELETE'])
def delete_domain(domain_guid):
    """Return the deleted count

    Deletes a domain from the domain collection
    """
    current_app.logger.info("Retrieving delete domain request.")
    output = bpc.delete_domain(_get_domain_collection(), _get_interpretation_collection(), domain_guid)
    current_app.logger.debug(output)
    
    if output == 1:
        return str(output), status.HTTP_202_ACCEPTED
    elif output > 1:
        return str(output), status.HTTP_409_CONFLICT
    else:
        return str(output), status.HTTP_404_NOT_FOUND
    
@bp.route('/delete/interpretation/<interpretation_guid>', methods=['DELETE'])
def delete_interpretation(interpretation_guid):
    """Return the deleted count
    
    Deletes an interpretation from the interpretation collection
    """
    current_app.logger.info("Retrieving delete interpretation request.")
    output = bpc.delete_interpretation(_get_interpretation_collection(), interpretation_guid)
    current_app.logger.debug(output)
    
    if output == 1:
        return str(output), status.HTTP_202_ACCEPTED
    elif output > 1:
        return str(output), status.HTTP_409_CONFLICT
    else:
        return str(output), status.HTTP_404_NOT_FOUND

@bp.route('/interpret', methods = ['POST'])
def run_interpretation_engine():
    """Return a profile with new interpretations
    
    Performs the interpretation of a given profile
    """
    try:
        current_app.logger.info("Interpret call received.")
        if request.headers['Content-Type'] == 'application/json':
            if 'profile' not in request.json:
                current_app.logger.error("Error: Undefined content received (no \'profile\' key).")
                return None
            num_keys = len(request.json['profile'])
            current_app.logger.info("Received interpret request for profile with " + str(num_keys) +" keys.")
            domain_guid=bpc._get_domain_id_by_name(_get_domain_collection(), request.json['domain_guid'])
            if domain_guid is not None:
                    
                uninterpretted_profile=request.json['profile']
                i = bpc.run_interpretation(_get_interpretation_collection(),
                        domain_guid, uninterpretted_profile)
                logging.info("Interpretation mapping:" + str(i))
                return Response(i, status=200, mimetype='application/json')
            else:
                current_app.logger.error("Domain guid not found for name " + request.json['domain_guid'])
                return Response(status=404, mimetype='application/json')
        else:
            current_app.logger.error("Error: Content-Type json not received.")
            return None
    except Exception as exc:
        current_app.logger.exception(exc)
        return None
    
@bp.route('/reversegeo', methods = ['POST'])
def run_reverse_geo_batch():
    """Return a dict of reverse geocoding results from given coordinates
    
    Runs the reverse geo service on a series of coordinates
    """
    if request.headers['Content-Type'] == 'application/json':
        if 'coordinates' not in request.json:
            current_app.logger.error("Error: Undefined content received (no \'coordinates\' key).")
            return None
        num_Coordinates = len(request.json['coordinates'])
        current_app.logger.info("Received " + str(num_Coordinates) + " reverse geocoding requests.")
        geo_data = bpc.reverse_geo_batch(_get_reverse_geo_collection(), request.json['coordinates'])
        return Response(geo_data, status=200, mimetype='application/json')
    else:
        current_app.logger.error("Error: Content-Type json not received.")
        return Response("Bad Request", status=403, mimetype='text/html')
    
@bp.route('/validatePythonScript', methods = ['POST'])
def validate_script():
    """Return the validation of a Python script"""
    try:
        current_app.logger.info("Received validate script request.")
        if request.headers['Content-Type'] == 'application/json':
            current_app.logger.debug("Content type is application/json")
            iIdJson = request.json
            current_app.logger.debug("Object received: " + str(iIdJson))
            
            interpretation_guid = iIdJson["iId"]
            current_app.logger.info("Retrieving interpretation for validation.")
            current_app.logger.debug("interpretation_guid: " + interpretation_guid)
            interpretation_json_array_str = bpc.get_interpretation_by_guid(_get_interpretation_collection(), interpretation_guid)
            current_app.logger.debug(interpretation_json_array_str)
            interpretation_json_str = interpretation_json_array_str[1:(len(interpretation_json_array_str) - 1)]
            current_app.logger.debug(interpretation_json_str)
            interpretation_json = json.loads(interpretation_json_str)
            script = interpretation_json["iScript"]
            
            validate_result = bpc.validate_python_script(script)
            script_result = validate_result["discrepancies"]
            current_app.logger.debug("Script result: " + script_result)
            
            if(validate_result["isValid"] == True):
                return Response(script_result, status=200, mimetype='application/json')
            else:
                return Response(script_result, status=417, mimetype='application/json')
        else:
            return "Request header must be application/json.", status.HTTP_400_BAD_REQUEST
    except Exception as exc:
        current_app.logger.exception(exc)
        return None
   
@bp.route('/testPythonScript', methods = ['POST'])
def test_script():
    """Return the success of failure (true/false) of a script against example values"""
    try:
        current_app.logger.info("Received test script request.")
        if request.headers['Content-Type'] == 'application/json':
            current_app.logger.debug("Content type is application/json")
            testContentJson = request.json
            current_app.logger.debug("Object received: " + str(testContentJson))
            
            interpretation_guid = testContentJson["iId"]
            profile = Profile('exampleField', testContentJson["profile"]['exampleField'])
            current_app.logger.info("Retrieving interpretation for testing.")
            current_app.logger.debug("interpretation_guid: " + interpretation_guid)
            current_app.logger.debug("profile: " + str(profile.convert_to_metrics_dictionary().user_facing_profile_mapping))
            interpretation_json_array_str = bpc.get_interpretation_by_guid(_get_interpretation_collection(), interpretation_guid)
            current_app.logger.debug(interpretation_json_array_str)
            interpretation_json_str = interpretation_json_array_str[1:(len(interpretation_json_array_str) - 1)]
            current_app.logger.debug(interpretation_json_str)
            interpretation_json = json.loads(interpretation_json_str)
            script = interpretation_json["iScript"]
            current_app.logger.debug("Script (base64): " + script)
            # Test this 
            #test_result = bpc.test_python_script(script, profile)
            test_result = bpc.test_interpretation(interpretation_json, profile)
            if test_result is None:
                current_app.logger.error("Unexpected 'None' result from sidekick.")
                return Response(dumps(Response().to_schema_wizard_object()), status=500, mimetype='application/json')
            current_app.logger.debug("Test result: " + str(test_result.result))
            current_app.logger.debug("Processed test result: " + str(test_result))
            
            if not test_result.error:
                return Response(dumps(test_result.to_schema_wizard_object()), status=200, mimetype='application/json')
            else:
                return Response(dumps(test_result.to_schema_wizard_object()), status=417, mimetype='application/json')
        else:
            return "Request header must be application/json.", status.HTTP_400_BAD_REQUEST, {'Content-Type': 'text/plain; charset=utf-8'}
    except Exception as exc:
        current_app.logger.exception(exc)
        return None

def _get_domain_collection():
    """Essentially a connection to the domains collection"""
    return client[domain_db_name]['domains']

def _get_interpretation_collection():
    """Essentially a connection to the interpretations collection"""
    return client[domain_db_name]['interpretations']

def _get_reverse_geo_collection():
    """Essentially a connection to the geospatial collection"""
    return client[reverse_geo_db_name]['geospatial']
    