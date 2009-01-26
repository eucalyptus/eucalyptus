#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pthread.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdarg.h>

#include <sys/ioctl.h>
#include <net/if.h>  

#include <vnetwork.h>
#include <misc.h>

void vnetInit(vnetConfig *vnetconfig, char *mode, char *eucahome, char *path, int role, char *pubInterface, char *numberofaddrs, char *network, char *netmask, char *broadcast, char *nameserver, char *router, char *daemon, char *dhcpuser, char *bridgedev) {
  uint32_t nw=0, nm=0, unw=0, unm=0, dns=0, bc=0, rt=0, rc=0, slashnet=0;
  int vlan=0, numaddrs=1;
  char cmd[256];
  
  if (param_check("vnetInit", vnetconfig, mode, eucahome, path, role, pubInterface, numberofaddrs, network, netmask, broadcast, nameserver, router, daemon, bridgedev)) return;
  
  if (!vnetconfig->initialized) {
    bzero(vnetconfig, sizeof(vnetConfig));
    if (path) strncpy(vnetconfig->path, path, 1024);
    if (eucahome) strncpy(vnetconfig->eucahome, eucahome, 1024);
    if (pubInterface) strncpy(vnetconfig->pubInterface, pubInterface, 32);
    if (mode) strncpy(vnetconfig->mode, mode, 32);
    if (bridgedev) strncpy(vnetconfig->bridgedev, bridgedev, 32);
    if (daemon) strncpy(vnetconfig->dhcpdaemon, daemon, 1024);
    if (dhcpuser) strncpy(vnetconfig->dhcpuser, dhcpuser, 32);
    vnetconfig->role = role;
    vnetconfig->enabled=1;
    vnetconfig->initialized = 1;
    if (numberofaddrs) vnetconfig->numaddrs = atoi(numberofaddrs);

    // populate networks
    bzero(vnetconfig->users, sizeof(userEntry) * NUMBER_OF_VLANS);
    bzero(vnetconfig->networks, sizeof(networkEntry) * NUMBER_OF_VLANS);
    bzero(vnetconfig->etherdevs, NUMBER_OF_VLANS * 32);

    bzero(vnetconfig->publicips, sizeof(publicip) * NUMBER_OF_PUBLIC_IPS);
    
    if (role != NC) {
      if (network) nw = dot2hex(network);
      if (netmask) nm = dot2hex(netmask);
      if (nameserver) dns = dot2hex(nameserver);
      if (broadcast) bc = dot2hex(broadcast);
      if (router) rt = dot2hex(router);
      if (numberofaddrs) numaddrs = atoi(numberofaddrs);
      
      numaddrs-=1;
      if (!strcmp(mode, "MANAGED") || !strcmp(mode, "MANAGED-NOVLAN")) {
	// set up iptables
	logprintfl(EUCADEBUG, "flushing 'filter' table\n");
	rc = vnetApplySingleTableRule(vnetconfig, "filter", "-F");
	
	logprintfl(EUCADEBUG, "flushing 'nat' table\n");
	rc = vnetApplySingleTableRule(vnetconfig, "nat", "-F");
	
	if (path) {
	  vnetLoadIPTables(vnetconfig);
	}
	
	rc = vnetApplySingleTableRule(vnetconfig, "filter", "-P FORWARD DROP");
	
	rc = vnetApplySingleTableRule(vnetconfig, "filter", "-A FORWARD -m conntrack --ctstate ESTABLISHED -j ACCEPT");
	
	slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - nm)) + 1);
	snprintf(cmd, 256, "-A FORWARD -d ! %s/%d -j ACCEPT", network, slashnet);
	rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);

	snprintf(cmd, 256, "-A POSTROUTING -d ! %s/%d -j MASQUERADE", network, slashnet);
	rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);

	snprintf(cmd, 256, "%s/usr/share/eucalyptus/euca_rootwrap ip addr add 169.254.169.254 dev %s", vnetconfig->eucahome, vnetconfig->pubInterface);
	rc = system(cmd);
	
	snprintf(cmd, 256, "-A PREROUTING -s %s/%d -d 169.254.169.254 -p tcp --dport 80 -j DNAT --to 169.254.169.254:8773", network, slashnet);
	rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);

	unm = 0xFFFFFFFF - numaddrs;
	unw = nw;
	for (vlan=2; vlan<NUMBER_OF_VLANS; vlan++) {
	  vnetconfig->networks[vlan].nw = unw;
	  vnetconfig->networks[vlan].nm = unm;
	  vnetconfig->networks[vlan].bc = unw + numaddrs;
	  vnetconfig->networks[vlan].dns = dns;
	  vnetconfig->networks[vlan].router = unw+1;
	  unw += numaddrs + 1;	  
	}	
      } else if (!strcmp(mode, "STATIC")) {
	for (vlan=0; vlan<NUMBER_OF_VLANS; vlan++) {
	  vnetconfig->networks[vlan].nw = nw;
	  vnetconfig->networks[vlan].nm = nm;
	  vnetconfig->networks[vlan].bc = bc;
	  vnetconfig->networks[vlan].dns = dns;
	  vnetconfig->networks[vlan].router = rt;
	  vnetconfig->numaddrs = 0xFFFFFFFF - nm;
	}
      }
    }
  }
}

int vnetAddHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan) {
  int i, done, found;
  char *newip;

  if (param_check("vnetAddHost", vnetconfig, mac, ip, vlan)) return(1);

  if (!vnetconfig->enabled) {
    logprintfl(EUCADEBUG,"network support is not enabled\n");
    return(1);
  }
  
  done=found=0;
  //  for (i=2; i<NUMBER_OF_HOSTS_PER_VLAN && !done; i++) {
  logprintfl(EUCAINFO,"NUMADDRS %d %d\n", vnetconfig->numaddrs, 2);
  for (i=2; i<vnetconfig->numaddrs-2 && !done; i++) {
    if (vnetconfig->networks[vlan].addrs[i].mac[0] == '\0') {
      if (!found) found=i;
    } else if (!strcmp(mac, vnetconfig->networks[vlan].addrs[i].mac)) {
      done++;
    }
  }
  
  if (done) {
    // duplicate IP fond
    logprintfl(EUCAWARN,"attempting to add duplicate macmap entry, ignoring\n");
  } else if (found) {
    strncpy(vnetconfig->networks[vlan].addrs[found].mac, mac, 24);
    if (ip) {
      vnetconfig->networks[vlan].addrs[found].ip = dot2hex(ip);
    } else {
      newip = hex2dot(vnetconfig->networks[vlan].nw + found);
      vnetconfig->networks[vlan].addrs[found].ip = dot2hex(newip);
      if (newip) free(newip);
    }
    vnetconfig->networks[vlan].numhosts++;
  } else {
    logprintfl(EUCAERROR,"failed to add host %s on vlan %d\n", mac, vlan);
    return(1);
  }
  return(0);
}

int vnetDelHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan) {
  int i, done;
  
  if (param_check("vnetDelHost", vnetconfig, mac, ip, vlan)) return(1);

  if (!vnetconfig->enabled) {
    logprintfl(EUCADEBUG,"network support is not enabled\n");
    return(1);
  }
  
  done=0;
  //  for (i=2; i<NUMBER_OF_HOSTS_PER_VLAN && !done; i++) {
  for (i=2; i<vnetconfig->numaddrs-2 && !done; i++) {
    if ( (!mac || !strcmp(vnetconfig->networks[vlan].addrs[i].mac, mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)) ) ) {
      bzero(&(vnetconfig->networks[vlan].addrs[i]), sizeof(netEntry));
      vnetconfig->networks[vlan].numhosts--;
      done++;
    }
  }
  
  if (!done) {
    return(1);
  }
  return(0);
}
int vnetEnableHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan) {
  int i, done;
  
  if (param_check("vnetEnableHost", vnetconfig, mac, ip, vlan)) return(1);

  if (!vnetconfig->enabled) {
    logprintfl(EUCADEBUG,"network support is not enabled\n");
    return(1);
  }
  
  done=0;
  //  for (i=2; i<NUMBER_OF_HOSTS_PER_VLAN && !done; i++) {
  for (i=2; i<vnetconfig->numaddrs-2 && !done; i++) {
    if ( (!mac || !strcmp(vnetconfig->networks[vlan].addrs[i].mac, mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)) ) ) {
      vnetconfig->networks[vlan].addrs[i].active=1;
      done++;
    }
  }
  if (!done) {
    return(1);
  }
  return(0);
}

int vnetDisableHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan) {
  int i, done;
  
  if (!vnetconfig->enabled) {
    logprintfl(EUCADEBUG,"network support is not enabled\n");
    return(1);
  }

  done=0;
  //  for (i=2; i<NUMBER_OF_HOSTS_PER_VLAN && !done; i++) {
  for (i=2; i<vnetconfig->numaddrs-2 && !done; i++) {
    if ( (!mac || !strcmp(vnetconfig->networks[vlan].addrs[i].mac, mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)) ) ) {
      vnetconfig->networks[vlan].addrs[i].active=0;
      done++;
    }
  }
  if (!done) {
    return(1);
  }
  return(0);
}

int vnetDeleteChain(vnetConfig *vnetconfig, char *userName, char *netName) {
  char cmd[256];
  int rc, runcount;
  
  if (param_check("vnetDeleteChain", vnetconfig, userName, netName)) return(1);
  
  rc = check_chain(vnetconfig, userName, netName);
  logprintfl(EUCADEBUG, "DeleteChainParams: %s %s %d\n", userName, netName, rc);
  if (!rc) {
    snprintf(cmd, 256, "-D FORWARD -j %s-%s", userName, netName);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    if (rc) {
      logprintfl(EUCAERROR, "'%s' failed; cannot remove link to chain %s-%s\n", cmd, userName, netName);
    }
    runcount=0;
    while(!rc && runcount < 10) {
      rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
      //      rc = system(cmd);
      runcount++;
    }
  
    snprintf(cmd, 256, "-F %s-%s", userName, netName);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    if (rc) {
      logprintfl(EUCAERROR, "'%s' failed; cannot flush chain %s-%s\n", cmd, userName, netName);
    }

    snprintf(cmd, 256, "-X %s-%s", userName, netName);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    if (rc) {
      logprintfl(EUCAERROR, "'%s' failed; cannot remove chain %s-%s\n", cmd, userName, netName);
    }
    runcount=0;
    while(!rc && runcount < 10) {
      rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
      runcount++;
    }
  }
  
  return(0);
}

int vnetCreateChain(vnetConfig *vnetconfig, char *userName, char *netName) {
  char cmd[256];
  int rc, ret, count;
  
  if (param_check("vnetCreateChain", vnetconfig, userName, netName)) return(1);

  ret = 0;
  rc = check_chain(vnetconfig, userName, netName);
  if (rc) {
    snprintf(cmd, 256, "-N %s-%s", userName, netName);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    if (rc) {
      logprintfl(EUCAERROR, "'%s' failed; cannot create chain %s-%s\n", cmd, userName, netName);
      ret=1;
    }
  }    
  if (!ret) {
    snprintf(cmd, 256, "-D FORWARD -j %s-%s", userName, netName);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    count=0;
    while(!rc && count < 100) {
      rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
      count++;
    }


    snprintf(cmd, 256, "-A FORWARD -j %s-%s", userName, netName);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    if (rc) {
      logprintfl(EUCAERROR, "'%s' failed; cannot link to chain %s-%s\n", cmd, userName, netName);
      ret=1;
    }
  }
  return(ret);
}

int vnetFlushTable(vnetConfig *vnetconfig, char *userName, char *netName) {
  char cmd[256];
  int rc;
  if ((userName && netName) && !check_chain(vnetconfig, userName, netName)) {
    snprintf(cmd, 256, "-F %s-%s", userName, netName);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    return(rc);
  }
  return(1);
}

int vnetApplySingleTableRule(vnetConfig *vnetconfig, char *table, char *rule) {
  int rc, fd;
  char *file, cmd[256];
  FILE *FH;

  if (!rule) {
    return(1);
  }

  file = strdup("/tmp/euca-ipt-XXXXXX");
  if (!file) {
    return(1);
  }
  fd = mkstemp(file);
  if (fd < 0) {
    free(file);
    return(1);
  }
  chmod(file, 0644);
  FH = fdopen(fd, "w");
  if (!FH) {
    free(file);
    unlink(file);
    return(1);
  }

  fprintf(FH, "%s\n", rule);
  fclose(FH);
  close(fd);
  
  snprintf(cmd, 256, "%s/usr/share/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/euca_ipt %s %s", vnetconfig->eucahome, vnetconfig->eucahome, table, file);
  logprintfl(EUCADEBUG, "running cmd '%s'\n", cmd);
  rc = system(cmd);
  
  unlink(file);
  free(file);
    
  return(rc);
}
									 

int vnetTableRule(vnetConfig *vnetconfig, char *type, char *destUserName, char *destName, char *sourceUserName, char *sourceNet, char *sourceNetName, char *protocol, int minPort, int maxPort) {
  int i, rc, done, destVlan, srcVlan, slashnet;
  char rule[1024], newrule[1024], srcNet[32], dstNet[32];
  char *tmp;

  logprintfl(EUCADEBUG, "vnetTableRule(): input: %s,%s,%s,%s,%s,%s,%d,%d\n",destUserName, destName, sourceUserName, sourceNet,sourceNetName,protocol,minPort,maxPort);
  if (param_check("vnetTableRule", vnetconfig, type, destUserName, destName, sourceNet, sourceUserName, sourceNetName)) return(1);
  
  destVlan = vnetGetVlan(vnetconfig, destUserName, destName);
  if (destVlan < 0) {
    logprintfl(EUCAERROR,"no vlans associated with network %s/%s\n", destUserName, destName);
    return(1);
  }
  
  slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - vnetconfig->networks[destVlan].nm)+1))));
  tmp = hex2dot(vnetconfig->networks[destVlan].nw);
  snprintf(dstNet, 32, "%s/%d", tmp, slashnet);
  free(tmp);
  
  //  printf("HMM: %d %d %s %s %d\n", destVlan, slashnet, dstNet, hex2dot(vnetconfig->networks[destVlan].nm), (0xFFFFFFFF - vnetconfig->networks[destVlan].nm));
  
  if (sourceNetName) {
    srcVlan = vnetGetVlan(vnetconfig, sourceUserName, sourceNetName);
    if (srcVlan < 0) {
      logprintfl(EUCAERROR,"cannot locate source vlan for network %s/%s\n", sourceUserName, sourceNetName);
      return(1);
    }
    tmp = hex2dot(vnetconfig->networks[srcVlan].nw);
    snprintf(srcNet, 32, "%s/%d", tmp, slashnet);
    free(tmp);
  } else {
    snprintf(srcNet, 32, "%s", sourceNet);
  }
  

  if (!strcmp(type, "firewall-open")) {
    snprintf(rule, 1024, "-A %s-%s", destUserName, destName);
    //    snprintf(rule, 1024, "iptables -A %s-%s", destUserName, destName);
  } else if (!strcmp(type, "firewall-close")) {
    snprintf(rule, 1024, "-D %s-%s", destUserName, destName);
    //    snprintf(rule, 1024, "iptables -D %s-%s", destUserName, destName);
  }
  
  snprintf(newrule, 1024, "%s -s %s -d %s", rule, srcNet, dstNet);
  strcpy(rule, newrule);
  
  if (protocol) {
    snprintf(newrule, 1024, "%s -p %s", rule, protocol);
    strcpy(rule, newrule);
  }
  
  if (minPort && maxPort) {
    if (protocol && (!strcmp(protocol, "tcp") || !strcmp(protocol, "udp")) ) {
      snprintf(newrule, 1024, "%s --dport %d:%d", rule, minPort, maxPort);
      strcpy(rule, newrule);
    }
  }
  
  snprintf(newrule, 1024, "%s -j ACCEPT", rule);
  strcpy(rule, newrule);
  
  if (!strcmp(type, "firewall-close")) {
    // this means that the network should already be flushed and empty (default policy == drop)
  } else {
    logprintfl(EUCAINFO,"applying iptables rule: %s\n", rule);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", rule);
    //  rc = system(rule);
    if (rc) {
      logprintfl(EUCAERROR,"iptables rule application failed: %d\n", rc);
      return(1);
    }
  }
  return(0);
}


int vnetSetVlan(vnetConfig *vnetconfig, int vlan, char *user, char *network) {
  
  if (param_check("vnetSetVlan", vnetconfig, vlan, user, network)) return(1);

  strncpy(vnetconfig->users[vlan].userName, user, 32);
  strncpy(vnetconfig->users[vlan].netName, network, 32);
  
  return(0);
}

int vnetGetVlan(vnetConfig *vnetconfig, char *user, char *network) {
  int i, done;
  
  done=0;
  for (i=0; i<NUMBER_OF_VLANS; i++) {
    if (!strcmp(vnetconfig->users[i].userName, user) && !strcmp(vnetconfig->users[i].netName, network)) {
      return(i);
    }
  }
  return(-1);
}


int vnetGetNextHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan) {
  int i, done;
  char *newip;
  
  if (param_check("vnetGetNextHost", vnetconfig, mac, ip, vlan)) return(1);

  if (!vnetconfig->enabled) {
    logprintfl(EUCADEBUG,"network support is not enabled\n");
    return(1);
  }
  
  done=0;
  //  for (i=2; i<NUMBER_OF_HOSTS_PER_VLAN && !done; i++) {
  for (i=2; i<vnetconfig->numaddrs-2 && !done; i++) {
    if (vnetconfig->networks[vlan].addrs[i].mac[0] != '\0' && vnetconfig->networks[vlan].addrs[i].ip != 0 && vnetconfig->networks[vlan].addrs[i].active == 0) {
      strncpy(mac, vnetconfig->networks[vlan].addrs[i].mac, 24);
      newip = hex2dot(vnetconfig->networks[vlan].addrs[i].ip);
      strncpy(ip, newip, 16);
      free(newip);
      vnetconfig->networks[vlan].addrs[i].active = 1;
      done++;
    }
  }
  if (!done) {
    return(1);
  }
  return(0);
}

int vnetAddDev(vnetConfig *vnetconfig, char *dev) {
  int i, done, foundone;

  if (param_check("vnetAddDev", vnetconfig, dev)) return(1);

  done=0;
  foundone = -1;
  for (i=0; i<NUMBER_OF_VLANS && !done; i++) {
    if (!strcmp(vnetconfig->etherdevs[i], dev)) {
      return(1);
    }
    if (vnetconfig->etherdevs[i][0] == '\0') {
      foundone = i;
    }
  }
  if (foundone >= 0) {
    strncpy(vnetconfig->etherdevs[foundone], dev, 32);
  }
  return(0);
}

int vnetDelDev(vnetConfig *vnetconfig, char *dev) {
  int i, done;

  if (param_check("vnetDelDev", vnetconfig, dev)) return(1);

  done=0;
  for (i=0; i<NUMBER_OF_VLANS && !done; i++) {
    if (!strncmp(vnetconfig->etherdevs[i], dev, 32)) {
      bzero(vnetconfig->etherdevs[i], 32);
      done++;
    }
  }
  return(0);
}

int vnetGenerateDHCP(vnetConfig *vnetconfig) {
  FILE *fp;
  char fname[1024],
    *network, *netmask,
    *broadcast, *nameserver,
    *router, *mac, *newip;
  int i,j;

  if (param_check("vnetGenerateDHCP", vnetconfig)) return(1);

  snprintf(fname, 1024, "%s/euca-dhcp.conf", vnetconfig->path);

  fp = fopen(fname, "w");
  if (fp == NULL) {
    return(1);
  }
  
  fprintf(fp, "# automatically generated config file for DHCP server\ndefault-lease-time 1200;\nmax-lease-time 1200;\nddns-update-style none;\n\n");
  
  fprintf(fp, "shared-network euca {\n");
  for (i=0; i<NUMBER_OF_VLANS; i++) {
    if (vnetconfig->networks[i].numhosts > 0) {
      network = hex2dot(vnetconfig->networks[i].nw);
      netmask = hex2dot(vnetconfig->networks[i].nm);
      broadcast = hex2dot(vnetconfig->networks[i].bc);
      nameserver = hex2dot(vnetconfig->networks[i].dns);
      router = hex2dot(vnetconfig->networks[i].router);
			
      fprintf(fp, "subnet %s netmask %s {\n  option subnet-mask %s;\n  option broadcast-address %s;\n  option domain-name-servers %s;\n  option routers %s;\n}\n", network, netmask, netmask, broadcast, nameserver, router);

      //      for (j=2; j<NUMBER_OF_HOSTS_PER_VLAN; j++) {
      for (j=2; j<vnetconfig->numaddrs-2; j++) {
	if (vnetconfig->networks[i].addrs[j].active == 1) {

	  newip = hex2dot(vnetconfig->networks[i].addrs[j].ip);
	  printf("%s ACTIVE\n", newip);
	  mac = vnetconfig->networks[i].addrs[j].mac;
	  fprintf(fp, "\nhost node-%s {\n  hardware ethernet %s;\n  fixed-address %s;\n}\n", newip, mac, newip);
	  if (newip) free(newip);

	}
      }
    }
  }
  fprintf(fp, "}\n");
  fclose (fp);
  
  return(0);
}

int vnetKickDHCP(vnetConfig *vnetconfig) {

  char dstring [512] = "";
  char buf [512];
  int rc, i;
  
  if (param_check("vnetKickDHCP", vnetconfig)) return(1);

  if (!strcmp(vnetconfig->mode, "SYSTEM")) {
    return(0);
  }

  rc = vnetGenerateDHCP(vnetconfig);
  if (rc) {
    logprintfl(EUCAERROR, "failed to (re)create DHCP config (%s/euca-dhcp.conf)\n", vnetconfig->path);
    return(1);
  }
  
  for (i=0; i<NUMBER_OF_VLANS; i++) {
    if (vnetconfig->etherdevs[i][0] != '\0') {
      strncat (dstring, " ", 512);
      strncat (dstring, vnetconfig->etherdevs[i], 32);
    }
  }
  
  /* force dhcpd to reload the conf */
  snprintf (buf, 512, "%s/usr/share/eucalyptus/euca_rootwrap kill `cat %s/euca-dhcp.pid`", vnetconfig->eucahome, vnetconfig->path);
  logprintfl(EUCADEBUG, "executing: %s\n", buf);
  rc = system (buf);
  
  snprintf (buf, 512, "%s/usr/share/eucalyptus/euca_rootwrap kill -9 `cat %s/euca-dhcp.pid`", vnetconfig->eucahome, vnetconfig->path);
  logprintfl(EUCADEBUG, "executing: %s\n", buf);
  rc = system (buf);
  usleep(250000);
  
  snprintf (buf, 512, "%s/euca-dhcp.trace", vnetconfig->path);
  unlink(buf);
  //  snprintf (buf, 512, "rm -f %s/euca-dhcp.trace", vnetconfig->path);
  //  logprintfl(EUCADEBUG, "executing: %s\n", buf);
  //  rc = system (buf);
  
  snprintf (buf, 512, "%s/euca-dhcp.leases", vnetconfig->path);
  rc = open(buf, O_WRONLY | O_CREAT, 0644);
  close(rc);
  //  snprintf (buf, 512, "touch %s/euca-dhcp.leases", vnetconfig->path);
  //  logprintfl(EUCADEBUG, "executing: %s\n", buf);
  //  rc = system (buf);
  
  if (strncmp(vnetconfig->dhcpuser, "root", 32) && vnetconfig->path && strncmp(vnetconfig->path, "/", 1024) && strstr(vnetconfig->path, "eucalyptus/net")) {
    snprintf(buf, 512, "%s/usr/share/eucalyptus/euca_rootwrap chgrp -R %s %s", vnetconfig->eucahome, vnetconfig->dhcpuser, vnetconfig->path);
    logprintfl(EUCADEBUG, "executing: %s\n", buf);
    rc = system(buf);
    
    snprintf(buf, 512, "%s/usr/share/eucalyptus/euca_rootwrap chmod -R 0775 %s", vnetconfig->eucahome, vnetconfig->path);
    logprintfl(EUCADEBUG, "executing: %s\n", buf);
    rc = system(buf);
  }
  
  snprintf (buf, 512, "%s/usr/share/eucalyptus/euca_rootwrap %s -cf %s/euca-dhcp.conf -lf %s/euca-dhcp.leases -pf %s/euca-dhcp.pid -tf %s/euca-dhcp.trace %s", vnetconfig->eucahome, vnetconfig->dhcpdaemon, vnetconfig->path, vnetconfig->path, vnetconfig->path, vnetconfig->path, dstring);
  
  logprintfl(EUCAINFO, "executing: %s\n", buf);
  rc = system (buf);
  logprintfl(EUCAINFO, "\tRC from cmd: %d\n", rc);
  
  return(rc);
  
}

int check_chain(vnetConfig *vnetconfig, char *userName, char *netName) {
  char cmd[256];
  int rc;
  snprintf(cmd, 256, "-L %s-%s", userName, netName);
  rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
  return(rc);
}

int check_device(char *dev) {
  char rbuf[256], devbuf[256], *ptr;
  FILE *FH=NULL;
  
  
  if (!dev) return(1);
  
  FH = fopen("/proc/net/dev", "r");
  if (!FH) {
    return(1);
  }
  
  while(fgets(rbuf, 256, FH)) {
    ptr = strrchr(rbuf, ':');
    if (ptr) {
      *ptr = '\0';
      ptr = strrchr(rbuf, ' ');
      if (ptr) {
        ptr = ptr + 1;
      } else {
        ptr = rbuf;
      }
      if (!strcmp(ptr, dev)) {
	// found it
	fclose(FH);
	return(0);
      }
    }
  }
  fclose(FH);

  return(1);
}

int check_bridge(char *brname) {
  return(check_device(brname));
}

int vnetStartNetworkManaged(vnetConfig *vnetconfig, int vlan, char *userName, char *netName, char **outbrname) {
  char cmd[1024], newdevname[32], newbrname[32];
  int rc;

  // check input params...
  if (!vnetconfig || !outbrname) {
    if (!vnetconfig) {
      logprintfl(EUCAERROR, "bad input params to vnetStartNetworkManaged()\n");
      return(1);
    } else {
      return(0);
    }
  }

  *outbrname = NULL;
  
  if (vnetconfig->role == NC && vlan > 0) {

    // first, create tagged interface
    if (!strcmp(vnetconfig->mode, "MANAGED")) {
      snprintf(newdevname, 32, "%s.%d", vnetconfig->pubInterface, vlan);
      rc = check_device(newdevname);
      if (rc) {
	snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap vconfig add %s %d", vnetconfig->eucahome, vnetconfig->pubInterface, vlan);
	rc = system(cmd);
	if (rc != 0) {
	  // failed to create vlan tagged device
	  logprintfl(EUCAERROR, "cannot create new vlan device %s.%d\n", vnetconfig->pubInterface, vlan);
	  return(1);
	}
      }

      // create new bridge
      snprintf(newbrname, 32, "eucabr%d", vlan);
      *outbrname = strdup(newbrname);
      rc = check_bridge(newbrname);
      if (rc) {
	// bridge does not yet exist
	snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap brctl addbr %s", vnetconfig->eucahome, newbrname);
	rc = system(cmd);
	if (rc) {
	  logprintfl(EUCAERROR, "could not create new bridge %s\n", newbrname);
	  return(1);
	}      
      }
      
      // add if to bridge
      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap brctl addif %s %s", vnetconfig->eucahome, newbrname, newdevname);
      rc = system(cmd);
      
      // bring br up
      //      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip addr add 0.0.0.0 dev %s", vnetconfig->eucahome, newbrname);
      //      rc = system(cmd);
      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip link set dev %s up", vnetconfig->eucahome, newbrname);
      rc = system(cmd);
      
      // bring if up
      //      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip addr add 0.0.0.0 dev %s", vnetconfig->eucahome, newdevname);
      //      rc = system(cmd);
      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip link set dev %s up", vnetconfig->eucahome, newdevname);
      rc = system(cmd);

      /*
      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ifconfig %s 0.0.0.0 up", vnetconfig->eucahome, newbrname);
      rc = system(cmd);
      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ifconfig %s up", vnetconfig->eucahome, newdevname);
      rc = system(cmd);
      */
      
    } else {
      snprintf(newbrname, 32, "%s", vnetconfig->bridgedev);
    }
    
    *outbrname = strdup(newbrname);
  } else if (vlan > 0 && (vnetconfig->role == CC || vnetconfig->role == CLC)) {
    //    char *newip, *netmask;

    rc = vnetSetVlan(vnetconfig, vlan, userName, netName);
    rc = vnetCreateChain(vnetconfig, userName, netName);
    
    if (!strcmp(vnetconfig->mode, "MANAGED")) {
      snprintf(newdevname, 32, "%s.%d", vnetconfig->pubInterface, vlan);
      rc = check_device(newdevname);
      if (rc) {
	snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap vconfig add %s %d", vnetconfig->eucahome, vnetconfig->pubInterface, vlan);
	rc = system(cmd);
	if (rc) {
	  logprintfl(EUCAERROR, "could not tag %s with vlan %d\n", vnetconfig->pubInterface, vlan);
	  return(1);
	}
      }

      // create new bridge                                                                                                                                     
      snprintf(newbrname, 32, "eucabr%d", vlan);
      rc = check_bridge(newbrname);
      if (rc) {
        // bridge does not yet exist
        snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap brctl addbr %s", vnetconfig->eucahome, newbrname);
        rc = system(cmd);
        if (rc) {
          logprintfl(EUCAERROR, "could not create new bridge %s\n", newbrname);
          return(1);
        }
      }

      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap brctl addif %s %s", vnetconfig->eucahome, newbrname, newdevname);
      rc = system(cmd);
      
      // bring br up
      //      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip addr add 0.0.0.0 dev %s", vnetconfig->eucahome, newbrname);
      //      rc = system(cmd);
      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip link set dev %s up", vnetconfig->eucahome, newbrname);
      rc = system(cmd);
      
      // bring if up
      //      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip addr add 0.0.0.0 dev %s", vnetconfig->eucahome, newdevname);
      //      rc = system(cmd);
      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip link set dev %s up", vnetconfig->eucahome, newdevname);
      rc = system(cmd);

      snprintf(newdevname, 32, "%s", newbrname);
    } else {
      snprintf(newdevname, 32, "%s", vnetconfig->pubInterface);
    }
    
    rc = vnetAddGatewayIP(vnetconfig, vlan, newdevname);
    if (rc) {
      return(rc);
    }
    /*
      newip = hex2dot(vnetconfig->networks[vlan].router);
      netmask = hex2dot(vnetconfig->networks[vlan].nm);
      
      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ifconfig %s %s netmask %s up", vnetconfig->eucahome, newdevname, newip, netmask);
      rc = system(cmd);
      if (rc) {
      logprintfl(EUCAERROR, "could not bring up new device %s with ip %s\n", newdevname, newip);
      if (newip) free(newip);
      if (netmask) free(netmask);
      return(1);
      }
      if (newip) free(newip);
      if (netmask) free(netmask);
    */
    
    *outbrname = strdup(newdevname);
  }
  return(0);
}

int vnetAddGatewayIP(vnetConfig *vnetconfig, int vlan, char *devname) {
  char *newip, *broadcast;
  int rc, slashnet;
  char cmd[1024];

  newip = hex2dot(vnetconfig->networks[vlan].router);
  broadcast = hex2dot(vnetconfig->networks[vlan].bc);
  
  //  snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ifconfig %s %s netmask %s up", vnetconfig->eucahome, devname, newip, netmask);
  slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
  snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip addr add %s/%d broadcast %s dev %s", vnetconfig->eucahome, newip, slashnet, broadcast, devname);
  //  snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip addr add %s/%d dev %s", vnetconfig->eucahome, newip, slashnet, devname);
  logprintfl(EUCADEBUG, "running cmd '%s'\n", cmd);
  rc = system(cmd);
  if (rc) {
    logprintfl(EUCAERROR, "could not bring up new device %s with ip %s\n", devname, newip);
    if (newip) free(newip);
    if (broadcast) free(broadcast);
    return(1);
  }
  if (newip) free(newip);
  if (broadcast) free(broadcast);

  snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip link set dev %s up", vnetconfig->eucahome, devname);
  rc = system(cmd);
  if (rc) {
    logprintfl(EUCAERROR, "could not bring up interface '%s'\n", devname);
    return(1);
  }
  return(0);
}

int vnetDelGatewayIP(vnetConfig *vnetconfig, int vlan, char *devname) {
  char *newip, *broadcast;
  int rc;
  int slashnet;
  char cmd[1024];
  
  newip = hex2dot(vnetconfig->networks[vlan].router);
  broadcast = hex2dot(vnetconfig->networks[vlan].bc);
  
  //  snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ifconfig %s %s netmask %s up", vnetconfig->eucahome, devname, newip, netmask);
  slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
  //slashnet = 16;
  snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip addr del %s/%d broadcast %s dev %s", vnetconfig->eucahome, newip, slashnet, broadcast, devname);
  //  snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip addr del %s/%d dev %s", vnetconfig->eucahome, newip, slashnet, devname);
  if (newip) free(newip);
  if (broadcast) free(broadcast);
  
  rc = system(cmd);
  if (rc) {
    logprintfl(EUCAERROR, "could not bring up new device %s with ip %s\n", devname, newip);
    return(1);
  }
  return(0);
}

int vnetStopNetworkManaged(vnetConfig *vnetconfig, int vlan, char *userName, char *netName) {
  char cmd[1024], newdevname[32], newbrname[32];
  int rc, ret;
  
  ret = 0;
  //if (vnetconfig->role == NC) {
    
  if (!strcmp(vnetconfig->mode, "MANAGED")) {
    snprintf(newbrname, 32, "eucabr%d", vlan);
    snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip link set dev %s down", vnetconfig->eucahome, newbrname);
    rc = system(cmd);
    if (rc) {
      logprintfl(EUCAERROR, "cmd '%s' failed\n", cmd);
      ret = 1;
    }
  }

    //  }
  
  if (!strcmp(vnetconfig->mode, "MANAGED")) {
    snprintf(newdevname, 32, "%s.%d", vnetconfig->pubInterface, vlan);
    rc = check_device(newdevname);
    if (!rc) {
      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap ip link set dev %s down", vnetconfig->eucahome, newdevname);
      rc = system(cmd);
      if (rc) {
	logprintfl(EUCAERROR, "cmd '%s' failed\n", cmd);
	//    return(1);
	ret=1;
      }
  
      snprintf(cmd, 1024, "%s/usr/share/eucalyptus/euca_rootwrap vconfig rem %s", vnetconfig->eucahome, newdevname);
      rc = system(cmd);
      if (rc) {
	logprintfl(EUCAERROR, "cmd '%s' failed\n", cmd);
	ret = 1;
      }
    }
    snprintf(newdevname, 32, "%s", newbrname);
  } else {
    snprintf(newdevname, 32, "%s", vnetconfig->pubInterface);
  }
  
  if ((vnetconfig->role == CC || vnetconfig->role == CLC)) {
    
    if (!strcmp(vnetconfig->mode, "MANAGED")) {
      rc = vnetDelDev(vnetconfig, newdevname);
      if (rc) {
	logprintfl(EUCAERROR, "could not remove '%s' from list of interfaces\n", newdevname);
      }
    } else {
      rc = vnetDelGatewayIP(vnetconfig, vlan, newdevname);
    }
    
    if (userName && netName) {
      rc = vnetDeleteChain(vnetconfig, userName, netName);
      if (rc) {
	logprintfl(EUCAERROR, "could not delete chain (%s/%s)\n", userName, netName);
	ret = 1;
      }
    }
  }
  
  return(ret);
}

int vnetStartNetwork(vnetConfig *vnetconfig, int vlan, char *userName, char *netName, char **outbrname) {
  int rc;

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC")) {
    if (outbrname) {
      if (vnetconfig->role == NC) {
	*outbrname = strdup(vnetconfig->bridgedev);
      } else {
	*outbrname = strdup(vnetconfig->pubInterface);
      }
    }
    rc = 0;
  } else {
    rc = vnetStartNetworkManaged(vnetconfig, vlan, userName, netName, outbrname);
  }
  
  if (vnetconfig->role != NC && *outbrname) {
    vnetAddDev(vnetconfig, *outbrname);
  }
  return(rc);
}

int vnetGetPublicIP(vnetConfig *vnetconfig, char *ip, char **dstip, int *allocated, int *addrdevno) {
  int i, done, rc;

  if (param_check("vnetGetPublicIP", vnetconfig, ip, allocated, addrdevno)) return(1);
  
  *allocated = *addrdevno = 0;
  done=0;
  for (i=1; i<NUMBER_OF_PUBLIC_IPS && !done; i++) {
    if (vnetconfig->publicips[i].ip == dot2hex(ip)) {
      if (dstip != NULL) {
	*dstip = hex2dot(vnetconfig->publicips[i].dstip);
      }
      *allocated = vnetconfig->publicips[i].allocated;
      *addrdevno = i;
      done++;
    }
  }

  if (!done) {
    logprintfl(EUCAERROR, "could not find ip %s in list of allocateable publicips\n", ip);
    return(1);
  }
  return(0);
}
int vnetAddPublicIP(vnetConfig *vnetconfig, char *inip) {
  int i, rc, done, slashnet, numips, j, found;
  uint32_t minip, theip;
  char tmp[32], *ip, *ptr;

  if (param_check("vnetAddPublicIP", vnetconfig, inip)) return(1);

  if (inip[0] == '!') {
    // remove mode
    ip = inip+1;  

    theip = dot2hex(ip);
    done=0;
    for (i=1; i<NUMBER_OF_PUBLIC_IPS && !done; i++) {
      if (vnetconfig->publicips[i].ip == theip) {
	vnetconfig->publicips[i].ip = 0;
	done++;
      }
    }
  } else {
    // add mode
    ip = inip;
    slashnet = 0;
    if ((ptr = strchr(ip, '/'))) {
      *ptr = '\0';
      ptr++;
      theip = dot2hex(ip);
      slashnet = atoi(ptr);
      minip = theip+1;
      numips = pow(2.0, (double)(32 - slashnet)) - 2;
  } else {
      minip = dot2hex(ip);
      numips = 1;
    }
    
    for (j=0; j<numips; j++) {
      theip = minip + j;
      done=found=0;
      for (i=1; i<NUMBER_OF_PUBLIC_IPS && !done; i++) {
	if (!vnetconfig->publicips[i].ip) {
	  if (!found) found=i;
	} else if (vnetconfig->publicips[i].ip == theip) {
	  done++;
	}
      }      
      
      if (done) {
	//already there
      } else if (found) {
	  vnetconfig->publicips[found].ip = theip;
      } else {
	logprintfl(EUCAERROR, "cannot add any more public IPS (limit:%d)\n", NUMBER_OF_PUBLIC_IPS);
	return(1);
      }
    }
  }

  return(0);
}

int vnetAssignAddress(vnetConfig *vnetconfig, char *src, char *dst) {
  int rc=0;
  char cmd[256];

  if ((vnetconfig->role == CC || vnetconfig->role == CLC) && (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN"))) {

    snprintf(cmd, 255, "-A PREROUTING -d %s -j DNAT --to %s", src, dst);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);

    snprintf(cmd, 255, "-A POSTROUTING -s %s -j SNAT --to %s", dst, src);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
  }
  return(rc);
}

int vnetAllocatePublicIP(vnetConfig *vnetconfig, char *ip, char *dstip) {
  return(vnetSetPublicIP(vnetconfig, ip, dstip, 1));
}

int vnetDeallocatePublicIP(vnetConfig *vnetconfig, char *ip, char *dstip) {
  return(vnetSetPublicIP(vnetconfig, ip, NULL, 0));
}

int vnetSetPublicIP(vnetConfig *vnetconfig, char *ip, char *dstip, int setval) {
  int i, done;
  uint32_t hip;
  
  if (param_check("vnetSetPublicIP", vnetconfig, ip, setval)) return(1);
  
  hip = dot2hex(ip);
  
  done=0;
  for (i=1; i<NUMBER_OF_PUBLIC_IPS && !done; i++) {
    if (vnetconfig->publicips[i].ip == hip) {
      if (dstip) {
	vnetconfig->publicips[i].dstip = dot2hex(dstip);
      } else {
	vnetconfig->publicips[i].dstip = 0;
      }
      vnetconfig->publicips[i].allocated = setval;
      done++;
    }
  }
  return(0);

}

int vnetUnassignAddress(vnetConfig *vnetconfig, char *src, char *dst) {
  int rc=0, count;
  char cmd[256];
  
  if ((vnetconfig->role == CC || vnetconfig->role == CLC) && (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN"))) {

    snprintf(cmd, 255, "-D PREROUTING -d %s -j DNAT --to %s", src, dst);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
    count=0;
    while(rc != 0 && count < 10) {
      rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
      count++;
    }

    snprintf(cmd, 255, "-D POSTROUTING -s %s -j SNAT --to %s", dst, src);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
    count=0;
    while(rc != 0 && count < 10) {
      rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
      count++;
    }
  }
  return(rc);
}

int vnetStopNetwork(vnetConfig *vnetconfig, int vlan, char *userName, char *netName) {
  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC")) {
    return(0);
  }
  return(vnetStopNetworkManaged(vnetconfig, vlan, userName, netName));
}


int fill_arp(char *subnet) {
  int pid, status, rc;

  if (!subnet) return(1);
  
  pid = fork();
  if (!pid) {
    char arga[1024];
    char cmd[1024];
    int sid;
    sid = setsid();
    rc = chdir("/");
    close(0);
    close(1);
    close(2);
    snprintf(arga, 1024, "%s.255", subnet);
    snprintf(cmd, 1024, "ping -b -c 1 %s", arga);
    rc = system(cmd);
    exit(0);
    exit(execlp("ping", "ping", "-b", "-c", "1", arga, NULL));
  }
  wait(&status);

  return(0);
}

int discover_mac(vnetConfig *vnetconfig, char *mac, char **ip) {
  int rc, i, j;
  char cmd[1024], rbuf[256], *tok;
  struct ifreq ifinfo;
  FILE *FH=NULL;

  if (mac == NULL || ip == NULL) {
    return(1);
  }

  FH=fopen("/proc/net/arp", "r");
  if (!FH) {
    return(1);
  }
  while(fgets(rbuf, 256, FH) != NULL) {
    if (strstr(rbuf, mac)) {
      tok = strtok(rbuf, " ");
      if (tok != NULL) {
	*ip = strdup(tok);
	fclose(FH);
	return(0);
      }
    }
  }
  fclose(FH);

  return(1);
}

uint32_t dot2hex(char *in) {
  int a, b, c, d;

  sscanf(in, "%d.%d.%d.%d", &a, &b, &c, &d);
  a = a<<24;
  b = b<<16;
  c = c<<8;
  
  return(a|b|c|d);
}

char *hex2dot(uint32_t in) {
  char out[16];
  bzero(out, 16);

  snprintf(out, 16, "%u.%u.%u.%u", (in & 0xFF000000)>>24, (in & 0x00FF0000)>>16, (in & 0x0000FF00)>>8, in & 0x000000FF);

  return(strdup(out));
}

int vnetSaveIPTables(vnetConfig *vnetconfig) {
  char cmd[256];
  int rc;

  snprintf(cmd, 255, "%s/usr/share/eucalyptus/euca_rootwrap iptables-save > %s/iptables-state", vnetconfig->eucahome, vnetconfig->path);
  rc = system(cmd);
  return(WEXITSTATUS(rc));
}

int vnetLoadIPTables(vnetConfig *vnetconfig) {
  char cmd[256], file[1024];
  struct stat statbuf;
  int rc=0;

  snprintf(file, 1023, "%s/iptables-preload", vnetconfig->path);
  if (stat(file, &statbuf) == 0) {
    snprintf(cmd, 255, "%s/usr/share/eucalyptus/euca_rootwrap iptables-restore < %s", vnetconfig->eucahome, file);
    rc = system(cmd);
  }

  snprintf(file, 1023, "%s/iptables-state", vnetconfig->path);
  if (stat(file, &statbuf) == 0) {
    snprintf(cmd, 255, "%s/usr/share/eucalyptus/euca_rootwrap iptables-restore < %s", vnetconfig->eucahome, file);
    rc = system(cmd);
  }
  return(WEXITSTATUS(rc));
}
