#!/bin/bash

# Copyright 2009-2015 Ent. Services Development Corporation LP
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the
# following conditions are met:
#
#   Redistributions of source code must retain the above copyright
#   notice, this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer
#   in the documentation and/or other materials provided with the
#   distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
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

# When enabled and if virt-inspector2 command is available,
# this hook will use it to analyze the root disk of the 
# instance just prior to boting, will save results in an XML
# file in instance directory, and will append the value of
# <product_name> element found in the output to the file that
# gets prepended to data returned for GetConsoleOutput request.
#
# HOW TO USE:
#
# - Place this script into $EUCALYPTUS/etc/eucalyptus/nc-hooks
#   directory on all NCs and make it executable by 'eucalyptus'
# - Install the virt-inspector2 command

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

# we only care about pre-boot event
if [ "$event" != "euca-nc-pre-boot" ] ; then
    exit 0;
fi

if [ "$inst_home" == "" ] ; then
    echo "euca-nc-pre-boot called without instance home path"
    exit 1;
fi

# Euca command used for elevating privileges
rootwrap=${euca_home}/usr/lib/eucalyptus/euca_rootwrap

# Log file for debugging NC hooks
log_file=${euca_home}/var/log/eucalyptus/nc-hooks.log

# File the contents of which appear first in 
# GetConsoleOutput results. This hook uses is
# to report the operating system of the instance.
console_file=${inst_home}/console.append.log

# File in instance directory where this hook puts
# virt-inspector2 output. It gets cleaned up when 
# instance is terminated or stopped.
inspector_results_file=${inst_home}/virt-inspector.xml

# The file (a symbolink link to a block device) of 
# the root disk of the instance.
root_disk_file=$(ls ${inst_home}/link-to-*da)

# If present on the system, this is the path to
# virt-inspector2 command.
inspector_path=`which virt-inspector2`

if [ -e $inspector_path -a -e $root_disk_file ] ; then
    $rootwrap $inspector_path $root_disk_file >$inspector_results_file
    os=$(grep -oPm1 "(?<=<product_name>)[^<]+" $inspector_results_file)
    if [ "$os" != "" ] ; then
	product_string="OS: $os"
	echo `date` "$product_string" >>$console_file
    fi
fi

echo `date` "$event $inst_home '$product_string'" >>$log_file

exit 0;
