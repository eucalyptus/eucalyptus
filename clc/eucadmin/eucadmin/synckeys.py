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
import sys
import pwd
import socket
import shutil
from boto.utils import mklist
from eucadmin.command import Command

SyncMethods = ['local', 'rsync', 'scp', 'smb']

class SyncKeys(object):

    def __init__(self, src_dirs, dst_dir, remote_host, file_names,
                 use_rsync=True, use_scp=True, use_smb=False,
                 remote_user='root'):
        self.src_dirs = mklist(src_dirs)
        self.src_dirs = [os.path.expanduser(sd) for sd in self.src_dirs]
        self.src_dirs = [os.path.expandvars(sd) for sd in self.src_dirs]
        self.dst_dir = dst_dir
        self.remote_host = remote_host
        self.remote_user = remote_user
        self.file_names = mklist(file_names)
        self.use_rsync = use_rsync
        self.use_scp = use_scp
        self.use_smb = use_smb
        self.files = []
        self.is_local = self.check_local()

    def error(self, msg):
        print 'Error: %s' % msg
        sys.exit(1)

    def warning(self, msg):
        print 'Warning: %s' % msg

    def get_file_list(self):
        found = []
        for fn in self.file_names:
            for sd in self.src_dirs:
                path = os.path.join(sd, fn)
                if os.path.isfile(path):
                    self.files.append(path)
                    found.append(fn)
        not_found = [fn for fn in self.file_names if fn not in found]
        if not_found:
            self.warning("Can't find %s in %s" % (not_found, self.src_dirs))

    def check_local(self):
        if self.remote_host == '127.0.0.1':
            self.is_remote = True
        elif self.remote_host == 'localhost':
            self.is_remote = True
        elif self.remote_host == socket.gethostname():
            self.is_remote = True

    def sync_local(self):
        for fn in self.files:
            if not os.path.isfile(fn):
                self.error('cannot find cluster credentials')
            else:
                try:
                    shutil.copy2(fn, self.dst_dir)
                except:
                    self.error('cannot copy %s to %s' % (fn, self.dst_dir))

    def sync_rsync(self):
        if not self.use_rsync:
            return
        print
        print 'Trying rsync to sync keys with %s' % self.remote_host
        cmd = 'rsync -az '
        cmd += ' '.join(self.files)
        cmd += ' %s@%s:%s' % (self.remote_user, self.remote_host, self.dst_dir)
        cmd = Command(cmd)
        if cmd.status == 0:
            print 'done'
            return True
        else:
            print 'failed.'
            return False

    def get_euca_user(self):
        euca_user = os.environ.get('EUCA_USER', None)
        if not euca_user:
            try:
                pwd.getpwnam('eucalyptus')
                euca_user = 'eucalyptus'
            except KeyError:
                self.error('EUCA_USER is not defined!')
        return euca_user

    def sync_scp(self):
        euca_user = self.get_euca_user()
        print
        print 'Trying scp to sync keys with %s (user %s)' % (self.remote_host,
                                                             euca_user)
        # TODO: handle sudo password prompt
        cmd = 'sudo -u %s scp ' % euca_user
        cmd += ' '.join(self.files)
        cmd += ' %s@%s:%s' % (euca_user, self.remote_host, self.dst_dir)
        cmd = Command(cmd)
        if cmd.status == 0:
            print 'done'
            return True
        else:
            print 'failed.'
            return False

    def sync(self):
        self.get_file_list()
        if self.check_local():
            self.sync_local()
            return True
        else:
            if self.use_rsync and self.sync_rsync():
                return True
            if self.use_scp and self.sync_scp():
                return True
            return False
            
        
