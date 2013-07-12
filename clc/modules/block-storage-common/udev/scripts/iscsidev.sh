#!/bin/sh

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
