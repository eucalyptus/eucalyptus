/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

#ifndef INCLUDE_EUCALYPTUS_H
#define INCLUDE_EUCALYPTUS_H

// Maybe we should have no defaults, but I will put them here for the moment.
#ifndef LIBDIR
#define LIBDIR "/usr/lib"
#endif
#ifndef SYSCONFDIR
#define SYSCONFDIR "/etc"
#endif
#ifndef DATADIR
#define DATADIR	"/usr/share"
#endif
#ifndef LIBEXECDIR
#define LIBEXECDIR	"/usr/lib"
#endif
#ifndef SBINDIR
#define SBINDIR	"/usr/sbin"
#endif
#ifndef LOCALSTATEDIR
#define LOCALSTATEDIR	"/var"
#endif
#ifndef HELPERDIR
#define HELPERDIR	DATADIR
#endif

/* environment variable set at startup */
#define EUCALYPTUS_ENV_VAR_NAME  "EUCALYPTUS"

/* file paths relative to $EUCALYPTUS */
#define EUCALYPTUS_DATA_DIR        "%s" DATADIR "/eucalyptus"
#define EUCALYPTUS_CONF_DIR        "%s" SYSCONFDIR "/eucalyptus"
#define EUCALYPTUS_LIB_DIR         "%s" LIBDIR "/eucalyptus"
#define EUCALYPTUS_LIBEXEC_DIR     "%s" LIBEXECDIR "/eucalyptus"
#define EUCALYPTUS_RUN_DIR         "%s" LOCALSTATEDIR "/run/eucalyptus"
#define EUCALYPTUS_SBIN_DIR        "%s" SBINDIR
#define EUCALYPTUS_STATE_DIR       "%s" LOCALSTATEDIR "/lib/eucalyptus"
#define EUCALYPTUS_LOG_DIR         "%s" LOCALSTATEDIR "/log/eucalyptus"

#define EUCALYPTUS_FAULT_DIR            EUCALYPTUS_DATA_DIR "/faults"
#define EUCALYPTUS_CUSTOM_FAULT_DIR     EUCALYPTUS_CONF_DIR "/faults"

// scripts ... to Fedora / RHEL they are the same as indirectly
// executed binaries.  We need a separate variable in case people
// want them in /usr/share, though.
#define EUCALYPTUS_HELPER_DIR      "%s" HELPERDIR "/eucalyptus"

// Java stuff, oddly named
#define EUCA_ETC_DIR               EUCALYPTUS_CONF_DIR "/cloud.d"
#define EUCA_SCRIPT_DIR            EUCA_ETC_DIR "/scripts"
#define EUCALYPTUS_JAVA_LIB_DIR    EUCALYPTUS_DATA_DIR
#define EUCA_CLASSCACHE_DIR        EUCALYPTUS_RUN_DIR "/classcache"

#define EUCALYPTUS_KEYS_DIR        EUCALYPTUS_STATE_DIR "/keys"
#define EUCALYPTUS_CONF_LOCATION   EUCALYPTUS_CONF_DIR "/eucalyptus.conf"
#define EUCALYPTUS_CONF_OVERRIDE_LOCATION   EUCALYPTUS_CONF_DIR "/eucalyptus.local.conf"
#define EUCALYPTUS_LIBVIRT_XSLT    EUCALYPTUS_CONF_DIR "/libvirt.xsl"
#define EUCALYPTUS_VOLUME_XML_PATH_FORMAT "%s/%s.xml"
#define EUCALYPTUS_VOLUME_LIBVIRT_XML_PATH_FORMAT "%s/%s-libvirt.xml"
#define EUCALYPTUS_ROOTWRAP        EUCALYPTUS_LIBEXEC_DIR "/euca_rootwrap"
#define EUCALYPTUS_ADD_KEY         EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/add_key.pl " EUCALYPTUS_LIBEXEC_DIR "/euca_mountwrap"
#define EUCALYPTUS_GEN_LIBVIRT_XML EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/gen_libvirt_xml"
#define EUCALYPTUS_GEN_KVM_LIBVIRT_XML EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/gen_kvm_libvirt_xml"
#define EUCALYPTUS_GET_XEN_INFO    EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/get_xen_info"
#define EUCALYPTUS_GET_KVM_INFO    EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/get_sys_info"
#define EUCALYPTUS_DISK_CONVERT    EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/partition2disk"
#define EUCALYPTUS_VIRSH           EUCALYPTUS_ROOTWRAP " virsh"
#define EUCALYPTUS_DETACH          EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/detach.pl"

#define EUCALYPTUS_XM           "sudo xm"

#define EUCALYPTUS_CONNECT_ISCSI    EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/connect_iscsitarget.pl"
#define EUCALYPTUS_DISCONNECT_ISCSI EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/disconnect_iscsitarget.pl"
#define EUCALYPTUS_GET_ISCSI        EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/get_iscsitarget.pl"
#define EUCALYPTUS_NC_CHECK_BUCKET "euca-check-bucket"  // can be overriden from eucalyptus.conf
#define EUCALYPTUS_NC_BUNDLE_UPLOAD "euca-bundle-upload"    // can be overriden from eucalyptus.conf
#define EUCALYPTUS_NC_DELETE_BUNDLE "euca-delete-bundle"    // can be overriden from eucalyptus.conf
#define EUCALYPTUS_NC_HOOKS_DIR     EUCALYPTUS_CONF_DIR "/nc-hooks"

#define NC_NET_PATH_DEFAULT         EUCALYPTUS_RUN_DIR "/net"
#define CC_NET_PATH_DEFAULT         EUCALYPTUS_RUN_DIR "/net"

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
#define CONFIG_NC_OVERHEAD_SIZE "NC_WORK_OVERHEAD_SIZE"
#define CONFIG_NC_SWAP_SIZE "SWAP_SIZE"
#define CONFIG_SAVE_INSTANCES "MANUAL_INSTANCES_CLEANUP"
#define CONFIG_DISABLE_KEY_INJECTION "DISABLE_KEY_INJECTION"
#define CONFIG_CONCURRENT_DISK_OPS "CONCURRENT_DISK_OPS"
#define CONFIG_CONCURRENT_CLEANUP_OPS "CONCURRENT_CLEANUP_OPS"
#define CONFIG_DISABLE_SNAPSHOTS "DISABLE_CACHE_SNAPSHOTS"
#define CONFIG_USE_VIRTIO_NET "USE_VIRTIO_NET"
#define CONFIG_USE_VIRTIO_DISK "USE_VIRTIO_DISK"
#define CONFIG_USE_VIRTIO_ROOT "USE_VIRTIO_ROOT"
#define CONFIG_NC_BUNDLE_UPLOAD "NC_BUNDLE_UPLOAD_PATH"
#define CONFIG_NC_CHECK_BUCKET "NC_CHECK_BUCKET_PATH"
#define CONFIG_NC_DELETE_BUNDLE "NC_DELETE_BUNDLE_PATH"
#define CONFIG_NC_STAGING_CLEANUP_THRESHOLD     "NC_STAGING_CLEANUP_THRESHOLD"
#define CONFIG_NC_BOOTING_CLEANUP_THRESHOLD     "NC_BOOTING_CLEANUP_THRESHOLD"
#define CONFIG_NC_BUNDLING_CLEANUP_THRESHOLD    "NC_BUNDLING_CLEANUP_THRESHOLD"
#define CONFIG_NC_CREATEIMAGE_CLEANUP_THRESHOLD "NC_CREATEIMAGE_CLEANUP_THRESHOLD"
#define CONFIG_NC_TEARDOWN_STATE_DURATION       "NC_TEARDOWN_STATE_DURATION"

/* name of the administrative user within Eucalyptus */
#define EUCALYPTUS_ADMIN "eucalyptus"

// percent at which a C component will log a 'very low on disk space' fault
#define DISK_TOO_LOW_PERCENT 10

/* system limit defaults */
#define MAXNODES 1024
#define MAXINSTANCES_PER_CC 2048
#define MAXINSTANCES_PER_NC 256
#define MAXLOGFILESIZE 10485760
#define EUCA_MAX_GROUPS 64
#define EUCA_MAX_VOLUMES 27
#define EUCA_MAX_VBRS 64
#define EUCA_MAX_PATH 4096
#define EUCA_MAX_PARTITIONS 32  // partitions per disk
#define EUCA_MAX_DISKS 26       // disks per bus: sd[a-z]
#define MAX_PATH_SIZE 4096      // TODO: remove

// NC hook events
#define NC_EVENT_PRE_INIT      "euca-nc-pre-init"   // p1: eucalyptusHome
#define NC_EVENT_POST_INIT     "euca-nc-post-init"  // p1: eucalyptusHome
#define NC_EVENT_PRE_HYP_CHECK "euca-nc-pre-hyp-check"  // p1: eucalyptusHome
#define NC_EVENT_PRE_BOOT      "euca-nc-pre-boot"   // p1: eucalyptusHome p2: instancePath
#define NC_EVENT_ADOPTING      "euca-nc-pre-adopt"  // p1: eucalyptusHome p2: instancePath
#define NC_EVENT_PRE_CLEAN     "euca-nc-pre-clean"  // p1: eucalyptusHome p2: instancePath
#define NC_EVENT_PRE_ATTACH    "euca-nc-pre-attach" // p1: eucalyptusHome p2: volumeXmlPath
#define NC_EVENT_POST_DETACH   "euca-nc-post-detach"    // p1: eucalyptusHome p2: volumeXmlPath

// Timeout values (suggestions)
#define ATTACH_VOL_TIMEOUT_SECONDS	180 //CC Timeout for an doAttachVolume() operation on the NC. In failure cases NC may take 60 sec.
#define DETACH_VOL_TIMEOUT_SECONDS	180 //CC Timeout for an doDetachVolume() operation on the NC. In failure cases NC may take 60 sec.

#define MEGABYTE 1048576

// return codes
#define OK 0
#define ERROR 1
#define ERROR_FATAL 1
#define ERROR_RETRY -1
#define READER 2
#define WRITER 3

typedef enum instance_states_t {    // these must match instance_sate_names[] below!
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

    /* createImage states */
    CREATEIMAGE_SHUTDOWN,
    CREATEIMAGE_SHUTOFF,

    /* the only three states reported to CLC */
    PENDING,                    /* staging in data, starting to boot, failed to boot */
    EXTANT,                     /* guest OS booting, running, shutting down, cleaning up state */
    TEARDOWN,                   /* a marker for a terminated domain, one not taking up resources */

    TOTAL_STATES
} instance_states;

static char *instance_state_names[] = {
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
    "CreateImage-Shutdown",
    "CreateImage-Shutoff",

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

static char *bundling_progress_names[] = {
    "none",
    "bundling",
    "succeeded",
    "failed",
    "cancelled"
};

typedef enum createImage_progress_t {
    NOT_CREATEIMAGE = 0,
    CREATEIMAGE_IN_PROGRESS,
    CREATEIMAGE_SUCCESS,
    CREATEIMAGE_FAILED,
    CREATEIMAGE_CANCELLED
} createImage_progress;

static char *createImage_progress_names[] = {
    "none",
    "creating",
    "succeeded",
    "failed",
    "cancelled"
};

#ifndef EUCA_FREE
#define EUCA_FREE(_x)  \
{                      \
	free((_x));        \
	(_x) = NULL;       \
}
#endif /* ! EUCA_FREE */
#endif
