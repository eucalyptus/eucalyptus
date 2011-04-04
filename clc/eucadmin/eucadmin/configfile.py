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
    CommentCmd = r"""sed -i "s<^[[:blank:]]*\($%s.*\)<#\1<" %s"""
    UncommentCmd = r"""sed -i "s<^[#[:blank:]]*\($%s.*\)<\1<" %s"""

    def __init__(self, filepath, test=False):
        dict.__init__(self)
        self.test = test
        self._save_to_file = False
        self.path = os.path.expanduser(filepath)
        self.path = os.path.expandvars(self.path)
        if not os.access(self.path, os.F_OK):
            raise IOError('The file (%s) does not exist' % self.path)
        if not os.access(self.path, os.W_OK):
            raise IOError("You don't have write access to %s" % self.path)
        self.need_backup = True
        self._read_config_data()
        self._save_to_file = True

    def __setitem__(self, key, value):
        if self._save_to_file:
            self._backup()
            cmd_str =  self.ChangeCmd % (key, value, self.path)
            cmd = eucadmin.command.Command(cmd_str, self.test)
        dict.__setitem__(self, key, value)

    def _read_config_data(self):
        fp = open(self.path)
        for line in fp.readlines():
            if not line.startswith('#'):
                t = line.split('=')
                if len(t) == 2:
                    self[t[0]] = t[1].strip('"\n ')
        fp.close()
        
    def _backup(self):
        if self.need_backup:
            shutil.copyfile(self.path, self.path+'.bak')
            self.need_backup = False

    def comment(self, pattern):
        self.backup()
        cmd_str = self.CommentCmd % (pattern, self.path)
        cmd = eucadmin.command.Command(cmd_str, self.test)
        if pattern in self:
            del self[pattern]

    def uncomment(self, pattern):
        self.backup()
        cmd_str = self.UncommentCmd % (pattern, self.path)
        cmd = eucadmin.command.Command(cmd_str, self.test)
        self['pattern'] = ''
        
