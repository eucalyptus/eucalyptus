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
//! @file storage/ebs_utils.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <misc.h>
#include <euca_string.h>
#include <eucalyptus.h>
#include <iscsi.h>
#include <log.h>
#include "storage-controller.h"
#include "ebs_utils.h"

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

#ifdef _UNIT_TEST
const char *euca_this_component_name = "sc";    //!< Eucalyptus Component Name
const char *euca_client_component_name = "nc";  //!< The client component name
#endif /* _UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static sem *vol_sem = NULL;            //!< Semaphore to protect volume operations

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
//! Initialize ebs data structures and semaphores for EBS
//! Should only be called once!
//!
//! @return
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int init_ebs_utils(void)
{
    LOGINFO("Initializing EBS utils\n");
    if (vol_sem == NULL) {
        vol_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
    }
    LOGINFO("Completed EBS util initialization\n");
    return EUCA_OK;
}

//!
//! Gets the local device for the volume (i.e. /dev/sdf)
//!
//! @param[in] connection_string
//!
//! @return
//!
//! @pre
//!
//! @post
//!
//! @note
//!
char *get_volume_local_device(const char *connection_string)
{
    // Invoke the iscsi stuff.
    return get_iscsi_target(connection_string);
}

//!
//! Replaces "sc://" with the actual url of the storage service. Caller must provide the sc-url
//!
//! @param[in] volume_string containing the encoded volume information
//! @param[in] scUrl
//! @param[in] dest_string place to write modified string
//!
//! @return ok|fail
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int replace_sc_url(const char *volume_string, const char *scUrl, char *restrict dest_string)
{
    int prefix_length = strlen(VOLUME_STRING_PREFIX);

    //Check if this has been done already
    char *vol_prefix = EUCA_ZALLOC(prefix_length, sizeof(char));
    if (vol_prefix == NULL) {
        LOGERROR("Could not allocate memory!\n");
        return EUCA_ERROR;
    }

    euca_strncpy(vol_prefix, volume_string, prefix_length + 1);
    if (strcmp(vol_prefix, VOLUME_STRING_PREFIX)) {
        LOGWARN("Cannot insert sc url, already found %s\n", volume_string);
        EUCA_FREE(vol_prefix);
        return EUCA_ERROR;
    }

    const char *data_start = volume_string + (sizeof(char) * prefix_length);    //Go past the sc:// prefix

    //Prepend the SC URL to the remote device string
    if (data_start > 0) {
        LOGDEBUG("Inserting the SC URL to volume string: %s, %s, using token %s \n", volume_string, scUrl, data_start);
        snprintf(dest_string, EUCA_MAX_PATH, "%s/%s", scUrl, data_start);
        LOGDEBUG("Adjusted remote dev string: %s\n", dest_string);
    } else {
        LOGERROR("Error parsing volume string: %s\n", volume_string);
        return EUCA_ERROR;
    }

    return EUCA_OK;
}

//!
//! Parses the volume string and returns a newly allocated ebs_volume_data structure via the parameter.
//! Caller must free the returned structure.
//! Format expected:
//!    sc://volumeId,token.
//!    OR
//!    http://schost:port/services/Storage/volumeId,token
//!
//! @param[in] volume_string containing the encoded volume information
//! @param[in] dest a pointer to a pointer for ebs_volume_data, the referenced pointer will be set to newly allocated struct
//!
//! @return ok|fail
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int deserialize_volume(char *volume_string, ebs_volume_data ** dest)
{
    if (volume_string == NULL) {
        *dest = NULL;
        return EUCA_ERROR;
    }
    //Create working copy
    char working_string[EUCA_MAX_PATH];
    euca_strncpy(working_string, volume_string, EUCA_MAX_PATH);

    ebs_volume_data *vol_data = EUCA_ZALLOC(1, sizeof(ebs_volume_data));
    if (!vol_data) {
        LOGERROR("Cannot allocate memory!\n");
        *dest = NULL;
        return EUCA_ERROR;
    }

    char *volume_start = strrchr(volume_string, '/') + sizeof(char);    //Go 1 past so token_start points to beginning of token
    char *token_start = strchr(volume_start, ',') + sizeof(char);   //Go 1 past the comma delimiter

    euca_strncpy(vol_data->volumeId, volume_start, token_start - volume_start);
    LOGTRACE("Parsed volume: %s\n", vol_data->volumeId);

    euca_strncpy(vol_data->token, token_start, strlen(token_start) + 1);
    LOGTRACE("Parse token: %s\n", vol_data->token);

    *dest = vol_data;
    return EUCA_OK;
}

//!
//! Serializes the ebs_volume_data struct into a single string that is
//! pointed to by the 'dest' argument
//!
//! @param[in] vol_data
//! @param[in] dest
//!
//! @return
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int serialize_volume(ebs_volume_data * vol_data, char **dest)
{
    if (vol_data == NULL) {
        LOGTRACE("Cannot serialize a NULL to volume string\n");
        return EUCA_ERROR;
    }

    char *working_string = NULL;
    int working_size = strlen(vol_data->token) + 1 + strlen(vol_data->volumeId) + 1;
    working_string = EUCA_ZALLOC(1, working_size);
    if (working_string == NULL) {
        LOGERROR("Cannot allocate memory!\n");
        return EUCA_ERROR;
    }
    //Ensure / at end of scURL
    snprintf(working_string, working_size, "%s%s,%s", VOLUME_STRING_PREFIX, vol_data->volumeId, vol_data->token);

    LOGTRACE("Serialized volume struct into %s\n", working_string);
    *dest = working_string;
    return EUCA_OK;
}

//!
//! New version, uses external sc url...likely derived from service info
//!
//! @param[in] sc_url
//! @param[in] attachment_token
//! @param[in] use_ws_sec
//! @param[in] ws_sec_policy_file
//! @param[in] local_ip
//! @param[in] local_iqn
//! @param[in] result_device
//! @param[in] vol_data
//!
//! @return
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int connect_ebs_volume(char *sc_url, char *attachment_token, int use_ws_sec, char *ws_sec_policy_file, char *local_ip, char *local_iqn, char **result_device,
                       ebs_volume_data ** vol_data)
{
    int rc = 0;
    int ret = 0;

    LOGTRACE("Parsing volume information from: %s\n", attachment_token);
    rc = deserialize_volume(attachment_token, vol_data);
    if (rc) {
        LOGERROR("Failed parsing volume string %s\n", attachment_token);
        ret = EUCA_ERROR;
        goto release;
    }

    LOGTRACE("Parsed volume info: volumeId=%s, scURL=%s, encrypted token=%s\n", (*vol_data)->volumeId, sc_url, (*vol_data)->token);

    LOGTRACE("Requesting volume lock\n");
    sem_p(vol_sem);                    //Acquire the lock
    LOGTRACE("Got volume lock\n");

    LOGTRACE("Calling ExportVolume on SC at %s\n", sc_url);
    char *connect_string = NULL;
    rc = scClientCall(NULL, NULL, use_ws_sec, ws_sec_policy_file, DEFAULT_SC_CALL_TIMEOUT, sc_url, "ExportVolume", (*vol_data)->volumeId, (*vol_data)->token, local_ip, local_iqn,
                      &connect_string);
    if (rc) {
        LOGERROR("Failed to get connection information for volume %s from storage controller at: %s\n", (*vol_data)->volumeId, sc_url);
        ret = EUCA_ERROR;
        goto release;
    } else {
        if (euca_strncpy((*vol_data)->connect_string, connect_string, EBS_CONNECT_STRING_MAX_LENGTH)) {
            ret = EUCA_OK;
        } else {
            LOGERROR("Failed to copy connect string from SC response: %s\n", connect_string);
            ret = EUCA_ERROR;
        }

    }

    //copy the connection info from the SC return to the resourceLocation.
    char *dev = connect_iscsi_target((*vol_data)->connect_string);
    if (!dev || !strstr(dev, "/dev")) {
        LOGERROR("Failed to connect to iSCSI target: %s\n", (*vol_data)->connect_string);
        ret = EUCA_ERROR;
        goto release;
    }

    *result_device = dev;

release:
    LOGTRACE("Releasing volume lock\n");
    sem_v(vol_sem);
    LOGTRACE("Released volume lock\n");
    return ret;
}

//!
//! Detach a local device that is iSCSI and disconnect the session.
//!
//! @param[in] sc_url
//! @param[in] use_ws_sec
//! @param[in] ws_sec_policy_file
//! @param[in] volume_string
//! @param[in] connect_string
//! @param[in] local_ip
//! @param[in] local_iqn
//!
//! @return
//!
//! @pre
//!
//! @post
//!
//! @note should only be invoked after detachment from the guest
//!
int disconnect_ebs_volume(char *sc_url, int use_ws_sec, char *ws_sec_policy_file, char *volume_string, char *connect_string, char *local_ip, char *local_iqn)
{
    int ret = 0;
    int rc = 0;
    int norescan = 0;                  //send a 0 to indicate no rescan requested
    ebs_volume_data *vol_data = NULL;

    LOGTRACE("Disconnecting an EBS volume\n");

    rc = deserialize_volume(volume_string, &vol_data);
    if (rc) {
        LOGERROR("Could not deserialize volume string %s\n", volume_string);
        return EUCA_ERROR;
    }

    LOGTRACE("Requesting volume lock\n");

    //Grab a lock.
    sem_p(vol_sem);
    LOGTRACE("Got volume lock\n");

    LOGDEBUG("[%s] attempting to disconnect iscsi target\n", vol_data->volumeId);
    if (disconnect_iscsi_target(connect_string, norescan) != 0) {
        LOGERROR("[%s] failed to disconnet iscsi target\n", vol_data->volumeId);
        ret = EUCA_ERROR;
        goto release;
    }

    if (ret == EUCA_ERROR) {
        LOGDEBUG("Skipping SC Call due to previous errors\n");
        goto release;
    } else {
        //TODO: decrypt token using node pk
        LOGTRACE("Calling scClientCall with url: %s and token %s\n", sc_url, vol_data->token);
        rc = scClientCall(NULL, NULL, use_ws_sec, ws_sec_policy_file, DEFAULT_SC_CALL_TIMEOUT, sc_url, "UnexportVolume", vol_data->volumeId, vol_data->token, local_ip, local_iqn);
        if (rc) {
            LOGERROR("ERROR unexporting volume %s\n", vol_data->volumeId);
            ret = EUCA_ERROR;
            goto release;
        } else {
            //Ok, now refresh local session to be sure it's gone.
            char *refreshedDev = NULL;
            //Should return error of not found.
            refreshedDev = get_iscsi_target(connect_string);
            if (refreshedDev) {
                //Failure, should have NULL.
                ret = EUCA_ERROR;
                goto release;
            } else {
                //We're good
                ret = EUCA_OK;
            }
        }
    }

release:
    LOGTRACE("Releasing volume lock\n");
    //Release the volume lock
    sem_v(vol_sem);
    LOGTRACE("Released volume lock\n");
    return ret;
}

//!
//! Assumes that the vol_data struct is populated, including the connection_string
//!
//! @param[in] sc_url
//! @param[in] use_ws_sec
//! @param[in] ws_sec_policy_file
//! @param[in] vol_data
//! @param[in] local_ip
//! @param[in] local_iqn
//!
//! @return
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int disconnect_ebs_volume_with_struct(char *sc_url, int use_ws_sec, char *ws_sec_policy_file, ebs_volume_data * vol_data, char *local_ip, char *local_iqn)
{
    int ret = 0;
    int rc = 0;
    if (vol_data == NULL) {
        LOGERROR("Could not disconnect volume, got null volume data struct\n");
        return EUCA_ERROR;
    }

    LOGTRACE("Requesting volume lock\n");
    //Grab a lock.
    sem_p(vol_sem);
    LOGTRACE("Got volume lock\n");
    LOGDEBUG("[%s] attempting to disconnect iscsi target\n", vol_data->volumeId);
    if (disconnect_iscsi_target(vol_data->connect_string, 0) != 0) {
        LOGERROR("[%s] failed to disconnet iscsi target\n", vol_data->volumeId);
        //if (!force)
        ret = EUCA_ERROR;
        goto release;
    }

    if (ret == EUCA_ERROR) {
        LOGDEBUG("Skipping SC Call due to previous errors\n");
        goto release;
    } else {
        //TODO: decrypt token using node pk
        LOGTRACE("Calling scClientCall with url: %s and token %s\n", sc_url, vol_data->token);
        rc = scClientCall(NULL, NULL, use_ws_sec, ws_sec_policy_file, DEFAULT_SC_CALL_TIMEOUT, sc_url, "UnexportVolume", vol_data->volumeId, vol_data->token, local_ip, local_iqn);

        if (!rc) {
            LOGERROR("ERROR unexporting volume %s\n", vol_data->volumeId);
            ret = EUCA_ERROR;
            goto release;
        } else {
            //Ok, now refresh local session to be sure it's gone.
            char *refreshedDev = NULL;
            //Should return error of not found.
            refreshedDev = get_iscsi_target(vol_data->connect_string);
            if (refreshedDev) {
                //Failure, should have NULL.
                ret = EUCA_ERROR;
                goto release;
            } else {
                //We're good
                ret = EUCA_OK;
            }
        }
    }

release:
    LOGTRACE("Releasing volume lock\n");
    //Release the volume lock
    sem_v(vol_sem);
    LOGTRACE("Released volume lock\n");
    return ret;
}

#ifdef _UNIT_TEST
//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char **argv)
{
    char *sc_url_prefix = "sc://";
    char *sc_ur1 = "http://192.168.1.1:8773/services/Storage";
    char *sc_ur1_slash = "http://192.168.1.1:8773/services/Storage/";

    char *sc_hostname_url = "http://testhost.com:8773/services/Storage";
    char *sc_hostname_ur1_slash = "http://testhost.com:8773/services/Storage/";

    char *serialized1 = "sc://vol-123ABCD,10aavaeosvas-sd-adsf-asdfa-vasdva";
    char *serialized2 = "sc://vol-123ABCD,10aavaeosvas-sd-adsf-asdfa-vasdva";

    ebs_volume_data test_struct1 = { "testtoken123", "vol-AB123DD" };
    ebs_volume_data test_struct2 = { "1aoivlna-adflnew-aavaa0an12zc", "vol-123ABCD" };

    char *output = NULL;
    ebs_volume_data *ebs_out = NULL;
    int result;

    printf("Testing serialization\n");
    result = serialize_volume(&test_struct1, &output);
    printf("Got serialized output: %s\n", output);
    EUCA_FREE(output);

    result = serialize_volume(&test_struct2, &output);
    printf("Got serialized output: %s\n", output);
    EUCA_FREE(output);

    printf("Testing de-serialization\n");
    result = deserialize_volume(serialized1, &ebs_out);
    printf("Input %s\n\tGot de-serialized: %s %s\n", serialized1, ebs_out->volumeId, ebs_out->token);
    EUCA_FREE(ebs_out);

    result = deserialize_volume(serialized2, &ebs_out);
    printf("Input %s\n\tGot de-serialized: %s %s\n", serialized2, ebs_out->volumeId, ebs_out->token);
    EUCA_FREE(ebs_out);

    //int replace_sc_url(const char * volume_string, const char * scUrl, char * restrict dest_string)
    /*printf("Testing url replacement\n");
       char url_out1[512];
       result = replace_sc_url(serialized1, "http://localhost.com:8773/services/Storage", url_out1);
       printf("Result string %s\n", url_out1);

       char url_out2[512];
       result = replace_sc_url(serialized2, "http://192.168.1.1:8773/services/Storage/", url_out2);
       printf("Result string %s\n", url_out2);

       char url_out3[512];
       result = replace_sc_url(serialized3, "http://localhost.com:8773/services/Storage", url_out3);
       printf("Result string %s\n", url_out3); */

    return (0);
}
#endif /* _UNIT_TEST */
