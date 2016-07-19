'''
Created on Mar 31, 2016

@author: yoonj1
'''
from root import rest as rest_engine
from root import config
import logging
import logging.config
import os

if __name__ == '__main__':
    parent_dir = os.path.abspath(os.path.join(__file__, os.pardir))
    logging.config.fileConfig(parent_dir + os.sep + 'logging.conf')
    logging.info("Starting Interpretation Engine.")
    logging.info(config)
    rest_engine.run_engine('0.0.0.0', config.IEConfig().port_num)