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
//! @file storage/SCclient.c
//! C-client for Storage Controller to test/dev operations from the NC to SC.
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <unistd.h>                    /* getopt */

#include <eucalyptus.h>
#include <misc.h>
//#include <data.h>
#include <euca_string.h>
#include <euca_axis.h>

#include "storage-controller.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define SC_ENDPOINT                 "/services/Storage"
#define DEFAULT_SC_HOSTPORT         "localhost:8773"
#define DEFAULT_MAC_ADDR            "aa:bb:cc:dd:ee:ff"
#define DEFAULT_PUBLIC_IP           "10.1.2.3"
#define BUFSIZE                     1024

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

boolean debug = FALSE;                 //!< Enables debug mode if set to TRUE

#ifndef NO_COMP
const char *euca_this_component_name = "sc";    //!< Eucalyptus Component Name
const char *euca_client_component_name = "nc";  //!< The client component name
#endif /* ! NO_COMP */

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

#define CHECK_PARAM(par, name) if (par==NULL) { fprintf (stderr, "ERROR: no %s specified (try -h)\n", name); exit (1); }

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Prints the command help to stderr
//!
void usage(void)
{
    fprintf(stderr, "usage: SCclient [command] [options]\n"
            "\tcommands:\t\t\trequired options:\n"
            "\t\tExportVolume\t\t[-t -v -i -p]\n"
            "\t\tUnexportVolume\t\t[-t -v -i -p]\n"
            "\toptions:\n"
            "\t\t-d \t\t- print debug output\n"
            "\t\t-l \t\t- local invocation => do not use WSSEC\n"
            "\t\t-h \t\t- this help information\n"
            "\t\t-s [host:port] \t- SC endpoint\n" "\t\t-i [str] \t- IQN to allow\n" "\t\t-p [str] \t- IP to allow\n" "\t\t-V [str] \t- volume ID\n" "\t\t-t [str] \t- token\n");
    exit(1);
}

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return Always return 0 or exit(1) on failure
//!
int main(int argc, char **argv)
{
    char *sc_hostport = DEFAULT_SC_HOSTPORT;
    char *sc_endpoint = SC_ENDPOINT;
    char *volume_id = NULL;
    char *token = NULL;
    char *command = NULL;
    char *ip = NULL;
    char *iqn = NULL;
    int local = 0;
    int ch = 0;
    int rc = 0;

    while ((ch = getopt(argc, argv, "lhdn:d:l:h:s:i:p:V:t:")) != -1) {
        switch (ch) {
        case 'd':
            debug = 1;
            break;
        case 'l':
            local = 1;
            break;
        case 's':
            sc_hostport = optarg;
            break;
        case 'V':
            volume_id = optarg;
            break;
        case 't':
            token = optarg;
            break;
        case 'p':
            ip = optarg;
            break;
        case 'i':
            iqn = optarg;
            break;
        case 'h':
            usage();                   // will exit
            break;
        case '?':
        default:
            fprintf(stderr, "ERROR: unknown parameter (try -h)\n");
            exit(1);
        }
    }
    argc -= optind;
    argv += optind;

    if (argc > 0) {
        command = argv[0];
        if (argc > 1) {
            fprintf(stderr, "WARNING: too many parameters, using first one as command\n");
        }
    } else {
        fprintf(stderr, "ERROR: command not specified (try -h)\n");
        exit(1);
    }

    char configFile[1024], policyFile[1024];
    char *euca_home;
    int use_wssec;
    char *tmpstr;
    char *correlationId = "correlate-me-please";
    char *userId = "eucalyptus";

    euca_home = getenv("EUCALYPTUS");
    if (!euca_home) {
        euca_home = "";
    }
    snprintf(configFile, 1024, EUCALYPTUS_CONF_LOCATION, euca_home);
    snprintf(policyFile, 1024, EUCALYPTUS_KEYS_DIR "/sc-client-policy.xml", euca_home);
    rc = get_conf_var(configFile, "ENABLE_WS_SECURITY", &tmpstr);
    if (rc != 1) {
        /* Default to enabled */
        use_wssec = 1;
    } else {
        if (!strcmp(tmpstr, "Y")) {
            use_wssec = 1;
        } else {
            use_wssec = 0;
        }
    }

    char sc_url[BUFSIZE];
    snprintf(sc_url, BUFSIZE, "http://%s%s", sc_hostport, sc_endpoint);
    if (debug)
        printf("connecting to SC at %s\n", sc_url);

    if (use_wssec && !local) {
        if (debug)
            printf("using policy file %s\n", policyFile);
    }

    /***********************************************************/
    if (!strcmp(command, "ExportVolume")) {
        CHECK_PARAM(volume_id, "volume ID");
        CHECK_PARAM(token, "token");
        CHECK_PARAM(ip, "ip");
        CHECK_PARAM(iqn, "iqn");
        char *connection_string = NULL;

        rc = scClientCall(correlationId, userId, use_wssec, policyFile, DEFAULT_SC_REQUEST_TIMEOUT, sc_url, "ExportVolume", volume_id, token, ip, iqn, &connection_string);
        if (rc != 0) {
            printf("scExportVolume() failed: error=%d\n", rc);
            exit(1);
        } else {
            printf("Got response: %s\n", connection_string);
        }

        /***********************************************************/
    } else if (!strcmp(command, "UnexportVolume")) {
        CHECK_PARAM(volume_id, "volume ID");
        CHECK_PARAM(token, "token");
        CHECK_PARAM(ip, "ip");
        CHECK_PARAM(iqn, "iqn");

        rc = scClientCall(correlationId, userId, use_wssec, policyFile, DEFAULT_SC_REQUEST_TIMEOUT, sc_url, "ExportVolume", volume_id, token, ip, iqn);
        if (rc != 0) {
            printf("scUnexportVolume() failed: error=%d\n", rc);
            exit(1);
        } else {
            printf("Got response: %d\n", rc);
        }

        /***********************************************************/
    } else {
        fprintf(stderr, "ERROR: command %s unknown (try -h)\n", command);
        exit(1);
    }

    if (local) {
        pthread_exit(NULL);
    } else {
        _exit(0);
    }
}
