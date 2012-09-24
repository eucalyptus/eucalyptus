import os
import ConfigParser

# List of config file locations
CONFIG_FILE_LIST = ['console.ini',
                    'server/console.ini',
                    '/etc/eucalyptus-ui/console.ini']

class ConfigLoader:
    @staticmethod
    def getParser(config_file=None):
        parser = ConfigParser.ConfigParser()
        if config_file:
            CONFIG_FILE_LIST.insert(0, config_file);
        for config in CONFIG_FILE_LIST:
            if os.path.isfile(config):
                parser.read(config)
                return parser

