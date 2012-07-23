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

EGREP="egrep --color=always"
ip=192.168.51.119
ssh root@$ip /opt/eucalyptus/etc/init.d/eucalyptus-cloud stop
while ! euca-describe-services -T vmwarebroker | ${EGREP} NOTREADY; do sleep 1; done
while ! euca-describe-services -T cluster | ${EGREP} NOTREADY; do sleep 1; done;
ssh root@$ip /opt/eucalyptus/etc/init.d/eucalyptus-cloud start
while ! (euca-describe-services -T vmwarebroker; euca-describe-services -T cluster) | ${EGREP} DISABLED; do sleep 1; date; done
euca-describe-services -T cluster
euca-describe-services -T vmwarebroker
while ! euca-describe-services -T vmwarebroker | ${EGREP} ENABLED; do 
	sleep 1
	euca-describe-services -T cluster
	euca-describe-services -T vmwarebroker
	date 
done
