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
    u32 publicIp, privateIp;
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
