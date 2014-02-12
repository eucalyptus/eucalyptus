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

vnetConfig *vnetconfig = NULL;
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
    int rc = 0, opt = 0, debug = 0, firstrun = 1, counter = 0;
    int epoch_updates = 0, epoch_failed_updates = 0;
    int update_localnet_failed = 0, update_networktopo_failed = 0, update_cc_config_failed = 0, update_clcip_failed = 0;
    int update_localnet = 0, update_networktopo = 0, update_cc_config = 0, update_clcip = 0, i;
    time_t epoch_timer = 0;

    // initialize
    eucanetdInit();

    // parse commandline arguments  
    while ((opt = getopt(argc, argv, "s:dh")) != -1) {
        switch (opt) {
        case 'd':
            config->debug = 1;
            break;
        case 's':
            config->ccIp = strdup(optarg);
            config->cc_cmdline_override = 1;
            break;
        case 'h':
            printf("USAGE: %s OPTIONS\n  %-12s| override automatic detection of CC IP address with <ccIp>\n  %-12s| debug - run eucanetd in foreground, all output to terminal\n",
                   argv[0], "-s <ccIp>", "-d");
            exit(1);
            break;
        default:
            printf("USAGE: %s OPTIONS\n  %-12s| override automatic detection of CC IP address with <ccIp>\n  %-12s| debug - run eucanetd in foreground, all output to terminal\n",
                   argv[0], "-s <ccIp>", "-d");
            exit(1);
            break;
        }
    }

    // initialize vnetconfig from local eucalyptus.conf and remote (CC) dynamic config; spin looking for config from CC until one is available
    vnetconfig = malloc(sizeof(vnetConfig));
    if (!vnetconfig) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }
    bzero(vnetconfig, sizeof(vnetConfig));

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
    // spin here until we get the latest config from active CC
    rc = 1;
    while (rc) {
        rc = read_config();
        if (rc) {
            LOGWARN("cannot complete pre-flight checks, retrying\n");
            sleep(1);
        }
    }

    // got all config, enter main loop
    //  while(counter<50000) {
    while (1) {
        update_localnet = update_networktopo = update_cc_config = update_clcip = 0;

        counter++;

        // fetch all latest networking information from various sources
        rc = fetch_latest_network(&update_clcip, &update_networktopo, &update_cc_config, &update_localnet);
        if (rc) {
            LOGWARN("one or more fetches for latest network information was unsucessful\n");
        }
        // first time we run, force an update
        if (firstrun) {
            update_networktopo = update_localnet = update_cc_config = update_clcip = 1;
            firstrun = 0;
        }
        // if the last update operations failed, regardless of new info, force an update
        LOGDEBUG("failed bits 1: update_clcip_failed=%d update_networktopo_failed=%d update_localnet_failed=%d\n", update_clcip_failed, update_networktopo_failed,
                 update_localnet_failed);
        if (update_clcip_failed)
            update_clcip = 1;
        if (update_networktopo_failed)
            update_networktopo = 1;
        if (update_localnet_failed)
            update_localnet = 1;

        // whether or not updates have occurred due to remote content being updated, read local networking info
        rc = read_latest_network();
        if (rc) {
            LOGWARN("read_latest_network failed, skipping update\n");
            // if the local read failed for some reason, skip any attempt to update (leave current state in place)
            update_localnet = update_networktopo = update_cc_config = update_clcip = 0;
        }
        // now, preform any updates that are required    
        if (update_clcip) {
            LOGINFO("new networking state (CLC IP metadata service): updating system\n");
            // update metadata redirect rule
            update_clcip_failed = 0;
            rc = update_metadata_redirect();
            if (rc) {
                LOGERROR("could not update metadata redirect rule\n");
                update_clcip_failed = 1;
            }
        }
        // if information on sec. group rules/membership has changed, apply
        if (update_networktopo || update_localnet) {
            LOGINFO("new networking state (network topology/security groups): updating system\n");
            update_networktopo_failed = 0;
            // install iptables FW rules, using IPsets for sec. group 
            rc = update_sec_groups();
            if (rc) {
                LOGERROR("could not complete update of security groups\n");
                update_networktopo_failed = 1;
            }
        }
        // if information about local VM network config has changed, apply
        if (update_networktopo || update_localnet) {
            LOGINFO("new networking state (VM public/private network addresses): updating system\n");
            update_localnet_failed = 0;

            // update list of private IPs, handle DHCP daemon re-configure and restart
            rc = update_private_ips();
            if (rc) {
                LOGERROR("could not complete update of private IPs\n");
                update_localnet_failed = 1;
            }
            // update public IP assignment and NAT table entries
            rc = update_public_ips();
            if (rc) {
                LOGERROR("could not complete update of public IPs\n");
                update_localnet_failed = 1;
            }
            // install ebtables rules for isolation
            rc = update_isolation_rules();
            if (rc) {
                LOGERROR("could not complete update of VM network isolation rules\n");
                update_localnet_failed = 1;
            }
        }

        if (update_localnet || update_networktopo || update_clcip) {
            if (update_clcip_failed || update_localnet_failed || update_networktopo_failed) {
                epoch_failed_updates++;
            } else {
                epoch_updates++;
            }
        }
        // temporary exit after one iteration
        //    exit(0);

        if (epoch_timer >= 300) {
            LOGINFO("eucanetd has performed %d successful updates and %d failed updates during this %f minute duty cycle\n", epoch_updates, epoch_failed_updates, 10.0 / 60.0);
            epoch_updates = epoch_failed_updates = epoch_timer = 0;
        }
        // do it all over again...

        if (update_clcip_failed || update_localnet_failed || update_networktopo_failed) {
            LOGDEBUG("main loop complete: failures detected sleeping %d seconds before next poll\n", 1);
            sleep(1);
        } else {
            LOGDEBUG("main loop complete: sleeping %d seconds before next poll\n", config->cc_polling_frequency);
            sleep(config->cc_polling_frequency);
        }
        epoch_timer += config->cc_polling_frequency;
    }

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
    int rc;

    if (isConfigModified(config->configFiles, 2) > 0) { // config modification time has changed
        if (readConfigFile(config->configFiles, 2)) {
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
    char pidfile[MAX_PATH];
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
        snprintf(pidfile, MAX_PATH, "%s/var/run/eucalyptus/eucalyptus-eucanetd.pid", config->eucahome);
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
    int rc, ret = 0, i, fd, j, doit;
    char cmd[MAX_PATH];
    char *strptra = NULL, *strptrb = NULL, *vnetinterface = NULL, *gwip = NULL, *brmac = NULL;
    gni_securityGroup *group = NULL;

    // TODO - check ebtables thoroughly
    // this rule clears, but dont understand exactly why: ebtables -I EUCA_EBT_FWD -p IPv4 -i vnet3 --logical-in br0 --ip-src 1.1.0.5 -j ACCEPT

    rc = ebt_handler_repopulate(config->ebt);

    rc = ebt_table_add_chain(config->ebt, "filter", "EUCA_EBT_FWD", "ACCEPT", "");
    rc = ebt_chain_add_rule(config->ebt, "filter", "FORWARD", "-j EUCA_EBT_FWD");
    rc = ebt_chain_flush(config->ebt, "filter", "EUCA_EBT_FWD");
    rc = ebt_table_add_chain(config->ebt, "nat", "EUCA_EBT_NAT_PRE", "ACCEPT", "");
    rc = ebt_chain_add_rule(config->ebt, "nat", "PREROUTING", "-j EUCA_EBT_NAT_PRE");
    rc = ebt_chain_flush(config->ebt, "nat", "EUCA_EBT_NAT_PRE");

    // add these for DHCP to pass
    rc = ebt_chain_add_rule(config->ebt, "filter", "EUCA_EBT_FWD", "-p IPv4 -d Broadcast --ip-proto udp --ip-dport 67:68 -j ACCEPT");
    rc = ebt_chain_add_rule(config->ebt, "filter", "EUCA_EBT_FWD", "-p IPv4 -d Broadcast --ip-proto udp --ip-sport 67:68 -j ACCEPT");

    for (i = 0; i < config->max_security_groups; i++) {
        group = &(config->security_groups[i]);
        for (j = 0; j < group->max_member_ips; j++) {
            if (group->member_ips[j] && maczero(group->member_macs[j])) {
                strptra = strptrb = NULL;
                strptra = hex2dot(group->member_ips[j]);
                hex2mac(group->member_macs[j], &strptrb);
                vnetinterface = mac2interface(strptrb);
                gwip = hex2dot(config->defaultgw);
                brmac = interface2mac(vnetconfig->bridgedev);

                if (strptra && strptrb && vnetinterface && gwip && brmac) {
                    if (!config->disable_l2_isolation) {
                        snprintf(cmd, MAX_PATH, "-p IPv4 -i %s --logical-in %s --ip-src %s -j ACCEPT", vnetinterface, vnetconfig->bridgedev, strptra);
                        rc = ebt_chain_add_rule(config->ebt, "filter", "EUCA_EBT_FWD", cmd);
                        snprintf(cmd, MAX_PATH, "-p IPv4 -s %s -i %s --ip-src ! %s -j DROP", strptrb, vnetinterface, strptra);
                        rc = ebt_chain_add_rule(config->ebt, "filter", "EUCA_EBT_FWD", cmd);
                        snprintf(cmd, MAX_PATH, "-p IPv4 -s ! %s -i %s --ip-src %s -j DROP", strptrb, vnetinterface, strptra);
                        rc = ebt_chain_add_rule(config->ebt, "filter", "EUCA_EBT_FWD", cmd);
                    }
                    if (config->fake_router) {
                        snprintf(cmd, MAX_PATH, "-i %s -p arp --arp-ip-dst %s -j arpreply --arpreply-mac %s", vnetinterface, gwip, brmac);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);
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
    }

    rc = ebt_handler_print(config->ebt);
    rc = ebt_handler_deploy(config->ebt);
    if (rc) {
        LOGERROR("could not install ebtables rules\n");
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

    if (!config->clcIp || !strlen(config->clcIp)) {
        LOGWARN("no valid CLC IP has been set yet, skipping metadata redirect update\n");
        return (1);
    }

    if (ipt_handler_repopulate(config->ipt))
        return (1);

    snprintf(rule, 1024, "-A PREROUTING -d 169.254.169.254/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", config->clcIp);
    if (ipt_chain_add_rule(config->ipt, "nat", "PREROUTING", rule))
        return (1);

    //  rc = ipt_handler_print(config->ipt);
    rc = ipt_handler_deploy(config->ipt);
    if (rc) {
        LOGERROR("could not apply new rule (%s)\n", rule);
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
    int rc;
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

    if (!globalnetworkinfo) {
        globalnetworkinfo = malloc(sizeof(globalNetworkInfo));
        if (!globalnetworkinfo) {
            LOGFATAL("out of memory\n");
            exit(1);
        }
    }
    bzero(globalnetworkinfo, sizeof(globalNetworkInfo));

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
    int ret = 0, i, rc, j, fd;
    char ips_file[MAX_PATH], *strptra = NULL;
    char cmd[MAX_PATH], clcmd[MAX_PATH], rule[1024];
    FILE *FH = NULL;
    sequence_executor cmds;
    gni_securityGroup *group = NULL;

    ret = 0;

    // pull in latest IPT state
    rc = ipt_handler_repopulate(config->ipt);
    if (rc) {
        LOGERROR("cannot read current IPT rules\n");
        return (1);
    }
    // pull in latest IPS state
    rc = ips_handler_repopulate(config->ips);
    if (rc) {
        LOGERROR("cannot read current IPS sets\n");
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
    rc = ips_handler_deletesetmatch(config->ips, "EU_");

    // add chains/rules
    for (i = 0; i < config->max_security_groups; i++) {
        group = &(config->security_groups[i]);
        rule[0] = '\0';

        ips_handler_add_set(config->ips, group->chainname);
        ips_set_flush(config->ips, group->chainname);

        strptra = hex2dot(config->defaultgw);
        ips_set_add_ip(config->ips, group->chainname, strptra);
        EUCA_FREE(strptra);
        for (j = 0; j < group->max_member_ips; j++) {
            if (group->member_ips[j]) {
                strptra = hex2dot(group->member_ips[j]);
                ips_set_add_ip(config->ips, group->chainname, strptra);
                EUCA_FREE(strptra);
            }
        }

        // add forward chain
        ipt_table_add_chain(config->ipt, "filter", group->chainname, "-", "[0:0]");
        ipt_chain_flush(config->ipt, "filter", group->chainname);

        // add jump rule
        snprintf(rule, 1024, "-A EUCA_FILTER_FWD -m set --match-set %s dst -j %s", group->chainname, group->chainname);
        ipt_chain_add_rule(config->ipt, "filter", "EUCA_FILTER_FWD", rule);

        // populate forward chain

        // this one needs to be first
        snprintf(rule, 1024, "-A %s -m set --match-set %s src,dst -j ACCEPT", group->chainname, group->chainname);
        ipt_chain_add_rule(config->ipt, "filter", group->chainname, rule);
        // make sure conntrack rule is in place
        snprintf(rule, 1024, "-A %s -m conntrack --ctstate ESTABLISHED -j ACCEPT", group->chainname);
        ipt_chain_add_rule(config->ipt, "filter", group->chainname, rule);

        // then put all the group specific IPT rules (temporary one here)
        if (group->max_grouprules) {
            for (j = 0; j < group->max_grouprules; j++) {
                snprintf(rule, 1024, "-A %s %s -j ACCEPT", group->chainname, group->grouprules[j]);
                ipt_chain_add_rule(config->ipt, "filter", group->chainname, rule);
            }
        }
        // this ones needs to be last
        snprintf(rule, 1024, "-A %s -j DROP", group->chainname);
        ipt_chain_add_rule(config->ipt, "filter", group->chainname, rule);

    }

    if (1 || !ret) {
        ips_handler_print(config->ips);
        rc = ips_handler_deploy(config->ips, 0);
        if (rc) {
            LOGERROR("could not apply ipsets\n");
            ret = 1;
        }
    }

    if (1 || !ret) {
        ipt_handler_print(config->ipt);
        rc = ipt_handler_deploy(config->ipt);
        if (rc) {
            LOGERROR("could not apply new rules\n");
            ret = 1;
        }
    }

    if (1 || !ret) {
        ips_handler_print(config->ips);
        rc = ips_handler_deploy(config->ips, 1);
        if (rc) {
            LOGERROR("could not apply ipsets\n");
            ret = 1;
        }
    }
    return (ret);
}

//!
//! Function description.
//!
//! @param[in] groups
//! @param[in] max_groups
//! @param[in] privip
//! @param[in] outfoundidx
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
gni_securityGroup *find_sec_group_bypriv(gni_securityGroup * groups, int max_groups, u32 privip, int *outfoundidx)
{
    int i, j, rc = 0, found = 0, foundgidx = 0, foundipidx = 0;

    if (!groups || max_groups <= 0 || !privip) {
        return (NULL);
    }

    for (i = 0; i < max_groups && !found; i++) {
        for (j = 0; j < groups[i].max_member_ips && !found; j++) {
            if (groups[i].member_ips[j] == privip) {
                foundgidx = i;
                foundipidx = j;
                found++;
            }
        }
    }
    if (found) {
        if (outfoundidx) {
            *outfoundidx = foundipidx;
        }
        return (&(groups[foundgidx]));
    }
    return (NULL);
}

//!
//! Function description.
//!
//! @param[in] groups
//! @param[in] max_groups
//! @param[in] pubip
//! @param[in] outfoundidx
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
gni_securityGroup *find_sec_group_bypub(gni_securityGroup * groups, int max_groups, u32 pubip, int *outfoundidx)
{
    int i, j, rc = 0, found = 0, foundgidx = 0, foundipidx = 0;

    if (!groups || max_groups <= 0 || !pubip) {
        return (NULL);
    }

    for (i = 0; i < max_groups && !found; i++) {
        for (j = 0; j < groups[i].max_member_ips && !found; j++) {
            if (groups[i].member_public_ips[j] == pubip) {
                foundgidx = i;
                foundipidx = j;
                found++;
            }
        }
    }
    if (found) {
        if (outfoundidx) {
            *outfoundidx = foundipidx;
        }
        return (&(groups[foundgidx]));
    }
    return (NULL);
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
    int slashnet = 0, ret = 0, rc = 0, i = 0, j = 0, foundidx = 0, doit = 0;
    char cmd[MAX_PATH], clcmd[MAX_PATH], rule[1024];
    char *strptra = NULL, *strptrb = NULL;
    sequence_executor cmds;
    gni_securityGroup *group = NULL;

    slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - vnetconfig->networks[0].nm) + 1))));

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
        LOGERROR("could not add euca net chains\n");
        ret = 1;
    }

    rc = ipt_handler_repopulate(config->ipt);
    rc = se_init(&cmds, config->cmdprefix, 2, 1);

    ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_PRE");
    ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_POST");
    ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_OUT");

    strptra = hex2dot(vnetconfig->networks[0].nw);

    snprintf(rule, 1024, "-A EUCA_NAT_PRE -s %s/%d -d %s/%d -j MARK --set-xmark 0x2a/0xffffffff", strptra, slashnet, strptra, slashnet);
    ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_PRE", rule);

    snprintf(rule, 1024, "-A EUCA_COUNTERS_IN -d %s/%d", strptra, slashnet);
    ipt_chain_add_rule(config->ipt, "filter", "EUCA_COUNTERS_IN", rule);

    snprintf(rule, 1024, "-A EUCA_COUNTERS_OUT -s %s/%d", strptra, slashnet);
    ipt_chain_add_rule(config->ipt, "filter", "EUCA_COUNTERS_OUT", rule);

    EUCA_FREE(strptra);

    for (i = 0; i < config->max_security_groups; i++) {
        group = &(config->security_groups[i]);
        for (j = 0; j < group->max_member_ips; j++) {
            strptra = hex2dot(group->member_public_ips[j]);
            strptrb = hex2dot(group->member_ips[j]);
            if ((group->member_public_ips[j] && group->member_ips[j]) && (group->member_public_ips[j] != group->member_ips[j])) {
                snprintf(cmd, MAX_PATH, "ip addr add %s/%d dev %s >/dev/null 2>&1", strptra, 32, vnetconfig->pubInterface);
                rc = se_add(&cmds, cmd, NULL, ignore_exit2);

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

                // actually added some stuff to do
                doit++;
            }

            EUCA_FREE(strptra);
            EUCA_FREE(strptrb);
        }
    }

    if (doit) {
        se_print(&cmds);
        rc = se_execute(&cmds);
        if (rc) {
            LOGERROR("could not execute command sequence 1\n");
            ret = 1;
        }
        se_free(&cmds);
    }
    //  rc = ipt_handler_print(config->ipt);
    rc = ipt_handler_deploy(config->ipt);
    if (rc) {
        LOGERROR("could not apply new rules\n");
        ret = 1;
    }
    // if all has gone well, now clear any public IPs that have not been mapped to private IPs
    if (!ret) {
        se_init(&cmds, config->cmdprefix, 2, 1);
        for (i = 0; i < config->max_all_public_ips; i++) {
            group = find_sec_group_bypub(config->security_groups, config->max_security_groups, config->all_public_ips[i], &foundidx);
            if (!group) {
                strptra = hex2dot(config->all_public_ips[i]);
                snprintf(cmd, MAX_PATH, "ip addr del %s/%d dev %s >/dev/null 2>&1", strptra, 32, vnetconfig->pubInterface);
                rc = se_add(&cmds, cmd, NULL, ignore_exit2);
                EUCA_FREE(strptra);
            }
        }
        se_print(&cmds);
        rc = se_execute(&cmds);
        if (rc) {
            LOGERROR("could not execute command sequence 2\n");
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
    int ret = 0, rc, i, j;
    char mac[32], *strptra = NULL, *strptrb = NULL;
    gni_securityGroup *group = NULL;

    bzero(mac, 32);

    bzero(vnetconfig->networks[0].addrs, sizeof(netEntry) * NUMBER_OF_HOSTS_PER_VLAN);

    // populate vnetconfig with new info
    for (i = 0; i < config->max_security_groups; i++) {
        group = &(config->security_groups[i]);
        for (j = 0; j < group->max_member_ips; j++) {
            strptra = hex2dot(group->member_public_ips[j]);
            strptrb = hex2dot(group->member_ips[j]);
            if (group->member_ips[j] && group->member_local[j]) {
                LOGDEBUG("adding ip: %s\n", strptrb);
                rc = vnetAddPrivateIP(vnetconfig, strptrb);
                if (rc) {
                    LOGERROR("could not add private IP '%s'\n", strptrb);
                    ret = 1;
                } else {
                    vnetGenerateNetworkParams(vnetconfig, "", 0, -1, mac, strptra, strptrb);
                }
            }
            EUCA_FREE(strptra);
            EUCA_FREE(strptrb);
        }
    }

    // generate DHCP config, monitor/start DHCP service
    rc = vnetKickDHCP(vnetconfig);
    if (rc) {
        LOGERROR("failed to kick dhcpd\n");
        ret = 1;
    }

    return (ret);
}

//!
//! Function description.
//!
//! @param[in] update_serviceIps
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
int fetch_latest_serviceIps(int *update_serviceIps)
{
    int ret, foundall, foundcc, foundclc;
    char ccIp[64], clcIp[64], buf[1024];
    FILE *FH = NULL;

    ret = foundall = foundcc = foundclc = 0;
    ccIp[0] = clcIp[0] = '\0';

    FH = fopen(config->nc_localnetfile.dest, "r");
    if (FH) {
        while (fgets(buf, 1024, FH) && foundall < 2) {
            LOGTRACE("line: %s\n", SP(buf));
            if (strstr(buf, "CCIP=")) {
                sscanf(buf, "CCIP=%[0-9.]", ccIp);
                LOGDEBUG("parsed line from file(%s): ccIp=%s\n", SP(config->nc_localnetfile.dest), ccIp);
                if (strlen(ccIp) && strcmp(ccIp, "0.0.0.0")) {
                    foundcc = 1;
                } else {
                    LOGWARN("malformed ccIp entry in file, skipping: %s\n", SP(buf));
                }
            } else if (strstr(buf, "CLCIP=")) {
                sscanf(buf, "CLCIP=%[0-9.]", clcIp);
                LOGDEBUG("parsed line from file(%s): clcIp=%s\n", SP(config->nc_localnetfile.dest), clcIp);
                if (strlen(clcIp) && strcmp(clcIp, "0.0.0.0")) {
                    foundclc = 1;
                } else {
                    LOGWARN("malformed clcIp entry in file, skipping: %s\n", SP(buf));
                }
            }
            foundall = foundcc + foundclc;
        }

        if (!config->cc_cmdline_override) {
            if (!strlen(ccIp)) {
                LOGERROR("could not find valid CCIP=<ccip> in file(%s)\n", SP(config->nc_localnetfile.dest));
                ret = 1;
            } else {
                if (config->ccIp) {
                    if (!strcmp(config->ccIp, ccIp)) {
                        // no change
                    } else {
                        if (update_serviceIps) {
                            *update_serviceIps = 1;
                        }
                    }
                    EUCA_FREE(config->ccIp);
                }
                config->ccIp = strdup(ccIp);
            }
        }

        if (!strlen(clcIp)) {
            LOGERROR("could not find valid CLCIP=<clcip> in file(%s)\n", SP(config->nc_localnetfile.dest));
            ret = 1;
        } else {
            if (config->clcIp) {
                if (!strcmp(config->clcIp, clcIp)) {
                    // no change
                } else {
                    if (update_serviceIps) {
                        *update_serviceIps = 1;
                    }
                }
                EUCA_FREE(config->clcIp);
            }
            config->clcIp = strdup(clcIp);
        }
        fclose(FH);
    } else {
        LOGERROR("could not open file(%s)\n", SP(config->nc_localnetfile.dest));
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
int read_config_bootstrap(void)
{
    char *eucaenv = getenv(EUCALYPTUS_ENV_VAR_NAME), *eucauserenv = getenv(EUCALYPTUS_USER_ENV_VAR_NAME), home[MAX_PATH], user[MAX_PATH], eucadir[MAX_PATH], logfile[MAX_PATH];
    int rc, ret, i;
    struct passwd *pwent = NULL;

    ret = 0;

    if (!eucaenv) {
        snprintf(home, MAX_PATH, "/");
    } else {
        snprintf(home, MAX_PATH, "%s", eucaenv);
    }

    if (!eucauserenv) {
        snprintf(user, MAX_PATH, "eucalyptus");
    } else {
        snprintf(user, MAX_PATH, eucauserenv);
    }

    snprintf(eucadir, MAX_PATH, "%s/var/log/eucalyptus", home);
    if (check_directory(eucadir)) {
        fprintf(stderr, "cannot locate eucalyptus installation: make sure EUCALYPTUS env is set\n");
        exit(1);
    }

    config->eucahome = strdup(home);
    config->eucauser = strdup(user);

    if (!config->debug) {
        snprintf(logfile, MAX_PATH, "%s/var/log/eucalyptus/eucanetd.log", config->eucahome);
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
    char *tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME), home[MAX_PATH], url[MAX_PATH], netPath[MAX_PATH], destfile[MAX_PATH], sourceuri[MAX_PATH], eucadir[MAX_PATH];
    char *cvals[EUCANETD_CVAL_LAST];
    int fd, rc, ret, i, to_update = 0;

    ret = 0;
    bzero(cvals, sizeof(char *) * EUCANETD_CVAL_LAST);

    for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
        EUCA_FREE(cvals[i]);
    }

    if (!tmpstr) {
        snprintf(home, MAX_PATH, "/");
    } else {
        snprintf(home, MAX_PATH, "%s", tmpstr);
    }

    snprintf(eucadir, MAX_PATH, "%s/var/log/eucalyptus", home);
    if (check_directory(eucadir)) {
        LOGFATAL("cannot locate eucalyptus installation: make sure EUCALYPTUS env is set\n");
        exit(1);
    }

    snprintf(netPath, MAX_PATH, CC_NET_PATH_DEFAULT, home);

    snprintf(destfile, MAX_PATH, "%s/var/lib/eucalyptus/eucanetd_nc_local_net_file", home);
    snprintf(sourceuri, MAX_PATH, "file://" EUCALYPTUS_LOG_DIR "/local-net", home);
    atomic_file_init(&(config->nc_localnetfile), sourceuri, destfile);

    rc = atomic_file_get(&(config->nc_localnetfile), &to_update);
    if (rc) {
        LOGWARN("cannot get latest info from NC generated file (%s)\n", config->nc_localnetfile.dest);
        for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
            EUCA_FREE(cvals[i]);
        }
        return (1);
    }

    rc = fetch_latest_serviceIps(NULL);
    if (rc) {
        LOGWARN("cannot get latest service IPs from NC generated file (%s)\n", config->nc_localnetfile.dest);
        for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
            EUCA_FREE(cvals[i]);
        }
        return (1);
    }

    snprintf(destfile, MAX_PATH, "%s/var/lib/eucalyptus/eucanetd_cc_config_file", home);
    snprintf(sourceuri, MAX_PATH, "http://%s:8776/config-cc", SP(config->ccIp));
    atomic_file_init(&(config->cc_configfile), sourceuri, destfile);

    rc = atomic_file_get(&(config->cc_configfile), &to_update);
    if (rc) {
        LOGWARN("cannot fetch config file from CC (%s)\n", config->ccIp);
        for (i = 0; i < EUCANETD_CVAL_LAST; i++) {
            EUCA_FREE(cvals[i]);
        }
        return (1);
    }

    snprintf(destfile, MAX_PATH, "%s/var/lib/eucalyptus/eucanetd_cc_network_topology_file", home);
    snprintf(sourceuri, MAX_PATH, "http://%s:8776/network-topology", SP(config->ccIp));
    atomic_file_init(&(config->cc_networktopofile), sourceuri, destfile);

    snprintf(config->configFiles[0], MAX_PATH, EUCALYPTUS_CONF_LOCATION, home);
    snprintf(config->configFiles[1], MAX_PATH, "%s", config->cc_configfile.dest);

    configInitValues(configKeysRestartEUCANETD, configKeysNoRestartEUCANETD);   // initialize config subsystem
    readConfigFile(config->configFiles, 2);

    // thing to read from the local NC config file
    cvals[EUCANETD_CVAL_PUBINTERFACE] = configFileValue("VNET_PUBINTERFACE");
    cvals[EUCANETD_CVAL_PRIVINTERFACE] = configFileValue("VNET_PRIVINTERFACE");
    cvals[EUCANETD_CVAL_BRIDGE] = configFileValue("VNET_BRIDGE");
    cvals[EUCANETD_CVAL_EUCAHOME] = configFileValue("EUCALYPTUS");
    cvals[EUCANETD_CVAL_MODE] = configFileValue("VNET_MODE");
    cvals[EUCANETD_CVAL_EUCA_USER] = configFileValue("EUCA_USER");
    cvals[EUCANETD_CVAL_DHCPDAEMON] = configFileValue("VNET_DHCPDAEMON");
    cvals[EUCANETD_CVAL_DHCPUSER] = configFileValue("VNET_DHCPUSER");

    // things to read from the fetched CC config file
    cvals[EUCANETD_CVAL_ADDRSPERNET] = configFileValue("VNET_ADDRSPERNET");
    cvals[EUCANETD_CVAL_SUBNET] = configFileValue("VNET_SUBNET");
    cvals[EUCANETD_CVAL_NETMASK] = configFileValue("VNET_NETMASK");
    cvals[EUCANETD_CVAL_BROADCAST] = configFileValue("VNET_BROADCAST");
    cvals[EUCANETD_CVAL_DNS] = configFileValue("VNET_DNS");
    cvals[EUCANETD_CVAL_DOMAINNAME] = configFileValue("VNET_DOMAINNAME");
    cvals[EUCANETD_CVAL_ROUTER] = configFileValue("VNET_ROUTER");
    cvals[EUCANETD_CVAL_MACPREFIX] = configFileValue("VNET_MACPREFIX");

    cvals[EUCANETD_CVAL_CC_POLLING_FREQUENCY] = configFileValue("CC_POLLING_FREQUENCY");
    cvals[EUCANETD_CVAL_DISABLE_L2_ISOLATION] = configFileValue("DISABLE_L2_ISOLATION");
    cvals[EUCANETD_CVAL_FAKE_ROUTER] = configFileValue("FAKE_ROUTER");

    config->eucahome = strdup(cvals[EUCANETD_CVAL_EUCAHOME]);
    config->eucauser = strdup(cvals[EUCANETD_CVAL_EUCA_USER]);
    snprintf(config->cmdprefix, MAX_PATH, EUCALYPTUS_ROOTWRAP, config->eucahome);
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
    config->defaultgw = dot2hex(cvals[EUCANETD_CVAL_ROUTER]);

    LOGDEBUG
        ("required variables read from local config file: EUCALYPTUS=%s EUCA_USER=%s VNET_MODE=%s VNET_PUBINTERFACE=%s VNET_PRIVINTERFACE=%s VNET_BRIDGE=%s VNET_DHCPDAEMON=%s\n",
         SP(cvals[EUCANETD_CVAL_EUCAHOME]), SP(cvals[EUCANETD_CVAL_EUCA_USER]), SP(cvals[EUCANETD_CVAL_MODE]), SP(cvals[EUCANETD_CVAL_PUBINTERFACE]),
         SP(cvals[EUCANETD_CVAL_PRIVINTERFACE]), SP(cvals[EUCANETD_CVAL_BRIDGE]), SP(cvals[EUCANETD_CVAL_DHCPDAEMON]));

    rc = logInit();
    if (rc) {
        LOGERROR("unable to initialize logging subsystem\n");
        ret = 1;
    }

    rc = vnetInit(vnetconfig, cvals[EUCANETD_CVAL_MODE], cvals[EUCANETD_CVAL_EUCAHOME], netPath, CLC, cvals[EUCANETD_CVAL_PUBINTERFACE], cvals[EUCANETD_CVAL_PRIVINTERFACE],
                  cvals[EUCANETD_CVAL_ADDRSPERNET], cvals[EUCANETD_CVAL_SUBNET], cvals[EUCANETD_CVAL_NETMASK], cvals[EUCANETD_CVAL_BROADCAST], cvals[EUCANETD_CVAL_DNS],
                  cvals[EUCANETD_CVAL_DOMAINNAME], cvals[EUCANETD_CVAL_ROUTER], cvals[EUCANETD_CVAL_DHCPDAEMON], cvals[EUCANETD_CVAL_DHCPUSER], cvals[EUCANETD_CVAL_BRIDGE], NULL,
                  cvals[EUCANETD_CVAL_MACPREFIX]);
    if (rc) {
        LOGERROR("unable to initialize vnetwork subsystem\n");
        ret = 1;
    }

    config->ipt = malloc(sizeof(ipt_handler));
    if (!config->ipt) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }
    rc = ipt_handler_init(config->ipt, config->cmdprefix);
    if (rc) {
        LOGERROR("could not initialize ipt_handler\n");
        ret = 1;
    }

    config->ips = malloc(sizeof(ips_handler));
    if (!config->ips) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }
    rc = ips_handler_init(config->ips, config->cmdprefix);
    if (rc) {
        LOGERROR("could not initialize ips_handler\n");
        ret = 1;
    }

    config->ebt = malloc(sizeof(ebt_handler));
    if (!config->ebt) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }
    rc = ebt_handler_init(config->ebt, config->cmdprefix);
    if (rc) {
        LOGERROR("could not initialize ebt_handler\n");
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
    char *log_facility = NULL, *log_prefix = NULL, logfile[MAX_PATH];

    if (!config->debug) {
        snprintf(logfile, MAX_PATH, "%s/var/log/eucalyptus/eucanetd.log", config->eucahome);
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
//! @param[in] update_clcip
//! @param[in] update_networktopo
//! @param[in] update_cc_config
//! @param[in] update_localnet
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
int fetch_latest_network(int *update_clcip, int *update_networktopo, int *update_cc_config, int *update_localnet)
{
    int rc = 0, ret = 0;

    if (!update_clcip || !update_networktopo || !update_cc_config || !update_localnet) {
        LOGERROR("BUG: input contains null pointers\n");
        return (1);
    }
    // don't run any updates unless something new has happened
    *update_localnet = *update_networktopo = *update_cc_config = *update_clcip = 0;

    rc = fetch_latest_localconfig();
    if (rc) {
        LOGWARN("cannot read in changes to local configuration file: check local eucalyptus.conf\n");
    }
    // get latest CC/CLC IP addrs and set update flag if CC/CLC IPs have changed
    rc = fetch_latest_serviceIps(update_clcip);
    if (rc) {
        LOGWARN("cannot get latest serviceIps from NC: check that NC is running\n");
        ret = 1;
    }
    // get latest networking data from eucalyptus, set update flags if content has changed
    rc = fetch_latest_cc_network(update_networktopo, update_cc_config, update_localnet);
    if (rc) {
        LOGWARN("cannot get latest network topology, configuration and/or local VM network from CC/NC: check that CC and NC are running\n");
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

    rc = parse_network_topology(config->cc_networktopofile.dest);
    if (rc) {
        LOGERROR("cannot parse network-topology file (%s)\n", config->cc_networktopofile.dest);
        ret = 1;
    }

    rc = parse_pubprivmap(config->nc_localnetfile.dest);
    if (rc) {
        LOGERROR("cannot parse pubprivmap file (%s)\n", config->nc_localnetfile.dest);
        ret = 1;
    }

    sec_groups_print(config->security_groups, config->max_security_groups);

    rc = parse_ccpubprivmap(config->cc_configfile.dest);
    if (rc) {
        LOGERROR("cannot parse pubprivmap file (%s)\n", config->cc_configfile.dest);
        ret = 1;
    }

    return (ret);
}

//!
//! Function description.
//!
//! @param[in] cc_configfile
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
int parse_ccpubprivmap(char *cc_configfile)
{
    int ret = 0;
    char pub[64], priv[64], buf[1024];
    FILE *FH;

    config->max_all_public_ips = 0;

    FH = fopen(cc_configfile, "r");
    if (FH) {
        while (fgets(buf, 1024, FH)) {
            pub[0] = priv[0] = '\0';
            if (strstr(buf, "IPMAP=")) {
                sscanf(buf, "IPMAP=%[0-9.] %[0-9.]", pub, priv);
                if (strlen(pub)) {
                    config->all_public_ips[config->max_all_public_ips] = dot2hex(pub);
                    config->max_all_public_ips++;
                }
            }
        }
        fclose(FH);
    } else {
        LOGERROR("could not open file (%s)\n", cc_configfile);
        ret = 1;
    }

    return (ret);
}

//!
//! Function description.
//!
//! @param[in] pubprivmap_file
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
int parse_pubprivmap(char *pubprivmap_file)
{
    char buf[1024], priv[64], pub[64], mac[64], instid[64], bridgedev[64], tmp[64], vlan[64], ccIp[64];
    int count = 0, ret = 0, foundidx = 0;
    FILE *FH = NULL;
    gni_securityGroup *group = NULL;

    FH = fopen(pubprivmap_file, "r");
    if (FH) {
        while (fgets(buf, 1024, FH)) {
            priv[0] = pub[0] = ccIp[0] = '\0';

            if (strstr(buf, "CCIP=") || strstr(buf, "CLCIP=")) {
            } else if (strstr(buf, "i-")) {
                sscanf(buf, "%s %s %s %s %s %[0-9.] %[0-9.]", instid, bridgedev, tmp, vlan, mac, pub, priv);
                LOGDEBUG("parsed line from local pubprivmapfile: instId=%s bridgedev=%s NA=%s vlan=%s mac=%s pub=%s priv=%s\n", instid, bridgedev, tmp, vlan, mac, pub, priv);
                if ((strlen(priv) && strlen(pub)) && !(!strcmp(priv, "0.0.0.0") && !strcmp(pub, "0.0.0.0"))) {
                    group = find_sec_group_bypriv(config->security_groups, config->max_security_groups, dot2hex(priv), &foundidx);
                    if (!group) {
                        LOGDEBUG("group is null\n");
                    }
                    if (group && (foundidx >= 0)) {
                        group->member_public_ips[foundidx] = dot2hex(pub);
                        mac2hex(mac, group->member_macs[foundidx]);
                        //                snprintf(group->bridgedev, 32, "%s", bridgedev);
                        group->member_local[foundidx] = 1;
                    }
                }
            }
        }
        fclose(FH);
    } else {
        LOGERROR("could not open map file for read (%s)\n", pubprivmap_file);
        ret = 1;
    }
    sec_groups_print(config->security_groups, config->max_security_groups);
    return (ret);
}

#if 0
//!
//! Function description.
//!
//! @param[in] pubprivmap_file
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
int parse_pubprivmap_cc(char *pubprivmap_file)
{
    char buf[1024], priv[64], pub[64];
    int count = 0, ret = 0;
    FILE *FH = NULL;

    FH = fopen(pubprivmap_file, "r");
    if (FH) {
        while (fgets(buf, 1024, FH)) {
            priv[0] = pub[0] = '\0';
            sscanf(buf, "%[0-9.]=%[0-9.]", pub, priv);
            if ((strlen(priv) && strlen(pub)) && !(!strcmp(priv, "0.0.0.0") && !strcmp(pub, "0.0.0.0"))) {
                config->private_ips[count] = dot2hex(priv);
                config->public_ips[count] = dot2hex(pub);
                count++;
                config->max_ips = count;
            }
        }
        fclose(FH);
    } else {
        LOGERROR("could not open map file for read (%s)\n", pubprivmap_file);
        ret = 1;
    }
    return (ret);
}
#endif

//!
//! Function description.
//!
//! @param[in] update_networktopo
//! @param[in] update_cc_config
//! @param[in] update_localnet
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
int fetch_latest_cc_network(int *update_networktopo, int *update_cc_config, int *update_localnet)
{
    int rc = 0, ret = 0;

    rc = atomic_file_get(&(config->cc_networktopofile), update_networktopo);
    if (rc) {
        LOGWARN("could not fetch latest network topology from CC\n");
        ret = 1;
    }

    rc = atomic_file_get(&(config->cc_configfile), update_cc_config);
    if (rc) {
        LOGWARN("could not fetch latest configuration from CC\n");
        ret = 1;
    }

    rc = atomic_file_get(&(config->nc_localnetfile), update_localnet);
    if (rc) {
        LOGWARN("could not fetch latest local network info from NC\n");
        ret = 1;
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
int parse_network_topology(char *file)
{
    int ret = 0, rc, gidx, i;
    FILE *FH = NULL;
    char buf[MAX_PATH], rulebuf[4097], newrule[2048];
    char *toka = NULL, *ptra = NULL, *modetok = NULL, *grouptok = NULL, chainname[32], *chainhash;
    gni_securityGroup *newgroups = NULL, *group = NULL;
    int max_newgroups = 0, curr_group = 0;
    u32 newip = 0;

    // do the GROUP pass first, then RULE pass
    FH = fopen(file, "r");
    if (!FH) {
        ret = 1;
    } else {
        while (fgets(buf, MAX_PATH, FH)) {
            modetok = strtok_r(buf, " ", &ptra);
            grouptok = strtok_r(NULL, " ", &ptra);

            if (modetok && grouptok) {

                if (!strcmp(modetok, "GROUP")) {
                    curr_group = max_newgroups;
                    max_newgroups++;
                    newgroups = realloc(newgroups, sizeof(gni_securityGroup) * max_newgroups);
                    if (!newgroups) {
                        LOGFATAL("out of memory!\n");
                        exit(1);
                    }
                    bzero(&(newgroups[curr_group]), sizeof(gni_securityGroup));
                    sscanf(grouptok, "%128[0-9]-%128s", newgroups[curr_group].accountId, newgroups[curr_group].name);
                    hash_b64enc_string(grouptok, &chainhash);
                    if (chainhash) {
                        snprintf(newgroups[curr_group].chainname, 32, "EU_%s", chainhash);
                        EUCA_FREE(chainhash);
                    }

                    toka = strtok_r(NULL, " ", &ptra);
                    while (toka) {
                        newip = dot2hex(toka);
                        if (newip) {
                            newgroups[curr_group].member_ips[newgroups[curr_group].max_member_ips] = dot2hex(toka);
                            newgroups[curr_group].member_public_ips[newgroups[curr_group].max_member_ips] = 0;
                            newgroups[curr_group].member_local[newgroups[curr_group].max_member_ips] = 0;
                            newgroups[curr_group].max_member_ips++;
                        }
                        toka = strtok_r(NULL, " ", &ptra);
                    }
                }
            }
        }
        fclose(FH);
    }

    if (ret == 0) {
        int i, j;
        for (i = 0; i < config->max_security_groups; i++) {
            group = &(config->security_groups[i]);
            for (j = 0; j < group->max_grouprules; j++) {
                if (group->grouprules[j]) {
                    group->grouprules[j][0] = '\0';
                    //   EUCA_FREE(group->grouprules[j]);
                }
            }
        }
        if (config->security_groups)
            EUCA_FREE(config->security_groups);
        config->security_groups = newgroups;
        config->max_security_groups = max_newgroups;
    }
    // now do RULE pass
    FH = fopen(file, "r");
    if (!FH) {
        ret = 1;
    } else {
        while (fgets(buf, MAX_PATH, FH)) {
            modetok = strtok_r(buf, " ", &ptra);
            grouptok = strtok_r(NULL, " ", &ptra);
            rulebuf[0] = '\0';

            if (modetok && grouptok) {
                if (!strcmp(modetok, "RULE")) {
                    gidx = -1;
                    hash_b64enc_string(grouptok, &chainhash);
                    if (chainhash) {
                        snprintf(chainname, 32, "EU_%s", chainhash);
                        for (i = 0; i < config->max_security_groups; i++) {
                            group = &(config->security_groups[i]);
                            if (!strcmp(group->chainname, chainname)) {
                                gidx = i;
                                break;
                            }
                        }
                        EUCA_FREE(chainhash);
                    }
                    if (gidx >= 0) {
                        toka = strtok_r(NULL, " ", &ptra);
                        while (toka) {
                            strncat(rulebuf, toka, 2048);
                            strncat(rulebuf, " ", 2048);
                            toka = strtok_r(NULL, " ", &ptra);
                        }
                        rc = ruleconvert(rulebuf, newrule);
                        if (rc) {
                            LOGERROR("could not convert rule (%s)\n", SP(rulebuf));
                        } else {
                            snprintf(config->security_groups[gidx].grouprules[config->security_groups[gidx].max_grouprules], 1024, "%s", newrule);
                            config->security_groups[gidx].max_grouprules++;
                        }
                    }
                }
            }
        }
        fclose(FH);
    }

    sec_groups_print(config->security_groups, config->max_security_groups);

    return (ret);
}

//!
//! Function description.
//!
//! @param[in] rulebuf
//! @param[in] outrule
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
int ruleconvert(char *rulebuf, char *outrule)
{
    int ret = 0;
    char proto[64], portrange[64], sourcecidr[64], icmptyperange[64], sourceowner[64], sourcegroup[64], newrule[4097], buf[2048];
    char *ptra = NULL, *toka = NULL, *idx = NULL;

    proto[0] = portrange[0] = sourcecidr[0] = icmptyperange[0] = newrule[0] = sourceowner[0] = sourcegroup[0] = '\0';

    if ((idx = strchr(rulebuf, '\n'))) {
        *idx = '\0';
    }

    toka = strtok_r(rulebuf, " ", &ptra);
    while (toka) {
        if (!strcmp(toka, "-P")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka)
                snprintf(proto, 64, "%s", toka);
        } else if (!strcmp(toka, "-p")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka)
                snprintf(portrange, 64, "%s", toka);
            if ((idx = strchr(portrange, '-'))) {
                char minport[64], maxport[64];
                sscanf(portrange, "%[0-9]-%[0-9]", minport, maxport);
                if (!strcmp(minport, maxport)) {
                    snprintf(portrange, 64, "%s", minport);
                } else {
                    *idx = ':';
                }
            }
        } else if (!strcmp(toka, "-s")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka)
                snprintf(sourcecidr, 64, "%s", toka);
            if (!strcmp(sourcecidr, "0.0.0.0/0")) {
                sourcecidr[0] = '\0';
            }
        } else if (!strcmp(toka, "-t")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka)
                snprintf(icmptyperange, 64, "any");
        } else if (!strcmp(toka, "-o")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka)
                snprintf(sourcegroup, 64, toka);
        } else if (!strcmp(toka, "-u")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka)
                snprintf(sourceowner, 64, toka);
        }
        toka = strtok_r(NULL, " ", &ptra);
    }

    LOGDEBUG("PROTO: %s PORTRANGE: %s SOURCECIDR: %s ICMPTYPERANGE: %s SOURCEOWNER: %s SOURCEGROUP: %s\n", proto, portrange, sourcecidr, icmptyperange, sourceowner, sourcegroup);

    // check if enough info is present to construct rule
    if (strlen(proto) && (strlen(portrange) || strlen(icmptyperange))) {
        if (strlen(sourcecidr)) {
            snprintf(buf, 2048, "-s %s ", sourcecidr);
            strncat(newrule, buf, 2048);
        }
        if (strlen(sourceowner) && strlen(sourcegroup)) {
            char ug[64], *chainhash = NULL;
            snprintf(ug, 64, "%s-%s", sourceowner, sourcegroup);
            hash_b64enc_string(ug, &chainhash);
            if (chainhash) {
                snprintf(buf, 2048, "-m set --set EU_%s src ", chainhash);
                strncat(newrule, buf, 2048);
                EUCA_FREE(chainhash);
            }
        }
        if (strlen(proto)) {
            snprintf(buf, 2048, "-p %s -m %s ", proto, proto);
            strncat(newrule, buf, 2048);
        }
        if (strlen(portrange)) {
            snprintf(buf, 2048, "--dport %s ", portrange);
            strncat(newrule, buf, 2048);
        }
        if (strlen(icmptyperange)) {
            snprintf(buf, 2048, "--icmp-type %s ", icmptyperange);
            strncat(newrule, buf, 2048);
        }

        while (newrule[strlen(newrule) - 1] == ' ') {
            newrule[strlen(newrule) - 1] = '\0';
        }

        snprintf(outrule, 2048, "%s", newrule);
        LOGDEBUG("CONVERTED RULE: %s\n", outrule);
    } else {
        LOGWARN("not enough information in RULE to construct iptables rule\n");
        ret = 1;
    }

    return (ret);
}

//!
//! Function description.
//!
//! @param[in] newgroups
//! @param[in] max_newgroups
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
void sec_groups_print(gni_securityGroup * newgroups, int max_newgroups)
{
    int i, j;
    char *strptra = NULL, *strptrb = NULL;

    if (log_level_get() == EUCA_LOG_TRACE) {
        for (i = 0; i < max_newgroups; i++) {
            LOGTRACE("GROUPNAME: %s GROUPACCOUNTID: %s GROUPCHAINNAME: %s\n", newgroups[i].name, newgroups[i].accountId, newgroups[i].chainname);
            for (j = 0; j < newgroups[i].max_member_ips; j++) {
                strptra = hex2dot(newgroups[i].member_ips[j]);
                strptrb = hex2dot(newgroups[i].member_public_ips[j]);
                LOGTRACE("\tIP MEMBER: %s (%s)\n", strptra, strptrb);
                EUCA_FREE(strptra);
                EUCA_FREE(strptrb);
            }
            for (j = 0; j < newgroups[i].max_grouprules; j++) {
                LOGTRACE("\tRULE: %s\n", newgroups[i].grouprules[j]);
            }
        }
    }
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
    struct dirent dent, *result = NULL;
    DIR *DH = NULL;
    FILE *FH = NULL;
    char *ret = NULL, *tmpstr = NULL, macstr[64], mac_file[MAX_PATH], *strptra = NULL, *strptrb = NULL;
    int rc, match;

    if (!mac) {
        return (NULL);
    }

    DH = opendir("/sys/class/net/");
    if (DH) {
        rc = readdir_r(DH, &dent, &result);
        match = 0;
        while (!match && !rc && result) {
            if (strcmp(result->d_name, ".") && strcmp(result->d_name, "..")) {
                snprintf(mac_file, MAX_PATH, "/sys/class/net/%s/address", result->d_name);
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
                    LOGERROR("could not open sys interface file for read: file=%s\n", SP(mac_file));
                    ret = NULL;
                }
            }
            rc = readdir_r(DH, &dent, &result);
        }
        closedir(DH);
    } else {
        LOGERROR("could not open sys dir for read (/sys/class/net/)\n");
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
    char *ret = NULL, devpath[MAX_PATH];
    int rc;

    if (!dev) {
        return (NULL);
    }

    snprintf(devpath, MAX_PATH, "/sys/class/net/%s/address", dev);
    ret = file2str(devpath);

    return (ret);
}
