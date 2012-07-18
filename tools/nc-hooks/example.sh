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


# All executables placed into this directory will be invoked by NC
# at certain points with certain arguments. Currently supported are:
#
#   euca-nc-post-init <eucalyptus-home>
#   euca-nc-pre-boot  <eucalyptus-home> <instance-home>
#   euca-nc-pre-adopt <eucalyptus-home> <instance-home>
#   euca-nc-pre-clean <eucalyptus-home> <instance-home>
#
# If the executables return 0, the execution proceeds normally.
# If values [1-99] are returned, NC will abort some operation:
# it will not initialize, or will not run or adopt the instance.
# Any directories or files that cannot be executed are ignored.
#
# Make this bash script executable for it to work.

event=$1
euca_home=$2
inst_home=$3

if [ "$event" == "" ] ; then
    echo "an NC hook is called without an event"
    exit 1;
fi

if [ "$euca_home" == "" ] ; then
    echo "an NC hook is called without EUCALYPTUS home"
    exit 1;
fi

log_file=${euca_home}/var/log/eucalyptus/nc-hooks.log

echo `date` "$event $euca_home $inst_home" >>$log_file

exit 0;
