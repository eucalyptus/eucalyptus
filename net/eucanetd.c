// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
#include <sys/socket.h>
#include <netinet/in.h>
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
#include "ebt_handler.h"
#include "dev_handler.h"
#include "euca_gni.h"
#include "euca-to-mido.h"
#include "eucanetd.h"
#include "eucanetd_util.h"
#include "eucalyptus-config.h"

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
    {"VNET_SYSTEMCTL", "/usr/bin/systemctl"}
    ,
    {"VNET_DNS", NULL}
    ,
    {"VNET_DOMAINNAME", "eucalyptus.internal"}
    ,
    {"VNET_MODE", NETMODE_EDGE}
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
    {"VNET_SUBNET", NULL}
    ,
    {"VNET_MACPREFIX", "d0:0d"}
    ,
    {"VNET_ADDRSPERNET", "32"}
    ,
    {"EUCA_USER", "eucalyptus"}
    ,
    {"MIDO_INTRT_CIDR", "169.254.0.0/17"}
    ,
    {"MIDO_INTMD_CIDR", "169.254.128.0/17"}
    ,
    {"MIDO_EXTMD_CIDR", "169.254.169.248/29"}
    ,
    {"MIDO_MD_CIDR", "255.0.0.0/8"}
    ,
    {"MIDO_MD_VETH_USE_NETNS", "N"}
    ,
    {"MIDO_MAX_RTID", "10240"}
    ,
    {"MIDO_MAX_ENIID", "1048576"}
    ,
    {"MIDO_VALIDATE_MIDOCONFIG", "Y"}
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
    {"MIDO_ENABLE_ARPTABLE", "Y"}
    ,
    {"MIDO_ENABLE_MIDOMD", "Y"}
    ,
    {"MIDO_MD_254_EGRESS", "tcp:80"}
    ,
    {"MIDO_MD_253_EGRESS", "udp:53 tcp:53 udp:5353 tcp:5353"}
    ,
    {"MIDO_API_URIBASE", ""}
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

int sig_rcvd = 0;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Global EUCANETD configuration structure
static eucanetdConfig *config = NULL;

//! Pointer to the proper Network Driver Interface (NDI)
static driver_handler *pDriverHandler = NULL;

static globalNetworkInfo *pGni = NULL;
static globalNetworkInfo *pGniApplied = NULL;
static globalNetworkInfo *gni_a = NULL;
static globalNetworkInfo *gni_b = NULL;

//! Main loop termination condition
static volatile boolean gIsRunning = FALSE;

//! USR1 and USR2 signals
static volatile boolean gUsr1Caught = FALSE;
static volatile boolean gUsr2Caught = FALSE;
static volatile boolean gHupCaught = FALSE;
static volatile boolean gTermCaught = FALSE;

//! Dummy UDP socket
int eucanetd_dummysock = 0;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void eucanetd_sigterm_handler(int signal);
static void eucanetd_sighup_handler(int signal);
static void eucanetd_sigusr1_handler(int signal);
static void eucanetd_sigusr2_handler(int signal);
static void eucanetd_install_signal_handlers(void);

static int eucanetd_daemonize(void);
static int eucanetd_fetch_latest_local_config(void);
static int eucanetd_initialize(void);
static int eucanetd_initialize_network_drivers(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static int eucanetd_cleanup(void);
static int eucanetd_read_config_bootstrap(void);
static int eucanetd_setlog_bootstrap(void);
static int eucanetd_read_config(globalNetworkInfo *pGni);
static int eucanetd_initialize_logs(void);
static int eucanetd_fetch_latest_network(boolean *update_globalnet, boolean *config_changed);
static int eucanetd_fetch_latest_euca_network(boolean *update_globalnet);
static int eucanetd_read_latest_network(globalNetworkInfo *pGni, boolean *update_globalnet);
static int eucanetd_detect_peer(globalNetworkInfo *pGni);

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
/**
 * EUCANETD application main entry point
 *
 * @param argc [in] the number of arguments passed on the command line
 * @param argv [in] the list of arguments passed on the command line
 *
 * @return 0 on success or 1 on failure
 */
int main(int argc, char **argv) {
    u32 scrubResult = EUCANETD_RUN_NO_API;
    int rc = 0;
    int opt = 0;
    int firstrun = 1;
    int counter = 0;
    int epoch_updates = 0;
    int epoch_failed_updates = 0;
    int epoch_checks = 0;
    time_t epoch_timer = 0;
    struct timeval tv = { 0 };
    struct timeval ttv = { 0 };
 
    boolean config_changed = FALSE;
    boolean update_globalnet = FALSE;
    boolean update_globalnet_failed = FALSE;
    boolean update_version_file = FALSE;

    // initialize
    eucanetd_initialize();

    // parse commandline arguments
    config->flushmode = FLUSH_NONE;
    while ((opt = getopt(argc, argv, "dhHlgfFmMsuUCZT:v:V:z:")) != -1) {
        switch (opt) {
        case 'd':
            config->debug = EUCANETD_DEBUG_TRACE;
            break;
        case 'l':
            config->flushmode = FLUSH_MIDO_LISTVPC;
            config->debug = EUCANETD_DEBUG_INFO;
            config->multieucanetd_safe = TRUE;
            break;
        case 'g':
            config->flushmode = FLUSH_MIDO_LISTGATEWAYS;
            config->debug = EUCANETD_DEBUG_INFO;
            config->multieucanetd_safe = TRUE;
            break;
        case 'F':
            config->flushmode = FLUSH_ALL;
            config->debug = EUCANETD_DEBUG_INFO;
            break;
        case 'f':
            config->flushmode = FLUSH_DYNAMIC;
            config->debug = EUCANETD_DEBUG_INFO;
            break;
        case 'C':
            config->flushmode = FLUSH_MIDO_DYNAMIC;
            config->debug = EUCANETD_DEBUG_INFO;
            break;
        case 'Z':
            config->flushmode = FLUSH_MIDO_ALL;
            config->debug = EUCANETD_DEBUG_INFO;
            break;
        case 'm':
            config->flushmode = FLUSH_MIDO_CHECKDUPS;
            config->debug = EUCANETD_DEBUG_INFO;
            config->multieucanetd_safe = TRUE;
            break;
        case 'M':
            config->flushmode = FLUSH_MIDO_DUPS;
            config->debug = EUCANETD_DEBUG_INFO;
            break;
        case 's':
            config->systemd = TRUE;
            break;
        case 'u':
            config->flushmode = FLUSH_MIDO_CHECKUNCONNECTED;
            config->debug = EUCANETD_DEBUG_INFO;
            config->multieucanetd_safe = TRUE;
            break;
        case 'U':
            config->flushmode = FLUSH_MIDO_UNCONNECTED;
            config->debug = EUCANETD_DEBUG_INFO;
            break;
        case 'v':
            config->flushmode = FLUSH_MIDO_CHECKVPC;
            config->debug = EUCANETD_DEBUG_INFO;
            config->flushmodearg = optarg;
            config->multieucanetd_safe = TRUE;
            break;
        case 'V':
            config->flushmode = FLUSH_MIDO_VPC;
            config->debug = EUCANETD_DEBUG_INFO;
            config->flushmodearg = optarg;
            break;
        case 'T':
            config->flushmode = FLUSH_MIDO_TZONE;
            config->debug = EUCANETD_DEBUG_INFO;
            config->flushmodearg = optarg;
            break;
        case 'z':
            config->flushmode = FLUSH_MIDO_TEST;
            config->debug = EUCANETD_DEBUG_INFO;
            config->flushmodearg = optarg;
            break;
        case 'H':
            printf("EXPERIMENTAL OPTIONS (USE AT YOUR OWN RISK)\n"
                    "\t%-12s| list VPCMIDO objects\n"
                    "\t%-12s| list VPCMIDO gateways (bgp status)\n"
                    "\t%-12s| detect duplicate objects in MidoNet\n"
                    "\t%-12s| detect and flush duplicate objects in MidoNet\n"
                    "\t%-12s| detect unconnected objects in MidoNet\n"
                    "\t%-12s| detect and flush unconnected objects in MidoNet\n"
                    "\t%-12s| check a VPC model (i-x | eni-x | vpc-x | subnet-x | nat-x | sg-x)\n"
                    "\t%-12s| flush a VPC model (i-x | eni-x | vpc-x | subnet-x | nat-x | sg-x)\n"
                    "\t%-12s| create tunnel-zone using IP on (dev)\n"
                    "\t\t\tbefore using -T make sure that midolman is running on all hosts\n"
                    "\t\t\tall hosts are assumed to have device (dev) with 1 IP address\n"
                    "\t\tlowercase options are read-only, and work with eucanetd service running\n"
                    "\t\tuppercase options can only be executed with eucanetd service stopped\n"
                     , "-l", "-g", "-m", "-M", "-u", "-U", "-v (id)", "-V (id)", "-T (dev)");
            exit (1);
            break;
        case 'h':
        default:
            printf("Eucalyptus version %s - eucanetd\n"
                    "USAGE: %s OPTIONS\n"
                    "\t%-12s| debug - run eucanetd in foreground, all output to terminal\n"
                    "\t%-12s| flush all EDGE eucanetd artifacts\n"
                    "\t%-12s| flush only dynamic EDGE eucanetd artifacts\n"
                    "\t%-12s| flush all but core objects that implement VPC models\n"
                    "\t%-12s| flush all objects (including core) that implement VPC models\n"
                    "\t\tNote: depending on the size of your cloud, flushing objects\n"
                    "\t\tfrom MidoNet and subsequently reconstructing them can take\n"
                    "\t\ta long time. For example, flushing 4,000 instances (or ENIs)\n"
                    "\t\tcan take over 1 hour (and another hour to reconstruct)\n"
                    , EUCA_VERSION, argv[0], "-d", "-F", "-f", "-C", "-Z");
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

    if (!config->multieucanetd_safe) {
        rc = eucanetd_dummy_udpsock();
        if (rc == -1) {
            LOGERROR("Cannot start eucanetd: another eucanetd might be running\n");
            exit(1);
        }
    }

    if (!config->systemd) {
        // daemonize this process!
        rc = eucanetd_daemonize();
        if (rc) {
            fprintf(stderr, "failed to eucanetd_daemonize eucanetd, exiting\n");
            exit(1);
        }
    }

    eucanetd_setlog_bootstrap();

    LOGINFO("eucanetd (%s) started\n", EUCA_VERSION);

    // Install the signal handlers
    gIsRunning = TRUE;
    eucanetd_install_signal_handlers();

    gni_a = gni_init();
    gni_b = gni_init();
    pGni = gni_a;
    // spin here until we get the latest config
    LOGINFO("eucanetd: starting pre-flight checks\n");
    rc = 1;
    boolean config_mode_ok = FALSE;
    boolean config_peer_ok = FALSE;
    for (int i = 0; rc; i++) {
        if (config->debug != EUCANETD_DEBUG_NONE) {
            // Temporarily disable verbose debug messages (it will be set by eucanetd_read_config)
            log_params_set(EUCA_LOG_WARN, 0, 100000);
        }
        rc = eucanetd_read_config(pGni);
        if (rc) {
            if (i % 100 == 0) {
                LOGINFO("eucanetd: waiting for a valid GNI and/or basic configuration\n");
            } else {
                LOGTRACE("Failed to perform basic eucanetd configuration, will retry in 1 sec\n");
            }
        } else {
            // At this point we have read a valid global network information
            // Sanity check before entering eucanetd main loop
            if (config->nmCode == NM_INVALID) {
                if (i % 30 == 0) {
                    LOGWARN("Invalid network mode detected. Waiting for a valid mode in GNI\n");
                }
                rc = 1;
                sleep(1);
                continue;
            } else {
                if (!config_mode_ok) {
                    LOGINFO("\tconfiguring eucanetd in %s mode\n", config->netMode);
                    config_mode_ok = TRUE;
                }
            }

            //
            // Verify that the kernel configuration parameters are enabled
            // only need to check in EDGE mode
            //
            if (IS_NETMODE_EDGE(pGni)) {
                if (!sysctl_enabled(SYSCTL_IP_FORWARD)) {
                    if ((i % 100) == 0) {
                        LOGERROR("Kernel parameter: net.ipv4.ip_forward is not enabled\n");
                        LOGERROR("This parameter is typically enabled by default in /usr/lib/sysctl.d/*-eucanetd.conf\n");
                    }
                    rc = 1;
                }
                if (!check_directory(SYSCTL_BRIDGE_PATH)) {
                    if (!sysctl_enabled(SYSCTL_BRIDGE_CALLIPTABLES)) {
                        if ((i % 100) == 0) {
                            LOGERROR("Kernel parameter: net.bridge.bridge-nf-call-iptables is not enabled\n");
                            LOGERROR("This parameter is typically enabled by default in /usr/lib/sysctl.d/*-eucanetd.conf\n");
                        }
                        rc = 1;
                    }
                } else { 
                    // Proc directory is missing, which probably means that br_netfilter isn't loaded
                    if ((i % 100) == 0) {                    
                        LOGERROR("Unable to find bridge info in proc filesystem\n");
                        LOGERROR("Ensure that br_netfilter has been properly loaded into the kernel\n");
                    }
                    rc = 1;
                }
            }
            
            if (!IS_NETMODE_VPCMIDO(pGni)) {
                eucanetdPeer = eucanetd_detect_peer(pGni);
                if ((PEER_IS_NONE(eucanetdPeer)) || (!PEER_IS_NC(eucanetdPeer))) {
                    if (i % 100 == 0) {
                        LOGWARN("eucanetd in mode %s requires a NC service peer\n", config->netMode);
                    }
                    rc = 1;
                } else {
                    if (!config_peer_ok) {
                        config_peer_ok = TRUE;
                        LOGINFO("\teucanetd valid service peer (%s) detected\n", PEER2STR(eucanetdPeer));
                    }
                }
            } else {
                char *clcip = hex2dot(pGni->enabledCLCIp);
                if (!gni_is_self_getifaddrs(clcip)) {
                    eucanetdPeer = PEER_CLC;
                    if (!config_peer_ok) {
                        config_peer_ok = TRUE;
                        LOGINFO("\teucanetd valid service peer (%s) detected\n", PEER2STR(eucanetdPeer));
                    }
                } else {
                    if (i % 100 == 0) {
                        LOGWARN("eucanetd in mode %s requires CLC service peer\n", config->netMode);
                    }
                    rc = 1;
                }
                EUCA_FREE(clcip);
            }
        }

        if (rc && config->flushmode) {
            LOGFATAL("Unable to complete eucanetd pre-flight checks. Flush aborted.\n");
            exit(1);
        }
        if (gTermCaught) {
            LOGINFO("shutting down eucanetd due to SIGTERM\n");
            exit(0);
        }

        // Initialize our network driver
        if (!rc) {
            rc = eucanetd_initialize_network_drivers(config, pGni);
            if (rc) {
                LOGFATAL("Failed to initialize network driver: eucanetd going down\n");
                exit(1);
            }
        }

        if (rc) {
            sleep(1);
        }
    }
    LOGINFO("eucanetd: pre-flight checks complete.\n");

    // got all config, enter main loop
    while (gIsRunning) {
        eucanetd_timer(&ttv);
        counter++;

        // fetch all latest networking information from various sources
        rc = eucanetd_fetch_latest_network(&update_globalnet, &config_changed);
        if (rc) {
            LOGWARN("one or more fetches for latest network information was unsuccessful\n");
        }
        if (config_changed) {
            pGniApplied = NULL;
        }
        // first time we run, force an update
        if (firstrun) {
            update_globalnet = TRUE;
            firstrun = 0;
        }

        if (gUsr1Caught) {
            if (pDriverHandler->handle_signal) {
                pDriverHandler->handle_signal(config, pGni, SIGUSR1);
            }
            gUsr1Caught = FALSE;
        }
        if (gUsr2Caught) {
            if (pDriverHandler->handle_signal) {
                pDriverHandler->handle_signal(config, pGni, SIGUSR2);
            }
            gUsr2Caught = FALSE;
            // emulate HUP - force update
            gHupCaught = TRUE;
        }
        // Force an update if SIGHUP is caught
        if (gHupCaught) {
            update_globalnet = TRUE;
            // Invalidate last applied version
            config->lastAppliedVersion[0] = '\0';
            gHupCaught = FALSE;
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
            LOGWARN("Failed to populate GNI. skipping update\n");
            // if the local read failed for some reason, skip any attempt to update (leave current state in place)
            update_globalnet = FALSE;
        }

        if (update_globalnet && (pGni->nmCode != config->nmCode)) {
            LOGWARN("Inconsistent network mode in GNI(%s) and eucalyptus.conf(%s). Skipping update.\n", pGni->sMode, config->netMode);
            update_globalnet = FALSE;
        }

        // Do we need to run the network upgrade stuff?
        if (pDriverHandler->upgrade) {
            if (pDriverHandler->upgrade(config, pGni) == 0) {
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
        // Do we need to flush all eucalyptus networking artifacts?
        if (config->flushmode) {
            if (IS_NETMODE_VPCMIDO(pGni) && ((config->flushmode == FLUSH_DYNAMIC) || (config->flushmode == FLUSH_ALL))) {
                LOGERROR("options '-f' and '-F' cannot be used in VPCMIDO mode\n");
            } else {
                if (!IS_NETMODE_VPCMIDO(pGni) && (config->flushmode > FLUSH_DYNAMIC)) {
                    // invalid flush mode for non-VPCMIDO modes
                    LOGERROR("Invalid flush mode selected\n");
                } else {
                    eucanetd_timer(&tv);
                    // Make sure we were given a flush API prior to calling it
                    if (pDriverHandler->system_flush) {
                        if (pDriverHandler->system_flush(config, pGni)) {
                            LOGERROR("flushing of euca networking artifacts failed\n");
                        }
                    }
                    LOGINFO("eucanetd flush executed in %ld ms.\n", eucanetd_timer(&tv));
                }
            }
            update_globalnet = FALSE;
            gIsRunning = FALSE;
            config->flushmode = FLUSH_NONE;
        }
        // if information on sec. group rules/membership has changed, apply
        if (update_globalnet) {
            eucanetd_timer_usec(&tv);
            update_version_file = FALSE;
            LOGINFO("new networking state: updating system\n");

            //
            // If we don't have a scrub API, just call all APIs. Any driver design must have this
            // API defined but for development purpose it make sense to sometimes bypass it.
            //
            if (!pDriverHandler->system_scrub) {
                // Run ALL
                scrubResult = EUCANETD_RUN_ALL_API;
            } else {
                // Scrub the system so see what needs to be done
                scrubResult = pDriverHandler->system_scrub(config, pGni, pGniApplied);
                LOGINFO("eucanetd system_scrub executed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
            }

            // Make sure the scrub did not fail
            if ((scrubResult & EUCANETD_RUN_ERROR_API) == 0) {
                // update network artifacts (devices, tunnels, etc.) if the scrub indicate so
                if (pDriverHandler->implement_network && (scrubResult & EUCANETD_RUN_NETWORK_API)) {
                    rc = pDriverHandler->implement_network(config, pGni);
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
                    rc = pDriverHandler->implement_sg(config, pGni);
                    if (rc) {
                        LOGERROR("could not complete update of security groups: check above log errors for details\n");
                        update_globalnet_failed = TRUE;
                    } else {
                        LOGINFO("eucanetd implement_sg executed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
                    }
                }
                // update IP addressing, elastic IPs, etc. if the scrub indicate so
                if (pDriverHandler->implement_addressing && (scrubResult & EUCANETD_RUN_ADDRESSING_API)) {
                    rc = pDriverHandler->implement_addressing(config, pGni);
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
        }

        if (update_globalnet) {
            if (update_globalnet_failed == TRUE) {
                epoch_failed_updates++;
                if ((scrubResult == EUCANETD_VPCMIDO_IFERROR) ||
                        (scrubResult == EUCANETD_VPCMIDO_GWERROR)) {
                    update_version_file = TRUE;
                }
            } else {
                update_version_file = TRUE;
                snprintf(config->lastAppliedVersion, GNI_VERSION_LEN, "%s", pGni->version);
                config->eucanetd_err = 0;
            }
            if (update_version_file) {
                char versionFile[EUCA_MAX_PATH];
                // update was requested and was successful
                epoch_updates++;
                
                snprintf(versionFile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/global_network_info.version", config->eucahome);
                if (!strlen(pGni->version) || (str2file(pGni->version, versionFile, O_CREAT | O_TRUNC | O_WRONLY, 0644, FALSE) != EUCA_OK) ) {
                    LOGWARN("failed to populate GNI version file '%s': check permissions and disk capacity\n", versionFile);
                }
                
                if (config->eucanetd_first_update) {
                    config->eucanetd_first_update = FALSE;
                }
            }
        }
        epoch_checks++;

        if (epoch_timer >= 300) {
            LOGINFO("eucanetd report: tot_checks=%d tot_update_attempts=%d\n\tsuccess_update_attempts=%d fail_update_attempts=%d duty_cycle_minutes=%f\n", epoch_checks,
                    epoch_updates + epoch_failed_updates, epoch_updates, epoch_failed_updates, (float)epoch_timer / 60.0);
            epoch_checks = epoch_updates = epoch_failed_updates = epoch_timer = 0;
        }

        if ((update_globalnet_failed == FALSE) && (update_globalnet == FALSE) && (gIsRunning == TRUE)) {
            if (pDriverHandler->system_maint) {
                rc = pDriverHandler->system_maint(config, pGniApplied);
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
                LOGTRACE("main loop complete (%ld ms): sleeping %d seconds before next poll\n", eucanetd_timer(&ttv), config->polling_frequency);
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
        if (pDriverHandler->cleanup(config, pGni, FALSE) != 0) {
            LOGERROR("Failed to cleanup '%s' network driver.\n", pDriverHandler->name);
        }
    }

    eucanetd_cleanup();
    GNI_FREE(gni_a);
    GNI_FREE(gni_b);

    LOGINFO("=== eucanetd down ===\n");
    exit(0);
}
#endif /* ! EUCANETD_UNIT_TEST */

/**
 * Handles the SIGTERM signal
 * @param signal [in] received signal number (SIGTERM is expected)
 */
static void eucanetd_sigterm_handler(int signal) {
    LOGINFO("eucanetd caught SIGTERM signal.\n");
    gIsRunning = FALSE;
    gTermCaught = TRUE;
    sig_rcvd = signal;
}

/**
 * Handles the SIGHUP signal
 * @param signal [in] received signal number (SIGHUP is expected)
 */
static void eucanetd_sighup_handler(int signal) {
    LOGINFO("eucanetd caught a SIGHUP signal.\n");
    config->flushmode = FLUSH_NONE;
    gHupCaught = TRUE;
    sig_rcvd = signal;
}

/**
 * Handles SIGUSR1 signal.
 * @param signal [in] received signal number.
 */
static void eucanetd_sigusr1_handler(int signal) {
    LOGDEBUG("eucanetd caught a SIGUSR1 (%d) signal.\n", signal);
    gUsr1Caught = TRUE;
    sig_rcvd = signal;
}

/**
 * Handles SIGUSR2 signal.
 * @param signal [in] received signal number.
 */
static void eucanetd_sigusr2_handler(int signal) {
    LOGDEBUG("eucanetd caught a SIGUSR2 (%d) signal.\n", signal);
    gUsr2Caught = TRUE;
    sig_rcvd = signal;
}

/**
 * Emulates the reception of SIGUSR2.
 */
void eucanetd_emulate_sigusr2(void) {
    gUsr2Caught = TRUE;
    sig_rcvd = SIGUSR2;
}

/**
 * Installs signal handlers for this application
 */
static void eucanetd_install_signal_handlers(void) {
    struct sigaction act = { {0} };

    // Install the SIGTERM signal handler
    bzero(&act, sizeof(struct sigaction));
    act.sa_handler = &eucanetd_sigterm_handler;
    if (sigaction(SIGTERM, &act, NULL) < 0) {
        LOGFATAL("Failed to install SIGTERM handler");
        exit(1);
    }
    // Install the SIGHUP signal handler
    bzero(&act, sizeof(struct sigaction));
    act.sa_handler = &eucanetd_sighup_handler;
    if (sigaction(SIGHUP, &act, NULL) < 0) {
        LOGFATAL("Failed to install SIGTERM handler");
        exit(1);
    }
    // Install the SIGUSR1 signal handler
    bzero(&act, sizeof(struct sigaction));
    act.sa_handler = &eucanetd_sigusr1_handler;
    if (sigaction(SIGUSR1, &act, NULL) < 0) {
        LOGFATAL("Failed to install SIGUSR1 handler");
        exit(1);
    }
    // Install the SIGUSR1 signal handler
    bzero(&act, sizeof(struct sigaction));
    act.sa_handler = &eucanetd_sigusr2_handler;
    if (sigaction(SIGUSR2, &act, NULL) < 0) {
        LOGFATAL("Failed to install SIGUSR2 handler");
        exit(1);
    }
}

/**
 * Check eucanetd config files for changes
 *
 * @return 0 if no changes have been detected. Positive integer if configuration
 * parameters not requiring restart has changed.
 *
 * @note
 *     Currently only /etc/eucalyptus/eucalyptus.conf is checked
 */
static int eucanetd_fetch_latest_local_config(void) {
    int ret = 0;
    char *cval = NULL;

    if (isConfigModified(config->configFiles, NUM_EUCANETD_CONFIG) > 0) {
        // config modification time has changed
        if (readConfigFile(config->configFiles, NUM_EUCANETD_CONFIG)) {
            // something has changed that can be read in
            LOGINFO("ingressing new configuration options\n");
            eucanetd_initialize_logs();

            if (IS_NETMODE_VPCMIDO(config)) {
                cval = configFileValue("MIDO_ENABLE_ARPTABLE");
                if (!strcmp(cval, "Y")) {
                    if (!config->enable_mido_arptable) {
                        config->mido_arptable_config_changed = TRUE;
                    }
                    config->enable_mido_arptable = TRUE;
                } else {
                    if (config->enable_mido_arptable) {
                        config->mido_arptable_config_changed = TRUE;
                    }
                    config->enable_mido_arptable = FALSE;
                }
                EUCA_FREE(cval);

                cval = configFileValue("MIDO_ENABLE_MIDOMD");
                if (!strcmp(cval, "Y")) {
                    if (!config->enable_mido_md) {
                        config->mido_md_config_changed = TRUE;
                    }
                    config->enable_mido_md = TRUE;
                } else {
                    if (config->enable_mido_md) {
                        config->mido_md_config_changed = TRUE;
                    }
                    config->enable_mido_md = FALSE;
                }
                EUCA_FREE(cval);

                cval = configFileValue("MIDO_MD_254_EGRESS");
                if (strcmp(cval, config->mido_md_254_egress)) {
                    config->mido_md_egress_rules_changed = TRUE;
                    snprintf(config->mido_md_254_egress, 256, "%s", cval);
                }
                EUCA_FREE(cval);
                cval = configFileValue("MIDO_MD_253_EGRESS");
                if (strcmp(cval, config->mido_md_253_egress)) {
                    config->mido_md_egress_rules_changed = TRUE;
                    snprintf(config->mido_md_253_egress, 256, "%s", cval);
                }
                EUCA_FREE(cval);

                cval = configFileValue("MIDO_API_URIBASE");
                if (strcmp(cval, config->mido_api_uribase)) {
                    LOGINFO("config MIDO_API_URIBASE = %s\n", cval);
                    snprintf(config->mido_api_uribase, URI_LEN, "%s", cval);
                    int uribaselen = strlen(config->mido_api_uribase);
                    if (uribaselen && config->mido_api_uribase[uribaselen - 1] == '/') {
                        config->mido_api_uribase[uribaselen - 1] = '\0';
                    }
                    pDriverHandler->cleanup(config, pGni, FALSE);
                }
                EUCA_FREE(cval);

                // emulate HUP signal
                gHupCaught = TRUE;
            }
            ret++;

            // TODO  pick up other eucanetd options dynamically
        }
    }
    return (ret);
}

/**
 * Daemonize switches user (drop priv), closes FDs, and back-grounds
 *
 * @return 0 or exits
 *
 * @post On success, the process has been daemonized; STDIN, STDOUT and STDERR
 *       have been closed (non-debug); and the daemon has been setup properly.
 *       On failure, the process will exit.
 */
static int eucanetd_daemonize(void) {
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

    char eucadir[EUCA_MAX_PATH] = "";
    snprintf(eucadir, EUCA_MAX_PATH, "%s/var/log/eucalyptus", config->eucahome);
    if (check_directory(eucadir)) {
        fprintf(stderr, "cannot locate eucalyptus installation: make sure EUCALYPTUS env is set\n");
        exit(1);
    }

    pid = getpid();
    if (pid > 1) {
        snprintf(pidfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/eucanetd.pid", config->eucahome);
        FH = fopen(pidfile, "w");
        if (FH) {
            fprintf(FH, "%d\n", pid);
            fclose(FH);
        } else {
            fprintf(stderr, "could not open pidfile for write (%s)\n", pidfile);
            exit(1);
        }
    }

    if (config->debug == EUCANETD_DEBUG_NONE) {
        close(STDIN_FILENO);
        close(STDOUT_FILENO);
        close(STDERR_FILENO);
    }

    return (0);
}

/**
 * Initialize eucanetd service
 *
 * @return 0 on success or exits with a value of 1 on failure
 *
 * @post On success, eucanetd service has been initialized. On
 *       failure, the process will be terminated
 */
static int eucanetd_initialize(void) {
    if (!config) {
        config = EUCA_ZALLOC_C(1, sizeof(eucanetdConfig));
    }

    config->euca_version = euca_version_dot2hex(EUCA_VERSION);
    config->euca_version_str = hex2dot(config->euca_version);
    config->eucanetd_first_update = TRUE;

    config->polling_frequency = 5;
    config->init = 1;
    
    return (0);
}

/**
 * Initialize the network drivers. When implementing a new network driver, simply set
 * the global 'pDriverHandler' variable to your new driver callback structure.
 * @param pConfig [in] a pointer to the eucanetd configuration structure
 * @param pGni [in] a pointer to our global network information structure
 * @return On success, the proper driver handler is selected and the driver initialization
 * routine has been called. On failure, the state of the driver is left undetermined.
 */
static int eucanetd_initialize_network_drivers(eucanetdConfig *pConfig, globalNetworkInfo *pGni) {
    // Make sure our given parameter is valid
    if (pConfig) {
        LOGINFO("Loading '%s' mode driver.\n", pConfig->netMode);
        if (IS_NETMODE_EDGE(pConfig)) {
            pDriverHandler = &edgeDriverHandler;
        } else if (IS_NETMODE_VPCMIDO(pConfig)) {
            pDriverHandler = &midoVpcDriverHandler;
        } else {
            LOGERROR("Invalid network mode '%s' configured!\n", pConfig->netMode);
            return (1);
        }

        // If we have an init function. Lets call it now
        if (pDriverHandler->init) {
            if (pDriverHandler->init(pConfig, pGni) != 0) {
                LOGERROR("Failed to initialize '%s' driver!\n", pConfig->netMode);
                return (1);
            }
        }
        return (0);
    }
    return (1);
}

/**
 * Cleanup eucanetd service
 *
 * @return always 0.
 *
 * @post On success, resources initialized by eucanetd service is cleaned
 */
static int eucanetd_cleanup(void) {
    if (config) {
        EUCA_FREE(config->eucahome);
        EUCA_FREE(config->eucauser);
        EUCA_FREE(config->euca_version_str);
        EUCA_FREE(config->my_ips);
        ipt_handler_close(config->ipt);
        ips_handler_close(config->ips);
        ebt_handler_close(config->ebt);
        EUCA_FREE(config->ipt);
        EUCA_FREE(config->ips);
        EUCA_FREE(config->ebt);
        config->polling_frequency = 5;
        config->init = 1;
        atomic_file_free(&(config->global_network_info_file));
    }
    EUCA_FREE(config);

    return (0);
}

/**
 * Read and sets the environment parameters.
 * @return 0 on success or the process will exit with a value of 1 on failure.
 */
static int eucanetd_read_config_bootstrap(void) {
    int ret = 0;
    char *eucaenv = getenv(EUCALYPTUS_ENV_VAR_NAME);
    char *eucauserenv = getenv(EUCALYPTUS_USER_ENV_VAR_NAME);
    char home[EUCA_MAX_PATH] = "";
    char user[EUCA_MAX_PATH] = "";

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

    config->eucahome = strdup(home);
    if (strlen(config->eucahome)) {
        if (config->eucahome[strlen(config->eucahome) - 1] == '/') {
            config->eucahome[strlen(config->eucahome) - 1] = '\0';
        }
    }
    config->eucauser = strdup(user);
    snprintf(config->cmdprefix, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, config->eucahome);

    return (ret);
}

/**
 * Performs basic configuration to the log subsystem
 * @return always 0
 */
static int eucanetd_setlog_bootstrap(void) {
    int ret = 0;
    char logfile[EUCA_MAX_PATH] = "";

    ret = 0;

    switch (config->debug) {
        case EUCANETD_DEBUG_NONE:
            snprintf(logfile, EUCA_MAX_PATH, "%s/var/log/eucalyptus/eucanetd.log", config->eucahome);
            log_file_set(logfile, NULL);
            log_params_set(EUCA_LOG_INFO, 0, 100000);
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

/**
 * Reads the eucalyptus.conf configuration file and pull the important fields. It
 * also attempt to read the global network information XML and starts applying some
 * of these configuration to the system.
 *
 * @return 0 on success or 1 on failure. If any FATAL error occurs, the
 *         process fill exit with an exit code of 1.
 */
static int eucanetd_read_config(globalNetworkInfo *pGni) {
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
        return (1);
    }

    snprintf(netPath, EUCA_MAX_PATH, CC_NET_PATH_DEFAULT, home);

    // search for the global state file from eucalyptus
    snprintf(sourceuri, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/global_network_info.xml", home);
    if (check_file(sourceuri)) {
        snprintf(sourceuri, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/cc_global_network_info.xml", home);
        if (check_file(sourceuri)) {
            snprintf(sourceuri, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/global_network_info.xml", home);
            if (check_file(sourceuri)) {
                LOGTRACE("cannot find global_network_info.xml state file in $EUCALYPTUS/var/lib/eucalyptus or $EUCALYPTUS/var/run/eucalyptus yet.\n");
                return (1);
            } else {
                snprintf(sourceuri, EUCA_MAX_PATH, "file://" EUCALYPTUS_STATE_DIR "/global_network_info.xml", home);
                snprintf(destfile, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/eucanetd_global_network_info.xml", home);
                LOGTRACE("found global_network_info.xml state file: setting source URI to '%s'\n", sourceuri);
            }
        } else {
            snprintf(sourceuri, EUCA_MAX_PATH, "file://" EUCALYPTUS_RUN_DIR "/cc_global_network_info.xml", home);
            snprintf(destfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/eucanetd_global_network_info.xml", home);
            LOGTRACE("found global_network_info.xml state file: setting source URI to '%s'\n", sourceuri);
        }
    } else {
        snprintf(sourceuri, EUCA_MAX_PATH, "file://" EUCALYPTUS_RUN_DIR "/global_network_info.xml", home);
        snprintf(destfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/eucanetd_global_network_info.xml", home);
        LOGTRACE("found global_network_info.xml state file: setting source URI to '%s'\n", sourceuri);
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

    rc = gni_populate_v(GNI_POPULATE_CONFIG, pGni, host_info, config->global_network_info_file.dest);
    if (rc) {
        LOGDEBUG("could not initialize global network info data structures from XML input\n");
        for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
            EUCA_FREE(cvals[i]);
        }
        return (1);
    }
    gni_print(pGni, EUCA_LOG_TRACE);

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
    cvals[EUCANETD_CVAL_SYSTEMCTL] = configFileValue("VNET_SYSTEMCTL");
    cvals[EUCANETD_CVAL_POLLING_FREQUENCY] = configFileValue("POLLING_FREQUENCY");
    cvals[EUCANETD_CVAL_DISABLE_L2_ISOLATION] = configFileValue("DISABLE_L2_ISOLATION");
    cvals[EUCANETD_CVAL_NC_PROXY] = configFileValue("NC_PROXY");
    cvals[EUCANETD_CVAL_NC_ROUTER] = configFileValue("NC_ROUTER");
    cvals[EUCANETD_CVAL_NC_ROUTER_IP] = configFileValue("NC_ROUTER_IP");
    cvals[EUCANETD_CVAL_METADATA_USE_VM_PRIVATE] = configFileValue("METADATA_USE_VM_PRIVATE");
    cvals[EUCANETD_CVAL_METADATA_IP] = configFileValue("METADATA_IP");
    cvals[EUCANETD_CVAL_MIDO_INTRTCIDR] = configFileValue("MIDO_INTRT_CIDR");
    cvals[EUCANETD_CVAL_MIDO_INTMDCIDR] = configFileValue("MIDO_INTMD_CIDR");
    cvals[EUCANETD_CVAL_MIDO_EXTMDCIDR] = configFileValue("MIDO_EXTMD_CIDR");
    cvals[EUCANETD_CVAL_MIDO_MDCIDR] = configFileValue("MIDO_MD_CIDR");
    cvals[EUCANETD_CVAL_MIDO_MAX_RTID] = configFileValue("MIDO_MAX_RTID");
    cvals[EUCANETD_CVAL_MIDO_MAX_ENIID] = configFileValue("MIDO_MAX_ENIID");
    cvals[EUCANETD_CVAL_MIDO_ENABLE_ARPTABLE] = configFileValue("MIDO_ENABLE_ARPTABLE");
    cvals[EUCANETD_CVAL_MIDO_ENABLE_MIDOMD] = configFileValue("MIDO_ENABLE_MIDOMD");
    cvals[EUCANETD_CVAL_MIDO_API_URIBASE] = configFileValue("MIDO_API_URIBASE");
    cvals[EUCANETD_CVAL_MIDO_MD_VETH_USE_NETNS] = configFileValue("MIDO_MD_VETH_USE_NETNS");
    cvals[EUCANETD_CVAL_MIDO_MD_254_EGRESS] = configFileValue("MIDO_MD_254_EGRESS");
    cvals[EUCANETD_CVAL_MIDO_MD_253_EGRESS] = configFileValue("MIDO_MD_253_EGRESS");
#ifdef VPCMIDO_DEVELOPER
    cvals[EUCANETD_CVAL_MIDO_VALIDATE_MIDOCONFIG] = configFileValue("MIDO_VALIDATE_MIDOCONFIG");
#else
    cvals[EUCANETD_CVAL_MIDO_VALIDATE_MIDOCONFIG] = strdup("Y");
#endif // VPCMIDO_DEVELOPER

    EUCA_FREE(config->eucahome);
    config->eucahome = strdup(cvals[EUCANETD_CVAL_EUCAHOME]);
    if (strlen(config->eucahome)) {
        if (config->eucahome[strlen(config->eucahome) - 1] == '/') {
            config->eucahome[strlen(config->eucahome) - 1] = '\0';
        }
    }

    EUCA_FREE(config->eucauser);
    config->eucauser = strdup(cvals[EUCANETD_CVAL_EUCA_USER]);

    snprintf(config->cmdprefix, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, config->eucahome);
    config->polling_frequency = atoi(cvals[EUCANETD_CVAL_POLLING_FREQUENCY]);

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

    // Only accept network mode configuration from GNI
    if (strlen(pGni->sMode)) {
        snprintf(config->netMode, NETMODE_LEN, "%s", pGni->sMode);
    } else {
        snprintf(config->netMode, NETMODE_LEN, "%s", NETMODE_INVALID);
    }
    //snprintf(config->netMode, NETMODE_LEN, "%s", cvals[EUCANETD_CVAL_MODE]);
    config->nmCode = euca_netmode_atoi(config->netMode);
    snprintf(config->pubInterface, IF_NAME_LEN, "%s", cvals[EUCANETD_CVAL_PUBINTERFACE]);
    snprintf(config->privInterface, IF_NAME_LEN, "%s", cvals[EUCANETD_CVAL_PRIVINTERFACE]);
    snprintf(config->bridgeDev, IF_NAME_LEN, "%s", cvals[EUCANETD_CVAL_BRIDGE]);
    snprintf(config->dhcpDaemon, EUCA_MAX_PATH, "%s", cvals[EUCANETD_CVAL_DHCPDAEMON]);
    snprintf(config->systemctl, EUCA_MAX_PATH, "%s", cvals[EUCANETD_CVAL_SYSTEMCTL]);

    if (strlen(config->systemctl)) {
        char cmd[EUCA_MAX_PATH] = "";
        snprintf(cmd, EUCA_MAX_PATH, "%s %s --version", config->cmdprefix, config->systemctl);
        rc = timeshell_nb(cmd, 10, TRUE);
        if (rc != 0) {
            LOGERROR("invalid VNET_SYSTEMCTL (%s) - reverting to \"systemctl\"\n", config->systemctl);
            snprintf(config->systemctl, EUCA_MAX_PATH, "%s", "systemctl");
        }
    }

    // mido config opts
    if (IS_NETMODE_VPCMIDO(config)) {
        if (cvals[EUCANETD_CVAL_MIDO_INTRTCIDR])
            snprintf(config->mido_intrtcidr, NETWORK_ADDR_LEN, "%s", cvals[EUCANETD_CVAL_MIDO_INTRTCIDR]);
        if (cvals[EUCANETD_CVAL_MIDO_INTMDCIDR])
            snprintf(config->mido_intmdcidr, NETWORK_ADDR_LEN, "%s", cvals[EUCANETD_CVAL_MIDO_INTMDCIDR]);
        if (cvals[EUCANETD_CVAL_MIDO_EXTMDCIDR])
            snprintf(config->mido_extmdcidr, NETWORK_ADDR_LEN, "%s", cvals[EUCANETD_CVAL_MIDO_EXTMDCIDR]);
        if (cvals[EUCANETD_CVAL_MIDO_MDCIDR])
            snprintf(config->mido_mdcidr, NETWORK_ADDR_LEN, "%s", cvals[EUCANETD_CVAL_MIDO_MDCIDR]);
        config->mido_max_rtid = atoi(cvals[EUCANETD_CVAL_MIDO_MAX_RTID]);
        config->mido_max_eniid = atoi(cvals[EUCANETD_CVAL_MIDO_MAX_ENIID]);
        if (cvals[EUCANETD_CVAL_MIDO_MD_254_EGRESS])
            snprintf(config->mido_md_254_egress, 256, "%s", cvals[EUCANETD_CVAL_MIDO_MD_254_EGRESS]);
        if (cvals[EUCANETD_CVAL_MIDO_MD_253_EGRESS])
            snprintf(config->mido_md_253_egress, 256, "%s", cvals[EUCANETD_CVAL_MIDO_MD_253_EGRESS]);
        if (!strcmp(cvals[EUCANETD_CVAL_MIDO_ENABLE_ARPTABLE], "Y")) {
            config->enable_mido_arptable = TRUE;
        } else {
            config->enable_mido_arptable = FALSE;
        }
        if (!strcmp(cvals[EUCANETD_CVAL_MIDO_ENABLE_MIDOMD], "Y")) {
            config->enable_mido_md = TRUE;
        } else {
            config->enable_mido_md = FALSE;
        }
        if ((cvals[EUCANETD_CVAL_MIDO_API_URIBASE]) && (strlen(cvals[EUCANETD_CVAL_MIDO_API_URIBASE]))) {
            snprintf(config->mido_api_uribase, URI_LEN, "%s", cvals[EUCANETD_CVAL_MIDO_API_URIBASE]);
            int uribaselen = strlen(config->mido_api_uribase);
            if (uribaselen && config->mido_api_uribase[uribaselen - 1] == '/') {
                config->mido_api_uribase[uribaselen - 1] = '\0';
            }
        }
        if (!strcmp(cvals[EUCANETD_CVAL_MIDO_MD_VETH_USE_NETNS], "Y")) {
            config->mido_md_veth_use_netns = TRUE;
        } else {
            config->mido_md_veth_use_netns = FALSE;
        }
        if (!strcmp(cvals[EUCANETD_CVAL_MIDO_VALIDATE_MIDOCONFIG], "Y")) {
            config->validate_mido_config = TRUE;
        } else {
            config->validate_mido_config = FALSE;
        }
    }

    LOGTRACE("required variables read from local config file: EUCALYPTUS=%s EUCA_USER=%s VNET_MODE=%s VNET_PUBINTERFACE=%s VNET_PRIVINTERFACE=%s VNET_BRIDGE=%s "
            "VNET_DHCPDAEMON=%s\n", SP(cvals[EUCANETD_CVAL_EUCAHOME]), SP(cvals[EUCANETD_CVAL_EUCA_USER]), SP(cvals[EUCANETD_CVAL_MODE]), SP(cvals[EUCANETD_CVAL_PUBINTERFACE]),
            SP(cvals[EUCANETD_CVAL_PRIVINTERFACE]), SP(cvals[EUCANETD_CVAL_BRIDGE]), SP(cvals[EUCANETD_CVAL_DHCPDAEMON]));

    rc = eucanetd_initialize_logs();
    if (rc) {
        LOGERROR("unable to initialize logging subsystem: check permissions and log config options\n");
        ret = 1;
    }

    for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
        EUCA_FREE(cvals[i]);
    }

    return (ret);
}

/**
 * Initialize the logging services
 *
 * @return Always returns 0
 *
 * @see log_file_set(), configReadLogParams(), log_params_set(), log_prefix_set()
 */
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
        case EUCANETD_DEBUG_WARN:
            log_params_set(EUCA_LOG_WARN, 0, 100000);
            break;
        case EUCANETD_DEBUG_ERROR:
            log_params_set(EUCA_LOG_ERROR, 0, 100000);
            break;
        case EUCANETD_DEBUG_FATAL:
            log_params_set(EUCA_LOG_FATAL, 0, 100000);
            break;
        default:
            log_params_set(EUCA_LOG_TRACE, 0, 100000);
            break;
    }

    return (0);
}

/**
 * Checks if the contents of the global network information has changed.
 *
 * @param update_globalnet [out] set to true if the network information changed
 * @param config_changed [out] set to true if local configuration (not requiring
 * restart) has changed
 *
 * @return 0 on success. 1 on failure.
 */
static int eucanetd_fetch_latest_network(boolean *update_globalnet, boolean *config_changed) {
    int rc = 0, ret = 0;

    LOGTRACE("fetching latest network view\n");

    if (!update_globalnet) {
        LOGERROR("BUG: input contains null pointers\n");
        return (1);
    }
    // don't run any updates unless something new has happened
    *update_globalnet = FALSE;

    if (config_changed) {
        *config_changed = FALSE;
    }
    rc = eucanetd_fetch_latest_local_config();
    if (rc) {
        if (config_changed) {
            *config_changed = TRUE;
        }
    }
    // get latest networking data from eucalyptus, set update flags if content has changed
    rc = eucanetd_fetch_latest_euca_network(update_globalnet);
    if (rc) {
        LOGWARN("cannot get latest network topology, configuration and/or local VM network from CC/NC: check that CC and NC are running\n");
        ret = 1;
    }

    return (ret);
}

/**
 * Checks if the contents of the global network information has changed.
 *
 * @param update_globalnet [out] set to true if the network information changed
 *
 * @return 0 on success. 1 on failure.
 */
static int eucanetd_fetch_latest_euca_network(boolean * update_globalnet) {
    int rc = 0, ret = 0;

    rc = atomic_file_get(&(config->global_network_info_file), update_globalnet);
    if (rc) {
        LOGWARN("Failed to fetch latest global network\n");
        ret = 1;
    }

    return (ret);
}

/**
 * Retrieves and parses the latest network information
 * @param pGni [out] globalNetworkInfo data structure where parsed data will be stored
 * @param update_globalnet [out] set to FALSE if no update is necessary
 * @return 0  on success. 1 on failure.
 */
static int eucanetd_read_latest_network(globalNetworkInfo *pGni, boolean *update_globalnet) {
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

    LOGTRACE("reading latest network view into eucanetd\n");

    if (!update_globalnet) {
        LOGWARN("Invalid argument: update_globalnet is null.\n");
        return (1);
    }
    rc = gni_populate(pGni, host_info, config->global_network_info_file.dest);
    if (rc) {
        LOGERROR("failed to initialize global network info data structures from XML file: check network config settings\n");
        ret = 1;
    } else {
        gni_print(pGni, EUCA_LOG_TRACE);

        // regardless, if the last successfully applied version matches the current GNI version, skip the update
        if ((strlen(pGni->version) && strlen(config->lastAppliedVersion))) {
            if (!strcmp(pGni->version, config->lastAppliedVersion)) {
                LOGINFO("global network version (%s) already applied, skipping update\n", pGni->version);
                *update_globalnet = FALSE;
            } else {
                LOGTRACE("global network version (%s) does not match last successfully applied version (%s), continuing\n", pGni->version, config->lastAppliedVersion);
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
                    LOGTRACE("using default cluster private subnet GW as VM default GW: %s\n", strptra);
                    EUCA_FREE(strptra);
                } else {
                    // user has specified use of NC as router
                    if (!config->nc_router_ip) {
                        // user has not specified a router IP, use 'fake_router' mode                                              
                        config->vmGatewayIP = mycluster->private_subnet.gateway;
                        strptra = hex2dot(config->vmGatewayIP);
                        LOGTRACE("using default cluster private subnet GW, with ARP spoofing, as VM default GW: %s\n", strptra);
                        EUCA_FREE(strptra);
                    } else if (config->nc_router_ip && strcmp(config->ncRouterIP, "AUTO")) {
                        // user has specified an explicit IP to use as NC router IP
                        config->vmGatewayIP = dot2hex(config->ncRouterIP);
                        LOGTRACE("using user specified NC IP as VM default GW: %s\n", config->ncRouterIP);
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
                                    LOGTRACE("using auto-detected NC IP as VM default GW: %s\n", strptra);
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

/**
 * Checks whether we are running alongside a CC or NC service
 *
 * @param pGni [in] a pointer to our global network information structure
 *
 * @return Returns the proper role associated with this service
 */
static int eucanetd_detect_peer(globalNetworkInfo *pGni)
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
 * Creates an UDP socket listening on UDP port NEUCA (63822). If bind fails, another
 * instance of eucanetd is likely to be running.
 * @return 0 on success. -1 on error.
 */
int eucanetd_dummy_udpsock(void) {
    struct sockaddr_in dummysock;
    int s = -1;

    s = socket(AF_INET, SOCK_DGRAM | SOCK_CLOEXEC, IPPROTO_UDP);
    if (s == -1) {
        LOGERROR("Failed to create eucanetd udp socket.\n");
        return (-1);
    }

    bzero(&dummysock, sizeof(struct sockaddr_in));
    dummysock.sin_family = AF_INET;
    dummysock.sin_port = htons(EUCANETD_DUMMY_UDP_PORT);
    inet_aton("127.0.0.1", &(dummysock.sin_addr));
    if (bind(s, (struct sockaddr *) &dummysock, sizeof(dummysock)) == -1) {
        close(s);
        return (-1);
    }
    shutdown(s, SHUT_RD);
    eucanetd_dummysock = s;
    return (0);
}

/**
 * Closes the UDP socket listening on UDP port NEUCA (63822).
 * @return 0 on success. -1 on error.
 */
int eucanetd_dummy_udpsock_close(void) {
    int rc = 0;
    if (eucanetd_dummysock > 0) {
        rc = close(eucanetd_dummysock);
        if (rc) {
            LOGWARN("Failed to close eucanetd_dummysock\n");
        }
        eucanetd_dummysock = -1;
        return (rc);
    }
    return (1);
}

