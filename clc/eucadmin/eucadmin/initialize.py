# Copyright 2011-2012 Eucalyptus Systems, Inc.
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

from eucadmin.command import Command
import os
import pwd
import sys

InitCommand = """%s/usr/sbin/eucalyptus-cloud -u %s -h %s --initialize"""

DebugInitCommand = """%s/usr/sbin/eucalyptus-cloud -u %s -h %s --initialize --log-level=EXTREME -L console"""

VarRunPath = """%s/var/run/eucalyptus"""

class Initialize(object):
    def __init__(self, config, debug=False):
        self.config = config
        self.debug = debug
        self.euca_user_name = None
        self.euca_user_id = None
        self.euca_user_group_id = None

    def main(self):
        self.euca_user_name = self.config['EUCA_USER']
        if self.euca_user_name == 'root':
            root_data = pwd.getpwnam('root')
            self.euca_user_id = root_data.pw_uid
        else:
            try:
                user_data = pwd.getpwnam(self.euca_user_name)
            except KeyError:
                raise ValueError('Is EUCA_USER defined?')
            self.euca_user_id = user_data.pw_uid
            self.euca_user_group_id = user_data.pw_gid

        db_dir = os.path.join(self.config['EUCALYPTUS'],
                              'var','lib','eucalyptus','db')
        if os.path.exists(os.path.join(db_dir, 'data/ibdata1')):
            sys.exit('Database in %s already exists' % db_dir)
        if self.debug:
            cmd_string = DebugInitCommand % (self.config['EUCALYPTUS'],
                                             self.config['EUCA_USER'],
                                             self.config['EUCALYPTUS'])
        else:
            cmd_string = InitCommand % (self.config['EUCALYPTUS'],
                                        self.config['EUCA_USER'],
                                        self.config['EUCALYPTUS'])
        self.init_scripts()
        if 'CLOUD_OPTS' in self.config:
            cmd_string += ' %s' % self.config['CLOUD_OPTS']
        print 'Initializing Database...'
        cmd = Command(cmd_string)
        if self.debug:
            print '\tStatus=%d' % cmd.status
            print '\tOutput:'
            print cmd.stdout
            print cmd.stderr
        if cmd.status:
            print 'Initialize command failed'
        else:
            print 'Initialize command succeeded'
        return cmd.status

    def init_scripts(self):
        path_string = VarRunPath % self.config['EUCALYPTUS']
        if not os.path.isdir(path_string):
            if self.debug:
                print 'Creating %s' % path_string
            os.mkdir(path_string)
            os.chown(path_string, self.euca_user_id, self.euca_user_group_id)
        initd_dir = os.path.join(
            self.config['EUCALYPTUS'],'etc','eucalyptus','cloud.d','init.d')
        init_files = sorted(os.listdir( initd_dir ))
        for init_file_name in init_files:
            init_file_path = os.path.join(initd_dir, init_file_name)
            if self.debug:
                print 'Evaluating init script %s' % init_file_path
            cmd = Command('bash %s init' % init_file_path)
            if self.debug:
                print '\tStatus=%d' % cmd.status
                print '\tOutput:'
                print cmd.stdout
                print cmd.stderr
            if cmd.status:
                print 'Init script failed %s' % init_file_path
