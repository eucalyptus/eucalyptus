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
//! @file storage/Wclient.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>
#include <unistd.h>                    /* getopt */
#include <fcntl.h>                     /* open */
#include <sys/types.h>
#include <sys/stat.h>

#include <eucalyptus.h>
#include <euca_auth.h>
#include <misc.h>
#include <euca_string.h>
#include "objectstorage.h"
#include "http.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define BUFSIZE                                  262144 //!< should be big enough for CERT and the signature
#define STRSIZE                                    1024 //!< for short strings: files, hosts, URLs

#define OBJECT_STORAGE_ENDPOINT                          "/services/objectstorage"
#define DEFAULT_HOST_PORT                        "localhost:8773"
#define DEFAULT_COMMAND                          "GetObject"

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

boolean debug = FALSE;

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

#define USAGE()                                                                                                                                                      \
{                                                                                                                                                                    \
	fprintf(stderr, "Usage: Wclient [GetDecryptedImage|GetObject|HttpPut] -h [host:port] -u [URL] -m [manifest] -f [in|out file] -l [login] -p [password] [-z]\n"); \
	exit(1);                                                                                                                                                        \
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

 //!
 //! Main entry point of the application
 //!
 //! @param[in] argc the number of parameter passed on the command line
 //! @param[in] argv the list of arguments
 //!
 //! @return EUCA_OK
 //!
int main(int argc, char *argv[])
{
    int ch = 0;
    int result = 0;
    int tmp_fd = -1;
    char *tmp_name = NULL;
    char *command = DEFAULT_COMMAND;
    char *hostport = NULL;
    char *manifest = NULL;
    char *file_name = NULL;
    char *url = NULL;
    char *login = NULL;
    char *password = NULL;
    char request[STRSIZE] = { 0 };
    boolean do_compress = FALSE;
    boolean do_get = FALSE;

    while ((ch = getopt(argc, argv, "dh:m:f:zu:l:p:")) != -1) {
        switch (ch) {
        case 'h':
            hostport = optarg;
            break;
        case 'm':
            manifest = optarg;
            break;
        case 'd':
            debug = TRUE;
            break;
        case 'f':
            file_name = optarg;
            break;
        case 'u':
            url = optarg;
            break;
        case 'l':
            login = optarg;
            break;
        case 'p':
            password = optarg;
            break;
        case 'z':
            do_compress = TRUE;
            break;
        case '?':
        default:
            USAGE();
            break;
        }
    }
    argc -= optind;
    argv += optind;

    if (argc > 0) {
        command = argv[0];
    }

    if (strcmp(command, "GetDecryptedImage") == 0 || strcmp(command, "GetObject") == 0) {
        if (manifest == NULL) {
            fprintf(stderr, "Error: manifest must be specified\n");
            USAGE();
        }
        do_get = TRUE;
    } else if (strcmp(command, "HttpPut") == 0) {
        if (url == NULL || file_name == NULL) {
            fprintf(stderr, "Error: URL and input file must be specified\n");
            USAGE();
        }
        do_get = FALSE;
    } else {
        fprintf(stderr, "Error: unknown command [%s]\n", command);
        USAGE();
    }

    if (do_get) {
        /* use a temporary file for network data */
        tmp_name = strdup("objectstorage-download-XXXXXX");
        tmp_fd = safe_mkstemp(tmp_name);
        if (tmp_fd < 0) {
            fprintf(stderr, "Error: failed to create a temporary file\n");
            USAGE();
        }
        close(tmp_fd);

        if (hostport) {
            snprintf(request, STRSIZE, "http://%s%s/%s", hostport, OBJECT_STORAGE_ENDPOINT, manifest);
            if (strcmp(command, "GetObject") == 0) {
                result = objectstorage_object_by_url(request, tmp_name, do_compress);
            } else {
                result = objectstorage_image_by_manifest_url(request, tmp_name, do_compress);
            }
        } else {
            euca_strncpy(request, manifest, STRSIZE);
            if (strcmp(command, "GetObject") == 0) {
                result = objectstorage_object_by_path(request, tmp_name, do_compress);
            } else {
                result = objectstorage_image_by_manifest_path(request, tmp_name, do_compress);
            }
        }

        if (result) {
            /* error has occured */
            cat(tmp_name);
            fprintf(stderr, "\n");     /* in case error doesn't end with a newline */
            remove(tmp_name);
        } else {
            /* all's well */
            if (file_name) {
                rename(tmp_name, file_name);
            } else {
                fprintf(stderr, "Saved output in %s\n", tmp_name);
            }
        }

        EUCA_FREE(tmp_name);
    } else {                           // HttpPut
        result = http_put(file_name, url, login, password);
    }
    return (EUCA_OK);
}
