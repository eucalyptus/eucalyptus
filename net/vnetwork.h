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

#ifndef INCLUDE_VNETWORK_H
#define INCLUDE_VNETWORK_H

//!
//! @file
//! Defines the Virtual Network library.
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <string.h>
#include <stdint.h>
#include <stdarg.h>
#include <eucalyptus.h>
#include <linux/limits.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifndef MAX_PATH
#define MAX_PATH                                 4096
#endif /* ! MAX_PATH */

#define NUMBER_OF_VLANS                          4096
#define NUMBER_OF_HOSTS_PER_VLAN                 2048
#define NUMBER_OF_PUBLIC_IPS                     2048
#define NUMBER_OF_CCS                               8
#define MAX_ETH_DEV_PATH                           16

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
    NC,
    CC,
    CLC
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct netEntry_t {
    unsigned char mac[6];
    char active;
    uint32_t ip;
} netEntry;

typedef struct userEntry_t {
    char netName[64];
    char userName[48];
    char uuid[48];
} userEntry;

typedef struct networkEntry_t {
    int numhosts;
    char active;
    uint32_t nw;
    uint32_t nm;
    uint32_t bc;
    uint32_t dns;
    uint32_t router;
    netEntry addrs[NUMBER_OF_HOSTS_PER_VLAN];
} networkEntry;

typedef struct publicip_t {
    uint32_t ip;
    uint32_t dstip;
    int allocated;
    char uuid[48];
} publicip;

typedef struct tunnelData_t {
    int localIpId;
    int localIpIdLast;
    uint32_t ccs[NUMBER_OF_CCS];
    time_t ccsTunnelStart[NUMBER_OF_CCS];
    time_t tunpassMtime;
    int tunneling;
} tunnelData;

typedef struct vnetConfig_t {
    char eucahome[MAX_PATH];
    char path[MAX_PATH];
    char dhcpdaemon[MAX_PATH];
    char dhcpuser[32];
    char pubInterface[32];
    char privInterface[32];
    char bridgedev[32];
    char mode[32];
    char macPrefix[6];
    uint32_t localIps[32];
    uint32_t nw;
    uint32_t nm;
    uint32_t euca_ns;
    uint32_t cloudIp;
    char euca_domainname[256];
    int role;
    int enabled;
    int initialized;
    int numaddrs;
    int addrIndexMin;
    int addrIndexMax;
    int max_vlan;
    tunnelData tunnels;
    char etherdevs[NUMBER_OF_VLANS][MAX_ETH_DEV_PATH];
    userEntry users[NUMBER_OF_VLANS];
    networkEntry networks[NUMBER_OF_VLANS];
    publicip publicips[NUMBER_OF_PUBLIC_IPS];
    char iptables[4194304];
} vnetConfig;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int vnetInit(vnetConfig * vnetconfig, char *mode, char *eucahome, char *path, int role, char *pubInterface, char *privInterface, char *numberofaddrs,
             char *network, char *netmask, char *broadcast, char *nameserver, char *domainname, char *router, char *daemon, char *dhcpuser,
             char *bridgedev, char *localIp, char *macPrefix);

int vnetSetMetadataRedirect(vnetConfig * vnetconfig);
int vnetUnsetMetadataRedirect(vnetConfig * vnetconfig);

int vnetInitTunnels(vnetConfig * vnetconfig);

int vnetAddHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan, int idx);
int vnetDelHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan);
int vnetRefreshHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan, int idx);
int vnetEnableHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan);
int vnetDisableHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan);

int vnetDeleteChain(vnetConfig * vnetconfig, char *userName, char *netName);
int vnetCreateChain(vnetConfig * vnetconfig, char *userName, char *netName);
int vnetSaveTablesToMemory(vnetConfig * vnetconfig);
int vnetRestoreTablesFromMemory(vnetConfig * vnetconfig);
int vnetFlushTable(vnetConfig * vnetconfig, char *userName, char *netName);
int vnetApplySingleEBTableRule(vnetConfig * vnetconfig, char *table, char *rule);
int vnetApplySingleTableRule(vnetConfig * vnetconfig, char *table, char *rule);
int vnetTableRule(vnetConfig * vnetconfig, char *type, char *destUserName, char *destName, char *sourceUserName, char *sourceNet, char *sourceNetName,
                  char *protocol, int minPort, int maxPort);

int vnetSetVlan(vnetConfig * vnetconfig, int vlan, char *uuid, char *user, char *network);
int vnetGetVlan(vnetConfig * vnetconfig, char *user, char *network);
int vnetGetAllVlans(vnetConfig * vnetconfig, char ***outusers, char ***outnets, int *len);

int vnetGenerateNetworkParams(vnetConfig * vnetconfig, char *instId, int vlan, int nidx, char *outmac, char *outpubip, char *outprivip);
int vnetGetNextHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan, int idx);
int vnetCountLocalIP(vnetConfig * vnetconfig);
int vnetCheckLocalIP(vnetConfig * vnetconfig, uint32_t ip);
int vnetAddLocalIP(vnetConfig * vnetconfig, uint32_t ip);

int vnetAddDev(vnetConfig * vnetconfig, char *dev);
int vnetDelDev(vnetConfig * vnetconfig, char *dev);

int vnetGenerateDHCP(vnetConfig * vnetconfig, int *numHosts);
int vnetKickDHCP(vnetConfig * vnetconfig);

int vnetAddCCS(vnetConfig * vnetconfig, uint32_t cc);
int vnetDelCCS(vnetConfig * vnetconfig, uint32_t cc);
int vnetSetCCS(vnetConfig * vnetconfig, char **ccs, int ccsLen);

int vnetStartInstanceNetwork(vnetConfig * vnetconfig, int vlan, char *publicIp, char *privateIp, char *macaddr);
int vnetStopInstanceNetwork(vnetConfig * vnetconfig, int vlan, char *publicIp, char *privateIp, char *macaddr);
int vnetStartNetworkManaged(vnetConfig * vnetconfig, int vlan, char *uuid, char *userName, char *netName, char **outbrname);

int vnetAttachTunnels(vnetConfig * vnetconfig, int vlan, char *newbrname);
int vnetDetachTunnels(vnetConfig * vnetconfig, int vlan, char *newbrname);
int vnetTeardownTunnels(vnetConfig * vnetconfig);
int vnetTeardownTunnelsVTUN(vnetConfig * vnetconfig);
int vnetSetupTunnels(vnetConfig * vnetconfig);
int vnetSetupTunnelsVTUN(vnetConfig * vnetconfig);

int vnetAddGatewayIP(vnetConfig * vnetconfig, int vlan, char *devname, int localIpId);
int vnetApplyArpTableRules(vnetConfig * vnetconfig);
int vnetDelGatewayIP(vnetConfig * vnetconfig, int vlan, char *devname, int localIpId);

int vnetStopNetworkManaged(vnetConfig * vnetconfig, int vlan, char *userName, char *netName);
int vnetStartNetwork(vnetConfig * vnetconfig, int vlan, char *uuid, char *userName, char *netName, char **outbrname);

int vnetGetPublicIP(vnetConfig * vnetconfig, char *ip, char **dstip, int *allocated, int *addrdevno);
int vnetCheckPublicIP(vnetConfig * vnetconfig, char *ip);
int vnetAddPublicIP(vnetConfig * vnetconfig, char *inip);
int vnetAssignAddress(vnetConfig * vnetconfig, char *src, char *dst);
int vnetAllocatePublicIP(vnetConfig * vnetconfig, char *uuid, char *ip, char *dstip);
int vnetDeallocatePublicIP(vnetConfig * vnetconfig, char *uuid, char *ip, char *dstip);
int vnetSetPublicIP(vnetConfig * vnetconfig, char *uuid, char *ip, char *dstip, int setval);
int vnetReassignAddress(vnetConfig * vnetconfig, char *uuid, char *src, char *dst);
int vnetUnassignAddress(vnetConfig * vnetconfig, char *src, char *dst);

int vnetStopNetwork(vnetConfig * vnetconfig, int vlan, char *userName, char *netName);

int instId2mac(vnetConfig * vnetconfig, char *instId, char *outmac);
int ip2mac(vnetConfig * vnetconfig, char *ip, char **mac);
int mac2ip(vnetConfig * vnetconfig, char *mac, char **ip);
uint32_t dot2hex(char *in);
int getdevinfo(char *dev, uint32_t ** outips, uint32_t ** outnms, int *len);
void hex2mac(unsigned char in[6], char **out);
void mac2hex(char *in, unsigned char out[6]);
int maczero(unsigned char in[6]);
int machexcmp(char *ina, unsigned char inb[6]);
char *hex2dot(uint32_t in);
char *ipdot2macdot(char *ip, char *macprefix);
int vnetLoadIPTables(vnetConfig * vnetconfig);

int check_chain(vnetConfig * vnetconfig, char *userName, char *netName);
int check_deviceup(char *dev);
int check_device(char *dev);
int check_bridgestp(char *br);
int check_bridgedev(char *br, char *dev);
int check_bridge(char *brname);
int check_tablerule(vnetConfig * vnetconfig, char *table, char *rule);
int check_isip(char *ip);
char *host2ip(char *host);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! INCLUDE_VNETWORK_H */
