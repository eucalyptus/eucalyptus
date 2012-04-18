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
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
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
#include <sys/wait.h> // waitpid
#include <stdarg.h>
#include <fcntl.h>
#include <errno.h>
#include <limits.h>
#include <assert.h>
#include <dirent.h>

#include "misc.h" // logprintfl, ensure_...
#include "hash.h"
#include "data.h"
#include "vbr.h"
#include "walrus.h"
#include "blobstore.h"
#include "diskutil.h"
#include "iscsi.h"
#include "http.h"

#define VBR_SIZE_SCALING 1024 // TODO: remove this adjustment after CLC sends bytes instead of KBs

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
            snprintf (vbr->preparedResourceLocation, sizeof(vbr->preparedResourceLocation), "%s/%s", service->uris[0], l); // TODO: for now we just pick the first one
            return OK;
        }
    }
    logprintfl (EUCAERROR, "failed to find service '%s' in eucalyptusMessage\n", typeName);
    return ERROR;
}

// parse spec_str as a VBR record and add it to 
// vm_type->virtualBootRecord[virtualBootRecordLen]
// return 0 if OK, return 1 on error
int vbr_add_ascii (const char * spec_str, virtualMachine * vm_type)
{
    if (vm_type->virtualBootRecordLen==EUCA_MAX_VBRS) {
        logprintfl (EUCAERROR, "too many entries in VBR already\n");
        return 1;
    }
    virtualBootRecord * vbr = &(vm_type->virtualBootRecord[vm_type->virtualBootRecordLen++]);
    
    char * spec_copy = strdup (spec_str);
    char * type_spec = strtok (spec_copy, ":");
    char * id_spec = strtok (NULL, ":");
    char * size_spec = strtok (NULL, ":");
    char * format_spec = strtok (NULL, ":");
    char * dev_spec = strtok (NULL, ":");
    char * loc_spec = strtok (NULL, ":");
    if (type_spec==NULL) { logprintfl (EUCAERROR, "error: invalid 'type' specification in VBR '%s'\n", spec_str); goto out_error; }
    safe_strncpy (vbr->typeName, type_spec, sizeof (vbr->typeName));

    if (id_spec==NULL) { logprintfl (EUCAERROR, "error: invalid 'id' specification in VBR '%s'\n", spec_str); goto out_error; }
    safe_strncpy (vbr->id, id_spec, sizeof (vbr->id));

    if (size_spec==NULL) { logprintfl (EUCAERROR, "error: invalid 'size' specification in VBR '%s'\n", spec_str); goto out_error; }
    vbr->size = atoi (size_spec);

    if (format_spec==NULL) { logprintfl (EUCAERROR, "error: invalid 'format' specification in VBR '%s'\n", spec_str); goto out_error; }
    safe_strncpy (vbr->formatName, format_spec, sizeof (vbr->formatName));

    if (dev_spec==NULL) { logprintfl (EUCAERROR, "error: invalid 'guestDeviceName' specification in VBR '%s'\n", spec_str); goto out_error; }
    safe_strncpy (vbr->guestDeviceName, dev_spec, sizeof (vbr->guestDeviceName));

    if (loc_spec==NULL) { logprintfl (EUCAERROR, "error: invalid 'resourceLocation' specification in VBR '%s'\n", spec_str); goto out_error; }
    safe_strncpy (vbr->resourceLocation, spec_str + (loc_spec - spec_copy), sizeof (vbr->resourceLocation));
    
    free (spec_copy);
    return 0;
    
 out_error:
    vm_type->virtualBootRecordLen--;
    free (spec_copy);
    return 1;
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
            vm->root = vbr;
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
    if (strcasestr (vbr->resourceLocation, "http://") == vbr->resourceLocation ||
        strcasestr (vbr->resourceLocation, "https://") == vbr->resourceLocation) { 
        if (strcasestr (vbr->resourceLocation, "/services/Walrus/")) {
            vbr->locationType = NC_LOCATION_WALRUS;
        } else {
            vbr->locationType = NC_LOCATION_URL;
        }
        safe_strncpy (vbr->preparedResourceLocation, vbr->resourceLocation, sizeof(vbr->preparedResourceLocation));
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
            safe_strncpy (buf, vbr->guestDeviceName + 5, sizeof (buf));
            strncpy (vbr->guestDeviceName, buf, sizeof (vbr->guestDeviceName));
        }
        
        if (strlen (vbr->guestDeviceName)<3 ||
            (vbr->guestDeviceName [0] == 'x' && strlen(vbr->guestDeviceName) < 4)) {
            logprintfl (EUCAERROR, "Error: invalid guestDeviceName '%s'\n", vbr->guestDeviceName);
            return ERROR;
        }
        
        {
            char t = vbr->guestDeviceName [0]; // type
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

            int letters_len = 3; // e.g. "sda"
            if (t == 'x') letters_len = 4; // e.g., "xvda"
            if (t == 'f') letters_len = 2; // e.g., "fd0"
            char d = vbr->guestDeviceName [letters_len-2]; // when 3+, the 'd'
            char n = vbr->guestDeviceName [letters_len-1]; // when 3+, the disk number
            if (strlen (vbr->guestDeviceName) > letters_len) {
                long long int p = 0; // partition or floppy drive number
                errno = 0;
                p = strtoll (vbr->guestDeviceName + letters_len, NULL, 10);
                if (errno!=0) { 
                    logprintfl (EUCAERROR, "Error: failed to parse partition number in guestDeviceName '%s'\n", vbr->guestDeviceName);
                    return ERROR; 
                } 
                if (p<0 || p>EUCA_MAX_PARTITIONS) {
                    logprintfl (EUCAERROR, "Error: unexpected partition or disk number '%d' in guestDeviceName '%s'\n", p, vbr->guestDeviceName);
                    return ERROR;
                }
                if (t=='f') {
                    vbr->diskNumber = p;
                } else {
                    if (p<1) {
                        logprintfl (EUCAERROR, "Error: unexpected partition number '%d' in guestDeviceName '%s'\n", p, vbr->guestDeviceName);
                        return ERROR;
                    }
                    vbr->partitionNumber = p;
                }
            } else {
                vbr->partitionNumber = 0;
            }
            
            if (vbr->guestDeviceType != DEV_TYPE_FLOPPY) {
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
    virtualBootRecord * partitions [BUS_TYPES_TOTAL][EUCA_MAX_DISKS][EUCA_MAX_PARTITIONS]; // for validating partitions
    bzero (partitions, sizeof (partitions));
    for (int i=0, j=0; i<EUCA_MAX_VBRS && i<vm->virtualBootRecordLen; i++) {
        virtualBootRecord * vbr = &(vm->virtualBootRecord[i]);

        if (strlen (vbr->typeName) == 0) { // this must be the combined disk's VBR
            return OK;
        }

        if (parse_rec (vbr, vm, meta) != OK)
            return ERROR;
        
        if (vbr->type!=NC_RESOURCE_KERNEL && vbr->type!=NC_RESOURCE_RAMDISK)
            partitions [vbr->guestDeviceBus][vbr->diskNumber][vbr->partitionNumber] = vbr;
        
        if (vm->root==NULL) { // we have not identified the EMI yet
            if (vbr->type==NC_RESOURCE_IMAGE) {
                vm->root=vbr;
            }
        } else {
            if (vm->root!=vbr && vbr->type==NC_RESOURCE_IMAGE) {
                logprintfl (EUCAERROR, "Error: more than one EMI specified in the boot record\n");
                return ERROR;
            }
        }
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
                if (vm->root==NULL) { // root partition or disk have not been found yet (no NC_RESOURCE_IMAGE)
                    virtualBootRecord * vbr;
                    if (has_partitions)
                        vbr = partitions [i][j][1];
                    else
                        vbr = partitions [i][j][0];
                    if (vbr && (vbr->type == NC_RESOURCE_EBS))
                        vm->root = vbr;
                }
            }
        }
    }

    if (vm->root==NULL) {
        logprintfl (EUCAERROR, "Error: no root partition or disk have been found\n");
        return ERROR;
    }

    return OK;
}

int // returns OK or ERROR
vbr_legacy ( // constructs VBRs for {image|kernel|ramdisk}x{Id|URL} entries (DEPRECATED)
            const char * instanceId,
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
            logprintfl (EUCAINFO, "[%s]                VBR[%d] type=%s id=%s dev=%s size=%lld format=%s %s\n", 
                        instanceId, i, vbr->typeName, vbr->id, vbr->guestDeviceName, vbr->size, vbr->formatName, vbr->resourceLocation);
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
            logprintfl (EUCAINFO, "[%s] IGNORING image %s passed outside the virtual boot record\n", instanceId,  imageId);
        } else {
            logprintfl (EUCAINFO, "[%s] LEGACY pre-VBR image id=%s URL=%s\n", instanceId,  imageId, imageURL);
            if (i>=EUCA_MAX_VBRS-2) {
                logprintfl (EUCAERROR, "[%s] error: out of room in the Virtual Boot Record for legacy image %s\n", instanceId,  imageId);
                return ERROR;
            }
            
            { // create root partition VBR
                virtualBootRecord * vbr = &(params->virtualBootRecord[i++]);
                safe_strncpy (vbr->resourceLocation, imageURL, sizeof (vbr->resourceLocation));
                strncpy (vbr->guestDeviceName, "sda1", sizeof (vbr->guestDeviceName));
                safe_strncpy (vbr->id, imageId, sizeof (vbr->id));
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
            logprintfl (EUCAINFO, "[%s] IGNORING kernel %s passed outside the virtual boot record\n", instanceId,  kernelId);
        } else {
            logprintfl (EUCAINFO, "[%s] LEGACY pre-VBR kernel id=%s URL=%s\n", instanceId,  kernelId, kernelURL);
            if (i==EUCA_MAX_VBRS) {
                logprintfl (EUCAERROR, "[%s] error: out of room in the Virtual Boot Record for legacy kernel %s\n", instanceId,  kernelId);
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
            logprintfl (EUCAINFO, "[%s] IGNORING ramdisk %s passed outside the virtual boot record\n", instanceId,  ramdiskId);
        } else {
            logprintfl (EUCAINFO, "[%s] LEGACY pre-VBR ramdisk id=%s URL=%s\n", instanceId,  ramdiskId, ramdiskURL);
            if (i==EUCA_MAX_VBRS) {
                logprintfl (EUCAERROR, "[%s] error: out of room in the Virtual Boot Record for legacy ramdisk %s\n", instanceId,  ramdiskId);
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

static void update_vbr_with_backing_info (artifact * a)
{
    assert (a);
    if (a->vbr==NULL) return;
    virtualBootRecord * vbr = a->vbr;

    assert (a->bb);
    if (! a->must_be_file && strlen (blockblob_get_dev (a->bb))) {
        safe_strncpy (vbr->backingPath, blockblob_get_dev (a->bb), sizeof (vbr->backingPath));
        vbr->backingType = SOURCE_TYPE_BLOCK;
    } else {
        assert (blockblob_get_file (a->bb));
        safe_strncpy (vbr->backingPath, blockblob_get_file (a->bb), sizeof (vbr->backingPath));
        vbr->backingType = SOURCE_TYPE_FILE;
    }
    vbr->size = a->bb->size_bytes;
}

// The following *_creator funcitons produce an artifact in blobstore, 
// either from scratch (such as Walrus download or a new partition) or 
// by converting, combining, and augmenting existing artifacts.  
//
// When invoked, creators can assume that any input blobs and the output 
// blob are open (and thus locked for their exclusive use).
//
// Creators return OK or an error code: either generic one (ERROR) or
// a code specific to a failed blobstore operation, which can be obtained
// using blobstore_get_error().

static int url_creator (artifact * a)
{
    assert (a->bb);
    assert (a->vbr);
    virtualBootRecord * vbr = a->vbr;
    const char * dest_path = blockblob_get_file (a->bb);

    assert (vbr->preparedResourceLocation);
    logprintfl (EUCAINFO, "[%s] downloading %s\n", a->instanceId, vbr->preparedResourceLocation);
    if (http_get (vbr->preparedResourceLocation, dest_path) != OK) {
        logprintfl (EUCAERROR, "[%s] error: failed to download component %s\n", a->instanceId, vbr->preparedResourceLocation);
        return ERROR;
    }

    return OK;
}

static int walrus_creator (artifact * a) // creates an artifact by downloading it from Walrus
{
    assert (a->bb);
    assert (a->vbr);
    virtualBootRecord * vbr = a->vbr;
    const char * dest_path = blockblob_get_file (a->bb);

    assert (vbr->preparedResourceLocation);
    logprintfl (EUCAINFO, "[%s] downloading %s\n", a->instanceId, vbr->preparedResourceLocation);
    if (walrus_image_by_manifest_url (vbr->preparedResourceLocation, dest_path, TRUE) != OK) {
        logprintfl (EUCAERROR, "[%s] error: failed to download component %s\n", a->instanceId, vbr->preparedResourceLocation);
        return ERROR;
    }

    return OK;
}

static int partition_creator (artifact * a) // creates a new partition from scratch
{
    assert (a->bb);
    assert (a->vbr);
    virtualBootRecord * vbr = a->vbr;
    const char * dest_dev = blockblob_get_dev (a->bb);

    assert (dest_dev);
    logprintfl (EUCAINFO, "[%s] creating partition of size %lld bytes and type %s in %s\n", a->instanceId, a->size_bytes, vbr->formatName, a->id);
    int format = ERROR;
    switch (vbr->format) {
    case NC_FORMAT_NONE:
        format = OK;
        break;
    case NC_FORMAT_EXT2: // TODO: distinguish ext2 and ext3!
    case NC_FORMAT_EXT3:
        format = diskutil_mkfs (dest_dev, a->size_bytes);
        break;
    case NC_FORMAT_SWAP:
        format = diskutil_mkswap (dest_dev, a->size_bytes);
        break;
    default:
        logprintfl (EUCAERROR, "[%s] error: format of type %d/%s is NOT IMPLEMENTED\n", a->instanceId, vbr->format, vbr->formatName);
    }
    
    if (format != OK) {
        logprintfl (EUCAERROR, "[%s] failed to create partition in blob %s\n", a->instanceId, a->id);
        return ERROR;
    }

    return OK;
}

// sets vbr->guestDeviceName based on other entries in the struct
// (guestDevice{Type|Bus}, {disk|partition}Number}
static void set_disk_dev (virtualBootRecord * vbr)
{
    char type [3] = "\0\0\0";
    if (vbr->guestDeviceType==DEV_TYPE_FLOPPY) {
        type [0] = 'f';
    } else { // a disk 
        switch (vbr->guestDeviceBus) {
        case BUS_TYPE_IDE:    type [0] = 'h'; break;
        case BUS_TYPE_SCSI:   type [0] = 's'; break;
        case BUS_TYPE_VIRTIO: type [0] = 'v'; break;
        case BUS_TYPE_XEN:    type [0] = 'x'; type [1] = 'v'; break;
        case BUS_TYPES_TOTAL:
        default:
            type [0] = '?'; // error
        }
    }
    
    char disk;
    if (vbr->guestDeviceType==DEV_TYPE_FLOPPY) {
        assert (vbr->diskNumber >=0 && vbr->diskNumber <= 9);
        disk = '0' + vbr->diskNumber;
    } else { // a disk 
        assert (vbr->diskNumber >=0 && vbr->diskNumber <= 26);
        disk = 'a' + vbr->diskNumber;
    }
    
    char part [3] = "\0";
    if (vbr->partitionNumber) {
        snprintf (part, sizeof(part), "%d", vbr->partitionNumber);
    }
    
    snprintf (vbr->guestDeviceName, sizeof (vbr->guestDeviceName), "%sd%c%s", type, disk, part);
}

static int disk_creator (artifact * a) // creates a 'raw' disk based on partitions
{
    int ret = ERROR;
    assert (a->bb);
    const char * dest_dev = blockblob_get_dev (a->bb);
    
    assert (dest_dev);
    logprintfl (EUCAINFO, "[%s] constructing disk of size %lld bytes in %s (%s)\n", a->instanceId, a->size_bytes, a->id, blockblob_get_dev (a->bb));

    blockmap map [EUCA_MAX_PARTITIONS] = { {BLOBSTORE_SNAPSHOT, BLOBSTORE_ZERO, {blob:NULL}, 0, 0, MBR_BLOCKS} }; // initially only MBR is in the map
 
    // run through partitions, add their sizes, populate the map
    virtualBootRecord * p1 = NULL;
    virtualBootRecord * disk = a->vbr;
    int map_entries = 1; // first map entry is for the MBR
    int root_entry = -1; // we do not know the root entry 
    int root_part = -1; // we do not know the root partition
    const char * kernel_path = NULL;
    const char * ramdisk_path = NULL; 
    long long offset_bytes = 512 * MBR_BLOCKS; // first partition begins after MBR
    assert (disk);
    for (int i=0; i<MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        artifact * dep = a->deps[i];
        if (! dep->is_partition) {
            if (dep->vbr && dep->vbr->type==NC_RESOURCE_KERNEL)
                kernel_path = blockblob_get_file (dep->bb);
            if (dep->vbr && dep->vbr->type==NC_RESOURCE_RAMDISK)
                ramdisk_path = blockblob_get_file (dep->bb);
            continue;
        }
        virtualBootRecord * p = dep->vbr;
        assert (p);
        if (p1==NULL)
            p1 = p;

        assert (dep->bb);
        assert (dep->size_bytes>0);
        map [map_entries].relation_type = BLOBSTORE_MAP;
        map [map_entries].source_type = BLOBSTORE_BLOCKBLOB;
        map [map_entries].source.blob = dep->bb;
        map [map_entries].first_block_src = 0;
        map [map_entries].first_block_dst = (offset_bytes / 512);
        map [map_entries].len_blocks = (dep->size_bytes / 512);
        logprintfl (EUCADEBUG, "[%s] mapping partition %d from %s [%lld-%lld]\n", 
                    a->instanceId, 
                    map_entries, 
                    blockblob_get_dev (a->deps[i]->bb), 
                    map [map_entries].first_block_dst, 
                    map [map_entries].first_block_dst + map [map_entries].len_blocks - 1);
        offset_bytes+=dep->size_bytes;
        if (p->type==NC_RESOURCE_IMAGE && root_entry==-1) {
            root_entry = map_entries;
            root_part = i;
        }
        map_entries++;
    }
    assert (p1);
    
    // set fields in vbr that are needed for
    // xml.c:gen_instance_xml() to generate correct disk entries
    disk->guestDeviceType = p1->guestDeviceType;
    disk->guestDeviceBus = p1->guestDeviceBus;
    disk->diskNumber = p1->diskNumber;
    set_disk_dev (disk);

    // map the partitions to the disk
    if (blockblob_clone (a->bb, map, map_entries)==-1) {
        ret = blobstore_get_error();
        logprintfl (EUCAERROR, "[%s] error: failed to clone partitions to created disk: %d %s\n", a->instanceId, ret, blobstore_get_last_msg());
        goto cleanup;
    }

    // create MBR
    logprintfl (EUCAINFO, "[%s] creating MBR\n", a->instanceId);
    if (diskutil_mbr (blockblob_get_dev (a->bb), "msdos") == ERROR) { // issues `parted mklabel`
        logprintfl (EUCAERROR, "[%s] error: failed to add MBR to disk: %d %s\n", a->instanceId, blobstore_get_error(), blobstore_get_last_msg());
        goto cleanup;
    }

    // add partition information to MBR
    for (int i=1; i<map_entries; i++) { // map [0] is for the MBR
        logprintfl (EUCAINFO, "[%s] adding partition %d to partition table (%s)\n", a->instanceId, i, blockblob_get_dev (a->bb));
        if (diskutil_part (blockblob_get_dev (a->bb),  // issues `parted mkpart`
                           "primary", // TODO: make this work with more than 4 partitions
                           NULL, // do not create file system
                           map [i].first_block_dst, // first sector
                           map [i].first_block_dst + map [i].len_blocks - 1) == ERROR) {
            logprintfl (EUCAERROR, "[%s] error: failed to add partition %d to disk: %d %s\n", a->instanceId, i, blobstore_get_error(), blobstore_get_last_msg());
            goto cleanup;
        }
    }

    //  make disk bootable if necessary
    if (a->do_make_bootable) {
        boolean bootification_failed = 1;

        logprintfl (EUCAINFO, "[%s] making disk bootable\n", a->instanceId);
        if (root_entry<1 || root_part<0) {
            logprintfl (EUCAERROR, "[%s] error: cannot make bootable a disk without an image\n", a->instanceId);
            goto cleanup;
        }
        if (kernel_path==NULL) {
            logprintfl (EUCAERROR, "[%s] error: no kernel found among the VBRs\n", a->instanceId);
            goto cleanup;
        }
        if (ramdisk_path==NULL) {
            logprintfl (EUCAERROR, "[%s] error: no ramdisk found among the VBRs\n", a->instanceId);
            goto cleanup;
        }
        // `parted mkpart` creates children devices for each partition
        // (e.g., /dev/mapper/euca-diskX gets /dev/mapper/euca-diskXp1 and so on)
        // we mount such a device here so as to copy files to the root partition
        // (we cannot mount the dev of the partition's blob because it becomes
        // 'busy' after the clone operation)
        char dev [EUCA_MAX_PATH];
        snprintf (dev, sizeof (dev), "%sp%d", blockblob_get_dev (a->bb), root_entry);
        
        // mount the root partition
        char mnt_pt [EUCA_MAX_PATH] = "/tmp/euca-mount-XXXXXX";
        if (safe_mkdtemp (mnt_pt)==NULL) {
            logprintfl (EUCAINFO, "[%s] error: mkdtemp() failed: %s\n", a->instanceId, strerror (errno));
            goto cleanup;
        }
        if (diskutil_mount (dev, mnt_pt) != OK) {
            logprintfl (EUCAINFO, "[%s] error: failed to mount '%s' on '%s'\n", a->instanceId, dev, mnt_pt);
            goto cleanup;
        }
        
        // copy in kernel and ramdisk and run grub over the root partition and the MBR
        logprintfl (EUCAINFO, "[%s] making partition %d bootable\n", a->instanceId, root_part);
        logprintfl (EUCAINFO, "[%s] with kernel %s\n", a->instanceId, kernel_path);
        logprintfl (EUCAINFO, "[%s] and ramdisk %s\n", a->instanceId, ramdisk_path);
        if (diskutil_grub (blockblob_get_dev (a->bb), mnt_pt, root_part, kernel_path, ramdisk_path)!=OK) {
            logprintfl (EUCAERROR, "[%s] error: failed to make disk bootable\n", a->instanceId, root_part);
            goto unmount;
        }
        // change user of the blob device back to 'eucalyptus' (grub sets it to 'root')
        sleep (1); // without this, perms on dev-mapper devices can flip back, presumably because in-kernel ops complete after grub process finishes
        if (diskutil_ch (blockblob_get_dev (a->bb), EUCALYPTUS_ADMIN, NULL, 0) != OK) {
            logprintfl (EUCAINFO, "[%s] error: failed to change user for '%s' to '%s'\n", a->instanceId, dev, EUCALYPTUS_ADMIN);
        }
        bootification_failed = 0;
        
    unmount:
        
        // unmount partition and delete the mount point
        if (diskutil_umount (mnt_pt) != OK) {
            logprintfl (EUCAINFO, "[%s] error: failed to unmount %s (there may be a resource leak)\n", a->instanceId, mnt_pt);
            bootification_failed = 1;
        }
        if (rmdir (mnt_pt) != 0) {
            logprintfl (EUCAINFO, "[%s] error: failed to remove %s (there may be a resource leak): %s\n", a->instanceId, mnt_pt, strerror(errno));
            bootification_failed = 1;
        }
        if (bootification_failed)
            goto cleanup;
    }
    
    ret = OK;
 cleanup:
    return ret;
}

static int iqn_creator (artifact * a)
{
    assert (a);
    virtualBootRecord * vbr = a->vbr;
    assert (vbr);

    char * dev = connect_iscsi_target (vbr->resourceLocation);
    if (!dev || !strstr(dev, "/dev")) {
        logprintfl(EUCAERROR, "[%s] error: failed to connect to iSCSI target\n", a->instanceId);
        return ERROR;
    } 
    // update VBR with device location
    safe_strncpy (vbr->backingPath, dev, sizeof (vbr->backingPath));
    vbr->backingType = SOURCE_TYPE_BLOCK;

    return OK;
}

static int aoe_creator (artifact * a)
{
    assert (a);
    virtualBootRecord * vbr = a->vbr;
    assert (vbr);

    char * dev = vbr->resourceLocation;
    if (!dev || !strstr(dev, "/dev") || check_block(dev)!=0) {
        logprintfl(EUCAERROR, "[%s] error: failed to locate AoE device %s\n", a->instanceId, dev);
        return ERROR;
    } 
    // update VBR with device location
    safe_strncpy (vbr->backingPath, dev, sizeof (vbr->backingPath));
    vbr->backingType = SOURCE_TYPE_BLOCK;

    return OK;
}

static int copy_creator (artifact * a)
{
    assert (a->deps[0]);
    assert (a->deps[0]->bb);
    assert (a->deps[1]==NULL);
    artifact * dep = a->deps[0];
    virtualBootRecord * vbr = a->vbr;
    assert (vbr);

    logprintfl (EUCAINFO, "[%s] copying/cloning blob %s to blob %s\n", a->instanceId, dep->bb->id, a->bb->id);
    if (a->must_be_file) {
        if (blockblob_copy (dep->bb, 0L, a->bb, 0L, 0L)==-1) {
            logprintfl (EUCAERROR, "[%s] error: failed to copy blob %s to blob %s: %d %s\n", a->instanceId, dep->bb->id, a->bb->id, blobstore_get_error(), blobstore_get_last_msg());
            return blobstore_get_error();
        }
    } else {
        blockmap map [] = {
            {BLOBSTORE_SNAPSHOT, BLOBSTORE_BLOCKBLOB, {blob:dep->bb}, 0, 0, round_up_sec (dep->size_bytes) / 512}
        };
        if (blockblob_clone (a->bb, map, 1)==-1) {
            logprintfl (EUCAERROR, "[%s] error: failed to clone blob %s to blob %s: %d %s\n", a->instanceId, dep->bb->id, a->bb->id, blobstore_get_error(), blobstore_get_last_msg());
            return blobstore_get_error();
        }
    }

    const char * dev = blockblob_get_dev (a->bb);    
    const char * bbfile = blockblob_get_file(a->bb);

    if (a->do_tune_fs) {
        // tune file system, which is needed to boot EMIs fscked long ago
        logprintfl (EUCAINFO, "[%s] tuning root file system on disk %d partition %d\n", a->instanceId, vbr->diskNumber, vbr->partitionNumber);
        if (diskutil_tune (dev) == ERROR) {
            logprintfl (EUCAERROR, "[%s] error: failed to tune root file system: %s\n", a->instanceId, blobstore_get_last_msg());
            return ERROR;
        }
    }
    
    if (!strcmp(vbr->typeName, "kernel") || !strcmp(vbr->typeName, "ramdisk")) {
        // for libvirt/kvm, kernel and ramdisk must be readable by libvirt
        if (diskutil_ch (bbfile, NULL, NULL, 0664) != OK) {
            logprintfl (EUCAINFO, "[%s] error: failed to change user and/or permissions for '%s' '%s'\n", a->instanceId, vbr->typeName, bbfile);
        }
    }
    
    if (strlen (a->sshkey)) {

        int injection_failed = 1;
        logprintfl (EUCAINFO, "[%s] injecting the ssh key\n", a->instanceId);

        // mount the partition
        char mnt_pt [EUCA_MAX_PATH] = "/tmp/euca-mount-XXXXXX";
        if (safe_mkdtemp (mnt_pt)==NULL) {
            logprintfl (EUCAINFO, "[%s] error: mkdtemp() failed: %s\n", a->instanceId,  strerror (errno));
            goto error;
        }
        if (diskutil_mount (dev, mnt_pt) != OK) {
            logprintfl (EUCAINFO, "[%s] error: failed to mount '%s' on '%s'\n", a->instanceId, dev, mnt_pt);
            goto error;
        }
        
        // save the SSH key, with the right permissions
        char path [EUCA_MAX_PATH];
        snprintf (path, sizeof (path), "%s/root/.ssh", mnt_pt);
        if (diskutil_mkdir (path) == -1) {
            logprintfl (EUCAINFO, "[%s] error: failed to create path '%s'\n", a->instanceId, path);
            goto unmount;
        }
        if (diskutil_ch (path, "root", NULL, 0700) != OK) {
            logprintfl (EUCAINFO, "[%s] error: failed to change user and/or permissions for '%s'\n", a->instanceId, path);
            goto unmount;
        }
        snprintf (path, sizeof (path), "%s/root/.ssh/authorized_keys", mnt_pt);
        if (diskutil_write2file (path, a->sshkey) != OK) { // TODO: maybe append the key instead of overwriting?
            logprintfl (EUCAINFO, "[%s] error: failed to save key in '%s'\n", a->instanceId, path);
            goto unmount;
        }
        if (diskutil_ch (path, "root", NULL, 0600) != OK) {
            logprintfl (EUCAINFO, "[%s] error: failed to change user and/or permissions for '%s'\n", a->instanceId, path);
            goto unmount;
        }
        // change user of the blob device back to 'eucalyptus' (tune and maybe other commands above set it to 'root')
        if (diskutil_ch (dev, EUCALYPTUS_ADMIN, NULL, 0) != OK) {
            logprintfl (EUCAINFO, "[%s] error: failed to change user for '%s' to '%s'\n", a->instanceId, dev, EUCALYPTUS_ADMIN);
        }
        injection_failed = 0;

    unmount:
        
        // unmount partition and delete the mount point
        if (diskutil_umount (mnt_pt) != OK) {
            logprintfl (EUCAINFO, "[%s] error: failed to unmount %s (there may be a resource leak)\n", a->instanceId, mnt_pt);
            injection_failed = 1;
        }
        if (rmdir (mnt_pt) != 0) {
            logprintfl (EUCAINFO, "[%s] error: failed to remove %s (there may be a resource leak): %s\n", a->instanceId, mnt_pt, strerror(errno));
            injection_failed = 1;
        }

    error:

        if (injection_failed)
            return ERROR;
    }        

    return OK;
}

#define ART_SIG_MAX 32768

// Functions for adding and freeing artifacts on a tree.
// Currently each artifact tree is used within a single
// thread (startup thread), so these do not need to be thread safe.

int art_add_dep (artifact * a, artifact * dep)
{
    if (dep==NULL)
        return OK;

    for (int i = 0; i < MAX_ARTIFACT_DEPS; i++) {
        if (a->deps[i] == NULL) {
            logprintfl (EUCADEBUG, "[%s] added to artifact %03d|%s artifact %03d|%s\n", 
                        a->instanceId, a->seq, a->id, dep->seq, dep->id);
            a->deps[i] = dep;
            dep->refs++;
            return OK;
        }
    }
    return ERROR;
}

void art_free (artifact * a) // frees the artifact and all its dependencies
{
    if (a->refs > 0)
        a->refs--; // this free reduces reference count, if positive, by 1

    if (a->refs == 0) { // if this is the last reference

        // try freeing dependents recursively
        for (int i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
            art_free (a->deps[i]);
        }
        logprintfl (EUCADEBUG2, "[%s] freeing artifact %03d|%s size=%lld vbr=%u cache=%d file=%d\n", 
                    a->instanceId, a->seq, a->id, a->size_bytes, a->vbr, a->may_be_cached, a->must_be_file);
        free (a);
    }
}

void arts_free (artifact * array [], unsigned int array_len)
{
    for (int i=0; i<array_len; i++)
        if (array [i])
            art_free (array [i]);
}

static void art_print_tree (const char * prefix, artifact * a)
{
    logprintfl (EUCADEBUG, "[%s] artifacts tree: %s%03d|%s cache=%d file=%d creator=%0x vbr=%0x\n", 
                a->instanceId, prefix, a->seq, a->id, a->may_be_cached, a->must_be_file, a->creator, a->vbr);

    char new_prefix [512];
    snprintf (new_prefix, sizeof (new_prefix), "%s\t", prefix);
    for (int i=0; i< MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        art_print_tree (new_prefix, a->deps[i]);
    }
}

boolean tree_uses_blobstore (artifact * a)
{
    if (!a->id_is_path)
        return TRUE;
    for (int i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        if (tree_uses_blobstore (a->deps[i]))
            return TRUE;
    }
    return FALSE;
}

boolean tree_uses_cache (artifact * a)
{
    if (a->may_be_cached)
        return TRUE;
    for (int i = 0; i < MAX_ARTIFACT_DEPS && a->deps[i]; i++) {
        if (tree_uses_cache (a->deps[i]))
            return TRUE;
    }
    return FALSE;
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

static __thread char current_instanceId [512] = ""; // instance ID that is being serviced, for logging only

artifact * art_alloc (const char * id, const char * sig, long long size_bytes, boolean may_be_cached, boolean must_be_file, boolean must_be_hollow, int (* creator) (artifact * a), virtualBootRecord * vbr)
{
    artifact * a = calloc (1, sizeof (artifact));
    if (a==NULL)
        return NULL;

    static int seq = 0;
    a->seq = ++seq; // not thread safe, but seq's are just for debugging
    safe_strncpy (a->instanceId, current_instanceId, sizeof (a->instanceId)); // for logging
    logprintfl (EUCADEBUG, "[%s] allocated artifact %03d|%s size=%lld vbr=%u cache=%d file=%d\n", a->instanceId,  seq, id, size_bytes, vbr, may_be_cached, must_be_file);

    if (id)
        safe_strncpy (a->id, id, sizeof (a->id));
    if (sig)
        safe_strncpy (a->sig, sig, sizeof (a->sig));
    a->size_bytes = size_bytes;
    a->may_be_cached = may_be_cached;
    a->must_be_file = must_be_file;
    a->must_be_hollow = must_be_hollow;
    a->creator = creator;
    a->vbr = vbr;
    a->do_tune_fs = FALSE;
    if (vbr && (vbr->type==NC_RESOURCE_IMAGE && vbr->partitionNumber>0 )) //this is hacky
        a->do_tune_fs = TRUE;

    return a;
}

// convert emi-XXXX-YYYY to dsk-XXXX
static void convert_id (const char * src, char * dst, unsigned int size)
{
    if (strcasestr (src, "emi-") == src) {
        const char * s = src + 4;  // position aftter 'emi'
        char * d = dst + strlen (dst); // position after 'dsk' or 'emi' or whatever
        * d++ = '-';
        while ((*s>='0') && (*s<='z') // copy letters and numbers up to a hyphen
               && (d-dst < size)) { // don't overrun dst
            * d++ = * s++;
        }
        * d = '\0';
    }
}

static char * url_get_digest (const char * url)
{
    char * digest_str = NULL;
    char * digest_path = strdup ("/tmp/url-digest-XXXXXX");

    if(!digest_path) {
       logprintfl (EUCAERROR, "{%u} error: failed to strdup digest path\n", (unsigned int)pthread_self());
       return digest_path;
    }

    int tmp_fd = safe_mkstemp (digest_path);
    if (tmp_fd<0) {
        logprintfl (EUCAERROR, "{%u} error: failed to create a digest file %s\n", (unsigned int)pthread_self(), digest_path);
    } else {
        close (tmp_fd);

        // download a fresh digest
        if (http_get_timeout (url, digest_path, 10, 4, 0, 0) != 0 ) {
            logprintfl (EUCAERROR, "{%u} error: failed to download digest to %s\n", (unsigned int)pthread_self(), digest_path);
        } else {
            digest_str = file2strn (digest_path, 100000);
        }
        unlink (digest_path);
    }
    if(digest_path) {
        free(digest_path);
    }
    return digest_str;
}

static artifact * art_alloc_vbr (virtualBootRecord * vbr, boolean do_make_work_copy, boolean must_be_file, const char * sshkey)
{
    artifact * a = NULL;

    switch (vbr->locationType) {
    case NC_LOCATION_CLC: 
    case NC_LOCATION_SC:
        logprintfl (EUCAERROR, "[%s] error: location of type %d is NOT IMPLEMENTED\n", current_instanceId, vbr->locationType);
        return NULL;

    case NC_LOCATION_URL: {
        // get the digest for size and signature
        char manifestURL[MAX_PATH];
        char * blob_sig = NULL;
        int rc;
        snprintf(manifestURL, MAX_PATH, "%s.manifest.xml", vbr->preparedResourceLocation);
        blob_sig = url_get_digest(manifestURL);
        if (blob_sig==NULL) goto u_out;
        
        // extract size from the digest
        long long bb_size_bytes = str2longlong (blob_sig, "<size>", "</size>"); // pull size from the digest
        if (bb_size_bytes < 1) goto u_out;
        vbr->size = bb_size_bytes; // record size in VBR now that we know it

        // generate ID of the artifact (append -##### hash of sig)
        char art_id [48];
        if (art_gen_id (art_id, sizeof(art_id), vbr->id, blob_sig) != OK) goto w_out;

        // allocate artifact struct
        a = art_alloc (art_id, blob_sig, bb_size_bytes, TRUE, must_be_file, FALSE, url_creator, vbr);

        u_out:
        
        if (blob_sig)
            free (blob_sig);
        break;
    }
    case NC_LOCATION_WALRUS: {
        // get the digest for size and signature
        char * blob_sig = walrus_get_digest (vbr->preparedResourceLocation);
        if (blob_sig==NULL) {
            logprintfl (EUCAERROR, "[%s] error: failed to obtain image digest from  Walrus\n", current_instanceId);
            goto w_out;
        }

        // extract size from the digest
        long long bb_size_bytes = str2longlong (blob_sig, "<size>", "</size>"); // pull size from the digest
        if (bb_size_bytes < 1) {
            logprintfl (EUCAERROR, "[%s] error: incorrect image digest or error returned from Walrus\n", current_instanceId);
            goto w_out;
        }
        vbr->size = bb_size_bytes; // record size in VBR now that we know it

        // generate ID of the artifact (append -##### hash of sig)
        char art_id [48];
        if (art_gen_id (art_id, sizeof(art_id), vbr->id, blob_sig) != OK) {
            logprintfl (EUCAERROR, "[%s] error: failed to generate artifact id\n", current_instanceId);
            goto w_out;
        }

        // allocate artifact struct
        a = art_alloc (art_id, blob_sig, bb_size_bytes, TRUE, must_be_file, FALSE, walrus_creator, vbr);

        w_out:
        
        if (blob_sig)
            free (blob_sig);
        break;
    }        

    case NC_LOCATION_IQN: {
        a = art_alloc ("iscsi-vol", NULL, -1, FALSE, FALSE, FALSE, iqn_creator, vbr);
        goto out;
    }

    case NC_LOCATION_AOE: {
        a = art_alloc ("aoe-vol", NULL, -1, FALSE, FALSE, FALSE, aoe_creator, vbr);
        goto out;
    }

    case NC_LOCATION_NONE: {
        assert (vbr->size > 0L);

        vbr->size = vbr->size * VBR_SIZE_SCALING; // TODO: remove this adjustment (CLC sends size in KBs) 

        char art_sig [ART_SIG_MAX]; // signature for this artifact based on its salient characteristics
        if (snprintf (art_sig, sizeof (art_sig), "id=%s size=%lld format=%s\n\n", 
                      vbr->id, vbr->size, vbr->formatName) >= sizeof (art_sig)) // output was truncated
            break;

        char buf [32]; // first part of artifact ID
        char * art_pref;
        if (strcmp (vbr->id, "none")==0) {
            if (snprintf (buf, sizeof (buf), "prt-%05lld%s", 
                          vbr->size/1048576, vbr->formatName) >= sizeof (buf)) // output was truncated
                break;
            art_pref = buf;
        } else {
            art_pref = vbr->id;
        }

        char art_id [48]; // ID of the artifact (append -##### hash of sig)
        if (art_gen_id (art_id, sizeof(art_id), art_pref, art_sig) != OK) 
            break;

        a = art_alloc (art_id, art_sig, vbr->size, TRUE, must_be_file, FALSE, partition_creator, vbr);
        break;
    }
    default:
        logprintfl (EUCAERROR, "[%s] error: unrecognized locationType %d\n", current_instanceId, vbr->locationType);
    }

    // allocate another artifact struct if a work copy is requested
    // or if an SSH key is supplied
    if (a && (do_make_work_copy || sshkey)) {

        artifact * a2 = NULL;
        char art_id [48];
        safe_strncpy (art_id, a->id, sizeof (art_id));
        char art_sig [ART_SIG_MAX];
        safe_strncpy (art_sig, a->sig, sizeof (art_sig));

        if (sshkey) { // if SSH key is included, recalculate sig and ID
            if (strlen(sshkey) > sizeof(a->sshkey)) {
                logprintfl (EUCAERROR, "[%s] error: received SSH key is too long\n", a->instanceId);
                goto free;
            }
            
            char key_sig [ART_SIG_MAX];
            if ((snprintf (key_sig, sizeof(key_sig), "KEY /root/.ssh/authorized_keys\n%s\n\n", 
                           sshkey) >= sizeof (key_sig)) // output truncated
                ||
                ((strlen (art_sig) + strlen (key_sig)) >= sizeof (art_sig))) { // overflow
                logprintfl (EUCAERROR, "[%s] error: internal buffers (ART_SIG_MAX) too small for signature\n", a->instanceId);
                goto free;
            }
            strncat (art_sig, key_sig, sizeof (art_sig) - strlen (key_sig) - 1);
            
            char art_pref [EUCA_MAX_PATH] = "emi";
            convert_id (a->id, art_pref, sizeof (art_pref));
            if (art_gen_id (art_id, sizeof(art_id), art_pref, key_sig) != OK) {
                goto free;
            }
        }
         
        a2 = art_alloc (art_id, art_sig, a->size_bytes, !do_make_work_copy, must_be_file, FALSE, copy_creator, vbr);
        if (a2) {
            if (sshkey)
                strncpy (a2->sshkey, sshkey, sizeof (a2->sshkey)-1 );

            if (art_add_dep (a2, a) == OK) {
                a = a2;
            } else {
                art_free (a2);
                goto free;
            }
        } else {
            goto free;
        }

        goto out;
        
    free:
        if (a) {
            art_free (a);        
            a = NULL;
        }
    }

 out:
    return a;
}

static artifact * // pointer to 'keyed' disk artifact or NULL on error
art_alloc_disk ( // allocates a 'keyed' disk artifact and possibly the underlying 'raw' disk 
                virtualBootRecord * vbr, // VBR of the newly created
                artifact * prereqs [], int num_prereqs, // prerequisites (kernel and ramdisk), if any
                artifact * parts [], int num_parts, // OPTION A: partitions for constructing a 'raw' disk
                artifact * emi_disk, // OPTION B: the artifact of the EMI that serves as a full disk
                boolean do_make_bootable, // kernel injection is requested (not needed on KVM and Xen)
                boolean do_make_work_copy) // generated disk should be a work copy
{
    char art_sig [ART_SIG_MAX] = ""; 
    char art_pref [EUCA_MAX_PATH] = "dsk";
    long long disk_size_bytes = 512LL * MBR_BLOCKS;

    // run through partitions, adding up their signatures and their size
    for (int i = 0; i<num_parts; i++) {
        assert (parts);
        artifact * p = parts [i];
        
        // construct signature for the disk, based on the sigs of underlying components
        char part_sig [ART_SIG_MAX];
        if ((snprintf (part_sig, sizeof(part_sig), "PARTITION %d (%s)\n%s\n\n", 
                       i, p->id, p->sig) >= sizeof (part_sig)) // output truncated
            ||
            ((strlen (art_sig) + strlen (part_sig)) >= sizeof (art_sig))) { // overflow
            logprintfl (EUCAERROR, "[%s] error: internal buffers (ART_SIG_MAX) too small for signature\n", current_instanceId);
            return NULL;
        }
        strncat (art_sig, part_sig, sizeof (art_sig) - strlen (art_sig) - 1);

        // verify and add up the sizes of partitions
        if (p->size_bytes < 1) {
            logprintfl (EUCAERROR, "[%s] error: unknown size for partition %d\n", current_instanceId, i);
            return NULL;
        }
        if (p->size_bytes % 512) {
            logprintfl (EUCAERROR, "[%s] error: size for partition %d is not a multiple of 512\n", current_instanceId, i);
            return NULL;
        }
        disk_size_bytes += p->size_bytes;
        convert_id (p->id, art_pref, sizeof (art_pref));
    }
    
    // run through prerequisites (kernel and ramdisk), if any, adding up their signature
    // (this will not happen on KVM and Xen where injecting kernel is not necessary)
    for (int i = 0; do_make_bootable && i<num_prereqs; i++) {
        artifact * p = prereqs [i];
        
        // construct signature for the disk, based on the sigs of underlying components
        char part_sig [ART_SIG_MAX];
        if ((snprintf (part_sig, sizeof(part_sig), "PREREQUISITE %s\n%s\n\n", 
                       p->id, p->sig) >= sizeof (part_sig)) // output truncated
            ||
            ((strlen (art_sig) + strlen (part_sig)) >= sizeof (art_sig))) { // overflow
            logprintfl (EUCAERROR, "[%s] error: internal buffers (ART_SIG_MAX) too small for signature\n", current_instanceId);
            return NULL;
        }
        strncat (art_sig, part_sig, sizeof (art_sig) - strlen (art_sig) - 1);
    }

    artifact * disk;

    if (emi_disk) { // we have a full disk
        if (do_make_work_copy) { // allocate a work copy of it
            disk_size_bytes = emi_disk->size_bytes;
            if ((strlen (art_sig) + strlen (emi_disk->sig)) >= sizeof (art_sig)) { // overflow
                logprintfl (EUCAERROR, "[%s] error: internal buffers (ART_SIG_MAX) too small for signature\n", current_instanceId);
                return NULL;
            }
            strncat (art_sig, emi_disk->sig, sizeof (art_sig) - strlen (art_sig) - 1);
            
            if ((disk = art_alloc (emi_disk->id, art_sig, emi_disk->size_bytes, FALSE, FALSE, FALSE, copy_creator, NULL)) == NULL ||
                art_add_dep (disk, emi_disk) != OK) {
                goto free;
            }
        } else {
            disk = emi_disk; // no work copy needed - we're done
        }
        
    } else { // allocate the 'raw' disk artifact
        char art_id [48]; // ID of the artifact (append -##### hash of sig)
        if (art_gen_id (art_id, sizeof(art_id), art_pref, art_sig) != OK) 
            return NULL;
        
        disk = art_alloc (art_id, art_sig, disk_size_bytes, !do_make_work_copy, FALSE, TRUE, disk_creator, vbr);
        if (disk==NULL) {
            logprintfl (EUCAERROR, "[%s] error: failed to allocate an artifact for raw disk\n", disk->instanceId);
            return NULL;
        }
        disk->do_make_bootable = do_make_bootable;
        
        // attach partitions as dependencies of the raw disk        
        for (int i = 0; i<num_parts; i++) {
            artifact * p = parts [i];
            if (art_add_dep (disk, p) != OK) {
                logprintfl (EUCAERROR, "[%s] error: failed to add dependency to an artifact\n", disk->instanceId);
                goto free;
            }
            p->is_partition = TRUE;
        }
    
        // optionally, attach prereqs as dependencies of the raw disk
        for (int i = 0; do_make_bootable && i<num_prereqs; i++) {
            artifact * p = prereqs [i];
            if (art_add_dep (disk, p) != OK) {
                logprintfl (EUCAERROR, "[%s] error: failed to add a prerequisite to an artifact\n", disk->instanceId);
                goto free;
            }
        }
    }

    return disk;
free:
    if(disk) art_free (disk);
    return NULL;
}

// sets instance ID in thread-local variable, for logging
// (same effect as passing it into vbr_alloc_tree)
void art_set_instanceId (const char * instanceId) 
{
    safe_strncpy (current_instanceId, instanceId, sizeof (current_instanceId));
}

artifact * // returns pointer to the root of artifact tree or NULL on error
vbr_alloc_tree ( // creates a tree of artifacts for a given VBR (caller must free the tree)
                virtualMachine * vm, // virtual machine containing the VBR
                boolean do_make_bootable, // make the disk bootable by copying kernel and ramdisk into it and running grub
                boolean do_make_work_copy, // ensure that all components that get modified at run time have work copies
                const char * sshkey, // key to inject into the root partition or NULL if no key
                const char * instanceId) // ID of the instance (for logging purposes only)
{
    if (instanceId)
        safe_strncpy (current_instanceId, instanceId, sizeof (current_instanceId));

    // sort vbrs into prereq [] and parts[] so they can be approached in the right order
    virtualBootRecord * prereq_vbrs [EUCA_MAX_VBRS];
    int total_prereq_vbrs = 0;
    virtualBootRecord * parts  [BUS_TYPES_TOTAL][EUCA_MAX_DISKS][EUCA_MAX_PARTITIONS];
    int total_parts = 0;
    bzero (parts, sizeof (parts));
    for (int i=0; i<EUCA_MAX_VBRS && i<vm->virtualBootRecordLen; i++) {
        virtualBootRecord * vbr = &(vm->virtualBootRecord[i]);
        if (vbr->type==NC_RESOURCE_KERNEL || vbr->type==NC_RESOURCE_RAMDISK) {
            prereq_vbrs [total_prereq_vbrs++] = vbr;
        } else {
            parts [vbr->guestDeviceBus][vbr->diskNumber][vbr->partitionNumber] = vbr;
            total_parts++;
        }
    }
    logprintfl (EUCADEBUG, "[%s] found %d prereqs and %d partitions in the VBR\n", instanceId, total_prereq_vbrs, total_parts);

    artifact * root = art_alloc (instanceId, NULL, -1, FALSE, FALSE, FALSE, NULL, NULL); // allocate a sentinel artifact
    if (root == NULL)
        return NULL;
    
    // allocate kernel and ramdisk artifacts.
    artifact * prereq_arts [EUCA_MAX_VBRS];
    int total_prereq_arts = 0;
    for (int i=0; i<total_prereq_vbrs; i++) {
        virtualBootRecord * vbr = prereq_vbrs [i];
        artifact * dep = art_alloc_vbr (vbr, do_make_work_copy, TRUE, NULL);
        if (dep == NULL) 
            goto free;
        prereq_arts [total_prereq_arts++] = dep;
        
        // if disk does not need to be bootable, we'll need 
        // kernel and ramdisk as a top-level dependencies
        if (!do_make_bootable)
            if (art_add_dep (root, dep) != OK)
                goto free;
    }
    
    // then attach disks and partitions
    for (int i=0; i<BUS_TYPES_TOTAL; i++) { 
        for (int j=0; j<EUCA_MAX_DISKS; j++) {
            int partitions = 0;
            artifact * disk_arts [EUCA_MAX_PARTITIONS];
            bzero (disk_arts, sizeof (disk_arts));
            for (int k=0; k<EUCA_MAX_PARTITIONS; k++) {
                virtualBootRecord * vbr = parts [i][j][k];
                const char * use_sshkey = NULL;
                if (vbr) { // either a disk (k==0) or a partition (k>0)
                    if (vbr->type==NC_RESOURCE_IMAGE && k > 0) { // only inject SSH key into an EMI which has a single partition (whole disk)
                        use_sshkey = sshkey;
                    }
                    disk_arts [k] = art_alloc_vbr (vbr, do_make_work_copy, FALSE, use_sshkey);
                    if (disk_arts [k] == NULL) {
                        arts_free (disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }
                    if (vbr->type == NC_RESOURCE_EBS) // EBS-backed instances need no additional artifacts
                        continue;
                    if (k==0) { // if this is a disk artifact, insert a work copy in front of it
                        disk_arts [k] = art_alloc_disk (vbr, 
                                                        prereq_arts, total_prereq_arts, 
                                                        NULL, 0, 
                                                        disk_arts [k], 
                                                        do_make_bootable, 
                                                        do_make_work_copy);
                        if (disk_arts [k] == NULL) {
                            arts_free (disk_arts, EUCA_MAX_PARTITIONS);
                            goto free;
                        }   
                    } else { // k>0
                        partitions++; 
                    }
                    
                } else if (partitions) { // there were partitions and we saw them all
                    assert (disk_arts [0] == NULL);
                    if (vm->virtualBootRecordLen==EUCA_MAX_VBRS) {
                        logprintfl (EUCAERROR, "[%s] error: out of room in the virtual boot record while adding disk %d on bus %d\n", instanceId, j, i);
                        goto out;
                    }
                    disk_arts [0] = art_alloc_disk (&(vm->virtualBootRecord [vm->virtualBootRecordLen]), 
                                                    prereq_arts, 
                                                    total_prereq_arts, 
                                                    disk_arts + 1, partitions, 
                                                    NULL, 
                                                    do_make_bootable, 
                                                    do_make_work_copy);
                    if (disk_arts [0] == NULL) {
                        arts_free (disk_arts, EUCA_MAX_PARTITIONS);
                        goto free;
                    }
                    vm->virtualBootRecordLen++;
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
                    disk_arts [k] = NULL;
                    if (k==0) { // for a disk partition artifacts, if any, are already attached to it
                        break;
                    }
                }
            }
        }
    }
    art_print_tree ("", root);
    goto out;
    
 free:
    art_free (root);
    root = NULL;

 out:
    return root;
}

#define FIND_BLOB_TIMEOUT_USEC   50000LL // TODO: use 100 or less to induce rare timeouts
#define DELETE_BLOB_TIMEOUT_USEC 50000LL

static int // returns OK or BLOBSTORE_ERROR_ error codes
find_or_create_blob ( // either opens a blockblob or creates it
                     int flags, // determine whether blob is created or opened
                     blobstore * bs, // the blobstore in which to open/create blockblob
                     const char * id, // id of the blockblob
                     long long size_bytes, // size of the blockblob
                     const char * sig, // signature of the blockblob
                     blockblob ** bbp) // RESULT: opened blockblob handle or NULL if ERROR is returned
{
    blockblob * bb = NULL;
    int ret = OK;
    
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
 
    // determine blob IDs for cache and work
    const char * id_cache = a->id;
    char id_work  [BLOBSTORE_MAX_PATH];
    if (work_prefix && strlen (work_prefix))
        snprintf (id_work, sizeof (id_work), "%s/%s", work_prefix, a->id);
    else 
        safe_strncpy (id_work, a->id, sizeof (id_work));

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
        if (check_path (a->id)) {
            if (do_create) {
                return OK; // creating only matters for blobs, which get locked, not for files
            } else {
                return BLOBSTORE_ERROR_NOENT;
            }
        } else {
            return OK;
        }
    }

    assert (work_bs);
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
        ret = find_or_create_blob (flags, cache_bs, id_cache, size_bytes, a->sig, bbp);
        
        // for some error conditions from cache we try work blobstore
        if (( do_create && ret==BLOBSTORE_ERROR_NOSPC) ||
            (!do_create && ret==BLOBSTORE_ERROR_NOENT) ||
            (!do_create && ret==BLOBSTORE_ERROR_SIGNATURE)

            // these reduce reliance on cache (work copies are created more aggressively)
            //|| ret==BLOBSTORE_ERROR_NOENT 
            //|| ret==BLOBSTORE_ERROR_AGAIN
            //|| ret==BLOBSTORE_ERROR_EXIST
            
            ) {
            goto try_work;
        } else { // for all others we return the error or success
            return ret;
        }
    }
 try_work:
    logprintfl (EUCADEBUG, "[%s] switching to work blobstore for %s (do_create=%d ret=%d)\n", a->instanceId, id_cache, do_create, ret);
    if (ret==BLOBSTORE_ERROR_SIGNATURE) {
        logprintfl (EUCAWARN, "[%s] warning: signature mismatch on cached blob %s\n", a->instanceId, id_cache); // TODO: maybe invalidate?
    }
    return find_or_create_blob (flags, work_bs, id_work, size_bytes, a->sig, bbp);
}

#define ARTIFACT_RETRY_SLEEP_USEC 500000LL

// Given a root node in a tree of blob artifacts, unless the root
// blob already exists and has the right signature, this function:
//
// - ensures that any depenent blobs are present and open
// - creates the root blob and invokes to creator function to fill it
// - closes any dependent blobs
// 
// The function is recursive and the contract is that when it returns
//
// - with success, the root blob is open and ready
// - with failure, the root blob is closed and possibly non-existant
//
// Either way, none of the child blobs are open.

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

    logprintfl (EUCADEBUG, "[%s] implementing artifact %03d|%s\n", root->instanceId, root->seq, root->id);

    int ret = OK;
    int tries = 0;
    do { // we may have to retry multiple times due to competition
        int num_opened_deps = 0;
        boolean do_deps = TRUE;
        boolean do_create = TRUE;

        if (tries++)
            usleep (ARTIFACT_RETRY_SLEEP_USEC); 

        if (!root->creator) { // sentinel nodes do not have a creator
            do_create = FALSE;
            
        } else { // not a sentinel
            if (root->vbr && root->vbr->type == NC_RESOURCE_EBS)
                goto create; // EBS artifacts have no disk manifestation and no dependencies, so skip to creation

            // try to open the artifact
            switch (ret = find_or_create_artifact (FIND, root, work_bs, cache_bs, work_prefix, &(root->bb))) {
            case OK:
                logprintfl (EUCADEBUG, "[%s] found existing artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
                update_vbr_with_backing_info (root);
                do_deps = FALSE;
                do_create = FALSE;
                break;
            case BLOBSTORE_ERROR_NOENT: // doesn't exist yet => ok, create it
                break; 
            case BLOBSTORE_ERROR_AGAIN: // timed out the => competition took too long
            case BLOBSTORE_ERROR_MFILE: // out of file descriptors for locking => same problem
                goto retry_or_fail;
                break;
            default: // all other errors
                logprintfl (EUCAERROR, "[%s] error: failed to provision artifact %03d|%s (error=%d) on try %d\n", root->instanceId, root->seq, root->id, ret, tries);
                goto retry_or_fail;
            }
        }

        // at this point the artifact we need does not seem to exist
        // (though it could be created before we get around to that)
        
        if (do_deps) { // recursively go over dependencies, if any
            for (int i = 0; i < MAX_ARTIFACT_DEPS && root->deps[i]; i++) {

                // recalculate the time that remains in the timeout period
                long long new_timeout_usec = timeout_usec;
                if (timeout_usec > 0) {
                    new_timeout_usec -= time_usec()-started;
                    if (new_timeout_usec < 1) { // timeout exceeded, so bail out of this function
                        ret=BLOBSTORE_ERROR_AGAIN;
                        goto retry_or_fail;
                    }
                }
                switch (ret = art_implement_tree (root->deps[i], work_bs, cache_bs, work_prefix, new_timeout_usec)) {
                case OK:
                    if (do_create) { // we'll hold the dependency open for the creator
                        num_opened_deps++;
                    } else { // this is a sentinel, we're not creating anything, so release the dep immediately
                        if (root->deps[i]->bb && (blockblob_close (root->deps[i]->bb) == -1)) {
                            logprintfl (EUCAERROR, "[%s] error: failed to close dependency of %s: %d %s (potential resource leak!) on try %d\n",
                                        root->instanceId, root->id, blobstore_get_error(), blobstore_get_last_msg(), tries);
                        }
                        root->deps[i]->bb = 0; // for debugging
                    }
                    break; // out of the switch statement
                case BLOBSTORE_ERROR_AGAIN: // timed out => the competition took too long
                case BLOBSTORE_ERROR_MFILE: // out of file descriptors for locking => same problem
                    goto retry_or_fail;
                default: // all other errors
                    logprintfl (EUCAERROR, "[%s] error: failed to provision dependency %s for artifact %s (error=%d) on try %d\n", 
                                root->instanceId, root->deps[i]->id, root->id, ret, tries);
                    goto retry_or_fail;
                }
            }
        }
 
        // at this point the dependencies, if any, needed to create
        // the artifact, have been created and opened (i.e. locked
        // for exclusive use by this process and thread)
       
        if (do_create) {
            // try to create the artifact since last time we checked it did not exist
            switch (ret = find_or_create_artifact (CREATE, root, work_bs, cache_bs, work_prefix, &(root->bb))) {
            case OK:
                logprintfl (EUCADEBUG, "[%s] created a blob for an artifact %03d|%s on try %d\n", root->instanceId,  root->seq, root->id, tries);
                break;
            case BLOBSTORE_ERROR_EXIST: // someone else created it => loop back and open it
                ret = BLOBSTORE_ERROR_AGAIN;
                // fall through
            case BLOBSTORE_ERROR_AGAIN: // timed out (but probably exists)
            case BLOBSTORE_ERROR_MFILE: // out of file descriptors for locking => same problem
                goto retry_or_fail;
                break;
            default: // all other errors
                logprintfl (EUCAERROR, "[%s] error: failed to allocate artifact %s (%d %s) on try %d\n", root->instanceId, root->id, ret, blobstore_get_last_msg(), tries);
                goto retry_or_fail;
            }

        create:
            ret = root->creator (root); // create and open this artifact for exclusive use
            if (ret != OK) {
                logprintfl (EUCAERROR, "[%s] error: failed to create artifact %s (error=%d, may retry) on try %d\n", root->instanceId, root->id, ret, tries);
                // delete the partially created artifact so we can retry with a clean slate
                if (root->id_is_path) { // artifact is not a blob, but a file
                    unlink (root->id); // attempt to delete, but it may not even exist

                } else {
                    if (blockblob_delete (root->bb, DELETE_BLOB_TIMEOUT_USEC, 0) == -1) {
                        // failure of 'delete' is bad, since we may have an open blob
                        // that will prevent others from ever opening it again, so at
                        // least try to close it
                        logprintfl (EUCAERROR, "[%s] error: failed to remove partially created artifact %s: %d %s (potential resource leak!) on try %d\n",
                                    root->instanceId, root->id, blobstore_get_error(), blobstore_get_last_msg(), tries);
                        if (blockblob_close (root->bb) == -1) {
                            logprintfl (EUCAERROR, "[%s] error: failed to close partially created artifact %s: %d %s (potential deadlock!) on try %d\n",
                                        root->instanceId, root->id, blobstore_get_error(), blobstore_get_last_msg(), tries);
                        }
                    }
                }
            } else {
                if (root->vbr && root->vbr->type != NC_RESOURCE_EBS)
                    update_vbr_with_backing_info (root);
            }
        }

    retry_or_fail:
        // close all opened dependent blobs, whether we're trying again or returning
        for (int i=0; i<num_opened_deps; i++) {
            blockblob_close (root->deps[i]->bb);
            root->deps[i]->bb = 0; // for debugging
        }
        
    } while ((ret==BLOBSTORE_ERROR_AGAIN || ret==BLOBSTORE_ERROR_MFILE) // only timeout-type error causes us to keep trying
             && ( timeout_usec==0 // indefinitely if there is no timeout at all
                  || (time_usec()-started)<timeout_usec )); // or until we exceed the timeout

    if (ret!=OK) {
        logprintfl (EUCADEBUG, "[%s] error: failed to implement artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
    } else {
        logprintfl (EUCADEBUG, "[%s] implemented artifact %03d|%s on try %d\n", root->instanceId, root->seq, root->id, tries);
    }
    
    return ret;
}

/////////////////////////////////////////////// unit testing code ///////////////////////////////////////////////////

#ifdef _UNIT_TEST

#define BS_SIZE 20000000000/512
#define KEY1 "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCVWU+h3gDF4sGjUB7t...\n"
#define KEY2 "ssh-rsa BBBBB3NzaC1yc2EAAAADAQABAAABAQCVWU+h3gDF4sGjUB7t...\n"
#define EKI1 "eki-1ABC123"
#define ERI1 "eri-1BCD234"
#define EMI1 "emi-1CDE345"
#define EMI2 "emi-2DEF456"
#define GEN_ID gen_id (id, sizeof(id), "12345678")
#define SERIAL_ITERATIONS 3
#define COMPETITIVE_PARTICIPANTS 3
#define COMPETITIVE_ITERATIONS 3

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
    blobstore * bs = blobstore_open (bs_path, size_blocks, BLOBSTORE_FLAG_CREAT, format, revocation, snapshot);
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
        safe_strncpy (vbr->formatName, formatName, sizeof (vbr->formatName));
    if (id)
        safe_strncpy (vbr->id, id, sizeof (vbr->id));
    vbr->format = format;
    vbr->type = type;
    vbr->locationType = locationType;
    vbr->diskNumber = diskNumber;
    vbr->partitionNumber = partitionNumber;
    vbr->guestDeviceBus = guestDeviceBus;
    if (preparedResourceLocation)
        safe_strncpy (vbr->preparedResourceLocation, preparedResourceLocation, sizeof (vbr->preparedResourceLocation));
}

static int next_instances_slot = 0;
static int provisioned_instances = 0;
static pthread_mutex_t competitors_mutex = PTHREAD_MUTEX_INITIALIZER; // process-global mutex
#define TOTAL_VMS 1+SERIAL_ITERATIONS+COMPETITIVE_ITERATIONS*COMPETITIVE_PARTICIPANTS
#define VBR_SIZE ( 2LL * MEGABYTE ) / VBR_SIZE_SCALING
static virtualMachine vm_slots [TOTAL_VMS];
static char vm_ids [TOTAL_VMS][PATH_MAX];
static boolean do_fork = 0;

static int provision_vm (const char * id, const char * sshkey, const char * eki, const char * eri, const char * emi, blobstore * cache_bs, blobstore * work_bs, boolean do_make_work_copy)
{
    pthread_mutex_lock (&competitors_mutex);
    virtualMachine * vm = &(vm_slots [next_instances_slot]); // we don't use vm_slots[] pointers in code
    safe_strncpy (vm_ids [next_instances_slot], id, PATH_MAX);
    next_instances_slot++;
    pthread_mutex_unlock (&competitors_mutex);

    bzero   (vm, sizeof (*vm));
    add_vbr (vm, VBR_SIZE, NC_FORMAT_NONE, "none", eki,    NC_RESOURCE_KERNEL,    NC_LOCATION_NONE, 0, 0, 0, NULL);
    add_vbr (vm, VBR_SIZE, NC_FORMAT_NONE, "none", eri,    NC_RESOURCE_RAMDISK,   NC_LOCATION_NONE, 0, 0, 0, NULL);
    add_vbr (vm, VBR_SIZE, NC_FORMAT_EXT3, "ext3", emi,    NC_RESOURCE_IMAGE,     NC_LOCATION_NONE, 0, 1, BUS_TYPE_SCSI, NULL);
    add_vbr (vm, VBR_SIZE, NC_FORMAT_EXT3, "ext3", "none", NC_RESOURCE_EPHEMERAL, NC_LOCATION_NONE, 0, 3, BUS_TYPE_SCSI, NULL);
    add_vbr (vm, VBR_SIZE, NC_FORMAT_SWAP, "swap", "none", NC_RESOURCE_SWAP,      NC_LOCATION_NONE, 0, 2, BUS_TYPE_SCSI, NULL);

    safe_strncpy (current_instanceId, strstr (id, "/") + 1, sizeof (current_instanceId));
    artifact * sentinel = vbr_alloc_tree (vm, FALSE, do_make_work_copy, sshkey, id);
    if (sentinel == NULL) {
        printf ("error: vbr_alloc_tree failed id=%s\n", id);
        return 1;
    }

    printf ("implementing artifact tree sentinel=%012lx\n", (unsigned long)sentinel);
    int ret;
    if ((ret = art_implement_tree (sentinel, work_bs, cache_bs, id, 1000000LL * 60 * 2)) != OK) {
        printf ("error: art_implement_tree failed ret=%d sentinel=%012lx\n", ret, (unsigned long)sentinel);
        return 1;
    }

    pthread_mutex_lock (&competitors_mutex);
    provisioned_instances++;
    pthread_mutex_unlock (&competitors_mutex);

    printf ("freeing artifact tree sentinel=%012lx\n", (unsigned long)sentinel);
    art_free (sentinel);

    return 0;
}

static int cleanup_vms (void) // cleans up all provisioned VMs
{
    int errors = 0;

    pthread_mutex_lock (&competitors_mutex);
    for (int i=0; i<provisioned_instances; i++) {
        virtualMachine *vm = &(vm_slots [next_instances_slot-i-1]);
        char * id = vm_ids [next_instances_slot-i-1];
        char regex [PATH_MAX];
        snprintf (regex, sizeof (regex), "%s/.*", id);
        errors += (blobstore_delete_regex (work_bs, regex)<0);
    }
    provisioned_instances = 0;
    pthread_mutex_unlock (&competitors_mutex);
    
    return errors;
}

static char * gen_id (char * id, unsigned int id_len, const char * prefix)
{
    snprintf (id, id_len, "%s/i-%08x", prefix, rand());
    return id;
}

static void * competitor_function (void * ptr)
{
    int errors = 0;
    pid_t pid = -1;

    if (do_fork) {
        pid = fork();
        if (pid < 0) { // fork problem
            * (long long *) ptr = 1;
            return NULL;

        } else if (pid > 0) { // parent
            int status;
            waitpid (pid, &status, 0);
            * (long long *) ptr = WEXITSTATUS(status);
            return NULL;
        }
    }

    if (pid<1) {
        printf ("%u/%u: competitor running (provisioned=%d)\n", (unsigned int)pthread_self(), (int)getpid(), provisioned_instances);
        
        for (int i=0; i<COMPETITIVE_ITERATIONS; i++) {
            char id [32];
            errors += provision_vm (GEN_ID, KEY1, EKI1, ERI1, EMI2, cache_bs, work_bs, TRUE);
            usleep ((long long)(100*((double)random()/RAND_MAX)));
        }
        
        printf ("%u/%u: competitor done (provisioned=%d errors=%d)\n", (unsigned int)pthread_self(), (int)getpid(), provisioned_instances, errors);
    }    
    
    if (pid==0) {
        exit (errors);
    }

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
        pclose (f);
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
        printf ("warning: unexpected disk state: [%s] = %d != %d\n", cmd, found, expect);
        return 1;
    }
    return 0;
}

static void dummy_err_fn (const char * msg) 
{ 
    logprintfl (EUCADEBUG, "BLOBSTORE: %s\n", msg);
}

int main (int argc, char ** argv)
{
    char id [32];
    int errors = 0;
    int warnings = 0;
    char cwd [1024];

    getcwd (cwd, sizeof (cwd));
    srandom (time(NULL));
    blobstore_set_error_function (dummy_err_fn);    

    printf ("testing vbr.c\n");

    cache_bs = create_teststore (BS_SIZE, cwd, "cache", BLOBSTORE_FORMAT_DIRECTORY, BLOBSTORE_REVOCATION_LRU, BLOBSTORE_SNAPSHOT_ANY);
    work_bs  = create_teststore (BS_SIZE, cwd, "work", BLOBSTORE_FORMAT_FILES, BLOBSTORE_REVOCATION_NONE, BLOBSTORE_SNAPSHOT_ANY);

    if (cache_bs==NULL || work_bs==NULL) {
        printf ("error: failed to create blobstores\n");
        errors++;
        goto out;
    }

    goto skip_cache_only; // TODO: figure out why only one or the other works

    printf ("running test that only uses cache blobstore\n");
    if (errors += provision_vm (GEN_ID, KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, FALSE))
        goto out;
    printf ("provisioned first VM\n\n\n\n");
    if (errors += provision_vm (GEN_ID, KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, FALSE))
        goto out;
    printf ("provisioned second VM\n\n\n\n");
    if (errors += provision_vm (GEN_ID, KEY2, EKI1, ERI1, EMI1, cache_bs, work_bs, FALSE))
        goto out;
    printf ("provisioned third VM with a different key\n\n\n\n");
    if (errors += provision_vm (GEN_ID, KEY2, EKI1, ERI1, EMI1, cache_bs, work_bs, FALSE))
        goto out;
    printf ("provisioned fourth VM\n\n\n\n");
    if (errors += provision_vm (GEN_ID, KEY2, EKI1, ERI1, EMI2, cache_bs, work_bs, FALSE))
        goto out;
    printf ("provisioned fifth VM with different EMI\n\n\n\n");
    if (errors += provision_vm (GEN_ID, KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, FALSE))
        goto out;

    check_blob (work_bs, "blocks", 0);
    printf ("cleaning cache blobstore\n");
    blobstore_delete_regex (cache_bs, ".*");
    check_blob (cache_bs, "blocks", 0);

    printf ("done with vbr.c cache-only test errors=%d warnings=%d\n", errors, warnings);
    exit (errors);

 skip_cache_only:

    printf ("\n\n\n\n\nrunning test with use of work blobstore\n");

    int emis_in_use = 1;
    if (errors += provision_vm (GEN_ID, KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, TRUE))
        goto out;
#define CHECK_BLOBS \
    warnings += check_blob (cache_bs, "blocks", 4 + 1 * emis_in_use);   \
    warnings += check_blob (work_bs, "blocks", 6 * provisioned_instances);
    CHECK_BLOBS;
    warnings += cleanup_vms();
    CHECK_BLOBS;

    for (int i=0; i<SERIAL_ITERATIONS; i++) {
        errors += provision_vm (GEN_ID, KEY1, EKI1, ERI1, EMI1, cache_bs, work_bs, TRUE);
    }
    if (errors) {
        printf ("error: failed sequential instance provisioning test\n");
    }
    CHECK_BLOBS;
    warnings += cleanup_vms();
    CHECK_BLOBS;

    for (int i=0; i<2; i++) {
        if (i%1) {
            do_fork = 0;
        } else {
            do_fork = 1;
        }
        printf ("===============================================\n");
        printf ("spawning %d competing %s\n", COMPETITIVE_PARTICIPANTS, (do_fork)?("processes"):("threads"));
        emis_in_use++; // we'll have threads creating a new EMI
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
        CHECK_BLOBS;
        warnings += cleanup_vms();
        CHECK_BLOBS;
    }

out:
    printf ("\nfinal check of work blobstore\n");
    check_blob (work_bs, "blocks", 0);
    printf ("cleaning cache blobstore\n");
    blobstore_delete_regex (cache_bs, ".*");
    check_blob (cache_bs, "blocks", 0);

    printf ("done with vbr.c errors=%d warnings=%d\n", errors, warnings);
    exit(errors);
}

#endif
