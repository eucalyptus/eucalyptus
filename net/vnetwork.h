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

#define NUMBER_OF_VLANS                          4096
#define NUMBER_OF_HOSTS_PER_VLAN                 2048
#define NUMBER_OF_PUBLIC_IPS                     2048
#define NUMBER_OF_PRIVATE_IPS                    2048
#define NUMBER_OF_CCS                               8
#define NUMBER_OF_NAME_SERVERS                     32
#define MAX_ETH_DEV_PATH                           16
#define MAX_SEC_GROUPS                           NUMBER_OF_VLANS

#define LOCALHOST_HEX                            0x7F000001
#define LOCALHOST_STRING                         "127.0.0.1"

//! @{
//! @name Defines the various supported network mode names

#define NETMODE_EDGE                             "EDGE"
#define NETMODE_STATIC                           "STATIC"
#define NETMODE_SYSTEM                           "SYSTEM"
#define NETMODE_MANAGED                          "MANAGED"
#define NETMODE_MANAGED_NOVLAN                   "MANAGED-NOVLAN"

//! @}

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

//! Defines a list of eucalyptus components
enum {
    NC,                                //!< Node Controller
    CC,                                //!< Cluster Controller
    CLC,                               //!< Cloud Controller
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct netEntry_t {
    u8 mac[6];
    char active;
    u32 ip;
} netEntry;

typedef struct userEntry_t {
    char netName[64];
    char userName[48];
    char uuid[48];
} userEntry;

typedef struct networkEntry_t {
    int numhosts;
    char active;
    u32 nw;
    u32 nm;
    u32 bc;
    u32 dns;
    u32 router;
    netEntry addrs[NUMBER_OF_HOSTS_PER_VLAN];
    time_t createTime;
} networkEntry;

typedef struct publicip_t {
    u32 ip;
    u32 dstip;
    int allocated;
    char uuid[48];
} publicip;

typedef struct tunnelData_t {
    int localIpId;
    int localIpIdLast;
    u32 ccs[NUMBER_OF_CCS];
    time_t ccsTunnelStart[NUMBER_OF_CCS];
    time_t tunpassMtime;
    int tunneling;
} tunnelData;

typedef struct vnetConfig_t {
    char eucahome[EUCA_MAX_PATH];      //!< Home path for the eucalyptus installation
    char path[EUCA_MAX_PATH];          //!< Path to the VNET run dir (e.g. /opt/eucalyptus/var/run/eucalyptus/net)
    char dhcpdaemon[EUCA_MAX_PATH];    //!< Name of the DHCP daemon application
    char dhcpuser[32];                 //!< System DHCP user
    char pubInterface[32];             //!< Name of the public interface
    char privInterface[32];            //!< Name of the private interface
    char bridgedev[32];                //!< Name of the bridge device
    char mode[32];                     //!< Networking mode name (e.g. STATIC, EDGE, MANAGED, etc.)
    char macPrefix[6];                 //!< Mac prefix to combine with the host IP
    u32 localIps[32];                  //!<
    u32 nw;                            //!< this is the configured network for this virtual network
    u32 nm;                            //!< this is the network mask for this virtual network
    u32 eucaNameServer[NUMBER_OF_NAME_SERVERS]; //!< This is the list of DNS servers received from CLC for this vitual network
    u32 cloudIp;                       //!< This is the IP of the CLC communicating with us
    char eucaDomainName[256];          //!< This is the domain name configured by the CLC for this virtual network
    int role;                          //!< This is the role of this network (i.e. CLC, CC or NC)
    boolean enabled;                   //!< Set to TRUE if this virtual network is enabled. Othersize set to FALSE
    boolean initialized;               //!< Set to TRUE if this virtual network is initialized properly. Otherwise set to FALSE
    int numaddrs;
    int addrIndexMin;
    int addrIndexMax;
    int max_vlan;
    tunnelData tunnels;
    char etherdevs[NUMBER_OF_VLANS][MAX_ETH_DEV_PATH];
    userEntry users[NUMBER_OF_VLANS];
    networkEntry networks[NUMBER_OF_VLANS];
    publicip publicips[NUMBER_OF_PUBLIC_IPS];
    publicip privateips[NUMBER_OF_PRIVATE_IPS];
    char iptables[4194304];
} vnetConfig;

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

int vnetInit(vnetConfig * vnetconfig, char *mode, char *eucahome, char *path, int role, char *pubInterface, char *privInterface, char *numberofaddrs,
             char *network, char *netmask, char *broadcast, char *nameserver, char *domainname, char *router, char *daemon, char *dhcpuser,
             char *bridgedev, char *localIp, char *macPrefix);

int vnetIptReInit(vnetConfig * pVnetCfg, boolean isActive);

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
int vnetCheckLocalIP(vnetConfig * vnetconfig, u32 ip);
int vnetAddLocalIP(vnetConfig * vnetconfig, u32 ip);

int vnetAddDev(vnetConfig * vnetconfig, char *dev);
int vnetDelDev(vnetConfig * vnetconfig, char *dev);

int vnetGenerateDHCP(vnetConfig * vnetconfig, int *numHosts);
int vnetKickDHCP(vnetConfig * vnetconfig);

int vnetAddCCS(vnetConfig * vnetconfig, u32 cc);
int vnetDelCCS(vnetConfig * vnetconfig, u32 cc);
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
int vnetAddPrivateIP(vnetConfig * vnetconfig, char *inip);
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
int getdevinfo(char *dev, u32 ** outips, u32 ** outnms, int *len);
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

#endif /* ! INCLUDE_VNETWORK_H */
