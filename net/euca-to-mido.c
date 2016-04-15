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
#include "dev_handler.h"
#include "euca_gni.h"
#include "midonet-api.h"
#include "euca-to-mido.h"
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

extern int midonet_api_system_changed;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int midocache_invalid = 0;

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
int do_metaproxy_teardown(mido_config * mido) {
    LOGDEBUG("tearing down metaproxy subsystem\n");
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
int do_metaproxy_setup(mido_config * mido) {
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
int do_metaproxy_maintain(mido_config * mido, int mode) {
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
int delete_mido_meta_core(mido_config * mido) {
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
int create_mido_meta_core(mido_config * mido) {
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH];
    sequence_executor cmds;

    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);

    rc = se_init(&cmds, rr, 2, 1);

    snprintf(cmd, EUCA_MAX_PATH, "brctl addbr metabr");
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "brctl setfd metabr 2");
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "brctl sethello metabr 2");
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
int delete_mido_meta_vpc_namespace(mido_config * mido, mido_vpc * vpc) {
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH], sid[16];
    sequence_executor cmds;
    struct timeval tv;

    eucanetd_timer_usec(&tv);
    // create a meta tap namespace/devices
    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);
    sscanf(vpc->name, "vpc-%8s", sid);

    rc = se_init(&cmds, rr, 10, 1);

    //    snprintf(cmd, EUCA_MAX_PATH, "ip link set vn2_%s down", sid);
    //    rc = se_add(&cmds, cmd, NULL, ignore_exit);

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

    LOGINFO("\tVPC ip namespace deleted in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
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
int create_mido_meta_vpc_namespace(mido_config * mido, mido_vpc * vpc) {
    int ret = 0, rc = 0;
    u32 nw, ip;
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH], *ipstr = NULL, sid[16];
    sequence_executor cmds;
    struct timeval tv;

    eucanetd_timer_usec(&tv);
    rc = read_mido_meta_vpc_namespace(mido, vpc);
    if (!rc) {
        LOGDEBUG("namespace (%s) already exists, skipping create\n", vpc->name);
        return (0);
    }

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

    LOGINFO("\tVPC ip namespace created in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (ret);
}

int read_mido_meta_vpc_namespace(mido_config * mido, mido_vpc * vpc) {
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH], sid[16];

    // create a meta tap namespace/devices
    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);
    sscanf(vpc->name, "vpc-%8s", sid);

    snprintf(cmd, EUCA_MAX_PATH, "/var/run/netns/%s", vpc->name);
    if (check_path(cmd)) {
        LOGDEBUG("cannot find VPC netns: %s\n", cmd);
        return (1);
    } else {
        LOGTRACE("found VPC netns: %s\n", cmd);
    }

    snprintf(cmd, EUCA_MAX_PATH, "vn2_%s", sid);
    if (!dev_exist(cmd)) {
        LOGDEBUG("cannot find VPC metataps vn2_%s\n", sid);
        return (1);
    } else {
        LOGTRACE("found VPC metataps vn2_%s\n", sid);
    }

    return (0);

    /*

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

     */
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
int delete_mido_meta_subnet_veth(mido_config * mido, char *name) {
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH], sid[16];
    sequence_executor cmds;
    struct timeval tv;

    eucanetd_timer_usec(&tv);
    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);
    sscanf(name, "subnet-%8s", sid);

    rc = se_init(&cmds, rr, 2, 1);

    //    snprintf(cmd, EUCA_MAX_PATH, "ip link set vn0_%s down", sid);
    //    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "ip link del vn0_%s", sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not delete subnet tap ifaces: see above log entries for details\n");
        ret = 1;
    }
    se_free(&cmds);

    LOGINFO("\tVPC subnet metadata veth deleted in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
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
int create_mido_meta_subnet_veth(mido_config * mido, mido_vpc * vpc, char *name, char *subnet, char *slashnet, char **tapiface) {
    int ret = 0, rc = 0;
    u32 nw, gw;
    char cmd[EUCA_MAX_PATH], rr[EUCA_MAX_PATH], *gateway = NULL, sid[16];
    sequence_executor cmds;
    struct timeval tv;

    eucanetd_timer_usec(&tv);
    // create a meta tap
    snprintf(rr, EUCA_MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", mido->eucahome);
    sscanf(name, "subnet-%8s", sid);

    snprintf(cmd, EUCA_MAX_PATH, "vn0_%s", sid);
    if (dev_exist(cmd)) {
        LOGDEBUG("subnet device (%s) already exists, skipping\n", cmd);
        *tapiface = calloc(16, sizeof (char));
        snprintf(*tapiface, 16, "vn0_%s", sid);
        return (0);
    }


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
        *tapiface = calloc(16, sizeof (char));
        snprintf(*tapiface, 16, "vn0_%s", sid);
    }
    se_free(&cmds);

    LOGINFO("\tVPC subnet metadata veth created in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (ret);
}

/**
 * Populates euca VPC models (data structures) from MidoNet models.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. 1 on any failure.
 */
int do_midonet_populate(mido_config * mido) {
    int i = 0, j = 0, k = 0, rc = 0, rtid = 0, natgrtid = 0, ret = 0;
    char subnetname[16], vpcname[16], sgname[16];
    char instanceId[16];
    char natgname[32];
    char tmpstr[64];
    char *iface = NULL;
    midoname **routers = NULL;
    int max_routers = 0;
    midoname **bridges = NULL;
    int max_bridges;
    midoname **chains = NULL;
    int max_chains = 0;
    mido_vpc_secgroup *vpcsecgroup = NULL;
    mido_vpc_instance *vpcinstance = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc_natgateway *vpcnatg = NULL;
    mido_vpc *vpc = NULL;
    struct timeval tv;

    eucanetd_timer_usec(&tv);
    rc = midonet_api_cache_refresh();
    if (rc) {
        LOGERROR("failed to retrieve objects from MidoNet.\n");
        return (1);
    }
    LOGINFO("\tMidoNet objects cached in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    rc = reinitialize_mido(mido);
    if (rc) {
        LOGERROR("failed to initialize euca-mido model data structures.\n");
    }
    LOGINFO("\treinitialize_mido() in %ld us.\n", eucanetd_timer_usec(&tv));

    // populated core
    rc = populate_mido_core(mido, mido->midocore);
    if (rc) {
        LOGERROR("failed to populate midonet core (eucabr, eucart): check midonet health\n");
        return (1);
    }
    LOGINFO("\tmido_core populated in %.2f ms.\n",  eucanetd_timer_usec(&tv) / 1000.0);

    // make sure that all core objects are in place
    rc = create_mido_core(mido, mido->midocore);
    if (rc) {
        LOGERROR("failed to setup midonet core: check midonet health\n");
        return (1);
    }
    if (midonet_api_system_changed == 1) {
        LOGINFO("\tvpcmido core created in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    } else {
        LOGINFO("\tvpcmido core maint in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    }

    // pattern
    // - find all VPC routers (and populate VPCs)
    // - for each VPC, find all subnets (and populate subnets)
    // - for each VPC, for each subnet, find all instances (and populate instances)

    // VPCs
    rc = mido_get_routers(VPCMIDO_TENANT, &routers, &max_routers);
    if (max_routers > 0) {
        mido->vpcs = EUCA_ZALLOC_C(max_routers, sizeof (mido_vpc));
    }
    for (i = 0; i < max_routers; i++) {
        LOGTRACE("inspecting mido router '%s'\n", routers[i]->name);
        bzero(vpcname, 16);
        sscanf(routers[i]->name, "vr_%12s_%d", vpcname, &rtid);
        if (strlen(vpcname)) {
            vpc = &(mido->vpcs[mido->max_vpcs]);
            mido->max_vpcs++;
            LOGTRACE("discovered VPC installed in midonet: %s\n", vpcname);

            snprintf(vpc->name, sizeof (vpc->name), "%s", vpcname);
            set_router_id(mido, rtid);
            vpc->rtid = rtid;
            rc = populate_mido_vpc(mido, mido->midocore, vpc);
            if (rc) {
                LOGERROR("cannot populate midonet VPC '%s': check midonet health\n", vpc->name);
                ret = 1;
            }
        }
    }
    LOGINFO("\tvpcs populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    // SUBNETS
    rc = mido_get_bridges(VPCMIDO_TENANT, &bridges, &max_bridges);
    for (i = 0; i < max_bridges; i++) {
        LOGTRACE("inspecting bridge '%s'\n", bridges[i]->name);

        bzero(vpcname, 16);
        bzero(subnetname, 16);

        sscanf(bridges[i]->name, "vb_%12s_%15s", vpcname, subnetname);
        if (strlen(vpcname) && strlen(subnetname)) {
            LOGDEBUG("discovered VPC subnet installed in midonet: %s/%s\n", vpcname, subnetname);
            find_mido_vpc(mido, vpcname, &vpc);
            if (vpc) {
                LOGDEBUG("found VPC matching discovered subnet: %s/%s\n", vpc->name, subnetname);
                vpc->subnets = EUCA_REALLOC_C(vpc->subnets, (vpc->max_subnets + 1), sizeof (mido_vpc_subnet));
                vpcsubnet = &(vpc->subnets[vpc->max_subnets]);
                vpc->max_subnets++;
                bzero(vpcsubnet, sizeof (mido_vpc_subnet));
                snprintf(vpcsubnet->name, 16, "%s", subnetname);
                snprintf(vpcsubnet->vpcname, 16, "%s", vpcname);
                vpcsubnet->vpc = vpc;
                rc = populate_mido_vpc_subnet(mido, vpc, vpcsubnet);
                if (rc) {
                    LOGERROR("cannot populate midonet VPC '%s' subnet '%s': check midonet health\n", vpc->name, vpcsubnet->name);
                    ret = 1;
                }

                // Search for NAT Gateways
                for (j = 0; j < max_routers; j++) {
                    if (routers[j] == NULL) {
                        continue;
                    }
                    natgname[0] = '\0';
                    natgrtid = 0;
                    snprintf(tmpstr, 64, "natr_%%21s_%s_%%d", subnetname);
                    sscanf(routers[j]->name, tmpstr, natgname, &natgrtid);
                    if ((strlen(natgname)) && (natgrtid != 0)) {
                        LOGDEBUG("discovered %s in %s installed in midonet\n", natgname, subnetname);
                        vpcsubnet->natgateways = EUCA_REALLOC_C(vpcsubnet->natgateways, vpcsubnet->max_natgateways + 1, sizeof (mido_vpc_natgateway));
                        vpcnatg = &(vpcsubnet->natgateways[vpcsubnet->max_natgateways]);
                        (vpcsubnet->max_natgateways)++;
                        bzero(vpcnatg, sizeof (mido_vpc_natgateway));
                        snprintf(vpcnatg->name, sizeof (vpcnatg->name), "%s", natgname);
                        set_router_id(mido, natgrtid);
                        vpcnatg->rtid = natgrtid;
                        rc = populate_mido_vpc_natgateway(mido, vpc, vpcsubnet, vpcnatg);
                        if (rc) {
                            LOGERROR("cannot populate %s: check midonet health\n", natgname);
                            ret = 1;
                        }
                    }
                }
            }
        }
    }
    EUCA_FREE(bridges);
    EUCA_FREE(routers);
    LOGINFO("\tvpc subnets populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    // SECGROUPS
    rc = mido_get_chains(VPCMIDO_TENANT, &chains, &max_chains);
    for (i = 0; i < max_chains; i++) {
        LOGTRACE("inspecting chain '%s'\n", chains[i]->name);
        sgname[0] = '\0';

        sscanf(chains[i]->name, "sg_ingress_%11s", sgname);
        if (strlen(sgname)) {
            LOGDEBUG("discovered VPC security group installed in midonet: %s\n", sgname);
            mido->vpcsecgroups = EUCA_REALLOC_C(mido->vpcsecgroups, (mido->max_vpcsecgroups + 1), sizeof (mido_vpc_secgroup));
            vpcsecgroup = &(mido->vpcsecgroups[mido->max_vpcsecgroups]);
            (mido->max_vpcsecgroups)++;
            bzero(vpcsecgroup, sizeof (mido_vpc_secgroup));
            snprintf(vpcsecgroup->name, 16, "%s", sgname);
            rc = populate_mido_vpc_secgroup(mido, vpcsecgroup);
            if (rc) {
                LOGERROR("cannot populate mido SG '%s': check midonet health\n", vpcsecgroup->name);
                ret = 1;
            }
        }
    }
    EUCA_FREE(chains);
    LOGINFO("\tsecurity groups populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    // INSTANCES
    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            midoname **brports = vpcsubnet->subnetbr->ports;
            int max_brports = vpcsubnet->subnetbr->max_ports;
            for (k = 0; k < max_brports; k++) {
                bzero(instanceId, 16);
                rc = mido_getel_midoname(brports[k], "interfaceName", &iface);

                if (iface) {
                    sscanf(iface, "vn_%s", instanceId);

                    if (strlen(instanceId)) {
                        LOGDEBUG("discovered VPC subnet instance/interface: %s/%s/%s\n", vpc->name, vpcsubnet->name, instanceId);

                        vpcsubnet->instances = EUCA_REALLOC_C(vpcsubnet->instances,
                                (vpcsubnet->max_instances + 1), sizeof (mido_vpc_instance));
                        vpcinstance = &(vpcsubnet->instances[vpcsubnet->max_instances]);
                        bzero(vpcinstance, sizeof (mido_vpc_instance));
                        vpcsubnet->max_instances++;
                        snprintf(vpcinstance->name, INTERFACE_ID_LEN, "%s", instanceId);

                        rc = populate_mido_vpc_instance(mido, mido->midocore, vpc, vpcsubnet, vpcinstance);
                        if (rc) {
                            LOGERROR("could not populate instance: check mido health\n");
                            ret = 1;
                        }
                    }
                }
                EUCA_FREE(iface);
            }
        }
    }
    LOGINFO("\tinstances populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    // END population phase
    return (ret);
}

/**
 * Teardown all artifacts created by VPCMIDO driver.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_teardown(mido_config * mido) {
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
        delete_mido_vpc_secgroup(mido, &(mido->vpcsecgroups[i]));
    }

    char *bgprecovery = NULL;
    bgprecovery = discover_mido_bgps(mido);
    if (bgprecovery && strlen(bgprecovery)) {
        LOGINFO("mido BGP configuration (for manual recovery):\n%s\n", bgprecovery);
    }
    EUCA_FREE(bgprecovery);

    if (mido->flushmode == FLUSH_ALL) {
        LOGINFO("deleting mido core\n");
        delete_mido_core(mido, mido->midocore);
    } else {
        LOGDEBUG("skipping the delete of midocore - FLUSH_DYNAMIC selected.\n");
    }

    do_midonet_delete_all(mido);
    free_mido_config(mido);

    return (ret);
}

/**
 * Removes all bridges, routers, chains, and ip-address-groups from mido
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_delete_all(mido_config *mido) {
    int ret = 0, rc = 0;

    rc = midonet_api_cache_refresh();
    if (rc) {
        LOGERROR("cannot populate midocache prior to cleanup: check midonet health\n");
        return (1);
    }
    midonet_api_delete_all();

    return (ret);
}

/**
 * Tags objects that are in both GNI and mido.
 * Creates the meta-data instance/interface IP map file.
 * @param gni [in] Global Network Information to be applied.
 * @param appliedGni [in] most recently applied global network state.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. 1 otherwise.
 */
int do_midonet_update_pass1(globalNetworkInfo *gni, globalNetworkInfo *appliedGni, mido_config *mido) {
    int ret = 0, i = 0, j = 0, k = 0, rc = 0;
    int vpcidx = 0;
    int vpcsubnetidx = 0;
    int vpcnatgidx = 0;
    int vpcroutetidx = 0;
    int vpcsgidx = 0;
    int vpcinstanceidx = 0;
    char *privIp = NULL, mapfile[EUCA_MAX_PATH];
    FILE *PFH = NULL;

/*
    int found = 0;
    u32 *hexips = NULL;
    int max_hexips = 0;
*/

    mido_vpc *vpc = NULL;
    mido_vpc_secgroup *vpcsecgroup = NULL;
    mido_vpc_instance *vpcinstance = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc_natgateway *vpcnatgateway = NULL;

    gni_vpc *gnivpc = NULL;
    gni_vpcsubnet *gnivpcsubnet = NULL;
    gni_nat_gateway *gninatgateway = NULL;
    gni_route_table *gniroutetable = NULL;
    gni_instance *gniinstance = NULL;
    gni_secgroup *gnisecgroup = NULL;

    gni_vpc *appliedvpc = NULL;
    gni_vpcsubnet *appliedvpcsubnet = NULL;
    gni_nat_gateway *appliednatgateway = NULL;
    gni_route_table *appliedroutetable = NULL;
    gni_instance *appliedinstance = NULL;
    gni_secgroup *appliedsecgroup = NULL;

    // pass1: ensure that the meta-data map is populated right away
    snprintf(mapfile, EUCA_MAX_PATH, "%s/var/run/eucalyptus/eucanetd_vpc_instance_ip_map", mido->eucahome);
    unlink(mapfile);
    PFH = fopen(mapfile, "w");
    if (!PFH) {
        LOGERROR("cannot open VPC map file %s: check permissions and disk capacity\n", mapfile);
        ret = 1;
    }

    // pass1 - tag everything that is both in GNI and in MIDO
    // pass1: do vpcs and subnets
    if ((gni->max_vpcs > 0) && (ret == 0)) {
        // Allocate vpcs buffer for the worst case (add all VPCs in GNI)
        mido->vpcs = EUCA_REALLOC_C(mido->vpcs, mido->max_vpcs + gni->max_vpcs, sizeof (mido_vpc));
    }
    vpcidx = 0;
    for (i = 0; i < gni->max_vpcs; i++) {
        appliedvpc = NULL;
        gnivpc = &(gni->vpcs[i]);
        rc = find_mido_vpc(mido, gnivpc->name, &vpc);
        if (rc) {
            LOGTRACE("pass1: global VPC %s in mido: N\n", gnivpc->name);
        } else {
            LOGTRACE("pass1: global VPC %s in mido: Y\n", gnivpc->name);
            if (appliedGni) {
                if (vpc->gniVpc) {
                    appliedvpc = vpc->gniVpc;
                } else {
                    appliedvpc = gni_get_vpc(appliedGni, vpc->name, &vpcidx);
                }
            }
            if (!vpc->population_failed && !cmp_gni_vpc(appliedvpc, gnivpc)) {
                LOGEXTREME("12095: %s fully implemented \n", vpc->name);
                vpc->midopresent = 1;
            } else {
                vpc->midopresent = 0;
            }
            vpc->gniVpc = gnivpc;
            vpc->gnipresent = 1;
            gnivpc->mido_present = vpc;

            if ((gnivpc->max_subnets > 0) && (ret == 0)) {
                // Allocate vpcsubnets buffer for the worst case
                vpc->subnets = EUCA_REALLOC_C(vpc->subnets, vpc->max_subnets + gnivpc->max_subnets, sizeof (mido_vpc_subnet));
            }
        }

        vpcsubnetidx = 0;
        for (j = 0; j < gnivpc->max_subnets; j++) {
            appliedvpcsubnet = NULL;
            gnivpcsubnet = &(gnivpc->subnets[j]);
            rc = find_mido_vpc_subnet(vpc, gnivpcsubnet->name, &vpcsubnet);
            if (rc) {
                LOGTRACE("pass1: global VPC SUBNET %s in mido: N\n", gnivpcsubnet->name);
            } else {
                LOGTRACE("pass1: global VPC SUBNET %s in mido: Y\n", gnivpcsubnet->name);
                if (appliedGni) {
                    if (vpcsubnet->gniSubnet) {
                        appliedvpcsubnet = vpcsubnet->gniSubnet;
                    } else {
                        appliedvpcsubnet = gni_get_vpcsubnet(appliedvpc, vpcsubnet->name, &vpcsubnetidx);
                    }
                }
                if (!vpcsubnet->population_failed && !cmp_gni_vpcsubnet(appliedvpcsubnet, gnivpcsubnet)) {
                    LOGEXTREME("12095: %s fully implemented \n", vpcsubnet->name);
                    vpcsubnet->midopresent = 1;
                } else {
                    vpcsubnet->midopresent = 0;
                }
                vpcsubnet->gniSubnet = gnivpcsubnet;
                vpcsubnet->gnipresent = 1;
                gnivpcsubnet->mido_present = vpcsubnet;
                if ((gnivpcsubnet->max_interfaces > 0) && (ret == 0)) {
                    // Allocate interfaces buffer for the worst case
                    vpcsubnet->instances = EUCA_REALLOC_C(vpcsubnet->instances, vpcsubnet->max_instances + gnivpcsubnet->max_interfaces, sizeof (mido_vpc_instance));
                }
                if ((gnivpc->max_natGateways > 0) && (ret == 0)) {
                    // Allocate natgateways buffer for the worst case
                    vpcsubnet->natgateways = EUCA_REALLOC_C(vpcsubnet->natgateways, vpcsubnet->max_natgateways + gnivpc->max_natGateways, sizeof (mido_vpc_natgateway));
                }
            }

            vpcinstanceidx = 0;
            for (k = 0; k < gnivpcsubnet->max_interfaces; k++) {
                appliedinstance = NULL;
                gniinstance = gnivpcsubnet->interfaces[k];
                privIp = hex2dot(gniinstance->privateIp);
                if (PFH) fprintf(PFH, "%s %s %s\n", SP(gniinstance->vpc), SP(gniinstance->name), SP(privIp));
                EUCA_FREE(privIp);

                if (vpcsubnet != NULL) {
                    rc = find_mido_vpc_instance(vpcsubnet, gniinstance->name, &vpcinstance);
                } else {
                    rc = 1;
                }
                if (rc) {
                    LOGTRACE("pass1: global VPC INSTANCE/INTERFACE %s in mido: N\n", gniinstance->name);
                } else {
                    LOGTRACE("pass1: global VPC INSTANCE/INTERFACE %s in mido: Y\n", gniinstance->name);
                    if (appliedGni) {
                        if (vpcinstance->gniInst) {
                            appliedinstance = vpcinstance->gniInst;
                        } else {
                            appliedinstance = gni_get_interface(appliedvpcsubnet, vpcinstance->name, &vpcinstanceidx);
                        }
                    }
                    if (!vpcinstance->population_failed && !cmp_gni_interface(
                            appliedinstance, gniinstance, &(vpcinstance->pubip_changed),
                            &(vpcinstance->srcdst_changed), &(vpcinstance->host_changed),
                            &(vpcinstance->sg_changed))) {
                        LOGEXTREME("12095: %s fully implemented \n", vpcinstance->name);
                        vpcinstance->midopresent = 1;
                    } else {
                        vpcinstance->midopresent = 0;
                    }
                    vpcinstance->gniInst = gniinstance;
                    vpcinstance->gnipresent = 1;
                    gniinstance->mido_present = vpcinstance;
                }
            }
        }
        
        vpcnatgidx = 0;
        for (j = 0; j < gnivpc->max_natGateways; j++) {
            appliednatgateway = NULL;
            gninatgateway = &(gnivpc->natGateways[j]);
            rc = find_mido_vpc_natgateway(vpc, gninatgateway->name, &vpcnatgateway);
            if (rc) {
                LOGTRACE("pass1: global VPC NAT Gateway %s in mido: N\n", gninatgateway->name);
            } else {
                LOGTRACE("pass1: global VPC NAT Gateway %s in mido: Y\n", gninatgateway->name);
                if (appliedGni) {
                    if (vpcnatgateway->gniNatGateway) {
                        appliednatgateway = vpcnatgateway->gniNatGateway;
                    } else {
                        appliednatgateway = gni_get_natgateway(appliedvpc, vpcnatgateway->name, &vpcnatgidx);
                    }
                }
                if (!vpcnatgateway->population_failed && !cmp_gni_nat_gateway(appliednatgateway, gninatgateway)) {
                    LOGEXTREME("12095: %s fully implemented \n", vpcnatgateway->name);
                    vpcnatgateway->midopresent = 1;
                } else {
                    vpcnatgateway->midopresent = 0;
                }
                vpcnatgateway->gniNatGateway = gninatgateway;
                vpcnatgateway->gnipresent = 1;
                gninatgateway->mido_present = vpcnatgateway;
            }
        }

        // detect changes in route tables
        vpcroutetidx = 0;
        for (j = 0; j < gnivpc->max_routeTables; j++) {
            appliedroutetable = NULL;
            gniroutetable = &(gnivpc->routeTables[j]);
            if (appliedGni) {
                appliedroutetable = gni_get_routetable(appliedvpc, gniroutetable->name, &vpcroutetidx);
                if (!cmp_gni_route_table(appliedroutetable, gniroutetable)) {
                    LOGEXTREME("12095: %s no changes\n", gniroutetable->name);
                    gniroutetable->changed = 0;
                } else {
                    gniroutetable->changed = 1;
                }
            } else {
                gniroutetable->changed = -1;
            }
        }
    }

    if (PFH) fclose(PFH);

    // pass1: do security groups
    if ((gni->max_secgroups > 0) && (ret == 0)) {
        // Allocate secgroups buffer for the worst case (add all secgroups in GNI)
        mido->vpcsecgroups = EUCA_REALLOC_C(mido->vpcsecgroups, mido->max_vpcsecgroups + gni->max_secgroups, sizeof (mido_vpc_secgroup));
    }
    vpcsgidx = 0;
    for (i = 0; i < gni->max_secgroups; i++) {
        appliedsecgroup = NULL;
        gnisecgroup = &(gni->secgroups[i]);
        rc = find_mido_vpc_secgroup(mido, gnisecgroup->name, &vpcsecgroup);
        if (rc) {
            LOGTRACE("pass1: global VPC SECGROUP %s in mido: N\n", gnisecgroup->name);
        } else {
            LOGTRACE("pass1: global VPC SECGROUP %s in mido: Y\n", gnisecgroup->name);
            if (appliedGni) {
                if (vpcsecgroup->gniSecgroup) {
                    appliedsecgroup = vpcsecgroup->gniSecgroup;
                } else {
                    appliedsecgroup = gni_get_secgroup(appliedGni, vpcsecgroup->name, &vpcsgidx);
                }
            }
            if (!vpcsecgroup->population_failed && !cmp_gni_secgroup(appliedsecgroup,
                    gnisecgroup, &(vpcsecgroup->ingress_changed), &(vpcsecgroup->egress_changed),
                    &(vpcsecgroup->interfaces_changed))) {
                LOGEXTREME("12095: %s fully implemented \n", vpcsecgroup->name);
                vpcsecgroup->midopresent = 1;
            } else {
                vpcsecgroup->midopresent = 0;
                // skip SG egress and ingress chain flush on eucanetd restart (if number of rules matches)
                if (appliedsecgroup == NULL) {
                    if (vpcsecgroup->egress && (gnisecgroup->max_egress_rules == vpcsecgroup->egress->rules_count)) {
                        vpcsecgroup->egress_changed = 0;
                    }
                    if (vpcsecgroup->ingress && (gnisecgroup->max_ingress_rules == vpcsecgroup->ingress->rules_count)) {
                        vpcsecgroup->ingress_changed = 0;
                    }
                }
            }
            vpcsecgroup->gniSecgroup = gnisecgroup;
            vpcsecgroup->gnipresent = 1;
            gnisecgroup->mido_present = vpcsecgroup;
            
/*
            // tag SG interface IP addresses
            for (j = 0; j < gnisecgroup->max_interfaces; j++) {
                gniinstance = gnisecgroup->interfaces[j];

                found = 0;
                hexips = NULL;
                max_hexips = 0;
                if (vpcsecgroup->iag_pub) {
                    hexips = vpcsecgroup->iag_pub->hexips;
                    max_hexips = vpcsecgroup->iag_pub->max_ips;
                    if (max_hexips > 0) {
                        vpcsecgroup->gnipresent_pubips = EUCA_REALLOC_C(vpcsecgroup->gnipresent_pubips, max_hexips, sizeof (int));
                        bzero (vpcsecgroup->gnipresent_pubips, max_hexips * sizeof (int));
                    }
                }
                for (k = 0; k < max_hexips && !found; k++) {
                    if ((hexips[k] == 0) || (gniinstance->publicIp == 0)) {
                        continue;
                    }
                    if (hexips[k] == gniinstance->publicIp) {
                        vpcsecgroup->gnipresent_pubips[k] = 1;
                        found = 1;
                        LOGEXTREME("pass1: \tglobal SG pubip %x in mido: Y\n", gniinstance->publicIp);
                    }
                }
                if (!found) {
                    LOGEXTREME("pass1: \tglobal SG pubip %x in mido: N\n", gniinstance->publicIp);
                }

                found = 0;
                hexips = NULL;
                max_hexips = 0;
                if (vpcsecgroup->iag_priv) {
                    hexips = vpcsecgroup->iag_priv->hexips;
                    max_hexips = vpcsecgroup->iag_priv->max_ips;
                    if (max_hexips > 0) {
                        vpcsecgroup->gnipresent_privips = EUCA_REALLOC_C(vpcsecgroup->gnipresent_privips, max_hexips, sizeof (int));
                        bzero (vpcsecgroup->gnipresent_privips, max_hexips * sizeof (int));
                    }
                }
                for (k = 0; k < max_hexips && !found; k++) {
                    if (hexips[k] == 0) {
                        continue;
                    }
                    if (hexips[k] == gniinstance->privateIp) {
                        vpcsecgroup->gnipresent_privips[k] = 1;
                        found = 1;
                        LOGEXTREME("pass1: \tglobal SG privip %x in mido: Y\n", gniinstance->privateIp);
                    }
                }
                if (!found) {
                    LOGEXTREME("pass1: \tglobal SG privip %x in mido: N\n", gniinstance->privateIp);
                }

                hexips = NULL;
                max_hexips = 0;
                if (vpcsecgroup->iag_all) {
                    hexips = vpcsecgroup->iag_all->hexips;
                    max_hexips = vpcsecgroup->iag_all->max_ips;
                    if (max_hexips > 0) {
                        vpcsecgroup->gnipresent_allips_pub = EUCA_REALLOC_C(vpcsecgroup->gnipresent_allips_pub, max_hexips, sizeof (int));
                        bzero (vpcsecgroup->gnipresent_allips_pub, max_hexips * sizeof (int));
                        vpcsecgroup->gnipresent_allips_priv = EUCA_REALLOC_C(vpcsecgroup->gnipresent_allips_priv, max_hexips, sizeof (int));
                        bzero (vpcsecgroup->gnipresent_allips_priv, max_hexips * sizeof (int));
                    }
                }
                int found1 = 0;
                int found2 = 0;
                for (k = 0; k < max_hexips && (!found1 || !found2); k++) {
                    if (hexips[k] == 0) {
                        continue;
                    }
                    if (!found1 && (gniinstance->publicIp == 0)) {
                        found1 = 1;
                    }
                    if (!found1 && (hexips[k] == gniinstance->publicIp)) {
                        vpcsecgroup->gnipresent_allips_pub[k] = 1;
                        found1 = 1;
                        LOGEXTREME("pass1: \tglobal SG pubip (all) %x in mido: Y\n", gniinstance->publicIp);
                        continue;
                    }
                    if (!found2 && (hexips[k] == gniinstance->privateIp)) {
                        vpcsecgroup->gnipresent_allips_priv[k] = 1;
                        found2 = 1;
                        LOGEXTREME("pass1: \tglobal SG privip (all) %x in mido: Y\n", gniinstance->privateIp);
                        continue;
                    }
                }
                if (!found1) {
                    LOGEXTREME("pass1: \tglobal SG pubip (all) %x in mido: N\n", gniinstance->publicIp);
                }
                if (!found2) {
                    LOGEXTREME("pass1: \tglobal SG privip %x (all) in mido: N\n", gniinstance->privateIp);
                }
            }
*/
        }
    }

    return (ret);
}

/**
 * Remove mido objects that are not in GNI. mido models should have been tagged in
 * pass1.
 * @param gni [in] Global Network Information to be applied.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. 1 otherwise.
 */
int do_midonet_update_pass2(globalNetworkInfo *gni, mido_config *mido) {
    int i = 0, j = 0, k = 0, rc = 0, ret = 0;

    mido_vpc *vpc = NULL;
    mido_vpc_secgroup *vpcsecgroup = NULL;
    mido_vpc_natgateway *vpcnatgateway = NULL;
    mido_vpc_instance *vpcinstance = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    //mido_resource_ipaddrgroup *ipag = NULL;

    // pass2 - remove anything in MIDO that is not in GNI
    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        vpcsecgroup = &(mido->vpcsecgroups[i]);
        if (strlen(vpcsecgroup->name) == 0) {
            continue;
        }
        LOGEXTREME("processing %s\n", vpcsecgroup->name);
        if (!vpcsecgroup->gnipresent) {
            LOGINFO("\tdeleting %s\n", vpcsecgroup->name);
            rc = delete_mido_vpc_secgroup(mido, vpcsecgroup);
        } else {
            LOGTRACE("pass2: mido VPC SECGROUP %s in global: Y\n", vpcsecgroup->name);

            // remove any IPs from mido SG that are not in GNI SG instance list
            gni_secgroup *gnisecgroup = NULL;
            int found = 0;
            gni_instance **sginterfaces = NULL;
            int max_sginterfaces = 0;
            midonet_api_ipaddrgroup *iag = NULL;
            u32 *hexips = NULL;
            int max_hexips = 0;

            gnisecgroup = vpcsecgroup->gniSecgroup;
            sginterfaces = gnisecgroup->interfaces;
            max_sginterfaces = gnisecgroup->max_interfaces;

            // Tag IP that is already in mido
            vpcsecgroup->midopresent_pubips = EUCA_REALLOC_C(vpcsecgroup->midopresent_pubips,
                    max_sginterfaces, sizeof (int));
            bzero(vpcsecgroup->midopresent_pubips, max_sginterfaces * sizeof (int));
            vpcsecgroup->midopresent_privips = EUCA_REALLOC_C(vpcsecgroup->midopresent_privips,
                    max_sginterfaces, sizeof (int));
            bzero(vpcsecgroup->midopresent_privips, max_sginterfaces * sizeof (int));
            vpcsecgroup->midopresent_allips_pub = EUCA_REALLOC_C(vpcsecgroup->midopresent_allips_pub,
                    max_sginterfaces, sizeof (int));
            bzero(vpcsecgroup->midopresent_allips_pub, max_sginterfaces * sizeof (int));
            vpcsecgroup->midopresent_allips_priv = EUCA_REALLOC_C(vpcsecgroup->midopresent_allips_priv,
                    max_sginterfaces, sizeof (int));
            bzero(vpcsecgroup->midopresent_allips_priv, max_sginterfaces * sizeof (int));

            hexips = NULL;
            max_hexips = 0;
            iag = vpcsecgroup->iag_pub;
            if (iag) {
                hexips = iag->hexips;
                max_hexips = iag->max_ips;
            }
            for (k = 0; k < max_hexips; k++) {
                if (hexips[k] == 0) {
                    continue;
                }
                found = 0;
                for (j = 0; j < max_sginterfaces && !found; j++) {
                    if (sginterfaces[j]->publicIp == hexips[k]) {
                        LOGEXTREME("pass2: mido VPC SECGROUP %s member public IP %s in global: Y\n", vpcsecgroup->name, iag->ips[k]->name);
                        found++;
                        vpcsecgroup->midopresent_pubips[j] = 1;
                    }
                }
                if (!found) {
                    LOGTRACE("\tdeleting %s member pubip %s\n", vpcsecgroup->name, iag->ips[k]->name);
                    rc = mido_delete_ipaddrgroup_ip(iag, iag->ips[k]);
                    if (rc) {
                        LOGWARN("Failed to remove %s from ip address group\n", iag->ips[k]->name);
                    }
                    if (iag->max_ips == 0) {
                        break;
                    }
                }
            }

            hexips = NULL;
            max_hexips = 0;
            iag = vpcsecgroup->iag_priv;
            if (iag) {
                hexips = iag->hexips;
                max_hexips = iag->max_ips;
            }
            for (k = 0; k < max_hexips; k++) {
                if (hexips[k] == 0) {
                    continue;
                }
                found = 0;
                for (j = 0; j < max_sginterfaces && !found; j++) {
                    if (sginterfaces[j]->privateIp == hexips[k]) {
                        LOGEXTREME("pass2: mido VPC SECGROUP %s member private IP %s in global: Y\n", vpcsecgroup->name, iag->ips[k]->name);
                        found++;
                        vpcsecgroup->midopresent_privips[j] = 1;
                    }
                }
                if (!found) {
                    LOGTRACE("\tdeleting %s member privip %s\n", vpcsecgroup->name, iag->ips[k]->name);
                    rc = mido_delete_ipaddrgroup_ip(iag, iag->ips[k]);
                    if (rc) {
                        LOGWARN("Failed to remove %s from ip address group\n", iag->ips[k]->name);
                    }
                    if (iag->max_ips == 0) {
                        break;
                    }
                }
            }

            hexips = NULL;
            max_hexips = 0;
            iag = vpcsecgroup->iag_all;
            if (iag) {
                hexips = iag->hexips;
                max_hexips = iag->max_ips;
            }
            for (k = 0; k < max_hexips; k++) {
                if (hexips[k] == 0) {
                    continue;
                }
                found = 0;
                for (j = 0; j < max_sginterfaces && !found; j++) {
                    if (sginterfaces[j]->publicIp == hexips[k]) {
                        LOGEXTREME("pass2: mido VPC SECGROUP %s member all IP %s in global: Y\n", vpcsecgroup->name, iag->ips[k]->name);
                        found++;
                        vpcsecgroup->midopresent_allips_pub[j] = 1;
                    }
                    if (sginterfaces[j]->privateIp == hexips[k]) {
                        LOGEXTREME("pass2: mido VPC SECGROUP %s member all IP %s in global: Y\n", vpcsecgroup->name, iag->ips[k]->name);
                        found++;
                        vpcsecgroup->midopresent_allips_priv[j] = 1;
                    }
                }
                if (!found) {
                    LOGTRACE("\tdeleting %s member allip %s\n", vpcsecgroup->name, iag->ips[k]->name);
                    rc = mido_delete_ipaddrgroup_ip(iag, iag->ips[k]);
                    if (rc) {
                        LOGWARN("Failed to remove %s from ip address group\n", iag->ips[k]->name);
                    }
                    if (iag->max_ips == 0) {
                        break;
                    }
                }
            }
        }
    }

    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        if (strlen(vpc->name) == 0) {
            continue;
        }
        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            if (strlen(vpcsubnet->name) == 0) {
                continue;
            }
            for (k = 0; k < vpcsubnet->max_natgateways; k++) {
                vpcnatgateway = &(vpcsubnet->natgateways[k]);
                if (strlen(vpcnatgateway->name) == 0) {
                    continue;
                }
                if ((!vpcnatgateway->midos[NATG_RT]) || (vpcnatgateway->midos[NATG_RT]->init == 0)) {
                    continue;
                }
                if (!vpc->gnipresent || !vpcsubnet->gnipresent || !vpcnatgateway->gnipresent) {
                    LOGINFO("\tdeleting %s\n", vpcnatgateway->name);
                    rc = delete_mido_vpc_natgateway(mido, vpcsubnet, vpcnatgateway);
                } else {
                    LOGTRACE("pass2: mido VPC NAT gateway %s in global: Y\n", vpcnatgateway->name);
                }
            }
            for (k = 0; k < vpcsubnet->max_instances; k++) {
                vpcinstance = &(vpcsubnet->instances[k]);
                if (strlen(vpcinstance->name) == 0) {
                    continue;
                }
                if (!vpc->gnipresent || !vpcsubnet->gnipresent || !vpcinstance->gnipresent) {
                    LOGINFO("\tdeleting %s\n", vpcinstance->name);
                    rc = delete_mido_vpc_instance(mido, vpc, vpcsubnet, vpcinstance);
                } else {
                    LOGTRACE("pass2: mido VPC INSTANCE %s in global: Y\n", vpcinstance->name);
                }
            }
            if (!vpc->gnipresent || !vpcsubnet->gnipresent) {
                LOGINFO("\tdeleting %s\n", vpcsubnet->name);
                rc = delete_mido_vpc_subnet(mido, vpc, vpcsubnet);
            } else {
                LOGTRACE("pass2: mido VPC SUBNET %s in global: Y\n", vpcsubnet->name);
            }
        }
        if (!vpc->gnipresent) {
            LOGINFO("\tdeleting %s\n", vpc->name);
            rc = do_metaproxy_teardown(mido);
            if (rc) {
                LOGERROR("cannot teardown metadata proxies: see above log for details\n");
                ret = 1;
            }
            rc = delete_mido_vpc(mido, vpc);
        } else {
            LOGTRACE("pass2: mido VPC %s in global: Y\n", vpc->name);
        }
    }

    return (ret);
}


/**
 * Implements VPCs (create mido objects) as described in GNI. VPC subnets and
 * NAT gateways are also processed.
 * @param gni [in] Global Network Information to be applied.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. 1 otherwise.
 */
int do_midonet_update_pass3_vpcs(globalNetworkInfo *gni, mido_config *mido) {
    int i = 0, j = 0, rc = 0, ret = 0;

    char subnet_buf[24], slashnet_buf[8], gw_buf[24];
 
    mido_vpc *vpc = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc_natgateway *vpcnatg = NULL;
 
    gni_route_table *gni_rtable = NULL;

    gni_vpc *gnivpc = NULL;
    gni_vpcsubnet *gnivpcsubnet = NULL;
    gni_nat_gateway *gninatg = NULL;

    // now, go through GNI and create new VPCs
    LOGDEBUG("initializing VPCs (%d)\n", gni->max_vpcs);
    for (i = 0; i < gni->max_vpcs; i++) {
        gnivpc = NULL;
        gnivpcsubnet = NULL;
        vpc = NULL;
        vpcsubnet = NULL;

        gnivpc = &(gni->vpcs[i]);

        vpc = (mido_vpc *) gnivpc->mido_present;
        if (vpc) {
            LOGTRACE("found gni VPC '%s' already extant\n", gnivpc->name);
        } else {
            LOGINFO("\tcreating %s\n", gnivpc->name);
            // necessary memory should have been allocated in pass1
            vpc = &(mido->vpcs[mido->max_vpcs]);
            bzero(vpc, sizeof (mido_vpc));
            mido->max_vpcs++;
            snprintf(vpc->name, 16, "%s", gnivpc->name);
            vpc->gniVpc = gnivpc;
            gnivpc->mido_present = vpc;
            get_next_router_id(mido, &(vpc->rtid));
            // allocate space for subnets and natgateways
            if (gnivpc->max_subnets > 0) {
                vpc->subnets = EUCA_ZALLOC_C(gnivpc->max_subnets, sizeof (mido_vpc_subnet));
            }
        }

        if (vpc->midopresent) {
            // VPC presence test passed in pass1
            LOGTRACE("12095: skipping pass3 for %s\n", gnivpc->name);
        } else {
            rc = create_mido_vpc(mido, mido->midocore, vpc);
            if (rc) {
                LOGERROR("failed to create VPC %s: check midonet health\n", gnivpc->name);
                rc = delete_mido_vpc(mido, vpc);
                LOGERROR("failed to cleanup VPC %s\n", gnivpc->name);
                ret++;
                continue;
            }
        }
        vpc->gnipresent = 1;

        // do subnets
        for (j = 0; j < gnivpc->max_subnets; j++) {
            gnivpcsubnet = &(gnivpc->subnets[j]);

            vpcsubnet = (mido_vpc_subnet *) gnivpcsubnet->mido_present;
            if (vpcsubnet) {
                LOGTRACE("found gni VPC %s subnet %s\n", vpc->name, vpcsubnet->name);
            } else {
                LOGINFO("\tcreating %s\n", gnivpc->subnets[j].name);
                // necessary memory should have been allocated in pass1
                vpcsubnet = &(vpc->subnets[vpc->max_subnets]);
                vpc->max_subnets++;
                bzero(vpcsubnet, sizeof (mido_vpc_subnet));
                snprintf(vpcsubnet->name, 16, "%s", gnivpc->subnets[j].name);
                snprintf(vpcsubnet->vpcname, 16, "%s", vpc->name);
                vpcsubnet->gniSubnet = gnivpcsubnet;
                gnivpcsubnet->mido_present = vpcsubnet;
                // Allocate space for interfaces
                vpcsubnet->instances = EUCA_ZALLOC_C(gnivpcsubnet->max_interfaces, sizeof (mido_vpc_instance));
                if (gnivpc->max_natGateways > 0) {
                    vpcsubnet->natgateways = EUCA_ZALLOC_C(gnivpc->max_natGateways, sizeof (mido_vpc_natgateway));
                }
            }

            subnet_buf[0] = slashnet_buf[0] = gw_buf[0] = '\0';
            cidr_split(gnivpcsubnet->cidr, subnet_buf, slashnet_buf, gw_buf, NULL);

            if (vpcsubnet->midopresent) {
                // VPC subnet presence test passed in pass1
                LOGTRACE("12095: skipping pass3 for %s\n", gnivpcsubnet->name);
                rc = 0;
            } else {
                rc = create_mido_vpc_subnet(mido, vpc, vpcsubnet, subnet_buf, slashnet_buf,
                        gw_buf, gni->instanceDNSDomain, gni->instanceDNSServers, gni->max_instanceDNSServers);
            }
            if (rc) {
                LOGERROR("failed to create VPC %s subnet %s: check midonet health\n", gnivpc->name, gnivpcsubnet->name);
                ret = 1;
                rc = delete_mido_vpc_subnet(mido, vpc, vpcsubnet);
                if (rc) {
                    LOGERROR("Failed to delete subnet %s. Check for duplicate midonet objects.\n", vpcsubnet->name);
                }
                continue;
            } else {
                // Implement subnet routing table routes
                gni_rtable = gnivpcsubnet->routeTable;
                if (gni_rtable != NULL) {
                    if (gni_rtable->changed != 0) {
                        // populate vpcsubnet routes
                        rc = find_mido_vpc_subnet_routes(mido, vpc, vpcsubnet);
                        if (rc != 0) {
                            LOGWARN("VPC subnet population failed to populate route table.\n");
                        }
                        rc = create_mido_vpc_subnet_route_table(mido, vpc, vpcsubnet,
                                subnet_buf, slashnet_buf, gni_rtable, gnivpc);
                        if (rc) {
                            LOGWARN("Failed to create %s for %s\n", gnivpcsubnet->routeTable_name, gnivpcsubnet->name);
                            ret = 1;
                        }
                    } else {
                        LOGTRACE("12095: skipping pass3 for %s\n", gni_rtable->name);
                    }
                } else {
                    LOGWARN("route table for %s not found.\n", gnivpcsubnet->name);
                }
            }
            vpcsubnet->gnipresent = 1;
            // Update references to vpc and subnet for each interface
            for (int k = 0; k < gnivpcsubnet->max_interfaces; k++) {
                gni_instance *gniif = gnivpcsubnet->interfaces[k];
                gniif->mido_vpc = vpc;
                gniif->mido_vpcsubnet = vpcsubnet;
            }
        }

        // do NAT gateways
        for (j = 0; j < gnivpc->max_natGateways; j++) {
            gninatg = &(gnivpc->natGateways[j]);
            vpcnatg = (mido_vpc_natgateway *) gninatg->mido_present;
            if (vpcnatg) {
                LOGTRACE("found %s in mido\n", vpcnatg->name);
                if (vpcnatg->midopresent) {
                    // VPC nat gateway presence test passed in pass1
                    LOGTRACE("12095: skipping pass3 for %s\n", gninatg->name);
                    continue;
                }
            } else {
                LOGINFO("\tcreating %s\n", gnivpc->natGateways[j].name);
                // get the subnet
                find_mido_vpc_subnet(vpc, gninatg->subnet, &vpcsubnet);
                if (vpcsubnet == NULL) {
                    LOGERROR("Unable to find %s for %s - aborting NAT Gateway creation\n", gninatg->subnet, gninatg->name);
                    continue;
                }
                // necessary memory should have been allocated in pass1
                vpcnatg = &(vpcsubnet->natgateways[vpc->max_natgateways]);
                (vpcsubnet->max_natgateways)++;
                bzero(vpcnatg, sizeof (mido_vpc_natgateway));
                snprintf(vpcnatg->name, 32, "%s", gnivpc->natGateways[j].name);
                vpcnatg->gniNatGateway = gninatg;
                get_next_router_id(mido, &(vpcnatg->rtid));
                vpcnatg->gniVpcSubnet = gni_vpc_get_vpcsubnet(gnivpc, gninatg->subnet);
                if (vpcnatg->gniVpcSubnet == NULL) {
                    LOGERROR("Unable to find %s for %s - aborting NAT Gateway creation\n", gninatg->subnet, gninatg->name);
                    continue;
                }
                rc = create_mido_vpc_natgateway(mido, vpc, vpcsubnet, vpcnatg);
                if (rc) {
                    LOGERROR("failed to create %s: check midonet health\n", vpcnatg->name);
                }
            }
        }    
    }

    // set up metadata proxies once vpcs/subnets are all set up
    rc = do_metaproxy_setup(mido);
    if (rc) {
        LOGERROR("cannot set up metadata proxies: see above log for details\n");
        ret = 1;
    }

    return (ret);
}

/**
 * Implements security groups (create mido objects) as described in GNI.
 * @param gni [in] Global Network Information to be applied.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. 1 otherwise.
 */
int do_midonet_update_pass3_sgs(globalNetworkInfo *gni, mido_config *mido) {
    int rc = 0, ret = 0, i = 0, j = 0;

    mido_vpc_secgroup *vpcsecgroup = NULL;
    gni_secgroup *gnisecgroup = NULL;

    // Process security groups
    for (i = 0; i < gni->max_secgroups; i++) {
        gnisecgroup = &(gni->secgroups[i]);
        vpcsecgroup = (mido_vpc_secgroup *) gnisecgroup->mido_present;
        if (vpcsecgroup != NULL) {
            LOGTRACE("found gni SG %s already extant\n", gnisecgroup->name);
        } else {
            LOGINFO("\tcreating %s\n", gnisecgroup->name);
            // necessary memory should have been allocated in pass1
            vpcsecgroup = &(mido->vpcsecgroups[mido->max_vpcsecgroups]);
            bzero(vpcsecgroup, sizeof (mido_vpc_secgroup));
            mido->max_vpcsecgroups++;
            snprintf(vpcsecgroup->name, SECURITY_GROUP_ID_LEN, "%s", gnisecgroup->name);
            vpcsecgroup->gniSecgroup = gnisecgroup;
            gnisecgroup->mido_present = vpcsecgroup;
            vpcsecgroup->midopresent_pubips = EUCA_ZALLOC_C(gnisecgroup->max_interfaces, sizeof (int));
            vpcsecgroup->midopresent_privips = EUCA_ZALLOC_C(gnisecgroup->max_interfaces, sizeof (int));
            vpcsecgroup->midopresent_allips_pub = EUCA_ZALLOC_C(gnisecgroup->max_interfaces, sizeof (int));
            vpcsecgroup->midopresent_allips_priv = EUCA_ZALLOC_C(gnisecgroup->max_interfaces, sizeof (int));
            vpcsecgroup->egress_changed = 1;
            vpcsecgroup->ingress_changed = 1;
            vpcsecgroup->interfaces_changed = 1;
        }

        if (vpcsecgroup->midopresent) {
            // SG presence test passed in pass1
            LOGTRACE("12095: %s found in mido\n", gnisecgroup->name);
        } else {
            rc = create_mido_vpc_secgroup(mido, vpcsecgroup);
            if (rc) {
                LOGERROR("cannot create mido security group %s: check midonet health\n", vpcsecgroup->name);
                ret = 1;
                continue;
            }
        }
        vpcsecgroup->gnipresent = 1;
        LOGTRACE("12095:\t%s ingress %d egress %d intf %d\n", vpcsecgroup->name, vpcsecgroup->ingress_changed, vpcsecgroup->egress_changed, vpcsecgroup->interfaces_changed);

        mido_parsed_chain_rule sgrule;

        // Process egress rules
        if (!vpcsecgroup->population_failed && !vpcsecgroup->egress_changed) {
            LOGTRACE("12095: skipping pass3 for %s egress\n", gnisecgroup->name);
        } else {
            // clear egress rules
            rc = mido_clear_rules(vpcsecgroup->egress);
            for (j = 0; j < gnisecgroup->max_egress_rules; j++) {
                rc = parse_mido_secgroup_rule(mido, &(gnisecgroup->egress_rules[j]), &sgrule);
                if (rc == 0) {
                    rc = create_mido_vpc_secgroup_rule(vpcsecgroup->egress, NULL, -1,
                            MIDO_RULE_SG_EGRESS, &sgrule);
                    if (rc) {
                        LOGWARN("failed to create %s egress rule at idx %d\n", gnisecgroup->name, j);
                        ret++;
                    }
                } else {
                    LOGWARN("failed to parse %s egress rule at idx %d\n", gnisecgroup->name, j);
                    ret++;
                }
            }
        }
        
        // Process ingress rules
        if (!vpcsecgroup->population_failed && !vpcsecgroup->ingress_changed) {
            LOGTRACE("12095: skipping pass3 for %s ingress\n", gnisecgroup->name);
        } else {
            // clear ingress rules
            rc = mido_clear_rules(vpcsecgroup->ingress);
            for (j = 0; j < gnisecgroup->max_ingress_rules; j++) {
                rc = parse_mido_secgroup_rule(mido, &(gnisecgroup->ingress_rules[j]), &sgrule);
                if (rc == 0) {
                    rc = create_mido_vpc_secgroup_rule(vpcsecgroup->ingress, NULL, -1,
                            MIDO_RULE_SG_INGRESS, &sgrule);
                    if (rc) {
                        LOGWARN("failed to create %s ingress rule at idx %d\n", gnisecgroup->name, j);
                        ret++;
                    }
                } else {
                    LOGWARN("failed to parse %s ingress rule at idx %d\n", gnisecgroup->name, j);
                    ret++;
                }
            }
        }

        // Process SG member IP addresses
        for (j = 0; j < gnisecgroup->max_interfaces; j++) {
            char *pubipstr = NULL;
            char *privipstr = NULL;
            gni_instance *gniif = gnisecgroup->interfaces[j];
            pubipstr = hex2dot(gniif->publicIp);
            privipstr = hex2dot(gniif->privateIp);
            if (vpcsecgroup->midopresent_pubips[j] == 1) {
                LOGTRACE("12095:\t%s already in mido %s\n", pubipstr, gnisecgroup->name);
            } else {
                if (gniif->publicIp != 0) {
                    rc = mido_create_ipaddrgroup_ip(vpcsecgroup->iag_pub, NULL, pubipstr, NULL);
                    if (rc) {
                        LOGWARN("failed to add %s to %s\n", pubipstr, vpcsecgroup->midos[VPCSG_IAGPUB]->name);
                        ret++;
                    }
                }
            }
            if (vpcsecgroup->midopresent_privips[j] == 1) {
                LOGTRACE("12095:\t%s already in mido %s\n", privipstr, gnisecgroup->name);
            } else {
                rc = mido_create_ipaddrgroup_ip(vpcsecgroup->iag_priv, NULL, privipstr, NULL);
                if (rc) {
                    LOGWARN("failed to add %s to %s\n", privipstr, vpcsecgroup->midos[VPCSG_IAGPRIV]->name);
                    ret++;
                }
            }
            if (vpcsecgroup->midopresent_allips_pub[j] == 1) {
                LOGTRACE("12095:\t%s already in mido %s\n", pubipstr, gnisecgroup->name);
            } else {
                if (gniif->publicIp != 0) {
                    rc = mido_create_ipaddrgroup_ip(vpcsecgroup->iag_all, NULL, pubipstr, NULL);
                    if (rc) {
                        LOGWARN("failed to add %s to %s\n", pubipstr, vpcsecgroup->midos[VPCSG_IAGALL]->name);
                        ret++;
                    }
                }
            }
            if (vpcsecgroup->midopresent_allips_priv[j] == 1) {
                LOGTRACE("12095:\t%s already in mido %s\n", privipstr, gnisecgroup->name);
            } else {
                rc = mido_create_ipaddrgroup_ip(vpcsecgroup->iag_all, NULL, privipstr, NULL);
                if (rc) {
                    LOGWARN("failed to add %s to %s\n", privipstr, vpcsecgroup->midos[VPCSG_IAGALL]->name);
                    ret++;
                }
            }
            EUCA_FREE(pubipstr);
            EUCA_FREE(privipstr);
        }
    }

    return (ret);
}

/**
 * Implements instances/interfaces (create mido objects) as described in GNI.
 * @param gni [in] Global Network Information to be applied.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. 1 otherwise.
 */
int do_midonet_update_pass3_insts(globalNetworkInfo *gni, mido_config *mido) {
    int rc = 0, ret = 0, i = 0, j = 0, k = 0;
    char subnet_buf[24], slashnet_buf[8], gw_buf[24], pt_buf[24];

    mido_vpc_secgroup *vpcsecgroup = NULL;
    mido_vpc_instance *vpcif = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc *vpc = NULL;
    
    gni_instance *gniif = NULL;
    midonet_api_host *gni_instance_node = NULL;

    midoname **jprules_egress = NULL;
    char **jprules_tgt_egress = NULL;
    int max_jprules_egress = 0;
    int *jpe_gni_present;
    midoname **jprules_ingress = NULL;
    char **jprules_tgt_ingress = NULL;
    int max_jprules_ingress = 0;
    int *jpi_gni_present;
    int found = 0;

    struct timeval tv;

    // Process instances/interfaces
    for (i = 0; i < gni->max_interfaces; i++) {
        eucanetd_timer_usec(&tv);
        gniif = &(gni->interfaces[i]);
        if (strlen(gniif->name) == 0) {
            LOGWARN("Empty interface detected in GNI.\n");
            ret++;
            continue;
        }
        vpc = (mido_vpc *) gniif->mido_vpc;
        vpcsubnet = (mido_vpc_subnet *) gniif->mido_vpcsubnet;
        vpcif = (mido_vpc_instance *) gniif->mido_present;
        if (!vpc || !vpcsubnet) {
            LOGWARN("Unable to find %s and/or %s\n", gniif->vpc, gniif->subnet);
            ret++;
            continue;
        }

        if (vpcif) {
            LOGTRACE("found instance %s in vpc %s subnet %s\n", vpcif->name, vpc->name, vpcsubnet->name);
            vpcif->gniInst = gniif;
        } else {
            // create the instance model
            // necessary memory should have been allocated in pass1
            vpcif = &(vpcsubnet->instances[vpcsubnet->max_instances]);
            bzero(vpcif, sizeof (mido_vpc_instance));
            vpcsubnet->max_instances++;
            snprintf(vpcif->name, INTERFACE_ID_LEN, "%s", gniif->name);
            vpcif->gniInst = gniif;
            gniif->mido_present = vpcif;
            vpcif->host_changed = 1;
            vpcif->srcdst_changed = 1;
            vpcif->pubip_changed = 1;
            vpcif->sg_changed = 1;
            LOGINFO("\tcreating %s\n", gniif->name);
        }

        if (vpcif->midopresent) {
            LOGTRACE("12095: skipping pass3 for %s\n", gniif->name);
            continue;
        } else {
            rc = create_mido_vpc_instance(vpcif);
            if (rc) {
                LOGERROR("failed to create VPC instance %s: check midonet health\n", gniif->name);
                ret++;
                continue;
            }
        }
        vpcif->gnipresent = 1;

        // check for potential VMHOST change
        if (!vpcif->population_failed && !vpcif->host_changed) {
            LOGTRACE("12095:\t%s host did not change\n", gniif->name);
        } else {
            gni_instance_node = mido_get_host_byip(gniif->node);
            if (vpcif->midos[INST_VMHOST] && vpcif->midos[INST_VMHOST]->init) {
                if (gni_instance_node) {
                    if ((gni_instance_node->obj == vpcif->midos[INST_VMHOST]) ||
                            (!strcmp(gni_instance_node->obj->uuid, vpcif->midos[INST_VMHOST]->uuid))) {
                        LOGTRACE("12095:\t%s host did not change.\n", gniif->name);
                        vpcif->host_changed = 0;
                    } else {
                        LOGINFO("\t%s vmhost change detected.\n", gniif->name);
                        disconnect_mido_vpc_instance(vpcsubnet, vpcif);
                    }
                } else {
                    LOGERROR("\t%s host %s not found: check midonet and/or midolman health\n", gniif->name, gniif->node);
                    continue;
                }
            }
            vpcif->midos[INST_VMHOST] = gni_instance_node->obj;
        }

        // do instance/interface-host connection
        if (vpcif->host_changed) {
            LOGTRACE("\tconnecting mido host %s with interface %s\n",
                    vpcif->midos[INST_VMHOST]->name, gniif->name);
            rc = connect_mido_vpc_instance(vpcsubnet, vpcif, gni->instanceDNSDomain);
            if (rc) {
                LOGERROR("failed to connect %s to %s: check midolman\n", gniif->name, vpcif->midos[INST_VMHOST]->name);
            }
        }

        // check public/elastic IP changes
        if (!vpcif->population_failed && !vpcif->pubip_changed) {
            LOGTRACE("12095:\t%s pubip did not change\n", gniif->name);
        } else {
            if (gniif->publicIp == vpcif->pubip) {
                LOGTRACE("12095:\t%s pubip did not change.\n", gniif->name);
                vpcif->pubip_changed = 0;
            } else {
                if (vpcif->population_failed || (vpcif->pubip != 0)) {
                    // disconnect public/elastic IP
                    rc = disconnect_mido_vpc_instance_elip(mido, vpc, vpcif);
                    if (rc) {
                        LOGERROR("failed to disconnect %s elip\n", gniif->name);
                        ret++;
                    } else {
                        vpcif->pubip = 0;
                    }
                } 
            }
        }

        // do instance/interface public/elastic IP connection
        if (vpcif->population_failed || vpcif->pubip_changed) {
            // Do not run connect for private interfaces
            if (gniif->publicIp != 0) {
                rc = connect_mido_vpc_instance_elip(mido, vpc, vpcsubnet, vpcif);
                if (rc) {
                    LOGERROR("failed to setup public/elastic IP for %s\n", gniif->name);
                    ret++;
                }
            }
        }
            
        char pos_str[32];
        char *instMac = NULL;
        char *instIp = NULL;
        int rulepos = 0;

        midoname *ptmpmn;

        subnet_buf[0] = '\0'; 
        slashnet_buf[0] = '\0';
        gw_buf[0] = '\0';
        cidr_split(vpcsubnet->gniSubnet->cidr, subnet_buf, slashnet_buf, gw_buf, pt_buf);

        hex2mac(gniif->macAddress, &instMac);
        instIp = hex2dot(gniif->privateIp);
        for (int i = 0; i < strlen(instMac); i++) {
            instMac[i] = tolower(instMac[i]);
        }

        // anti-spoof
        // block any source mac that isn't the registered instance mac
        rulepos = 1;
        snprintf(pos_str, 32, "%d", rulepos);
        // Check if the rule is already in place
        rc = mido_find_rule_from_list(vpcif->prechain->rules, vpcif->prechain->max_rules, &ptmpmn,
                "type", "drop", "dlSrc", instMac, "invDlSrc", "true", NULL);

        if ((rc == 0) && ptmpmn && (ptmpmn->init == 1)) {
            if (mido->disable_l2_isolation) {
                LOGTRACE("\tdeleting L2 rule for %s\n", gniif->name);
                rc = mido_delete_rule(vpcif->prechain, ptmpmn);
                if (rc) {
                    LOGWARN("Failed to delete src mac check rule for %s\n", gniif->name);
                    ret++;
                }
            }
        } else {
            if (!mido->disable_l2_isolation) {
                LOGTRACE("\tcreating L2 rule for %s\n", gniif->name);
                rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                        NULL, &rulepos, "position", pos_str, "type", "drop", "dlSrc", instMac,
                        "invDlSrc", "true", NULL);
                if (rc) {
                    LOGWARN("Failed to create src mac check rule for %s\n", gniif->name);
                    ret++;
                }
            }
        }

        // block any outgoing IP traffic that isn't from the VM private IP
        // Check if the rule is already in place
        rc = mido_find_rule_from_list(vpcif->prechain->rules, vpcif->prechain->max_rules, &ptmpmn,
                "type", "drop", "dlType", "2048", "nwSrcAddress", instIp, "nwSrcLength", "32", "invNwSrc", "true", NULL);
        if ((rc == 0) && ptmpmn && (ptmpmn->init == 1)) {
            if ((mido->disable_l2_isolation) || (!gniif->srcdstcheck)) {
                LOGTRACE("\tdeleting L3 rule for %s\n", gniif->name);
                rc = mido_delete_rule(vpcif->prechain, ptmpmn);
                if (rc) {
                    LOGWARN("Failed to delete src IP check rule for %s\n", gniif->name);
                    ret++;
                }
            }
        } else {
            if ((!mido->disable_l2_isolation) && (gniif->srcdstcheck)) {
                LOGTRACE("\tcreating L3 rule for %s\n", gniif->name);
                rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                        NULL, &rulepos, "position", pos_str, "type", "drop", "dlType", "2048",
                        "nwSrcAddress", instIp, "nwSrcLength", "32", "invNwSrc", "true", NULL);
                if (rc) {
                    LOGWARN("Failed to create src IP check rule for %s\n", gniif->name);
                    ret++;
                }
            }
        }

        // anti arp poisoning
        // block any outgoing ARP that does not have sender hardware address set to the registered MAC
        // Check if the rule is already in place
        rc = mido_find_rule_from_list(vpcif->prechain->rules, vpcif->prechain->max_rules, &ptmpmn,
                "type", "drop", "dlSrc", instMac, "invDlSrc", "true",
                "dlType", "2054", "invDlType", "false", NULL);
        if ((rc == 0) && ptmpmn && (ptmpmn->init == 1)) {
            if (mido->disable_l2_isolation) {
                LOGTRACE("\tdeleting ARP_SHA rule for %s\n", gniif->name);
                rc = mido_delete_rule(vpcif->prechain, ptmpmn);
                if (rc) {
                    LOGWARN("Failed to delete ARP_SHA rule for %s\n", gniif->name);
                    ret++;
                }
            }
        } else {
            if (!mido->disable_l2_isolation) {
                LOGTRACE("\tcreating ARP_SHA rule for %s\n", gniif->name);
                rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                        NULL, &rulepos, "position", pos_str, "type", "drop", "dlSrc", instMac,
                        "invDlSrc", "true", "dlType", "2054", "invDlType", "false", NULL);
                if (rc) {
                    LOGWARN("Failed to create ARP_SHA rule for %s\n", gniif->name);
                    ret++;
                }
            }
        }

        // block any outgoing ARP that does not have sender protocol address set to the VM private IP
        // Check if the rule is already in place
        rc = mido_find_rule_from_list(vpcif->prechain->rules, vpcif->prechain->max_rules, &ptmpmn,
                "type", "drop", "dlType", "2054", "nwSrcAddress",
                instIp, "nwSrcLength", "32", "invNwSrc", "true",
                "invDlType", "false", NULL);
        if ((rc == 0) && ptmpmn && (ptmpmn->init == 1)) {
            if (mido->disable_l2_isolation) {
                LOGTRACE("\tdeleting ARP_SPA rule for %s\n", gniif->name);
                rc = mido_delete_rule(vpcif->prechain, ptmpmn);
                if (rc) {
                    LOGWARN("Failed to delete ARP_SPA rule for %s\n", gniif->name);
                    ret++;
                }
            }
        } else {
            if (!mido->disable_l2_isolation) {
                LOGTRACE("\tcreating ARP_SPA rule for %s\n", gniif->name);
                rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                        NULL, &rulepos, "position", pos_str, "type", "drop", "dlType", "2054",
                        "nwSrcAddress", instIp, "nwSrcLength", "32", "invNwSrc", "true",
                        "invDlType", "false", NULL);
                if (rc) {
                    LOGWARN("Failed to create src IP check rule for %s\n", gniif->name);
                    ret++;
                }
            }
        }

        // block any incoming ARP replies that does not have target hardware address set to the registered MAC
        // Check if the rule is already in place
        rc = mido_find_rule_from_list(vpcif->postchain->rules, vpcif->postchain->max_rules, &ptmpmn,
                "type", "drop", "dlDst", instMac, "invDlDst", "true",
                "dlType", "2054", "invDlType", "false", "nwProto", "2",
                "invNwProto", "false", NULL);
        if ((rc == 0) && ptmpmn && (ptmpmn->init == 1)) {
            if (mido->disable_l2_isolation) {
                LOGTRACE("\tdeleting ARP_THA rule for %s\n", gniif->name);
                rc = mido_delete_rule(vpcif->postchain, ptmpmn);
                if (rc) {
                    LOGWARN("Failed to delete ARP_THA rule for %s\n", gniif->name);
                    ret++;
                }
            }
        } else {
            if (!mido->disable_l2_isolation) {
                LOGTRACE("\tcreating ARP_SHA rule for %s\n", gniif->name);
                rc = mido_create_rule(vpcif->postchain, vpcif->midos[INST_POSTCHAIN],
                        NULL, &rulepos, "position", pos_str, "type", "drop", "dlDst", instMac,
                        "invDlDst", "true", "dlType", "2054", "invDlType", "false", "nwProto", "2",
                        "invNwProto", "false", NULL);
                if (rc) {
                    LOGWARN("Failed to create ARP_THA rule for %s\n", gniif->name);
                    ret++;
                }
            }
        }

        // block any incoming ARP that does not have target protocol address set to the VM private IP
        // Check if the rule is already in place
        rc = mido_find_rule_from_list(vpcif->postchain->rules, vpcif->postchain->max_rules, &ptmpmn,
                "type", "drop", "dlType", "2054", "nwDstAddress",
                instIp, "nwDstLength", "32", "invNwDst", "true",
                "invDlType", "false", NULL);
        if ((rc == 0) && ptmpmn && (ptmpmn->init == 1)) {
            if (mido->disable_l2_isolation) {
                LOGTRACE("\tdeleting ARP_TPA rule for %s\n", gniif->name);
                rc = mido_delete_rule(vpcif->postchain, ptmpmn);
                if (rc) {
                    LOGWARN("Failed to delete ARP_TPA rule for %s\n", gniif->name);
                    ret++;
                }
            }
        } else {
            if (!mido->disable_l2_isolation) {
                LOGTRACE("\tcreating ARP_TPA rule for %s\n", gniif->name);
                rc = mido_create_rule(vpcif->postchain, vpcif->midos[INST_POSTCHAIN],
                        NULL, &rulepos, "position", pos_str, "type", "drop", "dlType", "2054",
                        "nwDstAddress", instIp, "nwDstLength", "32", "invNwDst", "true",
                        "invDlType", "false", NULL);
                if (rc) {
                    LOGWARN("Failed to create ARP_TPA rule for %s\n", gniif->name);
                    ret++;
                }
            }
        }

        EUCA_FREE(instMac);
        EUCA_FREE(instIp);

        // metadata
        // metadata redirect egress
        rulepos = vpcif->prechain->rules_count + 1;
        snprintf(pos_str, 32, "%d", rulepos);
        rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                NULL, &rulepos,
                "position", pos_str, "type", "dnat", "flowAction", "continue",
                "ipAddrGroupDst", mido->midocore->midos[CORE_METADATA_IPADDRGROUP]->uuid,
                "nwProto", "6", "tpDst", "jsonjson", "tpDst:start", "80", "tpDst:end", "80",
                "tpDst:END", "END", "natTargets", "jsonlist", "natTargets:addressTo", pt_buf,
                "natTargets:addressFrom", pt_buf, "natTargets:portFrom",
                "31337", "natTargets:portTo", "31337", "natTargets:END", "END", NULL);
        if (rc) {
            LOGWARN("Failed to create MD dnat rule for %s\n", gniif->name);
            ret++;
        }

        // metadata redirect ingress
        rulepos = vpcif->postchain->rules_count + 1;
        snprintf(pos_str, 32, "%d", rulepos);
        rc = mido_create_rule(vpcif->postchain, vpcif->midos[INST_POSTCHAIN],
                NULL, &rulepos,
                "position", pos_str, "type", "snat", "flowAction", "continue",
                "nwSrcAddress", pt_buf, "nwSrcLength", "32", "nwProto", "6",
                "tpSrc", "jsonjson", "tpSrc:start", "31337", "tpSrc:end", "31337", "tpSrc:END", "END",
                "natTargets", "jsonlist", "natTargets:addressTo", "169.254.169.254",
                "natTargets:addressFrom", "169.254.169.254", "natTargets:portFrom", "80",
                "natTargets:portTo", "80", "natTargets:END", "END", NULL);
        if (rc) {
            LOGWARN("Failed to create MD snat rule for %s\n", gniif->name);
            ret++;
        }

        // contrack
        // conntrack egress
        rulepos = vpcif->prechain->rules_count + 1;
        snprintf(pos_str, 32, "%d", rulepos);
        rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                NULL, &rulepos,
                "position", pos_str, "type", "accept", "matchReturnFlow", "true", NULL);
        if (rc) {
            LOGWARN("Failed to create egress conntrack for %s\n", gniif->name);
            ret++;
        }

        // conn track ingress
        rulepos = vpcif->postchain->rules_count + 1;
        snprintf(pos_str, 32, "%d", rulepos);
        rc = mido_create_rule(vpcif->postchain, vpcif->midos[INST_POSTCHAIN],
                NULL, &rulepos,
                "position", pos_str, "type", "accept", "matchReturnFlow", "true", NULL);
        if (rc) {
            LOGWARN("Failed to create ingress conntrack for %s\n", gniif->name);
            ret++;
        }

        // plus two accept for metadata egress
        rulepos = vpcif->prechain->rules_count + 1;
        snprintf(pos_str, 32, "%d", rulepos);
        rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                NULL, &rulepos,
                "position", pos_str, "type", "accept", "nwDstAddress", pt_buf, "nwDstLength", "32", NULL);
        if (rc) {
            LOGWARN("Failed to create egress +2 rule for %s\n", gniif->name);
            ret++;
        }

        // drops
        // default drop all else egress
        rulepos = vpcif->prechain->rules_count + 1;
        snprintf(pos_str, 32, "%d", rulepos);
        rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                NULL, &rulepos,
                "position", pos_str, "type", "drop", "invDlType",
                "true", "dlType", "2054", NULL);
        if (rc) {
            LOGWARN("Failed to create egress drop rule for %s\n", gniif->name);
            ret++;
        }

        // default drop all else ingress
        rulepos = vpcif->postchain->rules_count + 1;
        snprintf(pos_str, 32, "%d", rulepos);
        rc = mido_create_rule(vpcif->postchain, vpcif->midos[INST_POSTCHAIN],
                NULL, &rulepos,
                "type", "drop", "invDlType", "true", "position", pos_str,
                "dlType", "2054", NULL);
        if (rc) {
            LOGWARN("Failed to create ingress drop rule for %s\n", gniif->name);
            ret++;
        }

        // now set up the jumps to SG chains
        if (!vpcif->population_failed && !vpcif->sg_changed) {
            LOGTRACE("12095:\t%s sec groups did not change\n", gniif->name);
        } else {
            // Get all SG jump rules from interface chains
            rc = mido_get_jump_rules(vpcif->prechain, &jprules_egress, &max_jprules_egress,
                    &jprules_tgt_egress, &max_jprules_egress);
            rc = mido_get_jump_rules(vpcif->postchain,&jprules_ingress, &max_jprules_ingress,
                    &jprules_tgt_ingress, &max_jprules_ingress);
            jpe_gni_present = EUCA_ZALLOC_C(max_jprules_egress, sizeof (int));
            jpi_gni_present = EUCA_ZALLOC_C(max_jprules_ingress, sizeof (int));

            for (j = 0; j < gniif->max_secgroup_names; j++) {
                // go through the interface SGs in GNI
                if (gniif->gnisgs[j] && gniif->gnisgs[j]->mido_present) {
                    vpcsecgroup = (mido_vpc_secgroup *) gniif->gnisgs[j]->mido_present;
                    if (vpcsecgroup) {
                        found = 0;
                        for (k = 0; k < max_jprules_egress && !found; k++) {
                            if (!strcmp(vpcsecgroup->midos[VPCSG_EGRESS]->uuid, jprules_tgt_egress[k])) {
                                LOGTRACE("12095:\tegress jump to %s found.\n", vpcsecgroup->name);
                                jpe_gni_present[k] = 1;
                                found = 1;
                            }
                        }
                        if (!found) {
                            // add the SG chain jump egress - right before the drop rule
                            rulepos = vpcif->prechain->rules_count;
                            snprintf(pos_str, 32, "%d", rulepos);
                            rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                                    NULL, &rulepos,
                                    "position", pos_str, "type", "jump", "jumpChainId",
                                    vpcsecgroup->midos[VPCSG_EGRESS]->uuid, NULL);
                            if (rc) {
                                LOGWARN("Failed to create egress jump rule %s %s\n", vpcsecgroup->name, gniif->name);
                                ret++;
                            }
                        }

                        // Get the ingress jump rules currently in mido
                        found = 0;
                        for (k = 0; k < max_jprules_ingress && !found; k++) {
                            if (!strcmp(vpcsecgroup->midos[VPCSG_INGRESS]->uuid, jprules_tgt_ingress[k])) {
                                LOGTRACE("12095:\tingress jump to %s found.\n", vpcsecgroup->name);
                                jpi_gni_present[k] = 1;
                                found = 1;
                            }
                        }
                        if (!found) {
                            // add the SG chain jump egress - right before the drop rule
                            rulepos = vpcif->postchain->rules_count;
                            snprintf(pos_str, 32, "%d", rulepos);
                            rc = mido_create_rule(vpcif->postchain, vpcif->midos[INST_POSTCHAIN],
                                    NULL, &rulepos,
                                    "position", pos_str, "type", "jump", "jumpChainId",
                                    vpcsecgroup->midos[VPCSG_INGRESS]->uuid, NULL);
                            if (rc) {
                                LOGWARN("Failed to create ingress jump rule %s %s\n", vpcsecgroup->name, gniif->name);
                                ret++;
                            }
                        }
                    } else {
                        LOGWARN("cannot locate %s\n", gniif->secgroup_names[j].name);
                        ret++;
                    }
                } else {
                    LOGWARN("Inconsistent GNI detected while processing %s\n", gniif->name);
                    ret++;
                }
            }
            
            // Delete jump rules not in GNI
            for (j = 0; j < max_jprules_egress; j++) {
                if (jpe_gni_present[j] == 0) {
                    rc = mido_delete_rule(vpcif->prechain, jprules_egress[j]);
                    if (rc != 0) {
                        LOGWARN("failed to delete %s egress jump rule\n", vpcif->name);
                    }
                }
            }
            for (j = 0; j < max_jprules_ingress; j++) {
                if (jpi_gni_present[j] == 0) {
                    rc = mido_delete_rule(vpcif->postchain, jprules_ingress[j]);
                    if (rc != 0) {
                        LOGWARN("failed to delete %s ingress jump rule\n", vpcif->name);
                    }
                }
            }

            // release memory
            for (k = 0; k < max_jprules_egress; k++) {
                EUCA_FREE(jprules_tgt_egress[k]);
            }
            for (k = 0; k < max_jprules_ingress; k++) {
                EUCA_FREE(jprules_tgt_ingress[k]);
            }
            EUCA_FREE(jprules_egress);
            EUCA_FREE(jprules_tgt_egress);
            EUCA_FREE(jpe_gni_present);
            EUCA_FREE(jprules_ingress);
            EUCA_FREE(jprules_tgt_ingress);
            EUCA_FREE(jpi_gni_present);
        }

        LOGDEBUG("\t%s implemented in %.2f ms\n", vpcif->name, eucanetd_timer_usec(&tv) / 1000.0);
    }
    
    return (ret);
}

/**
 * Executes VPCMIDO maintenance.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. 1 otherwise.
 */
int do_midonet_maint(mido_config *mido) {
    //int rc = 0;
    int ret = 0;
    //struct timeval tv;

    if (!mido) {
        return (1);
    }

/*
    eucanetd_timer_usec(&tv);
    if (0 && midonet_api_system_changed) {
        eucanetd_timer_usec(&tv);
        rc = reinitialize_mido(mido);
        LOGINFO("\treinitialize_mido() in %ld us.\n", eucanetd_timer_usec(&tv));

        rc = do_midonet_populate(mido);
        if (rc) {
            LOGERROR("failed to populate euca VPC models.\n");
            return (1);
        }
        LOGINFO("midonet (re)populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
        mido_info_http_count();
        midonet_api_system_changed = 0;
    }
*/
    return (ret);
}

/**
 * Executes a VPCMIDO update based on the Global Network Information.
 * @param gni [in] current global network state.
 * @param appliedGni [in] most recently applied global network state.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. 1 otherwise.
 */
int do_midonet_update(globalNetworkInfo *gni, globalNetworkInfo *appliedGni, mido_config *mido) {
    int rc = 0, ret = 0;
    struct timeval tv;

    if (!gni || !mido) {
        return (1);
    }
    if (appliedGni == NULL) {
        midocache_invalid = 1;
    }

    mido->enabledCLCIp = gni->enabledCLCIp;
    eucanetd_timer_usec(&tv);
    if (!midocache_invalid) {
        clear_mido_gnitags(mido);
        LOGINFO("\tgni/mido tags cleared in %ld us.\n", eucanetd_timer_usec(&tv));
    } else {
        midocache_invalid = 0;

        rc = do_midonet_populate(mido);
        if (rc) {
            LOGERROR("failed to populate euca VPC models.\n");
            ret++;
        }
        LOGINFO("\tmidonet populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
        mido_info_http_count();
        midonet_api_system_changed = 0;
    }

    rc = do_midonet_update_pass1(gni, appliedGni, mido);
    if (rc) {
        LOGERROR("pass1: failed update - check midonet health\n");
        ret++;
    }
    LOGINFO("\tgni/mido tagging processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    rc = do_midonet_update_pass2(gni, mido);
    if (rc) {
        LOGERROR("pass2: failed update - check midonet health\n");
        ret++;
    }
    LOGINFO("\tremove anything in mido not in gni processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    if (ret) {
        rc = mido_check_state();
        if (rc) {
            LOGERROR("===\n");
            LOGERROR("Unable to access midonet-api.\n");
            LOGERROR("===\n");
        }
        return (ret);
    }

    rc = do_midonet_update_pass3_vpcs(gni, mido);
    if (rc) {
        LOGERROR("pass3_vpcs: failed update - check midonet health\n");
        return (1);
    }
    LOGINFO("\tvpcs processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    rc = do_midonet_update_pass3_sgs(gni, mido);
    if (rc) {
        LOGERROR("pass3_sgs: failed update - check midonet health\n");
        return (1);
    }
    LOGINFO("\tsgs processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    rc = do_midonet_update_pass3_insts(gni, mido);
    if (rc) {
        LOGERROR("pass3_insts: failed update - check midonet health\n");
        return (1);
    }
    LOGINFO("\tinstances processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    mido_info_http_count();
    return (ret);
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
int get_next_router_id(mido_config * mido, int *nextid) {
    int i;
    for (i = 2; i < MAX_RTID; i++) {
        if (!mido->router_ids[i]) {
            mido->router_ids[i] = 1;
            *nextid = i;
            LOGDEBUG("router id %d allocated.\n", i);
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
int set_router_id(mido_config * mido, int id) {
    if (id < MAX_RTID) {
        mido->router_ids[id] = 1;
        return (0);
    }
    return (1);
}

//!
//! Clears the use flag of a router ID.
//!
//! @param[in] mido current mido_config data structure.
//! @param[in] id router ID of interest.
//!
//! @return 0 on success. 1 otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int clear_router_id(mido_config * mido, int id) {
    if (id < MAX_RTID) {
        mido->router_ids[id] = 0;
        LOGDEBUG("router id %d released.\n", id);
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
void print_mido_vpc(mido_vpc * vpc) {
    LOGDEBUG("PRINT VPC: name=%s max_subnets=%d gnipresent=%d\n", vpc->name, vpc->max_subnets, vpc->gnipresent);
    mido_print_midoname(vpc->midos[VPC_VPCRT]);
    mido_print_midoname(vpc->midos[VPC_EUCABR_DOWNLINK]);
    mido_print_midoname(vpc->midos[VPC_VPCRT_UPLINK]);
    mido_print_midoname(vpc->midos[VPC_VPCRT_UPLINK_PRECHAIN]);
    mido_print_midoname(vpc->midos[VPC_VPCRT_UPLINK_POSTCHAIN]);
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
void print_mido_vpc_subnet(mido_vpc_subnet * vpcsubnet) {
    LOGDEBUG("PRINT VPCSUBNET: name=%s vpcname=%s max_instances=%d gnipresent=%d\n", vpcsubnet->name, vpcsubnet->vpcname,
            vpcsubnet->max_instances, vpcsubnet->gnipresent);
    mido_print_midoname(vpcsubnet->midos[SUBN_BR]);
    mido_print_midoname(vpcsubnet->midos[SUBN_BR_RTPORT]);
    mido_print_midoname(vpcsubnet->midos[SUBN_VPCRT_BRPORT]);
    mido_print_midoname(vpcsubnet->midos[SUBN_BR_DHCP]);
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
void print_mido_vpc_instance(mido_vpc_instance * vpcinstance) {
    LOGDEBUG("PRINT VPCINSTANCE: name=%s gnipresent=%d\n", vpcinstance->name, vpcinstance->gnipresent);
    mido_print_midoname(vpcinstance->midos[INST_VPCBR_VMPORT]);
    mido_print_midoname(vpcinstance->midos[INST_VPCBR_DHCPHOST]);
    mido_print_midoname(vpcinstance->midos[INST_VMHOST]);
    mido_print_midoname(vpcinstance->midos[INST_ELIP_PRE]);
    mido_print_midoname(vpcinstance->midos[INST_ELIP_POST]);
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
void print_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup) {
    int i;
    LOGDEBUG("PRINT VPCSECGROUP: name=%s gnipresent=%d\n", vpcsecgroup->name, vpcsecgroup->gnipresent);
    for (i = 0; i < VPCSG_END; i++) {
        mido_print_midoname(vpcsecgroup->midos[i]);
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
int find_mido_vpc_instance(mido_vpc_subnet * vpcsubnet, char *instancename, mido_vpc_instance ** outvpcinstance) {
    int i;
    if (!vpcsubnet || !instancename || !outvpcinstance) {
        return (1);
    }

    LOGTRACE("Searching instance %s\n", instancename);
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
    mido_vpc *vpc = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;

    if (!mido || !instancename || !outvpcinstance) {
        return (1);
    }

    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            rc = find_mido_vpc_instance(vpcsubnet, instancename, outvpcinstance);
            if (!rc) {
                return (0);
            }
        }
    }

    return (1);
}

/**
 * Searches the discovered VPC data structure for the NAT gateway in the argument.
 *
 * @param vpc [in] data structure holding information about a VPC.
 * @param natgname [in] name of the NAT Gateway of interest.
 * @param outvpcnatgateway [out] pointer to vpcnatgateway data structure of interest (if found).
 *
 * @return 0 if the NAT gateway is found. 1 otherwise.
 */
int find_mido_vpc_natgateway(mido_vpc *vpc, char *natgname, mido_vpc_natgateway **outvpcnatgateway) {
    if (!vpc || !natgname || !outvpcnatgateway) {
        return (1);
    }

    *outvpcnatgateway = NULL;
    for (int i = 0; i < vpc->max_subnets; i++) {
        mido_vpc_subnet *vpcsubnet = &(vpc->subnets[i]);
        for (int j = 0; j < vpcsubnet->max_natgateways; j++) {
            if ((!vpcsubnet->natgateways[j].midos[NATG_RT]) || (vpcsubnet->natgateways[j].midos[NATG_RT]->init == 0)) {
                continue;
            }
            LOGEXTREME("Is (mido) %s == (gni) %s\n", vpcsubnet->natgateways[j].name, natgname);
            if (!strcmp(natgname, vpcsubnet->natgateways[j].name)) {
                *outvpcnatgateway = &(vpcsubnet->natgateways[j]);
                return (0);
            }
        }
    }

    return (1);
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
int find_mido_vpc_subnet(mido_vpc * vpc, char *subnetname, mido_vpc_subnet ** outvpcsubnet) {
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

/**
 * Given the mido VPC, mido subnet, and GNI route table in the argument, implement
 * the route table.
 *
 * @param mido [in] data structure that holds MidoNet configuration
 * @param vpc [in] information about the VPC of interest - discovered in MidoNet
 * @param vpcsubnet [in] VPC subnet of interest - discovered in MidoNet
 * @param subnetNetaddr [in] network address of VPC subnet CIDR block
 * @param subnetSlashnet [in] subnet mask in /xx form
 * @param rtable [in] route table as described in GNI to be implemented
 * @param gnivpc [in] VPC of the subnet associated with the route table to be implemented, as described in GNI
 *
 * @return 0 on success. 1 otherwise.
 *
 * @pre MidoNet discovery is assumed to be executed and all MidoNet resources pre-populated
 * in mido_config, mido_vpc, and mido_vpc_subnet data structures. The VPC subnet route table 
 * is assumed to be free of unnecessary route entries.
 */
int create_mido_vpc_subnet_route_table(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet,
        char *subnetNetaddr, char *subnetSlashnet, gni_route_table *rtable, gni_vpc *gnivpc) {
    int i = 0, j = 0;
    int rc = 0;
    int found = 0;
    mido_parsed_route *rt_entries = NULL;
    int max_rt_entries = 0;
    struct timeval tv = {0};

    if (!mido || !vpc || !vpcsubnet || !rtable) {
        LOGWARN("Invalid argument - cannot create NULL route table.\n");
        return (1);
    }
    eucanetd_timer_usec(&tv);
    LOGTRACE("Creating route table %s\n", rtable->name);
    // Parse GNI routes
    rc = parse_mido_vpc_subnet_route_table(mido, vpc, vpcsubnet, subnetNetaddr, subnetSlashnet,
            rtable, gnivpc, &rt_entries, &max_rt_entries);
    // Delete routes not described in GNI - cannot do in pass2 since routes needs to be parsed/translated
    for (i = 0; i < vpcsubnet->max_routes; i++) {
        found = 0;
        for (j = 0; j < max_rt_entries && !found; j++) {
            rc = mido_find_route_from_list(&(vpcsubnet->routes[i]), 1, &(rt_entries[j].rport),
                    rt_entries[j].src_net, rt_entries[j].src_length, rt_entries[j].dst_net,
                    rt_entries[j].dst_length, rt_entries[j].next_hop_ip, rt_entries[j].weight,
                    NULL);
            if (rc == 0) {
                found = 1;
                rt_entries[j].mido_present = 1;
            }
        }
        if (found) {
            LOGTRACE("\t%s in GNI: Y\n", vpcsubnet->routes[i]->name);
        } else {
            LOGTRACE("\t%s in GNI: N - deleting\n", vpcsubnet->routes[i]->name);
            rc = mido_delete_route(vpc->vpcrt, vpcsubnet->routes[i]);
            if (rc) {
                LOGWARN("failed to delete %s route %s\n", rtable->name, vpcsubnet->routes[i]->jsonbuf);
            }
        }
    }
    // Create routes not in mido
    rc = 0;
    for (i = 0; i < max_rt_entries; i++) {
        if (rt_entries[i].mido_present == 1) {
            continue;
        }
        LOGINFO("\tcreating route %s %s/%s -> %s\n", vpcsubnet->name, rt_entries[i].dst_net, rt_entries[i].dst_length, rt_entries[i].next_hop_ip);
        rc |= mido_create_route(NULL, &(rt_entries[i].router), &(rt_entries[i].rport),
                rt_entries[i].src_net, rt_entries[i].src_length, rt_entries[i].dst_net,
                rt_entries[i].dst_length, rt_entries[i].next_hop_ip, rt_entries[i].weight,
                NULL);
    }
    mido_free_mido_parsed_route_list(rt_entries, max_rt_entries);
    EUCA_FREE(rt_entries);
    return (rc);
}

//!
//! Given the mido VPC, mido subnet, and GNI route table in the argument, parse
//! the GNI route table and fill the proutes array.
//!
//! @param[in]  mido data structure that holds MidoNet configuration
//! @param[in]  vpc information about the VPC of interest - discovered in MidoNet
//! @param[in]  vpcsubnet VPC subnet of interest - discovered in MidoNet
//! @param[in]  subnetNetaddr network address of VPC subnet CIDR block
//! @param[in]  subnetSlashnet subnet mask in /xx form
//! @param[in]  rtable route table as described in GNI to be implemented.
//! @param[in]  gnivpc VPC of the subnet associated with the route table to be implemented, as described in GNI
//! @param[out] proutes pointer where the resulting array of mido_parsed_route structures will be located. 
//! @param[out] max_proutes number of parsed routes. 
//!
//! @return 0  on success. 1 otherwise.
//!
//! @see
//!
//! @pre MidoNet discovery is assumed to be executed and all MidoNet resources pre-populated
//! in mido_config, mido_vpc, and mido_vpc_subnet data structures.
//!
//! @post
//!
//! @note
//!
int parse_mido_vpc_subnet_route_table(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet,
        char *subnetNetaddr, char *subnetSlashnet, gni_route_table *rtable, gni_vpc *gnivpc,
        mido_parsed_route **proutes, int *max_proutes) {
    int i = 0;
    int j = 0;
    char eucartgw[32];
    char natgw[32];
    char *tmpstr = NULL;
    char dstNetaddr[24], dstSlashnet[8];
    mido_parsed_route *retroutes = NULL;
    int max_retroutes = 0;
    gni_instance **gniinterfaces = NULL;
    int max_gniinterfaces = 0;
    boolean valid = FALSE;

    LOGTRACE("Parsing vpc subnet route table\n");
    if (!mido || !vpc || !vpcsubnet || !rtable || !proutes || !max_proutes) {
        LOGWARN("Invalid argument - cannot parse NULL route table.\n");
        return (1);
    }

    gniinterfaces = gnivpc->interfaces;
    max_gniinterfaces = gnivpc->max_interfaces;
    tmpstr = hex2dot(mido->int_rtaddr);
    snprintf(eucartgw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    for (i = 0; i < rtable->max_entries; i++) {
        cidr_split(rtable->entries[i].destCidr, dstNetaddr, dstSlashnet, NULL, NULL);

        switch (parse_mido_route_entry_target(rtable->entries[i].target)) {
            case VPC_TARGET_LOCAL:
                // Local route cannot be removed. It is implemented on VPC subnet creation.
                LOGTRACE("local route added on subnet creation. Nothing to do.\n");
                break;
            case VPC_TARGET_INTERNET_GATEWAY:
                valid = FALSE;
                for (j = 0; j < gnivpc->max_internetGatewayNames && !valid; j++) {
                    // Sanity check: make sure that the target igw is in the correct VPC
                    if (!strcmp(gnivpc->internetGatewayNames[j].name, rtable->entries[i].target)) {
                        LOGTRACE("%s in VPC: Y\n", rtable->entries[i].target);
                        valid = TRUE;
                    } else {
                        LOGTRACE("%s in VPC: N\n", rtable->entries[i].target);
                    }
                }
                if (valid) {
                    retroutes = EUCA_REALLOC(retroutes, max_retroutes + 1, sizeof (mido_parsed_route));
                    bzero(&(retroutes[max_retroutes]), sizeof (mido_parsed_route));
                    mido_copy_midoname(&(retroutes[max_retroutes].router), vpc->midos[VPC_VPCRT]);
                    mido_copy_midoname(&(retroutes[max_retroutes].rport), vpc->midos[VPC_VPCRT_UPLINK]);
                    retroutes[max_retroutes].src_net = strdup(subnetNetaddr);
                    retroutes[max_retroutes].src_length = strdup(subnetSlashnet);
                    retroutes[max_retroutes].dst_net = strdup(dstNetaddr);
                    retroutes[max_retroutes].dst_length = strdup(dstSlashnet);
                    retroutes[max_retroutes].next_hop_ip = strdup(eucartgw);
                    retroutes[max_retroutes].weight = strdup("10");
                    max_retroutes++;
                } else {
                    LOGWARN("Invalid igw route target %s\n", rtable->entries[i].target);
                }
                break;
            case VPC_TARGET_ENI:
                valid = FALSE;
                gni_instance *interface = NULL;
                mido_vpc_subnet *ifvpcsubnet = NULL;
                for (j = 0; j < max_gniinterfaces && !valid; j++) {
                    // Sanity check: make sure that the target interface is attached to the correct VPC
                    if (!strcmp(gniinterfaces[j]->ifname, rtable->entries[i].target)) {
                        LOGTRACE("%s == %s: Y\n", gniinterfaces[j]->ifname, rtable->entries[i].target);
                        interface = gniinterfaces[j];
                        valid = TRUE;
                    } else {
                        LOGTRACE(" %s == %s: N\n", gniinterfaces[j]->ifname, rtable->entries[i].target);
                    }
                }
                if (valid) {
                    // find the VPC subnet in mido data structures
                    valid = FALSE;
                    for (j = 0; j < vpc->max_subnets; j++) {
                        if (!strcmp(vpc->subnets[j].name, interface->subnet)) {
                            LOGTRACE("%s in %s: Y\n", interface->name, vpc->subnets[j].name);
                            ifvpcsubnet = &(vpc->subnets[j]);
                            valid = TRUE;
                        } else {
                            LOGTRACE("%s in %s: N\n", interface->name, vpc->subnets[j].name);
                        }
                    }
                }
                if (valid) {
                    retroutes = EUCA_REALLOC(retroutes, max_retroutes + 1, sizeof (mido_parsed_route));
                    bzero(&(retroutes[max_retroutes]), sizeof (mido_parsed_route));
                    mido_copy_midoname(&(retroutes[max_retroutes].router), vpc->midos[VPC_VPCRT]);
                    mido_copy_midoname(&(retroutes[max_retroutes].rport), ifvpcsubnet->midos[SUBN_VPCRT_BRPORT]);
                    retroutes[max_retroutes].src_net = strdup(subnetNetaddr);
                    retroutes[max_retroutes].src_length = strdup(subnetSlashnet);
                    retroutes[max_retroutes].dst_net = strdup(dstNetaddr);
                    retroutes[max_retroutes].dst_length = strdup(dstSlashnet);
                    retroutes[max_retroutes].next_hop_ip = hex2dot(interface->privateIp);
                    retroutes[max_retroutes].weight = strdup("30");
                    max_retroutes++;
                } else {
                    LOGWARN("Invalid eni route target %s\n", rtable->entries[i].target);
                }
                break;
            case VPC_TARGET_NAT_GATEWAY:
                valid = FALSE;
                gni_nat_gateway *natgateway = NULL;
                mido_vpc_subnet *natgatewaysubnet = NULL;

                for (j = 0; j < gnivpc->max_natGateways && !valid; j++) {
                    // Sanity check: make sure that the target nat is in the correct VPC
                    if (!strcmp(gnivpc->natGateways[j].name, rtable->entries[i].target)) {
                        LOGTRACE("%s in %s: Y\n", rtable->entries[i].target, gnivpc->name);
                        natgateway = &(gnivpc->natGateways[j]);
                        valid = TRUE;
                    } else {
                        LOGTRACE("%s in %s: N\n", rtable->entries[i].target, gnivpc->name);
                    }
                }
                if (valid) {
                    // find the VPC subnet in mido data structures
                    valid = FALSE;
                    for (j = 0; j < vpc->max_subnets; j++) {
                        if (!strcmp(vpc->subnets[j].name, natgateway->subnet)) {
                            LOGTRACE("%s in %s: Y\n", natgateway->name, vpc->subnets[j].name);
                            natgatewaysubnet = &(vpc->subnets[j]);
                            valid = TRUE;
                        } else {
                            LOGTRACE("%s in %s: N\n", natgateway->name, vpc->subnets[j].name);
                        }
                    }
                }
                if (valid) {
                    tmpstr = hex2dot(natgateway->privateIp);
                    snprintf(natgw, 32, "%s", tmpstr);
                    EUCA_FREE(tmpstr);

                    retroutes = EUCA_REALLOC(retroutes, max_retroutes + 1, sizeof (mido_parsed_route));
                    bzero(&(retroutes[max_retroutes]), sizeof (mido_parsed_route));
                    mido_copy_midoname(&(retroutes[max_retroutes].router), vpc->midos[VPC_VPCRT]);
                    mido_copy_midoname(&(retroutes[max_retroutes].rport), natgatewaysubnet->midos[SUBN_VPCRT_BRPORT]);
                    retroutes[max_retroutes].src_net = strdup(subnetNetaddr);
                    retroutes[max_retroutes].src_length = strdup(subnetSlashnet);
                    retroutes[max_retroutes].dst_net = strdup(dstNetaddr);
                    retroutes[max_retroutes].dst_length = strdup(dstSlashnet);
                    retroutes[max_retroutes].next_hop_ip = strdup(natgw);
                    retroutes[max_retroutes].weight = strdup("20");
                    max_retroutes++;
                } else {
                    LOGWARN("Invalid nat gateway route target %s\n", rtable->entries[i].target);
                }
                break;
            case VPC_TARGET_PEERING:
            case VPC_TARGET_VPRIVATE_GATEWAY:
            default:
                LOGWARN("Invalid or unsupported route target %s\n", rtable->entries[i].target);
                break;
        }

    }
    *proutes = retroutes;
    *max_proutes = max_retroutes;
    return (0);
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
int find_mido_vpc(mido_config * mido, char *vpcname, mido_vpc ** outvpc) {
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
//! Searches the ports discovered in MidoNet for the ones that belong to the
//! router in the argument.
//!
//! @param[in] mido data structure holding all discovered MidoNet resources.
//! @param[in] device router of interest - it is assumed that this is indeed a router,
//! checks are not performed.
//! @param[out] outports reference to midoname data structure of the ports that
//! belong to the given device. Memory is allocated. Caller should release once
//! done.
//! @param[out] outports_max number of ports that belong to the router of interest.
//!
//! @return 0 if port(s) that belong(s) to the given device is/are found among
//! discovered Midonet ports. 1 otherwise.
//!
//! @see
//!
//! @pre mido_config data structure should have been populated.
//!
//! @post
//!
//! @note
//!
/*
int find_mido_router_ports(mido_config *mido, midoname *device, midoname **outports, int *outports_max) {
    int i;
    int rc;
    int ret = 1;
    int startindex = 0;
    char *rtuuid = NULL;
    midoname *rtport = NULL;
    midoname *retports = NULL;

    if (!mido || !outports || !outports_max || !device) {
        return (1);
    }
    if (!device->init) {
        return (1);
    }

 *outports_max = 0;
    LOGDEBUG("Searching for ports of router %s\n", device->name);
    for (i = 0; (i < mido->max_rtports) && (ret); i++) {
        rtport = &(mido->rtports[i]);
        rc = mido_getel_midoname(rtport, "deviceId", &rtuuid);
        if ((rc == 0) && (!strcmp(rtuuid, device->uuid))) {
            LOGDEBUG("Found device port %s\n", rtport->name);
            if (*outports_max == 0) {
                startindex = i;
            }
            (*outports_max)++;
        } else {
            if (*outports_max != 0) {
                ret = 0;
            }
        }
        EUCA_FREE(rtuuid);
        rtuuid = NULL;
    }
    if (*outports_max != 0) {
        retports = EUCA_ZALLOC(*outports_max, sizeof(midoname));
        if (!retports) {
            LOGERROR("out of memory.\n");
 *outports_max = 0;
            return (1);
        }
        for (i = 0; i < (*outports_max); i++) {
            mido_copy_midoname(&(retports[i]), &(mido->rtports[startindex + i]));
        }
 *outports = retports;
    }
    return (ret);
}
 */

//!
//! Searches the ports discovered in MidoNet for the ones that belong to the
//! bridge in the argument.
//!
//! @param[in] mido data structure holding all discovered MidoNet resources.
//! @param[in] bridge of interest - it is assumed that this is indeed a bridge,
//! checks are not performed.
//! @param[out] outports reference to midoname data structure of the ports that
//! belong to the given device. Memory is allocated. Caller should release once
//! done.
//! @param[out] outports_max number of ports that belong to the bridge of interest.
//!
//! @return 0 if port(s) that belong(s) to the given device is/are found among
//! discovered Midonet ports. 1 otherwise.
//!
//! @see
//!
//! @pre mido_config data structure should have been populated.
//!
//! @post
//!
//! @note
//!
/*
int find_mido_bridge_ports(mido_config *mido, midoname *device, midoname **outports, int *outports_max) {
    int i;
    int rc;
    int ret = 1;
    int startindex = 0;
    char *bruuid = NULL;
    midoname *brport = NULL;
    midoname *retports = NULL;

    if (!mido || !outports || !outports_max || !device) {
        return (1);
    }
    if (!device->init) {
        return (1);
    }

 *outports_max = 0;
    LOGDEBUG("Searching for ports of bridge %s\n", device->name);
    for (i = 0; (i < mido->max_brports) && (ret); i++) {
        brport = &(mido->brports[i]);
        rc = mido_getel_midoname(brport, "deviceId", &bruuid);
        if ((rc == 0) && (!strcmp(bruuid, device->uuid))) {
            LOGDEBUG("Found device port %s\n", brport->name);
            if (*outports_max == 0) {
                startindex = i;
            }
            (*outports_max)++;
        } else {
            if (*outports_max != 0) {
                ret = 0;
            }
        }
        EUCA_FREE(bruuid);
        bruuid = NULL;
    }
    if (*outports_max != 0) {
        retports = EUCA_ZALLOC(*outports_max, sizeof(midoname));
        if (!retports) {
            LOGERROR("out of memory.\n");
 *outports_max = 0;
            return (1);
        }
        for (i = 0; i < (*outports_max); i++) {
            mido_copy_midoname(&(retports[i]), &(mido->brports[startindex + i]));
        }
 *outports = retports;
    }
    return (ret);
}
 */

/**
 * Searches a bridge for the port that matches the given interface name.
 *
 * @param bridge [in] data structure holding MidoNet bridge model.
 * @param name [in] name of the interface of interest. Partial matches are accepted.
 *
 * @return pointer to midoname data structure if found. NULL otherwise.
 */
midoname *find_mido_bridge_port_byinterface(midonet_api_bridge *br, char *name) {
    int i = 0;
    int rc = 0;
    int found = 0;
    char *tmpstr = NULL;
    midoname *res = NULL;

    if (!br || !name) {
        LOGWARN("Invalid argument: unable to search NULL bridge ports.\n");
        return (NULL);
    }
    for (i = 0; i < br->max_ports && !found; i++) {
        if (br->ports[i] == NULL) {
            continue;
        }
        tmpstr = NULL;
        rc = mido_getel_midoname(br->ports[i], "interfaceName", &tmpstr);
        if (!rc && tmpstr && strlen(tmpstr)) {
            if (strstr(tmpstr, name)) {
                found = 1;
                res = br->ports[i];
            }
        }
        EUCA_FREE(tmpstr);
    }

    return (res);
}

//!
//! Searches the given list of MidoNet ports for ports that belongs to the device
//! specified in the argument.
//!
//! @param[in] ports pointer to an array of MidoNet ports.
//! @param[in] max_ports number of ports in the array.
//! @param[in] device of interest
//! @param[out] outports pointer to an array of midoname data structure references
//! of the ports that belong to the given device. Memory is allocated.
//! Caller should release once done.
//! @param[out] outports_max number of ports that belong to the device of interest.
//!
//! @return 0 if port(s) that belong(s) to the given device is/are found. 1 otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int find_mido_device_ports(midoname **ports, int max_ports, midoname *device, midoname ***outports, int *outports_max) {
    int i;
    int rc;
    char *devuuid = NULL;
    midoname *port = NULL;
    midoname **retports = NULL;

    if (!ports || !max_ports || !outports || !outports_max || !device) {
        return (1);
    }
    if (device->init == 0) {
        return (1);
    }
    *outports_max = 0;
    for (i = 0; i < max_ports; i++) {
        port = ports[i];
        if (port->init == 0) {
            continue;
        }
        rc = mido_getel_midoname(port, "deviceId", &devuuid);
        if ((rc == 0) && (!strcmp(devuuid, device->uuid))) {
            retports = EUCA_REALLOC(retports, *outports_max + 1, sizeof (midoname *));
            retports[*outports_max] = port;
            (*outports_max)++;
        }
        EUCA_FREE(devuuid);
        devuuid = NULL;
    }
    *outports = retports;
    return (0);
}

//!
//! Searches the given list of MidoNet ports for ports bound to the host
//! specified in the argument.
//!
//! @param[in] ports pointer to an array of MidoNet ports.
//! @param[in] max_ports number of ports in the array.
//! @param[in] host of interest
//! @param[out] outports pointer to an array of midoname data structure references
//! of the ports with binding to the given host. Memory is allocated.
//! Caller should release once done.
//! @param[out] outports_max number of ports with binding to the host of interest.
//!
//! @return 0 if port(s) with binding to the given host is found. 1 otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int find_mido_host_ports(midoname **ports, int max_ports, midoname *host, midoname ***outports, int *outports_max) {
    int i;
    int rc;
    char *hostuuid = NULL;
    midoname *port = NULL;
    midoname **retports = NULL;

    if (!ports || !max_ports || !outports || !outports_max || !host) {
        return (1);
    }
    if (host->init == 0) {
        return (1);
    }
    *outports_max = 0;
    for (i = 0; i < max_ports; i++) {
        port = ports[i];
        if (port->init == 0) {
            continue;
        }
        rc = mido_getel_midoname(port, "hostId", &hostuuid);
        if ((rc == 0) && (!strcmp(hostuuid, host->uuid))) {
            retports = EUCA_REALLOC(retports, *outports_max + 1, sizeof (midoname *));
            retports[*outports_max] = port;
            (*outports_max)++;
        }
        EUCA_FREE(hostuuid);
        hostuuid = NULL;
    }
    *outports = retports;
    return (0);
}

//!
//! Searches the given list of MidoNet ports for ports that are members of the
//! portgroup specified in the argument.
//!
//! @param[in] ports pointer to an array of MidoNet ports.
//! @param[in] max_ports number of ports in the array.
//! @param[in] portgroup of interest
//! @param[out] outports pointer to an array of midoname data structure references
//! of the ports that are members of the given portgroup. Memory is allocated.
//! Caller should release once done.
//! @param[out] outports_max number of ports with binding to the host of interest.
//!
//! @return 0 if port(s) that is/are member(s) of the given portgroup is/are found. 1 otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int find_mido_portgroup_ports(midoname **ports, int max_ports, midoname *portgroup, midoname ***outports, int *outports_max) {
    int i, j;
    int rc;
    char *portuuid = NULL;
    midoname *port = NULL;
    midoname **retports = NULL;

    if (!ports || !max_ports || !outports || !outports_max || !portgroup) {
        return (1);
    }
    if (portgroup->init == 0) {
        return (1);
    }

    midoname **pgports = NULL;
    int pgports_max = 0;
    rc = mido_get_portgroup_ports(portgroup, &pgports, &pgports_max);
    if (!rc && pgports_max) {
        *outports_max = 0;
        retports = EUCA_ZALLOC_C(pgports_max, sizeof (midoname *));
        for (i = 0; i < pgports_max; i++) {
            rc = mido_getel_midoname(pgports[i], "portId", &portuuid);
            if (rc == 0) {
                for (j = 0; j < max_ports; j++) {
                    port = ports[j];
                    if (port->init == 0) {
                        continue;
                    }
                    if (!strcmp(portuuid, port->uuid)) {
                        retports[*outports_max] = port;
                        (*outports_max)++;
                    }
                }
            } else {
                LOGWARN("Unable to retrieve port UUID for %s\n", pgports[i]->name);
            }
            EUCA_FREE(portuuid);
        }
        EUCA_FREE(pgports);
    }
    if (pgports_max != *outports_max) {
        LOGWARN("Found %d members for portgroup %s (expected %d)\n", *outports_max, portgroup->name, pgports_max);
    }
    *outports = retports;
    return (0);
}

//!
//! Parses the given vpcsubnet MidoNet resource to get its corresponding CIDR (network
//! address and network mask).
//!
//! @param[in] vpcsubnet VPC subnet of interest - discovered in MidoNet
//! @param[out] net pointer to a string that holds the network address information
//! @param[out] length pointer to a string that holds the network mask information
//!
//! @return 0 if net/mask information of the given vpcsubnet is successfully parsed. 1 otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int parse_mido_vpc_subnet_cidr(mido_vpc_subnet *vpcsubnet, char **net, char **length) {
    int rc = 0;
    if (!vpcsubnet || !net || !length) {
        LOGWARN("Invalid argument: NULL pointer.\n");
        return (1);
    }
    if ((!vpcsubnet->midos[SUBN_VPCRT_BRPORT]) || (!vpcsubnet->midos[SUBN_VPCRT_BRPORT]->init)) {
        LOGWARN("Invalid argument: vpcsubnet VPCRT_BRPORT not found.\n");
        return (1);
    }
    rc |= mido_getel_midoname(vpcsubnet->midos[SUBN_VPCRT_BRPORT], "networkAddress", net);
    rc |= mido_getel_midoname(vpcsubnet->midos[SUBN_VPCRT_BRPORT], "networkLength", length);
    return (rc);
}

//!
//! Parses the given route MidoNet resource to get its corresponding source network
//! (network/mask) and destination network (network/mask).
//!
//! @param[in] route route of interest - discovered in MidoNet
//! @param[out] srcnet pointer to a string that holds the source network address information
//! @param[out] srclength pointer to a string that holds the source network mask information
//! @param[out] dstnet pointer to a string that holds the destination network address information
//! @param[out] dstlength pointer to a string that holds the destination network mask information
//!
//! @return 0 if the parse is successful. 1 otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int parse_mido_vpc_route_addr(midoname *route, char **srcnet, char **srclength, char **dstnet, char **dstlength) {
    int rc = 0;
    if (!route) {
        LOGWARN("Invalid argument: NULL pointer.\n");
        return (1);
    }
    if (srcnet && srclength) {
        rc |= mido_getel_midoname(route, "srcNetworkAddr", srcnet);
        rc |= mido_getel_midoname(route, "srcNetworkLength", srclength);
    }
    if (dstnet && dstlength) {
        rc |= mido_getel_midoname(route, "dstNetworkAddr", dstnet);
        rc |= mido_getel_midoname(route, "dstNetworkLength", dstlength);
    }
    return (rc);
}

/**
 * Searches the discovered MidoNet resources for the given vpc subnet custom route
 * entries.
 *
 * @param mido [in] data structure that holds MidoNet configuration
 * @param vpc  [in]information about the VPC of interest - discovered in MidoNet
 * @param vpcsubnet [in] VPC subnet of interest - discovered in MidoNet
 *
 * @return 0 if custom route(s) of the given vpcsubnet is/are successfully searched.
 *         Successful search does not mean that routes are found. 1 otherwise.
 */
int find_mido_vpc_subnet_routes(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet) {
    int i = 0;
    int rc = 0;
    char *vpcsubnet_net = NULL;
    char *vpcsubnet_mask = NULL;
    char *srcnet = NULL, *srcmask = NULL;

    if (!mido || !vpc || !vpcsubnet) {
        LOGWARN("Invalid argument: cannot find subnet routes for NULL.\n");
        return (1);
    }
    if (strcmp(vpc->name, vpcsubnet->vpcname)) {
        LOGWARN("Invalid argument: %s is not a subnet of %s\n", vpcsubnet->vpcname, vpc->name);
        return (1);
    }
    if (vpcsubnet->routes) {
        EUCA_FREE(vpcsubnet->routes);
        vpcsubnet->routes = NULL;
        vpcsubnet->max_routes = 0;
    }

    rc = parse_mido_vpc_subnet_cidr(vpcsubnet, &vpcsubnet_net, &vpcsubnet_mask);
    if (rc != 0) {
        LOGWARN("Unable to find subnet information for %s\n", vpcsubnet->name);
        if (vpcsubnet_net) EUCA_FREE(vpcsubnet_net);
        if (vpcsubnet_mask) EUCA_FREE(vpcsubnet_mask);
        return (1);
    }

    midoname **rtroutes = vpc->vpcrt->routes;
    int max_rtroutes = vpc->vpcrt->max_routes;
    for (i = 0; i < max_rtroutes && !rc; i++) {
        if (rtroutes[i] == NULL) {
            continue;
        }
        rc = parse_mido_vpc_route_addr(rtroutes[i], &srcnet, &srcmask, NULL, NULL);
        if (rc == 0) {
            if ((!strcmp(srcnet, vpcsubnet_net)) && (!strcmp(srcmask, vpcsubnet_mask))) {
                vpcsubnet->routes = EUCA_REALLOC_C(vpcsubnet->routes, vpcsubnet->max_routes + 1, sizeof (midoname *));
                vpcsubnet->routes[vpcsubnet->max_routes] = rtroutes[i];
                vpcsubnet->max_routes = vpcsubnet->max_routes + 1;
            }
        } else {
            LOGWARN("Unable to parse %s routes\n", vpc->name);
        }
        EUCA_FREE(srcnet);
        EUCA_FREE(srcmask);
    }
    
    EUCA_FREE(vpcsubnet_net);
    EUCA_FREE(vpcsubnet_mask);
    return (rc);
}

/**
 * Populates an euca Security Group model.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param vpcsecgroup [i/o] data structure that holds the euca Security Group model of interest.
 * @return 0 on success. 1 on any failure.
 */
int populate_mido_vpc_secgroup(mido_config *mido, mido_vpc_secgroup *vpcsecgroup) {
    int ret = 0;
    int i = 0;
    char name[64];

    if (!mido || !vpcsecgroup) {
        return (1);
    }

    snprintf(name, 64, "sg_ingress_%11s", vpcsecgroup->name);
    midonet_api_chain *sgchain = mido_get_chain(name);
    if (sgchain != NULL) {
        LOGTRACE("Found SG chain %s\n", sgchain->obj->name);
        vpcsecgroup->ingress = sgchain;
        vpcsecgroup->midos[VPCSG_INGRESS] = sgchain->obj;
    }

    snprintf(name, 64, "sg_egress_%11s", vpcsecgroup->name);
    sgchain = mido_get_chain(name);
    if (sgchain != NULL) {
        LOGTRACE("Found SG chain %s\n", sgchain->obj->name);
        vpcsecgroup->egress = sgchain;
        vpcsecgroup->midos[VPCSG_EGRESS] = sgchain->obj;
    }

    snprintf(name, 64, "sg_priv_%11s", vpcsecgroup->name);
    midonet_api_ipaddrgroup *sgipaddrgroup = mido_get_ipaddrgroup(name);
    if (sgipaddrgroup != NULL) {
        LOGTRACE("Found SG IAG %s\n", sgipaddrgroup->obj->name);
        vpcsecgroup->iag_priv = sgipaddrgroup;
        vpcsecgroup->midos[VPCSG_IAGPRIV] = sgipaddrgroup->obj;
    }

    snprintf(name, 64, "sg_pub_%11s", vpcsecgroup->name);
    sgipaddrgroup = mido_get_ipaddrgroup(name);
    if (sgipaddrgroup != NULL) {
        LOGTRACE("Found SG IAG %s\n", sgipaddrgroup->obj->name);
        vpcsecgroup->iag_pub = sgipaddrgroup;
        vpcsecgroup->midos[VPCSG_IAGPUB] = sgipaddrgroup->obj;
    }

    snprintf(name, 64, "sg_all_%11s", vpcsecgroup->name);
    sgipaddrgroup = mido_get_ipaddrgroup(name);
    if (sgipaddrgroup != NULL) {
        LOGTRACE("Found SG IAG %s\n", sgipaddrgroup->obj->name);
        vpcsecgroup->iag_all = sgipaddrgroup;
        vpcsecgroup->midos[VPCSG_IAGALL] = sgipaddrgroup->obj;
    }

    LOGTRACE("vpc secgroup (%s): AFTER POPULATE\n", vpcsecgroup->name);
    for (i = 0; i < VPCSG_END; i++) {
        if (vpcsecgroup->midos[i]) {
            LOGTRACE("\tmidos[%d]: %d\n", i, vpcsecgroup->midos[i]->init);
        }
        if (!vpcsecgroup->midos[i] || !vpcsecgroup->midos[i]->init) {
            LOGWARN("failed to populate %s midos[%d]\n", vpcsecgroup->name, i);
            vpcsecgroup->population_failed = 1;
            ret = 1;
        }
    }

    return (ret);
}

/**
 * Create necessary objects in MidoNet to implement a Security Group.
 *
 * @param[in] mido data structure that holds MidoNet configuration.
 * @param[in] vpcsecgroup data structure that holds information about the Security Group of interest.
 *
 * @return 0 on success. 1 on any error.
 */
int create_mido_vpc_secgroup(mido_config *mido, mido_vpc_secgroup *vpcsecgroup) {
    int ret = 0;
    char name[32];
    midonet_api_chain *ch = NULL;
    midonet_api_ipaddrgroup *ipag = NULL;

    if (!mido || !vpcsecgroup) {
        return (1);
    }
    snprintf(name, 32, "sg_ingress_%11s", vpcsecgroup->name);
    ch = mido_create_chain(VPCMIDO_TENANT, name, &(vpcsecgroup->midos[VPCSG_INGRESS]));
    if (!ch) {
        LOGWARN("Failed to create chain %s.\n", name);
        ret = 1;
    } else {
        vpcsecgroup->ingress = ch;
    }

    snprintf(name, 32, "sg_egress_%11s", vpcsecgroup->name);
    ch = mido_create_chain(VPCMIDO_TENANT, name, &(vpcsecgroup->midos[VPCSG_EGRESS]));
    if (!ch) {
        LOGWARN("Failed to create chain %s.\n", name);
        ret = 1;
    } else {
        vpcsecgroup->egress = ch;
    }

    snprintf(name, 32, "sg_priv_%11s", vpcsecgroup->name);
    ipag = mido_create_ipaddrgroup(VPCMIDO_TENANT, name, &(vpcsecgroup->midos[VPCSG_IAGPRIV]));
    if (!ipag) {
        LOGWARN("Failed to create ipaddrgroup %s.\n", name);
        ret = 1;
    } else {
        vpcsecgroup->iag_priv = ipag;
    }

    snprintf(name, 32, "sg_pub_%11s", vpcsecgroup->name);
    ipag = mido_create_ipaddrgroup(VPCMIDO_TENANT, name, &(vpcsecgroup->midos[VPCSG_IAGPUB]));
    if (!ipag) {
        LOGWARN("Failed to create ipaddrgroup %s.\n", name);
        ret = 1;
    } else {
        vpcsecgroup->iag_pub = ipag;
    }

    snprintf(name, 32, "sg_all_%11s", vpcsecgroup->name);
    ipag = mido_create_ipaddrgroup(VPCMIDO_TENANT, name, &(vpcsecgroup->midos[VPCSG_IAGALL]));
    if (!ipag) {
        LOGWARN("Failed to create ipaddrgroup %s.\n", name);
        ret = 1;
    } else {
        vpcsecgroup->iag_all = ipag;
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
/*
int delete_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup)
{
    int ret = 0, rc = 0;

    if (!vpcsecgroup) {
        return (1);
    }

    rc = mido_delete_chain(&(vpcsecgroup->midos[VPCSG_INGRESS]));
    if (rc) {
        LOGWARN("Failed to delete chain %s\n", vpcsecgroup->midos[VPCSG_INGRESS].name);
    }
    rc = mido_delete_chain(&(vpcsecgroup->midos[VPCSG_EGRESS]));
    if (rc) {
        LOGWARN("Failed to delete chain %s\n", vpcsecgroup->midos[VPCSG_EGRESS].name);
    }

    rc = mido_delete_ipaddrgroup(&(vpcsecgroup->midos[VPCSG_IAGPRIV]));
    if (rc) {
        LOGWARN("Failed to delete ipaddrgroup %s\n", vpcsecgroup->midos[VPCSG_IAGPRIV].name);
    }
    rc = mido_delete_ipaddrgroup(&(vpcsecgroup->midos[VPCSG_IAGPUB]));
    if (rc) {
        LOGWARN("Failed to delete ipaddrgroup %s\n", vpcsecgroup->midos[VPCSG_IAGPUB].name);
    }
    rc = mido_delete_ipaddrgroup(&(vpcsecgroup->midos[VPCSG_IAGALL]));
    if (rc) {
        LOGWARN("Failed to delete ipaddrgroup %s\n", vpcsecgroup->midos[VPCSG_IAGALL].name);
    }

    free_mido_vpc_secgroup(vpcsecgroup);

    bzero(vpcsecgroup, sizeof(mido_vpc_secgroup));

    return (ret);
}
 */

/**
 * Deletes mido objects of a VPC security group.
 *
 * @param mido [in] data structure holding all discovered MidoNet resources.
 * @param vpcsecgroup [in] security group of interest.
 *
 * @return 0 on success. 1 otherwise.
 */
int delete_mido_vpc_secgroup(mido_config *mido, mido_vpc_secgroup *vpcsecgroup) {
    int ret = 0, rc = 0;

    if ((!vpcsecgroup) || (strlen(vpcsecgroup->name) == 0)) {
        return (1);
    }

    rc += mido_delete_chain(vpcsecgroup->midos[VPCSG_INGRESS]);
    rc += mido_delete_chain(vpcsecgroup->midos[VPCSG_EGRESS]);
    rc += mido_delete_ipaddrgroup(vpcsecgroup->midos[VPCSG_IAGPRIV]);
    rc += mido_delete_ipaddrgroup(vpcsecgroup->midos[VPCSG_IAGPUB]);
    rc += mido_delete_ipaddrgroup(vpcsecgroup->midos[VPCSG_IAGALL]);

    free_mido_vpc_secgroup(vpcsecgroup);
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
int find_mido_vpc_secgroup(mido_config * mido, char *secgroupname, mido_vpc_secgroup ** outvpcsecgroup) {
    int i = 0;

    if (!mido || !secgroupname || !outvpcsecgroup) {
        return (1);
    }

    *outvpcsecgroup = NULL;
    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        if (strlen(mido->vpcsecgroups[i].name) && !strcmp(mido->vpcsecgroups[i].name, secgroupname)) {
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
int free_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup) {
    int ret = 0;

    if (!vpcsecgroup)
        return (0);

    EUCA_FREE(vpcsecgroup->midopresent_privips);
    EUCA_FREE(vpcsecgroup->midopresent_pubips);
    EUCA_FREE(vpcsecgroup->midopresent_allips_pub);
    EUCA_FREE(vpcsecgroup->midopresent_allips_priv);

    bzero(vpcsecgroup, sizeof (mido_vpc_secgroup));

    return (ret);
}

//!
//! Parses the given gni_rule to get its corresponding mido_parsed_chain_rule.
//!
//! @param[in]  mido current mido_config data structure.
//! @param[in]  rule gni_rule of interest.
//! @param[out] parsed_rule data structure to store the parsed results.
//!
//! @return 0 if the parse is successful. 1 otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int parse_mido_secgroup_rule(mido_config *mido, gni_rule *rule, mido_parsed_chain_rule *parsed_rule) {
    char subnet_buf[24];
    char slashnet_buf[8];
    int rc = 0;
    int ret = 0;
    mido_vpc_secgroup *rule_sg = NULL;

    if (!mido || !rule || !parsed_rule) {
        LOGWARN("Invalid argument: cannot parse NULL secgroup.\n");
        return (1);
    }
    LOGTRACE("Parsing secgroup rule\n");

    clear_parsed_chain_rule(parsed_rule);

    // determine if the source is a CIDR or another SG (default CIDR if it is either unset (implying 0.0.0.0) or set explicity)
    if (strlen(rule->groupId)) {
        rc = find_mido_vpc_secgroup(mido, rule->groupId, &rule_sg);
        if (!rc && rule_sg && rule_sg->midos[VPCSG_IAGALL] && rule_sg->midos[VPCSG_IAGALL]->init) {
            LOGTRACE("FOUND SRC IPADDRGROUP MATCH: %s/%s\n", rule_sg->midos[VPCSG_IAGALL]->name, rule_sg->midos[VPCSG_IAGALL]->uuid);
            snprintf(parsed_rule->jsonel[MIDO_CRULE_GRPUUID], 64, "%s", rule_sg->midos[VPCSG_IAGALL]->uuid);
        } else {
            // source SG set, but is not present (no instances membership)
            LOGWARN("%s referenced but not found\n", rule->groupId);
            return (1);
        }
    } else {
        // source SG is not set, default CIDR 0.0.0.0 or set explicitly
        subnet_buf[0] = slashnet_buf[0] = '\0';
        if (strlen(rule->cidr)) {
            cidr_split(rule->cidr, subnet_buf, slashnet_buf, NULL, NULL);
            snprintf(parsed_rule->jsonel[MIDO_CRULE_NW], 64, "%s", subnet_buf);
            snprintf(parsed_rule->jsonel[MIDO_CRULE_NWLEN], 64, "%s", slashnet_buf);
        } else {
            snprintf(parsed_rule->jsonel[MIDO_CRULE_NW], 64, "%s", "0.0.0.0");
            snprintf(parsed_rule->jsonel[MIDO_CRULE_NWLEN], 64, "%s", "0");
        }
    }

    // protocol
    snprintf(parsed_rule->jsonel[MIDO_CRULE_PROTO], 64, "%d", rule->protocol);
    switch (rule->protocol) {
        case 1: // ICMP
            if (rule->icmpType != -1) {
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPS], 64, "jsonjson");
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPS_S], 64, "%d", rule->icmpType);
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPS_E], 64, "%d", rule->icmpType);
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPS_END], 64, "END");
            }
            if (rule->icmpCode != -1) {
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD], 64, "jsonjson");
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_S], 64, "%d", rule->icmpCode);
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_E], 64, "%d", rule->icmpCode);
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_END], 64, "END");
            }
            break;
        case 6:  // TCP
        case 17: // UDP
            snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD], 64, "jsonjson");
            snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_S], 64, "%d", rule->fromPort);
            snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_E], 64, "%d", rule->toPort);
            snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_END], 64, "END");
            break;
        case -1: // All protocols
            snprintf(parsed_rule->jsonel[MIDO_CRULE_PROTO], 64, "0");
            break;
        default:
            // Protocols accepted by EC2 are ICMP/TCP/UDP.
            break;
    }

    return (ret);
}

/**
 * Implements the given mido_parsed_chain_rule in the given chain.
 *
 * @param chain [in] midoname structure of the chain of interest.
 * @param outname [i/o] pointer to an extant MidoNet rule (parameters will be checked
 * to avoid duplicate rule creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created port
 * will not be returned.
 * @param pos position in the chain for the newly created rule. -1 appends the rule.
 * @param ruletype gni_rule type (MIDO_RULE_EGRESS or MIDO_RULE_INGRESS)
 * @param rule mido_parsed_chain_rule to be created.
 *
 * @return 0 if the parse is successful. 1 otherwise.
 */
int create_mido_vpc_secgroup_rule(midonet_api_chain *chain, midoname **outname,
        int pos, int ruletype, mido_parsed_chain_rule *rule) {
    int rc = 0;
    int ret = 0;
    char spos[8];

    if (!chain) {
        LOGWARN("Invalid argument: cannot create secgroup rule in a NULL chain.\n");
        return (1);
    }
    if ((strlen(rule->jsonel[MIDO_CRULE_PROTO]) == 0) || (!strcmp(rule->jsonel[MIDO_CRULE_PROTO], "UNSET"))) {
        LOGWARN("Invalid argument: cannot create secgroup rule with invalid protocol or cidr\n");
        return (1);
    }

    if (pos == -1) {
        pos = chain->rules_count + 1;
    }
    snprintf(spos, 8, "%d", pos);

    switch (ruletype) {
        case MIDO_RULE_SG_EGRESS:
            rc = mido_create_rule(chain, chain->obj, outname, NULL,
                    "position", spos, "type", "accept", "tpDst", rule->jsonel[MIDO_CRULE_TPD],
                    "tpDst:start", rule->jsonel[MIDO_CRULE_TPD_S], "tpDst:end", rule->jsonel[MIDO_CRULE_TPD_E],
                    "tpDst:END", rule->jsonel[MIDO_CRULE_TPD_END], "tpSrc", rule->jsonel[MIDO_CRULE_TPS],
                    "tpSrc:start", rule->jsonel[MIDO_CRULE_TPS_S], "tpSrc:end", rule->jsonel[MIDO_CRULE_TPS_E],
                    "tpSrc:END", rule->jsonel[MIDO_CRULE_TPS_END], "nwProto", rule->jsonel[MIDO_CRULE_PROTO],
                    "ipAddrGroupDst", rule->jsonel[MIDO_CRULE_GRPUUID], "nwDstAddress", rule->jsonel[MIDO_CRULE_NW],
                    "nwDstLength", rule->jsonel[MIDO_CRULE_NWLEN], NULL);
            break;
        case MIDO_RULE_SG_INGRESS:
            rc = mido_create_rule(chain, chain->obj, outname, NULL,
                    "position", spos, "type", "accept", "tpDst", rule->jsonel[MIDO_CRULE_TPD],
                    "tpDst:start", rule->jsonel[MIDO_CRULE_TPD_S], "tpDst:end", rule->jsonel[MIDO_CRULE_TPD_E],
                    "tpDst:END", rule->jsonel[MIDO_CRULE_TPD_END], "tpSrc", rule->jsonel[MIDO_CRULE_TPS],
                    "tpSrc:start", rule->jsonel[MIDO_CRULE_TPS_S], "tpSrc:end", rule->jsonel[MIDO_CRULE_TPS_E],
                    "tpSrc:END", rule->jsonel[MIDO_CRULE_TPS_END], "nwProto", rule->jsonel[MIDO_CRULE_PROTO],
                    "ipAddrGroupDst", rule->jsonel[MIDO_CRULE_GRPUUID], "nwSrcAddress", rule->jsonel[MIDO_CRULE_NW],
                    "nwSrcLength", rule->jsonel[MIDO_CRULE_NWLEN], NULL);
            break;
        case MIDO_RULE_INVALID:
        default:
            LOGWARN("Invalid argument: cannot create invalid secgroup rule.\n");
            rc = 1;
    }
    
    if (rc) {
        ret = 1;
    }
    return (ret);
}

//!
//! Clears the contents of the mido_parsed_chain_rule structure in the argument.
//!
//! @param[in] rule mido_parsed_chain_rule structure of interest.
//!
//! @return 0 on success. 1 otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int clear_parsed_chain_rule(mido_parsed_chain_rule *rule) {
    if (rule == NULL) {
        LOGWARN("Invalid argument: cannot clear NULL mido_parsed_chain_rule.\n");
        return (1);
    }
    for (int i = 0; i < MIDO_CRULE_END; i++) {
        snprintf(rule->jsonel[i], 8, "UNSET");
    }

    return(0);
}

/**
 * Populates an euca VPC instance/interface model.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @param vpc [in] data structure that holds the euca VPC model of interest.
 * @param vpcsubnet [in] data structure that holds the euca VPC subnet model of interest.
 * @param vpc [i/o] data structure that holds the euca VPC instance/interface model of interest.
 * @return 0 on success. 1 on any failure.
 */
int populate_mido_vpc_instance(mido_config *mido, mido_core *midocore, mido_vpc *vpc,
        mido_vpc_subnet *vpcsubnet, mido_vpc_instance *vpcinstance) {
    int ret = 0, rc = 0, found = 0, i = 0, j = 0;
    char *tmpstr = NULL, fstr[64], tmp_name[32];
    char *targetIP = NULL;
    char *rdst = NULL;
    char pubip[16];
    char privip[16];
    char matchStr[64];

    LOGTRACE("populating VPC instance %s\n", vpcinstance->name);
    midonet_api_bridge *subnetbr = vpcsubnet->subnetbr;
    if (subnetbr != NULL) {
        LOGTRACE("Found subnet bridge %s\n", subnetbr->obj->name);
        midoname *instanceport = find_mido_bridge_port_byinterface(subnetbr, vpcinstance->name);
        if (instanceport != NULL) {
            LOGTRACE("Found instance port %s\n", instanceport->name);
            vpcinstance->midos[INST_VPCBR_VMPORT] = instanceport;
            tmpstr = NULL;
            rc = mido_getel_midoname(instanceport, "hostId", &tmpstr);
            midonet_api_host *instancehost = NULL;
            if (!rc && tmpstr && strlen(tmpstr)) {
                instancehost = mido_get_host(NULL, tmpstr);
            }
            if (instancehost != NULL) {
                LOGTRACE("Found instance host %s\n", instancehost->obj->name);
                vpcinstance->midos[INST_VMHOST] = instancehost->obj;
            }
            EUCA_FREE(tmpstr);
        }
    }
    if (vpcinstance->midos[INST_VPCBR_VMPORT] && vpcinstance->midos[INST_VMHOST]) {
        found = 1;
    } else {
        LOGWARN("Unable to populate vpcinstance %s VPCBR_VMPORT and/or VMHOST.\n", vpcinstance->name);
    }

    if (vpcsubnet->subnetbr) {
        found = 0;
        midonet_api_dhcp *dhcp = NULL;
        midoname **dhcphosts = NULL;
        int max_dhcphosts = 0;
        if (vpcsubnet->subnetbr->max_dhcps) {
            dhcp = vpcsubnet->subnetbr->dhcps[0];
            dhcphosts = dhcp->dhcphosts;
            max_dhcphosts = dhcp->max_dhcphosts;
        }
        for (i = 0; i < max_dhcphosts && !found; i++) {
            if (dhcphosts[i] == NULL) {
                continue;
            }
            tmpstr = NULL;
            rc = mido_getel_midoname(dhcphosts[i], "name", &tmpstr);
            if (!rc && tmpstr && strlen(tmpstr) && !strcmp(tmpstr, vpcinstance->name)) {
                LOGTRACE("Found dhcp host %s\n", dhcphosts[i]->name);
                vpcinstance->midos[INST_VPCBR_DHCPHOST] = dhcphosts[i];
                found = 1;
            }
            EUCA_FREE(tmpstr);
        }
    }

    // process public IP
    pubip[0] = '\0';
    snprintf(fstr, 64, "elip_pre_%s", vpcinstance->name);
    midonet_api_ipaddrgroup *ipag = mido_get_ipaddrgroup(fstr);
    if (ipag != NULL) {
        LOGTRACE("Found ipag %s\n", ipag->obj->name);
        vpcinstance->iag_pre = ipag;
        vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP] = ipag->obj;
        if (ipag->ips_count == 1) {
            sscanf(ipag->ips[0]->name, "versions/6/ip_addrs/%s", pubip);
            vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP_IP] = ipag->ips[0];
            vpcinstance->pubip = dot2hex(pubip);
            vpcinstance->iag_pre = ipag;
            found = 0;
            // SNAT rule
            midoname **postchain_rules = NULL;
            int max_postchain_rules = 0;
            if (vpc->rt_uplink_postchain && vpc->rt_uplink_postchain->max_rules) {
                postchain_rules = vpc->rt_uplink_postchain->rules;
                max_postchain_rules = vpc->rt_uplink_postchain->max_rules;
            }
            for (j = 0; j < max_postchain_rules && !found; j++) {
                if (postchain_rules[j] == NULL) {
                    continue;
                }
                rc = mido_getel_midoname(postchain_rules[j], "natTargets", &targetIP);
                snprintf(matchStr, 64, "\"addressTo\": \"%s\"", pubip);
                if (targetIP && strstr(targetIP, matchStr)) {
                    LOGTRACE("Found rule %s\n", postchain_rules[j]->name);
                    vpcinstance->midos[INST_ELIP_POST] = postchain_rules[j];
                    found = 1;
                }
                if (targetIP) EUCA_FREE(targetIP);
            }
            // ELIP route
            if (vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP] && midocore->midos[CORE_EUCART]) {
                midonet_api_router *eucart = midocore->eucart;
                if (eucart != NULL) {
                    found = 0;
                    for (j = 0; j < eucart->max_routes && !found; j++) {
                        if (eucart->routes[j] == NULL) {
                            continue;
                        }
                        rc = mido_getel_midoname(eucart->routes[j], "dstNetworkAddr", &rdst);
                        if (!strcmp(pubip, rdst)) {
                            LOGTRACE("Found route %s\n", eucart->routes[j]->name);
                            vpcinstance->midos[INST_ELIP_ROUTE] = eucart->routes[j];
                            found = 1;
                        }
                        EUCA_FREE(rdst);
                    }
                } else {
                    LOGWARN("Unable to populate instance %s: eucart not found.\n", vpcinstance->name);
                }
            }
        } else {
            LOGDEBUG("Unexpected number of IP addresses (%d) in %s\n", ipag->ips_count, ipag->obj->name);
        }
    }

    // process private IP
    privip[0] = '\0';
    snprintf(fstr, 64, "elip_post_%s", vpcinstance->name);
    ipag = mido_get_ipaddrgroup(fstr);
    if (ipag != NULL) {
        LOGTRACE("Found ipag %s\n", ipag->obj->name);
        vpcinstance->iag_post = ipag;
        vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP] = ipag->obj;
        if (ipag->ips_count == 1) {
            sscanf(ipag->ips[0]->name, "versions/6/ip_addrs/%s", privip);
            LOGTRACE("Found privip %s\n", privip);
            vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP_IP] = ipag->ips[0];
            vpcinstance->privip = dot2hex(privip);
            vpcinstance->iag_post = ipag;
            found = 0;
            // DNAT rule
            midoname **preelipchain_rules = NULL;
            int max_preelipchain_rules = 0;
            if (vpc->rt_preelipchain && vpc->rt_preelipchain->max_rules) {
                preelipchain_rules = vpc->rt_preelipchain->rules;
                max_preelipchain_rules = vpc->rt_preelipchain->max_rules;
            }
            for (j = 0; j < max_preelipchain_rules && !found; j++) {
                if (preelipchain_rules[j] == NULL) {
                    continue;
                }
                rc = mido_getel_midoname(preelipchain_rules[j], "natTargets", &targetIP);
                snprintf(matchStr, 64, "\"addressTo\": \"%s\"", privip);
                if (targetIP && strstr(targetIP, matchStr)) {
                    LOGTRACE("Found rule %s\n", preelipchain_rules[j]->name);
                    vpcinstance->midos[INST_ELIP_PRE] = preelipchain_rules[j];
                    found = 1;
                }
                if (targetIP) EUCA_FREE(targetIP);
            }
        } else {
            LOGDEBUG("Unexpected number of IP addresses (%d) in %s\n", ipag->ips_count, ipag->obj->name);
        }
    }

    if (vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP] && vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP]) {
        found = 1;
    } else {
        LOGWARN("Unable to populate vpcinstance %s IPADDRGROUP\n", vpcinstance->name);
    }

    snprintf(tmp_name, 32, "ic_%s_prechain", vpcinstance->name);
    midonet_api_chain *ic = mido_get_chain(tmp_name);
    if (ic != NULL) {
        LOGTRACE("Found chain %s\n", ic->obj->name);
        vpcinstance->prechain = ic;
        vpcinstance->midos[INST_PRECHAIN] = ic->obj;
    }
    snprintf(tmp_name, 32, "ic_%s_postchain", vpcinstance->name);
    ic = mido_get_chain(tmp_name);
    if (ic != NULL) {
        LOGTRACE("Found chain %s\n", ic->obj->name);
        vpcinstance->postchain = ic;
        vpcinstance->midos[INST_POSTCHAIN] = ic->obj;
    }

    LOGTRACE("vpc instance (%s): AFTER POPULATE\n", vpcinstance->name);
    for (i = 0; i < INST_ELIP_PRE_IPADDRGROUP_IP; i++) {
        if (vpcinstance->midos[i] == NULL) {
            LOGWARN("VPC instance population failed to populate resource at idx %d\n", i);
            vpcinstance->population_failed = 1;
            ret = 1;
        }
    }
    if (vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP_IP]) {
        for (i = INST_ELIP_PRE_IPADDRGROUP_IP; i < INST_END; i++) {
            if (vpcinstance->midos[i] == NULL) {
                LOGWARN("VPC elip instance population failed to populate resource at idx %d\n", i);
                vpcinstance->population_failed = 1;
                ret = 1;
            }
        }
    }

    for (i = 0; i < INST_END; i++) {
        if (vpcinstance->midos[i]) {
            LOGTRACE("\tmidos[%d]: %d\n", i, vpcinstance->midos[i]->init);
        }
    }
    return (ret);
}

/**
 * Creates mido objects that support the implementation of an instance/interface.
 * @param vpcinstance [in] mido_vpc_instance structure that holds information about the
 * instance/interface of interest.
 * @return 0 on success. Positive number otherwise.
 */
int create_mido_vpc_instance(mido_vpc_instance *vpcinstance) {
    int ret = 0;
    char iagname[64], tmp_name[32];
    midonet_api_ipaddrgroup *iag = NULL;
    midonet_api_chain *ch = NULL;

    if (!vpcinstance) {
        LOGWARN("Invalid argument: NULL cannot create_mido_vpc_instance.\n");
    }
    
    // set up elip ipaddrgroups
    snprintf(iagname, 64, "elip_pre_%s", vpcinstance->name);
    iag = mido_create_ipaddrgroup(VPCMIDO_TENANT, iagname, &(vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP]));
    if (iag == NULL) {
        LOGWARN("Failed to create IAG %s.\n", iagname);
        ret++;
    }
    vpcinstance->iag_pre = iag;

    iag = NULL;    
    snprintf(iagname, 64, "elip_post_%s", vpcinstance->name);
    iag = mido_create_ipaddrgroup(VPCMIDO_TENANT, iagname, &(vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP]));
    if (iag == NULL) {
        LOGWARN("Failed to create IAG %s.\n", iagname);
        ret++;
    }
    vpcinstance->iag_post = iag;

    // setup instance chains
    snprintf(tmp_name, 32, "ic_%s_prechain", vpcinstance->name);
    ch = mido_create_chain(VPCMIDO_TENANT, tmp_name, &(vpcinstance->midos[INST_PRECHAIN]));
    if (ch == NULL) {
        LOGWARN("Failed to create chain %s.\n", tmp_name);
        ret++;
    }
    vpcinstance->prechain = ch;

    snprintf(tmp_name, 32, "ic_%s_postchain", vpcinstance->name);
    ch = NULL;
    ch = mido_create_chain(VPCMIDO_TENANT, tmp_name, &(vpcinstance->midos[INST_POSTCHAIN]));
    if (ch == NULL) {
        LOGWARN("Failed to create chain %s.\n", tmp_name);
        ret++;
    }
    vpcinstance->postchain = ch;

    return (ret);
}

/**
 * Deletes all MidoNet objects created to implement the instance/interface in the argument.
 *
 * @param mido [in] data structure holding all discovered MidoNet resources.
 * @param vpc [in] vpc of the interface of interest.
 * @param subnet [in] vpc subnet of the interface of interest.
 * @param vpcinstance [in] data structure holding information about the interface of interest.
 *
 * @return 0 on success. Positive integer if error(s) is/are detected.
 */
int delete_mido_vpc_instance(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *subnet, mido_vpc_instance *vpcinstance) {
    int rc = 0;

    if (!mido || !vpc || !subnet || !vpcinstance || !strlen(vpcinstance->name)) {
        LOGERROR("Invalid argument: cannot delete NULL instance\n");
        return (1);
    }

    if (vpcinstance->pubip != 0) {
        rc += disconnect_mido_vpc_instance_elip(mido, vpc, vpcinstance);
    }
    rc += disconnect_mido_vpc_instance(subnet, vpcinstance);
    rc += mido_delete_ipaddrgroup(vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP]);
    rc += mido_delete_ipaddrgroup(vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP]);
    rc += mido_delete_chain(vpcinstance->midos[INST_PRECHAIN]);
    rc += mido_delete_chain(vpcinstance->midos[INST_POSTCHAIN]);

    free_mido_vpc_instance(vpcinstance);

    return (rc);
}

/**
 * Populate VPC Gateway data structure with information discovered in MidoNet.
 *
 * @param mido [in] data structure holding all discovered MidoNet resources.
 * @param vpc [in] data structure holding information about the VPC associated with the NAT Gateway.
 * @param vpcsubnet [in] data structure holding information about the VPC subnet associated with the NAT Gateway.
 * @param vpcnatgateway [in] data structure holding information about the NAT Gateway of interest.
 *
 * @return 0 on success. 1 on any error.
 */
int populate_mido_vpc_natgateway(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet,
        mido_vpc_natgateway *natgateway) {
    int ret = 0, rc = 0, i = 0, j = 0;
    char *tmpstr = NULL, tmp_name[64];
    char pubip[16];
    int foundcnt = 0;
    boolean found1 = FALSE;
    boolean found2 = FALSE;

    LOGTRACE("populating %s\n", natgateway->name);
    natgateway->midos[NATG_EUCABR] = mido->midocore->eucabr->obj;
    natgateway->midos[NATG_EUCART] = mido->midocore->eucart->obj;
    natgateway->midos[NATG_SUBNBR] = vpcsubnet->subnetbr->obj;
    // Search NAT Gateway Router in MidoNet
    snprintf(tmp_name, 64, "natr_%s", natgateway->name);
    midonet_api_router *router =  mido_get_router(tmp_name);
    if (router != NULL) {
        LOGTRACE("Found router %s\n", router->obj->name);
        natgateway->midos[NATG_RT] = router->obj;
        natgateway->natgrt = router;
        // Search for NATG_RT_UPLINK
        if (mido->midocore->midos[CORE_EUCABR]->init) {
            midoname **rtports = router->ports;
            int max_rtports = router->max_ports;
            midoname **eucabrports = mido->midocore->eucabr->ports;
            int max_eucabrports = mido->midocore->eucabr->max_ports;
            midoname **snbrports = vpcsubnet->subnetbr->ports;
            int max_snbrports = vpcsubnet->subnetbr->max_ports;
            foundcnt = 0;
            for (i = 0; i < max_rtports && (foundcnt != 2); i++) {
                rc = mido_getel_midoname(rtports[i], "peerId", &tmpstr);
                for (j = 0; j < max_eucabrports && !found1; j++) {
                    if (eucabrports[j] == NULL) {
                        continue;
                    }
                    if (!rc && tmpstr && eucabrports[j] && !strcmp(tmpstr, eucabrports[j]->uuid)) {
                        LOGTRACE("Found natr - eucabr link: %s %s\n", rtports[i]->name, eucabrports[j]->name);
                        natgateway->midos[NATG_EUCABR_DOWNLINK] = eucabrports[j];
                        natgateway->midos[NATG_RT_UPLINK] = rtports[i];
                        foundcnt++;
                        found1 = TRUE;
                    }
                }
                // Search for NATG_RT_BRPORT
                for (j = 0; j < max_snbrports && !found2; j++) {
                    if (snbrports[j] == NULL) {
                        continue;
                    }
                    if (!rc && tmpstr && snbrports[j] && !strcmp(tmpstr, snbrports[j]->uuid)) {
                        LOGTRACE("Found natr - subnetbr link: %s %s\n", rtports[i]->name, snbrports[j]->name);
                        natgateway->midos[NATG_SUBNBR_RTPORT] = snbrports[j];
                        natgateway->midos[NATG_RT_BRPORT] = rtports[i];
                        foundcnt++;
                        found2 = TRUE;
                    }
                }
                EUCA_FREE(tmpstr);
            }
        }
    }

    // Search public IP
    pubip[0] = '\0';
    snprintf(tmp_name, 64, "elip_pre_%s", natgateway->name);
    midonet_api_ipaddrgroup *ipag = mido_get_ipaddrgroup(tmp_name);
    if (ipag != NULL) {
        LOGTRACE("Found ipag %s\n", ipag->obj->name);
        natgateway->midos[NATG_ELIP_PRE_IPADDRGROUP] = ipag->obj;
        if (ipag->ips_count == 1) {
            sscanf(ipag->ips[0]->name, "versions/6/ip_addrs/%s", pubip);
            LOGTRACE("Found pubip %s\n", pubip);
            natgateway->midos[NATG_ELIP_PRE_IPADDRGROUP_IP] = ipag->ips[0];
        } else {
            LOGDEBUG("Unexpected number of IP addresses (%d) in %s\n", ipag->ips_count, ipag->obj->name);
        }
    }

    // Search for public IP route
    if ((natgateway->midos[NATG_ELIP_PRE_IPADDRGROUP] && mido->midocore->midos[CORE_EUCART]) &&
            (natgateway->midos[NATG_ELIP_PRE_IPADDRGROUP]->init && mido->midocore->midos[CORE_EUCART]->init)) {
        midonet_api_router *eucart = mido->midocore->eucart;
        if (eucart != NULL) {
            foundcnt = 0;
            for (j = 0; j < eucart->max_routes && !foundcnt; j++) {
                if (eucart->routes[j] == NULL) {
                    continue;
                }
                rc = mido_getel_midoname(eucart->routes[j], "dstNetworkAddr", &tmpstr);
                if (!strcmp(pubip, tmpstr)) {
                    LOGTRACE("Found %s route %s\n", pubip, eucart->routes[j]->name);
                    natgateway->midos[NATG_ELIP_ROUTE] = eucart->routes[j];
                    foundcnt = 1;
                }
                EUCA_FREE(tmpstr);
            }
        } else {
            LOGWARN("Unable to find ELIP route for %s\n", natgateway->name);
        }
    }

    // Search NAT Gateway router chains
    snprintf(tmp_name, 64, "natc_%s_rtin", natgateway->name);
    midonet_api_chain *icin = mido_get_chain(tmp_name);
    if (icin != NULL) {
        LOGTRACE("Found chain %s\n", icin->obj->name);
        natgateway->midos[NATG_RT_INCHAIN] = icin->obj;
        natgateway->inchain = icin;
    }
    snprintf(tmp_name, 64, "natc_%s_rtout", natgateway->name);
    midonet_api_chain *icout = mido_get_chain(tmp_name);
    if (icout != NULL) {
        LOGTRACE("Found chain %s\n", icout->obj->name);
        natgateway->midos[NATG_RT_OUTCHAIN] = icout->obj;
        natgateway->outchain = icout;
    }

    LOGTRACE("(%s): AFTER POPULATE\n", natgateway->name);
    for (i = 0; i < NATG_END; i++) {
        if (natgateway->midos[i]) {
            LOGTRACE("\tmidos[%d]: %d\n", i, natgateway->midos[i]->init);
        }
        if ((!natgateway->midos[i]) || (!natgateway->midos[i]->init)) {
            LOGWARN("Failed to populate %s midos[%d]\n", natgateway->name, i);
            natgateway->population_failed = 1;
            ret = 1;
        }
    }
    if (ret == 1) {
        // Cleanup partial NAT Gateway
        LOGINFO("\tdeleting %s\n", natgateway->name);
        rc = delete_mido_vpc_natgateway(mido, vpcsubnet, natgateway);
    }
    return (ret);
}

/**
 * Create necessary objects in MidoNet to implement a VPC NAT Gateway.
 *
 * @param mido [in] data structure that holds MidoNet configuration.
 * @param vpc [in] data structure that holds information about the VPC associated with the NAT Gateway.
 * @param vpcsubnet [in] data structure that holds information about the VPC subnet associated with the NAT Gateway.
 * @param vpcnatgateway [in] data structure holding information about the NAT Gateway of interest.
 *
 * @return 0 on success. 1 on any error.
 *
 * @note Minimal checks for duplicates when creating MidoNet artifacts. Incomplete
 *       NAT gateways are deleted in populate phase.
 */
int create_mido_vpc_natgateway(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, mido_vpc_natgateway *vpcnatgateway) {
    int rc = 0;
    int ret = 0;
    int i = 0;
    char mido_rtnet[INET_ADDR_LEN];
    char mido_rtmask[INET_ADDR_LEN];
    char mido_rtip[INET_ADDR_LEN];
    char natg_rtip[INET_ADDR_LEN];
    char vpc_net[INET_ADDR_LEN];
    char vpc_mask[INET_ADDR_LEN];
    char subnet_net[INET_ADDR_LEN];
    char subnet_mask[INET_ADDR_LEN];
    char subnet_gwip[INET_ADDR_LEN];
    char natg_subnetip[INET_ADDR_LEN];
    char natg_subnetmac[ENET_ADDR_LEN];
    char natg_pubip[INET_ADDR_LEN];
    char natg_rtname[64];
    char tmpbuf[64];
    char iagname[64];
    char *tmpstr = NULL;

    if (!mido || !vpc || !vpcsubnet || !vpcnatgateway) {
        LOGERROR("Invalid argument - cannot create NULL vpc gateway.\n");
        return (1);
    }
    if ((vpcnatgateway->gniNatGateway == NULL) || (vpcnatgateway->gniVpcSubnet == NULL) || (vpcnatgateway->rtid == 0)) {
        LOGERROR("Invalid argument: cannot proceed without VPC NAT gateway information from GNI.\n");
        return (1);
    }

    tmpstr = hex2dot(mido->int_rtnw);
    snprintf(mido_rtnet, INET_ADDR_LEN, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    snprintf(mido_rtmask, INET_ADDR_LEN, "%d", mido->int_rtsn);

    tmpstr = hex2dot(mido->int_rtnw + vpcnatgateway->rtid);
    snprintf(natg_rtip, INET_ADDR_LEN, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    tmpstr = hex2dot(mido->int_rtaddr);
    snprintf(mido_rtip, INET_ADDR_LEN, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    cidr_split(vpc->gniVpc->cidr, vpc_net, vpc_mask, NULL, NULL);
    cidr_split(vpcnatgateway->gniVpcSubnet->cidr, subnet_net, subnet_mask, subnet_gwip, NULL);
    
    tmpstr = hex2dot(vpcnatgateway->gniNatGateway->privateIp);
    snprintf(natg_subnetip, INET_ADDR_LEN, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    hex2mac(vpcnatgateway->gniNatGateway->macAddress, &tmpstr);
    snprintf(natg_subnetmac, ENET_ADDR_LEN, "%s", tmpstr);
    EUCA_FREE(tmpstr);
    for (i = 0; i < strlen(natg_subnetmac); i++) {
        natg_subnetmac[i] = tolower(natg_subnetmac[i]);
    }

    tmpstr = hex2dot(vpcnatgateway->gniNatGateway->publicIp);
    snprintf(natg_pubip, INET_ADDR_LEN, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    // Create the NAT Gateway mido router
    snprintf(natg_rtname, 64, "natr_%s_%s_%d", vpcnatgateway->name, vpcsubnet->name, vpcnatgateway->rtid);
    vpcnatgateway->natgrt = mido_create_router(VPCMIDO_TENANT, natg_rtname, &(vpcnatgateway->midos[NATG_RT]));
    if (vpcnatgateway->natgrt == NULL) {
        LOGERROR("Failed to create %s: check midonet health\n", natg_rtname);
        ret = 1;
    }

    if (ret == 0) {
        // link the NAT Gateway and euca network
        //    Create an eucabr port (downlink to nr)
        rc = mido_create_bridge_port(mido->midocore->eucabr, mido->midocore->midos[CORE_EUCABR], &(vpcnatgateway->midos[NATG_EUCABR_DOWNLINK]));
        if (rc) {
            LOGERROR("cannot create core bridge port: check midonet health\n");
            ret = 1;
        }

        //    Create an nr port (uplink to eucabr)
        rc = mido_create_router_port(vpcnatgateway->natgrt, vpcnatgateway->midos[NATG_RT], natg_rtip,
                mido_rtnet, mido_rtmask, NULL, &(vpcnatgateway->midos[NATG_RT_UPLINK]));
        if (rc) {
            LOGERROR("cannot create nat gateway router port: check midonet health\n");
            ret = 1;
        }

        //    Link nr with eucabr
        rc = mido_link_ports(vpcnatgateway->midos[NATG_EUCABR_DOWNLINK], vpcnatgateway->midos[NATG_RT_UPLINK]);
        if (rc) {
            LOGERROR("cannot create midonet bridge <-> nat gateway router link: check midonet health\n");
            ret = 1;
        }
    }

    if (ret == 0) {
        // link the NAT Gateway and VPC subnet
        //     create an nr subnet port
        rc = mido_create_router_port(vpcnatgateway->natgrt, vpcnatgateway->midos[NATG_RT], natg_subnetip,
                subnet_net, subnet_mask, natg_subnetmac, &(vpcnatgateway->midos[NATG_RT_BRPORT]));
        if (rc) {
            LOGERROR("cannot create nat gateway subnet port: check midonet health\n");
            ret = 1;
        }

        //     create an subnetbr port
        rc = mido_create_bridge_port(vpcsubnet->subnetbr, vpcsubnet->midos[SUBN_BR], &(vpcnatgateway->midos[NATG_SUBNBR_RTPORT]));
        if (rc) {
            LOGERROR("cannot create subnet bridge port: check midonet health\n");
            ret = 1;
        }

        //     link nr subnet port with subnetbr port
        rc = mido_link_ports(vpcnatgateway->midos[NATG_RT_BRPORT], vpcnatgateway->midos[NATG_SUBNBR_RTPORT]);
        if (rc) {
            LOGERROR("cannot create nat gateway router <-> subnet bridge link: check midonet health\n");
            ret = 1;
        }
    }

    if (ret == 0) {
        // Route the NAT gateway public IP from the Internet to nr
        rc = mido_create_route(mido->midocore->eucart, mido->midocore->midos[CORE_EUCART], mido->midocore->midos[CORE_EUCART_BRPORT],
                "0.0.0.0", "0", natg_pubip, "32", natg_rtip, "100", &(vpcnatgateway->midos[NATG_ELIP_ROUTE]));
        if (rc) {
            LOGERROR("cannot create nat gateway elip route: check midonet health\n");
            ret = 1;
        }

        // Default route to eucart
        rc = mido_create_route(vpcnatgateway->natgrt, vpcnatgateway->midos[NATG_RT], vpcnatgateway->midos[NATG_RT_UPLINK],
                "0.0.0.0", "0", "0.0.0.0", "0", mido_rtip, "0", NULL);
        if (rc) {
            LOGERROR("cannot create nat gateway default route: check midonet health\n");
            ret = 1;
        }

        // Route to euca internal network
        rc = mido_create_route(vpcnatgateway->natgrt, vpcnatgateway->midos[NATG_RT], vpcnatgateway->midos[NATG_RT_UPLINK],
                "0.0.0.0", "0", mido_rtnet, mido_rtmask, "UNSET", "0", NULL);
        if (rc) {
            LOGERROR("cannot create midonet router route: check midonet health\n");
            return (1);
        }

        // Route traffic destined to VPC CIDR block to VPC Router
        rc = mido_create_route(vpcnatgateway->natgrt, vpcnatgateway->midos[NATG_RT], vpcnatgateway->midos[NATG_RT_BRPORT],
                "0.0.0.0", "0", vpc_net, vpc_mask, subnet_gwip, "0", NULL);
        if (rc) {
            LOGERROR("cannot create midonet router route: check midonet health\n");
            ret = 1;
        }
    }

    midonet_api_chain *ch = NULL;
    if (ret == 0) {
        // create nat gateway router chains
        snprintf(tmpbuf, 64, "natc_%s_rtin", vpcnatgateway->name);
        ch = mido_create_chain(VPCMIDO_TENANT, tmpbuf, &(vpcnatgateway->midos[NATG_RT_INCHAIN]));
        if (!ch) {
            LOGWARN("Failed to create chain %s.\n", tmpbuf);
            ret = 1;
        } else {
            vpcnatgateway->inchain = ch;
        }

        snprintf(tmpbuf, 64, "natc_%s_rtout", vpcnatgateway->name);
        ch = mido_create_chain(VPCMIDO_TENANT, tmpbuf, &(vpcnatgateway->midos[NATG_RT_OUTCHAIN]));
        if (!ch) {
            LOGWARN("Failed to create chain %s.\n", tmpbuf);
            ret = 1;
        } else {
            vpcnatgateway->outchain = ch;
        }

        // set NATG_RT_INCHAIN and NATG_RT_OUTCHAIN to NATG_RT
        if ((vpcnatgateway->midos[NATG_RT] && vpcnatgateway->midos[NATG_RT_INCHAIN] && vpcnatgateway->midos[NATG_RT_OUTCHAIN]) &&
                (vpcnatgateway->midos[NATG_RT]->init && vpcnatgateway->midos[NATG_RT_INCHAIN]->init && vpcnatgateway->midos[NATG_RT_OUTCHAIN]->init)) {
            rc = mido_update_router(vpcnatgateway->midos[NATG_RT], "inboundFilterId", vpcnatgateway->midos[NATG_RT_INCHAIN]->uuid,
                    "outboundFilterId", vpcnatgateway->midos[NATG_RT_OUTCHAIN]->uuid, "name", vpcnatgateway->midos[NATG_RT]->name, NULL);
        } else {
            rc = 1;
        }
        if (rc > 0) {
            LOGERROR("cannot update router infilter and/or outfilter: check midonet health\n");
            ret = 1;
        }

        // Create SNAT rule (post-routing/out)
        snprintf(tmpbuf, 8, "0");
        if ((vpcnatgateway->midos[NATG_RT_OUTCHAIN]) && (vpcnatgateway->midos[NATG_RT_OUTCHAIN]->init)) {
            rc = mido_create_rule(vpcnatgateway->outchain, vpcnatgateway->midos[NATG_RT_OUTCHAIN], NULL, NULL,
                    "position", tmpbuf, "type", "snat", "nwSrcAddress", vpc_net, "nwSrcLength", vpc_mask,
                    "invNwSrc", "false", "flowAction", "accept", "natTargets", "jsonlist",
                    "natTargets:addressFrom", natg_pubip, "natTargets:addressTo", natg_pubip,
                    "natTargets:portFrom", "1024", "natTargets:portTo", "65535", "natTargets:END", "END",
                    "outPorts", "jsonarr", "outPorts:", vpcnatgateway->midos[NATG_RT_UPLINK]->uuid, "outPorts:END",
                    "END", "invOutPorts", "false", NULL);
            if (rc) {
                LOGWARN("Failed to create SNAT rule for %s\n", vpcnatgateway->name);
                ret = 1;
            }
        }

        // Create REV_SNAT rule (pre-routing/in)
        if ((vpcnatgateway->midos[NATG_RT_INCHAIN]) && (vpcnatgateway->midos[NATG_RT_INCHAIN]->init)) {
            rc = mido_create_rule(vpcnatgateway->inchain, vpcnatgateway->midos[NATG_RT_INCHAIN], NULL, NULL,
                    "position", tmpbuf, "type", "rev_snat", "nwDstAddress", natg_pubip, "nwDstLength", "32",
                    "invNwDst", "false", "flowAction", "accept", NULL);
            if (rc) {
                LOGWARN("Failed to create REV_SNAT rule for %s\n", vpcnatgateway->name);
            }
        }
    }

    midonet_api_ipaddrgroup *ig = NULL;
    if (ret == 0) {
        snprintf(iagname, 64, "elip_pre_%s", vpcnatgateway->name);
        ig = mido_create_ipaddrgroup(VPCMIDO_TENANT, iagname, &(vpcnatgateway->midos[NATG_ELIP_PRE_IPADDRGROUP]));
        if (!ig) {
            LOGWARN("Failed to create IAG %s.\n", iagname);
            ret = 1;
        }

        if (vpcnatgateway->midos[NATG_ELIP_PRE_IPADDRGROUP] && vpcnatgateway->midos[NATG_ELIP_PRE_IPADDRGROUP]->init) {
            rc = mido_create_ipaddrgroup_ip(ig, vpcnatgateway->midos[NATG_ELIP_PRE_IPADDRGROUP],
                    natg_pubip, &(vpcnatgateway->midos[NATG_ELIP_PRE_IPADDRGROUP_IP]));
            if (rc) {
                LOGERROR("Failed to add %s as member of %s\n", natg_pubip, vpcnatgateway->midos[NATG_ELIP_PRE_IPADDRGROUP]->name);
                ret = 1;
            }
        }
    }

    return (ret);
}

/**
 * Deletes all MidoNet objects created to implement the NAT gateway in the argument.
 *
 * @param mido [in] data structure holding all discovered MidoNet resources.
 * @param vpcsubnet [in] data structure that holds information about the VPC subnet associated with the NAT Gateway.
 * @param vpcnatgateway [in] data structure holding information about the NAT Gateway of interest.
 *
 * @return 0 on success. 1 on any error.
 */
int delete_mido_vpc_natgateway(mido_config *mido, mido_vpc_subnet *vpcsubnet, mido_vpc_natgateway *vpcnatgateway) {
    int ret = 0, rc = 0;

    if (!mido || !vpcnatgateway || !strlen(vpcnatgateway->name)) {
        LOGFATAL("Invalid argument: cannot delete a NULL natgateway\n");
        return (1);
    }

    // Delete NAT Gateway outfilter
    rc += mido_delete_chain(vpcnatgateway->midos[NATG_RT_OUTCHAIN]);
    // Delete NAT Gateway infilter
    rc += mido_delete_chain(vpcnatgateway->midos[NATG_RT_INCHAIN]);
    // Delete ELIP route from eucart
    rc += mido_delete_route(mido->midocore->eucart, vpcnatgateway->midos[NATG_ELIP_ROUTE]);
    // Delete ELIP ip-address-group
    rc += mido_delete_ipaddrgroup(vpcnatgateway->midos[NATG_ELIP_PRE_IPADDRGROUP]);
    // Delete VPC bridge port
    rc += mido_delete_bridge_port(vpcsubnet->subnetbr, vpcnatgateway->midos[NATG_SUBNBR_RTPORT]);
    // Delete eucabr port
    rc += mido_delete_bridge_port(mido->midocore->eucabr, vpcnatgateway->midos[NATG_EUCABR_DOWNLINK]);
    // Delete NAT Gateway router
    rc += mido_delete_router(vpcnatgateway->midos[NATG_RT]);

    clear_router_id(mido, vpcnatgateway->rtid);
    free_mido_vpc_natgateway(vpcnatgateway);
    if (rc != 0) {
        ret = 1;
    }
    return (ret);
}

//!
//!
//!
//! @param[in] mido
//! @param[in] eucahome
//! @param[in] setupcore
//! @param[in] ext_eucanetdhostname
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
int initialize_mido(mido_config * mido, char *eucahome, int flushmode, int disable_l2_isolation, char *ext_eucanetdhostname, char *ext_rthosts, char *ext_pubnw,
        char *ext_pubgwip, char *int_rtnetwork, char *int_rtslashnet) {
    int ret = 0;

    if (!mido ||
            !eucahome ||
            !ext_eucanetdhostname ||
            !ext_pubnw ||
            !ext_pubgwip ||
            !int_rtnetwork ||
            !int_rtslashnet ||
            !ext_rthosts ||
            !strlen(ext_eucanetdhostname) ||
            !strlen(ext_pubnw) || !strlen(ext_pubgwip) || !strlen(int_rtnetwork) || !strlen(int_rtslashnet) || !strlen(ext_rthosts))
        return (1);

    bzero(mido, sizeof (mido_config));

    mido->eucahome = strdup(eucahome);

    mido->flushmode = flushmode;

    mido->disable_l2_isolation = disable_l2_isolation;

    mido->ext_eucanetdhostname = strdup(ext_eucanetdhostname);

    char *toksA[32], *toksB[3];
    int numtoksA = 0, numtoksB = 0, i = 0, idx = 0;

    numtoksA = euca_tokenizer(ext_rthosts, " ", toksA, 32);
    for (i = 0; i < numtoksA; i++) {
        numtoksB = euca_tokenizer(toksA[i], ",", toksB, 3);
        if (numtoksB == 3) {
            mido->ext_rthostnamearr[idx] = strdup(toksB[0]);
            mido->ext_rthostaddrarr[idx] = strdup(toksB[1]);
            mido->ext_rthostifacearr[idx] = strdup(toksB[2]);
            EUCA_FREE(toksB[0]);
            EUCA_FREE(toksB[1]);
            EUCA_FREE(toksB[2]);
            idx++;
        }
        EUCA_FREE(toksA[i]);
    }
    mido->ext_rthostarrmax = idx;

    for (i = 0; i < mido->ext_rthostarrmax; i++) {
        LOGDEBUG("parsed mido GW host information: GW id=%d: %s/%s/%s\n", i, mido->ext_rthostnamearr[i], mido->ext_rthostaddrarr[i], mido->ext_rthostifacearr[i]);
    }

    mido->ext_pubnw = strdup(ext_pubnw);
    mido->ext_pubgwip = strdup(ext_pubgwip);
    mido->int_rtnw = dot2hex(int_rtnetwork); // strdup(int_rtnetwork);
    mido->int_rtsn = atoi(int_rtslashnet);
    mido->int_rtaddr = mido->int_rtnw + 1;
    mido->midocore = EUCA_ZALLOC(1, sizeof (mido_core));
    LOGDEBUG("mido initialized: mido->ext_eucanetdhostname=%s mido->ext_pubnw=%s mido->ext_pubgwip=%s int_rtcidr=%s/%s \n",
            SP(mido->ext_eucanetdhostname), SP(mido->ext_pubnw), SP(mido->ext_pubgwip), SP(int_rtnetwork),
            SP(int_rtslashnet));

    midonet_api_system_changed = 1;

    return (ret);
}

int reinitialize_mido(mido_config *mido) {
    LOGDEBUG("Clearing current mido config.\n");
    clear_mido_config(mido);
    return (0);
}

//!
//! Clear all gnipresent tags of discovered/populated MidoNet resources.
//!
//! @param[in] mido data structure holding all discovered MidoNet resources.
//!
//! @return always returns 0.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int clear_mido_gnitags(mido_config *mido) {
    int i = 0, j = 0, k = 0;
    mido_vpc *vpc = NULL;
    mido_vpc_subnet *subnet = NULL;
    mido_vpc_instance *instance = NULL;
    mido_vpc_secgroup *sg = NULL;

    // go through vpcs
    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        // for each VPC, go through subnets
        for (j = 0; j < vpc->max_subnets; j++) {
            subnet = &(vpc->subnets[j]);
            // for each subnet, go through instances
            for (k = 0; k < subnet->max_instances; k++) {
                instance = &(subnet->instances[k]);
                instance->gnipresent = 0;
                instance->midopresent = 0;
                instance->srcdst_changed = 0;
                instance->pubip_changed = 0;
                instance->host_changed = 0;
                //instance->gniInst = NULL;
            }
            subnet->gnipresent = 0;
            subnet->midopresent = 0;
            //subnet->gniSubnet = NULL;
        }
        // go through NAT Gateways
        for (j = 0; j < vpc->max_natgateways; j++) {
            vpc->natgateways[j].gnipresent = 0;
            vpc->natgateways[j].midopresent = 0;
        }
        vpc->gnipresent = 0;
        vpc->midopresent = 0;
        //vpc->gniVpc = NULL;
    }
    // go through security groups
    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        sg = &(mido->vpcsecgroups[i]);
        sg->gnipresent = 0;
        sg->midopresent = 0;
        //sg->gniSecgroup = NULL;
    }
    return (0);
}

//!
//! Check if VPCMIDO tunnel-zone (assumed to be "mido-tz") exists.
//!
//! @param[in] mido data structure holding MidoNet configuration.
//!
//! @return 0 if mido-tz is found and has at least 1 member. 1 otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int check_mido_tunnelzone() {
    int rc = 0;
    int i = 0;
    int ret = 1;
    int max_tzs = 0;
    int max_tzhosts = 0;
    midoname **tzs = NULL;
    char *tztype = NULL;
    midoname **tzhosts = NULL;

    rc = mido_get_tunnelzones(VPCMIDO_TENANT, &tzs, &max_tzs);
    if (rc == 0) {
        for (i = 0; i < max_tzs; i++) {
            rc = mido_getel_midoname(tzs[i], "type", &tztype);
            if ((rc == 0) && (strstr(VPCMIDO_TUNNELZONE, tzs[i]->name))) {
                rc = mido_get_tunnelzone_hosts(tzs[i], &tzhosts, &max_tzhosts);
                if ((rc == 0) && (max_tzhosts > 0)) {
                    LOGINFO("Found %s tunnel-zone %s with %d members\n", tztype, tzs[i]->name, max_tzhosts);
                    ret = 0;
                }
                if (tzhosts) {
                    EUCA_FREE(tzhosts);
                    tzhosts = NULL;
                }
            }
            if (tztype) {
                EUCA_FREE(tztype);
                tztype = NULL;
            }
        }
    } else {
        LOGWARN("Failed to retrieve MidoNet tunnel-zones.\n");
    }

    if (tzs) {
        EUCA_FREE(tzs);
    }

    return (ret);
}

/**
 * Searches for BGP configuration in midocore, and returns a string containing midonet-cli
 * commands to recover the BGP configuration.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return string containing midonet-cli commands to recover the found BGP configuration.
 * NULL if BGP is not found.
 */
char *discover_mido_bgps(mido_config *mido) {
    int rc = 0;
    midoname **bgps = NULL;
    int max_bgps = 0;
    midoname **bgp_routes = NULL;
    int max_bgp_routes = 0;
    char *localAS = NULL;
    char *peerAddr = NULL;
    char *peerAS = NULL;
    char *nwPrefix = NULL;
    char *prefixLength = NULL;
    char *res = NULL;
    char *resptr = NULL;
    char *interfaceName = NULL;
    char *networkAddress = NULL;
    char *networkLength = NULL;
    char *portAddress = NULL;
    char *portMac = NULL;
    if (!mido || !mido->midocore) {
        return (NULL);
    }
    res = EUCA_ZALLOC_C(2048, sizeof (char));
    resptr = &(res[0]);
    mido_core *midocore = mido->midocore;
    for (int i = 0; i < midocore->max_gws; i++) {
        mido_getel_midoname(midocore->gwports[i], "interfaceName", &interfaceName);
        mido_getel_midoname(midocore->gwports[i], "networkAddress", &networkAddress);
        mido_getel_midoname(midocore->gwports[i], "networkLength", &networkLength);
        mido_getel_midoname(midocore->gwports[i], "portAddress", &portAddress);
        mido_getel_midoname(midocore->gwports[i], "portMac", &portMac);
        if (!interfaceName || !networkAddress || !networkLength || !portAddress || !portMac) {
            LOGWARN("failed to retrieve gateway port information\n");
            continue;
        }
        resptr = &(res[strlen(res)]);
        snprintf(resptr, 2048, "$GW[%d] if %s %s %s/%s %s\n", i, interfaceName, portAddress, networkAddress, networkLength, portMac);
        EUCA_FREE(interfaceName);
        EUCA_FREE(networkAddress);
        EUCA_FREE(networkLength);
        EUCA_FREE(portAddress);
        EUCA_FREE(portMac);
        bgps = NULL;
        rc = mido_get_bgps(midocore->gwports[i], &bgps, &max_bgps);
        if (!rc && bgps) {
            if (max_bgps > 1) {
                LOGWARN("Unexpected number (%d) of bgps found on port %s\n", max_bgps, midocore->gwports[i]->name);
            }
            if (max_bgps == 0) {
                LOGWARN("BGP is not configured on port %s\n", midocore->gwports[i]->name);
            } else {
                mido_getel_midoname(bgps[0], "localAS", &localAS);
                mido_getel_midoname(bgps[0], "peerAddr", &peerAddr);
                mido_getel_midoname(bgps[0], "peerAS", &peerAS);
                if (!localAS || !peerAS || !peerAS) {
                    LOGWARN("failed to retrieve bgp information from port %s\n", midocore->gwports[i]->name);
                    continue;
                }
                resptr = &(res[strlen(res)]);
                snprintf(resptr, 2048, "router EUCART port GW[%d] add bgp local-AS %s peer-AS %s peer %s\n", i, localAS, peerAS, peerAddr);
                rc = mido_get_bgp_routes(bgps[0], &bgp_routes, &max_bgp_routes);
                if (!rc && bgp_routes && max_bgp_routes) {
                    for (int j = 0; j < max_bgp_routes; j++) {
                        mido_getel_midoname(bgp_routes[j], "nwPrefix", &nwPrefix);
                        mido_getel_midoname(bgp_routes[j], "prefixLength", &prefixLength);
                        if (!nwPrefix || !prefixLength) {
                            LOGWARN("failed to retrieve bgp routes from %s\n", bgps[0]->uuid);
                            continue;
                        }
                        resptr = &(res[strlen(res)]);
                        snprintf(resptr, 2048, "router EUCART port GW[%d] bgp bgp0 add route net %s/%s\n", i, nwPrefix, prefixLength);
                        EUCA_FREE(nwPrefix);
                        EUCA_FREE(prefixLength);
                    }
                } else {
                    LOGWARN("routes not found for bgp %s\n", bgps[0]->uuid);
                }
                EUCA_FREE(bgp_routes);
                EUCA_FREE(localAS);
                EUCA_FREE(peerAddr);
                EUCA_FREE(peerAS);
            }
        }
        EUCA_FREE(bgps);
    }
    return (res);
}

/**
 * Populates an euca VPC subnet model.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param vpc [in] data structure that holds the euca VPC model of interest.
 * @param vpcsubnet [i/o] data structure that holds the euca VPC subnet model of interest.
 * @return 0 on success. 1 on any failure.
 */
int populate_mido_vpc_subnet(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet) {
    int rc = 0, ret = 0, i = 0, j = 0, found = 0;
    char name[64];
    char *tmpstr = NULL;

    if (mido->midocore->eucanetdhost) {
        vpcsubnet->midos[SUBN_BR_METAHOST] = mido->midocore->eucanetdhost->obj;
    }

    snprintf(name, 64, "vb_%s_%s", vpc->name, vpcsubnet->name);
    midonet_api_bridge *subnetbridge = mido_get_bridge(name);
    if (subnetbridge != NULL) {
        LOGTRACE("Found bridge %s\n", subnetbridge->obj->name);
        vpcsubnet->midos[SUBN_BR] = subnetbridge->obj;
        vpcsubnet->subnetbr = subnetbridge;

        if (subnetbridge->max_dhcps) {
            LOGTRACE("%d dhcp for bridge %s\n", subnetbridge->max_dhcps, subnetbridge->obj->name);
            vpcsubnet->midos[SUBN_BR_DHCP] = subnetbridge->dhcps[0]->obj;
        }
    }

    if ((subnetbridge) && (vpcsubnet->midos[SUBN_BR]) && (vpcsubnet->midos[SUBN_BR]->init)) {
        midoname **brports = subnetbridge->ports;
        int max_brports = subnetbridge->max_ports;
        midoname **rtports = vpc->vpcrt->ports;
        int max_rtports = vpc->vpcrt->max_ports;
        found = 0;
        for (i = 0; i < max_brports && !found; i++) {
            if (brports[i] == NULL) {
                continue;
            }
            for (j = 0; j < max_rtports && !found; j++) {
                if (rtports[j] == NULL) {
                    continue;
                }
                tmpstr = NULL;
                rc = mido_getel_midoname(rtports[j], "peerId", &tmpstr);
                if (!rc && tmpstr) {
                    if (!strcmp(tmpstr, brports[i]->uuid)) {
                        LOGTRACE("Found rt-br link %s %s", rtports[j]->name, brports[i]->name);
                        vpcsubnet->midos[SUBN_BR_RTPORT] = brports[i];
                        vpcsubnet->midos[SUBN_VPCRT_BRPORT] = rtports[j];
                        found = 1;
                    }
                }
                EUCA_FREE(tmpstr);
            }
        }

        found = 0;
        for (i = 0; i < max_brports && !found; i++) {
            if (brports[i] == NULL) {
                continue;
            }
            rc = mido_getel_midoname(brports[i], "interfaceName", &tmpstr);
            if (!rc && tmpstr && strlen(tmpstr) && strstr(tmpstr, "vn0_")) {
                // found the meta iface
                LOGTRACE("Found meta interface %s", brports[i]->name);
                vpcsubnet->midos[SUBN_BR_METAPORT] = brports[i];
                found = 1;
            }
            EUCA_FREE(tmpstr);
        }

        // populate vpcsubnet routes
        rc = find_mido_vpc_subnet_routes(mido, vpc, vpcsubnet);
        if (rc != 0) {
            LOGWARN("VPC subnet population failed to populate route table.\n");
        }
    }

    for (i = 0; i < SUBN_END; i++) {
        if (vpcsubnet->midos[i] == NULL) {
            LOGWARN("VPC subnet population failed to populate resource at idx %d\n", i);
            vpcsubnet->population_failed = 1;
        }
    }

    LOGTRACE("vpc subnet (%s): AFTER POPULATE\n", vpcsubnet->name);
    for (i = 0; i < SUBN_END; i++) {
        if (vpcsubnet->midos[i]) {
            LOGTRACE("\tmidos[%d]: %d\n", i, vpcsubnet->midos[i]->init);
        }
    }

    return (ret);
}

/**
 * Populates an euca VPC model.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @param vpc [i/o] data structure that holds the euca VPC model of interest.
 * @return 0 on success. 1 on any failure.
 */
int populate_mido_vpc(mido_config *mido, mido_core *midocore, mido_vpc *vpc) {
    int rc = 0, ret = 0, i = 0, j = 0;
    char *url = NULL, vpcname[32];

    snprintf(vpcname, 32, "vr_%s", vpc->name);
    midonet_api_router *vpcrt = mido_get_router(vpcname);
    if (vpcrt != NULL) {
        LOGTRACE("Found vpcrt %s\n", vpcrt->obj->name);
        vpc->vpcrt = vpcrt;
        vpc->midos[VPC_VPCRT] = vpcrt->obj;
    }

    midonet_api_chain *chain = NULL;

    snprintf(vpcname, 32, "vc_%s_prechain", vpc->name);
    chain = mido_get_chain(vpcname);
    if (chain != NULL) {
        LOGTRACE("Found chain %s", chain->obj->name);
        vpc->rt_uplink_prechain = chain;
        vpc->midos[VPC_VPCRT_UPLINK_PRECHAIN] = chain->obj;
    }

    snprintf(vpcname, 32, "vc_%s_preelip", vpc->name);
    chain = mido_get_chain(vpcname);
    if (chain != NULL) {
        LOGTRACE("Found chain %s", chain->obj->name);
        vpc->rt_preelipchain = chain;
        vpc->midos[VPC_VPCRT_PREELIPCHAIN] = chain->obj;
    }

    snprintf(vpcname, 32, "vc_%s_postchain", vpc->name);
    chain = mido_get_chain(vpcname);
    if (chain != NULL) {
        LOGTRACE("Found chain %s", chain->obj->name);
        vpc->rt_uplink_postchain = chain;
        vpc->midos[VPC_VPCRT_UPLINK_POSTCHAIN] = chain->obj;
    }

    int found = 0;
    if ((vpc->vpcrt) && (midocore->midos[CORE_EUCABR]) && (midocore->midos[CORE_EUCABR]->init)) {
        midoname **brports = midocore->eucabr->ports;
        int max_brports = midocore->eucabr->max_ports;
        midoname **rtports = vpc->vpcrt->ports;
        int max_rtports = vpc->vpcrt->max_ports;
        for (i = 0; i < max_brports && !found; i++) {
            if (brports[i] == NULL) {
                continue;
            }
            for (j = 0; j < max_rtports && !found; j++) {
                if (rtports[j] == NULL) {
                    continue;
                }
                rc = mido_getel_midoname(rtports[j], "peerId", &url);
                if (!rc && url) {
                    if (!strcmp(url, brports[i]->uuid)) {
                        LOGTRACE("Found rt-br link %s %s", rtports[j]->name, brports[i]->name);
                        vpc->midos[VPC_EUCABR_DOWNLINK] = brports[i];
                        vpc->midos[VPC_VPCRT_UPLINK] = rtports[j];
                        found = 1;
                    }
                }
                EUCA_FREE(url);
            }
        }
    }

    for (i = 0; i < VPC_END; i++) {
        if (vpc->midos[i] == NULL) {
            LOGWARN("VPC population failed to populate resource at idx %d\n", i);
            vpc->population_failed = 1;
        }
    }

    LOGTRACE("vpc (%s): AFTER POPULATE\n", vpc->name);
    for (i = 0; i < VPC_END; i++) {
        if (vpc->midos[i]) {
            LOGTRACE("\tmidos[%d]: %d\n", i, vpc->midos[i]->init);
        }
    }

    return (ret);
}

/**
 * Populates euca VPC core models from mido models.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @return 0 on success. 1 on any failure.
 */
int populate_mido_core(mido_config *mido, mido_core *midocore) {
    int rc = 0, ret = 0, i = 0, j = 0, k = 0;
    char *url = NULL;
    midoname **brports = NULL;
    int max_brports = 0;
    midoname **rtports = NULL;
    int max_rtports = 0;

    midonet_api_host *endhost = NULL;
    endhost = mido_get_host(mido->ext_eucanetdhostname, NULL);
    if (endhost) {
        midocore->eucanetdhost = endhost;
    } else {
        LOGERROR("unable to find eucanetd host %s in mido\n", mido->ext_eucanetdhostname);
        return (1);
    }
    
    midonet_api_router *eucart = NULL;
    eucart = mido_get_router("eucart");
    if (eucart) {
        LOGTRACE("Found core router %s\n", eucart->obj->name);
        midocore->eucart = eucart;
        midocore->midos[CORE_EUCART] = eucart->obj;
        rtports = eucart->ports;
        max_rtports = eucart->max_ports;
    }

    midonet_api_bridge *eucabr = NULL;
    eucabr = mido_get_bridge("eucabr");
    if (eucabr) {
        LOGTRACE("Found core bridge %s\n", eucabr->obj->name);
        midocore->eucabr = eucabr;
        midocore->midos[CORE_EUCABR] = eucabr->obj;
        brports = eucabr->ports;
        max_brports = eucabr->max_ports;
    }

    midonet_api_ipaddrgroup *mdipag = NULL;
    mdipag = mido_get_ipaddrgroup("metadata_ip");
    if (mdipag) {
        LOGTRACE("Found metadata ip-address-group %s\n", mdipag->obj->name);
        midocore->metadata_iag = mdipag;
        midocore->midos[CORE_METADATA_IPADDRGROUP] = mdipag->obj;
    }

    // search all ports for RT/BR ports
    for (i = 0; i < max_brports; i++) {
        if (brports[i] == NULL) {
            continue;
        }
        for (j = 0; j < max_rtports; j++) {
            if (rtports[j] == NULL) {
                continue;
            }
            rc = mido_getel_midoname(rtports[j], "peerId", &url);
            if (!rc && url && brports[i]->uuid) {
                if (!strcmp(url, brports[i]->uuid)) {
                    LOGTRACE("Found eucart-eucabr link.\n");
                    midocore->midos[CORE_EUCABR_RTPORT] = brports[i];
                    midocore->midos[CORE_EUCART_BRPORT] = rtports[j];
                }
            }
            EUCA_FREE(url);
        }
    }

    // search for mido GW ports
    for (j = 0; j < max_rtports; j++) {
        if (rtports[j] == NULL) {
            continue;
        }
        url = NULL;
        rc = mido_getel_midoname(rtports[j], "portAddress", &url);
        if (!rc && url) {
            for (k = 0; k < mido->ext_rthostarrmax; k++) {
                if (!strcmp(url, mido->ext_rthostaddrarr[k])) {
                    LOGTRACE("Found gw port for %s.\n", mido->ext_rthostaddrarr[k])
                    midocore->gwports[k] = rtports[j];
                    if (midocore->max_gws < k) {
                        midocore->max_gws = k;
                    }
                }
            }
        }
        EUCA_FREE(url);
    }

    midoname **hosts = NULL;
    int max_hosts = 0;
    rc = mido_get_hosts(&hosts, &max_hosts);
    for (k = 0; k < mido->ext_rthostarrmax; k++) {
        for (i = 0; i < max_hosts; i++) {
            if (!strcmp(hosts[i]->name, mido->ext_rthostnamearr[k])) {
                LOGTRACE("Found gw host %s\n", hosts[i]->name);
                midocore->gwhosts[k] = hosts[i];
                if (midocore->max_gws < k) {
                    LOGWARN("Unexpected number of gwhosts(%d) - > number of gwports(%d).\n", k, midocore->max_gws);
                    midocore->max_gws = k;
                }
            }
        }
    }
    (midocore->max_gws)++;
    EUCA_FREE(hosts);

    midonet_api_portgroup *eucapg = NULL;
    eucapg = mido_get_portgroup("eucapg");
    if (eucapg) {
        LOGTRACE("Found gw portgroup %s\n", eucapg->obj->name);
        midocore->midos[CORE_GWPORTGROUP] = eucapg->obj;
    }

    LOGDEBUG("midocore: AFTER POPULATE\n");
    for (i = 0; i < CORE_END; i++) {
        LOGTRACE("\tmidos[%d]: %d\n", i, (midocore->midos[i] == NULL) ? 0 : midocore->midos[i]->init);
    }

    int gw_ok = 1;
    for (i = 0; i < midocore->max_gws; i++) {
        if ((midocore->gwhosts[i] && midocore->gwports[i]) && ((midocore->gwhosts[i]->init == 0) || (midocore->gwports[i]->init == 0))) {
            LOGWARN("Invalid gwhost or gwport found.\n");
            gw_ok = 0;
            continue;
        }
        LOGTRACE("\tgwhost[%s]: %d gwport[%s]: %d\n", midocore->gwhosts[i]->name, midocore->gwhosts[i]->init, midocore->gwports[i]->name, midocore->gwports[i]->init);
    }
    return (ret);
}

/**
 * Disconnects an instance/interface from mido - link of the interface to the
 * VPC bridge port is removed; the corresponding bridge port is deleted; and
 * the corresponding VPC bridge dhcp entry is removed.
 *
 * @param subnet [in] the subnet where the interface of interst is linked.
 * @param vpcinstance [in] the interface of interest
 *
 * @return 0 on success. non-zero number otherwise.
 */
int disconnect_mido_vpc_instance(mido_vpc_subnet *subnet, mido_vpc_instance *vpcinstance) {
    int ret = 0, rc = 0;

    if (!vpcinstance || !strlen(vpcinstance->name) || !subnet) {
        LOGERROR("Invalid argument: cannot disconnect a NULL instance\n");
        return (1);
    }
    if ((vpcinstance->midos[INST_VMHOST] != NULL) && (vpcinstance->midos[INST_VMHOST]->init == 1)) {
        LOGINFO("\tdisconnecting %s from %s\n", vpcinstance->name, vpcinstance->midos[INST_VMHOST]->name);
    } else {
        LOGERROR("cannot disconnect %s from NULL host\n", vpcinstance->name);
    }

    // unlink port, delete port, delete dhcp entry
    rc = mido_unlink_host_port(vpcinstance->midos[INST_VMHOST], vpcinstance->midos[INST_VPCBR_VMPORT]);
    ret += rc;

    midonet_api_bridge *br = subnet->subnetbr;
    if (!br) {
        LOGERROR("Unable to find subnet bridge. Aborting interface disconnect\n");
        return (1);
    }
    rc = mido_delete_bridge_port(br, vpcinstance->midos[INST_VPCBR_VMPORT]);
    vpcinstance->midos[INST_VPCBR_VMPORT] = NULL;
    ret += rc;

    if (!br->dhcps || !br->dhcps[0]) {
        LOGERROR("Unable to find subnet dhcp. Aborting interface disconnect\n");
        return (1);
    }
    rc = mido_delete_dhcphost(br->obj, br->dhcps[0]->obj, vpcinstance->midos[INST_VPCBR_DHCPHOST]);
    vpcinstance->midos[INST_VPCBR_DHCPHOST] = NULL;
    ret += rc;
    
    return (ret);
}

/**
 * Remove MidoNet objects created to implement elip/pubip of the given interface.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param vpc [in] vpc of the interface of interest.
 * @param vpcinstance [in] interface of interest.
 * @return 0 on success. 1 otherwise.
 */
int disconnect_mido_vpc_instance_elip(mido_config *mido, mido_vpc *vpc, mido_vpc_instance *vpcinstance) {
    int ret = 0, rc = 0;

    if (!mido || !vpc || !vpcinstance) {
        LOGERROR("Invalid argument: cannot disconnect NULL elip\n");
        return (1);
    }
    rc = mido_delete_route(mido->midocore->eucart, vpcinstance->midos[INST_ELIP_ROUTE]);
    vpcinstance->midos[INST_ELIP_ROUTE] = NULL;
    if (rc) {
        LOGWARN("Failed to delete ELIP_ROUTE for %s\n", vpcinstance->name);
        ret = 1;
    }

    rc = mido_delete_rule(vpc->rt_preelipchain, vpcinstance->midos[INST_ELIP_PRE]);
    vpcinstance->midos[INST_ELIP_PRE] = NULL;
    if (rc) {
        LOGWARN("Failed to delete ELIP_PRE for %s\n", vpcinstance->name);
        ret = 1;
    }

    rc = mido_delete_rule(vpc->rt_uplink_postchain, vpcinstance->midos[INST_ELIP_POST]);
    vpcinstance->midos[INST_ELIP_POST] = NULL;
    if (rc) {
        LOGWARN("Failed to delete ELIP_POST for %s\n", vpcinstance->name);
        ret = 1;
    }

    rc = mido_delete_ipaddrgroup_ip(vpcinstance->iag_pre, vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP_IP]);
    vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP_IP] = NULL;
    if (rc) {
        LOGWARN("could not delete instance (%s) IP addr from ipaddrgroup\n", vpcinstance->name);
        ret = 1;
    }

    rc = mido_delete_ipaddrgroup_ip(vpcinstance->iag_post, vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP_IP]);
    vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP_IP] = NULL;
    if (rc) {
        LOGWARN("could not delete instance (%s) IP addr from ipaddrgroup\n", vpcinstance->name);
        ret = 1;
    }

    LOGINFO("\tdisconnecting %s elastic IP\n", vpcinstance->name);
    return (ret);
}

/**
 * Create MidoNet objects to implement elip/pubip of the given interface.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param vpc [in] vpc of the interface of interest.
 * @param vpcinstance [in] interface of interest.
 * @return 0 on success. 1 otherwise.
 */
int connect_mido_vpc_instance_elip(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, mido_vpc_instance *vpcinstance) {
    int rc = 0, ret = 0;
    char *ipAddr_pub = NULL, *ipAddr_priv = NULL, *tmpstr = NULL, vpc_nw[24], vpc_nm[24];
    char vpc_rtip[32];
    mido_core *midocore = NULL;
    
    if (!mido || !vpc || !vpcsubnet || !vpcinstance) {
        LOGWARN("Invalid argument: cannot process elip for NULL\n");
        return (1);
    }

    if (!vpcinstance->gniInst->publicIp || !vpcinstance->gniInst->privateIp) {
        LOGWARN("input ip is 0.0.0.0: - will not connetc_mido_vpc_instance_elip\n");
        return (0);
    }

    midocore = mido->midocore;
    
    cidr_split(vpc->gniVpc->cidr, vpc_nw, vpc_nm, NULL, NULL);
    tmpstr = hex2dot(mido->int_rtnw + vpc->rtid);
    snprintf(vpc_rtip, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    ipAddr_pub = hex2dot(vpcinstance->gniInst->publicIp);
    ipAddr_priv = hex2dot(vpcinstance->gniInst->privateIp);
    vpcinstance->pubip = vpcinstance->gniInst->publicIp;
    vpcinstance->privip = vpcinstance->gniInst->privateIp;

    rc = mido_create_ipaddrgroup_ip(vpcinstance->iag_pre, vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP],
            ipAddr_pub, &(vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP_IP]));
    if (rc) {
        LOGERROR("Failed to add %s as member of ipag\n", ipAddr_pub);
        ret++;
    }

    rc = mido_create_ipaddrgroup_ip(vpcinstance->iag_post, vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP],
            ipAddr_priv, &(vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP_IP]));
    if (rc) {
        LOGERROR("Failed to add %s as member of ipag\n", ipAddr_priv);
        ret++;
    }

    // DNAT
    rc = mido_create_rule(vpc->rt_preelipchain, vpc->midos[VPC_VPCRT_PREELIPCHAIN], &(vpcinstance->midos[INST_ELIP_PRE]),
            NULL, "type", "dnat", "flowAction", "continue", "ipAddrGroupDst",
            vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP]->uuid, "natTargets", "jsonlist", "natTargets:addressTo",
            ipAddr_priv, "natTargets:addressFrom", ipAddr_priv, "natTargets:portFrom", "0",
            "natTargets:portTo", "0", "natTargets:END", "END", NULL);
    if (rc) {
        LOGERROR("cannot create elip dnat rule: check midonet health\n");
        ret++;
    }

    // SNAT
    rc = mido_create_rule(vpc->rt_uplink_postchain, vpc->midos[VPC_VPCRT_UPLINK_POSTCHAIN], &(vpcinstance->midos[INST_ELIP_POST]),
            NULL, "type", "snat", "nwDstAddress", vpc_nw, "invNwDst", "true",
            "nwDstLength", vpc_nm, "flowAction", "continue", "ipAddrGroupSrc",
            vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP]->uuid, "natTargets", "jsonlist",
            "natTargets:addressTo", ipAddr_pub, "natTargets:addressFrom", ipAddr_pub,
            "natTargets:portFrom", "0", "natTargets:portTo", "0", "natTargets:END", "END", NULL);
    if (rc) {
        LOGERROR("cannot create elip snat rule: check midonet health\n");
        ret++;
    }

    // EL ip route in main router (eucart)
    if (!ret && vpcinstance->gniInst->publicIp) {
        rc = mido_create_route(midocore->eucart, midocore->midos[CORE_EUCART], midocore->midos[CORE_EUCART_BRPORT],
                "0.0.0.0", "0", ipAddr_pub, "32", vpc_rtip, "100", &(vpcinstance->midos[INST_ELIP_ROUTE]));
        if (rc) {
            LOGERROR("failed to setup pub/el IP route on midonet router: check midonet health\n");
            ret++;
        }
    }

    EUCA_FREE(ipAddr_pub);
    EUCA_FREE(ipAddr_priv);
    return (ret);
}

/**
 * Connects an instance/interface to mido host - link of the interface to the
 * VPC bridge port is created; the corresponding bridge port is created; and
 * the corresponding VPC bridge dhcp entry is created.
 *
 * @param subnet [in] the subnet where the interface of interst is linked.
 * @param vpcinstance [in] the interface of interest
 * @param instanceDNSDomain [in] DNS domain to be used in the DHCP entry
 *
 * @return 0 on success. non-zero number otherwise.
 */
int connect_mido_vpc_instance(mido_vpc_subnet *vpcsubnet, mido_vpc_instance *vpcinstance, char *instanceDNSDomain) {
    int ret = 0, rc = 0;
    char *macAddr = NULL, *ipAddr = NULL;
    char ifacename[IF_NAME_LEN];

    snprintf(ifacename, IF_NAME_LEN, "vn_%s", vpcinstance->gniInst->name);

    // create the Exterior port for VMs
    rc = mido_create_bridge_port(vpcsubnet->subnetbr, vpcsubnet->midos[SUBN_BR], &(vpcinstance->midos[INST_VPCBR_VMPORT]));
    if (rc) {
        LOGERROR("cannot create midonet bridge port: check midonet health\n");
        ret++;
    }

    // link vm host port to vm bridge port
    if (ret == 0) {
        rc = mido_link_host_port(vpcinstance->midos[INST_VMHOST], ifacename, vpcsubnet->midos[SUBN_BR], vpcinstance->midos[INST_VPCBR_VMPORT]);
        if (rc) {
            LOGERROR("cannot create midonet bridge port to vm interface link: check midonet health\n");
            ret++;
        }
    }

    // set up dhcp host entry
    hex2mac(vpcinstance->gniInst->macAddress, &macAddr);
    ipAddr = hex2dot(vpcinstance->gniInst->privateIp);
    for (int i = 0; i < strlen(macAddr); i++) {
        macAddr[i] = tolower(macAddr[i]);
    }

    LOGDEBUG("\tadding host %s/%s to dhcp server\n", SP(macAddr), SP(ipAddr));
    rc = mido_create_dhcphost(vpcsubnet->subnetbr, vpcsubnet->midos[SUBN_BR_DHCP],
            vpcinstance->gniInst->name, macAddr, ipAddr, instanceDNSDomain, &(vpcinstance->midos[INST_VPCBR_DHCPHOST]));
    if (rc) {
        LOGERROR("failed to create midonet dhcp host entry: check midonet health\n");
        ret = 1;
    }
    EUCA_FREE(ipAddr);
    EUCA_FREE(macAddr);

    // apply the chains to the instance port
    rc = mido_update_port(vpcinstance->midos[INST_VPCBR_VMPORT], "inboundFilterId", vpcinstance->midos[INST_PRECHAIN]->uuid,
            "outboundFilterId", vpcinstance->midos[INST_POSTCHAIN]->uuid, "id", vpcinstance->midos[INST_VPCBR_VMPORT]->uuid,
            "type", "Bridge", NULL);
    if (rc > 0) {
        LOGERROR("cannot attach midonet chain to midonet port: check midonet health\n");
        ret++;
    }

    return (ret);
}

int clear_mido_config(mido_config *mido) {
    return (free_mido_config_v(mido, 0));
}

int free_mido_config(mido_config *mido) {
    return (free_mido_config_v(mido, 1));
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
int free_mido_config_v(mido_config *mido, int mode) {
    int ret = 0, i = 0;

    if (!mido)
        return (0);

    if (mode == 1) {
        EUCA_FREE(mido->eucahome);

        EUCA_FREE(mido->ext_eucanetdhostname);
        EUCA_FREE(mido->ext_pubnw);
        EUCA_FREE(mido->ext_pubgwip);

        for (i = 0; i < mido->ext_rthostarrmax; i++) {
            EUCA_FREE(mido->ext_rthostnamearr[i]);
            EUCA_FREE(mido->ext_rthostaddrarr[i]);
            EUCA_FREE(mido->ext_rthostifacearr[i]);
        }
    }

    free_mido_core(mido->midocore);
    if (mode == 1) {
        EUCA_FREE(mido->midocore);
        mido->midocore = NULL;
    }

    for (i = 0; i < mido->max_vpcs; i++) {
        free_mido_vpc(&(mido->vpcs[i]));
    }
    EUCA_FREE(mido->vpcs);
    mido->vpcs = NULL;
    mido->max_vpcs = 0;

    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        free_mido_vpc_secgroup(&(mido->vpcsecgroups[i]));
    }
    EUCA_FREE(mido->vpcsecgroups);
    mido->vpcsecgroups = NULL;
    mido->max_vpcsecgroups = 0;

    if (mode == 1) {
        bzero(mido, sizeof (mido_config));
    }

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
int free_mido_core(mido_core * midocore) {
    int ret = 0;

    if (!midocore)
        return (0);

    bzero(midocore, sizeof (mido_core));

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
int free_mido_vpc(mido_vpc * vpc) {
    int ret = 0, i = 0;

    if (!vpc)
        return (0);

    for (i = 0; i < vpc->max_subnets; i++) {
        free_mido_vpc_subnet(&(vpc->subnets[i]));
    }
    EUCA_FREE(vpc->subnets);
    
    for (i = 0; i < vpc->max_natgateways; i++) {
        free_mido_vpc_natgateway(&(vpc->natgateways[i]));
    }
    EUCA_FREE(vpc->natgateways);

    bzero(vpc, sizeof (mido_vpc));

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
int free_mido_vpc_subnet(mido_vpc_subnet * vpcsubnet) {
    int ret = 0, i = 0;

    if (!vpcsubnet)
        return (0);

    EUCA_FREE(vpcsubnet->routes);

    for (i = 0; i < vpcsubnet->max_natgateways; i++) {
        free_mido_vpc_natgateway(&(vpcsubnet->natgateways[i]));
    }
    EUCA_FREE(vpcsubnet->natgateways);

    for (i = 0; i < vpcsubnet->max_instances; i++) {
        free_mido_vpc_instance(&(vpcsubnet->instances[i]));
    }
    EUCA_FREE(vpcsubnet->instances);

    bzero(vpcsubnet, sizeof (mido_vpc_subnet));

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
int free_mido_vpc_instance(mido_vpc_instance * vpcinstance) {
    int ret = 0;

    if (!vpcinstance)
        return (0);

    bzero(vpcinstance, sizeof (mido_vpc_instance));

    return (ret);
}

//!
//! Releases memory resources allocated for an NAT Gateway.
//!
//! @param[in] vpcnatgateway NAT Gateway of interest
//!
//! @return 0 on success. 1 on any error.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int free_mido_vpc_natgateway(mido_vpc_natgateway *vpcnatgateway) {
    int ret = 0;

    if (!vpcnatgateway) {
        return (0);        
    }

    //mido_free_midoname_list(vpcnatgateway->midos, NATG_END);
    bzero(vpcnatgateway, sizeof (mido_vpc_natgateway));
    return (ret);
}

/**
 * Deletes MidoNet objects created to implement the given vpc subnet.
 * @param mido [in] data structure holding all discovered MidoNet resources.
 * @param vpc [in] vpc of the interface of interest.
 * @param subnet [in] vpc subnet of the interface of interest.
 * @param vpcinstance [in] data structure holding information about the interface of interest.
 *
 * @return 0 on success. Positive integer if error(s) is/are detected.
 */
int delete_mido_vpc_subnet(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet) {
    int rc = 0, ret = 0, i = 0;

    if (!mido || !vpc || !vpc->vpcrt || !vpcsubnet || !strlen(vpcsubnet->name)) {
        LOGERROR("Invalid argument: delete_mido_vpc_subnet\n");
        return (1);
    }

    LOGDEBUG("DELETING SUBNET '%s'\n", vpcsubnet->name);

    // delete all nat gateways
    for (i = 0; i < vpcsubnet->max_natgateways; i++) {
        if (strlen(vpcsubnet->natgateways[i].name)) {
            rc = delete_mido_vpc_natgateway(mido, vpcsubnet, &(vpcsubnet->natgateways[i]));
        }
    }

    // delete all instances on this subnet
    for (i = 0; i < vpcsubnet->max_instances; i++) {
        if (strlen(vpcsubnet->instances[i].name)) {
            rc = delete_mido_vpc_instance(mido, vpc, vpcsubnet, &(vpcsubnet->instances[i]));
        }
    }

    rc += mido_delete_router_port(vpc->vpcrt, vpcsubnet->midos[SUBN_VPCRT_BRPORT]);
    rc += mido_delete_bridge(vpcsubnet->midos[SUBN_BR]);
    rc += delete_mido_meta_subnet_veth(mido, vpcsubnet->name);

    free_mido_vpc_subnet(vpcsubnet);

    return (ret);
}

/**
 * Deletes MidoNet objects created to implement the given vpc.
 * @param mido [in] data structure holding all discovered MidoNet resources.
 * @param vpc [in] vpc of the interface of interest.
 * @return 0 on success. Positive integer if error(s) is/are detected.
 */
int delete_mido_vpc(mido_config *mido, mido_vpc *vpc) {
    int rc = 0, ret = 0, i = 0;

    if (!mido || !mido->midocore || !mido->midocore->eucabr || !vpc || !vpc->name) {
        LOGERROR("Invalid argument: cannot delete NULL vpc\n");
        return (1);
    }

    LOGDEBUG("DELETING VPC: %s, %s\n", vpc->name, vpc->midos[VPC_VPCRT]->name);

    for (i = 0; i < vpc->max_subnets; i++) {
        if (strlen(vpc->subnets[i].name)) {
            rc = delete_mido_vpc_subnet(mido, vpc, &(vpc->subnets[i]));
        }
    }
    
    rc = mido_delete_bridge_port(mido->midocore->eucabr, vpc->midos[VPC_EUCABR_DOWNLINK]);
    rc = mido_delete_router(vpc->midos[VPC_VPCRT]);

    rc = mido_delete_chain(vpc->midos[VPC_VPCRT_PREELIPCHAIN]);
    rc = mido_delete_chain(vpc->midos[VPC_VPCRT_UPLINK_PRECHAIN]);
    rc = mido_delete_chain(vpc->midos[VPC_VPCRT_UPLINK_POSTCHAIN]);

    rc = delete_mido_meta_vpc_namespace(mido, vpc);

    clear_router_id(mido, vpc->rtid);
    free_mido_vpc(vpc);

    return (ret);
}

/**
 * Creates mido objects to implement a VPC.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param midocore [in] data structure that holds midocore configuration
 * @param vpc [in] data structure that holds information about the VPC of interest.
 * @param vpcsubnet [i/o] data structure that holds information about the VPC subnet of interest.
 * @param subnet [in] VPC subnet network address.
 * @param slashnet [in] VPC subnet network prefix length.
 * @param gw [in] VPC subnet gateway (.1 address).
 * @param instanceDNSDomain [in] DNS domain to be used in VPC subnet dhcp server configuration.
 * @param instanceDNSServers [in] DNS servers to be returned through DHCP to instances in the VPC subnet.
 * @param max_instanceDNSServers [in] number of DNS servers.
 * @return 0 on success. 1 on any failure.
 */
int create_mido_vpc_subnet(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet,
        char *subnet, char *slashnet, char *gw, char *instanceDNSDomain,
        u32 * instanceDNSServers, int max_instanceDNSServers) {
    int rc = 0, ret = 0;
    char name_buf[32], *tapiface = NULL;

    // Create a VPC router port - to be linked with the VPC subnet port
    rc = mido_create_router_port(vpc->vpcrt, vpc->midos[VPC_VPCRT], gw, subnet, slashnet, NULL, &(vpcsubnet->midos[SUBN_VPCRT_BRPORT]));
    if (rc) {
        LOGERROR("cannot create midonet router port: check midonet health\n");
        return (1);
    }

    // Route the VPC subnet CIDR block through the port just created
    rc = mido_create_route(vpc->vpcrt, vpc->midos[VPC_VPCRT], vpcsubnet->midos[SUBN_VPCRT_BRPORT],
            "0.0.0.0", "0", subnet, slashnet, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create midonet router route: check midonet health\n");
        return (1);
    }

    // Create the VPC subnet mido bridge
    snprintf(name_buf, 32, "vb_%s_%s", vpc->name, vpcsubnet->name);
    vpcsubnet->subnetbr = mido_create_bridge(VPCMIDO_TENANT, name_buf, &(vpcsubnet->midos[SUBN_BR]));
    if (!vpcsubnet->subnetbr) {
        LOGERROR("cannot create midonet bridge: check midonet health\n");
        return (1);
    }

    // Create a VPC subnet mido bridge port - to be linked the the VPC mido router
    rc = mido_create_bridge_port(vpcsubnet->subnetbr, vpcsubnet->midos[SUBN_BR], &(vpcsubnet->midos[SUBN_BR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create midonet bridge port: check midonet health\n");
        return (1);
    }

    // Link VPC mido router and VPC subnet mido bridge
    rc = mido_link_ports(vpcsubnet->midos[SUBN_VPCRT_BRPORT], vpcsubnet->midos[SUBN_BR_RTPORT]);
    if (rc) {
        LOGERROR("cannot create midonet router <-> bridge link: check midonet health\n");
        return (1);
    }

    // setup DHCP on the bridge for this subnet
    rc = mido_create_dhcp(vpcsubnet->subnetbr, vpcsubnet->midos[SUBN_BR], subnet, slashnet, gw,
            instanceDNSServers, max_instanceDNSServers, &(vpcsubnet->midos[SUBN_BR_DHCP]));
    if (rc) {
        LOGERROR("cannot create midonet dhcp server: check midonet health\n");
        return (1);
    }

    // meta tap
    if (mido->midocore->eucanetdhost) {
        vpcsubnet->midos[SUBN_BR_METAHOST] = mido->midocore->eucanetdhost->obj;
    } else {
        LOGERROR("unable to find eucanetd host in mido.\n");
        return (1);
    }

    if ((vpcsubnet->midos[SUBN_BR_METAHOST]) && (vpcsubnet->midos[SUBN_BR_METAHOST]->init)) {
        rc = create_mido_meta_subnet_veth(mido, vpc, vpcsubnet->name, subnet, slashnet, &tapiface);
        if (rc || !tapiface) {
            LOGERROR("cannot create metadata taps: check log output for details\n");
            ret = 1;
        } else {
            LOGTRACE("created tap iface: %s\n", SP(tapiface));
        }

        if (!ret) {
            // create tap port
            rc = mido_create_bridge_port(vpcsubnet->subnetbr, vpcsubnet->midos[SUBN_BR], &(vpcsubnet->midos[SUBN_BR_METAPORT]));
            if (rc) {
                LOGERROR("cannot create midonet bridge port: check midonet health\n");
                ret = 1;
            }
        }

        if (!ret) {
            // link tap port
            rc = mido_link_host_port(vpcsubnet->midos[SUBN_BR_METAHOST], tapiface,
                    vpcsubnet->midos[SUBN_BR], vpcsubnet->midos[SUBN_BR_METAPORT]);
            if (rc) {
                LOGERROR("cannot link port to host interface: check midonet health\n");
                ret = 1;
            }
        }
    }
    EUCA_FREE(tapiface);

    return (ret);
}

/**
 * Creates mido objects to implement a VPC.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param midocore [in] data structure that holds midocore configuration
 * @param vpc [i/o] data structure that holds information about the VPC of interest.
 * @return 0 on success. 1 on any failure.
 */
int create_mido_vpc(mido_config *mido, mido_core *midocore, mido_vpc *vpc) {
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

    // Create the VPC mido router
    snprintf(name_buf, 32, "vr_%s_%d", vpc->name, vpc->rtid);
    vpc->vpcrt = mido_create_router(VPCMIDO_TENANT, name_buf, &(vpc->midos[VPC_VPCRT]));
    if (vpc->vpcrt == NULL) {
        LOGERROR("cannot create midonet router: check midonet health\n");
        return (1);
    }

    // Create an eucabr port where the VPC router will be linked
    rc = mido_create_bridge_port(midocore->eucabr, midocore->midos[CORE_EUCABR], &(vpc->midos[VPC_EUCABR_DOWNLINK]));
    if (rc) {
        LOGERROR("cannot create midonet bridge port: check midonet health\n");
        return (1);
    }

    // Create a VPC router port - uplink to eucabr
    rc = mido_create_router_port(vpc->vpcrt, vpc->midos[VPC_VPCRT], ip, nw, sn, NULL, &(vpc->midos[VPC_VPCRT_UPLINK]));
    if (rc) {
        LOGERROR("cannot create midonet router port: check midonet health\n");
        return (1);
    }

    // link the vpc network and euca network
    rc = mido_link_ports(vpc->midos[VPC_EUCABR_DOWNLINK], vpc->midos[VPC_VPCRT_UPLINK]);
    if (rc) {
        LOGERROR("cannot create midonet bridge <-> router link: check midonet health\n");
        return (1);
    }

    // Create the VPC CIDR block route through uplink
    rc = mido_create_route(vpc->vpcrt, vpc->midos[VPC_VPCRT], vpc->midos[VPC_VPCRT_UPLINK],
            "0.0.0.0", "0", nw, sn, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create midonet router route: check midonet health\n");
        return (1);
    }

    // Create pre, preelip, and post chains
    midonet_api_chain *ch = NULL;
    snprintf(name_buf, 32, "vc_%s_prechain", vpc->name);
    ch = mido_create_chain(VPCMIDO_TENANT, name_buf, &(vpc->midos[VPC_VPCRT_UPLINK_PRECHAIN]));
    if (!ch) {
        LOGERROR("cannot create midonet pre chain: check midonet health\n");
        return (1);
    } else {
        vpc->rt_uplink_prechain = ch;
    }

    snprintf(name_buf, 32, "vc_%s_preelip", vpc->name);
    ch = mido_create_chain(VPCMIDO_TENANT, name_buf, &(vpc->midos[VPC_VPCRT_PREELIPCHAIN]));
    if (!ch) {
        LOGERROR("cannot create midonet preelip chain: check midonet health\n");
        return (1);
    } else {
        vpc->rt_preelipchain = ch;
    }

    snprintf(name_buf, 32, "vc_%s_postchain", vpc->name);
    ch = mido_create_chain(VPCMIDO_TENANT, name_buf, &(vpc->midos[VPC_VPCRT_UPLINK_POSTCHAIN]));
    if (!ch) {
        LOGERROR("cannot create midonet post chain: check midonet health\n");
        return (1);
    } else {
        vpc->rt_uplink_postchain = ch;
    }
  
    // add the jump chains (pre -> preelip)
    rc = mido_create_rule(vpc->rt_uplink_prechain, vpc->midos[VPC_VPCRT_UPLINK_PRECHAIN], NULL, NULL,
            "position", "1", "type", "jump", "jumpChainId",
            vpc->midos[VPC_VPCRT_PREELIPCHAIN]->uuid, NULL);
    if (rc) {
        LOGERROR("cannot create midonet rule: check midonet health\n");
    }

    // apply PRECHAIN and POST chain to VPCRT_UPLINK port (support custom routes based on source routing)
    rc = mido_update_port(vpc->midos[VPC_VPCRT_UPLINK], "outboundFilterId", vpc->midos[VPC_VPCRT_UPLINK_POSTCHAIN]->uuid,
            "inboundFilterId", vpc->midos[VPC_VPCRT_UPLINK_PRECHAIN]->uuid, "id", vpc->midos[VPC_VPCRT_UPLINK]->uuid,
            "networkAddress", nw, "portAddress", ip, "type", "Router", NULL);
    if (rc > 0) {
        LOGERROR("cannot update router port infilter and/or outfilter: check midonet health\n");
        return (1);
    }

    rc = create_mido_meta_vpc_namespace(mido, vpc);
    if (rc) {
        LOGERROR("cannot create netns for VPC %s: check above log for details\n", vpc->name);
        return (1);
    }

    return (0);
}

/**
 * Removes MidoNet objects that are needed to implement euca VPC core.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @return 0 on success. 1 otherwise.
 */
int delete_mido_core(mido_config *mido, mido_core *midocore) {
    int rc = 0, i = 0;

    // delete the metadata_ip ipaddrgroup
    rc = mido_delete_ipaddrgroup(midocore->midos[CORE_METADATA_IPADDRGROUP]);
    midocore->midos[CORE_METADATA_IPADDRGROUP] = NULL;

    // delete the host/port links
    for (i = 0; i < midocore->max_gws; i++) {
        if ((midocore->gwhosts[i]->init == 0) || (midocore->gwports[i]->init == 0)) {
            continue;
        }
        rc = mido_unlink_host_port(midocore->gwhosts[i], midocore->gwports[i]);
    }

    // delete the port-group
    rc = mido_delete_portgroup(midocore->midos[CORE_GWPORTGROUP]);
    midocore->midos[CORE_GWPORTGROUP] = NULL;

    // delete the bridge
    rc = mido_delete_bridge(midocore->midos[CORE_EUCABR]);
    midocore->midos[CORE_EUCABR] = NULL;

    // delete the router
    rc = mido_delete_router(midocore->midos[CORE_EUCART]);
    midocore->midos[CORE_EUCART] = NULL;

    rc = delete_mido_meta_core(mido);

    return (0);
}

/**
 * Creates MidoNet objects that are needed to implement euca VPC core.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @return 0 on success. 1 otherwise.
 */
int create_mido_core(mido_config * mido, mido_core * midocore) {
    int ret = 0, rc = 0, i = 0;
    char nw[32], sn[32], gw[32], *tmpstr = NULL, pubnw[32], pubnm[32];

    tmpstr = hex2dot(mido->int_rtnw);
    snprintf(nw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    tmpstr = hex2dot(mido->int_rtaddr);
    snprintf(gw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    snprintf(sn, 32, "%d", mido->int_rtsn);

    LOGDEBUG("creating mido core\n");
    midocore->eucart = mido_create_router(VPCMIDO_TENANT, "eucart", &(midocore->midos[CORE_EUCART]));
    if (midocore->eucart == NULL) {
        LOGERROR("cannot create router: check midonet health\n");
        return (1);
    }

    // core bridge port and addr/route
    rc = mido_create_router_port(midocore->eucart, midocore->midos[CORE_EUCART], gw, nw, sn,
            NULL, &(midocore->midos[CORE_EUCART_BRPORT]));
    if (rc) {
        LOGERROR("cannot create router port: check midonet health\n");
        return (1);
    }

    rc = mido_create_route(midocore->eucart, midocore->midos[CORE_EUCART], midocore->midos[CORE_EUCART_BRPORT],
            "0.0.0.0", "0", nw, sn, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create router route: check midonet health\n");
        return (1);
    }

    rc = mido_create_portgroup(VPCMIDO_TENANT, "eucapg", &(midocore->midos[CORE_GWPORTGROUP]));
    if (rc) {
        LOGWARN("cannot create portgroup: check midonet health\n");
    }

    // conditional here depending on whether we have multi GW or single GW defined in config
    if (mido->ext_rthostarrmax) {
        for (i = 0; i < mido->ext_rthostarrmax; i++) {
            cidr_split(mido->ext_pubnw, pubnw, pubnm, NULL, NULL);
            rc = mido_create_router_port(midocore->eucart, midocore->midos[CORE_EUCART], mido->ext_rthostaddrarr[i],
                    pubnw, pubnm, NULL, &(midocore->gwports[i]));
            if (rc) {
                LOGERROR("cannot create router port: check midonet health\n");
                ret = 1;
            }

            if (midocore->gwports[i]->init) {
                // exterior port GW IP
                rc = mido_create_route(midocore->eucart, midocore->midos[CORE_EUCART], midocore->gwports[i],
                        "0.0.0.0", "0", pubnw, pubnm, "UNSET", "0", NULL);
                if (rc) {
                    LOGERROR("cannot create router route: check midonet health\n");
                    ret = 1;
                }

                // exterior port default GW
                rc = mido_create_route(midocore->eucart, midocore->midos[CORE_EUCART], midocore->gwports[i],
                        "0.0.0.0", "0", "0.0.0.0", "0", mido->ext_pubgwip, "0", NULL);
                if (rc) {
                    LOGERROR("cannot create router route: check midonet health\n");
                    ret = 1;
                }

                // link exterior port 
                rc = mido_link_host_port(midocore->gwhosts[i], mido->ext_rthostifacearr[i],
                        midocore->midos[CORE_EUCART], midocore->gwports[i]);
                if (rc) {
                    LOGERROR("cannot link router port to host interface: check midonet health\n");
                    ret = 1;
                }

                rc = mido_create_portgroup_port(midocore->midos[CORE_GWPORTGROUP],
                        midocore->gwports[i], NULL);
                if (rc) {
                    LOGWARN("cannot add portgroup port: check midonet health\n");
                    ret = 1;
                }
            }
        }
    }

    midocore->eucabr = mido_create_bridge(VPCMIDO_TENANT, "eucabr", &(midocore->midos[CORE_EUCABR]));
    if (midocore->eucabr == NULL) {
        LOGERROR("cannot create bridge: check midonet health\n");
        return (1);
    }

    rc = mido_create_bridge_port(midocore->eucabr, midocore->midos[CORE_EUCABR], &(midocore->midos[CORE_EUCABR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create bridge port: check midonet health\n");
        return (1);
    }

    rc = mido_link_ports(midocore->midos[CORE_EUCART_BRPORT], midocore->midos[CORE_EUCABR_RTPORT]);
    if (rc) {
        LOGERROR("cannot create router <-> bridge link: check midonet health\n");
        ret = 1;
    }

    midonet_api_ipaddrgroup *ig = NULL;
    ig = mido_create_ipaddrgroup(VPCMIDO_TENANT, "metadata_ip", &(midocore->midos[CORE_METADATA_IPADDRGROUP]));
    if (!ig) {
        LOGWARN("Failed to create ip adress group metadata_ip.\n");
        return (1);
    } else {
        midocore->metadata_iag = ig;
    }
    rc = mido_create_ipaddrgroup_ip(ig, midocore->midos[CORE_METADATA_IPADDRGROUP],
            "169.254.169.254", NULL);
    if (rc) {
        LOGERROR("cannot add metadata IP to metadata ipaddrgroup.\n");
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
int cidr_split(char *cidr, char *outnet, char *outslashnet, char *outgw, char *outplustwo) {
    char *tok = NULL;
    char *cpy = NULL;
    u32 nw = 0, gw = 0;

    if (!cidr) {
        return (1);
    }

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

//!
//! Checks if the IP address in the argument is a MidoNet VPC subnet +2 address.
//! For example, if subnets 172.16.0.0/20 and 172.16.16.0/20 are valid subnets in
//! MidoNet configuration, this function will return 0 iff the argument IP address
//! is 172.16.0.2 or 172.16.16.2
//!
//! @param[in] mido - pointer to MidoNet configuration data structure.
//! @param[in] iptocheck - IP address to check.
//!
//! @return 0 iff the argument IP address matches a MidoNet VPC subnetwork address
//!         +2. Returns a value != 0 otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int is_mido_vpc_plustwo(mido_config *mido, char *iptocheck) {
    int rc = 0;
    int i = 0, j = 0;
    mido_vpc *vpc = NULL;
    mido_vpc_subnet *vpcsubnet;
    char tmpip[32];

    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            if (vpc->gnipresent && vpcsubnet->gnipresent) {
                tmpip[0] = '\0';
                rc = cidr_split(vpcsubnet->gniSubnet->cidr, NULL, NULL, NULL, tmpip);
                if (!rc) {
                    LOGDEBUG("cidr_split() failed.\n");
                }
                if (strcmp(tmpip, iptocheck) == 0) {
                    return 0;
                }
            }
        }
    }
    return 1;
}

//!
//! Parses the route entry target string in the argument and translates into one
//! of valid vpc_route_entry_target_t enumeration value.
//!
//! @param[in] target - string representation of route entry target.
//!
//! @return a valid vpc_route_entry_target_t enumeration value (which includes
//!         VPC_TARGET_INVALID in case of an invalid argument.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
enum vpc_route_entry_target_t parse_mido_route_entry_target(const char *target) {
    if (!target) {
        return (VPC_TARGET_INVALID);
    }
    if (strstr(target, "local")) {
        return (VPC_TARGET_LOCAL);
    }
    if (strstr(target, "igw-")) {
        return (VPC_TARGET_INTERNET_GATEWAY);
    }
    if (strstr(target, "eni-")) {
        return (VPC_TARGET_ENI);
    }
    if (strstr(target, "nat-")) {
        return (VPC_TARGET_NAT_GATEWAY);
    }
    // todo: other network devices
    return (VPC_TARGET_INVALID);
}

/*  LocalWords:  sgrulepos
 */
