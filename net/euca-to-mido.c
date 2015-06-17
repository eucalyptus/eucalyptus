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
//! @file net/euca-to-mido.c
//! Need definition
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
#include <ctype.h>
#include <curl/curl.h>
#include <json/json.h>

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

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ebt_handler.h"
#include "euca_gni.h"
#include "midonet-api.h"
#include "euca-to-mido.h"

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
//!
//!
//! @param[in] mido
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int do_metaproxy_teardown(mido_config * mido)
{
    LOGDEBUG("RUNNING TEARDOWN\n");
    return (do_metaproxy_maintain(mido, 1));
}

//!
//!
//!
//! @param[in] mido
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int do_metaproxy_setup(mido_config * mido)
{
    return (do_metaproxy_maintain(mido, 0));
}

//!
//!
//!
//! @param[in] mido
//! @param[in] mode
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int do_metaproxy_maintain(mido_config * mido, int mode)
{
    int ret = 0, rc = 0, i = 0, dorun = 0;
    pid_t npid = 0;
    char rr[EUCA_MAX_PATH], cmd[EUCA_MAX_PATH], *pidstr = NULL, pidfile[EUCA_MAX_PATH];
    sequence_executor cmds;

    if (!mido || mode < 0 || mode > 1) {
        LOGERROR("BUG: invalid input parameters\n");
        return (1);
    }

    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);

    rc = se_init(&cmds, rr, 2, 1);

    dorun = 0;
    snprintf(pidfile, EUCA_MAX_PATH, "%s/var/run/eucalyptus/nginx_localproxy.pid", mido->eucahome);
    if (!check_file(pidfile)) {
        pidstr = file2str(pidfile);
        if (pidstr) {
            npid = atoi(pidstr);
        } else {
            npid = 0;
        }
        EUCA_FREE(pidstr);
    } else {
        npid = 0;
    }

    if (mode == 0) {
        if (npid > 1) {
            if (check_process(npid, "nginx")) {
                unlink(pidfile);
                dorun = 1;
            }
        } else {
            dorun = 1;
        }
    } else if (mode == 1) {
        if (npid > 1 && !check_process(npid, "nginx")) {
            dorun = 1;
        }
    }

    if (dorun) {
        if (mode == 0) {
            LOGDEBUG("core proxy not running, starting new core proxy\n");
            snprintf(cmd, EUCA_MAX_PATH,
                     "nginx -p . -c %s/usr/share/eucalyptus/nginx_proxy.conf -g 'pid %s/var/run/eucalyptus/nginx_localproxy.pid; env NEXTHOP=127.0.0.1; env NEXTHOPPORT=8773; env EUCAHOME=%s;'",
                     mido->eucahome, mido->eucahome, mido->eucahome);
        } else if (mode == 1) {
            LOGDEBUG("core proxy running, terminating core proxy\n");
            snprintf(cmd, EUCA_MAX_PATH, "kill %d", npid);
        }
        rc = se_add(&cmds, cmd, NULL, ignore_exit);
    } else {
        LOGDEBUG("not maintaining proxy, no action to take for pid (%d)\n", npid);
    }

    for (i = 0; i < mido->max_vpcs; i++) {
        if (strlen(mido->vpcs[i].name)) {
            dorun = 0;
            snprintf(pidfile, EUCA_MAX_PATH, "%s/var/run/eucalyptus/nginx_vpcproxy_%s.pid", mido->eucahome, mido->vpcs[i].name);
            if (!check_file(pidfile)) {
                pidstr = file2str(pidfile);
                if (pidstr) {
                    npid = atoi(pidstr);
                } else {
                    npid = 0;
                }
                EUCA_FREE(pidstr);
            } else {
                npid = 0;
            }

            if (mode == 0) {
                if (npid > 1) {
                    if (check_process(npid, "nginx")) {
                        unlink(pidfile);
                        dorun = 1;
                    }
                } else {
                    dorun = 1;
                }
            } else if (mode == 1) {
                if (npid > 1 && !check_process(npid, "nginx")) {
                    dorun = 1;
                }
            }

            if (dorun) {
                if (mode == 0) {
                    LOGDEBUG("VPC (%s) proxy not running, starting new proxy\n", mido->vpcs[i].name);
                    snprintf(cmd, EUCA_MAX_PATH,
                             "ip netns exec %s nginx -p . -c %s/usr/share/eucalyptus/nginx_proxy.conf -g 'pid %s/var/run/eucalyptus/nginx_vpcproxy_%s.pid; env VPCID=%s; env NEXTHOP=169.254.0.1; env NEXTHOPPORT=31338; env EUCAHOME=%s;'",
                             mido->vpcs[i].name, mido->eucahome, mido->eucahome, mido->vpcs[i].name, mido->vpcs[i].name, mido->eucahome);
                } else if (mode == 1) {
                    LOGDEBUG("VPC (%s) proxy running, terminating VPC proxy\n", mido->vpcs[i].name);
                    snprintf(cmd, EUCA_MAX_PATH, "kill %d", npid);
                }
                rc = se_add(&cmds, cmd, NULL, ignore_exit);
            } else {
                LOGDEBUG("not maintaining VPC (%s) proxy, no action to take on pid (%d)\n", mido->vpcs[i].name, npid);
            }
        }
    }

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not execute meta core bridge setup/proxy commands: see above log entries for details\n");
        ret = 1;
    }
    se_free(&cmds);

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int delete_mido_meta_core(mido_config * mido)
{
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH];
    sequence_executor cmds;

    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);

    rc = se_init(&cmds, rr, 2, 1);

    snprintf(cmd, EUCA_MAX_PATH, "ip link set metabr down");
    rc = se_add(&cmds, cmd, NULL, ignore_exit2);

    snprintf(cmd, EUCA_MAX_PATH, "brctl delbr metabr");
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not execute meta core bridge setup/proxy commands: see above log entries for details\n");
        ret = 1;
    }
    se_free(&cmds);

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int create_mido_meta_core(mido_config * mido)
{
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH];
    sequence_executor cmds;

    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);

    rc = se_init(&cmds, rr, 2, 1);

    snprintf(cmd, EUCA_MAX_PATH, "brctl addbr metabr");
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "ip addr add 169.254.0.1/16 dev metabr");
    rc = se_add(&cmds, cmd, NULL, ignore_exit2);

    snprintf(cmd, EUCA_MAX_PATH, "ip link set metabr up");
    rc = se_add(&cmds, cmd, NULL, ignore_exit2);

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not execute meta core bridge setup/proxy commands: see above log entries for details\n");
        ret = 1;
    }
    se_free(&cmds);

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] vpc
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int delete_mido_meta_vpc_namespace(mido_config * mido, mido_vpc * vpc)
{
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH], sid[16];
    sequence_executor cmds;

    // create a meta tap namespace/devices
    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);
    sscanf(vpc->name, "vpc-%8s", sid);

    rc = se_init(&cmds, rr, 10, 1);

    snprintf(cmd, EUCA_MAX_PATH, "ip link del vn2_%s", sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "ip netns del %s", vpc->name);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not execute netns for VPC or create/ip assign commands: see above log entries for details\n");
        ret = 1;
    }
    se_free(&cmds);

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] vpc
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int create_mido_meta_vpc_namespace(mido_config * mido, mido_vpc * vpc)
{
    int ret = 0, rc = 0;
    u32 nw, ip;
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH], *ipstr = NULL, sid[16];
    sequence_executor cmds;

    // create a meta tap namespace/devices
    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);
    sscanf(vpc->name, "vpc-%8s", sid);

    rc = se_init(&cmds, rr, 2, 1);

    snprintf(cmd, EUCA_MAX_PATH, "ip netns add %s", vpc->name);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "ip link add vn2_%s type veth peer name vn3_%s", sid, sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "ip link set vn2_%s up", sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "brctl addif metabr vn2_%s", sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "ip link set vn3_%s netns %s", sid, vpc->name);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    nw = dot2hex("169.254.0.0");
    ip = nw + vpc->rtid;
    ipstr = hex2dot(ip);
    snprintf(cmd, EUCA_MAX_PATH, "ip netns exec %s ip addr add %s/16 dev vn3_%s", vpc->name, ipstr, sid);
    EUCA_FREE(ipstr);
    se_add(&cmds, cmd, NULL, ignore_exit2);

    snprintf(cmd, EUCA_MAX_PATH, "ip netns exec %s ip link set vn3_%s up", vpc->name, sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not execute netns for VPC or create/ip assign commands: see above log entries for details\n");
        ret = 1;
    }

    se_free(&cmds);

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] name
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int delete_mido_meta_subnet_veth(mido_config * mido, char *name)
{
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH], sid[16];
    sequence_executor cmds;

    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);
    sscanf(name, "subnet-%8s", sid);

    rc = se_init(&cmds, rr, 2, 1);

    snprintf(cmd, EUCA_MAX_PATH, "ip link del vn0_%s", sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not delete subnet tap ifaces: see above log entries for details\n");
        ret = 1;
    }
    se_free(&cmds);

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] vpc
//! @param[in] name
//! @param[in] subnet
//! @param[in] slashnet
//! @param[in] tapiface
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int create_mido_meta_subnet_veth(mido_config * mido, mido_vpc * vpc, char *name, char *subnet, char *slashnet, char **tapiface)
{
    int ret = 0, rc = 0;
    u32 nw, gw;
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH], *gateway = NULL, sid[16];
    sequence_executor cmds;

    // create a meta tap
    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);
    sscanf(name, "subnet-%8s", sid);

    rc = se_init(&cmds, rr, 2, 1);

    //  snprintf(cmd, EUCA_MAX_PATH, "ssh root@h-41 ip link add vn0_%s type veth peer name vn1_%s", sid, sid);
    snprintf(cmd, EUCA_MAX_PATH, "ip link add vn0_%s type veth peer name vn1_%s", sid, sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    //  snprintf(cmd, EUCA_MAX_PATH, "ssh root@h-41 ip link set vn0_%s up", sid);
    snprintf(cmd, EUCA_MAX_PATH, "ip link set vn0_%s up", sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "ip link set vn1_%s netns %s", sid, vpc->name);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    nw = dot2hex(subnet);
    gw = nw + 2;
    gateway = hex2dot(gw);
    //  snprintf(cmd, EUCA_MAX_PATH, "ssh root@h-41 ip addr add %s/%s dev vn1_%s", gateway, slashnet, sid);
    snprintf(cmd, EUCA_MAX_PATH, "ip netns exec %s ip addr add %s/%s dev vn1_%s", vpc->name, gateway, slashnet, sid);
    EUCA_FREE(gateway);
    se_add(&cmds, cmd, NULL, ignore_exit2);

    snprintf(cmd, EUCA_MAX_PATH, "ip netns exec %s ip link set vn1_%s up", vpc->name, sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not execute tap interface create/ip assign commands: see above log entries for details\n");
        *tapiface = NULL;
        ret = 1;
    } else {
        *tapiface = calloc(16, sizeof(char));
        snprintf(*tapiface, 16, "vn0_%s", sid);
    }
    se_free(&cmds);

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int do_midonet_populate(mido_config * mido)
{
    int i = 0, j = 0, k = 0, rc = 0, rtid = 0, ret = 0;
    char subnetname[16], vpcname[16], chainname[16];
    char instanceId[16], deviceId[16];
    char *iface = NULL, *devid = NULL;
    mido_vpc_secgroup *vpcsecgroup = NULL;
    mido_vpc_instance *vpcinstance = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc *vpc = NULL;

    // mido discovery
    rc = discover_mido_resources(mido);
    if (rc) {
        LOGERROR("could not discover resources from midonet: check midonet health\n");
        return (1);
    }
    // always populate the core from that which was discovered
    rc = populate_mido_core(mido, mido->midocore);
    if (rc) {
        LOGERROR("could not populate midonet core (eucabr, eucart): check midonet health\n");
        return (1);
    }

    if (mido->setupcore) {
        // create the core (only the things that aren't already there)
        rc = create_mido_core(mido, mido->midocore);
        if (rc) {
            LOGERROR("cannot setup midonet core router/bridge: check midonet health\n");
            return (1);
        }
    } else {
        LOGDEBUG("skipping the create of midocore (see MIDOSETUPCORE parameter in eucalyptus.conf)\n");
    }

    // pattern
    // - find all VPC routers (and populate VPCs)
    // - for each VPC, find all subnets (and populate subnets)
    // - for each VPC, for each subnet, find all instances (and populate instances)

    // VPCs
    for (i = 0; i < mido->max_routers; i++) {
        LOGDEBUG("inspecting mido router '%s'\n", mido->routers[i].name);

        bzero(vpcname, 16);

        sscanf(mido->routers[i].name, "vr_%12s_%d", vpcname, &rtid);
        if (strlen(vpcname)) {
            mido->vpcs = realloc(mido->vpcs, sizeof(mido_vpc) * (mido->max_vpcs + 1));
            vpc = &(mido->vpcs[mido->max_vpcs]);
            bzero(vpc, sizeof(mido_vpc));
            mido->max_vpcs++;
            LOGINFO("discovered VPC installed in midonet: %s\n", vpcname);

            snprintf(vpc->name, sizeof(vpc->name), "%s", vpcname);
            set_router_id(mido, rtid);
            vpc->rtid = rtid;
            rc = populate_mido_vpc(mido, mido->midocore, vpc);
            if (rc) {
                LOGERROR("cannot populate midonet VPC '%s': check midonet health\n", vpc->name);
                ret = 1;
            }
        }
    }

    // SUBNETS
    for (i = 0; i < mido->max_bridges; i++) {
        LOGDEBUG("inspecting bridge '%s'\n", mido->bridges[i].name);

        bzero(vpcname, 16);
        bzero(subnetname, 16);

        sscanf(mido->bridges[i].name, "vb_%12s_%15s", vpcname, subnetname);
        if (strlen(vpcname) && strlen(subnetname)) {
            LOGINFO("discovered VPC subnet installed in midonet: %s/%s\n", vpcname, subnetname);
            find_mido_vpc(mido, vpcname, &vpc);
            if (vpc) {
                LOGDEBUG("found VPC matching discovered subnet: '%s'/'%s'\n", vpc->name, subnetname);
                vpc->subnets = realloc(vpc->subnets, sizeof(mido_vpc_subnet) * (vpc->max_subnets + 1));
                vpcsubnet = &(vpc->subnets[vpc->max_subnets]);
                vpc->max_subnets++;
                bzero(vpcsubnet, sizeof(mido_vpc_subnet));
                snprintf(vpcsubnet->name, 16, "%s", subnetname);
                rc = populate_mido_vpc_subnet(mido, vpc, vpcsubnet);
                if (rc) {
                    LOGERROR("cannot populate midonet VPC '%s' subnet '%s': check midonet health\n", vpc->name, vpcsubnet->name);
                    ret = 1;
                }
            }
        }
    }

    // SECGROUPS
    for (i = 0; i < mido->max_chains; i++) {
        LOGDEBUG("inspecting chain '%s'\n", mido->chains[i].name);
        bzero(chainname, 16);

        sscanf(mido->chains[i].name, "sg_ingress_%11s", chainname);
        if (strlen(chainname)) {
            LOGINFO("discovered VPC security group installed in midonet: %s\n", chainname);
            mido->vpcsecgroups = realloc(mido->vpcsecgroups, sizeof(mido_vpc_secgroup) * (mido->max_vpcsecgroups + 1));
            vpcsecgroup = &(mido->vpcsecgroups[mido->max_vpcsecgroups]);
            mido->max_vpcsecgroups++;
            bzero(vpcsecgroup, sizeof(mido_vpc_secgroup));
            snprintf(vpcsecgroup->name, 16, "%s", chainname);
            rc = populate_mido_vpc_secgroup(mido, vpcsecgroup);
            if (rc) {
                LOGERROR("cannot populate mido SG '%s': check midonet health\n", vpcsecgroup->name);
                ret = 1;
            }
        }
    }

    // INSTANCES
    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            for (k = 0; k < vpcsubnet->max_brports; k++) {

                bzero(instanceId, 16);
                bzero(deviceId, 16);

                rc = mido_getel_midoname(&(vpcsubnet->brports[k]), "interfaceName", &iface);
                rc = mido_getel_midoname(&(vpcsubnet->brports[k]), "deviceId", &devid);

                if ((iface && devid) && !strcmp(devid, vpcsubnet->midos[VPCBR].uuid)) {
                    sscanf(iface, "vn_%s", instanceId);
                    snprintf(deviceId, 16, "%s", devid);

                    if (strlen(instanceId) && strlen(devid)) {
                        LOGINFO("discovered VPC subnet instance interface: %s/%s/%s\n", vpc->name, vpcsubnet->name, instanceId);

                        vpcsubnet->instances = realloc(vpcsubnet->instances, sizeof(mido_vpc_instance) * (vpcsubnet->max_instances + 1));
                        vpcinstance = &(vpcsubnet->instances[vpcsubnet->max_instances]);
                        bzero(vpcinstance, sizeof(mido_vpc_instance));
                        vpcsubnet->max_instances++;
                        snprintf(vpcinstance->name, INSTANCE_ID_LEN, "%s", instanceId);

                        rc = populate_mido_vpc_instance(mido, mido->midocore, vpc, vpcsubnet, vpcinstance);
                        if (rc) {
                            LOGERROR("could not populate instance: check mido health\n");
                            ret = 1;
                        }
                    }
                }
                EUCA_FREE(iface);
                EUCA_FREE(devid);
            }
        }
    }

    // END population phase

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int do_midonet_teardown(mido_config * mido)
{
    int ret = 0, rc = 0, i = 0;

    rc = do_midonet_populate(mido);
    if (rc) {
        LOGERROR("cannot populate prior to teardown: check midonet health\n");
        return (1);
    }

    rc = do_metaproxy_teardown(mido);
    if (rc) {
        LOGERROR("cannot teardown meta proxies: see above log for details\n");
    }

    for (i = 0; i < mido->max_vpcs; i++) {
        delete_mido_vpc(mido, &(mido->vpcs[i]));
    }

    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        delete_mido_vpc_secgroup(&(mido->vpcsecgroups[i]));
    }

    if (mido->setupcore) {
        delete_mido_core(mido, mido->midocore);
    } else {
        LOGDEBUG("skipping the delete of midocore (see MIDOSETUPCORE param in eucalyptus.conf)\n");
    }

    free_mido_config(mido);

    return (ret);
}

//!
//!
//!
//! @param[in] gni
//! @param[in] mido
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int do_midonet_update(globalNetworkInfo * gni, mido_config * mido)
{
    int i = 0, j = 0, k = 0, rc = 0;
    char subnet_buf[24], slashnet_buf[8], gw_buf[24], pt_buf[24];
    mido_vpc_secgroup *vpcsecgroup = NULL;
    mido_vpc_instance *vpcinstance = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc *vpc = NULL;

    if (!gni || !mido) {
        return (1);
    }

    mido->enabledCLCIp = gni->enabledCLCIp;

    rc = do_midonet_populate(mido);
    if (rc) {
        LOGERROR("could not populate prior to update: see above log entries for details\n");
        return (1);
    }

    // temporary print
    LOGDEBUG("TOTAL SEC. GROUPS: %d\n", mido->max_vpcsecgroups);
    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        vpcsecgroup = &(mido->vpcsecgroups[i]);
        print_mido_vpc_secgroup(vpcsecgroup);
    }

    LOGDEBUG("TOTAL VPCS: %d\n", mido->max_vpcs);
    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        print_mido_vpc(vpc);
        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            print_mido_vpc_subnet(vpcsubnet);
            for (k = 0; k < vpcsubnet->max_instances; k++) {
                vpcinstance = &(vpcsubnet->instances[k]);
                print_mido_vpc_instance(vpcinstance);
            }
        }
    }

    // new thing: pass1 - tag everything that is both in GNI and in MIDO
    for (i = 0; i < gni->max_vpcs; i++) {
        gni_vpc *gnivpc = NULL;
        gnivpc = &(gni->vpcs[i]);
        rc = find_mido_vpc(mido, gnivpc->name, &vpc);

        if (rc) {
            LOGDEBUG("pass1: global VPC %s in mido: N\n", gnivpc->name);
        } else {
            LOGDEBUG("pass1: global VPC %s in mido: Y\n", gnivpc->name);
            vpc->gniVpc = gnivpc;
            vpc->gnipresent = 1;
        }

        for (j = 0; j < gnivpc->max_subnets; j++) {
            gni_vpcsubnet *gnivpcsubnet = NULL;
            gnivpcsubnet = &(gnivpc->subnets[j]);
            rc = find_mido_vpc_subnet(vpc, gnivpcsubnet->name, &vpcsubnet);
            if (rc) {
                LOGDEBUG("pass1: global VPC SUBNET %s in mido: N\n", gnivpcsubnet->name);
            } else {
                LOGDEBUG("pass1: global VPC SUBNET %s in mido: Y\n", gnivpcsubnet->name);
                vpcsubnet->gniSubnet = gnivpcsubnet;
                vpcsubnet->gnipresent = 1;
            }
            
        }
    }

    for (i = 0; i < gni->max_instances; i++) {
        gni_instance *gniinstance = NULL;
        gniinstance = &(gni->instances[i]);

        rc = find_mido_vpc_instance_global(mido, gniinstance->name, &vpcinstance);
        if (rc) {
            LOGDEBUG("pass1: global VPC INSTANCE %s in mido: N\n", gniinstance->name);
        } else {
            LOGDEBUG("pass1: global VPC INSTANCE %s in mido: Y\n", gniinstance->name);
            vpcinstance->gniInst = gniinstance;
            vpcinstance->gnipresent = 1;
        }
    }

    for (i = 0; i < gni->max_secgroups; i++) {
        gni_secgroup *gnisecgroup = NULL;
        gnisecgroup = &(gni->secgroups[i]);
        rc = find_mido_vpc_secgroup(mido, gnisecgroup->name, &vpcsecgroup);
        if (rc) {
            LOGDEBUG("pass1: global VPC SECGROUP %s in mido: N\n", gnisecgroup->name);
        } else {
            LOGDEBUG("pass1: global VPC SECGROUP %s in mido: Y\n", gnisecgroup->name);
            vpcsecgroup->gniSecgroup = gnisecgroup;
            vpcsecgroup->gnipresent = 1;
        }
    }


    // new thing: pass2 - remove anything in MIDO that is not in GNI
    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        vpcsecgroup = &(mido->vpcsecgroups[i]);
        if (!vpcsecgroup->gnipresent) {
            LOGDEBUG("pass2: mido VPC SECGROUP %s in global: N (deleting)\n", vpcsecgroup->name);
            rc = delete_mido_vpc_secgroup(vpcsecgroup);
        } else {
            LOGDEBUG("pass2: mido VPC SECGROUP %s in global: Y\n", vpcsecgroup->name);

            // remove any IPs from mido SG that are not in GNI SG instance list
            {
                int secgroupinstances_max=0;
                gni_instance *secgroupinstances=NULL;
                gni_secgroup *gnisecgroup=NULL;
                midoname *pubips=NULL, *privips=NULL, *allips=NULL;
                int max_pubips=0, max_privips=0, max_allips=0;
                int found=0;
                
                gnisecgroup = vpcsecgroup->gniSecgroup;
                
                rc = mido_get_ipaddrgroup_ips(&(vpcsecgroup->midos[VPCSG_IAGPRIV]), &privips, &max_privips);
                rc = mido_get_ipaddrgroup_ips(&(vpcsecgroup->midos[VPCSG_IAGPUB]), &pubips, &max_pubips);
                rc = mido_get_ipaddrgroup_ips(&(vpcsecgroup->midos[VPCSG_IAGALL]), &allips, &max_allips);
                rc = gni_secgroup_get_instances(gni, gnisecgroup, NULL, 0, NULL, 0, &secgroupinstances, &secgroupinstances_max);
                
                for (k=0; k<max_pubips; k++) {
                    char pubip_mido[32];
                    sscanf(pubips[k].name, "versions/6/ip_addrs/%s", pubip_mido);
                    
                    found=0;
                    for (j=0; j<secgroupinstances_max && !found; j++) {
                        char *pubip_gni=NULL;
                        pubip_gni = hex2dot(secgroupinstances[j].publicIp);
                        
                        if (!strcmp(pubip_mido, pubip_gni)) {
                            LOGDEBUG("pass2: mido VPC SECGROUP %s member public IP %s in global: Y\n", vpcsecgroup->name, pubip_mido);
                            found++;
                        }
                        EUCA_FREE(pubip_gni);
                    }
                    if (!found) {
                        LOGDEBUG("pass2: mido VPC SECGROUP %s member public IP %s in global: N\n", vpcsecgroup->name, pubip_mido);
                        mido_delete_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGPUB]), &(pubips[k]));
                    }
                }
                
                for (k=0; k<max_privips; k++) {
                    char privip_mido[32];
                    sscanf(privips[k].name, "versions/6/ip_addrs/%s", privip_mido);
                    
                    found=0;
                    for (j=0; j<secgroupinstances_max && !found; j++) {
                        char *privip_gni=NULL;
                        privip_gni = hex2dot(secgroupinstances[j].privateIp);
                        
                        if (!strcmp(privip_mido, privip_gni)) {
                            LOGDEBUG("pass2: mido VPC SECGROUP %s member private IP %s in global: Y\n", vpcsecgroup->name, privip_mido);
                            found++;
                        }
                        EUCA_FREE(privip_gni);
                    }
                    if (!found) {
                        LOGDEBUG("pass2: mido VPC SECGROUP %s member private IP %s in global: N\n", vpcsecgroup->name, privip_mido);
                        mido_delete_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGPRIV]), &(privips[k]));
                    }
                }
                
                for (k=0; k<max_allips; k++) {
                    char allip_mido[32];
                    sscanf(allips[k].name, "versions/6/ip_addrs/%s", allip_mido);
                    
                    found=0;
                    for (j=0; j<secgroupinstances_max && !found; j++) {
                        char *pubip_gni=NULL;
                        char *privip_gni=NULL;
                        pubip_gni = hex2dot(secgroupinstances[j].publicIp);
                        privip_gni = hex2dot(secgroupinstances[j].privateIp);
                        
                        if (!strcmp(allip_mido, pubip_gni)) {
                            LOGDEBUG("pass2: mido VPC SECGROUP %s member all IP %s in global: Y\n", vpcsecgroup->name, allip_mido);
                            found++;
                        }
                        EUCA_FREE(pubip_gni);
                        
                        if (!strcmp(allip_mido, privip_gni)) {
                            LOGDEBUG("pass2: mido VPC SECGROUP %s member all IP %s in global: Y\n", vpcsecgroup->name, allip_mido);
                            found++;
                        }
                        EUCA_FREE(privip_gni);
                    }
                    if (!found) {
                        LOGDEBUG("pass2: mido VPC SECGROUP %s member all IP %s in global: N\n", vpcsecgroup->name, allip_mido);
                        mido_delete_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGALL]), &(allips[k]));
                    }
                }
                    
                EUCA_FREE(secgroupinstances);
                mido_free_midoname_list(pubips, max_pubips);
                EUCA_FREE(pubips);
                mido_free_midoname_list(privips, max_privips);
                EUCA_FREE(privips);
                mido_free_midoname_list(allips, max_allips);
                EUCA_FREE(allips);
            }
        }
    }
    
    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);        
        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            for (k = 0; k < vpcsubnet->max_instances; k++) {
                vpcinstance = &(vpcsubnet->instances[k]);
                if (!vpc->gnipresent || !vpcsubnet->gnipresent || !vpcinstance->gnipresent) {
                    LOGDEBUG("pass2: mido VPC INSTANCE %s in global: N (deleting)\n", vpcinstance->name);
                    rc = delete_mido_vpc_instance(vpcinstance);
                } else {
                    LOGDEBUG("pass2: mido VPC INSTANCE %s in global: Y\n", vpcinstance->name);
                }
            }
            if (!vpc->gnipresent || !vpcsubnet->gnipresent) {
                LOGDEBUG("pass2: mido VPC SUBNET %s in global: N (deleting)\n", vpcsubnet->name);
                rc = delete_mido_vpc_subnet(mido, vpcsubnet);
            } else {
                LOGDEBUG("pass2: mido VPC SUBNET %s in global: Y\n", vpcsubnet->name);
            }
        }
        if (!vpc->gnipresent) {
            LOGDEBUG("pass2: mido VPC %s in global: N (deleting)\n", vpc->name);
            rc = do_metaproxy_teardown(mido);
            if (rc) {
                LOGERROR("cannot teardown metadata proxies: see above log for details\n");
            }
            rc = delete_mido_vpc(mido, vpc);
        } else {
            LOGDEBUG("pass2: mido VPC %s in global: Y\n", vpc->name);
        }
    }

    // now, go through GNI and create new VPCs
    LOGINFO("initializing VPCs (%d)\n", gni->max_vpcs);

    for (i = 0; i < gni->max_vpcs; i++) {
        gni_vpc *gnivpc = NULL;
        gni_vpcsubnet *gnivpcsubnet = NULL;
        mido_vpc *vpc = NULL;
        mido_vpc_subnet *vpcsubnet = NULL;

        gnivpc = &(gni->vpcs[i]);
        LOGINFO("initializing VPC '%s' with '%d' subnets\n", gnivpc->name, gnivpc->max_subnets);

        rc = find_mido_vpc(mido, gnivpc->name, &vpc);
        if (vpc) {
            LOGINFO("found gni VPC '%s' already extant\n", gnivpc->name);
        } else {
            LOGINFO("creating new VPC '%s'\n", gnivpc->name);
            mido->vpcs = realloc(mido->vpcs, sizeof(mido_vpc) * (mido->max_vpcs + 1));
            vpc = &(mido->vpcs[mido->max_vpcs]);
            bzero(vpc, sizeof(mido_vpc));
            mido->max_vpcs++;

            snprintf(vpc->name, 16, "%s", gnivpc->name);
            get_next_router_id(mido, &(vpc->rtid));
        }

        rc = create_mido_vpc(mido, mido->midocore, vpc);
        if (rc) {
            LOGERROR("failed to create VPC '%s': check midonet health\n", gnivpc->name);
        }
        vpc->gnipresent = 1;

        // do subnets
        for (j = 0; j < gnivpc->max_subnets; j++) {
            gnivpcsubnet = &(gnivpc->subnets[j]);

            rc = find_mido_vpc_subnet(vpc, gnivpcsubnet->name, &vpcsubnet);
            if (vpcsubnet) {
                LOGINFO("found gni VPC '%s' subnet '%s' already extant\n", vpc->name, vpcsubnet->name);
                vpcsubnet->gniSubnet = gnivpcsubnet;
            } else {
                LOGINFO("creating new VPC '%s' subnet '%s'\n", vpc->name, gnivpc->subnets[j].name);

                vpc->subnets = realloc(vpc->subnets, sizeof(mido_vpc_subnet) * (vpc->max_subnets + 1));
                vpcsubnet = &(vpc->subnets[vpc->max_subnets]);
                vpc->max_subnets++;
                bzero(vpcsubnet, sizeof(mido_vpc_subnet));
                snprintf(vpcsubnet->name, 16, "%s", gnivpc->subnets[j].name);
                vpcsubnet->gniSubnet = gnivpcsubnet;
            }

            subnet_buf[0] = slashnet_buf[0] = gw_buf[0] = '\0';
            cidr_split(gnivpcsubnet->cidr, subnet_buf, slashnet_buf, gw_buf, NULL);

            rc = create_mido_vpc_subnet(mido, vpc, vpcsubnet, subnet_buf, slashnet_buf, gw_buf, gni->instanceDNSDomain, gni->instanceDNSServers, gni->max_instanceDNSServers);
            if (rc) {
                LOGERROR("failed to create VPC '%s' subnet '%s': check midonet health\n", gnivpc->name, gnivpc->subnets[j].name);
            }
            vpcsubnet->gnipresent = 1;
        }
    }

    // now add sec. groups
    for (i = 0; i < gni->max_secgroups; i++) {
        gni_secgroup *gnisecgroup = &(gni->secgroups[i]);
        
        // create the SG
        rc = find_mido_vpc_secgroup(mido, gnisecgroup->name, &vpcsecgroup);
        if (vpcsecgroup) {
            // found one
            vpcsecgroup->gniSecgroup = gnisecgroup;
        } else {
            mido->vpcsecgroups = realloc(mido->vpcsecgroups, sizeof(mido_vpc_secgroup) * (mido->max_vpcsecgroups + 1));
            vpcsecgroup = &(mido->vpcsecgroups[mido->max_vpcsecgroups]);
            bzero(vpcsecgroup, sizeof(mido_vpc_secgroup));
            mido->max_vpcsecgroups++;
            snprintf(vpcsecgroup->name, SECURITY_GROUP_ID_LEN, "%s", gnisecgroup->name);
            vpcsecgroup->gniSecgroup = gnisecgroup;
        }
        
        LOGDEBUG("ABOUT TO CREATE SG '%s'\n", vpcsecgroup->name);
        
        rc = create_mido_vpc_secgroup(mido, vpcsecgroup);
        if (rc) {
            // TODO
        }
        vpcsecgroup->gnipresent = 1;
    }

    // now do instances
    FILE *PFH = NULL;
    char mapfile[EUCA_MAX_PATH];
    snprintf(mapfile, EUCA_MAX_PATH, "%s/var/run/eucalyptus/eucanetd_vpc_instance_ip_map", mido->eucahome);
    unlink(mapfile);
    PFH = fopen(mapfile, "w");

    for (i = 0; i < gni->max_instances; i++) {
        gni_instance *gniinstance = &(gni->instances[i]);

        LOGDEBUG("inspecting gni instance '%s'\n", gni->instances[i].name);

        // check that we can do something about this instance:
        if (gniinstance->vpc && strlen(gniinstance->vpc) && gniinstance->nodehostname && strlen(gniinstance->nodehostname)) {
            rc = find_mido_vpc(mido, gniinstance->vpc, &vpc);
            if (vpc) {
                rc = find_mido_vpc_subnet(vpc, gniinstance->subnet, &vpcsubnet);
                if (vpcsubnet) {
                    rc = find_mido_vpc_instance(vpcsubnet, gniinstance->name, &vpcinstance);
                    if (vpcinstance) {
                        LOGDEBUG("found instance '%s' is in extant vpc '%s' subnet '%s' '%d'\n", vpcinstance->name, vpc->name, vpcsubnet->name, vpcinstance->midos[VMHOST].init);
                        vpcinstance->gniInst = gniinstance;
                    } else {
                        // create the instance
                        vpcsubnet->instances = realloc(vpcsubnet->instances, sizeof(mido_vpc_instance) * (vpcsubnet->max_instances + 1));
                        vpcinstance = &(vpcsubnet->instances[vpcsubnet->max_instances]);
                        bzero(vpcinstance, sizeof(mido_vpc_instance));
                        vpcsubnet->max_instances++;
                        snprintf(vpcinstance->name, INSTANCE_ID_LEN, "%s", gniinstance->name);
                        vpcinstance->gniInst = gniinstance;
                    }

                    LOGDEBUG("ABOUT TO CREATE INSTANCE '%s' ON HOST '%s'\n", vpcinstance->name, gniinstance->nodehostname);
                    rc = create_mido_vpc_instance(mido, vpcinstance, gniinstance->nodehostname);
                    if (rc) {
                        // TODO
                    }

                    vpcinstance->gnipresent = 1;

                    // add to proxy instance-map
                    char *privIp = NULL;
                    privIp = hex2dot(gniinstance->privateIp);
                    fprintf(PFH, "%s %s %s\n", vpc->name, vpcinstance->name, privIp);
                    EUCA_FREE(privIp);

                    // do instance<->port connection and elip
                    if (vpcinstance->midos[VMHOST].init) {
                        LOGINFO("connecting gni host '%s' with midonet host '%s' interface for instance '%s'\n", gniinstance->nodehostname, vpcinstance->midos[VMHOST].name,
                                gniinstance->name);

                        rc = connect_mido_vpc_instance(vpcsubnet, vpcinstance, &(vpcinstance->midos[VMHOST]));
                        if (rc) {
                            LOGERROR("cannot connect instance to midonet: check midonet health\n");
                        } else {
                            // check to see if something has changed before running the DC
                            {
                                char *pubip_gni=NULL, pubip_mido[32];
                                
                                pubip_gni = hex2dot(gniinstance->publicIp);
                                if (vpcinstance->midos[ELIP_PRE_IPADDRGROUP_IP].name && strlen(vpcinstance->midos[ELIP_PRE_IPADDRGROUP_IP].name) && pubip_gni && strlen(pubip_gni)) {
                                    sscanf(vpcinstance->midos[ELIP_PRE_IPADDRGROUP_IP].name, "versions/6/ip_addrs/%s", pubip_mido);
                                    
                                    if (!strcmp(pubip_gni, pubip_mido)) {
                                        LOGDEBUG("elips are unchanged (%s %s), skipping disconnect\n", pubip_gni, pubip_mido);
                                    } else {
                                        LOGDEBUG("detected instance (%s) ELIP change from %s to %s: running disconnect\n", vpcinstance->name, pubip_mido, pubip_gni);
                                        
                                        rc = disconnect_mido_vpc_instance_elip(vpcinstance);
                                        if (rc) {
                                            LOGERROR("cannot remove prior midonet floating IP for instance: check midonet health\n");
                                        }
                                        LOGDEBUG("disconnect done\n");
                                    }
                                }
                                EUCA_FREE(pubip_gni);
                            }

                            rc = connect_mido_vpc_instance_elip(mido, mido->midocore, vpc, vpcsubnet, vpcinstance);
                            if (rc) {
                                LOGERROR("cannot setup midonet floating IP <-> instance mapping: check midonet health\n");
                            }
                        }
                    } else {
                        LOGERROR("could not find midonet host for instance '%s': check midonet/euca node/host mappings\n", vpcinstance->name);
                    }

                    // do sec. group rule application for instance
                    {
                        gni_secgroup *gnisecgroups = NULL;
                        int max_gnisecgroups, rulepos = 1, sgrulepos = 1;
                        char tmp_name1[32], tmp_name2[32], tmp_name3[32], tmp_name4[32], tmp_buf[1024];

                        subnet_buf[0] = slashnet_buf[0] = gw_buf[0] = '\0';
                        cidr_split(vpcsubnet->gniSubnet->cidr, subnet_buf, slashnet_buf, gw_buf, pt_buf);

                        // for egress
                        rulepos = 1;

                        snprintf(tmp_name3, 32, "%d", rulepos);
                        rc = mido_create_rule(&(vpcinstance->midos[INST_PRECHAIN]), NULL, "position", tmp_name3, "type", "dnat", "flowAction", "continue", "ipAddrGroupDst",
                                              mido->midocore->midos[METADATA_IPADDRGROUP].uuid, "nwProto", "6", "tpDst", "jsonjson", "tpDst:start", "80", "tpDst:end", "80",
                                              "tpDst:END", "END", "natTargets", "jsonlist", "natTargets:addressTo", pt_buf, "natTargets:addressFrom", pt_buf, "natTargets:portFrom",
                                              "31337", "natTargets:portTo", "31337", "natTargets:END", "END", NULL);
                        if (rc) {
                        } else {
                            rulepos++;
                        }

                        snprintf(tmp_name3, 32, "%d", rulepos);
                        rc = mido_create_rule(&(vpcinstance->midos[INST_PRECHAIN]), NULL, "position", tmp_name3, "type", "accept", "matchReturnFlow", "true", NULL);
                        if (rc) {
                        } else {
                            rulepos++;
                        }

                        snprintf(tmp_name3, 32, "%d", rulepos);
                        rc = mido_create_rule(&(vpcinstance->midos[INST_PRECHAIN]), NULL, "position", tmp_name3, "type", "accept", "nwDstAddress", pt_buf, "nwDstLength", "32", NULL);
                        if (rc) {
                        } else {
                            rulepos++;
                        }

                        /*
                        snprintf(tmp_name3, 32, "%d", rulepos);
                        rc = mido_create_rule(&(vpcinstance->midos[INST_PRECHAIN]), NULL, "position", tmp_name3, "type", "accept", "ipAddrGroupSrc",
                                              vpcinstance->midos[ELIP_POST_IPADDRGROUP].uuid, "matchForwardFlow", "true", NULL);
                        if (rc) {
                        } else {
                            rulepos++;
                        }
                        */

                        rc = gni_instance_get_secgroups(gni, gniinstance, NULL, 0, NULL, 0, &gnisecgroups, &max_gnisecgroups);
                        for (j = 0; j < max_gnisecgroups; j++) {
                            gni_secgroup *gnisecgroup = &(gnisecgroups[j]);
                            
                            char *tmpstr = NULL;
                            
                            // find the SG
                            rc = find_mido_vpc_secgroup(mido, gnisecgroup->name, &vpcsecgroup);
                            if (vpcsecgroup) {
                                // found one

                                tmpstr = hex2dot(gniinstance->privateIp);
                                rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGPRIV]), tmpstr, NULL);
                                rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGALL]), tmpstr, NULL);
                                EUCA_FREE(tmpstr);
                                
                                tmpstr = hex2dot(gniinstance->publicIp);
                                if (tmpstr && strcmp(tmpstr, "0.0.0.0")) {
                                    rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGPUB]), tmpstr, NULL);
                                    rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGALL]), tmpstr, NULL);
                                }
                                EUCA_FREE(tmpstr);
                                
                                // TODO make this better (not entire clear/reset each time
                                {
                                    midoname *rules = NULL;
                                    int max_rules = 0, r = 0;

                                    rc = mido_get_rules(&(vpcsecgroup->midos[VPCSG_EGRESS]), &rules, &max_rules);
                                    if (max_rules != gnisecgroup->max_egress_rules) {
                                        for (r = 0; r < max_rules; r++) {
                                            mido_delete_rule(&(rules[r]));
                                        }
                                    }
                                    mido_free_midoname_list(rules, max_rules);
                                    EUCA_FREE(rules);
                                }
                                
                                snprintf(tmp_name3, 32, "%d", rulepos);
                                rc = mido_create_rule(&(vpcinstance->midos[INST_PRECHAIN]), NULL, "position", tmp_name3, "type", "jump", "jumpChainId",
                                                      vpcsecgroup->midos[VPCSG_EGRESS].uuid, NULL);
                                if (rc) {
                                } else {
                                    rulepos++;
                                }
                                
                                snprintf(tmp_name3, 32, "%d", rulepos);
                                rc = mido_create_rule(&(vpcinstance->midos[INST_POSTCHAIN]), NULL, "type", "drop", "invDlType", "true", "position", tmp_name3, "dlType", "2054", NULL);
                                if (rc) {
                                } else {
                                    rulepos++;
                                }
                                
                                sgrulepos = 1;
                                for (k = 0; k < gnisecgroup->max_egress_rules; k++) {
                                    // TODO other protos?
                                    
                                    snprintf(tmp_name4, 32, "%d", gnisecgroup->egress_rules[k].protocol);
                                    if (strlen(gnisecgroup->egress_rules[k].groupId)) {
                                        // other group
                                        midoname *midos = NULL;
                                        int max_midos = 0, r;
                                        char name[32], *mname = NULL;
                                        int found = 0;
                                        rc = mido_get_ipaddrgroups("euca_tenant_1", &midos, &max_midos);
                                        for (r = 0; r < max_midos && !found; r++) {
                                            snprintf(name, 32, "sg_all_%11s", vpcsecgroup->name);
                                            rc = mido_getel_midoname(&(midos[r]), "name", &mname);
                                            if (mname && !strcmp(name, mname)) {
                                                LOGTRACE("FOUND: %s/%s\n", mname, midos[r].uuid);
                                                snprintf(tmp_name3, 32, "%d", rulepos);
                                                rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_EGRESS]), NULL, "position", tmp_name3, "type", "accept", "ipAddrGroupDst", midos[r].uuid, NULL);
                                                if (rc) {
                                                } else {
                                                    sgrulepos++;
                                                }
                                                found++;
                                            }
                                            EUCA_FREE(mname);
                                        }
                                        mido_free_midoname_list(midos, max_midos);
                                        EUCA_FREE(midos);
                                    } else if (gnisecgroup->egress_rules[k].protocol == 6 || gnisecgroup->egress_rules[k].protocol == 17 || gnisecgroup->egress_rules[k].protocol == 132) {
                                        // TCP/UDP/SCTP
                                        
                                        snprintf(tmp_name1, 32, "%d", gnisecgroup->egress_rules[k].fromPort);
                                        snprintf(tmp_name2, 32, "%d", gnisecgroup->egress_rules[k].toPort);
                                        
                                        snprintf(tmp_name3, 32, "%d", sgrulepos);
                                        rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_EGRESS]), NULL, "position", tmp_name3, "type", "accept", "tpDst", "jsonjson", "tpDst:start",
                                                              tmp_name1, "tpDst:end", tmp_name2, "tpDst:END", "END", "nwProto", tmp_name4, NULL);
                                        if (rc) {
                                        } else {
                                            sgrulepos++;
                                        }
                                        
                                    } else if (gnisecgroup->egress_rules[k].protocol == 1) {
                                        // ICMP
                                        
                                        snprintf(tmp_name3, 32, "%d", sgrulepos);
                                        
                                        if (gnisecgroup->egress_rules[k].icmpCode >= 0) {
                                            snprintf(tmp_name1, 32, "%d", gnisecgroup->egress_rules[k].icmpCode);
                                            rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_EGRESS]), NULL, "position", tmp_name3, "type", "accept", "tpDst", "jsonjson",
                                                                  "tpDst:start", tmp_name1, "tpDst:end", tmp_name1, "tpDst:END", "END", "nwProto", tmp_name4, NULL);
                                            if (rc) {
                                            } else {
                                                sgrulepos++;
                                            }
                                        } else {
                                            // its the all rule
                                            rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_EGRESS]), NULL, "position", tmp_name3, "type", "accept", "nwProto", tmp_name4, NULL);
                                            if (rc) {
                                            } else {
                                                sgrulepos++;
                                            }
                                        }
                                        
                                    } else if (gnisecgroup->egress_rules[k].protocol == -1) {
                                        // DAN HERE - why is proto -1 not triggering?
                                        snprintf(tmp_name3, 32, "%d", sgrulepos);
                                        rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_EGRESS]), NULL, "position", tmp_name3, "type", "accept", NULL);
                                        if (rc) {
                                        } else {
                                            sgrulepos++;
                                        }
                                    }
                                }
                            } else {
                                LOGWARN("cannot locate extant security group in mido for group %s\n", gnisecgroup->name);
                            }

                        }
                        EUCA_FREE(gnisecgroups);
                        

                        snprintf(tmp_name3, 32, "%d", rulepos);
                        rc = mido_create_rule(&(vpcinstance->midos[INST_PRECHAIN]), NULL, "position", tmp_name3, "type", "drop", "invDlType", "true", "dlType", "2054", NULL);
                        if (rc) {
                        } else {
                            rulepos++;
                        }

                        
                        // for ingress
                        rulepos = 1;

                        snprintf(tmp_name3, 32, "%d", rulepos);
                        rc = mido_create_rule(&(vpcinstance->midos[INST_POSTCHAIN]), NULL, "position", tmp_name3, "type", "snat", "flowAction", "continue", "nwSrcAddress", pt_buf,
                                              "nwSrcLength", "32", "nwProto", "6", "tpSrc", "jsonjson", "tpSrc:start", "31337", "tpSrc:end", "31337", "tpSrc:END", "END",
                                              "natTargets", "jsonlist", "natTargets:addressTo", "169.254.169.254", "natTargets:addressFrom", "169.254.169.254",
                                              "natTargets:portFrom", "80", "natTargets:portTo", "80", "natTargets:END", "END", NULL);
                        if (rc) {
                        } else {
                            rulepos++;
                        }

                        snprintf(tmp_name3, 32, "%d", rulepos);
                        rc = mido_create_rule(&(vpcinstance->midos[INST_POSTCHAIN]), NULL, "position", tmp_name3, "type", "accept", "matchReturnFlow", "true", NULL);
                        if (rc) {
                        } else {
                            rulepos++;
                        }

                        rc = gni_instance_get_secgroups(gni, gniinstance, NULL, 0, NULL, 0, &gnisecgroups, &max_gnisecgroups);
                        for (j = 0; j < max_gnisecgroups; j++) {
                            gni_secgroup *gnisecgroup = &(gnisecgroups[j]);
                            
                            char *tmpstr = NULL;
                            
                            // find the SG
                            rc = find_mido_vpc_secgroup(mido, gnisecgroup->name, &vpcsecgroup);
                            if (vpcsecgroup) {
                                // found one

                                tmpstr = hex2dot(gniinstance->privateIp);
                                rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGPRIV]), tmpstr, NULL);
                                rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGALL]), tmpstr, NULL);
                                EUCA_FREE(tmpstr);
                                
                                tmpstr = hex2dot(gniinstance->publicIp);
                                if (tmpstr && strcmp(tmpstr, "0.0.0.0")) {
                                    rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGPUB]), tmpstr, NULL);
                                    rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGALL]), tmpstr, NULL);
                                }
                                EUCA_FREE(tmpstr);
                                
                                // TODO make this better (not entire clear/reset each time
                                {
                                    midoname *rules = NULL;
                                    int max_rules = 0, r = 0;

                                    rc = mido_get_rules(&(vpcsecgroup->midos[VPCSG_INGRESS]), &rules, &max_rules);
                                    if (max_rules != gnisecgroup->max_ingress_rules) {
                                        for (r = 0; r < max_rules; r++) {
                                            mido_delete_rule(&(rules[r]));
                                        }
                                    }
                                    mido_free_midoname_list(rules, max_rules);
                                    EUCA_FREE(rules);
                                }
                                
                                snprintf(tmp_name3, 32, "%d", rulepos);
                                rc = mido_create_rule(&(vpcinstance->midos[INST_POSTCHAIN]), NULL, "position", tmp_name3, "type", "jump", "jumpChainId",
                                                      vpcsecgroup->midos[VPCSG_INGRESS].uuid, NULL);
                                if (rc) {
                                } else {
                                    rulepos++;
                                }
                                
                                snprintf(tmp_name3, 32, "%d", rulepos);
                                rc = mido_create_rule(&(vpcinstance->midos[INST_POSTCHAIN]), NULL, "type", "drop", "invDlType", "true", "position", tmp_name3, "dlType", "2054", NULL);
                                if (rc) {
                                } else {
                                    rulepos++;
                                }
                                
                                rulepos = 1;
                                for (k = 0; k < gnisecgroup->max_ingress_rules; k++) {
                                    // TODO other protos?
                                    // TODO add ingress from other SGs (set up IAGs and such)
                                    
                                    snprintf(tmp_name4, 32, "%d", gnisecgroup->ingress_rules[k].protocol);
                                    if (strlen(gnisecgroup->ingress_rules[k].groupId)) {
                                        // other group
                                        midoname *midos = NULL;
                                        int max_midos = 0, r;
                                        char name[32], *mname = NULL;
                                        int found = 0;
                                        rc = mido_get_ipaddrgroups("euca_tenant_1", &midos, &max_midos);
                                        for (r = 0; r < max_midos && !found; r++) {
                                            snprintf(name, 32, "sg_all_%11s", vpcsecgroup->name);
                                            rc = mido_getel_midoname(&(midos[r]), "name", &mname);
                                            if (mname && !strcmp(name, mname)) {
                                                LOGTRACE("FOUND: %s/%s\n", mname, midos[r].uuid);
                                                snprintf(tmp_name3, 32, "%d", rulepos);
                                                rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_INGRESS]), NULL, "position", tmp_name3, "type", "accept", "ipAddrGroupSrc", midos[r].uuid, NULL);
                                                if (rc) {
                                                } else {
                                                    rulepos++;
                                                }
                                                found++;
                                            }
                                            EUCA_FREE(mname);
                                        }
                                        mido_free_midoname_list(midos, max_midos);
                                        EUCA_FREE(midos);
                                    } else if (gnisecgroup->ingress_rules[k].protocol == 6 || gnisecgroup->ingress_rules[k].protocol == 17 || gnisecgroup->ingress_rules[k].protocol == 132) {
                                        // TCP/UDP/SCTP
                                        
                                        snprintf(tmp_name1, 32, "%d", gnisecgroup->ingress_rules[k].fromPort);
                                        snprintf(tmp_name2, 32, "%d", gnisecgroup->ingress_rules[k].toPort);
                                        
                                        snprintf(tmp_name3, 32, "%d", rulepos);
                                        rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_INGRESS]), NULL, "position", tmp_name3, "type", "accept", "tpDst", "jsonjson", "tpDst:start",
                                                              tmp_name1, "tpDst:end", tmp_name2, "tpDst:END", "END", "nwProto", tmp_name4, NULL);
                                        if (rc) {
                                        } else {
                                            rulepos++;
                                        }
                                        
                                    } else if (gnisecgroup->ingress_rules[k].protocol == 1) {
                                        // ICMP
                                        
                                        snprintf(tmp_name3, 32, "%d", rulepos);
                                        
                                        if (gnisecgroup->ingress_rules[k].icmpCode >= 0) {
                                            snprintf(tmp_name1, 32, "%d", gnisecgroup->ingress_rules[k].icmpCode);
                                            rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_INGRESS]), NULL, "position", tmp_name3, "type", "accept", "tpDst", "jsonjson",
                                                                  "tpDst:start", tmp_name1, "tpDst:end", tmp_name1, "tpDst:END", "END", "nwProto", tmp_name4, NULL);
                                            if (rc) {
                                            } else {
                                                rulepos++;
                                            }
                                        } else {
                                            // its the all rule
                                            rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_INGRESS]), NULL, "position", tmp_name3, "type", "accept", "nwProto", tmp_name4, NULL);
                                            if (rc) {
                                            } else {
                                                rulepos++;
                                            }
                                        }
                                        
                                    }
                                }
                            } else {
                                LOGWARN("cannot locate extant security group in mido for group %s\n", gnisecgroup->name);
                            }

                        }
                        EUCA_FREE(gnisecgroups);

                    }

                    // done
                }
            }
        }
    }
    fclose(PFH);

    // temporary print
    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        print_mido_vpc(vpc);
        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            print_mido_vpc_subnet(vpcsubnet);
            for (k = 0; k < vpcsubnet->max_instances; k++) {
                vpcinstance = &(vpcsubnet->instances[k]);
                print_mido_vpc_instance(vpcinstance);
            }
        }
    }
    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        vpcsecgroup = &(mido->vpcsecgroups[i]);
        print_mido_vpc_secgroup(vpcsecgroup);
    }
    
    /*
    // check and clear VPCs/subnets/instances/sgs
    LOGDEBUG("TOTAL SECGROUPS: %d\n", mido->max_vpcsecgroups);
    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        vpcsecgroup = &(mido->vpcsecgroups[i]);
        LOGDEBUG("CHECK: %s/%d\n", vpcsecgroup->name, vpcsecgroup->gnipresent);
        if (!vpcsecgroup->gnipresent) {
            LOGINFO("tearing down VPC sec. group %s\n", vpcsecgroup->name);
            rc = delete_mido_vpc_secgroup(vpcsecgroup);
        }
    }
    
    LOGDEBUG("TOTAL VPCS: %d\n", mido->max_vpcs);
    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        LOGDEBUG("CHECK: %s/%d\n", vpc->name, vpc->gnipresent);

        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            LOGDEBUG("\tCHECK: %s/%d\n", vpcsubnet->name, vpcsubnet->gnipresent);

            for (k = 0; k < vpcsubnet->max_instances; k++) {
                vpcinstance = &(vpcsubnet->instances[k]);
                LOGDEBUG("\t\tCHECK: %s/%d\n", vpcinstance->name, vpcinstance->gnipresent);

                if (!vpc->gnipresent || !vpcsubnet->gnipresent || !vpcinstance->gnipresent) {
                    rc = delete_mido_vpc_instance(vpcinstance);
                }
            }
            if (!vpc->gnipresent || !vpcsubnet->gnipresent) {
                LOGINFO("tearing down VPC '%s' subnet '%s'\n", vpc->name, vpcsubnet->name);
                rc = delete_mido_vpc_subnet(mido, vpcsubnet);
            }
        }
        if (!vpc->gnipresent) {
            LOGINFO("tearing down VPC '%s'\n", vpc->name);

            rc = do_metaproxy_teardown(mido);
            if (rc) {
                // TODO
                LOGERROR("cannot teardown metadata proxies: see above log for details\n");
                // ret=1;
            }

            rc = delete_mido_vpc(mido, vpc);
        }

    }
    */

    rc = do_metaproxy_setup(mido);
    if (rc) {
        LOGERROR("cannot set up metadata proxies: see above log for details\n");
        //    ret = 1;
    }

    return (0);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] nextid
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int get_next_router_id(mido_config * mido, int *nextid)
{
    int i;
    for (i = 2; i < 4096; i++) {
        if (!mido->router_ids[i]) {
            mido->router_ids[i] = 1;
            *nextid = i;
            return (0);
        }
    }
    return (1);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] id
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int set_router_id(mido_config * mido, int id)
{
    if (id < 4096) {
        mido->router_ids[id] = 1;
        return (0);
    }
    return (1);
}

//!
//!
//!
//! @param[in] vpc
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
void print_mido_vpc(mido_vpc * vpc)
{
    LOGDEBUG("PRINT VPC: name=%s max_rtports=%d max_subnets=%d gnipresent=%d\n", vpc->name, vpc->max_rtports, vpc->max_subnets, vpc->gnipresent);
    mido_print_midoname(&(vpc->midos[VPCRT]));
    mido_print_midoname(&(vpc->midos[EUCABR_DOWNLINK]));
    mido_print_midoname(&(vpc->midos[VPCRT_UPLINK]));
    mido_print_midoname(&(vpc->midos[VPCRT_PRECHAIN]));
    mido_print_midoname(&(vpc->midos[VPCRT_POSTCHAIN]));

}

//!
//!
//!
//! @param[in] vpcsubnet
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
void print_mido_vpc_subnet(mido_vpc_subnet * vpcsubnet)
{
    LOGDEBUG("PRINT VPCSUBNET: name=%s vpcname=%s max_brports=%d max_dhcphosts=%d max_instances=%d gnipresent=%d\n", vpcsubnet->name, vpcsubnet->vpcname, vpcsubnet->max_brports,
             vpcsubnet->max_dhcphosts, vpcsubnet->max_instances, vpcsubnet->gnipresent);
    mido_print_midoname(&(vpcsubnet->midos[VPCBR]));
    mido_print_midoname(&(vpcsubnet->midos[VPCBR_RTPORT]));
    mido_print_midoname(&(vpcsubnet->midos[VPCRT_BRPORT]));
    mido_print_midoname(&(vpcsubnet->midos[VPCBR_DHCP]));
}

//!
//!
//!
//! @param[in] vpcinstance
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
void print_mido_vpc_instance(mido_vpc_instance * vpcinstance)
{
    LOGDEBUG("PRINT VPCINSTANCE: name=%s gnipresent=%d\n", vpcinstance->name, vpcinstance->gnipresent);
    mido_print_midoname(&(vpcinstance->midos[VPCBR_VMPORT]));
    mido_print_midoname(&(vpcinstance->midos[VPCBR_DHCPHOST]));
    mido_print_midoname(&(vpcinstance->midos[VMHOST]));
}

//!
//!
//!
//! @param[in] vpcsecgroup
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
void print_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup)
{
    int i;
    LOGDEBUG("PRINT VPCSECGROUP: name=%s gnipresent=%d\n", vpcsecgroup->name, vpcsecgroup->gnipresent);
    for (i = 0; i < VPCSG_END; i++) {
        mido_print_midoname(&(vpcsecgroup->midos[i]));
    }
}

//!
//!
//!
//! @param[in] vpcsubnet
//! @param[in] instancename
//! @param[in] outvpcinstance
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int find_mido_vpc_instance(mido_vpc_subnet * vpcsubnet, char *instancename, mido_vpc_instance ** outvpcinstance)
{
    int i;
    if (!vpcsubnet || !instancename || !outvpcinstance) {
        return (1);
    }

    *outvpcinstance = NULL;
    for (i = 0; i < vpcsubnet->max_instances; i++) {
        if (!strcmp(instancename, vpcsubnet->instances[i].name)) {
            *outvpcinstance = &(vpcsubnet->instances[i]);
            return (0);
        }
    }

    return (1);
}

int find_mido_vpc_instance_global(mido_config * mido, char *instancename, mido_vpc_instance ** outvpcinstance) {
    int i, j, rc;
    mido_vpc *vpc=NULL;
    mido_vpc_subnet *vpcsubnet=NULL;

    if (!mido || !instancename || !outvpcinstance) {
        return(1);
    }

    for (i=0; i<mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        for (j=0; j<vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            rc = find_mido_vpc_instance(vpcsubnet, instancename, outvpcinstance);
            if (!rc) {
                return(0);
            }
        }
    }
    
    return(1);
}

//!
//!
//!
//! @param[in] vpc
//! @param[in] subnetname
//! @param[in] outvpcsubnet
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int find_mido_vpc_subnet(mido_vpc * vpc, char *subnetname, mido_vpc_subnet ** outvpcsubnet)
{
    int i;

    if (!vpc || !subnetname || !outvpcsubnet) {
        return (1);
    }

    *outvpcsubnet = NULL;

    for (i = 0; i < vpc->max_subnets; i++) {
        if (!strcmp(subnetname, vpc->subnets[i].name)) {
            *outvpcsubnet = &(vpc->subnets[i]);
            return (0);
        }
    }
    return (1);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] vpcname
//! @param[in] outvpc
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int find_mido_vpc(mido_config * mido, char *vpcname, mido_vpc ** outvpc)
{
    int i;

    if (!mido || !outvpc || !vpcname) {
        return (1);
    }

    *outvpc = NULL;
    for (i = 0; i < mido->max_vpcs; i++) {
        if (!strcmp(vpcname, mido->vpcs[i].name)) {
            *outvpc = &(mido->vpcs[i]);
            return (0);
        }
    }

    return (1);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] vpcsecgroup
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int populate_mido_vpc_secgroup(mido_config * mido, mido_vpc_secgroup * vpcsecgroup)
{
    int ret = 0, found = 0, foundcount = 0, rc = 0, i = 0;
    midoname *midos = NULL;
    int max_midos = 0;
    char name[64], *tmpstr = NULL;

    if (!mido || !vpcsecgroup) {
        return (1);
    }

    foundcount = found = 0;
    rc = mido_get_chains("euca_tenant_1", &midos, &max_midos);
    for (i = 0; i < max_midos && !found; i++) {
        snprintf(name, 64, "sg_ingress_%11s", vpcsecgroup->name);
        tmpstr = NULL;
        rc = mido_getel_midoname(&(midos[i]), "name", &tmpstr);
        if (tmpstr && !strcmp(name, tmpstr)) {
            // found
            mido_copy_midoname(&(vpcsecgroup->midos[VPCSG_INGRESS]), &(midos[i]));
            foundcount++;
        }
        EUCA_FREE(tmpstr);

        snprintf(name, 64, "sg_egress_%11s", vpcsecgroup->name);
        tmpstr = NULL;
        rc = mido_getel_midoname(&(midos[i]), "name", &tmpstr);
        if (tmpstr && !strcmp(name, tmpstr)) {
            // found
            mido_copy_midoname(&(vpcsecgroup->midos[VPCSG_EGRESS]), &(midos[i]));
            foundcount++;
        }
        EUCA_FREE(tmpstr);

        if (foundcount >= 2) {
            found++;
        }
    }
    mido_free_midoname_list(midos, max_midos);
    EUCA_FREE(midos);

    foundcount = found = 0;
    rc = mido_get_ipaddrgroups("euca_tenant_1", &midos, &max_midos);
    for (i = 0; i < max_midos && !found; i++) {
        snprintf(name, 64, "sg_priv_%11s", vpcsecgroup->name);
        tmpstr = NULL;
        rc = mido_getel_midoname(&(midos[i]), "name", &tmpstr);
        if (tmpstr && !strcmp(name, tmpstr)) {
            // found
            mido_copy_midoname(&(vpcsecgroup->midos[VPCSG_IAGPRIV]), &(midos[i]));
            foundcount++;
        }
        EUCA_FREE(tmpstr);

        snprintf(name, 64, "sg_pub_%11s", vpcsecgroup->name);
        tmpstr = NULL;
        rc = mido_getel_midoname(&(midos[i]), "name", &tmpstr);
        if (tmpstr && !strcmp(name, tmpstr)) {
            // found
            mido_copy_midoname(&(vpcsecgroup->midos[VPCSG_IAGPUB]), &(midos[i]));
            foundcount++;
        }
        EUCA_FREE(tmpstr);

        snprintf(name, 64, "sg_all_%11s", vpcsecgroup->name);
        tmpstr = NULL;
        rc = mido_getel_midoname(&(midos[i]), "name", &tmpstr);
        if (tmpstr && !strcmp(name, tmpstr)) {
            // found
            mido_copy_midoname(&(vpcsecgroup->midos[VPCSG_IAGALL]), &(midos[i]));
            foundcount++;
        }
        EUCA_FREE(tmpstr);

        if (foundcount >= 3) {
            found++;
        }
    }
    mido_free_midoname_list(midos, max_midos);
    EUCA_FREE(midos);

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] vpcsecgroup
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int create_mido_vpc_secgroup(mido_config * mido, mido_vpc_secgroup * vpcsecgroup)
{
    int ret = 0, rc = 0;
    char name[32];

    if (!mido || !vpcsecgroup) {
        return (1);
    }

    snprintf(name, 32, "sg_ingress_%11s", vpcsecgroup->name);
    rc = mido_create_chain("euca_tenant_1", name, &(vpcsecgroup->midos[VPCSG_INGRESS]));
    if (rc) {
        // TODO
        ret = 1;
    }

    snprintf(name, 32, "sg_egress_%11s", vpcsecgroup->name);
    rc = mido_create_chain("euca_tenant_1", name, &(vpcsecgroup->midos[VPCSG_EGRESS]));
    if (rc) {
        // TODO
        ret = 1;
    }

    snprintf(name, 32, "sg_priv_%11s", vpcsecgroup->name);
    rc = mido_create_ipaddrgroup("euca_tenant_1", name, &(vpcsecgroup->midos[VPCSG_IAGPRIV]));
    if (rc) {
        // TODO
        ret = 1;
    }

    snprintf(name, 32, "sg_pub_%11s", vpcsecgroup->name);
    rc = mido_create_ipaddrgroup("euca_tenant_1", name, &(vpcsecgroup->midos[VPCSG_IAGPUB]));
    if (rc) {
        // TODO
        ret = 1;
    }

    snprintf(name, 32, "sg_all_%11s", vpcsecgroup->name);
    rc = mido_create_ipaddrgroup("euca_tenant_1", name, &(vpcsecgroup->midos[VPCSG_IAGALL]));
    if (rc) {
        // TODO
        ret = 1;
    }

    return (ret);
}

//!
//!
//!
//! @param[in] vpcsecgroup
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int delete_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup)
{
    int ret = 0, rc = 0;

    if (!vpcsecgroup) {
        return (1);
    }

    rc = mido_delete_chain(&(vpcsecgroup->midos[VPCSG_INGRESS]));
    rc = mido_delete_chain(&(vpcsecgroup->midos[VPCSG_EGRESS]));

    rc = mido_delete_ipaddrgroup(&(vpcsecgroup->midos[VPCSG_IAGPRIV]));
    rc = mido_delete_ipaddrgroup(&(vpcsecgroup->midos[VPCSG_IAGPUB]));
    rc = mido_delete_ipaddrgroup(&(vpcsecgroup->midos[VPCSG_IAGALL]));

    free_mido_vpc_secgroup(vpcsecgroup);

    bzero(vpcsecgroup, sizeof(mido_vpc_secgroup));

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] secgroupname
//! @param[in] outvpcsecgroup
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int find_mido_vpc_secgroup(mido_config * mido, char *secgroupname, mido_vpc_secgroup ** outvpcsecgroup)
{
    int i = 0;

    if (!mido || !secgroupname || !outvpcsecgroup) {
        return (1);
    }

    *outvpcsecgroup = NULL;
    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        if (mido->vpcsecgroups[i].name && !strcmp(mido->vpcsecgroups[i].name, secgroupname)) {
            *outvpcsecgroup = &(mido->vpcsecgroups[i]);
            return (0);
        }
    }

    return (1);
}

//!
//!
//!
//! @param[in] vpcsecgroup
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int free_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup)
{
    int ret = 0;

    if (!vpcsecgroup)
        return (0);

    mido_free_midoname_list(vpcsecgroup->midos, VPCSG_END);
    bzero(vpcsecgroup, sizeof(mido_vpc_secgroup));

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] midocore
//! @param[in] vpc
//! @param[in] vpcsubnet
//! @param[in] vpcinstance
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int populate_mido_vpc_instance(mido_config * mido, mido_core * midocore, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet, mido_vpc_instance * vpcinstance)
{
    int ret = 0, rc = 0, found = 0, i = 0, founda = 0, j = 0;
    char *tmpstr = NULL, fstr[64], tmp_name[32];
    midoname *ips = NULL, *rules = NULL, *routes = NULL;
    int max_ips = 0, max_rules = 0, max_routes = 0;
    char *ip = NULL, *targetIP = NULL, *rdst = NULL;
    char matchStr[64];

    if (vpcsubnet->midos[VPCBR].init) {
        for (i = 0; i < vpcsubnet->max_brports && !found; i++) {
            LOGDEBUG("VPC BRPORTS: %s/%s\n", SP(vpcsubnet->brports[i].name), vpcsubnet->brports[i].uuid);

            tmpstr = NULL;
            rc = mido_getel_midoname(&(vpcsubnet->brports[i]), "hostId", &tmpstr);
            if (!rc && tmpstr && strlen(tmpstr)) {
                founda = 0;
                for (j = 0; j < mido->max_hosts && !founda; j++) {
                    if (!strcmp(tmpstr, mido->hosts[j].uuid)) {
                        LOGDEBUG("matched host %s with instance %s\n", mido->hosts[j].name, vpcinstance->name);
                        mido_copy_midoname(&(vpcinstance->midos[VMHOST]), &(mido->hosts[j]));
                        founda = 1;
                    }
                }
            }
            EUCA_FREE(tmpstr);

            tmpstr = NULL;
            rc = mido_getel_midoname(&(vpcsubnet->brports[i]), "interfaceName", &tmpstr);
            if (!rc && tmpstr && strlen(tmpstr)) {
                LOGTRACE("PORT INTERFACE NAME: %s\n", SP(tmpstr));
                LOGTRACE("TRYING TO MATCH INSTANCE %s WITH PORT %s\n", vpcinstance->name, SP(tmpstr));
                if (strstr(tmpstr, vpcinstance->name)) {
                    LOGDEBUG("matched port %s with instance %s\n", vpcsubnet->brports[i].uuid, vpcinstance->name);
                    mido_copy_midoname(&(vpcinstance->midos[VPCBR_VMPORT]), &(vpcsubnet->brports[i]));
                }
            }
            EUCA_FREE(tmpstr);

            if (vpcinstance->midos[VPCBR_VMPORT].init && vpcinstance->midos[VMHOST].init) {
                found = 1;
            }
        }
    }

    if (vpcsubnet->midos[VPCBR_DHCP].init) {
        found = 0;
        for (i = 0; i < vpcsubnet->max_dhcphosts && !found; i++) {
            tmpstr = NULL;
            rc = mido_getel_midoname(&(vpcsubnet->dhcphosts[i]), "name", &tmpstr);
            if (!rc && tmpstr && strlen(tmpstr) && !strcmp(tmpstr, vpcinstance->name)) {
                mido_copy_midoname(&(vpcinstance->midos[VPCBR_DHCPHOST]), &(vpcsubnet->dhcphosts[i]));
                found = 1;
            }
            EUCA_FREE(tmpstr);
        }
    }

    found = 0;
    for (i = 0; i < mido->max_ipaddrgroups && !found; i++) {
        snprintf(fstr, 64, "elip_pre_%s", vpcinstance->name);
        if (!strcmp(fstr, mido->ipaddrgroups[i].name)) {
            mido_copy_midoname(&vpcinstance->midos[ELIP_PRE_IPADDRGROUP], &(mido->ipaddrgroups[i]));
        }

        snprintf(fstr, 64, "elip_post_%s", vpcinstance->name);
        if (!strcmp(fstr, mido->ipaddrgroups[i].name)) {
            mido_copy_midoname(&vpcinstance->midos[ELIP_POST_IPADDRGROUP], &(mido->ipaddrgroups[i]));
        }
        if (vpcinstance->midos[ELIP_PRE_IPADDRGROUP].init && vpcinstance->midos[ELIP_POST_IPADDRGROUP].init) {
            found = 1;
        }
    }

    // this is the public IP
    if (vpcinstance->midos[ELIP_PRE_IPADDRGROUP].init) {
        found = 0;

        rc = mido_get_ipaddrgroup_ips(&(vpcinstance->midos[ELIP_PRE_IPADDRGROUP]), &ips, &max_ips);
        for (i = 0; i < max_ips && !found; i++) {
            rc = mido_getel_midoname(&(ips[i]), "addr", &ip);
            if (ip) {
                // found it
                mido_copy_midoname(&(vpcinstance->midos[ELIP_PRE_IPADDRGROUP_IP]), &(ips[i]));
                found = 1;

                // now find the associated chain rule
                rc = mido_get_rules(&(vpc->midos[VPCRT_POSTCHAIN]), &rules, &max_rules);
                for (j = 0; j < max_rules; j++) {
                    rc = mido_getel_midoname(&(rules[j]), "natTargets", &targetIP);
                    snprintf(matchStr, 64, "\"addressTo\": \"%s\"", ip);
                    if (targetIP && ip && strstr(targetIP, matchStr)) {
                        mido_copy_midoname(&(vpcinstance->midos[ELIP_PRE]), &(rules[j]));
                        EUCA_FREE(targetIP);
                    }
                }
                mido_free_midoname_list(rules, max_rules);
                EUCA_FREE(rules);
                EUCA_FREE(ip);
            }
        }
        mido_free_midoname_list(ips, max_ips);
        EUCA_FREE(ips);
    }
    // this is the private IP
    if (vpcinstance->midos[ELIP_POST_IPADDRGROUP].init) {
        found = 0;

        rc = mido_get_ipaddrgroup_ips(&(vpcinstance->midos[ELIP_POST_IPADDRGROUP]), &ips, &max_ips);
        for (i = 0; i < max_ips && !found; i++) {
            rc = mido_getel_midoname(&(ips[i]), "addr", &ip);
            if (ip) {
                // found it
                mido_copy_midoname(&(vpcinstance->midos[ELIP_POST_IPADDRGROUP_IP]), &(ips[i]));
                found = 1;

                // now find the associated chain rule
                rc = mido_get_rules(&(vpc->midos[VPCRT_PREELIPCHAIN]), &rules, &max_rules);
                for (j = 0; j < max_rules; j++) {
                    rc = mido_getel_midoname(&(rules[j]), "natTargets", &targetIP);
                    snprintf(matchStr, 64, "\"addressTo\": \"%s\"", ip);
                    if (targetIP && ip && strstr(targetIP, matchStr)) {
                        mido_copy_midoname(&(vpcinstance->midos[ELIP_POST]), &(rules[j]));
                        EUCA_FREE(targetIP);
                    }
                }
                mido_free_midoname_list(rules, max_rules);
                EUCA_FREE(rules);
                EUCA_FREE(ip);
            }
        }
        mido_free_midoname_list(ips, max_ips);
        EUCA_FREE(ips);
    }

    if (vpcinstance->midos[ELIP_PRE_IPADDRGROUP].init && midocore->midos[EUCART].init) {
        found = 0;

        rc = mido_get_ipaddrgroup_ips(&(vpcinstance->midos[ELIP_PRE_IPADDRGROUP]), &ips, &max_ips);
        for (i = 0; i < max_ips && !found; i++) {
            rc = mido_getel_midoname(&(ips[i]), "addr", &ip);
            if (ip) {
                rc = mido_get_routes(&(midocore->midos[EUCART]), &routes, &max_routes);
                for (j = 0; j < max_routes && !found; j++) {
                    rc = mido_getel_midoname(&(routes[j]), "dstNetworkAddr", &rdst);
                    if (!strcmp(ip, rdst)) {
                        // found it!
                        mido_copy_midoname(&(vpcinstance->midos[ELIP_ROUTE]), &(routes[j]));
                        found = 1;
                    }
                    EUCA_FREE(rdst);
                }
                mido_free_midoname_list(routes, max_routes);
                EUCA_FREE(routes);
                EUCA_FREE(ip);
            }
        }
        mido_free_midoname_list(ips, max_ips);
        EUCA_FREE(ips);
    }

    for (i = 0; i < mido->max_chains; i++) {
        snprintf(tmp_name, 32, "ic_%s_prechain", vpcinstance->name);
        if (!strcmp(mido->chains[i].name, tmp_name)) {
            mido_copy_midoname(&(vpcinstance->midos[INST_PRECHAIN]), &(mido->chains[i]));
        }
        snprintf(tmp_name, 32, "ic_%s_postchain", vpcinstance->name);
        if (!strcmp(mido->chains[i].name, tmp_name)) {
            mido_copy_midoname(&(vpcinstance->midos[INST_POSTCHAIN]), &(mido->chains[i]));
        }
    }

    LOGTRACE("vpc instance: AFTER POPULATE\n");
    for (i = 0; i < VPCINSTANCEEND; i++) {
        LOGTRACE("\tmidos[%d]: |%d|\n", i, vpcinstance->midos[i].init);
    }
    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] vpcinstance
//! @param[in] nodehostname
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int create_mido_vpc_instance(mido_config * mido, mido_vpc_instance * vpcinstance, char *nodehostname)
{
    int ret = 0, found = 0, i = 0, rc = 0;
    char aigname[64], tmp_name[32];

    // find the interface mapping
    found = 0;
    for (i = 0; i < mido->max_hosts && !found; i++) {
        if (strstr(mido->hosts[i].name, nodehostname)) {
            mido_copy_midoname(&(vpcinstance->midos[VMHOST]), &(mido->hosts[i]));
            found = 1;
        }
    }

    // set up elip ipaddrgroups
    snprintf(aigname, 64, "elip_pre_%s", vpcinstance->name);
    rc = mido_create_ipaddrgroup("euca_tenant_1", aigname, &(vpcinstance->midos[ELIP_PRE_IPADDRGROUP]));
    if (rc) {
        ret = 1;
    }

    snprintf(aigname, 64, "elip_post_%s", vpcinstance->name);
    rc = mido_create_ipaddrgroup("euca_tenant_1", aigname, &(vpcinstance->midos[ELIP_POST_IPADDRGROUP]));
    if (rc) {
        ret = 1;
    }

    snprintf(tmp_name, 32, "ic_%s_prechain", vpcinstance->name);
    rc = mido_create_chain("euca_tenant_1", tmp_name, &(vpcinstance->midos[INST_PRECHAIN]));
    if (rc) {
        ret = 1;
    }
    snprintf(tmp_name, 32, "ic_%s_postchain", vpcinstance->name);
    rc = mido_create_chain("euca_tenant_1", tmp_name, &(vpcinstance->midos[INST_POSTCHAIN]));
    if (rc) {
        ret = 1;
    }

    return (ret);
}

//!
//!
//!
//! @param[in] vpcinstance
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int delete_mido_vpc_instance(mido_vpc_instance * vpcinstance)
{
    int ret = 0, rc = 0;

    if (!vpcinstance || !strlen(vpcinstance->name)) {
        LOGFATAL("BUG: invalid input params\n");
        return (1);
    }

    rc = disconnect_mido_vpc_instance_elip(vpcinstance);
    if (rc) {
        ret = 1;
    }
    // unlink port, delete port, delete dhcp entry
    rc = mido_unlink_host_port(&(vpcinstance->midos[VMHOST]), &(vpcinstance->midos[VPCBR_VMPORT]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_port(&(vpcinstance->midos[VPCBR_VMPORT]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_dhcphost(&(vpcinstance->midos[VPCBR_DHCPHOST]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_ipaddrgroup(&(vpcinstance->midos[ELIP_PRE_IPADDRGROUP]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_ipaddrgroup(&(vpcinstance->midos[ELIP_POST_IPADDRGROUP]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_chain(&(vpcinstance->midos[INST_PRECHAIN]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_chain(&(vpcinstance->midos[INST_POSTCHAIN]));
    if (rc) {
        ret = 1;
    }

    free_mido_vpc_instance(vpcinstance);

    bzero(vpcinstance, sizeof(mido_vpc_instance));

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] eucahome
//! @param[in] setupcore
//! @param[in] ext_eucanetdhostname
//! @param[in] ext_rthostname
//! @param[in] ext_rtaddr
//! @param[in] ext_rtiface
//! @param[in] ext_pubnw
//! @param[in] ext_pubgwip
//! @param[in] int_rtnetwork
//! @param[in] int_rtslashnet
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int initialize_mido(mido_config * mido, char *eucahome, char *setupcore, char *ext_eucanetdhostname, char *ext_rthostname, char *ext_rtaddr, char *ext_rtiface, char *ext_pubnw,
                    char *ext_pubgwip, char *int_rtnetwork, char *int_rtslashnet)
{
    int ret = 0;

    if (!mido ||
        !eucahome ||
        !ext_eucanetdhostname ||
        !ext_rthostname ||
        !ext_rtaddr ||
        !ext_rtiface ||
        !ext_pubnw ||
        !ext_pubgwip ||
        !int_rtnetwork ||
        !int_rtslashnet ||
        !strlen(ext_eucanetdhostname) ||
        !strlen(ext_rthostname) || !strlen(ext_rtaddr) || !strlen(ext_rtiface) || !strlen(ext_pubnw) || !strlen(ext_pubgwip) || !strlen(int_rtnetwork) || !strlen(int_rtslashnet))
        return (1);

    bzero(mido, sizeof(mido_config));

    mido->eucahome = strdup(eucahome);

    mido->setupcore = 1;
    if (setupcore && (strlen(setupcore) > 0) && (!strcmp(setupcore, "N") || !strcmp(setupcore, "n"))) {
        mido->setupcore = 0;
    }

    mido->ext_eucanetdhostname = strdup(ext_eucanetdhostname);
    mido->ext_rthostname = strdup(ext_rthostname);
    mido->ext_rtaddr = strdup(ext_rtaddr);
    mido->ext_rtiface = strdup(ext_rtiface);
    mido->ext_pubnw = strdup(ext_pubnw);
    mido->ext_pubgwip = strdup(ext_pubgwip);
    mido->int_rtnw = dot2hex(int_rtnetwork);    // strdup(int_rtnetwork);
    mido->int_rtsn = atoi(int_rtslashnet);
    mido->int_rtaddr = mido->int_rtnw + 1;
    mido->midocore = calloc(1, sizeof(mido_core));
    LOGDEBUG("mido initialized: mido->ext_eucanetdhostname=%s ext_rthostname=%s ext_rtaddr=%s  mido->ext_rtiface=%s mido->ext_pubnw=%s mido->ext_pubgwip=%s int_rtcidr=%s/%s \n",
             SP(mido->ext_eucanetdhostname), SP(mido->ext_rthostname), SP(mido->ext_rtaddr), SP(mido->ext_rtiface), SP(mido->ext_pubnw), SP(mido->ext_pubgwip), SP(int_rtnetwork),
             SP(int_rtslashnet));

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int discover_mido_resources(mido_config * mido)
{
    int rc = 0, ret = 0, i = 0, max_ports = 0, j = 0, count = 0;
    midoname *ports = NULL;

    rc = mido_get_routers("euca_tenant_1", &(mido->routers), &(mido->max_routers));
    if (rc) {
        LOGWARN("no routers in midonet\n");
    }

    count = 0;
    for (i = 0; i < mido->max_routers; i++) {
        //      mido_get_ports(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->brports), &(vpcsubnet->max_brports));
        rc = mido_get_ports(&(mido->routers[i]), &ports, &max_ports);
        if (!rc && max_ports) {
            mido->rtports = realloc(mido->rtports, sizeof(midoname) * (mido->max_rtports + max_ports));
            for (j = 0; j < max_ports; j++) {
                bzero(&(mido->rtports[count]), sizeof(midoname));
                mido_copy_midoname(&(mido->rtports[count]), &(ports[j]));
                count++;
            }
            mido->max_rtports += max_ports;
        }

        if (ports && max_ports > 0) {
            mido_free_midoname_list(ports, max_ports);
            EUCA_FREE(ports);
        }

    }

    ports = NULL;
    max_ports = 0;

    rc = mido_get_bridges("euca_tenant_1", &(mido->bridges), &(mido->max_bridges));
    if (rc) {
        LOGWARN("no bridges in midonet\n");
    }
    count = 0;
    for (i = 0; i < mido->max_bridges; i++) {
        //      mido_get_ports(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->brports), &(vpcsubnet->max_brports));
        rc = mido_get_ports(&(mido->bridges[i]), &ports, &max_ports);
        if (!rc && max_ports) {
            mido->brports = realloc(mido->brports, sizeof(midoname) * (mido->max_brports + max_ports));
            for (j = 0; j < max_ports; j++) {
                bzero(&(mido->brports[count]), sizeof(midoname));
                mido_copy_midoname(&(mido->brports[count]), &(ports[j]));
                count++;
            }
            mido->max_brports += max_ports;
        }
        if (ports && max_ports > 0) {
            mido_free_midoname_list(ports, max_ports);
            EUCA_FREE(ports);
        }
    }

    rc = mido_get_chains("euca_tenant_1", &(mido->chains), &(mido->max_chains));
    if (rc) {
        LOGWARN("no chains in midonet\n");
    }

    rc = mido_get_hosts(&(mido->hosts), &(mido->max_hosts));
    if (rc) {
        LOGERROR("cannot get hosts from midonet: check midonet health\n");
        return (1);
    }
    /*
       for (i=0; i<mido->max_hosts; i++) {
       printf("%s/%s\n", mido->hosts[i].name, mido->hosts[i].uuid);
       }
     */

    rc = mido_get_ipaddrgroups("euca_tenant_1", &(mido->ipaddrgroups), &(mido->max_ipaddrgroups));
    if (rc) {
        LOGWARN("no ip address groups in midonet\n");
    }

    /*
       for (i=0; i<mido->max_ipaddrgroups; i++) {
       LOGINFO("hello - %s/%s\n", mido->ipaddrgroups[i].name, mido->ipaddrgroups[i].uuid);
       }
     */

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] vpc
//! @param[in] vpcsubnet
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int populate_mido_vpc_subnet(mido_config * mido, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet)
{
    int rc = 0, ret = 0, i = 0, j = 0, found = 0;
    char name[64];
    midoname *dhcps = NULL;
    int max_dhcps = 0;
    char *tmpstr = NULL;

    found = 0;
    for (i = 0; i < mido->max_hosts && !found; i++) {
        if (strstr(mido->hosts[i].name, mido->ext_eucanetdhostname)) {
            mido_copy_midoname(&(vpcsubnet->midos[VPCBR_METAHOST]), &(mido->hosts[i]));
            found = 1;
        }
    }

    snprintf(name, 64, "vb_%s_%s", vpc->name, vpcsubnet->name);
    for (i = 0; i < mido->max_bridges; i++) {
        if (!strcmp(mido->bridges[i].name, name)) {
            mido_copy_midoname(&(vpcsubnet->midos[VPCBR]), &(mido->bridges[i]));
            mido_get_ports(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->brports), &(vpcsubnet->max_brports));
        }
    }

    if (vpcsubnet->midos[VPCBR].init) {
        mido_get_dhcps(&(vpcsubnet->midos[VPCBR]), &dhcps, &max_dhcps);
        if (max_dhcps) {
            LOGDEBUG("VPC DHCP SERVERS: %s/%s\n", dhcps[0].uuid, dhcps[0].jsonbuf);
            mido_copy_midoname(&(vpcsubnet->midos[VPCBR_DHCP]), &(dhcps[0]));
        }
    }
    mido_free_midoname_list(dhcps, max_dhcps);
    EUCA_FREE(dhcps);

    if (vpcsubnet->midos[VPCBR_DHCP].init) {
        rc = mido_get_dhcphosts(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->midos[VPCBR_DHCP]), &(vpcsubnet->dhcphosts), &(vpcsubnet->max_dhcphosts));
        if (rc) {
        }
    }

    if (vpcsubnet->midos[VPCBR].init) {
        for (i = 0; i < vpcsubnet->max_brports; i++) {
            LOGDEBUG("VPC BRPORTS: %s/%s\n", SP(vpcsubnet->brports[i].name), vpcsubnet->brports[i].uuid);

            LOGDEBUG("RERRO1: %s\n", SP(tmpstr));
            for (j = 0; j < vpc->max_rtports; j++) {
                tmpstr = NULL;
                rc = mido_getel_midoname(&(vpc->rtports[j]), "peerId", &tmpstr);
                if (!rc && tmpstr && vpcsubnet->brports[i].uuid) {
                    if (!strcmp(tmpstr, vpcsubnet->brports[i].uuid)) {
                        mido_copy_midoname(&(vpcsubnet->midos[VPCBR_RTPORT]), &(vpcsubnet->brports[i]));
                        mido_copy_midoname(&(vpcsubnet->midos[VPCRT_BRPORT]), &(vpc->rtports[j]));
                    }
                }
                EUCA_FREE(tmpstr);
            }

            rc = mido_getel_midoname(&(vpcsubnet->brports[i]), "interfaceName", &tmpstr);
            LOGDEBUG("RERRO: %s\n", SP(tmpstr));
            if (!rc && tmpstr && strlen(tmpstr) && strstr(tmpstr, "vn0_")) {
                // found the meta iface
                mido_copy_midoname(&(vpcsubnet->midos[VPCBR_METAPORT]), &(vpcsubnet->brports[i]));
            }
            EUCA_FREE(tmpstr);
        }
    }

    for (i = 0; i < VPCSUBNETEND; i++) {
        if (!vpcsubnet->midos[i].init) {
            LOGWARN("VPC subnet population failed to populate resource at idx %d\n", i);
        }
    }

    {
        // temporary
        LOGDEBUG("AFTER POPULATE: VPCBR: %d VPCBR_RTPORT: %d VPCRT_BRPORT: %d VPCBR_DHCP: %d VPCBR_METAPORT: %d VPCBR_METAHOST: %d\n", vpcsubnet->midos[VPCBR].init,
                 vpcsubnet->midos[VPCBR_RTPORT].init, vpcsubnet->midos[VPCRT_BRPORT].init, vpcsubnet->midos[VPCBR_DHCP].init, vpcsubnet->midos[VPCBR_METAPORT].init,
                 vpcsubnet->midos[VPCBR_METAHOST].init);
    }

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] midocore
//! @param[in] vpc
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int populate_mido_vpc(mido_config * mido, mido_core * midocore, mido_vpc * vpc)
{
    int rc = 0, ret = 0, i = 0, j = 0;
    char *url = NULL, vpcname[32];

    snprintf(vpcname, 32, "vr_%s", vpc->name);
    for (i = 0; i < mido->max_routers; i++) {
        if (strstr(mido->routers[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT]), &(mido->routers[i]));
            mido_get_ports(&(vpc->midos[VPCRT]), &(vpc->rtports), &(vpc->max_rtports));
        }
    }

    snprintf(vpcname, 32, "vc_%s_prechain", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PRECHAIN]), &(mido->chains[i]));
        }
    }

    /*
    snprintf(vpcname, 32, "vc_%s_preether", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PREETHERCHAIN]), &(mido->chains[i]));
        }
    }
    

    snprintf(vpcname, 32, "vc_%s_premeta", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PREMETACHAIN]), &(mido->chains[i]));
        }
    }

    snprintf(vpcname, 32, "vc_%s_prevpcinternal", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PREVPCINTERNALCHAIN]), &(mido->chains[i]));
        }
    }

    snprintf(vpcname, 32, "vc_%s_prefw", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PREFWCHAIN]), &(mido->chains[i]));
        }
    }
    */


    snprintf(vpcname, 32, "vc_%s_preelip", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PREELIPCHAIN]), &(mido->chains[i]));
        }
    }

    snprintf(vpcname, 32, "vc_%s_postchain", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_POSTCHAIN]), &(mido->chains[i]));
        }
    }

    if (midocore->midos[EUCABR].init) {
        for (i = 0; i < midocore->max_brports; i++) {
            LOGDEBUG("MIDO BRPORTS: %s/%s\n", SP(midocore->brports[i].name), midocore->brports[i].uuid);
            for (j = 0; j < vpc->max_rtports; j++) {
                rc = mido_getel_midoname(&(vpc->rtports[j]), "peerId", &url);
                if (!rc && url && midocore->brports[i].uuid) {
                    if (!strcmp(url, midocore->brports[i].uuid)) {
                        mido_copy_midoname(&(vpc->midos[EUCABR_DOWNLINK]), &(midocore->brports[i]));
                        mido_copy_midoname(&(vpc->midos[VPCRT_UPLINK]), &(vpc->rtports[j]));
                    }
                }
                EUCA_FREE(url);
            }
        }
    }

    {
        // temporary
        LOGDEBUG("AFTER POPULATE: VPCRT: %d VPCRT_PRECHAIN: %d VPCRT_POSTCHAIN: %d EUCABR_DOWNLINK: %d VPCRT_UPLINK: %d\n", vpc->midos[VPCRT].init, vpc->midos[VPCRT_PRECHAIN].init,
                 vpc->midos[VPCRT_POSTCHAIN].init, vpc->midos[EUCABR_DOWNLINK].init, vpc->midos[VPCRT_UPLINK].init);
    }

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] midocore
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int populate_mido_core(mido_config * mido, mido_core * midocore)
{
    int rc = 0, ret = 0, i = 0, j = 0;
    char *url = NULL;

    for (i = 0; i < mido->max_routers; i++) {
        if (!strcmp(mido->routers[i].name, "eucart")) {
            mido_copy_midoname(&(midocore->midos[EUCART]), &(mido->routers[i]));
            mido_get_ports(&(midocore->midos[EUCART]), &(midocore->rtports), &(midocore->max_rtports));
        }
    }

    for (i = 0; i < mido->max_bridges; i++) {
        if (!strcmp(mido->bridges[i].name, "eucabr")) {
            mido_copy_midoname(&(midocore->midos[EUCABR]), &(mido->bridges[i]));
            mido_get_ports(&(midocore->midos[EUCABR]), &(midocore->brports), &(midocore->max_brports));
        }
    }

    for (i = 0; i < midocore->max_brports; i++) {
        for (j = 0; j < midocore->max_rtports; j++) {
            rc = mido_getel_midoname(&(midocore->rtports[j]), "peerId", &url);
            if (!rc && url && midocore->brports[i].uuid) {
                if (!strcmp(url, midocore->brports[i].uuid)) {
                    mido_copy_midoname(&(midocore->midos[EUCABR_RTPORT]), &(midocore->brports[i]));
                    mido_copy_midoname(&(midocore->midos[EUCART_BRPORT]), &(midocore->rtports[j]));
                }
            }
            EUCA_FREE(url);
            url = NULL;
            rc = mido_getel_midoname(&(midocore->rtports[j]), "portAddress", &url);
            if (!rc && url && mido->ext_rtaddr) {
                if (!strcmp(mido->ext_rtaddr, url)) {
                    mido_copy_midoname(&(midocore->midos[EUCART_GWPORT]), &(midocore->rtports[j]));
                }
            }
            EUCA_FREE(url);
        }
    }

    for (i = 0; i < mido->max_hosts; i++) {
        if (strstr(mido->hosts[i].name, mido->ext_rthostname)) {
            mido_copy_midoname(&(midocore->midos[GWHOST]), &(mido->hosts[i]));
        }
    }

    {
        // temporary
        LOGDEBUG("AFTER POPULATE: EUCART: %d EUCABR: %d EUCART_BRPORT: %d EUCABR_RTPORT: %d EUCART_GWPORT: %d GWHOST: %d\n", midocore->midos[EUCART].init,
                 midocore->midos[EUCABR].init, midocore->midos[EUCART_BRPORT].init, midocore->midos[EUCABR_RTPORT].init, midocore->midos[EUCART_GWPORT].init,
                 midocore->midos[GWHOST].init);
    }

    return (ret);
}

//!
//!
//!
//! @param[in] vpcinstance
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int disconnect_mido_vpc_instance_elip(mido_vpc_instance * vpcinstance)
{
    int ret = 0, rc = 0;

    mido_print_midoname(&(vpcinstance->midos[ELIP_ROUTE]));
    rc = mido_delete_route(&(vpcinstance->midos[ELIP_ROUTE]));
    if (rc) {
        ret = 1;
    }

    mido_print_midoname(&(vpcinstance->midos[ELIP_PRE]));
    rc = mido_delete_rule(&(vpcinstance->midos[ELIP_PRE]));
    if (rc) {
        //TODO
        ret = 1;
    }

    mido_print_midoname(&(vpcinstance->midos[ELIP_POST]));
    rc = mido_delete_rule(&(vpcinstance->midos[ELIP_POST]));
    if (rc) {
        //TODO
        ret = 1;
    }

    mido_print_midoname(&(vpcinstance->midos[ELIP_PRE_IPADDRGROUP_IP]));
    rc = mido_delete_ipaddrgroup_ip(&(vpcinstance->midos[ELIP_PRE_IPADDRGROUP]), &(vpcinstance->midos[ELIP_PRE_IPADDRGROUP_IP]));
    if (rc) {
        LOGERROR("could not delete instance IP addr from ipaddrgroup\n");
        ret = 1;
    }

    mido_print_midoname(&(vpcinstance->midos[ELIP_POST_IPADDRGROUP_IP]));
    rc = mido_delete_ipaddrgroup_ip(&(vpcinstance->midos[ELIP_POST_IPADDRGROUP]), &(vpcinstance->midos[ELIP_POST_IPADDRGROUP_IP]));
    if (rc) {
        LOGERROR("could not delete instance IP addr from ipaddrgroup\n");
        ret = 1;
    }

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] midocore
//! @param[in] vpc
//! @param[in] vpcsubnet
//! @param[in] vpcinstance
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int connect_mido_vpc_instance_elip(mido_config * mido, mido_core * midocore, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet, mido_vpc_instance * vpcinstance)
{
    int rc = 0, ret = 0;
    char *ipAddr_pub = NULL, *ipAddr_priv = NULL, *tmpstr = NULL, pt_buf[24];
    char ip[32];

    if (!vpcinstance->gniInst->publicIp || !vpcinstance->gniInst->privateIp) {
        LOGERROR("input ip is 0.0.0.0\n");
        return (1);
    }

    cidr_split(vpcsubnet->gniSubnet->cidr, NULL, NULL, NULL, pt_buf);

    tmpstr = hex2dot(mido->int_rtnw + vpc->rtid);
    snprintf(ip, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    ipAddr_pub = hex2dot(vpcinstance->gniInst->publicIp);
    ipAddr_priv = hex2dot(vpcinstance->gniInst->privateIp);

    rc = mido_create_ipaddrgroup_ip(&(vpcinstance->midos[ELIP_PRE_IPADDRGROUP]), ipAddr_pub, &(vpcinstance->midos[ELIP_PRE_IPADDRGROUP_IP]));
    if (rc) {
    }

    rc = mido_create_ipaddrgroup_ip(&(vpcinstance->midos[ELIP_POST_IPADDRGROUP]), ipAddr_priv, &(vpcinstance->midos[ELIP_POST_IPADDRGROUP_IP]));
    if (rc) {
    }

    rc = mido_create_rule(&(vpc->midos[VPCRT_PREELIPCHAIN]), &(vpcinstance->midos[ELIP_PRE]), "type", "dnat", "flowAction", "continue", "ipAddrGroupDst",
                          vpcinstance->midos[ELIP_PRE_IPADDRGROUP].uuid, "natTargets", "jsonlist", "natTargets:addressTo", ipAddr_priv, "natTargets:addressFrom", ipAddr_priv,
                          "natTargets:portFrom", "0", "natTargets:portTo", "0", "natTargets:END", "END", NULL);
    if (rc) {
        LOGERROR("cannot create midonet rule: check midonet health\n");
        ret = 1;
    }

    rc = mido_create_rule(&(vpc->midos[VPCRT_POSTCHAIN]), &(vpcinstance->midos[ELIP_POST]), "type", "snat", "nwDstAddress", pt_buf, "invNwDst", "true", "nwDstLength", "32",
                          "flowAction", "continue", "ipAddrGroupSrc", vpcinstance->midos[ELIP_POST_IPADDRGROUP].uuid, "natTargets", "jsonlist", "natTargets:addressTo", ipAddr_pub,
                          "natTargets:addressFrom", ipAddr_pub, "natTargets:portFrom", "0", "natTargets:portTo", "0", "natTargets:END", "END", NULL);
    if (rc) {
        LOGERROR("cannot create midonet rule: check midonet health\n");
        ret = 1;
    }

    if (!ret && strcmp(ipAddr_pub, "0.0.0.0")) {
        // create the EL ip route in main router
        rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_BRPORT]), "0.0.0.0", "0", ipAddr_pub, "32", ip, "100", &(vpcinstance->midos[ELIP_ROUTE]));
        if (rc) {
            LOGERROR("setup float IP router on midonet router: check midonet health\n");
            ret = 1;
        }
    }

    EUCA_FREE(ipAddr_pub);
    EUCA_FREE(ipAddr_priv);

    return (ret);
}

//!
//!
//!
//! @param[in] vpcsubnet
//! @param[in] vpcinstance
//! @param[in] vmhost
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int connect_mido_vpc_instance(mido_vpc_subnet * vpcsubnet, mido_vpc_instance * vpcinstance, midoname * vmhost)
{
    int ret = 0, rc = 0;
    char *macAddr = NULL, *ipAddr = NULL;
    char ifacename[IF_NAME_LEN];

    snprintf(ifacename, IF_NAME_LEN, "vn_%s", vpcinstance->gniInst->name);

    // create the Exterior ports for VMs
    rc = mido_create_port(&(vpcsubnet->midos[VPCBR]), "ExteriorBridge", NULL, NULL, NULL, &(vpcinstance->midos[VPCBR_VMPORT]));
    if (rc) {
        LOGERROR("cannot create midonet bridge port: check midonet health\n");
        return (1);
    }
    // link vm host port to vm bridge port
    rc = mido_link_host_port(vmhost, ifacename, &(vpcsubnet->midos[VPCBR]), &(vpcinstance->midos[VPCBR_VMPORT]));
    if (rc) {
        LOGERROR("cannot create midonet bridge port to vm interface link: check midonet health\n");
        return (1);
    }
    // set up dhcp host entry
    hex2mac(vpcinstance->gniInst->macAddress, &macAddr);
    ipAddr = hex2dot(vpcinstance->gniInst->privateIp);
    for (int i = 0; i < strlen(macAddr); i++) {
        macAddr[i] = tolower(macAddr[i]);
    }

    LOGDEBUG("adding host %s/%s to dhcp server\n", SP(macAddr), SP(ipAddr));
    //  bzero(&(vpcinstance->midos[VPCBR_DHCPHOST]), sizeof(midoname));
    rc = mido_create_dhcphost(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->midos[VPCBR_DHCP]), vpcinstance->gniInst->name, macAddr, ipAddr, &(vpcinstance->midos[VPCBR_DHCPHOST]));
    EUCA_FREE(ipAddr);
    EUCA_FREE(macAddr);
    if (rc) {
        LOGERROR("cannot create midonet dhcp host entry: check midonet health\n");
        return (1);
    }
    // apply the chains to the instance port
    rc = mido_update_port(&(vpcinstance->midos[VPCBR_VMPORT]), "inboundFilterId", vpcinstance->midos[INST_PRECHAIN].uuid, NULL);
    if (rc) {
        LOGERROR("cannot attach midonet chain to midonet port: check midonet health\n");
        return (1);
    }

    rc = mido_update_port(&(vpcinstance->midos[VPCBR_VMPORT]), "outboundFilterId", vpcinstance->midos[INST_POSTCHAIN].uuid, NULL);
    if (rc) {
        LOGERROR("cannot attach midonet chain to midonet port: check midonet health\n");
        return (1);
    }

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int free_mido_config(mido_config * mido)
{
    int ret = 0, i = 0;

    if (!mido)
        return (0);

    EUCA_FREE(mido->eucahome);

    EUCA_FREE(mido->ext_eucanetdhostname);
    EUCA_FREE(mido->ext_rthostname);
    EUCA_FREE(mido->ext_rtaddr);
    EUCA_FREE(mido->ext_rtiface);
    EUCA_FREE(mido->ext_pubnw);
    EUCA_FREE(mido->ext_pubgwip);

    mido_free_midoname_list(mido->hosts, mido->max_hosts);
    EUCA_FREE(mido->hosts);

    mido_free_midoname_list(mido->routers, mido->max_routers);
    EUCA_FREE(mido->routers);

    mido_free_midoname_list(mido->bridges, mido->max_bridges);
    EUCA_FREE(mido->bridges);

    mido_free_midoname_list(mido->chains, mido->max_chains);
    EUCA_FREE(mido->chains);

    mido_free_midoname_list(mido->brports, mido->max_brports);
    EUCA_FREE(mido->brports);

    mido_free_midoname_list(mido->rtports, mido->max_rtports);
    EUCA_FREE(mido->rtports);

    mido_free_midoname_list(mido->ipaddrgroups, mido->max_ipaddrgroups);
    EUCA_FREE(mido->ipaddrgroups);

    free_mido_core(mido->midocore);
    EUCA_FREE(mido->midocore);

    for (i = 0; i < mido->max_vpcs; i++) {
        free_mido_vpc(&(mido->vpcs[i]));
    }
    EUCA_FREE(mido->vpcs);

    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        free_mido_vpc_secgroup(&(mido->vpcsecgroups[i]));
    }
    EUCA_FREE(mido->vpcsecgroups);

    bzero(mido, sizeof(mido_config));

    return (ret);
}

//!
//!
//!
//! @param[in] midocore
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int free_mido_core(mido_core * midocore)
{
    int ret = 0;

    if (!midocore)
        return (0);

    /*
       mido_free_midoname(&(midocore->midos[EUCART]));
       mido_free_midoname(&(midocore->midos[EUCART_BRPORT]));
       mido_free_midoname(&(midocore->midos[EUCABR_RTPORT]));
       mido_free_midoname(&(midocore->midos[EUCABR]));
       mido_free_midoname(&(midocore->midos[EUCART_GWPORT]));
       mido_free_midoname(&(midocore->midos[GWHOST]));
     */

    mido_free_midoname_list(midocore->midos, MIDOCOREEND);

    mido_free_midoname_list(midocore->brports, midocore->max_brports);
    EUCA_FREE(midocore->brports);

    mido_free_midoname_list(midocore->rtports, midocore->max_rtports);
    EUCA_FREE(midocore->rtports);

    bzero(midocore, sizeof(mido_core));

    return (ret);
}

//!
//!
//!
//! @param[in] vpc
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int free_mido_vpc(mido_vpc * vpc)
{
    int ret = 0, i = 0;

    if (!vpc)
        return (0);

    mido_free_midoname_list(vpc->midos, VPCEND);

    mido_free_midoname_list(vpc->rtports, vpc->max_rtports);
    EUCA_FREE(vpc->rtports);

    for (i = 0; i < vpc->max_subnets; i++) {
        free_mido_vpc_subnet(&(vpc->subnets[i]));
    }
    EUCA_FREE(vpc->subnets);

    bzero(vpc, sizeof(mido_vpc));

    return (ret);
}

//!
//!
//!
//! @param[in] vpcsubnet
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int free_mido_vpc_subnet(mido_vpc_subnet * vpcsubnet)
{
    int ret = 0, i = 0;

    if (!vpcsubnet)
        return (0);

    mido_free_midoname_list(vpcsubnet->midos, VPCSUBNETEND);

    mido_free_midoname_list(vpcsubnet->brports, vpcsubnet->max_brports);
    EUCA_FREE(vpcsubnet->brports);

    mido_free_midoname_list(vpcsubnet->dhcphosts, vpcsubnet->max_dhcphosts);
    EUCA_FREE(vpcsubnet->dhcphosts);

    for (i = 0; i < vpcsubnet->max_instances; i++) {
        free_mido_vpc_instance(&(vpcsubnet->instances[i]));
    }
    EUCA_FREE(vpcsubnet->instances);

    bzero(vpcsubnet, sizeof(mido_vpc_subnet));

    return (ret);
}

//!
//!
//!
//! @param[in] vpcinstance
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int free_mido_vpc_instance(mido_vpc_instance * vpcinstance)
{
    int ret = 0;

    if (!vpcinstance)
        return (0);

    mido_free_midoname_list(vpcinstance->midos, VPCINSTANCEEND);

    bzero(vpcinstance, sizeof(mido_vpc_instance));

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] vpcsubnet
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int delete_mido_vpc_subnet(mido_config * mido, mido_vpc_subnet * vpcsubnet)
{
    int rc = 0, ret = 0, i = 0;

    if (!mido || !vpcsubnet || !strlen(vpcsubnet->name)) {
        LOGFATAL("BUG: invalid input parameters\n");
        return (1);
    }

    LOGDEBUG("DELETING SUBNET '%s'\n", vpcsubnet->name);

    // delete all instances on this subnet
    for (i = 0; i < vpcsubnet->max_instances; i++) {
        if (strlen(vpcsubnet->instances[i].name)) {
            rc = delete_mido_vpc_instance(&(vpcsubnet->instances[i]));
        }
    }

    //  rc = mido_delete_route(&(vpcsubnet->vpcrt_route));

    rc = mido_delete_port(&(vpcsubnet->midos[VPCRT_BRPORT]));

    rc = mido_delete_dhcp(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->midos[VPCBR_DHCP]));

    rc = mido_delete_bridge(&(vpcsubnet->midos[VPCBR]));

    rc = delete_mido_meta_subnet_veth(mido, vpcsubnet->name);

    free_mido_vpc_subnet(vpcsubnet);

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] vpc
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int delete_mido_vpc(mido_config * mido, mido_vpc * vpc)
{
    int rc = 0, ret = 0, i = 0;

    if (!mido || !vpc || !vpc->name) {
        LOGFATAL("BUG: invalid input parameters\n");
        return (1);
    }

    LOGDEBUG("DELETING VPC: %s, %s\n", vpc->name, vpc->midos[VPCRT].name);

    rc = mido_delete_port(&(vpc->midos[EUCABR_DOWNLINK]));
    rc = mido_delete_router(&(vpc->midos[VPCRT]));

    /*
        rc = mido_delete_chain(&(vpc->midos[VPCRT_PREETHERCHAIN]));
        rc = mido_delete_chain(&(vpc->midos[VPCRT_PREMETACHAIN]));
        rc = mido_delete_chain(&(vpc->midos[VPCRT_PREVPCINTERNALCHAIN]));
        rc = mido_delete_chain(&(vpc->midos[VPCRT_PREFWCHAIN]));
    */
    rc = mido_delete_chain(&(vpc->midos[VPCRT_PREELIPCHAIN]));
    rc = mido_delete_chain(&(vpc->midos[VPCRT_PRECHAIN]));
    rc = mido_delete_chain(&(vpc->midos[VPCRT_POSTCHAIN]));

    for (i = 0; i < vpc->max_subnets; i++) {
        if (strlen(vpc->subnets[i].name)) {
            rc = delete_mido_vpc_subnet(mido, &(vpc->subnets[i]));
        }
    }

    rc = delete_mido_meta_vpc_namespace(mido, vpc);

    free_mido_vpc(vpc);

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] vpc
//! @param[in] vpcsubnet
//! @param[in] subnet
//! @param[in] slashnet
//! @param[in] gw
//! @param[in] instanceDNSDomain
//! @param[in] instanceDNSServers
//! @param[in] max_instanceDNSServers
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int create_mido_vpc_subnet(mido_config * mido, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet, char *subnet, char *slashnet, char *gw, char *instanceDNSDomain,
                           u32 * instanceDNSServers, int max_instanceDNSServers)
{
    int rc = 0, ret = 0, i = 0, found = 0;
    char name_buf[32], *tapiface = NULL;

    rc = mido_create_port(&(vpc->midos[VPCRT]), "InteriorRouter", gw, subnet, slashnet, &(vpcsubnet->midos[VPCRT_BRPORT]));
    if (rc) {
        LOGERROR("cannot create midonet router port: check midonet health\n");
        return (1);
    }

    rc = mido_create_route(&(vpc->midos[VPCRT]), &(vpcsubnet->midos[VPCRT_BRPORT]), "0.0.0.0", "0", subnet, slashnet, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create midonet router route: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vb_%s_%s", vpc->name, vpcsubnet->name);
    rc = mido_create_bridge("euca_tenant_1", name_buf, &(vpcsubnet->midos[VPCBR]));
    if (rc) {
        LOGERROR("cannot create midonet bridge: check midonet health\n");
        return (1);
    }

    rc = mido_create_port(&(vpcsubnet->midos[VPCBR]), "InteriorBridge", NULL, NULL, NULL, &(vpcsubnet->midos[VPCBR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create midonet bridge port: check midonet health\n");
        return (1);
    }

    rc = mido_link_ports(&(vpcsubnet->midos[VPCRT_BRPORT]), &(vpcsubnet->midos[VPCBR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create midonet router <-> bridge link: check midonet health\n");
        return (1);
    }
    // setup DHCP on the bridge for this subnet
    rc = mido_create_dhcp(&(vpcsubnet->midos[VPCBR]), subnet, slashnet, gw, instanceDNSServers, max_instanceDNSServers, &(vpcsubnet->midos[VPCBR_DHCP]));
    if (rc) {
        LOGERROR("cannot create midonet dhcp server: check midonet health\n");
        return (1);
    }
    // meta tap

    // find the interface mapping
    found = 0;
    for (i = 0; i < mido->max_hosts && !found; i++) {
        if (strstr(mido->hosts[i].name, mido->ext_eucanetdhostname)) {
            mido_copy_midoname(&(vpcsubnet->midos[VPCBR_METAHOST]), &(mido->hosts[i]));
            found = 1;
        }
    }

    if (vpcsubnet->midos[VPCBR_METAHOST].init) {
        rc = create_mido_meta_subnet_veth(mido, vpc, vpcsubnet->name, subnet, slashnet, &tapiface);
        if (rc || !tapiface) {
            LOGERROR("cannot create metadata taps: check log output for details\n");
            ret = 1;
        } else {
            LOGDEBUG("created tap iface: %s\n", SP(tapiface));
        }

        if (!ret) {
            // create tap port
            rc = mido_create_port(&(vpcsubnet->midos[VPCBR]), "ExteriorBridge", NULL, NULL, NULL, &(vpcsubnet->midos[VPCBR_METAPORT]));
            if (rc) {
                LOGERROR("cannot create midonet bridge port: check midonet health\n");
                ret = 1;
            }
        }

        if (!ret) {
            // link tap port
            rc = mido_link_host_port(&(vpcsubnet->midos[VPCBR_METAHOST]), tapiface, &(vpcsubnet->midos[VPCBR]), &(vpcsubnet->midos[VPCBR_METAPORT]));
            if (rc) {
                LOGERROR("cannot link port to host interface: check midonet health\n");
                ret = 1;
            }
        }
    }
    EUCA_FREE(tapiface);

    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] midocore
//! @param[in] vpc
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int create_mido_vpc(mido_config * mido, mido_core * midocore, mido_vpc * vpc)
{
    int rc = 0;
    char name_buf[32], nw[32], sn[32], ip[32], gw[32], *tmpstr = NULL;

    tmpstr = hex2dot(mido->int_rtnw);
    snprintf(nw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    snprintf(sn, 32, "%d", mido->int_rtsn);

    tmpstr = hex2dot(mido->int_rtnw + vpc->rtid);
    snprintf(ip, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    tmpstr = hex2dot(mido->int_rtaddr);
    snprintf(gw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    //  snprintf(vpc->name, 16, "%s", name);
    snprintf(name_buf, 32, "vr_%s_%d", vpc->name, vpc->rtid);
    rc = mido_create_router("euca_tenant_1", name_buf, &(vpc->midos[VPCRT]));
    if (rc) {
        LOGERROR("cannot create midonet router: check midonet health\n");
        return (1);
    }
    // link the vpc network and euca network
    rc = mido_create_port(&(midocore->midos[EUCABR]), "InteriorBridge", NULL, NULL, NULL, &(vpc->midos[EUCABR_DOWNLINK]));
    if (rc) {
        LOGERROR("cannot create midonet bridge port: check midonet health\n");
        return (1);
    }

    rc = mido_create_port(&(vpc->midos[VPCRT]), "InteriorRouter", ip, nw, sn, &(vpc->midos[VPCRT_UPLINK]));
    if (rc) {
        LOGERROR("cannot create midonet router port: check midonet health\n");
        return (1);
    }

    rc = mido_create_route(&(vpc->midos[VPCRT]), &(vpc->midos[VPCRT_UPLINK]), "0.0.0.0", "0", nw, sn, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create midonet router route: check midonet health\n");
        return (1);
    }

    rc = mido_create_route(&(vpc->midos[VPCRT]), &(vpc->midos[VPCRT_UPLINK]), "0.0.0.0", "0", "0.0.0.0", "0", gw, "0", NULL);
    if (rc) {
        LOGERROR("cannot create midonet router route: check midonet health\n");
        return (1);
    }

    rc = mido_link_ports(&(vpc->midos[EUCABR_DOWNLINK]), &(vpc->midos[VPCRT_UPLINK]));
    if (rc) {
        LOGERROR("cannot create midonet bridge <-> router link: check midonet health\n");
        return (1);
    }
    // create the chains
    snprintf(name_buf, 32, "vc_%s_prechain", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PRECHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }

    
    /*
    snprintf(name_buf, 32, "vc_%s_preether", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PREETHERCHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vc_%s_premeta", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PREMETACHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vc_%s_prevpcinternal", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PREVPCINTERNALCHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vc_%s_prefw", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PREFWCHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }
    */    

    snprintf(name_buf, 32, "vc_%s_preelip", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PREELIPCHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vc_%s_postchain", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_POSTCHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }
    // add the jump chains
    /*
        rc = mido_create_rule(&(vpc->midos[VPCRT_PRECHAIN]), NULL, "position", "1", "type", "jump", "jumpChainId", vpc->midos[VPCRT_PREETHERCHAIN].uuid, NULL);
        rc = mido_create_rule(&(vpc->midos[VPCRT_PRECHAIN]), NULL, "position", "2", "type", "jump", "jumpChainId", vpc->midos[VPCRT_PREMETACHAIN].uuid, NULL);
        rc = mido_create_rule(&(vpc->midos[VPCRT_PRECHAIN]), NULL, "position", "3", "type", "jump", "jumpChainId", vpc->midos[VPCRT_PREVPCINTERNALCHAIN].uuid, NULL);
        rc = mido_create_rule(&(vpc->midos[VPCRT_PRECHAIN]), NULL, "position", "5", "type", "jump", "jumpChainId", vpc->midos[VPCRT_PREFWCHAIN].uuid, NULL);
    */
    rc = mido_create_rule(&(vpc->midos[VPCRT_PRECHAIN]), NULL, "position", "1", "type", "jump", "jumpChainId", vpc->midos[VPCRT_PREELIPCHAIN].uuid, NULL);


    // meta
#if 0
    tmpstr = hex2dot(mido->enabledCLCIp);
    rc = mido_create_rule(&(vpc->midos[VPCRT_PREMETACHAIN]), NULL, "type", "dnat", "flowAction", "continue", "ipAddrGroupDst", midocore->midos[METADATA_IPADDRGROUP].uuid,
                          "natTargets", "jsonlist", "natTargets:addressTo", tmpstr, "natTargets:addressFrom", tmpstr, "natTargets:portFrom", "8773", "natTargets:portTo", "8773",
                          "natTargets:END", "END", NULL);
    EUCA_FREE(tmpstr);
    rc = mido_create_rule(&(vpc->midos[VPCRT_POSTCHAIN]), NULL, "type", "rev_dnat", "flowAction", "accept", NULL);
#endif

    // apply the chains to the vpc router
    rc = mido_update_router(&(vpc->midos[VPCRT]), "inboundFilterId", vpc->midos[VPCRT_PRECHAIN].uuid, NULL);
    if (rc) {
        LOGERROR("cannot attach midonet chain to midonet router: check midonet health\n");
        return (1);
    }

    rc = mido_update_router(&(vpc->midos[VPCRT]), "outboundFilterId", vpc->midos[VPCRT_POSTCHAIN].uuid, NULL);
    if (rc) {
        LOGERROR("cannot attach midonet chain to midonet router: check midonet health\n");
        return (1);
    }

    rc = create_mido_meta_vpc_namespace(mido, vpc);
    if (rc) {
        LOGERROR("cannot create netns for VPC %s: check above log for details\n", vpc->name);
        return (1);
    }

    return (0);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] midocore
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int delete_mido_core(mido_config * mido, mido_core * midocore)
{
    int rc;

    rc = mido_unlink_host_port(&(midocore->midos[GWHOST]), &(midocore->midos[EUCART_GWPORT]));
    sleep(1);

    rc = mido_delete_bridge(&(midocore->midos[EUCABR]));
    sleep(1);

    rc = mido_delete_router(&(midocore->midos[EUCART]));
    sleep(1);

    rc = delete_mido_meta_core(mido);

    return (0);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] midocore
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int create_mido_core(mido_config * mido, mido_core * midocore)
{
    int ret = 0, rc = 0;
    char nw[32], sn[32], gw[32], *tmpstr = NULL, pubnw[32], pubnm[32];

    tmpstr = hex2dot(mido->int_rtnw);
    snprintf(nw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    tmpstr = hex2dot(mido->int_rtaddr);
    snprintf(gw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    snprintf(sn, 32, "%d", mido->int_rtsn);

    LOGINFO("creating mido core router\n");
    //  rc = mido_create_router("euca_tenant_1", "eucart", &(midocore->midos[EUCART]));
    rc = mido_create_router("euca_tenant_1", "eucart", &(midocore->midos[EUCART]));
    if (rc) {
        LOGERROR("cannot create router: check midonet health\n");
        return (1);
    }

    rc = mido_create_port(&(midocore->midos[EUCART]), "InteriorRouter", gw, nw, sn, &(midocore->midos[EUCART_BRPORT]));
    if (rc) {
        LOGERROR("cannot create router port: check midonet health\n");
        return (1);
    }

    rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_BRPORT]), "0.0.0.0", "0", nw, sn, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create router route: check midonet health\n");
        return (1);
    }

    cidr_split(mido->ext_pubnw, pubnw, pubnm, NULL, NULL);
    //  rc = mido_create_port(&(midocore->midos[EUCART]), "ExteriorRouter", "10.111.5.57", "10.111.0.0", "16", &(midocore->midos[EUCART_GWPORT]));
    rc = mido_create_port(&(midocore->midos[EUCART]), "ExteriorRouter", mido->ext_rtaddr, pubnw, pubnm, &(midocore->midos[EUCART_GWPORT]));
    if (rc) {
        LOGERROR("cannot create router port: check midonet health\n");
        return (1);
    }
    //  rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_GWPORT]), "0.0.0.0", "0", "10.111.0.0", "16", NULL, "0", NULL);
    rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_GWPORT]), "0.0.0.0", "0", pubnw, pubnm, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create router route: check midonet health\n");
        return (1);
    }
    //  rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_GWPORT]), "0.0.0.0", "0", "0.0.0.0", "0", "10.111.5.34", "0", NULL);
    rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_GWPORT]), "0.0.0.0", "0", "0.0.0.0", "0", mido->ext_pubgwip, "0", NULL);
    if (rc) {
        LOGERROR("cannot create router route: check midonet health\n");
        return (1);
    }

    rc = mido_link_host_port(&(midocore->midos[GWHOST]), mido->ext_rtiface, &(midocore->midos[EUCART]), &(midocore->midos[EUCART_GWPORT]));
    if (rc) {
        LOGERROR("cannot link router port to host interface: check midonet health\n");
        return (1);
    }

    rc = mido_create_bridge("euca_tenant_1", "eucabr", &(midocore->midos[EUCABR]));
    if (rc) {
        LOGERROR("cannot create bridge: check midonet health\n");
        return (1);
    }

    rc = mido_create_port(&(midocore->midos[EUCABR]), "InteriorBridge", NULL, NULL, NULL, &(midocore->midos[EUCABR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create bridge port: check midonet health\n");
        return (1);
    }

    rc = mido_link_ports(&(midocore->midos[EUCART_BRPORT]), &(midocore->midos[EUCABR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create router <-> bridge link: check midonet health\n");
        return (1);
    }

    rc = mido_create_ipaddrgroup("euca_tenant_1", "metadata_ip", &(midocore->midos[METADATA_IPADDRGROUP]));
    if (rc) {
        LOGERROR("cannot create metadata ipaddrgroup: check midonet health\n");
        ret = 1;
    }

    rc = mido_create_ipaddrgroup_ip(&(midocore->midos[METADATA_IPADDRGROUP]), "169.254.169.254", NULL);
    if (rc) {
        // TODO
        ret = 1;
    }

    rc = create_mido_meta_core(mido);
    if (rc) {
        LOGERROR("cannot create metadata tap core bridge/devices: check above log for details\n");
        ret = 1;
    }

    return (ret);
}

//!
//!
//!
//! @param[in]  cidr
//! @param[out] outnet
//! @param[out] outslashnet
//! @param[out] outgw
//! @param[out] outplustwo
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int cidr_split(char *cidr, char *outnet, char *outslashnet, char *outgw, char *outplustwo)
{
    char *tok = NULL;
    char *cpy = NULL;
    u32 nw = 0, gw = 0;

    cpy = strdup(cidr);
    tok = strchr(cpy, '/');
    if (tok) {
        *tok = '\0';
        if (outnet) {
            snprintf(outnet, strlen(cpy) + 1, "%s", cpy);
        }
        nw = dot2hex(cpy);
        tok++;
        if (outslashnet) {
            snprintf(outslashnet, strlen(tok) + 1, "%s", tok);
        }
    }
    EUCA_FREE(cpy);

    if (nw) {
        gw = nw + 1;
        tok = hex2dot(gw);
        if (outgw) {
            snprintf(outgw, strlen(tok) + 1, "%s", tok);
        }
        EUCA_FREE(tok);

        if (outplustwo) {
            gw = nw + 2;
            tok = hex2dot(gw);
            snprintf(outplustwo, strlen(tok) + 1, "%s", tok);
        }
        EUCA_FREE(tok);
    }

    return (0);
}
