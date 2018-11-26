// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
#include <json-c/json.h>

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

static int maint_c = 0;
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

/**
 * Deletes all VPC metaproxy namespace interfaces and metabr.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @return 0 on success. 1 otherwise.
 */
int do_delete_meta_nslinks(mido_config *mido) {
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH], sid[16];
    char devpath[EUCA_MAX_PATH];
    sequence_executor cmds;

    mido_vpc *vpc;
    se_init(&cmds, mido->config->cmdprefix, 4, 1);
    for (int i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        sscanf(vpc->name, "vpc-%8s", sid);

        snprintf(devpath, EUCA_MAX_PATH, "/sys/class/net/vn2_%s/", sid);
        if (!check_directory(devpath)) {
            snprintf(cmd, EUCA_MAX_PATH, "ip link del vn2_%s", sid);
            se_add(&cmds, cmd, NULL, ignore_exit);
        }
    }

    rc = se_execute(&cmds);
    if (rc) {
        LOGWARN("failed to delete metaproxy ip namespace veth pairs\n");
        ret = 1;
    }

    delete_mido_meta_core(mido);

    se_free(&cmds);

    return (ret);
}

/**
 * Deletes all VPC instances/interfaces MidoNet chains.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @return 0 on success. 1 otherwise.
 */
int do_delete_vpceni_chains(mido_config *mido) {
    int ret = 0;
    if (!mido) {
        LOGWARN("Invalid argument: cannot delete VPC ifs chains - NULL config.\n");
        return (1);
    }

    // Remove all ic_* chains
    midoname **chains = NULL;
    int max_chains = 0;
    
    mido_get_chains(VPCMIDO_TENANT, &chains, &max_chains);
    for (int i = 0; i < max_chains; i++) {
        midoname *chain = chains[i];
        if (strstr(chain->name, "ic_")) {
            mido_delete_chain(chain);
        }
    }
    EUCA_FREE(chains);

    return (ret);
}

/**
 * Removes MidoNet objects that are needed to implement metaproxy-based VPC MD.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @return 0 on success. 1 otherwise.
 */
int do_metaproxy_disable(mido_config *mido) {
    int ret = 0;
    int rc = 0;
    if (!mido) {
        LOGWARN("Invalid argument: cannot disable metaproxy - NULL config.\n");
        return (1);
    }

    if (!mido->config->enable_mido_md) {
        return (0);
    }

    // Tear down metaproxies
    rc = do_metaproxy_teardown(mido);
    if (rc) {
        LOGERROR("failed to teardown metadata proxies\n");
        ret++;
    }

    // Disconnect instances/interfaces from md
    for (int i = 0; i < mido->max_vpcs; i++) {
        mido_vpc *vpc = &(mido->vpcs[i]);
        for (int j = 0; j < vpc->max_subnets; j++) {
            mido_vpc_subnet *subnet = &(vpc->subnets[j]);
            u32 subnet_addr = 0;
            // Get the Subnet CIDR
            boolean found = FALSE;
            for (int k = 0; vpc->vpcrt && k < vpc->vpcrt->max_routes && !found; k++) {
                midoname *route = vpc->vpcrt->routes[k];
                if (!route || !subnet->midos[SUBN_VPCRT_BRPORT]) {
                    continue;
                }
                if (!strcmp(route->route->nexthopport, subnet->midos[SUBN_VPCRT_BRPORT]->uuid) &&
                        !route->route->nexthopgateway && strcmp(route->route->dstlen, "32")) {
                    found = TRUE;
                    subnet_addr = dot2hex(route->route->dstnet);
                    LOGTRACE("\t%s : %s\n", subnet->name, route->route->dstnet);
                }
            }
            // Due to dot2 dns nat rules, removing metaproxy nat rules is not sufficient
            for (int k = 0; k < subnet->max_instances && FALSE; k++) {
                mido_vpc_instance *vpcif = &(subnet->instances[k]);
                if (!vpcif->prechain || !vpcif->postchain) {
                    continue;
                }
                midoname *ptmpmn = NULL;
                // Search metaproxy DNAT rule
                char *pt_buf = hex2dot(subnet_addr + 3);
                mido_find_rule_from_list(vpcif->prechain->rules, vpcif->prechain->max_rules, &ptmpmn,
                        "type", "dnat", "flowAction", "continue",
                        "ipAddrGroupDst", mido->midocore->midos[CORE_METADATA_IPADDRGROUP]->uuid,
                        "nwProto", "6", "tpDst", "jsonjson", "tpDst:start", "80", "tpDst:end", "80",
                        "tpDst:END", "END", "natTargets", "jsonlist", "natTargets:addressTo", pt_buf,
                        "natTargets:addressFrom", pt_buf, "natTargets:portFrom",
                        "8008", "natTargets:portTo", "8008", "natTargets:END", "END", NULL);
                if (ptmpmn) {
                    LOGTRACE("%s: deleting DNAT rule %s\n", vpcif->name, ptmpmn->uuid);
                    mido_delete_rule(vpcif->prechain, ptmpmn);
                }
                // Search metaproxy SNAT rule
                mido_find_rule_from_list(vpcif->postchain->rules, vpcif->postchain->max_rules, &ptmpmn,
                        "type", "snat", "flowAction", "continue",
                        "nwSrcAddress", pt_buf, "nwSrcLength", "32", "nwProto", "6",
                        "tpSrc", "jsonjson", "tpSrc:start", "8008", "tpSrc:end", "8008", "tpSrc:END", "END",
                        "natTargets", "jsonlist", "natTargets:addressTo", "169.254.169.254",
                        "natTargets:addressFrom", "169.254.169.254", "natTargets:portFrom", "80",
                        "natTargets:portTo", "80", "natTargets:END", "END", NULL);
                if (ptmpmn) {
                    LOGTRACE("%s: deleting SNAT rule %s\n", vpcif->name, ptmpmn->uuid);
                    mido_delete_rule(vpcif->postchain, ptmpmn);
                }
                EUCA_FREE(pt_buf);
            }
        }
    }

    return (ret);
}

/**
 * Terminate metaproxy (nginx) processes of all VPCs
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. Positive integer otherwise.
 */
int do_metaproxy_teardown(mido_config *mido) {
    LOGDEBUG("tearing down metaproxy subsystem\n");
    return (do_metaproxy_maintain(mido, 1));
}

/**
 * Start metaproxy (nginx) processes for all VPCs
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. Positive integer otherwise.
 */
int do_metaproxy_setup(mido_config *mido) {
    return (do_metaproxy_maintain(mido, 0));
}

/**
 * Maintenance of metaproxy (nginx) process(es)
 * @param mido [in] data structure that holds MidoNet configuration
 * @param mode [in] 0 to create. 1 to terminate.
 * @return 0 on success. Positive integer otherwise.
 * 
 * @note both core and vpc nginx processes hold the network namespace open. In order
 * to properly cleanup network namespaces, core nginx needs to be terminated when
 * deleting VPCs.
 */
int do_metaproxy_maintain(mido_config *mido, int mode) {
    int ret = 0, rc, i = 0, dorun = 0;
    pid_t npid = 0;
    char cmd[EUCA_MAX_PATH], *pidstr = NULL, pidfile[EUCA_MAX_PATH];
    sequence_executor cmds;

    if (!mido || mode < 0 || mode > 1) {
        LOGERROR("invalid argument: unable to maintain metaproxy for NULL mido\n");
        return (1);
    }

    rc = se_init(&cmds, mido->config->cmdprefix, 4, 1);

    dorun = 0;
    snprintf(pidfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/nginx_localproxy.pid", mido->eucahome);
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
            LOGTRACE("core proxy not running, starting new core proxy\n");
            snprintf(cmd, EUCA_MAX_PATH,
                    "nginx -p . -c " EUCALYPTUS_DATA_DIR "/nginx_proxy.conf -g 'pid "
                    EUCALYPTUS_RUN_DIR "/nginx_localproxy.pid; env NEXTHOP=127.0.0.1; "
                    "env NEXTHOPPORT=8773; env EUCAHOME=%s;'",
                    mido->eucahome, mido->eucahome, mido->eucahome);
        } else if (mode == 1) {
            LOGTRACE("core proxy running, terminating core proxy\n");
            snprintf(cmd, EUCA_MAX_PATH, "kill %d", npid);
        }
        rc = se_add(&cmds, cmd, NULL, ignore_exit);
    } else {
        LOGTRACE("not maintaining proxy, no action to take for pid (%d)\n", npid);
    }

    for (i = 0; i < mido->max_vpcs; i++) {
        if (strlen(mido->vpcs[i].name)) {
            dorun = 0;
            snprintf(pidfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/nginx_vpcproxy_%s.pid",
                    mido->eucahome, mido->vpcs[i].name);
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
                    LOGTRACE("VPC (%s) proxy not running, starting new proxy\n", mido->vpcs[i].name);
                    snprintf(cmd, EUCA_MAX_PATH,
                            "nsenter --net=/var/run/netns/%s nginx -p . -c " EUCALYPTUS_DATA_DIR
                            "/nginx_proxy.conf -g 'pid " EUCALYPTUS_RUN_DIR "/nginx_vpcproxy_%s.pid; "
                            "env VPCID=%s; env NEXTHOP=169.254.0.1; env NEXTHOPPORT=8009; env EUCAHOME=%s;'",
                            mido->vpcs[i].name, mido->eucahome, mido->eucahome, mido->vpcs[i].name, mido->vpcs[i].name, mido->eucahome);
                } else if (mode == 1) {
                    LOGTRACE("VPC (%s) proxy running, terminating VPC proxy\n", mido->vpcs[i].name);
                    snprintf(cmd, EUCA_MAX_PATH, "kill %d", npid);
                }
                rc = se_add(&cmds, cmd, NULL, ignore_exit);
            } else {
                LOGTRACE("not maintaining VPC (%s) proxy, no action to take on pid (%d)\n", mido->vpcs[i].name, npid);
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

/**
 * Controls metadata nginx instance
 * @param mido [in] data structure that holds MidoNet configuration
 * @param mode [in] VPCMIDO_NGINX_START to create. VPCMIDO_NGINX_STOP to terminate.
 * @return 0 on success. Positive integer otherwise.
 */
int do_md_nginx_maintain(mido_config *mido, enum vpcmido_nginx_t mode) {
    int ret = 0;
    int rc = 0;
    boolean do_start = FALSE;
    boolean do_stop = FALSE;
    char cmd[EUCA_MAX_PATH];

    if (!mido || !mido->config || mode < VPCMIDO_NGINX_START || mode > VPCMIDO_NGINX_STOP) {
        LOGERROR("invalid argument: unable to maintain md nginx\n");
        return (1);
    }

    snprintf(cmd, EUCA_MAX_PATH, "%s %s is-active %s", mido->config->cmdprefix,
            mido->config->systemctl, EUCANETD_NGINX_UNIT);
    rc = timeshell_nb(cmd, 10, FALSE);

    if (mode == VPCMIDO_NGINX_START) {
        if (rc != 0) {
            do_start = TRUE;
        }
    } else if (mode == VPCMIDO_NGINX_STOP) {
        if (rc == 0) {
            do_stop = TRUE;
        }
    }

    if (do_start) {
        LOGINFO("\tstarting md nginx process\n");
        snprintf(cmd, EUCA_MAX_PATH, "%s %s start %s", mido->config->cmdprefix,
                mido->config->systemctl, EUCANETD_NGINX_UNIT);
        rc = timeshell_nb(cmd, 10, FALSE);
        if (rc != 0) {
            LOGWARN("failed to start eucanetd-nginx\n");
        }
    }
    if (do_stop) {
        LOGINFO("\tstopping md nginx process\n");
        snprintf(cmd, EUCA_MAX_PATH, "%s %s start %s", mido->config->cmdprefix,
                mido->config->systemctl, EUCANETD_NGINX_UNIT);
        rc = timeshell_nb(cmd, 10, FALSE);
        if (rc != 0) {
            LOGWARN("failed to stop eucanetd-nginx\n");
        }
    }

    return (ret);
}

/**
 * Remove metadata core artifacts from the system.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. Positive integer otherwise.
 */
int delete_mido_meta_core(mido_config *mido) {
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH];
    char devpath[EUCA_MAX_PATH];
    sequence_executor cmds;

    rc = se_init(&cmds, mido->config->cmdprefix, 4, 1);

    snprintf(devpath, EUCA_MAX_PATH, "/sys/class/net/metabr/");
    if (!check_directory(devpath)) {
        snprintf(cmd, EUCA_MAX_PATH, "ip link set metabr down");
        rc = se_add(&cmds, cmd, NULL, ignore_exit2);

        snprintf(cmd, EUCA_MAX_PATH, "brctl delbr metabr");
        rc = se_add(&cmds, cmd, NULL, ignore_exit);
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

/**
 * Creates the metadata core artifacts.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. Positive integer otherwise.
 */
int create_mido_meta_core(mido_config *mido) {
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH];
    sequence_executor cmds;

    rc = se_init(&cmds, mido->config->cmdprefix, 4, 1);

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

/**
 * Deletes metadata artifacts created for VPC vpc.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param vpcname [in] vpc name/id of interest
 * @return 0 on success. Positive integer otherwise.
 */
int delete_mido_meta_vpc_namespace(mido_config *mido, char *vpcname) {
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH], sid[16];
    char devpath[EUCA_MAX_PATH];
    sequence_executor cmds;
    struct timeval tv;

    eucanetd_timer_usec(&tv);

    sscanf(vpcname, "vpc-%8s", sid);

    rc = se_init(&cmds, mido->config->cmdprefix, 10, 1);

    //    snprintf(cmd, EUCA_MAX_PATH, "ip link set vn2_%s down", sid);
    //    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(devpath, EUCA_MAX_PATH, "/sys/class/net/vn2_%s/", sid);
    if (!check_directory(devpath)) {
        snprintf(cmd, EUCA_MAX_PATH, "ip link del vn2_%s", sid);
        rc = se_add(&cmds, cmd, NULL, ignore_exit);
    }

    snprintf(cmd, EUCA_MAX_PATH, "ip netns del %s", vpcname);
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

/**
 * Creates metadata artifacts for the VPC vpc.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param vpc [in] pointer to the mido_vpc data structure of interest
 * @return 0 on success. Positive integer otherwise.
 */
int create_mido_meta_vpc_namespace(mido_config *mido, mido_vpc *vpc) {
    int ret = 0, rc = 0;
    u32 nw, ip;
    char cmd[EUCA_MAX_PATH], *ipstr = NULL, sid[16];
    sequence_executor cmds;
    struct timeval tv;

    eucanetd_timer_usec(&tv);
    rc = read_mido_meta_vpc_namespace(mido, vpc);
    if (!rc) {
        LOGTRACE("namespace (%s) already exists, skipping create\n", vpc->name);
        return (0);
    }

    // create meta tap namespace/devices
    sscanf(vpc->name, "vpc-%8s", sid);

    rc = se_init(&cmds, mido->config->cmdprefix, 4, 1);

    snprintf(cmd, EUCA_MAX_PATH, "ip netns add %s", vpc->name);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    if (!mido->config->enable_mido_md) {
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
        snprintf(cmd, EUCA_MAX_PATH, "nsenter --net=/var/run/netns/%s ip addr add %s/16 dev vn3_%s", vpc->name, ipstr, sid);
        EUCA_FREE(ipstr);
        se_add(&cmds, cmd, NULL, ignore_exit2);

        snprintf(cmd, EUCA_MAX_PATH, "nsenter --net=/var/run/netns/%s ip link set vn3_%s up", vpc->name, sid);
        rc = se_add(&cmds, cmd, NULL, ignore_exit);
    }

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

/**
 * Checks whether metadata artifacts for VPC vpc is present in the system.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param vpc [in] pointer to the mido_vpc data structure of interest
 * @return 0 on success. Positive integer otherwise.
 */
int read_mido_meta_vpc_namespace(mido_config *mido, mido_vpc *vpc) {
    char cmd[EUCA_MAX_PATH], sid[16];

    // create a meta tap namespace/devices
    sscanf(vpc->name, "vpc-%8s", sid);

    snprintf(cmd, EUCA_MAX_PATH, "/var/run/netns/%s", vpc->name);
    if (check_path(cmd)) {
        LOGTRACE("cannot find VPC netns: %s\n", cmd);
        return (1);
    } else {
        LOGTRACE("found VPC netns: %s\n", cmd);
    }

    if (!mido->config->enable_mido_md) {
        snprintf(cmd, EUCA_MAX_PATH, "vn2_%s", sid);
        if (!dev_exist(cmd)) {
            LOGTRACE("cannot find VPC metataps vn2_%s\n", sid);
            return (1);
        } else {
            LOGTRACE("found VPC metataps vn2_%s\n", sid);
        }
    }

    return (0);
}

/**
 * Deletes the metadata veth interface pair created for subnet name.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param name [in] name of the subnet of interest
 * @return 0 on success. Positive integer otherwise.
 */
int delete_mido_meta_subnet_veth(mido_config *mido, char *name) {
    int ret = 0, rc = 0;
    char cmd[EUCA_MAX_PATH], sid[16];
    sequence_executor cmds;
    struct timeval tv;

    eucanetd_timer_usec(&tv);
    sscanf(name, "subnet-%8s", sid);

    rc = se_init(&cmds, mido->config->cmdprefix, 4, 1);

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

/**
 * Creates a metadata veth interface pair for subnet name in VPC vpc with CIDR subnet/slashnet.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param vpc [in] pointer to the mido_vpc data structure of interest
 * @param name [in] name of the subnet of interest
 * @param subnet [in] network address of the subnet
 * @param slashnet [in] network mask of the subnet
 * @param tapiface [out] name of the veth interface (to be bound to MN bridge)
 * @return 0 on success. Positive integer otherwise.
 */
int create_mido_meta_subnet_veth(mido_config *mido, mido_vpc *vpc, char *name, char *subnet, char *slashnet, char **tapiface) {
    int ret = 0, rc = 0;
    u32 nw, gw;
    char cmd[EUCA_MAX_PATH], *gateway = NULL, sid[16];
    sequence_executor cmds;
    struct timeval tv;

    eucanetd_timer_usec(&tv);
    // create a meta tap
    sscanf(name, "subnet-%8s", sid);

    snprintf(cmd, EUCA_MAX_PATH, "vn0_%s", sid);
    if (dev_exist(cmd)) {
        LOGTRACE("subnet device (%s) already exists, skipping\n", cmd);
        *tapiface = EUCA_ZALLOC_C(16, sizeof (char));
        snprintf(*tapiface, 16, "vn0_%s", sid);
        return (0);
    }

    rc = se_init(&cmds, mido->config->cmdprefix, 4, 1);

    snprintf(cmd, EUCA_MAX_PATH, "ip link add vn0_%s type veth peer name vn1_%s", sid, sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "ip link set vn0_%s up", sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    snprintf(cmd, EUCA_MAX_PATH, "ip link set vn1_%s netns %s", sid, vpc->name);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    nw = dot2hex(subnet);
    gw = nw + 3;
    gateway = hex2dot(gw);
    snprintf(cmd, EUCA_MAX_PATH, "nsenter --net=/var/run/netns/%s ip addr add %s/%s dev vn1_%s", vpc->name, gateway, slashnet, sid);
    EUCA_FREE(gateway);
    se_add(&cmds, cmd, NULL, ignore_exit2);

    snprintf(cmd, EUCA_MAX_PATH, "nsenter --net=/var/run/netns/%s ip link set vn1_%s up", vpc->name, sid);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not execute tap interface create/ip assign commands: see above log entries for details\n");
        *tapiface = NULL;
        ret = 1;
    } else {
        *tapiface = EUCA_ZALLOC_C(16, sizeof (char));
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
int do_midonet_populate(mido_config *mido) {
    int rc = 0;
    struct timeval tv;

    eucanetd_timer_usec(&tv);
    rc = reinitialize_mido(mido);
    if (rc) {
        LOGERROR("failed to initialize euca-mido model data structures.\n");
    }
    LOGTRACE("\treinitialize_mido() in %ld us.\n", eucanetd_timer_usec(&tv));

    // populated core
    eucanetd_timer_usec(&tv);
    rc = populate_mido_core(mido, mido->midocore);
    if (rc) {
        return (1);
    }
    
    // populate md
    if (mido_get_router(VPCMIDO_MDRT)) {
        mido->config->populate_mido_md = TRUE;
    } else {
        mido->config->populate_mido_md = FALSE;
    }
    if (mido->config->enable_mido_md || mido->config->populate_mido_md) {
        rc = populate_mido_md(mido);
        if (rc) {
            return (1);
        }
    }
    
    LOGINFO("\tmido_core populated in %.2f ms.\n",  eucanetd_timer_usec(&tv) / 1000.0);

    return (do_midonet_populate_vpcs(mido));
}

/**
 * Populates euca VPC models (data structures) from MidoNet models.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. 1 on any failure.
 */
int do_midonet_populate_vpcs(mido_config *mido) {
    // pattern
    // - find all VPC routers (and populate VPCs)
    // - for each VPC, find all subnets (and populate subnets)
    // - for each VPC, for each subnet, find all instances (and populate instances)

    int i = 0, j = 0, k = 0, rc = 0, rtid = 0, natgrtid = 0, ret = 0;
    char subnetname[VPC_SUBNET_ID_LEN];
    char vpcname[VPC_ID_LEN];
    char aclname[NETWORK_ACL_ID_LEN];
    char sgname[SECURITY_GROUP_ID_LEN];
    char instanceId[INSTANCE_ID_LEN];
    char natgname[NATG_ID_LEN];
    char tmpstr[MIDO_NAME_LEN];
    char *pattern;
    midoname **chains = NULL;
    int max_chains = 0;
    mido_vpc_secgroup *vpcsecgroup = NULL;
    mido_vpc_instance *vpcinstance = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc_natgateway *vpcnatg = NULL;
    mido_vpc_nacl *vpcnacl = NULL;
    mido_vpc *vpc = NULL;
    struct timeval tv;

    eucanetd_timer_usec(&tv);
    // VPCs
    midonet_api_router **routers = NULL;
    int max_routers = 0;
    rc = midonet_api_cache_get_routers(&routers, &max_routers);
    if (max_routers > 0) {
        mido->vpcs = EUCA_ZALLOC_C(max_routers, sizeof (mido_vpc));
    }
    for (i = 0; i < max_routers; i++) {
        LOGTRACE("inspecting mido router '%s'\n", routers[i]->obj->name);
        memset(vpcname, 0, VPC_ID_LEN);
        if ((strlen(routers[i]->obj->name) > 15) && (routers[i]->obj->name[15] == '_')) {
          pattern = "vr_%12s_%d";
        } else {
          pattern = "vr_%21s_%d";
        }
        sscanf(routers[i]->obj->name, pattern, vpcname, &rtid);
        if ((sscanf(routers[i]->obj->name, pattern, vpcname, &rtid) == 2) &&
                strlen(vpcname) && rtid) {
            vpc = &(mido->vpcs[mido->max_vpcs]);
            mido->max_vpcs++;
            LOGTRACE("discovered VPC installed in midonet: %s\n", vpcname);

            snprintf(vpc->name, sizeof (vpc->name), "%s", vpcname);
            set_router_id(mido, rtid);
            vpc->rtid = rtid;
            vpc->vpcrt = routers[i];
            vpc->midos[VPC_VPCRT] = routers[i]->obj;
            populate_mido_vpc(mido, mido->midocore, vpc);
        }
    }
    LOGINFO("\tvpcs populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    // SUBNETS
    midonet_api_router **natgrouters = NULL;
    int max_natgrouters = 0;
    rc = midonet_api_cache_get_natg_routers(&natgrouters, &max_natgrouters);
    midonet_api_bridge **bridges = NULL;
    int max_bridges;
    rc = midonet_api_cache_get_bridges(&bridges, &max_bridges);
    for (i = 0; i < max_bridges; i++) {
        LOGTRACE("inspecting bridge '%s'\n", bridges[i]->obj->name);

        memset(vpcname, 0, VPC_ID_LEN);
        memset(subnetname, 0, VPC_SUBNET_ID_LEN);

        if ((strlen(bridges[i]->obj->name) > 15) && (bridges[i]->obj->name[15] == '_')) {
          pattern = "vb_%12s_%24s";
        } else {
          pattern = "vb_%21s_%24s";
        }
        sscanf(bridges[i]->obj->name, pattern, vpcname, subnetname);
        if (strlen(vpcname) && strlen(subnetname)) {
            LOGTRACE("discovered VPC subnet installed in midonet: %s/%s\n", vpcname, subnetname);
            find_mido_vpc(mido, vpcname, &vpc);
            if (vpc) {
                LOGTRACE("found VPC matching discovered subnet: %s/%s\n", vpc->name, subnetname);
                vpc->subnets = EUCA_REALLOC_C(vpc->subnets, (vpc->max_subnets + 1), sizeof (mido_vpc_subnet));
                vpcsubnet = &(vpc->subnets[vpc->max_subnets]);
                vpc->max_subnets++;
                bzero(vpcsubnet, sizeof (mido_vpc_subnet));
                snprintf(vpcsubnet->name, VPC_SUBNET_ID_LEN, "%s", subnetname);
                snprintf(vpcsubnet->vpcname, VPC_ID_LEN, "%s", vpcname);
                vpcsubnet->vpc = vpc;
                vpcsubnet->subnetbr = bridges[i];
                vpcsubnet->midos[SUBN_BR] = bridges[i]->obj;
                if (bridges[i]->max_dhcps) {
                    LOGTRACE("%d dhcp for bridge %s\n", bridges[i]->max_dhcps, bridges[i]->obj->name);
                    vpcsubnet->midos[SUBN_BR_DHCP] = bridges[i]->dhcps[0]->obj;
                }

                populate_mido_vpc_subnet(mido, vpc, vpcsubnet);

                // Search for NAT Gateways
                for (j = 0; j < max_natgrouters; j++) {
                    if (natgrouters[j] == NULL) {
                        continue;
                    }
                    natgname[0] = '\0';
                    natgrtid = 0;
                    snprintf(tmpstr, MIDO_NAME_LEN, "natr_%%21s_%s_%%d", subnetname);
                    sscanf(natgrouters[j]->obj->name, tmpstr, natgname, &natgrtid);
                    if ((strlen(natgname)) && (natgrtid != 0)) {
                        LOGTRACE("discovered %s in %s installed in midonet\n", natgname, subnetname);
                        vpcsubnet->natgateways = EUCA_REALLOC_C(vpcsubnet->natgateways, vpcsubnet->max_natgateways + 1, sizeof (mido_vpc_natgateway));
                        vpcnatg = &(vpcsubnet->natgateways[vpcsubnet->max_natgateways]);
                        (vpcsubnet->max_natgateways)++;
                        bzero(vpcnatg, sizeof (mido_vpc_natgateway));
                        snprintf(vpcnatg->name, sizeof (vpcnatg->name), "%s", natgname);
                        set_router_id(mido, natgrtid);
                        vpcnatg->rtid = natgrtid;
                        vpcnatg->natgrt = natgrouters[j];
                        vpcnatg->midos[NATG_RT] = natgrouters[j]->obj;
                        rc = populate_mido_vpc_natgateway(mido, vpc, vpcsubnet, vpcnatg);
                        if (rc) {
                            LOGERROR("cannot populate %s: check midonet health\n", natgname);
                            natgrouters[j] = NULL;
                            ret++;
                        }
                    }
                }
            }
        }
    }
    EUCA_FREE(bridges);
    EUCA_FREE(routers);
    EUCA_FREE(natgrouters);
    LOGINFO("\tvpc subnets populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    // Network ACLs
    mido_get_chains(VPCMIDO_TENANT, &chains, &max_chains);
    for (i = 0; i < max_chains; i++) {
        LOGTRACE("inspecting chain '%s'\n", chains[i]->name);

        memset(vpcname, 0, VPC_ID_LEN);
        memset(aclname, 0, NETWORK_ACL_ID_LEN);

        if ((strlen(chains[i]->name) > 24) && (chains[i]->name[24] == '_')) {
          pattern = "acl_ingress_%12s_%21s";
        } else {
          pattern = "acl_ingress_%21s_%21s";
        }
        sscanf(chains[i]->name, pattern, vpcname, aclname);
        if (strlen(vpcname) && strlen(aclname)) {
            LOGTRACE("discovered VPC network acl installed in midonet: %s/%s\n", vpcname, aclname);
            find_mido_vpc(mido, vpcname, &vpc);
            if (vpc) {
                LOGTRACE("found VPC matching discovered network acl: %s/%s\n", vpc->name, aclname);
                vpc->nacls = EUCA_REALLOC_C(vpc->nacls, (vpc->max_nacls + 1), sizeof (mido_vpc_nacl));
                vpcnacl = &(vpc->nacls[vpc->max_nacls]);
                vpc->max_nacls++;
                memset(vpcnacl, 0, sizeof (mido_vpc_nacl));
                snprintf(vpcnacl->name, NETWORK_ACL_ID_LEN, "%s", aclname);
                snprintf(vpcnacl->vpcname, VPC_ID_LEN, "%s", vpcname);
                vpcnacl->vpc = vpc;

                populate_mido_vpc_nacl(mido, vpcnacl);
            }
        }
    }
    EUCA_FREE(chains);
    LOGINFO("\tvpc network acls populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    // SECGROUPS
    mido_get_chains(VPCMIDO_TENANT, &chains, &max_chains);
    for (i = 0; i < max_chains; i++) {
        LOGTRACE("inspecting chain '%s'\n", chains[i]->name);
        sgname[0] = '\0';

        sscanf(chains[i]->name, "sg_ingress_%20s", sgname);
        if (strlen(sgname)) {
            LOGTRACE("discovered VPC security group installed in midonet: %s\n", sgname);
            mido->vpcsecgroups = EUCA_REALLOC_C(mido->vpcsecgroups, (mido->max_vpcsecgroups + 1), sizeof (mido_vpc_secgroup));
            vpcsecgroup = &(mido->vpcsecgroups[mido->max_vpcsecgroups]);
            (mido->max_vpcsecgroups)++;
            bzero(vpcsecgroup, sizeof (mido_vpc_secgroup));
            snprintf(vpcsecgroup->name, SECURITY_GROUP_ID_LEN, "%s", sgname);
            populate_mido_vpc_secgroup(mido, vpcsecgroup);
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
                if (!brports[k]) {
                    continue;
                }
                memset(instanceId, 0, INSTANCE_ID_LEN);

                if (brports[k]->port && brports[k]->port->ifname) {
                    sscanf(brports[k]->port->ifname, "vn_%s", instanceId);

                    if (strlen(instanceId)) {
                        char *mido_instanceId = find_mido_vpc_instance_id(instanceId);
                        if (mido_instanceId) {
                            snprintf(instanceId, INSTANCE_ID_LEN, "%s", mido_instanceId);
                        }
                        EUCA_FREE(mido_instanceId);
                        LOGTRACE("discovered VPC subnet instance/interface: %s/%s/%s\n", vpc->name, vpcsubnet->name, instanceId);

                        vpcsubnet->instances = EUCA_REALLOC_C(vpcsubnet->instances,
                                (vpcsubnet->max_instances + 1), sizeof (mido_vpc_instance));
                        vpcinstance = &(vpcsubnet->instances[vpcsubnet->max_instances]);
                        memset(vpcinstance, 0, sizeof (mido_vpc_instance));
                        vpcsubnet->max_instances++;
                        snprintf(vpcinstance->name, INTERFACE_ID_LEN, "%s", instanceId);
                        vpcinstance->midos[INST_VPCBR_VMPORT] = brports[k];

                        populate_mido_vpc_instance(mido, mido->midocore, vpc, vpcsubnet, vpcinstance);
                    }
                }
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
int do_midonet_teardown(mido_config *mido) {
    int ret = 0, rc = 0, i = 0;

    //rc = midonet_api_cache_refresh();
    rc = midonet_api_cache_refresh_v_threads(MIDO_CACHE_REFRESH_ALL);
    if (rc) {
        LOGERROR("failed to retrieve objects from MidoNet.\n");
        return (1);
    }

    rc = do_midonet_populate(mido);
    if (rc) {
        LOGWARN("failed to populate VPC models prior to teardown.\n");
    }

    rc = 1;
    for (i = 0; i < 5 && rc; i--) {
        rc = do_metaproxy_teardown(mido);
        if (rc) {
            LOGERROR("cannot teardown meta proxies: see above log for details\n");
            sleep(1);
        }
    }
    if (rc) {
        LOGFATAL("Failed to teardown metaproxies. Aborting midonet teardown\n");
        return (1);
    }

    for (i = 0; i < mido->max_vpcs; i++) {
        delete_mido_vpc(mido, &(mido->vpcs[i]));
    }

    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        delete_mido_vpc_secgroup(mido, &(mido->vpcsecgroups[i]));
    }

    if ((mido->config->flushmode == FLUSH_MIDO_ALL) || (!mido->config->enable_mido_md && mido->config->populate_mido_md)) {
        delete_mido_md(mido);
    }

    char *bgprecovery = NULL;
    bgprecovery = discover_mido_bgps(mido);
    if (bgprecovery && strlen(bgprecovery)) {
        LOGINFO("mido BGP configuration (for manual recovery):\n%s\n", bgprecovery);
    }
    EUCA_FREE(bgprecovery);

    if (mido->config->flushmode == FLUSH_MIDO_ALL) {
        LOGINFO("deleting mido core\n");
        delete_mido_core(mido, mido->midocore);
        do_md_nginx_maintain(mido, VPCMIDO_NGINX_STOP);
    } else {
        LOGDEBUG("skipping the delete of midocore - FLUSH_DYNAMIC selected.\n");
    }

    do_midonet_delete_all(mido);
    midonet_api_cache_flush(NULL);
    reinitialize_mido(mido);

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
    int vpcnaclidx = 0;
    int vpcnatgidx = 0;
    int vpcroutetidx = 0;
    int vpcsgidx = 0;
    int vpcinstanceidx = 0;
    char *privIp = NULL, mapfile[EUCA_MAX_PATH];
    FILE *PFH = NULL;
    char intipmapfile_tmp[EUCA_MAX_PATH];
    char intipmapfile[EUCA_MAX_PATH];
    FILE *INTIPMAPFH = NULL;

    mido_vpc *vpc = NULL;
    mido_vpc_secgroup *vpcsecgroup = NULL;
    mido_vpc_instance *vpcinstance = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc_natgateway *vpcnatgateway = NULL;
    mido_vpc_nacl *vpcnacl = NULL;

    gni_vpc *gnivpc = NULL;
    gni_vpcsubnet *gnivpcsubnet = NULL;
    gni_nat_gateway *gninatgateway = NULL;
    gni_route_table *gniroutetable = NULL;
    gni_instance *gniinstance = NULL;
    gni_secgroup *gnisecgroup = NULL;
    gni_network_acl *gninacl = NULL;

    gni_vpc *appliedvpc = NULL;
    gni_vpcsubnet *appliedvpcsubnet = NULL;
    gni_nat_gateway *appliednatgateway = NULL;
    gni_route_table *appliedroutetable = NULL;
    gni_instance *appliedinstance = NULL;
    gni_secgroup *appliedsecgroup = NULL;
    gni_network_acl *appliednacl = NULL;

    // pass1: ensure that the meta-data map is populated right away
    snprintf(mapfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/eucanetd_vpc_instance_ip_map", mido->eucahome);
    unlink(mapfile);
    PFH = fopen(mapfile, "w");
    if (!PFH) {
        LOGERROR("cannot open VPC map file %s: check permissions and disk capacity\n", mapfile);
        ret = 1;
    }

    if (mido->config->enable_mido_md) {
        snprintf(intipmapfile_tmp, EUCA_MAX_PATH, INTIP_ENI_MAP_FILE_TMP, mido->eucahome);
        snprintf(intipmapfile, EUCA_MAX_PATH, INTIP_ENI_MAP_FILE, mido->eucahome);
        unlink(INTIP_ENI_MAP_FILE_TMP);
        INTIPMAPFH = fopen(intipmapfile_tmp, "w");
        if (!INTIPMAPFH) {
            LOGERROR("unable to open %s: check permissions and disk capacity\n", intipmapfile_tmp);
            LOGINFO("Eucalyptus metadata service will not work properly.\n");
            ret = 1;
        }
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
            if (!cmp_gni_vpc(appliedvpc, gnivpc) && !vpc->population_failed) {
                LOGEXTREME("\t\t%s fully implemented \n", vpc->name);
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
            if ((gnivpc->max_networkAcls > 0) && (ret == 0)) {
                // Allocate vpc_nacls buffer for the worst case
                vpc->nacls = EUCA_REALLOC_C(vpc->nacls, vpc->max_nacls + gnivpc->max_networkAcls, sizeof (mido_vpc_nacl));
            }
        }

        vpcnaclidx = 0;
        for (j = 0; j < gnivpc->max_networkAcls; j++) {
            appliednacl = NULL;
            gninacl = &(gnivpc->networkAcls[j]);
            rc = find_mido_vpc_nacl(vpc, gninacl->name, &vpcnacl);
            if (rc) {
                LOGTRACE("pass1: global VPC Network ACL %s in mido: N\n", gninacl->name);
            } else {
                LOGTRACE("pass1: global VPC Network ACL %s in mido: Y\n", gninacl->name);
                if (appliedGni) {
                    if (vpcnacl->gniNacl) {
                        appliednacl = vpcnacl->gniNacl;
                    } else {
                        appliednacl = gni_get_networkacl(appliedvpc, vpcnacl->name, &vpcnaclidx);
                    }
                }
                if (!cmp_gni_nacl(appliednacl, gninacl, &(vpcnacl->ingress_changed), &(vpcnacl->egress_changed))
                        && !vpcnacl->population_failed) {
                    LOGEXTREME("\t\t%s fully implemented \n", vpcnacl->name);
                    vpcnacl->midopresent = 1;
                } else {
                    vpcnacl->midopresent = 0;
                    // skip NACL egress and ingress chain flush on eucanetd restart (if number of rules matches)
                    if (appliednacl == NULL) {
                        LOGTRACE("egress  gni %d rules mido %d rules\n", gninacl->max_egress, vpcnacl->egress->rules_count);
                        LOGTRACE("ingress gni %d rules mido %d rules\n", gninacl->max_ingress, vpcnacl->ingress->rules_count);
                        if (vpcnacl->egress && (gninacl->max_egress == vpcnacl->egress->rules_count)) {
                            vpcnacl->egress_changed = 0;
                        }
                        if (vpcnacl->ingress && (gninacl->max_ingress == vpcnacl->ingress->rules_count)) {
                            vpcnacl->ingress_changed = 0;
                        }
                    }
                }
                vpcnacl->gniNacl = gninacl;
                vpcnacl->gnipresent = 1;
                gninacl->mido_present = vpcnacl;
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
                if (!cmp_gni_vpcsubnet(appliedvpcsubnet, gnivpcsubnet, &(vpcsubnet->nacl_changed))
                        && !vpcsubnet->population_failed) {
                    LOGEXTREME("\t\t%s fully implemented \n", vpcsubnet->name);
                    vpcsubnet->midopresent = 1;
                    // tag route table entries
                    if (gnivpcsubnet->rt_entry_applied && gnivpcsubnet->routeTable) {
                        for (k = 0; k < gnivpcsubnet->routeTable->max_entries; k++) {
                            gnivpcsubnet->rt_entry_applied[k] = 1;
                        }
                    }
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
                    if (!cmp_gni_interface(appliedinstance, gniinstance, &(vpcinstance->pubip_changed),
                            &(vpcinstance->srcdst_changed), &(vpcinstance->host_changed),
                            &(vpcinstance->sg_changed)) && !vpcinstance->population_failed) {
                        LOGEXTREME("\t\t%s fully implemented \n", vpcinstance->name);
                        vpcinstance->midopresent = 1;
                    } else {
                        vpcinstance->midopresent = 0;
                    }
                    vpcinstance->gniInst = gniinstance;
                    vpcinstance->gnipresent = 1;
                    gniinstance->mido_present = vpcinstance;
                    if (INTIPMAPFH && vpcinstance->eniid) {
                        char *internalIp = hex2dot(mido->mdconfig.mdnw + vpcinstance->eniid);
                        fprintf(INTIPMAPFH, "%s %s %s %s\n", SP(internalIp), SP(vpcinstance->name),
                                SP(vpcinstance->gniInst->vpc), SP(vpcinstance->gniInst->subnet));
                        EUCA_FREE(internalIp);
                    }
                }
            }
        }
        
        vpcnatgidx = 0;
        for (j = 0; j < gnivpc->max_natGateways; j++) {
            appliednatgateway = NULL;
            gninatgateway = &(gnivpc->natGateways[j]);
            rc = find_mido_vpc_natgateway(vpc, gninatgateway->name, NULL, &vpcnatgateway);
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
                if (!cmp_gni_nat_gateway(appliednatgateway, gninatgateway) && !vpcnatgateway->population_failed) {
                    LOGEXTREME("\t\t%s fully implemented \n", vpcnatgateway->name);
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
                    LOGEXTREME("\t\t%s no changes\n", gniroutetable->name);
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
    if (INTIPMAPFH) {
        fclose(INTIPMAPFH);
        // move temporary map file to EUCALYPUTS_RUN_DIR
        if (rename(intipmapfile_tmp, intipmapfile)) {
            LOGWARN("Failed to move %s\n", intipmapfile_tmp);
        }
    }

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
            if (!cmp_gni_secgroup(appliedsecgroup,gnisecgroup, &(vpcsecgroup->ingress_changed),
                    &(vpcsecgroup->egress_changed), &(vpcsecgroup->interfaces_changed))
                    && !vpcsecgroup->population_failed) {
                LOGEXTREME("\t\t%s fully implemented \n", vpcsecgroup->name);
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
    mido_vpc_nacl *vpcnacl = NULL;

    // pass2 - remove anything in MIDO that is not in GNI
    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        vpcsecgroup = &(mido->vpcsecgroups[i]);
        if (strlen(vpcsecgroup->name) == 0) {
            continue;
        }
        LOGEXTREME("processing %s\n", vpcsecgroup->name);
        if (!vpcsecgroup->gnipresent) {
            LOGINFO("\tdeleting %s\n", vpcsecgroup->name);
            ret += delete_mido_vpc_secgroup(mido, vpcsecgroup);
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
                        LOGWARN("Failed to remove pubip from ip address group\n");
                        ret += rc;
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
                        LOGWARN("Failed to remove privip from ip address group\n");
                        ret += rc;
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
                        LOGWARN("Failed to remove allip from ip address group\n");
                        ret += rc;
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
                    ret += delete_mido_vpc_natgateway(mido, vpcsubnet, vpcnatgateway);
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
                    ret += delete_mido_vpc_instance(mido, vpc, vpcsubnet, vpcinstance);
                } else {
                    LOGTRACE("pass2: mido VPC INSTANCE %s in global: Y\n", vpcinstance->name);
                    if (vpcinstance->gniInst && vpcinstance->pubip &&
                            (vpcinstance->gniInst->publicIp != vpcinstance->pubip)) {
                        rc = disconnect_mido_vpc_instance_elip(mido, vpc, vpcinstance);
                        if (rc) {
                            LOGERROR("failed to disconnect %s elip\n", vpcinstance->gniInst->name);
                        } else {
                            vpcinstance->pubip = 0;
                            vpcinstance->pubip_changed = 1;
                        }
                    }
                }
            }
            if (!vpc->gnipresent || !vpcsubnet->gnipresent) {
                LOGINFO("\tdeleting %s\n", vpcsubnet->name);
                ret += delete_mido_vpc_subnet(mido, vpc, vpcsubnet);
            } else {
                LOGTRACE("pass2: mido VPC SUBNET %s in global: Y\n", vpcsubnet->name);
            }
        }
        for (j = 0; j < vpc->max_nacls; j++) {
            vpcnacl = &(vpc->nacls[j]);
            if (strlen(vpcnacl->name) == 0) {
                continue;
            }
            if (!vpc->gnipresent || !vpcnacl->gnipresent) {
                LOGINFO("\tdeleting %s\n", vpcnacl->name);
                ret += delete_mido_vpc_nacl(mido, vpcnacl);
            } else {
                LOGTRACE("pass2: mido VPC Network ACL %s in global: Y\n", vpcnacl->name);
            }
        }
        if (!vpc->gnipresent) {
            LOGINFO("\tdeleting %s\n", vpc->name);
            if (!mido->config->enable_mido_md) {
                rc = do_metaproxy_teardown(mido);
                if (rc) {
                    LOGERROR("cannot teardown metadata proxies\n");
                    ret += rc;
                }
            }
            ret += delete_mido_vpc(mido, vpc);
            // Re-enable nginx
            if (!mido->config->enable_mido_md) {
                rc = do_metaproxy_setup(mido);
                if (rc) {
                    LOGERROR("failed to start metadata proxies\n");
                }
            }
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
    LOGTRACE("initializing VPCs (%d)\n", gni->max_vpcs);
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
            snprintf(vpc->name, VPC_ID_LEN, "%s", gnivpc->name);
            vpc->gniVpc = gnivpc;
            gnivpc->mido_present = vpc;
            get_next_router_id(mido, &(vpc->rtid));
            // allocate space for subnets
            if (gnivpc->max_subnets > 0) {
                vpc->subnets = EUCA_ZALLOC_C(gnivpc->max_subnets, sizeof (mido_vpc_subnet));
            }
            // allocate space for network acls
            if (gnivpc->max_networkAcls > 0) {
                vpc->nacls = EUCA_ZALLOC_C(gnivpc->max_networkAcls, sizeof (mido_vpc_nacl));
            }
        }

        if (vpc->midopresent) {
            // VPC presence test passed in pass1
            LOGTRACE("\t\tskipping pass3 for %s\n", gnivpc->name);
        } else {
            rc = create_mido_vpc(mido, mido->midocore, vpc);
            if (rc) {
                LOGERROR("failed to create VPC %s: check midonet health\n", gnivpc->name);
                rc = 0;
                if (!mido->config->enable_mido_md) {
                    rc = do_metaproxy_teardown(mido);
                }
                if (rc) {
                    LOGERROR("cannot teardown metadata proxies\n");
                    ret++;
                } else {
                    rc = delete_mido_vpc(mido, vpc);
                    if (rc) {
                        LOGERROR("failed to cleanup VPC %s\n", gnivpc->name);
                    }
                    ret++;
                }
                // Re-enable nginx
                rc = 0;
                if (!mido->config->enable_mido_md) {
                    rc = do_metaproxy_setup(mido);
                }
                if (rc) {
                    LOGERROR("failed to setup metadata proxies\n");
                }
                continue;
            } else {
                vpc->population_failed = 0;
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
                snprintf(vpcsubnet->name, VPC_SUBNET_ID_LEN, "%s", gnivpc->subnets[j].name);
                snprintf(vpcsubnet->vpcname, VPC_ID_LEN, "%s", vpc->name);
                vpcsubnet->gniSubnet = gnivpcsubnet;
                gnivpcsubnet->mido_present = vpcsubnet;
                vpcsubnet->nacl_changed = 1;
                // Allocate space for interfaces
                vpcsubnet->instances = EUCA_ZALLOC_C(gnivpcsubnet->max_interfaces, sizeof (mido_vpc_instance));
                // Allocate space for nat gateways
                if (gnivpc->max_natGateways > 0) {
                    vpcsubnet->natgateways = EUCA_ZALLOC_C(gnivpc->max_natGateways, sizeof (mido_vpc_natgateway));
                }
            }

            subnet_buf[0] = slashnet_buf[0] = gw_buf[0] = '\0';
            cidr_split(gnivpcsubnet->cidr, subnet_buf, slashnet_buf, gw_buf, NULL, NULL);

            if (vpcsubnet->midopresent) {
                // VPC subnet presence test passed in pass1
                LOGTRACE("\t\tskipping pass3 for %s\n", gnivpcsubnet->name);
                rc = 0;
            } else {
                rc = create_mido_vpc_subnet(mido, vpc, vpcsubnet, subnet_buf, slashnet_buf,
                        gw_buf, gni->instanceDNSDomain, gni->instanceDNSServers, gni->max_instanceDNSServers);
            }
            if (rc) {
                LOGERROR("failed to create VPC %s subnet %s: check midonet health\n", gnivpc->name, gnivpcsubnet->name);
                ret++;
                rc = delete_mido_vpc_subnet(mido, vpc, vpcsubnet);
                if (rc) {
                    LOGERROR("Failed to delete subnet %s. Check for duplicate midonet objects.\n", gnivpcsubnet->name);
                }
                continue;
            } else {
                vpcsubnet->population_failed = 0;
            }
            vpcsubnet->gnipresent = 1;
            // Update references to vpc and subnet for each interface
            for (int k = 0; k < gnivpcsubnet->max_interfaces; k++) {
                gni_instance *gniif = gnivpcsubnet->interfaces[k];
                gniif->mido_vpc = vpc;
                gniif->mido_vpcsubnet = vpcsubnet;
            }
        }

        // do subnets route tables
        for (j = 0; j < gnivpc->max_subnets; j++) {
            gnivpcsubnet = &(gnivpc->subnets[j]);

            vpcsubnet = (mido_vpc_subnet *) gnivpcsubnet->mido_present;
            if (!vpcsubnet) {
                // failed to create subnet
                continue;
            }

            subnet_buf[0] = slashnet_buf[0] = gw_buf[0] = '\0';
            cidr_split(gnivpcsubnet->cidr, subnet_buf, slashnet_buf, gw_buf, NULL, NULL);

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
                        vpcsubnet->population_failed = 1;
                        ret++;
                    }
                } else {
                    LOGTRACE("\t\tskipping pass3 for %s\n", gni_rtable->name);
                }
            } else {
                LOGWARN("route table for %s not found.\n", gnivpcsubnet->name);
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
                    LOGTRACE("\t\tskipping pass3 for %s\n", gninatg->name);
                    continue;
                }
            } else {
                LOGINFO("\tcreating %s\n", gnivpc->natGateways[j].name);
                // get the subnet
                find_mido_vpc_subnet(vpc, gninatg->subnet, &vpcsubnet);
                if (vpcsubnet == NULL) {
                    LOGERROR("Unable to find %s in vpc for %s - aborting NAT Gateway creation\n", gninatg->subnet, gninatg->name);
                    continue;
                }
                // necessary memory should have been allocated in pass1
                vpcnatg = &(vpcsubnet->natgateways[vpcsubnet->max_natgateways]);
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
                } else {
                    vpcnatg->population_failed = 0;
                }
            }
        }    
    }

    // set up metadata proxies once vpcs/subnets are all set up
    rc = 0;
    if (!mido->config->enable_mido_md) {
        rc = do_metaproxy_setup(mido);
    }
    if (rc) {
        LOGERROR("cannot set up metadata proxies: see above log for details\n");
        ret++;
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
            LOGTRACE("\t\t%s found in mido\n", gnisecgroup->name);
        } else {
            rc = create_mido_vpc_secgroup(mido, vpcsecgroup);
            if (rc) {
                LOGERROR("cannot create mido security group %s: check midonet health\n", vpcsecgroup->name);
                rc = delete_mido_vpc_secgroup(mido, vpcsecgroup);
                if (rc) {
                    LOGERROR("failed to cleanup %s\n", gnisecgroup->name);
                }
                ret++;
                continue;
            } else {
                vpcsecgroup->population_failed = 0;
            }
        }
        vpcsecgroup->gnipresent = 1;
        LOGTRACE("\t\t%s ingress %d egress %d intf %d\n", vpcsecgroup->name,
                vpcsecgroup->ingress_changed, vpcsecgroup->egress_changed,
                vpcsecgroup->interfaces_changed);
    }

    // Process security group rules
    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        int ecnt = ret;
        vpcsecgroup = &(mido->vpcsecgroups[i]);
        gnisecgroup = vpcsecgroup->gniSecgroup;
        mido_parsed_chain_rule sgrule;

        if (strlen(vpcsecgroup->name) == 0) {
            continue;
        }
        if (gnisecgroup == NULL) {
            LOGWARN("unknown security group %s\n", vpcsecgroup->name);
            continue;
        }
        // Process egress rules
        if (!vpcsecgroup->population_failed && !vpcsecgroup->egress_changed) {
            LOGTRACE("\t\tskipping pass3 for %s egress\n", gnisecgroup->name);
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
                }
            }
        }
        
        // Process ingress rules
        if (!vpcsecgroup->population_failed && !vpcsecgroup->ingress_changed) {
            LOGTRACE("\t\tskipping pass3 for %s ingress\n", gnisecgroup->name);
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
                LOGTRACE("\t\t%s already in mido %s\n", pubipstr, gnisecgroup->name);
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
                LOGTRACE("\t\t%s already in mido %s\n", privipstr, gnisecgroup->name);
            } else {
                rc = mido_create_ipaddrgroup_ip(vpcsecgroup->iag_priv, NULL, privipstr, NULL);
                if (rc) {
                    LOGWARN("failed to add %s to %s\n", privipstr, vpcsecgroup->midos[VPCSG_IAGPRIV]->name);
                    ret++;
                }
            }
            if (vpcsecgroup->midopresent_allips_pub[j] == 1) {
                LOGTRACE("\t\t%s already in mido %s\n", pubipstr, gnisecgroup->name);
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
                LOGTRACE("\t\t%s already in mido %s\n", privipstr, gnisecgroup->name);
            } else {
                rc = mido_create_ipaddrgroup_ip(vpcsecgroup->iag_all, NULL, privipstr, NULL);
                if (rc) {
                    LOGWARN("failed to add %s to %s\n", privipstr, vpcsecgroup->midos[VPCSG_IAGALL]->name);
                    ret++;
                }
            }
            EUCA_FREE(pubipstr);
            EUCA_FREE(privipstr);

            if (ecnt != ret) {
                vpcsecgroup->population_failed = 1;
            } else {
                vpcsecgroup->population_failed = 0;
            }
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
    char subnet_buf[24], slashnet_buf[8], gw_buf[24], dot2[24], dot3[24];

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

    // Open Internal_IP-to-ENI_ID map file
    char intipmapfile[EUCA_MAX_PATH];
    FILE *INTIPMAPFH = NULL;
    if (mido->config->enable_mido_md) {
        snprintf(intipmapfile, EUCA_MAX_PATH, INTIP_ENI_MAP_FILE, mido->eucahome);
        INTIPMAPFH = fopen(intipmapfile, "a");
        if (!INTIPMAPFH) {
            LOGERROR("unable to open %s: check permissions and disk capacity\n", intipmapfile);
            LOGINFO("Eucalyptus metadata service will not work properly.\n");
            ret++;
        }
    }

    // Process instances/interfaces
    for (i = 0; i < gni->max_ifs; i++) {
        eucanetd_timer_usec(&tv);
        gniif = gni->ifs[i];
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
            memset(vpcif, 0, sizeof (mido_vpc_instance));
            vpcsubnet->max_instances++;
            snprintf(vpcif->name, INTERFACE_ID_LEN, "%s", gniif->name);
            vpcif->gniInst = gniif;
            gniif->mido_present = vpcif;
            vpcif->host_changed = 1;
            vpcif->srcdst_changed = 1;
            vpcif->pubip_changed = 1;
            vpcif->sg_changed = 1;
            vpcif->eniid_changed = 1;
            LOGINFO("\tcreating %s\n", gniif->name);
        }

        if (vpcif->midopresent) {
            LOGTRACE("\t\tskipping pass3 for %s\n", gniif->name);
            continue;
        } else {
            rc = create_mido_vpc_instance(vpcif);
            if (rc) {
                LOGERROR("failed to create VPC instance %s: check midonet health\n", gniif->name);
                rc = delete_mido_vpc_instance(mido, vpc, vpcsubnet, vpcif);
                if (rc) {
                    LOGERROR("failed to cleanup %s\n", gniif->name);
                }
                ret++;
                continue;
            }
        }
        vpcif->gnipresent = 1;

        int ecnt = ret;
        // check for potential VMHOST change
        if (!vpcif->population_failed && !vpcif->host_changed) {
            LOGTRACE("\t\t%s host did not change\n", gniif->name);
        } else {
            gni_instance_node = mido_get_host_byname(gniif->node);
            if (!gni_instance_node) {
                LOGERROR("\thost %s for %s not found: check midonet and/or midolman health\n", gniif->node, gniif->name);
                continue;
            } else {
                if (vpcif->midos[INST_VMHOST] && vpcif->midos[INST_VMHOST]->init) {
                    if ((gni_instance_node->obj == vpcif->midos[INST_VMHOST]) ||
                            (!strcmp(gni_instance_node->obj->uuid, vpcif->midos[INST_VMHOST]->uuid))) {
                        LOGTRACE("\t\t%s host did not change.\n", gniif->name);
                        vpcif->host_changed = 0;
                    } else {
                        LOGINFO("\t%s vmhost change detected.\n", gniif->name);
                        disconnect_mido_vpc_instance(vpcsubnet, vpcif);
                    }
                }
                vpcif->midos[INST_VMHOST] = gni_instance_node->obj;
            }
        }

        // do instance/interface-host connection
        if (vpcif->host_changed || vpcif->population_failed || mido->config->eucanetd_first_update) {
            LOGTRACE("\tconnecting mido host %s with interface %s\n",
                    vpcif->midos[INST_VMHOST]->name, gniif->name);
            rc = connect_mido_vpc_instance(mido, vpcsubnet, vpcif, gni->instanceDNSDomain);
            if (rc) {
                LOGERROR("failed to connect %s to %s: check midolman\n", gniif->name, vpcif->midos[INST_VMHOST]->name);
            }
        }

        // check public/elastic IP changes
        if (!vpcif->population_failed && !vpcif->pubip_changed) {
            LOGTRACE("\t\t%s pubip did not change\n", gniif->name);
        } else {
            if (gniif->publicIp == vpcif->pubip) {
                LOGTRACE("\t\t%s pubip did not change.\n", gniif->name);
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
        if (vpcif->population_failed || vpcif->pubip_changed || !vpcif->population_failed) {
            // Do not run connect for private interfaces
            if (gniif->publicIp != 0) {
                rc = connect_mido_vpc_instance_elip(mido, vpc, vpcsubnet, vpcif);
                if (rc) {
                    LOGERROR("failed to setup public/elastic IP for %s\n", gniif->name);
                    ret++;
                }
            } else {
                // Create private ip-address-group IP
                rc = mido_create_ipaddrgroup_ip(vpcif->iag_post, vpcif->midos[INST_ELIP_POST_IPADDRGROUP],
                        hex2dot_s(vpcif->gniInst->privateIp), &(vpcif->midos[INST_ELIP_POST_IPADDRGROUP_IP]));
                if (rc) {
                    LOGERROR("Failed to add %s as member of ipag\n", hex2dot_s(vpcif->gniInst->privateIp));
                    ret++;
                }
            }
        }
            
        // do instance/interface md connection
        if (mido->config->enable_mido_md) {
            if (vpcif->population_failed || vpcif->eniid_changed) {
                if (vpcif->eniid == 0) {
                    get_next_eni_id(mido, &(vpcif->eniid));
                }
                vpcif->eniid_changed = 0;
                if (vpcif->eniid != 0) {
                    rc = connect_mido_vpc_instance_md(mido, vpc, vpcsubnet, vpcif);
                    if (rc) {
                        LOGERROR("failed to setup md IP for %s\n", gniif->name);
                        ret++;
                    } else {
                        char *internalIp = hex2dot(mido->mdconfig.mdnw + vpcif->eniid);
                        fprintf(INTIPMAPFH, "%s %s %s %s new\n", SP(internalIp), SP(vpcif->name),
                                SP(vpcif->gniInst->vpc), SP(vpcif->gniInst->subnet));
                        EUCA_FREE(internalIp);
                    }
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
        dot2[0] = '\0';
        dot3[0] = '\0';
        cidr_split(vpcsubnet->gniSubnet->cidr, NULL, NULL, gw_buf, dot2, dot3);

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
                LOGTRACE("\tdeleting L3 rule src for %s\n", gniif->name);
                rc = mido_delete_rule(vpcif->prechain, ptmpmn);
                if (rc) {
                    LOGWARN("Failed to delete src IP check rule for %s\n", gniif->name);
                    ret++;
                }
            }
        } else {
            if ((!mido->disable_l2_isolation) && (gniif->srcdstcheck)) {
                LOGTRACE("\tcreating L3 rule src for %s\n", gniif->name);
                rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                        NULL, &rulepos, "position", pos_str, "type", "drop", "dlType", "2048",
                        "nwSrcAddress", instIp, "nwSrcLength", "32", "invNwSrc", "true", NULL);
                if (rc) {
                    LOGWARN("Failed to create src IP check rule for %s\n", gniif->name);
                    ret++;
                }
            }
        }

        // block any incoming IP traffic that isn't destined to the VM private IP
        // Check if the rule is already in place
        rc = mido_find_rule_from_list(vpcif->postchain->rules, vpcif->postchain->max_rules, &ptmpmn,
                "type", "drop", "dlType", "2048", "nwDstAddress", instIp, "nwDstLength", "32", "invNwDst", "true", NULL);
        if ((rc == 0) && ptmpmn && (ptmpmn->init == 1)) {
            if ((mido->disable_l2_isolation) || (!gniif->srcdstcheck)) {
                LOGTRACE("\tdeleting L3 rule dst for %s\n", gniif->name);
                rc = mido_delete_rule(vpcif->postchain, ptmpmn);
                if (rc) {
                    LOGWARN("Failed to delete dst IP check rule for %s\n", gniif->name);
                    ret++;
                }
            }
        } else {
            if ((!mido->disable_l2_isolation) && (gniif->srcdstcheck)) {
                LOGTRACE("\tcreating L3 rule dst for %s\n", gniif->name);
                rc = mido_create_rule(vpcif->postchain, vpcif->midos[INST_POSTCHAIN],
                        NULL, &rulepos, "position", pos_str, "type", "drop", "dlType", "2048",
                        "nwDstAddress", instIp, "nwDstLength", "32", "invNwDst", "true", NULL);
                if (rc) {
                    LOGWARN("Failed to create dst IP check rule for %s\n", gniif->name);
                    ret++;
                }
            }
        }

        // Allow DHCP responses (this rule needs to be placed before dst check rule)
        // Check if the rule is already in place
        rc = mido_find_rule_from_list(vpcif->postchain->rules, vpcif->postchain->max_rules, &ptmpmn,
                "type", "accept", "nwProto", "17", "tpDst", "jsonjson", "tpDst:start", "67", "tpDst:end", "68",
                "tpDst:END", "END", NULL);
        if ((rc == 0) && ptmpmn && (ptmpmn->init == 1)) {
            if ((mido->disable_l2_isolation) || (!gniif->srcdstcheck)) {
                LOGTRACE("\tdeleting DHCP rule for %s\n", gniif->name);
                rc = mido_delete_rule(vpcif->postchain, ptmpmn);
                if (rc) {
                    LOGWARN("Failed to delete DHCP rule for %s\n", gniif->name);
                    ret++;
                }
            }
        } else {
            if ((!mido->disable_l2_isolation) && (gniif->srcdstcheck)) {
                LOGTRACE("\tcreating DHCP rule for %s\n", gniif->name);
                rc = mido_create_rule(vpcif->postchain, vpcif->midos[INST_POSTCHAIN],
                        NULL, &rulepos, "position", pos_str, "type", "accept", "nwProto", "17",
                        "tpDst", "jsonjson", "tpDst:start", "67", "tpDst:end", "68",
                        "tpDst:END", "END", NULL);
                if (rc) {
                    LOGWARN("Failed to create DHCP rule for %s\n", gniif->name);
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
        if (!mido->config->enable_mido_md) {
            // metadata redirect egress
            rulepos = vpcif->prechain->rules_count + 1;
            snprintf(pos_str, 32, "%d", rulepos);
            rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                    NULL, &rulepos,
                    "position", pos_str, "type", "dnat", "flowAction", "continue",
                    "ipAddrGroupDst", mido->midocore->midos[CORE_METADATA_IPADDRGROUP]->uuid,
                    "nwProto", "6", "tpDst", "jsonjson", "tpDst:start", "80", "tpDst:end", "80",
                    "tpDst:END", "END", "natTargets", "jsonlist", "natTargets:addressTo", dot3,
                    "natTargets:addressFrom", dot3, "natTargets:portFrom",
                    "8008", "natTargets:portTo", "8008", "natTargets:END", "END", NULL);
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
                    "nwSrcAddress", dot3, "nwSrcLength", "32", "nwProto", "6",
                    "tpSrc", "jsonjson", "tpSrc:start", "8008", "tpSrc:end", "8008", "tpSrc:END", "END",
                    "natTargets", "jsonlist", "natTargets:addressTo", "169.254.169.254",
                    "natTargets:addressFrom", "169.254.169.254", "natTargets:portFrom", "80",
                    "natTargets:portTo", "80", "natTargets:END", "END", NULL);
            if (rc) {
                LOGWARN("Failed to create MD snat rule for %s\n", gniif->name);
                ret++;
            }
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

        // plus two/three accept for metaproxy-based md, and dot2 DNS egress 
        rulepos = vpcif->prechain->rules_count + 1;
        snprintf(pos_str, 32, "%d", rulepos);
        rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                NULL, &rulepos,
                "position", pos_str, "type", "accept", "nwDstAddress", dot2, "nwDstLength", "31", NULL);
        if (rc) {
            LOGWARN("Failed to create egress +2/+3 rule for %s\n", gniif->name);
            ret++;
        }

        // midomd subnet accept (169.254.169.248/29)
        cidr_split(mido->config->mido_extmdcidr, subnet_buf, slashnet_buf, NULL, NULL, NULL);
        rulepos = vpcif->prechain->rules_count + 1;
        snprintf(pos_str, 32, "%d", rulepos);
        rc = mido_create_rule(vpcif->prechain, vpcif->midos[INST_PRECHAIN],
                NULL, &rulepos,
                "position", pos_str, "type", "accept", "nwDstAddress", subnet_buf,
                "nwDstLength", slashnet_buf, NULL);
        if (rc) {
            LOGWARN("Failed to create egress +2/+3 rule for %s\n", gniif->name);
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
            LOGTRACE("\t\t%s sec groups did not change\n", gniif->name);
        } else {
            // Get all SG jump rules from interface chains
            rc = mido_get_jump_rules(vpcif->prechain, &jprules_egress, &max_jprules_egress,
                    &jprules_tgt_egress, &max_jprules_egress);
            rc = mido_get_jump_rules(vpcif->postchain, &jprules_ingress, &max_jprules_ingress,
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
                                LOGTRACE("\t\tegress jump to %s found.\n", vpcsecgroup->name);
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

                        found = 0;
                        for (k = 0; k < max_jprules_ingress && !found; k++) {
                            if (!strcmp(vpcsecgroup->midos[VPCSG_INGRESS]->uuid, jprules_tgt_ingress[k])) {
                                LOGTRACE("\t\tingress jump to %s found.\n", vpcsecgroup->name);
                                jpi_gni_present[k] = 1;
                                found = 1;
                            }
                        }
                        if (!found) {
                            // add the SG chain jump ingress - right before the drop rule
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
                        LOGWARN("failed to delete egress jump rule\n");
                    }
                }
            }
            for (j = 0; j < max_jprules_ingress; j++) {
                if (jpi_gni_present[j] == 0) {
                    rc = mido_delete_rule(vpcif->postchain, jprules_ingress[j]);
                    if (rc != 0) {
                        LOGWARN("failed to delete ingress jump rule\n");
                    }
                }
            }

            EUCA_FREE(jprules_egress);
            EUCA_FREE(jprules_tgt_egress);
            EUCA_FREE(jpe_gni_present);
            EUCA_FREE(jprules_ingress);
            EUCA_FREE(jprules_tgt_ingress);
            EUCA_FREE(jpi_gni_present);
            
            if (ecnt != ret) {
                vpcif->population_failed = 1;
            } else {
                vpcif->population_failed = 0;
            }
        }

        LOGDEBUG("\t%s implemented in %.2f ms\n", vpcif->name, eucanetd_timer_usec(&tv) / 1000.0);
    }
    
    if (INTIPMAPFH) {
        fclose(INTIPMAPFH);
    }
    
    return (ret);
}

/**
 * Implements network acls (create mido objects) as described in GNI.
 * @param gni [in] Global Network Information to be applied.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. 1 otherwise.
 */
int do_midonet_update_pass3_nacls(globalNetworkInfo *gni, mido_config *mido) {
    int rc = 0;
    int ret = 0;
    mido_parsed_chain_rule crule;

    for (int i = 0; i < gni->max_vpcs; i++) {
        gni_vpc *gnivpc = &(gni->vpcs[i]);
        mido_vpc *vpc = (mido_vpc *) gnivpc->mido_present;
        if (!vpc) {
            LOGWARN("Unable to process network acls for %s\n", gnivpc->name);
            continue;
        }
        for (int j = 0; j < gnivpc->max_networkAcls; j++) {
            gni_network_acl *gninacl = &(gnivpc->networkAcls[j]);
            mido_vpc_nacl *vpcnacl = (mido_vpc_nacl *) gninacl->mido_present;
            if (vpcnacl != NULL) {
                LOGTRACE("found gni %s already extant\n", gninacl->name);
            } else {
                LOGINFO("\tcreating %s\n", gninacl->name);
                // necessary memory should have been allocated in pass1
                vpcnacl = &(vpc->nacls[vpc->max_nacls]);
                memset(vpcnacl, 0, sizeof (mido_vpc_nacl));
                vpc->max_nacls++;
                snprintf(vpcnacl->name, NETWORK_ACL_ID_LEN, "%s", gninacl->name);
                snprintf(vpcnacl->vpcname, VPC_ID_LEN, "%s", vpc->name);
                vpcnacl->vpc = vpc;
                vpcnacl->gniNacl = gninacl;
                gninacl->mido_present = vpcnacl;
                vpcnacl->egress_changed = 1;
                vpcnacl->ingress_changed = 1;
            }

            if (vpcnacl->midopresent) {
                // nacl presence test passed in pass1
                LOGTRACE("\t\t%s found in mido\n", gninacl->name);
            } else {
                rc = create_mido_vpc_nacl(mido, vpc, vpcnacl);
                if (rc) {
                    LOGERROR("cannot create mido network acl %s: check midonet health\n", vpcnacl->name);
                    rc = delete_mido_vpc_nacl(mido, vpcnacl);
                    if (rc) {
                        LOGERROR("failed to cleanup %s\n", gninacl->name);
                    }
                    ret++;
                    continue;
                } else {
                    vpcnacl->population_failed = 0;
                }
            }
            vpcnacl->gnipresent = 1;

            // Process ingress rules
            if (!vpcnacl->population_failed && !vpcnacl->ingress_changed) {
                LOGTRACE("\t\tskipping pass3 for %s ingress\n", gninacl->name);
            } else {
                mido_clear_rules(vpcnacl->ingress);
                for (int k = 0; k < gninacl->max_ingress; k++) {
                    gni_acl_entry *acl_entry = &(gninacl->ingress[k]);
                    parse_mido_nacl_entry(mido, acl_entry, &crule);
                    rc = create_mido_vpc_nacl_entry(vpcnacl->ingress, NULL, -1,
                            MIDO_RULE_ACLENTRY_INGRESS, &crule);
                    if (rc) {
                        LOGWARN("failed to create %s ingress rule at idx %d\n", gninacl->name, k);
                        ret++;
                    }
                }
            }

            // Process egress rules
            if (!vpcnacl->population_failed && !vpcnacl->egress_changed) {
                LOGTRACE("\t\tskipping pass3 for %s egress\n", gninacl->name);
            } else {
                mido_clear_rules(vpcnacl->egress);
                for (int k = 0; k < gninacl->max_egress; k++) {
                    gni_acl_entry *acl_entry = &(gninacl->egress[k]);
                    parse_mido_nacl_entry(mido, acl_entry, &crule);
                    rc = create_mido_vpc_nacl_entry(vpcnacl->egress, NULL, -1,
                            MIDO_RULE_ACLENTRY_EGRESS, &crule);
                    if (rc) {
                        LOGWARN("failed to create %s ingress rule at idx %d\n", gninacl->name, k);
                        ret++;
                    }
                }
            }

            // Attach NACL chains to bridges
            for (int k = 0; k < vpc->max_subnets; k++) {
                mido_vpc_subnet *subnet = &(vpc->subnets[k]);
                
                if (strlen(subnet->name) == 0) {
                    continue;
                }

                int rulepos = 1;
                char rulepos_str[8];
                char subnet_buf[NETWORK_ADDR_LEN];
                char slashnet_buf[NETWORK_ADDR_LEN];

                subnet_buf[0] = slashnet_buf[0] = '\0';
                cidr_split(subnet->gniSubnet->cidr, subnet_buf, slashnet_buf, NULL, NULL, NULL);

                snprintf(rulepos_str, 8, "%d", rulepos);

                if (!subnet->inchain || !subnet->outchain) {
                    LOGWARN("%s is missing infilter or outfilter\n", subnet->name);
                    continue;
                }

                // subnet inbound default rules
                if (subnet->inchain->rules_count < 4) {
                    // allow traffic from reserved IP addresses (subnet/30) 
                    rc = mido_create_rule(subnet->inchain, NULL, NULL, &rulepos,
                            "position", rulepos_str, "type", "accept", "nwSrcAddress",
                            subnet_buf, "nwSrcLength", "30", NULL);
                    if (rc) {
                        LOGWARN("Failed to create %s inbound reserved IPs rule\n", subnet->name);
                        ret++;
                    }

                    // allow link-local traffic (169.254.0.0/16)
                    rc = mido_create_rule(subnet->inchain, NULL, NULL, &rulepos,
                            "position", rulepos_str, "type", "accept", "nwSrcAddress",
                            "169.254.0.0", "nwSrcLength", "16", NULL);
                    if (rc) {
                        LOGWARN("Failed to create %s inbound link-local rule\n", subnet->name);
                        ret++;
                    }

                    // allow incoming DHCP (UDP source port 67)
                    rc = mido_create_rule(subnet->inchain, NULL, NULL, &rulepos,
                            "position", rulepos_str, "type", "accept", "nwProto", "17",
                            "tpDst", "jsonjson", "tpDst:start", "67", "tpDst:end", "68",
                            "tpDst:END", "END", NULL);
                    if (rc) {
                        LOGWARN("Failed to create %s inbound DHCP rule\n", subnet->name);
                        ret++;
                    }

                    // allow intra-subnet traffic
                    rc = mido_create_rule(subnet->inchain, NULL, NULL, &rulepos,
                            "position", rulepos_str, "type", "accept", "nwSrcAddress",
                            subnet_buf, "nwSrcLength", slashnet_buf, NULL);
                    if (rc) {
                        LOGWARN("Failed to create %s inbound intra-subnet rule\n", subnet->name);
                        ret++;
                    }
                }

                // subnet outbound default rules
                if (subnet->outchain->rules_count < 4) {
                    // allow traffic to reserved IP addresses (subnet/30) 
                    rc = mido_create_rule(subnet->outchain, NULL, NULL, &rulepos,
                            "position", rulepos_str, "type", "accept", "nwDstAddress",
                            subnet_buf, "nwDstLength", "30", NULL);
                    if (rc) {
                        LOGWARN("Failed to create %s outbound reserved IPs rule\n", subnet->name);
                        ret++;
                    }

                    // allow link-local traffic (169.254.0.0/16)
                    rc = mido_create_rule(subnet->outchain, NULL, NULL, &rulepos,
                            "position", rulepos_str, "type", "accept", "nwDstAddress",
                            "169.254.0.0", "nwDstLength", "16", NULL);
                    if (rc) {
                        LOGWARN("Failed to create %s outbound link-local rule\n", subnet->name);
                        ret++;
                    }

                    // allow outgoing DHCP (UDP source port 67)
                    rc = mido_create_rule(subnet->outchain, NULL, NULL, &rulepos,
                            "position", rulepos_str, "type", "accept", "nwProto", "17",
                            "tpDst", "jsonjson", "tpDst:start", "67", "tpDst:end", "68",
                            "tpDst:END", "END", NULL);
                    if (rc) {
                        LOGWARN("Failed to create %s outbound DHCP rule\n", subnet->name);
                        ret++;
                    }

                    // allow intra-subnet traffic
                    rc = mido_create_rule(subnet->outchain, NULL, NULL, &rulepos,
                            "position", rulepos_str, "type", "accept", "nwDstAddress",
                            subnet_buf, "nwDstLength", slashnet_buf, NULL);
                    if (rc) {
                        LOGWARN("Failed to create %s outbound intra-subnet rule\n", subnet->name);
                        ret++;
                    }
                }

                // jump rules to NACL chains
                if (subnet->nacl_changed && !strcmp(subnet->gniSubnet->networkAcl_name, vpcnacl->name)) {
                    midoname **jprules_out = NULL;
                    char **jprules_tgt_out = NULL;
                    int max_jprules_out = 0;
                    midoname **jprules_in = NULL;
                    char **jprules_tgt_in = NULL;
                    int max_jprules_in = 0;

                    // Get all jump rules from subnet bridge chains
                    rc = mido_get_jump_rules(subnet->outchain, &jprules_out, &max_jprules_out,
                            &jprules_tgt_out, &max_jprules_out);
                    rc = mido_get_jump_rules(subnet->inchain, &jprules_in, &max_jprules_in,
                            &jprules_tgt_in, &max_jprules_in);

                    // Only one jump rule is expected
                    if (max_jprules_out > 1) {
                        LOGWARN("%s inconsistent outfilter chain: %d jump rules found\n", subnet->name, max_jprules_out);
                        for (int m = 0; m < max_jprules_out; m++) {
                            rc = mido_delete_rule(subnet->outchain, jprules_out[m]);
                            if (rc != 0) {
                                LOGWARN("failed to delete %s outfilter jump rule\n", subnet->name);
                            }
                        }
                        max_jprules_out = 0;
                    }
                    if (max_jprules_in > 1) {
                        LOGWARN("%s inconsistent infilter chain: %d jump rules found\n", subnet->name, max_jprules_in);
                        for (int m = 0; m < max_jprules_in; m++) {
                            rc = mido_delete_rule(subnet->inchain, jprules_in[m]);
                            if (rc != 0) {
                                LOGWARN("failed to delete %s infilter jump rule\n", subnet->name);
                            }
                        }
                        max_jprules_in = 0;
                    }
                    
                    // Check if jump rule to NACL (egress) is in place
                    if ((max_jprules_out == 1) && (!strcmp(jprules_tgt_out[0], vpcnacl->egress->obj->uuid))) {
                        LOGTRACE("\t\tskipping %s->%s outfilter jump rule\n", subnet->name, vpcnacl->name);                            
                    } else {
                        if (max_jprules_out > 0) {
                            rc = mido_delete_rule(subnet->outchain, jprules_out[0]);
                            if (rc == 0) {
                                LOGWARN("failed to delete %s outfilter jump rule\n", vpcnacl->name);
                            }
                        }
                        LOGTRACE("\t\tcreating %s->%s outfilter jump rule\n", subnet->name, vpcnacl->name);
                        snprintf(rulepos_str, 8, "%d", subnet->outchain->rules_count + 1);
                        rc = mido_create_rule(subnet->outchain, NULL, NULL, &rulepos,
                                "position", rulepos_str, "type", "jump", "jumpChainId",
                                vpcnacl->egress->obj->uuid, NULL);
                        if (rc) {
                            LOGWARN("Failed to create %s outfilter jump rule\n", subnet->name);
                            ret++;
                        }
                    }
                    
                    // Check if jump rule to NACL (ingress) is in place
                    if ((max_jprules_in == 1) && (!strcmp(jprules_tgt_in[0], vpcnacl->ingress->obj->uuid))) {
                        LOGTRACE("\t\tskipping %s->%s infilter jump rule\n", subnet->name, vpcnacl->name);                            
                    } else {
                        if (max_jprules_in > 0) {
                            rc = mido_delete_rule(subnet->inchain, jprules_in[0]);
                            if (rc == 0) {
                                LOGWARN("failed to delete %s infilter jump rule\n", vpcnacl->name);
                            }
                        }
                        LOGTRACE("\t\tcreating %s->%s infilter jump rule\n", subnet->name, vpcnacl->name);
                        snprintf(rulepos_str, 8, "%d", subnet->inchain->rules_count + 1);
                        rc = mido_create_rule(subnet->inchain, NULL, NULL, &rulepos,
                                "position", rulepos_str, "type", "jump", "jumpChainId",
                                vpcnacl->ingress->obj->uuid, NULL);
                        if (rc) {
                            LOGWARN("Failed to create %s infilter jump rule\n", subnet->name);
                            ret++;
                        }
                    }

                    EUCA_FREE(jprules_out);
                    EUCA_FREE(jprules_tgt_out);
                    EUCA_FREE(jprules_in);
                    EUCA_FREE(jprules_tgt_in);
                }
            }
        }
    }
    return (ret);
}

/**
 * Executes VPCMIDO maintenance.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. 1 otherwise.
 */
int do_midonet_maint(mido_config *mido) {
    if (!mido) {
        return (1);
    }

    if ((maint_c % 30) == 0) {
        maint_c = 0;
    }
    midoname_list *ml = midonet_api_cache_midos_get();
    if (ml) {
        ml->released += 1;
    }

    // Check for number of midoname releases in midocache_midos
    midonet_api_cache_check();
    
    if (!midocache_invalid) {
        if (maint_c == 0) {
            midonet_api_cache *tmpcache = EUCA_ZALLOC_C(1, sizeof (midonet_api_cache));
            midonet_api_cache_midos_init();
            midonet_api_cache_refresh_hosts(tmpcache);
            midonet_api_cache_refresh_tunnelzones(tmpcache);
            if ((midonet_api_cache_get_nhosts(tmpcache) != midonet_api_cache_get_nhosts(NULL)) ||
                    (midonet_api_cache_get_ntzhosts(tmpcache) != midonet_api_cache_get_ntzhosts(NULL))) {
                midocache_invalid = 1;
            }
            midonet_api_cache_flush(tmpcache);
        }
        maint_c++;
    }

    if (midocache_invalid) {
        eucanetd_emulate_sigusr2();
    }

    return (0);
}

/**
 * Executes a VPCMIDO update based on the Global Network Information.
 * @param gni [in] current global network state.
 * @param appliedGni [in] most recently applied global network state.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. Positive integer when failures are detected. -2 if
 * failures are detected on instances/interfaces creation. -1 if failures are
 * detected on gateway(s) processing.
 */
int do_midonet_update(globalNetworkInfo *gni, globalNetworkInfo *appliedGni, mido_config *mido) {
    int rc = 0, ret = 0;
    struct timeval tv;

    if (!gni || !mido || !mido->config) {
        return (1);
    }
    if (appliedGni == NULL) {
        midocache_invalid = 1;
    }

    eucanetd_timer_usec(&tv);
    mido->enabledCLCIp = gni->enabledCLCIp;

    if (!midocache_invalid) {
        clear_mido_gnitags(mido);
        LOGTRACE("\tgni/mido tags cleared in %ld us.\n", eucanetd_timer_usec(&tv));
    } else {
        midocache_invalid = 0;

        //rc = midonet_api_cache_refresh();
        rc = midonet_api_cache_refresh_v_threads(MIDO_CACHE_REFRESH_ALL);
        if (rc) {
            LOGERROR("failed to retrieve objects from MidoNet.\n");
            mido->config->eucanetd_err = EUCANETD_ERR_VPCMIDO_API;
            return (1);
        }
        LOGINFO("\tMidoNet objects cached in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

        // Check tunnel-zone
        if (!mido->midotz_ok) {
            LOGDEBUG("Checking MidoNet tunnel-zone.\n");
        }
        int msg_len = 2048;
        char *buffer = alloca(msg_len);
        buffer[msg_len - 1] = '\0';
        char *msg = buffer;
        while (!mido->midotz_ok) {
            // Refresh MN data
            midonet_api_cache_refresh_tunnelzones(NULL);
            midonet_api_cache_iphostmap_populate(NULL);
            // Check tunnel-zone
            rc = check_mido_tunnelzone(gni, &msg, &msg_len);
            if (rc) {
                if (strlen(msg)) {
                    LOGWARN("%s", buffer);
                    msg = NULL;
                }
                if (sig_rcvd) {
                    sig_rcvd = 0;
                    break;
                } else {
                    sleep(3);
                }
            } else {
                mido->midotz_ok = TRUE;
            }
        }
        if (!mido->midotz_ok) {
            LOGERROR("Cannot proceed without a valid tunnel-zone.\n");
            mido->config->eucanetd_err = EUCANETD_ERR_VPCMIDO_TZ;
            return (1);
        }

        rc = do_midonet_populate(mido);
        if (rc) {
            LOGWARN("failed to populate euca VPC models.\n");
        }
        LOGINFO("\tVPCMIDO models populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

        // Check unconnected objects in MN if errors were detected
        if (mido->config->eucanetd_first_update || mido->config->eucanetd_err) {
            LOGINFO("Checking unconnected objects:\n");
            if (do_midonet_delete_unconnected(mido, FALSE) < 0) {
                return (1);
            }            
        }

        // Check mido_arptable config changes
        if (mido->config->mido_arptable_config_changed || mido->config->eucanetd_first_update) {
            // Clear arp_table and mac_table
            if (!mido->config->enable_mido_arptable) {
                do_midonet_clean_arpmactables(mido);
            }
            // arp_table and mac_table will be populated during eucanetd iteration if enabled
        }
        
        // Check mido_md config changes
        if (mido->config->mido_md_config_changed || mido->config->eucanetd_first_update) {
            mido->config->mido_md_config_changed = FALSE;
            // Disable eucamd
            if (!mido->config->enable_mido_md) {
                disable_mido_md(mido);
                // Delete all instance/interface chains and force full eucanetd iteration
                if (mido_get_router(VPCMIDO_CORERT) && mido->config->populate_mido_md) {
                    do_delete_vpceni_chains(mido);
                    return (1);
                }
            }
            // Disable metaproxy-based MD
            if (mido->config->enable_mido_md) {
                do_metaproxy_disable(mido);
                do_delete_meta_nslinks(mido);
                if (mido_get_router(VPCMIDO_CORERT) && !mido->config->populate_mido_md) {
                    do_delete_vpceni_chains(mido);
                    return (1);
                }
            }
        }
        
        // make sure that all core objects are in place
        midonet_api_system_changed = 0;
        rc = create_mido_core(mido, mido->midocore);
        if (rc) {
            if (rc == -1) {
                LOGWARN("failures detected when setting up gateway(s).\n");
                LOGINFO("Check gateway and midonet health. Instances access to public network may not work.\n");
            } else {
                LOGERROR("failed to setup midonet core: check midonet health\n");
                mido->config->eucanetd_err = EUCANETD_ERR_VPCMIDO_CORE;
                return (1);
            }
        }
        if (midonet_api_system_changed == 1) {
            LOGINFO("\tvpcmido core created in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
        } else {
            LOGINFO("\tvpcmido core maint in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
        }
        
        // create mido md objects
        if (mido->config->enable_mido_md) {
            midonet_api_system_changed = 0;
            rc = create_mido_md(mido);
            if (rc) {
                LOGERROR("failed to setup midonet md: check midonet health\n");
                mido->config->eucanetd_err = EUCANETD_ERR_VPCMIDO_MD;
                return (1);
            }
            if (midonet_api_system_changed == 1) {
                LOGINFO("\tvpcmido md created in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
            } else {
                LOGINFO("\tvpcmido md maint in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
            }
        }
        
        mido_info_http_count();
        midonet_api_system_changed = 0;
    }

    eucanetd_timer_usec(&tv);
    rc = do_midonet_update_pass1(gni, appliedGni, mido);
    if (rc) {
        LOGERROR("pass1: failed update - check midonet health\n");
        ret++;
        mido->config->eucanetd_err = EUCANETD_ERR_VPCMIDO_PASS1;
        return (ret);
    }
    LOGINFO("\tgni/mido tagging processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    rc = do_midonet_update_pass2(gni, mido);
    if (rc) {
        LOGERROR("pass2: failed update - check midonet health\n");
        ret++;
        mido->config->eucanetd_err = EUCANETD_ERR_VPCMIDO_PASS2;
        return (ret);
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
        ret++;
        mido->config->eucanetd_err = EUCANETD_ERR_VPCMIDO_VPCS;
        return (ret);
    }
    LOGINFO("\tvpcs processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    rc = do_midonet_update_pass3_sgs(gni, mido);
    if (rc) {
        LOGERROR("pass3_sgs: failed update - check midonet health\n");
        ret++;
        mido->config->eucanetd_err = EUCANETD_ERR_VPCMIDO_SGS;
        return (ret);
    }
    LOGINFO("\tsgs processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    rc = do_midonet_update_pass3_insts(gni, mido);
    if (rc) {
        LOGERROR("pass3_insts: failed update - check midonet health\n");
        mido->config->eucanetd_err = EUCANETD_ERR_VPCMIDO_ENIS;
        return (-2);
    }
    LOGINFO("\tinstances processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    rc = do_midonet_update_pass3_nacls(gni, mido);
    if (rc) {
        LOGERROR("pass3_nacls: failed update - check midonet health\n");
        ret++;
        mido->config->eucanetd_err = EUCANETD_ERR_VPCMIDO_NACLS;
        return (ret);
    }
    LOGINFO("\tnetwork acls processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    mido_info_http_count();
    return (ret);
}

/**
 * Allocates a free internal VPCMIDO router ID.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param nextid [out] the newly allocated router id
 * @return 0 on success. Positive integer otherwise.
 */
int get_next_router_id(mido_config *mido, int *nextid) {
    int i;
    for (i = 2; i < mido->config->mido_max_rtid; i++) {
        // Skip IDs that conflict with 169.254.41.248 through 169.254.41.255
        if ((i >= RTID_169_248) && (i <= RTID_169_255)) {
            continue;
        }
        if (!mido->rt_ids[i]) {
            set_router_id(mido, i);
            if (nextid) {
                *nextid = i;
            }
            return (0);
        }
    }
    return (1);
}

/**
 * Marks the router ID id as in use.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param id [in] router id to mark as used
 * @return 0 on success. Positive integer otherwise.
 */
int set_router_id(mido_config *mido, int id) {
    if (id < mido->config->mido_max_rtid) {
        mido->rt_ids[id] = TRUE;
        LOGTRACE("router id %d allocated.\n", id);
        return (0);
    }
    return (1);
}

/**
 * Clears the use flag of a router ID.
 * @param mido [in] current mido_config data structure.
 * @param id [in] router ID of interest.
 * @return 0 on success. Positive integer otherwise.
 */
int clear_router_id(mido_config *mido, int id) {
    if (id < mido->config->mido_max_rtid) {
        mido->rt_ids[id] = FALSE;
        LOGTRACE("router id %d released.\n", id);
        return (0);
    }
    return (1);
}

/**
 * Allocates a free internal VPCMIDO ENI ID.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param nextid [out] the newly allocated eni id
 * @return 0 on success. Positive integer otherwise.
 */
int get_next_eni_id(mido_config *mido, int *nextid) {
    int i;
    for (i = 10; i < mido->config->mido_max_eniid; i++) {
        if (!mido->eni_ids[i]) {
            set_eni_id(mido, i);
            if (nextid) {
                *nextid = i;
            }
            return (0);
        }
    }
    return (1);
}

/**
 * Marks the ENI ID id as in use.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param id [in] eni id to mark as used
 * @return 0 on success. Positive integer otherwise.
 */
int set_eni_id(mido_config *mido, int id) {
    if (id < mido->config->mido_max_eniid) {
        mido->eni_ids[id] = TRUE;
        LOGTRACE("eni id %d allocated.\n", id);
        return (0);
    }
    return (1);
}

/**
 * Clears the use flag of an ENI ID.
 * @param mido [in] current mido_config data structure.
 * @param id [in] eni id of interest.
 * @return 0 on success. Positive integer otherwise.
 */
int clear_eni_id(mido_config *mido, int id) {
    if (id < mido->config->mido_max_eniid) {
        mido->eni_ids[id] = FALSE;
        LOGTRACE("eni id %d released.\n", id);
        return (0);
    }
    return (1);
}

/**
 * Logs the VPC vpc state
 * @param vpc [in] data structure holding information about the VPC of interest
 */
void print_mido_vpc(mido_vpc *vpc) {
    LOGTRACE("PRINT VPC: name=%s max_subnets=%d gnipresent=%d\n", vpc->name, vpc->max_subnets, vpc->gnipresent);
    mido_print_midoname(vpc->midos[VPC_VPCRT]);
    mido_print_midoname(vpc->midos[VPC_EUCABR_DOWNLINK]);
    mido_print_midoname(vpc->midos[VPC_VPCRT_UPLINK]);
    mido_print_midoname(vpc->midos[VPC_VPCRT_UPLINK_PRECHAIN]);
    mido_print_midoname(vpc->midos[VPC_VPCRT_UPLINK_POSTCHAIN]);
}

/**
 * Logs the VPC subnet vpcsubnet state
 * @param vpcsubnet [in] data structure holding information about the VPC subnet of interest
 */
void print_mido_vpc_subnet(mido_vpc_subnet *vpcsubnet) {
    LOGTRACE("PRINT VPCSUBNET: name=%s vpcname=%s max_instances=%d gnipresent=%d\n", vpcsubnet->name, vpcsubnet->vpcname,
            vpcsubnet->max_instances, vpcsubnet->gnipresent);
    mido_print_midoname(vpcsubnet->midos[SUBN_BR]);
    mido_print_midoname(vpcsubnet->midos[SUBN_BR_RTPORT]);
    mido_print_midoname(vpcsubnet->midos[SUBN_VPCRT_BRPORT]);
    mido_print_midoname(vpcsubnet->midos[SUBN_BR_DHCP]);
}

/**
 * Logs the state of VPC instance vpcinstance.
 * @param vpcinstance [in] data structure holding information about the VPC instance of interest.
 */
void print_mido_vpc_instance(mido_vpc_instance *vpcinstance) {
    LOGTRACE("PRINT VPCINSTANCE: name=%s gnipresent=%d\n", vpcinstance->name, vpcinstance->gnipresent);
    mido_print_midoname(vpcinstance->midos[INST_VPCBR_VMPORT]);
    mido_print_midoname(vpcinstance->midos[INST_VPCBR_DHCPHOST]);
    mido_print_midoname(vpcinstance->midos[INST_VMHOST]);
    mido_print_midoname(vpcinstance->midos[INST_ELIP_PRE]);
    mido_print_midoname(vpcinstance->midos[INST_ELIP_POST]);
}

/**
 * Logs the state of a VPC security group.
 * @param vpcsecgroup [in] data structure holding information about the VPC SG of interest
 */
void print_mido_vpc_secgroup(mido_vpc_secgroup *vpcsecgroup) {
    int i;
    LOGTRACE("PRINT VPCSECGROUP: name=%s gnipresent=%d\n", vpcsecgroup->name, vpcsecgroup->gnipresent);
    for (i = 0; i < VPCSG_END; i++) {
        mido_print_midoname(vpcsecgroup->midos[i]);
    }
}

/**
 * Logs the state of a Mido Gateway.
 * @param gw [in] data structure holding information about the Mido Gateway of interest
 * @param llevel [in] log level to be used.
 */
void print_mido_gw(mido_gw *gw, log_level_e llevel) {
    if (!gw) {
        return;
    }
    EUCALOG(llevel, "PRINT MIDO GW:\n");
    EUCALOG(llevel, "\tGateway Host: %s\n", gw->host->obj->name);
    EUCALOG(llevel, "\tport        : %s\n", gw->port->name);
    EUCALOG(llevel, "\text IP      : %s\n", gw->ext_ip);
    EUCALOG(llevel, "\text CIDR    : %s\n", gw->ext_cidr);
    EUCALOG(llevel, "\text DEV     : %s\n", gw->ext_dev);
    EUCALOG(llevel, "\tpeer IP     : %s\n", gw->peer_ip);
    if (gw->asn) {
        EUCALOG(llevel, "\t  ASN       : %d\n", gw->asn);
        EUCALOG(llevel, "\t  peer ASN  : %d\n", gw->peer_asn);
        if (gw->bgp_v1) {
            EUCALOG(llevel, "\t  BGPv1     : %s\n", gw->bgp_v1->name);
        }
        if (gw->bgp_peer) {
            EUCALOG(llevel, "\t  BGPv5     : %s\n", gw->bgp_peer->name);
        }
        EUCALOG(llevel, "\t  ad-routes :\n");
        for (int i = 0; i < gw->max_ad_routes; i++) {
            mido_gw_ad_route *ar = gw->ad_routes[i];
            if (!ar) continue;
            EUCALOG(llevel, "\t\t %s %s\n", ar->cidr, ar->route->name);
        }
        for (int i = 0; i < gw->max_bgp_networks; i++) {
            mido_gw_ad_route *ar = gw->bgp_networks[i];
            if (!ar) continue;
            EUCALOG(llevel, "\t\t %s %s\n", ar->cidr, ar->route->name);
        }
    }
}

/**
 * Searches the discovered VPC data structure for the instance/interface in the argument.
 *
 * @param vpcsubnet [in] data structure holding information about the subnet in
 * which the interface of interest is connected.
 * @param instancename [in] name of the instance/interface of interest.
 * @param outvpcinstance [out] pointer to vpcinstance data structure of interest (if found).
 *
 * @return 0 if the interface is found. 1 otherwise.
 */
int find_mido_vpc_instance(mido_vpc_subnet *vpcsubnet, char *instancename, mido_vpc_instance **outvpcinstance) {
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

/**
 * Searches for the instance/interface with name instancename
 * @param mido [in] mido data structure that holds MidoNet configuration
 * @param instancename [in] name of the instance of interest
 * @param outvpc [out] pointer to the mido_vpc structure when found
 * @param outvpcsubnet [out] pointer to the mido_vpc_subnet structure when found
 * @param outvpcnatgateway [out] pointer to the mido_vpc_natgateway structure when found
 * @return 0 on success. 1 otherwise.
 */
int find_mido_vpc_instance_global(mido_config *mido, char *instancename, mido_vpc **outvpc,
        mido_vpc_subnet **outvpcsubnet, mido_vpc_instance **outvpcinstance) {
    int i, j, rc;
    mido_vpc *vpc = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;

    if (!mido || !instancename || !outvpc || !outvpcsubnet || !outvpcinstance) {
        return (1);
    }

    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            rc = find_mido_vpc_instance(vpcsubnet, instancename, outvpcinstance);
            if (!rc) {
                *outvpc = vpc;
                *outvpcsubnet = vpcsubnet;
                return (0);
            }
        }
    }

    return (1);
}

/**
 * Searches for the instance/interface id in MidoNet and extracts/returns the
 * matching id from MidoNet object(s). The argument id is expected to be a short
 * id. Long id is returned if found. The search looks for ip-address-group named
 * elip_post_id.
 * The returned pointer points to a newly allocated string or NULL if the search
 * fails.
 * @param id [in] instance or interface id of interest.
 * @return a string with the id found in MidoNet object(s). NULL if not found.
 */
char *find_mido_vpc_instance_id(const char *id) {
    if (!id) {
        LOGWARN("Invalid argument: unable to find NULL instance id\n");
        return (NULL);
    }
    char iagname[MIDO_NAME_LEN];
    snprintf(iagname, MIDO_NAME_LEN, "elip_post_%s", id);
    midonet_api_ipaddrgroup *iag = mido_get_ipaddrgroup(iagname);
    if (iag) {
        char *foundid = strstr(iag->obj->name, id);
        if (foundid) {
            return (strdup(foundid));
        }
    }
    return NULL;
}

/**
 * Searches the discovered VPC data structure for the NAT gateway in the argument.
 *
 * @param vpc [in] data structure holding information about a VPC.
 * @param natgname [in] name of the NAT Gateway of interest.
 * @param outvpcsubnet [out] optional pointer to vpcsubnet data structure of interest (if found).
 * @param outvpcnatgateway [out] pointer to vpcnatgateway data structure of interest (if found).
 *
 * @return 0 if the NAT gateway is found. 1 otherwise.
 */
int find_mido_vpc_natgateway(mido_vpc *vpc, char *natgname, mido_vpc_subnet **outvpcsubnet,
        mido_vpc_natgateway **outvpcnatgateway) {
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
                if (outvpcsubnet) {
                    *outvpcsubnet = vpcsubnet;
                }
                return (0);
            }
        }
    }

    return (1);
}

/**
 * Searches for the NAT gateway with name natgname
 * @param mido [in] mido data structure that holds MidoNet configuration
 * @param natgname [in] name of the NAT gateway of interest
 * @param outvpc [out] pointer to the mido_vpc structure when found
 * @param outvpcsubnet [out] pointer to the mido_vpc_subnet structure when found
 * @param outvpcnatgateway [out] pointer to the mido_vpc_natgateway structure when found
 * @return 0 on success. 1 otherwise.
 */
int find_mido_vpc_natgateway_global(mido_config *mido, char *natgname, mido_vpc **outvpc,
        mido_vpc_subnet **outvpcsubnet, mido_vpc_natgateway **outvpcnatgateway) {
    int i;

    if (!mido || !natgname || !outvpc || !outvpcsubnet) {
        return (1);
    }

    *outvpcnatgateway = NULL;

    for (i = 0; i < mido->max_vpcs; i++) {
        find_mido_vpc_natgateway(&(mido->vpcs[i]), natgname, outvpcsubnet, outvpcnatgateway);
        if (*outvpcnatgateway) {
            *outvpc = &(mido->vpcs[i]);
            return (0);
        }
    }
    return (1);
}

/**
 * Searches for the vpc subnet with name subnetname (the vpc of interest is known)
 * @param vpc [in] the vpc in which the subnet will be searched
 * @param subnetname [in] name of the subnet of interest
 * @param outvpcsubnet [out] pointer to the mido_vpc_subnet structure when found
 * @return 0 on success. 1 otherwise.
 */
int find_mido_vpc_subnet(mido_vpc *vpc, char *subnetname, mido_vpc_subnet **outvpcsubnet) {
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
 * Searches for the vpc subnet with name subnetname
 * @param mido [in] mido data structure that holds MidoNet configuration
 * @param subnetname [in] name of the subnet of interest
 * @param outvpc [out] pointer to the mido_vpc structure when found
 * @param outvpcsubnet [out] pointer to the mido_vpc_subnet structure when found
 * @return 0 on success. 1 otherwise.
 */
int find_mido_vpc_subnet_global(mido_config *mido, char *subnetname, mido_vpc **outvpc, mido_vpc_subnet **outvpcsubnet) {
    int i;

    if (!mido || !subnetname || !outvpc || !outvpcsubnet) {
        return (1);
    }

    *outvpcsubnet = NULL;

    for (i = 0; i < mido->max_vpcs; i++) {
        find_mido_vpc_subnet(&(mido->vpcs[i]), subnetname, outvpcsubnet);
        if (*outvpcsubnet) {
            *outvpc = &(mido->vpcs[i]);
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
                LOGWARN("failed to delete %s route\n", rtable->name);
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

/**
 * Given the mido VPC, mido subnet, and GNI route table in the argument, parse
 * the GNI route table and fill the proutes array.
 *
 * @param mido [in] data structure that holds MidoNet configuration
 * @param vpc [in] information about the VPC of interest - discovered in MidoNet
 * @param vpcsubnet [in] VPC subnet of interest - discovered in MidoNet
 * @param subnetNetaddr [in] network address of VPC subnet CIDR block
 * @param subnetSlashnet [in] subnet mask in /xx form
 * @param rtable [in] route table as described in GNI to be implemented.
 * @param gnivpc [in] VPC of the subnet associated with the route table to be implemented, as described in GNI
 * @param proutes [out] pointer where the resulting array of mido_parsed_route structures will be located. 
 * @param max_proutes [out] number of parsed routes. 
 *
 * @return 0  on success. 1 otherwise.
 *
 * @pre MidoNet discovery is assumed to be executed and all MidoNet resources pre-populated
 * in mido_config, mido_vpc, and mido_vpc_subnet data structures.
 */
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
    
    gni_vpcsubnet *gnisn = vpcsubnet->gniSubnet;

    for (i = 0; i < rtable->max_entries; i++) {
        cidr_split(rtable->entries[i].destCidr, dstNetaddr, dstSlashnet, NULL, NULL, NULL);

        switch (parse_mido_route_entry_target(rtable->entries[i].target)) {
            case VPC_TARGET_LOCAL:
                // Local route cannot be removed. It is implemented on VPC subnet creation.
                LOGTRACE("local route added on subnet creation. Nothing to do.\n");
                if (gnisn && gnisn->rt_entry_applied) {
                    gnisn->rt_entry_applied[i] = 1;
                }
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
                    retroutes = EUCA_REALLOC_C(retroutes, max_retroutes + 1, sizeof (mido_parsed_route));
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
                    if (gnisn && gnisn->rt_entry_applied) {
                        gnisn->rt_entry_applied[i] = 1;
                    }
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
                    retroutes = EUCA_REALLOC_C(retroutes, max_retroutes + 1, sizeof (mido_parsed_route));
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
                    if (gnisn && gnisn->rt_entry_applied) {
                        gnisn->rt_entry_applied[i] = 1;
                    }
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

                    retroutes = EUCA_REALLOC_C(retroutes, max_retroutes + 1, sizeof (mido_parsed_route));
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
                    if (gnisn && gnisn->rt_entry_applied) {
                        gnisn->rt_entry_applied[i] = 1;
                    }
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

/**
 * Searches for the vpc with name vpcname
 * @param mido [in] mido data structure that holds MidoNet configuration
 * @param vpcname [in] name of the vpc of interest
 * @param outvpc [out] pointer to the mido_vpc structure when found
 * @return 0 on success. 1 otherwise.
 */
int find_mido_vpc(mido_config *mido, char *vpcname, mido_vpc **outvpc) {
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
    midoname *res = NULL;

    if (!br || !name) {
        LOGWARN("Invalid argument: unable to search NULL bridge ports.\n");
        return (NULL);
    }
    for (i = 0; i < br->max_ports && !found; i++) {
        if (br->ports[i] == NULL) {
            continue;
        }
        if (!rc && br->ports[i]->port && br->ports[i]->port->ifname && strlen(br->ports[i]->port->ifname)) {
            if (strstr(br->ports[i]->port->ifname, name)) {
                found = 1;
                res = br->ports[i];
            }
        }
    }

    return (res);
}

/**
 * Searches the given list of MidoNet ports for ports that belongs to the device
 * specified in the argument.
 *
 * @param ports [in] pointer to an array of MidoNet ports.
 * @param max_ports [in] number of ports in the array.
 * @param device [in] of interest
 * @param outports [out] pointer to an array of midoname data structure references
 * of the ports that belong to the given device. Memory is allocated.
 * Caller should release once done.
 * @param outports_max [out] number of ports that belong to the device of interest.
 *
 * @return 0 if port(s) that belong(s) to the given device is/are found. 1 otherwise.
 */
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
            retports = EUCA_REALLOC_C(retports, *outports_max + 1, sizeof (midoname *));
            retports[*outports_max] = port;
            (*outports_max)++;
        }
        EUCA_FREE(devuuid);
        devuuid = NULL;
    }
    *outports = retports;
    return (0);
}

/**
 * Searches the given list of MidoNet ports for ports bound to the host
 * specified in the argument.
 *
 * @param ports [in] pointer to an array of MidoNet ports.
 * @param max_ports [in] number of ports in the array.
 * @param host [in] of interest
 * @param outports [out] pointer to an array of midoname data structure references
 * of the ports with binding to the given host. Memory is allocated.
 * Caller should release once done.
 * @param outports_max [out] number of ports with binding to the host of interest.
 *
 * @return 0 if port(s) with binding to the given host is found. 1 otherwise.
 */
int find_mido_host_ports(midoname **ports, int max_ports, midoname *host, midoname ***outports, int *outports_max) {
    int i;
    //int rc;
    //char *hostuuid = NULL;
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
        //rc = mido_getel_midoname(port, "hostId", &hostuuid);
        if (port->port && port->port->hostid && (!strcmp(port->port->hostid, host->uuid))) {
            retports = EUCA_REALLOC_C(retports, *outports_max + 1, sizeof (midoname *));
            retports[*outports_max] = port;
            (*outports_max)++;
        }
        //EUCA_FREE(hostuuid);
    }
    *outports = retports;
    return (0);
}

/**
 * Searches the given list of MidoNet ports for ports that are members of the
 * portgroup specified in the argument.
 *
 * @param ports [in] pointer to an array of MidoNet ports.
 * @param max_ports [in] number of ports in the array.
 * @param portgroup [in] of interest
 * @param outports [out] pointer to an array of midoname data structure references
 * of the ports that are members of the given portgroup. Memory is allocated.
 * Caller should release once done.
 * @param outports_max [out] number of ports with binding to the host of interest.
 *
 * @return 0 if port(s) that is/are member(s) of the given portgroup is/are found. 1 otherwise.
 */
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
    }
    EUCA_FREE(pgports);
    if (pgports_max != *outports_max) {
        LOGWARN("Found %d members for portgroup %s (expected %d)\n", *outports_max, portgroup->name, pgports_max);
    }
    *outports = retports;
    return (0);
}

/**
 * Parses the given vpcsubnet MidoNet resource to get its corresponding CIDR (network
 * address and network mask).
 *
 * @param vpcsubnet [in] VPC subnet of interest - discovered in MidoNet
 * @param net [out] pointer to a string that holds the network address information
 * @param length [out] pointer to a string that holds the network mask information
 *
 * @return 0 if net/mask information of the given vpcsubnet is successfully parsed. 1 otherwise.
 */
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
    if (vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port && vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port->netaddr &&
            vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port->netlen &&
            strlen(vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port->netaddr) &&
            strlen(vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port->netlen)) {
        *net = vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port->netaddr;
        *length = vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port->netlen;
    } else {
        rc = 1;
    }
    //rc |= mido_getel_midoname(vpcsubnet->midos[SUBN_VPCRT_BRPORT], "networkAddress", net);
    //rc |= mido_getel_midoname(vpcsubnet->midos[SUBN_VPCRT_BRPORT], "networkLength", length);
    return (rc);
}

/**
 * Parses the given route MidoNet resource to get its corresponding source network
 * (network/mask) and destination network (network/mask).
 *
 * @param route [in] route of interest - discovered in MidoNet
 * @param srcnet [out] pointer to a string that holds the source network address information
 * @param srclength [out] pointer to a string that holds the source network mask information
 * @param dstnet [out] pointer to a string that holds the destination network address information
 * @param dstlength [out] pointer to a string that holds the destination network mask information
 *
 * @return 0 if the parse is successful. 1 otherwise.
 */
int parse_mido_vpc_route_addr(midoname *route, char **srcnet, char **srclength, char **dstnet, char **dstlength) {
    int rc = 0;
    if (!route) {
        LOGWARN("Invalid argument: NULL pointer.\n");
        return (1);
    }
    if (srcnet && srclength) {
        //rc |= mido_getel_midoname(route, "srcNetworkAddr", srcnet);
        //rc |= mido_getel_midoname(route, "srcNetworkLength", srclength);
        if (route->route && route->route->srcnet && route->route->srclen &&
                strlen(route->route->srcnet) && strlen(route->route->srclen)) {
            *srcnet = route->route->srcnet;
            *srclength = route->route->srclen;
        } else {
            rc++;
        }
    }
    if (dstnet && dstlength) {
        //rc |= mido_getel_midoname(route, "dstNetworkAddr", dstnet);
        //rc |= mido_getel_midoname(route, "dstNetworkLength", dstlength);
        if (route->route && route->route->dstnet && route->route->dstlen &&
                strlen(route->route->dstnet) && strlen(route->route->dstlen)) {
            *dstnet = route->route->dstnet;
            *dstlength = route->route->dstlen;
        } else {
            rc++;
        }
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
    }
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
    char name[MIDO_NAME_LEN];

    if (!mido || !vpcsecgroup) {
        return (1);
    }

    snprintf(name, MIDO_NAME_LEN, "sg_ingress_%20s", vpcsecgroup->name);
    midonet_api_chain *sgchain = mido_get_chain(name);
    if (sgchain != NULL) {
        LOGTRACE("Found SG chain %s\n", sgchain->obj->name);
        vpcsecgroup->ingress = sgchain;
        vpcsecgroup->midos[VPCSG_INGRESS] = sgchain->obj;
    }

    snprintf(name, MIDO_NAME_LEN, "sg_egress_%20s", vpcsecgroup->name);
    sgchain = mido_get_chain(name);
    if (sgchain != NULL) {
        LOGTRACE("Found SG chain %s\n", sgchain->obj->name);
        vpcsecgroup->egress = sgchain;
        vpcsecgroup->midos[VPCSG_EGRESS] = sgchain->obj;
    }

    snprintf(name, MIDO_NAME_LEN, "sg_priv_%20s", vpcsecgroup->name);
    midonet_api_ipaddrgroup *sgipaddrgroup = mido_get_ipaddrgroup(name);
    if (sgipaddrgroup != NULL) {
        LOGTRACE("Found SG IAG %s\n", sgipaddrgroup->obj->name);
        vpcsecgroup->iag_priv = sgipaddrgroup;
        vpcsecgroup->midos[VPCSG_IAGPRIV] = sgipaddrgroup->obj;
    }

    snprintf(name, MIDO_NAME_LEN, "sg_pub_%20s", vpcsecgroup->name);
    sgipaddrgroup = mido_get_ipaddrgroup(name);
    if (sgipaddrgroup != NULL) {
        LOGTRACE("Found SG IAG %s\n", sgipaddrgroup->obj->name);
        vpcsecgroup->iag_pub = sgipaddrgroup;
        vpcsecgroup->midos[VPCSG_IAGPUB] = sgipaddrgroup->obj;
    }

    snprintf(name, MIDO_NAME_LEN, "sg_all_%20s", vpcsecgroup->name);
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
        }
    }

    return (ret);
}

/**
 * Create necessary objects in MidoNet to implement a Security Group.
 *
 * @param mido [in] data structure that holds MidoNet configuration.
 * @param vpcsecgroup [in] data structure that holds information about the Security Group of interest.
 *
 * @return 0 on success. 1 on any error.
 */
int create_mido_vpc_secgroup(mido_config *mido, mido_vpc_secgroup *vpcsecgroup) {
    int ret = 0;
    char name[MIDO_NAME_LEN];
    midonet_api_chain *ch = NULL;
    midonet_api_ipaddrgroup *ipag = NULL;

    if (!mido || !vpcsecgroup) {
        return (1);
    }
    snprintf(name, MIDO_NAME_LEN, "sg_ingress_%20s", vpcsecgroup->name);
    ch = mido_create_chain(VPCMIDO_TENANT, name, &(vpcsecgroup->midos[VPCSG_INGRESS]));
    if (!ch) {
        LOGWARN("Failed to create chain %s.\n", name);
        ret = 1;
    } else {
        vpcsecgroup->ingress = ch;
    }

    snprintf(name, MIDO_NAME_LEN, "sg_egress_%20s", vpcsecgroup->name);
    ch = mido_create_chain(VPCMIDO_TENANT, name, &(vpcsecgroup->midos[VPCSG_EGRESS]));
    if (!ch) {
        LOGWARN("Failed to create chain %s.\n", name);
        ret = 1;
    } else {
        vpcsecgroup->egress = ch;
    }

    snprintf(name, MIDO_NAME_LEN, "sg_priv_%20s", vpcsecgroup->name);
    ipag = mido_create_ipaddrgroup(VPCMIDO_TENANT, name, &(vpcsecgroup->midos[VPCSG_IAGPRIV]));
    if (!ipag) {
        LOGWARN("Failed to create ipaddrgroup %s.\n", name);
        ret = 1;
    } else {
        vpcsecgroup->iag_priv = ipag;
    }

    snprintf(name, MIDO_NAME_LEN, "sg_pub_%20s", vpcsecgroup->name);
    ipag = mido_create_ipaddrgroup(VPCMIDO_TENANT, name, &(vpcsecgroup->midos[VPCSG_IAGPUB]));
    if (!ipag) {
        LOGWARN("Failed to create ipaddrgroup %s.\n", name);
        ret = 1;
    } else {
        vpcsecgroup->iag_pub = ipag;
    }

    snprintf(name, MIDO_NAME_LEN, "sg_all_%20s", vpcsecgroup->name);
    ipag = mido_create_ipaddrgroup(VPCMIDO_TENANT, name, &(vpcsecgroup->midos[VPCSG_IAGALL]));
    if (!ipag) {
        LOGWARN("Failed to create ipaddrgroup %s.\n", name);
        ret = 1;
    } else {
        vpcsecgroup->iag_all = ipag;
    }

    return (ret);
}

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

/**
 * Searches for VPC SG secgroupname.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param secgroupname [in] name of the security group of interest
 * @param outvpcsecgroup [out] data structure holding information about the security
 * group of interest.
 * @return 0 on success. Positive integer otherwise.
 */
int find_mido_vpc_secgroup(mido_config *mido, char *secgroupname, mido_vpc_secgroup **outvpcsecgroup) {
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

/**
 * Releases resources allocated to hold information about SG vpcsecgroup.
 * @param vpcsecgroup [in] data structure holding information about the SG of interest
 * @return 0 on success. Positive integer otherwise.
 */
int free_mido_vpc_secgroup(mido_vpc_secgroup *vpcsecgroup) {
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

/**
 * Releases resources allocated to hold information about network ACL vpcnacl.
 * @param vpcnacl [in] data structure holding information about the network ACL of interest
 * @return 0 on success. Positive integer otherwise.
 */
int free_mido_vpc_nacl(mido_vpc_nacl *vpcnacl) {
    int ret = 0;
    
    if (!vpcnacl) {
        return (ret);
    }
    
    memset(vpcnacl, 0, sizeof (mido_vpc_nacl));
    return (ret);
}

/**
 * Parses the given gni_rule to get its corresponding mido_parsed_chain_rule
 * @param mido [in] mido current mido_config data structure.
 * @param rule [in] rule gni_rule of interest.
 * @param parsed_rule [out] parsed_rule data structure to store the parsed results.
 * @return  0 if the parse is successful. 1 otherwise.
 */
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
            cidr_split(rule->cidr, subnet_buf, slashnet_buf, NULL, NULL, NULL);
            snprintf(parsed_rule->jsonel[MIDO_CRULE_NW], 64, "%s", subnet_buf);
            snprintf(parsed_rule->jsonel[MIDO_CRULE_NWLEN], 64, "%s", slashnet_buf);
        } else {
            snprintf(parsed_rule->jsonel[MIDO_CRULE_NW], 64, "%s", "0.0.0.0");
            snprintf(parsed_rule->jsonel[MIDO_CRULE_NWLEN], 64, "%s", "0");
        }
    }

    // protocol
    parse_mido_chain_rule_protocol(rule->protocol, rule->icmpType, rule->icmpCode,
            rule->fromPort, rule->toPort, parsed_rule);

    return (ret);
}

/**
 * Parses the protocol specified in the argument and gets its corresponding mido_parsed_chain_rule
 * @param proto [in] protocol number of interest
 * @param icmpType [in] if protocol is ICMP (1), specify its type (ignored if not ICMP)
 * @param icmpCode [in] if protocol is ICMP (1), specify its code (ignored if not ICMP)
 * @param fromPort [in] transport port range starting value (ignored if not TCP or UDP)
 * @param toPort [in] transport port range end value (ignored if not TCP or UDP)
 * @param parsed_rule [out] parsed_rule data structure to store the parsed results.
 * @return  0 if the parse is successful. 1 otherwise.
 */
int parse_mido_chain_rule_protocol(int proto, int icmpType, int icmpCode,
        int fromPort, int toPort, mido_parsed_chain_rule *parsed_rule) {

    if (!parsed_rule) {
        LOGWARN("Invalid argument: cannot parse protocol of NULL\n");
        return (1);
    }

    snprintf(parsed_rule->jsonel[MIDO_CRULE_PROTO], 64, "%d", proto);
    switch (proto) {
        case 1: // ICMP
            if (icmpType != -1) {
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPS], 64, "jsonjson");
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPS_S], 64, "%d", icmpType);
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPS_E], 64, "%d", icmpType);
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPS_END], 64, "END");
            }
            if (icmpCode != -1) {
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD], 64, "jsonjson");
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_S], 64, "%d", icmpCode);
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_E], 64, "%d", icmpCode);
                snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_END], 64, "END");
            }
            break;
        case 6:  // TCP
        case 17: // UDP
            snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD], 64, "jsonjson");
            snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_S], 64, "%d", fromPort);
            snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_E], 64, "%d", toPort);
            snprintf(parsed_rule->jsonel[MIDO_CRULE_TPD_END], 64, "END");
            break;
        case -1: // All protocols
            if (is_midonet_api_v1()) {
                snprintf(parsed_rule->jsonel[MIDO_CRULE_PROTO], 64, "0");
            } else if (is_midonet_api_v5()) {
                snprintf(parsed_rule->jsonel[MIDO_CRULE_PROTO], 64, "null");
            }
            break;
        default:
            // Protocols accepted by EC2 are ICMP/TCP/UDP.
            break;
    }
    return (0);
}

/**
 * Implements the given mido_parsed_chain_rule (assumed to be a security group rule)
 * in the given chain.
 *
 * @param chain [in] midoname structure of the chain of interest.
 * @param outname [i/o] pointer to an extant MidoNet rule (parameters will be checked
 * to avoid duplicate rule creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created rule
 * will not be returned.
 * @param pos [in] position in the chain for the newly created rule. -1 appends the rule.
 * @param ruletype [in] gni_rule type (MIDO_RULE_SG_EGRESS or MIDO_RULE_SG_INGRESS)
 * @param rule [in] mido_parsed_chain_rule to be created.
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
        LOGWARN("Invalid argument: cannot create secgroup rule with invalid protocol\n");
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
                    "ipAddrGroupSrc", rule->jsonel[MIDO_CRULE_GRPUUID], "nwSrcAddress", rule->jsonel[MIDO_CRULE_NW],
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

/**
 * Clears the contents of the mido_parsed_chain_rule structure in the argument.
 *
 * @param rule [in] mido_parsed_chain_rule structure of interest.
 *
 * @return 0 on success. 1 otherwise.
 */
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
 * Populates an euca Network ACL model.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param vpcsecgroup [i/o] data structure that holds the euca Network ACL model of interest.
 * @return 0 on success. 1 on any failure.
 */
int populate_mido_vpc_nacl(mido_config *mido, mido_vpc_nacl *vpcnacl) {
    int ret = 0;
    int i = 0;
    char name[MIDO_NAME_LEN];

    if (!mido || !vpcnacl || !strlen(vpcnacl->name) || !strlen(vpcnacl->vpcname)) {
        return (1);
    }

    snprintf(name, MIDO_NAME_LEN, "acl_ingress_%s_%s", vpcnacl->vpcname, vpcnacl->name);
    midonet_api_chain *aclchain = mido_get_chain(name);
    if (aclchain != NULL) {
        LOGTRACE("Found NACL ingress chain %s\n", aclchain->obj->name);
        vpcnacl->ingress = aclchain;
        vpcnacl->midos[VPCNACL_INGRESS] = aclchain->obj;
    }

    snprintf(name, MIDO_NAME_LEN, "acl_egress_%s_%s", vpcnacl->vpcname, vpcnacl->name);
    aclchain = mido_get_chain(name);
    if (aclchain != NULL) {
        LOGTRACE("Found NACL egress chain %s\n", aclchain->obj->name);
        vpcnacl->egress = aclchain;
        vpcnacl->midos[VPCNACL_EGRESS] = aclchain->obj;
    }

    LOGTRACE("vpc nacl (%s): AFTER POPULATE\n", vpcnacl->name);
    for (i = 0; i < VPCNACL_END; i++) {
        if (vpcnacl->midos[i]) {
            LOGTRACE("\tmidos[%d]: %d\n", i, vpcnacl->midos[i]->init);
        }
        if (!vpcnacl->midos[i] || !vpcnacl->midos[i]->init) {
            LOGWARN("failed to populate %s midos[%d]\n", vpcnacl->name, i);
            vpcnacl->population_failed = 1;
        }
    }

    return (ret);
}

/**
 * Create necessary objects in MidoNet to implement a Network ACL.
 *
 * @param mido [in] data structure that holds MidoNet configuration.
 * @param vpc [in] data structure that holds information about the VPC in which
 * the network ACL of interest is associated with.
 * @param vpcnacl [in] data structure that holds information about the Network ACL of interest.
 *
 * @return 0 on success. 1 on any error.
 */
int create_mido_vpc_nacl(mido_config *mido, mido_vpc *vpc, mido_vpc_nacl *vpcnacl) {
    int ret = 0;
    char name[MIDO_NAME_LEN];
    midonet_api_chain *ch = NULL;

    if (!mido || !vpc || !vpcnacl) {
        return (1);
    }
    snprintf(name, MIDO_NAME_LEN, "acl_ingress_%s_%s", vpc->name, vpcnacl->name);
    ch = mido_create_chain(VPCMIDO_TENANT, name, &(vpcnacl->midos[VPCNACL_INGRESS]));
    if (!ch) {
        LOGWARN("Failed to create chain %s.\n", name);
        ret = 1;
    } else {
        vpcnacl->ingress = ch;
    }

    snprintf(name, MIDO_NAME_LEN, "acl_egress_%s_%s", vpc->name, vpcnacl->name);
    ch = mido_create_chain(VPCMIDO_TENANT, name, &(vpcnacl->midos[VPCNACL_EGRESS]));
    if (!ch) {
        LOGWARN("Failed to create chain %s.\n", name);
        ret = 1;
    } else {
        vpcnacl->egress = ch;
    }

    return (ret);
}

/**
 * Implements the given mido_parsed_chain_rule (assumed to be a network acl entry)
 * in the given chain.
 *
 * @param chain [in] midoname structure of the chain of interest.
 * @param outname [i/o] pointer to an extant MidoNet rule (parameters will be checked
 * to avoid duplicate rule creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created rule
 * will not be returned.
 * @param pos [in] position in the chain for the newly created rule. -1 appends the rule.
 * @param ruletype [in] gni_rule type (MIDO_RULE_ACLENTRY_EGRESS or MIDO_RULE_ACLENTRY_INGRESS)
 * @param entry [in] mido_parsed_chain_rule to be created.
 *
 * @return 0 if the parse is successful. 1 otherwise.
 */
int create_mido_vpc_nacl_entry(midonet_api_chain *chain, midoname **outname,
        int pos, int ruletype, mido_parsed_chain_rule *entry) {
    int rc = 0;
    int ret = 0;
    char spos[8];

    if (!chain) {
        LOGWARN("Invalid argument: cannot create nacl entry in a NULL chain.\n");
        return (1);
    }
    if ((strlen(entry->jsonel[MIDO_CRULE_PROTO]) == 0) || (!strcmp(entry->jsonel[MIDO_CRULE_PROTO], "UNSET")) ||
            (!strcmp(entry->jsonel[MIDO_CRULE_TYPE], "UNSET"))) {
        LOGWARN("Invalid argument: cannot create nacl entry with invalid protocol or type\n");
        return (1);
    }

    if (pos == -1) {
        pos = chain->rules_count + 1;
    }
    snprintf(spos, 8, "%d", pos);

    switch (ruletype) {
        case MIDO_RULE_ACLENTRY_EGRESS:
            rc = mido_create_rule(chain, chain->obj, outname, NULL,
                    "position", spos, "type", entry->jsonel[MIDO_CRULE_TYPE],
                    "tpDst", entry->jsonel[MIDO_CRULE_TPD],
                    "tpDst:start", entry->jsonel[MIDO_CRULE_TPD_S], "tpDst:end", entry->jsonel[MIDO_CRULE_TPD_E],
                    "tpDst:END", entry->jsonel[MIDO_CRULE_TPD_END],
                    "tpSrc", entry->jsonel[MIDO_CRULE_TPS],
                    "tpSrc:start", entry->jsonel[MIDO_CRULE_TPS_S], "tpSrc:end", entry->jsonel[MIDO_CRULE_TPS_E],
                    "tpSrc:END", entry->jsonel[MIDO_CRULE_TPS_END],
                    "nwProto", entry->jsonel[MIDO_CRULE_PROTO],
                    "nwDstAddress", entry->jsonel[MIDO_CRULE_NW],
                    "nwDstLength", entry->jsonel[MIDO_CRULE_NWLEN], NULL);
            break;
        case MIDO_RULE_ACLENTRY_INGRESS:
            rc = mido_create_rule(chain, chain->obj, outname, NULL,
                    "position", spos, "type", entry->jsonel[MIDO_CRULE_TYPE],
                    "tpDst", entry->jsonel[MIDO_CRULE_TPD],
                    "tpDst:start", entry->jsonel[MIDO_CRULE_TPD_S], "tpDst:end", entry->jsonel[MIDO_CRULE_TPD_E],
                    "tpDst:END", entry->jsonel[MIDO_CRULE_TPD_END],
                    "tpSrc", entry->jsonel[MIDO_CRULE_TPS],
                    "tpSrc:start", entry->jsonel[MIDO_CRULE_TPS_S], "tpSrc:end", entry->jsonel[MIDO_CRULE_TPS_E],
                    "tpSrc:END", entry->jsonel[MIDO_CRULE_TPS_END],
                    "nwProto", entry->jsonel[MIDO_CRULE_PROTO],
                    "nwSrcAddress", entry->jsonel[MIDO_CRULE_NW],
                    "nwSrcLength", entry->jsonel[MIDO_CRULE_NWLEN], NULL);
            break;
        case MIDO_RULE_INVALID:
        default:
            LOGWARN("Invalid argument: cannot create invalid acl entry.\n");
            rc = 1;
    }
    
    if (rc) {
        ret = 1;
    }
    return (ret);
}

/**
 * Deletes mido objects of a VPC network acl.
 *
 * @param mido [in] data structure holding all discovered MidoNet resources.
 * @param vpcnacl [in] network acl of interest.
 *
 * @return 0 on success. 1 otherwise.
 */
int delete_mido_vpc_nacl(mido_config *mido, mido_vpc_nacl *vpcnacl) {
    int ret = 0, rc = 0;

    if ((!vpcnacl) || (strlen(vpcnacl->name) == 0)) {
        return (1);
    }

    rc += mido_delete_chain(vpcnacl->midos[VPCNACL_INGRESS]);
    rc += mido_delete_chain(vpcnacl->midos[VPCNACL_EGRESS]);

    free_mido_vpc_nacl(vpcnacl);
    return (ret);
}

/**
 * Searches for the vpc network acl with name aclname (the vpc of interest is known)
 * @param vpc [in] the vpc in which the network acl will be searched
 * @param naclname [in] name of the network acl of interest
 * @param outvpcsubnet [out] pointer to the mido_vpc_nacl structure when found
 * @return 0 on success. 1 otherwise.
 */
int find_mido_vpc_nacl(mido_vpc *vpc, char *naclname, mido_vpc_nacl **outvpcnacl) {
    int i;

    if (!vpc || !naclname || !outvpcnacl) {
        return (1);
    }

    *outvpcnacl = NULL;

    for (i = 0; i < vpc->max_nacls; i++) {
        if (!strcmp(naclname, vpc->nacls[i].name)) {
            *outvpcnacl = &(vpc->nacls[i]);
            return (0);
        }
    }
    return (1);
}

/**
 * Parses the given network acl entry to get its corresponding mido_parsed_chain_rule
 * @param mido [in] mido current mido_config data structure.
 * @param entry [in] gni_acl_entry structure of interest.
 * @param parsed_rule [out] parsed_rule data structure to store the parsed results.
 * @return  0 if the parse is successful. 1 otherwise.
 */
int parse_mido_nacl_entry(mido_config *mido, gni_acl_entry *entry, mido_parsed_chain_rule *parsed_rule) {
    int ret = 0;

    if (!mido || !entry || !parsed_rule) {
        LOGWARN("Invalid argument: cannot parse NULL nacl entry.\n");
        return (1);
    }
    LOGTRACE("Parsing nacl entry\n");

    clear_parsed_chain_rule(parsed_rule);

    // default CIDR 0.0.0.0 or set explicitly
    if (strlen(entry->cidr)) {
        snprintf(parsed_rule->jsonel[MIDO_CRULE_NW], 64, "%s", hex2dot_s(entry->cidrNetaddr));
        snprintf(parsed_rule->jsonel[MIDO_CRULE_NWLEN], 64, "%d", entry->cidrSlashnet);
    } else {
        snprintf(parsed_rule->jsonel[MIDO_CRULE_NW], 64, "%s", "0.0.0.0");
        snprintf(parsed_rule->jsonel[MIDO_CRULE_NWLEN], 64, "%d", 0);
    }

    // protocol
    parse_mido_chain_rule_protocol(entry->protocol, entry->icmpType, entry->icmpCode,
            entry->fromPort, entry->toPort, parsed_rule);

    // type
    if (entry->allow) {
        snprintf(parsed_rule->jsonel[MIDO_CRULE_TYPE], 64, "%s", "accept");
    } else {
        snprintf(parsed_rule->jsonel[MIDO_CRULE_TYPE], 64, "%s", "drop");
    }
    return (ret);
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
    int ret = 0, found = 0, i = 0, j = 0;
    char fstr[64], tmp_name[MIDO_NAME_LEN];
    char *targetIP = NULL;
    char *rdst = NULL;
    char *privip = NULL;
    char mdip[NETWORK_ADDR_LEN] = { 0 };
    char matchStr[64];
    midoname *instanceport = NULL;
    midonet_api_host *instancehost = NULL;

    LOGTRACE("populating VPC instance %s\n", vpcinstance->name);
    midonet_api_bridge *subnetbr = vpcsubnet->subnetbr;
    if (subnetbr != NULL) {
        LOGTRACE("Found subnet bridge %s\n", subnetbr->obj->name);
        instanceport = vpcinstance->midos[INST_VPCBR_VMPORT];
        if (instanceport != NULL) {
            LOGTRACE("Found instance port %s\n", instanceport->name);
            if (instanceport->port && instanceport->port->hostid && strlen(instanceport->port->hostid)) {
                instancehost = mido_get_host(NULL, instanceport->port->hostid);
            }
            if (instancehost != NULL) {
                LOGTRACE("Found instance host %s\n", instancehost->obj->name);
                vpcinstance->midos[INST_VMHOST] = instancehost->obj;
            }
        }
        found = 0;
        midonet_api_dhcp *dhcp = NULL;
        if (subnetbr->max_dhcps) {
            dhcp = subnetbr->dhcps[0];
            midoname *tmpmn = mido_get_dhcphost(dhcp, vpcinstance->name);
            if (tmpmn) {
                LOGTRACE("Found dhcp host %s\n", tmpmn->name);
                vpcinstance->midos[INST_VPCBR_DHCPHOST] = tmpmn;
            }
        }
    }
    if (!vpcinstance->midos[INST_VPCBR_VMPORT] || !vpcinstance->midos[INST_VMHOST]) {
        LOGWARN("Unable to populate vpcinstance %s VPCBR_VMPORT and/or VMHOST.\n", vpcinstance->name);
    }

    // process public IP
    snprintf(fstr, 64, "elip_pre_%s", vpcinstance->name);
    midonet_api_ipaddrgroup *ipag = mido_get_ipaddrgroup(fstr);
    if (ipag != NULL) {
        LOGTRACE("Found ipag %s\n", ipag->obj->name);
        vpcinstance->iag_pre = ipag;
        vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP] = ipag->obj;
        if ((ipag->ips_count == 1) && ipag->ips[0]->ipagip && ipag->ips[0]->ipagip->ip
                && strlen(ipag->ips[0]->ipagip->ip)) {
            vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP_IP] = ipag->ips[0];
            vpcinstance->pubip = dot2hex(ipag->ips[0]->ipagip->ip);
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
                targetIP = NULL;
                if (postchain_rules[j]->rule && postchain_rules[j]->rule->nattarget) {
                    targetIP = postchain_rules[j]->rule->nattarget;
                    snprintf(matchStr, 64, "\"addressTo\": \"%s\"", ipag->ips[0]->ipagip->ip);
                    if (targetIP && strstr(targetIP, matchStr)) {
                        LOGTRACE("Found rule %s\n", postchain_rules[j]->name);
                        vpcinstance->midos[INST_ELIP_POST] = postchain_rules[j];
                        found = 1;
                    }
                }
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
                        rdst = NULL;
                        if (eucart->routes[j]->route) {
                            rdst = eucart->routes[j]->route->dstnet;
                        }
                        if (rdst && !strcmp(ipag->ips[0]->ipagip->ip, rdst)) {
                            LOGTRACE("Found route %s\n", eucart->routes[j]->name);
                            vpcinstance->midos[INST_ELIP_ROUTE] = eucart->routes[j];
                            found = 1;
                        }
                    }
                } else {
                    LOGWARN("Unable to populate instance %s: eucart not found.\n", vpcinstance->name);
                }
            }
        } else {
            if (ipag->ips_count != 0) {
                LOGWARN("Unexpected number of IP addresses (%d) in %s\n", ipag->ips_count, ipag->obj->name);
            }
        }
    }

    // process private IP
    snprintf(fstr, 64, "elip_post_%s", vpcinstance->name);
    ipag = mido_get_ipaddrgroup(fstr);
    if (ipag != NULL) {
        LOGTRACE("Found ipag %s\n", ipag->obj->name);
        vpcinstance->iag_post = ipag;
        vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP] = ipag->obj;
        if ((ipag->ips_count == 1) && ipag->ips[0]->ipagip && ipag->ips[0]->ipagip->ip
                && strlen(ipag->ips[0]->ipagip->ip)) {
            vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP_IP] = ipag->ips[0];
            vpcinstance->privip = dot2hex(ipag->ips[0]->ipagip->ip);
            privip = ipag->ips[0]->ipagip->ip;
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
                targetIP = NULL;
                if (preelipchain_rules[j]->rule && preelipchain_rules[j]->rule->nattarget) {
                    targetIP = preelipchain_rules[j]->rule->nattarget;
                    snprintf(matchStr, 64, "\"addressTo\": \"%s\"", ipag->ips[0]->ipagip->ip);
                    if (targetIP && strstr(targetIP, matchStr)) {
                        LOGTRACE("Found rule %s\n", preelipchain_rules[j]->name);
                        vpcinstance->midos[INST_ELIP_PRE] = preelipchain_rules[j];
                        found = 1;
                    }
                }
            }
        } else {
            if (ipag->ips_count != 0) {
                LOGWARN("Unexpected number of IP addresses (%d) in %s\n", ipag->ips_count, ipag->obj->name);
            }
        }
    }

    if (!vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP] || !vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP]) {
        LOGWARN("Unable to populate vpcinstance %s IPADDRGROUP\n", vpcinstance->name);
    }

    snprintf(tmp_name, MIDO_NAME_LEN, "ic_%s_prechain", vpcinstance->name);
    midonet_api_chain *ic = mido_get_chain(tmp_name);
    if (ic != NULL) {
        LOGTRACE("Found chain %s\n", ic->obj->name);
        vpcinstance->prechain = ic;
        vpcinstance->midos[INST_PRECHAIN] = ic->obj;
    }
    snprintf(tmp_name, MIDO_NAME_LEN, "ic_%s_postchain", vpcinstance->name);
    ic = mido_get_chain(tmp_name);
    if (ic != NULL) {
        LOGTRACE("Found chain %s\n", ic->obj->name);
        vpcinstance->postchain = ic;
        vpcinstance->midos[INST_POSTCHAIN] = ic->obj;
    }

    if (privip && (mido->config->enable_mido_md || mido->config->populate_mido_md)) {
        // MD SNAT rule
        midoname **chain_rules = NULL;
        int max_chain_rules = 0;
        if (vpc->rt_mduplink_outfilter && vpc->rt_mduplink_outfilter->max_rules) {
            chain_rules = vpc->rt_mduplink_outfilter->rules;
            max_chain_rules = vpc->rt_mduplink_outfilter->max_rules;
        }
        found = 0;
        for (j = 0; j < max_chain_rules && !found; j++) {
            if (chain_rules[j] == NULL) {
                continue;
            }
            if (chain_rules[j]->rule && chain_rules[j]->rule->type && (!strcmp(chain_rules[j]->rule->type, "snat")) &&
                    chain_rules[j]->rule->nwsrcaddress && (!strcmp(chain_rules[j]->rule->nwsrcaddress, privip))) {
                sscanf(chain_rules[j]->rule->nattarget, "%*[^\"]\"addressFrom\": \"%16[^\"]", mdip);
                vpcinstance->midos[INST_MD_SNAT] = chain_rules[j];
            }
        }
        if (strlen(mdip)) {
            vpcinstance->eniid = dot2hex(mdip) - mido->mdconfig.mdnw;
            set_eni_id(mido, vpcinstance->eniid);
        }

        // MD DNAT rule
        if (vpcinstance->eniid) {
            midoname **chain_rules = NULL;
            int max_chain_rules = 0;
            if (vpc->rt_mduplink_infilter && vpc->rt_mduplink_infilter->max_rules) {
                chain_rules = vpc->rt_mduplink_infilter->rules;
                max_chain_rules = vpc->rt_mduplink_infilter->max_rules;
            }
            found = 0;
            for (j = 0; j < max_chain_rules && !found; j++) {
                if (chain_rules[j] == NULL) {
                    continue;
                }
                if (chain_rules[j]->rule && chain_rules[j]->rule->type && (!strcmp(chain_rules[j]->rule->type, "dnat")) &&
                        chain_rules[j]->rule->nwdstaddress && (!strcmp(chain_rules[j]->rule->nwdstaddress, mdip))) {
                    if (chain_rules[j]->rule->nattarget) {
                        targetIP = chain_rules[j]->rule->nattarget;
                        snprintf(matchStr, 64, "\"addressTo\": \"%s\"", privip);
                        if (targetIP && strstr(targetIP, matchStr)) {
                            vpcinstance->midos[INST_MD_DNAT] = chain_rules[j];
                            found = 1;
                        }
                    }
                }
            }
        }

        // MD IP route
        if (vpcinstance->eniid) {
            midonet_api_router *eucamdrt = mido->midomd->eucamdrt;
            if (eucamdrt != NULL) {
                found = 0;
                for (j = 0; j < eucamdrt->max_routes && !found; j++) {
                    if (eucamdrt->routes[j] == NULL) {
                        continue;
                    }
                    rdst = NULL;
                    if (eucamdrt->routes[j]->route) {
                        rdst = eucamdrt->routes[j]->route->dstnet;
                    }
                    if (rdst && !strcmp(mdip, rdst)) {
                        vpcinstance->midos[INST_MD_ROUTE] = eucamdrt->routes[j];
                        found = 1;
                    }
                }
            } else {
                LOGWARN("Unable to populate instance %s: eucamdrt not found.\n", vpcinstance->name);
            }
        }

    }

    if (mido->config->enable_mido_arptable) {
        if (subnetbr) {
            // populate the dot2 arp entry
            if (privip) {
                midoname *ip4mac = midonet_api_cache_lookup_ip4mac_byip(subnetbr, privip, NULL);
                if (ip4mac) {
                    vpcinstance->midos[INST_IP4MAC] = ip4mac;
                    LOGEXTREME("Found arp_table entry %s_%s\n", ip4mac->ip4mac->ip, ip4mac->ip4mac->mac);
                }
                if (instanceport) {
                    midoname *macport = midonet_api_cache_lookup_macport_byport(subnetbr, instanceport->uuid, NULL);
                    if (macport) {
                        vpcinstance->midos[INST_MACPORT] = macport;
                        LOGEXTREME("Found mac_table entry %s_%s\n", macport->macport->macAddr, macport->macport->portId);
                    }
                }
            }
        }
    }

    LOGTRACE("vpc instance (%s): AFTER POPULATE\n", vpcinstance->name);
    for (i = 0; i < INST_ELIP_PRE_IPADDRGROUP_IP; i++) {
        if (vpcinstance->midos[i] == NULL) {
            LOGWARN("VPC instance population failed to populate resource at idx %d\n", i);
            vpcinstance->population_failed = 1;
        }
    }
    if (vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP_IP]) {
        for (i = INST_ELIP_PRE_IPADDRGROUP_IP; i < INST_MD_DNAT; i++) {
            if (vpcinstance->midos[i] == NULL) {
                LOGWARN("VPC instance population failed to populate resource at idx %d\n", i);
                vpcinstance->population_failed = 1;
            }
        }
    }
    if (mido->config->enable_mido_md) {
        for (i = INST_MD_DNAT; i < INST_IP4MAC; i++) {
            if (vpcinstance->midos[i] == NULL) {
                LOGWARN("VPC instance %s population failed to populate resource at idx %d\n", vpcinstance->name, i);
                vpcinstance->population_failed = 1;
            }
        }
    }
    if (mido->config->enable_mido_arptable) {
        for (i = INST_IP4MAC; i < INST_END; i++) {
            if (vpcinstance->midos[i] == NULL) {
                LOGWARN("VPC instance %s population failed to populate resource at idx %d\n", vpcinstance->name, i);
                vpcinstance->population_failed = 1;
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
    char iagname[MIDO_NAME_LEN], tmp_name[MIDO_NAME_LEN];
    midonet_api_ipaddrgroup *iag = NULL;
    midonet_api_chain *ch = NULL;

    if (!vpcinstance) {
        LOGWARN("Invalid argument: NULL cannot create_mido_vpc_instance.\n");
    }
    
    // set up elip ipaddrgroups
    snprintf(iagname, MIDO_NAME_LEN, "elip_pre_%s", vpcinstance->name);
    iag = mido_create_ipaddrgroup(VPCMIDO_TENANT, iagname, &(vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP]));
    if (iag == NULL) {
        LOGWARN("Failed to create IAG %s.\n", iagname);
        ret++;
    }
    vpcinstance->iag_pre = iag;

    iag = NULL;    
    snprintf(iagname, MIDO_NAME_LEN, "elip_post_%s", vpcinstance->name);
    iag = mido_create_ipaddrgroup(VPCMIDO_TENANT, iagname, &(vpcinstance->midos[INST_ELIP_POST_IPADDRGROUP]));
    if (iag == NULL) {
        LOGWARN("Failed to create IAG %s.\n", iagname);
        ret++;
    }
    vpcinstance->iag_post = iag;

    // setup instance chains
    snprintf(tmp_name, MIDO_NAME_LEN, "ic_%s_prechain", vpcinstance->name);
    ch = mido_create_chain(VPCMIDO_TENANT, tmp_name, &(vpcinstance->midos[INST_PRECHAIN]));
    if (ch == NULL) {
        LOGWARN("Failed to create chain %s.\n", tmp_name);
        ret++;
    }
    vpcinstance->prechain = ch;

    snprintf(tmp_name, MIDO_NAME_LEN, "ic_%s_postchain", vpcinstance->name);
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

    if (mido->config->enable_mido_md || mido->config->populate_mido_md) {
        rc += disconnect_mido_vpc_instance_md(mido, vpc, vpcinstance);
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
    char *tmpstr = NULL, tmp_name[MIDO_NAME_LEN];
    char *pubip = NULL;
    int foundcnt = 0;
    boolean found1 = FALSE;
    boolean found2 = FALSE;

    LOGTRACE("populating %s\n", natgateway->name);
    if (!mido || !mido->midocore || !vpc || !vpcsubnet || !natgateway) {
        LOGDEBUG("Invalid argument: cannot populate with NULL information.\n");
        return (1);
    }
    if (mido->midocore->eucabr) {
        natgateway->midos[NATG_EUCABR] = mido->midocore->eucabr->obj;
    }
    if (mido->midocore->eucart) {
        natgateway->midos[NATG_EUCART] = mido->midocore->eucart->obj;
    }
    if (vpcsubnet && vpcsubnet->subnetbr) {
        natgateway->midos[NATG_SUBNBR] = vpcsubnet->subnetbr->obj;
    }
    // Search NAT Gateway Router in MidoNet
    snprintf(tmp_name, MIDO_NAME_LEN, "natr_%s", natgateway->name);
    midonet_api_router *router =  natgateway->natgrt;
    if (router != NULL) {
        LOGTRACE("Found router %s\n", router->obj->name);
        // Search for NATG_RT_UPLINK
        if (mido->midocore->midos[CORE_EUCABR] &&
                mido->midocore->midos[CORE_EUCABR]->init &&
                vpcsubnet && vpcsubnet->subnetbr) {
            midoname **rtports = router->ports;
            int max_rtports = router->max_ports;
            midoname **eucabrports = mido->midocore->eucabr->ports;
            int max_eucabrports = mido->midocore->eucabr->max_ports;
            midoname **snbrports = vpcsubnet->subnetbr->ports;
            int max_snbrports = vpcsubnet->subnetbr->max_ports;
            foundcnt = 0;
            for (i = 0; i < max_rtports && (foundcnt != 2); i++) {
                tmpstr = NULL;
                if (rtports[i]->port) {
                    tmpstr = rtports[i]->port->peerid;
                }
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
            }
        }
    }

    // Search public IP
    snprintf(tmp_name, MIDO_NAME_LEN, "elip_pre_%s", natgateway->name);
    midonet_api_ipaddrgroup *ipag = mido_get_ipaddrgroup(tmp_name);
    if (ipag != NULL) {
        LOGTRACE("Found ipag %s\n", ipag->obj->name);
        natgateway->midos[NATG_ELIP_PRE_IPADDRGROUP] = ipag->obj;
        if ((ipag->ips_count == 1) && ipag->ips[0]->ipagip && ipag->ips[0]->ipagip->ip
                && strlen(ipag->ips[0]->ipagip->ip)) {
            natgateway->midos[NATG_ELIP_PRE_IPADDRGROUP_IP] = ipag->ips[0];
            pubip = ipag->ips[0]->ipagip->ip;
        } else {
            if (ipag->ips_count != 0) {
                LOGWARN("Unexpected number of IP addresses (%d) in %s\n", ipag->ips_count, ipag->obj->name);
            }
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
                tmpstr = NULL;
                if (eucart->routes[j]->route) {
                    tmpstr = eucart->routes[j]->route->dstnet;
                }
                if (tmpstr && pubip && !strcmp(pubip, tmpstr)) {
                    LOGTRACE("Found %s route %s\n", pubip, eucart->routes[j]->name);
                    natgateway->midos[NATG_ELIP_ROUTE] = eucart->routes[j];
                    foundcnt = 1;
                }
            }
        } else {
            LOGWARN("Unable to find ELIP route for %s\n", natgateway->name);
        }
    }

    // Search NAT Gateway router chains
    snprintf(tmp_name, MIDO_NAME_LEN, "natc_%s_rtin", natgateway->name);
    midonet_api_chain *icin = mido_get_chain(tmp_name);
    if (icin != NULL) {
        LOGTRACE("Found chain %s\n", icin->obj->name);
        natgateway->midos[NATG_RT_INCHAIN] = icin->obj;
        natgateway->inchain = icin;
    }
    snprintf(tmp_name, MIDO_NAME_LEN, "natc_%s_rtout", natgateway->name);
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
        delete_mido_vpc_natgateway(mido, vpcsubnet, natgateway);
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
    char natg_rtname[MIDO_NAME_LEN];
    char tmpbuf[MIDO_NAME_LEN];
    char iagname[MIDO_NAME_LEN];
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

    cidr_split(vpc->gniVpc->cidr, vpc_net, vpc_mask, NULL, NULL, NULL);
    cidr_split(vpcnatgateway->gniVpcSubnet->cidr, subnet_net, subnet_mask, subnet_gwip, NULL, NULL);
    
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
    snprintf(natg_rtname, MIDO_NAME_LEN, "natr_%s_%s_%d", vpcnatgateway->name, vpcsubnet->name, vpcnatgateway->rtid);
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
        snprintf(tmpbuf, MIDO_NAME_LEN, "natc_%s_rtin", vpcnatgateway->name);
        ch = mido_create_chain(VPCMIDO_TENANT, tmpbuf, &(vpcnatgateway->midos[NATG_RT_INCHAIN]));
        if (!ch) {
            LOGWARN("Failed to create chain %s.\n", tmpbuf);
            ret = 1;
        } else {
            vpcnatgateway->inchain = ch;
        }

        snprintf(tmpbuf, MIDO_NAME_LEN, "natc_%s_rtout", vpcnatgateway->name);
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
        snprintf(iagname, MIDO_NAME_LEN, "elip_pre_%s", vpcnatgateway->name);
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

/**
 * Initializes the mido_config data structure
 * @param mido [in] mido_config data structure of interest
 * @param eucanetd_config [in] data structure holding information about eucanetd
 * @param gni [in] pointer to the Global Network Information structure
 * @return 0 on success. 1 on failure.
 */
int initialize_mido(mido_config *mido, eucanetdConfig *eucanetd_config, globalNetworkInfo *gni) {
    int ret = 0;

    if (!mido || !eucanetd_config || !gni ||
            !eucanetd_config->eucahome ||
            !strlen(eucanetd_config->mido_intrtcidr) ||
            !strlen(eucanetd_config->mido_intmdcidr) ||
            !strlen(eucanetd_config->mido_extmdcidr) ||
            !strlen(eucanetd_config->mido_mdcidr)) {
        LOGWARN("Cannot initialize mido with NULL config.\n");
        return (1);
    }

    mido->midocore = EUCA_ZALLOC_C(1, sizeof (mido_core));
    mido->midomd = EUCA_ZALLOC_C(1, sizeof (mido_md));

    mido->rt_ids = EUCA_ZALLOC_C(mido->config->mido_max_rtid, sizeof (boolean));
    mido->eni_ids = EUCA_ZALLOC_C(mido->config->mido_max_eniid, sizeof (boolean));

    mido->eucahome = strdup(eucanetd_config->eucahome);
    if (strlen(mido->eucahome)) {
        if (mido->eucahome[strlen(mido->eucahome) - 1] == '/') {
            mido->eucahome[strlen(mido->eucahome) - 1] = '\0';
        }
    }

    mido->disable_l2_isolation = eucanetd_config->disable_l2_isolation;

    mido->gni_gws = EUCA_ZALLOC_C(gni->max_midogws, sizeof (gni_mido_gateway));
    for (int i = 0; i < gni->max_midogws; i++) {
        gni_midogw_dup(&(mido->gni_gws[i]), &(gni->midogws[i]));
    }
    mido->max_gni_gws = gni->max_midogws;

    for (int i = 0; i < mido->max_gni_gws; i++) {
        LOGEXTREME("GW id=%d: %s/%s/%s\n",
                i, mido->gni_gws[i].host, mido->gni_gws[i].ext_ip, mido->gni_gws[i].ext_dev);
    }

    char nw[NETWORK_ADDR_LEN] = { 0 };
    char sn[NETWORK_ADDR_LEN] = { 0 };

    // Subnet used in internal routing - 169.254.0.0/17
    cidr_split(eucanetd_config->mido_intrtcidr, nw, sn, NULL, NULL, NULL);
    mido->int_rtnw = dot2hex(nw);
    mido->int_rtsn = atoi(sn);
    mido->int_rtaddr = mido->int_rtnw + 1;

    u32 netmask = 0;
    // Subnet used in internal MD routing - 0.0.0.0/17
    cidr_split(eucanetd_config->mido_intmdcidr, nw, sn, NULL, NULL, NULL);
    mido->mdconfig.int_mdsn = atoi(sn);
    netmask = (u32) 0xFFFFFFFF << (32 - mido->mdconfig.int_mdsn);
    mido->mdconfig.int_mdnw = dot2hex(nw) & netmask;

    // Subnet used for MD - 232.0.0.0/8
    cidr_split(eucanetd_config->mido_mdcidr, nw, sn, NULL, NULL, NULL);
    mido->mdconfig.mdsn = atoi(sn);
    netmask = (u32) 0xFFFFFFFF << (32 - mido->mdconfig.mdsn);
    mido->mdconfig.mdnw = dot2hex(nw) & netmask;

    // Subnet used in external MD routing - 169.254.169.248/29
    cidr_split(eucanetd_config->mido_extmdcidr, nw, sn, NULL, NULL, NULL);
    mido->mdconfig.ext_mdsn = atoi(sn);
    netmask = (u32) 0xFFFFFFFF << (32 - mido->mdconfig.ext_mdsn);
    mido->mdconfig.ext_mdnw = dot2hex(nw) & netmask;

    if (eucanetd_config->validate_mido_config) {
        if (validate_mido(mido) != EUCA_OK) {
            return (1);
        }
    }

    // Reserved IP addressses in external MD subnet
    mido->mdconfig.md_veth_mido = mido->mdconfig.ext_mdnw + 1;
    mido->mdconfig.md_veth_host = (mido->mdconfig.ext_mdnw | (~netmask)) - 1;
    mido->mdconfig.md_http = mido->mdconfig.md_veth_host;
    mido->mdconfig.md_dns = mido->mdconfig.md_veth_host - 1;

    LOGDEBUG("mido initialized: int_rtcidr=%s intmdcidr=%s extmdcidr=%s mdcidr=%s\n",
            SP(eucanetd_config->mido_intrtcidr), SP(eucanetd_config->mido_intmdcidr),
            SP(eucanetd_config->mido_extmdcidr), SP(eucanetd_config->mido_mdcidr));

    mido_set_apiuribase(mido->config->mido_api_uribase);
    midonet_api_init();
    mido_info_midonetapi();

    if (!strlen(mido_get_apiuribase())) {
        ret++;
    }
    return (ret);
}

/**
 * Reinitializes the given mido_config data structure.
 * @param mido [in] mido_config data structure of interest.
 * @return always 0.
 */
int reinitialize_mido(mido_config *mido) {
    LOGTRACE("Clearing current mido config.\n");
    clear_mido_config(mido);
    return (0);
}

/**
 * Validates the information in the given mido_config data structure.
 * @param mido [in] mido_config data structure of interest
 * @return EUCA_OK if mido_config parameters are all valid. EUCA_ERROR otherwise.
 */
int validate_mido(mido_config *mido) {
    int ret = EUCA_OK;

    if (mido->config->enable_mido_md) {
        // Internal metadata Routing Subnet - 169.254.128.0/17
        if ((mido->mdconfig.int_mdnw != 0xa9fe8000) || (mido->mdconfig.int_mdsn != 17)) {
            LOGERROR("Invalid MIDO_INTMD_CIDR - %s\n", mido->config->mido_intmdcidr);
            ret = EUCA_ERROR;
        }
        // External metadata Routing Subnet - 169.254.169.248/29
        if ((mido->mdconfig.ext_mdnw != 0xa9fea9f8) || (mido->mdconfig.ext_mdsn != 29)) {
            LOGERROR("Invalid MIDO_EXTMD_CIDR - %s\n", mido->config->mido_extmdcidr);
            ret = EUCA_ERROR;
        }
        // metadata Subnet - 255.0.0.0/8
        if ((mido->mdconfig.mdnw != 0xff000000) || (mido->mdconfig.mdsn != 8)) {
            LOGERROR("Invalid MIDO_MD_CIDR - %s\n", mido->config->mido_mdcidr);
            ret = EUCA_ERROR;
        }
    }
    // Internal Routing Subnet - 169.254.0.0/17
    if ((mido->int_rtnw != 0xa9fe0000) || (mido->int_rtsn != 17)) {
        LOGERROR("Invalid MIDO_INTRT_CIDR - %s\n", mido->config->mido_intrtcidr);
        ret = EUCA_ERROR;
    }
    if (mido->config->mido_max_rtid > 32750) {
        LOGERROR("Invalid MIDO_MAX_RTID - %d\n", mido->config->mido_max_rtid);
        ret = EUCA_ERROR;
    }
    if (mido->config->mido_max_eniid > 16777215) {
        LOGERROR("Invalid MIDO_MAX_ENIID - %d\n", mido->config->mido_max_eniid);
        ret = EUCA_ERROR;
    }
    // MidoNet-API base uri
    if (strlen(mido->config->mido_api_uribase)) {
        char *tmpstr = strdup(mido->config->mido_api_uribase);
        char *proto = NULL;
        char *host = NULL;
        char *port = NULL;
        char *subd = NULL;
        char *tmpptr = NULL;
        char *next = tmpstr;
        
        tmpptr = strstr(next, "://");
        if (tmpptr) {
            proto = next;
            *tmpptr = '\0';
            for (next = tmpptr + 3; *next == '/'; next++);
        }
        tmpptr = strchr(next, ':');
        if (tmpptr) {
            for (host = next; *host == '/'; host++);
            *tmpptr = '\0';
            next = tmpptr + 1;
            tmpptr = strchr(next, '/');
            if (tmpptr) {
                port = next;
                *tmpptr = '\0';
                next = tmpptr + 1;
            }
        } else {
            tmpptr = strchr(next, '/');
            if (tmpptr) {
                for (host = next; *host == '/'; host++);
                *tmpptr = '\0';
                next = tmpptr + 1;
            }
        }
        if (strlen(next)) {
            for (subd = next; *subd == '/'; subd++);
        }

        LOGTRACE("MN API proto %s, host %s, port %s, subd %s\n", SP(proto), SP(host), SP(port), SP(subd));
        if (!proto || !host || !subd) {
            LOGINFO("%s is not a valid MN API base uri\n", mido->config->mido_api_uribase);
            LOGINFO("MN API base uri reverted to default\n");
            mido->config->mido_api_uribase[0] = '\0';
        } else {
            if (strcmp(proto, "http") || (strcmp(host, "localhost") && strcmp(host, "127.0.0.1"))) {
                LOGWARN("Unsupported MidoNet API base URL - %s\n"
                        "Recommended base URLs: %s (MEM v1.9)\n"
                        "                       %s (MEM v5.2)\n",
                        mido->config->mido_api_uribase, MIDONET_API_BASE_URL_8080,
                        MIDONET_API_BASE_URL_8181);
            }
        }
        EUCA_FREE(tmpstr);
    }
    return (ret);
}

/**
 * Clear all gnipresent tags of discovered/populated MidoNet resources.
 * @param mido [in] data structure holding all discovered MidoNet resources.
 * @return always returns 0.
 */
int clear_mido_gnitags(mido_config *mido) {
    int i = 0, j = 0, k = 0;
    mido_vpc *vpc = NULL;
    mido_vpc_subnet *subnet = NULL;
    mido_vpc_instance *instance = NULL;
    mido_vpc_nacl *nacl = NULL;
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
            // go through NAT Gateways
            for (k = 0; k < subnet->max_natgateways; k++) {
                subnet->natgateways[k].gnipresent = 0;
                subnet->natgateways[k].midopresent = 0;
            }
            subnet->nacl_changed = 0;
            subnet->gnipresent = 0;
            subnet->midopresent = 0;
            //subnet->gniSubnet = NULL;
        }
        // for ecah VPC, go through network acls
        for (j = 0; j < vpc->max_nacls; j++) {
            nacl = &(vpc->nacls[j]);
            nacl->gnipresent = 0;
            nacl->midopresent = 0;
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

/**
 * Check if VPCMIDO tunnel-zone (assumed to be "mido-tz") exists.
 * "midotz", "euca-tz", and "eucatz" are also accepted.
 * @param gni [in] current global network state.
 * @param msg [out] pointer to a buffer where log messages will be written.
 * @param msg_len [out] length of msg buffer.
 * @return 0 if mido-tz is found and has at least 1 member. 1 otherwise.
 */
int check_mido_tunnelzone(globalNetworkInfo *gni, char **msg, int *msg_len) {
    int rc = 0;
    int i = 0;
    char *tztype = NULL;
    midoname **tzs = NULL;
    int max_tzs = 0;
    midoname **tzhosts = NULL;
    int max_tzhosts = 0;
    char **tzhostuuids = NULL;
    int max_tzhostuuids = 0;
    midonet_api_host *h = NULL;
    boolean tzfound = FALSE;
    boolean tzok = FALSE;

    rc = mido_get_tunnelzones(VPCMIDO_TENANT, &tzs, &max_tzs);
    if (rc == 0) {
        for (i = 0; i < max_tzs; i++) {
            rc = mido_getel_midoname(tzs[i], "type", &tztype);
            if ((rc == 0) && (strstr(VPCMIDO_TUNNELZONE, tzs[i]->name))) {
                rc = mido_get_tunnelzone_hosts(tzs[i], &tzhosts, &max_tzhosts);
                if ((rc == 0) && (max_tzhosts > 0)) {
                    tzfound = TRUE;
                    tzok = TRUE;
                    LOGDEBUG("\tfound %s tunnel-zone %s with %d members\n", tztype, tzs[i]->name, max_tzhosts);
                    euca_buffer_snprintf(msg, msg_len, "\tfound %s tunnel-zone %s with %d members\n", tztype, tzs[i]->name, max_tzhosts);
                    for (int j = 0; j < max_tzhosts; j++) {
                        char *hostid = NULL;
                        mido_getel_midoname(tzhosts[j], "hostId", &hostid);
                        if (hostid) {
                            euca_string_set_insert(&tzhostuuids, &max_tzhostuuids, hostid);
                        }
                        EUCA_FREE(hostid);
                    }
                    for (int j = 0; j < gni->max_clusters; j++) {
                        for (int k = 0; k < gni->clusters[j].max_nodes; k++) {
                            h = mido_get_host_byname(gni->clusters[j].nodes[k].name);
                            if (h && euca_string_set_get(tzhostuuids, max_tzhostuuids, h->obj->uuid)) {
                                LOGTRACE("%s found in %s\n", gni->clusters[j].nodes[k].name, tzs[i]->name);
                            } else {
                                if (h) {
                                    euca_buffer_snprintf(msg, msg_len, "\t\t%s not found in MN tunnel-zone.\n", gni->clusters[j].nodes[k].name);
                                    tzok = FALSE;
                                } else {
                                    LOGWARN("\t\t%s not found in MN.\n", gni->clusters[j].nodes[k].name);
                                }
                            }
                        }
                    }
                    for (int j = 0; j < gni->max_midogws; j++) {
                        h = mido_get_host_byname(gni->midogws[j].host);
                        if (h && euca_string_set_get(tzhostuuids, max_tzhostuuids, h->obj->uuid)) {
                            LOGTRACE("%s found in %s\n", gni->midogws[j].host, tzs[i]->name);
                        } else {
                            if (h) {
                                euca_buffer_snprintf(msg, msg_len, "\t\t%s not found in MN tunnel-zone.\n", gni->midogws[j].host);
                                tzok = FALSE;
                            } else {
                                LOGWARN("\t\t%s not found in MN.\n", gni->midogws[j].host);
                            }
                        }
                    }
                    char *clcip = hex2dot_s(gni->enabledCLCIp);
                    h = mido_get_host_byname(clcip);
                    if (h && euca_string_set_get(tzhostuuids, max_tzhostuuids, h->obj->uuid)) {
                        LOGTRACE("%s found in %s\n", clcip, tzs[i]->name);
                    } else {
                        if (h) {
                            euca_buffer_snprintf(msg, msg_len, "\t\t%s not found in MN tunnel-zone.\n", clcip);
                            tzok = FALSE;
                        } else {
                            LOGWARN("\t\t%s not found in MN.\n", clcip);
                        }
                    }
                    free_ptrarr(tzhostuuids, max_tzhostuuids);
                }
                if (tzhosts) {
                    EUCA_FREE(tzhosts);
                }
            }
            if (tztype) {
                EUCA_FREE(tztype);
            }
        }
    } else {
        euca_buffer_snprintf(msg, msg_len, "Failed to retrieve MidoNet tunnel-zones.\n");
    }

    if (tzs) {
        EUCA_FREE(tzs);
    }

    return ((tzfound && tzok) ? 0 : 1);
}

/**
 * Searches for BGP configuration in midocore, and returns a string containing midonet-cli
 * commands to recover the BGP configuration.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return string containing midonet-cli commands to recover the found BGP configuration.
 * NULL if BGP is not found.
 */
char *discover_mido_bgps(mido_config *mido) {
    char *ret = NULL;

    if (mido && mido->gni_gws && mido->max_gni_gws) {
        if (mido->gni_gws[0].asn) {
            return (ret);
        }
    }
    if (is_midonet_api_v1()) {
        ret = discover_mido_bgps_v1(mido);
    } else if (is_midonet_api_v5()) {
        ret = discover_mido_bgps_v5(mido);
    }
    return (ret);
}

/**
 * Searches for BGP configuration in midocore, and returns a string containing midonet-cli
 * commands to recover the BGP configuration. Compatible with MN1.9.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return string containing midonet-cli commands to recover the found BGP configuration.
 * NULL if BGP is not found.
 */
char *discover_mido_bgps_v1(mido_config *mido) {
    if (!mido || !mido->midocore) {
        return (NULL);
    }

    boolean mna_defined = FALSE;
    char *res = EUCA_ZALLOC_C(2048, sizeof (char));
    char *resptr = &(res[0]);
    mido_core *midocore = mido->midocore;
    for (int i = 0; i < midocore->max_gws; i++) {
        mido_gw *gw = midocore->gws[i];
        if (!gw) {
            continue;
        }
        if (gw->asn) {
            if (!mna_defined) {
                mna_defined = TRUE;
                resptr = &(res[strlen(res)]);
                snprintf(resptr, 1024, "MNA=\"midonet-cli -A --midonet-url=%s\"\n",
                        midonet_api_get_uribase(NULL));
            }
            resptr = &(res[strlen(res)]);
            snprintf(resptr, 1024, "GWPORT%d=$($MNA -e router name eucart list port | grep %s | awk '{print $2}')\n",
                    i, gw->ext_ip);
            resptr = &(res[strlen(res)]);
            snprintf(resptr, 1024, "BGP%d=$($MNA -e port $GWPORT%d add bgp local-AS %d peer-AS %d peer %s)\n",
                    i, i, gw->asn, gw->peer_asn, gw->peer_ip);
            for (int j = 0; j < gw->max_ad_routes; j++) {
                mido_gw_ad_route *ar = gw->ad_routes[j];
                if (!ar) continue;
                resptr = &(res[strlen(res)]);
                snprintf(resptr, 1024, "$MNA -e port $GWPORT%d bgp $BGP%d add route net %s\n",
                        i, i, ar->cidr);
            }
        }
    }
    return (res);
}

/**
 * Searches for BGP configuration in midocore, and returns a string containing midonet-cli
 * commands to recover the BGP configuration. Compatible with MN5.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return string containing midonet-cli commands to recover the found BGP configuration.
 * NULL if BGP is not found.
 */
char *discover_mido_bgps_v5(mido_config *mido) {
    if (!mido || !mido->midocore || !mido->midocore->eucart) {
        return (NULL);
    }

    boolean mna_defined = FALSE;
    char *res = EUCA_ZALLOC_C(2048, sizeof (char));
    char *resptr = &(res[0]);
    mido_core *midocore = mido->midocore;
    for (int i = 0; i < midocore->max_gws; i++) {
        mido_gw *gw = midocore->gws[i];
        if (!gw) {
            continue;
        }
        if (gw->asn) {
            if (!mna_defined) {
                mna_defined = TRUE;
                resptr = &(res[strlen(res)]);
                snprintf(resptr, 1024, "MNA=\"midonet-cli -A --midonet-url=%s\"\n",
                        midonet_api_get_uribase(NULL));
            }
            resptr = &(res[strlen(res)]);
            snprintf(resptr, 1024, "GWPORT%d=$($MNA -e router name eucart list port | grep %s | awk '{print $2}')\n",
                    i, gw->ext_ip);
            resptr = &(res[strlen(res)]);
            snprintf(resptr, 1024, "$MNA -e router name eucart set asn %d\n",
                    gw->asn);
            resptr = &(res[strlen(res)]);
            snprintf(resptr, 1024, "$MNA -e router name eucart add bgp-peer asn %d address %s\n",
                    gw->peer_asn, gw->peer_ip);
            for (int j = 0; j < gw->max_bgp_networks; j++) {
                mido_gw_ad_route *ar = gw->bgp_networks[j];
                if (!ar) continue;
                resptr = &(res[strlen(res)]);
                snprintf(resptr, 1024, "$MNA -e router name eucart add bgp-network net %s\n",
                        ar->cidr);
            }
        }
    }
    return (res);
}

/**
 * Populates an euca VPCMIDO gateway BGP configuration.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param port [in] data structure that holds MN port bound to VPCMIDO gateway.
 * @param gw [in] data structure that holds the euca VPCMIDO gateway model of interest.
 * @return 0 on success. 1 on any failure.
 */
int populate_mido_gw_bgp(mido_config *mido, midoname *port, mido_gw *gw) {
    int ret = 0;

    if (is_midonet_api_v1()) {
        ret = populate_mido_gw_bgp_v1(mido, port, gw);
    } else if (is_midonet_api_v5()) {
        ret = populate_mido_gw_bgp_v5(mido, port, gw);
    }
    return (ret);
}

/**
 * Populates an euca VPCMIDO gateway BGP configuration. Compatible with MN1.9.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param port [in] data structure that holds MN port bound to VPCMIDO gateway.
 * @param gw [in] data structure that holds the euca VPCMIDO gateway model of interest.
 * @return 0 on success. 1 on any failure.
 */
int populate_mido_gw_bgp_v1(mido_config *mido, midoname *port, mido_gw *gw) {
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

    if (!mido || !port || !gw) {
        LOGWARN("Invalid argument: cannot populate BGP from NULL\n");
        return (1);
    }
    bgps = NULL;
    gw->port = port;
    rc = mido_get_bgps(port, &bgps, &max_bgps);
    if (!rc && bgps && max_bgps) {
        if (max_bgps > 1) {
            LOGWARN("Unexpected number (%d) of bgps found on port %s\n", max_bgps, port->uuid);
        } else {
            gw->bgp_v1 = bgps[0];
            mido_getel_midoname(bgps[0], "localAS", &localAS);
            mido_getel_midoname(bgps[0], "peerAddr", &peerAddr);
            mido_getel_midoname(bgps[0], "peerAS", &peerAS);
            if (!localAS || !peerAS || !peerAS) {
                LOGWARN("failed to retrieve bgp information from port %s\n", port->uuid);
            } else {
                gw->asn = atoi(localAS);
                gw->peer_asn = atoi(peerAS);
                snprintf(gw->peer_ip, NETWORK_ADDR_LEN, "%s", peerAddr);

                rc = mido_get_bgp_routes(bgps[0], &bgp_routes, &max_bgp_routes);
                if (!rc && bgp_routes && max_bgp_routes) {
                    gw->ad_routes = EUCA_ZALLOC_C(max_bgp_routes, sizeof (mido_gw_ad_route *));
                    int bgp_route_idx = 0;
                    for (int j = 0; j < max_bgp_routes; j++) {
                        mido_getel_midoname(bgp_routes[j], "nwPrefix", &nwPrefix);
                        mido_getel_midoname(bgp_routes[j], "prefixLength", &prefixLength);
                        if (!nwPrefix || !prefixLength) {
                            LOGWARN("failed to retrieve bgp ad routes from %s\n", bgps[0]->uuid);
                            continue;
                        } else {
                            gw->ad_routes[bgp_route_idx] = EUCA_ZALLOC_C(1, sizeof (mido_gw_ad_route));
                            snprintf(gw->ad_routes[bgp_route_idx]->cidr, NETWORK_ADDR_LEN, "%s/%s", nwPrefix, prefixLength);
                            gw->ad_routes[bgp_route_idx]->route = bgp_routes[j];
                            bgp_route_idx++;
                        }
                        EUCA_FREE(nwPrefix);
                        EUCA_FREE(prefixLength);
                    }
                    gw->max_ad_routes = bgp_route_idx;
                }
                EUCA_FREE(bgp_routes);
            }
            EUCA_FREE(localAS);
            EUCA_FREE(peerAddr);
            EUCA_FREE(peerAS);
        }
    }
    EUCA_FREE(bgps);
    return (0);
}

/**
 * Populates an euca VPCMIDO gateway BGP configuration. Compatible with MN5.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param port [in] data structure that holds MN port bound to VPCMIDO gateway.
 * @param gw [in] data structure that holds the euca VPCMIDO gateway model of interest.
 * @return 0 on success. 1 on any failure.
 */
int populate_mido_gw_bgp_v5(mido_config *mido, midoname *port, mido_gw *gw) {
    midoname **bgps = NULL;
    int max_bgps = 0;
    char *localAS = NULL;
    char *peerAddr = NULL;
    char *peerAS = NULL;

    if (!mido || !mido->midocore || !mido->midocore->eucart || !port || !gw) {
        LOGWARN("Invalid argument: cannot populate BGP from NULL\n");
        return (1);
    }
    gw->port = port;
    mido_core *midocore = mido->midocore;
    if (midocore->eucart) {
        mido_getel_midoname(midocore->eucart->obj, "asNumber", &localAS);
        if (localAS && strlen(localAS) && strcmp(localAS, "0")) {
            gw->asn = atoi(localAS);
        }
        EUCA_FREE(localAS);
    }

    bgps = mido->midocore->bgp_peers;
    max_bgps = mido->midocore->max_bgp_peers;
    boolean found = FALSE;
    for (int i = 0; i < max_bgps && !found; i++) {
        mido_getel_midoname(bgps[i], "address", &peerAddr);
        mido_getel_midoname(bgps[i], "asNumber", &peerAS);
        if (!peerAS || !peerAddr) {
            LOGWARN("failed to retrieve bgp information\n");
        } else {
            if (!strcmp(peerAddr, gw->peer_ip)) {
                found = TRUE;
                gw->bgp_peer = bgps[i];
                gw->peer_asn = atoi(peerAS);
            }
        }
        EUCA_FREE(peerAddr);
        EUCA_FREE(peerAS);
    }
    gw->bgp_networks = mido->midocore->bgp_networks;
    gw->max_bgp_networks = mido->midocore->max_bgp_networks;
    return (0);
}

/**
 * Populates an euca VPCMIDO gateway model.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param port [in] data structure that holds MN port bound to VPCMIDO gateway.
 * @param gw [in] data structure that holds the euca VPCMIDO gateway model of interest.
 * @return 0 on success. 1 on any failure.
 */
int populate_mido_gw(mido_config *mido, midoname *port, mido_gw *gw) {
    int ret = 0;
    if (!mido || !mido->midocore || !mido->midocore->eucart || !port || !port->port ||
            !port->port->ifname || !port->port->netaddr || !port->port->netlen ||
            !port->port->portaddr || !gw || !gw->host) {
        LOGWARN("Invalid argument: cannot populate gw from NULL\n");
        return (1);
    }
    snprintf(gw->ext_dev, IF_NAME_LEN, "%s", port->port->ifname);
    snprintf(gw->ext_cidr, NETWORK_ADDR_LEN, "%s/%s", port->port->netaddr, port->port->netlen);
    snprintf(gw->ext_ip, NETWORK_ADDR_LEN, "%s", port->port->portaddr);
    midonet_api_router *eucart = mido->midocore->eucart;
    for (int i = 0; i < eucart->max_routes && (!gw->def_route || !gw->ext_cidr_route || !gw->peer_ip_route); i++) {
        if (!eucart->routes[i]) {
            continue;
        }
        if (strcmp(eucart->routes[i]->route->nexthopport, port->uuid)) {
            continue;
        }
        // default route
        if (eucart->routes[i]->route->nexthopgateway &&
                !strcmp(eucart->routes[i]->route->dstlen, "0") &&
                !strcmp(eucart->routes[i]->route->dstnet, "0.0.0.0")) {
            snprintf(gw->peer_ip, NETWORK_ADDR_LEN, "%s", eucart->routes[i]->route->nexthopgateway);
            gw->def_route = eucart->routes[i];
            LOGTRACE("gw %s found default route via %s\n", gw->host->obj->name, eucart->routes[i]->route->nexthopgateway);
            continue;
        }
        if (eucart->routes[i]->route->nexthopgateway) {
            continue;
        }
        // peer_ip route
        if (!strcmp(eucart->routes[i]->route->dstlen, "32") &&
                strcmp(eucart->routes[i]->route->dstnet, port->port->portaddr)) {
            gw->peer_ip_route = eucart->routes[i];
            LOGTRACE("gw %s found peer_ip route via %s\n", gw->host->obj->name, port->name);
            continue;
        }
        // ext_cidr route
        if (!strcmp(eucart->routes[i]->route->dstlen, port->port->netlen) &&
                !strcmp(eucart->routes[i]->route->dstnet, port->port->netaddr)) {
            gw->ext_cidr_route = eucart->routes[i];
            LOGTRACE("gw %s found ext_cidr route via %s\n", gw->host->obj->name, port->name);
            continue;
        }
    }
    // peer_ip
    if (gw->peer_ip_route) {
        snprintf(gw->peer_ip, NETWORK_ADDR_LEN, "%s", gw->peer_ip_route->route->dstnet);
    } else if (gw->def_route) {
        snprintf(gw->peer_ip, NETWORK_ADDR_LEN, "%s", gw->def_route->route->nexthopgateway);
    }
    populate_mido_gw_bgp(mido, port, gw);
    return (ret);
}

/**
 * Creates MidoNet objects that are needed to implement VPCMIDO gateway.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param gw [in] data structure that holds information about the gateway of interest
 * @param gni_gw [in] dat structure that holds information about paramters of the gateway
 * of interest, as described in GNI.
 * @return 0 on success. 1 otherwise.
 */
int create_mido_gw(mido_config *mido, mido_gw *gw, gni_mido_gateway *gni_gw) {
    char ext_net[32];
    char ext_len[32];
    int rc = 0;
    int ret = 0;

    if (!mido || !mido->midocore || !gw || !gni_gw) {
        LOGWARN("Invalid argument: cannot create mido gateway from NULL\n");
        return (1);
    }

    mido_core *midocore = mido->midocore;
    cidr_split(gni_gw->ext_cidr, ext_net, ext_len, NULL, NULL, NULL);
    rc = mido_create_router_port(midocore->eucart, midocore->midos[CORE_EUCART], gni_gw->ext_ip,
            ext_net, ext_len, NULL, &(gw->port));
    if (rc) {
        LOGERROR("cannot create router port: check midonet health\n");
        ret++;
    }

    if (gw->port->init) {
        // exterior port GW IP
        rc = mido_create_route(midocore->eucart, midocore->midos[CORE_EUCART], gw->port,
                "0.0.0.0", "0", ext_net, ext_len, "UNSET", "0", NULL);
        rc = rc << 1;
        rc |= mido_create_route(midocore->eucart, midocore->midos[CORE_EUCART], gw->port,
                "0.0.0.0", "0", gni_gw->peer_ip, "32", "UNSET", "0", NULL);
        if (rc) {
            LOGERROR("cannot create router route(%x): check midonet health\n", rc);
            ret++;
        }

        // exterior port default GW
        rc = mido_create_route(midocore->eucart, midocore->midos[CORE_EUCART], gw->port,
                "0.0.0.0", "0", "0.0.0.0", "0", gni_gw->peer_ip, "0", NULL);
        if (rc) {
            LOGERROR("cannot create router route: check midonet health\n");
            ret++;
        }

        // link exterior port
        midonet_api_host *gwhost = mido_get_host_byname(gni_gw->host);
        if (gwhost) {
            rc = mido_link_host_port(gwhost->obj, gni_gw->ext_dev, midocore->midos[CORE_EUCART], gw->port);
            if (rc) {
                LOGERROR("cannot link router port to host interface: check midonet health\n");
                ret++;
            }
        } else {
            LOGERROR("cannot find gw host %s in midonet\n", gni_gw->host);
            ret++;
        }

        rc = mido_create_portgroup_port(midocore->midos[CORE_GWPORTGROUP],
                gw->port, NULL);
        if (rc) {
            LOGWARN("cannot add portgroup port: check midonet health\n");
            ret++;
        }
    }
    return (ret);
}

/**
 * Creates MidoNet objects that are needed to configure VPCMIDO gateway(s) BGP.
 * This function should only be called once base constructs of gateways are in place
 * (i.e., create_mido_gw() for each gateway in GNI).
 * No-op for gateways with BGP constructs in place (diff between MN and GNI not performed).
 * Changes in configuration must be detected (and cleaned) before calling this function.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @return 0 on success. 1 otherwise.
 */
int create_mido_gws_bgp(mido_config *mido, mido_core *midocore) {
    int ret = 0;
    if (!mido || !mido->midocore) {
        LOGWARN("Invalid argument: cannot create mido gateway BGP from NULL\n");
        return (1);
    }

    boolean config_bgp = FALSE;
    for (int i = 0; i < mido->max_gni_gws; i++) {
        gni_mido_gateway *gni_gw = &(mido->gni_gws[i]);
        if (gni_gw->asn && gni_gw->peer_asn) {
            config_bgp = TRUE;
            break;
        }
    }

    if (config_bgp) {
        if (is_midonet_api_v1()) {
            ret = create_mido_gws_bgp_v1(mido, midocore);
        } else if (is_midonet_api_v5()) {
            ret = create_mido_gws_bgp_v5(mido, midocore);
        }
    } else {
        LOGDEBUG("bgp config not detected in GNI\n");
    }
    return (ret);
}

/**
 * Creates MidoNet objects that are needed to configure VPCMIDO gateway(s) BGP.
 * For MN API v1.9
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @return 0 on success. 1 otherwise.
 */
int create_mido_gws_bgp_v1(mido_config *mido, mido_core *midocore) {
    int rc = 0;
    int ret = 0;
    if (!mido || !mido->midocore) {
        LOGWARN("Invalid argument: cannot create mido gateway BGP v1.9 from NULL\n");
        return (1);
    }

    // All constructs in place assumed to be valid (if not, it should have been deleted)
    for (int i = 0; i < midocore->max_gws; i++) {
        mido_gw *gw = midocore->gws[i];
        if (!gw) {
            continue;
        }
        gni_mido_gateway *gni_gw = gw->gni_gw;
        if (gw->port) {
            // no-op if bgp config was detected in this port
            if (!gw->bgp_v1) {
                LOGTRACE("creating bgp %u->%u(%s)\n", gni_gw->asn, gni_gw->peer_asn, gni_gw->peer_ip);
                rc = mido_create_bgp_v1(gw->port, gni_gw->asn, gni_gw->peer_asn, gni_gw->peer_ip, &(gw->bgp_v1));
                if (rc) {
                    LOGWARN("Failed to create bgp %u->%u(%s)\n", gni_gw->asn, gni_gw->peer_asn, gni_gw->peer_ip);
                    ret++;
                }
            }
            // no-op if bgp ad-routes were detected in this bgp
            if (gw->bgp_v1 && !gw->max_ad_routes && !gw->ad_routes) {
                for (int j = 0; j < gni_gw->max_ad_routes; j++) {
                    char nw[NETWORK_ADDR_LEN];
                    char len[NETWORK_ADDR_LEN];
                    cidr_split(gni_gw->ad_routes[j], nw, len, NULL, NULL, NULL);
                    if (strlen(nw) && strlen(len)) {
                        midoname *out = NULL;
                        LOGTRACE("creating bgp ad-route %s\n", gni_gw->ad_routes[j]);
                        rc = mido_create_bgp_route_v1(gw->bgp_v1, nw, len, &out);
                        if (rc) {
                            LOGWARN("Failed to create bgp ad-route %s\n", gni_gw->ad_routes[j]);
                        } else {
                            mido_gw_ad_route *adr = EUCA_ZALLOC_C(1, sizeof (mido_gw_ad_route));
                            gw->ad_routes = EUCA_APPEND_PTRARR(gw->ad_routes, &(gw->max_ad_routes), adr);
                            snprintf(adr->cidr, NETWORK_ADDR_LEN, "%s", gni_gw->ad_routes[j]);
                            adr->route = out;
                        }
                    }
                }
            }
        }
    }
    return (ret);
}

/**
 * Creates MidoNet objects that are needed to configure VPCMIDO gateway(s) BGP.
 * For MN API v5.0
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @return 0 on success. 1 otherwise.
 */
int create_mido_gws_bgp_v5(mido_config *mido, mido_core *midocore) {
    int rc = 0;
    int ret = 0;
    if (!mido || !mido->midocore || !midocore->eucart) {
        LOGWARN("Invalid argument: cannot create mido gateway BGP v5.0 from NULL\n");
        return (1);
    }

    // All constructs in place assumed to be valid (if not, it should have been deleted)
    for (int i = 0; i < midocore->max_gws; i++) {
        mido_gw *gw = midocore->gws[i];
        if (!gw) {
            continue;
        }
        gni_mido_gateway *gni_gw = gw->gni_gw;
        if (gw->port) {
            // no-op if bgp config was detected in this port
            if (!gw->bgp_peer) {
                LOGTRACE("creating bgp-peer %u->%u(%s)\n", gni_gw->asn, gni_gw->peer_asn, gni_gw->peer_ip);
                rc = mido_create_bgp_v5(midocore->eucart->obj, gni_gw->asn, gni_gw->peer_asn, gni_gw->peer_ip, &(gw->bgp_peer));
                if (rc) {
                    LOGWARN("Failed to create bgp-peer %u->%u(%s)\n", gni_gw->asn, gni_gw->peer_asn, gni_gw->peer_ip);
                    ret++;
                } else {
                    midocore->bgp_peers = EUCA_APPEND_PTRARR(midocore->bgp_peers,
                            &(midocore->max_bgp_peers), gw->bgp_peer);
                }
            } else {
                LOGTRACE("found bgp-peer %u->%u(%s)\n", gni_gw->asn, gni_gw->peer_asn, gni_gw->peer_ip);
            }
        }
    }

    char **cidrs = NULL;
    int max_cidrs = 0;
    for (int i = 0; i < mido->max_gni_gws; i++) {
        gni_mido_gateway *gni_gw = &(mido->gni_gws[i]);
        for (int j = 0; j < gni_gw->max_ad_routes; j++) {
            euca_string_set_insert(&cidrs, &max_cidrs, gni_gw->ad_routes[j]);
        }
    }

    for (int i = 0; i < max_cidrs; i++) {
        boolean found = FALSE;
        for (int j = 0; j < midocore->max_bgp_networks; j++) {
            if (!strcmp(cidrs[i], midocore->bgp_networks[j]->cidr)) {
                found = TRUE;
            }
        }
        if (!found) {
            LOGTRACE("creating bgp-network %s\n", cidrs[i]);
            char nw[NETWORK_ADDR_LEN];
            char len[NETWORK_ADDR_LEN];
            cidr_split(cidrs[i], nw, len, NULL, NULL, NULL);
            if (strlen(nw) && strlen(len)) {
                midoname *out = NULL;
                rc = mido_create_bgp_route_v5(midocore->eucart->obj, nw, len, &out);
                if (rc) {
                    LOGWARN("Failed to create bgp-network %s\n", cidrs[i]);
                } else {
                    mido_gw_ad_route *adr = EUCA_ZALLOC_C(1, sizeof (mido_gw_ad_route));
                    midocore->bgp_networks = EUCA_APPEND_PTRARR(midocore->bgp_networks, &(midocore->max_bgp_networks), adr);
                    snprintf(adr->cidr, NETWORK_ADDR_LEN, "%s", cidrs[i]);
                    adr->route = out;
                }
            }
        } else {
            LOGTRACE("found bgp-network %s\n", cidrs[i]);
        }
    }

    for (int i = 0; i < max_cidrs; i++) {
        EUCA_FREE(cidrs[i]);
    }
    EUCA_FREE(cidrs);

    return (ret);
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
    char tmp_name[MIDO_NAME_LEN] = { 0 };

    if (mido->midocore->eucanetdhost) {
        vpcsubnet->midos[SUBN_BR_METAHOST] = mido->midocore->eucanetdhost->obj;
    }

    midonet_api_bridge *subnetbridge = vpcsubnet->subnetbr;

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
                if (rtports[j]->port && rtports[j]->port->peerid && !strcmp(rtports[j]->port->peerid, brports[i]->uuid)) {
                    LOGTRACE("Found rt-br link %s %s", rtports[j]->name, brports[i]->name);
                    vpcsubnet->midos[SUBN_BR_RTPORT] = brports[i];
                    vpcsubnet->midos[SUBN_VPCRT_BRPORT] = rtports[j];
                    found = 1;
                    
                    // populate the dot2 arp entry
                    if (vpcsubnet->midos[SUBN_VPCRT_BRPORT] && vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port) {
                        midoname *dot2ip4mac = midonet_api_cache_lookup_ip4mac_bymac(
                                vpcsubnet->subnetbr, vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port->portmac, NULL);
                        if (dot2ip4mac) {
                            vpcsubnet->midos[SUBN_BR_DOT2ARP] = dot2ip4mac;
                            LOGTRACE("Found dot2 arp %s_%s\n", vpcsubnet->midos[SUBN_BR_DOT2ARP]->ip4mac->ip, vpcsubnet->midos[SUBN_BR_DOT2ARP]->ip4mac->mac);
                        }
                    }
                }
            }
        }
        found = 0;
        for (i = 0; i < max_brports && !found; i++) {
            if (brports[i] == NULL) {
                continue;
            }
            if (!rc && brports[i]->port && brports[i]->port->ifname && strlen(brports[i]->port->ifname) && strstr(brports[i]->port->ifname, "vn0_")) {
                // found the meta iface
                LOGTRACE("Found meta interface %s", brports[i]->name);
                vpcsubnet->midos[SUBN_BR_METAPORT] = brports[i];
                found = 1;
            }
        }

        // populate vpcsubnet routes
        rc = find_mido_vpc_subnet_routes(mido, vpc, vpcsubnet);
        if (rc != 0) {
            LOGWARN("%s failed to populate route table.\n", vpcsubnet->name);
        }

        // populate vpcsubnet infilter and outfilter
        snprintf(tmp_name, MIDO_NAME_LEN, "sc_%s_in", vpcsubnet->name);
        midonet_api_chain *sc = mido_get_chain(tmp_name);
        if (sc != NULL) {
            LOGTRACE("Found chain %s\n", sc->obj->name);
            vpcsubnet->inchain = sc;
            vpcsubnet->midos[SUBN_BR_INFILTER] = sc->obj;
        }
        snprintf(tmp_name, MIDO_NAME_LEN, "sc_%s_out", vpcsubnet->name);
        sc = mido_get_chain(tmp_name);
        if (sc != NULL) {
            LOGTRACE("Found chain %s\n", sc->obj->name);
            vpcsubnet->outchain = sc;
            vpcsubnet->midos[SUBN_BR_OUTFILTER] = sc->obj;
        }
    }

    LOGTRACE("vpc subnet (%s): AFTER POPULATE\n", vpcsubnet->name);
    for (i = 0; i < SUBN_END; i++) {
        if (vpcsubnet->midos[i] == NULL) {
            LOGWARN("%s failed to populate resource at idx %d\n", vpcsubnet->name, i);
            vpcsubnet->population_failed = 1;
        } else {
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
    int ret = 0, i = 0, j = 0;
    char vpcname[MIDO_NAME_LEN];

    midonet_api_chain *chain = NULL;

    snprintf(vpcname, MIDO_NAME_LEN, "vc_%s_prechain", vpc->name);
    chain = mido_get_chain(vpcname);
    if (chain != NULL) {
        LOGTRACE("Found chain %s", chain->obj->name);
        vpc->rt_uplink_prechain = chain;
        vpc->midos[VPC_VPCRT_UPLINK_PRECHAIN] = chain->obj;
    }

    snprintf(vpcname, MIDO_NAME_LEN, "vc_%s_preelip", vpc->name);
    chain = mido_get_chain(vpcname);
    if (chain != NULL) {
        LOGTRACE("Found chain %s", chain->obj->name);
        vpc->rt_preelipchain = chain;
        vpc->midos[VPC_VPCRT_PREELIPCHAIN] = chain->obj;
    }

    snprintf(vpcname, MIDO_NAME_LEN, "vc_%s_postchain", vpc->name);
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
                if (rtports[j]->port && rtports[j]->port->peerid && !strcmp(rtports[j]->port->peerid, brports[i]->uuid)) {
                    LOGTRACE("Found rt-br link %s %s", rtports[j]->name, brports[i]->name);
                    vpc->midos[VPC_EUCABR_DOWNLINK] = brports[i];
                    vpc->midos[VPC_VPCRT_UPLINK] = rtports[j];
                    found = 1;
                }
            }
        }
    }

    found = 0;
    if (mido->config->enable_mido_md || mido->config->populate_mido_md) {
        snprintf(vpcname, MIDO_NAME_LEN, "vc_%s_mdulin", vpc->name);
        chain = mido_get_chain(vpcname);
        if (chain != NULL) {
            LOGTRACE("Found chain %s", chain->obj->name);
            vpc->rt_mduplink_infilter = chain;
            vpc->midos[VPC_VPCRT_MDUPLINK_INFILTER] = chain->obj;
        }
        snprintf(vpcname, MIDO_NAME_LEN, "vc_%s_mdulout", vpc->name);
        chain = mido_get_chain(vpcname);
        if (chain != NULL) {
            LOGTRACE("Found chain %s", chain->obj->name);
            vpc->rt_mduplink_outfilter = chain;
            vpc->midos[VPC_VPCRT_MDUPLINK_OUTFILTER] = chain->obj;
        }
        if ((vpc->vpcrt) && (mido->midomd) && (mido->midomd->eucamdbr)) {
            midoname **brports = mido->midomd->eucamdbr->ports;
            int max_brports = mido->midomd->eucamdbr->max_ports;
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
                    if (rtports[j]->port && rtports[j]->port->peerid && !strcmp(rtports[j]->port->peerid, brports[i]->uuid)) {
                        LOGTRACE("Found rt-mdbr link %s %s", rtports[j]->name, brports[i]->name);
                        vpc->midos[VPC_EUCAMDBR_DOWNLINK] = brports[i];
                        vpc->midos[VPC_VPCRT_MDUPLINK] = rtports[j];
                        found = 1;
                    }
                }
            }
        }
    }

    LOGTRACE("vpc (%s): AFTER POPULATE\n", vpc->name);
    int iend = mido->config->enable_mido_md ? VPC_END : VPC_EUCAMDBR_DOWNLINK;
    for (i = 0; i < iend; i++) {
        if (vpc->midos[i] == NULL) {
            LOGWARN("%s failed to populate resource at idx %d\n", vpc->name, i);
            vpc->population_failed = 1;
        } else {
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
    int ret = 0, rc = 0, i = 0, j = 0;
    int found = 0;
    midoname **brports = NULL;
    int max_brports = 0;
    midoname **rtports = NULL;
    int max_rtports = 0;

    midonet_api_host *endhost = NULL;
    for (i = 0; i < 60 && !endhost; i++) {
        euca_getifaddrs(&(mido->config->my_ips), &(mido->config->max_my_ips));
        if (mido->config->my_ips && mido->config->max_my_ips) {
            char *strptr = hex2dot(mido->config->my_ips[0]);
            endhost = mido_get_host_byip(strptr);
            EUCA_FREE(strptr);
        }
        if (endhost) {
            midocore->eucanetdhost = endhost;
            LOGTRACE("\tfound eucanetdhost %s\n", endhost->obj->name);
        } else {
            if (i == 0) {
                LOGWARN("Unable to find eucanetd host in mido\n");
            }
            sleep(1);
        }
    }
    if (!endhost) {
        // Meta-tap setup will fail without eucanetdhost
        LOGERROR("eucanetd host not found in mido\n");
        LOGINFO("Make sure that midolman is running\n");
        ret++;
    }
    
    midonet_api_router *eucart = NULL;
    eucart = mido_get_router(VPCMIDO_CORERT);
    if (eucart) {
        LOGTRACE("Found core router %s\n", eucart->obj->name);
        midocore->eucart = eucart;
        midocore->midos[CORE_EUCART] = eucart->obj;
        rtports = eucart->ports;
        max_rtports = eucart->max_ports;
    } else {
        LOGINFO("\t\tcore router not found\n");
    }

    midonet_api_bridge *eucabr = NULL;
    eucabr = mido_get_bridge(VPCMIDO_COREBR);
    if (eucabr) {
        LOGTRACE("Found core bridge %s\n", eucabr->obj->name);
        midocore->eucabr = eucabr;
        midocore->midos[CORE_EUCABR] = eucabr->obj;
        brports = eucabr->ports;
        max_brports = eucabr->max_ports;
    } else {
        LOGINFO("\t\tcore bridge not found\n");
    }

    midonet_api_ipaddrgroup *mdipag = NULL;
    mdipag = mido_get_ipaddrgroup("metadata_ip");
    if (mdipag) {
        LOGTRACE("Found metadata ip-address-group %s\n", mdipag->obj->name);
        midocore->metadata_iag = mdipag;
        midocore->midos[CORE_METADATA_IPADDRGROUP] = mdipag->obj;
    } else {
        LOGINFO("\t\tmetadata ip-address-group not found\n");
    }

    midonet_api_chain *eucabr_infilter = NULL;
    eucabr_infilter = mido_get_chain("eucabr_infilter");
    if (eucabr_infilter) {
        LOGTRACE("Found eucabr chain %s\n", eucabr_infilter->obj->name);
        midocore->eucabr_infilter = eucabr_infilter;
        midocore->midos[CORE_EUCABR_INFILTER] = eucabr_infilter->obj;
    } else {
        LOGINFO("\t\teucabr infilter not found\n");
    }
    
    // search all ports for RT/BR ports
    found = 0;
    for (i = 0; i < max_brports && !found; i++) {
        if (brports[i] == NULL) {
            continue;
        }
        for (j = 0; j < max_rtports && !found; j++) {
            if (rtports[j] == NULL) {
                continue;
            }
            if (rtports[j]->port && rtports[j]->port->peerid && brports[i]->uuid) {
                if (!strcmp(rtports[j]->port->peerid, brports[i]->uuid)) {
                    LOGTRACE("Found eucart-eucabr link.\n");
                    midocore->midos[CORE_EUCABR_RTPORT] = brports[i];
                    midocore->midos[CORE_EUCART_BRPORT] = rtports[j];
                    found = 1;
                }
            }
        }
    }
    if (!found) {
        LOGINFO("\t\teucart-eucabr link not found.\n");
    }

    // populate MN5 bgp-peers and bgp-networks
    if (midocore->eucart && is_midonet_api_v5()) {
        midoname **bgps = NULL;
        int max_bgps = 0;
        rc = mido_get_bgps(midocore->eucart->obj, &bgps, &max_bgps);
        if (rc) {
            LOGWARN("unable to retrieve eucart bgp-peers\n");
            EUCA_FREE(bgps);
        } else {
            midocore->bgp_peers = bgps;
            midocore->max_bgp_peers = max_bgps;
        }

        midoname **bgp_routes = NULL;
        int max_bgp_routes = 0;
        rc = mido_get_bgp_routes(midocore->eucart->obj, &bgp_routes, &max_bgp_routes);
        if (rc) {
            LOGWARN("unable to retrieve eucart bgp-networks\n");
        } else {
            for (int i = 0; i < max_bgp_routes; i++) {
                char *nwPrefix = NULL;
                char *prefixLength = NULL;
                mido_getel_midoname(bgp_routes[i], "subnetAddress", &nwPrefix);
                mido_getel_midoname(bgp_routes[i], "subnetLength", &prefixLength);
                if (!nwPrefix || !prefixLength) {
                    LOGWARN("failed to retrieve bgp ad route\n");
                    continue;
                } else {
                    mido_gw_ad_route *adr = EUCA_ZALLOC_C(1, sizeof (mido_gw_ad_route));
                    midocore->bgp_networks = EUCA_APPEND_PTRARR(midocore->bgp_networks,
                            &(midocore->max_bgp_networks), adr);
                    adr->route = bgp_routes[i];
                    snprintf(adr->cidr, NETWORK_ADDR_LEN, "%s/%s", nwPrefix, prefixLength);
                }
                EUCA_FREE(nwPrefix);
                EUCA_FREE(prefixLength);
            }
        }
        EUCA_FREE(bgp_routes);
    }

    for (i = 0; i < max_rtports; i++) {
        // look for ports bound to a host
        if (rtports[i]->port->hostid && strlen(rtports[i]->port->hostid)) {
            midonet_api_host *host = mido_get_host(NULL, rtports[i]->port->hostid);
            if (host) {
                mido_gw *gw = EUCA_ZALLOC_C(1, sizeof (mido_gw));
                midocore->gws = EUCA_APPEND_PTRARR(midocore->gws, &(midocore->max_gws), gw);
                gw->port = rtports[i];
                gw->host = host;
                populate_mido_gw(mido, rtports[i], gw);
                print_mido_gw(gw, EUCA_LOG_DEBUG);
            }
        }
    }

    midonet_api_portgroup *eucapg = NULL;
    eucapg = mido_get_portgroup(VPCMIDO_CORERTPG);
    if (eucapg) {
        LOGTRACE("Found gw portgroup %s\n", eucapg->obj->name);
        midocore->midos[CORE_GWPORTGROUP] = eucapg->obj;
    } else {
        LOGINFO("\t\tGateway port-group not found\n");
    }

    midonet_api_ipaddrgroup *veripag = NULL;
    veripag = mido_get_ipaddrgroup(VPCMIDO_EUCAVER);
    if (veripag) {
        LOGTRACE("Found euca_version ip-address-group %s\n", veripag->obj->name);
        midocore->eucaver_iag = veripag;
        midocore->midos[CORE_EUCAVER_IPADDRGROUP] = veripag->obj;
        if ((veripag->max_ips == 1) && (veripag->ips[0] && veripag->ips[0]->ipagip))  {
            mido->euca_version = euca_version_dot2hex(veripag->ips[0]->ipagip->ip);
        } else {
            mido_delete_ipaddrgroup(midocore->midos[CORE_EUCAVER_IPADDRGROUP]);
            midocore->midos[CORE_EUCAVER_IPADDRGROUP] = NULL;
            mido->euca_version = 0;
        }
    }

    LOGTRACE("midocore: AFTER POPULATE\n");
    for (i = 0; i < CORE_END; i++) {
        if (midocore->midos[i] == NULL) {
            midocore->population_failed = 1;
        }
        LOGTRACE("\tmidos[%d]: %d\n", i, (midocore->midos[i] == NULL) ? 0 : midocore->midos[i]->init);
    }

    return (ret);
}

/**
 * Populates euca VPC MD models from mido models.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @return 0 on success. 1 on any failure.
 */
int populate_mido_md(mido_config *mido) {
    int ret = 0, i = 0, j = 0;
    int found = 0;
    midoname **brports = NULL;
    int max_brports = 0;
    midoname **rtports = NULL;
    int max_rtports = 0;

    if (!mido || !mido->midocore || !mido->midomd) {
        LOGWARN("Invalid argument: cannot populate midomd from NULL config\n");
        return (1);
    }
    
    mido_md *midomd = mido->midomd;

    midonet_api_router *eucamdrt = NULL;
    eucamdrt = mido_get_router(VPCMIDO_MDRT);
    if (eucamdrt) {
        LOGTRACE("Found md router %s\n", eucamdrt->obj->name);
        midomd->eucamdrt = eucamdrt;
        midomd->midos[MD_RT] = eucamdrt->obj;
        rtports = eucamdrt->ports;
        max_rtports = eucamdrt->max_ports;
    } else {
        LOGINFO("\t\tmd router not found\n");
    }

    midonet_api_bridge *eucamdbr = NULL;
    eucamdbr = mido_get_bridge(VPCMIDO_MDBR);
    if (eucamdbr) {
        LOGTRACE("Found md bridge %s\n", eucamdbr->obj->name);
        midomd->eucamdbr = eucamdbr;
        midomd->midos[MD_BR] = eucamdbr->obj;
        brports = eucamdbr->ports;
        max_brports = eucamdbr->max_ports;
    } else {
        LOGINFO("\t\tmd bridge not found\n");
    }

    // search all ports for RT/BR ports
    found = 0;
    for (i = 0; i < max_brports && !found; i++) {
        if (brports[i] == NULL) {
            continue;
        }
        for (j = 0; j < max_rtports && !found; j++) {
            if (rtports[j] == NULL) {
                continue;
            }
            if (rtports[j]->port && rtports[j]->port->peerid && brports[i]->uuid) {
                if (!strcmp(rtports[j]->port->peerid, brports[i]->uuid)) {
                    LOGTRACE("Found eucamdrt-eucamdbr link.\n");
                    midomd->midos[MD_BR_RTPORT] = brports[i];
                    midomd->midos[MD_RT_BRPORT] = rtports[j];
                    found = 1;
                }
            }
        }
    }
    if (!found) {
        LOGINFO("\t\teucamdrt-eucamdbr link not found.\n");
    }

    // search for md ext port
    found = 0;
    for (j = 0; j < max_rtports && !found; j++) {
        if (rtports[j] == NULL) {
            continue;
        }
        if (rtports[j]->port && rtports[j]->port->portaddr) {
            if (dot2hex(rtports[j]->port->portaddr) == mido->mdconfig.ext_mdnw + 1) {
                LOGTRACE("Found md ext port.\n");
                if (!rtports[j]->port->ifname) {
                    midomd->population_failed = 1;
                } else {
                    midomd->midos[MD_RT_EXTPORT] = rtports[j];
                    found = 1;
                }
            }
        }
    }

    // search for md ext port outfilter
    midonet_api_chain *eucamdrt_extport_outfilter = NULL;
    eucamdrt_extport_outfilter = mido_get_chain("eucamdrt_extportout");
    if (eucamdrt_extport_outfilter) {
        LOGTRACE("Found eucamdrt chain %s\n", eucamdrt_extport_outfilter->obj->name);
        midomd->eucamdrt_extport_outfilter = eucamdrt_extport_outfilter;
        midomd->midos[MD_RT_EXTPORT_OUTFILTER] = eucamdrt_extport_outfilter->obj;
    } else {
        LOGINFO("\t\teucamdrt extport outfilter not found\n");
    }
    
    LOGTRACE("midomd: AFTER POPULATE\n");
    for (i = 0; i < MD_END; i++) {
        if (midomd->midos[i] == NULL) {
            midomd->population_failed = 1;
        }
        LOGTRACE("\tmidos[%d]: %d\n", i, (midomd->midos[i] == NULL) ? 0 : midomd->midos[i]->init);
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

    // Remove arp_table and mac_table entries
    if (vpcinstance->midos[INST_IP4MAC]) {
        rc = mido_delete_ip4mac(subnet->subnetbr, vpcinstance->midos[INST_IP4MAC]);
        ret += rc;
    }
    if (vpcinstance->midos[INST_MACPORT]) {
        rc = mido_delete_macport(subnet->subnetbr, vpcinstance->midos[INST_MACPORT]);
        ret += rc;
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
    
    cidr_split(vpc->gniVpc->cidr, vpc_nw, vpc_nm, NULL, NULL, NULL);
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
    // Condition using nwDstAddress instead of ipAddrGroupDst due to bug in MN5
    // TODO: revert to use ipAddrGroupDst once MN5 bug is fixed (in order to avoid possible inconsistencies)
    rc = mido_create_rule(vpc->rt_preelipchain, vpc->midos[VPC_VPCRT_PREELIPCHAIN], &(vpcinstance->midos[INST_ELIP_PRE]),
            NULL, "type", "dnat", "flowAction", "continue", "nwDstAddress",
            ipAddr_pub, "nwDstLength", "32", "natTargets", "jsonlist", "natTargets:addressTo",
            ipAddr_priv, "natTargets:addressFrom", ipAddr_priv, "natTargets:portFrom", "0",
            "natTargets:portTo", "0", "natTargets:END", "END", NULL);
/*
    rc = mido_create_rule(vpc->rt_preelipchain, vpc->midos[VPC_VPCRT_PREELIPCHAIN], &(vpcinstance->midos[INST_ELIP_PRE]),
            NULL, "type", "dnat", "flowAction", "continue", "ipAddrGroupDst",
            vpcinstance->midos[INST_ELIP_PRE_IPADDRGROUP]->uuid, "natTargets", "jsonlist", "natTargets:addressTo",
            ipAddr_priv, "natTargets:addressFrom", ipAddr_priv, "natTargets:portFrom", "0",
            "natTargets:portTo", "0", "natTargets:END", "END", NULL);
*/
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
 * Remove MidoNet objects created to implement MD network path of vpcinstance.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param vpc [in] vpc of the interface of interest.
 * @param vpcinstance [in] interface of interest.
 * @return 0 on success. 1 otherwise.
 */
int disconnect_mido_vpc_instance_md(mido_config *mido, mido_vpc *vpc, mido_vpc_instance *vpcinstance) {
    int ret = 0, rc = 0;

    if (!mido || !vpc || !vpcinstance) {
        LOGERROR("Invalid argument: cannot disconnect md from NULL\n");
        return (1);
    }
    rc = mido_delete_route(mido->midomd->eucamdrt, vpcinstance->midos[INST_MD_ROUTE]);
    vpcinstance->midos[INST_MD_ROUTE] = NULL;
    if (rc) {
        LOGWARN("Failed to delete MD_ROUTE for %s\n", vpcinstance->name);
        ret = 1;
    }

    rc = mido_delete_rule(vpc->rt_mduplink_infilter, vpcinstance->midos[INST_MD_DNAT]);
    vpcinstance->midos[INST_MD_DNAT] = NULL;
    if (rc) {
        LOGWARN("Failed to delete MD_DNAT for %s\n", vpcinstance->name);
        ret = 1;
    }

    rc = mido_delete_rule(vpc->rt_mduplink_outfilter, vpcinstance->midos[INST_MD_SNAT]);
    vpcinstance->midos[INST_MD_SNAT] = NULL;
    if (rc) {
        LOGWARN("Failed to delete MD_SNAT for %s\n", vpcinstance->name);
        ret = 1;
    }

    if (vpcinstance->eniid) {
        clear_eni_id(mido, vpcinstance->eniid);
        vpcinstance->eniid = 0;
    }

    return (ret);
}

/**
 * Create MidoNet objects to route metadata traffic to/from vpcinstance interface.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param vpc [in] vpc of the interface of interest.
 * @param vpcinstance [in] interface of interest.
 * @return 0 on success. 1 otherwise.
 */
int connect_mido_vpc_instance_md(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, mido_vpc_instance *vpcinstance) {
    int rc = 0, ret = 0;
    char *ipAddr_priv = NULL, *ipAddr_md = NULL, *tmpstr = NULL;
    char *ext_mdnw = NULL;
    char ext_mdsn[NETWORK_ADDR_LEN];
    char vpc_rtip[NETWORK_ADDR_LEN];
    mido_md *midomd = NULL;
    
    if (!mido || !vpc || !vpcsubnet || !vpcinstance) {
        LOGWARN("Invalid argument: cannot process ENI md path for NULL\n");
        return (1);
    }

    if (!vpcinstance->gniInst->privateIp) {
        LOGWARN("input ip is 0.0.0.0: - will not connect_mido_vpc_instance_md\n");
        return (0);
    }

    midomd = mido->midomd;
    
    tmpstr = hex2dot(mido->mdconfig.int_mdnw + vpc->rtid);
    snprintf(vpc_rtip, NETWORK_ADDR_LEN, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    ipAddr_priv = hex2dot(vpcinstance->gniInst->privateIp);
    ipAddr_md = hex2dot(mido->mdconfig.mdnw + vpcinstance->eniid);
    
    ext_mdnw = hex2dot(mido->mdconfig.ext_mdnw);
    snprintf(ext_mdsn, NETWORK_ADDR_LEN, "%d", mido->mdconfig.ext_mdsn);

    // DNAT
    rc = mido_create_rule(vpc->rt_mduplink_infilter, NULL, &(vpcinstance->midos[INST_MD_DNAT]),
            NULL, "type", "dnat", "flowAction", "continue", "nwDstAddress",
            ipAddr_md, "nwDstLength", "32", "natTargets", "jsonlist", "natTargets:addressTo",
            ipAddr_priv, "natTargets:addressFrom", ipAddr_priv, "natTargets:portFrom", "0",
            "natTargets:portTo", "0", "natTargets:END", "END", NULL);
    if (rc) {
        LOGERROR("cannot create md dnat rule: check midonet health\n");
        ret++;
    }

    // SNAT
    rc = mido_create_rule(vpc->rt_mduplink_outfilter, NULL, &(vpcinstance->midos[INST_MD_SNAT]),
            NULL, "type", "snat", "nwDstAddress", ext_mdnw,
            "nwDstLength", ext_mdsn, "flowAction", "continue", "nwSrcAddress",
            ipAddr_priv, "nwSrcLength", "32", "natTargets", "jsonlist",
            "natTargets:addressTo", ipAddr_md, "natTargets:addressFrom", ipAddr_md,
            "natTargets:portFrom", "0", "natTargets:portTo", "0", "natTargets:END", "END", NULL);
    if (rc) {
        LOGERROR("cannot create md snat rule: check midonet health\n");
        ret++;
    }

    // MD ip route in main MD router (eucamdrt)
    rc = mido_create_route(midomd->eucamdrt, NULL, midomd->midos[MD_RT_BRPORT],
            "0.0.0.0", "0", ipAddr_md, "32", vpc_rtip, "100", &(vpcinstance->midos[INST_MD_ROUTE]));
    if (rc) {
        LOGERROR("failed to setup md IP route on midomdrt: check midonet health\n");
        ret++;
    }

    EUCA_FREE(ipAddr_priv);
    EUCA_FREE(ipAddr_md);
    EUCA_FREE(ext_mdnw);
    return (ret);
}

/**
 * Connects an instance/interface to mido host - link of the interface to the
 * VPC bridge port is created; the corresponding bridge port is created;
 * the corresponding VPC bridge dhcp entry is created; and the corresponding
 * arp_table and mac_table entries are created.
 *
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param subnet [in] the subnet where the interface of interst is linked.
 * @param vpcinstance [in] the interface of interest
 * @param instanceDNSDomain [in] DNS domain to be used in the DHCP entry
 *
 * @return 0 on success. non-zero number otherwise.
 */
int connect_mido_vpc_instance(mido_config *mido, mido_vpc_subnet *vpcsubnet, mido_vpc_instance *vpcinstance, char *instanceDNSDomain) {
    int ret = 0, rc = 0;
    char *macAddr = NULL, *ipAddr = NULL;
    char ifacename[IF_NAME_LEN];

    char *shortid = euca_truncate_interfaceid(vpcinstance->gniInst->name);
    if (shortid) {
        snprintf(ifacename, IF_NAME_LEN, "vn_%s", shortid);
    } else {
        snprintf(ifacename, IF_NAME_LEN, "vn_%s", vpcinstance->gniInst->name);
    }
    EUCA_FREE(shortid);

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

    LOGTRACE("\tadding host %s/%s to dhcp server\n", SP(macAddr), SP(ipAddr));
    rc = mido_create_dhcphost(vpcsubnet->subnetbr, vpcsubnet->midos[SUBN_BR_DHCP],
            vpcinstance->gniInst->name, macAddr, ipAddr, instanceDNSDomain, &(vpcinstance->midos[INST_VPCBR_DHCPHOST]));
    if (rc) {
        LOGERROR("failed to create midonet dhcp host entry: check midonet health\n");
        ret = 1;
    }
    
    // setup arp_table and mac_table entries
    if (mido->config->enable_mido_arptable) {
        if (vpcinstance->midos[INST_VPCBR_VMPORT] && vpcinstance->midos[INST_VPCBR_VMPORT]->uuid &&
                strlen(ipAddr) && strlen(macAddr)) {
            LOGEXTREME("setting arp_entry for %s\n", vpcinstance->name);
            rc = mido_create_ip4mac(vpcsubnet->subnetbr, NULL, ipAddr, macAddr, &(vpcinstance->midos[INST_IP4MAC]));
            if (rc) {
                LOGWARN("failed to create arp_table entry for %s\n", vpcinstance->name);
            }
            LOGEXTREME("setting mac_entry for %s\n", vpcinstance->name);
            rc = mido_create_macport(vpcsubnet->subnetbr, NULL, macAddr, vpcinstance->midos[INST_VPCBR_VMPORT]->uuid, &(vpcinstance->midos[INST_MACPORT]));
            if (rc) {
                LOGWARN("failed to create mac_table entry for %s\n", vpcinstance->name);
            }
        }
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

/**
 * Clear data from the given mido_config data structure and release non-core resources
 * @param mido [in] mido_config data structure of interest
 * @return always 0.
 */
int clear_mido_config(mido_config *mido) {
    return (free_mido_config_v(mido, VPCMIDO_CONFIG_CLEAR));
}

/**
 * Clear data from the given mido_config data structure and release allocated memory
 * @param mido [in] mido_config data structure of interest
 * @return always 0.
 */
int free_mido_config(mido_config *mido) {
    return (free_mido_config_v(mido, VPCMIDO_CONFIG_FREE));
}


/**
 * Clear data from the given mido_config data structure and release allocated memory
 * @param mido [in] mido_config data structure of interest
 * @param mode [in] set to VPCMIDO_CONFIG_CLEAR to clear and release non-core parameters.
 * Set to VPCMIDO_CONFIG_FREE to fully clear mido_config
 * @return always 0.
 */
int free_mido_config_v(mido_config *mido, vpcmido_config_op mode) {
    int ret = 0, i = 0;

    if (!mido) {
        return (0);
    }

    for (i = 0; i < mido->max_gni_gws; i++) {
        mido->gni_gws[i].mido_present = NULL;
    }
    if (mode == VPCMIDO_CONFIG_FREE) {
        EUCA_FREE(mido->eucahome);

        for (i = 0; i < mido->max_gni_gws; i++) {
            gni_midogw_clear(&(mido->gni_gws[i]));
        }
        EUCA_FREE(mido->gni_gws);
    }

    free_mido_core(mido->midocore);
    if (mode == VPCMIDO_CONFIG_FREE) {
        EUCA_FREE(mido->midocore);
    }

    free_mido_md(mido->midomd);
    if (mode == VPCMIDO_CONFIG_FREE) {
        EUCA_FREE(mido->midomd);
    }
    
    if (mode == VPCMIDO_CONFIG_FREE) {
        EUCA_FREE(mido->rt_ids);
        EUCA_FREE(mido->eni_ids);
    }

    for (i = 0; i < mido->max_vpcs; i++) {
        free_mido_vpc(&(mido->vpcs[i]));
    }
    EUCA_FREE(mido->vpcs);
    mido->max_vpcs = 0;

    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        free_mido_vpc_secgroup(&(mido->vpcsecgroups[i]));
    }
    EUCA_FREE(mido->vpcsecgroups);
    mido->max_vpcsecgroups = 0;

    if (mode == VPCMIDO_CONFIG_FREE) {
        boolean midotz_ok_bak = mido->midotz_ok;
        memset(mido, 0, sizeof (mido_config));
        mido->midotz_ok = midotz_ok_bak;
    }

    return (ret);
}

/**
 * Release resources allocated for the given mido_core data structure.
 * @param midocore [in] mido_core data structure of interest.
 * @return always 0.
 */
int free_mido_core(mido_core *midocore) {
    int ret = 0;

    if (!midocore)
        return (0);

    for (int i = 0; i < midocore->max_gws; i++) {
        if (midocore->gws[i]) {
            free_mido_gw(midocore->gws[i]);
        }
    }
    EUCA_FREE(midocore->gws);
    EUCA_FREE(midocore->bgp_peers);
    for (int i = 0; i < midocore->max_bgp_networks; i++) {
        EUCA_FREE(midocore->bgp_networks[i]);
    }
    EUCA_FREE(midocore->bgp_networks);
    memset(midocore, 0, sizeof (mido_core));

    return (ret);
}

/**
 * Release resources allocated for the midomd data structure.
 * @param midomd [in] mido_md data structure of interest.
 * @return always 0.
 */
int free_mido_md(mido_md *midomd) {
    int ret = 0;

    if (!midomd)
        return (0);

    memset(midomd, 0, sizeof (mido_md));

    return (ret);
}

/**
 * Release resources allocated for mido_gw gw data structure.
 * @param gw [in] mido_gw data structure of interest.
 * @return always 0.
 */
int free_mido_gw(mido_gw *gw) {
    for (int i = 0; i < gw->max_ad_routes; i++) {
        if (gw->ad_routes[i]) {
            EUCA_FREE(gw->ad_routes[i]);
        }
    }
    EUCA_FREE(gw->ad_routes);
    EUCA_FREE(gw);
    return (0);
}

/**
 * Release resources allocated for the given mido_vpc data structure.
 * @param vpc [in] mido_vpc data structure of interest.
 * @return always 0.
 */
int free_mido_vpc(mido_vpc *vpc) {
    int ret = 0, i = 0;

    if (!vpc)
        return (0);

    for (i = 0; i < vpc->max_subnets; i++) {
        free_mido_vpc_subnet(&(vpc->subnets[i]));
    }
    EUCA_FREE(vpc->subnets);
    
    for (i = 0; i < vpc->max_nacls; i++) {
        free_mido_vpc_nacl(&(vpc->nacls[i]));
    }
    EUCA_FREE(vpc->nacls);
    
    memset(vpc, 0, sizeof (mido_vpc));

    return (ret);
}

/**
 * Release resources allocated for the given mido_vpc_subnet data structure.
 * @param vpcsubnet [in] mido_vpc_subnet data structure of interest.
 * @return always 0.
 */
int free_mido_vpc_subnet(mido_vpc_subnet *vpcsubnet) {
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

    memset(vpcsubnet, 0, sizeof (mido_vpc_subnet));

    return (ret);
}

/**
 * Release resources allocated for the given mido_vpc_instance data structure.
 * @param vpcinstance [in] mido_vpc_instance data structure of interest.
 * @return always 0.
 */
int free_mido_vpc_instance(mido_vpc_instance *vpcinstance) {
    int ret = 0;

    if (!vpcinstance)
        return (0);

    memset(vpcinstance, 0, sizeof (mido_vpc_instance));

    return (ret);
}

/**
 * Release resources allocated for the given mido_vpc_natgateway data structure.
 * @param vpcnatgateway [in] mido_vpc_natgateway data structure of interest.
 * @return always 0.
 */
int free_mido_vpc_natgateway(mido_vpc_natgateway *vpcnatgateway) {
    int ret = 0;

    if (!vpcnatgateway) {
        return (0);        
    }

    //mido_free_midoname_list(vpcnatgateway->midos, NATG_END);
    memset(vpcnatgateway, 0, sizeof (mido_vpc_natgateway));
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

    LOGTRACE("DELETING SUBNET '%s'\n", vpcsubnet->name);

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
    rc += mido_delete_chain(vpcsubnet->midos[SUBN_BR_INFILTER]);
    rc += mido_delete_chain(vpcsubnet->midos[SUBN_BR_OUTFILTER]);
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

    LOGTRACE("DELETING VPC: %s, %s\n", vpc->name, vpc->midos[VPC_VPCRT]->name);

    for (i = 0; i < vpc->max_subnets; i++) {
        if (strlen(vpc->subnets[i].name)) {
            rc += delete_mido_vpc_subnet(mido, vpc, &(vpc->subnets[i]));
        }
    }
    
    for (i = 0; i < vpc->max_nacls; i++) {
        if (strlen(vpc->nacls[i].name)) {
            rc += delete_mido_vpc_nacl(mido, &(vpc->nacls[i]));
        }
    }

    rc += mido_delete_bridge_port(mido->midocore->eucabr, vpc->midos[VPC_EUCABR_DOWNLINK]);
    rc += mido_delete_router(vpc->midos[VPC_VPCRT]);

    rc += mido_delete_chain(vpc->midos[VPC_VPCRT_PREELIPCHAIN]);
    rc += mido_delete_chain(vpc->midos[VPC_VPCRT_UPLINK_PRECHAIN]);
    rc += mido_delete_chain(vpc->midos[VPC_VPCRT_UPLINK_POSTCHAIN]);

    if (mido->config->enable_mido_md || mido->config->populate_mido_md) {
        rc += mido_delete_bridge_port(mido->midomd->eucamdbr, vpc->midos[VPC_EUCAMDBR_DOWNLINK]);
        rc += mido_delete_chain(vpc->midos[VPC_VPCRT_MDUPLINK_INFILTER]);
        rc += mido_delete_chain(vpc->midos[VPC_VPCRT_MDUPLINK_OUTFILTER]);
    }

    rc += delete_mido_meta_vpc_namespace(mido, vpc->name);

    clear_router_id(mido, vpc->rtid);
    free_mido_vpc(vpc);

    return (ret);
}

/**
 * Creates mido objects to implement a VPC subnet.
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
        u32 *instanceDNSServers, int max_instanceDNSServers) {
    int rc = 0, ret = 0;
    char name_buf[MIDO_NAME_LEN], *tapiface = NULL;

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
    snprintf(name_buf, MIDO_NAME_LEN, "vb_%s_%s", vpc->name, vpcsubnet->name);
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

    // Create bridge infilter
    snprintf(name_buf, MIDO_NAME_LEN, "sc_%s_in", vpcsubnet->name);
    midonet_api_chain *ch = mido_create_chain(VPCMIDO_TENANT, name_buf, &(vpcsubnet->midos[SUBN_BR_INFILTER]));
    if (ch == NULL) {
        LOGWARN("Failed to create chain %s.\n", name_buf);
        return (1);
    }
    vpcsubnet->inchain = ch;
    // Create bridge outfilter
    snprintf(name_buf, MIDO_NAME_LEN, "sc_%s_out", vpcsubnet->name);
    ch = NULL;
    ch = mido_create_chain(VPCMIDO_TENANT, name_buf, &(vpcsubnet->midos[SUBN_BR_OUTFILTER]));
    if (ch == NULL) {
        LOGWARN("Failed to create chain %s.\n", name_buf);
        return (1);
    }
    vpcsubnet->outchain = ch;
    // Apply chains to bridge
    rc = mido_update_bridge(vpcsubnet->subnetbr->obj, "inboundFilterId", vpcsubnet->inchain->obj->uuid,
            "outboundFilterId", vpcsubnet->outchain->obj->uuid,
            "name", vpcsubnet->subnetbr->obj->name, NULL);
    if (rc > 0) {
        LOGERROR("failed to attach infilter and/or outfilter to %s\n", vpcsubnet->name);
    }

    // setup DHCP on the bridge for this subnet
    rc = mido_create_dhcp(vpcsubnet->subnetbr, vpcsubnet->midos[SUBN_BR], subnet, slashnet, gw,
            instanceDNSServers, max_instanceDNSServers, &(vpcsubnet->midos[SUBN_BR_DHCP]));
    if (rc) {
        LOGERROR("cannot create midonet dhcp server: check midonet health\n");
        return (1);
    }

    // setup dot2 address arp-proxy
    if (vpcsubnet->midos[SUBN_VPCRT_BRPORT] && vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port &&
            vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port->portmac) {
        u32 dot2address = dot2hex(subnet);
        dot2address += 2;
        LOGEXTREME("creating dot2 arp_table entry for %s\n", vpcsubnet->name);
        rc = mido_create_ip4mac(vpcsubnet->subnetbr, NULL, hex2dot_s(dot2address),
                vpcsubnet->midos[SUBN_VPCRT_BRPORT]->port->portmac, &(vpcsubnet->midos[SUBN_BR_DOT2ARP]));
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
    char name_buf[MIDO_NAME_LEN];
    char nw[INET_ADDR_LEN], sn[INET_ADDR_LEN], ip[INET_ADDR_LEN], gw[INET_ADDR_LEN];
    char mdip[INET_ADDR_LEN], mdnw[INET_ADDR_LEN], mdsn[INET_ADDR_LEN], mdgw[INET_ADDR_LEN];
    char mdhttp[INET_ADDR_LEN], mddns[INET_ADDR_LEN];
    char *tmpstr = NULL;

    tmpstr = hex2dot(mido->int_rtnw);
    snprintf(nw, INET_ADDR_LEN, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    snprintf(sn, INET_ADDR_LEN, "%d", mido->int_rtsn);

    tmpstr = hex2dot(mido->int_rtnw + vpc->rtid);
    snprintf(ip, INET_ADDR_LEN, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    tmpstr = hex2dot(mido->int_rtaddr);
    snprintf(gw, INET_ADDR_LEN, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    // Create the VPC mido router
    snprintf(name_buf, MIDO_NAME_LEN, "vr_%s_%d", vpc->name, vpc->rtid);
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
    snprintf(name_buf, MIDO_NAME_LEN, "vc_%s_prechain", vpc->name);
    ch = mido_create_chain(VPCMIDO_TENANT, name_buf, &(vpc->midos[VPC_VPCRT_UPLINK_PRECHAIN]));
    if (!ch) {
        LOGERROR("cannot create midonet pre chain: check midonet health\n");
        return (1);
    } else {
        vpc->rt_uplink_prechain = ch;
    }

    snprintf(name_buf, MIDO_NAME_LEN, "vc_%s_preelip", vpc->name);
    ch = mido_create_chain(VPCMIDO_TENANT, name_buf, &(vpc->midos[VPC_VPCRT_PREELIPCHAIN]));
    if (!ch) {
        LOGERROR("cannot create midonet preelip chain: check midonet health\n");
        return (1);
    } else {
        vpc->rt_preelipchain = ch;
    }

    snprintf(name_buf, MIDO_NAME_LEN, "vc_%s_postchain", vpc->name);
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
    char *portmac = NULL;
    if (vpc->midos[VPC_VPCRT_UPLINK] && vpc->midos[VPC_VPCRT_UPLINK]->port && vpc->midos[VPC_VPCRT_UPLINK]->port->portmac) {
        portmac = vpc->midos[VPC_VPCRT_UPLINK]->port->portmac;
    }
    rc = mido_update_port(vpc->midos[VPC_VPCRT_UPLINK], "outboundFilterId", vpc->midos[VPC_VPCRT_UPLINK_POSTCHAIN]->uuid,
            "inboundFilterId", vpc->midos[VPC_VPCRT_UPLINK_PRECHAIN]->uuid, "id", vpc->midos[VPC_VPCRT_UPLINK]->uuid,
            "networkAddress", nw, "networkLength", sn, "portAddress", ip, "portMac", portmac, "type", "Router", NULL);
    if (rc > 0) {
        LOGERROR("cannot update router port infilter and/or outfilter: check midonet health\n");
        return (1);
    }

    if (mido->config->enable_mido_md) {
        tmpstr = hex2dot(mido->mdconfig.int_mdnw);
        snprintf(mdnw, INET_ADDR_LEN, "%s", tmpstr);
        EUCA_FREE(tmpstr);

        snprintf(mdsn, INET_ADDR_LEN, "%d", mido->mdconfig.int_mdsn);

        tmpstr = hex2dot(mido->mdconfig.int_mdnw + vpc->rtid);
        snprintf(mdip, INET_ADDR_LEN, "%s", tmpstr);
        EUCA_FREE(tmpstr);

        tmpstr = hex2dot(mido->mdconfig.int_mdnw + 1);
        snprintf(mdgw, INET_ADDR_LEN, "%s", tmpstr);
        EUCA_FREE(tmpstr);

        tmpstr = hex2dot(mido->mdconfig.md_http);
        snprintf(mdhttp, INET_ADDR_LEN, "%s", tmpstr);
        EUCA_FREE(tmpstr);

        tmpstr = hex2dot(mido->mdconfig.md_dns);
        snprintf(mddns, INET_ADDR_LEN, "%s", tmpstr);
        EUCA_FREE(tmpstr);

        mido_md *midomd = mido->midomd;

        // Create an eucamdbr port where this VPC router will be linked
        rc = mido_create_bridge_port(midomd->eucamdbr, midomd->midos[MD_BR], &(vpc->midos[VPC_EUCAMDBR_DOWNLINK]));
        if (rc) {
            LOGERROR("cannot create midomd bridge port: check midonet health\n");
            return (1);
        }

        // Create a VPC router port - uplink to eucamdbr
        rc = mido_create_router_port(vpc->vpcrt, vpc->midos[VPC_VPCRT], mdip, mdnw, mdsn, NULL, &(vpc->midos[VPC_VPCRT_MDUPLINK]));
        if (rc) {
            LOGERROR("cannot create midomd router port: check midonet health\n");
            return (1);
        }

        // link the vpc network and euca md network
        rc = mido_link_ports(vpc->midos[VPC_EUCAMDBR_DOWNLINK], vpc->midos[VPC_VPCRT_MDUPLINK]);
        if (rc) {
            LOGERROR("cannot create midomd bridge <-> router link: check midonet health\n");
            return (1);
        }

        // Route md_http and md_dns through uplink
        rc = mido_create_route(vpc->vpcrt, vpc->midos[VPC_VPCRT], vpc->midos[VPC_VPCRT_MDUPLINK],
                "0.0.0.0", "0", mdhttp, "32", mdgw, "0", NULL);
        if (rc) {
            LOGERROR("cannot create route to md_http: check midonet health\n");
            return (1);
        }
        rc = mido_create_route(vpc->vpcrt, vpc->midos[VPC_VPCRT], vpc->midos[VPC_VPCRT_MDUPLINK],
                "0.0.0.0", "0", mddns, "32", mdgw, "0", NULL);
        if (rc) {
            LOGERROR("cannot create route to md_dns: check midonet health\n");
            return (1);
        }

        // Create chains
        midonet_api_chain *ch = NULL;
        snprintf(name_buf, MIDO_NAME_LEN, "vc_%s_mdulin", vpc->name);
        ch = mido_create_chain(VPCMIDO_TENANT, name_buf, &(vpc->midos[VPC_VPCRT_MDUPLINK_INFILTER]));
        if (!ch) {
            LOGERROR("cannot create midomd infilter: check midonet health\n");
            return (1);
        } else {
            vpc->rt_mduplink_infilter = ch;
        }

        snprintf(name_buf, MIDO_NAME_LEN, "vc_%s_mdulout", vpc->name);
        ch = mido_create_chain(VPCMIDO_TENANT, name_buf, &(vpc->midos[VPC_VPCRT_MDUPLINK_OUTFILTER]));
        if (!ch) {
            LOGERROR("cannot create midomd outfilter chain: check midonet health\n");
            return (1);
        } else {
            vpc->rt_mduplink_outfilter = ch;
        }

        // apply chains to VPCRT_MDUPLINK port
        char *portmac = NULL;
        if (vpc->midos[VPC_VPCRT_MDUPLINK] && vpc->midos[VPC_VPCRT_MDUPLINK]->port && vpc->midos[VPC_VPCRT_MDUPLINK]->port->portmac) {
            portmac = vpc->midos[VPC_VPCRT_MDUPLINK]->port->portmac;
        }
        rc = mido_update_port(vpc->midos[VPC_VPCRT_MDUPLINK], "outboundFilterId", vpc->midos[VPC_VPCRT_MDUPLINK_OUTFILTER]->uuid,
                "inboundFilterId", vpc->midos[VPC_VPCRT_MDUPLINK_INFILTER]->uuid, "id", vpc->midos[VPC_VPCRT_MDUPLINK]->uuid,
                "networkAddress", mdnw, "networkLength", mdsn, "portAddress", mdip, "portMac", portmac, "type", "Router", NULL);
        if (rc > 0) {
            LOGERROR("cannot update router md port infilter and/or outfilter: check midonet health\n");
            return (1);
        }
    }

    rc = create_mido_meta_vpc_namespace(mido, vpc);
    if (rc) {
        LOGERROR("cannot create netns for VPC %s: check above log for details\n", vpc->name);
        return (1);
    }

    return (0);
}

/**
 * Check populated mido gateway constructs and tag all gateways both in GNI and MN.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @return 0 on success. 1 otherwise.
 */
int tag_mido_gws(mido_config *mido, mido_core *midocore) {
    if (!mido || !midocore || !midocore->eucart) {
        LOGWARN("Invalid argument: cannot delete gw not in gni with NULL config\n");
        return (1);
    }

    // for each gw in GNI, search for corresponding constructs in MN
    for (int i = 0; i < mido->max_gni_gws; i++) {
        gni_mido_gateway *gni_gw = &(mido->gni_gws[i]);
        midonet_api_host *gni_gwhost = mido_get_host_byname(gni_gw->host);
        for (int j = 0; j < midocore->max_gws; j++) {
            mido_gw *gw = midocore->gws[j];
            if (!gw) {
                continue;
            }
            if (gni_gwhost && (gni_gwhost == gw->host) &&
                    !strcmp(gni_gw->ext_dev, gw->ext_dev)) {
                gw->gni_gw = gni_gw;
                gni_gw->mido_present = (mido_gw *) gw;
                break;
            }
        }
    }
    return (0);
}

/**
 * Check populated mido gateway constructs and delete everything not in GNI.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @return 0 on success. 1 otherwise.
 */
int delete_mido_gws_notingni(mido_config *mido, mido_core *midocore) {
    if (!mido || !midocore || !midocore->eucart) {
        LOGWARN("Invalid argument: cannot delete gw not in gni with NULL config\n");
        return (1);
    }
    
    int rc = 0;
    boolean config_bgp = FALSE;
    for (int i = 0; i < mido->max_gni_gws; i++) {
        gni_mido_gateway *gni_gw = &(mido->gni_gws[i]);
        if (gni_gw->asn && gni_gw->peer_asn) {
            config_bgp = TRUE;
            break;
        } else {
        }
    }

    // delete gw populated from MN not in GNI
    for (int i = 0; i < midocore->max_gws; i++) {
        mido_gw *gw = midocore->gws[i];
        if (!gw) {
            continue;
        }
        boolean do_del_gw = FALSE;
        char *gwname = hex2dot_s(euca_getaddr(gw->host->obj->name, NULL));
        // if host/ext_dev not in GNI, delete from MN
        if (!gw->gni_gw) {
            LOGTRACE("gw %s/%s in gni N - delete\n", gwname, gw->ext_dev);
            do_del_gw = TRUE;
        } else {
            LOGTRACE("gw %s/%s in gni Y\n", gwname, gw->ext_dev);
            // if ext_ip or ext_cidr config changed, delete port
            if (strcmp(gw->ext_ip, gw->gni_gw->ext_ip) || strcmp(gw->ext_cidr, gw->gni_gw->ext_cidr)) {
                LOGTRACE("gw %s/%s changes in ext_ip/ext_cidr detected\n", gwname, gw->ext_dev);
                do_del_gw = TRUE;
            }
            // if peer ip changed, delete port
            if (strcmp(gw->peer_ip, gw->gni_gw->peer_ip)) {
                LOGTRACE("gw %s/%s change in peer detected\n", gwname, gw->ext_dev);
                do_del_gw = TRUE;
            }
            // if bgp parameters changed, delete port
            if (config_bgp && ((gw->asn != gw->gni_gw->asn) ||
                    (gw->peer_asn != gw->gni_gw->peer_asn))) {
                LOGTRACE("gw %s/%s change in bgp detected\n", gwname, gw->ext_dev);
                do_del_gw = TRUE;
            }
            // remove ad-routes not in GNI (MN1.9)
            if (config_bgp && is_midonet_api_v1()) {
                for (int j = 0; j < gw->max_ad_routes; j++) {
                    if (!gw->ad_routes[j]) continue;
                    boolean found = FALSE;
                    for (int k = 0; k < gw->gni_gw->max_ad_routes && !found; k++) {
                        if (!strcmp(gw->ad_routes[j]->cidr, gw->gni_gw->ad_routes[k])) {
                            found = TRUE;
                        }
                    }
                    if (found) {
                        LOGTRACE("ad-route %s in GNI Y\n", gw->ad_routes[j]->cidr);
                    } else {
                        LOGTRACE("ad-route %s in GNI N - delete\n", gw->ad_routes[j]->cidr);
                        rc = mido_delete_resource(NULL, gw->ad_routes[j]->route);
                        if (!rc) {
                            EUCA_FREE(gw->ad_routes[j]);
                        }
                    }
                }
                gw->ad_routes = compact_ptrarr(gw->ad_routes, &(gw->max_ad_routes));
            }
            
        }
        if (do_del_gw) {
            delete_mido_gw(mido, midocore, i);
        }
    }

    // MN5: delete bgp-peer(s) not populated
    if (config_bgp && is_midonet_api_v5()) {
        for (int i = 0; i < midocore->max_bgp_peers; i++) {
            char *address = NULL;
            char *asNumber = NULL;
            midoname *bp = midocore->bgp_peers[i];
            mido_getel_midoname(bp, "address", &address);
            mido_getel_midoname(bp, "asNumber", &asNumber);
            boolean found = FALSE;
            for (int j = 0; j < midocore->max_gws && !found; j++) {
                mido_gw *gw = midocore->gws[j];
                if (!gw) continue;
                if (gw->bgp_peer == bp) {
                    found = TRUE;
                }
            }
            if (!found) {
                LOGTRACE("bgp-peer ip %s asn %s in GNI N - delete\n", SP(address), SP(asNumber));
                rc = mido_delete_resource(NULL, bp);
                if (!rc) {
                    midocore->bgp_peers[i] = NULL;
                }
            } else {
                LOGTRACE("bgp-peer ip %s asn %s in GNI Y\n", SP(address), SP(asNumber));
            }
            EUCA_FREE(address);
            EUCA_FREE(asNumber);
        }
        midocore->bgp_peers = compact_ptrarr(midocore->bgp_peers, &(midocore->max_bgp_peers));
    }
    
    // MN5: delete bgp-network(s) not in GNI
    if (config_bgp && is_midonet_api_v5()) {
        char **cidrs = NULL;
        int max_cidrs = 0;
        for (int i = 0; i < mido->max_gni_gws; i++) {
            gni_mido_gateway *gni_gw = &(mido->gni_gws[i]);
            for (int j = 0; j < gni_gw->max_ad_routes; j++) {
                euca_string_set_insert(&cidrs, &max_cidrs, gni_gw->ad_routes[j]);
            }
        }
        for (int i = 0; i < midocore->max_bgp_networks; i++) {
            mido_gw_ad_route *adr = midocore->bgp_networks[i];
            if (!adr) continue;
            boolean found = FALSE;
            for (int j = 0; j < max_cidrs && !found; j++) {
                if (!strcmp(cidrs[j], adr->cidr)) {
                    found = TRUE;
                }
            }
            if (!found) {
                LOGTRACE("ad-route %s in GNI N - delete\n", adr->cidr);
                rc = mido_delete_resource(NULL, adr->route);
                if (!rc) {
                    midocore->bgp_networks[i] = NULL;
                }
            } else {
                LOGTRACE("ad-route %s in GNI Y\n", adr->cidr);
            }
        }
        midocore->bgp_networks = compact_ptrarr(midocore->bgp_networks, &(midocore->max_bgp_networks));

        for (int i = 0; i < max_cidrs; i++) {
            EUCA_FREE(cidrs[i]);
        }
        EUCA_FREE(cidrs);
    }
    
    return (0);
}

/**
 * Deletes mido gateway constructs from MN.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @param entry [in] entry of the gateway to be deleted
 * @return 0 on success. 1 otherwise.
 */
int delete_mido_gw(mido_config *mido, mido_core *midocore, int entry) {
    int rc = 0;
    int ret = 0;
    if (!mido || !midocore || !midocore->eucart || (entry >= midocore->max_gws) ||
            !midocore->gws[entry] || !midocore->gws[entry]->port) {
        LOGWARN("Invalid argument: cannot delete gateway entry with NULL config\n");
        return (1);
    }

    mido_gw *gw = midocore->gws[entry];

    rc = mido_delete_router_port(midocore->eucart, gw->port);

    for (int i = 0; i < mido->max_gni_gws; i++) {
        gni_mido_gateway *gni_gw = &(mido->gni_gws[i]);
        if (gni_gw->mido_present == midocore->gws[entry]) {
            gni_gw->mido_present = NULL;
        }
    }
    free_mido_gw(midocore->gws[entry]);
    midocore->gws[entry] = NULL;
    ret += rc;

    return (0);
}

/**
 * Removes MidoNet objects that are needed to implement euca VPC core.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @return 0 on success. 1 otherwise.
 */
int delete_mido_core(mido_config *mido, mido_core *midocore) {
    int rc = 0;

    // delete the euca_version ipaddrgroup
    rc += mido_delete_ipaddrgroup(midocore->midos[CORE_EUCAVER_IPADDRGROUP]);
    midocore->midos[CORE_EUCAVER_IPADDRGROUP] = NULL;

    // delete the metadata_ip ipaddrgroup
    rc += mido_delete_ipaddrgroup(midocore->midos[CORE_METADATA_IPADDRGROUP]);
    midocore->midos[CORE_METADATA_IPADDRGROUP] = NULL;

    // delete the port-group
    rc += mido_delete_portgroup(midocore->midos[CORE_GWPORTGROUP]);
    midocore->midos[CORE_GWPORTGROUP] = NULL;

    // delete the bridge
    rc += mido_delete_bridge(midocore->midos[CORE_EUCABR]);
    midocore->midos[CORE_EUCABR] = NULL;
    
    // delete the bridge infilter
    if (midocore->eucabr_infilter && midocore->eucabr_infilter->obj) {
        rc += mido_delete_chain(midocore->eucabr_infilter->obj);
        midocore->eucabr_infilter = NULL;
    } 

    // delete the router
    rc += mido_delete_router(midocore->midos[CORE_EUCART]);
    midocore->midos[CORE_EUCART] = NULL;

    if (!mido->config->enable_mido_md) {
        rc += delete_mido_meta_core(mido);
    }

    return (0);
}

/**
 * Deletes the veth pair that connects euca VPC md core to external network
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @return 0 on success. 1 otherwise.
 */
int disconnect_mido_md_ext_veth(mido_config *mido) {
    int ret = 0, rc = 0;

    if (!mido || !mido->midomd) {
        LOGWARN("Invalid argument: cannot disconnect md with NULL config.\n");
        return (1);
    }

    // delete external MD veth pair
    char cmd[EUCA_MAX_PATH];
    sequence_executor cmds;

    rc = se_init(&cmds, mido->config->cmdprefix, 4, 1);

    snprintf(cmd, EUCA_MAX_PATH, "ip link del %s", VPCMIDO_MD_VETH_M);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    if (mido->config->mido_md_veth_use_netns) {
        snprintf(cmd, EUCA_MAX_PATH, "ip netns del %s", VPCMIDO_MD_NETNS);
        rc = se_add(&cmds, cmd, NULL, ignore_exit);
    }

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not delete md veth pair\n");
        ret = 1;
    }
    se_free(&cmds);

    return (ret);
}

/**
 * Removes MidoNet objects that are needed to implement euca VPC md core.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @return 0 on success. 1 otherwise.
 */
int delete_mido_md(mido_config *mido) {
    int ret = 0, rc = 0;

    if (!mido || !mido->midomd) {
        LOGWARN("Invalid argument: cannot delete md from NULL config.\n");
        return (1);
    }

    do_md_nginx_maintain(mido, VPCMIDO_NGINX_STOP);
    
    mido_md *midomd = mido->midomd;

    // delete the eucanetd host md interface
    if (mido->midocore && mido->midocore->eucanetdhost && midomd->midos[MD_RT_EXTPORT] && midomd->midos[MD_RT_EXTPORT]->init) {
        rc += mido_unlink_host_port(mido->midocore->eucanetdhost->obj, midomd->midos[MD_RT_EXTPORT]);
    }

    // delete the bridge
    rc += mido_delete_bridge(midomd->midos[MD_BR]);
    midomd->midos[MD_BR] = NULL;
    midomd->midos[MD_BR_RTPORT] = NULL;
    
    // delete the router
    rc += mido_delete_router(midomd->midos[MD_RT]);
    midomd->midos[MD_RT] = NULL;
    midomd->midos[MD_RT_BRPORT] = NULL;

    // delete the router extport outfilter
    if (midomd->eucamdrt_extport_outfilter && midomd->eucamdrt_extport_outfilter->obj) {
        rc += mido_delete_chain(midomd->eucamdrt_extport_outfilter->obj);
        midomd->eucamdrt_extport_outfilter = NULL;
    } 

    // delete external MD veth pair
    rc += disconnect_mido_md_ext_veth(mido);
    
    ret = rc;

    return (ret);
}

/**
 * Removes MidoNet objects that are needed to implement euca VPC md.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @return 0 on success. 1 otherwise.
 */
int disable_mido_md(mido_config *mido) {
    if (!mido || !mido->midomd) {
        LOGWARN("Invalid argument: cannot delete md from NULL config.\n");
        return (1);
    }

    mido_md *midomd = mido->midomd;
    
    if (mido->config->enable_mido_md || !mido->config->populate_mido_md) {
        return (0);
    }

    if (!midomd || !midomd->eucamdrt || !midomd->eucamdbr) {
        return (0);
    }

    // Disconnect instances/interfaces from md
    for (int i = 0; i < mido->max_vpcs; i++) {
        mido_vpc *vpc = &(mido->vpcs[i]);
        for (int j = 0; j < vpc->max_subnets; j++) {
            mido_vpc_subnet *subnet = &(vpc->subnets[j]);
            for (int k = 0; k < subnet->max_instances; k++) {
                mido_vpc_instance *vpcif = &(subnet->instances[k]);
                disconnect_mido_vpc_instance_md(mido, vpc, vpcif);
            }
        }
    }

    // Disconnect VPC routers from md
    for (int i = 0; i < mido->max_vpcs; i++) {
        mido_vpc *vpc = &(mido->vpcs[i]);
        mido_delete_bridge_port(mido->midomd->eucamdbr, vpc->midos[VPC_EUCAMDBR_DOWNLINK]);
        mido_delete_chain(vpc->midos[VPC_VPCRT_MDUPLINK_INFILTER]);
        mido_delete_chain(vpc->midos[VPC_VPCRT_MDUPLINK_OUTFILTER]);
    }

    return (delete_mido_md(mido));
}

/**
 * Creates MidoNet objects that are needed to implement euca VPC core.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param midocore [in] data structure that holds euca VPC core resources (eucart, eucabr, gateways)
 * @return 0 on success. -1 if failures are detected during gateway(s) processing.
 * 1 on all other errors.
 */
int create_mido_core(mido_config *mido, mido_core *midocore) {
    int ret = 0, rc = 0, i = 0;
    int gw_failed = 0;
    char nw[32], sn[32], gw[32], *tmpstr = NULL;

    tmpstr = hex2dot(mido->int_rtnw);
    snprintf(nw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    tmpstr = hex2dot(mido->int_rtaddr);
    snprintf(gw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    snprintf(sn, 32, "%d", mido->int_rtsn);

    if (!midocore->population_failed) {
        LOGTRACE("\tmidocore fully implemented\n");
    }
    LOGTRACE("creating mido core\n");

    midonet_api_ipaddrgroup *ig = NULL;
    ig = mido_create_ipaddrgroup(VPCMIDO_TENANT, VPCMIDO_EUCAVER, &(midocore->midos[CORE_EUCAVER_IPADDRGROUP]));
    if (!ig) {
        LOGWARN("Failed to create ip adress group euca_version.\n");
        ret++;
    } else {
        midocore->eucaver_iag = ig;
    }
    rc = mido_create_ipaddrgroup_ip(ig, midocore->midos[CORE_EUCAVER_IPADDRGROUP],
            mido->config->euca_version_str, NULL);
    if (rc) {
        LOGERROR("cannot add entry to euca_version ipaddrgroup.\n");
        ret++;
    }

    midocore->eucart = mido_create_router(VPCMIDO_TENANT, VPCMIDO_CORERT, &(midocore->midos[CORE_EUCART]));
    if (midocore->eucart == NULL) {
        LOGERROR("cannot create router: check midonet health\n");
        ret++;
    }
    // core bridge port and addr/route
    rc = mido_create_router_port(midocore->eucart, midocore->midos[CORE_EUCART], gw, nw, sn,
            NULL, &(midocore->midos[CORE_EUCART_BRPORT]));
    if (rc) {
        LOGERROR("cannot create router port: check midonet health\n");
        ret++;
    }

    rc = mido_create_route(midocore->eucart, midocore->midos[CORE_EUCART], midocore->midos[CORE_EUCART_BRPORT],
            "0.0.0.0", "0", nw, sn, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create router route: check midonet health\n");
        ret++;
    }

    rc = mido_create_portgroup(VPCMIDO_TENANT, VPCMIDO_CORERTPG, &(midocore->midos[CORE_GWPORTGROUP]));
    if (rc) {
        LOGWARN("cannot create portgroup: check midonet health\n");
        ret++;
    }

    // Tag gateway constructs
    tag_mido_gws(mido, midocore);

    // Delete gateway constructs not in GNI
    rc = delete_mido_gws_notingni(mido, midocore);
    if (rc) {
        LOGWARN("failed to delete mido gateway constructs not in GNI.\n");
    }

    // Create gateway(s)
    for (i = 0; i < mido->max_gni_gws; i++) {
        gni_mido_gateway *gni_gw = &(mido->gni_gws[i]);
        mido_gw *gw = (mido_gw *) gni_gw->mido_present;
        if (!gw) {
            gw = EUCA_ZALLOC_C(1, sizeof (mido_gw));
            midocore->gws = EUCA_APPEND_PTRARR(midocore->gws, &(midocore->max_gws), gw);
            gni_gw->mido_present = (mido_gw *) gw;
        }

        rc = create_mido_gw(mido, gw, gni_gw);
        if (rc) {
            LOGERROR("failed to create gateway %s\n", hex2dot_s(euca_getaddr(gni_gw->host, NULL)));
            gw_failed++;
            delete_mido_gw(mido, midocore, midocore->max_gws - 1);
        } else {
            gw->gni_gw = gni_gw;
        }
    }
    if (mido->max_gni_gws && (mido->max_gni_gws == gw_failed)) {
        ret++;
    }

    // Configure BGP
    rc = create_mido_gws_bgp(mido, midocore);
    if (rc) {
        LOGERROR("failed to create gateway bgp\n");
        ret++;
    }

    midocore->eucabr = mido_create_bridge(VPCMIDO_TENANT, VPCMIDO_COREBR, &(midocore->midos[CORE_EUCABR]));
    if (midocore->eucabr == NULL) {
        LOGERROR("cannot create bridge: check midonet health\n");
        ret++;
    }

    rc = mido_create_bridge_port(midocore->eucabr, midocore->midos[CORE_EUCABR], &(midocore->midos[CORE_EUCABR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create bridge port: check midonet health\n");
        ret++;
    }

    rc = mido_link_ports(midocore->midos[CORE_EUCART_BRPORT], midocore->midos[CORE_EUCABR_RTPORT]);
    if (rc) {
        LOGERROR("cannot create router <-> bridge link: check midonet health\n");
        ret++;
    }

    ig = NULL;
    ig = mido_create_ipaddrgroup(VPCMIDO_TENANT, "metadata_ip", &(midocore->midos[CORE_METADATA_IPADDRGROUP]));
    if (!ig) {
        LOGWARN("Failed to create ip adress group metadata_ip.\n");
        ret++;
    } else {
        midocore->metadata_iag = ig;
    }
    rc = mido_create_ipaddrgroup_ip(ig, midocore->midos[CORE_METADATA_IPADDRGROUP],
            "169.254.169.254", NULL);
    if (rc) {
        LOGERROR("cannot add metadata IP to metadata ipaddrgroup.\n");
        ret++;
    }

    midonet_api_chain *eucabr_infilter = NULL;
    eucabr_infilter = mido_create_chain(VPCMIDO_TENANT, "eucabr_infilter", &(midocore->midos[CORE_EUCABR_INFILTER]));
    if (!eucabr_infilter) {
        LOGERROR("cannot create eucabr infilter.\n");
        ret++;
    } else {
        midocore->eucabr_infilter = eucabr_infilter;
        rc = mido_update_bridge(midocore->midos[CORE_EUCABR], "inboundFilterId",
                eucabr_infilter->obj->uuid, "name", midocore->midos[CORE_EUCABR]->name, NULL);
        if ((rc != 0) && (rc != -1)) {
            LOGERROR("cannot attach eucabr infilter\n");
            ret++;
        }
        rc = mido_create_rule(eucabr_infilter, eucabr_infilter->obj, NULL, NULL, "type", "drop",
                "invDlType", "false", "dlType", "2048", "nwDstAddress", nw, "nwDstLength", sn,
                "invNwDst", "false", NULL);
        if (rc != 0) {
            LOGWARN("Failed to eucabr drop rule\n");
            ret++;
        }
    }

    if (!mido->config->enable_mido_md) {
        rc = create_mido_meta_core(mido);
        if (rc) {
            LOGERROR("cannot create metadata tap core bridge/devices: check above log for details\n");
            ret++;
        }
    }
    
    if (!ret && gw_failed) {
        return (-1);
    }
    return (ret);
}

/**
 * Connect euca vpc md to external veth pair.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @return 0 on success. 1 otherwise.
 */
int connect_mido_md_ext_veth(mido_config *mido) {
    int ret = 0;
    int rc = 0;
    char nw[NETWORK_ADDR_LEN];
    char sn[NETWORK_ADDR_LEN];
    char ip1[NETWORK_ADDR_LEN];
    char ip2[NETWORK_ADDR_LEN];
    char ip3[NETWORK_ADDR_LEN];
    char nsprefix[256];
    char *strptr = NULL;

    if (!mido || !mido->midomd) {
        LOGWARN("Invalid argument: cannot connect mido MD with NULL config\n");
        return (1);
    }
    
    // create veth pair and connect eucamdrt
    char cmd[EUCA_MAX_PATH];
    sequence_executor cmds;

    strptr = hex2dot(mido->mdconfig.ext_mdnw);
    snprintf(nw, NETWORK_ADDR_LEN, "%s", strptr);
    EUCA_FREE(strptr);

    strptr = hex2dot(mido->mdconfig.md_veth_mido);
    snprintf(ip1, NETWORK_ADDR_LEN, "%s", strptr);
    EUCA_FREE(strptr);

    strptr = hex2dot(mido->mdconfig.md_veth_host);
    snprintf(ip2, NETWORK_ADDR_LEN, "%s", strptr);
    EUCA_FREE(strptr);

    strptr = hex2dot(mido->mdconfig.md_dns);
    snprintf(ip3, NETWORK_ADDR_LEN, "%s", strptr);
    EUCA_FREE(strptr);

    snprintf(sn, NETWORK_ADDR_LEN, "%d", mido->mdconfig.ext_mdsn);

    rc = se_init(&cmds, mido->config->cmdprefix, 4, 1);

    snprintf(cmd, EUCA_MAX_PATH, "ip link add %s type veth peer name %s", VPCMIDO_MD_VETH_M, VPCMIDO_MD_VETH_H);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    nsprefix[0] = '\0';
    if (mido->config->mido_md_veth_use_netns) {
        snprintf(cmd, EUCA_MAX_PATH, "ip netns add %s", VPCMIDO_MD_NETNS);
        rc = se_add(&cmds, cmd, NULL, ignore_exit);

        snprintf(cmd, EUCA_MAX_PATH, "ip link set %s netns %s", VPCMIDO_MD_VETH_H, VPCMIDO_MD_NETNS);
        rc = se_add(&cmds, cmd, NULL, ignore_exit);

        snprintf(nsprefix, 256, "nsenter --net=/var/run/netns/%s ", VPCMIDO_MD_NETNS);
    }

    snprintf(cmd, EUCA_MAX_PATH, "%s ip addr add %s/%s dev %s", nsprefix, ip2, sn, VPCMIDO_MD_VETH_H);
    se_add(&cmds, cmd, NULL, ignore_exit2);

    snprintf(cmd, EUCA_MAX_PATH, "%s ip addr add %s/32 dev %s", nsprefix, ip3, VPCMIDO_MD_VETH_H);
    se_add(&cmds, cmd, NULL, ignore_exit2);

    snprintf(cmd, EUCA_MAX_PATH, "%s ip link set %s up", nsprefix, VPCMIDO_MD_VETH_H);
    se_add(&cmds, cmd, NULL, ignore_exit2);

    snprintf(cmd, EUCA_MAX_PATH, "ip link set %s up", VPCMIDO_MD_VETH_M);
    rc = se_add(&cmds, cmd, NULL, ignore_exit);

    strptr = hex2dot(mido->mdconfig.mdnw);
    snprintf(nw, NETWORK_ADDR_LEN, "%s", strptr);
    EUCA_FREE(strptr);

    snprintf(sn, NETWORK_ADDR_LEN, "%d", mido->mdconfig.mdsn);

    snprintf(cmd, EUCA_MAX_PATH, "%s ip route add %s/%s via %s", nsprefix, nw, sn, ip1);
    rc = se_add(&cmds, cmd, NULL, ignore_exit2);

    se_print(&cmds);
    rc = se_execute(&cmds);
    if (rc) {
        LOGERROR("could not create eucamd external interfaces: see above log entries for details\n");
        ret = 1;
    }
    se_free(&cmds);

    return (ret);
}

/**
 * Extract midomd external port egress rules from configuration and create an
 * array of mido_parsed_chain_rule data structures.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param parsedrules [out] pointer to an array of pointer to mido_parsed_chain_rule data structures.
 * Memory is allocated for the array of pointers, and for the data structure pointed by
 * each entry in the array. Caller is responsible to release this memory.
 * Use free_ptrarr() to release memory.
 * @param max_parsedrules [out] number entries in parsedrules
 * @return 0 on success. 1 on any failure.
 */
int parse_mido_md_egress_rules(mido_config *mido, mido_parsed_chain_rule ***parsedrules, int *max_parsedrules) {
    if (!mido || !parsedrules || !max_parsedrules) {
        LOGWARN("Invalid argument: cannot parse md egress rules with NULL\n");
        return (1);
    }

    mido_parsed_chain_rule **result = *parsedrules;
    
    char **ports = NULL;
    int max_ports = 0;
    
    // Get rules to create
    euca_split_string(mido->config->mido_md_254_egress, &ports, &max_ports, ' ');
    for (int i = 0; i < max_ports; i++) {
        char protoc[8];
        int port = 0;
        protoc[0] = '\0';
        if (sscanf(ports[i], "%3s:%d", protoc, &port) == 2) {
            mido_parsed_chain_rule *crule = NULL;
            result = EUCA_REALLOC_C(result, *max_parsedrules + 1, sizeof (void *));
            result[*max_parsedrules] = EUCA_ZALLOC_C(1, sizeof (mido_parsed_chain_rule));
            crule = result[*max_parsedrules];
            (*max_parsedrules)++;
            clear_parsed_chain_rule(crule);
            if (!strcmp(protoc, "udp")) {
                snprintf(crule->jsonel[MIDO_CRULE_PROTO], 64, "17");
            }
            if (!strcmp(protoc, "tcp")) {
                snprintf(crule->jsonel[MIDO_CRULE_PROTO], 64, "6");
            }
            snprintf(crule->jsonel[MIDO_CRULE_TPD], 64, "jsonjson");
            snprintf(crule->jsonel[MIDO_CRULE_TPD_S], 64, "%d", port);
            snprintf(crule->jsonel[MIDO_CRULE_TPD_E], 64, "%d", port);
            snprintf(crule->jsonel[MIDO_CRULE_TPD_END], 64, "END");
            snprintf(crule->jsonel[MIDO_CRULE_NW], 64, "169.254.169.254");
            snprintf(crule->jsonel[MIDO_CRULE_NWLEN], 64, "32");
        }
    }
    free_ptrarr(ports, max_ports);
    ports = NULL;
    max_ports = 0;
    
    euca_split_string(mido->config->mido_md_253_egress, &ports, &max_ports, ' ');
    for (int i = 0; i < max_ports; i++) {
        char protoc[8];
        int port = 0;
        protoc[0] = '\0';
        if (sscanf(ports[i], "%3s:%d", protoc, &port) == 2) {
            mido_parsed_chain_rule *crule = NULL;
            result = EUCA_REALLOC_C(result, *max_parsedrules + 1, sizeof (void *));
            result[*max_parsedrules] = EUCA_ZALLOC_C(1, sizeof (mido_parsed_chain_rule));
            crule = result[*max_parsedrules];
            (*max_parsedrules)++;
            clear_parsed_chain_rule(crule);
            if (!strcmp(protoc, "udp")) {
                snprintf(crule->jsonel[MIDO_CRULE_PROTO], 64, "6");
            }
            if (!strcmp(protoc, "tcp")) {
                snprintf(crule->jsonel[MIDO_CRULE_PROTO], 64, "17");
            }
            snprintf(crule->jsonel[MIDO_CRULE_TPD], 64, "jsonjson");
            snprintf(crule->jsonel[MIDO_CRULE_TPD_S], 64, "%d", port);
            snprintf(crule->jsonel[MIDO_CRULE_TPD_E], 64, "%d", port);
            snprintf(crule->jsonel[MIDO_CRULE_TPD_END], 64, "END");
            snprintf(crule->jsonel[MIDO_CRULE_NW], 64, "169.254.169.253");
            snprintf(crule->jsonel[MIDO_CRULE_NWLEN], 64, "32");
        }
    }
    free_ptrarr(ports, max_ports);
    ports = NULL;
    max_ports = 0;

    *parsedrules = result;
    return (0);
}

/**
 * Add egress filtering rules to the given chain. The chain in the argument is assumed
 * to be the chain attached to VPCMIDO eucamdrt external port.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @param chain [in] MN chain where egress rules will be added.
 * @return 0 on success. Positive integer otherwise.
 */
int create_mido_md_egress_rules(mido_config *mido, midonet_api_chain *chain) {
    int ret = 0;
    int rc = 0;

    if (!mido || !mido->midomd || !chain) {
        LOGWARN("Invalid argument: cannot create mido MD egress rules with NULL config\n");
        return (1);
    }

    mido_parsed_chain_rule **rules = NULL;
    int max_rules = 0;
    parse_mido_md_egress_rules(mido, &rules, &max_rules);
    
    if (mido->config->eucanetd_first_update) {
        if ((max_rules + 1) != chain->rules_count) {
            mido->config->mido_md_egress_rules_changed = TRUE;
        }
    }

    if (mido->config->mido_md_egress_rules_changed) {
        rc = mido_clear_rules(chain);
        if (rc) {
            LOGWARN("failed to clear mido_md egress rules\n");
        }
        mido->config->mido_md_egress_rules_changed = FALSE;
    }
    
    for (int i = 0; i < max_rules; i++) {
        mido_parsed_chain_rule *crule = rules[i];
        rc = create_mido_vpc_secgroup_rule(chain, NULL, -1, MIDO_RULE_SG_EGRESS, crule);
        if (rc) {
            ret++;
        }
    }

    // default drop all
    char pos[16];
    snprintf(pos, 16, "%d", chain->rules_count + 1);
    rc = mido_create_rule(chain, chain->obj, NULL, NULL,
            "position", pos, "type", "drop", "invDlType", "true", "dlType", "2054", NULL);
    if (rc) {
        LOGWARN("Failed to create egress drop rule for midomdrt ext port\n");
        ret++;
    }

    free_ptrarr(rules, max_rules);
    return (ret);
}

/**
 * Creates MidoNet objects that are needed to implement euca VPC MD.
 * @param mido [in] data structure that holds all discovered MidoNet configuration/resources.
 * @return 0 on success. 1 otherwise.
 */
int create_mido_md(mido_config *mido) {
    int ret = 0;
    int rc = 0;
    char nw[NETWORK_ADDR_LEN];
    char sn[NETWORK_ADDR_LEN];
    char ip1[NETWORK_ADDR_LEN];
    char *strptr = NULL;

    if (!mido || !mido->midomd) {
        LOGWARN("Invalid argument: cannot create mido MD with NULL config\n");
        return (1);
    }
    
    mido_md *midomd = mido->midomd;

    if (!midomd->population_failed) {
        LOGTRACE("\tmidomd fully implemented\n");
    }
    LOGTRACE("creating midomd\n");

    midomd->eucamdrt = mido_create_router(VPCMIDO_TENANT, VPCMIDO_MDRT, &(midomd->midos[MD_RT]));
    if (midomd->eucamdrt == NULL) {
        LOGERROR("cannot create eucamdrt: check midonet health\n");
        ret++;
    }

    strptr = hex2dot(mido->mdconfig.int_mdnw);
    snprintf(nw, NETWORK_ADDR_LEN, "%s", strptr);
    EUCA_FREE(strptr);

    strptr = hex2dot(mido->mdconfig.int_mdnw + 1);
    snprintf(ip1, NETWORK_ADDR_LEN, "%s", strptr);
    EUCA_FREE(strptr);

    snprintf(sn, NETWORK_ADDR_LEN, "%d", mido->mdconfig.int_mdsn);

    rc = mido_create_router_port(midomd->eucamdrt, midomd->midos[MD_RT], ip1, nw, sn,
            NULL, &(midomd->midos[MD_RT_BRPORT]));
    if (rc) {
        LOGERROR("cannot create eucamdrt port: check midonet health\n");
        ret++;
    }

    rc = mido_create_route(midomd->eucamdrt, midomd->midos[MD_RT], midomd->midos[MD_RT_BRPORT],
            "0.0.0.0", "0", nw, sn, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create eucamdrt int route: check midonet health\n");
        ret++;
    }

    midomd->eucamdbr = mido_create_bridge(VPCMIDO_TENANT, VPCMIDO_MDBR, &(midomd->midos[MD_BR]));
    if (midomd->eucamdbr == NULL) {
        LOGERROR("cannot create eucamdbr: check midonet health\n");
        ret++;
    }

    rc = mido_create_bridge_port(midomd->eucamdbr, midomd->midos[MD_BR], &(midomd->midos[MD_BR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create eucamdbr port: check midonet health\n");
        ret++;
    }

    rc = mido_link_ports(midomd->midos[MD_RT_BRPORT], midomd->midos[MD_BR_RTPORT]);
    if (rc) {
        LOGERROR("cannot create eucamdrt <-> eucamdbr link: check midonet health\n");
        ret++;
    }

    strptr = hex2dot(mido->mdconfig.ext_mdnw);
    snprintf(nw, NETWORK_ADDR_LEN, "%s", strptr);
    EUCA_FREE(strptr);

    strptr = hex2dot(mido->mdconfig.md_veth_mido);
    snprintf(ip1, NETWORK_ADDR_LEN, "%s", strptr);
    EUCA_FREE(strptr);

    snprintf(sn, NETWORK_ADDR_LEN, "%d", mido->mdconfig.ext_mdsn);

    rc = mido_create_router_port(midomd->eucamdrt, midomd->midos[MD_RT], ip1, nw, sn,
            NULL, &(midomd->midos[MD_RT_EXTPORT]));
    if (rc) {
        LOGERROR("cannot create eucamdrt port: check midonet health\n");
        ret++;
    }
    rc = mido_create_route(midomd->eucamdrt, midomd->midos[MD_RT], midomd->midos[MD_RT_EXTPORT],
            "0.0.0.0", "0", nw, sn, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create eucamdrt ext route: check midonet health\n");
        ret++;
    }

    // attach outfilter to eucamdrt external port
    midonet_api_chain *eucamdrt_extport_outfilter = NULL;
    eucamdrt_extport_outfilter = mido_create_chain(VPCMIDO_TENANT, "eucamdrt_extportout", &(midomd->midos[MD_RT_EXTPORT_OUTFILTER]));
    if (!eucamdrt_extport_outfilter) {
        LOGERROR("cannot create eucamdrt outfilter.\n");
        ret++;
    } else {
        midomd->eucamdrt_extport_outfilter = eucamdrt_extport_outfilter;
        char *portmac = NULL;
        if (midomd->midos[MD_RT_EXTPORT] && midomd->midos[MD_RT_EXTPORT]->port && midomd->midos[MD_RT_EXTPORT]->port->portmac) {
            portmac = midomd->midos[MD_RT_EXTPORT]->port->portmac;
        }
        rc = mido_update_port(midomd->midos[MD_RT_EXTPORT],
                "outboundFilterId", midomd->midos[MD_RT_EXTPORT_OUTFILTER]->uuid,
                "id", midomd->midos[MD_RT_EXTPORT]->uuid,
                "networkAddress", nw, "networkLength", sn, "portAddress", ip1,
                "type", "Router",
                "portMac", portmac,
                NULL);

        if ((rc != 0) && (rc != -1)) {
            LOGERROR("cannot attach eucamdrt extport outfilter\n");
            ret++;
        }
        
        // Create eucamdrt external port egress rules
        create_mido_md_egress_rules(mido, eucamdrt_extport_outfilter);
    }

    // create veth pair and connect eucamdrt
    rc = connect_mido_md_ext_veth(mido);

    if (mido->midocore && mido->midocore->eucanetdhost) {
        rc = mido_link_host_port(mido->midocore->eucanetdhost->obj, VPCMIDO_MD_VETH_M,
                midomd->midos[MD_RT], midomd->midos[MD_RT_EXTPORT]);
        if (rc) {
            LOGERROR("cannot link eucamdrt port to host interface: check midonet health\n");
            ret++;
        } else {
            // start md nginx
            do_md_nginx_maintain(mido, VPCMIDO_NGINX_START);
        }
    }

    return (ret);
}

/**
 * Removes all bridges, routers, chains, and ip-address-groups from mido
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_delete_all(mido_config *mido) {
    int ret = midonet_api_delete_all();

    return (ret);
}

/**
 * Go through bridges, routers, chains, and ip-address-groups and check/delete
 * duplicate entries.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param onlycheck [in] if true, do not delete (only perform snaity checks)
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_delete_dups(mido_config *mido, boolean checkonly) {
    int ret = midonet_api_delete_dups(checkonly);

    return (ret);
}

/**
 * Populate the given ID from MidoNet and check its health or delete from MidoNet.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param id [in] ID of the VPC object of interest
 * @param onlycheck [in] if true, do not delete (only perform sanity checks)
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_delete_vpc_object(mido_config *mido, char *id, boolean checkonly) {
    enum object_type {
        INSTANCE,
        VPC,
        SUBNET,
        NATG,
        SG,
        LIST,
        LIST_GATEWAYS,
        UNCONNECTED,
        TEST,
    } do_type;
    boolean do_process = FALSE;
    int ret = 1;
    if (!id) {
        LOGWARN("Cannot do anything with a NULL id\n");
        return (1);
    }
    if (!do_process && (strlen(id) >= 10) && (strstr(id, "i-") || strstr(id, "eni-"))) {
        LOGINFO("\tprocessing interface %s\n", id);
        do_process = TRUE;
        do_type = INSTANCE;
    }
    if (!do_process && (strlen(id) >= 10) && (strstr(id, "vpc-"))) {
        LOGINFO("\tprocessing vpc %s\n", id);
        do_process = TRUE;
        do_type = VPC;
    }
    if (!do_process && (strlen(id) >= 10) && (strstr(id, "subnet-"))) {
        LOGINFO("\tprocessing vpc subnet %s\n", id);
        do_process = TRUE;
        do_type = SUBNET;
    }
    if (!do_process && (strlen(id) >= 10) && (strstr(id, "nat-"))) {
        LOGINFO("\tprocessing vpc NAT gateway %s\n", id);
        do_process = TRUE;
        do_type = NATG;
    }
    if (!do_process && (strlen(id) >= 10) && (strstr(id, "sg-"))) {
        LOGINFO("\tprocessing security group %s\n", id);
        do_process = TRUE;
        do_type = SG;
    }
    if (!do_process && (!strcmp(id, "list"))) {
        do_process = TRUE;
        do_type = LIST;
    }
    if (!do_process && (!strcmp(id, "list_gateways"))) {
        do_process = TRUE;
        do_type = LIST_GATEWAYS;
    }
    if (!do_process && (!strcmp(id, "unconnected"))) {
        do_process = TRUE;
        do_type = UNCONNECTED;
    }
    if (!do_process && (!strcmp(id, "test"))) {
        do_process = TRUE;
        do_type = TEST;
    }

    if (!do_process) {
        LOGWARN("Unable to recognize %s\n", id);
    } else {
        int rc = 0;
        log_params_set(EUCA_LOG_ERROR, 0, 100000);
        LOGINFO("Loading objects from MidoNet.\n");
        rc = midonet_api_cache_refresh_v_threads(MIDO_CACHE_REFRESH_ALL);
        if (rc) {
            LOGERROR("failed to retrieve objects from MidoNet.\n");
            return (ret);
        }

        rc = reinitialize_mido(mido);
        if (rc) {
            LOGERROR("unable to initialize data structures\n");
            return (ret);
        }
        populate_mido_core(mido, mido->midocore);
        populate_mido_md(mido);
        rc = do_midonet_populate_vpcs(mido);
        if (rc) {
            LOGERROR("failed to populate euca VPC models\n");
            return (ret);
        }
        log_params_set(EUCA_LOG_INFO, 0, 100000);

        switch (do_type) {
            case VPC:
                ret = do_midonet_delete_vpc(mido, id, checkonly);
                break;
            case SUBNET:
                ret = do_midonet_delete_vpcsubnet(mido, id, checkonly);
                break;
            case NATG:
                ret = do_midonet_delete_natg(mido, id, checkonly);
                break;
            case INSTANCE:
                ret = do_midonet_delete_interface(mido, id, checkonly);
                break;
            case SG:
                ret = do_midonet_delete_securitygroup(mido, id, checkonly);
                break;
            case LIST:
                ret = do_midonet_list(mido);
                break;
            case LIST_GATEWAYS:
                ret = do_midonet_list_gateways(mido);
                break;
            case UNCONNECTED:
                ret = do_midonet_delete_unconnected(mido, checkonly);
                break;
            case TEST:
                do_midonet_test(mido);
                break;
        }
    }
    return (ret);
}

/**
 * Check the given vpc health or delete from MidoNet.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param id [in] ID of the vpc of interest
 * @param onlycheck [in] if true, do not delete (only perform snaity checks)
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_delete_vpc(mido_config *mido, char *id, boolean checkonly) {
    int rc = 0;
    if (!mido || !id) {
        return (1);
    }
    boolean incomplete = FALSE;
    boolean clean = TRUE;
    mido_vpc *vpc = NULL;
    if (!find_mido_vpc(mido, id, &vpc) && vpc) {
        if (!checkonly) {
            if (!mido->config->enable_mido_md) {
                rc = do_metaproxy_teardown(mido);
                if (rc) {
                    LOGERROR("cannot teardown metadata proxies, aborting vpc flush\n");
                    return (1);
                }
            }

            int ret = delete_mido_vpc(mido, vpc);

            // Re-enable nginx
            rc = 0;
            if (!mido->config->enable_mido_md) {
                rc = do_metaproxy_setup(mido);
            }
            if (rc) {
                LOGERROR("failed to setup metadata proxies\n");
            }
            return (ret);
        }
        for (int i = 0; i < VPC_END; i++) {
            if (!vpc->midos[i]) {
                LOGWARN("\t\tmissing object (%d)\n", i);
                incomplete = TRUE;
            }
        }
        if (!incomplete) {
            LOGINFO("\tchecking %s %d routes\n", vpc->vpcrt->obj->name, vpc->vpcrt->max_routes);
            if (midonet_api_delete_dups_routes(vpc->vpcrt, checkonly)) {
                clean = FALSE;
            }
            LOGINFO("\tchecking %s %d port(s)\n", vpc->vpcrt->obj->name, vpc->vpcrt->max_ports);
            if (midonet_api_delete_unconnected_ports(vpc->vpcrt->ports,
                    vpc->vpcrt->max_ports, checkonly)) {
                clean = FALSE;
            }
            LOGINFO("\tchecking %s %d rules(s)\n", vpc->rt_preelipchain->obj->name, vpc->rt_preelipchain->max_rules);
            if (midonet_api_delete_dups_rules(vpc->rt_preelipchain, checkonly)) {
                clean = FALSE;
            }
            LOGINFO("\tchecking %s %d rules(s)\n", vpc->rt_uplink_prechain->obj->name, vpc->rt_uplink_prechain->max_rules);
            if (midonet_api_delete_dups_rules(vpc->rt_uplink_prechain, checkonly)) {
                clean = FALSE;
            }
            LOGINFO("\tchecking %s %d rules(s)\n", vpc->rt_uplink_postchain->obj->name, vpc->rt_uplink_postchain->max_rules);
            if (midonet_api_delete_dups_rules(vpc->rt_uplink_postchain, checkonly)) {
                clean = FALSE;
            }
            if (clean) {
                LOGINFO("\t=== ok ===\n");
            }

            for (int i = 0; i < vpc->max_subnets; i++) {
                mido_vpc_subnet *subnet = &(vpc->subnets[i]);
                LOGINFO("Checking subnet %s\n", subnet->name);
                if (do_midonet_delete_vpcsubnet(mido, subnet->name, checkonly)) {
                    clean = FALSE;
                }
            }

            if (clean) {
                return (0);
            }
        } else {
            LOGWARN("\t\t%s is not fully implemented\n", vpc->name);
        }
    } else {
        LOGWARN("\t\t%s not found\n", id);
    }
    return (1);
}

/**
 * Check the given vpcsubnet health or delete from MidoNet.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param id [in] ID of the vpcsubnet of interest
 * @param onlycheck [in] if true, do not delete (only perform snaity checks)
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_delete_vpcsubnet(mido_config *mido, char *id, boolean checkonly) {
    if (!mido || !id) {
        return (1);
    }
    boolean incomplete = FALSE;
    boolean clean = TRUE;
    mido_vpc *vpc = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    if (!find_mido_vpc_subnet_global(mido, id, &vpc, &vpcsubnet) && vpc && vpcsubnet) {
        if (!checkonly) {
            return (delete_mido_vpc_subnet(mido, vpc, vpcsubnet));
        }
        for (int i = 0; i < SUBN_END; i++) {
            if (!vpcsubnet->midos[i]) {
                LOGWARN("\t\tmissing object (%d)\n", i);
                incomplete = TRUE;
            }
        }
        if (!incomplete) {
            LOGINFO("\tchecking %s %d port(s)\n", vpcsubnet->subnetbr->obj->name, vpcsubnet->subnetbr->max_ports);
            if (midonet_api_delete_unconnected_ports(vpcsubnet->subnetbr->ports,
                    vpcsubnet->subnetbr->max_ports, checkonly)) {
                clean = FALSE;
            }
            LOGINFO("\tchecking %s %d dhcp(s)\n", vpcsubnet->subnetbr->obj->name, vpcsubnet->subnetbr->max_dhcps);
            if (vpcsubnet->subnetbr->max_dhcps != 1) {
                LOGWARN("\t\tunexpected number of dhcps\n");
                clean = FALSE;
            } else {
                LOGINFO("\tchecking %s %d dhcphost(s)\n", vpcsubnet->subnetbr->dhcps[0]->obj->name,
                        vpcsubnet->subnetbr->dhcps[0]->max_dhcphosts);
                if (vpcsubnet->max_instances != vpcsubnet->subnetbr->dhcps[0]->max_dhcphosts) {
                    LOGWARN("\t\tunexpected number of dhcphost(s)\n");
                    clean = FALSE;
                }
            }
            if (clean) {
                LOGINFO("\t=== ok ===\n");
            }

            for (int i = 0; i < vpcsubnet->max_natgateways; i++) {
                mido_vpc_natgateway *natg = &(vpcsubnet->natgateways[i]);
                LOGINFO("checking %s\n", natg->name);
                if (do_midonet_delete_natg(mido, natg->name, checkonly)) {
                    clean = FALSE;
                }
            }
            for (int i = 0; i < vpcsubnet->max_instances; i++) {
                mido_vpc_instance *ifc = &(vpcsubnet->instances[i]);
                LOGINFO("checking interface %s\n", ifc->name);
                if (do_midonet_delete_interface(mido, ifc->name, checkonly)) {
                    clean = FALSE;
                }
            }
            if (clean) {
                return (0);
            }
        } else {
            LOGWARN("\t\t%s is not fully implemented\n", vpcsubnet->name);
        }
    } else {
        LOGWARN("\t\t%s not found\n", id);
    }
    return (1);
}

/**
 * Check the given NAT gateway health or delete from MidoNet.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param id [in] ID of the NAT gateway of interest
 * @param onlycheck [in] if true, do not delete (only perform snaity checks)
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_delete_natg(mido_config *mido, char *id, boolean checkonly) {
    if (!mido || !id) {
        return (1);
    }
    boolean incomplete = FALSE;
    boolean clean = TRUE;
    mido_vpc *vpc = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc_natgateway *vpcnatgateway = NULL;
    if (!find_mido_vpc_natgateway_global(mido, id, &vpc, &vpcsubnet, &vpcnatgateway) &&
            vpc && vpcsubnet && vpcnatgateway) {
        if (!checkonly) {
            return (delete_mido_vpc_natgateway(mido, vpcsubnet, vpcnatgateway));
        }
        for (int i = 0; i < NATG_END; i++) {
            if (!vpcnatgateway->midos[i]) {
                LOGWARN("\t\tmissing object (%d)\n", i);
                incomplete = TRUE;
            }
        }
        if (!incomplete) {
            LOGINFO("\tchecking %s %d port(s)\n", vpcnatgateway->natgrt->obj->name, vpcnatgateway->natgrt->max_routes);
            if (!midonet_api_delete_dups_routes(vpc->vpcrt, checkonly)) {
            } else {
                clean = FALSE;
            }
            LOGINFO("\tchecking %s %d port(s)\n", vpcnatgateway->natgrt->obj->name, vpcnatgateway->natgrt->max_ports);
            if (!midonet_api_delete_unconnected_ports(vpcnatgateway->natgrt->ports,
                    vpcnatgateway->natgrt->max_ports, checkonly)) {
            }
            LOGINFO("\tchecking %s %d rules(s)\n", vpcnatgateway->inchain->obj->name, vpcnatgateway->inchain->max_rules);
            if (midonet_api_delete_dups_rules(vpcnatgateway->inchain, checkonly)) {
                clean = FALSE;
            }
            LOGINFO("\tchecking %s %d rules(s)\n", vpcnatgateway->outchain->obj->name, vpcnatgateway->outchain->max_rules);
            if (midonet_api_delete_dups_rules(vpcnatgateway->outchain, checkonly)) {
                clean = FALSE;
            }

            if (clean) {
                LOGINFO("\t=== ok ===\n");
                return (0);
            }
        } else {
            LOGWARN("\t\t%s is not fully implemented\n", vpcnatgateway->name);
        }
    } else {
        LOGWARN("\t\t%s not found\n", id);
    }
    return (1);
}

/**
 * Check the given interface health or delete from MidoNet.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param id [in] ID of the interface of interest
 * @param onlycheck [in] if true, do not delete (only perform sanity check)
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_delete_interface(mido_config *mido, char *id, boolean checkonly) {
    if (!mido || !id) {
        return (1);
    }
    boolean incomplete = FALSE;
    boolean clean = TRUE;
    mido_vpc *vpc = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc_instance *ifc = NULL;
    if (!find_mido_vpc_instance_global(mido, id, &vpc, &vpcsubnet, &ifc) &&
            vpc && vpcsubnet && ifc) {
        if (!checkonly) {
            return (delete_mido_vpc_instance(mido, vpc, vpcsubnet, ifc));
        }
        for (int i = 0; i < INST_ELIP_PRE_IPADDRGROUP_IP; i++) {
            if (ifc->midos[i] == NULL) {
                LOGWARN("\t\tmissing object (%d)\n", i);
                incomplete = TRUE;
            }
        }
        if (ifc->midos[INST_ELIP_PRE_IPADDRGROUP_IP]) {
            for (int i = INST_ELIP_PRE_IPADDRGROUP_IP; i < INST_END; i++) {
                if (ifc->midos[i] == NULL) {
                    LOGWARN("\t\tmissing object (%d)\n", i);
                    incomplete = TRUE;
                }
            }
        }
        if (!incomplete) {
            LOGINFO("\tchecking %s %d rules(s)\n", ifc->prechain->obj->name, ifc->prechain->max_rules);
            if (midonet_api_delete_dups_rules(ifc->prechain, checkonly)) {
                clean = FALSE;
            }
            LOGINFO("\tchecking %s %d rules(s)\n", ifc->postchain->obj->name, ifc->postchain->max_rules);
            if (midonet_api_delete_dups_rules(ifc->postchain, checkonly)) {
                clean = FALSE;
            }

            if (clean) {
                LOGINFO("\t=== ok ===\n");
                return (0);
            }
        } else {
            LOGWARN("\t\t%s is not fully implemented\n", ifc->name);
        }
    } else {
        LOGWARN("\t\t%s not found\n", id);
    }
    return (1);
}

/**
 * Check the given security group health or delete from MidoNet.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param id [in] ID of the security group of interest
 * @param onlycheck [in] if true, do not delete (only perform sanity check)
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_delete_securitygroup(mido_config *mido, char *id, boolean checkonly) {
    if (!mido || !id) {
        return (1);
    }
    boolean incomplete = FALSE;
    boolean clean = TRUE;
    mido_vpc_secgroup *sg = NULL;
    if (!find_mido_vpc_secgroup(mido, id, &sg) && sg) {
        if (!checkonly) {
            return (delete_mido_vpc_secgroup(mido, sg));
        }
        for (int i = 0; i < VPCSG_END; i++) {
            if (sg->midos[i] == NULL) {
                LOGWARN("\t\tmissing object (%d)\n", i);
                incomplete = TRUE;
            }
        }
        if (!incomplete) {
            LOGINFO("\tchecking %s %d rules(s)\n", sg->ingress->obj->name, sg->ingress->max_rules);
            if (midonet_api_delete_dups_rules(sg->ingress, checkonly)) {
                clean = FALSE;
            }
            LOGINFO("\tchecking %s %d rules(s)\n", sg->egress->obj->name, sg->egress->max_rules);
            if (midonet_api_delete_dups_rules(sg->egress, checkonly)) {
                clean = FALSE;
            }
            LOGINFO("\t%s %d ips\n", sg->iag_priv->obj->name, sg->iag_priv->ips_count);
            LOGINFO("\t%s %d ips\n", sg->iag_priv->obj->name, sg->iag_pub->ips_count);
            LOGINFO("\t%s %d ips\n", sg->iag_priv->obj->name, sg->iag_all->ips_count);

            if (clean) {
                LOGINFO("\t=== ok ===\n");
                return (0);
            }
        } else {
            LOGWARN("\t\t%s is not fully implemented\n", sg->name);
        }
    } else {
        LOGWARN("\t\t%s not found\n", id);
    }
    return (1);
}

/**
 * List all VPCMIDO constructs detected in MN.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_list(mido_config *mido) {
    if (!mido) {
        return (1);
    }
    LOGINFO("Detected VPCMIDO objects:\n");

    int max_vpcs = mido->max_vpcs;
    int max_subnets = 0;
    int max_interfaces = 0;
    int max_natgs = 0;
    int max_sgs = mido->max_vpcsecgroups;
    for (int i = 0; i < mido->max_vpcs; i++) {
        mido_vpc *vpc = &(mido->vpcs[i]);
        max_subnets += vpc->max_subnets;
        for (int j = 0; j < vpc->max_subnets; j++) {
            mido_vpc_subnet *subnet = &(vpc->subnets[j]);
            max_interfaces += subnet->max_instances;
            max_natgs += subnet->max_natgateways;
        }
    }
    
    LOGINFO("%d VPCs, %d subnets, %d interfaces, %d NAT gws, %d SGs\n",
            max_vpcs, max_subnets, max_interfaces, max_natgs, max_sgs);

    int buflen = 32 * (max_vpcs + max_subnets + max_interfaces + max_natgs + max_sgs) + 4000;
    char *buf = EUCA_ZALLOC_C(1, buflen);

    char *pbuf = buf;
    for (int i = 0; i < mido->max_vpcs; i++) {
        mido_vpc *vpc = &(mido->vpcs[i]);
        euca_buffer_snprintf(&pbuf, &buflen, "\n%s", vpc->name);
        for (int j = 0; j < vpc->max_subnets; j++) {
            mido_vpc_subnet *subnet = &(vpc->subnets[j]);
            euca_buffer_snprintf(&pbuf, &buflen, "\n\t%s", subnet->name);
            for (int k = 0; k < subnet->max_instances; k++) {
                mido_vpc_instance *ifc = &(subnet->instances[k]);
                if (k % 4 == 0) {
                    euca_buffer_snprintf(&pbuf, &buflen, "\n\t\t");
                }
                euca_buffer_snprintf(&pbuf, &buflen, "%s ", ifc->name);
            }
            for (int k = 0; k < subnet->max_natgateways; k++) {
                mido_vpc_natgateway *natg = &(subnet->natgateways[k]);
                if (k % 2 == 0) {
                    euca_buffer_snprintf(&pbuf, &buflen, "\n\t\t");
                }
                euca_buffer_snprintf(&pbuf, &buflen, "%s ", natg->name);
            }
        }
    }
    for (int i = 0; i < mido->max_vpcsecgroups; i++) {
        mido_vpc_secgroup *sg = &(mido->vpcsecgroups[i]);
        if (i % 4 == 0) {
            euca_buffer_snprintf(&pbuf, &buflen, "\n");
        }
        euca_buffer_snprintf(&pbuf, &buflen, "%s ", sg->name);
    }

    if (mido->midocore && mido->midocore->max_gws) {
        euca_buffer_snprintf(&pbuf, &buflen, "\n\ngateways: host:ext_dev:ext_ip:ext_cidr\n");
        for (int i = 0; i < mido->midocore->max_gws; i++) {
            mido_gw *gw = mido->midocore->gws[i];
            if (gw && gw->host && gw->host->obj) {
                euca_buffer_snprintf(&pbuf, &buflen, "\t%s:%s:%s:%s", gw->host->obj->name,
                        gw->ext_dev, gw->ext_ip, gw->ext_cidr);
                if (gw->asn && gw->peer_asn) {
                    euca_buffer_snprintf(&pbuf, &buflen, " (BGP)\n");
                } else {
                    euca_buffer_snprintf(&pbuf, &buflen, "\n");
                }
            }
        }
    }
    char *bgprecovery = NULL;
    bgprecovery = discover_mido_bgps(mido);
    if (bgprecovery && strlen(bgprecovery)) {
        euca_buffer_snprintf(&pbuf, &buflen, "\nmido BGP configuration (for manual recovery):\n%s", bgprecovery);
    }
    EUCA_FREE(bgprecovery);

    printf("%s\n", buf);
    
    EUCA_FREE(buf);
    return (0);
}

/**
 * List all VPCMIDO gateway constructs detected in MN.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_list_gateways(mido_config *mido) {
    if (!mido || !mido->midocore) {
        return (1);
    }

    mido_core *midocore = mido->midocore;
    int max_gws = midocore->max_gws;
    
    LOGINFO("%d VPCMIDO gateway(s) detected\n", max_gws);

    int buflen = max_gws * 2048;
    char *buf = EUCA_ZALLOC_C(1, buflen);

    char *pbuf = buf;

    for (int i = 0; i < max_gws; i++) {
        mido_gw *gw = midocore->gws[i];
        if (gw && gw->host && gw->host->obj) {
            euca_buffer_snprintf(&pbuf, &buflen, "\ngateway[%d]:\n", i);
            euca_buffer_snprintf(&pbuf, &buflen, "\thost     : %s\n", gw->host->obj->name);
            euca_buffer_snprintf(&pbuf, &buflen, "\text_dev  : %s\n", gw->ext_dev);
            euca_buffer_snprintf(&pbuf, &buflen, "\text_ip   : %s\n", gw->ext_ip);
            euca_buffer_snprintf(&pbuf, &buflen, "\text_cidr : %s\n", gw->ext_cidr);
            euca_buffer_snprintf(&pbuf, &buflen, "\tpeer_ip  : %s\n", gw->peer_ip);
            if (gw->asn && gw->peer_asn) {
                euca_buffer_snprintf(&pbuf, &buflen, "\t    asn      : %d\n", gw->asn);
                euca_buffer_snprintf(&pbuf, &buflen, "\t    peer_asn : %d\n", gw->peer_asn);
                if (is_midonet_api_v1()) {
                    if (gw->bgp_v1) {
                        char *status = NULL;
                        mido_getel_midoname(gw->bgp_v1, "status", &status);
                        if (status && strlen(status)) {
                            euca_buffer_snprintf(&pbuf, &buflen, "\t    bgp_status :\n%s\n", status);
                        }
                        EUCA_FREE(status);
                    }
                }
                if (is_midonet_api_v5()) {
                    if (gw->port) {
                        char *status = NULL;
                        mido_getel_midoname(gw->port, "bgpStatus", &status);
                        if (status && strlen(status)) {
                            euca_buffer_snprintf(&pbuf, &buflen, "\t    bgp_status :\n%s\n", status);
                        }
                        EUCA_FREE(status);
                    }
                }
            } else {
                euca_buffer_snprintf(&pbuf, &buflen, "\n");
            }
        }
    }

    printf("%s\n", buf);
    
    EUCA_FREE(buf);
    return (0);
}

/**
 * Search and delete unconnected bridge/router ports, ip-address-groups, and chains.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param onlycheck [in] if true, do not delete (only perform sanity check)
 * @return 0 on success. Negative integer if unconnected object(s) was/were detected.
 * Positive integer on any error.
 */
int do_midonet_delete_unconnected(mido_config *mido, boolean checkonly) {
    boolean gDetected = FALSE;
    do_midonet_tag_midonames(mido);
    midonet_api_cache *cache = midonet_api_cache_get();
    if (!cache) {
        LOGWARN("failed to access MIDOCACHE\n");
        return (1);
    }
    // Delete all ports that are not connected
    LOGINFO("\tchecking ports\n");
    if (midonet_api_delete_unconnected_ports(cache->ports, cache->max_ports, checkonly)) {
        gDetected = TRUE;
        if (!checkonly) {
        }
    }
    LOGINFO("\tchecking chains\n");
    for (int i = 0; i < cache->max_chains; i++) {
        if (!cache->chains[i]) {
            continue;
        }
        if (!cache->chains[i]->obj->tag) {
            LOGINFO("\t\t%s\n", cache->chains[i]->obj->name);
            gDetected = TRUE;
            if (!checkonly) {
                mido_delete_resource(NULL, cache->chains[i]->obj);
            }
        }
    }
    LOGINFO("\tchecking ip-address-groups\n");
    for (int i = 0; i < cache->max_ipaddrgroups; i++) {
        if (!cache->ipaddrgroups[i]) {
            continue;
        }
        if (!cache->ipaddrgroups[i]->obj->tag) {
            LOGINFO("\t\t%s\n", cache->ipaddrgroups[i]->obj->name);
            gDetected = TRUE;
            if (!checkonly) {
                mido_delete_resource(NULL, cache->ipaddrgroups[i]->obj);
            }
        }
    }
    LOGINFO("\tchecking arp_table and mac_table entries\n");
    for (int i = 0; i < cache->max_bridges; i++) {
        midonet_api_bridge *br = cache->bridges[i];
        if (!br) {
            continue;
        }
        for (int j = 0; j < br->max_ip4mac_pairs; j++) {
            midoname *ip4mac = br->ip4mac_pairs[j];
            if (!ip4mac) {
                continue;
            }
            if (!ip4mac->tag) {
                LOGINFO("\t\t%s_%s\n", ip4mac->ip4mac->ip, ip4mac->ip4mac->mac);
                gDetected = TRUE;
                if (!checkonly) {
                    mido_delete_resource(NULL, ip4mac);
                }
            }
        }
        for (int j = 0; j < br->max_macport_pairs; j++) {
            midoname *macport = br->macport_pairs[j];
            if (!macport) {
                continue;
            }
            if (!macport->tag) {
                LOGINFO("\t\t%s_%s\n", macport->macport->macAddr, macport->macport->portId);
                // macport pair entries are also automatically created
                // gDetected = TRUE;
                if (!checkonly) {
                    mido_delete_resource(NULL, macport);
                }
            }
        }
    }

    if (!gDetected) {
        LOGINFO("\t=== ok ===\n");
    } else {
        return (-1);
    }
    return (0);
}

/**
 * Creates mido-tz tunnel-zone and adds all detected hosts as member. IP address
 * of device dev is used as address of each member.
 * @param mido [in] data structure that holds MidoNet configuration
 * @param type [in] type of tunnel-zone (gre|vxlan) - defaults to gre
 * @param dev [in] device with the IP address of each tunnel-zone member host
 * @param refreshmido [in] if TRUE, reload mido data structures
 * @return 0 on success. Positive integer on error.
 */
int do_midonet_create_tzone(mido_config *mido, char *type, char *dev, boolean refreshmido) {
    int rc = 0;
    int i = 0;
    midoname **tzs = NULL;
    int max_tzs = 0;
    midoname **tzhosts = NULL;
    int max_tzhosts = 0;
    midoname *midotz = NULL;

    LOGINFO("\n");
    LOGINFO("Creating VPCMIDO tunnel-zone\n");
    log_params_set(EUCA_LOG_ERROR, 0, 100000);
    if (refreshmido) {
        LOGINFO("Loading objects from MidoNet.\n");
        rc = midonet_api_cache_refresh_v_threads(MIDO_CACHE_REFRESH_ALL);
        if (rc) {
            LOGERROR("failed to retrieve objects from MidoNet.\n");
            return (1);
        }
        rc = reinitialize_mido(mido);
        if (rc) {
            LOGERROR("unable to initialize data structures\n");
            return (1);
        }
    }

    rc = mido_get_tunnelzones(VPCMIDO_TENANT, &tzs, &max_tzs);
    if (rc == 0) {
        for (i = 0; i < max_tzs; i++) {
            if ((rc == 0) && (strstr(VPCMIDO_TUNNELZONE, tzs[i]->name))) {
                midotz = tzs[i];
            }
        }
    }

    char *tzname = NULL;
    midonet_api_tunnelzone *tz = NULL;
    if (midotz) {
        tzname = strdup(midotz->name);
    } else {
        tzname = strdup(VPCMIDO_DEFAULT_TZ);
    }
    char *tztype = NULL;
    if (!type || (strcmp(type, "gre") && strcmp(type, "vxlan"))) {
        tztype = strdup("gre");
    } else {
        tztype = strdup(type);
    }
    tz = mido_create_tunnelzone(tzname, tztype, &midotz);
    EUCA_FREE(tzname);
    EUCA_FREE(tztype);
    
    log_params_set(EUCA_LOG_INFO, 0, 100000);

    rc = mido_get_hosts(&tzhosts, &max_tzhosts);
    if (!rc) {
        for (i = 0; i < max_tzhosts; i++) {
            u32 addr = 0;
            rc = mido_get_address(tzhosts[i], dev, &addr);
            if (!rc) {
                char *ipAddress = hex2dot_s(addr);
                midoname *tzmember = NULL;
                LOGINFO("\tadding %s %s\n", tzhosts[i]->name, ipAddress);
                mido_create_tunnelzone_member(tz, NULL, tzhosts[i], ipAddress, &tzmember);
            }
        }
    }
    EUCA_FREE(tzhosts);

    if (tzs) {
        EUCA_FREE(tzs);
    }
    return (0);
}

/**
 * Use for VPCMIDO test. Should be left empty once tests are done.
 * @param mido [in] data structure that holds MidoNet configuration
 */
void do_midonet_test(mido_config *mido) {

}

/**
 * Tag objects in MidoNet that are referred by VPCMIDO models.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return always 0.
 */
int do_midonet_tag_midonames(mido_config *mido) {
    if (!mido) {
        return 0;
    }
    
    for (int i = 0; i < mido->max_vpcs; i++) {
        mido_vpc *vpc = &(mido->vpcs[i]);
        for (int j = 0; j < VPC_END; j++) {
            if (vpc->midos[j]) vpc->midos[j]->tag = 1;
        }
        for (int j = 0; j < vpc->max_subnets; j++) {
            mido_vpc_subnet *subnet = &(vpc->subnets[j]);
            for (int k = 0; k < SUBN_END; k++) {
                if (subnet->midos[k]) subnet->midos[k]->tag = 1;
            }
            for (int k = 0; k < subnet->max_instances; k++) {
                mido_vpc_instance *ifc = &(subnet->instances[k]);
                for (int l = 0; l < INST_END; l++) {
                    if (ifc->midos[l]) ifc->midos[l]->tag = 1;
                }
            }
            for (int k = 0; k < subnet->max_natgateways; k++) {
                mido_vpc_natgateway *natg = &(subnet->natgateways[k]);
                for (int l = 0; l < NATG_END; l++) {
                    if (natg->midos[l]) natg->midos[l]->tag = 1;
                }
            }
        }
        for (int j = 0; j < vpc->max_nacls; j++) {
            mido_vpc_nacl *nacl = &(vpc->nacls[j]);
            for (int k= 0; k < VPCNACL_END; k++) {
                if (nacl->midos[k]) nacl->midos[k]->tag = 1;
            }
        }
    }
    for (int i = 0; i < mido->max_vpcsecgroups; i++) {
        mido_vpc_secgroup *sg = &(mido->vpcsecgroups[i]);
        for (int j = 0; j < VPCSG_END; j++) {
            if (sg->midos[j]) sg->midos[j]->tag = 1;
        }
    }
    for (int i = 0; i < CORE_END; i++) {
        if (mido->midocore->midos[i]) mido->midocore->midos[i]->tag = 1;
    }
    for (int i = 0; mido->midomd && i < MD_END; i++) {
        if (mido->midomd->midos[i]) mido->midomd->midos[i]->tag = 1;
    }
    return (0);
}

/**
 * Clean VPC subnets' arp_table and mac_table entries.
 * @param mido [in] data structure that holds MidoNet configuration
 * @return 0 on success. Positive integer on any error.
 */
int do_midonet_clean_arpmactables(mido_config *mido) {
    if (!mido) {
        LOGWARN("cannot clean arp_ and mac_table of NULL mido config\n");
        return (1);
    }
    
    for (int i = 0; i < mido->max_vpcs; i++) {
        mido_vpc *vpc = &(mido->vpcs[i]);
        if (strlen(vpc->name) == 0) {
            continue;
        }
        for (int j = 0; j < vpc->max_subnets; j++) {
            mido_vpc_subnet *vpcsubnet = &(vpc->subnets[j]);
            if (strlen(vpcsubnet->name) == 0) {
                continue;
            }
            midonet_api_bridge *br = vpcsubnet->subnetbr;
            if (!br) {
                continue;
            }
            int max_macport_pairs = br->max_macport_pairs;
            for (int k = 0; k < max_macport_pairs; k++) {
                midoname *macport = br->macport_pairs[k];
                if (!macport) {
                    continue;
                }
                mido_delete_macport(br, macport);
            }
            int max_ip4mac_pairs = br->max_ip4mac_pairs;
            midonet_api_dhcp *dhcp = NULL;
            if (br->dhcps) {
                dhcp = br->dhcps[0];
            }
            if (!dhcp) {
                continue;
            }
            char *netaddrstr = strdup(dhcp->obj->uuid);
            char *tmpchar = strchr(netaddrstr, '_');
            if (!tmpchar) {
                EUCA_FREE(netaddrstr);
                continue;
            }
            *tmpchar = '\0';
            u32 netaddr = dot2hex(netaddrstr);
            EUCA_FREE(netaddrstr);
            char *dot2 = hex2dot(netaddr + 2);
            for (int k = 0; k < max_ip4mac_pairs; k++) {
                midoname *ip4mac = br->ip4mac_pairs[k];
                if (!ip4mac) {
                    continue;
                }
                if (strcmp(dot2, ip4mac->ip4mac->ip)) {
                    mido_delete_ip4mac(br, ip4mac);
                }
            }
            EUCA_FREE(dot2);
        }
    }
    return (0);
}

/**
 * Replaces all occurrences of character f in string str with the character r.
 * @param str [in] the string of interest
 * @param f [in] character to find in the string str
 * @param r [in] character to be used as a replacement
 * @return a newly allocated string with character replacements. Caller is responsible
 * for releasing the allocated memory.
 */
char *replace_char(char *str, char f, char r) {
    if (!str) {
        return NULL;
    }
    char *res = strdup(str);
    if (f == r) {
        return (res);
    }
    int l = strlen(res);
    for (int i = 0; i < l; i++) {
        if (res[i] == f) {
            res[i] = r;
        }
    }
    return(res);
}

/**
 * Splits the input string cidr that represents a CIDR block to subnet and slashnet
 * parts. Plusone and plustwo addresses are also computed. Buffer of the output
 * variables are assumed to be allocated by the caller.
 * @param cidr [in] string representing a CIDR block (subnet/mask)
 * @param outnet [out] extracted network address (optional)
 * @param outslashnet [out] extracted network mask (optional)
 * @param outgw [out] extracted gateway (plusone) address (optional)
 * @param outplustwo [out] extracted plustwo address (optional)
 * @param outplusthree [out] extracted plusthree address (optional)
 * @return 0 on success. 1 on failure.
 */
int cidr_split(char *cidr, char *outnet, char *outslashnet, char *outgw, char *outplustwo, char *outplusthree) {
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
        if (outgw) {
            gw = nw + 1;
            tok = hex2dot(gw);
            snprintf(outgw, strlen(tok) + 1, "%s", tok);
            EUCA_FREE(tok);
        }

        if (outplustwo) {
            gw = nw + 2;
            tok = hex2dot(gw);
            snprintf(outplustwo, strlen(tok) + 1, "%s", tok);
            EUCA_FREE(tok);
        }

        if (outplusthree) {
            gw = nw + 3;
            tok = hex2dot(gw);
            snprintf(outplusthree, strlen(tok) + 1, "%s", tok);
            EUCA_FREE(tok);
        }
    }

    return (0);
}

/**
 * Checks if the IP address in the argument is a MidoNet VPC subnet +2 address.
 * For example, if subnets 172.16.0.0/20 and 172.16.16.0/20 are valid subnets in
 * MidoNet configuration, this function will return 0 iff the argument IP address
 * is 172.16.0.2 or 172.16.16.2
 *
 * @param mido [in] - pointer to MidoNet configuration data structure.
 * @param iptocheck [in] - IP address to check.
 *
 * @return 0 iff the argument IP address matches a MidoNet VPC subnetwork address
 *         +2. Returns a value != 0 otherwise.
 */
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
                rc = cidr_split(vpcsubnet->gniSubnet->cidr, NULL, NULL, NULL, tmpip, NULL);
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

/**
 * Parses the route entry target string in the argument and translates into one
 * of valid vpc_route_entry_target_t enumeration value.
 *
 * @param target [in] - string representation of route entry target.
 *
 * @return a valid vpc_route_entry_target_t enumeration value (which includes
 *         VPC_TARGET_INVALID in case of an invalid argument.
 */
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

/**
 * Compare data describing gateways. gnigw holds information extracted from GNI, while
 * midogw holds information populated from MN. Gateway host is assumed to match.
 * BGP parameters are also ignored.
 * @param gnigw [in] gateway information extracted from GNI
 * @param midogw [in] gateway information populated from MN
 * @return 0 if all parameters match. 
 */
int cmp_gnigw_midogw(gni_mido_gateway *gnigw, mido_gw *midogw) {
    int match = 1;
    if (!gnigw && !midogw) {
        return (match);
    }
    if (!gnigw || !midogw) {
        match = 0;
        return (match);
    }
    if (match && strcmp(gnigw->ext_ip, midogw->ext_ip)) {
        match = 0;
    }
    if (match && strcmp(gnigw->ext_dev, midogw->ext_dev)) {
        match = 0;
    }
    if (match && strcmp(gnigw->ext_cidr, midogw->ext_cidr)) {
        match = 0;
    }
    if (match && strcmp(gnigw->peer_ip, midogw->peer_ip)) {
        match = 0;
    }
    return (match);
}


/*  LocalWords:  sgrulepos
 */
