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
//! @file gatherlog/GLclient.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <gl-client-marshal.h>
#include <euca_auth.h>
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
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return The result of gl_getLogs() or gl_getKeys() on success or the
//!         following error codes:
//!         \li EUCA_ERROR: if any error occured.
//!         \li EUCA_INVALID_ERROR: if we did not meet out preconditions
//!
//! @see gl_getLogs()
//! @see gl_getKeys()
//!
//! @pre \li The argc parameter must be at least 4
//!      \li The AXIS2C_HOME environment variable must be set
//!      \li The request argument must be getLogs or getKeys only
//!
//! @post On success, the output for getKeys or getLogs will be displayed
//!       to stdout. On failure, the proper error message will be displayed.
//!
int main(int argc, char **argv)
{
#define APPNAME_ARG    0
#define URI_ARG        1
#define REQUEST_ARG    2
#define SERVICE_ARG    3
#define NB_ARG         4

    int rc = EUCA_OK;
    char *cccert = NULL;
    char *nccert = NULL;
    axutil_env_t *env = NULL;
    axis2_char_t *client_home = NULL;
    axis2_char_t endpoint_uri[256] = { 0 };
    axis2_stub_t *stub = NULL;

    if (argc < NB_ARG) {
        printf("ERROR: passed %d arguments, expected %d\n", argc, NB_ARG);
        return (EUCA_INVALID_ERROR);
    }

    if ((client_home = AXIS2_GETENV("AXIS2C_HOME")) == NULL) {
        printf("ERROR: must have AXIS2C_HOME set\n");
        return (EUCA_INVALID_ERROR);
    }

    snprintf(endpoint_uri, 256, " http://%s/axis2/services/EucalyptusGL", argv[URI_ARG]);
    if ((env = axutil_env_create_all("/tmp/fooh", AXIS2_LOG_LEVEL_TRACE)) == NULL) {
        printf("ERROR: fail to retrieve AXIS2 environment.\n");
        return (EUCA_ERROR);
    }

    if ((stub = axis2_stub_create_EucalyptusGL(env, client_home, endpoint_uri)) == NULL) {
        printf("ERROR: fail to retrieve AXIS2 stub.\n");
        return (EUCA_ERROR);
    }

    /*if (!strcmp(argv[REQUEST_ARG], "getLogs")) {
       if ((rc = gl_getLogs(argv[SERVICE_ARG], &clog, &nlog, &hlog, &alog, env, stub)) == EUCA_OK) {
       if (clog)
       printf("CLOG\n----------\n%s\n-----------\n", base64_dec(((unsigned char *)clog), strlen(clog)));
       if (nlog)
       printf("NLOG\n----------\n%s\n-----------\n", base64_dec(((unsigned char *)nlog), strlen(nlog)));
       if (hlog)
       printf("HLOG\n----------\n%s\n-----------\n", base64_dec(((unsigned char *)hlog), strlen(hlog)));
       if (alog)
       printf("ALOG\n----------\n%s\n-----------\n", base64_dec(((unsigned char *)alog), strlen(alog)));
       }
       } else */
    if (!strcmp(argv[REQUEST_ARG], "getKeys")) {
        if ((rc = gl_getKeys(argv[SERVICE_ARG], &cccert, &nccert, env, stub)) == EUCA_OK) {
            if (cccert)
                printf("CCCERT\n----------\n%s\n-----------\n", base64_dec(((unsigned char *)cccert), strlen(cccert)));
            if (nccert)
                printf("NCCERT\n----------\n%s\n-----------\n", base64_dec(((unsigned char *)nccert), strlen(nccert)));
        }
    } else {
        printf("ERROR: Invalid argument %s\n", argv[REQUEST_ARG]);
        return (EUCA_INVALID_ERROR);
    }

    return (rc);

#undef APPNAME_ARG
#undef URI_ARG
#undef REQUEST_ARG
#undef SERVICE_ARG
#undef NB_ARG
}
