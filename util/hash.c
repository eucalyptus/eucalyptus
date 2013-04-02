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
//! @file util/hash.c
//! Implements various MD5 and Jenkins hash functionality
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
#include <string.h>                    // strlen, strcpy
#include <ctype.h>                     // isspace
#include <assert.h>
#include <stdarg.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <time.h>
#include <math.h>                      // powf
#include <fcntl.h>                     // open
#include <utime.h>                     // utime
#include <sys/wait.h>
#include <sys/types.h>
#include <dirent.h>                    // opendir, etc
#include <errno.h>                     // errno
#include <sys/time.h>                  // gettimeofday
#include <limits.h>
#include <openssl/md5.h>
#include <sys/mman.h>                  // mmap
#include <pthread.h>

#include "eucalyptus.h"
#include "misc.h"
#include "hash.h"
#include "euca_auth.h"                 // base64_enc
#include "vnetwork.h"                  // OK / ERROR

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
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int hash_b64enc_string(const char *in, char **out);

int str2md5str(char *sBuf, u32 bufSize, const char *sValue);

char *file2md5str(const char *path);

u32 jenkins(const char *key, size_t len);
int hexjenkins(char *sBuf, u32 bufSize, const char *sValue);

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
//! Computes the MD5 hash of a given string and encodes it on a 16 bytes string
//!
//! @param[in]  in  the buffer to encode
//! @param[out] out the result of the encoding operation (MD5_DIGEST_LENGTH bytes string)
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre Both in and out parameters must not be NULL.
//!
//! @post On success the out parameter will point the encoded MD5_DIGEST_LENGTH bytes hash. On failure,
//!       the out parameter value will be set to NULL explicitedly.
//!
int hash_b64enc_string(const char *in, char **out)
{
    u8 *md5ret = NULL;
    u8 hash[MD5_DIGEST_LENGTH + 1] = { 0 }; // +1 for NULL termination.

    // Make sure our given parameters are valid
    if (!in || !out) {
        return (EUCA_ERROR);
    }

    LOGDEBUG("in=%s\n", in);

    // Forces out to NULL in calse of failure
    *out = NULL;

    // Make use our hash buffer is set to all 0s.
    bzero(hash, sizeof(hash));

    // Compute the MD5 hash
    if ((md5ret = MD5(((const u8 *)in), strlen(in), hash)) != NULL) {
        // Encode our hash value
        if ((*out = base64_enc(hash, MD5_DIGEST_LENGTH)) == NULL) {
            return (EUCA_ERROR);
        }
    }

    return (EUCA_OK);
}

//!
//! Calculates the md5 hash of 'sValue' and places it into 'sBuf' in human readable hex values
//!
//! @param[in,out] sBuf    the string buffer that contains the result
//! @param[in]     bufSize the size of our output string buffer
//! @param[in]     sValue  the string to compute MD5 hash from
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR if the MD5 operation failed
//!         \li EUCA_INVALID_ERROR if the given paramters do not match our pre-conditions
//!         \li EUCA_NO_SPACE_ERROR if the provided sBuf is not big enough to contain the data
//!
//! @pre \li Both sBuf and sValue fields must not be NULL
//!      \li sBuf should be big enough to contain ((MD5_DIGEST_LENGTH * 2) + 1) characters.
//!
//! @post on success, the MD5 hash of sValue will be computed and sBuf will contain the human
//!       readable hex values.
//!
int str2md5str(char *sBuf, u32 bufSize, const char *sValue)
{
    u32 i = 0;
    char *pBuf = NULL;
    u8 md5digest[MD5_DIGEST_LENGTH + 1] = { 0 };    // +1 for NULL termination.

    // Make sure our parameters are valid
    if (!sBuf || !sValue)
        return (EUCA_INVALID_ERROR);

    // Make sure we have enough space to write the hash in the given buffer
    if (bufSize < ((MD5_DIGEST_LENGTH * 2) + 1))
        return (EUCA_NO_SPACE_ERROR);

    // zero out out digest array
    bzero(md5digest, sizeof(md5digest));

    // Compute the MD5 hash
    if (MD5(((const u8 *)sValue), strlen(sValue), md5digest) == NULL)
        return (EUCA_ERROR);

    // zero out the buffer
    bzero(sBuf, bufSize);

    // Convert the computed hash to readable hex values
    for (i = 0, pBuf = sBuf; i < MD5_DIGEST_LENGTH; i++, pBuf += 2) {
        sprintf(pBuf, "%02x", md5digest[i]);
    }

    return (EUCA_OK);
}

//!
//! Retrieves a new string with a hex value of an MD5 hash of a file (same as `md5sum`)
//! or NULL if there was an error.
//!
//! @param[in] path the path to the file to compute the MD5 hash
//!
//! @return a new string with a hex value of an MD5 hash of a file or NULL if error
//!
//! @pre The path parameter must not be NULL and must be a valid path to an existing file
//!
//! @post On success, an allocated string with the MD5 value is returned
//!
//! @note The caller is responsible for freeing the allocated memory for the returned value
//!
char *file2md5str(const char *path)
{
    u32 i = 0;
    int fd = -1;
    char *p = NULL;
    char *buf = NULL;
    char *md5string = NULL;
    u8 md5digest[MD5_DIGEST_LENGTH + 1] = { 0 };
    struct stat mystat = { 0 };

    // Make sure our given parameter is valid
    if (path == NULL)
        return (NULL);

    // Open the file for read only
    if ((fd = open(path, O_RDONLY)) < 0)
        return (NULL);

    // can we stat this file?
    if (fstat(fd, &mystat) >= 0) {
        // Create a memory map for this file descriptor for the size of this file
        if ((buf = mmap(0, mystat.st_size, PROT_READ, MAP_SHARED, fd, 0)) != MAP_FAILED) {
            // Zero out our digest buffer...
            bzero(md5digest, sizeof(md5digest));

            // Compute the MD5 checksum of the content of this file
            if (MD5(((const u8 *)buf), mystat.st_size, md5digest) != NULL) {
                // Allocate memory for our output string
                if ((md5string = EUCA_ZALLOC(((MD5_DIGEST_LENGTH * 2) + 1), sizeof(char))) != NULL) {
                    for (i = 0, p = md5string; i < MD5_DIGEST_LENGTH; i++, p += 2) {
                        sprintf(p, "%02x", md5digest[i]);
                    }
                }
            }
            // Don't forget to free mapped memory
            munmap(buf, mystat.st_size);
        }
    }
    // Don't forget to close our file
    close(fd);
    return (md5string);
}

//!
//! Jenkins hash function (from http://en.wikipedia.org/wiki/Jenkins_hash_function)
//!
//! @param[in] key the key string to compute the hash from
//! @param[in] len the length of the key string
//!
//! @return the Jenkins hash value of a key
//!
//! @pre The key parameter must not be NULL
//!
//! @post On success, the function will return the hash of the key
//!
u32 jenkins(const char *key, size_t len)
{
    register u32 i = 0;
    register u32 hash = 0;

    if (key) {
        for (hash = 0, i = 0; i < len; ++i) {
            hash += key[i];
            hash += (hash << 10);
            hash ^= (hash >> 6);
        }

        hash += (hash << 3);
        hash ^= (hash >> 11);
        hash += (hash << 15);
    }
    return (hash);
}

//!
//! Calculates a Jenkins hash of 'str' and places it into 'buf' in hex
//!
//! @param[in,out] sBuf    the string buffer that contains the result
//! @param[in]     bufSize the size of our string buffer
//! @param[in]     sValue  the string to compute Jenkins hash from
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_INVALID_ERROR: If any parameter does not meet the preconditions
//!
//! @pre \li Both buf and str parameters must not be NULL.
//!      \li The buf_size parameter must be greater than 0 and should contain at least 8 characters.
//!
//! @post On success, the buf parameter will contain the hexadecimal string representation
//!       of the Jenkins hash.
//!
int hexjenkins(char *sBuf, u32 bufSize, const char *sValue)
{
    if ((sBuf != NULL) && (bufSize > 0) && (sValue != NULL)) {
        snprintf(sBuf, bufSize, "%08x", jenkins(sValue, strlen(sValue)));
        return (EUCA_OK);
    }
    return (EUCA_INVALID_ERROR);
}
