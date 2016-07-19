'''
Created on Jun 10, 2016

@author: leegc
'''

import sidekick.checks as checks

class MessageHandler(object):
    allowed_exceptions = ["AttributeError", "EOFError", "FloatingPointError", "GeneratorExit", "ImportError", "IndexError"
                              "KeyError", "KeyboardInterrupt", "MemoryError", "NameError", "NotImplementedError", "OSError"
                              "OverflowError", "RecursionError", "ReferenceError", "RuntimeError", "StopIteration", "StopAsyncIteration",
                              "IndentationError", "TabError", "SystemError", "SystemExit", "TypeError", "UnboundLocalError", "UnicodeError"
                              "UnicodeEncodeError", "UnicodeDecodeError", "UnicodeTranslateError", "ValueError", "ZeroDivisionError",
                              "BlockingIOError", "ChildProcessError", "ConnectionError", "BrokenPipeError", "ConnectionAbortedError",
                              "ConnectionRefusedError", "ConnectionResetError", "FileExistsError", "FileNotFoundError", "InterruptedError",
                              "IsADirectoryError", "NotADirectoryError", "ProcessLookupError", "TimeoutError"]
    suspicious_exceptions = ["PermissionError", "CalledProcessError"]
        

    def handle_name_and_line(self, exception_name, line_no):
        reset_output = False
        message = None
        if exception_name is None:
            return message, False
        try:
            if exception_name in self.allowed_exceptions:
                try:
                    line_number = int(line_no)
                    message = exception_name + " on line " + str(line_number) + "."
                except:
                    message = exception_name
            elif exception_name in self.suspicious_exceptions:
                # handle specific ones differently?
                message = checks.error_messages[checks.SUSPICIOUS]
                reset_output = True
            else:
                message = checks.error_messages[checks.SUSPICIOUS]
                reset_output = True
        except:
            message = checks.error_messages[checks.SUSPICIOUS]
            reset_output = True
        return message, reset_output
        