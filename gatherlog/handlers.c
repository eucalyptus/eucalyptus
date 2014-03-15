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
//! @file gatherlog/handlers.c
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
#include <linux/limits.h>

#include <gl-client-marshal.h>
#include <handlers.h>
#include <euca_auth.h>

#include <eucalyptus.h>

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
//! Handles the get logs service request
//!
//! @param[in]  service service name identifier
//! @param[out] outCCLog output CC logs
//! @param[out] outNCLog output NC logs
//! @param[out] outHTTPDLog output HTTPD logs
//! @param[out] outAxis2Log output AXIS2 logs
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_MEMORY_ERROR: if we fail to allocate memory
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre All parameters must be non-NULLs.
//!
//! @post On success, the outCCLog, outNCLog, outHTTPDLog and outAxis2Log parameters are
//!       set appropriately.
//!
/* int doGetLogs(char *service, char **outCCLog, char **outNCLog, char **outHTTPDLog, char **outAxis2Log)
{
    int fd = -1;
    int rc = 0;
    int bufsize = 0;
    int pid = 0;
    int filedes[2] = { 0 };
    int status = 0;
    char *buf = NULL;
    char *tmp = NULL;
    char *clog = NULL;
    char *hlog = NULL;
    char *alog = NULL;
    char *nlog = NULL;
    char *home = NULL;
    char file[EUCA_MAX_PATH] = { 0 };
    axutil_env_t *env = NULL;
    axis2_char_t *client_home = NULL;
    axis2_stub_t *stub = NULL;

    if (!service || !outCCLog || !outNCLog || !outHTTPDLog || !outAxis2Log) {
        printf("Invalid params: service=%s, outCCLog=%p, outNCLog=%p outHTTPDLog=%p, outAxis2Log=%p!\n", service, outCCLog, outNCLog, outHTTPDLog, outAxis2Log);
        return (EUCA_INVALID_ERROR);
    }

    *outCCLog = *outNCLog = *outHTTPDLog = *outAxis2Log = NULL;

    bufsize = 1000 * 1024;
    if ((buf = EUCA_ALLOC(bufsize, sizeof(char))) == NULL) {
        printf("Out of memory!\n");
        return (EUCA_MEMORY_ERROR);
    }

    if (!strcmp(service, "self")) {
        home = NULL;
        if ((tmp = getenv("EUCALYPTUS")) != NULL)
            home = strdup(tmp);

        if (!home)
            home = strdup("");

        if (!home) {
            printf("Out of memory!\n");
            EUCA_FREE(buf);
            return (EUCA_MEMORY_ERROR);
        }

        snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR "/cc.log", home);
        if ((fd = open(file, O_RDONLY)) >= 0) {
            bzero(buf, bufsize);
            lseek(fd, -1 * bufsize, SEEK_END);
            if ((rc = read(fd, buf, bufsize)) > 0) {
                *outCCLog = base64_enc((unsigned char *)buf, strlen(buf));
            }
            close(fd);
        }

        snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR "/nc.log", home);
        if ((fd = open(file, O_RDONLY)) >= 0) {
            bzero(buf, bufsize);
            lseek(fd, -1 * bufsize, SEEK_END);
            if ((rc = read(fd, buf, bufsize)) > 0) {
                *outNCLog = base64_enc((unsigned char *)buf, strlen(buf));
            }
            close(fd);
        }

        snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR "/httpd-nc_error_log", home);
        if ((fd = open(file, O_RDONLY)) < 0) {
            snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR "/httpd-cc_error_log", home);
            fd = open(file, O_RDONLY);
        }

        if (fd >= 0) {
            bzero(buf, bufsize);
            if ((rc = read(fd, buf, bufsize)) > 0) {
                *outHTTPDLog = base64_enc((unsigned char *)buf, strlen(buf));
            }
            close(fd);
        }

        snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR "/axis2c.log", home);
        if ((fd = open(file, O_RDONLY)) >= 0) {
            bzero(buf, bufsize);
            if ((rc = read(fd, buf, bufsize)) > 0) {
                *outAxis2Log = base64_enc((unsigned char *)buf, strlen(buf));
            }
            close(fd);
        }

        EUCA_FREE(home);
    } else {
        if (pipe(filedes) == 0) {
            pid = fork();
            if (pid == 0) {
                close(filedes[0]);

                env = axutil_env_create_all(NULL, 0);
                if ((client_home = AXIS2_GETENV("AXIS2C_HOME")) == NULL) {
                    exit(1);
                } else {
                    stub = axis2_stub_create_EucalyptusGL(env, client_home, service);
                    clog = nlog = hlog = alog = NULL;
                    if ((rc = gl_getLogs("self", &clog, &nlog, &hlog, &alog, env, stub)) == EUCA_OK) {
                        bzero(buf, bufsize);
                        if (clog)
                            snprintf(buf, bufsize, "%s", clog);
                        rc = write(filedes[1], buf, bufsize);

                        bzero(buf, bufsize);
                        if (nlog)
                            snprintf(buf, bufsize, "%s", nlog);
                        rc = write(filedes[1], buf, bufsize);

                        bzero(buf, bufsize);
                        if (hlog)
                            snprintf(buf, bufsize, "%s", hlog);
                        rc = write(filedes[1], buf, bufsize);

                        bzero(buf, bufsize);
                        if (alog)
                            snprintf(buf, bufsize, "%s", alog);
                        rc = write(filedes[1], buf, bufsize);
                    }
                }
                close(filedes[1]);
                exit(0);
            } else {
                close(filedes[1]);

                bzero(buf, bufsize);
                rc = read(filedes[0], buf, bufsize - 1);
                if (rc && buf[0] != '\0') {
                    *outCCLog = strdup(buf);
                }

                bzero(buf, bufsize);
                rc = read(filedes[0], buf, bufsize - 1);
                if (rc && buf[0] != '\0') {
                    *outNCLog = strdup(buf);
                }

                bzero(buf, bufsize);
                rc = read(filedes[0], buf, bufsize - 1);
                if (rc && buf[0] != '\0') {
                    *outHTTPDLog = strdup(buf);
                }

                bzero(buf, bufsize);
                rc = read(filedes[0], buf, bufsize - 1);
                if (rc && buf[0] != '\0') {
                    *outAxis2Log = strdup(buf);
                }
                close(filedes[0]);
                wait(&status);
            }
        }
    }

    EUCA_FREE(buf);
    return (EUCA_OK);
} */

//!
//! Handles the get keys service request.
//!
//! @param[in]  service service name identifier
//! @param[out] outCCCert output CC Certificate
//! @param[out] outNCCert output NC certificate
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_MEMORY_ERROR: if we fail to allocate memory
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre All parameters must be non-NULLs.
//!
//! @post On success, the outCCCert, outNCCert parameters are set appropriately.
//!
int doGetKeys(char *service, char **outCCCert, char **outNCCert)
{
    int fd = -1;
    int rc = 0;
    int bufsize = 0;
    int pid = 0;
    int filedes[2] = { 0 };
    int status = 0;
    char *tmp = NULL;
    char *buf = NULL;
    char *home = NULL;
    char *ccert = NULL;
    char *ncert = NULL;
    char file[EUCA_MAX_PATH] = { 0 };
    axutil_env_t *env = NULL;
    axis2_char_t *client_home = NULL;
    axis2_stub_t *stub = NULL;

    if (!service || !outCCCert || !outNCCert) {
        printf("ERROR: Invalid params: service=%s, outCCCert=%p, outNCCert=%p\n", service, outCCCert, outNCCert);
        return (EUCA_INVALID_ERROR);
    }

    *outCCCert = *outNCCert = NULL;

    bufsize = 1000 * 1024;
    if ((buf = EUCA_ALLOC(bufsize, sizeof(char))) == NULL) {
        printf("ERROR: Out of memory!\n");
        return (EUCA_MEMORY_ERROR);
    }

    if (!strcmp(service, "self")) {
        home = NULL;
        if ((tmp = getenv("EUCALYPTUS")) != NULL)
            home = strdup(tmp);

        if (!home)
            home = strdup("");

        if (!home) {
            printf("ERROR: Out of memory!\n");
            EUCA_FREE(buf);
            return (EUCA_MEMORY_ERROR);
        }

        snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_KEYS_DIR "/cluster-cert.pem", home);
        if ((fd = open(file, O_RDONLY)) >= 0) {
            bzero(buf, bufsize);
            lseek(fd, -1 * bufsize, SEEK_END);
            if ((rc = read(fd, buf, bufsize)) > 0) {
                buf[(bufsize - 1)] = '\0';
                *outCCCert = base64_enc(((unsigned char *)buf), strlen(buf));
            }
            close(fd);
        }

        snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_KEYS_DIR "/node-cert.pem", home);
        if ((fd = open(file, O_RDONLY)) >= 0) {
            bzero(buf, bufsize);
            lseek(fd, -1 * bufsize, SEEK_END);
            // make sure that buf is NULL terminated
            if ((rc = read(fd, buf, bufsize - 1)) > 0) {
                buf[(bufsize - 1)] = '\0';
                *outNCCert = base64_enc(((unsigned char *)buf), strlen(buf));
            }
            close(fd);
        }

        EUCA_FREE(home);
    } else {
        if (pipe(filedes) == 0) {
            pid = fork();
            if (pid == 0) {
                close(filedes[0]);

                env = axutil_env_create_all(NULL, 0);
                if ((client_home = AXIS2_GETENV("AXIS2C_HOME")) == NULL) {
                    printf("ERROR: cannot retrieve AXIS2_HOME environment variable.\n");
                    exit(1);
                } else {
                    if ((stub = axis2_stub_create_EucalyptusGL(env, client_home, service)) == NULL) {
                        printf("ERROR: cannot retrieve AXIS2 stub.\n");
                        exit(1);
                    }

                    ccert = ncert = NULL;
                    if ((rc = gl_getKeys("self", &ccert, &ncert, env, stub)) == EUCA_OK) {
                        bzero(buf, bufsize);
                        if (ccert)
                            snprintf(buf, bufsize, "%s", ccert);
                        rc = write(filedes[1], buf, bufsize);

                        bzero(buf, bufsize);
                        if (ncert)
                            snprintf(buf, bufsize, "%s", ncert);
                        rc = write(filedes[1], buf, bufsize);
                    }
                }
                close(filedes[1]);
                exit(0);
            } else {
                close(filedes[1]);

                if ((rc = read(filedes[0], buf, (bufsize - 1))) > 0) {
                    buf[(bufsize - 1)] = '\0';
                    *outCCCert = strdup(buf);
                }

                if ((rc = read(filedes[0], buf, bufsize - 1)) > 0) {
                    buf[(bufsize - 1)] = '\0';
                    *outNCCert = strdup(buf);
                }

                close(filedes[0]);
                wait(&status);
            }
        }
    }

    EUCA_FREE(buf);
    return (EUCA_OK);
}
