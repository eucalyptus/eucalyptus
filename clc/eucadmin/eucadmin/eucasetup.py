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

# # first time setup
# if [ -n "$SETUP" ]; then
# 	ROOTWRAP="$EUCALYPTUS/usr/lib/eucalyptus/euca_rootwrap"
	
# 	# first of all setup euca_rootwrap
# 	if [ ! -x "$ROOTWRAP" ]; then
# 		echo "Cannot find $ROOTWRAP (or not readable)!"
# 		exit 1
# 	fi
# 	# get EUCA group
# 	if [ -z "$EUCA_USER" ]; then
# 		echo "Is EUCA_USER defined?"
# 		exit 1
# 	fi
# 	# if running as root no need to do anything
# 	if [ "$EUCA_USER" != "root" ]; then
# 		ID="`which id 2> /dev/null`"
# 		if [ -z "$ID" ]; then
# 			echo "Cannot find command $ID"
# 			exit 1
# 		fi
# 		if ! $ID $EUCA_USER > /dev/null 2> /dev/null ; then
# 			echo "User $EUCA_USER doesn't exists!"
# 			exit 1
# 		fi
# 		EUCA_GROUP="`$ID -ng $EUCA_USER 2>/dev/null`"
# 		if [ -z "$EUCA_GROUP" ]; then
# 			echo "Cannot detect $EUCA_USER group"
# 			exit 1
# 		fi
# 		if ! chown root:$EUCA_GROUP $ROOTWRAP ; then
# 			exit 1
# 		fi
# 		if ! chmod 4750 $ROOTWRAP ; then
# 			exit 1
# 		fi
# 	fi
	
# 	# let's create the instance path
# 	if [ -n "$INSTANCE_PATH" -a "$INSTANCE_PATH" != "not_configured" -a ! -d "$INSTANCE_PATH" ]; then
# 		if ! mkdir -p $INSTANCE_PATH ; then
# 			echo "Failed to create instance path!"
# 			exit 1
# 		fi
# 	fi
# 	if ! chown $EUCA_USER:$EUCA_GROUP $INSTANCE_PATH ; then
#                 echo "Failed to modify ownership of $INSTANCE_PATH (must be owned by $EUCA_USER:$EUCA_GROUP)!"
# 		exit 1
# 	fi

# 	chown -R $EUCA_USER:$EUCA_GROUP $EUCALYPTUS/var/lib/eucalyptus
# 	ret=$?
# 	chown -R $EUCA_USER:$EUCA_GROUP $EUCALYPTUS/var/log/eucalyptus
# 	let $((ret += $?))
# 	chown -R $EUCA_USER:$EUCA_GROUP $EUCALYPTUS/var/run/eucalyptus
# 	let $((ret += $?))
# 	chown $EUCA_USER:$EUCA_GROUP $EUCALYPTUS/etc/eucalyptus/eucalyptus.conf
# 	let $((ret += $?))
# 	chown $EUCA_USER:$EUCA_GROUP $EUCALYPTUS/etc/eucalyptus
# 	let $((ret += $?))

# 	# let's create more needed directory with the right permissions
# 	mkdir -p $EUCALYPTUS/var/lib/eucalyptus/dynserv/data
# 	let $((ret += $?))
# 	chown -R $EUCA_USER:$EUCA_GROUP $EUCALYPTUS/var/lib/eucalyptus/dynserv
# 	let $((ret += $?))
# 	chmod -R 700 $EUCALYPTUS/var/lib/eucalyptus/dynserv
# 	let $((ret += $?))
# 	mkdir -p $EUCALYPTUS/var/lib/eucalyptus/db
# 	let $((ret += $?))
# 	chown $EUCA_USER:$EUCA_GROUP $EUCALYPTUS/var/lib/eucalyptus/db
# 	let $((ret += $?))
# 	chmod 700 $EUCALYPTUS/var/lib/eucalyptus/db
# 	let $((ret += $?))
# 	mkdir -p $EUCALYPTUS/var/lib/eucalyptus/keys
# 	let $((ret += $?))
# 	chown $EUCA_USER:$EUCA_GROUP $EUCALYPTUS/var/lib/eucalyptus/keys
# 	let $((ret += $?))
# 	chmod 700 $EUCALYPTUS/var/lib/eucalyptus/keys
# 	let $((ret += $?))
# 	mkdir -p $EUCALYPTUS/var/lib/eucalyptus/CC
# 	let $((ret += $?))
# 	chown $EUCA_USER:$EUCA_GROUP $EUCALYPTUS/var/lib/eucalyptus/CC
# 	let $((ret += $?))
# 	chmod 700 $EUCALYPTUS/var/lib/eucalyptus/CC
# 	let $((ret += $?))

# 	exit $ret
# fi

import os
import pwd

RootWrapPath = 	'usr/lib/eucalyptus/euca_rootwrap'

MakeDirs = ['var/lib/eucalyptus/dynserv',
            'var/lib/eucalyptus/dynserv/data',
            'var/lib/eucalyptus/db',
            'var/lib/eucalyptus/keys',
            'var/lib/eucalyptus/CC']

ChownPaths = ['var/lib/eucalyptus',
              'var/log/eucalyptus',
              'var/run/eucalyptus',
              'etc/eucalyptus/eucalyptus.conf',
              'etc/eucalyptus',
              'var/lib/eucalyptus/dynserv/data',
              'var/lib/eucalyptus/dynserv',
              'var/lib/eucalyptus/db',
              'var/lib/eucalyptus/keys',
              'var/lib/eucalyptus/CC']

ChmodPaths = [('var/lib/eucalyptus/dynserv', 0700),
              ('var/lib/eucalyptus/dynserv/data', 0700),
              ('var/lib/eucalyptus/db', 0700),
              ('var/lib/eucalyptus/keys', 0700),
              ('var/lib/eucalyptus/CC', 0700)]

class EucaSetup(object):

    def __init__(self, config):
        self.config = config
        self.euca_user_name = None
        self.euca_user_id = None
        self.euca_user_group_id = None

    def chown_paths(self):
        for path in ChownPaths:
            path = os.path.join(self.config['EUCALYPTUS'], path)
            os.chown(path, self.euca_user_id, self.euca_user_group_id)

    def chmod_paths(self):
        for path,mod in ChmodPaths:
            path = os.path.join(self.config['EUCALYPTUS'], path)
            os.chmod(path, mod)

    def make_dirs(self):
        for dir_name in MakeDirs:
            path = os.path.join(self.config['EUCALYPTUS'], dir_name)
            os.makedirs(path)

    def main(self):
        # check for existence of rootwrap
        rootwrap = os.path.join(self.config['EUCALYPTUS'], RootWrapPath)
        if not os.path.isfile(rootwrap):
            raise IOError('Cannot find %s or not executable' % rootwrap)
        self.euca_user_name = self.config['EUCA_USER']
        root_data = pwd.getpwnam('root')
        if self.euca_user_name == 'root':
            self.euca_user_id = root_data.pw_uid
        else:
            try:
                user_data = pwd.getpwnam(self.euca_user_name)
            except KeyError:
                raise ValueError('Is EUCA_USER defined?')
            self.euca_user_id = user_data.pw_uid
            self.euca_user_group_id = user_data.pw_gid
            os.chown(rootwrap, root_data.pw_uid, self.euca_user_group_id)
            os.chmod(rootwrap, 04750)
        self.instance_path = self.config['INSTANCE_PATH']
        if self.instance_path and self.instance_path != 'not_configured':
            if not os.path.isdir(self.instance_path):
                os.mkdir(self.instance_path)
            os.chown(self.instance_path, self.euca_user_id,
                     self.euca_user_group_id)
        self.make_dirs()
        self.chown_paths()
        self.chmod_paths()
