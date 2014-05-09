#!/bin/sh
#
# Copyright 2011-2014 Eucalyptus Systems, Inc.
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the following
# conditions are met:
#
#   Redistributions of source code must retain the above copyright notice,
#   this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in the
#   documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

BUS=${1}
HOST=${BUS%%:*}

[ -e /sys/class/iscsi_host ] || exit 1

#this location is kernel/distro dependent.
file="/sys/class/iscsi_host/host${HOST}/device/session*/iscsi_session*/session*/targetname"

if [ ! -e ${file} ]; then
   file="/sys/class/iscsi_host/host${HOST}/device/session*/iscsi_session*/targetname"
fi

target_name=$(cat ${file})

if [ -z "${target_name}" ]; then
   exit 1
fi

check_target_name=${target_name%%:*}
if [ $check_target_name = "iqn.2001-05.com.equallogic" ] || [ $check_target_name = "iqn.1992-08.com.netapp" ] || [ $check_target_name = "iqn.1992-04.com.emc" ]; then
    target_name=`echo "${target_name#${check_target_name}:}"`
else
   exit 1
fi

echo "${target_name}"
