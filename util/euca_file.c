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
//! @file util/euca_file.h
//! This file implements the various file and directory utility functions
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64           // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <dirent.h>
#include <string.h>                    // strlen, strcpy
#include <ctype.h>                     // isspace
#include <assert.h>
#include <stdarg.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/errno.h>                 // errno
#include <limits.h>
#include <wchar.h>

#include "eucalyptus.h"
#include "misc.h"
#include "euca_string.h"
#include "euca_file.h"

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
//! Remove a directory given by the psPath string pointer. If the 'force' parameter
//! is set to TRUE, the directory will be emptied and deleted. If the 'force'
//! parameter is set to FALSE then this is the equivalent of an 'rmdir()' system
//! calls which will not delete a non-empty directory.
//!
//! @param[in] psPath a constant pointer to the string containing the path to the directory to remove.
//! @param[in] force set to TRUE, will force remove a non-empty directory
//!
//! @return EUCA_OK on success or the following error code:
//!         EUCA_INVALID_ERROR if any of our pre-conditions are not met
//!         EUCA_SYSTEM_ERROR if the system fails to remove a file or directory in the process
//!         EUCA_MEMORY_ERROR if we fail to allocate memory during the process
//!
//! @see
//!
//! @pre \li psPath must not be NULL
//!      \li psPath must be a valid path to a directory
//!      \li if force is set to FALSE, the directory should be empty
//!
//! @post \li on success, the directory will be removed
//!       \li on failure, the directory may remain with some content
//!
//! @note
//!
int euca_rmdir(const char *psPath, boolean force)
{
    int result = EUCA_OK;
    char *psBuf = NULL;
    DIR *pDir = NULL;
    size_t len = 0;
    size_t pathLen = 0;
    struct dirent *pDirEnt = NULL;
    struct stat statbuf = { 0 };

    // Make sure we have a path
    if (psPath == NULL) {
        return (EUCA_INVALID_ERROR);
    }
    // If force was set, read the directory and empty it
    if (force) {
        // Retrieve the length of our directory path
        pathLen = strlen(psPath);

        // Open the directory and start scanning for items
        if ((pDir = opendir(psPath)) != NULL) {
            while ((result == EUCA_OK) && ((pDirEnt = readdir(pDir)) != NULL)) {
                // Skip the names "." and ".." as we don't want to recurse on them.
                if (!strcmp(pDirEnt->d_name, ".") || !strcmp(pDirEnt->d_name, "..")) {
                    continue;
                }

                len = pathLen + strlen(pDirEnt->d_name) + 2;
                if ((psBuf = EUCA_ALLOC(len, sizeof(char))) != NULL) {

                    snprintf(psBuf, len, "%s/%s", psPath, pDirEnt->d_name);

                    if (stat(psBuf, &statbuf) == 0) {
                        if (S_ISDIR(statbuf.st_mode)) {
                            result = euca_rmdir(psBuf, TRUE);
                        } else {
                            if (unlink(psBuf) != 0) {
                                result = EUCA_SYSTEM_ERROR;
                            }
                        }
                    }

                    EUCA_FREE(psBuf);
                } else {
                    // Memory failure
                    result = EUCA_MEMORY_ERROR;
                }
            }

            closedir(pDir);
        } else {
            // return the proper error
            if (errno == ENOTDIR)
                return (EUCA_INVALID_ERROR);
            return (EUCA_SYSTEM_ERROR);
        }
    }
    // If we were successful so far, remove the directory
    if (result == EUCA_OK) {
        if (rmdir(psPath) != 0) {
            // Set the proper return error
            if (errno == ENOTDIR)
                return (EUCA_INVALID_ERROR);
            return (EUCA_SYSTEM_ERROR);
        }
    }

    return (result);
}
