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

from boto.roboto.awsqueryrequest import AWSQueryRequest
from boto.roboto.param import Param
from eucadmin.command import Command
from eucadmin.cmdstrings import get_cmdstring
import eucadmin
import os
import sys
import time
import boto.utils
import pgdb as db
import hashlib
import binascii
from M2Crypto import RSA

GetCertURL = 'https://localhost:8443/getX509?account=%s&user=%s&code=%s'

EucaP12File = '%s/var/lib/eucalyptus/keys/euca.p12'
CloudPKFile = '%s/var/lib/eucalyptus/keys/cloud-pk.pem'

class GetCredentials(AWSQueryRequest):
    ServiceClass = eucadmin.EucAdmin
    Description = ("Download a user's credentials to <zipfile>.  New X.509 "
                   "credentials are created each time this command is called.")

    Params = [Param(name='euca_home',
                    short_name='e', long_name='euca-home',
                    ptype='string', optional=True,
                    doc='Eucalyptus install dir, default is $EUCALYPTUS'),
              Param(name='account',
                    short_name='a', long_name='account',
                    ptype='string', optional=True, default='eucalyptus',
                    doc=('account containing the user for which to get '
                         'credentials (default: eucalyptus)')),
              Param(name='user',
                    short_name='u', long_name='user',
                    ptype='string', optional=True, default='admin',
                    doc=('user name for which to get credentials '
                         '(default: admin)'))]
    Args = [Param(name='zipfile', long_name='zipfile',
                  ptype='string', optional=False,
                  doc='The path to the resulting zip file with credentials')]

    def check_zipfile(self):
        if os.path.exists(self.zipfile):
            msg = 'file %s already exists, ' % self.zipfile
            msg += 'please remove and try again'
            raise IOError(msg)

    def check_cloudpk_file(self):
        if os.path.exists(self.cloudpk_file):
            stats = os.stat(self.cloudpk_file)
            if stats.st_size > 0:
                return True
        return False

    def gen_cloudpk_file(self):
        cmd_string = get_cmdstring('openssl')
        cmd = Command(cmd_string % (self.eucap12_file, self.cloudpk_file))

    def get_credentials(self):
        data = boto.utils.retry_url(GetCertURL % (self.account,
                                                  self.user,
                                                  self.token),
                                    num_retries=1)
        fp = open(self.zipfile, 'wb')
        fp.write(data)
        fp.close()

    def get_dbpass(self):
        def passphrase_callback():
            return "eucalyptus"
        d = hashlib.sha256()
        d.update("eucalyptus")
        pk = RSA.load_key(self.cloudpk_file,passphrase_callback)
        self.db_pass = binascii.hexlify(pk.sign(d.digest(),algo="sha256"))

    def cli_formatter(self, data):
        pass

    def setup_query(self):
        self.token = None
        self.db_pass = None
        if 'euca_home' in self.request_params:
            self.euca_home = self.request_params['euca_home']
        else:
            if 'EUCALYPTUS' in os.environ:
                self.euca_home = os.environ['EUCALYPTUS']
            else:
                # check if self.ServiceClass.InstallPath is the Euca home
                if os.path.exists(os.path.join(self.ServiceClass.InstallPath, 'var/lib/eucalyptus/keys/')):
                    self.euca_home = self.ServiceClass.InstallPath
                else:
                    raise ValueError('Unable to find EUCALYPTUS home')
        self.account = self.request_params['account']
        self.user = self.request_params['user']
        self.zipfile = self.request_params['zipfile']
        self.eucap12_file = EucaP12File % self.euca_home
        self.cloudpk_file = CloudPKFile % self.euca_home
        if not self.check_cloudpk_file:
            self.gen_cloudpk_file()
        self.get_dbpass()

    def get_accesskey_secretkey(self, **args):
        self.args.update(args)
        self.process_args()
        result = self.get_keys()
        return '\t'.join(result)

    def get_keys(self):
        self.setup_query()
        con1 = db.connect(host='localhost:8777', user='eucalyptus', password=self.db_pass, database='eucalyptus_auth')
        cur1 = con1.cursor()
        cur1.execute("""select k.auth_access_key_query_id, k.auth_access_key_key 
                          from auth_access_key k 
                          join auth_user u on k.auth_access_key_owning_user=u.id
                          join auth_group_has_users gu on u.id=gu.auth_user_id 
                          join auth_group g on gu.auth_group_id=g.id 
                          join auth_account a on g.auth_group_owning_account=a.id 
                         where a.auth_account_name=%(acctname)s and g.auth_group_name=%(grpname)s and k.auth_access_key_active=TRUE""",
                     params={'acctname': self.args.get('account'),
                             'grpname': '_' + self.args.get('user')})
        result = cur1.fetchall()
        if not len(result):
            return ("", "")
        return result[0]

    def get_token(self):
        con1 = db.connect(host='localhost:8777', user='eucalyptus', password=self.db_pass, database='eucalyptus_auth')
        cur1 = con1.cursor()
        cur1.execute("""select u.auth_user_token 
                          from auth_user u 
                          join auth_group_has_users gu on u.id=gu.auth_user_id
                          join auth_group g on gu.auth_group_id=g.id 
                          join auth_account a on g.auth_group_owning_account=a.id 
                          where a.auth_account_name=%(acctname)s and g.auth_group_name=%(grpname)s""",
                     params={'acctname': self.account, 'grpname': '_' + self.user})
        result = cur1.fetchall()
        return result[0][0]

    def main(self, **args):
        self.args.update(args)
        self.process_args()
        self.setup_query()
        self.check_zipfile()
        # check local service?

        try:
            self.token = self.get_token()
        except IndexError:
            sys.exit('error: no such account or user')

        self.get_credentials()

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()
