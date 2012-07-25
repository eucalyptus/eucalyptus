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

NODES=""
CC=""
SC=""
WALRUS=""
CLOUD=""
NAGIOS=""
NAGIOS_PIPE="/var/lib/nagios3/rw/nagios.cmd"
NC_HOSTS=""
CC_HOSTS=""
SC_HOSTS=""
PRINT_HOSTS=""
PRINT_MEMBERS=""
DEBUG="N"
WGET="`which wget 2> /dev/null`"
TOUT=30

usage () {

	echo "$0 [options]"
	echo
	echo "   --help                       this message"
	echo "   --nodes \"<hostA> <hostB>\"    use this node list"
	echo "   --cc \"<hostA> <hostB>\"       use this cc list"
	echo "   --sc \"<hostA> <hostB>\"       use this sc list"
	echo "   --cloud <hostA>              use this cloud server"
	echo "   --walrus <hostA>             use this walrus"
	echo "   --setup                      print the nagios rules"
	echo "   --check <nagios pipe>        check NC status and tell nagios"
	echo
}

# print the configuration
print_host() {
	local CHECK_HOST="${1}"

	if [ -z "${CHECK_HOST}" ]; then
		return
	fi

	cat  <<EOF
define host {
	max_check_attempts      4
	host_name               ${CHECK_HOST}
}

define service {
        max_check_attempts	4
        host_name		${CHECK_HOST}
        service_description	PING
        check_command		check_ping!100.0,20%!500.0,60%
}

EOF
}

print_config() {
	local NAME="$1"
	local FULLNAME="$2"

	# we expect PRINT_HOSTS and PRINT_MEMBERS to be non null
	if [ -z "${PRINT_HOSTS}${PRINT_MEMBERS}" ]; then
		return
	fi
	
	cat <<EOF
define service {
	max_check_attempts	4
	host_name		${PRINT_HOSTS}
	service_description 	${NAME}
	passive_checks_enabled 	1
	active_checks_enabled 	0
	check_command		check_load
}

#define service {
#        max_check_attempts	4
#        host_name		${PRINT_HOSTS}
#        service_description	PING
#        check_command		check_ping!100.0,20%!500.0,60%
#}

define servicegroup {
	servicegroup_name	${NAME}
	alias			${FULLNAME}
	members			${PRINT_MEMBERS}
}

define hostgroup {
	hostgroup_name		${NAME}
	alias			${FULLNAME}
	members			${PRINT_HOSTS}
}

EOF
}

get_status() {
	local address="$1"
	local name="$2"
	local ret="false"

	ret="`$WGET -O - -o /dev/null --timeout=$TOUT http://${address}:8773/services/Heartbeat|grep \"${name}\" |sed 's/.*local=\([[:alnum:]]*\)[[:blank:]].*/\1/'`"
	
	if [ "$ret" = "true" ]; then	
		return 0
	fi

	return 1
}

# let's parse the command line
while [ $# -gt 0 ]; do
	if [ "$1" = "-h" -o "$1" = "-help" -o "$1" = "?" -o "$1" = "--help" ]; then
		usage
		exit 0
	fi

	if [ "$1" = "-setup" -o "$1" = "--setup" ]; then 
		NAGIOS="Y"
		shift
		continue
	fi
	if [ "$1" = "-check" -o "$1" = "--check" ]; then 
		if [ -z "$2" ]; then
			echo "Need the pipe name!"
			exit 1
		fi
		NAGIOS_PIPE="$2"
		shift; shift
		continue
	fi
	if [ "$1" = "-nodes" -o "$1" = "--nodes" ]; then 
		if [ -z "$2" ]; then
			echo "Need the node list!"
			exit 1
		fi
		NODES="${2}"
		shift; shift
		continue
	fi
	if [ "$1" = "-cc" -o "$1" = "--cc" ]; then 
		if [ -z "$2" ]; then
			echo "Need the cc list!"
			exit 1
		fi
		CC="${2}"
		shift; shift
		continue
	fi
	if [ "$1" = "-sc" -o "$1" = "--sc" ]; then 
		if [ -z "$2" ]; then
			echo "Need the sc list!"
			exit 1
		fi
		SC="${2}"
		shift; shift
		continue
	fi
	if [ "$1" = "-cloud" -o "$1" = "--cloud" ]; then 
		if [ -z "$2" ]; then
			echo "Need the cloud manager!"
			exit 1
		fi
		CLOUD="${2}"
		shift; shift
		continue
	fi
	if [ "$1" = "-walrus" -o "$1" = "--walrus" ]; then 
		if [ -z "$2" ]; then
			echo "Need walrus!"
			exit 1
		fi
		WALRUS="${2}"
		shift; shift
		continue
	fi
	if [ "$1" = "--debug"  ]; then 
		DEBUG="Y"
		shift
		continue
	fi
	usage
	exit 1
done

# some basic check
if [ -z "$NAGIOS" -a -z "$NAGIOS_PIPE" ]; then
	echo "I need either -setup or -check!"
	exit 1
fi
if [ -z "${NODES}${CC}${SC}${WARUS}${CLOUD}" ]; then
	echo "At least one service needs to be specified!"
	exit 1
fi

if [ -n "$NAGIOS" ]; then
	CHECK="N"
	for x in $CLOUD ; do
		if [ "$CHECK" = "Y" ]; then
			echo "Only one cloud can be specified!" 
			exit 1
		fi
		CHECK="Y"
	done
	CHECK="N"
	for x in $WALRUS ; do
		if [ "$CHECK" = "Y" ]; then
			echo "Only one walrus can be specified!"
			exit 1
		fi
		CHECK="Y"
	done
	
	# on with real work
	if [ -n "$NODES" ]; then
		NC_HOSTS=""
		MEMBERS=""
		# define the node controller nodes`
		for x in $NODES ; do
			# nagios doesn't want double hosts
			if `echo "$NC_HOSTS"|grep $x > /dev/null` ; then
				continue
			fi
			print_host ${x}
			if [ -n "$NC_HOSTS" ]; then
				NC_HOSTS="$NC_HOSTS, $x"
				MEMBERS="$MEMBERS, $x, eucalyptus-nc"
			else
				NC_HOSTS="$x"
				MEMBERS="$x, eucalyptus-nc"
			fi
		done

		# services
		PRINT_HOSTS="${NC_HOSTS}"
		PRINT_MEMBERS="${MEMBERS}"
		print_config eucalyptus-nc "Eucalyptus Node Controller"
	fi

	if [ -n "${CC}" ]; then
		# now for CC
		CC_HOSTS=""
		MEMBERS=""
		# define the node controller nodes`
		for x in $CC ; do
			# nagios doesn't want double hosts
			if  `echo "$CC_HOSTS"|grep $x > /dev/null` ; then
				continue
			fi
			if ! `echo "$NC_HOSTS"|grep $x > /dev/null` ; then
				print_host ${x}
			fi
			if [ -n "$CC_HOSTS" ]; then
				CC_HOSTS="$CC_HOSTS, $x"
				MEMBERS="$MEMBERS, $x, eucalyptus-cc"
			else
				CC_HOSTS="$x"
				MEMBERS="$x, eucalyptus-cc"
			fi
		done

		# define the services
		PRINT_HOSTS="${CC_HOSTS}"
		PRINT_MEMBERS="${MEMBERS}"
		print_config eucalyptus-cc "Eucalyptus Cluster Controller"
	fi

	if [ -n "${SC}" ]; then
		# now for SC
		SC_HOSTS=""
		MEMBERS=""
		# define the node controller nodes`
		for x in $SC ; do
			# nagios doesn't want double hosts
			if  `echo "$SC_HOSTS"|grep $x > /dev/null` ; then
				continue
			fi
			if ! `echo "$NC_HOSTS $CC_HOSTS"|grep $x > /dev/null` ; then
				print_host ${x}
			fi
			if [ -n "$SC_HOSTS" ]; then
				CC_HOSTS="$SC_HOSTS, $x"
				MEMBERS="$MEMBERS, $x, eucalyptus-sc"
			else
				SC_HOSTS="$x"
				MEMBERS="$x, eucalyptus-sc"
			fi
		done

		# define the services
		PRINT_HOSTS="${SC_HOSTS}"
		PRINT_MEMBERS="${MEMBERS}"
		print_config eucalyptus-sc "Eucalyptus Storage Controller"
	fi

	if [ -n "${CLOUD}" ]; then
		# now for the CLOUD
		if ! `echo "$SC_HOSTS $CC_HOSTS $NC_HOSTS"|grep $CLOUD > /dev/null` ; then
			print_host ${CLOUD}
		fi
		PRINT_HOSTS="${CLOUD}"
		PRINT_MEMBERS="${CLOUD}, eucalyptus-cloud"
		print_config eucalyptus-cloud "Eucalyptus Cloud Controller"
	fi

	if [ -n "${WALRUS}" ]; then
		# now for the WALRUS
		if ! `echo "$CC_HOSTS $NC_HOSTS $CLOUD $SC_HOSTS"|grep $WALRUS > /dev/null` ; then
			print_host ${WALRUS}
		fi
		PRINT_HOSTS="${WALRUS}"
		PRINT_MEMBERS="${WALRUS}, eucalyptus-walrus"
		print_config eucalyptus-walrus "Eucalyptus Walrus"
	fi
	exit 0
fi

if [ -n "$NAGIOS_PIPE" ]; then
	if [ -z "$WGET" ]; then
		echo "wget is missing!"
		exit 1
	fi
	if [ ! -p "$NAGIOS_PIPE" -o ! -w "$NAGIOS_PIPE" ]; then
		echo "$NAGIOS_PIPE is not a pipe or is not writable!"
		exit 1
	fi

	# let's check the NCs
	if [ -n "$NODES" ]; then
		for x in $NODES ; do
			# get the status of the node
			if $WGET -O - -o /dev/null --timeout=$TOUT http://$x:8775/axis2/services |grep Deployed|grep EucalyptusNC > /dev/null; then
				STATUS="0"
			else
				STATUS="2"
			fi
			DESCRIPTION="Eucalyptus Node Controller status"
		
			# let's tell nagios
			[ "$DEBUG" = "Y" ] && echo "[`date +%s`] PROCESS_SERVICE_CHECK_RESULT;$x;eucalyptus-nc;$STATUS;$DESCRIPTION"
			echo "[`date +%s`] PROCESS_SERVICE_CHECK_RESULT;$x;eucalyptus-nc;$STATUS;$DESCRIPTION" > $NAGIOS_PIPE
		done
	fi

	# let's check CC
	if [ -n "$CC"  ]; then
		for x in "$CC" ; do
			# get the status 
			if $WGET -O - -o /dev/null --timeout=$TOUT http://$x:8774/axis2/services |grep EucalyptusCC > /dev/null; then
				STATUS="0"
			else
				STATUS="2"
			fi
			DESCRIPTION="Eucalyptus Cluster Controller status"
		
			# let's tell nagios
			[ "$DEBUG" = "Y" ] && echo "[`date +%s`] PROCESS_SERVICE_CHECK_RESULT;$x;eucalyptus-cc;$STATUS;$DESCRIPTION"
			echo "[`date +%s`] PROCESS_SERVICE_CHECK_RESULT;$x;eucalyptus-cc;$STATUS;$DESCRIPTION" > $NAGIOS_PIPE
		done
	fi

	# let's check the cloud
	if [ -n "$CLOUD" ]; then
		for x in "$CLOUD" ; do
			# get the status 
			if get_status $x eucalyptus ; then
				STATUS="0"
			else
				STATUS="2"
			fi
			DESCRIPTION="Eucalyptus Cloud Controller status"
		
			# let's tell nagios
			[ "$DEBUG" = "Y" ] && echo "[`date +%s`] PROCESS_SERVICE_CHECK_RESULT;$x;eucalyptus-cloud;$STATUS;$DESCRIPTION"
			echo "[`date +%s`] PROCESS_SERVICE_CHECK_RESULT;$x;eucalyptus-cloud;$STATUS;$DESCRIPTION" > $NAGIOS_PIPE
		done
	fi

	# let's check the walrus
	if [ -n "$WALRUS" ]; then
		for x in "$WALRUS" ; do
			# get the status 
			if get_status $x walrus ; then
				STATUS="0"
			else
				STATUS="2"
			fi
			DESCRIPTION="Eucalyptus Walrus status"
		
			# let's tell nagios
			[ "$DEBUG" = "Y" ] && echo "[`date +%s`] PROCESS_SERVICE_CHECK_RESULT;$x;eucalyptus-walrus;$STATUS;$DESCRIPTION"
			echo "[`date +%s`] PROCESS_SERVICE_CHECK_RESULT;$x;eucalyptus-walrus;$STATUS;$DESCRIPTION" > $NAGIOS_PIPE
		done
	fi

	# let's check SC
	if [ -n "$SC"  ]; then
		for x in "$SC" ; do
			# get the status 
			if get_status $x storage ; then
				STATUS="0"
			else
				STATUS="2"
			fi
			DESCRIPTION="Eucalyptus Storage Controller status"
		
			# let's tell nagios
			[ "$DEBUG" = "Y" ] && echo "[`date +%s`] PROCESS_SERVICE_CHECK_RESULT;$x;eucalyptus-sc;$STATUS;$DESCRIPTION"
			echo "[`date +%s`] PROCESS_SERVICE_CHECK_RESULT;$x;eucalyptus-sc;$STATUS;$DESCRIPTION" > $NAGIOS_PIPE
		done
	fi

	exit 0
fi
