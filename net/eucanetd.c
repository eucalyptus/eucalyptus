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
#include <config.h>

#include "eucanetd.h"
#include "config-eucanetd.h"

u32 private_ips[NUMBER_OF_PRIVATE_IPS];
u32 public_ips[NUMBER_OF_PUBLIC_IPS];
int max_ips=0;

char *last_pubprivmap_hash=NULL, *last_network_topology_hash=NULL;
char *curr_pubprivmap_hash=NULL, *curr_network_topology_hash=NULL;

vnetConfig *vnetconfig = NULL;
int cc_polling_frequency=1;
char *clcIp=NULL;

sec_group *security_groups=NULL;
int max_security_groups=0;

int main (int argc, char **argv) {
  int rc=0;
  char *ccIp=NULL;

  // initialize the logfile
  init_log();

  // parse commandline arguments
  if (argv[1]) {
    ccIp = strdup(argv[1]);
  }

  if (!ccIp) {
    LOGERROR("must supply ccIp on the CLI\n");
    exit(1);
  }

  // initialize some globals
  last_network_topology_hash = strdup("UNSET");
  curr_network_topology_hash = strdup("UNSET");
  last_pubprivmap_hash = strdup("UNSET");
  curr_pubprivmap_hash = strdup("UNSET");
  
  // initialize vnetconfig from local eucalyptus.conf and remote (CC) dynamic config; spin looking for config from CC until one is available
  vnetconfig = malloc(sizeof(vnetConfig));
  bzero(vnetconfig, sizeof(vnetConfig));  
  rc = 1;
  while(rc) {
    rc = get_config_cc(ccIp);
    if (rc) {
      LOGWARN("cannot fetch latest initial config from CC (%s), waiting for config to become available\n", ccIp);
      sleep(1);
    }
  }
  
  // initialize the nat chains
  rc = create_euca_edge_chains();
  if (rc) {
    LOGERROR("could not create euca chains\n");
  }

  // enter main loop
  while(1) {
    int update = 0, i;

    // find out who the current CC is
    // TODO: NC needs to drop current CC (for HA)
    
    // fetch and read run-time VM network information
    update = 0;
    rc = fetchread_latest_network(ccIp);
    if (rc) {
      LOGWARN("fetchread_latest_network from CC failed, skipping update\n");
    } else {
      // decide if any updates are required (possibly make fine grained)
      update = check_for_network_update();
    }
    
    // if an update is required, implement changes
    if (update) {
      LOGINFO("new networking state: updating system\n");

      // update list of private IPs, handle DHCP daemon re-configure and restart
      rc = update_private_ips();
      if (rc) {
	LOGERROR("could not complete update of private IPs\n");
      }
      
      // update public IP assignment and NAT table entries
      rc = update_public_ips();
      if (rc) {
	LOGERROR("could not complete update of public IPs\n");
      }
      
      // update metadata redirect rule
      
      // install ebtables rules for isolation
      
      // install iptables FW rules, using IPsets for sec. group 
      rc = update_sec_groups();
      if (rc) {
	LOGERROR("could not complete update of security groups\n");
      }
      
    }
    
    // do it all over again...
    sleep (cc_polling_frequency);
  }
  
  exit(0);
}

int update_sec_groups() {
  int ret=0, i, rc, j, fd;
  char ips_file[MAX_PATH];
  FILE *FH=NULL;


  // make ipsets
  snprintf(ips_file, MAX_PATH, "/tmp/ips_file-XXXXXX");
  fd = safe_mkstemp(ips_file);
  if (fd < 0) {
    LOGERROR("cannot open ips_file '%s'\n", ips_file);
    return (1);
  }
  chmod(ips_file, 0644);
  close(fd);
  

  rc = vrun("iptables -N euca-ipsets-fwd"); rc=rc>>8;
  rc = vrun("iptables -N euca-ipsets-in"); rc=rc>>8;
  rc = vrun("iptables -N euca-ipsets-out"); rc=rc>>8;
  rc = vrun("iptables -D INPUT -j euca-ipsets-in"); rc=rc>>8;
  rc = vrun("iptables -A INPUT -j euca-ipsets-in"); rc=rc>>8;
  rc = vrun("iptables -D FORWARD -j euca-ipsets-fwd"); rc=rc>>8;
  rc = vrun("iptables -A FORWARD -j euca-ipsets-fwd"); rc=rc>>8;
  rc = vrun("iptables -D OUTPUT -j euca-ipsets-out"); rc=rc>>8;
  rc = vrun("iptables -A OUTPUT -j euca-ipsets-out"); rc=rc>>8;
  rc = vrun("iptables -F euca-ipsets-in"); rc=rc>>8;
  rc = vrun("iptables -F euca-ipsets-fwd"); rc=rc>>8;
  rc = vrun("iptables -F euca-ipsets-out"); rc=rc>>8;
  rc = vrun("iptables -A FORWARD -m conntrack --ctstate ESTABLISHED -j ACCEPT"); rc=rc>>8;
    

  for (i=0; i<max_security_groups; i++) {

    FH=fopen(ips_file, "w");
    if (!FH) {
    } else {
      rc = vrun("ipset -X %s.stage", security_groups[i].chainname); rc=rc>>8;
      rc = vrun("ipset -N %s iphash", security_groups[i].chainname); rc=rc>>8;
      fprintf(FH, "-N %s.stage iphash --hashsize 1024 --probes 8 --resize 50\n", security_groups[i].chainname);
      for (j=0; j<security_groups[i].max_member_ips; j++) {
	fprintf(FH, "-A %s.stage %s\n", security_groups[i].chainname, hex2dot(security_groups[i].member_ips[j]));
      }
      fprintf(FH, "COMMIT\n");
      fclose(FH);
      rc = vrun("cat %s | ipset --restore", ips_file); rc=rc>>8;
      rc = vrun("ipset --swap %s.stage %s", security_groups[i].chainname, security_groups[i].chainname); rc=rc>>8;
      rc = vrun("ipset -X %s.stage", security_groups[i].chainname); rc=rc>>8;
    }

    // TODO: add fail checks
    
    rc = vrun("iptables -F %s", security_groups[i].chainname); rc=rc>>8;
      // add forward chain
    rc = vrun("iptables -N %s", security_groups[i].chainname); rc=rc>>8;
    
    // add jump rule
    rc = vrun("iptables -A euca-ipsets-fwd -m set --set %s dst -j %s", security_groups[i].chainname, security_groups[i].chainname); rc=rc>>8;

    // populate forward chain
    // this one needs to be first
    rc = vrun("iptables -I %s -m set --set %s src,dst -j ACCEPT", security_groups[i].chainname, security_groups[i].chainname); rc=rc>>8;
    
    // then put all the group specific IPT rules (temporary one here)
    rc = vrun("iptables -A %s -s 1.1.0.1 -p tcp -m tcp --dport 22 -j ACCEPT", security_groups[i].chainname); rc=rc>>8;

    // this ones needs to be last
    rc = vrun("iptables -A %s -j DROP", security_groups[i].chainname); rc=rc>>8;
    
  }
  
  unlink(ips_file);
  return(ret);
}

int update_public_ips() {
  int slashnet, ret=0, rc, i;
  
  // install EL IP addrs and NAT rules
  // add addr/32 to pub interface
  rc = flush_euca_edge_chains();
  if (rc) {
    LOGERROR("failed to flush table euca-edge-nat\n");
    return(1);
  }
  
  slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - vnetconfig->networks[0].nm) + 1))));
  for (i=0; i<max_ips; i++) {
    if ((public_ips[i] && private_ips[i]) && (public_ips[i] != private_ips[i])) {
      rc = vrun("ip addr add %s/%d dev %s >/dev/null 2>&1", hex2dot(public_ips[i]), slashnet, vnetconfig->pubInterface); rc = rc>>8;
      if (rc && (rc != 2)) {
	ret = 1;
      } else {
	// install DNAT and SNAT rules for pub->priv mappings
	rc = install_euca_edge_natrules(hex2dot(public_ips[i]), hex2dot(private_ips[i]));
	if (rc) {
	  LOGERROR("failed to install natrules for host '%s'\n", hex2dot(private_ips[i]));
	  ret=1;
	}
      }
    } else if (public_ips[i] && !private_ips[0]) {
      rc = vrun("ip addr del %s/%d dev %s >/dev/null 2>&1", hex2dot(public_ips[i]), slashnet, vnetconfig->pubInterface); rc = rc>>8;
      if (rc && (rc != 2)) {
	ret=1;
      }
    }
  }
  
  return(ret);
}


int update_private_ips() {
  int ret=0, rc, i;
  char mac[32];

  // populate vnetconfig with new info
  for (i=0; i<max_ips; i++) {
    if (private_ips[i]) {
      LOGINFO("adding ip: %s\n", hex2dot(private_ips[i]));
      rc = vnetAddPrivateIP(vnetconfig, hex2dot(private_ips[i]));
      if (rc) {
	LOGERROR("could not add private IP '%s'\n", hex2dot(private_ips[i]));
	ret=1;
      } else {
	rc = vnetGenerateNetworkParams(vnetconfig, "", 0, -1, mac, hex2dot(public_ips[i]), hex2dot(private_ips[i]));
	if (rc) {
	  LOGERROR("could not enable host '%s'\n", hex2dot(private_ips[i]));
	  ret=1;
	}
      }
    }
  }
  
  // generate DHCP config, monitor/start DHCP service
  rc = vnetKickDHCP(vnetconfig);
  if (rc) {
    LOGERROR("failed to kick dhcpd\n");
    ret=1;
  }

  return(ret);
}

int check_for_network_update() {
  if (strcmp(last_network_topology_hash, curr_network_topology_hash) || strcmp(last_pubprivmap_hash, curr_pubprivmap_hash)) {
    if (last_network_topology_hash) EUCA_FREE(last_network_topology_hash);
    if (last_pubprivmap_hash) EUCA_FREE(last_pubprivmap_hash);
    last_network_topology_hash = strdup(curr_network_topology_hash);
    last_pubprivmap_hash = strdup(curr_pubprivmap_hash);
    return(1);
  }
  return(0);
}

int get_config_cc(char *ccIp) {
  char configFiles[2][MAX_PATH];
  char *tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME), home[MAX_PATH], url[MAX_PATH], config_ccfile[MAX_PATH], netPath[MAX_PATH];
  char *cvals[EUCANETD_CVAL_LAST];
  int fd, rc, ret, i;
  
  ret = 0;
  bzero(cvals, sizeof(char *) * EUCANETD_CVAL_LAST);
  
  for (i=0; i<EUCANETD_CVAL_LAST; i++) {
    EUCA_FREE(cvals[i]);
  }

  if (!tmpstr) {
    snprintf(home, MAX_PATH, "/");
  } else {
    snprintf(home, MAX_PATH, "%s", tmpstr);
  }

  snprintf(config_ccfile, MAX_PATH, "/tmp/euca-config_ccfile-XXXXXX");
  
  fd = safe_mkstemp(config_ccfile);
  if (fd < 0) {
    LOGERROR("cannot open config_ccfile '%s'\n", config_ccfile);
    for (i=0; i<EUCANETD_CVAL_LAST; i++) {
      EUCA_FREE(cvals[i]);
    }
    return (1);
  }
  chmod(config_ccfile, 0644);
  close(fd);

  snprintf(netPath, MAX_PATH, CC_NET_PATH_DEFAULT, home);
  snprintf(configFiles[0], MAX_PATH, EUCALYPTUS_CONF_LOCATION, home);
  snprintf(configFiles[1], MAX_PATH, "%s", config_ccfile);
  
  // fetch CC configuration
  snprintf(url, MAX_PATH, "http://%s:8776/config-cc", ccIp);
  rc = http_get_timeout(url, config_ccfile, 0, 0, 10, 15);
  if (rc) {
    LOGWARN("cannot get latest configuration from CC(%s)\n", SP(ccIp));
    unlink(config_ccfile);
    for (i=0; i<EUCANETD_CVAL_LAST; i++) {
      EUCA_FREE(cvals[i]);
    }
    return (1);
  }
  
  configInitValues(configKeysRestartEUCANETD, configKeysNoRestartEUCANETD);   // initialize config subsystem
  readConfigFile(configFiles, 2);
  
  // thing to read from the NC config file
  cvals[EUCANETD_CVAL_PUBINTERFACE] = configFileValue("VNET_PUBINTERFACE");
  cvals[EUCANETD_CVAL_PRIVINTERFACE] = configFileValue("VNET_PRIVINTERFACE");
  cvals[EUCANETD_CVAL_BRIDGE] = configFileValue("VNET_BRIDGE");
  cvals[EUCANETD_CVAL_EUCAHOME] = configFileValue("EUCALYPTUS");
  cvals[EUCANETD_CVAL_MODE] = configFileValue("VNET_MODE");
  
  cvals[EUCANETD_CVAL_ADDRSPERNET] = configFileValue("VNET_ADDRSPERNET");
  cvals[EUCANETD_CVAL_SUBNET] = configFileValue("VNET_SUBNET");
  cvals[EUCANETD_CVAL_NETMASK] = configFileValue("VNET_NETMASK");
  cvals[EUCANETD_CVAL_BROADCAST] = configFileValue("VNET_BROADCAST");
  cvals[EUCANETD_CVAL_DNS] = configFileValue("VNET_DNS");
  cvals[EUCANETD_CVAL_DOMAINNAME] = configFileValue("VNET_DOMAINNAME");
  cvals[EUCANETD_CVAL_ROUTER] = configFileValue("VNET_ROUTER");
  cvals[EUCANETD_CVAL_DHCPDAEMON] = configFileValue("VNET_DHCPDAEMON");
  cvals[EUCANETD_CVAL_DHCPUSER] = configFileValue("VNET_DHCPUSER");
  cvals[EUCANETD_CVAL_MACPREFIX] = configFileValue("VNET_MACPREFIX");

  cvals[EUCANETD_CVAL_CLCIP] = configFileValue("CLCIP");
  cvals[EUCANETD_CVAL_CC_POLLING_FREQUENCY] = configFileValue("CC_POLLING_FREQUENCY");
  
  ret = vnetInit(vnetconfig, cvals[EUCANETD_CVAL_MODE], cvals[EUCANETD_CVAL_EUCAHOME], netPath, CLC, cvals[EUCANETD_CVAL_PUBINTERFACE], cvals[EUCANETD_CVAL_PRIVINTERFACE], cvals[EUCANETD_CVAL_ADDRSPERNET], cvals[EUCANETD_CVAL_SUBNET], cvals[EUCANETD_CVAL_NETMASK], cvals[EUCANETD_CVAL_BROADCAST], cvals[EUCANETD_CVAL_DNS], cvals[EUCANETD_CVAL_DOMAINNAME], cvals[EUCANETD_CVAL_ROUTER], cvals[EUCANETD_CVAL_DHCPDAEMON], cvals[EUCANETD_CVAL_DHCPUSER], cvals[EUCANETD_CVAL_BRIDGE], NULL, cvals[EUCANETD_CVAL_MACPREFIX]);

  if (clcIp) EUCA_FREE(clcIp);
  clcIp = strdup(cvals[EUCANETD_CVAL_CLCIP]);
  cc_polling_frequency = atoi(cvals[EUCANETD_CVAL_CC_POLLING_FREQUENCY]);
  
  for (i=0; i<EUCANETD_CVAL_LAST; i++) {
    EUCA_FREE(cvals[i]);
  }
  unlink(config_ccfile);

  return(ret);
  
}

int install_euca_edge_natrules(char *pubip, char *privip) {
  int rc, ret=0;

  rc = vrun("iptables -t nat -I euca-edge-nat-pre -d %s/32 -j DNAT --to-destination %s", pubip, privip); rc=rc>>8;
  if (rc) {
    ret=1;
  }

  rc = vrun("iptables -t nat -I euca-edge-nat-out -d %s/32 -j DNAT --to-destination %s", pubip, privip); rc=rc>>8;
  if (rc) {
    ret=1;
  }
  
  rc = vrun("iptables -t nat -I euca-edge-nat-post -d %s/32 -j SNAT --to-source %s", privip, pubip); rc=rc>>8;
  if (rc) {
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
    rc = vrun(cmds[i]); rc=rc>>8;
    if (rc > 1) {
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
    rc = vrun(cmds[i]); rc=rc>>8;
    if (rc > 1) {
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

int fetchread_latest_network(char *ccIp) {
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
  } else {
    rc = parse_network_topology(network_topology_file);
    if (rc) {
      LOGERROR("cannot parse network-topology file (%s)\n", network_topology_file);
      unlink(network_topology_file);
      return(1);
    }
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

int parse_network_topology(char *file) {
  int ret=0;
  FILE *FH=NULL;
  char buf[MAX_PATH];
  char *toka=NULL, *ptra=NULL, *modetok=NULL, *grouptok=NULL;
  int linemode=0;
  sec_group *newgroups=NULL;
  int max_newgroups=0, curr_group=0;

  enum {PNT_ZERO, PNT_RULE, PNT_GROUP};
  
  FH=fopen(file, "r");
  if (!FH) {
    ret=1;
  } else {
    while (fgets(buf, MAX_PATH, FH)) {
      modetok = strtok_r(buf, " ", &ptra);
      grouptok = strtok_r(NULL, " ", &ptra);

      if (modetok && grouptok) {
	
	if (!strcmp(modetok, "GROUP")) {
	  char *tmp=NULL;
	  linemode = PNT_GROUP;
	  curr_group = max_newgroups;
	  max_newgroups++;
	  newgroups = realloc(newgroups, sizeof(sec_group) * max_newgroups);
	  bzero(&(newgroups[curr_group]), sizeof(sec_group));
	  sscanf(grouptok, "%128[0-9]-%128s", newgroups[curr_group].accountId, newgroups[curr_group].name);
	  hash_b64enc_string(grouptok, &tmp);
	  if (tmp) {
	    snprintf(newgroups[curr_group].chainname, 32, "%s", tmp);
	  }
	} else if (!strcmp(modetok, "RULE")) {
	  linemode = PNT_RULE;
	} else {
	  linemode = PNT_ZERO;
	}

	if (linemode) {
	  toka = strtok_r(NULL, " ", &ptra);
	  while(toka) {
	    if (linemode == PNT_GROUP) {
	      newgroups[curr_group].member_ips[newgroups[curr_group].max_member_ips] = dot2hex(toka);
	      newgroups[curr_group].max_member_ips++;
	    } else if (linemode == PNT_RULE) {
	      
	    }
	    
	    toka = strtok_r(NULL, " ", &ptra);
	  }

	} else {
	  LOGWARN("bad linemode (%s), skipping\n", toka);
	}
      }
    }
    fclose(FH);
  }

  if (ret == 0) {
    if (security_groups) EUCA_FREE(security_groups);
    security_groups = newgroups;
    max_security_groups = max_newgroups;
  }
  
  print_sec_groups(security_groups, max_security_groups);

  return(ret);
}

void print_sec_groups(sec_group *newgroups, int max_newgroups) {
  int i, j;
  for (i=0; i<max_newgroups; i++) {
    printf("GROUPNAME: %s GROUPACCOUNTID: %s GROUPCHAINNAME: %s\n", newgroups[i].name, newgroups[i].accountId, newgroups[i].chainname);
    for (j=0; j<newgroups[i].max_member_ips; j++) {
      printf("\tIP MEMBER: %s\n", hex2dot(newgroups[i].member_ips[j]));
    }
  }
}
