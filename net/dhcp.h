#ifndef DHCP_H
#define DHCP_H

typedef struct dhcpConfig_t {
  char pubmode[32],
    deamon[1024],
    config[1024],
    pidfile[1024],
    leases[1024],
    trace[1024],
    privSubnet[32],
    privSubnetMask[32],
    privBroadcastAddress[32],
    pubInterface[32],
    pubSubnet[32],
    pubSubnetMask[32],
    pubBroadcastAddress[32],
    pubRouter[32],
    pubDNS[32],
    pubRangeMin[32],
    pubRangeMax[32];
  char etherdevs[128][32];
  char macs[2048][32];
  char ips[2048][32];
  int initialized;
} dhcpConfig;

int initDHCP(dhcpConfig *dhcpconfig, char *path, char *deamon, char *pubmode, char *pubInterface,char *pubSubnet,char *pubSubnetMask,char *pubBroadcastAddress,char *pubRouter,char *pubDNS, char *pubRangeMin, char *pubRangeMax, char *privSubnet, char *privSubnetMask, char *privBroadcastAddress);

int writeDHCPConfig(dhcpConfig *dhcpconfig);

int addDHCPDev(dhcpConfig *dhcpconfig, char *dev); 
int delDHCPDev(dhcpConfig *dhcpconfig, char *dev);

int addDHCPHost(dhcpConfig *dhcpconfig, char *hostmac, char *hostip); 
int delDHCPHost(dhcpConfig *dhcpconfig, char *hostmac, char *hostip);

int kickDHCPDaemon(dhcpConfig *dhcpconfig);

#endif
