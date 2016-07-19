'''
Created on Mar 30, 2016

@author: leegc
'''
import logging

class _MongoAccessor(object):
    
    def __init__(self):
        self.output = True
    
    def interpretation_list(self, domain):
        static_latitude_constraints_mapping = {'min':-90, 'max':90, 'main-type':'number', 'detail-type':'decimal'}
        static_latitude_interpretation_map = {'iName':'Latitude', 'iMatchingNames' : ['latitude'],
                                               'iConstraints':static_latitude_constraints_mapping}
        static_latitude_interpretation_map['iScript'] = "ZGVmIHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk6DQogICAgcmV0dXJuIFRydWUNCmZpZWxkX3Byb2ZpbGUgPSBsb2NhbHMoKVsnZmllbGRfcHJvZmlsZSddDQppc192YWxpZF9pbnRlcnByZXRhdGlvbiA9IHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk="
        static_longitude_constraints_mapping = {'main-type':'number', 'detail-type':'decimal', 'min':-180, 'max':180 }
        static_longitude_interpretation_map = {'iName':'Longitude', 'iMatchingNames' : ['longitude'],
                                              'iConstraints':static_longitude_constraints_mapping}
        static_longitude_interpretation_map['iScript'] = "ZGVmIHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk6DQogICAgcmV0dXJuIFRydWUNCmZpZWxkX3Byb2ZpbGUgPSBsb2NhbHMoKVsnZmllbGRfcHJvZmlsZSddDQppc192YWxpZF9pbnRlcnByZXRhdGlvbiA9IHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk="
        
        static_altitude_constraints_mapping = {'main-type':'number','detail-type':'integer','min':-1000,'max':100000}
        static_altitude_interpretation_map = {'iName':'Altitude', 'iMatchingNames' : ['altitude'],
                                               'iConstraints':static_altitude_constraints_mapping}
        static_altitude_interpretation_map['iScript'] = "ZGVmIHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk6DQogICAgcmV0dXJuIFRydWUNCmZpZWxkX3Byb2ZpbGUgPSBsb2NhbHMoKVsnZmllbGRfcHJvZmlsZSddDQppc192YWxpZF9pbnRlcnByZXRhdGlvbiA9IHZhbGlkYXRlSW50ZXJwcmV0YXRpb24oZmllbGRfcHJvZmlsZSk="
        
        static_date_time_interpretation = {"iScript" : "aW1wb3J0IGRhdGV0aW1lDQpkZWYgdmFsaWRhdGVJbnRlcnByZXRhdGlvbihmaWVsZF9wcm9maWxlKToNCiAgICBmb3IgZXhhbXBsZV92YWx1ZSBpbiBmaWVsZF9wcm9maWxlWydleGFtcGxlX3ZhbHVlcyddOg0KICAgICAgICB0cnk6DQogICAgICAgICAgICBkYXRldGltZS5kYXRldGltZS5zdHJwdGltZShleGFtcGxlX3ZhbHVlLCAnJW0vJWQvJVkgJUg6JU0nKQ0KICAgICAgICBleGNlcHQgVmFsdWVFcnJvcjoNCiAgICAgICAgICAgIHJldHVybiBGYWxzZQ0KICAgIHJldHVybiBUcnVlDQpmaWVsZF9wcm9maWxlID0gbG9jYWxzKClbJ2ZpZWxkX3Byb2ZpbGUnXQ0KaXNfdmFsaWRfaW50ZXJwcmV0YXRpb24gPSB2YWxpZGF0ZUludGVycHJldGF0aW9uKGZpZWxkX3Byb2ZpbGUp", 
                                           "iMatchingNames" : [ "Date/Time", "date", "time", "dtg" ], "iName" : "Date/Time", 
                                           "iConstraints" : { "max-length" : 17, "detail-type" : "phrase", "min-length" : 13, "main-type" : "string" } }
        
        static_interpretation_list = [static_latitude_interpretation_map, static_longitude_interpretation_map, static_altitude_interpretation_map, static_date_time_interpretation]
        if self.output:
            logging.debug("\nStatic interpretation list: ")
            for interpretation in static_interpretation_list:
                logging.debug("\t" + interpretation['iName'])
            self.output = False
        return static_interpretation_list
        
def mongo_instance(global_vars):
    return global_vars.get('_mongo_data_access_object_', _MongoAccessor())