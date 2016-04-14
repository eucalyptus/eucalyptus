# Simple configuration for eucadmin
#
# Copyright 2013 Eucalyptus Systems, Inc.
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the following
# conditions are met:
#
#   Redistributions of source code must retain the above copyright notice,
#   this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in the
#   documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import os
import shlex
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
