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
//! @file node/client-marshal-adb.c
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

#include <neethi_policy.h>
#include <neethi_util.h>

#include <axis2_stub_EucalyptusNC.h>

#include "client-marshal.h"
#include "handlers.h"

#include <misc.h>
#include <adb-helpers.h>
#include <sensor.h>
#include <euca_string.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define NULL_ERROR_MSG               "operation on %s could not be invoked (check NC host, port, and credentials): %s\n", \
                                     pStub->node_name, axutil_error_get_message(env->error)

#define CORRELATION_ID               NULL   //!< Default Corelation ID value

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
    char *uri = NULL;
    char *p = NULL;
    char *node_name = NULL;
    ncStub *pStub = NULL;
    axutil_env_t *env = NULL;
    axis2_char_t *client_home = NULL;
    axis2_stub_t *stub = NULL;

    axutil_error_init();               // initialize error strings in Axis2

    if (logfile) {
        env = axutil_env_create_all(logfile, AXIS2_LOG_LEVEL_TRACE);
    } else {
        env = axutil_env_create_all(NULL, 0);
    }

    if (homedir) {
        client_home = (axis2_char_t *) homedir;
    } else {
        client_home = AXIS2_GETENV("AXIS2C_HOME");
    }

    if (client_home == NULL) {
        LOGERROR("cannot get AXIS2C_HOME");
        return NULL;
    }

    if (endpoint_uri == NULL) {
        LOGERROR("empty endpoint_url");
        return NULL;
    }

    uri = endpoint_uri;

    // extract node name from the endpoint
    p = strstr(uri, "://");            // find "http[s]://..."
    if (p == NULL) {
        LOGERROR("received invalid URI %s\n", uri);
        return NULL;
    }

    node_name = strdup(p + 3);         // copy without the protocol prefix
    if (node_name == NULL) {
        LOGERROR("is out of memory\n");
        return NULL;
    }

    if ((p = strchr(node_name, ':')) != NULL)
        *p = '\0';                     // cut off the port

    if ((p = strchr(node_name, '/')) != NULL)
        *p = '\0';                     // if there is no port

    // see if we should redirect to a local broker
    if (strstr(uri, "EucalyptusBroker")) {
        uri = "http://localhost:8773/services/EucalyptusBroker";
        LOGDEBUG("redirecting request to %s\n", uri);
    }
    //! @todo what if endpoint_uri, home, or env are NULL?
    stub = axis2_stub_create_EucalyptusNC(env, client_home, (axis2_char_t *) uri);

    if (stub) {
        if ((pStub = EUCA_ZALLOC(1, sizeof(ncStub))) != NULL) {
            pStub->env = env;
            pStub->client_home = strdup((char *)client_home);
            pStub->endpoint_uri = (axis2_char_t *) strdup(endpoint_uri);
            pStub->node_name = (axis2_char_t *) strdup(node_name);
            pStub->stub = stub;
            if (pStub->client_home == NULL || pStub->endpoint_uri == NULL || pStub->node_name == NULL) {
                LOGWARN("out of memory (%s:%s:%d client_home=%s endpoint_uri=%s node_name=%s)", __FILE__, __FUNCTION__, __LINE__,
                        pStub->client_home, pStub->endpoint_uri, pStub->node_name);
            }
        } else {
            LOGWARN("out of memory for 'st' (%s:%s:%d)\n", __FILE__, __FUNCTION__, __LINE__);
        }
    } else {
        LOGERROR("failed to create a stub for EucalyptusNC service (stub=%p env=%p client_home=%s)\n", stub, env, client_home);
    }

    EUCA_FREE(node_name);
    return (pStub);
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
    if (pStub) {
        axis2_stub_free(pStub->stub, pStub->env);
        axutil_env_free(pStub->env);

        EUCA_FREE(pStub->client_home);
        EUCA_FREE(pStub->endpoint_uri);
        EUCA_FREE(pStub->node_name);
        EUCA_FREE(pStub);
    }
    return (EUCA_OK);
}

//!
//! Marshals the Run instance request
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
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncRunInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *uuid, char *instanceId, char *reservationId, virtualMachine * params, char *imageId,
                      char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *ownerId, char *accountId,
                      char *keyName, netConfig * netparams, char *userData, char *credential, char *launchIndex, char *platform, int expiryTime, char **groupNames,
                      int groupNamesSize, ncInstance ** outInstPtr)
{
    int i = 0;
    int status = 0;
    axutil_env_t *env = pStub->env;
    axis2_stub_t *stub = pStub->stub;
    adb_ncRunInstance_t *input = adb_ncRunInstance_create(env);
    adb_ncRunInstanceType_t *request = adb_ncRunInstanceType_create(env);
    axutil_date_time_t *dt = NULL;
    adb_ncRunInstanceResponse_t *output = NULL;
    adb_ncRunInstanceResponseType_t *response = NULL;
    adb_instanceType_t *instance = NULL;

    // set standard input fields
    adb_ncRunInstanceType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncRunInstanceType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncRunInstanceType_set_uuid(request, env, uuid);
    adb_ncRunInstanceType_set_instanceId(request, env, instanceId);
    adb_ncRunInstanceType_set_reservationId(request, env, reservationId);
    adb_ncRunInstanceType_set_instanceType(request, env, copy_vm_type_to_adb(env, params));

    adb_ncRunInstanceType_set_imageId(request, env, imageId);
    adb_ncRunInstanceType_set_imageURL(request, env, imageURL);
    adb_ncRunInstanceType_set_kernelId(request, env, kernelId);
    adb_ncRunInstanceType_set_kernelURL(request, env, kernelURL);
    adb_ncRunInstanceType_set_ramdiskId(request, env, ramdiskId);
    adb_ncRunInstanceType_set_ramdiskURL(request, env, ramdiskURL);
    adb_ncRunInstanceType_set_ownerId(request, env, ownerId);
    adb_ncRunInstanceType_set_accountId(request, env, accountId);
    adb_ncRunInstanceType_set_keyName(request, env, keyName);
    adb_netConfigType_t *netConfig = adb_netConfigType_create(env);
    adb_netConfigType_set_privateMacAddress(netConfig, env, netparams->privateMac);
    adb_netConfigType_set_privateIp(netConfig, env, netparams->privateIp);
    adb_netConfigType_set_publicIp(netConfig, env, netparams->publicIp);
    adb_netConfigType_set_vlan(netConfig, env, netparams->vlan);
    adb_netConfigType_set_networkIndex(netConfig, env, netparams->networkIndex);
    adb_ncRunInstanceType_set_netParams(request, env, netConfig);
    adb_ncRunInstanceType_set_userData(request, env, userData);
    adb_ncRunInstanceType_set_credential(request, env, credential);
    adb_ncRunInstanceType_set_launchIndex(request, env, launchIndex);
    adb_ncRunInstanceType_set_platform(request, env, platform);

    dt = axutil_date_time_create_with_offset(env, expiryTime);
    adb_ncRunInstanceType_set_expiryTime(request, env, dt);

    for (i = 0; i < groupNamesSize; i++) {
        adb_ncRunInstanceType_add_groupNames(request, env, groupNames[i]);
    }

    adb_ncRunInstance_set_ncRunInstance(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncRunInstance(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncRunInstanceResponse_get_ncRunInstanceResponse(output, env);
        if (adb_ncRunInstanceResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("[%s] returned an error\n", instanceId);
            status = 1;
        }

        instance = adb_ncRunInstanceResponseType_get_instance(response, env);
        *outInstPtr = copy_instance_from_adb(instance, env);
    }

    return (status);
}

//!
//! Marshals the get console output request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[out] consoleOutput a pointer to the console output string
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncGetConsoleOutputStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char **consoleOutput)
{
    int status = 0;
    axutil_env_t *env = pStub->env;
    axis2_stub_t *stub = pStub->stub;
    adb_ncGetConsoleOutput_t *input = NULL;
    adb_ncGetConsoleOutputType_t *request = NULL;
    adb_ncGetConsoleOutputResponse_t *output = NULL;
    adb_ncGetConsoleOutputResponseType_t *response = NULL;

    if (!consoleOutput)
        return -1;

    input = adb_ncGetConsoleOutput_create(env);
    request = adb_ncGetConsoleOutputType_create(env);

    /* set input fields */
    adb_ncGetConsoleOutputType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncGetConsoleOutputType, request, pMeta);
    }

    adb_ncGetConsoleOutputType_set_instanceId(request, env, instanceId);
    adb_ncGetConsoleOutput_set_ncGetConsoleOutput(input, env, request);

    /* do it */
    if ((output = axis2_stub_op_EucalyptusNC_ncGetConsoleOutput(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        *consoleOutput = NULL;
        status = -1;
    } else {
        response = adb_ncGetConsoleOutputResponse_get_ncGetConsoleOutputResponse(output, env);
        if (adb_ncGetConsoleOutputResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("[%s] returned an error\n", instanceId);
            status = 1;
        }

        *consoleOutput = adb_ncGetConsoleOutputResponseType_get_consoleOutput(response, env);
    }

    return (status);
}

//!
//! Marshals the reboot instance request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncRebootInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncRebootInstance_t *input = NULL;
    adb_ncRebootInstanceType_t *request = NULL;
    adb_ncRebootInstanceResponse_t *output = NULL;
    adb_ncRebootInstanceResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;

    input = adb_ncRebootInstance_create(env);
    request = adb_ncRebootInstanceType_create(env);

    /* set input fields */
    adb_ncRebootInstanceType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncRebootInstanceType, request, pMeta);
    }

    adb_ncRebootInstanceType_set_instanceId(request, env, instanceId);
    adb_ncRebootInstance_set_ncRebootInstance(input, env, request);

    if ((output = axis2_stub_op_EucalyptusNC_ncRebootInstance(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncRebootInstanceResponse_get_ncRebootInstanceResponse(output, env);
        if (adb_ncRebootInstanceResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("[%s] returned an error\n", instanceId);
            status = 1;
        }

        status = adb_ncRebootInstanceResponseType_get_status(response, env);
    }

    return (status);
}

//!
//! Marshals the Terminate instance request
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in]  force if set to 1 will force the termination of the instance
//! @param[out] shutdownState the instance state code after the call to find_and_terminate_instance() if successful
//! @param[out] previousState the instance state code after the call to find_and_terminate_instance() if successful
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncTerminateInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, int force, int *shutdownState, int *previousState)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncTerminateInstance_t *input = NULL;
    adb_ncTerminateInstanceType_t *request = NULL;
    adb_ncTerminateInstanceResponse_t *output = NULL;
    adb_ncTerminateInstanceResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;

    input = adb_ncTerminateInstance_create(env);
    request = adb_ncTerminateInstanceType_create(env);

    /* set input fields */
    adb_ncTerminateInstanceType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncTerminateInstanceType, request, pMeta);
    }

    adb_ncTerminateInstanceType_set_instanceId(request, env, instanceId);
    if (force) {
        adb_ncTerminateInstanceType_set_force(request, env, AXIS2_TRUE);
    } else {
        adb_ncTerminateInstanceType_set_force(request, env, AXIS2_FALSE);
    }
    adb_ncTerminateInstance_set_ncTerminateInstance(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncTerminateInstance(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncTerminateInstanceResponse_get_ncTerminateInstanceResponse(output, env);
        if (adb_ncTerminateInstanceResponseType_get_return(response, env) == AXIS2_FALSE) {
            // suppress error message because we conservatively call Terminate on all nodes
            //LOGERROR("returned an error\n");
            status = 1;
        }
        //! @todo fix the state char->int conversion
        *shutdownState = 0;            //strdup(adb_ncTerminateInstanceResponseType_get_shutdownState(response, env));
        *previousState = 0;            //strdup(adb_ncTerminateInstanceResponseType_get_previousState(response, env));
    }

    return (status);
}

//!
//! Marshals the client describe instance request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instIds a pointer the list of instance identifiers to retrieve data for
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[out] outInsts a pointer the list of instances for which we have data
//! @param[out] outInstsLen the number of instances in the outInsts list.
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncDescribeInstancesStub(ncStub * pStub, ncMetadata * pMeta, char **instIds, int instIdsLen, ncInstance *** outInsts, int *outInstsLen)
{
    int i = 0;
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_instanceType_t *instance = NULL;
    adb_ncDescribeInstances_t *input = NULL;
    adb_ncDescribeInstancesType_t *request = NULL;
    adb_ncDescribeInstancesResponse_t *output = NULL;
    adb_ncDescribeInstancesResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncDescribeInstances_create(env);
    request = adb_ncDescribeInstancesType_create(env);

    /* set input fields */
    adb_ncDescribeInstancesType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncDescribeInstancesType, request, pMeta);
    }

    for (i = 0; i < instIdsLen; i++) {
        adb_ncDescribeInstancesType_add_instanceIds(request, env, instIds[i]);
    }
    adb_ncDescribeInstances_set_ncDescribeInstances(input, env, request);

    if ((output = axis2_stub_op_EucalyptusNC_ncDescribeInstances(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncDescribeInstancesResponse_get_ncDescribeInstancesResponse(output, env);
        if (adb_ncDescribeInstancesResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("returned an error\n");
            status = 1;
        }

        if ((*outInstsLen = adb_ncDescribeInstancesResponseType_sizeof_instances(response, env)) != 0) {
            if ((*outInsts = EUCA_ZALLOC(*outInstsLen, sizeof(ncInstance *))) == NULL) {
                LOGERROR("out of memory\n");
                *outInstsLen = 0;
                status = 2;
            } else {
                for (i = 0; i < *outInstsLen; i++) {
                    instance = adb_ncDescribeInstancesResponseType_get_instances_at(response, env, i);
                    (*outInsts)[i] = copy_instance_from_adb(instance, env);
                }
            }
        }
    }

    return (status);
}

//!
//! Handle the client describe resource request
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  resourceType UNUSED
//! @param[out] outRes a list of resources we retrieved data for
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncDescribeResourceStub(ncStub * pStub, ncMetadata * pMeta, char *resourceType, ncResource ** outRes)
{
    int status = 0;
    ncResource *res = NULL;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncDescribeResource_t *input = NULL;
    adb_ncDescribeResourceType_t *request = NULL;
    adb_ncDescribeResourceResponse_t *output = NULL;
    adb_ncDescribeResourceResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncDescribeResource_create(env);
    request = adb_ncDescribeResourceType_create(env);

    /* set input fields */
    adb_ncDescribeResourceType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncDescribeResourceType, request, pMeta);
    }

    if (resourceType) {
        adb_ncDescribeResourceType_set_resourceType(request, env, resourceType);
    }
    adb_ncDescribeResource_set_ncDescribeResource(input, env, request);

    if ((output = axis2_stub_op_EucalyptusNC_ncDescribeResource(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncDescribeResourceResponse_get_ncDescribeResourceResponse(output, env);
        if (adb_ncDescribeResourceResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("returned an error\n");
            status = 1;
        }

        res = allocate_resource((char *)adb_ncDescribeResourceResponseType_get_nodeStatus(response, env),
                                (boolean) adb_ncDescribeResourceResponseType_get_migrationCapable(response, env),
                                (char *)adb_ncDescribeResourceResponseType_get_iqn(response, env),
                                (int)adb_ncDescribeResourceResponseType_get_memorySizeMax(response, env),
                                (int)adb_ncDescribeResourceResponseType_get_memorySizeAvailable(response, env),
                                (int)adb_ncDescribeResourceResponseType_get_diskSizeMax(response, env),
                                (int)adb_ncDescribeResourceResponseType_get_diskSizeAvailable(response, env),
                                (int)adb_ncDescribeResourceResponseType_get_numberOfCoresMax(response, env),
                                (int)adb_ncDescribeResourceResponseType_get_numberOfCoresAvailable(response, env),
                                (char *)adb_ncDescribeResourceResponseType_get_publicSubnets(response, env));

        if (!res) {
            LOGERROR("out of memory\n");
            status = 2;
        }
        *outRes = res;
    }

    return (status);
}

//!
//! Marshals the client network broadcast info request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] networkInfo is a string 
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncBroadcastNetworkInfoStub(ncStub * pStub, ncMetadata * pMeta, char *networkInfo)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncBroadcastNetworkInfo_t *input = NULL;
    adb_ncBroadcastNetworkInfoType_t *request = NULL;
    adb_ncBroadcastNetworkInfoResponse_t *output = NULL;
    adb_ncBroadcastNetworkInfoResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncBroadcastNetworkInfo_create(env);
    request = adb_ncBroadcastNetworkInfoType_create(env);

    // set standard input fields
    adb_ncBroadcastNetworkInfoType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncBroadcastNetworkInfoType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncBroadcastNetworkInfoType_set_networkInfo(request, env, networkInfo);

    adb_ncBroadcastNetworkInfo_set_ncBroadcastNetworkInfo(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncBroadcastNetworkInfo(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncBroadcastNetworkInfoResponse_get_ncBroadcastNetworkInfoResponse(output, env);
        if (adb_ncBroadcastNetworkInfoResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("returned an error\n");
            status = 1;
        }
    }

    return (status);
}

//!
//! Marshals the client assign address request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] publicIp a string representation of the public IP to assign to the instance
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncAssignAddressStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *publicIp)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncAssignAddress_t *input = NULL;
    adb_ncAssignAddressType_t *request = NULL;
    adb_ncAssignAddressResponse_t *output = NULL;
    adb_ncAssignAddressResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncAssignAddress_create(env);
    request = adb_ncAssignAddressType_create(env);

    // set standard input fields
    adb_ncAssignAddressType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncAssignAddressType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncAssignAddressType_set_instanceId(request, env, instanceId);
    adb_ncAssignAddressType_set_publicIp(request, env, publicIp);

    adb_ncAssignAddress_set_ncAssignAddress(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncAssignAddress(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncAssignAddressResponse_get_ncAssignAddressResponse(output, env);
        if (adb_ncAssignAddressResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("[%s] returned an error\n", instanceId);
            status = 1;
        }
    }

    return (status);
}

//!
//! Marshals the client power down rquest
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return Always return EUCA_OK.
//!
int ncPowerDownStub(ncStub * pStub, ncMetadata * pMeta)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncPowerDown_t *input = NULL;
    adb_ncPowerDownType_t *request = NULL;
    adb_ncPowerDownResponse_t *output = NULL;
    adb_ncPowerDownResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncPowerDown_create(env);
    request = adb_ncPowerDownType_create(env);

    // set standard input fields
    adb_ncPowerDownType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncPowerDownType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncPowerDown_set_ncPowerDown(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncPowerDown(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncPowerDownResponse_get_ncPowerDownResponse(output, env);
        if (adb_ncPowerDownResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("returned an error\n");
            status = 1;
        }
    }

    return (status);
}

//!
//! Marshals the start network request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  uuid the unique user identifier string
//! @param[in]  peers a list of peers
//! @param[in]  peersLen the number of peers in the peers list
//! @param[in]  port the network port to use
//! @param[in]  vlan the network vlan to use
//! @param[out] outStatus a pointer to the network status
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncStartNetworkStub(ncStub * pStub, ncMetadata * pMeta, char *uuid, char **peers, int peersLen, int port, int vlan, char **outStatus)
{
    int i = 0;
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncStartNetwork_t *input = NULL;
    adb_ncStartNetworkType_t *request = NULL;
    adb_ncStartNetworkResponse_t *output = NULL;
    adb_ncStartNetworkResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncStartNetwork_create(env);
    request = adb_ncStartNetworkType_create(env);

    // set standard input fields
    adb_ncStartNetworkType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncStartNetworkType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncStartNetworkType_set_uuid(request, env, uuid);
    adb_ncStartNetworkType_set_vlan(request, env, vlan);
    adb_ncStartNetworkType_set_remoteHostPort(request, env, port);
    for (i = 0; i < peersLen; i++) {
        adb_ncStartNetworkType_add_remoteHosts(request, env, peers[i]);
    }
    adb_ncStartNetwork_set_ncStartNetwork(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncStartNetwork(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncStartNetworkResponse_get_ncStartNetworkResponse(output, env);
        if (adb_ncStartNetworkResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("returned an error\n");
            status = 1;
        }
        // extract the fields from reponse
        if (outStatus != NULL) {
            *outStatus = strdup(adb_ncStartNetworkResponseType_get_networkStatus(response, env));
        }
    }

    return (status);
}

//!
//! Marshals the client attach volume request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the target device name
//! @param[in] localDev the local device name
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncAttachVolumeStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev, char *localDev)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncAttachVolume_t *input = NULL;
    adb_ncAttachVolumeType_t *request = NULL;
    adb_ncAttachVolumeResponse_t *output = NULL;
    adb_ncAttachVolumeResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncAttachVolume_create(env);
    request = adb_ncAttachVolumeType_create(env);

    // set standard input fields
    adb_ncAttachVolumeType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncAttachVolumeType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncAttachVolumeType_set_instanceId(request, env, instanceId);
    adb_ncAttachVolumeType_set_volumeId(request, env, volumeId);
    adb_ncAttachVolumeType_set_remoteDev(request, env, remoteDev);
    adb_ncAttachVolumeType_set_localDev(request, env, localDev);
    adb_ncAttachVolume_set_ncAttachVolume(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncAttachVolume(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncAttachVolumeResponse_get_ncAttachVolumeResponse(output, env);
        if (adb_ncAttachVolumeResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("[%s][%s] returned an error\n", instanceId, volumeId);
            status = 1;
        }
    }

    return (status);
}

//!
//! Marshals the client detach volume request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the target device name
//! @param[in] localDev the local device name
//! @param[in] force if set to 1, this will force the volume to detach
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncDetachVolumeStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncDetachVolume_t *input = NULL;
    adb_ncDetachVolumeType_t *request = NULL;
    adb_ncDetachVolumeResponse_t *output = NULL;
    adb_ncDetachVolumeResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncDetachVolume_create(env);
    request = adb_ncDetachVolumeType_create(env);

    // set standard input fields
    adb_ncDetachVolumeType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncDetachVolumeType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncDetachVolumeType_set_instanceId(request, env, instanceId);
    adb_ncDetachVolumeType_set_volumeId(request, env, volumeId);
    adb_ncDetachVolumeType_set_remoteDev(request, env, remoteDev);
    adb_ncDetachVolumeType_set_localDev(request, env, localDev);
    if (force) {
        adb_ncDetachVolumeType_set_force(request, env, AXIS2_TRUE);
    } else {
        adb_ncDetachVolumeType_set_force(request, env, AXIS2_FALSE);
    }
    adb_ncDetachVolume_set_ncDetachVolume(input, env, request);

    {
        // do it
        if ((output = axis2_stub_op_EucalyptusNC_ncDetachVolume(stub, env, input)) == NULL) {
            LOGERROR(NULL_ERROR_MSG);
            status = -1;
        } else {
            response = adb_ncDetachVolumeResponse_get_ncDetachVolumeResponse(output, env);
            if (adb_ncDetachVolumeResponseType_get_return(response, env) == AXIS2_FALSE) {
                LOGERROR("[%s][%s] returned an error\n", instanceId, volumeId);
                status = 1;
            }
        }
    }

    return (status);
}

//!
//! Marshals the client bundle instance request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] bucketName the bucket name string to which the bundle will be saved
//! @param[in] filePrefix the prefix name string of the bundle
//! @param[in] objectStorageURL the object storage URL address string
//! @param[in] userPublicKey the public key string
//! @param[in] S3Policy the S3 engine policy
//! @param[in] S3PolicySig the S3 engine policy signature
//!
//! @return Always return EUCA_OK
//!
int ncBundleInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *objectStorageURL,
                         char *userPublicKey, char *S3Policy, char *S3PolicySig)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncBundleInstance_t *input = NULL;
    adb_ncBundleInstanceType_t *request = NULL;
    adb_ncBundleInstanceResponse_t *output = NULL;
    adb_ncBundleInstanceResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncBundleInstance_create(env);
    request = adb_ncBundleInstanceType_create(env);

    // set standard input fields
    adb_ncBundleInstanceType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncBundleInstanceType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncBundleInstanceType_set_instanceId(request, env, instanceId);
    adb_ncBundleInstanceType_set_bucketName(request, env, bucketName);
    adb_ncBundleInstanceType_set_filePrefix(request, env, filePrefix);
    adb_ncBundleInstanceType_set_objectStorageURL(request, env, objectStorageURL);
    adb_ncBundleInstanceType_set_userPublicKey(request, env, userPublicKey);
    adb_ncBundleInstanceType_set_S3Policy(request, env, S3Policy);
    adb_ncBundleInstanceType_set_S3PolicySig(request, env, S3PolicySig);
    adb_ncBundleInstance_set_ncBundleInstance(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncBundleInstance(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncBundleInstanceResponse_get_ncBundleInstanceResponse(output, env);
        if (adb_ncBundleInstanceResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("[%s] returned an error\n", instanceId);
            status = 1;
        }
    }

    return (status);
}

//!
//! Marshals the client restart instance request once bundling has completed.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return Always return EUCA_OK
//!
int ncBundleRestartInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncBundleRestartInstance_t *input = NULL;
    adb_ncBundleRestartInstanceType_t *request = NULL;
    adb_ncBundleRestartInstanceResponse_t *output = NULL;
    adb_ncBundleRestartInstanceResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncBundleRestartInstance_create(env);
    request = adb_ncBundleRestartInstanceType_create(env);

    // set standard input fields
    adb_ncBundleRestartInstanceType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncBundleRestartInstanceType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncBundleRestartInstanceType_set_instanceId(request, env, instanceId);
    adb_ncBundleRestartInstance_set_ncBundleRestartInstance(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncBundleRestartInstance(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncBundleRestartInstanceResponse_get_ncBundleRestartInstanceResponse(output, env);
        if (adb_ncBundleRestartInstanceResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("[%s] returned an error\n", instanceId);
            status = 1;
        }
    }

    return (status);
}

//!
//! Marshals the client cancel bundle task request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return Always return EUCA_OK
//!
int ncCancelBundleTaskStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncCancelBundleTask_t *input = NULL;
    adb_ncCancelBundleTaskType_t *request = NULL;
    adb_ncCancelBundleTaskResponse_t *output = NULL;
    adb_ncCancelBundleTaskResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncCancelBundleTask_create(env);
    request = adb_ncCancelBundleTaskType_create(env);

    // set standard input fields
    adb_ncCancelBundleTaskType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncCancelBundleTaskType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncCancelBundleTaskType_set_instanceId(request, env, instanceId);
    adb_ncCancelBundleTask_set_ncCancelBundleTask(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncCancelBundleTask(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncCancelBundleTaskResponse_get_ncCancelBundleTaskResponse(output, env);
        if (adb_ncCancelBundleTaskResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("[%s] returned an error\n", instanceId);
            status = 1;
        }
    }

    return (status);
}

//!
//! Marshals the client describe bundles task request
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
    int i = 0;
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_bundleTaskType_t *bundle = NULL;
    adb_ncDescribeBundleTasks_t *input = NULL;
    adb_ncDescribeBundleTasksType_t *request = NULL;
    adb_ncDescribeBundleTasksResponse_t *output = NULL;
    adb_ncDescribeBundleTasksResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncDescribeBundleTasks_create(env);
    request = adb_ncDescribeBundleTasksType_create(env);

    // set standard input fields
    if (pMeta) {
        adb_ncDescribeBundleTasksType_set_correlationId(request, env, pMeta->correlationId);
        adb_ncDescribeBundleTasksType_set_userId(request, env, pMeta->userId);
    }
    // set op-specific input fields
    for (i = 0; i < instIdsLen; i++) {
        adb_ncDescribeBundleTasksType_add_instanceIds(request, env, instIds[i]);
    }

    adb_ncDescribeBundleTasks_set_ncDescribeBundleTasks(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncDescribeBundleTasks(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncDescribeBundleTasksResponse_get_ncDescribeBundleTasksResponse(output, env);
        if (adb_ncDescribeBundleTasksResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("returned an error\n");
            status = 1;
        }

        *outBundleTasksLen = adb_ncDescribeBundleTasksResponseType_sizeof_bundleTasks(response, env);
        *outBundleTasks = EUCA_ZALLOC(*outBundleTasksLen, sizeof(bundleTask *));
        for (i = 0; i < *outBundleTasksLen; i++) {
            bundle = adb_ncDescribeBundleTasksResponseType_get_bundleTasks_at(response, env, i);
            (*outBundleTasks)[i] = EUCA_ZALLOC(1, sizeof(bundleTask));
            snprintf((*outBundleTasks)[i]->instanceId, CHAR_BUFFER_SIZE, "%s", adb_bundleTaskType_get_instanceId(bundle, env));
            snprintf((*outBundleTasks)[i]->state, CHAR_BUFFER_SIZE, "%s", adb_bundleTaskType_get_state(bundle, env));
        }
    }

    return (status);
}

//!
//! Marshals the client create image request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the remote device name
//!
//! @return Always return EUCA_OK
//!
int ncCreateImageStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncCreateImage_t *input = NULL;
    adb_ncCreateImageType_t *request = NULL;
    adb_ncCreateImageResponse_t *output = NULL;
    adb_ncCreateImageResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncCreateImage_create(env);
    request = adb_ncCreateImageType_create(env);

    // set standard input fields
    adb_ncCreateImageType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncCreateImageType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncCreateImageType_set_instanceId(request, env, instanceId);
    adb_ncCreateImageType_set_volumeId(request, env, volumeId);
    adb_ncCreateImageType_set_remoteDev(request, env, remoteDev);
    adb_ncCreateImage_set_ncCreateImage(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncCreateImage(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncCreateImageResponse_get_ncCreateImageResponse(output, env);
        if (adb_ncCreateImageResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("[%s] returned an error\n", instanceId);
            status = 1;
        }
    }

    return (status);
}

//!
//! Marshals the client describe sensor request.
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
//! @return Always return EUCA_OK
//!
int ncDescribeSensorsStub(ncStub * pStub, ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds, int instIdsLen,
                          char **sensorIds, int sensorIdsLen, sensorResource *** outResources, int *outResourcesLen)
{
    int i = 0;
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncDescribeSensors_t *input = NULL;
    adb_ncDescribeSensorsType_t *request = NULL;
    adb_ncDescribeSensorsResponse_t *output = NULL;
    adb_ncDescribeSensorsResponseType_t *response = NULL;
    adb_sensorsResourceType_t *resource = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncDescribeSensors_create(env);
    request = adb_ncDescribeSensorsType_create(env);

    // set standard input fields
    adb_ncDescribeSensorsType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncDescribeSensorsType, request, pMeta);
    }
    // set custom input fields
    adb_ncDescribeSensorsType_set_historySize(request, env, historySize);
    adb_ncDescribeSensorsType_set_collectionIntervalTimeMs(request, env, collectionIntervalTimeMs);
    for (i = 0; i < instIdsLen; i++) {
        adb_ncDescribeSensorsType_add_instanceIds(request, env, instIds[i]);
    }

    for (i = 0; i < sensorIdsLen; i++) {
        adb_ncDescribeSensorsType_add_sensorIds(request, env, sensorIds[i]);
    }

    adb_ncDescribeSensors_set_ncDescribeSensors(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncDescribeSensors(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncDescribeSensorsResponse_get_ncDescribeSensorsResponse(output, env);
        if (adb_ncDescribeSensorsResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("returned an error\n");
            status = 1;
        };
        if ((*outResourcesLen = adb_ncDescribeSensorsResponseType_sizeof_sensorsResources(response, env)) > 0) {
            if ((*outResources = EUCA_ZALLOC(*outResourcesLen, sizeof(sensorResource *))) == NULL) {
                LOGERROR("out of memory\n");
                *outResourcesLen = 0;
                status = 2;
            } else {
                for (i = 0; i < *outResourcesLen; i++) {
                    resource = adb_ncDescribeSensorsResponseType_get_sensorsResources_at(response, env, i);
                    (*outResources)[i] = copy_sensor_resource_from_adb(resource, env);
                }
            }
        }
    }

    return (status);
}

//!
//! Marshals the node controller modification request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  stateName the next state for the node controller
//!
//! @return 0 for success, non-zero for error
//!
//! @see ncModifyNode()
//!
int ncModifyNodeStub(ncStub * pStub, ncMetadata * pMeta, char *stateName)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncModifyNode_t *input = NULL;
    adb_ncModifyNodeType_t *request = NULL;
    adb_ncModifyNodeResponse_t *output = NULL;
    adb_ncModifyNodeResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncModifyNode_create(env);
    request = adb_ncModifyNodeType_create(env);

    // set standard input fields
    adb_ncModifyNodeType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncModifyNodeType, request, pMeta);
    }
    // set op-specific input fields
    adb_ncModifyNodeType_set_stateName(request, env, stateName);
    adb_ncModifyNode_set_ncModifyNode(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncModifyNode(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncModifyNodeResponse_get_ncModifyNodeResponse(output, env);
        if (adb_ncModifyNodeResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("returned an error\n");
            status = 1;
        }
        // no output other than success/failure
    }

    return (status);
}

//!
//! Marshals the instance migration request, with different behavior on source and destination.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instances metadata for the instance to migrate to destination
//! @param[in]  instancesLen number of instances in the instance list
//! @param[in]  action IP of the destination Node Controller
//! @param[in]  credentials credentials that enable the migration
//!
//! @return 0 for success, non-zero for error
//!
//! @see ncMigrateInstances()
//!
int ncMigrateInstancesStub(ncStub * pStub, ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncMigrateInstances_t *input = NULL;
    adb_ncMigrateInstancesType_t *request = NULL;
    adb_ncMigrateInstancesResponse_t *output = NULL;
    adb_ncMigrateInstancesResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncMigrateInstances_create(env);
    request = adb_ncMigrateInstancesType_create(env);

    // set standard input fields
    adb_ncMigrateInstancesType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncMigrateInstancesType, request, pMeta);
    }

    LOGDEBUG("marshalling %d instance(s) [0].id=%s with action %s\n", instancesLen, instances[0]->instanceId, action);

    // set op-specific input fields
    for (int i = 0; i < instancesLen; i++) {
        adb_instanceType_t *instance_adb = adb_instanceType_create(env);
        copy_instance_to_adb(instance_adb, env, instances[i]);
        adb_ncMigrateInstancesType_add_instances(request, env, instance_adb);
    }
    adb_ncMigrateInstancesType_set_action(request, env, action);
    if (credentials != NULL)
        adb_ncMigrateInstancesType_set_credentials(request, env, credentials);
    adb_ncMigrateInstances_set_ncMigrateInstances(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncMigrateInstances(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncMigrateInstancesResponse_get_ncMigrateInstancesResponse(output, env);
        if (adb_ncMigrateInstancesResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("returned an error\n");
            status = 1;
        }
        char *statusMessage = adb_ncMigrateInstancesResponseType_get_statusMessage(response, env);
        if (statusMessage != NULL)
            pMeta->replyString = strdup(statusMessage);
    }

    return (status);
}

//!
//! Marshals the StartInstance request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return 0 for success, non-zero for error
//!
//! @see ncStartInstance()
//!
int ncStartInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncStartInstance_t *input = NULL;
    adb_ncStartInstanceType_t *request = NULL;
    adb_ncStartInstanceResponse_t *output = NULL;
    adb_ncStartInstanceResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncStartInstance_create(env);
    request = adb_ncStartInstanceType_create(env);

    // set standard input fields
    adb_ncStartInstanceType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncStartInstanceType, request, pMeta);
    }
    // set op-specific input fields
    // e.g. adb_ncStartInstanceType_set_Z(request, env, Z);
    adb_ncStartInstanceType_set_instanceId(request, env, instanceId);
    adb_ncStartInstance_set_ncStartInstance(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncStartInstance(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncStartInstanceResponse_get_ncStartInstanceResponse(output, env);
        if (adb_ncStartInstanceResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("returned an error\n");
            status = 1;
        }
        // extract the fields from reponse
    }

    return (status);
}

//!
//! Marshals the StopInstance request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return 0 for success, non-zero for error
//!
//! @see ncStopInstance()
//!
int ncStopInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncStopInstance_t *input = NULL;
    adb_ncStopInstanceType_t *request = NULL;
    adb_ncStopInstanceResponse_t *output = NULL;
    adb_ncStopInstanceResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncStopInstance_create(env);
    request = adb_ncStopInstanceType_create(env);

    // set standard input fields
    adb_ncStopInstanceType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
        EUCA_FREE(pMeta->correlationId);
        EUCA_MESSAGE_MARSHAL(ncStopInstanceType, request, pMeta);
    }
    // set op-specific input fields
    // e.g. adb_ncStopInstanceType_set_Z(request, env, Z);
    adb_ncStopInstanceType_set_instanceId(request, env, instanceId);
    adb_ncStopInstance_set_ncStopInstance(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncStopInstance(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncStopInstanceResponse_get_ncStopInstanceResponse(output, env);
        if (adb_ncStopInstanceResponseType_get_return(response, env) == AXIS2_FALSE) {
            LOGERROR("returned an error\n");
            status = 1;
        }
        // extract the fields from reponse
    }

    return (status);
}

/*************************
 a template for future ops
 *************************
//!
//! Marshals the OPERATION request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return 0 for success, non-zero for error
//!
//! @see ncOPERATION()
//!
int ncOPERATIONStub (ncStub *pStub, ncMetadata *pMeta, ...)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ncOPERATION_t *input = NULL;
    adb_ncOPERATIONType_t *request = NULL;
    adb_ncOPERATIONResponse_t *output = NULL;
    adb_ncOPERATIONResponseType_t *response = NULL;

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ncOPERATION_create (env);
    request = adb_ncOPERATIONType_create (env);

    // set standard input fields
    adb_ncOPERATIONType_set_nodeName(request, env, pStub->node_name);
    if (pMeta) {
      EUCA_FREE(pMeta->correlationId);
      EUCA_MESSAGE_MARSHAL(ncOPERATIONType, request, pMeta);
    }

    // set op-specific input fields
    // e.g. adb_ncOPERATIONType_set_Z(request, env, Z);
    adb_ncOPERATION_set_ncOPERATION(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusNC_ncOPERATION (stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ncOPERATIONResponse_get_ncOPERATIONResponse (output, env);
        if ( adb_ncOPERATIONResponseType_get_return(response, env) == AXIS2_FALSE ) {
            LOGERROR("returned an error\n");
            status = 1;
        }

        // extract the fields from reponse
    }

    return (status);
}
 */
