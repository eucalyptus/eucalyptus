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
#include <curl/curl.h>
#include <json/json.h>

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
#include <log.h>

#include "globalnetwork.h"
#include "midonet-api.h"

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

void mido_print_midoname(midoname * name)
{
    //    printf("init=%d tenant=%s name=%s uuid=%s resource_type=%s content_type=%s jsonbuf=%s\n", name->init, SP(name->tenant), SP(name->name), SP(name->uuid), SP(name->resource_type), SP(name->content_type), SP(name->jsonbuf));
    LOGDEBUG("init=%d tenant=%s name=%s uuid=%s resource_type=%s content_type=%s\n", name->init, SP(name->tenant), SP(name->name), SP(name->uuid), SP(name->resource_type),
             SP(name->content_type));
}

void mido_free_midoname_list(midoname * name, int max_name)
{
    int i = 0;
    //    return;
    for (i = 0; i < max_name; i++) {
        mido_free_midoname(&(name[i]));
    }
}

void mido_free_midoname(midoname * name)
{

    if (!name) {
        return;
    }
    EUCA_FREE(name->name);
    EUCA_FREE(name->uuid);
    EUCA_FREE(name->tenant);
    EUCA_FREE(name->jsonbuf);
    EUCA_FREE(name->resource_type);
    EUCA_FREE(name->content_type);
    bzero(name, sizeof(midoname));
}

int mido_getel_midoname(midoname * name, char *key, char **val)
{
    int ret = 0;
    json_object *jobj = NULL;

    if (!name || !key || !val) {
        return (1);
    }

    *val = NULL;
    jobj = json_tokener_parse(name->jsonbuf);
    if (jobj) {
        json_object_object_foreach(jobj, elkey, elval) {
            if (!*val && elkey && elval) {
                if (!strcmp(elkey, key)) {
                    *val = strdup(SP(json_object_get_string(elval)));
                }
            }
        }
        json_object_put(jobj);
    }

    if (*val == NULL) {
        ret = 1;
    }

    return (ret);
}

int mido_create_router(char *tenant, char *name, midoname * outname)
{
    int rc;
    midoname myname;

    bzero(&myname, sizeof(midoname));
    myname.tenant = strdup(tenant);
    myname.name = strdup(name);
    myname.resource_type = strdup("routers");
    myname.content_type = strdup("Router");

    rc = mido_create_resource(NULL, 0, &myname, outname, "name", myname.name, NULL);

    mido_free_midoname(&myname);
    //    mido_free_midoname(&parentname);
    return (rc);
}

int mido_read_router(midoname * name)
{
    return (mido_read_resource("routers", name));
}

int mido_update_router(midoname * name, ...)
{
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource("routers", "Router", name, &al);
    va_end(al);

    return (ret);
}

int mido_print_router(midoname * name)
{
    return (mido_print_resource("routers", name));
}

int mido_delete_router(midoname * name)
{
    return (mido_delete_resource(NULL, name));
}

int mido_create_bridge(char *tenant, char *name, midoname * outname)
{
    int rc;
    midoname myname;

    /*
       midoname parentname = {0,0,0,0,0};

       parentname.tenant = strdup(tenant);
       parentname.name = strdup(tenant);
       parentname.uuid = strdup(tenant);
       parentname.resource_type = strdup("tenants");
       parentname.content_type = strdup("Tenant");
       parentname.jsonbuf = NULL;
     */
    bzero(&myname, sizeof(midoname));
    myname.tenant = strdup(tenant);
    myname.name = strdup(name);
    myname.resource_type = strdup("bridges");
    myname.content_type = strdup("Bridge");

    rc = mido_create_resource(NULL, 0, &myname, outname, "name", myname.name, NULL);

    mido_free_midoname(&myname);
    return (rc);
}

int mido_read_bridge(midoname * name)
{
    return (mido_read_resource("bridges", name));
}

int mido_update_bridge(midoname * name, ...)
{
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource("bridges", "Bridge", name, &al);
    va_end(al);

    return (ret);
}

int mido_print_bridge(midoname * name)
{
    return (mido_print_resource("bridges", name));
}

int mido_delete_bridge(midoname * name)
{
    return (mido_delete_resource(NULL, name));
}

int mido_create_ipaddrgroup(char *tenant, char *name, midoname * outname)
{
    int rc = 0, max_iags = 0, found = 0, i;
    midoname myname, *iags = NULL;

    bzero(&myname, sizeof(midoname));
    myname.tenant = strdup(tenant);
    myname.resource_type = strdup("ip_addr_groups");
    myname.content_type = strdup("IpAddrGroup");

    // only create if it doesn't already exist
    rc = mido_get_resources(NULL, 0, myname.tenant, "ip_addr_groups", &iags, &max_iags);
    if (!rc) {
        found = 0;
        for (i = 0; i < max_iags && !found; i++) {
            rc = mido_cmp_midoname_to_input(&(iags[i]), "name", name, NULL);
            if (!rc) {
                if (outname) {
                    mido_copy_midoname(outname, &(iags[i]));
                }
                found = 1;
            }
        }
    }
    if (iags && max_iags > 0) {
        mido_free_midoname_list(iags, max_iags);
        EUCA_FREE(iags);
    }

    if (!found) {
        rc = mido_create_resource(NULL, 0, &myname, outname, "name", name, NULL);
    } else {
        rc = 0;
    }

    mido_free_midoname(&myname);
    return (rc);
}

int mido_read_ipaddrgroup(midoname * name)
{
    return (mido_read_resource("ip_addr_groups", name));
}

int mido_update_ipaddrgroup(midoname * name, ...)
{
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource("ip_addr_groups", "IpAddrGroup", name, &al);
    va_end(al);

    return (ret);
}

int mido_delete_ipaddrgroup(midoname * name)
{
    return (mido_delete_resource(NULL, name));
}

int mido_print_ipaddrgroup(midoname * name)
{
    int ret = 0;
    return (ret);
}

int mido_get_ipaddrgroups(char *tenant, midoname ** outnames, int *outnames_max)
{
    return (mido_get_resources(NULL, 0, tenant, "ip_addr_groups", outnames, outnames_max));
}

int mido_create_dhcp(midoname * devname, char *subnet, char *slashnet, char *gw, char *dns, midoname * outname)
{
    int rc;
    midoname myname;

    bzero(&myname, sizeof(midoname));
    myname.tenant = strdup(devname->tenant);
    myname.resource_type = strdup("dhcp");
    myname.content_type = strdup("DhcpSubnet");

    rc = mido_create_resource(devname, 1, &myname, outname, "subnetPrefix", subnet, "subnetLength", slashnet, "defaultGateway", gw, "dnsServerAddrs", "jsonarr", "dnsServerAddrs:",
                              dns, "dnsServerAddrs:END", "END", NULL);

    mido_free_midoname(&myname);
    return (rc);
}

int mido_read_dhcp(midoname * name)
{
    return (mido_read_resource("dhcp", name));
}

int mido_update_dhcp(midoname * name, ...)
{
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource("dhcp", "DhcpSubnet", name, &al);
    va_end(al);

    return (ret);
}

int mido_print_dhcp(midoname * name)
{
    return (mido_print_resource("dhcp", name));
}

int mido_delete_dhcp(midoname * devname, midoname * name)
{
    return (mido_delete_resource(devname, name));
}

int mido_get_dhcps(midoname * devname, midoname ** outnames, int *outnames_max)
{
    return (mido_get_resources(devname, 1, devname->tenant, "dhcp", outnames, outnames_max));
}

int mido_get_dhcphosts(midoname * devname, midoname * dhcp, midoname ** outnames, int *outnames_max)
{
    int rc = 0;
    midoname *parents = NULL;
    parents = calloc(2, sizeof(midoname));

    mido_copy_midoname(&(parents[0]), devname);
    mido_copy_midoname(&(parents[1]), dhcp);
    rc = mido_get_resources(parents, 2, devname->tenant, "hosts", outnames, outnames_max);
    mido_free_midoname_list(parents, 2);
    EUCA_FREE(parents);

    return (rc);
}

int mido_create_dhcphost(midoname * devname, midoname * dhcp, char *name, char *mac, char *ip, midoname * outname)
{
    int rc = 0, ret = 0, max_dhcphosts = 0, found = 0, i = 0;
    midoname myname;
    midoname *parents = NULL, *dhcphosts = NULL;

    bzero(&myname, sizeof(midoname));
    myname.name = strdup(name);
    myname.tenant = strdup(devname->tenant);
    myname.resource_type = strdup("hosts");
    myname.content_type = strdup("DhcpHost");

    parents = calloc(2, sizeof(midoname));

    mido_copy_midoname(&(parents[0]), devname);
    mido_copy_midoname(&(parents[1]), dhcp);

    // check if host already has a rule in place
    rc = mido_get_resources(parents, 2, myname.tenant, "hosts", &dhcphosts, &max_dhcphosts);
    if (!rc) {
        found = 0;
        for (i = 0; i < max_dhcphosts && !found; i++) {
            rc = mido_cmp_midoname_to_input(&(dhcphosts[i]), "macAddr", mac, "ipAddr", ip, NULL);
            if (!rc) {
                LOGTRACE("ALREADY EXISTS: dhcp host %s/%s\n", SP(mac), SP(ip));
                if (outname) {
                    mido_copy_midoname(outname, &(dhcphosts[i]));
                }
                found = 1;
            }
        }
    }
    mido_free_midoname_list(dhcphosts, max_dhcphosts);
    EUCA_FREE(dhcphosts);

    if (!found) {
        rc = mido_create_resource(parents, 2, &myname, outname, "name", myname.name, "macAddr", mac, "ipAddr", ip, NULL);
        if (rc) {
            ret = 1;
        }
    }

    mido_free_midoname(&(parents[0]));
    mido_free_midoname(&(parents[1]));
    mido_free_midoname(&myname);
    EUCA_FREE(parents);
    return (ret);
}

int mido_delete_dhcphost(midoname * name)
{
    return (mido_delete_resource(NULL, name));
}

int mido_create_chain(char *tenant, char *name, midoname * outname)
{
    int rc, max_chains, found = 0, i;
    midoname myname, *chains = NULL;

    bzero(&myname, sizeof(midoname));
    myname.tenant = strdup(tenant);
    myname.name = strdup(name);
    myname.resource_type = strdup("chains");
    myname.content_type = strdup("Chain");

    // check if host already has a rule in place
    rc = mido_get_resources(NULL, 0, myname.tenant, "chains", &chains, &max_chains);
    if (!rc) {
        found = 0;
        for (i = 0; i < max_chains && !found; i++) {
            rc = mido_cmp_midoname_to_input(&(chains[i]), "name", name, NULL);
            if (!rc) {
                LOGTRACE("ALREADY EXISTS: chain %s\n", SP(name));
                if (outname) {
                    mido_copy_midoname(outname, &(chains[i]));
                }
                found = 1;
            }
        }
    }
    mido_free_midoname_list(chains, max_chains);
    EUCA_FREE(chains);

    rc = 0;
    if (!found) {
        rc = mido_create_resource(NULL, 0, &myname, outname, "name", myname.name, NULL);
    }

    mido_free_midoname(&myname);
    return (rc);
}

int mido_read_chain(midoname * name)
{
    return (mido_read_resource("chains", name));
}

int mido_update_chain(midoname * name, ...)
{
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource("chains", "Chain", name, &al);
    va_end(al);

    return (ret);
}

int mido_print_chain(midoname * name)
{
    return (mido_print_resource("chains", name));
}

int mido_delete_chain(midoname * name)
{
    return (mido_delete_resource(NULL, name));
}

int mido_create_ipaddrgroup_ip(midoname * ipaddrgroup, char *ip, midoname * outname)
{
    int rc = 0, ret = 0, max_ips = 0, found = 0, i = 0;
    midoname myname, *ips = NULL;

    bzero(&myname, sizeof(midoname));

    myname.tenant = strdup(ipaddrgroup->tenant);
    myname.resource_type = strdup("ip_addrs");
    myname.content_type = strdup("IpAddrGroupAddr");

    // check if host already has a rule in place
    rc = mido_get_resources(ipaddrgroup, 1, myname.tenant, "ip_addrs", &ips, &max_ips);
    if (!rc) {
        found = 0;
        for (i = 0; i < max_ips && !found; i++) {
            rc = mido_cmp_midoname_to_input(&(ips[i]), "addr", ip, NULL);
            if (!rc) {
                if (outname) {
                    mido_copy_midoname(outname, &(ips[i]));
                }
                found = 1;
            }
        }
    }
    if (ips && (max_ips > 0)) {
        mido_free_midoname_list(ips, max_ips);
        EUCA_FREE(ips);
    }

    if (!found) {
        rc = mido_create_resource(ipaddrgroup, 1, &myname, outname, "addr", ip, "version", "4", NULL);
        if (rc) {
            ret = 1;
        }
    }

    mido_free_midoname(&myname);
    return (ret);
}

int mido_delete_ipaddrgroup_ip(midoname * ipaddrgroup, midoname * name)
{
    return (mido_delete_resource(ipaddrgroup, name));
}

int mido_get_ipaddrgroup_ips(midoname * ipaddrgroup, midoname ** outnames, int *outnames_max)
{
    return (mido_get_resources(ipaddrgroup, 1, ipaddrgroup->tenant, "ip_addrs", outnames, outnames_max));
}

/*
int mido_allocate_midorule(char *position, char *type, char *action, char *protocol, char *srcIAGuuid, char *src_port_min,  char *src_port_max, char *dstIAGuuid, char *dst_port_min, char *dst_port_max, char *matchForwardFlow, char *matchReturnFlow, char *nat_target, char *nat_port_min, char *nat_port_max, midorule *outrule) {
    int ret=0;
    if (!outrule) {
        return(1);
    }
    
    bzero(outrule, sizeof(midorule));

    if (position) snprintf(outrule->position, sizeof(outrule->position), "%s", position);
    if (type) snprintf(outrule->type, sizeof(outrule->type), "%s", type);
    if (action) snprintf(outrule->action, sizeof(outrule->action), "%s", action);
    if (protocol) snprintf(outrule->protocol, sizeof(outrule->protocol), "%s", protocol);
    if (srcIAGuuid) snprintf(outrule->srcIAGuuid, sizeof(outrule->srcIAGuuid), "%s", srcIAGuuid);
    if (src_port_min) snprintf(outrule->src_port_min, sizeof(outrule->src_port_min), "%s", src_port_min);
    if (src_port_max) snprintf(outrule->src_port_max, sizeof(outrule->src_port_max), "%s", src_port_max);
    if (dstIAGuuid) snprintf(outrule->dstIAGuuid, sizeof(outrule->dstIAGuuid), "%s", dstIAGuuid);
    if (dst_port_min) snprintf(outrule->dst_port_min, sizeof(outrule->dst_port_min), "%s", dst_port_min);
    if (dst_port_max) snprintf(outrule->dst_port_max, sizeof(outrule->dst_port_max), "%s", dst_port_max);
    if (matchForwardFlow) snprintf(outrule->matchForwardFlow, sizeof(outrule->matchForwardFlow), "%s", matchForwardFlow);
    if (matchReturnFlow) snprintf(outrule->matchReturnFlow, sizeof(outrule->matchReturnFlow), "%s", matchReturnFlow);
    if (nat_target) snprintf(outrule->nat_target, sizeof(outrule->nat_target), "%s", nat_target);
    if (nat_port_min) snprintf(outrule->nat_port_min, sizeof(outrule->nat_port_min), "%s", nat_port_min);
    if (nat_port_max) snprintf(outrule->nat_port_max, sizeof(outrule->nat_port_max), "%s", nat_port_max);
    
    return(ret);
}
*/
int mido_create_rule(midoname * chain, midoname * outname, ...)
{
    int rc = 0, ret = 0, max_rules = 0, found = 0, i = 0;
    midoname myname, *rules = NULL;
    va_list ap = { {0} }, ap1 = { {
    0}}, ap2 = { {
    0}};

    va_start(ap, outname);
    va_copy(ap1, ap);
    va_copy(ap2, ap);

    bzero(&myname, sizeof(midoname));

    myname.tenant = strdup(chain->tenant);
    myname.resource_type = strdup("rules");
    myname.content_type = strdup("Rule");

    // check if host already has a rule in place
    rc = mido_get_resources(chain, 1, myname.tenant, "rules", &rules, &max_rules);
    if (!rc) {
        found = 0;
        for (i = 0; i < max_rules && !found; i++) {
            rc = mido_cmp_midoname_to_input_json_v(&(rules[i]), &ap1);
            va_end(ap1);
            if (!rc) {
                if (outname) {
                    mido_copy_midoname(outname, &(rules[i]));
                }
                found = 1;
            }
        }
    }
    mido_free_midoname_list(rules, max_rules);
    EUCA_FREE(rules);

    LOGTRACE("Rule FOUND?: %d\n", found);
    if (!found) {
        rc = mido_create_resource_v(chain, 1, &myname, outname, &ap2);
        va_end(ap2);
        if (rc) {
            ret = 1;
        }
    }

    mido_free_midoname(&myname);
    va_end(ap);
    return (ret);
}

/*
int mido_create_rule_v1(midoname *chain, midorule *rule, midoname *outname) {
    int rc=0, ret=0, max_rules=0, found=0, i=0;
    midoname myname, *rules=NULL;
    
    bzero(&myname, sizeof(midoname));

    myname.tenant = strdup(chain->tenant);
    myname.resource_type = strdup("rules");
    myname.content_type = strdup("Rule");

    // check if host already has a rule in place
    rc = mido_get_resources(chain, 1, myname.tenant, "rules", &rules, &max_rules);
    if (!rc) {
        found=0;
        for (i=0; i<max_rules && !found; i++) {
            if (!strcmp(rule->type, "jump")) {
                rc = mido_cmp_midoname_to_input_json(&(rules[i]), "type", rule->type, "jumpChainId", rule->action, NULL);
            } else {
                rc = mido_cmp_midoname_to_input_json(&(rules[i]), "type", rule->type, "flowAction", rule->action, "position", "UNSET", "matchForwardFlow", rule->matchForwardFlow, "matchReturnFlow", rule->matchReturnFlow, "ipAddrGroupSrc", rule->srcIAGuuid, "ipAddrGroupDst", rule->dstIAGuuid, "natTargets", "jsonlist", "natTargets:addressTo", rule->nat_target, "natTargets:addressFrom", rule->nat_target, "natTargets:portTo", rule->nat_port_max, "natTargets:portFrom", rule->nat_port_min, "natTargets:END", "END", "tpDst", "jsonjson", "tpDst:start", rule->dst_port_min, "tpDst:end", rule->dst_port_max, "tpDst:END", "END", "tpSrc", "jsonjson", "tpSrc:start", rule->src_port_min, "tpSrc:end", rule->src_port_max, "tpSrc:END", "END", NULL);
            }
            if (!rc) {
                if (outname) {
                    mido_copy_midoname(outname, &(rules[i]));
                }
                found=1;
            }
        }
    }
    mido_free_midoname_list(rules, max_rules);
    EUCA_FREE(rules);
    
    LOGTRACE("FOUND?: %d\n", found);
    if (!found) {    
        if (!strcmp(rule->type, "jump")) {
            rc = mido_create_resource(chain, 1, &myname, outname, "type", rule->type, "jumpChainId", rule->action, "position", rule->position, NULL);
        } else {
            rc = mido_create_resource(chain, 1, &myname, outname, "type", rule->type, "flowAction", rule->action, "position", rule->position, "matchForwardFlow", rule->matchForwardFlow, "matchReturnFlow", rule->matchReturnFlow,"ipAddrGroupSrc", rule->srcIAGuuid, "ipAddrGroupDst", rule->dstIAGuuid, "natTargets", "jsonlist", "natTargets:addressTo", rule->nat_target, "natTargets:addressFrom", rule->nat_target, "natTargets:portTo", rule->nat_port_max, "natTargets:portFrom", rule->nat_port_min, "natTargets:END", "END", "tpDst", "jsonjson", "tpDst:start", rule->dst_port_min, "tpDst:end", rule->dst_port_max, "tpDst:END", "END", "tpSrc", "jsonjson", "tpSrc:start", rule->src_port_min, "tpSrc:end", rule->src_port_max, "tpSrc:END", "END", NULL);
        }
        if (rc) {
            ret=1;
        }
    }

    mido_free_midoname(&myname);
    return(ret);
}
*/
int mido_read_rule(midoname * name)
{
    return (mido_read_resource("ports", name));
}

int mido_update_rule(midoname * name, ...)
{
    return (0);
}

int mido_print_rule(midoname * name)
{
    return (mido_print_resource("ports", name));
}

int mido_delete_rule(midoname * name)
{
    return (mido_delete_resource(NULL, name));
}

int mido_create_port(midoname * devname, char *port_type, char *ip, char *nw, char *slashnet, midoname * outname)
{
    //int mido_create_port(midoname *devname, char *port_type, midoname *outname) {
    int rc;
    midoname myname;

    bzero(&myname, sizeof(midoname));

    myname.tenant = strdup(devname->tenant);
    myname.resource_type = strdup("ports");
    myname.content_type = strdup("Port");

    //{"type":"InteriorRouter","portAddress":"1.2.3.4","networkAddress":"1.2.3.0","networkLength":"24","tenantId":"euca_tenant_0"}
    if (ip && nw && slashnet) {
        rc = mido_create_resource(devname, 1, &myname, outname, "type", port_type, "portAddress", ip, "networkAddress", nw, "networkLength", slashnet, NULL);
    } else {
        rc = mido_create_resource(devname, 1, &myname, outname, "type", port_type, NULL);
    }

    mido_free_midoname(&myname);
    return (rc);
}

int mido_read_port(midoname * name)
{
    return (mido_read_resource("ports", name));
}

int mido_update_port(midoname * name, ...)
{
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource("ports", "Port", name, &al);
    va_end(al);

    return (ret);
}

int mido_print_port(midoname * name)
{
    return (mido_print_resource("ports", name));
}

int mido_delete_port(midoname * name)
{
    return (mido_delete_resource(NULL, name));
}

int mido_get_ports(midoname * devname, midoname ** outnames, int *outnames_max)
{
    return (mido_get_resources(devname, 1, devname->tenant, "ports", outnames, outnames_max));
}

int mido_get_rules(midoname * chainname, midoname ** outnames, int *outnames_max)
{
    return (mido_get_resources(chainname, 1, chainname->tenant, "rules", outnames, outnames_max));
}

int mido_unlink_host_port(midoname * host, midoname * port)
{
    int rc = 0, ret = 0;
    char url[EUCA_MAX_PATH];

    snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/hosts/%s/ports/%s", host->uuid, port->uuid);
    rc = midonet_http_delete(url);
    if (rc) {
        ret = 1;
    }
    return (ret);
}

int mido_link_host_port(midoname * host, char *interface, midoname * device, midoname * port)
{

    int rc = 0, ret = 0, found = 0;
    midoname myname;
    char *hinterface = NULL;

    bzero(&myname, sizeof(midoname));

    myname.tenant = strdup(device->tenant);
    myname.name = strdup("port");
    myname.resource_type = strdup("ports");
    myname.content_type = NULL;

    // check to see if the port is already mapped
    rc = mido_getel_midoname(port, "hostInterfacePort", &hinterface);
    if (!rc) {
    }
    EUCA_FREE(hinterface);

    if (!found) {
        rc = mido_create_resource(host, 1, &myname, NULL, "bridgeId", device->uuid, "portId", port->uuid, "hostId", host->uuid, "interfaceName", interface, NULL);
        if (rc) {
            ret = 1;
        }
    }

    mido_free_midoname(&myname);

    return (ret);
}

int mido_link_ports(midoname * a, midoname * b)
{
    int rc = 0, found = 0, ret = 0;
    midoname myname;
    char *asideval = NULL, *bsideval = NULL;
    bzero(&myname, sizeof(midoname));

    myname.tenant = strdup(a->tenant);
    myname.name = strdup("link");
    myname.resource_type = strdup("link");
    myname.content_type = NULL;

    // check to see if link already exists before making new link
    if (!mido_getel_midoname(a, "peer", &asideval) && !mido_getel_midoname(b, "peer", &bsideval)) {
        if (strstr(asideval, b->uuid) && strstr(bsideval, a->uuid)) {
            LOGTRACE("ALREADY EXISTS: link from port %s to port %s\n", SP(a->uuid), SP(b->uuid));
            found = 1;
        }
    }
    EUCA_FREE(asideval);
    EUCA_FREE(bsideval);

    if (!found) {
        rc = mido_create_resource(a, 1, &myname, NULL, "peerId", b->uuid, NULL);
        if (rc) {
            ret = 1;
        }
    }

    mido_free_midoname(&myname);

    return (ret);
}

int mido_update_resource(char *resource_type, char *content_type, midoname * name, va_list * al)
{
    char url[EUCA_MAX_PATH];
    int rc = 0, ret = 0;
    char *key = NULL, *val = NULL;
    struct json_object *jobj = NULL, *el = NULL;

    jobj = json_tokener_parse(name->jsonbuf);
    if (jobj) {
        key = va_arg(*al, char *);
        if (key)
            val = va_arg(*al, char *);
        while (key && val) {
            el = json_object_object_get(jobj, key);
            if (el) {
                json_object_object_add(jobj, key, json_object_new_string(val));
                //                json_object_put(el);
            } else {
                json_object_object_add(jobj, key, json_object_new_string(val));
            }
            key = va_arg(*al, char *);
            if (key)
                val = va_arg(*al, char *);
        }

        EUCA_FREE(name->jsonbuf);
        name->jsonbuf = strdup(json_object_to_json_string(jobj));

        json_object_put(jobj);
        ret = mido_update_midoname(name);
    } else {
        printf("ERROR: json_tokener_parse(...): returned NULL\n");
        ret = 1;
    }

    // ready to send the http_put
    if (!ret) {
        snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s/%s", resource_type, name->uuid);
        rc = midonet_http_put(url, content_type, name->jsonbuf);
        if (rc) {
            ret = 1;
        }
    }
    return (ret);
}

int mido_read_resource(char *resource_type, midoname * name)
{
    char url[EUCA_MAX_PATH], *outhttp = NULL;
    int rc = 0, ret = 0;

    snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s/%s", resource_type, name->uuid);
    rc = midonet_http_get(url, &outhttp);
    if (rc) {
        ret = 1;
    } else {
        EUCA_FREE(name->jsonbuf);
        name->jsonbuf = strdup(outhttp);
        ret = mido_update_midoname(name);
    }
    EUCA_FREE(outhttp);
    return (ret);
}

int mido_print_resource(char *resource_type, midoname * name)
{
    int ret = 0;
    struct json_object *jobj = NULL;

    jobj = json_tokener_parse(name->jsonbuf);
    if (!jobj) {
        printf("ERROR: json_tokener_parse(...): returned NULL\n");
        ret = 1;
    } else {
        printf("TYPE: %s NAME: %s UUID: %s\n", resource_type, SP(name->name), name->uuid);
        json_object_object_foreach(jobj, key, val) {
            printf("\t%s: %s\n", key, SP(json_object_get_string(val)));
        }
        json_object_put(jobj);
    }

    return (ret);
}

char *mido_jsonize(char *tenant, va_list * al)
{
    char *payload = NULL;
    struct json_object *jobj = NULL, *jobj_sublist = NULL, *jarr_sublist = NULL;
    char *key = NULL, *val = NULL, *listobjtag = NULL, *listarrtag = NULL, *listjsontag = NULL;
    int listobjtag_count = 0, listarrtag_count = 0, listjsontag_count = 0;

    jobj = json_object_new_object();
    if (!jobj) {
        printf("ERROR: json_object_new_object(...): returned NULL\n");
        payload = NULL;
    } else {
        if (tenant) {
            json_object_object_add(jobj, "tenantId", json_object_new_string(tenant));
        }
        key = va_arg(*al, char *);
        if (key)
            val = va_arg(*al, char *);
        while (key && val) {
            //            LOGTRACE("HERE: %s/%s\n", key, val);
            if (!strcmp(val, "UNSET")) {
            } else {
                if (!strcmp(val, "jsonlist")) {
                    EUCA_FREE(listobjtag);
                    EUCA_FREE(listarrtag);
                    EUCA_FREE(listjsontag);
                    listobjtag = strdup(key);
                    listobjtag_count = 0;
                    jobj_sublist = json_object_new_object();
                } else if (!strcmp(val, "jsonjson")) {
                    EUCA_FREE(listobjtag);
                    EUCA_FREE(listarrtag);
                    EUCA_FREE(listjsontag);
                    listjsontag = strdup(key);
                    listjsontag_count = 0;
                    jobj_sublist = json_object_new_object();
                } else if (!strcmp(val, "jsonarr")) {
                    EUCA_FREE(listobjtag);
                    EUCA_FREE(listarrtag);
                    EUCA_FREE(listjsontag);
                    listarrtag = strdup(key);
                    listarrtag_count = 0;
                    jobj_sublist = json_object_new_array();
                } else if ((listobjtag && strstr(key, listobjtag)) || (listjsontag && strstr(key, listjsontag))) {
                    char *subkey = NULL;
                    subkey = strchr(key, ':');
                    subkey++;
                    if (!strcmp(val, "END")) {
                        // add the thing
                        if (listjsontag) {
                            if (listjsontag_count) {
                                json_object_object_add(jobj, listjsontag, jobj_sublist);
                            }
                            EUCA_FREE(listjsontag);
                            listjsontag = NULL;
                        } else if (listobjtag) {
                            if (listobjtag_count) {
                                jarr_sublist = json_object_new_array();
                                json_object_array_add(jarr_sublist, jobj_sublist);
                                json_object_object_add(jobj, listobjtag, jarr_sublist);
                            }
                            EUCA_FREE(listobjtag);
                            listobjtag = NULL;
                        }
                    } else {
                        listjsontag_count++;
                        listobjtag_count++;
                        json_object_object_add(jobj_sublist, subkey, json_object_new_string(val));
                    }
                } else if (listarrtag && strstr(key, listarrtag)) {
                    if (!strcmp(val, "END")) {
                        if (listarrtag_count) {
                            json_object_object_add(jobj, listarrtag, jobj_sublist);
                        }
                        EUCA_FREE(listarrtag);
                        listarrtag = NULL;
                    } else {
                        listarrtag_count++;
                        json_object_array_add(jobj_sublist, json_object_new_string(val));
                    }
                } else {
                    if (listobjtag) {
                        jarr_sublist = json_object_new_array();
                        json_object_array_add(jarr_sublist, jobj_sublist);
                        json_object_object_add(jobj, listobjtag, jarr_sublist);
                        EUCA_FREE(listobjtag);
                        listobjtag = NULL;
                    } else if (listjsontag) {
                        json_object_object_add(jobj, listjsontag, jobj_sublist);
                        EUCA_FREE(listjsontag);
                        listjsontag = NULL;
                    } else if (listarrtag) {
                        json_object_object_add(jobj, listarrtag, jobj_sublist);
                        EUCA_FREE(listarrtag);
                        listarrtag = NULL;
                    }
                    json_object_object_add(jobj, key, json_object_new_string(val));
                }
            }
            key = va_arg(*al, char *);
            if (key)
                val = va_arg(*al, char *);
        }
        if (listobjtag) {
            jarr_sublist = json_object_new_array();
            json_object_array_add(jarr_sublist, jobj_sublist);
            json_object_object_add(jobj, listobjtag, jarr_sublist);
            EUCA_FREE(listobjtag);
            listobjtag = NULL;
        } else if (listjsontag) {
            json_object_object_add(jobj, listjsontag, jobj_sublist);
            EUCA_FREE(listjsontag);
            listjsontag = NULL;
        } else if (listarrtag) {
            json_object_object_add(jobj, listarrtag, jobj_sublist);
            EUCA_FREE(listarrtag);
            listarrtag = NULL;
        }
        //        printf("JSON: %s\n", json_object_to_json_string(jobj));
        payload = strdup(json_object_to_json_string(jobj));
        //        LOGTRACE("PAYLOAD: %s\n", payload);
        json_object_put(jobj);
    }
    return (payload);
}

int mido_create_resource(midoname * parents, int max_parents, midoname * newname, midoname * outname, ...)
{
    int ret = 0;
    va_list al;
    va_start(al, outname);
    ret = mido_create_resource_v(parents, max_parents, newname, outname, &al);
    va_end(al);
    return (ret);
}

int mido_create_resource_v(midoname * parents, int max_parents, midoname * newname, midoname * outname, va_list * al)
{
    int ret = 0, rc = 0;
    char url[EUCA_MAX_PATH];
    char *outloc = NULL, *outhttp = NULL, *payload = NULL;
    char tmpbuf[EUCA_MAX_PATH];
    int i;

    if (outname) {
        if (outname->init) {
            LOGTRACE("ALREADY EXISTS: %s/%s\n", outname->resource_type, outname->uuid);
            return (0);
            //            return(mido_read_resource(outname->resource_type, outname));
        }
        bzero(outname, sizeof(midoname));
    }
    //  construct the payload
    payload = mido_jsonize(newname->tenant, al);

    if (payload) {
        if (!parents) {
            snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s", newname->resource_type);
        } else {
            snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/");
            for (i = 0; i < max_parents; i++) {
                tmpbuf[0] = '\0';
                snprintf(tmpbuf, EUCA_MAX_PATH, "%s/%s/", parents[i].resource_type, parents[i].uuid);
                strcat(url, tmpbuf);
            }
            tmpbuf[0] = '\0';
            snprintf(tmpbuf, EUCA_MAX_PATH, "%s", newname->resource_type);
            strcat(url, tmpbuf);
        }

        // perform the create
        rc = midonet_http_post(url, newname->content_type, payload, &outloc);
        if (rc) {
            LOGERROR("midonet_http_post(%s, ...) failed\n", url);
            ret = 1;
        }
    } else {
        LOGERROR("could not generate payload\n");
        ret = 1;
    }

    // if all goes well, store the new resource
    if (!ret) {
        if (outname && outloc) {
            rc = midonet_http_get(outloc, &outhttp);
            if (rc) {
                ret = 1;
            } else {
                if (newname->tenant)
                    outname->tenant = strdup(newname->tenant);
                if (outhttp)
                    outname->jsonbuf = strdup(outhttp);
                if (newname->resource_type)
                    outname->resource_type = strdup(newname->resource_type);
                if (newname->content_type)
                    outname->content_type = strdup(newname->content_type);
                outname->init = 1;
                ret = mido_update_midoname(outname);
            }
        }
    }

    EUCA_FREE(payload);
    EUCA_FREE(outhttp);
    EUCA_FREE(outloc);
    return (ret);

}

void mido_copy_midoname(midoname * dst, midoname * src)
{
    if (!dst || !src) {
        return;
    }
    if (dst->init) {
        mido_free_midoname(dst);
    }

    bzero(dst, sizeof(midoname));
    if (src->tenant)
        dst->tenant = strdup(src->tenant);
    if (src->name)
        dst->name = strdup(src->name);
    if (src->uuid)
        dst->uuid = strdup(src->uuid);
    if (src->jsonbuf)
        dst->jsonbuf = strdup(src->jsonbuf);
    if (src->resource_type)
        dst->resource_type = strdup(src->resource_type);
    if (src->content_type)
        dst->content_type = strdup(src->content_type);
    dst->init = 1;
}

int mido_create_midoname(char *tenant, char *name, char *uuid, char *resource_type, char *content_type, char *jsonbuf, midoname * outname)
{
    if (!outname) {
        return (1);
    }

    bzero(outname, sizeof(midoname));
    if (tenant)
        outname->tenant = strdup(tenant);
    if (name)
        outname->name = strdup(name);
    if (uuid)
        outname->uuid = strdup(uuid);
    if (resource_type)
        outname->resource_type = strdup(resource_type);
    if (content_type)
        outname->content_type = strdup(content_type);
    if (jsonbuf)
        outname->jsonbuf = strdup(jsonbuf);
    outname->init = 1;

    return (0);
}

int mido_update_midoname(midoname * name)
{
    int ret = 0;
    struct json_object *jobj = NULL, *el = NULL;
    char special_uuid[EUCA_MAX_PATH];

    jobj = json_tokener_parse(name->jsonbuf);
    if (!jobj) {
        printf("ERROR: json_tokener_parse(...): returned NULL\n");
        ret = 1;
    } else {
        el = json_object_object_get(jobj, "id");
        if (el) {
            EUCA_FREE(name->uuid);
            name->uuid = strdup(json_object_get_string(el));
            //            json_object_put(el);                
        }

        el = json_object_object_get(jobj, "tenantId");
        if (el) {
            EUCA_FREE(name->tenant);
            name->tenant = strdup(json_object_get_string(el));
            //            json_object_put(el);
        }

        el = json_object_object_get(jobj, "name");
        if (el) {
            EUCA_FREE(name->name);
            name->name = strdup(json_object_get_string(el));
            //            json_object_put(el);
        }
        // special cases
        if (!strcmp(name->resource_type, "dhcp")) {
            char *subnet = NULL, *slashnet = NULL;
            EUCA_FREE(name->uuid);
            EUCA_FREE(name->name);

            el = json_object_object_get(jobj, "subnetPrefix");
            if (el) {
                subnet = strdup(json_object_get_string(el));
            }

            el = json_object_object_get(jobj, "subnetLength");
            if (el) {
                slashnet = strdup(json_object_get_string(el));
            }

            if (subnet && slashnet) {
                snprintf(special_uuid, EUCA_MAX_PATH, "%s_%s", subnet, slashnet);
                name->uuid = strdup(special_uuid);
            }
            EUCA_FREE(subnet);
            EUCA_FREE(slashnet);

        } else if (!strcmp(name->resource_type, "ip_addrs")) {
            char *ip = NULL;
            EUCA_FREE(name->uuid);
            EUCA_FREE(name->name);
            el = json_object_object_get(jobj, "addr");
            if (el) {
                ip = strdup(json_object_get_string(el));
            }
            if (ip) {
                snprintf(special_uuid, EUCA_MAX_PATH, "versions/6/ip_addrs/%s", ip);
                name->uuid = strdup(special_uuid);
            }
            EUCA_FREE(ip);

        } else {
            if (!name->uuid || !strlen(name->uuid)) {
                el = json_object_object_get(jobj, "uri");
                if (el) {
                    EUCA_FREE(name->uuid);
                    name->uuid = strdup(json_object_get_string(el));
                }
            }
        }

        json_object_put(jobj);
    }

    if (!name->name || (strlen(name->name) <= 0)) {
        name->name = strdup(name->uuid);
    }

    return (ret);
}

int mido_delete_resource(midoname * parentname, midoname * name)
{
    int rc = 0, ret = 0;
    char url[EUCA_MAX_PATH];
    json_object *jobj = NULL, *el = NULL;

    if (!name || !name->init) {
        return (0);
    }

    url[0] = '\0';

    jobj = json_tokener_parse(name->jsonbuf);
    if (jobj) {
        el = json_object_object_get(jobj, "uri");
        if (el) {
            snprintf(url, EUCA_MAX_PATH, "%s", json_object_get_string(el));
        }
        json_object_put(jobj);
    }

    if (!strlen(url)) {
        if (parentname) {
            snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s/%s/%s/%s", parentname->resource_type, parentname->uuid, name->resource_type, name->uuid);
        } else {
            snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s/%s", name->resource_type, name->uuid);
        }
    }

    LOGTRACE("resource to delete: %s/%s url to delete: %s\n", SP(name->name), SP(name->uuid), url);

    rc = midonet_http_delete(url);
    if (rc) {
        ret = 1;
    }
    return (ret);
}

struct mem_params_t {
    char *mem;
    size_t size;
};

static size_t mem_writer(void *contents, size_t size, size_t nmemb, void *in_params)
{
    struct mem_params_t *params = (struct mem_params_t *)in_params;

    if (!params->mem) {
        params->mem = calloc(1, 1);
    }
    params->mem = realloc(params->mem, params->size + (size * nmemb) + 1);
    if (params->mem == NULL) {
        return (0);
    }
    memcpy(&(params->mem[params->size]), contents, size * nmemb);
    params->size += size * nmemb;
    params->mem[params->size] = '\0';

    return (size * nmemb);
}

static size_t mem_reader(void *contents, size_t size, size_t nmemb, void *in_params)
{
    struct mem_params_t *params = (struct mem_params_t *)in_params;
    size_t bytes_to_copy = 0;

    if (!params->mem || params->size <= 0) {
        return (0);
    }

    if (!contents) {
        printf("ERROR: no mem to write into\n");
        params->size = 0;
        return (0);
    }

    bytes_to_copy = (params->size < (size * nmemb)) ? params->size : (size * nmemb);

    memcpy(contents, params->mem, bytes_to_copy);
    params->size -= bytes_to_copy;
    params->mem += bytes_to_copy;

    return (bytes_to_copy);
}

int midonet_http_get(char *url, char **out_payload)
{
    CURL *curl = NULL;
    CURLcode curlret;
    struct mem_params_t mem_writer_params = { 0, 0 };
    int ret = 0;
    long httpcode = 0L;
    //    struct curl_slist *headers = NULL;
    //    char hbuf[EUCA_MAX_PATH];

    *out_payload = NULL;

    curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_HTTPGET, 1L);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, mem_writer);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *)&mem_writer_params);

    /*
       snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: application/vnd.org.midonet.%s-v1+json", resource_type);
       headers = curl_slist_append(headers, hbuf);
       snprintf(hbuf, EUCA_MAX_PATH, "Expect:");
       headers = curl_slist_append(headers, hbuf);
       curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
     */

    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        printf("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    }
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
    if (httpcode != 200L) {
        ret = 1;
    }
    curl_easy_cleanup(curl);
    curl_global_cleanup();

    // convert to payload out

    if (!ret) {
        if (mem_writer_params.mem && mem_writer_params.size > 0) {
            *out_payload = calloc(mem_writer_params.size + 1, sizeof(char));
            memcpy(*out_payload, mem_writer_params.mem, mem_writer_params.size + 1);
        } else {
            printf("ERROR: no data to return after successful curl operation\n");
            ret = 1;
        }
    }
    if (mem_writer_params.mem)
        free(mem_writer_params.mem);
    return (ret);
}

int midonet_http_put(char *url, char *resource_type, char *payload)
{
    CURL *curl = NULL;
    CURLcode curlret;
    struct mem_params_t mem_reader_params = { 0, 0 };
    char hbuf[EUCA_MAX_PATH];
    struct curl_slist *headers = NULL;
    int ret = 0;
    long httpcode = 0L;

    mem_reader_params.mem = payload;
    mem_reader_params.size = strlen(payload) + 1;

    curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_UPLOAD, 1L);
    curl_easy_setopt(curl, CURLOPT_PUT, 1L);
    curl_easy_setopt(curl, CURLOPT_READFUNCTION, mem_reader);
    curl_easy_setopt(curl, CURLOPT_READDATA, (void *)&mem_reader_params);
    curl_easy_setopt(curl, CURLOPT_INFILESIZE_LARGE, (long)mem_reader_params.size);

    snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: application/vnd.org.midonet.%s-v1+json", resource_type);
    headers = curl_slist_append(headers, hbuf);
    snprintf(hbuf, EUCA_MAX_PATH, "Expect:");
    headers = curl_slist_append(headers, hbuf);
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        printf("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    }
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
    if (httpcode != 200L && httpcode != 204L) {
        ret = 1;
    }

    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
    curl_global_cleanup();

    return (ret);
}

static size_t header_find_location(char *content, size_t size, size_t nmemb, void *params)
{
    char *buf = NULL;
    char **loc = (char **)params;

    buf = calloc((size * nmemb) + 1, sizeof(char));
    memcpy(buf, content, size * nmemb);
    buf[size * nmemb] = '\0';

    if (buf && strstr(buf, "Location: ")) {
        *loc = calloc(strlen(buf), sizeof(char));
        sscanf(buf, "Location: %s", *loc);
    }
    free(buf);

    return (size * nmemb);
}

int midonet_http_post(char *url, char *resource_type, char *payload, char **out_payload)
{
    CURL *curl = NULL;
    CURLcode curlret;
    int ret = 0;
    char *loc = NULL, hbuf[EUCA_MAX_PATH];
    struct curl_slist *headers = NULL;

    *out_payload = NULL;

    curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    //    curl_easy_setopt(curl, CURLOPT_HEADER, 1L);
    curl_easy_setopt(curl, CURLOPT_NOBODY, 1L);
    curl_easy_setopt(curl, CURLOPT_POST, 1L);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, payload);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, strlen(payload));
    curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, header_find_location);
    curl_easy_setopt(curl, CURLOPT_HEADERDATA, &loc);
    if (!resource_type || strlen(resource_type) <= 0) {
        snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: application/json");
    } else {
        snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: application/vnd.org.midonet.%s-v1+json", resource_type);
    }
    headers = curl_slist_append(headers, hbuf);
    //    headers = curl_slist_append(headers, "Content-Type: application/vnd.org.midonet");
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        printf("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    }

    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
    curl_global_cleanup();

    if (!ret) {
        if (loc) {
            *out_payload = strdup(loc);
        }
    }
    EUCA_FREE(loc);

    return (ret);
}

int midonet_http_delete(char *url)
{
    CURL *curl = NULL;
    CURLcode curlret;
    int ret = 0;

    curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "DELETE");
    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        printf("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    }
    curl_easy_cleanup(curl);
    curl_global_cleanup();

    return (ret);
}

int mido_create_route(midoname * router, midoname * rport, char *src, char *src_slashnet, char *dst, char *dst_slashnet, char *next_hop_ip, char *weight, midoname * outname)
{
    int rc = 0, found = 0, ret = 0;
    midoname myname;
    midoname *routes = NULL;
    int max_routes = 0, i = 0;

    bzero(&myname, sizeof(midoname));

    myname.tenant = strdup(router->tenant);
    myname.resource_type = strdup("routes");
    myname.content_type = NULL;

    // only create the route if it doesn't already exist
    rc = mido_get_resources(router, 1, myname.tenant, "routes", &routes, &max_routes);
    if (!rc) {
        found = 0;
        for (i = 0; i < max_routes && !found; i++) {
            if (strcmp(next_hop_ip, "UNSET")) {
                rc = mido_cmp_midoname_to_input(&(routes[i]), "srcNetworkAddr", src, "srcNetworkLength", src_slashnet, "dstNetworkAddr", dst, "dstNetworkLength", dst_slashnet,
                                                "type", "Normal", "nextHopPort", rport->uuid, "weight", weight, "nextHopGateway", next_hop_ip, NULL);
                if (!rc) {
                    found = 1;
                }
            } else {
                rc = mido_cmp_midoname_to_input(&(routes[i]), "srcNetworkAddr", src, "srcNetworkLength", src_slashnet, "dstNetworkAddr", dst, "dstNetworkLength", dst_slashnet,
                                                "type", "Normal", "nextHopPort", rport->uuid, "weight", weight, NULL);
                if (!rc) {
                    found = 1;
                }
            }
        }
    }
    if (routes && max_routes > 0) {
        mido_free_midoname_list(routes, max_routes);
        EUCA_FREE(routes);
    }
    // route doesn't already exist, create it
    if (!found) {
        if (strcmp(next_hop_ip, "UNSET")) {
            rc = mido_create_resource(router, 1, &myname, outname, "srcNetworkAddr", src, "srcNetworkLength", src_slashnet, "dstNetworkAddr", dst, "dstNetworkLength", dst_slashnet,
                                      "type", "Normal", "nextHopPort", rport->uuid, "weight", weight, "nextHopGateway", next_hop_ip, NULL);
        } else {
            rc = mido_create_resource(router, 1, &myname, outname, "srcNetworkAddr", src, "srcNetworkLength", src_slashnet, "dstNetworkAddr", dst, "dstNetworkLength", dst_slashnet,
                                      "type", "Normal", "nextHopPort", rport->uuid, "weight", weight, NULL);
        }
        if (rc) {
            ret = 1;
        }
    }

    mido_free_midoname(&myname);
    return (ret);
}

int mido_delete_route(midoname * name)
{
    return (mido_delete_resource(NULL, name));
}

int mido_get_routes(midoname * router, midoname ** outnames, int *outnames_max)
{
    return (mido_get_resources(router, 1, router->tenant, "routes", outnames, outnames_max));
}

int json_object_cmp(json_object * one, json_object * two)
{
    int onetype = 0, twotype = 0, onesubtype = 0, twosubtype = 0, rc = 0, ret = 0, i = 0;
    char *oneel = NULL, *twoel = NULL;
    json_object *twoval = NULL;

    if (!one && !two) {
        LOGTRACE("both are null\n");
        return (0);
    } else if ((one && !two) || (!one && two)) {
        LOGTRACE("one is null\n");
        return (1);
    }

    LOGTRACE("in cmp:\none=%s\ntwo=%s\n", json_object_to_json_string(one), json_object_to_json_string(two));
    onetype = json_object_get_type(one);
    twotype = json_object_get_type(two);
    if (onetype != twotype) {
        LOGTRACE("types differ\n");
        //        return(1);
    }

    if (onetype == json_type_object) {
        json_object_object_foreach(one, onekey, oneval) {
            LOGTRACE("evaling key %s\n", onekey);
            onesubtype = json_object_get_type(oneval);
            twoval = json_object_object_get(two, onekey);
            twosubtype = json_object_get_type(twoval);
            if (onesubtype == json_type_object) {
                // recurse
                rc = json_object_cmp(oneval, twoval);
            } else if (onesubtype == json_type_array) {
                for (i = 0; i < json_object_array_length(oneval) && !rc; i++) {
                    rc = json_object_cmp(json_object_array_get_idx(oneval, i), json_object_array_get_idx(twoval, i));
                }
            } else {
                oneel = strdup(SP(json_object_get_string(oneval)));
                twoel = strdup(SP(json_object_get_string(twoval)));
                LOGTRACE("strcmp: %s/%s\n", oneel, twoel);
                rc = strcmp(oneel, twoel);
                EUCA_FREE(oneel);
                EUCA_FREE(twoel);
            }
            if (rc) {
                ret = 1;
            }
        }
    }

    LOGTRACE("result of cmp: %d\n", ret);
    return (ret);
}

int mido_cmp_midoname_to_input_json(midoname * name, ...)
{
    va_list al = { {0} };
    int ret = 0;

    va_start(al, name);
    ret = mido_cmp_midoname_to_input_json_v(name, &al);
    va_end(al);
    return (ret);
}

int mido_cmp_midoname_to_input_json_v(midoname * name, va_list * al)
{
    va_list ala = { {0} };
    char *jsonbuf = NULL;
    json_object *srcjobj = NULL, *dstjobj = NULL;
    int ret = 0;

    //    LOGTRACE("\n\n\nWTFHELLOn");

    va_copy(ala, *al);
    jsonbuf = mido_jsonize(NULL, &ala);
    va_end(ala);

    LOGTRACE("\nnew=%s\nold=%s\n", SP(jsonbuf), SP(name->jsonbuf));

    if (jsonbuf && name->jsonbuf) {

        dstjobj = json_tokener_parse(name->jsonbuf);
        srcjobj = json_tokener_parse(jsonbuf);

        // special case el removal
        if (!strcmp(name->resource_type, "rules")) {
            // for chain rules, remove the position element
            json_object_object_del(srcjobj, "position");
            json_object_object_del(dstjobj, "position");
        }

        if (json_object_cmp(srcjobj, dstjobj)) {
            ret = 1;
        } else {
            ret = 0;
        }

    } else {
        //        LOGTRACE("found a difference, one is empty\n");
        ret = 1;
    }

    if (srcjobj)
        json_object_put(srcjobj);
    if (dstjobj)
        json_object_put(dstjobj);

    //    LOGTRACE("RETURNING %d\n", ret);
    EUCA_FREE(jsonbuf);
    return (ret);
}

int mido_cmp_midoname_to_input(midoname * name, ...)
{
    va_list al = { {0} };
    int rc = 0;
    char *key = NULL, *dstval = NULL, *srcval = NULL;

    va_start(al, name);

    key = va_arg(al, char *);
    if (key)
        dstval = va_arg(al, char *);
    while (key && dstval) {
        rc = mido_getel_midoname(name, key, &srcval);
        if (!rc) {
            if (strcmp(dstval, srcval)) {
                EUCA_FREE(srcval);
                return (1);
            }
            EUCA_FREE(srcval);
        } else if (rc && !strcmp(dstval, "UNSET")) {
            // skip
        } else {
            EUCA_FREE(srcval);
            return (1);
        }
        key = va_arg(al, char *);
        if (key)
            dstval = va_arg(al, char *);
    }

    va_end(al);
    LOGTRACE("RESOURCE ALREADY IN PLACE: %s\n", SP(name->uuid));
    return (0);
}

int mido_get_routers(char *tenant, midoname ** outnames, int *outnames_max)
{
    return (mido_get_resources(NULL, 0, tenant, "routers", outnames, outnames_max));
}

int mido_get_bridges(char *tenant, midoname ** outnames, int *outnames_max)
{
    return (mido_get_resources(NULL, 0, tenant, "bridges", outnames, outnames_max));
}

int mido_get_chains(char *tenant, midoname ** outnames, int *outnames_max)
{
    return (mido_get_resources(NULL, 0, tenant, "chains", outnames, outnames_max));
}

int mido_get_resources(midoname * parents, int max_parents, char *tenant, char *resource_type, midoname ** outnames, int *outnames_max)
{
    int rc = 0, ret = 0, i = 0;
    char *payload = NULL, url[EUCA_MAX_PATH], tmpbuf[EUCA_MAX_PATH];
    midoname *names = NULL;
    int names_max = 0;

    *outnames = NULL;
    *outnames_max = 0;

    bzero(url, EUCA_MAX_PATH);
    if (!parents) {
        snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s?tenant_id=%s", resource_type, tenant);
    } else {
        snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/");
        for (i = 0; i < max_parents; i++) {
            bzero(tmpbuf, EUCA_MAX_PATH);
            snprintf(tmpbuf, EUCA_MAX_PATH, "%s/%s/", parents[i].resource_type, parents[i].uuid);
            strcat(url, tmpbuf);
        }
        bzero(tmpbuf, EUCA_MAX_PATH);
        snprintf(tmpbuf, EUCA_MAX_PATH, "%s?tenant_id=%s", resource_type, tenant);
        strcat(url, tmpbuf);
    }
    //    snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s?tenant_id=%s", resource_type, tenant);
    rc = midonet_http_get(url, &payload);
    //    LOGDEBUG("PAYLOAD: %s %s '%s' '%s' '%d' \n", SP(url), SP(payload), SP(resource_type), SP(tenant), max_parents);
    if (!rc) {
        struct json_object *jobj = NULL, *resource = NULL;

        jobj = json_tokener_parse(payload);
        if (!jobj) {
            printf("NOU\n");
        } else {
            //            jobj = json_object_get(jobj);
            if (json_object_is_type(jobj, json_type_array)) {

                names_max = 0;
                names = calloc(json_object_array_length(jobj), sizeof(midoname));

                for (i = 0; i < json_object_array_length(jobj); i++) {

                    resource = json_object_array_get_idx(jobj, i);
                    if (resource) {

                        /*
                           json_object_object_foreach(resource, key, val) {
                           printf("\t%s: %s\n", key, SP(json_object_get_string(val)));
                           }
                         */

                        names[names_max].tenant = strdup(tenant);
                        names[names_max].jsonbuf = strdup(json_object_to_json_string(resource));
                        names[names_max].resource_type = strdup(resource_type);
                        names[names_max].content_type = NULL;
                        names[names_max].init = 1;
                        mido_update_midoname(&(names[names_max]));
                        names_max++;

                        //                        json_object_put(resource);
                    }
                }
            }
            json_object_put(jobj);
        }
        EUCA_FREE(payload);
    }

    if (names && (names_max > 0)) {
        //        LOGINFO("WTF: %s %d, %d, %08X\n", resource_type, names_max, sizeof(midoname), *outnames);
        *outnames = calloc(names_max, sizeof(midoname));
        memcpy(*outnames, names, sizeof(midoname) * names_max);
        *outnames_max = names_max;
    }
    //    mido_free_midoname_list(names, names_max);
    EUCA_FREE(names);

    return (ret);
}

int mido_get_hosts(midoname ** outnames, int *outnames_max)
{
    int rc = 0, ret = 0, i = 0, hostup = 0;
    char *payload = NULL, url[EUCA_MAX_PATH];
    midoname *names = NULL;
    int names_max = 0;

    *outnames = NULL;
    *outnames_max = 0;

    bzero(url, EUCA_MAX_PATH);
    snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/hosts");
    rc = midonet_http_get(url, &payload);
    if (!rc) {
        struct json_object *jobj = NULL, *host = NULL, *el = NULL;

        jobj = json_tokener_parse(payload);
        if (!jobj) {
            printf("NOU\n");
        } else {
            if (json_object_is_type(jobj, json_type_array)) {
                //                printf("HMM: %s, %d\n", json_object_to_json_string(jobj), json_object_array_length(jobj));
                names_max = 0;
                names = calloc(json_object_array_length(jobj), sizeof(midoname));
                for (i = 0; i < json_object_array_length(jobj); i++) {
                    //                    bzero(&((*outnames)[i]), sizeof(midoname));
                    host = json_object_array_get_idx(jobj, i);
                    if (host) {
                        /*
                           json_object_object_foreach(host, key, val) {
                           printf("\t%s: %s\n", key, SP(json_object_get_string(val)));
                           }
                         */

                        el = json_object_object_get(host, "alive");
                        if (el) {
                            if (!strcmp(json_object_get_string(el), "false")) {
                                // host is down, skip
                                hostup = 0;
                            } else {
                                hostup = 1;
                            }
                            //                            json_object_put(el);
                        }

                        if (hostup) {
                            names[names_max].jsonbuf = strdup(json_object_to_json_string(host));

                            el = json_object_object_get(host, "id");
                            if (el) {
                                names[names_max].uuid = strdup(json_object_get_string(el));
                                //                                json_object_put(el);
                            }

                            el = json_object_object_get(host, "name");
                            if (el) {
                                names[names_max].name = strdup(json_object_get_string(el));
                                //                                json_object_put(el);
                            }

                            names[names_max].resource_type = strdup("hosts");
                            names[names_max].content_type = NULL;
                            names[names_max].init = 1;
                            names_max++;
                        }
                        //                        json_object_put(host);
                    }
                }
            }
            json_object_put(jobj);
        }

        EUCA_FREE(payload);
    }

    if (names && (names_max > 0)) {
        *outnames = calloc(names_max, sizeof(midoname));
        memcpy(*outnames, names, sizeof(midoname) * names_max);
        *outnames_max = names_max;
    }
    //    mido_free_midoname_list(names, names_max);
    EUCA_FREE(names);

    return (ret);
}

#ifdef MIDONET_API_TEST
int main(int argc, char **argv)
{
    int rc, i, j;

    /*
       TODO
       //     - routing table setup ops
       //     - write test setup (eucart, eucabr, vpcrt_X, vpcbr_X, all links)
       //     - manually link up and verify VM->rt
       //     - host ops
       //     - ext vm <-> bridge port linking
       //     - ext GW <-> rt port linking
       //     - chain ops
       //     - el. IP chain/routing setup
       - no pub IP masq chain setup
       //     - manually link up and verify pub->priv VM end-to-end
       BLOCKED     - DHCP and META taps and links
       BLOCKED     - test DHCP and META taps
       - populate operations for re-pop and automation
       - euca integration begin
       - new XML parse and conversion
       - NC interface naming and such on run
       - dhcp/meta tap creation and such
       - end-to-end testing
     */
    for (j = 0; j < 1; j++) {
        midoname eucabr, eucart, eucart_brport, eucabr_rtport, eucart_gwport;
        midoname vpcbr, vpcrt, vpcrt_brport, vpcbr_rtport;
        midoname vpcrt_uplink, eucabr_downlink;
        midoname vm_port_a, vm_port_b;
        midoname *names, vmhost, rthost, taphost;
        midoname vpcrt_prechain, vpcrt_postchain, vmrule_elip_dnat, vmrule_elip_snat;
        midoname vpcbr_tapport;
        midoname vpcbr_dhcp;
        int max;

        {
            // populate hosts

            for (i = 0; i < 1; i++) {
                rc = mido_get_hosts(&names, &max);

            }
            for (i = 0; i < max; i++) {
                printf("I: %d S: %s U: %s\n", i, names[i].name, names[i].uuid);
                if (strstr(names[i].name, "a-12.qa1")) {
                    mido_copy_midoname(&vmhost, &(names[i]));
                }
                if (strstr(names[i].name, "a-15.qa1")) {
                    mido_copy_midoname(&rthost, &(names[i]));
                }
                if (strstr(names[i].name, "h-41.qa1")) {
                    mido_copy_midoname(&taphost, &(names[i]));
                }
            }
        }
        printf("FOUND VM HOST: %s/%s\n", vmhost.name, vmhost.uuid);
        printf("FOUND RT HOST: %s/%s\n", rthost.name, rthost.uuid);
        printf("FOUND TAP HOST: %s/%s\n", taphost.name, taphost.uuid);

        {
            // set up euca routing GW

            rc = mido_create_router("euca_tenant_0", "eucart", &eucart);
            rc = mido_create_bridge("euca_tenant_0", "eucabr", &eucabr);
            rc = mido_create_port(&eucabr, "InteriorBridge", NULL, NULL, NULL, &eucabr_rtport);
            rc = mido_create_port(&eucart, "InteriorRouter", "192.168.254.1", "192.168.254.0", "24", &eucart_brport);
            rc = mido_router_create_route(&eucart, &eucart_brport, "0.0.0.0", "0", "192.168.254.0", "24", NULL, "0");
            rc = mido_link_ports(&eucart_brport, &eucabr_rtport);

            rc = mido_create_port(&eucart, "ExteriorRouter", "10.111.5.57", "10.111.0.0", "16", &eucart_gwport);
            rc = mido_router_create_route(&eucart, &eucart_gwport, "0.0.0.0", "0", "10.111.0.0", "16", NULL, "0");
            rc = mido_router_create_route(&eucart, &eucart_gwport, "0.0.0.0", "0", "0.0.0.0", "0", "10.111.5.34", "0");
            rc = mido_link_host_port(&rthost, "em1", &eucart, &eucart_gwport);
        }

        {
            // set up example VPC

            rc = mido_create_router("euca_tenant_0", "vpcrt_12345678", &vpcrt);
            rc = mido_create_bridge("euca_tenant_0", "vpcbr_12345678", &vpcbr);
            rc = mido_create_port(&vpcbr, "InteriorBridge", NULL, NULL, NULL, &vpcbr_rtport);
            rc = mido_create_port(&vpcrt, "InteriorRouter", "192.168.1.1", "192.168.1.0", "24", &vpcrt_brport);
            rc = mido_router_create_route(&vpcrt, &vpcrt_brport, "0.0.0.0", "0", "192.168.1.0", "24", NULL, "0");
            rc = mido_link_ports(&vpcrt_brport, &vpcbr_rtport);

        }

        {
            // link the vpc network and euca network

            rc = mido_create_port(&eucabr, "InteriorBridge", NULL, NULL, NULL, &eucabr_downlink);
            rc = mido_create_port(&vpcrt, "InteriorRouter", "192.168.254.2", "192.168.254.0", "24", &vpcrt_uplink);
            rc = mido_router_create_route(&vpcrt, &vpcrt_uplink, "0.0.0.0", "0", "192.168.254.0", "24", NULL, "0");
            rc = mido_router_create_route(&vpcrt, &vpcrt_uplink, "0.0.0.0", "0", "0.0.0.0", "0", "192.168.254.1", "0");
            rc = mido_link_ports(&eucabr_downlink, &vpcrt_uplink);
        }

        {
            // setup VM ports

            // create the Exterior ports for VMs
            rc = mido_create_port(&vpcbr, "ExteriorBridge", NULL, NULL, NULL, &vm_port_a);
            rc = mido_create_port(&vpcbr, "ExteriorBridge", NULL, NULL, NULL, &vm_port_b);

            // link vm host port to vm bridge port
            rc = mido_link_host_port(&vmhost, "vn_abcdefgh", &vpcbr, &vm_port_a);

            // set up dhcp for this bridge
            //int mido_create_dhcp(midoname *devname, char *subnet, char *slashnet, char *gw, char *dns, midoname *outname);

            rc = mido_create_dhcp(&vpcbr, "192.168.1.0", "24", "192.168.1.1", "8.8.8.8", &vpcbr_dhcp);
            midoname vpcbr_host_a;
            rc = mido_create_dhcphost(&vpcbr, &vpcbr_dhcp, "i-12345678", "d0:0d:00:7b:86:df", "192.168.1.2", &vpcbr_host_a);
        }

        {
            // create pre/post chains and DNAT/SNAT rules

            // create the chains
            rc = mido_create_chain("euca_tenant_0", "vpc_12345678_pre", &vpcrt_prechain);
            rc = mido_create_chain("euca_tenant_0", "vpc_12345678_post", &vpcrt_postchain);

            // create the rules
            rc = mido_create_rule(&vpcrt_prechain, "dnat", "UNSET", "UNSET", "UNSET", "10.111.200.11", "32", "UNSET", "accept", "192.168.1.2", "0", "0", &vmrule_elip_dnat);
            rc = mido_create_rule(&vpcrt_postchain, "snat", "192.168.1.2", "32", "UNSET", "UNSET", "UNSET", "UNSET", "accept", "10.111.200.11", "0", "0", &vmrule_elip_snat);

            // apply the chains to the vpc router
            rc = mido_update_router(&vpcrt, "inboundFilterId", vpcrt_prechain.uuid, NULL);
            rc = mido_update_router(&vpcrt, "outboundFilterId", vpcrt_postchain.uuid, NULL);

            // create the EL ip route in main router
            rc = mido_router_create_route(&eucart, &eucart_brport, "0.0.0.0", "0", "10.111.200.11", "32", "192.168.254.2", "100");

        }

        {
            // configure/bind to dhcp/meta taps

            //      rc = mido_create_port(&vpcbr, "ExteriorBridge", NULL, NULL, NULL, &vpcbr_tapport);
            //      rc = mido_link_host_port(&taphost, "eutap_12345678", &vpcbr, &vpcbr_tapport);
            //      rc = mido_link_host_port(&taphost, "em1.8", &vpcbr, &vpcbr_tapport);

        }

        scanf("%d", &i);

        // note; must unlink before delete
        rc = mido_unlink_host_port(&rthost, &eucart_gwport);
        rc = mido_delete_router(&eucart);

        // others appear to self cleanup
        rc = mido_delete_bridge(&eucabr);
        rc = mido_delete_router(&vpcrt);
        rc = mido_delete_bridge(&vpcbr);
        rc = mido_delete_chain(&vpcrt_prechain);
        rc = mido_delete_chain(&vpcrt_postchain);

        /*
           midoname eucabr, eucart, eucart_brport, eucabr_rtport, eucart_gwport;
           midoname vpcbr, vpcrt, vpcrt_brport, vpcbr_rtport;
           midoname vpcrt_uplink, eucabr_downlink;
           midoname vm_port_a, vm_port_b;
           midoname *names, vmhost, rthost;
           int max;
         */

        mido_free_midoname(&eucabr);
        mido_free_midoname(&eucart);
        mido_free_midoname(&eucart_brport);
        mido_free_midoname(&eucabr_rtport);
        mido_free_midoname(&eucart_gwport);
        mido_free_midoname(&vpcbr);
        mido_free_midoname(&vpcrt);
        mido_free_midoname(&vpcrt_brport);
        mido_free_midoname(&vpcbr_rtport);
        mido_free_midoname(&vpcrt_uplink);
        mido_free_midoname(&eucabr_downlink);
        mido_free_midoname(&vm_port_a);
        mido_free_midoname(&vm_port_b);
        mido_free_midoname(&vmhost);
        mido_free_midoname(&rthost);
        mido_free_midoname(&vpcrt_prechain);
        mido_free_midoname(&vpcrt_postchain);
        mido_free_midoname(&vmrule_elip_dnat);
        mido_free_midoname(&vmrule_elip_snat);
        for (i = 0; i < max; i++) {
            mido_free_midoname(&(names[i]));
        }
        EUCA_FREE(names);

        printf("ITER: %d\n", j);
    }
    exit(0);

    /*

       //  for (i=0; i<10000; i++) {
       for (i=0; i<100; i++) {
       mybridge = malloc(sizeof(midoname));
       myrouter = malloc(sizeof(midoname));
       myport = malloc(sizeof(midoname));
       bzero(mybridge, sizeof(midoname));
       bzero(myrouter, sizeof(midoname));
       bzero(myport, sizeof(midoname));

       rc = mido_create_bridge("euca_tenant_0", "testbr", mybridge);
       if (rc) {
       printf("ERROR: mido_create_bridge() failed\n");
       exit(1);
       } 

       rc = mido_create_port(mybridge, "InteriorBridge", NULL, NULL, NULL, myport);
       mido_free_midoname(myport);

       rc = mido_create_port(mybridge, "ExteriorBridge", NULL, NULL, NULL, myport);

       if (mido_print_bridge(mybridge)) {
       printf("ERROR: print() failed\n");
       exit(1);
       }

       rc = mido_update_bridge(mybridge, "name", "goober", NULL);
       if (!rc) {
       printf("ERROR: update(goober) failed\n");
       exit(1);
       }

       rc = mido_update_bridge(mybridge, "crabby", "goober", NULL);
       if (!rc) {
       printf("ERROR: update(crabby) failed\n");
       exit(1);
       }

       rc = mido_update_bridge(mybridge, "name", "goobertygoo", NULL);
       if (!rc) {
       printf("ERROR: update(goobertyfoo) failed\n");
       exit(1);
       }

       rc = mido_delete_bridge(mybridge);
       if (rc) {
       printf("ERROR: mido_delete_bridge() failed\n");
       exit(1);
       }

       rc = mido_create_router("euca_tenant_0", "testrt", myrouter);
       if (rc) {
       printf("ERROR: mido_create_router() failed\n");
       exit(1);
       }

       rc = mido_print_router(myrouter);
       if (rc) {
       printf("ERROR: mido_print_router() failed\n");
       exit (1);
       }  

       rc = mido_read_router(myrouter);
       if (rc) {
       printf("ERROR: mido_read_router() failed\n");
       exit (1);
       }

       rc = mido_print_router(myrouter);
       if (rc) {
       printf("ERROR: mido_print_router() failed\n");
       exit (1);
       }

       rc = mido_update_router(myrouter, "name", "goober", NULL);
       if (!rc) {
       printf("ERROR: update_router(goober) failed\n");
       exit(1);
       }

       rc = mido_delete_router(myrouter);
       if (rc) {
       printf("ERROR: mido_delete_router() failed\n");
       }

       rc = mido_read_router(myrouter);
       if (!rc) {
       printf("ERROR: mido_read_router() success but should have failed\n");
       exit (1);
       }

       printf("I: %d\n", i);
       mido_free_midoname(mybridge);
       mido_free_midoname(myrouter);
       mido_free_midoname(myport);
       EUCA_FREE(mybridge);
       EUCA_FREE(myrouter);
       EUCA_FREE(myport);

       }
     */

    exit(0);
}
#endif
