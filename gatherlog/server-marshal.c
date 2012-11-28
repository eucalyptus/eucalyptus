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
//! @file gatherlog/server-marshal.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <server-marshal.h>
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
 |                             EXPORTED VARIABLES                             |
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

adb_GetLogsResponse_t *GetLogsMarshal(adb_GetLogs_t * getLogs, const axutil_env_t * env);
adb_GetKeysResponse_t *GetKeysMarshal(adb_GetKeys_t * getKeys, const axutil_env_t * env);

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
//! Server side of the get logs service request
//!
//! @param[in] getLogs pointer to the request structure
//! @param[in] env pointer to the AXIS2 environment
//!
//! @return A pointer to the response structure
//!
adb_GetLogsResponse_t *GetLogsMarshal(adb_GetLogs_t * getLogs, const axutil_env_t * env)
{
    int rc = 0;
    axis2_bool_t status = AXIS2_TRUE;
    char *userId = NULL;
    char *correlationId = NULL;
    char *service = NULL;
    char statusMessage[256] = { 0 };
    char *outCCLog = NULL;
    char *outNCLog = NULL;
    char *outHTTPDLog = NULL;
    char *outAxis2Log = NULL;
    adb_GetLogsResponse_t *ret = NULL;
    adb_getLogsResponseType_t *response = NULL;
    adb_getLogsType_t *request = NULL;

    request = adb_GetLogs_get_GetLogs(getLogs, env);
    userId = adb_getLogsType_get_userId(request, env);
    correlationId = adb_getLogsType_get_correlationId(request, env);
    service = adb_getLogsType_get_serviceTag(request, env);
    response = adb_getLogsResponseType_create(env);

    if ((rc = doGetLogs(service, &outCCLog, &outNCLog, &outHTTPDLog, &outAxis2Log)) != EUCA_OK) {
        status = AXIS2_FALSE;
        snprintf(statusMessage, 255, "ERROR");
    } else {
        if (outCCLog) {
            adb_getLogsResponseType_set_CCLog(response, env, outCCLog);
            EUCA_FREE(outCCLog);
        }

        if (outNCLog) {
            adb_getLogsResponseType_set_NCLog(response, env, outNCLog);
            EUCA_FREE(outNCLog);
        }

        if (outHTTPDLog) {
            adb_getLogsResponseType_set_httpdLog(response, env, outHTTPDLog);
            EUCA_FREE(outHTTPDLog);
        }

        if (outAxis2Log) {
            adb_getLogsResponseType_set_axis2Log(response, env, outAxis2Log);
            EUCA_FREE(outAxis2Log);
        }
    }

    adb_getLogsResponseType_set_serviceTag(response, env, service);
    adb_getLogsResponseType_set_userId(response, env, userId);
    adb_getLogsResponseType_set_correlationId(response, env, correlationId);
    adb_getLogsResponseType_set_return(response, env, status);
    if (status == AXIS2_FALSE) {
        adb_getLogsResponseType_set_statusMessage(response, env, statusMessage);
    }

    ret = adb_GetLogsResponse_create(env);
    adb_GetLogsResponse_set_GetLogsResponse(ret, env, response);
    return (ret);
}

//!
//! Server side of the get keys service request
//!
//! @param[in] getKeys pointer to the request structure
//! @param[in] env pointer to the AXIS2 environment
//!
//! @return A pointer to the response structure
//!
adb_GetKeysResponse_t *GetKeysMarshal(adb_GetKeys_t * getKeys, const axutil_env_t * env)
{
    int rc = 0;
    axis2_bool_t status = AXIS2_FALSE;
    char *userId = NULL;
    char *correlationId = NULL;
    char *service = NULL;
    char statusMessage[256] = { 0 };
    char *outCCCert = NULL;
    char *outNCCert = NULL;
    adb_GetKeysResponse_t *ret = NULL;
    adb_getKeysResponseType_t *response = NULL;
    adb_getKeysType_t *request = NULL;

    request = adb_GetKeys_get_GetKeys(getKeys, env);
    userId = adb_getKeysType_get_userId(request, env);
    correlationId = adb_getKeysType_get_correlationId(request, env);
    service = adb_getKeysType_get_serviceTag(request, env);
    response = adb_getKeysResponseType_create(env);

    status = AXIS2_TRUE;
    if ((rc = doGetKeys(service, &outCCCert, &outNCCert)) != EUCA_OK) {
        status = AXIS2_FALSE;
        snprintf(statusMessage, 255, "ERROR");
    } else {
        if (outCCCert) {
            adb_getKeysResponseType_set_CCcert(response, env, outCCCert);
            EUCA_FREE(outCCCert);
        }

        if (outNCCert) {
            adb_getKeysResponseType_set_NCcert(response, env, outNCCert);
            EUCA_FREE(outNCCert);
        }
    }

    adb_getKeysResponseType_set_userId(response, env, userId);
    adb_getKeysResponseType_set_correlationId(response, env, correlationId);
    adb_getKeysResponseType_set_return(response, env, status);
    adb_getKeysResponseType_set_serviceTag(response, env, service);

    if (status == AXIS2_FALSE) {
        adb_getKeysResponseType_set_statusMessage(response, env, statusMessage);
    }

    ret = adb_GetKeysResponse_create(env);
    adb_GetKeysResponse_set_GetKeysResponse(ret, env, response);
    return (ret);
}
