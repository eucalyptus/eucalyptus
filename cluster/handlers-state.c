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
//! @file cluster/handlers-state.c
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

#include <eucalyptus.h>
#include "axis2_skel_EucalyptusCC.h"

#include <server-marshal.h>
#include <handlers.h>
#include <misc.h>
#include <euca_string.h>
#include <ipc.h>
#include <objectstorage.h>

#include <euca_axis.h>
#include "data.h"
#include "client-marshal.h"

#include <storage-windows.h>
#include <euca_auth.h>
#include <handlers-state.h>

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

extern ccConfig *config;

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
//!
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  serviceIds a list of service info.
//! @param[in]  serviceIdsLen the number of service info in the serviceIds list
//! @param[out] outStatuses list of service status
//! @param[out] outStatusesLen number of service status in the outStatuses list
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doDescribeServices(ncMetadata * pMeta, serviceInfoType * serviceIds, int serviceIdsLen, serviceStatusType ** outStatuses, int *outStatusesLen)
{
    int rc = 0;
    int i = 0;
    int j = 0;
    int port = 0;
    char uri[EUCA_MAX_PATH] = { 0 };
    char uriType[32] = { 0 };
    char host[EUCA_MAX_PATH] = { 0 };
    char path[EUCA_MAX_PATH] = { 0 };
    serviceStatusType *myStatus = NULL;
    int do_report_cluster = 1;         // always do report on the cluster, otherwise CC won't get ENABLED
    int do_report_nodes = 0;
    int do_report_all = 0;
    char *my_partition = NULL;

    rc = initialize(pMeta, TRUE);      // DescribeServices is the only authoritative source of epoch
    if (rc) {
        return (1);
    }

    LOGDEBUG("invoked: userId=%s, serviceIdsLen=%d\n", SP(pMeta ? pMeta->userId : "UNKNOWN"), serviceIdsLen);

    //! @TODO for now, return error if list of services is passed in as parameter
    /*
       if (serviceIdsLen > 0) {
       LOGERROR("DescribeServices(): received non-zero number of input services, returning fail\n");
       *outStatusesLen = 0;
       *outStatuses = NULL;
       return(1);
       }
     */
    sem_mywait(CONFIG);
    {
        if (!strcmp(config->ccStatus.serviceId.name, "self")) {
            for (i = 0; i < serviceIdsLen; i++) {
                LOGDEBUG("received input serviceId[%d]\n", i);
                if (strlen(serviceIds[i].type)) {
                    if (!strcmp(serviceIds[i].type, "cluster")) {
                        snprintf(uri, EUCA_MAX_PATH, "%s", serviceIds[i].uris[0]);
                        rc = tokenize_uri(uri, uriType, host, &port, path);
                        if (strlen(host)) {
                            LOGDEBUG("setting local serviceId to input serviceId (type=%s name=%s partition=%s)\n",
                                     SP(serviceIds[i].type), SP(serviceIds[i].name), SP(serviceIds[i].partition));
                            memcpy(&(config->ccStatus.serviceId), &(serviceIds[i]), sizeof(serviceInfoType));
                        }
                    } else if (!strcmp(serviceIds[i].type, "node")) {
                        do_report_nodes = 1;    // report on node services if requested explicitly
                    }
                }
            }
        }
    }
    sem_mypost(CONFIG);
    if (serviceIdsLen < 1) {           // if the describe request is not specific, report on everything
        do_report_cluster = 1;
        do_report_nodes = 1;
        do_report_all = 1;
    } else {                           // if the describe request is specific, identify which types are requested
        do_report_cluster = 0;
        do_report_nodes = 0;
        do_report_all = 0;
        for (i = 0; i < serviceIdsLen; i++) {
            LOGDEBUG("received input serviceId[%d]: %s %s %s %s\n", i, serviceIds[i].type, serviceIds[i].partition, serviceIds[i].name, serviceIds[i].uris[0]);
            if (strlen(serviceIds[i].type)) {
                if (!strcmp(serviceIds[i].type, "cluster")) {
                    do_report_cluster = 1;  // report on cluster services if requested explicitly
                } else if (!strcmp(serviceIds[i].type, "node")) {
                    do_report_nodes++; // count number of and report on node services if requested explicitly
                }
            }
        }
    }

    for (i = 0; i < 16; i++) {
        if (strlen(config->services[i].type)) {
            LOGDEBUG("internal serviceInfos type=%s name=%s partition=%s urisLen=%d\n", config->services[i].type,
                     config->services[i].name, config->services[i].partition, config->services[i].urisLen);
            if (!strcmp(config->services[i].type, "cluster")) {
                my_partition = config->services[i].partition;
            }
            for (j = 0; j < MAX_SERVICE_URIS; j++) {
                if (strlen(config->services[i].uris[j])) {
                    LOGDEBUG("internal serviceInfos\t uri[%d]:%s\n", j, config->services[i].uris[j]);
                }
            }
        }
    }

    for (i = 0; i < 16; i++) {
        if (strlen(config->disabledServices[i].type)) {
            LOGDEBUG("internal disabled serviceInfos type=%s name=%s partition=%s urisLen=%d\n", config->disabledServices[i].type,
                     config->disabledServices[i].name, config->disabledServices[i].partition, config->disabledServices[i].urisLen);
            for (j = 0; j < MAX_SERVICE_URIS; j++) {
                if (strlen(config->disabledServices[i].uris[j])) {
                    LOGDEBUG("internal disabled serviceInfos\t uri[%d]:%s\n", j, config->disabledServices[i].uris[j]);
                }
            }
        }
    }

    for (i = 0; i < 16; i++) {
        if (strlen(config->notreadyServices[i].type)) {
            LOGDEBUG("internal not ready serviceInfos type=%s name=%s partition=%s urisLen=%d\n", config->notreadyServices[i].type,
                     config->notreadyServices[i].name, config->notreadyServices[i].partition, config->notreadyServices[i].urisLen);
            for (j = 0; j < MAX_SERVICE_URIS; j++) {
                if (strlen(config->notreadyServices[i].uris[j])) {
                    LOGDEBUG("internal not ready serviceInfos\t uri[%d]:%s\n", j, config->notreadyServices[i].uris[j]);
                }
            }
        }
    }

    *outStatusesLen = 0;
    *outStatuses = NULL;

    if (do_report_cluster) {
        (*outStatusesLen) += 1;
        *outStatuses = EUCA_ZALLOC(1, sizeof(serviceStatusType));
        if (!*outStatuses) {
            LOGFATAL("out of memory!\n");
            unlock_exit(1);
        }

        myStatus = *outStatuses;
        snprintf(myStatus->localState, 32, "%s", config->ccStatus.localState);  // ENABLED, DISABLED, STOPPED, NOTREADY
        snprintf(myStatus->details, 1024, "%s", config->ccStatus.details);  // string that gets printed by 'euca-describe-services -E'
        myStatus->localEpoch = config->ccStatus.localEpoch;
        memcpy(&(myStatus->serviceId), &(config->ccStatus.serviceId), sizeof(serviceInfoType));
        LOGDEBUG("external services\t uri[%d]: %s %s %s %s %s\n",
                 0, myStatus->serviceId.type, myStatus->serviceId.partition, myStatus->serviceId.name, myStatus->localState, myStatus->serviceId.uris[0]);
    }

    if (do_report_nodes) {
        extern ccResourceCache *resourceCache;
        ccResourceCache resourceCacheLocal;

        sem_mywait(RESCACHE);
        memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
        sem_mypost(RESCACHE);

        if (resourceCacheLocal.numResources > 0 && my_partition != NULL) {  // parition is unknown at early stages of CC initialization
            for (int idIdx = 0; idIdx < serviceIdsLen; idIdx++) {
                if (do_report_all || !strcmp(serviceIds[idIdx].type, "node")) {
                    for (int rIdx = 0; rIdx < resourceCacheLocal.numResources; rIdx++) {
                        ccResource *r = resourceCacheLocal.resources + rIdx;
                        if (do_report_all || (strlen(serviceIds[idIdx].name) && strlen(r->ip) && !strcmp(serviceIds[idIdx].name, r->ip))) {

                            // we have a node that we want to report about
                            (*outStatusesLen) += 1;
                            *outStatuses = EUCA_REALLOC(*outStatuses, *outStatusesLen, sizeof(serviceStatusType));
                            if (*outStatuses == NULL) {
                                LOGFATAL("out of memory! (outStatusesLen=%d)\n", *outStatusesLen);
                                unlock_exit(1);
                            }
                            myStatus = *outStatuses + *outStatusesLen - 1;
                            {
                                int resState = r->state;
                                int resNcState = r->ncState;
                                char *state = "BUGGY";
                                char *msg = "";
                                if (resState == RESUP) {
                                    if (resNcState == ENABLED) {
                                        state = "ENABLED";
                                        msg = "the node is operating normally";
                                    } else if (resNcState == STOPPED) {
                                        state = "STOPPED";
                                        msg = "the node is not accepting new instances";
                                    } else if (resNcState == NOTREADY) {
                                        state = "NOTREADY";
                                        if (strnlen(r->nodeMessage, 1024)) {
                                            msg = r->nodeMessage;
                                        } else {
                                            msg = "the node is currently experiencing problems and needs attention";
                                        }
                                    }
                                } else if (resState == RESASLEEP || resState == RESWAKING) {
                                    state = "NOTREADY";
                                    msg = "the node is currently in the sleep state";
                                } else if (resState == RESDOWN) {
                                    state = "NOTREADY";
                                    if (strnlen(r->nodeMessage, 1024)) {
                                        msg = r->nodeMessage;
                                    } else {
                                        msg = "the node is not responding to the cluster controller";
                                    }
                                }
                                snprintf(myStatus->localState, 32, "%s", state);
                                snprintf(myStatus->details, 1024, "%s", msg);   // string that gets printed by 'euca-describe-services -E'
                            }
                            myStatus->localEpoch = config->ccStatus.localEpoch;
                            sprintf(myStatus->serviceId.type, "node");
                            sprintf(myStatus->serviceId.name, "%s", r->hostname);
                            sprintf(myStatus->serviceId.partition, "%s", config->ccStatus.serviceId.partition);
                            sprintf(myStatus->serviceId.uris[0], "%s", r->ncURL);
                            myStatus->serviceId.urisLen = 1;
                            LOGDEBUG("external services\t uri[%d]: %s %s %s %s %s\n",
                                     idIdx, myStatus->serviceId.type, myStatus->serviceId.partition, myStatus->serviceId.name, myStatus->localState, myStatus->serviceId.uris[0]);
                        }
                    }
                }
            }
        }
    }

    LOGDEBUG("done\n");
    return (0);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] serviceIds
//! @param[in] serviceIdsLen
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doStartService(ncMetadata * pMeta, serviceInfoType * serviceIds, int serviceIdsLen)
{
    int rc = 0;
    int ret = 0;

    rc = initialize(pMeta, FALSE);
    if (rc) {
        return (1);
    }

    LOGDEBUG("invoked: userId=%s\n", SP(pMeta ? pMeta->userId : "UNKNOWN"));

    int start_cc = 0;
    for (int i = 0; i < serviceIdsLen; i++) {
        if (strcmp(serviceIds[i].type, "cluster") == 0) {
            start_cc = 1;
        } else if (strcmp(serviceIds[i].type, "node") == 0) {
            //GRZE: This is currently a NOOP; also "enabled" would not be right
            //ret = doModifyNode(pMeta, serviceIds[i].name, "enabled");
        }
    }
    if (serviceIdsLen < 1) {
        start_cc = 1;
    }
    if (start_cc != 1)
        goto done;

    // this is actually a NOP
    sem_mywait(CONFIG);
    {
        if (config->ccState == SHUTDOWNCC) {
            LOGWARN("attempt to start a shutdown CC, skipping.\n");
            ret++;
        } else if (ccCheckState(0)) {
            LOGWARN("ccCheckState() returned failures, skipping.\n");
            ret++;
        } else {
            LOGINFO("starting service\n");
            ret = 0;
            config->kick_enabled = 0;
            ccChangeState(DISABLED);
        }
    }
    sem_mypost(CONFIG);

done:
    LOGTRACE("done\n");

    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] serviceIds
//! @param[in] serviceIdsLen
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doStopService(ncMetadata * pMeta, serviceInfoType * serviceIds, int serviceIdsLen)
{
    int rc = 0;
    int ret = 0;

    rc = initialize(pMeta, FALSE);
    if (rc) {
        return (1);
    }

    LOGDEBUG("invoked: userId=%s\n", SP(pMeta ? pMeta->userId : "UNKNOWN"));

    int stop_cc = 0;
    for (int i = 0; i < serviceIdsLen; i++) {
        if (strcmp(serviceIds[i].type, "cluster") == 0) {
            stop_cc = 1;
        } else if (strcmp(serviceIds[i].type, "node") == 0) {
            //GRZE: map stop operation to modifyNode("disabled") here
            ret = doModifyNode(pMeta, serviceIds[i].name, "disabled");
        }
    }
    if (serviceIdsLen < 1) {
        stop_cc = 1;
    }
    if (stop_cc != 1)
        goto done;

    sem_mywait(CONFIG);
    {
        if (config->ccState == SHUTDOWNCC) {
            LOGWARN("attempt to stop a shutdown CC, skipping.\n");
            ret++;
        } else if (ccCheckState(0)) {
            LOGWARN("ccCheckState() returned failures, skipping.\n");
            ret++;
        } else {
            LOGINFO("stopping service\n");
            ret = 0;
            config->kick_enabled = 0;
            ccChangeState(STOPPED);
        }
    }
    sem_mypost(CONFIG);

done:
    LOGTRACE("done\n");

    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] serviceIds
//! @param[in] serviceIdsLen
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doEnableService(ncMetadata * pMeta, serviceInfoType * serviceIds, int serviceIdsLen)
{
    int i = 0;
    int rc = 0;
    int ret = 0;
    int done = 0;

    rc = initialize(pMeta, FALSE);
    if (rc) {
        return (1);
    }

    LOGDEBUG("invoked: userId=%s\n", SP(pMeta ? pMeta->userId : "UNKNOWN"));

    int enable_cc = 0;
    for (int i = 0; i < serviceIdsLen; i++) {
        if (strcmp(serviceIds[i].type, "cluster") == 0) {
            enable_cc = 1;
        } else if (strcmp(serviceIds[i].type, "node") == 0) {
            ret = doModifyNode(pMeta, serviceIds[i].name, "enabled");
        }
    }
    if (serviceIdsLen < 1) {
        enable_cc = 1;
    }
    if (enable_cc != 1)
        goto done;

    sem_mywait(CONFIG);
    {
        if (config->ccState == SHUTDOWNCC) {
            LOGWARN("attempt to enable a shutdown CC, skipping.\n");
            ret++;
        } else if (ccCheckState(0)) {
            LOGWARN("ccCheckState() returned failures, skipping.\n");
            ret++;
        } else if (config->ccState != ENABLED) {
            LOGINFO("enabling service\n");
            ret = 0;
            // tell monitor thread to (re)enable
            config->kick_monitor_running = 0;
            config->kick_network = 1;
            config->kick_dhcp = 1;
            config->kick_enabled = 1;
            ccChangeState(ENABLED);
        }
    }
    sem_mypost(CONFIG);

    if (config->ccState == ENABLED) {
        // wait for a minute to make sure CC is running again
        done = 0;
        for (i = 0; i < 60 && !done; i++) {
            sem_mywait(CONFIG);
            {
                if (config->kick_monitor_running) {
                    done++;
                }
            }
            sem_mypost(CONFIG);

            if (!done) {
                LOGDEBUG("waiting for monitor to re-initialize (%d/60)\n", i);
                sleep(1);
            }
        }
    }

done:
    LOGTRACE("done\n");
    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] serviceIds
//! @param[in] serviceIdsLen
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doDisableService(ncMetadata * pMeta, serviceInfoType * serviceIds, int serviceIdsLen)
{
    int rc = 0;
    int ret = 0;

    rc = initialize(pMeta, FALSE);
    if (rc) {
        return (1);
    }

    LOGDEBUG("invoked: userId=%s\n", SP(pMeta ? pMeta->userId : "UNKNOWN"));

    int disable_cc = 0;
    for (int i = 0; i < serviceIdsLen; i++) {
        if (strcmp(serviceIds[i].type, "cluster") == 0) {
            disable_cc = 1;
        } else if (strcmp(serviceIds[i].type, "node") == 0) {
            ret = doModifyNode(pMeta, serviceIds[i].name, "disabled");
        }
    }
    if (serviceIdsLen < 1) {
        disable_cc = 1;
    }
    if (disable_cc != 1)
        goto done;

    sem_mywait(CONFIG);
    {
        if (config->ccState == SHUTDOWNCC) {
            LOGWARN("attempt to disable a shutdown CC, skipping.\n");
            ret++;
        } else if (ccCheckState(0)) {
            LOGWARN("ccCheckState() returned failures, skipping.\n");
            ret++;
        } else {
            LOGINFO("disabling service\n");
            ret = 0;
            config->kick_enabled = 0;
            ccChangeState(DISABLED);
        }
    }
    sem_mypost(CONFIG);

done:
    LOGTRACE("done\n");
    return (ret);
}

//!
//!
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int doShutdownService(ncMetadata * pMeta)
{
    int rc = 0;
    int ret = 0;

    rc = initialize(pMeta, FALSE);
    if (rc) {
        return (1);
    }

    LOGDEBUG("invoked: userId=%s\n", SP(pMeta ? pMeta->userId : "UNKNOWN"));

    sem_mywait(CONFIG);
    {
        config->kick_enabled = 0;
        ccChangeState(SHUTDOWNCC);
    }
    sem_mypost(CONFIG);

    LOGTRACE("done\n");
    return (ret);
}

//!
//!
//!
//! @param[in] inst a pointer to the instance structure
//! @param[in] in a transparent pointer (unused)
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int validCmp(ccInstance * inst, void *in)
{
    if (!inst) {
        return (1);
    }

    if (inst->instanceId[0] == '\0') {
        return (1);
    }

    return (0);
}

//!
//!
//!
//! @param[in] inst a pointer to the instance structure
//! @param[in] in a transparent pointer (unused)
//!
//! @return
//!
//! @pre The inst pointer must not be NULL.
//!
//! @note
//!
int instIpSync(ccInstance * inst, void *in)
{
    int ret = 0;

    if (!inst) {
        return (1);
    } else if ((strcmp(inst->state, "Pending") && strcmp(inst->state, "Extant"))) {
        return (0);
    }

    LOGDEBUG("instanceId=%s CCpublicIp=%s CCprivateIp=%s CCprivateMac=%s CCvlan=%d CCnetworkIndex=%d NCpublicIp=%s NCprivateIp=%s NCprivateMac=%s "
             "NCvlan=%d NCnetworkIndex=%d\n", inst->instanceId, inst->ccnet.publicIp, inst->ccnet.privateIp, inst->ccnet.privateMac, inst->ccnet.vlan,
             inst->ccnet.networkIndex, inst->ncnet.publicIp, inst->ncnet.privateIp, inst->ncnet.privateMac, inst->ncnet.vlan, inst->ncnet.networkIndex);

    if (inst->ccnet.vlan == 0 && inst->ccnet.networkIndex == 0 && inst->ccnet.publicIp[0] == '\0' && inst->ccnet.privateIp[0] == '\0' && inst->ccnet.privateMac[0] == '\0') {
        // ccnet is completely empty, make a copy of ncnet
        LOGDEBUG("ccnet is empty, copying ncnet\n");
        memcpy(&(inst->ccnet), &(inst->ncnet), sizeof(netConfig));
        return (1);
    }
    // IP cases
    // 1.) local CC cache has no IP info for VM, NC VM has no IP info
    //     - do nothing
    // 2.) local CC cache has no IP info, NC VM has IP info
    //     - ingress NC info, kick_network
    // 3.) local CC cache has IP info, NC VM has no IP info
    //     - send ncAssignAddress
    // 4.) local CC cache has IP info, NC VM has different IP info
    //     - ingress NC info, kick_network
    // 5.) local CC cache has IP info, NC VM has same IP info
    //     - do nothing
    if ((inst->ccnet.publicIp[0] == '\0' || !strcmp(inst->ccnet.publicIp, "0.0.0.0"))
        && (inst->ncnet.publicIp[0] != '\0' && strcmp(inst->ncnet.publicIp, "0.0.0.0"))) {
        // case 2
        LOGDEBUG("CC publicIp is empty, NC publicIp is set\n");
        snprintf(inst->ccnet.publicIp, INET_ADDR_LEN, "%s", inst->ncnet.publicIp);
        ret++;
    } else if (((inst->ccnet.publicIp[0] != '\0' && strcmp(inst->ccnet.publicIp, "0.0.0.0"))
                && (inst->ncnet.publicIp[0] != '\0' && strcmp(inst->ncnet.publicIp, "0.0.0.0")))
               && strcmp(inst->ccnet.publicIp, inst->ncnet.publicIp)) {
        // case 4
        LOGDEBUG("CC publicIp and NC publicIp differ\n");
        snprintf(inst->ccnet.publicIp, INET_ADDR_LEN, "%s", inst->ncnet.publicIp);
        ret++;
    }
    // VLAN cases
    if (inst->ccnet.vlan != inst->ncnet.vlan) {
        // problem
        LOGERROR("CC and NC vlans differ instanceId=%s CCvlan=%d NCvlan=%d\n", inst->instanceId, inst->ccnet.vlan, inst->ncnet.vlan);
    }

    inst->ccnet.vlan = inst->ncnet.vlan;

    // networkIndex cases
    if (inst->ccnet.networkIndex != inst->ncnet.networkIndex) {
        // problem
        LOGERROR("CC and NC networkIndicies differ instanceId=%s CCnetworkIndex=%d NCnetworkIndex=%d\n", inst->instanceId, inst->ccnet.networkIndex, inst->ncnet.networkIndex);
    }
    inst->ccnet.networkIndex = inst->ncnet.networkIndex;

    // mac addr cases
    if (strcmp(inst->ccnet.privateMac, inst->ncnet.privateMac)) {
        // problem;
        LOGERROR("CC and NC mac addrs differ instanceId=%s CCmac=%s NCmac=%s\n", inst->instanceId, inst->ccnet.privateMac, inst->ncnet.privateMac);
    }
    snprintf(inst->ccnet.privateMac, ENET_ADDR_LEN, "%s", inst->ncnet.privateMac);

    // privateIp cases
    if (strcmp(inst->ccnet.privateIp, inst->ncnet.privateIp)) {
        // sync em
        snprintf(inst->ccnet.privateIp, INET_ADDR_LEN, "%s", inst->ncnet.privateIp);
    }

    return (ret);
}
