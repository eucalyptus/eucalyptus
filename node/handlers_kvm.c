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
#define __USE_GNU                      /* strnlen */
#include <string.h>                    /* strlen, strcpy */
#include <time.h>
#include <sys/types.h>                 /* fork */
#include <sys/wait.h>                  /* waitpid */
#include <unistd.h>
#include <assert.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>                    /* SIGINT */
#include <sys/stat.h>
#include <fcntl.h>

#include <eucalyptus.h>
#include <ipc.h>
#include <misc.h>
#include <euca_auth.h>
#include <backing.h>
#include <diskutil.h>
#include <iscsi.h>
#include <sensor.h>
#include <euca_string.h>

#include "handlers.h"
#include "xml.h"
#include "vbr.h" // vbr_parse

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
static int doMigrateInstance (struct nc_state_t * nc, ncMetadata * pMeta, ncInstance * instance, char * sourceNodeName, char * destNodeName, char * credentials);

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
    .doDescribeSensors = NULL,
    .doModifyNode = NULL,
    .doMigrateInstance = doMigrateInstance
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
#define GET_VALUE(_name, _var)                                                           \
{                                                                                        \
	if (get_value (s, (_name), &(_var))) {                                               \
		LOGFATAL("did not find %s in output from %s\n", (_name), nc->get_info_cmd_path); \
		EUCA_FREE(s);                                                                    \
		return (EUCA_FATAL_ERROR);                                                       \
	}                                                                                    \
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
    return (EUCA_OK);
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
#define REATTACH_RETRIES      3

    int i = 0;
    int err = 0;
    int error = 0;
    int rc = 0;
    int log_level_for_devstring = EUCA_LOG_TRACE;
    char *xml = NULL;
    char *remoteDevStr = NULL;
    char path[MAX_PATH] = "";
    char lpath[MAX_PATH] = "";
    char resourceName[1][MAX_SENSOR_NAME_LEN] = { {0} };
    char resourceAlias[1][MAX_SENSOR_NAME_LEN] = { {0} };
    ncVolume *volume = NULL;
    ncInstance *instance = ((ncInstance *) arg);
    virDomainPtr dom = NULL;
    virConnectPtr *conn = NULL;

    LOGDEBUG("[%s] spawning rebooting thread\n", instance->instanceId);
    if ((xml = file2str(instance->libvirtFilePath)) == NULL) {
        LOGERROR("[%s] cannot obtain instance XML file %s\n", instance->instanceId, instance->libvirtFilePath);
        return NULL;
    }

    if ((conn = check_hypervisor_conn()) == NULL) {
        LOGERROR("[%s] cannot restart instance %s, abandoning it\n", instance->instanceId, instance->instanceId);
        change_state(instance, SHUTOFF);
        EUCA_FREE(xml);
        return NULL;
    }

    sem_p(hyp_sem);
    {
        dom = virDomainLookupByName(*conn, instance->instanceId);
    }
    sem_v(hyp_sem);

    if (dom == NULL) {
        EUCA_FREE(xml);
        return NULL;
    }

    sem_p(hyp_sem);
    {
        // for KVM, must stop and restart the instance
        LOGDEBUG("[%s] destroying domain\n", instance->instanceId);
        error = virDomainDestroy(dom); // @todo change to Shutdown? is this synchronous?
        virDomainFree(dom);
    }
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
    {
        LOGINFO("[%s] rebooting\n", instance->instanceId);
        dom = virDomainCreateLinux(*conn, xml, 0);
    }
    sem_v(hyp_sem);
    EUCA_FREE(xml);

    euca_strncpy(resourceName[0], instance->instanceId, MAX_SENSOR_NAME_LEN);
    sensor_refresh_resources(resourceName, resourceAlias, 1);   // refresh stats so we set base value accurately

    // re-attach each volume previously attached
    for (i = 0; i < EUCA_MAX_VOLUMES; ++i) {
        volume = &instance->volumes[i];
        if (strcmp(volume->stateName, VOL_STATE_ATTACHED) && strcmp(volume->stateName, VOL_STATE_ATTACHING))
            continue;                  // skip the entry unless attached or attaching

        LOGDEBUG("[%s] volumes [%d] = '%s'\n", instance->instanceId, i, volume->stateName);

        // get credentials, decrypt them
        remoteDevStr = get_iscsi_target(volume->remoteDev);
        if (!remoteDevStr || !strstr(remoteDevStr, "/dev")) {
            LOGERROR("[%s] failed to get local name of host iscsi device when re-attaching\n", instance->instanceId);
            rc = 1;
        } else {
            // set the path
            snprintf(path, sizeof(path), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volume->volumeId);  // vol-XXX.xml
            snprintf(lpath, sizeof(lpath), EUCALYPTUS_VOLUME_LIBVIRT_XML_PATH_FORMAT, instance->instancePath, volume->volumeId);    // vol-XXX-libvirt.xml

            // read in libvirt XML, which may have been modified by the hook above
            if ((xml = file2str(lpath)) == NULL) {
                LOGERROR("[%s][%s] failed to read volume XML from %s\n", instance->instanceId, volume->volumeId, lpath);
                rc = 1;
            }
        }

        EUCA_FREE(remoteDevStr);

        if (!rc) {
            // zhill - wrap with retry in case libvirt is dumb.
            err = 0;
            for (i = 1; i < REATTACH_RETRIES; i++) {
                // protect libvirt calls because we've seen problems during concurrent libvirt invocations
                sem_p(hyp_sem);
                {
                    err = virDomainAttachDevice(dom, xml);
                }
                sem_v(hyp_sem);

                if (err) {
                    LOGERROR("[%s][%s] failed to reattach volume (attempt %d of %d)\n", instance->instanceId, volume->volumeId, i, REATTACH_RETRIES);
                    LOGDEBUG("[%s][%s] error from virDomainAttachDevice: %d xml: %s\n", instance->instanceId, volume->volumeId, err, xml);
                    sleep(3);          // sleep a bit and retry
                } else {
                    LOGINFO("[%s][%s] volume reattached as '%s'\n", instance->instanceId, volume->volumeId, volume->localDevReal);
                    break;
                }
            }

            log_level_for_devstring = EUCA_LOG_TRACE;
            if (err)
                log_level_for_devstring = EUCA_LOG_DEBUG;
            EUCALOG(log_level_for_devstring, "[%s][%s] remote device string: %s\n", instance->instanceId, volume->volumeId, volume->remoteDev);
        }

        EUCA_FREE(xml);
    }

    if (dom == NULL) {
        LOGERROR("[%s] failed to restart instance\n", instance->instanceId);
        change_state(instance, SHUTOFF);
        return NULL;
    }

    sem_p(hyp_sem);
    {
        virDomainFree(dom);
    }
    sem_v(hyp_sem);
    return NULL;

#undef REATTACH_RETRIES
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
    pthread_t tcb = { 0 };
    ncInstance *instance = NULL;

    sem_p(inst_sem);
    {
        instance = find_instance(&global_instances, instanceId);
    }
    sem_v(inst_sem);

    if (instance == NULL) {
        LOGERROR("[%s] cannot find instance\n", instanceId);
        return (EUCA_ERROR);
    }
    // since shutdown/restart may take a while, we do them in a thread
    if (pthread_create(&tcb, NULL, rebooting_thread, (void *)instance)) {
        LOGERROR("[%s] failed to spawn a reboot thread\n", instanceId);
        return (EUCA_FATAL_ERROR);
    }

    if (pthread_detach(tcb)) {
        LOGERROR("[%s] failed to detach the rebooting thread\n", instanceId);
        return (EUCA_FATAL_ERROR);
    }

    return (EUCA_OK);
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
    int rc = 0;
    int fd = 0;
    int ret = EUCA_OK;
    int readsize = 0;
    char *console_output = NULL;
    char *console_append = NULL;
    char *console_main = NULL;
    char console_file[MAX_PATH] = "";
    char userId[48] = "";
    ncInstance *instance = NULL;
    struct stat statbuf = { 0 };

    *consoleOutput = NULL;
    readsize = 64 * 1024;

    // find the instance record
    sem_p(inst_sem);
    {
        if ((instance = find_instance(&global_instances, instanceId)) != NULL) {
            snprintf(console_file, 1024, "%s/console.append.log", instance->instancePath);
            snprintf(userId, 48, "%s", instance->userId);
        }
    }
    sem_v(inst_sem);

    if (!instance) {
        LOGERROR("[%s] cannot locate instance\n", instanceId);
        return (EUCA_NOT_FOUND_ERROR);
    }
    // read from console.append.log if it exists into dynamically allocated 4K console_append buffer
    if ((rc = stat(console_file, &statbuf)) >= 0) {
        if (diskutil_ch(console_file, nc->admin_user_id, nc->admin_user_id, 0) != EUCA_OK) {
            LOGERROR("[%s] failed to change ownership of %s\n", instanceId, console_file);
            return (EUCA_ERROR);
        }

        if ((fd = open(console_file, O_RDONLY)) >= 0) {
            if ((console_append = EUCA_ZALLOC(4096, sizeof(char))) != NULL) {
                rc = read(fd, console_append, (4096) - 1);
            }
            close(fd);
        }
    }

    sem_p(inst_sem);
    {
        snprintf(console_file, MAX_PATH, "%s/console.log", instance->instancePath);
    }
    sem_v(inst_sem);

    // read the last 64K from console.log or the whole file, if smaller, into dynamically allocated 64K console_main buffer
    if ((rc = stat(console_file, &statbuf)) >= 0) {
        if (diskutil_ch(console_file, nc->admin_user_id, nc->admin_user_id, 0) != EUCA_OK) {
            LOGERROR("[%s] failed to change ownership of %s\n", instanceId, console_file);
            EUCA_FREE(console_append);
            return (EUCA_ERROR);
        }

        if ((fd = open(console_file, O_RDONLY)) >= 0) {
            if ((rc = lseek(fd, (off_t) (-1 * readsize), SEEK_END)) < 0) {
                if ((rc = lseek(fd, (off_t) 0, SEEK_SET)) < 0) {
                    LOGERROR("[%s] cannot seek to beginning of file\n", instanceId);
                    if (console_append)
                        EUCA_FREE(console_append);
                    close(fd);
                    return (EUCA_ERROR);
                }
            }

            if ((console_main = EUCA_ZALLOC(readsize, sizeof(char))) != NULL) {
                rc = read(fd, console_main, (readsize) - 1);
            }
            close(fd);
        } else {
            LOGERROR("[%s] cannot open '%s' read-only\n", instanceId, console_file);
        }
    } else {
        LOGERROR("[%s] cannot stat console_output file '%s'\n", instanceId, console_file);
    }

    // concatenate console_append with console_main, base64-encode this, and put into dynamically allocated buffer consoleOutput
    ret = EUCA_ERROR;
    if ((console_output = EUCA_ZALLOC((readsize) + 4096, sizeof(char))) != NULL) {
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

//!                                                                                                                                                                              
//! TODO: doxygen
//!
static int doMigrateInstance (struct nc_state_t * nc, ncMetadata * pMeta, ncInstance * instance_req, char * sourceNodeName, char * destNodeName, char * credentials)
{
    int ret = EUCA_OK;

    if (!strcmp(pMeta->nodeName, sourceNodeName)) {

        // locate the instance structure
        ncInstance * instance;
        sem_p(inst_sem);
        {
            instance = find_instance(&global_instances, instance_req->instanceId);
        }
        sem_v(inst_sem);
        if (instance == NULL) {
            LOGERROR("[%s] cannot find instance\n", instance_req->instanceId);
            goto failed_src;
        }
        instance->migration_state = MIGRATION_PREPARING;
        euca_strncpy(instance->migration_src, sourceNodeName, HOSTNAME_SIZE);
        euca_strncpy(instance->migration_dst, destNodeName, HOSTNAME_SIZE);
        LOGINFO("[%s] migration source initating %s->%s\n", instance->instanceId, instance->migration_src, instance->migration_dst);

        virDomainPtr dom = NULL;
        virConnectPtr *conn = NULL;
        
        if ((conn = check_hypervisor_conn()) == NULL) {
            logprintfl(EUCAERROR, "[%s] cannot migrate instance %s (failed to connect to hypervisor), giving up\n", instance->instanceId, instance->instanceId);
            goto failed_src;
        }
        
        sem_p(hyp_sem);
        {
            dom = virDomainLookupByName(*conn, instance->instanceId);
        }
        sem_v(hyp_sem);
        
        if (dom == NULL) {
            logprintfl(EUCAERROR, "[%s] cannot migrate instance %s (failed to find domain), giving up\n", instance->instanceId, instance->instanceId);
            goto failed_src;
        }
        
        char duri [1024];
        snprintf (duri, sizeof(duri), "qemu+ssh://%s/system", destNodeName);
        virConnectPtr dconn = NULL;
        dconn = virConnectOpen(duri);
        if (dconn==NULL) {
            logprintfl(EUCAERROR, "[%s] cannot migrate instance %s (failed to connect to remote), giving up\n", instance->instanceId, instance->instanceId);
            goto failed_src;
        }
        
        instance->migration_state = MIGRATION_IN_PROGRESS;
        virDomain * ddom = virDomainMigrate(dom, 
                                            dconn, 
                                            VIR_MIGRATE_LIVE | VIR_MIGRATE_NON_SHARED_DISK, 
                                            NULL, // new name on destination (optional)
                                            NULL, // destination URI as seen from source (optional)
                                            0L); // bandwidth limitation (0 => unlimited)
        virConnectClose(dconn);

        if (ddom == NULL) {
            LOGERROR("[%s] cannot migrate instance %s, giving up\n", instance->instanceId, instance->instanceId);
            goto failed_src;
        }
        virDomainFree(ddom);
        virDomainFree(dom);

        goto out;
    failed_src:
        ret = EUCA_ERROR;

    } else if (!strcmp(pMeta->nodeName, destNodeName)) {

        // allocate a new instance struct
        ncInstance *instance = clone_instance(instance_req);
        if (instance == NULL) {
            LOGERROR("[%s] could not allocate instance struct\n", instance_req->instanceId);
            goto failed_dest;
        }
        instance->migration_state = MIGRATION_PREPARING;
        euca_strncpy(instance->migration_src, sourceNodeName, HOSTNAME_SIZE);
        euca_strncpy(instance->migration_dst, destNodeName, HOSTNAME_SIZE);
        LOGINFO("[%s] migration destination initating %s->%s\n", instance->instanceId, instance->migration_src, instance->migration_dst);
        
        int error;
        
        if (vbr_parse(&(instance->params), pMeta) != EUCA_OK) {
            goto failed_dest;
        }
        
        // set up networking
        char *brname = NULL;
        if ((error = vnetStartNetwork(nc->vnetconfig, instance->ncnet.vlan, NULL, NULL, NULL, &brname)) != EUCA_OK) {
            logprintfl(EUCAERROR, "[%s] start network failed for instance, terminating it\n", instance->instanceId);
            EUCA_FREE(brname);
            goto failed_dest;
        }

        // TODO: move stuff in startup_thread() into a function?

        instance->combinePartitions = nc->convert_to_disk;
        instance->do_inject_key = nc->do_inject_key;

        if ((error = create_instance_backing(instance))) {
            logprintfl(EUCAERROR, "[%s] failed to prepare images for instance (error=%d)\n", instance->instanceId, error);
            goto failed_dest;
        }
        
        instance->bootTime = time(NULL); // otherwise nc_state.booting_cleanup_threshold will kick in
        change_state(instance, BOOTING); // not STAGING, since in that mode we don't poll hypervisor for info
        instance->migration_state = MIGRATION_READY;

        sem_p(inst_sem);
        error = add_instance(&global_instances, instance);
        copy_instances();
        sem_v(inst_sem);
        if (error) {
            LOGERROR("[%s] could not save instance struct\n", instance->instanceId);
            goto failed_dest;
        }

        goto out;
    failed_dest:
        ret = EUCA_ERROR;
        EUCA_FREE(instance);

    } else {
        logprintfl(EUCAERROR, "unexpected migration request (node %s is neither source nor destination)\n", pMeta->nodeName);
        ret = EUCA_ERROR;
    }

 out:

    return ret;
}
