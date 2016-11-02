// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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

#include "euca_gni.h"
#include "eucanetd.h"
/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define INTIP_ENI_MAP_FILE     EUCALYPTUS_RUN_DIR "/eucanetd_intip_eni_map"
#define INTIP_ENI_MAP_FILE_TMP INTIP_ENI_MAP_FILE ".tmp"

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

enum vpcmido_nginx_t {
    VPCMIDO_NGINX_START,
    VPCMIDO_NGINX_STOP
};

enum vpc_sg_midos_t {
    VPCSG_INGRESS,
    VPCSG_EGRESS,
    VPCSG_IAGPRIV,
    VPCSG_IAGPUB,
    VPCSG_IAGALL,
    VPCSG_END
};

enum vpc_nat_gateway_midos_t {
    NATG_RT,
    NATG_SUBNBR,
    NATG_EUCART,
    NATG_EUCABR,
    NATG_EUCABR_DOWNLINK,
    NATG_RT_UPLINK,
    NATG_RT_BRPORT,
    NATG_SUBNBR_RTPORT,
    NATG_ELIP_PRE_IPADDRGROUP,
    NATG_ELIP_PRE_IPADDRGROUP_IP,
    NATG_ELIP_ROUTE,
    NATG_RT_INCHAIN,
    NATG_RT_OUTCHAIN,
    NATG_END
};

enum vpc_instance_midos_t {
    INST_VMHOST,
    INST_VPCBR_VMPORT,
    INST_VPCBR_DHCPHOST,
    INST_PRECHAIN,
    INST_POSTCHAIN,
    INST_ELIP_PRE_IPADDRGROUP,
    INST_ELIP_POST_IPADDRGROUP,
    INST_ELIP_PRE_IPADDRGROUP_IP,
    INST_ELIP_POST_IPADDRGROUP_IP,
    INST_ELIP_PRE,
    INST_ELIP_POST,
    INST_ELIP_ROUTE,
    INST_MD_DNAT,
    INST_MD_SNAT,
    INST_MD_ROUTE,
    INST_END
};

enum vpc_subnet_midos_t {
    SUBN_BR,
    SUBN_BR_RTPORT,
    SUBN_VPCRT_BRPORT,
    SUBN_BR_DHCP,
    SUBN_BR_METAPORT,
    SUBN_BR_METAHOST,
    SUBN_END
};

enum vpc_midos_t {
    VPC_VPCRT,
    VPC_EUCABR_DOWNLINK,
    VPC_VPCRT_UPLINK,
    VPC_VPCRT_UPLINK_PRECHAIN,
    VPC_VPCRT_UPLINK_POSTCHAIN,
    VPC_VPCRT_PREELIPCHAIN,
    VPC_EUCAMDBR_DOWNLINK,
    VPC_VPCRT_MDUPLINK,
    VPC_VPCRT_MDUPLINK_INFILTER,
    VPC_VPCRT_MDUPLINK_OUTFILTER,
    VPC_END
};

enum mido_core_midos_t {
    CORE_EUCART,
    CORE_EUCABR,
    CORE_EUCART_BRPORT,
    CORE_EUCABR_RTPORT,
    CORE_EUCABR_INFILTER,
    CORE_METADATA_IPADDRGROUP,
    CORE_GWPORTGROUP,
    CORE_EUCAVER_IPADDRGROUP,
    CORE_END
};

enum mido_md_midos_t {
    MD_RT,
    MD_BR,
    MD_RT_BRPORT,
    MD_BR_RTPORT,
    MD_RT_EXTPORT,
    MD_RT_EXTPORT_OUTFILTER,
    MD_END
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

enum mido_rule_type_t {
    MIDO_RULE_INVALID,
    MIDO_RULE_SG_EGRESS,
    MIDO_RULE_SG_INGRESS
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct mido_vpc_t mido_vpc;
typedef struct mido_vpc_subnet_t mido_vpc_subnet;
typedef struct mido_vpc_natgateway_t mido_vpc_natgateway;
typedef struct mido_vpc_instance_t mido_vpc_instance;
typedef struct mido_vpc_secgroup_t mido_vpc_secgroup;

struct mido_vpc_secgroup_t {
    gni_secgroup *gniSecgroup;
    char name[16];
    midonet_api_chain *ingress;
    midonet_api_chain *egress;
    midonet_api_ipaddrgroup *iag_priv;
    midonet_api_ipaddrgroup *iag_pub;
    midonet_api_ipaddrgroup *iag_all;
    midoname *midos[VPCSG_END];
    int *midopresent_pubips;
    int *midopresent_privips;
    int *midopresent_allips_pub;
    int *midopresent_allips_priv;
    int ingress_changed;
    int egress_changed;
    int interfaces_changed;
    int population_failed;
    int midopresent;
    int gnipresent;
};

struct mido_vpc_instance_t {
    gni_instance *gniInst;
    char name[16];
    midonet_api_chain *prechain;
    midonet_api_chain *postchain;
    midonet_api_ipaddrgroup *iag_pre;
    midonet_api_ipaddrgroup *iag_post;
    midoname *midos[INST_END];
    u32 privip;
    u32 pubip;
    int eniid;
    int pubip_changed;
    int host_changed;
    int srcdst_changed;
    int sg_changed;
    int eniid_changed;
    int population_failed;
    int midopresent;
    int gnipresent;
};

struct mido_vpc_natgateway_t {
    gni_nat_gateway *gniNatGateway;
    gni_vpcsubnet *gniVpcSubnet;
    midonet_api_router *natgrt;
    midonet_api_chain *inchain;
    midonet_api_chain *outchain;
    char name[32];
    int rtid;
    midoname *midos[NATG_END];
    int population_failed;
    int midopresent;
    int gnipresent;
};

struct mido_vpc_subnet_t {
    gni_vpcsubnet *gniSubnet;
    char name[16];
    char vpcname[16];
    mido_vpc *vpc;
    midonet_api_bridge *subnetbr;
    midoname *midos[SUBN_END];
    midoname **routes;
    mido_vpc_instance *instances;
    mido_vpc_natgateway *natgateways;
    int max_instances;
    int max_natgateways;
    int max_routes;
    int population_failed;
    int midopresent;
    int gnipresent;
};

struct mido_vpc_t {
    gni_vpc *gniVpc;
    char name[16];
    int rtid;
    midonet_api_router *vpcrt;
    midonet_api_chain *rt_uplink_prechain;
    midonet_api_chain *rt_uplink_postchain;
    midonet_api_chain *rt_preelipchain;
    midonet_api_chain *rt_mduplink_infilter;
    midonet_api_chain *rt_mduplink_outfilter;
    midoname *midos[VPC_END];
    mido_vpc_subnet *subnets;
    int max_subnets;
    int population_failed;
    int midopresent;
    int gnipresent;
};

typedef struct mido_core_t {
    midoname *midos[CORE_END];
    midonet_api_ipaddrgroup *eucaver_iag;
    midonet_api_router *eucart;
    midonet_api_bridge *eucabr;
    midonet_api_chain *eucabr_infilter;
    midonet_api_ipaddrgroup *metadata_iag;
    midonet_api_host *eucanetdhost;
    int population_failed;

    midoname *gwhosts[32];
    midoname *gwports[32];
    int max_gws;
} mido_core;

typedef struct mido_md_config_t {
    u32 int_mdnw;
    int int_mdsn;
    u32 ext_mdnw;
    int ext_mdsn;
    u32 mdnw;
    int mdsn;
    u32 md_veth_host;
    u32 md_veth_mido;
    u32 md_http;
    u32 md_dns;
} mido_md_config;

typedef struct mido_md_t {
    midoname *midos[MD_END];
    midonet_api_router *eucamdrt;
    midonet_api_bridge *eucamdbr;
    midonet_api_host *eucanetdhost;
    midonet_api_chain *eucamdrt_extport_outfilter;
    int population_failed;
} mido_md;

typedef struct mido_config_t {
    eucanetdConfig *config;
    u32 euca_version;

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
    int disable_l2_isolation;

    mido_md_config mdconfig;

    mido_core *midocore;
    mido_md *midomd;

    mido_vpc *vpcs;
    int max_vpcs;

    mido_vpc_secgroup *vpcsecgroups;
    int max_vpcsecgroups;

    int udpsock;
    boolean *rt_ids;
    boolean *eni_ids; 
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

int get_next_router_id(mido_config *mido, int *nextid);
int set_router_id(mido_config *mido, int id);
int clear_router_id(mido_config *mido, int id);
int get_next_eni_id(mido_config *mido, int *nextid);
int set_eni_id(mido_config *mido, int id);
int clear_eni_id(mido_config *mido, int id);

int cidr_split(char *cidr, char *outnet, char *outslashnet, char *outgw, char *outplustwo);
int is_mido_vpc_plustwo(mido_config *mido, char *iptocheck);

enum vpc_route_entry_target_t parse_mido_route_entry_target(const char *target);

int initialize_mido(mido_config *mido, eucanetdConfig *eucanetd_config);
int reinitialize_mido(mido_config *mido);
int validate_mido(mido_config *mido);

int clear_mido_gnitags(mido_config *mido);
int check_mido_tunnelzone();

char *discover_mido_bgps(mido_config *mido);
char *discover_mido_bgps_v1(mido_config *mido);
char *discover_mido_bgps_v5(mido_config *mido);

int populate_mido_core(mido_config *mido, mido_core *midocore);
int create_mido_core(mido_config *mido, mido_core *midocore);
int delete_mido_core(mido_config *mido, mido_core *midocore);

int populate_mido_md(mido_config *mido);
int create_mido_md(mido_config *mido);
int delete_mido_md(mido_config *mido);
int disable_mido_md(mido_config *mido);

int connect_mido_md_ext_veth(mido_config *mido);
int disconnect_mido_md_ext_veth(mido_config *mido);
int create_mido_md_egress_rules(mido_config *mido, midonet_api_chain *chain);
int parse_mido_md_egress_rules(mido_config *mido, mido_parsed_chain_rule ***parsedrules, int *max_parsedrules);

int populate_mido_vpc(mido_config *mido, mido_core *midocore, mido_vpc *vpc);
int create_mido_vpc(mido_config *mido, mido_core *midocore, mido_vpc *vpc);
int delete_mido_vpc(mido_config *mido, mido_vpc *vpc);
int find_mido_vpc(mido_config *mido, char *vpcname, mido_vpc ** outvpc);

int populate_mido_vpc_subnet(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet);
int create_mido_vpc_subnet(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, char *subnet, char *slashnet, char *gw, char *instanceDNSDomain,
                           u32 *instanceDNSServers, int max_instanceDNSServers);
int delete_mido_vpc_subnet(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *subnet);
int find_mido_vpc_subnet(mido_vpc *vpc, char *subnetname, mido_vpc_subnet **outvpcsubnet);
int find_mido_vpc_subnet_global(mido_config *mido, char *subnetname, mido_vpc **outvpc, mido_vpc_subnet **outvpcsubnet);

int parse_mido_vpc_subnet_route_table(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet,
        char *subnetNetaddr, char *subnetSlashnet, gni_route_table *rtable, gni_vpc *gnivpc,
        mido_parsed_route **proutes, int *max_proutes);
int create_mido_vpc_subnet_route_table(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet,
        char *subnetNetaddr, char *subnetSlashnet, gni_route_table *rtable, gni_vpc *gnivpc);

int populate_mido_vpc_instance(mido_config *mido, mido_core *midocore, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, mido_vpc_instance *vpcinstance);
int create_mido_vpc_instance(mido_vpc_instance *vpcinstance);
int delete_mido_vpc_instance(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *subnet, mido_vpc_instance *vpcinstance);
int find_mido_vpc_instance(mido_vpc_subnet *vpcsubnet, char *instancename, mido_vpc_instance **outvpcinstance);
int find_mido_vpc_instance_global(mido_config *mido, char *instancename, mido_vpc **outvpc,
        mido_vpc_subnet **outvpcsubnet, mido_vpc_instance **outvpcinstance);

int populate_mido_vpc_natgateway(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, mido_vpc_natgateway *vpcnatgateway);
int create_mido_vpc_natgateway(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, mido_vpc_natgateway *vpcnatgateway);
int delete_mido_vpc_natgateway(mido_config *mido, mido_vpc_subnet *vpcsubnet, mido_vpc_natgateway *vpcnatgateway);
int find_mido_vpc_natgateway(mido_vpc *vpc, char *natgname, mido_vpc_subnet **outvpcsubnet, mido_vpc_natgateway **outvpcnatgateway);
int find_mido_vpc_natgateway_global(mido_config *mido, char *natgname, mido_vpc **outvpc, mido_vpc_subnet **outvpcsubnet, mido_vpc_natgateway **outvpcnatgateway);

midoname *find_mido_bridge_port_byinterface(midonet_api_bridge *br, char *name);
int find_mido_device_ports(midoname **ports, int max_ports, midoname *device, midoname ***outports, int *outports_max);
int find_mido_host_ports(midoname **ports, int max_ports, midoname *host, midoname ***outports, int *outports_max);
int find_mido_portgroup_ports(midoname **ports, int max_ports, midoname *portgroup, midoname ***outports, int *outports_max);
int parse_mido_vpc_subnet_cidr(mido_vpc_subnet *vpcsubnet, char **net, char **length);
int parse_mido_vpc_route_addr(midoname *route, char **srcnet, char **srclength, char **dstnet, char **dstlength);
int find_mido_vpc_subnet_routes(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet);

int populate_mido_vpc_secgroup(mido_config *mido, mido_vpc_secgroup *vpcsecgroup);
int create_mido_vpc_secgroup(mido_config *mido, mido_vpc_secgroup *vpcsecgroup);
int create_mido_vpc_secgroup_rule(midonet_api_chain *chain, midoname **outname,
        int pos, int ruletype, mido_parsed_chain_rule *rule);
int delete_mido_vpc_secgroup(mido_config *mido, mido_vpc_secgroup *vpcsecgroup);
int find_mido_vpc_secgroup(mido_config *mido, char *secgroupname, mido_vpc_secgroup **outvpcsecgroup);

int parse_mido_secgroup_rule(mido_config *mido, gni_rule *rule, mido_parsed_chain_rule *parsed_rule);
int parse_mido_secgroup_rule_v1(mido_config *mido, gni_rule *rule, mido_parsed_chain_rule *parsed_rule);
int parse_mido_secgroup_rule_v5(mido_config *mido, gni_rule *rule, mido_parsed_chain_rule *parsed_rule);
int clear_parsed_chain_rule(mido_parsed_chain_rule *rule);

int connect_mido_vpc_instance(mido_vpc_subnet *vpcsubnet, mido_vpc_instance *inst, char *instanceDNSDomain);
int disconnect_mido_vpc_instance(mido_vpc_subnet *subnet, mido_vpc_instance *vpcinstance);

int connect_mido_vpc_instance_elip(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, mido_vpc_instance *vpcinstance);
int disconnect_mido_vpc_instance_elip(mido_config *mido, mido_vpc *vpc, mido_vpc_instance *vpcinstance);

int connect_mido_vpc_instance_md(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, mido_vpc_instance *vpcinstance);
int disconnect_mido_vpc_instance_md(mido_config *mido, mido_vpc *vpc, mido_vpc_instance *vpcinstance);

int clear_mido_config(mido_config *mido);

int free_mido_config_v(mido_config *mido, int mode);
int free_mido_config(mido_config *mido);
int free_mido_core(mido_core *midocore);
int free_mido_md(mido_md *midomd);
int free_mido_vpc(mido_vpc *vpc);
int free_mido_vpc_subnet(mido_vpc_subnet *vpcsubnet);
int free_mido_vpc_instance(mido_vpc_instance *vpcinstance);
int free_mido_vpc_natgateway(mido_vpc_natgateway *vpcnatgateway);
int free_mido_vpc_secgroup(mido_vpc_secgroup *vpcsecgroup);

void print_mido_vpc(mido_vpc *vpc);
void print_mido_vpc_subnet(mido_vpc_subnet *vpcsubnet);
void print_mido_vpc_instance(mido_vpc_instance *vpcinstance);
void print_mido_vpc_secgroup(mido_vpc_secgroup *vpcsecgroup);

int do_midonet_maint(mido_config *mido);
int do_midonet_populate(mido_config *mido);
int do_midonet_populate_vpcs(mido_config *mido);
int do_midonet_update(globalNetworkInfo *gni, globalNetworkInfo *appliedGni, mido_config *mido);
int do_midonet_update_pass1(globalNetworkInfo *gni, globalNetworkInfo *appliedGni, mido_config *mido);
int do_midonet_update_pass2(globalNetworkInfo *gni, mido_config *mido);
int do_midonet_update_pass3_vpcs(globalNetworkInfo *gni, mido_config *mido);
int do_midonet_update_pass3_sgs(globalNetworkInfo *gni, mido_config *mido);
int do_midonet_update_pass3_insts(globalNetworkInfo *gni, mido_config *mido);

int do_midonet_teardown(mido_config *mido);
int do_midonet_delete_all(mido_config *mido);

int do_midonet_delete_dups(mido_config *mido, boolean checkonly);
int do_midonet_delete_vpc_object(mido_config *mido, char *id, boolean checkonly);

int do_midonet_delete_vpc(mido_config *mido, char *id, boolean checkonly);
int do_midonet_delete_vpcsubnet(mido_config *mido, char *id, boolean checkonly);
int do_midonet_delete_natg(mido_config *mido, char *id, boolean checkonly);
int do_midonet_delete_interface(mido_config *mido, char *id, boolean checkonly);
int do_midonet_delete_securitygroup(mido_config *mido, char *id, boolean checkonly);
int do_midonet_list(mido_config *mido);
int do_midonet_delete_unconnected(mido_config *mido, boolean checkonly);

void do_midonet_test(mido_config *mido);
int do_midonet_tag_midonames(mido_config *mido);

int do_metaproxy_disable(mido_config *mido);
int do_metaproxy_setup(mido_config *mido);
int do_metaproxy_teardown(mido_config *mido);
int do_metaproxy_maintain(mido_config *mido, int mode);

int create_mido_meta_core(mido_config *mido);
int create_mido_meta_vpc_namespace(mido_config *mido, mido_vpc *vpc);
int create_mido_meta_subnet_veth(mido_config *mido, mido_vpc *vpc, char *name, char *subnet, char *slashnet, char **tapiface);
int do_md_nginx_maintain(mido_config *mido, enum vpcmido_nginx_t);

int delete_mido_meta_core(mido_config *mido);
int delete_mido_meta_vpc_namespace(mido_config *mido, mido_vpc *vpc);
int delete_mido_meta_subnet_veth(mido_config *mido, char *name);

int read_mido_meta_vpc_namespace(mido_config *mido, mido_vpc *vpc);

#endif /* ! _INCLUDE_EUCA_TO_MIDO_H_ */
