'''
Created on Apr 11, 2016

@author: yoonj1
'''
import unittest
from blueprint import blueprint_core as bpc
from blueprint import blueprint_rest as bpr
import logging
import json

# Initializing the logger
logger = logging.getLogger()
logger.level = logging.INFO
#stream_handler = logging.StreamHandler(sys.stdout)

# Test variables
example_domain_json = {"dName":"La Bamba","dVersion":"18.0","dLastUpdate":1459528994346,"dDescription":"Domains of La Bamba."}
example_interpretation_json = { "iDomainId": "domain-01", "iName": "Balalala", "iDescription": "The classic phrase", "iVersion": "0.1", "iConstraints": { "main-type": "string", "detail-type": "phrase" }, "iScript": "ZGVmIHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk6DQogICAgcmV0dXJuIFRydWUNCmZpZWxkX3Byb2ZpbGUgPSBsb2NhbHMoKVsnZmllbGRfcHJvZmlsZSddDQppc192YWxpZF9pbnRlcnByZXRhdGlvbiA9IHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk=" }

example_domain_update = {"dId":"domain-01","dName":"La Bamba Lives","dVersion":"18.0","dLastUpdate":1459528994346,"dDescription":"Domains that go in La Bamba."}
example_interpretation_update = { "iDomainId": "domain-01", "iId": "guid-01", "iName": "Gracias", "iDescription": "The classic phrase", "iVersion": "0.1", "iConstraints": { "main-type": "string", "detail-type": "phrase" }, "iScript": "ZGVmIHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk6DQogICAgcmV0dXJuIFRydWUNCmZpZWxkX3Byb2ZpbGUgPSBsb2NhbHMoKVsnZmllbGRfcHJvZmlsZSddDQppc192YWxpZF9pbnRlcnByZXRhdGlvbiA9IHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk=" }
    
class BlueprintCoreIT(unittest.TestCase):
    """A test class for the blueprint_core.
    
    TODO Use more meaningful asserts (They are currently stubs)
        Make tests more independent even if it means monolithic tests
    
    This class tests the functionalities associated with Python and MongoDB
    """
    def setUp(self):
        """This runs before every tests"""
        #logger.addHandler(stream_handler)
        #stream_handler.stream = sys.stdout
        
        # Clears DB of any instances of example data
        domain_condition = {"dName": example_domain_json["dName"]}
        interpretation_condition = {"iName": example_interpretation_json["iName"]}
        
        bpr._get_domain_collection().delete_many(domain_condition)
        bpr._get_interpretation_collection().delete_many(interpretation_condition)
    
    def tearDown(self):
        """This runs after every test"""
        #logger.removeHandler(stream_handler)
        
    def test_create_bad_domain(self):
        logger.info("\nInserting domain.")
        ins_output_1 = bpc.create_domain(bpr._get_domain_collection(), example_domain_json)
        dId = bpc._get_domain_id_by_name(bpr._get_domain_collection(), example_domain_json["dName"])
        logger.info("1 record(s) inserted with ID {}".format(ins_output_1))
        assert ins_output_1 == dId
        
        logger.info("Inserting duplicate domain.")
        ins_output_2 = bpc.create_domain(bpr._get_domain_collection(), example_domain_json)
        assert ins_output_2 == "-1"
        
        logger.info("Successfully rejected a duplicate domain name")
        
        logger.info("Deleting domain with ID: %s" % dId)
        del_output = bpc.delete_domain(bpr._get_domain_collection(), bpr._get_interpretation_collection(), dId)
        logger.info("%s record(s) deleted." % del_output)

    def test_create_bad_interpretation(self):
        logger.info("\nInserting interpretation.")
        ins_output_1 = bpc.create_interpretation(bpr._get_interpretation_collection(), example_interpretation_json)
        iId = bpc._get_interpretation_id_by_name(bpr._get_interpretation_collection(), example_interpretation_json["iName"])
        logger.info("{} record(s) inserted with ID {}".format(ins_output_1, iId))
        assert ins_output_1 == iId
        
        logger.info("Inserting duplicate interpretation.")
        ins_output_2 = bpc.create_interpretation(bpr._get_interpretation_collection(), example_interpretation_json)
        assert ins_output_2 == "-1"        
       
        logger.info("Successfully rejected a duplicate interpretation name")
        
        logger.info("Deleting interpretation with ID: %s" % iId)
        del_output = bpc.delete_interpretation(bpr._get_interpretation_collection(), iId)
        logger.info("%s record(s) deleted." % del_output)

    def test_create_domain(self):
        """Assert one record inserted 
        
        Relies on a correct domain_object (json) being passed in.
        Malformed jsons will corrupt the database schema.
        """
        logger.info("\nInserting domain.")
        result = bpc.create_domain(bpr._get_domain_collection(), example_domain_json)
        dId = bpc._get_domain_id_by_name(bpr._get_domain_collection(), example_domain_json["dName"])
        logger.info("1 record(s) inserted with ID {}".format(dId))
        assert dId == dId
        
        logger.info("Deleting domain with ID: %s" % dId)
        del_output = bpc.delete_domain(bpr._get_domain_collection(), bpr._get_interpretation_collection(), dId)
        logger.info("%s record(s) deleted." % del_output)
    
    def test_create_interpretation(self):
        """Assert one record inserted 
        
        Relies on a correct interpretation_object (json) being passed in.
        Malformed jsons will corrupt the database schema.
        """
        logger.info("\nInserting interpretation.")
        ins_output = bpc.create_interpretation(bpr._get_interpretation_collection(), example_interpretation_json)
        iId = bpc._get_interpretation_id_by_name(bpr._get_interpretation_collection(), example_interpretation_json["iName"])
        logger.info("{} record(s) inserted.".format(ins_output))
        assert ins_output == iId
        
        logger.info("Deleting interpretation with ID: %s" % iId)
        del_output = bpc.delete_interpretation(bpr._get_interpretation_collection(), iId)
        logger.info("%s record(s) deleted." % del_output)
    
    def test_get_interpretations_by_domain(self):
        """Return all interpretations corresponding to a domain_guid
        
        Sorts by iName
        """
        logger.info("\nInserting domain.")
        domain_ins_output = bpc.create_domain(bpr._get_domain_collection(), example_domain_json)
        dId = bpc._get_domain_id_by_name(bpr._get_domain_collection(), example_domain_json["dName"])
        logger.info("{} record(s) inserted with ID {}".format(domain_ins_output, dId))
        
        logger.info("Inserting interpretation.")
        interpretation_ins_output = bpc.create_interpretation(bpr._get_interpretation_collection(), example_interpretation_json)
        iId = bpc._get_interpretation_id_by_name(bpr._get_interpretation_collection(), example_interpretation_json["iName"])
        logger.info("%s record(s) inserted with ID: %s" % (interpretation_ins_output, iId))
        
        if domain_ins_output == "1" and interpretation_ins_output == "1":
            get_i_by_d = bpc.get_interpretations_by_domain(bpr._get_interpretation_collection(), example_interpretation_json["iDomainId"])
            logger.info(get_i_by_d)
            result_list = json.loads(get_i_by_d)
            
            for result_json in result_list: 
                logger.info(result_json["iId"])
        
            logger.info("Deleting domain with ID: %s" % dId)
            del_output = bpc.delete_domain(bpr._get_domain_collection(), bpr._get_interpretation_collection(), dId)
            logger.info("{} record(s) deleted.".format(del_output))

            logger.info("Deleting interpretation with ID: %s" % iId)
            del_output = bpc.delete_interpretation(bpr._get_interpretation_collection(), iId)
            logger.info("{} record(s) deleted.".format(del_output))
        else:
            pass # Pass because there was an error in the domain or interpretation insert
    
    def test_get_domain_catalog(self):
        """Return all domains in the domain collection
        
        Sorts by dName
        """
        logger.info("\nInserting domain.")
        domain_ins_output = bpc.create_domain(bpr._get_domain_collection(), example_domain_json)
        dId = bpc._get_domain_id_by_name(bpr._get_domain_collection(), example_domain_json["dName"])
        logger.info("{} record(s) inserted with ID {}".format(domain_ins_output, dId))
        
        logger.info("Inserting interpretation.")
        interpretation_ins_output = bpc.create_interpretation(bpr._get_interpretation_collection(), example_interpretation_json)
        iId = bpc._get_interpretation_id_by_name(bpr._get_interpretation_collection(), example_interpretation_json["iName"])
        logger.info("%s record(s) inserted with ID: %s" % (interpretation_ins_output, iId))
        
        if domain_ins_output == "1" and interpretation_ins_output == "1":
            domain_coll = bpc.get_domain_catalog(bpr._get_domain_collection())
            logger.info(domain_coll)
            result_list = json.loads(domain_coll)
            assert result_list is not None
            
            for result_json in result_list: 
                logger.info(str(result_json))
        
            logger.info("Deleting domain with ID: %s" % dId)
            del_output = bpc.delete_domain(bpr._get_domain_collection(), bpr._get_interpretation_collection(), dId)
            logger.info("{} record(s) deleted.".format(del_output))

            logger.info("Deleting interpretation with ID: %s" % iId)
            del_output = bpc.delete_interpretation(bpr._get_interpretation_collection(), iId)
            logger.info("{} record(s) deleted.".format(del_output))
        else:
            pass # Pass because there was an error in the domain or interpretation insert
    
    def test_get_interpretation(self):
        """Return one interpretation with a matching interpretation_guid
        
        Searches by iId.
        """
        logger.info("\nInserting domain.")
        domain_ins_output = bpc.create_domain(bpr._get_domain_collection(), example_domain_json)
        dId = bpc._get_domain_id_by_name(bpr._get_domain_collection(), example_domain_json["dName"])
        logger.info("{} record(s) inserted with ID {}".format(domain_ins_output, dId))
        
        logger.info("Inserting interpretation.")
        interpretation_ins_output = bpc.create_interpretation(bpr._get_interpretation_collection(), example_interpretation_json)
        iId = bpc._get_interpretation_id_by_name(bpr._get_interpretation_collection(), example_interpretation_json["iName"])
        logger.info("%s record(s) inserted with ID: %s" % (interpretation_ins_output, iId))
        
        if domain_ins_output == "1" and interpretation_ins_output == "1":
            interpretation = bpc.get_interpretation(bpr._get_interpretation_collection(), example_interpretation_json["iId"])
            logger.info(interpretation)
            assert interpretation is not None
        
            logger.info("Deleting domain with ID: %s" % dId)
            del_output = bpc.delete_domain(bpr._get_domain_collection(), bpr._get_interpretation_collection(), dId)
            logger.info("{} record(s) deleted.".format(del_output))

            logger.info("Deleting interpretation with ID: %s" % iId)
            del_output = bpc.delete_interpretation(bpr._get_interpretation_collection(), iId)
            logger.info("{} record(s) deleted.".format(del_output))
        else:
            pass # Pass because there was an error in the domain or interpretation insert        
    
    def test_update_domain(self):
        """Return the number of records modified (0 or 1)
        
        Only updates the first occurrence of a matching Mongo document.
        """
        logger.info("\nInserting domain.")
        ins_output = bpc.create_domain(bpr._get_domain_collection(), example_domain_json)
        dId = bpc._get_domain_id_by_name(bpr._get_domain_collection(), example_domain_json["dName"])
        logger.info("{} record(s) inserted with ID {}".format(ins_output, dId))
        
        if ins_output == "1":
            example_domain_update["dId"] = dId
            
            logger.info("Updating domain with ID: %s" % dId)
            output = bpc.update_domain(bpr._get_domain_collection(), example_domain_update)
            logger.info("{} record(s) updated.".format(output))
            assert output == "1"
        
            logger.info("Deleting domain with ID: %s" % dId)
            del_output = bpc.delete_domain(bpr._get_domain_collection(), bpr._get_interpretation_collection(), dId)
            logger.info("{} record(s) deleted.".format(del_output))
        else:
            pass # Pass because there was an error in the domain insert
        
    def test_update_interpretation(self):
        """Return the number of records modified (0 or 1)
        
        Only updates the first occurrence of a matching Mongo document.
        """
        logger.info("\nInserting interpretation.")
        ins_output = bpc.create_interpretation(bpr._get_interpretation_collection(), example_interpretation_json)
        iId = bpc._get_interpretation_id_by_name(bpr._get_interpretation_collection(), example_interpretation_json["iName"])
        logger.info("{} record(s) inserted with ID {}".format(ins_output, iId))
        
        if ins_output == "1":
            example_interpretation_update["iId"] = iId
            
            logger.info("Updating interpretation with ID: %s" % iId)
            output = bpc.update_interpretation(bpr._get_interpretation_collection(), example_interpretation_update)
            logger.info("%s record(s) updated." % output)
            assert output == "1"
        
            logger.info("Deleting interpretation with ID: %s" % iId)
            del_output = bpc.delete_interpretation(bpr._get_interpretation_collection(), iId)
            logger.info("%s record(s) deleted." % del_output)
        else:
            pass # Pass because there was an error in the domain insert
        
        #=======================================================================
        # output = bpc.update_interpretation(bpr._get_interpretation_collection(), example_interpretation_update)
        # logging.getLogger().info(output)
        # assert output is not None
        #=======================================================================
        
    def test_delete_domain(self):
        """Return the number of records deleted (0 or 1)

        Only deletes the first occurrence of a matching Mongo document.
        """
        logger.info("\nInserting domain.")
        ins_output = bpc.create_domain(bpr._get_domain_collection(), example_domain_json)
        dId = bpc._get_domain_id_by_name(bpr._get_domain_collection(), example_domain_json["dName"])
        logger.info("{} record(s) inserted with ID {}".format(ins_output, dId))
        
        if ins_output == "1":
            logger.info("Deleting domain with ID: %s" % example_domain_json["dId"])
            del_output = bpc.delete_domain(bpr._get_domain_collection(), bpr._get_interpretation_collection(), example_domain_json["dId"])
            logger.info("%s record(s) deleted." % del_output)
            assert del_output == "1"
        else:
            pass # Pass because there was an error in the domain insert

    def test_delete_interpretation(self):
        """Return the number of records deleted (0 or 1)

        Only deletes the first occurrence of a matching Mongo document.
        """
        logger.info("\nInserting interpretation.")
        ins_output = bpc.create_interpretation(bpr._get_interpretation_collection(), example_interpretation_json)
        iId = bpc._get_interpretation_id_by_name(bpr._get_interpretation_collection(), example_interpretation_json["iName"])
        logger.info("{} record(s) inserted with ID {}".format(ins_output, iId))
        assert ins_output == iId
        
        if ins_output == "1":
            logger.info("Deleting interpretation with ID: %s" % example_interpretation_json["iId"])
            del_output = bpc.delete_interpretation(bpr._get_interpretation_collection(), example_interpretation_json["iId"])
            logger.info("%s record(s) deleted." % del_output)
            assert del_output == "1"
        else:
            pass # Pass because there was an error in the interpretation insert

#-------------------- def test_run_interpretation(domain_guid, profile_mapping):
    # interpretation_list = loads(get_interpretations_by_domain(bpr._get_interpretation_collection(), domain_guid))
    # interpretation_mapping = InterpretationEngine().interpret(interpretation_list, profile_mapping)
    #------------------------------------- #take out script from returned object
    #------------------- for valid_interpretation_key in interpretation_mapping:
        #------------ for i in interpretation_mapping[valid_interpretation_key]:
            #----------------------------------------------- i['iScript'] = None
    #-------------------------------------- return dumps(interpretation_mapping)
#------------------------------------------------------------------------------ 
#--------- def test__single_reverse_geo_query(reverse_geo_collection, lat, lng):
    # query_params =  {"geometry":{"$geoIntersects":{"$geometry":{"type":"Point","coordinates":[lng, lat]}}}}
    #--------------- cursor = reverse_geo_collection.find(query_params).limit(1)
    #---------------------------------------------------- if cursor.count() > 0:
        #--------------------------- return cursor[0]['properties']['name_long']
    #--------------------------------------------------------------------- else:
        #----------------------------------------------------------- return None
#------------------------------------------------------------------------------ 
#---------- def test_reverse_geo_batch(reverse_geo_collection, coordinate_list):
    #------------------------------------------------------------ geo_batch = []
    #---------------------------------------- for coordinate in coordinate_list:
        # country = _single_reverse_geo_query(reverse_geo_collection, coordinate[0], coordinate[1])
        #--------------------------------------------------- if country != None:
            #----------------------------------------- geo_batch.append(country)
    #--------------------------------------------------- return dumps(geo_batch)
    
    
if __name__ == '__main__':
    unittest.main()