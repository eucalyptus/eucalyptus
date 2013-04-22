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
//! @file node/client-marshal-adb.c
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

#include <eucalyptus.h>

#include <neethi_policy.h>
#include <neethi_util.h>

#include <axis2_stub_EucalyptusSC.h>

#include <misc.h>
#include <euca_string.h>

#include "sc-client-marshal.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define NULL_ERROR_MSG               "operation on %s could not be invoked (check SC host, port, and credentials)\n", pStub->node_name

#define CORRELATION_ID               NULL   //!< Default Corelation ID value

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
 |                             EXPORTED PROTOTYPES                            |
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
//! Creates and initialize an NC stub entry
//!
//! @param[in] endpoint_uri the endpoint URI string
//! @param[in] logfile the log file name string
//! @param[in] homedir the home directory path string
//!
//! @return a pointer to the newly created NC stub structure
//!
scStub *scStubCreate(char *endpoint_uri, char *logfile, char *homedir)
{
    char *uri = NULL;
    char *p = NULL;
    char *node_name = NULL;
    scStub *pStub = NULL;
    axutil_env_t *env = NULL;
    axis2_char_t *client_home;
    axis2_stub_t *stub;

    if (logfile) {
        env = axutil_env_create_all(logfile, AXIS2_LOG_LEVEL_TRACE);
    } else {
        env = axutil_env_create_all(NULL, 0);
    }

    if (homedir) {
        client_home = (axis2_char_t *) homedir;
    } else {
        client_home = AXIS2_GETENV("AXIS2C_HOME");
    }

    if (client_home == NULL) {
        LOGERROR("cannot get AXIS2C_HOME");
        return NULL;
    }

    if (endpoint_uri == NULL) {
        LOGERROR("empty endpoint_url");
        return NULL;
    }

    uri = endpoint_uri;

    // extract node name from the endpoint
    p = strstr(uri, "://");     // find "http[s]://..."
    if (p == NULL) {
        LOGERROR("received invalid URI %s\n", uri);
        return NULL;
    }

    node_name = strdup(p + 3);  // copy without the protocol prefix
    if (node_name == NULL) {
        LOGERROR("is out of memory\n");
        return NULL;
    }

    if ((p = strchr(node_name, ':')) != NULL)
        *p = '\0';              // cut off the port

    if ((p = strchr(node_name, '/')) != NULL)
        *p = '\0';              // if there is no port

    //! @todo what if endpoint_uri, home, or env are NULL?
    stub = axis2_stub_create_EucalyptusSC(env, client_home, (axis2_char_t *) uri);

    if (stub) {
        if ((pStub = EUCA_ZALLOC(1, sizeof(scStub))) != NULL) {
            pStub->env = env;
            pStub->client_home = strdup((char *)client_home);
            pStub->endpoint_uri = (axis2_char_t *) strdup(endpoint_uri);
            pStub->node_name = (axis2_char_t *) strdup(node_name);
            pStub->stub = stub;
            if (pStub->client_home == NULL || pStub->endpoint_uri == NULL || pStub->node_name == NULL) {
                LOGWARN("out of memory (%s:%s:%d client_home=%s endpoint_uri=%s node_name=%s)", __FILE__, __FUNCTION__, __LINE__,
                        pStub->client_home, pStub->endpoint_uri, pStub->node_name);
            }
        } else {
            LOGWARN("out of memory for 'st' (%s:%s:%d)\n", __FILE__, __FUNCTION__, __LINE__);
        }
    } else {
        LOGERROR("failed to create a stub for EucalyptusNC service (stub=%p env=%p client_home=%s)\n", stub, env, client_home);
    }

    EUCA_FREE(node_name);
    return (pStub);
}

//!
//! destroy an NC stub structure
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//!
//! @return Always returns EUCA_OK
//!
int scStubDestroy(scStub * pStub)
{
    EUCA_FREE(pStub->client_home);
    EUCA_FREE(pStub->endpoint_uri);
    EUCA_FREE(pStub->node_name);
    EUCA_FREE(pStub);
    return (EUCA_OK);
}

//!
//! Marshals the client Export volume token request.
//!
//! @param[in] pStub a pointer to the storage controller (SC) stub structure
//! @param[in] correlationId a pointer to the correlationId string to use, may be NULL
//! @param[in] userId a pointer to the userId string to use, may be NULL
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] token the token to be Exportd by the SC
//! @param[in] ip the NC's ip to be used for token resolution
//! @param[in] iqn the NC's iqn to be used for token resolution
//! @param[in] connection_info a pointer to a pointer to hold the resulting volume connection structure on return
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int scExportVolumeStub(scStub * pStub, char *correlationId, char *userId, char *volumeId, char *token, char *ip, char *iqn, char **connection_string)
{
    int status = 0;
    axutil_env_t *env = NULL;
    axis2_stub_t *stub = NULL;
    adb_ExportVolume_t *input = NULL;
    adb_ExportVolumeResponse_t *output = NULL;
    adb_ExportVolumeType_t *request = NULL;
    adb_ExportVolumeResponseType_t *response = NULL;

    if(!connection_string) {
    	return -1;
    }

    env = pStub->env;
    stub = pStub->stub;
    input = adb_ExportVolume_create(env);
    request = adb_ExportVolumeType_create(env);

    // set op-specific input fields
    adb_ExportVolumeType_set_token(request, env, token);
    adb_ExportVolumeType_set_volumeId(request, env, volumeId);
    adb_ExportVolumeType_set_ip(request, env, ip);
    adb_ExportVolumeType_set_iqn(request, env, iqn);

    //Add the request structure to the message type
    adb_ExportVolume_set_ExportVolume(input, env, request);

    // do it
    if ((output = axis2_stub_op_EucalyptusSC_ExportVolume(stub, env, input)) == NULL) {
        LOGERROR(NULL_ERROR_MSG);
        status = -1;
    } else {
        response = adb_ExportVolumeResponse_get_ExportVolumeResponse(output, env);
        if(response == NULL) {
            LOGERROR("[%s] returned an error\n", volumeId);
            *connection_string = NULL;
            status = 1;
        } else {
        	//Set return values
        	char *returned_vol_id = adb_ExportVolumeResponseType_get_volumeId(response,env);
        	//Ensure that the returned token is for the requested vol.
        	if(strcmp(returned_vol_id, volumeId) == 0) {
        		*connection_string = adb_ExportVolumeResponseType_get_connectionString(response,env);
        	}
        }
    }

    return (status);
}

//!
//! Marshals the client unexport volume token request.
//!
//! @param[in] pStub a pointer to the storage controller (SC) stub structure
//! @param[in] pMeta a pointer to the NC metadata for getting node info (ip etc)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] token the token to be resolved by the SC
//! @param[in] ip the NC's ip to be used for token resolution
//! @param[in] iqn the NC's iqn to be used for token resolution
//! @param[in] result pointer to int to hold return result of operation
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int scUnexportVolumeStub(scStub * pStub, char *correlationId, char *userId, char *volumeId, char *token, char *ip, char *iqn)
{
	int status = 0;
	axutil_env_t *env = NULL;
	axis2_stub_t *stub = NULL;
	adb_UnexportVolume_t *input = NULL;
	adb_UnexportVolumeResponse_t *output = NULL;
	adb_UnexportVolumeType_t *request = NULL;
	adb_UnexportVolumeResponseType_t *response = NULL;

	env = pStub->env;
	stub = pStub->stub;
	input = adb_UnexportVolume_create(env);
	request = adb_UnexportVolumeType_create(env);

	// set op-specific input fields
	adb_UnexportVolumeType_set_token(request, env, token);
	adb_UnexportVolumeType_set_volumeId(request, env, volumeId);
	adb_UnexportVolumeType_set_ip(request, env, ip);
	adb_UnexportVolumeType_set_iqn(request, env, iqn);

	//Add the request structure to the message type
	adb_UnexportVolume_set_UnexportVolume(input, env, request);

	// do it
	if ((output = axis2_stub_op_EucalyptusSC_UnexportVolume(stub, env, input)) == NULL) {
		LOGERROR(NULL_ERROR_MSG);
		status = -1;
	} else {
		response = adb_UnexportVolumeResponse_get_UnexportVolumeResponse(output, env);
		if(response == NULL) {
			LOGERROR("[%s] returned an error\n", volumeId);
			status = 1;
		}
	}
	return (status);
}

