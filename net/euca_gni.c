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
#include "euca_lni.h"
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

//! Static prototypes
static int map_proto_to_names(int proto_number, char *out_proto_name, int out_proto_len);

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

//!
//! Creates a unique IP table chain name for a given security group. This name, if successful
//! will have the form of EU_[hash] where [hash] is the 64 bit encoding of the resulting
//! "[account id]-[group name]" string from the given security group information.
//!
//! @param[in] gni a pointer to the global network information structure
//! @param[in] secgroup a pointer to the security group for which we compute the chain name
//! @param[out] outchainname a pointer to the string that will contain the computed name
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!     The outchainname parameter Must not be NULL but it should point to a NULL value. If
//!     this does not point to NULL, the memory will be lost when replaced with the out value.
//!
//! @post
//!     On success, the outchainname will point to the resulting string. If a failure occur, any
//!     value pointed by outchainname is non-deterministic.
//!
//! @note
//!
int gni_secgroup_get_chainname(globalNetworkInfo * gni, gni_secgroup * secgroup, char **outchainname)
{
    char hashtok[16 + 128 + 1];
    char chainname[48];
    char *chainhash = NULL;

    if (!gni || !secgroup || !outchainname) {
        LOGERROR("invalid input\n");
        return (1);
    }

    snprintf(hashtok, 16 + 128 + 1, "%s-%s", secgroup->accountId, secgroup->name);
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

//!
//! Searches and returns a pointer to the route table data structure given its name in the argument..
//!
//! @param[in] vpc a pointer to the vpc gni data structure
//! @param[in] tableName name of the route table of interest
//!
//! @return pointer to the gni route table of interest if found. NULL otherwise
//!
//! @see
//!
//! @pre
//!     gni data structure is assumed to be populated.
//!
//! @post
//!
//! @note
//!
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

//!
//! Searches and returns a pointer to the VPC subnet data structure given its name in the argument.
//!
//! @param[in] vpc a pointer to the vpc gni data structure
//! @param[in] vpcsubnetName name of the subnet of interest
//!
//! @return pointer to the gni vpcsubnet of interest if found. NULL otherwise
//!
//! @see
//!
//! @pre
//!     gni data structure is assumed to be populated.
//!
//! @post
//!
//! @note
//!
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

//!
//! Searches and returns an array of pointers to gni_instance data structures (holding
//! interface information) that are associated with the given VPC.
//!
//! @param[in]  gni a pointer to the global network information structure
//! @param[in]  vpc a pointer to the vpc gni data structure of interest
//! @param[out] out_interfaces a list of pointers to interfaces of interest
//! @param[out] max_out_interfaces number of interfaces found
//!
//! @return 0 if the search is successfully executed - 0 interfaces found is still
//!         a successful search. 1 otherwise. 
//!
//! @see
//!
//! @pre
//!     gni data structure is assumed to be populated.
//!     out_interfaces should be free of memory allocations.
//!
//! @post
//!     memory allocated to hold the resulting list of interfaces should be released
//!     by the caller.
//!
//! @note
//!
int gni_vpc_get_interfaces(globalNetworkInfo *gni, gni_vpc *vpc, gni_instance ***out_interfaces, int *max_out_interfaces) {
    gni_instance **result = NULL;
    int max_result = 0;
    int i = 0;
    
    if (!gni || !vpc || !out_interfaces || !max_out_interfaces) {
        LOGWARN("Invalid argument: NULL pointer - failed to get vpc interfaces.\n");
        return (1);
    }
    LOGTRACE("Searching VPC interfaces.\n");
    for (i = 0; i < gni->max_interfaces; i++) {
        if (strcmp(gni->interfaces[i].vpc, vpc->name)) {
            LOGTRACE("%s in %s: N\n", gni->interfaces[i].name, vpc->name);
        } else {
            LOGTRACE("%s in %s: Y\n", gni->interfaces[i].name, vpc->name);
            result = EUCA_REALLOC(result, max_result + 1, sizeof (gni_instance *));
            result[max_result] = &(gni->interfaces[i]);
            max_result++;
        }
    }
    *out_interfaces = result;
    *max_out_interfaces = max_result;
    return (0);
}

//!
//! Searches and returns an array of pointers to gni_instance data structures (holding
//! interface information) that are associated with the given VPC subnet.
//!
//! @param[in]  gni a pointer to the global network information structure
//! @param[in]  vpcsubnet a pointer to the vpcsubnet gni data structure of interest
//! @param[in]  vpcinterfaces a list of pointers to interfaces to search
//! @param[in]  max_vpcinterfaces number of interfaces found
//! @param[out] out_interfaces a list of pointers to interfaces of interest
//! @param[out] max_out_interfaces number of interfaces found
//!
//! @return 0 if the search is successfully executed - 0 interfaces found is still
//!         a successful search. 1 otherwise. 
//!
//! @see
//!
//! @pre
//!     gni data structure is assumed to be populated.
//!     out_interfaces should be free of memory allocations.
//!
//! @post
//!     memory allocated to hold the resulting list of interfaces should be released
//!     by the caller.
//!
//! @note
//!
int gni_vpcsubnet_get_interfaces(globalNetworkInfo *gni, gni_vpcsubnet *vpcsubnet,
        gni_instance **vpcinterfaces, int max_vpcinterfaces, gni_instance ***out_interfaces, int *max_out_interfaces) {
    gni_instance **result = NULL;
    int max_result = 0;
    int i = 0;
    
    if (!gni || !vpcsubnet || !vpcinterfaces || !out_interfaces || !max_out_interfaces) {
        if (max_vpcinterfaces == 0) {
            return (0);
        } 
        LOGWARN("Invalid argument: NULL pointer - failed to get subnet interfaces.\n");
        return (1);
    }
    
    LOGTRACE("Searching VPC subnet interfaces.\n");
    for (i = 0; i < max_vpcinterfaces; i++) {
        if (strcmp(vpcinterfaces[i]->subnet, vpcsubnet->name)) {
            LOGTRACE("%s in %s: N\n", vpcinterfaces[i]->name, vpcsubnet->name);
        } else {
            LOGTRACE("%s in %s: Y\n", vpcinterfaces[i]->name, vpcsubnet->name);
            result = EUCA_REALLOC(result, max_result + 1, sizeof (gni_instance *));
            result[max_result] = vpcinterfaces[i];
            max_result++;
        }
    }
    *out_interfaces = result;
    *max_out_interfaces = max_result;
    return (0);
}

//!
//! Looks up for the cluster for which we are assigned within a configured cluster list. We can
//! be the cluster itself or one of its node.
//!
//! @param[in] gni a pointer to the global network information structure
//! @param[out] outclusterptr a pointer to the associated cluster structure pointer
//!
//! @return 0 if a matching cluster structure is found or 1 if not found or a failure occured
//!
//! @see gni_is_self()
//!
//! @pre
//!
//! @post
//!     On success the value pointed by outclusterptr is valid. On failure, this value
//!     is non-deterministic.
//!
//! @note
//!
int gni_find_self_cluster(globalNetworkInfo * gni, gni_cluster ** outclusterptr)
{
    int i, j;
    char *strptra = NULL;

    if (!gni || !outclusterptr) {
        LOGERROR("invalid input\n");
        return (1);
    }

    *outclusterptr = NULL;

    for (i = 0; i < gni->max_clusters; i++) {
        // check to see if local host is the enabled cluster controller
        strptra = hex2dot(gni->clusters[i].enabledCCIp);
        if (strptra) {
            if (!gni_is_self(strptra)) {
                EUCA_FREE(strptra);
                *outclusterptr = &(gni->clusters[i]);
                return (0);
            }
            EUCA_FREE(strptra);
        }
        // otherwise, check to see if local host is a node in the cluster
        for (j = 0; j < gni->clusters[i].max_nodes; j++) {
            //      if (!strcmp(gni->clusters[i].nodes[j].name, outnodeptr->name)) {
            if (!gni_is_self(gni->clusters[i].nodes[j].name)) {
                *outclusterptr = &(gni->clusters[i]);
                return (0);
            }
        }
    }
    return (1);
}

//!
//! Looks up for the cluster for which we are assigned within a configured cluster list. We can
//! be the cluster itself or one of its node.
//!
//! @param[in] gni a pointer to the global network information structure
//! @param[in] psGroupId a pointer to a constant string containing the Security-Group ID we're looking for
//! @param[out] pSecGroup a pointer to the associated security group structure pointer
//!
//! @return 0 if a matching security group structure is found or 1 if not found or a failure occured
//!
//! @see
//!
//! @pre
//!     All the provided parameter must be valid and non-NULL.
//!
//! @post
//!     On success the value pointed by pSecGroup is valid. On failure, this value
//!     is non-deterministic.
//!
//! @note
//!
int gni_find_secgroup(globalNetworkInfo * gni, const char *psGroupId, gni_secgroup ** pSecGroup)
{
    int i = 0;

    if (!gni || !psGroupId || !pSecGroup) {
        LOGERROR("invalid input\n");
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

int gni_find_instance(globalNetworkInfo * gni, const char *psInstanceId, gni_instance ** pInstance) {
    int i = 0;

    if (!gni || !psInstanceId || !pInstance) {
        LOGERROR("invalid input\n");
        return (1);
    }
    // Initialize to NULL
    (*pInstance) = NULL;

    LOGDEBUG("attempting search for instance id %s in gni\n", psInstanceId);

    // Go through our instance list and look for that instance
    for (i = 0; i < gni->max_instances; i++) {
        LOGDEBUG("attempting match between %s and %s\n", psInstanceId, gni->instances[i].name);
        if (!strcmp(psInstanceId, gni->instances[i].name)) {
            (*pInstance) = &(gni->instances[i]);
            return (0);
        }
    }

    return(1);
}

//!
//! Searches through the list of network interfaces in the gni returns all non-primary interfaces for a given instance
//!
//! @param[in] gni a pointer to the global network information structure
//! @param[in] psInstanceId a pointer to instance ID identifier (i-XXXXXXX)
//! @param[out] pAInstances an array of network interface pointers
//! @param[out] size a pointer to the size of the array
//!
//! @return 0 if lookup is successful or 1 if a failure occurred
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_find_secondary_interfaces(globalNetworkInfo * gni, const char *psInstanceId, gni_instance * pAInstances[], int *size) {
    if (!size || !gni || !psInstanceId || !sizeof(pAInstances)) {
        LOGERROR("invalid input\n");
        return EUCA_ERROR;
    }

    *size = 0;

    LOGDEBUG("attempting search for interfaces for instance id %s in gni\n", psInstanceId);

    for (int i=0; i < gni->max_interfaces; i++) {
        LOGDEBUG("attempting match between %s and %s\n", psInstanceId, gni->interfaces[i].instance_name.name);
        if (!strcmp(gni->interfaces[i].instance_name.name, psInstanceId) && 
            strcmp(gni->interfaces[i].name, psInstanceId)) {
            pAInstances[*size] = &(gni->interfaces[i]);
            (*size)++;
        }
    }

    return EUCA_OK;
}

//!
//! Looks up through a list of configured node for the one that is associated with
//! this currently running instance.
//!
//! @param[in]  gni a pointer to the global network information structure
//! @param[out] outnodeptr a pointer to the associated node structure pointer
//!
//! @return 0 if a matching node structure is found or 1 if not found or a failure occured
//!
//! @see gni_is_self()
//!
//! @pre
//!
//! @post
//!     On success the value pointed by outnodeptr is valid. On failure, this value
//!     is non-deterministic.
//!
//! @note
//!
int gni_find_self_node(globalNetworkInfo * gni, gni_node ** outnodeptr)
{
    int i, j;

    if (!gni || !outnodeptr) {
        LOGERROR("invalid input\n");
        return (1);
    }

    *outnodeptr = NULL;

    for (i = 0; i < gni->max_clusters; i++) {
        for (j = 0; j < gni->clusters[i].max_nodes; j++) {
            if (!gni_is_self(gni->clusters[i].nodes[j].name)) {
                *outnodeptr = &(gni->clusters[i].nodes[j]);
                return (0);
            }
        }
    }

    return (1);
}

//!
//! Validates if the given test_ip is a local IP address on this system
//!
//! @param[in] test_ip a string containing the IP to validate
//!
//! @return 0 if the test_ip is a local IP or 1 on failure or if not found locally
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_is_self(const char *test_ip)
{
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

//!
//! TODO: Function description.
//!
//! @param[in]  gni a pointer to the global network information structure
//! @param[in]  cluster_names
//! @param[in]  max_cluster_names
//! @param[out] out_cluster_names
//! @param[out] out_max_cluster_names
//! @param[out] out_clusters
//! @param[out] out_max_clusters
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_cloud_get_clusters(globalNetworkInfo * gni, char **cluster_names, int max_cluster_names, char ***out_cluster_names, int *out_max_cluster_names, gni_cluster ** out_clusters,
                           int *out_max_clusters)
{
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
        LOGDEBUG("nothing to do, both output variables are NULL\n");
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
            *out_cluster_names = EUCA_ZALLOC(gni->max_clusters, sizeof(char *));
        if (do_outstructs)
            *out_clusters = EUCA_ZALLOC(gni->max_clusters, sizeof(gni_cluster));
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
                memcpy(&(ret_clusters[i]), &(gni->clusters[i]), sizeof(gni_cluster));
            retcount++;
        } else {
            for (j = 0; j < max_cluster_names; j++) {
                if (!strcmp(cluster_names[j], gni->clusters[i].name)) {
                    if (do_outnames) {
                        *out_cluster_names = realloc(*out_cluster_names, sizeof(char *) * (retcount + 1));
                        ret_cluster_names = *out_cluster_names;
                        ret_cluster_names[retcount] = strdup(gni->clusters[i].name);
                    }
                    if (do_outstructs) {
                        *out_clusters = realloc(*out_clusters, sizeof(gni_cluster) * (retcount + 1));
                        ret_clusters = *out_clusters;
                        memcpy(&(ret_clusters[retcount]), &(gni->clusters[i]), sizeof(gni_cluster));
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

//!
//! Retrives the list of security groups configured under a cloud
//!
//! @param[in]  pGni a pointer to our global network view structure
//! @param[in]  psSecGroupNames a string pointer to the name of groups we're looking for
//! @param[in]  nbSecGroupNames the number of groups in the psSecGroupNames list
//! @param[out] psOutSecGroupNames a string pointer that will contain the list of group names we found (if non NULL)
//! @param[out] pOutNbSecGroupNames a pointer to the number of groups that matched in the psOutSecGroupNames list
//! @param[out] pOutSecGroups a pointer to the list of security group structures that match what we're looking for
//! @param[out] pOutNbSecGroups a pointer to the number of structures in the psOutSecGroups list
//!
//! @return 0 on success or 1 if any failure occured
//!
//! @see
//!
//! @pre  TODO:
//!
//! @post TODO:
//!
//! @note
//!
int gni_cloud_get_secgroups(globalNetworkInfo * pGni, char **psSecGroupNames, int nbSecGroupNames, char ***psOutSecGroupNames, int *pOutNbSecGroupNames,
                            gni_secgroup ** pOutSecGroups, int *pOutNbSecGroups)
{
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
        LOGDEBUG("nothing to do, both output variables are NULL\n");
        return (0);
    }
    // Do we have any groups?
    if (pGni->max_secgroups == 0)
        return (0);

    // If we do it all, allocate the memory now
    if (psSecGroupNames == NULL || !strcmp(psSecGroupNames[0], "*")) {
        getAll = TRUE;
        if (doOutNames)
            *psOutSecGroupNames = EUCA_ZALLOC(pGni->max_secgroups, sizeof(char *));
        if (doOutStructs)
            *pOutSecGroups = EUCA_ZALLOC(pGni->max_secgroups, sizeof(gni_secgroup));
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
                memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof(gni_secgroup));
            retCount++;
        } else {
            if (!strcmp(psSecGroupNames[x], pGni->secgroups[i].name)) {
                if (doOutNames) {
                    *psOutSecGroupNames = EUCA_REALLOC(*psOutSecGroupNames, (retCount + 1), sizeof(char *));
                    psRetSecGroupNames = *psOutSecGroupNames;
                    psRetSecGroupNames[retCount] = strdup(pGni->secgroups[i].name);
                }

                if (doOutStructs) {
                    *pOutSecGroups = EUCA_REALLOC(*pOutSecGroups, (retCount + 1), sizeof(gni_instance));
                    pRetSecGroup = *pOutSecGroups;
                    memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof(gni_secgroup));
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

//!
//! TODO: Function description.
//!
//! @param[in]  gni a pointer to the global network information structure
//! @param[in]  cluster
//! @param[in]  node_names
//! @param[in]  max_node_names
//! @param[out] out_node_names
//! @param[out] out_max_node_names
//! @param[out] out_nodes
//! @param[out] out_max_nodes
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_cluster_get_nodes(globalNetworkInfo * gni, gni_cluster * cluster, char **node_names, int max_node_names, char ***out_node_names, int *out_max_node_names,
                          gni_node ** out_nodes, int *out_max_nodes)
{
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
        LOGDEBUG("nothing to do, both output variables are NULL\n");
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

    cluster_names = EUCA_ZALLOC(1, sizeof(char *));
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
            *out_node_names = EUCA_ZALLOC(cluster->max_nodes, sizeof(char *));
        if (do_outstructs)
            *out_nodes = EUCA_ZALLOC(cluster->max_nodes, sizeof(gni_node));
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
                memcpy(&(ret_nodes[i]), &(out_clusters[0].nodes[i]), sizeof(gni_node));

            retcount++;
        } else {
            for (j = 0; j < max_node_names; j++) {
                if (!strcmp(node_names[j], out_clusters[0].nodes[i].name)) {
                    if (do_outnames) {
                        *out_node_names = realloc(*out_node_names, sizeof(char *) * (retcount + 1));
                        ret_node_names = *out_node_names;
                        ret_node_names[retcount] = strdup(out_clusters[0].nodes[i].name);
                    }
                    if (do_outstructs) {
                        *out_nodes = realloc(*out_nodes, sizeof(gni_node) * (retcount + 1));
                        ret_nodes = *out_nodes;
                        memcpy(&(ret_nodes[retcount]), &(out_clusters[0].nodes[i]), sizeof(gni_node));
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

//!
//! TODO: Function description.
//!
//! @param[in]  pGni a pointer to the global network information structure
//! @param[in]  pCluster
//! @param[in]  psInstanceNames
//! @param[in]  nbInstanceNames
//! @param[out] psOutInstanceNames
//! @param[out] pOutNbInstanceNames
//! @param[out] pOutInstances
//! @param[out] pOutNbInstances
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_cluster_get_instances(globalNetworkInfo * pGni, gni_cluster * pCluster, char **psInstanceNames, int nbInstanceNames,
                              char ***psOutInstanceNames, int *pOutNbInstanceNames, gni_instance ** pOutInstances, int *pOutNbInstances)
{
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
        LOGDEBUG("nothing to do, both output variables are NULL\n");
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
            *psOutInstanceNames = EUCA_ZALLOC(nbInstances, sizeof(char *));
        if (doOutStructs)
            *pOutInstances = EUCA_ZALLOC(nbInstances, sizeof(gni_instance));
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
                        if (!strcmp(pGni->instances[x].name, pCluster->nodes[i].instance_names[k].name)) {
                            memcpy(&(pRetInstances[retCount]), &(pGni->instances[x]), sizeof(gni_instance));
                            break;
                        }
                    }
                }
                retCount++;
            } else {
                for (x = 0; x < nbInstanceNames; x++) {
                    if (!strcmp(psInstanceNames[x], pCluster->nodes[i].instance_names[k].name)) {
                        if (doOutNames) {
                            *psOutInstanceNames = EUCA_REALLOC(*psOutInstanceNames, (retCount + 1), sizeof(char *));
                            psRetInstanceNames = *psOutInstanceNames;
                            psRetInstanceNames[retCount] = strdup(pCluster->nodes[i].instance_names[k].name);
                        }

                        if (doOutStructs) {
                            for (y = 0; y < pGni->max_instances; y++) {
                                if (!strcmp(pGni->instances[y].name, pCluster->nodes[i].instance_names[k].name)) {
                                    *pOutInstances = EUCA_REALLOC(*pOutInstances, (retCount + 1), sizeof(gni_instance));
                                    pRetInstances = *pOutInstances;
                                    memcpy(&(pRetInstances[retCount]), &(pGni->instances[y]), sizeof(gni_instance));
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

//!
//! Retrives the list of security groups configured and active on a given cluster
//!
//! @param[in]  pGni a pointer to our global network view structure
//! @param[in]  pCluster a pointer to the cluster we're building the security group list for
//! @param[in]  psSecGroupNames a string pointer to the name of groups we're looking for
//! @param[in]  nbSecGroupNames the number of groups in the psSecGroupNames list
//! @param[out] psOutSecGroupNames a string pointer that will contain the list of group names we found (if non NULL)
//! @param[out] pOutNbSecGroupNames a pointer to the number of groups that matched in the psOutSecGroupNames list
//! @param[out] pOutSecGroups a pointer to the list of security group structures that match what we're looking for
//! @param[out] pOutNbSecGroups a pointer to the number of structures in the psOutSecGroups list
//!
//! @return 0 on success or 1 if any failure occured
//!
//! @see
//!
//! @pre  TODO:
//!
//! @post TODO:
//!
//! @note
//!
int gni_cluster_get_secgroup(globalNetworkInfo * pGni, gni_cluster * pCluster, char **psSecGroupNames, int nbSecGroupNames, char ***psOutSecGroupNames, int *pOutNbSecGroupNames,
                             gni_secgroup ** pOutSecGroups, int *pOutNbSecGroups)
{
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
        LOGDEBUG("nothing to do, both output variables are NULL\n");
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
    // Allocate memory for all the groups if there is no search criterias
    if ((psSecGroupNames == NULL) || !strcmp(psSecGroupNames[0], "*")) {
        getAll = TRUE;
        if (doOutNames)
            *psOutSecGroupNames = EUCA_ZALLOC(pGni->max_secgroups, sizeof(char *));

        if (doOutStructs)
            *pOutSecGroups = EUCA_ZALLOC(pGni->max_secgroups, sizeof(gni_secgroup));
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
                    memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof(gni_secgroup));
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
                        *psOutSecGroupNames = EUCA_REALLOC(*psOutSecGroupNames, (retCount + 1), sizeof(char *));
                        psRetSecGroupNames = *psOutSecGroupNames;
                        psRetSecGroupNames[retCount] = strdup(pGni->secgroups[i].name);
                    }

                    if (doOutStructs) {
                        *pOutSecGroups = EUCA_REALLOC(*pOutSecGroups, (retCount + 1), sizeof(gni_instance));
                        pRetSecGroup = *pOutSecGroups;
                        memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof(gni_secgroup));
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

//!
//! TODO: Function description.
//!
//! @param[in]  gni a pointer to the global network information structure
//! @param[in]  node
//! @param[in]  instance_names
//! @param[in]  max_instance_names
//! @param[out] out_instance_names
//! @param[out] out_max_instance_names
//! @param[out] out_instances
//! @param[out] out_max_instances
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_node_get_instances(globalNetworkInfo * gni, gni_node * node, char **instance_names, int max_instance_names, char ***out_instance_names, int *out_max_instance_names,
                           gni_instance ** out_instances, int *out_max_instances)
{
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
        LOGDEBUG("nothing to do, both output variables are NULL\n");
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
            *out_instance_names = EUCA_ZALLOC(node->max_instance_names, sizeof(char *));
        if (do_outstructs)
            *out_instances = EUCA_ZALLOC(node->max_instance_names, sizeof(gni_instance));
    }

    if (do_outnames)
        ret_instance_names = *out_instance_names;
    if (do_outstructs)
        ret_instances = *out_instances;

    retcount = 0;
    for (i = 0; i < node->max_instance_names; i++) {
        if (getall) {
            if (do_outnames)
                ret_instance_names[i] = strdup(node->instance_names[i].name);
            if (do_outstructs) {
                for (k = 0; k < gni->max_instances; k++) {
                    if (!strcmp(gni->instances[k].name, node->instance_names[i].name)) {
                        memcpy(&(ret_instances[i]), &(gni->instances[k]), sizeof(gni_instance));
                        break;
                    }
                }
            }
            retcount++;
        } else {
            for (j = 0; j < max_instance_names; j++) {
                if (!strcmp(instance_names[j], node->instance_names[i].name)) {
                    if (do_outnames) {
                        *out_instance_names = realloc(*out_instance_names, sizeof(char *) * (retcount + 1));
                        ret_instance_names = *out_instance_names;
                        ret_instance_names[retcount] = strdup(node->instance_names[i].name);
                    }
                    if (do_outstructs) {
                        for (k = 0; k < gni->max_instances; k++) {
                            if (!strcmp(gni->instances[k].name, node->instance_names[i].name)) {
                                *out_instances = realloc(*out_instances, sizeof(gni_instance) * (retcount + 1));
                                ret_instances = *out_instances;
                                memcpy(&(ret_instances[retcount]), &(gni->instances[k]), sizeof(gni_instance));
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

//!
//! Retrives the list of security groups configured and active on a given cluster
//!
//! @param[in]  pGni a pointer to our global network view structure
//! @param[in]  pNode a pointer to the node we're building the security group list for
//! @param[in]  psSecGroupNames a string pointer to the name of groups we're looking for
//! @param[in]  nbSecGroupNames the number of groups in the psSecGroupNames list
//! @param[out] psOutSecGroupNames a string pointer that will contain the list of group names we found (if non NULL)
//! @param[out] pOutNbSecGroupNames a pointer to the number of groups that matched in the psOutSecGroupNames list
//! @param[out] pOutSecGroups a pointer to the list of security group structures that match what we're looking for
//! @param[out] pOutNbSecGroups a pointer to the number of structures in the psOutSecGroups list
//!
//! @return 0 on success or 1 if any failure occured
//!
//! @see
//!
//! @pre  TODO:
//!
//! @post TODO:
//!
//! @note
//!
int gni_node_get_secgroup(globalNetworkInfo * pGni, gni_node * pNode, char **psSecGroupNames, int nbSecGroupNames, char ***psOutSecGroupNames, int *pOutNbSecGroupNames,
                          gni_secgroup ** pOutSecGroups, int *pOutNbSecGroups)
{
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
        LOGDEBUG("nothing to do, both output variables are NULL\n");
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
    // Allocate memory for all the groups if there is no search criterias
    if ((psSecGroupNames == NULL) || !strcmp(psSecGroupNames[0], "*")) {
        getAll = TRUE;
        if (doOutNames)
            *psOutSecGroupNames = EUCA_ZALLOC(pGni->max_secgroups, sizeof(char *));

        if (doOutStructs)
            *pOutSecGroups = EUCA_ZALLOC(pGni->max_secgroups, sizeof(gni_secgroup));
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
                    memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof(gni_secgroup));
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
                        *psOutSecGroupNames = EUCA_REALLOC(*psOutSecGroupNames, (retCount + 1), sizeof(char *));
                        psRetSecGroupNames = *psOutSecGroupNames;
                        psRetSecGroupNames[retCount] = strdup(pGni->secgroups[i].name);
                    }

                    if (doOutStructs) {
                        *pOutSecGroups = EUCA_REALLOC(*pOutSecGroups, (retCount + 1), sizeof(gni_instance));
                        pRetSecGroup = *pOutSecGroups;
                        memcpy(&(pRetSecGroup[retCount]), &(pGni->secgroups[i]), sizeof(gni_secgroup));
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

//!
//! TODO: Function description.
//!
//! @param[in]  gni a pointer to the global network information structure
//! @param[in]  instance
//! @param[in]  secgroup_names
//! @param[in]  max_secgroup_names
//! @param[out] out_secgroup_names
//! @param[out] out_max_secgroup_names
//! @param[out] out_secgroups
//! @param[out] out_max_secgroups
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_instance_get_secgroups(globalNetworkInfo * gni, gni_instance * instance, char **secgroup_names, int max_secgroup_names, char ***out_secgroup_names,
                               int *out_max_secgroup_names, gni_secgroup ** out_secgroups, int *out_max_secgroups)
{
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
        LOGDEBUG("nothing to do, both output variables are NULL\n");
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
            *out_secgroup_names = EUCA_ZALLOC(instance->max_secgroup_names, sizeof(char *));
        if (do_outstructs)
            *out_secgroups = EUCA_ZALLOC(instance->max_secgroup_names, sizeof(gni_secgroup));
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
                        memcpy(&(ret_secgroups[i]), &(gni->secgroups[k]), sizeof(gni_secgroup));
                        break;
                    }
                }
            }
            retcount++;
        } else {
            for (j = 0; j < max_secgroup_names; j++) {
                if (!strcmp(secgroup_names[j], instance->secgroup_names[i].name)) {
                    if (do_outnames) {
                        *out_secgroup_names = realloc(*out_secgroup_names, sizeof(char *) * (retcount + 1));
                        ret_secgroup_names = *out_secgroup_names;
                        ret_secgroup_names[retcount] = strdup(instance->secgroup_names[i].name);
                    }
                    if (do_outstructs) {
                        for (k = 0; k < gni->max_secgroups; k++) {
                            if (!strcmp(gni->secgroups[k].name, instance->secgroup_names[i].name)) {
                                *out_secgroups = realloc(*out_secgroups, sizeof(gni_secgroup) * (retcount + 1));
                                ret_secgroups = *out_secgroups;
                                memcpy(&(ret_secgroups[retcount]), &(gni->secgroups[k]), sizeof(gni_secgroup));
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

//!
//! TODO: Function description.
//!
//! @param[in]  gni a pointer to the global network information structure
//! @param[in]  secgroup
//! @param[in]  instance_names
//! @param[in]  max_instance_names
//! @param[out] out_instance_names
//! @param[out] out_max_instance_names
//! @param[out] out_instances
//! @param[out] out_max_instances
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_secgroup_get_instances(globalNetworkInfo * gni, gni_secgroup * secgroup, char **instance_names, int max_instance_names, char ***out_instance_names,
                               int *out_max_instance_names, gni_instance ** out_instances, int *out_max_instances)
{
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

    if (!do_outnames && !do_outstructs) {
        LOGDEBUG("nothing to do, both output variables are NULL\n");
        return (0);
    }
    if (secgroup->max_instances == 0) {
        LOGDEBUG("nothing to do, no instance associated with %s\n", secgroup->name);
        return (0);
    }
    if (do_outnames) {
        *out_instance_names = EUCA_ZALLOC(secgroup->max_instances, sizeof(char *));
        *out_max_instance_names = 0;
        ret_instance_names = *out_instance_names;
    }
    if (do_outstructs) {
        *out_instances = EUCA_ZALLOC(secgroup->max_instances, sizeof(gni_instance));
        *out_max_instances = 0;
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
                memcpy(&(ret_instances[i]), secgroup->instances[i], sizeof(gni_instance));
            }
            retcount++;
        } else {
            for (j = 0; j < max_instance_names; j++) {
                if (!strcmp(instance_names[j], secgroup->instances[i]->name)) {
                    if (do_outnames) {
                        ret_instance_names[retcount] = strdup(secgroup->instances[i]->name);
                    }
                    if (do_outstructs) {
                        memcpy(&(ret_instances[retcount]), secgroup->instances[i], sizeof(gni_instance));
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

//!
//! Retrieve the interfaces that are members of the given security group.
//!
//! @param[in]  gni a pointer to the global network information structure
//! @param[in]  secgroup a pointer to the gni_secgroup structure of the SG of interest
//! @param[in]  interface_names restrict the search to this list of interface names
//! @param[in]  max_interface_names number of interfaces specified
//! @param[out] out_interface_names array of interface names that were found
//! @param[out] out_max_interface_names number of interfaces found
//! @param[out] out_interfaces array of found interface structure pointers
//! @param[out] out_max_interfaces number of found interfaces
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_secgroup_get_interfaces(globalNetworkInfo * gni, gni_secgroup * secgroup,
        char **interface_names, int max_interface_names, char ***out_interface_names,
        int *out_max_interface_names, gni_instance *** out_interfaces, int *out_max_interfaces)
{
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
        LOGDEBUG("nothing to do, both output variables are NULL\n");
        return (0);
    }
    if (secgroup->max_interfaces == 0) {
        LOGDEBUG("nothing to do, no instances/interfaces associated with %s\n", secgroup->name);
        return (0);
    }

    if (do_outnames) {
        *out_interface_names = EUCA_ZALLOC(secgroup->max_interfaces, sizeof (char *));
        if (*out_interface_names == NULL) {
            LOGFATAL("out of memory: failed to allocate out_interface_names\n");
            do_outnames = 0;
        }
        *out_max_interface_names = 0;
    }
    if (do_outstructs) {
        *out_interfaces = EUCA_ZALLOC(secgroup->max_interfaces, sizeof (gni_instance *));
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

//!
//! Function description.
//!
//! @param[in]  ctxptr
//! @param[in]  expression
//! @param[out] results
//! @param[out] max_results
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int evaluate_xpath_property(xmlXPathContextPtr ctxptr, char *expression, char ***results, int *max_results)
{
    int i, max_nodes = 0, result_count = 0;
    xmlXPathObjectPtr objptr;
    char **retresults;

    *max_results = 0;

    objptr = xmlXPathEvalExpression((unsigned char *)expression, ctxptr);
    if (objptr == NULL) {
        LOGERROR("unable to evaluate xpath expression '%s': check network config XML format\n", expression);
        return (1);
    } else {
        if (objptr->nodesetval) {
            max_nodes = (int)objptr->nodesetval->nodeNr;
            *results = EUCA_ZALLOC(max_nodes, sizeof(char *));
            retresults = *results;
            for (i = 0; i < max_nodes; i++) {
                if (objptr->nodesetval->nodeTab[i] && objptr->nodesetval->nodeTab[i]->children && objptr->nodesetval->nodeTab[i]->children->content) {

                    retresults[result_count] = strdup((char *)objptr->nodesetval->nodeTab[i]->children->content);
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
    return (0);
}

//!
//! TODO: Describe
//!
//! @param[in]  ctxptr a pointer to the XML context
//! @param[in]  expression a string pointer to the expression we want to evaluate
//! @param[out] results the results of the expression evaluation
//! @param[out] max_results the number of results returned
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int evaluate_xpath_element(xmlXPathContextPtr ctxptr, char *expression, char ***results, int *max_results)
{
    int i, max_nodes = 0, result_count = 0;
    xmlXPathObjectPtr objptr;
    char **retresults;

    *max_results = 0;

    objptr = xmlXPathEvalExpression((unsigned char *)expression, ctxptr);
    if (objptr == NULL) {
        LOGERROR("unable to evaluate xpath expression '%s': check network config XML format\n", expression);
        return (1);
    } else {
        if (objptr->nodesetval) {
            max_nodes = (int)objptr->nodesetval->nodeNr;
            *results = EUCA_ZALLOC(max_nodes, sizeof(char *));
            retresults = *results;
            for (i = 0; i < max_nodes; i++) {
                if (objptr->nodesetval->nodeTab[i] && objptr->nodesetval->nodeTab[i]->properties && objptr->nodesetval->nodeTab[i]->properties->children
                    && objptr->nodesetval->nodeTab[i]->properties->children->content) {
                    retresults[result_count] = strdup((char *)objptr->nodesetval->nodeTab[i]->properties->children->content);
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
    return (0);
}

/**
 * Allocates and initializes a new globalNetworkInfo structure.
 * @return A pointer to the newly allocated structure or NULL if any failure occurred.
 */
globalNetworkInfo *gni_init() {
    globalNetworkInfo *gni = NULL;
    gni = EUCA_ZALLOC(1, sizeof (globalNetworkInfo));
    if (!gni) {
        LOGFATAL("out of memory - allocating memory for GNI\n");
        exit(1);
    } else {
        //bzero(gni, sizeof(globalNetworkInfo));
        gni->init = 1;
    }
    return (gni);
}

//!
//! Populates a given globalNetworkInfo structure from the content of an XML file
//!
//! @param[in] gni a pointer to the global network information structure
//! @param[in] xmlpath path the XML file use to be used to populate
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_populate(globalNetworkInfo * gni, gni_hostname_info *host_info, char *xmlpath)
{
    int rc = 0;
    xmlDocPtr docptr;
    xmlXPathContextPtr ctxptr;
    struct timeval tv, ttv;

    eucanetd_timer_usec(&ttv);
    eucanetd_timer_usec(&tv);
    if (!gni) {
        LOGERROR("invalid input\n");
        return (1);
    }

    gni_clear(gni);
    LOGTRACE("gni cleared in %ld us.\n", eucanetd_timer_usec(&tv));

    xmlInitParser();
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

    LOGDEBUG("begin parsing XML into data structures\n");

    // GNI version
    rc = gni_populate_gnidata(gni, ctxptr);
    LOGTRACE("gni version populated in %ld us.\n", eucanetd_timer_usec(&tv));
    
    // Instances
    rc = gni_populate_instances(gni, ctxptr);
    LOGTRACE("gni instances populated in %ld us.\n", eucanetd_timer_usec(&tv));

    // Interfaces
    rc = gni_populate_interfaces(gni, ctxptr);
    LOGTRACE("gni interfaces populated in %ld us.\n", eucanetd_timer_usec(&tv));

    // Security Groups
    rc = gni_populate_sgs(gni, ctxptr);
    LOGTRACE("gni sgs populated in %ld us.\n", eucanetd_timer_usec(&tv));

    // VPCs
    rc = gni_populate_vpcs(gni, ctxptr);
    LOGTRACE("gni vpcs populated in %ld us.\n", eucanetd_timer_usec(&tv));

    // Configuration
    rc = gni_populate_configuration(gni, host_info, ctxptr);
    LOGTRACE("gni configuration populated in %ld us.\n", eucanetd_timer_usec(&tv));

    xmlXPathFreeContext(ctxptr);
    xmlFreeDoc(docptr);
    xmlCleanupParser();

    // Populate VPC and subnet interfaces
    for (int i = 0; i < gni->max_vpcs; i++) {
        gni_vpc *vpc = &(gni->vpcs[i]);
        rc = gni_vpc_get_interfaces(gni, vpc, &(vpc->interfaces), &(vpc->max_interfaces));
        if (rc) {
            LOGWARN("Failed to populate gni %s interfaces.\n", vpc->name);
            continue;
        }
        for (int j = 0; j < vpc->max_subnets; j++) {
            gni_vpcsubnet *gnisubnet = &(vpc->subnets[j]);
            rc = gni_vpcsubnet_get_interfaces(gni, gnisubnet, vpc->interfaces, vpc->max_interfaces,
                    &(gnisubnet->interfaces), &(gnisubnet->max_interfaces));
            if (rc) {
                LOGWARN("Failed to populate gni %s interfaces.\n", gnisubnet->name);
            }
        }
    }
    LOGDEBUG("end parsing XML into data structures\n");
    LOGTRACE("gni populate vpc/subnet interfaces %ld us.\n", eucanetd_timer_usec(&tv));

    eucanetd_timer_usec(&tv);
    rc = gni_validate(gni);
    if (rc) {
        LOGDEBUG("could not validate GNI after XML parse: check network config\n");
        return (1);
    }
    LOGINFO("gni validated in %ld us.\n", eucanetd_timer_usec(&tv));

    LOGINFO("gni populated in %.2f ms.\n", eucanetd_timer_usec(&ttv) / 1000.0);
    return (0);
}

//!
//! Populates globalNetworkInfo data from the content of an XML
//! file (xmlXPathContext is expected).
//!
//! @param[in] gni a pointer to the global network information structure
//! @param[in] ctxptr XPATH context to be used to populate
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note 
//!
int gni_populate_gnidata(globalNetworkInfo * gni, xmlXPathContextPtr ctxptr)
{
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i;

    if ((gni == NULL) || (ctxptr == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
    }
    if (gni->init == FALSE) {
        LOGERROR("Invalid argument: gni is not initialized.\n");
        return (1);
    }

    // Eucanetd mode - populate this first
    snprintf(expression, 2048, "/network-data/configuration/property[@name='mode']/value");
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->sMode, NETMODE_LEN, results[i]);
        gni->nmCode = euca_netmode_atoi(gni->sMode);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    // get version and applied version
    snprintf(expression, 2048, "/network-data/@version");
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->version, 32, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "/network-data/@applied-version");
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->appliedVersion, 32, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    return (0);
}

//!
//! Populates globalNetworkInfo instances structure from the content of an XML
//! file (xmlXPathContext is expected). The instances section of globalNetworkInfo
//! structure is expected to be empty/clean.
//!
//! @param[in] gni a pointer to the global network information structure.
//! @param[in] ctxptr XPATH context to be used to populate.
//!
//! @return 0 on success or 1 on failure.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note 
//!
int gni_populate_instances(globalNetworkInfo * gni, xmlXPathContextPtr ctxptr)
{
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i, j;

    if ((gni == NULL) || (ctxptr == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
    }
    if ((gni->init == FALSE) || (gni->max_instances != 0)) {
        LOGERROR("Invalid argument: gni is not initialized or instances section is not empty.\n");
        return (1);
    }

    snprintf(expression, 2048, "/network-data/instances/instance");
    rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
    gni->instances = EUCA_ZALLOC(max_results, sizeof(gni_instance));
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->instances[i].name, INSTANCE_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    gni->max_instances = max_results;
    EUCA_FREE(results);

    for (j = 0; j < gni->max_instances; j++) {
        gni_populate_instance_interface(&(gni->instances[j]), "/network-data/instances/instance", ctxptr);
    }
    return 0;
}

//!
//! Populates globalNetworkInfo interfaces structure from the content of an XML
//! file (xmlXPathContext is expected). The interfaces section of globalNetworkInfo
//! structure is expected to be empty/clean.
//!
//! @param[in] gni a pointer to the global network information structure.
//! @param[in] ctxptr XPATH context to be used to populate.
//!
//! @return 0 on success or 1 on failure.
//!
//! @see
//!
//! @pre Instances section of gni is expected to be pre-populated.
//!
//! @post
//!
//! @note 
//!
int gni_populate_interfaces(globalNetworkInfo *gni, xmlXPathContextPtr ctxptr)
{
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i, j;

    if ((gni == NULL) || (ctxptr == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
    }

    // interfaces is only relevant in VPCMIDO mode
    if (!IS_NETMODE_VPCMIDO(gni)) {
        return (0);
    }
    if ((gni->init == FALSE) || (gni->max_interfaces != 0)) {
        LOGERROR("Invalid argument: gni is not initialized or interfaces section is not empty.\n");
        return (1);
    }
    if (gni->max_instances == 0) {
        return (0);
    }

    for (i = 0; i < gni->max_instances; i++) {
        snprintf(expression, 2048, "/network-data/instances/instance[@name='%s']/networkInterfaces/networkInterface", gni->instances[i].name);
        rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
        if (max_results > 0) {
            gni->interfaces = EUCA_REALLOC(gni->interfaces, gni->max_interfaces + max_results, sizeof (gni_instance));
            if (gni->interfaces == NULL) {
                LOGERROR("out of memory.\n");
                return (1);
            }
            bzero(&(gni->interfaces[gni->max_interfaces]), max_results * sizeof (gni_instance));
            for (j = 0; j < max_results; j++) {
                snprintf(gni->interfaces[gni->max_interfaces + j].name, INTERFACE_ID_LEN, "%s", results[j]);
                snprintf(gni->interfaces[gni->max_interfaces + j].instance_name.name, 1024, gni->instances[i].name);
                gni_populate_instance_interface(&(gni->interfaces[gni->max_interfaces + j]), expression, ctxptr);
                //gni_instance_interface_print(&(gni->interfaces[gni->max_interfaces + j]), EUCA_LOG_INFO);
                EUCA_FREE(results[j]);
            }
            gni->max_interfaces += max_results;
        } else {
            LOGDEBUG("No interfaces for %s in GNI.\n", gni->instances[i].name);
        }
        EUCA_FREE(results);
    }

    return 0;
}

//!
//! Populates globalNetworkInfo security groups structure from the content of an XML
//! file (xmlXPathContext is expected). The security groups section of globalNetworkInfo
//! structure is expected to be empty/clean.
//!
//! @param[in] gni a pointer to the global network information structure
//! @param[in] ctxptr XPATH context to be used to populate
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre globalNetworkInfo instances section is assumed to be populated.
//!
//! @post
//!
//! @note 
//!
int gni_populate_sgs(globalNetworkInfo * gni, xmlXPathContextPtr ctxptr)
{
    int rc=0, found=0, count=0;
    char expression[2048];
    char **results = NULL;
    char *scidrnetaddr = NULL;
    int max_results = 0, i, j, k, l;

    if ((gni == NULL) || (ctxptr == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
        }
    if ((gni->init == FALSE) || (gni->max_secgroups != 0)) {
        LOGERROR("Invalid argument: gni is not initialized or sgs section is not empty.\n");
        return (1);
    }

    snprintf(expression, 2048, "/network-data/securityGroups/securityGroup");
    rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
    gni->secgroups = EUCA_ZALLOC(max_results, sizeof(gni_secgroup));
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->secgroups[i].name, SECURITY_GROUP_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    gni->max_secgroups = max_results;
    EUCA_FREE(results);

    for (j = 0; j < gni->max_secgroups; j++) {
        gni_secgroup *gsg = &(gni->secgroups[j]);
        gni_instance *gi = NULL;
        // populate secgroup's instance_names
        gsg->instances = EUCA_ZALLOC(gni->max_instances, sizeof (gni_instance *));
        gsg->max_instances = 0;
        for (k = 0; k < gni->max_instances; k++) {
            for (l = 0; l < gni->instances[k].max_secgroup_names; l++) {
                gi = &(gni->instances[k]);
                if (!strcmp(gi->secgroup_names[l].name, gsg->name)) {
                    gsg->instances[gsg->max_instances] = gi;
                    gsg->max_instances++;
                }
            }
        }
        // populate secgroup's interface_names
        if (IS_NETMODE_VPCMIDO(gni)) {
            gsg->interfaces = EUCA_ZALLOC(gni->max_interfaces, sizeof (gni_instance *));
            gsg->max_interfaces = 0;
            for (k = 0; k < gni->max_interfaces; k++) {
                gi = &(gni->interfaces[k]);
                for (l = 0; l < gi->max_secgroup_names; l++) {
                    if (!strcmp(gi->secgroup_names[l].name, gsg->name)) {
                        gi->gnisgs[l] = gsg;
                        gsg->interfaces[gsg->max_interfaces] = gi;
                        gsg->max_interfaces++;
                    }
                }
            }
        }

        snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/ownerId", gni->secgroups[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->secgroups[j].accountId, 128, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/rules/value", gni->secgroups[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        gni->secgroups[j].grouprules = EUCA_ZALLOC(max_results, sizeof(gni_name));
        for (i = 0; i < max_results; i++) {
            char newrule[2048];
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            rc = ruleconvert(results[i], newrule);
            if (!rc) {
                snprintf(gni->secgroups[j].grouprules[i].name, 1024, "%s", newrule);
            }
            EUCA_FREE(results[i]);
        }
        gni->secgroups[j].max_grouprules = max_results;
        EUCA_FREE(results);


        //ingress
        found=0;
        count=0;
/*
        while (!found) {
            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/ingressRules/rule[%d]/protocol", gni->secgroups[j].name, count + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            if (!max_results) {
                found = 1;
            } else {
                count++;
            }
            for (i = 0; i < max_results; i++) {
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);
        }
*/

        snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/ingressRules/rule[*]/protocol", gni->secgroups[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);
        count = max_results;

        gni->secgroups[j].ingress_rules = EUCA_ZALLOC(count, sizeof(gni_rule));
        gni->secgroups[j].max_ingress_rules = count;

        for (k = 0; k < gni->secgroups[j].max_ingress_rules; k++) {

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/ingressRules/rule[%d]/protocol", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->secgroups[j].ingress_rules[k].protocol = atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/ingressRules/rule[%d]/groupId", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->secgroups[j].ingress_rules[k].groupId, SECURITY_GROUP_ID_LEN, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/ingressRules/rule[%d]/groupOwnerId", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->secgroups[j].ingress_rules[k].groupOwnerId, 16, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/ingressRules/rule[%d]/cidr", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->secgroups[j].ingress_rules[k].cidr, NETWORK_ADDR_LEN, "%s", results[i]);
                cidrsplit(gni->secgroups[j].ingress_rules[k].cidr, &scidrnetaddr, &(gni->secgroups[j].ingress_rules[k].cidrSlashnet));
                gni->secgroups[j].ingress_rules[k].cidrNetaddr = dot2hex(scidrnetaddr);
                EUCA_FREE(scidrnetaddr);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/ingressRules/rule[%d]/fromPort", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->secgroups[j].ingress_rules[k].fromPort = atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/ingressRules/rule[%d]/toPort", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->secgroups[j].ingress_rules[k].toPort = atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/ingressRules/rule[%d]/icmpType", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->secgroups[j].ingress_rules[k].icmpType = atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/ingressRules/rule[%d]/icmpCode", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->secgroups[j].ingress_rules[k].icmpCode = atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

        }

        //egress
        found = 0;
        count = 0;
/*
        while (!found) {
            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/egressRules/rule[%d]/protocol", gni->secgroups[j].name, count + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            if (!max_results) {
                found = 1;
            } else {
                count++;
            }
            for (i = 0; i < max_results; i++) {
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);
        }
*/

        snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/egressRules/rule[*]/protocol", gni->secgroups[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);
        count = max_results;

        gni->secgroups[j].egress_rules = EUCA_ZALLOC(count, sizeof(gni_rule));
        gni->secgroups[j].max_egress_rules = count;

        for (k = 0; k < gni->secgroups[j].max_egress_rules; k++) {

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/egressRules/rule[%d]/protocol", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->secgroups[j].egress_rules[k].protocol = atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/egressRules/rule[%d]/groupId", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->secgroups[j].egress_rules[k].groupId, SECURITY_GROUP_ID_LEN, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/egressRules/rule[%d]/groupOwnerId", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->secgroups[j].egress_rules[k].groupOwnerId, 16, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/egressRules/rule[%d]/cidr", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->secgroups[j].egress_rules[k].cidr, NETWORK_ADDR_LEN, "%s", results[i]);
                cidrsplit(gni->secgroups[j].egress_rules[k].cidr, &scidrnetaddr, &(gni->secgroups[j].egress_rules[k].cidrSlashnet));
                gni->secgroups[j].egress_rules[k].cidrNetaddr = dot2hex(scidrnetaddr);
                EUCA_FREE(scidrnetaddr);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/egressRules/rule[%d]/fromPort", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->secgroups[j].egress_rules[k].fromPort = atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/egressRules/rule[%d]/toPort", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->secgroups[j].egress_rules[k].toPort = atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/egressRules/rule[%d]/icmpType", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->secgroups[j].egress_rules[k].icmpType = atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/securityGroups/securityGroup[@name='%s']/egressRules/rule[%d]/icmpCode", gni->secgroups[j].name, k + 1);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->secgroups[j].egress_rules[k].icmpCode = atoi(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);
        }
    }
    return (0);
}

//!
//! Populates globalNetworkInfo vpcs structure from the content of an XML
//! file (xmlXPathContext is expected). The vps section of globalNetworkInfo
//! structure is expected to be empty/clean.
//!
//! @param[in] gni a pointer to the global network information structure
//! @param[in] ctxptr XPATH context to be used to populate
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note 
//!
int gni_populate_vpcs(globalNetworkInfo * gni, xmlXPathContextPtr ctxptr)
{
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i, j, k, l;

    if ((gni == NULL) || (ctxptr == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
    }
    if ((gni->init == FALSE) || (gni->max_vpcs != 0)) {
        LOGERROR("Invalid argument: gni is not initialized or vpcs section is not empty.\n");
        return (1);
    }

    snprintf(expression, 2048, "/network-data/vpcs/vpc");
    rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
    gni->vpcs = EUCA_ZALLOC(max_results, sizeof(gni_vpc));
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->vpcs[i].name, 16, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    gni->max_vpcs = max_results;
    EUCA_FREE(results);

    for (j = 0; j < gni->max_vpcs; j++) {
        snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/ownerId", gni->vpcs[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->vpcs[j].accountId, 128, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/cidr", gni->vpcs[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->vpcs[j].cidr, 24, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/dhcpOptionSet", gni->vpcs[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->vpcs[j].dhcpOptionSet, 16, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/subnets/subnet/@name", gni->vpcs[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        gni->vpcs[j].subnets = EUCA_ZALLOC(max_results, sizeof(gni_vpcsubnet));
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->vpcs[j].subnets[i].name, 16, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        gni->vpcs[j].max_subnets = max_results;
        EUCA_FREE(results);

        snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/routeTables/routeTable/@name", gni->vpcs[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        gni->vpcs[j].routeTables = EUCA_ZALLOC(max_results, sizeof (gni_route_table));
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->vpcs[j].routeTables[i].name, 16, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        gni->vpcs[j].max_routeTables = max_results;
        EUCA_FREE(results);
        for (k = 0; k < gni->vpcs[j].max_routeTables; k++) {
            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/routeTables/routeTable[@name='%s']/ownerId", gni->vpcs[j].name, gni->vpcs[j].routeTables[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->vpcs[j].routeTables[k].accountId, 128, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/routeTables/routeTable[@name='%s']/routes/route[*]/destinationCidr", gni->vpcs[j].name, gni->vpcs[j].routeTables[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            gni->vpcs[j].routeTables[k].entries = EUCA_ZALLOC(max_results, sizeof(gni_route_entry));
            for (i = 0; i < max_results; i++) {
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);
            gni->vpcs[j].routeTables[k].max_entries = max_results;

            for (l = 0; l < gni->vpcs[j].routeTables[k].max_entries; l++) {
                snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/routeTables/routeTable[@name='%s']/routes/route[%d]/destinationCidr", gni->vpcs[j].name, gni->vpcs[j].routeTables[k].name, l + 1);
                rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("after function: %d: %s\n", i, results[i]);
                    snprintf(gni->vpcs[j].routeTables[k].entries[l].destCidr, 16, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);
                snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/routeTables/routeTable[@name='%s']/routes/route[%d]/gatewayId", gni->vpcs[j].name, gni->vpcs[j].routeTables[k].name, l + 1);
                rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
                for (i = 0; i < max_results; i++) {
                    LOGTRACE("after function: %d: %s\n", i, results[i]);
                    snprintf(gni->vpcs[j].routeTables[k].entries[l].target, 32, "%s", results[i]);
                    EUCA_FREE(results[i]);
                }
                EUCA_FREE(results);
                if (max_results == 0) {
                    // Check if the target is a network interface
                    snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/routeTables/routeTable[@name='%s']/routes/route[%d]/networkInterfaceId", gni->vpcs[j].name, gni->vpcs[j].routeTables[k].name, l + 1);
                    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
                    for (i = 0; i < max_results; i++) {
                        LOGTRACE("after function: %d: %s\n", i, results[i]);
                        snprintf(gni->vpcs[j].routeTables[k].entries[l].target, 32, "%s", results[i]);
                        EUCA_FREE(results[i]);
                    }
                    EUCA_FREE(results);
                }
                if (max_results == 0) {
                    // Check if the target is a nat gateway
                    snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/routeTables/routeTable[@name='%s']/routes/route[%d]/natGatewayId", gni->vpcs[j].name, gni->vpcs[j].routeTables[k].name, l + 1);
                    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
                    for (i = 0; i < max_results; i++) {
                        LOGTRACE("after function: %d: %s\n", i, results[i]);
                        snprintf(gni->vpcs[j].routeTables[k].entries[l].target, 32, "%s", results[i]);
                        EUCA_FREE(results[i]);
                    }
                    EUCA_FREE(results);
                }
            }
        }

        for (k = 0; k < gni->vpcs[j].max_subnets; k++) {
            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/subnets/subnet[@name='%s']/ownerId", gni->vpcs[j].name, gni->vpcs[j].subnets[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->vpcs[j].subnets[k].accountId, 128, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/subnets/subnet[@name='%s']/cidr", gni->vpcs[j].name, gni->vpcs[j].subnets[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->vpcs[j].subnets[k].cidr, 24, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/subnets/subnet[@name='%s']/cluster", gni->vpcs[j].name, gni->vpcs[j].subnets[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->vpcs[j].subnets[k].cluster_name, HOSTNAME_LEN, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/subnets/subnet[@name='%s']/networkAcl", gni->vpcs[j].name, gni->vpcs[j].subnets[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->vpcs[j].subnets[k].networkAcl_name, 16, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/subnets/subnet[@name='%s']/routeTable", gni->vpcs[j].name, gni->vpcs[j].subnets[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->vpcs[j].subnets[k].routeTable_name, 16, "%s", results[i]);
                gni->vpcs[j].subnets[k].routeTable = gni_vpc_get_routeTable(&(gni->vpcs[j]), results[i]);
                if (gni->vpcs[j].subnets[k].routeTable == NULL) {
                    LOGWARN("Failed to find GNI %s for %s\n", results[i], gni->vpcs[j].subnets[k].name)
                }
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

        }

        snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/internetGateways/value", gni->vpcs[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        gni->vpcs[j].internetGatewayNames = EUCA_ZALLOC(max_results, sizeof (gni_name));
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->vpcs[j].internetGatewayNames[i].name, 16, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        gni->vpcs[j].max_internetGatewayNames = max_results;
        EUCA_FREE(results);

        // NAT Gateways
        snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/natGateways/natGateway/@name", gni->vpcs[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        gni->vpcs[j].natGateways = EUCA_ZALLOC(max_results, sizeof (gni_nat_gateway));
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->vpcs[j].natGateways[i].name, 32, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        gni->vpcs[j].max_natGateways = max_results;
        EUCA_FREE(results);
        
        for (k = 0; k < gni->vpcs[j].max_natGateways; k++) {
            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/natGateways/natGateway[@name='%s']/ownerId", gni->vpcs[j].name, gni->vpcs[j].natGateways[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->vpcs[j].natGateways[k].accountId, 128, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/natGateways/natGateway[@name='%s']/macAddress", gni->vpcs[j].name, gni->vpcs[j].natGateways[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                mac2hex(results[i], gni->vpcs[j].natGateways[k].macAddress);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/natGateways/natGateway[@name='%s']/publicIp", gni->vpcs[j].name, gni->vpcs[j].natGateways[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->vpcs[j].natGateways[k].publicIp = dot2hex(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/natGateways/natGateway[@name='%s']/privateIp", gni->vpcs[j].name, gni->vpcs[j].natGateways[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                gni->vpcs[j].natGateways[k].privateIp = dot2hex(results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/natGateways/natGateway[@name='%s']/vpc", gni->vpcs[j].name, gni->vpcs[j].natGateways[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->vpcs[j].natGateways[k].vpc, 16, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);

            snprintf(expression, 2048, "/network-data/vpcs/vpc[@name='%s']/natGateways/natGateway[@name='%s']/subnet", gni->vpcs[j].name, gni->vpcs[j].natGateways[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->vpcs[j].natGateways[k].subnet, 16, "%s", results[i]);
                EUCA_FREE(results[i]);
            }
            EUCA_FREE(results);
        }

        // TODO: networkAcls
    }

    snprintf(expression, 2048, "/network-data/internetGateways/internetGateway/@name");
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    gni->vpcIgws = EUCA_ZALLOC(max_results, sizeof(gni_internet_gateway));
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->vpcIgws[i].name, 16, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    gni->max_vpcIgws = max_results;
    EUCA_FREE(results);

    for (j = 0; j < gni->max_vpcIgws; j++) {
        snprintf(expression, 2048, "/network-data/internetGateways/internetGateway[@name='%s']/ownerId", gni->vpcIgws[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->vpcIgws[j].accountId, 128, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);
    }

    return (0);
}

//!
//! Populates globalNetworkInfo eucanetd configuration from the content of an XML
//! file (xmlXPathContext is expected). Relevant sections of globalNetworkInfo
//! structure is expected to be empty/clean.
//!
//! @param[in] gni a pointer to the global network information structure
//! @param[in] ctxptr XPATH context to be used to populate
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note 
//!
int gni_populate_configuration(globalNetworkInfo * gni, gni_hostname_info *host_info, xmlXPathContextPtr ctxptr)
{
    int rc = 0;
    char expression[2048], *strptra = NULL;
    char **results = NULL;
    int max_results = 0, i, j, k, l;
    gni_hostname *gni_hostname_ptr = NULL;
    int hostnames_need_reset = 0;

    if ((gni == NULL) || (ctxptr == NULL)) {
        LOGERROR("Invalid argument: gni or ctxptr is NULL.\n");
        return (1);
    }
    if ((gni->init == FALSE)) {
        LOGERROR("Invalid argument: gni is not initialized or instances section is not empty.\n");
        return (1);
    }

    snprintf(expression, 2048, "/network-data/configuration/property[@name='enabledCLCIp']/value");
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        gni->enabledCLCIp = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "/network-data/configuration/property[@name='instanceDNSDomain']/value");
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->instanceDNSDomain, HOSTNAME_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

#ifdef USE_IP_ROUTE_HANDLER
    snprintf(expression, 2048, "/network-data/configuration/property[@name='publicGateway']/value");
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        gni->publicGateway = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);
#endif /* USE_IP_ROUTE_HANDLER */

    if (IS_NETMODE_VPCMIDO(gni)) {
        snprintf(expression, 2048, "/network-data/configuration/property[@name='mido']/property[@name='eucanetdHost']/value");
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->EucanetdHost, HOSTNAME_LEN, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        char gwtoks[6][2048];
        int good = 1, max_gws = 0;

        snprintf(expression, 2048, "/network-data/configuration/property[@name='mido']/property[@name='gateways']/*/property[@name='gatewayHost']/value");
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        max_gws = max_results;
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            bzero(gwtoks[i], 2048);
            snprintf(gwtoks[i], 2048, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "/network-data/configuration/property[@name='mido']/property[@name='gateways']/*/property[@name='gatewayIP']/value");
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        if (max_results != max_gws) good = 0;
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            euca_strncat(gwtoks[i], ",", 2048);
            euca_strncat(gwtoks[i], results[i], 2048);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "/network-data/configuration/property[@name='mido']/property[@name='gateways']/*/property[@name='gatewayInterface']/value");
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        if (max_results != max_gws) good = 0;
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            euca_strncat(gwtoks[i], ",", 2048);
            euca_strncat(gwtoks[i], results[i], 2048);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        if (!good || max_gws <= 0) {
            LOGERROR("Invalid mido gateway(s) detected. Check network configuration.\n");
        } else {
            for (i = 0; i < max_gws; i++) {
                euca_strncat(gni->GatewayHosts, gwtoks[i], HOSTNAME_LEN * 3 * 33);
                euca_strncat(gni->GatewayHosts, " ", HOSTNAME_LEN * 3 * 33);
            }
        }
        snprintf(expression, 2048, "/network-data/configuration/property[@name='mido']/property[@name='publicNetworkCidr']/value");
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->PublicNetworkCidr, HOSTNAME_LEN, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "/network-data/configuration/property[@name='mido']/property[@name='publicGatewayIP']/value");
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->PublicGatewayIP, HOSTNAME_LEN, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);
    }

    snprintf(expression, 2048, "/network-data/configuration/property[@name='instanceDNSServers']/value");
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    gni->instanceDNSServers = EUCA_ZALLOC(max_results, sizeof(u32));
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        gni->instanceDNSServers[i] = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    gni->max_instanceDNSServers = max_results;
    EUCA_FREE(results);

    snprintf(expression, 2048, "/network-data/configuration/property[@name='publicIps']/value");
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);

    if (results && max_results) {
        rc = gni_serialize_iprange_list(results, max_results, &(gni->public_ips), &(gni->max_public_ips));
        //    gni->public_ips = EUCA_ZALLOC(max_results, sizeof(u32));
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            //        gni->public_ips[i] = dot2hex(results[i]);
            EUCA_FREE(results[i]);
        }
        //    gni->max_public_ips = max_results;
        EUCA_FREE(results);
    }
    // Do we have any managed subnets?
    snprintf(expression, 2048, "/network-data/configuration/property[@name='managedSubnet']/managedSubnet");
    rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
    gni->managedSubnet = EUCA_ZALLOC(max_results, sizeof(gni_subnet));
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        gni->managedSubnet[i].subnet = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    gni->max_managedSubnets = max_results;
    EUCA_FREE(results);

    // If we do have any managed subnets, retrieve the rest of the information
    for (j = 0; j < gni->max_managedSubnets; j++) {
        strptra = hex2dot(gni->managedSubnet[j].subnet);

        // Get the netmask
        snprintf(expression, 2048, "/network-data/configuration/property[@name='managedSubnet']/managedSubnet[@name='%s']/property[@name='netmask']/value", SP(strptra));
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            gni->managedSubnet[j].netmask = dot2hex(results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        // Now get the minimum VLAN index
        snprintf(expression, 2048, "/network-data/configuration/property[@name='managedSubnet']/managedSubnet[@name='%s']/property[@name='minVlan']/value", SP(strptra));
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            gni->managedSubnet[j].minVlan = atoi(results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        // Now get the maximum VLAN index
        snprintf(expression, 2048, "/network-data/configuration/property[@name='managedSubnet']/managedSubnet[@name='%s']/property[@name='maxVlan']/value", SP(strptra));
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            gni->managedSubnet[j].maxVlan = atoi(results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        // Now get the minimum VLAN index
        snprintf(expression, 2048, "/network-data/configuration/property[@name='managedSubnet']/managedSubnet[@name='%s']/property[@name='segmentSize']/value", SP(strptra));
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            gni->managedSubnet[j].segmentSize = atoi(results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);
        EUCA_FREE(strptra);
    }

    snprintf(expression, 2048, "/network-data/configuration/property[@name='subnets']/subnet");
    rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
    gni->subnets = EUCA_ZALLOC(max_results, sizeof(gni_subnet));
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        gni->subnets[i].subnet = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    gni->max_subnets = max_results;
    EUCA_FREE(results);

    for (j = 0; j < gni->max_subnets; j++) {
        strptra = hex2dot(gni->subnets[j].subnet);
        snprintf(expression, 2048, "/network-data/configuration/property[@name='subnets']/subnet[@name='%s']/property[@name='netmask']/value", SP(strptra));
        EUCA_FREE(strptra);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            gni->subnets[j].netmask = dot2hex(results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        strptra = hex2dot(gni->subnets[j].subnet);
        snprintf(expression, 2048, "/network-data/configuration/property[@name='subnets']/subnet[@name='%s']/property[@name='gateway']/value", SP(strptra));
        EUCA_FREE(strptra);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            gni->subnets[j].gateway = dot2hex(results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);
    }

    // Process Node Controller names to populate the ip->hostname 'cache'
    // This is only relevant to VPCMIDO.
    // Search for nodes changed to MidoNet-based instead of DNS-based (EUCA-11997)
    if (0 && (IS_NETMODE_VPCMIDO(gni))) {
        snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/*/property[@name='nodes']/node");
        rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
        LOGTRACE("Found %d Nodes in the config\n", max_results);

        //
        // Create a temp list so we can detect if we need to refresh the cached names or not.
        gni_hostname_ptr = EUCA_ZALLOC(max_results, sizeof (gni_hostname));

        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            {
                struct hostent *hent;
                struct in_addr addr;
                char *tmp_hostname;

                if (inet_aton(results[i], &addr)) {
                    gni_hostname_ptr[i].ip_address.s_addr = addr.s_addr;

                    rc = gni_hostnames_get_hostname(host_info, results[i], &tmp_hostname);
                    if (rc) {
                        hostnames_need_reset = 1;
                        if ((hent = gethostbyaddr((char *) &(addr.s_addr), sizeof (addr.s_addr), AF_INET))) {
                            LOGTRACE("Found hostname via reverse lookup: %s\n", hent->h_name);
                            snprintf(gni_hostname_ptr[i].hostname, HOSTNAME_SIZE, "%s", hent->h_name);
                        } else {
                            LOGTRACE("Hostname not found for ip: %s using name: %s\n", results[i], results[i]);
                            snprintf(gni_hostname_ptr[i].hostname, HOSTNAME_SIZE, "%s", results[i]);
                        }
                    } else {
                        LOGTRACE("Found cached hostname storing: %s\n", tmp_hostname);
                        snprintf(gni_hostname_ptr[i].hostname, HOSTNAME_SIZE, "%s", tmp_hostname); // store the name
                        EUCA_FREE(tmp_hostname);
                    }
                }
            }
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        //
        // If we've added an entry that didn't exist previously, we need to set a new
        // hostname cache and free up the orignal, then re-sort.
        //
        if (hostnames_need_reset) {
            LOGTRACE("Hostname cache reset needed\n");

            EUCA_FREE(host_info->hostnames);
            host_info->hostnames = gni_hostname_ptr;
            host_info->max_hostnames = max_results;

            qsort(host_info->hostnames, host_info->max_hostnames, sizeof (gni_hostname), cmpipaddr);
            hostnames_need_reset = 0;
        } else {
            LOGTRACE("No hostname cache change, freeing up temp cache\n");
            EUCA_FREE(gni_hostname_ptr);
        }
    }

    snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster");
    rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
    gni->clusters = EUCA_ZALLOC(max_results, sizeof(gni_cluster));
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(gni->clusters[i].name, HOSTNAME_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    gni->max_clusters = max_results;
    EUCA_FREE(results);

    for (j = 0; j < gni->max_clusters; j++) {

        snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='enabledCCIp']/value", gni->clusters[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            gni->clusters[j].enabledCCIp = dot2hex(results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='macPrefix']/value", gni->clusters[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->clusters[j].macPrefix, ENET_MACPREFIX_LEN, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='privateIps']/value", gni->clusters[j].name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        if (results && max_results) {
            rc = gni_serialize_iprange_list(results, max_results, &(gni->clusters[j].private_ips), &(gni->clusters[j].max_private_ips));
            //        gni->clusters[j].private_ips = EUCA_ZALLOC(max_results, sizeof(u32));
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                //            gni->clusters[j].private_ips[i] = dot2hex(results[i]);
                EUCA_FREE(results[i]);
            }
            //        gni->clusters[j].max_private_ips = max_results;
            EUCA_FREE(results);
        }

        snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/subnet", gni->clusters[j].name);
        rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            gni->clusters[j].private_subnet.subnet = dot2hex(results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        strptra = hex2dot(gni->clusters[j].private_subnet.subnet);
        snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/subnet[@name='%s']/property[@name='netmask']/value",
                 gni->clusters[j].name, SP(strptra));
        EUCA_FREE(strptra);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            gni->clusters[j].private_subnet.netmask = dot2hex(results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        strptra = hex2dot(gni->clusters[j].private_subnet.subnet);
        snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/subnet[@name='%s']/property[@name='gateway']/value",
                 gni->clusters[j].name, SP(strptra));
        EUCA_FREE(strptra);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            gni->clusters[j].private_subnet.gateway = dot2hex(results[i]);
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='nodes']/node", gni->clusters[j].name);
        rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
        gni->clusters[j].nodes = EUCA_ZALLOC(max_results, sizeof(gni_node));
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(gni->clusters[j].nodes[i].name, HOSTNAME_LEN, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        gni->clusters[j].max_nodes = max_results;
        EUCA_FREE(results);

        for (k = 0; k < gni->clusters[j].max_nodes; k++) {

            snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='nodes']/node[@name='%s']/instanceIds/value",
                     gni->clusters[j].name, gni->clusters[j].nodes[k].name);
            rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
            gni->clusters[j].nodes[k].instance_names = EUCA_ZALLOC(max_results, sizeof (gni_name));
            for (i = 0; i < max_results; i++) {
                LOGTRACE("after function: %d: %s\n", i, results[i]);
                snprintf(gni->clusters[j].nodes[k].instance_names[i].name, 1024, "%s", results[i]);
                EUCA_FREE(results[i]);

                if (IS_NETMODE_VPCMIDO(gni)) {
                    for (l = 0; l < gni->max_instances; l++) {
                        if (!strcmp(gni->instances[l].name, gni->clusters[j].nodes[k].instance_names[i].name)) {
                            snprintf(gni->instances[l].node, HOSTNAME_LEN, "%s", gni->clusters[j].nodes[k].name);
/*
                            {
                                char *hostname = NULL;

                                rc = gni_hostnames_get_hostname(host_info, gni->instances[l].node, &hostname);
                                if (rc) {
                                    LOGTRACE("Failed to find cached hostname for IP: %s\n", gni->instances[l].node);
                                    snprintf(gni->instances[l].nodehostname, HOSTNAME_SIZE, "%s", gni->instances[l].node);
                                } else {
                                    LOGTRACE("Found cached hostname: %s for IP: %s\n", hostname, gni->instances[l].node);
                                    snprintf(gni->instances[l].nodehostname, HOSTNAME_SIZE, "%s", hostname);
                                    EUCA_FREE(hostname);
                                }
                            }
*/
                        }
                    }
                    for (l = 0; l < gni->max_interfaces; l++) {
                        if (!strcmp(gni->interfaces[l].instance_name.name, gni->clusters[j].nodes[k].instance_names[i].name)) {
                            snprintf(gni->interfaces[l].node, HOSTNAME_LEN, "%s", gni->clusters[j].nodes[k].name);
/*
                            {
                                char *hostname = NULL;

                                rc = gni_hostnames_get_hostname(host_info, gni->interfaces[l].node, &hostname);
                                if (rc) {
                                    LOGTRACE("Failed to find cached hostname for IP: %s\n", gni->interfaces[l].node);
                                    snprintf(gni->interfaces[l].nodehostname, HOSTNAME_SIZE, "%s", gni->interfaces[l].node);
                                } else {
                                    LOGTRACE("Found cached hostname: %s for IP: %s\n", hostname, gni->instances[l].node);
                                    snprintf(gni->interfaces[l].nodehostname, HOSTNAME_SIZE, "%s", hostname);
                                    EUCA_FREE(hostname);
                                }
                            }
*/
                        }
                    }
                }
            }
            gni->clusters[j].nodes[k].max_instance_names = max_results;
            EUCA_FREE(results);
        }
    }
    return (0);
}

//!
//! Populates globalNetworkInfo instance structure from the content of an XML
//! file (xmlXPathContext is expected). The target instance structure is assumed
//! to be clean, with the name pre-populated.
//!
//! @param[in] instance a pointer to the gni_instance structure to be populated.
//! @param[in] xmlpath partial xml path from where the instance information will
//! be extracted.
//! @param[in] ctxptr XPATH context to be used to populate.
//!
//! @return 0 on success or 1 on failure.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note 
//!
int gni_populate_instance_interface(gni_instance *instance, const char *xmlpath, xmlXPathContextPtr ctxptr)
{
    int rc = 0;
    char expression[2048];
    char **results = NULL;
    int max_results = 0, i;
    boolean is_instance = TRUE;

    if ((instance == NULL) || (ctxptr == NULL)) {
        LOGERROR("Invalid argument: instance or ctxptr is NULL.\n");
        return (1);
        }
    if ((instance->name == NULL) || (strlen(instance->name) == 0)) {
        LOGERROR("Invalid argument: invalid instance name.\n");
    }

    if (strstr(instance->name, "eni-")) {
        is_instance = FALSE;
    } else {
        is_instance = TRUE;
    }
    snprintf(expression, 2048, "%s[@name='%s']/ownerId", xmlpath, instance->name);
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(instance->accountId, 128, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "%s[@name='%s']/macAddress", xmlpath, instance->name);
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        mac2hex(results[i], instance->macAddress);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "%s[@name='%s']/publicIp", xmlpath, instance->name);
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        instance->publicIp = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "%s[@name='%s']/privateIp", xmlpath, instance->name);
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        instance->privateIp = dot2hex(results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "%s[@name='%s']/vpc", xmlpath, instance->name);
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(instance->vpc, 16, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "%s[@name='%s']/subnet", xmlpath, instance->name);
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(instance->subnet, 16, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    snprintf(expression, 2048, "%s[@name='%s']/securityGroups/value", xmlpath, instance->name);
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    instance->secgroup_names = EUCA_ZALLOC(max_results, sizeof (gni_name));
    instance->gnisgs = EUCA_ZALLOC(max_results, sizeof (gni_secgroup *));
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(instance->secgroup_names[i].name, 1024, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    instance->max_secgroup_names = max_results;
    EUCA_FREE(results);

    snprintf(expression, 2048, "%s[@name='%s']/attachmentId", xmlpath, instance->name);
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i = 0; i < max_results; i++) {
        LOGTRACE("after function: %d: %s\n", i, results[i]);
        snprintf(instance->attachmentId, ENI_ATTACHMENT_ID_LEN, "%s", results[i]);
        EUCA_FREE(results[i]);
    }
    EUCA_FREE(results);

    if (is_instance) {
        // Populate interfaces.
        snprintf(expression, 2048, "%s[@name='%s']/networkInterfaces/networkInterface", xmlpath, instance->name);
        rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
        instance->interface_names = EUCA_REALLOC(instance->interface_names, instance->max_interface_names + max_results, sizeof (gni_name));
        if (instance->interface_names == NULL) {
            LOGERROR("out of memory.\n");
        return (1);
        }
        bzero(&(instance->interface_names[instance->max_interface_names]), max_results * sizeof (gni_name));
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            snprintf(instance->interface_names[instance->max_interface_names + i].name, 1024, "%s", results[i]);
            EUCA_FREE(results[i]);
        }
        instance->max_interface_names += max_results;
        EUCA_FREE(results);
    }
    if (!is_instance) {
        snprintf(expression, 2048, "%s[@name='%s']/sourceDestCheck", xmlpath, instance->name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
            LOGTRACE("after function: %d: %s\n", i, results[i]);
            euca_strtolower(results[i]);
            if (!strcmp(results[i], "true")) {
                instance->srcdstcheck = TRUE;
            } else {
                instance->srcdstcheck = FALSE;
            }
            EUCA_FREE(results[i]);
        }
        EUCA_FREE(results);

        snprintf(expression, 2048, "%s[@name='%s']/deviceIndex", xmlpath, instance->name);
        rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
        for (i = 0; i < max_results; i++) {
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

//!
//! TODO: Describe
//!
//! @param[in]  inlist
//! @param[in]  inmax
//! @param[out] outlist
//! @param[out] outmax
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_serialize_iprange_list(char **inlist, int inmax, u32 ** outlist, int *outmax)
{
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
                numi = (int)(endb - startb) + 1;
                outlistbuf = realloc(outlistbuf, sizeof(u32) * (max_outlistbuf + numi));
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
        *outlist = malloc(sizeof(u32) * *outmax);
        memcpy(*outlist, outlistbuf, sizeof(u32) * max_outlistbuf);
    }
    EUCA_FREE(outlistbuf);

    return (ret);
}

//!
//! Iterates through a given globalNetworkInfo structure and execute the
//! given operation mode.
//!
//! @param[in] gni a pointer to the global network information structure
//! @param[in] mode the iteration mode: GNI_ITERATE_PRINT or GNI_ITERATE_FREE
//!
//! @return Always return 0
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_iterate(globalNetworkInfo * gni, int mode)
{
    int i, j;
    char *strptra = NULL;

    strptra = hex2dot(gni->enabledCLCIp);
    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("enabledCLCIp: %s\n", SP(strptra));
    EUCA_FREE(strptra);

    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("instanceDNSDomain: %s\n", gni->instanceDNSDomain);

#ifdef USE_IP_ROUTE_HANDLER
    if (mode == GNI_ITERATE_PRINT) {
        strptra = hex2dot(gni->publicGateway);
        LOGTRACE("publicGateway: %s\n", SP(strptra));
        EUCA_FREE(strptra);
    }
#endif /* USE_IP_ROUTE_HANDLER */

    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("instanceDNSServers: \n");
    for (i = 0; i < gni->max_instanceDNSServers; i++) {
        strptra = hex2dot(gni->instanceDNSServers[i]);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\tdnsServer %d: %s\n", i, SP(strptra));
        EUCA_FREE(strptra);
    }
    if (mode == GNI_ITERATE_FREE) {
        EUCA_FREE(gni->instanceDNSServers);
    }

    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("publicIps: \n");
    for (i = 0; i < gni->max_public_ips; i++) {
        strptra = hex2dot(gni->public_ips[i]);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\tip %d: %s\n", i, SP(strptra));
        EUCA_FREE(strptra);
    }
    if (mode == GNI_ITERATE_FREE) {
        EUCA_FREE(gni->public_ips);
    }

    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("subnets: \n");
    for (i = 0; i < gni->max_subnets; i++) {

        strptra = hex2dot(gni->subnets[i].subnet);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\tsubnet %d: %s\n", i, SP(strptra));
        EUCA_FREE(strptra);

        strptra = hex2dot(gni->subnets[i].netmask);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\tnetmask: %s\n", SP(strptra));
        EUCA_FREE(strptra);

        strptra = hex2dot(gni->subnets[i].gateway);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\tgateway: %s\n", SP(strptra));
        EUCA_FREE(strptra);

    }
    if (mode == GNI_ITERATE_FREE) {
        EUCA_FREE(gni->subnets);
    }

    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("managed_subnets: \n");
    for (i = 0; i < gni->max_subnets; i++) {
        strptra = hex2dot(gni->managedSubnet[i].subnet);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\tmanaged_subnet %d: %s\n", i, SP(strptra));
        EUCA_FREE(strptra);

        strptra = hex2dot(gni->managedSubnet[i].netmask);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\tnetmask: %s\n", SP(strptra));
        EUCA_FREE(strptra);

        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\tminVlan: %d\n", gni->managedSubnet[i].minVlan);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\tmaxVlan: %d\n", gni->managedSubnet[i].minVlan);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\tsegmentSize: %d\n", gni->managedSubnet[i].segmentSize);
    }
    if (mode == GNI_ITERATE_FREE) {
        EUCA_FREE(gni->managedSubnet);
    }

    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("clusters: \n");
    for (i = 0; i < gni->max_clusters; i++) {
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\tcluster %d: %s\n", i, gni->clusters[i].name);
        strptra = hex2dot(gni->clusters[i].enabledCCIp);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\tenabledCCIp: %s\n", SP(strptra));
        EUCA_FREE(strptra);

        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\tmacPrefix: %s\n", gni->clusters[i].macPrefix);

        strptra = hex2dot(gni->clusters[i].private_subnet.subnet);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\tsubnet: %s\n", SP(strptra));
        EUCA_FREE(strptra);

        strptra = hex2dot(gni->clusters[i].private_subnet.netmask);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\t\tnetmask: %s\n", SP(strptra));
        EUCA_FREE(strptra);

        strptra = hex2dot(gni->clusters[i].private_subnet.gateway);
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\t\tgateway: %s\n", SP(strptra));
        EUCA_FREE(strptra);

        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\tprivate_ips \n");
        for (j = 0; j < gni->clusters[i].max_private_ips; j++) {
            strptra = hex2dot(gni->clusters[i].private_ips[j]);
            if (mode == GNI_ITERATE_PRINT)
                LOGTRACE("\t\t\tip %d: %s\n", j, SP(strptra));
            EUCA_FREE(strptra);
        }
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\t\tnodes \n");
        for (j = 0; j < gni->clusters[i].max_nodes; j++) {
            if (mode == GNI_ITERATE_PRINT)
                LOGTRACE("\t\t\tnode %d: %s\n", j, gni->clusters[i].nodes[j].name);
            if (mode == GNI_ITERATE_FREE) {
                gni_node_clear(&(gni->clusters[i].nodes[j]));
            }
        }
        if (mode == GNI_ITERATE_FREE) {
            EUCA_FREE(gni->clusters[i].nodes);
            gni_cluster_clear(&(gni->clusters[i]));
        }
    }
    if (mode == GNI_ITERATE_FREE) {
        EUCA_FREE(gni->clusters);
    }

    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("instances: \n");
    for (i = 0; i < gni->max_instances; i++) {
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\tid: %s\n", gni->instances[i].name);
        if (mode == GNI_ITERATE_FREE) {
            gni_instance_clear(&(gni->instances[i]));
        }
    }
    if (mode == GNI_ITERATE_FREE) {
        EUCA_FREE(gni->instances);
    }

    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("interfaces: \n");
    for (i = 0; i < gni->max_interfaces; i++) {
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\tid: %s\n", gni->interfaces[i].name);
        if (mode == GNI_ITERATE_FREE) {
            gni_instance_clear(&(gni->interfaces[i]));
        }
    }
    if (mode == GNI_ITERATE_FREE) {
        EUCA_FREE(gni->interfaces);
    }

    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("secgroups: \n");
    for (i = 0; i < gni->max_secgroups; i++) {
        if (mode == GNI_ITERATE_PRINT)
            LOGTRACE("\tname: %s\n", gni->secgroups[i].name);
        if (mode == GNI_ITERATE_FREE) {
            gni_secgroup_clear(&(gni->secgroups[i]));
        }
    }
    if (mode == GNI_ITERATE_FREE) {
        EUCA_FREE(gni->secgroups);
    }

    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("vpcs: \n");
    for (i = 0; i < gni->max_vpcs; i++) {
        if (mode == GNI_ITERATE_PRINT) {
            LOGTRACE("\tname: %s\n", gni->vpcs[i].name);
            LOGTRACE("\taccountId: %s\n", gni->vpcs[i].accountId);
            LOGTRACE("\tsubnets: \n");
            for (j = 0; j < gni->vpcs[i].max_subnets; j++) {
                LOGTRACE("\t\tname: %s\n", gni->vpcs[i].subnets[j].name);
                LOGTRACE("\t\trouteTable: %s\n", gni->vpcs[i].subnets[j].routeTable_name);
            }
        }
        if (mode == GNI_ITERATE_FREE) {
            gni_vpc_clear(&(gni->vpcs[i]));
        }
    }
    if (mode == GNI_ITERATE_FREE) {
        EUCA_FREE(gni->vpcs);
    }

    if (mode == GNI_ITERATE_PRINT)
        LOGTRACE("Internet Gateways: \n");
    for (i = 0; i < gni->max_vpcIgws; i++) {
        if (mode == GNI_ITERATE_PRINT) {
            LOGTRACE("\tname: %s\n", gni->vpcIgws[i].name);
            LOGTRACE("\taccountId: %s\n", gni->vpcIgws[i].accountId);
        }
    }
    if (mode == GNI_ITERATE_FREE) {
        EUCA_FREE(gni->vpcIgws);
    }

    if (mode == GNI_ITERATE_FREE) {
        //bzero(gni, sizeof (globalNetworkInfo));
        gni->init = 1;
        gni->networkInfo[0] = '\0';
        char *version_addr = (char *) gni + (sizeof (gni->init) + sizeof (gni->networkInfo));
        memset(version_addr, 0, sizeof (globalNetworkInfo) - sizeof (gni->init) - sizeof (gni->networkInfo));
    }

    return (0);
}

//!
//! Clears a given globalNetworkInfo structure. This will free member's allocated memory and zero
//! out the structure itself.
//!
//! @param[in] gni a pointer to the global network information structure
//!
//! @return the result of the gni_iterate() call
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_clear(globalNetworkInfo * gni)
{
    return (gni_iterate(gni, GNI_ITERATE_FREE));
}

//!
//! Logs the content of a given globalNetworkInfo structure
//!
//! @param[in] gni a pointer to the global network information structure
//!
//! @return the result of the gni_iterate() call
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_print(globalNetworkInfo * gni)
{
    return (gni_iterate(gni, GNI_ITERATE_PRINT));
}

//!
//! Clears and free a given globalNetworkInfo structure.
//!
//! @param[in] gni a pointer to the global network information structure
//!
//! @return Always return 0
//!
//! @see gni_clear()
//!
//! @pre
//!
//! @post
//!
//! @note The caller should free the given pointer
//!
int gni_free(globalNetworkInfo * gni)
{
    if (!gni) {
        return (0);
    }
    gni_clear(gni);
    EUCA_FREE(gni);
    return (0);
}

//Maps the protocol number passed in, to the name 
static int map_proto_to_names(int proto_number, char *out_proto_name, int out_proto_len)
{
  struct protoent *proto = NULL;
    if (NULL == out_proto_name) {
        LOGERROR("Cannot map protocol number to name because arguments are null or not allocated enough buffers. Proto number=%d, out_proto_len=%d\n",
                 proto_number, out_proto_len);
        return 1;
    }

	if(proto_number < 0 || proto_number > 255) {
	  LOGERROR("Cannot map invalid protocol number: %d. Must be between 0 and 255 inclusive\n", proto_number);
	  return 1;
	}

	//Explicitly map only tcp/udp/icmp
	if(TCP_PROTOCOL_NUMBER == proto_number ||
	   UDP_PROTOCOL_NUMBER == proto_number ||
	   ICMP_PROTOCOL_NUMBER == proto_number) {
	  //Use libc to map number to name
	  proto = getprotobynumber(proto_number);
    }
	  if(NULL != proto) {
		//There is a name, use it
        if (NULL != proto->p_name && strlen(proto->p_name) > 0) {
		  euca_strncpy(out_proto_name, proto->p_name, out_proto_len);
        }
	  } else {
        //There is no name, just use the raw number
        snprintf(out_proto_name, out_proto_len, "%d", proto_number);
	  }
    return 0;
}

//!
//! TODO: Define
//!
//! @param[in]  rulebuf a string containing the IP table rule to convert
//! @param[out] outrule a string containing the converted rule
//!
//! @return 0 on success or 1 on failure.
//!
//! @see
//!
//! @pre Both rulebuf and outrule MUST not be NULL
//!
//! @post \li uppon success the outrule contains the converted value
//!       \li uppon failure, outrule does not contain any valid data
//!       \li regardless of success or failure case, rulebuf will be modified by a strtok_r() call
//!
//! @note
//!
int ruleconvert(char *rulebuf, char *outrule)
{
    int ret = 0;
    //char proto[4]; //Protocol is always a 3-digit number in global network xml.
    int protocol_number = -1;
    int rc = EUCA_ERROR;
    char portrange[64], sourcecidr[64], icmptyperange[64], sourceowner[64], sourcegroup[64], newrule[4097], buf[2048];
    char proto[64];                    //protocol name mapped for IPTABLES usage
    char *ptra = NULL, *toka = NULL, *idx = NULL;

    proto[0] = portrange[0] = sourcecidr[0] = icmptyperange[0] = newrule[0] = sourceowner[0] = sourcegroup[0] = '\0';

    if ((idx = strchr(rulebuf, '\n'))) {
        *idx = '\0';
    }

    toka = strtok_r(rulebuf, " ", &ptra);
    while (toka) {
        if (!strcmp(toka, "-P")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka) {
                protocol_number = atoi(toka);
            }
        } else if (!strcmp(toka, "-p")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka)
                snprintf(portrange, 64, "%s", toka);
            if ((idx = strchr(portrange, '-'))) {
                char minport[64], maxport[64];
                sscanf(portrange, "%[0-9]-%[0-9]", minport, maxport);
                if (!strcmp(minport, maxport)) {
                    snprintf(portrange, 64, "%s", minport);
                } else {
                    *idx = ':';
                }
            }
        } else if (!strcmp(toka, "-s")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka)
                snprintf(sourcecidr, 64, "%s", toka);
            if (!strcmp(sourcecidr, "0.0.0.0/0")) {
                sourcecidr[0] = '\0';
            }
        } else if (!strcmp(toka, "-t")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka)
                snprintf(icmptyperange, 64, "any");
        } else if (!strcmp(toka, "-o")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka)
                snprintf(sourcegroup, 64, toka);
        } else if (!strcmp(toka, "-u")) {
            toka = strtok_r(NULL, " ", &ptra);
            if (toka)
                snprintf(sourceowner, 64, toka);
        }
        toka = strtok_r(NULL, " ", &ptra);
    }

    LOGTRACE("TOKENIZED RULE: PROTO: %d PORTRANGE: %s SOURCECIDR: %s ICMPTYPERANGE: %s SOURCEOWNER: %s SOURCEGROUP: %s\n", protocol_number, portrange, sourcecidr, icmptyperange,
             sourceowner, sourcegroup);

    // check if enough info is present to construct rule
    // Fix for EUCA-10031, no port range required. Ports should be limited and enforced at front-end
    // per AWS policy, not in the backend since IPTABLES doesn't care
    if (protocol_number >= 0) {
        //Handle protocol mapping first
        rc = map_proto_to_names(protocol_number, proto, 64);
        if (!rc && strlen(proto) > 0) {
            if (TCP_PROTOCOL_NUMBER == protocol_number || UDP_PROTOCOL_NUMBER == protocol_number) {
                //For tcp and udp add a module for port handling
                snprintf(buf, 2048, "-p %s -m %s ", proto, proto);
            } else {
                snprintf(buf, 2048, "-p %s ", proto);
            }
            strncat(newrule, buf, 2048);
        } else {
            LOGERROR("Error mapping protocol number %d to string for iptables rules\n", protocol_number);
            return 1;
        }

        if (strlen(sourcecidr)) {
            snprintf(buf, 2048, "-s %s ", sourcecidr);
            strncat(newrule, buf, 2048);
        }

        //Only allow port ranges for tcp and udp
        if ((TCP_PROTOCOL_NUMBER == protocol_number || UDP_PROTOCOL_NUMBER == protocol_number) && strlen(portrange)) {
            snprintf(buf, 2048, "--dport %s ", portrange);
            strncat(newrule, buf, 2048);
        }

        //Only allow icmp for proper icmp
        if (protocol_number == 1 && strlen(icmptyperange)) {
            snprintf(buf, 2048, "--icmp-type %s ", icmptyperange);
            strncat(newrule, buf, 2048);
        }

        while (newrule[strlen(newrule) - 1] == ' ') {
            newrule[strlen(newrule) - 1] = '\0';
        }

        snprintf(outrule, 2048, "%s", newrule);
        LOGTRACE("CONVERTED RULE: %s\n", outrule);
    } else {
        LOGWARN("not enough information in source rule to construct iptables rule: skipping\n");
        ret = 1;
    }

    return (ret);
}

//!
//! Creates an iptables rule using the source CIDR specified in the argument, and
//! based on the ingress rule entry in the argument.
//!
//! @param[in] scidr a string containing a CIDR to be used in the output iptables rule to match the source (can ba a single IP address).
//! If null, the source address within the ingress rule will be used.
//! @param[in] ingress_rule gni_rule structure containing an ingress rule.
//! @param[in] flags integer containing extra conditions that will be added to the output iptables rule.
//! If 0, no condition is added. If 1 the output iptables rule will allow traffic between VMs on the same NC (see EUCA-11083).
//! @param[out] outrule a string containing the converted rule. A buffer with at least 1024 chars is expected.
//!
//! @return 0 on success or 1 on failure.
//!
//! @see
//!
//! @pre ingress_rule and outrule pointers MUST not be NULL
//!
//! @post \li uppon success the outrule contains the converted iptables rule.
//!       \li uppon failure, outrule does not contain any valid data
//!
//! @note
//!
int ingress_gni_to_iptables_rule(char *scidr, gni_rule *ingress_rule, char *outrule, int flags) {
#define MAX_RULE_LEN     1024
#define MAX_NEWRULE_LEN  2049
    char newrule[MAX_NEWRULE_LEN], buf[MAX_RULE_LEN];
    char *strptr = NULL;
    struct protoent *proto_info = NULL;

    if (!ingress_rule || !outrule) {
        LOGERROR("Invalid pointer(s) to ingress_gni_rule and/or iptables rule buffer.\n");
        return 1;
    }

    // Check for protocol all (-1) - should not happen in non-VPC
    if (-1 != ingress_rule->protocol) {
        proto_info = getprotobynumber(ingress_rule->protocol);
        if (proto_info == NULL) {
            LOGWARN("Invalid protocol (%d) - cannot create iptables rule.", ingress_rule->protocol);
            return 1;
        }
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
            // snprintf(buf, MAX_RULE_LEN, "-p %s ", proto_info->p_name);
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
        case 2: // Add condition to the rule to accept the packet if it would be SNATed (MANAGED).
            snprintf(buf, MAX_RULE_LEN, "-m mark --mark 0x15 ");
            strncat(newrule, buf, MAX_RULE_LEN);
            break;
        case 4: // Add condition to the rule to accept the packet if it would NOT be SNATed (MANAGED).
            snprintf(buf, MAX_RULE_LEN, "-m mark ! --mark 0x15 ");
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

//!
//! Clears a gni_cluster structure. This will free member's allocated memory and zero
//! out the structure itself.
//!
//! @param[in] cluster a pointer to the structure to clear
//!
//! @return This function always returns 0
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_cluster_clear(gni_cluster * cluster)
{
    if (!cluster) {
        return (0);
    }

    EUCA_FREE(cluster->private_ips);

    bzero(cluster, sizeof(gni_cluster));

    return (0);
}

//!
//! Clears a gni_node structure. This will free member's allocated memory and zero
//! out the structure itself.
//!
//! @param[in] node a pointer to the structure to clear
//!
//! @return This function always returns 0
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_node_clear(gni_node * node)
{
    if (!node) {
        return (0);
    }

    EUCA_FREE(node->instance_names);

    bzero(node, sizeof(gni_node));

    return (0);
}

//!
//! Clears a gni_instance structure. This will free member's allocated memory and zero
//! out the structure itself.
//!
//! @param[in] instance a pointer to the structure to clear
//!
//! @return This function always returns 0
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_instance_clear(gni_instance * instance)
{
    if (!instance) {
        return (0);
    }

    EUCA_FREE(instance->secgroup_names);
    EUCA_FREE(instance->interface_names);
    EUCA_FREE(instance->gnisgs);

    bzero(instance, sizeof(gni_instance));

    return (0);
}

//!
//! Clears a gni_secgroup structure. This will free member's allocated memory and zero
//! out the structure itself.
//!
//! @param[in] secgroup a pointer to the structure to clear
//!
//! @return This function always returns 0
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_secgroup_clear(gni_secgroup * secgroup)
{
    if (!secgroup) {
        return (0);
    }

    EUCA_FREE(secgroup->ingress_rules);
    EUCA_FREE(secgroup->egress_rules);
    EUCA_FREE(secgroup->grouprules);
    EUCA_FREE(secgroup->instances);
    EUCA_FREE(secgroup->interfaces);

    bzero(secgroup, sizeof(gni_secgroup));

    return (0);
}

//!
//! Zero out a VPC structure
//!
//! @param[in] vpc a pointer to the GNI VPC structure to reset
//!
//! @return Always return 0
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_vpc_clear(gni_vpc * vpc)
{
    int i = 0;
    if (!vpc) {
        return (0);
    }

    for (i = 0; i < vpc->max_subnets; i++) {
        EUCA_FREE(vpc->subnets[i].interfaces);
    }
    EUCA_FREE(vpc->subnets);
    EUCA_FREE(vpc->networkAcls);
    for (i = 0; i < vpc->max_routeTables; i++) {
        EUCA_FREE(vpc->routeTables[i].entries);
    }
    EUCA_FREE(vpc->routeTables);
    EUCA_FREE(vpc->natGateways);
    EUCA_FREE(vpc->internetGatewayNames);
    EUCA_FREE(vpc->interfaces);

    bzero(vpc, sizeof(gni_vpc));

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
                return &(vpcs[i]);
            }
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
                return &(vpcsubnets[i]);
            }
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
                return (interfaces[i]);
            }
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
                return &(vpcnatgateways[i]);
            }
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
                return &(vpcroutetables[i]);
            }
        }
    }
    return (NULL);
}

/**
 * Searches and returns the security group that matches the name in the argument, if found.
 * @param gni [in] globalNetworkInfo structure that holds the network state to search.
 * @param name [in] name of the security group of interest.
 * @param startidx [i/o] start index to the array of VPCs in gni. If a matching security group
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
                return &(secgroups[i]);
            }
        }
    }
    return (NULL);
}

//!
//! Validates a given globalNetworkInfo structure and its content
//!
//! @param[in] gni a pointer to the Global Network Information structure to validate
//!
//! @return 0 if the structure is valid or 1 if it isn't
//!
//! @see gni_subnet_validate(), gni_cluster_validate(), gni_instance_validate(), gni_secgroup_validate()
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_validate(globalNetworkInfo * gni)
{
    int i = 0;
    int j = 0;

    // this is going to be messy, until we get XML validation in place
    if (!gni) {
        LOGERROR("invalid input\n");
        return (1);
    }
    // GNI should be initialized... but check just in case
    if (!gni->init) {
        LOGWARN("BUG: gni is not initialized yet\n");
        return (1);
    }
    // Make sure we have a valid mode
    if (gni_netmode_validate(gni->sMode)) {
        if (strlen(gni->sMode) > 0) {
            LOGWARN("Invalid network mode (%s) provided: cannot validate XML\n", gni->sMode);
        } else {
            LOGDEBUG("Empty network mode provided: cannot validate XML\n");
        }
        return (1);
    }

    LOGDEBUG("Validating XML for '%s' networking mode.\n", gni->sMode);

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
    if (!gni->max_public_ips || !gni->public_ips) {
        LOGTRACE("no public_ips set: cannot validate XML\n");
    } else {
        // Make sure none of the public IPs is 0.0.0.0
        for (i = 0; i < gni->max_public_ips; i++) {
            if (gni->public_ips[i] == 0) {
                LOGWARN("empty public_ip set at idx %d: cannot validate XML\n", i);
                return (1);
            }
        }
    }

    // Now we have different behavior between managed and managed-novlan
    if (!strcmp(gni->sMode, NETMODE_MANAGED) || !strcmp(gni->sMode, NETMODE_MANAGED_NOVLAN)) {
        // We must have 1 managed subnet declaration
        if ((gni->max_managedSubnets != 1) || !gni->subnets) {
            LOGWARN("invalid number of managed subnets set '%d'.\n", gni->max_managedSubnets);
            return (1);
        }
        // Validate our managed subnet
        if (gni_managed_subnet_validate(gni->managedSubnet)) {
            LOGWARN("invalid managed subnet: cannot validate XML\n");
            return (1);
        }
        // Validate the clusters
        if (!gni->max_clusters || !gni->clusters) {
            LOGTRACE("no clusters set\n");
        } else {
            for (i = 0; i < gni->max_clusters; i++) {
                if (gni_cluster_validate(&(gni->clusters[i]), TRUE)) {
                    LOGWARN("invalid clusters set at idx %d: cannot validate XML\n", i);
                    return (1);
                }
            }
        }
    } else {
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

        // Validate the clusters
        if (!gni->max_clusters || !gni->clusters) {
            LOGTRACE("no clusters set\n");
        } else {
            for (i = 0; i < gni->max_clusters; i++) {
                if (gni_cluster_validate(&(gni->clusters[i]), FALSE)) {
                    LOGWARN("invalid clusters set at idx %d: cannot validate XML\n", i);
                    return (1);
                }
            }
        }
    }

    // If we have any instance provided, validate them
    if (!gni->max_instances || !gni->instances) {
        LOGTRACE("no instances set\n");
    } else {
        for (i = 0; i < gni->max_instances; i++) {
            if (gni_instance_validate(&(gni->instances[i]))) {
                LOGWARN("invalid instances set at idx %d: cannot validate XML\n", i);
                return (1);
            }
        }
    }

    // Validate interfaces
    if (!gni->max_interfaces || !gni->interfaces) {
        LOGTRACE("no interfaces set\n");
    } else {
        for (i = 0; i < gni->max_interfaces; i++) {
            if (gni_interface_validate(&(gni->interfaces[i]))) {
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
        }
    }
    return (0);
}

//!
//! Validate a networking mode provided in the GNI message. The only supported networking
//! mode strings are: EDGE, MANAGED and MANAGED-NOVLAN
//!
//! @param[in] psMode a string pointer to the network mode to validate
//!
//! @return 0 if the mode is valid or 1 if the mode isn't
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_netmode_validate(const char *psMode)
{
    int i = 0;

    //
    // In the globalNetworkInfo structure, the mode is a static string. But just in case
    // some bozo passed us a NULL pointer.
    //
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
        LOGWARN("invalid network mode '%s'\n", psMode);
    } else {
        LOGDEBUG("network mode is empty.\n");
    }
    return (1);
}

//!
//! Validate a gni_subnet structure content
//!
//! @param[in] pSubnet a pointer to the subnet structure to validate
//!
//! @return 0 if the structure is valid or 1 if the structure isn't
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_subnet_validate(gni_subnet * pSubnet)
{
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

//!
//! Validate a gni_subnet structure content
//!
//! @param[in] pSubnet a pointer to the subnet structure to validate
//!
//! @return 0 if the structure is valid or 1 if the structure isn't
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_managed_subnet_validate(gni_managedsubnet * pSubnet)
{
    // Make sure we didn't get a NULL pointer
    if (!pSubnet) {
        LOGERROR("invalid input\n");
        return (1);
    }
    // If any of the subnet or netmask is 0.0.0.0, this is invalid
    if ((pSubnet->subnet == 0) || (pSubnet->netmask == 0)) {
        LOGWARN("invalid managed subnet: subnet=%d netmask=%d\n", pSubnet->subnet, pSubnet->netmask);
        return (1);
    }
    // If the segment size is less than 16 or not a power of 2 than we have a problem
    if ((pSubnet->segmentSize < 16) || ((pSubnet->segmentSize & (pSubnet->segmentSize - 1)) != 0)) {
        LOGWARN("invalid managed subnet: segmentSize=%d\n", pSubnet->segmentSize);
        return (1);
    }
    // If minVlan is less than MIN_VLAN_EUCA or greater than MAX_VLAN_EUCA, we have a problem
    if ((pSubnet->minVlan < MIN_VLAN_EUCA) || (pSubnet->minVlan > MAX_VLAN_EUCA)) {
        LOGWARN("invalid managed subnet: minVlan=%d\n", pSubnet->minVlan);
        return (1);
    }
    // If maxVlan is less than MIN_VLAN_EUCA or greater than MAX_VLAN_EUCA, we have a problem
    if ((pSubnet->maxVlan < MIN_VLAN_EUCA) || (pSubnet->maxVlan > MAX_VLAN_EUCA)) {
        LOGWARN("invalid managed subnet: maxVlan=%d\n", pSubnet->maxVlan);
        return (1);
    }
    // If minVlan is greater than maxVlan, we have a problem too!!
    if (pSubnet->minVlan > pSubnet->maxVlan) {
        LOGWARN("invalid managed subnet: minVlan=%d, maxVlan=%d\n", pSubnet->minVlan, pSubnet->maxVlan);
        return (1);
    }
    return (0);
}

//!
//! Validate a gni_cluster structure content
//!
//! @param[in] cluster a pointer to the cluster structure to validate
//! @param[in] isManaged set to TRUE if this is a MANAGED style cluster or FALSE for EDGE
//!
//! @return 0 if the structure is valid or 1 if it isn't
//!
//! @see gni_node_validate()
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_cluster_validate(gni_cluster * cluster, boolean isManaged)
{
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
    // For non-MANAGED modes, we need to validate the subnet and the private IPs which
    // aren't provided in MANAGED mode
    //
    if (!isManaged) {
        // Validate the given private subnet
        if (gni_subnet_validate(&(cluster->private_subnet))) {
            LOGWARN("cluster %s: invalid cluster private_subnet\n", cluster->name);
            return (1);
        }
        // Validate the list of private IPs. We must have some.
        if (!cluster->max_private_ips || !cluster->private_ips) {
            LOGWARN("cluster %s: no private_ips\n", cluster->name);
            return (1);
        } else {
            // None of our private IPs should be 0.0.0.0
            for (i = 0; i < cluster->max_private_ips; i++) {
                if (cluster->private_ips[i] == 0) {
                    LOGWARN("cluster %s: empty private_ips set at idx %d\n", cluster->name, i);
                    return (1);
                }
            }
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

//!
//! Validate a gni_node structure content
//!
//! @param[in] node a pointer to the node structure to validate
//!
//! @return 0 if the structure is valid or 1 if it isn't
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_node_validate(gni_node * node)
{
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

//!
//! Validates a given instance_interface structure content for a valid instance
//! description
//!
//! @param[in] instance a pointer to the instance_interface structure to validate
//!
//! @return 0 if the structure is valid or 1 if it isn't
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_instance_validate(gni_instance * instance)
{
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
        LOGDEBUG("instance %s: no publicIp set (ignore if instance was run with private only addressing)\n", instance->name);
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

//!
//! Validates a given gni_instance_interface structure content for a valid interface
//! description.
//!
//! @param[in] interface a pointer to the instance_interface structure to validate
//!
//! @return 0 if the structure is valid or 1 if it isn't
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_interface_validate(gni_instance *interface)
{
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
        LOGDEBUG("instance %s: no publicIp set (ignore if instance was run with private only addressing)\n", interface->name);
    }

    if (!interface->privateIp) {
        LOGWARN("instance %s: no privateIp\n", interface->name);
        return (1);
    }

    if (!interface->max_secgroup_names || !interface->secgroup_names) {
        LOGDEBUG("instance %s: no secgroups\n", interface->name);
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

//!
//! Validates a given gni_secgroup structure content
//!
//! @param[in] secgroup a pointer to the security group structure to validate
//!
//! @return 0 if the structure is valid and 1 if the structure isn't
//!
//! @see gni_secgroup
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gni_secgroup_validate(gni_secgroup * secgroup)
{
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

    if (!secgroup->max_grouprules || !secgroup->grouprules) {
        LOGTRACE("secgroup %s: no secgroup rules\n", secgroup->name);
    } else {
        for (i = 0; i < secgroup->max_grouprules; i++) {
            if (!strlen(secgroup->grouprules[i].name)) {
                LOGWARN("secgroup %s: empty grouprules set at idx %d\n", secgroup->name, i);
                return (1);
            }
        }
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
 * @see
 *
 * @pre
 *
 * @post
 *
 * @note
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
 * @param vpc [in] a pointer to the vpcsubnet structure to validate
 *
 * @return 0 if the structure is valid and 1 if the structure isn't
 *
 * @see
 *
 * @pre
 *
 * @post
 *
 * @note
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
 * @param vpc [in] a pointer to the nat_gateway structure to validate
 *
 * @return 0 if the structure is valid and 1 if the structure isn't
 *
 * @see
 *
 * @pre
 *
 * @post
 *
 * @note
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
        LOGDEBUG("natg %s: no publicIp set (ignore if natg was run with private only addressing)\n", natg->name);
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
 * @param vpc [in] a pointer to the route_table structure to validate
 *
 * @return 0 if the structure is valid and 1 if the structure isn't
 *
 * @see
 *
 * @pre
 *
 * @post
 *
 * @note
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

//!
//! Logs the contents of an instance_interface structure.
//!
//! @param[in] inst instance_interface of interest.
//! @param[in] loglevel valid value from log_level_e enumeration.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
void gni_instance_interface_print(gni_instance *inst, int loglevel)
{
    char *mac = NULL;
    char *pubip = NULL;
    char *privip = NULL;
    hex2mac(inst->macAddress, &mac);
    pubip = hex2dot(inst->publicIp);
    privip = hex2dot(inst->privateIp);
    int i = 0;
    
    if (!inst) {
        EUCALOG(loglevel, "Invalid argument: NULL.\n");
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
    if (strstr(inst->name, "eni-")) {
        EUCALOG(loglevel, "\tinstance     = %s\n", inst->instance_name.name);
        EUCALOG(loglevel, "\tsrcdstcheck  = %s\n", inst->srcdstcheck ? "true" : "false");
        EUCALOG(loglevel, "\tdeviceidx    = %d\n", inst->deviceidx);
    } else {
        for (i = 0; i < inst->max_interface_names; i++) {
            EUCALOG(loglevel, "\tinterface[%d] = %s\n", i, inst->interface_names[i].name);
        }
    }
    for (i = 0; i < inst->max_secgroup_names; i++) {
        EUCALOG(loglevel, "\tsg[%d]        = %s\n", i, inst->secgroup_names[i].name);
    }
    EUCA_FREE(mac);
    EUCA_FREE(pubip);
    EUCA_FREE(privip);
}

//!
//! Logs the contents of an instance_interface structure.
//!
//! @param[in] inst instance_interface of interest.
//! @param[in] loglevel valid value from log_level_e enumeration.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
void gni_sg_print(gni_secgroup *sg, int loglevel)
{
    int i = 0;
    
    if (!sg) {
        EUCALOG(loglevel, "Invalid argument: NULL.\n");
    }
    EUCALOG(loglevel, "------ name = %s -----\n", sg->name);
    EUCALOG(loglevel, "\taccountId    = %s\n", sg->accountId);
    EUCALOG(loglevel, "\tgrouprules   = %d rules\n", sg->max_grouprules);
    EUCALOG(loglevel, "\tingress      = %d rules\n", sg->max_ingress_rules);
    EUCALOG(loglevel, "\tegress       = %d rules\n", sg->max_egress_rules);
    for (i = 0; i < sg->max_instances; i++) {
        EUCALOG(loglevel, "\tinstance[%d] = %s\n", i, sg->instances[i]->name);
    }
    for (i = 0; i < sg->max_interfaces; i++) {
        EUCALOG(loglevel, "\tinterface[%d] = %s\n", i, sg->interfaces[i]->name);
    }
}

gni_hostname_info *gni_init_hostname_info(void)
{
    gni_hostname_info *hni = EUCA_ZALLOC(1,sizeof(gni_hostname_info));
    hni->max_hostnames = 0;
    return (hni);
}

int gni_hostnames_print(gni_hostname_info *host_info)
{
    int i;

    LOGTRACE("Cached Hostname Info: \n");
    for (i = 0; i < host_info->max_hostnames; i++) {
        LOGTRACE("IP Address: %s Hostname: %s\n",inet_ntoa(host_info->hostnames[i].ip_address),host_info->hostnames[i].hostname);
    }
    return (0);
}

int gni_hostnames_free(gni_hostname_info *host_info)
{
    if (!host_info) {
        return (0);
    }

    EUCA_FREE(host_info->hostnames);
    EUCA_FREE(host_info);
    return (0);
}

int gni_hostnames_get_hostname(gni_hostname_info  *hostinfo, const char *ip_address, char **hostname)
{
    struct in_addr addr;
    gni_hostname key;
    gni_hostname *bsearch_result;

    if (!hostinfo) {
        return (1);
    }

    if (inet_aton(ip_address, &addr)) {
        key.ip_address.s_addr = addr.s_addr; // search by ip
        bsearch_result = bsearch(&key, hostinfo->hostnames, hostinfo->max_hostnames,sizeof(gni_hostname), cmpipaddr);

        if (bsearch_result) {
            *hostname = strdup(bsearch_result->hostname);
            LOGTRACE("bsearch hit: %s\n",*hostname);
            return (0);
        }
    } else {
        LOGTRACE("INET_ATON FAILED FOR: %s\n",ip_address); // we were passed a hostname
    }
    return (1);
}

//
// Used for qsort and bsearch methods against gni_hostname_info
//
int cmpipaddr(const void *p1, const void *p2)
{
    gni_hostname *hp1 = (gni_hostname *) p1;
    gni_hostname *hp2 = (gni_hostname *) p2;

    if (hp1->ip_address.s_addr == hp2->ip_address.s_addr)
        return 0;
    if (hp1->ip_address.s_addr < hp2->ip_address.s_addr)
        return -1;
    else
        return 1;
}

//!
//! Compares two gni_vpc structures in the argument.
//!
//! @param[in] a gni_vpc structure of interest.
//! @param[in] b gni_vpc structure of interest.
//! @return 0 if name and number of entries match. Non-zero otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
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
    if (strcmp(a->dhcpOptionSet, b->dhcpOptionSet)) {
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

//!
//! Compares two gni_vpcsubnet structures in the argument.
//!
//! @param[in] a gni_vpcsubnet structure of interest.
//! @param[in] b gni_vpcsubnet structure of interest.
//! @return 0 if name, routeTable_name and networkAcl_name match. Non-zero otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int cmp_gni_vpcsubnet(gni_vpcsubnet *a, gni_vpcsubnet *b) {
    if (a == b) {
        return (0);
    }
    if ((a == NULL) || (b == NULL)) {
        return (1);
    }
    if (strcmp(a->name, b->name)) {
        return (1);
    }
    if ((!strcmp(a->routeTable_name, b->routeTable_name)) &&
            (!strcmp(a->networkAcl_name, b->networkAcl_name)) &&
            (!cmp_gni_route_table(a->routeTable, b->routeTable))) {
        return (0);
    }
    return (1);
}

//!
//! Compares two gni_nat_gateway structures in the argument.
//!
//! @param[in] a gni_nat_gateway structure of interest.
//! @param[in] b gni_nat_gateway structure of interest.
//! @return 0 if name matches. Non-zero otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
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

//!
//! Compares two gni_route_table structures in the argument.
//!
//! @param[in] a gni_route_table structure of interest. Check for route entries
//!            applied flags.
//! @param[in] b gni_route_table structure of interest.
//! @return 0 if name and route entries match. Non-zero otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
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
        if (a->entries[i].applied == 0) {
            return (1);
        }
        if ((strcmp(a->entries[i].destCidr, b->entries[i].destCidr)) ||
                (strcmp(a->entries[i].target, b->entries[i].target))) {
            return (1);
        }
    }
    return (0);
}

//!
//! Compares two gni_secgroup structures in the argument.
//!
//! @param[in]  a gni_secgroup structure of interest.
//! @param[in]  b gni_secgroup structure of interest.
//! @param[out] ingress_diff set to 1 iff ingress rules of a and b differ.
//! @param[out] egress_diff set to 1 iff egress rules of a and b differ.
//! @param[out] interfaces_diff set to 1 iff member interfaces of a and b differ.
//! @return 0 if name and rule entries match. Non-zero otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note order of rules are assumed to be the same for both a and b.
//!
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
    }

    if (abmatch) {
        return (0);
    }
    return (1);
}

//!
//! Compares two gni_interface structures in the argument.
//!
//! @param[in]  a gni_interface structure of interest.
//! @param[in]  b gni_interface structure of interest.
//! @param[out] pubip_diff set to 1 iff public IP of a and b differ.
//! @param[out] sdc_diff set to 1 iff src/dst check flag of a and b differ.
//! @param[out] host_diff set to 1 iff host/node of a and b differ.
//! @param[out] sg_diff set to 1 iff list of security group names of a and b differ.
//! @return 0 if name and other properties of a and b match. Non-zero otherwise.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note order of rules are assumed to be the same for both a and b.
//!
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
