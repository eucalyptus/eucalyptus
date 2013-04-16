# Simple configuration for eucadmin
import shlex
import os
from .constants import *

class AdminConfig(object):
    KEYS = [ 'validator_config_path',
             'validator_script_path',
             'eucalyptus',
           ]

    def __init__(self, cfgfile=DEFAULT_CONFIG_FILE):
        self.validator_script_path = os.environ.get('EUCA_VALIDATOR_SCRIPT_PATH',
                                                    DEFAULT_VALIDATOR_SCRIPT_PATH)
        self.validator_config_path = os.environ.get('EUCA_VALIDATOR_CFG_PATH',
                                                    DEFAULT_VALIDATOR_CFG_PATH)
        self.eucalyptus = os.environ.get('EUCALYPTUS', '/')
        if cfgfile:
            self.readConfigFile(cfgfile)

    def readConfigFile(self, cfgfile, error_on_fail=False):
        if not os.path.exists(cfgfile):
            return # raise if error_on_fail
        elif not (os.access(cfgfile, os.R_OK)):
            return # raise if error_on_fail
        for line in open(cfgfile).readlines():
            configline = shlex.split(line)
            if len(configline) != 3 or configline[1] != '=':
                continue # raise if error_on_fail
            k, e, v = configline
            if k not in self.KEYS:
                continue # raise if error_on_fail
            setattr(self, k, v)

    def printConfig(self):
        for k in self.KEYS:
            print "%s = %s" % (k, getattr(self, k))
