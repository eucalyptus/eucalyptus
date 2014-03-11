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
//! @file node/handlers.c
//! This implements the default operations handlers supported by all hypervisor.
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS      64      //!< so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU                      /* strnlen */
#include <string.h>                    /* strlen, strcpy */
#include <time.h>
#include <limits.h>                    /* INT_MAX */
#include <sys/unistd.h>
#include <sys/types.h>                 /* fork */
#include <sys/wait.h>                  /* waitpid */
#include <unistd.h>
#include <fcntl.h>
#include <assert.h>
#include <sys/errno.h>
#include <sys/stat.h>
#include <pthread.h>
#ifndef __DARWIN_UNIX03
#include <sys/vfs.h>                   /* statfs */
#endif /* ! __DARWIN_UNIX03 */
#include <signal.h>                    /* SIGINT */
#include <linux/limits.h>
#include <pwd.h>                       /* getpwuid_r */
#include <netdb.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <eucalyptus.h>
#include <eucalyptus-config.h>
#include <ipc.h>
#include <misc.h>
#include <backing.h>
#include <diskutil.h>
#include <euca_auth.h>
#include <euca_axis.h>

#include <vbr.h>
#include <iscsi.h>
#include <config.h>
#include <fault.h>
#include <log.h>
#include <euca_string.h>

#define HANDLERS_FANOUT
#include "handlers.h"
#include "xml.h"
#include "hooks.h"
#include <ebs_utils.h>
#include "objectstorage.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MONITORING_PERIOD                           (5) //!< Instance state transition monitoring period in seconds.
#define MAX_CREATE_TRYS                              5
#define CREATE_TIMEOUT_SEC                           60
#define LIBVIRT_TIMEOUT_SEC                          5
#define PER_INSTANCE_BUFFER_MB                       20 //!< by default reserve this much extra room (in MB) per instance (for kernel, ramdisk, and metadata overhead)
#define MAX_SENSOR_RESOURCES                         MAXINSTANCES_PER_NC
#define SEC_PER_MB                                   ((1024 * 1024) / 512)

#define MIN_BLOBSTORE_SIZE_MB                        10 //!< even with boot-from-EBS one will need work space for kernel and ramdisk
#define FS_BUFFER_PERCENT                            0.03   //!< leave 3% extra when deciding on blobstore sizes automatically
#define WORK_BS_PERCENT                              0.33   //!< give a third of available space to work, the rest to cache

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

// declarations of available handlers
extern struct handlers xen_libvirt_handlers;
extern struct handlers kvm_libvirt_handlers;
extern struct handlers default_libvirt_handlers;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifndef NO_COMP
const char *euca_this_component_name = "nc";    //!< Name of this component
const char *euca_client_component_name = "cc";  //!< Name of this component's client
#endif /* NO_COMP */

/* used by lower level handlers */

sem *hyp_sem = NULL;                   //!< semaphore for serializing domain creation
sem *inst_sem = NULL;                  //!< guarding access to global instance structs
sem *inst_copy_sem = NULL;             //!< guarding access to global instance structs
sem *addkey_sem = NULL;                //!< guarding access to global instance structs
sem *loop_sem = NULL;                  //!< created in diskutils.c for serializing 'losetup' invocations
sem *log_sem = NULL;                   //!< used by log.c
sem *service_state_sem = NULL;         //!< Used to guard service state updates (i.e. topology updates)

bunchOfInstances *global_instances = NULL;  //!< pointer to the instance list
bunchOfInstances *global_instances_copy = NULL; //!< pointer to the copied instance list

const int default_staging_cleanup_threshold = 60 * 60 * 2;  //!< after this many seconds any STAGING domains will be cleaned up
const int default_booting_cleanup_threshold = 60;   //!< after this many seconds any BOOTING domains will be cleaned up
const int default_bundling_cleanup_threshold = 60 * 60 * 2; //!< after this many seconds any BUNDLING domains will be cleaned up
const int default_createImage_cleanup_threshold = 60 * 60 * 2;  //!< after this many seconds any CREATEIMAGE domains will be cleaned up
const int default_teardown_state_duration = 60 * 3; //!< after this many seconds in TEARDOWN state (no resources), we'll forget about the instance
const int default_migration_ready_threshold = 60 * 15;  //!< after this many seconds ready (and waiting) to migrate, migration will terminate and roll back

struct nc_state_t nc_state = { 0 };    //!< Global NC state structure

configEntry configKeysRestartNC[] = {
    {CONFIG_ENABLE_WS_SECURITY, "Y"},
    {"EUCALYPTUS", "/"},
    {"NC_PORT", "8775"},
    {"NC_SERVICE", "axis2/services/EucalyptusNC"},
    {NULL, NULL},
};

configEntry configKeysNoRestartNC[] = {
    {"LOGLEVEL", "INFO"},
    {"LOGROLLNUMBER", "10"},
    {"LOGMAXSIZE", "104857600"},
    {"LOGPREFIX", ""},
    {"LOGFACILITY", ""},
    {NULL, NULL},
};

int incoming_migrations_in_progress = 0;
int outgoing_migrations_in_progress = 0;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifdef EUCA_COMPILE_TIMESTAMP
static char *compile_timestamp_str = EUCA_COMPILE_TIMESTAMP;
#else /* EUCA_COMPILE_TIMESTAMP */
static char *compile_timestamp_str = "";
#endif /* EUCA_COMPILE_TIMESTAMP */

//! a NULL-terminated array of available handlers
static struct handlers *available_handlers[] = {
    &default_libvirt_handlers,
    &xen_libvirt_handlers,
    &kvm_libvirt_handlers,
    NULL,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void *libvirt_thread(void *ptr);
static void refresh_instance_info(struct nc_state_t *nc, ncInstance * instance);
static void update_log_params(void);
static void nc_signal_handler(int sig);
static int init(void);
static void updateServiceStateInfo(ncMetadata * pMeta, boolean authoritative);
static void printNCServiceStateInfo(void);
static void printMsgServiceStateInfo(ncMetadata * pMeta);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! rejection of certain operations when NC is disabled
#define DISABLED_CHECK                                                             \
{                                                                                  \
    if (nc_state.is_enabled == FALSE) {                                            \
        LOGERROR("operation %s is not allowed when node is DISABLED\n", __func__); \
        return (EUCA_ERROR);                                                       \
    }                                                                              \
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Authorize (or deauthorize) migration keys on destination host.
//!
//! @param[in] options command-line options to pass to the authorization script
//! @param[in] host hostname (IP address) to authorize
//! @param[in] credentials shared secret to authorize
//! @param[in] instance pointer to instance struct for logging information (optional--can be NULL)
//! @param[in] lock_hyp_sem set to true to hold the 'lock_hyp_sem' semaphore
//!
//! @return EUCA_OK, EUCA_INVALID_ERROR, or EUCA_SYSTEM_ERROR
//!
int authorize_migration_keys(char *options, char *host, char *credentials, ncInstance * instance, boolean lock_hyp_sem)
{
    int rc = 0;
    char command[EUCA_MAX_PATH] = "";
    char *euca_base = getenv(EUCALYPTUS_ENV_VAR_NAME);
    char *instanceId = instance ? instance->instanceId : "UNSET";

    if (!options && !host && !credentials) {
        LOGERROR("[%s] called with invalid arguments: options=%s, host=%s, creds=%s\n", SP(instanceId), SP(options), SP(host), (credentials == NULL) ? "UNSET" : "present");
        return (EUCA_INVALID_ERROR);
    }

    snprintf(command, EUCA_MAX_PATH, EUCALYPTUS_AUTHORIZE_MIGRATION_KEYS, NP(euca_base), NP(euca_base));
    LOGDEBUG("[%s] migration key authorization command: '%s %s %s %s'\n", SP(instanceId), command, NP(options), NP(host), NP(credentials));
    if (lock_hyp_sem == TRUE) {
        sem_p(hyp_sem);
    }

    rc = euca_execlp(NULL, command, NP(options), NP(host), NP(credentials), NULL);

    if (lock_hyp_sem == TRUE) {
        sem_v(hyp_sem);
    }

    if (rc != EUCA_OK) {
        LOGERROR("[%s] '%s %s %s %s' failed. rc=%d\n", SP(instanceId), command, NP(options), NP(host), NP(credentials), rc);
        return (EUCA_SYSTEM_ERROR);
    } else {
        LOGDEBUG("[%s] migration key authorization/deauthorization succeeded\n", SP(instanceId));
    }
    return (EUCA_OK);
}

//!
//! Copies the url string of the ENABLED service of the requested type into dest_buffer.
//! dest_buffer MUST be the same size as the services uri array length, 512.
//!
//! @param[in] service_type
//! @param[in] nc
//! @param[in] dest_buffer
//! @return EUCA_OK on success, EUCA_ERROR on failure.
//! @pre
//!
//! @post
//!
int get_service_url(const char *service_type, struct nc_state_t *nc, char *dest_buffer)
{
    int i = 0;
    boolean found = FALSE;

    if (service_type == NULL || nc == NULL || dest_buffer == NULL) {
        LOGERROR("Invalid input parameters. At least one is NULL.\n");
        return (EUCA_ERROR);
    }

    sem_p(service_state_sem);

    for (i = 0; i < 16; i++) {
        if (!strcmp(service_type, nc->services[i].type)) {
            //Winner!
            if (nc->services[i].urisLen > 0) {
                euca_strncpy(dest_buffer, nc->services[i].uris[0], 512);
                found = TRUE;
            }
        }
    }
    sem_v(service_state_sem);

    if (found) {
        LOGTRACE("Found enabled service URI for service type %s as %s\n", service_type, dest_buffer);
        return (EUCA_OK);
    }

    dest_buffer[0] = '\0';             //Ensure 0 length string
    LOGTRACE("No enabled service found for service type %s\n", service_type);
    return (EUCA_ERROR);
}

//!
//!
//!
//! @pre
//!
//! @post
//!
static void printNCServiceStateInfo(void)
{
    int i = 0;
    //Don't bother if not at trace logging
    if (log_level_get() <= EUCA_LOG_TRACE) {
        sem_p(service_state_sem);
        LOGTRACE("Printing %d services\n", nc_state.servicesLen);
        LOGTRACE("Epoch %d\n", nc_state.ncStatus.localEpoch);
        for (i = 0; i < nc_state.servicesLen; i++) {
            LOGTRACE("Service - %s %s %s %s\n", nc_state.services[i].name, nc_state.services[i].partition, nc_state.services[i].type, nc_state.services[i].uris[0]);
        }
        for (i = 0; i < nc_state.disabledServicesLen; i++) {
            LOGTRACE("Disabled Service - %s %s %s %s\n", nc_state.disabledServices[i].name, nc_state.disabledServices[i].partition, nc_state.disabledServices[i].type,
                     nc_state.disabledServices[i].uris[0]);
        }
        for (i = 0; i < nc_state.servicesLen; i++) {
            LOGTRACE("Notready Service - %s %s %s %s\n", nc_state.notreadyServices[i].name, nc_state.notreadyServices[i].partition, nc_state.notreadyServices[i].type,
                     nc_state.notreadyServices[i].uris[0]);
        }
        sem_v(service_state_sem);
    }
}

//!
//!
//!
//! @param[in] pMeta
//!
//! @pre
//!
//! @post
//!
static void printMsgServiceStateInfo(ncMetadata * pMeta)
{
    int i = 0;
    //Don't bother if not at trace logging
    if (log_level_get() <= EUCA_LOG_TRACE) {
        LOGTRACE("Printing %d services\n", pMeta->servicesLen);
        LOGTRACE("Msg-Meta epoch %d\n", pMeta->epoch);

        for (i = 0; i < pMeta->servicesLen; i++) {
            LOGTRACE("Msg-Meta: Service - %s %s %s %s\n", pMeta->services[i].name, pMeta->services[i].partition, pMeta->services[i].type, pMeta->services[i].uris[0]);
        }

        for (i = 0; i < pMeta->disabledServicesLen; i++) {
            LOGTRACE("Msg-Meta: Disabled Service - %s %s %s %s\n", pMeta->disabledServices[i].name, pMeta->disabledServices[i].partition, pMeta->disabledServices[i].type,
                     pMeta->disabledServices[i].uris[0]);
        }

        for (i = 0; i < pMeta->servicesLen; i++) {
            LOGTRACE("Msg-Meta: Notready Service - %s %s %s %s\n", pMeta->notreadyServices[i].name, pMeta->notreadyServices[i].partition, pMeta->notreadyServices[i].type,
                     pMeta->notreadyServices[i].uris[0]);
        }
    }
}

//!
//! Update the state of the services and topology as received from the CC
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] authoritative indicates whether this request is allowed to reset epoch
//! @pre
//!
//! @note
//!
static void updateServiceStateInfo(ncMetadata * pMeta, boolean authoritative)
{
    int i = 0;
    char scURL[512];
    if ((pMeta != NULL) && (pMeta->servicesLen > 0)) {
        LOGTRACE("Updating NC's topology/service state info: pMeta: userId=%s correlationId=%s\n", pMeta->userId, pMeta->correlationId);

        // store information from CLC that needs to be kept up-to-date in the NC
        sem_p(service_state_sem);

        if (pMeta->epoch >= nc_state.ncStatus.localEpoch || // we have updates ('=' is there in case CC does not bump epoch numbers)
            authoritative              // trust the authoritative requests and always take their services info, even if epoch goes backward
            ) {
            //Update the epoch first
            nc_state.ncStatus.localEpoch = pMeta->epoch;

            //Copy new services info wholesale
            memcpy(nc_state.services, pMeta->services, sizeof(serviceInfoType) * 16);
            memcpy(nc_state.disabledServices, pMeta->disabledServices, sizeof(serviceInfoType) * 16);
            memcpy(nc_state.notreadyServices, pMeta->notreadyServices, sizeof(serviceInfoType) * 16);
            nc_state.servicesLen = pMeta->servicesLen;
            nc_state.disabledServicesLen = pMeta->disabledServicesLen;
            nc_state.notreadyServicesLen = pMeta->notreadyServicesLen;

            //Make a copy of the SC url to use outside of the semaphore
            for (i = 0; i < nc_state.servicesLen; i++) {
                if (!strcmp(nc_state.services[i].type, "storage")) {
                    if (nc_state.services[i].urisLen > 0) {
                        memcpy(scURL, nc_state.services[i].uris[0], 512);
                        break;
                    }
                }
            }
        }
        sem_v(service_state_sem);

        LOGTRACE("Updating VBR localhost config sc url to: %s\n", scURL);
        //Push the change to the vbr code
        vbr_update_hostconfig_scurl(scURL);

    } else {
        LOGTRACE("Cannot update service infos, null found\n");
        return;
    }

    //Log the results...
    printNCServiceStateInfo();
    printMsgServiceStateInfo(pMeta);
}

//!
//! Utilitarian functions used in the lower level handlers. This scans the string buffer
//! 's' for a matching parameter 'name' to fill in the 'valp' value.
//!
//! @param[in]  s a non NULL string buffer
//! @param[in]  name the name of the parameter we're looking for
//! @param[out] valp a pointer to the integer returned if we found the parameter in 's'
//!
//! @return EUCA_OK on success; EUCA_ERROR if any parameters are invalid; or EUCA_NO_FOUND_ERROR
//!         if the 'name' parameter is not found in 's'. In any error case, 'valp' will remain
//!         invalid and could be modified.
//!
int get_value(char *s, const char *name, long long *valp)
{
    char buf[CHAR_BUFFER_SIZE] = "";

    if ((s == NULL) || (name == NULL) || (valp == NULL))
        return (EUCA_ERROR);
    snprintf(buf, CHAR_BUFFER_SIZE, "%s=%%lld", name);
    return ((euca_lscanf(s, buf, valp) == 1) ? EUCA_OK : EUCA_NOT_FOUND_ERROR);
}

//!
//! Handles the logging of libvirt errors
//!
//! @param[in] userData (UNUSED)
//! @param[in] error a pointer to the libvirt error information
//!
void libvirt_err_handler(void *userData, virErrorPtr error)
{
    int log_level = EUCA_LOG_ERROR;

    if (error == NULL) {
        LOGERROR("libvirt error handler was given a NULL pointer\n");
    } else {
        if (error->code == VIR_ERR_NO_DOMAIN) {
            // report "domain not found" errors as warnings, since they are expected when instance is being terminated
            log_level = EUCA_LOG_WARN;
        }

        EUCALOG(log_level, "libvirt: %s (code=%d)\n", error->message, error->code);
    }
}

//!
//! sets localDevReal (assummed to be at least 32 bytes long) to the filename portion
//! of the device path (e.g., "sda" of "/dev/sda") also, if non-NULL, sets localDevTag
//! to (e.g., "unknown,requested:/dev/sda")
//!
//! @param[in]  localDev the local device path string (e.g. /dev/sda)
//! @param[out] localDevReal the device file name portion of the device path (e.g. sda)
//! @param[out] localDevTag the full device tag string (e.g. unknown,requested:/dev/sda)
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @todo chuck make sure localDevTag passed is at least 256 char
//!
int convert_dev_names(char *localDev, char *localDevReal, char *localDevTag)
{
    printf("Got: %s, %s, %s\n", localDev, localDevReal, localDevTag);
    bzero(localDevReal, 32);
    if (strchr(localDev, '/') != NULL) {
        //Path-style device.../dev/xxx
        if (strncmp(localDev, "unknown,requested:/dev/", sizeof("unknown,requested:/dev/") - 1) == 0) {
            //The case on migration, where attachment on source was done
            sscanf(localDev, "unknown,requested:/dev/%s", localDevReal);
            snprintf(localDev, strlen(localDev), "/dev/%s", localDevReal);
        } else {
            sscanf(localDev, "/dev/%s", localDevReal);
        }
    } else {
        //No /dev/' prefix, just xxx
        if (strncmp(localDev, "unknown,requested:", sizeof("unknown,requested:") - 1) == 0) {
            //The case on migration, where attachment on source was done
            sscanf(localDev, "unknown,requested:%s", localDevReal);
            snprintf(localDev, strlen(localDev), "%s", localDevReal);
        }
        snprintf(localDevReal, 32, "%s", localDev);
    }

    if (localDevReal[0] == 0) {
        printf("bad input parameter for localDev (should be /dev/XXX): '%s'\n", localDev);
        return (EUCA_ERROR);
    }

    if (localDevTag) {
        //If localDev already has the unknown,requested...just copy it
        if (strncmp(localDev, "unknown,requested:", sizeof("unknown,requested:") - 1) == 0) {
            bzero(localDevTag, 256);
            snprintf(localDevTag, 256, "%s", localDev);
        } else {
            bzero(localDevTag, 256);
            snprintf(localDevTag, 256, "unknown,requested:%s", localDev);
        }
    }
    return EUCA_OK;
}

//!
//! This updates the 'aliases' of sensor 'dimensions' that store sensor data for specific
//! block devices. Dimensions are strings like 'root', 'ephemeral0', 'vol-XYZ', etc. The
//! purpose of aliases is to map block device statistics returned by getstats.pl script,
//! which use guest block device names, such as 'sda' or 'vdb', into dimensions. To deduce
//! the mapping, we use .xml files that are passed to libvirt. This is somewhat awkward, but
//! it gets us the guest device actually used by the hypervisor. (The device we request may
//! be modified by XSL transforms and NC hooks.)
//!
//! @param[in] instance a pointer to the instance
//!
//! @return Always return EUCA_OK
//!
int update_disk_aliases(ncInstance * instance)
{
    int i = 0;
    int j = 0;
    char *volumeId = NULL;
    char **devs = NULL;
    char lpath[EUCA_MAX_PATH] = "";
    boolean saw_ephemeral0 = FALSE;
    boolean saw_root = FALSE;
    ncVolume *volume = NULL;

    // update block devices from instance XML file
    if ((devs = get_xpath_content(instance->libvirtFilePath, "/domain/devices/disk/target[@dev]/@dev")) != NULL) {
        for (i = 0; devs[i]; i++) {
            volumeId = NULL;
            if (strstr(devs[i], "da1")) {   // regexp: [hsvx]v?da1?
                volumeId = "root";
                saw_root = TRUE;
            } else if (strstr(devs[i], "da2")) {
                if (saw_ephemeral0) {
                    LOGERROR("[%s] unexpected disk layout in instance", instance->instanceId);
                } else {
                    volumeId = "ephemeral0";
                    saw_ephemeral0 = TRUE;
                }
            } else if (strstr(devs[i], "da")) {
                volumeId = "root";
                saw_root = TRUE;
            } else if (strstr(devs[i], "db")) {
                if (saw_ephemeral0) {
                    LOGERROR("[%s] unexpected disk layout in instance", instance->instanceId);
                } else {
                    volumeId = "ephemeral0";
                    saw_ephemeral0 = TRUE;
                }
            } else if (strstr(devs[i], "dc")) {
                volumeId = "ephemeral1";
            } else if (strstr(devs[i], "dd")) {
                volumeId = "ephemeral2";
            } else if (strstr(devs[i], "de")) {
                volumeId = "ephemeral3";
            }

            if (volumeId) {
                ebs_volume_data *vol_data = NULL;

                if (strcmp("root", volumeId) == 0) {
                    if (instance->params.root->locationType == NC_LOCATION_SC) {
                        if (deserialize_volume(instance->params.root->resourceLocation, &vol_data) == 0) {
                            volumeId = vol_data->volumeId;
                        }
                    }
                }
                sensor_set_volume(instance->instanceId, volumeId, devs[i]);

                EUCA_FREE(vol_data);
            }
            EUCA_FREE(devs[i]);
        }
        EUCA_FREE(devs);
    }

    if (!saw_root) {
        LOGWARN("[%s] failed to find 'dev' entry for root\n", instance->instanceId);
    }
    // now update attached or detached volumes, if any
    for (i = 0; i < EUCA_MAX_VOLUMES; ++i) {
        volume = &instance->volumes[i];
        if (strlen(volume->volumeId) == 0)
            continue;

        snprintf(lpath, sizeof(lpath), EUCALYPTUS_VOLUME_LIBVIRT_XML_PATH_FORMAT, instance->instancePath, volume->volumeId);    // vol-XXX-libvirt.xml
        if ((devs = get_xpath_content(lpath, "/disk/target[@dev]/@dev")) != NULL) {
            if (devs[0] && devs[1] == NULL) {
                sensor_set_volume(instance->instanceId, volume->volumeId, devs[0]);
            } else {
                LOGWARN("[%s] failed to find 'dev' entry in %s\n", lpath, instance->instanceId);
            }

            for (j = 0; devs[j]; j++) {
                EUCA_FREE(devs[j]);
            }
            EUCA_FREE(devs);
        } else {
            sensor_set_volume(instance->instanceId, volume->volumeId, NULL);
        }
    }

    return EUCA_OK;
}

//!
//! Logs the currently running domains
//!
void print_running_domains(void)
{
    ncInstance *instance = NULL;
    bunchOfInstances *head = NULL;
    char buf[CHAR_BUFFER_SIZE] = "";

    sem_p(inst_sem);
    {
        for (head = global_instances; head; head = head->next) {
            instance = head->instance;
            if (instance->state == STAGING || instance->state == BOOTING || instance->state == RUNNING || instance->state == BLOCKED || instance->state == PAUSED) {
                strcat(buf, " ");
                strcat(buf, instance->instanceId);
            }
        }
    }
    sem_v(inst_sem);
    LOGINFO("currently running/booting: %s\n", buf);
}

//!
//!
//!
//! @param[in] ptr
//!
static void *libvirt_thread(void *ptr)
{
    int rc = 0;
    sigset_t mask = { {0} };

    // allow SIGUSR1 signal to be delivered to this thread and its children
    sigemptyset(&mask);
    sigaddset(&mask, SIGUSR1);
    sigprocmask(SIG_UNBLOCK, &mask, NULL);

    if (nc_state.conn) {
        if ((rc = virConnectClose(nc_state.conn)) != 0) {
            LOGDEBUG("refcount on close was non-zero: %d\n", rc);
        }
    }
    nc_state.conn = virConnectOpen(nc_state.uri);
    return (NULL);
}

//!
//! Checks and reset the hypervisor connection.
//!
//! @return a pointer to the hypervisor connection structure or NULL if we failed.
//!
virConnectPtr lock_hypervisor_conn()
{
    int rc = 0;
    int status = 0;
    pid_t cpid = 0;
    pthread_t thread = { 0 };
    long long thread_par = 0L;
    boolean bail = FALSE;
    struct timespec ts = { 0 };
    virConnectPtr tmp_conn = NULL;

    // Acquire our hypervisor semaphore
    sem_p(hyp_sem);

    if (call_hooks(NC_EVENT_PRE_HYP_CHECK, nc_state.home)) {
        LOGFATAL("hooks prevented check on the hypervisor\n");
        sem_v(hyp_sem);
        return NULL;
    }
    // Fork off a process just to open and immediately close a libvirt connection.
    // The purpose is to try to identify periods when open or close calls block indefinitely.
    // Success in the child process does not guarantee success in the parent process, but
    // hopefully it will flag certain bad conditions and will allow the parent to avoid them.

    if ((cpid = fork()) < 0) {         // fork error
        LOGERROR("failed to fork to check hypervisor connection\n");
        bail = TRUE;                   // we are in big trouble if we cannot fork
    } else if (cpid == 0) {            // child process - checks on the connection
        if ((tmp_conn = virConnectOpen(nc_state.uri)) == NULL)
            exit(1);
        virConnectClose(tmp_conn);
        exit(0);
    } else {                           // parent process - waits for the child, kills it if necessary
        if ((rc = timewait(cpid, &status, LIBVIRT_TIMEOUT_SEC)) < 0) {
            LOGERROR("failed to wait for forked process: %s\n", strerror(errno));
            bail = TRUE;
        } else if (rc == 0) {
            LOGERROR("timed out waiting for hypervisor checker pid=%d\n", cpid);
            bail = TRUE;
        } else if (WEXITSTATUS(status) != 0) {
            LOGERROR("child process failed to connect to hypervisor\n");
            bail = TRUE;
        }
        // terminate the child, if any
        killwait(cpid);
    }

    if (bail) {
        sem_v(hyp_sem);
        return NULL;                   // better fail the operation than block the whole NC
    }
    LOGTRACE("process check for libvirt succeeded\n");

    // At this point, the check for libvirt done in a separate process was
    // successful, so we proceed to close and reopen the connection in a
    // separate thread, which we will try to wake up with SIGUSR1 if it
    // blocks for too long (as a last-resource effort). The reason we reset
    // the connection so often is because libvirt operations have a
    // tendency to block indefinitely if we do not do this.

    if (pthread_create(&thread, NULL, libvirt_thread, (void *)&thread_par) != 0) {
        LOGERROR("failed to create the libvirt refreshing thread\n");
        bail = TRUE;
    } else {
        for (;;) {
            if (clock_gettime(CLOCK_REALTIME, &ts) == -1) {
                LOGERROR("failed to obtain time\n");
                bail = TRUE;
                break;
            }

            ts.tv_sec += LIBVIRT_TIMEOUT_SEC;
            if ((rc = pthread_timedjoin_np(thread, NULL, &ts)) == 0)
                break;                 // all is well

            if (rc != ETIMEDOUT) {     // error other than timeout
                LOGERROR("failed to wait for libvirt refreshing thread (rc=%d)\n", rc);
                bail = TRUE;
                break;
            }

            LOGERROR("timed out on libvirt refreshing thread\n");
            pthread_kill(thread, SIGUSR1);
            sleep(1);
        }
    }

    if (bail) {
        sem_v(hyp_sem);
        return NULL;
    }
    LOGTRACE("thread check for libvirt succeeded\n");

    if (nc_state.conn == NULL) {
        LOGERROR("failed to connect to %s\n", nc_state.uri);
        sem_v(hyp_sem);
        return NULL;
    }
    return nc_state.conn;
}

//!
//! Closes the connection with the hypervisor
//!
void unlock_hypervisor_conn()
{
    sem_v(hyp_sem);
}

//!
//! Instance state state machine.
//!
//! @param[in] instance a pointer to the instance to modify
//! @param[in] state the new instance state
//!
void change_state(ncInstance * instance, instance_states state)
{
    int old_state = instance->state;

    instance->state = ((int)state);
    switch (state) {                   /* mapping from NC's internal states into external ones */
    case STAGING:
    case CANCELED:
        instance->stateCode = PENDING;
        break;
    case BOOTING:
    case RUNNING:
    case BLOCKED:
    case PAUSED:
        instance->stateCode = EXTANT;
        instance->retries = LIBVIRT_QUERY_RETRIES;
        break;
    case CRASHED:
    case BUNDLING_SHUTDOWN:
    case BUNDLING_SHUTOFF:
    case CREATEIMAGE_SHUTDOWN:
    case CREATEIMAGE_SHUTOFF:
    case SHUTDOWN:
    case SHUTOFF:
        if (instance->stateCode != EXTANT) {
            instance->stateCode = PENDING;
        }
        instance->retries = LIBVIRT_QUERY_RETRIES;
        break;
    case TEARDOWN:
        instance->stateCode = TEARDOWN;
        break;
    default:
        LOGERROR("[%s] unexpected state (%d)\n", instance->instanceId, instance->state);
        return;
    }

    euca_strncpy(instance->stateName, instance_state_names[instance->stateCode], CHAR_BUFFER_SIZE);
    if (old_state != state) {
        LOGDEBUG("[%s] state change for instance: %s -> %s (%s)\n",
                 instance->instanceId, instance_state_names[old_state], instance_state_names[instance->state], instance_state_names[instance->stateCode]);
    }
}

//!
//! waits indefinitely until a state transition takes place  (timeouts are implemented in the
//! monitoring thread) and returns 0 if from_state->to_state transition takes place and 1 otherwise
//!
//! @param[in] instance a pointer to the instance we're monitoring
//! @param[in] from_state the starting state of the transition
//! @param[in] to_state the ending state of the transition
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int wait_state_transition(ncInstance * instance, instance_states from_state, instance_states to_state)
{
    instance_states current_state = NO_STATE;

    while (1) {
        current_state = instance->state;
        if (current_state == to_state)
            return (EUCA_OK);

        if (current_state != from_state)
            return (EUCA_ERROR);

        // no point in checking more frequently
        sleep(MONITORING_PERIOD);
    }
    return (EUCA_ERROR);
}

//!
//! Refresh instance information.
//!
//! (This is called while holding inst_sem.)
//!
//! @param[in] nc a pointer to the global NC state structure.
//! @param[in] instance a pointer to the instance being refreshed
//!
static void refresh_instance_info(struct nc_state_t *nc, ncInstance * instance)
{
    int error = 0;
    int rc = 0;
    char *ip = NULL;
    virDomainInfo info = { 0 };
    instance_states new_state = NO_STATE;
    instance_states old_state = instance->state;

    // no need to bug for domains without state on Hypervisor
    if (old_state == TEARDOWN || old_state == STAGING || old_state == BUNDLING_SHUTOFF || old_state == CREATEIMAGE_SHUTOFF)
        return;

    {                                  // all this is done while holding the hypervisor lock, with a valid connection
        virConnectPtr conn = lock_hypervisor_conn();
        if (conn == NULL)
            return;

        virDomainPtr dom = virDomainLookupByName(conn, instance->instanceId);

        if (dom == NULL) {             // hypervisor doesn't know about it
            if (old_state == BUNDLING_SHUTDOWN) {
                LOGINFO("[%s] detected disappearance of bundled domain\n", instance->instanceId);
                change_state(instance, BUNDLING_SHUTOFF);
            } else if (old_state == CREATEIMAGE_SHUTDOWN) {
                LOGINFO("[%s] detected disappearance of createImage domain\n", instance->instanceId);
                change_state(instance, CREATEIMAGE_SHUTOFF);
            } else if (old_state == RUNNING || old_state == BLOCKED || old_state == PAUSED || old_state == SHUTDOWN) {
                // If we just finished migration, then this is normal.
                //
                // Could this be a bad assumption if the
                // virDomainLookupByName() call above returns NULL for
                // some transient reason rather than because hypervisor
                // doesn't know of the domain any more?
                if (is_migration_src(instance)) {
                    if (instance->migration_state == MIGRATION_IN_PROGRESS) {
                        // This usually occurs when there has been some
                        // glitch in the migration: an i/o error or
                        // reset connction.  When that happens, we do
                        // *not* want to shut off the instance!
                        //
                        // It can also happen absent an anomaly, such as
                        // when refresh_instance_info() is called right
                        // as the migration is completing (there's a race).
                        LOGDEBUG("[%s] possible migration anomaly, not yet assuming completion\n", instance->instanceId);
                        unlock_hypervisor_conn();
                        return;
                    }
                    LOGINFO("[%s] migration completed (state='%s'), cleaning up\n", instance->instanceId, migration_state_names[instance->migration_state]);
                    change_state(instance, SHUTOFF);
                    unlock_hypervisor_conn();
                    return;
                }
                // most likely the user has shut it down from the inside
                if (instance->stop_requested) {
                    LOGDEBUG("[%s] ignoring domain in stopped state\n", instance->instanceId);
                } else if (instance->retries) {
                    LOGWARN("[%s] hypervisor failed to find domain, will retry %d more time(s)\n", instance->instanceId, instance->retries);
                    instance->retries--;
                } else {
                    LOGWARN("[%s] hypervisor failed to find domain, assuming it was shut off\n", instance->instanceId);
                    change_state(instance, SHUTOFF);
                }
            }
            // else 'old_state' stays in SHUTFOFF, BOOTING, CANCELED, or CRASHED

            // set guest power state
            strncpy(instance->guestStateName, GUEST_STATE_POWERED_OFF, CHAR_BUFFER_SIZE);

            // persist state updates to disk
            save_instance_struct(instance);

            unlock_hypervisor_conn();
            return;
        }

        error = virDomainGetInfo(dom, &info);
        if ((error < 0) || (info.state == VIR_DOMAIN_NOSTATE)) {
            LOGWARN("[%s] failed to get information for domain\n", instance->instanceId);
            // what to do? hopefully we'll find out more later
            virDomainFree(dom);
            unlock_hypervisor_conn();
            return;
        }

        new_state = info.state;
        switch (old_state) {
        case BOOTING:
        case RUNNING:
        case BLOCKED:
        case PAUSED:
            // migration-related logic
            if (is_migration_dst(instance)) {
                if (old_state == BOOTING && new_state == PAUSED) {
                    incoming_migrations_in_progress++;
                    LOGINFO("[%s] incoming (%s < %s) migration in progress (1 of %d)\n", instance->instanceId, instance->migration_dst, instance->migration_src,
                            incoming_migrations_in_progress);
                    instance->migration_state = MIGRATION_IN_PROGRESS;
                    LOGDEBUG("[%s] incoming (%s < %s) migration_state set to '%s'\n", instance->instanceId,
                             instance->migration_dst, instance->migration_src, migration_state_names[instance->migration_state]);

                } else if ((old_state == BOOTING || old_state == PAUSED)
                           && (new_state == RUNNING || new_state == BLOCKED)) {
                    LOGINFO("[%s] completing incoming (%s < %s) migration...\n", instance->instanceId, instance->migration_dst, instance->migration_src);
                    instance->migration_state = NOT_MIGRATING;  // done!
                    bzero(instance->migration_src, HOSTNAME_SIZE);
                    bzero(instance->migration_dst, HOSTNAME_SIZE);
                    bzero(instance->migration_credentials, CREDENTIAL_SIZE);
                    instance->migrationTime = 0;
                    save_instance_struct(instance);
                    // copy_intances is called upon return in monitoring_thread().
                    incoming_migrations_in_progress--;
                    LOGINFO("[%s] incoming migration complete (%d other incoming migration[s] actively in progress)\n", instance->instanceId, incoming_migrations_in_progress);
                    // If no remaining incoming or pending migrations, deauthorize all clients.
                    // TO-DO: Consolidate with similar sequence in handlers_kvm.c into a utility function?
                    if (!incoming_migrations_in_progress) {
                        int incoming_migrations_pending = 0;
                        int incoming_migrations_counted = 0;
                        LOGINFO("no remaining active incoming migrations -- checking to see if there are any pending migrations\n");
                        bunchOfInstances *head = NULL;
                        for (head = global_instances; head; head = head->next) {
                            if ((head->instance->migration_state == MIGRATION_PREPARING) || (head->instance->migration_state == MIGRATION_READY)) {
                                LOGINFO("[%s] is pending migration, migration_state='%s', deferring deauthorization of migration keys\n", head->instance->instanceId,
                                        migration_state_names[head->instance->migration_state]);
                                incoming_migrations_pending++;
                            }
                            // Belt and suspenders...
                            if ((head->instance->migration_state == MIGRATION_IN_PROGRESS) && !strcmp(nc_state.ip, head->instance->migration_dst)) {
                                LOGWARN("[%s] Possible internal bug detected: instance migration_state='%s', but incoming_migrations_in_progress=%d\n", head->instance->instanceId,
                                        migration_state_names[head->instance->migration_state], incoming_migrations_in_progress);
                                incoming_migrations_counted++;
                            }
                        }
                        if (incoming_migrations_counted != incoming_migrations_in_progress) {
                            LOGWARN("Possible internal bug detected: incoming_migrations_in_progress=%d, but %d incoming migrations counted\n", incoming_migrations_in_progress,
                                    incoming_migrations_counted);
                        }
                        if (!incoming_migrations_pending) {
                            LOGINFO("no remaining incoming or pending migrations -- deauthorizing all migration client keys\n");
                            authorize_migration_keys("-D -r", NULL, NULL, NULL, FALSE);
                        }
                    } else {
                        // Verify that our count of incoming_migrations_in_progress matches our version of reality.
                        bunchOfInstances *head = NULL;
                        int incoming_migrations_counted = 0;
                        for (head = global_instances; head; head = head->next) {
                            if ((head->instance->migration_state == MIGRATION_IN_PROGRESS) && !strcmp(nc_state.ip, head->instance->migration_dst)) {
                                incoming_migrations_counted++;
                            }
                        }
                        if (incoming_migrations_counted != incoming_migrations_in_progress) {
                            LOGWARN("Possible internal bug detected: incoming_migrations_in_progress=%d, but %d incoming migrations counted\n", incoming_migrations_in_progress,
                                    incoming_migrations_counted);
                        }
                    }
                } else if (new_state == SHUTOFF || new_state == SHUTDOWN) {
                    // this is normal at the beginning of incoming migration, before a domain is created in PAUSED state
                    break;
                }
            }

            if (new_state == SHUTOFF || new_state == SHUTDOWN || new_state == CRASHED) {
                LOGWARN("[%s] hypervisor reported previously running domain as %s\n", instance->instanceId, instance_state_names[new_state]);
            }
            // change to state, whatever it happens to be
            change_state(instance, new_state);
            break;
        case SHUTDOWN:
        case SHUTOFF:
        case CRASHED:
            if (new_state == RUNNING || new_state == BLOCKED || new_state == PAUSED) {
                // cannot go back!
                LOGWARN("[%s] detected prodigal domain, terminating it\n", instance->instanceId);
                virDomainDestroy(dom);
            } else {
                change_state(instance, new_state);
            }
            break;
        case BUNDLING_SHUTDOWN:
        case CREATEIMAGE_SHUTDOWN:
            LOGDEBUG("[%s] hypervisor state for bundle/createImage domain is %s\n", instance->instanceId, instance_state_names[new_state]);
            break;
        default:
            LOGERROR("[%s] unexpected state (%d) in refresh\n", instance->instanceId, old_state);
        }

        virDomainFree(dom);
        unlock_hypervisor_conn();
    }

    // if instance is running, try to find out its IP address
    if (instance->state == RUNNING || instance->state == BLOCKED || instance->state == PAUSED) {
        ip = NULL;

        if (!strncmp(instance->ncnet.publicIp, "0.0.0.0", IP_BUFFER_SIZE)) {
            if (!strcmp(nc_state.vnetconfig->mode, "SYSTEM") || !strcmp(nc_state.vnetconfig->mode, "STATIC")) {
                rc = mac2ip(nc_state.vnetconfig, instance->ncnet.privateMac, &ip);
                if (!rc && ip) {
                    LOGINFO("[%s] discovered public IP %s for instance\n", instance->instanceId, ip);
                    euca_strncpy(instance->ncnet.publicIp, ip, IP_BUFFER_SIZE);
                    EUCA_FREE(ip);
                }
            }
        }

        if (!strncmp(instance->ncnet.privateIp, "0.0.0.0", IP_BUFFER_SIZE)) {
            rc = mac2ip(nc_state.vnetconfig, instance->ncnet.privateMac, &ip);
            if (!rc && ip) {
                LOGINFO("[%s] discovered private IP %s for instance\n", instance->instanceId, ip);
                euca_strncpy(instance->ncnet.privateIp, ip, IP_BUFFER_SIZE);
                EUCA_FREE(ip);
            }
        }
        // set guest power state
        strncpy(instance->guestStateName, GUEST_STATE_POWERED_ON, CHAR_BUFFER_SIZE);
    } else {
        strncpy(instance->guestStateName, GUEST_STATE_POWERED_OFF, CHAR_BUFFER_SIZE);
    }

    // persist state updates to disk
    save_instance_struct(instance);
}

//!
//! copying the linked list for use by Describe* requests
//!
void copy_instances(void)
{
    ncInstance *instance = NULL;
    ncInstance *src_instance = NULL;
    ncInstance *dst_instance = NULL;
    bunchOfInstances *head = NULL;
    bunchOfInstances *container = NULL;

    sem_p(inst_copy_sem);
    {
        // free the old linked list copy
        for (head = global_instances_copy; head;) {
            container = head;
            instance = head->instance;
            head = head->next;
            EUCA_FREE(instance);
            EUCA_FREE(container);
        }

        global_instances_copy = NULL;

        // make a fresh copy
        for (head = global_instances; head; head = head->next) {
            src_instance = head->instance;
            dst_instance = (ncInstance *) EUCA_ALLOC(1, sizeof(ncInstance));
            memcpy(dst_instance, src_instance, sizeof(ncInstance));
            add_instance(&global_instances_copy, dst_instance);
        }
    }
    sem_v(inst_copy_sem);
}

//!
//! helper that is used during initialization and by monitornig thread
//!
static void update_log_params(void)
{
    int log_level = 0;
    int log_roll_number = 0;
    long log_max_size_bytes = 0;
    char *log_prefix = NULL;
    char *log_facility = NULL;

    // read log params from config file and update in-memory configuration
    configReadLogParams(&log_level, &log_roll_number, &log_max_size_bytes, &log_prefix);

    // reconfigure the logging subsystem to use the new values, if any
    log_params_set(log_level, log_roll_number, log_max_size_bytes);
    log_prefix_set(log_prefix);
    EUCA_FREE(log_prefix);

    if ((log_facility = configFileValue("LOGFACILITY")) != NULL) {
        if (strlen(log_facility) > 0) {
            log_facility_set(log_facility, "nc");
        }
        EUCA_FREE(log_facility);
    }
}

//!
//! This defines the NC monitoring thread
//!
//! @param[in] arg a transparent pointer to the global NC state structure
//!
//! @return Always return NULL
//!
void *monitoring_thread(void *arg)
{
    int i = 0;
    int tmpint = 0;
    int left = 0;
    int cleaned_up = 0;
    int destroy_files = 0;
    u32 ipHex = 0;
    FILE *FP = NULL;
    char nfile[EUCA_MAX_PATH] = "";
    char nfilefinal[EUCA_MAX_PATH] = "";
    char URL[EUCA_MAX_PATH] = "";
    char ccHost[EUCA_MAX_PATH] = "";
    char clcHost[EUCA_MAX_PATH] = "";
    char tmpbuf[EUCA_MAX_PATH] = "";
    long long iteration = 0;
    long long work_fs_size_mb = 0;
    long long work_fs_avail_mb = 0;
    long long cache_fs_size_mb = 0;
    long long cache_fs_avail_mb = 0;
    time_t now = 0;
    struct nc_state_t *nc = NULL;
    bunchOfInstances *head = NULL;
    bunchOfInstances *vnhead = NULL;
    ncInstance *instance = NULL;
    ncInstance *vninstance = NULL;

    LOGINFO("spawning monitoring thread\n");
    if (arg == NULL) {
        LOGFATAL("internal error (NULL parameter to monitoring_thread)\n");
        return NULL;
    }

    nc = ((struct nc_state_t *)arg);

    for (iteration = 0; TRUE; iteration++) {
        now = time(NULL);

        sem_p(inst_sem);

        snprintf(nfile, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR "/local-net.stage", nc_state.home);
        snprintf(nfilefinal, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR "/local-net", nc_state.home);
        if ((FP = fopen(nfile, "w")) == NULL) {
            LOGWARN("could not open file %s for writing\n", nfile);
        } else {
            // print out latest CC and CLC IP addr to the local-net file
            URL[0] = ccHost[0] = clcHost[0] = '\0';

            for (i = 0; i < nc_state.servicesLen; i++) {
                if (!strcmp(nc_state.services[i].type, "cluster")) {
                    if (nc_state.services[i].urisLen > 0) {
                        memcpy(URL, nc_state.services[i].uris[0], 512);
                        if (strlen(URL)) {
                            if (tokenize_uri(URL, tmpbuf, ccHost, &tmpint, tmpbuf)) {
                                snprintf(ccHost, EUCA_MAX_PATH, "0.0.0.0");
                            }
                        }
                    }
                } else if (!strcmp(nc_state.services[i].type, "eucalyptus")) {
                    if (nc_state.services[i].urisLen > 0) {
                        memcpy(URL, nc_state.services[i].uris[0], 512);
                        if (strlen(URL)) {
                            if (tokenize_uri(URL, tmpbuf, clcHost, &tmpint, tmpbuf)) {
                                snprintf(clcHost, EUCA_MAX_PATH, "0.0.0.0");
                            }
                        }
                    }
                }
            }

            if (strlen(ccHost)) {
                fprintf(FP, "CCIP=%s\n", ccHost);
            }

            if (strlen(clcHost)) {
                fprintf(FP, "CLCIP=%s\n", clcHost);

                // See if we're in system mode, which in this case we have some work to do
                if (!strcmp(nc_state.vnetconfig->mode, NETMODE_SYSTEM)) {
                    ipHex = dot2hex(clcHost);

                    // don't change anything unless our cloud controller knowledge changed
                    if (ipHex != nc_state.vnetconfig->cloudIp) {
                        if (ipHex == 0) {
                            // We lost the cloud controller IP, remove the metadata redirect first then
                            // update vnetconfig second. The vnetUnsetMetadaRedirect() IP needs the old IP.
                            // We only do this if we had a cloud IP set...
                            if (nc_state.vnetconfig->cloudIp != 0) {
                                if (vnetUnsetMetadataRedirect(nc_state.vnetconfig) == EUCA_OK) {
                                    nc_state.vnetconfig->cloudIp = ipHex;
                                    LOGDEBUG("Lost cloud controller IP information.\n");

                                    // now save our config
                                    if (gen_nc_xml(&nc_state) != EUCA_OK) {
                                        LOGERROR("failed to update NC state on disk after learning new cloud IP\n");
                                    }
                                } else {
                                    LOGERROR("Failed to unset metadata redirect after CLC loss.");
                                }
                            }
                        } else {
                            LOGDEBUG("Learned new cloud controller IP %s.\n", clcHost);

                            // HA, YAY!!! Cloud controller IP changed, remove our old metadata redirect and
                            // then update the IP and re-install the rule with the new IP.
                            if (vnetUnsetMetadataRedirect(nc_state.vnetconfig) == EUCA_OK) {
                                nc_state.vnetconfig->cloudIp = ipHex;
                                vnetSetMetadataRedirect(nc_state.vnetconfig);

                                // now save our config
                                if (gen_nc_xml(&nc_state) != EUCA_OK) {
                                    LOGERROR("failed to update NC state on disk after learning new cloud IP\n");
                                }
                            } else {
                                LOGERROR("Failed to unset metadata redirect after CLC IP change.");
                            }
                        }
                    }
                }
            }
            fflush(FP);
        }

        cleaned_up = 0;
        for (head = global_instances; head; head = head->next) {
            instance = head->instance;

            // query for current state, if any
            refresh_instance_info(nc, instance);

            // time out logic for migration-ready instances
            if (!strcmp(instance->stateName, "Extant") && ((instance->migration_state == MIGRATION_READY) || (instance->migration_state == MIGRATION_PREPARING))
                && ((now - instance->migrationTime) > nc_state.migration_ready_threshold)) {
                if (instance->migrationTime) {
                    if (outgoing_migrations_in_progress) {
                        LOGINFO("[%s] has been in migration state '%s' on source for %d seconds (threshold is %d), but not rolling back due to %d ongoing outgoing migration[s]\n",
                                instance->instanceId, migration_state_names[instance->migration_state], (int)(now - instance->migrationTime), nc_state.migration_ready_threshold,
                                outgoing_migrations_in_progress);
                        continue;
                    }

                    LOGWARN("[%s] has been in migration state '%s' on source for %d seconds (threshold is %d), rolling back [%d].\n",
                            instance->instanceId, migration_state_names[instance->migration_state], (int)(now - instance->migrationTime), nc_state.migration_ready_threshold,
                            instance->migrationTime);
                    migration_rollback(instance);
                    continue;
                } else {
                    if (instance->state == BOOTING) {
                        // Assume destination node. (Is this a safe assumption?)
                        LOGDEBUG("[%s] destination node ready: instance in booting state with no migrationTime.\n", instance->instanceId);
                    } else {
                        LOGWARN("[%s] in instance state '%s' is ready to migrate but has a zero instance migrationTime.\n",
                                instance->instanceId, instance_state_names[instance->state]);
                        migration_rollback(instance);
                    }
                }
            }
            // don't touch running or canceled threads
            if (instance->state != STAGING && instance->state != BOOTING &&
                instance->state != SHUTOFF &&
                instance->state != SHUTDOWN &&
                instance->state != BUNDLING_SHUTDOWN &&
                instance->state != BUNDLING_SHUTOFF && instance->state != CREATEIMAGE_SHUTDOWN && instance->state != CREATEIMAGE_SHUTOFF && instance->state != TEARDOWN) {

                if (FP && !strcmp(instance->stateName, "Extant")) {
                    //! @TODO is this still being used?
                    //! @TODO yes! for EDGE networking
                    // have a running instance, write its information to local state file
                    fprintf(FP, "%s %s %s %d %s %s %s\n",
                            SP(instance->instanceId), SP(nc_state.vnetconfig->pubInterface), "NA", instance->ncnet.vlan, SP(instance->ncnet.privateMac),
                            SP(instance->ncnet.publicIp), SP(instance->ncnet.privateIp));
                    fflush(FP);
                }
                continue;
            }

            if (instance->state == TEARDOWN) {
                // it's been long enough, we can forget the instance
                if ((now - instance->terminationTime) > nc_state.teardown_state_duration) {
                    remove_instance(&global_instances, instance);
                    LOGINFO("[%s] forgetting about instance\n", instance->instanceId);
                    free_instance(&instance);
                    break;             // need to get out since the list changed
                }
                continue;
            }
            // time out logic for STAGING or BOOTING or BUNDLING instances
            if (instance->state == STAGING && (now - instance->launchTime) < nc_state.staging_cleanup_threshold)
                continue;              // hasn't been long enough, spare it

            if (instance->state == BOOTING && (now - instance->bootTime) < nc_state.booting_cleanup_threshold)
                continue;

            if ((instance->state == BUNDLING_SHUTDOWN || instance->state == BUNDLING_SHUTOFF)
                && (now - instance->bundlingTime) < nc_state.bundling_cleanup_threshold)
                continue;

            if ((instance->state == CREATEIMAGE_SHUTDOWN || instance->state == CREATEIMAGE_SHUTOFF)
                && (now - instance->createImageTime) < nc_state.createImage_cleanup_threshold)
                continue;

            // terminate a booting instance as a special case, though not if it's an incoming migration
            if (instance->state == BOOTING) {
                if ((instance->migration_state == MIGRATION_PREPARING) || (instance->migration_state == MIGRATION_READY)) {
                    LOGDEBUG("[%s] instance has exceeded BOOTING cleanup threshold of %d seconds, but has migration_state=%s, so not terminating\n", instance->instanceId,
                             nc_state.booting_cleanup_threshold, migration_state_names[instance->migration_state]);
                    continue;
                } else {
                    LOGDEBUG("[%s] finding and terminating BOOTING instance, which has exceeded cleanup threshold of %d seconds\n", instance->instanceId,
                             nc_state.booting_cleanup_threshold);

                    // do the shutdown in a thread
                    pthread_attr_t tattr;
                    pthread_t tid;
                    pthread_attr_init(&tattr);
                    pthread_attr_setdetachstate(&tattr, PTHREAD_CREATE_DETACHED);
                    void *param = (void *)strdup(instance->instanceId);
                    if (pthread_create(&tid, &tattr, terminating_thread, (void *)param) != 0) {
                        LOGERROR("[%s] failed to start VM termination thread\n", instance->instanceId);
                    }
                }
            }

            if (cleaned_up < nc_state.concurrent_cleanup_ops) {
                // ok, it's been condemned => destroy the files
                cleaned_up++;
                destroy_files = !nc_state.save_instance_files;
                if (call_hooks(NC_EVENT_PRE_CLEAN, instance->instancePath)) {
                    if (destroy_files) {
                        LOGERROR("[%s] cancelled instance cleanup via hooks\n", instance->instanceId);
                        destroy_files = 0;
                    }
                }

                LOGINFO("[%s] cleaning up state for instance%s\n", instance->instanceId, (destroy_files) ? ("") : (" (but keeping the files)"));
                if (destroy_instance_backing(instance, destroy_files)) {
                    LOGWARN("[%s] failed to cleanup instance state\n", instance->instanceId);
                }
                // check to see if this is the last instance running on vlan, handle local networking information drop
                left = 0;
                for (vnhead = global_instances; vnhead; vnhead = vnhead->next) {
                    vninstance = vnhead->instance;
                    if (vninstance->ncnet.vlan == (instance->ncnet).vlan && strcmp(instance->instanceId, vninstance->instanceId)) {
                        left++;
                    }
                }
                if (left == 0) {
                    LOGINFO("[%s] stopping the network (vlan=%d)\n", instance->instanceId, (instance->ncnet).vlan);
                    vnetStopNetwork(nc_state.vnetconfig, (instance->ncnet).vlan, NULL, NULL);
                }
                change_state(instance, TEARDOWN);   // TEARDOWN = no more resources
                instance->terminationTime = time(NULL);
            }
        }

        if (FP) {
            fclose(FP);
            rename(nfile, nfilefinal);
        }

        copy_instances();              // copy global_instances to global_instances_copy
        sem_v(inst_sem);

        if (head) {
            // we got out because of modified list, no need to sleep now
            continue;
        }

        sleep(MONITORING_PERIOD);

        // do this on every iteration (every MONITORING_PERIOD seconds)
        if ((iteration % 1) == 0) {
            // see if config file has changed and react to those changes
            if (isConfigModified(nc_state.configFiles, 2) > 0) {    // config modification time has changed
                if (readConfigFile(nc_state.configFiles, 2)) {
                    // something has changed that can be read in
                    LOGINFO("configuration file has been modified, ingressing new options\n");

                    // log-related options
                    update_log_params();

                    //! @todo pick up other NC options dynamically?
                }
            }
        }
        // do this every 10th iteration (every 10*MONITORING_PERIOD seconds)
        if ((iteration % 10) == 0) {
            //! @todo 3.2 change 1 to 10

            // check file system state and blobstore state
            blobstore_meta work_meta, cache_meta;
            if (stat_backing_store(NULL, &work_meta, &cache_meta) == EUCA_OK) {
                work_fs_size_mb = (long long)(work_meta.fs_bytes_size / MEGABYTE);
                work_fs_avail_mb = (long long)(work_meta.fs_bytes_available / MEGABYTE);
                cache_fs_size_mb = (long long)(cache_meta.fs_bytes_size / MEGABYTE);
                cache_fs_avail_mb = (long long)(cache_meta.fs_bytes_available / MEGABYTE);

                if (work_fs_avail_mb < ((work_fs_size_mb * DISK_TOO_LOW_PERCENT) / 100)) {
                    log_eucafault("1003", "component", euca_this_component_name, "file", work_meta.path, NULL);
                }

                if (cache_fs_size_mb > 0 && cache_fs_avail_mb < ((cache_fs_size_mb * DISK_TOO_LOW_PERCENT) / 100)) {
                    log_eucafault("1003", "component", euca_this_component_name, "file", cache_meta.path, NULL);
                }
                //! @todo add more faults (cache or work reserved exceeds available space on file system)
            }
        }
    }

    return NULL;
}

//!
//! Fills in some of the fields of instance struct
//!
//! @param[in] instance struct to fill in
//!
void set_instance_params(ncInstance * instance)
{
    char *s = NULL;

    if (nc_state.config_use_virtio_net) {
        instance->params.nicType = NIC_TYPE_VIRTIO;
    } else {
        if (strstr(instance->platform, "windows")) {
            instance->params.nicType = NIC_TYPE_WINDOWS;
        } else {
            instance->params.nicType = NIC_TYPE_LINUX;
        }
    }

    euca_strncpy(instance->hypervisorType, nc_state.H->name, sizeof(instance->hypervisorType)); // set the hypervisor type

    instance->hypervisorCapability = nc_state.capability;   // set the cap (xen/hw/hw+xen)
    if ((s = system_output("getconf LONG_BIT")) != NULL) {
        int bitness = atoi(s);
        if (bitness == 32 || bitness == 64) {
            instance->hypervisorBitness = bitness;
        } else {
            LOGWARN("[%s] can't determine the host's bitness (%s, assuming 64)\n", instance->instanceId, s);
            instance->hypervisorBitness = 64;
        }
        EUCA_FREE(s);
    } else {
        LOGWARN("[%s] can't determine the host's bitness (assuming 64)\n", instance->instanceId);
        instance->hypervisorBitness = 64;
    }
    instance->combinePartitions = nc_state.convert_to_disk;
    instance->do_inject_key = nc_state.do_inject_key;
}

//!
//! Defines the instance startup thread
//!
//! @param[in] arg a transparent pointer to the instance structure to start
//!
//! @return Always return NULL
//!
void *startup_thread(void *arg)
{
    int i = 0;
    int error = EUCA_OK;
    int status = 0;
    int rc = 0;
    char *xml = NULL;
    char *brname = NULL;
    pid_t cpid = 0;
    boolean try_killing = FALSE;
    boolean created = FALSE;
    ncInstance *instance = ((ncInstance *) arg);
    virDomainPtr dom = NULL;

    LOGDEBUG("[%s] spawning startup thread\n", instance->instanceId);
    virConnectPtr conn = lock_hypervisor_conn();
    if (conn == NULL) {
        LOGERROR("[%s] could not contact the hypervisor, abandoning the instance\n", instance->instanceId);
        goto shutoff;
    }
    unlock_hypervisor_conn();          // unlock right away, since we are just checking on it

    // set up networking
    if ((error = vnetStartNetwork(nc_state.vnetconfig, instance->ncnet.vlan, NULL, NULL, NULL, &brname)) != EUCA_OK) {
        LOGERROR("[%s] start network failed for instance, terminating it\n", instance->instanceId);
        EUCA_FREE(brname);
        goto shutoff;
    }
    euca_strncpy(instance->params.guestNicDeviceName, brname, sizeof(instance->params.guestNicDeviceName));
    EUCA_FREE(brname);

    // set parameters like hypervisor type, bitness, NIC type, key injection, etc.
    set_instance_params(instance);

    if ((error = create_instance_backing(instance, FALSE))  // do the heavy lifting on the disk
        || (error = gen_instance_xml(instance)) // create euca-specific instance XML file
        || (error = gen_libvirt_instance_xml(instance))) {  // transform euca-specific XML into libvirt XML
        LOGERROR("[%s] failed to prepare images for instance (error=%d)\n", instance->instanceId, error);
        goto shutoff;
    }

    if (instance->state == TEARDOWN) { // timed out in STAGING
        goto free;
    }

    if (instance->state == CANCELED) {
        LOGERROR("[%s] cancelled instance startup\n", instance->instanceId);
        goto shutoff;
    }

    if (call_hooks(NC_EVENT_PRE_BOOT, instance->instancePath)) {
        LOGERROR("[%s] cancelled instance startup via hooks\n", instance->instanceId);
        goto shutoff;
    }

    xml = file2str(instance->libvirtFilePath);

    save_instance_struct(instance);    // to enable NC recovery
    sensor_add_resource(instance->instanceId, "instance", instance->uuid);
    sensor_set_resource_alias(instance->instanceId, instance->ncnet.privateIp);
    update_disk_aliases(instance);

    // serialize domain creation as hypervisors can get confused with
    // too many simultaneous create requests
    LOGTRACE("[%s] instance about to boot\n", instance->instanceId);

    for (i = 0; i < MAX_CREATE_TRYS; i++) { // retry loop
        if (i > 0) {
            LOGINFO("[%s] attempt %d of %d to create the instance\n", instance->instanceId, i + 1, MAX_CREATE_TRYS);
        }

        {                              // all this is done while holding the hypervisor lock, with a valid connection
            virConnectPtr conn = lock_hypervisor_conn();
            if (conn == NULL) {        // get a new connection for each loop iteration
                LOGERROR("[%s] could not contact the hypervisor, abandoning the instance\n", instance->instanceId);
                goto shutoff;
            }

            sem_p(loop_sem);

            // We have seen virDomainCreateLinux() on occasion block indefinitely,
            // which freezes all activity on the NC since hyp_sem and loop_sem are
            // being held by the thread. (This is on Lucid with AppArmor enabled.)
            // To protect against that, we invoke the function in a process and
            // terminate it after CREATE_TIMEOUT_SEC seconds.
            //
            // #0  0x00007f359f0b1f93 in poll () from /lib/libc.so.6
            // #1  0x00007f359a9a44e2 in ?? () from /usr/lib/libvirt.so.0
            // #2  0x00007f359a9a5060 in ?? () from /usr/lib/libvirt.so.0
            // #3  0x00007f359a9ac159 in ?? () from /usr/lib/libvirt.so.0
            // #4  0x00007f359a98d65b in virDomainCreateXML () from /usr/lib/libvirt.so.0
            // #5  0x00007f359b053c8e in startup_thread (arg=0x7f358813bf40) at handlers.c:644
            // #6  0x00007f359f3619ca in start_thread () from /lib/libpthread.so.0
            // #7  0x00007f359f0be70d in clone () from /lib/libc.so.6
            // #8  0x0000000000000000 in ?? ()

            if ((cpid = fork()) < 0) { // fork error
                LOGERROR("[%s] failed to fork to start instance\n", instance->instanceId);
            } else if (cpid == 0) {    // child process - creates the domain
                if ((dom = virDomainCreateLinux(conn, xml, 0)) != NULL) {
                    virDomainFree(dom); // To be safe. Docs are not clear on whether the handle exists outside the process.
                    exit(0);
                } else {
                    exit(1);
                }
            } else {
                // parent process - waits for the child, kills it if necessary
                try_killing = FALSE;
                if ((rc = timewait(cpid, &status, CREATE_TIMEOUT_SEC)) < 0) {
                    LOGERROR("[%s] failed to wait for forked process: %s\n", instance->instanceId, strerror(errno));
                    try_killing = TRUE;
                } else if (rc == 0) {
                    LOGERROR("[%s] timed out waiting for forked process pid=%d\n", instance->instanceId, cpid);
                    try_killing = TRUE;
                } else if (WEXITSTATUS(status) != 0) {
                    LOGERROR("[%s] hypervisor failed to create the instance\n", instance->instanceId);
                } else {
                    created = TRUE;
                }

                if (try_killing) {
                    killwait(cpid);
                }
            }

            sem_v(loop_sem);
            unlock_hypervisor_conn();  // guard against libvirtd connection badness
        }

        if (created)
            break;
        sleep(1);
    }

    if (!created) {
        goto shutoff;
    }
    //! @TODO bring back correlationId
    eventlog("NC", instance->userId, "", "instanceBoot", "begin");

    {                                  // make instance state changes while under lock
        sem_p(inst_sem);
        // check one more time for cancellation
        if (instance->state == TEARDOWN) {
            // timed out in BOOTING
        } else if (instance->state == CANCELED || instance->state == SHUTOFF) {
            LOGERROR("[%s] startup of instance was cancelled\n", instance->instanceId);
            change_state(instance, SHUTOFF);
        } else {
            LOGINFO("[%s] booting\n", instance->instanceId);
            instance->bootTime = time(NULL);
            change_state(instance, BOOTING);
        }
        copy_instances();
        sem_v(inst_sem);
    }
    goto free;

shutoff:                              // escape point for error conditions
    change_state(instance, SHUTOFF);

free:
    EUCA_FREE(xml);
    EUCA_FREE(brname);
    return NULL;
}

//!
//! Defines the termination thread.
//!
//! @param[in] arg a transparent pointer to the argument passed to this thread handler
//!
//! @return Always return NULL
//!
void *terminating_thread(void *arg)
{
    char *instanceId = (char *)arg;

    LOGDEBUG("[%s] spawning terminating thread\n", instanceId);

    int err = find_and_terminate_instance(instanceId);
    if (err != EUCA_OK) {
        EUCA_FREE(arg);
        return NULL;
    }

    {
        sem_p(inst_sem);
        ncInstance *instance = find_instance(&global_instances, instanceId);
        if (instance == NULL) {
            sem_v(inst_sem);
            EUCA_FREE(arg);
            return NULL;
        }
        // change the state and let the monitoring_thread clean up state
        if (instance->state != TEARDOWN && instance->state != CANCELED) {
            // do not leave TEARDOWN (cleaned up) or CANCELED (already trying to terminate)
            if (instance->state == STAGING) {
                change_state(instance, CANCELED);
            } else {
                change_state(instance, SHUTOFF);
            }
        }
        copy_instances();
        sem_v(inst_sem);
    }

    EUCA_FREE(arg);
    return NULL;
}

//!
//! On startup, adopt instance found running on the hypervisor.
//!
void adopt_instances()
{
    int dom_ids[MAXDOMS] = { 0 };
    int num_doms = 0;
    int i = 0;
    int error = 0;
    int err = 0;
    virDomainInfo info = { 0 };
    const char *dom_name = NULL;
    ncInstance *instance = NULL;
    virDomainPtr dom = NULL;
    virConnectPtr conn = NULL;

    conn = lock_hypervisor_conn();
    if (conn == NULL)
        return;

    LOGINFO("looking for existing domains\n");
    virSetErrorFunc(NULL, libvirt_err_handler);

    num_doms = virConnectListDomains(conn, dom_ids, MAXDOMS);
    if (num_doms == 0) {
        LOGINFO("no currently running domains to adopt\n");
        unlock_hypervisor_conn();
        return;
    }
    if (num_doms < 0) {
        LOGWARN("failed to find out about running domains\n");
        unlock_hypervisor_conn();
        return;
    }
    // WARNING: be sure to call virDomainFree when necessary so as to avoid leaking the virDomainPtr
    for (i = 0; i < num_doms; i++) {
        dom = virDomainLookupByID(conn, dom_ids[i]);
        if (!dom) {
            LOGWARN("failed to lookup running domain #%d, ignoring it\n", dom_ids[i]);
            continue;
        }
        error = virDomainGetInfo(dom, &info);
        if ((error < 0) || (info.state == VIR_DOMAIN_NOSTATE)) {
            LOGWARN("failed to get info on running domain #%d, ignoring it\n", dom_ids[i]);
            virDomainFree(dom);
            continue;
        }

        if (info.state == VIR_DOMAIN_SHUTDOWN || info.state == VIR_DOMAIN_SHUTOFF || info.state == VIR_DOMAIN_CRASHED) {
            LOGDEBUG("ignoring non-running domain #%d\n", dom_ids[i]);
            virDomainFree(dom);
            continue;
        }

        if ((dom_name = virDomainGetName(dom)) == NULL) {
            LOGWARN("failed to get name of running domain #%d, ignoring it\n", dom_ids[i]);
            virDomainFree(dom);
            continue;
        }
        if (!strcmp(dom_name, "Domain-0")) {
            virDomainFree(dom);
            continue;
        }

        if ((instance = load_instance_struct(dom_name)) == NULL) {
            LOGWARN("failed to recover Eucalyptus metadata of running domain %s, ignoring it\n", dom_name);
            virDomainFree(dom);
            continue;
        }

        virDomainFree(dom);

        if (call_hooks(NC_EVENT_ADOPTING, instance->instancePath)) {
            LOGINFO("[%s] ignoring running domain due to hooks\n", instance->instanceId);
            free_instance(&instance);
            continue;
        }

        change_state(instance, info.state);
        sem_p(inst_sem);
        {
            err = add_instance(&global_instances, instance);
        }
        sem_v(inst_sem);

        if (err) {
            free_instance(&instance);
            continue;
        }

        sensor_add_resource(instance->instanceId, "instance", instance->uuid);  // ensure the sensor system monitors this instance
        sensor_set_resource_alias(instance->instanceId, instance->ncnet.privateIp);
        update_disk_aliases(instance);

        //! @TODO try to re-check IPs?
        LOGINFO("[%s] - adopted running domain from user %s\n", instance->instanceId, instance->userId);
    }
    unlock_hypervisor_conn();

    sem_p(inst_sem);
    {
        copy_instances();              // copy global_instances to global_instances_copy
    }
    sem_v(inst_sem);
}

//!
//!
//!
//! @param[in] sig
//!
static void nc_signal_handler(int sig)
{
    LOGDEBUG("signal handler caught %d\n", sig);
}

//!
//! Initialize the NC handlers
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include EUCA_ERROR,
//!         EUCA_FATAL_ERROR
//!
static int init(void)
{
#define GET_VAR_INT(_var, _name, _def)                   \
{                                                        \
	s = getConfString(nc_state.configFiles, 2, (_name)); \
	if (s) {					                         \
		(_var) = atoi(s);                                \
		EUCA_FREE(s);                                    \
	} else {                                             \
		(_var) = (_def);                                 \
	}                                                    \
}

    static int initialized = 0;
    int do_warn = 0, i;
    char logFile[EUCA_MAX_PATH] = "";
    char *bridge = NULL;
    char *s = NULL;
    char *tmp = NULL;
    char *pubinterface = NULL;
    struct stat mystat = { 0 };
    struct handlers **h = NULL;
    sigset_t mask = { {0} };
    struct sigaction act = { {0} };

    // 0 => hasn't run, -1 => failed, 1 => ok
    if (initialized > 0)
        return EUCA_OK;
    else if (initialized < 0)
        return EUCA_ERROR;

    // ensure that MAXes are zeroed out
    bzero(&nc_state, sizeof(struct nc_state_t));
    strncpy(nc_state.version, EUCA_VERSION, sizeof(nc_state.version));  // set the version
    nc_state.is_enabled = TRUE;        // NC is enabled unless disk state will say otherwise

    // configure signal handling for this thread and its children:
    // - ignore SIGALRM, which may be used in libraries we depend on
    // - deliver SIGUSR1 to a no-op signal handler, as a way to unblock 'stuck' system calls in libraries we depend on
    {
        // add SIGUSR1 & SIGALRM to the list of signals blocked by this thread and all of its children threads
        sigemptyset(&mask);
        sigaddset(&mask, SIGUSR1);
        sigaddset(&mask, SIGALRM);
        sigprocmask(SIG_BLOCK, &mask, NULL);

        // establish function nc_signal_handler() as the handler for delivery of SIGUSR1, in whatever thread
        bzero(&act, sizeof(struct sigaction));
        act.sa_handler = nc_signal_handler;
        act.sa_flags = 0;
        sigemptyset(&act.sa_mask);
        sigaction(SIGUSR1, &act, NULL);
    }

    // read in configuration - this should be first!

    // determine home ($EUCALYPTUS)
    if ((tmp = getenv(EUCALYPTUS_ENV_VAR_NAME)) == NULL) {
        nc_state.home[0] = '\0';       // empty string means '/'
        do_warn = 1;
    } else {
        strncpy(nc_state.home, tmp, EUCA_MAX_PATH - 1);
    }

    //Set the SC client policy file path
    char policyFile[EUCA_MAX_PATH];
    bzero(policyFile, EUCA_MAX_PATH);
    snprintf(policyFile, EUCA_MAX_PATH, EUCALYPTUS_KEYS_DIR "/sc-client-policy.xml", nc_state.home);
    euca_strncpy(nc_state.config_sc_policy_file, policyFile, EUCA_MAX_PATH);

    // set the minimum log for now
    snprintf(logFile, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR "/nc.log", nc_state.home);
    log_file_set(logFile);
    LOGINFO("spawning Eucalyptus node controller v%s %s\n", nc_state.version, compile_timestamp_str);
    if (do_warn)
        LOGWARN("env variable %s not set, using /\n", EUCALYPTUS_ENV_VAR_NAME);

    // search for the config file
    snprintf(nc_state.configFiles[1], EUCA_MAX_PATH, EUCALYPTUS_CONF_LOCATION, nc_state.home);
    if (stat(nc_state.configFiles[1], &mystat)) {
        LOGFATAL("could not open configuration file %s\n", nc_state.configFiles[1]);
        return (EUCA_ERROR);
    }
    snprintf(nc_state.configFiles[0], EUCA_MAX_PATH, EUCALYPTUS_CONF_OVERRIDE_LOCATION, nc_state.home);
    LOGINFO("NC is looking for configuration in %s,%s\n", nc_state.configFiles[1], nc_state.configFiles[0]);

    configInitValues(configKeysRestartNC, configKeysNoRestartNC);   // initialize config subsystem
    readConfigFile(nc_state.configFiles, 2);
    update_log_params();

    // set default in the paths. the driver will override
    nc_state.config_network_path[0] = '\0';
    nc_state.xm_cmd_path[0] = '\0';
    nc_state.virsh_cmd_path[0] = '\0';
    nc_state.get_info_cmd_path[0] = '\0';
    snprintf(nc_state.libvirt_xslt_path, EUCA_MAX_PATH, EUCALYPTUS_LIBVIRT_XSLT, nc_state.home);    // for now, this must be set before anything in xml.c is invoked
    snprintf(nc_state.rootwrap_cmd_path, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, nc_state.home);

    {                                  // determine the hypervisor to use
        char *hypervisor = getConfString(nc_state.configFiles, 2, CONFIG_HYPERVISOR);
        if (!hypervisor) {
            LOGFATAL("value %s is not set in the config file\n", CONFIG_HYPERVISOR);
            return (EUCA_FATAL_ERROR);
        }
        // let's look for the right hypervisor driver
        for (h = available_handlers; *h; h++) {
            if (!strncmp((*h)->name, "default", CHAR_BUFFER_SIZE))
                nc_state.D = *h;

            if (!strncmp((*h)->name, hypervisor, CHAR_BUFFER_SIZE))
                nc_state.H = *h;

            if (!strncmp((*h)->name, "kvm", CHAR_BUFFER_SIZE) && !strcmp(hypervisor, "qemu")) {
                nc_state.H = *h;
                strcpy(nc_state.H->name, "qemu");   // TODO: kind of a hack, to make instance->hypervisorType right
            }
        }

        if (nc_state.H == NULL) {
            LOGFATAL("requested hypervisor type (%s) is not available\n", hypervisor);
            EUCA_FREE(hypervisor);
            return (EUCA_FATAL_ERROR);
        }
        // only load virtio config for kvm
        if (!strncmp("kvm", hypervisor, CHAR_BUFFER_SIZE) || !strncmp("qemu", hypervisor, CHAR_BUFFER_SIZE) || !strncmp("KVM", hypervisor, CHAR_BUFFER_SIZE)) {
            GET_VAR_INT(nc_state.config_use_virtio_net, CONFIG_USE_VIRTIO_NET, 0);  // for now, these three Virtio settings must be set before anything in xml.c is invoked
            GET_VAR_INT(nc_state.config_use_virtio_disk, CONFIG_USE_VIRTIO_DISK, 0);
            GET_VAR_INT(nc_state.config_use_virtio_root, CONFIG_USE_VIRTIO_ROOT, 0);
        }
        EUCA_FREE(hypervisor);
    }

    {
        // load NC's state from disk, if any
        char *psCloudIp = NULL;
        struct nc_state_t nc_state_disk = { 0 };

        // allocate temporary network struct (we cannot put vnetConfig on the stack, it is large: 102MB)
        if ((nc_state_disk.vnetconfig = EUCA_ZALLOC(1, sizeof(vnetConfig))) == NULL) {
            LOGFATAL("Cannot allocate temporary vnetconfig!\n");
            return (EUCA_FATAL_ERROR);
        }
        // Allocate our network structure
        if ((nc_state.vnetconfig = EUCA_ZALLOC(1, sizeof(vnetConfig))) == NULL) {
            LOGFATAL("Cannot allocate vnetconfig!\n");
            EUCA_FREE(nc_state_disk.vnetconfig);
            return (EUCA_FATAL_ERROR);
        }

        if (read_nc_xml(&nc_state_disk) == EUCA_OK) {
            //! @TODO currently read_nc_xml() relies on nc_state.libvirt_xslt_path and virtio flags being set, which is brittle - fix init() in xml.c
            LOGINFO("loaded NC state from previous invocation\n");

            // check on the version, in case it has changed
            if (strcmp(nc_state_disk.version, nc_state.version) != 0 && nc_state_disk.version[0] != '\0') {
                LOGINFO("found state from NC v%s while starting NC v%s\n", nc_state_disk.version, nc_state.version);
                // any NC upgrade/downgrade-related code can go here
            }
            // check on the state
            if (nc_state_disk.is_enabled == FALSE) {
                LOGINFO("NC will start up as DISABLED based on disk state\n");
                nc_state.is_enabled = FALSE;
            }
            // Check the Cloud Controller IP set?
            if (nc_state_disk.vnetconfig->cloudIp != 0) {
                psCloudIp = hex2dot(nc_state_disk.vnetconfig->cloudIp);
                LOGINFO("Found cloud controller IP %s while starting NC.\n", psCloudIp);
                nc_state.vnetconfig->cloudIp = nc_state_disk.vnetconfig->cloudIp;
                EUCA_FREE(psCloudIp);
            }
        } else {                       // there is no disk state, so create it
            if (gen_nc_xml(&nc_state) != EUCA_OK) {
                LOGERROR("failed to update NC state on disk\n");
            } else {
                LOGINFO("wrote NC state to disk\n");
            }
        }

        EUCA_FREE(nc_state_disk.vnetconfig);
    }

    {
        /* Initialize libvirtd.conf, since some buggy versions of libvirt
         * require it.  At least two versions of libvirt have had this issue,
         * most recently the version in RHEL 6.1.  Note that this happens
         * at each startup of the NC mainly because the location of the
         * required file depends on the process owner's home directory, which
         * may change after the initial installation.
         */
        char libVirtConf[EUCA_MAX_PATH];
        uid_t uid = geteuid();
        struct passwd *pw;
        FILE *fd;
        struct stat lvcstat;
        pw = getpwuid(uid);
        errno = 0;
        if (pw != NULL) {
            snprintf(libVirtConf, EUCA_MAX_PATH, "%s/.libvirt/libvirtd.conf", pw->pw_dir);
            if (access(libVirtConf, R_OK) == -1 && errno == ENOENT) {
                libVirtConf[strlen(libVirtConf) - strlen("/libvirtd.conf")] = '\0';
                errno = 0;
                if (stat(libVirtConf, &lvcstat) == -1 && errno == ENOENT) {
                    mkdir(libVirtConf, 0755);
                } else if (errno) {
                    LOGINFO("Failed to stat %s/.libvirt\n", pw->pw_dir);
                }
                libVirtConf[strlen(libVirtConf)] = '/';
                errno = 0;
                fd = fopen(libVirtConf, "a");
                if (fd == NULL) {
                    LOGINFO("Failed to open %s, error code %d\n", libVirtConf, errno);
                } else {
                    fclose(fd);
                }
            } else if (errno) {
                LOGINFO("Failed to access libvirtd.conf, error code %d\n", errno);
            }
        } else {
            LOGINFO("Cannot get EUID, not creating libvirtd.conf\n");
        }
    }

    {                                  // initialize hooks if their directory looks ok
        char dir[EUCA_MAX_PATH];
        snprintf(dir, sizeof(dir), EUCALYPTUS_NC_HOOKS_DIR, nc_state.home);
        // if 'dir' does not exist, init_hooks() will silently fail,
        // and all future call_hooks() will silently succeed
        init_hooks(nc_state.home, dir);

        if (call_hooks(NC_EVENT_PRE_INIT, nc_state.home)) {
            LOGFATAL("hooks prevented initialization\n");
            return (EUCA_FATAL_ERROR);
        }
    }

    GET_VAR_INT(nc_state.config_max_mem, CONFIG_MAX_MEM, 0);
    GET_VAR_INT(nc_state.config_max_cores, CONFIG_MAX_CORES, 0);
    GET_VAR_INT(nc_state.save_instance_files, CONFIG_SAVE_INSTANCES, 0);
    GET_VAR_INT(nc_state.concurrent_disk_ops, CONFIG_CONCURRENT_DISK_OPS, 4);
    GET_VAR_INT(nc_state.concurrent_cleanup_ops, CONFIG_CONCURRENT_CLEANUP_OPS, 30);
    GET_VAR_INT(nc_state.disable_snapshots, CONFIG_DISABLE_SNAPSHOTS, 0);
    GET_VAR_INT(nc_state.shutdown_grace_period_sec, CONFIG_SHUTDOWN_GRACE_PERIOD_SEC, 60);

    int disable_injection;
    GET_VAR_INT(disable_injection, CONFIG_DISABLE_KEY_INJECTION, 0);
    nc_state.do_inject_key = !disable_injection;
    strcpy(nc_state.admin_user_id, EUCALYPTUS_ADMIN);
    GET_VAR_INT(nc_state.staging_cleanup_threshold, CONFIG_NC_STAGING_CLEANUP_THRESHOLD, default_staging_cleanup_threshold);
    GET_VAR_INT(nc_state.booting_cleanup_threshold, CONFIG_NC_BOOTING_CLEANUP_THRESHOLD, default_booting_cleanup_threshold);
    GET_VAR_INT(nc_state.bundling_cleanup_threshold, CONFIG_NC_BUNDLING_CLEANUP_THRESHOLD, default_bundling_cleanup_threshold);
    GET_VAR_INT(nc_state.createImage_cleanup_threshold, CONFIG_NC_CREATEIMAGE_CLEANUP_THRESHOLD, default_createImage_cleanup_threshold);
    GET_VAR_INT(nc_state.teardown_state_duration, CONFIG_NC_TEARDOWN_STATE_DURATION, default_teardown_state_duration);
    GET_VAR_INT(nc_state.migration_ready_threshold, CONFIG_NC_MIGRATION_READY_THRESHOLD, default_migration_ready_threshold);
    int max_attempts;
    GET_VAR_INT(max_attempts, CONFIG_WALRUS_DOWNLOAD_MAX_ATTEMPTS, -1);
    if (max_attempts > 0 && max_attempts < 99)
        objectstorage_set_max_download_attempts(max_attempts);

    // add three eucalyptus directories with executables to PATH of this process
    add_euca_to_path(nc_state.home);

    // read in .pem files
    if (euca_init_cert()) {
        LOGERROR("failed to find cryptographic certificates\n");
        return (EUCA_FATAL_ERROR);
    }
    // check on dependencies (3rd-party programs that NC invokes)
    if (diskutil_init(FALSE)) {        // NC does not need GRUB for now
        LOGFATAL("failed to find required dependencies for disk operations\n");
        return (EUCA_FATAL_ERROR);
    }
    // check on the Imaging Toolkit readyness
    if (imaging_init(nc_state.home)) {
        LOGFATAL("failed to find required dependencies for image work\n");
        return (EUCA_FATAL_ERROR);
    }

    if (sensor_init(NULL, NULL, MAX_SENSOR_RESOURCES, FALSE, NULL) != EUCA_OK) {
        LOGERROR("failed to initialize sensor subsystem in this process\n");
        return (EUCA_FATAL_ERROR);
    }
    //// from now on we have unrecoverable failure, so no point in retrying to re-init ////
    initialized = -1;

    hyp_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
    inst_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
    inst_copy_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
    addkey_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
    log_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
    service_state_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);

    if (!hyp_sem || !inst_sem || !inst_copy_sem || !addkey_sem || !log_sem || !service_state_sem) {
        LOGFATAL("failed to create and initialize semaphores\n");
        return (EUCA_FATAL_ERROR);
    }
    if (log_sem_set(log_sem) != 0) {
        LOGFATAL("failed to set logging semaphore\n");
        return (EUCA_FATAL_ERROR);
    }
    if (sensor_set_hyp_sem(hyp_sem) != 0) {
        LOGFATAL("failed to set hypervisor semaphore for the sensor subsystem\n");
        return (EUCA_FATAL_ERROR);
    }
    if ((loop_sem = diskutil_get_loop_sem()) == NULL) { // NC does not need GRUB for now
        LOGFATAL("failed to find all dependencies\n");
        return (EUCA_FATAL_ERROR);
    }

    if (init_eucafaults(euca_this_component_name) == 0) {
        LOGFATAL("failed to initialize fault-logging subsystem\n");
        return (EUCA_FATAL_ERROR);
    }

    if (init_ebs_utils() != 0) {
        LOGFATAL("Failed to initialize ebs utils\n");
        return (EUCA_FATAL_ERROR);
    }

    init_iscsi(nc_state.home);

    // NOTE: this is the only call which needs to be called on both
    // the default and the specific handler! All the others will be
    // either or
    i = nc_state.D->doInitialize(&nc_state);
    if (nc_state.H->doInitialize)
        i += nc_state.H->doInitialize(&nc_state);

    if (i) {
        LOGFATAL("failed to initialized hypervisor driver!\n");
        return (EUCA_FATAL_ERROR);
    }

    {
        // check on hypervisor and pull out capabilities
        virConnectPtr conn = lock_hypervisor_conn();
        if (conn == NULL) {
            LOGFATAL("unable to contact hypervisor\n");
            return (EUCA_FATAL_ERROR);
        }
        char *caps_xml = virConnectGetCapabilities(conn);
        if (caps_xml == NULL) {
            LOGFATAL("unable to obtain hypervisor capabilities\n");
            unlock_hypervisor_conn();
            return (EUCA_FATAL_ERROR);
        }
        unlock_hypervisor_conn();
        if (strstr(caps_xml, "<live/>") != NULL) {
            nc_state.migration_capable = 1;
        }
        EUCA_FREE(caps_xml);
    }
    LOGINFO("hypervisor %scapable of live migration\n", nc_state.migration_capable ? "" : "not ");

    // now that hypervisor-specific initializers have discovered mem_max and cores_max,
    // adjust the values based on configuration parameters, if any
    if (nc_state.config_max_mem) {
        if (nc_state.config_max_mem > nc_state.mem_max)
            LOGWARN("MAX_MEM value is set to %lldMB that is greater than the amount of physical memory: %lldMB\n", nc_state.config_max_mem, nc_state.mem_max);
        nc_state.mem_max = nc_state.config_max_mem;
    } else {
        nc_state.mem_max = nc_state.phy_max_mem;
    }

    if (nc_state.config_max_cores) {
        int cores = nc_state.cores_max;
        nc_state.cores_max = nc_state.config_max_cores;
        if (nc_state.cores_max > MAXINSTANCES_PER_NC) {
            nc_state.cores_max = MAXINSTANCES_PER_NC;
            LOGWARN("ignoring excessive MAX_CORES value (leaving at %lld)\n", nc_state.cores_max);
        }

        if (nc_state.cores_max > cores)
            LOGWARN("MAX_CORES value is set to %lld that is greater than the amount of physical cores: %d\n", nc_state.cores_max, cores);
    } else {
        nc_state.cores_max = nc_state.phy_max_cores;
    }

    LOGINFO("physical memory available for instances: %lldMB\n", nc_state.mem_max);
    LOGINFO("virtual cpu cores available for instances: %lld\n", nc_state.cores_max);

    {
        // backing store configuration
        char *instances_path = getConfString(nc_state.configFiles, 2, INSTANCE_PATH);

        if (instances_path == NULL) {
            LOGERROR("%s is not set\n", INSTANCE_PATH);
            return (EUCA_FATAL_ERROR);
        }
        // create work and cache sub-directories so that stat_backing_store() below succeeds
        char cache_path[EUCA_MAX_PATH];
        snprintf(cache_path, sizeof(cache_path), "%s/cache", instances_path);
        if (ensure_directories_exist(cache_path, 0, NULL, NULL, BACKING_DIRECTORY_PERM) == -1) {
            EUCA_FREE(instances_path);
            return (EUCA_ERROR);
        }

        char work_path[EUCA_MAX_PATH];
        snprintf(work_path, sizeof(work_path), "%s/work", instances_path);
        if (ensure_directories_exist(work_path, 0, NULL, NULL, BACKING_DIRECTORY_PERM) == -1) {
            EUCA_FREE(instances_path);
            return (EUCA_ERROR);
        }
        // determine how much is used/available in work and cache areas on the backing store
        blobstore_meta work_meta, cache_meta;
        stat_backing_store(instances_path, &work_meta, &cache_meta);    // will zero-out work_ and cache_meta
        long long work_fs_size_mb = (long long)(work_meta.fs_bytes_size / MEGABYTE);
        long long work_fs_avail_mb = (long long)(work_meta.fs_bytes_available / MEGABYTE);
        long long cache_fs_size_mb = (long long)(cache_meta.fs_bytes_size / MEGABYTE);
        long long cache_fs_avail_mb = (long long)(cache_meta.fs_bytes_available / MEGABYTE);
        long long work_bs_size_mb = work_meta.blocks_limit ? (work_meta.blocks_limit / SEC_PER_MB) : (-1L); // convert sectors->MB
        long long work_bs_allocated_mb = work_meta.blocks_limit ? (work_meta.blocks_allocated / SEC_PER_MB) : 0;
        long long work_bs_reserved_mb = work_meta.blocks_limit ? ((work_meta.blocks_locked + work_meta.blocks_unlocked) / SEC_PER_MB) : 0;
        long long cache_bs_size_mb = cache_meta.blocks_limit ? (cache_meta.blocks_limit / SEC_PER_MB) : (-1L);
        long long cache_bs_allocated_mb = cache_meta.blocks_limit ? (cache_meta.blocks_allocated / SEC_PER_MB) : 0;
        long long cache_bs_reserved_mb = cache_meta.blocks_limit ? ((cache_meta.blocks_locked + cache_meta.blocks_unlocked) / SEC_PER_MB) : 0;

        // sanity check
        if (work_fs_avail_mb < MIN_BLOBSTORE_SIZE_MB) {
            LOGERROR("insufficient available work space (%lld MB) under %s/work\n", work_fs_avail_mb, instances_path);
            EUCA_FREE(instances_path);
            return (EUCA_FATAL_ERROR);
        }
        // look up configuration file settings for work and cache size
        long long conf_work_size_mb;
        GET_VAR_INT(conf_work_size_mb, CONFIG_NC_WORK_SIZE, -1);

        long long conf_cache_size_mb;
        GET_VAR_INT(conf_cache_size_mb, CONFIG_NC_CACHE_SIZE, -1);

        long long conf_work_overhead_mb;
        GET_VAR_INT(conf_work_overhead_mb, CONFIG_NC_OVERHEAD_SIZE, PER_INSTANCE_BUFFER_MB);

        {                              // accommodate legacy MAX_DISK setting by converting it
            int max_disk_gb;
            GET_VAR_INT(max_disk_gb, CONFIG_MAX_DISK, -1);
            if (max_disk_gb != -1) {
                if (conf_work_size_mb == -1) {
                    LOGWARN("using deprecated setting %s for the new setting %s\n", CONFIG_MAX_DISK, CONFIG_NC_WORK_SIZE);
                    if (max_disk_gb == 0) {
                        conf_work_size_mb = -1; // change in semantics: 0 used to mean 'unlimited', now 'unset' or -1 means that
                    } else {
                        conf_work_size_mb = max_disk_gb * 1024;
                    }
                } else {
                    LOGWARN("ignoring deprecated setting %s in favor of the new setting %s\n", CONFIG_MAX_DISK, CONFIG_NC_WORK_SIZE);
                }
            }
        }

        // decide what work and cache sizes should be, based on all the inputs
        long long work_size_mb = -1;
        long long cache_size_mb = -1;

        // above all, try to respect user-specified limits for work and cache
        if (conf_work_size_mb != -1) {
            if (conf_work_size_mb < MIN_BLOBSTORE_SIZE_MB) {
                LOGWARN("ignoring specified work size (%s=%lld) that is below acceptable minimum (%d)\n", CONFIG_NC_WORK_SIZE, conf_work_size_mb, MIN_BLOBSTORE_SIZE_MB);
            } else {
                if (work_bs_size_mb != -1 && work_bs_size_mb != conf_work_size_mb) {
                    LOGWARN("specified work size (%s=%lld) differs from existing work size (%lld), will try resizing\n", CONFIG_NC_WORK_SIZE, conf_work_size_mb, work_bs_size_mb);
                }
                work_size_mb = conf_work_size_mb;
            }
        }

        if (conf_cache_size_mb != -1) { // respect user-specified limit
            if (conf_cache_size_mb < MIN_BLOBSTORE_SIZE_MB) {
                cache_size_mb = 0;     // so it won't be used
            } else {
                if (cache_bs_size_mb != -1 && cache_bs_size_mb != conf_cache_size_mb) {
                    LOGWARN("specified cache size (%s=%lld) differs from existing cache size (%lld), will try resizing\n",
                            CONFIG_NC_CACHE_SIZE, conf_cache_size_mb, cache_bs_size_mb);
                }
                cache_size_mb = conf_cache_size_mb;
            }
        }
        // if the user did not specify sizes, try existing blobstores,
        // if any, whose limits would have been chosen earlier
        if (work_size_mb == -1 && work_bs_size_mb != -1)
            work_size_mb = work_bs_size_mb;

        if (cache_size_mb == -1 && cache_bs_size_mb != -1)
            cache_size_mb = cache_bs_size_mb;

        // if the user did not specify either or both of the sizes,
        // and blobstores do not exist yet, make reasonable choices
        if (memcmp(&work_meta.fs_id, &cache_meta.fs_id, sizeof(fsid_t)) == 0) { // cache and work are on the same file system
            long long fs_usable_mb = (long long)((double)work_fs_avail_mb - (double)(work_fs_avail_mb) * FS_BUFFER_PERCENT);
            if (work_size_mb == -1 && cache_size_mb == -1) {
                work_size_mb = (long long)((double)fs_usable_mb * WORK_BS_PERCENT);
                cache_size_mb = fs_usable_mb - work_size_mb;
            } else if (work_size_mb == -1) {
                work_size_mb = fs_usable_mb - cache_size_mb + cache_bs_allocated_mb;
            } else if (cache_size_mb == -1) {
                cache_size_mb = fs_usable_mb - work_size_mb + work_bs_allocated_mb;
            }
            // sanity check
            if ((cache_size_mb + work_size_mb - cache_bs_allocated_mb - work_bs_allocated_mb) > work_fs_avail_mb) {
                LOGWARN("sum of work and cache sizes exceeds available disk space\n");
            }
        } else {                       // cache and work are on different file systems
            if (work_size_mb == -1) {
                work_size_mb = (long long)((double)work_fs_avail_mb - (double)(work_fs_avail_mb) * FS_BUFFER_PERCENT);
            }

            if (cache_size_mb == -1) {
                cache_size_mb = (long long)((double)cache_fs_avail_mb - (double)(cache_fs_avail_mb) * FS_BUFFER_PERCENT);
            }
        }

        // sanity-check final results
        if (cache_size_mb < MIN_BLOBSTORE_SIZE_MB)
            cache_size_mb = 0;

        if (work_size_mb < MIN_BLOBSTORE_SIZE_MB) {
            LOGERROR("insufficient disk space for virtual machines\n");
            EUCA_FREE(instances_path);
            return (EUCA_FATAL_ERROR);
        }

        if (init_backing_store(instances_path, work_size_mb, cache_size_mb)) {
            LOGFATAL("failed to initialize backing store\n");
            EUCA_FREE(instances_path);
            return (EUCA_FATAL_ERROR);
        }
        // record the work-space limit for max_disk
        long long work_size_gb = (long long)(work_size_mb / MB_PER_DISK_UNIT);
        if (conf_work_overhead_mb < 0 || conf_work_overhead_mb > work_size_mb) {    // sanity check work overhead
            conf_work_overhead_mb = PER_INSTANCE_BUFFER_MB;
        }

        long long overhead_mb = work_size_gb * conf_work_overhead_mb;   // work_size_gb is the theoretical max number of instances
        long long disk_max_mb = work_size_mb - overhead_mb;
        nc_state.disk_max = disk_max_mb / MB_PER_DISK_UNIT;

        LOGINFO("disk space for instances: %s/work\n", instances_path);
        LOGINFO("                          %06lldMB limit (%.1f%% of the file system) - %lldMB overhead = %lldMB = %lldGB\n",
                work_size_mb, ((double)work_size_mb / (double)work_fs_size_mb) * 100.0, overhead_mb, disk_max_mb, nc_state.disk_max);
        LOGINFO("                          %06lldMB reserved for use (%.1f%% of limit)\n", work_bs_reserved_mb, ((double)work_bs_reserved_mb / (double)work_size_mb) * 100.0);
        LOGINFO("                          %06lldMB allocated for use (%.1f%% of limit, %.1f%% of the file system)\n", work_bs_allocated_mb,
                ((double)work_bs_allocated_mb / (double)work_size_mb) * 100.0, ((double)work_bs_allocated_mb / (double)work_fs_size_mb) * 100.0);

        if (cache_size_mb) {
            LOGINFO("    disk space for cache: %s/cache\n", instances_path);
            LOGINFO("                          %06lldMB limit (%.1f%% of the file system)\n", cache_size_mb, ((double)cache_size_mb / (double)cache_fs_size_mb) * 100.0);
            LOGINFO("                          %06lldMB reserved for use (%.1f%% of limit)\n", cache_bs_reserved_mb,
                    ((double)cache_bs_reserved_mb / (double)cache_size_mb) * 100.0);
            LOGINFO("                          %06lldMB allocated for use (%.1f%% of limit, %.1f%% of the file system)\n", cache_bs_allocated_mb,
                    ((double)cache_bs_allocated_mb / (double)cache_size_mb) * 100.0, ((double)cache_bs_allocated_mb / (double)cache_fs_size_mb) * 100.0);
        } else {
            LOGWARN("disk cache will not be used\n");
        }

        EUCA_FREE(instances_path);
    }

    // adopt running instances -- do this before disk integrity check so we know what can be purged
    adopt_instances();

    if (check_backing_store(&global_instances) != EUCA_OK) {    // integrity check, cleanup of unused instances and shrinking of cache
        LOGFATAL("integrity check of the backing store failed");
        return (EUCA_FATAL_ERROR);
    }
    // setup the network
    snprintf(nc_state.config_network_path, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT, nc_state.home);

    tmp = getConfString(nc_state.configFiles, 2, "VNET_MODE");
    if (!tmp) {
        LOGWARN("VNET_MODE is not defined, defaulting to 'SYSTEM'\n");
        tmp = strdup("SYSTEM");
        if (!tmp) {
            LOGFATAL("Out of memory\n");
            return (EUCA_FATAL_ERROR);
        }
    }

    int initFail = 0;

    if (tmp
        && !(!strcmp(tmp, NETMODE_SYSTEM) || !strcmp(tmp, NETMODE_STATIC) || !strcmp(tmp, NETMODE_MANAGED_NOVLAN) || !strcmp(tmp, NETMODE_MANAGED) || !strcmp(tmp, NETMODE_EDGE))) {
        char errorm[256];
        memset(errorm, 0, 256);
        sprintf(errorm, "Invalid VNET_MODE setting: %s", tmp);
        LOGFATAL("%s\n", errorm);
        log_eucafault("1012", "component", euca_this_component_name, "cause", errorm, NULL);
        initFail = 1;
    }

    if (tmp && (!strcmp(tmp, NETMODE_SYSTEM) || !strcmp(tmp, NETMODE_STATIC) || !strcmp(tmp, NETMODE_MANAGED_NOVLAN) || !strcmp(tmp, NETMODE_EDGE))) {
        bridge = getConfString(nc_state.configFiles, 2, "VNET_BRIDGE");
        if (!bridge) {
            LOGFATAL("in 'SYSTEM', 'STATIC', 'EDGE', or 'MANAGED-NOVLAN' network mode, you must specify a value for VNET_BRIDGE\n");
            initFail = 1;
        }
    }
    if (tmp && (!strcmp(tmp, NETMODE_MANAGED) || !strcmp(tmp, NETMODE_EDGE))) {
        pubinterface = getConfString(nc_state.configFiles, 2, "VNET_PUBINTERFACE");
        if (!pubinterface)
            pubinterface = getConfString(nc_state.configFiles, 2, "VNET_INTERFACE");

        if (!pubinterface) {
            LOGWARN("VNET_PUBINTERFACE is not defined, defaulting to 'eth0'\n");
            pubinterface = strdup("eth0");
            if (!pubinterface) {
                LOGFATAL("out of memory!\n");
                initFail = 1;
            }
        }
    }

    if (!initFail) {
        initFail = vnetInit(nc_state.vnetconfig,
                            tmp, nc_state.home, nc_state.config_network_path, NC, pubinterface, pubinterface, NULL, NULL, NULL, NULL, NULL, NULL,
                            NULL, NULL, NULL, bridge, NULL, NULL);
    }

    EUCA_FREE(pubinterface);
    EUCA_FREE(bridge);
    EUCA_FREE(tmp);

    if (initFail)
        return (EUCA_FATAL_ERROR);

    // set NC helper path
    tmp = getConfString(nc_state.configFiles, 2, CONFIG_NC_BUNDLE_UPLOAD);
    if (tmp) {
        snprintf(nc_state.ncBundleUploadCmd, EUCA_MAX_PATH, "%s", tmp);
        EUCA_FREE(tmp);
    } else {
        snprintf(nc_state.ncBundleUploadCmd, EUCA_MAX_PATH, "%s", EUCALYPTUS_NC_BUNDLE_UPLOAD); // default value
    }

    // set NC helper path
    tmp = getConfString(nc_state.configFiles, 2, CONFIG_NC_CHECK_BUCKET);
    if (tmp) {
        snprintf(nc_state.ncCheckBucketCmd, EUCA_MAX_PATH, "%s", tmp);
        EUCA_FREE(tmp);
    } else {
        snprintf(nc_state.ncCheckBucketCmd, EUCA_MAX_PATH, "%s", EUCALYPTUS_NC_CHECK_BUCKET);   // default value
    }

    // set NC helper path
    tmp = getConfString(nc_state.configFiles, 2, CONFIG_NC_DELETE_BUNDLE);
    if (tmp) {
        snprintf(nc_state.ncDeleteBundleCmd, EUCA_MAX_PATH, "%s", tmp);
        EUCA_FREE(tmp);
    } else {
        snprintf(nc_state.ncDeleteBundleCmd, EUCA_MAX_PATH, "%s", EUCALYPTUS_NC_DELETE_BUNDLE); // default value
    }

    {
        // set enable ws-security
        tmp = getConfString(nc_state.configFiles, 2, CONFIG_ENABLE_WS_SECURITY);
        if (tmp && !strcmp(tmp, "N")) {
            LOGDEBUG("Configuring no use of WS-SEC as specified in config file by explicit 'no' value\n");
            nc_state.config_use_ws_sec = 0;
            EUCA_FREE(tmp);
        } else {
            LOGDEBUG("Configured to use WS-SEC by default\n");
            if (tmp)
                EUCA_FREE(tmp);
            nc_state.config_use_ws_sec = 1;
        }
    }

    {                                  // find and set iqn
        snprintf(nc_state.iqn, CHAR_BUFFER_SIZE, "UNSET");
        char *ptr = NULL, *iqn = NULL, *tmp = NULL, cmd[EUCA_MAX_PATH];
        snprintf(cmd, EUCA_MAX_PATH, "%s cat /etc/iscsi/initiatorname.iscsi", nc_state.rootwrap_cmd_path);
        ptr = system_output(cmd);
        if (ptr) {
            iqn = strstr(ptr, "InitiatorName=");
            if (iqn) {
                iqn += strlen("InitiatorName=");
                tmp = strstr(iqn, "\n");
                if (tmp)
                    *tmp = '\0';
                snprintf(nc_state.iqn, CHAR_BUFFER_SIZE, "%s", iqn);
            }
            EUCA_FREE(ptr);
        }
    }

    {                                  // find and set IP
        char hostname[HOSTNAME_SIZE];
        if (gethostname(hostname, sizeof(hostname)) != 0) {
            LOGFATAL("failed to find hostname\n");
            return (EUCA_FATAL_ERROR);
        }

        struct hostent *he;
        if ((he = gethostbyname(hostname)) == NULL) {
            LOGFATAL("failed to obtain host information for %s\n", hostname);
            return (EUCA_FATAL_ERROR);
        }

        int found = 0;
        struct in_addr **addr_list = (struct in_addr **)he->h_addr_list;
        for (int i = 0; !found && addr_list[i] != NULL; i++) {
            if (!found) {
                euca_strncpy(nc_state.ip, inet_ntoa(*addr_list[i]), sizeof(nc_state.ip));
                found = 1;
            }
        }
        if (!found) {
            LOGFATAL("failed to obtain IP for %s\n", hostname);
            return (EUCA_FATAL_ERROR);
        }
        LOGINFO("using IP %s\n", nc_state.ip);

        LOGINFO("Initializing localhost info for vbr processing\n");
        if (vbr_init_hostconfig(nc_state.iqn, nc_state.ip, nc_state.config_sc_policy_file, nc_state.config_use_ws_sec) != 0) {
            LOGFATAL("Error initializing vbr localhost configuration\n");
            return (EUCA_FATAL_ERROR);
        }
    }

    {
        LOGINFO("Initializing service state and epoch\n");
        //Initialize the service state info.
        nc_state.ncStatus.localEpoch = 0;
        snprintf(nc_state.ncStatus.details, 1024, "ERRORS=0");
        snprintf(nc_state.ncStatus.serviceId.type, 32, "node");
        snprintf(nc_state.ncStatus.serviceId.name, 32, "self");
        snprintf(nc_state.ncStatus.serviceId.partition, 32, "unset");
        nc_state.ncStatus.serviceId.urisLen = 0;
        nc_state.servicesLen = 0;
        nc_state.disabledServicesLen = 0;
        nc_state.notreadyServicesLen = 0;

        for (i = 0; i < 32 && nc_state.ncStatus.serviceId.urisLen < 8; i++) {
            if (nc_state.vnetconfig->localIps[i]) {
                char *host;
                host = hex2dot(nc_state.vnetconfig->localIps[i]);
                if (host) {
                    snprintf(nc_state.ncStatus.serviceId.uris[nc_state.ncStatus.serviceId.urisLen], 512, "http://%s:8775/axis2/services/EucalyptusNC", host);
                    nc_state.ncStatus.serviceId.urisLen++;
                    EUCA_FREE(host);
                }
            }
        }
        LOGINFO("Done initializing services state\n");
    }

    {                                  // start the monitoring thread
        pthread_t tcb;
        if (pthread_create(&tcb, NULL, monitoring_thread, &nc_state)) {
            LOGFATAL("failed to spawn a monitoring thread\n");
            return (EUCA_FATAL_ERROR);
        }
        if (pthread_detach(tcb)) {
            LOGFATAL("failed to detach the monitoring thread\n");
            return (EUCA_FATAL_ERROR);
        }
    }

    // post-init hook
    if (call_hooks(NC_EVENT_POST_INIT, nc_state.home)) {
        LOGFATAL("hooks prevented initialization\n");
        return (EUCA_FATAL_ERROR);
    }

    initialized = 1;
    return (EUCA_OK);

#undef GET_VAR_INT
}

//!
//! Handles the describe instance request
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instIds a pointer the list of instance identifiers to retrieve data for
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[out] outInsts a pointer the list of instances for which we have data
//! @param[out] outInstsLen the number of instances in the outInsts list.
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         EUCA_MEMORY_ERROR, EUCA_MEMORY_ERROR
//!
int doDescribeInstances(ncMetadata * pMeta, char **instIds, int instIdsLen, ncInstance *** outInsts, int *outInstsLen)
{
#define NC_MONIT_FILENAME                        EUCALYPTUS_RUN_DIR  "/nc-stats"

    int i = 0;
    int j = 0;
    int ret = EUCA_OK;
    int len = 0;
    char *s = "";
    char *file_name = NULL;
    char myName[CHAR_BUFFER_SIZE] = "";
    FILE *f = NULL;
    long long used_mem = 0;
    long long used_disk = 0;
    long long used_cores = 0;
    u_int vols_count = 0;

    if (init())
        return (EUCA_ERROR);

    LOGTRACE("invoked\n");             // response will be at INFO, so this is TRACE

    updateServiceStateInfo(pMeta, FALSE);
    if (nc_state.H->doDescribeInstances)
        ret = nc_state.H->doDescribeInstances(&nc_state, pMeta, instIds, instIdsLen, outInsts, outInstsLen);
    else
        ret = nc_state.D->doDescribeInstances(&nc_state, pMeta, instIds, instIdsLen, outInsts, outInstsLen);

    if (ret)
        return ret;

    for (i = 0; i < (*outInstsLen); i++) {
        char vols_str[128] = "";
        char vol_str[16] = "";
        char mig_str[128] = "not migrating";
        ncInstance *instance = (*outInsts)[i];

        // construct a string summarizing the volumes attached to the instance
        vols_count = 0;
        for (j = 0; j < EUCA_MAX_VOLUMES; ++j) {
            ncVolume *volume = &instance->volumes[j];
            if (strlen(volume->volumeId) == 0)
                continue;
            vols_count++;

            s = "";
            if (!strcmp(volume->stateName, VOL_STATE_ATTACHING))
                s = "a";
            else if (!strcmp(volume->stateName, VOL_STATE_ATTACHED))
                s = "A";
            else if (!strcmp(volume->stateName, VOL_STATE_ATTACHING_FAILED))
                s = "af";
            else if (!strcmp(volume->stateName, VOL_STATE_DETACHING))
                s = "d";
            else if (!strcmp(volume->stateName, VOL_STATE_DETACHED))
                s = "D";
            else if (!strcmp(volume->stateName, VOL_STATE_DETACHING_FAILED))
                s = "df";

            snprintf(vol_str, sizeof(vol_str), "%s%s:%s", (vols_count > 1) ? (",") : (""), volume->volumeId, s);
            if ((strlen(vols_str) + strlen(vol_str)) < sizeof(vols_str)) {
                strcat(vols_str, vol_str);
            }
        }

        if (instance->migration_state != NOT_MIGRATING) {   // construct migration status string
            char *peer = "?";
            char dir = '?';
            if (!strcmp(nc_state.ip, instance->migration_src)) {
                peer = instance->migration_dst;
                dir = '>';
            } else {
                peer = instance->migration_src;
                dir = '<';
            }
            snprintf(mig_str, sizeof(mig_str), "%s %c%s", migration_state_names[instance->migration_state], dir, peer);
        }

        LOGDEBUG("[%s] %s (%s) pub=%s vols=%s\n", instance->instanceId, instance->stateName, mig_str, instance->ncnet.publicIp, vols_str);
    }

    // allocate enough memory
    len = (strlen(EUCALYPTUS_CONF_LOCATION) > strlen(NC_MONIT_FILENAME)) ? strlen(EUCALYPTUS_CONF_LOCATION) : strlen(NC_MONIT_FILENAME);
    len += 2 + strlen(nc_state.home);
    if ((file_name = EUCA_ALLOC(1, sizeof(char) * len)) == NULL) {
        LOGERROR("Out of memory!\n");
        return (EUCA_MEMORY_ERROR);
    }

    sprintf(file_name, NC_MONIT_FILENAME, nc_state.home);
    if (!strcmp(pMeta->userId, EUCALYPTUS_ADMIN)) {
        if ((f = fopen(file_name, "w")) == NULL) {
            if ((f = fopen(file_name, "w+")) == NULL) {
                LOGWARN("Cannot create %s!\n", file_name);
            } else {
                if ((len = fileno(f)) > 0)
                    fchmod(len, S_IRUSR | S_IWUSR);
            }
        }

        if (f) {
            fprintf(f, "version: %s\n", EUCA_VERSION);
            fprintf(f, "timestamp: %ld\n", time(NULL));
            if (gethostname(myName, CHAR_BUFFER_SIZE) == 0)
                fprintf(f, "node: %s\n", myName);
            fprintf(f, "hypervisor: %s\n", nc_state.H->name);
            fprintf(f, "network: %s\n", nc_state.vnetconfig->mode);

            used_disk = used_mem = used_cores = 0;
            for (i = 0; i < (*outInstsLen); i++) {
                ncInstance *instance = (*outInsts)[i];
                used_disk += instance->params.disk;
                used_mem += instance->params.mem;
                used_cores += instance->params.cores;
            }

            fprintf(f, "memory (max/avail/used) MB: %lld/%lld/%lld\n", nc_state.mem_max, nc_state.mem_max - used_mem, used_mem);
            fprintf(f, "disk (max/avail/used) GB: %lld/%lld/%lld\n", nc_state.disk_max, nc_state.disk_max - used_disk, used_disk);
            fprintf(f, "cores (max/avail/used): %lld/%lld/%lld\n", nc_state.cores_max, nc_state.cores_max - used_cores, used_cores);

            for (i = 0; i < (*outInstsLen); i++) {
                ncInstance *instance = (*outInsts)[i];
                fprintf(f, "id: %s", instance->instanceId);
                fprintf(f, " userId: %s", instance->userId);
                fprintf(f, " state: %s", instance->stateName);
                fprintf(f, " mem: %d", instance->params.mem);
                fprintf(f, " disk: %d", instance->params.disk);
                fprintf(f, " cores: %d", instance->params.cores);
                fprintf(f, " private: %s", instance->ncnet.privateIp);
                fprintf(f, " public: %s\n", instance->ncnet.publicIp);
            }
            fclose(f);
        }
    }
    EUCA_FREE(file_name);

    LOGTRACE("done\n");
    return (EUCA_OK);
}

//!
//! Handles the broadcast network info request
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] networkInfo is a string
//!
//! @return EUCA_ERROR on failure or the result of the proper doBroadcastNetworkInfo() handler call.
//!
int doBroadcastNetworkInfo(ncMetadata * pMeta, char *networkInfo)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);

    LOGDEBUG("invoked\n");
    LOGTRACE("invoked with networkInfo='%s'\n", SP(networkInfo));

    if (nc_state.H->doBroadcastNetworkInfo)
        ret = nc_state.H->doBroadcastNetworkInfo(&nc_state, pMeta, networkInfo);
    else
        ret = nc_state.D->doBroadcastNetworkInfo(&nc_state, pMeta, networkInfo);

    return ret;
}

//!
//! Handles the assign address request
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] publicIp a string representation of the public IP to assign to the instance
//!
//! @return EUCA_ERROR on failure or the result of the proper doAssignAddress() handler call.
//!
int doAssignAddress(ncMetadata * pMeta, char *instanceId, char *publicIp)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);

    LOGDEBUG("[%s] invoked (publicIp=%s)\n", instanceId, publicIp);

    if (nc_state.H->doAssignAddress)
        ret = nc_state.H->doAssignAddress(&nc_state, pMeta, instanceId, publicIp);
    else
        ret = nc_state.D->doAssignAddress(&nc_state, pMeta, instanceId, publicIp);

    return ret;
}

//!
//! Handles the power down request.
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return EUCA_ERROR on failure or the result of the proper doPowerDown() handler call.
//!
int doPowerDown(ncMetadata * pMeta)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);

    LOGDEBUG("invoked\n");

    if (nc_state.H->doPowerDown)
        ret = nc_state.H->doPowerDown(&nc_state, pMeta);
    else
        ret = nc_state.D->doPowerDown(&nc_state, pMeta);

    return ret;
}

//!
//! Handles the run instance request.
//!
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
//! @param[in]  launchIndex the launch index string
//! @param[in]  platform the platform name string
//! @param[in]  expiryTime the reservation expiration time
//! @param[in]  groupNames a list of group name string
//! @param[in]  groupNamesSize the number of group name in the groupNames list
//! @param[out] outInst the list of instances created by this request
//!
//! @return EUCA_ERROR on failure or the result of the proper doRunInstance() handler call.
//!
int doRunInstance(ncMetadata * pMeta, char *uuid, char *instanceId, char *reservationId, virtualMachine * params, char *imageId, char *imageURL,
                  char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *ownerId, char *accountId, char *keyName,
                  netConfig * netparams, char *userData, char *credential, char *launchIndex, char *platform, int expiryTime, char **groupNames, int groupNamesSize,
                  ncInstance ** outInst)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGINFO("[%s] running instance cores=%d disk=%d memory=%d vlan=%d net=%d priMAC=%s privIp=%s plat=%s kernel=%s ramdisk=%s\n", instanceId, params->cores, params->disk,
            params->mem, netparams->vlan, netparams->networkIndex, netparams->privateMac, netparams->privateIp, platform, kernelId, ramdiskId);
    if (vbr_legacy(instanceId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL) != EUCA_OK)
        return (EUCA_ERROR);
    // spark: kernel and ramdisk id are required for linux bundle-instance, but are not in the runInstance request;
    if (!kernelId || !ramdiskId) {
        for (int i = 0; i < EUCA_MAX_VBRS && i < params->virtualBootRecordLen; i++) {
            virtualBootRecord *vbr = &(params->virtualBootRecord[i]);
            if (strlen(vbr->resourceLocation) > 0) {
                if (!strcmp(vbr->typeName, "kernel")) {
                    // free our string if it was previously set
                    EUCA_FREE(kernelId);
                    kernelId = strdup(vbr->id);
                }

                if (!strcmp(vbr->typeName, "ramdisk")) {
                    // free our string if it was previously set
                    EUCA_FREE(ramdiskId);
                    ramdiskId = strdup(vbr->id);
                }
            } else {
                break;
            }
        }
    }

    if (nc_state.H->doRunInstance) {
        ret = nc_state.H->doRunInstance(&nc_state, pMeta, uuid, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId,
                                        ramdiskURL, ownerId, accountId, keyName, netparams, userData, credential, launchIndex, platform, expiryTime, groupNames, groupNamesSize,
                                        outInst);
    } else {
        ret = nc_state.D->doRunInstance(&nc_state, pMeta, uuid, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId,
                                        ramdiskURL, ownerId, accountId, keyName, netparams, userData, credential, launchIndex, platform, expiryTime, groupNames, groupNamesSize,
                                        outInst);
    }

    return ret;
}

//!
//! Finds and terminate an instance.
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in]  force if set to 1 will force the termination of the instance
//! @param[out] shutdownState the instance state code after the call to find_and_terminate_instance() if successful
//! @param[out] previousState the instance state code after the call to find_and_terminate_instance() if successful
//!
//! @return EUCA_ERROR on failure or the result of the proper doTerminateInstance() handler call.
//!
int doTerminateInstance(ncMetadata * pMeta, char *instanceId, int force, int *shutdownState, int *previousState)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGINFO("[%s] termination requested\n", instanceId);

    if (nc_state.H->doTerminateInstance)
        ret = nc_state.H->doTerminateInstance(&nc_state, pMeta, instanceId, force, shutdownState, previousState);
    else
        ret = nc_state.D->doTerminateInstance(&nc_state, pMeta, instanceId, force, shutdownState, previousState);

    return ret;
}

//!
//! Handles the reboot instance request
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_ERROR on failure or the result of the proper doRebootInstance() handler call.
//!
int doRebootInstance(ncMetadata * pMeta, char *instanceId)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGDEBUG("[%s] invoked\n", instanceId);

    if (nc_state.H->doRebootInstance)
        ret = nc_state.H->doRebootInstance(&nc_state, pMeta, instanceId);
    else
        ret = nc_state.D->doRebootInstance(&nc_state, pMeta, instanceId);

    return ret;
}

//!
//! Handles the get console output request
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[out] consoleOutput a pointer to the unallocated string that will contain the output
//!
//! @return EUCA_ERROR on failure or the result of the proper doGetConsoleOutput() handler call.
//!
int doGetConsoleOutput(ncMetadata * pMeta, char *instanceId, char **consoleOutput)
{
    int ret = EUCA_OK;

    if (init())
        return 1;

    LOGINFO("[%s] console output requested\n", instanceId);

    if (nc_state.H->doGetConsoleOutput)
        ret = nc_state.H->doGetConsoleOutput(&nc_state, pMeta, instanceId, consoleOutput);
    else
        ret = nc_state.D->doGetConsoleOutput(&nc_state, pMeta, instanceId, consoleOutput);

    return ret;
}

//!
//! Handles the describe resource request.
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  resourceType UNUSED
//! @param[out] outRes a list of resources we retrieved data for
//!
//! @return EUCA_ERROR on failure or the result of the proper doDescribeResource() handler call.
//!
int doDescribeResource(ncMetadata * pMeta, char *resourceType, ncResource ** outRes)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);

    updateServiceStateInfo(pMeta, TRUE);

    if (nc_state.H->doDescribeResource)
        ret = nc_state.H->doDescribeResource(&nc_state, pMeta, resourceType, outRes);
    else
        ret = nc_state.D->doDescribeResource(&nc_state, pMeta, resourceType, outRes);

    return ret;
}

//!
//! Starts the network process.
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] uuid a string containing the user unique identifier (UNUSED)
//! @param[in] remoteHosts the list of remote hosts (UNUSED)
//! @param[in] remoteHostsLen the number of hosts in the remoteHosts list (UNUSED)
//! @param[in] port the port number to use for the network (UNUSED)
//! @param[in] vlan the network vlan to use.
//!
//! @return EUCA_ERROR on failure or the result of the proper doStartNetwork() handler call.
//!
int doStartNetwork(ncMetadata * pMeta, char *uuid, char **remoteHosts, int remoteHostsLen, int port, int vlan)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);

    LOGDEBUG("invoked (remoteHostsLen=%d port=%d vlan=%d)\n", remoteHostsLen, port, vlan);

    if (nc_state.H->doStartNetwork)
        ret = nc_state.H->doStartNetwork(&nc_state, pMeta, uuid, remoteHosts, remoteHostsLen, port, vlan);
    else
        ret = nc_state.D->doStartNetwork(&nc_state, pMeta, uuid, remoteHosts, remoteHostsLen, port, vlan);

    return ret;
}

//!
//! Attach a given volume to an instance.
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the target device name
//! @param[in] localDev the local device name
//!
//! @return EUCA_ERROR on failure or the result of the proper doAttachVolume() handler call.
//!
int doAttachVolume(ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev, char *localDev)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGDEBUG("[%s][%s] volume attaching (localDev=%s)\n", instanceId, volumeId, localDev);

    if (nc_state.H->doAttachVolume)
        ret = nc_state.H->doAttachVolume(&nc_state, pMeta, instanceId, volumeId, remoteDev, localDev);
    else
        ret = nc_state.D->doAttachVolume(&nc_state, pMeta, instanceId, volumeId, remoteDev, localDev);

    return ret;
}

//!
//! Detach a given volume from an instance.
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] attachmentToken the target device name
//! @param[in] localDev the local device name
//! @param[in] force if set to 1, this will force the volume to detach
//! @param[in] grab_inst_sem if set to 1, will require the usage of the instance semaphore
//!
//! @return EUCA_ERROR on failure or the result of the proper doDetachVolume() handler call.
//!
int doDetachVolume(ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev, int force, int grab_inst_sem)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGDEBUG("[%s][%s] volume detaching (localDev=%s force=%d grab_inst_sem=%d)\n", instanceId, volumeId, localDev, force, grab_inst_sem);

    if (nc_state.H->doDetachVolume)
        ret = nc_state.H->doDetachVolume(&nc_state, pMeta, instanceId, volumeId, attachmentToken, localDev, force, grab_inst_sem);
    else
        ret = nc_state.D->doDetachVolume(&nc_state, pMeta, instanceId, volumeId, attachmentToken, localDev, force, grab_inst_sem);

    return ret;
}

//!
//! Handles the bundling instance request.
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] bucketName the bucket name string to which the bundle will be saved
//! @param[in] filePrefix the prefix name string of the bundle
//! @param[in] objectStorageURL the objectstorage URL address string
//! @param[in] userPublicKey the public key string
//! @param[in] S3Policy the S3 engine policy
//! @param[in] S3PolicySig the S3 engine policy signature
//!
//! @return EUCA_ERROR on failure or the result of the proper doBundleInstance() handler call.
//!
int doBundleInstance(ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *objectStorageURL, char *userPublicKey, char *S3Policy, char *S3PolicySig)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGINFO("[%s] starting instance bundling into bucket %s\n", instanceId, bucketName);
    LOGDEBUG("[%s] bundling parameters: bucketName=%s filePrefix=%s objectStorageURL=%s userPublicKey=%s S3Policy=%s, S3PolicySig=%s\n",
             instanceId, bucketName, filePrefix, objectStorageURL, userPublicKey, S3Policy, S3PolicySig);

    if (nc_state.H->doBundleInstance)
        ret = nc_state.H->doBundleInstance(&nc_state, pMeta, instanceId, bucketName, filePrefix, objectStorageURL, userPublicKey, S3Policy, S3PolicySig);
    else
        ret = nc_state.D->doBundleInstance(&nc_state, pMeta, instanceId, bucketName, filePrefix, objectStorageURL, userPublicKey, S3Policy, S3PolicySig);

    return ret;
}

//!
//! Handles the bundle restart request.
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_ERROR on failure or the result of the proper doBundleRestartInstance() handler call.
//!
int doBundleRestartInstance(ncMetadata * pMeta, char *instanceId)
{
    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGINFO("[%s] restarting bundling instance\n", instanceId);
    if (nc_state.H->doBundleRestartInstance)
        return (nc_state.H->doBundleRestartInstance(&nc_state, pMeta, instanceId));
    return (nc_state.D->doBundleRestartInstance(&nc_state, pMeta, instanceId));
}

//!
//! Handles the cancel bundle task request.
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_ERROR on failure or the result of the proper doCancelBundleTask() handler call.
//!
int doCancelBundleTask(ncMetadata * pMeta, char *instanceId)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGINFO("[%s] canceling bundling instance\n", instanceId);

    if (nc_state.H->doCancelBundleTask)
        ret = nc_state.H->doCancelBundleTask(&nc_state, pMeta, instanceId);
    else
        ret = nc_state.D->doCancelBundleTask(&nc_state, pMeta, instanceId);

    return ret;
}

//!
//! Handles the describe bundle tasks request.
//!
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instIds a list of instance identifier string
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[out] outBundleTasks a pointer to the created bundle tasks list
//! @param[out] outBundleTasksLen the number of bundle tasks in the outBundleTasks list
//!
//! @return EUCA_ERROR on failure or the result of the proper doDescribeBundleTasks() handler call.
//!
int doDescribeBundleTasks(ncMetadata * pMeta, char **instIds, int instIdsLen, bundleTask *** outBundleTasks, int *outBundleTasksLen)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGINFO("describing bundle tasks (for %d instances)\n", instIdsLen);

    if (nc_state.H->doDescribeBundleTasks)
        ret = nc_state.H->doDescribeBundleTasks(&nc_state, pMeta, instIds, instIdsLen, outBundleTasks, outBundleTasksLen);
    else
        ret = nc_state.D->doDescribeBundleTasks(&nc_state, pMeta, instIds, instIdsLen, outBundleTasks, outBundleTasksLen);

    return ret;
}

//!
//! Handles the image creation request.
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the remote device name
//!
//! @return EUCA_ERROR on failure or the result of the proper doCreateImage() handler call.
//!
int doCreateImage(ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGINFO("[%s][%s] creating image\n", instanceId, volumeId);

    if (nc_state.H->doCreateImage)
        ret = nc_state.H->doCreateImage(&nc_state, pMeta, instanceId, volumeId, remoteDev);
    else
        ret = nc_state.D->doCreateImage(&nc_state, pMeta, instanceId, volumeId, remoteDev);

    return ret;
}

//!
//! Handles the describe sensors request.
//!
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
//! @return EUCA_ERROR on failure or the result of the proper doDescribeSensors() handler call.
//!
int doDescribeSensors(ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds, int instIdsLen, char **sensorIds,
                      int sensorIdsLen, sensorResource *** outResources, int *outResourcesLen)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);

    LOGDEBUG("invoked (instIdsLen=%d sensorIdsLen=%d)\n", instIdsLen, sensorIdsLen);

    if (nc_state.H->doDescribeSensors) {
        ret = nc_state.H->doDescribeSensors(&nc_state, pMeta, historySize, collectionIntervalTimeMs, instIds, instIdsLen, sensorIds, sensorIdsLen, outResources, outResourcesLen);
    } else {
        ret = nc_state.D->doDescribeSensors(&nc_state, pMeta, historySize, collectionIntervalTimeMs, instIds, instIdsLen, sensorIds, sensorIdsLen, outResources, outResourcesLen);
    }

    return ret;
}

//!
//! Handles the modify node request.
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] stateName
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! TODO: doxygen
int doModifyNode(ncMetadata * pMeta, char *stateName)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);

    LOGDEBUG("invoked (stateName=%s)\n", stateName);

    if (nc_state.H->doModifyNode) {
        ret = nc_state.H->doModifyNode(&nc_state, pMeta, stateName);
    } else {
        ret = nc_state.D->doModifyNode(&nc_state, pMeta, stateName);
    }

    return ret;
}

//!
//! Handles the instance migration request.
//!
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
int doMigrateInstances(ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);

    LOGTRACE("invoked\n");

    LOGDEBUG("verifying %d instance[s] for migration...\n", instancesLen);
    for (int i = 0; i < instancesLen; i++) {
        LOGDEBUG("verifying instance # %d...\n", i);
        if (instances[i]) {
            LOGDEBUG("invoked (action=%s instance[%d].{id=%s src=%s dst=%s) creds=%s\n",
                     action, i, instances[i]->instanceId, instances[i]->migration_src, instances[i]->migration_dst, (credentials == NULL) ? "UNSET" : "present");
            if (!strcmp(instances[i]->migration_src, instances[i]->migration_dst)) {
                if (strcmp(action, "rollback")) {
                    // Anything but rollback.
                    LOGERROR("[%s] rejecting proposed SAME-NODE migration from %s to %s\n", instances[i]->instanceId, instances[i]->migration_src, instances[i]->migration_dst);
                    return (EUCA_UNSUPPORTED_ERROR);
                } else {
                    // Ignore the fact src & dst are the same if a rollback--it doesn't matter.
                    LOGDEBUG("[%s] ignoring apparent same-node migration hosts (%s > %s) for action '%s'\n", instances[i]->instanceId, instances[i]->migration_src,
                             instances[i]->migration_dst, action);
                }
            }
        }
    }

    if (nc_state.H->doMigrateInstances) {
        ret = nc_state.H->doMigrateInstances(&nc_state, pMeta, instances, instancesLen, action, credentials);
    } else {
        ret = nc_state.D->doMigrateInstances(&nc_state, pMeta, instances, instancesLen, action, credentials);
    }

    LOGTRACE("done\n");

    return ret;
}

//!
//! Handles the instance start request
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_ERROR on failure or the result of the actual doStartInstance() call
//!
int doStartInstance(ncMetadata * pMeta, char *instanceId)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGINFO("[%s] instance start requested\n", instanceId);
    if (nc_state.H->doStartInstance)
        ret = nc_state.H->doStartInstance(&nc_state, pMeta, instanceId);
    else
        ret = nc_state.D->doStartInstance(&nc_state, pMeta, instanceId);

    return ret;
}

//!
//! Handles the instance stop request
//!
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_ERROR on failure or the result of the actual doStopInstance() call
//!
int doStopInstance(ncMetadata * pMeta, char *instanceId)
{
    int ret = EUCA_OK;

    if (init())
        return (EUCA_ERROR);
    DISABLED_CHECK;

    LOGINFO("[%s] instance shutdown requested\n", instanceId);
    if (nc_state.H->doStopInstance)
        ret = nc_state.H->doStopInstance(&nc_state, pMeta, instanceId);
    else
        ret = nc_state.D->doStopInstance(&nc_state, pMeta, instanceId);

    return ret;
}

//!
//! Finds an instance in the global instance list
//!
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return a pointer to the instance structure if found. Otherwise NULL is returned.
//!
ncInstance *find_global_instance(const char *instanceId)
{
    return NULL;
}

//!
//! Predicate determining whether the instance is a migration destination
//!
//! @param[in] instance pointer to the instance struct
//!
//! @return true or false
//!
int is_migration_dst(const ncInstance * instance)
{
    if (instance->migration_state != NOT_MIGRATING && !strcmp(instance->migration_dst, nc_state.ip))
        return TRUE;
    return FALSE;
}

//!
//! Predicate determining whether the instance is a migration source
//!
//! @param[in] instance pointer to the instance struct
//!
//! @return true or false
//!
int is_migration_src(const ncInstance * instance)
{
    if (instance->migration_state != NOT_MIGRATING && !strcmp(instance->migration_src, nc_state.ip))
        return TRUE;
    return FALSE;
}

//!
//! Rollback a pending migration request on a source NC
//!
//! Currently only safe to call under the protection of inst_sem, such as from the migrating_thread().
//!
//! @param[in] instance pointer to the instance struct
//!
//! @return true or false
//!
int migration_rollback(ncInstance * instance)
{
    // TO-DO: duplicated code in two parts of conditional. Refactor.
    if (is_migration_src(instance)) {
        LOGINFO("[%s] starting migration rollback of instance on source %s\n", instance->instanceId, instance->migration_src);
        instance->migration_state = NOT_MIGRATING;
        // Not zeroing out the src & dst for debugging purposes:
        // There's a problem with refresh_instances_info() not finding domains
        // and eventually shutting them down.
        //bzero(instance->migration_src, HOSTNAME_SIZE);
        //bzero(instance->migration_dst, HOSTNAME_SIZE);
        bzero(instance->migration_credentials, CREDENTIAL_SIZE);
        instance->migrationTime = 0;
        save_instance_struct(instance);
        copy_instances();
        LOGINFO("[%s] migration source rolled back\n", instance->instanceId);
        return TRUE;
    } else if (is_migration_dst(instance)) {
        // TO-DO: Do I want to protect this functionality by requiring something like a 'force' option be passed to this function?
        LOGWARN("[%s] resetting migration state '%s' to 'none' for an already-migrated (%s < %s) instance. Something went wrong somewhere...\n",
                instance->instanceId, migration_state_names[instance->migration_state], instance->migration_dst, instance->migration_src);
        instance->migration_state = NOT_MIGRATING;
        bzero(instance->migration_src, HOSTNAME_SIZE);
        bzero(instance->migration_dst, HOSTNAME_SIZE);
        bzero(instance->migration_credentials, CREDENTIAL_SIZE);
        instance->migrationTime = 0;
        save_instance_struct(instance);
        copy_instances();
        LOGINFO("[%s] migration state reset.\n", instance->instanceId);
        return TRUE;
    }
    // Neither source nor destination node?
    LOGERROR("[%s] request to roll back migration of instance on non-source/destination node %s\n", instance->instanceId, nc_state.ip);
    // We've seen this case caused by a bug in the migration code--one that left the migration_dst blank in the instance struct.
    // So if this happens, we'll assume the rollback request was valid, and we'll reset its state and time so that it will get cleaned up--rather than stuck!
    instance->migration_state = NOT_MIGRATING;
    instance->migrationTime = 0;
    save_instance_struct(instance);
    copy_instances();
    return FALSE;
}
