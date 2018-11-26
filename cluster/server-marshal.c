// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

//!
//! @file cluster/server-marshal.c
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
#include <pthread.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_network.h>

#include "handlers.h"
#include "server-marshal.h"
#include <adb-helpers.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define DONOTHING                                0
#define EVENTLOG                                 0

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
extern euca_network *gpEucaNet;

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
//! initialize the AXIS2 services
//!
void adb_InitService(void)
{
    doInitCC();
}

//!
//! Process the attach volume request an provides the response.
//!
//! @param[in] attachVolume a pointer to the attach volume message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_AttachVolumeResponse_t *AttachVolumeMarshal(adb_AttachVolume_t * attachVolume, const axutil_env_t * env)
{
    adb_AttachVolumeResponse_t *ret = NULL;
    adb_attachVolumeResponseType_t *avrt = NULL;

    adb_attachVolumeType_t *avt = NULL;

    int rc;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256];
    char *volumeId = NULL, *instanceId = NULL, *attachmentToken = NULL, *localDev = NULL;
    ncMetadata ccMeta;
    long long call_time = time_ms();

    avt = adb_AttachVolume_get_AttachVolume(attachVolume, env);

    EUCA_MESSAGE_UNMARSHAL(attachVolumeType, avt, (&ccMeta));

    volumeId = adb_attachVolumeType_get_volumeId(avt, env);
    instanceId = adb_attachVolumeType_get_instanceId(avt, env);
    attachmentToken = adb_attachVolumeType_get_remoteDev(avt, env);
    localDev = adb_attachVolumeType_get_localDev(avt, env);

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doAttachVolume(&ccMeta, volumeId, instanceId, attachmentToken, localDev);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doAttachVolume() failed: %d (%s, %s)\n", rc, volumeId, instanceId);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }

    avrt = adb_attachVolumeResponseType_create(env);
    adb_attachVolumeResponseType_set_return(avrt, env, status);
    if (status == AXIS2_FALSE) {
        adb_attachVolumeResponseType_set_statusMessage(avrt, env, statusMessage);
    }

    adb_attachVolumeResponseType_set_correlationId(avrt, env, ccMeta.correlationId);
    adb_attachVolumeResponseType_set_userId(avrt, env, ccMeta.userId);

    ret = adb_AttachVolumeResponse_create(env);
    adb_AttachVolumeResponse_set_AttachVolumeResponse(ret, env, avrt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("AttachVolume", (long)call_time, rc);

    return (ret);
}

//!
//! Process the detach volume request an provides the response.
//!
//! @param[in] detachVolume a pointer to the detach volume message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_DetachVolumeResponse_t *DetachVolumeMarshal(adb_DetachVolume_t * detachVolume, const axutil_env_t * env)
{
    adb_DetachVolumeResponse_t *ret = NULL;
    adb_detachVolumeResponseType_t *dvrt = NULL;
    adb_detachVolumeType_t *dvt = NULL;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    axis2_bool_t forceBool = AXIS2_FALSE;
    char statusMessage[256] = { 0 };
    char *volumeId = NULL;
    char *instanceId = NULL;
    char *attachmentToken = NULL;
    char *localDev = NULL;
    int force = 0;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    dvt = adb_DetachVolume_get_DetachVolume(detachVolume, env);

    EUCA_MESSAGE_UNMARSHAL(detachVolumeType, dvt, (&ccMeta));

    volumeId = adb_detachVolumeType_get_volumeId(dvt, env);
    instanceId = adb_detachVolumeType_get_instanceId(dvt, env);
    attachmentToken = adb_detachVolumeType_get_remoteDev(dvt, env);
    localDev = adb_detachVolumeType_get_localDev(dvt, env);
    forceBool = adb_detachVolumeType_get_force(dvt, env);
    if (forceBool == AXIS2_TRUE) {
        force = 1;
    } else {
        force = 0;
    }

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doDetachVolume(&ccMeta, volumeId, instanceId, attachmentToken, localDev, force);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doDetachVolume() failed: %d (%s, %s)\n", rc, volumeId, instanceId);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }

    dvrt = adb_detachVolumeResponseType_create(env);
    adb_detachVolumeResponseType_set_return(dvrt, env, status);
    if (status == AXIS2_FALSE) {
        adb_detachVolumeResponseType_set_statusMessage(dvrt, env, statusMessage);
    }

    adb_detachVolumeResponseType_set_correlationId(dvrt, env, ccMeta.correlationId);
    adb_detachVolumeResponseType_set_userId(dvrt, env, ccMeta.userId);

    ret = adb_DetachVolumeResponse_create(env);
    adb_DetachVolumeResponse_set_DetachVolumeResponse(ret, env, dvrt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("DetachVolume", (long)call_time, rc);

    return (ret);
}

//!
//! Process the bundle instance request and provides the response
//!
//! @param[in] bundleInstance a pointer to the bundle instance message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_BundleInstanceResponse_t *BundleInstanceMarshal(adb_BundleInstance_t * bundleInstance, const axutil_env_t * env)
{
    adb_BundleInstanceResponse_t *ret = NULL;
    adb_bundleInstanceResponseType_t *birt = NULL;
    adb_bundleInstanceType_t *bit = NULL;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *instanceId = NULL;
    char *bucketName = NULL;
    char *filePrefix = NULL;
    char *objectStorageURL = NULL;
    char *userPublicKey = NULL;
    char *S3Policy = NULL;
    char *S3PolicySig = NULL;
    char *architecture = NULL;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    bit = adb_BundleInstance_get_BundleInstance(bundleInstance, env);

    EUCA_MESSAGE_UNMARSHAL(bundleInstanceType, bit, (&ccMeta));

    instanceId = adb_bundleInstanceType_get_instanceId(bit, env);
    bucketName = adb_bundleInstanceType_get_bucketName(bit, env);
    filePrefix = adb_bundleInstanceType_get_filePrefix(bit, env);
    objectStorageURL = adb_bundleInstanceType_get_objectStorageURL(bit, env);
    userPublicKey = adb_bundleInstanceType_get_userPublicKey(bit, env);
    S3Policy = adb_bundleInstanceType_get_S3Policy(bit, env);
    S3PolicySig = adb_bundleInstanceType_get_S3PolicySig(bit, env);
    architecture = adb_bundleInstanceType_get_architecture(bit, env);

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doBundleInstance(&ccMeta, instanceId, bucketName, filePrefix, objectStorageURL, userPublicKey, S3Policy, S3PolicySig, architecture);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doBundleInstance() failed: %d (%s, %s, %s)\n", rc, instanceId, bucketName, filePrefix);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }

    birt = adb_bundleInstanceResponseType_create(env);
    adb_bundleInstanceResponseType_set_return(birt, env, status);
    if (status == AXIS2_FALSE) {
        adb_bundleInstanceResponseType_set_statusMessage(birt, env, statusMessage);
    }

    adb_bundleInstanceResponseType_set_correlationId(birt, env, ccMeta.correlationId);
    adb_bundleInstanceResponseType_set_userId(birt, env, ccMeta.userId);

    ret = adb_BundleInstanceResponse_create(env);
    adb_BundleInstanceResponse_set_BundleInstanceResponse(ret, env, birt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("BundleInstance", (long)call_time, rc);

    return (ret);
}

//!
//! Process the bundle restart instance request and provides the response
//! Once the bundling is completed, we need to synchronize with CLC prior
//! restarting the instance. Once the CLC detects the bundling activity
//! has completed, it'll send the restart request.
//!
//! @param[in] bundleInstance a pointer to the bundle restart instance message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_BundleRestartInstanceResponse_t *BundleRestartInstanceMarshal(adb_BundleRestartInstance_t * bundleInstance, const axutil_env_t * env)
{
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *instanceId = NULL;
    ncMetadata ccMeta = { 0 };
    adb_BundleRestartInstanceResponse_t *ret = NULL;
    adb_bundleRestartInstanceResponseType_t *birt = NULL;
    adb_bundleRestartInstanceType_t *bit = NULL;
    long long call_time = time_ms();

    bit = adb_BundleRestartInstance_get_BundleRestartInstance(bundleInstance, env);

    EUCA_MESSAGE_UNMARSHAL(bundleRestartInstanceType, bit, (&ccMeta));

    instanceId = adb_bundleRestartInstanceType_get_instanceId(bit, env);
    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        if ((rc = doBundleRestartInstance(&ccMeta, instanceId)) != 0) {
            LOGERROR("doBundleRestartInstance() failed: %d (%s)\n", rc, instanceId);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
        unset_corrid(corr_id);
    }

    birt = adb_bundleRestartInstanceResponseType_create(env);
    adb_bundleRestartInstanceResponseType_set_return(birt, env, status);
    if (status == AXIS2_FALSE)
        adb_bundleRestartInstanceResponseType_set_statusMessage(birt, env, statusMessage);

    adb_bundleRestartInstanceResponseType_set_correlationId(birt, env, ccMeta.correlationId);
    adb_bundleRestartInstanceResponseType_set_userId(birt, env, ccMeta.userId);

    ret = adb_BundleRestartInstanceResponse_create(env);
    adb_BundleRestartInstanceResponse_set_BundleRestartInstanceResponse(ret, env, birt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("BundleRestartInstance", (long)call_time, rc);

    return (ret);
}

//!
//! Process the cancel bundle task request and provides the response
//!
//! @param[in] cancelBundleTask a pointer to the cancel bundle task message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_CancelBundleTaskResponse_t *CancelBundleTaskMarshal(adb_CancelBundleTask_t * cancelBundleTask, const axutil_env_t * env)
{
    adb_CancelBundleTaskResponse_t *ret = NULL;
    adb_cancelBundleTaskResponseType_t *birt = NULL;
    adb_cancelBundleTaskType_t *bit = NULL;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *instanceId = NULL;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    bit = adb_CancelBundleTask_get_CancelBundleTask(cancelBundleTask, env);

    EUCA_MESSAGE_UNMARSHAL(cancelBundleTaskType, bit, (&ccMeta));

    instanceId = adb_cancelBundleTaskType_get_instanceId(bit, env);

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doCancelBundleTask(&ccMeta, instanceId);
        if (rc) {
            LOGERROR("doCancelBundleTask() failed: %d (%s)\n", rc, instanceId);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
        unset_corrid(corr_id);
    }

    birt = adb_cancelBundleTaskResponseType_create(env);
    adb_cancelBundleTaskResponseType_set_return(birt, env, status);
    if (status == AXIS2_FALSE) {
        adb_cancelBundleTaskResponseType_set_statusMessage(birt, env, statusMessage);
    }

    adb_cancelBundleTaskResponseType_set_correlationId(birt, env, ccMeta.correlationId);
    adb_cancelBundleTaskResponseType_set_userId(birt, env, ccMeta.userId);

    ret = adb_CancelBundleTaskResponse_create(env);
    adb_CancelBundleTaskResponse_set_CancelBundleTaskResponse(ret, env, birt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("CancelBundleTask", (long)call_time, rc);

    return (ret);
}

//!
//! Process the describe sensors request and provides the response
//!
//! @param[in] describeSensors a pointer to the describe sensors message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_DescribeSensorsResponse_t *DescribeSensorsMarshal(adb_DescribeSensors_t * describeSensors, const axutil_env_t * env)
{
    int result = EUCA_ERROR;
    long long call_time = time_ms();
    adb_describeSensorsType_t *input = adb_DescribeSensors_get_DescribeSensors(describeSensors, env);
    adb_describeSensorsResponseType_t *output = adb_describeSensorsResponseType_create(env);

    // get operation-specific fields from input
    int historySize = adb_describeSensorsType_get_historySize(input, env);
    long long collectionIntervalTimeMs = adb_describeSensorsType_get_collectionIntervalTimeMs(input, env);

    int instIdsLen = adb_describeSensorsType_sizeof_instanceIds(input, env);
    char **instIds = NULL;
    if (instIdsLen > 0) {
        instIds = EUCA_ZALLOC(instIdsLen, sizeof(char *));
        if (instIds == NULL) {
            LOGERROR("out of memory for 'instIds' in 'DescribeSensorsMarshal'\n");
            goto reply;
        }
    }

    for (int i = 0; i < instIdsLen; i++) {
        instIds[i] = adb_describeSensorsType_get_instanceIds_at(input, env, i);
    }

    int sensorIdsLen = adb_describeSensorsType_sizeof_sensorIds(input, env);
    char **sensorIds = NULL;
    if (sensorIdsLen > 0) {
        sensorIds = EUCA_ZALLOC(sensorIdsLen, sizeof(char *));
        if (sensorIds == NULL) {
            LOGERROR("out of memory for 'sensorIds' in 'DescribeSensorsMarshal'\n");
            goto reply;
        }
    }

    for (int i = 0; i < sensorIdsLen; i++) {
        sensorIds[i] = adb_describeSensorsType_get_sensorIds_at(input, env, i);
    }

    {
        // do it
        ncMetadata meta;
        EUCA_MESSAGE_UNMARSHAL(describeSensorsType, input, (&meta));

        sensorResource **outResources = NULL;
        int outResourcesLen = 0;

        threadCorrelationId *corr_id = set_corrid(meta.correlationId);
        int rc = doDescribeSensors(&meta, historySize, collectionIntervalTimeMs, instIds, instIdsLen, sensorIds, sensorIdsLen, &outResources, &outResourcesLen);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doDescribeSensors() failed: %d (%d, %lld, %d)\n", rc, historySize, collectionIntervalTimeMs, instIdsLen);
            if (outResourcesLen > 0 && outResources != NULL) {
                for (int i = 0; i < outResourcesLen; i++) {
                    EUCA_FREE(outResources[i]);
                }
            }
            EUCA_FREE(outResources);
        } else {
            LOGTRACE("marshalling results outResourcesLen=%d\n", outResourcesLen);

            // set standard fields in output
            adb_describeSensorsResponseType_set_correlationId(output, env, meta.correlationId);
            adb_describeSensorsResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            for (int i = 0; i < outResourcesLen; i++) {
                adb_sensorsResourceType_t *resource = copy_sensor_resource_to_adb(env, outResources[i], historySize);
                EUCA_FREE(outResources[i]);
                adb_describeSensorsResponseType_add_sensorsResources(output, env, resource);
            }
            EUCA_FREE(outResources);

            result = EUCA_OK;          // success
        }
    }

    EUCA_FREE(sensorIds);

reply:

    EUCA_FREE(instIds);

    if (result != EUCA_OK) {
        adb_describeSensorsResponseType_set_return(output, env, AXIS2_FALSE);
    } else {
        adb_describeSensorsResponseType_set_return(output, env, AXIS2_TRUE);
    }

    // set response to output
    adb_DescribeSensorsResponse_t *response = adb_DescribeSensorsResponse_create(env);
    adb_DescribeSensorsResponse_set_DescribeSensorsResponse(response, env, output);

    LOGTRACE("done\n");

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("DescribeSensors", (long)call_time, result);

    return response;
}

//!
//! Process the stop network request and provides the response
//!
//! @param[in] stopNetwork a pointer to the stop network message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_StopNetworkResponse_t *StopNetworkMarshal(adb_StopNetwork_t * stopNetwork, const axutil_env_t * env)
{
    adb_StopNetworkResponse_t *ret = NULL;
    adb_stopNetworkResponseType_t *snrt = NULL;
    adb_stopNetworkType_t *snt = NULL;
    int rc = 0;
    int vlan = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *netName = NULL;
    char *accountId = NULL;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    snt = adb_StopNetwork_get_StopNetwork(stopNetwork, env);

    EUCA_MESSAGE_UNMARSHAL(stopNetworkType, snt, (&ccMeta));

    vlan = adb_stopNetworkType_get_vlan(snt, env);
    netName = adb_stopNetworkType_get_netName(snt, env);
    accountId = adb_stopNetworkType_get_accountId(snt, env);
    if (!accountId) {
        accountId = ccMeta.userId;
    }

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doStopNetwork(&ccMeta, accountId, netName, vlan);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doStopNetwork() failed: %d (%s, %s, %d)\n", rc, accountId, netName, vlan);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }

    snrt = adb_stopNetworkResponseType_create(env);

    adb_stopNetworkResponseType_set_correlationId(snrt, env, ccMeta.correlationId);
    adb_stopNetworkResponseType_set_userId(snrt, env, ccMeta.userId);
    adb_stopNetworkResponseType_set_return(snrt, env, status);
    if (status == AXIS2_FALSE) {
        adb_stopNetworkResponseType_set_statusMessage(snrt, env, statusMessage);
    }

    ret = adb_StopNetworkResponse_create(env);
    adb_StopNetworkResponse_set_StopNetworkResponse(ret, env, snrt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("StopNetwork", (long)call_time, rc);

    return (ret);
}

//!
//! Process the describe networks request and provides the response
//!
//! @param[in] describeNetworks a pointer to the describe network message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_DescribeNetworksResponse_t *DescribeNetworksMarshal(adb_DescribeNetworks_t * describeNetworks, const axutil_env_t * env)
{
    int i = 0;
    int rc = 0;
    int numAddrs = 0;
    int clusterControllersLen = 0;
    char *incc = NULL;
    char statusMessage[256] = { 0 };
    char **clusterControllers = NULL;
    char *vmsubdomain = NULL;
    char *nameservers = NULL;
    char *psNumAddrs = NULL;
    ncMetadata ccMeta = { 0 };
    axis2_bool_t status = AXIS2_TRUE;
    adb_describeNetworksType_t *snt = NULL;
    adb_DescribeNetworksResponse_t *ret = NULL;
    adb_describeNetworksResponseType_t *snrt = NULL;

    snt = adb_DescribeNetworks_get_DescribeNetworks(describeNetworks, env);
    EUCA_MESSAGE_UNMARSHAL(describeNetworksType, snt, (&ccMeta));

    vmsubdomain = adb_describeNetworksType_get_vmsubdomain(snt, env);
    nameservers = adb_describeNetworksType_get_nameserver(snt, env);

    clusterControllersLen = adb_describeNetworksType_sizeof_clusterControllers(snt, env);
    clusterControllers = EUCA_ZALLOC(clusterControllersLen, sizeof(char *));
    for (i = 0; i < clusterControllersLen; i++) {
        incc = adb_describeNetworksType_get_clusterControllers_at(snt, env, i);
        HOST2IP(incc, &clusterControllers[i]);
    }

    snrt = adb_describeNetworksResponseType_create(env);
    status = AXIS2_TRUE;
    if (!DONOTHING) {
        if ((rc = doDescribeNetworks(&ccMeta, clusterControllers, clusterControllersLen)) != 0) {
            LOGERROR("doDescribeNetworks() failed with %d\n", rc);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        } else {
            sem_mywait(NETCONFIG);
            {
                if (!gpEucaNet) {
                    LOGERROR("doDescribeNetworks() failed. Network mode not known yet.\n");
                    status = AXIS2_FALSE;
                    snprintf(statusMessage, 255, "ERROR");
                } else {
                    adb_describeNetworksResponseType_set_mode(snrt, env, gpEucaNet->sMode);
                    adb_describeNetworksResponseType_set_useVlans(snrt, env, 0);
                    adb_describeNetworksResponseType_set_vlanMin(snrt, env, MIN_VLAN_EUCA);
                    adb_describeNetworksResponseType_set_vlanMax(snrt, env, NB_VLAN_802_1Q);
                    status = AXIS2_TRUE;
                }
            }
            sem_mypost(NETCONFIG);
        }
    }

    for (i = 0; i < clusterControllersLen; i++) {
        EUCA_FREE(clusterControllers[i]);
    }
    EUCA_FREE(clusterControllers);

    adb_describeNetworksResponseType_set_return(snrt, env, status);
    if (status == AXIS2_FALSE) {
        adb_describeNetworksResponseType_set_statusMessage(snrt, env, statusMessage);
    }

    adb_describeNetworksResponseType_set_correlationId(snrt, env, ccMeta.correlationId);
    adb_describeNetworksResponseType_set_userId(snrt, env, ccMeta.userId);

    ret = adb_DescribeNetworksResponse_create(env);
    adb_DescribeNetworksResponse_set_DescribeNetworksResponse(ret, env, snrt);
    return (ret);
}

//!
//! Process the broadcastNetworkInfo request and provides the response
//!
//! @param[in] broadcastNetworkInfo a pointer to the broadcastNetworkInfo message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_BroadcastNetworkInfoResponse_t *BroadcastNetworkInfoMarshal(adb_BroadcastNetworkInfo_t * broadcastNetworkInfo, const axutil_env_t * env)
{
    int rc = 0;
    char statusMessage[256] = { 0 };
    char *networkInfo = NULL;
    long long call_time = time_ms();
    ncMetadata ccMeta = { 0 };
    axis2_bool_t status = AXIS2_TRUE;
    adb_BroadcastNetworkInfoResponse_t *ret = NULL;
    adb_broadcastNetworkInfoResponseType_t *response = NULL;
    adb_broadcastNetworkInfoType_t *input = NULL;

    input = adb_BroadcastNetworkInfo_get_BroadcastNetworkInfo(broadcastNetworkInfo, env);

    EUCA_MESSAGE_UNMARSHAL(broadcastNetworkInfoType, input, (&ccMeta));

    networkInfo = adb_broadcastNetworkInfoType_get_networkInfo(input, env);

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doBroadcastNetworkInfo(&ccMeta, networkInfo);
        unset_corrid(corr_id);
        if (rc) {
            // doBroadcastNetworkInfo returns failure in disable state, skipping logging the error since networkInfo can be long
            if (!ccIsEnabled()){
                LOGERROR("doBroadcastNetworkInfo() failed: %d (%s)\n", rc, networkInfo);
            }
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }

    response = adb_broadcastNetworkInfoResponseType_create(env);
    adb_broadcastNetworkInfoResponseType_set_return(response, env, status);
    if (status == AXIS2_FALSE) {
        adb_broadcastNetworkInfoResponseType_set_statusMessage(response, env, statusMessage);
    }

    adb_broadcastNetworkInfoResponseType_set_correlationId(response, env, ccMeta.correlationId);
    adb_broadcastNetworkInfoResponseType_set_userId(response, env, ccMeta.userId);

    ret = adb_BroadcastNetworkInfoResponse_create(env);
    adb_BroadcastNetworkInfoResponse_set_BroadcastNetworkInfoResponse(ret, env, response);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("BroadcastNetworkInfo", (long)call_time, rc);

    return (ret);
}

//!
//! Process the assign address request and provides the response
//!
//! @param[in] assignAddress a pointer to the assign address message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_AssignAddressResponse_t *AssignAddressMarshal(adb_AssignAddress_t * assignAddress, const axutil_env_t * env)
{
    adb_AssignAddressResponse_t *ret = NULL;
    adb_assignAddressResponseType_t *aart = NULL;
    adb_assignAddressType_t *aat = NULL;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *src = NULL;
    char *dst = NULL;
    char *uuid = NULL;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    aat = adb_AssignAddress_get_AssignAddress(assignAddress, env);

    EUCA_MESSAGE_UNMARSHAL(assignAddressType, aat, (&ccMeta));

    src = adb_assignAddressType_get_source(aat, env);
    dst = adb_assignAddressType_get_dest(aat, env);
    uuid = adb_assignAddressType_get_uuid(aat, env);

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doAssignAddress(&ccMeta, uuid, src, dst);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doAssignAddress() failed: %d (%s, %s, %s)\n", rc, src, dst, uuid);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }

    aart = adb_assignAddressResponseType_create(env);
    adb_assignAddressResponseType_set_return(aart, env, status);
    if (status == AXIS2_FALSE) {
        adb_assignAddressResponseType_set_statusMessage(aart, env, statusMessage);
    }

    adb_assignAddressResponseType_set_correlationId(aart, env, ccMeta.correlationId);
    adb_assignAddressResponseType_set_userId(aart, env, ccMeta.userId);

    ret = adb_AssignAddressResponse_create(env);
    adb_AssignAddressResponse_set_AssignAddressResponse(ret, env, aart);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("AssignAddress", (long)call_time, rc);

    return (ret);
}

//!
//! Process the unassign address request and provides the response
//!
//! @param[in] unassignAddress a pointer to the unassign address message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_UnassignAddressResponse_t *UnassignAddressMarshal(adb_UnassignAddress_t * unassignAddress, const axutil_env_t * env)
{
    adb_UnassignAddressResponse_t *ret = NULL;
    adb_unassignAddressResponseType_t *uart = NULL;
    adb_unassignAddressType_t *uat = NULL;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *src = NULL;
    char *dst = NULL;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    uat = adb_UnassignAddress_get_UnassignAddress(unassignAddress, env);
    EUCA_MESSAGE_UNMARSHAL(unassignAddressType, uat, (&ccMeta));

    src = adb_unassignAddressType_get_source(uat, env);
    dst = adb_unassignAddressType_get_dest(uat, env);

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doUnassignAddress(&ccMeta, src, dst);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doUnassignAddress() failed: %d (%s, %s)\n", rc, src, dst);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }

    uart = adb_unassignAddressResponseType_create(env);
    adb_unassignAddressResponseType_set_return(uart, env, status);
    if (status == AXIS2_FALSE) {
        adb_unassignAddressResponseType_set_statusMessage(uart, env, statusMessage);
    }

    adb_unassignAddressResponseType_set_correlationId(uart, env, ccMeta.correlationId);
    adb_unassignAddressResponseType_set_userId(uart, env, ccMeta.userId);

    ret = adb_UnassignAddressResponse_create(env);
    adb_UnassignAddressResponse_set_UnassignAddressResponse(ret, env, uart);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("UnassignAddress", (long)call_time, rc);

    return (ret);
}

//!
//! Process the configure network request and provides the response
//!
//! @param[in] configureNetwork a pointer to the configure network message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_ConfigureNetworkResponse_t *ConfigureNetworkMarshal(adb_ConfigureNetwork_t * configureNetwork, const axutil_env_t * env)
{
    ncMetadata ccMeta = { 0 };
    adb_configureNetworkType_t *cnt = NULL;
    adb_ConfigureNetworkResponse_t *ret = NULL;
    adb_configureNetworkResponseType_t *cnrt = NULL;

    cnt = adb_ConfigureNetwork_get_ConfigureNetwork(configureNetwork, env);
    EUCA_MESSAGE_UNMARSHAL(configureNetworkType, cnt, (&ccMeta));

    cnrt = adb_configureNetworkResponseType_create(env);

    adb_configureNetworkResponseType_set_correlationId(cnrt, env, ccMeta.correlationId);
    adb_configureNetworkResponseType_set_userId(cnrt, env, ccMeta.userId);
    adb_configureNetworkResponseType_set_return(cnrt, env, AXIS2_TRUE);

    ret = adb_ConfigureNetworkResponse_create(env);
    adb_ConfigureNetworkResponse_set_ConfigureNetworkResponse(ret, env, cnrt);

    return (ret);
}

//!
//! Process the get console output request and provides the response
//!
//! @param[in] getConsoleOutput a pointer to the get console output message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_GetConsoleOutputResponse_t *GetConsoleOutputMarshal(adb_GetConsoleOutput_t * getConsoleOutput, const axutil_env_t * env)
{
    adb_GetConsoleOutputResponse_t *ret = NULL;
    adb_getConsoleOutputResponseType_t *gcort = NULL;
    adb_getConsoleOutputType_t *gcot = NULL;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *instId = NULL;
    char *output = NULL;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    gcot = adb_GetConsoleOutput_get_GetConsoleOutput(getConsoleOutput, env);
    EUCA_MESSAGE_UNMARSHAL(getConsoleOutputType, gcot, (&ccMeta));

    instId = adb_getConsoleOutputType_get_instanceId(gcot, env);

    gcort = adb_getConsoleOutputResponseType_create(env);

    status = AXIS2_TRUE;
    output = NULL;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doGetConsoleOutput(&ccMeta, instId, &output);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doGetConsoleOutput() failed: %d (%s)\n", rc, instId);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        } else {
            if (output) {
                adb_getConsoleOutputResponseType_set_consoleOutput(gcort, env, output);
            }
        }
    }
    EUCA_FREE(output);

    adb_getConsoleOutputResponseType_set_correlationId(gcort, env, ccMeta.correlationId);
    adb_getConsoleOutputResponseType_set_userId(gcort, env, ccMeta.userId);
    adb_getConsoleOutputResponseType_set_return(gcort, env, status);
    if (status == AXIS2_FALSE) {
        adb_getConsoleOutputResponseType_set_statusMessage(gcort, env, statusMessage);
    }

    ret = adb_GetConsoleOutputResponse_create(env);
    adb_GetConsoleOutputResponse_set_GetConsoleOutputResponse(ret, env, gcort);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("GetConsoleOutput", (long)call_time, rc);

    return (ret);
}

//!
//! Process the start networks request and provides the response
//!
//! @param[in] startNetwork a pointer to the start network message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_StartNetworkResponse_t *StartNetworkMarshal(adb_StartNetwork_t * startNetwork, const axutil_env_t * env)
{
    adb_StartNetworkResponse_t *ret = NULL;
    adb_startNetworkResponseType_t *snrt = NULL;
    adb_startNetworkType_t *snt = NULL;
    int rc = 0;
    int i = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *netName = NULL;
    char *groupId = NULL;
    char **clusterControllers = NULL;
    char *vmsubdomain = NULL;
    char *nameservers = NULL;
    char *uuid = NULL;
    char *accountId = NULL;
    int vlan = 0;
    int clusterControllersLen = 0;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    snt = adb_StartNetwork_get_StartNetwork(startNetwork, env);
    EUCA_MESSAGE_UNMARSHAL(startNetworkType, snt, (&ccMeta));

    vlan = adb_startNetworkType_get_vlan(snt, env);
    netName = adb_startNetworkType_get_netName(snt, env);
    groupId = adb_startNetworkType_get_groupId(snt, env);
    vmsubdomain = adb_startNetworkType_get_vmsubdomain(snt, env);
    nameservers = adb_startNetworkType_get_nameserver(snt, env);
    uuid = adb_startNetworkType_get_uuid(snt, env);
    accountId = adb_startNetworkType_get_accountId(snt, env);
    if (!accountId) {
        accountId = ccMeta.userId;
    }

    clusterControllersLen = adb_startNetworkType_sizeof_clusterControllers(snt, env);
    clusterControllers = EUCA_ZALLOC(clusterControllersLen, sizeof(char *));
    for (i = 0; i < clusterControllersLen; i++) {
        HOST2IP(adb_startNetworkType_get_clusterControllers_at(snt, env, i), &clusterControllers[i]);
    }

    snrt = adb_startNetworkResponseType_create(env);
    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doStartNetwork(&ccMeta, accountId, uuid, groupId, netName, vlan, vmsubdomain, nameservers, clusterControllers, clusterControllersLen);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doStartNetwork() failed: %d (%s, %s, %s, %d)\n", rc, accountId, uuid, netName, vlan);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }

    for (i = 0; i < clusterControllersLen; i++) {
        EUCA_FREE(clusterControllers[i]);
    }
    EUCA_FREE(clusterControllers);

    adb_startNetworkResponseType_set_return(snrt, env, status);
    if (status == AXIS2_FALSE) {
        adb_startNetworkResponseType_set_statusMessage(snrt, env, statusMessage);
    }

    adb_startNetworkResponseType_set_correlationId(snrt, env, ccMeta.correlationId);
    adb_startNetworkResponseType_set_userId(snrt, env, ccMeta.userId);

    ret = adb_StartNetworkResponse_create(env);
    adb_StartNetworkResponse_set_StartNetworkResponse(ret, env, snrt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("StartNetwork", (long)call_time, rc);

    return (ret);
}

//!
//! Process the describe resources request and provides the response
//!
//! @param[in] describeResources a pointer to the describe resources message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_DescribeResourcesResponse_t *DescribeResourcesMarshal(adb_DescribeResources_t * describeResources, const axutil_env_t * env)
{
    adb_DescribeResourcesResponse_t *ret = NULL;
    adb_describeResourcesResponseType_t *drrt = NULL;
    adb_describeResourcesType_t *drt = NULL;
    int i = 0;
    int rc = 0;
    int *outTypesMax = NULL;
    int *outTypesAvail = NULL;
    int vmLen = 0;
    int outTypesLen = 0;
    ccResource *outNodes = NULL;
    int outNodesLen = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    virtualMachine *vms = NULL;
    adb_virtualMachineType_t *vm = NULL;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    drt = adb_DescribeResources_get_DescribeResources(describeResources, env);

    EUCA_MESSAGE_UNMARSHAL(describeResourcesType, drt, (&ccMeta));

    vmLen = adb_describeResourcesType_sizeof_instanceTypes(drt, env);
    vms = EUCA_ZALLOC(vmLen, sizeof(virtualMachine));

    for (i = 0; i < vmLen; i++) {
        vm = adb_describeResourcesType_get_instanceTypes_at(drt, env, i);
        copy_vm_type_from_adb(&(vms[i]), vm, env);
    }

    // do it
    drrt = adb_describeResourcesResponseType_create(env);

    rc = 1;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doDescribeResources(&ccMeta, &vms, vmLen, &outTypesMax, &outTypesAvail, &outTypesLen, &outNodes, &outNodesLen);
        unset_corrid(corr_id);
    }

    if (rc) {
        LOGERROR("doDescribeResources() failed: %d (%d)\n", rc, vmLen);
        status = AXIS2_FALSE;
        snprintf(statusMessage, 255, "ERROR");
    } else {
        for (i = 0; i < outNodesLen; i++) {
            //      adb_describeResourcesResponseType_add_serviceTags(drrt, env, outNodes[i].ncURL);
            adb_ccNodeType_t *nt = NULL;

            LOGTRACE("node %s %s\n", outNodes[i].ncURL, outNodes[i].iqn);
            nt = adb_ccNodeType_create(env);
            adb_ccNodeType_set_serviceTag(nt, env, outNodes[i].ncURL);
            adb_ccNodeType_set_iqn(nt, env, outNodes[i].iqn);
            adb_ccNodeType_set_hypervisor(nt, env, outNodes[i].hypervisor);
            adb_describeResourcesResponseType_add_nodes(drrt, env, nt);

        }
        EUCA_FREE(outNodes);

        for (i = 0; i < outTypesLen; i++) {
            adb_ccResourceType_t *rt = NULL;

            vm = copy_vm_type_to_adb(env, &(vms[i]));

            rt = adb_ccResourceType_create(env);
            adb_ccResourceType_set_instanceType(rt, env, vm);
            adb_ccResourceType_set_maxInstances(rt, env, outTypesMax[i]);
            adb_ccResourceType_set_availableInstances(rt, env, outTypesAvail[i]);
            adb_describeResourcesResponseType_add_resources(drrt, env, rt);
        }
        EUCA_FREE(outTypesMax);
        EUCA_FREE(outTypesAvail);
    }

    EUCA_FREE(vms);

    adb_describeResourcesResponseType_set_correlationId(drrt, env, ccMeta.correlationId);
    adb_describeResourcesResponseType_set_userId(drrt, env, ccMeta.userId);
    adb_describeResourcesResponseType_set_return(drrt, env, status);
    if (status == AXIS2_FALSE) {
        adb_describeResourcesResponseType_set_statusMessage(drrt, env, statusMessage);
    }
    ret = adb_DescribeResourcesResponse_create(env);
    adb_DescribeResourcesResponse_set_DescribeResourcesResponse(ret, env, drrt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("DescribeResources", (long)call_time, rc);
    return (ret);
}

//!
//! Process the describe instances request and provides the response
//!
//! @param[in] describeInstances a pointer to the describe instances message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_DescribeInstancesResponse_t *DescribeInstancesMarshal(adb_DescribeInstances_t * describeInstances, const axutil_env_t * env)
{
    adb_DescribeInstancesResponse_t *ret = NULL;
    adb_describeInstancesResponseType_t *dirt = NULL;
    adb_describeInstancesType_t *dit = NULL;
    adb_ccInstanceType_t *it = NULL;
    char **instIds = NULL;
    int instIdsLen = 0;
    int outInstsLen = 0;
    int i = 0;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    ccInstance *outInsts = NULL;
    ccInstance *myInstance = NULL;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    dit = adb_DescribeInstances_get_DescribeInstances(describeInstances, env);
    EUCA_MESSAGE_UNMARSHAL(describeInstancesType, dit, (&ccMeta));
    instIdsLen = adb_describeInstancesType_sizeof_instanceIds(dit, env);
    instIds = EUCA_ZALLOC(instIdsLen, sizeof(char *));

    for (i = 0; i < instIdsLen; i++) {
        instIds[i] = adb_describeInstancesType_get_instanceIds_at(dit, env, i);
    }

    dirt = adb_describeInstancesResponseType_create(env);

    rc = 1;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doDescribeInstances(&ccMeta, instIds, instIdsLen, &outInsts, &outInstsLen);
        unset_corrid(corr_id);
    }

    EUCA_FREE(instIds);
    if (rc) {
        LOGERROR("doDescribeInstances() failed: %d (%d)\n", rc, instIdsLen);
        status = AXIS2_FALSE;
        snprintf(statusMessage, 255, "ERROR");
    } else {
        for (i = 0; i < outInstsLen; i++) {
            myInstance = &(outInsts[i]);

            it = adb_ccInstanceType_create(env);
            rc = ccInstanceUnmarshal(it, myInstance, env);
            adb_describeInstancesResponseType_add_instances(dirt, env, it);
        }
        EUCA_FREE(outInsts);
    }

    adb_describeInstancesResponseType_set_correlationId(dirt, env, ccMeta.correlationId);
    adb_describeInstancesResponseType_set_userId(dirt, env, ccMeta.userId);
    adb_describeInstancesResponseType_set_return(dirt, env, status);
    if (status == AXIS2_FALSE) {
        adb_describeInstancesResponseType_set_statusMessage(dirt, env, statusMessage);
    }

    ret = adb_DescribeInstancesResponse_create(env);
    adb_DescribeInstancesResponse_set_DescribeInstancesResponse(ret, env, dirt);

    call_time = time_ms() - call_time;
    cached_message_stats_update("DescribeInstances", (long)call_time, rc);
    return (ret);
}

//!
//! Converts an instance structure to an AXIS2 instance structure.
//!
//! @param[in] dst a pointer to the AXIS2 instance structure
//! @param[in] src a pointer to the instance structure to convert
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int ccInstanceUnmarshal(adb_ccInstanceType_t * dst, ccInstance * src, const axutil_env_t * env)
{
    axutil_date_time_t *dt = NULL;
    adb_virtualMachineType_t *vm = NULL;
    adb_netConfigType_t *netconf = NULL;
    adb_volumeType_t *vol = NULL;
    int i = 0;

    dt = axutil_date_time_create_with_offset(env, src->ts - time(NULL));

    adb_ccInstanceType_set_instanceId(dst, env, src->instanceId);
    adb_ccInstanceType_set_uuid(dst, env, src->uuid);
    adb_ccInstanceType_set_reservationId(dst, env, src->reservationId);
    adb_ccInstanceType_set_ownerId(dst, env, src->ownerId);
    adb_ccInstanceType_set_accountId(dst, env, src->accountId);
    adb_ccInstanceType_set_imageId(dst, env, src->amiId);
    adb_ccInstanceType_set_kernelId(dst, env, src->kernelId);
    adb_ccInstanceType_set_ramdiskId(dst, env, src->ramdiskId);

    adb_ccInstanceType_set_keyName(dst, env, src->keyName);
    adb_ccInstanceType_set_stateName(dst, env, src->state);

    adb_ccInstanceType_set_launchTime(dst, env, dt);

    adb_ccInstanceType_set_serviceTag(dst, env, src->serviceTag);
    adb_ccInstanceType_set_userData(dst, env, src->userData);
    adb_ccInstanceType_set_launchIndex(dst, env, src->launchIndex);
    if (strlen(src->platform)) {
        adb_ccInstanceType_set_platform(dst, env, src->platform);
    }
    if (strlen(src->guestStateName)) {
        adb_ccInstanceType_set_guestStateName(dst, env, src->guestStateName);
    }
    if (strlen(src->bundleTaskStateName)) {
        adb_ccInstanceType_set_bundleTaskStateName(dst, env, src->bundleTaskStateName);
    }
    adb_ccInstanceType_set_bundleTaskProgress(dst, env, src->bundleTaskProgress);
    //GRZE: these strings should be made an enum indexed by the migration_states_t
    if (src->migration_state == MIGRATION_PREPARING) {
        adb_ccInstanceType_set_migrationStateName(dst, env, "preparing");
        if (strlen(src->migration_src) && strlen(src->migration_dst)) {
            adb_ccInstanceType_set_migrationDestination(dst, env, src->migration_dst);
            adb_ccInstanceType_set_migrationSource(dst, env, src->migration_src);
        }
    } else if (src->migration_state == MIGRATION_IN_PROGRESS) {
        adb_ccInstanceType_set_migrationStateName(dst, env, "migrating");
        if (strlen(src->migration_src) && strlen(src->migration_dst)) {
            adb_ccInstanceType_set_migrationDestination(dst, env, src->migration_dst);
            adb_ccInstanceType_set_migrationSource(dst, env, src->migration_src);
        }
    } else {
        adb_ccInstanceType_set_migrationStateName(dst, env, "none");
    }

    adb_ccInstanceType_set_blkbytes(dst, env, src->blkbytes);
    adb_ccInstanceType_set_netbytes(dst, env, src->netbytes);

    for (i = 0; i < 64; i++) {
        if (src->groupNames[i][0] != '\0') {
            adb_ccInstanceType_add_groupNames(dst, env, src->groupNames[i]);
        }
    }

    for (i = 0; i < src->volumesSize; i++) {
        vol = adb_volumeType_create(env);
        adb_volumeType_set_volumeId(vol, env, src->volumes[i].volumeId);
        adb_volumeType_set_remoteDev(vol, env, src->volumes[i].attachmentToken);
        adb_volumeType_set_localDev(vol, env, src->volumes[i].devName);
        adb_volumeType_set_state(vol, env, src->volumes[i].stateName);

        adb_ccInstanceType_add_volumes(dst, env, vol);
    }

    netconf = adb_netConfigType_create(env);
    adb_netConfigType_set_interfaceId(netconf, env, src->ccnet.interfaceId);
    adb_netConfigType_set_device(netconf, env, src->ccnet.device);
    adb_netConfigType_set_privateMacAddress(netconf, env, src->ccnet.privateMac);
    adb_netConfigType_set_privateIp(netconf, env, src->ccnet.privateIp);
    adb_netConfigType_set_publicIp(netconf, env, src->ccnet.publicIp);
    adb_netConfigType_set_vlan(netconf, env, src->ccnet.vlan);
    adb_netConfigType_set_networkIndex(netconf, env, src->ccnet.networkIndex);
    if (strlen(src->ccnet.attachmentId)) // vpc
        adb_netConfigType_set_attachmentId(netconf, env, src->ccnet.attachmentId);
    else // non-vpc
        adb_netConfigType_reset_attachmentId(netconf, env);
    adb_ccInstanceType_set_netParams(dst, env, netconf);

    for (i = 0; i < src->secNetCfgsSize; i++) {
       if (strlen( src->secNetCfgs[i].interfaceId) == 0)
           continue;
       netconf = adb_netConfigType_create(env);
       adb_netConfigType_set_interfaceId(netconf, env, src->secNetCfgs[i].interfaceId);
       adb_netConfigType_set_device(netconf, env, src->secNetCfgs[i].device);
       adb_netConfigType_set_privateMacAddress(netconf, env, src->secNetCfgs[i].privateMac);
       adb_netConfigType_set_privateIp(netconf, env, src->secNetCfgs[i].privateIp);
       adb_netConfigType_set_publicIp(netconf, env, src->secNetCfgs[i].publicIp);
       adb_netConfigType_set_vlan(netconf, env, src->secNetCfgs[i].vlan);
       adb_netConfigType_set_networkIndex(netconf, env, src->secNetCfgs[i].networkIndex);
       adb_netConfigType_set_attachmentId(netconf, env, src->secNetCfgs[i].attachmentId);
       adb_ccInstanceType_add_secondaryNetConfig(dst, env, netconf);
    }

    vm = copy_vm_type_to_adb(env, &(src->ccvm));
    adb_virtualMachineType_set_name(vm, env, src->ccvm.name);
    adb_ccInstanceType_set_instanceType(dst, env, vm);

    return (0);
}

//!
//! Process the run instances request and provides the response
//!
//! @param[in] runInstances a pointer to the run instance message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_RunInstancesResponse_t *RunInstancesMarshal(adb_RunInstances_t * runInstances, const axutil_env_t * env)
{
    int i = 0;
    int rc = 0;
    int vlan = 0;
    int minCount = 0;
    int maxCount = 0;
    int uuidsLen = 0;
    int netIdsLen = 0;
    int expiryTime = 0;
    int instIdsLen = 0;
    int outInstsLen = 0;
    int netNamesLen = 0;
    int macAddrsLen = 0;
    int privateIpsLen = 0;
    int networkIndexListLen = 0;
    int *networkIndexList = NULL;
    char *emiId = NULL;
    char *keyName = NULL;
    char **instIds = NULL;
    char *reservationId = NULL;
    char **netNames = NULL;
    char **netIds = NULL;
    char **macAddrs = NULL;
    char **privateIps = NULL;
    char *kernelId = NULL;
    char *ramdiskId = NULL;
    char *emiURL = NULL;
    char *kernelURL = NULL;
    char *ramdiskURL = NULL;
    char *vmName = NULL;
    char *userData = NULL;
    char *credential = NULL;
    char *launchIndex = NULL;
    char *platform = NULL;
    char *tmp = NULL;
    char **uuids = NULL;
    char *accountId = NULL;
    char *ownerId = NULL;
    char *rootDirective = NULL;
    char *eniAttachmentId = NULL; // only in vpc mode
    char statusMessage[256] = "";
    long long call_time = time_ms();
    ncMetadata ccMeta = { 0 };
    axis2_bool_t status = AXIS2_TRUE;
    virtualMachine ccvm = { 0 };
    axutil_date_time_t *dt = NULL;
    adb_RunInstancesResponse_t *ret = NULL;
    adb_runInstancesResponseType_t *rirt = NULL;
    adb_runInstancesType_t *rit = NULL;
    adb_ccInstanceType_t *it = NULL;
    adb_virtualMachineType_t *vm = NULL;
    ccInstance *outInsts = NULL;
    ccInstance *myInstance = NULL;
    netConfig secNetCfgs[EUCA_MAX_NICS] = {{ 0 }}; // only in vpc mode
    int secNetCfgsLen = 0;

    rit = adb_RunInstances_get_RunInstances(runInstances, env);
    EUCA_MESSAGE_UNMARSHAL(runInstancesType, rit, (&ccMeta));

    reservationId = adb_runInstancesType_get_reservationId(rit, env);

    maxCount = adb_runInstancesType_get_maxCount(rit, env);
    minCount = adb_runInstancesType_get_minCount(rit, env);
    keyName = adb_runInstancesType_get_keyName(rit, env);

    emiId = adb_runInstancesType_get_imageId(rit, env);
    kernelId = adb_runInstancesType_get_kernelId(rit, env);
    ramdiskId = adb_runInstancesType_get_ramdiskId(rit, env);

    emiURL = adb_runInstancesType_get_imageURL(rit, env);
    kernelURL = adb_runInstancesType_get_kernelURL(rit, env);
    ramdiskURL = adb_runInstancesType_get_ramdiskURL(rit, env);

    tmp = adb_runInstancesType_get_userData(rit, env);
    if (!tmp) {
        userData = strdup("");
    } else {
        userData = strdup(tmp);
    }
    tmp = adb_runInstancesType_get_credential(rit, env);
    if (!tmp) {
        credential = strdup("");
    } else {
        credential = strdup(tmp);
    }

    launchIndex = adb_runInstancesType_get_launchIndex(rit, env);
    platform = adb_runInstancesType_get_platform(rit, env);

    dt = adb_runInstancesType_get_expiryTime(rit, env);
    expiryTime = datetime_to_unix(dt, env);

    vm = adb_runInstancesType_get_instanceType(rit, env);
    copy_vm_type_from_adb(&ccvm, vm, env);
    vmName = adb_virtualMachineType_get_name(vm, env);
    snprintf(ccvm.name, 64, "%s", vmName);

    vlan = adb_runInstancesType_get_vlan(rit, env);

    instIdsLen = adb_runInstancesType_sizeof_instanceIds(rit, env);
    instIds = EUCA_ZALLOC(instIdsLen, sizeof(char *));
    for (i = 0; i < instIdsLen; i++) {
        instIds[i] = adb_runInstancesType_get_instanceIds_at(rit, env, i);
    }

    privateIpsLen = adb_runInstancesType_sizeof_privateIps(rit, env);
    privateIps = EUCA_ZALLOC(privateIpsLen, sizeof(char *));
    for (i = 0; i < privateIpsLen; i++) {
        privateIps[i] = adb_runInstancesType_get_privateIps_at(rit, env, i);
    }
    // DAN TEMPORARY
    //    privateIpsLen = 1;
    //    privateIps = EUCA_ZALLOC(privateIpsLen, sizeof(char *));
    //    privateIps[0] = strdup("10.111.101.156");

    netNamesLen = adb_runInstancesType_sizeof_netNames(rit, env);
    netNames = EUCA_ZALLOC(netNamesLen, sizeof(char *));
    if (netNamesLen > 1) {
        netNamesLen = 1;
    }

    for (i = 0; i < netNamesLen; i++) {
        netNames[i] = adb_runInstancesType_get_netNames_at(rit, env, i);
    }

    netIdsLen = adb_runInstancesType_sizeof_netIds(rit, env);
    netIds = EUCA_ZALLOC(netIdsLen, sizeof(char *));
    if (netIdsLen > 1) {
        netIdsLen = 1;
    }

    for (i = 0; i < netIdsLen; i++) {
        netIds[i] = adb_runInstancesType_get_netIds_at(rit, env, i);
    }

    macAddrsLen = adb_runInstancesType_sizeof_macAddresses(rit, env);
    macAddrs = EUCA_ZALLOC(macAddrsLen, sizeof(char *));
    for (i = 0; i < macAddrsLen; i++) {
        macAddrs[i] = adb_runInstancesType_get_macAddresses_at(rit, env, i);
    }

    uuidsLen = adb_runInstancesType_sizeof_uuids(rit, env);
    uuids = EUCA_ZALLOC(uuidsLen, sizeof(char *));
    for (i = 0; i < uuidsLen; i++) {
        uuids[i] = adb_runInstancesType_get_uuids_at(rit, env, i);
    }

    networkIndexList = NULL;
    networkIndexListLen = adb_runInstancesType_sizeof_networkIndexList(rit, env);
    if (networkIndexListLen) {
        networkIndexList = EUCA_ZALLOC(networkIndexListLen, sizeof(int));
        for (i = 0; i < networkIndexListLen; i++) {
            networkIndexList[i] = adb_runInstancesType_get_networkIndexList_at(rit, env, i);
        }
    }

    eniAttachmentId = adb_runInstancesType_get_primaryEniAttachmentId(rit, env);

    secNetCfgsLen = adb_runInstancesType_sizeof_secondaryNetConfig(rit, env);
    if (secNetCfgsLen > EUCA_MAX_NICS) {// Warn that number of net configs is greater than supported
        LOGWARN("Maximum number of secondary enis supported is %d\n", EUCA_MAX_NICS);
        secNetCfgsLen = EUCA_MAX_NICS;
    }
    for (i = 0; i < secNetCfgsLen; i++) {
        bzero(&(secNetCfgs[i]), sizeof(netConfig));
        adb_netConfigType_t *net = adb_runInstancesType_get_secondaryNetConfig_at(rit, env, i);
        euca_strncpy(secNetCfgs[i].interfaceId, adb_netConfigType_get_interfaceId(net, env), ENI_ID_LEN);
        secNetCfgs[i].device = adb_netConfigType_get_device(net, env);
        euca_strncpy(secNetCfgs[i].privateMac, adb_netConfigType_get_privateMacAddress(net, env), ENET_ADDR_LEN);
        euca_strncpy(secNetCfgs[i].privateIp, adb_netConfigType_get_privateIp(net, env), INET_ADDR_LEN);
        euca_strncpy(secNetCfgs[i].publicIp, adb_netConfigType_get_publicIp(net, env), INET_ADDR_LEN);
        secNetCfgs[i].vlan = adb_netConfigType_get_vlan(net, env);
        secNetCfgs[i].networkIndex = adb_netConfigType_get_networkIndex(net, env);
        euca_strncpy(secNetCfgs[i].attachmentId, adb_netConfigType_get_attachmentId(net, env), ENI_ATTACHMENT_ID_LEN);
    }

    accountId = adb_runInstancesType_get_accountId(rit, env);
    if (!accountId) {
        accountId = ccMeta.userId;
    }
    ownerId = adb_runInstancesType_get_ownerId(rit, env);
    if (!ownerId) {
        ownerId = accountId;
    }

    tmp = adb_runInstancesType_get_rootDirective(rit, env);
    if (!tmp) {
        rootDirective = strdup("");
    } else {
        rootDirective = strdup(tmp);
    }

    rirt = adb_runInstancesResponseType_create(env);
    rc = 1;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doRunInstances(&ccMeta, emiId, kernelId, ramdiskId, emiURL, kernelURL, ramdiskURL, instIds, instIdsLen, netNames, netNamesLen, netIds, netIdsLen, macAddrs,
                            macAddrsLen, networkIndexList, networkIndexListLen, uuids, uuidsLen, privateIps, privateIpsLen, minCount, maxCount, accountId, ownerId,
                            reservationId, &ccvm, keyName, vlan, userData, credential, launchIndex, platform, expiryTime, NULL, rootDirective, eniAttachmentId,
                            secNetCfgs, secNetCfgsLen, &outInsts, &outInstsLen);
        unset_corrid(corr_id);
    }

    if (rc) {
        LOGERROR("doRunInstances() failed: %d (%s, %d, %s, %s, %s)\n", rc, emiId, instIdsLen, instIds[0], accountId, ownerId);
        status = AXIS2_FALSE;
        snprintf(statusMessage, 255, "ERROR");
    } else {
        for (i = 0; i < outInstsLen; i++) {
            myInstance = &(outInsts[i]);

            it = adb_ccInstanceType_create(env);

            myInstance->ccvm.virtualBootRecordLen = 0;
            rc = ccInstanceUnmarshal(it, myInstance, env);
            adb_runInstancesResponseType_add_instances(rirt, env, it);
        }
    }

    EUCA_FREE(outInsts);

    adb_runInstancesResponseType_set_correlationId(rirt, env, ccMeta.correlationId);
    adb_runInstancesResponseType_set_userId(rirt, env, ccMeta.userId);
    adb_runInstancesResponseType_set_return(rirt, env, status);
    if (status == AXIS2_FALSE) {
        adb_runInstancesResponseType_set_statusMessage(rirt, env, statusMessage);
    }

    ret = adb_RunInstancesResponse_create(env);
    adb_RunInstancesResponse_set_RunInstancesResponse(ret, env, rirt);
    EUCA_FREE(netIds);
    EUCA_FREE(credential);
    EUCA_FREE(rootDirective);
    EUCA_FREE(networkIndexList);
    EUCA_FREE(macAddrs);
    EUCA_FREE(netNames);
    EUCA_FREE(privateIps);
    EUCA_FREE(instIds);
    EUCA_FREE(userData);
    EUCA_FREE(uuids);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("RunInstances", (long)call_time, rc);

    return (ret);
}

//!
//! Process the reboot instances request and provides the response
//!
//! @param[in] rebootInstances a pointer to the reboot instance message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_RebootInstancesResponse_t *RebootInstancesMarshal(adb_RebootInstances_t * rebootInstances, const axutil_env_t * env)
{
    adb_RebootInstancesResponse_t *ret = NULL;
    adb_rebootInstancesResponseType_t *rirt = NULL;
    adb_rebootInstancesType_t *rit = NULL;
    char **instIds = NULL;
    int instIdsLen = 0;
    int i = 0;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    rit = adb_RebootInstances_get_RebootInstances(rebootInstances, env);
    EUCA_MESSAGE_UNMARSHAL(rebootInstancesType, rit, (&ccMeta));

    instIdsLen = adb_rebootInstancesType_sizeof_instanceIds(rit, env);
    instIds = EUCA_ZALLOC(instIdsLen, sizeof(char *));
    for (i = 0; i < instIdsLen; i++) {
        instIds[i] = adb_rebootInstancesType_get_instanceIds_at(rit, env, i);
    }

    rc = 1;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doRebootInstances(&ccMeta, instIds, instIdsLen);
        unset_corrid(corr_id);
    }

    EUCA_FREE(instIds);

    rirt = adb_rebootInstancesResponseType_create(env);
    if (rc) {
        LOGERROR("doRebootInstances() failed: %d (%d, %s)\n", rc, instIdsLen, instIds[0]);
        status = AXIS2_FALSE;
        snprintf(statusMessage, 255, "ERROR");
    } else {
        status = AXIS2_TRUE;
    }

    adb_rebootInstancesResponseType_set_correlationId(rirt, env, ccMeta.correlationId);
    adb_rebootInstancesResponseType_set_userId(rirt, env, ccMeta.userId);
    //  adb_rebootInstancesResponseType_set_statusMessage(rirt, env, status);
    adb_rebootInstancesResponseType_set_return(rirt, env, status);
    if (status == AXIS2_FALSE) {
        adb_rebootInstancesResponseType_set_statusMessage(rirt, env, statusMessage);
    }

    ret = adb_RebootInstancesResponse_create(env);
    adb_RebootInstancesResponse_set_RebootInstancesResponse(ret, env, rirt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("RebootInstances", (long)call_time, rc);

    return (ret);
}

//!
//! Process the terminate isntances request and provides the response
//!
//! @param[in] terminateInstances a pointer to the terminate instance message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_TerminateInstancesResponse_t *TerminateInstancesMarshal(adb_TerminateInstances_t * terminateInstances, const axutil_env_t * env)
{
    adb_TerminateInstancesResponse_t *ret = NULL;
    adb_terminateInstancesResponseType_t *tirt = NULL;
    adb_terminateInstancesType_t *tit = NULL;
    char **instIds = NULL;
    int instIdsLen = 0;
    int i = 0;
    int rc = 0;
    int *outStatus = NULL;
    int force = 0;
    axis2_bool_t status = AXIS2_TRUE;
    axis2_bool_t forceBool = AXIS2_FALSE;
    char statusMessage[256] = { 0 };
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    tit = adb_TerminateInstances_get_TerminateInstances(terminateInstances, env);
    EUCA_MESSAGE_UNMARSHAL(terminateInstancesType, tit, (&ccMeta));

    instIdsLen = adb_terminateInstancesType_sizeof_instanceIds(tit, env);
    instIds = EUCA_ZALLOC(instIdsLen, sizeof(char *));
    for (i = 0; i < instIdsLen; i++) {
        instIds[i] = adb_terminateInstancesType_get_instanceIds_at(tit, env, i);
    }

    forceBool = adb_terminateInstancesType_get_force(tit, env);
    if (forceBool == AXIS2_TRUE) {
        force = 1;
    } else {
        force = 0;
    }

    rc = 1;
    if (!DONOTHING) {
        outStatus = EUCA_ZALLOC(instIdsLen, sizeof(int));
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doTerminateInstances(&ccMeta, instIds, instIdsLen, force, &outStatus);
        unset_corrid(corr_id);
    }

    EUCA_FREE(instIds);

    tirt = adb_terminateInstancesResponseType_create(env);
    if (rc) {
        LOGERROR("doTerminateInstances() failed: %d (%d, %s)\n", rc, instIdsLen, instIds[0]);
        status = AXIS2_FALSE;
        snprintf(statusMessage, 255, "ERROR");
    } else {
        for (i = 0; i < instIdsLen; i++) {
            if (outStatus[i]) {
                adb_terminateInstancesResponseType_add_isTerminated(tirt, env, AXIS2_TRUE);
            } else {
                adb_terminateInstancesResponseType_add_isTerminated(tirt, env, AXIS2_FALSE);
            }
        }
    }
    EUCA_FREE(outStatus);

    adb_terminateInstancesResponseType_set_correlationId(tirt, env, ccMeta.correlationId);
    adb_terminateInstancesResponseType_set_userId(tirt, env, ccMeta.userId);
    adb_terminateInstancesResponseType_set_return(tirt, env, status);
    if (status == AXIS2_FALSE) {
        adb_terminateInstancesResponseType_set_statusMessage(tirt, env, statusMessage);
    }

    ret = adb_TerminateInstancesResponse_create(env);
    adb_TerminateInstancesResponse_set_TerminateInstancesResponse(ret, env, tirt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("TerminateInstances", (long)call_time, rc);

    return (ret);
}

//!
//! Process the create image request and provides the response
//!
//! @param[in] createImage a pointer to the create image message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_CreateImageResponse_t *CreateImageMarshal(adb_CreateImage_t * createImage, const axutil_env_t * env)
{
    int rc = 0;
    adb_CreateImageResponse_t *ret = NULL;
    adb_createImageResponseType_t *cirt = NULL;
    adb_createImageType_t *cit = NULL;
    char *instanceId = NULL;
    char *volumeId = NULL;
    char *remoteDev = NULL;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    cit = adb_CreateImage_get_CreateImage(createImage, env);

    EUCA_MESSAGE_UNMARSHAL(createImageType, cit, (&ccMeta));

    instanceId = adb_createImageType_get_instanceId(cit, env);
    volumeId = adb_createImageType_get_volumeId(cit, env);
    remoteDev = adb_createImageType_get_remoteDev(cit, env);

    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doCreateImage(&ccMeta, instanceId, volumeId, remoteDev);
        unset_corrid(corr_id);
    }

    cirt = adb_createImageResponseType_create(env);
    if (rc) {
        LOGERROR("doCreateImage() failed: %d (%s, %s)\n", rc, instanceId, volumeId);
        status = AXIS2_FALSE;
        snprintf(statusMessage, 255, "ERROR");
    } else {
        status = AXIS2_TRUE;
    }

    adb_createImageResponseType_set_correlationId(cirt, env, ccMeta.correlationId);
    adb_createImageResponseType_set_userId(cirt, env, ccMeta.userId);

    adb_createImageResponseType_set_return(cirt, env, status);
    if (status == AXIS2_FALSE) {
        adb_createImageResponseType_set_statusMessage(cirt, env, statusMessage);
    }

    ret = adb_CreateImageResponse_create(env);
    adb_CreateImageResponse_set_CreateImageResponse(ret, env, cirt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("CreateImage", (long)call_time, rc);

    return (ret);
}

//!
//!
//!
//! @param[in] in a pointer to the AXIS2 instance type structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
void print_adb_ccInstanceType(adb_ccInstanceType_t * in)
{

}

//!
//! Unmarshalls request to modify a node controller, executes, responds.
//!
//! @param[in] modifyNode a pointer to the request message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_ModifyNodeResponse_t *ModifyNodeMarshal(adb_ModifyNode_t * modifyNode, const axutil_env_t * env)
{
    adb_ModifyNodeResponse_t *ret = NULL;
    adb_modifyNodeResponseType_t *mnrt = NULL;
    adb_modifyNodeType_t *mnt = NULL;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *nodeName = NULL;
    char *stateName = NULL;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    mnt = adb_ModifyNode_get_ModifyNode(modifyNode, env);

    EUCA_MESSAGE_UNMARSHAL(modifyNodeType, mnt, (&ccMeta));

    stateName = adb_modifyNodeType_get_stateName(mnt, env);
    nodeName = adb_modifyNodeType_get_nodeName(mnt, env);

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doModifyNode(&ccMeta, nodeName, stateName);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doModifyNode() failed: %d (%s, %s)\n", rc, nodeName, stateName);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }

    mnrt = adb_modifyNodeResponseType_create(env);
    adb_modifyNodeResponseType_set_return(mnrt, env, status);
    if (status == AXIS2_FALSE) {
        adb_modifyNodeResponseType_set_statusMessage(mnrt, env, statusMessage);
    }

    adb_modifyNodeResponseType_set_correlationId(mnrt, env, ccMeta.correlationId);
    adb_modifyNodeResponseType_set_userId(mnrt, env, ccMeta.userId);

    ret = adb_ModifyNodeResponse_create(env);
    adb_ModifyNodeResponse_set_ModifyNodeResponse(ret, env, mnrt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("ModifyNode", (long)call_time, rc);

    return (ret);
}

//!
//! Unmarshalls request to modify a node controller, executes, responds.
//!
//! @param[in] migrateInstances a pointer to the request message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_MigrateInstancesResponse_t *MigrateInstancesMarshal(adb_MigrateInstances_t * migrateInstances, const axutil_env_t * env)
{
    adb_MigrateInstancesResponse_t *ret = NULL;
    adb_migrateInstancesResponseType_t *mirt = NULL;
    adb_migrateInstancesType_t *mit = NULL;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *sourceNode = NULL;
    char *instanceId = NULL;
    char **destinationNodes = NULL;
    int destinationNodeCount = 0;
    int allowHosts;
    char **resourceLocations = NULL;
    int resourceLocationCount = 0;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    bzero(&ccMeta, sizeof(ncMetadata));

    mit = adb_MigrateInstances_get_MigrateInstances(migrateInstances, env);

    EUCA_MESSAGE_UNMARSHAL(migrateInstancesType, mit, (&ccMeta));

    sourceNode = adb_migrateInstancesType_get_sourceHost(mit, env);
    instanceId = adb_migrateInstancesType_get_instanceId(mit, env);
    allowHosts = adb_migrateInstancesType_get_allowHosts(mit, env);
    destinationNodeCount = adb_migrateInstancesType_sizeof_destinationHost(mit, env);

    destinationNodes = EUCA_ZALLOC(destinationNodeCount, sizeof(char *));
    for (int i = 0; i < destinationNodeCount; i++) {
        destinationNodes[i] = adb_migrateInstancesType_get_destinationHost_at(mit, env, i);
    }

    resourceLocationCount = adb_migrateInstancesType_sizeof_resourceLocation(mit, env);
    resourceLocations = EUCA_ZALLOC(resourceLocationCount, sizeof(char *));
    for (int i = 0; i < resourceLocationCount; i++) {
        resourceLocations[i] = adb_migrateInstancesType_get_resourceLocation_at(mit, env, i);
    }

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doMigrateInstances(&ccMeta, sourceNode, instanceId, destinationNodes, destinationNodeCount, allowHosts, "prepare", resourceLocations, resourceLocationCount);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doMigrateInstances() failed: %d (%s, %s, %d)\n", rc, sourceNode, instanceId, destinationNodeCount);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }
    if (ccMeta.replyString != NULL) {  // if replyString is set, we have a more detailed status/error message
        snprintf(statusMessage, sizeof(statusMessage), "%s", ccMeta.replyString);
        EUCA_FREE(ccMeta.replyString); // the caller must free
    }

    mirt = adb_migrateInstancesResponseType_create(env);
    adb_migrateInstancesResponseType_set_return(mirt, env, status);
    if (strlen(statusMessage) > 0) {
        adb_migrateInstancesResponseType_set_statusMessage(mirt, env, statusMessage);
    }

    adb_migrateInstancesResponseType_set_correlationId(mirt, env, ccMeta.correlationId);
    adb_migrateInstancesResponseType_set_userId(mirt, env, ccMeta.userId);

    ret = adb_MigrateInstancesResponse_create(env);
    adb_MigrateInstancesResponse_set_MigrateInstancesResponse(ret, env, mirt);

    EUCA_FREE(destinationNodes);
    EUCA_FREE(resourceLocations);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("MigrateInstances", (long)call_time, rc);

    return (ret);
}

//!
//! Unmarshalls request to start an instance, executes, responds.
//!
//! @param[in] startInstance a pointer to the request message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_StartInstanceResponse_t *StartInstanceMarshal(adb_StartInstance_t * startInstance, const axutil_env_t * env)
{
    adb_StartInstanceResponse_t *ret = NULL;
    adb_startInstanceResponseType_t *mirt = NULL;
    adb_startInstanceType_t *mit = NULL;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *instanceId = NULL;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    bzero(&ccMeta, sizeof(ncMetadata));

    mit = adb_StartInstance_get_StartInstance(startInstance, env);

    EUCA_MESSAGE_UNMARSHAL(startInstanceType, mit, (&ccMeta));

    instanceId = adb_startInstanceType_get_instanceId(mit, env);

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doStartInstance(&ccMeta, instanceId);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doStartInstance() failed: %d (%s)\n", rc, instanceId);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }

    if (ccMeta.replyString != NULL) {  // if replyString is set, we have a more detailed status/error message
        snprintf(statusMessage, sizeof(statusMessage), "%s", ccMeta.replyString);
        EUCA_FREE(ccMeta.replyString); // the caller must free
    }

    mirt = adb_startInstanceResponseType_create(env);
    adb_startInstanceResponseType_set_return(mirt, env, status);
    if (strlen(statusMessage) > 0) {
        adb_startInstanceResponseType_set_statusMessage(mirt, env, statusMessage);
    }

    adb_startInstanceResponseType_set_correlationId(mirt, env, ccMeta.correlationId);
    adb_startInstanceResponseType_set_userId(mirt, env, ccMeta.userId);

    ret = adb_StartInstanceResponse_create(env);
    adb_StartInstanceResponse_set_StartInstanceResponse(ret, env, mirt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("StartInstance", (long)call_time, rc);

    return (ret);
}

//!
//! Unmarshalls request to stop an instance, executes, responds.
//!
//! @param[in] stopInstance a pointer to the request message structure
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
adb_StopInstanceResponse_t *StopInstanceMarshal(adb_StopInstance_t * stopInstance, const axutil_env_t * env)
{
    adb_StopInstanceResponse_t *ret = NULL;
    adb_stopInstanceResponseType_t *mirt = NULL;
    adb_stopInstanceType_t *mit = NULL;
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256] = { 0 };
    char *instanceId = NULL;
    ncMetadata ccMeta = { 0 };
    long long call_time = time_ms();

    bzero(&ccMeta, sizeof(ncMetadata));

    mit = adb_StopInstance_get_StopInstance(stopInstance, env);

    EUCA_MESSAGE_UNMARSHAL(stopInstanceType, mit, (&ccMeta));

    instanceId = adb_stopInstanceType_get_instanceId(mit, env);

    status = AXIS2_TRUE;
    if (!DONOTHING) {
        threadCorrelationId *corr_id = set_corrid(ccMeta.correlationId);
        rc = doStopInstance(&ccMeta, instanceId);
        unset_corrid(corr_id);
        if (rc) {
            LOGERROR("doStopInstance() failed: %d (%s)\n", rc, instanceId);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
        }
    }
    if (ccMeta.replyString != NULL) {  // if replyString is set, we have a more detailed status/error message
        snprintf(statusMessage, sizeof(statusMessage), "%s", ccMeta.replyString);
        EUCA_FREE(ccMeta.replyString); // the caller must free
    }

    mirt = adb_stopInstanceResponseType_create(env);
    adb_stopInstanceResponseType_set_return(mirt, env, status);
    if (strlen(statusMessage) > 0) {
        adb_stopInstanceResponseType_set_statusMessage(mirt, env, statusMessage);
    }

    adb_stopInstanceResponseType_set_correlationId(mirt, env, ccMeta.correlationId);
    adb_stopInstanceResponseType_set_userId(mirt, env, ccMeta.userId);

    ret = adb_StopInstanceResponse_create(env);
    adb_StopInstanceResponse_set_StopInstanceResponse(ret, env, mirt);

    //update stats and return
    call_time = time_ms() - call_time;
    cached_message_stats_update("StopInstance", (long)call_time, rc);

    return (ret);
}

//!
//! Unmarshals, executes, responds to the attach network interface request.
//!
//! @param[in] AttachNetworkInterface a pointer to the attach betwork interface request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_AttachNetworkInterfaceResponse_t *AttachNetworkInterfaceMarshal(adb_AttachNetworkInterface_t * AttachNetworkInterface, const axutil_env_t * env)
{
    int error = EUCA_OK;
    ncMetadata meta = { 0 };
    netConfig netConfig = { 0 };
    axis2_char_t *instanceId = NULL;
    adb_netConfigType_t *netConfigType = NULL;
    adb_AttachNetworkInterfaceType_t *input = NULL;
    adb_AttachNetworkInterfaceResponse_t *response = NULL;
    adb_AttachNetworkInterfaceResponseType_t *output = NULL;
    long long call_time = time_ms();
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256];
    status = AXIS2_TRUE;

    input = adb_AttachNetworkInterface_get_AttachNetworkInterface(AttachNetworkInterface, env);
    response = adb_AttachNetworkInterfaceResponse_create(env);
    output = adb_AttachNetworkInterfaceResponseType_create(env);

    // get operation-specific fields from input
    instanceId = adb_AttachNetworkInterfaceType_get_instanceId(input, env);
    netConfigType = adb_AttachNetworkInterfaceType_get_netConfig(input, env);
    netConfig.vlan = adb_netConfigType_get_vlan(netConfigType, env);
    netConfig.networkIndex = adb_netConfigType_get_networkIndex(netConfigType, env);
    snprintf(netConfig.privateMac, ENET_ADDR_LEN, "%s", adb_netConfigType_get_privateMacAddress(netConfigType, env));
    snprintf(netConfig.privateIp, INET_ADDR_LEN, "%s", adb_netConfigType_get_privateIp(netConfigType, env));
    snprintf(netConfig.publicIp, INET_ADDR_LEN, "%s", adb_netConfigType_get_publicIp(netConfigType, env));
    snprintf(netConfig.interfaceId, ENI_ID_LEN, "%s", adb_netConfigType_get_interfaceId(netConfigType, env));
    snprintf(netConfig.attachmentId, ENI_ATTACHMENT_ID_LEN, "%s", adb_netConfigType_get_attachmentId(netConfigType, env));
    netConfig.device = adb_netConfigType_get_device(netConfigType, env);

    if(!DONOTHING) {
        // do it
        EUCA_MESSAGE_UNMARSHAL(AttachNetworkInterfaceType, input, (&meta));

        threadCorrelationId *corr_id = set_corrid(meta.correlationId);
        if ((error = doAttachNetworkInterface(&meta, instanceId, &netConfig)) != EUCA_OK) {
            LOGERROR("[%s][%s] failed error=%d\n", instanceId, netConfig.interfaceId, error);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
            adb_AttachNetworkInterfaceResponseType_set_return(output, env, AXIS2_FALSE);
            adb_AttachNetworkInterfaceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_AttachNetworkInterfaceResponseType_set_userId(output, env, meta.userId);
            adb_AttachNetworkInterfaceResponseType_set_statusMessage(output, env, statusMessage);
        } else {
            // set standard fields in output
            adb_AttachNetworkInterfaceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_AttachNetworkInterfaceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_AttachNetworkInterfaceResponseType_set_userId(output, env, meta.userId);
            // no operation-specific fields in output
        }
        unset_corrid(corr_id);
    }

    // set response to output
    adb_AttachNetworkInterfaceResponse_set_AttachNetworkInterfaceResponse(response, env, output);

    cached_message_stats_update("AttachNetworkInterface", (long)(time_ms() - call_time), error);
    return (response);
}

//!
//! Unmarshals, executes, responds to the detach network interface request.
//!
//! @param[in] ncNetworkInterfaceVolume a pointer to the detach network interface request parameters
//! @param[in] env pointer to the AXIS2 environment structure
//!
//! @return a pointer to the request's response structure
//!
adb_DetachNetworkInterfaceResponse_t *DetachNetworkInterfaceMarshal(adb_DetachNetworkInterface_t * DetachNetworkInterface, const axutil_env_t * env)
{
    int error = EUCA_OK;
    boolean force = FALSE;
    ncMetadata meta = { 0 };
    axis2_char_t *instanceId = NULL;
    axis2_char_t *interfaceId = NULL;
    axis2_bool_t forceBool = AXIS2_FALSE;
    adb_DetachNetworkInterfaceType_t *input = NULL;
    adb_DetachNetworkInterfaceResponse_t *response = NULL;
    adb_DetachNetworkInterfaceResponseType_t *output = NULL;
    long long call_time = time_ms();
    axis2_bool_t status = AXIS2_TRUE;
    char statusMessage[256];
    status = AXIS2_TRUE;

    input = adb_DetachNetworkInterface_get_DetachNetworkInterface(DetachNetworkInterface, env);
    response = adb_DetachNetworkInterfaceResponse_create(env);
    output = adb_DetachNetworkInterfaceResponseType_create(env);

    // get operation-specific fields from input
    instanceId = adb_DetachNetworkInterfaceType_get_instanceId(input, env);
    interfaceId = adb_DetachNetworkInterfaceType_get_attachmentId(input, env);
    forceBool = adb_DetachNetworkInterfaceType_get_force(input, env);
    if (forceBool == AXIS2_TRUE) {
        force = TRUE;
    } else {
        force = FALSE;
    }

    if(!DONOTHING) {
        // do it
        EUCA_MESSAGE_UNMARSHAL(DetachNetworkInterfaceType, input, (&meta));

        threadCorrelationId *corr_id = set_corrid(meta.correlationId);
        if ((error = doDetachNetworkInterface(&meta, instanceId, interfaceId, force)) != EUCA_OK) {
            LOGERROR("[%s][%s] failed error=%d\n", instanceId, interfaceId, error);
            status = AXIS2_FALSE;
            snprintf(statusMessage, 255, "ERROR");
            adb_DetachNetworkInterfaceResponseType_set_return(output, env, AXIS2_FALSE);
            adb_DetachNetworkInterfaceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_DetachNetworkInterfaceResponseType_set_userId(output, env, meta.userId);
            adb_DetachNetworkInterfaceResponseType_set_statusMessage(output, env, statusMessage);
        } else {
            // set standard fields in output
            adb_DetachNetworkInterfaceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_DetachNetworkInterfaceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_DetachNetworkInterfaceResponseType_set_userId(output, env, meta.userId);
            // no operation-specific fields in output
        }
        unset_corrid(corr_id);
    }

    // set response to output
    adb_DetachNetworkInterfaceResponse_set_DetachNetworkInterfaceResponse(response, env, output);

    cached_message_stats_update("DetachNetworkInterface", (long)(time_ms() - call_time), error);
    return (response);
}
