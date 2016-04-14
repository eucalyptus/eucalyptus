#!/usr/bin/python
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

import yaml
import os
import argparse
import json
import subprocess
import sys
import paramiko
import urllib
import urlparse
import logging

from .configfile import ConfigFile
from .cfg import AdminConfig
from .describeservices import DescribeServices
from .describenodes import DescribeNodes
from .sshconnection import SshConnection
from .constants import *
from eucadmin import debug


sys.excepthook = debug.gen_except_hook(True, True)


# Currently, these three are the only components returned by
# DescribeServices that we traverse.
COMPONENT_MAP = { 'cluster': 'CC',
                  'storage': 'SC',
                  'walrus':  'WS',
                }

LOGLEVELS = ['DEBUG', 'INFO', 'WARN', 'WARNING', 'ERROR', 'CRITICAL']

def read_validator_config(files=[]):
    '''
    Read one or more YAML config files and merge them.
    '''

    def merge(base, overlay):
        if isinstance(base, dict) and isinstance(overlay, dict):
            for k,v in overlay.iteritems():
                if k not in base:
                    base[k] = v
                else:
                    base[k] = merge(base[k],v)
        elif isinstance(base, list) and isinstance(overlay, list):
            # We could use sets here, but this preserves
            # ordering, simply eliminating duplicates
            base.extend([ x for x in overlay if x not in base ])
        return base

    data = {}
    for f in files:
        if os.path.exists(f):
            data = merge(data, yaml.load(open(f, 'r').read()))
    return data


def build_parser():
    parser = argparse.ArgumentParser(description='Eucalyptus cloud validator')
    parser.add_argument('stage', choices=('preinstall', 'postinstall',
                                          'register', 'monitor'),
                        default='monitor',
                        help='Which test stage to run (default: monitor)')
    parser.add_argument('-c', '--config-file',
                        default=DEFAULT_CONFIG_FILE,
                        help='The path to the eucadmin config')
    parser.add_argument('-C', '--component',
                        default='CLC',
                        help='The cloud component role(s) of this system.')
    parser.add_argument('-t', '--traverse', 
                        action='store_true',
                        help='Traverse other components in the cloud (requires ssh credentials)')
    group = parser.add_mutually_exclusive_group()
    group.add_argument('-j', '--json',
                        action='store_true',
                        help='Output JSON-formatted results')
    group.add_argument('-q', '--quiet',
                        action='store_true',
                        help='No output; only a return code')
    parser.add_argument('-l', '--log-level', choices=LOGLEVELS,
                        default='INFO', type=str.upper,
                        help='Log level (default: INFO)')
    parser.add_argument('-s', '--subtask', action='store_true',
                        help=argparse.SUPPRESS)  # subtask of another validator
    return parser

def run_script(scriptPath):
    po = subprocess.Popen([scriptPath], 
                          stdout=subprocess.PIPE,
                          cwd='/')
    stdout = po.communicate()[0]
    return stdout


class Validator(object):
    def __init__(self, stage="monitor", component="CLC", traverse=False, 
                 config_file=DEFAULT_CONFIG_FILE, log_level="INFO", 
                 subtask=False, **kwargs):

        # TODO: allow a component list?
        os.environ['EUCA_ROLES'] = component
        os.environ['TERM'] = 'dumb'
        self.stage = stage
        self.component = component
        self.traverse = traverse
        self.admincfg = AdminConfig(config_file)
        self.subtask = subtask

        if stage != 'preinstall' or (component == "CC" and traverse):
            self.euca_conf = ConfigFile(os.path.join(self.admincfg.eucalyptus,
                                                         EUCA_CONF_FILE))

        self.setupLogging(logging.getLevelName(log_level))

    def setupLogging(self, level=logging.INFO):
        logging.basicConfig()
        self.log = logging.getLogger('eucadmin.validator')
        self.log.setLevel(level)

    @classmethod
    def run(cls):
        parser = build_parser()
        args = parser.parse_args()
        obj = cls(**vars(args))
        result = obj.main()
        if args.json:
            print json.dumps(result, indent=4)
        elif args.quiet:
            sys.exit(Validator.check_nested_result("", result))
        else:
            sys.exit(Validator.check_nested_result("", result, print_output=True))

    def log_nested_result(self, parent, result):
        for key in result.keys():
            if type(result[key]) != dict:
                raise Exception("Error parsing validation data")
            if result[key].has_key('cmd'):
                self.log_nested_result(parent + key + ":",
                                       result[key]['output'])
            elif not result[key].has_key('failed'):
                self.log_nested_result(parent + key + ":",
                                       result[key])
            else:
                for level in LOGLEVELS:
                    if result[key].has_key(level):
                        self.log.log(logging.getLevelName(level.upper()), 
                                     "%s%s: %s" % (parent, key , result[key][level]))

    @staticmethod
    def check_nested_result(parent, result, print_output=False):
        failed = False
        for key in result.keys():
            if type(result[key]) != dict:
                raise Exception("Error parsing validation data")
            if result[key].has_key('cmd'):
                failed |= Validator.check_nested_result(parent + key + ":", 
                                                        result[key]['output'],
                                                        print_output=print_output)
            elif not result[key].has_key('failed'):
                failed |= Validator.check_nested_result(parent + key + ":", 
                                                        result[key],
                                                        print_output=print_output)
            else:                
                if int(result[key]['failed']):
                    if print_output:
                        print "%s%s: %s" % (parent, key, result[key].get('error', "No details provided"))
                    failed = True
        return failed

    def run_remote(self, host, component, stage, traverse=False, dbg=False):
       t=traverse and "-t" or ""
       ssh = SshConnection(host, username="root")
       # NB: euca-validator must be in the PATH and must have a usable
       # configuration on the remote system!
       cmd = 'euca-validator %s -C %s %s -j -s' % (t, COMPONENT_MAP.get(component, component), stage)
       out = ssh.cmd(cmd, timeout=600, get_pty=False)
       try:
           out['output'] = json.loads(out['output'])
           self.log_nested_result("%s-%s:" % (host, component), out['output'])
           return out
       except Exception, e:
           self.log.warn("Remote command failed: %s" % out['output'])
           return {'cmd': cmd, "output": { "euca-validator": { "failed": 1, "error": str(e) } } }
       finally:
           ssh.close()
    
    def main(self):
        self.log.debug("Reading configuration files: %s" % self.admincfg.validator_config_path)
        data = read_validator_config(files=self.admincfg.validator_config_path.split(':'))

        result = {}
        self.log.debug('Script search path is %s' % self.admincfg.validator_script_path)
        for script in data.get(self.stage, {}).get(self.component, []):
            for dirpath in self.admincfg.validator_script_path.split(':'):
                scriptPath = os.path.join(dirpath, script)
                if os.path.exists(scriptPath):
                    self.log.debug('Running script: %s' % scriptPath)
                    return_val = run_script(scriptPath)
                    try:
                        result[script] = json.loads(return_val)
                    except Exception, e:
                        self.log.error("Script %s did not return valid JSON." % scriptPath)
                        self.log.debug("returned data was %s" % return_val)
                        break
                    for level in LOGLEVELS:
                        if result[script].has_key(level):
                            if not self.subtask:
                                self.log.log(logging.getLevelName(level.upper()), 
                                             "%s: %s" % (script, result[script][level]))
                    break
            if not result.has_key(script):
                self.log.error("script %s not found" % script) 

        if self.component == "CLC" and self.traverse:
            # describe-services or get from config file
            ds = DescribeServices(url='http://localhost:8773',)
            data = ds.main()
            hosts = []

            for service in data['euca:DescribeServicesResponseType']['euca:serviceStatuses']:
                hostname = urllib.splitport(urlparse.urlparse(service['euca:serviceId']['euca:uri']).netloc)[0]
                status = service['euca:localState']
                component_type = service['euca:serviceId']['euca:type'] 
                if COMPONENT_MAP.get(component_type):
                    hosts.append((hostname, component_type, status))

            for host, component_type, status in hosts:
                # print "running sub-check: %s - %s - %s" % (host, component_type, args.stage)
                result['-'.join([host, component_type])] = self.run_remote(host, component_type, 
                                                                           self.stage, 
                                                                           traverse=self.traverse,
                                                                           dbg=True)

        elif self.component == "CC" and self.traverse:
            # describe nodes is a CLC call; get from config file
            # dn = DescribeNodes(url='http://localhost:8773',)
            # data = dn.main()
            for host in self.euca_conf["NODES"].split():
                result[host] = self.run_remote(host, "NC", self.stage, dbg=True)

        return result
