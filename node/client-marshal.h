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
//! @file node/client-marshal.h
//! This defines the AXIS2C Client requests handling
//!

#ifndef _INCLUDE_CLIENT_MARSHAL_H_
#define _INCLUDE_CLIENT_MARSHAL_H_

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include "axis2_stub_EucalyptusNC.h"   /* for axis2_ and axutil_ defs */
#include "data.h"                      /* for eucalyptus defs */
#include "sensor.h"                    // sensorResource

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

//! NC Stub structure
typedef struct ncStub_t {
    axutil_env_t *env;                 //!< Pointer to the AXIS2 environment structure
    axis2_char_t *client_home;         //!< The AXIS2 client home directory path string
    axis2_char_t *endpoint_uri;        //!< The AXIS2 endpoint URI string
    axis2_char_t *node_name;           //!< The AXIS2 node name parameter string
    axis2_stub_t *stub;                //!< Pointer to the AXIS2 stub structure
} ncStub;

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

ncStub *ncStubCreate(char *endpoint, char *logfile, char *homedir);
int ncStubDestroy(ncStub * stub);

int ncRunInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *uuid, char *instanceId, char *reservationId, virtualMachine * params, char *imageId,
                      char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *ownerId, char *accountId,
                      char *keyName, netConfig * netparams, char *userData, char *credential, char *launchIndex, char *platform, int expiryTime, char **groupNames,
                      int groupNamesSize, ncInstance ** outInstPtr);
int ncGetConsoleOutputStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char **consoleOutput);
int ncRebootInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId);
int ncTerminateInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, int force, int *shutdownState, int *previousState);
int ncDescribeInstancesStub(ncStub * pStub, ncMetadata * pMeta, char **instIds, int instIdsLen, ncInstance *** outInsts, int *outInstsLen);
int ncDescribeResourceStub(ncStub * pStub, ncMetadata * pMeta, char *resourceType, ncResource ** outRes);
int ncStartNetworkStub(ncStub * pStub, ncMetadata * pMeta, char *uuid, char **peers, int peersLen, int port, int vlan, char **outStatus);
int ncBroadcastNetworkInfoStub(ncStub * pStub, ncMetadata * pMeta, char *networkInfo);
int ncAssignAddressStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *publicIp);
int ncPowerDownStub(ncStub * pStub, ncMetadata * pMeta);
int ncAttachVolumeStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev, char *localDev);
int ncDetachVolumeStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force);
int ncCreateImageStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev);

int ncBundleInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *objectStorageURL,
                         char *userPublicKey, char *S3Policy, char *S3PolicySig);
int ncBundleRestartInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId);
int ncCancelBundleTaskStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId);
int ncDescribeBundleTasksStub(ncStub * pStub, ncMetadata * pMeta, char **instIds, int instIdsLen, bundleTask *** outBundleTasks, int *outBundleTasksLen);

int ncCreateImageStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev);
int ncDescribeSensorsStub(ncStub * pStub, ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds, int instIdsLen,
                          char **sensorIds, int sensorIdsLen, sensorResource *** outResources, int *outResourcesLen);
int ncModifyNodeStub(ncStub * pStub, ncMetadata * pMeta, char *stateName);
int ncMigrateInstancesStub(ncStub * pStub, ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials);
int ncStartInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId);
int ncStopInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId);

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

#endif /* ! _INCLUDE_CLIENT_MARSHAL_H_ */
