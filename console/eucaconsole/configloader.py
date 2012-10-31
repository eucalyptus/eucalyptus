import os
import ConfigParser
import logging

# List of config file locations
CONFIG_FILE_LIST = ['console.ini',
                    'eucaconsole/console.ini',
                    '/etc/eucalyptus-console/console.ini']


class Singleton(type):
    def __init__(cls, name, bases, dict):
        super(Singleton, cls).__init__(name, bases, dict)
        cls.instance = None 

    def __call__(cls,*args,**kw):
        if cls.instance is None:
            cls.instance = super(Singleton, cls).__call__(*args, **kw)
        return cls.instance


class ConfigError(Exception):
    pass


class ConfigLoader(object):
    __metaclass__ = Singleton
    def __init__(self):
        self.parser = None
        self.config = None

    def getParser(self, config_file=None):
        if self.parser:
            return self.parser
        self.parser = ConfigParser.ConfigParser()
        if config_file:
            CONFIG_FILE_LIST.insert(0, config_file);
        for config in CONFIG_FILE_LIST:
            if os.path.isfile(config):
                self.parser.read(config)
                self.config = config
                # using config file to configure logger as well
                logging.config.fileConfig(config)
                return self.parser
        raise ConfigError("No valid config file found")

