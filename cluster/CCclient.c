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
//! @file cluster/CCclient.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <time.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_axis.h>
#include <data.h>
#include <sensor.h>

#include <vnetwork.h>
#include "cc-client-marshal.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifndef MODE
#define MODE                                     0
#endif /* ! MODE */

#ifndef MAX_INSTANCES
#define MAX_INSTANCES                         1000  //!< Maximum number of instances supported by this client application
#endif /* ! MAX_INSTANCES */

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

#ifndef NO_COMP
const char *euca_this_component_name = "cc";
const char *euca_client_component_name = "clc";
#endif /* ! NO_COMP */

ncMetadata mymeta = { 0 };

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
//! @return EUCA_OK
//!
int main(int argc, char **argv)
{
    int rc = 0;
    int i = 0;
    int port = 0;
    int use_wssec = 0;
    int ccsLen = 0;
    int resSize = 0;
    int num = 0;
    int vlan = 0;
    char **ccs = NULL;
    char *nameserver = NULL;
    char *euca_home = NULL;
    char buf[40960] = { 0 };
    char configFile[1024] = { 0 };
    char policyFile[1024] = { 0 };
    char *instIds[256] = { 0 };
    char *amiId = NULL;
    char *amiURL = NULL;
    char *kernelId = NULL;
    char *kernelURL = NULL;
    char *ramdiskId = NULL;
    char *ramdiskURL = NULL;
    axutil_env_t *env = NULL;
    axis2_char_t *client_home = NULL;
    axis2_char_t endpoint_uri[256] = { 0 };
    axis2_char_t *tmpstr = NULL;
    axis2_stub_t *stub = NULL;
    virtualMachine params = { 64, 1, 64, "m1.small" };
    sensorResource **res;

    bzero(&mymeta, sizeof(ncMetadata));

    mymeta.userId = strdup("admin");
    mymeta.correlationId = strdup("1234abcd");
    mymeta.epoch = 3;
    mymeta.servicesLen = 16;
    snprintf(mymeta.services[15].name, 16, "eucalyptusname");
    snprintf(mymeta.services[15].type, 16, "eucalyptustype");
    snprintf(mymeta.services[15].partition, 16, "eucalyptuspart");
    mymeta.services[15].urisLen = 1;
    snprintf(mymeta.services[15].uris[0], 512, "http://192.168.254.3:8773/services/Eucalyptus");

    if (MODE == 0) {
        if (argc != 2 || strcmp(argv[1], "-9")) {
            printf("only runnable from inside euca\n");
            exit(1);
        }
    } else {
        if (argc < 3) {
            printf("USAGE: CCclient <host:port> <command> <opts>\n");
            exit(1);
        }
    }

    euca_home = getenv("EUCALYPTUS");
    if (!euca_home) {
        euca_home = "";
    }
    snprintf(configFile, 1024, EUCALYPTUS_CONF_LOCATION, euca_home);
    snprintf(policyFile, 1024, EUCALYPTUS_KEYS_DIR "/cc-client-policy.xml", euca_home);

    rc = get_conf_var(configFile, "CC_PORT", &tmpstr);
    if (rc != 1) {
        // error
        logprintf("ERROR: parsing config file (%s) for CC_PORT\n", configFile);
        exit(1);
    } else {
        port = atoi(tmpstr);
    }

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

    if (MODE == 0) {
        snprintf(endpoint_uri, 256, "http://localhost:%d/axis2/services/EucalyptusCC", port);
    } else {
        snprintf(endpoint_uri, 256, "http://%s/axis2/services/EucalyptusCC", argv[1]);
    }
    env = axutil_env_create_all("/tmp/fofo", AXIS2_LOG_LEVEL_TRACE);

    client_home = AXIS2_GETENV("AXIS2C_HOME");
    if (!client_home) {
        printf("must have AXIS2C_HOME set\n");
        exit(1);
    }
    stub = axis2_stub_create_EucalyptusCC(env, client_home, endpoint_uri);

    if (use_wssec) {
        rc = InitWSSEC(env, stub, policyFile);
        if (rc) {
            printf("cannot initialize WS-SEC policy (%s)\n", policyFile);
            exit(1);
        }
    }

    if (MODE == 0) {
        rc = cc_killallInstances(env, stub);
        if (rc != 0) {
            printf("cc_killallInstances() failed\n");
            exit(1);
        }
    } else {
        if (!strcmp(argv[2], "runInstances")) {
            if (argv[3])
                amiId = argv[3];
            if (argv[4])
                amiURL = argv[4];
            if (argv[5])
                kernelId = argv[5];
            if (argv[6])
                kernelURL = argv[6];
            if (argv[10])
                ramdiskId = argv[10];
            if (argv[11])
                ramdiskURL = argv[11];

            // Retrieve the number of instance and sanitize the value. Make sure its a positive number and less than a 1000
            num = atoi(argv[7]);
            if ((num < 0) || (num > MAX_INSTANCES)) {
                printf("cc_runInstances() failed: invalid instance count: num:%d\n", num);
                exit(1);
            }
            // retrieve the vlan value and sanitize it.
            vlan = atoi(argv[8]);
            if ((vlan < 1) || (vlan >= NUMBER_OF_VLANS)) {
                printf("cc_runInstances() failed: invalid instance count: vlan:%d\n", vlan);
                exit(1);
            }

            rc = cc_runInstances(amiId, amiURL, kernelId, kernelURL, ramdiskId, ramdiskURL, num, vlan, argv[9], &params, env, stub);
            if (rc != 0) {
                printf("cc_runInstances() failed: in:%s out:%d\n", argv[4], rc);
                exit(1);
            }
        } else if (!strcmp(argv[2], "describeInstances")) {
            rc = cc_describeInstances(NULL, 0, env, stub);
            if (rc != 0) {
                printf("cc_describeInstances() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "describeServices")) {
            rc = cc_describeServices(env, stub);
            if (rc != 0) {
                printf("cc_describeServices() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "startService")) {
            rc = cc_startService(env, stub);
            if (rc != 0) {
                printf("cc_startService() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "stopService")) {
            rc = cc_stopService(env, stub);
            if (rc != 0) {
                printf("cc_stopService() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "enableService")) {
            rc = cc_enableService(env, stub);
            if (rc != 0) {
                printf("cc_enableService() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "disableService")) {
            rc = cc_disableService(env, stub);
            if (rc != 0) {
                printf("cc_disableService() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "shutdownService")) {
            rc = cc_shutdownService(env, stub);
            if (rc != 0) {
                printf("cc_shutdownService() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "getConsoleOutput")) {
            rc = cc_getConsoleOutput(argv[3], env, stub);
            if (rc != 0) {
                printf("cc_getConsoleOutput() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "rebootInstances")) {
            if (argv[3] != NULL) {
                instIds[0] = strdup(argv[3]);
            }
            rc = cc_rebootInstances(instIds, 1, env, stub);
            if (rc != 0) {
                printf("cc_rebootInstances() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "terminateInstances")) {
            i = 3;
            while (argv[i] != NULL) {
                instIds[i - 3] = strdup(argv[i]);
                i++;
            }
            if ((i - 3) > 0) {
                rc = cc_terminateInstances(instIds, i - 3, env, stub);
                if (rc != 0) {
                    printf("cc_terminateInstances() failed\n");
                    exit(1);
                }
            }
        } else if (!strcmp(argv[2], "describeResources")) {
            rc = cc_describeResources(env, stub);
            if (rc != 0) {
                printf("cc_describeResources() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "startNetwork")) {
            ccs = EUCA_ALLOC(32, sizeof(char *));
            for (i = 0; i < 32; i++) {
                if (argv[i + 5]) {
                    ccs[i] = strdup(argv[i + 5]);
                    ccsLen++;
                } else {
                    i = 33;
                }
            }
            rc = cc_startNetwork(atoi(argv[3]), argv[4], ccs, ccsLen, env, stub);
            if (rc != 0) {
                printf("cc_startNetwork() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "describeNetworks")) {
            ccs = EUCA_ALLOC(32, sizeof(char *));
            for (i = 0; i < 32; i++) {
                if (argv[i + 3]) {
                    ccs[i] = strdup(argv[i + 3]);
                    ccsLen++;
                } else {
                    i = 33;
                }
            }
            nameserver = strdup("1.2.3.4");

            rc = cc_describeNetworks(nameserver, ccs, ccsLen, env, stub);
            if (rc != 0) {
                printf("cc_describeNetworks() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "configureNetwork")) {
            rc = cc_configureNetwork(argv[3], argv[4], argv[5], atoi(argv[6]), atoi(argv[7]), argv[8], env, stub);
            if (rc != 0) {
                printf("cc_configureNetwork() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "stopNetwork")) {
            rc = cc_stopNetwork(atoi(argv[3]), argv[4], env, stub);
            if (rc != 0) {
                printf("cc_stopNetwork() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "broadcastNetworkInfo")) {
            rc = cc_broadcastNetworkInfo(argv[3], env, stub);
            if (rc != 0) {
                printf("cc_broadcastNetworkInfo() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "assignAddress")) {
            rc = cc_assignAddress(argv[3], argv[4], env, stub);
            if (rc != 0) {
                printf("cc_assignAddress() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "unassignAddress")) {
            rc = cc_unassignAddress(argv[3], argv[4], env, stub);
            if (rc != 0) {
                printf("cc_unassignAddress() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "attachVolume")) {
            rc = cc_attachVolume(argv[3], argv[4], argv[5], argv[6], env, stub);
            if (rc != 0) {
                printf("cc_attachVolume() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "detachVolume")) {
            rc = cc_detachVolume(argv[3], argv[4], argv[5], argv[6], atoi(argv[7]), env, stub);
            if (rc != 0) {
                printf("cc_unassignNetwork() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "bundleInstance")) {
            rc = cc_bundleInstance(argv[3], argv[4], argv[5], argv[6], argv[7], env, stub);
            if (rc != 0) {
                printf("cc_bundleInstance() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "bundleRestartInstance")) {
            rc = cc_bundleRestartInstance(argv[3], env, stub);
            if (rc != 0) {
                printf("cc_bundleRestartInstance() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "createImage")) {
            rc = cc_createImage(argv[3], argv[4], argv[5], env, stub);
            if (rc != 0) {
                printf("cc_createImage() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "describePublicAddresses")) {
            rc = cc_describePublicAddresses(env, stub);
            if (rc != 0) {
                printf("cc_describePublicAddresses() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "killallInstances")) {
            rc = cc_killallInstances(env, stub);
            if (rc != 0) {
                printf("cc_killallInstances() failed\n");
                exit(1);
            }
        } else if (!strcmp(argv[2], "describeSensors")) {
            rc = cc_describeSensors(10, 5000, NULL, 0, NULL, 0, &res, &resSize, env, stub);
            if (rc != 0) {
                printf("cc_describeSensors() failed: error=%d\n", rc);
                exit(1);
            }
            sensor_res2str(buf, sizeof(buf), res, resSize);
            printf("resources: %d\n%s\n", resSize, buf);
        } else if (!strcmp(argv[2], "modifyNode")) {
            rc = cc_modifyNode(argv[3], argv[4], env, stub);
            if (rc != 0) {
                printf("cc_modifyNode() failed: error=%d\n", rc);
                exit(1);
            }
        } else if (!strcmp(argv[2], "migrateInstances")) {
            rc = cc_migrateInstances(argv[3], env, stub);
            if (rc != 0) {
                printf("cc_migrateInstances() failed: error=%d\n", rc);
                exit(1);
            }
        } else if (!strcmp(argv[2], "startInstance")) {
            rc = cc_startInstance(argv[3], env, stub);
            if (rc != 0) {
                printf("cc_migrateInstances() failed: error=%d\n", rc);
                exit(1);
            }
        } else if (!strcmp(argv[2], "stopInstance")) {
            rc = cc_stopInstance(argv[3], env, stub);
            if (rc != 0) {
                printf("cc_migrateInstances() failed: error=%d\n", rc);
                exit(1);
            }
        } else {
            printf("unrecognized operation '%s'\n", argv[2]);
            exit(1);
        }
    }

    return (EUCA_OK);
}
