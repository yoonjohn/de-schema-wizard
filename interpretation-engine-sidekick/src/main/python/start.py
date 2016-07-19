'''
Created on May 26, 2016

@author: leegc
'''

import os
import logging
import logging.config
from sidekick.sidekick import Sidekick

if __name__ == '__main__':
    parent_dir = os.path.abspath(os.path.join(__file__, os.pardir))
    logging.config.fileConfig(parent_dir + os.sep + 'logging.conf')
    sidekick = Sidekick()
    sidekick.join()