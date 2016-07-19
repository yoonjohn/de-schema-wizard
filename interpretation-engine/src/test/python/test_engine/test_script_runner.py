'''
Created on Apr 12, 2016

@author: leegc
'''
from dev.development_trusted_runner import InsecureScriptRunner
from engine.validation_template import return_template_encoded,\
    return_template_as_text
from engine.script_executor import ScriptExecutor
import logging
import base64
import unittest
from engine import script_executor

class Test(unittest.TestCase):
    
    def test_return_false(self):
        set_is_valid_interpretation_false = base64.b64encode(b"is_valid_interpretation = False")
        script_executor = InsecureScriptRunner()
        assertion = script_executor.execute(set_is_valid_interpretation_false, None).result == False
        if assertion:
            logging.info("Script runner returned expected value (False).")
        else:
            logging.error("Script runner return True.")
        assert assertion
    
    def test_return_true(self):
        set_is_valid_interpretation_true = base64.b64encode(b"is_valid_interpretation = True")
        script_executor = InsecureScriptRunner()
        assertion = script_executor.execute(set_is_valid_interpretation_true, None).result == True
        if assertion:
            logging.info("Script runner returned expected value (True).")
        else:
            logging.error("Script runner returned False.")
        assert assertion
    
    @unittest.skip
    def test_default_script_returns_true(self):
        default_script = base64.b64encode(return_template_as_text())
        script_executor = InsecureScriptRunner()
        assertion = script_executor.execute(default_script, None).result == True
        if assertion:
            logging.info("Default returned expected value (True).")
        else:
            logging.error("Default script returned False.")
        assert assertion
        
    def test_script_runner_assertion_error(self):
        failed_assertion_script = base64.b64encode(b"assert False")
        script_executor = InsecureScriptRunner()
        assertion = script_executor.execute(failed_assertion_script, None).result == False
        if assertion:
            logging.info("Failed assertion script returned expected value (False).")
        else:
            logging.error("Failed assertion script returned True.")
        assert assertion
        
    def dont_crach_on_invalid_chars(self):
        s = "ä"
        invalid_character_script = base64.b64encode(s, altchars="ä")
        script_executor = ScriptExecutor()
        try:
            assertion = script_executor.execute(invalid_character_script, None).result == False
            try:
                assert assertion
            except AssertionError:
                logging.error("Exception was not thrown by invalid char, but result was not False.")
                assert False
        except Exception:
            logging.error("Exception was thrown was executor was given an invalid character.")
            assert False