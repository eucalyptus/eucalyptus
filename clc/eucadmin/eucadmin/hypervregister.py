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
import tempfile
import shutil
import command

class HyperVRegister(object):

    KeyDirPath = "%s/var/lib/eucalyptus/keys"
    
    def __init__(self, hostname, registration_password,
                 euca_dir='/opt/eucalyptus', debug=False):
        self.hostname = hostname
        self.registration_password = registration_password
        self.euca_dir = euca_dir
        self.euca_key_dir = self.KeyDirPath % self.euca_dir
        self.tmp_dir = self.create_tmp_dir()
        self.debug = debug
        self.commands = []

    def create_tmp_dir(self):
        return tempfile.mkdtemp()

    def delete_tmp_dir(self):
        if not self.debug:
            if os.path.isdir(self.tmp_dir):
                shutil.rmtree(self.tmp_dir)
                
    def error(self, msg):
        self.delete_tmp_dir()
        print msg
        sys.exit(1)
        
    def copy_key_file(self, file_name):
        file_path = os.path.join(self.euca_key_dir, file_name)
        if not os.path.exists(file_path):
            self.error('%s not found' % file_path)
        try:
            shutil.copy(file_path, self.tmp_dir)
        except:
            self.error('Error copying %s to %s' % (file_path, self.tmp_dir))
 
    def copy_files_to_node(self):
        files = ['node.p12', 'cluster-cert.pem', 'cloud-cert.pem']
        cur_dir = os.getcwd()
        os.chdir(self.tmp_dir)
        cmd = 'smbclient "//%s/EucaKeyShare"' % self.hostname
        cmd += ' --user=Administrator'
        l = ['put %s' % fn for fn in files]
        cmd += ' --command "%s"' % '; '.join(l)
        cmd = command.Command(cmd)
        os.chdir(cur_dir)
        for fn in files:
            if cmd.stderr.find('putting file %s' % fn) < 0:
                self.error('REGISTRATION FAILED')
        self.commands.append(cmd)

    def generate_nodep12(self):
        node_cert = os.path.join(self.tmp_dir, 'node-cert.pem')
        node_pk = os.path.join(self.tmp_dir, 'node-pk.pem')
        p12_file = os.path.join(self.tmp_dir, 'node.p12')
        cmd = 'openssl pkcs12 -export -in %s' % node_cert
        cmd += ' -inkey %s' % node_pk
        cmd += ' -password "pass:%s"' % self.registration_password
        cmd += ' -out %s' % p12_file
        self.commands.append(command.Command(cmd))
        if not os.path.isfile(p12_file):
            self.error('%s file was not created' % p12_file)

    def main(self):
        self.copy_key_file('node-cert.pem')
        self.copy_key_file('node-pk.pem')
        self.copy_key_file('cloud-cert.pem')
        self.copy_key_file('cluster-cert.pem')
        self.generate_nodep12()
        self.copy_files_to_node()
        self.delete_tmp_dir()
        
