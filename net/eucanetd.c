#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

#include <eucalyptus.h>
#include <vnetwork.h>
#include <log.h>
#include <hash.h>
#include <math.h>
#include <http.h>

#include "eucanetd.h"

// this will come from dynamic configuration, stubbed for now
char *eucahome = "/opt/eucalyptus";
char *netPath = "/opt/eucalyptus/var/run/eucalyptus/net";
//char *pubInterface = "br0";
char *dhcpdaemon = "/usr/sbin/dhcpd41";
char *dhcpuser = "root";
char *macPrefix = "d0:0d";
char *pubSubnet = "1.1.0.0";
char *pubSubnetMask = "255.255.0.0";
char *pubBroadcastAddress = "1.1.255.255";
char *pubRouter = "1.1.0.1";
char *pubDNS = "192.168.7.1";
//char *privInterface = "em1";
char *numaddrs = NULL;
char *pubDomainname = NULL;
char *localIp = NULL;

u32 private_ips[NUMBER_OF_PRIVATE_IPS];
u32 public_ips[NUMBER_OF_PUBLIC_IPS];
int max_ips=0;

char *last_pubprivmap_hash=NULL, *last_network_topology_hash=NULL;
char *curr_pubprivmap_hash=NULL, *curr_network_topology_hash=NULL;

int main (int argc, char **argv) {
  vnetConfig *vnetconfig = NULL;
  int rc=0;


  // HERE is where eucalyptus configuration is read (interfaces, etc)
  char *pubInterface = argv[1];
  char *privInterface = argv[2];
  char *ccIp = argv[3];
  
  // HERE is where stable but site specific  network information is first fetched and read
  // stubbed out for now
  
  init_log();
  
  // initialize vnetconfig
  vnetconfig = malloc(sizeof(vnetConfig));
  bzero(vnetconfig, sizeof(vnetConfig));
  
  int ret = vnetInit(vnetconfig, "EDGE", eucahome, netPath, CLC, pubInterface, privInterface, numaddrs, pubSubnet, pubSubnetMask,
		     pubBroadcastAddress, pubDNS, pubDomainname, pubRouter, dhcpdaemon,
		     dhcpuser, NULL, localIp, macPrefix);
  
  last_network_topology_hash = strdup("UNSET");
  curr_network_topology_hash = strdup("UNSET");
  last_pubprivmap_hash = strdup("UNSET");
  curr_pubprivmap_hash = strdup("UNSET");

  // create the euca-edge-nat chain
  rc = create_euca_edge_chains();
  if (rc) {
    LOGERROR("could not create euca chains\n");
  }

  // enter main loop
  while(1) {
    int update = 0, i;
    
    update = 0;

    // find out who the current CC is
    
    // fetch and read run-time VM network information
    rc = fetchread_latest_network(vnetconfig, ccIp);
    if (rc) {
      LOGERROR("fetchread_latest_network failed\n");
    } else {
      // decide if any updates are required (possibly make fine grained)
      if (strcmp(last_network_topology_hash, curr_network_topology_hash) || strcmp(last_pubprivmap_hash, curr_pubprivmap_hash)) {
	if (last_network_topology_hash) EUCA_FREE(last_network_topology_hash);
	if (last_pubprivmap_hash) EUCA_FREE(last_pubprivmap_hash);
	last_network_topology_hash = strdup(curr_network_topology_hash);
	last_pubprivmap_hash = strdup(curr_pubprivmap_hash);
	update = 1;
      }
      //printf("FILE HASHES: l:%s c:%s l:%s c:%s\n", last_network_topology_hash, curr_network_topology_hash, last_pubprivmap_hash, curr_pubprivmap_hash);
    }
    
    if (update) {
      char mac[32], pubip[32], privip[32];
      char cmd[MAX_PATH];
      int slashnet;
      
      LOGINFO("new networking state: updating system\n");
      // populate vnetconfig with new info
      for (i=0; i<max_ips; i++) {
	LOGINFO("adding ip: %s\n", hex2dot(private_ips[i]));
	rc = vnetAddPrivateIP(vnetconfig, hex2dot(private_ips[i]));
	if (rc) {
	  LOGERROR("could not add private IP '%s'\n", hex2dot(private_ips[i]));
	} else {
	  rc = vnetGenerateNetworkParams(vnetconfig, "", 0, -1, mac, hex2dot(public_ips[i]), hex2dot(private_ips[i]));
	  if (rc) {
	    LOGERROR("could not enable host '%s'\n", hex2dot(private_ips[i]));
	  }
	}
      }
      
      // generate DHCP config, monitor/start DHCP service
      rc = vnetKickDHCP(vnetconfig);
      if (rc) {
	LOGERROR("failed to kick dhcpd\n");
      }

     
      // install EL IP addrs and NAT rules
      // add addr/32 to pub interface
      rc = flush_euca_edge_chains();
      if (rc) {
	LOGERROR("failed to flush table euca-edge-nat\n");
      }
      
      slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - vnetconfig->networks[0].nm) + 1))));
      for (i=0; i<max_ips; i++) {
	if (public_ips[i] != private_ips[i]) {
	  snprintf(cmd, MAX_PATH, "ip addr add %s/%d dev %s\n", hex2dot(public_ips[i]), slashnet, pubInterface);
	  rc = system(cmd);
	  rc = rc>>8;
	  if (rc && (rc != 2)) {
	    LOGERROR("add cmd failed '%s'\n", cmd);
	  } else {
	    // install DNAT and SNAT rules for pub->priv mappings
	    rc = install_euca_edge_natrules(hex2dot(public_ips[i]), hex2dot(private_ips[i]));
	    if (rc) {
	      LOGERROR("failed to install natrules for host '%s'\n", hex2dot(private_ips[i]));
	    }
	  }
	}
      }

      
      // install ebtables rules for isolation
      // install iptables FW rules
      // using IPsets for sec. group 

    }
    
    // do it all again...
    sleep (1);
  }
  
  exit(0);
}

int install_euca_edge_natrules(char *pubip, char *privip) {
  int rc, ret=0;
  char cmd[MAX_PATH];

  snprintf(cmd, MAX_PATH, "iptables -t nat -I euca-edge-nat-pre -d %s/32 -j DNAT --to-destination %s", pubip, privip); 
  rc = system(cmd); 
  if (rc) {
    LOGERROR("failed cmd '%s'\n", cmd);
    ret=1;
  }

  snprintf(cmd, MAX_PATH, "iptables -t nat -I euca-edge-nat-out -d %s/32 -j DNAT --to-destination %s", pubip, privip); 
  rc = system(cmd);
  if (rc) {
    LOGERROR("failed cmd '%s'\n", cmd);
    ret=1;
  }
  
  snprintf(cmd, MAX_PATH, "iptables -t nat -I euca-edge-nat-post -d %s/32 -j SNAT --to-source %s", privip, pubip); 
  rc = system(cmd);
  if (rc) {
    LOGERROR("failed cmd '%s'\n", cmd);
    ret=1;
  }

  return(ret);
}

int flush_euca_edge_chains() {
  int rc, ret=0, i;

  char cmds[][MAX_PATH] = {"iptables -t nat -F euca-edge-nat-pre", 
			   "iptables -t nat -F euca-edge-nat-post", 
			   "iptables -t nat -F euca-edge-nat-out", 
			   "last"};
  
  i=0;
  while(strcmp(cmds[i], "last")) {
    ret = 0;
    LOGDEBUG("running command '%s'\n", cmds[i]);
    rc = system(cmds[i]);
    rc=rc>>8;
    if (rc > 1) {
      LOGERROR("cmd failed '%s' rc=%d\n", cmds[i], rc);
      ret=1;
    }
    i++;
  }
  
  return(ret);
}

int create_euca_edge_chains() {
  int rc, ret=0, i;

  char cmds[][MAX_PATH] = {"iptables -t nat -F",
			   "iptables -t nat -N euca-edge-nat-pre", 
			   "iptables -t nat -N euca-edge-nat-post", 
			   "iptables -t nat -N euca-edge-nat-out", 
			   "iptables -t nat -I PREROUTING 1 -j euca-edge-nat-pre",
			   "iptables -t nat -I POSTROUTING 1 -j euca-edge-nat-post",
			   "iptables -t nat -I OUTPUT 1 -j euca-edge-nat-out",
			   "last"};

  i=0;
  while(strcmp(cmds[i], "last")) {
    ret = 0;
    LOGDEBUG("running command '%s'\n", cmds[i]);
    rc = system(cmds[i]);
    rc=rc>>8;
    if (rc > 1) {
      LOGERROR("cmd failed '%s' rc=%d\n", cmds[i], rc);
      ret=1;
    }
    i++;
  }
  
  return(ret);
}

int init_log() {
  int ret=0;
  
  //  log_file_set("/opt/eucalyptus/var/log/eucalyptus/eucanetd.log");
  log_params_set(EUCA_LOG_DEBUG, 10, 32768);
  /*
    log_params_set(config->log_level, (int)config->log_roll_number, config->log_max_size_bytes);
    log_prefix_set(config->log_prefix);
    log_facility_set(config->log_facility, "cc");
  */
  return(ret);
}

int fetchread_latest_network(vnetConfig *vnetconfig, char *ccIp) {
  char url[MAX_PATH], network_topology_file[MAX_PATH], pubprivmap_file[MAX_PATH];
  int rc=0,ret=0, fd=0;

  snprintf(network_topology_file, MAX_PATH, "/tmp/euca-network-topology-XXXXXX");
  snprintf(pubprivmap_file, MAX_PATH, "/tmp/euca-pubprivmap-XXXXXX");
  
  fd = safe_mkstemp(network_topology_file);
  if (fd < 0) {
    LOGERROR("cannot open network_topology_file '%s'\n", network_topology_file);
    return (1);
  }
  chmod(network_topology_file, 0644);
  close(fd);

  fd = safe_mkstemp(pubprivmap_file);
  if (fd < 0) {
    LOGERROR("cannot open pubprivmap_file '%s'\n", pubprivmap_file);
    return (1);
  }
  chmod(pubprivmap_file, 0644);
  close(fd);

  snprintf(url, MAX_PATH, "http://%s:8776/network-topology", ccIp);
  rc = http_get_timeout(url, network_topology_file, 0, 0, 10, 15);
  if (rc) {
    LOGWARN("cannot get latest network topology from CC\n");
    unlink(network_topology_file);
    return (1);
  }
  if (curr_network_topology_hash) EUCA_FREE(curr_network_topology_hash);
  curr_network_topology_hash=file2md5str(network_topology_file);
  if (!curr_network_topology_hash) curr_network_topology_hash = strdup("UNSET");

  snprintf(url, MAX_PATH, "http://%s:8776/pubprivipmap", ccIp);
  rc = http_get_timeout(url, pubprivmap_file, 0, 0, 10, 15);
  if (rc) {
    LOGWARN("cannot get latest pubprivmap from CC\n");
    unlink(network_topology_file);
    unlink(pubprivmap_file);
    return (1);
  }
  if (curr_pubprivmap_hash) EUCA_FREE(curr_pubprivmap_hash);
  curr_pubprivmap_hash=file2md5str(pubprivmap_file);
  if (!curr_pubprivmap_hash) curr_pubprivmap_hash=strdup("UNSET");

  // now read the data
  {
    char buf[1024];
    int count=0;

    FILE *FH = fopen(pubprivmap_file, "r");
    while (fgets(buf, 1024, FH)) {
      char priv[64], pub[64];
      priv[0] = pub[0] = '\0';
      sscanf(buf, "%[0-9.]=%[0-9.]", pub, priv);
      //      printf ("YHELLO: buf=%s, priv=%s, pub=%s\n", buf, priv, pub);
      if (strlen(priv) && strlen(pub)) {
	private_ips[count] = dot2hex(priv);
	public_ips[count] = dot2hex(pub);
	count++;      
	max_ips = count;
      }
    }
    fclose(FH);

  }

  unlink(network_topology_file);
  unlink(pubprivmap_file);
  return(0);
}
