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

static int vbr_creator (artifact * a)
{
    return OK;
}

static int disk_creator (artifact * a)
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

static int art_gen_id (char * buf, unsigned int buf_size, const char * first, const char * sig)
{
    char md5str [32];

    if (str2md5str (md5str, sizeof (md5str), sig) != OK)
        return ERROR;
    
    if (snprintf (buf, buf_size, "%s-%s", first, md5str) >= buf_size) // truncation
        return ERROR;

    return OK;
}

#define IGNORED 0 // special value to indicate boolean params that will be ignored

static artifact * art_alloc (char * id, char * sig, long long size_bytes, boolean may_be_cached, boolean must_be_file, int (* creator) (artifact * a), virtualBootRecord * vbr)
{
    artifact * a = calloc (1, sizeof (artifact));
    if (a==NULL)
        return NULL;
    if (id)
        strncpy (a->id, id, sizeof (a->id));
    if (sig)
        strncpy (a->sig, sig, sizeof (a->sig));
    a->size_bytes = size_bytes;
    a->may_be_cached = may_be_cached;
    a->must_be_file = must_be_file;
    a->creator = creator;
    a->vbr = vbr;

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

        // generate ID of the artifact (append -##### MD5 hash of sig)
        char art_id [48];
        if (art_gen_id (art_id, sizeof(art_id), vbr->id, blob_sig) != OK) goto w_out;

        // allocate artifact struct
        a = art_alloc (art_id, blob_sig, bb_size_bytes, TRUE, FALSE, vbr_creator, vbr);

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
        if (snprintf (art_sig, sizeof (art_sig), "%lld/%s", 
                      vbr->size, vbr->formatName) >= sizeof (art_sig)) // output was truncated
            break;

        char art_pref [32]; // first part of artifact ID
        if (snprintf (art_pref, sizeof (art_pref), "part-%05lld%s", 
                      vbr->size/1048576, vbr->formatName) >= sizeof (art_pref)) // output was truncated
            break;

        char art_id [48]; // ID of the artifact (append -##### MD5 hash of sig)
        if (art_gen_id (art_id, sizeof(art_id), art_pref, art_sig) != OK) 
            break;

        a = art_alloc (art_id, art_sig, vbr->size, TRUE, FALSE, vbr_creator, vbr);
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

static artifact * art_alloc_disk (artifact * partitions [], int num_partitions)
{
    char art_sig [ART_SIG_MAX] = ""; 
    char art_pref [EUCA_MAX_PATH] = "disk-";
    long long disk_size_bytes = 512LL * MBR_BLOCKS;
    assert (num_partitions);

    for (int i = 0; i<num_partitions; i++) {
        artifact * p = partitions [i];
        
        // construct signature for the disk, based on the sigs of underlying components
        char part_sig [ART_SIG_MAX];
        if ((snprintf (part_sig, sizeof(part_sig), "PARTITION %d\n%s\n\n", 
                       i, p->sig) >= sizeof (part_sig)) // output truncated
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
            char * src = p->id + 4;  // position aftter 'emi-'
            char * dst = art_pref + strlen (art_pref); // position after 'disk-'
            while ((*src>='0') && (*src<='z') // copy letters and numbers up to a hyphen
                   && (dst-art_pref < sizeof (art_pref))) { // don't overrun dst
                * dst++ = * src++;
            }
            * dst = '\0';
        }
    }
        
    char art_id [48]; // ID of the artifact (append -##### MD5 hash of sig)
    if (art_gen_id (art_id, sizeof(art_id), art_pref, art_sig) != OK) 
        return NULL;
    
    artifact * bootable_disk = art_alloc (art_id, art_sig, disk_size_bytes, TRUE, FALSE, disk_creator, NULL);
    // TODO: create keyed_disk
}

artifact * // returns pointer to the root of artifact tree or NULL on error
vbr_alloc_tree ( // creates a tree of artifacts for a given VBR (caller must free the tree)
                virtualMachine * vm) // virtual machine containing the VBR
{
    // sort vbrs into prereq [] and parts[] so they can be approached in the right order
    virtualBootRecord * prereq [EUCA_MAX_VBRS];
    int total_prereqs = 0;
    virtualBootRecord * parts  [BUS_TYPES_TOTAL][EUCA_MAX_DISKS][EUCA_MAX_PARTITIONS];
    bzero (parts, sizeof (parts));
    for (int i=0; i<EUCA_MAX_VBRS && i<vm->virtualBootRecordLen; i++) {
        virtualBootRecord * vbr = &(vm->virtualBootRecord[i]);
        if (vbr->type==NC_RESOURCE_KERNEL || vbr->type==NC_RESOURCE_RAMDISK) {
            prereq [total_prereqs++] = vbr;
        } else {
            parts [vbr->guestDeviceBus][vbr->diskNumber][vbr->partitionNumber] = vbr;
        }
    }
 
    artifact * root = art_alloc (NULL, NULL, -1, IGNORED, IGNORED, NULL, NULL); // allocate a sentinel artifact
    if (root == NULL)
        return NULL;
    
    // first attach the prerequisites
    for (int i=0; i<total_prereqs; i++) {
        virtualBootRecord * vbr = prereq [i];
        artifact * dep = art_alloc_vbr (vbr, TRUE, FALSE);
        if (dep == NULL) 
            goto free;
        if (art_add_dep (root, dep) != OK)
            goto free;
    }
    
    // then attach disks and partitions
    for (int i=0; i<BUS_TYPES_TOTAL; i++) { 
        for (int j=0; j<EUCA_MAX_DISKS; j++) {
            int partitions = 0;
            artifact * disk_arts [EUCA_MAX_PARTITIONS];
            for (int k=0; k<EUCA_MAX_PARTITIONS; k++) {
                virtualBootRecord * vbr = parts [i][j][k];
                if (vbr) { // either a disk (k==0) or a partition (k>0)
                    disk_arts [k] = art_alloc_vbr (vbr, FALSE, TRUE);
                    if (disk_arts [k] == NULL)
                        goto free;
                    if (k>0)
                        partitions++; 
                    
                } else if (partitions) { // there were partitions and we saw them all
                    assert (disk_arts [0] == NULL);
                    disk_arts [0] = art_alloc_disk (disk_arts + 1, partitions);
                    if (disk_arts [0] == NULL) {
                        goto free;
                    }
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

#define STORE_TIMEOUT_USEC 1000000LL*60*2

static int // returns READER, WRITER, or ERROR
find_or_create_blob ( // either opens a blockblob as a reader or, failing that, creates it
                     blobstore * bs, // the blobstore in which to open/create blockblob
                     const char * id, // id of the blockblob
                     long long size_bytes, // size of the blockblob
                     const char * sig, // signature of the blockblob
                     blockblob ** bbp) // RESULT: opened blockblob handle or NULL if ERROR is returned
{
    long long started = time_usec();
    blockblob * bb = NULL;
    int ret = ERROR;
    
    int flags = 0; // start as a reader
    long long timeout = STORE_TIMEOUT_USEC; // with a long timeout
    do {
        bb = blockblob_open (bs, id, size_bytes, flags, sig, timeout);
        if (bb) { // success!
            if (flags) {
                ret = WRITER;
            } else {
                ret = READER;
            }
            break;
        }
        
        // open failed
        int err = blobstore_get_error();
        
        if (err==BLOBSTORE_ERROR_NOENT) { // entry does not exist
            flags = BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL; // try creating
            timeout = 0LL;

        } else if (err==BLOBSTORE_ERROR_SIGNATURE) { // wrong signature or length
            
            // open with any signature and delete the old one
            bb = blockblob_open (bs, id, 0, 0, NULL, 0); // TODO: should there be a non-zero timeout?
            if (bb && blockblob_delete (bb, 0) == 0) {
                flags = BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL; // try creating
                bb = NULL; // ok because delete frees the handle
            } else {
                logprintfl (EUCAERROR, "error: failed to delete blob '%s' with non-matching signature\n", id);
                break; // give up
            }

        } else if (err==BLOBSTORE_ERROR_NOSPC) { // no space in the cache
            logprintfl (EUCAWARN, "error: insufficient space for blob '%s'\n", id);
            break; // give up
            
        } else if (err==BLOBSTORE_ERROR_AGAIN) {
            if (timeout>0) { // timed out waiting
                logprintfl (EUCAWARN, "error: timed out waiting for blob '%s'\n", id);
                break; // give up

            } else { // maybe the write timed out, try reading again
                flags = 0; 
                timeout = STORE_TIMEOUT_USEC;
            }                
        } else {
            logprintfl (EUCAWARN, "error; unkown error while preparing artifacts\n");
            break;
        }
        usleep (100); // safety sleep 

    } while ((time_usec()-started) < STORE_TIMEOUT_USEC);
    
    return ret;
}

static int // returns READER, WRITER, or ERROR
find_or_create_artifact ( // finds and opens or creates artifact's blob either in cache or in work blobstore
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
    
    // try cache and then work blobstores
    if (a->may_be_cached && cache_bs) {
        ret = find_or_create_blob (cache_bs, id_cache, a->size_bytes, a->sig, bbp);
    }
    
    if (ret==ERROR) { // we don't care what the error is, I guess
        ret = find_or_create_blob (work_bs, id_work, a->size_bytes, a->sig, bbp);
    }

    return ret;
}


int // returns OK or ERROR
art_implement_tree ( // traverse artifact tree and create/download/combine artifacts
                    artifact * root, // root of the tree
                    blobstore * work_bs, // work blobstore 
                    blobstore * cache_bs, // OPTIONAL cache blobstore
                    const char * work_prefix) // OPTIONAL instance-specific prefix for forming work blob IDs
{
    boolean do_create = TRUE;
    boolean do_deps = TRUE;
    int ret = ERROR;
    assert (root);
    assert (work_bs);

    if (!root->creator) // sentinel nodes do not have a creator
        do_create = FALSE;

    if (root->id) {
        assert (root->creator);
        switch (find_or_create_artifact (root, work_bs, cache_bs, work_prefix, &(root->bb))) {
        case READER:
            do_deps = FALSE;
            do_create = FALSE;
            break;
            
        case WRITER:
            break;

        case ERROR:
            goto out;
        }
    }
    
    int num_opened_deps = 0;
    if (do_deps) { // recursively go over dependencies, if any
        for (int i = 0; i < MAX_ARTIFACT_DEPS && root->deps [i]; i++) {
            if (art_implement_tree (root->deps [i], work_bs, cache_bs, work_prefix) != OK) 
                goto out;
            else 
                num_opened_deps++;
        }
    }
    
    if (do_create) {
        int created = root->creator (root);
        if (root->bb) { // we have blockblob handle
            blockblob_close (root->bb); // close WRITE handle no matter what
            if (created == OK) { // if successfully created, re-open as reader
                switch (find_or_create_artifact (root, work_bs, cache_bs, work_prefix, &(root->bb))) {
                case READER:
                    break;
                case WRITER: // this should not happen
                    logprintfl (EUCAERROR, "error: unknown provisioning error (could not re-open blob as read-only)\n");
                    blockblob_close (root->bb); // close the unexpected writer handle
                    // fall through
                case ERROR:
                    goto out;
                }
            }
        } else {
            if (created != OK)
                goto out;
        }
    }
    
    ret = OK;
 out:
    for (int i=0; i<num_opened_deps; i++) {
        blockblob_close (root->deps [i]->bb);
    }
    
    return ret;
}
