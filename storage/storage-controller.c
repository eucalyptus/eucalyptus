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
//! @file storage/storage-controller.c
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
#include <sys/types.h>                 /* fork */
#include <sys/wait.h>                  /* waitpid */
#include <fcntl.h>
#include <assert.h>
#include <sys/stat.h>

#include <eucalyptus.h>
#include <ipc.h>
#include <misc.h>
#include <euca_auth.h>
#include <euca_axis.h>

#include <config.h>
#include <fault.h>
#include <log.h>
#include <euca_string.h>

#include "storage-controller.h"
#include "sc-client-marshal.h"

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
//! Make a call to the SC as specified with a string and a timeout.
//!
//! This implementation is heavily borrowed from the CC's ncClientCall.
//! Uses fork/exec for client to preserve memory since Axis's destroy/free have issues
//!
//! @param[in] correlationId a pointer to the correlationId string to use for the call to the SC
//! @param[in] userId a pointer to the userId string to use for the call to the SC
//! @param[in] use_ws_sec an integer/boolean to indicate if WS-Sec should be used
//! @param[in] ws_sec_policy_file_path a pointer to the string giving the SC client policy file
//! @param[in] timeout the timeout for the call in seconds
//! @param[in] scURL the URL to send the request to
//! @param[in] scOp the operation to perform (i.e. "ExportVolume", "UnexportVolume",...)
//! @param[in] ...
//!
//! @return
//!
//! @pre
//!
//! @note
//!
int scClientCall(char *correlationId, char *userId, int use_ws_sec, char *ws_sec_policy_file_path, int timeout, char *scURL, char *scOp, ...)
{
    int pid = 0;
    int rc = 0;
    int ret = 0;
    int status = 0;
    int opFail = 0;
    int len = 0;
    int rbytes = 0;
    int i = 0;
    int filedes[2] = { 0 };
    char *localCorrelationId = NULL;
    char *localUserId = NULL;
    scStub *scs = NULL;
    va_list al = { {0} };

    LOGTRACE("invoked: scOps=%s scURL=%s timeout=%d\n", scOp, scURL, timeout);  // these are common

    if (timeout <= 0)
        timeout = DEFAULT_SC_REQUEST_TIMEOUT;

    if ((rc = pipe(filedes)) != 0) {
        LOGERROR("cannot create pipe scOps=%s\n", scOp);
        return (1);
    }

    va_start(al, scOp);

    //Fork and exec the client stub...do this for mem protection
    pid = fork();
    if (!pid) {
        //I'm the child, do the call.
        if (correlationId) {
            localCorrelationId = strdup(correlationId);
        } else {
            localCorrelationId = strdup("unset");
        }
        if (userId) {
            localUserId = strdup(userId);
        } else {
            localUserId = strdup("eucalyptus");
        }

        close(filedes[0]);
        scs = scStubCreate(scURL, NULL, NULL);
        if (use_ws_sec) {
            LOGTRACE("Configuring and Initializing WS-SEC for SC Client\n");
            rc = InitWSSEC(scs->env, scs->stub, ws_sec_policy_file_path);
            if (rc) {
                LOGERROR("Error initializing WSSEC state for SC Client\n");
            }
        }

        LOGTRACE("\tscOps=%s ppid=%d client calling '%s'\n", scOp, getppid(), scOp);
        if (!strcmp(scOp, "ExportVolume")) {
            // args: char *volumeId
            // args: char *token
            // args: char *ip
            // args: char *iqn
            // args: char **connectionInfo (for return)
            char *volumeId = va_arg(al, char *);
            char *token = va_arg(al, char *);
            char *ip = va_arg(al, char *);
            char *iqn = va_arg(al, char *);
            char **connectInfo = va_arg(al, char **);

            LOGTRACE("Calling Export Volume Stub\n");
            rc = scExportVolumeStub(scs, localCorrelationId, localUserId, volumeId, token, ip, iqn, connectInfo);
            if (connectInfo) {
                if (!rc && *connectInfo) {
                    len = strlen(*connectInfo) + 1;
                    LOGTRACE("SC Client child received output %d in length: %s\n", len, *connectInfo);
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = write(filedes[1], *connectInfo, sizeof(char) * len);
                    rc = 0;
                } else {
                    len = 0;
                    LOGTRACE("SC Client child received output %d in length\n", len);
                    rc = write(filedes[1], &len, sizeof(int));
                    rc = 1;
                }
            }
        } else if (!strcmp(scOp, "UnexportVolume")) {
            // args: char *volumeId
            // args: char *token
            // args: char *ip
            // args: char *iqn
            char *volumeId = va_arg(al, char *);
            char *token = va_arg(al, char *);
            char *ip = va_arg(al, char *);
            char *iqn = va_arg(al, char *);

            LOGTRACE("Calling Unexport Volume Stub\n");
            rc = scUnexportVolumeStub(scs, localCorrelationId, localUserId, volumeId, token, ip, iqn);
        } else {
            LOGWARN("\tscOps=%s operation '%s' not found\n", scOp, scOp);
            rc = 1;
        }

        LOGTRACE("\tscOps=%s ppid=%d done calling '%s' with exit code '%d'\n", scOp, getppid(), scOp, rc);
        if (rc) {
            ret = 1;
        } else {
            ret = 0;
        }
        close(filedes[1]);
        exit(ret);
    } else {
        // parent returns for each client call
        close(filedes[1]);

        if (!strcmp(scOp, "ExportVolume")) {
            // args: char *volumeId
            // args: char *token
            // args: char *ip
            // args: char *iqn
            // args: char **connectionInfo (for return)
            char *volumeId = va_arg(al, char *);
            char *token = va_arg(al, char *);
            char *ip = va_arg(al, char *);
            char *iqn = va_arg(al, char *);
            char **connectInfo = va_arg(al, char **);

            if (connectInfo) {
                *connectInfo = NULL;
            }

            if (connectInfo) {
                rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
                if (rbytes <= 0) {
                    LOGTRACE("SC Client received output length of %d, not writing data\n", rbytes);
                    killwait(pid);
                    opFail = 1;
                } else {
                    LOGTRACE("SC Client getting %d bytes of output from SC call\n", len);
                    *connectInfo = EUCA_ALLOC(len, sizeof(char));
                    if (!*connectInfo) {
                        LOGFATAL("out of memory! scOps=%s\n", scOp);
                        exit(1);
                    }
                    rbytes = timeread(filedes[0], *connectInfo, len, timeout);
                    LOGTRACE("SC Client got %d bytes in connect info %s\n", rbytes, *connectInfo);
                    if (rbytes <= 0) {
                        killwait(pid);
                        opFail = 1;
                    }
                }
            }
        } else {
            //Nothing to do in default case (success/fail in exit code)
        }

        close(filedes[0]);
        rc = timewait(pid, &status, timeout);
        rc = WEXITSTATUS(status);
    }

    LOGDEBUG("\tdone scOps=%s clientrc=%d opFail=%d\n", scOp, rc, opFail);
    if (rc || opFail) {
        ret = 1;
    } else {
        ret = 0;
    }

    va_end(al);
    return (ret);
}
