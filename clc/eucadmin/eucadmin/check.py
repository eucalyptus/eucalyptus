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

import os
import pwd
from eucadmin.command import Command

RootWrapPath = 'usr/lib/eucalyptus/euca_rootwrap'

class Check(object):

    def __init__(self, config, service):
        self.config = config
        self.service = service
        self.euca_user_name = None
        self.euca_user_id = None
        self.euca_user_group_id = None
        self.messages = []
        self.status = 0

    def main(self):
        self.check_rootwrap()
        self.check_eucauser()
        self.check_nc()
        self.check_dirs()
        self.check_vmware()

    def common(self):
        """
        This method performs a subset of the check operations
        that are common to all services.
        """
        self.check_rootwrap()
        self.check_eucauser()
        self.check_dirs()

    def check_rootwrap(self):
        # check for existence of rootwrap
        rootwrap = os.path.join(self.config['EUCALYPTUS'], RootWrapPath)
        if not os.path.isfile(rootwrap):
            self.messages.append('Cannot find euca_rootwrap(%s)' % rootwrap)
            self.status = 1
        elif not os.access(rootwrap, os.X_OK):
            self.messages.append('euca_rootwrap(%s) not executable' % rootwrap)
            self.status = 1

    def check_eucauser(self):
        self.euca_user_name = self.config['EUCA_USER']
        root_data = pwd.getpwnam('root')
        if self.euca_user_name is None or self.euca_user_name == 'root':
            self.messages.append('Running eucalyptus as root')
            self.euca_user_id = root_data.pw_uid
            self.euca_group_id = root_data.pw_gid
        else:
            try:
                user_data = pwd.getpwnam(self.euca_user_name)
                self.euca_user_id = user_data.pw_uid
                self.euca_user_group_id = user_data.pw_gid
            except KeyError:
                msg = 'User %s does not exist' % self.euca_user_name
                self.messages.append(msg)
                self.status = 1

    def check_nc(self):
        if self.service == 'nc':
            self.instance_path = self.config['INSTANCE_PATH']
            if self.instance_path is None:
                self.messages.append('INSTANCE_PATH is not defined')
                self.status = 1
            elif not os.path.isdir(self.instance_path):
                msg = '%s does not exist: ' % self.instance_path
                msg += 'did you run euca_conf --setup?'
                self.messages.append(msg)
                self.status = 1

    def check_dirs(self):
        d = os.path.join(self.config['EUCALYPTUS'], 'var/run/eucalyptus')
        if not os.path.isdir(d):
            try:
                os.mkdir(d)
                os.chown(d, self.euca_user_id, self.euca_user_group_id)
            except OSError:
                self.messages.append('Unable to make directory: %s' % d)
                self.status = 1
        if self.service == 'cc':
            d = os.path.join(self.config['EUCALYPTUS'],
                             'var/run/eucalyptus/net')
            if not os.path.isdir(d):
                try:
                    os.mkdir(d)
                    os.chown(d, self.euca_user_id, self.euca_user_group_id)
                except OSError:
                    self.messages.append('Unable to make directory: %s' % d)
                    self.status = 1

    def check_vmware(self):
        if self.service == 'vmware':
            s = os.path.join(self.config['EUCALYPTUS'],
                             'usr/share/eucalyptus/euca_vmware')
            s += ' --config '
            s += os.path.join(self.config['EUCALYPTUS'],
                              'etc/eucalyptus/vmware_conf.xml')
            cmd = Command(s)
            if cmd.status != 0:
                self.messages.append(cmd.stderr)
                self.status = cmd.status

                                          
