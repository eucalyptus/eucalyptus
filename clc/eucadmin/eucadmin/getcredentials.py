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

from boto.roboto.awsqueryrequest import AWSQueryRequest
from boto.roboto.param import Param
from eucadmin.command import Command
from ecuadmin.cmdstings import get_cmdstring
import eucadmin
import os
import time
import boto.utils

OpenSSLCmd = """openssl pkcs12 -in %s -name eucalyptus -name "eucalyptus" -password pass:eucalyptus  -passin pass:eucalyptus -passout pass:eucalyptus -nodes | grep -A30 "friendlyName: eucalyptus" | grep -A26 "BEGIN RSA" >  %s """

MySQLCmd = """echo "select u.auth_user_token from auth_user u inner join auth_group_has_users gu on u.id=gu.auth_user_id join auth_group g on gu.auth_group_id=g.id join auth_account a on g.auth_group_owning_account=a.id where a.auth_account_name='%s' and g.auth_group_name='_%s';" | mysql -u eucalyptus -P 8777 --protocol=TCP --password=%s eucalyptus_auth | tail -n1 """

DBPassCmd = """echo -n eucalyptus | openssl dgst -sha256 -sign %s/var/lib/eucalyptus/keys/cloud-pk.pem -hex"""

GetCertURL = 'https://localhost:8443/getX509?account=%s&user=%s&code=%s'

EucaP12File = '%s/var/lib/eucalyptus/keys/euca.p12'
CloudPKFile = '%s/var/lib/eucalyptus/keys/cloud-pk.pem'

class GetCredentials(AWSQueryRequest):
    
    ServiceClass = eucadmin.EucAdmin
    Description = 'Get credentials zip file.'
    Params = [Param(name='euca_home',
                    short_name='e', long_name='euca-home',
                    ptype='string', optional=True,
                    doc='Eucalyptus install dir, default is $EUCALYPTUS'),
              Param(name='account',
                    short_name='a', long_name='account',
                    ptype='string', optional=True, default='eucalyptus',
                    doc='The account whose credentials will be used'),
              Param(name='user',
                    short_name='u', long_name='user',
                    ptype='string', optional=True, default='admin',
                    doc='The Eucalyptus account that will be retrieved')]
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
                      
    def get_token(self, num_retries=10):
        i = 0
        while i < num_retries:
            cmd_string = get_cmdstring('mysql')
            cmd = Command(cmd_string % (self.account, self.user, self.db_pass))
            self.token = cmd.stdout.strip()
            if self.token:
                break
            print 'waiting for MySQL to respond'
            time.sleep(10)
            i += 1
        if not self.token:
            raise ValueError('cannot find code in database')

    def get_credentials(self):
        data = boto.utils.retry_url(GetCertURL % (self.account,
                                                  self.user,
                                                  self.token))
        fp = open(self.zipfile, 'wb')
        fp.write(data)
        fp.close()

    def get_dbpass(self):
        cmd_string = get_cmdstring('dbpass')
        cmd = Command(cmd_string % self.euca_home)
        self.db_pass = cmd.stdout.strip()

    def cli_formatter(self, data):
        pass

    def main(self, **args):
        self.args.update(args)
        self.process_args()
        if 'euca_home' in self.request_params:
            self.euca_home = self.request_params['euca_home']
        else:
            if 'EUCALYPTUS' in os.environ:
                self.euca_home = os.environ['EUCALYPTUS']
            else:
                raise ValueError('Unable to find EUCALYPTUS home')
        self.account = self.request_params['account']
        self.user = self.request_params['user']
        self.zipfile = self.request_params['zipfile']
        self.eucap12_file = EucaP12File % self.euca_home
        self.cloudpk_file = CloudPKFile % self.euca_home
        self.token = None
        self.db_pass = None
        self.check_zipfile()
        # check local service?
        if not self.check_cloudpk_file:
            self.gen_cloudpk_file()
        self.get_dbpass()
        self.get_token()
        self.get_credentials()

    def main_cli(self):
        self.do_cli()
        
        
