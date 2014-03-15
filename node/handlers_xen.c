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
//! @file node/handlers_xen.c
//! This implements the operations handlers supported by the XEN hypervisor.
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
#include <sensor.h>
#include <euca_string.h>

#include "handlers.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define HYPERVISOR_URI                           "xen:///"  ///< Defines the Hypervisor URI to use with KVM

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
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int doInitialize(struct nc_state_t *nc);
static int doRebootInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId);
static int doGetConsoleOutput(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char **consoleOutput);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              CALLBACK STRUCTURE                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! XEN LIBVIRT operation handlers
struct handlers xen_libvirt_handlers = {
    .name = "xen",
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
    .doMigrateInstances = NULL,        // no support on Xen for instance migration, currently
    .doStartInstance = NULL,
    .doStopInstance = NULL,
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
//! Initialize the NC state structure for the XEN hypervisor.
//!
//! @param[in] nc a pointer to the NC state structure to initialize
//!
//! @return EUCA_OK on success or EUCA_FATAL_ERROR on failure.
//!
static int doInitialize(struct nc_state_t *nc)
{
    char *s = NULL;
    virNodeInfo ni = { {0} };
    long long dom0_min_mem = 0;

    // set up paths of Eucalyptus commands NC relies on
    snprintf(nc->get_info_cmd_path, EUCA_MAX_PATH, EUCALYPTUS_GET_XEN_INFO, nc->home, nc->home);
    snprintf(nc->virsh_cmd_path, EUCA_MAX_PATH, EUCALYPTUS_VIRSH, nc->home);
    snprintf(nc->xm_cmd_path, EUCA_MAX_PATH, EUCALYPTUS_XM);
    snprintf(nc->detach_cmd_path, EUCA_MAX_PATH, EUCALYPTUS_DETACH, nc->home, nc->home);
    strcpy(nc->uri, HYPERVISOR_URI);
    nc->convert_to_disk = 0;
    nc->capability = HYPERVISOR_XEN_AND_HARDWARE;   //! @todo set to XEN_PARAVIRTUALIZED if on older Xen kernel

    // check connection is fresh
    virConnectPtr conn = lock_hypervisor_conn();
    if (conn == NULL) {
        return (EUCA_FATAL_ERROR);
    }
    // get resources
    if (virNodeGetInfo(conn, &ni)) {
        LOGFATAL("failed to discover resources\n");
        unlock_hypervisor_conn();
        return (EUCA_FATAL_ERROR);
    }
    unlock_hypervisor_conn();

    // dom0-min-mem has to come from xend config file
    s = system_output(nc->get_info_cmd_path);
    if (get_value(s, "dom0-min-mem", &dom0_min_mem)) {
        LOGFATAL("did not find dom0-min-mem in output from %s\n", nc->get_info_cmd_path);
        EUCA_FREE(s);
        return (EUCA_FATAL_ERROR);
    }
    EUCA_FREE(s);

    // calculate the available memory
    nc->phy_max_mem = ni.memory / 1024 - 32 - dom0_min_mem;

    // calculate the available cores
    nc->phy_max_cores = ni.cpus;
    return (EUCA_OK);
}

//!
//! Handles the reboot instance request.
//!
//! @param[in] nc a pointer to the initialized NC state structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_OK on success or EUCA_NOT_FOUND_ERROR on failure.
//!
static int doRebootInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId)
{
    int err = 0;
    char resourceName[1][MAX_SENSOR_NAME_LEN] = { {0} };
    char resourceAlias[1][MAX_SENSOR_NAME_LEN] = { {0} };
    ncInstance *instance = NULL;
    virConnectPtr conn = NULL;
    virDomainPtr dom = NULL;

    sem_p(inst_sem);
    {
        instance = find_instance(&global_instances, instanceId);
    }
    sem_v(inst_sem);

    if (instance == NULL)
        return (EUCA_NOT_FOUND_ERROR);

    /* reboot the Xen domain */
    if ((conn = lock_hypervisor_conn()) != NULL) {
        dom = virDomainLookupByName(conn, instanceId);
        if (dom) {
            // stop polling so values after reboot are not picked up until after we shift the metric
            sensor_suspend_polling();
            err = virDomainReboot(dom, 0);
            if (err == 0) {
                LOGINFO("[%s] rebooting Xen domain for instance\n", instanceId);
            }
            virDomainFree(dom);        //! @todo is this necessary?

            // Add a shift to values of three of the metrics: ones that
            // drop back to zero after a reboot. The shift, which is based
            // on the latest value, ensures that values sent upstream do
            // not go backwards .
            sensor_shift_metric(instance->instanceId, "CPUUtilization");
            sensor_shift_metric(instance->instanceId, "NetworkIn");
            sensor_shift_metric(instance->instanceId, "NetworkOut");

            euca_strncpy(resourceName[0], instance->instanceId, MAX_SENSOR_NAME_LEN);
            sensor_refresh_resources(resourceName, resourceAlias, 1);   // refresh stats immediately to minimize loss
            sensor_resume_polling();   // now that metrics have been shifted, resume polling
        } else {
            if (instance->state != BOOTING && instance->state != STAGING) {
                LOGWARN("[%s] domain to be rebooted not running on hypervisor\n", instanceId);
            }
        }
        unlock_hypervisor_conn();
    }

    return (EUCA_OK);
}

//!
//! Handles the console output retrieval request.
//!
//! @param[in]  nc a pointer to the initialized NC state structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[out] consoleOutput a pointer to the unallocated string that will contain the output
//!
//! @return EUCA_OK on success or proper error code. Known error code returned
//!         include: EUCA_ERROR, EUCA_NOT_FOUND_ERROR and EUCA_MEMORY_ERROR.
//!
static int doGetConsoleOutput(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char **consoleOutput)
{
    int rc = 0;
    int fd = 0;
    int ret = EUCA_OK;
    int bufsize = 0;
    int pid = 0;
    int status = 0;
    int count = 0;
    char *console_output = NULL;
    char *console_append = NULL;
    char *console_main = NULL;
    char *tmp = NULL;
    char console_file[EUCA_MAX_PATH] = "";
    char dest_file[EUCA_MAX_PATH] = "";
    char userId[48] = "";
    fd_set rfds = { {0} };
    ncInstance *instance = NULL;
    struct stat statbuf = { 0 };
    struct timeval tv = { 0 };

    *consoleOutput = NULL;

    // find the instance record
    sem_p(inst_sem);
    {
        if ((instance = find_instance(&global_instances, instanceId)) != NULL) {
            snprintf(userId, 48, "%s", instance->userId);
            snprintf(console_file, 1024, "%s/console.append.log", instance->instancePath);
        }
    }
    sem_v(inst_sem);

    if (instance == NULL) {
        LOGERROR("[%s] cannot locate instance\n", instanceId);
        return (EUCA_NOT_FOUND_ERROR);
    }

    if ((rc = stat(console_file, &statbuf)) >= 0) {
        if ((fd = open(console_file, O_RDONLY)) >= 0) {
            if ((console_append = EUCA_ZALLOC(4096, sizeof(char))) != NULL) {
                rc = read(fd, console_append, (4096) - 1);
                console_append[((4096) - 1)] = '\0';
            }
            close(fd);
        }
    }

    bufsize = sizeof(char) * 1024 * 64;
    if ((console_main = EUCA_ZALLOC(bufsize, sizeof(char))) == NULL) {
        LOGERROR("[%s] out of memory!\n", instanceId);
        EUCA_FREE(console_append);
        return (EUCA_MEMORY_ERROR);
    }

    if (getuid() != 0) {
        snprintf(console_file, EUCA_MAX_PATH, "/var/log/xen/console/guest-%s.log", instanceId);
        snprintf(dest_file, EUCA_MAX_PATH, "%s/console.log", instance->instancePath);
        LOGDEBUG("[%s] executing '%s cp %s %s'\n", instanceId, nc->rootwrap_cmd_path, console_file, dest_file);
        if ((rc = euca_execlp(NULL, nc->rootwrap_cmd_path, "cp", console_file, dest_file, NULL)) == EUCA_OK) {
            // was able to copy xen guest console file, read it
            LOGDEBUG("[%s] executing '%s chown %s:%s %s'\n", instanceId, nc->rootwrap_cmd_path, nc->admin_user_id, nc->admin_user_id, dest_file);
            if ((rc = euca_execlp(NULL, nc->rootwrap_cmd_path, "chown", nc->admin_user_id, nc->admin_user_id, dest_file, NULL)) == EUCA_OK) {
                if ((tmp = file2str_seek(dest_file, bufsize, 1)) != NULL) {
                    snprintf(console_main, bufsize, "%s", tmp);
                    EUCA_FREE(tmp);
                } else {
                    snprintf(console_main, bufsize, "NOT SUPPORTED");
                }
            } else {
                LOGERROR("[%s] cmd '%s chown %s:%s %s' failed %d\n", instanceId, nc->rootwrap_cmd_path, nc->admin_user_id, nc->admin_user_id, dest_file, rc);
                snprintf(console_main, bufsize, "NOT SUPPORTED");
            }
        } else {
            LOGERROR("[%s] cmd '%s cp %s %s' failed %d\n", instanceId, nc->rootwrap_cmd_path, console_file, dest_file, rc);
            snprintf(console_main, bufsize, "NOT SUPPORTED");
        }
    } else {
        snprintf(console_file, EUCA_MAX_PATH, "/tmp/consoleOutput.%s", instanceId);

        if ((pid = fork()) == 0) {
            if ((fd = open(console_file, O_WRONLY | O_TRUNC | O_CREAT, 0644)) >= 0) {
                dup2(fd, 2);
                dup2(2, 1);
                close(0);
                //! @todo test virsh console:
                // rc = execl(rootwrap_command_path, rootwrap_command_path, "virsh", "console", instanceId, NULL);
                rc = execl("/usr/sbin/xm", "/usr/sbin/xm", "console", instanceId, NULL);
                fprintf(stderr, "execl() failed\n");
                close(fd);
            }
            exit(0);
        } else {
            count = 0;
            while (count < 10000 && stat(console_file, &statbuf) < 0) {
                count++;
            }

            if ((fd = open(console_file, O_RDONLY)) < 0) {
                LOGERROR("[%s] could not open consoleOutput file %s for reading\n", instanceId, console_file);
            } else {
                FD_ZERO(&rfds);
                FD_SET(fd, &rfds);
                tv.tv_sec = 0;
                tv.tv_usec = 500000;
                rc = select(1, &rfds, NULL, NULL, &tv);
                count = 0;
                rc = 1;
                while (rc && count < 1000) {
                    rc = read(fd, console_main, (bufsize - 1));
                    console_main[(bufsize - 1)] = '\0';
                    count++;
                }
                close(fd);
            }
            kill(pid, 9);
            wait(&status);
        }

        unlink(console_file);
    }

    ret = EUCA_ERROR;
    if ((console_output = EUCA_ZALLOC(((64 * 1024) + 4096), sizeof(char))) != NULL) {
        if (console_append) {
            strncat(console_output, console_append, 4096);
        }

        if (console_main) {
            strncat(console_output, console_main, 1024 * 64);
        }

        *consoleOutput = base64_enc((unsigned char *)console_output, strlen(console_output));
        ret = EUCA_OK;
    }

    EUCA_FREE(console_append);
    EUCA_FREE(console_main);
    EUCA_FREE(console_output);
    return (ret);
}
