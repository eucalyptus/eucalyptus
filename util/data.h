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

#ifndef INCLUDE_DATA_H
#define INCLUDE_DATA_H

#include <pthread.h>
#include "eucalyptus.h"
#include "misc.h" // boolean

#define SMALL_CHAR_BUFFER_SIZE 64
#define CHAR_BUFFER_SIZE 512
#define BIG_CHAR_BUFFER_SIZE 1024

typedef struct publicAddressType_t {
  char uuid[48];
  char sourceAddress[32];
  char destAddress[32];
} publicAddressType;

typedef struct serviceInfoType_t {
  char type[32];
  char name[32];
  char uris[8][512];
  int urisLen;
} serviceInfoType;

typedef struct serviceStatusType_t {
  char localState[32];
  int localEpoch;
  char details[1024];
  serviceInfoType serviceId;
} serviceStatusType;

typedef struct ncMetadata_t {
    char *correlationId;
    char *userId;
    int epoch;
    serviceInfoType services[16], disabledServices[16], notreadyServices[16];
    int servicesLen, disabledServicesLen, notreadyServicesLen;
} ncMetadata;

typedef enum _hypervisorCapabilityType { // TODO: make bit field?
    HYPERVISOR_UNKNOWN = 0,
    HYPERVISOR_XEN_PARAVIRTUALIZED,
    HYPERVISOR_HARDWARE,
    HYPERVISOR_XEN_AND_HARDWARE
} hypervisorCapabilityType;

static char * hypervsorCapabilityTypeNames [] = {
    "unknown",
    "xen",
    "hw",
    "xen+hw"
};

typedef enum _livirtDevType {
    DEV_TYPE_DISK = 0,
    DEV_TYPE_FLOPPY,
    DEV_TYPE_CDROM
} libvirtDevType;

static char * libvirtDevTypeNames [] = {
    "disk",
    "floppy",
    "cdrom"
};
  
typedef enum _libvirtBusType {
    BUS_TYPE_IDE = 0,
    BUS_TYPE_SCSI,
    BUS_TYPE_VIRTIO,
    BUS_TYPE_XEN,
    BUS_TYPES_TOTAL,
} libvirtBusType;

static char * libvirtBusTypeNames [] = {
    "ide",
    "scsi",
    "virtio",
    "xen"
};

typedef enum _libvirtSourceType {
    SOURCE_TYPE_FILE = 0,
    SOURCE_TYPE_BLOCK 
} libvirtSourceType;

static char * libvirtSourceTypeNames [] = {
    "file",
    "block"
};

typedef enum _libvirtNicType {
    NIC_TYPE_NONE,
    NIC_TYPE_LINUX,
    NIC_TYPE_WINDOWS,
    NIC_TYPE_VIRTIO
} libvirtNicType;

static char * libvirtNicTypeNames [] = {
    "none",
    "e1000",
    "rtl8139",
    "virtio"
};

typedef enum _ncResourceType {
    NC_RESOURCE_IMAGE,
    NC_RESOURCE_RAMDISK,
    NC_RESOURCE_KERNEL,
    NC_RESOURCE_EPHEMERAL,
    NC_RESOURCE_SWAP,
    NC_RESOURCE_EBS 
} ncResourceType;

static char * ncResourceTypeName [] = {
    "image",
    "ramdisk",
    "kernel",
    "ephemeral",
    "swap",
    "ebs"
};

typedef enum _ncResourceLocationType {
    NC_LOCATION_URL,
    NC_LOCATION_WALRUS,
    NC_LOCATION_CLC,
    NC_LOCATION_SC,
    NC_LOCATION_IQN,
    NC_LOCATION_AOE,
    NC_LOCATION_NONE // for ephemeral disks
} ncResourceLocationType;

typedef enum _ncResourceFormatType {
    NC_FORMAT_NONE,
    NC_FORMAT_EXT2,
    NC_FORMAT_EXT3,
    NC_FORMAT_NTFS,
    NC_FORMAT_SWAP
} ncResourceFormatType;

typedef struct virtualBootRecord_t {
    // first six fields arrive in requests (RunInstance, {Attach|Detach}Volume)
    char resourceLocation[CHAR_BUFFER_SIZE]; // http|walrus|cloud|sc|iqn|aoe://... or none
    char guestDeviceName[SMALL_CHAR_BUFFER_SIZE]; // x?[vhsf]d[a-z]?[1-9]*
    long long size; // in bytes
    char formatName[SMALL_CHAR_BUFFER_SIZE]; // ext2|ext3|swap|none
    char id [SMALL_CHAR_BUFFER_SIZE]; // emi|eki|eri|vol|none
    char typeName [SMALL_CHAR_BUFFER_SIZE]; // machine|kernel|ramdisk|ephemeral|ebs

    // the remaining fields are set by NC
    ncResourceType type; // NC_RESOURCE_{IMAGE|RAMDISK|...}
    ncResourceLocationType locationType; // NC_LOCATION_{URL|WALRUS...}
    ncResourceFormatType format; // NC_FORMAT_{NONE|EXT2|EXT3|SWAP}
    int diskNumber; // 0 = [sh]da or fd0, 1 = [sh]db or fd1, etc.
    int partitionNumber; // 0 = whole disk, 1 = partition 1, etc.
    libvirtDevType guestDeviceType; // DEV_TYPE_{DISK|FLOPPY|CDROM}
    libvirtBusType guestDeviceBus; // BUS_TYPE_{IDE|SCSI|VIRTIO|XEN}
    libvirtSourceType backingType; // SOURCE_TYPE_{FILE|BLOCK}
    char backingPath [CHAR_BUFFER_SIZE]; // path to file or block device that backs the resource
    char preparedResourceLocation[CHAR_BUFFER_SIZE]; // e.g., URL + resourceLocation for Walrus downloads
} virtualBootRecord;

typedef struct virtualMachine_t {
    int mem, cores, disk;
    char name[64];
    virtualBootRecord * root;
    virtualBootRecord * kernel;
    virtualBootRecord * ramdisk;
    virtualBootRecord * swap;
    virtualBootRecord * ephemeral0;
    virtualBootRecord virtualBootRecord[EUCA_MAX_VBRS];
    int virtualBootRecordLen;
    libvirtNicType nicType;
    char guestNicDeviceName[64];
} virtualMachine;

int allocate_virtualMachine(virtualMachine *out, const virtualMachine *in);

typedef struct netConfig_t {
    int vlan, networkIndex;
    char privateMac[24], publicIp[24], privateIp[24];
} netConfig;
int allocate_netConfig(netConfig *out, char *pvMac, char *pvIp, char *pbIp, int vlan, int networkIndex);

#define VOL_STATE_ATTACHING        "attaching"
#define VOL_STATE_ATTACHED         "attached"
#define VOL_STATE_ATTACHING_FAILED "attaching failed"
#define VOL_STATE_DETACHING        "detaching"
#define VOL_STATE_DETACHED         "detached"
#define VOL_STATE_DETACHING_FAILED "detaching failed"

typedef struct ncVolume_t {
    char volumeId[CHAR_BUFFER_SIZE];
    char remoteDev[CHAR_BUFFER_SIZE];
    char localDev[CHAR_BUFFER_SIZE];
    char localDevReal[CHAR_BUFFER_SIZE];
    char stateName[CHAR_BUFFER_SIZE];
} ncVolume;

typedef struct ncInstance_t {
    char uuid[CHAR_BUFFER_SIZE];
    char instanceId[CHAR_BUFFER_SIZE];
    char reservationId[CHAR_BUFFER_SIZE];
    char userId[CHAR_BUFFER_SIZE];
    char ownerId[CHAR_BUFFER_SIZE];
    char accountId[CHAR_BUFFER_SIZE];
    char imageId[SMALL_CHAR_BUFFER_SIZE];
    char kernelId[SMALL_CHAR_BUFFER_SIZE];
    char ramdiskId[SMALL_CHAR_BUFFER_SIZE];
    int retries;
    
    // state as reported to CC & CLC
    char stateName[CHAR_BUFFER_SIZE];  // as string
    char bundleTaskStateName[CHAR_BUFFER_SIZE];  /* as string */
    char createImageTaskStateName[CHAR_BUFFER_SIZE];  /* as string */

    int stateCode; /* as int */

    // state as NC thinks of it
    instance_states state;
    bundling_progress bundleTaskState;
    int bundlePid, bundleBucketExists, bundleCanceled;
  
    createImage_progress createImageTaskState;
    int createImagePid, createImageCanceled;

    char keyName[CHAR_BUFFER_SIZE*4];
    char privateDnsName[CHAR_BUFFER_SIZE];
    char dnsName[CHAR_BUFFER_SIZE];
    int launchTime; // timestamp of RunInstances request arrival
    int expiryTime;
    int bootTime; // timestamp of STAGING->BOOTING transition
    int bundlingTime; // timestamp of ->BUNDLING transition
    int createImageTime; // timestamp of ->CREATEIMAGE transition
    int terminationTime; // timestamp of when resources are released (->TEARDOWN transition)
    
    virtualMachine params;
    netConfig ncnet;
    pthread_t tcb;
    char instancePath [CHAR_BUFFER_SIZE];

    // information needed for generating libvirt XML
    char xmlFilePath [CHAR_BUFFER_SIZE];
    char libvirtFilePath [CHAR_BUFFER_SIZE];
    char consoleFilePath [CHAR_BUFFER_SIZE];
    char floppyFilePath [CHAR_BUFFER_SIZE];
    char hypervisorType [SMALL_CHAR_BUFFER_SIZE];
    hypervisorCapabilityType hypervisorCapability;
    int hypervisorBitness;
    boolean combinePartitions; // hypervisor works only with disks (all except Xen)
    boolean do_inject_key; // whether or not NC injects SSH key into this instance (eucalyptus.conf option)

    // passed into NC via runInstances for safekeeping
    char userData[CHAR_BUFFER_SIZE*32];
    char launchIndex[CHAR_BUFFER_SIZE];
    char platform[CHAR_BUFFER_SIZE];
    char groupNames[EUCA_MAX_GROUPS][CHAR_BUFFER_SIZE];
    int groupNamesSize;

    // updated by NC upon Attach/DetachVolume
    ncVolume volumes[EUCA_MAX_VOLUMES];

    // reported by NC back, for report generation
    long long blkbytes, netbytes;
} ncInstance;

typedef struct ncResource_t {
    char nodeStatus[CHAR_BUFFER_SIZE];
    char iqn[CHAR_BUFFER_SIZE];
    int memorySizeMax;
    int memorySizeAvailable;
    int diskSizeMax;
    int diskSizeAvailable;
    int numberOfCoresMax;
    int numberOfCoresAvailable;
    char publicSubnets[CHAR_BUFFER_SIZE];
} ncResource;

// TODO: make this into something smarter than a linked list
typedef struct bunchOfInstances_t {
    ncInstance * instance;
    int count; // only valid on first node
    struct bunchOfInstances_t * next;
} bunchOfInstances;

typedef enum instance_error_codes_t {
    OUT_OF_MEMORY = 99,
    DUPLICATE,
    NOT_FOUND,
} instance_error_codes;

instance_error_codes add_instance (bunchOfInstances ** head, ncInstance * instance);
instance_error_codes remove_instance (bunchOfInstances ** head, ncInstance * instance);
instance_error_codes for_each_instance (bunchOfInstances ** headp, void (* function)(bunchOfInstances **, ncInstance *, void *), void *);
ncInstance * find_instance (bunchOfInstances **headp, char * instanceId);
ncInstance * get_instance (bunchOfInstances **head);
int total_instances (bunchOfInstances **head);

ncMetadata * allocate_metadata(char *correlationId, char *userId);
void free_metadata(ncMetadata ** meta);

ncInstance * allocate_instance(char *uuid,
                               char *instanceId, 
                               char *reservationId, 
                               virtualMachine *params, 
                               char *stateName, 
                               int stateCode, 
                               char *userId, 
                               char *ownerId,
                               char *accountId,
                               netConfig *ncnet, 
                               char *keyName,
                               char *userData, 
                               char *launchIndex, 
                               char *platform, 
                               int expiryTime, 
                               char **groupNames, 
                               int groupNamesSize);
void free_instance (ncInstance ** inst);

ncResource * allocate_resource(char *nodeStatus, 
                               char *iqn,
                               int memorySizeMax, int memorySizeAvailable, 
                               int diskSizeMax, int diskSizeAvailable,
                               int numberOfCoresMax, int numberOfCoresAvailable,
                               char *publicSubnets);
void free_resource(ncResource ** res);

int is_volume_used (const ncVolume * v);
ncVolume * save_volume (ncInstance * instance, const char *volumeId, const char *remoteDev, const char *localDev, const char *localDevReal, const char *stateName);
ncVolume * free_volume (ncInstance * instance, const char *volumeId);

#endif

