#include <string.h>
#include <stdint.h>
#include <stdarg.h>
#include <eucalyptus.h>

#ifndef INCLUDE_VNETWORK_H
#define INCLUDE_VNETWORK_H

#define NUMBER_OF_VLANS 4096
#define NUMBER_OF_HOSTS_PER_VLAN 256
#define NUMBER_OF_PUBLIC_IPS 256

typedef struct netEntry_t {
  char mac[24];
  uint32_t ip;
  int active;
} netEntry;

typedef struct userEntry_t {
  char netName[32];
  char userName[32];
} userEntry;

typedef struct networkEntry_t {
  int numhosts;
  uint32_t nw, nm, bc, dns, router;
  netEntry addrs[NUMBER_OF_HOSTS_PER_VLAN];
} networkEntry;

typedef struct publicip_t {
  uint32_t ip;
  uint32_t dstip;
  int allocated;
} publicip;

typedef struct vnetConfig_t {
  char eucahome[1024];
  char path[1024];
  char dhcpdaemon[1024];
  char dhcpuser[32];
  char pubInterface[32];
  char bridgedev[32];
  char mode[32];
  int role;
  int enabled;
  int initialized;
  int numaddrs;
  int max_vlan;
  char etherdevs[NUMBER_OF_VLANS][32];
  userEntry users[NUMBER_OF_VLANS];
  networkEntry networks[NUMBER_OF_VLANS];
  publicip publicips[NUMBER_OF_PUBLIC_IPS];
  char iptables[32768];
} vnetConfig;

enum {NC, CC, CLC};
void vnetInit(vnetConfig *vnetconfig, char *mode, char *eucapath, char *path, int role, char *pubInterface, char *numberofaddrs, char *network, char *netmask, char *broadcast, char *dns, char *router, char *daemon, char *dhcpuser, char *bridgedev);

int vnetStartNetwork(vnetConfig *vnetconfig, int vlan, char *userName, char *netName, char **outbrname);
int vnetStopNetwork(vnetConfig *vnetconfig, int vlan, char *userName, char *netName);
int vnetAddHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan);
int vnetDelHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan);
int vnetEnableHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan);
int vnetDisableHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan);
int vnetGetNextHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan);

int vnetAddDev(vnetConfig *vnetconfig, char *dev);
int vnetDelDev(vnetConfig *vnetconfig, char *dev);

int vnetGenerateDHCP(vnetConfig *vnetconfig, int *numHosts);
int vnetKickDHCP(vnetConfig *vnetconfig);

int vnetSetVlan(vnetConfig *vnetconfig, int vlan, char *user, char *network);
int vnetGetVlan(vnetConfig *vnetconfig, char *user, char *network);

int vnetTableRule(vnetConfig *vnetconfig, char *type, char *destUserName, char *destName, char *sourceUserName, char *sourceNet, char *sourceNetName, char *protocol, int minPort, int maxPort);
int vnetCreateChain(vnetConfig *vnetconfig, char *userName, char *netName);
int vnetFlushTable(vnetConfig *vnetconfig, char *userName, char *netName);
int vnetRestoreTablesFromMemory(vnetConfig *vnetconfig);
int vnetSaveTablesToMemory(vnetConfig *vnetconfig);

int vnetAddPublicIP(vnetConfig *vnetconfig, char *ip);
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
char *hex2dot(uint32_t in);
uint32_t dot2hex(char *in);
int discover_mac(vnetConfig *vnetconfig, char *mac, char **ip);
int check_chain(vnetConfig *vnetconfig, char *userName, char *netName);
int check_device(char *dev);
int check_bridge(char *dev);


#endif
