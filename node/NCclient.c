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
//! @file node/NCclient.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <unistd.h>

#include <data.h>

#include "client-marshal.h"

#include <eucalyptus.h>
#include <misc.h>
#include <euca_axis.h>
#include <sensor.h>
#include <adb-helpers.h>
#include <euca_string.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define NC_ENDPOINT                 "/axis2/services/EucalyptusNC"
#define WALRUS_ENDPOINT             "/services/Walrus"
#define DEFAULT_WALRUS_HOSTPORT     "localhost:8773"
#define DEFAULT_NC_HOSTPORT         "localhost:8775"
#define DEFAULT_MAC_ADDR            "aa:bb:cc:dd:ee:ff"
#define DEFAULT_PUBLIC_IP           "10.1.2.3"
#define BUFSIZE                     1024

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

#ifndef NO_COMP
const char *euca_this_component_name = "nc";    //!< Eucalyptus Component Name
const char *euca_client_component_name = "user";    //!< The client component name
#endif /* ! NO_COMP */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static boolean gDebug = FALSE;         //!< Enables debug mode if set to TRUE

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void usage(void);
static int add_vbr(const char *psSpec, virtualMachine * pVirtMachine);

static int ncClientRunInstance(ncStub * pStub, ncMetadata * pMeta, u32 nbInstances, char *psReservationId, char *psInstanceId, char *psMacAddr, char *psUUID,
                               virtualMachine * pVirtMachine, char *psImageId, char *psImageURL, char *psKernelId, char *psKernelURL, char *psRamdiskId, char *psRamdiskURL,
                               char *psUserData, char *psLaunchIndex, char **ppsGroupNames, u32 groupNameSize);
static int ncClientTerminateInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId);
static int ncClientDescribeInstances(ncStub * pStub, ncMetadata * pMeta);
static int ncClientBundleInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId);
static int ncClientBundleRestartInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId);
static int ncClientDescribeBundleTask(ncStub * pStub, ncMetadata * pMeta);
static int ncClientPowerDown(ncStub * pStub, ncMetadata * pMeta);
static int ncClientAssignAddress(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId, char *psPublicIP);
static int ncClientBroadcastNetworkInfo(ncStub * pStub, ncMetadata * pMeta, char *psNetworkInfo);
static int ncClientDescribeResources(ncStub * pStub, ncMetadata * pMeta);
static int ncClientAttachVolume(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId, char *psVolumeId, char *psRemoteDevice, char *psLocalDevice);
static int ncClientDetachVolume(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId, char *psVolumeId, char *psRemoteDevice, char *psLocalDevice, boolean force);
static int ncClientDescribeSensors(ncStub * pStub, ncMetadata * pMeta);
static int ncClientModifyNode(ncStub * pStub, ncMetadata * pMeta, char *psStateName);
static int ncClientMigrateInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId, char *psSrcNodeName, char *psDstNodeName, char *psStateName, char *psMigrationCreds);
static int ncClientStartInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId);
static int ncClientStopInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId);
static int ncClientConvertTimeStamp(ncStub * pStub, char *psTimeStamp);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Macro to validate the presence of a required parameter.
#define CHECK_PARAM(_param, _sName)                                       \
{                                                                         \
    if ((_param) == NULL) {                                               \
        fprintf (stderr, "ERROR: no %s specified (try -h)\n", (_sName));  \
        exit (1);                                                         \
    }                                                                     \
}

//! generate random IDs if they weren't specified
#define NC_RANDOM()                              ((rand() % 26) + 97)

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Prints the command help to stderr
//!
static void usage(void)
{
    fprintf(stderr, "usage: NCclient [command] [options]\n"
            "\tcommands:\t\t\trequired options:\n"
            "\t\trunInstance\t\t[-m -k] or multiple [-v]\n"
            "\t\tterminateInstance\t[-i]\n"
            "\t\tdescribeInstances\n"
            "\t\tdescribeResource\n"
            "\t\tattachVolume\t\t[-i -V -R -L]\n"
            "\t\tdetachVolume\t\t[-i -V -R -L]\n"
            "\t\tbundleInstance\t\t[-i]\n"
            "\t\tbundleRestartInstance\t[-i]\n"
            "\t\tdescribeSensors\n"
            "\t\tmodifyNode\t\t[-s]\n"
            "\t\tmigrateInstances\t\t[-i -M]\n"
            "\t\tstartInstance\t\t[-i]\n"
            "\t\tstopInstance\t\t[-i]\n"
            "\toptions:\n"
            "\t\t-d \t\t- print debug output\n"
            "\t\t-l \t\t- local invocation => do not use WSSEC\n"
            "\t\t-h \t\t- this help information\n"
            "\t\t-w [host:port] \t- Walrus endpoint\n"
            "\t\t-n [host:port] \t- NC endpoint\n"
            "\t\t-B -n node-ip \t- one of nodes controled by VB\n"
            "\t\t-i [str] \t- instance ID\n"
            "\t\t-e [str] \t- reservation ID\n"
            "\t\t-v [type:id:size:format:guestDeviceName:resourceLocation]\n"
            "\t\t\ttype = {machine|kernel|ramdisk|ephemeral|ebs}\n"
            "\t\t\tid = {none|emi-...|eki-...|eri-...|vol-...}\n"
            "\t\t\tsize = {-1|NNNN} - in bytes, only for local partitions\n"
            "\t\t\tformat = {none|ext3|swap} - only for local partitions\n"
            "\t\t\tguestDeviceName = {none|x?[vhsf]d[a-z]?[1-9]*} - e.g., sda1\n"
            "\t\t\tresourceLocation = {none|objectstorage://...|iqn://...|aoe://...}\n"
            "\t\t-m [id:path] \t- id and manifest path of disk image\n"
            "\t\t-k [id:path] \t- id and manifest path of kernel image\n"
            "\t\t-r [id:path] \t- id and manifest path of ramdisk image\n"
            "\t\t-a [address] \t- MAC address for instance to use\n"
            "\t\t-c [number] \t- number of instances to start\n"
            "\t\t-V [name] \t- name of the volume (for reference)\n"
            "\t\t-R [device] \t- remote/source device (e.g. /dev/etherd/e0.0)\n"
            "\t\t-L [device] \t- local/target device (e.g. hda)\n"
            "\t\t-F \t\t- force VolumeDetach\n"
            "\t\t-U [string] \t- user data to store with instance\n" "\t\t-I [string] \t- launch index to store with instance\n"
            "\t\t-G [str:str: ] \t- group names to store with instance\n"
            "\t\t-s [stateName] \t- name of state\n"
            "\t\t\t\tUse {enabled|disabled} for modifyNode operation\n"
            "\t\t\t\tUse {prepare|commit|rollback} for migrateInstances opration\n" "\t\t-M [src:dst:cr]\t- migration request source and destination IPs + credentials\n");

    exit(1);
}

//!
//! parse spec_str (-v parameter) into a VBR record and add it to the
//! vm_type->virtualBootRecord[virtualBootRecordLen]
//!
//! @param[in] psSpec
//! @param[in] pVirtMachine
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
static int add_vbr(const char *psSpec, virtualMachine * pVirtMachine)
{
    char *psSpecCopy = NULL;
    char *psTypeName = NULL;
    char *psID = NULL;
    char *psSizeBytes = NULL;
    char *psFormatName = NULL;
    char *psGuestDeviceName = NULL;
    char *psResourceLocation = NULL;
    virtualBootRecord *pVbr = NULL;

    if (pVirtMachine->virtualBootRecordLen == EUCA_MAX_VBRS) {
        fprintf(stderr, "ERROR: too many -v parameters\n");
        return (EUCA_OVERFLOW_ERROR);
    }

    pVbr = &(pVirtMachine->virtualBootRecord[pVirtMachine->virtualBootRecordLen++]);
    psSpecCopy = strdup(psSpec);
    psTypeName = strtok(psSpecCopy, ":");
    psID = strtok(NULL, ":");
    psSizeBytes = strtok(NULL, ":");
    psFormatName = strtok(NULL, ":");
    psGuestDeviceName = strtok(NULL, ":");
    psResourceLocation = strtok(NULL, ":");

    if (psTypeName == NULL) {
        fprintf(stderr, "ERROR: invalid 'type' specification in VBR '%s'\n", psSpec);
        goto out_error;
    }

    euca_strncpy(pVbr->typeName, psTypeName, sizeof(pVbr->typeName));

    if (psID == NULL) {
        fprintf(stderr, "ERROR: invalid 'id' specification in VBR '%s'\n", psSpec);
        goto out_error;
    }

    euca_strncpy(pVbr->id, psID, sizeof(pVbr->id));

    if (psSizeBytes == NULL) {
        fprintf(stderr, "ERROR: invalid 'size' specification in VBR '%s'\n", psSpec);
        goto out_error;
    }

    pVbr->sizeBytes = atoi(psSizeBytes);

    if (psFormatName == NULL) {
        fprintf(stderr, "ERROR: invalid 'format' specification in VBR '%s'\n", psSpec);
        goto out_error;
    }

    euca_strncpy(pVbr->formatName, psFormatName, sizeof(pVbr->formatName));

    if (psGuestDeviceName == NULL) {
        fprintf(stderr, "ERROR: invalid 'guestDeviceName' specification in VBR '%s'\n", psSpec);
        goto out_error;
    }

    euca_strncpy(pVbr->guestDeviceName, psGuestDeviceName, sizeof(pVbr->guestDeviceName));

    if (psResourceLocation == NULL) {
        fprintf(stderr, "ERROR: invalid 'resourceLocation' specification in VBR '%s'\n", psSpec);
        goto out_error;
    }

    euca_strncpy(pVbr->resourceLocation, (psSpec + (psResourceLocation - psSpecCopy)), sizeof(pVbr->resourceLocation));

    EUCA_FREE(psSpecCopy);
    return (EUCA_OK);

out_error:
    pVirtMachine->virtualBootRecordLen--;
    EUCA_FREE(psSpecCopy);
    return (EUCA_ERROR);
}

//!
//! Builds and execute the "RunInstance" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  nbInstances number of instances to run
//! @param[in]  psReservationId pointer to a string containing the reservation ID for this request
//! @param[in]  psInstanceId the instance identifier string (i-XXXXXXXX)
//! @param[in]  psMacAddr the reservation identifier string
//! @param[in]  psUUID pointer to a string containing the user unique ID.
//! @param[in]  pVirtMachine a pointer to the virtual machine parameters to use
//! @param[in]  psImageId UNUSED
//! @param[in]  psImageURL UNUSED
//! @param[in]  psKernelId the kernel image identifier (eki-XXXXXXXX)
//! @param[in]  psKernelURL the kernel image URL address
//! @param[in]  psRamdiskId the ramdisk image identifier (eri-XXXXXXXX)
//! @param[in]  psRamdiskURL the ramdisk image URL address
//! @param[in]  psUserData the user data string
//! @param[in]  psLaunchIndex the launch index string
//! @param[in]  ppsGroupNames a list of group name string
//! @param[in]  groupNameSize the number of group name in the groupNames list
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre None of the provided pointer and string should be NULL.
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientRunInstance(ncStub * pStub, ncMetadata * pMeta, u32 nbInstances, char *psReservationId, char *psInstanceId, char *psMacAddr, char *psUUID,
                               virtualMachine * pVirtMachine, char *psImageId, char *psImageURL, char *psKernelId, char *psKernelURL, char *psRamdiskId, char *psRamdiskURL,
                               char *psUserData, char *psLaunchIndex, char **ppsGroupNames, u32 groupNameSize)
{
    int i = 0;
    int rc = EUCA_OK;
    int vlan = 3;
    int devMapId = 0;
    char *psLocalInstanceId = NULL;
    char *psLocalReservationId = NULL;
    char *psLocalUUID = NULL;
    char *psPrivateMac = NULL;
    char *psPrivateIP = NULL;
    char *psPlatform = NULL;
    char *psCredential = NULL;
    char sTempBuffer[64] = "";
    netConfig netParams = { 0 };
    ncInstance *pOutInst = NULL;

    psPrivateMac = strdup(psMacAddr);
    psPrivateIP = strdup("10.0.0.202");
    srand(time(NULL));
    while (nbInstances--) {
        if ((psInstanceId == NULL) || (nbInstances > 1)) {
            snprintf(sTempBuffer, sizeof(sTempBuffer), "i-%c%c%c%c%c", NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM());
            psLocalInstanceId = sTempBuffer;
        } else {
            psLocalInstanceId = psInstanceId;
        }

        if ((psReservationId == NULL) || (nbInstances > 1)) {
            snprintf(sTempBuffer, sizeof(sTempBuffer), "r-%c%c%c%c%c", NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM());
            psLocalReservationId = sTempBuffer;
        } else {
            psLocalReservationId = psReservationId;
        }

        if ((psUUID == NULL) || (nbInstances > 1)) {
            snprintf(sTempBuffer, sizeof(sTempBuffer), "%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c", NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(),
                     NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(),
                     NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM(),
                     NC_RANDOM(), NC_RANDOM(), NC_RANDOM(), NC_RANDOM());
            psLocalUUID = sTempBuffer;
        } else {
            psLocalUUID = psUUID;
        }

        bzero(&netParams, sizeof(netParams));

        netParams.vlan = vlan;
        snprintf(netParams.privateIp, sizeof(netParams.privateIp), "%s", psPrivateIP);
        snprintf(netParams.privateMac, sizeof(netParams.privateMac), "%s", psPrivateMac);

        rc = ncRunInstanceStub(pStub, pMeta, psLocalUUID, psLocalInstanceId, psLocalReservationId, pVirtMachine, psImageId, psImageURL, psKernelId, psKernelURL, psRamdiskId,
                               psRamdiskURL, "eucalyptusUser", "eucalyptusAccount", "", &netParams, psUserData, psCredential, psLaunchIndex, psPlatform, 0, ppsGroupNames,
                               groupNameSize, &pOutInst);
        if (rc != EUCA_OK) {
            printf("ncRunInstanceStub = %d : instanceId=%s\n", rc, psInstanceId);
            exit(1);
        }
        // count device mappings
        for (i = 0, devMapId = 0; i < EUCA_MAX_VBRS; i++) {
            if (strlen(pOutInst->params.virtualBootRecord[i].typeName) > 0)
                devMapId++;
        }

        printf("ncRunInstanceStub = %d : instanceId=%s stateCode=%d stateName=%s deviceMappings=%d/%d\n", rc, pOutInst->instanceId, pOutInst->stateCode, pOutInst->stateName,
               devMapId, pOutInst->params.virtualBootRecordLen);
        EUCA_FREE(pOutInst);
    }

    EUCA_FREE(psPrivateIP);
    EUCA_FREE(psPrivateMac);
    return (EUCA_OK);
}

//!
//! Builds and execute the "TerminateInstance" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  psInstanceId a pointer to the string containing the instance identifier (i-XXXXXXXX)
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre None of the provided pointers should be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientTerminateInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId)
{
    int rc = EUCA_OK;
    int shutdownState = 0;
    int previousState = 0;

    if ((rc = ncTerminateInstanceStub(pStub, pMeta, psInstanceId, 0, &shutdownState, &previousState)) != EUCA_OK) {
        printf("ncTerminateInstanceStub = %d\n", rc);
        exit(1);
    }

    printf("ncTerminateInstanceStub = %d : shutdownState=%d, previousState=%d\n", rc, shutdownState, previousState);
    return (rc);
}

//!
//! Builds and execute the "DescribeInstances" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre The pStub and pMeta fields must not be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientDescribeInstances(ncStub * pStub, ncMetadata * pMeta)
{
    int i = 0;
    int j = 0;
    int rc = EUCA_OK;
    int volCount = 0;
    int outInstsLen = 0;
    ncInstance *pInstance = NULL;
    ncInstance **ppOutInsts = NULL;

    //! @TODO pull out of argv[] requested instanceIDs
    if ((rc = ncDescribeInstancesStub(pStub, pMeta, NULL, 0, &ppOutInsts, &outInstsLen)) != EUCA_OK) {
        printf("ncDescribeInstancesStub = %d\n", rc);
        exit(1);
    }

    for (i = 0; i < outInstsLen; i++) {
        pInstance = ppOutInsts[i];
        printf("instanceId=%s state=%s time=%d\n", pInstance->instanceId, pInstance->stateName, pInstance->launchTime);
        if (gDebug) {
            printf("\t\tuserData=%s launchIndex=%s groupNames=", pInstance->userData, pInstance->launchIndex);
            if (pInstance->groupNamesSize > 0) {
                for (j = 0; j < pInstance->groupNamesSize; j++) {
                    if (j > 0)
                        printf(":");
                    printf("%s", pInstance->groupNames[j]);
                }
            } else {
                printf("(none)");
            }
            printf("\n");

            printf("\t\tattached volumes: ");
            for (j = 0, volCount = 0; j < EUCA_MAX_VOLUMES; j++) {
                if (strlen(pInstance->volumes[j].volumeId) > 0) {
                    if (volCount > 0)
                        printf("\t\t                  ");
                    printf("%s %s %s\n", pInstance->volumes[j].volumeId, pInstance->volumes[j].attachmentToken, pInstance->volumes[j].localDev);
                    volCount++;
                }
            }

            if (volCount > 0)
                printf("(none)\n");
        }

        free_instance(&(ppOutInsts[i]));
    }

    EUCA_FREE(ppOutInsts);
    return (rc);
}

//!
//! Builds and execute the "BundleInstance" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  psInstanceId a pointer to the string containing the instance identifier (i-XXXXXXXX)
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre The pStub, pMeta and psInstanceId fields must not be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientBundleInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId)
{
    int rc = EUCA_OK;
    rc = ncBundleInstanceStub(pStub, pMeta, psInstanceId, "bucket-foo", "prefix-foo", "s3-url-foo", "user-key-foo", "s3policy-foo", "s3policy-sig");
    printf("ncBundleInstanceStub = %d\n", rc);
    return (rc);
}

//!
//! Builds and execute the "BundleRestartInstance" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  psInstanceId a pointer to the string containing the instance identifier (i-XXXXXXXX)
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre The pStub, pMeta and psInstanceId fields must not be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientBundleRestartInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId)
{
    int rc = EUCA_OK;
    rc = ncBundleRestartInstanceStub(pStub, pMeta, psInstanceId);
    printf("ncBundleRestartInstanceStub = %d\n", rc);
    return (rc);
}

//!
//! Builds and execute the "DescribeBundleTask" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre The pStub and pMeta fields must not be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientDescribeBundleTask(ncStub * pStub, ncMetadata * pMeta)
{
    int rc = EUCA_OK;
    int instIdsLen = 4;
    int outBundleTasksLen = 0;
    char *ppInstIds[4] = { NULL };
    bundleTask **ppOutBundleTasks = NULL;

    ppInstIds[0] = EUCA_ZALLOC(32, sizeof(char));
    ppInstIds[1] = EUCA_ZALLOC(32, sizeof(char));
    ppInstIds[2] = EUCA_ZALLOC(32, sizeof(char));
    ppInstIds[3] = EUCA_ZALLOC(32, sizeof(char));

    snprintf(ppInstIds[0], 32, "i-12345675");
    snprintf(ppInstIds[1], 32, "i-12345674");
    snprintf(ppInstIds[2], 32, "i-12345673");
    snprintf(ppInstIds[3], 32, "i-12345672");

    rc = ncDescribeBundleTasksStub(pStub, pMeta, ppInstIds, instIdsLen, &ppOutBundleTasks, &outBundleTasksLen);
    printf("ncDescribeBundleTasksStub = %d\n", rc);
    for (int i = 0; i < outBundleTasksLen; i++) {
        printf("\tBUNDLE %d: %s %s\n", i, ppOutBundleTasks[i]->instanceId, ppOutBundleTasks[i]->state);
        EUCA_FREE(ppOutBundleTasks[i]);
    }

    EUCA_FREE(ppOutBundleTasks);
    return (rc);
}

//!
//! Builds and execute the "PowerDown" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre The pStub and pMeta fields must not be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientPowerDown(ncStub * pStub, ncMetadata * pMeta)
{
    int rc = EUCA_OK;
    rc = ncPowerDownStub(pStub, pMeta);
    printf("ncPowerDownStub = %d\n", rc);
    return (rc);
}

//!
//! Builds and execute the "BroadcastNetworkInfo" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
///! @param[in]  psNetworkInfo a pointer to the string that is a file that will be read and sent as input to broadcastnetworkinfo()
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre None of the provided pointers should be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientBroadcastNetworkInfo(ncStub * pStub, ncMetadata * pMeta, char *psNetworkInfo)
{
    char *networkInfoBuf = NULL;
    int rc = EUCA_OK;
    networkInfoBuf = file2str(psNetworkInfo);
    rc = ncBroadcastNetworkInfoStub(pStub, pMeta, networkInfoBuf);
    printf("ncBroadcastNetworkInfoStub = %d, %s\n", rc, networkInfoBuf);
    return (rc);
}

//!
//! Builds and execute the "AssignAddress" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  psInstanceId a pointer to the string containing the instance identifier (i-XXXXXXXX)
//! @param[in]  psPublicIP a pointer to the string containing the public IP to assign
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre None of the provided pointers should be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientAssignAddress(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId, char *psPublicIP)
{
    int rc = EUCA_OK;
    rc = ncAssignAddressStub(pStub, pMeta, psInstanceId, psPublicIP);
    printf("ncAssignAddressStub = %d\n", rc);
    return (rc);
}

//!
//! Builds and execute the "DescribeResources" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre The pStub and pMeta fields must not be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientDescribeResources(ncStub * pStub, ncMetadata * pMeta)
{
    int rc = EUCA_OK;
    char *psType = strdup("TYPE");
    ncResource *pOutRes = NULL;

    if ((rc = ncDescribeResourceStub(pStub, pMeta, psType, &pOutRes)) != EUCA_OK) {
        printf("ncDescribeResourceStub = %d\n", rc);
        exit(1);
    }

    printf("ncDescribeResourceStub = %d : node status=[%s] memory=%d/%d disk=%d/%d cores=%d/%d subnets=[%s]\n", rc, pOutRes->nodeStatus, pOutRes->memorySizeMax,
           pOutRes->memorySizeAvailable, pOutRes->diskSizeMax, pOutRes->diskSizeAvailable, pOutRes->numberOfCoresMax, pOutRes->numberOfCoresAvailable, pOutRes->publicSubnets);
    return (rc);
}

//!
//! Builds and execute the "AttachVolume" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  psInstanceId a pointer to the string containing the instance identifier (i-XXXXXXXX)
//! @param[in]  psVolumeId a pointer to the string containing the volume identifier (vol-XXXXXXXX)
//! @param[in]  psRemoteDevice a pointer to the string containing the remote device name to attach to
//! @param[in]  psLocalDevice a pointer to the string containing the local device name on the host
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre None of the provided pointers should be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientAttachVolume(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId, char *psVolumeId, char *psRemoteDevice, char *psLocalDevice)
{
    int rc = EUCA_OK;

    if ((rc = ncAttachVolumeStub(pStub, pMeta, psInstanceId, psVolumeId, psRemoteDevice, psLocalDevice)) != EUCA_OK) {
        printf("ncAttachVolumeStub = %d\n", rc);
        exit(1);
    }

    printf("ncAttachVolumeStub = %d\n", rc);
    return (rc);
}

//!
//! Builds and execute the "DetachVolume" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  psInstanceId a pointer to the string containing the instance identifier (i-XXXXXXXX)
//! @param[in]  psVolumeId a pointer to the string containing the volume identifier (vol-XXXXXXXX)
//! @param[in]  psRemoteDevice a pointer to the string containing the remote device name to attach to
//! @param[in]  psLocalDevice a pointer to the string containing the local device name on the host
//! @param[in]  force set to TRUE to force detach a volume
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre None of the provided pointers should be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientDetachVolume(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId, char *psVolumeId, char *psRemoteDevice, char *psLocalDevice, boolean force)
{
    int rc = EUCA_OK;

    if ((rc = ncDetachVolumeStub(pStub, pMeta, psInstanceId, psVolumeId, psRemoteDevice, psLocalDevice, force)) != EUCA_OK) {
        printf("ncDetachVolumeStub = %d\n", rc);
        exit(1);
    }

    printf("ncDetachVolumeStub = %d\n", rc);
    return (rc);
}

//!
//! Builds and execute the "DescribeSensors" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre The pStub and pMeta fields must not be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientDescribeSensors(ncStub * pStub, ncMetadata * pMeta)
{
    int rc = EUCA_OK;
    int nbResources;
    char sBuffer[102400] = "";
    sensorResource **ppResources = NULL;

    if ((rc = ncDescribeSensorsStub(pStub, pMeta, 20, 5000, NULL, 0, NULL, 0, &ppResources, &nbResources)) != EUCA_OK) {
        printf("ncDescribeSensorsStub = %d\n", rc);
        exit(1);
    }

    printf("ncDescribeSensorsStub = %d\n", rc);
    sensor_res2str(sBuffer, sizeof(sBuffer), ppResources, nbResources);
    printf("\tresources: %d\n%s\n", nbResources, sBuffer);
    return (rc);
}

//!
//! Builds and execute the "ModifyNode" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  psStateName a pointer to the string containing the new state name
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre None of the provided pointers should be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientModifyNode(ncStub * pStub, ncMetadata * pMeta, char *psStateName)
{
    int rc = EUCA_OK;

    if ((rc = ncModifyNodeStub(pStub, pMeta, psStateName)) != EUCA_OK) {
        printf("ncModifyNodeStub = %d\n", rc);
        exit(1);
    }

    printf("ncModifyNodeStub = %d\n", rc);
    return (rc);
}

//!
//! Builds and execute the "RunInstance" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  psInstanceId a pointer to the string containing the instance identifier (i-XXXXXXXX)
//! @param[in]  psSrcNodeName a pointer to the source node name
//! @param[in]  psDstNodeName a pointer to the destination node name
//! @param[in]  psStateName a pointer to the migration state name
//! @param[in]  psMigrationCreds a pointer to the migration credentials
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre None of the provided pointers should be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientMigrateInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId, char *psSrcNodeName, char *psDstNodeName, char *psStateName, char *psMigrationCreds)
{
    int rc = EUCA_OK;
    ncInstance instance = { {0} };
    ncInstance *pInstance = &instance;

    bzero(&instance, sizeof(instance));

    euca_strncpy(instance.instanceId, psInstanceId, sizeof(instance.instanceId));
    euca_strncpy(instance.migration_src, psSrcNodeName, sizeof(instance.migration_src));
    euca_strncpy(instance.migration_dst, psDstNodeName, sizeof(instance.migration_dst));
    if ((rc = ncMigrateInstancesStub(pStub, pMeta, &pInstance, 1, psStateName, psMigrationCreds)) != EUCA_OK) {
        printf("ncMigrateInstancesStub = %d\n", rc);
        exit(1);
    }

    printf("ncMigrateInstancesStub = %d\n", rc);
    return (rc);
}

//!
//! Builds and executes the "StartInstance" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  psInstanceId a pointer to the string containing the instance identifier (i-XXXXXXXX)
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre The pStub, pMeta and psInstanceId fields must not be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientStartInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId)
{
    int rc = EUCA_OK;
    rc = ncStartInstanceStub(pStub, pMeta, psInstanceId);
    printf("ncStartInstanceStub = %d\n", rc);
    return (rc);
}

//!
//! Builds and executes the "StopInstance" request
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  psInstanceId a pointer to the string containing the instance identifier (i-XXXXXXXX)
//!
//! @return EUCA_OK on success. On failure, the program will terminate
//!
//! @see
//!
//! @pre The pStub, pMeta and psInstanceId fields must not be NULL
//!
//! @post The request is sent to the NC client and the result of the request will be displayed.
//!
//! @note
//!
static int ncClientStopInstance(ncStub * pStub, ncMetadata * pMeta, char *psInstanceId)
{
    int rc = EUCA_OK;
    rc = ncStopInstanceStub(pStub, pMeta, psInstanceId);
    printf("ncStopInstanceStub = %d\n", rc);
    return (rc);
}

//!
//! Converts a timestanp value
//!
//! @param[in]  pStub a pointer to the NC stub structure
//! @param[in]  psTimeStamp a pointer to the timestamp to convert
//!
//! @return Always returns EUCA_OK.
//!
//! @see
//!
//! @pre Both pointers must not be NULL
//!
//! @post The converted value is displayed on the console.
//!
//! @note
//!
static int ncClientConvertTimeStamp(ncStub * pStub, char *psTimeStamp)
{
    s64 tsIn = 0;
    s64 tsOut = 0;
    char *psDateTimeIn = NULL;
    axutil_date_time_t *pDateTime = NULL;

    tsIn = atoll(psTimeStamp);
    pDateTime = unixms_to_datetime(pStub->env, tsIn);
    psDateTimeIn = axutil_date_time_serialize_date_time(pDateTime, pStub->env);
    tsOut = datetime_to_unixms(pDateTime, pStub->env);

    printf("timestamp:  in = %ld %s\n", tsIn, psDateTimeIn);
    printf("           out = %ld\n", tsOut);
    return (EUCA_OK);
}

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return Always return 0 or exit(1) on failure
//!
int main(int argc, char *argv[])
{
    int ch = 0;
    int rc = 0;
    int useWSSEC = 0;
    int groupNameSize = 0;
    int nbInstances = 1;
    char *psEucaHomePath = NULL;
    char *psTmpBuffer = NULL;
    char *psImageURL = NULL;
    char *psKernelURL = NULL;
    char *psRamdiskURL = NULL;
    char *psNcHostPort = DEFAULT_NC_HOSTPORT;
    char *psWsHostPort = DEFAULT_WALRUS_HOSTPORT;
    char *psNcEndpoint = NC_ENDPOINT;
    char *psInstanceId = NULL;
    char *psImageId = NULL;
    char *psImageManifest = NULL;
    char *psKernelId = NULL;
    char *psKernelManifest = NULL;
    char *psRamdiskId = NULL;
    char *psRamdiskManifest = NULL;
    char *psReservationId = NULL;
    char *psUUID = NULL;
    char *psMacAddr = strdup(DEFAULT_MAC_ADDR);
    char *psPublicIP = strdup(DEFAULT_PUBLIC_IP);
    char *psNetworkInfo = NULL;
    char *psVolumeId = NULL;
    char *psRemoteDevice = NULL;
    char *psLocalDevice = NULL;
    char *psUserData = NULL;
    char *psLaunchIndex = NULL;
    char *psStateName = NULL;
    char *psSrcNodeName = NULL;
    char *psDstNodeName = NULL;
    char *psMigrationCreds = NULL;
    char *psTimeStamp = NULL;
    char *psCommand = NULL;
    char *psEucaHome = NULL;
    char **ppsGroupNames = NULL;
    char sConfigFile[BUFSIZE] = "";
    char sPolicyFile[BUFSIZE] = "";
    char sNcURL[BUFSIZE] = "";
    char sWsURL[BUFSIZE] = "";
    char sTemp[BUFSIZE] = "";
    char sLogFile[EUCA_MAX_PATH] = "";
    boolean force = FALSE;
    boolean local = FALSE;
    ncStub *pStub = NULL;
    ncMetadata meta = { 0 };
    serviceInfoType *pServInfo = NULL;
    virtualMachine virtMachine = { 64, 1, 1, "m1.small", NULL, NULL, NULL, NULL, NULL, NULL, {}, 0 };

    while ((ch = getopt(argc, argv, "lhdN:n:w:i:m:k:r:e:a:c:h:u:p:V:R:L:FU:I:G:v:t:s:M:B")) != -1) {
        switch (ch) {
        case 'c':
            nbInstances = atoi(optarg);
            break;
        case 'd':
            gDebug = TRUE;
            break;
        case 'l':
            local = TRUE;
            break;
        case 'n':
            psNcHostPort = optarg;
            break;
        case 'w':
            psWsHostPort = optarg;
            break;
        case 'i':
            psInstanceId = optarg;
            break;
        case 'p':
            psPublicIP = optarg;
            break;
        case 'N':
            psNetworkInfo = optarg;
            break;
        case 'm':
            psImageId = strtok(optarg, ":");
            psImageManifest = strtok(NULL, ":");
            if ((psImageId == NULL) || (psImageManifest == NULL)) {
                fprintf(stderr, "ERROR: could not parse image [id:manifest] paramters (try -h)\n");
                exit(1);
            }
            break;
        case 'k':
            psKernelId = strtok(optarg, ":");
            psKernelManifest = strtok(NULL, ":");
            if ((psKernelId == NULL) || (psKernelManifest == NULL)) {
                fprintf(stderr, "ERROR: could not parse kernel [id:manifest] paramters (try -h)\n");
                exit(1);
            }
            break;
        case 'r':
            psRamdiskId = strtok(optarg, ":");
            psRamdiskManifest = strtok(NULL, ":");
            if (psRamdiskId == NULL || psRamdiskManifest == NULL) {
                fprintf(stderr, "ERROR: could not parse ramdisk [id:manifest] paramters (try -h)\n");
                exit(1);
            }
            break;
        case 'e':
            psReservationId = optarg;
            break;
        case 'u':
            psUUID = optarg;
            break;
        case 'a':
            psMacAddr = optarg;
            break;
        case 'V':
            psVolumeId = optarg;
            break;
        case 'R':
            psRemoteDevice = optarg;
            break;
        case 'L':
            psLocalDevice = optarg;
            break;
        case 'F':
            force = TRUE;
            break;
        case 'U':
            psUserData = optarg;
            break;
        case 'I':
            psLaunchIndex = optarg;
            break;
        case 'G':
            {
                groupNameSize = 1;
                for (int i = 0; optarg[i]; i++) {
                    if (optarg[i] == ':')
                        groupNameSize++;
                }

                if ((ppsGroupNames = EUCA_ZALLOC(groupNameSize, sizeof(char *))) == NULL) {
                    fprintf(stderr, "ERROR: out of memory for group_names[]\n");
                    exit(1);
                }

                ppsGroupNames[0] = strtok(optarg, ":");
                for (int i = 1; i < groupNameSize; i++)
                    ppsGroupNames[i] = strtok(NULL, ":");
                break;
            }
        case 's':
            psStateName = optarg;
            break;
        case 'M':
            psSrcNodeName = strtok(optarg, ":");
            psDstNodeName = strtok(NULL, ":");
            psMigrationCreds = strtok(NULL, ":");
            if ((psSrcNodeName == NULL) || (psDstNodeName == NULL)) {
                fprintf(stderr, "ERROR: could not parse migration [src:dst:cr] paramters (try -h)\n");
                exit(1);
            }
            break;
        case 'v':
            if (add_vbr(optarg, &virtMachine)) {
                fprintf(stderr, "ERROR: could not parse the virtual boot record (try -h)\n");
                exit(1);
            }
            break;
        case 't':
            psTimeStamp = optarg;
            break;
        case 'h':
            usage();                   // will exit
            break;
        case 'B':
            psNcEndpoint = "/services/EucalyptusBroker";
            break;
        case '?':
        default:
            fprintf(stderr, "ERROR: unknown parameter (try -h)\n");
            exit(1);
        }
    }
    argc -= optind;
    argv += optind;

    if (argc > 0) {
        psCommand = argv[0];
        if (argc > 1) {
            fprintf(stderr, "WARNING: too many parameters, using first one as command\n");
        }
    } else {
        fprintf(stderr, "ERROR: command not specified (try -h)\n");
        exit(1);
    }

    if ((psEucaHomePath = getenv("EUCALYPTUS")) == NULL) {
        psEucaHomePath = "";
    }

    snprintf(sConfigFile, sizeof(sConfigFile), EUCALYPTUS_CONF_LOCATION, psEucaHomePath);
    snprintf(sPolicyFile, sizeof(sPolicyFile), EUCALYPTUS_KEYS_DIR "/nc-client-policy.xml", psEucaHomePath);
    if ((rc = get_conf_var(sConfigFile, "ENABLE_WS_SECURITY", &psTmpBuffer)) != 1) {
        // Default to enabled
        useWSSEC = 1;
    } else {
        if (!strcmp(psTmpBuffer, "Y")) {
            useWSSEC = 1;
        } else {
            useWSSEC = 0;
        }
    }

    snprintf(sNcURL, sizeof(sNcURL), "http://%s%s", psNcHostPort, psNcEndpoint);
    if (gDebug)
        printf("connecting to NC at %s\n", sNcURL);

    if ((psEucaHome = getenv(EUCALYPTUS_ENV_VAR_NAME)) == NULL) {
        snprintf(sLogFile, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR "/NCclient.log", "/");
    } else {
        snprintf(sLogFile, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR "/NCclient.log", psEucaHome);
    }

    if ((pStub = ncStubCreate(sNcURL, sLogFile, NULL)) == NULL) {
        fprintf(stderr, "ERROR: failed to connect to Web service\n");
        exit(2);
    }

    if (useWSSEC && !local) {
        if (gDebug)
            printf("using policy file %s\n", sPolicyFile);

        if ((rc = InitWSSEC(pStub->env, pStub->stub, sPolicyFile)) != 0) {
            fprintf(stderr, "ERROR: cannot initialize WS-SEC policy from %s\n", sPolicyFile);
            exit(1);
        }
    }

    snprintf(sWsURL, sizeof(sWsURL), "http://%s%s", psWsHostPort, WALRUS_ENDPOINT);
    pServInfo = &(meta.services[meta.servicesLen++]);
    euca_strncpy(pServInfo->type, "objectstorage", sizeof(pServInfo->type));
    euca_strncpy(pServInfo->name, "objectstorage", sizeof(pServInfo->name));
    euca_strncpy(pServInfo->uris[0], sWsURL, sizeof(pServInfo->uris[0]));
    pServInfo->urisLen = 1;

    if (psImageManifest) {
        snprintf(sTemp, sizeof(sTemp), "http://%s%s/%s", psWsHostPort, WALRUS_ENDPOINT, psImageManifest);
        psImageURL = strdup(sTemp);
    }

    if (psKernelManifest) {
        snprintf(sTemp, sizeof(sTemp), "http://%s%s/%s", psWsHostPort, WALRUS_ENDPOINT, psKernelManifest);
        psKernelURL = strdup(sTemp);
    }

    if (psRamdiskManifest) {
        snprintf(sTemp, sizeof(sTemp), "http://%s%s/%s", psWsHostPort, WALRUS_ENDPOINT, psRamdiskManifest);
        psRamdiskURL = strdup(sTemp);
    }

    meta.correlationId = strdup("correlate-me-please");
    meta.userId = strdup("eucalyptus");

    if (!strcmp(psCommand, "runInstance")) {
        if (virtMachine.virtualBootRecordLen < 1) {
            CHECK_PARAM(psImageId, "image ID and manifest path");
            CHECK_PARAM(psKernelId, "kernel ID and manifest path");
        }

        ncClientRunInstance(pStub, &meta, nbInstances, psReservationId, psInstanceId, psMacAddr, psUUID, &virtMachine, psImageId, psImageURL, psKernelId, psKernelURL, psRamdiskId,
                            psRamdiskURL, psUserData, psLaunchIndex, ppsGroupNames, groupNameSize);
    } else if (!strcmp(psCommand, "bundleInstance")) {
        CHECK_PARAM(psInstanceId, "instance id");
        ncClientBundleInstance(pStub, &meta, psInstanceId);
    } else if (!strcmp(psCommand, "bundleRestartInstance")) {
        CHECK_PARAM(psInstanceId, "instance id");
        ncClientBundleRestartInstance(pStub, &meta, psInstanceId);
    } else if (!strcmp(psCommand, "powerDown")) {
        ncClientPowerDown(pStub, &meta);
    } else if (!strcmp(psCommand, "describeBundleTasks")) {
        ncClientDescribeBundleTask(pStub, &meta);
    } else if (!strcmp(psCommand, "broadcastNetworkInfo")) {
        ncClientBroadcastNetworkInfo(pStub, &meta, psNetworkInfo);
    } else if (!strcmp(psCommand, "assignAddress")) {
        ncClientAssignAddress(pStub, &meta, psInstanceId, psPublicIP);
    } else if (!strcmp(psCommand, "terminateInstance")) {
        CHECK_PARAM(psInstanceId, "instance ID");
        ncClientTerminateInstance(pStub, &meta, psInstanceId);
    } else if (!strcmp(psCommand, "describeInstances")) {
        ncClientDescribeInstances(pStub, &meta);
    } else if (!strcmp(psCommand, "describeResource")) {
        ncClientDescribeResources(pStub, &meta);
    } else if (!strcmp(psCommand, "attachVolume")) {
        CHECK_PARAM(psInstanceId, "instance ID");
        CHECK_PARAM(psVolumeId, "volume ID");
        CHECK_PARAM(psRemoteDevice, "remote dev");
        CHECK_PARAM(psLocalDevice, "local dev");
        ncClientAttachVolume(pStub, &meta, psInstanceId, psVolumeId, psRemoteDevice, psLocalDevice);
    } else if (!strcmp(psCommand, "detachVolume")) {
        CHECK_PARAM(psInstanceId, "instance ID");
        CHECK_PARAM(psVolumeId, "volume ID");
        CHECK_PARAM(psRemoteDevice, "remote dev");
        CHECK_PARAM(psLocalDevice, "local dev");
        ncClientDetachVolume(pStub, &meta, psInstanceId, psVolumeId, psRemoteDevice, psLocalDevice, force);
    } else if (!strcmp(psCommand, "describeSensors")) {
        ncClientDescribeSensors(pStub, &meta);
    } else if (!strcmp(psCommand, "modifyNode")) {
        CHECK_PARAM(psStateName, "state name");
        ncClientModifyNode(pStub, &meta, psStateName);
    } else if (!strcmp(psCommand, "migrateInstances")) {
        // migration creds can be NULL
        CHECK_PARAM(psInstanceId, "instance ID");
        CHECK_PARAM(psSrcNodeName, "source node name");
        CHECK_PARAM(psDstNodeName, "destination node name");
        CHECK_PARAM(psStateName, "state name");
        ncClientMigrateInstance(pStub, &meta, psInstanceId, psSrcNodeName, psDstNodeName, psStateName, psMigrationCreds);
    } else if (!strcmp(psCommand, "startInstance")) {
        CHECK_PARAM(psInstanceId, "instance ID");
        ncClientStartInstance(pStub, &meta, psInstanceId);
    } else if (!strcmp(psCommand, "stopInstance")) {
        CHECK_PARAM(psInstanceId, "instance ID");
        ncClientStopInstance(pStub, &meta, psInstanceId);
    } else if (!strcmp(psCommand, "_convertTimestamp")) {
        CHECK_PARAM(psTimeStamp, "timestamp");
        ncClientConvertTimeStamp(pStub, psTimeStamp);
    } else {
        fprintf(stderr, "ERROR: command %s unknown (try -h)\n", psCommand);
        exit(1);
    }

    if (local) {
        pthread_exit(NULL);
    }

    _exit(0);
}
