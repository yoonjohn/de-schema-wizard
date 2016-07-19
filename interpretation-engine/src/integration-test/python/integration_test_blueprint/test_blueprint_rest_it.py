'''
Created on Apr 11, 2016

@author: yoonj1
'''

import unittest
from root.rest import RootREST
from bson.json_util import dumps
from root.config import IEConfig

example_domain_json = {"dName":"La Bamba","dVersion":"18.0","dLastUpdate":1459528994346,"dDescription":"Domains of La Bamba."}
example_interpretation_json = { "iDomainId": "domain-01", "iName": "Balalala", "iDescription": "The classic phrase", "iVersion": "0.1", "iConstraints": { "main-type": "string", "detail-type": "phrase" }, "iScript": "ZGVmIHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk6DQogICAgcmV0dXJuIFRydWUNCmZpZWxkX3Byb2ZpbGUgPSBsb2NhbHMoKVsnZmllbGRfcHJvZmlsZSddDQppc192YWxpZF9pbnRlcnByZXRhdGlvbiA9IHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk=" }

class RootRestTest(unittest.TestCase):

    def setUp(self):
        ie = IEConfig()
        self.root = RootREST('localhost', False, ie.port_num)
        self.app = self.root.app
        self.app.testing = True
        self.tester = self.app.test_client(self)

    def test_landing(self):
        response = self.tester.get('/', content_type='text/plain')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data.decode("utf-8"), 'Schema Wizard Interpretation Engine')
        
    def test_blueprint(self):
        response = self.tester.get('/blueprint/test', content_type='text/plain')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data.decode("utf-8"), 'Test')
        
    def test_blueprint_landing(self):
        response = self.tester.get('/blueprint/domains', content_type='application/json')
        self.assertEqual(response.status_code, 200)
        
    def test_create_domain(self):
        print(example_domain_json)
        response = self.tester.post('/blueprint/create/domain', data=dumps(example_domain_json), content_type = 'application/json')
        #self.assertEqual(response.status_code, 400)
        
    def test_get_interpretations(self):
        response = self.tester.get('/blueprint/domain/mocked-guid', content_type='application/json')
        self.assertEqual(response.status_code, 200)
        
    #===========================================================================
    # def test_create_domain(self):
    #     response = self.tester.post('blueprint/create/domain', content_type='application/json')
    #===========================================================================
        
if __name__ == "__main__":
    unittest.main()