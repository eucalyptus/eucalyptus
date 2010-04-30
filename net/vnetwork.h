/*
Copyright (c) 2009  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include <string.h>
#include <stdint.h>
#include <stdarg.h>
#include <eucalyptus.h>
#include <linux/limits.h>

#ifndef INCLUDE_VNETWORK_H
#define INCLUDE_VNETWORK_H

#ifndef MAX_PATH
#define MAX_PATH 4096
#endif

#define NUMBER_OF_VLANS 4096
#define NUMBER_OF_HOSTS_PER_VLAN 2048
#define NUMBER_OF_PUBLIC_IPS 2048
#define NUMBER_OF_CCS 8

typedef struct netEntry_t {
  unsigned char mac[6];
  char active;
  uint32_t ip;
} netEntry;

typedef struct userEntry_t {
  char netName[32];
  char userName[32];
} userEntry;

typedef struct networkEntry_t {
  int numhosts;
  char active;
  uint32_t nw, nm, bc, dns, router;
  netEntry addrs[NUMBER_OF_HOSTS_PER_VLAN];
} networkEntry;

typedef struct publicip_t {
  uint32_t ip;
  uint32_t dstip;
  int allocated;
} publicip;

typedef struct tunnelData_t {
  int localIpId;
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
  //  char localIp[32];
  uint32_t localIps[32];
  uint32_t nw, nm, euca_ns, cloudIp;
  int role;
  int enabled;
  int initialized;
  int numaddrs;
  int max_vlan;
  tunnelData tunnels;
  char etherdevs[NUMBER_OF_VLANS][16];
  userEntry users[NUMBER_OF_VLANS];
  networkEntry networks[NUMBER_OF_VLANS];
  publicip publicips[NUMBER_OF_PUBLIC_IPS];
  char iptables[32768];
} vnetConfig;

enum {NC, CC, CLC};
void vnetInit(vnetConfig *vnetconfig, char *mode, char *eucapath, char *path, int role, char *pubInterface, char *privInterface, char *numberofaddrs, char *network, char *netmask, char *broadcast, char *dns, char *router, char *daemon, char *dhcpuser, char *bridgedev, char *localIp, char *cloudIp);

int vnetStartNetwork(vnetConfig *vnetconfig, int vlan, char *userName, char *netName, char **outbrname);
int vnetStopNetwork(vnetConfig *vnetconfig, int vlan, char *userName, char *netName);
int vnetAddHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan, int idx);
int vnetDelHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan);
int vnetEnableHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan);
int vnetDisableHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan);
int vnetGetNextHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan, int idx);

int vnetGenerateNetworkParams(vnetConfig *vnetconfig, char *instId, int vlan, int nidx, char *outmac, char *outpubip, char *outprivip);

int vnetAddDev(vnetConfig *vnetconfig, char *dev);
int vnetDelDev(vnetConfig *vnetconfig, char *dev);
int vnetAddLocalIP(vnetConfig *vnetconfig, uint32_t ip);
int vnetCheckLocalIP(vnetConfig *vnetconfig, uint32_t ip);
int vnetCountLocalIP(vnetConfig *vnetconfig);

int vnetGenerateDHCP(vnetConfig *vnetconfig, int *numHosts);
int vnetKickDHCP(vnetConfig *vnetconfig);

int vnetSetVlan(vnetConfig *vnetconfig, int vlan, char *user, char *network);
int vnetGetVlan(vnetConfig *vnetconfig, char *user, char *network);

int vnetSetCCS(vnetConfig *vnetconfig, char **ccs, int ccsLen);
int vnetAddCCS(vnetConfig *vnetconfig, uint32_t cc);
int vnetDelCCS(vnetConfig *vnetconfig, uint32_t cc);

int vnetInitTunnels(vnetConfig *vnetconfig);
int vnetSetupTunnels(vnetConfig *vnetconfig);
int vnetSetupTunnelsVTUN(vnetConfig *vnetconfig);
int vnetTeardownTunnels(vnetConfig *vnetconfig);
int vnetTeardownTunnelsVTUN(vnetConfig *vnetconfig);
int vnetAttachTunnels(vnetConfig *vnetconfig, int vlan, char *newbrname);
int vnetDetachTunnels(vnetConfig *vnetconfig, int vlan, char *newbrname);

int vnetTableRule(vnetConfig *vnetconfig, char *type, char *destUserName, char *destName, char *sourceUserName, char *sourceNet, char *sourceNetName, char *protocol, int minPort, int maxPort);
int vnetCreateChain(vnetConfig *vnetconfig, char *userName, char *netName);
int vnetFlushTable(vnetConfig *vnetconfig, char *userName, char *netName);
int vnetRestoreTablesFromMemory(vnetConfig *vnetconfig);
int vnetSaveTablesToMemory(vnetConfig *vnetconfig);

int vnetAddPublicIP(vnetConfig *vnetconfig, char *ip);
int vnetCheckPublicIP(vnetConfig *vnetconfig, char *ip);
int vnetAllocatePublicIP(vnetConfig *vnetconfig, char *ip, char *dstip);
int vnetDeallocatePublicIP(vnetConfig *vnetconfig, char *ip, char *dstip);
int vnetSetPublicIP(vnetConfig *vnetconfig, char *ip, char *dstip, int setval);
int vnetGetPublicIP(vnetConfig *vnetconfig, char *ip, char **dstip, int *allocated, int *addrdevno);

int vnetAssignAddress(vnetConfig *vnetconfig, char *src, char *dst);
int vnetUnassignAddress(vnetConfig *vnetconfig, char *src, char *dst);

int vnetAddGatewayIP(vnetConfig *vnetconfig, int vlan, char *devname);
int vnetDelGatewayIP(vnetConfig *vnetconfig, int vlan, char *devname);

// linux managed mode driver
int vnetStartNetworkManaged(vnetConfig *vnetconfig, int vlan, char *userName, char *netName, char **outbrname);
int vnetStopNetworkManaged(vnetConfig *vnetconfig, int vlan, char *userName, char *netName);

// helper functions
int vnetSaveIPTables(vnetConfig *vnetconfig);
int vnetLoadIPTables(vnetConfig *vnetconfig);
int vnetApplySingleTableRule(vnetConfig *vnetconfig, char *table, char *rule);
int vnetSetMetadataRedirect(vnetConfig *vnetconfig, char *network, int slashnet);

char *host2ip(char *host);
char *hex2dot(uint32_t in);
uint32_t dot2hex(char *in);
int mac2ip(vnetConfig *vnetconfig, char *mac, char **ip);
int ip2mac(vnetConfig *vnetconfig, char *ip, char **mac);
void mac2hex(char *in, unsigned char out[6]);
void hex2mac(unsigned char in[6], char **out);
int maczero(unsigned char in[6]);
int machexcmp(char *ina, unsigned char inb[6]);

int instId2mac(char *instId, char *outmac);
int zeromac(unsigned char in[6]);
int maccmp(char *ina, unsigned char inb[6]);
int getdevinfo(char *dev, uint32_t **outips, uint32_t **outnms, int *len);


int check_chain(vnetConfig *vnetconfig, char *userName, char *netName);
int check_device(char *dev);
int check_deviceup(char *dev);
int check_bridge(char *dev);
int check_bridgedev(char *br, char *dev);
int check_bridgestp(char *br);
int check_tablerule(vnetConfig *vnetconfig, char *table, char *rule);
int check_isip(char *ip);

#endif
