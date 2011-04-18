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
import time
import boto.utils
from eucadmin.command import Command

OpenSSLCmd = """openssl pkcs12 -in %s -name eucalyptus -name "eucalyptus" -password pass:eucalyptus  -passin pass:eucalyptus -passout pass:eucalyptus -nodes | grep -A30 "friendlyName: eucalyptus" | grep -A26 "BEGIN RSA" >  %s"""

MySQLCmd = """echo "select u.auth_user_token from auth_user u inner join auth_group_has_users gu on u.id=gu.auth_user_id join auth_group g on gu.auth_group_id=g.id join auth_account a on g.auth_group_owning_account=a.id where a.auth_account_name='%s' and g.auth_group_name='_%s';" | mysql -u eucalyptus -P 8777 --protocol=TCP --password=%s eucalyptus_auth | tail -n1"""

MySQLCmdNew = \
"""echo "select u.auth_user_token,k.auth_access_key_query_id,k.auth_access_key_key from (auth_access_key k join auth_user u on k.auth_access_key_owning_user=u.id join auth_group_has_users gu on u.id=gu.auth_user_id join auth_group g on gu.auth_group_id=g.id join auth_account a on g.auth_group_owning_account=a.id) where a.auth_account_name='%s' and g.auth_group_name='_%s' and k.auth_access_key_active=1;"  | mysql -u eucalyptus -P 8777 --protocol=TCP --password=%s eucalyptus_auth | tail -n1 | awk '{print $1}' """

DBPassCmd = """echo -n eucalyptus | openssl dgst -sha256 -sign %(EUCALYPTUS)s/var/lib/eucalyptus/keys/cloud-pk.pem -hex"""

# Should wget be parameterized?  
GetCertURL = 'https://localhost:8443/getX509?account=%s&user=%s&code=%s'

EucaP12File = '%(EUCALYPTUS)s/var/lib/eucalyptus/keys/euca.p12'
CloudPKFile = '%(EUCALYPTUS)s/var/lib/eucalyptus/keys/cloud-pk.pem'

class GetCredentials(object):
    """
    Get credentials zip file.
    """

    def __init__(self, config, zipfile_name, account, user):
        """
        * config - the config object holding values for eucalpytus.conf
        * zipfile_name - the name of the resulting zip file
        * account - the account for which you are retrieving credentials
        * user - the user for which you are retrieving credentials
        """
        self.config = config
        self.zipfile_name = zipfile_name
        self.account = account
        self.user = user
        self.eucap12_file = EucaP12File % self.config
        self.cloudpk_file = CloudPKFile % self.config
        self.token = None
        self.db_pass = None

    def check_zipfile(self):
        if os.path.exists(self.zipfile_name):
            msg = 'file %s already exists, ' % self.zipfile_name
            msg += 'please remove and try again'
            raise IOError(msg)

    def check_cloudpk_file(self):
        if os.path.exists(self.cloudpk_file):
            stats = os.stat(self.cloudpk_file)
            if stats.st_size > 0:
                return True
        return False

    def gen_cloudpk_file(self):
        cmd = Command(OpenSSLCmd % (self.eucap12_file, self.cloudpk_file))
                      
    def get_token(self, num_retries=10):
        i = 0
        while i < num_retries:
            cmd = Command(MySQLCmd % (self.account, self.user, self.db_pass))
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
        fp = open(self.zipfile_name, 'wb')
        fp.write(data)
        fp.close()

    def get_dbpass(self):
        cmd = Command(DBPassCmd % self.config)
        self.db_pass = cmd.stdout.strip()

    def main(self):
        self.check_zipfile()
        # check local service?
        if not self.check_cloudpk_file:
            self.gen_cloudpk_file()
        self.get_dbpass()
        self.get_token()
        self.get_credentials()
        
        
