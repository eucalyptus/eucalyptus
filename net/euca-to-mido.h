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

#ifndef _INCLUDE_EUCA_TO_MIDO_H_
#define _INCLUDE_EUCA_TO_MIDO_H_

//!
//! @file net/euca-to-mido.h
//! Need definition
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <midonet-api.h>

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
enum {
    VPCSG_INGRESS,
    VPCSG_EGRESS,
    VPCSG_IAGPRIV,
    VPCSG_IAGPUB,
    VPCSG_IAGALL,
    VPCSG_END
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
    VPCRT_PRECHAIN,
    VPCRT_POSTCHAIN,
    VPCRT_PREETHERCHAIN,
    VPCRT_PREMETACHAIN,
    VPCRT_PREVPCINTERNALCHAIN,
    VPCRT_PREELIPCHAIN,
    VPCRT_PREFWCHAIN,
    VPCEND
};

enum {
    EUCART,
    EUCABR,
    EUCART_BRPORT,
    EUCABR_RTPORT,
    EUCART_GWPORT,
    GWHOST,
    METADATA_IPADDRGROUP,
    MIDOCOREEND
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct mido_vpc_secgroup_t {
    gni_secgroup *gniSecgroup;
    char name[16];
    midoname midos[VPCSG_END];
    int gnipresent;

} mido_vpc_secgroup;

typedef struct mido_vpc_instance_t {
    gni_instance *gniInst;
    char name[16];
    midoname midos[VPCINSTANCEEND];
    int gnipresent;
} mido_vpc_instance;

typedef struct mido_vpc_subnet_t {
    gni_vpcsubnet *gniSubnet;
    char name[16];
    char vpcname[16];
    midoname midos[VPCSUBNETEND];
    midoname *brports;
    midoname *dhcphosts;
    mido_vpc_instance *instances;
    int max_brports;
    int max_dhcphosts;
    int max_instances;
    int gnipresent;
} mido_vpc_subnet;

typedef struct mido_vpc_t {
    gni_vpc *gniVpc;
    char name[16];
    int rtid;
    midoname midos[VPCEND];
    midoname *rtports;
    mido_vpc_subnet *subnets;
    int max_rtports;
    int max_subnets;
    int gnipresent;
} mido_vpc;

typedef struct mido_core_t {
    midoname midos[MIDOCOREEND];
    midoname *brports;
    midoname *rtports;
    int max_brports;
    int max_rtports;
} mido_core;

typedef struct mido_config_t {
    char *ext_eucanetdhostname;
    char *ext_rthostname;
    char *ext_rtaddr;
    char *ext_rtiface;
    char *ext_pubnw;
    char *ext_pubgwip;
    char *eucahome;
    u32 int_rtnw;
    u32 int_rtaddr;
    u32 enabledCLCIp;
    int int_rtsn;
    int setupcore;
    midoname *hosts;
    midoname *routers;
    midoname *bridges;
    midoname *chains;
    midoname *brports;
    midoname *rtports;
    midoname *ipaddrgroups;
    int max_hosts;
    int max_routers;
    int max_bridges;
    int max_chains;
    int max_brports;
    int max_rtports;
    int max_ipaddrgroups;
    mido_core *midocore;
    mido_vpc *vpcs;
    int max_vpcs;
    mido_vpc_secgroup *vpcsecgroups;
    int max_vpcsecgroups;
    int router_ids[4096];
} mido_config;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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

int get_next_router_id(mido_config * mido, int *nextid);
int set_router_id(mido_config * mido, int id);

int cidr_split(char *cidr, char *outnet, char *outslashnet, char *outgw, char *outplustwo);

int initialize_mido(mido_config * mido, char *eucahome, char *setupcore, char *ext_eucanetdhostname, char *ext_rthostname, char *ext_rtaddr, char *ext_rtiface, char *ext_pubnw,
                    char *ext_pubgwip, char *int_rtnetwork, char *int_rtslashnet);
int discover_mido_resources(mido_config * mido);

int populate_mido_core(mido_config * mido, mido_core * midocore);
int create_mido_core(mido_config * mido, mido_core * midocore);
int delete_mido_core(mido_config * mido, mido_core * midocore);

int populate_mido_vpc(mido_config * mido, mido_core * midocore, mido_vpc * vpc);
int create_mido_vpc(mido_config * mido, mido_core * midocore, mido_vpc * vpc);
int delete_mido_vpc(mido_config * mido, mido_vpc * vpc);
int find_mido_vpc(mido_config * mido, char *vpcname, mido_vpc ** outvpc);

int populate_mido_vpc_subnet(mido_config * mido, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet);
int create_mido_vpc_subnet(mido_config * mido, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet, char *subnet, char *slashnet, char *gw, char *instanceDNSDomain,
                           u32 * instanceDNSServers, int max_instanceDNSServers);
int delete_mido_vpc_subnet(mido_config * mido, mido_vpc_subnet * subnet);
int find_mido_vpc_subnet(mido_vpc * vpc, char *subnetname, mido_vpc_subnet ** outvpcsubnet);
int find_mido_vpc_subnet_global(mido_config * mido, char *subnetname, mido_vpc_subnet ** outvpcsubnet);

int populate_mido_vpc_instance(mido_config * mido, mido_core * midocore, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet, mido_vpc_instance * vpcinstance);
int create_mido_vpc_instance(mido_config * mido, mido_vpc_instance * vpcinstance, char *nodehostname);
int delete_mido_vpc_instance(mido_vpc_instance * vpcinstance);
int find_mido_vpc_instance(mido_vpc_subnet * vpcsubnet, char *instancename, mido_vpc_instance ** outvpcinstance);
int find_mido_vpc_instance_global(mido_config * mido, char *instancename, mido_vpc_instance ** outvpcinstance);

int populate_mido_vpc_secgroup(mido_config * mido, mido_vpc_secgroup * vpcsecgroup);
int create_mido_vpc_secgroup(mido_config * mido, mido_vpc_secgroup * vpcsecgroup);
int delete_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup);
int find_mido_vpc_secgroup(mido_config * mido, char *secgroupname, mido_vpc_secgroup ** outvpcsecgroup);

int connect_mido_vpc_instance(mido_vpc_subnet * vpcsubnet, mido_vpc_instance * inst, midoname * vmhost);

int connect_mido_vpc_instance_elip(mido_config * mido, mido_core * midocore, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet, mido_vpc_instance * inst);
int disconnect_mido_vpc_instance_elip(mido_vpc_instance * vpcinstance);

int free_mido_config(mido_config * mido);
int free_mido_core(mido_core * midocore);
int free_mido_vpc(mido_vpc * vpc);
int free_mido_vpc_subnet(mido_vpc_subnet * vpcsubnet);
int free_mido_vpc_instance(mido_vpc_instance * vpcinstance);
int free_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup);

void print_mido_vpc(mido_vpc * vpc);
void print_mido_vpc_subnet(mido_vpc_subnet * vpcsubnet);
void print_mido_vpc_instance(mido_vpc_instance * vpcinstance);
void print_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup);

int do_midonet_update(globalNetworkInfo * gni, mido_config * mido);
int do_midonet_teardown(mido_config * mido);

int do_metaproxy_setup(mido_config * mido);
int do_metaproxy_teardown(mido_config * mido);
int do_metaproxy_maintain(mido_config * mido, int mode);

int create_mido_meta_core(mido_config * mido);
int create_mido_meta_vpc_namespace(mido_config * mido, mido_vpc * vpc);
int create_mido_meta_subnet_veth(mido_config * mido, mido_vpc * vpc, char *name, char *subnet, char *slashnet, char **tapiface);

int delete_mido_meta_core(mido_config * mido);
int delete_mido_meta_vpc_namespace(mido_config * mido, mido_vpc * vpc);
int delete_mido_meta_subnet_veth(mido_config * mido, char *name);

#endif /* ! _INCLUDE_EUCA_TO_MIDO_H_ */
