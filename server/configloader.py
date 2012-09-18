import os
import sys
import ConfigParser

# List of config file locations
CONFIG_FILE_LIST = ['console.ini',
                    'server/console.ini',
                    '/etc/eucalyptus-ui/console.ini']

class ConfigLoader:
    @staticmethod
    def getParser():
        parser = ConfigParser.ConfigParser()
        if '-c' in sys.argv:
            CONFIG_FILE_LIST.insert(0, sys.argv[sys.argv.index('-c') + 1]);
        for config in CONFIG_FILE_LIST:
            if os.path.isfile(config):
                parser.read(config)
                return parser

