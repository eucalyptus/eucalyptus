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
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#define _FILE_OFFSET_BITS 64 // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <math.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pthread.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdarg.h>
#include <ifaddrs.h>
#include <math.h> /* log2 */
#include <sys/socket.h>
#include <netdb.h>
#include <arpa/inet.h>

#include <sys/ioctl.h>
#include <net/if.h>  

#include <vnetwork.h>
#include <misc.h>
#include <hash.h>

char *iptablesCache=NULL;

int vnetInit(vnetConfig *vnetconfig, char *mode, char *eucahome, char *path, int role, char *pubInterface, char *privInterface, char *numberofaddrs, char *network, char *netmask, char *broadcast, char *nameserver, char *domainname, char *router, char *daemon, char *dhcpuser, char *bridgedev, char *localIp, char *macPrefix) {
  uint32_t nw=0, nm=0, unw=0, unm=0, dns=0, bc=0, rt=0, rc=0, slashnet=0, *ips=NULL, *nms=NULL;
  int vlan=0, numaddrs=1, len, i;
  char cmd[256];

  //  if (param_check("vnetInit", vnetconfig, mode, eucahome, path, role, pubInterface, numberofaddrs, network, netmask, broadcast, nameserver, router, daemon, bridgedev)) return(1);
  
  if (!vnetconfig->initialized) {
    bzero(vnetconfig, sizeof(vnetConfig));
    // always need 'mode' set
    if (mode) {
      safe_strncpy(vnetconfig->mode, mode, 32);
    } else {
      logprintfl(EUCAERROR, "vnetInit(): VNET_MODE is not set\n");
      return(1);
    }
    if (role != CLC && role != NC) {
      logprintfl(EUCAERROR, "vnetInit(): bad role specified\n");
      return(1);
    }

    //check mode specific parameters
    if (!strcmp(mode, "SYSTEM")) {
      if (role == CLC) {
      } else if (role == NC) {
	if (!bridgedev || check_bridge(bridgedev)) {
	  logprintfl (EUCAERROR, "vnetInit(): cannot verify VNET_BRIDGE(%s), please check parameters and bridge device\n", SP(bridgedev));
	  return(1);
	}
      }
    } else if (!strcmp(mode, "STATIC") || !strcmp(mode, "STATIC-DYNMAC")) {
      if (role == CLC) {
	if (!daemon || check_file(daemon)) {
	  logprintfl(EUCAERROR, "vnetInit(): cannot verify VNET_DHCPDAEMON (%s), please check parameter and location\n", SP(daemon));
	  return(1);
	}
	if (!privInterface || check_device(privInterface)) {
	  logprintfl(EUCAERROR, "vnetInit(): cannot verify VNET_PRIVINTERFACE (%s), please check parameter and device name\n", SP(privInterface));
	  return(1);
	}
	if (!network || !netmask || !broadcast || !nameserver || !router) {
	  logprintfl(EUCAERROR, "vnetInit(): cannot verify network settings (VNET_SUBNET(%s), VNET_NETMASK(%s), VNET_BROADCAST(%s), VNET_DNS(%s), VNET_ROUTER(%s)), please check parameters\n", SP(network), SP(netmask), SP(broadcast), SP(nameserver), SP(router));
	  return(1);
	}
      } else if (role == NC) {
	if (!strcmp(mode, "STATIC-DYNMAC")) {
	  if (!pubInterface || check_device(pubInterface)) {
	    logprintfl(EUCAERROR, "vnetInit(): cannot verify VNET_PUBINTERFACE(%s), please check parameters and device\n", SP(pubInterface));
	    return(1);
	  }
	}
      }
    } else if (!strcmp(mode, "MANAGED-NOVLAN")) {
      if (role == CLC) {
	if (!daemon || check_file(daemon)) {
	  logprintfl(EUCAERROR, "vnetInit(): cannot verify VNET_DHCPDAEMON (%s), please check parameter and location\n", SP(daemon));
	  return(1);
	}
	if (!pubInterface || check_device(pubInterface)) {
	  logprintfl(EUCAERROR, "vnetInit(): cannot verify VNET_PUBINTERFACE (%s), please check parameter and device name\n", SP(pubInterface));
	  return(1);
	}
	if (!privInterface || check_device(privInterface)) {
	  logprintfl(EUCAERROR, "vnetInit(): cannot verify VNET_PRIVINTERFACE (%s), please check parameter and device name\n", SP(privInterface));
	  return(1);
	}
	if (!network || !netmask || !nameserver) {
	  logprintfl(EUCAERROR, "vnetInit(): cannot verify network settings (VNET_SUBNET(%s), VNET_NETMASK(%s), VNET_DNS(%s), please check parameters\n", SP(network), SP(netmask), SP(nameserver));
	  return(1);
	}
      } else if (role == NC) {
	if (!bridgedev || check_bridge(bridgedev)) {
	  logprintfl (EUCAERROR, "vnetInit(): cannot verify VNET_BRIDGE(%s), please check parameters and bridge device\n");
	  return(1);
	}
      }
    } else if (!strcmp(mode, "MANAGED")) {
      if (role == CLC) {
	if (!daemon || check_file(daemon)) {
	  logprintfl(EUCAERROR, "vnetInit(): cannot verify VNET_DHCPDAEMON (%s), please check parameter and location\n", SP(daemon));
	  return(1);
	}
	if (!pubInterface || check_device(pubInterface)) {
	  logprintfl(EUCAERROR, "vnetInit(): cannot verify VNET_PUBINTERFACE (%s), please check parameter and device name\n", SP(pubInterface));
	  return(1);
	}
	if (!privInterface || check_device(privInterface)) {
	  logprintfl(EUCAERROR, "vnetInit(): cannot verify VNET_PRIVINTERFACE (%s), please check parameter and device name\n", SP(privInterface));
	  return(1);
	}
	if (!network || !netmask || !nameserver) {
	  logprintfl(EUCAERROR, "vnetInit(): cannot verify network settings (VNET_SUBNET(%s), VNET_NETMASK(%s), VNET_DNS(%s)), please check parameters\n", SP(network), SP(netmask), SP(nameserver));
	  return(1);
	}
      } else if (role == NC) {
      }
    } else {
      logprintfl(EUCAERROR, "vnetInit(): invalid networking mode %s, please check VNET_MODE parameter\n", SP(mode));
      return(1);
    }
    
    if (macPrefix) safe_strncpy(vnetconfig->macPrefix, macPrefix, 6);
    if (path) safe_strncpy(vnetconfig->path, path, MAX_PATH);
    if (eucahome) safe_strncpy(vnetconfig->eucahome, eucahome, MAX_PATH);
    if (pubInterface) safe_strncpy(vnetconfig->pubInterface, pubInterface, 32);
    if (bridgedev) safe_strncpy(vnetconfig->bridgedev, bridgedev, 32);
    if (daemon) safe_strncpy(vnetconfig->dhcpdaemon, daemon, MAX_PATH);
    if (privInterface) safe_strncpy(vnetconfig->privInterface, privInterface, 32);
    if (dhcpuser) safe_strncpy(vnetconfig->dhcpuser, dhcpuser, 32);
    if (domainname) {
      safe_strncpy(vnetconfig->euca_domainname, domainname, 256);
    } else {
      strncpy(vnetconfig->euca_domainname, "eucalyptus", strlen("eucalyptus") + 1);
    }

    if (localIp) {
      char *ipbuf=NULL;
      ipbuf = host2ip(localIp);
      if (ipbuf) {
	vnetAddLocalIP(vnetconfig, dot2hex(ipbuf));
	free(ipbuf);
      }
    }

    vnetconfig->tunnels.localIpId = -1;
    vnetconfig->tunnels.localIpIdLast = -1;
    vnetconfig->tunnels.tunneling = 0;
    vnetconfig->role = role;
    vnetconfig->enabled=1;
    vnetconfig->initialized = 1;
    vnetconfig->max_vlan = NUMBER_OF_VLANS;
    if (numberofaddrs) {
      if (atoi(numberofaddrs) > NUMBER_OF_HOSTS_PER_VLAN) {
	logprintfl(EUCAWARN, "vnetInit(): specified ADDRSPERNET exceeds maximum addresses per network (%d), setting to maximum.\n", NUMBER_OF_HOSTS_PER_VLAN);
	vnetconfig->numaddrs = NUMBER_OF_HOSTS_PER_VLAN;
      } else if (atoi(numberofaddrs) <= NUMBER_OF_CCS) {
	logprintfl(EUCAWARN, "vnetInit(): specified ADDRSPERNET lower than absolute minimum (16) setting to minimum.\n");
	vnetconfig->numaddrs = 16;
      } else {
	vnetconfig->numaddrs = atoi(numberofaddrs);
      }
    }
    vnetconfig->addrIndexMin = NUMBER_OF_CCS+1;
    vnetconfig->addrIndexMax = vnetconfig->numaddrs-2;
    
    if (network) vnetconfig->nw = dot2hex(network);
    if (netmask) vnetconfig->nm = dot2hex(netmask);

    // populate networks
    bzero(vnetconfig->users, sizeof(userEntry) * NUMBER_OF_VLANS);
    bzero(vnetconfig->networks, sizeof(networkEntry) * NUMBER_OF_VLANS);
    bzero(vnetconfig->etherdevs, NUMBER_OF_VLANS * MAX_ETH_DEV_PATH);
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
	// do some parameter checking
	if ( (numaddrs+1) < 4) {
	  logprintfl(EUCAERROR, "vnetInit(): NUMADDRS must be >= 4, instances will not start with current value of '%d'\n", numaddrs+1);
	}
	
	// check to make sure our specified range is big enough for all VLANs
	if ((0xFFFFFFFF - nm) < (NUMBER_OF_VLANS * (numaddrs+1))) {
	  // not big enough
	  vnetconfig->max_vlan = (0xFFFFFFFF - nm) / (numaddrs+1);
	  logprintfl(EUCAWARN, "vnetInit(): private network is not large enough to support all vlans, restricting to max vlan '%d'\n", vnetconfig->max_vlan);
	  if (vnetconfig->max_vlan < 2) {
	    logprintfl(EUCAWARN, "vnetInit(): Instances will not run with current max vlan '%d'.  Either increase the size of your private subnet (VNET_SUBNET/VNET_NETMASK) or decrease the number of addrs per group (VNET_ADDRSPERNET).\n", vnetconfig->max_vlan);
	  }
	} else {
	  vnetconfig->max_vlan = NUMBER_OF_VLANS;
	}

	// set up iptables
	snprintf(cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap iptables -L -n", vnetconfig->eucahome);
	rc = system(cmd);

	logprintfl(EUCADEBUG, "vnetInit(): flushing 'filter' table\n");
	rc = vnetApplySingleTableRule(vnetconfig, "filter", "-F");
	
	logprintfl(EUCADEBUG, "vnetInit(): flushing 'nat' table\n");
	rc = vnetApplySingleTableRule(vnetconfig, "nat", "-F");
	
	if (path) {
	  vnetLoadIPTables(vnetconfig);
	}
	
	rc = vnetApplySingleTableRule(vnetconfig, "filter", "-P FORWARD DROP");
	
	rc = vnetApplySingleTableRule(vnetconfig, "filter", "-A FORWARD -m conntrack --ctstate ESTABLISHED -j ACCEPT");
	
	slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - nm)) + 1);
	snprintf(cmd, 256, "-A FORWARD -d ! %s/%d -j ACCEPT", network, slashnet);
	rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);

	snprintf(cmd, 256, "-A POSTROUTING -d ! %s/%d -s %s/%d -j MASQUERADE", network, slashnet, network, slashnet);
	rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);

	//	snprintf(cmd, 256, "-A POSTROUTING -d %s/%d -j MASQUERADE", network, slashnet);
	//	rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);

	unm = 0xFFFFFFFF - numaddrs;
	unw = nw;
	for (vlan=2; vlan<vnetconfig->max_vlan; vlan++) {
	  vnetconfig->networks[vlan].nw = unw;
	  vnetconfig->networks[vlan].nm = unm;
	  vnetconfig->networks[vlan].bc = unw + numaddrs;
	  vnetconfig->networks[vlan].dns = dns;
	  vnetconfig->networks[vlan].router = unw+1;
	  unw += numaddrs + 1;
	}	
      } else if (!strcmp(mode, "STATIC") || !strcmp(mode, "STATIC-DYNMAC")) {
	for (vlan=0; vlan<vnetconfig->max_vlan; vlan++) {
	  vnetconfig->networks[vlan].nw = nw;
	  vnetconfig->networks[vlan].nm = nm;
	  vnetconfig->networks[vlan].bc = bc;
	  vnetconfig->networks[vlan].dns = dns;
	  vnetconfig->networks[vlan].router = rt;
	  vnetconfig->numaddrs = 0xFFFFFFFF - nm;
	  if (vnetconfig->numaddrs > NUMBER_OF_PUBLIC_IPS) {
	    vnetconfig->numaddrs = NUMBER_OF_PUBLIC_IPS;
	  }
	  vnetconfig->addrIndexMin = NUMBER_OF_CCS+1;
	  vnetconfig->addrIndexMax = vnetconfig->numaddrs-2;
	}
      }
    } else {
      if (!strcmp(vnetconfig->mode, "SYSTEM")) {
	// set up iptables rule to log DHCP replies to syslog
	snprintf(cmd, 256, "-A FORWARD -p udp -m udp --sport 67:68 --dport 67:68 -j LOG --log-level 6");
	rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
	if (rc) {
	  logprintfl(EUCAWARN, "vnetInit(): could not add logging rule for DHCP replies, may not see instance IPs as they are assigned by system DHCP server");
	}
      }

      if (strcmp(vnetconfig->mode, "MANAGED")) {
	/*
	// if we're not in MANAGED mode, set up ebtables	
	snprintf(cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap ebtables -F FORWARD", vnetconfig->eucahome);
	rc = system(cmd);
	if (rc) {
	  logprintfl(EUCAWARN, "vnetInit(): could not flush ebtables FORWARD rules\n");
	}

	snprintf(cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap ebtables -P FORWARD DROP", vnetconfig->eucahome);
	rc = system(cmd);
	if (rc) {
	  logprintfl(EUCAWARN, "vnetInit(): could set default ebtables FORWARD policy to DROP\n");
	} 

	// forward non-VM traffic
	snprintf(cmd, 256, "-A FORWARD -i %s -j ACCEPT", vnetconfig->pubInterface);
	rc = vnetApplySingleEBTableRule(vnetconfig, "filter", cmd);
	if (rc) {
	  logprintfl(EUCAWARN, "vnetInit(): could set up default ebtables rule '%s'\n", cmd);
	} 
	
	// allow VM DHCP traffic
	snprintf(cmd, 256, "-A FORWARD -p IPv4 -d Broadcast -i ! %s --ip-proto udp --ip-dport 67:68 -j ACCEPT", vnetconfig->pubInterface);
	rc = vnetApplySingleEBTableRule(vnetconfig, "filter", cmd);
	if (rc) {
	  logprintfl(EUCAWARN, "vnetInit(): could set up default ebtables rule '%s'\n", cmd);
	}
	*/
      }
    }
    logprintfl(EUCAINFO, "vnetInit(): VNET Configuration: eucahome=%s, path=%s, dhcpdaemon=%s, dhcpuser=%s, pubInterface=%s, privInterface=%s, bridgedev=%s, networkMode=%s\n", SP(vnetconfig->eucahome), SP(vnetconfig->path), SP(vnetconfig->dhcpdaemon), SP(vnetconfig->dhcpuser), SP(vnetconfig->pubInterface), SP(vnetconfig->privInterface), SP(vnetconfig->bridgedev), SP(vnetconfig->mode));
  }
  return(0);
}

int vnetSetMetadataRedirect(vnetConfig *vnetconfig) {
  char cmd[256];
  int rc;

  if (!vnetconfig) {
    logprintfl(EUCAERROR, "vnetSetMetadataRedirect(): bad input params\n");
    return(1);
  }

  //network = hex2dot(vnetconfig->nw);
  //slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->nm)) + 1); 

  snprintf(cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr add 169.254.169.254 scope link dev %s", vnetconfig->eucahome, vnetconfig->privInterface);
  rc = system(cmd);
  
  if (vnetconfig->cloudIp != 0) {
    char *ipbuf;
    ipbuf = hex2dot(vnetconfig->cloudIp);
    //    snprintf(cmd, 256, "-A PREROUTING -s %s/%d -d 169.254.169.254 -p tcp --dport 80 -j DNAT --to-destination %s:8773", network, slashnet, ipbuf);
    snprintf(cmd, 256, "-A PREROUTING -d 169.254.169.254 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", ipbuf);
    if (ipbuf) free(ipbuf);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
  } else {
    //    snprintf(cmd, 256, "-A PREROUTING -s %s/%d -d 169.254.169.254 -p tcp --dport 80 -j DNAT --to-destination 169.254.169.254:8773", network, slashnet);
    logprintfl(EUCAWARN, "vnetSetMetadataRedirect(): cloudIp is not yet set, not installing redirect rule\n");
  }
  
  //  if (network) free(network);

  return(0);
}

int vnetUnsetMetadataRedirect(vnetConfig *vnetconfig) {
  char cmd[256];
  int rc;

  if (!vnetconfig) {
    logprintfl(EUCAERROR, "vnetUnsetMetadataRedirect(): bad input params\n");
    return(1);
  }

  snprintf(cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr del 169.254.169.254 scope link dev %s", vnetconfig->eucahome, vnetconfig->privInterface);
  rc = system(cmd);
  
  if (vnetconfig->cloudIp != 0) {
    char *ipbuf;
    ipbuf = hex2dot(vnetconfig->cloudIp);
    snprintf(cmd, 256, "-D PREROUTING -d 169.254.169.254 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", ipbuf);
    if (ipbuf) free(ipbuf);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
  } else {
    logprintfl(EUCAWARN, "vnetUnsetMetadataRedirect(): cloudIp is not yet set, not installing redirect rule\n");
  }

  return(0);
}

int vnetInitTunnels(vnetConfig *vnetconfig) {
  int done=0, ret=0, rc=0;
  char file[MAX_PATH], *template=NULL, *pass=NULL;

  vnetconfig->tunnels.tunneling = 0;
  ret = 0;
  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    if (vnetCountLocalIP(vnetconfig) <= 0) {
      // localIp not set, no tunneling
      logprintfl(EUCAWARN, "vnetInitTunnels(): VNET_LOCALIP not set, tunneling is disabled\n");
      return(0);
    } else if (!strcmp(vnetconfig->mode, "MANAGED-NOVLAN") && check_bridge(vnetconfig->privInterface)) {
      logprintfl(EUCAWARN, "vnetInitTunnels(): in MANAGED-NOVLAN mode, priv interface '%s' must be a bridge, tunneling disabled\n", vnetconfig->privInterface);
      return(0);
    } else {
      ret = 0;
      snprintf(file, MAX_PATH, "%s/var/lib/eucalyptus/keys/vtunpass", vnetconfig->eucahome);
      if (check_file(file)) {
	logprintfl(EUCAWARN, "vnetInitTunnels(): cannot locate tunnel password file '%s', tunneling disabled\n", file);
	ret = 1;
      } else if (!check_file_newer_than(file, vnetconfig->tunnels.tunpassMtime)) {
	ret = 1;
	logprintfl(EUCADEBUG, "vnetInitTunnels(): tunnel password file has changed, reading new value\n");
	pass = file2str(file);
	if (pass) {
	  char *newl;
	  newl = strchr(pass, '\n');
	  if (newl) *newl = '\0';
	  snprintf(file, MAX_PATH, "%s/etc/eucalyptus/vtunall.conf.template", vnetconfig->eucahome);
	  template = file2str(file);
	  if (template) {
	    replace_string(&template, "VPASS", pass);
	    vnetconfig->tunnels.tunpassMtime = time(NULL);
	    done++;
	  }
	  free(pass);
	}
	if (done) {
	  // success
	  snprintf(file, MAX_PATH, "%s/var/lib/eucalyptus/keys/vtunall.conf", vnetconfig->eucahome);
	  rc = write2file(file, template);
	  if (rc) {
	    // error
	    logprintfl(EUCAERROR, "vnetInitTunnels(): cannot write vtun config file '%s', tunneling disabled\n", file);
	  } else {
	    vnetconfig->tunnels.tunneling = 1;
	    ret = 0;
	  }
	} else {
	  logprintfl(EUCAERROR, "vnetInitTunnels(): cannot set up tunnel configuration file, tunneling is disabled\n");
	}
	if (template) free(template);
      } else {
	ret=0;
      }
    }
  }
  // enable tunneling if all went well
  if (!ret) {
    vnetconfig->tunnels.tunneling = 1;
  }
  return(ret);
}


int vnetAddHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan, int idx) {
  int i, done, found, start, stop;
  char *newip;

  if (param_check("vnetAddHost", vnetconfig, mac, ip, vlan)) return(1);

  if (!vnetconfig->enabled) {
    logprintfl(EUCADEBUG,"vnetAddHost(): network support is not enabled\n");
    return(1);
  }
  
  if (idx < 0) {
    start = vnetconfig->addrIndexMin;
    stop = vnetconfig->addrIndexMax;
  } else if (idx >= vnetconfig->addrIndexMin && idx <= (vnetconfig->addrIndexMax)) {
    start = idx;
    stop = idx;
  } else {
    logprintfl(EUCAERROR, "vnetAddHost(): index out of bounds: idx=%d, min=%d max=%d\n", idx, vnetconfig->addrIndexMin, vnetconfig->addrIndexMax);
    return(1);
  }

  done=found=0;
  for (i=start; i<=stop && !done; i++) {
    if (!maczero(vnetconfig->networks[vlan].addrs[i].mac)) {
      if (!found) found=i;
    } else if (!machexcmp(mac, vnetconfig->networks[vlan].addrs[i].mac)) {
      done++;
    }
  }
  
  if (done) {
    // duplicate IP found
    logprintfl(EUCAWARN,"vnetAddHost(): attempting to add duplicate macmap entry, ignoring\n");
  } else if (found) {
    //    strncpy(vnetconfig->networks[vlan].addrs[found].mac, mac, 24);
    mac2hex(mac, vnetconfig->networks[vlan].addrs[found].mac);
    if (ip) {
      vnetconfig->networks[vlan].addrs[found].ip = dot2hex(ip);
    } else {
      newip = hex2dot(vnetconfig->networks[vlan].nw + found);
      if (!newip) {
         logprintfl(EUCAWARN,"vnetAddHost(): Out of memory\n");
      } else {
         vnetconfig->networks[vlan].addrs[found].ip = dot2hex(newip);
         free(newip);
      }
    }
    vnetconfig->networks[vlan].numhosts++;
  } else {
    logprintfl(EUCAERROR,"vnetAddHost(): failed to add host %s on vlan %d\n", mac, vlan);
    return(1);
  }
  return(0);
}

int vnetDelHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan) {
  int i, done;
  
  if (param_check("vnetDelHost", vnetconfig, mac, ip, vlan)) return(1);

  if (!vnetconfig->enabled) {
    logprintfl(EUCADEBUG,"vnetDelHost(): network support is not enabled\n");
    return(1);
  }
  
  done=0;
  //  for (i=2; i<NUMBER_OF_HOSTS_PER_VLAN && !done; i++) {
  for (i=vnetconfig->addrIndexMin; i<=vnetconfig->addrIndexMax && !done; i++) {
    //    if ( (!mac || !strcmp(vnetconfig->networks[vlan].addrs[i].mac, mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)) ) ) {
    if ( (!mac || !machexcmp(mac, vnetconfig->networks[vlan].addrs[i].mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)) ) ) {
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

int vnetRefreshHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan, int idx) {
  int i, done, found, start, stop;
  
  //  if (param_check("vnetRefreshHost", vnetconfig, mac, ip, vlan)) return(1);

  if (!vnetconfig->enabled) {
    logprintfl(EUCADEBUG,"vnetRefreshHost(): network support is not enabled\n");
    return(1);
  }

  if (idx < 0) {
    start = vnetconfig->addrIndexMin;
    stop = vnetconfig->addrIndexMax;
  } else if (idx >= vnetconfig->addrIndexMin && idx <= (vnetconfig->addrIndexMax)) {
    start = idx;
    stop = idx;
  } else {
    logprintfl(EUCAERROR, "vnetRefreshHost(): index out of bounds: idx=%d, min=%d max=%d\n", idx, vnetconfig->addrIndexMin, vnetconfig->addrIndexMax);
    return(1);
  }
  
  done=found=0;
  for (i=start; i<=stop && !done; i++) {

    if (ip) {
      if (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)) {
	found=i;
	done++;
      }
    }
    if (mac) {
      if (!machexcmp(mac, vnetconfig->networks[vlan].addrs[i].mac)) {
	found=i;
	done++;
      }
    }
  }

  if (!done) {
    return(vnetAddHost(vnetconfig, mac, ip, vlan, idx));
  } else {
    if (mac) {
      mac2hex(mac, vnetconfig->networks[vlan].addrs[found].mac);
    }
    if (ip) {
      vnetconfig->networks[vlan].addrs[found].ip = dot2hex(ip);      
    }
  }
  
  return(0);
}
int vnetEnableHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan) {
  int i, done;
  
  if (param_check("vnetEnableHost", vnetconfig, mac, ip, vlan)) return(1);

  if (!vnetconfig->enabled) {
    logprintfl(EUCADEBUG,"vnetEnableHost(): network support is not enabled\n");
    return(1);
  }
  
  done=0;
  //  for (i=2; i<NUMBER_OF_HOSTS_PER_VLAN && !done; i++) {
  for (i=vnetconfig->addrIndexMin; i<=vnetconfig->addrIndexMax && !done; i++) {
    //    if ( (!mac || !strcmp(vnetconfig->networks[vlan].addrs[i].mac, mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)) ) ) {
    if ( (!mac || !machexcmp(mac, vnetconfig->networks[vlan].addrs[i].mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)) ) ) {
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
    logprintfl(EUCADEBUG,"vnetDisableHost(): network support is not enabled\n");
    return(1);
  }

  done=0;
  //  for (i=2; i<NUMBER_OF_HOSTS_PER_VLAN && !done; i++) {
  for (i=vnetconfig->addrIndexMin; i<=vnetconfig->addrIndexMax && !done; i++) {
    //    if ( (!mac || !strcmp(vnetconfig->networks[vlan].addrs[i].mac, mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)) ) ) {
    if ( (!mac || !machexcmp(mac, vnetconfig->networks[vlan].addrs[i].mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)) ) ) {
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
  char *hashChain=NULL, userNetString[MAX_PATH];
  
  if (param_check("vnetDeleteChain", vnetconfig, userName, netName)) return(1);

  snprintf(userNetString, MAX_PATH, "%s%s", userName, netName);
  rc = hash_b64enc_string(userNetString, &hashChain);
  if (rc) {
    logprintfl(EUCAERROR, "vnetDeleteChain(): cannot hash user/net string (userNetString=%s)\n", userNetString);
    return(1);
  }
  
  rc = check_chain(vnetconfig, userName, netName);
  logprintfl(EUCADEBUG, "vnetDeleteChain(): params: userName=%s, netName=%s, rc=%d\n", SP(userName), SP(netName), rc);
  if (!rc) {
    snprintf(cmd, 256, "-D FORWARD -j %s", hashChain);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    if (rc) {
      logprintfl(EUCAERROR, "vnetDeleteChain(): '%s' failed; cannot remove link to chain %s\n", cmd, hashChain);
    }
    runcount=0;
    while(!rc && runcount < 10) {
      logprintfl(EUCADEBUG, "vnetDeleteChain(): duplicate rule found, removing others: %d/%d\n", runcount, 10);
      rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
      runcount++;
    }
    
    snprintf(cmd, 256, "-F %s", hashChain);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    if (rc) {
      logprintfl(EUCAERROR, "vnetDeleteChain(): '%s' failed; cannot flush chain %s\n", cmd, hashChain);
    }
    
    snprintf(cmd, 256, "-X %s", hashChain);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    if (rc) {
      logprintfl(EUCAERROR, "vnetDeleteChain(): '%s' failed; cannot remove chain %s\n", cmd, hashChain);
    }
    runcount=0;
    while(!rc && runcount < 10) {
      logprintfl(EUCADEBUG, "vnetDeleteChain(): duplicate rule found, removing others: %d/%d\n", runcount, 10);
      rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
      runcount++;
    }
  }

  if(hashChain)
     free(hashChain);
  
  return(0);
}

int vnetCreateChain(vnetConfig *vnetconfig, char *userName, char *netName) {
  char cmd[256];
  int rc, ret, count;
  char *hashChain=NULL, userNetString[MAX_PATH];
  
  if (param_check("vnetCreateChain", vnetconfig, userName, netName)) return(1);

  snprintf(userNetString, MAX_PATH, "%s%s", userName, netName);
  rc = hash_b64enc_string(userNetString, &hashChain);
  if (rc) {
    logprintfl(EUCAERROR, "vnetCreateChain(): cannot hash user/net string (userNetString=%s)\n", userNetString);
    return(1);
  }

  ret = 0;
  rc = check_chain(vnetconfig, userName, netName);
  if (rc) {
    //      snprintf(cmd, 256, "-N %s-%s", userName, netName);
    snprintf(cmd, 256, "-N %s", hashChain);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    if (rc) {
      logprintfl(EUCAERROR, "vnetCreateChain(): '%s' failed; cannot create chain %s\n", cmd, hashChain);
      ret=1;
    }
  }    
  if (!ret) {
    snprintf(cmd, 256, "-D FORWARD -j %s", hashChain);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    count=0;
    while(!rc && count < 10) {
      rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
      count++;
    }

    //    snprintf(cmd, 256, "-A FORWARD -j %s-%s", userName, netName);
    snprintf(cmd, 256, "-A FORWARD -j %s", hashChain);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    if (rc) {
      logprintfl(EUCAERROR, "vnetCreateChain(): '%s' failed; cannot link to chain %s\n", cmd, hashChain);
      ret=1;
    }
  }

  if (hashChain) free(hashChain);

  return(ret);
}

int vnetSaveTablesToMemory(vnetConfig *vnetconfig) {
  int rc, fd, ret=0, rbytes;
  char *file, cmd[256];
  
  if (!vnetconfig) {
    logprintfl(EUCAERROR, "vnetSaveTablesToMemory(): bad input params\n");
    return(1);
  }
  
  file = strdup("/tmp/euca-ipt-XXXXXX");
  if (!file) {
    return(1);
  }
  
  fd = safe_mkstemp(file);
  if (fd < 0) {
    free(file);
    return(1);
  }
  chmod(file, 0644);
  close(fd);
  
  snprintf(cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap iptables-save > %s", vnetconfig->eucahome, file);
  rc = system(cmd);
  if (rc) {
    logprintfl(EUCAERROR, "vnetSaveTablesToMemory(): cannot save iptables state '%s'\n", cmd);
    ret = 1;
  } else {
    fd = open(file, O_RDONLY);
    if (fd < 0) {
      // error
    } else {
      // read file
      bzero(vnetconfig->iptables, 4194304);
      rbytes = 0;
      rc = read(fd, vnetconfig->iptables+rbytes, 4194303 - rbytes);
      while(rc > 0 && rbytes <= 4194303) {
	rbytes += rc;
	rc = read(fd, vnetconfig->iptables+rbytes, 4194303 - rbytes);
      }
      close(fd);
    }
  }
  
  unlink(file);
  free(file);

  return(ret);
}

int vnetRestoreTablesFromMemory(vnetConfig *vnetconfig) {
  int rc, fd, ret=0, wbytes;
  char *file, cmd[256];
  FILE *FH;

  if (!vnetconfig) {
    logprintfl(EUCAERROR, "vnetRestoreTablesFromMemory(): bad input params\n");
    return(1);
  } else if (vnetconfig->iptables[0] == '\0') {
    // nothing to do
    return(0);
  }
  
  file = strdup("/tmp/euca-ipt-XXXXXX");
  if (!file) {
    return(1);
  }
  fd = safe_mkstemp(file);
  if (fd < 0) {
    free(file);
    return(1);
  }
  chmod(file, 0644);
  FH = fdopen(fd, "w");
  if (!FH) {
    close(fd);
    unlink(file);
    free(file);
    return(1);
  }
  
  // write file
  fprintf(FH, "%s", vnetconfig->iptables);
  fclose(FH);
  close(fd);

  snprintf(cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap iptables-restore < %s", vnetconfig->eucahome, file);
  rc = system(cmd);
  if (rc) {
    logprintfl(EUCAERROR, "vnetRestoreTablesFromMemory(): cannot restore iptables state from memory '%s'\n", cmd);
    ret = 1;
  }

  unlink(file);
  free(file);
  return(ret);
}

int vnetFlushTable(vnetConfig *vnetconfig, char *userName, char *netName) {
  char cmd[256];
  int rc;
  char *hashChain=NULL, userNetString[MAX_PATH];
  int ret = 1;
  
  snprintf(userNetString, MAX_PATH, "%s%s", userName, netName);
  rc = hash_b64enc_string(userNetString, &hashChain);
  if (rc) {
    logprintfl(EUCAERROR, "vnetFlushTable(): cannot hash user/net string (userNetString=%s)\n", userNetString);
    return(1);
  }

  if ((userName && netName) && !check_chain(vnetconfig, userName, netName)) {
    snprintf(cmd, 256, "-F %s", hashChain);
    ret = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
  }

  if(hashChain)
     free(hashChain);

  return ret;
}

int vnetApplySingleEBTableRule(vnetConfig *vnetconfig, char *table, char *rule) {
  char cmd[MAX_PATH];
  int rc;

  if (!rule || !table || !vnetconfig) {
    logprintfl(EUCAERROR, "vnetApplySingleEBTableRule(): bad input params: table=%s, rule=%s\n", SP(table), SP(rule));
    return(1);
  }
  snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ebtables -t %s %s\n", vnetconfig->eucahome, table, rule);
  logprintfl(EUCADEBUG, "vnetApplySingleEBTableRule(): running cmd '%s'\n", cmd);
  rc = system(cmd);
  rc = rc>>8;
  return(rc);
}
int vnetApplySingleTableRule(vnetConfig *vnetconfig, char *table, char *rule) {
  int rc, fd, ret=0;
  char *file, cmd[256];
  FILE *FH;
  
  if (!rule || !table || !vnetconfig) {
    logprintfl(EUCAERROR, "vnetApplySingleTableRule(): bad input params: table=%s, rule=%s\n", SP(table), SP(rule));
    return(1);
  }
  
  logprintfl(EUCADEBUG, "vnetApplySingleTableRule(): applying single table (%s) rule (%s)\n", table, rule);

  file = strdup("/tmp/euca-ipt-XXXXXX");
  if (!file) {
    return(1);
  }
  fd = safe_mkstemp(file);
  if (fd < 0) {
    free(file);
    return(1);
  }
  chmod(file, 0644);
  FH = fdopen(fd, "w");
  if (!FH) {
    close(fd);
    unlink(file);
    free(file);
    return(1);
  }
  
  fprintf(FH, "%s\n", rule);
  fclose(FH);
  close(fd);
  
  snprintf(cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/euca_ipt %s %s", vnetconfig->eucahome, vnetconfig->eucahome, table, file);
  //  logprintfl(EUCADEBUG, "running cmd '%s'\n", cmd);
  rc = system(cmd);
  if (rc) {
    ret = 1;
  }
  unlink(file);
  free(file);
  
  rc = vnetSaveTablesToMemory(vnetconfig);
    
  return(ret);
}
								 

int vnetTableRule(vnetConfig *vnetconfig, char *type, char *destUserName, char *destName, char *sourceUserName, char *sourceNet, char *sourceNetName, char *protocol, int minPort, int maxPort) {
  int i, rc, done, destVlan, srcVlan, slashnet;
  char rule[1024], newrule[1024], srcNet[32], dstNet[32];
  char *tmp;
  char *hashChain=NULL, userNetString[MAX_PATH];
  
  //  logprintfl(EUCADEBUG, "vnetTableRule(): input: %s,%s,%s,%s,%s,%s,%d,%d\n",destUserName, destName, sourceUserName, sourceNet,sourceNetName,protocol,minPort,maxPort);
  if (param_check("vnetTableRule", vnetconfig, type, destUserName, destName, sourceNet, sourceUserName, sourceNetName)) return(1);

  snprintf(userNetString, MAX_PATH, "%s%s", destUserName, destName);
  rc = hash_b64enc_string(userNetString, &hashChain);
  if (rc) {
    logprintfl(EUCAERROR, "vnetTablRule(): cannot hash user/net string (userNetString=%s)\n", userNetString);
    return(1);
  }
  
  destVlan = vnetGetVlan(vnetconfig, destUserName, destName);
  if (destVlan < 0) {
    logprintfl(EUCAERROR,"vnetTableRule(): no vlans associated with active network %s/%s\n", destUserName, destName);
    if(hashChain)
       free(hashChain);
    return(1);
  }
  
  slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - vnetconfig->networks[destVlan].nm)+1))));
  tmp = hex2dot(vnetconfig->networks[destVlan].nw);
  snprintf(dstNet, 32, "%s/%d", tmp, slashnet);
  free(tmp);
  
  if (sourceNetName) {
    srcVlan = vnetGetVlan(vnetconfig, sourceUserName, sourceNetName);
    if (srcVlan < 0) {
      logprintfl(EUCAWARN,"vnetTableRule(): cannot locate active source vlan for network %s/%s, skipping\n", sourceUserName, sourceNetName);
      if(hashChain)
	 free(hashChain);
      return(0);
    } else {
      tmp = hex2dot(vnetconfig->networks[srcVlan].nw);
      snprintf(srcNet, 32, "%s/%d", tmp, slashnet);
      free(tmp);
    }
  } else {
    snprintf(srcNet, 32, "%s", sourceNet);
  }
  
  
  if (!strcmp(type, "firewall-open")) {
    snprintf(rule, 1024, "-A %s", hashChain);
    //    snprintf(rule, 1024, "iptables -A %s-%s", destUserName, destName);
  } else if (!strcmp(type, "firewall-close")) {
    snprintf(rule, 1024, "-D %s", hashChain);
    //    snprintf(rule, 1024, "iptables -D %s-%s", destUserName, destName);
  }
  
  free(hashChain);

  snprintf(newrule, 1024, "%s -s %s -d %s", rule, srcNet, dstNet);
  strcpy(rule, newrule);
  
  if (protocol) {
    snprintf(newrule, 1024, "%s -p %s", rule, protocol);
    strcpy(rule, newrule);
  }
  
  if (minPort && maxPort) {
    if (protocol && (!strcmp(protocol, "tcp") || !strcmp(protocol, "udp")) ) {
      if (minPort != maxPort) {
	snprintf(newrule, 1024, "%s -m %s --dport %d:%d", rule, protocol, minPort, maxPort);
      } else {
	snprintf(newrule, 1024, "%s -m %s --dport %d", rule, protocol, minPort);
      }
      strcpy(rule, newrule);
    }
  }
  
  snprintf(newrule, 1024, "%s -j ACCEPT", rule);
  strcpy(rule, newrule);
  
  if (!strcmp(type, "firewall-close")) {
    // this means that the network should already be flushed and empty (default policy == drop)
  } else {
    logprintfl(EUCAINFO,"vnetTableRule(): applying iptables rule: %s\n", rule);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", rule);
    //  rc = system(rule);
    if (rc) {
      logprintfl(EUCAERROR,"vnetTableRule(): iptables rule application failed: %d\n", rc);
      return(1);
    }
  }

  return(0);
}


int vnetSetVlan(vnetConfig *vnetconfig, int vlan, char *uuid, char *user, char *network) {
  
  if (param_check("vnetSetVlan", vnetconfig, vlan, user, network)) return(1);

  safe_strncpy(vnetconfig->users[vlan].userName, user, 48);
  safe_strncpy(vnetconfig->users[vlan].netName, network, 64);
  if (uuid) safe_strncpy(vnetconfig->users[vlan].uuid, uuid, 48);
  
  return(0);
}

int vnetGetVlan(vnetConfig *vnetconfig, char *user, char *network) {
  int i, done;
  
  done=0;
  for (i=0; i<vnetconfig->max_vlan; i++) {
    if (!strcmp(vnetconfig->users[i].userName, user) && !strcmp(vnetconfig->users[i].netName, network)) {
      if (!vnetconfig->networks[i].active) {
	// network exists, but is inactive
	return(-1 * i);
      }
      return(i);
    }
  }
  return(-1);
}

int vnetGetAllVlans(vnetConfig *vnetconfig, char ***outusers, char ***outnets, int *len) {
  int i, rc;

  if (!vnetconfig || !outusers || !outnets || !len) {
    logprintfl(EUCAERROR, "vnetGetAllVlans(): bad input parameters\n");
    return(1);
  }
  
  *outusers = malloc(sizeof (char *) * vnetconfig->max_vlan);
  if (!*outusers) {
    logprintfl(EUCAFATAL, "vnetGetAllVlans(): out of memory!\n");
    return(1);
  }

  *outnets = malloc(sizeof (char *) * vnetconfig->max_vlan);
  if (!*outnets) {
    logprintfl(EUCAFATAL, "vnetGetAllVlans(): out of memory!\n");
    if (*outusers) free(*outusers);
    return(1);
  }

  *len = 0;
  for (i=0; i<vnetconfig->max_vlan; i++) {
    char userNetString[MAX_PATH], netslash[24];
    char *net=NULL, *chain=NULL;
    int slashnet=0;
    if (vnetconfig->networks[i].active) {
      snprintf(userNetString, MAX_PATH, "%s%s", vnetconfig->users[i].userName, vnetconfig->users[i].netName);
      rc = hash_b64enc_string(userNetString, &chain);
      if (rc) {
	logprintfl(EUCAERROR, "vnetGetAllVlans(): cannot hash user/net string (userNetString=%s)\n", userNetString);
      } else {
	net = hex2dot(vnetconfig->networks[i].nw);
	slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[i].nm)) + 1);
	if (net && slashnet >= 0 && slashnet <= 32) {
	  //	  fprintf(FH, "%s %s/%d\n", chain, net, slashnet);
	  netslash[0] = '\0';
	  snprintf(netslash, 24, "%s/%d", net, slashnet);
	  (*outusers)[(*len)] = strdup(chain);
	  (*outnets)[(*len)] = strdup(netslash);
	  (*len)++;
	}
	if (net) free(net);
      }
      if(chain) free(chain);
    }
  }

  return(0);
}

int vnetGenerateNetworkParams(vnetConfig *vnetconfig, char *instId, int vlan, int nidx, char *outmac, char *outpubip, char *outprivip) {
  int rc, ret=0, networkIdx, found, i;
  uint32_t inip;
  
  if (!instId || !outmac || !outpubip || !outprivip) {
    logprintfl(EUCAERROR, "vnetGenerateNetworkParams(): bad input params\n");
    return(1);
  }
  
  ret = 1;
  // define/get next mac and allocate IP
  if (!strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC")) {
    // search for existing entry
    inip = dot2hex(outprivip);
    found=0;
    for (i=vnetconfig->addrIndexMin; i<vnetconfig->addrIndexMax && !found; i++) {
      //      logprintfl(EUCADEBUG, "HELLO: %d %s %s %s %d %d\n", i, outmac, hex2dot(inip), hex2dot(vnetconfig->networks[0].addrs[i].ip), machexcmp(outmac, vnetconfig->networks[0].addrs[i].mac), (vnetconfig->networks[0].addrs[i].ip == inip));
      if (!machexcmp(outmac, vnetconfig->networks[0].addrs[i].mac) && (vnetconfig->networks[0].addrs[i].ip == inip)) {
	//	logprintfl(EUCADEBUG, "WOOT: %d %s %s %s %d %d\n", i, outmac, hex2dot(inip), hex2dot(vnetconfig->networks[0].addrs[i].ip), machexcmp(outmac, vnetconfig->networks[0].addrs[i].mac), (vnetconfig->networks[0].addrs[i].ip == inip));
	vnetconfig->networks[0].addrs[i].active = 1;
	found++;
	ret=0;
      }
    }
    // get the next valid mac/ip pairing for this vlan
    if (!found) {
      outmac[0] = '\0';
      rc = vnetGetNextHost(vnetconfig, outmac, outprivip, 0, -1);
      if (!rc) {
	snprintf(outpubip, strlen(outprivip)+1, "%s", outprivip);
	ret = 0;
      }
    }
  } else if (!strcmp(vnetconfig->mode, "SYSTEM")) {
    if (!strlen(outmac)) {
      rc = instId2mac(vnetconfig, instId, outmac);
      if (rc) {
	logprintfl(EUCAERROR, "vnetGenerateNetworkParams(): unable to convert instanceId (%s) to mac address\n", instId);
	return(1);
      }
    }
    ret = 0;
  } else if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    if (!strlen(outmac)) {
      rc = instId2mac(vnetconfig, instId, outmac);
      if (rc) {
	logprintfl(EUCAERROR, "vnetGenerateNetworkParams(): unable to convert instanceId (%s) to mac address\n", instId);
	return(1);
      }
    }
 
    if (nidx == -1) {
      networkIdx = -1;
    } else {
      networkIdx = nidx;
    }
      
    // add the mac address to the virtual network
    rc = vnetAddHost(vnetconfig, outmac, NULL, vlan, networkIdx);
    if (!rc) {
      // get the next valid mac/ip pairing for this vlan
      rc = vnetGetNextHost(vnetconfig, outmac, outprivip, vlan, networkIdx);
      if (!rc) {
	ret = 0;
      }
    }
  }
  return(ret);
}
int vnetGetNextHost(vnetConfig *vnetconfig, char *mac, char *ip, int vlan, int idx) {
  int i, done, start, stop;
  char *newip, *newmac;
  
  if (param_check("vnetGetNextHost", vnetconfig, mac, ip, vlan)) return(1);

  if (!vnetconfig->enabled) {
    logprintfl(EUCADEBUG,"vnetGetNextHost(): network support is not enabled\n");
    return(1);
  }
  
  if (idx < 0) {
    start = vnetconfig->addrIndexMin;
    stop = vnetconfig->addrIndexMax;
  } else if (idx >= vnetconfig->addrIndexMin && idx <= (vnetconfig->addrIndexMax)) {
    start = idx;
    stop = idx;
  } else {
    logprintfl(EUCAERROR, "vnetGetNextHost(): index out of bounds: idx=%d, min=%d max=%d\n", idx, vnetconfig->addrIndexMin, vnetconfig->addrIndexMax);
    return(1);
  }
  
  done=0;
  for (i=start; i<=stop && !done; i++) {
    //    if (vnetconfig->networks[vlan].addrs[i].mac[0] != '\0' && vnetconfig->networks[vlan].addrs[i].ip != 0 && vnetconfig->networks[vlan].addrs[i].active == 0) {
    if (maczero(vnetconfig->networks[vlan].addrs[i].mac) && vnetconfig->networks[vlan].addrs[i].ip != 0 && vnetconfig->networks[vlan].addrs[i].active == 0) {
      //      strncpy(mac, vnetconfig->networks[vlan].addrs[i].mac, 24);
      hex2mac(vnetconfig->networks[vlan].addrs[i].mac, &newmac);
      strncpy(mac, newmac, strlen(newmac));
      free(newmac);
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

int vnetCountLocalIP(vnetConfig *vnetconfig) {
  int count, i, ret;

  if (!vnetconfig) {
    return(0);
  }
  
  count=0;
  for (i=0; i<32; i++) {
    if (vnetconfig->localIps[i] != 0) {
      count++;
    }
  }
  return(count);
}

int vnetCheckLocalIP(vnetConfig *vnetconfig, uint32_t ip) {
  int i, done, ret;
  
  if (!vnetconfig) {
    return(1);
  }

  // local address? (127.0.0.0/8)
  if (ip >= 0x7F000000 && ip <= 0x7FFFFFFF) return(0);
  
  done=0;
  for (i=0; i<32; i++) {
    if (vnetconfig->localIps[i] == ip) {
      return(0);
    }
  }
  return(1);
}

int vnetAddLocalIP(vnetConfig *vnetconfig, uint32_t ip) {
  int i, done, foundone, ret;
  
  if (!vnetconfig) {
    return(1);
  }

  done=0;
  foundone = -1;
  for (i=0; i<32 && !done; i++) {
    if (vnetconfig->localIps[i] == ip) {
      return(0);
    }
    if (vnetconfig->localIps[i] == 0) {
      foundone = i;
      done++;
    }
  }
  if (foundone >= 0) {
    vnetconfig->localIps[foundone] = ip;
    ret = 0;
  } else {
    ret = 1;
  }

  return(ret);
}

int vnetAddDev(vnetConfig *vnetconfig, char *dev) {
  int i, done, foundone;

  if (param_check("vnetAddDev", vnetconfig, dev)) return(1);

  done=0;
  foundone = -1;
  for (i=0; i<vnetconfig->max_vlan && !done; i++) {
    if (!strcmp(vnetconfig->etherdevs[i], dev)) {
      return(1);
    }
    if (vnetconfig->etherdevs[i][0] == '\0') {
      foundone = i;
    }
  }
  if (foundone >= 0) {
    safe_strncpy(vnetconfig->etherdevs[foundone], dev, MAX_ETH_DEV_PATH);
  }
  return(0);
}

int vnetDelDev(vnetConfig *vnetconfig, char *dev) {
  int i, done;

  if (param_check("vnetDelDev", vnetconfig, dev)) return(1);

  done=0;
  for (i=0; i<vnetconfig->max_vlan && !done; i++) {
    if (!strncmp(vnetconfig->etherdevs[i], dev, MAX_ETH_DEV_PATH)) {
      bzero(vnetconfig->etherdevs[i], MAX_ETH_DEV_PATH);
      done++;
    }
  }
  return(0);
}

int vnetGenerateDHCP(vnetConfig *vnetconfig, int *numHosts) {
  FILE *fp=NULL;
  char fname[MAX_PATH],
    *network=NULL, 
    *netmask=NULL,
    *broadcast=NULL,
    *nameserver=NULL,
    *router=NULL, 
    *euca_nameserver=NULL,
    *mac=NULL, 
    *newip=NULL;
  char nameservers[1024];

  int i,j;

  *numHosts = 0;
  if (param_check("vnetGenerateDHCP", vnetconfig)) return(1);

  snprintf(fname, MAX_PATH, "%s/euca-dhcp.conf", vnetconfig->path);
  
  fp = fopen(fname, "w");
  if (fp == NULL) {
    return(1);
  }
  
  fprintf(fp, "# automatically generated config file for DHCP server\ndefault-lease-time 86400;\nmax-lease-time 86400;\nddns-update-style none;\n\n");
  
  fprintf(fp, "shared-network euca {\n");
  for (i=0; i<vnetconfig->max_vlan; i++) {
    if (vnetconfig->networks[i].numhosts > 0) {
      network = hex2dot(vnetconfig->networks[i].nw);
      netmask = hex2dot(vnetconfig->networks[i].nm);
      broadcast = hex2dot(vnetconfig->networks[i].bc);
      nameserver = hex2dot(vnetconfig->networks[i].dns);      
      if (vnetconfig->tunnels.localIpId < 0) {
	router = hex2dot(vnetconfig->networks[i].router);
      } else {
	router = hex2dot(vnetconfig->networks[i].router + vnetconfig->tunnels.localIpId);
	//router = hex2dot(vnetconfig->networks[i].router);
      }
      
      if (vnetconfig->euca_ns != 0) {
	euca_nameserver = hex2dot(vnetconfig->euca_ns);
	snprintf(nameservers, 1024, "%s, %s", nameserver, euca_nameserver);
      } else {
	snprintf(nameservers, 1024, "%s", nameserver);
      }	
      
      fprintf(fp, "subnet %s netmask %s {\n  option subnet-mask %s;\n  option broadcast-address %s;\n  option domain-name \"%s\";\n  option domain-name-servers %s;\n  option routers %s;\n}\n", network, netmask, netmask, broadcast, vnetconfig->euca_domainname, nameservers, router);
      
      if (euca_nameserver) free(euca_nameserver);
      if (nameserver) free(nameserver);
      if (network) free(network);
      if (netmask) free(netmask);
      if (broadcast) free(broadcast);
      if (router) free(router);
      

      //      for (j=2; j<NUMBER_OF_HOSTS_PER_VLAN; j++) {
      for (j=vnetconfig->addrIndexMin; j<=vnetconfig->addrIndexMax; j++) {
	if (vnetconfig->networks[i].addrs[j].active == 1) {
	  newip = hex2dot(vnetconfig->networks[i].addrs[j].ip);
	  //mac = vnetconfig->networks[i].addrs[j].mac;
	  hex2mac(vnetconfig->networks[i].addrs[j].mac, &mac);
	  fprintf(fp, "\nhost node-%s {\n  hardware ethernet %s;\n  fixed-address %s;\n}\n", newip, mac, newip);
	  (*numHosts)++;
	  if (mac) free(mac);
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
  struct stat statbuf;  
  char dstring [MAX_PATH] = "";
  char buf [MAX_PATH];
  char file[MAX_PATH];
  int rc, i, numHosts;

  if (param_check("vnetKickDHCP", vnetconfig)) return(1);

  if (!strcmp(vnetconfig->mode, "SYSTEM")) {
    return(0);
  }

  rc = vnetGenerateDHCP(vnetconfig, &numHosts);
  if (rc) {
    logprintfl(EUCAERROR, "vnetKickDHCP(): failed to (re)create DHCP config (%s/euca-dhcp.conf)\n", vnetconfig->path);
    return(1);
  } else if (numHosts <= 0) {
    // nothing to do
    return(0);
  }

  for (i=0; i<vnetconfig->max_vlan; i++) {
    if (vnetconfig->etherdevs[i][0] != '\0') {
      strncat (dstring, " ", MAX_PATH - 1);

      if((MAX_PATH - strlen(dstring) - 1) < MAX_ETH_DEV_PATH) {
	logprintfl(EUCAERROR, "vnetKickDHCP(): not enough buffer length left to copy ethernet dev name\n");
	return(1);
      }
      strncat (dstring, vnetconfig->etherdevs[i], MAX_ETH_DEV_PATH);
    }
  }

  /* force dhcpd to reload the conf */
  
  snprintf(file, MAX_PATH, "%s/euca-dhcp.pid", vnetconfig->path);
  if (stat(file, &statbuf) == 0) {
    char rootwrap[MAX_PATH];
    char *tmpstr=NULL;
    int tmppid = 0, tmpcount;

    snprintf(rootwrap, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", vnetconfig->eucahome);
    snprintf(buf, MAX_PATH, "%s/var/run/eucalyptus/net/euca-dhcp.pid", vnetconfig->eucahome);

    // little chunk of code to work-around bad dhcpd that takes some time to populate the pidfile...
    tmpstr = file2str(buf);
    if (tmpstr) {
      tmppid = atoi(tmpstr);
      free(tmpstr);
    }
    for (i=0; i<4 && tmppid <= 0; i++) {
      usleep(250000);
      tmpstr = file2str(buf);
      if (tmpstr) {
	tmppid = atoi(tmpstr);
	free(tmpstr);
      }
    }

    rc = safekillfile(buf, vnetconfig->dhcpdaemon, 9, rootwrap);
    if (rc) {
      logprintfl(EUCAWARN, "vnetKickDHCP(): failed to kill previous dhcp daemon\n");
    }
    usleep(250000);
  }
  
  snprintf (buf, MAX_PATH, "%s/euca-dhcp.trace", vnetconfig->path);
  unlink(buf);
  
  snprintf (buf, MAX_PATH, "%s/euca-dhcp.leases", vnetconfig->path);
  rc = open(buf, O_WRONLY | O_CREAT, 0644);
  if (rc != -1) {
    close(rc);
  } else {
    logprintfl(EUCAWARN, "vnetKickDHCP(): failed to create/open euca-dhcp.leases\n");
  }
  
  if (strncmp(vnetconfig->dhcpuser, "root", 32) && strncmp(vnetconfig->path, "/", MAX_PATH) && strstr(vnetconfig->path, "eucalyptus/net")) {
    snprintf(buf, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap chgrp -R %s %s", vnetconfig->eucahome, vnetconfig->dhcpuser, vnetconfig->path);
    logprintfl(EUCADEBUG, "vnetKickDHCP(): executing: %s\n", buf);
    rc = system(buf);
    
    snprintf(buf, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap chmod -R 0775 %s", vnetconfig->eucahome, vnetconfig->path);
    logprintfl(EUCADEBUG, "vnetKickDHCP(): executing: %s\n", buf);
    rc = system(buf);
  }
  
  snprintf (buf, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap %s -cf %s/euca-dhcp.conf -lf %s/euca-dhcp.leases -pf %s/euca-dhcp.pid -tf %s/euca-dhcp.trace %s", vnetconfig->eucahome, vnetconfig->dhcpdaemon, vnetconfig->path, vnetconfig->path, vnetconfig->path, vnetconfig->path, dstring);
  
  logprintfl(EUCAINFO, "vnetKickDHCP(): executing: %s\n", buf);
  // cannot use 'daemonrun()' here, dhcpd3 is too picky about FDs and signal handlers...
  rc = system(buf);
  logprintfl(EUCAINFO, "vnetKickDHCP(): RC from cmd: %d\n", rc);
  
  return(rc);
  
}

int vnetAddCCS(vnetConfig *vnetconfig, uint32_t cc) {
  int i;
  for (i=0; i<NUMBER_OF_CCS; i++) {
    if (vnetconfig->tunnels.ccs[i] == 0) {
      vnetconfig->tunnels.ccs[i] = cc;
      return(0);
    }
  }
  return(1);
}

int vnetDelCCS(vnetConfig *vnetconfig, uint32_t cc) {
  int i, rc;
  char file[MAX_PATH], rootwrap[MAX_PATH];
  char *pidstr;
  snprintf(rootwrap, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", vnetconfig->eucahome);

  for (i=0; i<NUMBER_OF_CCS; i++) {
    if (vnetconfig->tunnels.ccs[i] == cc) {
      // bring down the tunnel
      
      snprintf(file, MAX_PATH, "%s/var/run/eucalyptus/vtund-client-%d-%d.pid", vnetconfig->eucahome, vnetconfig->tunnels.localIpId, i);
      rc = safekillfile(file, "vtund", 9, rootwrap);

      vnetconfig->tunnels.ccs[i] = 0;
      return(0);
    }
  }
  return(1);
}

int vnetSetCCS(vnetConfig *vnetconfig, char **ccs, int ccsLen) {
  int i, j, found, lastj, localIpId=-1, rc;
  uint32_t tmpccs[NUMBER_OF_CCS];
  
  if (ccsLen < 0 || ccsLen > NUMBER_OF_CCS) {
    logprintfl(EUCAERROR, "vnetSetCCS(): specified number of cluster controllers out of bounds (in=%d, min=%d, max=%d)\n", ccsLen, 0, NUMBER_OF_CCS);
    return(1);
  }  
  
  found=0;
  for (i=0; i<ccsLen; i++) {
    logprintfl(EUCADEBUG, "vnetSetCCS(): input CC%d=%s\n", i, ccs[i]);
    tmpccs[i] = dot2hex(ccs[i]);
    rc = vnetCheckLocalIP(vnetconfig, tmpccs[i]);
    if (!rc && !found) {
      logprintfl(EUCADEBUG, "vnetSetCCS(): local IP found in input list of CCs, setting localIpId: %d\n", i);
      vnetconfig->tunnels.localIpIdLast = vnetconfig->tunnels.localIpId;
      vnetconfig->tunnels.localIpId = i;
      found=1;
    }    
  }
  if (!found) {
    logprintfl(EUCADEBUG, "vnetSetCCS(): local IP not found in input list of CCs, setting localIpId: %d\n", -1);
    vnetconfig->tunnels.localIpIdLast = vnetconfig->tunnels.localIpId;
    vnetconfig->tunnels.localIpId = -1;
  }
  return(0);

#if 0
  for (i=0; i<ccsLen; i++) {
    logprintfl(EUCADEBUG, "vnetSetCCS(): input CC=%s\n", ccs[i]);
    found=0;
    for (j=0; j<NUMBER_OF_CCS && !found; j++) {
      if (dot2hex(ccs[i]) == vnetconfig->tunnels.ccs[j]) {
	found=1;
      }
    }
    if (!found) {
      // exists in new list, but not locally, add it
      logprintfl(EUCADEBUG, "vnetSetCCS(): adding CC %s\n", ccs[i]);
      vnetAddCCS(vnetconfig, dot2hex(ccs[i]));
    }
  }
  
  for (i=0; i<NUMBER_OF_CCS; i++) {
    if (vnetconfig->tunnels.ccs[i] != 0) {
      found=0;
      for (j=0; j<ccsLen && !found; j++) {
	if (vnetconfig->tunnels.ccs[i] == dot2hex(ccs[j])) {
	  found=1;
	}
      }
      if (!found) {
	// exists locally, but not in new list, remove it
	logprintfl(EUCADEBUG, "vnetSetCCS(): removing CC %d\n", i);
	vnetDelCCS(vnetconfig, vnetconfig->tunnels.ccs[i]);
      }
    }
  }

  localIpId = -1;
  found=0;
  for (i=0; i<NUMBER_OF_CCS && !found; i++) {
    if (vnetconfig->tunnels.ccs[i] != 0) {
      rc = vnetCheckLocalIP(vnetconfig, vnetconfig->tunnels.ccs[i]);
      if (!rc) {
	logprintfl(EUCADEBUG, "vnetSetCCS(): setting localIpId: %d\n", i);
	localIpId = i;
	found=1;
      }
    }
  }
  if (localIpId >= 0) {
    vnetconfig->tunnels.localIpIdLast = vnetconfig->tunnels.localIpId;
    vnetconfig->tunnels.localIpId = localIpId;
  } else {
    logprintfl(EUCAWARN, "vnetSetCCS(): VNET_LOCALIP is not in list of CCS, tearing down tunnels\n");
    vnetTeardownTunnels(vnetconfig);
    bzero(vnetconfig->tunnels.ccs, sizeof(uint32_t) * NUMBER_OF_CCS);
    vnetconfig->tunnels.localIpIdLast = vnetconfig->tunnels.localIpId;
    vnetconfig->tunnels.localIpId = -1;
    return(0);
  }
  return(0);
#endif
}

int vnetStartInstanceNetwork(vnetConfig *vnetconfig, int vlan, char *publicIp, char *privateIp, char *macaddr) {
  char rules[4][MAX_PATH];
  char rule[MAX_PATH];
  int rc, ret=0, done, i, numrules;
  numrules = 3;
  
  return(0);
  if (!strcmp(vnetconfig->mode, "MANAGED")) {
    
  } else {

    // do ebtables to provide MAC/IP spoofing protection
    snprintf(rules[0], MAX_PATH, "FORWARD -i ! %s -p IPv4 -s %s --ip-src %s -j ACCEPT", vnetconfig->pubInterface, macaddr, privateIp);
    snprintf(rules[1], MAX_PATH, "FORWARD -i ! %s -p IPv4 -s %s --ip-src ! %s -j DROP", vnetconfig->pubInterface, macaddr, privateIp);
    snprintf(rules[2], MAX_PATH, "FORWARD -i ! %s -s %s -j ACCEPT", vnetconfig->pubInterface, macaddr);

    done=0;
    for (i=0; i<numrules && !done; i++) {
      snprintf(rule, MAX_PATH, "-A %s\n", rules[i]);
      rc = vnetApplySingleEBTableRule(vnetconfig, "filter", rule);
      if (rc) {
	logprintfl(EUCAERROR, "vnetStartInstanceNetwork(): could not apply ebtables rule '%s'\n", rule);
	done=1;
	ret=1;
      }
    }
    if (done) {
      // one of the rules failed, tear them down
      for (i=0; i<numrules; i++) {
	snprintf(rule, MAX_PATH, "-D %s\n", rules[i]);
	rc = vnetApplySingleEBTableRule(vnetconfig, "filter", rule);
      }
    }
  }
  return(ret);
}

int vnetStopInstanceNetwork(vnetConfig *vnetconfig, int vlan, char *publicIp, char *privateIp, char *macaddr) {
  char rules[3][MAX_PATH];
  char rule[MAX_PATH];
  int rc, ret=0, i, done, numrules;
  numrules = 3;

  return(0);
  if (!strcmp(vnetconfig->mode, "MANAGED")) {

  } else {
    snprintf(rules[0], MAX_PATH, "FORWARD -i ! %s -p IPv4 -s %s --ip-src %s -j ACCEPT", vnetconfig->pubInterface, macaddr, privateIp);
    snprintf(rules[1], MAX_PATH, "FORWARD -i ! %s -p IPv4 -s %s --ip-src ! %s -j DROP", vnetconfig->pubInterface, macaddr, privateIp);
    snprintf(rules[2], MAX_PATH, "FORWARD -i ! %s -s %s -j ACCEPT", vnetconfig->pubInterface, macaddr);
    done=0;
    for (i=0; i<numrules && !done; i++) {
      snprintf(rule, MAX_PATH, "-D %s\n", rules[i]);
      rc = vnetApplySingleEBTableRule(vnetconfig, "filter", rule);
    }
  }  
  return(ret);
}

int vnetStartNetworkManaged(vnetConfig *vnetconfig, int vlan, char *uuid, char *userName, char *netName, char **outbrname) {
  char cmd[MAX_PATH], newdevname[32], newbrname[32], *network=NULL;
  int rc, slashnet, i;

  // check input params...
  if (!vnetconfig || !outbrname) {
    if (!vnetconfig) {
      logprintfl(EUCAERROR, "vnetStartNetworkManaged(): bad input params\n");
      return(1);
    } else {
      return(0);
    }
  }
  
  logprintfl(EUCADEBUG, "vnetStartNetworkManaged(): params: vlan=%d, uuid=%s, userName=%s, netName=%s\n", vlan, SP(uuid), SP(userName), SP(netName));

  *outbrname = NULL;

  if (vlan < 0 || vlan > vnetconfig->max_vlan) {
    logprintfl(EUCAERROR, "vnetStartNetworkManaged(): supplied vlan '%d' is out of range (%d - %d), cannot start network\n", vlan, 0, vnetconfig->max_vlan);
    return(1);
  }

  if (vnetconfig->role == NC && vlan > 0) {
    // first, create tagged interface
    if (!strcmp(vnetconfig->mode, "MANAGED")) {
      snprintf(newdevname, 32, "%s.%d", vnetconfig->privInterface, vlan);
      rc = check_device(newdevname);
      if (rc) {
	snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap vconfig add %s %d", vnetconfig->eucahome, vnetconfig->privInterface, vlan);
	rc = system(cmd);
	if (rc != 0) {
	  // failed to create vlan tagged device
	  logprintfl(EUCAERROR, "vnetStartNetworkManaged(): cannot create new vlan device %s.%d\n", vnetconfig->privInterface, vlan);
	  return(1);
	}
      }

      // create new bridge
      snprintf(newbrname, 32, "eucabr%d", vlan);
      //      *outbrname = strdup(newbrname);
      rc = check_bridge(newbrname);
      if (rc) {
	// bridge does not yet exist
	snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap brctl addbr %s", vnetconfig->eucahome, newbrname);
	rc = system(cmd);
	if (rc) {
	  logprintfl(EUCAERROR, "vnetStartNetworkManaged(): could not create new bridge %s\n", newbrname);
	  return(1);
	}      
      }
      
      // add if to bridge
      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap brctl addif %s %s", vnetconfig->eucahome, newbrname, newdevname);
      rc = system(cmd);
      
      // bring br up
      if (check_deviceup(newbrname)) {
	snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip link set dev %s up", vnetconfig->eucahome, newbrname);
	rc = system(cmd);
      }
      
      // bring if up
      if (check_deviceup(newdevname)) {
	snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip link set dev %s up", vnetconfig->eucahome, newdevname);
	rc = system(cmd);
      }
    } else {
      snprintf(newbrname, 32, "%s", vnetconfig->bridgedev);
      if (!strcmp(vnetconfig->mode, "STATIC-DYNMAC")) {
	//ebtables rule(s) here, need mac/ip mapping and ethernet device
      }
    }
    
    *outbrname = strdup(newbrname);
  } else if (vlan > 0 && (vnetconfig->role == CC || vnetconfig->role == CLC)) {

    vnetconfig->networks[vlan].active = 1;
    for (i=0; i<=NUMBER_OF_CCS; i++) {
      vnetconfig->networks[vlan].addrs[i].active = 1;
    }
    vnetconfig->networks[vlan].addrs[vnetconfig->numaddrs-1].active = 1;
    
    rc = vnetSetVlan(vnetconfig, vlan, uuid, userName, netName);
    rc = vnetCreateChain(vnetconfig, userName, netName);
    
    // allow traffic on this net to flow freely
    slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
    network = hex2dot(vnetconfig->networks[vlan].nw);
    snprintf(cmd, 256, "-A FORWARD -s %s/%d -d %s/%d -j ACCEPT", network, slashnet, network, slashnet);
    //    if (check_tablerule(vnetconfig, "filter", cmd)) {
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
      //    }
    if (network) free(network);
    
    if (!strcmp(vnetconfig->mode, "MANAGED")) {
      snprintf(newdevname, 32, "%s.%d", vnetconfig->privInterface, vlan);
      rc = check_device(newdevname);
      if (rc) {
	snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap vconfig add %s %d", vnetconfig->eucahome, vnetconfig->privInterface, vlan);
	rc = system(cmd);
	if (rc) {
	  logprintfl(EUCAERROR, "vnetStartNetworkManaged(): could not tag %s with vlan %d\n", vnetconfig->privInterface, vlan);
	  return(1);
	}
      }

      // create new bridge
      snprintf(newbrname, 32, "eucabr%d", vlan);
      rc = check_bridge(newbrname);
      if (rc) {
        // bridge does not yet exist
        snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap brctl addbr %s", vnetconfig->eucahome, newbrname);
        rc = system(cmd);
        if (rc) {
          logprintfl(EUCAERROR, "vnetStartNetworkManaged(): could not create new bridge %s\n", newbrname);
          return(1);
        }
	// DAN temporary
	//        snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap brctl stp %s on", vnetconfig->eucahome, newbrname);
        snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap brctl stp %s off", vnetconfig->eucahome, newbrname);
        rc = system(cmd);
        if (rc) {
          logprintfl(EUCAWARN, "vnetStartNetworkManaged(): could not enable stp on bridge %s\n", newbrname);
        }

        snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap brctl setfd %s 2", vnetconfig->eucahome, newbrname);
        rc = system(cmd);
        if (rc) {
          logprintfl(EUCAWARN, "vnetStartNetworkManaged(): could not set fd time to 2 on bridge %s\n", newbrname);
        }

        snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap brctl sethello %s 2", vnetconfig->eucahome, newbrname);
        rc = system(cmd);
        if (rc) {
          logprintfl(EUCAWARN, "vnetStartNetworkManaged(): could not set hello time to 2 on bridge %s\n", newbrname);
        }
      }
      
      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap brctl addif %s %s", vnetconfig->eucahome, newbrname, newdevname);
      rc = system(cmd);
      
      // bring br up
      if (check_deviceup(newbrname)) {
	snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip link set dev %s up", vnetconfig->eucahome, newbrname);
	rc = system(cmd);
      }
      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr flush %s", vnetconfig->eucahome, newbrname);
      rc = system(cmd);
      
      // bring if up
      if (check_deviceup(newdevname)) {
	snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip link set dev %s up", vnetconfig->eucahome, newdevname);
	rc = system(cmd);
      }

      // attach tunnel(s)
      rc = vnetAttachTunnels(vnetconfig, vlan, newbrname);
      if (rc) {
	logprintfl(EUCAWARN, "vnetStartNetworkManaged(): failed to attach tunnels for vlan %d on bridge %s\n", vlan, newbrname);
      }
      
      snprintf(newdevname, 32, "%s", newbrname);
    } else {
      // attach tunnel(s)
      
      rc = vnetAttachTunnels(vnetconfig, vlan, vnetconfig->privInterface);
      if (rc) {
	logprintfl(EUCAWARN, "vnetStartNetworkManaged(): failed to attach tunnels for vlan %d on bridge %s\n", vlan, vnetconfig->privInterface);
      }
      
      snprintf(newdevname, 32, "%s", vnetconfig->privInterface);
    }

    rc = vnetAddGatewayIP(vnetconfig, vlan, newdevname, vnetconfig->tunnels.localIpId);
    if (rc) {
      logprintfl(EUCAWARN, "vnetStartNetworkManaged(): failed to add gateway IP to device %s\n", newdevname);
    }
    
    *outbrname = strdup(newdevname);
  }
  return(0);
}

int vnetAttachTunnels(vnetConfig *vnetconfig, int vlan, char *newbrname) {
  int rc, i, slashnet;
  char cmd[MAX_PATH], tundev[32], tunvlandev[32], *network=NULL;
  
  if (!vnetconfig) {
    logprintfl(EUCAERROR, "vnetAttachTunnels(): bad input params\n");
    return(1);
  }

  if (!vnetconfig->tunnels.tunneling) {
    return(0);
  }

  if (vlan < 0 || vlan > NUMBER_OF_VLANS || !newbrname || check_bridge(newbrname)) {
    logprintfl(EUCAERROR, "vnetAttachTunnels(): bad input params\n");
    return(1);
  }
  
  if (check_bridgestp(newbrname)) {
    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap brctl stp %s on", vnetconfig->eucahome, newbrname);
    rc = system(cmd);
    if (rc) {
      logprintfl(EUCAWARN, "vnetAttachTunnels(): could enable stp on bridge %s\n", newbrname);
    }
  }

  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    for (i=0; i<NUMBER_OF_CCS; i++) {
      //    logprintfl(EUCADEBUG, "attaching for CC %d vlan %d\n", i, vlan);
      if (i != vnetconfig->tunnels.localIpId) {
	snprintf(tundev, 32, "tap-%d-%d", vnetconfig->tunnels.localIpId, i);
	if (!check_device(tundev) && !check_device(newbrname)) {
	  if (!strcmp(vnetconfig->mode, "MANAGED")) {
	    snprintf(tunvlandev, 32, "tap-%d-%d.%d", vnetconfig->tunnels.localIpId, i, vlan);
	    if (check_device(tunvlandev)) {
	      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap vconfig add %s %d", vnetconfig->eucahome, tundev, vlan);
	      logprintfl(EUCADEBUG, "vnetAttachTunnels(): running cmd '%s'\n", cmd);
	      rc = system(cmd);
	      rc = rc>>8;
	    }
	  } else {
	    snprintf(tunvlandev, 32, "%s", tundev);
	  }
	  
	  if (check_bridgedev(newbrname, tunvlandev)) {
	    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap brctl addif %s %s", vnetconfig->eucahome, newbrname, tunvlandev);
	    logprintfl(EUCADEBUG, "vnetAttachTunnels(): running cmd '%s'\n", cmd);
	    rc = system(cmd);
	    rc = rc>>8;
	  }
	  
	  if (check_deviceup(tunvlandev)) {
	    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip link set up dev %s", vnetconfig->eucahome, tunvlandev);
	    logprintfl(EUCADEBUG, "vnetAttachTunnels(): running cmd '%s'\n", cmd);
	    rc = system(cmd);
	    rc = rc>>8;
	  }
	}
	
	snprintf(tundev, 32, "tap-%d-%d", i, vnetconfig->tunnels.localIpId);
	if (!check_device(tundev) && !check_device(newbrname)) {
	  if (!strcmp(vnetconfig->mode, "MANAGED")) {
	    snprintf(tunvlandev, 32, "tap-%d-%d.%d", i, vnetconfig->tunnels.localIpId, vlan);
	    if (check_device(tunvlandev)) {
	      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap vconfig add %s %d", vnetconfig->eucahome, tundev, vlan);
	      logprintfl(EUCADEBUG, "vnetAttachTunnels(): running cmd '%s'\n", cmd);
	      rc = system(cmd);
	      rc = rc>>8;
	    }
	  } else {
	    snprintf(tunvlandev, 32, "%s", tundev);
	  }
	  
	  if (check_bridgedev(newbrname, tunvlandev)) {
	    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap brctl addif %s %s", vnetconfig->eucahome, newbrname, tunvlandev);
	    logprintfl(EUCADEBUG, "vnetAttachTunnels(): running cmd '%s'\n", cmd);
	    rc = system(cmd);
	    rc = rc>>8;
	  }
	  
	  if (check_deviceup(tunvlandev)) {
	    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip link set up dev %s", vnetconfig->eucahome, tunvlandev);
	    logprintfl(EUCADEBUG, "vnetAttachTunnels(): running cmd '%s'\n", cmd);
	    rc = system(cmd);
	    rc = rc>>8;
	  }
	}
      }
    }
  } else {
    return(0);
  }
  
  return(0);
}

int vnetDetachTunnels(vnetConfig *vnetconfig, int vlan, char *newbrname) {
  int rc, i, slashnet;
  char cmd[MAX_PATH], tundev[32], tunvlandev[32], *network=NULL;
  
  slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
  network = hex2dot(vnetconfig->networks[vlan].nw);
  snprintf(cmd, MAX_PATH, "-D FORWARD -s %s/%d -d %s/%d -j ACCEPT", network, slashnet, network, slashnet);
  rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
  if (network) free(network);
  
  for (i=0; i<NUMBER_OF_CCS; i++) {
    if (i != vnetconfig->tunnels.localIpId) {
      snprintf(tundev, 32, "tap-%d-%d", vnetconfig->tunnels.localIpId, i);
      if (!check_device(tundev) && !check_device(newbrname)) {
	snprintf(tunvlandev, 32, "tap-%d-%d.%d", vnetconfig->tunnels.localIpId, i, vlan);
	if (!check_device(tunvlandev)) {
	  snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap vconfig rem %s", vnetconfig->eucahome, tunvlandev);
	  logprintfl(EUCADEBUG, "vnetDetachTunnels(): running cmd '%s'\n", cmd);
	  rc = system(cmd);
	  rc = rc>>8;
	}
      }

      snprintf(tundev, 32, "tap-%d-%d", i, vnetconfig->tunnels.localIpId);
      if (!check_device(tundev) && !check_device(newbrname)) {
	snprintf(tunvlandev, 32, "tap-%d-%d.%d", i, vnetconfig->tunnels.localIpId, vlan);
	if (!check_device(tunvlandev)) {
	  snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap vconfig rem %s", vnetconfig->eucahome, tunvlandev);
	  logprintfl(EUCADEBUG, "vnetDetachTunnels(): running cmd '%s'\n", cmd);
	  rc = system(cmd);
	  rc = rc>>8;
	}
      }
    }
  }
  
  return(0);
}

int vnetTeardownTunnels(vnetConfig *vnetconfig) {
  return(vnetTeardownTunnelsVTUN(vnetconfig));
}

int vnetTeardownTunnelsVTUN(vnetConfig *vnetconfig) {
  int i, rc;
  char file[MAX_PATH], rootwrap[MAX_PATH];
  
  snprintf(rootwrap, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", vnetconfig->eucahome);

  snprintf(file, MAX_PATH, "%s/var/run/eucalyptus/vtund-server.pid", vnetconfig->eucahome);
  rc = safekillfile(file, "vtund", 9, rootwrap);
  
  if (vnetconfig->tunnels.localIpId != -1) {
    for (i=0; i<NUMBER_OF_CCS; i++) {
      if (vnetconfig->tunnels.ccs[i] != 0) {
	snprintf(file, MAX_PATH, "%s/var/run/eucalyptus/vtund-client-%d-%d.pid", vnetconfig->eucahome, vnetconfig->tunnels.localIpId, i);
	rc = safekillfile(file, "vtund", 9, rootwrap);
      }
    }
  }

  return(0);
}

int vnetSetupTunnels(vnetConfig *vnetconfig) {
  return(vnetSetupTunnelsVTUN(vnetconfig));
}

int vnetSetupTunnelsVTUN(vnetConfig *vnetconfig) {
  int i, done, rc;
  char cmd[MAX_PATH], tundev[32], *remoteIp=NULL, pidfile[MAX_PATH], rootwrap[MAX_PATH];
  time_t startTime;

  if (!vnetconfig->tunnels.tunneling || (vnetconfig->tunnels.localIpId == -1)) {
    return(0);
  }
  snprintf(rootwrap, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", vnetconfig->eucahome);  

  snprintf(pidfile, MAX_PATH, "%s/var/run/eucalyptus/vtund-server.pid", vnetconfig->eucahome);
  snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap vtund -s -n -f %s/var/lib/eucalyptus/keys/vtunall.conf", vnetconfig->eucahome, vnetconfig->eucahome);
  rc = daemonmaintain(cmd, "vtund", pidfile, 0, rootwrap);
  if (rc) {
    logprintfl(EUCAERROR, "vnetSetupTunnelsVTUN(): cannot run tunnel server: '%s'\n", cmd);
  }

  done=0;
  for (i=0; i<NUMBER_OF_CCS && !done; i++) {
    if (vnetconfig->tunnels.ccs[i] != 0) {
      remoteIp = hex2dot(vnetconfig->tunnels.ccs[i]);
      if (vnetconfig->tunnels.localIpId != i) {
	snprintf(tundev, 32, "tap-%d-%d", vnetconfig->tunnels.localIpId, i);
	rc = check_device(tundev);
	if (rc) {
	  logprintfl(EUCADEBUG, "vnetSetupTunnelsVTUN(): maintaining tunnel for endpoint: %s\n", remoteIp);
	  snprintf(pidfile, MAX_PATH, "%s/var/run/eucalyptus/vtund-client-%d-%d.pid", vnetconfig->eucahome, vnetconfig->tunnels.localIpId, i);
	  snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap vtund -n -f %s/var/lib/eucalyptus/keys/vtunall.conf -p tun-%d-%d %s", vnetconfig->eucahome, vnetconfig->eucahome, vnetconfig->tunnels.localIpId, i, remoteIp);
	  rc = daemonmaintain(cmd, "vtund", pidfile, 0, rootwrap);
	  if (rc) {
	    logprintfl(EUCAERROR, "vnetSetupTunnelsVTUN(): cannot run tunnel client: '%s'\n", cmd);
	  } else {
	    logprintfl(EUCADEBUG, "vnetSetupTunnelsVTUN(): ran cmd '%s'\n", cmd);
	  }
	}
      }
      if (remoteIp) free(remoteIp);
    }
  }
  
  return(0);
}

int vnetAddGatewayIP(vnetConfig *vnetconfig, int vlan, char *devname, int localIpId) {
  char *newip, *broadcast;
  int rc, slashnet;
  char cmd[MAX_PATH];

  if (localIpId < 0) {
    logprintfl(EUCAWARN, "vnetAddGatewayIP(): negative localIpId supplied, defaulting to base gw\n");
    localIpId = 0;
  }

  newip = hex2dot(vnetconfig->networks[vlan].router + localIpId);
  //  newip = hex2dot(vnetconfig->networks[vlan].router);
  broadcast = hex2dot(vnetconfig->networks[vlan].bc);
  logprintfl(EUCADEBUG, "vnetAddGatewayIP(): adding gateway IP: %s\n", newip);

  //  snprintf(cmd, 1024, "%s/usr/lib/eucalyptus/euca_rootwrap ifconfig %s %s netmask %s up", vnetconfig->eucahome, devname, newip, netmask);
  slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
  snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr add %s/%d broadcast %s dev %s", vnetconfig->eucahome, newip, slashnet, broadcast, devname);

  logprintfl(EUCADEBUG, "vnetAddGatewayIP(): running cmd '%s'\n", cmd);
  rc = system(cmd);
  rc = rc>>8;
  if (rc && rc != 2) {
    logprintfl(EUCAERROR, "vnetAddGatewayIP(): could not bring up new device %s with ip %s\n", devname, newip);
    if (newip) free(newip);
    if (broadcast) free(broadcast);
    return(1);
  }
  if (newip) free(newip);
  if (broadcast) free(broadcast);

  if (check_deviceup(devname)) {
    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip link set dev %s up", vnetconfig->eucahome, devname);
    rc = system(cmd);
    rc = rc>>8;
    if (rc) {
      logprintfl(EUCAERROR, "vnetAddGatewayIP(): could not bring up interface '%s'\n", devname);
      return(1);
    }
  }
  return(0);
}


int vnetApplyArpTableRules(vnetConfig *vnetconfig) {
  int rc, fd, ret=0, i, j, k, done, slashnet;
  char *file, cmd[256];
  FILE *FH;
  
  if (!vnetconfig) {
    logprintfl(EUCAERROR, "vnetApplyArpTableRules(): bad input params (null vnetconfig)\n");
    return(1);
  }
  
  logprintfl(EUCADEBUG, "vnetApplyArpTableRules(): applying arptable rules\n");

  file = strdup("/tmp/euca-arpt-XXXXXX");
  if (!file) {
    return(1);
  }
  fd = safe_mkstemp(file);
  if (fd < 0) {
    free(file);
    return(1);
  }
  chmod(file, 0644);
  FH = fdopen(fd, "w");
  if (!FH) {
    close(fd);
    unlink(file);
    free(file);
    return(1);
  }
  
  //  fprintf(FH, "%s\n", rule);
  for (i=0; i<NUMBER_OF_VLANS; i++) {
    if (vnetconfig->networks[i].active) {
      char *net=NULL, *gw=NULL;
      net = hex2dot(vnetconfig->networks[i].nw);
      gw = hex2dot(vnetconfig->networks[i].router);

      for (j=0; j<NUMBER_OF_HOSTS_PER_VLAN; j++) {
	if (vnetconfig->networks[i].addrs[j].ip && vnetconfig->networks[i].addrs[j].active) {
	  done=0;
	  for (k=0; k<NUMBER_OF_PUBLIC_IPS && !done; k++) {
	    if (vnetconfig->publicips[k].allocated && (vnetconfig->publicips[k].dstip == vnetconfig->networks[i].addrs[j].ip)) {
	      char *ip=NULL;
	      ip = hex2dot(vnetconfig->networks[i].addrs[j].ip);
	      if (ip && gw) {
		fprintf(FH, "IP=%s,%s\n", ip,gw);
		done++;
	      }
	      if (ip) free(ip);
	    }
	  }
	}
      }
      for (k=0; k<NUMBER_OF_PUBLIC_IPS; k++) {
	if (vnetconfig->publicips[k].allocated && vnetconfig->publicips[k].dstip) {
	  char *ip=NULL;
	  ip = hex2dot(vnetconfig->publicips[k].dstip);
	  if (ip && gw) {
	    fprintf(FH, "IP=%s,%s\n", ip,gw);
	    free(ip);
	  }
	}
      }
      if (net && gw) {
	slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[i].nm)) + 1);
	fprintf(FH, "NET=%s/%d,%s\n", net, slashnet, gw);
	free(gw);
	free(net);
      }
    }
  }

  fclose(FH);
  close(fd);
  
  snprintf(cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/euca_arpt %s", vnetconfig->eucahome, vnetconfig->eucahome, file);
  //  logprintfl(EUCADEBUG, "running cmd '%s'\n", cmd);
  rc = system(cmd);
  if (rc) {
    ret = 1;
  }
  unlink(file);
  free(file);
    
  return(ret);  
}

int vnetDelGatewayIP(vnetConfig *vnetconfig, int vlan, char *devname, int localIpId) {
  char *newip, *broadcast;
  int rc;
  int slashnet;
  char cmd[MAX_PATH];

  if (localIpId < 0) {
    logprintfl(EUCAWARN, "vnetDelGatewayIP(): negative localIpId supplied, defaulting to base gw\n");
    localIpId = 0;
  }

  newip = hex2dot(vnetconfig->networks[vlan].router + localIpId);
  //  newip = hex2dot(vnetconfig->networks[vlan].router);
  broadcast = hex2dot(vnetconfig->networks[vlan].bc);
  logprintfl(EUCADEBUG, "vnetDelGatewayIP(): removing gateway IP: %s\n", newip);  
  //  snprintf(cmd, 1024, "%s/usr/lib/eucalyptus/euca_rootwrap ifconfig %s %s netmask %s up", vnetconfig->eucahome, devname, newip, netmask);
  slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
  //slashnet = 16;
  snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr del %s/%d broadcast %s dev %s", vnetconfig->eucahome, newip, slashnet, broadcast, devname);
  //  snprintf(cmd, 1024, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr del %s/%d dev %s", vnetconfig->eucahome, newip, slashnet, devname);
  rc = system(cmd);
  if (rc) {
    logprintfl(EUCAERROR, "vnetDelGatewayIP(): could not bring down new device %s with ip %s\n", devname, newip);
    if (newip) free(newip);
    if (broadcast) free(broadcast);
    return(1);
  }
  if (newip) free(newip);
  if (broadcast) free(broadcast);
  return(0);
}

int vnetStopNetworkManaged(vnetConfig *vnetconfig, int vlan, char *userName, char *netName) {
  char cmd[MAX_PATH], newdevname[32], newbrname[32], *network;
  int rc, ret, slashnet;
  
  ret = 0;
  //if (vnetconfig->role == NC) {
  if (vlan < 0 || vlan > vnetconfig->max_vlan) {
    logprintfl(EUCAWARN, "vnetStopNetworkManaged(): supplied vlan '%d' is out of range (%d - %d), nothing to do\n", vlan, 0, vnetconfig->max_vlan);
    return(0);
  }
  
  vnetconfig->networks[vlan].active = 0;
  bzero(vnetconfig->networks[vlan].addrs, sizeof(netEntry) * NUMBER_OF_HOSTS_PER_VLAN);

  if (!strcmp(vnetconfig->mode, "MANAGED")) {
    snprintf(newbrname, 32, "eucabr%d", vlan);
    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip link set dev %s down", vnetconfig->eucahome, newbrname);
    rc = system(cmd);
    if (rc) {
      logprintfl(EUCAERROR, "vnetStopNetworkManaged(): cmd '%s' failed\n", cmd);
      ret = 1;
    }    

    snprintf(newdevname, 32, "%s.%d", vnetconfig->privInterface, vlan);
    rc = check_device(newdevname);
    // DAN temporary for QA, re-enable for release
    if (!rc) {
      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip link set dev %s down", vnetconfig->eucahome, newdevname);
      rc = system(cmd);
      if (rc) {
	logprintfl(EUCAERROR, "vnetStopNetworkManaged(): cmd '%s' failed\n", cmd);
	ret=1;
      }
  
      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap vconfig rem %s", vnetconfig->eucahome, newdevname);
      rc = system(cmd);
      if (rc) {
	logprintfl(EUCAERROR, "vnetStopNetworkManaged(): cmd '%s' failed\n", cmd);
	ret = 1;
      }
    }
    snprintf(newdevname, 32, "%s", newbrname);
  } else {
    snprintf(newdevname, 32, "%s", vnetconfig->privInterface);
  }
  
  if ((vnetconfig->role == CC || vnetconfig->role == CLC)) {

    // disallow traffic on this net from flowing freely
    slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
    network = hex2dot(vnetconfig->networks[vlan].nw);
    snprintf(cmd, MAX_PATH, "-D FORWARD -s %s/%d -d %s/%d -j ACCEPT", network, slashnet, network, slashnet);
    //    if (check_tablerule(vnetconfig, "filter", cmd)) {
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    //    }
    if (network) free(network);
    
    if (!strcmp(vnetconfig->mode, "MANAGED")) {

      rc = vnetDetachTunnels(vnetconfig, vlan, newbrname);
      if (rc) {
	logprintfl(EUCAWARN, "vnetStopNetworkManaged(): failed to detach tunnels\n");
      }
      
      rc = vnetDelDev(vnetconfig, newdevname);
      if (rc) {
	logprintfl(EUCAWARN, "vnetStopNetworkManaged(): could not remove '%s' from list of interfaces\n", newdevname);
      }
    }
    rc = vnetDelGatewayIP(vnetconfig, vlan, newdevname, vnetconfig->tunnels.localIpId);
    if (rc) {
      logprintfl(EUCAWARN, "vnetStopNetworkManaged(): failed to delete gateway IP from interface %s\n", newdevname);
    }
    
    if (userName && netName) {
      rc = vnetDeleteChain(vnetconfig, userName, netName);
      if (rc) {
	logprintfl(EUCAERROR, "vnetStopNetworkManaged(): could not delete chain (%s/%s)\n", userName, netName);
	ret = 1;
      }
    }
  }
  
  return(ret);
}

int vnetStartNetwork(vnetConfig *vnetconfig, int vlan, char *uuid, char *userName, char *netName, char **outbrname) {
  int rc;

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC")) {
    if (outbrname) {
      if (vnetconfig->role == NC) {
	*outbrname = strdup(vnetconfig->bridgedev);
      } else {
	*outbrname = strdup(vnetconfig->privInterface);
      }
      if (*outbrname == NULL) {
         logprintfl(EUCAERROR, "vnetStartNetwork(): out of memory!\n");
      }
    } else {
         logprintfl(EUCADEBUG, "vnetStartNetwork(): outbrname is NULL\n");
    }
    rc = 0;
  } else {
    rc = vnetStartNetworkManaged(vnetconfig, vlan, uuid, userName, netName, outbrname);
  }
  
  if (vnetconfig->role != NC && outbrname && *outbrname) {
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
    logprintfl(EUCAERROR, "vnetGetPublicIP(): could not find ip %s in list of allocateable publicips\n", ip);
    return(1);
  }
  return(0);
}

int vnetCheckPublicIP(vnetConfig *vnetconfig, char *ip) {
  int i, rc, done;
  uint32_t theip;
  
  if (!vnetconfig || !ip) return(1);
  
  theip = dot2hex(ip);
  
  for (i=0; i<NUMBER_OF_PUBLIC_IPS; i++) {
    if (vnetconfig->publicips[i].ip == theip) {
      return(0);
    }
  }
  return(1);
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
    } else if ((ptr = strchr(ip, '-'))) {
      *ptr = '\0';
      ptr++;
      minip = dot2hex(ip);
      theip = dot2hex(ptr);
      numips = (theip - minip)+1;
      // check (ip >= 0x7F000000 && ip <= 0x7FFFFFFF) looks for ip in lo range
      if (numips <= 0 || numips > 256 || (minip >= 0x7F000000 && minip <= 0x7FFFFFFF) || (theip >= 0x7F000000 && theip <= 0x7FFFFFFF)) {
	logprintfl(EUCAERROR, "vnetAddPublicIP(): incorrect PUBLICIPS range specified: %s-%s\n", ip, ptr);
	numips = 0;
      }

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
	if (!strcmp(vnetconfig->mode, "STATIC-DYNMAC")) {
	  char *theipstr=NULL, *themacstr=NULL;
	  theipstr = hex2dot(theip);
	  //	  themacstr = ipdot2macdot(theipstr, "D0:0D");
	  if(theipstr)
	     themacstr = ipdot2macdot(theipstr, vnetconfig->macPrefix);

	  if (theipstr && themacstr) {
	    vnetRefreshHost(vnetconfig, themacstr, theipstr, 0, -1);
	  }
	  if(themacstr) free(themacstr);
	  if(theipstr) free(theipstr);
	} else {
	  vnetconfig->publicips[found].ip = theip;
	}
      } else {
	logprintfl(EUCAERROR, "vnetAddPublicIP(): cannot add any more public IPS (limit:%d)\n", NUMBER_OF_PUBLIC_IPS);
	return(1);
      }
    }
  }

  return(0);
}

int vnetAssignAddress(vnetConfig *vnetconfig, char *src, char *dst) {
  int rc=0, slashnet, ret=0;
  char cmd[MAX_PATH], *network;

  if ((vnetconfig->role == CC || vnetconfig->role == CLC) && (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN"))) {

    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr add %s/32 dev %s", vnetconfig->eucahome, src, vnetconfig->pubInterface);
    logprintfl(EUCADEBUG,"vnetAssignAddress(): running cmd %s\n", cmd);
    rc = system(cmd);
    rc = rc>>8;
    if (rc && (rc != 2)) {
      logprintfl(EUCAERROR,"vnetAssignAddress(): failed to assign IP address '%s'\n", cmd);
      ret = 1;
    }

    snprintf(cmd, MAX_PATH, "-A PREROUTING -d %s -j DNAT --to-destination %s", src, dst);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
    if (rc) {
      logprintfl(EUCAERROR,"vnetAssignAddress(): failed to apply DNAT rule '%s'\n", cmd);
      ret = 1;
    }
    snprintf(cmd, MAX_PATH, "-A OUTPUT -d %s -j DNAT --to-destination %s", src, dst);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
    if (rc) {
      logprintfl(EUCAERROR,"vnetAssignAddress(): failed to apply DNAT rule '%s'\n", cmd);
      ret = 1;
    }

    slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->nm)) + 1);
    network = hex2dot(vnetconfig->nw);
    //snprintf(cmd, 255, "-I POSTROUTING -s %s -d ! %s/%d -j SNAT --to-source %s", dst, network, slashnet, src);
    snprintf(cmd, MAX_PATH, "-I POSTROUTING -s %s -j SNAT --to-source %s", dst, src);
    if (network) free(network);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
    if (rc) {
      logprintfl(EUCAERROR,"vnetAssignAddress(): failed to apply SNAT rule '%s'\n", cmd);
      ret = 1;
    }
  }
  return(ret);
}

int vnetAllocatePublicIP(vnetConfig *vnetconfig, char *uuid, char *ip, char *dstip) {
  return(vnetSetPublicIP(vnetconfig, uuid, ip, dstip, 1));
}

int vnetDeallocatePublicIP(vnetConfig *vnetconfig, char *uuid, char *ip, char *dstip) {
  return(vnetSetPublicIP(vnetconfig, uuid, ip, NULL, 0));
}

int vnetSetPublicIP(vnetConfig *vnetconfig, char *uuid, char *ip, char *dstip, int setval) {
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
      if (uuid) {
	if (setval) {
	  snprintf(vnetconfig->publicips[i].uuid, 48, "%s", uuid);
	} else {
	  bzero(vnetconfig->publicips[i].uuid, sizeof(char) * 48);
	}
      } else {
	bzero(vnetconfig->publicips[i].uuid, sizeof(char) * 48);
      }
      done++;
    }
  }
  return(0);

}

int vnetReassignAddress(vnetConfig *vnetconfig, char *uuid, char *src, char *dst) {
  int done, i, isallocated, pubidx, rc;
  char *currdst=NULL, cmd[MAX_PATH];
  
  // assign address if unassigned, unassign/reassign if assigned
  if (!uuid || !src) {
    logprintfl(EUCAERROR, "vnetReassignAddress(): bad input params uuid=%s, src=%s, dst=%s\n", SP(uuid), SP(src), SP(dst));
    return(1);
  }
  
  //  if (vnetCheckPublicIP(vnetconfig, src)) {
  //    return(0);
  //  }
  
  // get the publicIP of interest
  isallocated = 0;
  pubidx = 0;
  currdst = NULL;
  done=0;
  for (i=1; i<NUMBER_OF_PUBLIC_IPS && !done; i++) {
    if (vnetconfig->publicips[i].ip == dot2hex(src)) {
      currdst = hex2dot(vnetconfig->publicips[i].dstip);
      isallocated = vnetconfig->publicips[i].allocated;
      pubidx = i;
      done++;
    }
  }
  
  if (!done) {
    logprintfl(EUCAERROR, "vnetReassignAddress(): could not find ip %s in list of allocateable publicips\n", src);
    return(1);
  }

  logprintfl(EUCADEBUG, "vnetReassignAddress(): deciding what to do: src=%s dst=%s allocated=%d currdst=%s\n", SP(src), SP(dst), isallocated, SP(currdst));
  // determine if reassign must happen
  if ( isallocated && strcmp(currdst, dst) ) {
    rc = vnetUnassignAddress(vnetconfig, src, currdst);
    if (rc) {
       if (currdst) free(currdst);
       return(1);
    }
  }

  // not used anymore
  if (currdst) free(currdst);

  // do the (re)assign
  if (!dst || !strcmp(dst, "0.0.0.0")) {
    vnetconfig->publicips[pubidx].dstip = 0;
    vnetconfig->publicips[pubidx].allocated = 0;
  } else {
    rc = vnetAssignAddress(vnetconfig, src, dst);
    if (rc) {
      return(1);
    }
    vnetconfig->publicips[pubidx].dstip = dot2hex(dst);
    vnetconfig->publicips[pubidx].allocated = 1;
  }
  snprintf(vnetconfig->publicips[pubidx].uuid, 48, "%s", uuid);
  logprintfl(EUCADEBUG, "vnetReassignAddress(): successfully set src=%s to dst=%s with uuid=%s, allocated=%d\n", SP(src), SP(dst), SP(uuid), vnetconfig->publicips[pubidx].allocated);

  return(0);
}

int vnetUnassignAddress(vnetConfig *vnetconfig, char *src, char *dst) {
  int rc=0, count, slashnet, ret=0;
  char cmd[MAX_PATH], *network;
  
  if ((vnetconfig->role == CC || vnetconfig->role == CLC) && (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN"))) {

    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr del %s/32 dev %s", vnetconfig->eucahome, src, vnetconfig->pubInterface);
    logprintfl(EUCADEBUG,"vnetUnassignAddress(): running cmd %s\n", cmd);
    rc = system(cmd);
    rc = rc>>8;
    if (rc && (rc != 2)) {
      logprintfl(EUCAERROR,"vnetUnassignAddress(): failed to assign IP address '%s'\n", cmd);
      ret = 1;
    }
    
    snprintf(cmd, MAX_PATH, "-D PREROUTING -d %s -j DNAT --to-destination %s", src, dst);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
    count=0;
    while(rc != 0 && count < 10) {
      rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
      count++;
    }
    if (rc) {
      logprintfl(EUCAERROR,"vnetUnassignAddress(): failed to remove DNAT rule '%s'\n", cmd);
      ret = 1;      
    }

    snprintf(cmd, MAX_PATH, "-D OUTPUT -d %s -j DNAT --to-destination %s", src, dst);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
    count=0;
    while(rc != 0 && count < 10) {
      rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
      count++;
    }
    if (rc) {
      logprintfl(EUCAERROR,"vnetUnassignAddress(): failed to remove DNAT rule '%s'\n", cmd);
      ret = 1;      
    }

    slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->nm)) + 1);
    network = hex2dot(vnetconfig->nw);
    //    snprintf(cmd, 255, "-D POSTROUTING -s %s -d ! %s/%d -j SNAT --to-source %s", dst, network, slashnet, src);
    snprintf(cmd, MAX_PATH, "-D POSTROUTING -s %s -j SNAT --to-source %s", dst, src);
    if (network) free(network);
    rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
    count=0;
    while(rc != 0 && count < 10) {
      rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
      count++;
    }
    if (rc) {
      logprintfl(EUCAERROR,"vnetUnassignAddress(): failed to remove SNAT rule '%s'\n", cmd);
      ret = 1;
    }

  }
  return(ret);
}

int vnetStopNetwork(vnetConfig *vnetconfig, int vlan, char *userName, char *netName) {
  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC")) {
    return(0);
  }
  return(vnetStopNetworkManaged(vnetconfig, vlan, userName, netName));
}

int instId2mac(vnetConfig *vnetconfig, char *instId, char *outmac) {
  char *p, dst[24];
  int i;

  if (!vnetconfig || !instId || !outmac) {
    return(1);
  }
  dst[0] = '\0';
  
  p = strstr(instId, "i-");
  if (p == NULL) {
    logprintfl(EUCAWARN, "instId2mac(): invalid instId=%s\n", SP(instId));
    return(1);
  }
  p += 2;
  if (strlen(p) == 8) {
    //    strncat(dst, "D0:0D", 5);
    strncat(dst, vnetconfig->macPrefix, 5);
    for (i=0; i<4; i++) {
      strncat(dst, ":", 1);
      strncat(dst, p, 2);
      p+=2;
    }
  } else {
    logprintfl(EUCAWARN, "instId2mac(): invalid instId=%s\n", SP(instId));
    return(1);
  }
  
  snprintf(outmac, 24, "%s", dst);
  return(0);
}

int ip2mac(vnetConfig *vnetconfig, char *ip, char **mac) {
  char rc, i, j;
  char cmd[MAX_PATH], rbuf[256], *tok, ipspace[25];
  FILE *FH=NULL;

  if (mac == NULL || ip == NULL) {
    return(1);
  }
  *mac = NULL;
  
  FH=fopen("/proc/net/arp", "r");
  if (!FH) {
    return(1);
  }
  
  snprintf(ipspace, 25, "%s ", ip);
  while(fgets(rbuf, 256, FH) != NULL) {
    //    logprintfl(EUCADEBUG, "'%s' '%s' '%s'\n", rbuf, ip, strstr(rbuf, ip));
    if (strstr(rbuf, ipspace)) {
      int count=0;
      tok = strtok(rbuf, " ");
      while(tok && count < 4) {
	//	logprintfl(EUCADEBUG, "COUNT: %d TOK: %s\n", count, tok);
	count++;
	if (count < 4) {
	  tok = strtok(NULL, " ");
	}
      }
      if (tok != NULL) {
        *mac = strdup(tok);
        fclose(FH);
        return(0);
      }
    }
  }
  fclose(FH);
  
  return(1);
}

int mac2ip(vnetConfig *vnetconfig, char *mac, char **ip) {
  int rc, i, j;
  char cmd[MAX_PATH], rbuf[256], *tok, lowbuf[256], lowmac[256];
  
  FILE *FH=NULL;
  
  if (mac == NULL || ip == NULL) {
    return(1);
  }
  
  *ip = NULL;

  if (!strcmp(vnetconfig->mode, "SYSTEM")) {
    // try to fill up the arp cache
    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/populate_arp.pl", vnetconfig->eucahome, vnetconfig->eucahome);
    rc = system(cmd);
    if (rc) {
      logprintfl(EUCAWARN, "mac2ip(): could not execute arp cache populator script, check httpd log for errors\n");
    }
  }
  
  FH=fopen("/proc/net/arp", "r");
  if (!FH) {
    return(1);
  }
  
  bzero(lowmac, 256);
  for (i=0; i<strlen(mac); i++) {
    lowmac[i] = tolower(mac[i]);
  }
  
  while(fgets(rbuf, 256, FH) != NULL) {
    bzero(lowbuf, 256);
    for (i=0; i<strlen(rbuf); i++) {
      lowbuf[i] = tolower(rbuf[i]);
    }
    
    if (strstr(lowbuf, lowmac)) {
      tok = strtok(lowbuf, " ");
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
  int a=0, b=0, c=0, d=0, rc;

  rc = sscanf(in, "%d.%d.%d.%d", &a, &b, &c, &d);
  if (rc != 4 || (a<0||a>255) || (b<0||b>255) || (c<0||c>255) || (d<0||d>255)) {
    a=127;
    b=0;
    c=0;
    d=1;
  }
  a = a<<24;
  b = b<<16;
  c = c<<8;
  
  return(a|b|c|d);
}

int getdevinfo(char *dev, uint32_t **outips, uint32_t **outnms, int *len) {
  struct ifaddrs *ifaddr, *ifa;
  char host[NI_MAXHOST];
  int rc, count;
  
  rc = getifaddrs(&ifaddr);
  if (rc) {
    return(1);
  }
  *outips = *outnms = NULL;
  *len = 0;
  
  count=0;
  for (ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next) {
    if (!strcmp(dev, "all") || !strcmp(ifa->ifa_name, dev)) {
      if (ifa->ifa_addr && ifa->ifa_addr->sa_family == AF_INET) {
	rc = getnameinfo(ifa->ifa_addr, sizeof(struct sockaddr_in), host, NI_MAXHOST, NULL, 0, NI_NUMERICHOST);
	if (!rc) {
	  void *tmpAddrPtr;
	  char buf[32];
	  char *dot;
	  uint32_t ip, nm;
	  
	  count++;
	  *outips = realloc(*outips, sizeof(uint32_t) * count);
	  *outnms = realloc(*outnms, sizeof(uint32_t) * count);
	  
	  (*outips)[count-1] = dot2hex(host);
	  
	  tmpAddrPtr=&((struct sockaddr_in *)ifa->ifa_netmask)->sin_addr;
	  if (inet_ntop(AF_INET, tmpAddrPtr, buf, 32)) {
	    (*outnms)[count-1] = dot2hex(buf);
	    //(0xFFFFFFFF - dot2hex(buf)) | (dot2hex(host) & dot2hex(buf));
	    //	  nm = dot2hex(buf);
	    //	  ip = dot2hex(host);
	    //	  dot = hex2dot( (0xFFFFFFFF - nm) | (ip & nm));
	    //	  printf("\tnetmask: <%s> <%s>\n", buf, dot);
	    //	  if (dot) free(dot);
	  }
	}
      }
    }
  }
  freeifaddrs(ifaddr);
  *len = count;
  return(0);
}

void hex2mac(unsigned char in[6], char **out) {
  if (out == NULL) {
    return;
  }
  *out = malloc(sizeof(char) * 24);
  if (*out == NULL) {
    return;
  }
  snprintf(*out, 24, "%02X:%02X:%02X:%02X:%02X:%02X", in[0], in[1], in[2], in[3], in[4], in[5]);
  return;
}

void mac2hex(char *in, unsigned char out[6]) {
  unsigned int tmp[6];
  if (in == NULL) {
    return;
  }
  sscanf(in, "%X:%X:%X:%X:%X:%X", (unsigned int *)&tmp[0], (unsigned int *)&tmp[1], (unsigned int *)&tmp[2], (unsigned int *)&tmp[3], (unsigned int *)&tmp[4], (unsigned int *)&tmp[5]);
  out[0] = (unsigned char)tmp[0];
  out[1] = (unsigned char)tmp[1];
  out[2] = (unsigned char)tmp[2];
  out[3] = (unsigned char)tmp[3];
  out[4] = (unsigned char)tmp[4];
  out[5] = (unsigned char)tmp[5];
  return;
}

int maczero(unsigned char in[6]) {
  unsigned char zeromac[6];
  bzero(zeromac, sizeof(unsigned char)*6);
  return(memcmp(in, zeromac, sizeof(unsigned char)*6));
}

int machexcmp(char *ina, unsigned char inb[6]) {
  unsigned char mconv[6];
  mac2hex(ina, mconv);
  return(memcmp(mconv, inb, sizeof(unsigned char) * 6));
}

char *hex2dot(uint32_t in) {
  char out[16];
  bzero(out, 16);
  
  snprintf(out, 16, "%u.%u.%u.%u", (in & 0xFF000000)>>24, (in & 0x00FF0000)>>16, (in & 0x0000FF00)>>8, in & 0x000000FF);
  
  return(strdup(out));
}

char *ipdot2macdot(char *ip, char *macprefix) {
  int a=0, b=0, c=0, d=0, rc;
  char *ret=NULL;
  
  rc = sscanf(ip, "%d.%d.%d.%d", &a, &b, &c, &d);
  if (rc != 4 || (a<0||a>255) || (b<0||b>255) || (c<0||c>255) || (d<0||d>255)) {
    a=127;
    b=0;
    c=0;
    d=1;
  }
  ret = malloc(sizeof(char) * 24);
  bzero(ret, sizeof(char) * 24);
  if (macprefix) {
    snprintf(ret, 24, "%s:%02X:%02X:%02X:%02X", macprefix, a, b, c, d);
  } else {
    snprintf(ret, 24, "%s:%02X:%02X:%02X:%02X", "D0:0D", a, b, c, d);
  }
  return(ret);
}

int vnetLoadIPTables(vnetConfig *vnetconfig) {
  char cmd[MAX_PATH], file[MAX_PATH];
  struct stat statbuf;
  int rc=0, ret;

  ret = 0;
  snprintf(file, MAX_PATH, "%s/iptables-preload", vnetconfig->path);
  if (stat(file, &statbuf) == 0) {
    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap iptables-restore < %s", vnetconfig->eucahome, file);
    rc = system(cmd);
    ret = WEXITSTATUS(rc);
  }
  return(ret);
}

int check_chain(vnetConfig *vnetconfig, char *userName, char *netName) {
  char cmd[MAX_PATH];
  int rc;
  char *hashChain=NULL, userNetString[MAX_PATH];

  snprintf(userNetString, MAX_PATH, "%s%s", userName, netName);
  rc = hash_b64enc_string(userNetString, &hashChain);
  if (rc) {
    logprintfl(EUCAERROR, "check_chain(): cannot hash user/net string (userNetString=%s)\n", userNetString);
    return(1);
  }

  snprintf(cmd, MAX_PATH, "-L %s -n", hashChain);
  rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);

  free(hashChain);
  return(rc);
}


int check_deviceup(char *dev) {
  int rc, ret;
  char rbuf[MAX_PATH];
  FILE *FH=NULL;

  if (check_device(dev)) {
    return(1);
  }
  
  snprintf(rbuf, MAX_PATH, "/sys/class/net/%s/operstate", dev);
  FH = fopen(rbuf, "r");
  if (!FH) {
    return(1);
  }
  
  ret=1;
  bzero(rbuf, MAX_PATH);
  if (fgets(rbuf, MAX_PATH, FH)) {
    char *p;
    p = strchr(rbuf, '\n');
    if (p) *p='\0';

    if (strncmp(rbuf, "down", MAX_PATH)) {
      ret = 0;
    }
  }

  fclose(FH);
  
  return(ret);
  
}
int check_device(char *dev) {
  char file[MAX_PATH];
  
  if (!dev) {
    return(1);
  }
  
  snprintf(file, MAX_PATH, "/sys/class/net/%s/", dev);
  if (check_directory(file)) {
    return(1);
  }
  return(0);
  /*
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
  */
}
int check_bridgestp(char *br) {
  char file[MAX_PATH];
  char *buf;
  int ret;

  if (!br || check_bridge(br)) {
    return(1);
  }
  
  ret=1;
  snprintf(file, MAX_PATH, "/sys/class/net/%s/bridge/stp_state", br);
  buf = file2str(file);
  if (buf) {
    if (atoi(buf) != 0) {
      ret=0;
    }
    free(buf);
  }
  return(ret);
}

int check_bridgedev(char *br, char *dev) {
  char file[MAX_PATH];
  
  if (!br || !dev || check_device(br) || check_device(dev)) {
    return(1);
  }

  snprintf(file, MAX_PATH, "/sys/class/net/%s/brif/%s/", br, dev);
  if (check_directory(file)) {
    return(1);
  }
  return(0);
}

int check_bridge(char *brname) {
  char file[MAX_PATH];
  
  if (!brname || check_device(brname)) {
    return(1);
  }
  snprintf(file, MAX_PATH, "/sys/class/net/%s/bridge/", brname);
  if (check_directory(file)) {
    return(1);
  }
  return(0);
}

int check_tablerule(vnetConfig *vnetconfig, char *table, char *rule) {
  int rc;
  char *out, *ptr, cmd[MAX_PATH];
  
  if (!table || !rule) {
    return(1);
  }

  snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap iptables -S -t %s", vnetconfig->eucahome, table);
  out = system_output(cmd);
  
  if (!out) {
    return(1);
  }
  
  ptr = strstr(out, rule);
  if (out) free(out);
  if (!ptr) {
    return(1);
  }
  return(0);
}

int check_isip(char *ip) {
  int a, b, c, d;
  int rc;
  
  rc = sscanf(ip, "%d.%d.%d.%d", &a, &b, &c, &d);
  if (rc != 4) {
    return(1);
  }
  return(0);
}

char *host2ip(char *host) {
  struct addrinfo hints, *result=NULL;
  int rc;
  char hostbuf[256], *ret=NULL;
  
  if (!host) return(NULL);
  
  ret = NULL;
  
  if (!strcmp(host, "localhost")) {
    ret = strdup("127.0.0.1");
    return(ret);
  }
  
  bzero(&hints, sizeof(struct addrinfo));
  rc = getaddrinfo(host, NULL, &hints, &result);
  if (!rc) {
    rc = getnameinfo(result->ai_addr, result->ai_addrlen, hostbuf, 256, NULL, 0, NI_NUMERICHOST);
    if (!rc && !check_isip(hostbuf)) {
      ret = strdup(hostbuf);
    }
  }
  if (result) freeaddrinfo(result);

  if (ret) {
    //    logprintfl(EUCADEBUG, "converted %s->%s\n", host, ret);
  } else {
    ret = strdup(host);
  }
  return(ret);
}
