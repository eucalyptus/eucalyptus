#!/bin/sh -x
#
#Copyright (c) 2011  Eucalyptus Systems, Inc.   
#
#This program is free software: you can redistribute it and/or modify
#it under the terms of the GNU General Public License as published by 
#the Free Software Foundation, only version 3 of the License.  
# 
#This file is distributed in the hope that it will be useful, but WITHOUT
#ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
#FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
#for more details.  
#
#You should have received a copy of the GNU General Public License along
#with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
#Please contact Eucalyptus Systems, Inc., 130 Castilian
#Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
#if you need additional information or have any questions.
#
#This file may incorporate work covered under the following copyright and
#permission notice:
#
#  Software License Agreement (BSD License)
#
#  Copyright (c) 2008, Regents of the University of California
#  
#
#  Redistribution and use of this software in source and binary forms, with
#  or without modification, are permitted provided that the following
#  conditions are met:
#
#    Redistributions of source code must retain the above copyright notice,
#    this list of conditions and the following disclaimer.
#
#    Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in the
#    documentation and/or other materials provided with the distribution.
#
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
#  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
#  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
#  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
#  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
#  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
#  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
#  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
#  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
#  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
#  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
#  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
#  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
#  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
#  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
#  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
#  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
#  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
#  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
#  ANY SUCH LICENSES OR RIGHTS.
#   

print_usage () {
	echo "Usage: generateReport.sh [options]"
	echo "  --help	This message"
	echo "  -t		Type of report (can be instance, storage, s3)"
	echo "  -s		Starting timestamp (milliseconds since Jan 1, 1970)"
	echo "  -e		Ending timestamp (milliseconds since Jan 1, 1970)"
	echo "  -f		Format (html, csv, pdf)"
	echo "  -c		Criterion (user, account, cluster, zone)"
	echo "  -g		Group-by criterion (none, user, account, cluster, zone)"
	echo "  -p		Admin password (required, defaults to admin"
	echo "  -o		Outfile, where the report will be stored (default report.html, report.pdf, etc)"
}

# Set param defaults
TYPE="user_vms"
START="0"
timestamp=`date +%s`
timestamp=$(($timestamp*1000))
END=$timestamp
FORMAT="csv"
CRITERION_ID="2"
GROUPBY_ID="0"
PASSWORD="admin"
OUTFILE=""

# Parse params
while [ $# -gt 0 ]; do
	if [ "$1" = "-t" ]; then
		if [ "$2" = "storage" ]; then
			TYPE="user_storage"
		elif [ "$2" = "s3" ]; then
			TYPE="user_s3"
		else
			echo "Unrecognized type $2, defaulting to instance"
		fi
		shift 2
		continue
	fi
	if [ "$1" = "-o" ]; then
		OUTFILE="$2"
		shift 2
		continue
	fi
	if [ "$1" = "-p" ]; then
		PASSWORD="$2"
		shift 2
		continue
	fi
	if [ "$1" = "-s" ]; then
		START="$2"
		shift 2
		continue
	fi
	if [ "$1" = "-e" ]; then
		END="$2"
		shift 2
		continue
	fi
	if [ "$1" = "-f" ]; then
		if [ "$2" = "csv" ]; then
			FORMAT=$2
		elif [ "$2" = "html" ]; then
			FORMAT=$2
		elif [ "$2" = "pdf" ]; then
			FORMAT=$2
		else
			echo "Unrecognized format $2, defaulting to csv"
		fi
		shift 2
		continue
	fi
	if [ "$1" = "-c" ]; then
		if [ "$2" = "user" ]; then
			CRITERION_ID="2"
		elif [ "$2" = "account" ]; then
			CRITERION_ID="1"
		elif [ "$2" = "cluster" ]; then
			CRITERION_ID="0"
		else
			echo "Unrecognized criterion $2, defaulting to user"
		fi
		shift 2
		continue
	fi
	if [ "$1" = "-g" ]; then
		if [ "$2" = "user" ]; then
			GROUPBY_ID="4"
		elif [ "$2" = "account" ]; then
			GROUPBY_ID="3"
		elif [ "$2" = "cluster" ]; then
			GROUPBY_ID="2"
		elif [ "$2" = "zone" ]; then
			GROUPBY_ID="1"
		else
			echo "Unrecognized criterion $2, defaulting to user"
		fi
		shift 2
		continue
	fi
	if [ "$1" = "--help" ]; then
		print_usage
		exit 0
	fi
done
if [ "$OUTFILE" = "" ]; then
	OUTFILE="report.${FORMAT}"
fi

echo "Using params:  type:$TYPE start:$START end:$END format:$FORMAT criterionId:$CRITERION_ID groupById:$GROUPBY_ID outfile:$OUTFILE"


# Login
wget -O /tmp/sessionId --no-check-certificate "https://localhost:8443/loginservlet?adminPw=$PASSWORD"
export SESSIONID=`cat /tmp/sessionId`


# Generate report based upon params
wget -O "${OUTFILE}" --no-check-certificate "https://localhost:8443/reports?session=$SESSIONID&name=$TYPE&type=$FORMAT&page=0&flush=false&start=$START&end=$END&criterionId=$CRITERION_ID&groupById=$GROUPBY_ID"

