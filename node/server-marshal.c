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
//! @file node/server-marshal.c
//! This implements the AXIS2C server requests handling
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <pthread.h>

#include <eucalyptus.h>

#include <axutil_utils_defines.h>

#include <misc.h>
#include <data.h>

#define HANDLERS_FANOUT
#include "handlers.h"
#include "server-marshal.h"
#include <adb-helpers.h>

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

pthread_mutex_t ncHandlerLock = PTHREAD_MUTEX_INITIALIZER;

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
//! Inits the ADB services
//!
void adb_InitService(void)
{
}

//!
//! Unmarshals, executes, responds to the network broadcast info request.
//!
//! @param[in] ncBroadcastNetworkInfo a pointer to the assign address request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncBroadcastNetworkInfoResponse_t *ncBroadcastNetworkInfoMarshal(adb_ncBroadcastNetworkInfo_t * ncBroadcastNetworkInfo, const axutil_env_t * env)
{
    int error = EUCA_OK;
    char *networkInfo = NULL;
    ncMetadata meta = { 0 };
    adb_ncBroadcastNetworkInfoType_t *input = NULL;
    adb_ncBroadcastNetworkInfoResponse_t *response = NULL;
    adb_ncBroadcastNetworkInfoResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncBroadcastNetworkInfo_get_ncBroadcastNetworkInfo(ncBroadcastNetworkInfo, env);
        response = adb_ncBroadcastNetworkInfoResponse_create(env);
        output = adb_ncBroadcastNetworkInfoResponseType_create(env);

        networkInfo = adb_ncBroadcastNetworkInfoType_get_networkInfo(input, env);

        // get operation-specific fields from input
        EUCA_MESSAGE_UNMARSHAL(ncBroadcastNetworkInfoType, input, (&meta));

        if ((error = doBroadcastNetworkInfo(&meta, networkInfo)) != EUCA_OK) {
            LOGERROR("failed error=%d\n", error);
            adb_ncBroadcastNetworkInfoResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncBroadcastNetworkInfoResponseType_set_userId(output, env, meta.userId);
            adb_ncBroadcastNetworkInfoResponseType_set_return(output, env, AXIS2_FALSE);

            // set operation-specific fields in output
            adb_ncBroadcastNetworkInfoResponseType_set_statusMessage(output, env, "2");
        } else {
            // set standard fields in output
            adb_ncBroadcastNetworkInfoResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncBroadcastNetworkInfoResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncBroadcastNetworkInfoResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncBroadcastNetworkInfoResponseType_set_statusMessage(output, env, "0");
        }

        // set response to output
        adb_ncBroadcastNetworkInfoResponse_set_ncBroadcastNetworkInfoResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the assign address request.
//!
//! @param[in] ncAssignAddress a pointer to the assign address request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncAssignAddressResponse_t *ncAssignAddressMarshal(adb_ncAssignAddress_t * ncAssignAddress, const axutil_env_t * env)
{
    int error = EUCA_OK;
    char *publicIp = NULL;
    char *instanceId = NULL;
    ncMetadata meta = { 0 };
    adb_ncAssignAddressType_t *input = NULL;
    adb_ncAssignAddressResponse_t *response = NULL;
    adb_ncAssignAddressResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncAssignAddress_get_ncAssignAddress(ncAssignAddress, env);
        response = adb_ncAssignAddressResponse_create(env);
        output = adb_ncAssignAddressResponseType_create(env);

        instanceId = adb_ncAssignAddressType_get_instanceId(input, env);
        publicIp = adb_ncAssignAddressType_get_publicIp(input, env);

        // get operation-specific fields from input
        EUCA_MESSAGE_UNMARSHAL(ncAssignAddressType, input, (&meta));

        if ((error = doAssignAddress(&meta, instanceId, publicIp)) != EUCA_OK) {
            LOGERROR("[%s] failed error=%d\n", instanceId, error);
            adb_ncAssignAddressResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncAssignAddressResponseType_set_userId(output, env, meta.userId);
            adb_ncAssignAddressResponseType_set_return(output, env, AXIS2_FALSE);

            // set operation-specific fields in output
            adb_ncAssignAddressResponseType_set_statusMessage(output, env, "2");
        } else {
            // set standard fields in output
            adb_ncAssignAddressResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncAssignAddressResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncAssignAddressResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncAssignAddressResponseType_set_statusMessage(output, env, "0");
        }

        // set response to output
        adb_ncAssignAddressResponse_set_ncAssignAddressResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the power down request.
//!
//! @param[in] ncPowerDown a pointer to the NC power down request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncPowerDownResponse_t *ncPowerDownMarshal(adb_ncPowerDown_t * ncPowerDown, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    adb_ncPowerDownType_t *input = adb_ncPowerDown_get_ncPowerDown(ncPowerDown, env);
    adb_ncPowerDownResponse_t *response = adb_ncPowerDownResponse_create(env);
    adb_ncPowerDownResponseType_t *output = adb_ncPowerDownResponseType_create(env);

    // do it
    EUCA_MESSAGE_UNMARSHAL(ncPowerDownType, input, (&meta));

    if ((error = doPowerDown(&meta)) != EUCA_OK) {
        LOGERROR("failed error=%d\n", error);
        adb_ncPowerDownResponseType_set_correlationId(output, env, meta.correlationId);
        adb_ncPowerDownResponseType_set_userId(output, env, meta.userId);
        adb_ncPowerDownResponseType_set_return(output, env, AXIS2_FALSE);

        // set operation-specific fields in output
        adb_ncPowerDownResponseType_set_statusMessage(output, env, "2");
    } else {
        // set standard fields in output
        adb_ncPowerDownResponseType_set_return(output, env, AXIS2_TRUE);
        adb_ncPowerDownResponseType_set_correlationId(output, env, meta.correlationId);
        adb_ncPowerDownResponseType_set_userId(output, env, meta.userId);

        // set operation-specific fields in output
        adb_ncPowerDownResponseType_set_statusMessage(output, env, "0");
    }

    // set response to output
    adb_ncPowerDownResponse_set_ncPowerDownResponse(response, env, output);
    return (response);
}

//!
//! Unmarshals, executes, responds to the start network request.
//!
//! @param[in] ncStartNetwork a pointer to the start nerwork request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncStartNetworkResponse_t *ncStartNetworkMarshal(adb_ncStartNetwork_t * ncStartNetwork, const axutil_env_t * env)
{
    int i = 0;
    int port = 0;
    int vlan = 0;
    int error = EUCA_OK;
    int peersLen = 0;
    char *uuid = NULL;
    char **peers = NULL;
    ncMetadata meta = { 0 };
    adb_ncStartNetworkType_t *input = NULL;
    adb_ncStartNetworkResponse_t *response = NULL;
    adb_ncStartNetworkResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncStartNetwork_get_ncStartNetwork(ncStartNetwork, env);
        response = adb_ncStartNetworkResponse_create(env);
        output = adb_ncStartNetworkResponseType_create(env);

        // get operation-specific fields from input
        uuid = adb_ncStartNetworkType_get_uuid(input, env);
        port = adb_ncStartNetworkType_get_remoteHostPort(input, env);
        vlan = adb_ncStartNetworkType_get_vlan(input, env);
        if ((peersLen = adb_ncStartNetworkType_sizeof_remoteHosts(input, env)) > 0) {
            peers = EUCA_ZALLOC(peersLen, sizeof(char *));
            for (i = 0; i < peersLen; i++) {
                peers[i] = adb_ncStartNetworkType_get_remoteHosts_at(input, env, i);
            }

            // do it
            EUCA_MESSAGE_UNMARSHAL(ncStartNetworkType, input, (&meta));

            if ((error = doStartNetwork(&meta, uuid, peers, peersLen, port, vlan)) != EUCA_OK) {
                LOGERROR("StartNetwork() invocation failed (error=%d)\n", error);
                adb_ncStartNetworkResponseType_set_return(output, env, AXIS2_FALSE);

                // set operation-specific fields in output
                adb_ncStartNetworkResponseType_set_networkStatus(output, env, "FAIL");
                adb_ncStartNetworkResponseType_set_statusMessage(output, env, "2");
            } else {
                // set standard fields in output
                adb_ncStartNetworkResponseType_set_return(output, env, AXIS2_TRUE);
                adb_ncStartNetworkResponseType_set_correlationId(output, env, meta.correlationId);
                adb_ncStartNetworkResponseType_set_userId(output, env, meta.userId);

                // set operation-specific fields in output
                adb_ncStartNetworkResponseType_set_networkStatus(output, env, "SUCCESS");
                adb_ncStartNetworkResponseType_set_statusMessage(output, env, "0");
            }

            EUCA_FREE(peers);

            // set response to output
            adb_ncStartNetworkResponse_set_ncStartNetworkResponse(response, env, output);
        } else {
            LOGERROR("invalid parameters to StartNetwork (peersLen=%d must be greater than 0)\n", peersLen);
        }
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the describe resource request.
//!
//! @param[in] ncDescribeResource a pointer to the describe resource request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncDescribeResourceResponse_t *ncDescribeResourceMarshal(adb_ncDescribeResource_t * ncDescribeResource, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    ncResource *outRes = NULL;
    axis2_char_t *resourceType = NULL;
    adb_ncDescribeResourceType_t *input = NULL;
    adb_ncDescribeResourceResponse_t *response = NULL;
    adb_ncDescribeResourceResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncDescribeResource_get_ncDescribeResource(ncDescribeResource, env);
        response = adb_ncDescribeResourceResponse_create(env);
        output = adb_ncDescribeResourceResponseType_create(env);

        // get operation-specific fields from input
        resourceType = adb_ncDescribeResourceType_get_resourceType(input, env);

        // do it
        EUCA_MESSAGE_UNMARSHAL(ncDescribeResourceType, input, (&meta));

        if ((error = doDescribeResource(&meta, resourceType, &outRes)) != EUCA_OK) {
            LOGERROR("failed error=%d\n", error);
            adb_ncDescribeResourceResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            // set standard fields in output
            adb_ncDescribeResourceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncDescribeResourceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncDescribeResourceResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncDescribeResourceResponseType_set_nodeStatus(output, env, outRes->nodeStatus);
            adb_ncDescribeResourceResponseType_set_migrationCapable(output, env, outRes->migrationCapable);
            adb_ncDescribeResourceResponseType_set_iqn(output, env, outRes->iqn);
            adb_ncDescribeResourceResponseType_set_memorySizeMax(output, env, outRes->memorySizeMax);
            adb_ncDescribeResourceResponseType_set_memorySizeAvailable(output, env, outRes->memorySizeAvailable);
            adb_ncDescribeResourceResponseType_set_diskSizeMax(output, env, outRes->diskSizeMax);
            adb_ncDescribeResourceResponseType_set_diskSizeAvailable(output, env, outRes->diskSizeAvailable);
            adb_ncDescribeResourceResponseType_set_numberOfCoresMax(output, env, outRes->numberOfCoresMax);
            adb_ncDescribeResourceResponseType_set_numberOfCoresAvailable(output, env, outRes->numberOfCoresAvailable);
            adb_ncDescribeResourceResponseType_set_publicSubnets(output, env, outRes->publicSubnets);
            free_resource(&outRes);
        }

        // set response to output
        adb_ncDescribeResourceResponse_set_ncDescribeResourceResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the run instance request.
//!
//! @param[in] ncRunInstance a pointer to the run instance request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncRunInstanceResponse_t *ncRunInstanceMarshal(adb_ncRunInstance_t * ncRunInstance, const axutil_env_t * env)
{
    int i = 0;
    int error = EUCA_OK;
    int expiryTime = 0;
    int groupNamesSize = 0;
    char **groupNames = NULL;
    netConfig netparams = { 0 };
    ncMetadata meta = { 0 };
    ncInstance *outInst = NULL;
    axis2_char_t *uuid = NULL;
    axis2_char_t *instanceId = NULL;
    axis2_char_t *reservationId = NULL;
    axis2_char_t *imageId = NULL;
    axis2_char_t *imageURL = NULL;
    axis2_char_t *kernelId = NULL;
    axis2_char_t *kernelURL = NULL;
    axis2_char_t *ramdiskId = NULL;
    axis2_char_t *ramdiskURL = NULL;
    axis2_char_t *ownerId = NULL;
    axis2_char_t *accountId = NULL;
    axis2_char_t *keyName = NULL;
    axis2_char_t *userData = NULL;
    axis2_char_t *credential = NULL;
    axis2_char_t *launchIndex = NULL;
    axis2_char_t *platform = NULL;
    virtualMachine params = { 0 };
    axutil_date_time_t *dt = NULL;
    adb_netConfigType_t *net_type = NULL;
    adb_ncRunInstanceType_t *input = NULL;
    adb_ncRunInstanceResponse_t *response = NULL;
    adb_ncRunInstanceResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncRunInstance_get_ncRunInstance(ncRunInstance, env);
        response = adb_ncRunInstanceResponse_create(env);
        output = adb_ncRunInstanceResponseType_create(env);

        // get operation-specific fields from input
        uuid = adb_ncRunInstanceType_get_uuid(input, env);
        instanceId = adb_ncRunInstanceType_get_instanceId(input, env);
        reservationId = adb_ncRunInstanceType_get_reservationId(input, env);
        copy_vm_type_from_adb(&params, adb_ncRunInstanceType_get_instanceType(input, env), env);
        imageId = adb_ncRunInstanceType_get_imageId(input, env);
        imageURL = adb_ncRunInstanceType_get_imageURL(input, env);
        kernelId = adb_ncRunInstanceType_get_kernelId(input, env);
        kernelURL = adb_ncRunInstanceType_get_kernelURL(input, env);
        ramdiskId = adb_ncRunInstanceType_get_ramdiskId(input, env);
        ramdiskURL = adb_ncRunInstanceType_get_ramdiskURL(input, env);
        ownerId = adb_ncRunInstanceType_get_ownerId(input, env);
        accountId = adb_ncRunInstanceType_get_accountId(input, env);
        keyName = adb_ncRunInstanceType_get_keyName(input, env);
        net_type = adb_ncRunInstanceType_get_netParams(input, env);
        netparams.vlan = adb_netConfigType_get_vlan(net_type, env);
        netparams.networkIndex = adb_netConfigType_get_networkIndex(net_type, env);
        snprintf(netparams.privateMac, MAC_BUFFER_SIZE, "%s", adb_netConfigType_get_privateMacAddress(net_type, env));
        snprintf(netparams.privateIp, IP_BUFFER_SIZE, "%s", adb_netConfigType_get_privateIp(net_type, env));
        snprintf(netparams.publicIp, IP_BUFFER_SIZE, "%s", adb_netConfigType_get_publicIp(net_type, env));
        userData = adb_ncRunInstanceType_get_userData(input, env);
        credential = adb_ncRunInstanceType_get_credential(input, env);
        launchIndex = adb_ncRunInstanceType_get_launchIndex(input, env);
        platform = adb_ncRunInstanceType_get_platform(input, env);

        dt = adb_ncRunInstanceType_get_expiryTime(input, env);
        expiryTime = datetime_to_unix(dt, env);

        groupNamesSize = adb_ncRunInstanceType_sizeof_groupNames(input, env);
        if ((groupNames = EUCA_ZALLOC(groupNamesSize, sizeof(char *))) == NULL) {
            LOGERROR("[%s] out of memory. Cannot allocate %d groups.\n", instanceId, groupNamesSize);
            adb_ncRunInstanceResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            for (i = 0; i < groupNamesSize; i++) {
                groupNames[i] = adb_ncRunInstanceType_get_groupNames_at(input, env, i);
            }

            // do it
            EUCA_MESSAGE_UNMARSHAL(ncRunInstanceType, input, (&meta));

            error = doRunInstance(&meta, uuid, instanceId, reservationId, &params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL,
                                  ownerId, accountId, keyName, &netparams, userData, credential, launchIndex, platform, expiryTime, groupNames, groupNamesSize, &outInst);

            if (error != EUCA_OK) {
                LOGERROR("[%s] failed error=%d\n", instanceId, error);
                adb_ncRunInstanceResponseType_set_return(output, env, AXIS2_FALSE);
            } else {
                // set standard fields in output
                adb_ncRunInstanceResponseType_set_return(output, env, AXIS2_TRUE);
                adb_ncRunInstanceResponseType_set_correlationId(output, env, meta.correlationId);
                adb_ncRunInstanceResponseType_set_userId(output, env, meta.userId);

                // set operation-specific fields in output
                adb_instanceType_t *instance = adb_instanceType_create(env);
                copy_instance_to_adb(instance, env, outInst);   // copy all values outInst->instance

                //! @TODO should we free_instance(&outInst) here or not? currently you don't have to
                adb_ncRunInstanceResponseType_set_instance(output, env, instance);
            }

            EUCA_FREE(groupNames);
        }

        // set response to output
        adb_ncRunInstanceResponse_set_ncRunInstanceResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the describe instance request.
//!
//! @param[in] ncDescribeInstances a pointer to the describe instance request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncDescribeInstancesResponse_t *ncDescribeInstancesMarshal(adb_ncDescribeInstances_t * ncDescribeInstances, const axutil_env_t * env)
{
    int i = 0;
    int error = EUCA_OK;
    int instIdsLen = 0;
    int outInstsLen = 0;
    char **instIds = NULL;
    ncMetadata meta = { 0 };
    ncInstance **outInsts = NULL;
    adb_instanceType_t *instance = NULL;
    adb_ncDescribeInstancesType_t *input = NULL;
    adb_ncDescribeInstancesResponse_t *response = NULL;
    adb_ncDescribeInstancesResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncDescribeInstances_get_ncDescribeInstances(ncDescribeInstances, env);
        response = adb_ncDescribeInstancesResponse_create(env);
        output = adb_ncDescribeInstancesResponseType_create(env);

        // get operation-specific fields from input
        instIdsLen = adb_ncDescribeInstancesType_sizeof_instanceIds(input, env);
        if ((instIds = EUCA_ZALLOC(instIdsLen, sizeof(char *))) == NULL) {
            LOGERROR("out of memory\n");
            adb_ncDescribeInstancesResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            for (i = 0; i < instIdsLen; i++) {
                instIds[i] = adb_ncDescribeInstancesType_get_instanceIds_at(input, env, i);
            }

            // do it
            EUCA_MESSAGE_UNMARSHAL(ncDescribeInstancesType, input, (&meta));

            if ((error = doDescribeInstances(&meta, instIds, instIdsLen, &outInsts, &outInstsLen)) != EUCA_OK) {
                LOGERROR("failed error=%d\n", error);
                adb_ncDescribeInstancesResponseType_set_return(output, env, AXIS2_FALSE);
            } else {
                // set standard fields in output
                adb_ncDescribeInstancesResponseType_set_return(output, env, AXIS2_TRUE);
                adb_ncDescribeInstancesResponseType_set_correlationId(output, env, meta.correlationId);
                adb_ncDescribeInstancesResponseType_set_userId(output, env, meta.userId);

                // set operation-specific fields in output
                for (i = 0; i < outInstsLen; i++) {
                    instance = adb_instanceType_create(env);
                    copy_instance_to_adb(instance, env, outInsts[i]);   // copy all values outInst->instance
                    EUCA_FREE(outInsts[i]);

                    //! @TODO should we free_instance(&outInst) here or not? currently you only have to free outInsts[]
                    adb_ncDescribeInstancesResponseType_add_instances(output, env, instance);
                }

                EUCA_FREE(outInsts);
            }
        }

        EUCA_FREE(instIds);

        // set response to output
        adb_ncDescribeInstancesResponse_set_ncDescribeInstancesResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the reboot instance request.
//!
//! @param[in] ncRebootInstance a pointer to the reboot instance request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncRebootInstanceResponse_t *ncRebootInstanceMarshal(adb_ncRebootInstance_t * ncRebootInstance, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    axis2_char_t *instanceId = NULL;
    adb_ncRebootInstanceType_t *input = NULL;
    adb_ncRebootInstanceResponse_t *response = NULL;
    adb_ncRebootInstanceResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncRebootInstance_get_ncRebootInstance(ncRebootInstance, env);
        response = adb_ncRebootInstanceResponse_create(env);
        output = adb_ncRebootInstanceResponseType_create(env);

        // get operation-specific fields from input
        instanceId = adb_ncRebootInstanceType_get_instanceId(input, env);

        // do it
        EUCA_MESSAGE_UNMARSHAL(ncRebootInstanceType, input, (&meta));

        if ((error = doRebootInstance(&meta, instanceId)) != EUCA_OK) {
            LOGERROR("[%s] failed error=%d\n", instanceId, error);
            adb_ncRebootInstanceResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            // set standard fields in output
            adb_ncRebootInstanceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncRebootInstanceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncRebootInstanceResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncRebootInstanceResponseType_set_status(output, env, 0);
        }

        // set response to output
        adb_ncRebootInstanceResponse_set_ncRebootInstanceResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the get console output request.
//!
//! @param[in] ncGetConsoleOutput a pointer to the get console output request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncGetConsoleOutputResponse_t *ncGetConsoleOutputMarshal(adb_ncGetConsoleOutput_t * ncGetConsoleOutput, const axutil_env_t * env)
{
    int error = EUCA_OK;
    char *consoleOutput = NULL;
    ncMetadata meta = { 0 };
    axis2_char_t *instanceId = NULL;
    adb_ncGetConsoleOutputType_t *input = NULL;
    adb_ncGetConsoleOutputResponse_t *response = NULL;
    adb_ncGetConsoleOutputResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncGetConsoleOutput_get_ncGetConsoleOutput(ncGetConsoleOutput, env);
        response = adb_ncGetConsoleOutputResponse_create(env);
        output = adb_ncGetConsoleOutputResponseType_create(env);

        // get operation-specific fields from input
        instanceId = adb_ncGetConsoleOutputType_get_instanceId(input, env);

        // do it
        EUCA_MESSAGE_UNMARSHAL(ncGetConsoleOutputType, input, (&meta));

        if ((error = doGetConsoleOutput(&meta, instanceId, &consoleOutput)) != EUCA_OK) {
            LOGERROR("[%s] failed error=%d\n", instanceId, error);
            adb_ncGetConsoleOutputResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            // set standard fields in output
            adb_ncGetConsoleOutputResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncGetConsoleOutputResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncGetConsoleOutputResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncGetConsoleOutputResponseType_set_consoleOutput(output, env, consoleOutput);
        }

        EUCA_FREE(consoleOutput);

        // set response to output
        adb_ncGetConsoleOutputResponse_set_ncGetConsoleOutputResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the terminate instance request.
//!
//! @param[in] ncTerminateInstance a pointer to the terminate instance request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncTerminateInstanceResponse_t *ncTerminateInstanceMarshal(adb_ncTerminateInstance_t * ncTerminateInstance, const axutil_env_t * env)
{
    int error = EUCA_OK;
    int shutdownState = 0;
    int previousState = 0;
    char s[128] = "";
    boolean force = FALSE;
    ncMetadata meta = { 0 };
    axis2_char_t *instanceId = NULL;
    axis2_bool_t forceBool = AXIS2_FALSE;
    adb_ncTerminateInstanceType_t *input = NULL;
    adb_ncTerminateInstanceResponse_t *response = NULL;
    adb_ncTerminateInstanceResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncTerminateInstance_get_ncTerminateInstance(ncTerminateInstance, env);
        response = adb_ncTerminateInstanceResponse_create(env);
        output = adb_ncTerminateInstanceResponseType_create(env);

        // get operation-specific fields from input
        instanceId = adb_ncTerminateInstanceType_get_instanceId(input, env);
        forceBool = adb_ncTerminateInstanceType_get_force(input, env);
        if (forceBool == AXIS2_TRUE) {
            force = TRUE;
        } else {
            force = FALSE;
        }

        // do it
        EUCA_MESSAGE_UNMARSHAL(ncTerminateInstanceType, input, (&meta));

        if ((error = doTerminateInstance(&meta, instanceId, force, &shutdownState, &previousState)) != EUCA_OK) {
            LOGERROR("[%s] failed error=%d\n", instanceId, error);
            adb_ncTerminateInstanceResponseType_set_return(output, env, AXIS2_FALSE);
            adb_ncTerminateInstanceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncTerminateInstanceResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncTerminateInstanceResponseType_set_instanceId(output, env, instanceId);
        } else {
            // set standard fields in output
            adb_ncTerminateInstanceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncTerminateInstanceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncTerminateInstanceResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncTerminateInstanceResponseType_set_instanceId(output, env, instanceId);

            //! @TODO change the WSDL to use the name/code pair
            snprintf(s, 128, "%d", shutdownState);
            adb_ncTerminateInstanceResponseType_set_shutdownState(output, env, s);
            snprintf(s, 128, "%d", previousState);
            adb_ncTerminateInstanceResponseType_set_previousState(output, env, s);
        }

        // set response to output
        adb_ncTerminateInstanceResponse_set_ncTerminateInstanceResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the attach volume request.
//!
//! @param[in] ncAttachVolume a pointer to the attach volume request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncAttachVolumeResponse_t *ncAttachVolumeMarshal(adb_ncAttachVolume_t * ncAttachVolume, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    axis2_char_t *instanceId = NULL;
    axis2_char_t *volumeId = NULL;
    axis2_char_t *remoteDev = NULL;
    axis2_char_t *localDev = NULL;
    adb_ncAttachVolumeType_t *input = NULL;
    adb_ncAttachVolumeResponse_t *response = NULL;
    adb_ncAttachVolumeResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncAttachVolume_get_ncAttachVolume(ncAttachVolume, env);
        response = adb_ncAttachVolumeResponse_create(env);
        output = adb_ncAttachVolumeResponseType_create(env);

        // get operation-specific fields from input
        instanceId = adb_ncAttachVolumeType_get_instanceId(input, env);
        volumeId = adb_ncAttachVolumeType_get_volumeId(input, env);
        remoteDev = adb_ncAttachVolumeType_get_remoteDev(input, env);
        localDev = adb_ncAttachVolumeType_get_localDev(input, env);

        {
            // do it
            EUCA_MESSAGE_UNMARSHAL(ncAttachVolumeType, input, (&meta));

            if ((error = doAttachVolume(&meta, instanceId, volumeId, remoteDev, localDev)) != EUCA_OK) {
                LOGERROR("[%s][%s] failed error=%d\n", instanceId, volumeId, error);
                adb_ncAttachVolumeResponseType_set_return(output, env, AXIS2_FALSE);
                adb_ncAttachVolumeResponseType_set_correlationId(output, env, meta.correlationId);
                adb_ncAttachVolumeResponseType_set_userId(output, env, meta.userId);
            } else {
                // set standard fields in output
                adb_ncAttachVolumeResponseType_set_return(output, env, AXIS2_TRUE);
                adb_ncAttachVolumeResponseType_set_correlationId(output, env, meta.correlationId);
                adb_ncAttachVolumeResponseType_set_userId(output, env, meta.userId);
                // no operation-specific fields in output
            }
        }

        // set response to output
        adb_ncAttachVolumeResponse_set_ncAttachVolumeResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the detach volume request.
//!
//! @param[in] ncDetachVolume a pointer to the detach volume request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncDetachVolumeResponse_t *ncDetachVolumeMarshal(adb_ncDetachVolume_t * ncDetachVolume, const axutil_env_t * env)
{
    int error = EUCA_OK;
    boolean force = FALSE;
    ncMetadata meta = { 0 };
    axis2_bool_t forceBool = AXIS2_FALSE;
    axis2_char_t *instanceId = NULL;
    axis2_char_t *volumeId = NULL;
    axis2_char_t *remoteDev = NULL;
    axis2_char_t *localDev = NULL;
    adb_ncDetachVolumeType_t *input = NULL;
    adb_ncDetachVolumeResponse_t *response = NULL;
    adb_ncDetachVolumeResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncDetachVolume_get_ncDetachVolume(ncDetachVolume, env);
        response = adb_ncDetachVolumeResponse_create(env);
        output = adb_ncDetachVolumeResponseType_create(env);

        // get operation-specific fields from input
        instanceId = adb_ncDetachVolumeType_get_instanceId(input, env);
        volumeId = adb_ncDetachVolumeType_get_volumeId(input, env);
        remoteDev = adb_ncDetachVolumeType_get_remoteDev(input, env);
        localDev = adb_ncDetachVolumeType_get_localDev(input, env);
        forceBool = adb_ncDetachVolumeType_get_force(input, env);
        if (forceBool == AXIS2_TRUE) {
            force = TRUE;
        } else {
            force = FALSE;
        }

        // do it
        EUCA_MESSAGE_UNMARSHAL(ncDetachVolumeType, input, (&meta));

        if ((error = doDetachVolume(&meta, instanceId, volumeId, remoteDev, localDev, force, 1)) != EUCA_OK) {
            LOGERROR("[%s][%s] failed error=%d\n", instanceId, volumeId, error);
            adb_ncDetachVolumeResponseType_set_return(output, env, AXIS2_FALSE);
            adb_ncDetachVolumeResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncDetachVolumeResponseType_set_userId(output, env, meta.userId);
        } else {
            // set standard fields in output
            adb_ncDetachVolumeResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncDetachVolumeResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncDetachVolumeResponseType_set_userId(output, env, meta.userId);
            // no operation-specific fields in output
        }

        // set response to output
        adb_ncDetachVolumeResponse_set_ncDetachVolumeResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the create image request.
//!
//! @param[in] ncCreateImage a pointer to the create image request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncCreateImageResponse_t *ncCreateImageMarshal(adb_ncCreateImage_t * ncCreateImage, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    axis2_char_t *instanceId = NULL;
    axis2_char_t *volumeId = NULL;
    axis2_char_t *remoteDev = NULL;
    adb_ncCreateImageType_t *input = NULL;
    adb_ncCreateImageResponse_t *response = NULL;
    adb_ncCreateImageResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncCreateImage_get_ncCreateImage(ncCreateImage, env);
        response = adb_ncCreateImageResponse_create(env);
        output = adb_ncCreateImageResponseType_create(env);

        // get operation-specific fields from input
        instanceId = adb_ncCreateImageType_get_instanceId(input, env);
        volumeId = adb_ncCreateImageType_get_volumeId(input, env);
        remoteDev = adb_ncCreateImageType_get_remoteDev(input, env);

        {
            // do it
            EUCA_MESSAGE_UNMARSHAL(ncCreateImageType, input, (&meta));

            if ((error = doCreateImage(&meta, instanceId, volumeId, remoteDev)) != EUCA_OK) {
                LOGERROR("[%s][%s] failed error=%d\n", instanceId, volumeId, error);
                adb_ncCreateImageResponseType_set_return(output, env, AXIS2_FALSE);
                adb_ncCreateImageResponseType_set_correlationId(output, env, meta.correlationId);
                adb_ncCreateImageResponseType_set_userId(output, env, meta.userId);
            } else {
                // set standard fields in output
                adb_ncCreateImageResponseType_set_return(output, env, AXIS2_TRUE);
                adb_ncCreateImageResponseType_set_correlationId(output, env, meta.correlationId);
                adb_ncCreateImageResponseType_set_userId(output, env, meta.userId);
                // no operation-specific fields in output
            }
        }
        // set response to output
        adb_ncCreateImageResponse_set_ncCreateImageResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the bundle instance request.
//!
//! @param[in] ncBundleInstance a pointer to the bundle instance request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncBundleInstanceResponse_t *ncBundleInstanceMarshal(adb_ncBundleInstance_t * ncBundleInstance, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    axis2_char_t *correlationId = NULL;
    axis2_char_t *userId = NULL;
    axis2_char_t *instanceId = NULL;
    axis2_char_t *bucketName = NULL;
    axis2_char_t *filePrefix = NULL;
    axis2_char_t *objectStorageURL = NULL;
    axis2_char_t *userPublicKey = NULL;
    axis2_char_t *S3Policy = NULL;
    axis2_char_t *S3PolicySig = NULL;
    adb_ncBundleInstanceType_t *input = NULL;
    adb_ncBundleInstanceResponse_t *response = NULL;
    adb_ncBundleInstanceResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncBundleInstance_get_ncBundleInstance(ncBundleInstance, env);
        response = adb_ncBundleInstanceResponse_create(env);
        output = adb_ncBundleInstanceResponseType_create(env);

        // get standard fields from input
        correlationId = adb_ncBundleInstanceType_get_correlationId(input, env);
        userId = adb_ncBundleInstanceType_get_userId(input, env);

        // get operation-specific fields from input
        instanceId = adb_ncBundleInstanceType_get_instanceId(input, env);
        bucketName = adb_ncBundleInstanceType_get_bucketName(input, env);
        filePrefix = adb_ncBundleInstanceType_get_filePrefix(input, env);
        objectStorageURL = adb_ncBundleInstanceType_get_objectStorageURL(input, env);
        userPublicKey = adb_ncBundleInstanceType_get_userPublicKey(input, env);
        S3Policy = adb_ncBundleInstanceType_get_S3Policy(input, env);
        S3PolicySig = adb_ncBundleInstanceType_get_S3PolicySig(input, env);

        eventlog("NC", userId, correlationId, "BundleInstance", "begin");

        // do it
        meta.correlationId = correlationId;
        meta.userId = userId;

        if ((error = doBundleInstance(&meta, instanceId, bucketName, filePrefix, objectStorageURL, userPublicKey, S3Policy, S3PolicySig)) != EUCA_OK) {
            LOGERROR("[%s] failed error=%d\n", instanceId, error);
            adb_ncBundleInstanceResponseType_set_return(output, env, AXIS2_FALSE);
            adb_ncBundleInstanceResponseType_set_correlationId(output, env, correlationId);
            adb_ncBundleInstanceResponseType_set_userId(output, env, userId);
        } else {
            // set standard fields in output
            adb_ncBundleInstanceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncBundleInstanceResponseType_set_correlationId(output, env, correlationId);
            adb_ncBundleInstanceResponseType_set_userId(output, env, userId);
            // no operation-specific fields in output
        }

        // set response to output
        adb_ncBundleInstanceResponse_set_ncBundleInstanceResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);

    eventlog("NC", userId, correlationId, "BundleInstance", "end");
    return (response);
}

//!
//! Unmarshals, executes, responds to the bundle restart instance request.
//!
//! @param[in] ncBundleRestartInstance a pointer to the bundle restart instance request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncBundleRestartInstanceResponse_t *ncBundleRestartInstanceMarshal(adb_ncBundleRestartInstance_t * ncBundleRestartInstance, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    axis2_char_t *correlationId = NULL;
    axis2_char_t *userId = NULL;
    axis2_char_t *instanceId = NULL;
    adb_ncBundleRestartInstanceType_t *input = NULL;
    adb_ncBundleRestartInstanceResponse_t *response = NULL;
    adb_ncBundleRestartInstanceResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncBundleRestartInstance_get_ncBundleRestartInstance(ncBundleRestartInstance, env);
        response = adb_ncBundleRestartInstanceResponse_create(env);
        output = adb_ncBundleRestartInstanceResponseType_create(env);

        // get standard fields from input
        correlationId = adb_ncBundleRestartInstanceType_get_correlationId(input, env);
        userId = adb_ncBundleRestartInstanceType_get_userId(input, env);

        // get operation-specific fields from input
        instanceId = adb_ncBundleRestartInstanceType_get_instanceId(input, env);

        eventlog("NC", userId, correlationId, "BundleRestartInstance", "begin");

        // do it
        meta.correlationId = correlationId;
        meta.userId = userId;

        if ((error = doBundleRestartInstance(&meta, instanceId)) != EUCA_OK) {
            LOGERROR("[%s] failed error=%d\n", instanceId, error);
            adb_ncBundleRestartInstanceResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            adb_ncBundleRestartInstanceResponseType_set_return(output, env, AXIS2_TRUE);
        }

        adb_ncBundleRestartInstanceResponseType_set_correlationId(output, env, correlationId);
        adb_ncBundleRestartInstanceResponseType_set_userId(output, env, userId);

        // set response to output
        adb_ncBundleRestartInstanceResponse_set_ncBundleRestartInstanceResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);

    eventlog("NC", userId, correlationId, "BundleRestartInstance", "end");
    return (response);
}

//!
//! Unmarshals, executes, responds to the cancel bundle task request.
//!
//! @param[in] ncCancelBundleTask a pointer to the cancel bundle task request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncCancelBundleTaskResponse_t *ncCancelBundleTaskMarshal(adb_ncCancelBundleTask_t * ncCancelBundleTask, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    axis2_char_t *correlationId = NULL;
    axis2_char_t *userId = NULL;
    axis2_char_t *instanceId = NULL;
    adb_ncCancelBundleTaskType_t *input = NULL;
    adb_ncCancelBundleTaskResponse_t *response = NULL;
    adb_ncCancelBundleTaskResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncCancelBundleTask_get_ncCancelBundleTask(ncCancelBundleTask, env);
        response = adb_ncCancelBundleTaskResponse_create(env);
        output = adb_ncCancelBundleTaskResponseType_create(env);

        // get standard fields from input
        correlationId = adb_ncCancelBundleTaskType_get_correlationId(input, env);
        userId = adb_ncCancelBundleTaskType_get_userId(input, env);

        // get operation-specific fields from input
        instanceId = adb_ncCancelBundleTaskType_get_instanceId(input, env);

        eventlog("NC", userId, correlationId, "CancelBundleTask", "begin");

        // do it
        meta.correlationId = correlationId;
        meta.userId = userId;

        if ((error = doCancelBundleTask(&meta, instanceId)) != EUCA_OK) {
            LOGERROR("[%s] failed error=%d\n", instanceId, error);
            adb_ncCancelBundleTaskResponseType_set_return(output, env, AXIS2_FALSE);
            adb_ncCancelBundleTaskResponseType_set_correlationId(output, env, correlationId);
            adb_ncCancelBundleTaskResponseType_set_userId(output, env, userId);
        } else {
            // set standard fields in output
            adb_ncCancelBundleTaskResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncCancelBundleTaskResponseType_set_correlationId(output, env, correlationId);
            adb_ncCancelBundleTaskResponseType_set_userId(output, env, userId);
            // no operation-specific fields in output
        }

        // set response to output
        adb_ncCancelBundleTaskResponse_set_ncCancelBundleTaskResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);

    eventlog("NC", userId, correlationId, "CancelBundleTask", "end");
    return (response);
}

//!
//! Unmarshals, executes, responds to the describe bundle task request.
//!
//! @param[in] ncDescribeBundleTasks a pointer to the describe bundle task request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncDescribeBundleTasksResponse_t *ncDescribeBundleTasksMarshal(adb_ncDescribeBundleTasks_t * ncDescribeBundleTasks, const axutil_env_t * env)
{
    int i = 0;
    int error = EUCA_OK;
    int instIdsLen = 0;
    int outBundleTasksLen = 0;
    char **instIds = NULL;
    ncMetadata meta = { 0 };
    bundleTask **outBundleTasks = NULL;
    axis2_char_t *correlationId = NULL;
    axis2_char_t *userId = NULL;
    adb_bundleTaskType_t *btt = NULL;
    adb_ncDescribeBundleTasksType_t *input = NULL;
    adb_ncDescribeBundleTasksResponse_t *response = NULL;
    adb_ncDescribeBundleTasksResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncDescribeBundleTasks_get_ncDescribeBundleTasks(ncDescribeBundleTasks, env);
        response = adb_ncDescribeBundleTasksResponse_create(env);
        output = adb_ncDescribeBundleTasksResponseType_create(env);

        // get standard fields from input
        correlationId = adb_ncDescribeBundleTasksType_get_correlationId(input, env);
        userId = adb_ncDescribeBundleTasksType_get_userId(input, env);

        // get operation-specific fields from input
        instIdsLen = adb_ncDescribeBundleTasksType_sizeof_instanceIds(input, env);
        if ((instIds = EUCA_ZALLOC(instIdsLen, sizeof(char *))) == NULL) {
            LOGERROR("out of memory\n");
            adb_ncDescribeBundleTasksResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            for (i = 0; i < instIdsLen; i++) {
                instIds[i] = adb_ncDescribeBundleTasksType_get_instanceIds_at(input, env, i);
            }

            eventlog("NC", userId, correlationId, "DescribeBundleTasks", "begin");

            // do it
            meta.correlationId = correlationId;
            meta.userId = userId;

            if ((error = doDescribeBundleTasks(&meta, instIds, instIdsLen, &outBundleTasks, &outBundleTasksLen)) != EUCA_OK) {
                LOGERROR("failed error=%d\n", error);
                adb_ncDescribeBundleTasksResponseType_set_return(output, env, AXIS2_FALSE);
            } else {
                // set standard fields in output
                adb_ncDescribeBundleTasksResponseType_set_return(output, env, AXIS2_TRUE);
                adb_ncDescribeBundleTasksResponseType_set_correlationId(output, env, correlationId);
                adb_ncDescribeBundleTasksResponseType_set_userId(output, env, userId);

                // set operation specific values
                for (i = 0; i < outBundleTasksLen; i++) {
                    btt = adb_bundleTaskType_create(env);
                    adb_bundleTaskType_set_instanceId(btt, env, outBundleTasks[i]->instanceId);
                    adb_bundleTaskType_set_state(btt, env, outBundleTasks[i]->state);
                    adb_ncDescribeBundleTasksResponseType_add_bundleTasks(output, env, btt);
                    EUCA_FREE(outBundleTasks[i]);
                }

                EUCA_FREE(outBundleTasks);
            }
        }

        // set response to output
        adb_ncDescribeBundleTasksResponse_set_ncDescribeBundleTasksResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);

    eventlog("NC", userId, correlationId, "DescribeBundleTasks", "end");
    return (response);
}

//!
//! Unmarshals, executes, responds to the describe sensors request.
//!
//! @param[in] ncDescribeSensors a pointer to the describe sensors request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncDescribeSensorsResponse_t *ncDescribeSensorsMarshal(adb_ncDescribeSensors_t * ncDescribeSensors, const axutil_env_t * env)
{
    int i = 0;
    int error = EUCA_ERROR;
    int historySize = 0;
    int instIdsLen = 0;
    int sensorIdsLen = 0;
    int outResourcesLen = 0;
    long long collectionIntervalTimeMs = 0;
    char **sensorIds = NULL;
    char **instIds = NULL;
    ncMetadata meta = { 0 };
    axis2_char_t *correlationId = NULL;
    axis2_char_t *userId = NULL;
    sensorResource **outResources = NULL;
    adb_sensorsResourceType_t *resource = NULL;
    adb_ncDescribeSensorsType_t *input = NULL;
    adb_ncDescribeSensorsResponse_t *response = NULL;
    adb_ncDescribeSensorsResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncDescribeSensors_get_ncDescribeSensors(ncDescribeSensors, env);
        response = adb_ncDescribeSensorsResponse_create(env);
        output = adb_ncDescribeSensorsResponseType_create(env);

        // get standard fields from input
        correlationId = adb_ncDescribeSensorsType_get_correlationId(input, env);
        userId = adb_ncDescribeSensorsType_get_userId(input, env);

        // get operation-specific fields from input
        historySize = adb_ncDescribeSensorsType_get_historySize(input, env);
        collectionIntervalTimeMs = adb_ncDescribeSensorsType_get_collectionIntervalTimeMs(input, env);
        if ((instIdsLen = adb_ncDescribeSensorsType_sizeof_instanceIds(input, env)) > 0) {
            if ((instIds = EUCA_ZALLOC(instIdsLen, sizeof(char *))) == NULL) {
                LOGERROR("out of memory for 'instIds'\n");
                goto reply;
            }
        }

        for (i = 0; i < instIdsLen; i++) {
            instIds[i] = adb_ncDescribeSensorsType_get_instanceIds_at(input, env, i);
        }

        if ((sensorIdsLen = adb_ncDescribeSensorsType_sizeof_sensorIds(input, env)) > 0) {
            if ((sensorIds = EUCA_ZALLOC(sensorIdsLen, sizeof(char *))) == NULL) {
                LOGERROR("out of memory for 'sensorIds'\n");
                goto reply;
            }
        }

        for (i = 0; i < sensorIdsLen; i++) {
            sensorIds[i] = adb_ncDescribeSensorsType_get_sensorIds_at(input, env, i);
        }

        // do it
        EUCA_MESSAGE_UNMARSHAL(ncDescribeSensorsType, input, (&meta));

        error = doDescribeSensors(&meta, historySize, collectionIntervalTimeMs, instIds, instIdsLen, sensorIds, sensorIdsLen, &outResources, &outResourcesLen);

        if (error != EUCA_OK) {
            LOGERROR("failed error=%d\n", error);
        } else {
            // set standard fields in output
            adb_ncDescribeSensorsResponseType_set_correlationId(output, env, correlationId);
            adb_ncDescribeSensorsResponseType_set_userId(output, env, userId);

            // set operation-specific fields in output
            for (i = 0; i < outResourcesLen; i++) {
                resource = copy_sensor_resource_to_adb(env, outResources[i], historySize);
                adb_ncDescribeSensorsResponseType_add_sensorsResources(output, env, resource);
            }
        }

        if (outResources) {
            for (i = 0; i < outResourcesLen; i++) {
                EUCA_FREE(outResources[i]);
            }
            EUCA_FREE(outResources);
        }

        EUCA_FREE(sensorIds);

reply:
        EUCA_FREE(instIds);
        if (error != EUCA_OK) {
            adb_ncDescribeSensorsResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            adb_ncDescribeSensorsResponseType_set_return(output, env, AXIS2_TRUE);
        }

        // set response to output
        adb_ncDescribeSensorsResponse_set_ncDescribeSensorsResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);
    return (response);
}

//!
//! Unmarshals, executes, responds to the node modification request.
//!
//! @param[in] ncModifyNode a pointer to the node modification request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncModifyNodeResponse_t *ncModifyNodeMarshal(adb_ncModifyNode_t * ncModifyNode, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    axis2_char_t *correlationId = NULL;
    axis2_char_t *userId = NULL;
    axis2_char_t *stateName = NULL;
    adb_ncModifyNodeType_t *input = NULL;
    adb_ncModifyNodeResponse_t *response = NULL;
    adb_ncModifyNodeResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncModifyNode_get_ncModifyNode(ncModifyNode, env);
        response = adb_ncModifyNodeResponse_create(env);
        output = adb_ncModifyNodeResponseType_create(env);

        // get standard fields from input
        correlationId = adb_ncModifyNodeType_get_correlationId(input, env);
        userId = adb_ncModifyNodeType_get_userId(input, env);

        // get operation-specific fields from input
        stateName = adb_ncModifyNodeType_get_stateName(input, env);

        eventlog("NC", userId, correlationId, "ModifyNode", "begin");

        // do it
        meta.correlationId = correlationId;
        meta.userId = userId;

        error = doModifyNode(&meta, stateName);
        if (error != EUCA_OK) {
            LOGERROR("failed error=%d\n", error);
            adb_ncModifyNodeResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            // set standard fields in output
            adb_ncModifyNodeResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncModifyNodeResponseType_set_correlationId(output, env, correlationId);
            adb_ncModifyNodeResponseType_set_userId(output, env, userId);
            // no operation-specific fields in output
        }

        // set response to output
        adb_ncModifyNodeResponse_set_ncModifyNodeResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);

    eventlog("NC", userId, correlationId, "ModifyNode", "end");
    return (response);
}

//!
//! Unmarshals, executes, responds to the instance migration request.
//!
//! @param[in] ncMigrateInstances a pointer to the instance migration request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncMigrateInstancesResponse_t *ncMigrateInstancesMarshal(adb_ncMigrateInstances_t * ncMigrateInstances, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    ncInstance **instances = NULL;
    int instancesLen = 0;
    axis2_char_t *action = NULL;
    axis2_char_t *credentials = NULL;
    adb_ncMigrateInstancesType_t *input = NULL;
    adb_ncMigrateInstancesResponse_t *response = NULL;
    adb_ncMigrateInstancesResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncMigrateInstances_get_ncMigrateInstances(ncMigrateInstances, env);
        response = adb_ncMigrateInstancesResponse_create(env);
        output = adb_ncMigrateInstancesResponseType_create(env);

        // get operation-specific fields from input
        instancesLen = adb_ncMigrateInstancesType_sizeof_instances(input, env);
        if ((instances = EUCA_ZALLOC(instancesLen, sizeof(ncInstance *))) == NULL) {
            LOGERROR("out of memory\n");
            goto mi_error;
        }
        for (int i = 0; i < instancesLen; i++) {
            adb_instanceType_t *instance_adb = adb_ncMigrateInstancesType_get_instances_at(input, env, i);
            ncInstance *instance = copy_instance_from_adb(instance_adb, env);
            if (instance == NULL) {
                LOGERROR("out of memory\n");
                goto mi_error;
            }
            instances[i] = instance;
        }
        action = adb_ncMigrateInstancesType_get_action(input, env);
        credentials = adb_ncMigrateInstancesType_get_credentials(input, env);

        // get standard fields from input
        EUCA_MESSAGE_UNMARSHAL(ncMigrateInstancesType, input, (&meta));
        meta.nodeName = adb_ncMigrateInstancesType_get_nodeName(input, env);

        eventlog("NC", meta.userId, meta.correlationId, "MigrateInstances", "begin");

        // do it
        error = doMigrateInstances(&meta, instances, instancesLen, action, credentials);
        if (error != EUCA_OK) {
            LOGERROR("failed error=%d\n", error);
mi_error:
            adb_ncMigrateInstancesResponseType_set_return(output, env, AXIS2_FALSE);
            if (meta.replyString != NULL) {
                adb_ncMigrateInstancesResponseType_set_statusMessage(output, env, meta.replyString);
                EUCA_FREE(meta.replyString);
            }
        } else {
            // set standard fields in output
            adb_ncMigrateInstancesResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncMigrateInstancesResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncMigrateInstancesResponseType_set_userId(output, env, meta.userId);
            // no operation-specific fields in output
        }

        // set response to output
        adb_ncMigrateInstancesResponse_set_ncMigrateInstancesResponse(response, env, output);

        // free what we allocated
        for (int i = 0; i < instancesLen; i++) {
            EUCA_FREE(instances[i]);
        }
        EUCA_FREE(instances);
    }
    pthread_mutex_unlock(&ncHandlerLock);

    eventlog("NC", meta.userId, meta.correlationId, "MigrateInstances", "end");
    return (response);
}

//!
//! Unmarshals, executes, responds to the instance start request.
//!
//! @param[in] ncStartInstance a pointer to the instance start request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncStartInstanceResponse_t *ncStartInstanceMarshal(adb_ncStartInstance_t * ncStartInstance, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    axis2_char_t *correlationId = NULL;
    axis2_char_t *userId = NULL;
    axis2_char_t *instanceId = NULL;
    adb_ncStartInstanceType_t *input = NULL;
    adb_ncStartInstanceResponse_t *response = NULL;
    adb_ncStartInstanceResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncStartInstance_get_ncStartInstance(ncStartInstance, env);
        response = adb_ncStartInstanceResponse_create(env);
        output = adb_ncStartInstanceResponseType_create(env);

        // get standard fields from input
        correlationId = adb_ncStartInstanceType_get_correlationId(input, env);
        userId = adb_ncStartInstanceType_get_userId(input, env);

        // get operation-specific fields from input
        instanceId = adb_ncStartInstanceType_get_instanceId(input, env);

        eventlog("NC", userId, correlationId, "StartInstance", "begin");

        // do it
        meta.correlationId = correlationId;
        meta.userId = userId;

        error = doStartInstance(&meta, instanceId);
        if (error != EUCA_OK) {
            LOGERROR("failed error=%d\n", error);
            adb_ncStartInstanceResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            // set standard fields in output
            adb_ncStartInstanceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncStartInstanceResponseType_set_correlationId(output, env, correlationId);
            adb_ncStartInstanceResponseType_set_userId(output, env, userId);
            // set operation-specific fields in output
        }

        // set response to output
        adb_ncStartInstanceResponse_set_ncStartInstanceResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);

    eventlog("NC", userId, correlationId, "StartInstance", "end");
    return (response);
}

//!
//! Unmarshals, executes, responds to the instance stop request.
//!
//! @param[in] ncStopInstance a pointer to the instance stop request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_ncStopInstanceResponse_t *ncStopInstanceMarshal(adb_ncStopInstance_t * ncStopInstance, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    axis2_char_t *correlationId = NULL;
    axis2_char_t *userId = NULL;
    axis2_char_t *instanceId = NULL;
    adb_ncStopInstanceType_t *input = NULL;
    adb_ncStopInstanceResponse_t *response = NULL;
    adb_ncStopInstanceResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncStopInstance_get_ncStopInstance(ncStopInstance, env);
        response = adb_ncStopInstanceResponse_create(env);
        output = adb_ncStopInstanceResponseType_create(env);

        // get standard fields from input
        correlationId = adb_ncStopInstanceType_get_correlationId(input, env);
        userId = adb_ncStopInstanceType_get_userId(input, env);

        // get operation-specific fields from input
        instanceId = adb_ncStopInstanceType_get_instanceId(input, env);

        eventlog("NC", userId, correlationId, "StopInstance", "begin");

        // do it
        meta.correlationId = correlationId;
        meta.userId = userId;

        error = doStopInstance(&meta, instanceId);
        if (error != EUCA_OK) {
            LOGERROR("failed error=%d\n", error);
            adb_ncStopInstanceResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            // set standard fields in output
            adb_ncStopInstanceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncStopInstanceResponseType_set_correlationId(output, env, correlationId);
            adb_ncStopInstanceResponseType_set_userId(output, env, userId);
            // set operation-specific fields in output
        }

        // set response to output
        adb_ncStopInstanceResponse_set_ncStopInstanceResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);

    eventlog("NC", userId, correlationId, "StopInstance", "end");
    return (response);
}

/***********************
 template for future ops
 ***********************

    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    axis2_char_t *correlationId = NULL;
    axis2_char_t *userId = NULL;
    adb_ncOPERATIONType_t *input = NULL;
    adb_ncOPERATIONResponse_t *response = NULL;
    adb_ncOPERATIONResponseType_t *output = NULL;

    pthread_mutex_lock(&ncHandlerLock);
    {
        input = adb_ncOPERATION_get_ncOPERATION(ncOPERATION, env);
        response = adb_ncOPERATIONResponse_create(env);
        output = adb_ncOPERATIONResponseType_create(env);

        // get standard fields from input
        correlationId = adb_ncOPERATIONType_get_correlationId(input, env);
        userId = adb_ncOPERATIONType_get_userId(input, env);

        // get operation-specific fields from input
        // e.g.: instanceId = adb_ncOPERATIONType_get_instanceId(input, env);

        eventlog("NC", userId, correlationId, "OPERATION", "begin");

        // do it
        meta.correlationId = correlationId;
        meta.userId = userId;

        error = doOPERATION (&meta, instanceId, ...
        if (error != EUCA_OK) {
            LOGERROR("failed error=%d\n", error);
            adb_ncOPERATIONResponseType_set_return(output, env, AXIS2_FALSE);
        } else {
            // set standard fields in output
            adb_ncOPERATIONResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncOPERATIONResponseType_set_correlationId(output, env, correlationId);
            adb_ncOPERATIONResponseType_set_userId(output, env, userId);
            // set operation-specific fields in output
        }

        // set response to output
        adb_ncOPERATIONResponse_set_ncOPERATIONResponse(response, env, output);
    }
    pthread_mutex_unlock(&ncHandlerLock);

    eventlog("NC", userId, correlationId, "OPERATION", "end");
    return (response);
*/
