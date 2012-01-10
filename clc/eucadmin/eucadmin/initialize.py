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

from eucadmin.command import Command
import os.path
import sys

InitCommand = """%s/usr/sbin/eucalyptus-cloud -u %s -h %s --initialize"""

DebugInitCommand = """%s/usr/sbin/eucalyptus-cloud -u %s -h %s --initialize --log-level=EXTREME -L console"""

class Initialize(object):

    def __init__(self, config, debug=False):
        self.config = config
        self.debug = debug

    def main(self):
        db_dir = os.path.join(self.config['EUCALYPTUS'],
                              'var/lib/eucalyptus/db')
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
