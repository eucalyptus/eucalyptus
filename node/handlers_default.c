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
//! @file node/handlers_default.c
//! This implements the default operations handlers supported by all hypervisor.
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64           // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU                      /* strnlen */
#include <string.h>                    /* strlen, strcpy */
#include <time.h>
#ifndef __DARWIN_UNIX03
#include <limits.h>                    /* INT_MAX */
#else /* ! _DARWIN_C_SOURCE */
#include <i386/limits.h>
#endif /* ! _DARWIN_C_SOURCE */
#include <sys/types.h>                 /* fork */
#include <sys/wait.h>                  /* waitpid */
#include <unistd.h>
#include <fcntl.h>
#include <assert.h>
#include <errno.h>
#include <sys/stat.h>
#include <pthread.h>
#include <signal.h>                    /* SIGINT */
#include <libvirt/libvirt.h>
#include <libvirt/virterror.h>
#include <ctype.h>
#include <linux/limits.h>

#include <eucalyptus.h>
#include <ipc.h>
#include <misc.h>
#include <backing.h>
#include <vnetwork.h>
#include <euca_auth.h>
#include <vbr.h>
#include <sensor.h>
#include <euca_string.h>
#include <euca_file.h>

#include "handlers.h"
#include "xml.h"
#include "hooks.h"
#include <ebs_utils.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define VOL_RETRIES 3
#define SHUTDOWN_GRACE_PERIOD_SEC 60

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

//! Struct used to pass parameters to startstop_thread by the two functions that use it
typedef struct startstop_params_ {
    char instanceId[CHAR_BUFFER_SIZE];
    boolean do_stop;
} startstop_params;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

// coming from handlers.c
extern sem *hyp_sem;
extern sem *inst_sem;
extern sem *inst_copy_sem;
extern bunchOfInstances *global_instances;
extern bunchOfInstances *global_instances_copy;

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
 |                             EXTERNAL PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */
extern int update_disk_aliases(ncInstance * instance);  // defined in handlers.c

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int doInitialize(struct nc_state_t *nc);
static int doRunInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *uuid, char *instanceId, char *reservationId, virtualMachine * params,
                         char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *ownerId,
                         char *accountId, char *keyName, netConfig * netparams, char *userData, char *credential, char *launchIndex, char *platform, int expiryTime,
                         char **groupNames, int groupNamesSize, ncInstance ** outInst);
static int doRebootInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId);
static int doGetConsoleOutput(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char **consoleOutput);
static int doTerminateInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, int force, int *shutdownState, int *previousState);
static int doDescribeInstances(struct nc_state_t *nc, ncMetadata * pMeta, char **instIds, int instIdsLen, ncInstance *** outInsts, int *outInstsLen);
static int doDescribeResource(struct nc_state_t *nc, ncMetadata * pMeta, char *resourceType, ncResource ** outRes);
static int doBroadcastNetworkInfo(struct nc_state_t *nc, ncMetadata * pMeta, char *networkInfo);
static int doAssignAddress(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *publicIp);
static int doPowerDown(struct nc_state_t *nc, ncMetadata * pMeta);
static int doStartNetwork(struct nc_state_t *nc, ncMetadata * pMeta, char *uuid, char **remoteHosts, int remoteHostsLen, int port, int vlan);
static int xen_detach_helper(struct nc_state_t *nc, char *instanceId, char *localDevReal, char *xml);
static int doAttachVolume(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev);
static int doDetachVolume(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev, int force, int grab_inst_sem);
static void change_createImage_state(ncInstance * instance, createImage_progress state);
static int cleanup_createImage_task(ncInstance * instance, struct createImage_params_t *params, instance_states state, createImage_progress result);
static void *createImage_thread(void *arg);
static int doCreateImage(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev);
static void change_bundling_state(ncInstance * instance, bundling_progress state);
static int cleanup_bundling_task(ncInstance * instance, struct bundling_params_t *params, bundling_progress result);
static void *bundling_thread(void *arg);
static int doBundleInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *objectStorageURL,
                            char *userPublicKey, char *S3Policy, char *S3PolicySig);
static int doBundleRestartInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *psInstanceId);
static int doCancelBundleTask(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId);
static int doDescribeBundleTasks(struct nc_state_t *nc, ncMetadata * pMeta, char **instIds, int instIdsLen, bundleTask *** outBundleTasks, int *outBundleTasksLen);
static int doDescribeSensors(struct nc_state_t *nc, ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds,
                             int instIdsLen, char **sensorIds, int sensorIdsLen, sensorResource *** outResources, int *outResourcesLen);
static int doModifyNode(struct nc_state_t *nc, ncMetadata * pMeta, char *stateName);
static int doMigrateInstances(struct nc_state_t *nc, ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials);
static void *startstop_thread(void *arg);
static int doStartInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId);
static int doStopInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              CALLBACK STRUCTURE                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Default LIBVIRT operation handlers
struct handlers default_libvirt_handlers = {
    .name = "default",
    .doInitialize = doInitialize,
    .doDescribeInstances = doDescribeInstances,
    .doRunInstance = doRunInstance,
    .doTerminateInstance = doTerminateInstance,
    .doRebootInstance = doRebootInstance,
    .doGetConsoleOutput = doGetConsoleOutput,
    .doDescribeResource = doDescribeResource,
    .doStartNetwork = doStartNetwork,
    .doBroadcastNetworkInfo = doBroadcastNetworkInfo,
    .doAssignAddress = doAssignAddress,
    .doPowerDown = doPowerDown,
    .doAttachVolume = doAttachVolume,
    .doDetachVolume = doDetachVolume,
    .doCreateImage = doCreateImage,
    .doBundleInstance = doBundleInstance,
    .doBundleRestartInstance = doBundleRestartInstance,
    .doCancelBundleTask = doCancelBundleTask,
    .doDescribeBundleTasks = doDescribeBundleTasks,
    .doDescribeSensors = doDescribeSensors,
    .doModifyNode = doModifyNode,
    .doMigrateInstances = doMigrateInstances,
    .doStartInstance = doStartInstance,
    .doStopInstance = doStopInstance,
};

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
//! Default hypervisor initialize handler
//!
//! @param[in] nc a pointer to the NC state structure
//!
//! @return Always returns EUCA_OK for now
//!
static int doInitialize(struct nc_state_t *nc)
{
    return (EUCA_OK);
}

//!
//! Default hypervisor handler to run instances
//!
//! @param[in]  nc a pointer to the NC state structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  uuid unique user identifier string
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in]  reservationId the reservation identifier string
//! @param[in]  params a pointer to the virtual machine parameters to use
//! @param[in]  imageId UNUSED
//! @param[in]  imageURL UNUSED
//! @param[in]  kernelId the kernel image identifier (eki-XXXXXXXX)
//! @param[in]  kernelURL the kernel image URL address
//! @param[in]  ramdiskId the ramdisk image identifier (eri-XXXXXXXX)
//! @param[in]  ramdiskURL the ramdisk image URL address
//! @param[in]  ownerId the owner identifier string
//! @param[in]  accountId the account identifier string
//! @param[in]  keyName the key name string
//! @param[in]  netparams a pointer to the network parameters string
//! @param[in]  userData the user data string
//! @param[in]  credential the credential string
//! @param[in]  launchIndex the launch index string
//! @param[in]  platform the platform name string
//! @param[in]  expiryTime the reservation expiration time
//! @param[in]  groupNames a list of group name string
//! @param[in]  groupNamesSize the number of group name in the groupNames list
//! @param[out] outInstPtr the list of instances created by this request
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR
//!         EUCA_MEMORY_ERROR and EUCA_THREAD_ERROR
//!
static int doRunInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *uuid, char *instanceId, char *reservationId, virtualMachine * params,
                         char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *ownerId,
                         char *accountId, char *keyName, netConfig * netparams, char *userData, char *credential, char *launchIndex, char *platform, int expiryTime,
                         char **groupNames, int groupNamesSize, ncInstance ** outInstPtr)
{
    int ret = EUCA_OK;
    ncInstance *instance = NULL;
    netConfig ncnet = { 0 };

    *outInstPtr = NULL;

    memcpy(&ncnet, netparams, sizeof(netConfig));

    // check as much as possible before forking off and returning
    sem_p(inst_sem);
    instance = find_instance(&global_instances, instanceId);
    sem_v(inst_sem);
    if (instance) {
        if (instance->state == TEARDOWN) {
            // fully cleaned up, so OK to revive it, e.g., with euca-start-instance
            remove_instance(&global_instances, instance);
            free_instance(&instance);
        } else {
            LOGERROR("[%s] instance already running\n", instanceId);
            return EUCA_ERROR;         //! @todo return meaningful error codes?
        }
    }
    instance = allocate_instance(uuid, instanceId, reservationId, params, instance_state_names[PENDING], PENDING, pMeta->userId, ownerId, accountId,
                                 &ncnet, keyName, userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize);
    if (kernelId)
        euca_strncpy(instance->kernelId, kernelId, sizeof(instance->kernelId));

    if (ramdiskId)
        euca_strncpy(instance->ramdiskId, ramdiskId, sizeof(instance->ramdiskId));

    if (instance == NULL) {
        LOGERROR("[%s] could not allocate instance struct\n", instanceId);
        return EUCA_MEMORY_ERROR;
    }
    instance->launchTime = time(NULL);

    // parse and sanity-check the virtual boot record
    if (vbr_parse(&(instance->params), pMeta) != EUCA_OK) {
        ret = EUCA_ERROR;
        goto error;
    }
    // prepare instance credential
    if (credential && strlen(credential)) {
        char symm_key[512];
        char enc_key[KEY_STRING_SIZE];
        char enc_tok[KEY_STRING_SIZE];
        char *ptr[5];
        int i = 0;
        char *pch = strtok(credential, "\n");
        while (i < 5 && pch != NULL) {
            ptr[i++] = pch;
            pch = strtok(NULL, "\n");
        }
        if (i < 5) {
            LOGERROR("Malformed instance credential. Num tokens: %d\n", i);
        } else {
            euca_strncpy(instance->euareKey, ptr[0], sizeof(instance->euareKey));
            euca_strncpy(instance->instancePubkey, ptr[1], sizeof(instance->instancePubkey));
            euca_strncpy(enc_tok, ptr[2], sizeof(enc_tok));
            euca_strncpy(symm_key, ptr[3], sizeof(symm_key));
            euca_strncpy(enc_key, ptr[4], sizeof(enc_key));

            char *pk = NULL;
            int out_len = -1;
            if (decrypt_string_with_node_and_symmetric_key(enc_key, symm_key, &pk, &out_len) != EUCA_OK || out_len <= 0) {
                LOGERROR("failed to decrypt the instance credential\n");
            } else {
                memcpy(instance->instancePk, pk, strlen(pk));
                EUCA_FREE(pk);
                if (decrypt_string_with_node_and_symmetric_key(enc_tok, symm_key, &pk, &out_len) != EUCA_OK || out_len <= 0) {
                    LOGERROR("failed to decrypt the instance token\n");
                } else {
                    memcpy(instance->instanceToken, pk, strlen(pk));
                    EUCA_FREE(pk);
                }
            }
        }
    }

    change_state(instance, STAGING);

    sem_p(inst_sem);
    int error = add_instance(&global_instances, instance);
    copy_instances();
    sem_v(inst_sem);
    if (error) {
        LOGERROR("[%s] could not save instance struct\n", instanceId);
        ret = EUCA_ERROR;
        goto error;
    }
    // do the potentially long tasks in a thread
    pthread_attr_t *attr = (pthread_attr_t *) EUCA_ZALLOC(1, sizeof(pthread_attr_t));
    if (!attr) {
        LOGERROR("[%s] out of memory\n", instanceId);
        ret = EUCA_MEMORY_ERROR;
        goto error;
    }
    pthread_attr_init(attr);
    pthread_attr_setdetachstate(attr, PTHREAD_CREATE_DETACHED);

    if (pthread_create(&(instance->tcb), attr, startup_thread, (void *)instance)) {
        pthread_attr_destroy(attr);
        LOGERROR("[%s] failed to spawn a VM startup thread\n", instanceId);
        sem_p(inst_sem);
        remove_instance(&global_instances, instance);
        copy_instances();
        sem_v(inst_sem);
        EUCA_FREE(attr);
        ret = EUCA_THREAD_ERROR;
        goto error;
    }
    pthread_attr_destroy(attr);
    EUCA_FREE(attr);

    *outInstPtr = instance;
    return EUCA_OK;

error:
    free_instance(&instance);
    return (ret);
}

//!
//! Unsupported reboot instance operation. Needs to be redefined for each hypervisor.
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return Always return EUCA_FATAL_ERROR
//!
static int doRebootInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId)
{
    LOGERROR("[%s] no default for %s!\n", instanceId, __func__);
    return (EUCA_UNSUPPORTED_ERROR);
}

//!
//! Unsupported get console output operation. Needs to be redefined for each hypervisor.
//!
//! @param[in]  nc a pointer to the NC state structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[out] consoleOutput a pointer to the unallocated string that will contain the output
//!
//! @return Always return EUCA_FATAL_ERROR
//!
static int doGetConsoleOutput(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char **consoleOutput)
{
    LOGERROR("[%s] no default for %s!\n", instanceId, __func__);
    return (EUCA_UNSUPPORTED_ERROR);
}

//!
//! given instance ID, first tries to shut down the domain
//! gracefully (via ACPI signal to the OS), then, after a timeout,
//! forecfully shuts it down
//!
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] do_destroy set to TRUE if we need to destroy the domain
//!
//! @return 0 for success and -1 for failure
//!
int shutdown_then_destroy_domain(const char *instanceId, boolean do_destroy)
{
    time_t deadline = 0;
    int error = 0;

    for (boolean done = FALSE; (!done);) {
        virConnectPtr conn = lock_hypervisor_conn();
        if (conn == NULL) {
            LOGERROR("[%s] cannot connect to hypervisor to shut down instance\n", instanceId);
            return -1;
        }

        virDomainPtr dom = virDomainLookupByName(conn, instanceId);
        if (dom == NULL) {             // domain is gone, so we are done
            LOGTRACE("[%s] domain not found\n", instanceId);
            unlock_hypervisor_conn();
            break;
        }

        boolean failed_to_shutdown = FALSE;

        if (deadline == 0) {           // first time through the loop
            deadline = time(NULL) + SHUTDOWN_GRACE_PERIOD_SEC;

            // give OS a chance to shut down cleanly
            LOGDEBUG("shutting down instance\n");
            error = virDomainShutdown(dom);
            if (error) {
                failed_to_shutdown = TRUE;
            }

        } else if (time(NULL) < deadline) { // within grace period - check on domain
            int dom_status = virDomainIsActive(dom);
            LOGTRACE("domain status '%d'\n", dom_status);
            if (dom_status != 1)       // 1 if running, 0 if inactive, -1 on error
                done = TRUE;

        } else {                       // deadline exceeded
            failed_to_shutdown = TRUE;
        }

        if (do_destroy && failed_to_shutdown) {
            LOGDEBUG("destroying instance\n");
            error = virDomainDestroy(dom);
            done = TRUE;
        }

        virDomainFree(dom);
        unlock_hypervisor_conn();

        if (!done)
            sleep(2);                  // sleep outside the hypervisor lock
    }

    return error;
}

//!
//! finds instance by ID and destroys it on the hypervisor
//!
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_NOT_FOUND_ERROR.
//!
int find_and_terminate_instance(char *instanceId)
{
    char state = 0;
    int err = 0;

    {
        // ensure the instance is known and save its last state on the stack
        sem_p(inst_sem);
        ncInstance *instance = find_instance(&global_instances, instanceId);
        if (instance == NULL) {
            sem_v(inst_sem);
            return EUCA_NOT_FOUND_ERROR;
        }
        state = instance->state;
        sem_v(inst_sem);
    }

    // try stopping the domain
    err = shutdown_then_destroy_domain(instanceId, TRUE);

    // log the outcome at the appropriate log level
    if (err == 0) {
        LOGINFO("[%s] instance terminated\n", instanceId);
    } else {
        if (state != BOOTING && state != STAGING && state != TEARDOWN) {
            LOGERROR("[%s] failed to terminate instance\n", instanceId);
        } else {
            LOGDEBUG("[%s] failed to terminate instance\n", instanceId);
        }
    }

    return EUCA_OK;
}

//!
//! finds instance by ID and stop it saving the important artifacts for a later restart
//!
//! @param[in] psInstanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_NOT_FOUND_ERROR.
//!
int find_and_stop_instance(char *psInstanceId)
{
    char *psXML = NULL;
    virDomainPtr dom = NULL;
    virConnectPtr conn = NULL;
    ncInstance *pInstance = NULL;

    LOGDEBUG("[%s] stopping instance\n", psInstanceId);

    {
        // we hold hyp_sem in this block
        if ((conn = lock_hypervisor_conn()) == NULL) {
            LOGERROR("[%s] cannot connect to hypervisor to stop instance, giving up\n", psInstanceId);
            return (EUCA_ERROR);
        }

        if ((dom = virDomainLookupByName(conn, psInstanceId)) == NULL) {
            LOGERROR("[%s] cannot locate instance to stop, giving up\n", psInstanceId);
            unlock_hypervisor_conn();
            return (EUCA_NOT_FOUND_ERROR);
        }
        // obtain the most up-to-date XML for domain from libvirt
        psXML = virDomainGetXMLDesc(dom, 0);
        virDomainFree(dom);            // release libvirt resource
        unlock_hypervisor_conn();
    }

    if (psXML == NULL) {
        LOGERROR("[%s] cannot obtain metadata for instance to stop, giving up\n", psInstanceId);
        return (EUCA_ERROR);
    }

    sem_p(inst_sem);
    {
        // we hold inst_sem in this block
        if ((pInstance = find_instance(&global_instances, psInstanceId)) == NULL) {
            LOGERROR("[%s] failed to locate instance in memory\n", psInstanceId);
            sem_v(inst_sem);
            EUCA_FREE(psXML);
            return (EUCA_NOT_FOUND_ERROR);
        }
        // verify that we are not already trying trying to shut this instance down
        if (pInstance->stop_requested == TRUE) {
            LOGERROR("[%s] instance shutdown already requested\n", psInstanceId);
            sem_v(inst_sem);
            EUCA_FREE(psXML);
            return (0);
        }
        // save the XML to the file system
        if (str2file(psXML, pInstance->libvirtFilePath, O_CREAT | O_TRUNC | O_WRONLY, BACKING_FILE_PERM, FALSE) != EUCA_OK) {
            LOGERROR("[%s] failed to update libvirt XML file %s\n", psInstanceId, pInstance->libvirtFilePath);
            sem_v(inst_sem);
            EUCA_FREE(psXML);
            return (EUCA_IO_ERROR);
        }
        // note in instance state that shutdown was authorized
        pInstance->stop_requested = TRUE;
        save_instance_struct(pInstance);
    }
    sem_v(inst_sem);
    EUCA_FREE(psXML);

    // try to shutdown
    if (shutdown_then_destroy_domain(psInstanceId, TRUE) != EUCA_OK) {
        LOGERROR("[%s] failed to shutdown\n", psInstanceId);
        return (EUCA_ERROR);
    }
    return (0);
}

//!
//! finds instance by ID and start it using the saved artifacts during the stop
//!
//! @param[in] psInstanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_NOT_FOUND_ERROR.
//!
int find_and_start_instance(char *psInstanceId)
{
    char *psXML = NULL;
    char sResourceName[1][MAX_SENSOR_NAME_LEN] = { "" };
    char sResourceAlias[1][MAX_SENSOR_NAME_LEN] = { "" };
    ncInstance *pInstance = NULL;
    virDomainPtr dom = NULL;
    virConnectPtr conn = NULL;

    LOGDEBUG("[%s] restarting instance\n", psInstanceId);

    sem_p(inst_sem);
    {
        // we hold inst_sem in this block
        if ((pInstance = find_instance(&global_instances, psInstanceId)) == NULL) {
            LOGERROR("[%s] failed to locate instance in memory\n", psInstanceId);
            sem_v(inst_sem);
            return (EUCA_NOT_FOUND_ERROR);
        }
        // verify that this instance was stopped earlier
        if (pInstance->stop_requested != TRUE) {
            LOGERROR("[%s] cannot start instance that was not stopped earlier\n", psInstanceId);
            sem_v(inst_sem);
            return (EUCA_ERROR);
        }
        // load the XML to the file system
        if ((psXML = file2str(pInstance->libvirtFilePath)) == NULL) {
            LOGERROR("[%s] failed to load libvirt XML from file file %s\n", psInstanceId, pInstance->libvirtFilePath);
            sem_v(inst_sem);
            return (EUCA_IO_ERROR);
        }
        // note in instance state that instance is not expected to be shut down anymore
        pInstance->stop_requested = FALSE;
        save_instance_struct(pInstance);
    }
    sem_v(inst_sem);

    //! @TODO: check if we sensor values survive stop/start

    // Add a shift to values of three of the metrics: ones that
    // drop back to zero after a reboot. The shift, which is based
    // on the latest value, ensures that values sent upstream do
    // not go backwards .
    sensor_shift_metric(psInstanceId, "CPUUtilization");
    sensor_shift_metric(psInstanceId, "NetworkIn");
    sensor_shift_metric(psInstanceId, "NetworkOut");

    {
        // we hold hyp_sem in this block
        if ((conn = lock_hypervisor_conn()) == NULL) {
            LOGERROR("[%s] cannot connect to hypervisor to restart instance, giving up\n", psInstanceId);
            EUCA_FREE(psXML);
            return (EUCA_ERROR);
        }
        // ensure it is not running already
        if ((dom = virDomainLookupByName(conn, psInstanceId)) != NULL) {
            LOGERROR("[%s] instance to start is already running, giving up\n", psInstanceId);
            unlock_hypervisor_conn();
            EUCA_FREE(psXML);
            virDomainFree(dom);
            return (EUCA_ERROR);
        }
        // start it
        if ((dom = virDomainCreateLinux(conn, psXML, 0)) == NULL) {
            LOGERROR("[%s] failed to start instance\n", psInstanceId);
        } else {
            //! @TODO: check if we sensor values survive stop/start
            euca_strncpy(sResourceName[0], psInstanceId, MAX_SENSOR_NAME_LEN);
            sensor_refresh_resources(sResourceName, sResourceAlias, 1); // refresh stats so we set base value accurately
            virDomainFree(dom);
        }
        unlock_hypervisor_conn();
    }
    EUCA_FREE(psXML);
    return (0);
}

//!
//! Finds and terminate an instance.
//!
//! @param[in]  nc a pointer to the NC state structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in]  force if set to 1 will force the termination of the instance
//! @param[out] shutdownState hard-coded to 0 on success
//! @param[out] previousState hard-coded to 0 on success
//!
//! @return EUCA_OK if instanceId is valid and the termination thread could be spawned
//!
//! @see find_and_terminate_instance()
//!
static int doTerminateInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, int force, int *shutdownState, int *previousState)
{
    ncInstance *instance = NULL;
    int err = EUCA_ERROR;
    char resourceName[1][MAX_SENSOR_NAME_LEN] = { {0} };
    char resourceAlias[1][MAX_SENSOR_NAME_LEN] = { {0} };

    {                                  // find the instance to ensure we know about it
        sem_p(inst_sem);
        instance = find_instance(&global_instances, instanceId);
        sem_v(inst_sem);
    }

    if (instance == NULL)
        return EUCA_NOT_FOUND_ERROR;

    // refresh stats so latest instance measurements are captured before it disappears
    euca_strncpy(resourceName[0], instanceId, MAX_SENSOR_NAME_LEN);
    sem_p(hyp_sem);                    // we serialize all hypervisor calls and sensor_refresh_resources() may ultimately call the hypervisor
    sensor_refresh_resources(resourceName, resourceAlias, 1);
    sem_v(hyp_sem);

    // do the shutdown in a thread
    pthread_attr_t tattr;
    pthread_t tid;
    pthread_attr_init(&tattr);
    pthread_attr_setdetachstate(&tattr, PTHREAD_CREATE_DETACHED);
    void *param = (void *)strdup(instanceId);
    if (pthread_create(&tid, &tattr, terminating_thread, (void *)param) != 0) {
        LOGERROR("[%s] failed to start VM termination thread\n", instanceId);
    } else {
        // previous and shutdown state are ignored by CC anyway
        *previousState = 0;
        *shutdownState = 0;
        err = EUCA_OK;
    }

    return err;
}

//!
//! Finds and retrieves information in regards to instances
//!
//! @param[in]  nc a pointer to the NC state structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instIds a pointer the list of instance identifiers to retrieve data for
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[out] outInsts a pointer the list of instances for which we have data
//! @param[out] outInstsLen the number of instances in the outInsts list.
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_MEMORY_ERROR.
//!
static int doDescribeInstances(struct nc_state_t *nc, ncMetadata * pMeta, char **instIds, int instIdsLen, ncInstance *** outInsts, int *outInstsLen)
{
    ncInstance *instance = NULL;
    ncInstance *tmp = NULL;
    int total = 0;
    int i = 0;
    int j = 0;
    int k = 0;

    LOGDEBUG("invoked userId=%s correlationId=%s epoch=%d services[0]{.name=%s .type=%s .uris[0]=%s}\n",
             SP(pMeta->userId), SP(pMeta->correlationId), pMeta->epoch, SP(pMeta->services[0].name), SP(pMeta->services[0].type), SP(pMeta->services[0].uris[0]));

    *outInstsLen = 0;
    *outInsts = NULL;

    sem_p(inst_copy_sem);
    if (instIdsLen == 0)               // describe all instances
        total = total_instances(&global_instances_copy);
    else
        total = instIdsLen;

    *outInsts = EUCA_ZALLOC(total, sizeof(ncInstance *));
    if ((*outInsts) == NULL) {
        sem_v(inst_copy_sem);
        return EUCA_MEMORY_ERROR;
    }

    k = 0;
    for (i = 0; (instance = get_instance(&global_instances_copy)) != NULL; i++) {
        // only pick ones the user (or admin) is allowed to see
        if (strcmp(pMeta->userId, nc->admin_user_id)
            && strcmp(pMeta->userId, instance->userId))
            continue;

        if (instIdsLen > 0) {
            for (j = 0; j < instIdsLen; j++)
                if (!strcmp(instance->instanceId, instIds[j]))
                    break;

            if (j >= instIdsLen)
                // instance of no relevance right now
                continue;
        }
        // (* outInsts)[k++] = instance;
        tmp = (ncInstance *) EUCA_ALLOC(1, sizeof(ncInstance));
        memcpy(tmp, instance, sizeof(ncInstance));
        (*outInsts)[k++] = tmp;
    }
    *outInstsLen = k;
    sem_v(inst_copy_sem);

    return EUCA_OK;
}

//!
//! Describe the resources status for this component
//!
//! @param[in]  nc a pointer to the NC state structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  resourceType UNUSED
//! @param[out] outRes a list of resources we retrieved data for
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_OVERFLOW_ERROR,
//!         EUCA_MEMORY_ERROR and EUCA_ERROR.
//!
static int doDescribeResource(struct nc_state_t *nc, ncMetadata * pMeta, char *resourceType, ncResource ** outRes)
{
    ncResource *res = NULL;
    ncInstance *inst = NULL;

    // stats to re-calculate now
    long long mem_free = 0;
    long long disk_free = 0;
    int cores_free = 0;

    // intermediate sums
    long long sum_mem = 0;             // for known domains: sum of requested memory
    long long sum_disk = 0;            // for known domains: sum of requested disk sizes
    int sum_cores = 0;                 // for known domains: sum of requested cores

    *outRes = NULL;

    sem_p(inst_copy_sem);
    while ((inst = get_instance(&global_instances_copy)) != NULL) {
        if (inst->state == TEARDOWN)
            continue;                  // they don't take up resources
        sum_mem += inst->params.mem;
        sum_disk += (inst->params.disk);
        sum_cores += inst->params.cores;
    }
    sem_v(inst_copy_sem);

    disk_free = nc->disk_max - sum_disk;
    if (disk_free < 0)
        disk_free = 0;                 // should not happen

    cores_free = nc->cores_max - sum_cores; //! @todo should we -1 for dom0?
    if (cores_free < 0)
        cores_free = 0;                // due to timesharing

    mem_free = nc->mem_max - sum_mem;
    if (mem_free < 0)
        mem_free = 0;                  // should not happen

    // check for potential overflow - should not happen
    if (nc->mem_max > INT_MAX || mem_free > INT_MAX || nc->disk_max > INT_MAX || disk_free > INT_MAX) {
        LOGERROR("stats integer overflow error (bump up the units?)\n");
        LOGERROR("   memory: max=%-10lld free=%-10lld\n", nc->mem_max, mem_free);
        LOGERROR("     disk: max=%-10lld free=%-10lld\n", nc->disk_max, disk_free);
        LOGERROR("    cores: max=%-10lld free=%-10d\n", nc->cores_max, cores_free);
        LOGERROR("       INT_MAX=%-10d\n", INT_MAX);
        return EUCA_OVERFLOW_ERROR;
    }
    res = allocate_resource(nc->is_enabled ? "enabled" : "disabled",
                            nc->migration_capable, nc->iqn, nc->mem_max, mem_free, nc->disk_max, disk_free, nc->cores_max, cores_free, "none");
    if (res == NULL) {
        LOGERROR("out of memory\n");
        return EUCA_MEMORY_ERROR;
    }
    *outRes = res;

    LOGDEBUG("Core status:   in-use %d physical %lld over-committed %s\n", sum_cores, nc->phy_max_cores, (((sum_cores - cores_free) > nc->phy_max_cores) ? "yes" : "no"));
    LOGDEBUG("Memory status: in-use %lld physical %lld over-committed %s\n", sum_mem, nc->phy_max_mem, (((sum_mem - mem_free) > nc->phy_max_mem) ? "yes" : "no"));
    LOGDEBUG("returning status=%s cores=%d/%d mem=%d/%d disk=%d/%d iqn=%s\n",
             res->nodeStatus, res->numberOfCoresAvailable, res->numberOfCoresMax, res->memorySizeAvailable, res->memorySizeMax, res->diskSizeAvailable, res->diskSizeMax, res->iqn);
    return EUCA_OK;
}

//!
//! Accepts a broadcast of global network info
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] networkInfo is a string
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_INVALID_ERROR.
//!
static int doBroadcastNetworkInfo(struct nc_state_t *nc, ncMetadata * pMeta, char *networkInfo)
{
    char *xmlbuf = NULL, xmlpath[EUCA_MAX_PATH];
    int ret = EUCA_OK, rc = 0;

    if (networkInfo == NULL) {
        LOGERROR("internal error (bad input parameters to doBroadcastNetworkInfo)\n");
        return (EUCA_INVALID_ERROR);
    }

    LOGTRACE("encoded networkInfo=%s\n", networkInfo);
    snprintf(xmlpath, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/global_network_info.xml", nc->home);
    LOGDEBUG("decoding/writing buffer to (%s)\n", xmlpath);
    xmlbuf = base64_dec((unsigned char *)networkInfo, strlen(networkInfo));
    if (xmlbuf) {
        rc = str2file(xmlbuf, xmlpath, O_CREAT | O_TRUNC | O_WRONLY, 0600, FALSE);
        if (rc) {
            LOGERROR("could not write XML data to file (%s)\n", xmlpath);
            ret = EUCA_ERROR;
        }
        EUCA_FREE(xmlbuf);
    } else {
        LOGERROR("could not b64 decode input buffer\n");
        ret = EUCA_ERROR;
    }

    return (ret);
}

//!
//! Assigns a public IP address to an instance
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] publicIp a string representation of the public IP to assign to the instance
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_INVALID_ERROR.
//!
static int doAssignAddress(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *publicIp)
{
    ncInstance *instance = NULL;

    if (instanceId == NULL || publicIp == NULL) {
        LOGERROR("[%s] internal error (bad input parameters to doAssignAddress)\n", instanceId);
        return (EUCA_INVALID_ERROR);
    }

    sem_p(inst_sem);
    {
        if ((instance = find_instance(&global_instances, instanceId)) != NULL) {
            snprintf(instance->ncnet.publicIp, IP_BUFFER_SIZE, "%s", publicIp);
            save_instance_struct(instance);
        }
        copy_instances();
    }
    sem_v(inst_sem);

    return EUCA_OK;
}

//!
//! Powers down this component to save some $$$.
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return Always return EUCA_OK.
//!
static int doPowerDown(struct nc_state_t *nc, ncMetadata * pMeta)
{
    int rc = 0;

    LOGDEBUG("saving power: %s /usr/sbin/powernap-now\n", nc->rootwrap_cmd_path);
    if ((rc = euca_execlp(NULL, nc->rootwrap_cmd_path, "/usr/sbin/powernap-now", NULL)) != EUCA_OK)
        LOGERROR("cmd '%s /usr/sbin/powernap-now' failed: %d\n", nc->rootwrap_cmd_path, rc);
    return (EUCA_OK);
}

//!
//! Starts the network process.
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] uuid a string containing the user unique identifier (UNUSED)
//! @param[in] remoteHosts the list of remote hosts (UNUSED)
//! @param[in] remoteHostsLen the number of hosts in the remoteHosts list (UNUSED)
//! @param[in] port the port number to use for the network (UNUSED)
//! @param[in] vlan the network vlan to use.
//!
//! @return the error codes from vnetStartNetwork()
//!
//! @see vnetStartNetwork()
//!
static int doStartNetwork(struct nc_state_t *nc, ncMetadata * pMeta, char *uuid, char **remoteHosts, int remoteHostsLen, int port, int vlan)
{
    int rc = 0;
    int ret = EUCA_ERROR;
    char *brname = NULL;

    rc = vnetStartNetwork(nc->vnetconfig, vlan, NULL, NULL, NULL, &brname);
    if (rc) {
        ret = EUCA_ERROR;
        LOGERROR("failed to start network (port=%d vlan=%d return=%d)\n", port, vlan, rc);
    } else {
        ret = EUCA_OK;
        LOGINFO("started network (port=%d vlan=%d)\n", port, vlan);
    }

    // Regardless of the error code, we should always check and free brname
    EUCA_FREE(brname);
    return (ret);
}

//!
//! because libvirt detach has a bug in some version (which one?) this helper detaches
//! volumes using 'virsh', bypassing libvirt
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] localDevReal the target device
//! @param[in] xml the XML file path this will save to.
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR.
//!
static int xen_detach_helper(struct nc_state_t *nc, char *instanceId, char *localDevReal, char *xml)
{
    int fd = 0;
    int rc = -1;
    int status = 0;
    int err = EUCA_ERROR;
    char devReal[32] = "";
    char *tmp = NULL;
    char tmpfile[EUCA_MAX_PATH] = "";
    pid_t pid = 0;

    if ((pid = fork()) == 0) {
        snprintf(tmpfile, 32, "/tmp/detachxml.XXXXXX");
        fd = safe_mkstemp(tmpfile);

        if ((tmp = strstr(xml, "<target")) == NULL) {
            LOGERROR("[%s] '<target' not found in the device xml\n", instanceId);
            return (-1);
        }

        if ((tmp = strstr(tmp, "dev=\"")) == NULL) {
            LOGERROR("[%s] '<target dev' not found in the device xml\n", instanceId);
            return (-1);
        }

        snprintf(devReal, 32, "%s", tmp + strlen("dev=\""));
        for (int i = 0; i < 32; i++) {
            if (devReal[i] == '\"') {
                for (; i < 32; i++)
                    devReal[i] = '\0';
            }
        }

        if (fd > 0) {
            if (write(fd, xml, strlen(xml)) != strlen(xml)) {
                LOGERROR("[%s] fail to write %ld bytes in temp file.\n", instanceId, strlen(xml));
            }
            close(fd);

            LOGDEBUG("[%s] executing '%s %s `which virsh` %s %s %s'\n", instanceId, nc->detach_cmd_path, nc->rootwrap_cmd_path, instanceId, devReal, tmpfile);
            if ((rc = euca_execlp(NULL, nc->detach_cmd_path, nc->rootwrap_cmd_path, "`which virsh`", instanceId, devReal, tmpfile, NULL)) != EUCA_OK)
                LOGERROR("[%s] cmd '%s %s `which virsh` %s %s %s' failed %d\n", instanceId, nc->detach_cmd_path, nc->rootwrap_cmd_path, instanceId, devReal, tmpfile, rc);

            unlink(tmpfile);
        } else {
            LOGERROR("[%s] could not write to tmpfile for detach XML: %s\n", instanceId, tmpfile);
            rc = 1;
        }
        exit(rc);
    }
    // parent or failed to fork
    rc = timewait(pid, &status, 15);
    if (WEXITSTATUS(status)) {
        LOGERROR("[%s] failed to sucessfully run detach helper\n", instanceId);
        err = EUCA_ERROR;
    } else {
        err = EUCA_OK;
    }
    return (err);
}

//!
//! Attach a given volume to an instance.
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] attachmentToken the token string for the attachment target
//! @param[in] localDev the local device name
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         EUCA_NOT_FOUND_ERROR and EUCA_HYPERVISOR_ERROR.
//!
//! @see convert_dev_names()
//!
static int doAttachVolume(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev)
{
    int ret = EUCA_OK;
    int have_remote_device = 0;
    char *xml = NULL;
    char *tagBuf = NULL;
    char *localDevName = NULL;
    char *remoteDevStr = NULL;
    char localDevReal[32] = "";
    char localDevTag[256] = "";
    char remoteDevReal[132] = "";
    char scUrl[512] = "";              //Tmp holder for sc url for sc call.
    char path[EUCA_MAX_PATH] = "";
    char lpath[EUCA_MAX_PATH] = "";
    ncVolume *volume = NULL;
    ebs_volume_data *vol_data = NULL;

    if (!strcmp(nc->H->name, "xen")) {
        tagBuf = NULL;
        localDevName = localDevReal;
    } else if (!strcmp(nc->H->name, "kvm") || !strcmp(nc->H->name, "qemu")) {
        tagBuf = localDevTag;
        localDevName = localDevTag;
    } else {
        LOGERROR("[%s][%s] unknown hypervisor type '%s'\n", instanceId, volumeId, nc->H->name);
        return EUCA_ERROR;
    }

    // sets localDevReal to the file name from the device path
    // and, for KVM, sets localDevTag to the "unknown" string
    ret = convert_dev_names(localDev, localDevReal, tagBuf);
    if (ret)
        return ret;

    // find the instance record
    sem_p(inst_sem);
    ncInstance *instance = find_instance(&global_instances, instanceId);
    sem_v(inst_sem);
    if (instance == NULL) {
        return EUCA_NOT_FOUND_ERROR;
    }

    {                                  // connect to hypervisor and query it
        virConnectPtr conn = lock_hypervisor_conn();
        if (conn == NULL) {
            LOGERROR("[%s][%s] cannot get connection to hypervisor\n", instanceId, volumeId);
            return EUCA_HYPERVISOR_ERROR;
        }
        // find domain on hypervisor
        virDomainPtr dom = virDomainLookupByName(conn, instanceId);
        if (dom == NULL) {
            if (instance->state != BOOTING && instance->state != STAGING) {
                LOGWARN("[%s][%s] domain not running on hypervisor, cannot attach device\n", instanceId, volumeId);
            }
            unlock_hypervisor_conn();
            return EUCA_HYPERVISOR_ERROR;
        }
        virDomainFree(dom);            // release libvirt resource
        unlock_hypervisor_conn();
    }

    // mark volume as 'attaching', save token value as we got from the wire
    sem_p(inst_sem);
    {
        volume = save_volume(instance, volumeId, attachmentToken, NULL, localDevName, localDevReal, VOL_STATE_ATTACHING);
        save_instance_struct(instance);
        copy_instances();
    }
    sem_v(inst_sem);

    if (!volume) {
        LOGERROR("[%s][%s] failed to update the volume record, aborting volume attachment\n", instanceId, volumeId);
        return EUCA_ERROR;
    }
    //Do the ebs connect.
    LOGTRACE("[%s][%s] Connecting EBS volume to local host\n", instanceId, volumeId);
    if (get_service_url("storage", nc, scUrl) != EUCA_OK || strlen(scUrl) == 0) {
        LOGERROR("[%s][%s] Failed to lookup enabled Storage Controller. Cannot attach volume: %s\n", instanceId, volumeId, volumeId);
        have_remote_device = 0;
        ret = EUCA_ERROR;
        goto release;
    } else {
        LOGTRACE("[%s][%s] Using SC URL: %s\n", instanceId, volumeId, scUrl);
    }

    if (connect_ebs_volume(scUrl, attachmentToken, nc->config_use_ws_sec, nc->config_sc_policy_file, nc->ip, nc->iqn, &remoteDevStr, &vol_data) != EUCA_OK) {
        LOGERROR("Error connecting ebs volume %s\n", attachmentToken);
        have_remote_device = 0;
        ret = EUCA_ERROR;
        goto release;
    }

    if (!remoteDevStr || !strstr(remoteDevStr, "/dev")) {
        LOGERROR("[%s][%s] failed to connect to iscsi target\n", instanceId, volumeId);
        remoteDevReal[0] = '\0';
        have_remote_device = 0;
        ret = EUCA_ERROR;
        goto release;
    } else {
        LOGDEBUG("[%s][%s] attached iSCSI target of host device '%s'\n", instanceId, volumeId, remoteDevStr);
        snprintf(remoteDevReal, sizeof(remoteDevReal), "%s", remoteDevStr);
        have_remote_device = 1;
    }

    // Update volume with new connection info
    sem_p(inst_sem);
    {
        volume = save_volume(instance, volumeId, attachmentToken, vol_data->connect_string, localDevName, localDevReal, VOL_STATE_ATTACHING);
        save_instance_struct(instance);
        copy_instances();
    }
    sem_v(inst_sem);

    if (!volume) {
        LOGERROR("[%s][%s] failed to update the volume record, aborting volume attachment\n", instanceId, volumeId);
        ret = EUCA_ERROR;
        goto release;
    }
    // make sure there is a block device
    if (check_block(remoteDevReal)) {
        LOGERROR("[%s][%s] cannot verify that host device '%s' is available for hypervisor attach\n", instanceId, volumeId, remoteDevReal);
        ret = EUCA_ERROR;
        goto release;
    }
    // generate XML for libvirt attachment request
    if (gen_volume_xml(volumeId, instance, localDevReal, remoteDevReal) // creates vol-XXX.xml
        || gen_libvirt_volume_xml(volumeId, instance)) {    // creates vol-XXX-libvirt.xml via XSLT transform
        LOGERROR("[%s][%s] could not produce attach device xml\n", instanceId, volumeId);
        ret = EUCA_ERROR;
        goto release;
    }
    snprintf(path, (sizeof(path) - 1), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volumeId);    // vol-XXX.xml
    path[sizeof(path) - 1] = '\0';

    snprintf(lpath, (sizeof(lpath) - 1), EUCALYPTUS_VOLUME_LIBVIRT_XML_PATH_FORMAT, instance->instancePath, volumeId);  // vol-XXX-libvirt.xml
    lpath[sizeof(lpath) - 1] = '\0';

    // invoke hooks
    if (call_hooks(NC_EVENT_PRE_ATTACH, lpath)) {
        LOGERROR("[%s][%s] cancelled volume attachment via hooks\n", instance->instanceId, volumeId);
        ret = EUCA_ERROR;
        goto release;
    }
    // read in libvirt XML, which may have been modified by the hook above
    if ((xml = file2str(lpath)) == NULL) {
        LOGERROR("[%s][%s] failed to read volume XML from %s\n", instance->instanceId, volumeId, lpath);
        ret = EUCA_ERROR;
        goto release;
    }

    {                                  // connect to hypervisor, find the domain, attach the volume
        virConnectPtr conn = lock_hypervisor_conn();
        if (conn == NULL) {
            LOGERROR("[%s][%s] cannot get connection to hypervisor\n", instanceId, volumeId);
            return EUCA_HYPERVISOR_ERROR;
        }
        // find domain on hypervisor
        virDomainPtr dom = virDomainLookupByName(conn, instanceId);
        if (dom == NULL) {
            if (instance->state != BOOTING && instance->state != STAGING) {
                LOGWARN("[%s][%s] domain not running on hypervisor, cannot attach device\n", instanceId, volumeId);
            }
            unlock_hypervisor_conn();
            goto release;
        }

        int err = 0;
        for (int i = 1; i <= VOL_RETRIES; i++) {
            err = virDomainAttachDevice(dom, xml);
            if (err) {
                LOGERROR("[%s][%s] failed to attach host device '%s' to guest device '%s' on attempt %d of 3\n", instanceId, volumeId, remoteDevReal, localDevReal, i);
                LOGDEBUG("[%s][%s] virDomainAttachDevice() failed (err=%d) XML='%s'\n", instanceId, volumeId, err, xml);
                sleep(3);              // sleep a bit and retry.
            } else {
                break;
            }
        }

        if (err) {
            LOGERROR("[%s][%s] failed to attach host device '%s' to guest device '%s' after %d tries\n", instanceId, volumeId, remoteDevReal, localDevReal, VOL_RETRIES);
            LOGDEBUG("[%s][%s] virDomainAttachDevice() failed (err=%d) XML='%s'\n", instanceId, volumeId, err, xml);
            ret = EUCA_ERROR;
        }

        virDomainFree(dom);            // release libvirt resource
        unlock_hypervisor_conn();
    }

release:

    {                                  // record volume state in memory and on disk
        char *next_vol_state;
        if (ret == EUCA_OK) {
            next_vol_state = VOL_STATE_ATTACHED;
        } else {
            next_vol_state = VOL_STATE_ATTACHING_FAILED;
        }

        sem_p(inst_sem);
        {
            volume = save_volume(instance, volumeId, NULL, NULL, NULL, NULL, next_vol_state);   // now we can record remoteDevReal
            save_instance_struct(instance);
            copy_instances();
            update_disk_aliases(instance);  // ask sensor subsystem to track the volume
        }
        sem_v(inst_sem);
    }

    if ((volume == NULL) && (xml != NULL)) {
        LOGERROR("[%s][%s] failed to save the volume record, aborting volume attachment (detaching)\n", instanceId, volumeId);

        // connect to hypervisor, find the domain, detach the volume
        virConnectPtr conn = lock_hypervisor_conn();
        if (conn == NULL) {
            LOGERROR("[%s][%s] cannot get connection to hypervisor\n", instanceId, volumeId);
            ret = EUCA_HYPERVISOR_ERROR;
            goto cleanup;
        }
        // find domain on hypervisor
        virDomainPtr dom = virDomainLookupByName(conn, instanceId);
        int err = 0;
        if (dom != NULL) {
            err = virDomainDetachDevice(dom, xml);
            virDomainFree(dom);        // release libvirt resource
        }
        if (err) {
            LOGERROR("[%s][%s] failed to detach as part of aborting\n", instanceId, volumeId);
            LOGDEBUG("[%s][%s] virDomainDetachDevice() failed (err=%d) XML='%s'\n", instanceId, volumeId, err, xml);
        }
        unlock_hypervisor_conn();
        ret = EUCA_ERROR;
    }
    // if iSCSI and there were problems, try to disconnect the target
    if (ret != EUCA_OK && have_remote_device) {
        LOGDEBUG("[%s][%s] attempting to disconnect iscsi target due to attachment failure\n", instanceId, volumeId);
        if (vol_data != NULL && vol_data->connect_string[0] != '\0') {
            if (disconnect_ebs_volume(scUrl, nc->config_use_ws_sec, nc->config_sc_policy_file, attachmentToken, vol_data->connect_string, nc->ip, nc->iqn) != EUCA_OK) {
                LOGERROR("[%s][%s] Error disconnecting ebs volume on error rollback.\n", instanceId, volumeId);
            }
        }
    }

    if (ret == EUCA_OK) {
        LOGINFO("[%s][%s] volume attached as host device '%s' to guest device '%s'\n", instanceId, volumeId, remoteDevReal, localDevReal);
    }
    // remoteDev can be a long string, so to keep log readable, we log it at TRACE level unless there was a problem
    int log_level_for_devstring = EUCA_LOG_TRACE;
    if (ret != EUCA_OK)
        log_level_for_devstring = EUCA_LOG_DEBUG;
    EUCALOG(log_level_for_devstring, "[%s][%s] remote device string: %s\n", instanceId, volumeId, remoteDevStr);

cleanup:
    EUCA_FREE(vol_data);
    EUCA_FREE(remoteDevStr);
    EUCA_FREE(xml);
    return ret;
}

//!
//! Detach a given volume from an instance.
//! First, removes the volume from the libvirt domain.
//!
//! Uses the received token string and the local NC volume state struct.
//! Looks up the volume attachment info locally then does a local disconnect.
//! If successful, then calls the SC to unexport the volume from this host using the token.
//! Then does another check to ensure that the device is removed.
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] attachmentToken the target device name
//! @param[in] localDev the local device name
//! @param[in] force if set to 1, this will force the volume to detach
//! @param[in] grab_inst_sem if set to 1, will require the usage of the instance semaphore
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         EUCA_NOT_FOUND_ERROR and EUCA_HYPERVISOR_ERROR.
//!
//! @see convert_dev_names()
//!
static int doDetachVolume(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev, int force, int grab_inst_sem)
{
    int ret = EUCA_OK;
    int have_remote_device = 0;
    char *xml = NULL;
    char resourceName[1][MAX_SENSOR_NAME_LEN] = { {0} };
    char resourceAlias[1][MAX_SENSOR_NAME_LEN] = { {0} };
    char volpath[EUCA_MAX_PATH];
    char lvolpath[EUCA_MAX_PATH];
    char *tagBuf;
    char *localDevName;
    char localDevReal[32], localDevTag[256], remoteDevReal[132];
    char scUrl[512];
    char *connectionString = NULL;
    instance_states lastState = NO_STATE;
    char *remoteDevStr = NULL;
    ncVolume *volume = NULL;

    if (!strcmp(nc->H->name, "xen")) {
        tagBuf = NULL;
        localDevName = localDevReal;
    } else if (!strcmp(nc->H->name, "kvm") || !strcmp(nc->H->name, "qemu")) {
        tagBuf = localDevTag;
        localDevName = localDevTag;
    } else {
        LOGERROR("[%s][%s] unknown hypervisor type '%s'\n", instanceId, volumeId, nc->H->name);
        return EUCA_ERROR;
    }

    // get the file name from the device path and, for KVM, the "unknown" string
    ret = convert_dev_names(localDev, localDevReal, tagBuf);
    if (ret)
        return ret;

    {                                  // find the instance record and work with it
        if (grab_inst_sem)
            sem_p(inst_sem);

        ncInstance *instance = find_instance(&global_instances, instanceId);
        if (instance == NULL) {
            LOGERROR("[%s][%s] failed to find instance for volume detachment\n", instanceId, volumeId);
            if (grab_inst_sem)
                sem_v(inst_sem);
            return EUCA_NOT_FOUND_ERROR;
        }
        // set up paths for later
        snprintf(volpath, sizeof(volpath), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volumeId);    // vol-XXX.xml
        snprintf(lvolpath, sizeof(lvolpath), EUCALYPTUS_VOLUME_LIBVIRT_XML_PATH_FORMAT, instance->instancePath, volumeId);  // vol-XXX-libvirt.xml

        // save current state on the stack
        lastState = instance->state;

        // mark volume as 'detaching', do not over-write the attachment token used for
        volume = save_volume(instance, volumeId, NULL, NULL, localDevName, localDevReal, VOL_STATE_DETACHING);
        if (!volume) {
            LOGERROR("[%s][%s] failed to update the volume record, aborting volume detachment\n", instanceId, volumeId);
            if (grab_inst_sem)
                sem_v(inst_sem);
            return EUCA_ERROR;
        }
        save_instance_struct(instance);
        copy_instances();

        // lookup the volume info locally for detachment
        if (volume->connectionString[0] == '\0' || volume->attachmentToken[0] == '\0') {
            LOGERROR("[%s][%s] failed to find the local volume attachment record, aborting volume detachment\n", instanceId, volumeId);
            if (grab_inst_sem)
                sem_v(inst_sem);
            return EUCA_ERROR;
        }
        // do iscsi connect shellout if remoteDev is an iSCSI target
        // get credentials, decrypt them
        // (used to have check if iscsi here, not necessary with AOE deprecation.)
        remoteDevStr = get_volume_local_device(volume->connectionString);
        if (!remoteDevStr || !strstr(remoteDevStr, "/dev")) {
            LOGERROR("[%s][%s] failed to get local name of host iscsi device\n", instanceId, volumeId);
            remoteDevReal[0] = '\0';
        } else {
            snprintf(remoteDevReal, sizeof(remoteDevReal), "%s", remoteDevStr);
            have_remote_device = 1;
        }
        EUCA_FREE(remoteDevStr);

        if (grab_inst_sem)
            sem_v(inst_sem);
    }

    // something went wrong above, abort
    if (!have_remote_device) {
        ret = EUCA_ERROR;
        goto disconnect;
    }
    // make sure there is a block device
    if (check_block(remoteDevReal)) {
        LOGERROR("[%s][%s] cannot verify that host device '%s' is available for hypervisor detach\n", instanceId, volumeId, remoteDevReal);
        if (!force)
            ret = EUCA_ERROR;
        goto disconnect;
    }
    // read in libvirt XML
    xml = file2str(lvolpath);
    if (xml == NULL) {
        LOGERROR("[%s][%s] failed to read volume XML from %s\n", instanceId, volumeId, lvolpath);
        ret = EUCA_ERROR;
        goto disconnect;
    }

    {                                  // connect to hypervisor and do
        virConnectPtr conn = lock_hypervisor_conn();
        if (conn == NULL) {
            LOGERROR("[%s][%s] cannot get connection to hypervisor\n", instanceId, volumeId);
            ret = EUCA_HYPERVISOR_ERROR;
            goto disconnect;
        }
        // refresh stats so volume measurements are captured before it disappears
        euca_strncpy(resourceName[0], instanceId, MAX_SENSOR_NAME_LEN);
        sensor_refresh_resources(resourceName, resourceAlias, 1);

        // find domain on hypervisor
        virDomainPtr dom = virDomainLookupByName(conn, instanceId);
        if (dom == NULL) {
            if (lastState != BOOTING && lastState != STAGING) {
                LOGWARN("[%s][%s] domain not running on hypervisor, will not try to detach device\n", instanceId, volumeId);
            }
            unlock_hypervisor_conn();
            goto disconnect;
        }
        // protect libvirt calls because we've seen problems during concurrent libvirt invocation
        int err = virDomainDetachDevice(dom, xml);
        if (!strcmp(nc->H->name, "xen")) {
            err = xen_detach_helper(nc, instanceId, localDevReal, xml);
        }

        if (err) {
            LOGERROR("[%s][%s] failed to detach host device '%s' from guest device '%s'\n", instanceId, volumeId, remoteDevReal, localDevReal);
            LOGERROR("[%s][%s] virDomainDetachDevice() or 'virsh detach' failed (err=%d) XML='%s'\n", instanceId, volumeId, err, xml);
            if (!force)
                ret = EUCA_HYPERVISOR_ERROR;
        } else {
            call_hooks(NC_EVENT_POST_DETACH, volpath);  // invoke hooks, but do not do anything if they return error
            unlink(lvolpath);          // remove vol-XXX-libvirt.xml
            unlink(volpath);           // remove vol-XXXX.xml file
        }

        virDomainFree(dom);            // release libvirt resource
        unlock_hypervisor_conn();      // unlock the connection to the hypervisor
    }

disconnect:

    {                                  // update the instance structure while under a lock
        if (grab_inst_sem)
            sem_p(inst_sem);

        ncInstance *instance = find_instance(&global_instances, instanceId);
        if (instance == NULL) {
            LOGWARN("[%s][%s] failed to find the instance to update volume state\n", instanceId, volumeId);
        } else {
            // record volume state in memory and on disk
            char *next_vol_state;
            if (ret == EUCA_OK) {
                next_vol_state = VOL_STATE_DETACHED;
            } else {
                next_vol_state = VOL_STATE_DETACHING_FAILED;
            }
            ncVolume *volume = save_volume(instance, volumeId, NULL, NULL, NULL, NULL, next_vol_state);
            if (volume == NULL) {
                LOGWARN("[%s][%s] failed to save the volume record\n", instanceId, volumeId);
                ret = EUCA_ERROR;
            } else {
                connectionString = strdup(volume->connectionString);
            }
            save_instance_struct(instance);
            copy_instances();
            update_disk_aliases(instance);  // ask sensor subsystem to stop tracking the volume
        }

        if (grab_inst_sem)
            sem_v(inst_sem);
    }

    // if iSCSI, try to disconnect the target
    if (have_remote_device) {
        //Do the ebs disconnect.
        LOGTRACE("[%s][%s] Disconnecting EBS volume to local host\n", instanceId, volumeId);
        if (get_service_url("storage", nc, scUrl) != EUCA_OK || strlen(scUrl) == 0) {
            LOGWARN("[%s][%s] could not obtain SC URL (is SC enabled?)\n", instanceId, volumeId);
            scUrl[0] = '\0';
        }
        LOGTRACE("[%s][%s] Using SC Url: %s\n", instanceId, volumeId, scUrl);
        //Use the volume attachment token from the initial attachment instead of the one that came over the wire. This ensures parity between attach/detach.
        if (disconnect_ebs_volume(scUrl, nc->config_use_ws_sec, nc->config_sc_policy_file, volume->attachmentToken, connectionString, nc->ip, nc->iqn) != EUCA_OK) {
            LOGERROR("[%s][%s] failed to disconnect volume\n", instanceId, volumeId);
            if (!force)
                ret = EUCA_ERROR;

        }
    }

    if (ret == EUCA_OK)
        LOGINFO("[%s][%s] detached as host device '%s' and guest device '%s'\n", instanceId, volumeId, remoteDevReal, localDevReal);
    // remoteDev can be a long string, so to keep log readable, we log it at TRACE level unless there was a problem
    int log_level_for_devstring = EUCA_LOG_TRACE;
    if (ret != EUCA_OK)
        log_level_for_devstring = EUCA_LOG_DEBUG;
    EUCALOG(log_level_for_devstring, "[%s][%s] remote device string: %s\n", instanceId, volumeId, connectionString);
    EUCA_FREE(connectionString);
    EUCA_FREE(xml);

    if (force) {
        return (EUCA_OK);
    }
    return ret;
}

//!
//! helper for changing bundling task state and stateName together
//!
//! @param[in] instance a pointer to the instance we are changing the state for
//! @param[in] state the new state
//!
static void change_createImage_state(ncInstance * instance, createImage_progress state)
{
    instance->createImageTaskState = state;
    euca_strncpy(instance->createImageTaskStateName, createImage_progress_names[state], CHAR_BUFFER_SIZE);
}

//!
//! helper for cleaning up
//!
//! @param[in] instance a pointer ot the instance to cleanup
//! @param[in] params a pointer to the create image parameters
//! @param[in] state the new instance to set
//! @param[in] result the result of the create image processing
//!
//! @return EUCA_OK on success otherwise EUCA_ERROR is returned.
//!
static int cleanup_createImage_task(ncInstance * instance, struct createImage_params_t *params, instance_states state, createImage_progress result)
{
    LOGINFO("[%s] createImage task result=%s\n", instance->instanceId, createImage_progress_names[result]);
    sem_p(inst_sem);
    {
        change_createImage_state(instance, result);
        if (state != NO_STATE)         // do not touch instance state (these are early failures, before we destroyed the domain)
            change_state(instance, state);
        copy_instances();
    }
    sem_v(inst_sem);

    if (params) {
        // if the result was failed or cancelled, clean up object storage state
        if (result == CREATEIMAGE_FAILED || result == CREATEIMAGE_CANCELLED) {
        }
        EUCA_FREE(params->workPath);
        EUCA_FREE(params->volumeId);
        EUCA_FREE(params->remoteDev);
        EUCA_FREE(params->diskPath);
        EUCA_FREE(params->eucalyptusHomePath);
        EUCA_FREE(params);
    }

    return (result == CREATEIMAGE_SUCCESS) ? EUCA_OK : EUCA_ERROR;
}

//!
//! Defines the create image thread.
//!
//! @param[in] arg a transparent pointer to the argument passed to this thread handler
//!
//! @return Always return NULL
//!
static void *createImage_thread(void *arg)
{
    struct createImage_params_t *params = (struct createImage_params_t *)arg;
    ncInstance *instance = params->instance;
    int rc;

    LOGDEBUG("[%s] spawning create-image thread\n", instance->instanceId);
    LOGINFO("[%s] waiting for instance to shut down\n", instance->instanceId);
    // wait until monitor thread changes the state of the instance instance
    if (wait_state_transition(instance, CREATEIMAGE_SHUTDOWN, CREATEIMAGE_SHUTOFF)) {
        if (instance->createImageCanceled) {    // cancel request came in while the instance was shutting down
            LOGINFO("[%s] cancelled while createImage for instance\n", instance->instanceId);
            cleanup_createImage_task(instance, params, SHUTOFF, CREATEIMAGE_CANCELLED);
        } else {
            LOGINFO("[%s] failed while createImage for instance\n", instance->instanceId);
            cleanup_createImage_task(instance, params, SHUTOFF, CREATEIMAGE_FAILED);
        }
        return NULL;
    }

    LOGINFO("[%s] started createImage for instance\n", instance->instanceId);
    {
        rc = 0;
        if (rc == 0) {
            cleanup_createImage_task(instance, params, SHUTOFF, CREATEIMAGE_SUCCESS);
            LOGINFO("[%s] finished createImage for instance\n", instance->instanceId);
        } else if (rc == -1) {
            // bundler child was cancelled (killed)
            cleanup_createImage_task(instance, params, SHUTOFF, CREATEIMAGE_CANCELLED);
            LOGINFO("[%s] cancelled while createImage for instance (rc=%d)\n", instance->instanceId, rc);
        } else {
            cleanup_createImage_task(instance, params, SHUTOFF, CREATEIMAGE_FAILED);
            LOGINFO("[%s] failed while createImage for instance (rc=%d)\n", instance->instanceId, rc);
        }
    }

    return NULL;
}

//!
//! Handles the image creation request.
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the remote device name
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         EUCA_NOT_FOUND_ERROR. Error code from cleanup_createImage_task() and find_and_terminate_instance()
//!         are also returned.
//!
//! @see cleanup_createImage_task()
//! @see find_and_terminate_instance()
//!
static int doCreateImage(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev)
{
    // sanity checking
    if (instanceId == NULL || remoteDev == NULL || volumeId == NULL) {
        LOGERROR("[%s][%s] called with invalid parameters\n", ((instanceId == NULL) ? "UNKNOWN" : instanceId), ((volumeId == NULL) ? "UNKNOWN" : volumeId));
        return EUCA_ERROR;
    }
    // find the instance
    ncInstance *instance = find_instance(&global_instances, instanceId);
    if (instance == NULL) {
        LOGERROR("[%s][%s] instance not found\n", instanceId, volumeId);
        return EUCA_NOT_FOUND_ERROR;
    }
    // "marshall" thread parameters
    struct createImage_params_t *params = EUCA_ZALLOC(1, sizeof(struct createImage_params_t));
    if (params == NULL)
        return cleanup_createImage_task(instance, params, NO_STATE, CREATEIMAGE_FAILED);

    params->instance = instance;       //! @TODO pass instanceId instead
    params->volumeId = strdup(volumeId);
    params->remoteDev = strdup(remoteDev);

    // terminate the instance
    sem_p(inst_sem);
    instance->createImageTime = time(NULL);
    change_state(instance, CREATEIMAGE_SHUTDOWN);
    change_createImage_state(instance, CREATEIMAGE_IN_PROGRESS);
    sem_v(inst_sem);

    int err = find_and_terminate_instance(instanceId);
    if (err != EUCA_OK) {
        EUCA_FREE(params);
        return err;
    }

    sem_p(inst_sem);
    copy_instances();
    sem_v(inst_sem);

    // do the rest in a thread
    pthread_attr_t tattr;
    pthread_t tid;
    pthread_attr_init(&tattr);
    pthread_attr_setdetachstate(&tattr, PTHREAD_CREATE_DETACHED);
    if (pthread_create(&tid, &tattr, createImage_thread, (void *)params) != 0) {
        LOGERROR("[%s][%s] failed to start VM createImage thread\n", instanceId, volumeId);
        return cleanup_createImage_task(instance, params, SHUTOFF, CREATEIMAGE_FAILED);
    }

    return EUCA_OK;
}

//!
//! helper for changing bundling task state and stateName together
//!
//! @param[in] instance a pointer to the instance we are changing the bundling state for
//! @param[in] state the new bundling state
//!
static void change_bundling_state(ncInstance * instance, bundling_progress state)
{
    instance->bundleTaskState = state;
    euca_strncpy(instance->bundleTaskStateName, bundling_progress_names[state], CHAR_BUFFER_SIZE);
}

//!
//! Cleans up the bundling task uppon completion or cancellation.
//!
//! @param[in] pInstance a pointer to the instance we're cleaning the bundling task for
//! @param[in] pParams a pointer to the bundling task parameters
//! @param[in] result the bundling task result
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
static int cleanup_bundling_task(ncInstance * pInstance, struct bundling_params_t *pParams, bundling_progress result)
{
    LOGINFO("[%s] bundling task result=%s\n", pInstance->instanceId, bundling_progress_names[result]);

    sem_p(inst_sem);
    {
        change_bundling_state(pInstance, result);
        copy_instances();
    }
    sem_v(inst_sem);

    if (pParams) {
        // Free our parameters allocated strings
        EUCA_FREE(pParams->workPath);
        EUCA_FREE(pParams->bucketName);
        EUCA_FREE(pParams->filePrefix);
        EUCA_FREE(pParams->objectStorageURL);
        EUCA_FREE(pParams->userPublicKey);
        EUCA_FREE(pParams->diskPath);
        EUCA_FREE(pParams->eucalyptusHomePath);
        EUCA_FREE(pParams->ncBundleUploadCmd);
        EUCA_FREE(pParams->ncCheckBucketCmd);
        EUCA_FREE(pParams->ncDeleteBundleCmd);
        EUCA_FREE(pParams);
    }

    return ((result == BUNDLING_SUCCESS) ? EUCA_OK : EUCA_ERROR);
}

//!
//! Defines the bundling thread
//!
//! @param[in] arg a transparent pointer to the argument passed to this thread handler
//!
//! @return Always return NULL
//!
static void *bundling_thread(void *arg)
{
    int rc = 0;
    int status = 0;

    struct bundling_params_t *pParams = ((struct bundling_params_t *)arg);
    ncInstance *pInstance = pParams->instance;

    LOGDEBUG("[%s] spawning bundling thread\n", pInstance->instanceId);
    LOGINFO("[%s] terminating instance\n", pInstance->instanceId);
    rc = find_and_stop_instance(pInstance->instanceId);
    sem_p(inst_sem);
    {
        copy_instances();
    }
    sem_v(inst_sem);

    // wait until monitor thread changes the state of the pInstance pInstance
    if (wait_state_transition(pInstance, BUNDLING_SHUTDOWN, BUNDLING_SHUTOFF)) {
        if (pInstance->bundleCanceled) {    // cancel request came in while the pInstance was shutting down
            LOGINFO("[%s] cancelled while bundling instance\n", pInstance->instanceId);
            cleanup_bundling_task(pInstance, pParams, BUNDLING_CANCELLED);
        } else {
            LOGINFO("[%s] failed while bundling instance\n", pInstance->instanceId);
            cleanup_bundling_task(pInstance, pParams, BUNDLING_FAILED);
        }
        return NULL;
    }
    // check if bundling was cancelled while we waited
    if (pInstance->bundleCanceled) {
        LOGINFO("[%s] bundle task canceled; terminating bundling thread\n", pInstance->instanceId);
        cleanup_bundling_task(pInstance, pParams, BUNDLING_CANCELLED);
        return NULL;
    }

    LOGINFO("[%s] starting to bundle\n", pInstance->instanceId);
    char node_pk_path[EUCA_MAX_PATH];
    snprintf(node_pk_path, sizeof(node_pk_path), EUCALYPTUS_KEYS_DIR "/node-pk.pem", pParams->eucalyptusHomePath);
    char cloud_cert_path[EUCA_MAX_PATH];
    snprintf(cloud_cert_path, sizeof(cloud_cert_path), EUCALYPTUS_KEYS_DIR "/cloud-cert.pem", pParams->eucalyptusHomePath);
    char run_workflow_path[EUCA_MAX_PATH];
    snprintf(run_workflow_path, sizeof(run_workflow_path), "%s/usr/libexec/eucalyptus/euca-run-workflow", pParams->eucalyptusHomePath);

#define _COMMON_BUNDLING_PARAMS \
                         run_workflow_path,\
                         "read-raw/up-bundle",\
                         "--input-path", pInstance->params.root->backingPath,\
                         "--encryption-cert-path", cloud_cert_path,\
                         "--signing-key-path", node_pk_path,\
                         "--prefix", pParams->filePrefix,\
                         "--bucket", pParams->bucketName,\
                         "--work-dir", "/tmp", /* @TODO: should not be needed any more*/ \
                         "--arch", "x86_64", /* @TODO: obtain arch from instance*/ \
                         "--account", "123456789012", /* @TODO: obtain account for real*/ \
                         "--access-key", pParams->userPublicKey, /* @TODO: "PublicKey" is a misnomer*/ \
                         "--object-store-url", pParams->objectStorageURL,\
                         "--policy", pParams->S3Policy,\
                         "--policy-signature", pParams->S3PolicySig,\
                         "--emi", pInstance->imageId,

    if ((pParams->kernelId != NULL) && (pParams->ramdiskId != NULL)) {
        rc = euca_execlp(&status,
                         _COMMON_BUNDLING_PARAMS
                         "--eki", pParams->kernelId,
                         "--eri", pParams->ramdiskId,
                         NULL);
    } else {
        rc = euca_execlp(&status,
                         _COMMON_BUNDLING_PARAMS
                         NULL);
    }
    if (rc == EUCA_OK) {
        cleanup_bundling_task(pInstance, pParams, BUNDLING_SUCCESS);
        LOGINFO("[%s] finished bundling instance\n", pInstance->instanceId);
    } else if (rc == EUCA_THREAD_ERROR) {
        // bundler child was cancelled (killed), but should report it as failed
        cleanup_bundling_task(pInstance, pParams, BUNDLING_FAILED);
        LOGINFO("[%s] cancelled while bundling instance (rc=%d)\n", pInstance->instanceId, rc);
    } else {
        cleanup_bundling_task(pInstance, pParams, BUNDLING_FAILED);
        LOGINFO("[%s] failed while bundling instance (rc=%d)\n", pInstance->instanceId, rc);
    }

    return NULL;
}

//!
//! Checks bucket names for invalid characters, as per AWS spec, using
//! (the more permissive) rules of the "US Standard region".
//!
//!   http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html
//!
//! @return 0 if the name is a valid S3 bucket name and 1 if not
//!
static int verify_bucket_name(const char *name)
{
    if (name == NULL)
        return 1;

    int len = strlen(name);
    if (len < 3 || len > 255)          // "Bucket names must be at least 3... Bucket names can be as long as 255 characters"
        return 1;

    for (int i = 0; i < len; i++) {
        char c = tolower(name[i]);
        if (c >= 'a' && c <= 'z')      // "any combination of uppercase letters, lowercase letters,..."
            continue;
        if (c >= '0' && c <= '9')      // "numbers,..."
            continue;
        if (c == '.' ||                // "periods,..."
            c == '-' ||                // "dashes,..."
            c == '_')                  // "and underscores"
            continue;

        // otherwise there is an invalid character
        return 1;
    }
    return 0;
}

//!
//! Handles the bundling instance request.
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] bucketName the bucket name string to which the bundle will be saved
//! @param[in] filePrefix the prefix name string of the bundle
//! @param[in] objectStorageURL the object storage URL address string
//! @param[in] userPublicKey the public key string
//! @param[in] S3Policy the S3 engine policy
//! @param[in] S3PolicySig the S3 engine policy signature
//!
//! @return EUCA_OK on success or proper error code. known error code returned include: EUCA_ERROR and
//!         EUCA_NOT_FOUND_ERROR. Error code from cleanup_bundling_task() and find_and_terminate_instance()
//!         are also returned.
//!
//! @see cleanup_bundling_task()
//! @see find_and_terminate_instance()
//!
static int doBundleInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *objectStorageURL, char *userPublicKey,
                            char *S3Policy, char *S3PolicySig)
{
    pthread_t tid = { 0 };
    pthread_attr_t tattr = { {0} };
    ncInstance *pInstance = NULL;
    struct bundling_params_t *pParams = NULL;

    // sanity checking
    if ((instanceId == NULL) || (bucketName == NULL) || (filePrefix == NULL) || (objectStorageURL == NULL) || (userPublicKey == NULL) || (S3Policy == NULL)
        || (S3PolicySig == NULL)) {
        LOGERROR("[%s] bundling instance called with invalid parameters\n", ((instanceId == NULL) ? "UNKNOWN" : instanceId));
        return EUCA_ERROR;
    }
    if (verify_bucket_name(bucketName) || verify_bucket_name(filePrefix)) {
        LOGERROR("[%s] invalid bucket name or file prefix\n", instanceId);
        return EUCA_ERROR;
    }
    // find the instance
    if ((pInstance = find_instance(&global_instances, instanceId)) == NULL) {
        LOGERROR("[%s] instance not found\n", instanceId);
        return EUCA_NOT_FOUND_ERROR;
    }
    // "marshall" thread parameters
    if ((pParams = EUCA_ZALLOC(1, sizeof(struct bundling_params_t))) == NULL)
        return cleanup_bundling_task(pInstance, pParams, BUNDLING_FAILED);

    pParams->instance = pInstance;
    pParams->bucketName = strdup(bucketName);
    pParams->filePrefix = strdup(filePrefix);
    pParams->objectStorageURL = strdup(objectStorageURL);
    pParams->userPublicKey = strdup(userPublicKey);
    pParams->S3Policy = strdup(S3Policy);
    pParams->S3PolicySig = strdup(S3PolicySig);
    pParams->eucalyptusHomePath = strdup(nc->home);
    pParams->ncBundleUploadCmd = strdup(nc->ncBundleUploadCmd);
    pParams->ncCheckBucketCmd = strdup(nc->ncCheckBucketCmd);
    pParams->ncDeleteBundleCmd = strdup(nc->ncDeleteBundleCmd);

    pParams->workPath = strdup(pInstance->instancePath);
    if (!strcmp(pInstance->platform, "linux") && (pInstance->kernelId[0] != '\0') && (pInstance->ramdiskId[0] != '\0')) {
        pParams->kernelId = strdup(pInstance->kernelId);
        pParams->ramdiskId = strdup(pInstance->ramdiskId);
    } else {
        pParams->kernelId = NULL;
        pParams->ramdiskId = NULL;
    }

    // mark instance as being bundled
    sem_p(inst_sem);
    {
        pInstance->bundlingTime = time(NULL);
        change_state(pInstance, BUNDLING_SHUTDOWN);
        change_bundling_state(pInstance, BUNDLING_IN_PROGRESS);
    }
    sem_v(inst_sem);

    // do the rest in a thread
    pthread_attr_init(&tattr);
    pthread_attr_setdetachstate(&tattr, PTHREAD_CREATE_DETACHED);
    if (pthread_create(&tid, &tattr, bundling_thread, ((void *)pParams)) != 0) {
        LOGERROR("[%s] failed to start VM bundling thread\n", instanceId);
        return cleanup_bundling_task(pInstance, pParams, BUNDLING_FAILED);
    }

    return EUCA_OK;
}

//!
//! Handles the bundle restart request.
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] psInstanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return the result of the restart_instance() call, EUCA_ERROR or EUCA_NOT_FOUND_ERROR.
//!
//! @see restart_instance()
//!
static int doBundleRestartInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *psInstanceId)
{
    int error = 0;
    ncInstance *pInstance = NULL;

    // sanity checking
    if (psInstanceId == NULL) {
        LOGERROR("bundle restart instance called with invalid parameters\n");
        return (EUCA_ERROR);
    }

    sem_p(inst_sem);
    {
        // we hold inst_sem in this block
        if ((pInstance = find_instance(&global_instances, psInstanceId)) == NULL) {
            LOGERROR("[%s] failed to locate instance in memory\n", psInstanceId);
            sem_v(inst_sem);
            return (EUCA_NOT_FOUND_ERROR);
        }
        // Reset a few of our fields
        pInstance->state = BOOTING;
        pInstance->stateCode = EXTANT;
        pInstance->retries = LIBVIRT_QUERY_RETRIES;
        pInstance->bundlingTime = 0;
        pInstance->bundlePid = 0;
        pInstance->bundleCanceled = 0;
        pInstance->bundleBucketExists = 0;
        pInstance->bundleTaskState = NOT_BUNDLING;

        // Set our state strings
        euca_strncpy(pInstance->stateName, instance_state_names[pInstance->stateCode], CHAR_BUFFER_SIZE);
        euca_strncpy(pInstance->bundleTaskStateName, bundling_progress_names[pInstance->bundleTaskState], CHAR_BUFFER_SIZE);

    }
    sem_v(inst_sem);

    // Now restart this instance regardless of bundling success or failure
    if ((error = find_and_start_instance(psInstanceId)) != EUCA_OK) {
        LOGERROR("[%s] instance not found\n", psInstanceId);
        return (error);
    }
    return (0);
}

//!
//! Handles the cancel bundle task request.
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] psInstanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_OK on success or EUCA_NOT_FOUND_ERROR on failure.
//!
static int doCancelBundleTask(struct nc_state_t *nc, ncMetadata * pMeta, char *psInstanceId)
{
    ncInstance *pInstance = NULL;

    if ((pInstance = find_instance(&global_instances, psInstanceId)) == NULL) {
        LOGERROR("[%s] instance not found\n", psInstanceId);
        return EUCA_NOT_FOUND_ERROR;
    }

    pInstance->bundleCanceled = 1;     // record the intent to cancel bundling so that bundling thread can abort
    if ((pInstance->bundlePid > 0) && !check_process(pInstance->bundlePid, "euca-bundle-upload")) {
        LOGDEBUG("[%s] found bundlePid '%d', sending kill signal...\n", psInstanceId, pInstance->bundlePid);
        kill(pInstance->bundlePid, 9);
        pInstance->bundlePid = 0;
    }

    return (EUCA_OK);
}

//!
//! Handles the describe bundle tasks request.
//!
//! @param[in]  nc a pointer to the NC state structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instIds a list of instance identifier string
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[out] outBundleTasks a pointer to the created bundle tasks list
//! @param[out] outBundleTasksLen the number of bundle tasks in the outBundleTasks list
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR
//!         and EUCA_MEMORY_ERROR.
//!
static int doDescribeBundleTasks(struct nc_state_t *nc, ncMetadata * pMeta, char **instIds, int instIdsLen, bundleTask *** outBundleTasks, int *outBundleTasksLen)
{
    int i = 0;
    int j = 0;
    ncInstance *pInstance = NULL;

    if ((instIdsLen < 1) || (instIds == NULL)) {
        LOGDEBUG("internal error (invalid parameters to doDescribeBundleTasks)\n");
        return EUCA_ERROR;
    }
    // Maximum size
    if ((*outBundleTasks = EUCA_ZALLOC(instIdsLen, sizeof(bundleTask *))) == NULL) {
        return EUCA_MEMORY_ERROR;
    }

    *outBundleTasksLen = 0;            // we may return fewer than instIdsLen

    for (i = 0, j = 0; i < instIdsLen; i++) {
        bundleTask *bundle = NULL;

        sem_p(inst_sem);
        if ((pInstance = find_instance(&global_instances, instIds[i])) != NULL) {
            if ((bundle = allocate_bundleTask(pInstance)) == NULL) {
                LOGERROR("out of memory\n");
                sem_v(inst_sem);
                return EUCA_MEMORY_ERROR;
            }
        }
        sem_v(inst_sem);

        if (bundle) {
            (*outBundleTasks)[j++] = bundle;
            (*outBundleTasksLen)++;
        }
    }

    return EUCA_OK;
}

//!
//! Handles the describe sensors request.
//!
//! @param[in]  nc a pointer to the NC state structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  historySize the size of the data history to retrieve
//! @param[in]  collectionIntervalTimeMs the data collection interval in milliseconds
//! @param[in]  instIds the list of instance identifiers string
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[in]  sensorIds a list of sensor identifiers string
//! @param[in]  sensorIdsLen the number of sensor identifiers string in the sensorIds list
//! @param[out] outResources a list of sensor resources created by this request
//! @param[out] outResourcesLen the number of sensor resources contained in the outResources list
//!
//! @return EUCA_OK on success or EUCA_MEMORY_ERROR on failure.
//!
static int doDescribeSensors(struct nc_state_t *nc, ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds,
                             int instIdsLen, char **sensorIds, int sensorIdsLen, sensorResource *** outResources, int *outResourcesLen)
{
    int total;

    int err = sensor_config(historySize, collectionIntervalTimeMs); // update the config parameters if they are different
    if (err != 0)
        LOGERROR("failed to update sensor configuration (err=%d)\n", err);

    sem_p(inst_copy_sem);
    if (instIdsLen == 0)               // describe all instances
        total = total_instances(&global_instances_copy);
    else
        total = instIdsLen;

    sensorResource **rss = NULL;
    if (total > 0) {
        rss = EUCA_ZALLOC(total, sizeof(sensorResource *));
        if (rss == NULL) {
            sem_v(inst_copy_sem);
            return EUCA_MEMORY_ERROR;
        }
    }

    int k = 0;

    ncInstance *instance;
    for (int i = 0; (instance = get_instance(&global_instances_copy)) != NULL; i++) {
        // only pick ones the user (or admin) is allowed to see
        if (strcmp(pMeta->userId, nc->admin_user_id)
            && strcmp(pMeta->userId, instance->userId))
            continue;

        if (instIdsLen > 0) {
            int j;

            for (j = 0; j < instIdsLen; j++)
                if (!strcmp(instance->instanceId, instIds[j]))
                    break;

            if (j >= instIdsLen)
                // instance of no relevance right now
                continue;
        }

        assert(k < total);
        rss[k] = EUCA_ZALLOC(1, sizeof(sensorResource));
        if (sensor_get_instance_data(instance->instanceId, sensorIds, sensorIdsLen, rss + k, 1) != EUCA_OK) {
            LOGDEBUG("[%s] failed to retrieve sensor data\n", instance->instanceId);
            EUCA_FREE(rss[k]);
        } else {
            k++;
        }
    }

    *outResourcesLen = k;
    *outResources = rss;
    sem_v(inst_copy_sem);

    LOGDEBUG("found %d resource(s)\n", k);
    return EUCA_OK;
}

//!
//! Handles the node modification request.
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] stateName
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! TODO: doxygen
//!
static int doModifyNode(struct nc_state_t *nc, ncMetadata * pMeta, char *stateName)
{
    int ret = EUCA_OK;
    boolean did_change_state = FALSE;

    if (!strcasecmp(stateName, "enabled")) {
        if (nc->is_enabled == FALSE) {
            nc->is_enabled = TRUE;
            did_change_state = TRUE;
        }

    } else if (!strcasecmp(stateName, "disabled")) {
        if (nc->is_enabled == TRUE) {
            nc->is_enabled = FALSE;
            did_change_state = TRUE;
        }

    } else {
        LOGERROR("unexpected state '%s' requested for the node\n", stateName);
        ret = EUCA_ERROR;
    }

    if (did_change_state) {
        LOGINFO("node state change to '%s'\n", stateName);
        if (gen_nc_xml(nc) != EUCA_OK) {
            LOGERROR("failed to update NC state on disk\n");
            ret = EUCA_ERROR;
        } else {
            LOGINFO("wrote NC state to disk\n");
        }
    }

    return ret;
}

//!
//! Handles the instance migration request.
//!
//! @param[in]  nc a pointer to the NC state structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instances metadata for the instance to migrate to destination
//! @param[in]  instancesLen number of instances in the instance list
//! @param[in]  action IP of the destination Node Controller
//! @param[in]  credentials credentials that enable the migration
//!
//! @return EUCA_OK on sucess or EUCA_ERROR on failure
//!
//! TODO: doxygen
//!
static int doMigrateInstances(struct nc_state_t *nc, ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials)
{
    LOGERROR("no default for %s!\n", __func__);
    return (EUCA_UNSUPPORTED_ERROR);
}

//!
//! Defines the thread that does the actual restart or shutdown of an instance
//!
//! @param[in] arg a transparent pointer to the argument passed to this thread handler
//!
//! @return Always return NULL
//!
static void *startstop_thread(void *arg)
{
    startstop_params *pParams = ((startstop_params *) arg);

    LOGDEBUG("[%s] spawning start/stop thread\n", pParams->instanceId);

    if (pParams->do_stop) {
        // this is a 'stop'
        find_and_stop_instance(pParams->instanceId);
    } else {
        // this is a 'start'
        find_and_start_instance(pParams->instanceId);
    }

    EUCA_FREE(pParams);
    return (NULL);
}

//!
//! Handles the start request for an instance.
//!
//! @param[in] nc a pointer to the NC state structure to initialize
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_OK on success or proper error code.
//!
static int doStartInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId)
{
    pthread_t tcb = { 0 };
    ncInstance *instance = NULL;
    startstop_params *params = NULL;

    params = EUCA_ZALLOC(1, sizeof(startstop_params));
    if (params == NULL) {
        return EUCA_MEMORY_ERROR;
    }

    sem_p(inst_sem);
    {
        instance = find_instance(&global_instances, instanceId);
    }
    sem_v(inst_sem);

    if (instance == NULL) {
        LOGERROR("[%s] cannot find instance\n", instanceId);
        EUCA_FREE(params);
        return (EUCA_NOT_FOUND_ERROR);
    }
    // since shutdown/restart may take a while, we do them in a thread
    euca_strncpy(params->instanceId, instanceId, CHAR_BUFFER_SIZE);
    params->do_stop = FALSE;           // the only difference between do{Start|Stop}Instance()
    if (pthread_create(&tcb, NULL, startstop_thread, (void *)params)) {
        LOGERROR("[%s] failed to spawn the start/stop thread\n", instanceId);
        EUCA_FREE(params);
        return (EUCA_FATAL_ERROR);
    }
    // from here on we do not need to free 'params' as the thread will do it

    if (pthread_detach(tcb)) {
        LOGERROR("[%s] failed to detach the start/stop thread\n", instanceId);
        return (EUCA_FATAL_ERROR);
    }

    return (EUCA_OK);
}

//!
//! Handles the shutdown request for an instance.
//!
//! @param[in] nc a pointer to the NC state structure to initialize
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_OK on success or proper error code.
//!
static int doStopInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId)
{
    pthread_t tcb = { 0 };
    ncInstance *instance = NULL;
    startstop_params *params = NULL;

    params = EUCA_ZALLOC(1, sizeof(startstop_params));
    if (params == NULL) {
        return EUCA_MEMORY_ERROR;
    }

    sem_p(inst_sem);
    {
        instance = find_instance(&global_instances, instanceId);
    }
    sem_v(inst_sem);

    if (instance == NULL) {
        LOGERROR("[%s] cannot find instance\n", instanceId);
        EUCA_FREE(params);
        return (EUCA_NOT_FOUND_ERROR);
    }
    // since shutdown/restart may take a while, we do them in a thread
    euca_strncpy(params->instanceId, instanceId, CHAR_BUFFER_SIZE);
    params->do_stop = TRUE;            // the only difference between do{Start|Stop}Instance()
    if (pthread_create(&tcb, NULL, startstop_thread, (void *)params)) {
        LOGERROR("[%s] failed to spawn the start/stop thread\n", instanceId);
        EUCA_FREE(params);
        return (EUCA_FATAL_ERROR);
    }
    // from here on we do not need to free 'params' as the thread will do it

    if (pthread_detach(tcb)) {
        LOGERROR("[%s] failed to detach the start/stop thread\n", instanceId);
        return (EUCA_FATAL_ERROR);
    }

    return (EUCA_OK);
}
