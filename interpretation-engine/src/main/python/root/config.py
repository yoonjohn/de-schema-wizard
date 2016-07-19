'''
Created on Jun 24, 2016

@author: leegc
'''

import os
import logging

SW_CONFIG_PROPERTIES = "SW_CONFIG_PROPERTIES" 
DOCKER_COMPOSE_MONGODB_HOST = "DOCKER_COMPOSE_MONGODB_HOST"
DOCKER_COMPOSE_MONGODB_PORT = "DOCKER_COMPOSE_MONGODB_PORT"

class IEConfig(object):
    
    def __init__(self):
        try:
            logging.info("")
            self.mongo_host = os.environ[DOCKER_COMPOSE_MONGODB_HOST]
            self.mongo_port = os.environ[DOCKER_COMPOSE_MONGODB_PORT]
            # TODO
            # Hardcoded default to 5000. Needs to be changed for beta-2 release
            self.port_num = "5000"
            logging.info("Initializing Mongo connections using Docker-Compose method.")
            logging.info("\tMongo host - " + str(self.mongo_host) + ":" + str(self.mongo_port))
        except Exception:
            logging.warn("No environmental variable for the Mongo Database was specified. Attempting to use the configuration file.")
        
            does_user_file_exist = os.path.isfile("~/build.properties")
            if does_user_file_exist:
                file_path = "~/build.properties"
            elif "SW_CONFIG_PROPERTIES" in os.environ:
                file_path = os.environ[SW_CONFIG_PROPERTIES]
            else:
                file_path = os.path.join(os.path.abspath(__file__), os.pardir, os.pardir) + os.sep + "build.properties"
            logging.info("Got path " + os.path.abspath(file_path))
            normalized_path = os.path.normpath(file_path)
            logging.info("Normalizing path to " + os.path.abspath(file_path))
            logging.info("Using file " + os.path.abspath(normalized_path) + " as configuration file.")
            properties = {}
            try:
                with open(normalized_path, 'r') as config:
                    content = config.readlines()
                    for line in content:
                        ind = line.find('=')
                        if ind >= 0:
                            properties[line[:ind]] = line[ind+1:]
                url = properties['ie.url']
                ind = url.rfind(':')
                self.port_num = url[ind+1:].strip()
                #self.mongo_host = properties['ie.mongo.host'].strip()
                #self.mongo_port = properties['ie.mongo.portnum'].strip()
            except Exception as exc:
                logging.exception(exc)
                logging.error("There was an error reading from the properties file.  Defaulting to http://localhost:5000")
                self.port_num = "5000"
                #self.mongo_host = "localhost"
                #self.mongo_port = "27017"
                
    def __str__(self):
        return "Config - \n\tIE port - " + str(self.port_num) + "\n\tMongo host - " + str(self.mongo_host) + ":" + str(self.mongo_port) 

config = IEConfig()