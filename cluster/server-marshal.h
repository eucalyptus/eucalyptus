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

#ifndef _INCLUDE_SERVER_MARSHAL_H_
#define _INCLUDE_SERVER_MARSHAL_H_

//!
//! @file cluster/server-marshal.h
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include "axis2_skel_EucalyptusCC.h"
#include <server-marshal-state.h>
#include <handlers.h>

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
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

void adb_InitService(void);
adb_AttachVolumeResponse_t *AttachVolumeMarshal(adb_AttachVolume_t * attachVolume, const axutil_env_t * env);
adb_DetachVolumeResponse_t *DetachVolumeMarshal(adb_DetachVolume_t * detachVolume, const axutil_env_t * env);
adb_BundleInstanceResponse_t *BundleInstanceMarshal(adb_BundleInstance_t * bundleInstance, const axutil_env_t * env);
adb_BundleRestartInstanceResponse_t *BundleRestartInstanceMarshal(adb_BundleRestartInstance_t * bundleInstance, const axutil_env_t * env);
adb_CancelBundleTaskResponse_t *CancelBundleTaskMarshal(adb_CancelBundleTask_t * cancelBundleTask, const axutil_env_t * env);
adb_DescribeSensorsResponse_t *DescribeSensorsMarshal(adb_DescribeSensors_t * describeSensors, const axutil_env_t * env);
adb_StopNetworkResponse_t *StopNetworkMarshal(adb_StopNetwork_t * stopNetwork, const axutil_env_t * env);
adb_DescribeNetworksResponse_t *DescribeNetworksMarshal(adb_DescribeNetworks_t * describeNetworks, const axutil_env_t * env);
adb_DescribePublicAddressesResponse_t *DescribePublicAddressesMarshal(adb_DescribePublicAddresses_t * describePublicAddresses, const axutil_env_t * env);
adb_BroadcastNetworkInfoResponse_t *BroadcastNetworkInfoMarshal(adb_BroadcastNetworkInfo_t * broadcastNetworkInfo, const axutil_env_t * env);
adb_AssignAddressResponse_t *AssignAddressMarshal(adb_AssignAddress_t * assignAddress, const axutil_env_t * env);
adb_UnassignAddressResponse_t *UnassignAddressMarshal(adb_UnassignAddress_t * unassignAddress, const axutil_env_t * env);
adb_ConfigureNetworkResponse_t *ConfigureNetworkMarshal(adb_ConfigureNetwork_t * configureNetwork, const axutil_env_t * env);
adb_GetConsoleOutputResponse_t *GetConsoleOutputMarshal(adb_GetConsoleOutput_t * getConsoleOutput, const axutil_env_t * env);
adb_StartNetworkResponse_t *StartNetworkMarshal(adb_StartNetwork_t * startNetwork, const axutil_env_t * env);
adb_DescribeResourcesResponse_t *DescribeResourcesMarshal(adb_DescribeResources_t * describeResources, const axutil_env_t * env);
adb_DescribeInstancesResponse_t *DescribeInstancesMarshal(adb_DescribeInstances_t * describeInstances, const axutil_env_t * env);
int ccInstanceUnmarshal(adb_ccInstanceType_t * dst, ccInstance * src, const axutil_env_t * env);
adb_RunInstancesResponse_t *RunInstancesMarshal(adb_RunInstances_t * runInstances, const axutil_env_t * env);
adb_RebootInstancesResponse_t *RebootInstancesMarshal(adb_RebootInstances_t * rebootInstances, const axutil_env_t * env);
adb_TerminateInstancesResponse_t *TerminateInstancesMarshal(adb_TerminateInstances_t * terminateInstances, const axutil_env_t * env);
adb_CreateImageResponse_t *CreateImageMarshal(adb_CreateImage_t * createImage, const axutil_env_t * env);
void print_adb_ccInstanceType(adb_ccInstanceType_t * in);
adb_ModifyNodeResponse_t *ModifyNodeMarshal(adb_ModifyNode_t * modifyNode, const axutil_env_t * env);
adb_MigrateInstancesResponse_t *MigrateInstancesMarshal(adb_MigrateInstances_t * migrateInstances, const axutil_env_t * env);
adb_StartInstanceResponse_t *StartInstanceMarshal(adb_StartInstance_t * startInstance, const axutil_env_t * env);
adb_StopInstanceResponse_t *StopInstanceMarshal(adb_StopInstance_t * stopInstance, const axutil_env_t * env);

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

#endif /* ! _INCLUDE_SERVER_MARSHAL_H_ */
