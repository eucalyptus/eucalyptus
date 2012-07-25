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

# Runs any class in the standard JUnit harness. Does not require Eucalyptus to be
#  running. Initializes everything, sets up database and persistence contexts, etc.
#
# Syntax: runJUnitTest.sh fullyQualifiedClassName
#
# Example usage:
#  runJUnitTest.sh com.eucalyptus.cloud.ws.EucaImagerTest
#
# TODO: unify this with runTest.sh, which does the same thing except it
# runs our own test harness and does not set some env variables

set -e
log() {
  FSTR="${1}\n"
  shift
  printf "${FSTR}" ${@} | tee -a ${UP_LOG}
}

if [ -z "${EUCALYPTUS}" ]; then
	echo "EUCALYPTUS must be set to run tests."
fi

# setup the classpath
CLASSPATH=".:"
FILES=$(\ls -1 ${EUCALYPTUS}/usr/share/eucalyptus/*.jar)
for FILE in $FILES; do
  export CLASSPATH=${FILE}:${CLASSPATH}
done
CLASSPATH=${EUCALYPTUS}/etc/eucalyptus/cloud.d/upgrade:${EUCALYPTUS}/etc/eucalyptus/cloud.d/scripts:${CLASSPATH}
#echo $CLASSPATH

# set a bunch of other variables for euca_imager to work
export VDDK_HOME="$EUCALYPTUS/packages/vddk"
export LD_LIBRARY_PATH="$EUCALYPTUS/packages/axis2c-1.6.0/lib:$EUCALYPTUS/packages/axis2c-1.6.0/modules/rampart:$EUCALYPTUS/usr/lib/eucalyptus:$VDDK_HOME/lib:$VDDK_HOME/lib/vmware-vix-disklib/lib32:$VDDK_HOME/lib/vmware-vix-disklib/lib64/" # to ensure euca_imager finds VDDK libs
export PATH="$EUCALYPTUS/usr/lib/eucalyptus:$PATH" # to ensure euca_imager has euca_rootwrap

# since JUnit expects class names, we assume anything starting with '-' is a JVM argument
for arg in "$@" ; do
    if [ "${arg:0:1}" = '-' ] ; then
        JVM_PARAMS="$JVM_PARAMS $arg"
    else
        JUNIT_PARAMS="$JUNIT_PARAMS $arg"
    fi
done

java -Xbootclasspath/p:${EUCALYPTUS}/usr/share/eucalyptus/openjdk-crypto.jar -classpath ${CLASSPATH} \
    -Deuca.home=${EUCALYPTUS} \
    -Deuca.lib.dir=${EUCALYPTUS} \
    -Deuca.upgrade.new.dir=${EUCALYPTUS} \
    -Deuca.upgrade.destination=com.eucalyptus.upgrade.MysqldbDestination \
    -Deuca.log.level=TRACE  \
    -Deuca.log.appender=console \
    -Djava.security.egd=file:/dev/./urandom \
    ${JVM_PARAMS} \
    org.junit.runner.JUnitCore ${JUNIT_PARAMS}
