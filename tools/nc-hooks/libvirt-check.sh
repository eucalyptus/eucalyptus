#!/bin/bash
# 
# This hook is executed every time NC is about to perform a
# request to libvirtd.
#
# The hook checks if libvirtd process is running and starts it
# if it is not. This hook should only be installed on systems 
# where libvirtd daemon is not monitored via some other means.

event=$1
euca_home=$2

# only respond to euca-nc-pre-hyp-check events
if [ "$event" != "euca-nc-pre-hyp-check" ] ; then
    exit 0;
fi

# expect the first parameter to be Eucalyptus home
if [ "$euca_home" == "" ] ; then
    exit 0;
fi

log_file="${euca_home}/var/log/eucalyptus/nc-hooks.log"
rootwrap="${euca_home}/usr/lib/eucalyptus/euca_rootwrap"

if [ ! -x "$rootwrap" ] ; then
    exit 0;
fi

if [ -x /etc/init.d/libvirtd ] ; then
    ctrl_cmd="$rootwrap /etc/init.d/libvirtd"
else
    exit 0;
fi

if ! $ctrl_cmd status >/dev/null 2>&1 ; then
    if ! $ctrl_cmd restart >/dev/null 2>&1 ; then
	echo `date` "ERROR failed to restart libvirt daemon" >>$log_file
    else
	if ! $ctrl_cmd status >/dev/null 2>&1 ; then
	    echo `date` "ERROR restarted libvirt daemon not available" >>$log_file
	else
	    echo `date` "INFO  restarted libvirt daemon" >>$log_file
	fi
    fi
fi

# always return success, so hypervisor check may proceed
exit 0;
