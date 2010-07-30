/*
Copyright (c) 2009  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#ifndef INCLUDE_EUCALYPTUS_H
#define INCLUDE_EUCALYPTUS_H

/* environment variable set at startup */
#define EUCALYPTUS_ENV_VAR_NAME  "EUCALYPTUS"

/* file paths relative to $EUCALYPTUS */
#define EUCALYPTUS_CONF_LOCATION   "%s/etc/eucalyptus/eucalyptus.conf"
#define EUCALYPTUS_CONF_OVERRIDE_LOCATION   "%s/etc/eucalyptus/eucalyptus.local.conf"
#define EUCALYPTUS_ROOTWRAP        "%s/usr/lib/eucalyptus/euca_rootwrap"
#define EUCALYPTUS_ADD_KEY         "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/add_key.pl %s/usr/lib/eucalyptus/euca_mountwrap"
#define EUCALYPTUS_GEN_LIBVIRT_XML "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/gen_libvirt_xml"
#define EUCALYPTUS_GEN_KVM_LIBVIRT_XML "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/gen_kvm_libvirt_xml"
#define EUCALYPTUS_GET_XEN_INFO    "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/get_xen_info"
#define EUCALYPTUS_GET_KVM_INFO    "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/get_sys_info"
#define EUCALYPTUS_DISK_CONVERT    "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/partition2disk"
#define EUCALYPTUS_VIRSH           "%s/usr/lib/eucalyptus/euca_rootwrap virsh"
#define EUCALYPTUS_ROOTWRAP           "%s/usr/lib/eucalyptus/euca_rootwrap"
#define EUCALYPTUS_DETACH           "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/detach.pl"
#define EUCALYPTUS_XM           "sudo xm"

#define EUCALYPTUS_CONNECT_ISCSI    "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/connect_iscsitarget.pl"
#define EUCALYPTUS_DISCONNECT_ISCSI "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/disconnect_iscsitarget.pl"
#define EUCALYPTUS_GET_ISCSI "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/get_iscsitarget.pl"

#define NC_NET_PATH_DEFAULT        "%s/var/run/eucalyptus/net"
#define CC_NET_PATH_DEFAULT        "%s/var/run/eucalyptus/net"

/* various defaults */
#define NC_NET_PORT_DEFAULT      1976
#define CC_NET_PORT_DEFAULT      1976

/* names of variables in the configuration file */
#define CONFIG_MAX_MEM   "MAX_MEM"
#define CONFIG_MAX_DISK  "MAX_DISK"
#define CONFIG_MAX_CORES "MAX_CORES"
#define REGISTERED_PATH  "REGISTERED_PATH"
#define INSTANCE_PATH    "INSTANCE_PATH"
#define CONFIG_VNET_PORT "VNET_PORT"
#define CONFIG_VNET_DHCPDAEMON "VNET_DHCPDAEMON"
#define CONFIG_VNET_PRIVINTERFACE "VNET_PRIVINTERFACE"
#define CONFIG_NC_SERVICE "NC_SERVICE"
#define CONFIG_NC_PORT "NC_PORT"
#define CONFIG_NODES "NODES"
#define CONFIG_HYPERVISOR "HYPERVISOR"
#define CONFIG_NC_CACHE_SIZE "NC_CACHE_SIZE"
#define CONFIG_NC_WORK_SIZE "NC_WORK_SIZE"
#define CONFIG_NC_SWAP_SIZE "SWAP_SIZE"
#define CONFIG_SAVE_INSTANCES "MANUAL_INSTANCES_CLEANUP"
#define CONFIG_CONCURRENT_DISK_OPS "CONCURRENT_DISK_OPS"
#define CONFIG_USE_VIRTIO_NET "USE_VIRTIO_NET"
#define CONFIG_USE_VIRTIO_DISK "USE_VIRTIO_DISK"
#define CONFIG_USE_VIRTIO_ROOT "USE_VIRTIO_ROOT"

/* name of the administrative user within Eucalyptus */
#define EUCALYPTUS_ADMIN "eucalyptus"

/* system limit defaults */
#define MAXNODES 1024
#define MAXINSTANCES 2048
#define MAXLOGFILESIZE 32768000
#define EUCA_MAX_GROUPS 64
#define EUCA_MAX_VOLUMES 256
#define EUCA_MAX_DEVMAPS 64
#define EUCA_MAX_PATH 4096
#define DEFAULT_NC_CACHE_SIZE 999999 // in MB
#define DEFAULT_NC_WORK_SIZE  999999 // in MB
#define DEFAULT_SWAP_SIZE 512 /* in MB */
#define MAX_PATH_SIZE 4096 // TODO: remove

#define MEGABYTE 1048576
#define OK 0
#define ERROR 1
#define ERROR_FATAL 1
#define ERROR_RETRY -1

typedef enum instance_states_t { // these must match instance_sate_names[] below!
    /* the first 7 should match libvirt */
    NO_STATE = 0, 
    RUNNING,
    BLOCKED,
    PAUSED,
    SHUTDOWN,
    SHUTOFF,
    CRASHED,

    /* start-time states */
    STAGING,
    BOOTING,
    CANCELED,

    /* state after running */
    BUNDLING_SHUTDOWN,
    BUNDLING_SHUTOFF,

    /* the only three states reported to CLC */
    PENDING,  /* staging in data, starting to boot, failed to boot */ 
    EXTANT,   /* guest OS booting, running, shutting down, cleaning up state */
    TEARDOWN, /* a marker for a terminated domain, one not taking up resources */

    TOTAL_STATES
} instance_states;

static char * instance_state_names[] = {
    "Unknown",
    "Running",
    "Waiting",
    "Paused",
    "Shutdown",
    "Shutoff",
    "Crashed",

    "Staging",
    "Booting",
    "Canceled",

	"Bundling-Shutdown",
    "Bundling-Shutoff",

    "Pending",
    "Extant",
    "Teardown"
};

typedef enum bundling_progress_t {
    NOT_BUNDLING = 0,
	BUNDLING_IN_PROGRESS,
	BUNDLING_SUCCESS,
        BUNDLING_FAILED,
        BUNDLING_CANCELLED
} bundling_progress; 

static char * bundling_progress_names[] = {
	"none",
	"bundling",
	"succeeded",
	"failed",
        "cancelled"
};

#endif

