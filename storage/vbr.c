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
#include <string.h>             // strcasestr
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>           // waitpid
#include <stdarg.h>
#include <fcntl.h>
#include <errno.h>
#include <limits.h>
#include <assert.h>
#include <dirent.h>

#include "misc.h"               // logprintfl, ensure_...
#include "hash.h"
#include "data.h"
#include "vbr.h"
#include "walrus.h"
#include "blobstore.h"
#include "diskutil.h"
#include "iscsi.h"
#include "http.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define VBR_SIZE_SCALING                         1024   //!< @TODO remove this adjustment after CLC sends bytes instead of KBs

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
#define VBR_SIZE                                 ( 2LL * MEGABYTE ) / VBR_SIZE_SCALING
#define EKI_SIZE                                 ( 1024LL ) / VBR_SIZE_SCALING
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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static __thread char current_instanceId[512] = "";  //!< instance ID that is being serviced, for logging only

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
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int vbr_add_ascii(const char *spec_str, virtualMachine * vm_type);
int vbr_parse(virtualMachine * vm, ncMetadata * pMeta);
int vbr_legacy(const char *instanceId, virtualMachine * params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId,
               char *ramdiskURL);

int art_add_dep(artifact * a, artifact * dep);
void art_free(artifact * a);
void arts_free(artifact * array[], unsigned int array_len);

boolean tree_uses_blobstore(artifact * a);
boolean tree_uses_cache(artifact * a);

artifact *art_alloc(const char *id, const char *sig, long long size_bytes, boolean may_be_cached, boolean must_be_file, boolean must_be_hollow,
                    int (*creator) (artifact * a), virtualBootRecord * vbr);

void art_set_instanceId(const char *instanceId);
artifact *vbr_alloc_tree(virtualMachine * vm, boolean do_make_bootable, boolean do_make_work_copy, const char *sshkey, const char *instanceId);
int art_implement_tree(artifact * root, blobstore * work_bs, blobstore * cache_bs, const char *work_prefix, long long timeout_usec);

#ifdef _UNIT_TEST
int main(int argc, char **argv);
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
//! The following *_creator funcitons produce an artifact in blobstore,  either from scratch (such as Walrus
//! download or a new partition) or by converting, combining, and augmenting existing artifacts.
//!
//! When invoked, creators can assume that any input blobs and the output blob are open (and thus locked for
//! their exclusive use).
//!
//! Creators return OK or an error code: either generic one (ERROR) or a code specific to a failed blobstore
//! operation, which can be obtained using blobstore_get_error().
static int url_creator(artifact * a);
static int walrus_creator(artifact * a);
static int partition_creator(artifact * a);
static void set_disk_dev(virtualBootRecord * vbr);
static int disk_creator(artifact * a);
static int iqn_creator(artifact * a);
static int aoe_creator(artifact * a);
static int copy_creator(artifact * a);
//! @}

static void art_print_tree(const char *prefix, artifact * a);
static int art_gen_id(char *buf, unsigned int buf_size, const char *first, const char *sig);
static void convert_id(const char *src, char *dst, unsigned int size);
static char *url_get_digest(const char *url);
static artifact *art_alloc_vbr(virtualBootRecord * vbr, boolean do_make_work_copy, boolean must_be_file, const char *sshkey);
static artifact *art_alloc_disk(virtualBootRecord * vbr, artifact * prereqs[], int num_prereqs, artifact * parts[], int num_parts,
                                artifact * emi_disk, boolean do_make_bootable, boolean do_make_work_copy);
static int find_or_create_blob(int flags, blobstore * bs, const char *id, long long size_bytes, const char *sig, blockblob ** bbp);
static int find_or_create_artifact(int do_create, artifact * a, blobstore * work_bs, blobstore * cache_bs, const char *work_prefix, blockblob ** bbp);

#ifdef _UNIT_TEST
static blobstore *create_teststore(int size_blocks, const char *base, const char *name, blobstore_format_t format, blobstore_revocation_t revocation,
                                   blobstore_snapshot_t snapshot);
static void add_vbr(virtualMachine * vm, long long size, ncResourceFormatType format, char *formatName, const char *id, ncResourceType type,
                    ncResourceLocationType locationType, int diskNumber, int partitionNumber, libvirtBusType guestDeviceBus,
                    char *preparedResourceLocation);
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
    int i = 0;
    char *l = NULL;
    serviceInfoType *service = NULL;

    for (i = 0; i < pMeta->servicesLen; i++) {
        service = &(pMeta->services[i]);
        if (strncmp(service->type, typeName, strlen(typeName) - 3) == 0 && service->urisLen > 0) {
            l = vbr->resourceLocation + (strlen(typeName) + 3);   // +3 for "://", so 'l' points past, e.g., "walrus:"
            snprintf(vbr->preparedResourceLocation, sizeof(vbr->preparedResourceLocation), "%s/%s", service->uris[0], l);   //! @TODO for now we just pick the first one
            return (EUCA_OK);
        }
    }
    logprintfl(EUCAERROR, "failed to find service '%s' in eucalyptusMessage\n", typeName);
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
    char *spec_copy = NULL;
    char *type_spec = NULL;
    char *id_spec = NULL;
    char *size_spec = NULL;
    char *format_spec = NULL;
    char *dev_spec = NULL;
    char *loc_spec = NULL;
    virtualBootRecord *vbr = NULL;

    if (vm_type->virtualBootRecordLen == EUCA_MAX_VBRS) {
        logprintfl(EUCAERROR, "too many entries in VBR already\n");
        return (EUCA_ERROR);
    }

    vbr = &(vm_type->virtualBootRecord[vm_type->virtualBootRecordLen++]);

    spec_copy = strdup(spec_str);
    type_spec = strtok(spec_copy, ":");
    id_spec = strtok(NULL, ":");
    size_spec = strtok(NULL, ":");
    format_spec = strtok(NULL, ":");
    dev_spec = strtok(NULL, ":");
    loc_spec = strtok(NULL, ":");
    if (type_spec == NULL) {
        logprintfl(EUCAERROR, "error: invalid 'type' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    safe_strncpy(vbr->typeName, type_spec, sizeof(vbr->typeName));

    if (id_spec == NULL) {
        logprintfl(EUCAERROR, "error: invalid 'id' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    safe_strncpy(vbr->id, id_spec, sizeof(vbr->id));

    if (size_spec == NULL) {
        logprintfl(EUCAERROR, "error: invalid 'size' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    vbr->size = atoi(size_spec);

    if (format_spec == NULL) {
        logprintfl(EUCAERROR, "error: invalid 'format' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    safe_strncpy(vbr->formatName, format_spec, sizeof(vbr->formatName));

    if (dev_spec == NULL) {
        logprintfl(EUCAERROR, "error: invalid 'guestDeviceName' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    safe_strncpy(vbr->guestDeviceName, dev_spec, sizeof(vbr->guestDeviceName));

    if (loc_spec == NULL) {
        logprintfl(EUCAERROR, "error: invalid 'resourceLocation' specification in VBR '%s'\n", spec_str);
        goto out_error;
    }
    safe_strncpy(vbr->resourceLocation, spec_str + (loc_spec - spec_copy), sizeof(vbr->resourceLocation));

    EUCA_FREE(spec_copy);
    return (EUCA_OK);

out_error:
    vm_type->virtualBootRecordLen--;
    EUCA_FREE(spec_copy);
    return (EUCA_ERROR);
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
    int error = EUCA_OK;
    int letters_len = 0;
    char d = '\0';
    char n = '\0';
    char t = '\0';
    char buf[10] = { 0 };
    long long int p = 0;    // partition or floppy drive number

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
        if (strstr(vbr->typeName, "ephemeral0") == vbr->typeName) { //! @TODO remove
            if (vm) {
                vm->ephemeral0 = vbr;
            }
        }
    } else if (strstr(vbr->typeName, "swap") == vbr->typeName) {    //! @TODO remove
        vbr->type = NC_RESOURCE_SWAP;
        if (vm)
            vm->swap = vbr;
    } else if (strstr(vbr->typeName, "ebs") == vbr->typeName) {
        vbr->type = NC_RESOURCE_EBS;
    } else {
        logprintfl(EUCAERROR, "Error: failed to parse resource type '%s'\n", vbr->typeName);
        return (EUCA_ERROR);
    }

    // identify the type of resource location from location string
    if (strcasestr(vbr->resourceLocation, "http://") == vbr->resourceLocation
        || strcasestr(vbr->resourceLocation, "https://") == vbr->resourceLocation) {
        if (strcasestr(vbr->resourceLocation, "/services/Walrus/")) {
            vbr->locationType = NC_LOCATION_WALRUS;
        } else {
            vbr->locationType = NC_LOCATION_URL;
        }
        safe_strncpy(vbr->preparedResourceLocation, vbr->resourceLocation, sizeof(vbr->preparedResourceLocation));
    } else if (strcasestr(vbr->resourceLocation, "iqn://") == vbr->resourceLocation || strchr(vbr->resourceLocation, ',')) {    //! @TODO remove this transitionary iSCSI crutch?
        vbr->locationType = NC_LOCATION_IQN;
    } else if (strcasestr(vbr->resourceLocation, "aoe://") == vbr->resourceLocation || strcasestr(vbr->resourceLocation, "/dev/") == vbr->resourceLocation) {   //! @TODO remove this transitionary AoE crutch
        vbr->locationType = NC_LOCATION_AOE;
    } else if (strcasestr(vbr->resourceLocation, "walrus://") == vbr->resourceLocation) {
        vbr->locationType = NC_LOCATION_WALRUS;
        if (pMeta)
            error = prep_location(vbr, pMeta, "walrus");
    } else if (strcasestr(vbr->resourceLocation, "cloud://") == vbr->resourceLocation) {
        vbr->locationType = NC_LOCATION_CLC;
        if (pMeta)
            error = prep_location(vbr, pMeta, "cloud");
    } else if (strcasestr(vbr->resourceLocation, "sc://") == vbr->resourceLocation || strcasestr(vbr->resourceLocation, "storage://") == vbr->resourceLocation) {   //! @TODO is it 'sc' or 'storage'?
        vbr->locationType = NC_LOCATION_SC;
        if (pMeta)
            error = prep_location(vbr, pMeta, "sc");
    } else if (strcasestr(vbr->resourceLocation, "none") == vbr->resourceLocation) {
        if (vbr->type != NC_RESOURCE_EPHEMERAL && vbr->type != NC_RESOURCE_SWAP) {
            logprintfl(EUCAERROR, "Error: resourceLocation not specified for non-ephemeral resource '%s'\n", vbr->resourceLocation);
            return (EUCA_ERROR);
        }
        vbr->locationType = NC_LOCATION_NONE;
    } else {
        logprintfl(EUCAERROR, "Error: failed to parse resource location '%s'\n", vbr->resourceLocation);
        return (EUCA_ERROR);
    }

    if (error != EUCA_OK) {
        logprintfl(EUCAERROR, "Error: URL for resourceLocation '%s' is not in the message\n", vbr->resourceLocation);
        return (EUCA_ERROR);
    }
    // device can be 'none' only for kernel and ramdisk types
    if (!strcmp(vbr->guestDeviceName, "none")) {
        if (vbr->type != NC_RESOURCE_KERNEL && vbr->type != NC_RESOURCE_RAMDISK) {
            logprintfl(EUCAERROR, "Error: guestDeviceName not specified for resource '%s'\n", vbr->resourceLocation);
            return (EUCA_ERROR);
        }
    } else {                    // should be a valid device
        // trim off "/dev/" prefix, if present, and verify the rest
        if (strstr(vbr->guestDeviceName, "/dev/") == vbr->guestDeviceName) {
            logprintfl(EUCAWARN, "Warning: trimming off invalid prefix '/dev/' from guestDeviceName '%s'\n", vbr->guestDeviceName);
            safe_strncpy(buf, vbr->guestDeviceName + 5, sizeof(buf));
            safe_strncpy(vbr->guestDeviceName, buf, sizeof(vbr->guestDeviceName));
        }

        if (strlen(vbr->guestDeviceName) < 3 || (vbr->guestDeviceName[0] == 'x' && strlen(vbr->guestDeviceName) < 4)) {
            logprintfl(EUCAERROR, "Error: invalid guestDeviceName '%s'\n", vbr->guestDeviceName);
            return (EUCA_ERROR);
        }

        {
            t = vbr->guestDeviceName[0];   // type
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
                logprintfl(EUCAERROR, "Error: failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                return (EUCA_ERROR);
            }

            letters_len = 3;    // e.g. "sda"
            if (t == 'x')
                letters_len = 4;    // e.g., "xvda"
            if (t == 'f')
                letters_len = 2;    // e.g., "fd0"
            d = vbr->guestDeviceName[letters_len - 2]; // when 3+, the 'd'
            n = vbr->guestDeviceName[letters_len - 1]; // when 3+, the disk number
            if (strlen(vbr->guestDeviceName) > letters_len) {
                p = 0;    // partition or floppy drive number
                errno = 0;
                p = strtoll(vbr->guestDeviceName + letters_len, NULL, 10);
                if (errno != 0) {
                    logprintfl(EUCAERROR, "Error: failed to parse partition number in guestDeviceName '%s'\n", vbr->guestDeviceName);
                    return (EUCA_ERROR);
                }
                if (p < 0 || p > EUCA_MAX_PARTITIONS) {
                    logprintfl(EUCAERROR, "Error: unexpected partition or disk number '%d' in guestDeviceName '%s'\n", p, vbr->guestDeviceName);
                    return (EUCA_ERROR);
                }
                if (t == 'f') {
                    vbr->diskNumber = p;
                } else {
                    if (p < 1) {
                        logprintfl(EUCAERROR, "Error: unexpected partition number '%d' in guestDeviceName '%s'\n", p, vbr->guestDeviceName);
                        return (EUCA_ERROR);
                    }
                    vbr->partitionNumber = p;
                }
            } else {
                vbr->partitionNumber = 0;
            }

            if (vbr->guestDeviceType != DEV_TYPE_FLOPPY) {
                if (d != 'd') {
                    logprintfl(EUCAERROR, "Error: failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                    return (EUCA_ERROR);
                }
                assert(EUCA_MAX_DISKS >= 'z' - 'a');
                if (!(n >= 'a' && n <= 'z')) {
                    logprintfl(EUCAERROR, "Error: failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                    return (EUCA_ERROR);
                }
                vbr->diskNumber = n - 'a';
            }
        }
    }

    // parse ID
    if (strlen(vbr->id) < 4) {
        logprintfl(EUCAERROR, "Error: failed to parse VBR resource ID '%s' (use 'none' when no ID)\n", vbr->id);
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
        logprintfl(EUCAERROR, "Error: failed to parse resource format '%s'\n", vbr->formatName);
        return (EUCA_ERROR);
    }
    if (vbr->type == NC_RESOURCE_EPHEMERAL || vbr->type == NC_RESOURCE_SWAP) {  //! @TODO should we allow ephemeral/swap that reside remotely?
        if (vbr->size < 1) {
            logprintfl(EUCAERROR, "Error: invalid size '%d' for ephemeral resource '%s'\n", vbr->size, vbr->resourceLocation);
            return (EUCA_ERROR);
        }
    } else {
        //            if (vbr->size!=1 || vbr->format!=NC_FORMAT_NONE) { //! @TODO check for size!=-1
        if (vbr->format != NC_FORMAT_NONE) {
            logprintfl(EUCAERROR, "Error: invalid size '%d' or format '%s' for non-ephemeral resource '%s'\n", vbr->size, vbr->formatName,
                       vbr->resourceLocation);
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
    int i = 0;
    int j = 0;
    int k = 0;
    boolean has_partitions = FALSE;
    virtualBootRecord *vbr = NULL;
    virtualBootRecord *partitions[BUS_TYPES_TOTAL][EUCA_MAX_DISKS][EUCA_MAX_PARTITIONS];    // for validating partitions

    bzero(partitions, sizeof(partitions));  //! @fixme: chuck - this is not zeroing out the hole structure!!!
    for (i = 0; i < EUCA_MAX_VBRS && i < vm->virtualBootRecordLen; i++) {
        vbr = &(vm->virtualBootRecord[i]);

        if (strlen(vbr->typeName) == 0) {   // this must be the combined disk's VBR
            return (EUCA_OK);
        }

        if (parse_rec(vbr, vm, pMeta) != EUCA_OK)
            return (EUCA_ERROR);

        if (vbr->type != NC_RESOURCE_KERNEL && vbr->type != NC_RESOURCE_RAMDISK)
            partitions[vbr->guestDeviceBus][vbr->diskNumber][vbr->partitionNumber] = vbr;

        if (vm->root == NULL) { // we have not identified the EMI yet
            if (vbr->type == NC_RESOURCE_IMAGE) {
                vm->root = vbr;
            }
        } else {
            if (vm->root != vbr && vbr->type == NC_RESOURCE_IMAGE) {
                logprintfl(EUCAERROR, "Error: more than one EMI specified in the boot record\n");
                return (EUCA_ERROR);
            }
        }
    }

    // ensure that partitions are contiguous and that partitions and disks are not mixed
    for (i = 0; i < BUS_TYPES_TOTAL; i++) { // each bus type is treated separatedly
        for (j = 0; j < EUCA_MAX_DISKS; j++) {
            has_partitions = FALSE;
            for (k = EUCA_MAX_PARTITIONS - 1; k >= 0; k--) {    // count down
                if (partitions[i][j][k]) {
                    if (k == 0 && has_partitions) {
                        logprintfl(EUCAERROR, "Error: specifying both disk and a partition on the disk is not allowed\n");
                        return (EUCA_ERROR);
                    }
                    has_partitions = TRUE;
                } else {
                    if (k != 0 && has_partitions) {
                        logprintfl(EUCAERROR, "Error: gaps in partition table are not allowed\n");
                        return (EUCA_ERROR);
                    }
                }
                if (vm->root == NULL) { // root partition or disk have not been found yet (no NC_RESOURCE_IMAGE)
                    if (has_partitions)
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
        logprintfl(EUCAERROR, "Error: no root partition or disk have been found\n");
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
int vbr_legacy(const char *instanceId, virtualMachine * params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId,
               char *ramdiskURL)
{
    int i = 0;
    boolean found_image = FALSE;
    boolean found_kernel = FALSE;
    boolean found_ramdisk = FALSE;
    virtualBootRecord *vbr = NULL;

    for (i = 0; i < EUCA_MAX_VBRS && i < params->virtualBootRecordLen; i++) {
        vbr = &(params->virtualBootRecord[i]);
        if (strlen(vbr->resourceLocation) > 0) {
            logprintfl(EUCADEBUG, "[%s]                VBR[%d] type=%s id=%s dev=%s size=%lld format=%s %s\n",
                       instanceId, i, vbr->typeName, vbr->id, vbr->guestDeviceName, vbr->size, vbr->formatName, vbr->resourceLocation);
            if (!strcmp(vbr->typeName, "machine"))
                found_image = TRUE;
            if (!strcmp(vbr->typeName, "kernel"))
                found_kernel = TRUE;
            if (!strcmp(vbr->typeName, "ramdisk"))
                found_ramdisk = TRUE;
        } else {
            break;
        }
    }

    // legacy support for image{Id|URL}
    if (imageId && imageURL) {
        if (found_image) {
            logprintfl(EUCAWARN, "[%s] IGNORING image %s passed outside the virtual boot record\n", instanceId, imageId);
        } else {
            logprintfl(EUCAWARN, "[%s] LEGACY pre-VBR image id=%s URL=%s\n", instanceId, imageId, imageURL);
            if (i >= EUCA_MAX_VBRS - 2) {
                logprintfl(EUCAERROR, "[%s] error: out of room in the Virtual Boot Record for legacy image %s\n", instanceId, imageId);
                return (EUCA_ERROR);
            }

            {
                // create root partition VBR
                vbr = &(params->virtualBootRecord[i++]);
                safe_strncpy(vbr->resourceLocation, imageURL, sizeof(vbr->resourceLocation));
                safe_strncpy(vbr->guestDeviceName, "sda1", sizeof(vbr->guestDeviceName));
                safe_strncpy(vbr->id, imageId, sizeof(vbr->id));
                safe_strncpy(vbr->typeName, "machine", sizeof(vbr->typeName));
                vbr->size = -1;
                safe_strncpy(vbr->formatName, "none", sizeof(vbr->formatName));
                params->virtualBootRecordLen++;
            }
            {
                // create ephemeral partition VBR
                vbr = &(params->virtualBootRecord[i++]);
                safe_strncpy(vbr->resourceLocation, "none", sizeof(vbr->resourceLocation));
                safe_strncpy(vbr->guestDeviceName, "sda2", sizeof(vbr->guestDeviceName));
                safe_strncpy(vbr->id, "none", sizeof(vbr->id));
                safe_strncpy(vbr->typeName, "ephemeral0", sizeof(vbr->typeName));
                vbr->size = 524288; // we cannot compute it here, so pick something
                safe_strncpy(vbr->formatName, "ext2", sizeof(vbr->formatName));
                params->virtualBootRecordLen++;
            }
            {
                // create swap partition VBR
                vbr = &(params->virtualBootRecord[i++]);
                safe_strncpy(vbr->resourceLocation, "none", sizeof(vbr->resourceLocation));
                safe_strncpy(vbr->guestDeviceName, "sda3", sizeof(vbr->guestDeviceName));
                safe_strncpy(vbr->id, "none", sizeof(vbr->id));
                safe_strncpy(vbr->typeName, "swap", sizeof(vbr->typeName));
                vbr->size = 524288;
                safe_strncpy(vbr->formatName, "swap", sizeof(vbr->formatName));
                params->virtualBootRecordLen++;
            }
        }
    }
    // legacy support for kernel{Id|URL}
    if (kernelId && kernelURL) {
        if (found_kernel) {
            logprintfl(EUCAINFO, "[%s] IGNORING kernel %s passed outside the virtual boot record\n", instanceId, kernelId);
        } else {
            logprintfl(EUCAINFO, "[%s] LEGACY pre-VBR kernel id=%s URL=%s\n", instanceId, kernelId, kernelURL);
            if (i == EUCA_MAX_VBRS) {
                logprintfl(EUCAERROR, "[%s] error: out of room in the Virtual Boot Record for legacy kernel %s\n", instanceId, kernelId);
                return (EUCA_ERROR);
            }
            vbr = &(params->virtualBootRecord[i++]);
            safe_strncpy(vbr->resourceLocation, kernelURL, sizeof(vbr->resourceLocation));
            safe_strncpy(vbr->guestDeviceName, "none", sizeof(vbr->guestDeviceName));
            safe_strncpy(vbr->id, kernelId, sizeof(vbr->id));
            safe_strncpy(vbr->typeName, "kernel", sizeof(vbr->typeName));
            vbr->size = -1;
            safe_strncpy(vbr->formatName, "none", sizeof(vbr->formatName));
            params->virtualBootRecordLen++;
        }
    }
    // legacy support for ramdisk{Id|URL}
    if (ramdiskId && ramdiskURL) {
        if (found_ramdisk) {
            logprintfl(EUCAINFO, "[%s] IGNORING ramdisk %s passed outside the virtual boot record\n", instanceId, ramdiskId);
        } else {
            logprintfl(EUCAINFO, "[%s] LEGACY pre-VBR ramdisk id=%s URL=%s\n", instanceId, ramdiskId, ramdiskURL);
            if (i == EUCA_MAX_VBRS) {
                logprintfl(EUCAERROR, "[%s] error: out of room in the Virtual Boot Record for legacy ramdisk %s\n", instanceId, ramdiskId);
                return (EUCA_ERROR);
            }
            vbr = &(params->virtualBootRecord[i++]);
            safe_strncpy(vbr->resourceLocation, ramdiskURL, sizeof(vbr->resourceLocation));
            safe_strncpy(vbr->guestDeviceName, "none", sizeof(vbr->guestDeviceName));
            safe_strncpy(vbr->id, ramdiskId, sizeof(vbr->id));
            safe_strncpy(vbr->typeName, "ramdisk", sizeof(vbr->typeName));
            vbr->size = -1;
            safe_strncpy(vbr->formatName, "none", sizeof(vbr->formatName));
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
    virtualBootRecord *vbr = NULL;

    assert(a);
    if (a->vbr != NULL) {
        vbr = a->vbr;

        assert(a->bb);
        if (!a->must_be_file && strlen(blockblob_get_dev(a->bb)) && (blobstore_snapshot_t) a->bb->store->snapshot_policy != BLOBSTORE_SNAPSHOT_NONE) {  // without snapshots we can use files
            safe_strncpy(vbr->backingPath, blockblob_get_dev(a->bb), sizeof(vbr->backingPath));
            vbr->backingType = SOURCE_TYPE_BLOCK;
        } else {
            assert(blockblob_get_file(a->bb));
            safe_strncpy(vbr->backingPath, blockblob_get_file(a->bb), sizeof(vbr->backingPath));
            vbr->backingType = SOURCE_TYPE_FILE;
        }
        vbr->size = a->bb->size_bytes;
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
    const char *dest_path = NULL;
    virtualBootRecord *vbr = NULL;

    assert(a->bb);
    assert(a->vbr);

    vbr = a->vbr;
    dest_path = blockblob_get_file(a->bb);

    assert(vbr->preparedResourceLocation);
    logprintfl(EUCAINFO, "[%s] downloading %s\n", a->instanceId, vbr->preparedResourceLocation);
    if (http_get(vbr->preparedResourceLocation, dest_path) != EUCA_OK) {
        logprintfl(EUCAERROR, "[%s] error: failed to download component %s\n", a->instanceId, vbr->preparedResourceLocation);
        return (EUCA_ERROR);
    }

    return (EUCA_OK);
}

//!
//! Creates an artifact by downloading it from Walrus
//!
//! @param[in] a
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int walrus_creator(artifact * a)
{
    const char *dest_path = NULL;
    virtualBootRecord *vbr = NULL;

    assert(a->bb);
    assert(a->vbr);

    vbr = a->vbr;
    dest_path = blockblob_get_file(a->bb);

    assert(vbr->preparedResourceLocation);
    logprintfl(EUCAINFO, "[%s] downloading %s\n", a->instanceId, vbr->preparedResourceLocation);
    if (walrus_image_by_manifest_url(vbr->preparedResourceLocation, dest_path, TRUE) != EUCA_OK) {
        logprintfl(EUCAERROR, "[%s] error: failed to download component %s\n", a->instanceId, vbr->preparedResourceLocation);
        return (EUCA_ERROR);
    }

    return (EUCA_OK);
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
    int format = EUCA_ERROR;
    const char *dest_dev = NULL;
    virtualBootRecord *vbr = NULL;

    assert(a->bb);
    assert(a->vbr);

    vbr = a->vbr;
    dest_dev = blockblob_get_dev(a->bb);

    assert(dest_dev);
    logprintfl(EUCAINFO, "[%s] creating partition of size %lld bytes and type %s in %s\n", a->instanceId, a->size_bytes, vbr->formatName, a->id);
    switch (vbr->format) {
    case NC_FORMAT_NONE:
        format = EUCA_OK;
        break;
    case NC_FORMAT_EXT2:       //! @TODO distinguish ext2 and ext3!
    case NC_FORMAT_EXT3:
        format = diskutil_mkfs(dest_dev, a->size_bytes);
        break;
    case NC_FORMAT_SWAP:
        format = diskutil_mkswap(dest_dev, a->size_bytes);
        break;
    default:
        logprintfl(EUCAERROR, "[%s] error: format of type %d/%s is NOT IMPLEMENTED\n", a->instanceId, vbr->format, vbr->formatName);
        break;
    }

    if (format != EUCA_OK) {
        logprintfl(EUCAERROR, "[%s] failed to create partition in blob %s\n", a->instanceId, a->id);
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
    char disk = '\0';
    char type[3] = "\0\0\0";
    char part[3] = "\0\0\0";

    if (vbr->guestDeviceType == DEV_TYPE_FLOPPY) {
        type[0] = 'f';
    } else {                    // a disk 
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
            type[0] = '?';      // error
            break;
        }
    }

    if (vbr->guestDeviceType == DEV_TYPE_FLOPPY) {
        assert(vbr->diskNumber >= 0 && vbr->diskNumber <= 9);
        disk = '0' + vbr->diskNumber;
    } else {                    // a disk 
        assert(vbr->diskNumber >= 0 && vbr->diskNumber <= 26);
        disk = 'a' + vbr->diskNumber;
    }

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
    int i = 0;
    int ret = EUCA_ERROR;
    int map_entries = 1;
    int root_entry = -1;
    int root_part = -1;
    char *mapper_dev = NULL;
    char dev_with_p[EUCA_MAX_PATH] = { 0 };
    char dev_without_p[EUCA_MAX_PATH] = { 0 };  // on Ubuntu Precise, some dev names do not have 'p' in them
    char loop_dev[EUCA_MAX_PATH] = { 0 };
    char mnt_pt[EUCA_MAX_PATH] = "/tmp/euca-mount-XXXXXX";
    const char *kernel_path = NULL;
    const char *ramdisk_path = NULL;
    long long offset_bytes = 0;
    const char *dest_dev = NULL;
    boolean bootification_failed = TRUE;
    artifact *dep = NULL;
    blockmap_relation_t mbr_op = BLOBSTORE_SNAPSHOT;
    blockmap_relation_t part_op = BLOBSTORE_MAP;    // use map by default as it is faster
    blockmap map[EUCA_MAX_PARTITIONS] = { { 0 } };      // initially only MBR is in the map
    virtualBootRecord *p = NULL;
    virtualBootRecord *p1 = NULL;
    virtualBootRecord *disk = NULL;

    assert(a->bb);
    dest_dev = blockblob_get_dev(a->bb);

    assert(dest_dev);
    logprintfl(EUCAINFO, "[%s] constructing disk of size %lld bytes in %s (%s)\n", a->instanceId, a->size_bytes, a->id, blockblob_get_dev(a->bb));

    if ((blobstore_snapshot_t) a->bb->store->snapshot_policy == BLOBSTORE_SNAPSHOT_NONE) {
        // but fall back to copy when snapshots are not possible or desired
        mbr_op = BLOBSTORE_COPY;
        part_op = BLOBSTORE_COPY;
    }

    map[0].relation_type = mbr_op;
    map[0].source_type = BLOBSTORE_ZERO;
    map[0].source.blob = NULL;
    map[0].first_block_src = 0;
    map[0].first_block_dst = 0;
    map[0].len_blocks = MBR_BLOCKS;

    // run through partitions, add their sizes, populate the map
    disk = a->vbr;
    map_entries = 1;        // first map entry is for the MBR
    root_entry = -1;        // we do not know the root entry
    root_part = -1;         // we do not know the root partition
    kernel_path = NULL;
    ramdisk_path = NULL;
    offset_bytes = 512 * MBR_BLOCKS;  // first partition begins after MBR
    assert(disk);

    for (i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        dep = a->deps[i];
        if (!dep->is_partition) {
            if (dep->vbr && dep->vbr->type == NC_RESOURCE_KERNEL)
                kernel_path = blockblob_get_file(dep->bb);
            if (dep->vbr && dep->vbr->type == NC_RESOURCE_RAMDISK)
                ramdisk_path = blockblob_get_file(dep->bb);
            continue;
        }

        p = dep->vbr;
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
        logprintfl(EUCADEBUG, "[%s] mapping partition %d from %s [%lld-%lld]\n",
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

    // set fields in vbr that are needed for
    // xml.c:gen_instance_xml() to generate correct disk entries
    disk->guestDeviceType = p1->guestDeviceType;
    disk->guestDeviceBus = p1->guestDeviceBus;
    disk->diskNumber = p1->diskNumber;
    set_disk_dev(disk);

    // map the partitions to the disk
    if (blockblob_clone(a->bb, map, map_entries) == -1) {
        ret = blobstore_get_error();
        logprintfl(EUCAERROR, "[%s] failed to clone partitions to created disk: %d %s\n", a->instanceId, ret, blobstore_get_last_msg());
        goto cleanup;
    }
    // create MBR
    logprintfl(EUCAINFO, "[%s] creating MBR\n", a->instanceId);
    if (diskutil_mbr(blockblob_get_dev(a->bb), "msdos") != EUCA_OK) {   // issues `parted mklabel`
        logprintfl(EUCAERROR, "[%s] failed to add MBR to disk: %d %s\n", a->instanceId, blobstore_get_error(), blobstore_get_last_msg());
        goto cleanup;
    }
    // add partition information to MBR
    for (i = 1; i < map_entries; i++) { // map [0] is for the MBR
        logprintfl(EUCAINFO, "[%s] adding partition %d to partition table (%s)\n", a->instanceId, i, blockblob_get_dev(a->bb));
        if (diskutil_part(blockblob_get_dev(a->bb), // issues `parted mkpart`
                          "primary",    //! @TODO make this work with more than 4 partitions
                          NULL, // do not create file system
                          map[i].first_block_dst,   // first sector
                          map[i].first_block_dst + map[i].len_blocks - 1) != EUCA_OK) {
            logprintfl(EUCAERROR, "[%s] error: failed to add partition %d to disk: %d %s\n", a->instanceId, i, blobstore_get_error(),
                       blobstore_get_last_msg());
            goto cleanup;
        }
    }

    //  make disk bootable if necessary
    if (a->do_make_bootable) {
        bootification_failed = TRUE;

        logprintfl(EUCAINFO, "[%s] making disk bootable\n", a->instanceId);
        if (root_entry < 1 || root_part < 0) {
            logprintfl(EUCAERROR, "[%s] error: cannot make bootable a disk without an image\n", a->instanceId);
            goto cleanup;
        }
        if (kernel_path == NULL) {
            logprintfl(EUCAERROR, "[%s] error: no kernel found among the VBRs\n", a->instanceId);
            goto cleanup;
        }
        if (ramdisk_path == NULL) {
            logprintfl(EUCAERROR, "[%s] error: no ramdisk found among the VBRs\n", a->instanceId);
            goto cleanup;
        }
        // `parted mkpart` causes children devices for each partition to be created
        // (e.g., /dev/mapper/euca-diskX gets /dev/mapper/euca-diskXp1 or ...X1 and so on)
        // we mount such a device here so as to copy files to the root partition
        // (we cannot mount the dev of the partition's blob because it becomes
        // 'busy' after the clone operation)
        mapper_dev = NULL;
        snprintf(dev_with_p, sizeof(dev_with_p), "%sp%d", blockblob_get_dev(a->bb), root_entry);
        snprintf(dev_without_p, sizeof(dev_without_p), "%s%d", blockblob_get_dev(a->bb), root_entry);
        if (check_path(dev_with_p) == 0) {
            mapper_dev = dev_with_p;
        } else if (check_path(dev_without_p) == 0) {
            mapper_dev = dev_without_p;
        } else {
            logprintfl(EUCAERROR, "[%s] failed to stat partition device [%s]\n", a->instanceId, mapper_dev, strerror(errno));
            goto cleanup;
        }
        logprintfl(EUCAINFO, "[%s] found partition device %s\n", a->instanceId, mapper_dev);

        // point a loopback device at the partition device because grub-probe on Ubuntu Precise 
        // sometimes does not grok root partitions mounted from /dev/mapper/... 
        if (diskutil_loop(mapper_dev, 0, loop_dev, sizeof(loop_dev)) != EUCA_OK) {
            logprintfl(EUCAINFO, "[%s] error: failed to attach '%s' on a loopback device\n", a->instanceId, mapper_dev);
            goto cleanup;
        }
        assert(strncmp(loop_dev, "/dev/loop", 9) == 0);

        // mount the root partition
        if (safe_mkdtemp(mnt_pt) == NULL) {
            logprintfl(EUCAINFO, "[%s] error: mkdtemp() failed: %s\n", a->instanceId, strerror(errno));
            goto unloop;
        }
        if (diskutil_mount(loop_dev, mnt_pt) != EUCA_OK) {
            logprintfl(EUCAINFO, "[%s] error: failed to mount '%s' on '%s'\n", a->instanceId, loop_dev, mnt_pt);
            goto unloop;
        }
        // copy in kernel and ramdisk and run grub over the root partition and the MBR
        logprintfl(EUCAINFO, "[%s] making partition %d bootable\n", a->instanceId, root_part);
        logprintfl(EUCAINFO, "[%s] with kernel %s\n", a->instanceId, kernel_path);
        logprintfl(EUCAINFO, "[%s] and ramdisk %s\n", a->instanceId, ramdisk_path);
        if (diskutil_grub(blockblob_get_dev(a->bb), mnt_pt, root_part, kernel_path, ramdisk_path) != EUCA_OK) {
            logprintfl(EUCAERROR, "[%s] error: failed to make disk bootable\n", a->instanceId, root_part);
            goto unmount;
        }
        // change user of the blob device back to 'eucalyptus' (grub sets it to 'root')
        sleep(1);               // without this, perms on dev-mapper devices can flip back, presumably because in-kernel ops complete after grub process finishes
        if (diskutil_ch(blockblob_get_dev(a->bb), EUCALYPTUS_ADMIN, NULL, 0) != EUCA_OK) {
            logprintfl(EUCAINFO, "[%s] error: failed to change user for '%s' to '%s'\n", a->instanceId, blockblob_get_dev(a->bb), EUCALYPTUS_ADMIN);
        }
        bootification_failed = FALSE;

unmount:

        // unmount partition and delete the mount point
        if (diskutil_umount(mnt_pt) != EUCA_OK) {
            logprintfl(EUCAINFO, "[%s] error: failed to unmount %s (there may be a resource leak)\n", a->instanceId, mnt_pt);
            bootification_failed = TRUE;
        }
        if (rmdir(mnt_pt) != 0) {
            logprintfl(EUCAINFO, "[%s] error: failed to remove %s (there may be a resource leak): %s\n", a->instanceId, mnt_pt, strerror(errno));
            bootification_failed = TRUE;
        }

unloop:
        if (diskutil_unloop(loop_dev) != EUCA_OK) {
            logprintfl(EUCAINFO, "[%s] error: failed to remove %s (there may be a resource leak): %s\n", a->instanceId, loop_dev, strerror(errno));
            bootification_failed = TRUE;
        }
        if (bootification_failed)
            goto cleanup;
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
static int iqn_creator(artifact * a)
{
    char *dev = NULL;
    virtualBootRecord *vbr = NULL;

    assert(a);
    vbr = a->vbr;
    assert(vbr);

    dev = connect_iscsi_target(vbr->resourceLocation);
    if (!dev || !strstr(dev, "/dev")) {
        logprintfl(EUCAERROR, "[%s] error: failed to connect to iSCSI target\n", a->instanceId);
        return (EUCA_ERROR);
    }

    // update VBR with device location
    safe_strncpy(vbr->backingPath, dev, sizeof(vbr->backingPath));
    vbr->backingType = SOURCE_TYPE_BLOCK;

    return (EUCA_OK);
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
static int aoe_creator(artifact * a)
{
    char *dev = NULL;
    virtualBootRecord *vbr = NULL;

    assert(a);
    vbr = a->vbr;
    assert(vbr);

    dev = vbr->resourceLocation;
    if (!dev || !strstr(dev, "/dev") || check_block(dev) != 0) {
        logprintfl(EUCAERROR, "[%s] error: failed to locate AoE device %s\n", a->instanceId, dev);
        return (EUCA_ERROR);
    }
    // update VBR with device location
    safe_strncpy(vbr->backingPath, dev, sizeof(vbr->backingPath));
    vbr->backingType = SOURCE_TYPE_BLOCK;

    return (EUCA_OK);
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
static int copy_creator(artifact * a)
{
    artifact *dep = NULL;
    virtualBootRecord *vbr = NULL;
    blockmap_relation_t op = BLOBSTORE_SNAPSHOT;    // use snapshot by default as it is faster
    blockmap map[] = { { 0 } };
    boolean injection_failed = TRUE;
    const char *dev = NULL;
    const char *bbfile = NULL;
    char path[EUCA_MAX_PATH] = { 0 };
    char mnt_pt[EUCA_MAX_PATH] = "/tmp/euca-mount-XXXXXX";

    assert(a->deps[0]);
    assert(a->deps[1] == NULL);
    dep = a->deps[0];
    vbr = a->vbr;
    assert(vbr);

    if (dep->bb != NULL) {      // skip copy if source is NULL (as in the case of a bypassed redundant work artifact due to caching failure)
        logprintfl(EUCAINFO, "[%s] copying/cloning blob %s to blob %s\n", a->instanceId, dep->bb->id, a->bb->id);
        if (a->must_be_file) {
            if (blockblob_copy(dep->bb, 0L, a->bb, 0L, 0L) == -1) {
                logprintfl(EUCAERROR, "[%s] error: failed to copy blob %s to blob %s: %d %s\n", a->instanceId, dep->bb->id, a->bb->id,
                           blobstore_get_error(), blobstore_get_last_msg());
                return (blobstore_get_error());
            }
        } else {
            if ((blobstore_snapshot_t) a->bb->store->snapshot_policy == BLOBSTORE_SNAPSHOT_NONE) {
                op = BLOBSTORE_COPY;    // but fall back to copy when snapshots are not possible or desired
            }

            map[0].relation_type = op;
            map[0].source_type = BLOBSTORE_BLOCKBLOB;
            map[0].source.blob = dep->bb;
            map[0].first_block_src = 0;
            map[0].first_block_dst = 0;
            map[0].len_blocks = round_up_sec(dep->size_bytes) / 512;

            if (blockblob_clone(a->bb, map, 1) == -1) {
                logprintfl(EUCAERROR, "[%s] error: failed to clone/copy blob %s to blob %s: %d %s\n", a->instanceId, dep->bb->id, a->bb->id,
                           blobstore_get_error(), blobstore_get_last_msg());
                return (blobstore_get_error());
            }
        }
    }

    dev = blockblob_get_dev(a->bb);
    bbfile = blockblob_get_file(a->bb);

    if (a->do_tune_fs) {
        // tune file system, which is needed to boot EMIs fscked long ago
        logprintfl(EUCAINFO, "[%s] tuning root file system on disk %d partition %d\n", a->instanceId, vbr->diskNumber, vbr->partitionNumber);
        if (diskutil_tune(dev) != EUCA_OK) {
            logprintfl(EUCAWARN, "[%s] error: failed to tune root file system: %s\n", a->instanceId, blobstore_get_last_msg());
        }
    }

    if (!strcmp(vbr->typeName, "kernel") || !strcmp(vbr->typeName, "ramdisk")) {
        // for libvirt/kvm, kernel and ramdisk must be readable by libvirt
        if (diskutil_ch(bbfile, NULL, NULL, 0664) != EUCA_OK) {
            logprintfl(EUCAINFO, "[%s] error: failed to change user and/or permissions for '%s' '%s'\n", a->instanceId, vbr->typeName, bbfile);
        }
    }

    if (strlen(a->sshkey)) {
        injection_failed = TRUE;
        logprintfl(EUCAINFO, "[%s] injecting the ssh key\n", a->instanceId);

        // mount the partition
        if (safe_mkdtemp(mnt_pt) == NULL) {
            logprintfl(EUCAINFO, "[%s] error: mkdtemp() failed: %s\n", a->instanceId, strerror(errno));
            goto error;
        }
        if (diskutil_mount(dev, mnt_pt) != EUCA_OK) {
            logprintfl(EUCAINFO, "[%s] error: failed to mount '%s' on '%s'\n", a->instanceId, dev, mnt_pt);
            goto error;
        }
        // save the SSH key, with the right permissions
        snprintf(path, sizeof(path), "%s/root/.ssh", mnt_pt);
        if (diskutil_mkdir(path) == -1) {
            logprintfl(EUCAINFO, "[%s] error: failed to create path '%s'\n", a->instanceId, path);
            goto unmount;
        }
        if (diskutil_ch(path, "root", NULL, 0700) != EUCA_OK) {
            logprintfl(EUCAINFO, "[%s] error: failed to change user and/or permissions for '%s'\n", a->instanceId, path);
            goto unmount;
        }
        snprintf(path, sizeof(path), "%s/root/.ssh/authorized_keys", mnt_pt);
        if (diskutil_write2file(path, a->sshkey) != EUCA_OK) {  //! @TODO maybe append the key instead of overwriting?
            logprintfl(EUCAINFO, "[%s] error: failed to save key in '%s'\n", a->instanceId, path);
            goto unmount;
        }
        if (diskutil_ch(path, "root", NULL, 0600) != EUCA_OK) {
            logprintfl(EUCAINFO, "[%s] error: failed to change user and/or permissions for '%s'\n", a->instanceId, path);
            goto unmount;
        }
        // change user of the blob device back to 'eucalyptus' (tune and maybe other commands above set it to 'root')
        if (diskutil_ch(dev, EUCALYPTUS_ADMIN, NULL, 0) != EUCA_OK) {
            logprintfl(EUCAINFO, "[%s] error: failed to change user for '%s' to '%s'\n", a->instanceId, dev, EUCALYPTUS_ADMIN);
        }
        injection_failed = FALSE;

unmount:

        // unmount partition and delete the mount point
        if (diskutil_umount(mnt_pt) != EUCA_OK) {
            logprintfl(EUCAINFO, "[%s] error: failed to unmount %s (there may be a resource leak)\n", a->instanceId, mnt_pt);
            injection_failed = 1;
        }
        if (rmdir(mnt_pt) != 0) {
            logprintfl(EUCAINFO, "[%s] error: failed to remove %s (there may be a resource leak): %s\n", a->instanceId, mnt_pt, strerror(errno));
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
    int i = 0;
    if (dep == NULL)
        return (EUCA_OK);

    for (i = 0; i < MAX_ARTIFACT_DEPS; i++) {
        if (a->deps[i] == NULL) {
            logprintfl(EUCADEBUG, "[%s] added to artifact %03d|%s artifact %03d|%s\n", a->instanceId, a->seq, a->id, dep->seq, dep->id);
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
    int i = 0;

    if (a) {
        if (a->refs > 0) {
            // this free reduces reference count, if positive, by 1
            a->refs--;
        }
        // if this is the last reference
        if (a->refs == 0) {
            // try freeing dependents recursively
            for (i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
                ART_FREE(a->deps[i]);
            }
            logprintfl(EUCATRACE, "[%s] freeing artifact %03d|%s size=%lld vbr=%u cache=%d file=%d\n", a->instanceId, a->seq, a->id, a->size_bytes,
                       a->vbr, a->may_be_cached, a->must_be_file);
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
    int i = 0;
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
    int i = 0;
    char new_prefix[512] = { 0 };

    logprintfl(EUCADEBUG, "[%s] artifacts tree: %s%03d|%s cache=%d file=%d creator=%0x vbr=%0x\n", a->instanceId, prefix, a->seq, a->id,
               a->may_be_cached, a->must_be_file, a->creator, a->vbr);

    snprintf(new_prefix, sizeof(new_prefix), "%s\t", prefix);
    for (i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
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
    int i = 0;

    if (!a->id_is_path)
        return (TRUE);

    for (i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        if (tree_uses_blobstore(a->deps[i]))
            return (TRUE);
    }
    return (FALSE);
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
    int i = 0;

    if (a->may_be_cached)
        return (TRUE);

    for (i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        if (tree_uses_cache(a->deps[i]))
            return (TRUE);
    }
    return (FALSE);
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
    char hash[48] = { 0 };

    if (hexjenkins(hash, sizeof(hash), sig) != EUCA_OK)
        return (EUCA_ERROR);

    if (snprintf(buf, buf_size, "%s-%s", first, hash) >= buf_size)  // truncation
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
    artifact *a = NULL;
    static int seq = 0;

    if ((a = EUCA_ZALLOC(1, sizeof(artifact))) == NULL)
        return (NULL);

    a->seq = ++seq;             // not thread safe, but seq's are just for debugging
    safe_strncpy(a->instanceId, current_instanceId, sizeof(a->instanceId)); // for logging
    logprintfl(EUCADEBUG, "[%s] allocated artifact %03d|%s size=%lld vbr=%u cache=%d file=%d\n", a->instanceId, seq, id, size_bytes, vbr,
               may_be_cached, must_be_file);

    if (id)
        safe_strncpy(a->id, id, sizeof(a->id));

    if (sig)
        safe_strncpy(a->sig, sig, sizeof(a->sig));

    a->size_bytes = size_bytes;
    a->may_be_cached = may_be_cached;
    a->must_be_file = must_be_file;
    a->must_be_hollow = must_be_hollow;
    a->creator = creator;
    a->vbr = vbr;
    a->do_tune_fs = FALSE;

    if (vbr && (vbr->type == NC_RESOURCE_IMAGE && vbr->partitionNumber > 0))    //this is hacky
        a->do_tune_fs = TRUE;

    return (a);
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
    char *d = NULL;
    const char *s = NULL;

    if (strcasestr(src, "emi-") == src) {
        s = src + 4;    // position aftter 'emi'
        d = dst + strlen(dst);    // position after 'dsk' or 'emi' or whatever
        *d++ = '-';
        while ((*s >= '0') && (*s <= 'z')   // copy letters and numbers up to a hyphen
               && (d - dst < size)) {   // don't overrun dst
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
    int tmp_fd = -1;
    char *digest_str = NULL;
    char *digest_path = strdup("/tmp/url-digest-XXXXXX");

    if (!digest_path) {
        logprintfl(EUCAERROR, "{%u} error: failed to strdup digest path\n", (unsigned int)pthread_self());
        return (digest_path);
    }

    if ((tmp_fd = safe_mkstemp(digest_path)) < 0) {
        logprintfl(EUCAERROR, "{%u} error: failed to create a digest file %s\n", (unsigned int)pthread_self(), digest_path);
    } else {
        close(tmp_fd);

        // download a fresh digest
        if (http_get_timeout(url, digest_path, 10, 4, 0, 0) != 0) {
            logprintfl(EUCAERROR, "{%u} error: failed to download digest to %s\n", (unsigned int)pthread_self(), digest_path);
        } else {
            digest_str = file2strn(digest_path, 100000);
        }
        unlink(digest_path);
    }
    EUCA_FREE(digest_path);
    return (digest_str);
}

//!
//!
//!
//! @param[in] vbr
//! @param[in] do_make_work_copy
//! @param[in] must_be_file
//! @param[in] sshkey
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static artifact *art_alloc_vbr(virtualBootRecord * vbr, boolean do_make_work_copy, boolean must_be_file, const char *sshkey)
{
    artifact *a = NULL;
    artifact *a2 = NULL;
    char art_id[48] = { 0 };
    char art_pref2[EUCA_MAX_PATH] = "emi";
    char *blob_sig = NULL;
    char *art_pref = NULL;
    char buf[32] = { 0 };       // first part of artifact ID
    char manifestURL[MAX_PATH] = { 0 };
    char art_sig[ART_SIG_MAX] = { 0 };  // signature for this artifact based on its salient characteristics
    char key_sig[ART_SIG_MAX] = { 0 };
    long long bb_size_bytes = 0L;

    switch (vbr->locationType) {
    case NC_LOCATION_CLC:
    case NC_LOCATION_SC:
        logprintfl(EUCAERROR, "[%s] error: location of type %d is NOT IMPLEMENTED\n", current_instanceId, vbr->locationType);
        return NULL;

    case NC_LOCATION_URL:{
            // get the digest for size and signature
            snprintf(manifestURL, MAX_PATH, "%s.manifest.xml", vbr->preparedResourceLocation);
            blob_sig = url_get_digest(manifestURL);
            if (blob_sig == NULL)
                goto u_out;

            // extract size from the digest
            bb_size_bytes = str2longlong(blob_sig, "<size>", "</size>");  // pull size from the digest
            if (bb_size_bytes < 1)
                goto u_out;
            vbr->size = bb_size_bytes;  // record size in VBR now that we know it

            // generate ID of the artifact (append -##### hash of sig)
            if (art_gen_id(art_id, sizeof(art_id), vbr->id, blob_sig) != EUCA_OK)
                goto w_out;

            // allocate artifact struct
            a = art_alloc(art_id, blob_sig, bb_size_bytes, TRUE, must_be_file, FALSE, url_creator, vbr);

u_out:
            EUCA_FREE(blob_sig);
            break;
        }
    case NC_LOCATION_WALRUS:{
            // get the digest for size and signature
            if ((blob_sig = walrus_get_digest(vbr->preparedResourceLocation)) == NULL) {
                logprintfl(EUCAERROR, "[%s] error: failed to obtain image digest from  Walrus\n", current_instanceId);
                goto w_out;
            }
            // extract size from the digest
            bb_size_bytes = str2longlong(blob_sig, "<size>", "</size>");  // pull size from the digest
            if (bb_size_bytes < 1) {
                logprintfl(EUCAERROR, "[%s] error: incorrect image digest or error returned from Walrus\n", current_instanceId);
                goto w_out;
            }
            vbr->size = bb_size_bytes;  // record size in VBR now that we know it

            // generate ID of the artifact (append -##### hash of sig)
            if (art_gen_id(art_id, sizeof(art_id), vbr->id, blob_sig) != EUCA_OK) {
                logprintfl(EUCAERROR, "[%s] error: failed to generate artifact id\n", current_instanceId);
                goto w_out;
            }
            // allocate artifact struct
            a = art_alloc(art_id, blob_sig, bb_size_bytes, TRUE, must_be_file, FALSE, walrus_creator, vbr);

w_out:
            EUCA_FREE(blob_sig);
            break;
        }

    case NC_LOCATION_IQN:{
            a = art_alloc("iscsi-vol", NULL, -1, FALSE, FALSE, FALSE, iqn_creator, vbr);
            goto out;
        }

    case NC_LOCATION_AOE:{
            a = art_alloc("aoe-vol", NULL, -1, FALSE, FALSE, FALSE, aoe_creator, vbr);
            goto out;
        }

    case NC_LOCATION_NONE:{
            assert(vbr->size > 0L);

            vbr->size = vbr->size * VBR_SIZE_SCALING;   //! @TODO remove this adjustment (CLC sends size in KBs)

            if (snprintf(art_sig, sizeof(art_sig), "id=%s size=%lld format=%s\n\n", vbr->id, vbr->size, vbr->formatName) >= sizeof(art_sig))    // output was truncated
                break;

            if (strcmp(vbr->id, "none") == 0) {
                if (snprintf(buf, sizeof(buf), "prt-%05lld%s", vbr->size / 1048576, vbr->formatName) >= sizeof(buf))    // output was truncated
                    break;
                art_pref = buf;
            } else {
                art_pref = vbr->id;
            }

            if (art_gen_id(art_id, sizeof(art_id), art_pref, art_sig) != EUCA_OK)
                break;

            a = art_alloc(art_id, art_sig, vbr->size, TRUE, must_be_file, FALSE, partition_creator, vbr);
            break;
        }
    default:
        logprintfl(EUCAERROR, "[%s] error: unrecognized locationType %d\n", current_instanceId, vbr->locationType);
        break;
    }

    // allocate another artifact struct if a work copy is requested
    // or if an SSH key is supplied
    if (a && (do_make_work_copy || sshkey)) {

        safe_strncpy(art_id, a->id, sizeof(art_id));
        safe_strncpy(art_sig, a->sig, sizeof(art_sig));

        if (sshkey) {           // if SSH key is included, recalculate sig and ID
            if (strlen(sshkey) > sizeof(a->sshkey)) {
                logprintfl(EUCAERROR, "[%s] error: received SSH key is too long\n", a->instanceId);
                goto free;
            }

            if ((snprintf(key_sig, sizeof(key_sig), "KEY /root/.ssh/authorized_keys\n%s\n\n", sshkey) >= sizeof(key_sig))   // output truncated
                || ((strlen(art_sig) + strlen(key_sig)) >= sizeof(art_sig))) {  // overflow
                logprintfl(EUCAERROR, "[%s] error: internal buffers (ART_SIG_MAX) too small for signature\n", a->instanceId);
                goto free;
            }
            strncat(art_sig, key_sig, sizeof(art_sig) - strlen(key_sig) - 1);

            convert_id(a->id, art_pref2, sizeof(art_pref2));
            if (art_gen_id(art_id, sizeof(art_id), art_pref2, key_sig) != EUCA_OK) {
                goto free;
            }
        }

        a2 = art_alloc(art_id, art_sig, a->size_bytes, !do_make_work_copy, must_be_file, FALSE, copy_creator, vbr);
        if (a2) {
            if (sshkey)
                safe_strncpy(a2->sshkey, sshkey, sizeof(a2->sshkey));

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
    return (a);
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
//!
//! @return A pointer to 'keyed' disk artifact or NULL on error
//!
//! @pre
//!
//! @note
//!
static artifact *art_alloc_disk(virtualBootRecord * vbr, artifact * prereqs[], int num_prereqs, artifact * parts[], int num_parts,
                                artifact * emi_disk, boolean do_make_bootable, boolean do_make_work_copy)
{
    int i = 0;
    char art_id[48] = { 0 };        // ID of the artifact (append -##### hash of sig)
    char art_sig[ART_SIG_MAX] = "";
    char art_pref[EUCA_MAX_PATH] = "dsk";
    char part_sig[ART_SIG_MAX] = { 0 };
    long long disk_size_bytes = 512LL * MBR_BLOCKS;
    artifact *p = NULL;
    artifact *disk = NULL;

    // run through partitions, adding up their signatures and their size
    for (i = 0; i < num_parts; i++) {
        assert(parts);
        p = parts[i];

        // construct signature for the disk, based on the sigs of underlying components
        if ((snprintf(part_sig, sizeof(part_sig), "PARTITION %d (%s)\n%s\n\n", i, p->id, p->sig) >= sizeof(part_sig))   // output truncated
            || ((strlen(art_sig) + strlen(part_sig)) >= sizeof(art_sig))) { // overflow
            logprintfl(EUCAERROR, "[%s] error: internal buffers (ART_SIG_MAX) too small for signature\n", current_instanceId);
            return NULL;
        }
        strncat(art_sig, part_sig, sizeof(art_sig) - strlen(art_sig) - 1);

        // verify and add up the sizes of partitions
        if (p->size_bytes < 1) {
            logprintfl(EUCAERROR, "[%s] error: unknown size for partition %d\n", current_instanceId, i);
            return NULL;
        }

        if (p->size_bytes % 512) {
            logprintfl(EUCAERROR, "[%s] error: size for partition %d is not a multiple of 512\n", current_instanceId, i);
            return NULL;
        }
        disk_size_bytes += p->size_bytes;
        convert_id(p->id, art_pref, sizeof(art_pref));
    }

    // run through prerequisites (kernel and ramdisk), if any, adding up their signature
    // (this will not happen on KVM and Xen where injecting kernel is not necessary)
    for (i = 0; do_make_bootable && i < num_prereqs; i++) {
        p = prereqs[i];

        // construct signature for the disk, based on the sigs of underlying components
        if ((snprintf(part_sig, sizeof(part_sig), "PREREQUISITE %s\n%s\n\n", p->id, p->sig) >= sizeof(part_sig))    // output truncated
            || ((strlen(art_sig) + strlen(part_sig)) >= sizeof(art_sig))) { // overflow
            logprintfl(EUCAERROR, "[%s] error: internal buffers (ART_SIG_MAX) too small for signature\n", current_instanceId);
            return NULL;
        }
        strncat(art_sig, part_sig, sizeof(art_sig) - strlen(art_sig) - 1);
    }

    if (emi_disk) {             //! we have a full disk (@TODO remove this unused if-condition)
        if (do_make_work_copy) {    // allocate a work copy of it
            disk_size_bytes = emi_disk->size_bytes;
            if ((strlen(art_sig) + strlen(emi_disk->sig)) >= sizeof(art_sig)) { // overflow
                logprintfl(EUCAERROR, "[%s] error: internal buffers (ART_SIG_MAX) too small for signature\n", current_instanceId);
                return NULL;
            }
            strncat(art_sig, emi_disk->sig, sizeof(art_sig) - strlen(art_sig) - 1);

            if ((disk = art_alloc(emi_disk->id, art_sig, emi_disk->size_bytes, FALSE, FALSE, FALSE, copy_creator, NULL)) == NULL
                || art_add_dep(disk, emi_disk) != EUCA_OK) {
                goto free;
            }
        } else {
            disk = emi_disk;    // no work copy needed - we're done
        }

    } else {                    // allocate the 'raw' disk artifact
        if (art_gen_id(art_id, sizeof(art_id), art_pref, art_sig) != EUCA_OK)
            return NULL;

        disk = art_alloc(art_id, art_sig, disk_size_bytes, !do_make_work_copy, FALSE, TRUE, disk_creator, vbr);
        if (disk == NULL) {
            logprintfl(EUCAERROR, "[%s] error: failed to allocate an artifact for raw disk\n", disk->instanceId);
            return NULL;
        }
        disk->do_make_bootable = do_make_bootable;

        // attach partitions as dependencies of the raw disk        
        for (i = 0; i < num_parts; i++) {
            p = parts[i];
            if (art_add_dep(disk, p) != EUCA_OK) {
                logprintfl(EUCAERROR, "[%s] error: failed to add dependency to an artifact\n", disk->instanceId);
                goto free;
            }
            p->is_partition = TRUE;
        }

        // optionally, attach prereqs as dependencies of the raw disk
        for (int i = 0; do_make_bootable && i < num_prereqs; i++) {
            p = prereqs[i];
            if (art_add_dep(disk, p) != EUCA_OK) {
                logprintfl(EUCAERROR, "[%s] error: failed to add a prerequisite to an artifact\n", disk->instanceId);
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
    safe_strncpy(current_instanceId, instanceId, sizeof(current_instanceId));
}

//!
//! Creates a tree of artifacts for a given VBR (caller must free the tree)
//!
//! @param[in] vm pointer to virtual machine containing the VBR
//! @param[in] do_make_bootable make the disk bootable by copying kernel and ramdisk into it and running grub
//! @param[in] do_make_work_copy ensure that all components that get modified at run time have work copies
//! @param[in] sshkey key to inject into the root partition or NULL if no key
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return A pointer to the root of artifact tree or NULL on error
//!
//! @pre
//!
//! @note
//!
artifact *vbr_alloc_tree(virtualMachine * vm, boolean do_make_bootable, boolean do_make_work_copy, const char *sshkey, const char *instanceId)
{
    int i = 0;
    int j = 0;
    int k = 0;
    int total_parts = 0;
    int total_prereq_vbrs = 0;
    int total_prereq_arts = 0;
    int partitions = 0;
    artifact *root = NULL;
    artifact *dep = NULL;
    artifact *prereq_arts[EUCA_MAX_VBRS] = { NULL };
    artifact *disk_arts[EUCA_MAX_PARTITIONS];
    virtualBootRecord *vbr = NULL;
    virtualBootRecord *prereq_vbrs[EUCA_MAX_VBRS] = { NULL };
    virtualBootRecord *parts[BUS_TYPES_TOTAL][EUCA_MAX_DISKS][EUCA_MAX_PARTITIONS] = { { { NULL } } };
    const char *use_sshkey = NULL;

    if (instanceId)
        safe_strncpy(current_instanceId, instanceId, sizeof(current_instanceId));

    // sort vbrs into prereq [] and parts[] so they can be approached in the right order
    bzero(parts, sizeof(parts));    //! @fixme: chuck - this is not zeroing out the whole array!!!
    for (i = 0; i < EUCA_MAX_VBRS && i < vm->virtualBootRecordLen; i++) {
        vbr = &(vm->virtualBootRecord[i]);
        if (vbr->type == NC_RESOURCE_KERNEL || vbr->type == NC_RESOURCE_RAMDISK) {
            prereq_vbrs[total_prereq_vbrs++] = vbr;
        } else {
            parts[vbr->guestDeviceBus][vbr->diskNumber][vbr->partitionNumber] = vbr;
            total_parts++;
        }
    }
    logprintfl(EUCADEBUG, "[%s] found %d prereqs and %d partitions in the VBR\n", instanceId, total_prereq_vbrs, total_parts);

    root = art_alloc(instanceId, NULL, -1, FALSE, FALSE, FALSE, NULL, NULL);  // allocate a sentinel artifact
    if (root == NULL)
        return NULL;

    // allocate kernel and ramdisk artifacts.
    for (i = 0; i < total_prereq_vbrs; i++) {
        vbr = prereq_vbrs[i];
        dep = art_alloc_vbr(vbr, do_make_work_copy, TRUE, NULL);
        if (dep == NULL)
            goto free;

        prereq_arts[total_prereq_arts++] = dep;

        // if disk does not need to be bootable, we'll need 
        // kernel and ramdisk as a top-level dependencies
        if (!do_make_bootable) {
            if (art_add_dep(root, dep) != EUCA_OK)
                goto free;
        }
    }

    // then attach disks and partitions
    for (i = 0; i < BUS_TYPES_TOTAL; i++) {
        for (j = 0; j < EUCA_MAX_DISKS; j++) {
            bzero(disk_arts, sizeof(disk_arts));    //! @fixme: chuck - this is not zeroing out the whole structure!!!
            for (k = 0; k < EUCA_MAX_PARTITIONS; k++) {
                vbr = parts[i][j][k];
                use_sshkey = NULL;
                if (vbr) {      // either a disk (k==0) or a partition (k>0)
                    if (vbr->type == NC_RESOURCE_IMAGE && k > 0) {  // only inject SSH key into an EMI which has a single partition (whole disk)
                        use_sshkey = sshkey;
                    }

                    disk_arts[k] = art_alloc_vbr(vbr, do_make_work_copy, FALSE, use_sshkey);    // this brings in disks or partitions and their work copies, if requested
                    if (disk_arts[k] == NULL) {
                        arts_free(disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }

                    if (vbr->type == NC_RESOURCE_EBS)   // EBS-backed instances need no additional artifacts
                        continue;

                    if (k > 0) {
                        partitions++;
                    }
                } else if (partitions) {    // there were partitions and we saw them all
                    assert(disk_arts[0] == NULL);
                    if (vm->virtualBootRecordLen == EUCA_MAX_VBRS) {
                        logprintfl(EUCAERROR, "[%s] out of room in the virtual boot record while adding disk %d on bus %d\n", instanceId, j, i);
                        goto out;
                    }
                    disk_arts[0] = art_alloc_disk(&(vm->virtualBootRecord[vm->virtualBootRecordLen]),
                                                  prereq_arts, total_prereq_arts, disk_arts + 1, partitions, NULL, do_make_bootable,
                                                  do_make_work_copy);
                    if (disk_arts[0] == NULL) {
                        arts_free(disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }
                    vm->virtualBootRecordLen++;
                    break;      // out of the inner loop
                }
            }

            // run though all disk artifacts and either add the disk or all the partitions to sentinel
            for (k = 0; k < EUCA_MAX_PARTITIONS; k++) {
                if (disk_arts[k]) {
                    if (art_add_dep(root, disk_arts[k]) != EUCA_OK) {
                        arts_free(disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }
                    disk_arts[k] = NULL;
                    if (k == 0) {   // for a disk partition artifacts, if any, are already attached to it
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
    if ((bb = blockblob_open(bs, id, size_bytes, flags, sig, FIND_BLOB_TIMEOUT_USEC)) != NULL) {
        *bbp = bb;
    } else {
        ret = blobstore_get_error();
    }

    return ret;
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
    int flags = 0;
    const char *id_cache = NULL;
    char id_work[BLOBSTORE_MAX_PATH] = { 0 };
    long long size_bytes = 0;

    assert(a);

    // determine blob IDs for cache and work
    id_cache = a->id;
    if (work_prefix && strlen(work_prefix))
        snprintf(id_work, sizeof(id_work), "%s/%s", work_prefix, a->id);
    else
        safe_strncpy(id_work, a->id, sizeof(id_work));

    // determine flags
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
                return EUCA_OK; // creating only matters for blobs, which get locked, not for files
            } else {
                return BLOBSTORE_ERROR_NOENT;
            }
        } else {
            return EUCA_OK;
        }
    }

    assert(work_bs);
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

        } else {                // for all others we return the error or success
            if (ret == BLOBSTORE_ERROR_OK)
                a->is_in_cache = TRUE;
            return ret;
        }
    }

try_work:
    if (ret == BLOBSTORE_ERROR_SIGNATURE) {
        logprintfl(EUCAWARN, "[%s] warning: signature mismatch on cached blob %03d|%s\n", a->instanceId, a->seq, id_cache); //! @TODO maybe invalidate?
    }
    logprintfl(EUCADEBUG, "[%s] checking work blobstore for %03d|%s (do_create=%d ret=%d)\n", a->instanceId, a->seq, id_cache, do_create, ret);
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
    int i = 0;
    int ret = EUCA_OK;
    int tries = 0;
    int num_opened_deps = 0;
    long long started = time_usec();
    long long new_timeout_usec = 0L;
    boolean do_deps = TRUE;
    boolean do_create = TRUE;

    assert(root);

    logprintfl(EUCADEBUG, "[%s] implementing artifact %03d|%s\n", root->instanceId, root->seq, root->id);

    do {                        // we may have to retry multiple times due to competition
        num_opened_deps = 0;
        do_deps = TRUE;
        do_create = TRUE;

        if (tries++)
            usleep(ARTIFACT_RETRY_SLEEP_USEC);

        if (!root->creator) {   // sentinel nodes do not have a creator
            do_create = FALSE;
        } else {                // not a sentinel
            if (root->vbr && root->vbr->type == NC_RESOURCE_EBS)
                goto create;    // EBS artifacts have no disk manifestation and no dependencies, so skip to creation

            // try to open the artifact
            switch (ret = find_or_create_artifact(FIND, root, work_bs, cache_bs, work_prefix, &(root->bb))) {
            case BLOBSTORE_ERROR_OK:
                logprintfl(EUCADEBUG, "[%s] found existing artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
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
            default:           // all other errors
                logprintfl(EUCAERROR, "[%s] error: failed to provision artifact %03d|%s (error=%d) on try %d\n", root->instanceId, root->seq,
                           root->id, ret, tries);
                goto retry_or_fail;
            }
        }

        // at this point the artifact we need does not seem to exist
        // (though it could be created before we get around to that)

        if (do_deps) {
            // recursively go over dependencies, if any
            for (i = 0; i < MAX_ARTIFACT_DEPS && root->deps[i]; i++) {
                // recalculate the time that remains in the timeout period
                new_timeout_usec = timeout_usec;
                if (timeout_usec > 0) {
                    new_timeout_usec -= time_usec() - started;
                    if (new_timeout_usec < 1) { // timeout exceeded, so bail out of this function
                        ret = BLOBSTORE_ERROR_AGAIN;
                        goto retry_or_fail;
                    }
                }

                switch (ret = art_implement_tree(root->deps[i], work_bs, cache_bs, work_prefix, new_timeout_usec)) {
                case BLOBSTORE_ERROR_OK:
                    if (do_create) {    // we'll hold the dependency open for the creator
                        num_opened_deps++;
                    } else {    // this is a sentinel, we're not creating anything, so release the dep immediately
                        if (root->deps[i]->bb && (blockblob_close(root->deps[i]->bb) == -1)) {
                            logprintfl(EUCAERROR, "[%s] error: failed to close dependency of %s: %d %s (potential resource leak!) on try %d\n",
                                       root->instanceId, root->id, blobstore_get_error(), blobstore_get_last_msg(), tries);
                        }
                        root->deps[i]->bb = 0;  // for debugging
                    }
                    break;      // out of the switch statement
                case BLOBSTORE_ERROR_AGAIN:    // timed out => the competition took too long
                case BLOBSTORE_ERROR_MFILE:    // out of file descriptors for locking => same problem
                    goto retry_or_fail;
                default:       // all other errors
                    logprintfl(EUCAERROR, "[%s] error: failed to provision dependency %s for artifact %s (error=%d) on try %d\n", root->instanceId,
                               root->deps[i]->id, root->id, ret, tries);
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
                logprintfl(EUCADEBUG, "[%s] bypassing redundant artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
                root->bb = root->deps[0]->bb;
                root->deps[0]->bb = NULL;
                num_opened_deps--;  // so we won't attempt to close deps's blockblob
            } else {
                // try to create the artifact since last time we checked it did not exist
                switch (ret = find_or_create_artifact(CREATE, root, work_bs, cache_bs, work_prefix, &(root->bb))) {
                case BLOBSTORE_ERROR_OK:
                    logprintfl(EUCADEBUG, "[%s] created a blob for an artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
                    break;
                case BLOBSTORE_ERROR_EXIST:    // someone else created it => loop back and open it
                    ret = BLOBSTORE_ERROR_AGAIN;
                    goto retry_or_fail;
                    break;
                case BLOBSTORE_ERROR_AGAIN:    // timed out (but probably exists)
                case BLOBSTORE_ERROR_MFILE:    // out of file descriptors for locking => same problem
                    goto retry_or_fail;
                    break;
                default:       // all other errors
                    logprintfl(EUCAERROR, "[%s] error: failed to allocate artifact %s (%d %s) on try %d\n", root->instanceId, root->id, ret,
                               blobstore_get_last_msg(), tries);
                    goto retry_or_fail;
                }
            }

create:
            ret = root->creator(root);  // create and open this artifact for exclusive use
            if (ret != EUCA_OK) {
                logprintfl(EUCAERROR, "[%s] error: failed to create artifact %s (error=%d, may retry) on try %d\n", root->instanceId, root->id, ret,
                           tries);
                // delete the partially created artifact so we can retry with a clean slate
                if (root->id_is_path) { // artifact is not a blob, but a file
                    unlink(root->id);   // attempt to delete, but it may not even exist
                } else {
                    if (blockblob_delete(root->bb, DELETE_BLOB_TIMEOUT_USEC, 0) == -1) {
                        // failure of 'delete' is bad, since we may have an open blob
                        // that will prevent others from ever opening it again, so at
                        // least try to close it
                        logprintfl(EUCAERROR,
                                   "[%s] error: failed to remove partially created artifact %s: %d %s (potential resource leak!) on try %d\n",
                                   root->instanceId, root->id, blobstore_get_error(), blobstore_get_last_msg(), tries);
                        if (blockblob_close(root->bb) == -1) {
                            logprintfl(EUCAERROR,
                                       "[%s] error: failed to close partially created artifact %s: %d %s (potential deadlock!) on try %d\n",
                                       root->instanceId, root->id, blobstore_get_error(), blobstore_get_last_msg(), tries);
                        }
                    }
                }
            } else {
                if (root->vbr && root->vbr->type != NC_RESOURCE_EBS)
                    update_vbr_with_backing_info(root);
            }
        }

retry_or_fail:
        // close all opened dependent blobs, whether we're trying again or returning
        for (i = 0; i < num_opened_deps; i++) {
            blockblob_close(root->deps[i]->bb);
            root->deps[i]->bb = 0;  // for debugging
        }

    } while ((ret == BLOBSTORE_ERROR_AGAIN || ret == BLOBSTORE_ERROR_MFILE) // only timeout-type error causes us to keep trying
             && (timeout_usec == 0  // indefinitely if there is no timeout at all
                 || (time_usec() - started) < timeout_usec));   // or until we exceed the timeout

    if (ret != EUCA_OK) {
        logprintfl(EUCADEBUG, "[%s] error: failed to implement artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
    } else {
        logprintfl(EUCADEBUG, "[%s] implemented artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
    }

    return ret;
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
static blobstore *create_teststore(int size_blocks, const char *base, const char *name, blobstore_format_t format, blobstore_revocation_t revocation,
                                   blobstore_snapshot_t snapshot)
{
    char bs_path[PATH_MAX] = { 0 };
    static int ts = 0;

    if (ts == 0) {
        ts = ((int)time(NULL)) - 1292630988;
    }

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
                    long long size,
                    ncResourceFormatType format,
                    char *formatName,
                    const char *id, ncResourceType type, ncResourceLocationType locationType, int diskNumber, int partitionNumber,
                    libvirtBusType guestDeviceBus, char *preparedResourceLocation)
{
    virtualBootRecord *vbr = vm->virtualBootRecord + vm->virtualBootRecordLen++;

    vbr->size = size;
    if (formatName)
        safe_strncpy(vbr->formatName, formatName, sizeof(vbr->formatName));

    if (id)
        safe_strncpy(vbr->id, id, sizeof(vbr->id));

    vbr->format = format;
    vbr->type = type;
    vbr->locationType = locationType;
    vbr->diskNumber = diskNumber;
    vbr->partitionNumber = partitionNumber;
    vbr->guestDeviceBus = guestDeviceBus;

    if (preparedResourceLocation)
        safe_strncpy(vbr->preparedResourceLocation, preparedResourceLocation, sizeof(vbr->preparedResourceLocation));
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
static int provision_vm(const char *id, const char *sshkey, const char *eki, const char *eri, const char *emi, blobstore * cache_bs,
                        blobstore * work_bs, boolean do_make_work_copy)
{
    int ret = EUCA_OK;
    virtualMachine *vm = NULL;
    artifact *sentinel = NULL;

    pthread_mutex_lock(&competitors_mutex);
    {
        vm = &(vm_slots[next_instances_slot]);  // we don't use vm_slots[] pointers in code
        safe_strncpy(vm_ids[next_instances_slot], id, PATH_MAX);
        next_instances_slot++;
    }
    pthread_mutex_unlock(&competitors_mutex);

    bzero(vm, sizeof(*vm));
    add_vbr(vm, EKI_SIZE, NC_FORMAT_NONE, "none", eki, NC_RESOURCE_KERNEL, NC_LOCATION_NONE, 0, 0, 0, NULL);
    add_vbr(vm, EKI_SIZE, NC_FORMAT_NONE, "none", eri, NC_RESOURCE_RAMDISK, NC_LOCATION_NONE, 0, 0, 0, NULL);
    add_vbr(vm, VBR_SIZE, NC_FORMAT_EXT3, "ext3", emi, NC_RESOURCE_IMAGE, NC_LOCATION_NONE, 0, 1, BUS_TYPE_SCSI, NULL);
    add_vbr(vm, VBR_SIZE, NC_FORMAT_EXT3, "ext3", "none", NC_RESOURCE_EPHEMERAL, NC_LOCATION_NONE, 0, 3, BUS_TYPE_SCSI, NULL);
    add_vbr(vm, VBR_SIZE, NC_FORMAT_SWAP, "swap", "none", NC_RESOURCE_SWAP, NC_LOCATION_NONE, 0, 2, BUS_TYPE_SCSI, NULL);

    safe_strncpy(current_instanceId, strstr(id, "/") + 1, sizeof(current_instanceId));
    if ((sentinel = vbr_alloc_tree(vm, FALSE, do_make_work_copy, sshkey, id)) == NULL) {
        printf("error: vbr_alloc_tree failed id=%s\n", id);
        return (EUCA_MEMORY_ERROR);
    }

    printf("implementing artifact tree sentinel=%012lx\n", (unsigned long)sentinel);
    if ((ret = art_implement_tree(sentinel, work_bs, cache_bs, id, 1000000LL * 60 * 2)) != EUCA_OK) {
        printf("error: art_implement_tree failed ret=%d sentinel=%012lx\n", ret, (unsigned long)sentinel);
        return (EUCA_ERROR);
    }

    pthread_mutex_lock(&competitors_mutex);
    {
        provisioned_instances++;
    }
    pthread_mutex_unlock(&competitors_mutex);

    printf("freeing artifact tree sentinel=%012lx\n", (unsigned long)sentinel);
    ART_FREE(sentinel);

    return (EUCA_OK);
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
    int i = 0;
    int errors = 0;
    char *id = NULL;
    char regex[PATH_MAX] = { 0 };

    pthread_mutex_lock(&competitors_mutex);
    {
        for (i = 0; i < provisioned_instances; i++) {
            id = vm_ids[next_instances_slot - i - 1];
            snprintf(regex, sizeof(regex), "%s/.*", id);
            errors += (blobstore_delete_regex(work_bs, regex) < 0);
        }
        provisioned_instances = 0;
    }
    pthread_mutex_unlock(&competitors_mutex);

    return (errors);
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
    return (id);
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
    int i = 0;
    int status = 0;
    int errors = 0;
    pid_t pid = -1;
    char id[32] = { 0 };

    if (do_fork) {
        pid = fork();
        if (pid < 0) {          // fork problem
            *(long long *)ptr = 1;
            return NULL;
        } else if (pid > 0) {   // parent
            waitpid(pid, &status, 0);
            *(long long *)ptr = WEXITSTATUS(status);
            return NULL;
        }
    }

    if (pid < 1) {
        printf("%u/%u: competitor running (provisioned=%d)\n", (unsigned int)pthread_self(), (int)getpid(), provisioned_instances);
        for (i = 0; i < COMPETITIVE_ITERATIONS; i++) {
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

//!
//! check if the blobstore has the expected number of 'block' entries
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
    int bytes = 0;
    char buf[32] = { 0 };
    char cmd[1024] = { 0 };
    FILE *f = NULL;

    snprintf(cmd, sizeof(cmd), "find %s | grep %s | wc -l", bs->path, keyword);
    if ((f = popen(cmd, "r")) == NULL) {
        printf("error: failed to popen() command '%s'\n", cmd);
        perror("test_vbr");
        return (EUCA_PERMISSION_ERROR);
    }

    if ((bytes = fread(buf, 1, sizeof(buf) - 1, f)) < 1) {
        printf("error: failed to fread() from output of '%s' (returned %d)\n", cmd, bytes);
        perror("test_vbr");
        pclose(f);
        return (EUCA_ACCESS_ERROR);
    }
    buf[bytes] = '\0';

    if (pclose(f)) {
        printf("error: failed pclose()\n");
        perror("test_vbr");
        return (EUCA_ACCESS_ERROR);
    }

    int found = atoi(buf);
    if (found != expect) {
        printf("warning: unexpected disk state: [%s] = %d != %d\n", cmd, found, expect);
        return (EUCA_ERROR);
    }

    return (EUCA_OK);
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
    logprintfl(EUCADEBUG, "BLOBSTORE: %s\n", msg);
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
#define CHECK_BLOBS                                                        \
{                                                                          \
    warnings += check_blob (cache_bs, "blocks", 4 + 1 * emis_in_use);      \
    warnings += check_blob (work_bs, "blocks", 6 * provisioned_instances); \
}

    int i = 0;
    int errors = 0;
    int warnings = 0;
    int emis_in_use = 1;
    int thread_par_sum = 0;
    char id[32] = { 0 };
    char cwd[1024] = { 0 };
    pthread_t threads[COMPETITIVE_PARTICIPANTS] = { 0 };
    long long thread_par[COMPETITIVE_PARTICIPANTS] = { 0 };

    getcwd(cwd, sizeof(cwd));
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

    if (errors += provision_vm(GEN_ID(), KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, TRUE))
        goto out;

    CHECK_BLOBS;
    warnings += cleanup_vms();
    CHECK_BLOBS;

    for (i = 0; i < SERIAL_ITERATIONS; i++) {
        errors += provision_vm(GEN_ID(), KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, TRUE);
    }

    if (errors) {
        printf("error: failed sequential instance provisioning test\n");
    }

    CHECK_BLOBS;
    warnings += cleanup_vms();
    CHECK_BLOBS;

    for (i = 0; i < 2; i++) {
        if (i % 1) {
            do_fork = 0;
        } else {
            do_fork = 1;
        }

        printf("===============================================\n");
        printf("spawning %d competing %s\n", COMPETITIVE_PARTICIPANTS, (do_fork) ? ("processes") : ("threads"));
        emis_in_use++;          // we'll have threads creating a new EMI
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

out:
    printf("\nfinal check of work blobstore\n");
    check_blob(work_bs, "blocks", 0);
    printf("cleaning cache blobstore\n");
    blobstore_delete_regex(cache_bs, ".*");
    check_blob(cache_bs, "blocks", 0);

    printf("done with vbr.c errors=%d warnings=%d\n", errors, warnings);
    exit(errors);

#undef CHECK_BLOBS
}
#endif /* _UNIT_TEST */
