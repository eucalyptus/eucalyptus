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

import os.path
import pwd
import shutil
import socket
import subprocess
import sys

class SyncKeys(object):
    def __init__(self, src_files, dst_dir, remote_host,
                 use_rsync=True, use_scp=True, remote_user='root'):
        self.src_files   = src_files
        self.dst_dir     = dst_dir
        self.remote_host = remote_host
        self.remote_user = remote_user
        self.use_rsync   = use_rsync
        self.use_scp     = use_scp

    def warn(self, msg):
        print >> sys.stderr, 'warning:', msg

    def error(self, msg):
        print >> sys.stderr, 'error:', msg

    def get_extant_src_files(self):
        found = []
        for src_file in self.src_files:
            if os.path.isfile(src_file):
                found.append(src_file)
            else:
                self.warn('unable to sync file %s because it does not exist; '
                          'services may have trouble communicating' % src_file)
        return found

    def can_use_local_sync(self):
        if self.remote_host in ('127.0.0.1', 'localhost',
                                socket.gethostname()):
            return True
        return False

    def sync_local(self, src_files):
        for src_file in src_files:
            try:
                shutil.copy2(src_file, self.dst_dir)
            except Exception as exc:
                self.error('failed to copy %s to %s: %s' %
                           (src_file, self.dst_dir, str(exc)))
                return False
        return True

    def sync_with_rsync(self, src_files):
        cmd = ['rsync', '-az'] + src_files
        cmd.append('%s@%s:%s' % (self.remote_user, self.remote_host,
                                 self.dst_dir))

        # Check if we need to elevate privileges
        if any(not os.access(src_file, os.R_OK) for src_file in src_files):
            print 'elevating privileges with sudo'
            cmd = ['sudo', '-u', self.get_euca_user()] + cmd

        try:
            subprocess.check_call(cmd)
            return True
        except subprocess.CalledProcessError as err:
            self.error('key sync using rsync failed: %s' % str(err))
            return False

    def get_euca_user(self):
        euca_user = os.environ.get('EUCA_USER', None)
        if not euca_user:
            try:
                pwd.getpwnam('eucalyptus')
                euca_user = 'eucalyptus'
            except KeyError:
                self.error('EUCA_USER is not defined')
                sys.exit(1)
        return euca_user

    def sync_with_scp(self, src_files):
        cmd = ['scp'] + src_files
        cmd.append('%s@%s:%s' % (self.remote_user, self.remote_host,
                                 self.dst_dir))

        # Check if we need to elevate privileges
        if any(not os.access(src_file, os.R_OK) for src_file in src_files):
            print 'elevating privileges with sudo'
            cmd = ['sudo', '-u', self.get_euca_user()] + cmd

        try:
            subprocess.check_call(cmd)
            return True
        except subprocess.CalledProcessError as err:
            self.error('key sync using scp failed: %s' % str(err))
            return False

    def sync_keys(self):
        src_files = self.get_extant_src_files()
        success = False
        if self.can_use_local_sync():
            success = self.sync_local(src_files)
        if not success and self.use_rsync:
            success = self.sync_with_rsync(src_files)
        if not success and self.use_scp:
            success = self.sync_with_scp(src_files)
        return success
