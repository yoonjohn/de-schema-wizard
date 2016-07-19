'''
Created on Apr 12, 2016

@author: leegc
'''
import unittest
import logging
from engine.constraints import MinimumConstraint
from engine.constraints import MaximumConstraint
from engine.constraints import EqualityConstraint

class ConstraintTest(unittest.TestCase):
    def testMinimumConstraint(self):
        minimum_constraint = MinimumConstraint("min", 10)
        test_value1 = 5
        test_value2 = 15
        a1 = minimum_constraint.validate(test_value1)
        a2 = minimum_constraint.validate(test_value2)
        if a1:
            logging.error("Minimum set to 10, but 5 was valid.")
        if not a2:
            logging.error("Minimum set to 10, and 15 was invalid.")
        assert not a1
        assert a2
    
    def testMaximumConstraint(self):
        maximum_constraint = MaximumConstraint("max", 10)
        test_value1 = 5
        test_value2 = 15
        a1 = maximum_constraint.validate(test_value1)
        a2 = maximum_constraint.validate(test_value2)
        if not a1:
            logging.error("Maximum set to 10, and 5 was invalid.")
        if a2:
            logging.error("Maximum set to 10, and 15 was valid.")
        assert a1
        assert not a2
    
    def testEquialityConstraint(self):
        equality_constraint = EqualityConstraint("main-type", "string")
        test_value1 = "string"
        test_value2 = "number"
        a1 = equality_constraint.validate(test_value1)
        a2 = equality_constraint.validate(test_value2)
        if not a1:
            logging.error("Main types \'string\' and \'string\' not determined to be equal.")
        if a2:
            logging.error("Main types \'string\' and \'number\' determined to be equal.")
        assert a1
        assert not a2
    
    def testPatternConstraint(self):
        pass
    