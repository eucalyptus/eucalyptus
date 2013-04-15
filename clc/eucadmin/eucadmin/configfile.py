# Copyright 2011-2013 Eucalyptus Systems, Inc.
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

import os
import re
import shutil
from .constants import EUCA_CONF_FILE

keyValString = '^(\S+)\s*=\s*"(.*)"\s*(#.*)?$'
commentedKeyValString = '^#\s+(%s)$' % keyValString[1:-1]
kvRE = re.compile(keyValString)
ckvRE = re.compile(commentedKeyValString)

class ConfigFile(dict):

    @staticmethod
    def get_file_path():
        """
        find eucalyptus.conf
        """
        return os.path.join(os.environ.get('EUCALYPTUS', '/'),
                            EUCA_CONF_FILE)

    def __init__(self, filepath, test=False, autosave=True):
        dict.__init__(self)
        self.test = test
        self.autosave = False
        self._save_to_file = False
        self.path = os.path.expanduser(filepath)
        self.path = os.path.expandvars(self.path)
        self.need_backup = True
        self._ordered_key_list = []
        self._read_config_data()
        self._dirty = False
        self._save_to_file = True
        self.autosave = autosave

    def update(self, newDict):
        dict.update(self, newDict)
        self.setDirty()

    def __setitem__(self, key, value):
        dict.__setitem__(self, key, value)
        self.setDirty()

    def __delitem__(self, key):
        dict.__delitem__(self, key)
        self.setDirty()

    def setDirty(self):
        self._dirty = True
        if self.autosave:
            self.save()

    def _read_config_data(self, path=None):
        """
        Read lines from a config file into a list and populate the dict.
        """

        if not path:
            path = self.path

        if not os.access(path, os.F_OK):
            raise IOError('The file (%s) does not exist' % path)

        fp = open(path)
        self._content = [ x.strip() for x in fp.readlines() ]
        fp.close()
        self.reset()

    def reset(self):
        """
        Reset the dictionary content to exactly what was in the config
        file the last time it was read.
        """

        self.clear()
        self._ordered_key_list = []
        for line in self._content:
            if not line.startswith('#'):
                t = line.split('=', 1)
                if len(t) == 2:
                    name = t[0]
                    # Remove comments at end of line
                    value = t[1].split('#', 1)[0].strip('"\n ')
                    self[name] = value
                    self._ordered_key_list.append(name)

    def _backup(self):
        if self.need_backup:
            shutil.copyfile(self.path, self.path+'.bak')
            self.need_backup = False

    def mergefile(self, oldconfig):
        """
        Read key/value pairs from a different config file and merge them
        into this dictionary.  This was used for upgrades, but would also
        be useful if we ever support included configs or a '.d' directory
        """

        for line in open(oldconfig, 'r').readlines():
            if not line.startswith('#'):
                t = line.split('=', 1)
                if len(t) == 2:
                    name = t[0]
                    # Remove comments at end of line
                    value = t[1].split('#', 1)[0].strip('"\n ')
                    self[name] = value

        # I can't think of any case where EUCALYPTUS setting should not be
        # self.path minus /etc/eucalyptus/eucalyptus.conf
        if not self.get('EUCALYPTUS', None):
            self['EUCALYPTUS'] = "/".join(self.path.split("/")[:-3]) or "/"

        if self.autosave:
            self.save()

    def save(self):
        """
        Write the current dictionary back to the config file.  This function
        is now very sensitive to duplicate keys, and will try to preserve
        key/value pairs in comments if there is already an uncommented line
        containing the same key.
        """

        unseenKeys = self.keys()

        if not self._dirty:
            return

        if not os.access(self.path, os.W_OK):
            raise IOError("You don't have write access to %s" % self.path)

        self._backup()

        idx = 0
        while idx < len(self._content):
            line = self._content[idx]
            if line.startswith('#'):
                m = ckvRE.match(line)
                if m:
                   match = m.groups()
                   if self.has_key(match[1]) and \
                      match[1] not in self._ordered_key_list and \
                      match[1] in unseenKeys:
                       self._content[idx] = match[0].replace(match[2], self[match[1]])
                       unseenKeys.pop(unseenKeys.index(match[1]))
            elif re.match("^\s*$", line):
                pass
            else:
                m = kvRE.match(line)
                if not m:
                    # There's an invalid line here.  Should we remove it?
                    continue
                match = m.groups()
                if not self.has_key(match[0]):
                    # Comment out deleted values
                    self._content[idx] = '# ' + line
                elif self._ordered_key_list.count(match[0]) > 1:
                    # Comment out duplicate values
                    self._content[idx] = '# ' + line
                    self._ordered_key_list.pop(self._ordered_key_list.index(match[0]))
                else:
                    if self[match[0]] != match[1]:
                        self._content[idx] = line.replace(match[1],
                                                          self[match[0]])
                    unseenKeys.pop(unseenKeys.index(match[0]))
            idx += 1

        for key in unseenKeys:
            self._content.append('%s="%s"' % (key, self[key]))

        open(self.path, 'w').write('\n'.join(self._content) + '\n')
        self._dirty = False

