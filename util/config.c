// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
//! @file util/config.c
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
#include <sys/stat.h>
#include <unistd.h>
#include <sys/errno.h>
#include <assert.h>
#include <string.h>

#include "eucalyptus.h"
#include "misc.h"
#include "config.h"

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

//! @{
//! @name Manages the configurations parameters that are only picked on application restart

static u32 configRestartLen = 0;       //!< Number of items present in the restart list
static configEntry *aConfigKeysRestart = NULL;  //!< The key pair list of items where key is the config parameter name and value is the default value
static char *asConfigValuesRestart[256] = { NULL }; //!< The list of modified values for each items in the list

//! @}

//! @{
//! @name Manages the configurations parameters that are only picked at all time during the application lifecycle

static u32 configNoRestartLen = 0;     //!< Number of items present in the no-restart list
static configEntry *aConfigKeysNoRestart = NULL;    //!< The key pair list of items where key is the config parameter name and value is the default value
static char *asConfigValuesNoRestart[256] = { NULL };   //!< The list of modified values for each items in the list

//! @}

//! Hold the timestamp of when we last processed the config files
static time_t lastConfigMtime[4] = { 0, 0, 0, 0 };

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
//! Initialize our local configuration key parameters
//!
//! @param[in] aNewConfigKeysRestart the list of configuration keys available on restart
//! @param[in] aNewConfigKeysNoRestart the list of configuration keys available while the system is up
//!
//! @pre \li Both \p aNewConfigKeysRestart and \p aNewConfigKeysNoRestart fields must not be NULL
//!      \li Both \p aNewConfigKeysRestart and \p aNewConfigKeysNoRestart must be arrays
//!
//! @post The local aNewConfigKeysRestart and aNewConfigKeysNoRestart fields are set.
//!
void configInitValues(configEntry aNewConfigKeysRestart[], configEntry aNewConfigKeysNoRestart[])
{
    aConfigKeysRestart = aNewConfigKeysRestart;
    aConfigKeysNoRestart = aNewConfigKeysNoRestart;
}

//!
//! Checks wether or not a given list of configuration file has been modified since we
//! last read them.
//!
//! @param[in] asConfigFiles the list of configuration file names.
//! @param[in] numFiles the number of file names in the list
//!
//! @return 0 if the configuration files were not modified; 1 if the configuration files were
//!         modified; and -1 if an error occured while poking at the configuration files.
//!
//! @pre \li The \p asConfigFiles field must not be NULL
//!      \li The \p asConfigFiles field must be an array of config files name with a max length of EUCA_MAX_PATH
//!      \li The \p asConfigFiles field must contain \p numFiles elements
//!      \li The \p numFiles field must be equal to 1 at the very least
//!
//! @post On success, the lastConfigMtime field is updated
//!
int isConfigModified(char asConfigFiles[][EUCA_MAX_PATH], u32 numFiles)
{
    u32 i = 0;
    u32 statone = 0;
    time_t configMtime[4] = { 0, 0, 0, 0 };
    struct stat statbuf = { 0 };

    for (i = 0; i < numFiles; i++) {
        // stat the config file, update modification time
        if (stat(asConfigFiles[i], &statbuf) == 0) {
            if ((statbuf.st_mtime > 0) || (statbuf.st_ctime > 0)) {
                if (statbuf.st_ctime > statbuf.st_mtime) {
                    configMtime[i] = statbuf.st_ctime;
                } else {
                    configMtime[i] = statbuf.st_mtime;
                }
            }
        }
    }

    statone = 0;
    for (i = 0; i < numFiles; i++) {
        if (configMtime[i] != 0) {
            statone++;
        }
    }

    if (statone == 0) {
        LOGERROR("could not stat any config files\n");
        return (-1);
    }

    for (i = 0; i < numFiles; i++) {
        if (lastConfigMtime[i] != configMtime[i]) {
            LOGDEBUG("file=%s current mtime=%ld, stored mtime=%ld\n", asConfigFiles[i], configMtime[i], lastConfigMtime[i]);
            lastConfigMtime[i] = configMtime[i];
            return (1);
        }
    }
    return (0);
}

//!
//! Retrieves a configuration value based on the given keyname.
//!
//! @param[in] sKey the name of the key for which we're looking for its paired value
//!
//! @return a string copy of the value matching the provided keyname or NULL if the
//!         key is not found. The caller of this function is responsible to free the
//!         returned string value.
//!
//! @pre \li The \p sKey field should not be NULL.
//!      \li The \p sKey field should exist in the configKeysRestart list or the configKeysNoRestart list
//!
//! @post a duplicate string the allocated for the element matching the key.
//!
//! @note the caller is responsible for freeing the allocated memory
//!
char *configFileValue(const char *sKey)
{
    u32 i = 0;

    // Make sure our parameters are valid
    if (sKey != NULL) {
        // Scan our configKeysRestart list first
        for (i = 0; i < configRestartLen; i++) {
            if (aConfigKeysRestart[i].key) {
                if (!strcmp(aConfigKeysRestart[i].key, sKey)) {
                    // Do we have a known value?
                    if (asConfigValuesRestart[i])
                        return (strdup(asConfigValuesRestart[i]));

                    // Do we have a default value?
                    if (aConfigKeysRestart[i].defaultValue)
                        return (strdup(aConfigKeysRestart[i].defaultValue));

                    // Doh!!
                    return (NULL);
                }
            }
        }

        // Scan our configKeysNoRestart list second if not found in configKeysRestart
        for (i = 0; i < configNoRestartLen; i++) {
            if (aConfigKeysNoRestart[i].key) {
                if (!strcmp(aConfigKeysNoRestart[i].key, sKey)) {
                    // Do we have a known value?
                    if (asConfigValuesNoRestart[i])
                        return (strdup(asConfigValuesNoRestart[i]));

                    // Do we have a default value?
                    if (aConfigKeysNoRestart[i].defaultValue)
                        return (strdup(aConfigKeysNoRestart[i].defaultValue));

                    // Doh!!
                    return (NULL);
                }
            }
        }
    }

    return (NULL);
}

//!
//! Retrieves a "long" integer value from the configuration entry.
//!
//! @param[in]  sKey the name of the key for which we're looking for its paired value
//! @param[out] pVal the matching long value to be set if \p sKey is found
//!
//! @return TRUE if the key was found and the matching value was a valid long integer and
//!         the \p pVal value is set appropriately, otherwise FALSE is returned.
//!
//! @pre \li The \p sKey field should not be NULL
//!      \li The \p pVal field must not be NULL
//!      \li The \p sKey must match a config value in our lists and be of type "long"
//!
//! @post The matching value is converted to a long integer and assigned to val and TRUE is returned.
//!
boolean configFileValueLong(const char *sKey, long *pVal)
{
    long v = 0;
    char *endptr = NULL;
    char *tmpstr = configFileValue(sKey);
    boolean found = FALSE;

    if ((tmpstr != NULL) && (pVal != NULL)) {
        errno = 0;
        v = (long)strtoll(tmpstr, &endptr, 10);
        if ((errno == 0) && ((*endptr) == '\0')) {
            // successful complete conversion
            (*pVal) = v;
            found = TRUE;
        }
    }
    // make sure we free out temp string
    EUCA_FREE(tmpstr);
    return (found);
}

//!
//! Reads a list of configuration files and fill in our configuration holders
//!
//! @param[in] asConfigFiles a list of configuration file path
//! @param[in] numFiles the number of configuration files in the list
//!
//! @return the number of configuration parameteres that were actually updated.
//!
//! @pre The \p asConfigFiles list must not be NULL and should contain as least 1 item
//!
//! @post our local asConfigValuesRestart and asConfigValuesNoRestart lists are being updated.
//!
int readConfigFile(char asConfigFiles[][EUCA_MAX_PATH], int numFiles)
{
    u32 i = 0;
    int ret = 0;
    char *old = NULL;
    char *new = NULL;

    for (i = 0; aConfigKeysRestart[i].key; i++) {
        old = asConfigValuesRestart[i];
        new = getConfString(asConfigFiles, numFiles, aConfigKeysRestart[i].key);
        if (configRestartLen) {
            if ((!old && new) || (old && !new) || ((old && new) && strcmp(old, new))) {
                LOGWARN("configuration file changed (KEY=%s, ORIGVALUE=%s, NEWVALUE=%s): clean restart is required before this change "
                        "will take effect!\n", aConfigKeysRestart[i].key, SP(old), SP(new));
            }

            EUCA_FREE(new);
        } else {
            LOGINFO("read (%s=%s, default=%s)\n", aConfigKeysRestart[i].key, SP(new), SP(aConfigKeysRestart[i].defaultValue));
            EUCA_FREE(asConfigValuesRestart[i]);
            asConfigValuesRestart[i] = new;
            ret++;
        }
    }
    configRestartLen = i;

    for (i = 0; aConfigKeysNoRestart[i].key; i++) {
        old = asConfigValuesNoRestart[i];
        new = getConfString(asConfigFiles, numFiles, aConfigKeysNoRestart[i].key);

        if (configNoRestartLen) {
            if ((!old && new) || (old && !new) || ((old && new) && strcmp(old, new))) {
                LOGINFO("configuration file changed (KEY=%s, ORIGVALUE=%s, NEWVALUE=%s): change will take effect immediately.\n", aConfigKeysNoRestart[i].key, SP(old), SP(new));
                ret++;
                EUCA_FREE(asConfigValuesNoRestart[i]);
                asConfigValuesNoRestart[i] = new;
            } else {
                EUCA_FREE(new);
            }
        } else {
            LOGINFO("read (%s=%s, default=%s)\n", aConfigKeysNoRestart[i].key, SP(new), SP(aConfigKeysNoRestart[i].defaultValue));
            EUCA_FREE(asConfigValuesNoRestart[i]);
            asConfigValuesNoRestart[i] = new;
            ret++;
        }
    }
    configNoRestartLen = i;

    return (ret);
}

//!
//! Helper for reading log-related params from eucalyptus.conf
//!
//! @param[out] pLogLevel a pointer to the memory location where to store the log level value
//! @param[out] pLogRollNumber a pointer to the memory location where to store the log roll number value
//! @param[out] pLogMaxSizeBytes a pointer to the memory location where to store the maximum log size in bytes value
//! @param[out] psLogPrefix a pointer to the memory location where to store the log prefix string value
//!
//! @pre The given parameters should not be NULL. This function will check for provided NULL pointers. If any parameter
//!      is NULL, we simply won't set the value. The parameters we're looking for should also be present in the config
//!      files.
//!
//! @post For any non-NULL provided parameters and assuming that we have the information in the config
//!       files, the values will be set accordingly.
//!
void configReadLogParams(int *pLogLevel, int *pLogRollNumber, long *pLogMaxSizeBytes, char **psLogPrefix)
{
    long l = 0;
    char *sLogLevel = NULL;

    sLogLevel = configFileValue("LOGLEVEL");
    assert(sLogLevel != NULL);

    if (pLogLevel)
        (*pLogLevel) = log_level_int(sLogLevel);
    EUCA_FREE(sLogLevel);

    if (pLogRollNumber) {
        configFileValueLong("LOGROLLNUMBER", &l);
        (*pLogRollNumber) = ((int)l);
    }

    if (pLogMaxSizeBytes)
        configFileValueLong("LOGMAXSIZE", pLogMaxSizeBytes);

    if (psLogPrefix)
        (*psLogPrefix) = configFileValue("LOGPREFIX");
}
