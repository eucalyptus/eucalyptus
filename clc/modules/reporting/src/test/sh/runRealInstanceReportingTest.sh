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
#
# This file may incorporate work covered under the following copyright
# and permission notice:
#
#   Software License Agreement (BSD License)
#
#   Copyright (c) 2008, Regents of the University of California
#   All rights reserved.
#
#   Redistribution and use of this software in source and binary forms,
#   with or without modification, are permitted provided that the
#   following conditions are met:
#
#     Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#
#     Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer
#     in the documentation and/or other materials provided with the
#     distribution.
#
#   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
#   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
#   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
#   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
#   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
#   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
#   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
#   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
#   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
#   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
#   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
#   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
#   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
#   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
#   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
#   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
#   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
#   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
#   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.

# Gather CLC IP
CLC_IP=`cat ../input/2b_tested.lst |grep '\[.*CLC.*\]'|awk '{ print $1 }'`


# Set timing 
WRITE_INTERVAL_MS=60000
SLEEP_TIME_SECS=$(((WRITE_INTERVAL_MS*2)/1000))

# Set image paths
EKI=`euca-describe-images|awk '{ print $2 }'|grep 'eki-'`
ERI=`euca-describe-images|awk '{ print $2 }'|grep 'eri-'`
EMI=`euca-describe-images|awk '{ print $2 }'|grep 'emi-'`


# Login, and get session id
wget -O /tmp/sessionId --no-check-certificate 'https://localhost:8443/loginservlet?adminPw=admin'
if [ "$?" -ne "0" ]; then echo "Login failed"; exit 1; fi
export SESSIONID=`cat /tmp/sessionId`
echo "session id:" $SESSIONID


# Clear all prior data 
wget --no-check-certificate -O /tmp/nothing "https://localhost:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=removeAllData"
if [ "$?" -ne "0" ]; then echo "Clearing failed"; exit 1; fi


# Check that the data is cleared
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=containsRecentRows&methodArgs=false"
if [ "$?" -ne "0" ]; then echo "Data did not clear"; exit 1; fi


# Generate data
wget --no-check-certificate -O /tmp/nothing "https://localhost:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=setWriteIntervalMs&methodArgs=$WRITE_INTERVAL_MS"
if [ "$?" -ne "0" ]; then echo "Setting interval failed"; exit 1; fi
euca-run-instances -n 2 --kernel $EKI --ramdisk $ERI $EMI
if [ "$?" -ne "0" ]; then echo "Data generation failed"; exit 1; fi


# Sleep to allow instance data to propagate to CLC and be recorded
echo "Sleeping for $SLEEP_TIME_SECS seconds..."
sleep $SLEEP_TIME_SECS


# Check that data arrived in DB and has recent timestamp
wget --no-check-certificate -O /tmp/nothing "https://$CLC_IP:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.instance.FalseDataGenerator&methodName=containsRecentRows&methodArgs=true"
if [ "$?" -ne "0" ]; then echo "Data didnt arrive or timestamps incorrect"; exit 1; fi
