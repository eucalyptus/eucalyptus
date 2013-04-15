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
//! @file gatherlog/gl-client-marshal-adb.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <assert.h>
#include <dirent.h>
#include <sys/errno.h>
#include <limits.h>
#include <locale.h>
#include <pthread.h>
#include <stdarg.h>
#include <string.h>
#include <unistd.h>
#include <wchar.h>
#include <sys/stat.h>
#include <ctype.h>

#include <gl-client-marshal.h>
#include <eucalyptus.h>
#include <euca_auth.h>

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
//! Client side of the get log service request
//!
//! @param[in]  service service name identifier
//! @param[out] outCClog output CC logs
//! @param[out] outNClog output NC logs
//! @param[out] outHlog output HTTPD logs
//! @param[out] outAlog output AXIS2 logs
//! @param[in]  env pointer to AXIS2 environment
//! @param[in]  stub pointer to AXIS2 stub
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int gl_getLogs(char *service, char **outCClog, char **outNClog, char **outHlog, char **outAlog, axutil_env_t * env, axis2_stub_t * stub)
{
    adb_GetLogsResponse_t *out = NULL;
    adb_getLogsResponseType_t *response = NULL;
    adb_GetLogs_t *in = NULL;
    adb_getLogsType_t *request = NULL;

    request = adb_getLogsType_create(env);
    adb_getLogsType_set_userId(request, env, "eucalyptus");
    adb_getLogsType_set_correlationId(request, env, "12345678");
    adb_getLogsType_set_serviceTag(request, env, service);

    in = adb_GetLogs_create(env);
    adb_GetLogs_set_GetLogs(in, env, request);

    if ((out = axis2_stub_op_EucalyptusGL_GetLogs(stub, env, in)) == NULL) {
        printf("ERROR: operation call failed\n");
        return (EUCA_ERROR);
    }

    response = adb_GetLogsResponse_get_GetLogsResponse(out, env);
    *outCClog = adb_getLogsResponseType_get_CCLog(response, env);
    *outNClog = adb_getLogsResponseType_get_NCLog(response, env);
    *outHlog = adb_getLogsResponseType_get_httpdLog(response, env);
    *outAlog = adb_getLogsResponseType_get_axis2Log(response, env);
    return (EUCA_OK);
}

 //!
 //! Client side of the get keys service request.
 //!
 //! @param[in]  service service name identifier
 //! @param[out] outCCCert output CC Certificate
 //! @param[out] outNCCert output NC Certificate
 //! @param[in]  env pointer to the AXIS2 environment
 //! @param[in]  stub pointer to the AXIS2 stub
 //!
 //! @return EUCA_OK on success or EUCA_ERROR on failure
 //!
int gl_getKeys(char *service, char **outCCCert, char **outNCCert, axutil_env_t * env, axis2_stub_t * stub)
{
    adb_GetKeysResponse_t *out = NULL;
    adb_getKeysResponseType_t *response = NULL;
    adb_GetKeys_t *in = NULL;
    adb_getKeysType_t *request = NULL;

    request = adb_getKeysType_create(env);
    adb_getKeysType_set_userId(request, env, "eucalyptus");
    adb_getKeysType_set_correlationId(request, env, "12345678");
    adb_getKeysType_set_serviceTag(request, env, service);

    in = adb_GetKeys_create(env);
    adb_GetKeys_set_GetKeys(in, env, request);

    if ((out = axis2_stub_op_EucalyptusGL_GetKeys(stub, env, in)) == NULL) {
        printf("ERROR: operation call failed\n");
        return (EUCA_ERROR);
    }

    response = adb_GetKeysResponse_get_GetKeysResponse(out, env);
    *outCCCert = adb_getKeysResponseType_get_CCcert(response, env);
    *outNCCert = adb_getKeysResponseType_get_NCcert(response, env);
    return (EUCA_OK);
}
