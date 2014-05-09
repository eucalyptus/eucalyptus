// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

#ifndef _INCLUDE_EUCALYPTUS_H_
#define _INCLUDE_EUCALYPTUS_H_

//!
//! @file util/eucalyptus.h
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdint.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifdef CFG_USE_INLINE
#define INLINE
#else /* ! CFG_USE_INLINE */
#define INLINE                                   inline
#endif /* ! CFG_USE_INLINE */

#include <stdint.h>

// Maybe we should have no defaults, but I will put them here for the moment.
#ifndef LIBDIR
#define LIBDIR                                   "/usr/lib" //!< default user library directory path
#endif /* ! LIBDIR */

#ifndef SYSCONFDIR
#define SYSCONFDIR                               "/etc" //!< default etc directory path
#endif /* ! SYSCONFDIR */

#ifndef DATADIR
#define DATADIR                                  "/usr/share"   //!< default user share directory path
#endif /* ! DATADIR */

#ifndef LIBEXECDIR
#define LIBEXECDIR                               "/usr/lib" //!< default user exec library directory path
#endif /* ! LIBEXECDIR */

#ifndef SBINDIR
#define SBINDIR                                  "/usr/sbin"    //!< default user standard binary directory path
#endif /* ! SBINDIR */

#ifndef LOCALSTATEDIR
#define LOCALSTATEDIR                            "/var" //!< default var directory path
#endif /* ! LOCALSTATEDIR */

#ifndef HELPERDIR
#define HELPERDIR                                DATADIR    //!< default help directory path
#endif /* ! HELPERDIR */

//! environment variable set at startup
#define EUCALYPTUS_ENV_VAR_NAME                  "EUCALYPTUS"   //!< Eucalyptus environment variable name
#define EUCALYPTUS_USER_ENV_VAR_NAME             "EUCA_USER"    //!< Eucalyptus unix user environment variable name

//! @{
//! @name file paths relative to $EUCALYPTUS

#define EUCALYPTUS_DATA_DIR                      "%s" DATADIR "/eucalyptus"
#define EUCALYPTUS_CONF_DIR                      "%s" SYSCONFDIR "/eucalyptus"
#define EUCALYPTUS_LIB_DIR                       "%s" LIBDIR "/eucalyptus"
#define EUCALYPTUS_LIBEXEC_DIR                   "%s" LIBEXECDIR "/eucalyptus"
#define EUCALYPTUS_RUN_DIR                       "%s" LOCALSTATEDIR "/run/eucalyptus"
#define EUCALYPTUS_SBIN_DIR                      "%s" SBINDIR
#define EUCALYPTUS_STATE_DIR                     "%s" LOCALSTATEDIR "/lib/eucalyptus"
#define EUCALYPTUS_LOG_DIR                       "%s" LOCALSTATEDIR "/log/eucalyptus"
#define EUCALYPTUS_HELPER_DIR                    "%s" HELPERDIR "/eucalyptus"

//! @}

//! @{
//! @name Java related paths

#define EUCA_ETC_DIR                             EUCALYPTUS_CONF_DIR "/cloud.d"
#define EUCA_SCRIPT_DIR                          EUCA_ETC_DIR "/scripts"
#define EUCALYPTUS_JAVA_LIB_DIR                  EUCALYPTUS_DATA_DIR
#define EUCA_CLASSCACHE_DIR                      EUCALYPTUS_RUN_DIR "/classcache"

//! @}

#define EUCALYPTUS_FAULT_DIR                     EUCALYPTUS_DATA_DIR "/faults"
#define EUCALYPTUS_CUSTOM_FAULT_DIR              EUCALYPTUS_CONF_DIR "/faults"
#define EUCALYPTUS_KEYS_DIR                      EUCALYPTUS_STATE_DIR "/keys"
#define EUCALYPTUS_NC_STATE_FILE                 EUCALYPTUS_STATE_DIR "/nc_state.xml"
#define EUCALYPTUS_CONF_LOCATION                 EUCALYPTUS_CONF_DIR "/eucalyptus.conf"
#define EUCALYPTUS_CONF_OVERRIDE_LOCATION        EUCALYPTUS_CONF_DIR "/eucalyptus.local.conf"
#define EUCALYPTUS_LIBVIRT_XSLT                  EUCALYPTUS_CONF_DIR "/libvirt.xsl"
#define EUCALYPTUS_VOLUME_XML_PATH_FORMAT        "%s/%s.xml"
#define EUCALYPTUS_VOLUME_LIBVIRT_XML_PATH_FORMAT "%s/%s-libvirt.xml"
#define EUCALYPTUS_ROOTWRAP                      EUCALYPTUS_LIBEXEC_DIR "/euca_rootwrap"
#define EUCALYPTUS_ADD_KEY                       EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/add_key.pl " EUCALYPTUS_LIBEXEC_DIR "/euca_mountwrap"
#define EUCALYPTUS_GEN_LIBVIRT_XML               EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/gen_libvirt_xml"
#define EUCALYPTUS_GEN_KVM_LIBVIRT_XML           EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/gen_kvm_libvirt_xml"
#define EUCALYPTUS_GET_XEN_INFO                  EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/get_xen_info"
#define EUCALYPTUS_GET_KVM_INFO                  EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/get_sys_info"
#define EUCALYPTUS_DISK_CONVERT                  EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/partition2disk"
#define EUCALYPTUS_VIRSH                         EUCALYPTUS_ROOTWRAP " virsh"
#define EUCALYPTUS_DETACH                        EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/detach.pl"
#define EUCALYPTUS_GENERATE_MIGRATION_KEYS       EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/generate-migration-keys.sh"
#define EUCALYPTUS_AUTHORIZE_MIGRATION_KEYS      EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/authorize-migration-keys.pl"

#define EUCALYPTUS_CONNECT_ISCSI                 EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/connect_iscsitarget.pl"
#define EUCALYPTUS_DISCONNECT_ISCSI              EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/disconnect_iscsitarget.pl"
#define EUCALYPTUS_GET_ISCSI                     EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/get_iscsitarget.pl"
#define EUCALYPTUS_NC_CHECK_BUCKET               "euca-check-bucket"    //!< can be overriden from eucalyptus.conf
#define EUCALYPTUS_NC_BUNDLE_UPLOAD              "euca-bundle-upload"   //!< can be overriden from eucalyptus.conf
#define EUCALYPTUS_NC_DELETE_BUNDLE              "euca-delete-bundle"   //!< can be overriden from eucalyptus.conf
#define EUCALYPTUS_NC_HOOKS_DIR                  EUCALYPTUS_CONF_DIR "/nc-hooks"

#define NC_NET_PATH_DEFAULT                      EUCALYPTUS_RUN_DIR "/net"
#define CC_NET_PATH_DEFAULT                      EUCALYPTUS_RUN_DIR "/net"

#define EUCALYPTUS_XM                            "sudo xm"

//! @{
//! @name various default communication ports

#define NC_NET_PORT_DEFAULT                      1976   //!< NC network communication port
#define CC_NET_PORT_DEFAULT                      1976   //!< network CC communication port

//! @}

//! @{
//! @name names of variables in the configuration file

#define CONFIG_MAX_MEM                          "MAX_MEM"
#define CONFIG_MAX_DISK                         "MAX_DISK"
#define CONFIG_MAX_CORES                        "MAX_CORES"
#define REGISTERED_PATH                         "REGISTERED_PATH"
#define INSTANCE_PATH                           "INSTANCE_PATH"
#define CONFIG_VNET_PORT                        "VNET_PORT"
#define CONFIG_VNET_DHCPDAEMON                  "VNET_DHCPDAEMON"
#define CONFIG_VNET_PRIVINTERFACE               "VNET_PRIVINTERFACE"
#define CONFIG_NC_SERVICE                       "NC_SERVICE"
#define CONFIG_NC_PORT                          "NC_PORT"
#define CONFIG_NODES                            "NODES"
#define CONFIG_HYPERVISOR                       "HYPERVISOR"
#define CONFIG_NC_CACHE_SIZE                    "NC_CACHE_SIZE"
#define CONFIG_NC_WORK_SIZE                     "NC_WORK_SIZE"
#define CONFIG_NC_OVERHEAD_SIZE                 "NC_WORK_OVERHEAD_SIZE"
#define CONFIG_NC_SWAP_SIZE                     "SWAP_SIZE"
#define CONFIG_SAVE_INSTANCES                   "MANUAL_INSTANCES_CLEANUP"
#define CONFIG_DISABLE_KEY_INJECTION            "DISABLE_KEY_INJECTION"
#define CONFIG_CONCURRENT_DISK_OPS              "CONCURRENT_DISK_OPS"
#define CONFIG_SC_REQUEST_TIMEOUT               "SC_REQUEST_TIMEOUT"
#define CONFIG_CONCURRENT_CLEANUP_OPS           "CONCURRENT_CLEANUP_OPS"
#define CONFIG_DISABLE_SNAPSHOTS                "DISABLE_CACHE_SNAPSHOTS"
#define CONFIG_USE_VIRTIO_NET                   "USE_VIRTIO_NET"
#define CONFIG_USE_VIRTIO_DISK                  "USE_VIRTIO_DISK"
#define CONFIG_USE_VIRTIO_ROOT                  "USE_VIRTIO_ROOT"
#define CONFIG_NC_BUNDLE_UPLOAD                 "NC_BUNDLE_UPLOAD_PATH"
#define CONFIG_NC_CHECK_BUCKET                  "NC_CHECK_BUCKET_PATH"
#define CONFIG_NC_DELETE_BUNDLE                 "NC_DELETE_BUNDLE_PATH"
#define CONFIG_NC_STAGING_CLEANUP_THRESHOLD     "NC_STAGING_CLEANUP_THRESHOLD"
#define CONFIG_NC_BOOTING_CLEANUP_THRESHOLD     "NC_BOOTING_CLEANUP_THRESHOLD"
#define CONFIG_NC_BUNDLING_CLEANUP_THRESHOLD    "NC_BUNDLING_CLEANUP_THRESHOLD"
#define CONFIG_NC_CREATEIMAGE_CLEANUP_THRESHOLD "NC_CREATEIMAGE_CLEANUP_THRESHOLD"
#define CONFIG_NC_TEARDOWN_STATE_DURATION       "NC_TEARDOWN_STATE_DURATION"
#define CONFIG_NC_MIGRATION_READY_THRESHOLD     "NC_MIGRATION_READY_THRESHOLD"
#define CONFIG_SHUTDOWN_GRACE_PERIOD_SEC        "NC_SHUTDOWN_GRACE_PERIOD_SEC"
#define CONFIG_ENABLE_WS_SECURITY				"ENABLE_WS_SECURITY"
#define CONFIG_WALRUS_DOWNLOAD_MAX_ATTEMPTS     "WALRUS_DOWNLOAD_MAX_ATTEMPTS"

//! @}

//! name of the administrative user within Eucalyptus
#define EUCALYPTUS_ADMIN                         "eucalyptus"

//! percent at which a C component will log a 'very low on disk space' fault
#define DISK_TOO_LOW_PERCENT                     10

//! @{
//! @name system limit defaults */

#define MAXNODES                                     1024
#define MAXINSTANCES_PER_CC                          2048
#define MAXINSTANCES_PER_NC                           256
#define MAXLOGFILESIZE                          104857600
#define EUCA_MAX_GROUPS                                64
#define EUCA_MAX_VOLUMES                               27
#define EUCA_MAX_VBRS                                  64   //!< Number of Virtual Boot Record supported
#define EUCA_MAX_PATH                                4096
#define EUCA_MAX_PARTITIONS                            32   //!< partitions per disk
#define EUCA_MAX_DISKS                                 26   //!< disks per bus: sd[a-z]
#define MAX_PATH_SIZE                                4096   //!< Maximum path string length @TODO: remove
#define MAXBUNDLES                               MAXINSTANCES_PER_NC

//! @}

//! @{
//! @name NC hook events

#define NC_EVENT_PRE_INIT                        "euca-nc-pre-init" //!< p1: eucalyptusHome
#define NC_EVENT_POST_INIT                       "euca-nc-post-init"    //!< p1: eucalyptusHome
#define NC_EVENT_PRE_HYP_CHECK                   "euca-nc-pre-hyp-check"    //!< p1: eucalyptusHome
#define NC_EVENT_PRE_BOOT                        "euca-nc-pre-boot" //!< p1: eucalyptusHome p2: instancePath
#define NC_EVENT_ADOPTING                        "euca-nc-pre-adopt"    //!< p1: eucalyptusHome p2: instancePath
#define NC_EVENT_PRE_CLEAN                       "euca-nc-pre-clean"    //!< p1: eucalyptusHome p2: instancePath
#define NC_EVENT_PRE_ATTACH                      "euca-nc-pre-attach"   //!< p1: eucalyptusHome p2: volumeXmlPath
#define NC_EVENT_POST_DETACH                     "euca-nc-post-detach"  //!< p1: eucalyptusHome p2: volumeXmlPath

//! @}

//! @{
//! @name Timeout values (suggestions)

#define ATTACH_VOL_TIMEOUT_SECONDS	180 //!< CC Timeout for an doAttachVolume() operation on the NC. In failure cases NC may take 60 sec.
#define DETACH_VOL_TIMEOUT_SECONDS	180 //!< CC Timeout for an doDetachVolume() operation on the NC. In failure cases NC may take 60 sec.

//! @}

#if 0
// DO NO USE.
// Will be removed at the end of 3.3.
// Use the euca_error_e enum instead.
#define OK                                       0
#define ERROR                                    1
#define ERROR_FATAL                              1
#define ERROR_RETRY                             -1
#define READER                                   2
#define WRITER                                   3
#endif /* 0 */

#define MEGABYTE                                 1048576

#ifndef _attribute_format_
#define _attribute_format_(index, first)         __attribute__ ((format (printf, index, first)))
#endif /* ! _attribute_format_ */

#ifndef _attribute_wur_
#define _attribute_wur_                          __attribute__ ((__warn_unused_result__))
#endif /* ! _attribute_wur_ */

#ifndef _attribute_noreturn_
#define _attribute_noreturn_                     __attribute__ ((noreturn))
#endif /* ! _attribute_noreturn_ */

#ifndef _attribute_packed_
#define _attribute_packed_                       __attribute__ ((packed))
#endif /* ! _attribute_packed_ */

#ifndef _attribute_nonnull_
#define _attribute_nonnull_                      __attribute__ ((nonnull))
#endif /* ! _attribute_nonnull_ */

#ifndef __attribute_nonnull__
#define __attribute_nonnull__(params)            __attribute__ ((nonnull params))
#endif /* ! _attribute_nonnull_ */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name Defines various unsigned bytes size holders
typedef uint8_t u8;
typedef uint16_t u16;
typedef uint32_t u32;
typedef uint64_t u64;
//! @}

//! @{
//! @name Defines various signed bytes size holders
typedef int8_t s8;
typedef int16_t s16;
typedef int32_t s32;
typedef int64_t s64;
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! defines various instance states
typedef enum instance_states_t {
    //! @{
    //! @name the first 7 should match libvirt
    NO_STATE = 0,
    RUNNING,
    BLOCKED,
    PAUSED,
    SHUTDOWN,
    SHUTOFF,
    CRASHED,
    //! @}

    //! @{
    //! @name start-time states
    STAGING,
    BOOTING,
    CANCELED,
    //! @}

    //! @{
    //! @name state after running
    BUNDLING_SHUTDOWN,
    BUNDLING_SHUTOFF,
    //! @}

    //! @{
    //! @name createImage states
    CREATEIMAGE_SHUTDOWN,
    CREATEIMAGE_SHUTOFF,
    //! @}

    //! @{
    //! @name the only three states reported to CLC
    PENDING,                           //!< staging in data, starting to boot, failed to boot
    EXTANT,                            //!< guest OS booting, running, shutting down, cleaning up state
    TEARDOWN,                          //!< a marker for a terminated domain, one not taking up resources
    //! @}

    TOTAL_STATES
} instance_states;

//! Defines the bundling task progress states
typedef enum bundling_progress_t {
    NOT_BUNDLING = 0,
    BUNDLING_IN_PROGRESS,
    BUNDLING_SUCCESS,
    BUNDLING_FAILED,
    BUNDLING_CANCELLED
} bundling_progress;

//! Defines the create image task progress states
typedef enum createImage_progress_t {
    NOT_CREATEIMAGE = 0,
    CREATEIMAGE_IN_PROGRESS,
    CREATEIMAGE_SUCCESS,
    CREATEIMAGE_FAILED,
    CREATEIMAGE_CANCELLED
} createImage_progress;

//! Enumeration of migration-related states
typedef enum migration_states_t {
    NOT_MIGRATING = 0,
    MIGRATION_PREPARING,
    MIGRATION_READY,
    MIGRATION_IN_PROGRESS,
    MIGRATION_CLEANING,
    TOTAL_MIGRATION_STATES
} migration_states;

//! Various Eucalyptus standard error code.
enum euca_error_e {
    EUCA_OK = 0,                       //!< Operation successful
    EUCA_ERROR = 1,                    //!< Generic operation error code
    EUCA_FATAL_ERROR = 2,              //!< Generic operation unrecoverable error code
    EUCA_NOT_FOUND_ERROR = 3,          //!< Searched item not found
    EUCA_MEMORY_ERROR = 4,             //!< Out of memory error
    EUCA_IO_ERROR = 5,                 //!< Input/Output error
    EUCA_HYPERVISOR_ERROR = 6,         //!< Error caused by the Hypervisor.
    EUCA_THREAD_ERROR = 7,             //!< Failure to initialize or start a thread.
    EUCA_DUPLICATE_ERROR = 8,          //!< Duplicate entry error
    EUCA_INVALID_ERROR = 9,            //!< Invalid argument error
    EUCA_OVERFLOW_ERROR = 10,          //!< Variable overflow error
    EUCA_UNSUPPORTED_ERROR = 11,       //!< Operation unsupported error
    EUCA_PERMISSION_ERROR = 12,        //!< Operation not permitted error
    EUCA_ACCESS_ERROR = 13,            //!< Access permission denied
    EUCA_NO_SPACE_ERROR = 14,          //!< No more space available in list or memory space
    EUCA_TIMEOUT_ERROR = 15,           //!< Execution of an operation timed out
    EUCA_SYSTEM_ERROR = 16,            //!< If any system execution has failed
    EUCA_LAST_ERROR,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! String value of each instance state enumeration entry
extern const char *instance_state_names[];

//! String value of each bundling progress state enumeration entry
extern const char *bundling_progress_names[];

//! String value of each create image progress state enumeration entry
extern const char *createImage_progress_names[];

//! String value of each migrate-related state enumeration entry
extern const char *migration_state_names[];

//! String value of each error enumeration entry
extern const char *euca_error_names[];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifndef EUCA_ERROR_NAME
//! Macro to convert a euca_error entry to a string value
#define EUCA_ERROR_NAME(_error)                  (((_error) > EUCA_LAST_ERROR) ? euca_error_names[EUCA_LAST_ERROR] : euca_error_names[(_error)])
#endif /* ! EUCA_ERROR_NAME */

#ifndef EUCA_ALLOC
//! Macro for fast (non-zeroed) memory allocation
#define EUCA_ALLOC(_nmemb, _size)                malloc((_nmemb) * (_size))
#endif /* ! EUCA_ALLOC */

#ifndef EUCA_ZALLOC
//! Macro for slow (zeroed) memory allocation
#define EUCA_ZALLOC(_nmemb, _size)               calloc((_nmemb), (_size))
#endif /* ! EUCA_ZALLOC */

#ifndef EUCA_REALLOC
//! Macro for fast (non-zeroed) memory allocation
#define EUCA_REALLOC(_ptr, _nmemb, _size)        realloc((_ptr), (_nmemb) * (_size))
#endif /* ! EUCA_REALLOC */

#ifndef EUCA_FREE
//! Macro to free a pointer and ensure that it'll be set to NULL
#define EUCA_FREE(_x) \
{                     \
	free((_x));       \
	(_x) = NULL;      \
}
#endif /* ! EUCA_FREE */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_EUCALYPTUS_H_ */
