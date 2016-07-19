from engine import script_executor
from dev import development_trusted_runner
from prod import linux_untrusted_runner
import unittest
import logging

class SecureScriptExecutor(unittest.TestCase):
    
    def test_init(self):
        pass
        #executor = script_executor.ScriptExecutor()
        #assert executor.script_executor.__class__.__name__ == linux_untrusted_runner.SecureScriptRunner.__name__