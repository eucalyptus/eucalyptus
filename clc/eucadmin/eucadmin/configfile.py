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

class ConfigFile(object):

    ChangeCmd = r"""sed -i "s<^[[:blank:]#]*\(%s\).*<\1=\"%s\"<" %s"""
    CommentCmd = r"""sed -i "s<^[[:blank:]]*\($%s.*\)<#\1<" %s"""
    UncommentCmd = r"""sed -i "s<^[#[:blank:]]*\($%s.*\)<\1<" %s"""

    def __init__(self, filepath, test=False):
        self.path = os.path.expanduser(filepath)
        self.path = os.path.expandvars(self.path)
        if not os.access(self.path, os.F_OK):
            raise IOError('The file (%s) does not exist', self.path)
        if not os.access(self.path, os.W_OK):
            raise IOError("You don't have write access to %s" % self.path)
        self.need_backup = True
        self.test = test

    def backup(self):
        if self.need_backup:
            shutil.copyfile(self.path, self.path+'.bak')
            self.need_backup = False

    def change_value(self, var_name, new_value):
        self.backup()
        cmd_str =  self.ChangeCmd % (var_name, new_value, self.path)
        cmd = eucadmin.command.Command(cmd_str, self.test)

    def comment(self, pattern):
        self.backup()
        cmd_str = self.CommentCmd % (pattern, self.path)
        cmd = eucadmin.command.Command(cmd_str, self.test)

    def uncomment(self, pattern):
        self.backup()
        cmd_str = self.UncommentCmd % (pattern, self.path)
        cmd = eucadmin.command.Command(cmd_str, self.test)
        
