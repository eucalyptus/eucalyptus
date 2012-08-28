import os
import ConfigParser

# List of config file locations
CONFIG_FILE_LIST = ['eui.ini',
                    'server/eui.ini',
                    '/etc/eucalyptus-ui/eui.ini']

class ConfigLoader:
    @staticmethod
    def getParser():
        parser = ConfigParser.ConfigParser()
        for config in CONFIG_FILE_LIST:
            if os.path.isfile(config):
                parser.read(config)
                return parser

