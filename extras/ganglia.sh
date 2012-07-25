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

TYPE="node"
NC_STAT="/var/run/eucalyptus/nc-stats"
SC_STAT="/var/log/eucalyptus/sc-stats.log"
WALRUS_STAT="/var/log/eucalyptus/walrus-stats.log"
GMETRIC="`which gmetric 2> /dev/null`"
DEBUG="N"
EUCALYPTUS="/"

usage () {

	echo "$0 [options]"
	echo
	echo "   -help                       this message"
	echo "   -type [nc|walrus|sc]        which stat file to look for"
	echo "   -d <eucalyptus_dir>         where eucalyptus is installed"
	echo
}

# let's parse the command line
while [ $# -gt 0 ]; do
	if [ "$1" = "-h" -o "$1" = "-help" -o "$1" = "?" -o "$1" = "--help" ]; then
		usage
		exit 0
	fi

	if [ "$1" = "-type" ]; then
		if [ -z "$2" ]; then
			echo "Need the type!"
			exit 1
		fi
		TYPE="$2"
		shift; shift
		continue
	fi
	if [ "$1" = "-debug" ]; then
		DEBUG="Y"
		shift
		continue
	fi
	if [ "$1" = "-d" ]; then
		if [ -z "$2" ]; then
			echo "Need eucalyptus directory!"
			exit 1
		fi
		EUCALYPTUS="$2"
		shift; shift
		continue
	fi
	usage
	exit 1
done

# some checks
if [ -z "$GMETRIC" ]; then
	echo "Cannot find gmetric: do you have ganglia installed?"
	exit 1
fi
if [ ! -e "$EUCALYPTUS/etc/eucalyptus/eucalyptus.conf" ]; then
	echo "Is Eucalyptus installed in $EUCALYPTUS?"
	exit 1
fi
if [ "$TYPE" = "nc" ]; then
	# let's check we have the stat file
	if [ ! -e ${EUCALYPTUS}${NC_STAT} ]; then
		echo "Cannot find NC stat file!"
		exit 1
	fi

	# number of running VMs
	NUM="`grep ^id: ${EUCALYPTUS}${NC_STAT}|wc -l`"
	# memory available to VMs
	M_AVAIL="`grep ^memory ${EUCALYPTUS}${NC_STAT}|sed \"s;memory.*MB: [[:digit:]]*/\([[:digit:]]*\)/[[:digit:]]*;\1;\"`"
	# memory used by VMs
	M_USED="`grep ^memory ${EUCALYPTUS}${NC_STAT}|sed \"s;memory.*MB: [[:digit:]]*/[[:digit:]]*/\([[:digit:]]*\);\1;\"`"
	# cores available to VMs
	C_AVAIL="`grep ^cores ${EUCALYPTUS}${NC_STAT}|sed \"s;cores.*: [[:digit:]]*/\([[:digit:]]*\)/[[:digit:]]*;\1;\"`"
	# cores used by VMs
	C_USED="`grep ^cores ${EUCALYPTUS}${NC_STAT}|sed \"s;cores.*: [[:digit:]]*/[[:digit:]]*/\([[:digit:]]*\);\1;\"`"
	# disk available to VMs
	D_AVAIL="`grep ^disk ${EUCALYPTUS}${NC_STAT}|sed \"s;disk.*GB: [[:digit:]]*/\([[:digit:]]*\)/[[:digit:]]*;\1;\"`"
	# disk used by VMs
	D_USED="`grep ^disk ${EUCALYPTUS}${NC_STAT}|sed \"s;disk.*GB: [[:digit:]]*/[[:digit:]]*/\([[:digit:]]*\);\1;\"`"
	
	[ "$DEBUG" = "Y" ] && echo $GMETRIC -n "Running VMs" -v $NUM -t int16 
	[ "$DEBUG" = "Y" ] && echo $GMETRIC -n "VMs available memory" -v $M_AVAIL -t int32 -u MB
	[ "$DEBUG" = "Y" ] && echo $GMETRIC -n "VMs used memory" -v $M_USED -t int32 -u MB
	[ "$DEBUG" = "Y" ] && echo $GMETRIC -n "VMs available cores" -v $C_AVAIL -t int32 
	[ "$DEBUG" = "Y" ] && echo $GMETRIC -n "VMs used cores" -v $C_USED -t int32 
	[ "$DEBUG" = "Y" ] && echo $GMETRIC -n "VMs available disks" -v $D_AVAIL -t int32 -u GB
	[ "$DEBUG" = "Y" ] && echo $GMETRIC -n "VMs used disks" -v $D_USED -t int32 -u GB

	$GMETRIC -n "Running VMs" -v $NUM -t int16 
	$GMETRIC -n "VMs available memory" -v $M_AVAIL -t int32 -u MB
	$GMETRIC -n "VMs used memory" -v $M_USED -t int32 -u MB
	$GMETRIC -n "VMs available cores" -v $C_AVAIL -t int32 
	$GMETRIC -n "VMs used cores" -v $C_USED -t int32 
	$GMETRIC -n "VMs available disks" -v $D_AVAIL -t int32 -u GB
	$GMETRIC -n "VMs used disks" -v $D_USED -t int32 -u GB

elif [ "$TYPE" = "sc" ]; then
	# let's check we have the stat file
	if [ ! -e ${EUCALYPTUS}${SC_STAT} ]; then
		echo "Cannot find SC stat file!"
		exit 1
	fi

	V_USED="`tail -n 1 ${EUCALYPTUS}${SC_STAT}|sed \"s/.*Volumes: \([[:digit:]]*\).*/\1/\"`"
	S_USED="`tail -n 1 ${EUCALYPTUS}${SC_STAT}|sed \"s/.*Space Used: \([[:digit:]]*\)/\1/\"`"

	[ "$DEBUG" = "Y" ] && echo $GMETRIC -n "Volumes" -v $V_USED -t int16 
	[ "$DEBUG" = "Y" ] && echo $GMETRIC -n "Volumes disk usage" -v $S_USED -t int16 

	$GMETRIC -n "Volumes" -v $V_USED -t int16 
	$GMETRIC -n "Volumes disk usage" -v $S_USED -t int16 

elif [ "$TYPE" = "walrus" ]; then
	# let's check we have the stat file
	if [ ! -e ${EUCALYPTUS}${WALRUS_STAT} ]; then
		echo "Cannot find wlarus stat file!"
		exit 1
	fi

	B_USED="`tail -n 1 ${EUCALYPTUS}${WALRUS_STAT}|sed \"s/.*Buckets: \([[:digit:]]*\).*/\1/\"`"
	S_USED="`tail -n 1 ${EUCALYPTUS}${WALRUS_STAT}|sed \"s/.*Space Used: \([[:digit:]]*\)/\1/\"`"

	[ "$DEBUG" = "Y" ] && echo $GMETRIC -n "Buckets" -v $B_USED -t int16 
	[ "$DEBUG" = "Y" ] && echo $GMETRIC -n "Buckets disk usage" -v $S_USED -t int16 

	$GMETRIC -n "Buckets" -v $B_USED -t int16 
	$GMETRIC -n "Buckets disk usage" -v $S_USED -t int16 
else
	echo "Unknown type!"
	exit 1
fi
