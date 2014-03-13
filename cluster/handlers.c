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
//! @file cluster/handlers.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <semaphore.h>
#include <netdb.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <signal.h>
#include <math.h>

#include <eucalyptus.h>
#include "axis2_skel_EucalyptusCC.h"

#include <server-marshal.h>
#include <handlers.h>
#include <vnetwork.h>
#include <misc.h>
#include <ipc.h>
#include <objectstorage.h>
#include <http.h>
#include <globalnetwork.h>

#include <euca_axis.h>
#include "data.h"
#include "handlers.h"
#include "client-marshal.h"
#include "config-cc.h"

#include <storage-windows.h>
#include <euca_auth.h>

#include <handlers-state.h>
#include <fault.h>
#include <euca_string.h>
#include <axutil_error.h>

#include <ebs_utils.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define SUPERUSER                                "eucalyptus"
#define MAX_SENSOR_RESOURCES                     MAXINSTANCES_PER_CC
#define POLL_INTERVAL_SAFETY_MARGIN_SEC          3
#define POLL_INTERVAL_MINIMUM_SEC                6

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

//! @{
// @name local globals
int config_init = 0;
int local_init = 0;
int thread_init = 0;
int sensor_initd = 0;
int init = 0;
//! @}

//! @{
//! @name shared (between CC processes) globals
ccConfig *config = NULL;
ccInstanceCache *instanceCache = NULL;
vnetConfig *vnetconfig = NULL;
globalNetworkInfo *globalnetworkinfo = NULL;
ccResourceCache *resourceCache = NULL;
ccResourceCache *resourceCacheStage = NULL;
sensorResourceCache *ccSensorResourceCache = NULL;
//! @}

//! @{
//! @name shared (between CC processes) semaphores
sem_t *locks[ENDLOCK];
int mylocks[ENDLOCK];
//! @}

#ifndef NO_COMP
const char *euca_this_component_name = "cc";
const char *euca_client_component_name = "clc";
#endif /* ! NO_COMP */

char *SCHEDPOLICIES[SCHEDLAST] = {
    "GREEDY",
    "ROUNDROBIN",
    "POWERSAVE",
    "USER",
};

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

static int schedule_instance_migration(ncInstance * instance, char **includeNodes, char **excludeNodes, int includeNodeCount, int excludeNodeCount, int inresid, int *outresid,
                                       ccResourceCache * resourceCacheLocal, char **replyString);
static int migration_handler(ccInstance * myInstance, char *host, char *src, char *dst, migration_states migration_state, char **node, char **instance, char **action);
static int populateOutboundMeta(ncMetadata * pMeta);

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
//!
//!
//! @note
//!
void doInitCC(void)
{
    initialize(NULL, FALSE);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId
//! @param[in] bucketName
//! @param[in] filePrefix
//! @param[in] objectStorageURL
//! @param[in] userPublicKey
//! @param[in] S3Policy
//! @param[in] S3PolicySig
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doBundleInstance(ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *objectStorageURL, char *userPublicKey, char *S3Policy, char *S3PolicySig)
{
    int i, j, rc, start = 0, stop = 0, ret = 0, timeout, done;
    char internalObjectStorageURL[EUCA_MAX_PATH], theObjectStorageURL[EUCA_MAX_PATH];
    ccInstance *myInstance;
    time_t op_start;
    ccResourceCache resourceCacheLocal;

    i = j = 0;
    myInstance = NULL;
    op_start = time(NULL);

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGINFO("[%s] bundling requested\n", instanceId);
    LOGDEBUG("invoked: userId=%s, instanceId=%s, bucketName=%s, filePrefix=%s, objectStorageURL=%s, userPublicKey=%s, S3Policy=%s, S3PolicySig=%s\n",
             SP(pMeta ? pMeta->userId : "UNSET"), SP(instanceId), SP(bucketName), SP(filePrefix), SP(objectStorageURL), SP(userPublicKey), SP(S3Policy), SP(S3PolicySig));
    if (!instanceId) {
        LOGERROR("bad input params\n");
        return (1);
    }
    // get internal object storage IP
    done = 0;
    internalObjectStorageURL[0] = '\0';
    for (i = 0; i < 16 && !done; i++) {
        if (!strcmp(config->services[i].type, "objectstorage")) {
            snprintf(internalObjectStorageURL, EUCA_MAX_PATH, "%s", config->services[i].uris[0]);
            done++;
        }
    }
    if (done) {
        snprintf(theObjectStorageURL, EUCA_MAX_PATH, "%s", internalObjectStorageURL);
    } else {
        strncpy(theObjectStorageURL, objectStorageURL, strlen(objectStorageURL) + 1);
    }

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    rc = find_instanceCacheId(instanceId, &myInstance);
    if (!rc) {
        // found the instance in the cache
        if (myInstance) {
            start = myInstance->ncHostIdx;
            stop = start + 1;
            EUCA_FREE(myInstance);
        }
    } else {
        start = 0;
        stop = resourceCacheLocal.numResources;
    }

    done = 0;
    for (j = start; j < stop && !done; j++) {
        timeout = ncGetTimeout(op_start, OP_TIMEOUT, stop - start, j);
        rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncBundleInstance",
                          instanceId, bucketName, filePrefix, theObjectStorageURL, userPublicKey, S3Policy, S3PolicySig);
        if (rc) {
            ret = 1;
        } else {
            ret = 0;
            done++;
        }
    }

    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doBundleRestartInstance(ncMetadata * pMeta, char *instanceId)
{
    int j = 0;
    int rc = 0;
    int start = 0;
    int stop = 0;
    int ret = 0;
    int timeout = 0;
    int done = 0;
    ccInstance *myInstance = NULL;
    time_t op_start = time(NULL);
    ccResourceCache resourceCacheLocal;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled())
        return (1);

    LOGINFO("[%s] bundling instance restart\n", SP(instanceId));
    LOGDEBUG("invoked: instanceId=%s userId=%s\n", SP(instanceId), SP(pMeta ? pMeta->userId : "UNSET"));
    if (instanceId == NULL) {
        LOGERROR("bad input params\n");
        return (1);
    }

    sem_mywait(RESCACHE);
    {
        memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    }
    sem_mypost(RESCACHE);

    if ((rc = find_instanceCacheId(instanceId, &myInstance)) == 0) {
        // found the instance in the cache
        if (myInstance) {
            start = myInstance->ncHostIdx;
            stop = start + 1;
            EUCA_FREE(myInstance);
            myInstance = NULL;
        }
    } else {
        start = 0;
        stop = resourceCacheLocal.numResources;
    }

    done = 0;
    for (j = start; ((j < stop) && !done); j++) {
        timeout = ncGetTimeout(op_start, OP_TIMEOUT, (stop - start), j);
        rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncBundleRestartInstance", instanceId);
        if (rc) {
            ret = 1;
        } else {
            ret = 0;
            done++;
        }
    }

    LOGTRACE("done\n");
    shawn();
    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doCancelBundleTask(ncMetadata * pMeta, char *instanceId)
{
    int i, rc, start = 0, stop = 0, ret = 0, done, timeout;
    ccInstance *myInstance;
    time_t op_start;
    ccResourceCache resourceCacheLocal;

    i = 0;
    myInstance = NULL;
    op_start = time(NULL);

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }
    LOGINFO("[%s] bundle task cancelled\n", SP(instanceId));
    LOGDEBUG("invoked: instanceId=%s userId=%s\n", SP(instanceId), SP(pMeta ? pMeta->userId : "UNSET"));
    if (!instanceId) {
        LOGERROR("bad input params\n");
        return (1);
    }

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    rc = find_instanceCacheId(instanceId, &myInstance);
    if (!rc) {
        // found the instance in the cache
        if (myInstance) {
            start = myInstance->ncHostIdx;
            stop = start + 1;
            EUCA_FREE(myInstance);
        }
    } else {
        start = 0;
        stop = resourceCacheLocal.numResources;
    }

    done = 0;
    for (i = start; i < stop && !done; i++) {
        timeout = ncGetTimeout(op_start, OP_TIMEOUT, stop - start, i);
        rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[i].lockidx, resourceCacheLocal.resources[i].ncURL, "ncCancelBundleTask", instanceId);
        if (rc) {
            ret = 1;
        } else {
            ret = 0;
            done++;
        }
    }

    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//! Remove cluster and storage services from other partitions so NCs only get globals and cluster-local services
//! Modifies the meta in-place
void filter_services(ncMetadata * meta, char *filter_partition)
{
    int i = 0, j = 0;
    serviceInfoType tmp;
    int copySize = sizeof(serviceInfoType);
    for (i = 0; i < meta->servicesLen; i++) {
        //Only filter cluster controllers and storage controllers.
        if ((!strcmp(meta->services[i].type, "cluster") || !strcmp(meta->services[i].type, "storage")) && strcmp(meta->services[i].partition, filter_partition)) {
            //Not equal, remove by making string len 0.
            LOGTRACE("Filtering out service: %s , %s\n", meta->services[i].name, meta->services[i].partition);

            //Null the strings.
            meta->services[i].name[0] = '\0';
            meta->services[i].partition[0] = '\0';
            meta->services[i].type[0] = '\0';
            for (j = 0; j < meta->services[i].urisLen; j++) {
                meta->services[i].uris[j][0] = '\0';
            }
            meta->services[i].urisLen = 0;

            //Swap this one and the one at the end and decrement the length.
            memcpy(&tmp, &(meta->services[i]), copySize);
            memcpy(&(meta->services[i]), &(meta->services[meta->servicesLen - 1]), copySize);
            memcpy(&(meta->services[meta->servicesLen - 1]), &(tmp), copySize);
            meta->servicesLen--;
        }
    }
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] timeout
//! @param[in] ncLock
//! @param[in] ncURL
//! @param[in] ncOp
//! @param[in] ...
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int ncClientCall(ncMetadata * pMeta, int timeout, int ncLock, char *ncURL, char *ncOp, ...)
{
#define WRITE_REPLY_STRING                                                                \
{                                                                                         \
    if (timeout) {                                                                        \
        int __len = 0;                                                                    \
        if (localmeta->replyString) {                                                     \
            __len = strlen(localmeta->replyString);                                       \
        }                                                                                 \
        int __bytes = write(filedes[1], &__len, sizeof(int));                             \
        if (__len > 0) {                                                                  \
            __bytes += write(filedes[1], localmeta->replyString, (sizeof(char) * __len)); \
        }                                                                                 \
        LOGTRACE("child process wrote %d bytes (len=%d)\n", __bytes, __len);              \
    }                                                                                     \
}

#define READ_REPLY_STRING                                                      \
{                                                                              \
    if (timeout) {                                                             \
        int __len = 0;                                                         \
        rbytes = timeread(filedes[0], &__len, sizeof(int), timeout);           \
        LOGTRACE("parent process read %d bytes (len=%d)\n", rbytes, __len);    \
        if (rbytes <= 0) {                                                     \
            killwait(pid);                                                     \
            opFail = 1;                                                        \
        } else if (__len > 0) {                                                \
            pMeta->replyString = EUCA_ALLOC(__len, sizeof(char));              \
            if (pMeta->replyString == NULL) {                                  \
                LOGFATAL("out of memory! ncOps=%s\n", ncOp);                   \
                unlock_exit(1);                                                \
            }                                                                  \
            rbytes = timeread(filedes[0], pMeta->replyString, __len, timeout); \
            if (rbytes <= 0) {                                                 \
                killwait(pid);                                                 \
                opFail = 1;                                                    \
            }                                                                  \
        }                                                                      \
    }                                                                          \
}

    int i = 0;
    int pid = 0;
    int rc = 0;
    int ret = 0;
    int status = 0;
    int opFail = 0;
    int len = 0;
    int rbytes = 0;
    int filedes[2] = { 0 };
    va_list al = { {0} };

    LOGTRACE("invoked: ncOps=%s ncURL=%s timeout=%d\n", ncOp, ncURL, timeout);  // these are common

    if ((rc = pipe(filedes)) != 0) {
        LOGERROR("cannot create pipe ncOps=%s\n", ncOp);
        return (1);
    }

    va_start(al, ncOp);

    // grab the lock
    sem_mywait(ncLock);

    if ((pid = fork()) == 0) {
        ncStub *ncs;
        ncMetadata *localmeta = NULL;

        LOGTRACE("forked to service NC invocation: %s\n", ncOp);
        localmeta = EUCA_ZALLOC(1, sizeof(ncMetadata));
        if (!localmeta) {
            LOGFATAL("out of memory! ncOps=%s\n", ncOp);
            unlock_exit(1);
        }
        memcpy(localmeta, pMeta, sizeof(ncMetadata));
        if (pMeta->correlationId) {
            localmeta->correlationId = strdup(pMeta->correlationId);
        } else {
            localmeta->correlationId = strdup("unset");
        }
        if (pMeta->userId) {
            localmeta->userId = strdup(pMeta->userId);
        } else {
            localmeta->userId = strdup("eucalyptus");
        }

        //TODO: zhill, change this to only be invoked on DescribeInstances and/or DescribeResources?
        //Update meta from config
        if (populateOutboundMeta(localmeta)) {
            LOGERROR("Failed to update output service metadata\n");
        }
        //Don't need to filter, CC should only have received.
        //filter_services(localmeta, config->ccStatus.serviceId.partition);

        close(filedes[0]);
        ncs = ncStubCreate(ncURL, NULL, NULL);
        if (config->use_wssec) {
            rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
        }

        LOGTRACE("\tncOps=%s ppid=%d client calling '%s'\n", ncOp, getppid(), ncOp);
        if (!strcmp(ncOp, "ncGetConsoleOutput")) {
            // args: char *instId
            char *instId = va_arg(al, char *);
            char **consoleOutput = va_arg(al, char **);

            rc = ncGetConsoleOutputStub(ncs, localmeta, instId, consoleOutput);
            if (timeout && consoleOutput) {
                if (!rc && *consoleOutput) {
                    len = strlen(*consoleOutput) + 1;
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = write(filedes[1], *consoleOutput, sizeof(char) * len);
                    rc = 0;
                } else {
                    len = 0;
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = 1;
                }
            }
        } else if (!strcmp(ncOp, "ncAttachVolume")) {
            char *instanceId = va_arg(al, char *);
            char *volumeId = va_arg(al, char *);
            char *remoteDev = va_arg(al, char *);
            char *localDev = va_arg(al, char *);

            rc = ncAttachVolumeStub(ncs, localmeta, instanceId, volumeId, remoteDev, localDev);
        } else if (!strcmp(ncOp, "ncDetachVolume")) {
            char *instanceId = va_arg(al, char *);
            char *volumeId = va_arg(al, char *);
            char *remoteDev = va_arg(al, char *);
            char *localDev = va_arg(al, char *);
            int force = va_arg(al, int);

            rc = ncDetachVolumeStub(ncs, localmeta, instanceId, volumeId, remoteDev, localDev, force);
        } else if (!strcmp(ncOp, "ncCreateImage")) {
            char *instanceId = va_arg(al, char *);
            char *volumeId = va_arg(al, char *);
            char *remoteDev = va_arg(al, char *);

            rc = ncCreateImageStub(ncs, localmeta, instanceId, volumeId, remoteDev);
        } else if (!strcmp(ncOp, "ncPowerDown")) {
            rc = ncPowerDownStub(ncs, localmeta);
        } else if (!strcmp(ncOp, "ncAssignAddress")) {
            char *instanceId = va_arg(al, char *);
            char *publicIp = va_arg(al, char *);

            rc = ncAssignAddressStub(ncs, localmeta, instanceId, publicIp);
        } else if (!strcmp(ncOp, "ncBroadcastNetworkInfo")) {
            char *networkInfo = va_arg(al, char *);
            rc = ncBroadcastNetworkInfoStub(ncs, localmeta, networkInfo);
        } else if (!strcmp(ncOp, "ncRebootInstance")) {
            char *instId = va_arg(al, char *);

            rc = ncRebootInstanceStub(ncs, localmeta, instId);
        } else if (!strcmp(ncOp, "ncTerminateInstance")) {
            char *instId = va_arg(al, char *);
            int force = va_arg(al, int);
            int *shutdownState = va_arg(al, int *);
            int *previousState = va_arg(al, int *);

            rc = ncTerminateInstanceStub(ncs, localmeta, instId, force, shutdownState, previousState);

            if (timeout) {
                if (!rc) {
                    len = 2;
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = write(filedes[1], shutdownState, sizeof(int));
                    rc = write(filedes[1], previousState, sizeof(int));
                    rc = 0;
                } else {
                    len = 0;
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = 1;
                }
            }
        } else if (!strcmp(ncOp, "ncStartNetwork")) {   //! @TODO remove this NC call logic, since it is not used any more
            char *uuid = va_arg(al, char *);
            char **peers = va_arg(al, char **);
            int peersLen = va_arg(al, int);
            int port = va_arg(al, int);
            int vlan = va_arg(al, int);
            char **outStatus = va_arg(al, char **);

            rc = ncStartNetworkStub(ncs, localmeta, uuid, peers, peersLen, port, vlan, outStatus);
            if (timeout && outStatus) {
                if (!rc && *outStatus) {
                    len = strlen(*outStatus) + 1;
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = write(filedes[1], *outStatus, sizeof(char) * len);
                    rc = 0;
                } else {
                    len = 0;
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = 1;
                }
            }

            if (outStatus)
                EUCA_FREE(*outStatus);
        } else if (!strcmp(ncOp, "ncRunInstance")) {
            char *uuid = va_arg(al, char *);
            char *instId = va_arg(al, char *);
            char *reservationId = va_arg(al, char *);
            virtualMachine *ncvm = va_arg(al, virtualMachine *);
            char *imageId = va_arg(al, char *);
            char *imageURL = va_arg(al, char *);
            char *kernelId = va_arg(al, char *);
            char *kernelURL = va_arg(al, char *);
            char *ramdiskId = va_arg(al, char *);
            char *ramdiskURL = va_arg(al, char *);
            char *ownerId = va_arg(al, char *);
            char *accountId = va_arg(al, char *);
            char *keyName = va_arg(al, char *);
            netConfig *ncnet = va_arg(al, netConfig *);
            char *userData = va_arg(al, char *);
            char *credential = va_arg(al, char *);
            char *launchIndex = va_arg(al, char *);
            char *platform = va_arg(al, char *);
            int expiryTime = va_arg(al, int);
            char **netNames = va_arg(al, char **);
            int netNamesLen = va_arg(al, int);
            ncInstance **outInst = va_arg(al, ncInstance **);

            rc = ncRunInstanceStub(ncs, localmeta, uuid, instId, reservationId, ncvm, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL,
                                   ownerId, accountId, keyName, ncnet, userData, credential, launchIndex, platform, expiryTime, netNames, netNamesLen, outInst);
            if (timeout && outInst) {
                if (!rc && *outInst) {
                    len = sizeof(ncInstance);
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = write(filedes[1], *outInst, sizeof(ncInstance));
                    rc = 0;
                } else {
                    len = 0;
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = 1;
                }
            }

            if (outInst)
                EUCA_FREE(*outInst);
        } else if (!strcmp(ncOp, "ncDescribeInstances")) {
            char **instIds = va_arg(al, char **);
            int instIdsLen = va_arg(al, int);
            ncInstance ***ncOutInsts = va_arg(al, ncInstance ***);
            int *ncOutInstsLen = va_arg(al, int *);

            rc = ncDescribeInstancesStub(ncs, localmeta, instIds, instIdsLen, ncOutInsts, ncOutInstsLen);
            if (timeout && ncOutInsts && ncOutInstsLen) {
                if (!rc) {
                    len = *ncOutInstsLen;
                    rc = write(filedes[1], &len, sizeof(int));
                    for (i = 0; i < len; i++) {
                        ncInstance *inst;
                        inst = (*ncOutInsts)[i];
                        rc = write(filedes[1], inst, sizeof(ncInstance));
                    }
                    rc = 0;
                } else {
                    len = 0;
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = 1;
                }
            }

            if (ncOutInsts) {
                if (ncOutInstsLen) {
                    for (i = 0; i < (*ncOutInstsLen); i++) {
                        EUCA_FREE((*ncOutInsts)[i]);
                    }
                }
                EUCA_FREE(*ncOutInsts);
            }
        } else if (!strcmp(ncOp, "ncDescribeResource")) {
            char *resourceType = va_arg(al, char *);
            ncResource **outRes = va_arg(al, ncResource **);
            char **errMsg = va_arg(al, char **);

            LOGTRACE("\tcalling ncDescribeResourceStub with resourceType=%s outRes=%lx errMsg=%lx\n", resourceType, (unsigned long)outRes, (unsigned long)errMsg);
            rc = ncDescribeResourceStub(ncs, localmeta, resourceType, outRes);
            LOGTRACE("\tcalled  ncDescribeResourceStub, rc = %d, timeout = %d\n", rc, timeout);
            if (timeout && outRes) {
                if (!rc && *outRes) {
                    len = sizeof(ncResource);
                    rc = write(filedes[1], &rc, sizeof(int));   //NOTE: we write back rc as well
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = write(filedes[1], *outRes, sizeof(ncResource));
                    rc = 0;
                } else {
                    (*errMsg) = (char *)axutil_error_get_message(ncs->env->error);
                    LOGTRACE("\terrMsg = %s\n", *errMsg);
                    if (*errMsg && (len = strnlen(*errMsg, 1024 - 1))) {
                        len += 1;
                        rc = write(filedes[1], &rc, sizeof(int));   //NOTE: we write back rc as well
                        rc = write(filedes[1], &len, sizeof(int));
                        rc = write(filedes[1], *errMsg, sizeof(char) * len);
                    } else {
                        len = 0;
                        rc = write(filedes[1], &rc, sizeof(int));   //NOTE: we write back rc as well
                        rc = write(filedes[1], &len, sizeof(int));
                    }
                    rc = 1;
                }
            }

            if (outRes)
                EUCA_FREE(*outRes);
        } else if (!strcmp(ncOp, "ncDescribeSensors")) {
            int history_size = va_arg(al, int);
            long long collection_interval_time_ms = va_arg(al, long long);
            char **instIds = va_arg(al, char **);
            int instIdsLen = va_arg(al, int);
            char **sensorIds = va_arg(al, char **);
            int sensorIdsLen = va_arg(al, int);
            sensorResource ***srs = va_arg(al, sensorResource ***);
            int *srsLen = va_arg(al, int *);

            rc = ncDescribeSensorsStub(ncs, localmeta, history_size, collection_interval_time_ms, instIds, instIdsLen, sensorIds, sensorIdsLen, srs, srsLen);
            if (timeout && srs && srsLen) {
                if (!rc) {
                    len = *srsLen;
                    rc = write(filedes[1], &len, sizeof(int));
                    for (i = 0; i < len; i++) {
                        sensorResource *sr;
                        sr = (*srs)[i];
                        rc = write(filedes[1], sr, sizeof(sensorResource));
                    }
                    rc = 0;
                } else {
                    len = 0;
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = 1;
                }
            }

            if (srs) {
                if (srsLen) {
                    for (i = 0; i < (*srsLen); i++) {
                        EUCA_FREE((*srs)[i]);
                    }
                }
                EUCA_FREE(*srs);
            }
        } else if (!strcmp(ncOp, "ncBundleInstance")) {
            char *instanceId = va_arg(al, char *);
            char *bucketName = va_arg(al, char *);
            char *filePrefix = va_arg(al, char *);
            char *objectStorageURL = va_arg(al, char *);
            char *userPublicKey = va_arg(al, char *);
            char *S3Policy = va_arg(al, char *);
            char *S3PolicySig = va_arg(al, char *);

            rc = ncBundleInstanceStub(ncs, localmeta, instanceId, bucketName, filePrefix, objectStorageURL, userPublicKey, S3Policy, S3PolicySig);
        } else if (!strcmp(ncOp, "ncBundleRestartInstance")) {
            char *instanceId = va_arg(al, char *);
            rc = ncBundleRestartInstanceStub(ncs, localmeta, instanceId);
        } else if (!strcmp(ncOp, "ncCancelBundleTask")) {
            char *instanceId = va_arg(al, char *);
            rc = ncCancelBundleTaskStub(ncs, localmeta, instanceId);
        } else if (!strcmp(ncOp, "ncModifyNode")) {
            char *stateName = va_arg(al, char *);
            rc = ncModifyNodeStub(ncs, localmeta, stateName);
        } else if (!strcmp(ncOp, "ncMigrateInstances")) {
            ncInstance **instances = va_arg(al, ncInstance **);
            int instancesLen = va_arg(al, int);
            char *action = va_arg(al, char *);
            char *credentials = va_arg(al, char *);
            rc = ncMigrateInstancesStub(ncs, localmeta, instances, instancesLen, action, credentials);
            WRITE_REPLY_STRING;
        } else if (!strcmp(ncOp, "ncStartInstance")) {
            char *instanceId = va_arg(al, char *);
            rc = ncStartInstanceStub(ncs, localmeta, instanceId);
            WRITE_REPLY_STRING;
        } else if (!strcmp(ncOp, "ncStopInstance")) {
            char *instanceId = va_arg(al, char *);
            rc = ncStopInstanceStub(ncs, localmeta, instanceId);
            WRITE_REPLY_STRING;
        } else {
            LOGWARN("\tncOps=%s ppid=%d operation '%s' not found\n", ncOp, getppid(), ncOp);
            rc = 1;
        }
        LOGTRACE("\tncOps=%s ppid=%d done calling '%s' with exit code '%d'\n", ncOp, getppid(), ncOp, rc);
        if (localmeta->replyString != NULL) {
            LOGDEBUG("NC replied to '%s' with '%s'\n", ncOp, localmeta->replyString);
        }
        if (rc) {
            ret = 1;
        } else {
            ret = 0;
        }
        close(filedes[1]);

        // Free our local meta data structure and associated memory
        EUCA_FREE(localmeta->replyString);
        EUCA_FREE(localmeta->correlationId);
        EUCA_FREE(localmeta->userId);
        EUCA_FREE(localmeta);

        // ditch our stub
        if (ncs != NULL) {
            ncStubDestroy(ncs);
            ncs = NULL;
        }
        exit(ret);
    } else {
        // returns for each client call
        close(filedes[1]);

        if (!strcmp(ncOp, "ncGetConsoleOutput")) {
            char *instId = NULL;
            char **outConsoleOutput = NULL;

            instId = va_arg(al, char *);
            outConsoleOutput = va_arg(al, char **);
            if (outConsoleOutput) {
                *outConsoleOutput = NULL;
            }
            if (timeout && outConsoleOutput) {
                rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
                if (rbytes <= 0) {
                    killwait(pid);
                    opFail = 1;
                } else {
                    *outConsoleOutput = EUCA_ALLOC(len, sizeof(char));
                    if (!*outConsoleOutput) {
                        LOGFATAL("out of memory! ncOps=%s\n", ncOp);
                        unlock_exit(1);
                    }
                    rbytes = timeread(filedes[0], *outConsoleOutput, len, timeout);
                    if (rbytes <= 0) {
                        killwait(pid);
                        opFail = 1;
                    }
                }
            }
        } else if (!strcmp(ncOp, "ncTerminateInstance")) {
            char *instId = NULL;
            int force = 0;
            int *shutdownState = NULL;
            int *previousState = NULL;

            instId = va_arg(al, char *);
            force = va_arg(al, int);
            shutdownState = va_arg(al, int *);
            previousState = va_arg(al, int *);
            if (shutdownState && previousState) {
                *shutdownState = *previousState = 0;
            }
            if (timeout && shutdownState && previousState) {
                rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
                if (rbytes <= 0) {
                    killwait(pid);
                    opFail = 1;
                } else {
                    rbytes = timeread(filedes[0], shutdownState, sizeof(int), timeout);
                    if (rbytes <= 0) {
                        killwait(pid);
                        opFail = 1;
                    }
                    rbytes = timeread(filedes[0], previousState, sizeof(int), timeout);
                    if (rbytes <= 0) {
                        killwait(pid);
                        opFail = 1;
                    }
                }
            }
        } else if (!strcmp(ncOp, "ncStartNetwork")) {   //! @TODO remove this NC call logic, since it is not used any more
            char *uuid = NULL;
            char **peers = NULL;
            int peersLen = 0;
            int port = 0;
            int vlan = 0;
            char **outStatus = NULL;

            uuid = va_arg(al, char *);
            peers = va_arg(al, char **);
            peersLen = va_arg(al, int);
            port = va_arg(al, int);
            vlan = va_arg(al, int);
            outStatus = va_arg(al, char **);
            if (outStatus) {
                *outStatus = NULL;
            }
            if (timeout && outStatus) {
                *outStatus = NULL;
                rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
                if (rbytes <= 0) {
                    killwait(pid);
                    opFail = 1;
                } else {
                    *outStatus = EUCA_ALLOC(len, sizeof(char));
                    if (!*outStatus) {
                        LOGFATAL("out of memory! ncOps=%s\n", ncOp);
                        unlock_exit(1);
                    }
                    rbytes = timeread(filedes[0], *outStatus, len, timeout);
                    if (rbytes <= 0) {
                        killwait(pid);
                        opFail = 1;
                    }
                }
            }
        } else if (!strcmp(ncOp, "ncRunInstance")) {
            char *uuid = NULL;
            char *instId = NULL;
            char *reservationId = NULL;
            virtualMachine *ncvm = NULL;
            char *imageId = NULL;
            char *imageURL = NULL;
            char *kernelId = NULL;
            char *kernelURL = NULL;
            char *ramdiskId = NULL;
            char *ramdiskURL = NULL;
            char *ownerId = NULL;
            char *accountId = NULL;
            char *keyName = NULL;
            netConfig *ncnet = NULL;
            char *userData = NULL;
            char *credential = NULL;
            char *launchIndex = NULL;
            char *platform = NULL;
            int expiryTime = 0;
            char **netNames = NULL;
            int netNamesLen = 0;
            ncInstance **outInst = NULL;

            uuid = va_arg(al, char *);
            instId = va_arg(al, char *);
            reservationId = va_arg(al, char *);
            ncvm = va_arg(al, virtualMachine *);
            imageId = va_arg(al, char *);
            imageURL = va_arg(al, char *);
            kernelId = va_arg(al, char *);
            kernelURL = va_arg(al, char *);
            ramdiskId = va_arg(al, char *);
            ramdiskURL = va_arg(al, char *);
            ownerId = va_arg(al, char *);
            accountId = va_arg(al, char *);
            keyName = va_arg(al, char *);
            ncnet = va_arg(al, netConfig *);
            userData = va_arg(al, char *);
            credential = va_arg(al, char *);
            launchIndex = va_arg(al, char *);
            platform = va_arg(al, char *);
            expiryTime = va_arg(al, int);
            netNames = va_arg(al, char **);
            netNamesLen = va_arg(al, int);
            outInst = va_arg(al, ncInstance **);
            if (outInst) {
                *outInst = NULL;
            }
            if (timeout && outInst) {
                rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
                if (rbytes <= 0) {
                    killwait(pid);
                    opFail = 1;
                } else {
                    *outInst = EUCA_ZALLOC(1, sizeof(ncInstance));
                    if (!*outInst) {
                        LOGFATAL("out of memory! ncOps=%s\n", ncOp);
                        unlock_exit(1);
                    }
                    rbytes = timeread(filedes[0], *outInst, sizeof(ncInstance), timeout);
                    if (rbytes <= 0) {
                        killwait(pid);
                        opFail = 1;
                    }
                }
            }
        } else if (!strcmp(ncOp, "ncDescribeInstances")) {
            char **instIds = NULL;
            int instIdsLen = 0;
            ncInstance ***ncOutInsts = NULL;
            int *ncOutInstsLen = NULL;

            instIds = va_arg(al, char **);
            instIdsLen = va_arg(al, int);
            ncOutInsts = va_arg(al, ncInstance ***);
            ncOutInstsLen = va_arg(al, int *);
            if (ncOutInstsLen && ncOutInsts) {
                *ncOutInstsLen = 0;
                *ncOutInsts = NULL;
            }
            if (timeout && ncOutInsts && ncOutInstsLen) {
                rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
                if (rbytes <= 0) {
                    killwait(pid);
                    opFail = 1;
                } else {
                    *ncOutInsts = EUCA_ZALLOC(len, sizeof(ncInstance *));
                    if (!*ncOutInsts) {
                        LOGFATAL("out of memory! ncOps=%s\n", ncOp);
                        unlock_exit(1);
                    }
                    *ncOutInstsLen = len;
                    for (i = 0; i < len; i++) {
                        ncInstance *inst;
                        inst = EUCA_ZALLOC(1, sizeof(ncInstance));
                        if (!inst) {
                            LOGFATAL("out of memory! ncOps=%s\n", ncOp);
                            unlock_exit(1);
                        }
                        rbytes = timeread(filedes[0], inst, sizeof(ncInstance), timeout);
                        (*ncOutInsts)[i] = inst;
                    }
                }
            }
        } else if (!strcmp(ncOp, "ncDescribeResource")) {
            char *resourceType = NULL;
            char **errMsg = NULL;
            ncResource **outRes = NULL;

            resourceType = va_arg(al, char *);
            outRes = va_arg(al, ncResource **);
            errMsg = va_arg(al, char **);
            if (outRes) {
                *outRes = NULL;
            }
            if (timeout && outRes) {
                // first int we read back is the 'rc', then the 'len'
                rbytes = timeread(filedes[0], &opFail, sizeof(int), timeout);
                if (rbytes <= 0 || (rbytes = timeread(filedes[0], &len, sizeof(int), timeout)) <= 0) {
                    killwait(pid);
                    opFail = 1;
                } else if (opFail && len) {
                    *errMsg = EUCA_ZALLOC(len, sizeof(char));
                    if (*errMsg == NULL) {
                        LOGFATAL("out of memory! ncOps=%s\n", ncOp);
                        unlock_exit(1);
                    }
                    rbytes = timeread(filedes[0], *errMsg, len * sizeof(char), timeout);
                    if (rbytes <= 0 || opFail) {
                        kill(pid, SIGKILL);
                        opFail = 1;
                    }
                } else {
                    *outRes = EUCA_ZALLOC(1, sizeof(ncResource));
                    if (*outRes == NULL) {
                        LOGFATAL("out of memory! ncOps=%s\n", ncOp);
                        unlock_exit(1);
                    }
                    rbytes = timeread(filedes[0], *outRes, sizeof(ncResource), timeout);
                    if (rbytes <= 0) {
                        killwait(pid);
                        opFail = 1;
                    }
                }
            }
        } else if (!strcmp(ncOp, "ncDescribeSensors")) {
            int history_size = 0;
            long long collection_interval_time_ms = 0L;
            char **instIds = NULL;
            int instIdsLen = 0;
            char **sensorIds = NULL;
            int sensorIdsLen = 0;
            sensorResource ***srs = NULL;
            int *srsLen = NULL;

            history_size = va_arg(al, int);
            collection_interval_time_ms = va_arg(al, long long);
            instIds = va_arg(al, char **);
            instIdsLen = va_arg(al, int);
            sensorIds = va_arg(al, char **);
            sensorIdsLen = va_arg(al, int);
            srs = va_arg(al, sensorResource ***);
            srsLen = va_arg(al, int *);

            if (srs && srsLen) {
                *srs = NULL;
                *srsLen = 0;
            }
            if (timeout && srs && srsLen) {
                rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
                if (rbytes <= 0) {
                    killwait(pid);
                    opFail = 1;
                } else {
                    *srs = EUCA_ZALLOC(len, sizeof(sensorResource *));
                    if (*srs == NULL) {
                        LOGFATAL("out of memory! ncOps=%s\n", ncOp);
                        unlock_exit(1);
                    }
                    *srsLen = len;
                    for (i = 0; i < len; i++) {
                        sensorResource *sr;
                        sr = EUCA_ZALLOC(1, sizeof(sensorResource));
                        if (sr == NULL) {
                            LOGFATAL("out of memory! ncOps=%s\n", ncOp);
                            unlock_exit(1);
                        }
                        rbytes = timeread(filedes[0], sr, sizeof(sensorResource), timeout);
                        (*srs)[i] = sr;
                    }
                }
            }
        } else if (!strcmp(ncOp, "ncMigrateInstances")) {
            READ_REPLY_STRING;
        } else if (!strcmp(ncOp, "ncStartInstance")) {
            READ_REPLY_STRING;
        } else if (!strcmp(ncOp, "ncStopInstance")) {
            READ_REPLY_STRING;
        } else {
            // nothing to do in default case (succ/fail encoded in exit code)
        }

        close(filedes[0]);
        if (timeout) {
            rc = timewait(pid, &status, timeout);
            if (WIFEXITED(status)) {
                rc = WEXITSTATUS(status);
            } else {
                int sig = -1;
                int dump = 0;
                if (WIFSIGNALED(status)) {
                    sig = WTERMSIG(status);
                    dump = WCOREDUMP(status);
                }
                if (sig == SIGTERM || sig == SIGKILL) { // our killwait() first tries SIGTERM and then SIGKILL
                    LOGDEBUG("child process %d handling '%s' was terminated with %d\n", pid, ncOp, sig);
                } else {
                    LOGERROR("BUG: child process %d handling '%s' was terminated with %d (core=%d)\n", pid, ncOp, sig, dump);
                }
                rc = 1;
            }
        } else {
            rc = 0;
        }
    }

    LOGTRACE("done ncOps=%s clientrc=%d opFail=%d\n", ncOp, rc, opFail);
    if (rc || opFail) {
        ret = 1;
    } else {
        ret = 0;
    }

    // release the lock
    sem_mypost(ncLock);

    va_end(al);

    return (ret);

#undef WRITE_REPLY_STRING
#undef READ_REPLY_STRING
}

//!
//! Calculate nc call timeout, based on when operation was started (op_start), the total
//! number of calls to make (numCalls), and the current progress (idx)
//!
//! @param[in] op_start
//! @param[in] op_max
//! @param[in] numCalls
//! @param[in] idx
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int ncGetTimeout(time_t op_start, time_t op_max, int numCalls, int idx)
{
    time_t op_timer, op_pernode;
    int numLeft;

    numLeft = numCalls - idx;
    if (numLeft <= 0) {
        numLeft = 1;
    }

    op_timer = op_max - (time(NULL) - op_start);
    op_pernode = op_timer / numLeft;

    return (maxint(minint(op_pernode, OP_TIMEOUT_PERNODE), OP_TIMEOUT_MIN));
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] instanceId
//! @param[in] remoteDev
//! @param[in] localDev
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doAttachVolume(ncMetadata * pMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev)
{
    int i, rc, start = 0, stop = 0, ret = 0, done = 0, timeout;
    ccInstance *myInstance;
    time_t op_start;
    ccResourceCache resourceCacheLocal;

    i = 0;
    myInstance = NULL;
    op_start = time(NULL);

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGINFO("[%s][%s] attaching volume\n", SP(instanceId), SP(volumeId));
    LOGDEBUG("invoked: userId=%s, volumeId=%s, instanceId=%s, remoteDev=%s, localDev=%s\n", SP(pMeta ? pMeta->userId : "UNSET"),
             SP(volumeId), SP(instanceId), SP(remoteDev), SP(localDev));
    if (!volumeId || !instanceId || !remoteDev || !localDev) {
        LOGERROR("bad input params\n");
        return (1);
    }

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    rc = find_instanceCacheId(instanceId, &myInstance);
    if (!rc) {
        // found the instance in the cache
        if (myInstance) {
            start = myInstance->ncHostIdx;
            stop = start + 1;
            EUCA_FREE(myInstance);
        }
    } else {
        start = 0;
        stop = resourceCacheLocal.numResources;
    }

    done = 0;
    for (i = start; i < stop && !done; i++) {
        timeout = ncGetTimeout(op_start, OP_TIMEOUT, stop - start, i);
        timeout = maxint(timeout, ATTACH_VOL_TIMEOUT_SECONDS);

        rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[i].lockidx, resourceCacheLocal.resources[i].ncURL, "ncAttachVolume",
                          instanceId, volumeId, remoteDev, localDev);

        if (rc) {
            ret = 1;
        } else {
            ret = 0;
            done++;
        }
    }

    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] instanceId
//! @param[in] remoteDev
//! @param[in] localDev
//! @param[in] force
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doDetachVolume(ncMetadata * pMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev, int force)
{
    int i, rc, start = 0, stop = 0, ret = 0, done = 0, timeout;
    ccInstance *myInstance;
    time_t op_start;
    ccResourceCache resourceCacheLocal;

    i = 0;
    myInstance = NULL;
    op_start = time(NULL);

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGINFO("[%s][%s] detaching volume\n", SP(instanceId), SP(volumeId));
    LOGDEBUG("invoked: volumeId=%s, instanceId=%s, remoteDev=%s, localDev=%s, force=%d\n", SP(volumeId), SP(instanceId), SP(remoteDev), SP(localDev), force);
    if (!volumeId || !instanceId || !remoteDev || !localDev) {
        LOGERROR("bad input params\n");
        return (1);
    }

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    rc = find_instanceCacheId(instanceId, &myInstance);
    if (!rc) {
        // found the instance in the cache
        if (myInstance) {
            start = myInstance->ncHostIdx;
            stop = start + 1;
            EUCA_FREE(myInstance);
        }
    } else {
        start = 0;
        stop = resourceCacheLocal.numResources;
    }

    for (i = start; i < stop; i++) {
        timeout = ncGetTimeout(op_start, OP_TIMEOUT, stop - start, i);
        timeout = maxint(timeout, DETACH_VOL_TIMEOUT_SECONDS);

        rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[i].lockidx, resourceCacheLocal.resources[i].ncURL, "ncDetachVolume",
                          instanceId, volumeId, remoteDev, localDev, force);
        if (rc) {
            ret = 1;
        } else {
            ret = 0;
            done++;
        }
    }

    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] accountId
//! @param[in] type
//! @param[in] namedLen
//! @param[in] sourceNames
//! @param[in] userNames
//! @param[in] netLen
//! @param[in] sourceNets
//! @param[in] destName
//! @param[in] destUserName
//! @param[in] protocol
//! @param[in] minPort
//! @param[in] maxPort
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doConfigureNetwork(ncMetadata * pMeta, char *accountId, char *type, int namedLen, char **sourceNames, char **userNames, int netLen,
                       char **sourceNets, char *destName, char *destUserName, char *protocol, int minPort, int maxPort)
{
    int rc, i, fail;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGINFO("configuring network %s\n", SP(destName));
    LOGDEBUG("invoked: userId=%s, accountId=%s, type=%s, namedLen=%d, netLen=%d, destName=%s, destUserName=%s, protocol=%s, minPort=%d, maxPort=%d\n",
             pMeta ? SP(pMeta->userId) : "UNSET", SP(accountId), SP(type), namedLen, netLen, SP(destName), SP(destUserName), SP(protocol), minPort, maxPort);

    if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM) || !strcmp(vnetconfig->mode, NETMODE_STATIC) || !strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        fail = 0;
    } else {

        if (destUserName == NULL) {
            if (accountId) {
                destUserName = accountId;
            } else {
                // destUserName is not set, return fail
                LOGERROR("cannot set destUserName from pMeta or input\n");
                return (1);
            }
        }

        sem_mywait(VNET);

        fail = 0;
        for (i = 0; i < namedLen; i++) {
            if (sourceNames && userNames) {
                rc = vnetTableRule(vnetconfig, type, destUserName, destName, userNames[i], NULL, sourceNames[i], protocol, minPort, maxPort);
            }
            if (rc) {
                LOGERROR("vnetTableRule() returned error rc=%d\n", rc);
                fail = 1;
            }
        }
        for (i = 0; i < netLen; i++) {
            if (sourceNets) {
                rc = vnetTableRule(vnetconfig, type, destUserName, destName, NULL, sourceNets[i], NULL, protocol, minPort, maxPort);
            }
            if (rc) {
                LOGERROR("vnetTableRule() returned error rc=%d\n", rc);
                fail = 1;
            }
        }
        sem_mypost(VNET);
    }

    LOGTRACE("done\n");

    shawn();

    if (fail) {
        return (1);
    }
    return (0);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] accountId
//! @param[in] destName
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doFlushNetwork(ncMetadata * pMeta, char *accountId, char *destName)
{
    int rc;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM) || !strcmp(vnetconfig->mode, NETMODE_STATIC) || !strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        return (0);
    }

    sem_mywait(VNET);
    rc = vnetFlushTable(vnetconfig, accountId, destName);
    sem_mypost(VNET);
    return (rc);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] networkInfo
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doBroadcastNetworkInfo(ncMetadata * pMeta, char *networkInfo)
{
    int rc = 0, ret = 0, i = 0;
    char *xmlbuf = NULL;
    char xmlfile[EUCA_MAX_PATH];
    globalNetworkInfo *gni = NULL;
    gni_cluster *myself = NULL;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGDEBUG("invoked: networkInfo=%.16s\n", SP(networkInfo));

    if (!networkInfo) {
        LOGDEBUG("bad input params\n");
        return (1);
    }

    sem_mywait(GLOBALNETWORKINFO);

    // init the XML
    xmlbuf = base64_dec((unsigned char *)networkInfo, strlen(networkInfo));
    if (xmlbuf) {
        snprintf(xmlfile, EUCA_MAX_PATH, "/tmp/euca-global-net-XXXXXX");

        if (str2file(xmlbuf, xmlfile, O_CREAT | O_EXCL | O_RDWR, 0644, TRUE) == EUCA_OK) {
            LOGDEBUG("created and populated tmpfile '%s'\n", xmlfile);

            gni = gni_init();
            if (gni) {
                // decode/read/parse the globalnetworkinfo, assign any incorrect public/private IP mappings based on global view
                rc = gni_populate(gni, xmlfile);
                LOGDEBUG("done with gni_populate()\n");

                // do any CC actions based on contents of new network view

                // reset macprefix
                rc = gni_find_self_cluster(gni, &myself);
                if (rc) {
                    LOGWARN("failed to find local host IP in list of enabled clusters, skipping macPrefix update\n");
                } else if (myself && strlen(myself->macPrefix) && strcmp(vnetconfig->macPrefix, myself->macPrefix)) {
                    LOGDEBUG("reset local cluster macPrefix from '%s' to '%s'\n", vnetconfig->macPrefix, myself->macPrefix);
                    snprintf(vnetconfig->macPrefix, 6, "%s", myself->macPrefix);
                }

                LOGTRACE("gni->max_instances == %d\n", gni->max_instances);
                for (i = 0; i < gni->max_instances; i++) {
                    char *strptra = NULL, *strptrb = NULL;
                    strptra = hex2dot(gni->instances[i].publicIp);
                    strptrb = hex2dot(gni->instances[i].privateIp);

                    if (gni->instances[i].publicIp && gni->instances[i].privateIp) {
                        LOGDEBUG("found instance in broadcast network info: %s (%s/%s)\n", gni->instances[i].name, SP(strptra), SP(strptrb));
                        // here, we should decide if we need to send the mapping, or not?
                        rc = doAssignAddress(pMeta, NULL, strptra, strptrb);

                        LOGDEBUG("assigned address: (%s -> %s) rc: %d\n", strptra, strptrb, rc);
                    } else {
                        LOGDEBUG("instance does not have either public or private IP set (id=%s pub=%s priv=%s)\n", gni->instances[i].name, SP(strptra), SP(strptrb));
                    }
                    EUCA_FREE(strptra);
                    EUCA_FREE(strptrb);

                }

                // free the gni
                rc = gni_free(gni);
            }

            unlink(xmlfile);
        }

        EUCA_FREE(xmlbuf);
    }
    // populate globalnetworkinfo
    snprintf(globalnetworkinfo->networkInfo, MAX_NETWORK_INFO, "%s", networkInfo);
    config->kick_broadcast_network_info = 1;

    sem_mypost(GLOBALNETWORKINFO);

    LOGTRACE("done.\n");
    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] uuid
//! @param[in] src
//! @param[in] dst
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doAssignAddress(ncMetadata * pMeta, char *uuid, char *src, char *dst)
{
    int rc, ret;
    ccInstance *myInstance = NULL;
    ccResourceCache resourceCacheLocal;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGINFO("assigning address %s to %s\n", SP(src), SP(dst));
    LOGDEBUG("invoked: src=%s, dst=%s, uuid=%s\n", SP(src), SP(dst), SP(uuid));

    if (!src || !dst || !strcmp(src, "0.0.0.0")) {
        LOGDEBUG("bad input params\n");
        return (1);
    }
    set_dirty_instanceCache();

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    ret = 1;
    if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM) || !strcmp(vnetconfig->mode, NETMODE_STATIC)) {
        ret = 0;
    } else {

        rc = find_instanceCacheIP(dst, &myInstance);
        if (!rc) {
            if (!strcmp(vnetconfig->mode, NETMODE_EDGE)) {
                ret = 0;
            } else if (myInstance) {
                LOGDEBUG("found local instance, applying %s->%s mapping\n", src, dst);
                sem_mywait(VNET);
                rc = vnetReassignAddress(vnetconfig, uuid, src, dst);
                if (rc) {
                    LOGERROR("vnetReassignAddress() failed rc=%d\n", rc);
                    ret = 1;
                } else {
                    ret = 0;
                }
                sem_mypost(VNET);
            }
            EUCA_FREE(myInstance);

        } else {
            LOGDEBUG("skipping %s->%s mapping, as this clusters does not own the instance (%s)\n", src, dst, dst);
        }
    }

    if (!ret && strcmp(dst, "0.0.0.0")) {
        // everything worked, update instance cache

        rc = map_instanceCache(privIpCmp, dst, pubIpSet, src);
        if (rc) {
            LOGERROR("map_instanceCache() failed to assign %s->%s\n", dst, src);
        } else {
            rc = find_instanceCacheIP(src, &myInstance);
            if (!rc) {
                LOGDEBUG("found instance (%s) in cache with IP (%s)\n", myInstance->instanceId, myInstance->ccnet.publicIp);
                // found the instance in the cache
                if (myInstance) {
                    //timeout = ncGetTimeout(op_start, OP_TIMEOUT, 1, myInstance->ncHostIdx);
                    rc = ncClientCall(pMeta, OP_TIMEOUT, resourceCacheLocal.resources[myInstance->ncHostIdx].lockidx,
                                      resourceCacheLocal.resources[myInstance->ncHostIdx].ncURL, "ncAssignAddress", myInstance->instanceId, myInstance->ccnet.publicIp);
                    if (rc) {
                        LOGERROR("could not sync public IP %s with NC\n", src);
                        ret = 1;
                    } else {
                        ret = 0;
                    }
                    EUCA_FREE(myInstance);
                }
            }
        }
    }

    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//!
//!
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[out] outAddresses
//! @param[out] outAddressesLen
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doDescribePublicAddresses(ncMetadata * pMeta, publicip ** outAddresses, int *outAddressesLen)
{
    int rc, ret;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGDEBUG("invoked: userId=%s\n", SP(pMeta ? pMeta->userId : "UNSET"));

    ret = 0;
    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN)) {
        sem_mywait(VNET);
        *outAddresses = vnetconfig->publicips;
        *outAddressesLen = NUMBER_OF_PUBLIC_IPS;
        sem_mypost(VNET);
    } else {
        *outAddresses = NULL;
        *outAddressesLen = 0;
        ret = 0;
    }

    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] src
//! @param[in] dst
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doUnassignAddress(ncMetadata * pMeta, char *src, char *dst)
{
    int rc, ret;
    ccInstance *myInstance = NULL;
    ccResourceCache resourceCacheLocal;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGINFO("unassigning address %s\n", SP(src));
    LOGDEBUG("invoked: userId=%s, src=%s, dst=%s\n", SP(pMeta ? pMeta->userId : "UNSET"), SP(src), SP(dst));

    if (!src || !dst || !strcmp(src, "0.0.0.0")) {
        LOGDEBUG("bad input params\n");
        return (1);
    }
    set_dirty_instanceCache();

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    ret = 0;

    if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM) || !strcmp(vnetconfig->mode, NETMODE_STATIC)) {
        ret = 0;
    } else {

        sem_mywait(VNET);

        ret = vnetReassignAddress(vnetconfig, "UNSET", src, "0.0.0.0");
        if (ret) {
            LOGERROR("vnetReassignAddress() failed ret=%d\n", ret);
            ret = 1;
        }

        sem_mypost(VNET);
    }

    if (!ret) {

        rc = find_instanceCacheIP(src, &myInstance);
        if (!rc) {
            LOGDEBUG("found instance %s in cache with IP %s\n", myInstance->instanceId, myInstance->ccnet.publicIp);
            // found the instance in the cache
            if (myInstance) {
                //timeout = ncGetTimeout(op_start, OP_TIMEOUT, 1, myInstance->ncHostIdx);
                rc = ncClientCall(pMeta, OP_TIMEOUT, resourceCacheLocal.resources[myInstance->ncHostIdx].lockidx,
                                  resourceCacheLocal.resources[myInstance->ncHostIdx].ncURL, "ncAssignAddress", myInstance->instanceId, "0.0.0.0");
                if (rc) {
                    LOGERROR("could not sync IP with NC\n");
                    ret = 1;
                } else {
                    ret = 0;
                }
                EUCA_FREE(myInstance);
            }
        }
        // refresh instance cache
        rc = map_instanceCache(pubIpCmp, src, pubIpSet, "0.0.0.0");
        if (rc) {
            LOGERROR("map_instanceCache() failed to assign %s->%s\n", dst, src);
        }
    }

    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] accountId
//! @param[in] netName
//! @param[in] vlan
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doStopNetwork(ncMetadata * pMeta, char *accountId, char *netName, int vlan)
{
    int rc, ret;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGINFO("stopping network %d\n", vlan);
    LOGDEBUG("invoked: userId=%s, accountId=%s, netName=%s, vlan=%d\n", SP(pMeta ? pMeta->userId : "UNSET"), SP(accountId), SP(netName), vlan);
    if (!pMeta || !netName || vlan < 0) {
        LOGERROR("bad input params\n");
    }

    if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM) || !strcmp(vnetconfig->mode, NETMODE_STATIC) || !strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        ret = 0;
    } else {

        sem_mywait(VNET);
        if (pMeta != NULL) {
            rc = vnetStopNetwork(vnetconfig, vlan, accountId, netName);
        }
        ret = rc;
        sem_mypost(VNET);
    }

    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//!
//!
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  vmsubdomain the internal subdomain name to put in euca-dhcp.conf to provide to instances
//! @param[in]  nameservers comma separated list of name servers to give to the instances
//! @param[in]  ccs
//! @param[in]  ccsLen
//! @param[out] outvnetConfig
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doDescribeNetworks(ncMetadata * pMeta, char *vmsubdomain, char *nameservers, char **ccs, int ccsLen, vnetConfig * outvnetConfig)
{
    int i = 0;
    int rc = 0;
    int nbNameServers = 0;
    u32 ip = 0;
    char *nameServerList[NUMBER_OF_NAME_SERVERS] = { NULL };
    boolean kickDhcp = FALSE;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGDEBUG("invoked: userId=%s, vmsubdomain=%s, nameservers='%s', ccsLen=%d\n", SP(pMeta ? pMeta->userId : "UNSET"), SP(vmsubdomain), SP(nameservers), ccsLen);

    // ensure that we have the latest network state from the CC (based on instance cache) before responding to CLC
    rc = checkActiveNetworks();
    if (rc) {
        LOGWARN("checkActiveNetworks() failed, will attempt to re-sync\n");
    }

    sem_mywait(VNET);
    {
        if (vmsubdomain) {
            // Check if our configuration differ, in this case, we'll have to kick the DHCP config
            if (strcmp(vnetconfig->eucaDomainName, vmsubdomain))
                kickDhcp = TRUE;

            snprintf(vnetconfig->eucaDomainName, sizeof(vnetconfig->eucaDomainName), "%s.internal", vmsubdomain);
        }

        if (nameservers) {
            memset(vnetconfig->eucaNameServer, 0, sizeof(vnetconfig->eucaNameServer));
            if ((nbNameServers = euca_tokenizer(nameservers, ",", nameServerList, NUMBER_OF_NAME_SERVERS)) > 0) {
                for (i = 0; i < nbNameServers; i++) {
                    // Convert our IP address to u32
                    ip = dot2hex(nameServerList[i]);

                    // Check if anything is different cause we'll have to kick the DHCP config
                    if (vnetconfig->eucaNameServer[i] != ip)
                        kickDhcp = TRUE;

                    // Update our list
                    vnetconfig->eucaNameServer[i] = ip;
                    EUCA_FREE(nameServerList[i]);
                }
            }
        }

        if (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN)) {
            rc = vnetSetCCS(vnetconfig, ccs, ccsLen);
            rc = vnetSetupTunnels(vnetconfig);
        }
        // We will only kick the DHCP configuration if anything change and if we're not in SYSTEM mode.
        if (strcmp(vnetconfig->mode, NETMODE_SYSTEM) && kickDhcp)
            config->kick_dhcp = TRUE;

        memcpy(outvnetConfig, vnetconfig, sizeof(vnetConfig));
    }
    sem_mypost(VNET);
    LOGTRACE("done\n");

    shawn();

    return (0);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] accountId
//! @param[in] uuid
//! @param[in] netName
//! @param[in] vlan
//! @param[in] vmsubdomain the internal subdomain name to put in euca-dhcp.conf to provide to instances
//! @param[in] nameservers comma separated list of name servers to give to the instances
//! @param[in] ccs
//! @param[in] ccsLen
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doStartNetwork(ncMetadata * pMeta, char *accountId, char *uuid, char *netName, int vlan, char *vmsubdomain, char *nameservers, char **ccs, int ccsLen)
{
    int i = 0;
    int rc = 0;
    int ret = 0;
    int nbNameServers = 0;
    char *brname = NULL;
    char *nameServerList[NUMBER_OF_NAME_SERVERS] = { NULL };

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGINFO("starting network %s with VLAN %d\n", SP(netName), vlan);
    LOGDEBUG("invoked: userId=%s, accountId=%s, vmsubdomain=%s, nameservers=%s, ccsLen=%d\n",
             SP(pMeta ? pMeta->userId : "UNSET"), SP(accountId), SP(vmsubdomain), SP(nameservers), ccsLen);

    if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM) || !strcmp(vnetconfig->mode, NETMODE_STATIC) || !strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        ret = 0;
    } else {
        sem_mywait(VNET);
        {
            if (vmsubdomain) {
                snprintf(vnetconfig->eucaDomainName, sizeof(vnetconfig->eucaDomainName), "%s.internal", vmsubdomain);
            }

            if (nameservers) {
                memset(vnetconfig->eucaNameServer, 0, sizeof(vnetconfig->eucaNameServer));
                if ((nbNameServers = euca_tokenizer(nameservers, ",", nameServerList, NUMBER_OF_NAME_SERVERS)) > 0) {
                    for (i = 0; i < nbNameServers; i++) {
                        vnetconfig->eucaNameServer[i] = dot2hex(nameServerList[i]);
                        EUCA_FREE(nameServerList[i]);
                    }
                }
            }

            rc = vnetSetCCS(vnetconfig, ccs, ccsLen);
            rc = vnetSetupTunnels(vnetconfig);

            brname = NULL;
            rc = vnetStartNetwork(vnetconfig, vlan, uuid, accountId, netName, &brname);
            EUCA_FREE(brname);
        }
        sem_mypost(VNET);

        if (rc) {
            LOGERROR("vnetStartNetwork() failed (%d)\n", rc);
            ret = 1;
        } else {
            ret = 0;
        }

    }

    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//!
//!
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  ccvms
//! @param[in]  vmLen
//! @param[out] outTypesMax
//! @param[out] outTypesAvail
//! @param[out] outTypesLen
//! @param[out] outNodes
//! @param[out] outNodesLen
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doDescribeResources(ncMetadata * pMeta, virtualMachine ** ccvms, int vmLen, int **outTypesMax, int **outTypesAvail, int *outTypesLen, ccResource ** outNodes, int *outNodesLen)
{
    int i;
    int rc, diskpool, mempool, corepool;
    int j;
    ccResource *res;
    ccResourceCache resourceCacheLocal;

    LOGDEBUG("invoked: userId=%s, vmLen=%d\n", SP(pMeta ? pMeta->userId : "UNSET"), vmLen);

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    if (outTypesMax == NULL || outTypesAvail == NULL || outTypesLen == NULL || outNodes == NULL || outNodesLen == NULL) {
        // input error
        return (1);
    }

    *outTypesMax = NULL;
    *outTypesAvail = NULL;

    *outTypesMax = EUCA_ZALLOC(vmLen, sizeof(int));
    *outTypesAvail = EUCA_ZALLOC(vmLen, sizeof(int));
    if (*outTypesMax == NULL || *outTypesAvail == NULL) {
        LOGERROR("out of memory\n");
        unlock_exit(1);
    }

    *outTypesLen = vmLen;

    for (i = 0; i < vmLen; i++) {
        if ((*ccvms)[i].mem <= 0 || (*ccvms)[i].cores <= 0 || (*ccvms)[i].disk <= 0) {
            LOGERROR("input error\n");
            EUCA_FREE(*outTypesAvail);
            EUCA_FREE(*outTypesMax);
            *outTypesLen = 0;
            return (1);
        }
    }

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);
    {
        *outNodes = EUCA_ZALLOC(resourceCacheLocal.numResources, sizeof(ccResource));
        if (*outNodes == NULL) {
            LOGFATAL("out of memory!\n");
            unlock_exit(1);
        } else {
            memcpy(*outNodes, resourceCacheLocal.resources, sizeof(ccResource) * resourceCacheLocal.numResources);
            *outNodesLen = resourceCacheLocal.numResources;
        }

        for (i = 0; i < resourceCacheLocal.numResources; i++) {
            res = &(resourceCacheLocal.resources[i]);
            for (j = 0; j < vmLen; j++) {
                if (res->ncState == STOPPED) {
                    mempool = 0;
                    diskpool = 0;
                    corepool = 0;
                } else {
                    mempool = res->availMemory;
                    diskpool = res->availDisk;
                    corepool = res->availCores;
                }

                mempool -= (*ccvms)[j].mem;
                diskpool -= (*ccvms)[j].disk;
                corepool -= (*ccvms)[j].cores;
                while (mempool >= 0 && diskpool >= 0 && corepool >= 0) {
                    (*outTypesAvail)[j]++;
                    mempool -= (*ccvms)[j].mem;
                    diskpool -= (*ccvms)[j].disk;
                    corepool -= (*ccvms)[j].cores;
                }

                if (res->ncState == STOPPED) {
                    mempool = 0;
                    diskpool = 0;
                    corepool = 0;
                } else {
                    mempool = res->maxMemory;
                    diskpool = res->maxDisk;
                    corepool = res->maxCores;
                }

                mempool -= (*ccvms)[j].mem;
                diskpool -= (*ccvms)[j].disk;
                corepool -= (*ccvms)[j].cores;
                while (mempool >= 0 && diskpool >= 0 && corepool >= 0) {
                    (*outTypesMax)[j]++;
                    mempool -= (*ccvms)[j].mem;
                    diskpool -= (*ccvms)[j].disk;
                    corepool -= (*ccvms)[j].cores;
                }
            }
        }
    }

    if (vmLen >= 5) {
        LOGDEBUG("resources summary ({avail/max}): %s{%d/%d} %s{%d/%d} %s{%d/%d} %s{%d/%d} %s{%d/%d}\n", (*ccvms)[0].name,
                 (*outTypesAvail)[0], (*outTypesMax)[0], (*ccvms)[1].name, (*outTypesAvail)[1], (*outTypesMax)[1], (*ccvms)[2].name,
                 (*outTypesAvail)[2], (*outTypesMax)[2], (*ccvms)[3].name, (*outTypesAvail)[3], (*outTypesMax)[3], (*ccvms)[4].name, (*outTypesAvail)[4], (*outTypesMax)[4]);
    }

    LOGTRACE("done\n");

    shawn();

    return (0);
}

//!
//!
//!
//! @param[in] in
//! @param[in] newstate
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int changeState(ccResource * in, int newstate)
{
    if (in == NULL)
        return (1);
    if (in->state == newstate)
        return (0);

    in->lastState = in->state;
    in->state = newstate;
    in->stateChange = time(NULL);
    in->idleStart = 0;

    return (0);
}

int broadcast_network_info(ncMetadata * pMeta, int timeout, int dolock)
{
    int i = 0;
    int rc = 0;
    int pid = 0;
    int *pids = NULL;
    int status = 0;
    time_t op_start = { 0 };
    char *networkInfo = NULL;

    if (timeout <= 0)
        timeout = 1;

    op_start = time(NULL);
    LOGDEBUG("invoked: timeout=%d, dolock=%d\n", timeout, dolock);

    // critical NC call section
    sem_mywait(RESCACHE);
    memcpy(resourceCacheStage, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    sem_close(locks[REFRESHLOCK]);
    locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);

    pids = EUCA_ZALLOC(resourceCacheStage->numResources, sizeof(int));
    if (!pids) {
        LOGFATAL("out of memory!\n");
        unlock_exit(1);
    }
    // here is where we will convert the globalnetworkinfo into the broadcast string
    sem_mywait(GLOBALNETWORKINFO);
    networkInfo = strdup(globalnetworkinfo->networkInfo);
    sem_mypost(GLOBALNETWORKINFO);

    for (i = 0; i < resourceCacheStage->numResources; i++) {
        sem_mywait(REFRESHLOCK);

        pid = fork();
        if (!pid) {
            // do the broadcast
            rc = ncClientCall(pMeta, 0, resourceCacheStage->resources[i].lockidx, resourceCacheStage->resources[i].ncURL, "ncBroadcastNetworkInfo", networkInfo);

            if (rc != 0) {
                LOGERROR("bad return from ncDescribeResource(%s) (%d)\n", resourceCacheStage->resources[i].hostname, rc);
            }

            sem_mypost(REFRESHLOCK);
            exit(0);
        } else {
            pids[i] = pid;
        }
    }

    // free the broadcast string
    EUCA_FREE(networkInfo);

    for (i = 0; i < resourceCacheStage->numResources; i++) {
        rc = timewait(pids[i], &status, 120);
        if (!rc) {
            // timed out, really bad failure (reset REFRESHLOCK semaphore)
            sem_close(locks[REFRESHLOCK]);
            locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);
            rc = 1;
        } else if (rc > 0) {
            // process exited, and wait picked it up.
            if (WIFEXITED(status)) {
                rc = WEXITSTATUS(status);
            } else {
                rc = 1;
            }
        } else {
            // process no longer exists, and someone else reaped it
            rc = 0;
        }
        if (rc) {
            LOGWARN("error waiting for child pid '%d', exit code '%d'\n", pids[i], rc);
        }
    }

    sem_mywait(RESCACHE);
    memcpy(resourceCache, resourceCacheStage, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    EUCA_FREE(pids);
    LOGTRACE("done\n");
    return (0);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] timeout
//! @param[in] dolock
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int refresh_resources(ncMetadata * pMeta, int timeout, int dolock)
{
    int i, rc, nctimeout, pid, *pids = NULL;
    int status;
    time_t op_start;
    ncResource *ncResDst = NULL;

    if (timeout <= 0)
        timeout = 1;

    op_start = time(NULL);
    LOGDEBUG("invoked: timeout=%d, dolock=%d\n", timeout, dolock);

    // critical NC call section
    sem_mywait(RESCACHE);
    memcpy(resourceCacheStage, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    sem_close(locks[REFRESHLOCK]);
    locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);

    pids = EUCA_ZALLOC(resourceCacheStage->numResources, sizeof(int));
    if (!pids) {
        LOGFATAL("out of memory!\n");
        unlock_exit(1);
    }

    for (i = 0; i < resourceCacheStage->numResources; i++) {
        sem_mywait(REFRESHLOCK);

        pid = fork();
        if (!pid) {
            ncResDst = NULL;
            if (resourceCacheStage->resources[i].state != RESASLEEP && resourceCacheStage->resources[i].running == 0) {
                nctimeout = ncGetTimeout(op_start, timeout, 1, 1);
                char *errMsg = NULL;
                rc = ncClientCall(pMeta, nctimeout, resourceCacheStage->resources[i].lockidx, resourceCacheStage->resources[i].ncURL,
                                  "ncDescribeResource", NULL, &ncResDst, &errMsg);
                if (rc != 0) {
                    powerUp(&(resourceCacheStage->resources[i]));

                    if (resourceCacheStage->resources[i].state == RESWAKING && ((time(NULL) - resourceCacheStage->resources[i].stateChange) < config->wakeThresh)) {
                        LOGDEBUG("resource still waking up (%ld more seconds until marked as down)\n",
                                 config->wakeThresh - (time(NULL) - resourceCacheStage->resources[i].stateChange));
                    } else {
                        LOGERROR("bad return from ncDescribeResource(%s) (%d)\n", resourceCacheStage->resources[i].hostname, rc);
                        resourceCacheStage->resources[i].maxMemory = 0;
                        resourceCacheStage->resources[i].availMemory = 0;
                        resourceCacheStage->resources[i].maxDisk = 0;
                        resourceCacheStage->resources[i].availDisk = 0;
                        resourceCacheStage->resources[i].maxCores = 0;
                        resourceCacheStage->resources[i].availCores = 0;
                        changeState(&(resourceCacheStage->resources[i]), RESDOWN);
                        resourceCacheStage->resources[i].ncState = NOTREADY;
                        resourceCacheStage->resources[i].migrationCapable = FALSE;
                        euca_strncpy(resourceCacheStage->resources[i].nodeMessage, SP(errMsg), 1024);
                        LOGERROR("error message from ncDescribeResource: %s\n", resourceCacheStage->resources[i].nodeMessage);
                    }
                } else {
                    LOGDEBUG("received data from node=%s status=%s mem=%d/%d disk=%d/%d cores=%d/%d migrationCapable=%s\n",
                             resourceCacheStage->resources[i].hostname,
                             ncResDst->nodeStatus,
                             ncResDst->memorySizeAvailable, ncResDst->memorySizeMax,
                             ncResDst->diskSizeAvailable, ncResDst->diskSizeMax, ncResDst->numberOfCoresAvailable, ncResDst->numberOfCoresMax,
                             (ncResDst->migrationCapable == TRUE) ? "TRUE" : "FALSE");
                    resourceCacheStage->resources[i].maxMemory = ncResDst->memorySizeMax;
                    resourceCacheStage->resources[i].availMemory = ncResDst->memorySizeAvailable;
                    resourceCacheStage->resources[i].maxDisk = ncResDst->diskSizeMax;
                    resourceCacheStage->resources[i].availDisk = ncResDst->diskSizeAvailable;
                    resourceCacheStage->resources[i].maxCores = ncResDst->numberOfCoresMax;
                    resourceCacheStage->resources[i].availCores = ncResDst->numberOfCoresAvailable;
                    if (!strcmp(ncResDst->nodeStatus, "enabled")) {
                        resourceCacheStage->resources[i].ncState = ENABLED;
                    } else if (!strcmp(ncResDst->nodeStatus, "disabled")) {
                        resourceCacheStage->resources[i].ncState = STOPPED;
                    }
                    resourceCacheStage->resources[i].migrationCapable = ncResDst->migrationCapable;
                    euca_strncpy(resourceCacheStage->resources[i].nodeStatus, ncResDst->nodeStatus, 24);
////                    // temporarily duplicate the NC reported value in the node message for debugging
                    strcpy(resourceCacheStage->resources[i].nodeMessage, "");
                    // set iqn, if set
                    if (strlen(ncResDst->iqn)) {
                        snprintf(resourceCacheStage->resources[i].iqn, 128, "%s", ncResDst->iqn);
                    }

                    changeState(&(resourceCacheStage->resources[i]), RESUP);
                }
                if (errMsg != NULL) {
                    EUCA_FREE(errMsg);
                }
            } else {
                LOGDEBUG("resource asleep/running instances (%d), skipping resource update\n", resourceCacheStage->resources[i].running);
            }

            // try to discover the mac address of the resource
            if (resourceCacheStage->resources[i].mac[0] == '\0' && resourceCacheStage->resources[i].ip[0] != '\0') {
                char *mac;
                rc = ip2mac(vnetconfig, resourceCacheStage->resources[i].ip, &mac);
                if (!rc) {
                    euca_strncpy(resourceCacheStage->resources[i].mac, mac, 24);
                    EUCA_FREE(mac);
                    LOGDEBUG("discovered MAC '%s' for host %s(%s)\n", resourceCacheStage->resources[i].mac,
                             resourceCacheStage->resources[i].hostname, resourceCacheStage->resources[i].ip);
                }
            }

            EUCA_FREE(ncResDst);
            sem_mypost(REFRESHLOCK);
            exit(0);
        } else {
            pids[i] = pid;
        }
    }

    for (i = 0; i < resourceCacheStage->numResources; i++) {
        rc = timewait(pids[i], &status, 120);
        if (!rc) {
            // timed out, really bad failure (reset REFRESHLOCK semaphore)
            sem_close(locks[REFRESHLOCK]);
            locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);
            rc = 1;
        } else if (rc > 0) {
            // process exited, and wait picked it up.
            if (WIFEXITED(status)) {
                rc = WEXITSTATUS(status);
            } else {
                rc = 1;
            }
        } else {
            // process no longer exists, and someone else reaped it
            rc = 0;
        }
        if (rc) {
            LOGWARN("error waiting for child pid '%d', exit code '%d'\n", pids[i], rc);
        }
    }

    sem_mywait(RESCACHE);
    memcpy(resourceCache, resourceCacheStage, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    EUCA_FREE(pids);
    LOGTRACE("done\n");
    return (0);
}

//!
//! @param[in] myInstance instance to check for migration
//! @param[in] host reported hostname
//! @param[in] src source node for migration
//! @param[in] dst destination node for migration
//! @param[in] migration_state reported migration state
//! @param[out] node node to which to send migration action request
//! @param[out] instance
//! @param[out] action migration action to request of node
//!
//! @return EUCA_OK or EUCA
//!
//! @pre
//!
//! @note
//!
static int migration_handler(ccInstance * myInstance, char *host, char *src, char *dst, migration_states migration_state, char **node, char **instance, char **action)
{
    int rc = 0;

    LOGDEBUG("invoked\n");

    if (!strcmp(host, dst)) {
        if ((migration_state == MIGRATION_READY) && !strcmp(myInstance->state, "Extant")) {
            LOGDEBUG("[%s] destination node %s reports %s(%s), checking source node %s\n", myInstance->instanceId, host, myInstance->state,
                     migration_state_names[myInstance->migration_state], src);
            ccInstance *srcInstance = NULL;
            rc = find_instanceCacheId(myInstance->instanceId, &srcInstance);
            if (!rc) {
                if (srcInstance->migration_state == MIGRATION_READY) {
                    LOGINFO("[%s] source node %s last reported %s(%s), destination node %s reports %s(%s), preparing to commit migration\n", myInstance->instanceId, src,
                            srcInstance->state, migration_state_names[srcInstance->migration_state], dst, myInstance->state, migration_state_names[myInstance->migration_state]);
                    EUCA_FREE(*node);
                    EUCA_FREE(*instance);
                    EUCA_FREE(*action);
                    *node = strdup(src);
                    *instance = strdup(myInstance->instanceId);
                    *action = strdup("commit");
                } else if (srcInstance->migration_state == MIGRATION_IN_PROGRESS) {
                    LOGDEBUG("[%s] source node %s last reported migration to %s in progress\n", myInstance->instanceId, src, dst);
                } else if (srcInstance->migration_state == NOT_MIGRATING) {
                    LOGINFO("[%s] source node %s last reported %s(%s), destination node %s reports %s(%s), preparing to roll back destination node\n",
                            myInstance->instanceId, src, srcInstance->state, migration_state_names[srcInstance->migration_state], dst, myInstance->state,
                            migration_state_names[myInstance->migration_state]);
                    EUCA_FREE(*node);
                    EUCA_FREE(*instance);
                    EUCA_FREE(*action);
                    *node = strdup(dst);
                    *instance = strdup(myInstance->instanceId);
                    *action = strdup("rollback");
                } else {
                    LOGDEBUG("[%s] source node %s last reported %s(%s), has not yet reported ready to commit migration to %s\n",
                             myInstance->instanceId, src, srcInstance->state, migration_state_names[srcInstance->migration_state], dst);
                }
            } else {
                LOGERROR("[%s] could not find migration source node %s in the instance cache\n", myInstance->instanceId, src);
            }
            EUCA_FREE(srcInstance);
        } else if (((migration_state == MIGRATION_CLEANING) || (migration_state == MIGRATION_READY)) && !strcmp(myInstance->state, "Teardown")) {
            LOGDEBUG("[%s] destination node %s reports %s(%s), checking source node %s\n", myInstance->instanceId, host, myInstance->state,
                     migration_state_names[myInstance->migration_state], src);
            ccInstance *srcInstance = NULL;
            rc = find_instanceCacheId(myInstance->instanceId, &srcInstance);
            if (!rc) {
                // TO-DO: State matrix says also to roll back if source
                // is Extant(preparing), but that's causing some odd
                // (though benign?) effects right now. So only rolling
                // back if Extant(ready).
                // Part of the fix for this should be to cache (locally)
                // the scheduled destination node for the migration so
                // we can pass it when requesting a rollback to a source
                // that has not yet reported ready during a polling
                // cycle.
                if ((srcInstance->migration_state == MIGRATION_READY) && !strcmp(srcInstance->state, "Extant")) {
                    LOGINFO("[%s] source node %s last reported %s(%s), destination node %s reports %s(%s), preparing to roll back source node\n",
                            myInstance->instanceId, src, srcInstance->state, migration_state_names[srcInstance->migration_state], dst, myInstance->state,
                            migration_state_names[myInstance->migration_state]);
                    EUCA_FREE(*node);
                    EUCA_FREE(*instance);
                    EUCA_FREE(*action);
                    *node = strdup(src);
                    *instance = strdup(myInstance->instanceId);
                    *action = strdup("rollback");
                } else {
                    LOGDEBUG("[%s] source node %s last reported %s(%s), destination node %s reports %s(%s), doing nothing\n",
                             myInstance->instanceId, src, srcInstance->state, migration_state_names[srcInstance->migration_state], dst, myInstance->state,
                             migration_state_names[myInstance->migration_state]);
                }
            } else {
                LOGDEBUG("[%s] could not find migration source node %s in the instance cache\n", myInstance->instanceId, src);
            }
            EUCA_FREE(srcInstance);
        } else {
            LOGDEBUG("[%s] ignoring update from destination node %s during migration: %s(%s)\n", myInstance->instanceId, host, myInstance->state,
                     migration_state_names[myInstance->migration_state]);
        }
    } else if (!strcmp(host, src)) {
        LOGDEBUG("[%s] received state %s(%s) from source node %s\n", myInstance->instanceId, myInstance->state, migration_state_names[migration_state], host);
    } else {
        LOGERROR("[%s] received status from a migrating node that's neither the source (%s) nor the destination (%s): %s\n", myInstance->instanceId, src, dst, host);
    }
    LOGDEBUG("done\n");
    return (rc);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] timeout
//! @param[in] dolock
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int refresh_instances(ncMetadata * pMeta, int timeout, int dolock)
{
    ccInstance *myInstance = NULL;
    int i, numInsts = 0, found, ncOutInstsLen, rc, pid, nctimeout, *pids = NULL, status;
    time_t op_start;
    char *migration_host = NULL;
    char *migration_instance = NULL;
    char *migration_action = NULL;

    ncInstance **ncOutInsts = NULL;

    op_start = time(NULL);

    LOGDEBUG("invoked: timeout=%d, dolock=%d\n", timeout, dolock);
    set_clean_instanceCache();

    // critical NC call section
    sem_mywait(RESCACHE);
    memcpy(resourceCacheStage, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    sem_close(locks[REFRESHLOCK]);
    locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);

    pids = EUCA_ZALLOC(resourceCacheStage->numResources, sizeof(int));
    if (!pids) {
        LOGFATAL("out of memory!\n");
        unlock_exit(1);
    }

    invalidate_instanceCache();

    for (i = 0; i < resourceCacheStage->numResources; i++) {
        sem_mywait(REFRESHLOCK);
        pid = fork();
        if (!pid) {
            if (resourceCacheStage->resources[i].state == RESUP) {
                int j;

                nctimeout = ncGetTimeout(op_start, timeout, 1, 1);
                rc = ncClientCall(pMeta, nctimeout, resourceCacheStage->resources[i].lockidx, resourceCacheStage->resources[i].ncURL,
                                  "ncDescribeInstances", NULL, 0, &ncOutInsts, &ncOutInstsLen);
                if (!rc) {

                    // if idle, power down
                    if (ncOutInstsLen == 0) {
                        LOGDEBUG("node %s idle since %ld: (%ld/%d) seconds\n", resourceCacheStage->resources[i].hostname,
                                 resourceCacheStage->resources[i].idleStart, time(NULL) - resourceCacheStage->resources[i].idleStart, config->idleThresh);
                        if (!resourceCacheStage->resources[i].idleStart) {
                            resourceCacheStage->resources[i].idleStart = time(NULL);
                        } else if ((time(NULL) - resourceCacheStage->resources[i].idleStart) > config->idleThresh) {
                            // call powerdown

                            if (powerDown(pMeta, &(resourceCacheStage->resources[i]))) {
                                LOGWARN("powerDown for %s failed\n", resourceCacheStage->resources[i].hostname);
                            }
                        }
                    } else {
                        resourceCacheStage->resources[i].idleStart = 0;
                    }

                    // populate instanceCache
                    for (j = 0; j < ncOutInstsLen; j++) {
                        found = 1;
                        if (found) {
                            myInstance = NULL;
                            // add it
                            LOGDEBUG("describing instance %s, %s, %d\n", ncOutInsts[j]->instanceId, ncOutInsts[j]->stateName, j);
                            numInsts++;

                            // grab instance from cache, if available.  otherwise, start from scratch
                            rc = find_instanceCacheId(ncOutInsts[j]->instanceId, &myInstance);
                            if (rc || !myInstance) {
                                myInstance = EUCA_ZALLOC(1, sizeof(ccInstance));
                                if (!myInstance) {
                                    LOGFATAL("out of memory!\n");
                                    unlock_exit(1);
                                }
                            }
                            // update CC instance with instance state from NC
                            rc = ncInstance_to_ccInstance(myInstance, ncOutInsts[j]);

                            // migration-related logic
                            if (ncOutInsts[j]->migration_state != NOT_MIGRATING) {

                                rc = migration_handler(myInstance,
                                                       resourceCacheStage->resources[i].hostname,
                                                       ncOutInsts[j]->migration_src,
                                                       ncOutInsts[j]->migration_dst, ncOutInsts[j]->migration_state, &migration_host, &migration_instance, &migration_action);

                                // For now just ignore updates from destination while migrating.
                                if (!strcmp(resourceCacheStage->resources[i].hostname, ncOutInsts[j]->migration_dst)) {
                                    LOGTRACE("[%s] ignoring update from destination node %s during migration (host=%s, instance=%s, action=%s)\n",
                                             myInstance->instanceId, ncOutInsts[j]->migration_dst, SP(migration_host), SP(migration_instance), SP(migration_action));
                                    EUCA_FREE(myInstance);
                                    continue;
                                }
                            }
                            // instance info that the CC maintains
                            myInstance->ncHostIdx = i;

                            // Is this redundant?
                            myInstance->migration_state = ncOutInsts[j]->migration_state;

                            euca_strncpy(myInstance->serviceTag, resourceCacheStage->resources[i].ncURL, 384);
                            {
                                char *ip = NULL;
                                if (!strcmp(myInstance->ccnet.publicIp, "0.0.0.0")) {
                                    if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM) || !strcmp(vnetconfig->mode, NETMODE_STATIC)) {
                                        rc = mac2ip(vnetconfig, myInstance->ccnet.privateMac, &ip);
                                        if (!rc) {
                                            euca_strncpy(myInstance->ccnet.publicIp, ip, IP_BUFFER_SIZE);
                                        }
                                    }
                                }

                                EUCA_FREE(ip);
                                if (!strcmp(myInstance->ccnet.privateIp, "0.0.0.0")) {
                                    rc = mac2ip(vnetconfig, myInstance->ccnet.privateMac, &ip);
                                    if (!rc) {
                                        euca_strncpy(myInstance->ccnet.privateIp, ip, IP_BUFFER_SIZE);
                                    }
                                }

                                EUCA_FREE(ip);
                            }

                            if ((myInstance->ccnet.publicIp[0] != '\0' && strcmp(myInstance->ccnet.publicIp, "0.0.0.0"))
                                && (myInstance->ncnet.publicIp[0] == '\0' || !strcmp(myInstance->ncnet.publicIp, "0.0.0.0"))) {
                                // CC has network info, NC does not
                                LOGDEBUG("sending ncAssignAddress to sync NC\n");
                                rc = ncClientCall(pMeta, nctimeout, resourceCacheStage->resources[i].lockidx, resourceCacheStage->resources[i].ncURL,
                                                  "ncAssignAddress", myInstance->instanceId, myInstance->ccnet.publicIp);
                                if (rc) {
                                    // problem, but will retry next time
                                    LOGWARN("could not send AssignAddress to NC\n");
                                }
                            }

                            refresh_instanceCache(myInstance->instanceId, myInstance);
                            if (!strcmp(myInstance->state, "Extant")) {
                                if (myInstance->ccnet.vlan < 0) {
                                    vnetEnableHost(vnetconfig, myInstance->ccnet.privateMac, myInstance->ccnet.privateIp, 0);
                                } else {
                                    vnetEnableHost(vnetconfig, myInstance->ccnet.privateMac, myInstance->ccnet.privateIp, myInstance->ccnet.vlan);
                                }
                            }
                            LOGDEBUG("storing instance state: %s/%s/%s/%s\n", myInstance->instanceId, myInstance->state, myInstance->ccnet.publicIp, myInstance->ccnet.privateIp);
                            print_ccInstance("refresh_instances(): ", myInstance);
                            sensor_set_resource_alias(myInstance->instanceId, myInstance->ncnet.privateIp);
                            EUCA_FREE(myInstance);
                        }

                    }
                }
                if (ncOutInsts) {
                    for (j = 0; j < ncOutInstsLen; j++) {
                        free_instance(&(ncOutInsts[j]));
                    }
                    EUCA_FREE(ncOutInsts);
                }
            }
            sem_mypost(REFRESHLOCK);

            if (migration_host) {
                if (!strcmp(migration_action, "commit")) {
                    LOGDEBUG("[%s] notifying source %s to commit migration\n", migration_instance, migration_host);
                    // Note: Really only need to specify the instance here.
                    doMigrateInstances(pMeta, migration_host, migration_instance, NULL, 0, 0, "commit");
                } else if (!strcmp(migration_action, "rollback")) {
                    LOGDEBUG("[%s] notifying node %s to roll back migration\n", migration_instance, migration_host);
                    doMigrateInstances(pMeta, migration_host, migration_instance, NULL, 0, 0, "rollback");
                } else {
                    LOGWARN("unexpected migration action '%s' for node %s -- doing nothing\n", migration_action, migration_host);
                }
                EUCA_FREE(migration_host);
            }
            EUCA_FREE(migration_instance);
            EUCA_FREE(migration_action);

            exit(0);
        } else {
            pids[i] = pid;
        }
    }

    for (i = 0; i < resourceCacheStage->numResources; i++) {
        rc = timewait(pids[i], &status, 120);
        if (!rc) {
            // timed out, really bad failure (reset REFRESHLOCK semaphore)
            sem_close(locks[REFRESHLOCK]);
            locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);
            rc = 1;
        } else if (rc > 0) {
            // process exited, and wait picked it up.
            if (WIFEXITED(status)) {
                rc = WEXITSTATUS(status);
            } else {
                rc = 1;
            }
        } else {
            // process no longer exists, and someone else reaped it
            rc = 0;
        }
        if (rc) {
            LOGWARN("error waiting for child pid '%d', exit code '%d'\n", pids[i], rc);
        }
    }

    invalidate_instanceCache();

    sem_mywait(RESCACHE);
    memcpy(resourceCache, resourceCacheStage, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    EUCA_FREE(pids);

    LOGTRACE("done\n");
    return (0);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] timeout
//! @param[in] dolock
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int refresh_sensors(ncMetadata * pMeta, int timeout, int dolock)
{

    time_t op_start = time(NULL);
    LOGDEBUG("invoked: timeout=%d, dolock=%d\n", timeout, dolock);

    int history_size;
    long long collection_interval_time_ms;
    if ((sensor_get_config(&history_size, &collection_interval_time_ms) != 0) || history_size < 1 || collection_interval_time_ms == 0)
        return (1);                    // sensor system not configured yet

    // critical NC call section
    sem_mywait(RESCACHE);
    memcpy(resourceCacheStage, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    sem_close(locks[REFRESHLOCK]);
    locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);

    int *pids = EUCA_ZALLOC(resourceCacheStage->numResources, sizeof(int));
    if (!pids) {
        LOGFATAL("out of memory!\n");
        unlock_exit(1);
    }

    for (int i = 0; i < resourceCacheStage->numResources; i++) {
        sem_mywait(REFRESHLOCK);
        pid_t pid = fork();
        if (!pid) {
            if (resourceCacheStage->resources[i].state == RESUP) {
                int nctimeout = ncGetTimeout(op_start, timeout, 1, 1);

                sensorResource **srs;
                int srsLen;
                int rc = ncClientCall(pMeta, nctimeout, resourceCacheStage->resources[i].lockidx, resourceCacheStage->resources[i].ncURL,
                                      "ncDescribeSensors", history_size, collection_interval_time_ms,
                                      NULL, 0, NULL, 0, &srs, &srsLen);

                if (!rc) {
                    // update our cache
                    if (sensor_merge_records(srs, srsLen, TRUE) != EUCA_OK) {
                        LOGWARN("failed to store all sensor data due to lack of space");
                    }

                    if (srsLen > 0) {
                        for (int j = 0; j < srsLen; j++) {
                            EUCA_FREE(srs[j]);
                        }
                        EUCA_FREE(srs);
                    }
                }
            }
            sem_mypost(REFRESHLOCK);
            exit(0);
        } else {
            pids[i] = pid;
        }
    }

    for (int i = 0; i < resourceCacheStage->numResources; i++) {
        int status;

        int rc = timewait(pids[i], &status, 120);
        if (!rc) {
            // timed out, really bad failure (reset REFRESHLOCK semaphore)
            sem_close(locks[REFRESHLOCK]);
            locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);
            rc = 1;
        } else if (rc > 0) {
            // process exited, and wait picked it up.
            if (WIFEXITED(status)) {
                rc = WEXITSTATUS(status);
            } else {
                rc = 1;
            }
        } else {
            // process no longer exists, and someone else reaped it
            rc = 0;
        }
        if (rc) {
            LOGWARN("error waiting for child pid '%d', exit code '%d'\n", pids[i], rc);
        }
    }

    sem_mywait(RESCACHE);
    memcpy(resourceCache, resourceCacheStage, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    EUCA_FREE(pids);
    LOGTRACE("done\n");
    return (0);
}

//!
//!
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instIds
//! @param[in]  instIdsLen
//! @param[out] outInsts
//! @param[out] outInstsLen
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doDescribeInstances(ncMetadata * pMeta, char **instIds, int instIdsLen, ccInstance ** outInsts, int *outInstsLen)
{
    int i, rc, count;
    time_t op_start;

    LOGDEBUG("invoked: userId=%s, instIdsLen=%d\n", SP(pMeta ? pMeta->userId : "UNSET"), instIdsLen);

    op_start = time(NULL);

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    *outInsts = NULL;
    *outInstsLen = 0;

    sem_mywait(INSTCACHE);
    count = 0;
    if (instanceCache->numInsts) {
        *outInsts = EUCA_ZALLOC(instanceCache->numInsts, sizeof(ccInstance));
        if (!*outInsts) {
            LOGFATAL("out of memory!\n");
            unlock_exit(1);
        }

        for (i = 0; i < MAXINSTANCES_PER_CC; i++) {
            if (instanceCache->cacheState[i] == INSTVALID) {
                if (count >= instanceCache->numInsts) {
                    LOGWARN("found more instances than reported by numInsts, will only report a subset of instances\n");
                    count = 0;
                }
                memcpy(&((*outInsts)[count]), &(instanceCache->instances[i]), sizeof(ccInstance));
                // We only report a subset of possible migration statuses upstream to the CLC.
                if ((*outInsts)[count].migration_state == MIGRATION_READY) {
                    (*outInsts)[count].migration_state = MIGRATION_PREPARING;
                } else if ((*outInsts)[count].migration_state == MIGRATION_CLEANING) {
                    (*outInsts)[count].migration_state = MIGRATION_IN_PROGRESS;
                }
                count++;
            }
        }

        *outInstsLen = instanceCache->numInsts;
    }
    sem_mypost(INSTCACHE);

    for (i = 0; i < (*outInstsLen); i++) {
        LOGDEBUG("instances summary: instanceId=%s, state=%s, migration_state=%s, publicIp=%s, privateIp=%s\n", (*outInsts)[i].instanceId,
                 (*outInsts)[i].state, migration_state_names[(*outInsts)[i].migration_state], (*outInsts)[i].ccnet.publicIp, (*outInsts)[i].ccnet.privateIp);
    }

    LOGTRACE("done\n");

    shawn();

    return (0);
}

//!
//!
//!
//! @param[in] res
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int powerUp(ccResource * res)
{
    int i = 0;
    int len = 0;
    int ret = EUCA_OK;
    char *bc = NULL;
    char rootwrap[EUCA_MAX_PATH] = "";
    uint32_t *ips = NULL;
    uint32_t *nms = NULL;

    if (config->schedPolicy != SCHEDPOWERSAVE) {
        return (0);
    }

    if ((ret = getdevinfo(vnetconfig->privInterface, &ips, &nms, &len)) != EUCA_OK) {
        if ((ips = EUCA_ZALLOC(1, sizeof(uint32_t))) == NULL) {
            LOGFATAL("out of memory!\n");
            unlock_exit(1);
        }

        if ((nms = EUCA_ZALLOC(1, sizeof(uint32_t))) == NULL) {
            LOGFATAL("out of memory!\n");
            unlock_exit(1);
        }

        ips[0] = 0xFFFFFFFF;
        nms[0] = 0xFFFFFFFF;
        len = 1;
    }

    snprintf(rootwrap, sizeof(rootwrap), EUCALYPTUS_ROOTWRAP, vnetconfig->eucahome);
    for (i = 0; i < len; i++) {
        LOGDEBUG("attempting to wake up resource %s(%s/%s)\n", res->hostname, res->ip, res->mac);
        // try to wake up res

        // broadcast
        bc = hex2dot((0xFFFFFFFF - nms[i]) | (ips[i] & nms[i]));

        ret = EUCA_ERROR;
        if (strcmp(res->mac, "00:00:00:00:00:00")) {
            LOGINFO("waking up powered off host %s(%s/%s): '%s powerwake -b %s %s'\n", res->hostname, res->ip, res->mac, rootwrap, bc, res->mac);
            if ((ret = euca_execlp(NULL, rootwrap, "powerwake", "-b", bc, res->mac, NULL)) == EUCA_OK) {
                changeState(res, RESWAKING);
            } else {
                LOGERROR("Failed to execute '%s powerwake -b %s %s", rootwrap, bc, res->mac);
            }
        } else if (strcmp(res->ip, "0.0.0.0")) {
            LOGINFO("waking up powered off host %s(%s/%s): '%s powerwake -b %s %s'\n", res->hostname, res->ip, res->mac, rootwrap, bc, res->ip);
            if ((ret = euca_execlp(NULL, rootwrap, "powerwake", "-b", bc, res->ip, NULL)) == EUCA_OK) {
                changeState(res, RESWAKING);
            } else {
                LOGERROR("Failed to execute '%s powerwake -b %s %s", rootwrap, bc, res->ip);
            }
        }
        EUCA_FREE(bc);
    }

    EUCA_FREE(ips);
    EUCA_FREE(nms);
    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] node
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int powerDown(ncMetadata * pMeta, ccResource * node)
{
    int rc, timeout;
    time_t op_start;

    if (config->schedPolicy != SCHEDPOWERSAVE) {
        node->idleStart = 0;
        return (0);
    }

    op_start = time(NULL);

    LOGINFO("powerdown to %s\n", node->hostname);

    timeout = ncGetTimeout(op_start, OP_TIMEOUT, 1, 1);
    rc = ncClientCall(pMeta, timeout, node->lockidx, node->ncURL, "ncPowerDown");

    if (rc == 0) {
        changeState(node, RESASLEEP);
    }
    return (rc);
}

//!
//!
//!
//! @param[in] prestr
//! @param[in] in
//!
//! @pre
//!
//! @note
//!
void print_netConfig(char *prestr, netConfig * in)
{
    LOGDEBUG("%s: vlan:%d networkIndex:%d privateMac:%s publicIp:%s privateIp:%s\n", prestr, in->vlan, in->networkIndex, in->privateMac, in->publicIp, in->privateIp);
}

//!
//!
//!
//! @param[in] dst
//! @param[in] src
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int ncInstance_to_ccInstance(ccInstance * dst, ncInstance * src)
{
    int i;

    euca_strncpy(dst->uuid, src->uuid, 48);
    euca_strncpy(dst->instanceId, src->instanceId, 16);
    euca_strncpy(dst->reservationId, src->reservationId, 16);
    euca_strncpy(dst->accountId, src->accountId, 48);
    euca_strncpy(dst->ownerId, src->ownerId, 48);
    euca_strncpy(dst->amiId, src->imageId, 16);
    euca_strncpy(dst->kernelId, src->kernelId, 16);
    euca_strncpy(dst->ramdiskId, src->ramdiskId, 16);
    euca_strncpy(dst->keyName, src->keyName, 1024);
    euca_strncpy(dst->launchIndex, src->launchIndex, 64);
    euca_strncpy(dst->platform, src->platform, 64);
    euca_strncpy(dst->guestStateName, src->guestStateName, 64);
    euca_strncpy(dst->bundleTaskStateName, src->bundleTaskStateName, 64);
    euca_strncpy(dst->createImageTaskStateName, src->createImageTaskStateName, 64);
    euca_strncpy(dst->userData, src->userData, 16384);
    euca_strncpy(dst->state, src->stateName, 16);
    euca_strncpy(dst->migration_src, src->migration_src, HOSTNAME_SIZE);
    euca_strncpy(dst->migration_dst, src->migration_dst, HOSTNAME_SIZE);
    dst->ts = src->launchTime;
    dst->migration_state = src->migration_state;

    memcpy(&(dst->ncnet), &(src->ncnet), sizeof(netConfig));

    for (i = 0; i < src->groupNamesSize && i < 64; i++) {
        snprintf(dst->groupNames[i], 64, "%s", src->groupNames[i]);
    }

    memcpy(dst->volumes, src->volumes, sizeof(ncVolume) * EUCA_MAX_VOLUMES);
    dst->volumesSize = 0;
    for (i = 0; i < EUCA_MAX_VOLUMES; i++) {
        if (strlen(dst->volumes[i].volumeId) == 0)
            break;
        dst->volumesSize++;
    }

    memcpy(&(dst->ccvm), &(src->params), sizeof(virtualMachine));

    dst->blkbytes = src->blkbytes;
    dst->netbytes = src->netbytes;

    return (0);
}

//!
//!
//!
//! @param[in] dst
//! @param[in] src
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int ccInstance_to_ncInstance(ncInstance * dst, ccInstance * src)
{
    int i;

    euca_strncpy(dst->uuid, src->uuid, 48);
    euca_strncpy(dst->instanceId, src->instanceId, 16);
    euca_strncpy(dst->reservationId, src->reservationId, 16);
    euca_strncpy(dst->accountId, src->accountId, 48);
    euca_strncpy(dst->userId, src->ownerId, 48);    //! @TODO: is this right?
    euca_strncpy(dst->ownerId, src->ownerId, 48);
    euca_strncpy(dst->imageId, src->amiId, 16);
    euca_strncpy(dst->kernelId, src->kernelId, 16);
    euca_strncpy(dst->ramdiskId, src->ramdiskId, 16);
    euca_strncpy(dst->keyName, src->keyName, 1024);
    euca_strncpy(dst->launchIndex, src->launchIndex, 64);
    euca_strncpy(dst->platform, src->platform, 64);
    euca_strncpy(dst->guestStateName, src->guestStateName, 64);
    euca_strncpy(dst->bundleTaskStateName, src->bundleTaskStateName, 64);
    euca_strncpy(dst->createImageTaskStateName, src->createImageTaskStateName, 64);
    euca_strncpy(dst->userData, src->userData, 16384);
    euca_strncpy(dst->stateName, src->state, 16);
    euca_strncpy(dst->migration_src, src->migration_src, HOSTNAME_SIZE);
    euca_strncpy(dst->migration_dst, src->migration_dst, HOSTNAME_SIZE);
    dst->launchTime = src->ts;
    dst->migration_state = src->migration_state;

    memcpy(&(dst->ncnet), &(src->ncnet), sizeof(netConfig));

    for (i = 0; i < 64; i++) {
        snprintf(dst->groupNames[i], 64, "%s", src->groupNames[i]);
    }

    memcpy(dst->volumes, src->volumes, sizeof(ncVolume) * EUCA_MAX_VOLUMES);
    for (i = 0; i < EUCA_MAX_VOLUMES; i++) {
        if (strlen(dst->volumes[i].volumeId) == 0)
            break;
    }

    memcpy(&(dst->params), &(src->ccvm), sizeof(virtualMachine));

    dst->blkbytes = src->blkbytes;
    dst->netbytes = src->netbytes;

    return (0);
}

//!
//!
//!
//! @param[in]  vm
//! @param[in]  amiId
//! @param[in]  kernelId
//! @param[in]  ramdiskId
//! @param[in]  instId
//! @param[in]  userData
//! @param[in]  platform
//! @param[in]  targetNode
//! @param[out] outresid
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int schedule_instance(virtualMachine * vm, char *amiId, char *kernelId, char *ramdiskId, char *instId, char *userData, char *platform, char *targetNode, int *outresid)
{
    int ret;

    if (targetNode != NULL) {
        ret = schedule_instance_explicit(vm, targetNode, outresid, FALSE);
    } else if (config->schedPolicy == SCHEDGREEDY) {
        ret = schedule_instance_greedy(vm, outresid);
    } else if (config->schedPolicy == SCHEDROUNDROBIN) {
        ret = schedule_instance_roundrobin(vm, outresid);
    } else if (config->schedPolicy == SCHEDPOWERSAVE) {
        ret = schedule_instance_greedy(vm, outresid);
    } else if (config->schedPolicy == SCHEDUSER) {
        ret = schedule_instance_user(vm, amiId, kernelId, ramdiskId, instId, userData, platform, outresid);
    } else {
        ret = schedule_instance_greedy(vm, outresid);
    }

    return (ret);
}

//!
//!
//!
//! @param[in]  vm
//! @param[out] outresid
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int schedule_instance_roundrobin(virtualMachine * vm, int *outresid)
{
    int i, done, start, found, resid = 0;
    ccResource *res;

    *outresid = 0;

    LOGDEBUG("scheduler using ROUNDROBIN policy to find next resource\n");
    // find the best 'resource' on which to run the instance
    done = found = 0;
    start = config->schedState;
    i = start;

    LOGDEBUG("scheduler state starting at resource %d\n", config->schedState);
    while (!done) {
        int mem, disk, cores;

        res = &(resourceCache->resources[i]);
        if ((res->state != RESDOWN) && (res->ncState == ENABLED)) {
            mem = res->availMemory - vm->mem;
            disk = res->availDisk - vm->disk;
            cores = res->availCores - vm->cores;

            if (mem >= 0 && disk >= 0 && cores >= 0) {
                resid = i;
                found = 1;
                done++;
            }
        }
        i++;
        if (i >= resourceCache->numResources) {
            i = 0;
        }
        if (i == start) {
            done++;
        }
    }

    if (!found) {
        // didn't find a resource
        return (1);
    }

    *outresid = resid;
    config->schedState = i;

    LOGDEBUG("scheduler state finishing at resource %d\n", config->schedState);

    return (0);
}

//!
//! @param[in]  instance           instance to migrate
//! @param[in]  includeNodes       hosts to be included as possible migration destinations
//! @param[in]  excludeNodes       hosts to be excluded as migration destinations
//! @param[in]  includeNodeCount   number of host entries in destination-inclusion list
//! @param[in]  excludeNodeCount   number of host entries in destination-exclusion list
//! @param[in]  inresid            resource-cache index of migration source node
//! @param[out] outresid           resource-cache index of scheduled migration destination node
//! @param[in]  resourceCacheLocal local copy of global resource cache
//! @param[out] replyString
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static int schedule_instance_migration(ncInstance * instance, char **includeNodes, char **excludeNodes, int includeNodeCount, int excludeNodeCount, int inresid, int *outresid,
                                       ccResourceCache * resourceCacheLocal, char **replyString)
{
    int ret = 0;

    LOGDEBUG("invoked: include=%d, exclude=%d\n", includeNodeCount, excludeNodeCount);

    if (includeNodes && excludeNodes) {
        LOGERROR("[%s] migration scheduler cannot be called with both nodes to include and nodes to exclude; the options are mutually exclusive.\n", instance->instanceId);
        ret = 1;
        goto out;
    }
    // Trivial case: migration to a specific node:
    if (includeNodeCount == 1) {
        LOGINFO("[%s] attempting to schedule migration to specific node: %s\n", instance->instanceId, includeNodes[0]);
        if (!strcmp(instance->migration_src, includeNodes[0])) {
            LOGERROR("[%s] cannot schedule SAME-NODE migration from %s to %s\n", instance->instanceId, instance->migration_src, includeNodes[0]);
            *replyString = strdup("source and destination cannot be the same");
            ret = 1;
            goto out;
        }
        ret = schedule_instance_explicit(&(instance->params), includeNodes[0], outresid, TRUE);
        if (resourceCacheLocal->resources[*outresid].migrationCapable == FALSE) {
            LOGWARN("[%s] cannot schedule migration to node (%s) that is not migration capable\n", instance->instanceId, includeNodes[0]);
            *replyString = strdup("requested destination is not migration capable");
            ret = 1;
            goto out;
        }
    } else {
        if (config->schedPolicy == SCHEDROUNDROBIN) {
            LOGDEBUG("[%s] scheduling migration using ROUNDROBIN scheduler\n", instance->instanceId);
        } else if (config->schedPolicy == SCHEDGREEDY || config->schedPolicy == SCHEDPOWERSAVE) {
            LOGINFO
                ("[%s] scheduling migration using ROUNDROBIN scheduler, despite GREEDY or POWERSAVE scheduler specification in Eucalyptus configuration file; GREEDY scheduling can be emulated by selecting specific destination nodes for migrations\n",
                 instance->instanceId);
        } else {
            LOGWARN("[%s] unsupported scheduler configuration--scheduling migration using ROUNDROBIN scheduler\n", instance->instanceId);
        }
        // This is relatively easy: we can keep calling the round-robin scheduler until we get a node we like.
        int first_try = -1;            // To break loops.
        int done = 0;
        int found = 0;
        while (!done) {
            ret = schedule_instance_roundrobin(&(instance->params), outresid);

            if (first_try == -1) {
                first_try = *outresid;
            } else if (*outresid == first_try) {
                LOGERROR("[%s] has looped around without scheduling a destination, breaking loop\n", instance->instanceId);
                // We've already been here. We know this one won't work.
                //done++;
                break;
            }

            if (*outresid == inresid) {
                // Tried to schduled to the source node, so retry.
                LOGDEBUG("[%s] cannot schedule src_index=%d == dst_index=%d (%s > %s), trying again...\n",
                         instance->instanceId, inresid, *outresid, instance->migration_src, resourceCacheLocal->resources[*outresid].hostname);
            } else if (resourceCacheLocal->resources[*outresid].migrationCapable == FALSE) {
                LOGDEBUG("[%s] cannot schedule src_index=%d, dst_index=%d because node %s is not migration capable\n", instance->instanceId, inresid, *outresid,
                         resourceCacheLocal->resources[*outresid].hostname);
            } else if (check_for_string_in_list(resourceCacheLocal->resources[*outresid].hostname, excludeNodes, excludeNodeCount)) {
                // Exclusion list takes priority over inclusion list.
                LOGDEBUG("[%s] cannot schedule src_index=%d, dst_index=%d because node %s is in destination-exclusion list\n",
                         instance->instanceId, inresid, *outresid, resourceCacheLocal->resources[*outresid].hostname);
            } else if (includeNodeCount) {
                if (!check_for_string_in_list(resourceCacheLocal->resources[*outresid].hostname, includeNodes, includeNodeCount)) {
                    LOGDEBUG("[%s] cannot schedule src_index=%d, dst_index=%d because node %s is not in destination-inclusion list\n",
                             instance->instanceId, inresid, *outresid, resourceCacheLocal->resources[*outresid].hostname);
                } else {
                    LOGDEBUG("[%s] scheduled: src_index=%d, dst_index=%d (%s > %s) -- destination node is in inclusion list\n",
                             instance->instanceId, inresid, *outresid, resourceCacheLocal->resources[*outresid].hostname, instance->migration_src);
                    done++;
                    found++;
                }
            } else if (*outresid != inresid) {
                // Found a destination node that's not the source node.
                LOGDEBUG("[%s] scheduled: src_index=%d, dst_index=%d (%s > %s)\n", instance->instanceId, inresid, *outresid, instance->migration_src,
                         resourceCacheLocal->resources[*outresid].hostname);
                done++;
                found++;
            }
        }
        if (!found) {
            ret = 1;
        }
    }

out:
    if (ret) {
        LOGERROR("[%s] migration scheduler could not schedule destination node\n", instance->instanceId);
        *replyString = strdup("not enough resources available for migration");
        *outresid = -1;
    }

    LOGDEBUG("done\n");

    return (ret);
}

//!
//!
//!
//! @param[in]  vm
//! @param[in]  targetNode
//! @param[out] outresid
//! @param[in]  is_migration
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int schedule_instance_explicit(virtualMachine * vm, char *targetNode, int *outresid, boolean is_migration)
{
    int i, done, resid, sleepresid;
    ccResource *res;

    *outresid = 0;

    LOGDEBUG("scheduler using EXPLICIT policy to run VM on target node '%s'\n", targetNode);

    // find the best 'resource' on which to run the instance
    resid = sleepresid = -1;
    done = 0;
    for (i = 0; i < resourceCache->numResources && !done; i++) {
        int mem, disk, cores;

        res = &(resourceCache->resources[i]);
        if (!strcmp(res->hostname, targetNode)) {
            done++;
            if ((res->state == RESUP) && ((res->ncState == ENABLED) || (is_migration && (res->ncState == STOPPED)))) {
                if (is_migration && (res->ncState == STOPPED)) {
                    LOGINFO("scheduler overriding STOPPED state of target node (due to explicit scheduling request)\n");
                }
                mem = res->availMemory - vm->mem;
                disk = res->availDisk - vm->disk;
                cores = res->availCores - vm->cores;

                if (mem >= 0 && disk >= 0 && cores >= 0) {
                    resid = i;
                }
            } else if ((res->state == RESASLEEP) && ((res->ncState == ENABLED) || (is_migration && (res->ncState == STOPPED)))) {
                if (is_migration && (res->ncState == STOPPED)) {
                    LOGINFO("scheduler overriding STOPPED state of target node (due to explicit scheduling request)\n");
                }
                mem = res->availMemory - vm->mem;
                disk = res->availDisk - vm->disk;
                cores = res->availCores - vm->cores;

                if (mem >= 0 && disk >= 0 && cores >= 0) {
                    sleepresid = i;
                }
            }
        }
    }

    if (resid == -1 && sleepresid == -1) {
        // target resource is unavailable
        return (1);
    }

    if (resid != -1) {
        res = &(resourceCache->resources[resid]);
        *outresid = resid;
    } else if (sleepresid != -1) {
        res = &(resourceCache->resources[sleepresid]);
        *outresid = sleepresid;
    }
    if (res->state == RESASLEEP) {
        powerUp(res);
    }

    return (0);
}

//!
//!
//!
//! @param[in]  vm
//! @param[out] outresid
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int schedule_instance_greedy(virtualMachine * vm, int *outresid)
{
    int i, done, resid, sleepresid;
    ccResource *res;

    *outresid = 0;

    if (config->schedPolicy == SCHEDGREEDY) {
        LOGDEBUG("scheduler using GREEDY policy to find next resource\n");
    } else if (config->schedPolicy == SCHEDPOWERSAVE) {
        LOGDEBUG("scheduler using POWERSAVE policy to find next resource\n");
    }
    // find the best 'resource' on which to run the instance
    resid = sleepresid = -1;
    done = 0;
    for (i = 0; i < resourceCache->numResources && !done; i++) {
        int mem, disk, cores;

        res = &(resourceCache->resources[i]);
        if ((res->state == RESUP || res->state == RESWAKING) && (resid == -1) && (res->ncState == ENABLED)) {
            mem = res->availMemory - vm->mem;
            disk = res->availDisk - vm->disk;
            cores = res->availCores - vm->cores;

            if (mem >= 0 && disk >= 0 && cores >= 0) {
                resid = i;
                done++;
            }
        } else if ((res->state == RESASLEEP) && (sleepresid == -1) && (res->ncState == ENABLED)) {
            mem = res->availMemory - vm->mem;
            disk = res->availDisk - vm->disk;
            cores = res->availCores - vm->cores;

            if (mem >= 0 && disk >= 0 && cores >= 0) {
                sleepresid = i;
            }
        }
    }

    if (resid == -1 && sleepresid == -1) {
        // didn't find a resource
        return (1);
    }

    if (resid != -1) {
        res = &(resourceCache->resources[resid]);
        *outresid = resid;
    } else if (sleepresid != -1) {
        res = &(resourceCache->resources[sleepresid]);
        *outresid = sleepresid;
    }
    if (res->state == RESASLEEP) {
        powerUp(res);
    }

    return (0);
}

int schedule_instance_user(virtualMachine * vm, char *amiId, char *kernelId, char *ramdiskId, char *instId, char *userData, char *platform, int *outresid)
{
    int i = 0;
    int rc = 0;
    int done = 0;
    int resid = 0;
    int sleepresid = 0;
    ccResource *res = NULL;
    ccInstance *inst = NULL;
    FILE *OFH = NULL;

    // create a temporary file for relaying resource information to the scheduler
    char schedfile[EUCA_MAX_PATH] = "/tmp/euca-schedfile-XXXXXX";
    if (str2file(NULL, schedfile, O_CREAT | O_EXCL | O_WRONLY, 0644, TRUE) != EUCA_OK)
        return (-1);

    // create a temporary file for relaying information about running instances to the scheduler
    char instfile[EUCA_MAX_PATH] = "/tmp/euca-instfile-XXXXXX";
    if (str2file(NULL, instfile, O_CREAT | O_EXCL | O_WRONLY, 0644, TRUE) != EUCA_OK)
        return (-1);

    // create a temporary file for relaying instance's user data to the scheduler
    char datafile[EUCA_MAX_PATH] = "/tmp/euca-datafile-XXXXXX";
    if (str2file(userData, datafile, O_CREAT | O_EXCL | O_WRONLY, 0644, TRUE) != EUCA_OK)
        return (-1);

    // clear out the result
    *outresid = 0;

    // populate the file with resource information
    resid = sleepresid = -1;
    done = 0;
    OFH = fopen(schedfile, "w");
    if (!OFH) {
        LOGERROR("cannot open resources file '%s' for writing\n", schedfile);
        return (-1);
    }
    char lbuf[512];
    for (i = 0; i < resourceCache->numResources && !done; i++) {
        res = &(resourceCache->resources[i]);
        if (res) {
            snprintf(lbuf, sizeof(lbuf), "idx=%d,ip=%s,state=%d,availmem=%d,availdisk=%d,availcores=%d", i + 1, res->ip, res->state, res->availMemory, res->availDisk,
                     res->availCores);
            fprintf(OFH, "%s\n", lbuf);
        }
    }
    fclose(OFH);

    // populate the file with information about instances
    OFH = fopen(instfile, "w");
    if (!OFH) {
        LOGERROR("cannot open temporary instance file '%s' for writing\n", instfile);
        return (-1);
    }
    for (i = 0; i < instanceCache->numInsts; i++) {
        inst = &(instanceCache->instances[i]);
        if (inst) {
            snprintf(lbuf, sizeof(lbuf), "id=%s,state=%s,nchost=%s,mem=%d,disk=%d,cores=%d,secgroupidx=%d,publicip=%s,privateip=%s,ownerId=%s,accountId=%s,launchTime=%ld",
                     inst->instanceId, inst->state, inst->serviceTag, inst->ccvm.mem, inst->ccvm.disk, inst->ccvm.cores, inst->ccnet.vlan, inst->ccnet.publicIp,
                     inst->ccnet.privateIp, inst->accountId, inst->ownerId, inst->ts);
            fprintf(OFH, "%s\n", lbuf);
        }
    }
    fclose(OFH);

    // invoke the external scheduler, passing it the two files as well as resource requirements of the new instance
    char cmd[EUCA_MAX_PATH * 3 + CHAR_BUFFER_SIZE]; // 3 paths on command line, plus other stuff
    char stdout_str[VERY_BIG_CHAR_BUFFER_SIZE];
    char stderr_str[VERY_BIG_CHAR_BUFFER_SIZE];
    snprintf(cmd, sizeof(cmd), "%s %s %s %d %d %d %s %s %s", config->schedPath, schedfile, instfile, vm->mem, vm->disk, vm->cores, instId, datafile, platform);
    rc = timeshell(cmd, stdout_str, stderr_str, VERY_BIG_CHAR_BUFFER_SIZE, SCHED_TIMEOUT_SEC);
    LOGDEBUG("external scheduler returned: %d, stdout: '%s', stderr: '%s'\n", rc, stdout_str, stderr_str);
    unlink(schedfile);
    unlink(instfile);
    unlink(datafile);

    resid = rc - 1;                    // rc for valid nodes [1..N], 0 means scheduler could not find a resource
    if (resid < 0 || resid >= resourceCache->numResources) {
        // didn't find a resource
        LOGWARN("couldn't find a resource or user scheduler is incorrect\n");
        return (1);
    }
    *outresid = resid;

    return (0);
}

//!
//!
//!
//! @param[in] gerund
//! @param[in] instIds
//! @param[in] instIdsLen
//!
static void print_abbreviated_instances(const char *gerund, char **instIds, int instIdsLen)
{
    int k = 0;
    int offset = 0;
    char list[60] = "";

    for (k = 0; ((k < instIdsLen) && (offset < ((sizeof(list) - 4)))); k++) {
        offset += snprintf(list + offset, sizeof(list) - 3 - offset, "%s%s", (k == 0) ? ("") : (", "), instIds[k]);
    }

    if (strlen(list) == (sizeof(list) - 4)) {
        sprintf(list + offset, "...");
    }

    LOGINFO("%s %d instance(s): %s\n", gerund, instIdsLen, list);
}

//!
//!
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  amiId
//! @param[in]  kernelId the kernel image identifier (eki-XXXXXXXX)
//! @param[in]  ramdiskId the ramdisk image identifier (eri-XXXXXXXX)
//! @param[in]  amiURL
//! @param[in]  kernelURL the kernel image URL address
//! @param[in]  ramdiskURL the ramdisk image URL address
//! @param[in]  instIds
//! @param[in]  instIdsLen
//! @param[in]  netNames
//! @param[in]  netNamesLen
//! @param[in]  macAddrs
//! @param[in]  macAddrsLen
//! @param[in]  networkIndexList
//! @param[in]  networkIndexListLen
//! @param[in]  uuids
//! @param[in]  uuidsLen
//! @param[in]  minCount
//! @param[in]  maxCount
//! @param[in]  accountId
//! @param[in]  ownerId
//! @param[in]  reservationId
//! @param[in]  ccvm
//! @param[in]  keyName
//! @param[in]  vlan
//! @param[in]  userData
//! @param[in]  launchIndex
//! @param[in]  platform
//! @param[in]  expiryTime
//! @param[in]  targetNode
//! @param[out] outInsts
//! @param[out] outInstsLen
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doRunInstances(ncMetadata * pMeta, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char **instIds,
                   int instIdsLen, char **netNames, int netNamesLen, char **macAddrs, int macAddrsLen, int *networkIndexList, int networkIndexListLen,
                   char **uuids, int uuidsLen, char **privateIps, int privateIpsLen, int minCount, int maxCount, char *accountId, char *ownerId,
                   char *reservationId, virtualMachine * ccvm, char *keyName, int vlan, char *userData, char *credential, char *launchIndex,
                   char *platform, int expiryTime, char *targetNode, ccInstance ** outInsts, int *outInstsLen)
{
    int rc = 0, i = 0, done = 0, runCount = 0, resid = 0, foundnet = 0, error = 0, nidx = 0, thenidx = 0;
    ccInstance *myInstance = NULL, *retInsts = NULL;
    char instId[16], uuid[48];
    ccResource *res = NULL;
    char mac[32], privip[32], pubip[32];

    ncInstance *outInst = NULL;
    virtualMachine ncvm;
    netConfig ncnet;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }
    print_abbreviated_instances("running", instIds, instIdsLen);
    LOGDEBUG("invoked: userId=%s, emiId=%s, kernelId=%s, ramdiskId=%s, emiURL=%s, kernelURL=%s, ramdiskURL=%s, instIdsLen=%d, netNamesLen=%d, "
             "macAddrsLen=%d, networkIndexListLen=%d, minCount=%d, maxCount=%d, accountId=%s, ownerId=%s, reservationId=%s, keyName=%s, vlan=%d, "
             "userData=%s, credential=%s, launchIndex=%s, platform=%s, targetNode=%s\n", SP(pMeta ? pMeta->userId : "UNSET"), SP(amiId), SP(kernelId), SP(ramdiskId),
             SP(amiURL), SP(kernelURL), SP(ramdiskURL), instIdsLen, netNamesLen, macAddrsLen, networkIndexListLen, minCount, maxCount, SP(accountId),
             SP(ownerId), SP(reservationId), SP(keyName), vlan, SP(userData), SP(credential), SP(launchIndex), SP(platform), SP(targetNode));

    if (config->use_proxy) {
        char objectStorageURL[EUCA_MAX_PATH], *strptr = NULL, newURL[EUCA_MAX_PATH];

        // get objectstorage IP
        done = 0;
        for (i = 0; i < 16 && !done; i++) {
            if (!strcmp(config->services[i].type, "objectstorage")) {
                snprintf(objectStorageURL, EUCA_MAX_PATH, "%s", config->services[i].uris[0]);
                done++;
            }
        }

        if (done) {
            // cache and reset endpoint
            for (i = 0; i < ccvm->virtualBootRecordLen; i++) {
                newURL[0] = '\0';
                if (!strcmp(ccvm->virtualBootRecord[i].typeName, "machine") || !strcmp(ccvm->virtualBootRecord[i].typeName, "kernel")
                    || !strcmp(ccvm->virtualBootRecord[i].typeName, "ramdisk")) {
                    strptr = strstr(ccvm->virtualBootRecord[i].resourceLocation, "objectstorage://");
                    if (strptr) {
                        strptr += strlen("objectstorage://");
                        snprintf(newURL, EUCA_MAX_PATH, "%s/%s", objectStorageURL, strptr);
                        LOGDEBUG("constructed cacheable URL: %s\n", newURL);
                        rc = image_cache(ccvm->virtualBootRecord[i].id, newURL);
                        if (!rc) {
                            snprintf(ccvm->virtualBootRecord[i].resourceLocation, CHAR_BUFFER_SIZE, "http://%s:8776/%s", config->proxyIp, ccvm->virtualBootRecord[i].id);
                        } else {
                            LOGWARN("could not cache image %s/%s\n", ccvm->virtualBootRecord[i].id, newURL);
                        }
                    }
                }
            }
        }
    }

    *outInstsLen = 0;

    if (!ccvm) {
        LOGERROR("invalid ccvm\n");
        return (-1);
    }
    if (minCount <= 0 || maxCount <= 0 || instIdsLen < maxCount) {
        LOGERROR("bad min or max count, or not enough instIds (%d, %d, %d)\n", minCount, maxCount, instIdsLen);
        return (-1);
    }
    // check health of the networkIndexList
    if ((!strcmp(vnetconfig->mode, NETMODE_SYSTEM) || !strcmp(vnetconfig->mode, NETMODE_STATIC) || !strcmp(vnetconfig->mode, NETMODE_EDGE)) || networkIndexList == NULL) {
        // disabled
        nidx = -1;
        if (vlan > 0) {
            vlan = 0;
        }
    } else {
        if ((networkIndexListLen < minCount) || (networkIndexListLen > maxCount)) {
            LOGERROR("network index length (%d) is out of bounds for min/max instances (%d-%d)\n", networkIndexListLen, minCount, maxCount);
            return (1);
        }
        for (i = 0; i < networkIndexListLen; i++) {
            if ((networkIndexList[i] < 0) || (networkIndexList[i] > (vnetconfig->numaddrs - 1))) {
                LOGERROR("network index (%d) out of bounds (0-%d)\n", networkIndexList[i], vnetconfig->numaddrs - 1);
                return (1);
            }
        }

        // all checked out
        nidx = 0;
    }

    retInsts = EUCA_ZALLOC(maxCount, sizeof(ccInstance));
    if (!retInsts) {
        LOGFATAL("out of memory!\n");
        unlock_exit(1);
    }

    runCount = 0;

    // get updated resource information

    done = 0;
    for (i = 0; i < maxCount && !done; i++) {
        snprintf(instId, 16, "%s", instIds[i]);
        if (uuidsLen > i) {
            snprintf(uuid, 48, "%s", uuids[i]);
        } else {
            snprintf(uuid, 48, "UNSET");
        }

        LOGDEBUG("running instance %s\n", instId);

        // generate new mac
        bzero(mac, 32);
        bzero(pubip, 32);
        bzero(privip, 32);

        strncpy(pubip, "0.0.0.0", 32);

        if (privateIpsLen > 0) {
            snprintf(privip, 32, "%s", privateIps[i]);
        } else {
            strncpy(privip, "0.0.0.0", 32);
        }

        sem_mywait(VNET);
        {
            if (nidx == -1) {
                rc = vnetGenerateNetworkParams(vnetconfig, instId, vlan, -1, mac, pubip, privip);
                thenidx = -1;
            } else {
                rc = vnetGenerateNetworkParams(vnetconfig, instId, vlan, networkIndexList[nidx], mac, pubip, privip);
                thenidx = nidx;
                nidx++;
            }
            if (rc) {
                foundnet = 0;
            } else {
                foundnet = 1;
            }
        }
        sem_mypost(VNET);

        if (thenidx != -1) {
            LOGDEBUG("assigning MAC/IP: %s/%s/%s/%d\n", mac, pubip, privip, networkIndexList[thenidx]);
        } else {
            LOGDEBUG("assigning MAC/IP: %s/%s/%s/%d\n", mac, pubip, privip, thenidx);
        }

        if (mac[0] == '\0' || !foundnet) {
            LOGERROR("could not find/initialize any free network address, failing doRunInstances()\n");
        } else {
            // "run" the instance
            memcpy(&ncvm, ccvm, sizeof(virtualMachine));

            ncnet.vlan = vlan;
            if (thenidx >= 0) {
                ncnet.networkIndex = networkIndexList[thenidx];
            } else {
                ncnet.networkIndex = -1;
            }
            snprintf(ncnet.privateMac, MAC_BUFFER_SIZE, "%s", mac);
            snprintf(ncnet.privateIp, IP_BUFFER_SIZE, "%s", privip);
            snprintf(ncnet.publicIp, IP_BUFFER_SIZE, "%s", pubip);

            sem_mywait(RESCACHE);

            resid = 0;

            sem_mywait(CONFIG);
            rc = schedule_instance(ccvm, amiId, kernelId, ramdiskId, instId, userData, platform, targetNode, &resid);
            sem_mypost(CONFIG);

            res = &(resourceCache->resources[resid]);
            if (rc) {
                // could not find resource
                LOGERROR("scheduler could not find resource to run the instance on\n");
                // couldn't run this VM, remove networking information from system
                free_instanceNetwork(mac, vlan, 1, 1);
            } else {
                int pid, ret;

                // try to run the instance on the chosen resource
                LOGINFO("scheduler decided to run instance %s on resource %s, running count %d\n", instId, res->ncURL, res->running);

                outInst = NULL;

                pid = fork();
                if (pid == 0) {
                    time_t startRun, ncRunTimeout;

                    sem_mywait(RESCACHE);
                    if (res->running > 0) {
                        res->running++;
                    }
                    sem_mypost(RESCACHE);

                    ret = 0;
                    LOGTRACE("sending run instance: node=%s instanceId=%s emiId=%s mac=%s privIp=%s pubIp=%s vlan=%d networkIdx=%d key=%.32s... "
                             "mem=%d disk=%d cores=%d\n", res->ncURL, instId, SP(amiId), ncnet.privateMac, ncnet.privateIp, ncnet.publicIp,
                             ncnet.vlan, ncnet.networkIndex, SP(keyName), ncvm.mem, ncvm.disk, ncvm.cores);

                    rc = 1;
                    startRun = time(NULL);
                    if (config->schedPolicy == SCHEDPOWERSAVE) {
                        ncRunTimeout = config->wakeThresh;
                    } else {
                        ncRunTimeout = 15;
                    }

                    while (rc && ((time(NULL) - startRun) < ncRunTimeout)) {

                        // if we're running windows, and are an NC, create the pw/floppy locally
                        if (strstr(platform, "windows") && !strstr(res->ncURL, "EucalyptusNC")) {
                            //if (strstr(platform, "windows")) {
                            char cdir[EUCA_MAX_PATH];
                            snprintf(cdir, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/windows/", config->eucahome);
                            if (check_directory(cdir))
                                mkdir(cdir, 0700);
                            snprintf(cdir, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/windows/%s/", config->eucahome, instId);
                            if (check_directory(cdir))
                                mkdir(cdir, 0700);
                            if (check_directory(cdir)) {
                                LOGERROR("could not create console/floppy cache directory '%s'\n", cdir);
                            } else {
                                // drop encrypted windows password and floppy on filesystem
                                rc = makeWindowsFloppy(config->eucahome, cdir, keyName, instId);
                                if (rc) {
                                    LOGERROR("could not create console/floppy cache\n");
                                }
                            }
                        }
                        rc = ncClientCall(pMeta, OP_TIMEOUT_PERNODE, res->lockidx, res->ncURL, "ncRunInstance", uuid, instId, reservationId, &ncvm,
                                          amiId, amiURL, kernelId, kernelURL, ramdiskId, ramdiskURL, ownerId, accountId, keyName, &ncnet, userData, credential,
                                          launchIndex, platform, expiryTime, netNames, netNamesLen, &outInst);
                        LOGDEBUG("sent run request for instance '%s' on resource '%s': result '%s' uuis '%s'\n", instId, res->ncURL, uuid, rc ? "FAIL" : "SUCCESS");
                        if (rc) {
                            // make sure we get the latest topology information before trying again
                            sem_mywait(CONFIG);
                            memcpy(pMeta->services, config->services, sizeof(serviceInfoType) * 16);
                            memcpy(pMeta->disabledServices, config->disabledServices, sizeof(serviceInfoType) * 16);
                            memcpy(pMeta->notreadyServices, config->notreadyServices, sizeof(serviceInfoType) * 16);
                            sem_mypost(CONFIG);
                            sleep(1);
                        }
                    }
                    if (!rc) {
                        ret = 0;
                    } else {
                        ret = 1;
                    }

                    sem_mywait(RESCACHE);
                    if (res->running > 0) {
                        res->running--;
                    }
                    sem_mypost(RESCACHE);

                    exit(ret);
                } else {
                    rc = 0;
                    LOGDEBUG("call complete (pid/rc): %d/%d\n", pid, rc);
                }
                if (rc != 0) {
                    // problem
                    LOGERROR("tried to run the VM, but runInstance() failed; marking resource '%s' as down\n", res->ncURL);
                    res->state = RESDOWN;
                    i--;
                    // couldn't run this VM, remove networking information from system
                    free_instanceNetwork(mac, vlan, 1, 1);
                } else {
                    res->availMemory -= ccvm->mem;
                    res->availDisk -= ccvm->disk;
                    res->availCores -= ccvm->cores;

                    LOGDEBUG("resource information after schedule/run: %d/%d, %d/%d, %d/%d\n", res->availMemory, res->maxMemory,
                             res->availCores, res->maxCores, res->availDisk, res->maxDisk);

                    myInstance = &(retInsts[runCount]);
                    bzero(myInstance, sizeof(ccInstance));

                    allocate_ccInstance(myInstance, instId, amiId, kernelId, ramdiskId, amiURL, kernelURL, ramdiskURL, ownerId, accountId, "Pending",
                                        "", time(NULL), reservationId, &ncnet, &ncnet, ccvm, resid, keyName, resourceCache->resources[resid].ncURL,
                                        userData, launchIndex, platform, myInstance->guestStateName, myInstance->bundleTaskStateName, myInstance->groupNames, myInstance->volumes,
                                        myInstance->volumesSize);

                    sensor_add_resource(myInstance->instanceId, "instance", uuid);
                    sensor_set_resource_alias(myInstance->instanceId, myInstance->ncnet.privateIp);

                    // start up DHCP
                    sem_mywait(CONFIG);
                    config->kick_dhcp = 1;
                    sem_mypost(CONFIG);

                    // add the instance to the cache, and continue on
                    //      add_instanceCache(myInstance->instanceId, myInstance);
                    refresh_instanceCache(myInstance->instanceId, myInstance);
                    print_ccInstance("", myInstance);

                    runCount++;
                }
            }

            sem_mypost(RESCACHE);

        }

    }
    *outInstsLen = runCount;
    *outInsts = retInsts;

    LOGTRACE("done\n");

    shawn();

    if (runCount < 1) {
        error++;
        LOGERROR("unable to run input instance\n");
    }
    if (error) {
        return (1);
    }
    return (0);
}

//!
//!
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId
//! @param[out] consoleOutput
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doGetConsoleOutput(ncMetadata * pMeta, char *instanceId, char **consoleOutput)
{
    int i = 0;
    int rc = 0;
    int start = 0;
    int stop = 0;
    int done = 0;
    int ret = EUCA_OK;
    int timeout = 0;
    char *rawconsole = NULL;
    char pwfile[EUCA_MAX_PATH] = "";
    time_t op_start = 0;
    ccInstance *myInstance = NULL;
    ccResourceCache resourceCacheLocal = { {{{0}}} };

    op_start = time(NULL);
    *consoleOutput = NULL;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (EUCA_ERROR);
    }

    LOGINFO("[%s] requesting console output\n", SP(instanceId));
    LOGDEBUG("invoked: instId=%s\n", SP(instanceId));

    sem_mywait(RESCACHE);
    {
        memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    }
    sem_mypost(RESCACHE);

    if ((rc = find_instanceCacheId(instanceId, &myInstance)) == 0) {
        // found the instance in the cache
        start = myInstance->ncHostIdx;
        stop = start + 1;
        EUCA_FREE(myInstance);
    } else {
        start = 0;
        stop = resourceCacheLocal.numResources;
    }

    for (i = start, done = 0; ((i < stop) && !done); i++) {
        EUCA_FREE(*consoleOutput);

        // if not talking to Eucalyptus NC (but, e.g., a Broker)
        if (!strstr(resourceCacheLocal.resources[i].ncURL, "EucalyptusNC")) {
            *consoleOutput = NULL;
            snprintf(pwfile, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/windows/%s/console.append.log", config->eucahome, instanceId);

            rawconsole = NULL;
            if (!check_file(pwfile)) { // the console log file should exist for a Windows guest (with encrypted password in it)
                rawconsole = file2str(pwfile);
            } else {                   // the console log file will not exist for a Linux guest
                rawconsole = strdup("not implemented");
            }

            if (rawconsole) {
                *consoleOutput = base64_enc(((u8 *) rawconsole), strlen(rawconsole));
                EUCA_FREE(rawconsole);
            }
            // set the return code accordingly
            if (*consoleOutput == NULL) {
                rc = 1;
            } else {
                rc = 0;
            }
            done++;                    // quit on the first host, since they are not queried remotely
        } else {                       // otherwise, we *are* talking to a Eucalyptus NC, so make the remote call
            timeout = ncGetTimeout(op_start, OP_TIMEOUT, (stop - start), i);
            rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[i].lockidx, resourceCacheLocal.resources[i].ncURL, "ncGetConsoleOutput", instanceId, consoleOutput);
        }

        if (rc) {
            ret = EUCA_ERROR;
        } else {
            ret = EUCA_OK;
            done++;
        }
    }

    LOGTRACE("done\n");
    shawn();
    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instIds
//! @param[in] instIdsLen
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doRebootInstances(ncMetadata * pMeta, char **instIds, int instIdsLen)
{
    int i, j, rc, numInsts, start, stop, done, timeout = 0, ret = 0;
    char *instId;
    ccInstance *myInstance;
    time_t op_start;
    ccResourceCache resourceCacheLocal;

    i = j = numInsts = 0;
    instId = NULL;
    myInstance = NULL;
    op_start = time(NULL);

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGINFO("rebooting %d instances\n", instIdsLen);
    LOGDEBUG("invoked: instIdsLen=%d\n", instIdsLen);

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    for (i = 0; i < instIdsLen; i++) {
        instId = instIds[i];
        rc = find_instanceCacheId(instId, &myInstance);
        if (!rc) {
            // found the instance in the cache
            start = myInstance->ncHostIdx;
            stop = start + 1;
            EUCA_FREE(myInstance);
        } else {
            start = 0;
            stop = resourceCacheLocal.numResources;
        }

        done = 0;
        for (j = start; j < stop && !done; j++) {
            timeout = ncGetTimeout(op_start, OP_TIMEOUT, (stop - start), j);
            rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncRebootInstance", instId);
            if (rc) {
                ret = 1;
            } else {
                ret = 0;
                done++;
            }
        }
    }

    LOGTRACE("done\n");

    shawn();

    return (0);                        /// XXX:gholms
}

//!
//!
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instIds
//! @param[in]  instIdsLen
//! @param[in]  force
//! @param[out] outStatus
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doTerminateInstances(ncMetadata * pMeta, char **instIds, int instIdsLen, int force, int **outStatus)
{
    int i, j, shutdownState, previousState, rc, start, stop, done = 0, ret = 0;
    char *instId;
    ccInstance *myInstance = NULL;
    ccResourceCache resourceCacheLocal;

    i = j = 0;
    instId = NULL;
    myInstance = NULL;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }
    set_dirty_instanceCache();

    print_abbreviated_instances("terminating", instIds, instIdsLen);
    LOGDEBUG("invoked: userId=%s, instIdsLen=%d, firstInstId=%s, force=%d\n", SP(pMeta ? pMeta->userId : "UNSET"), instIdsLen, SP(instIdsLen ? instIds[0] : "UNSET"), force);

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    for (i = 0; i < instIdsLen; i++) {
        instId = instIds[i];
        rc = find_instanceCacheId(instId, &myInstance);
        if (!rc) {
            // found the instance in the cache
            if (myInstance != NULL && (!strcmp(myInstance->state, "Pending") || !strcmp(myInstance->state, "Extant") || !strcmp(myInstance->state, "Unknown"))) {
                start = myInstance->ncHostIdx;
                stop = start + 1;
            } else {
                // instance is not in a terminatable state
                start = 0;
                stop = 0;
                (*outStatus)[i] = 0;
            }
            EUCA_FREE(myInstance);
        } else {
            // instance is not in cache, try all resources

            start = 0;
            stop = 0;
            (*outStatus)[i] = 0;
        }

        done = 0;
        for (j = start; j < stop && !done; j++) {
            if (resourceCacheLocal.resources[j].state == RESUP) {

                if (!strstr(resourceCacheLocal.resources[j].ncURL, "EucalyptusNC")) {
                    char cdir[EUCA_MAX_PATH];
                    char cfile[EUCA_MAX_PATH];
                    snprintf(cdir, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/windows/%s/", config->eucahome, instId);
                    if (!check_directory(cdir)) {
                        snprintf(cfile, EUCA_MAX_PATH, "%s/floppy", cdir);
                        if (!check_file(cfile))
                            unlink(cfile);
                        snprintf(cfile, EUCA_MAX_PATH, "%s/console.append.log", cdir);
                        if (!check_file(cfile))
                            unlink(cfile);
                        rmdir(cdir);
                    }
                }

                rc = ncClientCall(pMeta, 0, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncTerminateInstance",
                                  instId, force, &shutdownState, &previousState);
                if (rc) {
                    (*outStatus)[i] = 1;
                    LOGWARN("failed to terminate '%s': instance may not exist any longer\n", instId);
                    ret = 1;
                } else {
                    (*outStatus)[i] = 0;
                    ret = 0;
                    done++;
                }
                rc = ncClientCall(pMeta, 0, resourceCacheStage->resources[j].lockidx, resourceCacheStage->resources[j].ncURL, "ncAssignAddress", instId, "0.0.0.0");
                if (rc) {
                    // problem, but will retry next time
                    LOGWARN("could not send AssignAddress to NC\n");
                }
            }
        }
    }

    LOGTRACE("done\n");

    shawn();

    return (0);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doCreateImage(ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev)
{
    int i, rc, start = 0, stop = 0, ret = 0, done = 0, timeout;
    ccInstance *myInstance;
    time_t op_start;
    ccResourceCache resourceCacheLocal;

    i = 0;
    myInstance = NULL;
    op_start = time(NULL);

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGINFO("[%s] creating image\n", SP(instanceId));
    LOGDEBUG("invoked: userId=%s, volumeId=%s, instanceId=%s, remoteDev=%s\n", SP(pMeta ? pMeta->userId : "UNSET"), SP(volumeId), SP(instanceId), SP(remoteDev));
    if (!volumeId || !instanceId || !remoteDev) {
        LOGERROR("bad input params\n");
        return (1);
    }

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    rc = find_instanceCacheId(instanceId, &myInstance);
    if (!rc) {
        // found the instance in the cache
        if (myInstance) {
            start = myInstance->ncHostIdx;
            stop = start + 1;
            EUCA_FREE(myInstance);
        }
    } else {
        start = 0;
        stop = resourceCacheLocal.numResources;
    }

    done = 0;
    for (i = start; i < stop && !done; i++) {
        timeout = ncGetTimeout(op_start, OP_TIMEOUT, stop - start, i);
        rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[i].lockidx, resourceCacheLocal.resources[i].ncURL, "ncCreateImage", instanceId, volumeId, remoteDev);
        if (rc) {
            ret = 1;
        } else {
            ret = 0;
            done++;
        }
    }

    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//!
//!
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  historySize
//! @param[in]  collectionIntervalTimeMs
//! @param[in]  instIds
//! @param[in]  instIdsLen
//! @param[in]  sensorIds
//! @param[in]  sensorIdsLen
//! @param[out] outResources
//! @param[out] outResourcesLen
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doDescribeSensors(ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds, int instIdsLen, char **sensorIds, int sensorIdsLen,
                      sensorResource *** outResources, int *outResourcesLen)
{
    int i = 0;
    int rc = 0;
    int err = 0;
    int num_results = 0;
    int num_instances = 0;
    int num_resources = 0;
    int col_interval_sec = 0;
    int nc_poll_interval_sec = 0;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }

    LOGDEBUG("invoked: historySize=%d collectionIntervalTimeMs=%lld instIdsLen=%d i[0]='%s' sensorIdsLen=%d s[0]='%s'\n",
             historySize, collectionIntervalTimeMs, instIdsLen, ((instIdsLen > 0) ? instIds[0] : "*"), sensorIdsLen, ((sensorIdsLen > 0) ? sensorIds[0] : "*"));

    // update the config parameters if they are different
    if ((err = sensor_config(historySize, collectionIntervalTimeMs)) != 0)
        LOGWARN("failed to update sensor configuration (err=%d)\n", err);

    if ((historySize > 0) && (collectionIntervalTimeMs > 0)) {
        col_interval_sec = collectionIntervalTimeMs / 1000;
        nc_poll_interval_sec = ((col_interval_sec * historySize) - POLL_INTERVAL_SAFETY_MARGIN_SEC);
        nc_poll_interval_sec = ((nc_poll_interval_sec < POLL_INTERVAL_MINIMUM_SEC) ? POLL_INTERVAL_MINIMUM_SEC : nc_poll_interval_sec);

        if (config->ncSensorsPollingInterval != nc_poll_interval_sec) {
            config->ncSensorsPollingInterval = nc_poll_interval_sec;
            LOGDEBUG("changed NC sensors poll interval to %d (col_interval_sec=%d historySize=%d)\n", nc_poll_interval_sec, col_interval_sec, historySize);
        }
    }

    if ((num_resources = sensor_get_num_resources()) < 0) {
        LOGERROR("failed to determine number of available sensor resources\n");
        return (1);
    }
    // oddly, an empty set of instanceIds or sensorIds in XML is presented
    // by Axis as an array of size 1 with an empty string as the only element
    num_instances = instIdsLen;
    if ((instIdsLen == 1) && (strlen(instIds[0]) == 0))
        num_instances = 0;             // which is to say all instances

    (*outResources) = NULL;
    (*outResourcesLen) = 0;

    if (num_resources > 0) {
        if (num_instances == 0) {
            // report on all instances
            if (((*outResources) = EUCA_ZALLOC(num_resources, sizeof(sensorResource *))) == NULL) {
                return (OUT_OF_MEMORY);
            }

            for (i = 0; i < num_resources; i++) {
                if (((*outResources)[i] = EUCA_ZALLOC(1, sizeof(sensorResource))) == NULL) {
                    // We may return out of memory but the caller may ignore the error type, in this case, we should
                    // let the caller know that 'i' amount of memory was allocated so he can free
                    (*outResourcesLen) = i;
                    return (OUT_OF_MEMORY);
                }
            }
            // if number of resources has changed since the call to sensor_get_num_resources(),
            // then we may not report on everything (ok, since we'll get it next time)
            // or we may have fewer records in outResrouces[] (ok, since empty ones will be ignored)
            if (sensor_get_instance_data(NULL, NULL, 0, *outResources, num_resources) == 0) {
                (*outResourcesLen) = num_resources; // actually (*outResourcesLen) <= num_resources, but that's OK
            } else {
                // Ok, we had a failure, lets make sure we free our resources so the caller isn't confused
                for (i = 0; i < num_resources; i++) {
                    EUCA_FREE((*outResources)[i]);
                }
                EUCA_FREE(*outResources);
            }
        } else {
            // report on specific instances
            if (((*outResources) = EUCA_ZALLOC(num_instances, sizeof(sensorResource *))) == NULL) {
                return (OUT_OF_MEMORY);
            }
            // if some instances requested by ID were not found on this CC,
            // we will have fewer records in outResources[] (ok, since empty ones will be ignored)
            for (i = 0, num_results = 0; i < num_instances; i++) {
                if ((*outResources)[num_results] == NULL) {
                    if (((*outResources)[num_results] = EUCA_ZALLOC(1, sizeof(sensorResource))) == NULL) {
                        // We may return out of memory but the caller may ignore the error type, in this case, we should
                        // let the caller know that 'num_results' amount of memory was allocated so he can free
                        (*outResourcesLen) = num_results;
                        return (OUT_OF_MEMORY);
                    }
                }

                if (sensor_get_instance_data(instIds[i], NULL, 0, (*outResources + num_results), 1) == 0) {
                    num_results++;
                } else {
                    EUCA_FREE((*outResources)[num_results]);
                }
            }

            (*outResourcesLen) = num_results;
        }
    }

    LOGTRACE("returning (outResourcesLen=%d)\n", (*outResourcesLen));
    return (0);
}

//!
//! Implements the CC logic of modifying state of a node controller
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] nodeName the IP of the NC to effect
//! @param[in] stateName the state for the NC
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doModifyNode(ncMetadata * pMeta, char *nodeName, char *stateName)
{
    int i, rc, ret = 0, timeout;
    int src_index = -1;
    ccResourceCache resourceCacheLocal;

    // no need to call initialize(pMeta, FALSE) because we call doModifyNode internally, from doEnable/DisableService
    if (ccIsEnabled()) {
        return (1);
    }

    if (!nodeName || !stateName) {
        LOGERROR("bad input params\n");
        return (1);
    }
    LOGINFO("modifying node %s with state=%s\n", SP(nodeName), SP(stateName));

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    for (i = 0; i < resourceCacheLocal.numResources && (src_index == -1); i++) {
        if (resourceCacheLocal.resources[i].state != RESASLEEP) {
            if (!strcmp(resourceCacheLocal.resources[i].hostname, nodeName)) {
                // found it
                src_index = i;
                break;
            }
        }
    }
    if (src_index == -1) {
        LOGERROR("node requested for modification (%s) cannot be found\n", SP(nodeName));
        ret = 1;
        goto out;
    }

    timeout = ncGetTimeout(time(NULL), OP_TIMEOUT, 1, 0);
    rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[src_index].lockidx, resourceCacheLocal.resources[src_index].ncURL, "ncModifyNode", stateName);   // no need to pass nodeName as ncClientCall sets that up for all NC requests
    if (rc) {
        ret = 1;
        goto out;

    } else {                           // state change succeded => update nodeStatus and resource availability if the change succeeds
        sem_mywait(RESCACHE);
        for (i = 0; i < MAXNODES; i++) {
            if (!strcmp(resourceCache->resources[i].hostname, nodeName)) {
                ccResource *res = &(resourceCache->resources[i]);
                if (!strcmp(res->nodeStatus, "enabled")) {
                    res->ncState = ENABLED;
                } else if (!strcmp(res->nodeStatus, "disabled")) {
                    res->ncState = STOPPED;
                }
                euca_strncpy(res->nodeStatus, stateName, 24);
                break;
            }
        }
        sem_mypost(RESCACHE);
    }

out:
    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//!
//! Implements the CC logic of migrating instances from a node controller
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] actionNode the IP of the NC to migrate instances from (or roll back)
//! @param[in] instanceId the instance to migrate
//! @param[in] destinationNodes the IP(s) of the NCs to either whitelist or blacklist as migration destinations
//! @param[in] destinationNodeCount the number of destinationNodes
//! @param[in] allowHosts determines whether destinationNodes is used as a whitelist or blacklist
//! @param[in] nodeAction the action to perform on the NC
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doMigrateInstances(ncMetadata * pMeta, char *actionNode, char *instanceId, char **destinationNodes, int destinationNodeCount, int allowHosts, char *nodeAction)
{
    char credentials[CREDENTIAL_SIZE];
    int i, rc, ret = 0, timeout;
    int src_index = -1, dst_index = -1;
    int preparing = 0;
    int committing = 0;
    int rollback = 0;
    int found_instances = 0;
    ccResourceCache resourceCacheLocal;
    ccInstance **cc_instances = NULL;
    ncInstance **nc_instances = NULL;

    LOGTRACE("invoked\n");

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled()) {
        return (1);
    }
    if (!actionNode && !instanceId) {
        LOGERROR("bad input params\n");
        pMeta->replyString = strdup("internal error (neither node nor instance were set)");
        return (1);
    }
    if (!strcmp(nodeAction, "prepare")) {
        if (actionNode && instanceId) {
            LOGWARN
                ("[%s] specified migration preparation using both instance ID and source node (%s). Ignoring source-node specification and migrating the single specfied instance.\n",
                 SP(instanceId), SP(actionNode));
        }
        if (instanceId) {
            LOGINFO("preparing migration for specific instance %s\n", SP(instanceId));
        } else {
            LOGINFO("preparing migration from node %s\n", SP(actionNode));
        }
        preparing = 1;
    } else if (!strcmp(nodeAction, "commit")) {
        LOGINFO("[%s] committing migration from node %s\n", SP(instanceId), SP(actionNode));
        committing = 1;
    } else if (!strcmp(nodeAction, "rollback")) {
        // This could actually be the destination node.
        LOGINFO("rolling back migration on node %s\n", SP(actionNode));
        rollback = 1;
    } else {
        LOGERROR("invalid action parameter: %s\n", nodeAction);
        pMeta->replyString = strdup("internal error (invalid action)");
        return (1);
    }

    sem_mywait(RESCACHE);
    memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    sem_mypost(RESCACHE);

    if (!instanceId) {
        for (i = 0; i < resourceCacheLocal.numResources && (src_index == -1); i++) {
            if (resourceCacheLocal.resources[i].state != RESASLEEP) {
                if (!strcmp(resourceCacheLocal.resources[i].hostname, actionNode)) {
                    // found it
                    src_index = i;
                    break;
                }
            }
        }
        if (src_index == -1) {
            LOGERROR("node requested (%s) for migration action '%s' cannot be found\n", SP(actionNode), SP(nodeAction));
            pMeta->replyString = strdup("requested node cannot be found by the CC");
            goto out;
        }
    }

    sem_mywait(INSTCACHE);
    if (instanceCache->numInsts) {
        for (i = 0; i < MAXINSTANCES_PER_CC; i++) {
            if (instanceCache->cacheState[i] == INSTVALID && (instanceId || instanceCache->instances[i].ncHostIdx == src_index)
                && (!strcmp(instanceCache->instances[i].state, "Extant"))) {
                if (instanceId) {
                    // Only looking for a specific instance?
                    if (strcmp(instanceCache->instances[i].instanceId, instanceId)) {
                        // Yes, but this is not the one, so keep looking.
                        continue;
                    } else {
                        // Found our instance.
                        src_index = instanceCache->instances[i].ncHostIdx;
                        LOGDEBUG("[%s] found instance running on node %s\n", instanceId, resourceCacheLocal.resources[src_index].hostname);
                    }
                }
                // TO-DO: Wrap alloc()'s
                cc_instances = EUCA_REALLOC(cc_instances, found_instances + 1, sizeof(ccInstance *));
                cc_instances[found_instances] = EUCA_ZALLOC(1, sizeof(ccInstance));
                memcpy(cc_instances[found_instances], &(instanceCache->instances[i]), sizeof(ccInstance));
                LOGTRACE("[%s] copied cc_instances[%d] (reservation=%s, uuid=%s) from instance cache\n",
                         cc_instances[found_instances]->instanceId, found_instances, cc_instances[found_instances]->reservationId, cc_instances[found_instances]->uuid);
                found_instances++;

                if (instanceId) {
                    // If we get here, we've found our one specified instance, so quit looking.
                    break;
                }
            }
        }
    }
    sem_mypost(INSTCACHE);

    if (!found_instances) {
        if (instanceId) {
            LOGINFO("[%s] could not find instance\n", SP(instanceId));
            pMeta->replyString = strdup("requested instance not found");
        } else {
            LOGINFO("no instances running on host %s\n", SP(actionNode));
            pMeta->replyString = strdup("no instances running on the host");
        }
        ret = 1;
        goto out;
    } else if (found_instances > 1 && committing) {
        LOGERROR("internal error: trying to perform a migration commit with multiple (%d) instances\n", found_instances);
        goto out;
    }

    for (int idx = 0; idx < found_instances; idx++) {
        // TO-DO: Wrap alloc()'s.
        nc_instances = EUCA_REALLOC(nc_instances, idx + 1, sizeof(ncInstance *));
        nc_instances[idx] = EUCA_ZALLOC(1, sizeof(ncInstance));
        LOGTRACE("[%s] converting cc_instances[%d] -> nc_instances[%d]\n", cc_instances[idx]->instanceId, idx, idx);
        ccInstance_to_ncInstance(nc_instances[idx], cc_instances[idx]);
        strncpy(nc_instances[idx]->migration_src, resourceCacheLocal.resources[src_index].hostname, HOSTNAME_SIZE);
        // Don't know migration_dst if preparing, will copy that in after scheduling.
        LOGTRACE("[%s] migration hostnames: CC(%s > %s), NC(%s > %s)\n", nc_instances[idx]->instanceId,
                 SP(cc_instances[idx]->migration_src), SP(cc_instances[idx]->migration_dst), nc_instances[idx]->migration_src, nc_instances[idx]->migration_dst);
    }

    if (preparing) {
        for (int idx = 0; idx < found_instances; idx++) {
            if (allowHosts) {
                // destinationHosts is whitelist, pass as includeNodes.
                LOGDEBUG("[%s] scheduling instance with a destination-inclusion list\n", nc_instances[idx]->instanceId);
                rc = schedule_instance_migration(nc_instances[idx], destinationNodes, NULL, destinationNodeCount, 0, src_index, &dst_index, &resourceCacheLocal,
                                                 &(pMeta->replyString));
            } else {
                LOGDEBUG("[%s] scheduling instance with a destination-exclusion list\n", nc_instances[idx]->instanceId);
                // destinationHosts is blacklist, pass as excludeNodes.
                rc = schedule_instance_migration(nc_instances[idx], NULL, destinationNodes, 0, destinationNodeCount, src_index, &dst_index, &resourceCacheLocal,
                                                 &(pMeta->replyString));
            }

            if (rc || (dst_index == -1)) {
                LOGERROR("[%s] cannot schedule destination node for migration from source %s\n", nc_instances[idx]->instanceId, nc_instances[idx]->migration_src);
                ret = 1;
                goto out;
            } else {
                strncpy(nc_instances[idx]->migration_dst, resourceCacheLocal.resources[dst_index].hostname, HOSTNAME_SIZE);
                LOGINFO("[%s] scheduled instance migration from %s to %s\n", nc_instances[idx]->instanceId, nc_instances[idx]->migration_src, nc_instances[idx]->migration_dst);
            }
        }
    }

    if (preparing) {
        // notify source

        // Generate migration credentials.
        const char cred_chars[] = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        struct timeval tv;
        gettimeofday(&tv, NULL);
        srand(tv.tv_sec * tv.tv_usec * getpid());
        for (int ci = 0; ci < (CREDENTIAL_SIZE - 1); ci++) {
            credentials[ci] = cred_chars[rand() % (sizeof(cred_chars) - 1)];
        }
        credentials[CREDENTIAL_SIZE - 1] = 0;

        timeout = ncGetTimeout(time(NULL), OP_TIMEOUT, 1, 0);
        LOGDEBUG("about to ncClientCall source node '%s' with nc_instances (%s %d) [creds='%s'] %s\n",
                 SP(resourceCacheLocal.resources[src_index].hostname), nodeAction, found_instances, credentials, SP(found_instances == 1 ? nc_instances[0]->instanceId : ""));

        //Populate service metadata in request. Needed for ebs-volume attachment
        populateOutboundMeta(pMeta);

        rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[src_index].lockidx, resourceCacheLocal.resources[src_index].ncURL, "ncMigrateInstances",
                          nc_instances, found_instances, nodeAction, credentials);
        if (rc) {
            LOGERROR("failed: request to prepare migration[s] from source %s\n", resourceCacheLocal.resources[src_index].hostname);
            if (pMeta->replyString == NULL) {   // NC did not send back an error string
                pMeta->replyString = strdup("source host was not able to prepare for migration(s)");
            }
            ret = 1;
            goto out;
        } else {
            // Update the instance cache to show the instances as
            // PREPARING pending their reporting back (asynchronously)
            // as READY from their source node[s].
            for (int idx = 0; idx < found_instances; idx++) {
                ccInstance *cacheInstance;
                rc = find_instanceCacheId(cc_instances[idx]->instanceId, &cacheInstance);
                if (!rc) {
                    // found the instance in the cache
                    cacheInstance->migration_state = MIGRATION_PREPARING;
                    refresh_instanceCache(cc_instances[idx]->instanceId, cacheInstance);
                    EUCA_FREE(cacheInstance);
                }
            }
        }

        // notify the destinations, but do it asynchronously so that we can return to caller.
        // (spec says only prepare call to source should block.)
        pid_t pid = fork();
        if (!pid) {
            int inst_succ = 0;
            int inst_fail = 0;

            timeout = ncGetTimeout(time(NULL), OP_TIMEOUT, 1, 0);
            LOGDEBUG("about to ncClientCall destination node[s] with nc_instances (%s %d) [creds='%s']\n", nodeAction, found_instances, credentials);
            for (int idx = 0; idx < found_instances; idx++) {
                LOGDEBUG("[%s] about to ncClientCall destination node '%s' with nc_instances (%s %d) [creds='%s']\n",
                         SP(nc_instances[idx]->instanceId), SP(nc_instances[idx]->migration_dst), nodeAction, 1, credentials);

                dst_index = -1;
                for (int res_idx = 0; res_idx < resourceCacheLocal.numResources && (dst_index == -1); res_idx++) {
                    if (!strcmp(resourceCacheLocal.resources[res_idx].hostname, nc_instances[idx]->migration_dst)) {
                        dst_index = res_idx;
                    }
                }

                if (dst_index < 0) {
                    LOGERROR("[%s] failed: request to prepare migration. Invalid resource index -1.\n", nc_instances[idx]->instanceId);
                    exit(1);
                }
                //Populate service metadata in request. Needed for ebs-volume attachment
                populateOutboundMeta(pMeta);

                rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[dst_index].lockidx, resourceCacheLocal.resources[dst_index].ncURL, "ncMigrateInstances",
                                  &(nc_instances[idx]), 1, nodeAction, credentials);
                if (rc) {
                    LOGERROR("[%s] failed: request to prepare migration on destination %s\n", nc_instances[idx]->instanceId, resourceCacheLocal.resources[dst_index].hostname);
                    ++inst_fail;
                    //exit(1);
                } else {
                    ++inst_succ;
                }
            }
            LOGDEBUG("called destination nodes[s] to prepare for %d incoming migrations[s], %d call[s] succeeded, %d call[s] failed\n", found_instances, inst_succ, inst_fail);
            exit(0);
        } else {
            // parent
            LOGDEBUG("calling destination node[s] asynchronously to prepare for %d incoming migration[s]; using pid %d\n", found_instances, pid);
        }
    } else if (committing) {
        // call commit on source
        timeout = ncGetTimeout(time(NULL), OP_TIMEOUT, 1, 0);
        LOGDEBUG("about to ncClientCall source node '%s' with nc_instances (%s %d) %s\n",
                 SP(resourceCacheLocal.resources[src_index].hostname), nodeAction, found_instances, SP(found_instances == 1 ? nc_instances[0]->instanceId : ""));

        //Populate service metadata in request. Needed for ebs-volume attachment
        populateOutboundMeta(pMeta);

        // No need to send credentials with commit call: they were already passed to source and destination during prepare call.
        rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[src_index].lockidx, resourceCacheLocal.resources[src_index].ncURL, "ncMigrateInstances",
                          nc_instances, found_instances, nodeAction, NULL);
        if (rc) {
            LOGERROR("failed: request to commit migration on source\n");
            ret = 1;
            goto out;
        }
    } else if (rollback) {
        // call rollback on node--could be source or destination
        timeout = ncGetTimeout(time(NULL), OP_TIMEOUT, 1, 0);

        dst_index = -1;
        for (int res_idx = 0; res_idx < resourceCacheLocal.numResources && (dst_index == -1); res_idx++) {
            if (!strcmp(resourceCacheLocal.resources[res_idx].hostname, actionNode)) {
                dst_index = res_idx;
            }
        }

        if (dst_index < 0) {
            LOGERROR("failed: request to roll back migration on node. Invalid destination index\n");
            ret = 1;
            goto out;
        }
        // Might not have migration_dst in instance struct here if rollback.
        // TO-DO: In some cases, such as certain rollback requests to source node, this can put the source node's IP in ->migration_dst. Debug.
        if (!strlen(nc_instances[0]->migration_dst)) {
            strncpy(nc_instances[0]->migration_dst, resourceCacheLocal.resources[dst_index].hostname, HOSTNAME_SIZE);
        }

        LOGDEBUG("about to ncClientCall node %s (> %s) with nc_instances (%s %d) %s using URL %s\n",
                 SP(resourceCacheLocal.resources[dst_index].hostname), SP(found_instances == 1 ? nc_instances[0]->migration_dst : ""), nodeAction, found_instances,
                 SP(found_instances == 1 ? nc_instances[0]->instanceId : ""), resourceCacheLocal.resources[dst_index].ncURL);

        //Populate service metadata in request. Needed for ebs-volume attachment
        populateOutboundMeta(pMeta);

        rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[dst_index].lockidx, resourceCacheLocal.resources[dst_index].ncURL, "ncMigrateInstances",
                          nc_instances, found_instances, nodeAction, NULL);
        if (rc) {
            LOGERROR("failed: request to roll back migration on node\n");
            ret = 1;
            goto out;
        }
    } else {
        LOGERROR("failed: unknown or unsupported migration action: %s\n", nodeAction);
        pMeta->replyString = strdup("internal error (unrecognized action)");
        ret = 1;
        goto out;
    }

out:
    for (int z = 0; z < found_instances; z++) {
        EUCA_FREE(cc_instances[z]);
        EUCA_FREE(nc_instances[z]);
    }
    EUCA_FREE(cc_instances);
    EUCA_FREE(nc_instances);

    LOGTRACE("done\n");

    shawn();

    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doStartInstance(ncMetadata * pMeta, char *instanceId)
{
    int j = 0;
    int rc = 0;
    int start = 0;
    int stop = 0;
    int ret = 0;
    int timeout = 0;
    int done = 0;
    ccInstance *myInstance = NULL;
    time_t op_start = time(NULL);
    ccResourceCache resourceCacheLocal;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled())
        return (1);

    LOGINFO("[%s] instance start requested\n", SP(instanceId));
    LOGDEBUG("invoked: instanceId=%s userId=%s\n", SP(instanceId), SP(pMeta ? pMeta->userId : "UNSET"));
    if (instanceId == NULL) {
        LOGERROR("bad input params\n");
        return (1);
    }

    sem_mywait(RESCACHE);
    {
        memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    }
    sem_mypost(RESCACHE);

    if ((rc = find_instanceCacheId(instanceId, &myInstance)) == 0) {
        // found the instance in the cache
        if (myInstance) {
            start = myInstance->ncHostIdx;
            stop = start + 1;
            EUCA_FREE(myInstance);
            myInstance = NULL;
        }
    } else {
        start = 0;
        stop = resourceCacheLocal.numResources;
    }

    done = 0;
    for (j = start; ((j < stop) && !done); j++) {
        timeout = ncGetTimeout(op_start, OP_TIMEOUT, (stop - start), j);
        rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncStartInstance", instanceId);
        if (rc) {
            ret = 1;
        } else {
            ret = 0;
            done++;
        }
    }

    LOGTRACE("done\n");
    shawn();
    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doStopInstance(ncMetadata * pMeta, char *instanceId)
{
    int j = 0;
    int rc = 0;
    int start = 0;
    int stop = 0;
    int ret = 0;
    int timeout = 0;
    int done = 0;
    ccInstance *myInstance = NULL;
    time_t op_start = time(NULL);
    ccResourceCache resourceCacheLocal;

    rc = initialize(pMeta, FALSE);
    if (rc || ccIsEnabled())
        return (1);

    LOGINFO("[%s] instance shutdown requested\n", SP(instanceId));
    LOGDEBUG("invoked: instanceId=%s userId=%s\n", SP(instanceId), SP(pMeta ? pMeta->userId : "UNSET"));
    if (instanceId == NULL) {
        LOGERROR("bad input params\n");
        return (1);
    }

    sem_mywait(RESCACHE);
    {
        memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
    }
    sem_mypost(RESCACHE);

    if ((rc = find_instanceCacheId(instanceId, &myInstance)) == 0) {
        // found the instance in the cache
        if (myInstance) {
            start = myInstance->ncHostIdx;
            stop = start + 1;
            EUCA_FREE(myInstance);
            myInstance = NULL;
        }
    } else {
        start = 0;
        stop = resourceCacheLocal.numResources;
    }

    done = 0;
    for (j = start; ((j < stop) && !done); j++) {
        timeout = ncGetTimeout(op_start, OP_TIMEOUT, (stop - start), j);
        rc = ncClientCall(pMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncStopInstance", instanceId);
        if (rc) {
            ret = 1;
        } else {
            ret = 0;
            done++;
        }
    }

    LOGTRACE("done\n");
    shawn();
    return (ret);
}

//!
//!
//!
//! @param[in] buf
//! @param[in] bufname
//! @param[in] bytes
//! @param[in] lock
//! @param[in] lockname
//! @param[in] mode
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int setup_shared_buffer(void **buf, char *bufname, size_t bytes, sem_t ** lock, char *lockname, int mode)
{
    int shd, rc, ret;

    // create a lock and grab it
    *lock = sem_open(lockname, O_CREAT, 0644, 1);
    sem_wait(*lock);
    ret = 0;

    if (mode == SHARED_MEM) {
        // set up shared memory segment for config
        shd = shm_open(bufname, O_CREAT | O_RDWR | O_EXCL, 0644);
        if (shd >= 0) {
            // if this is the first process to create the config, init to 0
            rc = ftruncate(shd, bytes);
        } else {
            shd = shm_open(bufname, O_CREAT | O_RDWR, 0644);
        }
        if (shd < 0) {
            fprintf(stderr, "cannot initialize shared memory segment\n");
            sem_post(*lock);
            sem_close(*lock);
            return (1);
        }
        *buf = mmap(0, bytes, PROT_READ | PROT_WRITE, MAP_SHARED, shd, 0);
    } else if (mode == SHARED_FILE) {
        char *tmpstr, path[EUCA_MAX_PATH];
        struct stat mystat;
        int fd;

        tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME);
        if (!tmpstr) {
            snprintf(path, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/CC/%s", "", bufname);
        } else {
            snprintf(path, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/CC/%s", tmpstr, bufname);
        }
        fd = open(path, O_RDWR | O_CREAT, 0600);
        if (fd < 0) {
            fprintf(stderr, "ERROR: cannot open/create '%s' to set up mmapped buffer\n", path);
            ret = 1;
        } else {
            mystat.st_size = 0;
            rc = fstat(fd, &mystat);
            // this is the check to make sure we're dealing with a valid prior config
            if (mystat.st_size != bytes) {
                rc = ftruncate(fd, bytes);
            }
            *buf = mmap(NULL, bytes, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
            if (*buf == NULL) {
                fprintf(stderr, "ERROR: cannot mmap fd\n");
                ret = 1;
            }
            close(fd);
        }
    }
    sem_post(*lock);
    return (ret);
}

//copy from cc config into message metadata.
static int populateOutboundMeta(ncMetadata * pMeta)
{
    int i = 0;
    if (pMeta != NULL) {
        sem_mywait(CONFIG);
        memcpy(pMeta->services, config->services, sizeof(serviceInfoType) * 16);
        memcpy(pMeta->disabledServices, config->disabledServices, sizeof(serviceInfoType) * 16);
        memcpy(pMeta->notreadyServices, config->notreadyServices, sizeof(serviceInfoType) * 16);

        //Update the epoch if not already populated with latest from CC
        if (pMeta->epoch < config->ccStatus.localEpoch) {
            pMeta->epoch = config->ccStatus.localEpoch;
        }
        sem_mypost(CONFIG);

        pMeta->servicesLen = 0;
        pMeta->disabledServicesLen = 0;
        pMeta->notreadyServicesLen = 0;
        for (i = 0; i < 16; i++) {
            if (strlen(config->services[i].name) > 0) {
                pMeta->servicesLen++;
            }
            if (strlen(config->disabledServices[i].name) > 0) {
                pMeta->disabledServicesLen++;
            }
            if (strlen(config->notreadyServices[i].name) > 0) {
                pMeta->notreadyServicesLen++;
            }
        }

    } else {
        return (EUCA_ERROR);
    }

    return (EUCA_OK);
}

//!
//! Initialization function called by all handlers. Each handler call may
//! go to a new process (as decided by apache), so this function does a lot
//! to set up shared memory, logging, fault subsystem, etc, for the process.
//! Since it is called for every request from CLC, this is also where we
//! update state based on the latest values from CLC:
//!
//!   * cloudIp
//!   * epoch
//!   * services (enabled / disabled / not ready)
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] authoritative indicates whether this request is allowed to reset epoch
//! @return
//!
//! @pre
//!
//! @note
//!
int initialize(ncMetadata * pMeta, boolean authoritative)
{
    int rc, ret;

    ret = 0;
    rc = init_thread();
    if (rc) {
        ret = 1;
        LOGERROR("cannot initialize thread\n");
    }

    rc = init_log();
    if (rc) {
        ret = 1;
        LOGERROR("cannot initialize local state\n");
    }

    rc = init_eucafaults("cc");        // Returns # of faults loaded into registry.
    if (!rc) {
        LOGERROR("cannot initialize eucafault registry at startup--will retry initialization upon detection of any faults.\n");
    }

    rc = init_config();
    if (rc) {
        ret = 1;
        LOGERROR("cannot initialize from configuration file\n");
    }

    if (config->use_tunnels) {
        rc = vnetInitTunnels(vnetconfig);
        if (rc) {
            LOGERROR("cannot initialize tunnels\n");
        }
    }

    rc = init_pthreads();
    if (rc) {
        LOGERROR("cannot initialize background threads\n");
        ret = 1;
    }

    if (pMeta != NULL) {
        LOGTRACE("pMeta: userId=%s correlationId=%s\n", SP(pMeta->userId), pMeta->correlationId);
    }

    if (!ret) {
        // store information from CLC that needs to be kept up-to-date in the CC
        if (pMeta != NULL) {
            int i;
            sem_mywait(CONFIG);
            LOGTRACE("pMeta: epoch=%d (vs %d) enabled %d, disabled %d, notready %d\n",
                     pMeta->epoch, config->ccStatus.localEpoch, pMeta->servicesLen, pMeta->disabledServicesLen, pMeta->notreadyServicesLen);
            if (pMeta->epoch >= config->ccStatus.localEpoch ||  // we missed some updates, so let us catch up
                authoritative) {       // trust the authoritative requests and always take their services info
                memcpy(config->services, pMeta->services, sizeof(serviceInfoType) * 16);
                memcpy(config->disabledServices, pMeta->disabledServices, sizeof(serviceInfoType) * 16);
                memcpy(config->notreadyServices, pMeta->notreadyServices, sizeof(serviceInfoType) * 16);
                config->ccStatus.localEpoch = pMeta->epoch;
            }

            for (i = 0; i < 16; i++) {
                if (strlen(config->services[i].type)) {
                    // search for this CCs serviceInfoType
                    /*  if (!strcmp(config->services[i].type, "cluster")) {
                       char uri[EUCA_MAX_PATH], uriType[32], host[EUCA_MAX_PATH], path[EUCA_MAX_PATH];
                       int port, done;
                       snprintf(uri, EUCA_MAX_PATH, "%s", config->services[i].uris[0]);
                       rc = tokenize_uri(uri, uriType, host, &port, path);
                       if (strlen(host)) {
                       done=0;
                       for (j=0; j<32 && !done; j++) {
                       uint32_t hostip;
                       hostip = dot2hex(host);
                       if (hostip == vnetconfig->localIps[j]) {
                       // found a match, update local serviceInfoType
                       memcpy(&(config->ccStatus.serviceId), &(config->services[i]), sizeof(serviceInfoType));
                       done++;
                       }
                       }
                       }
                       } else */
                    if (!strcmp(config->services[i].type, "eucalyptus")) {
                        char uri[EUCA_MAX_PATH], uriType[32], host[EUCA_MAX_PATH], path[EUCA_MAX_PATH];
                        int port;
                        // this is the cloud controller serviceInfo
                        snprintf(uri, EUCA_MAX_PATH, "%s", config->services[i].uris[0]);
                        rc = tokenize_uri(uri, uriType, host, &port, path);
                        if (strlen(host)) {
                            config->cloudIp = dot2hex(host);
                        }
                    }
                }
            }
            sem_mypost(CONFIG);
        }

        sem_mywait(INIT);
        if (!init) {
            // first time operations with everything initialized
            sem_mywait(VNET);
            vnetconfig->cloudIp = 0;
            sem_mypost(VNET);
            sem_mywait(CONFIG);
            config->cloudIp = 0;
            sem_mypost(CONFIG);
        }
        // initialization went well, this thread is now initialized
        init = 1;
        sem_mypost(INIT);
    }

    return (ret);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int ccIsEnabled(void)
{
    // initialized, but ccState is disabled (refuse to service operations)

    if (!config || config->ccState != ENABLED) {
        return (1);
    }
    return (0);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int ccIsDisabled(void)
{
    // initialized, but ccState is disabled (refuse to service operations)

    if (!config || config->ccState != DISABLED) {
        return (1);
    }
    return (0);
}

//!
//!
//!
//! @param[in] newstate
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int ccChangeState(int newstate)
{
    if (config) {
        if (config->ccState == SHUTDOWNCC) {
            // CC is to be shut down, there is no transition out of this state
            return (0);
        }
        char localState[32];
        config->ccLastState = config->ccState;
        config->ccState = newstate;
        ccGetStateString(localState, 32);
        snprintf(config->ccStatus.localState, 32, "%s", localState);
        return (0);
    }
    return (1);
}

//!
//!
//!
//! @param[in] statestr
//! @param[in] n
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int ccGetStateString(char *statestr, int n)
{
    if (config->ccState == ENABLED) {
        snprintf(statestr, n, "ENABLED");
    } else if (config->ccState == DISABLED) {
        snprintf(statestr, n, "DISABLED");
    } else if (config->ccState == STOPPED) {
        snprintf(statestr, n, "STOPPED");
    } else if (config->ccState == LOADED) {
        snprintf(statestr, n, "LOADED");
    } else if (config->ccState == INITIALIZED) {
        snprintf(statestr, n, "INITIALIZED");
    } else if (config->ccState == PRIMORDIAL) {
        snprintf(statestr, n, "PRIMORDIAL");
    } else if (config->ccState == NOTREADY || config->ccState == SHUTDOWNCC) {
        snprintf(statestr, n, "NOTREADY");
    }
    return (0);
}

//!
//!
//!
//! @param[in] clcTimer
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int ccCheckState(int clcTimer)
{
    int rc = EUCA_OK;
    int ret = 0;
    char cmd[EUCA_MAX_PATH];
    char localDetails[1024] = "";

    if (!config) {
        return (1);
    }
    // check local configuration
    if (config->ccState == SHUTDOWNCC) {
        LOGINFO("this cluster controller marked as shut down\n");
        ret++;
    }
    // configuration
    {
        char cmd[EUCA_MAX_PATH];
        snprintf(cmd, EUCA_MAX_PATH, "%s", config->eucahome);
        if (check_directory(cmd)) {
            LOGERROR("cannot find directory '%s'\n", cmd);
            ret++;
        }
    }

    // shellouts
    {
        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, config->eucahome);
        if (check_file(cmd)) {
            LOGERROR("cannot find shellout '%s'\n", cmd);
            ret++;
        }

        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_HELPER_DIR "/dynserv.pl", config->eucahome);
        if (check_file(cmd)) {
            LOGERROR("cannot find shellout '%s'\n", cmd);
            ret++;
        }

        if (euca_execlp(NULL, "ip", "addr", "show", NULL) != EUCA_OK) {
            LOGERROR("cannot run shellout 'ip addr show'\n");
            ret++;
        }
    }
    // filesystem

    // network
    // arbitrators
    if (clcTimer == 1 && strlen(config->arbitrators)) {
        char *tok, buf[256], *host;
        uint32_t hostint;
        int count = 0;
        int arbitratorFails = 0;
        snprintf(buf, 255, "%s", config->arbitrators);
        tok = strtok(buf, " ");
        while (tok && count < 3) {
            hostint = dot2hex(tok);
            host = hex2dot(hostint);
            if (host) {
                LOGDEBUG("checking health of arbitrator (%s)\n", tok);
                if ((rc = euca_execlp(NULL, "ping", "-c", "1", host, NULL)) != EUCA_OK) {
                    LOGDEBUG("cannot ping arbitrator %s (ping rc=%d)\n", host, rc);
                    arbitratorFails++;
                }
                EUCA_FREE(host);
            }
            tok = strtok(NULL, " ");
            count++;
        }
        if (arbitratorFails) {
            config->arbitratorFails++;
        } else {
            config->arbitratorFails = 0;
        }

        if (config->arbitratorFails > 10) {
            LOGDEBUG("more than 10 arbitrator ping fails in a row (%d), failing check\n", config->arbitratorFails);
            ret++;
        }
    }
    // broker pairing algo
    rc = doBrokerPairing();
    if (rc) {
        ret++;
    }

    snprintf(localDetails, 1023, "ERRORS=%d", ret);
    snprintf(config->ccStatus.details, 1023, "%s", localDetails);

    return (ret);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doBrokerPairing(void)
{
    int ret, local_broker_down, i, is_ha_cc, port;
    char buri[EUCA_MAX_PATH], uriType[32], bhost[EUCA_MAX_PATH], path[EUCA_MAX_PATH], curi[EUCA_MAX_PATH], chost[EUCA_MAX_PATH];

    ret = 0;
    local_broker_down = 0;
    is_ha_cc = 0;

    snprintf(curi, EUCA_MAX_PATH, "%s", config->ccStatus.serviceId.uris[0]);
    bzero(chost, sizeof(char) * EUCA_MAX_PATH);
    tokenize_uri(curi, uriType, chost, &port, path);

    //enabled
    for (i = 0; i < 16; i++) {
        if (!strcmp(config->ccStatus.serviceId.name, "self")) {
            //      LOGDEBUG("local CC service info not yet initialized\n");
        } else if (!memcmp(&(config->ccStatus.serviceId), &(config->services[i]), sizeof(serviceInfoType))) {
            //      LOGDEBUG("found local CC information in services()\n");
        } else if (!strcmp(config->services[i].type, "cluster") && !strcmp(config->services[i].partition, config->ccStatus.serviceId.partition)) {
            // service is not 'me', but is a 'cluster' and in has the same 'partition', must be in HA mode
            //      LOGDEBUG("CC is in HA mode\n");
            is_ha_cc = 1;
        }
    }
    //disabled
    for (i = 0; i < 16; i++) {
        if (!strcmp(config->ccStatus.serviceId.name, "self")) {
            //      LOGDEBUG("local CC service info not yet initialized\n");
        } else if (!memcmp(&(config->ccStatus.serviceId), &(config->disabledServices[i]), sizeof(serviceInfoType))) {
            //      LOGDEBUG("found local CC information in disabled services()\n");
        } else if (!strcmp(config->disabledServices[i].type, "cluster")
                   && !strcmp(config->disabledServices[i].partition, config->ccStatus.serviceId.partition)) {
            // service is not 'me', but is a 'cluster' and in has the same 'partition', must be in HA mode
            //      LOGDEBUG("CC is in HA mode\n");
            is_ha_cc = 1;
        }
    }
    //notready
    for (i = 0; i < 16; i++) {
        int j;
        //test
        if (!strcmp(config->ccStatus.serviceId.name, "self")) {
            //      LOGDEBUG("local CC service info not yet initialized\n");
        } else if (!memcmp(&(config->ccStatus.serviceId), &(config->notreadyServices[i]), sizeof(serviceInfoType))) {
            //      LOGDEBUG("found local CC information in notreadyServices()\n");
        } else if (!strcmp(config->notreadyServices[i].type, "cluster")
                   && !strcmp(config->notreadyServices[i].partition, config->ccStatus.serviceId.partition)) {
            // service is not 'me', but is a 'cluster' and in has the same 'partition', must be in HA mode
            //      LOGDEBUG("CC is in HA mode\n");
            is_ha_cc = 1;
        }

        if (strlen(config->notreadyServices[i].type)) {
            if (!strcmp(config->notreadyServices[i].type, "vmwarebroker")) {
                for (j = 0; j < MAX_SERVICE_URIS; j++) {
                    if (strlen(config->notreadyServices[i].uris[j])) {
                        LOGDEBUG("found broker - %s\n", config->notreadyServices[i].uris[j]);

                        snprintf(buri, EUCA_MAX_PATH, "%s", config->notreadyServices[i].uris[j]);
                        bzero(bhost, sizeof(char) * EUCA_MAX_PATH);
                        tokenize_uri(buri, uriType, bhost, &port, path);

                        LOGDEBUG("comparing found not ready broker host (%s) with local CC host (%s)\n", bhost, chost);
                        if (!strcmp(chost, bhost)) {
                            LOGWARN("detected local broker (%s) matching local CC (%s) in NOTREADY state\n", bhost, chost);
                            //          ret++;
                            local_broker_down = 1;
                        }
                    }
                }
            }
        }
    }

    if (local_broker_down && is_ha_cc) {
        LOGDEBUG("detected CC in HA mode, and local broker is not ENABLED\n");
        ret++;
    }
    return (ret);
}

//!
//! The CC will start a background thread to poll its collection of nodes. This thread populates an
//! in-memory cache of instance and resource information that can be accessed via the regular describeInstances
//! and describeResources calls to the CC.  The purpose of this separation is to allow for a more scalable
//! framework where describe operations do not block on access to node controllers.
//!
//! @param[in] in
//!
//! @return
//!
//! @pre
//!
//! @note
//!
void *monitor_thread(void *in)
{
    int rc, ncTimer, clcTimer, ncSensorsTimer, ncRefresh = 0, clcRefresh = 0, ncSensorsRefresh = 0;
    ncMetadata pMeta;
    char pidfile[EUCA_MAX_PATH], *pidstr = NULL;

    bzero(&pMeta, sizeof(ncMetadata));
    pMeta.correlationId = strdup("monitor");
    pMeta.userId = strdup("eucalyptus");
    if (!pMeta.correlationId || !pMeta.userId) {
        LOGFATAL("out of memory!\n");
        unlock_exit(1);
    }
    // set up default signal handler for this child process (for SIGTERM)
    struct sigaction newsigact;
    newsigact.sa_handler = SIG_DFL;
    newsigact.sa_flags = 0;
    sigemptyset(&newsigact.sa_mask);
    sigprocmask(SIG_SETMASK, &newsigact.sa_mask, NULL);
    sigaction(SIGTERM, &newsigact, NULL);

    // add 1 to each Timer so they will all fire upon the first loop iteration
    ncTimer = config->ncPollingFrequency + 1;
    clcTimer = config->clcPollingFrequency + 1;
    ncSensorsTimer = config->ncSensorsPollingInterval + 1;

    while (1) {
        LOGTRACE("running\n");

        if (config->kick_enabled) {
            ccChangeState(ENABLED);
            config->kick_enabled = 0;
        }

        rc = update_config();
        if (rc) {
            LOGWARN("bad return from update_config(), check your config file\n");
        }

        if (config->ccState == ENABLED) {

            // NC Polling operations
            if (ncTimer >= config->ncPollingFrequency) {
                ncTimer = 0;
                ncRefresh = 1;
            }
            ncTimer++;

            // CLC Polling operations
            if (clcTimer >= config->clcPollingFrequency) {
                clcTimer = 0;
                clcRefresh = 1;
            }
            clcTimer++;

            // NC Sensors Polling operation
            if (ncSensorsTimer >= config->ncSensorsPollingInterval) {
                ncSensorsTimer = 0;
                ncSensorsRefresh = 1;
            }
            ncSensorsTimer++;

            if (ncRefresh) {
                rc = refresh_resources(&pMeta, 60, 1);
                if (rc) {
                    LOGWARN("call to refresh_resources() failed in monitor thread\n");
                }

                rc = refresh_instances(&pMeta, 60, 1);
                if (rc) {
                    LOGWARN("call to refresh_instances() failed in monitor thread\n");
                }
            }

            if (config->kick_broadcast_network_info) {
                rc = broadcast_network_info(&pMeta, 60, 1);
                if (rc) {
                    LOGWARN("call to broadcast_network_info() failed in monitor thread\n");
                }
                config->kick_broadcast_network_info = 0;
            }

            {                          // print a periodic summary of instances in the log
                static time_t last_log_update = 0;

                int res_idle = 0, res_busy = 0, res_bad = 0;
                sem_mywait(RESCACHE);
                for (int i = 0; i < resourceCache->numResources; i++) {
                    ccResource *res = &(resourceCache->resources[i]);
                    if (res->state == RESDOWN) {
                        res_bad++;
                    } else {
                        if (res->maxCores != res->availCores) {
                            res_busy++;
                        } else {
                            res_idle++;
                        }
                    }
                }
                sem_mypost(RESCACHE);

                int num_pending = 0, num_extant = 0, num_teardown = 0;
                sem_mywait(INSTCACHE);
                if (instanceCache->numInsts) {
                    for (int i = 0; i < MAXINSTANCES_PER_CC; i++) {
                        if (!strcmp(instanceCache->instances[i].state, "Pending")) {
                            num_teardown++;
                        } else if (!strcmp(instanceCache->instances[i].state, "Extant")) {
                            num_extant++;
                        } else if (!strcmp(instanceCache->instances[i].state, "Teardown")) {
                            num_teardown++;
                        }
                    }
                }
                sem_mypost(INSTCACHE);

                time_t now = time(NULL);
                if ((now - last_log_update) > LOG_INTERVAL_SUMMARY_SEC) {
                    last_log_update = now;
                    LOGINFO("instances: %04d (%04d extant + %04d pending + %04d terminated)\n", (num_pending + num_extant + num_teardown), num_extant, num_pending, num_teardown);
                    LOGINFO("    nodes: %04d (%04d busy + %04d idle + %04d unresponsive)\n", (res_busy + res_idle + res_bad), res_busy, res_idle, res_bad);
                }
            }

            if (ncSensorsRefresh) {
                rc = refresh_sensors(&pMeta, 60, 1);
                if (rc == 0) {
                    // refresh_sensors() only returns non-zero when sensor subsystem has not been initialized.
                    // Until it is initialized, keep checking every second, so that sensory subsystems on NCs are
                    // initialized soon after it is initialized on the CC (otherwise it may take a while and NC
                    // may miss initial measurements from early instances). Once initialized, refresh can happen
                    // as configured by config->ncSensorsPollingInterval.
                    ncSensorsRefresh = 0;
                }
            }

            if (ncRefresh) {
                if (is_clean_instanceCache()) {
                    // Network state operations
                    //  sem_mywait(RESCACHE);

                    LOGDEBUG("syncing network state\n");
                    rc = syncNetworkState();
                    if (rc) {
                        LOGDEBUG("syncNetworkState() triggering network restore\n");
                        config->kick_network = 1;
                    }
                    //    sem_mypost(RESCACHE);

                    if (config->kick_network) {
                        LOGDEBUG("restoring network state\n");
                        rc = restoreNetworkState();
                        if (rc) {
                            // failed to restore network state, continue
                            LOGWARN("restoreNetworkState returned false (may be already restored)\n");
                        } else {
                            sem_mywait(CONFIG);
                            config->kick_network = 0;
                            sem_mypost(CONFIG);
                        }
                    }
                } else {
                    LOGDEBUG("instanceCache is dirty, skipping network update\n");
                }
            }

            if (clcRefresh) {
                LOGDEBUG("syncing CLC network rules ground truth with local state\n");
                rc = reconfigureNetworkFromCLC();
                if (rc) {
                    LOGWARN("cannot get network ground truth from CLC\n");
                }
            }

            if (ncRefresh) {
                LOGDEBUG("maintaining network state\n");
                rc = maintainNetworkState();
                if (rc) {
                    LOGERROR("network state maintainance failed\n");
                }
            }

            if (config->use_proxy) {
                rc = image_cache_invalidate();
                if (rc) {
                    LOGERROR("cannot invalidate image cache\n");
                }
                snprintf(pidfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/httpd-dynserv.pid", config->eucahome);
                pidstr = file2str(pidfile);
                if (pidstr) {
                    if (check_process(atoi(pidstr), "dynserv-httpd.conf")) {
                        rc = image_cache_proxykick(resourceCache->resources, &(resourceCache->numResources));
                        if (rc) {
                            LOGERROR("could not start proxy cache\n");
                        }
                    }
                    EUCA_FREE(pidstr);
                } else {
                    rc = image_cache_proxykick(resourceCache->resources, &(resourceCache->numResources));
                    if (rc) {
                        LOGERROR("could not start proxy cache\n");
                    }
                }
            }
            config->kick_monitor_running = 1;
        } else {
            // this CC is not enabled, ensure that local network state is disabled
            rc = clean_network_state();
            if (rc) {
                LOGERROR("could not cleanup network state\n");
            }
        }

        // do state checks under CONFIG lock
        sem_mywait(CONFIG);
        if (ccCheckState(clcTimer)) {
            LOGERROR("ccCheckState() returned failures\n");
            config->kick_enabled = 0;
            ccChangeState(NOTREADY);
        } else if (config->ccState == NOTREADY) {
            ccChangeState(DISABLED);
        }
        sem_mypost(CONFIG);
        shawn();

        LOGTRACE("localState=%s - done.\n", config->ccStatus.localState);
        //sleep(config->ncPollingFrequency);
        ncRefresh = clcRefresh = 0;
        sleep(1);
    }

    EUCA_FREE(pMeta.correlationId);
    EUCA_FREE(pMeta.userId);
    return (NULL);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int init_pthreads(void)
{
    // start any background threads
    if (!config_init) {
        return (1);
    }
    sem_mywait(CONFIG);

    if (sensor_initd == 0) {
        sem *s = sem_alloc_posix(locks[SENSORCACHE]);
        if (config->threads[SENSOR] == 0 || check_process(config->threads[SENSOR], NULL)) {
            int pid;
            pid = fork();
            if (!pid) {
                // set up default signal handler for this child process (for SIGTERM)
                struct sigaction newsigact = { {NULL} };
                newsigact.sa_handler = SIG_DFL;
                newsigact.sa_flags = 0;
                sigemptyset(&newsigact.sa_mask);
                sigprocmask(SIG_SETMASK, &newsigact.sa_mask, NULL);
                sigaction(SIGTERM, &newsigact, NULL);
                LOGDEBUG("sensor polling process running\n");
                LOGDEBUG("calling sensor_init() to not return.\n");
                if (sensor_init(s, ccSensorResourceCache, MAX_SENSOR_RESOURCES, TRUE, update_config) != EUCA_OK)    // this call will not return
                    LOGERROR("failed to invoke the sensor polling process\n");
                exit(0);
            } else {
                config->threads[SENSOR] = pid;
            }
        }
        LOGDEBUG("calling sensor_init(..., NULL) to return.\n");
        if (sensor_init(s, ccSensorResourceCache, MAX_SENSOR_RESOURCES, FALSE, NULL) != EUCA_OK) {  // this call will return
            LOGERROR("failed to initialize sensor subsystem in this process\n");
        } else {
            LOGDEBUG("sensor subsystem initialized in this process\n");
            sensor_initd = 1;
        }
    }
    // sensor initialization should preceed monitor thread creation so
    // that monitor thread has its sensor subsystem initialized

    if (config->threads[MONITOR] == 0 || check_process(config->threads[MONITOR], "httpd-cc.conf")) {
        int pid;
        pid = fork();
        if (!pid) {
            // set up default signal handler for this child process (for SIGTERM)
            struct sigaction newsigact = { {NULL} };
            newsigact.sa_handler = SIG_DFL;
            newsigact.sa_flags = 0;
            sigemptyset(&newsigact.sa_mask);
            sigprocmask(SIG_SETMASK, &newsigact.sa_mask, NULL);
            sigaction(SIGTERM, &newsigact, NULL);
            config->kick_dhcp = 1;
            config->kick_network = 1;
            monitor_thread(NULL);
            exit(0);
        } else {
            config->threads[MONITOR] = pid;
        }
    }

    sem_mypost(CONFIG);

    return (0);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int init_log(void)
{
    char logFile[EUCA_MAX_PATH], configFiles[2][EUCA_MAX_PATH], home[EUCA_MAX_PATH];

    if (local_init == 0) {             // called by this process for the first time

        //! @TODO code below is replicated in init_config(), it would be good to join them
        bzero(logFile, EUCA_MAX_PATH);
        bzero(home, EUCA_MAX_PATH);
        bzero(configFiles[0], EUCA_MAX_PATH);
        bzero(configFiles[1], EUCA_MAX_PATH);

        char *tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME);
        if (!tmpstr) {
            snprintf(home, EUCA_MAX_PATH, "/");
        } else {
            snprintf(home, EUCA_MAX_PATH, "%s", tmpstr);
        }

        snprintf(configFiles[1], EUCA_MAX_PATH, EUCALYPTUS_CONF_LOCATION, home);
        snprintf(configFiles[0], EUCA_MAX_PATH, EUCALYPTUS_CONF_OVERRIDE_LOCATION, home);
        snprintf(logFile, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR "/cc.log", home);

        configInitValues(configKeysRestartCC, configKeysNoRestartCC);   // initialize config subsystem
        readConfigFile(configFiles, 2);

        char *log_prefix;
        configReadLogParams(&(config->log_level), &(config->log_roll_number), &(config->log_max_size_bytes), &log_prefix);
        if (log_prefix && strlen(log_prefix) > 0) {
            euca_strncpy(config->log_prefix, log_prefix, sizeof(config->log_prefix));
        }
        EUCA_FREE(log_prefix);

        char *log_facility = configFileValue("LOGFACILITY");
        if (log_facility) {
            if (strlen(log_facility) > 0) {
                euca_strncpy(config->log_facility, log_facility, sizeof(config->log_facility));
            }
            EUCA_FREE(log_facility);
        }
        // set the log file path (levels and size limits are set below)
        log_file_set(logFile);

        local_init = 1;
    }
    // update log params on every request so that the updated values discovered
    // by monitoring_thread will get picked up by other processes, too
    log_params_set(config->log_level, (int)config->log_roll_number, config->log_max_size_bytes);
    log_prefix_set(config->log_prefix);
    log_facility_set(config->log_facility, "cc");

    return (0);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int init_thread(void)
{
    int rc, i;

    LOGDEBUG("init=%d %p %p %p %p\n", init, config, vnetconfig, instanceCache, resourceCache);
    if (thread_init) {
        // thread has already been initialized
    } else {
        // this thread has not been initialized, set up shared memory segments
        srand(time(NULL));

        bzero(locks, sizeof(sem_t *) * ENDLOCK);
        bzero(mylocks, sizeof(int) * ENDLOCK);

        locks[INIT] = sem_open("/eucalyptusCCinitLock", O_CREAT, 0644, 1);
        sem_mywait(INIT);

        for (i = NCCALL0; i <= NCCALL31; i++) {
            char lockname[EUCA_MAX_PATH];
            snprintf(lockname, EUCA_MAX_PATH, "/eucalyptusCCncCallLock%d", i);
            locks[i] = sem_open(lockname, O_CREAT, 0644, 1);
        }

        if (config == NULL) {
            rc = setup_shared_buffer((void **)&config, "/eucalyptusCCConfig", sizeof(ccConfig), &(locks[CONFIG]), "/eucalyptusCCConfigLock", SHARED_FILE);
            if (rc != 0) {
                fprintf(stderr, "Cannot set up shared memory region for ccConfig, exiting...\n");
                sem_mypost(INIT);
                exit(1);
            }
        }

        if (instanceCache == NULL) {
            rc = setup_shared_buffer((void **)&instanceCache, "/eucalyptusCCInstanceCache", sizeof(ccInstanceCache), &(locks[INSTCACHE]),
                                     "/eucalyptusCCInstanceCacheLock", SHARED_FILE);
            if (rc != 0) {
                fprintf(stderr, "Cannot set up shared memory region for ccInstanceCache, exiting...\n");
                sem_mypost(INIT);
                exit(1);
            }
        }

        if (resourceCache == NULL) {
            rc = setup_shared_buffer((void **)&resourceCache, "/eucalyptusCCResourceCache", sizeof(ccResourceCache), &(locks[RESCACHE]),
                                     "/eucalyptusCCResourceCacheLock", SHARED_FILE);
            if (rc != 0) {
                fprintf(stderr, "Cannot set up shared memory region for ccResourceCache, exiting...\n");
                sem_mypost(INIT);
                exit(1);
            }
        }

        if (resourceCacheStage == NULL) {
            rc = setup_shared_buffer((void **)&resourceCacheStage, "/eucalyptusCCResourceCacheStage", sizeof(ccResourceCache),
                                     &(locks[RESCACHESTAGE]), "/eucalyptusCCResourceCacheStatgeLock", SHARED_FILE);
            if (rc != 0) {
                fprintf(stderr, "Cannot set up shared memory region for ccResourceCacheStage, exiting...\n");
                sem_mypost(INIT);
                exit(1);
            }
        }

        if (ccSensorResourceCache == NULL) {
            rc = setup_shared_buffer((void **)&ccSensorResourceCache, "/eucalyptusCCSensorResourceCache",
                                     sizeof(sensorResourceCache) + sizeof(sensorResource) * (MAX_SENSOR_RESOURCES - 1), &(locks[SENSORCACHE]),
                                     "/eucalyptusCCSensorResourceCacheLock", SHARED_FILE);
            if (rc != 0) {
                fprintf(stderr, "Cannot set up shared memory region for ccSensorResourceCache, exiting...\n");
                sem_mypost(INIT);
                exit(1);
            }
        }

        if (vnetconfig == NULL) {
            rc = setup_shared_buffer((void **)&vnetconfig, "/eucalyptusCCVNETConfig", sizeof(vnetConfig), &(locks[VNET]), "/eucalyptusCCVNETConfigLock", SHARED_FILE);
            if (rc != 0) {
                fprintf(stderr, "Cannot set up shared memory region for ccVNETConfig, exiting...\n");
                sem_mypost(INIT);
                exit(1);
            }
        }

        if (globalnetworkinfo == NULL) {
            rc = setup_shared_buffer((void **)&globalnetworkinfo, "/eucalyptusCCglobalNetworkInfo", sizeof(globalNetworkInfo), &(locks[GLOBALNETWORKINFO]),
                                     "/eucalyptusCCglobalNetworkInfoLock", SHARED_FILE);
            if (rc != 0) {
                fprintf(stderr, "Cannot set up shared memory region for globalNetworkInfo, exiting...\n");
                sem_mypost(INIT);
                exit(1);
            }
        }

        sem_mypost(INIT);
        thread_init = 1;
    }
    return (0);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int update_config(void)
{
    char *tmpstr = NULL;
    ccResource *res = NULL;
    int rc, numHosts, ret = 0;

    sem_mywait(CONFIG);

    rc = isConfigModified(config->configFiles, 2);
    if (rc < 0) {                      // error
        sem_mypost(CONFIG);
        return (1);
    } else if (rc > 0) {               // config modification time has changed
        rc = readConfigFile(config->configFiles, 2);
        if (rc) {
            // something has changed that can be read in
            LOGINFO("ingressing new options\n");

            // read log params from config file and update in-memory configuration
            char *log_prefix;
            configReadLogParams(&(config->log_level), &(config->log_roll_number), &(config->log_max_size_bytes), &log_prefix);
            if (log_prefix && strlen(log_prefix) > 0) {
                euca_strncpy(config->log_prefix, log_prefix, sizeof(config->log_prefix));
            }
            EUCA_FREE(log_prefix);

            char *log_facility = configFileValue("LOGFACILITY");
            if (log_facility) {
                if (strlen(log_facility) > 0) {
                    euca_strncpy(config->log_facility, log_facility, sizeof(config->log_facility));
                }
                EUCA_FREE(log_facility);
            }
            // reconfigure the logging subsystem to use the new values, if any
            log_params_set(config->log_level, (int)config->log_roll_number, config->log_max_size_bytes);
            log_prefix_set(config->log_prefix);
            log_facility_set(config->log_facility, "cc");

            // NODES
            LOGINFO("refreshing node list\n");
            res = NULL;
            rc = refreshNodes(config, &res, &numHosts);
            if (rc) {
                LOGERROR("cannot read list of nodes, check your config file\n");
                sem_mywait(RESCACHE);
                resourceCache->numResources = 0;
                config->schedState = 0;
                bzero(resourceCache->resources, sizeof(ccResource) * MAXNODES);
                sem_mypost(RESCACHE);
                ret = 1;
            } else {
                sem_mywait(RESCACHE);
                if (numHosts > MAXNODES) {
                    LOGWARN("the list of nodes specified exceeds the maximum number of nodes that a single CC can support (%d). "
                            "Truncating list to %d nodes.\n", MAXNODES, MAXNODES);
                    numHosts = MAXNODES;
                }
                resourceCache->numResources = numHosts;
                config->schedState = 0;
                memcpy(resourceCache->resources, res, sizeof(ccResource) * numHosts);
                sem_mypost(RESCACHE);
            }
            EUCA_FREE(res);

            // CC Arbitrators
            tmpstr = configFileValue("CC_ARBITRATORS");
            if (tmpstr) {
                snprintf(config->arbitrators, 255, "%s", tmpstr);
                EUCA_FREE(tmpstr);
            } else {
                bzero(config->arbitrators, 256);
            }

            // polling frequencies

            // CLC
            tmpstr = configFileValue("CLC_POLLING_FREQUENCY");
            if (tmpstr) {
                if (atoi(tmpstr) > 0) {
                    config->clcPollingFrequency = atoi(tmpstr);
                } else {
                    config->clcPollingFrequency = 6;
                }
                EUCA_FREE(tmpstr);
            } else {
                config->clcPollingFrequency = 6;
            }

            // NC
            tmpstr = configFileValue("NC_POLLING_FREQUENCY");
            if (tmpstr) {
                if (atoi(tmpstr) > 6) {
                    config->ncPollingFrequency = atoi(tmpstr);
                } else {
                    config->ncPollingFrequency = 6;
                }
                EUCA_FREE(tmpstr);
            } else {
                config->ncPollingFrequency = 6;
            }

        }
    }

    sem_mypost(CONFIG);

    return (ret);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int init_config(void)
{
    ccResource *res = NULL;
    char *tmpstr = NULL, *proxyIp = NULL;
    int rc, numHosts, use_wssec, use_tunnels, use_proxy, proxy_max_cache_size, schedPolicy, idleThresh, wakeThresh, i;

    char configFiles[2][EUCA_MAX_PATH], netPath[EUCA_MAX_PATH], eucahome[EUCA_MAX_PATH], policyFile[EUCA_MAX_PATH], home[EUCA_MAX_PATH], proxyPath[EUCA_MAX_PATH], arbitrators[256],
        schedPath[EUCA_MAX_PATH];

    time_t instanceTimeout, ncPollingFrequency, clcPollingFrequency, ncFanout;

    // read in base config information
    tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME);
    if (!tmpstr) {
        snprintf(home, EUCA_MAX_PATH, "/");
    } else {
        snprintf(home, EUCA_MAX_PATH, "%s", tmpstr);
    }

    bzero(configFiles[0], EUCA_MAX_PATH);
    bzero(configFiles[1], EUCA_MAX_PATH);
    bzero(netPath, EUCA_MAX_PATH);
    bzero(policyFile, EUCA_MAX_PATH);
    bzero(schedPath, EUCA_MAX_PATH);

    snprintf(configFiles[1], EUCA_MAX_PATH, EUCALYPTUS_CONF_LOCATION, home);
    snprintf(configFiles[0], EUCA_MAX_PATH, EUCALYPTUS_CONF_OVERRIDE_LOCATION, home);
    snprintf(netPath, EUCA_MAX_PATH, CC_NET_PATH_DEFAULT, home);
    snprintf(policyFile, EUCA_MAX_PATH, EUCALYPTUS_KEYS_DIR "/nc-client-policy.xml", home);
    snprintf(eucahome, EUCA_MAX_PATH, "%s/", home);

    sem_mywait(INIT);

    if (config_init && config->initialized) {
        // this means that this thread has already been initialized
        sem_mypost(INIT);
        return (0);
    }

    if (config->initialized) {
        config_init = 1;
        sem_mypost(INIT);
        return (0);
    }

    LOGINFO("initializing CC configuration\n");

    configInitValues(configKeysRestartCC, configKeysNoRestartCC);
    readConfigFile(configFiles, 2);

    // DHCP configuration section
    {
        char *daemon = NULL,
            *dhcpuser = NULL,
            *numaddrs = NULL,
            *pubmode = NULL,
            *pubmacmap = NULL,
            *pubips = NULL,
            *pubInterface = NULL,
            *privInterface = NULL, *pubSubnet = NULL, *pubSubnetMask = NULL, *pubBroadcastAddress = NULL, *pubRouter = NULL, *pubDomainname =
            NULL, *pubDNS = NULL, *localIp = NULL, *macPrefix = NULL;

        uint32_t *ips, *nms;
        int initFail = 0, len, usednew = 0;;

        // DHCP Daemon Configuration Params
        daemon = configFileValue("VNET_DHCPDAEMON");
        if (!daemon) {
            LOGWARN("no VNET_DHCPDAEMON defined in config, using default\n");
        }

        dhcpuser = configFileValue("VNET_DHCPUSER");
        if (!dhcpuser) {
            dhcpuser = strdup("root");
            if (!dhcpuser) {
                LOGFATAL("Out of memory\n");
                unlock_exit(1);
            }
        }

        pubmode = configFileValue("VNET_MODE");
        if (!pubmode) {
            LOGWARN("VNET_MODE is not defined, defaulting to 'SYSTEM'\n");
            pubmode = strdup(NETMODE_SYSTEM);
            if (!pubmode) {
                LOGFATAL("Out of memory\n");
                unlock_exit(1);
            }
        }

        macPrefix = configFileValue("VNET_MACPREFIX");
        if (!macPrefix) {
            LOGWARN("VNET_MACPREFIX is not defined, defaulting to 'd0:0d'\n");
            macPrefix = strdup("d0:0d");
            if (!macPrefix) {
                LOGFATAL("Out of memory!\n");
                unlock_exit(1);
            }
        } else {
            unsigned int a = 0, b = 0;
            if (sscanf(macPrefix, "%02X:%02X", &a, &b) != 2 || (a > 0xFF || b > 0xFF)) {
                LOGWARN("VNET_MACPREFIX is not defined, defaulting to 'd0:0d'\n");
                EUCA_FREE(macPrefix);
                macPrefix = strdup("d0:0d");
            }
        }

        pubInterface = configFileValue("VNET_PUBINTERFACE");
        if (!pubInterface) {
            LOGWARN("VNET_PUBINTERFACE is not defined, defaulting to 'eth0'\n");
            pubInterface = strdup("eth0");
            if (!pubInterface) {
                LOGFATAL("out of memory!\n");
                unlock_exit(1);
            }
        } else {
            usednew = 1;
        }

        privInterface = NULL;
        privInterface = configFileValue("VNET_PRIVINTERFACE");
        if (!privInterface) {
            LOGWARN("VNET_PRIVINTERFACE is not defined, defaulting to 'eth0'\n");
            privInterface = strdup("eth0");
            if (!privInterface) {
                LOGFATAL("out of memory!\n");
                unlock_exit(1);
            }
            usednew = 0;
        }

        if (!usednew) {
            tmpstr = NULL;
            tmpstr = configFileValue("VNET_INTERFACE");
            if (tmpstr) {
                LOGWARN("VNET_INTERFACE is deprecated, please use VNET_PUBINTERFACE and VNET_PRIVINTERFACE instead. Will set both to value of "
                        "VNET_INTERFACE (%s) for now.\n", tmpstr);
                EUCA_FREE(pubInterface);
                pubInterface = strdup(tmpstr);
                if (!pubInterface) {
                    LOGFATAL("out of memory!\n");
                    unlock_exit(1);
                }

                EUCA_FREE(privInterface);
                privInterface = strdup(tmpstr);
                if (!privInterface) {
                    LOGFATAL("out of memory!\n");
                    unlock_exit(1);
                }
            }
            EUCA_FREE(tmpstr);
        }

        if (pubmode && !(!strcmp(pubmode, NETMODE_SYSTEM) || !strcmp(pubmode, NETMODE_STATIC) || !strcmp(pubmode, NETMODE_EDGE) || !strcmp(pubmode, NETMODE_MANAGED_NOVLAN)
                         || !strcmp(pubmode, NETMODE_MANAGED))) {
            char errorm[256];
            memset(errorm, 0, 256);
            sprintf(errorm, "Invalid VNET_MODE setting: %s", pubmode);
            LOGFATAL("%s\n", errorm);
            log_eucafault("1012", "component", euca_this_component_name, "cause", errorm, NULL);
            initFail = 1;
        }

        if (pubmode && (!strcmp(pubmode, NETMODE_STATIC))) {
            pubSubnet = configFileValue("VNET_SUBNET");
            pubSubnetMask = configFileValue("VNET_NETMASK");
            pubBroadcastAddress = configFileValue("VNET_BROADCAST");
            pubRouter = configFileValue("VNET_ROUTER");
            pubDNS = configFileValue("VNET_DNS");
            pubDomainname = configFileValue("VNET_DOMAINNAME");
            pubmacmap = configFileValue("VNET_MACMAP");

            if (!pubSubnet || !pubSubnetMask || !pubBroadcastAddress || !pubRouter || !pubDNS || !pubmacmap) {
                LOGFATAL("in '%s' network mode, you must specify values for 'VNET_SUBNET, VNET_NETMASK, VNET_BROADCAST, VNET_ROUTER, "
                         "VNET_DNS and %s'\n", pubmode, "VNET_MACMAP");
                initFail = 1;
            }
        } else if (pubmode && !strcmp(pubmode, NETMODE_EDGE)) {

        } else if (pubmode && (!strcmp(pubmode, NETMODE_MANAGED) || !strcmp(pubmode, NETMODE_MANAGED_NOVLAN))) {
            numaddrs = configFileValue("VNET_ADDRSPERNET");
            pubSubnet = configFileValue("VNET_SUBNET");
            pubSubnetMask = configFileValue("VNET_NETMASK");
            pubDNS = configFileValue("VNET_DNS");
            pubDomainname = configFileValue("VNET_DOMAINNAME");
            pubips = configFileValue("VNET_PUBLICIPS");
            localIp = configFileValue("VNET_LOCALIP");
            if (!localIp) {
                LOGWARN("VNET_LOCALIP not defined, will attempt to auto-discover (consider setting this explicitly if tunnelling does not function " "properly.)\n");
            }

            if (!pubSubnet || !pubSubnetMask || !pubDNS || !numaddrs) {
                LOGFATAL("in 'MANAGED' or 'MANAGED-NOVLAN' network mode, you must specify values for 'VNET_SUBNET, VNET_NETMASK, " "VNET_ADDRSPERNET, and VNET_DNS'\n");
                initFail = 1;
            }
        }

        if (initFail) {
            LOGFATAL("bad network parameters, must fix before system will work\n");
            EUCA_FREE(pubSubnet);
            EUCA_FREE(pubSubnetMask);
            EUCA_FREE(pubBroadcastAddress);
            EUCA_FREE(pubRouter);
            EUCA_FREE(pubDomainname);
            EUCA_FREE(pubDNS);
            EUCA_FREE(pubmacmap);
            EUCA_FREE(numaddrs);
            EUCA_FREE(pubips);
            EUCA_FREE(localIp);
            EUCA_FREE(pubInterface);
            EUCA_FREE(privInterface);
            EUCA_FREE(dhcpuser);
            EUCA_FREE(daemon);
            EUCA_FREE(pubmode);
            EUCA_FREE(macPrefix);
            sem_mypost(INIT);
            return (1);
        }

        sem_mywait(VNET);

        int ret = vnetInit(vnetconfig, pubmode, eucahome, netPath, CLC, pubInterface, privInterface, numaddrs, pubSubnet, pubSubnetMask,
                           pubBroadcastAddress, pubDNS, pubDomainname, pubRouter, daemon,
                           dhcpuser, NULL, localIp, macPrefix);
        EUCA_FREE(pubSubnet);
        EUCA_FREE(pubSubnetMask);
        EUCA_FREE(pubBroadcastAddress);
        EUCA_FREE(pubDomainname);
        EUCA_FREE(pubDNS);
        EUCA_FREE(pubRouter);
        EUCA_FREE(numaddrs);
        EUCA_FREE(pubmode);
        EUCA_FREE(dhcpuser);
        EUCA_FREE(daemon);
        EUCA_FREE(privInterface);
        EUCA_FREE(pubInterface);
        EUCA_FREE(macPrefix);
        EUCA_FREE(localIp);

        if (ret > 0) {
            sem_mypost(VNET);
            sem_mypost(INIT);
            EUCA_FREE(pubips);
            return (1);
        }

        vnetAddDev(vnetconfig, vnetconfig->privInterface);

        if (pubmacmap) {
            char *mac = NULL, *ip = NULL, *ptra = NULL, *toka = NULL, *ptrb = NULL;
            toka = strtok_r(pubmacmap, " ", &ptra);
            while (toka) {
                mac = ip = NULL;
                mac = strtok_r(toka, "=", &ptrb);
                ip = strtok_r(NULL, "=", &ptrb);
                if (mac && ip) {
                    vnetAddHost(vnetconfig, mac, ip, 0, -1);
                }
                toka = strtok_r(NULL, " ", &ptra);
            }
            vnetKickDHCP(vnetconfig);
            EUCA_FREE(pubmacmap);
        } else if (pubips) {
            char *ip, *ptra, *toka;
            toka = strtok_r(pubips, " ", &ptra);
            while (toka) {
                ip = toka;
                if (ip) {
                    rc = vnetAddPublicIP(vnetconfig, ip);
                    if (rc) {
                        LOGERROR("could not add public IP '%s'\n", ip);
                    }
                }
                toka = strtok_r(NULL, " ", &ptra);
            }

            // detect and populate ips
            if (vnetCountLocalIP(vnetconfig) <= 0) {
                ips = nms = NULL;
                rc = getdevinfo("all", &ips, &nms, &len);
                if (!rc) {
                    for (i = 0; i < len; i++) {
                        char *theip = NULL;
                        theip = hex2dot(ips[i]);
                        if (vnetCheckPublicIP(vnetconfig, theip)) {
                            vnetAddLocalIP(vnetconfig, ips[i]);
                        }
                        EUCA_FREE(theip);
                    }
                }
                EUCA_FREE(ips);
                EUCA_FREE(nms);
            }
        }
        EUCA_FREE(pubips);

        /*
           if (privips) {
           char *ip, *ptra, *toka;
           toka = strtok_r(privips, " ", &ptra);
           while (toka) {
           ip = toka;
           if (ip) {
           rc = vnetAddPrivateIP(vnetconfig, ip);
           if (rc) {
           LOGERROR("could not add private IP '%s'\n", ip);
           }
           }
           toka = strtok_r(NULL, " ", &ptra);
           }
           }
           EUCA_FREE(privips);
         */

        sem_mypost(VNET);
    }

    tmpstr = configFileValue("SCHEDPOLICY");
    if (tmpstr == NULL) {
        // error
        LOGWARN("failed to parse config file (%s) for SCHEDPOLICY, defaulting to GREEDY\n", configFiles[0]);
        schedPolicy = SCHEDGREEDY;
    } else {
        if (!strcmp(tmpstr, "GREEDY"))
            schedPolicy = SCHEDGREEDY;
        else if (!strcmp(tmpstr, "ROUNDROBIN"))
            schedPolicy = SCHEDROUNDROBIN;
        else if (!strcmp(tmpstr, "POWERSAVE"))
            schedPolicy = SCHEDPOWERSAVE;
        else if (access(tmpstr, X_OK) == 0) {   // scheduler is an executable path, assumed to be user scheduler
            LOGWARN("will use user-defined scheduler at '%s'\n", tmpstr);
            euca_strncpy(schedPath, tmpstr, sizeof(schedPath));
            schedPolicy = SCHEDUSER;
        } else
            schedPolicy = SCHEDGREEDY;
    }
    EUCA_FREE(tmpstr);

    // powersave options
    tmpstr = configFileValue("POWER_IDLETHRESH");
    if (!tmpstr) {
        if (SCHEDPOWERSAVE == schedPolicy)
            LOGWARN("parsing config file (%s) for POWER_IDLETHRESH, defaulting to 300 seconds\n", configFiles[0]);
        idleThresh = 300;
        tmpstr = NULL;
    } else {
        idleThresh = atoi(tmpstr);
        if (idleThresh < 300) {
            LOGWARN("POWER_IDLETHRESH set too low (%d seconds), resetting to minimum (300 seconds)\n", idleThresh);
            idleThresh = 300;
        }
    }
    EUCA_FREE(tmpstr);

    tmpstr = configFileValue("POWER_WAKETHRESH");
    if (!tmpstr) {
        if (SCHEDPOWERSAVE == schedPolicy)
            LOGWARN("parsing config file (%s) for POWER_WAKETHRESH, defaulting to 300 seconds\n", configFiles[0]);
        wakeThresh = 300;
        tmpstr = NULL;
    } else {
        wakeThresh = atoi(tmpstr);
        if (wakeThresh < 300) {
            LOGWARN("POWER_WAKETHRESH set too low (%d seconds), resetting to minimum (300 seconds)\n", wakeThresh);
            wakeThresh = 300;
        }
    }
    EUCA_FREE(tmpstr);

    // some administrative options
    tmpstr = configFileValue("NC_POLLING_FREQUENCY");
    if (!tmpstr) {
        ncPollingFrequency = 6;
        tmpstr = NULL;
    } else {
        ncPollingFrequency = atoi(tmpstr);
        if (ncPollingFrequency < 6) {
            LOGWARN("NC_POLLING_FREQUENCY set too low (%ld seconds), resetting to minimum (6 seconds)\n", ncPollingFrequency);
            ncPollingFrequency = 6;
        }
    }
    EUCA_FREE(tmpstr);

    tmpstr = configFileValue("CLC_POLLING_FREQUENCY");
    if (!tmpstr) {
        clcPollingFrequency = 6;
        tmpstr = NULL;
    } else {
        clcPollingFrequency = atoi(tmpstr);
        if (clcPollingFrequency < 1) {
            LOGWARN("CLC_POLLING_FREQUENCY set too low (%ld seconds), resetting to default (6 seconds)\n", clcPollingFrequency);
            clcPollingFrequency = 6;
        }
    }
    EUCA_FREE(tmpstr);

    // CC Arbitrators
    tmpstr = configFileValue("CC_ARBITRATORS");
    if (tmpstr) {
        snprintf(arbitrators, 255, "%s", tmpstr);
        EUCA_FREE(tmpstr);
    } else {
        bzero(arbitrators, 256);
    }

    tmpstr = configFileValue("NC_FANOUT");
    if (!tmpstr) {
        ncFanout = 1;
        tmpstr = NULL;
    } else {
        ncFanout = atoi(tmpstr);
        if (ncFanout < 1 || ncFanout > 32) {
            LOGWARN("NC_FANOUT set out of bounds (min=%d max=%d) (current=%ld), resetting to default (1 NC)\n", 1, 32, ncFanout);
            ncFanout = 1;
        }
    }
    EUCA_FREE(tmpstr);

    tmpstr = configFileValue("INSTANCE_TIMEOUT");
    if (!tmpstr) {
        instanceTimeout = 300;
        tmpstr = NULL;
    } else {
        instanceTimeout = atoi(tmpstr);
        if (instanceTimeout < 30) {
            LOGWARN("INSTANCE_TIMEOUT set too low (%ld seconds), resetting to minimum (30 seconds)\n", instanceTimeout);
            instanceTimeout = 30;
        }
    }
    EUCA_FREE(tmpstr);

    // WS-Security
    use_wssec = 0;
    tmpstr = configFileValue("ENABLE_WS_SECURITY");
    if (!tmpstr) {
        // error
        LOGFATAL("parsing config file (%s) for ENABLE_WS_SECURITY\n", configFiles[0]);
        sem_mypost(INIT);
        return (1);
    } else {
        if (!strcmp(tmpstr, "Y")) {
            use_wssec = 1;
        }
    }
    EUCA_FREE(tmpstr);

    // Multi-cluster tunneling
    use_tunnels = 1;
    tmpstr = configFileValue("DISABLE_TUNNELING");
    if (tmpstr) {
        if (!strcmp(tmpstr, "Y")) {
            use_tunnels = 0;
        }
    }
    EUCA_FREE(tmpstr);

    // CC Image Caching
    proxyIp = NULL;
    use_proxy = 0;
    tmpstr = configFileValue("CC_IMAGE_PROXY");
    if (tmpstr) {
        proxyIp = strdup(tmpstr);
        if (!proxyIp) {
            LOGFATAL("out of memory!\n");
            unlock_exit(1);
        }
        use_proxy = 1;
    }
    EUCA_FREE(tmpstr);

    proxy_max_cache_size = 32768;
    tmpstr = configFileValue("CC_IMAGE_PROXY_CACHE_SIZE");
    if (tmpstr) {
        proxy_max_cache_size = atoi(tmpstr);
        if (proxy_max_cache_size <= 0) {
            LOGINFO("disabling CC image proxy cache due to size %d\n", proxy_max_cache_size);
            use_proxy = 0;             /* Disable proxy if zero-sized. */
        }
    }
    EUCA_FREE(tmpstr);

    tmpstr = configFileValue("CC_IMAGE_PROXY_PATH");
    if (tmpstr) {
        snprintf(proxyPath, EUCA_MAX_PATH, "%s", tmpstr);
        EUCA_FREE(tmpstr);
    } else {
        snprintf(proxyPath, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/dynserv", eucahome);
    }

    if (use_proxy)
        LOGINFO("enabling CC image proxy cache with size %d, path %s\n", proxy_max_cache_size, proxyPath);

    sem_mywait(CONFIG);
    // set up the current config
    euca_strncpy(config->eucahome, eucahome, EUCA_MAX_PATH);
    euca_strncpy(config->policyFile, policyFile, EUCA_MAX_PATH);
    //  snprintf(config->proxyPath, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/dynserv/data", config->eucahome);
    snprintf(config->proxyPath, EUCA_MAX_PATH, "%s", proxyPath);
    config->use_proxy = use_proxy;
    config->proxy_max_cache_size = proxy_max_cache_size;
    if (use_proxy) {
        snprintf(config->proxyIp, 32, "%s", proxyIp);
    }
    EUCA_FREE(proxyIp);

    config->use_wssec = use_wssec;
    config->use_tunnels = use_tunnels;
    config->schedPolicy = schedPolicy;
    euca_strncpy(config->schedPath, schedPath, sizeof(config->schedPath));
    config->idleThresh = idleThresh;
    config->wakeThresh = wakeThresh;
    config->instanceTimeout = instanceTimeout;
    config->ncPollingFrequency = ncPollingFrequency;
    config->ncSensorsPollingInterval = ncPollingFrequency;  // initially poll sensors with the same frequency as other NC ops
    config->clcPollingFrequency = clcPollingFrequency;
    config->ncFanout = ncFanout;
    locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);
    config->initialized = 1;
    ccChangeState(LOADED);
    config->ccStatus.localEpoch = 0;
    snprintf(config->arbitrators, 255, "%s", arbitrators);
    snprintf(config->ccStatus.details, 1024, "ERRORS=0");
    snprintf(config->ccStatus.serviceId.type, 32, "cluster");
    snprintf(config->ccStatus.serviceId.name, 256, "self");
    snprintf(config->ccStatus.serviceId.partition, 256, "unset");
    config->ccStatus.serviceId.urisLen = 0;
    for (i = 0; i < 32 && config->ccStatus.serviceId.urisLen < MAX_SERVICE_URIS; i++) {
        if (vnetconfig->localIps[i]) {
            char *host;
            host = hex2dot(vnetconfig->localIps[i]);
            if (host) {
                snprintf(config->ccStatus.serviceId.uris[config->ccStatus.serviceId.urisLen], 512, "http://%s:8774/axis2/services/EucalyptusCC", host);
                config->ccStatus.serviceId.urisLen++;
                EUCA_FREE(host);
            }
        }
    }
    snprintf(config->configFiles[0], EUCA_MAX_PATH, "%s", configFiles[0]);
    snprintf(config->configFiles[1], EUCA_MAX_PATH, "%s", configFiles[1]);

    LOGINFO("   CC Configuration: eucahome=%s\n", SP(config->eucahome));
    LOGINFO("                     policyfile=%s\n", SP(config->policyFile));
    LOGINFO("                     ws-security=%s\n", use_wssec ? "ENABLED" : "DISABLED");
    LOGINFO("                     schedulerPolicy=%s\n", SP(SCHEDPOLICIES[config->schedPolicy]));
    LOGINFO("                     idleThreshold=%d\n", config->idleThresh);
    LOGINFO("                     wakeThreshold=%d\n", config->wakeThresh);
    sem_mypost(CONFIG);

    res = NULL;
    rc = refreshNodes(config, &res, &numHosts);
    if (rc) {
        LOGERROR("cannot read list of nodes, check your config file\n");
        sem_mypost(INIT);
        return (1);
    }
    // update resourceCache
    sem_mywait(RESCACHE);
    resourceCache->numResources = numHosts;
    if (numHosts) {
        memcpy(resourceCache->resources, res, sizeof(ccResource) * numHosts);
    }
    EUCA_FREE(res);
    resourceCache->lastResourceUpdate = 0;
    sem_mypost(RESCACHE);

    config_init = 1;
    LOGTRACE("done\n");

    sem_mypost(INIT);
    return (0);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int syncNetworkState(void)
{
    int rc, ret = 0;

    LOGDEBUG("syncNetworkState(): syncing public/private IP mapping ground truth with local state\n");
    rc = map_instanceCache(validCmp, NULL, instIpSync, NULL);
    if (rc) {
        LOGWARN("syncNetworkState(): network sync implies network restore is necessary\n");
        ret++;
    }

    return (ret);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int checkActiveNetworks(void)
{
    int i, rc;
    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN)) {
        int activeNetworks[NUMBER_OF_VLANS];
        bzero(activeNetworks, sizeof(int) * NUMBER_OF_VLANS);

        LOGDEBUG("checkActiveNetworks(): maintaining active networks\n");
        for (i = 0; i < MAXINSTANCES_PER_CC; i++) {
            if (instanceCache->cacheState[i] != INSTINVALID) {
                if (strcmp(instanceCache->instances[i].state, "Teardown")) {
                    int vlan = instanceCache->instances[i].ccnet.vlan;
                    activeNetworks[vlan] = 1;
                    if (!vnetconfig->networks[vlan].active) {
                        LOGWARN("checkActiveNetworks(): instance running in network that is currently inactive (%s, %s, %d)\n",
                                vnetconfig->users[vlan].userName, vnetconfig->users[vlan].netName, vlan);
                    }
                }
            }
        }

        for (i = 0; i < NUMBER_OF_VLANS; i++) {
            sem_mywait(VNET);
            if (!activeNetworks[i] && vnetconfig->networks[i].active && ((time(NULL) - vnetconfig->networks[i].createTime) > 300)) {
                LOGWARN("checkActiveNetworks(): network active but no running instances (%s, %s, %d)\n", vnetconfig->users[i].userName, vnetconfig->users[i].netName, i);
                rc = vnetStopNetwork(vnetconfig, i, vnetconfig->users[i].userName, vnetconfig->users[i].netName);
                if (rc) {
                    LOGERROR("checkActiveNetworks(): failed to stop network (%s, %s, %d), will re-try\n", vnetconfig->users[i].userName, vnetconfig->users[i].netName, i);
                }
            }
            sem_mypost(VNET);

            /*
               if ( activeNetworks[i] ) {
               // make sure all active network indexes are used by an instance
               for (j=0; j<NUMBER_OF_HOSTS_PER_VLAN; j++) {
               if (vnetconfig->networks[i].addrs[j].active && (vnetconfig->networks[i].addrs[j].ip != 0) ) {
               // dan
               char *ip=NULL;
               ccInstance *myInstance=NULL;

               ip = hex2dot(vnetconfig->networks[i].addrs[j].ip);
               rc = find_instanceCacheIP(ip, &myInstance);
               if (rc) {
               // network index marked as used, but no instance in cache with that index/ip
               LOGWARN("checkActiveNetworks(): address active but no instances using addr (%s, %d, %d\n", ip, i, j);
               } else {
               LOGDEBUG("checkActiveNetworks(): address active and found for instance (%s, %s, %d, %d\n", myInstance->instanceId, ip, i, j);
               }
               EUCA_FREE(myInstance);
               EUCA_FREE(ip);
               }
               }
               }
             */
        }
    }
    return (0);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int maintainNetworkState(void)
{
    int rc = 0;
    int i = 0;
    int ret = 0;
    char pidfile[EUCA_MAX_PATH] = "";
    char *pidstr = NULL;

    if (!strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        LOGDEBUG("no network maintain required for EDGE\n");
        return (0);
    }

    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN)) {
        //    rc = checkActiveNetworks();
        //    if (rc) {
        //      LOGWARN("checkActiveNetworks() failed, attempting to re-sync\n");
        //    }

        LOGDEBUG("maintaining metadata redirect and tunnel health\n");
        sem_mywait(VNET);

        // check to see if cloudIp has changed
        char *cloudIp1 = hex2dot(config->cloudIp);
        char *cloudIp2 = hex2dot(vnetconfig->cloudIp);
        LOGDEBUG("CCcloudIp=%s VNETcloudIp=%s\n", cloudIp1, cloudIp2);
        EUCA_FREE(cloudIp1);
        EUCA_FREE(cloudIp2);

        if (config->cloudIp && (config->cloudIp != vnetconfig->cloudIp)) {
            rc = vnetUnsetMetadataRedirect(vnetconfig);
            if (rc) {
                LOGWARN("failed to unset old metadata redirect\n");
            }
            vnetconfig->cloudIp = config->cloudIp;
            rc = vnetSetMetadataRedirect(vnetconfig);
            if (rc) {
                LOGWARN("failed to set new metadata redirect\n");
            }
        }
        // check to see if this CCs localIpId has changed
        if (vnetconfig->tunnels.localIpId != vnetconfig->tunnels.localIpIdLast) {
            LOGDEBUG("local CC index has changed (%d -> %d): re-assigning gateway IPs and tunnel connections.\n", vnetconfig->tunnels.localIpId, vnetconfig->tunnels.localIpIdLast);

            for (i = 2; i < NUMBER_OF_VLANS; i++) {
                if (vnetconfig->networks[i].active) {
                    char brname[32];
                    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED)) {
                        snprintf(brname, 32, "eucabr%d", i);
                    } else {
                        snprintf(brname, 32, "%s", vnetconfig->privInterface);
                    }

                    if (vnetconfig->tunnels.localIpIdLast >= 0) {
                        vnetDelGatewayIP(vnetconfig, i, brname, vnetconfig->tunnels.localIpIdLast);
                    }
                    if (vnetconfig->tunnels.localIpId >= 0) {
                        vnetAddGatewayIP(vnetconfig, i, brname, vnetconfig->tunnels.localIpId);
                    }
                }
            }
            rc = vnetTeardownTunnels(vnetconfig);
            if (rc) {
                LOGERROR("failed to tear down tunnels\n");
                ret = 1;
            }

            config->kick_dhcp = 1;
            vnetconfig->tunnels.localIpIdLast = vnetconfig->tunnels.localIpId;
        }

        rc = vnetSetupTunnels(vnetconfig);
        if (rc) {
            LOGERROR("failed to setup tunnels during maintainNetworkState()\n");
            ret = 1;
        }

        for (i = 2; i < NUMBER_OF_VLANS; i++) {
            if (vnetconfig->networks[i].active) {
                char brname[32];
                if (!strcmp(vnetconfig->mode, NETMODE_MANAGED)) {
                    snprintf(brname, 32, "eucabr%d", i);
                } else {
                    snprintf(brname, 32, "%s", vnetconfig->privInterface);
                }
                rc = vnetAttachTunnels(vnetconfig, i, brname);
                if (rc) {
                    LOGDEBUG("failed to attach tunnels for vlan %d during maintainNetworkState()\n", i);
                    ret = 1;
                }
            }
        }

        //    rc = vnetApplyArpTableRules(vnetconfig);
        //    if (rc) {
        //      LOGWARN("failed to maintain arp tables\n");
        //    }

        sem_mypost(VNET);
    }

    if (strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        sem_mywait(CONFIG);
        snprintf(pidfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/net/euca-dhcp.pid", config->eucahome);
        if (!check_file(pidfile)) {
            pidstr = file2str(pidfile);
        } else {
            pidstr = NULL;
        }
        if (config->kick_dhcp || !pidstr || check_process(atoi(pidstr), "euca-dhcp.pid")) {
            rc = vnetKickDHCP(vnetconfig);
            if (rc) {
                LOGERROR("cannot start DHCP daemon\n");
                ret = 1;
            } else {
                config->kick_dhcp = 0;
            }
        }
        sem_mypost(CONFIG);

        EUCA_FREE(pidstr);
    }

    return (ret);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int restoreNetworkState(void)
{
    int rc, ret = 0, i;

    /* this function should query both internal and external information sources and restore the CC to correct networking state
       1.) restore from internal instance state
       - local IPs (instance and cloud)
       - networks (bridges)
       2.) query CLC for sec. group rules and apply (and/or apply from in-memory iptables?)
       3.) (re)start local network processes (dhcpd)
     */

    LOGDEBUG("restoring network state\n");

    if (!strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        LOGDEBUG("no restore necessary in EDGE\n");
        return (0);
    }

    sem_mywait(VNET);

    // sync up internal network state with information from instances
    LOGDEBUG("syncing internal network state with current instance state\n");
    rc = map_instanceCache(validCmp, NULL, instNetParamsSet, NULL);
    if (rc) {
        LOGERROR("could not sync internal network state with current instance state\n");
        ret = 1;
    }

    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN)) {
        // restore iptables state, if internal iptables state exists
        LOGDEBUG("restarting iptables\n");
        // Currently only called when CC is enabled
        if ((rc = vnetIptReInit(vnetconfig, TRUE)) != EUCA_OK) {
            LOGERROR("cannot restore iptables state\n");
            ret = 1;
        }
        // if we have the cloudIp set, re-install the Metadata Rule
        if (vnetconfig->cloudIp) {
            if ((rc = vnetSetMetadataRedirect(vnetconfig)) != 0) {
                LOGERROR("Fail to re-install metadata redirect. rc=%d\n", rc);
            }
        }
        // re-create all active networks (bridges, vlan<->bridge mappings)
        LOGDEBUG("restarting networks\n");
        for (i = 2; i < NUMBER_OF_VLANS; i++) {
            if (vnetconfig->networks[i].active) {
                char *brname = NULL;
                LOGDEBUG("found active network: %d\n", i);
                rc = vnetStartNetwork(vnetconfig, i, NULL, vnetconfig->users[i].userName, vnetconfig->users[i].netName, &brname);
                if (rc) {
                    LOGDEBUG("failed to reactivate network: %d", i);
                }
                EUCA_FREE(brname);
            }
        }

    }

    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN) || !strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        rc = map_instanceCache(validCmp, NULL, instNetReassignAddrs, NULL);
        if (rc) {
            LOGERROR("could not (re)assign public/private IP mappings\n");
            ret = 1;
        }
    }

    if (strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        // get DHCPD back up and running
        LOGDEBUG("restarting DHCPD\n");
        rc = vnetKickDHCP(vnetconfig);
        if (rc) {
            LOGERROR("cannot start DHCP daemon, please check your network settings\n");
            ret = 1;
        }
    }
    sem_mypost(VNET);

    LOGDEBUG("done restoring network state\n");

    return (ret);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int reconfigureNetworkFromCLC(void)
{
    int fd = 0;
    int i = 0;
    int rc = 0;
    int ret = 0;
    int usernetlen = 0;
    char *cloudIp = NULL;
    char **users = NULL;
    char **nets = NULL;
    char url[EUCA_MAX_PATH] = "";
    char cmd[EUCA_MAX_PATH] = "";
    char rootwrap[EUCA_MAX_PATH] = "";
    char clcnetfile[EUCA_MAX_PATH] = "";
    char chainmapfile[EUCA_MAX_PATH] = "";
    FILE *FH = NULL;

    if (strcmp(vnetconfig->mode, NETMODE_MANAGED) && strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN)) {
        return (0);
    }
    // get the latest cloud controller IP address
    if (config->cloudIp) {
        cloudIp = hex2dot(config->cloudIp);
    } else {
        cloudIp = strdup("localhost");
        if (!cloudIp) {
            LOGFATAL("out of memory!\n");
            unlock_exit(1);
        }
    }

    // create and populate network state files
    snprintf(clcnetfile, EUCA_MAX_PATH, "/tmp/euca-clcnet-XXXXXX");
    snprintf(chainmapfile, EUCA_MAX_PATH, "/tmp/euca-chainmap-XXXXXX");

    fd = safe_mkstemp(clcnetfile);
    if (fd < 0) {
        LOGERROR("cannot open clcnetfile '%s'\n", clcnetfile);
        EUCA_FREE(cloudIp);
        return (1);
    }
    chmod(clcnetfile, 0644);
    close(fd);

    fd = safe_mkstemp(chainmapfile);
    if (fd < 0) {
        LOGERROR("cannot open chainmapfile '%s'\n", chainmapfile);
        EUCA_FREE(cloudIp);
        unlink(clcnetfile);
        return (1);
    }
    chmod(chainmapfile, 0644);
    close(fd);

    /*
       fd = safe_mkstemp(config_ccfile);
       if (fd < 0) {
       LOGERROR("cannot open config_ccfile '%s'\n", config_ccfile);
       EUCA_FREE(cloudIp);
       unlink(clcnetfile);
       unlink(chainmapfile);
       return (1);
       }
       chmod(config_ccfile, 0644);
       close(fd);
     */

    // clcnet populate
    snprintf(url, EUCA_MAX_PATH, "http://%s:8773/latest/network-topology", cloudIp);
    rc = http_get_timeout(url, clcnetfile, 0, 0, 10, 15);
    EUCA_FREE(cloudIp);
    if (rc) {
        LOGWARN("cannot get latest network topology from cloud controller\n");
        unlink(clcnetfile);
        unlink(chainmapfile);
        return (1);
    }
    // chainmap populate
    FH = fopen(chainmapfile, "w");
    if (!FH) {
        LOGERROR("cannot write chain/net map to chainmap file '%s'\n", chainmapfile);
        unlink(clcnetfile);
        unlink(chainmapfile);
        return (1);
    }

    sem_mywait(VNET);
    rc = vnetGetAllVlans(vnetconfig, &users, &nets, &usernetlen);
    if (rc) {
    } else {
        for (i = 0; i < usernetlen; i++) {
            fprintf(FH, "%s %s\n", users[i], nets[i]);
            EUCA_FREE(users[i]);
            EUCA_FREE(nets[i]);
        }
    }
    fclose(FH);

    EUCA_FREE(users);
    EUCA_FREE(nets);

    snprintf(rootwrap, sizeof(rootwrap), EUCALYPTUS_ROOTWRAP, vnetconfig->eucahome);

    snprintf(cmd, sizeof(cmd), EUCALYPTUS_HELPER_DIR "/euca_ipt", vnetconfig->eucahome);
    if ((rc = euca_execlp(NULL, rootwrap, cmd, "filter", clcnetfile, chainmapfile, NULL)) != EUCA_OK) {
        LOGERROR("cannot run command '%s %s filter %s %s'. rc=%d\n", rootwrap, cmd, clcnetfile, chainmapfile, rc);
        ret = 1;
    }

    /*
       // removed by dan, not needed for EDGE 4.0
       FH = fopen(config_ccfile, "w");
       if (!FH) {
       } else {
       fprintf(FH, "EUCALYPTUS=%s\n", SP(config->eucahome));
       fprintf(FH, "VNET_MODE=%s\n", SP(vnetconfig->mode));
       strptra = hex2dot(vnetconfig->networks[0].nw);
       fprintf(FH, "VNET_SUBNET=%s\n", SP(strptra));
       EUCA_FREE(strptra);
       strptra = hex2dot(vnetconfig->networks[0].nm);
       fprintf(FH, "VNET_NETMASK=%s\n", SP(strptra));
       EUCA_FREE(strptra);
       strptra = hex2dot(vnetconfig->networks[0].bc);
       fprintf(FH, "VNET_BROADCAST=%s\n", SP(strptra));
       EUCA_FREE(strptra);
       strptra = hex2dot(vnetconfig->networks[0].router);
       fprintf(FH, "VNET_ROUTER=%s\n", SP(strptra));
       EUCA_FREE(strptra);
       strptra = hex2dot(vnetconfig->networks[0].dns);
       fprintf(FH, "VNET_DNS=%s\n", SP(strptra));
       EUCA_FREE(strptra);
       fprintf(FH, "VNET_DHCPDAEMON=%s\n", SP(vnetconfig->dhcpdaemon));
       fprintf(FH, "VNET_DHCPUSER=%s\n", SP(vnetconfig->dhcpuser));
       //        fprintf(FH, "CCIP=%s\n",  SP(config->proxyIp));
       strptra = hex2dot(config->cloudIp);
       fprintf(FH, "CLCIP=%s\n", SP(strptra));
       EUCA_FREE(strptra);

       for (i = 0; i < NUMBER_OF_PUBLIC_IPS; i++) {
       if (!vnetconfig->publicips[i].allocated) {
       if (!vnetconfig->publicips[i].ip && !vnetconfig->publicips[i].dstip) {
       } else {
       strptra = hex2dot(vnetconfig->publicips[i].ip);
       strptrb = hex2dot(vnetconfig->publicips[i].dstip);
       fprintf(FH, "IPMAP=%s %s\n", strptra, strptrb);
       EUCA_FREE(strptra);
       EUCA_FREE(strptrb);
       }
       }
       }
       rc = map_instanceCache(validCmp, NULL, writePubPrivIPMap, FH);
       if (rc) {
       LOGERROR("failed to write Public/Private IP Instance mapping file\n");
       }

       fclose(FH);
       }
     */
    sem_mypost(VNET);

    /*
       // removed by dan, not needed for EDGE 4.0
       if (!strcmp(vnetconfig->mode, NETMODE_EDGE)) {
       char destfile[EUCA_MAX_PATH];

       // make sure there is some content in file
       FH = fopen(clcnetfile, "a");
       if (FH) {
       fprintf(FH, "\n#MARK\n");
       fclose(FH);
       }

       snprintf(destfile, EUCA_MAX_PATH, "%s/data/network-topology", config->proxyPath);
       //        rename(clcnetfile, destfile);
       copy_file(clcnetfile, destfile);

       snprintf(destfile, EUCA_MAX_PATH, "%s/data/config-cc", config->proxyPath);
       //        rename(config_ccfile, destfile);
       copy_file(config_ccfile, destfile);

       }
     */

    unlink(clcnetfile);
    unlink(chainmapfile);
    //    unlink(config_ccfile);

    return (ret);
}

int writePubPrivIPMap(ccInstance * inst, void *in)
{
    FILE *FH = NULL;

    FH = ((FILE *) in);

    if (!inst) {
        return (1);
    } else if ((strcmp(inst->state, "Pending") && strcmp(inst->state, "Extant"))) {
        return (0);
    }
    //    LOGDEBUG("writePubPrivIPMap(): instanceId=%s publicIp=%s privateIp=%s\n", inst->instanceId, inst->ccnet.publicIp, inst->ccnet.privateIp);
    if (strcmp(inst->ccnet.publicIp, "0.0.0.0") && strcmp(inst->ccnet.privateIp, "0.0.0.0")) {
        fprintf(FH, "IPMAP=%s %s\n", inst->ccnet.publicIp, inst->ccnet.privateIp);
    }

    return (0);
}

//!
//!
//!
//! @param[in]  config
//! @param[out] res
//! @param[out] numHosts
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int refreshNodes(ccConfig * config, ccResource ** res, int *numHosts)
{
    int rc, i, lockmod;
    char *tmpstr, *ipbuf;
    char ncservice[512];
    int ncport;
    char **hosts;

    *numHosts = 0;
    *res = NULL;

    tmpstr = configFileValue(CONFIG_NC_SERVICE);
    if (!tmpstr) {
        // error
        LOGFATAL("parsing config files (%s,%s) for NC_SERVICE\n", config->configFiles[1], config->configFiles[0]);
        return (1);
    } else {
        if (tmpstr) {
            snprintf(ncservice, 512, "%s", tmpstr);
        }

    }
    EUCA_FREE(tmpstr);

    tmpstr = configFileValue(CONFIG_NC_PORT);
    if (!tmpstr) {
        // error
        LOGFATAL("parsing config files (%s,%s) for NC_PORT\n", config->configFiles[1], config->configFiles[0]);
        return (1);
    } else {
        if (tmpstr)
            ncport = atoi(tmpstr);
    }
    EUCA_FREE(tmpstr);

    tmpstr = configFileValue(CONFIG_NODES);
    if (!tmpstr) {
        // error
        LOGWARN("NODES parameter is missing from config files(%s,%s)\n", config->configFiles[1], config->configFiles[0]);
        return (0);
    } else {
        hosts = from_var_to_char_list(tmpstr);
        if (hosts == NULL) {
            LOGWARN("NODES list is empty in config files(%s,%s)\n", config->configFiles[1], config->configFiles[0]);
            EUCA_FREE(tmpstr);
            return (0);
        }

        *numHosts = 0;
        lockmod = 0;
        i = 0;
        while (hosts[i] != NULL) {
            (*numHosts)++;
            *res = EUCA_REALLOC(*res, (*numHosts), sizeof(ccResource));
            bzero(&((*res)[*numHosts - 1]), sizeof(ccResource));
            snprintf((*res)[*numHosts - 1].hostname, 256, "%s", hosts[i]);

            ipbuf = host2ip(hosts[i]);
            if (ipbuf) {
                snprintf((*res)[*numHosts - 1].ip, 24, "%s", ipbuf);
            }
            EUCA_FREE(ipbuf);

            (*res)[*numHosts - 1].ncPort = ncport;
            snprintf((*res)[*numHosts - 1].ncService, 128, "%s", ncservice);
            snprintf((*res)[*numHosts - 1].ncURL, 384, "http://%s:%d/%s", hosts[i], ncport, ncservice);
            (*res)[*numHosts - 1].state = RESDOWN;
            (*res)[*numHosts - 1].lastState = RESDOWN;
            (*res)[*numHosts - 1].lockidx = NCCALL0 + lockmod;
            lockmod = (lockmod + 1) % 32;
            EUCA_FREE(hosts[i]);
            i++;
        }
    }

    if (config->use_proxy) {
        rc = image_cache_proxykick(*res, numHosts);
        if (rc) {
            LOGERROR("could not restart the image proxy\n");
        }
    }

    EUCA_FREE(hosts);
    EUCA_FREE(tmpstr);

    return (0);
}

//!
//!
//!
//! @note
//!
void shawn(void)
{
    int p = 1, status;

    // clean up any orphaned child processes
    while (p > 0) {
        p = waitpid(-1, &status, WNOHANG);
    }

    if (instanceCache)
        msync(instanceCache, sizeof(ccInstanceCache), MS_ASYNC);
    if (resourceCache)
        msync(resourceCache, sizeof(ccResourceCache), MS_ASYNC);
    if (config)
        msync(config, sizeof(ccConfig), MS_ASYNC);
    if (vnetconfig)
        msync(vnetconfig, sizeof(vnetConfig), MS_ASYNC);

}

//!
//!
//!
//! @param[in] out
//! @param[in] ncURL
//! @param[in] ncService
//! @param[in] ncPort
//! @param[in] hostname
//! @param[in] mac
//! @param[in] ip
//! @param[in] maxMemory
//! @param[in] availMemory
//! @param[in] maxDisk
//! @param[in] availDisk
//! @param[in] maxCores
//! @param[in] availCores
//! @param[in] state
//! @param[in] laststate
//! @param[in] stateChange
//! @param[in] idleStart
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int allocate_ccResource(ccResource * out, char *ncURL, char *ncService, int ncPort, char *hostname, char *mac, char *ip, int maxMemory,
                        int availMemory, int maxDisk, int availDisk, int maxCores, int availCores, int state, int laststate, time_t stateChange, time_t idleStart)
{

    if (out != NULL) {
        if (ncURL)
            euca_strncpy(out->ncURL, ncURL, 384);
        if (ncService)
            euca_strncpy(out->ncService, ncService, 128);
        if (hostname)
            euca_strncpy(out->hostname, hostname, 256);
        if (mac)
            euca_strncpy(out->mac, mac, 24);
        if (ip)
            euca_strncpy(out->ip, ip, 24);

        out->ncPort = ncPort;
        out->maxMemory = maxMemory;
        out->availMemory = availMemory;
        out->maxDisk = maxDisk;
        out->availDisk = availDisk;
        out->maxCores = maxCores;
        out->availCores = availCores;
        out->state = state;
        out->lastState = laststate;
        out->stateChange = stateChange;
        out->idleStart = idleStart;
    }

    return (0);
}

//!
//!
//!
//! @param[in] mac
//! @param[in] vlan
//! @param[in] force
//! @param[in] dolock
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int free_instanceNetwork(char *mac, int vlan, int force, int dolock)
{
    int i = 0;
    u8 hexmac[6] = { 0 };
    boolean inuse = FALSE;

    if (mac2hex(mac, hexmac) == NULL)
        return (0);

    if (!maczero(hexmac)) {
        return (0);
    }

    if (dolock) {
        sem_mywait(INSTCACHE);
    }

    inuse = FALSE;
    if (!force) {
        // check to make sure the mac isn't in use elsewhere
        for (i = 0; ((i < MAXINSTANCES_PER_CC) && !inuse); i++) {
            if (!strcmp(instanceCache->instances[i].ccnet.privateMac, mac) && strcmp(instanceCache->instances[i].state, "Teardown")) {
                inuse = TRUE;
            }
        }
    }

    if (dolock) {
        sem_mypost(INSTCACHE);
    }

    if (!inuse) {
        // remove private network info from system
        sem_mywait(VNET);
        vnetDisableHost(vnetconfig, mac, NULL, 0);
        if (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN)) {
            vnetDelHost(vnetconfig, mac, NULL, vlan);
        }
        sem_mypost(VNET);
    }
    return (0);
}

//!
//!
//!
//! @param[in] out
//! @param[in] id
//! @param[in] amiId
//! @param[in] kernelId the kernel image identifier (eki-XXXXXXXX)
//! @param[in] ramdiskId the ramdisk image identifier (eri-XXXXXXXX)
//! @param[in] amiURL
//! @param[in] kernelURL the kernel image URL address
//! @param[in] ramdiskURL the ramdisk image URL address
//! @param[in] ownerId
//! @param[in] accountId
//! @param[in] state
//! @param[in] ccState
//! @param[in] ts
//! @param[in] reservationId
//! @param[in] ccnet
//! @param[in] ncnet
//! @param[in] ccvm
//! @param[in] ncHostIdx
//! @param[in] keyName
//! @param[in] serviceTag
//! @param[in] userData
//! @param[in] launchIndex
//! @param[in] platform
//! @param[in] guestStateName
//! @param[in] bundleTaskStateName
//! @param[in] groupNames
//! @param[in] volumes
//! @param[in] volumesSize
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int allocate_ccInstance(ccInstance * out, char *id, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL,
                        char *ownerId, char *accountId, char *state, char *ccState, time_t ts, char *reservationId, netConfig * ccnet,
                        netConfig * ncnet, virtualMachine * ccvm, int ncHostIdx, char *keyName, char *serviceTag, char *userData, char *launchIndex,
                        char *platform, char *guestStateName, char *bundleTaskStateName, char groupNames[][64], ncVolume * volumes, int volumesSize)
{
    if (out != NULL) {
        bzero(out, sizeof(ccInstance));
        if (id)
            euca_strncpy(out->instanceId, id, 16);
        if (amiId)
            euca_strncpy(out->amiId, amiId, 16);
        if (kernelId)
            euca_strncpy(out->kernelId, kernelId, 16);
        if (ramdiskId)
            euca_strncpy(out->ramdiskId, ramdiskId, 16);

        if (amiURL)
            euca_strncpy(out->amiURL, amiURL, 512);
        if (kernelURL)
            euca_strncpy(out->kernelURL, kernelURL, 512);
        if (ramdiskURL)
            euca_strncpy(out->ramdiskURL, ramdiskURL, 512);

        if (state)
            euca_strncpy(out->state, state, 16);
        if (ccState)
            euca_strncpy(out->ccState, ccState, 16);
        if (ownerId)
            euca_strncpy(out->ownerId, ownerId, 48);
        if (accountId)
            euca_strncpy(out->accountId, accountId, 48);
        if (reservationId)
            euca_strncpy(out->reservationId, reservationId, 16);
        if (keyName)
            euca_strncpy(out->keyName, keyName, 1024);
        out->ts = ts;
        out->ncHostIdx = ncHostIdx;
        if (serviceTag)
            euca_strncpy(out->serviceTag, serviceTag, 384);
        if (userData)
            euca_strncpy(out->userData, userData, 16384);
        if (launchIndex)
            euca_strncpy(out->launchIndex, launchIndex, 64);
        if (platform)
            euca_strncpy(out->platform, platform, 64);
        if (guestStateName)
            euca_strncpy(out->guestStateName, guestStateName, 64);
        if (bundleTaskStateName)
            euca_strncpy(out->bundleTaskStateName, bundleTaskStateName, 64);
        if (groupNames) {
            int i;
            for (i = 0; i < 64; i++) {
                if (groupNames[i]) {
                    euca_strncpy(out->groupNames[i], groupNames[i], 64);
                }
            }
        }

        if (volumes) {
            memcpy(out->volumes, volumes, sizeof(ncVolume) * EUCA_MAX_VOLUMES);
        }
        out->volumesSize = volumesSize;

        if (ccnet)
            allocate_netConfig(&(out->ccnet), ccnet->privateMac, ccnet->privateIp, ccnet->publicIp, ccnet->vlan, ccnet->networkIndex);
        if (ncnet)
            allocate_netConfig(&(out->ncnet), ncnet->privateMac, ncnet->privateIp, ncnet->publicIp, ncnet->vlan, ncnet->networkIndex);
        if (ccvm)
            allocate_virtualMachine(&(out->ccvm), ccvm);
    }
    return (0);
}

//!
//!
//!
//! @param[in] inst
//! @param[in] ip
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int pubIpCmp(ccInstance * inst, void *ip)
{
    if (!ip || !inst) {
        return (1);
    }

    if (!strcmp((char *)ip, inst->ccnet.publicIp)) {
        return (0);
    }
    return (1);
}

//!
//!
//!
//! @param[in] inst
//! @param[in] ip
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int privIpCmp(ccInstance * inst, void *ip)
{
    if (!ip || !inst) {
        return (1);
    }

    if (!strcmp((char *)ip, inst->ccnet.privateIp)) {
        return (0);
    }
    return (1);
}

//!
//!
//!
//! @param[in] inst
//! @param[in] ip
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int privIpSet(ccInstance * inst, void *ip)
{
    if (!ip || !inst) {
        return (1);
    }

    if ((strcmp(inst->state, "Pending") && strcmp(inst->state, "Extant"))) {
        snprintf(inst->ccnet.privateIp, IP_BUFFER_SIZE, "0.0.0.0");
        return (0);
    }

    LOGDEBUG("privIpSet(): set: %s/%s\n", inst->ccnet.privateIp, (char *)ip);
    snprintf(inst->ccnet.privateIp, IP_BUFFER_SIZE, "%s", (char *)ip);
    return (0);
}

//!
//!
//!
//! @param[in] inst
//! @param[in] ip
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int pubIpSet(ccInstance * inst, void *ip)
{
    if (!ip || !inst) {
        return (1);
    }

    if ((strcmp(inst->state, "Pending") && strcmp(inst->state, "Extant"))) {
        snprintf(inst->ccnet.publicIp, IP_BUFFER_SIZE, "0.0.0.0");
        return (0);
    }

    LOGDEBUG("pubIpSet(): set: %s/%s\n", inst->ccnet.publicIp, (char *)ip);
    snprintf(inst->ccnet.publicIp, IP_BUFFER_SIZE, "%s", (char *)ip);
    return (0);
}

//!
//!
//!
//! @param[in] match
//! @param[in] matchParam
//! @param[in] operate
//! @param[in] operateParam
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int map_instanceCache(int (*match) (ccInstance *, void *), void *matchParam, int (*operate) (ccInstance *, void *), void *operateParam)
{
    int i, ret = 0;

    sem_mywait(INSTCACHE);

    for (i = 0; i < MAXINSTANCES_PER_CC; i++) {
        if (!match(&(instanceCache->instances[i]), matchParam)) {
            if (operate(&(instanceCache->instances[i]), operateParam)) {
                LOGWARN("instance cache mapping failed to operate at index %d\n", i);
                ret++;
            }
        }
    }

    sem_mypost(INSTCACHE);
    return (ret);
}

//!
//!
//!
//! @note
//!
void print_instanceCache(void)
{
    int i;

    sem_mywait(INSTCACHE);
    for (i = 0; i < MAXINSTANCES_PER_CC; i++) {
        if (instanceCache->cacheState[i] == INSTVALID) {
            LOGDEBUG("\tcache: %d/%d %s %s %s %s\n", i, instanceCache->numInsts, instanceCache->instances[i].instanceId,
                     instanceCache->instances[i].ccnet.publicIp, instanceCache->instances[i].ccnet.privateIp, instanceCache->instances[i].state);
        }
    }
    sem_mypost(INSTCACHE);
}

//!
//!
//!
//! @param[in] tag
//! @param[in] in
//!
//! @pre
//!
//! @note
//!
void print_ccInstance(char *tag, ccInstance * in)
{
    char *volbuf, *groupbuf;
    int i;

    volbuf = EUCA_ZALLOC((EUCA_MAX_VOLUMES * 2), sizeof(ncVolume));
    if (!volbuf) {
        LOGFATAL("out of memory!\n");
        unlock_exit(1);
    }

    groupbuf = EUCA_ZALLOC((64 * 32 * 2), sizeof(char));
    if (!groupbuf) {
        LOGFATAL("out of memory!\n");
        unlock_exit(1);
    }

    for (i = 0; i < 64; i++) {
        if (in->groupNames[i][0] != '\0') {
            strncat(groupbuf, in->groupNames[i], 64);
            strncat(groupbuf, " ", 1);
        }
    }

    for (i = 0; i < EUCA_MAX_VOLUMES; i++) {
        if (in->volumes[i].volumeId[0] != '\0') {
            strncat(volbuf, in->volumes[i].volumeId, CHAR_BUFFER_SIZE);
            strncat(volbuf, ",", 1);
            strncat(volbuf, in->volumes[i].attachmentToken, CHAR_BUFFER_SIZE);
            strncat(volbuf, ",", 1);
            strncat(volbuf, in->volumes[i].localDev, CHAR_BUFFER_SIZE);
            strncat(volbuf, ",", 1);
            strncat(volbuf, in->volumes[i].stateName, CHAR_BUFFER_SIZE);
            strncat(volbuf, " ", 1);
        }
    }

    LOGDEBUG("%s instanceId=%s reservationId=%s state=%s accountId=%s ownerId=%s ts=%ld keyName=%s ccnet={privateIp=%s publicIp=%s privateMac=%s "
             "vlan=%d networkIndex=%d} ccvm={cores=%d mem=%d disk=%d} ncHostIdx=%d serviceTag=%s userData=%s launchIndex=%s platform=%s "
             "bundleTaskStateName=%s, volumesSize=%d volumes={%s} groupNames={%s} migration_state=%s guestStateName=%s\n", tag, in->instanceId, in->reservationId, in->state,
             in->accountId, in->ownerId, in->ts, in->keyName, in->ccnet.privateIp, in->ccnet.publicIp, in->ccnet.privateMac, in->ccnet.vlan,
             in->ccnet.networkIndex, in->ccvm.cores, in->ccvm.mem, in->ccvm.disk, in->ncHostIdx, in->serviceTag, in->userData, in->launchIndex,
             in->platform, in->bundleTaskStateName, in->volumesSize, volbuf, groupbuf, migration_state_names[in->migration_state], in->guestStateName);

    EUCA_FREE(volbuf);
    EUCA_FREE(groupbuf);
}

//!
//!
//!
//! @note
//!
void set_clean_instanceCache(void)
{
    sem_mywait(INSTCACHE);
    instanceCache->dirty = 0;
    sem_mypost(INSTCACHE);
}

//!
//!
//!
//! @note
//!
void set_dirty_instanceCache(void)
{
    sem_mywait(INSTCACHE);
    instanceCache->dirty = 1;
    sem_mypost(INSTCACHE);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int is_clean_instanceCache(void)
{
    int ret = 1;
    sem_mywait(INSTCACHE);
    if (instanceCache->dirty) {
        ret = 0;
    } else {
        ret = 1;
    }
    sem_mypost(INSTCACHE);
    return (ret);
}

//!
//!
//!
//! @note
//!
void invalidate_instanceCache(void)
{
    int i;

    sem_mywait(INSTCACHE);
    for (i = 0; i < MAXINSTANCES_PER_CC; i++) {
        // if instance is in teardown, free up network information
        if (!strcmp(instanceCache->instances[i].state, "Teardown")) {
            free_instanceNetwork(instanceCache->instances[i].ccnet.privateMac, instanceCache->instances[i].ccnet.vlan, 0, 0);
        }
        if ((instanceCache->cacheState[i] == INSTVALID) && ((time(NULL) - instanceCache->lastseen[i]) > config->instanceTimeout)) {
            LOGDEBUG("invalidating instance '%s' (last seen %ld seconds ago)\n", instanceCache->instances[i].instanceId, (time(NULL) - instanceCache->lastseen[i]));
            bzero(&(instanceCache->instances[i]), sizeof(ccInstance));
            instanceCache->lastseen[i] = 0;
            instanceCache->cacheState[i] = INSTINVALID;
            instanceCache->numInsts--;
        }
    }
    sem_mypost(INSTCACHE);
}

//!
//!
//!
//! @param[in] instanceId
//! @param[in] in
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int refresh_instanceCache(char *instanceId, ccInstance * in)
{
    int i, done;

    if (!instanceId || !in) {
        return (1);
    }

    sem_mywait(INSTCACHE);
    done = 0;
    for (i = 0; i < MAXINSTANCES_PER_CC && !done; i++) {
        if (!strcmp(instanceCache->instances[i].instanceId, instanceId)) {
            // in cache
            // give precedence to instances that are in Extant/Pending over expired instances, when info comes from two different nodes
            if (strcmp(in->serviceTag, instanceCache->instances[i].serviceTag) && strcmp(in->state, instanceCache->instances[i].state)
                && !strcmp(in->state, "Teardown")) {
                // skip
                LOGDEBUG("skipping cache refresh with instance in Teardown (instance with non-Teardown from different node already cached)\n");
            } else {
                // update cached instance info
                memcpy(&(instanceCache->instances[i]), in, sizeof(ccInstance));
                instanceCache->lastseen[i] = time(NULL);
            }
            sem_mypost(INSTCACHE);
            return (0);
        }
    }
    sem_mypost(INSTCACHE);

    add_instanceCache(instanceId, in);

    return (0);
}

//!
//!
//!
//! @param[in] instanceId
//! @param[in] in
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int add_instanceCache(char *instanceId, ccInstance * in)
{
    int i, done, firstNull = 0;

    if (!instanceId || !in) {
        return (1);
    }

    sem_mywait(INSTCACHE);
    done = 0;
    for (i = 0; i < MAXINSTANCES_PER_CC && !done; i++) {
        if ((instanceCache->cacheState[i] == INSTVALID) && (!strcmp(instanceCache->instances[i].instanceId, instanceId))) {
            // already in cache
            LOGDEBUG("'%s/%s/%s' already in cache\n", instanceId, in->ccnet.publicIp, in->ccnet.privateIp);
            instanceCache->lastseen[i] = time(NULL);
            sem_mypost(INSTCACHE);
            return (0);
        } else if (instanceCache->cacheState[i] == INSTINVALID) {
            firstNull = i;
            done++;
        }
    }
    LOGDEBUG("adding '%s/%s/%s/%d' to cache\n", instanceId, in->ccnet.publicIp, in->ccnet.privateIp, in->volumesSize);
    allocate_ccInstance(&(instanceCache->instances[firstNull]), in->instanceId, in->amiId, in->kernelId, in->ramdiskId, in->amiURL, in->kernelURL,
                        in->ramdiskURL, in->ownerId, in->accountId, in->state, in->ccState, in->ts, in->reservationId, &(in->ccnet), &(in->ncnet),
                        &(in->ccvm), in->ncHostIdx, in->keyName, in->serviceTag, in->userData, in->launchIndex, in->platform, in->guestStateName, in->bundleTaskStateName,
                        in->groupNames, in->volumes, in->volumesSize);
    instanceCache->numInsts++;
    instanceCache->lastseen[firstNull] = time(NULL);
    instanceCache->cacheState[firstNull] = INSTVALID;

    sem_mypost(INSTCACHE);
    return (0);
}

//!
//!
//!
//! @param[in] instanceId
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int del_instanceCacheId(char *instanceId)
{
    int i;

    sem_mywait(INSTCACHE);
    for (i = 0; i < MAXINSTANCES_PER_CC; i++) {
        if ((instanceCache->cacheState[i] == INSTVALID) && (!strcmp(instanceCache->instances[i].instanceId, instanceId))) {
            // del from cache
            bzero(&(instanceCache->instances[i]), sizeof(ccInstance));
            instanceCache->lastseen[i] = 0;
            instanceCache->cacheState[i] = INSTINVALID;
            instanceCache->numInsts--;
            sem_mypost(INSTCACHE);
            return (0);
        }
    }
    sem_mypost(INSTCACHE);
    return (0);
}

//!
//!
//!
//! @param[in]  instanceId
//! @param[out] out
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int find_instanceCacheId(char *instanceId, ccInstance ** out)
{
    int i, done;

    if (!instanceId || !out) {
        return (1);
    }

    sem_mywait(INSTCACHE);
    *out = NULL;
    done = 0;
    for (i = 0; i < MAXINSTANCES_PER_CC && !done; i++) {
        if (!strcmp(instanceCache->instances[i].instanceId, instanceId)) {
            // found it
            *out = EUCA_ZALLOC(1, sizeof(ccInstance));
            if (!*out) {
                LOGFATAL("out of memory!\n");
                unlock_exit(1);
            }

            allocate_ccInstance(*out, instanceCache->instances[i].instanceId, instanceCache->instances[i].amiId, instanceCache->instances[i].kernelId,
                                instanceCache->instances[i].ramdiskId, instanceCache->instances[i].amiURL, instanceCache->instances[i].kernelURL,
                                instanceCache->instances[i].ramdiskURL, instanceCache->instances[i].ownerId, instanceCache->instances[i].accountId,
                                instanceCache->instances[i].state, instanceCache->instances[i].ccState, instanceCache->instances[i].ts,
                                instanceCache->instances[i].reservationId, &(instanceCache->instances[i].ccnet), &(instanceCache->instances[i].ncnet),
                                &(instanceCache->instances[i].ccvm), instanceCache->instances[i].ncHostIdx, instanceCache->instances[i].keyName,
                                instanceCache->instances[i].serviceTag, instanceCache->instances[i].userData, instanceCache->instances[i].launchIndex,
                                instanceCache->instances[i].platform, instanceCache->instances[i].guestStateName, instanceCache->instances[i].bundleTaskStateName,
                                instanceCache->instances[i].groupNames, instanceCache->instances[i].volumes, instanceCache->instances[i].volumesSize);
            LOGTRACE("found instance in cache '%s/%s/%s'\n", instanceCache->instances[i].instanceId,
                     instanceCache->instances[i].ccnet.publicIp, instanceCache->instances[i].ccnet.privateIp);
            // migration-related
            // TO-DO: move to allocate_ccInstance() ?
            (*out)->migration_state = instanceCache->instances[i].migration_state;
            LOGTRACE("instance %s migration state=%s\n", instanceCache->instances[i].instanceId, migration_state_names[(*out)->migration_state]);
            done++;
        }
    }
    sem_mypost(INSTCACHE);
    if (done) {
        return (0);
    }
    return (1);
}

//!
//!
//!
//! @param[in]  ip
//! @param[out] out
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int find_instanceCacheIP(char *ip, ccInstance ** out)
{
    int i, done;

    if (!ip || !out) {
        return (1);
    }

    sem_mywait(INSTCACHE);
    *out = NULL;
    done = 0;
    for (i = 0; i < MAXINSTANCES_PER_CC && !done; i++) {
        if ((instanceCache->instances[i].ccnet.publicIp[0] != '\0' || instanceCache->instances[i].ccnet.privateIp[0] != '\0')) {
            if (!strcmp(instanceCache->instances[i].ccnet.publicIp, ip) || !strcmp(instanceCache->instances[i].ccnet.privateIp, ip)) {
                // found it
                *out = EUCA_ZALLOC(1, sizeof(ccInstance));
                if (!*out) {
                    LOGFATAL("out of memory!\n");
                    unlock_exit(1);
                }

                allocate_ccInstance(*out, instanceCache->instances[i].instanceId, instanceCache->instances[i].amiId,
                                    instanceCache->instances[i].kernelId, instanceCache->instances[i].ramdiskId, instanceCache->instances[i].amiURL,
                                    instanceCache->instances[i].kernelURL, instanceCache->instances[i].ramdiskURL,
                                    instanceCache->instances[i].ownerId, instanceCache->instances[i].accountId, instanceCache->instances[i].state,
                                    instanceCache->instances[i].ccState, instanceCache->instances[i].ts, instanceCache->instances[i].reservationId,
                                    &(instanceCache->instances[i].ccnet), &(instanceCache->instances[i].ncnet), &(instanceCache->instances[i].ccvm),
                                    instanceCache->instances[i].ncHostIdx, instanceCache->instances[i].keyName,
                                    instanceCache->instances[i].serviceTag, instanceCache->instances[i].userData,
                                    instanceCache->instances[i].launchIndex, instanceCache->instances[i].platform,
                                    instanceCache->instances[i].guestStateName, instanceCache->instances[i].bundleTaskStateName, instanceCache->instances[i].groupNames,
                                    instanceCache->instances[i].volumes, instanceCache->instances[i].volumesSize);
                done++;
            }
        }
    }

    sem_mypost(INSTCACHE);
    if (done) {
        return (0);
    }
    return (1);
}

//!
//!
//!
//! @note
//!
void print_resourceCache(void)
{
    int i;

    sem_mywait(RESCACHE);
    for (i = 0; i < MAXNODES; i++) {
        if (resourceCache->cacheState[i] == RESVALID) {
            LOGDEBUG("\tcache: %s %s %s %s/%s state=%d\n", resourceCache->resources[i].hostname, resourceCache->resources[i].ncURL,
                     resourceCache->resources[i].ncService, resourceCache->resources[i].mac, resourceCache->resources[i].ip, resourceCache->resources[i].state);
        }
    }
    sem_mypost(RESCACHE);
}

//!
//!
//!
//! @note
//!
void invalidate_resourceCache(void)
{
    sem_mywait(RESCACHE);

    bzero(resourceCache->cacheState, sizeof(int) * MAXNODES);
    resourceCache->numResources = 0;
    resourceCache->resourceCacheUpdate = 0;

    sem_mypost(RESCACHE);

}

//!
//!
//!
//! @param[in] host
//! @param[in] in
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int refresh_resourceCache(char *host, ccResource * in)
{
    int i, done;

    if (!host || !in) {
        return (1);
    }

    sem_mywait(RESCACHE);
    done = 0;
    for (i = 0; i < MAXNODES && !done; i++) {
        if (resourceCache->cacheState[i] == RESVALID) {
            if (!strcmp(resourceCache->resources[i].hostname, host)) {
                // in cache
                memcpy(&(resourceCache->resources[i]), in, sizeof(ccResource));
                sem_mypost(RESCACHE);
                return (0);
            }
        }
    }
    sem_mypost(RESCACHE);

    add_resourceCache(host, in);

    return (0);
}

//!
//!
//!
//! @param[in] host
//! @param[in] in
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int add_resourceCache(char *host, ccResource * in)
{
    int i, done, firstNull = 0;

    if (!host || !in) {
        return (1);
    }

    sem_mywait(RESCACHE);
    done = 0;
    for (i = 0; i < MAXNODES && !done; i++) {
        if (resourceCache->cacheState[i] == RESVALID) {
            if (!strcmp(resourceCache->resources[i].hostname, host)) {
                // already in cache
                sem_mypost(RESCACHE);
                return (0);
            }
        } else {
            firstNull = i;
            done++;
        }
    }
    resourceCache->cacheState[firstNull] = RESVALID;
    allocate_ccResource(&(resourceCache->resources[firstNull]), in->ncURL, in->ncService, in->ncPort, in->hostname, in->mac, in->ip, in->maxMemory,
                        in->availMemory, in->maxDisk, in->availDisk, in->maxCores, in->availCores, in->state, in->lastState, in->stateChange, in->idleStart);

    resourceCache->numResources++;
    sem_mypost(RESCACHE);
    return (0);
}

//!
//!
//!
//! @param[in] host
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int del_resourceCacheId(char *host)
{
    int i;

    sem_mywait(RESCACHE);
    for (i = 0; i < MAXNODES; i++) {
        if (resourceCache->cacheState[i] == RESVALID) {
            if (!strcmp(resourceCache->resources[i].hostname, host)) {
                // del from cache
                bzero(&(resourceCache->resources[i]), sizeof(ccResource));
                resourceCache->cacheState[i] = RESINVALID;
                resourceCache->numResources--;
                sem_mypost(RESCACHE);
                return (0);
            }
        }
    }
    sem_mypost(RESCACHE);
    return (0);
}

//!
//!
//!
//! @param[in]  host
//! @param[out] out
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int find_resourceCacheId(char *host, ccResource ** out)
{
    int i, done;

    if (!host || !out) {
        return (1);
    }

    sem_mywait(RESCACHE);
    *out = NULL;
    done = 0;
    for (i = 0; i < MAXNODES && !done; i++) {
        if (resourceCache->cacheState[i] == RESVALID) {
            if (!strcmp(resourceCache->resources[i].hostname, host)) {
                // found it
                *out = EUCA_ZALLOC(1, sizeof(ccResource));
                if (!*out) {
                    LOGFATAL("out of memory!\n");
                    unlock_exit(1);
                }
                allocate_ccResource(*out, resourceCache->resources[i].ncURL, resourceCache->resources[i].ncService,
                                    resourceCache->resources[i].ncPort, resourceCache->resources[i].hostname, resourceCache->resources[i].mac,
                                    resourceCache->resources[i].ip, resourceCache->resources[i].maxMemory, resourceCache->resources[i].availMemory,
                                    resourceCache->resources[i].maxDisk, resourceCache->resources[i].availDisk, resourceCache->resources[i].maxCores,
                                    resourceCache->resources[i].availCores, resourceCache->resources[i].state, resourceCache->resources[i].lastState,
                                    resourceCache->resources[i].stateChange, resourceCache->resources[i].idleStart);
                done++;
            }
        }
    }

    sem_mypost(RESCACHE);
    if (done) {
        return (0);
    }
    return (1);
}

//!
//!
//!
//! @param[in] code
//!
//! @pre
//!
//! @note
//!
void unlock_exit(int code)
{
    int i;

    LOGDEBUG("params: code=%d\n", code);

    for (i = 0; i < ENDLOCK; i++) {
        if (mylocks[i]) {
            LOGWARN("unlocking index '%d'\n", i);
            sem_post(locks[i]);
        }
    }
    exit(code);
}

//!
//!
//!
//! @param[in] lockno
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int sem_mywait(int lockno)
{
    int rc;
    rc = sem_wait(locks[lockno]);
    mylocks[lockno] = 1;
    return (rc);
}

//!
//!
//!
//! @param[in] lockno
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int sem_mypost(int lockno)
{
    mylocks[lockno] = 0;
    return (sem_post(locks[lockno]));
}

//!
//!
//!
//! @param[in] id
//! @param[in] url
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int image_cache(char *id, char *url)
{
    int rc;
    int pid;
    char path[EUCA_MAX_PATH], finalpath[EUCA_MAX_PATH];

    if (url && id) {
        pid = fork();
        if (!pid) {
            snprintf(finalpath, EUCA_MAX_PATH, "%s/data/%s.manifest.xml", config->proxyPath, id);
            snprintf(path, EUCA_MAX_PATH, "%s/data/%s.manifest.xml.staging", config->proxyPath, id);
            if (check_file(path) && check_file(finalpath)) {
                rc = objectstorage_object_by_url(url, path, 0);
                if (rc) {
                    LOGERROR("could not cache image manifest (%s/%s)\n", id, url);
                    unlink(path);
                    exit(1);
                }
                rename(path, finalpath);
                chmod(finalpath, 0600);
            }
            snprintf(path, EUCA_MAX_PATH, "%s/data/%s.staging", config->proxyPath, id);
            snprintf(finalpath, EUCA_MAX_PATH, "%s/data/%s", config->proxyPath, id);
            if (check_file(path) && check_file(finalpath)) {
                rc = objectstorage_image_by_manifest_url(url, path, 1);
                if (rc) {
                    LOGERROR("could not cache image (%s/%s)\n", id, url);
                    unlink(path);
                    exit(1);
                }
                rename(path, finalpath);
                chmod(finalpath, 0600);
            }
            exit(0);
        }
    }

    return (0);
}

//!
//!
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int image_cache_invalidate(void)
{
    time_t oldest;
    char proxyPath[EUCA_MAX_PATH], path[EUCA_MAX_PATH], oldestpath[EUCA_MAX_PATH], oldestmanifestpath[EUCA_MAX_PATH];
    DIR *DH = NULL;
    struct dirent dent, *result = NULL;
    struct stat mystat;
    int rc, total_megs = 0;

    if (config->use_proxy) {
        proxyPath[0] = '\0';
        path[0] = '\0';
        oldestpath[0] = '\0';
        oldestmanifestpath[0] = '\0';

        oldest = time(NULL);
        snprintf(proxyPath, EUCA_MAX_PATH, "%s/data", config->proxyPath);
        DH = opendir(proxyPath);
        if (!DH) {
            LOGERROR("could not open dir '%s'\n", proxyPath);
            return (1);
        }

        rc = readdir_r(DH, &dent, &result);
        while (!rc && result) {
            if (strcmp(dent.d_name, ".") && strcmp(dent.d_name, "..") && !strstr(dent.d_name, "manifest.xml") && strcmp(dent.d_name, "network-topology")
                && strcmp(dent.d_name, "config-cc")) {
                snprintf(path, EUCA_MAX_PATH, "%s/%s", proxyPath, dent.d_name);
                rc = stat(path, &mystat);
                if (!rc) {
                    LOGDEBUG("evaluating file: name=%s size=%ld atime=%ld'\n", dent.d_name, mystat.st_size / 1048576, mystat.st_atime);
                    if (mystat.st_atime < oldest) {
                        oldest = mystat.st_atime;
                        snprintf(oldestpath, EUCA_MAX_PATH, "%s", path);
                        snprintf(oldestmanifestpath, EUCA_MAX_PATH, "%s.manifest.xml", path);
                    }
                    total_megs += mystat.st_size / 1048576;
                }
            }
            rc = readdir_r(DH, &dent, &result);
        }
        closedir(DH);
        LOGDEBUG("summary: totalMBs=%d oldestAtime=%ld oldestFile=%s\n", total_megs, oldest, oldestpath);
        if (total_megs > config->proxy_max_cache_size) {
            // start slowly deleting
            LOGINFO("invalidating cached image %s\n", oldestpath);
            unlink(oldestpath);
            unlink(oldestmanifestpath);
        }
    }

    return (0);
}

//!
//!
//!
//! @param[in]  res
//! @param[out] numHosts
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int image_cache_proxykick(ccResource * res, int *numHosts)
{
    int i = 0;
    int rc = 0;
    char *nodestr = NULL;
    char cmd[EUCA_MAX_PATH] = "";

    if ((nodestr = EUCA_ZALLOC((((*numHosts) * 128) + (*numHosts) + 1), sizeof(char))) == NULL) {
        LOGFATAL("out of memory!\n");
        unlock_exit(1);
    }

    for (i = 0; i < (*numHosts); i++) {
        strcat(nodestr, res[i].hostname);
        strcat(nodestr, " ");
    }

    snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_HELPER_DIR "/dynserv.pl %s %s", config->eucahome, config->proxyPath, nodestr);
    LOGDEBUG("running cmd '%s'\n", cmd);
    rc = system(cmd);

    EUCA_FREE(nodestr);
    return (rc);
}
