'''
Created on Apr 12, 2016

@author: leegc
'''

import unittest
from json import loads
from json import dumps
from engine.interpret import InterpretationEngine
from engine.profile import STRUCTURED_KEY_SEPARATOR
import mongo_dao
import logging
import os

class Test(unittest.TestCase):
    
    def test_interpret_with_sample_file(self):
        logging.info("Reading test file.")
        file = open(os.path.dirname(os.path.abspath(__file__)) + os.sep + "SampleTest1_NoInterpretations.json", 'r')
        contents = file.read()
        json = loads(contents)
        file.close()
        ie = InterpretationEngine()
        mock_interpretation_list = mongo_dao.mongo_instance(globals()).interpretation_list("mocked-guid")
        result = ie.interpret(mock_interpretation_list, json['dsProfile'])
        for valid_interpretation_key in result:
            for i in result[valid_interpretation_key]:
                i['iScript'] = None
        test_key_1 = 'vehicle'+STRUCTURED_KEY_SEPARATOR+'positions'+STRUCTURED_KEY_SEPARATOR+'latitude'
        test_key_2 = 'vehicle'+STRUCTURED_KEY_SEPARATOR+'positions'+STRUCTURED_KEY_SEPARATOR+'longitude'
        hasDesiredInterpretations = (len(result[test_key_1]) > 0 
                                     and len(result[test_key_2]) > 0)
        if hasDesiredInterpretations:
            logging.info("Desired interpretations successfully added.  Test passed.")
            assert True
        else:
            logging.error("Test error: desired interpretations not successfully added.")
            assert False

    def test_interpret_with_sigacts_sample(self):
        logging.info("Reading test sigacts file.")
        file = open(os.path.dirname(os.path.abspath(__file__)) + os.sep + "sigactsprofile.txt", 'r')
        contents = file.read()
        json = loads(contents)
        file.close()
        ie = InterpretationEngine()
        mock_interpretation_list = mongo_dao.mongo_instance(globals()).interpretation_list("mocked-guid")
        result = ie.interpret(mock_interpretation_list, json['profile'])
        
        hasDesiredInterpretations = (len(result['CATEGORY']) == 0 and len(result['DTG']) > 0)
        if hasDesiredInterpretations:
            logging.info("Desired interpretations successfully added.  Test passed.")
            assert True
        else:
            logging.error("Test error: desired interpretations not successfully added.")
            logging.error("CATEGORY " + str(result['CATEGORY']) + " and DTG " + str(result['DTG']))
            assert False
    