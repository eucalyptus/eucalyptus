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
//! @file net/euca_gni.c
//! Implementation of the global network interface
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <stdarg.h>
#include <sys/types.h>
#include <dirent.h>
#include <linux/limits.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <ifaddrs.h>

#include <eucalyptus.h>
#include <misc.h>
#include <hash.h>
#include <euca_string.h>
#include <euca_network.h>
#include <atomic_file.h>

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ebt_handler.h"
#include "dev_handler.h"
#include "euca_gni.h"
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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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
#define TCP_PROTOCOL_NUMBER 6
#define UDP_PROTOCOL_NUMBER 17
#define ICMP_PROTOCOL_NUMBER 1
static boolean xml_initialized = FALSE;    //!< To determine if the XML library has been initialized

#ifndef XML_INIT                    // if compiling as a stand-alone binary (for unit testing)
#define XML_INIT() if (!xml_initialized) { xmlInitParser(); xml_initialized = TRUE; }
#endif

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
 * Creates a unique IP table chain name for a given security group. This name, if successful
 * will have the form of EU_[hash] where [hash] is the 64 bit encoding of the resulting
 * "[account id]-[group name]" string from the given security group information.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param secgroup [in] a pointer to the security group for which we compute the chain name
 * @param outchainname [out] a pointer to the string that will contain the computed name
 *
 * @return 0 on success or 1 on failure
 *
 * @pre
 *     The outchainname parameter Must not be NULL but it should point to a NULL value. If
 *     this does not point to NULL, the memory will be lost when replaced with the out value.
 *
 * @post
 *     On success, the outchainname will point to the resulting string. If a failure occur, any
 *     value pointed by outchainname is non-deterministic.
 *
 * @note
 *     Chain name deprecated since Eucalyptus 4.4
 */
int gni_secgroup_get_chainname(globalNetworkInfo *gni, gni_secgroup *secgroup, char **outchainname) {
    char hashtok[16 + 128 + 1];
    char chainname[48];
    char *chainhash = NULL;

    if (!gni || !secgroup || !outchainname) {
        LOGWARN("invalid argument: cannot get chainname from NULL\n");
        return (1);
    }

    snprintf(hashtok, OWNER_ID_LEN + SECURITY_GROUP_ID_LEN + 1, "%s-%s", secgroup->accountId, secgroup->name);
    hash_b64enc_string(hashtok, &chainhash);
    if (chainhash) {
        snprintf(chainname, 48, "EU_%s", chainhash);
        *outchainname = strdup(chainname);
        EUCA_FREE(chainhash);
        return (0);
    }
    LOGERROR("could not create iptables compatible chain name for sec. group (%s)\n", secgroup->name);
    return (1);
}

/**
 * Retrieves security groups associated with the given list of instances.
 * @param gni [in] pointer to global network information structure
 * @param instances [in] pointer to array of instances structure of interest
 * @param max_instances [in] number of entries in the array of instances
 * @param out_secgroups [out] pointer to the list of structures representing security
 * groups that were found. Caller is responsible to release the allocated memory
 * @param max_out_secgroups [out] number of found security groups
 * @return 0 on success, 1 otherwise
 */
int gni_get_secgroups_from_instances(globalNetworkInfo *gni, gni_instance *instances,
        int max_instances, gni_secgroup ***out_secgroups, int *max_out_secgroups) {
    char **sgnames = NULL;
    int sgnames_max = 0;
    gni_secgroup **result = NULL;
    if (!instances || !out_secgroups || !gni) {
        LOGWARN("Invalid argument: cannot get sgs from NULL\n");
        return (1);
    }
    *max_out_secgroups = 0;
    for (int i = 0; i < max_instances; i++) {
        for (int j = 0; j < instances[i].max_secgroup_names; j++) {
            euca_string_set_insert(&sgnames, &sgnames_max, instances[i].secgroup_names[j].name);
        }
    }
    result = EUCA_ZALLOC_C(sgnames_max, sizeof(gni_secgroup *));
    int sgcount = 0;
    for (int i = 0; i < sgnames_max; i++) {
        result[sgcount] = gni_get_secgroup(gni, sgnames[i], NULL);
        if (result[sgcount] == NULL) {
            LOGWARN("%s not found in global network view.\n", sgnames[i]);
        } else {
            sgcount++;
        }
        EUCA_FREE(sgnames[i]);
    }
    EUCA_FREE(sgnames);
    *out_secgroups = result;
    *max_out_secgroups = sgcount;
    return (0);
}

/**
 * Retrieves security groups that are referenced by the given list of security groups.
 * @param gni [in] pointer to global network information structure
 * @param sgs [in] pointer to array of security group structures of interest
 * @param max_sgs [in] number of entries in the array of instances
 * @param out_secgroups [out] pointer to the list of structures representing security
 * groups that were found. Caller is responsible to release the allocated memory
 * @param max_out_secgroups [out] number of found security groups
 * @return 0 on success, 1 otherwise
 */
int gni_get_referenced_secgroups(globalNetworkInfo *gni, gni_secgroup **sgs,
        int max_sgs, gni_secgroup ***out_secgroups, int *max_out_secgroups) {
    char **sgnames = NULL;
    int sgnames_max = 0;
    gni_secgroup **result = NULL;
    if (!sgs || !out_secgroups || !gni) {
        LOGWARN("Invalid argument: cannot get referenced sgs from NULL\n");
        return (1);
    }
    *max_out_secgroups = 0;
    for (int i = 0; i < max_sgs; i++) {
        for (int j = 0; j < sgs[i]->max_ingress_rules; j++) {
            if (strlen(sgs[i]->ingress_rules[j].groupId) > 0) {
                euca_string_set_insert(&sgnames, &sgnames_max, sgs[i]->ingress_rules[j].groupId);
            }
        }
        for (int j = 0; j < sgs[i]->max_egress_rules; j++) {
            if (strlen(sgs[i]->egress_rules[j].groupId) > 0) {
                euca_string_set_insert(&sgnames, &sgnames_max, sgs[i]->egress_rules[j].groupId);
            }
        }
    }
    result = EUCA_ZALLOC_C(sgnames_max, sizeof(gni_secgroup *));
    int sgcount = 0;
    for (int i = 0; i < sgnames_max; i++) {
        result[sgcount] = gni_get_secgroup(gni, sgnames[i], NULL);
        if (result[sgcount] == NULL) {
            LOGWARN("%s not found in global network view.\n", sgnames[i]);
        } else {
            sgcount++;
        }
        EUCA_FREE(sgnames[i]);
    }
    EUCA_FREE(sgnames);
    *out_secgroups = result;
    *max_out_secgroups = sgcount;
    return (0);
}

/**
 * Searches and returns a pointer to the route table data structure given its name in the argument..
 *
 * @param vpc [in] a pointer to the vpc gni data structure
 * @param tableName [in] name of the route table of interest
 *
 * @return pointer to the gni route table of interest if found. NULL otherwise
 *
 * @pre
 *     gni data structure is assumed to be populated.
 */
gni_route_table *gni_vpc_get_routeTable(gni_vpc *vpc, const char *tableName) {
    int i = 0;
    boolean found = FALSE;
    gni_route_table *result = NULL;
    for (i = 0; i < vpc->max_routeTables && !found; i++) {
        if (strcmp(tableName, vpc->routeTables[i].name) == 0) {
            result = &(vpc->routeTables[i]);
            found = TRUE;
        }
    }
    return (result);
}

/**
 * Searches and returns a pointer to the VPC subnet data structure given its name in the argument.
 *
 * @param vpc [in] a pointer to the vpc gni data structure
 * @param vpcsubnetName [in] name of the subnet of interest
 *
 * @return pointer to the gni vpcsubnet of interest if found. NULL otherwise
 *
 * @pre
 *     gni data structure is assumed to be populated.
 */
gni_vpcsubnet *gni_vpc_get_vpcsubnet(gni_vpc *vpc, const char *vpcsubnetName) {
    int i = 0;
    boolean found = FALSE;
    gni_vpcsubnet *result = NULL;
    for (i = 0; i < vpc->max_subnets && !found; i++) {
        if (strcmp(vpcsubnetName, vpc->subnets[i].name) == 0) {
            result = &(vpc->subnets[i]);
            found = TRUE;
        }
    }
    return (result);
}

/**
 * Searches and returns an array of pointers to gni_instance data structures (holding
 * interface information) that are associated with the given VPC.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param vpc [in] a pointer to the vpc gni data structure of interest
 * @param out_interfaces [out] a list of pointers to interfaces of interest
 * @param max_out_interfaces [out] number of interfaces found
 *
 * @return 0 if the search is successfully executed - 0 interfaces found is still
 *         a successful search. 1 otherwise. 
 *
 * @pre
 *     gni data structure is assumed to be populated.
 *     out_interfaces should be free of memory allocations.
 *
 * @post
 *     memory allocated to hold the resulting list of interfaces should be released
 *     by the caller.
 */
int gni_vpc_get_interfaces(globalNetworkInfo *gni, gni_vpc *vpc, gni_instance ***out_interfaces, int *max_out_interfaces) {
    gni_instance **result = NULL;
    int max_result = 0;
    int i = 0;

    if (!gni || !vpc || !out_interfaces || !max_out_interfaces) {
        LOGWARN("Invalid argument: failed to get vpc interfaces.\n");
        return (1);
    }
    LOGTRACE("Searching VPC interfaces.\n");
    for (i = 0; i < gni->max_ifs; i++) {
        if (strcmp(gni->ifs[i]->vpc, vpc->name)) {
            LOGTRACE("%s in %s: N\n", gni->ifs[i]->name, vpc->name);
        } else {
            LOGTRACE("%s in %s: Y\n", gni->ifs[i]->name, vpc->name);
            result = EUCA_REALLOC_C(result, max_result + 1, sizeof (gni_instance *));
            result[max_result] = gni->ifs[i];
            max_result++;
        }
    }
    *out_interfaces = result;
    *max_out_interfaces = max_result;
    return (0);
}

/**
 * Searches and returns an array of pointers to gni_instance data structures (holding
 * interface information) that are associated with the given VPC subnet.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param vpcsubnet [in] a pointer to the vpcsubnet gni data structure of interest
 * @param vpcinterfaces [in] a list of pointers to interfaces to search
 * @param max_vpcinterfaces [in] number of interfaces found
 * @param out_interfaces [out] a list of pointers to interfaces of interest
 * @param max_out_interfaces [out] number of interfaces found
 *
 * @return 0 if the search is successfully executed - 0 interfaces found is still
 *         a successful search. 1 otherwise. 
 *
 * @pre
 *     gni data structure is assumed to be populated.
 *     out_interfaces should be free of memory allocations.
 *
 * @post
 *     memory allocated to hold the resulting list of interfaces should be released
 *     by the caller.
 */
int gni_vpcsubnet_get_interfaces(globalNetworkInfo *gni, gni_vpcsubnet *vpcsubnet,
        gni_instance **vpcinterfaces, int max_vpcinterfaces, gni_instance ***out_interfaces, int *max_out_interfaces) {
    gni_instance **result = NULL;
    int max_result = 0;
    int i = 0;

    if (!gni || !vpcsubnet || !vpcinterfaces || !out_interfaces || !max_out_interfaces) {
        if (max_vpcinterfaces == 0) {
            return (0);
        }
        LOGWARN("Invalid argument: failed to get subnet interfaces.\n");
        return (1);
    }

    LOGTRACE("Searching VPC subnet interfaces.\n");
    for (i = 0; i < max_vpcinterfaces; i++) {
        if (strcmp(vpcinterfaces[i]->subnet, vpcsubnet->name)) {
            LOGTRACE("%s in %s: N\n", vpcinterfaces[i]->name, vpcsubnet->name);
        } else {
            LOGTRACE("%s in %s: Y\n", vpcinterfaces[i]->name, vpcsubnet->name);
            result = EUCA_REALLOC_C(result, max_result + 1, sizeof (gni_instance *));
            result[max_result] = vpcinterfaces[i];
            max_result++;
        }
    }
    *out_interfaces = result;
    *max_out_interfaces = max_result;
    return (0);
}

/**
 * Looks up for the cluster for which we are assigned within a configured cluster list. We can
 * be the cluster itself or one of its node.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param outclusterptr [out] a pointer to the associated cluster structure pointer
 *
 * @return 0 if a matching cluster structure is found or 1 if not found or a failure occured
 *
 * @see gni_is_self()
 *
 * @post
 *     On success the value pointed by outclusterptr is valid. On failure, this value
 *     is non-deterministic.
 */
int gni_find_self_cluster(globalNetworkInfo * gni, gni_cluster ** outclusterptr) {
    int i, j;
    char *strptra = NULL;

    if (!gni || !outclusterptr) {
        LOGWARN("invalid argument: cannot find cluster from NULL\n");
        return (1);
    }

    *outclusterptr = NULL;

    for (i = 0; i < gni->max_clusters; i++) {
        // check to see if local host is the enabled cluster controller
        strptra = hex2dot(gni->clusters[i].enabledCCIp);
        if (strptra) {
            if (!gni_is_self_getifaddrs(strptra)) {
                EUCA_FREE(strptra);
                *outclusterptr = &(gni->clusters[i]);
                return (0);
            }
            EUCA_FREE(strptra);
        }
        // otherwise, check to see if local host is a node in the cluster
        for (j = 0; j < gni->clusters[i].max_nodes; j++) {
            //      if (!strcmp(gni->clusters[i].nodes[j].name, outnodeptr->name)) {
            if (!gni_is_self_getifaddrs(gni->clusters[i].nodes[j].name)) {
                *outclusterptr = &(gni->clusters[i]);
                return (0);
            }
        }
    }
    return (1);
}

/**
 * Looks up for the security group ID in the argument.
 * @param gni [in] a pointer to the global network information structure
 * @param psGroupId [in] a pointer to a constant string containing the Security-Group ID of interest
 * @param pSecGroup [out] a pointer to the associated security group structure pointer
 * @return 0 if a matching security group structure is found or 1 if not found or a failure occurred
 */
int gni_find_secgroup(globalNetworkInfo *gni, const char *psGroupId, gni_secgroup **pSecGroup) {
    int i = 0;

    if (!gni || !psGroupId || !pSecGroup) {
        LOGWARN("invalid argument: cannot find secgroup from NULL\n");
        return (1);
    }
    // Initialize to NULL
    (*pSecGroup) = NULL;

    // Go through our security group list and look for that group
    for (i = 0; i < gni->max_secgroups; i++) {
        if (!strcmp(psGroupId, gni->secgroups[i].name)) {
            (*pSecGroup) = &(gni->secgroups[i]);
            return (0);
        }
    }
    return (1);
}

/**
 * Searches for the given instance name and returns the associated gni_instance structure.
 * @param gni [in] pointer to the global network information structure.
 * @param psInstanceId [in] the ID string of the instance of interest.
 * @param pInstance [out] pointer to the gni_instance structure of interest.
 * @return 0 on success. Positive integer otherwise.
 */
int gni_find_instance(globalNetworkInfo *gni, const char *psInstanceId, gni_instance ** pInstance) {
    if (!gni || !psInstanceId || !pInstance) {
        LOGWARN("invalid argument: cannot find instance from NULL\n");
        return (1);
    }
    // Initialize to NULL
    (*pInstance) = NULL;

    LOGTRACE("attempting search for instance id %s in gni\n", psInstanceId);

    // binary search - instances should be already sorted in GNI (uncomment below if not sorted)
/*
    if (gni->sorted_instances == FALSE) {
        qsort(gni->instances, gni->max_instances,
                sizeof (gni_instance *), compare_gni_instance_name);
        gni->sorted_instances = TRUE;
    }
*/
    gni_instance inst;
    snprintf(inst.name, INTERFACE_ID_LEN, "%s", psInstanceId);
    gni_instance *pinst = &inst;
    gni_instance **res = (gni_instance **) bsearch(&pinst,
            gni->instances, gni->max_instances,
            sizeof (gni_instance *), compare_gni_instance_name);
    if (res) {
        *pInstance = *res;
        return (0);
    }

    return (1);
}

/**
 * Searches through the list of network interfaces in the gni  and returns all
 * non-primary interfaces for a given instance
 *
 * @param gni [in] a pointer to the global network information structure
 * @param psInstanceId [in] a pointer to instance ID identifier (i-XXXXXXX)
 * @param pAInstances [out] an array of network interface pointers
 * @param size [out] a pointer to the size of the array
 *
 * @return 0 if lookup is successful or 1 if a failure occurred
 */
int gni_find_secondary_interfaces(globalNetworkInfo *gni, const char *psInstanceId, gni_instance *pAInstances[], int *size) {
    if (!size || !gni || !psInstanceId || !sizeof (pAInstances)) {
        LOGERROR("invalid argument: cannot find secondary interfaces for NULL\n");
        return EUCA_ERROR;
    }

    *size = 0;

    LOGTRACE("attempting search for interfaces for instance id %s in gni\n", psInstanceId);

    gni_instance *inst = NULL;
    int rc = 0;
    rc = gni_find_instance(gni, psInstanceId, &inst);
    if (!rc) {
        for (int i = 0; i < inst->max_interfaces; i++) {
            if (inst->interfaces[i]->deviceidx != 0) {
                pAInstances[*size] = inst->interfaces[i];
                (*size)++;
            }
        }
    }

    return EUCA_OK;
}

/**
 * Looks up through a list of configured node for the one that is associated with
 * this currently running instance.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param outnodeptr [out] a pointer to the associated node structure pointer
 *
 * @return 0 if a matching node structure is found or 1 if not found or a failure occured
 *
 * @see gni_is_self()
 *
 * @post
 *     On success the value pointed by outnodeptr is valid. On failure, this value
 *     is non-deterministic.
 */
int gni_find_self_node(globalNetworkInfo *gni, gni_node **outnodeptr) {
    int i, j;

    if (!gni || !outnodeptr) {
        LOGERROR("invalid input\n");
        return (1);
    }

    *outnodeptr = NULL;

    for (i = 0; i < gni->max_clusters; i++) {
        for (j = 0; j < gni->clusters[i].max_nodes; j++) {
            if (!gni_is_self_getifaddrs(gni->clusters[i].nodes[j].name)) {
                *outnodeptr = &(gni->clusters[i].nodes[j]);
                return (0);
            }
        }
    }

    return (1);
}

/**
 * Validates if the given test_ip is a local IP address on this system
 *
 * @param test_ip [in] a string containing the IP to validate
 *
 * @return 0 if the test_ip is a local IP or 1 on failure or if not found locally
 */
int gni_is_self(const char *test_ip) {
    DIR *DH = NULL;
    struct dirent dent, *result = NULL;
    int max, rc, i;
    u32 *outips = NULL, *outnms = NULL;
    char *strptra = NULL;

    if (!test_ip) {
        LOGERROR("invalid input\n");
        return (1);
    }

    DH = opendir("/sys/class/net/");
    if (!DH) {
        LOGERROR("could not open directory /sys/class/net/ for read: check permissions\n");
        return (1);
    }

    rc = readdir_r(DH, &dent, &result);
    while (!rc && result) {
        if (strcmp(dent.d_name, ".") && strcmp(dent.d_name, "..")) {
            rc = getdevinfo(dent.d_name, &outips, &outnms, &max);
            for (i = 0; i < max; i++) {
                strptra = hex2dot(outips[i]);
                if (strptra) {
                    if (!strcmp(strptra, test_ip)) {
                        EUCA_FREE(strptra);
                        EUCA_FREE(outips);
                        EUCA_FREE(outnms);
                        closedir(DH);
                        return (0);
                    }
                    EUCA_FREE(strptra);
                }
            }
            EUCA_FREE(outips);
            EUCA_FREE(outnms);
        }
        rc = readdir_r(DH, &dent, &result);
    }
    closedir(DH);

    return (1);
}

/**
 * Validates if the given test_ip is a local IP address on this system. This function
 * is based on getifaddrs() call.
 * @param test_ip [in] a string containing the IP to validate
 *
 * @return 0 if the test_ip is a local IP or 1 on failure or if not found locally
 */
int gni_is_self_getifaddrs(const char *test_ip) {
    struct ifaddrs *ifas = NULL;
    struct ifaddrs *elem = NULL;
    int found = 0;
    int rc = 0;

    if (!test_ip) {
        LOGERROR("invalid input: cannot check NULL IP\n");
        return (1);
    }

    rc = getifaddrs(&ifas);
    if (rc) {
        LOGERROR("unable to retrieve system IPv4 addresses.\n");
        freeifaddrs(ifas);
        return (1);
    }

    elem = ifas;
    while (elem && !found) {
        if (elem->ifa_addr && elem->ifa_addr->sa_family == AF_INET) {
            struct sockaddr_in *saddr = (struct sockaddr_in *) elem->ifa_addr;
            if (!strcmp(test_ip, inet_ntoa(saddr->sin_addr))) {
                found = 1;
            }
        }
        elem = elem->ifa_next;
    }
    freeifaddrs(ifas);
    if (found) {
        return (0);
    }
    return (1);
}

/**
 * Return a copy of GNI clusters
 * @param gni [in] a pointer to the global network information structure
 * @param cluster_names [in] optional list of cluster names to look for
 * @param max_cluster_names [in] number of cluster names in the optional list
 * @param out_cluster_names [out] list of found cluster names
 * @param out_max_cluster_names [out] number of entries in the found cluster name array
 * @param out_clusters [out] array of gni_cluster data structures
 * @param out_max_clusters [out] number oif entries in the array of gni_cluster data structures
 * @return 0 on success. 1 on failure.
 */
int gni_cloud_get_clusters(globalNetworkInfo *gni, char **cluster_names, int max_cluster_names,
        char ***out_cluster_names, int *out_max_cluster_names, gni_cluster **out_clusters,
        int *out_max_clusters) {
    int ret = 0, getall = 0, i = 0, j = 0, retcount = 0, do_outnames = 0, do_outstructs = 0;
    gni_cluster *ret_clusters = NULL;
    char **ret_cluster_names = NULL;

    if (!cluster_names || max_cluster_names <= 0) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (out_cluster_names && out_max_cluster_names) {
        do_outnames = 1;
    }
    if (out_clusters && out_max_clusters) {
        do_outstructs = 1;
    }

    if (!do_outnames && !do_outstructs) {
        LOGEXTREME("nothing to do, both output variables are NULL\n");
        return (0);
    }

    if (do_outnames) {
        *out_cluster_names = NULL;
        *out_max_cluster_names = 0;
    }
    if (do_outstructs) {
        *out_clusters = NULL;
        *out_max_clusters = 0;
    }

    if (!strcmp(cluster_names[0], "*")) {
        getall = 1;
        if (do_outnames)
            *out_cluster_names = EUCA_ZALLOC_C(gni->max_clusters, sizeof (char *));
        if (do_outstructs)
            *out_clusters = EUCA_ZALLOC_C(gni->max_clusters, sizeof (gni_cluster));
    }

    if (do_outnames)
        ret_cluster_names = *out_cluster_names;
    if (do_outstructs)
        ret_clusters = *out_clusters;

    retcount = 0;
    for (i = 0; i < gni->max_clusters; i++) {
        if (getall) {
            if (do_outnames)
                ret_cluster_names[i] = strdup(gni->clusters[i].name);
            if (do_outstructs)
                memcpy(&(ret_clusters[i]), &(gni->clusters[i]), sizeof (gni_cluster));
            retcount++;
        } else {
            for (j = 0; j < max_cluster_names; j++) {
                if (!strcmp(cluster_names[j], gni->clusters[i].name)) {
                    if (do_outnames) {
                        *out_cluster_names = EUCA_REALLOC_C(*out_cluster_names, (retcount + 1), sizeof (char *));
                        ret_cluster_names = *out_cluster_names;
                        ret_cluster_names[retcount] = strdup(gni->clusters[i].name);
                    }
                    if (do_outstructs) {
                        *out_clusters = EUCA_REALLOC_C(*out_clusters, (retcount + 1), sizeof (gni_cluster));
                        ret_clusters = *out_clusters;
                        memcpy(&(ret_clusters[retcount]), &(gni->clusters[i]), sizeof (gni_cluster));
                    }
                    retcount++;
                }
            }
        }
    }
    if (do_outnames)
        *out_max_cluster_names = retcount;
    if (do_outstructs)
        *out_max_clusters = retcount;

    return (ret);
}

/**
 * Retrives the list of security groups configured under a cloud
 *
 * @param pGni [in] a pointer to our global network view structure
 * @param psSecGroupNames [in] a string pointer to the name of groups we're looking for
 * @param nbSecGroupNames [in] the number of groups in the psSecGroupNames list
 * @param psOutSecGroupNames [out] a string pointer that will contain the list of group names we found (if non NULL)
 * @param pOutNbSecGroupNames [out] a pointer to the number of groups that matched in the psOutSecGroupNames list
 * @param pOutSecGroups [out] a pointer to the list of security group structures that match what we're looking for
 * @param pOutNbSecGroups [out] a pointer to the number of structures in the psOutSecGroups list
 *
 * @return 0 on success or 1 if any failure occured
 */
int gni_cloud_get_secgroups(globalNetworkInfo *pGni, char **psSecGroupNames,
        int nbSecGroupNames, char ***psOutSecGroupNames, int *pOutNbSecGroupNames,
        gni_secgroup ** pOutSecGroups, int *pOutNbSecGroups) {
    int ret = 0;
    int i = 0;
    int x = 0;
    int retCount = 0;
    char **psRetSecGroupNames = NULL;
    boolean getAll = FALSE;
    boolean doOutNames = FALSE;
    boolean doOutStructs = FALSE;
    gni_secgroup *pRetSecGroup = NULL;

    // Make sure our GNI pointer isn't NULL
    if (!pGni) {
        LOGERROR("invalid input\n");
        return (1);
    }
    // Are we building the name list?
    if (psOutSecGroupNames && pOutNbSecGroupNames) {
        doOutNames = TRUE;
        *psOutSecGroupNames = NULL;
        *pOutNbSecGroupNames = 0;
    }
    // Are we building the structure list?
    if (pOutSecGroups && pOutNbSecGroups) {
        doOutStructs = TRUE;
        *pOutSecGroups = NULL;
        *pOutNbSecGroups = 0;
    }
    // Are we doing anything?
    if (!doOutNames && !doOutStructs) {
        LOGEXTREME("nothing to do, both output variables are NULL\n");
        return (0);
    }
    // Do we have any groups?
    if (pGni->max_secgroups == 0)
        return (0);

    // If we do it all, allocate the memory now
    if (psSecGroupNames == NULL || !strcmp(psSecGroupNames[0], "*")) {
        getAll = TRUE;
        if (doOutNames)
            *psOutSecGroupNames = EUCA_ZALLOC_C(pGni->max_secgroups, sizeof (char *));
        if (doOutStructs)
            *pOutSecGroups = EUCA_ZALLOC_C(pGni->max_secgroups, sizeof (gni_secgroup));
    }
    // Setup our returning name list pointer
    if (doOutNames)
        psRetSecGroupNames = *psOutSecGroupNames;

    // Setup our returning structure list pointer
    if (doOutStructs)
        pRetSecGroup = *pOutSecGroups;

    // Go through the group list
    for (i = 0, retCount = 0; i < pGni->max_secgroups; i++) {
        if (getAll) {
            if (doOutNames)
                psRetSecGroupNames[retCount] = strdup(pGni->secgroups[i].name);

            if (doOutStructs)
                memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof (gni_secgroup));
            retCount++;
        } else {
            if (!strcmp(psSecGroupNames[x], pGni->secgroups[i].name)) {
                if (doOutNames) {
                    *psOutSecGroupNames = EUCA_REALLOC_C(*psOutSecGroupNames, (retCount + 1), sizeof (char *));
                    psRetSecGroupNames = *psOutSecGroupNames;
                    psRetSecGroupNames[retCount] = strdup(pGni->secgroups[i].name);
                }

                if (doOutStructs) {
                    *pOutSecGroups = EUCA_REALLOC_C(*pOutSecGroups, (retCount + 1), sizeof (gni_instance));
                    pRetSecGroup = *pOutSecGroups;
                    memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof (gni_secgroup));
                }
                retCount++;
            }
        }
    }

    if (doOutNames)
        *pOutNbSecGroupNames = retCount;

    if (doOutStructs)
        *pOutNbSecGroups = retCount;

    return (ret);
}

/**
 * Returns a copy of nodes in the given cluster.
 * @param gni [in] a pointer to our global network view structure
 * @param cluster [in] gni_cluster data structure of interest
 * @param node_names [in] optional list of node names to look for
 * @param max_node_names [in] number of entries in the optional list of node names
 * @param out_node_names [out] list of found node names
 * @param out_max_node_names [out] number of entries in the list of found node names
 * @param out_nodes [out] array of found gni_node data structures
 * @param out_max_nodes [out] number of entries in the array of found gni_node data structures
 * @return 0 on success. 1 on failure.
 */
int gni_cluster_get_nodes(globalNetworkInfo *gni, gni_cluster *cluster,
        char **node_names, int max_node_names, char ***out_node_names,
        int *out_max_node_names, gni_node ** out_nodes, int *out_max_nodes) {
    int ret = 0, rc = 0, getall = 0, i = 0, j = 0, retcount = 0, do_outnames = 0, do_outstructs = 0, out_max_clusters = 0;
    gni_node *ret_nodes = NULL;
    gni_cluster *out_clusters = NULL;
    char **ret_node_names = NULL, **cluster_names = NULL;

    if (!gni) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (out_node_names && out_max_node_names) {
        do_outnames = 1;
    }
    if (out_nodes && out_max_nodes) {
        do_outstructs = 1;
    }

    if (!do_outnames && !do_outstructs) {
        LOGEXTREME("nothing to do, both output variables are NULL\n");
        return (0);
    }

    if (do_outnames) {
        *out_node_names = NULL;
        *out_max_node_names = 0;
    }
    if (do_outstructs) {
        *out_nodes = NULL;
        *out_max_nodes = 0;
    }

    cluster_names = EUCA_ZALLOC_C(1, sizeof (char *));
    cluster_names[0] = cluster->name;
    rc = gni_cloud_get_clusters(gni, cluster_names, 1, NULL, NULL, &out_clusters, &out_max_clusters);
    if (rc || out_max_clusters <= 0) {
        LOGWARN("nothing to do, no matching cluster named '%s' found\n", cluster->name);
        EUCA_FREE(cluster_names);
        EUCA_FREE(out_clusters);
        return (0);
    }

    if ((node_names == NULL) || !strcmp(node_names[0], "*")) {
        getall = 1;
        if (do_outnames)
            *out_node_names = EUCA_ZALLOC_C(cluster->max_nodes, sizeof (char *));
        if (do_outstructs)
            *out_nodes = EUCA_ZALLOC_C(cluster->max_nodes, sizeof (gni_node));
    }

    if (do_outnames)
        ret_node_names = *out_node_names;

    if (do_outstructs)
        ret_nodes = *out_nodes;

    retcount = 0;
    for (i = 0; i < cluster->max_nodes; i++) {
        if (getall) {
            if (do_outnames)
                ret_node_names[i] = strdup(out_clusters[0].nodes[i].name);

            if (do_outstructs)
                memcpy(&(ret_nodes[i]), &(out_clusters[0].nodes[i]), sizeof (gni_node));

            retcount++;
        } else {
            for (j = 0; j < max_node_names; j++) {
                if (!strcmp(node_names[j], out_clusters[0].nodes[i].name)) {
                    if (do_outnames) {
                        *out_node_names = EUCA_REALLOC_C(*out_node_names, (retcount + 1), sizeof (char *));
                        ret_node_names = *out_node_names;
                        ret_node_names[retcount] = strdup(out_clusters[0].nodes[i].name);
                    }
                    if (do_outstructs) {
                        *out_nodes = EUCA_REALLOC_C(*out_nodes, (retcount + 1), sizeof (gni_node));
                        ret_nodes = *out_nodes;
                        memcpy(&(ret_nodes[retcount]), &(out_clusters[0].nodes[i]), sizeof (gni_node));
                    }
                    retcount++;
                }
            }
        }
    }

    if (do_outnames)
        *out_max_node_names = retcount;

    if (do_outstructs)
        *out_max_nodes = retcount;

    EUCA_FREE(cluster_names);
    EUCA_FREE(out_clusters);
    return (ret);
}

/**
 * Returns a copy of instances in the given cluster.
 * @param pGni [in] a pointer to our global network view structure
 * @param pCluster [in] gni_cluster data structure of interest
 * @param psInstanceNames [in] optional list of instance names to look for
 * @param nbInstanceNames [in] entries in the optional list of instance names
 * @param psOutInstanceNames [out] list of found instance names
 * @param pOutNbInstanceNames [out] entries in the list of found instance names
 * @param pOutInstances [out] array of found gni_instance data structures
 * @param pOutNbInstances [out] number of entries in the found gni_instance data structures
 * @return  0 on success. 1 on failure.
 */
int gni_cluster_get_instances(globalNetworkInfo *pGni, gni_cluster *pCluster, char **psInstanceNames, int nbInstanceNames,
        char ***psOutInstanceNames, int *pOutNbInstanceNames, gni_instance ** pOutInstances, int *pOutNbInstances) {
    int ret = 0;
    int i = 0;
    int k = 0;
    int x = 0;
    int y = 0;
    int retCount = 0;
    int nbInstances = 0;
    char **psRetInstanceNames = NULL;
    boolean getAll = FALSE;
    boolean doOutNames = FALSE;
    boolean doOutStructs = FALSE;
    gni_instance *pRetInstances = NULL;

    if (!pGni || !pCluster) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (psOutInstanceNames && pOutNbInstanceNames) {
        doOutNames = TRUE;
    }
    if (pOutInstances && pOutNbInstances) {
        doOutStructs = TRUE;
    }

    if (!doOutNames && !doOutStructs) {
        LOGEXTREME("nothing to do, both output variables are NULL\n");
        return (0);
    }

    if (doOutNames) {
        *psOutInstanceNames = NULL;
        *pOutNbInstanceNames = 0;
    }
    if (doOutStructs) {
        *pOutInstances = NULL;
        *pOutNbInstances = 0;
    }
    // Do we have any nodes?
    if (pCluster->max_nodes == 0)
        return (0);

    for (i = 0, nbInstances = 0; i < pCluster->max_nodes; i++) {
        nbInstances += pCluster->nodes[i].max_instance_names;
    }

    // Do we have any instances?
    if (nbInstances == 0)
        return (0);

    if (psInstanceNames == NULL || !strcmp(psInstanceNames[0], "*")) {
        getAll = TRUE;
        if (doOutNames)
            *psOutInstanceNames = EUCA_ZALLOC_C(nbInstances, sizeof (char *));
        if (doOutStructs)
            *pOutInstances = EUCA_ZALLOC_C(nbInstances, sizeof (gni_instance));
    }

    if (doOutNames)
        psRetInstanceNames = *psOutInstanceNames;

    if (doOutStructs)
        pRetInstances = *pOutInstances;

    for (i = 0, retCount = 0; i < pCluster->max_nodes; i++) {
        for (k = 0; k < pCluster->nodes[i].max_instance_names; k++) {
            if (getAll) {
                if (doOutNames)
                    psRetInstanceNames[retCount] = strdup(pCluster->nodes[i].instance_names[k].name);

                if (doOutStructs) {
                    for (x = 0; x < pGni->max_instances; x++) {
                        if (!strcmp(pGni->instances[x]->name, pCluster->nodes[i].instance_names[k].name)) {
                            memcpy(&(pRetInstances[retCount]), pGni->instances[x], sizeof (gni_instance));
                            break;
                        }
                    }
                }
                retCount++;
            } else {
                for (x = 0; x < nbInstanceNames; x++) {
                    if (!strcmp(psInstanceNames[x], pCluster->nodes[i].instance_names[k].name)) {
                        if (doOutNames) {
                            *psOutInstanceNames = EUCA_REALLOC_C(*psOutInstanceNames, (retCount + 1), sizeof (char *));
                            psRetInstanceNames = *psOutInstanceNames;
                            psRetInstanceNames[retCount] = strdup(pCluster->nodes[i].instance_names[k].name);
                        }

                        if (doOutStructs) {
                            for (y = 0; y < pGni->max_instances; y++) {
                                if (!strcmp(pGni->instances[y]->name, pCluster->nodes[i].instance_names[k].name)) {
                                    *pOutInstances = EUCA_REALLOC_C(*pOutInstances, (retCount + 1), sizeof (gni_instance));
                                    pRetInstances = *pOutInstances;
                                    memcpy(&(pRetInstances[retCount]), pGni->instances[y], sizeof (gni_instance));
                                    break;
                                }
                            }
                        }
                        retCount++;
                    }
                }
            }
        }
    }

    if (doOutNames)
        *pOutNbInstanceNames = retCount;

    if (doOutStructs)
        *pOutNbInstances = retCount;

    return (ret);
}

/**
 * Retrieves the list of security groups configured and active on a given cluster
 *
 * @param pGni [in] a pointer to our global network view structure
 * @param pCluster [in] a pointer to the cluster we're building the security group list for
 * @param psSecGroupNames [in] a string pointer to the name of groups we're looking for
 * @param nbSecGroupNames [in] the number of groups in the psSecGroupNames list
 * @param psOutSecGroupNames [out] a string pointer that will contain the list of group names we found (if non NULL)
 * @param pOutNbSecGroupNames [out] a pointer to the number of groups that matched in the psOutSecGroupNames list
 * @param pOutSecGroups [out] a pointer to the list of security group structures that match what we're looking for
 * @param pOutNbSecGroups [out] a pointer to the number of structures in the psOutSecGroups list
 *
 * @return 0 on success or 1 if any failure occured
 */
int gni_cluster_get_secgroup(globalNetworkInfo *pGni, gni_cluster *pCluster,
        char **psSecGroupNames, int nbSecGroupNames, char ***psOutSecGroupNames,
        int *pOutNbSecGroupNames, gni_secgroup ** pOutSecGroups, int *pOutNbSecGroups) {
    int ret = 0;
    int i = 0;
    int k = 0;
    int x = 0;
    int retCount = 0;
    int nbInstances = 0;
    char **psRetSecGroupNames = NULL;
    boolean found = FALSE;
    boolean getAll = FALSE;
    boolean doOutNames = FALSE;
    boolean doOutStructs = FALSE;
    gni_instance *pInstances = NULL;
    gni_secgroup *pRetSecGroup = NULL;

    // Make sure our GNI and Cluster pointers are valid
    if (!pGni || !pCluster) {
        LOGERROR("invalid input\n");
        return (1);
    }
    // We will need to get the instances that are running under this cluster
    if (gni_cluster_get_instances(pGni, pCluster, NULL, 0, NULL, NULL, &pInstances, &nbInstances)) {
        LOGERROR("Failed to retrieve instances for cluster '%s'\n", pCluster->name);
        return (1);
    }
    // Are we building the name list?
    if (psOutSecGroupNames && pOutNbSecGroupNames) {
        doOutNames = TRUE;
        *psOutSecGroupNames = NULL;
        *pOutNbSecGroupNames = 0;
    }
    // Are we building the structure list?
    if (pOutSecGroups && pOutNbSecGroups) {
        doOutStructs = TRUE;
        *pOutSecGroups = NULL;
        *pOutNbSecGroups = 0;
    }
    // Are we doing anything?
    if (!doOutNames && !doOutStructs) {
        LOGEXTREME("nothing to do, both output variables are NULL\n");
        EUCA_FREE(pInstances);
        return (0);
    }
    // Do we have any instances?
    if (nbInstances == 0) {
        EUCA_FREE(pInstances);
        return (0);
    }

    // Do we have any groups?
    if (pGni->max_secgroups == 0) {
        EUCA_FREE(pInstances);
        return (0);
    }
    // Allocate memory for all the groups if there is no search criteria
    if ((psSecGroupNames == NULL) || !strcmp(psSecGroupNames[0], "*")) {
        getAll = TRUE;
        if (doOutNames)
            *psOutSecGroupNames = EUCA_ZALLOC_C(pGni->max_secgroups, sizeof (char *));

        if (doOutStructs)
            *pOutSecGroups = EUCA_ZALLOC_C(pGni->max_secgroups, sizeof (gni_secgroup));
    }
    // Setup our returning name pointer
    if (doOutNames)
        psRetSecGroupNames = *psOutSecGroupNames;

    // Setup our returning structure pointer
    if (doOutStructs)
        pRetSecGroup = *pOutSecGroups;

    // Scan all our groups
    for (i = 0, retCount = 0; i < pGni->max_secgroups; i++) {
        if (getAll) {
            // Check if this we have any instance using this group
            for (k = 0, found = FALSE; ((k < nbInstances) && !found); k++) {
                for (x = 0; ((x < pInstances[k].max_secgroup_names) && !found); x++) {
                    if (!strcmp(pGni->secgroups[i].name, pInstances[k].secgroup_names[x].name)) {
                        found = TRUE;
                    }
                }
            }

            // If we have any instance using this group, then copy it
            if (found) {
                if (doOutNames)
                    psRetSecGroupNames[retCount] = strdup(pGni->secgroups[i].name);

                if (doOutStructs)
                    memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof (gni_secgroup));
                retCount++;
            }
        } else {
            if (!strcmp(psSecGroupNames[x], pGni->secgroups[i].name)) {
                // Check if this we have any instance using this group
                for (k = 0, found = FALSE; ((k < nbInstances) && !found); k++) {
                    for (x = 0; ((x < pInstances[k].max_secgroup_names) && !found); x++) {
                        if (!strcmp(pGni->secgroups[i].name, pInstances[k].secgroup_names[x].name)) {
                            found = TRUE;
                        }
                    }
                }

                // If we have any instance using this group, then copy it
                if (found) {
                    if (doOutNames) {
                        *psOutSecGroupNames = EUCA_REALLOC_C(*psOutSecGroupNames, (retCount + 1), sizeof (char *));
                        psRetSecGroupNames = *psOutSecGroupNames;
                        psRetSecGroupNames[retCount] = strdup(pGni->secgroups[i].name);
                    }

                    if (doOutStructs) {
                        *pOutSecGroups = EUCA_REALLOC_C(*pOutSecGroups, (retCount + 1), sizeof (gni_instance));
                        pRetSecGroup = *pOutSecGroups;
                        memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof (gni_secgroup));
                    }
                    retCount++;
                }
            }
        }
    }

    if (doOutNames)
        *pOutNbSecGroupNames = retCount;

    if (doOutStructs)
        *pOutNbSecGroups = retCount;

    EUCA_FREE(pInstances);
    return (ret);
}

/**
 * Gets the instances hosted by the node in the argument.
 * @param gni [in] global network information structure
 * @param node [in] node of interest
 * @param instance_names [in] optional list of instance names
 * @param max_instance_names [in] number of instance names
 * @param out_instance_names [out] list of instance names found
 * @param out_max_instance_names [out] number of instance names found
 * @param out_instances [out] array of structures representing instances that were found
 * @param out_max_instances [out] number of entries in out_instances
 * @return 0 on success, 1 otherwise.
 */
int gni_node_get_instances(globalNetworkInfo *gni, gni_node *node, char **instance_names,
        int max_instance_names, char ***out_instance_names, int *out_max_instance_names,
        gni_instance **out_instances, int *out_max_instances) {
    int ret = 0, getall = 0, i = 0, j = 0, k = 0, retcount = 0, do_outnames = 0, do_outstructs = 0;
    gni_instance *ret_instances = NULL;
    char **ret_instance_names = NULL;

    if (!gni) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (out_instance_names && out_max_instance_names) {
        do_outnames = 1;
    }
    if (out_instances && out_max_instances) {
        do_outstructs = 1;
    }

    if (!do_outnames && !do_outstructs) {
        LOGEXTREME("nothing to do, both output variables are NULL\n");
        return (0);
    }

    if (do_outnames) {
        *out_instance_names = NULL;
        *out_max_instance_names = 0;
    }
    if (do_outstructs) {
        *out_instances = NULL;
        *out_max_instances = 0;
    }

    if (instance_names == NULL || !strcmp(instance_names[0], "*")) {
        getall = 1;
        if (do_outnames)
            *out_instance_names = EUCA_ZALLOC_C(node->max_instance_names, sizeof (char *));
        if (do_outstructs)
            *out_instances = EUCA_ZALLOC_C(node->max_instance_names, sizeof (gni_instance));
    }

    if (do_outnames)
        ret_instance_names = *out_instance_names;
    if (do_outstructs)
        ret_instances = *out_instances;

    retcount = 0;
    if (getall) {
        for (i = 0; i < gni->max_instances; i++) {
            if (retcount == node->max_instance_names) {
                break;
            }
            if (!strcmp(gni->instances[i]->node, node->name)) {
                if (do_outstructs) {
                    memcpy(&(ret_instances[retcount]), gni->instances[i], sizeof (gni_instance));
                }
                if (do_outnames) {
                    ret_instance_names[retcount] = strdup(gni->instances[i]->name);
                }
                retcount++;
            }
        }
    } else {
        for (i = 0; i < node->max_instance_names; i++) {
            for (j = 0; j < max_instance_names; j++) {
                if (!strcmp(instance_names[j], node->instance_names[i].name)) {
                    if (do_outnames) {
                        *out_instance_names = EUCA_REALLOC_C(*out_instance_names, (retcount + 1), sizeof (char *));
                        ret_instance_names = *out_instance_names;
                        ret_instance_names[retcount] = strdup(node->instance_names[i].name);
                    }
                    if (do_outstructs) {
                        for (k = 0; k < gni->max_instances; k++) {
                            if (!strcmp(gni->instances[k]->name, node->instance_names[i].name)) {
                                *out_instances = EUCA_REALLOC_C(*out_instances, (retcount + 1), sizeof (gni_instance));
                                ret_instances = *out_instances;
                                memcpy(&(ret_instances[retcount]), gni->instances[k], sizeof (gni_instance));
                                break;
                            }
                        }
                    }
                    retcount++;
                }
            }
        }
    }
    if (do_outnames)
        *out_max_instance_names = retcount;
    if (do_outstructs)
        *out_max_instances = retcount;

    return (ret);
}

/**
 * Retrieves the list of security groups configured and active on a given cluster
 *
 * @param pGni [in] a pointer to our global network view structure
 * @param pNode [in] a pointer to the node we're building the security group list for
 * @param psSecGroupNames [in] a string pointer to the name of groups we're looking for
 * @param nbSecGroupNames [in] the number of groups in the psSecGroupNames list
 * @param psOutSecGroupNames [out] a string pointer that will contain the list of group names we found (if non NULL)
 * @param pOutNbSecGroupNames [out] a pointer to the number of groups that matched in the psOutSecGroupNames list
 * @param pOutSecGroups [out] a pointer to the list of security group structures that match what we're looking for
 * @param pOutNbSecGroups [out] a pointer to the number of structures in the psOutSecGroups list
 *
 * @return 0 on success or 1 if any failure occured
 */
int gni_node_get_secgroup(globalNetworkInfo *pGni, gni_node *pNode, char **psSecGroupNames, int nbSecGroupNames, char ***psOutSecGroupNames, int *pOutNbSecGroupNames,
        gni_secgroup ** pOutSecGroups, int *pOutNbSecGroups) {
    int ret = 0;
    int i = 0;
    int k = 0;
    int x = 0;
    int retCount = 0;
    int nbInstances = 0;
    char **psRetSecGroupNames = NULL;
    boolean found = FALSE;
    boolean getAll = FALSE;
    boolean doOutNames = FALSE;
    boolean doOutStructs = FALSE;
    gni_instance *pInstances = NULL;
    gni_secgroup *pRetSecGroup = NULL;

    // Make sure our GNI and Cluster pointers are valid
    if (!pGni || !pNode) {
        LOGERROR("invalid input\n");
        return (1);
    }
    // We will need to get the instances that are running under this cluster
    if (gni_node_get_instances(pGni, pNode, NULL, 0, NULL, NULL, &pInstances, &nbInstances)) {
        LOGERROR("Failed to retrieve instances for node '%s'\n", pNode->name);
        return (1);
    }
    // Are we building the name list?
    if (psOutSecGroupNames && pOutNbSecGroupNames) {
        doOutNames = TRUE;
        *psOutSecGroupNames = NULL;
        *pOutNbSecGroupNames = 0;
    }
    // Are we building the structure list?
    if (pOutSecGroups && pOutNbSecGroups) {
        doOutStructs = TRUE;
        *pOutSecGroups = NULL;
        *pOutNbSecGroups = 0;
    }
    // Are we doing anything?
    if (!doOutNames && !doOutStructs) {
        LOGEXTREME("nothing to do, both output variables are NULL\n");
        EUCA_FREE(pInstances);
        return (0);
    }
    // Do we have any instances?
    if (nbInstances == 0) {
        EUCA_FREE(pInstances);
        return (0);
    }
    // Do we have any groups?
    if (pGni->max_secgroups == 0) {
        EUCA_FREE(pInstances);
        return (0);
    }
    // Allocate memory for all the groups if there is no search criteria
    if ((psSecGroupNames == NULL) || !strcmp(psSecGroupNames[0], "*")) {
        getAll = TRUE;
        if (doOutNames)
            *psOutSecGroupNames = EUCA_ZALLOC_C(pGni->max_secgroups, sizeof (char *));

        if (doOutStructs)
            *pOutSecGroups = EUCA_ZALLOC_C(pGni->max_secgroups, sizeof (gni_secgroup));
    }
    // Setup our returning name pointer
    if (doOutNames)
        psRetSecGroupNames = *psOutSecGroupNames;

    // Setup our returning structure pointer
    if (doOutStructs)
        pRetSecGroup = *pOutSecGroups;

    // Scan all our groups
    for (i = 0, retCount = 0; i < pGni->max_secgroups; i++) {
        if (getAll) {
            // Check if this we have any instance using this group
            for (k = 0, found = FALSE; ((k < nbInstances) && !found); k++) {
                for (x = 0; ((x < pInstances[k].max_secgroup_names) && !found); x++) {
                    if (!strcmp(pGni->secgroups[i].name, pInstances[k].secgroup_names[x].name)) {
                        found = TRUE;
                    }
                }
            }

            // If we have any instance using this group, then copy it
            if (found) {
                if (doOutNames)
                    psRetSecGroupNames[retCount] = strdup(pGni->secgroups[i].name);

                if (doOutStructs)
                    memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof (gni_secgroup));
                retCount++;
            }
        } else {
            if (!strcmp(psSecGroupNames[x], pGni->secgroups[i].name)) {
                // Check if this we have any instance using this group
                for (k = 0, found = FALSE; ((k < nbInstances) && !found); k++) {
                    for (x = 0; ((x < pInstances[k].max_secgroup_names) && !found); x++) {
                        if (!strcmp(pGni->secgroups[i].name, pInstances[k].secgroup_names[x].name)) {
                            found = TRUE;
                        }
                    }
                }

                // If we have any instance using this group, then copy it
                if (found) {
                    if (doOutNames) {
                        *psOutSecGroupNames = EUCA_REALLOC_C(*psOutSecGroupNames, (retCount + 1), sizeof (char *));
                        psRetSecGroupNames = *psOutSecGroupNames;
                        psRetSecGroupNames[retCount] = strdup(pGni->secgroups[i].name);
                    }

                    if (doOutStructs) {
                        *pOutSecGroups = EUCA_REALLOC_C(*pOutSecGroups, (retCount + 1), sizeof (gni_instance));
                        pRetSecGroup = *pOutSecGroups;
                        memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof (gni_secgroup));
                    }
                    retCount++;
                }
            }
        }
    }

    if (doOutNames)
        *pOutNbSecGroupNames = retCount;

    if (doOutStructs)
        *pOutNbSecGroups = retCount;

    EUCA_FREE(pInstances);
    return (ret);
}

/**
 * Returns a copy of gni_secgroup data structures that the given instance is a member of.
 * @param gni [in] a pointer to our global network view structure
 * @param instance [in] gni_instance data structure of interest
 * @param secgroup_names [in] optional list of security groups to look for
 * @param max_secgroup_names [in] number of entries in the optional list of security groups
 * @param out_secgroup_names [out] list of found security group names
 * @param out_max_secgroup_names [out] number of entries in the list of found security groups
 * @param out_secgroups [out] array of found gni_secgroup data structures 
 * @param out_max_secgroups [out] number of entries in the found gni_secgroup data structures 
 * @return 0 on success. 1 on failure.
 */
int gni_instance_get_secgroups(globalNetworkInfo *gni, gni_instance *instance,
        char **secgroup_names, int max_secgroup_names, char ***out_secgroup_names,
        int *out_max_secgroup_names, gni_secgroup **out_secgroups, int *out_max_secgroups) {
    int ret = 0, getall = 0, i = 0, j = 0, k = 0, retcount = 0, do_outnames = 0, do_outstructs = 0;
    gni_secgroup *ret_secgroups = NULL;
    char **ret_secgroup_names = NULL;

    if (!gni || !instance) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (out_secgroup_names && out_max_secgroup_names) {
        do_outnames = 1;
    }
    if (out_secgroups && out_max_secgroups) {
        do_outstructs = 1;
    }

    if (!do_outnames && !do_outstructs) {
        LOGEXTREME("nothing to do, both output variables are NULL\n");
        return (0);
    }

    if (do_outnames) {
        *out_secgroup_names = NULL;
        *out_max_secgroup_names = 0;
    }
    if (do_outstructs) {
        *out_secgroups = NULL;
        *out_max_secgroups = 0;
    }

    if ((secgroup_names == NULL) || !strcmp(secgroup_names[0], "*")) {
        getall = 1;
        if (do_outnames)
            *out_secgroup_names = EUCA_ZALLOC_C(instance->max_secgroup_names, sizeof (char *));
        if (do_outstructs)
            *out_secgroups = EUCA_ZALLOC_C(instance->max_secgroup_names, sizeof (gni_secgroup));
    }

    if (do_outnames)
        ret_secgroup_names = *out_secgroup_names;
    if (do_outstructs)
        ret_secgroups = *out_secgroups;

    retcount = 0;
    for (i = 0; i < instance->max_secgroup_names; i++) {
        if (getall) {
            if (do_outnames)
                ret_secgroup_names[i] = strdup(instance->secgroup_names[i].name);
            if (do_outstructs) {
                for (k = 0; k < gni->max_secgroups; k++) {
                    if (!strcmp(gni->secgroups[k].name, instance->secgroup_names[i].name)) {
                        memcpy(&(ret_secgroups[i]), &(gni->secgroups[k]), sizeof (gni_secgroup));
                        break;
                    }
                }
            }
            retcount++;
        } else {
            for (j = 0; j < max_secgroup_names; j++) {
                if (!strcmp(secgroup_names[j], instance->secgroup_names[i].name)) {
                    if (do_outnames) {
                        *out_secgroup_names = EUCA_REALLOC_C(*out_secgroup_names, (retcount + 1), sizeof (char *));
                        ret_secgroup_names = *out_secgroup_names;
                        ret_secgroup_names[retcount] = strdup(instance->secgroup_names[i].name);
                    }
                    if (do_outstructs) {
                        for (k = 0; k < gni->max_secgroups; k++) {
                            if (!strcmp(gni->secgroups[k].name, instance->secgroup_names[i].name)) {
                                *out_secgroups = EUCA_REALLOC_C(*out_secgroups, (retcount + 1), sizeof (gni_secgroup));
                                ret_secgroups = *out_secgroups;
                                memcpy(&(ret_secgroups[retcount]), &(gni->secgroups[k]), sizeof (gni_secgroup));
                                break;
                            }
                        }
                    }
                    retcount++;
                }
            }
        }
    }
    if (do_outnames)
        *out_max_secgroup_names = retcount;
    if (do_outstructs)
        *out_max_secgroups = retcount;

    return (ret);

}

/**
 * Returns a copy of gni_instance data structures that the given security group contains.
 * @param gni [in] a pointer to our global network view structure
 * @param secgroup [in] gni_secgroup data structure of interest
 * @param instance_names [in] optional list of instance names to look for
 * @param max_instance_names [in] number of entries in the optional list of instance names
 * @param out_instance_names [out] list of found instance names
 * @param out_max_instance_names [out] number of entries in the list of found instance names
 * @param out_instances [out] array of found gni_instance data structures
 * @param out_max_instances [out] number of entries in the array of found gni_instance data structures
 * @return 0 on success. 1 on failure.
 */
int gni_secgroup_get_instances(globalNetworkInfo *gni, gni_secgroup *secgroup,
        char **instance_names, int max_instance_names, char ***out_instance_names,
        int *out_max_instance_names, gni_instance **out_instances, int *out_max_instances) {
    int ret = 0, getall = 0, i = 0, j = 0, retcount = 0, do_outnames = 0, do_outstructs = 0;
    gni_instance *ret_instances = NULL;
    char **ret_instance_names = NULL;

    if (!gni || !secgroup) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (out_instance_names && out_max_instance_names) {
        do_outnames = 1;
    }
    if (out_instances && out_max_instances) {
        do_outstructs = 1;
    }

    if (out_max_instance_names) {
        *out_max_instance_names = 0;
    }
    if (out_max_instances) {
        *out_max_instances = 0;
    }

    if (!do_outnames && !do_outstructs) {
        LOGEXTREME("nothing to do, both output variables are NULL\n");
        return (0);
    }
    if (secgroup->max_instances == 0) {
        LOGEXTREME("nothing to do, no instance associated with %s\n", secgroup->name);
        return (0);
    }
    if (do_outnames) {
        *out_instance_names = EUCA_ZALLOC_C(secgroup->max_instances, sizeof (char *));
        ret_instance_names = *out_instance_names;
    }
    if (do_outstructs) {
        *out_instances = EUCA_ZALLOC_C(secgroup->max_instances, sizeof (gni_instance));
        ret_instances = *out_instances;
    }

    if ((instance_names == NULL) || (!strcmp(instance_names[0], "*"))) {
        getall = 1;
    }

    retcount = 0;
    for (i = 0; i < secgroup->max_instances; i++) {
        if (getall) {
            if (do_outnames)
                ret_instance_names[i] = strdup(secgroup->instances[i]->name);
            if (do_outstructs) {
                memcpy(&(ret_instances[i]), secgroup->instances[i], sizeof (gni_instance));
            }
            retcount++;
        } else {
            for (j = 0; j < max_instance_names; j++) {
                if (!strcmp(instance_names[j], secgroup->instances[i]->name)) {
                    if (do_outnames) {
                        ret_instance_names[retcount] = strdup(secgroup->instances[i]->name);
                    }
                    if (do_outstructs) {
                        memcpy(&(ret_instances[retcount]), secgroup->instances[i], sizeof (gni_instance));
                    }
                    retcount++;
                }
            }
        }
    }
    if (do_outnames)
        *out_max_instance_names = retcount;
    if (do_outstructs)
        *out_max_instances = retcount;

    return (ret);
}

/**
 * Retrieve the interfaces that are members of the given security group.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param secgroup [in] a pointer to the gni_secgroup structure of the SG of interest
 * @param interface_names [in] restrict the search to this list of interface names
 * @param max_interface_names [in] number of interfaces specified
 * @param out_interface_names [out] array of interface names that were found
 * @param out_max_interface_names [out] number of interfaces found
 * @param out_interfaces [out] array of found interface structure pointers
 * @param out_max_interfaces [out] number of found interfaces
 *
 * @return 0 on success. 1 on failure.
 */
int gni_secgroup_get_interfaces(globalNetworkInfo *gni, gni_secgroup *secgroup,
        char **interface_names, int max_interface_names, char ***out_interface_names,
        int *out_max_interface_names, gni_instance *** out_interfaces, int *out_max_interfaces) {
    int ret = 0, getall = 0, i = 0, j = 0, retcount = 0, do_outnames = 0, do_outstructs = 0;
    gni_instance **ret_interfaces = NULL;
    char **ret_interface_names = NULL;

    if (!gni || !secgroup) {
        LOGERROR("Invalid argument: gni or secgroup is NULL - cannot get interfaces\n");
        return (1);
    }

    if (out_interface_names && out_max_interface_names) {
        do_outnames = 1;
    }
    if (out_interfaces && out_max_interfaces) {
        do_outstructs = 1;
    }
    if (!do_outnames && !do_outstructs) {
        LOGEXTREME("nothing to do, both output variables are NULL\n");
        return (0);
    }
    if (secgroup->max_interfaces == 0) {
        LOGEXTREME("nothing to do, no instances/interfaces associated with %s\n", secgroup->name);
        return (0);
    }

    if (do_outnames) {
        *out_interface_names = EUCA_ZALLOC_C(secgroup->max_interfaces, sizeof (char *));
        if (*out_interface_names == NULL) {
            LOGFATAL("out of memory: failed to allocate out_interface_names\n");
            do_outnames = 0;
        }
        *out_max_interface_names = 0;
    }
    if (do_outstructs) {
        *out_interfaces = EUCA_ZALLOC_C(secgroup->max_interfaces, sizeof (gni_instance *));
        if (*out_interfaces == NULL) {
            LOGFATAL("out of memory: failed to allocate out_interfaces\n");
            do_outstructs = 0;
        }
        *out_max_interfaces = 0;
    }

    if ((interface_names == NULL) || (!strcmp(interface_names[0], "*"))) {
        getall = 1;
    }

    if (do_outnames)
        ret_interface_names = *out_interface_names;
    if (do_outstructs)
        ret_interfaces = *out_interfaces;

    retcount = 0;
    for (i = 0; i < secgroup->max_interfaces; i++) {
        if (getall) {
            if (do_outnames)
                ret_interface_names[i] = strdup(secgroup->interfaces[i]->name);
            if (do_outstructs) {
                ret_interfaces[i] = secgroup->interfaces[i];
            }
            retcount++;
        } else {
            for (j = 0; j < max_interface_names; j++) {
                if (!strcmp(interface_names[j], secgroup->interfaces[i]->name)) {
                    if (do_outnames) {
                        ret_interface_names[retcount] = strdup(secgroup->interfaces[i]->name);
                    }
                    if (do_outstructs) {
                        ret_interfaces[retcount] = secgroup->interfaces[i];
                    }
                    retcount++;
                }
            }
        }
    }
    if (do_outnames)
        *out_max_interface_names = retcount;
    if (do_outstructs)
        *out_max_interfaces = retcount;

    return (ret);
}

/**
 * Invoke and parse results of xmlXPathEvalExpression()
 * @param ctxptr [in] pointer to xmlXPathContext
 * @param doc [in] the xml document of interest
 * @param startnode [in] xmlNodePtr where the search should start
 * @param expression [in] expression to evaluate. Path should be relative to startnode
 * @param results [out] parsed results (array of strings)
 * @param max_results [out] number of elements in results
 * @param resultnodeset [out] pointer to xmlNodeSet from the query result
 * @return 0 on success. 1 on failure.
 */
int evaluate_xpath_property(xmlXPathContextPtr ctxptr, xmlDocPtr doc, xmlNodePtr startnode, char *expression, char ***results, int *max_results) {
    int i, max_nodes = 0, result_count = 0;
    xmlXPathObjectPtr objptr;
    char **retresults;
    int res = 0;

    *max_results = 0;

    if ((!ctxptr) || (!doc) || (!results)) {
        LOGWARN("Invalid argument: null xmlXPathContext, xmlDoc, or results\n");
        res = 1;
    } else {
        ctxptr->node = startnode;
        ctxptr->doc = doc;
        objptr = xmlXPathEvalExpression((unsigned char *) expression, ctxptr);
        if (objptr == NULL) {
            LOGERROR("unable to evaluate xpath expression '%s'\n", expression);
            res = 1;
        } else {
            if (objptr->nodesetval) {
                max_nodes = (int) objptr->nodesetval->nodeNr;
                *results = EUCA_ZALLOC_C(max_nodes, sizeof (char *));
                retresults = *results;
                for (i = 0; i < max_nodes; i++) {
                    if (objptr->nodesetval->nodeTab[i] && objptr->nodesetval->nodeTab[i]->children && objptr->nodesetval->nodeTab[i]->children->content) {
                        retresults[result_count] = strdup((char *) objptr->nodesetval->nodeTab[i]->children->content);
                        result_count++;
                    }
                }
                *max_results = result_count;

                LOGEXTREME("%d results after evaluated expression %s\n", *max_results, expression);
                for (i = 0; i < *max_results; i++) {
                    LOGEXTREME("\tRESULT %d: %s\n", i, retresults[i]);
                }
            }
        }
        xmlXPathFreeObject(objptr);
    }

    return (res);
}

/**
 * Invoke and parse results of xmlXPathEvalExpression()
 * @param ctxptr [in] pointer to xmlXPathContext
 * @param doc [in] the xml document of interest
 * @param startnode [in] xmlNodePtr where the search should start
 * @param expression [in] expression to evaluate. Path should be relative to startnode
 * @param results [out] parsed results (array of strings)
 * @param max_results [out] number of elements in results
 * @param resultnodeset [out] pointer to xmlNodeSet from the query result
 * @return 0 on success. 1 on failure.
 */
int evaluate_xpath_element(xmlXPathContextPtr ctxptr, xmlDocPtr doc, xmlNodePtr startnode, char *expression, char ***results, int *max_results) {
    int i, max_nodes = 0, result_count = 0;
    xmlXPathObjectPtr objptr;
    char **retresults;
    int res = 0;

    *max_results = 0;

    if ((!ctxptr) || (!doc) || (!results)) {
        LOGERROR("Invalid argument: NULL xpath context, xmlDoc, or results\n");
        res = 1;
    } else {
        ctxptr->node = startnode;
        ctxptr->doc = doc;
        objptr = xmlXPathEvalExpression((unsigned char *) expression, ctxptr);
        if (objptr == NULL) {
            LOGERROR("unable to evaluate xpath expression '%s'\n", expression);
            res = 1;
        } else {
            if (objptr->nodesetval) {
                max_nodes = (int) objptr->nodesetval->nodeNr;
                *results = EUCA_ZALLOC_C(max_nodes, sizeof (char *));
                retresults = *results;
                for (i = 0; i < max_nodes; i++) {
                    if (objptr->nodesetval->nodeTab[i] && objptr->nodesetval->nodeTab[i]->properties && objptr->nodesetval->nodeTab[i]->properties->children
                            && objptr->nodesetval->nodeTab[i]->properties->children->content) {
                        retresults[result_count] = strdup((char *) objptr->nodesetval->nodeTab[i]->properties->children->content);
                        result_count++;
                    }
                }
                *max_results = result_count;

                LOGTRACE("%d results after evaluated expression %s\n", *max_results, expression);
                for (i = 0; i < *max_results; i++) {
                    LOGTRACE("\tRESULT %d: %s\n", i, retresults[i]);
                }
            }
        }
        xmlXPathFreeObject(objptr);
    }

    return (res);
}

/**
 * Evaluates XPATH and retrieves the xmlNodeSet of the query.
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] a pointer to the XML document
 * @param startnode [in] xmlNodePtr where the search should start
 * @param expression [in] expression a string pointer to the expression we want to evaluate
 * @param nodeset [out] xmlNodeSetPtr of the query.
 * @return 0 on success. 1 otherwise.
 */
int evaluate_xpath_nodeset(xmlXPathContextPtr ctxptr, xmlDocPtr doc, xmlNodePtr startnode, char *expression, xmlNodeSetPtr nodeset) {
    xmlXPathObjectPtr objptr;
    int res = 0;

    if (!nodeset) {
        LOGWARN("cannot return nodeset to NULL\n");
        return (1);
    }
    bzero(nodeset, sizeof (xmlNodeSet));
    if ((!ctxptr) || (!doc) || (!nodeset)) {
        LOGERROR("Invalid argument: NULL xpath context, xmlDoc, or nodeset\n");
        res = 1;
    } else {
        ctxptr->node = startnode;
        objptr = xmlXPathEvalExpression((unsigned char *) expression, ctxptr);
        if (objptr == NULL) {
            LOGERROR("unable to evaluate xpath expression '%s'\n", expression);
            res = 1;
        } else {
            if (objptr->nodesetval) {
                nodeset->nodeNr = objptr->nodesetval->nodeNr;
                nodeset->nodeMax = objptr->nodesetval->nodeMax;
                nodeset->nodeTab = EUCA_ZALLOC_C(nodeset->nodeMax, sizeof (xmlNodePtr));
                memcpy(nodeset->nodeTab, objptr->nodesetval->nodeTab, nodeset->nodeMax * sizeof (xmlNodePtr));
            }
        }
        xmlXPathFreeObject(objptr);
    }
    return (res);
}

/**
 * Allocates and initializes a new globalNetworkInfo structure.
 * @return A pointer to the newly allocated structure or NULL if any failure occurred.
 */
globalNetworkInfo *gni_init() {
    globalNetworkInfo *gni = NULL;
    gni = EUCA_ZALLOC_C(1, sizeof (globalNetworkInfo));

    gni->init = 1;
    return (gni);
}

/**
 * Populates a given globalNetworkInfo structure from the content of an XML file
 * @param gni [in] a pointer to the global network information structure
 * @param host_info [in] a pointer to the hostname info data structure (only relevant to VPCMIDO - to be deprecated)
 * @param xmlpath [in] path to the XML file to be used to populate
 * @return 0 on success or 1 on failure
 */
int gni_populate(globalNetworkInfo *gni, gni_hostname_info *host_info, char *xmlpath) {
    return (gni_populate_v(GNI_POPULATE_ALL, gni, host_info, xmlpath));
}

/**
 * Populates a given globalNetworkInfo structure from the content of an XML file
 * @param mode [in] mode what to populate GNI_POPULATE_ALL || GNI_POPULATE_CONFIG || GNI_POPULATE_NONE
 * @param gni [in] a pointer to the global network information structure
 * @param host_info [in] a pointer to the hostname info data structure (only relevant to VPCMIDO - to be deprecated)
 * @param xmlpath [in] path to the XML file to be used to populate
 * @return 0 on success or 1 on failure
 */
int gni_populate_v(int mode, globalNetworkInfo *gni, gni_hostname_info *host_info, char *xmlpath) {
    int rc = 0;
    xmlDocPtr docptr;
    xmlXPathContextPtr ctxptr;
    struct timeval tv, ttv;
    xmlNode *gni_nodes[GNI_XPATH_INVALID] = {0};

    if (mode == GNI_POPULATE_NONE) {
        return (0);
    }

    eucanetd_timer_usec(&ttv);
    eucanetd_timer_usec(&tv);
    if (!gni) {
        LOGERROR("invalid input\n");
        return (1);
    }

    gni_clear(gni);
    LOGTRACE("gni cleared in %ld us.\n", eucanetd_timer_usec(&tv));

    XML_INIT();
    LIBXML_TEST_VERSION
    docptr = xmlParseFile(xmlpath);
    if (docptr == NULL) {
        LOGERROR("unable to parse XML file (%s)\n", xmlpath);
        return (1);
    }

    ctxptr = xmlXPathNewContext(docptr);
    if (ctxptr == NULL) {
        LOGERROR("unable to get new xml context\n");
        xmlFreeDoc(docptr);
        return (1);
    }
    LOGTRACE("xml Xpath context - %ld us.\n", eucanetd_timer_usec(&tv));

    eucanetd_timer_usec(&tv);
    rc = gni_populate_xpathnodes(docptr, gni_nodes);

    LOGTRACE("begin parsing XML into data structures\n");

    // GNI version
    rc = gni_populate_gnidata(gni, gni_nodes[GNI_XPATH_CONFIGURATION], ctxptr, docptr);
    LOGTRACE("gni version populated in %ld us.\n", eucanetd_timer_usec(&tv));

    if (mode == GNI_POPULATE_ALL) {
        // Instances
        rc = gni_populate_instances(gni, gni_nodes[GNI_XPATH_INSTANCES], ctxptr, docptr);
        LOGTRACE("gni instances populated in %ld us.\n", eucanetd_timer_usec(&tv));

        // Security Groups
        rc = gni_populate_sgs(gni, gni_nodes[GNI_XPATH_SECURITYGROUPS], ctxptr, docptr);
        LOGTRACE("gni sgs populated in %ld us.\n", eucanetd_timer_usec(&tv));

        // VPCs
        rc = gni_populate_vpcs(gni, gni_nodes[GNI_XPATH_VPCS], ctxptr, docptr);
        LOGTRACE("gni vpcs populated in %ld us.\n", eucanetd_timer_usec(&tv));
        
        // Internet Gateways
        rc = gni_populate_internetgateways(gni, gni_nodes[GNI_XPATH_INTERNETGATEWAYS], ctxptr, docptr);
        LOGTRACE("gni Internet Gateways populated in %ld us.\n", eucanetd_timer_usec(&tv));

        // DHCP Option Sets
        rc = gni_populate_dhcpos(gni, gni_nodes[GNI_XPATH_DHCPOPTIONSETS], ctxptr, docptr);
        LOGTRACE("gni DHCP Option Sets populated in %ld us.\n", eucanetd_timer_usec(&tv));
    }

    // Configuration
    rc = gni_populate_configuration(gni, host_info, gni_nodes[GNI_XPATH_CONFIGURATION], ctxptr, docptr);
    LOGTRACE("gni configuration populated in %ld us.\n", eucanetd_timer_usec(&tv));

    xmlXPathFreeContext(ctxptr);
    xmlFreeDoc(docptr);

    if (mode == GNI_POPULATE_ALL) {
        // Find VPC and subnet interfaces
        for (int i = 0; i < gni->max_vpcs; i++) {
            gni_vpc *vpc = &(gni->vpcs[i]);
            rc = gni_vpc_get_interfaces(gni, vpc, &(vpc->interfaces), &(vpc->max_interfaces));
            if (rc) {
                LOGWARN("Failed to populate gni %s interfaces.\n", vpc->name);
            }
            vpc->dhcpOptionSet = gni_get_dhcpos(gni, vpc->dhcpOptionSet_name, NULL);
            for (int j = 0; j < vpc->max_subnets; j++) {
                gni_vpcsubnet *gnisubnet = &(vpc->subnets[j]);
                rc = gni_vpcsubnet_get_interfaces(gni, gnisubnet, vpc->interfaces, vpc->max_interfaces,
                        &(gnisubnet->interfaces), &(gnisubnet->max_interfaces));
                if (rc) {
                    LOGWARN("Failed to populate gni %s interfaces.\n", gnisubnet->name);
                }
                gnisubnet->networkAcl = gni_get_networkacl(vpc, gnisubnet->networkAcl_name, NULL);
            }
        }
    }
    LOGTRACE("end parsing XML into data structures\n");

    eucanetd_timer_usec(&tv);
    rc = gni_validate(gni);
    if (rc) {
        LOGDEBUG("could not validate GNI after XML parse: check network config\n");
        return (1);
    }
    LOGDEBUG("gni validated in %ld us.\n", eucanetd_timer_usec(&tv));

    LOGDEBUG("gni populated in %.2f ms.\n", eucanetd_timer_usec(&ttv) / 1000.0);

/*
    for (int i = 0; i < gni->max_instances; i++) {
        gni_instance_interface_print(&(gni->instances[i]), EUCA_LOG_INFO);
    }
    for (int i = 0; i < gni->max_interfaces; i++) {
        gni_instance_interface_print(&(gni->interfaces[i]), EUCA_LOG_INFO);
    }
    for (int j = 0; j < gni->max_secgroups; j++) {
        gni_sg_print(&(gni->secgroups[j]), EUCA_LOG_INFO);
    }
    for (int i = 0; i < gni->max_vpcs; i++) {
        gni_vpc_print(&(gni->vpcs[i]), EUCA_LOG_INFO);
    }
    for (int i = 0; i < gni->max_vpcIgws; i++) {
        gni_internetgateway_print(&(gni->vpcIgws[i]), EUCA_LOG_INFO);
    }
    for (int i = 0; i < gni->max_dhcpos; i++) {
        gni_dhcpos_print(&(gni->dhcpos[i]), EUCA_LOG_INFO);
    }
*/

    return (0);
}

/**
 * Retrieve pointers to xmlNode of GNI top level nodes (i.e., configuration, vpcs,
 * instances, dhcpOptionSets, internetGateways, securityGroups).
 * @param doc [in] xml document to be used
 * @param gni_nodes [in] an array of pointers to xmlNode (sufficient space for all
 * gni_xpath_node_type is expected).
 * @return 0 on success (array of pointers can have NULL elements if not found).
 * 1 on error.
 */
int gni_populate_xpathnodes(xmlDocPtr doc, xmlNode **gni_nodes) {
    xmlNodePtr node = NULL;

    if (doc && doc->children) {
        node = doc->children;
    } else {
        LOGERROR("Cannot populate from NULL xml ctx\n");
        return (1);
    }

    if (!node) {
        LOGERROR("Cannot populate from empty xml\n");
        return (1);
    }

    if (xmlStrcmp(node->name, (const xmlChar *) "network-data")) {
        LOGERROR("network-data node not found in GNI xml\n");
        return (1);
    }

    node = node->children;
    if (!node) {
        LOGTRACE("Empty xml ctx\n");
        return (0);
    }

    while (node) {
        int nodetype = gni_xmlstr2type(node->name);
        if (nodetype == GNI_XPATH_INVALID) {
            LOGTRACE("Unknown GNI xml node %s\n", node->name);
            node = node->next;
            continue;
        }
        gni_nodes[nodetype] = node;
        node = node->next;
    }
    return (0);
}

/**
 * Converts an xml node name to a numeric representation.
 * @param nodename [in] xml node name of interest.
 * @return numeric representation of the xml node of interest.
 */
gni_xpath_node_type gni_xmlstr2type(const xmlChar *nodename) {
    if (!xmlStrcmp(nodename, (const xmlChar *) "configuration")) {
        return (GNI_XPATH_CONFIGURATION);
    }
    if (!xmlStrcmp(nodename, (const xmlChar *) "vpcs")) {
        return (GNI_XPATH_VPCS);
    }
    if (!xmlStrcmp(nodename, (const xmlChar *) "instances")) {
        return (GNI_XPATH_INSTANCES);
    }
    if (!xmlStrcmp(nodename, (const xmlChar *) "dhcpOptionSets")) {
        return (GNI_XPATH_DHCPOPTIONSETS);
    }
    if (!xmlStrcmp(nodename, (const xmlChar *) "internetGateways")) {
        return (GNI_XPATH_INTERNETGATEWAYS);
    }
    if (!xmlStrcmp(nodename, (const xmlChar *) "securityGroups")) {
        return (GNI_XPATH_SECURITYGROUPS);
    }
    return (GNI_XPATH_INVALID);
}

/**
 * Populates globalNetworkInfo data from the content of an XML
 * file (xmlXPathContext is expected).
 *
 * @param gni [in] a pointer to the global network information structure
 * @param xmlnode [in] pointer to the "configuration" xmlNode
 * @param ctxptr [in] pointer to xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_gnidata(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0;
    int i = 0;

    if ((gni == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: gni or doc is NULL.\n");
        return (1);
    }
    if (gni->init == FALSE) {
        LOGERROR("Invalid argument: gni is not initialized.\n");
        return (1);
    }

    if (xmlnode && xmlnode->name) {
        snprintf(expression, 2048, "./property[@name='mode']/value");
        rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->sMode, NETMODE_LEN, results[i]);
            gni->nmCode = euca_netmode_atoi(gni->sMode);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);
    }

    // get version and applied version
    snprintf(expression, 2048, "/network-data/@version");
    rc += evaluate_xpath_property(ctxptr, doc, NULL, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->version, GNI_VERSION_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "/network-data/@applied-version");
    rc += evaluate_xpath_property(ctxptr, doc, NULL, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->appliedVersion, GNI_VERSION_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    return (0);
}

/**
 * Populates globalNetworkInfo eucanetd configuration from the content of an XML
 * file (xmlXPathContext is expected). Relevant sections of globalNetworkInfo
 * structure is expected to be empty/clean.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param host_info [in] pointer to hostname_info structure (populated as needed) - deprecated (EUCA-11997)
 * @param xmlnode [in] pointer to the "configuration" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_configuration(globalNetworkInfo *gni, gni_hostname_info *host_info, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048], *strptra = NULL;
    char **results = NULL;
    int max_results = 0;
    int i, j, k, l, m;
    xmlNodeSet nodeset = {0};
    xmlNodePtr startnode;

    if ((gni == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: gni or doc is NULL.\n");
        return (1);
    }
    if ((gni->init == FALSE)) {
        LOGERROR("Invalid argument: gni is not initialized or instances section is not empty.\n");
        return (1);
    }
    if (!xmlnode || !xmlnode->name) {
        LOGERROR("Invalid argument: configuration xml node is required\n");
        return (1);
    }

    snprintf(expression, 2048, "./property[@name='enabledCLCIp']/value");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        gni->enabledCLCIp = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./property[@name='instanceDNSDomain']/value");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->instanceDNSDomain, HOSTNAME_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    if (IS_NETMODE_VPCMIDO(gni)) {
        char *peer_ip = NULL;
        char *external_cidr = NULL;

        snprintf(expression, 2048, "./property[@name='mido']");
        rc = evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
        if (nodeset.nodeNr >= 1) {
            startnode = nodeset.nodeTab[0];

            /* pre-4.3 Mido Gateway (start)*/
            snprintf(expression, 2048, "./property[@name='publicNetworkCidr']/value");
            rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                EUCA_FREE(external_cidr);
                external_cidr = strdup(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "./property[@name='publicGatewayIP']/value");
            rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                EUCA_FREE(peer_ip);
                peer_ip = strdup(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            u32 asn = 0;
            snprintf(expression, 2048, "./property[@name='bgpAsn']/value");
            rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                asn = (u32) atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            int max_gws = 0;
            xmlNodeSet gwnodeset = {0};

            snprintf(expression, 2048, "./property[@name='gateways']/gateway");
            rc = evaluate_xpath_nodeset(ctxptr, doc, startnode, expression, &gwnodeset);
            LOGTRACE("Found %d gateways\n", gwnodeset.nodeNr);

            max_gws = gwnodeset.nodeNr;
            gni->midogws = EUCA_ZALLOC_C(max_gws, sizeof (gni_mido_gateway));
            gni->max_midogws = max_gws;
            for (j = 0; j < max_gws; j++) {
                gni_mido_gateway *midogw = &(gni->midogws[j]);

                startnode = gwnodeset.nodeTab[j];
                snprintf(expression, 2048, "./property[@name='gatewayHost']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    snprintf(midogw->host, HOSTNAME_LEN, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                snprintf(expression, 2048, "./property[@name='gatewayIP']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    snprintf(midogw->ext_ip, INET_ADDR_LEN, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                snprintf(expression, 2048, "./property[@name='gatewayInterface']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    snprintf(midogw->ext_dev, IF_NAME_LEN, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                if (external_cidr) {
                    snprintf(midogw->ext_cidr, NETWORK_ADDR_LEN, "%s", external_cidr);
                }
                if (peer_ip) {
                    snprintf(midogw->peer_ip, INET_ADDR_LEN, "%s", peer_ip);
                }
                /* pre-4.3 Mido Gateway (end)*/
                
                /* pre-4.3 Mido Gateway values are overwritten if 4.4 values are present*/
                snprintf(expression, 2048, "./property[@name='ip']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    snprintf(midogw->host, INET_ADDR_LEN, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                snprintf(expression, 2048, "./property[@name='externalCidr']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    snprintf(midogw->ext_cidr, NETWORK_ADDR_LEN, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                snprintf(expression, 2048, "./property[@name='externalIp']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    snprintf(midogw->ext_ip, INET_ADDR_LEN, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                snprintf(expression, 2048, "./property[@name='externalDevice']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    snprintf(midogw->ext_dev, IF_NAME_LEN, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                // static router
                snprintf(expression, 2048, "./property[@name='externalRouterIp']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    snprintf(midogw->peer_ip, INET_ADDR_LEN, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);
                
                // BGP parameters
                snprintf(expression, 2048, "./property[@name='bgpPeerIp']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    snprintf(midogw->peer_ip, INET_ADDR_LEN, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);
                
                snprintf(expression, 2048, "./property[@name='bgpPeerAsn']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    midogw->peer_asn = (u32) atoi(results[i]);
                    midogw->asn = asn;
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                snprintf(expression, 2048, "./property[@name='bgpAdRoutes']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                midogw->ad_routes = EUCA_ZALLOC_C(max_results, sizeof (char *));
                midogw->max_ad_routes = max_results;
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    midogw->ad_routes[i] = strdup(results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);
            }
            EUCA_FREE(gwnodeset.nodeTab);
            EUCA_FREE(external_cidr);
            EUCA_FREE(peer_ip);

            if (max_gws <= 0) {
                LOGERROR("Invalid mido gateway(s) detected. Check network configuration.\n");
            }
        } else {
            LOGTRACE("mido section not found in GNI\n");
        }
        EUCA_FREE(nodeset.nodeTab);
    }

    snprintf(expression, 2048, "./property[@name='instanceDNSServers']/value");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    gni->instanceDNSServers = EUCA_ZALLOC_C(max_results, sizeof (u32));
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        gni->instanceDNSServers[i] = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    gni->max_instanceDNSServers = max_results;
    EUCA_FREE(results);

    snprintf(expression, 2048, "./property[@name='publicIps']/value");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    gni->public_ips_str = EUCA_ZALLOC_C(max_results, sizeof (char *));
    gni->max_public_ips_str = max_results;
    if (results && max_results) {
        // expand ips list only if/when necessary
/*
        rc += gni_serialize_iprange_list(results, max_results, &(gni->public_ips), &(gni->max_public_ips));
*/
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            gni->public_ips_str[i] = strdup(results[i]);
            EUCA_FREE(results[i]);
        }
    }
    EUCA_FREE(results);

    // Do we have any global subnets?
    snprintf(expression, 2048, "./property[@name='subnets']/subnet");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        gni->subnets = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_subnet));
        gni->max_subnets = nodeset.nodeNr;

        for (j = 0; j < gni->max_subnets; j++) {
            startnode = nodeset.nodeTab[j];
            if (startnode) {
                // Get the subnet
                snprintf(expression, 2048, "./property[@name='subnet']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    gni->subnets[j].subnet = dot2hex(results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                // Get the netmask
                snprintf(expression, 2048, "./property[@name='netmask']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    gni->subnets[j].netmask = dot2hex(results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                strptra = hex2dot(gni->subnets[j].subnet);
                snprintf(expression, 2048, "./property[@name='gateway']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    gni->subnets[j].gateway = dot2hex(results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);
                EUCA_FREE(strptra);
            } else {
                LOGWARN("invalid global subnet at idx %d\n", j);
            }
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    // Clusters
    snprintf(expression, 2048, "./property[@name='clusters']/cluster");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        gni->clusters = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_cluster));
        gni->max_clusters = nodeset.nodeNr;

        for (j = 0; j < gni->max_clusters; j++) {
            startnode = nodeset.nodeTab[j];
            if (startnode && startnode->properties) {
                for (xmlAttr *prop = startnode->properties; prop != NULL; prop = prop->next) {
                    if (!strcmp((char *) prop->name, "name")) {
                        snprintf(gni->clusters[j].name, HOSTNAME_LEN, "%s", (char *) prop->children->content);
                        break;
                    }
                }

                snprintf(expression, 2048, "./property[@name='enabledCCIp']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    gni->clusters[j].enabledCCIp = dot2hex(results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                snprintf(expression, 2048, "./property[@name='macPrefix']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                    snprintf(gni->clusters[j].macPrefix, ENET_MACPREFIX_LEN, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);

                snprintf(expression, 2048, "./property[@name='privateIps']/value");
                rc += evaluate_xpath_property(ctxptr, doc, startnode, expression, &results, &max_results);
                gni->clusters[j].private_ips_str = EUCA_ZALLOC_C(max_results, sizeof (char *));
                gni->clusters[j].max_private_ips_str = max_results;
                if (results && max_results) {
                    // expand ips list only if/when necessary
/*
                    rc += gni_serialize_iprange_list(results, max_results, &(gni->clusters[j].private_ips), &(gni->clusters[j].max_private_ips));
*/
                    for (i = 0; i < max_results; i++) {
                        LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
                        gni->clusters[j].private_ips_str[i] = strdup(results[i]);
                        EUCA_FREE(results[i]);
                    }
                }
                EUCA_FREE(results);

                xmlNodeSet snnodeset = {0};
                xmlNodePtr snstartnode = NULL;
                snprintf(expression, 2048, "./subnet");
                rc += evaluate_xpath_nodeset(ctxptr, doc, startnode, expression, &snnodeset);
                if (snnodeset.nodeNr > 0) {
                    snstartnode = snnodeset.nodeTab[0];
                    if (snstartnode) {
                        snprintf(expression, 2048, "./property[@name='subnet']/value");
                        rc += evaluate_xpath_property(ctxptr, doc, snstartnode, expression, &results, &max_results);
                        for (i = 0; i < max_results; i++) {
                            LOGTRACE("\t\tafter function: %d: %s\n", i, results[i]);
                            gni->clusters[j].private_subnet.subnet = dot2hex(results[i]);
                            EUCA_FREE(results[i]);
                        }
                        EUCA_FREE(results);

                        snprintf(expression, 2048, "./property[@name='netmask']/value");
                        rc += evaluate_xpath_property(ctxptr, doc, snstartnode, expression, &results, &max_results);
                        for (i = 0; i < max_results; i++) {
                            LOGTRACE("\t\tafter function: %d: %s\n", i, results[i]);
                            gni->clusters[j].private_subnet.netmask = dot2hex(results[i]);
                            EUCA_FREE(results[i]);
                        }
                        EUCA_FREE(results);

                        snprintf(expression, 2048, "./property[@name='gateway']/value");
                        rc += evaluate_xpath_property(ctxptr, doc, snstartnode, expression, &results, &max_results);
                        for (i = 0; i < max_results; i++) {
                            LOGTRACE("\t\tafter function: %d: %s\n", i, results[i]);
                            gni->clusters[j].private_subnet.gateway = dot2hex(results[i]);
                            EUCA_FREE(results[i]);
                        }
                        EUCA_FREE(results);
                    }
                }
                EUCA_FREE(snnodeset.nodeTab);

                xmlNodeSet nnodeset = {0};
                xmlNodePtr nstartnode = NULL;
                snprintf(expression, 2048, "./property[@name='nodes']/node");
                rc += evaluate_xpath_nodeset(ctxptr, doc, startnode, expression, &nnodeset);
                if (nnodeset.nodeNr > 0) {
                    gni->clusters[j].nodes = EUCA_ZALLOC_C(nnodeset.nodeNr, sizeof (gni_node));
                    gni->clusters[j].max_nodes = nnodeset.nodeNr;

                    for (k = 0; k < nnodeset.nodeNr; k++) {
                        nstartnode = nnodeset.nodeTab[k];
                        if (nstartnode && nstartnode->properties) {
                            for (xmlAttr *prop = nstartnode->properties; prop != NULL; prop = prop->next) {
                                if (!strcmp((char *) prop->name, "name")) {
                                    snprintf(gni->clusters[j].nodes[k].name, HOSTNAME_LEN, "%s", (char *) prop->children->content);
                                    break;
                                }
                            }
                        }

                        snprintf(expression, 2048, "./instanceIds/value");
                        rc += evaluate_xpath_property(ctxptr, doc, nstartnode, expression, &results, &max_results);
                        gni->clusters[j].nodes[k].instance_names = EUCA_ZALLOC_C(max_results, sizeof (gni_name_32));
                        for (i = 0; i < max_results; i++) {
                            LOGTRACE("\t\t\tafter function: %d: %s\n", i, results[i]);
                            snprintf(gni->clusters[j].nodes[k].instance_names[i].name, 32, "%s", results[i]);
                            EUCA_FREE(results[i]);

                            char *nc = gni->clusters[j].nodes[k].name;
                            char *instid = gni->clusters[j].nodes[k].instance_names[i].name;
                            for (l = 0; l < gni->max_instances; l++) {
                                if (!strcmp(gni->instances[l]->name, instid)) {
                                    snprintf(gni->instances[l]->node, HOSTNAME_LEN, "%s", nc);
                                    if (IS_NETMODE_VPCMIDO(gni)) {
                                        for (m = 0; m < gni->instances[l]->max_interfaces; m++) {
                                            snprintf(gni->instances[l]->interfaces[m]->node, HOSTNAME_LEN, "%s", nc);
                                        }
                                    }
                                    l = gni->max_instances;
                                }
                            }
                        }
                        if (IS_NETMODE_VPCMIDO(gni)) {
                            for (m = 0; m < gni->max_ifs; m++) {
                                LOGTRACE("\t%s is on NC %s\n", gni->ifs[m]->name, gni->ifs[m]->node);
                            }
                        }
                        gni->clusters[j].nodes[k].max_instance_names = max_results;
                        EUCA_FREE(results);
                    }
                }
                EUCA_FREE(nnodeset.nodeTab);

            } else {
                LOGWARN("invalid cluster at idx %d\n", j);
            }
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    return (0);
}

/**
 * Populates globalNetworkInfo instances structure from the content of an XML
 * file (xmlXPathContext is expected). The instances section of globalNetworkInfo
 * structure is expected to be empty/clean.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param xmlnode [in] pointer to the "instances" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_instances(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    int i;

    if ((gni == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
    }
    if ((gni->init == FALSE) || (gni->max_instances != 0)) {
        LOGERROR("Invalid argument: gni is not initialized or instances section is not empty.\n");
        return (1);
    }
    xmlNodeSet nodeset = {0};
    snprintf(expression, 2048, "./instance");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        gni->instances = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_instance *));
        gni->max_instances = nodeset.nodeNr;
    }
    LOGTRACE("Found %d instances\n", gni->max_instances);
    for (i = 0; i < gni->max_instances; i++) {
        if (nodeset.nodeTab[i]) {
            gni->instances[i] = EUCA_ZALLOC_C(1, sizeof (gni_instance));
            gni_populate_instance_interface(gni->instances[i], nodeset.nodeTab[i], ctxptr, doc);
            //gni_instance_interface_print(gni->instances[i], EUCA_LOG_INFO);
            gni_populate_interfaces(gni, gni->instances[i], nodeset.nodeTab[i], ctxptr, doc);
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    return 0;
}

/**
 * Populates globalNetworkInfo interfaces structure. Appends the interfaces of the
 * instance specified in xmlnode..
 *
 * @param gni [in] a pointer to the global network information structure
 * @param instance [in] instance that has the interfaces of interest
 * @param xmlnode [in] pointer to the "configuration" xmlNode (if NULL, full path search is performed)
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_interfaces(globalNetworkInfo *gni, gni_instance *instance, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    int i;

    if ((gni == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
    }
    // interfaces are only relevant in VPCMIDO mode
    if (!IS_NETMODE_VPCMIDO(gni)) {
        return (0);
    }
    if (gni->init == FALSE) {
        LOGERROR("Invalid argument: gni is not initialized.\n");
        return (1);
    }

    xmlNodeSet nodeset = {0};
    snprintf(expression, 2048, "./networkInterfaces/networkInterface");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        instance->interfaces = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_instance *));
        instance->max_interfaces = nodeset.nodeNr;
        //gni->interfaces = EUCA_REALLOC_C(gni->interfaces, gni->max_interfaces + nodeset.nodeNr, sizeof (gni_instance));
        //memset(&(gni->interfaces[gni->max_interfaces]), 0, nodeset.nodeNr * sizeof (gni_instance));
        gni->ifs = EUCA_REALLOC_C(gni->ifs, gni->max_ifs + nodeset.nodeNr, sizeof (gni_instance *));
        memset(&(gni->ifs[gni->max_ifs]), 0, nodeset.nodeNr * sizeof (gni_instance *));
        LOGTRACE("Found %d interfaces\n", nodeset.nodeNr);
        for (i = 0; i < nodeset.nodeNr; i++) {
            if (nodeset.nodeTab[i]) {
                //snprintf(gni->interfaces[gni->max_interfaces + i].instance_name.name, 1024, instance->name);
                //gni_populate_instance_interface(&(gni->interfaces[gni->max_interfaces + i]), nodeset.nodeTab[i], ctxptr, doc);
                gni->ifs[gni->max_ifs + i] = EUCA_ZALLOC_C(1, sizeof (gni_instance));
                snprintf(gni->ifs[gni->max_ifs + i]->instance_name.name, 32, instance->name);
                gni_populate_instance_interface(gni->ifs[gni->max_ifs + i], nodeset.nodeTab[i], ctxptr, doc);
                instance->interfaces[i] = gni->ifs[gni->max_ifs + i];
                //gni_instance_interface_print(gni->ifs[gni->max_ifs + i]), EUCA_LOG_INFO);
            }
        }
        //gni->max_interfaces += nodeset.nodeNr;
        gni->max_ifs += nodeset.nodeNr;
    }
    EUCA_FREE(nodeset.nodeTab);

    return (0);
}

/**
 * Populates globalNetworkInfo instance structure from the content of an XML
 * file (xmlXPathContext is expected). The target instance structure is assumed
 * to be clean.
 *
 * @param instance [in] a pointer to the global network information instance structure
 * @param xmlnode [in] pointer to the "configuration" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_instance_interface(gni_instance *instance, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i;
    boolean is_instance = TRUE;

    if ((instance == NULL) || (xmlnode == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: instance or ctxptr is NULL.\n");
        return (1);
    }

    if (xmlnode && xmlnode->properties) {
        for (xmlAttr *prop = xmlnode->properties; prop != NULL; prop = prop->next) {
            if (!strcmp((char *) prop->name, "name")) {
                snprintf(instance->name, INTERFACE_ID_LEN, "%s", (char *) prop->children->content);
                break;
            }
        }
    }

    if ((instance->name == NULL) || (strlen(instance->name) == 0)) {
        LOGERROR("Invalid argument: invalid instance name.\n");
    }

    if (strstr(instance->name, "eni-")) {
        is_instance = FALSE;
    } else {
        is_instance = TRUE;
    }
    snprintf(expression, 2048, "./ownerId");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
        snprintf(instance->accountId, OWNER_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./macAddress");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
        mac2hex(results[i], instance->macAddress);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./publicIp");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
        instance->publicIp = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./privateIp");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
        instance->privateIp = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./vpc");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
        snprintf(instance->vpc, VPC_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./subnet");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
        snprintf(instance->subnet, VPC_SUBNET_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./securityGroups/value");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    instance->secgroup_names = EUCA_ZALLOC_C(max_results, sizeof (gni_name_32));
    instance->gnisgs = EUCA_ZALLOC_C(max_results, sizeof (gni_secgroup *));
    for (i = 0; i < max_results; i++) {
        LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
        snprintf(instance->secgroup_names[i].name, 32, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    instance->max_secgroup_names = max_results;
    EUCA_FREE(results);

    snprintf(expression, 2048, "./attachmentId");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
        snprintf(instance->attachmentId, ENI_ATTACHMENT_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    if (!is_instance) {
        snprintf(expression, 2048, "./sourceDestCheck");
        rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
            euca_strtolower(results[i]);
            if (!strcmp(results[i], "true")) {
                instance->srcdstcheck = TRUE;
            } else {
                instance->srcdstcheck = FALSE;
            }
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "./deviceIndex");
        rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("\tafter function: %d: %s\n", i, results[i]);
            instance->deviceidx = atoi(results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);
        // Use the instance name for primary interfaces
        snprintf(instance->ifname, INTERFACE_ID_LEN, "%s", instance->name);
        if (instance->deviceidx == 0) {
            snprintf(instance->name, INTERFACE_ID_LEN, "%s", instance->instance_name.name);
        }
    }
    return (0);
}

/**
 * Populates globalNetworkInfo security groups structure from the content of an XML
 * file (xmlXPathContext is expected). The security groups section of globalNetworkInfo
 * structure is expected to be empty/clean.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param xmlnode [in] pointer to the "securityGroups" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_sgs(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i, j, k, l;

    if ((gni == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
    }
    if ((gni->init == FALSE) || (gni->max_secgroups != 0)) {
        LOGERROR("Invalid argument: gni is not initialized or sgs section is not empty.\n");
        return (1);
    }

    xmlNodeSet nodeset = {0};
    snprintf(expression, 2048, "./securityGroup");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        gni->secgroups = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_secgroup));
        gni->max_secgroups = nodeset.nodeNr;
    }
    LOGTRACE("Found %d security groups\n", gni->max_secgroups);
    for (j = 0; j < gni->max_secgroups; j++) {
        if (nodeset.nodeTab[j]) {
            xmlNodePtr sgnode = nodeset.nodeTab[j];
            gni_secgroup *gsg = &(gni->secgroups[j]);
            if (sgnode && sgnode->properties) {
                for (xmlAttr *prop = sgnode->properties; prop != NULL; prop = prop->next) {
                    if (!strcmp((char *) prop->name, "name")) {
                        snprintf(gsg->name, SECURITY_GROUP_ID_LEN, "%s", (char *) prop->children->content);
                        break;
                    }
                }
            }

            // populate secgroup's instance_names
            gni_instance *gi = NULL;
            gsg->max_instances = 0;
            for (k = 0; k < gni->max_instances; k++) {
                for (l = 0; l < gni->instances[k]->max_secgroup_names; l++) {
                    gi = gni->instances[k];
                    if (!strcmp(gi->secgroup_names[l].name, gsg->name)) {
                        gsg->instances = EUCA_REALLOC_C(gsg->instances, gsg->max_instances + 1, sizeof (gni_instance *));
                        gsg->instances[gsg->max_instances] = gi;
                        gsg->max_instances++;
                    }
                }
            }
            // populate secgroup's interface_names
            if (IS_NETMODE_VPCMIDO(gni)) {
                gsg->max_interfaces = 0;
                for (k = 0; k < gni->max_ifs; k++) {
                    gi = gni->ifs[k];
                    for (l = 0; l < gi->max_secgroup_names; l++) {
                        if (!strcmp(gi->secgroup_names[l].name, gsg->name)) {
                            gsg->interfaces = EUCA_REALLOC_C(gsg->interfaces, gsg->max_interfaces + 1, sizeof (gni_instance *));
                            gi->gnisgs[l] = gsg;
                            gsg->interfaces[gsg->max_interfaces] = gi;
                            gsg->max_interfaces++;
                        }
                    }
                }
            }

            snprintf(expression, 2048, "./ownerId");
            rc += evaluate_xpath_property(ctxptr, doc, sgnode, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->secgroups[j].accountId, OWNER_ID_LEN, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            // ingress rules
            xmlNodeSet ingressNodeset = {0};
            snprintf(expression, 2048, "./ingressRules/rule");
            rc += evaluate_xpath_nodeset(ctxptr, doc, sgnode, expression, &ingressNodeset);
            if (ingressNodeset.nodeNr > 0) {
                gni->secgroups[j].ingress_rules = EUCA_ZALLOC_C(ingressNodeset.nodeNr, sizeof (gni_rule));
                gni->secgroups[j].max_ingress_rules = ingressNodeset.nodeNr;
            }
            LOGTRACE("\tFound %d ingress rules\n", gni->secgroups[j].max_ingress_rules);
            for (k = 0; k < ingressNodeset.nodeNr; k++) {
                if (ingressNodeset.nodeTab[k]) {
                    gni_populate_rule(&(gni->secgroups[j].ingress_rules[k]),
                            ingressNodeset.nodeTab[k], ctxptr, doc);
                }
            }
            EUCA_FREE(ingressNodeset.nodeTab);

            // egress rules
            xmlNodeSet egressNodeset = {0};
            snprintf(expression, 2048, "./egressRules/rule");
            rc += evaluate_xpath_nodeset(ctxptr, doc, sgnode, expression, &egressNodeset);
            if (egressNodeset.nodeNr > 0) {
                gni->secgroups[j].egress_rules = EUCA_ZALLOC_C(egressNodeset.nodeNr, sizeof (gni_rule));
                gni->secgroups[j].max_egress_rules = egressNodeset.nodeNr;
            }
            LOGTRACE("\tFound %d egress rules\n", gni->secgroups[j].max_egress_rules);
            for (k = 0; k < egressNodeset.nodeNr; k++) {
                if (egressNodeset.nodeTab[k]) {
                    gni_populate_rule(&(gni->secgroups[j].egress_rules[k]),
                            egressNodeset.nodeTab[k], ctxptr, doc);
                }
            }
            EUCA_FREE(egressNodeset.nodeTab);
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    return (0);
}

/**
 * Populates globalNetworkInfo security group rule structure from the content of an XML
 * file (xmlXPathContext is expected). The target rule structure is assumed
 * to be clean.
 *
 * @param rule [in] a pointer to the global network information rule structure
 * @param xmlnode [in] pointer to the "rule" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_rule(gni_rule *rule, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i;

    if ((rule == NULL) || (xmlnode == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: rule or ctxptr is NULL.\n");
        return (1);
    }

    snprintf(expression, 2048, "./protocol");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        rule->protocol = atoi(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./groupId");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(rule->groupId, SECURITY_GROUP_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./groupOwnerId");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(rule->groupOwnerId, OWNER_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./cidr");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        char *scidrnetaddr = NULL;
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(rule->cidr, NETWORK_ADDR_LEN, "%s", results[i]);
        cidrsplit(rule->cidr, &scidrnetaddr, &(rule->cidrSlashnet));
        rule->cidrNetaddr = dot2hex(scidrnetaddr);
        EUCA_FREE(scidrnetaddr);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./fromPort");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        rule->fromPort = atoi(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./toPort");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        rule->toPort = atoi(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./icmpType");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        rule->icmpType = atoi(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./icmpCode");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        rule->icmpCode = atoi(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    return (0);
}

/**
 * Populates globalNetworkInfo vpcs structure from the content of an XML
 * file (xmlXPathContext is expected). The vps section of globalNetworkInfo
 * structure is expected to be empty/clean.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param xmlnode [in] pointer to the "vpcs" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_vpcs(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    int j;

    if ((gni == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
    }
    if ((gni->init == FALSE) || (gni->max_vpcs != 0)) {
        LOGERROR("Invalid argument: gni is not initialized or vpcs section is not empty.\n");
        return (1);
    }

    xmlNodeSet nodeset = {0};
    snprintf(expression, 2048, "./vpc");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        gni->vpcs = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_vpc));
        gni->max_vpcs = nodeset.nodeNr;
    }
    LOGTRACE("Found %d vpcs\n", gni->max_vpcs);
    for (j = 0; j < gni->max_vpcs; j++) {
        if (nodeset.nodeTab[j]) {
            xmlNodePtr vpcnode = nodeset.nodeTab[j];
            gni_vpc *gvpc = &(gni->vpcs[j]);
            if (vpcnode && vpcnode->properties) {
                for (xmlAttr *prop = vpcnode->properties; prop != NULL; prop = prop->next) {
                    if (!strcmp((char *) prop->name, "name")) {
                        snprintf(gvpc->name, VPC_ID_LEN, "%s", (char *) prop->children->content);
                        break;
                    }
                }
                gni_populate_vpc(gvpc, vpcnode, ctxptr, doc);
            }
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    return (0);
}

/**
 * Populates globalNetworkInfo VPC structure from the content of an XML
 * file (xmlXPathContext is expected). The target vpc structure is assumed
 * to be clean.
 *
 * @param vpc [in] a pointer to the global network information vpc structure
 * @param xmlnode [in] pointer to the "vpc" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_vpc(gni_vpc *vpc, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0;

    if ((vpc == NULL) || (xmlnode == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: vpc or ctxptr is NULL.\n");
        return (1);
    }

    snprintf(expression, 2048, "./ownerId");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (int i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(vpc->accountId, OWNER_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./cidr");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (int i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(vpc->cidr, NETWORK_ADDR_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./dhcpOptionSet");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (int i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(vpc->dhcpOptionSet_name, DHCP_OS_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    xmlNodeSet nodeset;
    bzero(&nodeset, sizeof (xmlNodeSet));
    snprintf(expression, 2048, "./routeTables/routeTable");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        vpc->routeTables = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_route_table));
        vpc->max_routeTables = nodeset.nodeNr;
    }
    LOGTRACE("\tFound %d vpc route tables\n", vpc->max_routeTables);
    for (int j = 0; j < vpc->max_routeTables; j++) {
        if (nodeset.nodeTab[j]) {
            xmlNodePtr rtbnode = nodeset.nodeTab[j];
            gni_route_table *groutetb = &(vpc->routeTables[j]);
            if (rtbnode && rtbnode->properties) {
                for (xmlAttr *prop = rtbnode->properties; prop != NULL; prop = prop->next) {
                    if (!strcmp((char *) prop->name, "name")) {
                        snprintf(groutetb->name, RTB_ID_LEN, "%s", (char *) prop->children->content);
                        break;
                    }
                }
                gni_populate_routetable(vpc, groutetb, rtbnode, ctxptr, doc);
            }
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    bzero(&nodeset, sizeof (xmlNodeSet));
    snprintf(expression, 2048, "./subnets/subnet");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        vpc->subnets = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_vpcsubnet));
        vpc->max_subnets = nodeset.nodeNr;
    }
    LOGTRACE("\tFound %d vpc subnets\n", vpc->max_subnets);
    for (int j = 0; j < vpc->max_subnets; j++) {
        if (nodeset.nodeTab[j]) {
            xmlNodePtr vpcsnnode = nodeset.nodeTab[j];
            gni_vpcsubnet *gvpcsn = &(vpc->subnets[j]);
            if (vpcsnnode && vpcsnnode->properties) {
                for (xmlAttr *prop = vpcsnnode->properties; prop != NULL; prop = prop->next) {
                    if (!strcmp((char *) prop->name, "name")) {
                        snprintf(gvpcsn->name, VPC_SUBNET_ID_LEN, "%s", (char *) prop->children->content);
                        break;
                    }
                }
                gni_populate_vpcsubnet(vpc, gvpcsn, vpcsnnode, ctxptr, doc);
            }
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    snprintf(expression, 2048, "./internetGateways/value");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    vpc->internetGatewayNames = EUCA_ZALLOC_C(max_results, sizeof (gni_name_32));
    for (int i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(vpc->internetGatewayNames[i].name, 32, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    vpc->max_internetGatewayNames = max_results;
    EUCA_FREE(results);

    // NAT Gateways
    bzero(&nodeset, sizeof (xmlNodeSet));
    snprintf(expression, 2048, "./natGateways/natGateway");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        vpc->natGateways = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_nat_gateway));
        vpc->max_natGateways = nodeset.nodeNr;
    }
    LOGTRACE("\tFound %d vpc nat gateways\n", vpc->max_natGateways);
    for (int j = 0; j < vpc->max_natGateways; j++) {
        if (nodeset.nodeTab[j]) {
            xmlNodePtr ngnode = nodeset.nodeTab[j];
            gni_nat_gateway *gninatg = &(vpc->natGateways[j]);
            if (ngnode && ngnode->properties) {
                for (xmlAttr *prop = ngnode->properties; prop != NULL; prop = prop->next) {
                    if (!strcmp((char *) prop->name, "name")) {
                        snprintf(gninatg->name, NATG_ID_LEN, "%s", (char *) prop->children->content);
                        break;
                    }
                }
                gni_populate_natgateway(gninatg, ngnode, ctxptr, doc);
            }
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    // Network ACLs
    bzero(&nodeset, sizeof (xmlNodeSet));
    snprintf(expression, 2048, "./networkAcls/networkAcl");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        vpc->networkAcls = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_network_acl));
        vpc->max_networkAcls = nodeset.nodeNr;
    }
    LOGTRACE("\tFound %d vpc network acls\n", vpc->max_networkAcls);
    for (int j = 0; j < vpc->max_networkAcls; j++) {
        if (nodeset.nodeTab[j]) {
            xmlNodePtr aclnode = nodeset.nodeTab[j];
            gni_network_acl *gniacl = &(vpc->networkAcls[j]);
            if (aclnode && aclnode->properties) {
                for (xmlAttr *prop = aclnode->properties; prop != NULL; prop = prop->next) {
                    if (!strcmp((char *) prop->name, "name")) {
                        snprintf(gniacl->name, NETWORK_ACL_ID_LEN, "%s", (char *) prop->children->content);
                        break;
                    }
                }
                gni_populate_networkacl(gniacl, aclnode, ctxptr, doc);
            }
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    return (0);
}

/**
 * Populates globalNetworkInfo VPC route table structure from the content of an XML
 * file (xmlXPathContext is expected). The target route_table structure is assumed
 * to be clean.
 *
 * @param vpc [in] a pointer to the global network information vpc structure
 * @param routetable [in] a pointer to the global network information route_table structure
 * @param xmlnode [in] pointer to the "routeTable" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_routetable(gni_vpc *vpc, gni_route_table *routetable, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i;

    if ((routetable == NULL) || (xmlnode == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: route table or ctxptr is NULL.\n");
        return (1);
    }

    snprintf(expression, 2048, "./ownerId");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(routetable->accountId, OWNER_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    xmlNodeSet nodeset;
    bzero(&nodeset, sizeof (xmlNodeSet));
    snprintf(expression, 2048, "./routes/route");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        routetable->entries = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_route_entry));
        routetable->max_entries = nodeset.nodeNr;
    }
    LOGTRACE("\t\tFound %d vpc route table entries\n", routetable->max_entries);
    for (int j = 0; j < routetable->max_entries; j++) {
        if (nodeset.nodeTab[j]) {
            xmlNodePtr routenode = nodeset.nodeTab[j];
            gni_route_entry *gre = &(routetable->entries[j]);
            gni_populate_route(gre, routenode, ctxptr, doc);
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    return (0);
}

/**
 * Populates globalNetworkInfo route table route_entry structure from the content of an XML
 * file (xmlXPathContext is expected). The target route_entry structure is assumed
 * to be clean.
 *
 * @param route [in] a pointer to the global network information route_entry structure
 * @param xmlnode [in] pointer to the "rule" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_route(gni_route_entry *route, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i;

    if ((route == NULL) || (xmlnode == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: route or ctxptr is NULL.\n");
        return (1);
    }

    snprintf(expression, 2048, "./destinationCidr");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(route->destCidr, NETWORK_ADDR_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);
    snprintf(expression, 2048, "./gatewayId");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(route->target, LID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);
    if (max_results == 0) {
        // Check if the target is a network interface
        snprintf(expression, 2048, "./networkInterfaceId");
        rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(route->target, LID_LEN, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);
    }
    if (max_results == 0) {
        // Check if the target is a nat gateway
        snprintf(expression, 2048, "./natGatewayId");
        rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(route->target, LID_LEN, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);
    }

    return (0);
}

/**
 * Populates globalNetworkInfo VPC subnet structure from the content of an XML
 * file (xmlXPathContext is expected). The target vpcsubnet structure is assumed
 * to be clean.
 *
 * @param vpc [in] a pointer to the global network information vpc structure
 * @param vpcsubnet [in] a pointer to the global network information vpcsubnet structure
 * @param xmlnode [in] pointer to the "vpcsubnet" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_vpcsubnet(gni_vpc *vpc, gni_vpcsubnet *vpcsubnet, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i;

    if ((vpcsubnet == NULL) || (xmlnode == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: subnet or ctxptr is NULL.\n");
        return (1);
    }

    snprintf(expression, 2048, "./ownerId");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(vpcsubnet->accountId, OWNER_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./cidr");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(vpcsubnet->cidr, NETWORK_ADDR_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./cluster");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(vpcsubnet->cluster_name, HOSTNAME_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./networkAcl");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(vpcsubnet->networkAcl_name, NETWORK_ACL_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./routeTable");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(vpcsubnet->routeTable_name, RTB_ID_LEN, "%s", results[i]);
        vpcsubnet->routeTable = gni_vpc_get_routeTable(vpc, results[i]);
        if (vpcsubnet->routeTable == NULL) {
            LOGWARN("Failed to find GNI %s for %s\n", results[i], vpcsubnet->name)
        } else {
            vpcsubnet->rt_entry_applied = EUCA_ZALLOC_C(vpcsubnet->routeTable->max_entries, sizeof (int));
        }
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    return (0);
}

/**
 * Populates globalNetworkInfo VPC NAT gateway structure from the content of an XML
 * file (xmlXPathContext is expected). The target nat_gateway structure is assumed
 * to be clean.
 *
 * @param natg [in] a pointer to the global network information nat_gateway structure
 * @param xmlnode [in] pointer to the "vpcsubnet" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_natgateway(gni_nat_gateway *natg, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i;

    if ((natg == NULL) || (xmlnode == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: subnet or ctxptr is NULL.\n");
        return (1);
    }

    snprintf(expression, 2048, "./ownerId");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(natg->accountId, OWNER_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./macAddress");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        mac2hex(results[i], natg->macAddress);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./publicIp");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        natg->publicIp = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./privateIp");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        natg->privateIp = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./vpc");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(natg->vpc, VPC_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./subnet");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(natg->subnet, VPC_SUBNET_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    return (0);
}

/**
 * Populates globalNetworkInfo VPC network acl structure from the content of an XML
 * file (xmlXPathContext is expected). The target networkAcl structure is assumed
 * to be clean.
 *
 * @param netacl [in] a pointer to the global network information route_table structure
 * @param xmlnode [in] pointer to the "networkAcl" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_networkacl(gni_network_acl *netacl, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i;

    if ((netacl == NULL) || (xmlnode == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: route table or ctxptr is NULL.\n");
        return (1);
    }

    snprintf(expression, 2048, "./ownerId");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(netacl->accountId, OWNER_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    xmlNodeSet nodeset;
    bzero(&nodeset, sizeof (xmlNodeSet));
    snprintf(expression, 2048, "./ingressEntries/entry");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        netacl->ingress = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_acl_entry));
        netacl->max_ingress = nodeset.nodeNr;
    }
    LOGTRACE("\t\tFound %d ingress entries\n", netacl->max_ingress);
    for (int j = 0; j < netacl->max_ingress; j++) {
        if (nodeset.nodeTab[j]) {
            xmlNodePtr aclnode = nodeset.nodeTab[j];
            gni_acl_entry *gaclentry = &(netacl->ingress[j]);
            gni_populate_aclentry(gaclentry, aclnode, ctxptr, doc);
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    bzero(&nodeset, sizeof (xmlNodeSet));
    snprintf(expression, 2048, "./egressEntries/entry");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        netacl->egress = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_acl_entry));
        netacl->max_egress = nodeset.nodeNr;
    }
    LOGTRACE("\t\tFound %d egress entries\n", netacl->max_egress);
    for (int j = 0; j < netacl->max_egress; j++) {
        if (nodeset.nodeTab[j]) {
            xmlNodePtr aclnode = nodeset.nodeTab[j];
            gni_acl_entry *gaclentry = &(netacl->egress[j]);
            gni_populate_aclentry(gaclentry, aclnode, ctxptr, doc);
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    return (0);
}

/**
 * Populates globalNetworkInfo network acl entry structure from the content of an XML
 * file (xmlXPathContext is expected). The target acl_entry structure is assumed
 * to be clean.
 *
 * @param aclentry [in] a pointer to the global network information acl_entry structure
 * @param xmlnode [in] pointer to the "entry" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_aclentry(gni_acl_entry *aclentry, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i;

    if ((aclentry == NULL) || (xmlnode == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: aclentry or ctxptr is NULL.\n");
        return (1);
    }

    if (xmlnode && xmlnode->properties) {
        for (xmlAttr *prop = xmlnode->properties; prop != NULL; prop = prop->next) {
            if (!strcmp((char *) prop->name, "number")) {
                aclentry->number = atoi((char *) prop->children->content);
                break;
            }
        }
    }

    snprintf(expression, 2048, "./action");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        if (!strcmp(results[i], "allow")) {
            aclentry->allow = 1;
        }
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./protocol");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        aclentry->protocol = atoi(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./cidr");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        char *scidrnetaddr = NULL;
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(aclentry->cidr, NETWORK_ADDR_LEN, "%s", results[i]);
        cidrsplit(aclentry->cidr, &scidrnetaddr, &(aclentry->cidrSlashnet));
        aclentry->cidrNetaddr = dot2hex(scidrnetaddr);
        EUCA_FREE(scidrnetaddr);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./portRangeFrom");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        aclentry->fromPort = atoi(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./portRangeTo");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        aclentry->toPort = atoi(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./icmpType");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        aclentry->icmpType = atoi(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "./icmpCode");
    rc += evaluate_xpath_property(ctxptr, doc, xmlnode, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        aclentry->icmpCode = atoi(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    return (0);
}

/**
 * Populates globalNetworkInfo internet gateway structure from the content of an XML
 * file (xmlXPathContext is expected). The internet_gateway section of globalNetworkInfo
 * structure is expected to be empty/clean.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param xmlnode [in] pointer to the "internetGateways" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_internetgateways(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i, j;

    if ((gni == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
    }
    if ((gni->init == FALSE) || (gni->max_vpcIgws != 0)) {
        LOGERROR("Invalid argument: gni is not initialized or internet gateways section is not empty.\n");
        return (1);
    }

    xmlNodeSet nodeset = {0};
    snprintf(expression, 2048, "./internetGateway");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        gni->vpcIgws = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_internet_gateway));
        gni->max_vpcIgws = nodeset.nodeNr;
    }
    LOGTRACE("Found %d Internet Gateways\n", gni->max_vpcIgws);
    for (j = 0; j < gni->max_vpcIgws; j++) {
        if (nodeset.nodeTab[j]) {
            xmlNodePtr ignode = nodeset.nodeTab[j];
            gni_internet_gateway *gig = &(gni->vpcIgws[j]);
            if (ignode && ignode->properties) {
                for (xmlAttr *prop = ignode->properties; prop != NULL; prop = prop->next) {
                    if (!strcmp((char *) prop->name, "name")) {
                        snprintf(gig->name, INETG_ID_LEN, "%s", (char *) prop->children->content);
                        break;
                    }
                }
            }

            snprintf(expression, 2048, "./ownerId");
            rc += evaluate_xpath_property(ctxptr, doc, ignode, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gig->accountId, OWNER_ID_LEN, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);
        }
    }
    EUCA_FREE(nodeset.nodeTab);

    return (0);
}

/**
 * Populates globalNetworkInfo dhcp option set structure from the content of an XML
 * file (xmlXPathContext is expected). The dhcp_os section of globalNetworkInfo
 * structure is expected to be empty/clean.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param xmlnode [in] pointer to the "dhcpOptionSets" xmlNode
 * @param ctxptr [in] pointer to the xmlXPathContext
 * @param doc [in] xml document to be used to populate
 *
 * @return 0 on success or 1 on failure
 */
int gni_populate_dhcpos(globalNetworkInfo *gni, xmlNodePtr xmlnode, xmlXPathContextPtr ctxptr, xmlDocPtr doc) {
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i, j;

    if ((gni == NULL) || (ctxptr == NULL) || (doc == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
    }
    if ((gni->init == FALSE) || (gni->max_dhcpos != 0)) {
        LOGERROR("Invalid argument: gni is not initialized or DHCP option sets section is not empty.\n");
        return (1);
    }

    xmlNodeSet nodeset = {0};
    snprintf(expression, 2048, "./dhcpOptionSet");
    rc += evaluate_xpath_nodeset(ctxptr, doc, xmlnode, expression, &nodeset);
    if (nodeset.nodeNr > 0) {
        gni->dhcpos = EUCA_ZALLOC_C(nodeset.nodeNr, sizeof (gni_dhcp_os));
        gni->max_dhcpos = nodeset.nodeNr;
    }
    LOGTRACE("Found %d DHCP Option Sets\n", gni->max_dhcpos);
    for (j = 0; j < gni->max_dhcpos; j++) {
        if (nodeset.nodeTab[j]) {
            xmlNodePtr dhnode = nodeset.nodeTab[j];
            gni_dhcp_os *gdh = &(gni->dhcpos[j]);
            if (dhnode && dhnode->properties && dhnode->properties->children &&
                    dhnode->properties->children->content) {
                for (xmlAttr *prop = dhnode->properties; prop != NULL; prop = prop->next) {
                    if (!strcmp((char *) prop->name, "name")) {
                        snprintf(gdh->name, DHCP_OS_ID_LEN, "%s", (char *) prop->children->content);
                        break;
                    }
                }
            }

            snprintf(expression, 2048, "./ownerId");
            rc += evaluate_xpath_property(ctxptr, doc, dhnode, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gdh->accountId, OWNER_ID_LEN, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "./property[@name='domain-name']/value");
            rc += evaluate_xpath_property(ctxptr, doc, dhnode, expression, &results, &max_results);
            gdh->domains = EUCA_ZALLOC_C(max_results, sizeof (gni_name_256));
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gdh->domains[i].name, 256, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            gdh->max_domains = max_results;
            EUCA_FREE(results);

            snprintf(expression, 2048, "./property[@name='domain-name-servers']/value");
            rc += evaluate_xpath_property(ctxptr, doc, dhnode, expression, &results, &max_results);
            gdh->dns = EUCA_ZALLOC_C(max_results, sizeof (u32));
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gdh->dns[i] = dot2hex(results[i]);
                EUCA_FREE(results[i]);
            }
            gdh->max_dns = max_results;
            EUCA_FREE(results);

            snprintf(expression, 2048, "./property[@name='ntp-servers']/value");
            rc += evaluate_xpath_property(ctxptr, doc, dhnode, expression, &results, &max_results);
            gdh->ntp = EUCA_ZALLOC_C(max_results, sizeof (u32));
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gdh->ntp[i] = dot2hex(results[i]);
                EUCA_FREE(results[i]);
            }
            gdh->max_ntp = max_results;
            EUCA_FREE(results);

            snprintf(expression, 2048, "./property[@name='netbios-name-servers']/value");
            rc += evaluate_xpath_property(ctxptr, doc, dhnode, expression, &results, &max_results);
            gdh->netbios_ns = EUCA_ZALLOC_C(max_results, sizeof (u32));
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gdh->netbios_ns[i] = dot2hex(results[i]);
                EUCA_FREE(results[i]);
            }
            gdh->max_ntp = max_results;
            EUCA_FREE(results);

            snprintf(expression, 2048, "./property[@name='netbios-node-type']/value");
            rc += evaluate_xpath_property(ctxptr, doc, dhnode, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gdh->netbios_type = atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

        }
    }
    EUCA_FREE(nodeset.nodeTab);

    return (0);
}

/**
 * Parses a list of IP address ranges (start - end) and converts into a linear
 * array.
 * @param inlist [in] list of IP address ranges of interest
 * @param inmax [in] number of entries in the list
 * @param outlist [out] list of IP addresses converted from the input list
 * @param outmax [out] number of entries in the list of IP addresses
 * @return  0 on success. 1 on failure.
 */
int gni_serialize_iprange_list(char **inlist, int inmax, u32 **outlist, int *outmax) {
    int i = 0;
    int ret = 0;
    int outidx = 0;
    u32 *outlistbuf = NULL;
    int max_outlistbuf = 0;

    if (!inlist || inmax < 0 || !outlist || !outmax) {
        LOGERROR("invalid input\n");
        return (1);
    }
    *outlist = NULL;
    *outmax = 0;

    for (i = 0; i < inmax; i++) {
        char *range = NULL;
        char *tok = NULL;
        char *start = NULL;
        char *end = NULL;
        int numi = 0;

        LOGTRACE("parsing input range: %s\n", inlist[i]);

        range = strdup(inlist[i]);
        tok = strchr(range, '-');
        if (tok) {
            *tok = '\0';
            tok++;
            if (strlen(tok)) {
                start = strdup(range);
                end = strdup(tok);
            } else {
                LOGERROR("empty end range from input '%s': check network config\n", inlist[i]);
                start = NULL;
                end = NULL;
            }
        } else {
            start = strdup(range);
            end = strdup(range);
        }
        EUCA_FREE(range);

        if (start && end) {
            uint32_t startb, endb, idxb, localhost;

            LOGTRACE("start=%s end=%s\n", start, end);
            localhost = dot2hex("127.0.0.1");
            startb = dot2hex(start);
            endb = dot2hex(end);
            if ((startb <= endb) && (startb != localhost) && (endb != localhost)) {
                numi = (int) (endb - startb) + 1;
                outlistbuf = EUCA_REALLOC_C(outlistbuf, (max_outlistbuf + numi), sizeof (u32));
                outidx = max_outlistbuf;
                max_outlistbuf += numi;
                for (idxb = startb; idxb <= endb; idxb++) {
                    outlistbuf[outidx] = idxb;
                    outidx++;
                }
            } else {
                LOGERROR("end range '%s' is smaller than start range '%s' from input '%s': check network config\n", end, start, inlist[i]);
                ret = 1;
            }
        } else {
            LOGERROR("couldn't parse range from input '%s': check network config\n", inlist[i]);
            ret = 1;
        }

        EUCA_FREE(start);
        EUCA_FREE(end);
    }

    if (max_outlistbuf > 0) {
        *outmax = max_outlistbuf;
        *outlist = EUCA_ZALLOC_C(*outmax, sizeof (u32));
        memcpy(*outlist, outlistbuf, sizeof (u32) * max_outlistbuf);
    }
    EUCA_FREE(outlistbuf);

    return (ret);
}

/**
 * Iterates through a given globalNetworkInfo structure and execute the
 * given operation mode.
 *
 * @param gni [in] a pointer to the global network information structure
 * @param mode [in] the iteration mode: GNI_ITERATE_PRINT or GNI_ITERATE_FREE
 * @param llevel [in] log level to be used in mode GNI_ITERATE_PRINT
 *
 * @return Always return 0
 */
int gni_iterate(globalNetworkInfo *gni, gni_iterate_mode mode, log_level_e llevel) {
    int i, j;
    char *strptra = NULL;

    switch (mode) {
        case GNI_ITERATE_PRINT:

            strptra = hex2dot(gni->enabledCLCIp);
            EUCALOG(llevel, "enabledCLCIp: %s\n", SP(strptra));
            EUCA_FREE(strptra);

            EUCALOG(llevel, "instanceDNSDomain: %s\n", gni->instanceDNSDomain);

            EUCALOG(llevel, "instanceDNSServers: \n");
            for (i = 0; i < gni->max_instanceDNSServers; i++) {
                strptra = hex2dot(gni->instanceDNSServers[i]);
                EUCALOG(llevel, "\tdnsServer %d: %s\n", i, SP(strptra));
                EUCA_FREE(strptra);
            }

            EUCALOG(llevel, "publicIps: \n");
            for (i = 0; i < gni->max_public_ips_str; i++) {
                EUCALOG(llevel, "\tip %d: %s\n", i, gni->public_ips_str[i]);
            }

            EUCALOG(llevel, "subnets: \n");
            for (i = 0; i < gni->max_subnets; i++) {
                strptra = hex2dot(gni->subnets[i].subnet);
                EUCALOG(llevel, "\tsubnet %d: %s\n", i, SP(strptra));
                EUCA_FREE(strptra);

                strptra = hex2dot(gni->subnets[i].netmask);
                EUCALOG(llevel, "\t\tnetmask: %s\n", SP(strptra));
                EUCA_FREE(strptra);

                strptra = hex2dot(gni->subnets[i].gateway);
                EUCALOG(llevel, "\t\tgateway: %s\n", SP(strptra));
                EUCA_FREE(strptra);
            }

            EUCALOG(llevel, "clusters: \n");
            for (i = 0; i < gni->max_clusters; i++) {
                EUCALOG(llevel, "\tcluster %d: %s\n", i, gni->clusters[i].name);
                strptra = hex2dot(gni->clusters[i].enabledCCIp);
                EUCALOG(llevel, "\t\tenabledCCIp: %s\n", SP(strptra));
                EUCA_FREE(strptra);

                EUCALOG(llevel, "\t\tmacPrefix: %s\n", gni->clusters[i].macPrefix);

                strptra = hex2dot(gni->clusters[i].private_subnet.subnet);
                EUCALOG(llevel, "\t\tsubnet: %s\n", SP(strptra));
                EUCA_FREE(strptra);

                strptra = hex2dot(gni->clusters[i].private_subnet.netmask);
                EUCALOG(llevel, "\t\t\tnetmask: %s\n", SP(strptra));
                EUCA_FREE(strptra);

                strptra = hex2dot(gni->clusters[i].private_subnet.gateway);
                EUCALOG(llevel, "\t\t\tgateway: %s\n", SP(strptra));
                EUCA_FREE(strptra);

                EUCALOG(llevel, "\t\tprivate_ips \n");
                for (j = 0; j < gni->clusters[i].max_private_ips_str; j++) {
                    EUCALOG(llevel, "\t\t\tip %d: %s\n", j, gni->clusters[i].private_ips_str[j]);
                    EUCA_FREE(strptra);
                }

                EUCALOG(llevel, "\t\tnodes \n");
                for (j = 0; j < gni->clusters[i].max_nodes; j++) {
                    EUCALOG(llevel, "\t\t\tnode %d: %s\n", j, gni->clusters[i].nodes[j].name);
                }
            }

            EUCALOG(llevel, "mido gateways: \n");
            for (i = 0; i < gni->max_midogws; i++) {
                EUCALOG(llevel, "\t%d: %s\n", i, gni->midogws[i].host);
                EUCALOG(llevel, "\t\text CIDR: %s\n", gni->midogws[i].ext_cidr);
                EUCALOG(llevel, "\t\text DEV : %s\n", gni->midogws[i].ext_dev);
                EUCALOG(llevel, "\t\text IP  : %s\n", gni->midogws[i].ext_ip);
                EUCALOG(llevel, "\t\tpeer IP : %s\n", gni->midogws[i].peer_ip);
                EUCALOG(llevel, "\t\tASN     : %d\n", gni->midogws[i].asn);
                EUCALOG(llevel, "\t\tpeer ASN: %d\n", gni->midogws[i].peer_asn);
                for (j = 0; j < gni->midogws[i].max_ad_routes; j++) {
                    EUCALOG(llevel, "\t\t\t%s\n", gni->midogws[i].ad_routes[j]);
                }
            }

            EUCALOG(llevel, "instances: \n");
            for (i = 0; i < gni->max_instances; i++) {
                EUCALOG(llevel, "\tid: %s\n", gni->instances[i]->name);
            }

            EUCALOG(llevel, "interfaces: \n");
            for (i = 0; i < gni->max_ifs; i++) {
                EUCALOG(llevel, "\tid: %s\n", gni->ifs[i]->name);
            }

            EUCALOG(llevel, "secgroups: \n");
            for (i = 0; i < gni->max_secgroups; i++) {
                EUCALOG(llevel, "\tname: %s\n", gni->secgroups[i].name);
            }

            EUCALOG(llevel, "vpcs: \n");
            for (i = 0; i < gni->max_vpcs; i++) {
                EUCALOG(llevel, "\tname: %s\n", gni->vpcs[i].name);
                EUCALOG(llevel, "\taccountId: %s\n", gni->vpcs[i].accountId);
                EUCALOG(llevel, "\tsubnets: \n");
                for (j = 0; j < gni->vpcs[i].max_subnets; j++) {
                    EUCALOG(llevel, "\t\tname: %s\n", gni->vpcs[i].subnets[j].name);
                    EUCALOG(llevel, "\t\trouteTable: %s\n", gni->vpcs[i].subnets[j].routeTable_name);
                }
            }

            EUCALOG(llevel, "Internet Gateways: \n");
            for (i = 0; i < gni->max_vpcIgws; i++) {
                EUCALOG(llevel, "\tname: %s\n", gni->vpcIgws[i].name);
                EUCALOG(llevel, "\taccountId: %s\n", gni->vpcIgws[i].accountId);
            }

            EUCALOG(llevel, "DHCP Option Sets: \n");
            for (i = 0; i < gni->max_dhcpos; i++) {
                EUCALOG(llevel, "\tname: %s\n", gni->dhcpos[i].name);
                EUCALOG(llevel, "\taccountId: %s\n", gni->dhcpos[i].accountId);
                char *dhcpdstr = NULL;
                char dhcpsstr[1024];
                dhcpsstr[0] = '\0';
                for (j = 0; j < gni->dhcpos[i].max_domains; j++) {
                    strncat(dhcpsstr, gni->dhcpos[i].domains[j].name, 512);
                    strncat(dhcpsstr, " ", 512);
                }
                if (gni->dhcpos[i].max_domains) {
                    EUCALOG(llevel, "\t\tdomains: %s\n", dhcpsstr);
                }
                dhcpsstr[0] = '\0';
                for (j = 0; j < gni->dhcpos[i].max_dns; j++) {
                    dhcpdstr = hex2dot(gni->dhcpos[i].dns[j]);
                    strncat(dhcpsstr, dhcpdstr, 512);
                    strncat(dhcpsstr, ", ", 512);
                    EUCA_FREE(dhcpdstr);
                }
                if (gni->dhcpos[i].max_dns) {
                    if (strlen(dhcpsstr) > 2) {
                        dhcpsstr[strlen(dhcpsstr) - 2] = '\0';
                    }
                    EUCALOG(llevel, "\t\tdns: %s\n", dhcpsstr);
                }
                dhcpsstr[0] = '\0';
                for (j = 0; j < gni->dhcpos[i].max_ntp; j++) {
                    dhcpdstr = hex2dot(gni->dhcpos[i].ntp[j]);
                    strncat(dhcpsstr, dhcpdstr, 512);
                    strncat(dhcpsstr, ", ", 512);
                    EUCA_FREE(dhcpdstr);
                }
                if (gni->dhcpos[i].max_ntp) {
                    if (strlen(dhcpsstr) > 2) {
                        dhcpsstr[strlen(dhcpsstr) - 2] = '\0';
                    }
                    EUCALOG(llevel, "\t\tntp: %s\n", dhcpsstr);
                }
                for (j = 0; j < gni->dhcpos[i].max_netbios_ns; j++) {
                    dhcpdstr = hex2dot(gni->dhcpos[i].netbios_ns[j]);
                    strncat(dhcpsstr, dhcpdstr, 512);
                    strncat(dhcpsstr, ", ", 512);
                    EUCA_FREE(dhcpdstr);
                }
                if (gni->dhcpos[i].max_netbios_ns) {
                    if (strlen(dhcpsstr) > 2) {
                        dhcpsstr[strlen(dhcpsstr) - 2] = '\0';
                    }
                    EUCALOG(llevel, "\t\tnetbios_ns: %s\n", dhcpsstr);
                }
                if (gni->dhcpos[i].netbios_type) {
                    EUCALOG(llevel, "\t\tnetbios_type: %d\n", gni->dhcpos[i].netbios_type);
                }
            }
            break;
        case GNI_ITERATE_FREE:
            EUCA_FREE(gni->instanceDNSServers);

            for (i = 0; i < gni->max_midogws; i++) {
                gni_midogw_clear(&(gni->midogws[i]));
            }
            EUCA_FREE(gni->midogws);

            EUCA_FREE(gni->public_ips);
            for (i = 0; i < gni->max_public_ips_str; i++) {
                EUCA_FREE(gni->public_ips_str[i]);
            }
            EUCA_FREE(gni->public_ips_str);

            EUCA_FREE(gni->subnets);

            for (i = 0; i < gni->max_clusters; i++) {
                for (j = 0; j < gni->clusters[i].max_nodes; j++) {
                    gni_node_clear(&(gni->clusters[i].nodes[j]));
                }
                gni_cluster_clear(&(gni->clusters[i]));
            }
            EUCA_FREE(gni->clusters);

            for (i = 0; i < gni->max_instances; i++) {
                gni_instance_clear(gni->instances[i]);
                EUCA_FREE(gni->instances[i]);
            }
            EUCA_FREE(gni->instances);

            for (i = 0; i < gni->max_ifs; i++) {
                gni_instance_clear(gni->ifs[i]);
                EUCA_FREE(gni->ifs[i]);
            }
            EUCA_FREE(gni->ifs);

            for (i = 0; i < gni->max_secgroups; i++) {
                gni_secgroup_clear(&(gni->secgroups[i]));
            }
            EUCA_FREE(gni->secgroups);

            for (i = 0; i < gni->max_vpcs; i++) {
                gni_vpc_clear(&(gni->vpcs[i]));
            }
            EUCA_FREE(gni->vpcs);

            EUCA_FREE(gni->vpcIgws);

            for (i = 0; i < gni->max_dhcpos; i++) {
                gni_dhcpos_clear(&(gni->dhcpos[i]));
            }
            EUCA_FREE(gni->dhcpos);

            gni->init = 1;
            gni->networkInfo[0] = '\0';
            // version_addr statements below are equivalent. Using second one to avoid Coverity alert
            //char *version_addr = (char *) &(gni->version);
            char *version_addr = (char *) gni + (sizeof (gni->init) + sizeof (gni->networkInfo));
            memset(version_addr, 0, sizeof (globalNetworkInfo) - sizeof (gni->init) - sizeof (gni->networkInfo));

            break;
        default:
            LOGWARN("Invalid argument: gni_iterate() invalid mode %d\n", mode);
    }
    
    return (0);
}

/**
 * Clears a given globalNetworkInfo structure. This will free member's allocated memory and zero
 * out the structure itself.
 *
 * @param gni [in] a pointer to the global network information structure
 *
 * @return the result of the gni_iterate() call
 */
int gni_clear(globalNetworkInfo *gni) {
    return (gni_iterate(gni, GNI_ITERATE_FREE, log_level_get()));
}

/**
 * Logs the content of a given globalNetworkInfo structure
 *
 * @param gni [in] a pointer to the global network information structure
 * @param llevel [in] log level to log
 *
 * @return the result of the gni_iterate() call
 */
int gni_print(globalNetworkInfo *gni, log_level_e llevel) {
    return (gni_iterate(gni, GNI_ITERATE_PRINT, llevel));
}

/**
 * Clears and free a given globalNetworkInfo structure.
 *
 * @param gni [in] a pointer to the global network information structure
 *
 * @return Always return 0
 *
 * @see gni_clear()
 *
 * @note The caller should free the given pointer
 */
int gni_free(globalNetworkInfo *gni) {
    if (!gni) {
        return (0);
    }
    gni_clear(gni);
    EUCA_FREE(gni);
    return (0);
}

/**
 * Creates an iptables rule using the source CIDR specified in the argument, and
 * based on the ingress rule entry in the argument.
 *
 * @param scidr [in] a string containing a CIDR to be used in the output iptables rule to match the source (can be a single IP address).
 * If null, the source address within the ingress rule will be used.
 * @param ingress_rule [in] gni_rule structure containing an ingress rule.
 * @param flags [in] integer containing extra conditions that will be added to the output iptables rule.
 * If 0, no condition is added. If 1 the output iptables rule will allow traffic between VMs on the same NC (see EUCA-11083).
 * @param outrule [out] a string containing the converted rule. A buffer with at least 1024 chars is expected.
 *
 * @return 0 on success or 1 on failure.
 *
 * @pre ingress_rule and outrule pointers MUST not be NULL
 *
 * @post \li upon success the outrule contains the converted iptables rule.
 *       \li upon failure, outrule does not contain any valid data
 */
int ingress_gni_to_iptables_rule(char *scidr, gni_rule *ingress_rule, char *outrule, int flags) {
#define MAX_RULE_LEN     1024
#define MAX_NEWRULE_LEN  2049
    char newrule[MAX_NEWRULE_LEN], buf[MAX_RULE_LEN];
    char *strptr = NULL;
    struct protoent *proto_info = NULL;

    if (!ingress_rule || !outrule) {
        LOGERROR("Invalid pointer(s) to ingress_gni_rule and/or iptables rule buffer.\n");
        return (1);
    }

    // Check for protocol all (-1) - should not happen in non-VPC
    if (-1 != ingress_rule->protocol) {
        proto_info = getprotobynumber(ingress_rule->protocol);
        if (proto_info == NULL) {
            LOGWARN("Invalid protocol (%d) - cannot create iptables rule.\n", ingress_rule->protocol);
            return (1);
        }
    } else {
        LOGWARN("Invalid protocol (%d) - cannot create iptables rule for EDGE.\n", ingress_rule->protocol);
        return (1);
    }

    newrule[0] = '\0';
    if (scidr) {
        strptr = scidr;
    } else {
        strptr = ingress_rule->cidr;
    }
    if (strptr && strlen(strptr)) {
        snprintf(buf, MAX_RULE_LEN, "-s %s ", strptr);
        strncat(newrule, buf, MAX_RULE_LEN);
    }
    switch (ingress_rule->protocol) {
        case 1: // ICMP
            snprintf(buf, MAX_RULE_LEN, "-p %s -m %s ", proto_info->p_name, proto_info->p_name);
            strncat(newrule, buf, MAX_RULE_LEN);
            if (ingress_rule->icmpType == -1) {
                snprintf(buf, MAX_RULE_LEN, "--icmp-type any ");
                strncat(newrule, buf, MAX_RULE_LEN);
            } else {
                snprintf(buf, MAX_RULE_LEN, "--icmp-type %d", ingress_rule->icmpType);
                strncat(newrule, buf, MAX_RULE_LEN);
                if (ingress_rule->icmpCode != -1) {
                    snprintf(buf, MAX_RULE_LEN, "/%d", ingress_rule->icmpCode);
                    strncat(newrule, buf, MAX_RULE_LEN);
                }
                snprintf(buf, MAX_RULE_LEN, " ");
                strncat(newrule, buf, MAX_RULE_LEN);
            }
            break;
        case 6: // TCP
        case 17: // UDP
            snprintf(buf, MAX_RULE_LEN, "-p %s -m %s ", proto_info->p_name, proto_info->p_name);
            strncat(newrule, buf, MAX_RULE_LEN);
            if (ingress_rule->fromPort) {
                snprintf(buf, MAX_RULE_LEN, "--dport %d", ingress_rule->fromPort);
                strncat(newrule, buf, MAX_RULE_LEN);
                if ((ingress_rule->toPort) && (ingress_rule->toPort > ingress_rule->fromPort)) {
                    snprintf(buf, MAX_RULE_LEN, ":%d", ingress_rule->toPort);
                    strncat(newrule, buf, MAX_RULE_LEN);
                }
                snprintf(buf, MAX_RULE_LEN, " ");
                strncat(newrule, buf, MAX_RULE_LEN);
            }
            break;
        default:
            // Protocols accepted by EC2 non-VPC are ICMP/TCP/UDP. Other protocols will default to numeric values on euca.
            snprintf(buf, MAX_RULE_LEN, "-p %d ", proto_info->p_proto);
            strncat(newrule, buf, MAX_RULE_LEN);
            break;
    }

    switch (flags) {
        case 0: // no condition
            break;
        case 1: // Add condition to the rule to accept the packet if it would be SNATed (EDGE).
            snprintf(buf, MAX_RULE_LEN, "-m mark ! --mark 0x2a ");
            strncat(newrule, buf, MAX_RULE_LEN);
            break;
        default:
            LOGINFO("Call with invalid flags: %d - ignored.\n", flags);
    }

    while (newrule[strlen(newrule) - 1] == ' ') {
        newrule[strlen(newrule) - 1] = '\0';
    }

    snprintf(outrule, MAX_RULE_LEN, "%s", newrule);
    LOGTRACE("IPTABLES RULE: %s\n", outrule);

    return 0;
}

/**
 * Clears a gni_cluster structure. This will free member's allocated memory and zero
 * out the structure itself.
 *
 * @param cluster [in] a pointer to the structure to clear
 *
 * @return This function always returns 0
 */
int gni_cluster_clear(gni_cluster *cluster)
{
    if (!cluster) {
        return (0);
    }

    for (int i = 0; i < cluster->max_private_ips_str; i++) {
        EUCA_FREE(cluster->private_ips_str[i]);
    }
    EUCA_FREE(cluster->private_ips_str);
    EUCA_FREE(cluster->private_ips);
    EUCA_FREE(cluster->nodes);

    bzero(cluster, sizeof (gni_cluster));

    return (0);
}

/**
 * Clears a gni_node structure. This will free member's allocated memory and zero
 * out the structure itself.
 *
 * @param node [in] a pointer to the structure to clear
 *
 * @return This function always returns 0
 */
int gni_node_clear(gni_node *node)
{
    if (!node) {
        return (0);
    }

    EUCA_FREE(node->instance_names);

    bzero(node, sizeof (gni_node));

    return (0);
}

/**
 * Clears a gni_instance structure. This will free member's allocated memory and zero
 * out the structure itself.
 *
 * @param instance [in] a pointer to the structure to clear
 *
 * @return This function always returns 0
 */
int gni_instance_clear(gni_instance *instance)
{
    if (!instance) {
        return (0);
    }

    EUCA_FREE(instance->secgroup_names);
    EUCA_FREE(instance->gnisgs);
    EUCA_FREE(instance->interfaces);

    bzero(instance, sizeof (gni_instance));

    return (0);
}

/**
 * Clears a gni_secgroup structure. This will free member's allocated memory and zero
 * out the structure itself.
 *
 * @param secgroup [in] a pointer to the structure to clear
 *
 * @return This function always returns 0
 */
int gni_secgroup_clear(gni_secgroup *secgroup) {
    if (!secgroup) {
        return (0);
    }

    EUCA_FREE(secgroup->ingress_rules);
    EUCA_FREE(secgroup->egress_rules);
    EUCA_FREE(secgroup->instances);
    EUCA_FREE(secgroup->interfaces);

    bzero(secgroup, sizeof (gni_secgroup));

    return (0);
}

/**
 * Clears a gni_vpc structure. This will free member's allocated memory and zero
 * out the structure itself.
 *
 * @param vpc [in] a pointer to the GNI VPC structure to reset
 *
 * @return Always return 0
 */
int gni_vpc_clear(gni_vpc *vpc) {
    int i = 0;
    if (!vpc) {
        return (0);
    }

    for (i = 0; i < vpc->max_subnets; i++) {
        EUCA_FREE(vpc->subnets[i].interfaces);
        EUCA_FREE(vpc->subnets[i].rt_entry_applied);
    }
    EUCA_FREE(vpc->subnets);
    for (i = 0; i < vpc->max_networkAcls; i++) {
        EUCA_FREE(vpc->networkAcls[i].ingress);
        EUCA_FREE(vpc->networkAcls[i].egress);
    }
    EUCA_FREE(vpc->networkAcls);
    for (i = 0; i < vpc->max_routeTables; i++) {
        EUCA_FREE(vpc->routeTables[i].entries);
    }
    EUCA_FREE(vpc->routeTables);
    EUCA_FREE(vpc->natGateways);
    EUCA_FREE(vpc->internetGatewayNames);
    EUCA_FREE(vpc->interfaces);

    memset(vpc, 0, sizeof (gni_vpc));

    return (0);
}

/**
 * Clears a gni_dhcp_ps structure. This will free member's allocated memory and zero
 * out the structure itself.
 * @param dhcpos [in] a pointer to the gni_dhcp_os to reset
 * @return Always return 0
 */
int gni_dhcpos_clear(gni_dhcp_os *dhcpos) {
    if (!dhcpos) {
        return (0);
    }

    EUCA_FREE(dhcpos->dns);
    EUCA_FREE(dhcpos->domains);
    EUCA_FREE(dhcpos->netbios_ns);
    EUCA_FREE(dhcpos->ntp);

    memset(dhcpos, 0, sizeof (gni_dhcp_os));

    return (0);
}

/**
 * Clears a gni_mido_gateway structure. This will free member's allocated memory and zero
 * out the structure itself.
 * @param midogw [in] a pointer to the gni_mido_gateway to reset
 * @return Always return 0
 */
int gni_midogw_clear(gni_mido_gateway *midogw) {
    if (!midogw) {
        return (0);
    }
    
    for (int i = 0; i < midogw->max_ad_routes; i++) {
        EUCA_FREE(midogw->ad_routes[i]);
    }
    EUCA_FREE(midogw->ad_routes);
    
    memset(midogw, 0, sizeof (gni_mido_gateway));
    return (0);
}

/**
 * Copies the contents of gni_mido_gateway structure src to dst.
 * @param dst [in] pointer to gni_mido_gateway structure where data from src will be copied
 * @param src [in] pointer to gni_mido_gateway structure with data to be copied
 * @return 0 on success. 1 on failure.
 */
int gni_midogw_dup(gni_mido_gateway *dst, gni_mido_gateway *src) {
    if (!src || !dst) {
        LOGWARN("Invalid argument: cannot copy to/from NULL gni_mido_gateway\n");
        return (1);
    }
    dst->asn = src->asn;
    dst->peer_asn = src->peer_asn;
    snprintf(dst->ext_cidr, NETWORK_ADDR_LEN, "%s", src->ext_cidr);
    snprintf(dst->ext_dev, IF_NAME_LEN, "%s", src->ext_dev);
    snprintf(dst->ext_ip, NETWORK_ADDR_LEN, "%s", src->ext_ip);
    snprintf(dst->peer_ip, NETWORK_ADDR_LEN, "%s", src->peer_ip);
    snprintf(dst->host, HOSTNAME_LEN, "%s", src->host);
    dst->ad_routes = EUCA_REALLOC_C(dst->ad_routes, src->max_ad_routes, sizeof (char *));
    memset(dst->ad_routes, 0, src->max_ad_routes * sizeof (char *));
    for (int i = 0; i < src->max_ad_routes; i++) {
        dst->ad_routes[i] = strdup(src->ad_routes[i]);
    }
    dst->max_ad_routes = src->max_ad_routes;
    return (0);
}

/**
 * Searches and returns the VPC that matches the name in the argument, if found.
 * @param gni [in] globalNetworkInfo structure that holds the network state to search.
 * @param name [in] name of the VPC of interest.
 * @param startidx [i/o] start index to the array of VPCs in gni. If a matching VPC
 * is found, startidx is updated to aid subsequent searches (ordering of objects in
 * GNI is assumed).
 * @return pointer to the gni_vpc of interest when found. NULL otherwise.
 */
gni_vpc *gni_get_vpc(globalNetworkInfo *gni, char *name, int *startidx) {
    gni_vpc *vpcs = NULL;
    int start = 0;

    if ((gni == NULL) || (name == NULL)) {
        return NULL;
    }
    if (startidx) {
        start = *startidx;
    }
    vpcs = gni->vpcs;
    for (int i = start; i < gni->max_vpcs; i++) {
        if (!strcmp(name, vpcs[i].name)) {
            if (startidx) {
                *startidx = i + 1;
            }
            return &(vpcs[i]);
        }
    }
    return (NULL);
}

/**
 * Searches and returns the VPC subnet that matches the name in the argument, if found.
 * @param vpc [in] gni_vpc that contains the subnet to search.
 * @param name [in] name of the VPC subnet of interest.
 * @param startidx [i/o] start index to the array of VPC subnets in gni. If a matching VPC
 * subnet is found, startidx is updated to aid subsequent searches (ordering of objects in
 * GNI is assumed).
 * @return pointer to the gni_vpcsubnet of interest when found. NULL otherwise.
 */
gni_vpcsubnet *gni_get_vpcsubnet(gni_vpc *vpc, char *name, int *startidx) {
    gni_vpcsubnet *vpcsubnets = NULL;
    int start = 0;

    if ((vpc == NULL) || (name == NULL)) {
        return NULL;
    }
    if (startidx) {
        start = *startidx;
    }
    vpcsubnets = vpc->subnets;
    for (int i = start; i < vpc->max_subnets; i++) {
        if (!strcmp(name, vpcsubnets[i].name)) {
            if (startidx) {
                *startidx = i + 1;
            }
            return &(vpcsubnets[i]);
        }
    }
    return (NULL);
}

/**
 * Searches and returns the VPC subnet interface that matches the name in the argument, if found.
 * @param vpcsubnet [in] gni_vpcsubnet structure that contains the interface to search.
 * @param name [in] name of the interface of interest.
 * @param startidx [i/o] start index to the array of VPC subnets interfaces in gni. If a matching VPC
 * subnet interface is found, startidx is updated to aid subsequent searches (ordering of objects in
 * GNI is assumed).
 * @return pointer to the gni_vpcsubnet of interest when found. NULL otherwise.
 */
gni_instance *gni_get_interface(gni_vpcsubnet *vpcsubnet, char *name, int *startidx) {
    gni_instance **interfaces = NULL;
    int start = 0;

    if ((vpcsubnet == NULL) || (name == NULL)) {
        return NULL;
    }
    if (startidx) {
        start = *startidx;
    }
    interfaces = vpcsubnet->interfaces;
    for (int i = start; i < vpcsubnet->max_interfaces; i++) {
        if (!strcmp(name, interfaces[i]->name)) {
            if (startidx) {
                *startidx = i + 1;
            }
            return (interfaces[i]);
        }
    }
    return (NULL);
}

/**
 * Searches and returns the VPC subnet interface that matches the name (short ID), if found.
 * This search matches both long and short IDs.
 * @param vpcsubnet [in] gni_vpcsubnet structure that contains the interface to search.
 * @param name [in] name of the interface of interest.
 * @param startidx [i/o] start index to the array of VPC subnets interfaces in gni. If a matching VPC
 * subnet interface is found, startidx is updated to aid subsequent searches (ordering of objects in
 * GNI is assumed).
 * @return pointer to the gni_vpcsubnet of interest when found. NULL otherwise.
 */
gni_instance *gni_get_interface_by_shortid(gni_vpcsubnet *vpcsubnet, char *name, int *startidx) {
    gni_instance **interfaces = NULL;
    int start = 0;

    if ((vpcsubnet == NULL) || (name == NULL)) {
        return NULL;
    }
    if (startidx) {
        start = *startidx;
    }
    interfaces = vpcsubnet->interfaces;
    for (int i = start; i < vpcsubnet->max_interfaces; i++) {
        if (strstr(interfaces[i]->name, name)) {
            if (startidx) {
                *startidx = i + 1;
            }
            return (interfaces[i]);
        }
    }
    return (NULL);
}

/**
 * Searches and returns the VPC natgateway that matches the name in the argument, if found.
 * @param vpc [in] gni_vpc that contains the natgateway to search.
 * @param name [in] name of the VPC natgateway of interest.
 * @param startidx [i/o] start index to the array of VPC natgateways in gni. If a matching VPC
 * natgateway is found, startidx is updated to aid subsequent searches (ordering of objects in
 * GNI is assumed).
 * @return pointer to the gni_vpcnatgateway of interest when found. NULL otherwise.
 */
gni_nat_gateway *gni_get_natgateway(gni_vpc *vpc, char *name, int *startidx) {
    gni_nat_gateway *vpcnatgateways = NULL;
    int start = 0;

    if ((vpc == NULL) || (name == NULL)) {
        return NULL;
    }
    if (startidx) {
        start = *startidx;
    }
    vpcnatgateways = vpc->natGateways;
    for (int i = start; i < vpc->max_natGateways; i++) {
        if (!strcmp(name, vpcnatgateways[i].name)) {
            if (startidx) {
                *startidx = i + 1;
            }
            return &(vpcnatgateways[i]);
        }
    }
    return (NULL);
}

/**
 * Searches and returns the VPC routetable that matches the name in the argument, if found.
 * @param vpc [in] gni_vpc that contains the routetable to search.
 * @param name [in] name of the VPC routetable of interest.
 * @param startidx [i/o] start index to the array of VPC routetables in gni. If a matching VPC
 * routetable is found, startidx is updated to aid subsequent searches (ordering of objects in
 * GNI is assumed).
 * @return pointer to the gni_vpcroutetable of interest when found. NULL otherwise.
 */
gni_route_table *gni_get_routetable(gni_vpc *vpc, char *name, int *startidx) {
    gni_route_table *vpcroutetables = NULL;
    int start = 0;

    if ((vpc == NULL) || (name == NULL)) {
        return NULL;
    }
    if (startidx) {
        start = *startidx;
    }
    vpcroutetables = vpc->routeTables;
    for (int i = start; i < vpc->max_routeTables; i++) {
        if (!strcmp(name, vpcroutetables[i].name)) {
            if (startidx) {
                *startidx = i + 1;
            }
            return &(vpcroutetables[i]);
        }
    }
    return (NULL);
}

/**
 * Searches and returns the security group that matches the name in the argument, if found.
 * @param gni [in] globalNetworkInfo structure that holds the network state to search.
 * @param name [in] name of the security group of interest.
 * @param startidx [i/o] start index to the array of SGs in gni. If a matching security group
 * is found, startidx is updated to aid subsequent searches (ordering of objects in
 * GNI is assumed).
 * @return pointer to the gni_secgroup of interest when found. NULL otherwise.
 */
gni_secgroup *gni_get_secgroup(globalNetworkInfo *gni, char *name, int *startidx) {
    gni_secgroup *secgroups = NULL;
    int start = 0;

    if ((gni == NULL) || (name == NULL)) {
        return NULL;
    }
    if (startidx) {
        start = *startidx;
    }
    secgroups = gni->secgroups;
    for (int i = start; i < gni->max_secgroups; i++) {
        if (!strcmp(name, secgroups[i].name)) {
            if (startidx) {
                *startidx = i + 1;
            }
            return &(secgroups[i]);
        }
    }
    return (NULL);
}

/**
 * Searches and returns the VPC networkacl that matches the name in the argument, if found.
 * @param vpc [in] gni_vpc that contains the networkacl to search.
 * @param name [in] name of the VPC networkacl of interest.
 * @param startidx [i/o] start index to the array of VPC networkacls in gni. If a matching VPC
 * networkacl is found, startidx is updated to aid subsequent searches (ordering of objects in
 * GNI is assumed).
 * @return pointer to the gni_network_acl of interest when found. NULL otherwise.
 */
gni_network_acl *gni_get_networkacl(gni_vpc *vpc, char *name, int *startidx) {
    gni_network_acl *netacls = NULL;
    int start = 0;

    if ((vpc == NULL) || (name == NULL)) {
        return NULL;
    }
    if (startidx) {
        start = *startidx;
    }
    netacls = vpc->networkAcls;
    for (int i = start; i < vpc->max_networkAcls; i++) {
        if (!strcmp(name, netacls[i].name)) {
            if (startidx) {
                *startidx = i + 1;
            }
            return &(netacls[i]);
        }
    }
    return (NULL);
}

/**
 * Searches and returns the DHCP Option Set that matches the name in the argument, if found.
 * @param gni [in] globalNetworkInfo structure that holds the network state to search.
 * @param name [in] name of the DHCP Option Set of interest.
 * @param startidx [i/o] start index to the array of DHCPOS in gni. If a matching dhcp_os
 * is found, startidx is updated to aid subsequent searches (ordering of objects in
 * GNI is assumed).
 * @return pointer to the gni_dhcp_os of interest when found. NULL otherwise.
 */
gni_dhcp_os *gni_get_dhcpos(globalNetworkInfo *gni, char *name, int *startidx) {
    gni_dhcp_os *dhcpos = NULL;
    int start = 0;

    if ((gni == NULL) || (name == NULL)) {
        return NULL;
    }
    if (startidx) {
        start = *startidx;
    }
    dhcpos = gni->dhcpos;
    for (int i = start; i < gni->max_dhcpos; i++) {
        if (!strcmp(name, dhcpos[i].name)) {
            if (startidx) {
                *startidx = i + 1;
            }
            return &(dhcpos[i]);
        }
    }
    return (NULL);
}

/**
 * Validates a given globalNetworkInfo structure and its content
 *
 * @param gni [in] a pointer to the Global Network Information structure to validate
 *
 * @return 0 if the structure is valid or 1 if it isn't
 *
 * @see gni_subnet_validate(), gni_cluster_validate(), gni_instance_validate(), gni_secgroup_validate()
 */
int gni_validate(globalNetworkInfo *gni) {
    int i = 0;
    int j = 0;

    // this is going to be messy, until we get XML validation in place
    if (!gni) {
        LOGERROR("invalid input\n");
        return (1);
    }
    // GNI should be initialized... but check just in case
    if (!gni->init) {
        LOGWARN("invalid input: gni is not initialized yet\n");
        return (1);
    }
    // Make sure we have a valid mode
    if (gni_netmode_validate(gni->sMode)) {
        return (1);
    }

    LOGTRACE("Validating XML for '%s' networking mode.\n", gni->sMode);

    // We need to know about which CLC is the enabled one. 0.0.0.0 means we don't know
    if (gni->enabledCLCIp == 0) {
        LOGWARN("no enabled CLC IP set: cannot validate XML\n");
        return (1);
    }
    // We should have some instance Domain Name information
    if (!strlen(gni->instanceDNSDomain)) {
        LOGWARN("no instanceDNSDomain set: cannot validate XML\n");
        return (1);
    }
    // We should have some instance Domain Name Server information
    if (!gni->max_instanceDNSServers || !gni->instanceDNSServers) {
        LOGWARN("no instanceDNSServers set: cannot validate XML\n");
        return (1);
    }
    // Validate that we don't have a corrupted list. All 0.0.0.0 addresses are invalid
    for (i = 0; i < gni->max_instanceDNSServers; i++) {
        if (gni->instanceDNSServers[i] == 0) {
            LOGWARN("empty instanceDNSServer set at idx %d: cannot validate XML\n", i);
            return (1);
        }
    }
    // We should have some public IPs set if not, we'll just warn the user
    // public IPs is irrelevant in VPCMIDO (see publicNetworkCidr in mido section)
    if (!IS_NETMODE_VPCMIDO(gni)) {
        if (!gni->max_public_ips_str) {
            LOGTRACE("no public_ips set\n");
        }
    }

    if (IS_NETMODE_EDGE(gni)) {
        //
        // This is for the EDGE case. We should have a valid list of subnets and our clusters
        // should be valid for an EDGE mode
        //
        if (!gni->max_subnets || !gni->subnets) {
            LOGTRACE("no subnets set\n");
        } else {
            for (i = 0; i < gni->max_subnets; i++) {
                if (gni_subnet_validate(&(gni->subnets[i]))) {
                    LOGWARN("invalid subnets set at idx %d: cannot validate XML\n", i);
                    return (1);
                }
            }
        }
        
        // No VPC elements are expected in EDGE mode
        if (gni->max_vpcs || gni->max_dhcpos || gni->max_vpcIgws) {
            LOGERROR("Invalid GNI (%s): VPC elements found in EDGE mode gni\n", gni->version);
            return (1);
        }
    }

    // Validate the clusters
    if (!gni->max_clusters || !gni->clusters) {
        LOGTRACE("no clusters set\n");
    } else {
        for (i = 0; i < gni->max_clusters; i++) {
            if (gni_cluster_validate(&(gni->clusters[i]), gni->nmCode)) {
                LOGWARN("invalid clusters set at idx %d: cannot validate XML\n", i);
                return (1);
            }
        }
    }

    // If we have any instance provided, validate them
    if (!gni->max_instances || !gni->instances) {
        LOGTRACE("no instances set\n");
    } else {
        for (i = 0; i < gni->max_instances; i++) {
            if (gni_instance_validate(gni->instances[i])) {
                LOGWARN("invalid instances set at idx %d: cannot validate XML\n", i);
                return (1);
            }
        }
    }

    // Validate interfaces
    if (!gni->max_ifs || !gni->ifs) {
        LOGTRACE("no interfaces set\n");
    } else {
        for (i = 0; i < gni->max_ifs; i++) {
            if (gni_interface_validate(gni->ifs[i])) {
                LOGWARN("invalid instances set at idx %d: cannot validate XML\n", i);
                return (1);
            }
        }
    }

    // If we have any security group provided, we should be able to validate them
    if (!gni->max_secgroups || !gni->secgroups) {
        LOGTRACE("no secgroups set\n");
    } else {
        for (i = 0; i < gni->max_secgroups; i++) {
            if (gni_secgroup_validate(&(gni->secgroups[i]))) {
                LOGWARN("invalid secgroups set at idx %d: cannot validate XML\n", i);
                return (1);
            }
        }
    }

    // Validate VPCMIDO elements
    if (IS_NETMODE_VPCMIDO(gni)) {
        // Validate VPCs
        for (i = 0; i < gni->max_vpcs; i++) {
            if (gni_vpc_validate(&(gni->vpcs[i]))) {
                LOGWARN("invalid vpc set at idx %d\n", i);
                return (1);
            }
            // Validate subnets
            for (j = 0; j < gni->vpcs[i].max_subnets; j++) {
                if (gni_vpcsubnet_validate(&(gni->vpcs[i].subnets[j]))) {
                    LOGWARN("invalid vpcsubnet set at idx %d\n", i);
                    return (1);
                }
            }
            // Validate NAT gateways
            for (j = 0; j < gni->vpcs[i].max_natGateways; j++) {
                if (gni_nat_gateway_validate(&(gni->vpcs[i].natGateways[j]))) {
                    LOGWARN("invalid NAT gateway set at idx %d\n", i);
                    return (1);
                }
            }
            // Validate route tables
            for (j = 0; j < gni->vpcs[i].max_routeTables; j++) {
                if (gni_route_table_validate(&(gni->vpcs[i].routeTables[j]))) {
                    LOGWARN("invalid route table set at idx %d\n", i);
                    return (1);
                }
            }
            // Validate network acls
            for (j = 0; j < gni->vpcs[i].max_networkAcls; j++) {
                if (gni_networkacl_validate(&(gni->vpcs[i].networkAcls[j]))) {
                    LOGWARN("invalid network ACL set at idx %d\n", i);
                    return (1);
                }
            }
        }
    }
    return (0);
}

/**
 * Validate a networking mode provided in the GNI message. The only supported networking
 * mode strings are: EDGE and VPCMIDO.
 *
 * @param psMode [in] a string pointer to the network mode to validate
 *
 * @return 0 if the mode is valid or 1 if the mode isn't
 */
int gni_netmode_validate(const char *psMode) {
    int i = 0;

    if (!psMode) {
        LOGERROR("invalid input\n");
        return (1);
    }
    // Do we know anything about this mode?
    for (i = 0; asNetModes[i] != NULL; i++) {
        if (!strcmp(psMode, asNetModes[i])) {
            return (0);
        }
    }

    if (strlen(psMode) > 0) {
        LOGDEBUG("invalid network mode '%s'\n", psMode);
    } else {
        LOGTRACE("network mode is empty.\n");
    }
    return (1);
}

/**
 * Validate a gni_subnet structure content
 *
 * @param pSubnet [in] a pointer to the subnet structure to validate
 *
 * @return 0 if the structure is valid or 1 if the structure isn't
 */
int gni_subnet_validate(gni_subnet *pSubnet) {
    if (!pSubnet) {
        LOGERROR("invalid input\n");
        return (1);
    }
    // If any of the subnet, netmask or gateway is 0.0.0.0, this is invalid
    if ((pSubnet->subnet == 0) || (pSubnet->netmask == 0) || (pSubnet->gateway == 0)) {
        LOGWARN("invalid subnet: subnet=%d netmask=%d gateway=%d\n", pSubnet->subnet, pSubnet->netmask, pSubnet->gateway);
        return (1);
    }

    return (0);
}

/**
 * Validate a gni_cluster structure content
 *
 * @param cluster [in] a pointer to the cluster structure to validate
 * @param nmode [in] valid euca_netmode enumeration value
 *
 * @return 0 if the structure is valid or 1 if it isn't
 *
 * @see gni_node_validate()
 */
int gni_cluster_validate(gni_cluster *cluster, euca_netmode nmode) {
    int i = 0;

    // Make sure our pointer is valid
    if (!cluster) {
        LOGERROR("invalid input\n");
        return (1);
    }
    // We must have a name
    if (!strlen(cluster->name)) {
        LOGWARN("no cluster name\n");
        return (1);
    }
    // The enabled CC IP must not be 0.0.0.0
    if (cluster->enabledCCIp == 0) {
        LOGWARN("cluster %s: no enabledCCIp\n", cluster->name);
        return (1);
    }
    // We must be provided with a MAC prefix
    if (strlen(cluster->macPrefix) == 0) {
        LOGWARN("cluster %s: no macPrefix\n", cluster->name);
        return (1);
    }
    //
    // For EDGE, we need to validate the subnet and the private IPs
    //
    if (nmode == NM_EDGE) {
        // Validate the given private subnet
        if (gni_subnet_validate(&(cluster->private_subnet))) {
            LOGWARN("cluster %s: invalid cluster private_subnet\n", cluster->name);
            return (1);
        }
        // Validate the list of private IPs. We must have some.
        if (!cluster->max_private_ips_str) {
            LOGWARN("cluster %s: no private_ips\n", cluster->name);
            return (1);
        }
    }
    // Do we have some nodes for this cluster?
    if (!cluster->max_nodes || !cluster->nodes) {
        LOGWARN("cluster %s: no nodes set\n", cluster->name);
    } else {
        // Validate each nodes
        for (i = 0; i < cluster->max_nodes; i++) {
            if (gni_node_validate(&(cluster->nodes[i]))) {
                LOGWARN("cluster %s: invalid nodes set at idx %d\n", cluster->name, i);
                return (1);
            }
        }
    }

    return (0);
}

/**
 * Validate a gni_node structure content
 *
 * @param node [in] a pointer to the node structure to validate
 *
 * @return 0 if the structure is valid or 1 if it isn't
 */
int gni_node_validate(gni_node *node) {
    int i;

    if (!node) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (!strlen(node->name)) {
        LOGWARN("no node name\n");
        return (1);
    }

    if (!node->max_instance_names || !node->instance_names) {
    } else {
        for (i = 0; i < node->max_instance_names; i++) {
            if (!strlen(node->instance_names[i].name)) {
                LOGWARN("node %s: empty instance_names set at idx %d\n", node->name, i);
                return (1);
            }
        }
    }

    return (0);
}

/**
 * Validates a given instance_interface structure content for a valid instance
 * description
 *
 * @param instance [in] a pointer to the instance_interface structure to validate
 *
 * @return 0 if the structure is valid or 1 if it isn't
 */
int gni_instance_validate(gni_instance *instance) {
    int i;

    if (!instance) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (!strlen(instance->name)) {
        LOGWARN("no instance name\n");
        return (1);
    }

    if (!strlen(instance->accountId)) {
        LOGWARN("instance %s: no accountId\n", instance->name);
        return (1);
    }

    if (!maczero(instance->macAddress)) {
        LOGWARN("instance %s: no macAddress\n", instance->name);
    }

    if (!instance->publicIp) {
        LOGTRACE("instance %s: no publicIp set (ignore if instance was run with private only addressing)\n", instance->name);
    }

    if (!instance->privateIp) {
        LOGWARN("instance %s: no privateIp\n", instance->name);
        return (1);
    }

    if (!instance->max_secgroup_names || !instance->secgroup_names) {
        LOGWARN("instance %s: no secgroups\n", instance->name);
        return (1);
    } else {
        for (i = 0; i < instance->max_secgroup_names; i++) {
            if (!strlen(instance->secgroup_names[i].name)) {
                LOGWARN("instance %s: empty secgroup_names set at idx %d\n", instance->name, i);
                return (1);
            }
        }
    }

    //gni_instance_interface_print(instance, EUCA_LOG_TRACE);
    return (0);
}

/**
 * Validates a given gni_instance_interface structure content for a valid interface
 * description.
 *
 * @param interface [in] a pointer to the instance_interface structure to validate
 *
 * @return 0 if the structure is valid or 1 if it isn't
 */
int gni_interface_validate(gni_instance *interface) {
    int i;

    if (!interface) {
        LOGERROR("Invalid argument: NULL\n");
        return (1);
    }

    if (!strlen(interface->name)) {
        LOGWARN("no instance name\n");
        return (1);
    }

    if (!strlen(interface->accountId)) {
        LOGWARN("instance %s: no accountId\n", interface->name);
        return (1);
    }

    if (!maczero(interface->macAddress)) {
        LOGWARN("instance %s: no macAddress\n", interface->name);
    }

    if (!interface->publicIp) {
        LOGTRACE("instance %s: no publicIp set (ignore if instance was run with private only addressing)\n", interface->name);
    }

    if (!interface->privateIp) {
        LOGWARN("instance %s: no privateIp\n", interface->name);
        return (1);
    }

    if (!interface->max_secgroup_names || !interface->secgroup_names) {
        LOGTRACE("instance %s: no secgroups\n", interface->name);
    } else {
        for (i = 0; i < interface->max_secgroup_names; i++) {
            if (!strlen(interface->secgroup_names[i].name)) {
                LOGWARN("instance %s: empty secgroup_names set at idx %d\n", interface->name, i);
                return (1);
            }
        }
    }

    // Validate properties specific to interfaces
    // TODO - validate srcdestcheck and deviceidx

    //gni_instance_interface_print(interface, EUCA_LOG_TRACE);
    return (0);
}

/**
 * Validates a given gni_secgroup structure content
 *
 * @param secgroup [in] a pointer to the security group structure to validate
 *
 * @return 0 if the structure is valid and 1 if the structure isn't
 *
 * @see gni_secgroup
 */
int gni_secgroup_validate(gni_secgroup *secgroup) {
    int i;

    if (!secgroup) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (!strlen(secgroup->name)) {
        LOGWARN("no secgroup name\n");
        return (1);
    }

    if (!strlen(secgroup->accountId)) {
        LOGWARN("secgroup %s: no accountId\n", secgroup->name);
        return (1);
    }

    if (!secgroup->max_ingress_rules || !secgroup->ingress_rules) {
        LOGTRACE("secgroup %s: no secgroup rules\n", secgroup->name);
    }

    if (!secgroup->max_instances || !secgroup->instances) {
        LOGTRACE("secgroup %s: no instances\n", secgroup->name);
    } else {
        for (i = 0; i < secgroup->max_instances; i++) {
            if (!strlen(secgroup->instances[i]->name)) {
                LOGWARN("secgroup %s: empty instance_name set at idx %d\n", secgroup->name, i);
                return (1);
            }
        }
    }

    if (!secgroup->max_interfaces || !secgroup->interfaces) {
        LOGTRACE("secgroup %s: no interfaces\n", secgroup->name);
    } else {
        for (i = 0; i < secgroup->max_interfaces; i++) {
            if (!strlen(secgroup->interfaces[i]->name)) {
                LOGWARN("secgroup %s: empty interface_name set at idx %d\n", secgroup->name, i);
                return (1);
            }
        }
    }

    //gni_sg_print(secgroup, EUCA_LOG_TRACE);
    return (0);
}

/**
 * Validates a given gni_vpc structure content
 *
 * @param vpc [in] a pointer to the vpc structure to validate
 *
 * @return 0 if the structure is valid and 1 if the structure isn't
 *
 */
int gni_vpc_validate(gni_vpc *vpc) {
    if (!vpc) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (!strlen(vpc->name)) {
        LOGWARN("no vpc name\n");
        return (1);
    }

    if (!strlen(vpc->accountId)) {
        LOGWARN("vpc %s: no accountId\n", vpc->name);
        return (1);
    }

    return (0);
}

/**
 * Validates a given gni_vpc structure content
 *
 * @param vpcsubnet [in] a pointer to the vpcsubnet structure to validate
 *
 * @return 0 if the structure is valid and 1 if the structure isn't
 *
 */
int gni_vpcsubnet_validate(gni_vpcsubnet *vpcsubnet) {
    if (!vpcsubnet) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (!strlen(vpcsubnet->name)) {
        LOGWARN("no vpc name\n");
        return (1);
    }

    if (!strlen(vpcsubnet->accountId)) {
        LOGWARN("vpc %s: no accountId\n", vpcsubnet->name);
        return (1);
    }

    return (0);
}

/**
 * Validates a given gni_vpc structure content
 *
 * @param natg [in] a pointer to the nat_gateway structure to validate
 *
 * @return 0 if the structure is valid and 1 if the structure isn't
 *
 */
int gni_nat_gateway_validate(gni_nat_gateway *natg) {
    if (!natg) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (!strlen(natg->name)) {
        LOGWARN("no natg name\n");
        return (1);
    }

    if (!strlen(natg->accountId)) {
        LOGWARN("natg %s: no accountId\n", natg->name);
        return (1);
    }

    if (!maczero(natg->macAddress)) {
        LOGWARN("natg %s: no macAddress\n", natg->name);
    }

    if (!natg->publicIp) {
        LOGTRACE("natg %s: no publicIp set\n", natg->name);
    }

    if (!natg->privateIp) {
        LOGWARN("natg %s: no privateIp\n", natg->name);
        return (1);
    }

    if (!strlen(natg->vpc)) {
        LOGWARN("natg %s: no vpc\n", natg->name);
        return (1);
    }

    if (!strlen(natg->subnet)) {
        LOGWARN("natg %s: no vpc subnet\n", natg->name);
        return (1);
    }

    return (0);
}

/**
 * Validates a given route_table structure content
 *
 * @param rtable [in] a pointer to the route_table structure to validate
 *
 * @return 0 if the structure is valid and 1 if the structure isn't
 *
 */
int gni_route_table_validate(gni_route_table *rtable) {
    if (!rtable) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (!strlen(rtable->name)) {
        LOGWARN("no route table name\n");
        return (1);
    }

    if (!strlen(rtable->accountId)) {
        LOGWARN("route table %s: no accountId\n", rtable->name);
        return (1);
    }

    for (int i = 0; i < rtable->max_entries; i++) {
        if (!strlen(rtable->entries[i].destCidr) || !strlen(rtable->entries[i].target)) {
            LOGWARN("route table %s: invalid route entry at idx %d\n", rtable->name, i);
            return (1);
        }
    }
    return (0);
}

/**
 * Validates a given gni_network_acl structure content
 *
 * @param acl [in] a pointer to the acl structure to validate
 *
 * @return 0 if the structure is valid and 1 if the structure isn't
 *
 */
int gni_networkacl_validate(gni_network_acl *acl) {
    if (!acl) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (!strlen(acl->name)) {
        LOGWARN("no network acl name\n");
        return (1);
    }

    if (!strlen(acl->accountId)) {
        LOGWARN("network acl %s: no accountId\n", acl->name);
        return (1);
    }

    for (int i = 0; i < acl->max_ingress; i++) {
        gni_acl_entry *entry = &(acl->ingress[i]);
        if (entry->number == 0) {
            LOGWARN("network acl %s: invalid ingress entry %d\n", acl->name, entry->number);
            return (1);
        }
        if (!strlen(entry->cidr)) {
            LOGWARN("network acl %s: invalid CIDR at entry %d\n", acl->name, entry->number);
            return (1);
        }
        if (entry->protocol == 0) {
            LOGWARN("network acl %s: invalid protocol at entry %d\n", acl->name, entry->number);
            return (1);
        }
    }

    for (int i = 0; i < acl->max_egress; i++) {
        gni_acl_entry *entry = &(acl->egress[i]);
        if (entry->number == 0) {
            LOGWARN("network acl %s: invalid egress entry %d\n", acl->name, entry->number);
            return (1);
        }
        if (!strlen(entry->cidr)) {
            LOGWARN("network acl %s: invalid CIDR at entry %d\n", acl->name, entry->number);
            return (1);
        }
        if (entry->protocol == 0) {
            LOGWARN("network acl %s: invalid protocol at entry %d\n", acl->name, entry->number);
            return (1);
        }
    }
    return (0);
}

/**
 * Logs the contents of an instance_interface structure.
 * @param inst [in] instance_interface of interest.
 * @param loglevel [in] valid value from log_level_e enumeration.
 */
void gni_instance_interface_print(gni_instance *inst, int loglevel) {
    char *mac = NULL;
    char *pubip = NULL;
    char *privip = NULL;
    hex2mac(inst->macAddress, &mac);
    pubip = hex2dot(inst->publicIp);
    privip = hex2dot(inst->privateIp);
    int i = 0;

    if (!inst) {
        EUCALOG(loglevel, "Invalid argument: NULL.\n");
        return;
    }
    EUCALOG(loglevel, "------ name = %s -----\n", inst->name);
    EUCALOG(loglevel, "\taccountId    = %s\n", inst->accountId);
    EUCALOG(loglevel, "\tmacAddress   = %s\n", mac);
    EUCALOG(loglevel, "\tpublicIp     = %s\n", pubip);
    EUCALOG(loglevel, "\tprivateIp    = %s\n", privip);
    EUCALOG(loglevel, "\tvpc          = %s\n", inst->vpc);
    EUCALOG(loglevel, "\tsubnet       = %s\n", inst->subnet);
    EUCALOG(loglevel, "\tnode         = %s\n", inst->node);
    //EUCALOG(loglevel, "\tnodehostname = %s\n", inst->nodehostname);
    EUCALOG(loglevel, "\tinstance     = %s\n", inst->instance_name.name);
    EUCALOG(loglevel, "\tifname       = %s\n", inst->ifname);
    EUCALOG(loglevel, "\tsrcdstcheck  = %s\n", inst->srcdstcheck ? "true" : "false");
    EUCALOG(loglevel, "\tdeviceidx    = %d\n", inst->deviceidx);
    EUCALOG(loglevel, "\tattachment   = %s\n", inst->attachmentId);

    for (i = 0; i < inst->max_interfaces; i++) {
        EUCALOG(loglevel, "\tinterface[%d] = %s idx %d\n", i, inst->interfaces[i]->name, inst->interfaces[i]->deviceidx);
    }
    for (i = 0; i < inst->max_secgroup_names; i++) {
        EUCALOG(loglevel, "\tsg[%d]        = %s\n", i, inst->secgroup_names[i].name);
    }
    EUCA_FREE(mac);
    EUCA_FREE(pubip);
    EUCA_FREE(privip);
}

/**
 * Logs the contents of an instance_interface structure.
 * @param sg [in] instance_interface of interest.
 * @param loglevel [in] valid value from log_level_e enumeration.
 */
void gni_sg_print(gni_secgroup *sg, int loglevel) {
    int i = 0;

    if (!sg) {
        EUCALOG(loglevel, "Invalid argument: NULL.\n");
        return;
    }
    EUCALOG(loglevel, "------ name = %s -----\n", sg->name);
    EUCALOG(loglevel, "\taccountId    = %s\n", sg->accountId);
    EUCALOG(loglevel, "\tingress      = %d rules\n", sg->max_ingress_rules);
    for (i = 0; i < sg->max_ingress_rules; i++) {
        EUCALOG(loglevel, "\t\t%s %d %d %d %d %d %s\n", sg->ingress_rules[i].cidr,
                sg->ingress_rules[i].protocol, sg->ingress_rules[i].fromPort, sg->ingress_rules[i].toPort,
                sg->ingress_rules[i].icmpType, sg->ingress_rules[i].icmpCode, sg->ingress_rules[i].groupId);
    }
    EUCALOG(loglevel, "\tegress       = %d rules\n", sg->max_egress_rules);
    for (i = 0; i < sg->max_egress_rules; i++) {
        EUCALOG(loglevel, "\t\t%s %d %d %d %d %d %s\n", sg->egress_rules[i].cidr,
                sg->egress_rules[i].protocol, sg->egress_rules[i].fromPort, sg->egress_rules[i].toPort,
                sg->egress_rules[i].icmpType, sg->egress_rules[i].icmpCode, sg->egress_rules[i].groupId);
    }
    for (i = 0; i < sg->max_instances; i++) {
        EUCALOG(loglevel, "\tinstance[%d] = %s\n", i, sg->instances[i]->name);
    }
    for (i = 0; i < sg->max_interfaces; i++) {
        EUCALOG(loglevel, "\tinterface[%d] = %s\n", i, sg->interfaces[i]->name);
    }
}

/**
 * Logs the contents of a vpc structure.
 * @param vpc [in] VPC of interest
 * @param loglevel [in] valid value from log_level_e enumeration
 */
void gni_vpc_print(gni_vpc *vpc, int loglevel) {
    int i = 0;

    if (!vpc) {
        EUCALOG(loglevel, "Invalid argument: NULL.\n");
        return;
    }
    EUCALOG(loglevel, "------ name = %s -----\n", vpc->name);
    EUCALOG(loglevel, "\taccountId    = %s\n", vpc->accountId);
    EUCALOG(loglevel, "\tcidr         = %s\n", vpc->cidr);
    EUCALOG(loglevel, "\tdhcpOptionSet= %s %p\n", vpc->dhcpOptionSet_name, vpc->dhcpOptionSet);
    EUCALOG(loglevel, "\tsubnets      = %d\n", vpc->max_subnets);
    for (i = 0; i < vpc->max_subnets; i++) {
        gni_vpcsubnet *s = &(vpc->subnets[i]);
        EUCALOG(loglevel, "\t---- name = %s ----\n", s->name);
        EUCALOG(loglevel, "\t\taccountId = %s\n", s->accountId);
        EUCALOG(loglevel, "\t\tcidr      = %s\n", s->cidr);
        EUCALOG(loglevel, "\t\tcluster   = %s\n", s->cluster_name);
        EUCALOG(loglevel, "\t\tnetAcl    = %s %p\n", s->networkAcl_name, s->networkAcl);
        EUCALOG(loglevel, "\t\trouteTable= %s\n", s->routeTable_name);
        EUCALOG(loglevel, "\t\tinterfaces= %d\n", s->max_interfaces);
    }
    EUCALOG(loglevel, "\tnetworkAcl   = %d\n", vpc->max_networkAcls);
    for (i = 0; i < vpc->max_networkAcls; i++) {
        //EUCALOG(loglevel, "\t\t\n");
    }
    EUCALOG(loglevel, "\trouteTables  = %d\n", vpc->max_routeTables);
    for (i = 0; i < vpc->max_routeTables; i++) {
        gni_route_table *t = &(vpc->routeTables[i]);
        EUCALOG(loglevel, "\t---- name =  %s ----\n", t->name);
        EUCALOG(loglevel, "\t\taccountId = %s\n", t->accountId);
        EUCALOG(loglevel, "\t\troutes    = %d\n", t->max_entries);
        for (int j = 0; j < t->max_entries; j++) {
            gni_route_entry *e = &(t->entries[j]);
            EUCALOG(loglevel, "\t\t\t%s -> %s\n", e->destCidr, e->target);
        }
    }
    EUCALOG(loglevel, "\tnatGateways  = %d\n", vpc->max_natGateways);
    for (i = 0; i < vpc->max_natGateways; i++) {
        gni_nat_gateway *t = &(vpc->natGateways[i]);
        char *mac = NULL;
        char *pubip = hex2dot(t->publicIp);
        char *privip = hex2dot(t->privateIp);
        hex2mac(t->macAddress, &mac);
        EUCALOG(loglevel, "\t---- name = %s ----\n", t->name);
        EUCALOG(loglevel, "\t\taccountId = %s\n", t->accountId);
        EUCALOG(loglevel, "\t\tmac       = %s\n", mac);
        EUCALOG(loglevel, "\t\tpublicIp  = %s\n", pubip);
        EUCALOG(loglevel, "\t\tprivateIp = %s\n", privip);
        EUCALOG(loglevel, "\t\tvpc       = %s\n", t->vpc);
        EUCALOG(loglevel, "\t\tsubnet    = %s\n", t->subnet);
        EUCA_FREE(mac);
        EUCA_FREE(pubip);
        EUCA_FREE(privip);
    }
    EUCALOG(loglevel, "\tnetworkAcls  = %d\n", vpc->max_networkAcls);
    for (i = 0; i < vpc->max_networkAcls; i++) {
        gni_network_acl *t = &(vpc->networkAcls[i]);
        EUCALOG(loglevel, "\t\tingress      = %d rules\n", t->max_ingress);
        for (i = 0; i < t->max_ingress; i++) {
            EUCALOG(loglevel, "\t\t\t%d %s %d %d %d %d %d %s\n", t->ingress[i].number, t->ingress[i].cidr,
                    t->ingress[i].protocol, t->ingress[i].fromPort, t->ingress[i].toPort,
                    t->ingress[i].icmpType, t->ingress[i].icmpCode, t->ingress[i].allow ? "allow" : "deny");
        }
        EUCALOG(loglevel, "\t\tegress       = %d rules\n", t->max_egress);
        for (i = 0; i < t->max_egress; i++) {
            EUCALOG(loglevel, "\t\t\t%d %s %d %d %d %d %d %s\n", t->ingress[i].number, t->egress[i].cidr,
                    t->egress[i].protocol, t->egress[i].fromPort, t->egress[i].toPort,
                    t->egress[i].icmpType, t->egress[i].icmpCode, t->egress[i].allow ? "allow" : "deny");
        }
    }
    EUCALOG(loglevel, "\tIGNames      = %d\n", vpc->max_internetGatewayNames);
    char names[2048];
    names[0] = '\0';
    for (i = 0; i < vpc->max_internetGatewayNames; i++) {
        gni_name_32 *t = &(vpc->internetGatewayNames[i]);
        strncat(names, t->name, 32);
        strncat(names, " ", 2);
    }
    if (strlen(names)) {
        EUCALOG(loglevel, "\t\t%s\n", names);
    }
    EUCALOG(loglevel, "\tinterfaces   = %d\n", vpc->max_interfaces);
    names[0] = '\0';
    for (i = 0; i < vpc->max_interfaces; i++) {
        gni_instance *t = vpc->interfaces[i];
        strncat(names, t->name, INTERFACE_ID_LEN);
        strncat(names, " ", 2);
    }
    if (strlen(names)) {
        EUCALOG(loglevel, "\t\t%s\n", names);
    }
}

/**
 * Logs the contents of an internet_gateway structure.
 * @param ig [in] internet gateway of interest.
 * @param loglevel [in] valid value from log_level_e enumeration.
 */
void gni_internetgateway_print(gni_internet_gateway *ig, int loglevel) {
    if (!ig) {
        EUCALOG(loglevel, "Invalid argument: NULL.\n");
        return;
    }
    EUCALOG(loglevel, "------ name = %s -----\n", ig->name);
    EUCALOG(loglevel, "\taccountId    = %s\n", ig->accountId);
}

/**
 * Logs the contents of a dhcp_os structure.
 * @param dhcpos [in] dhcp_os structure of interest.
 * @param loglevel [in] valid value from log_level_e enumeration.
 */
void gni_dhcpos_print(gni_dhcp_os *dhcpos, int loglevel) {
    if (!dhcpos) {
        EUCALOG(loglevel, "Invalid argument: NULL.\n");
        return;
    }
    EUCALOG(loglevel, "------ name = %s -----\n", dhcpos->name);
    EUCALOG(loglevel, "\taccountId    = %s\n", dhcpos->accountId);

    char *dhcpdstr = NULL;
    char dhcpsstr[1024];
    dhcpsstr[0] = '\0';
    for (int j = 0; j < dhcpos->max_domains; j++) {
        strncat(dhcpsstr, dhcpos->domains[j].name, 512);
        strncat(dhcpsstr, " ", 512);
    }
    if (dhcpos->max_domains) {
        EUCALOG(loglevel, "\tdomains: %s\n", dhcpsstr);
    }
    dhcpsstr[0] = '\0';
    for (int j = 0; j < dhcpos->max_dns; j++) {
        dhcpdstr = hex2dot(dhcpos->dns[j]);
        strncat(dhcpsstr, dhcpdstr, 512);
        strncat(dhcpsstr, ", ", 512);
        EUCA_FREE(dhcpdstr);
    }
    if (dhcpos->max_dns) {
        if (strlen(dhcpsstr) > 2) {
            dhcpsstr[strlen(dhcpsstr) - 2] = '\0';
        }
        EUCALOG(loglevel, "\tdns: %s\n", dhcpsstr);
    }
    dhcpsstr[0] = '\0';
    for (int j = 0; j < dhcpos->max_ntp; j++) {
        dhcpdstr = hex2dot(dhcpos->ntp[j]);
        strncat(dhcpsstr, dhcpdstr, 512);
        strncat(dhcpsstr, ", ", 512);
        EUCA_FREE(dhcpdstr);
    }
    if (dhcpos->max_ntp) {
        if (strlen(dhcpsstr) > 2) {
            dhcpsstr[strlen(dhcpsstr) - 2] = '\0';
        }
        EUCALOG(loglevel, "\tntp: %s\n", dhcpsstr);
    }
    for (int j = 0; j < dhcpos->max_netbios_ns; j++) {
        dhcpdstr = hex2dot(dhcpos->netbios_ns[j]);
        strncat(dhcpsstr, dhcpdstr, 512);
        strncat(dhcpsstr, ", ", 512);
        EUCA_FREE(dhcpdstr);
    }
    if (dhcpos->max_netbios_ns) {
        if (strlen(dhcpsstr) > 2) {
            dhcpsstr[strlen(dhcpsstr) - 2] = '\0';
        }
        EUCALOG(loglevel, "\tnetbios_ns: %s\n", dhcpsstr);
    }
    if (dhcpos->netbios_type) {
        EUCALOG(loglevel, "\tnetbios_type: %d\n", dhcpos->netbios_type);
    }
}

/**
 * Allocates memory for gni_host_info data structure
 * @return pointer to the allocated memory.
 */
gni_hostname_info *gni_init_hostname_info(void) {
    gni_hostname_info *hni = EUCA_ZALLOC_C(1, sizeof (gni_hostname_info));
    hni->max_hostnames = 0;
    return (hni);
}

/**
 * Logs the contents of a gni_hostname_info data structure
 * @param host_info [in] gni_hostname_info data structure of interest
 * @return always 0.
 */
int gni_hostnames_print(gni_hostname_info *host_info) {
    int i;

    LOGTRACE("Cached Hostname Info: \n");
    for (i = 0; i < host_info->max_hostnames; i++) {
        LOGTRACE("IP Address: %s Hostname: %s\n", inet_ntoa(host_info->hostnames[i].ip_address), host_info->hostnames[i].hostname);
    }
    return (0);
}

/**
 * Releases resources allocated for a gni_hostname_info data structure
 * @param host_info [in] gni_hostname_info data structure
 * @return always 0.
 */
int gni_hostnames_free(gni_hostname_info *host_info) {
    if (!host_info) {
        return (0);
    }

    EUCA_FREE(host_info->hostnames);
    EUCA_FREE(host_info);
    return (0);
}

/**
 * Searches hostname cache for the given IP address.
 * @param hostinfo [in] hostname cache in a gni_hostname_info data structure
 * @param ip_address [in] IP address to look for
 * @param hostname [out] corresponding hostname if found
 * @return 0 on success. 1 on failure
 */
int gni_hostnames_get_hostname(gni_hostname_info *hostinfo, const char *ip_address, char **hostname) {
    struct in_addr addr;
    gni_hostname key;
    gni_hostname *bsearch_result;

    if (!hostinfo) {
        return (1);
    }

    if (inet_aton(ip_address, &addr)) {
        key.ip_address.s_addr = addr.s_addr; // search by ip
        bsearch_result = bsearch(&key, hostinfo->hostnames, hostinfo->max_hostnames, sizeof (gni_hostname), cmpipaddr);

        if (bsearch_result) {
            *hostname = strdup(bsearch_result->hostname);
            LOGTRACE("bsearch hit: %s\n", *hostname);
            return (0);
        }
    } else {
        LOGTRACE("INET_ATON FAILED FOR: %s\n", ip_address); // we were passed a hostname
    }
    return (1);
}

//
// Used for qsort and bsearch methods against gni_hostname_info
//

/**
 * Compares gni_hostname p1 and p2 (used in qsort() and bsearch())
 * @param p1 [in] gni_hostname 1 to be compared
 * @param p2 [in] gni_hostname 2 to be compared
 * @return 0 if p1 and p2 match. -1 iff p1 < p2. 1 iff p1 > p2.
 */
int cmpipaddr(const void *p1, const void *p2) {
    gni_hostname *hp1 = (gni_hostname *) p1;
    gni_hostname *hp2 = (gni_hostname *) p2;

    if (hp1->ip_address.s_addr == hp2->ip_address.s_addr)
        return 0;
    if (hp1->ip_address.s_addr < hp2->ip_address.s_addr)
        return -1;
    else
        return 1;
}

/**
 * Compares two globalNetworkInfo structures an and b and search for
 * VPCMIDO configuration changes.
 * @param a [in] globalNetworkInfo structure of interest.
 * @param b [in] globalNetworkInfo structure of interest.
 * @return 0 if configuration parameters in a and b match. Non-zero otherwise. 
 */
int cmp_gni_config(globalNetworkInfo *a, globalNetworkInfo *b) {
    int ret = 0;
    if (a == b) {
        return (0);
    }
    if ((a == NULL) || (b == NULL)) {
        return (GNI_CONFIG_DIFF_OTHER);
    }
    if (a->enabledCLCIp != b->enabledCLCIp) {
        ret |= GNI_CONFIG_DIFF_ENABLEDCLCIP;
    }
    if (strcmp(a->instanceDNSDomain, b->instanceDNSDomain)) {
        ret |= GNI_CONFIG_DIFF_INSTANCEDNSDOMAIN;
    }
    if (a->max_instanceDNSServers != b->max_instanceDNSServers) {
        ret |= GNI_CONFIG_DIFF_INSTANCEDNSSERVERS;
    } else {
        for (int i = 0; i < a->max_instanceDNSServers; i++) {
            if (a->instanceDNSServers[i] != b->instanceDNSServers[i]) {
                ret |= GNI_CONFIG_DIFF_INSTANCEDNSSERVERS;
                break;
            }
        }
    }
    if (IS_NETMODE_VPCMIDO(a) && IS_NETMODE_VPCMIDO(b)) {
        if (a->max_midogws != b->max_midogws) {
            ret |= GNI_CONFIG_DIFF_MIDOGATEWAYS;
            ret |= GNI_CONFIG_DIFF_MIDONODES;
        } else {
            for (int i = 0; i < a->max_midogws; i++) {
                if (cmp_gni_mido_gateway(&(a->midogws[i]), &(b->midogws[i]))) {
                    ret |= GNI_CONFIG_DIFF_MIDOGATEWAYS;
                    if (strcmp(a->midogws[i].host, b->midogws[i].host)) {
                        ret |= GNI_CONFIG_DIFF_MIDONODES;
                    }
                }
            }
        }
        if (a->max_clusters != b->max_clusters) {
            ret |= GNI_CONFIG_DIFF_MIDONODES;
        } else {
            for (int i = 0; i < a->max_clusters; i++) {
                if (a->clusters[i].max_nodes != b->clusters[i].max_nodes) {
                    ret |= GNI_CONFIG_DIFF_MIDONODES;
                }
            }
        }
    }

    if (IS_NETMODE_EDGE(a) && IS_NETMODE_EDGE(b)) {
        if (a->max_subnets != b->max_subnets) {
            ret |= GNI_CONFIG_DIFF_SUBNETS;
        } else {
            for (int i = 0; i < a->max_subnets; i++) {
                if ((a->subnets[i].subnet != b->subnets[i].subnet) ||
                        (a->subnets[i].netmask != b->subnets[i].netmask) ||
                        (a->subnets[i].gateway != b->subnets[i].gateway)) {
                    ret |= GNI_CONFIG_DIFF_SUBNETS;
                }
            }
        }
    }

    if (a->nmCode != b->nmCode) {
        ret |= GNI_CONFIG_DIFF_SUBNETS;
    }
    return (ret);
}

/**
 * Compares two gni_vpc structures a and b.
 *
 * @param a [in] gni_vpc structure of interest.
 * @param b [in] gni_vpc structure of interest.
 * @return 0 if name and number of entries match. Non-zero otherwise.
 */
int cmp_gni_vpc(gni_vpc *a, gni_vpc *b) {
    if (a == b) {
        return (0);
    }
    if ((a == NULL) || (b == NULL)) {
        return (1);
    }
    if (strcmp(a->name, b->name)) {
        return (1);
    }
    if (strcmp(a->dhcpOptionSet_name, b->dhcpOptionSet_name)) {
        return (1);
    }
    if ((a->max_internetGatewayNames == b->max_internetGatewayNames) &&
            (a->max_natGateways == b->max_natGateways) &&
            (a->max_networkAcls == b->max_networkAcls) &&
            (a->max_routeTables == b->max_routeTables) &&
            (a->max_subnets == b->max_subnets)) {
        return (0);
    }
    return (1);
}

/**
 * Compares two gni_vpcsubnet structures a and b.
 *
 * @param a [in] gni_vpcsubnet structure of interest.
 * @param b [in] gni_vpcsubnet structure of interest.
 * @param nacl_diff [out] set to 1 iff network acl association of a and b differs.
 * @return 0 if name, routeTable_name match. Non-zero otherwise. Difference in
 * network acl association is reflected in nacl_diff.
 */
int cmp_gni_vpcsubnet(gni_vpcsubnet *a, gni_vpcsubnet *b, int *nacl_diff) {
    if (a == b) {
        if (nacl_diff) *nacl_diff = 0;
        return (0);
    }
    if ((a == NULL) || (b == NULL)) {
        if (nacl_diff) *nacl_diff = 1;
        return (1);
    }
    if (strcmp(a->name, b->name)) {
        if (nacl_diff) *nacl_diff = 1;
        return (1);
    }
    if (nacl_diff) {
        *nacl_diff = 0;
        if (strcmp(a->networkAcl_name, b->networkAcl_name)) {
            *nacl_diff = 1;
        }
    }
    if (a->rt_entry_applied && a->routeTable) {
        for (int i = 0; i < a->routeTable->max_entries; i++) {
            if (!a->rt_entry_applied[i]) {
                return (1);
            }
        }
    }
    if ((!strcmp(a->routeTable_name, b->routeTable_name)) &&
            (!strcmp(a->networkAcl_name, b->networkAcl_name)) &&
            (!cmp_gni_route_table(a->routeTable, b->routeTable))) {
        return (0);
    }
    return (1);
}

/**
 * Compares two gni_nat_gateway structures a and b.
 *
 * @param a [in] gni_nat_gateway structure of interest.
 * @param b [in] gni_nat_gateway structure of interest.
 * @return 0 if name matches. Non-zero otherwise.
 */
int cmp_gni_nat_gateway(gni_nat_gateway *a, gni_nat_gateway *b) {
    if (a == b) {
        return (0);
    }
    if ((a == NULL) || (b == NULL)) {
        return (1);
    }
    if (!strcmp(a->name, b->name)) {
        return (0);
    }
    return (1);
}

/**
 * Compares two gni_route_table structures a and b.
 *
 * @param a [in] gni_route_table structure of interest. Check for route entries
 *            applied flags.
 * @param b [in] gni_route_table structure of interest.
 * @return 0 if name and route entries match. Non-zero otherwise.
 */
int cmp_gni_route_table(gni_route_table *a, gni_route_table *b) {
    if (a == b) {
        return (0);
    }
    if ((a == NULL) || (b == NULL)) {
        return (1);
    }
    if (strcmp(a->name, b->name)) {
        return (1);
    }
    if (a->max_entries != b->max_entries) {
        return (1);
    }
    for (int i = 0; i < a->max_entries; i++) {
        if ((strcmp(a->entries[i].destCidr, b->entries[i].destCidr)) ||
                (strcmp(a->entries[i].target, b->entries[i].target))) {
            return (1);
        }
    }
    return (0);
}

/**
 * Compares two gni_secgroup structures a and b.
 *
 * @param a [in] gni_secgroup structure of interest.
 * @param b [in] gni_secgroup structure of interest.
 * @param ingress_diff [out] set to 1 iff ingress rules of a and b differ.
 * @param egress_diff [out] set to 1 iff egress rules of a and b differ.
 * @param interfaces_diff [out] set to 1 iff member interfaces of a and b differ.
 * @return 0 if name and rule entries match. Non-zero otherwise.
 *
 * @note order of rules are assumed to be the same for both a and b.
 */
int cmp_gni_secgroup(gni_secgroup *a, gni_secgroup *b, int *ingress_diff, int *egress_diff, int *interfaces_diff) {
    int abmatch = 1;
    if (a == b) {
        if (ingress_diff) *ingress_diff = 0;
        if (egress_diff) *egress_diff = 0;
        if (interfaces_diff) *interfaces_diff = 0;
        return (0);
    }

    if (ingress_diff) *ingress_diff = 1;
    if (egress_diff) *egress_diff = 1;
    if (interfaces_diff) *interfaces_diff = 1;

    if ((a == NULL) || (b == NULL)) {
        return (1);
    }
    if (strcmp(a->name, b->name)) {
        abmatch = 0;
    } else {
        if (a->max_ingress_rules == b->max_ingress_rules) {
            int diffound = 0;
            for (int i = 0; i < a->max_ingress_rules && !diffound; i++) {
                if ((a->ingress_rules[i].cidrNetaddr != b->ingress_rules[i].cidrNetaddr) ||
                        (a->ingress_rules[i].cidrSlashnet != b->ingress_rules[i].cidrSlashnet) ||
                        (a->ingress_rules[i].protocol != b->ingress_rules[i].protocol) ||
                        (a->ingress_rules[i].fromPort != b->ingress_rules[i].fromPort) ||
                        (a->ingress_rules[i].toPort != b->ingress_rules[i].toPort) ||
                        (a->ingress_rules[i].icmpCode != b->ingress_rules[i].icmpCode) ||
                        (a->ingress_rules[i].icmpType != b->ingress_rules[i].icmpType) ||
                        (strcmp(a->ingress_rules[i].groupId, b->ingress_rules[i].groupId))) {
                    diffound = 1;
                }
            }
            if (!diffound) {
                if (ingress_diff) *ingress_diff = 0;
            } else {
                abmatch = 0;
            }
        } else {
            abmatch = 0;
        }
        if (a->max_egress_rules == b->max_egress_rules) {
            int diffound = 0;
            for (int i = 0; i < a->max_egress_rules && !diffound; i++) {
                if ((a->egress_rules[i].cidrNetaddr != b->egress_rules[i].cidrNetaddr) ||
                        (a->egress_rules[i].cidrSlashnet != b->egress_rules[i].cidrSlashnet) ||
                        (a->egress_rules[i].protocol != b->egress_rules[i].protocol) ||
                        (a->egress_rules[i].fromPort != b->egress_rules[i].fromPort) ||
                        (a->egress_rules[i].toPort != b->egress_rules[i].toPort) ||
                        (a->egress_rules[i].icmpCode != b->egress_rules[i].icmpCode) ||
                        (a->egress_rules[i].icmpType != b->egress_rules[i].icmpType) ||
                        (strcmp(a->egress_rules[i].groupId, b->egress_rules[i].groupId))) {
                    diffound = 1;
                }
            }
            if (!diffound) {
                if (egress_diff) *egress_diff = 0;
            } else {
                abmatch = 0;
            }
        } else {
            abmatch = 0;
        }
        if (a->max_interfaces == b->max_interfaces) {
            int diffound = 0;
            for (int i = 0; i < a->max_interfaces && !diffound; i++) {
                if (strcmp(a->interfaces[i]->name, b->interfaces[i]->name)) {
                    diffound = 1;
                }
            }
            if (!diffound) {
                if (interfaces_diff) *interfaces_diff = 0;
            } else {
                abmatch = 0;
            }
        } else {
            abmatch = 0;
        }
        // For EDGE mode (EUCA-13389)
        if ((a->max_interfaces == 0) && (b->max_interfaces == 0) &&
                (a->max_instances == b->max_instances)) {
            int diffound = 0;
            for (int i = 0; i < a->max_instances && !diffound; i++) {
                if (strcmp(a->instances[i]->name, b->instances[i]->name)) {
                    diffound = 1;
                }
            }
            if (diffound) {
                abmatch = 0;
            }
        } else {
            abmatch = 0;
        }
    }

    if (abmatch) {
        return (0);
    }
    return (1);
}

/**
 * Compares two gni_network_acl structures a and b.
 *
 * @param a [in] gni_network_acl structure of interest.
 * @param b [in] gni_network_acl structure of interest.
 * @param ingress_diff [out] set to 1 iff ingress rules of a and b differ.
 * @param egress_diff [out] set to 1 iff egress rules of a and b differ.
 * @return 0 if name and rule entries match. Non-zero otherwise.
 *
 * @note order of rules are assumed to be the same for both a and b.
 */
int cmp_gni_nacl(gni_network_acl *a, gni_network_acl *b, int *ingress_diff, int *egress_diff) {
    int abmatch = 1;
    if (a == b) {
        if (ingress_diff) *ingress_diff = 0;
        if (egress_diff) *egress_diff = 0;
        return (0);
    }

    if (ingress_diff) *ingress_diff = 1;
    if (egress_diff) *egress_diff = 1;

    if ((a == NULL) || (b == NULL)) {
        return (1);
    }
    if (strcmp(a->name, b->name)) {
        abmatch = 0;
    } else {
        if (a->max_ingress == b->max_ingress) {
            int diffound = 0;
            for (int i = 0; i < a->max_ingress && !diffound; i++) {
                if ((a->ingress[i].number != b->ingress[i].number) ||
                        (a->ingress[i].allow != b->ingress[i].allow) ||
                        (a->ingress[i].cidrNetaddr != b->ingress[i].cidrNetaddr) ||
                        (a->ingress[i].cidrSlashnet != b->ingress[i].cidrSlashnet) ||
                        (a->ingress[i].protocol != b->ingress[i].protocol) ||
                        (a->ingress[i].fromPort != b->ingress[i].fromPort) ||
                        (a->ingress[i].toPort != b->ingress[i].toPort) ||
                        (a->ingress[i].icmpCode != b->ingress[i].icmpCode) ||
                        (a->ingress[i].icmpType != b->ingress[i].icmpType)) {
                    diffound = 1;
                }
            }
            if (!diffound) {
                if (ingress_diff) *ingress_diff = 0;
            } else {
                abmatch = 0;
            }
        } else {
            abmatch = 0;
        }
        if (a->max_egress == b->max_egress) {
            int diffound = 0;
            for (int i = 0; i < a->max_egress && !diffound; i++) {
                if ((a->egress[i].number != b->egress[i].number) ||
                        (a->egress[i].allow != b->egress[i].allow) ||
                        (a->egress[i].cidrNetaddr != b->egress[i].cidrNetaddr) ||
                        (a->egress[i].cidrSlashnet != b->egress[i].cidrSlashnet) ||
                        (a->egress[i].protocol != b->egress[i].protocol) ||
                        (a->egress[i].fromPort != b->egress[i].fromPort) ||
                        (a->egress[i].toPort != b->egress[i].toPort) ||
                        (a->egress[i].icmpCode != b->egress[i].icmpCode) ||
                        (a->egress[i].icmpType != b->egress[i].icmpType)) {
                    diffound = 1;
                }
            }
            if (!diffound) {
                if (egress_diff) *egress_diff = 0;
            } else {
                abmatch = 0;
            }
        } else {
            abmatch = 0;
        }
    }

    if (abmatch) {
        return (0);
    }
    return (1);
}

/**
 * Compares two gni_instance structures a and b. a and b are assumed to represent
 * VPC mode interfaces.
 *
 * @param a [in] gni_instance structure of interest.
 * @param b [in] gni_instance structure of interest.
 * @param pubip_diff [out] set to 1 iff public IP of a and b differ.
 * @param sdc_diff [out] set to 1 iff src/dst check flag of a and b differ.
 * @param host_diff [out] set to 1 iff host/node of a and b differ.
 * @param sg_diff [out] set to 1 iff list of security group names of a and b differ.
 * @return 0 if name and other properties of a and b match. Non-zero otherwise.
 */
int cmp_gni_interface(gni_instance *a, gni_instance *b, int *pubip_diff, int *sdc_diff, int *host_diff, int *sg_diff) {
    int abmatch = 1;
    int sgmatch = 1;
    if (a == b) {
        if (pubip_diff) *pubip_diff = 0;
        if (sdc_diff) *sdc_diff = 0;
        if (host_diff) *host_diff = 0;
        if (sg_diff) *sg_diff = 0;
        return (0);
    }

    if (pubip_diff) *pubip_diff = 1;
    if (sdc_diff) *sdc_diff = 1;
    if (host_diff) *host_diff = 1;
    if (sg_diff) *sg_diff = 1;

    if ((a == NULL) || (b == NULL)) {
        return (1);
    }
    if (strcmp(a->name, b->name)) {
        abmatch = 0;
    } else {
        if (a->srcdstcheck == b->srcdstcheck) {
            if (sdc_diff) *sdc_diff = 0;
        } else {
            abmatch = 0;
        }
        if (a->publicIp == b->publicIp) {
            if (pubip_diff) *pubip_diff = 0;
        } else {
            abmatch = 0;
        }
        if (!strcmp(a->node, b->node)) {
            if (host_diff) *host_diff = 0;
        } else {
            abmatch = 0;
        }
        if (a->max_secgroup_names != b->max_secgroup_names) {
            abmatch = 0;
            sgmatch = 0;
        } else {
            for (int i = 0; i < a->max_secgroup_names; i++) {
                if (strcmp(a->secgroup_names[i].name, b->secgroup_names[i].name)) {
                    abmatch = 0;
                    sgmatch = 0;
                    break;
                }
            }
        }
    }

    if (sg_diff && (sgmatch == 1)) {
        *sg_diff = 0;
    }
    if (abmatch) {
        return (0);
    }
    return (1);
}

/**
 * Compares gni_instance structures a and b.
 *
 * @param a [in] gni_instance structure of interest.
 * @param b [in] gni_instance structure of interest.
 * @return 0 if name and other properties of a and b match. Non-zero otherwise.
 */
int cmp_gni_instance(gni_instance *a, gni_instance *b) {
    int abmatch = 1;
    if (a == b) {
        return (0);
    }
    if ((a == NULL) || (b == NULL)) {
        return (1);
    }
    if (strcmp(a->name, b->name)) {
        abmatch = 0;
    } else {
        if (a->publicIp != b->publicIp) {
            abmatch = 0;
        }
        if (strcmp(a->node, b->node)) {
            abmatch = 0;
        }
        if (a->max_secgroup_names != b->max_secgroup_names) {
            abmatch = 0;
        } else {
            for (int i = 0; i < a->max_secgroup_names; i++) {
                if (strcmp(a->secgroup_names[i].name, b->secgroup_names[i].name)) {
                    abmatch = 0;
                    break;
                }
            }
        }
    }

    if (abmatch) {
        return (0);
    }
    return (1);
}

/**
 * Compares gni_mido_gateway structures a and b.
 *
 * @param a [in] gni_instance structure of interest.
 * @param b [in] gni_instance structure of interest.
 * @return 0 if name and other properties of a and b match. Non-zero otherwise.
 */
int cmp_gni_mido_gateway(gni_mido_gateway *a, gni_mido_gateway *b) {
    int abmatch = 1;
    if (a == b) {
        return (0);
    }
    if ((a == NULL) || (b == NULL)) {
        return (1);
    }
    if (strcmp(a->host, b->host)) {
        abmatch = 0;
    } else {
        if (a->asn != b->asn) {
            abmatch = 0;
        }
        if (a->peer_asn != b->peer_asn) {
            abmatch = 0;
        }
        if (strcmp(a->ext_ip, b->ext_ip)) {
            abmatch = 0;
        }
        if (strcmp(a->ext_dev, b->ext_dev)) {
            abmatch = 0;
        }
        if (strcmp(a->ext_cidr, b->ext_cidr)) {
            abmatch = 0;
        }
        if (strcmp(a->peer_ip, b->peer_ip)) {
            abmatch = 0;
        }
        if (a->max_ad_routes != b->max_ad_routes) {
            abmatch = 0;
        } else {
            for (int i = 0; i < a->max_ad_routes; i++) {
                if (strcmp(a->ad_routes[i], b->ad_routes[i])) {
                    abmatch = 0;
                    break;
                }
            }
        }
    }

    if (abmatch) {
        return (0);
    }
    return (1);
}

/**
 * Comparator function for gni_instance structures. Comparison is base on name property.
 * @param p1 [in] pointer to gni_instance pointer 1.
 * @param p2 [in] pointer to gni_instance pointer 2.
 * @return 0 iff p1->.->name == p2->.->name. -1 iff p1->.->name < p2->.->name.
 * 1 iff p1->.->name > p2->.->name.
 * NULL is considered larger than a non-NULL string.
 */
int compare_gni_instance_name(const void *p1, const void *p2) {
    gni_instance **pp1 = NULL;
    gni_instance **pp2 = NULL;
    gni_instance *e1 = NULL;
    gni_instance *e2 = NULL;
    char *name1 = NULL;
    char *name2 = NULL;

    if ((p1 == NULL) || (p2 == NULL)) {
        LOGWARN("Invalid argument: cannot compare NULL gni_instance\n");
        return (0);
    }
    pp1 = (gni_instance **) p1;
    pp2 = (gni_instance **) p2;
    e1 = *pp1;
    e2 = *pp2;
    if (e1 && strlen(e1->name)) {
        name1 = e1->name;
    }
    if (e2 && strlen(e2->name)) {
        name2 = e2->name;
    }
    if (name1 == name2) {
        return (0);
    }
    if (name1 == NULL) {
        return (1);
    }
    if (name2 == NULL) {
        return (-1);
    }
    return (strcmp(name1, name2));
}

