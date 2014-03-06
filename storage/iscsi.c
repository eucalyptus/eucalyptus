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
//! @file storage/iscsi.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>                    /* close */
#include <assert.h>
#include <string.h>
#include <strings.h>
#include <fcntl.h>                     /* open */
#include <signal.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <eucalyptus.h>
#include <config.h>
#include <misc.h>
#include <ipc.h>
#include <euca_string.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MAX_OUTPUT                               4096

#define CONNECT_TIMEOUT                           600
#define DISCONNECT_TIMEOUT                        600
#define GET_TIMEOUT                                60

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

static char home[EUCA_MAX_PATH] = "";
static char connect_storage_cmd_path[EUCA_MAX_PATH] = "";
static char disconnect_storage_cmd_path[EUCA_MAX_PATH] = "";
static char get_storage_cmd_path[EUCA_MAX_PATH] = "";

static sem *iscsi_sem = NULL;          //!< for serializing attach and detach invocations

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
//! @param[in] euca_home
//!
void init_iscsi(const char *euca_home)
{
    const char *tmp = NULL;
    if (euca_home) {
        tmp = euca_home;
    } else {
        if ((tmp = getenv(EUCALYPTUS_ENV_VAR_NAME)) == NULL) {
            tmp = "/opt/eucalyptus";
        }
    }

    euca_strncpy(home, tmp, sizeof(home));
    snprintf(connect_storage_cmd_path, EUCA_MAX_PATH, EUCALYPTUS_CONNECT_ISCSI, home, home);
    snprintf(disconnect_storage_cmd_path, EUCA_MAX_PATH, EUCALYPTUS_DISCONNECT_ISCSI, home, home);
    snprintf(get_storage_cmd_path, EUCA_MAX_PATH, EUCALYPTUS_GET_ISCSI, home, home);
    iscsi_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
}

//!
//!
//!
//! @param[in] dev_string
//!
//! @return a pointer
//!
char *connect_iscsi_target(const char *dev_string)
{
    int ret = 0;
    char command[EUCA_MAX_PATH] = "";
    char stdout_str[MAX_OUTPUT] = "";
    char stderr_str[MAX_OUTPUT] = "";

    assert(strlen(home));

    snprintf(command, EUCA_MAX_PATH, "%s %s,%s", connect_storage_cmd_path, home, dev_string);
    LOGTRACE("invoking `%s`\n", command);

    sem_p(iscsi_sem);
    ret = timeshell(command, stdout_str, stderr_str, MAX_OUTPUT, CONNECT_TIMEOUT);
    sem_v(iscsi_sem);
    LOGDEBUG("connect script returned: %d, stdout: '%s', stderr: '%s'\n", ret, stdout_str, stderr_str);

    if (ret == 0)
        return (strdup(stdout_str));
    return (NULL);
}

//!
//! Disconnects the iscsi device by closing the session and deleting the lun locally.
//!
//! @param[in] dev_string
//! @param[in] do_rescan
//!
//! @return -1 for any failures, 0 if a timeout occured or a positive value is success.
//!
int disconnect_iscsi_target(const char *dev_string, int do_rescan)
{
    int ret = 0;
    char command[EUCA_MAX_PATH] = "";
    char stdout_str[MAX_OUTPUT] = "";
    char stderr_str[MAX_OUTPUT] = "";

    assert(strlen(home));

    if (do_rescan) {
        snprintf(command, EUCA_MAX_PATH, "%s %s,%s", disconnect_storage_cmd_path, home, dev_string);
    } else {
        snprintf(command, EUCA_MAX_PATH, "%s %s,%s %s", disconnect_storage_cmd_path, home, dev_string, "norescan");
    }

    LOGTRACE("invoking `%s`\n", command);

    sem_p(iscsi_sem);
    ret = timeshell(command, stdout_str, stderr_str, MAX_OUTPUT, DISCONNECT_TIMEOUT);
    sem_v(iscsi_sem);
    LOGDEBUG("disconnect script returned: %d, stdout: '%s', stderr: '%s'\n", ret, stdout_str, stderr_str);

    return (ret);
}

//!
//!
//!
//! @param[in] dev_string
//!
//! @return a pointer
//!
char *get_iscsi_target(const char *dev_string)
{
    int ret = 0;
    char command[EUCA_MAX_PATH] = "";
    char stdout_str[MAX_OUTPUT] = "";
    char stderr_str[MAX_OUTPUT] = "";

    assert(strlen(home));

    snprintf(command, EUCA_MAX_PATH, "%s %s,%s", get_storage_cmd_path, home, dev_string);
    LOGDEBUG("invoking `%s`\n", command);

    sem_p(iscsi_sem);
    ret = timeshell(command, stdout_str, stderr_str, MAX_OUTPUT, GET_TIMEOUT);
    sem_v(iscsi_sem);
    LOGDEBUG("get storage script returned: %d, stdout: '%s', stderr: '%s'\n", ret, stdout_str, stderr_str);

    if (ret == 0)
        return (strdup(stdout_str));
    return (NULL);
}

//!
//!
//!
//! @param[in] dev_string
//!
//! @return EUCA_OK if the device is an ISCSI or EUCA_ERROR if not
//!
int check_iscsi(const char *dev_string)
{
    if (strchr(dev_string, ',') == NULL)
        return (EUCA_OK);
    return (EUCA_ERROR);
}
