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
from eucadmin.utils import chown_recursive, chmod_recursive

RootWrapPath = 	'usr/lib/eucalyptus/euca_rootwrap'
MountWrapPath = 'usr/lib/eucalyptus/euca_mountwrap'

MakeDirs = ['var/lib/eucalyptus/dynserv/data',
            'var/lib/eucalyptus/db',
            'var/lib/eucalyptus/keys',
            'var/lib/eucalyptus/CC']

ChownPaths = [('var/lib/eucalyptus', True),
              ('var/log/eucalyptus', True),
              ('var/run/eucalyptus', True),
              ('etc/eucalyptus/eucalyptus.conf', False),
              ('etc/eucalyptus', False),
              ('var/lib/eucalyptus/dynserv', True),
              ('var/lib/eucalyptus/db', False),
              ('var/lib/eucalyptus/keys', False),
              ('var/lib/eucalyptus/CC', False)]

ChmodPaths = [('var/lib/eucalyptus/dynserv', 0700, True),
              ('var/lib/eucalyptus/db', 0700, False),
              ('var/lib/eucalyptus/keys', 0700, False),
              ('var/lib/eucalyptus/CC', 0700, False)]

class EucaSetup(object):

    def __init__(self, config):
        self.config = config
        self.euca_user_name = None
        self.euca_user_id = None
        self.euca_user_group_id = None

    def chown_paths(self):
        for path,recurse_flg in ChownPaths:
            path = os.path.join(self.config['EUCALYPTUS'], path)
            if recurse_flg:
                chown_recursive(path, self.euca_user_id,
                                self.euca_user_group_id)
            else:
                os.chown(path, self.euca_user_id, self.euca_user_group_id)

    def chmod_paths(self):
        for path,mod,recurse_flg in ChmodPaths:
            path = os.path.join(self.config['EUCALYPTUS'], path)
            if recurse_flg:
                chmod_recursive(path, mod)
            else:
                os.chmod(path, mod)

    def make_dirs(self):
        for dir_name in MakeDirs:
            path = os.path.join(self.config['EUCALYPTUS'], dir_name)
            if not os.path.isdir(path):
                os.makedirs(path)

    def main(self):
        # check for existence of rootwrap
        rootwrap = os.path.join(self.config['EUCALYPTUS'], RootWrapPath)
        if not os.path.isfile(rootwrap):
            raise IOError('Cannot find %s or not executable' % rootwrap)
        # check for existence of mountwrap
        mountwrap = os.path.join(self.config['EUCALYPTUS'], MountWrapPath)
        if not os.path.isfile(mountwrap):
            raise IOError('Cannot find %s or not executable' % mountwrap)
        self.euca_user_name = self.config['EUCA_USER']
        root_data = pwd.getpwnam('root')
        if self.euca_user_name == 'root':
            self.euca_user_id = root_data.pw_uid
        else:
            try:
                user_data = pwd.getpwnam(self.euca_user_name)
            except KeyError:
                raise ValueError('Is EUCA_USER defined?')
            self.euca_user_id = user_data.pw_uid
            self.euca_user_group_id = user_data.pw_gid
            os.chown(rootwrap, root_data.pw_uid, self.euca_user_group_id)
            os.chmod(rootwrap, 04750)
            os.chown(mountwrap, root_data.pw_uid, self.euca_user_group_id)
            os.chmod(mountwrap, 04750)
        self.instance_path = self.config['INSTANCE_PATH']
        if self.instance_path and self.instance_path != 'not_configured':
            if not os.path.isdir(self.instance_path):
                os.makedirs(self.instance_path)
            os.chown(self.instance_path, self.euca_user_id,
                     self.euca_user_group_id)
        self.make_dirs()
        self.chown_paths()
        self.chmod_paths()
