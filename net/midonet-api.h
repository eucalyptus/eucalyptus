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

#ifndef _INCLUDE_MIDONET_API_H_
#define _INCLUDE_MIDONET_API_H_

#include "euca_gni.h"


//!
//! @file net/midonet-api.h
//! Need definition
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/
#define VPCMIDO_TENANT     "euca_tenant_1"
#define VPCMIDO_TUNNELZONE "mido-tz midotz euca-tz eucatz"
#define VPCMIDO_CORERT     "eucart"
#define VPCMIDO_COREBR     "eucabr"
// Maximum number of active VPCs (mido routers)
// Should be less than 43518 - avoid collision with metadata server IP, 169.254.169.254
// 32767 is a good value - to match router IPs in 169.254.0.0/17 subnet
#define MAX_RTID 8192

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
enum {
    VPCSG_INGRESS,
    VPCSG_EGRESS,
    VPCSG_IAGPRIV,
    VPCSG_IAGPUB,
    VPCSG_IAGALL,
    VPCSG_END
};

enum vpc_nat_gateway_midos_t {
    NATG_RT,
    NATG_EUCABR_DOWNLINK,
    NATG_RT_UPLINK,
    NATG_RT_BRPORT,
    NATG_VPCBR_RTPORT,
    NATG_ELIP_PRE_IPADDRGROUP,
    NATG_ELIP_PRE_IPADDRGROUP_IP,
    NATG_ELIP_ROUTE,
    NATG_RT_INCHAIN,
    NATG_RT_OUTCHAIN,
    NATG_END
};

enum {
    VPCBR_VMPORT,
    VPCBR_DHCPHOST,
    VMHOST,
    ELIP_PRE,
    ELIP_POST,
    ELIP_PRE_IPADDRGROUP,
    ELIP_POST_IPADDRGROUP,
    ELIP_PRE_IPADDRGROUP_IP,
    ELIP_POST_IPADDRGROUP_IP,
    ELIP_ROUTE,
    INST_PRECHAIN,
    INST_POSTCHAIN,
    VPCINSTANCEEND
};

enum {
    VPCBR,
    VPCBR_RTPORT,
    VPCRT_BRPORT,
    VPCBR_DHCP,
    VPCBR_METAPORT,
    VPCBR_METAHOST,
    VPCSUBNETEND
};

enum {
    VPCRT,
    EUCABR_DOWNLINK,
    VPCRT_UPLINK,
    VPCRT_UPLINK_PRECHAIN,
    VPCRT_UPLINK_POSTCHAIN,
    VPCRT_PREELIPCHAIN,
    //VPCRT_RTINCHAIN,
    //VPCRT_RTOUTCHAIN,
    VPCEND
};

enum {
    EUCART,
    EUCABR,
    EUCART_BRPORT,
    EUCABR_RTPORT,
    METADATA_IPADDRGROUP,
    GWPORTGROUP,
    MIDOCOREEND
};

enum vpc_route_entry_target_t {
    VPC_TARGET_LOCAL,
    VPC_TARGET_INTERNET_GATEWAY,
    VPC_TARGET_VPRIVATE_GATEWAY,
    VPC_TARGET_ENI,
    VPC_TARGET_PEERING,
    VPC_TARGET_NAT_GATEWAY,
    VPC_TARGET_INVALID
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct midoname_t {
    char *tenant;
    char *name;
    char *uuid;
    char *jsonbuf;
    char *resource_type;
    char *content_type;
    char *vers;
    char *uri;
    int init;
} midoname;

typedef struct mido_parsed_route_t {
    midoname router;
    midoname rport;
    char *src_net;
    char *src_length;
    char *dst_net;
    char *dst_length;
    char *next_hop_ip;
    char *weight;
    int mido_present;
} mido_parsed_route;

typedef struct mido_resource_router_t {
    midoname resc;
    midoname **ports;
    midoname *routes;
    int max_ports;
    int max_routes;
} mido_resource_router;

typedef struct mido_resource_dhcp_t {
    midoname resc;
    midoname *dhcphosts;
    int max_dhcphosts;
} mido_resource_dhcp;

typedef struct mido_resource_bridge_t {
    midoname resc;
    midoname **ports;
    mido_resource_dhcp *dhcps;
    int max_ports;
    int max_dhcps;
} mido_resource_bridge;

typedef struct mido_resource_chain_t {
    midoname resc;
    midoname *rules;
    int max_rules;
} mido_resource_chain;

typedef struct mido_resource_host_t {
    midoname resc;
    midoname **ports;
    int max_ports;
} mido_resource_host;

typedef struct mido_resource_ipaddrgroup_t {
    midoname resc;
    midoname *ips;
    int max_ips;
} mido_resource_ipaddrgroup;

typedef struct mido_resource_portgroup_t {
    midoname resc;
    midoname **ports;
    int max_ports;
} mido_resource_portgroup;

typedef struct mido_resources_t {
    midoname *ports;
    mido_resource_router *routers;
    mido_resource_bridge *bridges;
    mido_resource_chain *chains;
    mido_resource_host *hosts;
    mido_resource_ipaddrgroup *ipaddrgroups;
    mido_resource_portgroup *portgroups;
    int max_ports;
    int max_routers;
    int max_bridges;
    int max_chains;
    int max_hosts;
    int max_ipaddrgroups;
    int max_portgroups;
} mido_resources;

typedef struct mido_vpc_secgroup_t {
    gni_secgroup *gniSecgroup;
    char name[16];
    midoname midos[VPCSG_END];
    midoname *ingress_rules, *egress_rules;
    int max_ingress_rules, max_egress_rules;
    int gnipresent;

} mido_vpc_secgroup;

typedef struct mido_vpc_instance_t {
    gni_instance *gniInst;
    char name[16];
    midoname midos[VPCINSTANCEEND];
    int gnipresent;
} mido_vpc_instance;

typedef struct mido_vpc_natgateway_t {
    gni_nat_gateway *gniNatGateway;
    gni_vpcsubnet *gniVpcSubnet;
    char name[32];
    int rtid;
    midoname midos[NATG_END];
    int gnipresent;
} mido_vpc_natgateway;

typedef struct mido_vpc_subnet_t {
    gni_vpcsubnet *gniSubnet;
    char name[16];
    char vpcname[16];
    midoname midos[VPCSUBNETEND];
    midoname *brports;
    midoname *dhcphosts;
    midoname **routes;
    mido_vpc_instance *instances;
    int max_brports;
    int max_dhcphosts;
    int max_instances;
    int max_routes;
    int gnipresent;
} mido_vpc_subnet;

typedef struct mido_vpc_t {
    gni_vpc *gniVpc;
    char name[16];
    int rtid;
    midoname midos[VPCEND];
    midoname *rtports;
    midoname *rtpostchain_rules;
    midoname *rtpreelipchain_rules;
    mido_vpc_subnet *subnets;
    mido_vpc_natgateway *natgateways;
    int max_rtports;
    int max_rtpostchain_rules;
    int max_rtpreelipchain_rules;
    int max_subnets;
    int max_natgateways;
    int gnipresent;
} mido_vpc;

typedef struct mido_core_t {
    midoname midos[MIDOCOREEND];

    midoname *brports;
    int max_brports;

    midoname *rtports;
    int max_rtports;

    midoname gwhosts[32];
    midoname gwports[32];
    int max_gws;
} mido_core;

typedef struct mido_config_t {
    char *ext_eucanetdhostname;

    char *ext_rthostnamearr[32];
    char *ext_rthostaddrarr[32];
    char *ext_rthostifacearr[32];
    int ext_rthostarrmax;

    char *ext_pubnw;
    char *ext_pubgwip;
    char *eucahome;
    u32 int_rtnw;
    u32 int_rtaddr;
    u32 enabledCLCIp;
    int int_rtsn;
    int flushmode;
    int disable_l2_isolation;

//    midoname *hosts;
//    midoname *routers;
//    midoname *bridges;
//    midoname *chains;
//    midoname *brports;
//    midoname *rtports;
//    midoname *ipaddrgroups;
//    midoname *portgroups;
//
//    int max_hosts;
//    int max_routers;
//    int max_bridges;
//    int max_chains;
//    int max_brports;
//    int max_rtports;
//    int max_ipaddrgroups;
//    int max_portgroups;

    mido_resources *resources;
    mido_core *midocore;

    mido_vpc *vpcs;
    int max_vpcs;

    mido_vpc_secgroup *vpcsecgroups;
    int max_vpcsecgroups;

    int router_ids[MAX_RTID];
} mido_config;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/
extern int http_gets;
extern int http_posts;
extern int http_puts;
extern int http_deletes;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//int mido_allocate_midorule(char *position, char *type, char *action, char *protocol, char *srcIAGuuid, char *src_port_min, char *src_port_max,  char *dstIAGuuid, char *dst_port_min, char *dst_port_max, char *matchForwardFlow, char *matchReturnFlow, char *nat_target, char *nat_port_min, char *nat_port_max, midorule *outrule);

void mido_info_http_count();
void mido_info_http_count_total();

void mido_free_mido_parsed_route(mido_parsed_route *route);
void mido_free_mido_parsed_route_list(mido_parsed_route *routes, int max_routes);

int mido_create_midoname(char *tenant, char *name, char *uuid, char *resource_type, char *content_type, char *vers, char *uri, char *jsonbuf, midoname * outname);
void mido_free_midoname(midoname * name);
void mido_free_midoname_list(midoname * name, int max_name);
int mido_update_midoname(midoname * name);
void mido_copy_midoname(midoname * dst, midoname * src);
int mido_getel_midoname(midoname * name, char *key, char **val);
void mido_print_midoname(midoname * name);
int mido_cmp_midoname(midoname *a, midoname *b);
int mido_merge_midoname_lists(midoname *lista, int lista_max, midoname *listb, int listb_max, midoname **addmidos, int addmidos_max, midoname **delmidos, int delmidos_max);

int mido_check_state(void);

int mido_create_tenant(char *name, midoname * outname);
//int mido_read_tenant(midoname * name);
int mido_update_tenant(midoname * name);
int mido_delete_tenant(midoname * name);

int mido_get_tunnelzones(char *tenant, midoname **outnames, int *outnames_max);
int mido_get_tunnelzone_hosts(midoname *tzone, midoname **outnames, int *outnames_max);

int mido_create_bridge(char *tenant, char *name, midoname * outname);
//int mido_read_bridge(midoname * name);
int mido_update_bridge(midoname * name, ...);
int mido_print_bridge(midoname * name);
int mido_delete_bridge(midoname * name);
int mido_get_bridges(char *tenant, midoname ** outnames, int *outnames_max);

int mido_create_router(char *tenant, char *name, midoname * outname);
//int mido_read_router(midoname * name);
int mido_update_router(midoname * name, ...);
int mido_delete_router(midoname * name);
int mido_print_router(midoname * name);
int mido_get_routers(char *tenant, midoname ** outnames, int *outnames_max);

int mido_find_route_from_list(midoname *routes, int max_routes, midoname *rport, char *src, char *src_slashnet, char *dst, char *dst_slashnet, char *next_hop_ip, char *weight, int *foundidx);
int mido_create_route(mido_config *mido, midoname * router, midoname * rport, char *src, char *src_slashnet, char *dst, char *dst_slashnet, char *next_hop_ip, char *weight, midoname * outname);
//int mido_create_route(midoname * router, midoname * rport, char *src, char *src_slashnet, char *dst, char *dst_slashnet, char *next_hop_ip, char *weight, midoname * outname);
int mido_delete_route(midoname * name);
int mido_get_routes(midoname * router, midoname ** outnames, int *outnames_max);

int mido_create_dhcp(midoname * devname, char *subnet, char *slashnet, char *gw, u32 * dnsServers, int max_dnsServers, midoname * outname);
//int mido_read_dhcp(midoname * name);
int mido_update_dhcp(midoname * name, ...);
int mido_print_dhcp(midoname * name);
int mido_delete_dhcp(midoname * devname, midoname * name);
int mido_get_dhcps(midoname * devname, midoname ** outnames, int *outnames_max);

int mido_create_dhcphost(midoname * devname, midoname * dhcp, char *name, char *mac, char *ip, char *dns_domain, midoname * outname);
int mido_delete_dhcphost(midoname * name);
int mido_get_dhcphosts(midoname * devname, midoname * dhcp, midoname ** outnames, int *outnames_max);

int mido_create_portgroup(char *tenant, char *name, midoname *outname);
int mido_update_portgroup(midoname * name, ...);
int mido_delete_portgroup(midoname * name);
int mido_print_portgroup(midoname * name);
int mido_get_portgroups(char *tenant, midoname ** outnames, int *outnames_max);

int mido_create_portgroup_port(mido_config *mido, midoname * portgroup, char * portId, midoname * outname);
int mido_delete_portgroup_port(midoname * name);
int mido_get_portgroup_ports(midoname * portgroup, midoname ** outnames, int *outnames_max);


int mido_create_port(midoname * devname, char *port_type, char *ip, char *nw, char *slashnet, char *mac, midoname * outname);
//int mido_read_port(midoname * name);
int mido_update_port(midoname * name, ...);
int mido_print_port(midoname * name);
int mido_delete_port(midoname * name);
int mido_get_ports(midoname * devname, midoname ** outnames, int *outnames_max);

int mido_link_ports(midoname * a, midoname * b);

int mido_link_host_port(midoname * host, char *interface, midoname * device, midoname * port);
int mido_unlink_host_port(midoname * host, midoname * port);

int mido_get_hosts(midoname ** outnames, int *outnames_max);
int mido_get_interfaces(midoname * host, midoname ** outports, int *outports_max);

int mido_create_chain(char *tenant, char *name, midoname * outname);
//int mido_read_chain(midoname * name);
int mido_update_chain(midoname * name, ...);
int mido_print_chain(midoname * name);
int mido_delete_chain(midoname * name);
int mido_get_chains(char *tenant, midoname ** outnames, int *outnames_max);

//int mido_create_rule(midoname * chain, midoname * outname, midoname *memorules, int max_memorules, int *next_position, ...);
int mido_create_rule(midoname * chain, midoname * outname, midoname *memorules, int max_memorules, int * next_position, ...);
//int mido_create_rule_v1(midoname *chain, midorule *rule, midoname *outname);
//int mido_read_rule(midoname * name);
int mido_find_rule_from_list(midoname *rules, int max_rules, midoname *outrule, ...);
int mido_update_rule(midoname * name, ...);
int mido_print_rule(midoname * name);
int mido_delete_rule(midoname * name);
int mido_get_rules(midoname * chainname, midoname ** outnames, int *outnames_max);

int mido_create_ipaddrgroup(char *tenant, char *name, midoname * outname);
//int mido_read_ipaddrgroup(midoname * name);
int mido_update_ipaddrgroup(midoname * name, ...);
int mido_delete_ipaddrgroup(midoname * name);
int mido_print_ipaddrgroup(midoname * name);
int mido_get_ipaddrgroups(char *tenant, midoname ** outnames, int *outnames_max);

int mido_create_ipaddrgroup_ip(mido_config *mido, midoname *ipaddrgroup, char *ip, midoname *outname);
//int mido_create_ipaddrgroup_ip(midoname * ipaddrgroup, char *ip, midoname * outname);
int mido_delete_ipaddrgroup_ip(midoname * ipaddrgroup, midoname * ipaddrgroup_ip);
int mido_get_ipaddrgroup_ips(midoname * ipaddrgroup, midoname ** outnames, int *outnames_max);

int mido_create_resource_v(midoname * parents, int max_parents, midoname * newname, midoname * outname, va_list * al);
int mido_create_resource(midoname * parents, int max_parents, midoname * newname, midoname * outname, ...);
//int mido_read_resource(char *resource_type, midoname * name, char *apistr);
int mido_update_resource(char *resource_type, char *content_type, char *vers, midoname * name, va_list * al);
int mido_print_resource(char *resource_type, midoname * name);
int mido_delete_resource(midoname * parentname, midoname * name);
int mido_get_resources(midoname * parents, int max_parents, char *tenant, char *resource_type, char *apistr, midoname ** outnames, int *outnames_max);
//int mido_get_resources(midoname * parents, int max_parents, char *tenant, char *resource_type, midoname ** outnames, int *outnames_max);

int mido_cmp_midoname_to_input(midoname * name, ...);
int mido_cmp_midoname_to_input_json(midoname * name, ...);
int mido_cmp_midoname_to_input_json_v(midoname * name, va_list * al);
char *mido_jsonize(char *tenant, va_list * al);

int midonet_http_get(char *url, char *apistr, char **out_payload);
int midonet_http_put(char *url, char *resource_type, char *vers, char *payload);
int midonet_http_post(char *url, char *resource_type, char *vers, char *payload, char **out_payload);
int midonet_http_delete(char *url);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_MIDONET_API_H_ */
