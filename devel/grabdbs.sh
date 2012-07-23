#!/bin/bash

# Copyright 2009-2012 Eucalyptus Systems, Inc.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
# Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
# CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
# additional information or have any questions.

D=`date | sed "s/ //g"`
HOST1=$1
if [ -z "$HOST1" ]; then
    HOST1="127.0.0.1"
fi
HOST2=$2
if [ -z "$HOST2" ]; then
    HOST2="127.0.0.1"
fi
mkdir -p /tmp/$D
DBPASS=`./dbPass.sh`
if ( ! mysqldump --all-databases -u eucalyptus --password="$DBPASS" --port=8777 --protocol=TCP --host=$HOST1 --skip-extended-insert --skip-opt --lock-tables=false > /tmp/$D/db.$HOST1 2>/dev/null ); then
    rm -f /tmp/$D/db.$HOST1
fi

if ( ! mysqldump --all-databases -u eucalyptus --password="$DBPASS" --port=8777 --protocol=TCP --host=$HOST2 --skip-extended-insert --skip-opt --lock-tables=false > /tmp/$D/db.$HOST2 2>/dev/null ); then
    rm -f /tmp/$D/db.$HOST2
fi
echo "/tmp/$D $HOST1 $HOST2"
