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
//! @file node/client-marshal-local.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <eucalyptus.h>
#define HANDLERS_FANOUT
#include "handlers.h"
#include "client-marshal.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Creates and initialize an NC stub entry
//!
//! @param[in] endpoint_uri the endpoint URI string
//! @param[in] logfile the log file name string
//! @param[in] homedir the home directory path string
//!
//! @return a pointer to the newly created NC stub structure
//!
ncStub *ncStubCreate(char *endpoint_uri, char *logfile, char *homedir)
{
    return (EUCA_ZALLOC(1, sizeof(ncStub)));
}

//!
//! destroy an NC stub structure
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//!
//! @return Always returns EUCA_OK
//!
int ncStubDestroy(ncStub * pStub)
{
    EUCA_FREE(pStub);
    return (EUCA_OK);
}

//!
//! Handles the Run instance request
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  uuid unique user identifier string
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in]  reservationId the reservation identifier string
//! @param[in]  params a pointer to the virtual machine parameters to use
//! @param[in]  imageId the image identifier string
//! @param[in]  imageURL the image URL address tring
//! @param[in]  kernelId the kernel image identifier (eki-XXXXXXXX)
//! @param[in]  kernelURL the kernel image URL address
//! @param[in]  ramdiskId the ramdisk image identifier (eri-XXXXXXXX)
//! @param[in]  ramdiskURL the ramdisk image URL address
//! @param[in]  ownerId the owner identifier string
//! @param[in]  accountId the account identifier string
//! @param[in]  keyName the key name string
//! @param[in]  netparams a pointer to the network parameters string
//! @param[in]  userData the user data string
//! @param[in]  launchIndex the launch index string
//! @param[in]  platform the platform name string
//! @param[in]  expiryTime the reservation expiration time
//! @param[in]  groupNames a list of group name string
//! @param[in]  groupNamesSize the number of group name in the groupNames list
//! @param[out] outInstPtr the list of instances created by this request
//!
//! @return the result of doRunInstance()
//!
//! @see doRunInstance()
//!
int ncRunInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *uuid, char *instanceId, char *reservationId, virtualMachine * params, char *imageId,
                      char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *ownerId, char *accountId,
                      char *keyName, netConfig * netparams, char *userData, char *credential, char *launchIndex, char *platform, int expiryTime, char **groupNames,
                      int groupNamesSize, ncInstance ** outInstPtr)
{
    return doRunInstance(pMeta, uuid, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, ownerId,
                         accountId, keyName, netparams, userData, credential, launchIndex, platform, expiryTime, groupNames, groupNamesSize, outInstPtr);
}

//!
//! Handles the Terminate instance request
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in]  force if set to 1 will force the termination of the instance
//! @param[out] shutdownState the instance state code after the call to find_and_terminate_instance() if successful
//! @param[out] previousState the instance state code after the call to find_and_terminate_instance() if successful
//!
//! @return the result of doTerminateInstance()
//!
//! @see doTerminateInstance()
//!
int ncTerminateInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, int force, int *shutdownState, int *previousState)
{
    return doTerminateInstance(pMeta, instanceId, force, shutdownState, previousState);
}

//!
//! Handles the client network broadcast info request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] networkInfo is a string 
//!
//! @return Always return EUCA_OK
//!
int ncBroadcastNetworkInfoStub(ncStub * pStub, ncMetadata * pMeta, char *networkInfo)
{
    return (EUCA_OK);
}

//!
//! Handles the client assign address request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] publicIp a string representation of the public IP to assign to the instance
//!
//! @return Always return EUCA_OK
//!
int ncAssignAddressStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *publicIp)
{
    return (EUCA_OK);
}

//!
//! Handles the client power down rquest
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return Always return EUCA_OK
//!
int ncPowerDownStub(ncStub * pStub, ncMetadata * pMeta)
{
    return (EUCA_OK);
}

//!
//! Handles the client describe instance request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instIds a pointer the list of instance identifiers to retrieve data for
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[out] outInsts a pointer the list of instances for which we have data
//! @param[out] outInstsLen the number of instances in the outInsts list.
//!
//! @return the result of doDescribeInstances()
//!
//! @see doDescribeInstances()
//!
int ncDescribeInstancesStub(ncStub * pStub, ncMetadata * pMeta, char **instIds, int instIdsLen, ncInstance *** outInsts, int *outInstsLen)
{
    return doDescribeInstances(pMeta, instIds, instIdsLen, outInsts, outInstsLen);
}

//!
//! Handles the client bundle instance request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] bucketName the bucket name string to which the bundle will be saved
//! @param[in] filePrefix the prefix name string of the bundle
//! @param[in] objectStorageURL the object storage service URL address string
//! @param[in] userPublicKey the public key string
//! @param[in] S3Policy the S3 engine policy
//! @param[in] S3PolicySig the S3 engine policy signature
//!
//! @return Always return EUCA_OK
//!
int ncBundleInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *objectStorageURL,
                         char *userPublicKey, char *S3Policy, char *S3PolicySig)
{
    return (EUCA_OK);
}

//!
//! Handles the client restart instance request once bundling has completed.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return Always return EUCA_OK
//!
int ncBundleRestartInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    return (EUCA_OK);
}

//!
//! Handles the client cancel bundle task request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return Always return EUCA_OK
//!
int ncCancelBundleTaskStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    return (EUCA_OK);
}

//!
//! Handles the client describe bundles task request
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instIds a list of instance identifier string
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[out] outBundleTasks a pointer to the created bundle tasks list
//! @param[out] outBundleTasksLen the number of bundle tasks in the outBundleTasks list
//!
//! @return Always return EUCA_OK
//!
int ncDescribeBundleTasksStub(ncStub * pStub, ncMetadata * pMeta, char **instIds, int instIdsLen, bundleTask *** outBundleTasks, int *outBundleTasksLen)
{
    return (EUCA_OK);
}

//!
//! Handle the client describe resource request
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  resourceType UNUSED
//! @param[out] outRes a list of resources we retrieved data for
//!
//! @return the result of doDescribeResource()
//!
//! @see doDescribeResource()
//!
int ncDescribeResourceStub(ncStub * pStub, ncMetadata * pMeta, char *resourceType, ncResource ** outRes)
{
    return doDescribeResource(pMeta, resourceType, outRes);
}

//!
//! Handles the client attach volume request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the target device name
//! @param[in] localDev the local device name
//!
//! @return the result of doAttachVolume()
//!
//! @see doAttachVolume()
//!
int ncAttachVolumeStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev, char *localDev)
{
    return doAttachVolume(pMeta, instanceId, volumeId, remoteDev, localDev);
}

//!
//! Handles the client detach volume request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the target device name
//! @param[in] localDev the local device name
//! @param[in] force if set to 1, this will force the volume to detach
//!
//! @return the result of doDetachVolume()
//!
//! @see doDetachVolume()
//!
int ncDetachVolumeStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force)
{
    return doDetachVolume(pMeta, instanceId, volumeId, remoteDev, localDev, force, 1);
}

//!
//! Handles the client create image request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the remote device name
//!
//! @return the result of doCreateImage()
//!
//! @see doCreateImage()
//!
int ncCreateImageStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev)
{
    return doCreateImage(pMeta, instanceId, volumeId, remoteDev);
}

//!
//! Handles the client describe sensor request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  historySize teh size of the data history to retrieve
//! @param[in]  collectionIntervalTimeMs the data collection interval in milliseconds
//! @param[in]  instIds the list of instance identifiers string
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[in]  sensorIds a list of sensor identifiers string
//! @param[in]  sensorIdsLen the number of sensor identifiers string in the sensorIds list
//! @param[out] outResources a list of sensor resources created by this request
//! @param[out] outResourcesLen the number of sensor resources contained in the outResources list
//!
//! @return the result of doDescribeSensors()
//!
//! @see doDescribeSensors()
//!
int ncDescribeSensorsStub(ncStub * pStub, ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds, int instIdsLen,
                          char **sensorIds, int sensorIdsLen, sensorResource *** outResources, int *outResourcesLen)
{
    return doDescribeSensors(pMeta, historySize, collectionIntervalTimeMs, instIds, instIdsLen, sensorIds, sensorIdsLen, outResources, outResourcesLen);
}

//!
//! Handles the node controller modification request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  stateName the next state for the node controller
//! 
//! @return the result of doModifyNode() (either EUCA_OK or EUCA_ERROR)
//!
//! @see doModifyNode()
//!
int ncModifyNodeStub(ncStub * pStub, ncMetadata * pMeta, char *stateName)
{
    return doModifyNode(pMeta, stateName);
}

//!
//! Handles the instance migration request, with different behavior on source and destination.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instances metadata for the instance to migrate to destination
//! @param[in]  instancesLen number of instances in the instance list
//! @param[in]  action IP of the destination Node Controller
//! @param[in]  credentials credentials that enable the migration
//!
//! @return the result of doMigrateInstances() (either EUCA_OK or EUCA_ERROR)
//!
//! @see doMigrateInstances()
//!
int ncMigrateInstancesStub(ncStub * pStub, ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials)
{
    return doMigrateInstances(pMeta, instances, instancesLen, action, credentials);
}

//!
//! Handles the client start instance request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return Always return EUCA_OK
//!
int ncStartInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    return (EUCA_OK);
}

//!
//! Handles the client instance shutdown request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return Always return EUCA_OK
//!
int ncStopInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    return (EUCA_OK);
}
