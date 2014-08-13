/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
 ************************************************************************/

#ifndef INCLUDE_GLOBAL_NETWORK_H
#define INCLUDE_GLOBAL_NETWORK_H

#include <libxml/tree.h>
#include <libxml/parser.h>
#include <libxml/xpath.h>
#include <libxml/xpathInternals.h>

#include <eucalyptus.h>
#include <data.h>
#include <euca_string.h>
#include <vnetwork.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MAX_NETWORK_INFO                         10485760
enum { GNI_ITERATE_PRINT, GNI_ITERATE_FREE };

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct gni_name_t {
    char name[1024];
} gni_name;

typedef struct gni_secgroup_t {
    char accountId[128], name[128], chainname[32];
    gni_name *grouprules;
    int max_grouprules;
    gni_name *instance_names;
    int max_instance_names;
} gni_secgroup;

typedef struct gni_instance_t {
    char name[16];
    char accountId[128];
    u8 macAddress[6];
    char vpc[16], subnet[16];
    u32 publicIp, privateIp;
    char node[HOSTNAME_SIZE], nodehostname[HOSTNAME_SIZE];
    gni_name *secgroup_names;
    int max_secgroup_names;
} gni_instance;

typedef struct gni_subnet_t {
    u32 subnet, netmask, gateway;
} gni_subnet;

typedef struct gni_node_t {
    char name[HOSTNAME_SIZE];
    gni_name *instance_names;
    int max_instance_names;
} gni_node;

typedef struct gni_cluster_t {
    char name[HOSTNAME_SIZE];
    u32 enabledCCIp;
    char macPrefix[8];
    gni_subnet private_subnet;
    u32 *private_ips;
    int max_private_ips;
    gni_node *nodes;
    int max_nodes;
} gni_cluster;

typedef struct gni_network_acl_t {
} gni_network_acl;

typedef struct gni_route_table_t {
} gni_route_table;

typedef struct gni_internet_gateway_t {
} gni_internet_gateway;

typedef struct gni_vpcsubnet_t {
  char name[16];
  char accountId[128];
  char cidr[24];
  char cluster_name[HOSTNAME_SIZE];
  char networkAcl_name[16];
  char routeTable_name[16];
} gni_vpcsubnet;

typedef struct gni_vpc_t {
  char name[16];
  char accountId[128];
  char cidr[24];
  char dhcpOptionSet[16];
  gni_vpcsubnet *subnets;
  int max_subnets;
  gni_network_acl *networkAcls;
  int max_networkAcls;
  gni_route_table *routeTables;
  int max_routeTables;
  gni_internet_gateway *internetGateways;
  int max_internetGateways;
} gni_vpc;

typedef struct globalNetworkInfo_t {
    int init;
    char networkInfo[MAX_NETWORK_INFO];
    u32 enabledCLCIp;
    char instanceDNSDomain[HOSTNAME_SIZE];
    u32 *instanceDNSServers;
    int max_instanceDNSServers;
    u32 *public_ips;
    int max_public_ips;
    gni_subnet *subnets;
    int max_subnets;
    gni_cluster *clusters;
    int max_clusters;
    gni_instance *instances;
    int max_instances;
    gni_secgroup *secgroups;
    int max_secgroups;
    gni_vpc *vpcs;
    int max_vpcs;
} globalNetworkInfo;

globalNetworkInfo *gni_init(void);
int gni_populate(globalNetworkInfo * gni, char *xmlpath);
int gni_print(globalNetworkInfo * gni);
int gni_clear(globalNetworkInfo * gni);
int gni_free(globalNetworkInfo * gni);
int gni_iterate(globalNetworkInfo * gni, int mode);

int gni_cluster_clear(gni_cluster * cluster);
int gni_node_clear(gni_node * node);
int gni_instance_clear(gni_instance * instance);
int gni_secgroup_clear(gni_secgroup * secgroup);
int gni_vpc_clear(gni_vpc *vpc);

int gni_is_self(char *test_ip);
int gni_find_self_node(globalNetworkInfo * gni, gni_node ** outnodeptr);
int gni_find_self_cluster(globalNetworkInfo * gni, gni_cluster ** outclusterptr);
int gni_secgroup_get_chainname(globalNetworkInfo * gni, gni_secgroup * secgroup, char **outchainname);

int gni_cloud_get_clusters(globalNetworkInfo * gni, char **cluster_names, int max_cluster_names, char ***out_cluster_names, int *out_max_cluster_names, gni_cluster ** out_clusters,
                           int *out_max_clusters);
int gni_cluster_get_nodes(globalNetworkInfo * gni, gni_cluster * cluster, char **node_names, int max_node_names, char ***out_node_names, int *out_max_node_names,
                          gni_node ** out_nodes, int *out_max_nodes);
int gni_node_get_instances(globalNetworkInfo * gni, gni_node * node, char **instance_names, int max_instance_names, char ***out_instance_names, int *out_max_instance_names,
                           gni_instance ** out_instances, int *out_max_instances);
int gni_instance_get_secgroups(globalNetworkInfo * gni, gni_instance * instance, char **secgroup_names, int max_secgroup_names, char ***out_secgroup_names,
                               int *out_max_secgroup_names, gni_secgroup ** out_secgroups, int *out_max_secgroups);
int gni_secgroup_get_instances(globalNetworkInfo * gni, gni_secgroup * secgroup, char **instance_names, int max_instance_names, char ***out_instance_names,
                               int *out_max_instance_names, gni_instance ** out_instances, int *out_max_instances);

int gni_validate(globalNetworkInfo * gni);
int gni_subnet_validate(gni_subnet * subnet);
int gni_cluster_validate(gni_cluster * cluster);
int gni_node_validate(gni_node * node);
int gni_instance_validate(gni_instance * instance);
int gni_secgroup_validate(gni_secgroup * secgroup);

int gni_serialize_iprange_list(char **inlist, int inmax, u32 ** outlist, int *outmax);
int evaluate_xpath_property(xmlXPathContextPtr ctxptr, char *expression, char ***results, int *max_results);
int evaluate_xpath_element(xmlXPathContextPtr ctxptr, char *expression, char ***results, int *max_results);

int ruleconvert(char *rulebuf, char *outrule);

#endif
