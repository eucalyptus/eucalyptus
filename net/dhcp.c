#include <stdio.h>
#include <stdlib.h>
#include <string.h> /* strdup */
#include <sys/types.h> /* open */
#include <sys/stat.h> /* open */
#include <fcntl.h>  /* open */
#include <errno.h> /* perror */
#include "misc.h"
#include "dhcp.h"

int initDHCP(dhcpConfig *dhcpconfig, char *path, char *deamon, char *pubmode, char *pubInterface, char *pubSubnet, char *pubSubnetMask, char *pubBroadcastAddress, char *pubRouter,char *pubDNS, char *pubRangeMin, char *pubRangeMax, char *privSubnet, char *privSubnetMask, char *privBroadcastAddress) {
  char newpath[1024], cmd[1024];
  
  if (!dhcpconfig) {
    return(1);
  }
  
  if (!dhcpconfig->initialized) {
    
    bzero(dhcpconfig, sizeof(dhcpConfig));
  
    if (pubmode) {
      strncpy(dhcpconfig->pubmode, pubmode, 32);
    } else {
      strncpy(dhcpconfig->pubmode, "SYSTEM", 32);
    }
    
    if (!path) {
      path = strdup("");
    }
    
    snprintf(newpath, 1024, "%s/euca-dhcp.conf", path);
    snprintf(cmd, 1024, "mkdir -p %s; touch %s", path, newpath);
    system(cmd);
    strncpy(dhcpconfig->config, newpath, 1024);
    
    snprintf(newpath, 1024, "%s/euca-dhcp.pid", path);
    snprintf(cmd, 1024, "mkdir -p %s; touch %s", path, newpath);
    system(cmd);
    strncpy(dhcpconfig->pidfile, newpath, 1024);
    
    snprintf(newpath, 1024, "%s/euca-dhcp.leases", path);
    snprintf(cmd, 1024, "mkdir -p %s; touch %s", path, newpath);
    system(cmd);
    strncpy(dhcpconfig->leases, newpath, 1024);
    
    snprintf(newpath, 1024, "%s/euca-dhcp.trace", path);
    snprintf(cmd, 1024, "mkdir -p %s; touch %s", path, newpath);
    system(cmd);
    strncpy(dhcpconfig->trace, newpath, 1024);
    
    if (deamon) {
      strncpy(dhcpconfig->deamon, deamon, 32);
    } else {
      strncpy(dhcpconfig->deamon, "/usr/sbin/dhcpd", 32);
    }
    
    if (privSubnet) {
      strncpy(dhcpconfig->privSubnet, privSubnet, 32);
    } else {
      strncpy(dhcpconfig->privSubnet, "192.168.0.0", 32);
    }
    
    if (privSubnetMask) {
      strncpy(dhcpconfig->privSubnetMask, privSubnetMask, 32);
    } else {
      strncpy(dhcpconfig->privSubnetMask, "255.255.0.0", 32);
    }
    
    if (privBroadcastAddress) {
      strncpy(dhcpconfig->privBroadcastAddress, privBroadcastAddress, 32);
    } else {
      strncpy(dhcpconfig->privBroadcastAddress, "192.168.255.255", 32);
    }
    
    if (!strcmp(dhcpconfig->pubmode, "STATIC") || !strcmp(dhcpconfig->pubmode, "DYNAMIC")) { 
      if (pubInterface) {
	strncpy(dhcpconfig->pubInterface, pubInterface, 32);
      } else {
	strncpy(dhcpconfig->pubInterface, "eth0", 32);
      }
      
      if (pubSubnet) {
	strncpy(dhcpconfig->pubSubnet, pubSubnet, 32);
      } else {
	strncpy(dhcpconfig->pubSubnet, "10.0.0.0", 32);
      }
      
      if (pubSubnetMask) {
	strncpy(dhcpconfig->pubSubnetMask, pubSubnetMask, 32);
      } else {
	strncpy(dhcpconfig->pubSubnetMask, "255.0.0.0", 32);
      }
      
      if (pubBroadcastAddress) {
	strncpy(dhcpconfig->pubBroadcastAddress, pubBroadcastAddress, 32);
      } else {
	strncpy(dhcpconfig->pubBroadcastAddress, "10.255.255.255", 32);
      }
      
      if (pubRouter) {
	strncpy(dhcpconfig->pubRouter, pubRouter, 32);
      } else {
	strncpy(dhcpconfig->pubRouter, "10.1.1.1", 32);
      }
      
      if (pubDNS) {
	strncpy(dhcpconfig->pubDNS, pubDNS, 32);
      } else {
	strncpy(dhcpconfig->pubDNS, "10.1.1.1", 32);
      }
      
      
      if (pubRangeMin) {
	strncpy(dhcpconfig->pubRangeMin, pubRangeMin, 32);
      } else {
	strncpy(dhcpconfig->pubRangeMin, "10.128.1.1", 32);
      }
      
      if (pubRangeMax) {
	strncpy(dhcpconfig->pubRangeMax, pubRangeMax, 32);
      } else {
	strncpy(dhcpconfig->pubRangeMax, "10.128.1.254", 32);
      }
    }
    
    writeDHCPConfig(dhcpconfig);
    dhcpconfig->initialized=1;
  }
}
      
int writeDHCPConfig(dhcpConfig *dhcpconfig) {
  FILE *fp;
  int i;

  fp = fopen(dhcpconfig->config, "w");
  if (fp == NULL) {
    return(1);
  }
  
  fprintf(fp, "# automatically generated config file for DHCP server\ndefault-lease-time 1200;\nmax-lease-time 1200;\nddns-update-style none;\n\n");
  
  fprintf(fp, "subnet %s netmask %s {\n  option subnet-mask %s;\n  option broadcast-address %s;\n}\n", dhcpconfig->privSubnet, dhcpconfig->privSubnetMask, dhcpconfig->privSubnetMask, dhcpconfig->privBroadcastAddress);

  logprintf("public dhcp mode set to %s\n", dhcpconfig->pubmode);
  
  if (!strcmp(dhcpconfig->pubmode, "STATIC") || !strcmp(dhcpconfig->pubmode, "DYNAMIC")) { 
    fprintf(fp, "subnet %s netmask %s {\n  option subnet-mask %s;\n  option broadcast-address %s;\n  option domain-name-servers %s;\n  option routers %s;\n", dhcpconfig->pubSubnet, dhcpconfig->pubSubnetMask, dhcpconfig->pubSubnetMask, dhcpconfig->pubBroadcastAddress, dhcpconfig->pubDNS, dhcpconfig->pubRouter);
    
    if (!strcmp(dhcpconfig->pubmode, "DYNAMIC")) {
      fprintf(fp, "  range %s %s;\n", dhcpconfig->pubRangeMin, dhcpconfig->pubRangeMax);
    }
    fprintf(fp, "}\n");
  }
  
  for (i=0; i<2048; i++) {
    char *mac, *ip;
    
    if (dhcpconfig->macs[i][0] != '\0') {
      mac = dhcpconfig->macs[i];
      ip = dhcpconfig->ips[i];
      fprintf(fp, "\nhost node-%s {\n  hardware ethernet %s;\n  fixed-address %s;\n}\n", ip, mac, ip);
    }
    
  }
  fclose (fp);
  
  return(0);
}

int addDHCPDev(dhcpConfig *dhcpconfig, char *dev) {
  int i, done=0, found;
  
  if (!dhcpconfig || !dev) {
    return(1);
  }
  
  found=0;
  for (i=0; i<128 && !done; i++) {
    if (!found && dhcpconfig->etherdevs[i][0] == '\0') {
      found=i;
    } else if (!strncmp(dhcpconfig->etherdevs[i], dev, 32)) {
      done++;
    }
  }
  
  if (found && !done) {
    strncpy(dhcpconfig->etherdevs[found], dev, 32);
  }
  kickDHCPDaemon(dhcpconfig);
  return(0);
}

int delDHCPDev(dhcpConfig *dhcpconfig, char *dev) {
  int i, done=0;
  
  if (!dhcpconfig || !dev) {
    return(1);
  }
  
  for (i=0; i<128 && !done; i++) {
    if (!strncmp(dhcpconfig->etherdevs[i], dev, 32)) {
      bzero(dhcpconfig->etherdevs[i], 32);
      done++;
    }
  }
  
  kickDHCPDaemon(dhcpconfig);
  return(0);
}

int addDHCPHost(dhcpConfig *dhcpconfig, char *hostmac, char *hostip) {
  int i, done=0;
  
  if (!dhcpconfig || !hostmac || !hostip) {
    return(1);
  }
  
  for (i=0; i<2048 && !done; i++) {
    if (dhcpconfig->macs[i][0] == '\0') {
      strncpy(dhcpconfig->macs[i], hostmac, 32);
      strncpy(dhcpconfig->ips[i], hostip, 32);
      done++;
    }
  }
  kickDHCPDaemon(dhcpconfig);
  //  writeDHCPConfig(dhcpconfig);
  return(0);
}

int delDHCPHost(dhcpConfig *dhcpconfig, char *hostmac, char *hostip) {
  int i, done=0;
  
  for (i=0; i<2048 && !done; i++) {
    if ((!hostmac || !strncmp(dhcpconfig->macs[i], hostmac, 32)) && (!hostip || !strncmp(dhcpconfig->ips[i], hostip, 32))) {
      bzero(dhcpconfig->macs[i], 32);
      bzero(dhcpconfig->ips[i], 32);
      done++;
    }
  }

  kickDHCPDaemon(dhcpconfig);
  //  writeDHCPConfig(dhcpconfig);
  return(0);
}

int kickDHCPDaemon(dhcpConfig *dhcpconfig) {
  char dstring [512] = "";
  char buf [512];
  int rc, i;
  
  if (dhcpconfig->pubInterface) {
    snprintf(dstring, 512, "%s", dhcpconfig->pubInterface);
  }
  
  rc = writeDHCPConfig(dhcpconfig);
  if (rc) {
    logprintf ("failed to (re)create %s\n", dhcpconfig->config);
    return(1);
  }
  
  for (i=0; i<128; i++) {
    if (dhcpconfig->etherdevs[i][0] != '\0') {
      strncat (dstring, " ", 512);
      strncat (dstring, dhcpconfig->etherdevs[i], 32);
    }
  }

  /* force dhcpd to reload the conf */
  snprintf (buf, 512, "rm -f %s", dhcpconfig->trace);
  logprintf ("executing: %s\n", buf);
  system (buf);
  
  snprintf (buf, 512, "kill `cat %s`", dhcpconfig->pidfile);
  logprintf ("executing: %s\n", buf);
  system (buf);
  
  snprintf (buf, 512, "%s -cf %s -lf %s -pf %s -tf %s %s", 
	    dhcpconfig->deamon, dhcpconfig->config, dhcpconfig->leases, dhcpconfig->pidfile, dhcpconfig->trace, dstring);
  logprintf ("executing: %s\n", buf);
  rc = system (buf);
  logprintf("\tRC from cmd: %d\n", rc);

  return(0);
}
