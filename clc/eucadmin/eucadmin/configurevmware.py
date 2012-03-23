# Copyright (c) 2011, Eucalyptus Systems, Inc.
# All rights reserved.
#
# Redistribution and use of this software in source and binary forms, with or
# without modification, are permitted provided that the following conditions
# are met:
#
#   Redistributions of source code must retain the above
#   copyright notice, this list of conditions and the
#   following disclaimer.
#
#   Redistributions in binary form must reproduce the above
#   copyright notice, this list of conditions and the
#   following disclaimer in the documentation and/or other
#   materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
# Author: Mitch Garnaat mgarnaat@eucalyptus.com
"""
1. get the config from property, put in tmp file
2. if no config, prompt user and create it
3. edit the configuration (optional)
4. validation step
5. put the file back in the property
"""

from boto.roboto.awsqueryrequest import AWSQueryRequest
from boto.roboto.param import Param
from eucadmin.describeproperties import DescribeProperties
from eucadmin.modifypropertyvalue import ModifyPropertyValue
import eucadmin
import os
import sys
import tempfile
import subprocess

VMwareConfigDefault = ''
VMwareConfigPropSuffix = '.vmwarebroker.configxml'
VMwareCommand = 'usr/share/eucalyptus/euca_vmware'

class ConfigureVMware(AWSQueryRequest):

    ServiceClass = eucadmin.EucAdmin
    Description = 'Configure the VMware Broker.'
    Params = [Param(name='euca_home',
                    short_name='e', long_name='euca-home',
                    ptype='string', optional=True,
                    doc='Eucalyptus install dir, default is $EUCALYPTUS'),
              Param(name='edit', short_name='E', long_name='edit',
                    ptype='boolean', optional=True,
                    doc='Edit the current config file using $EDITOR'),
              Param(name='Partition', short_name='P', long_name='partition',
                    ptype='string', optional=True,
                    doc='Partition name for the service')]
    Args = [Param(name='configfile', long_name='configfile',
                  ptype='string', optional=True,
                  doc='The path to the input config file')]

    def get_prop_name(self, part):
        num_found = 0
        name_found = None
        obj = DescribeProperties()
        data = obj.main()
        props = getattr(data, 'euca:properties')
        for prop in props:
            pname = prop['euca:name']
            if (part and pname == (part + VMwareConfigPropSuffix)) or (not part and pname.endswith(VMwareConfigPropSuffix)):
                name_found = pname
                num_found += 1
        if num_found < 1:
            if part:
                print 'Failed to find partition %s.' % part
            else:
                print 'Failed to find any partitions: is VMwareBroker registered?'
            sys.exit(1)
        elif num_found > 1:
            print 'Multiple partitions detected. Please, use the --partition option.'
            sys.exit(1)
        return name_found

    def get_current_value(self, prop_name):
        """
        Reads the current value from the system and stores it to
        a temp file.  Returns the path to the temp file.
        """
        value = ''
        obj = DescribeProperties()
        data = obj.main()
        props = getattr(data, 'euca:properties')
        for prop in props:
            if prop['euca:name'] == prop_name:
                value = prop['euca:value']
                if value == VMwareConfigDefault:
                    value = ''
        print '---Current value is:'
        print value
        return value

    def save_to_file(self, value):
        fd, path = tempfile.mkstemp(suffix='.xml', prefix='euca_vmware')
        os.write(fd, value)
        os.close(fd)
        print '---saving to %s' % path
        return path

    def edit_file(self, path):
        editor = os.environ.get('EDITOR', None)
        if editor:
            cmd_string = '%s %s' % (editor, path)
            print '---running command %s' % cmd_string
            status = subprocess.call(cmd_string, shell=True)
            if status != 0:
                print 'Edit operation failed'
                sys.exit(1)
        else:
            print 'EDITOR not defined'
            sys.exit(1)

    def create_file(self, euca_home, path):
        validate_path = os.path.join(euca_home, VMwareCommand)
        cmd_string = '%s --config %s --prompt --force' % (validate_path, path)
        print '---running command %s' % cmd_string
        status = subprocess.call(cmd_string, shell=True)
        if status != 0:
            print 'An error occured creating the file.'
            print cmd.stderr
            sys.exit(1)

    def validate_file(self, euca_home, path):
        validate_path = os.path.join(euca_home, VMwareCommand)
        cmd_string = '%s --config %s' % (validate_path, path)
        print '---running command %s' % cmd_string
        status = subprocess.call(cmd_string, shell=True)
        if status != 0:
            print 'A validation error occured.'
            print cmd.stderr
            sys.exit(1)

    def save_new_value(self, path, prop):
        print '---saving new property value from %s' % path
        obj = ModifyPropertyValue()
        obj.main(property_from_file='%s=%s' % (prop, path))

    def cli_formatter(self, data):
        pass

    def main(self, **args):
        self.args.update(args)
        self.process_args()
        euca_home = self.request_params.get('euca_home', None)
        cfg_file = self.request_params.get('configfile', None)
        edit_flg = self.request_params.get('edit', False)
        prop = self.get_prop_name(self.request_params.get('Partition',None))

        if not euca_home:
            euca_home = os.environ.get('EUCALYPTUS', None)
            if not euca_home:
                print 'EUCALYPTUS not defined'
                sys.exit(1)
        # if a configfile was passed as an option, validate it and upload it
        if cfg_file:
            self.validate_file(euca_home, cfg_file)
            self.save_new_value(cfg_file, prop)
        # if no configfile was passed and edit_flg is True
        # edit the current value and then upload
        elif edit_flg:
            value = self.get_current_value(prop)
            path = self.save_to_file(value)
            self.edit_file(path)
            self.validate_file(euca_home, path)
            self.save_new_value(path, prop)
        # if no config file
        # and no edit flag, but value exists,
        # then just validate it again.
        # Need to check what the default, unitialized value for the vmware xmlconfig property is.
        elif prop and self.get_current_value(prop) and self.get_current_value(prop) != '<!-- Eucalyptus VMware Broker configuration file -->':
            value = self.get_current_value(prop)
            path = self.save_to_file(value)
            self.validate_file(euca_home, path)
            self.save_new_value(path, prop)
        # if no configfile was passed and edit_flg is False
        # create a new file and then upload it
        else:
            path = self.save_to_file('')
            self.create_file(euca_home, path)
            self.validate_file(euca_home, path)
            self.save_new_value(path, prop)

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()
