// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
#include <euca_auth.h>
#include <vbr.h>
#include <sensor.h>
#include <euca_string.h>
#include <euca_file.h>
#include <euca_gni.h>
#include <data.h>

#include "handlers.h"
#include "xml.h"
#include "hooks.h"
#include <ebs_utils.h>
#include "diskutil.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define VOL_RETRIES 3

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
extern struct nc_state_t nc_state;    //!< Global NC state structure

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

static boolean use_virtio_net = FALSE;
static boolean use_virtio_disk = FALSE;
static boolean use_virtio_root = FALSE;

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
                         char **groupNames, int groupNamesSize, char *rootDirective, char **groupIds, int groupIdsSize, netConfig * secNetCfgs, int secNetCfgsLen, ncInstance ** outInst);
static int doRebootInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId);
static int doGetConsoleOutput(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char **consoleOutput);
static int doTerminateInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, int force, int *shutdownState, int *previousState);
static int doDescribeInstances(struct nc_state_t *nc, ncMetadata * pMeta, char **instIds, int instIdsLen, ncInstance *** outInsts, int *outInstsLen);
static int doDescribeResource(struct nc_state_t *nc, ncMetadata * pMeta, char *resourceType, ncResource ** outRes);
static int doBroadcastNetworkInfo(struct nc_state_t *nc, ncMetadata * pMeta, char *networkInfo);
static int doAssignAddress(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *publicIp);
static int doPowerDown(struct nc_state_t *nc, ncMetadata * pMeta);
static int doStartNetwork(struct nc_state_t *nc, ncMetadata * pMeta, char *uuid, char **remoteHosts, int remoteHostsLen, int port, int vlan);
static int doAttachVolume(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev);
static int doDetachVolume(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev, int force);
static int doAttachNetworkInterface(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, netConfig * netCfg);
static int doDetachNetworkInterface(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *interfaceId, int force);
static void change_createImage_state(ncInstance * instance, createImage_progress state);
static int cleanup_createImage_task(ncInstance * instance, struct createImage_params_t *params, instance_states state, createImage_progress result);
static void *createImage_thread(void *arg);
static int doCreateImage(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev);
static void change_bundling_state(ncInstance * instance, bundling_progress state);
static int cleanup_bundling_task(ncInstance * instance, struct bundling_params_t *params, bundling_progress result);
static void *bundling_thread(void *arg);
static int doBundleInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *objectStorageURL,
                            char *userPublicKey, char *S3Policy, char *S3PolicySig, char *architecture);
static int doBundleRestartInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *psInstanceId);
static int doCancelBundleTask(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId);
static int doDescribeBundleTasks(struct nc_state_t *nc, ncMetadata * pMeta, char **instIds, int instIdsLen, bundleTask *** outBundleTasks, int *outBundleTasksLen);
static int doDescribeSensors(struct nc_state_t *nc, ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds,
                             int instIdsLen, char **sensorIds, int sensorIdsLen, sensorResource *** outResources, int *outResourcesLen);
static int doModifyNode(struct nc_state_t *nc, ncMetadata * pMeta, char *stateName);
static int doMigrateInstances(struct nc_state_t *nc, ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials, char **resourceLocations, int resourceLocationsLen);
static void *startstop_thread(void *arg);
static int doStartInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId);
static int doStopInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId);

static int get_instance_path(const char *instanceId, char *path, int path_len);

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
    .doAttachNetworkInterface = doAttachNetworkInterface,
    .doDetachNetworkInterface = doDetachNetworkInterface,
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

static void refresh_instance_resources(const char *instanceId)
{
    char res_name[1][MAX_SENSOR_NAME_LEN] = { "" };
    char res_alias[1][MAX_SENSOR_NAME_LEN] = { "" };

    euca_strncpy(res_name[0], instanceId, MAX_SENSOR_NAME_LEN);

    // we serialize all hypervisor calls and
    // sensor_refresh_resources() may ultimately
    // call the hypervisor
    {
        sem_p(hyp_sem);
        sensor_refresh_resources(res_name, res_alias, 1);   // refresh stats
        sem_v(hyp_sem);
    }
}

//!
//! Default hypervisor initialize handler
//!
//! @param[in] nc a pointer to the NC state structure
//!
//! @return Always returns EUCA_OK for now
//!
static int doInitialize(struct nc_state_t *nc)
{
    use_virtio_net = nc->config_use_virtio_net;
    use_virtio_disk = nc->config_use_virtio_disk;
    use_virtio_root = nc->config_use_virtio_root;
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
                         char **groupNames, int groupNamesSize, char *rootDirective, char **groupIds, int groupIdsSize,
                         netConfig * secNetCfgs, int secNetCfgsLen, ncInstance ** outInstPtr)
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
                                 &ncnet, keyName, userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize, groupIds, groupIdsSize,
                                 secNetCfgs, secNetCfgsLen);

    if (kernelId)
        euca_strncpy(instance->kernelId, kernelId, sizeof(instance->kernelId));

    if (ramdiskId)
        euca_strncpy(instance->ramdiskId, ramdiskId, sizeof(instance->ramdiskId));

    if (credential)
        euca_strncpy(instance->credential, credential, sizeof(instance->credential));

    if (rootDirective)
        euca_strncpy(instance->rootDirective, rootDirective, sizeof(instance->rootDirective));

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
    set_corrid_pthread(get_corrid() != NULL ? get_corrid()->correlation_id : NULL, instance->tcb);
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
            deadline = time(NULL) + nc_state.shutdown_grace_period_sec;

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

    // we want to move console.log out of the way, but we don't want to lose it
    {
        char ipath[EUCA_MAX_PATH] = { 0 };
        if (get_instance_path(instanceId, ipath, sizeof(ipath)) != EUCA_OK || strlen(ipath) <= 0) {
            LOGERROR("Failed to find instance path for instance %s\n", instanceId);
        } else if (check_path(ipath) == 0) {
            char log_path_current[EUCA_MAX_PATH] = { 0 };
            char log_path_new[EUCA_MAX_PATH] = { 0 };
            snprintf(log_path_current, (sizeof(log_path_current) - 1), "%s/console.log", ipath);
            snprintf(log_path_new,     (sizeof(log_path_new) - 1), "%s/console.log.old", ipath);
            if (check_path(log_path_current) == 0) {
                if (rename(log_path_current, log_path_new) == -1) {
                    LOGERROR("Failed to move console log file from %s to %s\n", log_path_current, log_path_new);
                }
            }
        }
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
            virDomainFree(dom);
            unlock_hypervisor_conn();
            EUCA_FREE(psXML);
            return (EUCA_ERROR);
        }
        // start it
        if ((dom = virDomainCreateLinux(conn, psXML, 0)) == NULL) {
            LOGERROR("[%s] failed to start instance\n", psInstanceId);
        } else {
            virDomainFree(dom);

            if (!strcmp(nc_state.pEucaNet->sMode, NETMODE_VPCMIDO)) {
                bridge_instance_interfaces_remove(&nc_state, pInstance);
            }
            // Fix for EUCA-12608
            if (!strcmp(nc_state.pEucaNet->sMode, NETMODE_EDGE)) {
                char iface[16];
                snprintf(iface, 16, "vn_%s", pInstance->instanceId);
                bridge_interface_set_hairpin(&nc_state, pInstance, iface);
            } 
        }
        unlock_hypervisor_conn();

        //! @TODO: check if we sensor values survive stop/start
        refresh_instance_resources(psInstanceId);
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

    {                                  // find the instance to ensure we know about it
        sem_p(inst_sem);
        instance = find_instance(&global_instances, instanceId);
        if (instance != NULL) {
            instance->bail_flag = TRUE; // request any pending download retries to bail
            instance->terminationRequestedTime = time(NULL);
            save_instance_struct(instance);
        }
        sem_v(inst_sem);
    }

    if (instance == NULL)
        return EUCA_NOT_FOUND_ERROR;

    refresh_instance_resources(instanceId);

    // do the shutdown in a thread
    pthread_attr_t tattr;
    pthread_t tid;
    pthread_attr_init(&tattr);
    pthread_attr_setdetachstate(&tattr, PTHREAD_CREATE_DETACHED);
    void *param = (void *)strdup(instanceId);
    if (pthread_create(&tid, &tattr, terminating_thread, (void *)param) != 0) {
        LOGERROR("[%s] failed to start VM termination thread\n", instanceId);
    } else {
        set_corrid_pthread(get_corrid() != NULL ? get_corrid()->correlation_id : NULL, tid);
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
//! Adds up NC disk usage for an instance
//!
static long long get_disk_use_gb(virtualMachine * vm)
{
    long long disk_use_bytes = 0L;

    for (int i = 0; i < EUCA_MAX_VBRS && i < vm->virtualBootRecordLen; i++) {
        virtualBootRecord *vbr = &(vm->virtualBootRecord[i]);
        if (vbr->type != NC_RESOURCE_EBS && // EBS volumes do not count
            vbr->type != NC_RESOURCE_KERNEL &&  // EKI doesn't count, though it maybe should
            vbr->type != NC_RESOURCE_RAMDISK && // ERI doesn't count, though it maybe should
            vbr->type != NC_RESOURCE_FULLDISK && // full pass through hollow disk created from partitions
            vbr->type != NC_RESOURCE_BOOT) {    // boot sector is too small to be worth counting
            disk_use_bytes += vbr->sizeBytes;
        }
    }

    return disk_use_bytes / (1024 * 1024 * 1024);
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

    // EUCA-10056 if EUCANETD is not running, then we have no resources available for instances
    if (!strcmp(nc->pEucaNet->sMode, NETMODE_EDGE)) {
        if (nc->isEucanetdEnabled == FALSE) {
            if ((res = allocate_resource("disabled", nc->migration_capable, nc->iqn, nc->mem_max, 0, nc->disk_max, 0, nc->cores_max, 0, "none", "KVM")) == NULL) {
                LOGERROR("out of memory\n");
                return (EUCA_MEMORY_ERROR);
            }
            LOGDEBUG("returning status=%s cores=%d/%d mem=%d/%d disk=%d/%d iqn=%s\n", res->nodeStatus, res->numberOfCoresAvailable, res->numberOfCoresMax, res->memorySizeAvailable,
                     res->memorySizeMax, res->diskSizeAvailable, res->diskSizeMax, res->iqn);
            (*outRes) = res;
            return (EUCA_OK);
        }
    }

    sem_p(inst_copy_sem);
    while ((inst = get_instance(&global_instances_copy)) != NULL) {
        if (inst->state == TEARDOWN)
            continue;                  // they don't take up resources
        sum_mem += inst->params.mem;
        sum_disk += get_disk_use_gb(&(inst->params));
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
    res = allocate_resource(nc->is_enabled ? "enabled" : "disabled", nc->migration_capable, nc->iqn, nc->mem_max, mem_free, nc->disk_max, disk_free, nc->cores_max, cores_free,
                            "none", "KVM");
    if (res == NULL) {
        LOGERROR("out of memory\n");
        return (EUCA_MEMORY_ERROR);
    }
    (*outRes) = res;

    LOGDEBUG("Core status:   in-use %d physical %lld over-committed %s\n", sum_cores, nc->phy_max_cores, (((sum_cores + cores_free) > nc->phy_max_cores) ? "yes" : "no"));
    LOGDEBUG("Memory status: in-use %lld physical %lld over-committed %s\n", sum_mem, nc->phy_max_mem, (((sum_mem + mem_free) > nc->phy_max_mem) ? "yes" : "no"));
    LOGDEBUG("returning status=%s cores=%d/%d mem=%d/%d disk=%d/%d iqn=%s\n", res->nodeStatus, res->numberOfCoresAvailable, res->numberOfCoresMax, res->memorySizeAvailable,
             res->memorySizeMax, res->diskSizeAvailable, res->diskSizeMax, res->iqn);
    return (EUCA_OK);
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
    snprintf(xmlpath, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/global_network_info.xml", nc->home);
    LOGDEBUG("decoding/writing buffer to (%s)\n", xmlpath);
    xmlbuf = base64_dec((unsigned char *)networkInfo, strlen(networkInfo));
    LOGTRACE("decoded networkInfo=%s\n", xmlbuf);
    if (xmlbuf) {
        rc = str2file(xmlbuf, xmlpath, O_CREAT | O_TRUNC | O_WRONLY, 0600, FALSE);
        if (rc) {
            LOGERROR("could not write XML data to file (%s): (%d)\n", xmlpath, rc);
            ret = EUCA_ERROR;
        }
        EUCA_FREE(xmlbuf);
    } else {
        LOGERROR("could not b64 decode input buffer\n");
        ret = EUCA_ERROR;
    }

    if (EUCA_OK == ret && 
        nc && nc->pEucaNet &&
        !strcmp(nc->pEucaNet->sMode, NETMODE_VPCMIDO)) {
        rc = EUCA_OK;
        LOGDEBUG("attempting to parse GNI at %s\n", xmlpath);

        time_t start, stop;

        time(&start);
        rc = find_interface_changes(xmlpath);
        time(&stop);
        LOGDEBUG("completed parsing GNI and performing actions\n");

        if (rc) {
            LOGERROR("error encountered during interface change reconciliation (%d)\n", rc)
            ret = EUCA_ERROR;
        }

        LOGDEBUG("%.6f seconds to complete interface change processing\n", difftime(stop, start));
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
            snprintf(instance->ncnet.publicIp, INET_ADDR_LEN, "%s", publicIp);
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
    return (0);
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
static int xen_detach_helper(struct nc_state_t *nc, const char *instanceId, const char *localDevReal, const char *xml)
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

// create or update volume record in the instance struct
static int update_volume(char *instanceId, char *volumeId, char *attachmentToken, char *connect_string, char *canonicalDev, const char *state, const char *xml,
                         boolean do_update_aliases)
{
    int ret = EUCA_OK;

    sem_p(inst_sem);
    {
        ncInstance *instance = find_instance(&global_instances, instanceId);
        ncVolume *volume = NULL;

        if (instance) {
            volume = save_volume(instance, volumeId, attachmentToken, connect_string, canonicalDev, state, xml);
        } else {
            LOGERROR("[%s][%s] failed to find instance for a volume operation\n", instanceId, volumeId);
            ret = EUCA_ERROR;
        }
        if (volume) {
            save_instance_struct(instance);
            copy_instances();
            if (do_update_aliases) {
                update_disk_aliases(instance);  // ask sensor subsystem to stop tracking the volume
            }
        } else {
            LOGERROR("[%s][%s] failed to update the volume record\n", instanceId, volumeId);
            ret = EUCA_ERROR;
        }
    }
    sem_v(inst_sem);

    return ret;
}

int connect_ebs(const char *dev_name, const char *dev_serial, const char *dev_bus, struct nc_state_t *nc, char *instanceId, char *volumeId, char *attachmentToken,
                char **libvirt_xml, ebs_volume_data ** vol_data)
{
    int ret = EUCA_OK;
    char scUrl[512];

    LOGTRACE("[%s][%s] Connecting EBS volume to local host\n", instanceId, volumeId);
    if (get_service_url("storage", nc, scUrl) != EUCA_OK || strlen(scUrl) == 0) {
        LOGERROR("[%s][%s] Failed to lookup enabled Storage Controller. Cannot attach volume: %s\n", instanceId, volumeId, volumeId);
        return EUCA_ERROR;
    } else {
        LOGTRACE("[%s][%s] Using SC URL: %s\n", instanceId, volumeId, scUrl);
    }

    if (connect_ebs_volume(dev_name, dev_serial, dev_bus, scUrl, attachmentToken, nc->config_use_ws_sec, nc->config_sc_policy_file, nc->ip, nc->iqn, libvirt_xml, vol_data) !=
        EUCA_OK) {
        LOGERROR("Error connecting ebs volume %s\n", attachmentToken);
        return EUCA_ERROR;
    }

    if (!libvirt_xml) {
        LOGERROR("[%s][%s] failed to connect to EBS target\n", instanceId, volumeId);
        return EUCA_ERROR;
    } else {
        LOGDEBUG("[%s][%s] attached EBS target\n", instanceId, volumeId);
    }

    return ret;
}

int disconnect_ebs(struct nc_state_t *nc, char *instanceId, char *volumeId, char *attachmentToken, char *connect_string)
{
    int ret = EUCA_OK;
    char scUrl[512];

    LOGTRACE("[%s][%s] Disnnecting EBS volume from local host\n", instanceId, volumeId);
    if (get_service_url("storage", nc, scUrl) != EUCA_OK || strlen(scUrl) == 0) {
        LOGERROR("[%s][%s] Failed to lookup enabled Storage Controller. Cannot attach volume: %s\n", instanceId, volumeId, volumeId);
        return EUCA_ERROR;
    } else {
        LOGTRACE("[%s][%s] Using SC URL: %s\n", instanceId, volumeId, scUrl);
    }

    if (disconnect_ebs_volume(scUrl, nc->config_use_ws_sec, nc->config_sc_policy_file, attachmentToken, connect_string, nc->ip, nc->iqn) != EUCA_OK) {
        LOGERROR("[%s][%s] Error disconnecting ebs volume on error rollback.\n", instanceId, volumeId);
    }

    return ret;
}

static int get_instance_path(const char *instanceId, char *path, int path_len)
{
    int ret = EUCA_OK;

    sem_p(inst_sem);
    {
        ncInstance *instance = find_instance(&global_instances, instanceId);
        if (instance != NULL) {
            euca_strncpy(path, instance->instancePath, path_len);
        } else {
            ret = EUCA_ERROR;
        }
    }
    sem_v(inst_sem);

    return ret;
}

int create_vol_xml(const char *instanceId, const char *volumeId, const char *xml_in, char **xml_out)
{
    int ret = EUCA_OK;

    char ipath[EUCA_MAX_PATH] = { 0 };
    if(get_instance_path(instanceId, ipath, sizeof(ipath)) != EUCA_OK || strlen(ipath) <= 0) {
        LOGERROR("Failed to find instance path for instance %s\n", instanceId);
        return EUCA_ERROR;
    }

    char lpath[EUCA_MAX_PATH] = { 0 };
    snprintf(lpath, (sizeof(lpath) - 1), EUCALYPTUS_VOLUME_LIBVIRT_XML_PATH_FORMAT, ipath, volumeId);  // vol-XXX-libvirt.xml
    lpath[sizeof(lpath) - 1] = '\0';

    if (str2file(xml_in, lpath, O_CREAT | O_TRUNC | O_WRONLY, BACKING_FILE_PERM, FALSE) != EUCA_OK) {
        LOGERROR("[%s][%s] failed to write volume XML to %s\n", instanceId, volumeId, lpath);
        ret = EUCA_ERROR;
        goto release;
    }
    // invoke hooks
    if (call_hooks(NC_EVENT_PRE_ATTACH, lpath)) {
        LOGERROR("[%s][%s] cancelled volume attachment via hooks\n", instanceId, volumeId);
        ret = EUCA_ERROR;
        goto release;
    }
    // read in libvirt XML, which may have been modified by the hook above
    if ((*xml_out = file2str(lpath)) == NULL) {
        LOGERROR("[%s][%s] failed to read volume XML from %s\n", instanceId, volumeId, lpath);
        ret = EUCA_ERROR;
        goto release;
    }
release:
    return ret;
}

char *read_vol_xml(const char *instanceId, const char *volumeId)
{
    char ipath[EUCA_MAX_PATH];
    get_instance_path(instanceId, ipath, sizeof(ipath));

    char lpath[EUCA_MAX_PATH];
    snprintf(lpath, (sizeof(lpath) - 1), EUCALYPTUS_VOLUME_LIBVIRT_XML_PATH_FORMAT, ipath, volumeId);   // vol-XXX-libvirt.xml
    lpath[sizeof(lpath) - 1] = '\0';

    // read in libvirt XML
    char *libvirt_xml = file2str(lpath);
    if (libvirt_xml == NULL) {
        LOGERROR("[%s][%s] failed to read volume XML from %s\n", instanceId, volumeId, lpath);
    }
    return libvirt_xml;
}

int delete_vol_xml(const char *instanceId, const char *volumeId)
{
    char ipath[EUCA_MAX_PATH];
    get_instance_path(instanceId, ipath, sizeof(ipath));

    char lpath[EUCA_MAX_PATH];
    snprintf(lpath, (sizeof(lpath) - 1), EUCALYPTUS_VOLUME_LIBVIRT_XML_PATH_FORMAT, ipath, volumeId);   // vol-XXX-libvirt.xml
    lpath[sizeof(lpath) - 1] = '\0';

    call_hooks(NC_EVENT_POST_DETACH, lpath);    // invoke hooks, but do not do anything if they return error

    return (unlink(lpath)) ? (EUCA_ERROR) : (EUCA_OK);
}

int attach_vol_instance(struct nc_state_t *nc, const char *devName, const char *instanceId, const char *volumeId, const char *libvirt_xml)
{
    int ret = EUCA_OK;

    virConnectPtr conn = lock_hypervisor_conn();
    if (conn == NULL) {
        LOGERROR("[%s][%s] cannot get connection to hypervisor\n", instanceId, volumeId);
        return EUCA_HYPERVISOR_ERROR;
    }
    // find domain on hypervisor
    virDomainPtr dom = virDomainLookupByName(conn, instanceId);
    if (dom == NULL) {
        unlock_hypervisor_conn();
        return EUCA_HYPERVISOR_ERROR;
    }

    int err = 0;
    for (int i = 1; i <= VOL_RETRIES; i++) {
        err = virDomainAttachDevice(dom, libvirt_xml);
        if (err) {
            LOGERROR("[%s][%s] failed to attach EBS guest device on attempt %d of 3\n", instanceId, volumeId, i);
            LOGDEBUG("[%s][%s] virDomainAttachDevice() failed (err=%d) XML='%s'\n", instanceId, volumeId, err, libvirt_xml);
            sleep(3);                  // sleep a bit and retry.
        } else {
            break;
        }
    }

    if (err) {
        LOGERROR("[%s][%s] failed to attach EBS guest device after %d tries\n", instanceId, volumeId, VOL_RETRIES);
        LOGDEBUG("[%s][%s] virDomainAttachDevice() failed (err=%d) XML='%s'\n", instanceId, volumeId, err, libvirt_xml);
        ret = EUCA_ERROR;
    }

    virDomainFree(dom);                // release libvirt resource
    unlock_hypervisor_conn();

    return ret;
}

int detach_vol_instance(struct nc_state_t *nc, const char *devName, const char *instanceId, const char *volumeId, const char *libvirt_xml)
{
    int ret = EUCA_OK;

    // connect to hypervisor, find the domain, detach the volume
    virConnectPtr conn = lock_hypervisor_conn();
    if (conn == NULL) {
        LOGERROR("[%s][%s] cannot get connection to hypervisor\n", instanceId, volumeId);
        ret = EUCA_HYPERVISOR_ERROR;
    } else {
        // find domain on hypervisor
        virDomainPtr dom = virDomainLookupByName(conn, instanceId);
        int libvirt_err = 0;
        if (dom != NULL) {
            libvirt_err = virDomainDetachDevice(dom, libvirt_xml);
            if (!strcmp(nc->H->name, "xen")) {
                libvirt_err = xen_detach_helper(nc, instanceId, devName, libvirt_xml);
            }
            virDomainFree(dom);        // release libvirt resource
        }
        unlock_hypervisor_conn();

        if (libvirt_err) {
            LOGERROR("[%s][%s] failed to detach EBS guest device\n", instanceId, volumeId);
            LOGDEBUG("[%s][%s] virDomainDetachDevice() or 'virsh detach' failed (err=%d) XML='%s'\n", instanceId, volumeId, libvirt_err, libvirt_xml);
            ret = EUCA_ERROR;
        }
    }

    return ret;
}

void set_serial_and_bus(const char *vol, const char *dev, char *serial, int serial_len, char *bus, int bus_len)
{
    snprintf(serial, serial_len, "euca-%s-dev-%s", vol, dev);

    if (use_virtio_disk) {
        snprintf(bus, bus_len, "virtio");
    } else {
        snprintf(bus, bus_len, "scsi");
    }
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
static int doAttachVolume(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev)
{
    int ret = EUCA_OK;
    boolean have_remote_device = FALSE;
    char *libvirt_xml = NULL;
    char *libvirt_xml_modified = NULL;
    char canonicalDev[32] = "";
    ebs_volume_data *vol_data = NULL;

    ret = canonicalize_dev(localDev, canonicalDev, sizeof(canonicalDev));
    if (ret)
        return ret;

    if (update_volume(instanceId, volumeId, attachmentToken, NULL, canonicalDev, VOL_STATE_ATTACHING, NULL, FALSE)) {
        return EUCA_ERROR;
    }

    char serial[128];
    char bus[16];
    set_serial_and_bus(volumeId, canonicalDev, serial, sizeof(serial), bus, sizeof(bus));
    if (connect_ebs(canonicalDev, serial, bus, nc, instanceId, volumeId, attachmentToken, &libvirt_xml, &vol_data)) {
        ret = EUCA_ERROR;
        goto release;
    }
    have_remote_device = TRUE;

    if (update_volume(instanceId, volumeId, attachmentToken, vol_data->connect_string, NULL, VOL_STATE_ATTACHING, libvirt_xml, FALSE)) {
        ret = EUCA_ERROR;
        goto release;
    }

    if (create_vol_xml(instanceId, volumeId, libvirt_xml, &libvirt_xml_modified) != EUCA_OK) {
        ret = EUCA_ERROR;
        goto release;
    }

    ret = attach_vol_instance(nc, canonicalDev, instanceId, volumeId, libvirt_xml_modified);

release:

    // record volume state in memory and on disk
    if (update_volume(instanceId, volumeId, NULL, NULL, NULL, (ret == EUCA_OK) ? VOL_STATE_ATTACHED : VOL_STATE_ATTACHING_FAILED, libvirt_xml_modified, (ret == EUCA_OK))
        && (libvirt_xml_modified != NULL)) {
        LOGERROR("[%s][%s] failed to save the volume record, aborting volume attachment (detaching)\n", instanceId, volumeId);

        detach_vol_instance(nc, canonicalDev, instanceId, volumeId, libvirt_xml_modified);
        ret = EUCA_ERROR;
    }
    // if iSCSI and there were problems, try to disconnect the target
    if (ret != EUCA_OK && have_remote_device) {
        LOGDEBUG("[%s][%s] attempting to disconnect EBS target due to attachment failure\n", instanceId, volumeId);
        if (vol_data != NULL && vol_data->connect_string[0] != '\0') {
            disconnect_ebs(nc, instanceId, volumeId, attachmentToken, vol_data->connect_string);
        }
    }

    if (ret == EUCA_OK) {
        LOGINFO("[%s][%s] EBS volume attached as guest device '%s'\n", instanceId, volumeId, canonicalDev);
    }

    EUCA_FREE(vol_data);
    EUCA_FREE(libvirt_xml);
    EUCA_FREE(libvirt_xml_modified);
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
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         EUCA_NOT_FOUND_ERROR and EUCA_HYPERVISOR_ERROR.
//!
static int doDetachVolume(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev, int force)
{
    int ret = EUCA_OK;
    char *libvirt_xml = NULL;
    char canonicalDev[32];
    char *connect_string = NULL;
    char *final_attachment_token = NULL;

    ret = canonicalize_dev(localDev, canonicalDev, sizeof(canonicalDev));
    if (ret)
        return ret;

    ret = update_volume(instanceId, volumeId, NULL, NULL, canonicalDev, VOL_STATE_DETACHING, NULL, FALSE);
    if (ret)
        return ret;

    // do iscsi connect shellout if remoteDev is an iSCSI target
    // get credentials, decrypt them
    // (used to have check if iscsi here, not necessary with AOE deprecation.)
    /// EBSTODO: verify that device is still there
    // remoteDevStr = get_volume_local_device(volume->connectionString);

    if ((libvirt_xml = read_vol_xml(instanceId, volumeId)) == NULL) {
        ret = EUCA_ERROR;
        goto disconnect;
    }
    // refresh stats so volume measurements are captured before it disappears
    refresh_instance_resources(instanceId);

    if (detach_vol_instance(nc, canonicalDev, instanceId, volumeId, libvirt_xml)) {
        if (!force)
            ret = EUCA_HYPERVISOR_ERROR;
    } else {
        delete_vol_xml(instanceId, volumeId);
    }

disconnect:

    update_volume(instanceId, volumeId, NULL, NULL, NULL, (ret == EUCA_OK) ? (VOL_STATE_DETACHED) : (VOL_STATE_DETACHING_FAILED), NULL, TRUE);

    sem_p(inst_sem);
    {
        ncInstance *instance = find_instance(&global_instances, instanceId);
        if (instance) {
            ncVolume *volume = save_volume(instance, volumeId, NULL, NULL, NULL, NULL, NULL);
            if (volume) {
                connect_string = strdup(volume->connectionString);
            }
            // check if input attachmentToken is valid
            if (attachmentToken == NULL || strlen(attachmentToken) == 0) {
                // If not, get it from volume struct
                final_attachment_token = strdup(volume->attachmentToken);
            } else {
                final_attachment_token = strdup(attachmentToken);
            }
        }
    }
    sem_v(inst_sem);

    ret = disconnect_ebs(nc, instanceId, volumeId, final_attachment_token, connect_string);
    if (ret == EUCA_OK)
        LOGINFO("[%s][%s] detached EBS guest device '%s'\n", instanceId, volumeId, canonicalDev);
    // remoteDev can be a long string, so to keep log readable, we log it at TRACE level unless there was a problem
    int log_level_for_devstring = EUCA_LOG_TRACE;
    if (ret != EUCA_OK)
        log_level_for_devstring = EUCA_LOG_DEBUG;
    EUCALOG(log_level_for_devstring, "[%s][%s] remote device string: %s\n", instanceId, volumeId, connect_string);
    EUCA_FREE(connect_string);
    EUCA_FREE(final_attachment_token);
    EUCA_FREE(libvirt_xml);

    if (force) {
        return (EUCA_OK);
    }
    return ret;
}

// create or update network interface record in the instance struct
static int update_network_interface(char *instanceId, netConfig * netCfg, const char *state)
{
    int ret = EUCA_OK;

    sem_p(inst_sem);
    {
        ncInstance *instance = find_instance(&global_instances, instanceId);
        netConfig *pNetCfg = NULL;

        if (instance) {
            pNetCfg = save_network_interface(instance, netCfg, state);
        } else {
            LOGERROR("[%s][%s] failed to find instance for a network interface operation\n", instanceId, netCfg->interfaceId);
            ret = EUCA_ERROR;
        }
        if (pNetCfg) {
            save_instance_struct(instance);
            copy_instances();
//            if (do_update_aliases) {
//                update_disk_aliases(instance);  // ask sensor subsystem to stop tracking the volume
//            }
        } else {
            LOGERROR("[%s][%s] failed to update the network interface record\n", instanceId, netCfg->interfaceId);
            ret = EUCA_ERROR;
        }
    }
    sem_v(inst_sem);

    return ret;
}

int attach_network_interface_instance(ncInstance *pInstance, const char *interfaceId, const char *libvirt_xml)
{
    int ret = EUCA_OK;

    virConnectPtr conn = lock_hypervisor_conn();
    if (conn == NULL) {
    LOGERROR("[%s][%s] cannot get connection to hypervisor\n", pInstance->instanceId, interfaceId);
        return EUCA_HYPERVISOR_ERROR;
    }

    // find domain on hypervisor
    virDomainPtr dom = virDomainLookupByName(conn, pInstance->instanceId);
    if (dom == NULL) {
        unlock_hypervisor_conn();
        return EUCA_HYPERVISOR_ERROR;
    }

    int err = 0;
    for (int i = 1; i <= VOL_RETRIES; i++) {
        LOGDEBUG("[%s][%s] attaching network interface to guest by invoking virDomainAttachDevice() with: %s\n", pInstance->instanceId, interfaceId, libvirt_xml);
        err = virDomainAttachDevice(dom, libvirt_xml);
        if (err) {
            LOGERROR("[%s][%s] failed to attach network interface to guest on attempt %d of 3\n", pInstance->instanceId, interfaceId, i);
            LOGDEBUG("[%s][%s] virDomainAttachDevice() failed (err=%d) XML='%s'\n", pInstance->instanceId, interfaceId, err, libvirt_xml);
            sleep(3);                  // sleep a bit and retry.
        } else {
            LOGTRACE("[%s][%s] attached network interface to guest, rc=%d\n", pInstance->instanceId, interfaceId, err);

            // remove the instance interface
            //char iface[16], cmd[EUCA_MAX_PATH], obuf[256], ebuf[256], sPath[EUCA_MAX_PATH];
            char iface[16];

            snprintf(iface, 16, "vn_%s", interfaceId);
            bridge_interface_remove(&nc_state, pInstance, iface);

/*
            // If this device does not have a 'brport' path, this isn't a bridge device
            snprintf(sPath, EUCA_MAX_PATH, "/sys/class/net/%s/brport/", iface);
            if (!check_directory(sPath)) {
                LOGDEBUG("[%s][%s] removing instance interface %s from host bridge\n", pInstance->instanceId, interfaceId, iface);
                snprintf(cmd, EUCA_MAX_PATH, "%s brctl delif %s %s", nc_state.rootwrap_cmd_path, pInstance->params.guestNicDeviceName, iface);
                err = timeshell(cmd, obuf, ebuf, 256, 10);
                if (err) {
                    LOGERROR("unable to remove instance interface from bridge after launch: instance will not be able to connect to midonet (will not connect to network): check bridge/libvirt/kvm health\n");
                }
            }
*/

            break;
        }
    }

    if (err) {
        LOGERROR("[%s][%s] failed to attach network device to guest after %d tries\n", pInstance->instanceId, interfaceId, VOL_RETRIES);
        LOGDEBUG("[%s][%s] virDomainAttachDevice() failed (err=%d) XML='%s'\n", pInstance->instanceId, interfaceId, err, libvirt_xml);
        ret = EUCA_ERROR;
    }

    virDomainFree(dom);                // release libvirt resource
    unlock_hypervisor_conn();

    return ret;
}

//!
//! Attach a given network interface to an instance (VPC mode only)
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] netConfig a pointer to netConfig data structure
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         EUCA_NOT_FOUND_ERROR and EUCA_HYPERVISOR_ERROR.
//!
static int doAttachNetworkInterface(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, netConfig * netCfg)
{
    int ret = EUCA_OK;
    char ipath[EUCA_MAX_PATH];
    char lpath[EUCA_MAX_PATH];
    char *libvirt_xml = NULL;

    LOGDEBUG("[%s][%s][%s] processing attach network interface request\n", instanceId, netCfg->interfaceId, netCfg->attachmentId);

    // Get the instance
    ncInstance *instance = find_instance(&global_instances, instanceId);
    assert(instance != NULL);

    if (is_network_interface_attached(instance, netCfg->interfaceId, netCfg->attachmentId)) {
        LOGDEBUG("[%s][%s][%s] network interface is either attaching or attached\n", instanceId, netCfg->interfaceId, netCfg->attachmentId);
        return EUCA_OK;
    }

    // Get the instance path
    euca_strncpy(ipath, instance->instancePath, EUCA_MAX_PATH);

    // Save network interface to instance structure, that should generate the network interface xml
    if ((ret = update_network_interface(instanceId, netCfg, VOL_STATE_ATTACHING))) {
        LOGERROR("[%s][%s][%d] Aborting network interface attach operation due to error creating network interface record\n", instanceId, netCfg->interfaceId, ret)
        goto release;
    }

    // Generate network interface libvirt xml
    if((ret = gen_libvirt_nic_xml(ipath, netCfg->interfaceId))) {
        LOGERROR("[%s][%s][%d] Aborting attach operation due to error updating network interface record\n", instanceId, netCfg->interfaceId, ret)
        goto release;
    }

    // Read libvirt xml content into a string
    snprintf(lpath, (sizeof(lpath) - 1), EUCALYPTUS_NIC_LIBVIRT_XML_PATH_FORMAT, ipath, netCfg->interfaceId);
    lpath[sizeof(lpath) - 1] = '\0';
    libvirt_xml = file2str(lpath);
    if (libvirt_xml == NULL) {
        LOGERROR("[%s][%s] failed to read network interface libvirt XML from %s\n", instanceId, netCfg->interfaceId, lpath);
        ret = EUCA_ERROR;
        goto release;
    }

    // Invoke libvirt attach device from an xml
    if ((ret = attach_network_interface_instance(instance, netCfg->interfaceId, libvirt_xml))) {
        LOGERROR("[%s][%s] libvirt attach device failed\n", instanceId, netCfg->interfaceId)
        goto release;
    }

    // Update network interface record in the instance structure
    if ((ret = update_network_interface(instanceId, netCfg, VOL_STATE_ATTACHED))) {
        LOGERROR("[%s][%s][%d] Aborting network interface attach operation due to error updating network interface record\n", instanceId, netCfg->interfaceId, ret)
        goto release;
    }

    LOGINFO("[%s][%s][%s] attached network interface successfully\n", instanceId, netCfg->interfaceId, netCfg->attachmentId);

    if(libvirt_xml) EUCA_FREE(libvirt_xml);
    return EUCA_OK;

release:

    if (update_network_interface(instanceId, netCfg, VOL_STATE_ATTACHING_FAILED)) {
        LOGERROR("[%s][%s]Failed to update network interface in cache during unwind from failed attach operation\n", instanceId, netCfg->interfaceId);
    }

    if(libvirt_xml) EUCA_FREE(libvirt_xml);
    return ret;
}

int detach_network_interface_instance(const char *instanceId, const char *interfaceId, const char *libvirt_xml)
{
    int ret = EUCA_OK;

    // connect to hypervisor, find the domain, detach the volume
    virConnectPtr conn = lock_hypervisor_conn();
    if (conn == NULL) {
        LOGERROR("[%s][%s] cannot get connection to hypervisor\n", instanceId, interfaceId);
        ret = EUCA_HYPERVISOR_ERROR;
    } else {
        // find domain on hypervisor
        virDomainPtr dom = virDomainLookupByName(conn, instanceId);
        int libvirt_err = 0;
        if (dom != NULL) {
            libvirt_err = virDomainDetachDevice(dom, libvirt_xml);
            virDomainFree(dom);        // release libvirt resource
        }
        unlock_hypervisor_conn();

        if (libvirt_err) {
            LOGERROR("[%s][%s] failed to detach network interface device\n", instanceId, interfaceId);
            LOGDEBUG("[%s][%s] virDomainDetachDevice() or 'virsh detach' failed (err=%d) XML='%s'\n",
                    instanceId, interfaceId, libvirt_err, libvirt_xml);
            ret = EUCA_ERROR;
        }
    }

    return ret;
}

//!
//! Detach a given network interface from an instance.
//!
//! @param[in] nc a pointer to the NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] attachmentId the attachment identifier string (eni-attach-XXXXXXXX)
//! @param[in] force if set to 1, this will force the volume to detach
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         EUCA_NOT_FOUND_ERROR and EUCA_HYPERVISOR_ERROR.
//!
static int doDetachNetworkInterface(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *attachmentId, int force)
{
    int ret = EUCA_OK;
    netConfig *netCfg = NULL;
    char ipath[EUCA_MAX_PATH];
    char lpath[EUCA_MAX_PATH];
    char *libvirt_xml = NULL;

    LOGINFO("[%s][%s] processing detach network interface request\n", instanceId, attachmentId);

    // Get the instance
    ncInstance *instance = find_instance(&global_instances, instanceId);
    assert(instance != NULL);

    // Get the network interface
    netCfg = find_network_interface_by_attachment(instance, attachmentId);
    if (netCfg == NULL) {
        LOGERROR("[%s][%s] no such network interface found\n", instanceId, attachmentId);
        return EUCA_ERROR;
    }

    if (!strncmp(netCfg->stateName, VOL_STATE_DETACHING, sizeof(VOL_STATE_DETACHING)) ||
            !strncmp(netCfg->stateName, VOL_STATE_DETACHED, sizeof(VOL_STATE_DETACHED))) {
        LOGDEBUG("[%s][%s] network interface is either detaching or detached\n", instanceId, attachmentId);
        return EUCA_OK;
    }

    euca_strncpy(ipath, instance->instancePath, EUCA_MAX_PATH);

    // Update network interface record in the instance structure
    if ((ret = update_network_interface(instanceId, netCfg, VOL_STATE_DETACHING))) {
        LOGERROR("[%s][%s][%d] Aborting network interface detach operation due to error updating network interface record\n", instanceId, attachmentId, ret)
        return ret;
    }

    // Read libvirt xml content into a string
    snprintf(lpath, (sizeof(lpath) - 1), EUCALYPTUS_NIC_LIBVIRT_XML_PATH_FORMAT, ipath, netCfg->interfaceId);
    lpath[sizeof(lpath) - 1] = '\0';
    libvirt_xml = file2str(lpath);
    if (libvirt_xml == NULL) {
        LOGERROR("[%s][%s] failed to read network interface libvirt XML from %s\n", instanceId, attachmentId, lpath);
        return ret;
    }

    // Invoke libvirt attach device from an xml
    if ((ret = detach_network_interface_instance(instanceId, attachmentId, libvirt_xml))) {
        LOGERROR("[%s][%s][%d] libvirt detach device failed\n", instanceId, attachmentId, ret)
        goto release;
    } else {
        // Remove libvirt.xml file
        if ((ret = (unlink(lpath) ? EUCA_ERROR : EUCA_OK))) {
            LOGERROR("[%s][%s][%d] failed to remove libvirt xml file %s\n", instanceId, attachmentId, ret, lpath);
        }
    }

    // Update network interface record in the instance structure
    if ((ret = update_network_interface(instanceId, netCfg, VOL_STATE_DETACHED))) {
        LOGERROR("[%s][%s][%d] Aborting network interface detach operation due to error updating network interface record\n", instanceId, netCfg->attachmentId, ret)
        goto release;
    }

    LOGINFO("[%s][%s] detached network interface successfully\n", instanceId, attachmentId);

    if(libvirt_xml) EUCA_FREE(libvirt_xml);
    return EUCA_OK;

release:

    if (update_network_interface(instanceId, netCfg, VOL_STATE_DETACHING_FAILED)) {
        LOGERROR("[%s][%s] Failed to update network interface in cache during unwind from failed detach operation\n", instanceId, attachmentId);
    }

    if(libvirt_xml) EUCA_FREE(libvirt_xml);
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
    LOGINFO("[%s] createImage result: %s\n", instance->instanceId, createImage_progress_names[result]);
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
        unset_corrid(get_corrid());
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
    unset_corrid(get_corrid());
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

    set_corrid_pthread(get_corrid() != NULL ? get_corrid()->correlation_id : NULL, tid);
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
    LOGINFO("[%s] bundling result: %s\n", pInstance->instanceId, bundling_progress_names[result]);

    sem_p(inst_sem);
    {
        change_bundling_state(pInstance, result);
        copy_instances();
    }
    sem_v(inst_sem);

    char run_workflow_work_dir[EUCA_MAX_PATH];
    snprintf(run_workflow_work_dir, sizeof(run_workflow_work_dir), "/tmp/bundle-%s", pInstance->instanceId);
    euca_rmdir(run_workflow_work_dir, TRUE);

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


int update_bundle_progress(char *instanceId, int readPercent)
{
    ncInstance *pInstance = NULL;
    // find the instance
    if ((pInstance = find_instance(&global_instances, instanceId)) == NULL) {
        LOGERROR("[%s] instance not found\n", instanceId);
        return EUCA_NOT_FOUND_ERROR;
    }
    double progress = (readPercent * 1.0)/100;
    sem_p(inst_sem);
    {
        pInstance->bundleTaskProgress = progress;
        LOGDEBUG("[%s] bundle progress is [%f]\n", instanceId, progress);
        copy_instances();
        save_instance_struct(pInstance);
    }
    sem_v(inst_sem);
    return 0;
}

int euca_run_bundle_parser(const char *line, void *instance_id)
{
    int read_percent;
    char *s;

    LOGTRACE("%s\n", line);            // log all output at TRACE level
    if (instance_id == NULL) {
        instance_id = (char *) "?";
    }
    // parse progress from lines like: 'Source file read: 12%'
    if ((s = strstr(line, "Source file read: "))
               && sscanf(s, "Source file read: %d", &read_percent) == 1) {
        if (update_bundle_progress((char *) instance_id, read_percent)) {
            LOGERROR("[%s] can't update bundling progress\n", (char *) instance_id);
        }
    } else if (strcasestr(line, "error")) { // any line with 'error'
        LOGERROR("%s\n", line);
    } else if (strcasestr(line, "warn")) {  // any line with 'warn'
        LOGWARN("%s\n", line);
    }
    return 0;
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
        unset_corrid(get_corrid());
        return NULL;
    }
    // check if bundling was cancelled while we waited
    if (pInstance->bundleCanceled) {
        LOGINFO("[%s] bundle task canceled; terminating bundling thread\n", pInstance->instanceId);
        cleanup_bundling_task(pInstance, pParams, BUNDLING_CANCELLED);
        unset_corrid(get_corrid());
        return NULL;
    }

    char backing_dev[PATH_MAX] = "";
    if (realpath(pInstance->params.root->backingPath, backing_dev) == NULL || diskutil_ch(backing_dev, EUCALYPTUS_ADMIN, NULL, 0) != EUCA_OK) { //! @TODO remove EUCALYPTUS_ADMIN
        LOGERROR("[%s] failed to resolve backing path (%s) or to chown it\n", pInstance->instanceId, backing_dev);
        cleanup_bundling_task(pInstance, pParams, BUNDLING_FAILED);
        unset_corrid(get_corrid());
        return NULL;
    }

    char totalSizeBytes[16];
    sprintf(totalSizeBytes, "%lld", pInstance->params.root->sizeBytes);
    LOGINFO("[%s] starting to bundle\n", pInstance->instanceId);
    char node_pk_path[EUCA_MAX_PATH];
    snprintf(node_pk_path, sizeof(node_pk_path), EUCALYPTUS_KEYS_DIR "/node-pk.pem", pParams->eucalyptusHomePath);
    char cloud_cert_path[EUCA_MAX_PATH];
    snprintf(cloud_cert_path, sizeof(cloud_cert_path), EUCALYPTUS_KEYS_DIR "/cloud-cert.pem", pParams->eucalyptusHomePath);
    char run_workflow_path[EUCA_MAX_PATH];
    snprintf(run_workflow_path, sizeof(run_workflow_path), "%s/usr/libexec/eucalyptus/euca-run-workflow", pParams->eucalyptusHomePath);
    char run_workflow_pid_path[EUCA_MAX_PATH];
    snprintf(run_workflow_pid_path, sizeof(run_workflow_pid_path), "/tmp/bundle-%s/process.pid", pInstance->instanceId);
    char run_workflow_work_dir[EUCA_MAX_PATH];
    snprintf(run_workflow_work_dir, sizeof(run_workflow_work_dir), "/tmp/bundle-%s", pInstance->instanceId);

    if (check_directory(run_workflow_work_dir)) {
        if (mkdir(run_workflow_work_dir, 0700)) {
            LOGWARN("mkdir failed: could not make directory '%s', check permissions\n", run_workflow_work_dir);
        }
    }
#define _COMMON_BUNDLING_PARAMS \
                         euca_run_bundle_parser,\
                         (void *)pInstance->instanceId,\
                         run_workflow_path,\
                         "read-raw/up-bundle",\
                         "--input-path", backing_dev,\
                         "--image-size", totalSizeBytes, \
                         "--encryption-cert-path", cloud_cert_path,\
                         "--signing-key-path", node_pk_path,\
                         "--prefix", pParams->filePrefix,\
                         "--bucket", pParams->bucketName,\
                         "--work-dir", run_workflow_work_dir, \
                         "--pid-path", run_workflow_pid_path, \
                         "--arch",  pParams->architecture, \
                         "--account", pParams->accountId, \
                         "--access-key", pParams->userPublicKey, /* @TODO: "PublicKey" is a misnomer*/ \
                         "--object-store-url", pParams->objectStorageURL,\
                         "--upload-policy", pParams->S3Policy,\
                         "--upload-policy-signature", pParams->S3PolicySig,\
                         "--emi", pInstance->imageId,

    if ((pParams->kernelId != NULL) && (pParams->ramdiskId != NULL)) {
        rc = euca_execlp_log(&status, _COMMON_BUNDLING_PARAMS "--eki", pParams->kernelId, "--eri", pParams->ramdiskId, NULL);
    } else {
        rc = euca_execlp_log(&status, _COMMON_BUNDLING_PARAMS NULL);
    }
    if (rc == EUCA_OK) {
        cleanup_bundling_task(pInstance, pParams, BUNDLING_SUCCESS);
        LOGINFO("[%s] finished bundling instance\n", pInstance->instanceId);
    } else if (rc == EUCA_THREAD_ERROR) {
        // bundler child was cancelled (killed), but should report it as failed
        cleanup_bundling_task(pInstance, pParams, BUNDLING_FAILED);
        LOGWARN("[%s] cancelled while bundling instance (rc=%d)\n", pInstance->instanceId, rc);
    } else {
        cleanup_bundling_task(pInstance, pParams, BUNDLING_FAILED);
        LOGERROR("[%s] failed while bundling instance (rc=%d)\n", pInstance->instanceId, rc);
    }

    unset_corrid(get_corrid());
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
//! @param[in] architecture image architecture
//!
//! @return EUCA_OK on success or proper error code. known error code returned include: EUCA_ERROR and
//!         EUCA_NOT_FOUND_ERROR. Error code from cleanup_bundling_task() and find_and_terminate_instance()
//!         are also returned.
//!
//! @see cleanup_bundling_task()
//! @see find_and_terminate_instance()
//!
static int doBundleInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *objectStorageURL, char *userPublicKey,
                            char *S3Policy, char *S3PolicySig, char *architecture)
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

    pParams->accountId = strdup(pInstance->accountId);  //! @TODO propagate requestor's accountId through the stack
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
    pParams->architecture = strdup(architecture);

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
        pInstance->bundleTaskProgress = 0.0;
    }
    sem_v(inst_sem);

    // do the rest in a thread
    pthread_attr_init(&tattr);
    pthread_attr_setdetachstate(&tattr, PTHREAD_CREATE_DETACHED);
    if (pthread_create(&tid, &tattr, bundling_thread, ((void *)pParams)) != 0) {
        LOGERROR("[%s] failed to start VM bundling thread\n", instanceId);
        return cleanup_bundling_task(pInstance, pParams, BUNDLING_FAILED);
    }
    set_corrid_pthread(get_corrid() != NULL ? get_corrid()->correlation_id : NULL, tid);

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
        pInstance->bundleCanceled = 0;
        pInstance->bundleBucketExists = 0;
        pInstance->bundleTaskState = NOT_BUNDLING;
        pInstance->bundleTaskProgress = 0.0;

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
    int pid = 0;
    char *pid_str = NULL;
    char run_workflow_pid_path[EUCA_MAX_PATH] = "";
    ncInstance *pInstance = NULL;

    if ((pInstance = find_instance(&global_instances, psInstanceId)) == NULL) {
        LOGERROR("[%s] instance not found\n", psInstanceId);
        return (EUCA_NOT_FOUND_ERROR);
    }
    // read pid
    snprintf(run_workflow_pid_path, sizeof(run_workflow_pid_path), "/tmp/bundle-%s/process.pid", pInstance->instanceId);
    if ((pid_str = file2strn(run_workflow_pid_path, 8)) != NULL) {
        if (strlen(pid_str) == 0) {
            EUCA_FREE(pid_str);
            return (EUCA_NOT_FOUND_ERROR);
        }

        pid = atoi(pid_str);
        pInstance->bundleCanceled = 1; // record the intent to cancel bundling so that bundling thread can abort
        if ((pid > 0) && !check_process(pid, "euca-run-workflow")) {
            LOGDEBUG("[%s] found bundlePid '%d', sending kill signal...\n", psInstanceId, pid);
            if (kill((-1) * pid, 9) != EUCA_OK) {   // kill process group
                LOGERROR("[%s] can't kill process group with pid '%d'\n", psInstanceId, (-1) * pid);
            }
        }
        EUCA_FREE(pid_str);
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
static int doMigrateInstances(struct nc_state_t *nc, ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials, char **resourceLocations, int resourceLocationsLen)
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

    unset_corrid(get_corrid());
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
    set_corrid_pthread(get_corrid() != NULL ? get_corrid()->correlation_id : NULL, tcb);
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
    set_corrid_pthread(get_corrid() != NULL ? get_corrid()->correlation_id : NULL, tcb);
    // from here on we do not need to free 'params' as the thread will do it

    if (pthread_detach(tcb)) {
        LOGERROR("[%s] failed to detach the start/stop thread\n", instanceId);
        return (EUCA_FATAL_ERROR);
    }

    return (EUCA_OK);
}

/**
 * @brief      Handles executing attach/detach based on state of interface found
 *              on instance and state in gni
 *
 * @param      pHead      pointer to head of array of ncInstances
 * @param      pInstance  ncInstance structure targeted during iteration
 * @param      pData      void pointer to globalNetworkInfo structure for context
 *
 * @return     EUCA_OK on success or proper error code.
 */
int attach_or_detach(bunchOfInstances **pHead, ncInstance *pInstance, void *pData){
    if (pInstance->state != RUNNING) {
        LOGDEBUG("[%s] skipping network interface check since instance state is not running\n", pInstance->instanceId);
        return EUCA_OK;
    } else {
        LOGDEBUG("[%s] compare local network state with gni\n", pInstance->instanceId);
    }

    globalNetworkInfo *gni = (globalNetworkInfo*) pData;
    int rc = EUCA_OK;

    // Gather all secondary network interfaces for the instance
    gni_instance * arrayOfInterfaces[EUCA_MAX_NICS] = {NULL};
    int arraySize = 0;
    rc = gni_find_secondary_interfaces(gni, pInstance->instanceId, arrayOfInterfaces, &(arraySize));
    if (rc || !arraySize) {
        LOGDEBUG("[%s] no secondary network interfaces found in gni\n", pInstance->instanceId);
    } else {
        LOGDEBUG("[%s] found %d secondary network interface(s) in gni\n", pInstance->instanceId, arraySize);
    }

    // Compute detachments for the instance
    // Outer loop - local state (secondary network interfaces)
    // Inner loop - gni (secondary network interfaces)
    // For every network interface in local state, check if the gni contains a corresponding network interface. If found do nothing, else detach
    for (int netIdx = 0; netIdx < EUCA_MAX_NICS; netIdx++){
        if (strlen(pInstance->secNetCfgs[netIdx].attachmentId) == 0 ||
                strcmp(pInstance->secNetCfgs[netIdx].stateName, VOL_STATE_DETACHED) == 0 ||
                strcmp(pInstance->secNetCfgs[netIdx].stateName, VOL_STATE_DETACHING) == 0) {
            LOGTRACE("[%s] skipping empty, detaching or detached network interface in slot %d\n", pInstance->instanceId, netIdx);
            continue;
        }

        boolean found = FALSE;
        rc = EUCA_OK;

        LOGDEBUG("[%s][%s][%s] evaluating if network interface detach is necessary in slot %d of %d\n",
                pInstance->instanceId, pInstance->secNetCfgs[netIdx].interfaceId, pInstance->secNetCfgs[netIdx].attachmentId, netIdx, (EUCA_MAX_NICS-1));
        for(int gniIdx = 0; gniIdx < arraySize; gniIdx++) {
            gni_instance *gniNetworkInterface = arrayOfInterfaces[gniIdx];

            if (gniNetworkInterface && !strncmp(gniNetworkInterface->attachmentId, pInstance->secNetCfgs[netIdx].attachmentId, ENI_ATTACHMENT_ID_LEN)) {
                LOGDEBUG("[%s][%s][%s] gni and local state contain network interface attachment, detach not required\n", gniNetworkInterface->instance_name.name, gniNetworkInterface->name, gniNetworkInterface->attachmentId)
                found = TRUE;
                arrayOfInterfaces[gniIdx] = NULL; //set to null since there shouldn't be any other action for this interface.
                break;
            }
        }

        if (!found) {
            // detach
            LOGDEBUG("[%s][%s][%s] gni does not contain network interface attachment but local state does, invoking detach\n",
                    pInstance->instanceId, pInstance->secNetCfgs[netIdx].interfaceId, pInstance->secNetCfgs[netIdx].attachmentId);
            rc = doDetachNetworkInterface(NULL, NULL, pInstance->instanceId, pInstance->secNetCfgs[netIdx].attachmentId, 1);
            if (rc) {
                LOGERROR("[%s][%s][%s] failed to detach network interface", pInstance->instanceId, pInstance->secNetCfgs[netIdx].interfaceId, pInstance->secNetCfgs[netIdx].attachmentId);
            }
        }
    }

    // Compute attachments for the instance
    // Outer loop - gni secondary network interfaces
    // Inner loop - local secondary network interfaces
    // For every network interface in gni, check if the local state contains a corresponding network interface. If found do nothing, else attach
    for (int gniIdx = 0; gniIdx < arraySize; gniIdx++) {
        gni_instance *gniNetworkInterface = arrayOfInterfaces[gniIdx];
        if (gniNetworkInterface != NULL && strcmp(gniNetworkInterface->name, pInstance->instanceId)) {
            LOGDEBUG("[%s][%s][%s] evaluating if network interface attach is necessary\n", gniNetworkInterface->instance_name.name, gniNetworkInterface->name, gniNetworkInterface->attachmentId)

            if (is_network_interface_attached(pInstance, gniNetworkInterface->name, gniNetworkInterface->attachmentId)) {
                LOGDEBUG("[%s][%s][%s] gni and local state contain network interface attachment, attach not required\n", gniNetworkInterface->instance_name.name, gniNetworkInterface->name, gniNetworkInterface->attachmentId);
                continue;
            } else {
                LOGDEBUG("[%s][%s][%s] gni contains network interface attachment but local state does not, invoking attach\n", gniNetworkInterface->instance_name.name, gniNetworkInterface->name, gniNetworkInterface->attachmentId);

                rc = EUCA_OK;
                netConfig net = {0};
                char *strmac = {0}, *strprivip = {0}, *strpubip = {0};
                hex2mac(gniNetworkInterface->macAddress, (void*) &strmac);
                strprivip = hex2dot(gniNetworkInterface->privateIp);
                strpubip = hex2dot(gniNetworkInterface->publicIp);

                rc = allocate_netConfig(&net,
                        gniNetworkInterface->name,
                        gniNetworkInterface->deviceidx,
                        strmac,
                        strprivip,
                        strpubip,
                        gniNetworkInterface->attachmentId,
                        -1, -1);

                EUCA_FREE(strmac);
                EUCA_FREE(strprivip);
                EUCA_FREE(strpubip);

                if (rc) {
                    LOGERROR("[%s] failed to allocate netConfig structure for interface %s\n", gniNetworkInterface->instance_name.name, gniNetworkInterface->name);
                    continue;
                }

                rc = doAttachNetworkInterface(NULL, NULL, pInstance->instanceId, &net);
                if (rc) {
                    LOGERROR("[%s][%s][%s] failed to attach network interface", gniNetworkInterface->instance_name.name, gniNetworkInterface->name, gniNetworkInterface->attachmentId);
                }
            }
        } else {
            LOGDEBUG("interface at index %d, this network interface may have been evaluated\n", gniIdx);
        }
    }

    return EUCA_OK;
}

/**
 * @brief      Handles loading gni and iterating instances
 *
 * @param[in]  gni_path  constant string representing the file path to load gni xml from
 *
 * @return     EUCA_OK on success or proper error code.
 */
int find_interface_changes(char *gni_path) {
    globalNetworkInfo *gni = NULL;
    int rc = EUCA_OK;
    static char lastVersion[32] = "";


    if (!global_instances) return EUCA_OK;

    gni = gni_init();

    if (gni) {
        rc = gni_populate(gni, NULL, gni_path);

        LOGDEBUG("lastVersion %s from gni\n", lastVersion);

        if (strcmp(gni->version, gni->appliedVersion)){
            LOGDEBUG("skipping detection of interfaces changes until gni version and appliedversion match %s %s\n", gni->version, gni->appliedVersion);
            if(gni) gni_free(gni);
            return EUCA_OK;
        }

        if (!strcmp(gni->appliedVersion, lastVersion)){
            LOGDEBUG("skipping detection of interfaces changes as gni appliedVersion and lastVersion match %s %s\n", gni->appliedVersion, lastVersion);
            if(gni) gni_free(gni);
            return EUCA_OK;
        }

        LOGDEBUG("calling foreach instance for %d global_instances\n", global_instances->count);
        bunchOfInstances *localInstances = global_instances;

        rc = for_each_instance(&localInstances, &attach_or_detach, (void*) gni);
        LOGDEBUG("completed detection of interface changes in gni (%d)\n", rc);

        if(!rc){
            LOGDEBUG("Updating lastVersion %s with appliedVersion %s\n", lastVersion, gni->appliedVersion);
            euca_strncpy(lastVersion, gni->appliedVersion, GNI_VERSION_LEN);
        }
    }

    rc = gni_free(gni);

    return rc;
}
