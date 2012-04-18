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
import shutil
import eucadmin.command

class ConfigFile(dict):

    ChangeCmd = r"""sed -i "s<^[[:blank:]#]*\(%s\).*<\1=\"%s\"<" %s"""
    CommentCmd = r"""sed -i "s<^[[:blank:]]*\(%s.*\)<#\1<" %s"""
    UncommentCmd = r"""sed -i "s<^[#[:blank:]]*\(%s.*\)<\1<" %s"""

    def __init__(self, filepath, test=False):
        dict.__init__(self)
        self.test = test
        self._save_to_file = False
        self.path = os.path.expanduser(filepath)
        self.path = os.path.expandvars(self.path)
        self.need_backup = True
        self._read_config_data()
        self._save_to_file = True

    def __setitem__(self, key, value):
        if self._save_to_file:
            self._backup()
            cmd_str =  self.ChangeCmd % (key, value, self.path)
            cmd = eucadmin.command.Command(cmd_str)
        dict.__setitem__(self, key, value)

    def _read_config_data(self, path=None):
        if not path:
            path = self.path

        if not os.access(path, os.F_OK):
            raise IOError('The file (%s) does not exist' % path)
        if path == self.path and not os.access(path, os.W_OK):
            raise IOError("You don't have write access to %s" % path)

        fp = open(path)
        for line in fp.readlines():
            if not line.startswith('#'):
                t = line.split('=', 1)
                if len(t) == 2:
                    name = t[0]
                    # Remove comments at end of line
                    value = t[1].split('#', 1)[0].strip('"\n ')
                    self[name] = value
        fp.close()
        
    def _backup(self):
        if self.need_backup:
            shutil.copyfile(self.path, self.path+'.bak')
            self.need_backup = False

    def comment(self, pattern):
        self._backup()
        cmd_str = self.CommentCmd % (pattern, self.path)
        cmd = eucadmin.command.Command(cmd_str)
        if pattern in self:
            del self[pattern]

    def uncomment(self, pattern):
        self._backup()
        cmd_str = self.UncommentCmd % (pattern, self.path)
        cmd = eucadmin.command.Command(cmd_str)
        self['pattern'] = ''
        
    def mergefile(self, oldconfig):
        old_version = ''
        old_version_file = os.path.join(os.path.dirname(oldconfig), 'eucalyptus-version')
        if os.access(old_version_file, os.F_OK):
            old_version = open(old_version_file).readlines()[0].strip()
      
        if old_version.startswith('2') or old_version.startswith('eee-2'):
            self.comment('DISABLE_ISCSI')
            self._read_config_data(oldconfig)
            if not self.has_key('DISABLE_ISCSI'):
                self['DISABLE_ISCSI'] = 'Y'

            deprecatedopts = [ '--walrus-host', '--cloud-host',
                               '--remote-storage', '--remote-walrus',
                               '--remote-cloud', '--disable-cloud',
                               '--disable-dns', '--disable-walrus',
                               '--disable-storage', '--disable-vmwarebroker']
            self['CLOUD_OPTS'] = " ".join([ x for x in self['CLOUD_OPTS'].split()
                                            if x not in deprecatedopts ])
            
            if self.has_key('MAX_DISK'):
                self['MAX_DISK'] = str(int(self['MAX_DISK']) / 1024)
        else:
            self._read_config_data(oldconfig)

        # I can't think of any case where EUCALYPTUS setting should not be
        # self.path minus /etc/eucalyptus/eucalyptus.conf
        self['EUCALYPTUS'] = "/".join(self.path.split("/")[:-3]) or "/"

