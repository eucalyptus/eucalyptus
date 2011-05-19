// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

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

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h> // strcasestr
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <fcntl.h>
#include <errno.h>
#include <limits.h>
#include <assert.h>
#include <dirent.h>
#include "misc.h" // logprintfl, ensure_...
#include "data.h"
#include "vbr.h"
#include "walrus.h"
#include "blobstore.h"
#include "diskutil.h"

static int // returns OK or ERROR
prep_location ( // picks a service URI and prepends it to resourceLocation in VBR 
               virtualBootRecord * vbr, 
               ncMetadata * meta, 
               const char * typeName)
{
    int i;
    
    for (i=0; i<meta->servicesLen; i++) {
        serviceInfoType * service = &(meta->services[i]);
        if (strncmp(service->type, typeName, strlen(typeName)-3)==0 && service->urisLen>0) {
            char * l = vbr->resourceLocation + (strlen (typeName) + 3); // +3 for "://", so 'l' points past, e.g., "walrus:"
            snprintf (vbr->preparedResourceLocation, sizeof(vbr->preparedResourceLocation), "%s%s", service->uris[0], l); // TODO: for now we just pick the first one
            return OK;
        }
    }
    logprintfl (EUCAERROR, "failed to find service '%s' in eucalyptusMessage\n", typeName);
    return ERROR;
}

static int // returns OK or ERROR
parse_rec ( // parses the VBR as supplied by a client or user, checks values, and fills out almost the rest of the struct with typed values
           virtualBootRecord * vbr, // a VBR record to parse and verify
           virtualMachine * vm, // OPTIONAL parameter for setting image/kernel/ramdik pointers in the virtualMachine struct
           ncMetadata * meta) // OPTIONAL parameter for translating, e.g., walrus:// URI into http:// URI

{
    // check the type (the only mandatory field)
    if (strstr (vbr->typeName, "machine") == vbr->typeName) { 
        vbr->type = NC_RESOURCE_IMAGE; 
        if (vm)
            vm->image = vbr;
    } else if (strstr (vbr->typeName, "kernel") == vbr->typeName) { 
        vbr->type = NC_RESOURCE_KERNEL; 
        if (vm)
            vm->kernel = vbr;
    } else if (strstr (vbr->typeName, "ramdisk") == vbr->typeName) { 
        vbr->type = NC_RESOURCE_RAMDISK; 
        if (vm)
            vm->ramdisk = vbr;
    } else if (strstr (vbr->typeName, "ephemeral") == vbr->typeName) { 
        vbr->type = NC_RESOURCE_EPHEMERAL; 
        if (strstr (vbr->typeName, "ephemeral0") == vbr->typeName) { // TODO: remove
            if (vm) {
                vm->ephemeral0 = vbr;
            }
        }
    } else if (strstr (vbr->typeName, "swap") == vbr->typeName) { // TODO: remove
        vbr->type = NC_RESOURCE_SWAP; 
        if (vm)
            vm->swap = vbr;
    } else if (strstr (vbr->typeName, "ebs") == vbr->typeName) { 
        vbr->type = NC_RESOURCE_EBS;
    } else {
        logprintfl (EUCAERROR, "Error: failed to parse resource type '%s'\n", vbr->typeName);
        return ERROR;
    }
    
    // identify the type of resource location from location string
    int error = OK;
    if (strcasestr (vbr->resourceLocation, "http://") == vbr->resourceLocation) { 
        vbr->locationType = NC_LOCATION_URL;
        strncpy (vbr->preparedResourceLocation, vbr->resourceLocation, sizeof(vbr->preparedResourceLocation));
    } else if (strcasestr (vbr->resourceLocation, "iqn://") == vbr->resourceLocation ||
               strchr (vbr->resourceLocation, ',')) { // TODO: remove this transitionary iSCSI crutch?
        vbr->locationType = NC_LOCATION_IQN;
    } else if (strcasestr (vbr->resourceLocation, "aoe://") == vbr->resourceLocation ||
               strcasestr (vbr->resourceLocation, "/dev/") == vbr->resourceLocation ) { // TODO: remove this transitionary AoE crutch
        vbr->locationType = NC_LOCATION_AOE;
    } else if (strcasestr (vbr->resourceLocation, "walrus://") == vbr->resourceLocation) {
        vbr->locationType = NC_LOCATION_WALRUS;
        if (meta) 
            error = prep_location (vbr, meta, "walrus");
    } else if (strcasestr (vbr->resourceLocation, "cloud://") == vbr->resourceLocation) {
        vbr->locationType = NC_LOCATION_CLC;
        if (meta)
            error = prep_location (vbr, meta, "cloud");
    } else if (strcasestr (vbr->resourceLocation, "sc://") == vbr->resourceLocation ||
               strcasestr (vbr->resourceLocation, "storage://") == vbr->resourceLocation) { // TODO: is it 'sc' or 'storage'?
        vbr->locationType = NC_LOCATION_SC;
        if (meta)
            error = prep_location (vbr, meta, "sc");
    } else if (strcasestr (vbr->resourceLocation, "none") == vbr->resourceLocation) { 
        if (vbr->type!=NC_RESOURCE_EPHEMERAL && vbr->type!=NC_RESOURCE_SWAP) {
            logprintfl (EUCAERROR, "Error: resourceLocation not specified for non-ephemeral resource '%s'\n", vbr->resourceLocation);
            return ERROR;
        }            
        vbr->locationType = NC_LOCATION_NONE;
    } else {
        logprintfl (EUCAERROR, "Error: failed to parse resource location '%s'\n", vbr->resourceLocation);
        return ERROR;
    }
    
    if (error!=OK) {
        logprintfl (EUCAERROR, "Error: URL for resourceLocation '%s' is not in the message\n", vbr->resourceLocation);
        return ERROR;
    }
    
    // device can be 'none' only for kernel and ramdisk types
    if (!strcmp (vbr->guestDeviceName, "none")) {
        if (vbr->type!=NC_RESOURCE_KERNEL &&
            vbr->type!=NC_RESOURCE_RAMDISK) {
            logprintfl (EUCAERROR, "Error: guestDeviceName not specified for resource '%s'\n", vbr->resourceLocation);
            return ERROR;
        }
        
    } else { // should be a valid device
        
        // trim off "/dev/" prefix, if present, and verify the rest
        if (strstr (vbr->guestDeviceName, "/dev/") == vbr->guestDeviceName) {
            logprintfl (EUCAWARN, "Warning: trimming off invalid prefix '/dev/' from guestDeviceName '%s'\n", vbr->guestDeviceName);
            char buf [10];
            strncpy (buf, vbr->guestDeviceName + 5, sizeof (buf));
            strncpy (vbr->guestDeviceName, buf, sizeof (vbr->guestDeviceName));
        }
        
        if (strlen (vbr->guestDeviceName)<3 ||
            (vbr->guestDeviceName [0] == 'x' && strlen(vbr->guestDeviceName) < 4)) {
            logprintfl (EUCAERROR, "Error: invalid guestDeviceName '%s'\n", vbr->guestDeviceName);
            return ERROR;
        }
        
        {
            int letters_len = 3; // e.g. "sda"
            if (vbr->guestDeviceName [0] == 'x') letters_len = 4; // e.g., "xvda"
            char t = vbr->guestDeviceName [0]; // type
            char d = vbr->guestDeviceName [letters_len-2]; // the 'd'
            char n = vbr->guestDeviceName [letters_len-1]; // the disk number
            long long int p = 0;
            if (strlen (vbr->guestDeviceName) > letters_len) {
                errno = 0;
                p = strtoll (vbr->guestDeviceName + letters_len, NULL, 10);
                if (errno!=0) { 
                    logprintfl (EUCAERROR, "Error: failed to parse partition number in guestDeviceName '%s'\n", vbr->guestDeviceName);
                    return ERROR; 
                } 
                if (p<1 || p>EUCA_MAX_PARTITIONS) {
                    logprintfl (EUCAERROR, "Error: unexpected partition number '%d' in guestDeviceName '%s'\n", p, vbr->guestDeviceName);
                    return ERROR;
                }
                vbr->partitionNumber = p;
            } else {
                vbr->partitionNumber = 0;
            }
            
            switch (t) {
            case 'h': vbr->guestDeviceType = DEV_TYPE_DISK;   vbr->guestDeviceBus = BUS_TYPE_IDE; break;
            case 's': vbr->guestDeviceType = DEV_TYPE_DISK;   vbr->guestDeviceBus = BUS_TYPE_SCSI; break;
            case 'f': vbr->guestDeviceType = DEV_TYPE_FLOPPY; vbr->guestDeviceBus = BUS_TYPE_IDE; break;
            case 'v': vbr->guestDeviceType = DEV_TYPE_DISK;   vbr->guestDeviceBus = BUS_TYPE_VIRTIO; break;
            case 'x': vbr->guestDeviceType = DEV_TYPE_DISK;   vbr->guestDeviceBus = BUS_TYPE_XEN; break;
            default:
                logprintfl (EUCAERROR, "Error: failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                return ERROR; 
            }
            if (d!='d') {
                logprintfl (EUCAERROR, "Error: failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                return ERROR; 
            }
            assert (EUCA_MAX_DISKS >= 'z'-'a');
            if (!(n>='a' && n<='z')) {
                logprintfl (EUCAERROR, "Error: failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                return ERROR; 
            }
            vbr->diskNumber = n - 'a';
        }
    }
    
    // parse ID
    if (strlen (vbr->id)<4) {
        logprintfl (EUCAERROR, "Error: failed to parse VBR resource ID '%s' (use 'none' when no ID)\n", vbr->id);
        return ERROR;
    }
    
    // parse disk formatting instructions (none = do not format)
    if (strstr (vbr->formatName, "none") == vbr->formatName) { vbr->format = NC_FORMAT_NONE;
    } else if (strstr (vbr->formatName, "ext2") == vbr->formatName) { vbr->format = NC_FORMAT_EXT2;
    } else if (strstr (vbr->formatName, "ext3") == vbr->formatName) { vbr->format = NC_FORMAT_EXT3;
    } else if (strstr (vbr->formatName, "ntfs") == vbr->formatName) { vbr->format = NC_FORMAT_NTFS;
    } else if (strstr (vbr->formatName, "swap") == vbr->formatName) { vbr->format = NC_FORMAT_SWAP;
    } else {
        logprintfl (EUCAERROR, "Error: failed to parse resource format '%s'\n", vbr->formatName);
        return ERROR;
    }
    if (vbr->type==NC_RESOURCE_EPHEMERAL || vbr->type==NC_RESOURCE_SWAP) { // TODO: should we allow ephemeral/swap that reside remotely?
        if (vbr->size<1) {
            logprintfl (EUCAERROR, "Error: invalid size '%d' for ephemeral resource '%s'\n", vbr->size, vbr->resourceLocation);
            return ERROR;
        }
    } else {
        //            if (vbr->size!=1 || vbr->format!=NC_FORMAT_NONE) { // TODO: check for size!=-1 
        if (vbr->format!=NC_FORMAT_NONE) {
            logprintfl (EUCAERROR, "Error: invalid size '%d' or format '%s' for non-ephemeral resource '%s'\n", vbr->size, vbr->formatName, vbr->resourceLocation);
            return ERROR;
        }
    }
    
    return OK;
}

int // returns OK or ERROR
vbr_parse ( // parses and verifies all VBR entries in the virtual machine definition
           virtualMachine * vm, // vm definition containing VBR records
           ncMetadata * meta) // OPTIONAL parameter for translating, e.g., walrus:// URI into http:// URI
{
    unsigned char partitions [BUS_TYPES_TOTAL][EUCA_MAX_DISKS][EUCA_MAX_PARTITIONS]; // for validating partitions
    bzero (partitions, sizeof (partitions));
    for (int i=0, j=0; i<EUCA_MAX_VBRS && i<vm->virtualBootRecordLen; i++) {
        virtualBootRecord * vbr = &(vm->virtualBootRecord[i]);

        if (parse_rec (vbr, vm, meta) != OK)
            return ERROR;
        
        if (vbr->type!=NC_RESOURCE_KERNEL && vbr->type!=NC_RESOURCE_RAMDISK)
            partitions [vbr->guestDeviceBus][vbr->diskNumber][vbr->partitionNumber] = 1;            
    }
    
    // ensure that partitions are contiguous and that partitions and disks are not mixed
    for (int i=0; i<BUS_TYPES_TOTAL; i++) { // each bus type is treated separatedly
        for (int j=0; j<EUCA_MAX_DISKS; j++) {
            int has_partitions = 0;
            for (int k=EUCA_MAX_PARTITIONS-1; k>=0; k--) { // count down 
                if (partitions [i][j][k]) {
                    if (k==0 && has_partitions) {
                        logprintfl (EUCAERROR, "Error: specifying both disk and a partition on the disk is not allowed\n");
                        return ERROR;
                    }
                    has_partitions = 1;
                } else {
                    if (k!=0 && has_partitions) {
                        logprintfl (EUCAERROR, "Error: gaps in partition table are not allowed\n");
                        return ERROR;
                    }
                }
            }
        }
    }

    return OK;
}

int // returns OK or ERROR
vbr_legacy ( // constructs VBRs for {image|kernel|ramdisk}x{Id|URL} entries (DEPRECATED)
            virtualMachine *params, 
            char *imageId, char *imageURL, // OPTIONAL
            char *kernelId, char *kernelURL, // OPTIONAL
            char *ramdiskId, char *ramdiskURL) // OPTIONAL
{
    int i;
    int found_image = 0;
    int found_kernel = 0;
    int found_ramdisk = 0;
    
    for (i=0; i<EUCA_MAX_VBRS && i<params->virtualBootRecordLen; i++) {
        virtualBootRecord * vbr = &(params->virtualBootRecord[i]);
        if (strlen(vbr->resourceLocation)>0) {
            logprintfl (EUCAINFO, "                         device mapping: type=%s id=%s dev=%s size=%d format=%s %s\n", 
                        vbr->id, vbr->typeName, vbr->guestDeviceName, vbr->size, vbr->formatName, vbr->resourceLocation);
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
            logprintfl (EUCAINFO, "                         IGNORING image %s passed outside the virtual boot record\n", imageId);
        } else {
            logprintfl (EUCAINFO, "                         LEGACY pre-VBR image id=%s URL=%s\n", imageId, imageURL);
            if (i>=EUCA_MAX_VBRS-2) {
                logprintfl (EUCAERROR, "Out of room in the Virtual Boot Record for legacy image %s\n", imageId);
                return ERROR;
            }
            
            { // create root partition VBR
                virtualBootRecord * vbr = &(params->virtualBootRecord[i++]);
                strncpy (vbr->resourceLocation, imageURL, sizeof (vbr->resourceLocation));
                strncpy (vbr->guestDeviceName, "sda1", sizeof (vbr->guestDeviceName));
                strncpy (vbr->id, imageId, sizeof (vbr->id));
                strncpy (vbr->typeName, "machine", sizeof (vbr->typeName));
                vbr->size = -1;
                strncpy (vbr->formatName, "none", sizeof (vbr->formatName));
                params->virtualBootRecordLen++;
            }
            { // create ephemeral partition VBR
                virtualBootRecord * vbr = &(params->virtualBootRecord[i++]);
                strncpy (vbr->resourceLocation, "none", sizeof (vbr->resourceLocation));
                strncpy (vbr->guestDeviceName, "sda2", sizeof (vbr->guestDeviceName));
                strncpy (vbr->id, "none", sizeof (vbr->id));
                strncpy (vbr->typeName, "ephemeral0", sizeof (vbr->typeName));
                vbr->size = 524288; // we cannot compute it here, so pick something
                strncpy (vbr->formatName, "ext2", sizeof (vbr->formatName));
                params->virtualBootRecordLen++;
            }
            { // create swap partition VBR
                virtualBootRecord * vbr = &(params->virtualBootRecord[i++]);
                strncpy (vbr->resourceLocation, "none", sizeof (vbr->resourceLocation));
                strncpy (vbr->guestDeviceName, "sda3", sizeof (vbr->guestDeviceName));
                strncpy (vbr->id, "none", sizeof (vbr->id));
                strncpy (vbr->typeName, "swap", sizeof (vbr->typeName));
                vbr->size = 524288;
                strncpy (vbr->formatName, "swap", sizeof (vbr->formatName));
                params->virtualBootRecordLen++;
            }
        }
    }
    
    // legacy support for kernel{Id|URL}
    if (kernelId && kernelURL) {
        if (found_kernel) {
            logprintfl (EUCAINFO, "                         IGNORING kernel %s passed outside the virtual boot record\n", kernelId);
        } else {
            logprintfl (EUCAINFO, "                         LEGACY pre-VBR kernel id=%s URL=%s\n", kernelId, kernelURL);
            if (i==EUCA_MAX_VBRS) {
                logprintfl (EUCAERROR, "Out of room in the Virtual Boot Record for legacy kernel %s\n", kernelId);
                return ERROR;
            }
            virtualBootRecord * vbr = &(params->virtualBootRecord[i++]);
            strncpy (vbr->resourceLocation, kernelURL, sizeof (vbr->resourceLocation));
            strncpy (vbr->guestDeviceName, "none", sizeof (vbr->guestDeviceName));
            strncpy (vbr->id, kernelId, sizeof (vbr->id));
            strncpy (vbr->typeName, "kernel", sizeof (vbr->typeName));
            vbr->size = -1;
            strncpy (vbr->formatName, "none", sizeof (vbr->formatName));
            params->virtualBootRecordLen++;
        }
    }
    
    // legacy support for ramdisk{Id|URL}
    if (ramdiskId && ramdiskURL) {
        if (found_ramdisk) {
            logprintfl (EUCAINFO, "                         IGNORING ramdisk %s passed outside the virtual boot record\n", ramdiskId);
        } else {
            logprintfl (EUCAINFO, "                         LEGACY pre-VBR ramdisk id=%s URL=%s\n", ramdiskId, ramdiskURL);
            if (i==EUCA_MAX_VBRS) {
                logprintfl (EUCAERROR, "Out of room in the Virtual Boot Record for legacy ramdisk %s\n", ramdiskId);
                return ERROR;
            }
            virtualBootRecord * vbr = &(params->virtualBootRecord[i++]);
            strncpy (vbr->resourceLocation, ramdiskURL, sizeof (vbr->resourceLocation));
            strncpy (vbr->guestDeviceName, "none", sizeof (vbr->guestDeviceName));
            strncpy (vbr->id, ramdiskId, sizeof (vbr->id));
            strncpy (vbr->typeName, "ramdisk", sizeof (vbr->typeName));
            vbr->size = -1;
            strncpy (vbr->formatName, "none", sizeof (vbr->formatName));
            params->virtualBootRecordLen++;
        }
    }
    return OK;
}

static void update_vbr_with_backing_info (virtualBootRecord * vbr, blockblob * bb, boolean must_be_file)
{
    if (! must_be_file && strlen (blockblob_get_dev (bb))) {
        strncpy (vbr->backingPath, blockblob_get_dev (bb), sizeof (vbr->backingPath));
        vbr->backingType = SOURCE_TYPE_BLOCK;
    } else {
        strncpy (vbr->backingPath, blockblob_get_file (bb), sizeof (vbr->backingPath));
        vbr->backingType = SOURCE_TYPE_FILE;
    }
}

// These *_creator funcitons produce an artifact in blobstore, either from
// scratch (such as Walrus download or a new partition) or by converting,
// combining, and augmenting existing artifacts.  When invoked, creators
// can assume that any input blobs and the output blob are open (and 
// thus locked for their exclusive use).

static int walrus_creator (artifact * a) // creates an artifact by downloading it from Walrus
{
    assert (a->bb);
    assert (a->vbr);
    virtualBootRecord * vbr = a->vbr;
    const char * dest_path = blockblob_get_file (a->bb);

    assert (vbr->preparedResourceLocation);
    logprintfl (EUCAINFO, 
                "[%s] downloading from Walrus %s\n"
                "[%s]             to %s\n", 
                a->instanceId, vbr->preparedResourceLocation, 
                a->instanceId, dest_path);
    if (walrus_image_by_manifest_url (vbr->preparedResourceLocation, dest_path, TRUE) != OK) {
        logprintfl (EUCAERROR, "[%s] error: failed to download component %s\n", a->instanceId, vbr->preparedResourceLocation);
        return ERROR;
    }
    update_vbr_with_backing_info (vbr, a->bb, a->must_be_file);

    return OK;
}

static int partition_creator (artifact * a) // creates a new partition from scratch
{
    assert (a->bb);
    assert (a->vbr);
    virtualBootRecord * vbr = a->vbr;
    const char * dest_dev = blockblob_get_dev (a->bb);

    assert (dest_dev);
    logprintfl (EUCAINFO, "[%s] creating partition of size %lld bytes and type %s in %s\n", a->instanceId, vbr->size, vbr->formatName, a->id);
    int format = ERROR;
    switch (vbr->format) {
    case NC_FORMAT_NONE:
        format = OK;
        break;
    case NC_FORMAT_EXT2: // TODO: distinguish ext2 and ext3!
    case NC_FORMAT_EXT3:
        format = diskutil_mkfs (dest_dev, vbr->size);
        break;
    case NC_FORMAT_SWAP:
        format = diskutil_mkswap (dest_dev, vbr->size);
        break;
    default:
        logprintfl (EUCAERROR, "[%s] error: format of type %s is NOT IMPLEMENTED\n", a->instanceId, vbr->formatName);
    }
    
    if (format == OK) {
        update_vbr_with_backing_info (vbr, a->bb, a->must_be_file);
    } else {
        logprintfl (EUCAERROR, "[%s] failed to create partition in blob %s\n", a->instanceId, a->id);
        return ERROR;
    }

    return OK;
}

static int disk_creator (artifact * a)
{
    return OK;
}

static int keyed_disk_creator (artifact * a)
{
    return OK;
}

static int iqn_creator (artifact * a)
{
    return OK;
}

static int copy_creator (artifact * a)
{
    return OK;
}

#define ART_SIG_MAX 4096

static int art_add_dep (artifact * a, artifact * dep) 
{
    for (int i = 0; i < MAX_ARTIFACT_DEPS; i++) {
        if (a->deps [i] == NULL) {
            logprintfl (EUCADEBUG, "added to artifact %03d/%s artifact %03d/%s\n", a->seq, a->id, dep->seq, dep->id);
            a->deps [i] = dep;
            return OK;
        }
    }
    return ERROR;
}

artifact * art_free (artifact * a) // frees the artifact and all its dependencies, always returns NULL
{
    for (int i = 0; i < MAX_ARTIFACT_DEPS && a->deps [i]; i++) {
        art_free (a->deps [i]);
    }
    free (a);
    return NULL;
}

void arts_free (artifact * array [], unsigned int size)
{
    for (int i=0; i<size; i++)
        if (array [i])
            art_free (array [i]);
}

static int art_gen_id (char * buf, unsigned int buf_size, const char * first, const char * sig)
{
    char hash [48];

    if (hexjenkins (hash, sizeof (hash), sig) != OK)
        return ERROR;
    
    if (snprintf (buf, buf_size, "%s-%s", first, hash) >= buf_size) // truncation
        return ERROR;

    return OK;
}

#define IGNORED 0 // special value to indicate boolean params that will be ignored
__thread char * instanceId = "";

static artifact * art_alloc (char * id, char * sig, long long size_bytes, boolean may_be_cached, boolean must_be_file, int (* creator) (artifact * a), virtualBootRecord * vbr)
{
    artifact * a = calloc (1, sizeof (artifact));
    if (a==NULL)
        return NULL;

    static int seq = 0;
    a->seq = ++seq; // not thread safe, but seq's are just for debugging
    logprintfl (EUCADEBUG, "allocated artifact %03d id=%s size=%lld vbr=%u cache=%d file=%d\n", seq, id, size_bytes, vbr, may_be_cached, must_be_file);

    if (id)
        strncpy (a->id, id, sizeof (a->id));
    if (sig)
        strncpy (a->sig, sig, sizeof (a->sig));
    a->size_bytes = size_bytes;
    a->may_be_cached = may_be_cached;
    a->must_be_file = must_be_file;
    a->creator = creator;
    a->vbr = vbr;

    strncpy (a->instanceId, instanceId, sizeof (a->instanceId)); // for logging
    return a;
}

static artifact * art_alloc_vbr (virtualBootRecord * vbr, boolean make_work_copy, boolean allow_block_dev) 
{
    artifact * a = NULL;

    switch (vbr->locationType) {
    case NC_LOCATION_URL:
    case NC_LOCATION_CLC: 
    case NC_LOCATION_SC:
    case NC_LOCATION_AOE:
        logprintfl (EUCAERROR, "error: backing of type %s is NOT IMPLEMENTED\n", vbr->typeName);
        return NULL;

    case NC_LOCATION_WALRUS: {
        // get the digest for size and signature
        char * blob_sig = walrus_get_digest (vbr->preparedResourceLocation);
        if (blob_sig==NULL) goto w_out;

        // extract size from the digest
        long long bb_size_bytes = str2longlong (blob_sig, "<size>", "</size>"); // pull size from the digest
        if (bb_size_bytes < 1) goto w_out;
        vbr->size = bb_size_bytes; // record size now that we know it

        // generate ID of the artifact (append -##### hash of sig)
        char art_id [48];
        if (art_gen_id (art_id, sizeof(art_id), vbr->id, blob_sig) != OK) goto w_out;

        // allocate artifact struct
        a = art_alloc (art_id, blob_sig, bb_size_bytes, TRUE, FALSE, walrus_creator, vbr);

        w_out:
        
        if (blob_sig)
            free (blob_sig);
        break;
    }        

    case NC_LOCATION_IQN: {
        assert (!make_work_copy);
        assert (allow_block_dev);
        a = art_alloc (NULL, NULL, -1, IGNORED, IGNORED, iqn_creator, vbr);
        break;
    }

    case NC_LOCATION_NONE: {
        assert (vbr->size > 0L);

        char art_sig [ART_SIG_MAX]; // signature for this artifact based on its salient characteristics
        if (snprintf (art_sig, sizeof (art_sig), "id=%s size=%lld format=%s", 
                      vbr->id, vbr->size, vbr->formatName) >= sizeof (art_sig)) // output was truncated
            break;

        char buf [32]; // first part of artifact ID
        char * art_pref;
        if (strcmp (vbr->id, "none")==0) {
            if (snprintf (buf, sizeof (buf), "part-%05lld%s", 
                          vbr->size/1048576, vbr->formatName) >= sizeof (buf)) // output was truncated
                break;
            art_pref = buf;
        } else {
            art_pref = vbr->id;
        }

        char art_id [48]; // ID of the artifact (append -##### hash of sig)
        if (art_gen_id (art_id, sizeof(art_id), art_pref, art_sig) != OK) 
            break;

        a = art_alloc (art_id, art_sig, vbr->size, TRUE, FALSE, partition_creator, vbr);
        break;
    }
    default:
        logprintfl (EUCAERROR, "error: unrecognized locationType %d\n", vbr->locationType);
    }

    // allocate another artifact struct if a work copy is requested
    if (a && make_work_copy) {
        artifact * a2 = art_alloc (a->id, a->sig, a->size_bytes, FALSE, !allow_block_dev, copy_creator, a->vbr);
        if (a2) {
            if (art_add_dep (a2, a) == OK) {
                a = a2;
            } else {
                art_free (a2);
                a = art_free (a);
            }
        } else {
            a = art_free (a);
        }
    }
    
    return a;
}

static artifact * art_alloc_disk (artifact * prereqs [], int num_prereqs, artifact * parts [], int num_parts, boolean make_bootable, const char * sshkey)
{
    char art_sig [ART_SIG_MAX] = ""; 
    char art_pref [EUCA_MAX_PATH] = "disk";
    long long disk_size_bytes = 512LL * MBR_BLOCKS;
    assert (num_parts);

    // run through partitions, adding up their signatures and their size
    for (int i = 0; i<num_parts; i++) {
        artifact * p = parts [i];
        
        // construct signature for the disk, based on the sigs of underlying components
        char part_sig [ART_SIG_MAX];
        if ((snprintf (part_sig, sizeof(part_sig), "PARTITION %d (%s)\n%s\n\n", 
                       i, p->id, p->sig) >= sizeof (part_sig)) // output truncated
            ||
            ((strlen (art_sig) + strlen (part_sig)) >= sizeof (art_sig))) { // overflow
            logprintfl (EUCAERROR, "error: internal buffers (ART_SI_MAX) too small for signature\n");
            return NULL;
        }
        strncat (art_sig, part_sig, sizeof (art_sig) - strlen (art_sig) - 1);

        // verify and add up the sizes of partitions
        if (p->size_bytes < 1) {
            logprintfl (EUCAERROR, "error: unknown size for partition %d\n", i);
            return NULL;
        }
        if (p->size_bytes % 512) {
            logprintfl (EUCAERROR, "error: size for partition %d is not a multiple of 512\n", i);
            return NULL;
        }
        disk_size_bytes += p->size_bytes;
        
        // look for the emi-XXXX-YYYY ID that the disk is based on
        // and construct disk-XXXX ID out of it
        if (strcasestr (p->id, "emi-") == p->id) {
            char * src = p->id + 4;  // position aftter 'emi'
            char * dst = art_pref + strlen (art_pref); // position after 'disk'
            * dst++ = '-';
            while ((*src>='0') && (*src<='z') // copy letters and numbers up to a hyphen
                   && (dst-art_pref < sizeof (art_pref))) { // don't overrun dst
                * dst++ = * src++;
            }
            * dst = '\0';
        }
    }
    
    // run through prerequisites (kernel and ramdisk), if any, adding up their signature
    for (int i = 0; i<num_prereqs; i++) {
        artifact * p = prereqs [i];
        
        // construct signature for the disk, based on the sigs of underlying components
        char part_sig [ART_SIG_MAX];
        if ((snprintf (part_sig, sizeof(part_sig), "PREREQUISITE %s\n%s\n\n", 
                       p->id, p->sig) >= sizeof (part_sig)) // output truncated
            ||
            ((strlen (art_sig) + strlen (part_sig)) >= sizeof (art_sig))) { // overflow
            logprintfl (EUCAERROR, "error: internal buffers (ART_SI_MAX) too small for signature\n");
            return NULL;
        }
        strncat (art_sig, part_sig, sizeof (art_sig) - strlen (art_sig) - 1);
    }

    artifact * disk;

    { // allocate the 'raw' disk artifact
        char art_id [48]; // ID of the artifact (append -##### hash of sig)
        if (art_gen_id (art_id, sizeof(art_id), art_pref, art_sig) != OK) 
            return NULL;
        
        disk = art_alloc (art_id, art_sig, disk_size_bytes, TRUE, FALSE, disk_creator, NULL);
        if (disk==NULL) {
            logprintfl (EUCAERROR, "error: failed to allocate an artifact for raw disk\n");
            return NULL;
        }
        disk->make_bootable = make_bootable;

        // attach prereqs and partitions as dependencies of the raw disk
        for (int i = 0; i<num_prereqs; i++) {
            artifact * p = prereqs [i];
            if (art_add_dep (disk, p) != OK) {
                logprintfl (EUCAERROR, "error: failed to add a prerequisite to an artifact\n");
                goto free;
            }
        }
        for (int i = 0; i<num_parts; i++) {
            artifact * p = parts [i];
            if (art_add_dep (disk, p) != OK) {
                logprintfl (EUCAERROR, "error: failed to add dependency to an artifact\n");
                goto free;
            }
        }
    }
    
    { // allocate the 'keyed' disk artifact

        // construct signature for the 'keyed' disk by appending the SSH key to 'raw' disk
        if (sshkey==NULL)
            sshkey = "";

        char key_sig [ART_SIG_MAX];
        if ((snprintf (key_sig, sizeof(key_sig), "KEY /root/.ssh/authorized_keys\n%s\n\n", 
                       sshkey) >= sizeof (key_sig)) // output truncated
            ||
            ((strlen (art_sig) + strlen (key_sig)) >= sizeof (art_sig))) { // overflow
            logprintfl (EUCAERROR, "error: internal buffers (ART_SI_MAX) too small for signature\n");
            goto free;
        }
        strncat (art_sig, key_sig, sizeof (art_sig) - strlen (key_sig) - 1);

        char art_id [48]; // ID of the artifact (append -##### hash of sig)
        if (art_gen_id (art_id, sizeof(art_id), art_pref, art_sig) != OK) {
            goto free;
        }
        
        artifact * keyed_disk = art_alloc (art_id, art_sig, disk_size_bytes, FALSE, FALSE, keyed_disk_creator, NULL);
        if (keyed_disk==NULL) {
            logprintfl (EUCAERROR, "error: failed to allocate an artifact for keyed disk\n");
            goto free;
        }
        
        if (art_add_dep (keyed_disk, disk) != OK) {
            logprintfl (EUCAERROR, "error: failed to add dependency to an artifact\n");
            art_free (keyed_disk);
            goto free;
        }

        disk = keyed_disk;
    }

    return disk;
 free:
    art_free (disk);
    return NULL;
}

artifact * // returns pointer to the root of artifact tree or NULL on error
vbr_alloc_tree ( // creates a tree of artifacts for a given VBR (caller must free the tree)
                virtualMachine * vm, // virtual machine containing the VBR
                boolean make_bootable,
                const char * sshkey,
                const char * instanceId)
{
    // sort vbrs into prereq [] and parts[] so they can be approached in the right order
    virtualBootRecord * prereq [EUCA_MAX_VBRS];
    int total_prereqs = 0;
    virtualBootRecord * parts  [BUS_TYPES_TOTAL][EUCA_MAX_DISKS][EUCA_MAX_PARTITIONS];
    int total_parts = 0;
    bzero (parts, sizeof (parts));
    for (int i=0; i<EUCA_MAX_VBRS && i<vm->virtualBootRecordLen; i++) {
        virtualBootRecord * vbr = &(vm->virtualBootRecord[i]);
        if (vbr->type==NC_RESOURCE_KERNEL || vbr->type==NC_RESOURCE_RAMDISK) {
            prereq [total_prereqs++] = vbr;
        } else {
            parts [vbr->guestDeviceBus][vbr->diskNumber][vbr->partitionNumber] = vbr;
            total_parts++;
        }
    }
    logprintfl (EUCADEBUG, "found %d prereqs and %d partitions in the VBR\n", total_prereqs, total_parts);

    artifact * root = art_alloc (NULL, NULL, -1, IGNORED, IGNORED, NULL, NULL); // allocate a sentinel artifact
    if (root == NULL)
        return NULL;
    
    // first attach the prerequisites
    artifact * prereq_arts [EUCA_MAX_VBRS];
    for (int i=0; i<total_prereqs; i++) {
        virtualBootRecord * vbr = prereq [i];
        artifact * dep = art_alloc_vbr (vbr, TRUE, FALSE);
        if (dep == NULL) 
            goto free;
        if (art_add_dep (root, dep) != OK)
            goto free;
        prereq_arts [i] = dep;
    }
    
    // then attach disks and partitions
    for (int i=0; i<BUS_TYPES_TOTAL; i++) { 
        for (int j=0; j<EUCA_MAX_DISKS; j++) {
            int partitions = 0;
            artifact * disk_arts [EUCA_MAX_PARTITIONS];
            bzero (disk_arts, sizeof (disk_arts));
            for (int k=0; k<EUCA_MAX_PARTITIONS; k++) {
                virtualBootRecord * vbr = parts [i][j][k];
                if (vbr) { // either a disk (k==0) or a partition (k>0)
                    disk_arts [k] = art_alloc_vbr (vbr, FALSE, TRUE);
                    if (disk_arts [k] == NULL) {
                        arts_free (disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }
                    if (k>0)
                        partitions++; 
                    
                } else if (partitions) { // there were partitions and we saw them all
                    assert (disk_arts [0] == NULL);
                    disk_arts [0] = art_alloc_disk (prereq_arts, total_prereqs, disk_arts + 1, partitions, make_bootable, sshkey);
                    if (disk_arts [0] == NULL) {
                        arts_free (disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }
                    break; // out of the inner loop
                }
            }
            
            // run though all disk artifacts and either add the disk or all the partitions to sentinel
            for (int k=0; k<EUCA_MAX_PARTITIONS; k++) {
                if (disk_arts [k]) {
                    if (art_add_dep (root, disk_arts [k]) != OK) {
                        arts_free (disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }
                    if (k==0) // if there is a disk, then partition artifacts, if any, are already attached to it
                        break;
                }
            }
        }
    }
    goto out;
    
 free:
    art_free (root);

 out:
    return root;
}

#define FIND_BLOB_TIMEOUT_USEC 0

static int // returns OK or BLOBSTORE_ERROR_ error codes
find_or_create_blob ( // either opens a blockblob or creates it
                     int do_create, // create if non-zero, open if 0
                     blobstore * bs, // the blobstore in which to open/create blockblob
                     const char * id, // id of the blockblob
                     long long size_bytes, // size of the blockblob
                     const char * sig, // signature of the blockblob
                     blockblob ** bbp) // RESULT: opened blockblob handle or NULL if ERROR is returned
{
    blockblob * bb = NULL;
    int ret = OK;
    
    int flags = 0; // for opening
    if (do_create) {
        flags = BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL; // for creating
    }
    // open with a short timeout (0-1000 usec), as we do not want to block 
    // here - we let higher-level functions do retries if necessary
    bb = blockblob_open (bs, id, size_bytes, flags, sig, FIND_BLOB_TIMEOUT_USEC);
    if (bb) { // success!
        * bbp = bb;
    } else {
        ret = blobstore_get_error();
    }

    return ret;
}

#define FIND 0
#define CREATE 1

static int // returns OK or BLOBSTORE_ERROR_ error codes
find_or_create_artifact ( // finds and opens or creates artifact's blob either in cache or in work blobstore
                         int do_create, // create if non-zero, open if 0
                         const artifact * a, // artifact to create or open
                         blobstore * work_bs, // work blobstore 
                         blobstore * cache_bs, // OPTIONAL cache blobstore
                         const char * work_prefix, // OPTIONAL instance-specific prefix for forming work blob IDs
                         blockblob ** bbp) // RESULT: opened blockblob handle or NULL if ERROR is returned
{
    int ret = ERROR;
    assert (a);
    assert (work_bs);

    // determine blob IDs for cache and work
    const char * id_cache = a->id;
    char id_work  [BLOBSTORE_MAX_PATH];
    if (work_prefix && strlen (work_prefix))
        snprintf (id_work, sizeof (id_work), "%s/%s", work_prefix, a->id);
    else 
        strncpy (id_work, a->id, sizeof (id_work));
    
    // first, try cache as long as we're allowed to and have one
    if (a->may_be_cached && cache_bs) {
        ret = find_or_create_blob (do_create, cache_bs, id_cache, a->size_bytes, a->sig, bbp);

        // for some error conditions from cache we try work blobstore
        if (( do_create && ret==BLOBSTORE_ERROR_NOSPC) ||
            (!do_create && ret==BLOBSTORE_ERROR_NOENT) ||
            (!do_create && ret==BLOBSTORE_ERROR_SIGNATURE) ||


            
            // TODO: remove these!
            ret==BLOBSTORE_ERROR_NOENT ||
            ret==BLOBSTORE_ERROR_AGAIN ||
            ret==BLOBSTORE_ERROR_EXIST



            ) {
            goto try_work;
        } else { // for all others we return the error or success
            return ret;
        }
    }
 try_work:
    if (ret==BLOBSTORE_ERROR_SIGNATURE) {
        logprintfl (EUCAWARN, "warning: signature mismatch on cached blob %s\n", id_cache); // TODO: maybe invalidate?
    }
    return find_or_create_blob (do_create, work_bs, id_work, a->size_bytes, a->sig, bbp);
}


int // returns OK or BLOBSTORE_ERROR_ error codes
art_implement_tree ( // traverse artifact tree and create/download/combine artifacts
                    artifact * root, // root of the tree
                    blobstore * work_bs, // work blobstore 
                    blobstore * cache_bs, // OPTIONAL cache blobstore
                    const char * work_prefix, // OPTIONAL instance-specific prefix for forming work blob IDs
                    long long timeout_usec) // timeout for the whole process, in microseconds or 0 for no timeout
{
    long long started = time_usec();
    assert (root);
    assert (work_bs);

    int ret;
    int tries = 0;
    do { // we may have to retry multiple times due to competition
        int num_opened_deps = 0;
        boolean do_deps = TRUE;
        boolean do_create = TRUE;

        if (tries++)
            usleep (100); 

        if (!root->creator) // sentinel nodes do not have a creator
            do_create = FALSE;
        
        if (strlen (root->id)) {
            assert (root->creator);

            // try to open the artifact
            switch (ret = find_or_create_artifact (FIND, root, work_bs, cache_bs, work_prefix, &(root->bb))) {
            case OK:
                logprintfl (EUCADEBUG, "found an existing artifact %03d/%s\n", root->seq, root->id);
                do_deps = FALSE;
                do_create = FALSE;
                break;
            case BLOBSTORE_ERROR_NOENT: // doesn't exist yet => create it
                break; 
            case BLOBSTORE_ERROR_AGAIN: // timed out
                goto out;
                break;
            default: // all other errors
                logprintfl (EUCAERROR, "failed to provision artifact %s\n", root->id);
                goto out;
            }
        }
        
        if (do_deps) { // recursively go over dependencies, if any
            for (int i = 0; i < MAX_ARTIFACT_DEPS && root->deps [i]; i++) {
                switch (ret=art_implement_tree (root->deps [i], work_bs, cache_bs, work_prefix, 0)) {
                case OK:
                    if (do_create) { // we'll hold the dependency open for the creator
                        num_opened_deps++;
                    } else { // this is a sentinel, we're not creating anything, so release the dep immediately
                        blockblob_close (root->deps [i]->bb);
                    }
                    break; // out of the switch statement
                case BLOBSTORE_ERROR_AGAIN: // timed out
                    goto out;
                default: // all other errors
                    logprintfl (EUCAERROR, "failed to provision dependency %s for artifact %s\n", root->deps [i]->id, root->id);
                    goto out;
                }
            }
        }
        
        if (do_create) {
            // try to create the artifact since last time we checked it did not exist
            switch (ret=find_or_create_artifact (CREATE, root, work_bs, cache_bs, work_prefix, &(root->bb))) {
            case OK:
                logprintfl (EUCADEBUG, "created a blob for an artifact %03d/%s\n", root->seq, root->id);
                break;
            case BLOBSTORE_ERROR_EXIST: // someone else created it => loop back and open it
                ret = BLOBSTORE_ERROR_AGAIN;
                // fall through
            case BLOBSTORE_ERROR_AGAIN: // timed out (but probably exists)
                goto out;
                break;
            default: // all other errors
                logprintfl (EUCAERROR, "failed to create artifact %s (error=%d)\n", root->id, ret);
                goto out;
            }

            ret = root->creator (root);
        }

    out:
        // close all opened blobs, whether we're trying again or giving up
        for (int i=0; i<num_opened_deps; i++) {
            blockblob_close (root->deps [i]->bb);
        }
        
    } while (ret==BLOBSTORE_ERROR_AGAIN && (time_usec()-started)>timeout_usec);
    
    return ret;
}

/////////////////////////////////////////////// unit testing code ///////////////////////////////////////////////////

#ifdef _UNIT_TEST

static blobstore * cache_bs;
static blobstore * work_bs;

static blobstore * create_teststore (int size_blocks, const char * base, const char * name, blobstore_format_t format, blobstore_revocation_t revocation, blobstore_snapshot_t snapshot)
{
    static int ts = 0;
    if (ts==0) {
        ts = ((int)time(NULL))-1292630988;
    }

    char bs_path [PATH_MAX];
    snprintf (bs_path, sizeof (bs_path), "%s/test_vbr_%05d_%s", base, ts, name);
    if (mkdir (bs_path, 0777) == -1) {
        printf ("failed to create %s\n", bs_path);
        return NULL;
    }
    printf ("created %s\n", bs_path);
    blobstore * bs = blobstore_open (bs_path, size_blocks, format, revocation, snapshot);
    if (bs==NULL) {
        printf ("ERROR: %s\n", blobstore_get_error_str(blobstore_get_error()));
        return NULL;
    }
    return bs;
}

// this function sets the fields in a VBR that are required for artifact processing
static void add_vbr (virtualMachine * vm,
                     long long size,
                     ncResourceFormatType format,
                     char * formatName,
                     const char * id,
                     ncResourceType type, 
                     ncResourceLocationType locationType,
                     int diskNumber,
                     int partitionNumber,
                     libvirtBusType guestDeviceBus,
                     char * preparedResourceLocation)
{
    virtualBootRecord * vbr = vm->virtualBootRecord + vm->virtualBootRecordLen++;
    vbr->size = size;
    if (formatName)
        strncpy (vbr->formatName, formatName, sizeof (vbr->formatName));
    if (id)
        strncpy (vbr->id, id, sizeof (vbr->id));
    vbr->format = format;
    vbr->type = type;
    vbr->locationType = locationType;
    vbr->diskNumber = diskNumber;
    vbr->partitionNumber = partitionNumber;
    vbr->guestDeviceBus = guestDeviceBus;
    if (preparedResourceLocation)
        strncpy (vbr->preparedResourceLocation, preparedResourceLocation, sizeof (vbr->preparedResourceLocation));
}

static volatile int provisioned_instances = 0;
static pthread_mutex_t competitors_mutex = PTHREAD_MUTEX_INITIALIZER; // process-global mutex

static int provision (const char * id, const char * sshkey, const char * eki, const char * eri, const char * emi, blobstore * cache_bs, blobstore * work_bs)
{
    virtualMachine vm;
    bzero   (&vm, sizeof (vm));

    add_vbr (&vm, MEGABYTE * 2L, NC_FORMAT_NONE, "none", eki,    NC_RESOURCE_KERNEL,    NC_LOCATION_NONE, 0, 0, 0, NULL);
    add_vbr (&vm, MEGABYTE * 2L, NC_FORMAT_NONE, "none", eri,    NC_RESOURCE_RAMDISK,   NC_LOCATION_NONE, 0, 0, 0, NULL);
    add_vbr (&vm, MEGABYTE * 2L, NC_FORMAT_NONE, "none", emi,    NC_RESOURCE_IMAGE,     NC_LOCATION_NONE, 0, 1, BUS_TYPE_SCSI, NULL);
    add_vbr (&vm, MEGABYTE * 2L, NC_FORMAT_EXT3, "ext3", "none", NC_RESOURCE_EPHEMERAL, NC_LOCATION_NONE, 0, 3, BUS_TYPE_SCSI, NULL);
    add_vbr (&vm, MEGABYTE * 2L, NC_FORMAT_SWAP, "swap", "none", NC_RESOURCE_SWAP,      NC_LOCATION_NONE, 0, 2, BUS_TYPE_SCSI, NULL);

    instanceId = strstr (id, "/") + 1;
    artifact * sentinel = vbr_alloc_tree (&vm, FALSE, sshkey, id);
    if (sentinel == NULL) {
        return 1;
    }
    printf ("allocated artifact tree sentinel=%012lx\n", (unsigned long)sentinel);
    if (art_implement_tree (sentinel, work_bs, cache_bs, id, 1000000LL * 60 * 2) != OK)
        return 1;

    pthread_mutex_lock (&competitors_mutex);
    provisioned_instances++;
    pthread_mutex_unlock (&competitors_mutex);

    return 0;
}

static char * gen_id (char * id, unsigned int size, const char * prefix)
{
    snprintf (id, size, "%s/i-%08x", prefix, rand());
    return id;
}

#define BS_SIZE 20000000000/512
#define KEY1 "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCVWU+h3gDF4sGjUB7t...\n"
#define EKI1 "eki-1ABC123"
#define ERI1 "eri-1BCD234"
#define EMI1 "emi-1CDE345"
#define EMI2 "emi-2DEF456"
#define GEN_ID gen_id (id, sizeof(id), "12345678")
#define COMPETITIVE_PARTICIPANTS 3
#define COMPETITIVE_ITERATIONS 30

static void * competitor_function (void * ptr)
{
    int errors = 0;

    printf ("%u/%u: competitor running (provisioned=%d)\n", (unsigned int)pthread_self(), (int)getpid(), provisioned_instances);
    
    for (int i=0; i<COMPETITIVE_ITERATIONS; i++) {
        char id [32];
        errors += provision (GEN_ID, KEY1, EKI1, ERI1, EMI2, cache_bs, work_bs);
        usleep ((long long)(100*((double)random()/RAND_MAX)));
    }

    printf ("%u/%u: competitor done (provisioned=%d errors=%d)\n", (unsigned int)pthread_self(), (int)getpid(), provisioned_instances, errors);
    
    * (long long *) ptr = errors;
    return NULL;
}

// check if the blobstore has the expected number of 'block' entries
static int check_blob (blobstore * bs, const char * keyword, int expect) 
{
    char cmd [1024];
    snprintf (cmd, sizeof (cmd), "find %s | grep %s | wc -l", bs->path, keyword);
    FILE * f = popen (cmd, "r");
    if (! f) {
        printf ("error: failed to popen() command '%s'\n", cmd);
        perror ("test_vbr");
        return 1;
    }

    char buf [32];
    int bytes;
    if ((bytes = fread (buf, 1, sizeof (buf) - 1, f)) < 1) {
        printf ("error: failed to fread() from output of '%s' (returned %d)\n", cmd, bytes);
        perror ("test_vbr");
        return 1;
    }
    buf [bytes] = '\0';

    if (pclose (f)) {
        printf ("error: failed pclose()\n");
        perror ("test_vbr");
        return 1;
    }

    int found = atoi (buf);
    if (found != expect) {
        printf ("error: unexpected disk state: [%s] = %d != %d\n", cmd, found, expect);
        return 1;
    }
    return 0;
}

static void dummy_err_fn (const char * msg) { }

int main (int argc, char ** argv)
{
    char id [32];
    int errors = 0;
    char cwd [1024];
    getcwd (cwd, sizeof (cwd));
    srandom (time(NULL));
    blobstore_set_error_function (dummy_err_fn);    

    printf ("testing vbr.c\n");

    cache_bs = create_teststore (BS_SIZE, cwd, "cache", BLOBSTORE_FORMAT_DIRECTORY, BLOBSTORE_REVOCATION_LRU, BLOBSTORE_SNAPSHOT_ANY);
    work_bs  = create_teststore (BS_SIZE, cwd, "work", BLOBSTORE_FORMAT_FILES, BLOBSTORE_REVOCATION_NONE, BLOBSTORE_SNAPSHOT_ANY);
    
    int emis_in_use = 1;
    if (errors += provision (GEN_ID, KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs))
        goto out;
#define CHECK_BLOBS \
    if (errors += check_blob (cache_bs, "blocks", 4 + 2 * emis_in_use)) goto out;  \
    if (errors += check_blob (work_bs, "blocks", 3 * provisioned_instances)) goto out
    CHECK_BLOBS;
    
    for (int i=0; i<5; i++) {
        errors += provision (GEN_ID, KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs);
    }
    if (errors) {
        printf ("error: failed sequential instance provisioning test\n");
    }
    CHECK_BLOBS;
    
    printf ("spawning %d competing threads\n", COMPETITIVE_PARTICIPANTS);
    emis_in_use ++; // we'll have threads creating a new EMI
    pthread_t threads [COMPETITIVE_PARTICIPANTS];
    long long thread_par [COMPETITIVE_PARTICIPANTS];
    int thread_par_sum = 0;
    for (int j=0; j<COMPETITIVE_PARTICIPANTS; j++) {
        pthread_create (&threads[j], NULL, competitor_function, (void *)&thread_par[j]);
    }
    for (int j=0; j<COMPETITIVE_PARTICIPANTS; j++) {
        pthread_join (threads[j], NULL);
        thread_par_sum += (int)thread_par [j];
    }
    printf ("waited for all competing threads (returned sum=%d)\n", thread_par_sum);
    if (errors += thread_par_sum) {
        printf ("error: failed parallel instance provisioning test\n");
    }
    sleep (1);
    CHECK_BLOBS;

out:
    _exit(errors);
}

#endif
