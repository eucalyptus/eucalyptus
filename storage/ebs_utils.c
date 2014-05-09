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
#include <euca_auth.h>
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
static int request_timeout_sec = DEFAULT_SC_REQUEST_TIMEOUT;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int cleanup_volume_attachment(char *sc_url, int use_ws_sec, char *ws_sec_policy_file, ebs_volume_data * vol_data, char *connect_string, char *local_ip, char *local_iqn,
                                     int do_rescan);
static int redact_token(char *src_token, char *redacted);   //! Returns a redacted version of the token (eg. 'advaoiaavae' -> '*****avae'
static int re_encrypt_token(char *in_token, char **out_token);  //! Decrypts token with NC cert and re-encrypts with the cloud public cert

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
int init_ebs_utils(int sc_request_timeout_sec)
{
    LOGDEBUG("Initializing EBS utils\n");
    request_timeout_sec = sc_request_timeout_sec;
    if (vol_sem == NULL) {
        vol_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
    }
    LOGDEBUG("Completed EBS util initialization\n");
    return EUCA_OK;
}

//!
//! Gets the local device for the volume connection string
//!
//! @param[in] connection_string
//!
//! @return the device name string for the local device associated with connection_string (e.g. '/dev/sdd')
//!
//! @pre
//!
//! @post
//!
//! @note
//!
char *get_volume_local_device(const char *connection_string)
{
    if (connection_string == NULL) {
        LOGERROR("Cannot get local device for NULL connection string\n");
        return NULL;
    }
    // Invoke the iscsi stuff.
    return get_iscsi_target(connection_string);
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
    if (volume_string == NULL || strlen(volume_string) <= strlen(VOLUME_STRING_PREFIX)) {
        *dest = NULL;
        return EUCA_ERROR;
    }

    ebs_volume_data *vol_data = EUCA_ZALLOC(1, sizeof(ebs_volume_data));
    if (vol_data == NULL) {
        LOGERROR("Cannot allocate memory!\n");
        *dest = NULL;
        return EUCA_ERROR;
    }

    char *volume_start = volume_string + strlen(VOLUME_STRING_PREFIX);  //skip the prefix.
    if (volume_start == NULL) {
        LOGERROR("Failed parsing token string: %s\n", volume_string);
        EUCA_FREE(vol_data);
        return EUCA_ERROR;
    }
    char *token_start = strchr(volume_start, ',');
    if (token_start == NULL) {
        LOGERROR("Failed parsing token string: %s\n", volume_string);
        EUCA_FREE(vol_data);
        return EUCA_ERROR;
    }
    token_start += sizeof(char);       //Go 1 past the comma delimiter

    if (euca_strncpy(vol_data->volumeId, volume_start, token_start - volume_start) == NULL) {
        EUCA_FREE(vol_data);
        return EUCA_ERROR;
    }

    if (euca_strncpy(vol_data->token, token_start, strlen(token_start) + 1) == NULL) {
        EUCA_FREE(vol_data);
        return EUCA_ERROR;
    }

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
    int out_size = -1;
    char *working_string = NULL;
    int working_size = -1;

    if (vol_data == NULL) {
        LOGTRACE("Cannot serialize a NULL to volume string\n");
        return EUCA_ERROR;
    }

    working_size = strlen(vol_data->token) + 1 + strlen(vol_data->volumeId) + 1;
    working_string = EUCA_ZALLOC(1, working_size);
    if (working_string == NULL) {
        LOGERROR("Cannot allocate memory!\n");
        return EUCA_ERROR;
    }
    //Ensure / at end of scURL
    out_size = snprintf(working_string, working_size, "%s%s,%s", VOLUME_STRING_PREFIX, vol_data->volumeId, vol_data->token);
    if (out_size <= 0 || out_size > working_size) {
        EUCA_FREE(working_string);
        return EUCA_ERROR;
    }

    LOGTRACE("Serialized volume struct into %s\n", working_string);
    *dest = working_string;
    return EUCA_OK;
}

//!
//! Connects an EBS volume to the local host as an ISCSI block device. The resulting device is placed in *result_device
//!
//! @param[in] sc_url - The URL to reach the cluster's SC at.
//! @param[in] attachment_token - The attachment token/volume string received in the attach request (or vbr)
//! @param[in] use_ws_sec - Boolean to use WS-SEC on SC call.
//! @param[in] ws_sec_policy_file - Policy file for WS-SEC on SC call.
//! @param[in] local_ip - External IP of the local host. Will be used to send to SC for authorization
//! @param[in] local_iqn - IQN of local host for sending to SC for authorization.
//! @param[out] result_device - The block device that is the EBS volume connected to local host, will be populated on success.
//! @param[out] vol_data - The populated ebs_volume_data struct that hold volume info (id, connect string, token, etc)
//!
//! @return EUCA_OK on success, EUCA_ERROR on failure.
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
    int ret = EUCA_OK;
    char *reencrypted_token = NULL;
    char *connect_string = NULL;
    char *dev = NULL;
    int do_rescan = 1;

    if (sc_url == NULL || strlen(sc_url) == 0 || attachment_token == NULL || local_ip == NULL || local_iqn == NULL) {
        LOGERROR("Cannont connect ebs volume. Got NULL input parameters.\n");
        return EUCA_ERROR;
    }

    if (deserialize_volume(attachment_token, vol_data) != EUCA_OK || *vol_data == NULL) {
        LOGERROR("Failed parsing volume string %s\n", attachment_token);
        ret = EUCA_ERROR;
        return ret;
    }

    if ((*vol_data)->volumeId == NULL || strlen((*vol_data)->volumeId) == 0 || (*vol_data)->token == NULL || strlen((*vol_data)->token) == 0) {
        LOGERROR("After deserializing volume string, still found null or empty volumeId or token");
        ret = EUCA_ERROR;
        return ret;
    }

    LOGTRACE("Parsed volume info: volumeId=%s, encrypted token=%s\n", (*vol_data)->volumeId, (*vol_data)->token);
    if (re_encrypt_token((*vol_data)->token, &reencrypted_token) != EUCA_OK || reencrypted_token == NULL || strlen(reencrypted_token) <= 0) {
        LOGERROR("Failed on re-encryption of token for call to SC\n");
        if (reencrypted_token != NULL) {
            EUCA_FREE(reencrypted_token);
        }
        return EUCA_ERROR;
    }

    LOGTRACE("Requesting volume lock\n");
    sem_p(vol_sem);                    //Acquire the lock, after this, failure requires 'goto release' for release of lock
    LOGTRACE("Got volume lock\n");

    LOGTRACE("Calling ExportVolume on SC at %s\n", sc_url);
    if (scClientCall(NULL, NULL, use_ws_sec, ws_sec_policy_file, request_timeout_sec, sc_url, "ExportVolume", (*vol_data)->volumeId, reencrypted_token, local_ip, local_iqn,
                     &connect_string) != EUCA_OK) {
        LOGERROR("Failed to get connection information for volume %s from storage controller at: %s\n", (*vol_data)->volumeId, sc_url);
        ret = EUCA_ERROR;
        goto release;
    } else {
        if (euca_strncpy((*vol_data)->connect_string, connect_string, EBS_CONNECT_STRING_MAX_LENGTH) == NULL) {
            LOGERROR("Failed to copy connect string from SC response: %s\n", connect_string);
            ret = EUCA_ERROR;
            goto release;
        }
    }

    //copy the connection info from the SC return to the resourceLocation.
    dev = connect_iscsi_target((*vol_data)->connect_string);
    if (!dev || !strstr(dev, "/dev")) {
        LOGERROR("Failed to connect to iSCSI target: %s\n", (*vol_data)->connect_string);
        //disconnect the volume
        if (cleanup_volume_attachment(sc_url, use_ws_sec, ws_sec_policy_file, (*vol_data), (*vol_data)->connect_string, local_ip, local_iqn, do_rescan) != EUCA_OK) {
            LOGTRACE("cleanup_volume_attachment returned failure on cleanup from connection failure\n");
        }
        ret = EUCA_ERROR;
        goto release;
    }

    *result_device = dev;

release:
    LOGTRACE("Releasing volume lock\n");
    sem_v(vol_sem);
    LOGTRACE("Released volume lock\n");

    if (reencrypted_token != NULL) {
        EUCA_FREE(reencrypted_token);
    }

    if (ret != EUCA_OK && (*vol_data) != NULL) {
        //Free the vol data struct too
        EUCA_FREE(*vol_data);
    }

    return ret;
}

//!
//! Detach a local device that is iSCSI and disconnect the session.
//!
//! @param[in] sc_url - The URL to reach the cluster's SC at.
//! @param[in] use_ws_sec - boolean to determine use of WS-SEC.
//! @param[in] ws_sec_policy_file - Policy file path for WS-SEC
//! @param[in] attachment_token - The volume/token string received in the request that will be used
//! @param[in] connect_string - The connect string used for attachment, to be re-used on disconnect
//! @param[in] local_ip - The local host's external IP
//! @param[in] local_iqn - The local host's IQN
//!
//! @return
//!
//! @pre
//!
//! @post
//!
//! @note should only be invoked after detachment from the guest
//!
int disconnect_ebs_volume(char *sc_url, int use_ws_sec, char *ws_sec_policy_file, char *attachment_token, char *connect_string, char *local_ip, char *local_iqn)
{
    int ret = EUCA_ERROR;
    int rc = 0;
    int norescan = 0;                  //send a 0 to indicate no rescan requested
    ebs_volume_data *vol_data = NULL;

    if (attachment_token == NULL || connect_string == NULL || local_ip == NULL || local_iqn == NULL) {
        LOGERROR("Cannont disconnect ebs volume. Got NULL input parameters.\n");
        return EUCA_ERROR;
    }

    LOGTRACE("Disconnecting an EBS volume\n");

    if (deserialize_volume(attachment_token, &vol_data) != EUCA_OK) {
        LOGERROR("Could not deserialize attachment token string %s\n", attachment_token);
        return EUCA_ERROR;
    }

    LOGTRACE("Requesting volume lock\n");
    sem_p(vol_sem);
    {
        LOGTRACE("Got volume lock\n");
        ret = cleanup_volume_attachment(sc_url, use_ws_sec, ws_sec_policy_file, vol_data, connect_string, local_ip, local_iqn, norescan);
        LOGTRACE("cleanup_volume_attachment returned: %d\n", ret);
        LOGTRACE("Releasing volume lock\n");
    }
    sem_v(vol_sem);
    LOGTRACE("Released volume lock\n");

    EUCA_FREE(vol_data);
    return ret;
}

//!
//! Assumes that the vol_data struct is populated, including the connection_string
//!
//! @param[in] sc_url - The URL to reach the cluster's SC at.
//! @param[in] use_ws_sec - boolean to determine use of WS-SEC.
//! @param[in] ws_sec_policy_file - Policy file path for WS-SEC
//! @param[in] vol_data - The ebs_volume_data struct pointer
//! @param[in] local_ip - The local host's external IP
//! @param[in] local_iqn - The local host's IQN
//!
//! @return EUCA_OK on success, EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int disconnect_ebs_volume_with_struct(char *sc_url, int use_ws_sec, char *ws_sec_policy_file, ebs_volume_data * vol_data, char *local_ip, char *local_iqn)
{
    int ret = EUCA_ERROR;
    int rc = 0;
    int do_rescan = 0;                 // don't do rescan

    if (vol_data == NULL) {
        LOGERROR("Could not disconnect volume, got null volume data struct\n");
        return EUCA_ERROR;
    }

    LOGTRACE("Requesting volume lock\n");
    //Grab a lock.
    sem_p(vol_sem);
    LOGTRACE("Got volume lock\n");

    ret = cleanup_volume_attachment(sc_url, use_ws_sec, ws_sec_policy_file, vol_data, vol_data->connect_string, local_ip, local_iqn, do_rescan);
    LOGTRACE("cleanup_volume_attachment returned: %d\n", ret);

    LOGTRACE("Releasing volume lock\n");
    //Release the volume lock
    sem_v(vol_sem);
    LOGTRACE("Released volume lock\n");
    return ret;
}

//!
//! Refactored logic for detaching a local iSCSI device and unexporting the volume. To be invoked only by other functions in this file after acquiring the necessary lock.
//!
//! @param[in] sc_url - The URL to reach the cluster's SC at.
//! @param[in] use_ws_sec - boolean to determine use of WS-SEC.
//! @param[in] ws_sec_policy_file - Policy file path for WS-SEC
//! @param[in] vol_data - The ebs_volume_data struct pointer
//! @param[in] connect_string - The connection string to use for local connection
//! @param[in] local_ip - The local host's external IP
//! @param[in] local_iqn - The local host's IQN
//! @param[in] do_rescan - Set to false to indicate no rescan should be done on disconnect, or true to use rescan
//!
//! @return EUCA_OK on success, EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
//! @note should be invoked only by functions in this file that acquired the necessary lock.
//!
static int cleanup_volume_attachment(char *sc_url, int use_ws_sec, char *ws_sec_policy_file, ebs_volume_data * vol_data, char *connect_string, char *local_ip, char *local_iqn,
                                     int do_rescan)
{
    int rc = 0;
    char *reencrypted_token = NULL;
    char *refreshedDev = NULL;

    if (vol_data == NULL || connect_string == NULL || local_ip == NULL || local_iqn == NULL) {
        LOGERROR("Cannot cleanup volume attachment. Got NULL input parameters.\n");
        return EUCA_ERROR;
    }

    LOGDEBUG("[%s] attempting to disconnect iscsi target\n", vol_data->volumeId);
    if (disconnect_iscsi_target(connect_string, do_rescan) != 0) {
        LOGERROR("[%s] failed to disconnect iscsi target\n", vol_data->volumeId);
        LOGDEBUG("Skipping SC Call due to previous errors\n");
        return EUCA_ERROR;
    }

    if (sc_url == NULL || strlen(sc_url) <= 0) {
        LOGERROR("[%s] Cannot invoke SC UnexportVolume, SC URL is invalid\n", vol_data->volumeId);
        return EUCA_ERROR;
    }

    rc = re_encrypt_token(vol_data->token, &reencrypted_token);
    if (rc != EUCA_OK || reencrypted_token == NULL || strlen(reencrypted_token) <= 0) {
        LOGERROR("Failed on re-encryption of token for call to SC\n");
        if (reencrypted_token != NULL) {
            EUCA_FREE(reencrypted_token);
        }
        return EUCA_ERROR;
    } else {
        LOGTRACE("Re-encrypted token for %s is %s\n", vol_data->volumeId, reencrypted_token);
    }

    LOGTRACE("Calling scClientCall with url: %s and token %s\n", sc_url, vol_data->token);
    if (scClientCall(NULL, NULL, use_ws_sec, ws_sec_policy_file, request_timeout_sec, sc_url, "UnexportVolume", vol_data->volumeId, reencrypted_token, local_ip, local_iqn) !=
        EUCA_OK) {
        EUCA_FREE(reencrypted_token);
        LOGERROR("ERROR unexporting volume %s\n", vol_data->volumeId);
        return EUCA_ERROR;
    } else {
        EUCA_FREE(reencrypted_token);
        //Ok, now refresh local session to be sure it's gone.
        //Should return error of not found.
        refreshedDev = get_iscsi_target(connect_string);
        if (refreshedDev) {
            //Failure, should have NULL.
            return EUCA_ERROR;
        } else {
            //We're good
            return EUCA_OK;
        }
    }
}

//! Decrypts the encrypted token and re-encrypts with the public cloud cert.
//! Used for preparation for request to SC for Export/Unexport.
//!
//! @param[in] in_token - the token encrypted with the NC public key
//! @param[in] out_token - the same token encrypted with CLC public key
//!
//! @return
//!
static int re_encrypt_token(char *in_token, char **out_token)
{
    int rc = 1;
    char *tmp_token = NULL;
    char redacted_token[512];

    if (in_token == NULL) {
        LOGERROR("Cannot re-encrypt NULL token\n");
        *out_token = NULL;
        return EUCA_ERROR;
    }

    if (decrypt_string_with_node(in_token, &tmp_token) != EUCA_OK || tmp_token == NULL) {
        LOGERROR("Failed decryption of token %s\n", in_token);
        *out_token = NULL;
        return EUCA_ERROR;
    }

    if (redact_token(tmp_token, redacted_token) != EUCA_OK) {
        LOGTRACE("Error redacting token value for log output. Continuing.");
    } else {
        LOGTRACE("Decrypted, redacted token: %s\n", redacted_token);
    }

    if (encrypt_string_with_cloud(tmp_token, out_token) != EUCA_OK || *out_token == NULL) {
        LOGERROR("Failed re-encryption of token %s\n", in_token);
        EUCA_FREE(tmp_token);
        *out_token = NULL;
        return EUCA_ERROR;
    }

    return EUCA_OK;
}

//!
//! Take the token plaintext and redact it to: '*********aaavds'
//! Replaces all but the last 4 chars with '*'. Will return failure
//! and do no-op if either param is NULL or strlen(src_token) <= 4
//!
//! @param[in] src_token - the token to redact
//! @param[in] redacted - the buffer to copy the redacted value into, must be
//!  be the same size as src_token
//!
//! @return EUCA_OK on success, EUCA_ERROR on fail
static int redact_token(char *src_token, char *redacted)
{
    int i = 0;
    if (src_token == NULL || redacted == NULL || strlen(src_token) <= 4) {
        return EUCA_ERROR;
    }

    for (i = 0; i < strlen(src_token) - 4; i++) {
        redacted[i] = '*';
    }

    //Include the null char at end of src_token
    for (; i < strlen(src_token) + 1; i++) {
        redacted[i] = src_token[i];
    }
    return EUCA_OK;
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
