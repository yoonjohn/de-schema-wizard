'''
Created on Mar 31, 2016

@author: yoonj1
'''
from bson.json_util import dumps
from json import loads
from engine.interpret import InterpretationEngine
from pymongo import ASCENDING
import uuid
from engine.script_validator import ScriptValidator

def test():
    """Simple test method for the REST interface"""
    return ("Test")

def display_info(domain_collection):
    """Return all domains in the domain collection
    
    Indiscriminate getter
    """
    list_domains = domain_collection.find()
    return dumps(list_domains)

def create_domain(domain_collection, domain_object):
    """Return the number of documents inserted
    
    Relies on a correct domain_object (json) being passed in.
    Malformed jsons will corrupt the database schema.
    """
    output = 0
    
    condition = {"dName": domain_object["dName"]}
    records = domain_collection.find(condition)
    
    if records.count() == 0:
        dId = str(uuid.uuid4())
        domain_object["dId"] = dId
        domain_collection.insert_one(domain_object)
        output = dId
    else:
        print ("Duplicate domain name attempting to be inserted. Will not proceed.")
        output = "-1"
    
    return output

def create_interpretation(interpretation_collection, interpretation_object):
    """Return the number of documents inserted
    
    Relies on a correct interpretation_object (json) being passed in.
    Malformed jsons will corrupt the database schema.
    """
    output = 0
    
    condition = {"iName": interpretation_object["iName"], "iDomainId":interpretation_object["iDomainId"]}
    records = interpretation_collection.find(condition)
    
    if records.count() == 0:
        iId = str(uuid.uuid4())
        interpretation_object["iId"] = iId
        interpretation_collection.insert_one(interpretation_object)
        output = iId
    else:
        print ("Duplicate interpretation name attempting to be inserted. Will not proceed.")
        output = "-1"
        
    return output

def get_interpretations_by_domain(interpretation_collection, domain_guid):
    """Return all interpretations corresponding to a domain_guid
    
    Sorts by iName
    """
    return dumps(interpretation_collection.find({'iDomainId':domain_guid}, {"_id":0}).sort([("iName", ASCENDING)]))

def get_interpretation_by_guid(interpretation_collection, interpretation_guid):
    """Return one interpretation that matches the given guid
    
    Sorts by iName
    """
    return dumps(interpretation_collection.find({'iId':interpretation_guid}))

def get_domain_catalog(domain_collection):
    """Return all domains in the domain collection
    
    Sorts by dName
    """
    return dumps(domain_collection.find().sort([("dName", ASCENDING)]))

def get_interpretation_catalog(interpretation_collection):
    """Return all interpretations in the interpretation collection
    
    Sorts by iName
    """
    return dumps(interpretation_collection.find().sort([("iName", ASCENDING)]))

def get_interpretation(interpretation_collection, interpretation_guid):
    """Return one interpretation with a matching interpretation_guid
    
    Searches by iId.
    """
    return dumps(interpretation_collection.find({'iId':interpretation_guid}))

def update_domain(domain_collection, domain_object):
    """Return the number of records modified (0 or 1)
    
    Only updates the first occurrence of a matching Mongo document.
    """
    num_modified_records = 0
    dId = domain_object['dId']
    condition = {"dId": dId}
    records = domain_collection.find(condition)
    if records.count() == 1:
        result = domain_collection.replace_one(condition, domain_object)
        num_modified_records = result.modified_count
    elif records.count() > 1:
        print ("More than one record found. Update will not proceed.")
        num_modified_records = records.count()
    else:
        print ("No updatable record found.")
        num_modified_records = records.count()
    return num_modified_records
    
def update_interpretation(interpretation_collection, interpretation_object):
    """Return the number of records modified (0 or 1)
    
    Only updates the first occurrence of a matching Mongo document.
    """
    num_modified_records = 0
    iId = interpretation_object['iId']
    condition = {"iId": iId}
    records = interpretation_collection.find(condition)
    if records.count() == 1:
        result = interpretation_collection.replace_one(condition, interpretation_object)
        num_modified_records = result.modified_count
    elif records.count() > 1:
        print ("More than one record found. Update will not proceed.")
        num_modified_records = records.count()
    else:
        print ("No updatable record found.")
        num_modified_records = records.count()        
    return num_modified_records
    
def delete_domain(domain_collection, interpretation_collection, domain_guid):
    """Return the number of records deleted (0 or 1)
    
    Deletes all associated interpretations!
    
    Only deletes the first occurrence of a matching Mongo document.
    """
    records_found = 0;
    delete_domain_condition = {"dId": domain_guid}
    records = domain_collection.find(delete_domain_condition)
    
    if records.count() == 1:
        # Delete the domain
        delete_domain_result = domain_collection.delete_one(delete_domain_condition)
        records_found = delete_domain_result.deleted_count
        
        # Delete the interpretations
        delete_interpretation_condition = {"iDomainId": domain_guid}
        delete_interpretation_result = interpretation_collection.delete_many(delete_interpretation_condition)
    elif records.count() > 1:
        print ("More than one record found. Delete will not proceed.")
        records_found = records.count()
    else:
        print ("No deletable record found.")
        records_found = records.count()        
    
    return records_found

def delete_interpretation(interpretation_collection, interpretation_guid):
    """Return the number of records deleted (0 or 1)
    
    Only deletes the first occurrence of a matching Mongo document.
    """    
    records_found = 0;
    delete_interpretation_condition = {"iId": interpretation_guid}
    records = interpretation_collection.find(delete_interpretation_condition)
    
    if records.count() == 1:
        # Delete the interpretations
        delete_interpretation_result = interpretation_collection.delete_one(delete_interpretation_condition)
        records_found = delete_interpretation_result.deleted_count
    elif records.count() > 1:
        print ("More than one record found. Delete will not proceed.")
        records_found = records.count()
    else:
        print ("No deletable record found.")
        records_found = records.count()        
    
    return records_found

def run_interpretation(interpretation_collection, domain_guid, profile_mapping):
    """Return the mapping with the newly added interpretations"""
    interpretation_list = loads(get_interpretations_by_domain(interpretation_collection, domain_guid))
    interpretation_mapping = InterpretationEngine().interpret(interpretation_list, profile_mapping)
    # take out script from returned object
    for valid_interpretation_key in interpretation_mapping:
        for i in interpretation_mapping[valid_interpretation_key]:
            i['iScript'] = None
    return dumps(interpretation_mapping)

def _get_domain_id_by_name(domain_collection, dName):
    condition = {"dName":dName}
    domain = domain_collection.find_one(condition, {"dId":1, "_id":0})
    if domain is not None:
        return domain["dId"]
    else:
        return None

def _get_interpretation_id_by_name(interpretation_collection, iName):
    condition = {"iName":iName}
    interpretation = interpretation_collection.find_one(condition, {"iId":1, "_id":0})
    return interpretation["iId"]

def _single_reverse_geo_query(reverse_geo_collection, lat, lng):
    """Used to format the data for reverse geo"""
    query_params = {"geometry":{"$geoIntersects":{"$geometry":{"type":"Point", "coordinates":[lng, lat]}}}}
    cursor = reverse_geo_collection.find(query_params).limit(1)
    if cursor.count() > 0:
        return cursor[0]['properties']['name_long']
    else:
        return None
    
def reverse_geo_batch(reverse_geo_collection, coordinate_list):
    """Return the list of reverse geo results
    
    Processes reverse get in a batch.
    """
    geo_batch = []
    for coordinate in coordinate_list:
        country = _single_reverse_geo_query(reverse_geo_collection, coordinate[0], coordinate[1])
        if country != None:
            geo_batch.append(country)
    return dumps(geo_batch)

def validate_python_script(b64Script):
    """Convert a base64 String and validate whether or not it is valid"""
    script_validator = ScriptValidator()
    return script_validator.validate(b64Script)
        
def test_python_script(script, profile_mapping):
    """Return the mapping with the newly added interpretations"""
    test_result = InterpretationEngine().interpret_one(script, profile_mapping)
    return test_result

def test_interpretation(interpretation, profile_mapping):
    test_result = InterpretationEngine().test_interpret(interpretation, profile_mapping)
    return test_result


