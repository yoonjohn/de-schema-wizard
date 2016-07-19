'''
Created on Mar 31, 2016

@author: yoonj1
'''
from flask import Flask, Blueprint
from blueprint.blueprint_rest import bp
from flask_cors import CORS
from root import core


class RootREST:

    def __init__(self, host, run_flask, port):
        self.host = host
        self.port = port
        self.run_flask = run_flask
        self.app = Flask(__name__)
        CORS(self.app,
             resources={
                 r'/*': {
                     'origins': '*',
                     'headers': ['Content-Type']
                 }
             }
        )
        #blueprintRest = BlueprintRest()
        self.app.register_blueprint(bp, url_prefix='/blueprint')
        #self.app.register_blueprint(Blueprint('blueprint', __name__), url_prefix='/blueprint')

        # Root service.
        @self.app.route('/')
        def landing():
            return core.landing_message()

        # Run Flask.
        if self.run_flask:
            self.app.run(host=self.host, port=self.port)


def run_engine(host, port=5000):
    RootREST(host, True, port)
    if __name__ == '__main__':
        run_engine('localhost')