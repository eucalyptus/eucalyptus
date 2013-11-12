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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct sec_group_t {
    char accountId[128], name[128], chainname[32];
    u32 member_ips[NUMBER_OF_PRIVATE_IPS];
    u32 member_public_ips[NUMBER_OF_PRIVATE_IPS];
    u8 member_macs[NUMBER_OF_PRIVATE_IPS][6];
    int member_local[NUMBER_OF_PRIVATE_IPS];
    int max_member_ips;
    char grouprules[MAX_RULES_PER_GROUP][1024];
    int max_grouprules;
} sec_group;

typedef struct globalNetworkInfo_t {
  char networkInfo[MAX_NETWORK_INFO];
} globalNetworkInfo;

#endif
