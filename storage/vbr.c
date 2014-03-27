// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

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

//!
//! @file storage/vbr.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>                    // strcasestr
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>                  // waitpid
#include <stdarg.h>
#include <fcntl.h>
#include <errno.h>
#include <limits.h>
#include <assert.h>
#include <dirent.h>

#include <eucalyptus.h>
#include <misc.h>                      // logprintfl, ensure_...
#include <hash.h>
#include <data.h>
#include <euca_string.h>
#include "handlers.h"                  // nc_state
#include "vbr.h"
#include "objectstorage.h"
#include "blobstore.h"
#include "diskutil.h"
//#include "iscsi.h"
#include "http.h"
#include "ebs_utils.h"
#include <ipc.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define ART_SIG_MAX                              262144 //!< must be big enough for a digest and then some

#define FIND_BLOB_TIMEOUT_USEC                   50000LL    //!< @TODO: use 100 or less to induce rare timeouts
#define DELETE_BLOB_TIMEOUT_USEC                 50000LL

#define FIND                                     0
#define CREATE                                   1

#define ARTIFACT_RETRY_SLEEP_USEC                500000LL

#ifdef _UNIT_TEST
#define BS_SIZE                                  20000000000 / 512
#define KEY1                                     "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCVWU+h3gDF4sGjUB7t...\n"
#define KEY2                                     "ssh-rsa BBBBB3NzaC1yc2EAAAADAQABAAABAQCVWU+h3gDF4sGjUB7t...\n"
#define EKI1                                     "eki-1ABC123"
#define ERI1                                     "eri-1BCD234"
#define EMI1                                     "emi-1CDE345"
#define EMI2                                     "emi-2DEF456"
#define SERIAL_ITERATIONS                        3
#define COMPETITIVE_PARTICIPANTS                 3
#define COMPETITIVE_ITERATIONS                   3

#define TOTAL_VMS                                1 + SERIAL_ITERATIONS + COMPETITIVE_ITERATIONS * COMPETITIVE_PARTICIPANTS
#define VBR_SIZE                                 ( 2LL * MEGABYTE )
#define EKI_SIZE                                 ( 1024LL )
#endif /* _UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifdef _UNIT_TEST
const char *euca_this_component_name = "sc";    //!< Eucalyptus Component Name
const char *euca_client_component_name = "nc";  //!< The client component name
#endif /*_UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static __thread char current_instanceId[512] = "";  //!< instance ID that is being serviced, for logging only
static sem *hostconfig_sem;

#ifdef _UNIT_TEST
static blobstore *cache_bs = NULL;
static blobstore *work_bs = NULL;

static int next_instances_slot = 0;
static int provisioned_instances = 0;
static pthread_mutex_t competitors_mutex = PTHREAD_MUTEX_INITIALIZER;   //!< process-global mutex
static virtualMachine vm_slots[TOTAL_VMS] = { {0} };
static char vm_ids[TOTAL_VMS][PATH_MAX] = { {0} };

static boolean do_fork = 0;
#endif /* _UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int prep_location(virtualBootRecord * vbr, ncMetadata * pMeta, const char *typeName);
static int parse_rec(virtualBootRecord * vbr, virtualMachine * vm, ncMetadata * pMeta);
static void update_vbr_with_backing_info(artifact * a);

//! @{
//! @name Creator Functions
//! The following *_creator funcitons produce an artifact in blobstore,  either from scratch (such as objectstorage
//! download or a new partition) or by converting, combining, and augmenting existing artifacts.
//!
//! When invoked, creators can assume that any input blobs and the output blob are open (and thus locked for
//! their exclusive use).
//!
//! Creators return OK or an error code: either generic one (ERROR) or a code specific to a failed blobstore
//! operation, which can be obtained using blobstore_get_error().
static int url_creator(artifact * a);
static int objectstorage_creator(artifact * a);
static int imaging_creator(artifact * a);
static int partition_creator(artifact * a);
static void set_disk_dev(virtualBootRecord * vbr);
static int disk_creator(artifact * a);
static int disk_expander(artifact * a);
#ifndef _NO_EBS
static int iqn_creator(artifact * a);
#endif // ! _NO_EBS
static int copy_creator(artifact * a);
//! @}

static void art_print_tree(const char *prefix, artifact * a);
static int art_gen_id(char *buf, unsigned int buf_size, const char *first, const char *sig);
static void convert_id(const char *src, char *dst, unsigned int size);
static char *url_get_digest(const char *url);
static artifact *art_alloc_vbr(virtualBootRecord * vbr, boolean do_make_work_copy, boolean is_migration_dest, boolean must_be_file, const char *sshkey);
static artifact *art_alloc_disk(virtualBootRecord * vbr, artifact * prereqs[], int num_prereqs, artifact * parts[], int num_parts,
                                artifact * emi_disk, boolean do_make_bootable, boolean do_make_work_copy, boolean is_migration_dest);
static int find_or_create_blob(int flags, blobstore * bs, const char *id, long long size_bytes, const char *sig, blockblob ** bbp);
static int find_or_create_artifact(int do_create, artifact * a, blobstore * work_bs, blobstore * cache_bs, const char *work_prefix, blockblob ** bbp);

#ifdef _UNIT_TEST
static blobstore *create_teststore(int size_blocks, const char *base, const char *name, blobstore_format_t format, blobstore_revocation_t revocation,
                                   blobstore_snapshot_t snapshot);
static void add_vbr(virtualMachine * vm, long long size, ncResourceFormatType format, char *formatName, const char *id, ncResourceType type,
                    ncResourceLocationType locationType, int diskNumber, int partitionNumber, libvirtBusType guestDeviceBus, char *preparedResourceLocation);
static int provision_vm(const char *id, const char *sshkey, const char *eki, const char *eri, const char *emi, blobstore * cache_bs,
                        blobstore * work_bs, boolean do_make_work_copy);
static int cleanup_vms(void);
static char *gen_id(char *id, unsigned int id_len, const char *prefix);
static void *competitor_function(void *ptr);
static int check_blob(blobstore * bs, const char *keyword, int expect);
static void dummy_err_fn(const char *msg);
#endif /* _UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! A safe macro to free an artifact. Forces the pointer to NULL afterwards
#define ART_FREE(_a)  \
{                     \
	art_free((_a));   \
	(_a) = NULL;      \
}

#ifdef _UNIT_TEST
#define GEN_ID()                                 gen_id(id, sizeof(id), "12345678")
#endif /* _UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Initializes the global host config
//!
//! @param[in] hostIqn
//! @param[in] hostIp
//! @param[in] ws_sec_policy_file
//! @param[in] use_ws_sec
//!
//! @return EUCA_OK
//!
//! @pre
//!
//! @note
//!
int vbr_init_hostconfig(char *hostIqn, char *hostIp, char *ws_sec_policy_file, int use_ws_sec)
{
    LOGDEBUG("Initializing host config for VBR. Setting IP, IQN, and security policy\n");
    euca_strncpy(localhost_config.iqn, hostIqn, CHAR_BUFFER_SIZE);
    euca_strncpy(localhost_config.ip, hostIp, HOSTNAME_SIZE);
    euca_strncpy(localhost_config.ws_sec_policy_file, ws_sec_policy_file, EUCA_MAX_PATH);
    localhost_config.use_ws_sec = use_ws_sec;
    LOGDEBUG("VBR host config set to ip: %s iqn: %s, use_sec = %d, policy file = %s\n", localhost_config.ip, localhost_config.iqn, localhost_config.use_ws_sec,
             localhost_config.ws_sec_policy_file);
    hostconfig_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
    return (EUCA_OK);
}

//!
//! Semaphore protected read of the sc_url from the config
//! Destination buffer must be at least 512 in size
//!
//! @param[in] dest
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @note
//!
int get_localhost_sc_url(char *dest)
{
    int ret = 0;
    sem_p(hostconfig_sem);
    {
        if (strlen(localhost_config.sc_url) == 0) {
            LOGERROR("No sc url found in localhost_config.\n");
            ret = EUCA_ERROR;
        } else {
            if (!euca_strncpy(dest, localhost_config.sc_url, 512)) {
                LOGERROR("Failed up copy VBR hostconfig SC URL %s to destination buffer\n", localhost_config.sc_url);
                ret = EUCA_ERROR;
            } else {
                ret = EUCA_OK;
            }
        }
    }
    sem_v(hostconfig_sem);
    return (ret);
}

//!
//! Semaphore protected hostconfig update.
//!
//! @param[in] new_sc_url
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int vbr_update_hostconfig_scurl(char *new_sc_url)
{
    sem_p(hostconfig_sem);
    if (!euca_strncpy(localhost_config.sc_url, new_sc_url, 512)) {
        LOGERROR("Failed up update VBR hostconfig SC URL to %s from %s\n", new_sc_url, localhost_config.sc_url);
        sem_v(hostconfig_sem);
        return (EUCA_ERROR);
    } else {
        LOGTRACE("Updated sc url in VBR hostconfig to %s\n", localhost_config.sc_url);
        sem_v(hostconfig_sem);
        return (EUCA_OK);
    }
}

//!
//! Picks a service URI and prepends it to resourceLocation in VBR
//!
//! @param[in] vbr
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] typeName
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int prep_location(virtualBootRecord * vbr, ncMetadata * pMeta, const char *typeName)
{
    int i;

    for (i = 0; i < pMeta->servicesLen; i++) {
        serviceInfoType *service = &(pMeta->services[i]);
        if (strncmp(service->type, typeName, strlen(typeName) - 3) == 0 && service->urisLen > 0) {
            if (strcmp(typeName, "storage")) {
                //Anything other than storage/ebs
                char *l = vbr->resourceLocation + (strlen(typeName) + 3);   // +3 for "://", so 'l' points past, e.g., "objectstorage:"
                snprintf(vbr->preparedResourceLocation, sizeof(vbr->preparedResourceLocation), "%s/%s", service->uris[0], l);   //! @TODO for now we just pick the first one
                snprintf(vbr->resourceLocation, sizeof(vbr->resourceLocation), vbr->preparedResourceLocation);  //! @TODO trying this out
            } else {
                //For storage, just copy the url for the SC into the preparedResourceLocation slot
                snprintf(vbr->preparedResourceLocation, sizeof(vbr->preparedResourceLocation), "%s", service->uris[0]); //! @TODO for now we just pick the first one
            }
            return (EUCA_OK);
        }
    }
    LOGERROR("failed to find service '%s' in eucalyptusMessage\n", typeName);
    return (EUCA_ERROR);
}

//!
//! Parse spec_str as a VBR record and add it to  vm_type->virtualBootRecord[virtualBootRecordLen]
//!
//! @param[in] spec_str
//! @param[in] vm_type
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int vbr_add_ascii(const char *spec_str, virtualMachine * vm_type)
{
    if (vm_type->virtualBootRecordLen == EUCA_MAX_VBRS) {
        LOGERROR("too many entries in VBR already\n");
        return (1);
    }
    virtualBootRecord *vbr = &(vm_type->virtualBootRecord[vm_type->virtualBootRecordLen++]);

    char *spec_copy = strdup(spec_str);
    char *type_spec = strtok(spec_copy, ":");
    char *id_spec = strtok(NULL, ":");
    char *size_spec = strtok(NULL, ":");
    char *format_spec = strtok(NULL, ":");
    char *dev_spec = strtok(NULL, ":");
    char *loc_spec = strtok(NULL, ":");
    if (type_spec == NULL) {
        LOGERROR("invalid 'type' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    euca_strncpy(vbr->typeName, type_spec, sizeof(vbr->typeName));

    if (id_spec == NULL) {
        LOGERROR("invalid 'id' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    euca_strncpy(vbr->id, id_spec, sizeof(vbr->id));

    if (size_spec == NULL) {
        LOGERROR("invalid 'size' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    vbr->sizeBytes = atoll(size_spec);

    if (format_spec == NULL) {
        LOGERROR("invalid 'format' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    euca_strncpy(vbr->formatName, format_spec, sizeof(vbr->formatName));

    if (dev_spec == NULL) {
        LOGERROR("invalid 'guestDeviceName' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    euca_strncpy(vbr->guestDeviceName, dev_spec, sizeof(vbr->guestDeviceName));

    if (loc_spec == NULL) {
        LOGERROR("invalid 'resourceLocation' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    euca_strncpy(vbr->resourceLocation, spec_str + (loc_spec - spec_copy), sizeof(vbr->resourceLocation));

    EUCA_FREE(spec_copy);
    return (0);

out_error:
    vm_type->virtualBootRecordLen--;
    EUCA_FREE(spec_copy);
    return (1);
}

//!
//! Parses the VBR as supplied by a client or user, checks values, and fills out almost the rest of the struct with typed values
//!
//! @param[in] vbr pointer to a VBR record to parse and verify
//! @param[in] vm OPTIONAL parameter for setting image/kernel/ramdik pointers in the virtualMachine struct
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int parse_rec(virtualBootRecord * vbr, virtualMachine * vm, ncMetadata * pMeta)
{
    // check the type (the only mandatory field)
    if (strstr(vbr->typeName, "machine") == vbr->typeName) {
        vbr->type = NC_RESOURCE_IMAGE;
        if (vm)
            vm->root = vbr;
    } else if (strstr(vbr->typeName, "kernel") == vbr->typeName) {
        vbr->type = NC_RESOURCE_KERNEL;
        if (vm)
            vm->kernel = vbr;
    } else if (strstr(vbr->typeName, "ramdisk") == vbr->typeName) {
        vbr->type = NC_RESOURCE_RAMDISK;
        if (vm)
            vm->ramdisk = vbr;
    } else if (strstr(vbr->typeName, "ephemeral") == vbr->typeName) {
        vbr->type = NC_RESOURCE_EPHEMERAL;
        if (strstr(vbr->typeName, "ephemeral0") == vbr->typeName) {
            if (vm) {
                vm->ephemeral0 = vbr;
            }
        }
    } else if (strstr(vbr->typeName, "swap") == vbr->typeName) {
        vbr->type = NC_RESOURCE_SWAP;
        if (vm)
            vm->swap = vbr;
    } else if (strstr(vbr->typeName, "ebs") == vbr->typeName) {
        vbr->type = NC_RESOURCE_EBS;
    } else if (strstr(vbr->typeName, "boot") == vbr->typeName) {
        vbr->type = NC_RESOURCE_BOOT;
        if (vm)
            vm->boot = vbr;
    } else {
        LOGERROR("failed to parse resource type '%s'\n", vbr->typeName);
        return (EUCA_ERROR);
    }

    // identify the type of resource location from location string
    int error = EUCA_OK;
    if (strcasestr(vbr->resourceLocation, "http://") == vbr->resourceLocation || strcasestr(vbr->resourceLocation, "https://") == vbr->resourceLocation) {
        if (strcasestr(vbr->resourceLocation, "://imaging@")) {
            vbr->locationType = NC_LOCATION_IMAGING;
            // remove 'imaging@' from the URL
            char *s = strdup(vbr->resourceLocation);
            euca_strreplace(&s, "imaging@", "");
            euca_strncpy(vbr->preparedResourceLocation, s, sizeof(vbr->preparedResourceLocation));
            free(s);
        } else if (strcasestr(vbr->resourceLocation, "/services/objectstorage/")) {
            vbr->locationType = NC_LOCATION_OBJECT_STORAGE;
            euca_strncpy(vbr->preparedResourceLocation, vbr->resourceLocation, sizeof(vbr->preparedResourceLocation));
        } else {
            vbr->locationType = NC_LOCATION_URL;
            euca_strncpy(vbr->preparedResourceLocation, vbr->resourceLocation, sizeof(vbr->preparedResourceLocation));
        }
    } else if (strcasestr(vbr->resourceLocation, "file:///") == vbr->resourceLocation) {
        vbr->locationType = NC_LOCATION_FILE;
        // remove 'file:///' from the URL to get a usable path
        char *s = strdup(vbr->resourceLocation);
        euca_strreplace(&s, "file:///", "/");
        euca_strncpy(vbr->preparedResourceLocation, s, sizeof(vbr->preparedResourceLocation));
        free(s);
    } else if (strcasestr(vbr->resourceLocation, "objectstorage://") == vbr->resourceLocation) {
        vbr->locationType = NC_LOCATION_OBJECT_STORAGE;
        if (pMeta)
            error = prep_location(vbr, pMeta, "objectstorage");
    } else if (strcasestr(vbr->resourceLocation, "cloud://") == vbr->resourceLocation) {
        vbr->locationType = NC_LOCATION_CLC;
        if (pMeta)
            error = prep_location(vbr, pMeta, "cloud");
    } else if (strcasestr(vbr->resourceLocation, "sc://") == vbr->resourceLocation || strcasestr(vbr->resourceLocation, "storage://") == vbr->resourceLocation) {   //! @TODO is it 'sc' or 'storage'?
        vbr->locationType = NC_LOCATION_SC;
        if (pMeta)
            error = prep_location(vbr, pMeta, "storage");
    } else if (strcasestr(vbr->resourceLocation, "none") == vbr->resourceLocation) {
        if (vbr->type != NC_RESOURCE_EPHEMERAL && vbr->type != NC_RESOURCE_SWAP && vbr->type != NC_RESOURCE_BOOT) {
            LOGERROR("resourceLocation not specified for non-ephemeral resource '%s'\n", vbr->resourceLocation);
            return (EUCA_ERROR);
        }
        vbr->locationType = NC_LOCATION_NONE;
    } else {
        LOGERROR("failed to parse resource location '%s'\n", vbr->resourceLocation);
        return (EUCA_ERROR);
    }

    if (error != EUCA_OK) {
        LOGERROR("URL for resourceLocation '%s' is not in the message\n", vbr->resourceLocation);
        return (EUCA_ERROR);
    }
    // device can be 'none' only for kernel and ramdisk types
    if (!strcmp(vbr->guestDeviceName, "none")) {
        if (vbr->type != NC_RESOURCE_KERNEL && vbr->type != NC_RESOURCE_RAMDISK) {
            LOGERROR("guestDeviceName not specified for resource '%s'\n", vbr->resourceLocation);
            return (EUCA_ERROR);
        }

    } else {                           // should be a valid device

        // trim off "/dev/" prefix, if present, and verify the rest
        if (strstr(vbr->guestDeviceName, "/dev/") == vbr->guestDeviceName) {
            LOGWARN("trimming off invalid prefix '/dev/' from guestDeviceName '%s'\n", vbr->guestDeviceName);
            char buf[10];
            euca_strncpy(buf, vbr->guestDeviceName + 5, sizeof(buf));
            euca_strncpy(vbr->guestDeviceName, buf, sizeof(vbr->guestDeviceName));
        }

        if (strlen(vbr->guestDeviceName) < 3 || (vbr->guestDeviceName[0] == 'x' && strlen(vbr->guestDeviceName) < 4)) {
            LOGERROR("invalid guestDeviceName '%s'\n", vbr->guestDeviceName);
            return (EUCA_ERROR);
        }

        {
            char t = vbr->guestDeviceName[0];   // type
            switch (t) {
            case 'h':
                vbr->guestDeviceType = DEV_TYPE_DISK;
                vbr->guestDeviceBus = BUS_TYPE_IDE;
                break;
            case 's':
                vbr->guestDeviceType = DEV_TYPE_DISK;
                vbr->guestDeviceBus = BUS_TYPE_SCSI;
                break;
            case 'f':
                vbr->guestDeviceType = DEV_TYPE_FLOPPY;
                vbr->guestDeviceBus = BUS_TYPE_IDE;
                break;
            case 'v':
                vbr->guestDeviceType = DEV_TYPE_DISK;
                vbr->guestDeviceBus = BUS_TYPE_VIRTIO;
                break;
            case 'x':
                vbr->guestDeviceType = DEV_TYPE_DISK;
                vbr->guestDeviceBus = BUS_TYPE_XEN;
                break;
            default:
                LOGERROR("failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                return (EUCA_ERROR);
            }

            size_t letters_len = 3;    // e.g. "sda"
            if (t == 'x')
                letters_len = 4;       // e.g., "xvda"
            if (t == 'f')
                letters_len = 2;       // e.g., "fd0"
            char d = vbr->guestDeviceName[letters_len - 2]; // when 3+, the 'd'
            char n = vbr->guestDeviceName[letters_len - 1]; // when 3+, the disk number
            if (strlen(vbr->guestDeviceName) > letters_len) {
                long long int p = 0;   // partition or floppy drive number
                errno = 0;
                p = strtoll(vbr->guestDeviceName + letters_len, NULL, 10);
                if (errno != 0) {
                    LOGERROR("failed to parse partition number in guestDeviceName '%s'\n", vbr->guestDeviceName);
                    return (EUCA_ERROR);
                }
                if (p < 0 || p > EUCA_MAX_PARTITIONS) {
                    LOGERROR("unexpected partition or disk number '%lld' in guestDeviceName '%s'\n", p, vbr->guestDeviceName);
                    return (EUCA_ERROR);
                }
                if (t == 'f') {
                    vbr->diskNumber = p;
                } else {
                    if (p < 1) {
                        LOGERROR("unexpected partition number '%lld' in guestDeviceName '%s'\n", p, vbr->guestDeviceName);
                        return (EUCA_ERROR);
                    }
                    vbr->partitionNumber = p;
                }
            } else {
                vbr->partitionNumber = 0;
            }

            if (vbr->guestDeviceType != DEV_TYPE_FLOPPY) {
                if (d != 'd') {
                    LOGERROR("failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                    return (EUCA_ERROR);
                }
                assert(EUCA_MAX_DISKS >= 'z' - 'a');
                if (!(n >= 'a' && n <= 'z')) {
                    LOGERROR("failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                    return (EUCA_ERROR);
                }
                vbr->diskNumber = n - 'a';
            }
        }
    }

    // parse ID
    if (strlen(vbr->id) < 4) {
        LOGERROR("failed to parse VBR resource ID '%s' (use 'none' when no ID)\n", vbr->id);
        return (EUCA_ERROR);
    }
    // parse disk formatting instructions (none = do not format)
    if (strstr(vbr->formatName, "none") == vbr->formatName) {
        vbr->format = NC_FORMAT_NONE;
    } else if (strstr(vbr->formatName, "ext2") == vbr->formatName) {
        vbr->format = NC_FORMAT_EXT2;
    } else if (strstr(vbr->formatName, "ext3") == vbr->formatName) {
        vbr->format = NC_FORMAT_EXT3;
    } else if (strstr(vbr->formatName, "ntfs") == vbr->formatName) {
        vbr->format = NC_FORMAT_NTFS;
    } else if (strstr(vbr->formatName, "swap") == vbr->formatName) {
        vbr->format = NC_FORMAT_SWAP;
    } else {
        LOGERROR("failed to parse resource format '%s'\n", vbr->formatName);
        return (EUCA_ERROR);
    }
    if (vbr->type == NC_RESOURCE_EPHEMERAL || vbr->type == NC_RESOURCE_SWAP || vbr->type == NC_RESOURCE_BOOT) { //! @TODO should we allow ephemeral/swap that reside remotely?
        if (vbr->sizeBytes < 1) {
            LOGERROR("invalid size '%lld' for ephemeral resource '%s'\n", vbr->sizeBytes, vbr->resourceLocation);
            return (EUCA_ERROR);
        }
    } else {
        //            if (vbr->sizeBytes!=1 || vbr->format!=NC_FORMAT_NONE) { //! @TODO check for sizeBytes!=-1
        if (vbr->format != NC_FORMAT_NONE) {
            LOGERROR("invalid format '%s' for non-ephemeral resource '%s'\n", vbr->formatName, vbr->resourceLocation);
            return (EUCA_ERROR);
        }
    }

    return (EUCA_OK);
}

//!
//! Parses and verifies all VBR entries in the virtual machine definition
//!
//! @param[in] vm pointer to vm definition containing VBR records
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int vbr_parse(virtualMachine * vm, ncMetadata * pMeta)
{
    virtualBootRecord *partitions[BUS_TYPES_TOTAL][EUCA_MAX_DISKS][EUCA_MAX_PARTITIONS];    // for validating partitions
    bzero(partitions, sizeof(partitions));  //! @fixme: chuck - this is not zeroing out the hole structure!!!
    for (int i = 0; i < EUCA_MAX_VBRS && i < vm->virtualBootRecordLen; i++) {
        virtualBootRecord *vbr = &(vm->virtualBootRecord[i]);

        if (strlen(vbr->typeName) == 0) {   // this must be the combined disk's VBR
            return (EUCA_OK);
        }

        if (parse_rec(vbr, vm, pMeta) != EUCA_OK)
            return (EUCA_ERROR);

        if (vbr->type != NC_RESOURCE_KERNEL && vbr->type != NC_RESOURCE_RAMDISK)
            partitions[vbr->guestDeviceBus][vbr->diskNumber][vbr->partitionNumber] = vbr;

        if (vm->root == NULL) {        // we have not identified the EMI yet
            if (vbr->type == NC_RESOURCE_IMAGE || (vbr->type == NC_RESOURCE_EBS && vbr->diskNumber == 0 && vbr->partitionNumber == 0)) {
                vm->root = vbr;
            }
        } else {
            if (vm->root != vbr && vbr->type == NC_RESOURCE_IMAGE) {
                LOGERROR("more than one EMI specified in the boot record\n");
                return (EUCA_ERROR);
            }
        }
    }

    // ensure that partitions are contiguous under most circumstances
    for (int i = 0; i < BUS_TYPES_TOTAL; i++) { // each bus type is treated separatedly
        for (int j = 0; j < EUCA_MAX_DISKS; j++) {
            int npartitions = 0;
            for (int k = EUCA_MAX_PARTITIONS - 1; k >= 0; k--) {    // count down
                if (partitions[i][j][k]) {
                    if (k != 0) {      // this is a partition
                        npartitions++;
                    } else {           // this is a disk
                        if (npartitions > 0 && partitions[i][j][1]) {
                            LOGERROR("both disk and partition 1 are not allowed\n");
                            return (EUCA_ERROR);
                        }
                        if (npartitions > 2) {
                            LOGERROR("at most 2 partitions may be appended to a disk\n");
                            return (EUCA_ERROR);
                        }
                    }
                } else {
                    if (k > 1 && npartitions) { // k==1 is a special case, one in which 'sda1' became 'sda'
                        LOGERROR("gaps in partition table are not allowed\n");
                        return (EUCA_ERROR);
                    }
                }
                if (vm->root == NULL) { // root partition or disk have not been found yet (no NC_RESOURCE_IMAGE)
                    virtualBootRecord *vbr;
                    if (npartitions > 0)
                        vbr = partitions[i][j][1];
                    else
                        vbr = partitions[i][j][0];
                    if (vbr && (vbr->type == NC_RESOURCE_EBS))
                        vm->root = vbr;
                }
            }
        }
    }

    if (vm->root == NULL) {
        LOGERROR("no root partition or disk have been found\n");
        return (EUCA_ERROR);
    }

    return (EUCA_OK);
}

//!
//! Constructs VBRs for {image|kernel|ramdisk}x{Id|URL} entries (DEPRECATED)
//!
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] params pointer to the virtual machine parameters
//! @param[in] imageId OPTIONAL
//! @param[in] imageURL OPTIONALL
//! @param[in] kernelId OPTIONAL
//! @param[in] kernelURL OPTIONAL
//! @param[in] ramdiskId OPTIONAL
//! @param[in] ramdiskURL OPTIONAL
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int vbr_legacy(const char *instanceId, virtualMachine * params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL)
{
    int i;
    int found_image = 0;
    int found_kernel = 0;
    int found_ramdisk = 0;

    for (i = 0; i < EUCA_MAX_VBRS && i < params->virtualBootRecordLen; i++) {
        virtualBootRecord *vbr = &(params->virtualBootRecord[i]);
        if (strlen(vbr->resourceLocation) > 0) {
            LOGDEBUG("[%s]                VBR[%d] type=%s id=%s dev=%s size=%lld format=%s %s\n",
                     instanceId, i, vbr->typeName, vbr->id, vbr->guestDeviceName, vbr->sizeBytes, vbr->formatName, vbr->resourceLocation);
            if (!strcmp(vbr->typeName, "machine"))
                found_image = 1;
            if (!strcmp(vbr->typeName, "kernel"))
                found_kernel = 1;
            if (!strcmp(vbr->typeName, "ramdisk"))
                found_ramdisk = 1;
        } else {
            break;
        }
    }

    // legacy support for image{Id|URL}
    if (imageId && imageURL) {
        if (found_image) {
            LOGWARN("[%s] IGNORING image %s passed outside the virtual boot record\n", instanceId, imageId);
        } else {
            LOGWARN("[%s] LEGACY pre-VBR image id=%s URL=%s\n", instanceId, imageId, imageURL);
            if (i >= EUCA_MAX_VBRS - 2) {
                LOGERROR("[%s] out of room in the Virtual Boot Record for legacy image %s\n", instanceId, imageId);
                return (EUCA_ERROR);
            }

            {                          // create root partition VBR
                virtualBootRecord *vbr = &(params->virtualBootRecord[i++]);
                euca_strncpy(vbr->resourceLocation, imageURL, sizeof(vbr->resourceLocation));
                euca_strncpy(vbr->guestDeviceName, "sda1", sizeof(vbr->guestDeviceName));
                euca_strncpy(vbr->id, imageId, sizeof(vbr->id));
                euca_strncpy(vbr->typeName, "machine", sizeof(vbr->typeName));
                vbr->sizeBytes = -1;
                euca_strncpy(vbr->formatName, "none", sizeof(vbr->formatName));
                params->virtualBootRecordLen++;
            }
            {                          // create ephemeral partition VBR
                virtualBootRecord *vbr = &(params->virtualBootRecord[i++]);
                euca_strncpy(vbr->resourceLocation, "none", sizeof(vbr->resourceLocation));
                euca_strncpy(vbr->guestDeviceName, "sda2", sizeof(vbr->guestDeviceName));
                euca_strncpy(vbr->id, "none", sizeof(vbr->id));
                euca_strncpy(vbr->typeName, "ephemeral0", sizeof(vbr->typeName));
                vbr->sizeBytes = 536870912; // we cannot compute it here, so pick something
                euca_strncpy(vbr->formatName, "ext2", sizeof(vbr->formatName));
                params->virtualBootRecordLen++;
            }
            {                          // create swap partition VBR
                virtualBootRecord *vbr = &(params->virtualBootRecord[i++]);
                euca_strncpy(vbr->resourceLocation, "none", sizeof(vbr->resourceLocation));
                euca_strncpy(vbr->guestDeviceName, "sda3", sizeof(vbr->guestDeviceName));
                euca_strncpy(vbr->id, "none", sizeof(vbr->id));
                euca_strncpy(vbr->typeName, "swap", sizeof(vbr->typeName));
                vbr->sizeBytes = 536870912;
                euca_strncpy(vbr->formatName, "swap", sizeof(vbr->formatName));
                params->virtualBootRecordLen++;
            }
        }
    }
    // legacy support for kernel{Id|URL}
    if (kernelId && kernelURL) {
        if (found_kernel) {
            LOGINFO("[%s] IGNORING kernel %s passed outside the virtual boot record\n", instanceId, kernelId);
        } else {
            LOGINFO("[%s] LEGACY pre-VBR kernel id=%s URL=%s\n", instanceId, kernelId, kernelURL);
            if (i == EUCA_MAX_VBRS) {
                LOGERROR("[%s] out of room in the Virtual Boot Record for legacy kernel %s\n", instanceId, kernelId);
                return (EUCA_ERROR);
            }
            virtualBootRecord *vbr = &(params->virtualBootRecord[i++]);
            euca_strncpy(vbr->resourceLocation, kernelURL, sizeof(vbr->resourceLocation));
            euca_strncpy(vbr->guestDeviceName, "none", sizeof(vbr->guestDeviceName));
            euca_strncpy(vbr->id, kernelId, sizeof(vbr->id));
            euca_strncpy(vbr->typeName, "kernel", sizeof(vbr->typeName));
            vbr->sizeBytes = -1;
            euca_strncpy(vbr->formatName, "none", sizeof(vbr->formatName));
            params->virtualBootRecordLen++;
        }
    }
    // legacy support for ramdisk{Id|URL}
    if (ramdiskId && ramdiskURL) {
        if (found_ramdisk) {
            LOGINFO("[%s] IGNORING ramdisk %s passed outside the virtual boot record\n", instanceId, ramdiskId);
        } else {
            LOGINFO("[%s] LEGACY pre-VBR ramdisk id=%s URL=%s\n", instanceId, ramdiskId, ramdiskURL);
            if (i == EUCA_MAX_VBRS) {
                LOGERROR("[%s] out of room in the Virtual Boot Record for legacy ramdisk %s\n", instanceId, ramdiskId);
                return (EUCA_ERROR);
            }
            virtualBootRecord *vbr = &(params->virtualBootRecord[i++]);
            euca_strncpy(vbr->resourceLocation, ramdiskURL, sizeof(vbr->resourceLocation));
            euca_strncpy(vbr->guestDeviceName, "none", sizeof(vbr->guestDeviceName));
            euca_strncpy(vbr->id, ramdiskId, sizeof(vbr->id));
            euca_strncpy(vbr->typeName, "ramdisk", sizeof(vbr->typeName));
            vbr->sizeBytes = -1;
            euca_strncpy(vbr->formatName, "none", sizeof(vbr->formatName));
            params->virtualBootRecordLen++;
        }
    }
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] a
//!
//! @pre
//!
//! @note
//!
static void update_vbr_with_backing_info(artifact * a)
{
    assert(a);
    if (a->vbr == NULL)
        return;
    virtualBootRecord *vbr = a->vbr;

    assert(a->bb);
    if (!a->must_be_file &&            // not required to be a file
        strlen(blockblob_get_dev(a->bb)) && // there is a block device
        blockblob_get_file(a->bb) == NULL &&    // there is NO file access
        (blobstore_snapshot_t) a->bb->store->snapshot_policy != BLOBSTORE_SNAPSHOT_NONE) {  // without snapshots we can use files
        euca_strncpy(vbr->backingPath, blockblob_get_dev(a->bb), sizeof(vbr->backingPath));
        vbr->backingType = SOURCE_TYPE_BLOCK;
    } else {
        assert(blockblob_get_file(a->bb));
        euca_strncpy(vbr->backingPath, blockblob_get_file(a->bb), sizeof(vbr->backingPath));
        vbr->backingType = SOURCE_TYPE_FILE;
    }
    vbr->sizeBytes = a->bb->size_bytes;

    // create a symlink so backing file/dev is predictable regadless
    // of whether the backing is a loopback dev or a device mapper dev
    // or a file: this is needed for migration to work
    {
        const char *path = vbr->backingPath;
        char bbdirpath[PATH_MAX];
        blockblob_get_dir(a->bb, bbdirpath, sizeof(bbdirpath)); // fill in blob's base directory
        char *linkname = "[invalid]";
        if (!strcmp(vbr->guestDeviceName, "none")) {
            linkname = vbr->typeName;;
        } else {
            linkname = vbr->guestDeviceName;
        }
        char linkpath[PATH_MAX];
        snprintf(linkpath, sizeof(linkpath), "%s/link-to-%s", bbdirpath, linkname);
        if (symlink(path, linkpath) == 0) {
            LOGDEBUG("[%s] symlinked %s to %s\n", a->instanceId, path, linkpath);
            euca_strncpy(vbr->backingPath, linkpath, sizeof(vbr->backingPath));
        }
    }
}

//!
//!
//!
//! @param[in] a
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int url_creator(artifact * a)
{
    assert(a->bb);
    assert(a->vbr);
    virtualBootRecord *vbr = a->vbr;
    const char *dest_path = blockblob_get_file(a->bb);

    assert(vbr->preparedResourceLocation);
    if (a->do_not_download) {
        LOGINFO("[%s] skipping download of %s\n", a->instanceId, vbr->preparedResourceLocation);
        return (EUCA_OK);
    }
    LOGINFO("[%s] downloading %s\n", a->instanceId, vbr->preparedResourceLocation);
    if (http_get(vbr->preparedResourceLocation, dest_path) != EUCA_OK) {
        LOGERROR("[%s] failed to download component %s\n", a->instanceId, vbr->preparedResourceLocation);
        return (EUCA_ERROR);
    }

    return (EUCA_OK);
}

//!
//! Creates an artifact by downloading it from objectstorage
//!
//! @param[in] a
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int objectstorage_creator(artifact * a)
{
    assert(a->bb);
    assert(a->vbr);
    virtualBootRecord *vbr = a->vbr;
    const char *dest_path = blockblob_get_file(a->bb);

    assert(vbr->preparedResourceLocation);
    if (a->do_not_download) {
        LOGINFO("[%s] skipping download of %s\n", a->instanceId, vbr->preparedResourceLocation);
        return (EUCA_OK);
    }
    LOGINFO("[%s] downloading %s\n", a->instanceId, vbr->preparedResourceLocation);

#if !defined( _UNIT_TEST) && !defined(_NO_EBS)
    extern struct nc_state_t nc_state;
    char cmd[1024];
    snprintf(cmd, sizeof(cmd), "%s/usr/share/eucalyptus/get_bundle %s %s %s %lld >> /tmp/euca_nc_unbundle.log 2>&1", nc_state.home, nc_state.home, vbr->preparedResourceLocation,
             dest_path, a->bb->size_bytes);
    LOGDEBUG("%s\n", cmd);
    if (system(cmd) == 0) {
        LOGDEBUG("[%s] downloaded and unbundled %s\n", a->instanceId, vbr->preparedResourceLocation);
        return (EUCA_OK);
    } else {
        LOGERROR("[%s] failed on download and unbundle with command %s\n", a->instanceId, cmd);
        return (EUCA_ERROR);
    }
#endif
    if (objectstorage_image_by_manifest_url(vbr->preparedResourceLocation, dest_path, TRUE) != EUCA_OK) {
        LOGERROR("[%s] failed to download component %s\n", a->instanceId, vbr->preparedResourceLocation);
        return (EUCA_ERROR);
    }

    return (EUCA_OK);
}

//!
//! Creates an artifact by downloading it using a download manifest
//!
//! @param[in] a pointer to artifact with all necesary information
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int imaging_creator(artifact * a)
{
    assert(a->bb);
    assert(a->vbr);
    virtualBootRecord *vbr = a->vbr;
    const char *dest_path = blockblob_get_file(a->bb);

    assert(vbr->preparedResourceLocation);
    if (a->do_not_download) {
        LOGINFO("[%s] skipping download of %s\n", a->instanceId, vbr->preparedResourceLocation);
        return (EUCA_OK);
    }
    LOGINFO("[%s] downloading %s\n", a->instanceId, vbr->preparedResourceLocation);
    if (imaging_image_by_manifest_url(a->instanceId, vbr->preparedResourceLocation, dest_path, a->bb->size_bytes) != EUCA_OK) {
        LOGERROR("[%s] failed to download component %s\n", a->instanceId, vbr->preparedResourceLocation);
        return (EUCA_ERROR);
    }

    return (EUCA_OK);
}

//!
//! This creator simply copies the source file content into the blobstore
//!
//! @param[in] a pointer to artifact with all necesary information
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int file_creator(artifact * a)
{
    assert(a->bb);
    assert(a->vbr);
    virtualBootRecord *vbr = a->vbr;
    const char *dest_path = blockblob_get_file(a->bb);

    assert(vbr->preparedResourceLocation);
    long long size_bytes = file_size(dest_path);
    if (size_bytes != a->bb->size_bytes) {
        LOGERROR("[%s] size of %s is unexpected (%lld != %lld)\n", a->instanceId, dest_path, size_bytes, a->bb->size_bytes);
        return EUCA_ERROR;
    }
    if (a->do_not_download) {
        LOGINFO("[%s] skipping copy of data from %s\n", a->instanceId, vbr->preparedResourceLocation);
        return EUCA_OK;
    }
    if (diskutil_cp(vbr->preparedResourceLocation, dest_path) != EUCA_OK) {
        LOGERROR("[%s] failed to copy '%s' to '%s'\n", a->instanceId, vbr->preparedResourceLocation, dest_path);
        return EUCA_ERROR;
    }
    return EUCA_OK;
}

//!
//! Creates a new partition from scratch
//!
//! @param[in] a
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int partition_creator(artifact * a)
{
    assert(a->bb);
    assert(a->vbr);
    virtualBootRecord *vbr = a->vbr;
    const char *dest_dev = blockblob_get_dev(a->bb);

    assert(dest_dev);
    if (a->do_not_download) {
        LOGINFO("[%s] skipping formatting of %s\n", a->instanceId, a->id);
        return (EUCA_OK);
    }
    LOGINFO("[%s] creating partition of size %lld bytes and type %s in %s\n", a->instanceId, a->size_bytes, vbr->formatName, a->id);
    int format = EUCA_ERROR;
    switch (vbr->format) {
    case NC_FORMAT_NONE:
        format = EUCA_OK;
        break;
    case NC_FORMAT_EXT2:              //! @TODO distinguish ext2 and ext3!
    case NC_FORMAT_EXT3:
        format = diskutil_mkfs(dest_dev, a->size_bytes);
        break;
    case NC_FORMAT_SWAP:
        format = diskutil_mkswap(dest_dev, a->size_bytes);
        break;
    default:
        LOGERROR("[%s] format of type %d/%s is NOT IMPLEMENTED\n", a->instanceId, vbr->format, vbr->formatName);
        break;
    }

    if (format != EUCA_OK) {
        LOGERROR("[%s] failed to create partition in blob %s\n", a->instanceId, a->id);
        return (EUCA_ERROR);
    }

    return (EUCA_OK);
}

//!
//! Sets vbr->guestDeviceName based on other entries in the struct (guestDevice{Type|Bus},
//! {disk|partition}Number}
//!
//! @param[in] vbr
//!
//! @pre
//!
//! @note
//!
static void set_disk_dev(virtualBootRecord * vbr)
{
    char type[3] = "\0\0\0";
    if (vbr->guestDeviceType == DEV_TYPE_FLOPPY) {
        type[0] = 'f';
    } else {                           // a disk
        switch (vbr->guestDeviceBus) {
        case BUS_TYPE_IDE:
            type[0] = 'h';
            break;
        case BUS_TYPE_SCSI:
            type[0] = 's';
            break;
        case BUS_TYPE_VIRTIO:
            type[0] = 'v';
            break;
        case BUS_TYPE_XEN:
            type[0] = 'x';
            type[1] = 'v';
            break;
        case BUS_TYPES_TOTAL:
        default:
            type[0] = '?';             // error
            break;
        }
    }

    char disk;
    if (vbr->guestDeviceType == DEV_TYPE_FLOPPY) {
        assert(vbr->diskNumber >= 0 && vbr->diskNumber <= 9);
        disk = '0' + vbr->diskNumber;
    } else {                           // a disk
        assert(vbr->diskNumber >= 0 && vbr->diskNumber <= 26);
        disk = 'a' + vbr->diskNumber;
    }

    char part[3] = "\0";
    if (vbr->partitionNumber) {
        snprintf(part, sizeof(part), "%d", vbr->partitionNumber);
    }

    snprintf(vbr->guestDeviceName, sizeof(vbr->guestDeviceName), "%sd%c%s", type, disk, part);
}

//!
//! Creates a 'raw' disk based on partitions
//!
//! @param[in] a
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int disk_creator(artifact * a)
{
    int ret = EUCA_ERROR;
    assert(a);
    assert(a->bb);
    const char *dest_dev = blockblob_get_dev(a->bb);

    assert(dest_dev);
    if (a->do_make_bootable) {
        a->size_bytes += (SECTOR_SIZE * BOOT_BLOCKS);
    }

    blockmap_relation_t mbr_op = BLOBSTORE_SNAPSHOT;
    blockmap_relation_t part_op = BLOBSTORE_MAP;    // use map by default as it is faster
    if ((blobstore_snapshot_t) a->bb->store->snapshot_policy == BLOBSTORE_SNAPSHOT_NONE) {
        // but fall back to copy when snapshots are not possible or desired
        mbr_op = BLOBSTORE_COPY;
        part_op = BLOBSTORE_COPY;
    }

blockmap map[EUCA_MAX_PARTITIONS] = { {mbr_op, BLOBSTORE_ZERO, {blob:NULL}
                                           , 0, 0, MBR_BLOCKS}
    };                                 // initially only MBR is in the map

    // run through partitions, add their sizes, populate the map
    virtualBootRecord *p1 = NULL;
    virtualBootRecord *disk = a->vbr;
    int map_entries = 1;               // first map entry is for the MBR
    int root_entry = -1;               // we do not know the root entry
    int root_part = -1;                // we do not know the root partition
    int boot_entry = -1;               // we do not know the boot entry
    int boot_part = -1;                // we do not know the boot partition
    const char *kernel_path = NULL;
    const char *ramdisk_path = NULL;
    long long offset_bytes = SECTOR_SIZE * MBR_BLOCKS;  // first partition begins after MBR

    // Actually, after /boot if we're bootable
    if (a->do_make_bootable) {
        offset_bytes += (SECTOR_SIZE * BOOT_BLOCKS);
    }

    LOGDEBUG("[%s] offset for the first partition will be %llu bytes\n", a->instanceId, offset_bytes);

    assert(disk);
    for (int i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        artifact *dep = a->deps[i];
        if (!dep->is_partition) {
            if (dep->vbr && dep->vbr->type == NC_RESOURCE_KERNEL)
                kernel_path = blockblob_get_file(dep->bb);
            if (dep->vbr && dep->vbr->type == NC_RESOURCE_RAMDISK)
                ramdisk_path = blockblob_get_file(dep->bb);
            continue;
        }
        virtualBootRecord *p = dep->vbr;
        assert(p);
        if (p1 == NULL)
            p1 = p;

        assert(dep->bb);
        assert(dep->size_bytes > 0);
        map[map_entries].relation_type = part_op;
        map[map_entries].source_type = BLOBSTORE_BLOCKBLOB;
        map[map_entries].source.blob = dep->bb;
        map[map_entries].first_block_src = 0;
        map[map_entries].first_block_dst = (offset_bytes / 512);
        map[map_entries].len_blocks = (dep->size_bytes / 512);
        LOGDEBUG("[%s] mapping partition %d from %s [%lld-%lld]\n",
                 a->instanceId, map_entries, blockblob_get_dev(a->deps[i]->bb), map[map_entries].first_block_dst,
                 map[map_entries].first_block_dst + map[map_entries].len_blocks - 1);
        offset_bytes += dep->size_bytes;
        if (p->type == NC_RESOURCE_IMAGE && root_entry == -1) {
            root_entry = map_entries;
            root_part = i;
        }
        map_entries++;
    }
    assert(p1);

    if (a->do_make_bootable) {
        map[map_entries - 1].first_block_dst = MBR_BLOCKS;
        map[map_entries - 1].len_blocks = BOOT_BLOCKS;
        boot_entry = map_entries - 1;
        boot_part = boot_entry - 1;
        LOGDEBUG("[%s] re-mapping partition %d from %s [%lld-%lld] %lld blocks\n", a->instanceId, boot_entry,
                 blockblob_get_dev(map[map_entries - 1].source.blob), map[map_entries - 1].first_block_dst,
                 (map[map_entries - 1].first_block_dst + map[map_entries - 1].len_blocks - 1), map[map_entries - 1].len_blocks);
    }
    // set fields in vbr that are needed for
    // xml.c:gen_instance_xml() to generate correct disk entries
    disk->guestDeviceType = p1->guestDeviceType;
    disk->guestDeviceBus = p1->guestDeviceBus;
    disk->diskNumber = p1->diskNumber;
    set_disk_dev(disk);

    if (a->do_not_download) {
        LOGINFO("[%s] skipping construction of %s\n", a->instanceId, a->id);
        return (EUCA_OK);
    }
    LOGINFO("[%s] constructing disk of size %lld bytes in %s (%s)\n", a->instanceId, a->size_bytes, a->id, blockblob_get_dev(a->bb));

    // map the partitions to the disk
    if (blockblob_clone(a->bb, map, map_entries) == -1) {
        ret = blobstore_get_error();
        LOGERROR("[%s] failed to clone partitions to created disk: %d %s\n", a->instanceId, ret, blobstore_get_last_msg());
        goto cleanup;
    }
    // create MBR
    LOGINFO("[%s] creating MBR\n", a->instanceId);
    if (diskutil_mbr(blockblob_get_dev(a->bb), "msdos") != EUCA_OK) {   // issues `parted mklabel`
        LOGERROR("[%s] failed to add MBR to disk: %d %s\n", a->instanceId, blobstore_get_error(), blobstore_get_last_msg());
        goto cleanup;
    }
    // add partition information to MBR
    for (int i = 1; i < map_entries; i++) { // map [0] is for the MBR
        LOGINFO("[%s] adding partition %d to partition table (%s)\n", a->instanceId, i, blockblob_get_dev(a->bb));
        if (diskutil_part(blockblob_get_dev(a->bb), // issues `parted mkpart`
                          "primary",   //! @TODO make this work with more than 4 partitions
                          NULL,        // do not create file system
                          map[i].first_block_dst,   // first sector
                          map[i].first_block_dst + map[i].len_blocks - 1) != EUCA_OK) {
            LOGERROR("[%s] failed to add partition %d to disk: %d %s\n", a->instanceId, i, blobstore_get_error(), blobstore_get_last_msg());
            goto cleanup;
        }
    }

    //  make disk bootable if necessary
    if (a->do_make_bootable) {
        boolean bootification_failed = 1;

        LOGINFO("[%s] making disk bootable\n", a->instanceId);
        if (boot_entry < 1 || boot_part < 0) {
            LOGERROR("[%s] cannot make bootable a disk without an image\n", a->instanceId);
            goto cleanup;
        }
        if (kernel_path == NULL) {
            LOGERROR("[%s] no kernel found among the VBRs\n", a->instanceId);
            goto cleanup;
        }
        if (ramdisk_path == NULL) {
            LOGERROR("[%s] no ramdisk found among the VBRs\n", a->instanceId);
            goto cleanup;
        }
        // `parted mkpart` causes children devices for each partition to be created
        // (e.g., /dev/mapper/euca-diskX gets /dev/mapper/euca-diskXp1 or ...X1 and so on)
        // we mount such a device here so as to copy files to the boot partition
        // (we cannot mount the dev of the partition's blob because it becomes
        // 'busy' after the clone operation)
        char *mapper_dev = NULL;
        char dev_with_p[EUCA_MAX_PATH];
        char dev_without_p[EUCA_MAX_PATH];  // on Ubuntu Precise, some dev names do not have 'p' in them
        snprintf(dev_with_p, sizeof(dev_with_p), "%sp%d", blockblob_get_dev(a->bb), boot_entry);
        snprintf(dev_without_p, sizeof(dev_without_p), "%s%d", blockblob_get_dev(a->bb), boot_entry);
        if (check_path(dev_with_p) == 0) {
            mapper_dev = dev_with_p;
        } else if (check_path(dev_without_p) == 0) {
            mapper_dev = dev_without_p;
        } else {
            LOGERROR("[%s] failed to stat partition device [%s]. errno=%d(%s)\n", a->instanceId, mapper_dev, errno, strerror(errno));
            goto cleanup;
        }
        LOGINFO("[%s] found partition device %s\n", a->instanceId, mapper_dev);

        // point a loopback device at the partition device because grub-probe on Ubuntu Precise
        // sometimes does not grok boot partitions mounted from /dev/mapper/...
        char loop_dev[EUCA_MAX_PATH];
        if (diskutil_loop(mapper_dev, 0, loop_dev, sizeof(loop_dev)) != EUCA_OK) {
            LOGINFO("[%s] failed to attach '%s' on a loopback device\n", a->instanceId, mapper_dev);
            goto cleanup;
        }
        assert(strncmp(loop_dev, "/dev/loop", 9) == 0);

        // mount the boot partition
        char mnt_pt[EUCA_MAX_PATH] = "/tmp/euca-mount-XXXXXX";
        if (safe_mkdtemp(mnt_pt) == NULL) {
            LOGINFO("[%s] mkdtemp() failed: %s\n", a->instanceId, strerror(errno));
            goto unloop;
        }
        if (diskutil_mount(loop_dev, mnt_pt) != EUCA_OK) {
            LOGINFO("[%s] failed to mount '%s' on '%s'\n", a->instanceId, loop_dev, mnt_pt);
            goto unloop;
        }
        // copy in kernel and ramdisk and run grub over the boot partition and the MBR
        LOGINFO("[%s] making partition %d bootable\n", a->instanceId, boot_part);
        LOGINFO("[%s] with kernel %s\n", a->instanceId, kernel_path);
        LOGINFO("[%s] and ramdisk %s\n", a->instanceId, ramdisk_path);
        if (diskutil_grub_files(mnt_pt, boot_part, kernel_path, ramdisk_path) != EUCA_OK) {
            LOGERROR("[%s] failed to make disk bootable (could not install grub files)\n", a->instanceId);
            goto unmount;
        }
        if (blockblob_sync(mapper_dev, a->bb) != 0) {
            LOGERROR("[%s] failed to flush I/O on disk\n", a->instanceId);
            goto unmount;
        }
        if (diskutil_grub2_mbr(blockblob_get_dev(a->bb), boot_part, mnt_pt) != EUCA_OK) {
            LOGERROR("[%s] failed to make disk bootable (could not install grub)\n", a->instanceId);
            goto unmount;
        }
        // change user of the blob device back to 'eucalyptus' (grub sets it to 'boot')
        sleep(1);                      // without this, perms on dev-mapper devices can flip back, presumably because in-kernel ops complete after grub process finishes
        if (diskutil_ch(blockblob_get_dev(a->bb), EUCALYPTUS_ADMIN, NULL, 0) != EUCA_OK) {
            LOGINFO("[%s] failed to change user for '%s' to '%s'\n", a->instanceId, blockblob_get_dev(a->bb), EUCALYPTUS_ADMIN);
        }
        bootification_failed = 0;

unmount:

        // unmount partition and delete the mount point
        if (diskutil_umount(mnt_pt) != EUCA_OK) {
            LOGINFO("[%s] failed to unmount %s (there may be a resource leak)\n", a->instanceId, mnt_pt);
            bootification_failed = 1;
        }
        if (rmdir(mnt_pt) != 0) {
            LOGINFO("[%s] failed to remove %s (there may be a resource leak): %s\n", a->instanceId, mnt_pt, strerror(errno));
            bootification_failed = 1;
        }

unloop:
        if (diskutil_unloop(loop_dev) != EUCA_OK) {
            LOGINFO("[%s] failed to remove %s (there may be a resource leak): %s\n", a->instanceId, loop_dev, strerror(errno));
            bootification_failed = 1;
        }
        if (bootification_failed)
            goto cleanup;
    }

    ret = EUCA_OK;
cleanup:
    return (ret);
}

//!
//! Expander a 'raw' disk with 1-2 new partitions
//!
//! @param[in] a
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int disk_expander(artifact * a)
{
    int ret = EUCA_ERROR;
    assert(a);
    assert(a->bb);
    const char *dest_dev = blockblob_get_dev(a->bb);
    assert(dest_dev);

    blockmap_relation_t part_op = BLOBSTORE_MAP;    // use map by default as it is faster
    if ((blobstore_snapshot_t) a->bb->store->snapshot_policy == BLOBSTORE_SNAPSHOT_NONE) {
        // but fall back to copy when snapshots are not possible or desired
        part_op = BLOBSTORE_COPY;
    }
    // run through partitions, add their sizes, populate the map
    virtualBootRecord *disk = a->vbr;
    assert(disk);
    blockmap map[EUCA_MAX_PARTITIONS]; // the map of disk sections
    int map_entries = 0;               // first map entry is for the disk
    long long offset_bytes = 0;

    for (int i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        artifact *dep = a->deps[i];
        if (dep->vbr &&                // TODO: write a more robust check
            (dep->vbr->type == NC_RESOURCE_KERNEL || dep->vbr->type == NC_RESOURCE_RAMDISK)) {
            continue;
        }
        virtualBootRecord *p = dep->vbr;
        assert(p);

        assert(dep->bb);
        assert(dep->size_bytes > 0);
        map[map_entries].relation_type = part_op;
        map[map_entries].source_type = BLOBSTORE_BLOCKBLOB;
        map[map_entries].source.blob = dep->bb;
        map[map_entries].first_block_src = 0;
        map[map_entries].first_block_dst = (offset_bytes / 512);
        map[map_entries].len_blocks = (dep->size_bytes / 512);
        LOGDEBUG("[%s] mapping partition %d from %s [%lld-%lld]\n",
                 a->instanceId, map_entries, blockblob_get_dev(a->deps[i]->bb), map[map_entries].first_block_dst,
                 map[map_entries].first_block_dst + map[map_entries].len_blocks - 1);
        offset_bytes += dep->size_bytes;
        map_entries++;
    }

    // set fields in vbr that are needed for
    // xml.c:gen_instance_xml() to generate correct disk entries
    disk->guestDeviceType = disk->guestDeviceType;
    disk->guestDeviceBus = disk->guestDeviceBus;
    disk->diskNumber = disk->diskNumber;
    set_disk_dev(disk);

    if (a->do_not_download) {
        LOGINFO("[%s] skipping construction of %s\n", a->instanceId, a->id);
        return (EUCA_OK);
    }
    LOGINFO("[%s] expanding disk to size %lld bytes in %s (%s)\n", a->instanceId, a->size_bytes, a->id, blockblob_get_dev(a->bb));

    // map the partitions to the disk
    if (blockblob_clone(a->bb, map, map_entries) == -1) {
        ret = blobstore_get_error();
        LOGERROR("[%s] failed to clone partitions to created disk: %d %s\n", a->instanceId, ret, blobstore_get_last_msg());
        goto cleanup;
    }
    // add the information to MBR for the new partitions
    for (int i = 1; i < map_entries; i++) { // map [0] is for the disk
        LOGINFO("[%s] adding partition %d to partition table (%s)\n", a->instanceId, i, blockblob_get_dev(a->bb));
        if (diskutil_part(blockblob_get_dev(a->bb), // issues `parted mkpart`
                          "primary",   //! @TODO make this work with more than 4 partitions
                          NULL,        // do not create file system
                          map[i].first_block_dst,   // first sector
                          map[i].first_block_dst + map[i].len_blocks - 1) != EUCA_OK) {
            LOGERROR("[%s] failed to add partition %d to disk: %d %s\n", a->instanceId, i, blobstore_get_error(), blobstore_get_last_msg());
            goto cleanup;
        }
    }

    ret = EUCA_OK;
cleanup:
    return (ret);
}

//!
//!
//!
//! @param[in] a
//!
//! @return
//!
//! @pre
//!
//! @note
//!
#ifndef _NO_EBS
static int iqn_creator(artifact * a)
{
    int rc = EUCA_OK;
    char *dev = NULL;
    ebs_volume_data *vol_data = NULL;
    virtualBootRecord *vbr = NULL;

    assert(a);
    vbr = a->vbr;
    assert(vbr);

    rc = connect_ebs_volume(vbr->preparedResourceLocation, vbr->resourceLocation, localhost_config.use_ws_sec, localhost_config.ws_sec_policy_file, localhost_config.ip,
                            localhost_config.iqn, &dev, &vol_data);
    if (rc) {
        LOGERROR("[%s] failed to attach volume during VBR construction for %s\n", a->instanceId, vbr->guestDeviceName);
        EUCA_FREE(vol_data);
        return (EUCA_ERROR);
    }

    if (!dev || !strstr(dev, "/dev")) {
        EUCA_FREE(vol_data);
        LOGERROR("[%s] failed to connect to iSCSI target\n", a->instanceId);
        return (EUCA_ERROR);
    } else {
        //Update the vbr preparedResourceLocation with the connection_string returned from token resolution
        euca_strncpy(vbr->preparedResourceLocation, vol_data->connect_string, sizeof(vbr->preparedResourceLocation));
    }

    // update VBR with device location
    euca_strncpy(vbr->backingPath, dev, sizeof(vbr->backingPath));
    vbr->backingType = SOURCE_TYPE_BLOCK;
    EUCA_FREE(vol_data);
    return (EUCA_OK);
}
#endif // ! _NO_EBS

//!
//!
//!
//! @param[in] a
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int copy_creator(artifact * a)
{
    assert(a->deps[0]);
    assert(a->deps[1] == NULL);
    artifact *dep = a->deps[0];
    virtualBootRecord *vbr = a->vbr;
    assert(vbr);

    if (a->do_not_download) {
        LOGINFO("[%s] skipping copying to %s\n", a->instanceId, a->bb->id);
        return (EUCA_OK);
    }

    if (dep->bb != NULL) {             // skip copy if source is NULL (as in the case of a bypassed redundant work artifact due to caching failure)
        LOGINFO("[%s] copying/cloning blob %s to blob %s\n", a->instanceId, dep->bb->id, a->bb->id);
        if (a->must_be_file) {
            if (blockblob_copy(dep->bb, 0L, a->bb, 0L, 0L) == -1) {
                LOGERROR("[%s] failed to copy blob %s to blob %s: %d %s\n", a->instanceId, dep->bb->id, a->bb->id, blobstore_get_error(), blobstore_get_last_msg());
                return blobstore_get_error();
            }
        } else {
            blockmap_relation_t op = BLOBSTORE_SNAPSHOT;    // use snapshot by default as it is faster
            if ((blobstore_snapshot_t) a->bb->store->snapshot_policy == BLOBSTORE_SNAPSHOT_NONE) {
                op = BLOBSTORE_COPY;   // but fall back to copy when snapshots are not possible or desired
            }
blockmap map[] = { {op, BLOBSTORE_BLOCKBLOB, {blob:dep->bb}
                                , 0, 0, round_up_sec(dep->size_bytes) / 512}
            };
            if (blockblob_clone(a->bb, map, 1) == -1) {
                LOGERROR("[%s] failed to clone/copy blob %s to blob %s: %d %s\n", a->instanceId, dep->bb->id, a->bb->id, blobstore_get_error(), blobstore_get_last_msg());
                return blobstore_get_error();
            }
        }
    }

    const char *dev = blockblob_get_dev(a->bb);
    const char *bbfile = blockblob_get_file(a->bb);

    if (a->do_tune_fs) {
        // tune file system, which is needed to boot EMIs fscked long ago
        LOGINFO("[%s] tuning root file system on disk %d partition %d\n", a->instanceId, vbr->diskNumber, vbr->partitionNumber);
        if (diskutil_tune(dev) != EUCA_OK) {
            LOGWARN("[%s] failed to tune root file system: %s\n", a->instanceId, blobstore_get_last_msg());
        }
    }

    if (!strcmp(vbr->typeName, "kernel") || !strcmp(vbr->typeName, "ramdisk")) {
        // for libvirt/kvm, kernel and ramdisk must be readable by libvirt
        if (diskutil_ch(bbfile, NULL, NULL, 0664) != EUCA_OK) {
            LOGERROR("[%s] failed to change user and/or permissions for '%s' '%s'\n", a->instanceId, vbr->typeName, bbfile);
        }
    }

    if (strlen(a->sshkey)) {

        int injection_failed = 1;
        LOGINFO("[%s] injecting the ssh key\n", a->instanceId);

        // mount the partition
        char mnt_pt[EUCA_MAX_PATH] = "/tmp/euca-mount-XXXXXX";
        if (safe_mkdtemp(mnt_pt) == NULL) {
            LOGERROR("[%s] mkdtemp() failed: %s\n", a->instanceId, strerror(errno));
            goto error;
        }
        if (diskutil_mount(dev, mnt_pt) != EUCA_OK) {
            LOGERROR("[%s] failed to mount '%s' on '%s'\n", a->instanceId, dev, mnt_pt);
            goto error;
        }
        // save the SSH key, with the right permissions
        char path[EUCA_MAX_PATH];
        snprintf(path, sizeof(path), "%s/root/.ssh", mnt_pt);
        if (diskutil_mkdir(path) == -1) {
            LOGERROR("[%s] failed to create path '%s'\n", a->instanceId, path);
            goto unmount;
        }
        if (diskutil_ch(path, "root", NULL, 0700) != EUCA_OK) {
            LOGERROR("[%s] failed to change user and/or permissions for '%s'\n", a->instanceId, path);
            goto unmount;
        }
        snprintf(path, sizeof(path), "%s/root/.ssh/authorized_keys", mnt_pt);
        if (diskutil_write2file(path, a->sshkey) != EUCA_OK) {  //! @TODO maybe append the key instead of overwriting?
            LOGERROR("[%s] failed to save key in '%s'\n", a->instanceId, path);
            goto unmount;
        }
        if (diskutil_ch(path, "root", NULL, 0600) != EUCA_OK) {
            LOGERROR("[%s] failed to change user and/or permissions for '%s'\n", a->instanceId, path);
            goto unmount;
        }
        // change user of the blob device back to 'eucalyptus' (tune and maybe other commands above set it to 'root')
        if (diskutil_ch(dev, EUCALYPTUS_ADMIN, NULL, 0) != EUCA_OK) {
            LOGERROR("[%s] failed to change user for '%s' to '%s'\n", a->instanceId, dev, EUCALYPTUS_ADMIN);
        }
        injection_failed = 0;

unmount:

        // unmount partition and delete the mount point
        if (diskutil_umount(mnt_pt) != EUCA_OK) {
            LOGERROR("[%s] failed to unmount %s (there may be a resource leak)\n", a->instanceId, mnt_pt);
            injection_failed = 1;
        }
        if (rmdir(mnt_pt) != 0) {
            LOGERROR("[%s] failed to remove %s (there may be a resource leak): %s\n", a->instanceId, mnt_pt, strerror(errno));
            injection_failed = 1;
        }

error:

        if (injection_failed)
            return (EUCA_ERROR);
    }

    return (EUCA_OK);
}

//!
//! Functions for adding and freeing artifacts on a tree. Currently each artifact tree
//! is used within a single thread (startup thread), so these do not need to be thread
//! safe.
//!
//! @param[in] a
//! @param[in] dep
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int art_add_dep(artifact * a, artifact * dep)
{
    if (dep == NULL)
        return (EUCA_OK);

    for (int i = 0; i < MAX_ARTIFACT_DEPS; i++) {
        if (a->deps[i] == NULL) {
            LOGDEBUG("[%s] added to artifact %03d|%s artifact %03d|%s\n", a->instanceId, a->seq, a->id, dep->seq, dep->id);
            a->deps[i] = dep;
            dep->refs++;
            return (EUCA_OK);
        }
    }
    return (EUCA_ERROR);
}

//!
//! Frees the artifact and all its dependencies
//!
//! @param[in] a
//!
//! @pre
//!
//! @note
//!
void art_free(artifact * a)
{
    if (a) {
        if (a->refs > 0) {
            // this free reduces reference count, if positive, by 1
            a->refs--;
        }
        // if this is the last reference
        if (a->refs == 0) {
            // try freeing dependents recursively
            for (int i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
                ART_FREE(a->deps[i]);
            }
            LOGTRACE("[%s] freeing artifact %03d|%s size=%lld vbr=%p cache=%d file=%d\n", a->instanceId, a->seq, a->id, a->size_bytes, a->vbr, a->may_be_cached, a->must_be_file);
            EUCA_FREE(a);
        }
    }
}

//!
//!
//!
//! @param[in] array
//! @param[in] array_len
//!
//! @return
//!
//! @pre
//!
//! @note
//!
void arts_free(artifact * array[], unsigned int array_len)
{
    u_int i = 0;
    if (array) {
        for (i = 0; i < array_len; i++)
            ART_FREE(array[i]);
    }
}

//!
//!
//!
//! @param[in] prefix
//! @param[in] a
//!
//! @pre
//!
//! @note
//!
static void art_print_tree(const char *prefix, artifact * a)
{
    LOGDEBUG("[%s] %s%03d|%s %lld c=%d f=%d cr=%p vbr=%p\n", a->instanceId, prefix, a->seq, a->id, a->size_bytes, a->may_be_cached, a->must_be_file, a->creator, a->vbr);

    char new_prefix[512];
    snprintf(new_prefix, sizeof(new_prefix), "%s\t", prefix);
    for (int i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        art_print_tree(new_prefix, a->deps[i]);
    }
}

//!
//!
//!
//! @param[in] a
//!
//! @return
//!
//! @pre
//!
//! @note
//!
boolean tree_uses_blobstore(artifact * a)
{
    if (!a->id_is_path)
        return TRUE;
    for (int i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        if (tree_uses_blobstore(a->deps[i]))
            return TRUE;
    }
    return FALSE;
}

//!
//!
//!
//! @param[in] a
//!
//! @return
//!
//! @pre
//!
//! @note
//!
boolean tree_uses_cache(artifact * a)
{
    if (a->may_be_cached)
        return TRUE;
    for (int i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        if (tree_uses_cache(a->deps[i]))
            return TRUE;
    }
    return FALSE;
}

//!
//!
//!
//! @param[in] buf
//! @param[in] buf_size
//! @param[in] first
//! @param[in] sig
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int art_gen_id(char *buf, unsigned int buf_size, const char *first, const char *sig)
{
    int bytesWritten = 0;
    char hash[48] = "";

    if (hexjenkins(hash, sizeof(hash), sig) != EUCA_OK)
        return (EUCA_ERROR);

    if ((bytesWritten = snprintf(buf, buf_size, "%s-%s", first, hash)) < 0)
        return (EUCA_ERROR);

    if (((unsigned)bytesWritten) >= buf_size)   // truncation
        return (EUCA_ERROR);

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] id
//! @param[in] sig
//! @param[in] size_bytes
//! @param[in] may_be_cached
//! @param[in] must_be_file
//! @param[in] must_be_hollow
//! @param[in] creator
//! @param[in] vbr
//!
//! @return
//!
//! @pre
//!
//! @note
//!
artifact *art_alloc(const char *id, const char *sig, long long size_bytes, boolean may_be_cached, boolean must_be_file, boolean must_be_hollow,
                    int (*creator) (artifact * a), virtualBootRecord * vbr)
{
    artifact *a = EUCA_ZALLOC(1, sizeof(artifact));
    if (a == NULL)
        return NULL;

    static int seq = 0;
    a->seq = ++seq;                    // not thread safe, but seq's are just for debugging
    euca_strncpy(a->instanceId, current_instanceId, sizeof(a->instanceId)); // for logging
    LOGDEBUG("[%s] allocated artifact %03d|%s size=%lld vbr=%p cache=%d file=%d\n", a->instanceId, seq, id, size_bytes, vbr, may_be_cached, must_be_file);

    if (id)
        euca_strncpy(a->id, id, sizeof(a->id));
    if (sig)
        euca_strncpy(a->sig, sig, sizeof(a->sig));
    a->size_bytes = size_bytes;
    a->may_be_cached = may_be_cached;
    a->must_be_file = must_be_file;
    a->must_be_hollow = must_be_hollow;
    a->creator = creator;
    a->vbr = vbr;
    a->do_tune_fs = FALSE;
    if (vbr && (vbr->type == NC_RESOURCE_IMAGE && vbr->partitionNumber > 0))    //this is hacky
        a->do_tune_fs = TRUE;

    return a;
}

//!
//! Convert emi-XXXX-YYYY to dsk-XXXX
//!
//! @param[in] src
//! @param[in] dst
//! @param[in] size
//!
//! @pre
//!
//! @note
//!
static void convert_id(const char *src, char *dst, unsigned int size)
{
    if (strcasestr(src, "emi-") == src) {
        const char *s = src + 4;       // position aftter 'emi'
        char *d = dst + strlen(dst);   // position after 'dsk' or 'emi' or whatever
        *d++ = '-';
        while ((*s >= '0') && (*s <= 'z')   // copy letters and numbers up to a hyphen
               && (d - dst < size)) {  // don't overrun dst
            *d++ = *s++;
        }
        *d = '\0';
    }
}

//!
//!
//!
//! @param[in] url
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static char *url_get_digest(const char *url)
{
    char *digest_str = NULL;
    char *digest_path = strdup("/tmp/url-digest-XXXXXX");

    if (!digest_path) {
        LOGERROR("failed to strdup digest path\n");
        return digest_path;
    }

    int tmp_fd = safe_mkstemp(digest_path);
    if (tmp_fd < 0) {
        LOGERROR("failed to create a digest file %s\n", digest_path);
    } else {
        close(tmp_fd);

        // download a fresh digest
        if (http_get_timeout(url, digest_path, 10, 4, 0, 0) != 0) {
            LOGERROR("failed to download digest to %s\n", digest_path);
        } else {
            digest_str = file2strn(digest_path, 100000);
        }
        unlink(digest_path);
    }
    EUCA_FREE(digest_path);
    return digest_str;
}

//!
//!
//!
//! @param[in] vbr
//! @param[in] do_make_work_copy
//! @param[in] is_migration_dest
//! @param[in] must_be_file
//! @param[in] sshkey
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static artifact *art_alloc_vbr(virtualBootRecord * vbr, boolean do_make_work_copy, boolean is_migration_dest, boolean must_be_file, const char *sshkey)
{
    artifact *a = NULL;
    char *blob_digest = NULL;

    switch (vbr->locationType) {
    case NC_LOCATION_CLC:
        LOGERROR("[%s] location of type %d is NOT IMPLEMENTED\n", current_instanceId, vbr->locationType);
        return NULL;

    case NC_LOCATION_URL:{
            // get the digest for size and signature
            char manifestURL[EUCA_MAX_PATH] = "";
            snprintf(manifestURL, EUCA_MAX_PATH, "%s.manifest.xml", vbr->preparedResourceLocation);
            blob_digest = url_get_digest(manifestURL);
            if (blob_digest == NULL)
                goto u_out;

            // extract size from the digest
            long long bb_size_bytes = euca_strtoll(blob_digest, "<size>", "</size>");   // pull size from the digest
            if (bb_size_bytes < 1)
                goto u_out;
            vbr->sizeBytes = bb_size_bytes; // record size in VBR now that we know it

            // generate ID of the artifact (append -##### hash of sig)
            char art_id[48];
            if (art_gen_id(art_id, sizeof(art_id), vbr->id, blob_digest) != EUCA_OK)
                goto u_out;

            // allocate artifact struct
            a = art_alloc(art_id, art_id, bb_size_bytes, !is_migration_dest, must_be_file, FALSE, url_creator, vbr);

u_out:
            EUCA_FREE(blob_digest);
            break;
        }
    case NC_LOCATION_OBJECT_STORAGE:{
            // get the digest for size and signature
            if ((blob_digest = objectstorage_get_digest(vbr->preparedResourceLocation)) == NULL) {
                LOGERROR("[%s] failed to obtain image digest from  objectstorage\n", current_instanceId);
                goto w_out;
            }
            // extract size from the digest
            long long bb_size_bytes = euca_strtoll(blob_digest, "<size>", "</size>");   // pull size from the digest
            if (bb_size_bytes < 1) {
                LOGERROR("[%s] incorrect image digest or error returned from objectstorage\n", current_instanceId);
                goto w_out;
            }
            vbr->sizeBytes = bb_size_bytes; // record size in VBR now that we know it

            // generate ID of the artifact (append -##### hash of sig)
            char art_id[48];
            if (art_gen_id(art_id, sizeof(art_id), vbr->id, blob_digest) != EUCA_OK) {
                LOGERROR("[%s] failed to generate artifact id\n", current_instanceId);
                goto w_out;
            }
            // allocate artifact struct
            a = art_alloc(art_id, art_id, bb_size_bytes, !is_migration_dest, must_be_file, FALSE, objectstorage_creator, vbr);

w_out:
            EUCA_FREE(blob_digest);
            break;
        }

    case NC_LOCATION_IMAGING:{
            // get the digest for size and signature
            if ((blob_digest = http_get2str(vbr->preparedResourceLocation)) == NULL) {
                LOGERROR("[%s] failed to obtain image digest from objectstorage\n", current_instanceId);
                goto i_out;
            }
            // extract size from the digest
            long long bb_size_bytes = euca_strtoll(blob_digest, "<unbundled-size>", "</unbundled-size>");   // pull size from the digest
            if (bb_size_bytes < 1) {
                LOGERROR("[%s] incorrect image digest or error returned from objectstorage\n", current_instanceId);
                goto i_out;
            }
            vbr->sizeBytes = bb_size_bytes; // record size in VBR now that we know it

            // generate ID of the artifact (append -##### hash of sig)
            char art_id[48];
            if (art_gen_id(art_id, sizeof(art_id), vbr->id, blob_digest) != EUCA_OK) {
                LOGERROR("[%s] failed to generate artifact id\n", current_instanceId);
                goto i_out;
            }
            // allocate artifact struct
            a = art_alloc(art_id, art_id, bb_size_bytes, !is_migration_dest, must_be_file, FALSE, imaging_creator, vbr);

i_out:
            EUCA_FREE(blob_digest);
            break;
        }

    case NC_LOCATION_FILE:{

            // get the size of the input file
            long long bb_size_bytes = file_size(vbr->preparedResourceLocation);
            if (bb_size_bytes < 1) {
                LOGERROR("[%s] invalid input file %s\n", current_instanceId, vbr->preparedResourceLocation);
                goto f_out;
            }
            vbr->sizeBytes = bb_size_bytes; // record size in VBR

            char art_id[48];
            char *path = strdup(vbr->preparedResourceLocation);
            char *name = basename(path);
            euca_strncpy(art_id, name, sizeof(art_id));
            free(path);

            // allocate artifact struct
            a = art_alloc(art_id, art_id, bb_size_bytes, !is_migration_dest, TRUE, FALSE, file_creator, vbr);

f_out:
            break;
        }

#ifndef _NO_EBS
    case NC_LOCATION_SC:{
            a = art_alloc("iscsi-vol", NULL, -1, FALSE, FALSE, FALSE, iqn_creator, vbr);
            goto out;
        }
#endif
    case NC_LOCATION_NONE:{
            assert(vbr->sizeBytes > 0L);

            char art_sig[ART_SIG_MAX]; // signature for this artifact based on its salient characteristics
            if (snprintf(art_sig, sizeof(art_sig), "id=%s size=%lld format=%s\n\n", vbr->id, vbr->sizeBytes, vbr->formatName) >= sizeof(art_sig))   // output was truncated
                break;

            char buf[32];              // first part of artifact ID
            char *art_pref;
            if (strcmp(vbr->id, "none") == 0) {
                if (snprintf(buf, sizeof(buf), "prt-%05lld%s", vbr->sizeBytes / 1048576, vbr->formatName) >= sizeof(buf))   // output was truncated
                    break;
                art_pref = buf;
            } else {
                art_pref = vbr->id;
            }

            char art_id[48];           // ID of the artifact (append -##### hash of sig)
            if (strlen(art_pref) > 18) {    // TODO: remove this length-based inference of the fact that ID should be inherited
                strcpy(art_id, art_pref);
            } else {
                if (art_gen_id(art_id, sizeof(art_id), art_pref, art_sig) != EUCA_OK)
                    break;
            }

            a = art_alloc(art_id, art_sig, vbr->sizeBytes, !is_migration_dest, must_be_file, FALSE, partition_creator, vbr);
            break;
        }
    default:
        LOGERROR("[%s] unrecognized locationType %d\n", current_instanceId, vbr->locationType);
        break;
    }
    if (a) {
        a->do_not_download = is_migration_dest;
    }
    // allocate another artifact struct if a work copy is requested
    // or if an SSH key is supplied
    if (a && (do_make_work_copy || sshkey)) {

        artifact *a2 = NULL;
        char art_id[48];
        euca_strncpy(art_id, a->id, sizeof(art_id));
        char art_sig[ART_SIG_MAX];
        euca_strncpy(art_sig, a->sig, sizeof(art_sig));

        if (sshkey) {                  // if SSH key is included, recalculate sig and ID
            if (strlen(sshkey) > sizeof(a->sshkey)) {
                LOGERROR("[%s] received SSH key is too long\n", a->instanceId);
                goto free;
            }

            char key_sig[ART_SIG_MAX];
            if ((snprintf(key_sig, sizeof(key_sig), "KEY /root/.ssh/authorized_keys\n%s\n\n", sshkey) >= sizeof(key_sig))   // output truncated
                || ((strlen(art_sig) + strlen(key_sig)) >= sizeof(art_sig))) {  // overflow
                LOGERROR("[%s] internal buffers (ART_SIG_MAX) too small for signature\n", a->instanceId);
                goto free;
            }
            strncat(art_sig, key_sig, sizeof(art_sig) - strlen(key_sig) - 1);

            char art_pref[EUCA_MAX_PATH] = "emi";
            convert_id(a->id, art_pref, sizeof(art_pref));
            if (art_gen_id(art_id, sizeof(art_id), art_pref, key_sig) != EUCA_OK) {
                goto free;
            }
        }

        a2 = art_alloc(art_id, art_sig, a->size_bytes, !do_make_work_copy, must_be_file, FALSE, copy_creator, vbr);
        if (a2) {
            a2->do_not_download = is_migration_dest;
            if (sshkey)
                euca_strncpy(a2->sshkey, sshkey, sizeof(a2->sshkey));

            if (art_add_dep(a2, a) == EUCA_OK) {
                a = a2;
            } else {
                ART_FREE(a2);
                goto free;
            }
        } else {
            goto free;
        }

        goto out;

free:
        ART_FREE(a);
    }

out:
    return a;
}

//!
//! Allocates a 'keyed' disk artifact and possibly the underlying 'raw' disk
//!
//! @param[in] vbr pointer to VBR of the newly created
//! @param[in] prereqs list of prerequisites (kernel and ramdisk), if any
//! @param[in] num_prereqs number of items in prereqs list
//! @param[in] parts OPTION A: partitions for constructing a 'raw' disk
//! @param[in] num_parts number of items in parts list
//! @param[in] emi_disk OPTION B: the artifact of the EMI that serves as a full disk
//! @param[in] do_make_bootable kernel injection is requested (not needed on KVM and Xen)
//! @param[in] do_make_work_copy generated disk should be a work copy
//! @param[in] is_migration_dest
//!
//! @return A pointer to 'keyed' disk artifact or NULL on error
//!
//! @pre
//!
//! @note
//!
static artifact *art_alloc_disk(virtualBootRecord * vbr,
                                artifact * prereqs[], int num_prereqs,
                                artifact * parts[], int num_parts, artifact * emi_disk, boolean do_make_bootable, boolean do_make_work_copy, boolean is_migration_dest)
{
    char art_sig[ART_SIG_MAX] = "";
    char art_pref[EUCA_MAX_PATH] = "dsk";
    long long disk_size_bytes = 512LL * MBR_BLOCKS;

    // run through partitions, adding up their signatures and their size
    for (int i = 0; i < num_parts; i++) {
        assert(parts);
        artifact *p = parts[i];

        // construct signature for the disk, based on the sigs of underlying components
        char part_sig[ART_SIG_MAX];
        if ((snprintf(part_sig, sizeof(part_sig), "PARTITION %d (%s)\n%s\n\n", i, p->id, p->sig) >= sizeof(part_sig))   // output truncated
            || ((strlen(art_sig) + strlen(part_sig)) >= sizeof(art_sig))) { // overflow
            LOGERROR("[%s] internal buffers (ART_SIG_MAX) too small for signature\n", current_instanceId);
            return NULL;
        }
        strncat(art_sig, part_sig, sizeof(art_sig) - strlen(art_sig) - 1);

        // verify and add up the sizes of partitions
        if (p->size_bytes < 1) {
            LOGERROR("[%s] unknown size for partition %d\n", current_instanceId, i);
            return NULL;
        }
        if (p->size_bytes % 512) {
            LOGERROR("[%s] size for partition %d is not a multiple of 512\n", current_instanceId, i);
            return NULL;
        }
        disk_size_bytes += p->size_bytes;
        convert_id(p->id, art_pref, sizeof(art_pref));
    }

    // run through prerequisites (kernel and ramdisk), if any, adding up their signature
    // (this will not happen on KVM and Xen where injecting kernel is not necessary)
    for (int i = 0; do_make_bootable && i < num_prereqs; i++) {
        artifact *p = prereqs[i];

        // construct signature for the disk, based on the sigs of underlying components
        char part_sig[ART_SIG_MAX];
        if ((snprintf(part_sig, sizeof(part_sig), "PREREQUISITE %s\n%s\n\n", p->id, p->sig) >= sizeof(part_sig))    // output truncated
            || ((strlen(art_sig) + strlen(part_sig)) >= sizeof(art_sig))) { // overflow
            LOGERROR("[%s] internal buffers (ART_SIG_MAX) too small for signature\n", current_instanceId);
            return NULL;
        }
        strncat(art_sig, part_sig, sizeof(art_sig) - strlen(art_sig) - 1);
    }

    artifact *disk;

    if (emi_disk) {                    //! we have a full disk (@TODO remove this unused if-condition)
        if (do_make_work_copy) {       // allocate a work copy of it
            disk_size_bytes = emi_disk->size_bytes;
            if ((strlen(art_sig) + strlen(emi_disk->sig)) >= sizeof(art_sig)) { // overflow
                LOGERROR("[%s] internal buffers (ART_SIG_MAX) too small for signature\n", current_instanceId);
                return NULL;
            }
            strncat(art_sig, emi_disk->sig, sizeof(art_sig) - strlen(art_sig) - 1);

            if ((disk = art_alloc(emi_disk->id, art_sig, emi_disk->size_bytes, FALSE, FALSE, FALSE, copy_creator, NULL)) == NULL || art_add_dep(disk, emi_disk) != EUCA_OK) {
                goto free;
            }
        } else {
            disk = emi_disk;           // no work copy needed - we're done
        }

    } else {                           // allocate the 'raw' disk artifact
        char art_id[48];               // ID of the artifact (append -##### hash of sig)
        if (art_gen_id(art_id, sizeof(art_id), art_pref, art_sig) != EUCA_OK)
            return NULL;

        disk = art_alloc(art_id, art_sig, disk_size_bytes, !do_make_work_copy, FALSE, TRUE, disk_creator, vbr);
        if (disk == NULL) {
            LOGERROR("[%s] failed to allocate an artifact for raw disk\n", disk->instanceId);
            return NULL;
        }
        disk->do_make_bootable = do_make_bootable;
        disk->do_not_download = is_migration_dest;

        // attach partitions as dependencies of the raw disk
        for (int i = 0; i < num_parts; i++) {
            artifact *p = parts[i];
            if (art_add_dep(disk, p) != EUCA_OK) {
                LOGERROR("[%s] failed to add dependency to an artifact\n", disk->instanceId);
                goto free;
            }
            p->is_partition = TRUE;
        }

        // optionally, attach prereqs as dependencies of the raw disk
        for (int i = 0; do_make_bootable && i < num_prereqs; i++) {
            artifact *p = prereqs[i];
            if (art_add_dep(disk, p) != EUCA_OK) {
                LOGERROR("[%s] failed to add a prerequisite to an artifact\n", disk->instanceId);
                goto free;
            }
        }
    }

    return disk;
free:
    ART_FREE(disk);
    return NULL;
}

//!
//! Allocates a 'keyed' disk artifact and possibly the underlying 'raw' disk
//!
//! @param[in] vbr pointer to VBR of the newly created
//! @param[in] prereqs list of prerequisites (kernel and ramdisk), if any
//! @param[in] num_prereqs number of items in prereqs list
//! @param[in] parts OPTION A: partitions for constructing a 'raw' disk
//! @param[in] num_parts number of items in parts list
//! @param[in] emi_disk OPTION B: the artifact of the EMI that serves as a full disk
//! @param[in] do_make_bootable kernel injection is requested (not needed on KVM and Xen)
//! @param[in] do_make_work_copy generated disk should be a work copy
//! @param[in] is_migration_dest
//!
//! @return A pointer to 'keyed' disk artifact or NULL on error
//!
//! @pre
//!
//! @note
//!
static artifact *art_realloc_disk(virtualBootRecord * vbr,
                                  artifact * prereqs[], int num_prereqs,
                                  artifact * old_disk, artifact * parts[], int num_parts, boolean do_make_bootable, boolean do_make_work_copy, boolean is_migration_dest)
{
    assert(old_disk);
    char art_sig[ART_SIG_MAX] = "";
    char art_pref[EUCA_MAX_PATH] = "dsk";
    long long disk_size_bytes = old_disk->size_bytes;

    LOGDEBUG("at realloc time old_size_bytes = %lld\n", disk_size_bytes);

    // run through partitions, adding up their signatures and their size
    for (int i = 0; i < num_parts; i++) {
        assert(parts);
        artifact *p = parts[i];

        // construct signature for the disk, based on the sigs of underlying components
        char part_sig[ART_SIG_MAX];
        if ((snprintf(part_sig, sizeof(part_sig), "PARTITION %d (%s)\n%s\n\n", i, p->id, p->sig) >= sizeof(part_sig))   // output truncated
            || ((strlen(art_sig) + strlen(part_sig)) >= sizeof(art_sig))) { // overflow
            LOGERROR("[%s] internal buffers (ART_SIG_MAX) too small for signature\n", current_instanceId);
            return NULL;
        }
        strncat(art_sig, part_sig, sizeof(art_sig) - strlen(art_sig) - 1);

        // verify and add up the sizes of partitions
        if (p->size_bytes < 1) {
            LOGERROR("[%s] unknown size for partition %d\n", current_instanceId, i);
            return NULL;
        }
        if (p->size_bytes % 512) {
            LOGERROR("[%s] size for partition %d is not a multiple of 512\n", current_instanceId, i);
            return NULL;
        }
        disk_size_bytes += p->size_bytes;
        convert_id(p->id, art_pref, sizeof(art_pref));
    }

    // run through prerequisites (kernel and ramdisk), if any, adding up their signature
    // (this will not happen on KVM and Xen where injecting kernel is not necessary)
    for (int i = 0; do_make_bootable && i < num_prereqs; i++) {
        artifact *p = prereqs[i];

        // construct signature for the disk, based on the sigs of underlying components
        char part_sig[ART_SIG_MAX];
        if ((snprintf(part_sig, sizeof(part_sig), "PREREQUISITE %s\n%s\n\n", p->id, p->sig) >= sizeof(part_sig))    // output truncated
            || ((strlen(art_sig) + strlen(part_sig)) >= sizeof(art_sig))) { // overflow
            LOGERROR("[%s] internal buffers (ART_SIG_MAX) too small for signature\n", current_instanceId);
            return NULL;
        }
        strncat(art_sig, part_sig, sizeof(art_sig) - strlen(art_sig) - 1);
    }

    char art_id[48];                   // ID of the artifact (append -##### hash of sig)
    if (art_gen_id(art_id, sizeof(art_id), art_pref, art_sig) != EUCA_OK)
        return NULL;
    artifact *disk = art_alloc(art_id, art_sig, disk_size_bytes, !do_make_work_copy, FALSE, TRUE, disk_expander, vbr);
    if (disk == NULL) {
        LOGERROR("[%s] failed to allocate an artifact for raw disk\n", disk->instanceId);
        return NULL;
    }
    disk->do_make_bootable = do_make_bootable;
    disk->do_not_download = is_migration_dest;

    /* TODO: are these important?
       a->may_be_cached = may_be_cached;
       a->must_be_file = must_be_file;
       a->must_be_hollow = must_be_hollow;
       a->do_tune_fs = FALSE;
     */

    // attach the smaller disk as a dependency
    if (art_add_dep(disk, old_disk) != EUCA_OK) {
        LOGERROR("[%s] failed to add old_disk as dependency to an artifact\n", disk->instanceId);
        goto free;
    }
    old_disk->sig[0] = '\0';           // temporarily suspend sig checking (TODO remove this)

    // attach partitions as dependencies of the raw disk
    for (int i = 0; i < num_parts; i++) {
        artifact *p = parts[i];
        if (art_add_dep(disk, p) != EUCA_OK) {
            LOGERROR("[%s] failed to add dependency to an artifact\n", disk->instanceId);
            goto free;
        }
        p->is_partition = TRUE;
    }

    // optionally, attach prereqs as dependencies of the raw disk
    for (int i = 0; do_make_bootable && i < num_prereqs; i++) {
        artifact *p = prereqs[i];
        if (art_add_dep(disk, p) != EUCA_OK) {
            LOGERROR("[%s] failed to add a prerequisite to an artifact\n", disk->instanceId);
            goto free;
        }
    }

    return disk;
free:
    EUCA_FREE(disk);
    return NULL;
}

//!
//! Sets instance ID in thread-local variable, for logging (same effect as
//! passing it into vbr_alloc_tree)
//!
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @pre
//!
//! @note
//!
void art_set_instanceId(const char *instanceId)
{
    euca_strncpy(current_instanceId, instanceId, sizeof(current_instanceId));
}

//!
//! Creates a tree of artifacts for a given VBR (caller must free the tree)
//!
//! @param[in] vm pointer to virtual machine containing the VBR
//! @param[in] do_make_bootable make the disk bootable by copying kernel and ramdisk into it and running grub
//! @param[in] do_make_work_copy ensure that all components that get modified at run time have work copies
//! @param[in] is_migration_dest
//! @param[in] sshkey key to inject into the root partition or NULL if no key
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return A pointer to the root of artifact tree or NULL on error
//!
//! @pre
//!
//! @note
//!
artifact *vbr_alloc_tree(virtualMachine * vm, boolean do_make_bootable, boolean do_make_work_copy, boolean is_migration_dest, const char *sshkey, const char *instanceId)
{
    if (instanceId)
        euca_strncpy(current_instanceId, instanceId, sizeof(current_instanceId));

    // sort vbrs into prereq [] and parts[] so they can be approached in the right order
    virtualBootRecord *prereq_vbrs[EUCA_MAX_VBRS];
    int total_prereq_vbrs = 0;
    bzero(prereq_vbrs, EUCA_MAX_VBRS * sizeof(virtualBootRecord *));

    virtualBootRecord *parts[BUS_TYPES_TOTAL][EUCA_MAX_DISKS][EUCA_MAX_PARTITIONS];
    int total_parts = 0;
    bzero(parts, BUS_TYPES_TOTAL * EUCA_MAX_DISKS * EUCA_MAX_PARTITIONS * sizeof(virtualBootRecord *));

    for (int i = 0; i < EUCA_MAX_VBRS && i < vm->virtualBootRecordLen; i++) {
        virtualBootRecord *vbr = &(vm->virtualBootRecord[i]);
        if (vbr->type == NC_RESOURCE_KERNEL || vbr->type == NC_RESOURCE_RAMDISK) {
            prereq_vbrs[total_prereq_vbrs++] = vbr;
        } else {
            parts[vbr->guestDeviceBus][vbr->diskNumber][vbr->partitionNumber] = vbr;
            total_parts++;
        }
    }
    LOGDEBUG("[%s] found %d prereqs and %d partitions/disks in the VBR\n", instanceId, total_prereq_vbrs, total_parts);

    artifact *root = art_alloc(instanceId, NULL, -1, FALSE, FALSE, FALSE, NULL, NULL);  // allocate a sentinel artifact
    if (root == NULL)
        return NULL;

    // allocate kernel and ramdisk artifacts and maybe attach them to the sentinel
    artifact *prereq_arts[EUCA_MAX_VBRS];
    int total_prereq_arts = 0;
    for (int i = 0; i < total_prereq_vbrs; i++) {
        virtualBootRecord *vbr = prereq_vbrs[i];
        artifact *dep = art_alloc_vbr(vbr, do_make_work_copy, FALSE, TRUE, NULL);   // is_migration_dest==FALSE because eki and eri *must* be filled in for migration
        if (dep == NULL)
            goto free;
        prereq_arts[total_prereq_arts++] = dep;

        // if disk does not need to be bootable, we'll need
        // kernel and ramdisk as top-level dependencies
        if (!do_make_bootable)
            if (art_add_dep(root, dep) != EUCA_OK)
                goto free;
    }

    // attach disks and partitions and attach them to the sentinel
    for (int i = 0; i < BUS_TYPES_TOTAL; i++) {
        for (int j = 0; j < EUCA_MAX_DISKS; j++) {
            int partitions = 0;
            artifact *disk_arts[EUCA_MAX_PARTITIONS];
            bzero(disk_arts, EUCA_MAX_PARTITIONS * sizeof(artifact *));
            for (int k = 0; k < EUCA_MAX_PARTITIONS; k++) {
                virtualBootRecord *vbr = parts[i][j][k];
                const char *use_sshkey = NULL;
                if (vbr) {             // either a disk (k==0) or a partition (k>0)
                    if (vbr->type == NC_RESOURCE_IMAGE && k > 0) {  // only inject SSH key into an EMI which has a single partition (whole disk)
                        use_sshkey = sshkey;
                    }
                    disk_arts[k] = art_alloc_vbr(vbr, do_make_work_copy, is_migration_dest, FALSE, use_sshkey); // this brings in disks or partitions and their work copies, if requested
                    if (disk_arts[k] == NULL) {
                        arts_free(disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }
                    if (vbr->type == NC_RESOURCE_EBS)   // EBS-backed instances need no additional artifacts
                        continue;
                    if (k > 0) {
                        partitions++;
                    }
                }
            }
            if (partitions) {          // there were partitions
                if (disk_arts[0] == NULL) { // no disk was specified, so we'll be creating one from paritions
                    if (vm->virtualBootRecordLen == EUCA_MAX_VBRS) {
                        LOGERROR("[%s] out of room in the VBR[] while adding disk %d on bus %d\n", instanceId, j, i);
                        goto out;
                    }
                    disk_arts[0] = art_alloc_disk(&(vm->virtualBootRecord[vm->virtualBootRecordLen]), prereq_arts, total_prereq_arts,   // the prereqs
                                                  disk_arts + 1, partitions,    // the partition artifacts
                                                  NULL, do_make_bootable, do_make_work_copy, is_migration_dest);
                    if (disk_arts[0] == NULL) {
                        arts_free(disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }
                    vm->virtualBootRecordLen++;
                } else {
                    disk_arts[0] = art_realloc_disk(&(vm->virtualBootRecord[vm->virtualBootRecordLen]), prereq_arts, total_prereq_arts, // the prereqs
                                                    disk_arts[0],   // the disk artifact
                                                    disk_arts + 2, partitions,  // the partition artifacts (TODO: fix the "+2")
                                                    do_make_bootable, do_make_work_copy, is_migration_dest);
                    if (disk_arts[0] == NULL) {
                        arts_free(disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }
                }
            }
            // run though all disk artifacts and either add the disk or all the partitions to sentinel
            for (int k = 0; k < EUCA_MAX_PARTITIONS; k++) {
                if (disk_arts[k]) {
                    if (art_add_dep(root, disk_arts[k]) != EUCA_OK) {
                        arts_free(disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }
                    disk_arts[k] = NULL;
                    if (k == 0) {      // for a disk partition artifacts, if any, are already attached to it
                        break;
                    }
                }
            }
        }
    }
    art_print_tree("", root);
    goto out;

free:
    ART_FREE(root);

out:
    return root;
}

//!
//! Either opens a blockblob or creates it
//!
//! @param[in]  flags determine whether blob is created or opened
//! @param[in]  bs pointer to the blobstore in which to open/create blockblob
//! @param[in]  id identifier of the blockblob
//! @param[in]  size_bytes size of the blockblob
//! @param[in]  sig signature of the blockblob
//! @param[out] bbp RESULT: opened blockblob handle or NULL if EUCA_ERROR is returned
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int find_or_create_blob(int flags, blobstore * bs, const char *id, long long size_bytes, const char *sig, blockblob ** bbp)
{
    blockblob *bb = NULL;
    int ret = EUCA_OK;

    // open with a short timeout (0-1000 usec), as we do not want to block
    // here - we let higher-level functions do retries if necessary
    bb = blockblob_open(bs, id, size_bytes, flags, sig, FIND_BLOB_TIMEOUT_USEC);
    if (bb) {                          // success!
        *bbp = bb;
    } else {
        ret = blobstore_get_error();
    }

    return (ret);
}

//!
//! Finds and opens or creates artifact's blob either in cache or in work blobstore
//!
//! @param[in]  do_create create if non-zero, open if 0
//! @param[in]  a pointer to artifact to create or open
//! @param[in]  work_bs pointer to work blobstore
//! @param[in]  cache_bs pointer to OPTIONAL cache blobstore
//! @param[in]  work_prefix OPTIONAL instance-specific prefix for forming work blob IDs
//! @param[out] bbp RESULT: opened blockblob handle or NULL if EUCA_ERROR is returned
//!
//! @return EUCA_OK or BLOBSTORE_ERROR_ error codes, will set a->is_in_cache if blob is found or created in cache
//!
//! @pre
//!
//! @note
//!
static int find_or_create_artifact(int do_create, artifact * a, blobstore * work_bs, blobstore * cache_bs, const char *work_prefix, blockblob ** bbp)
{
    int ret = EUCA_ERROR;
    assert(a);

    // determine blob IDs for cache and work
    const char *id_cache = a->id;
    char id_work[BLOBSTORE_MAX_PATH];
    if (work_prefix && strlen(work_prefix))
        snprintf(id_work, sizeof(id_work), "%s/%s", work_prefix, a->id);
    else
        euca_strncpy(id_work, a->id, sizeof(id_work));

    // determine flags
    int flags = 0;
    if (do_create) {
        flags |= BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL;
    }
    if (a->must_be_hollow) {
        flags |= BLOBSTORE_FLAG_HOLLOW;
    }
    // see if a file and if it exists
    if (a->id_is_path) {
        if (check_path(a->id)) {
            if (do_create) {
                return (EUCA_OK);      // creating only matters for blobs, which get locked, not for files
            } else {
                return BLOBSTORE_ERROR_NOENT;
            }
        } else {
            return (EUCA_OK);
        }
    }

    assert(work_bs);
    long long size_bytes;
    if (do_create) {
        size_bytes = a->size_bytes;
    } else {
        // do not verify size when opening blobs because some
        // conversions may change them, instead just rely on
        // signature comparison to validate the blobs
        size_bytes = 0;
    }

    // for a blob first try cache as long as we're allowed to and have one
    if (a->may_be_cached && cache_bs) {
        ret = find_or_create_blob(flags, cache_bs, id_cache, size_bytes, a->sig, bbp);

        // for some error conditions from cache we try work blobstore
        if ((do_create && ret == BLOBSTORE_ERROR_NOSPC) || (!do_create && ret == BLOBSTORE_ERROR_NOENT) || (ret == BLOBSTORE_ERROR_SIGNATURE)
            // these reduce reliance on cache (work copies are created more aggressively)
            //|| ret==BLOBSTORE_ERROR_NOENT
            //|| ret==BLOBSTORE_ERROR_AGAIN
            //|| ret==BLOBSTORE_ERROR_EXIST
            ) {
            if (ret == BLOBSTORE_ERROR_SIGNATURE)
                a->may_be_cached = FALSE;   // so we won't check cache on future invocations
            goto try_work;

        } else {                       // for all others we return the error or success
            if (ret == BLOBSTORE_ERROR_OK)
                a->is_in_cache = TRUE;
            return (ret);
        }
    }
try_work:
    if (ret == BLOBSTORE_ERROR_SIGNATURE) {
        LOGWARN("[%s] signature mismatch on cached blob %03d|%s\n", a->instanceId, a->seq, id_cache);   // TODO: maybe invalidate?
    }
    LOGDEBUG("[%s] checking work blobstore for %03d|%s (do_create=%d ret=%d)\n", a->instanceId, a->seq, id_cache, do_create, ret);
    return find_or_create_blob(flags, work_bs, id_work, size_bytes, a->sig, bbp);
}

//!
//! Traverse artifact tree and create/download/combine artifacts
//!
//! Given a root node in a tree of blob artifacts, unless the root
//! blob already exists and has the right signature, this function:
//!
//! \li ensures that any depenent blobs are present and open
//! \li creates the root blob and invokes to creator function to fill it
//! \li closes any dependent blobs
//!
//! The function is recursive and the contract is that when it returns
//!
//! \li with success, the root blob is open and ready
//! \li with failure, the root blob is closed and possibly non-existant
//!
//! Either way, none of the child blobs are open.
//!
//! @param[in] root pointer to root of the tree
//! @param[in] work_bs pointero to work blobstore
//! @param[in] cache_bs pointer to OPTIONAL cache blobstore
//! @param[in] work_prefix OPTIONAL instance-specific prefix for forming work blob IDs
//! @param[in] timeout_usec timeout for the whole process, in microseconds or 0 for no timeout
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int art_implement_tree(artifact * root, blobstore * work_bs, blobstore * cache_bs, const char *work_prefix, long long timeout_usec)
{
    long long started = time_usec();
    assert(root);

    LOGDEBUG("[%s] implementing artifact %03d|%s\n", root->instanceId, root->seq, root->id);

    int ret = EUCA_OK;
    int tries = 0;
    do {                               // we may have to retry multiple times due to competition
        int num_opened_deps = 0;
        boolean do_deps = TRUE;
        boolean do_create = TRUE;

        if (tries++)
            usleep(ARTIFACT_RETRY_SLEEP_USEC);

        if (!root->creator) {          // sentinel nodes do not have a creator
            do_create = FALSE;

        } else {                       // not a sentinel
            if (root->vbr && root->vbr->type == NC_RESOURCE_EBS)
                goto create;           // EBS artifacts have no disk manifestation and no dependencies, so skip to creation

            // try to open the artifact
            switch (ret = find_or_create_artifact(FIND, root, work_bs, cache_bs, work_prefix, &(root->bb))) {
            case BLOBSTORE_ERROR_OK:
                LOGDEBUG("[%s] found existing artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
                if (work_bs && blockblob_get_blobstore(root->bb) == work_bs)
                    update_vbr_with_backing_info(root);
                do_deps = FALSE;
                do_create = FALSE;
                break;
            case BLOBSTORE_ERROR_NOENT:    // doesn't exist yet => ok, create it
                break;
            case BLOBSTORE_ERROR_AGAIN:    // timed out the => competition took too long
            case BLOBSTORE_ERROR_MFILE:    // out of file descriptors for locking => same problem
                goto retry_or_fail;
                break;
            default:                  // all other errors
                LOGERROR("[%s] failed to provision artifact %03d|%s (error=%d) on try %d\n", root->instanceId, root->seq, root->id, ret, tries);
                goto retry_or_fail;
            }
        }

        // at this point the artifact we need does not seem to exist
        // (though it could be created before we get around to that)

        if (do_deps) {                 // recursively go over dependencies, if any
            for (int i = 0; i < MAX_ARTIFACT_DEPS && root->deps[i]; i++) {

                // recalculate the time that remains in the timeout period
                long long new_timeout_usec = timeout_usec;
                if (timeout_usec > 0) {
                    new_timeout_usec -= time_usec() - started;
                    if (new_timeout_usec < 1) { // timeout exceeded, so bail out of this function
                        ret = BLOBSTORE_ERROR_AGAIN;
                        goto retry_or_fail;
                    }
                }
                switch (ret = art_implement_tree(root->deps[i], work_bs, cache_bs, work_prefix, new_timeout_usec)) {
                case BLOBSTORE_ERROR_OK:
                    if (do_create) {   // we'll hold the dependency open for the creator
                        num_opened_deps++;
                    } else {           // this is a sentinel, we're not creating anything, so release the dep immediately
                        if (root->deps[i]->bb && (blockblob_close(root->deps[i]->bb) == -1)) {
                            LOGERROR("[%s] failed to close dependency of %s: %d %s (potential resource leak!) on try %d\n",
                                     root->instanceId, root->id, blobstore_get_error(), blobstore_get_last_msg(), tries);
                        }
                        root->deps[i]->bb = 0;  // for debugging
                    }
                    break;             // out of the switch statement
                case BLOBSTORE_ERROR_AGAIN:    // timed out => the competition took too long
                case BLOBSTORE_ERROR_MFILE:    // out of file descriptors for locking => same problem
                    goto retry_or_fail;
                default:              // all other errors
                    LOGERROR("[%s] failed to provision dependency %s for artifact %s (error=%d) on try %d\n", root->instanceId, root->deps[i]->id, root->id, ret, tries);
                    goto retry_or_fail;
                }
            }
        }
        // at this point the dependencies, if any, needed to create
        // the artifact, have been created and opened (i.e. locked
        // for exclusive use by this process and thread)

        if (do_create) {
            // shortcut for a case where a copy creator has a dependency that
            // could have been cached, but was not, so a copy is not necessary
            if (root->creator == copy_creator && root->deps[0] && root->deps[1] == NULL && root->deps[0]->may_be_cached && !root->deps[0]->is_in_cache
                && strcmp(root->id, root->deps[0]->id)) {
                // set blockblob pointer to the dependency's blockblob pointer
                // and the dependency's blockblob pointer to NULL --
                // copy_creator will notice this special condition and will
                // skip the copy (but not SSH key injection)
                LOGDEBUG("[%s] bypassing redundant artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
                root->bb = root->deps[0]->bb;
                root->deps[0]->bb = NULL;
                num_opened_deps--;     // so we won't attempt to close deps's blockblob
            } else {

                // try to create the artifact since last time we checked it did not exist
                switch (ret = find_or_create_artifact(CREATE, root, work_bs, cache_bs, work_prefix, &(root->bb))) {
                case BLOBSTORE_ERROR_OK:
                    LOGDEBUG("[%s] created a blob for an artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
                    break;
                case BLOBSTORE_ERROR_EXIST:    // someone else created it => loop back and open it
                    ret = BLOBSTORE_ERROR_AGAIN;
                    goto retry_or_fail;
                    break;

                case BLOBSTORE_ERROR_AGAIN:    // timed out (but probably exists)
                case BLOBSTORE_ERROR_MFILE:    // out of file descriptors for locking => same problem
                    goto retry_or_fail;
                    break;
                default:              // all other errors
                    LOGERROR("[%s] failed to allocate artifact %s (%d %s) on try %d\n", root->instanceId, root->id, ret, blobstore_get_last_msg(), tries);
                    goto retry_or_fail;
                }
            }

create:
            ret = root->creator(root); // create and open this artifact for exclusive use
            if (ret != EUCA_OK) {
                LOGERROR("[%s] failed to create artifact %s (error=%d, may retry) on try %d\n", root->instanceId, root->id, ret, tries);
                // delete the partially created artifact so we can retry with a clean slate
                if (root->id_is_path) { // artifact is not a blob, but a file
                    unlink(root->id);  // attempt to delete, but it may not even exist

                } else {
                    if (blockblob_delete(root->bb, DELETE_BLOB_TIMEOUT_USEC, 0) == -1) {
                        // failure of 'delete' is bad, since we may have an open blob
                        // that will prevent others from ever opening it again, so at
                        // least try to close it
                        LOGERROR("[%s] failed to remove partially created artifact %s: %d %s (potential resource leak!) on try %d\n",
                                 root->instanceId, root->id, blobstore_get_error(), blobstore_get_last_msg(), tries);
                        if (blockblob_close(root->bb) == -1) {
                            LOGERROR("[%s] failed to close partially created artifact %s: %d %s (potential deadlock!) on try %d\n",
                                     root->instanceId, root->id, blobstore_get_error(), blobstore_get_last_msg(), tries);
                        }
                    }
                }
            } else {
                if (root->vbr && root->vbr->type != NC_RESOURCE_EBS)
                    if (work_bs && blockblob_get_blobstore(root->bb) == work_bs)
                        update_vbr_with_backing_info(root);
            }
        }

retry_or_fail:
        // close all opened dependent blobs, whether we're trying again or returning
        for (int i = 0; i < num_opened_deps; i++) {
            if (root->deps[i]->bb != NULL)
                blockblob_close(root->deps[i]->bb);
            root->deps[i]->bb = 0;     // for debugging
        }

    } while ((ret == BLOBSTORE_ERROR_AGAIN || ret == BLOBSTORE_ERROR_MFILE) // only timeout-type error causes us to keep trying
             && (timeout_usec == 0     // indefinitely if there is no timeout at all
                 || (time_usec() - started) < timeout_usec));   // or until we exceed the timeout

    if (ret != EUCA_OK) {
        LOGDEBUG("[%s] failed to implement artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
    } else {
        LOGDEBUG("[%s] implemented artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
    }

    return (ret);
}

#ifdef _UNIT_TEST
//!
//!
//!
//! @param[in] size_blocks
//! @param[in] base
//! @param[in] name
//! @param[in] format
//! @param[in] revocation
//! @param[in] snapshot
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static blobstore *create_teststore(int size_blocks, const char *base, const char *name, blobstore_format_t format, blobstore_revocation_t revocation, blobstore_snapshot_t snapshot)
{
    static int ts = 0;
    if (ts == 0) {
        ts = ((int)time(NULL)) - 1292630988;
    }

    char bs_path[PATH_MAX];
    snprintf(bs_path, sizeof(bs_path), "%s/test_vbr_%05d_%s", base, ts, name);
    if (mkdir(bs_path, 0777) == -1) {
        printf("failed to create %s\n", bs_path);
        return NULL;
    }
    printf("created %s\n", bs_path);
    blobstore *bs = blobstore_open(bs_path, size_blocks, BLOBSTORE_FLAG_CREAT, format, revocation, snapshot);
    if (bs == NULL) {
        printf("ERROR: %s\n", blobstore_get_error_str(blobstore_get_error()));
        return NULL;
    }
    return bs;
}

//!
//! This function sets the fields in a VBR that are required for artifact processing
//!
//! @param[in] vm
//! @param[in] size
//! @param[in] format
//! @param[in] formatName
//! @param[in] id
//! @param[in] type
//! @param[in] locationType
//! @param[in] diskNumber
//! @param[in] partitionNumber
//! @param[in] guestDeviceBus
//! @param[in] preparedResourceLocation
//!
//! @pre
//!
//! @note
//!
static void add_vbr(virtualMachine * vm,
                    long long sizeBytes,
                    ncResourceFormatType format,
                    char *formatName,
                    const char *id, ncResourceType type, ncResourceLocationType locationType, int diskNumber, int partitionNumber,
                    libvirtBusType guestDeviceBus, char *preparedResourceLocation)
{
    virtualBootRecord *vbr = vm->virtualBootRecord + vm->virtualBootRecordLen++;
    vbr->sizeBytes = sizeBytes;
    if (formatName)
        euca_strncpy(vbr->formatName, formatName, sizeof(vbr->formatName));
    if (id)
        euca_strncpy(vbr->id, id, sizeof(vbr->id));
    vbr->format = format;
    vbr->type = type;
    vbr->locationType = locationType;
    vbr->diskNumber = diskNumber;
    vbr->partitionNumber = partitionNumber;
    vbr->guestDeviceBus = guestDeviceBus;
    if (preparedResourceLocation)
        euca_strncpy(vbr->preparedResourceLocation, preparedResourceLocation, sizeof(vbr->preparedResourceLocation));
}

//!
//!
//!
//! @param[in] id
//! @param[in] sshkey
//! @param[in] eki
//! @param[in] eri
//! @param[in] emi
//! @param[in] cache_bs
//! @param[in] work_bs
//! @param[in] do_make_work_copy
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int provision_vm(const char *id, const char *sshkey, const char *eki, const char *eri, const char *emi, blobstore * cache_bs, blobstore * work_bs, boolean do_make_work_copy)
{
    pthread_mutex_lock(&competitors_mutex);
    virtualMachine *vm = &(vm_slots[next_instances_slot]);  // we don't use vm_slots[] pointers in code
    euca_strncpy(vm_ids[next_instances_slot], id, PATH_MAX);
    next_instances_slot++;
    pthread_mutex_unlock(&competitors_mutex);

    bzero(vm, sizeof(*vm));
    add_vbr(vm, EKI_SIZE, NC_FORMAT_NONE, "none", eki, NC_RESOURCE_KERNEL, NC_LOCATION_NONE, 0, 0, 0, NULL);
    add_vbr(vm, EKI_SIZE, NC_FORMAT_NONE, "none", eri, NC_RESOURCE_RAMDISK, NC_LOCATION_NONE, 0, 0, 0, NULL);
    add_vbr(vm, VBR_SIZE, NC_FORMAT_EXT3, "ext3", emi, NC_RESOURCE_IMAGE, NC_LOCATION_NONE, 0, 1, BUS_TYPE_SCSI, NULL);
    add_vbr(vm, VBR_SIZE, NC_FORMAT_EXT3, "ext3", "none", NC_RESOURCE_EPHEMERAL, NC_LOCATION_NONE, 0, 3, BUS_TYPE_SCSI, NULL);
    add_vbr(vm, VBR_SIZE, NC_FORMAT_SWAP, "swap", "none", NC_RESOURCE_SWAP, NC_LOCATION_NONE, 0, 2, BUS_TYPE_SCSI, NULL);

    euca_strncpy(current_instanceId, strstr(id, "/") + 1, sizeof(current_instanceId));
    artifact *sentinel = vbr_alloc_tree(vm, FALSE, do_make_work_copy, FALSE, sshkey, id);
    if (sentinel == NULL) {
        printf("error: vbr_alloc_tree failed id=%s\n", id);
        return (1);
    }

    printf("implementing artifact tree sentinel=%012lx\n", (unsigned long)sentinel);
    int ret;
    if ((ret = art_implement_tree(sentinel, work_bs, cache_bs, id, 1000000LL * 60 * 2)) != EUCA_OK) {
        printf("error: art_implement_tree failed ret=%d sentinel=%012lx\n", ret, (unsigned long)sentinel);
        return (1);
    }

    pthread_mutex_lock(&competitors_mutex);
    provisioned_instances++;
    pthread_mutex_unlock(&competitors_mutex);

    printf("freeing artifact tree sentinel=%012lx\n", (unsigned long)sentinel);
    ART_FREE(sentinel);

    return (0);
}

//!
//! Cleans up all provisioned VMs
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int cleanup_vms(void)
{
    int errors = 0;

    pthread_mutex_lock(&competitors_mutex);
    for (int i = 0; i < provisioned_instances; i++) {
        char *id = vm_ids[next_instances_slot - i - 1];
        char regex[PATH_MAX];
        snprintf(regex, sizeof(regex), "%s/.*", id);
        errors += (blobstore_delete_regex(work_bs, regex) < 0);
    }
    provisioned_instances = 0;
    pthread_mutex_unlock(&competitors_mutex);

    return errors;
}

//!
//!
//!
//! @param[in] id
//! @param[in] id_len
//! @param[in] prefix
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static char *gen_id(char *id, unsigned int id_len, const char *prefix)
{
    snprintf(id, id_len, "%s/i-%08x", prefix, rand());
    return id;
}

//!
//!
//!
//! @param[in] ptr
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static void *competitor_function(void *ptr)
{
    int errors = 0;
    pid_t pid = -1;

    if (do_fork) {
        pid = fork();
        if (pid < 0) {                 // fork problem
            *(long long *)ptr = 1;
            return NULL;

        } else if (pid > 0) {          // parent
            int status;
            waitpid(pid, &status, 0);
            *(long long *)ptr = WEXITSTATUS(status);
            return NULL;
        }
    }

    if (pid < 1) {
        printf("%u/%u: competitor running (provisioned=%d)\n", (unsigned int)pthread_self(), (int)getpid(), provisioned_instances);

        for (int i = 0; i < COMPETITIVE_ITERATIONS; i++) {
            char id[32];
            errors += provision_vm(GEN_ID(), KEY1, EKI1, ERI1, EMI2, cache_bs, work_bs, TRUE);
            usleep((long long)(100 * ((double)random() / RAND_MAX)));
        }

        printf("%u/%u: competitor done (provisioned=%d errors=%d)\n", (unsigned int)pthread_self(), (int)getpid(), provisioned_instances, errors);
    }

    if (pid == 0) {
        exit(errors);
    }

    *(long long *)ptr = errors;
    return NULL;
}

// check if the blobstore has the expected number of 'block' entries
//!
//!
//!
//! @param[in] bs
//! @param[in] keyword
//! @param[in] expect
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int check_blob(blobstore * bs, const char *keyword, int expect)
{
    char cmd[1024];
    snprintf(cmd, sizeof(cmd), "find %s | grep %s | wc -l", bs->path, keyword);
    FILE *f = popen(cmd, "r");
    if (!f) {
        printf("error: failed to popen() command '%s'\n", cmd);
        perror("test_vbr");
        return (1);
    }

    char buf[32];
    int bytes;
    if ((bytes = fread(buf, 1, sizeof(buf) - 1, f)) < 1) {
        printf("error: failed to fread() from output of '%s' (returned %d)\n", cmd, bytes);
        perror("test_vbr");
        pclose(f);
        return (1);
    }
    buf[bytes] = '\0';

    if (pclose(f)) {
        printf("error: failed pclose()\n");
        perror("test_vbr");
        return (1);
    }

    int found = atoi(buf);
    if (found != expect) {
        printf("warning: unexpected disk state: [%s] = %d != %d\n", cmd, found, expect);
        return (1);
    }
    return (0);
}

//!
//!
//!
//! @param[in] msg
//!
//! @pre
//!
//! @note
//!
static void dummy_err_fn(const char *msg)
{
    LOGDEBUG("BLOBSTORE: %s\n", msg);
}

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char **argv)
{
    char id[32];
    int errors = 0;
    int warnings = 0;
    char cwd[1024];

    if (getcwd(cwd, sizeof(cwd)) != NULL) {
        srandom(time(NULL));
        blobstore_set_error_function(dummy_err_fn);

        printf("testing vbr.c\n");

        cache_bs = create_teststore(BS_SIZE, cwd, "cache", BLOBSTORE_FORMAT_DIRECTORY, BLOBSTORE_REVOCATION_LRU, BLOBSTORE_SNAPSHOT_ANY);
        work_bs = create_teststore(BS_SIZE, cwd, "work", BLOBSTORE_FORMAT_FILES, BLOBSTORE_REVOCATION_NONE, BLOBSTORE_SNAPSHOT_ANY);

        if (cache_bs == NULL || work_bs == NULL) {
            printf("error: failed to create blobstores\n");
            errors++;
            goto out;
        }

        printf("running test that only uses cache blobstore\n");
        if (errors += provision_vm(GEN_ID(), KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, FALSE))
            goto out;
        printf("provisioned first VM\n\n\n\n");
        if (errors += provision_vm(GEN_ID(), KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, FALSE))
            goto out;
        printf("provisioned second VM\n\n\n\n");
        if (errors += provision_vm(GEN_ID(), KEY2, EKI1, ERI1, EMI1, cache_bs, work_bs, FALSE))
            goto out;
        printf("provisioned third VM with a different key\n\n\n\n");
        if (errors += provision_vm(GEN_ID(), KEY2, EKI1, ERI1, EMI1, cache_bs, work_bs, FALSE))
            goto out;
        printf("provisioned fourth VM\n\n\n\n");
        if (errors += provision_vm(GEN_ID(), KEY2, EKI1, ERI1, EMI2, cache_bs, work_bs, FALSE))
            goto out;
        printf("provisioned fifth VM with different EMI\n\n\n\n");
        if (errors += provision_vm(GEN_ID(), KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, FALSE))
            goto out;

        check_blob(work_bs, "blocks", 0);
        printf("cleaning cache blobstore\n");
        blobstore_delete_regex(cache_bs, ".*");
        check_blob(cache_bs, "blocks", 0);

        printf("done with vbr.c cache-only test errors=%d warnings=%d\n", errors, warnings);

        printf("\n\n\n\n\nrunning test with use of work blobstore\n");

        int emis_in_use = 1;
        if (errors += provision_vm(GEN_ID(), KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, TRUE))
            goto out;

#define CHECK_BLOBS \
    warnings += check_blob (cache_bs, "blocks", 4 + 1 * emis_in_use);   \
    warnings += check_blob (work_bs, "blocks", 6 * provisioned_instances);
        CHECK_BLOBS;
        warnings += cleanup_vms();
        CHECK_BLOBS;

        for (int i = 0; i < SERIAL_ITERATIONS; i++) {
            errors += provision_vm(GEN_ID(), KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, TRUE);
        }
        if (errors) {
            printf("error: failed sequential instance provisioning test\n");
        }
        CHECK_BLOBS;
        warnings += cleanup_vms();
        CHECK_BLOBS;

        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                do_fork = 0;
            } else {
                do_fork = 1;
            }
            printf("===============================================\n");
            printf("spawning %d competing %s\n", COMPETITIVE_PARTICIPANTS, (do_fork) ? ("processes") : ("threads"));
            emis_in_use++;             // we'll have threads creating a new EMI
            pthread_t threads[COMPETITIVE_PARTICIPANTS];
            long long thread_par[COMPETITIVE_PARTICIPANTS];
            int thread_par_sum = 0;
            for (int j = 0; j < COMPETITIVE_PARTICIPANTS; j++) {
                pthread_create(&threads[j], NULL, competitor_function, (void *)&thread_par[j]);
            }
            for (int j = 0; j < COMPETITIVE_PARTICIPANTS; j++) {
                pthread_join(threads[j], NULL);
                thread_par_sum += (int)thread_par[j];
            }
            printf("waited for all competing threads (returned sum=%d)\n", thread_par_sum);
            if (errors += thread_par_sum) {
                printf("error: failed parallel instance provisioning test\n");
            }
            CHECK_BLOBS;
            warnings += cleanup_vms();
            CHECK_BLOBS;
        }
    }

out:
    printf("\nfinal check of work blobstore\n");
    check_blob(work_bs, "blocks", 0);
    printf("cleaning cache blobstore\n");
    blobstore_delete_regex(cache_bs, ".*");
    check_blob(cache_bs, "blocks", 0);

    printf("done with vbr.c errors=%d warnings=%d\n", errors, warnings);
    exit(errors);
}
#endif /* _UNIT_TEST */
