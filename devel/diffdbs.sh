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

D=$1
if [ -z "$D" ]; then
    echo "USAGE: diffdbs.sh <directory of db dumps> [<host1 ip>] [<host2 ip>]"
    exit 1
fi
if [ ! -d "$D" ]; then
    echo "ERROR: diffdbs.sh: can't find db dump directory '$D'"
    exit 1
fi

HOST1=$2
if [ -z "$HOST1" ]; then
    HOST1="127.0.0.1"
fi
HOST2=$3
if [ -z "$HOST2" ]; then
    HOST2="127.0.0.1"
fi
if [ ! -f "$D/db.$HOST1" -o ! -f "$D/db.$HOST2" ]; then
    echo "ERROR: diffdbs.sh: cannot find one of db dumps '$D/db.$HOST1' or '$D/db.$HOST2'"
    exit 1
fi

diff $D/db.$HOST1 $D/db.$HOST2 > $D/rawdiff
cat $D/db.$HOST1 | sed "s/,/\\n/g" | sed "s/)//g" | sed "s/;//g" | sort > $D/a
cat $D/db.$HOST2 | sed "s/,/\\n/g" | sed "s/)//g" | sed "s/;//g" | sort > $D/b
diff $D/a $D/b > $D/filterdiff
rm -f $D/a $D/b
echo "$D/rawdiff $D/filterdiff"
