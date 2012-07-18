#!/bin/sh

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

# Usage: getusercred.sh [ account [ user ] ]

ACCOUNT="eucalyptus"
USER="admin"

if [ ! -z $1 ]; then
  ACCOUNT=$1
  if [ ! -z $2 ]; then
    USER=$2
  fi
fi

echo "Retrieving credentials for arn:aws:iam::$ACCOUNT:user/$USER"

DIR=euca-$ACCOUNT-$USER

mkdir $DIR
usr/sbin/euca_conf --get-credentials $DIR/euca.zip --cred-account $ACCOUNT --cred-user $USER
( cd $DIR; unzip euca.zip )
