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
//! @file util/template.c
//! Template source file
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
#include <pwd.h>
#include <dirent.h>
#include <errno.h>

#include <eucalyptus.h>
#include <misc.h>
#include <vnetwork.h>
#include <euca_string.h>
#include <log.h>
#include <hash.h>
#include <math.h>
#include <http.h>
#include <config.h>
#include <sequence_executor.h>
#include <ipt_handler.h>
#include <atomic_file.h>

#include "eucanetd.h"
#include "config-eucanetd.h"
#include "globalnetwork.h"

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

//vnetConfig *vnetconfig = NULL;
eucanetdConfig *config = NULL;
globalNetworkInfo *globalnetworkinfo = NULL;

configEntry configKeysRestartEUCANETD[] = {
    {"EUCALYPTUS", "/"}
    ,
    {"VNET_BRIDGE", NULL}
    ,
    {"VNET_BROADCAST", NULL}
    ,
    {"VNET_DHCPDAEMON", "/usr/sbin/dhcpd41"}
    ,
    {"VNET_DHCPUSER", "root"}
    ,
    {"VNET_DNS", NULL}
    ,
    {"VNET_DOMAINNAME", "eucalyptus.internal"}
    ,
    {"VNET_MODE", "EDGE"}
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
    {"EUCA_USER", "eucalyptus"}
    ,
    {NULL, NULL}
    ,
};

configEntry configKeysNoRestartEUCANETD[] = {
    {"CC_POLLING_FREQUENCY", "5"}
    ,
    {"DISABLE_L2_ISOLATION", "N"}
    ,
    {"FAKE_ROUTER", "N"}
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
//! @param[in] argc
//! @param[in] argv
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
int main(int argc, char **argv)
{
    int rc = 0;
    int opt = 0;
    int firstrun = 1;
    int counter = 0;
    int epoch_updates = 0;
    int epoch_failed_updates = 0;
    int epoch_checks = 0;
    int update_globalnet_failed = 0;
    int update_globalnet = 0;
    time_t epoch_timer = 0;

    // initialize
    eucanetdInit();

    // parse commandline arguments
    while ((opt = getopt(argc, argv, "dhF")) != -1) {
        switch (opt) {
        case 'd':
            config->debug = 1;
            break;
        case 'F':
            config->flushmode = 1;
            config->debug = 1;
            break;
        case 'h':
            printf("USAGE: %s OPTIONS\n\t%-12s| debug - run eucanetd in foreground, all output to terminal\n\t%-12s| flush - clear all iptables/ebtables/ipset rules\n", argv[0],
                   "-d", "-F");
            exit(1);
            break;
        default:
            printf("USAGE: %s OPTIONS\n\t%-12s| debug - run eucanetd in foreground, all output to terminal\n\t%-12s| flush - clear all iptables/ebtables/ipset rules\n", argv[0],
                   "-d", "-F");
            exit(1);
            break;
        }
    }

    // need just enough config to initialize things and set up logging subsystem
    rc = read_config_bootstrap();
    if (rc) {
        fprintf(stderr, "could not read enough config to bootstrap eucanetd, exiting\n");
        exit(1);
    }
    // daemonize!
    rc = daemonize();
    if (rc) {
        fprintf(stderr, "failed to daemonize eucanetd, exiting\n");
        exit(1);
    }

    if (config->flushmode) {
        if (flush_all()) {
            LOGERROR("manual flushing of all euca networking artifacts (iptables, ebtables, ipset) failed: check above log errors for details\n");
            exit(1);
        }
        exit(0);
    }

    /* for testing XML validation
       {
       globalNetworkInfo *mygni;
       mygni = gni_init();
       gni_populate(mygni, "/tmp/euca-global-net-cwNFRB");
       exit(0);
       }
     */

    LOGINFO("eucanetd started\n");

    // spin here until we get the latest config from active CC
    rc = 1;
    while (rc) {
        rc = read_config();
        if (rc) {
            LOGWARN("cannot complete pre-flight checks (ignore if local NC has not yet been registered), retrying\n");
            sleep(1);
        }
    }

    // got all config, enter main loop
    //    while(counter<25) {
    while (1) {
        update_globalnet = 0;

        counter++;

        // fetch all latest networking information from various sources
        rc = fetch_latest_network(&update_globalnet);
        if (rc) {
            LOGWARN("one or more fetches for latest network information was unsucessful\n");
        }
        // first time we run, force an update
        if (firstrun) {
            update_globalnet = 1;
            firstrun = 0;
        }
        // if the last update operations failed, regardless of new info, force an update
        if (update_globalnet_failed) {
            LOGDEBUG("last update of network state failed, forcing a retry: update_globalnet_failed=%d\n", update_globalnet_failed);
            update_globalnet = 1;
        }
        update_globalnet_failed = 0;

        // whether or not updates have occurred due to remote content being updated, read local networking info
        rc = read_latest_network();
        if (rc) {
            LOGWARN("read_latest_network failed, skipping update\n");
            // if the local read failed for some reason, skip any attempt to update (leave current state in place)
            update_globalnet = 0;
        }
        // now, preform any updates that are required
        if (update_globalnet) {
            LOGINFO("new networking state (CLC IP metadata service): updating system\n");
            // update metadata redirect rule
            rc = update_metadata_redirect();
            if (rc) {
                LOGERROR("could not update metadata redirect rules: check above log errors for details\n");
                update_globalnet_failed = 1;
            } else {
                LOGINFO("new networking state (CLC IP metadata service): updated successfully\n");
            }
        }
        // if information on sec. group rules/membership has changed, apply
        if (update_globalnet) {
            LOGINFO("new networking state (VM security groups): updating system\n");
            // install iptables FW rules, using IPsets for sec. group
            rc = update_sec_groups();
            if (rc) {
                LOGERROR("could not complete update of security groups: check above log errors for details\n");
                update_globalnet_failed = 1;
            } else {
                LOGINFO("new networking state (VM security groups): updated successfully\n");
            }
        }
        // if information about local VM network config has changed, apply
        if (update_globalnet) {
            LOGINFO("new networking state (VM public/private network addresses, VM network isolation): updating system\n");
            // update list of private IPs, handle DHCP daemon re-configure and restart
            rc = update_private_ips();
            if (rc) {
                LOGERROR("could not complete update of private IPs: check above log errors for details\n");
                update_globalnet_failed = 1;
            } else {
                LOGINFO("new networking state (VM private network addresses): updated successfully\n");
            }

            // update public IP assignment and NAT table entries
            rc = update_public_ips();
            if (rc) {
                LOGERROR("could not complete update of public IPs: check above log errors for details\n");
                update_globalnet_failed = 1;
            } else {
                LOGINFO("new networking state (VM public network addresses): updated successfully\n");
            }

            // install ebtables rules for isolation
            rc = update_isolation_rules();
            if (rc) {
                if (epoch_failed_updates >= 60) {
                    LOGERROR("could not complete update of VM network isolation rules after 60 retries: check above log errors for details\n");
                } else {
                    LOGWARN("retry (%d): could not complete update of VM network isolation rules: retrying\n", epoch_failed_updates);
                }
                update_globalnet_failed = 1;
            } else {
                LOGINFO("new networking state (VM network isolation): updated successfully\n");
            }
        }

        if (update_globalnet) {
            if (update_globalnet_failed) {
                epoch_failed_updates++;
            } else {
                epoch_updates++;
            }
        }
        epoch_checks++;

        // temporary exit after one iteration
        //        LOGTRACE("MEH: exiting\n");
        //        exit(0);

        if (epoch_timer >= 300) {
            LOGINFO("eucanetd report: tot_checks=%d tot_update_attempts=%d success_update_attempts=%d fail_update_attempts=%d duty_cycle_minutes=%f\n", epoch_checks,
                    epoch_updates + epoch_failed_updates, epoch_updates, epoch_failed_updates, (float)epoch_timer / 60.0);
            epoch_checks = epoch_updates = epoch_failed_updates = epoch_timer = 0;
        }
        // do it all over again...

        if (update_globalnet_failed) {
            LOGDEBUG("main loop complete: failures detected sleeping %d seconds before next poll\n", 1);
            sleep(1);
        } else {
            LOGDEBUG("main loop complete: sleeping %d seconds before next poll\n", config->cc_polling_frequency);
            sleep(config->cc_polling_frequency);
        }

        epoch_timer += config->cc_polling_frequency;
    }

    //    gni_free(globalnetworkinfo);
    exit(0);
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
int fetch_latest_localconfig(void)
{
    if (isConfigModified(config->configFiles, NUM_EUCANETD_CONFIG) > 0) {   // config modification time has changed
        if (readConfigFile(config->configFiles, NUM_EUCANETD_CONFIG)) {
            // something has changed that can be read in
            LOGINFO("configuration file has been modified, ingressing new options\n");
            logInit();
            // TODO  pick up other NC options dynamically

        }
    }
    return (0);
}

//!
//! daemonize switches user (drop priv), closes FDs, and back-grounds
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
int daemonize(void)
{
    int pid, sid;
    struct passwd *pwent = NULL;
    char pidfile[EUCA_MAX_PATH];
    FILE *FH = NULL;

    if (!config->debug) {
        pid = fork();
        if (pid) {
            exit(0);
        }

        sid = setsid();
        if (sid < 0) {
            perror("daemonize()");
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

    if (!config->debug) {
        close(0);
        close(1);
        close(2);
    }

    return (0);
}

//!
//! application of EBT rules to only allow unique and known IP<->MAC pairings to send traffice through the bridge
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
int update_isolation_rules(void)
{
    int i = 0;
    int rc = 0;
    int ret = 0;
    char cmd[EUCA_MAX_PATH] = "";
    char *strptra = NULL;
    char *strptrb = NULL;
    char *vnetinterface = NULL;
    char *gwip = NULL;
    char *brmac = NULL;
    gni_node *myself = NULL;
    gni_cluster *mycluster = NULL;
    gni_instance *instances = NULL;
    int max_instances = 0;

    LOGDEBUG("updating network isolation rules\n");

    rc = gni_find_self_cluster(globalnetworkinfo, &mycluster);
    if (rc) {
        LOGERROR("cannot find cluster to which local node belongs, in global network view: check network config settings\n");
        return (1);
    }

    rc = ebt_handler_repopulate(config->ebt);

    rc = ebt_table_add_chain(config->ebt, "filter", "EUCA_EBT_FWD", "ACCEPT", "");
    rc = ebt_chain_add_rule(config->ebt, "filter", "FORWARD", "-j EUCA_EBT_FWD");
    rc = ebt_chain_flush(config->ebt, "filter", "EUCA_EBT_FWD");

    rc = ebt_table_add_chain(config->ebt, "nat", "EUCA_EBT_NAT_PRE", "ACCEPT", "");
    rc = ebt_chain_add_rule(config->ebt, "nat", "PREROUTING", "-j EUCA_EBT_NAT_PRE");
    rc = ebt_chain_flush(config->ebt, "nat", "EUCA_EBT_NAT_PRE");

    rc = ebt_table_add_chain(config->ebt, "nat", "EUCA_EBT_NAT_POST", "ACCEPT", "");
    rc = ebt_chain_add_rule(config->ebt, "nat", "POSTROUTING", "-j EUCA_EBT_NAT_POST");
    rc = ebt_chain_flush(config->ebt, "nat", "EUCA_EBT_NAT_POST");

    // add these for DHCP to pass
    //    rc = ebt_chain_add_rule(config->ebt, "filter", "EUCA_EBT_FWD", "-p IPv4 -d Broadcast --ip-proto udp --ip-dport 67:68 -j ACCEPT");
    //    rc = ebt_chain_add_rule(config->ebt, "filter", "EUCA_EBT_FWD", "-p IPv4 -d Broadcast --ip-proto udp --ip-sport 67:68 -j ACCEPT");

    rc = gni_find_self_node(globalnetworkinfo, &myself);
    if (!rc) {
        rc = gni_node_get_instances(globalnetworkinfo, myself, NULL, 0, NULL, 0, &instances, &max_instances);
    }

    for (i = 0; i < max_instances; i++) {
        if (instances[i].privateIp && maczero(instances[i].macAddress)) {
            strptra = strptrb = NULL;
            strptra = hex2dot(instances[i].privateIp);
            hex2mac(instances[i].macAddress, &strptrb);

            // this one is a special case, which only gets identified once the VM is actually running on the hypervisor - need to give it some time to appear
            vnetinterface = mac2interface(strptrb);

            gwip = hex2dot(mycluster->private_subnet.gateway);
            brmac = interface2mac(config->bridgeDev);

            if (strptra && strptrb && vnetinterface && gwip && brmac) {
                if (!config->disable_l2_isolation) {
                    //NOTE: much of this ruleset is a translation of libvirt FW example at http://libvirt.org/firewall.html

                    // PRE Routing

                    // basic MAC check
                    snprintf(cmd, EUCA_MAX_PATH, "-i %s -s ! %s -j DROP", vnetinterface, strptrb);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    // IPv4
                    snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -i %s -s %s --ip-proto udp --ip-dport 67:68 -j ACCEPT", vnetinterface, strptrb);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -i %s --ip-src ! %s -j DROP", vnetinterface, strptra);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -i %s -j ACCEPT", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    // ARP
                    if (config->fake_router) {
                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-ip-dst %s -j arpreply --arpreply-mac %s", vnetinterface, gwip, brmac);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);
                    }

                    snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-mac-src ! %s -j DROP", vnetinterface, strptrb);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-ip-src ! %s -j DROP", vnetinterface, strptra);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-op Request -j ACCEPT", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-op Reply -j ACCEPT", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP -j DROP", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    // RARP
                    snprintf(cmd, EUCA_MAX_PATH, "-i %s -p 0x8035 -s %s -d Broadcast --arp-op Request_Reverse --arp-ip-src 0.0.0.0 --arp-ip-dst 0.0.0.0 --arp-mac-src %s --arp-mac-dst %s -j ACCEPT", vnetinterface, strptrb, strptrb, strptrb);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-i %s -p 0x8035 -j DROP", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    // pass KVM migration weird packet
                    snprintf(cmd, EUCA_MAX_PATH, "-i %s -p 0x835 -j ACCEPT", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    // DROP everything else
                    snprintf(cmd, EUCA_MAX_PATH, "-i %s -j DROP", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                    // POST routing

                    // IPv4
                    snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -o %s -d ! %s --ip-proto udp --ip-dport 67:68 -j DROP", vnetinterface, strptrb);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -o %s -j ACCEPT", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    // ARP
                    snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Reply --arp-mac-dst ! %s -j DROP", vnetinterface, strptrb);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-ip-dst ! %s -j DROP", vnetinterface, strptra);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Request -j ACCEPT", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Request -j ACCEPT", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Reply -j ACCEPT", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s -j DROP", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    // RARP
                    snprintf(cmd, EUCA_MAX_PATH, "-p 0x8035 -o %s -d Broadcast --arp-op Request_Reverse --arp-ip-src 0.0.0.0 --arp-ip-dst 0.0.0.0 --arp-mac-src %s --arp-mac-dst %s -j ACCEPT", vnetinterface, strptrb, strptrb);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    snprintf(cmd, EUCA_MAX_PATH, "-p 0x8035 -o %s -j DROP", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    // DROP everything else
                    snprintf(cmd, EUCA_MAX_PATH, "-o %s -j DROP", vnetinterface);
                    rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);
                    

                } else {
                    if (config->fake_router) {
                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-ip-dst %s -j arpreply --arpreply-mac %s", vnetinterface, gwip, brmac);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);
                    }
                }
            } else {
                LOGWARN("could not retrieve one of: vmip (%s), vminterface (%s), vmmac (%s), gwip (%s), brmac (%s): skipping but will retry\n", SP(strptra), SP(vnetinterface),
                        SP(strptrb), SP(gwip), SP(brmac));
                ret = 1;
            }
            EUCA_FREE(vnetinterface);
            EUCA_FREE(strptra);
            EUCA_FREE(strptrb);
            EUCA_FREE(brmac);
            EUCA_FREE(gwip);
        }
    }

    EUCA_FREE(instances);

    rc = ebt_handler_print(config->ebt);
    rc = ebt_handler_deploy(config->ebt);
    if (rc) {
        LOGERROR("could not install ebtables rules: check above log errors for details\n");
        ret = 1;
    }

    return (ret);
}

//!
//! handle 169.254.169.254 AWS metadata redirect to the CLC
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
int update_metadata_redirect(void)
{
    int ret = 0, rc;
    char rule[1024];
    char *strptra = NULL;

    LOGDEBUG("updating metadata redirect rules\n");

    if (!globalnetworkinfo->enabledCLCIp) {
        LOGWARN("no valid CLC IP has been set yet, skipping metadata redirect update\n");
        return (1);
    }

    if (ipt_handler_repopulate(config->ipt))
        return (1);

    strptra = hex2dot(globalnetworkinfo->enabledCLCIp);
    snprintf(rule, 1024, "-A PREROUTING -d 169.254.169.254/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", strptra);
    rc = ipt_chain_add_rule(config->ipt, "nat", "PREROUTING", rule);
    EUCA_FREE(strptra);

    rc = ipt_handler_deploy(config->ipt);
    if (rc) {
        LOGERROR("could not apply metadata redirect rule '%s': check above log errors for details\n", rule);
        ret = 1;
    }

    return (ret);
}

//!
//! initialize eucanetd config
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
int eucanetdInit(void)
{
    if (!config) {
        config = malloc(sizeof(eucanetdConfig));
        if (!config) {
            LOGFATAL("out of memory\n");
            exit(1);
        }
    }
    bzero(config, sizeof(eucanetdConfig));
    config->cc_polling_frequency = 5;
    config->init = 1;

    /*
       fprintf(stderr, "SIZEOF: %lu\n", sizeof(globalNetworkInfo));
       fprintf(stderr, "SIZEOF: %lu\n", sizeof(gni_cluster));
       fprintf(stderr, "SIZEOF: %lu\n", sizeof(gni_node));
       fprintf(stderr, "SIZEOF: %lu\n", sizeof(gni_instance));
       fprintf(stderr, "SIZEOF: %lu\n", sizeof(gni_secgroup));
     */

    if (!globalnetworkinfo) {
        globalnetworkinfo = gni_init();
        if (!globalnetworkinfo) {
            LOGFATAL("out of memory\n");
            exit(1);
        }
    }

    return (0);
}

//!
//! update IPT
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
int update_sec_groups(void)
{
    int i = 0;
    int j = 0;
    int rc = 0;
    int ret = 0;
    int slashnet = 0;
    char *strptra = NULL;
    char rule[1024] = "";
    gni_cluster *mycluster = NULL;

    LOGDEBUG("updating security group membership and rules\n");

    ret = 0;

    rc = gni_find_self_cluster(globalnetworkinfo, &mycluster);
    if (rc) {
        LOGERROR("cannot find cluster to which local node belongs, in global network view: check network config settings\n");
        return (1);
    }
    // pull in latest IPT state
    rc = ipt_handler_repopulate(config->ipt);
    if (rc) {
        LOGERROR("cannot read current IPT rules: check above log errors for details\n");
        return (1);
    }
    // pull in latest IPS state
    rc = ips_handler_repopulate(config->ips);
    if (rc) {
        LOGERROR("cannot read current IPS sets: check above log errors for details\n");
        return (1);
    }
    // make sure euca chains are in place
    rc = ipt_table_add_chain(config->ipt, "filter", "EUCA_FILTER_FWD", "-", "[0:0]");
    rc = ipt_table_add_chain(config->ipt, "filter", "EUCA_COUNTERS_IN", "-", "[0:0]");
    rc = ipt_table_add_chain(config->ipt, "filter", "EUCA_COUNTERS_OUT", "-", "[0:0]");
    rc = ipt_chain_add_rule(config->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD");

    // clear all chains that we're about to (re)populate with latest network metadata
    rc = ipt_table_deletechainmatch(config->ipt, "filter", "EU_");
    rc = ipt_chain_flush(config->ipt, "filter", "EUCA_FILTER_FWD");
    rc = ipt_chain_add_rule(config->ipt, "filter", "EUCA_FILTER_FWD", "-A EUCA_FILTER_FWD -j EUCA_COUNTERS_IN");
    rc = ipt_chain_add_rule(config->ipt, "filter", "EUCA_FILTER_FWD", "-A EUCA_FILTER_FWD -j EUCA_COUNTERS_OUT");
    rc = ipt_chain_add_rule(config->ipt, "filter", "EUCA_FILTER_FWD", "-A EUCA_FILTER_FWD -m conntrack --ctstate ESTABLISHED -j ACCEPT");
    rc = ipt_chain_flush(config->ipt, "filter", "EUCA_COUNTERS_IN");
    rc = ipt_chain_flush(config->ipt, "filter", "EUCA_COUNTERS_OUT");

    // reset and create ipsets for allprivate and noneuca subnet sets
    rc = ips_handler_deletesetmatch(config->ips, "EU_");

    ips_handler_add_set(config->ips, "EUCA_ALLPRIVATE");
    ips_set_flush(config->ips, "EUCA_ALLPRIVATE");
    ips_set_add_net(config->ips, "EUCA_ALLPRIVATE", "127.0.0.1", 32);

    ips_handler_add_set(config->ips, "EUCA_ALLNONEUCA");
    ips_set_flush(config->ips, "EUCA_ALLNONEUCA");
    ips_set_add_net(config->ips, "EUCA_ALLNONEUCA", "127.0.0.1", 32);

    // add addition of private non-euca subnets to EUCA_ALLPRIVATE, here

    for (i = 0; i < globalnetworkinfo->max_subnets; i++) {
        strptra = hex2dot(globalnetworkinfo->subnets[i].subnet);
        //        strptrb = hex2dot(globalnetworkinfo->subnets[i].netmask);
        slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - globalnetworkinfo->subnets[i].netmask) + 1))));
        ips_set_add_net(config->ips, "EUCA_ALLNONEUCA", strptra, slashnet);
        EUCA_FREE(strptra);
    }

    // add chains/rules
    for (i = 0; i < globalnetworkinfo->max_secgroups; i++) {
        char *chainname = NULL;
        gni_secgroup *secgroup = NULL;
        gni_instance *instances = NULL;
        int max_instances;

        secgroup = &(globalnetworkinfo->secgroups[i]);
        rule[0] = '\0';
        rc = gni_secgroup_get_chainname(globalnetworkinfo, secgroup, &chainname);
        if (rc) {
            LOGERROR("cannot get chain name from security group: check above log errors for details\n");
            ret = 1;
        } else {

            ips_handler_add_set(config->ips, chainname);
            ips_set_flush(config->ips, chainname);

            strptra = hex2dot(mycluster->private_subnet.gateway);
            ips_set_add_ip(config->ips, chainname, strptra);
            ips_set_add_ip(config->ips, "EUCA_ALLPRIVATE", strptra);
            EUCA_FREE(strptra);

            rc = gni_secgroup_get_instances(globalnetworkinfo, secgroup, NULL, 0, NULL, 0, &instances, &max_instances);

            for (j = 0; j < max_instances; j++) {
                if (instances[j].privateIp) {
                    strptra = hex2dot(instances[j].privateIp);
                    ips_set_add_ip(config->ips, chainname, strptra);
                    ips_set_add_ip(config->ips, "EUCA_ALLPRIVATE", strptra);
                    EUCA_FREE(strptra);
                }
                if (instances[j].publicIp) {
                    strptra = hex2dot(instances[j].publicIp);
                    ips_set_add_ip(config->ips, chainname, strptra);
                    EUCA_FREE(strptra);
                }
            }

            EUCA_FREE(instances);

            // add forward chain
            ipt_table_add_chain(config->ipt, "filter", chainname, "-", "[0:0]");
            ipt_chain_flush(config->ipt, "filter", chainname);

            // add jump rule
            snprintf(rule, 1024, "-A EUCA_FILTER_FWD -m set --match-set %s dst -j %s", chainname, chainname);
            ipt_chain_add_rule(config->ipt, "filter", "EUCA_FILTER_FWD", rule);

            // populate forward chain

            // this one needs to be first
            snprintf(rule, 1024, "-A %s -m set --match-set %s src,dst -j ACCEPT", chainname, chainname);
            ipt_chain_add_rule(config->ipt, "filter", chainname, rule);
            // make sure conntrack rule is in place
            snprintf(rule, 1024, "-A %s -m conntrack --ctstate ESTABLISHED -j ACCEPT", chainname);
            ipt_chain_add_rule(config->ipt, "filter", chainname, rule);

            // then put all the group specific IPT rules (temporary one here)
            if (secgroup->max_grouprules) {
                for (j = 0; j < secgroup->max_grouprules; j++) {
                    snprintf(rule, 1024, "-A %s %s -j ACCEPT", chainname, secgroup->grouprules[j].name);
                    ipt_chain_add_rule(config->ipt, "filter", chainname, rule);
                }
            }
            // this ones needs to be last: DAN removed in lieu of new method (DROP after all FWD chains have been tried)
            //            snprintf(rule, 1024, "-A %s -j DROP", chainname);
            //            ipt_chain_add_rule(config->ipt, "filter", chainname, rule);

            EUCA_FREE(chainname);
        }
    }

    // last rule in place is to DROP if no accepts have made it past the FWD chains, and the dst IP is in the ALLPRIVATE ipset
    snprintf(rule, 1024, "-A EUCA_FILTER_FWD -m set --match-set EUCA_ALLPRIVATE dst -j DROP");
    ipt_chain_add_rule(config->ipt, "filter", "EUCA_FILTER_FWD", rule);

    if (1 || !ret) {
        ips_handler_print(config->ips);
        rc = ips_handler_deploy(config->ips, 0);
        if (rc) {
            LOGERROR("could not apply ipsets: check above log errors for details\n");
            ret = 1;
        }
    }

    if (1 || !ret) {
        ipt_handler_print(config->ipt);
        rc = ipt_handler_deploy(config->ipt);
        if (rc) {
            LOGERROR("could not apply new rules: check above log errors for details\n");
            ret = 1;
        }
    }

    if (1 || !ret) {
        ips_handler_print(config->ips);
        rc = ips_handler_deploy(config->ips, 1);
        if (rc) {
            LOGERROR("could not apply ipsets: check above log errors for details\n");
            ret = 1;
        }
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
int update_public_ips(void)
{
    int slashnet = 0, ret = 0, rc = 0, i = 0, j = 0, foundidx = 0;
    char cmd[EUCA_MAX_PATH], rule[1024];
    char *strptra = NULL, *strptrb = NULL;
    sequence_executor cmds;
    gni_cluster *mycluster = NULL;
    gni_node *myself = NULL;
    gni_instance *instances = NULL;
    int max_instances = 0;
    u32 nw, nm;

    LOGDEBUG("updating public IP to private IP mappings\n");

    rc = gni_find_self_cluster(globalnetworkinfo, &mycluster);
    if (rc) {
        LOGERROR("cannot locate cluster to which local node belongs, in global network view: check network config settings\n");
        return (1);
    }

    nw = mycluster->private_subnet.subnet;
    nm = mycluster->private_subnet.netmask;

    //    slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - vnetconfig->networks[0].nm) + 1))));
    slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - nm) + 1))));

    // install EL IP addrs and NAT rules
    rc = ipt_handler_repopulate(config->ipt);
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_PRE", "-", "[0:0]");
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_POST", "-", "[0:0]");
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_OUT", "-", "[0:0]");

    ipt_chain_add_rule(config->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE");
    ipt_chain_add_rule(config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST");
    ipt_chain_add_rule(config->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT");

    //  rc = ipt_handler_print(config->ipt);
    rc = ipt_handler_deploy(config->ipt);
    if (rc) {
        LOGERROR("could not add euca net chains: check above log errors for details\n");
        ret = 1;
    }

    rc = ipt_handler_repopulate(config->ipt);
    rc = se_init(&cmds, config->cmdprefix, 2, 1);

    ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_PRE");
    ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_POST");
    ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_OUT");

    //    strptra = hex2dot(vnetconfig->networks[0].nw);
    strptra = hex2dot(nw);

    //    snprintf(rule, 1024, "-A EUCA_NAT_PRE -s %s/%d -d %s/%d -j MARK --set-xmark 0x2a/0xffffffff", strptra, slashnet, strptra, slashnet);
    snprintf(rule, 1024, "-A EUCA_NAT_PRE -s %s/%d -m set --match-set EUCA_ALLPRIVATE dst -j MARK --set-xmark 0x2a/0xffffffff", strptra, slashnet);
    ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_PRE", rule);

    snprintf(rule, 1024, "-A EUCA_NAT_PRE -s %s/%d -m set --match-set EUCA_ALLNONEUCA dst -j MARK --set-xmark 0x2a/0xffffffff", strptra, slashnet);
    ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_PRE", rule);

    snprintf(rule, 1024, "-A EUCA_COUNTERS_IN -d %s/%d", strptra, slashnet);
    ipt_chain_add_rule(config->ipt, "filter", "EUCA_COUNTERS_IN", rule);

    snprintf(rule, 1024, "-A EUCA_COUNTERS_OUT -s %s/%d", strptra, slashnet);
    ipt_chain_add_rule(config->ipt, "filter", "EUCA_COUNTERS_OUT", rule);

    EUCA_FREE(strptra);

    rc = gni_find_self_node(globalnetworkinfo, &myself);
    if (!rc) {
        rc = gni_node_get_instances(globalnetworkinfo, myself, NULL, 0, NULL, 0, &instances, &max_instances);
    }

    for (i = 0; i < max_instances; i++) {
        strptra = hex2dot(instances[i].publicIp);
        strptrb = hex2dot(instances[i].privateIp);
        LOGTRACE("instance pub/priv: %s: %s/%s\n", instances[i].name, strptra, strptrb);
        if ((instances[i].publicIp && instances[i].privateIp) && (instances[i].publicIp != instances[i].privateIp)) {
            snprintf(cmd, EUCA_MAX_PATH, "ip addr add %s/%d dev %s >/dev/null 2>&1", strptra, 32, config->pubInterface);
            rc = se_add(&cmds, cmd, NULL, ignore_exit2);

            snprintf(cmd, EUCA_MAX_PATH, "arping -c 5 -w 1 -U -I %s %s >/dev/null 2>&1 &", config->pubInterface, strptra);
            rc = se_add(&cmds, cmd, NULL, ignore_exit);

            snprintf(rule, 1024, "-A EUCA_NAT_PRE -d %s/32 -j DNAT --to-destination %s", strptra, strptrb);
            rc = ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_PRE", rule);

            snprintf(rule, 1024, "-A EUCA_NAT_OUT -d %s/32 -j DNAT --to-destination %s", strptra, strptrb);
            rc = ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_OUT", rule);

            snprintf(rule, 1024, "-A EUCA_NAT_POST -s %s/32 -m mark ! --mark 0x2a -j SNAT --to-source %s", strptrb, strptra);
            rc = ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_POST", rule);

            //snprintf(rule, 1024, "-A EUCA_COUNTERS_IN -m conntrack --ctstate DNAT --ctorigdst %s/32", strptra);
            snprintf(rule, 1024, "-A EUCA_COUNTERS_IN -d %s/32", strptrb);
            rc = ipt_chain_add_rule(config->ipt, "filter", "EUCA_COUNTERS_IN", rule);

            //snprintf(rule, 1024, "-A EUCA_COUNTERS_OUT -m conntrack --ctstate SNAT --ctrepldst %s/32", strptra);
            snprintf(rule, 1024, "-A EUCA_COUNTERS_OUT -s %s/32", strptrb);
            rc = ipt_chain_add_rule(config->ipt, "filter", "EUCA_COUNTERS_OUT", rule);

        }

        EUCA_FREE(strptra);
        EUCA_FREE(strptrb);
    }
    EUCA_FREE(instances);

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not execute command sequence (check above log errors for details): adding ips, sending arpings\n");
        ret = 1;
    }
    se_free(&cmds);

    //  rc = ipt_handler_print(config->ipt);
    rc = ipt_handler_deploy(config->ipt);
    if (rc) {
        LOGERROR("could not apply new ipt handler rules: check above log errors for details\n");
        ret = 1;
    }
    // if all has gone well, now clear any public IPs that have not been mapped to private IPs
    if (!ret) {
        se_init(&cmds, config->cmdprefix, 2, 1);
        for (i = 0; i < globalnetworkinfo->max_public_ips; i++) {
            int found = 0;

            foundidx = 0;
            for (j = 0; j < globalnetworkinfo->max_instances && !found; j++) {
                if (globalnetworkinfo->instances[j].publicIp == globalnetworkinfo->public_ips[i]) {
                    found = 1;
                    foundidx = i;
                }
            }

            if (!found) {
                strptra = hex2dot(globalnetworkinfo->public_ips[i]);
                snprintf(cmd, EUCA_MAX_PATH, "ip addr del %s/%d dev %s >/dev/null 2>&1", strptra, 32, config->pubInterface);
                rc = se_add(&cmds, cmd, NULL, ignore_exit2);
                EUCA_FREE(strptra);
            }
        }

        se_print(&cmds);
        rc = se_execute(&cmds);
        if (rc) {
            LOGERROR("could not execute command sequence (check above log errors for details): revoking no longer in use ips\n");
            ret = 1;
        }
        se_free(&cmds);
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
int update_private_ips(void)
{
    int rc = 0, ret = 0;

    LOGDEBUG("updating private IP and DHCPD handling\n");

    rc = kick_dhcpd_server();
    if (rc) {
        LOGERROR("unable to (re)configure local dhcpd server: check above log errors for details\n");
        ret = 1;
    }

    return (ret);
}

int kick_dhcpd_server()
{
    int ret = 0;
    int rc = 0;
    int pid = 0;
    char *pidstr = NULL;
    char pidfile[EUCA_MAX_PATH] = "";
    char configfile[EUCA_MAX_PATH] = "";
    char leasefile[EUCA_MAX_PATH] = "";
    char tracefile[EUCA_MAX_PATH] = "";
    char rootwrap[EUCA_MAX_PATH] = "";
    char cmd[EUCA_MAX_PATH] = "";
    struct stat mystat = { 0 };

    rc = generate_dhcpd_config();
    if (rc) {
        LOGERROR("unable to generate new dhcp configuration file: check above log errors for details\n");
        ret = 1;
    } else if (stat(config->dhcpDaemon, &mystat) != 0) {
        LOGERROR("unable to find DHCP daemon binaries: '%s'\n", config->dhcpDaemon);
        ret = 1;
    } else {
        snprintf(pidfile, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.pid", config->eucahome);
        snprintf(leasefile, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.leases", config->eucahome);
        snprintf(tracefile, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.trace", config->eucahome);
        snprintf(configfile, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.conf", config->eucahome);
        snprintf(rootwrap, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, config->eucahome);

        if (stat(pidfile, &mystat) == 0) {
            pidstr = file2str(pidfile);
            pid = atoi(pidstr);
            EUCA_FREE(pidstr);

            if (pid > 1) {
                LOGDEBUG("attempting to kill old dhcp daemon (pid=%d)\n", pid);
                if ((rc = safekillfile(pidfile, config->dhcpDaemon, 9, rootwrap)) != 0) {
                    LOGWARN("failed to kill previous dhcp daemon\n");
                }
            }
        }

        if (stat(leasefile, &mystat) != 0) {
            LOGDEBUG("creating stub lease file (%s)\n", leasefile);
            rc = touch(leasefile);
            if (rc) {
                LOGWARN("cannot create empty leasefile\n");
            }
        }

        snprintf(cmd, EUCA_MAX_PATH, "%s %s -cf %s -lf %s -pf %s -tf %s", rootwrap, config->dhcpDaemon, configfile, leasefile, pidfile, tracefile);
        LOGDEBUG("running command (%s)\n", cmd);
        rc = system(cmd);
        if (rc) {
            LOGERROR("command failed: exitcode='%d' command='%s'\n", rc, cmd);
            ret = 1;
        } else {
            LOGDEBUG("dhcpd server restart command (%s) succeeded\n", cmd);
        }
    }

    return (ret);
}

int generate_dhcpd_config()
{
    int ret = 0, rc = 0, i;
    gni_node *myself = NULL;
    gni_cluster *mycluster = NULL;
    gni_instance *instances = NULL;
    int max_instances = 0;
    char dhcpd_config_path[EUCA_MAX_PATH];
    FILE *OFH = NULL;
    u32 nw, nm, rt;
    char *network = NULL, *netmask = NULL, *broadcast = NULL, *router = NULL, *strptra = NULL;

    rc = gni_find_self_cluster(globalnetworkinfo, &mycluster);
    if (rc) {
        LOGERROR("cannot find the cluster to which the local node belongs: check network config settings\n");
        return (1);
    }

    rc = gni_find_self_node(globalnetworkinfo, &myself);
    if (rc) {
        LOGERROR("cannot find local node in global network state: check network config settings\n");
        return (1);
    }

    rc = gni_node_get_instances(globalnetworkinfo, myself, NULL, 0, NULL, 0, &instances, &max_instances);
    if (rc) {
        LOGERROR("cannot find instances belonging to this node: check network config settings\n");
        return (1);
    }

    nw = mycluster->private_subnet.subnet;
    nm = mycluster->private_subnet.netmask;
    rt = mycluster->private_subnet.gateway;

    snprintf(dhcpd_config_path, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.conf", config->eucahome);
    OFH = fopen(dhcpd_config_path, "w");
    if (!OFH) {
        LOGERROR("cannot open dhcpd server config file for write '%s': check permissions\n", dhcpd_config_path);
        ret = 1;
    } else {

        fprintf(OFH, "# automatically generated config file for DHCP server\ndefault-lease-time 86400;\nmax-lease-time 86400;\nddns-update-style none;\n\n");
        fprintf(OFH, "shared-network euca {\n");

        network = hex2dot(nw);
        netmask = hex2dot(nm);
        broadcast = hex2dot(nw | ~nm);
        router = hex2dot(rt);

        fprintf(OFH, "subnet %s netmask %s {\n  option subnet-mask %s;\n  option broadcast-address %s;\n", network, netmask, netmask, broadcast);
        if (strlen(globalnetworkinfo->instanceDNSDomain)) {
            fprintf(OFH, "  option domain-name \"%s\";\n", globalnetworkinfo->instanceDNSDomain);
        }
        if (globalnetworkinfo->max_instanceDNSServers) {
            strptra = hex2dot(globalnetworkinfo->instanceDNSServers[0]);
            fprintf(OFH, "  option domain-name-servers %s", SP(strptra));
            EUCA_FREE(strptra);
            for (i = 1; i < globalnetworkinfo->max_instanceDNSServers; i++) {
                strptra = hex2dot(globalnetworkinfo->instanceDNSServers[i]);
                fprintf(OFH, ", %s", SP(strptra));
                EUCA_FREE(strptra);
            }
            fprintf(OFH, ";\n");
        } else {
            fprintf(OFH, "  option domain-name-servers 8.8.8.8;\n");
        }
        fprintf(OFH, "  option routers %s;\n}\n", router);

        EUCA_FREE(network);
        EUCA_FREE(netmask);
        EUCA_FREE(broadcast);
        EUCA_FREE(router);

        for (i = 0; i < max_instances; i++) {
            char *mac = NULL;
            char *ip = NULL;
            hex2mac(instances[i].macAddress, &mac);
            ip = hex2dot(instances[i].privateIp);
            fprintf(OFH, "\nhost node-%s {\n  hardware ethernet %s;\n  fixed-address %s;\n}\n", ip, mac, ip);
            EUCA_FREE(mac);
            EUCA_FREE(ip);
        }

        fprintf(OFH, "}\n");
        fclose(OFH);
    }

    EUCA_FREE(instances);

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
int read_config_bootstrap(void)
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

    if (!config->debug) {
        snprintf(logfile, EUCA_MAX_PATH, "%s/var/log/eucalyptus/eucanetd.log", config->eucahome);
        log_file_set(logfile);
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
    } else {
        log_params_set(EUCA_LOG_TRACE, 0, 100000);
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
int read_config(void)
{
    int i = 0;
    int rc = 0;
    int ret = 0;
    int to_update = 0;
    char *tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME);
    char home[EUCA_MAX_PATH] = "";
    char netPath[EUCA_MAX_PATH] = "";
    char destfile[EUCA_MAX_PATH] = "";
    char sourceuri[EUCA_MAX_PATH] = "";
    char eucadir[EUCA_MAX_PATH] = "";
    char *cvals[EUCANETD_CVAL_LAST] = { NULL };
    gni_cluster *mycluster = NULL;

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
    cvals[EUCANETD_CVAL_CC_POLLING_FREQUENCY] = configFileValue("CC_POLLING_FREQUENCY");
    cvals[EUCANETD_CVAL_DISABLE_L2_ISOLATION] = configFileValue("DISABLE_L2_ISOLATION");
    cvals[EUCANETD_CVAL_FAKE_ROUTER] = configFileValue("FAKE_ROUTER");

    //    temporary();

    // initialize and populate data from global_network_info.xml file
    snprintf(destfile, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/eucanetd_global_network_info.xml", home);
    snprintf(sourceuri, EUCA_MAX_PATH, "file://" EUCALYPTUS_STATE_DIR "/global_network_info.xml", home);
    atomic_file_init(&(config->global_network_info_file), sourceuri, destfile, 0);

    rc = atomic_file_get(&(config->global_network_info_file), &to_update);
    if (rc) {
        LOGWARN("cannot get latest global network info file (%s)\n", config->global_network_info_file.dest);
        for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
            EUCA_FREE(cvals[i]);
        }
        return (1);
    }

    rc = gni_populate(globalnetworkinfo, config->global_network_info_file.dest);
    if (rc) {
        LOGERROR("could not initialize global network info data structures from XML input\n");
        for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
            EUCA_FREE(cvals[i]);
        }
        return (1);
    }
    rc = gni_print(globalnetworkinfo);

    rc = gni_find_self_cluster(globalnetworkinfo, &mycluster);
    if (rc) {
        LOGERROR("cannot locate cluster to which local node belongs in global network view: check network config settings\n");
        for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
            EUCA_FREE(cvals[i]);
        }
        return (1);
    }

    EUCA_FREE(config->eucahome);
    config->eucahome = strdup(cvals[EUCANETD_CVAL_EUCAHOME]);
    EUCA_FREE(config->eucauser);
    config->eucauser = strdup(cvals[EUCANETD_CVAL_EUCA_USER]);
    snprintf(config->cmdprefix, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, config->eucahome);
    config->cc_polling_frequency = atoi(cvals[EUCANETD_CVAL_CC_POLLING_FREQUENCY]);
    if (!strcmp(cvals[EUCANETD_CVAL_DISABLE_L2_ISOLATION], "Y")) {
        config->disable_l2_isolation = 1;
    } else {
        config->disable_l2_isolation = 0;
    }
    if (!strcmp(cvals[EUCANETD_CVAL_FAKE_ROUTER], "Y")) {
        config->fake_router = 1;
    } else {
        config->fake_router = 0;
    }
    snprintf(config->pubInterface, 32, "%s", cvals[EUCANETD_CVAL_PUBINTERFACE]);
    snprintf(config->privInterface, 32, "%s", cvals[EUCANETD_CVAL_PRIVINTERFACE]);
    snprintf(config->bridgeDev, 32, "%s", cvals[EUCANETD_CVAL_BRIDGE]);
    snprintf(config->dhcpDaemon, EUCA_MAX_PATH, "%s", cvals[EUCANETD_CVAL_DHCPDAEMON]);

    //    config->defaultgw = dot2hex(cvals[EUCANETD_CVAL_ROUTER]);

    LOGDEBUG
        ("required variables read from local config file: EUCALYPTUS=%s EUCA_USER=%s VNET_MODE=%s VNET_PUBINTERFACE=%s VNET_PRIVINTERFACE=%s VNET_BRIDGE=%s VNET_DHCPDAEMON=%s\n",
         SP(cvals[EUCANETD_CVAL_EUCAHOME]), SP(cvals[EUCANETD_CVAL_EUCA_USER]), SP(cvals[EUCANETD_CVAL_MODE]), SP(cvals[EUCANETD_CVAL_PUBINTERFACE]),
         SP(cvals[EUCANETD_CVAL_PRIVINTERFACE]), SP(cvals[EUCANETD_CVAL_BRIDGE]), SP(cvals[EUCANETD_CVAL_DHCPDAEMON]));

    rc = logInit();
    if (rc) {
        LOGERROR("unable to initialize logging subsystem: check permissions and log config options\n");
        ret = 1;
    }

    /*
       rc = vnetInit(vnetconfig, cvals[EUCANETD_CVAL_MODE], cvals[EUCANETD_CVAL_EUCAHOME], netPath, CLC, cvals[EUCANETD_CVAL_PUBINTERFACE], cvals[EUCANETD_CVAL_PRIVINTERFACE],
       cvals[EUCANETD_CVAL_ADDRSPERNET], cvals[EUCANETD_CVAL_SUBNET], cvals[EUCANETD_CVAL_NETMASK], cvals[EUCANETD_CVAL_BROADCAST], cvals[EUCANETD_CVAL_DNS],
       cvals[EUCANETD_CVAL_DOMAINNAME], cvals[EUCANETD_CVAL_ROUTER], cvals[EUCANETD_CVAL_DHCPDAEMON], cvals[EUCANETD_CVAL_DHCPUSER], cvals[EUCANETD_CVAL_BRIDGE], NULL,
       cvals[EUCANETD_CVAL_MACPREFIX]);

       if (rc) {
       LOGERROR("unable to initialize vnetwork subsystem\n");
       ret = 1;
       }
     */

    config->ipt = malloc(sizeof(ipt_handler));
    if (!config->ipt) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }
    rc = ipt_handler_init(config->ipt, config->cmdprefix);
    if (rc) {
        LOGERROR("could not initialize ipt_handler: check above log errors for details\n");
        ret = 1;
    }

    config->ips = malloc(sizeof(ips_handler));
    if (!config->ips) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }
    rc = ips_handler_init(config->ips, config->cmdprefix);
    if (rc) {
        LOGERROR("could not initialize ips_handler: check above log errors for details\n");
        ret = 1;
    }

    config->ebt = malloc(sizeof(ebt_handler));
    if (!config->ebt) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }
    rc = ebt_handler_init(config->ebt, config->cmdprefix);
    if (rc) {
        LOGERROR("could not initialize ebt_handler: check above log errors for details\n");
        ret = 1;
    }

    for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
        EUCA_FREE(cvals[i]);
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
int logInit(void)
{
    int ret = 0;
    int log_level = 0;
    int log_roll_number = 0;
    long log_max_size_bytes = 0;
    char *log_prefix = NULL;
    char logfile[EUCA_MAX_PATH] = "";

    if (!config->debug) {
        snprintf(logfile, EUCA_MAX_PATH, "%s/var/log/eucalyptus/eucanetd.log", config->eucahome);
        log_file_set(logfile);

        configReadLogParams(&log_level, &log_roll_number, &log_max_size_bytes, &log_prefix);

        log_params_set(log_level, log_roll_number, log_max_size_bytes);
        log_prefix_set(log_prefix);
        EUCA_FREE(log_prefix);
    } else {
        log_params_set(EUCA_LOG_TRACE, 0, 100000);
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
int fetch_latest_network(int *update_globalnet)
{
    int rc = 0, ret = 0;

    LOGDEBUG("fetching latest network view\n");

    if (!update_globalnet) {
        LOGERROR("BUG: input contains null pointers\n");
        return (1);
    }
    // don't run any updates unless something new has happened
    *update_globalnet = 0;

    rc = fetch_latest_localconfig();
    if (rc) {
        LOGWARN("cannot read in changes to local configuration file: check local eucalyptus.conf\n");
    }
    // get latest networking data from eucalyptus, set update flags if content has changed
    rc = fetch_latest_euca_network(update_globalnet);
    if (rc) {
        LOGWARN("cannot get latest network topology, configuration and/or local VM network from CC/NC: check that CC and NC are running\n");
        ret = 1;
    }

    return (ret);
}

int fetch_latest_euca_network(int *update_globalnet)
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
int read_latest_network(void)
{
    int rc, ret = 0;

    LOGDEBUG("reading latest network view into eucanetd\n");

    rc = gni_populate(globalnetworkinfo, config->global_network_info_file.dest);
    if (rc) {
        LOGERROR("failed to initialize global network info data structures from XML file: check network config settings\n");
        ret = 1;
    } else {
        rc = gni_print(globalnetworkinfo);
    }
    return (ret);
}

//!
//! Function description.
//!
//! @param[in] rc
//! @param[in] o
//! @param[in] e
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
int check_stderr_already_exists(int rc, char *o, char *e)
{
    if (!rc)
        return (0);
    if (e && strstr(e, "already exists"))
        return (0);
    return (1);
}

//!
//! Function description.
//!
//! @param[in] mac
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
char *mac2interface(char *mac)
{
    int rc = 0;
    int match;
    char *ret = NULL;
    char *strptra = NULL;
    char *strptrb = NULL;
    char macstr[64] = "";
    char mac_file[EUCA_MAX_PATH] = "";
    DIR *DH = NULL;
    FILE *FH = NULL;
    struct dirent dent = { 0 };
    struct dirent *result = NULL;

    if (!mac) {
        return (NULL);
    }

    DH = opendir("/sys/class/net/");
    if (DH) {
        rc = readdir_r(DH, &dent, &result);
        match = 0;
        while (!match && !rc && result) {
            if (strcmp(result->d_name, ".") && strcmp(result->d_name, "..")) {
                snprintf(mac_file, EUCA_MAX_PATH, "/sys/class/net/%s/address", result->d_name);
                FH = fopen(mac_file, "r");
                if (FH) {
                    macstr[0] = '\0';
                    rc = fscanf(FH, "%s", macstr);
                    if (strlen(macstr)) {
                        strptra = strchr(macstr, ':');
                        strptrb = strchr(mac, ':');
                        if (strptra && strptrb) {
                            if (!strcasecmp(strptra, strptrb)) {
                                ret = strdup(result->d_name);
                                match++;
                            }
                        } else {
                            LOGERROR("BUG: parse error extracting mac (malformed) from sys interface file: file=%s macstr=%s\n", SP(mac_file), SP(macstr));
                            ret = NULL;
                        }
                    } else {
                        LOGERROR("BUG: parse error extracting mac from sys interface file: file=%s fscanf_rc=%d\n", SP(mac_file), rc);
                        ret = NULL;
                    }
                    fclose(FH);
                } else {
                    LOGERROR("could not open sys interface file for read '%s': check permissions\n", SP(mac_file));
                    ret = NULL;
                }
            }
            rc = readdir_r(DH, &dent, &result);
        }
        closedir(DH);
    } else {
        LOGERROR("could not open sys dir for read '/sys/class/net/': check permissions\n");
        ret = NULL;
    }
    return (ret);
}

//!
//! Function description.
//!
//! @param[in] dev
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
char *interface2mac(char *dev)
{
    char *ret = NULL;
    char devpath[EUCA_MAX_PATH] = "";

    if (!dev) {
        return (NULL);
    }

    snprintf(devpath, EUCA_MAX_PATH, "/sys/class/net/%s/address", dev);
    ret = file2str(devpath);

    return (ret);
}

int temporary()
{
    atomic_file nc_localnet_file, cc_config_file, cc_networktopo_file;
    char destfile[EUCA_MAX_PATH], sourceuri[EUCA_MAX_PATH], ccIp[32], clcIp[32], buf[1024];
    int to_update = 0, rc = 0;
    FILE *FH = NULL;

    snprintf(destfile, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/eucanetd_nc_local_net", config->eucahome);
    snprintf(sourceuri, EUCA_MAX_PATH, "file://" EUCALYPTUS_LOG_DIR "/local-net", config->eucahome);
    atomic_file_init(&nc_localnet_file, sourceuri, destfile, 0);
    rc = atomic_file_get(&nc_localnet_file, &to_update);
    if (rc) {
        LOGWARN("cannot get latest nc local-net file (%s)\n", nc_localnet_file.dest);
        return (1);
    }

    FH = fopen(nc_localnet_file.dest, "r");
    if (FH) {
        while (fgets(buf, 1024, FH)) {
            LOGTRACE("line: %s\n", SP(buf));
            if (strstr(buf, "CCIP=")) {
                sscanf(buf, "CCIP=%[0-9.]", ccIp);
                LOGDEBUG("parsed line from file(%s): ccIp=%s\n", SP(nc_localnet_file.dest), ccIp);
                if (strlen(ccIp) && strcmp(ccIp, "0.0.0.0")) {
                } else {
                    LOGWARN("malformed ccIp entry in file, skipping: %s\n", SP(buf));
                }
            } else if (strstr(buf, "CLCIP=")) {
                sscanf(buf, "CLCIP=%[0-9.]", clcIp);
                LOGDEBUG("parsed line from file(%s): clcIp=%s\n", SP(nc_localnet_file.dest), clcIp);
                if (strlen(clcIp) && strcmp(clcIp, "0.0.0.0")) {
                } else {
                    LOGWARN("malformed clcIp entry in file, skipping: %s\n", SP(buf));
                }
            }
        }
    }
    fclose(FH);

    snprintf(destfile, EUCA_MAX_PATH, "%s/var/lib/eucalyptus/eucanetd_cc_config_file", config->eucahome);
    snprintf(sourceuri, EUCA_MAX_PATH, "http://%s:8776/config-cc", SP(ccIp));
    atomic_file_init(&cc_config_file, sourceuri, destfile, 1);

    rc = atomic_file_get(&cc_config_file, &to_update);
    if (rc) {
        LOGWARN("cannot fetch config file from CC (%s)\n", ccIp);
        return (1);
    }

    snprintf(destfile, EUCA_MAX_PATH, "%s/var/lib/eucalyptus/eucanetd_cc_network_topology_file", config->eucahome);
    snprintf(sourceuri, EUCA_MAX_PATH, "http://%s:8776/network-topology", SP(ccIp));
    atomic_file_init(&cc_networktopo_file, sourceuri, destfile, 1);

    rc = atomic_file_get(&cc_networktopo_file, &to_update);
    if (rc) {
        LOGWARN("cannot fetch networktopo file from CC (%s)\n", ccIp);
        return (1);
    }

    LOGTRACE("converting old-style network view files into XML\n");

    printf("<network-data>\n<configuration>\n");

    printf("<property name=\"enabledCLCIp\">\n<value>%s</value>\n</property>\n", clcIp);
    printf("<property name=\"instanceDNSDomain\">\n<value>%s</value>\n</property>\n", "eucalyptus.internal");
    printf("<property name=\"instanceDNSServers\">\n<value>%s</value>\n</property>\n", "10.1.1.254");

    FH = fopen(cc_config_file.dest, "r");
    if (FH) {
        printf("<property name=\"publicIps\">\n");
        while (fgets(buf, 1024, FH)) {
            char tmp[1024];
            //            LOGTRACE("line: %s\n", SP(buf));
            if (strstr(buf, "IPMAP=")) {
                sscanf(buf, "IPMAP=%[0-9.]", tmp);
                //                LOGDEBUG("parsed line from file(%s): IPMAP==%s\n", SP(cc_config_file.dest), tmp);
                if (strlen(tmp) && strcmp(tmp, "0.0.0.0")) {
                    printf("<value>%s</value>\n", tmp);
                } else {
                    LOGWARN("malformed IPMAP entry in file, skipping: %s\n", SP(buf));
                }
            }
        }
        printf("</property>\n");
    }
    fclose(FH);

    printf("<property name=\"subnets\">\n");
    FH = fopen(cc_config_file.dest, "r");
    if (FH) {

        while (fgets(buf, 1024, FH)) {
            char tmp[1024];
            if (strstr(buf, "VNET_SUBNET=")) {
                sscanf(buf, "VNET_SUBNET=%[0-9.]", tmp);
                if (strlen(tmp) && strcmp(tmp, "0.0.0.0")) {
                    printf("<subnet name=\"%s\">\n", tmp);
                }
            }
        }

    }
    fclose(FH);

    FH = fopen(cc_config_file.dest, "r");
    if (FH) {

        while (fgets(buf, 1024, FH)) {
            char tmp[1024];
            if (strstr(buf, "VNET_SUBNET=")) {
                sscanf(buf, "VNET_SUBNET=%[0-9.]", tmp);
                if (strlen(tmp) && strcmp(tmp, "0.0.0.0")) {
                    printf("<property name=\"subnet\">\n<value>%s</value>\n</property>\n", tmp);
                }
            } else if (strstr(buf, "VNET_NETMASK=")) {
                sscanf(buf, "VNET_NETMASK=%[0-9.]", tmp);
                if (strlen(tmp) && strcmp(tmp, "0.0.0.0")) {
                    printf("<property name=\"netmask\">\n<value>%s</value>\n</property>\n", tmp);
                }
            } else if (strstr(buf, "VNET_ROUTER=")) {
                sscanf(buf, "VNET_ROUTER=%[0-9.]", tmp);
                if (strlen(tmp) && strcmp(tmp, "0.0.0.0")) {
                    printf("<property name=\"gateway\">\n<value>%s</value>\n</property>\n", tmp);
                }
            }
        }

    }
    fclose(FH);

    printf("</subnet>\n");
    printf("</property>\n");
    printf("</configuration>\n");
    printf("</network-data>\n");

    exit(0);
    return (0);
}

int flush_all(void)
{
    int rc = 0, ret = 0;

    ipt_handler *ipt = NULL;
    ebt_handler *ebt = NULL;
    ips_handler *ips = NULL;

    ipt = EUCA_ZALLOC(sizeof(ipt_handler), 1);
    ebt = EUCA_ZALLOC(sizeof(ebt_handler), 1);
    ips = EUCA_ZALLOC(sizeof(ips_handler), 1);

    if (!ipt || !ebt || !ips) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }
    // iptables
    rc = ipt_handler_init(ipt, config->cmdprefix);
    rc = ipt_handler_repopulate(ipt);
    rc = ipt_chain_flush(ipt, "filter", "EUCA_FILTER_FWD");
    rc = ipt_chain_flush(ipt, "filter", "EUCA_COUNTERS_IN");
    rc = ipt_chain_flush(ipt, "filter", "EUCA_COUNTERS_OUT");
    rc = ipt_chain_flush(ipt, "nat", "EUCA_NAT_PRE");
    rc = ipt_chain_flush(ipt, "nat", "EUCA_NAT_POST");
    rc = ipt_chain_flush(ipt, "nat", "EUCA_NAT_OUT");
    rc = ipt_table_deletechainmatch(ipt, "filter", "EU_");
    rc = ipt_handler_print(ipt);
    rc = ipt_handler_deploy(ipt);

    // ipsets
    rc = ips_handler_init(ips, config->cmdprefix);
    rc = ips_handler_repopulate(ips);
    rc = ips_handler_deletesetmatch(ips, "EU_");
    rc = ips_handler_deletesetmatch(ips, "EUCA_");
    rc = ips_handler_print(ips);
    rc = ips_handler_deploy(ips, 1);

    // ebtables
    rc = ebt_handler_init(ebt, config->cmdprefix);
    rc = ebt_handler_repopulate(ebt);
    rc = ebt_chain_flush(ebt, "filter", "EUCA_EBT_FWD");
    rc = ebt_chain_flush(ebt, "nat", "EUCA_EBT_NAT_PRE");
    rc = ebt_handler_deploy(ebt);

    EUCA_FREE(ipt);
    EUCA_FREE(ebt);
    EUCA_FREE(ips);

    return (ret);
}
