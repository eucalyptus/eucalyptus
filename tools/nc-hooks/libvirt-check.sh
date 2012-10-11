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
debug_log=/dev/null 

# only respond to euca-nc-pre-hyp-check events
if [ "$event" != "euca-nc-pre-hyp-check" ] ; then
    echo `date` "ignoring event other than euca-nc-pre-hyp-check" >>$debug_log
    exit 0;
fi

# expect the first parameter to be Eucalyptus home
if [ "$euca_home" == "" ] ; then
    echo `date` "ignoring invocation with no Euca home set" >>$debug_log
    exit 0;
fi

hook_log="${euca_home}/var/log/eucalyptus/nc-hooks.log"
rootwrap="${euca_home}/usr/lib/eucalyptus/euca_rootwrap"

# ensure we have the privilege-elevating script
if [ ! -x "$rootwrap" ] ; then
    echo `date` "ignoring invocation on system without executable $rootwrap" >>$debug_log
    exit 0;
fi

# find out what the libvirt daemon service is called on this distro
if [ -x /etc/init.d/libvirtd ] ; then
    service_name="libvirtd"
elif [ -x /etc/init.d/libvirt-bin ] ; then
    service_name="libvirt-bin"
else
    echo `date` "ignoring invocation on system where we cannot find libvirt" >>$debug_log
    exit 0;
fi

# prefer using 'service' to init script when available
if [ -x /sbin/service ] ; then
    ctrl_cmd="$rootwrap /sbin/service ${service_name}"
elif [ -x /usr/sbin/service ] ; then
    ctrl_cmd="$rootwrap /usr/sbin/service ${service_name}"
else
    ctrl_cmd="$rootwrap /etc/init.d/${service_name}"
fi
echo `date` "will use '$ctrl_cmd [status|restart]" >>$debug_log

# see if livbirt daemon is running and try to 'restart' if not
if ! $ctrl_cmd status >/dev/null 2>&1 ; then
    if ! $ctrl_cmd restart >/dev/null 2>&1 ; then
	echo `date` "ERROR failed to restart libvirt daemon" >>$hook_log
	echo `date` "failed to restart libvirt daemon" >>$debug_log
    else
	if ! $ctrl_cmd status >/dev/null 2>&1 ; then
	    echo `date` "ERROR the restarted libvirt daemon not available" >>$hook_log
	    echo `date` "the restarted libvirt daemon not available" >>$debug_log
	else
	    echo `date` "INFO  restarted libvirt daemon" >>$hook_log
	    echo `date` "restarted libvirt daemon" >>$debug_log
	fi
    fi
else
    echo `date` "libvirt daemon OK" >>$debug_log
fi

# always return success, so hypervisor check may proceed
exit 0;
