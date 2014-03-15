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
//! @file util/atomic_file.c
//! This file needs a description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

#include <log.h>
#include <http.h>
#include <hash.h>
#include "atomic_file.h"

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
//! Function description.
//!
//! @param[in] file
//! @param[in] source
//! @param[in] dest
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int atomic_file_init(atomic_file * file, char *source, char *dest, int dosort)
{
    if (!file) {
        return (1);
    }

    atomic_file_free(file);

    snprintf(file->dest, EUCA_MAX_PATH, "%s", dest);
    snprintf(file->source, EUCA_MAX_PATH, "%s", source);
    snprintf(file->tmpfilebase, EUCA_MAX_PATH, "%s-XXXXXX", dest);
    snprintf(file->tmpfile, EUCA_MAX_PATH, "%s-XXXXXX", dest);
    file->lasthash = strdup("UNSET");
    file->currhash = strdup("UNSET");
    file->dosort = dosort;
    return (0);
}

//!
//! Function description.
//!
//! @param[in] file
//! @param[in] file_updated
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int atomic_file_get(atomic_file * file, int *file_updated)
{
    int port = 0;
    int fd = 0;
    int ret = 0;
    int rc = 0;
    char *hash = NULL;
    char type[32] = "";
    char hostname[512] = "";
    char path[EUCA_MAX_PATH] = "";
    char tmpsource[EUCA_MAX_PATH] = "";
    char tmppath[EUCA_MAX_PATH] = "";

    if (!file || !file_updated) {
        return (1);
    }

    ret = 0;
    *file_updated = 0;

    snprintf(file->tmpfile, EUCA_MAX_PATH, "%s", file->tmpfilebase);
    fd = safe_mkstemp(file->tmpfile);
    if (fd < 0) {
        LOGERROR("cannot open tmpfile '%s': check permissions\n", file->tmpfile);
        return (1);
    }
    chmod(file->tmpfile, 0600);
    close(fd);

    snprintf(tmpsource, EUCA_MAX_PATH, "%s", file->source);
    type[0] = tmppath[0] = path[0] = hostname[0] = '\0';
    port = 0;

    tokenize_uri(tmpsource, type, hostname, &port, tmppath);
    snprintf(path, EUCA_MAX_PATH, "/%s", tmppath);

    if (!strcmp(type, "http")) {
        rc = http_get_timeout(file->source, file->tmpfile, 0, 0, 10, 15);
        if (rc) {
            LOGERROR("http client failed to fetch file URL=%s: check http server status\n", file->source);
            ret = 1;
        }
    } else if (!strcmp(type, "file")) {
        if (!strlen(path) || copy_file(path, file->tmpfile)) {
            LOGERROR("could not copy source file (%s) to dest file (%s): check permissions\n", path, file->tmpfile);
            ret = 1;
        }
    } else {
        LOGWARN("BUG: incompatible URI type (%s) passed to routine (only supports http, file)\n", type);
        ret = 1;
    }

    if (!ret) {
        if (file->dosort) {
            rc = atomic_file_sort_tmpfile(file);
            if (rc) {
                LOGWARN("could not sort tmpfile (%s) inplace: continuing without sort\n", file->tmpfile);
            }
        }
        // do checksum - only copy if file has changed
        hash = file2md5str(file->tmpfile);
        if (!hash) {
            LOGERROR("could not compute hash of tmpfile (%s): check permissions\n", file->tmpfile);
            ret = 1;
        } else {
            if (file->currhash)
                EUCA_FREE(file->currhash);
            file->currhash = hash;
            if (check_file(file->dest) || strcmp(file->currhash, file->lasthash)) {
                // hashes are different, put new file in place
                LOGINFO("update triggered due to file update (%s)\n", file->dest);
                LOGDEBUG("source and destination file contents have become different, triggering update of dest (%s)\n", file->dest);
                LOGDEBUG("renaming file %s -> %s\n", file->tmpfile, file->dest);
                if (rename(file->tmpfile, file->dest)) {
                    LOGERROR("could not rename (move) source file '%s' to dest file '%s': check permissions\n", file->tmpfile, file->dest);
                    ret = 1;
                } else {
                    EUCA_FREE(file->lasthash);
                    file->lasthash = strdup(file->currhash);
                    *file_updated = 1;
                }
            }
        }
    }

    unlink(file->tmpfile);
    return (ret);
}

//!
//! Function description.
//!
//! @param[in] file
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int atomic_file_sort_tmpfile(atomic_file * file)
{
    int currlines = 0;
    int i = 0;
    int fd = 0;
    int ret = 0;
    char **contents = NULL;
    char buf[4096] = "";
    char tmpfile[EUCA_MAX_PATH] = "";
    FILE *IFH = NULL;
    FILE *OFH = NULL;

    snprintf(tmpfile, EUCA_MAX_PATH, "%s-XXXXXX", file->dest);
    if ((fd = safe_mkstemp(tmpfile)) < 0) {
        LOGERROR("cannot open tmpfile '%s': check permissions\n", tmpfile);
        return (1);
    }
    chmod(tmpfile, 0600);
    close(fd);

    buf[0] = '\0';
    if ((IFH = fopen(file->tmpfile, "r")) != NULL) {
        while (fgets(buf, 4096, IFH)) {
            currlines++;
            if ((contents = realloc(contents, sizeof(char *) * currlines)) == NULL) {
                LOGFATAL("out of memory!\n");
                exit(1);
            }
            contents[currlines - 1] = strdup(buf);
        }
        fclose(IFH);

        if (contents) {
            qsort(contents, currlines, sizeof(char *), strcmp_ptr);
            if ((OFH = fopen(tmpfile, "w")) != NULL) {
                for (i = 0; i < currlines; i++) {
                    fprintf(OFH, "%s", contents[i]);
                    EUCA_FREE(contents[i]);
                }
                fclose(OFH);
                rename(tmpfile, file->tmpfile);
            }
            EUCA_FREE(contents);
        }
    }

    return (ret);
}

//!
//! Function description.
//!
//! @param[in] file
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int atomic_file_free(atomic_file * file)
{
    if (!file)
        return (1);

    if (file->lasthash)
        EUCA_FREE(file->lasthash);

    if (file->currhash)
        EUCA_FREE(file->currhash);
    bzero(file, sizeof(atomic_file));
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ina
//! @param[in] inb
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int strcmp_ptr(const void *ina, const void *inb)
{
    char **a = NULL;
    char **b = NULL;

    a = ((char **)ina);
    b = ((char **)inb);
    return (strcmp(*a, *b));
}
