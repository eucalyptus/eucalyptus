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

//!
//! @file util/data.h
//!
//! Definitions of many key Eucalyptus data structures used by
//! the C-language components.
//!

#ifndef _INCLUDE_DATA_H_
#define _INCLUDE_DATA_H_

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <pthread.h>
#include "eucalyptus.h"
#include "misc.h"                      // boolean

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name String Buffer Sizes
//! Defines various default string buffer size.

#define SMALL_CHAR_BUFFER_SIZE                     64   //!< Small string buffer size
#define CHAR_BUFFER_SIZE                          512   //!< Regular string buffer size
#define BIG_CHAR_BUFFER_SIZE                     1024   //!< Large string buffer size
#define VERY_BIG_CHAR_BUFFER_SIZE				 4096   //!< Extra large string buffer size
#define HOSTNAME_SIZE                             255   //!< Hostname buffer size
#define CREDENTIAL_SIZE                            17   //!< Migration-credential buffer size (16 chars + NULL)
#define MAX_SERVICE_URIS                            8   //!< Maximum number of serivce URIs Euca message can carry
#define IP_BUFFER_SIZE                             32   //!< Maximum size for a buffer holding a string representation of an IP address
#define MAC_BUFFER_SIZE                            32   //!< Maximum size for a buffer holding a string representation of a MAC address

#define KEY_STRING_SIZE				 4096   //! Buffer to hold RSA pub/private keys
//! @}

//! @{
//! @name Volume States
//! Defines various default volume state strings.

#define VOL_STATE_ATTACHING                      "attaching"    //!< The volume attaching state string
#define VOL_STATE_ATTACHED                       "attached" //!< The volume currently attached state string
#define VOL_STATE_ATTACHING_FAILED               "attaching failed" //!< The volume attachment failed state string
#define VOL_STATE_DETACHING                      "detaching"    //!< The volume detaching state string
#define VOL_STATE_DETACHED                       "detached" //!< The volume currently detached state string
#define VOL_STATE_DETACHING_FAILED               "detaching failed" //!< The volume detachment failed state string

//! @}

//! @{
//! @name Guest OS State
//! Defines the strings sent on the wire for guestStateName field of instance struce

#define GUEST_STATE_POWERED_ON  "poweredOn" //!< The instance is found on hypervisor
#define GUEST_STATE_POWERED_OFF "poweredOff"    //!< The instance is not found on hypervisor
//!@}

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

//! Hypervisor capability type enumeration
//! @todo make bit field?
typedef enum _hypervisorCapabilityType {
    HYPERVISOR_UNKNOWN = 0,            //!< Unknown hypervisor capabilities
    HYPERVISOR_XEN_PARAVIRTUALIZED,    //!< Supports XEN paravirtualized
    HYPERVISOR_HARDWARE,               //!< Support hypervisor hardware
    HYPERVISOR_XEN_AND_HARDWARE,       //!< Support XEN and Hardware
    HYPERVISOR_CAPABILITIES_TOTAL,
} hypervisorCapabilityType;

//! LIBVIRT device type enumeration
typedef enum _livirtDevType {
    DEV_TYPE_DISK = 0,                 //!< Disk device type
    DEV_TYPE_FLOPPY,                   //!< Floppy disk device type
    DEV_TYPE_CDROM,                    //!< CDROM device type
} libvirtDevType;

//! LIBVIRT bus type enumeration
typedef enum _libvirtBusType {
    BUS_TYPE_IDE = 0,                  //!< IDE bus type
    BUS_TYPE_SCSI,                     //!< SCSI bus type
    BUS_TYPE_VIRTIO,                   //!< Virtual IO bus type
    BUS_TYPE_XEN,                      //!< XEN bus type
    BUS_TYPES_TOTAL,                   //!< Last ID in array
} libvirtBusType;

//! LIBVIRT source type enumeration
typedef enum _libvirtSourceType {
    SOURCE_TYPE_FILE = 0,              //!< File type source
    SOURCE_TYPE_BLOCK,                 //!< Block type source
} libvirtSourceType;

//! LIBVIRT NIC types enumeration
typedef enum _libvirtNicType {
    NIC_TYPE_NONE,                     //!< Unknown NIC type
    NIC_TYPE_LINUX,                    //!< Linux NIC type
    NIC_TYPE_WINDOWS,                  //!< Windows NIC type
    NIC_TYPE_VIRTIO,                   //!< Virtual IO NIC type
} libvirtNicType;

//! NC Resource Type Enumeration
typedef enum _ncResourceType {
    NC_RESOURCE_IMAGE,                 //!< Image
    NC_RESOURCE_RAMDISK,               //!< Ramdisk
    NC_RESOURCE_KERNEL,                //!< Kernel
    NC_RESOURCE_EPHEMERAL,             //!< Ephemeral
    NC_RESOURCE_SWAP,                  //!< SWAP
    NC_RESOURCE_EBS,                   //!< EBS
    NC_RESOURCE_BOOT,                  //!< BOOTABLE
    NC_RESOURCE_TOTAL,
} ncResourceType;

//! NC Resource Location Type Enumeration
typedef enum _ncResourceLocationType {
    NC_LOCATION_FILE,                  //!< Resource is a local file
    NC_LOCATION_URL,                   //!< URL type location
    NC_LOCATION_IMAGING,               //!< URL of a download manifest
    NC_LOCATION_OBJECT_STORAGE,        //!< Object storage type location
    NC_LOCATION_CLC,                   //!< CLC type location
    NC_LOCATION_SC,                    //!< SC type location
    NC_LOCATION_NONE,                  //!< Unknown type for ephemeral disks
} ncResourceLocationType;

//! NC resource format type
typedef enum _ncResourceFormatType {
    NC_FORMAT_NONE,                    //!< Unknown resource format
    NC_FORMAT_EXT2,                    //!< EXT2 resource format
    NC_FORMAT_EXT3,                    //!< EXT3 resource format
    NC_FORMAT_NTFS,                    //!< NTFC resource format
    NC_FORMAT_SWAP,                    //!< SWAP resource format
} ncResourceFormatType;

//! Enumeration of instance error code
typedef enum instance_error_codes_t {
    OUT_OF_MEMORY = 99,                //!< Out of memory error code
    DUPLICATE,                         //!< Duplicate instance error code
    NOT_FOUND,                         //!< Instance not found error code
} instance_error_codes;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Bundle task structure
typedef struct bundleTask_t {
    char instanceId[CHAR_BUFFER_SIZE]; //!< the instance indentifier string (i-XXXXXXXX) for this bundle task
    char state[CHAR_BUFFER_SIZE];      //!< the state of the bundling task
} bundleTask;

//! Structure defining the public address type
typedef struct publicAddressType_t {
    char uuid[48];                     //!< Unique User Identifier string field
    char sourceAddress[32];            //!< Source address string field
    char destAddress[32];              //!< Destination address field
} publicAddressType;

//! Structure defining the service information
typedef struct serviceInfoType_t {
    char type[32];                     //!< Service type string field
    char name[256];                    //!< Service name string field
    char partition[256];               //!< Assigned partition name
    char uris[MAX_SERVICE_URIS][512];  //!< Service URI list
    int urisLen;                       //!< Number of service URI in the list (a value of -1 indicates an error with the URIS)
} serviceInfoType;

//! Structure defining the service status
typedef struct serviceStatusType_t {
    char localState[32];               //!< Local service state string
    int localEpoch;                    //!< Last status update local time in EPOCH
    char details[1024];                //!< Service status detail
    serviceInfoType serviceId;         //!< Service information
} serviceStatusType;

//! Structure defining the NC metadata
typedef struct ncMetadata_t {
    char *correlationId;               //!< Request Correlation Identifier
    char *userId;                      //!< User identifier
    char *nodeName;                    //!< Name/IP of the node the request is bound for (optional)
    int epoch;                         //!< Request timestamp in EPOCH format
    serviceInfoType services[16];      //!< List of services available
    serviceInfoType disabledServices[16];   //!< List of disabled services
    serviceInfoType notreadyServices[16];   //!< List of unavailable services
    int servicesLen;                   //!< Number of available services in the available list (a value of -1 indicates an error with the services)
    int disabledServicesLen;           //!< Number of disabled services in the disabled list (a value of -1 indicates an error with the services)
    int notreadyServicesLen;           //!< Number of unavailable services in the not ready list (a value of -1 indicates an error with the services)
    char *replyString;                 //!< If set, can be used to propagate error messages from handlers to marshalling code (and to the user)
} ncMetadata;

//! Structure defining the virtual boot record
typedef struct virtualBootRecord_t {
    //! @{
    //! @name first six fields arrive in requests (RunInstance, {Attach|Detach} Volume)
    char resourceLocation[CHAR_BUFFER_SIZE];    //!< http|objectstorage|cloud|sc|iqn|aoe://... or none
    char guestDeviceName[SMALL_CHAR_BUFFER_SIZE];   //!< x?[vhsf]d[a-z]?[1-9]*
    long long sizeBytes;               //!< Size of the boot record in bytes
    char formatName[SMALL_CHAR_BUFFER_SIZE];    //!< ext2|ext3|swap|none
    char id[SMALL_CHAR_BUFFER_SIZE];   //!< emi|eki|eri|vol|none
    char typeName[SMALL_CHAR_BUFFER_SIZE];  //!< machine|kernel|ramdisk|ephemeral|ebs
    //! @}

    //! @{
    //! @name the remaining fields are set by NC
    ncResourceType type;               //!< NC_RESOURCE_{IMAGE|RAMDISK|...}
    ncResourceLocationType locationType;    //!< NC_LOCATION_{URL|OBJECT_STORAGE...}
    ncResourceFormatType format;       //!< NC_FORMAT_{NONE|EXT2|EXT3|SWAP}
    int diskNumber;                    //!< 0 = [sh]da or fd0, 1 = [sh]db or fd1, etc.
    int partitionNumber;               //!< 0 = whole disk, 1 = partition 1, etc.
    libvirtDevType guestDeviceType;    //!< DEV_TYPE_{DISK|FLOPPY|CDROM}
    libvirtBusType guestDeviceBus;     //!< BUS_TYPE_{IDE|SCSI|VIRTIO|XEN}
    libvirtSourceType backingType;     //!< SOURCE_TYPE_{FILE|BLOCK}
    char backingPath[CHAR_BUFFER_SIZE]; //!< path to file or block device that backs the resource
    char preparedResourceLocation[VERY_BIG_CHAR_BUFFER_SIZE];   //!< e.g., URL + resourceLocation for Walrus downloads, sc url for ebs volumes prior to SC call, then connection string for ebs volumes returned from SC
    //! @}
} virtualBootRecord;

//! Structure defining a virtual machine
typedef struct virtualMachine_t {
    int mem;                           //!< Available memory
    int cores;                         //!< Available number of cores
    int disk;                          //!< Number of disk
    char name[64];                     //!< Name of virtual machine
    virtualBootRecord *root;           //!< Root boot record information
    virtualBootRecord *kernel;         //!< kernel boot record information
    virtualBootRecord *ramdisk;        //!< Ramdisk boot record information
    virtualBootRecord *swap;           //!< SWAP boot record information
    virtualBootRecord *ephemeral0;     //!< Ephemeral boot record information
    virtualBootRecord *boot;           //!< Boot sector
    virtualBootRecord virtualBootRecord[EUCA_MAX_VBRS]; //!< List of virtual boot records
    int virtualBootRecordLen;          //!< Number of VBRS in the list
    libvirtNicType nicType;            //!< Defines the virtual machine NIC type
    char guestNicDeviceName[64];       //!< Defines the guest NIC device name
} virtualMachine;

//! Structure defining the network configuration
typedef struct netConfig_t {
    int vlan;                          //!< Virtual LAN
    int networkIndex;                  //!< Network index
    char privateMac[MAC_BUFFER_SIZE];  //!< Private MAC address
    char publicIp[IP_BUFFER_SIZE];     //!< Public IP address
    char privateIp[IP_BUFFER_SIZE];    //!< Private IP address
} netConfig;

//! Structure defining NC Volumes
typedef struct ncVolume_t {
    char volumeId[CHAR_BUFFER_SIZE];   //!< Remote volume identifier string
    char attachmentToken[CHAR_BUFFER_SIZE]; //!< Remote device name string, the token reference
    char localDev[CHAR_BUFFER_SIZE];   //!< Local device name string
    char localDevReal[CHAR_BUFFER_SIZE];    //!< Local device name (real) string
    char stateName[CHAR_BUFFER_SIZE];  //!< Volume state name string
    char connectionString[VERY_BIG_CHAR_BUFFER_SIZE];   //!< Volume Token for attachment/detachment
} ncVolume;

//TODO: zhill, use this in the CC instead of ncVolume to save mem. Need to change the adb-helpers as well to copy nc->cc
//! Notably smaller structure for volumes on the CC since the connectionString is not part of CC state
typedef struct ccVolume_t {
    char volumeId[CHAR_BUFFER_SIZE];   //!< Remote volume identifier string
    char attachmentToken[CHAR_BUFFER_SIZE]; //!< Remote device name string, the token reference
    char localDev[CHAR_BUFFER_SIZE];   //!< Local device name string
    char localDevReal[CHAR_BUFFER_SIZE];    //!< Local device name (real) string
    char stateName[CHAR_BUFFER_SIZE];  //!< Volume state name string
} ccVolume;

//! Structure definint NC instances
typedef struct ncInstance_t {
    char uuid[CHAR_BUFFER_SIZE];       //!< Unique user identifier string
    char instanceId[CHAR_BUFFER_SIZE]; //!< Instance identifier string
    char reservationId[CHAR_BUFFER_SIZE];   //!< Reservation identifier string
    char userId[CHAR_BUFFER_SIZE];     //!< User identifier string
    char ownerId[CHAR_BUFFER_SIZE];    //!< Owner identifier string
    char accountId[CHAR_BUFFER_SIZE];  //!< Account identifier string
    char imageId[SMALL_CHAR_BUFFER_SIZE];   //!< Image identifier string
    char kernelId[SMALL_CHAR_BUFFER_SIZE];  //!< Kernel image identifier string
    char ramdiskId[SMALL_CHAR_BUFFER_SIZE]; //!< Ramdisk image identifier string
    int retries;                       //!< Number of times we try to communicate with LIBVIRT about this instance

    //! @{
    //! @name state as reported to CC & CLC
    char stateName[CHAR_BUFFER_SIZE];  //!< Instance state as a string
    char bundleTaskStateName[CHAR_BUFFER_SIZE]; //!< Instance's bundle task state as a string
    char createImageTaskStateName[CHAR_BUFFER_SIZE];    //!< Instance's image task state as a string
    //! @}

    int stateCode;                     //!< Instance state code as an integer value

    //! @{
    //! @name state as NC thinks of it
    instance_states state;             //!< Instance state
    bundling_progress bundleTaskState; //!< Bundling task progress state
    int bundlePid;                     //!< Bundling task PID value
    int bundleBucketExists;            //!< Boolean indicating if the bundle's bucket already exists
    int bundleCanceled;                //!< Boolean indicating if the bundle has been cancelled
    //! @}

    createImage_progress createImageTaskState;  //!< Image creation task progress state
    int createImagePid;                //!< Image creationg task PID value
    int createImageCanceled;           //!< Boolean indicating if the image creation task has been cancelled

    migration_states migration_state;  //!< Migration state
    char migration_src[HOSTNAME_SIZE]; //!< Name of the host from which the instance is being or needs to be migrated
    char migration_dst[HOSTNAME_SIZE]; //!< Name of the host to which the instance is being or needs to be migrated
    char migration_credentials[CREDENTIAL_SIZE];    //!< Migration shared secret

    char keyName[CHAR_BUFFER_SIZE * 4]; //!< Name of the key to use for this instance
    char privateDnsName[CHAR_BUFFER_SIZE];  //!< Private DNS name
    char dnsName[CHAR_BUFFER_SIZE];    //!< DNS name
    int launchTime;                    //!< timestamp of RunInstances request arrival
    int expiryTime;                    //!< timestamp of instance ->RUNNING expiration
    int bootTime;                      //!< timestamp of STAGING->BOOTING transition
    int bundlingTime;                  //!< timestamp of ->BUNDLING transition
    int createImageTime;               //!< timestamp of ->CREATEIMAGE transition
    int terminationTime;               //!< timestamp of when resources are released (->TEARDOWN transition)
    int migrationTime;                 //!< timestamp of migration request

    virtualMachine params;             //!< Virtual machine parameters
    netConfig ncnet;                   //!< Network configuration information
    pthread_t tcb;                     //!< Instance thread
    char instancePath[CHAR_BUFFER_SIZE];    //!< instance blobstore path

    //! @{
    //! @name information needed for generating libvirt XML
    char xmlFilePath[CHAR_BUFFER_SIZE]; //!< Instance XML file path name
    char libvirtFilePath[CHAR_BUFFER_SIZE]; //!< LIBVIRT file path name
    char consoleFilePath[CHAR_BUFFER_SIZE]; //!< console file path name
    char floppyFilePath[CHAR_BUFFER_SIZE];  //!< Floppy disk path name
    char hypervisorType[SMALL_CHAR_BUFFER_SIZE];    //!< Hypervisor type as a string
    hypervisorCapabilityType hypervisorCapability;  //!< What is the hypervisor capable of for this instance
    int hypervisorBitness;             //!< Hypervisor bitness (32 / 64 bits supported)
    boolean combinePartitions;         //!< hypervisor works only with disks (all except Xen)
    boolean do_inject_key;             //!< whether or not NC injects SSH key into this instance (eucalyptus.conf option)
    //! @}

    //! @{
    //! @name passed into NC via runInstances for safekeeping
    char userData[CHAR_BUFFER_SIZE * 32];   //!< user data to pass to the instance
    char launchIndex[CHAR_BUFFER_SIZE]; //!< the launch index for this instance
    char platform[CHAR_BUFFER_SIZE];   //!< the platform used for this instance (typically 'windows' or 'linux')
    char groupNames[EUCA_MAX_GROUPS][CHAR_BUFFER_SIZE]; //!< Network groups assigned to this instance.
    int groupNamesSize;                //!< Number of network groups.
    //! @}

    //! @{
    //! @name updated by NC upon Attach/DetachVolume
    ncVolume volumes[EUCA_MAX_VOLUMES]; //!< Instance's attached volume information
    //! @}

    //! @{
    //! @name reported by NC back, for report generation
    long long blkbytes;                //!< Number of block bytes
    long long netbytes;                //!< Number of network bytes
    time_t last_stat;                  //!< Last time these statistics were updated
    //! @}

    //! @{
    //! @name fields added in 3.4 for instance start/stop support
    char guestStateName[CHAR_BUFFER_SIZE];  //!< Guest OS state of the instance (see GUEST_STATE_* defines below)
    boolean stop_requested;            //!< instance was stopped and not yet restarted
    //! @}
    //

    char euareKey[KEY_STRING_SIZE];    //!<public key of Euare service that authorizes the instance
    char instancePubkey[KEY_STRING_SIZE];   //!<instance's public key
    char instanceToken[BIG_CHAR_BUFFER_SIZE];   //!< token from Euare service that proves the instances' authorization
    char instancePk[KEY_STRING_SIZE];  //!<instance's private key
} ncInstance;

//! Structure defining NC resource information
typedef struct ncResource_t {
    char nodeStatus[CHAR_BUFFER_SIZE]; //!< Node status as a string
    char iqn[CHAR_BUFFER_SIZE];        //!< IQN
    boolean migrationCapable;          //!< Whether NC is capable of live-migrating VM without shared storage
    int memorySizeMax;                 //!< Maximum memory size supported by this node controller
    int memorySizeAvailable;           //!< Currently available memory on this node controller
    int diskSizeMax;                   //!< Maximum disk size supported by this node controller
    int diskSizeAvailable;             //!< Currently available disk size on this node controller
    int numberOfCoresMax;              //!< Maximum number of core supported by this node controller
    int numberOfCoresAvailable;        //!< Currently available number of core on this node controller
    char publicSubnets[CHAR_BUFFER_SIZE];   //!< Public subnet configured on this node controller
} ncResource;

//! Instance list node structure
//! @todo make this into something smarter than a linked list
typedef struct bunchOfInstances_t {
    ncInstance *instance;              //!< Pointer to this node's assigned instance
    int count;                         //!< Number of instances in the list. Only valid on first node.
    struct bunchOfInstances_t *next;   //!< Pointer to our next node.
} bunchOfInstances;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! List of string to convert the hypervisor capability types enumeration
extern const char *hypervisorCapabilityTypeNames[];

//! List of string to convert the LIBVIRT device type enumeration
extern const char *libvirtDevTypeNames[];

//! List of string to convert the LIBVIRT bus types enumeration
extern const char *libvirtBusTypeNames[];

//! List of string to convert the LIBVIRT source types enumeration
extern const char *libvirtSourceTypeNames[];

//! List of string to convert the LIBVIRT NIC types enumeration
extern const char *libvirtNicTypeNames[];

//! List of string to convert the NC resource types enumeration
extern const char *ncResourceTypeNames[];

//! List of strings for converting VBR resource location type enums
extern const char *ncResourceLocationTypeNames[];

//! List of strings for converting VBR resource format enumss
extern const char *ncResourceFormatTypeNames[];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int allocate_virtualMachine(virtualMachine * pVirtMachineOut, const virtualMachine * pVirtMachingIn);
int allocate_netConfig(netConfig * pNetCfg, const char *sPvMac, const char *sPvIp, const char *sPbIp, int vlan, int networkIndex);

//! @{
//! @name Metadata APIs
ncMetadata *allocate_metadata(const char *sCorrelationId, const char *sUserId) _attribute_wur_;
void free_metadata(ncMetadata ** ppMeta);
//! @}

//! @{
//! @name Instances APIs
ncInstance *allocate_instance(const char *sUUID, const char *sInstanceId, const char *sReservationId, virtualMachine * pVirtMachine,
                              const char *sStateName, int stateCode, const char *sUserId, const char *sOwnerId, const char *sAccountId,
                              netConfig * pNetCfg, const char *sKeyName, const char *sUserData, const char *sLaunchIndex, const char *sPlatform,
                              int expiryTime, char **asGroupNames, int groupNamesSize) _attribute_wur_;
ncInstance *clone_instance(const ncInstance * old_instance);
void free_instance(ncInstance ** ppInstance);
int add_instance(bunchOfInstances ** ppHead, ncInstance * pInstance);
int remove_instance(bunchOfInstances ** ppHead, ncInstance * pInstance);
int for_each_instance(bunchOfInstances ** ppHead, void (*pFunction) (bunchOfInstances **, ncInstance *, void *), void *pParam);
ncInstance *find_instance(bunchOfInstances ** ppHead, const char *instanceId);
ncInstance *get_instance(bunchOfInstances ** ppHead);
int total_instances(bunchOfInstances ** ppHead);
//! @}

//! @{
//! @name Resources APIs
ncResource *allocate_resource(const char *sNodeStatus, boolean migrationCapable, const char *sIQN, int memorySizeMax, int memorySizeAvailable, int diskSizeMax,
                              int diskSizeAvailable, int numberOfCoresMax, int numberOfCoresAvailable, const char *sPublicSubnets) _attribute_wur_;
void free_resource(ncResource ** ppresource);
//! @}

//! @{
//! @name Volumes APIs
boolean is_volume_used(const ncVolume * pVolume);
ncVolume *save_volume(ncInstance * pInstance, const char *sVolumeId, const char *sVolumeAttachementToken, const char *sRemoteDev, const char *sLocalDev, const char *sLocalDevReal,
                      const char *sStateName);
ncVolume *free_volume(ncInstance * pInstance, const char *sVolumeId);
//! @}

//! @{
//! @name Bundling Task APIs
bundleTask *allocate_bundleTask(ncInstance * pInstance) _attribute_wur_;
//! @}

instance_states instance_state_from_string(const char *instance_state_name);
bundling_progress bundling_progress_from_string(const char *bundling_progress_name);
createImage_progress createImage_progress_from_string(const char *createImage_progress_name);
migration_states migration_state_from_string(const char *migration_state_name);
hypervisorCapabilityType hypervisorCapabilityType_from_string(const char *type_name);
ncResourceType ncResourceType_from_string(const char *str);
ncResourceLocationType ncResourceLocationType_from_string(const char *str);
ncResourceFormatType ncResourceFormatType_from_string(const char *str);
libvirtDevType libvirtDevType_from_string(const char *str);
libvirtBusType libvirtBusType_from_string(const char *str);
libvirtSourceType libvirtSourceType_from_string(const char *str);
libvirtNicType libvirtNicType_from_string(const char *str);

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_DATA_H_ */
