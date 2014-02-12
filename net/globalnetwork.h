#ifndef INCLUDE_GLOBAL_NETWORK_H
#define INCLUDE_GLOBAL_NETWORK_H

#include <vnetwork.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

// AWS specifies max 50 rules per group
#define MAX_RULES_PER_GROUP                      256
#define MAX_NETWORK_INFO                         1048576
#define MAX_CLUSTERS                             NUMBER_OF_CCS
#define MAX_INSTANCES_PER_CLUSTER                MAXINSTANCES_PER_CC
#define MAX_INSTANCES                            MAX_INSTANCES_PER_CLUSTER * MAX_CLUSTERS
#define MAX_SECURITY_GROUPS                      MAX_INSTANCES
#define MAX_PRIVATE_IPS                          NUMBER_OF_PRIVATE_IPS
#define MAX_PUBLIC_IPS                           NUMBER_OF_PRIVATE_IPS
#define MAX_NON_EUCA_SUBNETS                     32

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct gni_securityGroup_t {
    char accountId[128], name[128], chainname[32];
    u32 member_ips[NUMBER_OF_PRIVATE_IPS];
    u32 member_public_ips[NUMBER_OF_PRIVATE_IPS];
    u8 member_macs[NUMBER_OF_PRIVATE_IPS][6];
    int member_local[NUMBER_OF_PRIVATE_IPS];
    int max_member_ips;
    char grouprules[MAX_RULES_PER_GROUP][1024];
    int max_grouprules;
} gni_securityGroup;

typedef struct gni_instance_t {
    char name[16];
    char accountId[128];
    u8 macAddress[6];
    u32 publicIp, privateIp;
    gni_securityGroup sec_groups[32];
} gni_instance;

typedef struct gni_subnet_t {
    u32 subnet, netmask, gateway;
} gni_subnet;

typedef struct gni_node_t {
    char name[HOSTNAME_SIZE];
    char dhcpdPath[MAX_PATH];
    char bridgeInterface[32];
    char publicInterface[32];
} gni_node;

typedef struct gni_cluster_t {
    char name[HOSTNAME_SIZE];
    u32 enabledCCIp;
    char macPrefix[8];
    gni_subnet private_subnet;
    u32 private_ips[MAX_PRIVATE_IPS];
} gni_cluster;

typedef struct globalNetworkInfo_t {
    char networkInfo[MAX_NETWORK_INFO];
    u32 enabledCLCIp;
    char instanceDNSDomain[HOSTNAME_SIZE];
    u32 public_ips[MAX_PUBLIC_IPS];
    gni_subnet subnets[MAX_CLUSTERS + MAX_NON_EUCA_SUBNETS];
} globalNetworkInfo;

#endif
