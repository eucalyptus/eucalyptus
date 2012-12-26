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
//! @file node/handlers_kvm.c
//! This implements the operations handlers supported by the KVM hypervisor.
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU               /* strnlen */
#include <string.h>             /* strlen, strcpy */
#include <time.h>
#include <sys/types.h>          /* fork */
#include <sys/wait.h>           /* waitpid */
#include <unistd.h>
#include <assert.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>             /* SIGINT */
#include <sys/stat.h>
#include <fcntl.h>

#include <ipc.h>
#include <misc.h>
#include <eucalyptus.h>
#include <euca_auth.h>
#include <backing.h>
#include <diskutil.h>
#include <iscsi.h>
#include <sensor.h>
#include <euca_string.h>

#include "handlers.h"
#include "xml.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define HYPERVISOR_URI                        "qemu:///system" /**< Defines the Hypervisor URI to use with KVM */

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

// coming from handlers.c
extern sem *hyp_sem;
extern sem *inst_sem;
extern bunchOfInstances *global_instances;

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
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int doInitialize(struct nc_state_t *nc);
static void *rebooting_thread(void *arg);
static int doRebootInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId);
static int doGetConsoleOutput(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char **consoleOutput);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              CALLBACK STRUCTURE                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! KVM LIBVIRT operation handlers
struct handlers kvm_libvirt_handlers = {
    .name = "kvm",
    .doInitialize = doInitialize,
    .doDescribeInstances = NULL,
    .doRunInstance = NULL,
    .doTerminateInstance = NULL,
    .doRebootInstance = doRebootInstance,
    .doGetConsoleOutput = doGetConsoleOutput,
    .doDescribeResource = NULL,
    .doStartNetwork = NULL,
    .doAssignAddress = NULL,
    .doPowerDown = NULL,
    .doAttachVolume = NULL,
    .doDetachVolume = NULL,
    .doDescribeSensors = NULL
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
//! Initialize the NC state structure for the KVM hypervisor.
//!
//! @param[in] nc a pointer to the NC state structure to initialize
//!
//! @return Always return EUCA_OK
//!
static int doInitialize(struct nc_state_t *nc)
{
#define GET_VALUE(_name, _var)                                                                         \
{                                                                                                      \
	if (get_value (s, (_name), &(_var))) {                                                             \
		logprintfl (EUCAFATAL, "did not find %s in output from %s\n", (_name), nc->get_info_cmd_path); \
		EUCA_FREE(s);                                                                                  \
		return (EUCA_FATAL_ERROR);                                                                     \
	}                                                                                                  \
}

    char *s = NULL;

    // set up paths of Eucalyptus commands NC relies on
    snprintf(nc->get_info_cmd_path, MAX_PATH, EUCALYPTUS_GET_KVM_INFO, nc->home, nc->home);
    strcpy(nc->uri, HYPERVISOR_URI);
    nc->convert_to_disk = 1;
    nc->capability = HYPERVISOR_HARDWARE;   //! @todo indicate virtio support?

    s = system_output(nc->get_info_cmd_path);

    GET_VALUE("nr_cores", nc->cores_max);
    GET_VALUE("total_memory", nc->mem_max);
    EUCA_FREE(s);

    // we leave 256M to the host
    nc->mem_max -= 256;

    return EUCA_OK;
}

//!
//! Defines the thread that does the actual reboot of an instance.
//!
//! @param[in] arg a transparent pointer to the argument passed to this thread handler
//!
//! @return Always return NULL
//!
static void *rebooting_thread(void *arg)
{
    virConnectPtr *conn;
    ncInstance *instance = (ncInstance *) arg;
    char resourceName[1][MAX_SENSOR_NAME_LEN] = { {0} };
    char resourceAlias[1][MAX_SENSOR_NAME_LEN] = { {0} };

    logprintfl(EUCADEBUG, "[%s] spawning rebooting thread\n", instance->instanceId);
    char *xml = file2str(instance->libvirtFilePath);
    if (xml == NULL) {
        logprintfl(EUCAERROR, "[%s] cannot obtain instance XML file %s\n", instance->instanceId, instance->libvirtFilePath);
        return NULL;
    }

    conn = check_hypervisor_conn();
    if (!conn) {
        logprintfl(EUCAERROR, "[%s] cannot restart instance %s, abandoning it\n", instance->instanceId, instance->instanceId);
        change_state(instance, SHUTOFF);
        EUCA_FREE(xml);
        return NULL;
    }

    sem_p(hyp_sem);
    virDomainPtr dom = virDomainLookupByName(*conn, instance->instanceId);
    sem_v(hyp_sem);
    if (dom == NULL) {
        EUCA_FREE(xml);
        return NULL;
    }

    sem_p(hyp_sem);
    // for KVM, must stop and restart the instance
    logprintfl(EUCADEBUG, "[%s] destroying domain\n", instance->instanceId);
    int error = virDomainDestroy(dom);  // @todo change to Shutdown? is this synchronous?
    virDomainFree(dom);
    sem_v(hyp_sem);

    if (error) {
        EUCA_FREE(xml);
        return NULL;
    }
    // Add a shift to values of three of the metrics: ones that
    // drop back to zero after a reboot. The shift, which is based
    // on the latest value, ensures that values sent upstream do
    // not go backwards .
    sensor_shift_metric(instance->instanceId, "CPUUtilization");
    sensor_shift_metric(instance->instanceId, "NetworkIn");
    sensor_shift_metric(instance->instanceId, "NetworkOut");

    // domain is now shut down, create a new one with the same XML
    sem_p(hyp_sem);
    logprintfl(EUCAINFO, "[%s] rebooting\n", instance->instanceId);
    dom = virDomainCreateLinux(*conn, xml, 0);
    sem_v(hyp_sem);
    EUCA_FREE(xml);

    euca_strncpy(resourceName[0], instance->instanceId, MAX_SENSOR_NAME_LEN);
    sensor_refresh_resources(resourceName, resourceAlias, 1);   // refresh stats so we set base value accurately

    char *remoteDevStr = NULL;
    // re-attach each volume previously attached
    for (int i = 0; i < EUCA_MAX_VOLUMES; ++i) {
        ncVolume *volume = &instance->volumes[i];
        if (strcmp(volume->stateName, VOL_STATE_ATTACHED) && strcmp(volume->stateName, VOL_STATE_ATTACHING))
            continue;           // skip the entry unless attached or attaching

        logprintfl(EUCADEBUG, "[%s] volumes [%d] = '%s'\n", instance->instanceId, i, volume->stateName);
        char *xml = NULL;
        int rc = 0;
        // get credentials, decrypt them
        remoteDevStr = get_iscsi_target(volume->remoteDev);
        if (!remoteDevStr || !strstr(remoteDevStr, "/dev")) {
            logprintfl(EUCAERROR, "[%s] failed to get local name of host iscsi device when re-attaching\n", instance->instanceId);
            rc = 1;
        } else {
            // set the path
            char path[MAX_PATH];
            char lpath[MAX_PATH];
            snprintf(path, sizeof(path), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volume->volumeId);  // vol-XXX.xml
            snprintf(lpath, sizeof(lpath), EUCALYPTUS_VOLUME_LIBVIRT_XML_PATH_FORMAT, instance->instancePath, volume->volumeId);    // vol-XXX-libvirt.xml

            // read in libvirt XML, which may have been modified by the hook above
            xml = file2str(lpath);
            if (xml == NULL) {
                logprintfl(EUCAERROR, "[%s][%s] failed to read volume XML from %s\n", instance->instanceId, volume->volumeId, lpath);
                rc = 1;
            }
        }

        EUCA_FREE(remoteDevStr);

        if (!rc) {
            // zhill - wrap with retry in case libvirt is dumb.
            int err = 0;
#define REATTACH_RETRIES 3
            for (int i = 1; i < REATTACH_RETRIES; i++) {
                // protect libvirt calls because we've seen problems during concurrent libvirt invocations
                sem_p(hyp_sem);
                err = virDomainAttachDevice(dom, xml);
                sem_v(hyp_sem);
                if (err) {
                    logprintfl(EUCAERROR, "[%s][%s] failed to reattach volume (attempt %d of %d)\n", instance->instanceId, volume->volumeId, i,
                               REATTACH_RETRIES);
                    logprintfl(EUCADEBUG, "[%s][%s] error from virDomainAttachDevice: %d xml: %s\n", instance->instanceId, volume->volumeId, err,
                               xml);
                    sleep(3);   // sleep a bit and retry
                } else {
                    logprintfl(EUCAINFO, "[%s][%s] volume reattached as '%s'\n", instance->instanceId, volume->volumeId, volume->localDevReal);
                    break;
                }
            }
            int log_level_for_devstring = EUCATRACE;
            if (err)
                log_level_for_devstring = EUCADEBUG;
            logprintfl(log_level_for_devstring, "[%s][%s] remote device string: %s\n", instance->instanceId, volume->volumeId, volume->remoteDev);
        }

        EUCA_FREE(xml);
    }

    if (dom == NULL) {
        logprintfl(EUCAERROR, "[%s] failed to restart instance\n", instance->instanceId);
        change_state(instance, SHUTOFF);
        return NULL;
    }

    sem_p(hyp_sem);
    virDomainFree(dom);
    sem_v(hyp_sem);
    return NULL;
}

//!
//! Handles the reboot request of an instance.
//!
//! @param[in] nc a pointer to the NC state structure to initialize
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR
//!         and EUCA_FATAL_ERROR.
//!
static int doRebootInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId)
{
    sem_p(inst_sem);
    ncInstance *instance = find_instance(&global_instances, instanceId);
    sem_v(inst_sem);
    if (instance == NULL) {
        logprintfl(EUCAERROR, "[%s] cannot find instance\n", instanceId);
        return EUCA_ERROR;
    }

    pthread_t tcb;
    // since shutdown/restart may take a while, we do them in a thread
    if (pthread_create(&tcb, NULL, rebooting_thread, (void *)instance)) {
        logprintfl(EUCAERROR, "[%s] failed to spawn a reboot thread\n", instanceId);
        return EUCA_FATAL_ERROR;
    }
    if (pthread_detach(tcb)) {
        logprintfl(EUCAERROR, "[%s] failed to detach the rebooting thread\n", instanceId);
        return EUCA_FATAL_ERROR;
    }

    return EUCA_OK;
}

//!
//! Handles the console output retrieval request.
//!
//! @param[in]  nc a pointer to the NC state structure to initialize
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[out] consoleOutput a pointer to the unallocated string that will contain the output
//!
//! @return EUCA_OK on success or EUCA_ERROR and EUCA_NOT_FOUND_ERROR on failure.
//!
static int doGetConsoleOutput(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char **consoleOutput)
{
    char *console_output = NULL, *console_append = NULL, *console_main = NULL;
    char console_file[MAX_PATH], userId[48];
    int rc, fd, ret, readsize;
    struct stat statbuf;
    ncInstance *instance = NULL;

    *consoleOutput = NULL;
    readsize = 64 * 1024;

    // find the instance record
    sem_p(inst_sem);
    instance = find_instance(&global_instances, instanceId);
    if (instance) {
        snprintf(console_file, 1024, "%s/console.append.log", instance->instancePath);
        snprintf(userId, 48, "%s", instance->userId);
    }
    sem_v(inst_sem);

    if (!instance) {
        logprintfl(EUCAERROR, "[%s] cannot locate instance\n", instanceId);
        return (EUCA_NOT_FOUND_ERROR);
    }
    // read from console.append.log if it exists into dynamically allocated 4K console_append buffer
    rc = stat(console_file, &statbuf);
    if (rc >= 0) {
        if (diskutil_ch(console_file, nc->admin_user_id, nc->admin_user_id, 0) != EUCA_OK) {
            logprintfl(EUCAERROR, "[%s] failed to change ownership of %s\n", instanceId, console_file);
            return (EUCA_ERROR);
        }
        fd = open(console_file, O_RDONLY);
        if (fd >= 0) {
            console_append = EUCA_ZALLOC(4096, sizeof(char));
            if (console_append) {
                rc = read(fd, console_append, (4096) - 1);
                close(fd);
            }
        }
    }

    sem_p(inst_sem);
    snprintf(console_file, MAX_PATH, "%s/console.log", instance->instancePath);
    sem_v(inst_sem);

    // read the last 64K from console.log or the whole file, if smaller, into dynamically allocated 64K console_main buffer
    rc = stat(console_file, &statbuf);
    if (rc >= 0) {
        if (diskutil_ch(console_file, nc->admin_user_id, nc->admin_user_id, 0) != EUCA_OK) {
            logprintfl(EUCAERROR, "[%s] failed to change ownership of %s\n", instanceId, console_file);
            EUCA_FREE(console_append);
            return (EUCA_ERROR);
        }
        fd = open(console_file, O_RDONLY);
        if (fd >= 0) {
            rc = lseek(fd, (off_t) (-1 * readsize), SEEK_END);
            if (rc < 0) {
                rc = lseek(fd, (off_t) 0, SEEK_SET);
                if (rc < 0) {
                    logprintfl(EUCAERROR, "[%s] cannot seek to beginning of file\n", instanceId);
                    if (console_append)
                        free(console_append);
                    close(fd);
                    return (EUCA_ERROR);
                }
            }
            console_main = EUCA_ZALLOC(readsize, sizeof(char));
            if (console_main) {
                rc = read(fd, console_main, (readsize) - 1);
                close(fd);
            }
        } else {
            logprintfl(EUCAERROR, "[%s] cannot open '%s' read-only\n", instanceId, console_file);
        }
    } else {
        logprintfl(EUCAERROR, "[%s] cannot stat console_output file '%s'\n", instanceId, console_file);
    }

    // concatenate console_append with console_main, base64-encode this, and put into dynamically allocated buffer consoleOutput
    ret = EUCA_ERROR;
    console_output = EUCA_ZALLOC((readsize) + 4096, sizeof(char));
    if (console_output) {
        if (console_append) {
            strncat(console_output, console_append, 4096);
        }
        if (console_main) {
            strncat(console_output, console_main, readsize);
        }
        *consoleOutput = base64_enc((unsigned char *)console_output, strlen(console_output));
        ret = EUCA_OK;
    }

    EUCA_FREE(console_append);
    EUCA_FREE(console_main);
    EUCA_FREE(console_output);

    return (ret);
}
