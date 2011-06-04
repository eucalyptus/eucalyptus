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

from eucadmin.command import Command
import re

def get_openssl_version():
    cmd = Command('openssl version')
    if cmd.status == 0:
        return cmd.stdout
    else:
        raise RuntimeError('Unable to determine OpenSSL version')

def get_mysql_version():
    return ''

Commands = {
    'openssl' : {
        'get_version_fn' : get_openssl_version,
        'commands' : [(re.compile('OpenSSL 1\.0\..*'), """openssl pkcs12 -in %s -name eucalyptus -name "eucalyptus" -password pass:eucalyptus  -passin pass:eucalyptus -passout pass:eucalyptus -nodes | grep -A30 "friendlyName: eucalyptus" | grep -A27 "BEGIN PRIVATE" >  %s """),
                      (re.compile('.*'), """openssl pkcs12 -in %s -name eucalyptus -name "eucalyptus" -password pass:eucalyptus  -passin pass:eucalyptus -passout pass:eucalyptus -nodes | grep -A30 "friendlyName: eucalyptus" | grep -A26 "BEGIN RSA" >  %s """)]
        },
    'dbpass' : {
        'get_version_fn' : get_openssl_version,
        'commands' : [(re.compile('OpenSSL 1\.0\..*'), """echo -n eucalyptus | openssl dgst -sha256 -sign %s/var/lib/eucalyptus/keys/cloud-pk.pem -hex"""),
                      (re.compile('.*'), """echo -n eucalyptus | openssl dgst -sha256 -sign %(EUCALYPTUS)s/var/lib/eucalyptus/keys/cloud-pk.pem -hex | cut -d' ' -f2""")]
        },
    'mysql' : {
        'get_version_fn' : get_mysql_version,
        'commands' : [(re.compile('.*'), """echo "select u.auth_user_token from auth_user u inner join auth_group_has_users gu on u.id=gu.auth_user_id join auth_group g on gu.auth_group_id=g.id join auth_account a on g.auth_group_owning_account=a.id where a.auth_account_name='%s' and g.auth_group_name='_%s';" | mysql -u eucalyptus -P 8777 --protocol=TCP --password=%s eucalyptus_auth | tail -n1 """)]
        }
    }

def get_cmdstring(cmd_name, version_str=None):
    if cmd_name not in Commands:
        raise KeyError('Unable to find cmd_name: %s' % cmd_name)
    cmd = Commands[cmd_name]
    if not version_str:
        version_str = cmd['get_version_fn']()
    for regex, cmd_string in cmd['commands']:
        if regex.match(version_str):
            return cmd_string
    return None
