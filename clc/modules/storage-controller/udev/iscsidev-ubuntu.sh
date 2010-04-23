#!/bin/sh

BUS=${1}
HOST=${BUS%%:*}

[ -e /sys/class/iscsi_host ] || exit 1

#this location is kernel/distro dependent.
file="/sys/class/iscsi_host/host${HOST}/device/session*/iscsi_session*/session*/targetname"

target_name=$(cat ${file})

if [ -z "${target_name}" ]; then
   exit 1
fi

check_target_name=${target_name%%:*}
if [ $check_target_name = "iqn.2001-05.com.equallogic" ]; then
    target_name=`echo "${target_name#${check_target_name}:}"`
else
   exit 1
fi

echo "${target_name}"
