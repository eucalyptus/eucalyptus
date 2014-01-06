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
//! @file util/instance33.h
//!
//! Definitions of ncInstance struct as of versions 3.3.0 and 3.3.1. 
//! They are preserved in this file to enable upgrade from those versions, 
//! which relied on binary struct checkpoints of the instance struct, to
//! later versions, which rely on an XML checkpoint of the instance struct.
//!

#ifndef _INCLUDE_INSTANCE33_H_
#define _INCLUDE_INSTANCE33_H_

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <pthread.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name String Buffer Sizes
//! Defines various default string buffer size.

#define SMALL_CHAR_BUFFER_SIZE33                     64 //!< Small string buffer size
#define CHAR_BUFFER_SIZE33                          512 //!< Regular string buffer size
#define BIG_CHAR_BUFFER_SIZE33                     1024 //!< Large string buffer size
#define VERY_BIG_CHAR_BUFFER_SIZE33				 4096   //!< Extra large string buffer size
#define HOSTNAME_SIZE33                             255 //!< Hostname buffer size
#define CREDENTIAL_SIZE33                            17 //!< Migration-credential buffer size (16 chars + NULL)
#define MAX_SERVICE_URIS33                            8 //!< Maximum number of serivce URIs Euca message can carry

#define EUCA_MAX_VBRS33                              64 //!< Number of Virtual Boot Record supported
#define EUCA_MAX_GROUPS33                            64
#define EUCA_MAX_VOLUMES33                           27
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef unsigned char boolean33;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Hypervisor capability type enumeration
//! @todo make bit field?
typedef enum _hypervisorCapabilityType33 {
    HYPERVISOR_UNKNOWN33 = 0,          //!< Unknown hypervisor capabilities
    HYPERVISOR_XEN_PARAVIRTUALIZED33,  //!< Supports XEN paravirtualized
    HYPERVISOR_HARDWARE33,             //!< Support hypervisor hardware
    HYPERVISOR_XEN_AND_HARDWARE33,     //!< Support XEN and Hardware
} hypervisorCapabilityType33;

//! LIBVIRT device type enumeration
typedef enum _livirtDevType33 {
    DEV_TYPE_DISK33 = 0,               //!< Disk device type
    DEV_TYPE_FLOPPY33,                 //!< Floppy disk device type
    DEV_TYPE_CDROM33,                  //!< CDROM device type
} libvirtDevType33;

//! LIBVIRT bus type enumeration
typedef enum _libvirtBusType33 {
    BUS_TYPE_IDE33 = 0,                //!< IDE bus type
    BUS_TYPE_SCSI33,                   //!< SCSI bus type
    BUS_TYPE_VIRTIO33,                 //!< Virtual IO bus type
    BUS_TYPE_XEN33,                    //!< XEN bus type
    BUS_TYPES_TOTAL33,                 //!< Last ID in array
} libvirtBusType33;

//! LIBVIRT source type enumeration
typedef enum _libvirtSourceType33 {
    SOURCE_TYPE_FILE33 = 0,            //!< File type source
    SOURCE_TYPE_BLOCK33,               //!< Block type source
} libvirtSourceType33;

//! LIBVIRT NIC types enumeration
typedef enum _libvirtNicType33 {
    NIC_TYPE_NONE33,                   //!< Unknown NIC type
    NIC_TYPE_LINUX33,                  //!< Linux NIC type
    NIC_TYPE_WINDOWS33,                //!< Windows NIC type
    NIC_TYPE_VIRTIO33,                 //!< Virtual IO NIC type
} libvirtNicType33;

//! NC Resource Type Enumeration
typedef enum _ncResourceType33 {
    NC_RESOURCE_IMAGE33,               //!< Image
    NC_RESOURCE_RAMDISK33,             //!< Ramdisk
    NC_RESOURCE_KERNEL33,              //!< Kernel
    NC_RESOURCE_EPHEMERAL33,           //!< Ephemeral
    NC_RESOURCE_SWAP33,                //!< SWAP
    NC_RESOURCE_EBS33,                 //!< EBS
    NC_RESOURCE_BOOT33,                //!< BOOTABLE
} ncResourceType33;

//! NC Resource Location Type Enumeration
typedef enum _ncResourceLocationType33 {
    NC_LOCATION_URL33,                 //!< URL type location
    NC_LOCATION_WALRUS33,              //!< Walrus type location
    NC_LOCATION_CLC33,                 //!< CLC type location
    NC_LOCATION_SC33,                  //!< SC type location
    NC_LOCATION_NONE33,                //!< Unknown type for ephemeral disks
} ncResourceLocationType33;

//! NC resource format type
typedef enum _ncResourceFormatType33 {
    NC_FORMAT_NONE33,                  //!< Unknown resource format
    NC_FORMAT_EXT233,                  //!< EXT2 resource format
    NC_FORMAT_EXT333,                  //!< EXT3 resource format
    NC_FORMAT_NTFS33,                  //!< NTFC resource format
    NC_FORMAT_SWAP33,                  //!< SWAP resource format
} ncResourceFormatType33;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Structure defining the virtual boot record
typedef struct virtualBootRecord33_t {
    //! @{
    //! @name first six fields arrive in requests (RunInstance, {Attach|Detach} Volume)
    char resourceLocation[CHAR_BUFFER_SIZE33];  //!< http|objectstorage|cloud|sc|iqn|aoe://... or none
    char guestDeviceName[SMALL_CHAR_BUFFER_SIZE33]; //!< x?[vhsf]d[a-z]?[1-9]*
    long long sizeBytes;               //!< Size of the boot record in bytes
    char formatName[SMALL_CHAR_BUFFER_SIZE33];  //!< ext2|ext3|swap|none
    char id[SMALL_CHAR_BUFFER_SIZE33]; //!< emi|eki|eri|vol|none
    char typeName[SMALL_CHAR_BUFFER_SIZE33];    //!< machine|kernel|ramdisk|ephemeral|ebs
    //! @}

    //! @{
    //! @name the remaining fields are set by NC
    ncResourceType33 type;             //!< NC_RESOURCE_{IMAGE|RAMDISK|...}
    ncResourceLocationType33 locationType;  //!< NC_LOCATION_{URL|WALRUS...}
    ncResourceFormatType33 format;     //!< NC_FORMAT_{NONE|EXT2|EXT3|SWAP}
    int diskNumber;                    //!< 0 = [sh]da or fd0, 1 = [sh]db or fd1, etc.
    int partitionNumber;               //!< 0 = whole disk, 1 = partition 1, etc.
    libvirtDevType33 guestDeviceType;  //!< DEV_TYPE_{DISK|FLOPPY|CDROM}
    libvirtBusType33 guestDeviceBus;   //!< BUS_TYPE_{IDE|SCSI|VIRTIO|XEN}
    libvirtSourceType33 backingType;   //!< SOURCE_TYPE_{FILE|BLOCK}
    char backingPath[CHAR_BUFFER_SIZE33];   //!< path to file or block device that backs the resource
    char preparedResourceLocation[VERY_BIG_CHAR_BUFFER_SIZE33]; //!< e.g., URL + resourceLocation for Walrus downloads, sc url for ebs volumes prior to SC call, then connection string for ebs volumes returned from SC
    //! @}
} virtualBootRecord33;

//! Structure defining a virtual machine
typedef struct virtualMachine33_t {
    int mem;                           //!< Available memory
    int cores;                         //!< Available number of cores
    int disk;                          //!< Number of disk
    char name[64];                     //!< Name of virtual machine
    virtualBootRecord33 *root;         //!< Root boot record information
    virtualBootRecord33 *kernel;       //!< kernel boot record information
    virtualBootRecord33 *ramdisk;      //!< Ramdisk boot record information
    virtualBootRecord33 *swap;         //!< SWAP boot record information
    virtualBootRecord33 *ephemeral0;   //!< Ephemeral boot record information
    virtualBootRecord33 *boot;         //!< Boot sector
    virtualBootRecord33 virtualBootRecord[EUCA_MAX_VBRS33]; //!< List of virtual boot records
    int virtualBootRecordLen;          //!< Number of VBRS in the list
    libvirtNicType33 nicType;          //!< Defines the virtual machine NIC type
    char guestNicDeviceName[64];       //!< Defines the guest NIC device name
} virtualMachine33;

//! Structure defining the network configuration
typedef struct netConfig33_t {
    int vlan;                          //!< Virtual LAN
    int networkIndex;                  //!< Network index
    char privateMac[24];               //!< Private MAC address
    char publicIp[24];                 //!< Public IP address
    char privateIp[24];                //!< Private IP address
} netConfig33;

//! Structure defining NC Volumes
typedef struct ncVolume33_t {
    char volumeId[CHAR_BUFFER_SIZE33]; //!< Remote volume identifier string
    char attachmentToken[CHAR_BUFFER_SIZE33];   //!< Remote device name string, the token reference
    char localDev[CHAR_BUFFER_SIZE33]; //!< Local device name string
    char localDevReal[CHAR_BUFFER_SIZE33];  //!< Local device name (real) string
    char stateName[CHAR_BUFFER_SIZE33]; //!< Volume state name string
    char connectionString[VERY_BIG_CHAR_BUFFER_SIZE33]; //!< Volume Token for attachment/detachment
} ncVolume33;

//! defines various instance states
typedef enum instance_states33_t {
    //! @{
    //! @name the first 7 should match libvirt
    NO_STATE33 = 0,
    RUNNING33,
    BLOCKED33,
    PAUSED33,
    SHUTDOWN33,
    SHUTOFF33,
    CRASHED33,
    //! @}

    //! @{
    //! @name start-time states
    STAGING33,
    BOOTING33,
    CANCELED33,
    //! @}

    //! @{
    //! @name state after running
    BUNDLING_SHUTDOWN33,
    BUNDLING_SHUTOFF33,
    //! @}

    //! @{
    //! @name createImage states
    CREATEIMAGE_SHUTDOWN33,
    CREATEIMAGE_SHUTOFF33,
    //! @}

    //! @{
    //! @name the only three states reported to CLC
    PENDING33,                         //!< staging in data, starting to boot, failed to boot
    EXTANT33,                          //!< guest OS booting, running, shutting down, cleaning up state
    TEARDOWN33,                        //!< a marker for a terminated domain, one not taking up resources
    //! @}

    TOTAL_STATES33
} instance_states33;

//! Defines the bundling task progress states
typedef enum bundling_progress33_t {
    NOT_BUNDLING33 = 0,
    BUNDLING_IN_PROGRESS33,
    BUNDLING_SUCCESS33,
    BUNDLING_FAILED33,
    BUNDLING_CANCELLED33
} bundling_progress33;

//! Defines the create image task progress states
typedef enum createImage_progress33_t {
    NOT_CREATEIMAGE33 = 0,
    CREATEIMAGE_IN_PROGRESS33,
    CREATEIMAGE_SUCCESS33,
    CREATEIMAGE_FAILED33,
    CREATEIMAGE_CANCELLED33
} createImage_progress33;

//! Enumeration of migration-related states
typedef enum migration_states33_t {
    NOT_MIGRATING33 = 0,
    MIGRATION_PREPARING33,
    MIGRATION_READY33,
    MIGRATION_IN_PROGRESS33,
    MIGRATION_CLEANING33,
    TOTAL_MIGRATION_STATES33
} migration_states33;

//! Structure definint NC instances
typedef struct ncInstance33_t {
    char uuid[CHAR_BUFFER_SIZE33];     //!< Unique user identifier string
    char instanceId[CHAR_BUFFER_SIZE33];    //!< Instance identifier string
    char reservationId[CHAR_BUFFER_SIZE33]; //!< Reservation identifier string
    char userId[CHAR_BUFFER_SIZE33];   //!< User identifier string
    char ownerId[CHAR_BUFFER_SIZE33];  //!< Owner identifier string
    char accountId[CHAR_BUFFER_SIZE33]; //!< Account identifier string
    char imageId[SMALL_CHAR_BUFFER_SIZE33]; //!< Image identifier string
    char kernelId[SMALL_CHAR_BUFFER_SIZE33];    //!< Kernel image identifier string
    char ramdiskId[SMALL_CHAR_BUFFER_SIZE33];   //!< Ramdisk image identifier string
    int retries;                       //!< Number of times we try to communicate with LIBVIRT about this instance

    //! @{
    //! @name state as reported to CC & CLC
    char stateName[CHAR_BUFFER_SIZE33]; //!< Instance state as a string
    char bundleTaskStateName[CHAR_BUFFER_SIZE33];   //!< Instance's bundle task state as a string
    char createImageTaskStateName[CHAR_BUFFER_SIZE33];  //!< Instance's image task state as a string
    //! @}

    int stateCode;                     //!< Instance state code as an integer value

    //! @{
    //! @name state as NC thinks of it
    instance_states33 state;           //!< Instance state
    bundling_progress33 bundleTaskState;    //!< Bundling task progress state
    int bundlePid;                     //!< Bundling task PID value
    int bundleBucketExists;            //!< Boolean indicating if the bundle's bucket already exists
    int bundleCanceled;                //!< Boolean indicating if the bundle has been cancelled
    //! @}

    createImage_progress33 createImageTaskState;    //!< Image creation task progress state
    int createImagePid;                //!< Image creationg task PID value
    int createImageCanceled;           //!< Boolean indicating if the image creation task has been cancelled

    migration_states33 migration_state; //!< Migration state
    char migration_src[HOSTNAME_SIZE33];    //!< Name of the host from which the instance is being or needs to be migrated
    char migration_dst[HOSTNAME_SIZE33];    //!< Name of the host to which the instance is being or needs to be migrated
    char migration_credentials[CREDENTIAL_SIZE33];  //!< Migration shared secret

    char keyName[CHAR_BUFFER_SIZE33 * 4];   //!< Name of the key to use for this instance
    char privateDnsName[CHAR_BUFFER_SIZE33];    //!< Private DNS name
    char dnsName[CHAR_BUFFER_SIZE33];  //!< DNS name
    int launchTime;                    //!< timestamp of RunInstances request arrival
    int expiryTime;                    //!< timestamp of instance ->RUNNING expiration
    int bootTime;                      //!< timestamp of STAGING->BOOTING transition
    int bundlingTime;                  //!< timestamp of ->BUNDLING transition
    int createImageTime;               //!< timestamp of ->CREATEIMAGE transition
    int terminationTime;               //!< timestamp of when resources are released (->TEARDOWN transition)
    int migrationTime;                 //!< timestamp of migration request

    virtualMachine33 params;           //!< Virtual machine parameters
    netConfig33 ncnet;                 //!< Network configuration information
    pthread_t tcb;                     //!< Instance thread
    char instancePath[CHAR_BUFFER_SIZE33];  //!< instance blobstore path

    //! @{
    //! @name information needed for generating libvirt XML
    char xmlFilePath[CHAR_BUFFER_SIZE33];   //!< Instance XML file path name
    char libvirtFilePath[CHAR_BUFFER_SIZE33];   //!< LIBVIRT file path name
    char consoleFilePath[CHAR_BUFFER_SIZE33];   //!< console file path name
    char floppyFilePath[CHAR_BUFFER_SIZE33];    //!< Floppy disk path name
    char hypervisorType[SMALL_CHAR_BUFFER_SIZE33];  //!< Hypervisor type as a string
    hypervisorCapabilityType33 hypervisorCapability;    //!< What is the hypervisor capable of for this instance
    int hypervisorBitness;             //!< Hypervisor bitness (32 / 64 bits supported)
    boolean33 combinePartitions;       //!< hypervisor works only with disks (all except Xen)
    boolean33 do_inject_key;           //!< whether or not NC injects SSH key into this instance (eucalyptus.conf option)
    //! @}

    //! @{
    //! @name passed into NC via runInstances for safekeeping
    char userData[CHAR_BUFFER_SIZE33 * 32]; //!< user data to pass to the instance
    char launchIndex[CHAR_BUFFER_SIZE33];   //!< the launch index for this instance
    char platform[CHAR_BUFFER_SIZE33]; //!< the platform used for this instance (typically 'windows' or 'linux')
    char groupNames[EUCA_MAX_GROUPS33][CHAR_BUFFER_SIZE33]; //!< Network groups assigned to this instance.
    int groupNamesSize;                //!< Number of network groups.
    //! @}

    //! @{
    //! @name updated by NC upon Attach/DetachVolume
    ncVolume33 volumes[EUCA_MAX_VOLUMES33]; //!< Instance's attached volume information
    //! @}

    //! @{
    //! @name reported by NC back, for report generation
    long long blkbytes;                //!< Number of block bytes
    long long netbytes;                //!< Number of network bytes
    time_t last_stat;                  //!< Last time these statistics were updated
    //! @}
} ncInstance33;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_DATA33_H_ */
