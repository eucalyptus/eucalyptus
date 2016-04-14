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
//! @file net/eucanetd.c
//! Implementation of the service management layer
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <pwd.h>
#include <dirent.h>
#include <errno.h>
#include <signal.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <log.h>
#include <hash.h>
#include <math.h>
#include <http.h>
#include <config.h>
#include <sequence_executor.h>
#include <atomic_file.h>
#include <euca_network.h>

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ipr_handler.h"
#include "ebt_handler.h"
#include "dev_handler.h"
#include "eucanetd_config.h"
#include "euca_gni.h"
#include "euca_lni.h"
#include "euca-to-mido.h"
#include "eucanetd.h"
#include "eucanetd_util.h"

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

//! Global EUCANETD configuration structure
eucanetdConfig *config = NULL;

//! Global Network Information structure pointer.
//globalNetworkInfo *globalnetworkinfo = NULL;
gni_hostname_info *host_info = NULL;

//! Role of the component running alongside this eucanetd service
eucanetd_peer eucanetdPeer = PEER_INVALID;

//! List of configuration keys that are handled when the application starts
configEntry configKeysRestartEUCANETD[] = {
    {"EUCALYPTUS", "/"}
    ,
    {"VNET_BRIDGE", NULL}
    ,
    {"VNET_BROADCAST", NULL}
    ,
    {"VNET_DHCPDAEMON", "/usr/sbin/dhcpd"}
    ,
    {"VNET_DHCPUSER", "root"}
    ,
    {"VNET_DNS", NULL}
    ,
    {"VNET_DOMAINNAME", "eucalyptus.internal"}
    ,
    {"VNET_MODE", NETMODE_MANAGED_NOVLAN}
    ,
    {"VNET_LOCALIP", NULL}
    ,
    {"VNET_NETMASK", NULL}
    ,
    {"VNET_PRIVINTERFACE", NULL}
    ,
    {"VNET_PUBINTERFACE", NULL}
    ,
    {"VNET_PUBLICIPS", NULL}
    ,
    {"VNET_PRIVATEIPS", NULL}
    ,
    {"VNET_ROUTER", NULL}
    ,
    {"VNET_SUBNET", NULL}
    ,
    {"VNET_MACPREFIX", "d0:0d"}
    ,
    {"VNET_ADDRSPERNET", "32"}
    ,
    {"DISABLE_TUNNELING", "Y"}
    ,
    {"EUCA_USER", "eucalyptus"}
    ,
    {"MIDOEUCANETDHOST", NULL}
    ,
    {"MIDOGWHOSTS", NULL}
    ,
    {"MIDOPUBNW", NULL}
    ,
    {"MIDOPUBGWIP", NULL}
    ,
    {NULL, NULL}
    ,
};

//! List of configuration keys that are periodically monitored for changes
configEntry configKeysNoRestartEUCANETD[] = {
    {"POLLING_FREQUENCY", "1"}
    ,
    {"DISABLE_L2_ISOLATION", "N"}
    ,
    {"NC_PROXY", "N"}
    ,
    {"NC_ROUTER", "Y"}
    ,
    {"NC_ROUTER_IP", ""}
    ,
    {"METADATA_USE_VM_PRIVATE", "N"}
    ,
    {"METADATA_IP", ""}
    ,
    {"LOGLEVEL", "INFO"}
    ,
    {"LOGROLLNUMBER", "10"}
    ,
    {"LOGMAXSIZE", "104857600"}
    ,
    {"LOGPREFIX", ""}
    ,
    {"LOGFACILITY", ""}
    ,
    {NULL, NULL}
    ,
};

//! String representation of the system role
const char *asPeerRoleName[] = {
    "INVALID",
    "CLC",
    "CC",
    "NC",
    "NON-EUCA-HOST",
    "OUT-OF-BOUND",
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Pointer to the proper Network Driver Interface (NDI)
static driver_handler *pDriverHandler = NULL;

//! Pointer to our Local Netowork Information (LNI) structure
static lni_t *pLni = NULL;

static globalNetworkInfo *pGni = NULL;
static globalNetworkInfo *pGniApplied = NULL;
static globalNetworkInfo *gni_a = NULL;
static globalNetworkInfo *gni_b = NULL;

//! Main loop termination condition
static volatile boolean gIsRunning = FALSE;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void eucanetd_sigterm_handler(int signal);
static void eucanetd_sighup_handler(int signal);
static void eucanetd_install_signal_handlers(void);

static int eucanetd_daemonize(void);
static int eucanetd_fetch_latest_local_config(void);
static int eucanetd_initialize(void);
static int eucanetd_initialize_network_drivers(eucanetdConfig * pConfig);
static int eucanetd_read_config_bootstrap(void);
static int eucanetd_read_config(globalNetworkInfo *pGni);
static int eucanetd_initialize_logs(void);
static int eucanetd_fetch_latest_network(boolean * update_globalnet);
static int eucanetd_fetch_latest_euca_network(boolean * update_globalnet);
static int eucanetd_read_latest_network(globalNetworkInfo *pGni, boolean * update_globalnet);
static int eucanetd_detect_peer(globalNetworkInfo * pGni);

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

#ifndef EUCANETD_UNIT_TEST
//!
//! EUCANETD application main entry point
//!
//! @param[in] argc the number of arguments passed on the command line
//! @param[in] argv the list of arguments passed on the command line
//!
//! @return 0 on success or 1 on failure
//!
int main(int argc, char **argv)
{
    u32 scrubResult = EUCANETD_RUN_NO_API;
    int rc = 0;
    int opt = 0;
    int firstrun = 1;
    int counter = 0;
    int epoch_updates = 0;
    int lni_populated = 0;
    int epoch_failed_updates = 0;
    int epoch_checks = 0;
    int check_peer_attempts = 300;
    time_t epoch_timer = 0;
    struct timeval tv = { 0 };
    struct timeval ttv = { 0 };
    
    boolean update_globalnet = FALSE;
    boolean update_globalnet_failed = FALSE;

    /*
       {
       char *corrid=NULL, meh[75];
       int i;
       for (i=0; i<10000; i++) {
       //            corrid = create_corrid("89bd527c-9a72-4373-95b5-87cc1300c74b::6f585b45-9a75-4dcd-9e9e-c75664c63029");
       snprintf(meh, 75, "89bd527c-9a72-4373-95b5-87cc1300c74b::6f585b45-9a75-4dcd-9e9e-c75664c63029");
       corrid = create_corrid(meh);
       //            corrid = create_corrid("::");
       //            corrid = create_corrid(NULL);
       printf("MEH: %s\n", SP(corrid));
       }
       exit(0);
       }
     */

    // initialize
    eucanetd_initialize();

    // parse commandline arguments
    config->flushmode = FLUSH_NONE;
    while ((opt = getopt(argc, argv, "dhFf")) != -1) {
        switch (opt) {
        case 'd':
            config->debug = EUCANETD_DEBUG_TRACE;
            break;
        case 'F':
            config->flushmode = FLUSH_ALL;
            config->debug = EUCANETD_DEBUG_INFO;
            break;
        case 'f':
            config->flushmode = FLUSH_DYNAMIC;
            config->debug = EUCANETD_DEBUG_INFO;
            break;
        case 'h':
        default:
            printf("USAGE: %s OPTIONS\n\t%-12s| debug - run eucanetd in foreground, all output to terminal\n\t%-12s| flush - clear all eucanetd artifacts and exit\n"
                   "\t%-12s| flush dynamic - clear only dynamic eucanetd artifacts and exit\n", argv[0], "-d", "-F", "-f");
            exit(1);
            break;
        }
    }

    // need just enough config to initialize things and set up logging subsystem
    rc = eucanetd_read_config_bootstrap();
    if (rc) {
        fprintf(stderr, "could not read enough config to bootstrap eucanetd, exiting\n");
        exit(1);
    }

    // daemonize this process!
    rc = eucanetd_daemonize();
    if (rc) {
        fprintf(stderr, "failed to eucanetd_daemonize eucanetd, exiting\n");
        exit(1);
    }

    LOGINFO("eucanetd started\n");

    // Install the signal handlers
    gIsRunning = TRUE;
    eucanetd_install_signal_handlers();

    gni_a = gni_init();
    gni_b = gni_init();
    pGni = gni_a;
    // spin here until we get the latest config from active CC
    LOGINFO("eucanetd: starting pre-flight checks\n");
    rc = 1;
    while (rc) {
        rc = eucanetd_read_config(pGni);
        if (rc) {
            LOGDEBUG("cannot complete pre-flight checks (ignore if local NC has not yet been registered), retrying\n");
            sleep(1);
        } else {
            // At this point we have read a valid global network information
            // Sanity check before entering eucanetd main loop
            if (pGni->nmCode != config->nmCode) {
                LOGWARN("Inconsistent network mode in GNI(%s) and eucalyptus.conf(%s)\n", pGni->sMode, config->netMode);
                rc = 1;
                sleep(1);
            }
            if (check_peer_attempts > 0) {
                eucanetdPeer = eucanetd_detect_peer(pGni);
                if (PEER_IS_NONE(eucanetdPeer)) {
                    // PEER_NONE should be only valid for VPCMIDO
                    if (!IS_NETMODE_VPCMIDO(pGni)) {
                        LOGTRACE("eucanetd in mode %s should have a CC or NC service peer - instead of PEER_NONE.\n", config->netMode);
                        rc = 1;
                        check_peer_attempts--;
                        sleep(3);
                    }
                } else if (!PEER_IS_VALID(eucanetdPeer)) {
                    LOGTRACE("cannot find which service peer (CC/NC) is running alongside eucanetd.\n");
                    rc = 1;
                    check_peer_attempts--;
                    sleep(3);
                }
            } else {
                LOGWARN("Unable to detect service peer during pre-flight checks.\n");
            }
            eucanetdPeer = PEER_INVALID;
        }
        if (rc && config->flushmode) {
            LOGFATAL("Unable to complete eucanetd pre-flight checks. Flush aborted.\n");
            exit(1);
        }
    }
    LOGINFO("eucanetd: pre-flight checks complete.\n");

    // Set to setup our local network view structure
    if ((!pLni) && (!IS_NETMODE_VPCMIDO(pGni))) {
        if ((pLni = lni_init(config->cmdprefix, config->sIptPreload)) == NULL) {
            LOGFATAL("out of memory: unable to allocate LNI\n");
            exit(1);
        }
    }

    // got all config, enter main loop
    while (gIsRunning) {
        eucanetd_timer(&ttv);
        counter++;

        // fetch all latest networking information from various sources
        rc = eucanetd_fetch_latest_network(&update_globalnet);
        if (rc) {
            LOGWARN("one or more fetches for latest network information was unsuccessful\n");
        }
        // first time we run, force an update and flush has necessary
        if (firstrun) {
            update_globalnet = TRUE;
            firstrun = 0;
        }
        // if the last update operations failed, regardless of new info, force an update
        if (update_globalnet_failed == TRUE) {
            LOGDEBUG("last update of network state failed, forcing a retry: update_globalnet_failed=%d\n", update_globalnet_failed);
            update_globalnet = TRUE;
        }
        update_globalnet_failed = FALSE;

        if (update_globalnet) {
            rc = eucanetd_read_latest_network(pGni, &update_globalnet);
        }
        if (rc) {
            LOGWARN("eucanetd_read_latest_network failed, skipping update: check above errors for details\n");
            // if the local read failed for some reason, skip any attempt to update (leave current state in place)
            update_globalnet = FALSE;
        }

        if (update_globalnet && (pGni->nmCode != config->nmCode)) {
            LOGWARN("Inconsistent network mode in GNI(%s) and eucalyptus.conf(%s). Skipping update.\n", pGni->sMode, config->netMode);
            update_globalnet = FALSE;
        }

        if (eucanetdPeer == PEER_INVALID) {
            eucanetdPeer = eucanetd_detect_peer(pGni);
            if (!PEER_IS_VALID(eucanetdPeer)) {
                LOGERROR("cannot find which service peer (CC/NC) is running alongside eucanetd.\n");
                update_globalnet = FALSE;
                update_globalnet_failed = TRUE;
                sleep(1);
                continue;
            }
            // Initialize our network driver
            rc = eucanetd_initialize_network_drivers(config);
            if (rc) {
                LOGFATAL("could not initialize network driver: check above log errors for details\n");
                exit(1);
            }
        }

        // Do we need to run the network upgrade stuff?
        if (pDriverHandler->upgrade) {
            if (pDriverHandler->upgrade(pGni) == 0) {
                // We no longer need to run it
                pDriverHandler->upgrade = NULL;
            } else {
                if (epoch_failed_updates >= 60) {
                    LOGERROR("could not complete network upgrade after 60 retries: check above log errors for details\n");
                } else {
                    LOGWARN("retry (%d): could not complete network upgrade: retrying\n", epoch_failed_updates);
                }
                update_globalnet_failed = TRUE;
            }
        }
        // Do we need to flush all eucalyptus networking artifacts
        if (config->flushmode) {
            eucanetd_timer(&tv);
            // Make sure we were given a flush API prior to calling it
            if (pDriverHandler->system_flush) {
                if (pDriverHandler->system_flush(pGni)) {
                    LOGERROR("manual flushing of all euca networking artifacts (iptables, ebtables, ipset) failed: check above log errors for details\n");
                }
            }
            LOGINFO("eucanetd flush executed in %ld ms.\n", eucanetd_timer(&tv));
            update_globalnet = FALSE;
            gIsRunning = FALSE;
            config->flushmode = FLUSH_NONE;
        }
        // if information on sec. group rules/membership has changed, apply
        if (update_globalnet) {
            eucanetd_timer_usec(&tv);
            LOGINFO("new networking state: updating system\n");

            // Are we able to load the LNI information - no need for lni in VPCMIDO
            if (!IS_NETMODE_VPCMIDO(pGni)) {
                lni_populated = lni_populate(pLni);
            }
            if (lni_populated == 0) {
                //
                // If we don't have a scrub API, just call all APIs. Any driver design must have this
                // API defined but for development purpose it make sense to sometimes bypass it.
                //
                if (!pDriverHandler->system_scrub) {
                    // Run ALL
                    scrubResult = EUCANETD_RUN_ALL_API;
                } else {
                    // Scrub the system so see what needs to be done
                    scrubResult = pDriverHandler->system_scrub(pGni, pGniApplied, pLni);
                    LOGINFO("eucanetd system_scrub executed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
                }
                
                // Make sure the scrub did not fail
                if (scrubResult != EUCANETD_RUN_ERROR_API) {
                    // update network artifacts (devices, tunnels, etc.) if the scrub indicate so
                    if (pDriverHandler->implement_network && (scrubResult & EUCANETD_RUN_NETWORK_API)) {
                        rc = pDriverHandler->implement_network(pGni, pLni);
                        if (rc) {
                            if (epoch_failed_updates >= 60) {
                                LOGERROR("could not complete VM network update after 60 retries: check above log errors for details\n");
                            } else {
                                LOGWARN("retry (%d): could not complete VM network update: retrying\n", epoch_failed_updates);
                            }
                            update_globalnet_failed = TRUE;
                        } else {
                            LOGINFO("eucanetd implement_network executed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
                        }
                    }
                    // update security groups, membership, etc. if the scrub indicate so
                    if (pDriverHandler->implement_sg && (scrubResult & EUCANETD_RUN_SECURITY_GROUP_API)) {
                        rc = pDriverHandler->implement_sg(pGni, pLni);
                        if (rc) {
                            LOGERROR("could not complete update of security groups: check above log errors for details\n");
                            update_globalnet_failed = TRUE;
                        } else {
                            LOGINFO("eucanetd implement_sg executed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
                        }
                    }
                    // update IP addressing, elastic IPs, etc. if the scrub indicate so
                    if (pDriverHandler->implement_addressing && (scrubResult & EUCANETD_RUN_ADDRESSING_API)) {
                        rc = pDriverHandler->implement_addressing(pGni, pLni);
                        if (rc) {
                            LOGERROR("could not complete VM addressing update: check above log errors for details\n");
                            update_globalnet_failed = TRUE;
                        } else {
                            LOGINFO("eucanetd implement_addressing executed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
                        }
                    }
                } else {
                    LOGERROR("could not complete VM network update: check above log errors for details\n");
                    update_globalnet_failed = TRUE;
                }
                // We're done with our local network view, reset it before the next populate
                if (!IS_NETMODE_VPCMIDO(pGni)) {
                    LNI_RESET(pLni);
                }
            } else {
                LOGERROR("Failed to populate our local network view. Check above logs for details.\n");
                update_globalnet_failed = TRUE;
            }
        }

        if (update_globalnet) {
            if (update_globalnet_failed == TRUE) {
                epoch_failed_updates++;
            } else {
                char versionFile[EUCA_MAX_PATH];
                // update was requested and was successful
                epoch_updates++;
                
                snprintf(versionFile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/global_network_info.version", config->eucahome);
                if (!strlen(pGni->version) || (str2file(pGni->version, versionFile, O_CREAT | O_TRUNC | O_WRONLY, 0644, FALSE) != EUCA_OK) ) {
                    LOGWARN("failed to populate GNI version file '%s': check permissions and disk capacity\n", versionFile);
                } else {
                    snprintf(config->lastAppliedVersion, 32, "%s", pGni->version);
                }
            }
        }
        epoch_checks++;

        if (epoch_timer >= 300) {
            LOGINFO("eucanetd report: tot_checks=%d tot_update_attempts=%d success_update_attempts=%d fail_update_attempts=%d duty_cycle_minutes=%f\n", epoch_checks,
                    epoch_updates + epoch_failed_updates, epoch_updates, epoch_failed_updates, (float)epoch_timer / 60.0);
            epoch_checks = epoch_updates = epoch_failed_updates = epoch_timer = 0;
        }

        if ((update_globalnet_failed == FALSE) && (update_globalnet == FALSE) && (gIsRunning == TRUE)) {
            if (pDriverHandler->system_maint) {
                rc = pDriverHandler->system_maint(pGni, pLni);
                if (rc != 0) {
                    LOGWARN("Failed to execute maintenance for %s.\n", pDriverHandler->name);
                }
            }
        }
        // do it all over again...
        if (update_globalnet_failed == TRUE) {
            LOGWARN("main loop complete (%ld ms): failures detected sleeping %d seconds before next poll\n", eucanetd_timer(&ttv), 1);
            pGniApplied = NULL;
            sleep(config->polling_frequency);
        } else {
            if (update_globalnet == FALSE) {
                LOGDEBUG("main loop complete (%ld ms): sleeping %d seconds before next poll\n", eucanetd_timer(&ttv), config->polling_frequency);
                sleep(config->polling_frequency);
            } else {
                pGniApplied = pGni;
                if (pGni == gni_a) {
                    pGni = gni_b;
                } else {
                    pGni = gni_a;
                }
                LOGINFO("main loop complete (%ld ms), applied GNI %s\n", eucanetd_timer(&ttv), config->lastAppliedVersion);
            }
        }

        epoch_timer += config->polling_frequency;        
        
    }

    LOGINFO("eucanetd going down.\n");

    if (pDriverHandler->cleanup) {
        LOGINFO("Cleaning up '%s' network driver on termination.\n", pDriverHandler->name);
        if (pDriverHandler->cleanup(pGni, config->flushmode) != 0) {
            LOGERROR("Failed to cleanup '%s' network driver.\n", pDriverHandler->name);
        }
    }

    gni_hostnames_free(host_info);
    GNI_FREE(gni_a);
    GNI_FREE(gni_b);
    LNI_FREE(pLni);

    LOGINFO("=== eucanetd down ===\n");
    exit(0);
}
#endif /* ! EUCANETD_UNIT_TEST */

//!
//! Handles the SIGTERM signal
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static void eucanetd_sigterm_handler(int signal)
{
    LOGINFO("eucanetd caught SIGTERM signal.\n");
    gIsRunning = FALSE;
}

//!
//! Handles the SIGHUP signal
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static void eucanetd_sighup_handler(int signal)
{
    LOGINFO("eucanetd caught a SIGHUP signal.\n");
    config->flushmode = FLUSH_NONE;
}

//!
//! Installs signal handlers for this application
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static void eucanetd_install_signal_handlers(void)
{
    struct sigaction act = { {0} };

    // Install the SIGTERM signal handler
    bzero(&act, sizeof(struct sigaction));
    act.sa_handler = &eucanetd_sigterm_handler;
    if (sigaction(SIGTERM, &act, NULL) < 0) {
        LOGERROR("Failed to install SIGTERM handler");
        exit(1);
    }
    // Install the SIGHUP signal handler
    bzero(&act, sizeof(struct sigaction));
    act.sa_handler = &eucanetd_sighup_handler;
    if (sigaction(SIGHUP, &act, NULL) < 0) {
        LOGERROR("Failed to install SIGTERM handler");
        exit(1);
    }
}

//!
//! Function description.
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
static int eucanetd_fetch_latest_local_config(void)
{
    if (isConfigModified(config->configFiles, NUM_EUCANETD_CONFIG) > 0) {
        // config modification time has changed
        if (readConfigFile(config->configFiles, NUM_EUCANETD_CONFIG)) {
            // something has changed that can be read in
            LOGINFO("configuration file has been modified, ingressing new options\n");
            eucanetd_initialize_logs();

            // TODO  pick up other NC options dynamically
        }
    }
    return (0);
}

//!
//! Daemonize switches user (drop priv), closes FDs, and back-grounds
//!
//! @return 0 or exits
//!
//! @see
//!
//! @pre
//!
//! @post On success, the process has been daemonized; STDIN, STDOUT and STDERR
//!       have been closed (non-debug); and the daemon has been setup properly.
//!       On failure, the process will exit.
//!
//! @note
//!
static int eucanetd_daemonize(void)
{
    int pid, sid;
    struct passwd *pwent = NULL;
    char pidfile[EUCA_MAX_PATH];
    FILE *FH = NULL;

    if (config->debug == EUCANETD_DEBUG_NONE) {
        pid = fork();
        if (pid) {
            exit(0);
        }

        sid = setsid();
        if (sid < 0) {
            perror("eucanetd_daemonize()");
            fprintf(stderr, "could not establish a new session id\n");
            exit(1);
        }
    }

    pid = getpid();
    if (pid > 1) {
        snprintf(pidfile, EUCA_MAX_PATH, "%s/var/run/eucalyptus/eucanetd.pid", config->eucahome);
        FH = fopen(pidfile, "w");
        if (FH) {
            fprintf(FH, "%d\n", pid);
            fclose(FH);
        } else {
            fprintf(stderr, "could not open pidfile for write (%s)\n", pidfile);
            exit(1);
        }
    }

    pwent = getpwnam(config->eucauser);
    if (!pwent) {
        fprintf(stderr, "could not find UID of configured user '%s'\n", SP(config->eucauser));
        exit(1);
    }

    if (setgid(pwent->pw_gid) || setuid(pwent->pw_uid)) {
        perror("setgid() setuid()");
        fprintf(stderr, "could not switch daemon process to UID/GID '%d/%d'\n", pwent->pw_uid, pwent->pw_gid);
        exit(1);
    }

    if (config->debug == EUCANETD_DEBUG_NONE) {
        close(STDIN_FILENO);
        close(STDOUT_FILENO);
        close(STDERR_FILENO);
    }

    return (0);
}

//!
//! Initialize eucanetd service
//!
//! @return 0 on success or exits with a value of 1 on failure
//!
//! @see
//!
//! @pre
//!
//! @post On success, eucanetd service has been initialized. On
//!       failure, the process will be terminated
//!
//! @note
//!
static int eucanetd_initialize(void)
{
    if (!config) {
        config = EUCA_ZALLOC(1, sizeof(eucanetdConfig));
        if (!config) {
            LOGFATAL("out of memory\n");
            exit(1);
        }
    }

    config->polling_frequency = 5;
    config->init = 1;

/*
    if (!globalnetworkinfo) {
        globalnetworkinfo = gni_init();
        if (!globalnetworkinfo) {
            LOGFATAL("out of memory\n");
            exit(1);
        }
    }
*/


    if (!host_info) {
        host_info = gni_init_hostname_info();
        if (!host_info) {
            LOGFATAL("out of memory\n");
            exit(1);
        }
    }
    return (0);
}

//!
//! Initialize the network drivers. When implementing a new network driver, simply set
//! the global 'pDriverHandler' variable to your new driver callback structure.
//!
//! @param[in] pConfig a pointer to the eucanetd configuration structure
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre The pConfig parameter must not be NULL
//!
//! @post On success, the proper driver handler is selected and the driver initialization
//!       routine has been called. On failure, the state of the driver is left undetermined.
//!
//! @note
//!
static int eucanetd_initialize_network_drivers(eucanetdConfig * pConfig)
{
    // Make sure our given parameter is valid
    if (pConfig) {
        LOGINFO("Loading '%s' mode driver.\n", pConfig->netMode);
        if (IS_NETMODE_EDGE(pConfig)) {
            pDriverHandler = &edgeDriverHandler;
        } else if (IS_NETMODE_VPCMIDO(pConfig)) {
            pDriverHandler = &midoVpcDriverHandler;
        } else if (IS_NETMODE_MANAGED(pConfig)) {
            pDriverHandler = &managedDriverHandler;
        } else if (IS_NETMODE_MANAGED_NOVLAN(pConfig)) {
            pDriverHandler = &managedNoVlanDriverHandler;
        } else {
            LOGERROR("Invalid network mode '%s' configured!\n", pConfig->netMode);
            return (1);
        }

        // If we have an init function. Lets call it now
        if (pDriverHandler->init) {
            if (pDriverHandler->init(pConfig) != 0) {
                LOGERROR("Failed to initialize '%s' driver!\n", pConfig->netMode);
                return (1);
            }
        }
        return (0);
    }
    return (1);
}

//!
//! Read and sets the environment parameters.
//!
//! @return 0 on success or the process will exit with a value of 1 on failure.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static int eucanetd_read_config_bootstrap(void)
{
    int ret = 0;
    char *eucaenv = getenv(EUCALYPTUS_ENV_VAR_NAME);
    char *eucauserenv = getenv(EUCALYPTUS_USER_ENV_VAR_NAME);
    char home[EUCA_MAX_PATH] = "";
    char user[EUCA_MAX_PATH] = "";
    char eucadir[EUCA_MAX_PATH] = "";
    char logfile[EUCA_MAX_PATH] = "";
    struct passwd *pwent = NULL;

    ret = 0;

    if (!eucaenv) {
        snprintf(home, EUCA_MAX_PATH, "/");
    } else {
        snprintf(home, EUCA_MAX_PATH, "%s", eucaenv);
    }

    if (!eucauserenv) {
        snprintf(user, EUCA_MAX_PATH, "eucalyptus");
    } else {
        snprintf(user, EUCA_MAX_PATH, "%s", eucauserenv);
    }

    snprintf(eucadir, EUCA_MAX_PATH, "%s/var/log/eucalyptus", home);
    if (check_directory(eucadir)) {
        fprintf(stderr, "cannot locate eucalyptus installation: make sure EUCALYPTUS env is set\n");
        exit(1);
    }

    config->eucahome = strdup(home);
    config->eucauser = strdup(user);
    snprintf(config->cmdprefix, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, config->eucahome);

    switch (config->debug) {
        case EUCANETD_DEBUG_NONE:
            snprintf(logfile, EUCA_MAX_PATH, "%s/var/log/eucalyptus/eucanetd.log", config->eucahome);
            log_file_set(logfile, NULL);
            log_params_set(EUCA_LOG_INFO, 0, 100000);

            pwent = getpwnam(config->eucauser);
            if (!pwent) {
                fprintf(stderr, "could not find UID of configured user '%s'\n", SP(config->eucauser));
                exit(1);
            }

            if (chown(logfile, pwent->pw_uid, pwent->pw_gid) < 0) {
                perror("chown()");
                fprintf(stderr, "could not set ownership of logfile to UID/GID '%d/%d'\n", pwent->pw_uid, pwent->pw_gid);
                exit(1);
            }
            break;
        case EUCANETD_DEBUG_TRACE:
            log_params_set(EUCA_LOG_TRACE, 0, 100000);
            break;
        case EUCANETD_DEBUG_DEBUG:
            log_params_set(EUCA_LOG_DEBUG, 0, 100000);
            break;
        case EUCANETD_DEBUG_INFO:
            log_params_set(EUCA_LOG_INFO, 0, 100000);
            break;
        default:
            log_params_set(EUCA_LOG_TRACE, 0, 100000);
    }

    return (ret);
}

//!
//! Reads the eucalyptus.conf configuration file and pull the important fields. It
//! also attempt to read the global network information XML and starts applying some
//! of these configuration to the system.
//!
//! @return 0 on success or 1 on failure. If any FATAL error occurs, the
//!         process fill exit with an exit code of 1.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static int eucanetd_read_config(globalNetworkInfo *pGni)
{
    int i = 0;
    int rc = 0;
    int ret = 0;
    char *tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME);
    char home[EUCA_MAX_PATH] = "";
    char netPath[EUCA_MAX_PATH] = "";
    char destfile[EUCA_MAX_PATH] = "";
    char sourceuri[EUCA_MAX_PATH] = "";
    char eucadir[EUCA_MAX_PATH] = "";
    char *cvals[EUCANETD_CVAL_LAST] = { NULL };
    boolean to_update = FALSE;

    LOGDEBUG("reading configuration\n");

    bzero(cvals, sizeof(char *) * EUCANETD_CVAL_LAST);

    for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
        EUCA_FREE(cvals[i]);
    }

    // set 'home' based on environment
    if (!tmpstr) {
        snprintf(home, EUCA_MAX_PATH, "/");
    } else {
        snprintf(home, EUCA_MAX_PATH, "%s", tmpstr);
    }

    snprintf(eucadir, EUCA_MAX_PATH, "%s/var/log/eucalyptus", home);
    if (check_directory(eucadir)) {
        LOGFATAL("cannot locate eucalyptus installation: make sure EUCALYPTUS env is set\n");
        exit(1);
    }

    snprintf(netPath, EUCA_MAX_PATH, CC_NET_PATH_DEFAULT, home);

    // search for the global state file from eucalyptue
    snprintf(sourceuri, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/global_network_info.xml", home);
    if (check_file(sourceuri)) {
        snprintf(sourceuri, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/cc_global_network_info.xml", home);
        if (check_file(sourceuri)) {
            snprintf(sourceuri, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/global_network_info.xml", home);
            if (check_file(sourceuri)) {
                LOGDEBUG("cannot find global_network_info.xml state file in $EUCALYPTUS/var/lib/eucalyptus or $EUCALYPTUS/var/run/eucalyptus yet.\n");
                return (1);
            } else {
                snprintf(sourceuri, EUCA_MAX_PATH, "file://" EUCALYPTUS_STATE_DIR "/global_network_info.xml", home);
                snprintf(destfile, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/eucanetd_global_network_info.xml", home);
                LOGDEBUG("found global_network_info.xml state file: setting source URI to '%s'\n", sourceuri);
            }
        } else {
            snprintf(sourceuri, EUCA_MAX_PATH, "file://" EUCALYPTUS_RUN_DIR "/cc_global_network_info.xml", home);
            snprintf(destfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/eucanetd_global_network_info.xml", home);
            LOGDEBUG("found global_network_info.xml state file: setting source URI to '%s'\n", sourceuri);
        }
    } else {
        snprintf(sourceuri, EUCA_MAX_PATH, "file://" EUCALYPTUS_RUN_DIR "/global_network_info.xml", home);
        snprintf(destfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/eucanetd_global_network_info.xml", home);
        LOGDEBUG("found global_network_info.xml state file: setting source URI to '%s'\n", sourceuri);
    }

    // initialize and populate data from global_network_info.xml file
    atomic_file_init(&(config->global_network_info_file), sourceuri, destfile, 0);

    rc = atomic_file_get(&(config->global_network_info_file), &to_update);
    if (rc) {
        LOGWARN("cannot get latest global network info file (%s)\n", config->global_network_info_file.dest);
        for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
            EUCA_FREE(cvals[i]);
        }
        return (1);
    }

    rc = gni_populate(pGni, host_info, config->global_network_info_file.dest);
    if (rc) {
        LOGDEBUG("could not initialize global network info data structures from XML input\n");
        for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
            EUCA_FREE(cvals[i]);
        }
        return (1);
    }
    rc = gni_print(pGni);
    rc = gni_hostnames_print(host_info);

    // setup and read local NC eucalyptus.conf file
    snprintf(config->configFiles[0], EUCA_MAX_PATH, EUCALYPTUS_CONF_LOCATION, home);
    configInitValues(configKeysRestartEUCANETD, configKeysNoRestartEUCANETD);   // initialize config subsystem
    readConfigFile(config->configFiles, 1);

    cvals[EUCANETD_CVAL_PUBINTERFACE] = configFileValue("VNET_PUBINTERFACE");
    cvals[EUCANETD_CVAL_PRIVINTERFACE] = configFileValue("VNET_PRIVINTERFACE");
    cvals[EUCANETD_CVAL_BRIDGE] = configFileValue("VNET_BRIDGE");
    cvals[EUCANETD_CVAL_EUCAHOME] = configFileValue("EUCALYPTUS");
    cvals[EUCANETD_CVAL_MODE] = configFileValue("VNET_MODE");
    cvals[EUCANETD_CVAL_EUCA_USER] = configFileValue("EUCA_USER");
    cvals[EUCANETD_CVAL_DHCPDAEMON] = configFileValue("VNET_DHCPDAEMON");
    cvals[EUCANETD_CVAL_DHCPUSER] = configFileValue("VNET_DHCPUSER");
    cvals[EUCANETD_CVAL_POLLING_FREQUENCY] = configFileValue("POLLING_FREQUENCY");
    cvals[EUCANETD_CVAL_DISABLE_L2_ISOLATION] = configFileValue("DISABLE_L2_ISOLATION");
    cvals[EUCANETD_CVAL_DISABLE_TUNNELING] = configFileValue("DISABLE_TUNNELING");
    cvals[EUCANETD_CVAL_NC_PROXY] = configFileValue("NC_PROXY");
    cvals[EUCANETD_CVAL_NC_ROUTER] = configFileValue("NC_ROUTER");
    cvals[EUCANETD_CVAL_NC_ROUTER_IP] = configFileValue("NC_ROUTER_IP");
    cvals[EUCANETD_CVAL_METADATA_USE_VM_PRIVATE] = configFileValue("METADATA_USE_VM_PRIVATE");
    cvals[EUCANETD_CVAL_METADATA_IP] = configFileValue("METADATA_IP");
    cvals[EUCANETD_CVAL_MIDOEUCANETDHOST] = configFileValue("MIDOEUCANETDHOST");
    cvals[EUCANETD_CVAL_MIDOGWHOSTS] = configFileValue("MIDOGWHOSTS");
    cvals[EUCANETD_CVAL_MIDOPUBNW] = configFileValue("MIDOPUBNW");
    cvals[EUCANETD_CVAL_MIDOPUBGWIP] = configFileValue("MIDOPUBGWIP");

    EUCA_FREE(config->eucahome);
    config->eucahome = strdup(cvals[EUCANETD_CVAL_EUCAHOME]);

    EUCA_FREE(config->eucauser);
    config->eucauser = strdup(cvals[EUCANETD_CVAL_EUCA_USER]);

    snprintf(config->cmdprefix, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, config->eucahome);
    config->polling_frequency = atoi(cvals[EUCANETD_CVAL_POLLING_FREQUENCY]);

    if (!cvals[EUCANETD_CVAL_MIDOEUCANETDHOST]) {
        cvals[EUCANETD_CVAL_MIDOEUCANETDHOST] = strdup(pGni->EucanetdHost);
    }

    if (!cvals[EUCANETD_CVAL_MIDOGWHOSTS]) {
        cvals[EUCANETD_CVAL_MIDOGWHOSTS] = strdup(pGni->GatewayHosts);
    }

    if (!cvals[EUCANETD_CVAL_MIDOPUBNW]) {
        cvals[EUCANETD_CVAL_MIDOPUBNW] = strdup(pGni->PublicNetworkCidr);
    }

    if (!cvals[EUCANETD_CVAL_MIDOPUBGWIP]) {
        cvals[EUCANETD_CVAL_MIDOPUBGWIP] = strdup(pGni->PublicGatewayIP);
    }

    if (!strcmp(cvals[EUCANETD_CVAL_DISABLE_L2_ISOLATION], "Y")) {
        config->disable_l2_isolation = 1;
    } else {
        config->disable_l2_isolation = 0;
    }

    if (!strcmp(cvals[EUCANETD_CVAL_METADATA_USE_VM_PRIVATE], "Y")) {
        config->metadata_use_vm_private = 1;
    } else {
        config->metadata_use_vm_private = 0;
    }

    if (!strcmp(cvals[EUCANETD_CVAL_DISABLE_TUNNELING], "Y")) {
        config->disableTunnel = TRUE;
    } else {
        config->disableTunnel = FALSE;
    }

    config->localIp = 0;
    if (cvals[EUCANETD_CVAL_LOCALIP]) {
        config->localIp = euca_dot2hex(cvals[EUCANETD_CVAL_LOCALIP]);
    }

    if (strlen(cvals[EUCANETD_CVAL_METADATA_IP])) {
        u32 test_ip, test_localhost;

        test_localhost = dot2hex("127.0.0.1");
        test_ip = dot2hex(cvals[EUCANETD_CVAL_METADATA_IP]);
        if (test_ip == test_localhost) {
            LOGERROR("value specified for METADATA_IP is not a valid IP, defaulting to CLC registered address\n");
            config->metadata_ip = 0;
        } else {
            config->clcMetadataIP = dot2hex(cvals[EUCANETD_CVAL_METADATA_IP]);
            config->metadata_ip = 1;
        }
    } else {
        config->metadata_ip = 0;
    }

    if (!strcmp(cvals[EUCANETD_CVAL_NC_PROXY], "Y")) {
        config->nc_proxy = TRUE;
    } else {
        config->nc_proxy = FALSE;
    }

    if (!strcmp(cvals[EUCANETD_CVAL_NC_ROUTER], "Y")) {
        config->nc_router = 1;
        if (strlen(cvals[EUCANETD_CVAL_NC_ROUTER_IP])) {
            u32 test_ip, test_localhost;
            test_localhost = dot2hex("127.0.0.1");
            test_ip = dot2hex(cvals[EUCANETD_CVAL_NC_ROUTER_IP]);
            if (strcmp(cvals[EUCANETD_CVAL_NC_ROUTER_IP], "AUTO") && (test_ip == test_localhost)) {
                LOGERROR("value specified for NC_ROUTER_IP is not a valid IP or the string 'AUTO': defaulting to 'AUTO'\n");
                snprintf(config->ncRouterIP, INET_ADDR_LEN, "AUTO");
            } else {
                snprintf(config->ncRouterIP, INET_ADDR_LEN, "%s", cvals[EUCANETD_CVAL_NC_ROUTER_IP]);
            }
            config->nc_router_ip = 1;
        } else {
            config->nc_router_ip = 0;
            config->vmGatewayIP = 0;
        }
    } else {
        config->nc_router = 0;
        config->nc_router_ip = 0;
        config->vmGatewayIP = 0;
    }

    snprintf(config->netMode, NETMODE_LEN, "%s", cvals[EUCANETD_CVAL_MODE]);
    config->nmCode = euca_netmode_atoi(config->netMode);
    snprintf(config->pubInterface, IF_NAME_LEN, "%s", cvals[EUCANETD_CVAL_PUBINTERFACE]);
    snprintf(config->privInterface, IF_NAME_LEN, "%s", cvals[EUCANETD_CVAL_PRIVINTERFACE]);
    snprintf(config->bridgeDev, IF_NAME_LEN, "%s", cvals[EUCANETD_CVAL_BRIDGE]);
    snprintf(config->dhcpDaemon, EUCA_MAX_PATH, "%s", cvals[EUCANETD_CVAL_DHCPDAEMON]);

    // mido config opts

    if (cvals[EUCANETD_CVAL_MIDOEUCANETDHOST])
        snprintf(config->midoeucanetdhost, sizeof(config->midoeucanetdhost), "%s", cvals[EUCANETD_CVAL_MIDOEUCANETDHOST]);
    if (cvals[EUCANETD_CVAL_MIDOGWHOSTS])
        snprintf(config->midogwhosts, sizeof(config->midogwhosts), "%s", cvals[EUCANETD_CVAL_MIDOGWHOSTS]);
    if (cvals[EUCANETD_CVAL_MIDOPUBNW])
        snprintf(config->midopubnw, sizeof(config->midopubnw), "%s", cvals[EUCANETD_CVAL_MIDOPUBNW]);
    if (cvals[EUCANETD_CVAL_MIDOPUBGWIP])
        snprintf(config->midopubgwip, sizeof(config->midopubgwip), "%s", cvals[EUCANETD_CVAL_MIDOPUBGWIP]);
    if (cvals[EUCANETD_CVAL_MIDOEUCANETDHOST])
        snprintf(config->midoeucanetdhost, sizeof(config->midoeucanetdhost), "%s", cvals[EUCANETD_CVAL_MIDOEUCANETDHOST]);

    if (strlen(cvals[EUCANETD_CVAL_DHCPUSER]) > 0)
        snprintf(config->dhcpUser, 32, "%s", cvals[EUCANETD_CVAL_DHCPUSER]);

    LOGDEBUG("required variables read from local config file: EUCALYPTUS=%s EUCA_USER=%s VNET_MODE=%s VNET_PUBINTERFACE=%s VNET_PRIVINTERFACE=%s VNET_BRIDGE=%s "
             "VNET_DHCPDAEMON=%s\n", SP(cvals[EUCANETD_CVAL_EUCAHOME]), SP(cvals[EUCANETD_CVAL_EUCA_USER]), SP(cvals[EUCANETD_CVAL_MODE]), SP(cvals[EUCANETD_CVAL_PUBINTERFACE]),
             SP(cvals[EUCANETD_CVAL_PRIVINTERFACE]), SP(cvals[EUCANETD_CVAL_BRIDGE]), SP(cvals[EUCANETD_CVAL_DHCPDAEMON]));

    rc = eucanetd_initialize_logs();
    if (rc) {
        LOGERROR("unable to initialize logging subsystem: check permissions and log config options\n");
        ret = 1;
    }

    config->ipt = EUCA_ZALLOC(1, sizeof(ipt_handler));
    if (!config->ipt) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }

    rc = ipt_handler_init(config->ipt, config->cmdprefix, NULL);
    if (rc) {
        LOGERROR("could not initialize ipt_handler: check above log errors for details\n");
        ret = 1;
    }

    config->ips = EUCA_ZALLOC(1, sizeof(ips_handler));
    if (!config->ips) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }

    rc = ips_handler_init(config->ips, config->cmdprefix);
    if (rc) {
        LOGERROR("could not initialize ips_handler: check above log errors for details\n");
        ret = 1;
    }

#ifdef USE_IP_ROUTE_HANDLER
    if ((config->ipr = EUCA_ZALLOC(1, sizeof(ipr_handler))) == NULL) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }

    if ((rc = ipr_handler_init(config->ipr, config->cmdprefix)) != 0) {
        LOGERROR("could not initialize ipr_handler: check above log errors for details\n");
        ret = 1;
    }
#endif /* USE_IP_ROUTE_HANDLER */

    config->ebt = EUCA_ZALLOC(1, sizeof(ebt_handler));
    if (!config->ebt) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }

    rc = ebt_handler_init(config->ebt, config->cmdprefix);
    if (rc) {
        LOGERROR("could not initialize ebt_handler: check above log errors for details\n");
        ret = 1;
    }

    //
    // If an error has occured we need to clean up temporary files
    // that were created for the iptables, ebtables, ipset
    // and possibly ipr (if compiled)
    //
    if (ret) {
        //
        // These config handlers could be NULL, unlink_handler_file method call will handle NULL filenames
        // We need to free the memory as read_config() will get called again until registered with the cloud.
        //
        if (config->ips) {
            unlink_handler_file(config->ips->ips_file);
            EUCA_FREE(config->ips);
        }
        if (config->ipt) {
            unlink_handler_file(config->ipt->ipt_file);
            EUCA_FREE(config->ipt);
        }
        if (config->ebt) {
            unlink_handler_file(config->ebt->ebt_filter_file);
            unlink_handler_file(config->ebt->ebt_nat_file);
            unlink_handler_file(config->ebt->ebt_asc_file);
            EUCA_FREE(config->ebt);
        }
#ifdef USE_IP_ROUTE_HANDLER
        if (config->ipr) {
            unlink_handler_file(config->ipr->sIpRuleFile);
            EUCA_FREE(config->ipr);
        }
#endif /* USE_IP_ROUTE_HANDLER */
    }

    for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
        EUCA_FREE(cvals[i]);
    }

    return (ret);
}

//!
//! Initialize the logging services
//!
//! @return Always returns 0
//!
//! @see log_file_set(), configReadLogParams(), log_params_set(), log_prefix_set()
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static int eucanetd_initialize_logs(void)
{
    int log_level = 0;
    int log_roll_number = 0;
    long log_max_size_bytes = 0;
    char *log_prefix = NULL;
    char logfile[EUCA_MAX_PATH] = "";

    switch (config->debug) {
        case EUCANETD_DEBUG_NONE:
            snprintf(logfile, EUCA_MAX_PATH, "%s/var/log/eucalyptus/eucanetd.log", config->eucahome);
            log_file_set(logfile, NULL);

            configReadLogParams(&log_level, &log_roll_number, &log_max_size_bytes, &log_prefix);

            log_params_set(log_level, log_roll_number, log_max_size_bytes);
            log_prefix_set(log_prefix);
            EUCA_FREE(log_prefix);
            break;
        case EUCANETD_DEBUG_TRACE:
            log_params_set(EUCA_LOG_TRACE, 0, 100000);
            break;
        case EUCANETD_DEBUG_DEBUG:
            log_params_set(EUCA_LOG_DEBUG, 0, 100000);
            break;
        case EUCANETD_DEBUG_INFO:
            log_params_set(EUCA_LOG_INFO, 0, 100000);
            break;
        default:
            log_params_set(EUCA_LOG_TRACE, 0, 100000);
            break;
    }

    return (0);
}

//!
//! Function description.
//!
//! @param[in] update_globalnet
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
static int eucanetd_fetch_latest_network(boolean * update_globalnet)
{
    int rc = 0, ret = 0;

    LOGDEBUG("fetching latest network view\n");

    if (!update_globalnet) {
        LOGERROR("BUG: input contains null pointers\n");
        return (1);
    }
    // don't run any updates unless something new has happened
    *update_globalnet = FALSE;

    rc = eucanetd_fetch_latest_local_config();
    if (rc) {
        LOGWARN("cannot read in changes to local configuration file: check local eucalyptus.conf\n");
    }
    // get latest networking data from eucalyptus, set update flags if content has changed
    rc = eucanetd_fetch_latest_euca_network(update_globalnet);
    if (rc) {
        LOGWARN("cannot get latest network topology, configuration and/or local VM network from CC/NC: check that CC and NC are running\n");
        ret = 1;
    }

    return (ret);
}

//!
//! Function description.
//!
//! @param[in] update_globalnet
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
static int eucanetd_fetch_latest_euca_network(boolean * update_globalnet)
{
    int rc = 0, ret = 0;

    rc = atomic_file_get(&(config->global_network_info_file), update_globalnet);
    if (rc) {
        LOGWARN("could not fetch latest global network info from NC\n");
        ret = 1;
    }

    return (ret);
}

//!
//! Function description.
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
static int eucanetd_read_latest_network(globalNetworkInfo *pGni, boolean *update_globalnet)
{
    int i = 0;
    int rc = 0;
    int ret = 0;
    int brdev_len = 0;
    u32 *brdev_ips = NULL;
    u32 *brdev_nms = NULL;
    char *strptra = NULL;
    char *strptrb = NULL;
    char *strptrc = NULL;
    char *strptrd = NULL;
    boolean found_ip = FALSE;
    gni_cluster *mycluster = NULL;

    LOGDEBUG("reading latest network view into eucanetd\n");
    
    if (!update_globalnet) {
        LOGWARN("Invalid argument: update_globalnet is null.\n");
        return (1);
    }
    rc = gni_populate(pGni, host_info, config->global_network_info_file.dest);
    if (rc) {
      LOGERROR("failed to initialize global network info data structures from XML file: check network config settings\n");
      ret = 1;
    } else {
      gni_print(pGni);
      //gni_hostnames_print(host_info);

      // regardless, if the last successfully applied version matches the current GNI version, skip the update
      if ((strlen(pGni->version) && strlen(config->lastAppliedVersion))) {
          if (!strcmp(pGni->version, config->lastAppliedVersion) ) {
              LOGINFO("global network version (%s) already applied, skipping update\n", pGni->version);
              *update_globalnet = FALSE;
          } else {
              LOGDEBUG("global network version (%s) does not match last successfully applied version (%s), continuing\n", pGni->version, config->lastAppliedVersion);
          }
      }

      if (IS_NETMODE_VPCMIDO(pGni)) {
	// skip for VPCMIDO
	ret = 0;
      } else {
	rc = gni_find_self_cluster(pGni, &mycluster);
	if (rc) {
	  LOGERROR("cannot retrieve cluster to which this NC belongs: check global network configuration\n");
	  ret = 1;
	} else {
	  if (!config->nc_router) {
	    // user has not specified NC router, use the default cluster private subnet gateway
	    config->vmGatewayIP = mycluster->private_subnet.gateway;
	    strptra = hex2dot(config->vmGatewayIP);
	    LOGDEBUG("using default cluster private subnet GW as VM default GW: %s\n", strptra);
	    EUCA_FREE(strptra);
	  } else {
	    // user has specified use of NC as router
	    if (!config->nc_router_ip) {
	      // user has not specified a router IP, use 'fake_router' mode                                              
	      config->vmGatewayIP = mycluster->private_subnet.gateway;
	      strptra = hex2dot(config->vmGatewayIP);
	      LOGDEBUG("using default cluster private subnet GW, with ARP spoofing, as VM default GW: %s\n", strptra);
	      EUCA_FREE(strptra);
	    } else if (config->nc_router_ip && strcmp(config->ncRouterIP, "AUTO")) {
	      // user has specified an explicit IP to use as NC router IP
	      config->vmGatewayIP = dot2hex(config->ncRouterIP);
	      LOGDEBUG("using user specified NC IP as VM default GW: %s\n", config->ncRouterIP);
	    } else if (config->nc_router_ip && !strcmp(config->ncRouterIP, "AUTO")) {
	      // user has specified 'AUTO', so detect the IP on the bridge Device that falls within this node's cluster's private subnet
	      rc = getdevinfo(config->bridgeDev, &brdev_ips, &brdev_nms, &brdev_len);
	      if (rc) {
		LOGERROR("cannot retrieve IP information from specified bridge device '%s': check your configuration\n", config->bridgeDev);
		ret = 1;
	      } else {
		LOGTRACE("specified bridgeDev '%s': found %d assigned IPs\n", config->bridgeDev, brdev_len);
		for (i = 0; i < brdev_len && !found_ip; i++) {
		  strptra = hex2dot(brdev_ips[i]);
		  strptrb = hex2dot(brdev_nms[i]);
		  if ((brdev_nms[i] == mycluster->private_subnet.netmask) && ((brdev_ips[i] & mycluster->private_subnet.netmask) == mycluster->private_subnet.subnet)) {
		    strptrc = hex2dot(mycluster->private_subnet.subnet);
		    strptrd = hex2dot(mycluster->private_subnet.netmask);
		    LOGTRACE("auto-detected IP '%s' on specified bridge interface '%s' that matches cluster's specified subnet '%s/%s'\n", strptra, config->bridgeDev, strptrc, strptrd);
		    config->vmGatewayIP = brdev_ips[i];
		    LOGDEBUG("using auto-detected NC IP as VM default GW: %s\n", strptra);
		    found_ip = TRUE;
		    EUCA_FREE(strptrc);
		    EUCA_FREE(strptrd);
		  }
		  EUCA_FREE(strptra);
		  EUCA_FREE(strptrb);
		}
		if (!found_ip) {
		  strptra = hex2dot(mycluster->private_subnet.subnet);
		  strptrb = hex2dot(mycluster->private_subnet.netmask);
		  LOGERROR
		    ("cannot find an IP assigned to specified bridge device '%s' that falls within this cluster's specified subnet '%s/%s': check your configuration\n",
		     config->bridgeDev, strptra, strptrb);
		  EUCA_FREE(strptra);
		  EUCA_FREE(strptrb);
		  ret = 1;
		}
	      }
	      EUCA_FREE(brdev_ips);
	      EUCA_FREE(brdev_nms);
	    }
	  }
	}
      }
    }
    return (ret);
}

//!
//! Checks whether we are running alongside a CC or NC service
//!
//! @param[in] pGni a pointer to our global network information structure
//!
//! @return Returns the proper role associated with this service
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static int eucanetd_detect_peer(globalNetworkInfo * pGni)
{
    gni_node *pNode = NULL;
    gni_cluster *pCluster = NULL;

    // Make sure our given pointer isn't NULL
    if (pGni == NULL)
        return (PEER_INVALID);

    // Can we find ourselves as a node in the GNI. This check needs to happen first.
    if (gni_find_self_node(pGni, &pNode) == 0) {
        LOGINFO("eucanetd running on %s component.\n", PEER2STR(PEER_NC));
        return (PEER_NC);
    }
    // Can we find ourselves as a cluster in the GNI
    if (gni_find_self_cluster(pGni, &pCluster) == 0) {
        LOGINFO("eucanetd running on %s component.\n", PEER2STR(PEER_CC));
        return (PEER_CC);
    }

    return (PEER_NONE);
}

/**
 * Invokes calloc() and perform error checking.
 * @param nmemb [in] see calloc() man pages.
 * @param size [in] see calloc() man pages.
 * @return pointer to the allocated memory.
 */
void *zalloc_check(size_t nmemb, size_t size) {
    void *ret = calloc(nmemb, size);
    if (ret == NULL) {
        LOGFATAL("out of memory - check calling function with log level DEBUG\n");
        LOGFATAL("Shutting down eucanetd.\n");
        exit(1);
    }
    return (ret);
}

/**
 * Invokes realloc() and perform error checking.
 * @param nmemb [in] see calloc() man pages.
 * @param size [in] see calloc() man pages.
 * @return pointer to the allocated memory.
 */
void *realloc_check(void *ptr, size_t nmemb, size_t size) {
    void *ret = realloc(ptr, nmemb * size);
    if (ret == NULL) {
        LOGFATAL("out of memory - check calling function with log level DEBUG\n");
        LOGFATAL("Shutting down eucanetd.\n");
        exit(1);
    }
    return (ret);
}

/**
 * Appends pointer ptr to the end of the given pointer array arr. The array should
 * have been malloc'd. The allocation is adjusted as needed.
 * @param arr [i/o] arr pointer to an array of pointers
 * @param max_arr [i/o] max_arr the number of array entries.
 * @param ptr (in] pointer to be appended to the array.
 * @return 0 on success. 1 otherwise.
 */
void *append_ptrarr(void *arr, int *max_arr, void *ptr) {
    arr = EUCA_REALLOC(arr, *max_arr + 1, sizeof (void *));
    if (arr == NULL) {
        LOGFATAL("out of memory: failed to (re)allocate array of pointers\n");
        LOGFATAL("Shutting down eucanetd.\n");
        exit (1);
    }
    void **parr = arr;
    parr[*max_arr] = ptr;
    (*max_arr)++;
    return (arr);    
}

