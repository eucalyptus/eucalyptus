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

#ifndef _INCLUDE_EUCA_GNI_H_
#define _INCLUDE_EUCA_GNI_H_

//!
//! @file net/euca_gni.h
//! Defines the global network interface
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <libxml/tree.h>
#include <libxml/parser.h>
#include <libxml/xpath.h>
#include <libxml/xpathInternals.h>

#include <eucalyptus.h>
#include <data.h>
#include <euca_string.h>
#include <euca_network.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MAX_NETWORK_INFO_LEN                 52428800   //!< The maximum length of the network info string in GNI structure

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

typedef enum gni_iterate_mode_t {
    GNI_ITERATE_PRINT,
    GNI_ITERATE_FREE
} gni_iterate_mode;

enum gni_populate_mode_t {
    GNI_POPULATE_ALL,
    GNI_POPULATE_CONFIG,
    GNI_POPULATE_NONE,
};

typedef enum gni_xpath_node_t gni_xpath_node_type;
enum gni_xpath_node_t {
    GNI_XPATH_CONFIGURATION,
    GNI_XPATH_VPCS,
    GNI_XPATH_INSTANCES,
    GNI_XPATH_DHCPOPTIONSETS,
    GNI_XPATH_INTERNETGATEWAYS,
    GNI_XPATH_SECURITYGROUPS,
    GNI_XPATH_INVALID
};

enum gni_config_diff_t {
    GNI_CONFIG_DIFF_ENABLEDCLCIP       = 0x00000001,
    GNI_CONFIG_DIFF_INSTANCEDNSDOMAIN  = 0x00000002,
    GNI_CONFIG_DIFF_INSTANCEDNSSERVERS = 0x00000004,
    GNI_CONFIG_DIFF_MIDOGATEWAYS       = 0x00000008,
    GNI_CONFIG_DIFF_MIDONODES          = 0x00000010,
    GNI_CONFIG_DIFF_SUBNETS            = 0x00000020,
    GNI_CONFIG_DIFF_OTHER              = 0x80000000,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct gni_instance_t gni_instance;
typedef struct gni_secgroup_t gni_secgroup;

//! GNI element name structure
typedef struct gni_name_32_t {
    char name[32];
} gni_name_32;

typedef struct gni_name_256_t {
    char name[256];
} gni_name_256;

typedef struct gni_name_1024_t {
    char name[1024];                   //!< GNI element name string
} gni_name_1024;

//! GNI Mido Gateway
typedef struct gni_mido_gateway_t {
    char host[HOSTNAME_LEN];
    char ext_ip[NETWORK_ADDR_LEN];
    char ext_dev[IF_NAME_LEN];
    char ext_cidr[NETWORK_ADDR_LEN];
    char peer_ip[NETWORK_ADDR_LEN];
    u32 peer_asn;
    u32 asn;
    char **ad_routes;
    int max_ad_routes;
    void *mido_present;
} gni_mido_gateway;

//! GNI SG Rule
typedef struct gni_rule_t {
    int protocol;
    int fromPort;
    int toPort;
    int icmpType;
    int icmpCode;
    int cidrSlashnet;
    u32 cidrNetaddr;
    char cidr[NETWORK_ADDR_LEN];
    char groupId[SECURITY_GROUP_ID_LEN];
    char groupOwnerId[OWNER_ID_LEN];
} gni_rule;

//! GNI Network ACL entry
typedef struct gni_acl_entry_t {
    int number;
    int allow;
    int protocol;
    int fromPort;
    int toPort;
    int icmpType;
    int icmpCode;
    int cidrSlashnet;
    u32 cidrNetaddr;
    char cidr[NETWORK_ADDR_LEN];
} gni_acl_entry;

//! GNI Instance Information structure
struct gni_instance_t {
    char name[INTERFACE_ID_LEN];              //!< Instance ID string
    char ifname[INTERFACE_ID_LEN];            //!< Interface ID string
    char attachmentId[ENI_ATTACHMENT_ID_LEN]; //!< Attachment ID string
    char accountId[OWNER_ID_LEN];             //!< Instance Account ID string
    u8 macAddress[ENET_BUF_SIZE];             //!< Associated MAC address
    u32 publicIp;                             //!< Assigned public IP address
    u32 privateIp;                            //!< Assigned private IP address
    char vpc[VPC_ID_LEN];                     //!< VPC ID associated with this interface
    char subnet[VPC_SUBNET_ID_LEN];           //!< subnet ID associated with this interface
    char node[HOSTNAME_LEN];
    boolean srcdstcheck;                      //!< Source/Destination Check flag (only for interfaces)
    int deviceidx;                            //!< NIC device index (only for interfaces)
    gni_name_32 instance_name;                //!< Instance name associated
    gni_name_32 *secgroup_names;              //!< List of associated security group names
    int max_secgroup_names;                   //!< Number of security group names in the list
    gni_instance **interfaces;
    int max_interfaces;
    gni_secgroup **gnisgs;
    void *mido_present;                       //!< mido datastructure that implements this gni_instance
    void *mido_vpc;
    void *mido_vpcsubnet;
};

//! GNI Security Group Information structure
struct gni_secgroup_t {
    char accountId[OWNER_ID_LEN];      //!< Security Group Account ID string
    char name[SECURITY_GROUP_ID_LEN];  //!< Security Group Name string (i.e. sg-xxxxxxxx)
    gni_rule *ingress_rules;
    int max_ingress_rules;
    gni_rule *egress_rules;
    int max_egress_rules;
    gni_instance **instances;
    int max_instances;
    gni_instance **interfaces;
    int max_interfaces;
    void *mido_present;
};

//! GNI Subnet Information Structure
typedef struct gni_subnet_t {
    u32 subnet;                        //!< Subnet address
    u32 netmask;                       //!< Netmask address
    u32 gateway;                       //!< Gateway address
} gni_subnet;

//! GNI Node Information Structure
typedef struct gni_node_t {
    char name[HOSTNAME_LEN];           //!< The Node name
    gni_name_32 *instance_names;       //!< A list of associated instance names
    int max_instance_names;            //!< Number of instance names in the list
} gni_node;

//! GNI Cluster Information Structure
typedef struct gni_cluster_t {
    char name[HOSTNAME_LEN];            //!< The Cluster name
    u32 enabledCCIp;                    //!< The enabled CC IP address
    char macPrefix[ENET_MACPREFIX_LEN]; //!< The MAC address prefix to use for instances
    gni_subnet private_subnet;          //!< Cluster Subnet Information
    char **private_ips_str;
    int max_private_ips_str;
    u32 *private_ips;                   //!< List of private IPs associated with this cluster
    int max_private_ips;                //!< Number of private IPs in the list
    gni_node *nodes;                    //!< List of associated nodes information
    int max_nodes;                      //!< Number of nodes in the lsit
} gni_cluster;

typedef struct gni_network_acl_t {
    char accountId[OWNER_ID_LEN];
    char name[NETWORK_ACL_ID_LEN];
    gni_acl_entry *ingress;
    int max_ingress;
    gni_acl_entry *egress;
    int max_egress;
    int changed;
    void *mido_present;
} gni_network_acl;

typedef struct gni_dhcp_os_t {
    char accountId[OWNER_ID_LEN];
    char name[DHCP_OS_ID_LEN];
    u32 *dns;
    int max_dns;
    u32 *ntp;
    int max_ntp;
    u32 *netbios_ns;
    int max_netbios_ns;
    gni_name_256 *domains;
    int max_domains;
    int netbios_type;
    int changed;
} gni_dhcp_os;

typedef struct gni_route_entry_t {
    char destCidr[NETWORK_ADDR_LEN];
    char target[LID_LEN];
} gni_route_entry;

typedef struct gni_route_table_t {
    char name[RTB_ID_LEN];
    char accountId[OWNER_ID_LEN];
    gni_route_entry *entries;
    int max_entries;
    int changed;
} gni_route_table;

typedef struct gni_internet_gateway_t {
    char name[INETG_ID_LEN];
    char accountId[OWNER_ID_LEN];
} gni_internet_gateway;

typedef struct gni_nat_gateway_t {
    char name[NATG_ID_LEN];
    char accountId[OWNER_ID_LEN];
    u8 macAddress[ENET_BUF_SIZE];
    u32 publicIp;
    u32 privateIp;
    char vpc[VPC_ID_LEN];
    char subnet[VPC_SUBNET_ID_LEN];
    void *mido_present;
} gni_nat_gateway;

typedef struct gni_vpcsubnet_t {
    char name[VPC_SUBNET_ID_LEN];
    char accountId[OWNER_ID_LEN];
    char cidr[NETWORK_ADDR_LEN];
    char cluster_name[HOSTNAME_LEN];
    char networkAcl_name[NETWORK_ACL_ID_LEN];
    char routeTable_name[RTB_ID_LEN];
    gni_instance **interfaces;
    gni_route_table *routeTable;
    gni_network_acl *networkAcl;
    int *rt_entry_applied;
    int max_interfaces;
    void *mido_present;
} gni_vpcsubnet;

typedef struct gni_vpc_t {
    char name[VPC_ID_LEN];
    char accountId[OWNER_ID_LEN];
    char cidr[NETWORK_ADDR_LEN];
    char dhcpOptionSet_name[DHCP_OS_ID_LEN];
    gni_dhcp_os *dhcpOptionSet;
    gni_vpcsubnet *subnets;
    int max_subnets;
    gni_network_acl *networkAcls;
    int max_networkAcls;
    gni_route_table *routeTables;
    int max_routeTables;
    gni_nat_gateway *natGateways;
    int max_natGateways;
    gni_name_32 *internetGatewayNames;
    int max_internetGatewayNames;
    gni_instance **interfaces;
    int max_interfaces;
    void *mido_present;
} gni_vpc;

typedef struct gni_hostname_t {
    struct in_addr ip_address;
    char hostname[HOSTNAME_SIZE];
} gni_hostname;

typedef struct gni_hostname_info_t {
    gni_hostname *hostnames;
    int max_hostnames;
} gni_hostname_info;

//! Global GNI Information Structure
typedef struct globalNetworkInfo_t {
    boolean init;                           //!< has the structure been initialized successfully?
    char networkInfo[MAX_NETWORK_INFO_LEN]; //!< XML content used to build this structure
    char version[GNI_VERSION_LEN];          //!< latest version ID of the document
    char appliedVersion[GNI_VERSION_LEN];   //!< latest known applied version ID of the document
    char sMode[NETMODE_LEN];                //!< The network mode string passed in the GNI
    euca_netmode nmCode;                    //!< The network mode code (see euca_netmode_t)
    u32 enabledCLCIp;                       //!< IP address of the enabled CLC
    char instanceDNSDomain[HOSTNAME_LEN];   //!< The DNS domain name to use for the instances
    u32 *instanceDNSServers;                //!< List of DNS servers
    int max_instanceDNSServers;             //!< Number of DNS servers in the list
    // u32 publicGateway;                      //!< Public network default gateway
    u32 *public_ips;                        //!< List of associated public IPs
    int max_public_ips;                     //!< Number of associated public IPs in the list
    char **public_ips_str;
    int max_public_ips_str;
    gni_mido_gateway *midogws;
    int max_midogws;
    gni_subnet *subnets;                    //!< List of global subnet information
    int max_subnets;                        //!< Number of global subnets in the list
    gni_cluster *clusters;                  //!< List of clusters information
    int max_clusters;                       //!< Number of clusters in the list
    gni_instance **instances;               //!< List of instances information
    int max_instances;                      //!< Number of instances in the list
    boolean sorted_instances;
    gni_instance **ifs;                     //!< List of interfaces information
    int max_ifs;                            //!< Number of interfaces in the list
    gni_secgroup *secgroups;                //!< List of security group information
    int max_secgroups;                      //!< Number of security groups in the list
    gni_vpc *vpcs;                          //!< List of VPC information
    int max_vpcs;                           //!< Number of VPCs
    gni_internet_gateway *vpcIgws;          //!< List of VPC Internet Gateways
    int max_vpcIgws;                        //!< Number of VPC Internet Gateways
    gni_dhcp_os *dhcpos;                    //!< List of DHCP Options Set information
    int max_dhcpos;                         //!< Number of DHCP Option Sets
} globalNetworkInfo;

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

globalNetworkInfo *gni_init(void);
int gni_free(globalNetworkInfo *gni);
int gni_clear(globalNetworkInfo *gni);
int gni_print(globalNetworkInfo *gni, log_level_e llevel);
int gni_iterate(globalNetworkInfo *gni, gni_iterate_mode mode, log_level_e llevel);
int gni_populate(globalNetworkInfo *gni, gni_hostname_info *host_info, char *xmlpath);
int gni_populate_v(int mode, globalNetworkInfo *gni, gni_hostname_info *host_info, char *xmlpath);
int gni_populate_xpathnodes(xmlDocPtr doc, xmlNode **gni_nodes);
gni_xpath_node_type gni_xmlstr2type(const xmlChar *nodename);
int gni_populate_gnidata(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_configuration(globalNetworkInfo *gni, gni_hostname_info *host_info, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_instances(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_interfaces(globalNetworkInfo *gni, gni_instance *instance, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_instance_interface(gni_instance *instance, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_sgs(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_rule(gni_rule *rule, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_vpcs(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_vpc(gni_vpc *vpc, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_routetable(gni_vpc *vpc, gni_route_table *routetable, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_route(gni_route_entry *route, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_vpcsubnet(gni_vpc *vpc, gni_vpcsubnet *vpcsubnet, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_natgateway(gni_nat_gateway *natg, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_networkacl(gni_network_acl *netacl, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_aclentry(gni_acl_entry *aclentry, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_internetgateways(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);
int gni_populate_dhcpos(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc);

int gni_is_self(const char *test_ip);
int gni_is_self_getifaddrs(const char *test_ip);

int gni_cluster_clear(gni_cluster *cluster);
int gni_node_clear(gni_node *node);
int gni_instance_clear(gni_instance *instance);
int gni_secgroup_clear(gni_secgroup *secgroup);
int gni_vpc_clear(gni_vpc *vpc);
int gni_dhcpos_clear(gni_dhcp_os *dhcpos);
int gni_midogw_clear(gni_mido_gateway *midogw);
int gni_midogw_dup(gni_mido_gateway *dst, gni_mido_gateway *src);

int gni_find_self_node(globalNetworkInfo *gni, gni_node **outnodeptr);
int gni_find_self_cluster(globalNetworkInfo *gni, gni_cluster **outclusterptr);
int gni_find_secgroup(globalNetworkInfo *gni, const char *psGroupId, gni_secgroup **pSecGroup);
int gni_find_instance(globalNetworkInfo *gni, const char *psInstanceId, gni_instance **pInstance);
int gni_find_secondary_interfaces(globalNetworkInfo *gni, const char *psInstanceId, gni_instance *pAInstances[], int *size);

int gni_cloud_get_clusters(globalNetworkInfo *gni, char **cluster_names, int max_cluster_names, char ***out_cluster_names, int *out_max_cluster_names, gni_cluster **out_clusters,
                           int *out_max_clusters);
int gni_cloud_get_secgroups(globalNetworkInfo *pGni, char **psSecGroupNames, int nbSecGroupNames, char ***psOutSecGroupNames, int *pOutNbSecGroupNames,
                           gni_secgroup **pOutSecGroups, int *pOutNbSecGroups);
int gni_cluster_get_nodes(globalNetworkInfo *gni, gni_cluster *cluster, char **node_names, int max_node_names, char ***out_node_names, int *out_max_node_names,
                          gni_node **out_nodes, int *out_max_nodes);
int gni_cluster_get_instances(globalNetworkInfo *pGni, gni_cluster *pCluster, char **psInstanceNames, int maxInstanceNames, char ***psOutInstanceNames, int *pOutNbInstanceNames,
                              gni_instance **pOutInstances, int *pOutNbInstances);
int gni_cluster_get_secgroup(globalNetworkInfo *pGni, gni_cluster *pCluster, char **psSecGroupNames, int nbSecGroupNames, char ***psOutSecGroupNames, int *pOutNbSecGroupNames,
                             gni_secgroup **pOutSecGroups, int *pOutNbSecGroups);
int gni_node_get_instances(globalNetworkInfo *gni, gni_node *node, char **instance_names, int max_instance_names, char ***out_instance_names, int *out_max_instance_names,
                           gni_instance **out_instances, int *out_max_instances);
int gni_node_get_secgroup(globalNetworkInfo *pGni, gni_node *pNode, char **psSecGroupNames, int nbSecGroupNames, char ***psOutSecGroupNames, int *pOutNbSecGroupNames,
                          gni_secgroup **pOutSecGroups, int *pOutNbSecGroups);
int gni_instance_get_secgroups(globalNetworkInfo *gni, gni_instance *instance, char **secgroup_names, int max_secgroup_names, char ***out_secgroup_names,
                               int *out_max_secgroup_names, gni_secgroup **out_secgroups, int *out_max_secgroups);
int gni_secgroup_get_instances(globalNetworkInfo *gni, gni_secgroup *secgroup, char **instance_names, int max_instance_names, char ***out_instance_names,
                               int *out_max_instance_names, gni_instance **out_instances, int *out_max_instances);
int gni_secgroup_get_interfaces(globalNetworkInfo *gni, gni_secgroup *secgroup,
        char **interface_names, int max_interface_names, char ***out_interface_names,
        int *out_max_interface_names, gni_instance ***out_interfaces, int *out_max_interfaces);
int gni_secgroup_get_chainname(globalNetworkInfo *gni, gni_secgroup *secgroup, char **outchainname);

int gni_get_secgroups_from_instances(globalNetworkInfo *gni, gni_instance *instances,
        int max_instances, gni_secgroup ***out_secgroups, int *max_out_secgroups);
int gni_get_referenced_secgroups(globalNetworkInfo *gni, gni_secgroup **sgs,
        int max_sgs, gni_secgroup ***out_secgroups, int *max_out_secgroups);

gni_route_table *gni_vpc_get_routeTable(gni_vpc *vpc, const char *tableName);
gni_vpcsubnet *gni_vpc_get_vpcsubnet(gni_vpc *vpc, const char *vpcsubnetName);
int gni_vpc_get_interfaces(globalNetworkInfo *gni, gni_vpc *vpc, gni_instance ***out_interfaces, int *max_out_interfaces);
int gni_vpcsubnet_get_interfaces(globalNetworkInfo *gni, gni_vpcsubnet *vpcsubnet,
        gni_instance **vpcinterfaces, int max_vpcinterfaces, gni_instance ***out_interfaces, int *max_out_interfaces);

gni_vpc *gni_get_vpc(globalNetworkInfo *gni, char *name, int *startidx);
gni_vpcsubnet *gni_get_vpcsubnet(gni_vpc *vpc, char *name, int *startidx);
gni_instance *gni_get_interface(gni_vpcsubnet *vpcsubnet, char *name, int *startidx);
gni_instance *gni_get_interface_by_shortid(gni_vpcsubnet *vpcsubnet, char *name, int *startidx);
gni_nat_gateway *gni_get_natgateway(gni_vpc *vpc, char *name, int *startidx);
gni_route_table *gni_get_routetable(gni_vpc *vpc, char *name, int *startidx);
gni_secgroup *gni_get_secgroup(globalNetworkInfo *gni, char *name, int *startidx);
gni_network_acl *gni_get_networkacl(gni_vpc *vpc, char *name, int *startidx);
gni_dhcp_os *gni_get_dhcpos(globalNetworkInfo *gni, char *name, int *startidx);

int gni_validate(globalNetworkInfo *gni);
int gni_netmode_validate(const char *psMode);
int gni_subnet_validate(gni_subnet *subnet);
int gni_cluster_validate(gni_cluster *cluster, euca_netmode nmode);
int gni_node_validate(gni_node *node);
int gni_instance_validate(gni_instance *instance);
int gni_interface_validate(gni_instance *interface);
int gni_secgroup_validate(gni_secgroup *secgroup);
int gni_vpc_validate(gni_vpc *vpc);
int gni_vpcsubnet_validate(gni_vpcsubnet *vpcsubnet);
int gni_nat_gateway_validate(gni_nat_gateway *natg);
int gni_route_table_validate(gni_route_table *rtable);
int gni_networkacl_validate(gni_network_acl *acl);

int gni_serialize_iprange_list(char **inlist, int inmax, u32 **outlist, int *outmax);
int evaluate_xpath_property(xmlXPathContextPtr ctxptr, xmlDocPtr doc, xmlNodePtr startnode, char *expression, char ***results, int *max_results);
int evaluate_xpath_element(xmlXPathContextPtr ctxptr, xmlDocPtr doc, xmlNodePtr startnode, char *expression, char ***results, int *max_results);
int evaluate_xpath_nodeset(xmlXPathContextPtr ctxptr, xmlDocPtr doc, xmlNodePtr startnode, char *expression, xmlNodeSetPtr nodeset);

void gni_instance_interface_print(gni_instance *inst, int loglevel);
void gni_sg_print(gni_secgroup *sg, int loglevel);
void gni_vpc_print(gni_vpc *vpc, int loglevel);
void gni_internetgateway_print(gni_internet_gateway *ig, int loglevel);
void gni_dhcpos_print(gni_dhcp_os *dhcpos, int loglevel);

gni_hostname_info *gni_init_hostname_info(void);
int gni_hostnames_print(gni_hostname_info *host_info);
int gni_hostnames_free(gni_hostname_info *host_info);
int gni_hostnames_get_hostname(gni_hostname_info *host_info, const char *ip_address, char **hostname);
int cmpipaddr(const void *p1, const void *p2);

int cmp_gni_config(globalNetworkInfo *a, globalNetworkInfo *b);
int cmp_gni_vpc(gni_vpc *a, gni_vpc *b);
int cmp_gni_vpcsubnet(gni_vpcsubnet *a, gni_vpcsubnet *b, int *nacl_diff);
int cmp_gni_nat_gateway(gni_nat_gateway *a, gni_nat_gateway *b);
int cmp_gni_route_table(gni_route_table *a, gni_route_table *b);
int cmp_gni_secgroup(gni_secgroup *a, gni_secgroup *b, int *ingress_diff, int *egress_diff, int *interfaces_diff);
int cmp_gni_nacl(gni_network_acl *a, gni_network_acl *b, int *ingress_diff, int *egress_diff);
int cmp_gni_interface(gni_instance *a, gni_instance *b, int *pubip_diff, int *sdc_diff, int *host_diff, int *sg_diff);
int cmp_gni_instance(gni_instance *a, gni_instance *b);
int cmp_gni_mido_gateway(gni_mido_gateway *a, gni_mido_gateway *b);

int ruleconvert(char *rulebuf, char *outrule);
int ingress_gni_to_iptables_rule(char *scidr, gni_rule *iggnirule, char *outrule, int flags);

int compare_gni_instance_name(const void *p1, const void *p2);

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

//! A macro equivalent to the gni_free() call and ensures the given pointer is set to NULL
#define GNI_FREE(_pGni) \
{                       \
    gni_free(_pGni);    \
    (_pGni) = NULL;     \
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_EUCA_GNI_H_ */
