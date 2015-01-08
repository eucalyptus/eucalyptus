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
 *     notice, this list of conditions and te following disclaimer.
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
//! @file node/test_nc.c
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

#include <libvirt/libvirt.h>
#include <libvirt/virterror.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <diskutil.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MAXDOMS                    1024 //!< Maximum number of domains

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

const char *euca_this_component_name = "nc";    //!< Eucalyptus Component Name

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

static void print_libvirt_error(void);
#ifdef UNUSED_CODE
static char *find_conf_value(const char *eucahome, const char *param);
#endif /* UNUSED_CODE */

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
//! Prints any libvirt error to console.
//!
static void print_libvirt_error(void)
{
    virError *verr = NULL;
    if ((verr = virGetLastError()) != NULL) {
        fprintf(stderr, "libvirt error: %s (code=%d)\n", verr->message, verr->code);
        virResetLastError();
    }
}

#ifdef UNUSED_CODE
//!
//! find value of the given param in the eucalyptus.conf,
//! return NULL if the param is commented out
//!
//! @param[in] eucahome the path where Eucalyptus is installed
//! @param[in] param the parameter value we're looking for
//!
//! @return the matching value or NULL
//!
static char *find_conf_value(const char *eucahome, const char *param)
{
    int i = 0;
    int j = 0;
    int quote = 0;
    char *pch = NULL;
    char conf_path[1024] = "";
    char line[1024] = "";
    char *value = NULL;
    FILE *f_conf = NULL;

    if (!eucahome || !param)
        return (NULL);

    snprintf(conf_path, 1024, EUCALYPTUS_CONF_LOCATION, eucahome);
    if ((f_conf = fopen(conf_path, "r")) == NULL) {
        return (NULL);
    }

    while (fgets(line, 1024, f_conf) != NULL) {
        if (strstr(line, param) != NULL) {  // found the param in the line
            if (strchr(line, '#') != NULL) {    // the line is commented out (assume # can't appear in the middle)
                break;
            } else {
                pch = strtok(line, "=");    // again assume '=' can't appear in the middle of value
                pch = strtok(NULL, "=");
                if (pch && (strlen(pch) > 0)) {
                    if ((value = EUCA_ZALLOC(strlen(pch) + 1, 1)) == NULL) {
                        fclose(f_conf);
                        return (NULL);
                    }
                    snprintf(value, strlen(pch) + 1, "%s", pch);
                }
                break;
            }
        }
        bzero(line, 1024);
    }

    // remove "" from the value
    if (value) {
        quote = 0;
        for (i = 0; i < strlen(value); i++) {
            if (value[i] == '\"')
                quote++;
            else
                value[i - quote] = value[i];
        }
        value[strlen(value) - quote] = 0x00;

        // remove spaces
        i = 0;
        while ((value[i] == ' ') || (value[i] == '\t'))
            i++;

        for (j = i; j < strlen(value); j++)
            value[j - i] = value[j];
        value[strlen(value) - i] = 0x00;

        if (value[strlen(value) - 1] == '\n')
            value[strlen(value) - 1] = 0x00;
    }

    fclose(f_conf);
    return (value);
}
#endif /* UNUSED_CODE */

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return Always return 0
//!
int main(int argc, char *argv[])
{
    int dom_ids[MAXDOMS] = { 0 };
    int num_doms = 0;
    char *eucahome = NULL;
    char *hypervisor = NULL;
    char rootWrap[EUCA_MAX_PATH] = "";
    char cmd[EUCA_MAX_PATH] = "";
    char hypervisorURL[32] = "";
    virConnectPtr conn = NULL;

    //  logfile (NULL, EUCAFATAL); // suppress all messages

    if (argc != 2) {
        fprintf(stderr, "error: test_nc expects one parameter (name of hypervisor)\n");
        exit(1);
    }

    hypervisor = argv[1];
    if (!strcmp(hypervisor, "kvm") || !strcmp(hypervisor, "qemu")) {
        snprintf(hypervisorURL, 32, "qemu:///system");
    } else if (!strcmp(hypervisor, "xen")) {
        snprintf(hypervisorURL, 32, "xen:///");
    } else if (!strcmp(hypervisor, "not_configured")) {
        fprintf(stderr, "error: HYPERVISOR variable is not set in eucalyptus.conf\n");
        exit(1);
    } else {
        fprintf(stderr, "error: hypervisor type (%s) is not recognized\n", hypervisor);
        exit(1);
    }

    // check that commands that NC needs are there
    if (euca_execlp(NULL, "perl", "--version", NULL) != EUCA_OK) {
        fprintf(stderr, "error: could not run perl\n");
        exit(1);
    }

    if ((eucahome = getenv(EUCALYPTUS_ENV_VAR_NAME)) == NULL) {
        eucahome = strdup("");         // root by default
    } else {
        eucahome = strdup(eucahome);
        // Sanitize this string
        if (euca_sanitize_path(eucahome) != EUCA_OK) {
            EUCA_FREE(eucahome);
            eucahome = strdup("");
        }
    }

    add_euca_to_path(eucahome);

    fprintf(stderr, "looking for system utilities...\n");
    if (diskutil_init(FALSE)) {
        // NC does not require GRUB for now
        EUCA_FREE(eucahome);
        exit(1);
    }
    // ensure hypervisor information is available
    fprintf(stderr, "ok\n\nchecking the hypervisor...\n");
    snprintf(rootWrap, sizeof(rootWrap), EUCALYPTUS_ROOTWRAP, eucahome);
    if (!strcmp(hypervisor, "kvm") || !strcmp(hypervisor, "qemu")) {
        snprintf(cmd, sizeof(cmd), EUCALYPTUS_HELPER_DIR "/get_sys_info", eucahome);
    } else {
        snprintf(cmd, sizeof(cmd), EUCALYPTUS_HELPER_DIR "/get_xen_info", eucahome);
    }

    if (euca_execlp(NULL, rootWrap, cmd, NULL) != EUCA_OK) {
        fprintf(stderr, "error: could not run '%s %s'\n", rootWrap, cmd);
        EUCA_FREE(eucahome);
        exit(1);
    }
    // check that libvirt can query the hypervisor
    // NULL means local hypervisor
    if ((conn = virConnectOpen(hypervisorURL)) == NULL) {
        print_libvirt_error();
        fprintf(stderr, "error: failed to connect to hypervisor\n");
        EUCA_FREE(eucahome);
        exit(1);
    }

    if ((num_doms = virConnectListDomains(conn, dom_ids, MAXDOMS)) < 0) {
        print_libvirt_error();
        fprintf(stderr, "error: failed to query running domains\n");
        EUCA_FREE(eucahome);
        exit(1);
    }

    fprintf(stdout, "NC test was successful\n");
    EUCA_FREE(eucahome);
    return (0);
}
