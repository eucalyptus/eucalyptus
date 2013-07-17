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
#include <sequence_executor.h>

#include "eucanetd.h"
#include "config-eucanetd.h"

vnetConfig *vnetconfig = NULL;
eucanetdConfig *config = NULL;

int main (int argc, char **argv) {
  int rc=0;

  // initialize the logfile
  init_log();
  eucanetdInit();

  // parse commandline arguments
  if (argv[1]) {
    config->ccIp = strdup(argv[1]);
  }

  if (!config->ccIp) {
    LOGERROR("must supply ccIp on the CLI\n");
    exit(1);
  }

  // initialize some globals
  config->last_network_topology_hash = strdup("UNSET");
  config->curr_network_topology_hash = strdup("UNSET");
  config->last_pubprivmap_hash = strdup("UNSET");
  config->curr_pubprivmap_hash = strdup("UNSET");
  // initialize vnetconfig from local eucalyptus.conf and remote (CC) dynamic config; spin looking for config from CC until one is available
  vnetconfig = malloc(sizeof(vnetConfig));
  bzero(vnetconfig, sizeof(vnetConfig));  
  rc = 1;
  while(rc) {
    rc = get_config_cc(config->ccIp);
    if (rc) {
      LOGWARN("cannot fetch latest initial config from CC (%s), waiting for config to become available\n", config->ccIp);
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
    rc = fetch_latest_network(config->ccIp);
    if (rc) {
      LOGWARN("fetch_latest_network from CC failed\n");
    } 

    rc = read_latest_network();
    if (rc) {
      LOGWARN("read_latest_network failed, skipping update\n");
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
      rc = update_metadata_redirect();
      if (rc) {
	LOGERROR("could not update metadata redirect rule\n");
      }
      // install ebtables rules for isolation
      
      // install iptables FW rules, using IPsets for sec. group 
      rc = update_sec_groups();
      if (rc) {
	LOGERROR("could not complete update of security groups\n");
      }
      
    }
    
    // do it all over again...
    sleep (config->cc_polling_frequency);
  }
  
  exit(0);
}

int update_metadata_redirect() {
  int ret=0, rc;
  char cmd[MAX_PATH], clcmd[MAX_PATH];
  sequence_executor cmds;

  se_init(&cmds, 1);

  snprintf(cmd, MAX_PATH, "iptables -t nat -D PREROUTING -d 169.254.169.254/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", config->clcIp);
  se_add(&cmds, cmd, NULL, ignore_exit);

  snprintf(cmd, MAX_PATH, "iptables -t nat -A PREROUTING -d 169.254.169.254/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", config->clcIp);
  snprintf(clcmd, MAX_PATH, "iptables -t nat -D PREROUTING -d 169.254.169.254/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", config->clcIp);
  se_add(&cmds, cmd, clcmd, NULL);
  
  rc = se_execute(&cmds);
  if (rc) {
    LOGERROR("could not execute sequence of commands\n");
    se_print(&cmds);
    ret=1;
  }
  se_free(&cmds);
  return(ret);
}

int eucanetdInit() {
  if (!config) {
    config = malloc(sizeof(eucanetdConfig));
    if (!config) {
      LOGFATAL("out of memory\n");
      exit(1);
    }
  }
  bzero(config, sizeof(eucanetdConfig));
  config->cc_polling_frequency = 1;
  config->init = 1;
  return(0);
}

int update_sec_groups() {
  int ret=0, i, rc, j, fd;
  char ips_file[MAX_PATH];
  FILE *FH=NULL;
  sequence_executor cmds;

  // make ipsets
  snprintf(ips_file, MAX_PATH, "/tmp/ips_file-XXXXXX");
  fd = safe_mkstemp(ips_file);
  if (fd < 0) {
    LOGERROR("cannot open ips_file '%s'\n", ips_file);
    return (1);
  }
  chmod(ips_file, 0644);
  close(fd);

  se_init(&cmds, 1);
  rc = se_add(&cmds, "iptables -F euca-ipsets-fwd", NULL, ignore_exit);
  rc = se_execute(&cmds);
  se_free(&cmds);

  for (i=0; i<config->max_security_groups; i++) {
    char cmd[MAX_PATH], clcmd[MAX_PATH];
    
    FH=fopen(ips_file, "w");
    if (!FH) {
    } else {
      se_init(&cmds, 1);
      
      snprintf(cmd, MAX_PATH, "ipset -X %s.stage", config->security_groups[i].chainname);
      rc = se_add(&cmds, cmd, NULL, ignore_exit);
      
      snprintf(cmd, MAX_PATH, "ipset -N %s iphash", config->security_groups[i].chainname);
      snprintf(clcmd, MAX_PATH, "ipset -X %s", config->security_groups[i].chainname);
      rc = se_add(&cmds, cmd, clcmd, check_stderr_already_exists);
      
      fprintf(FH, "-N %s.stage iphash --hashsize 1024 --probes 8 --resize 50\n", config->security_groups[i].chainname);
      for (j=0; j<config->security_groups[i].max_member_ips; j++) {
	fprintf(FH, "-A %s.stage %s\n", config->security_groups[i].chainname, hex2dot(config->security_groups[i].member_ips[j]));
      }
      fprintf(FH, "COMMIT\n");
      fclose(FH);
      
      snprintf(cmd, MAX_PATH, "cat %s | ipset --restore", ips_file);
      snprintf(clcmd, MAX_PATH, "ipset -F %s", config->security_groups[i].chainname);
      rc = se_add(&cmds, cmd, NULL, NULL);
      
      snprintf(cmd, MAX_PATH, "ipset --swap %s.stage %s", config->security_groups[i].chainname, config->security_groups[i].chainname); rc=rc>>8;
      rc = se_add(&cmds, cmd, NULL, NULL);
      
      snprintf(cmd, MAX_PATH, "ipset -X %s.stage", config->security_groups[i].chainname);
      rc = se_add(&cmds, cmd, NULL, ignore_exit);
      
      // TODO: add fail checks
      
      // add forward chain
      snprintf(cmd, MAX_PATH, "iptables -N %s", config->security_groups[i].chainname);    
      snprintf(clcmd, MAX_PATH, "iptables -X %s", config->security_groups[i].chainname);    
      rc = se_add(&cmds, cmd, clcmd, check_stderr_already_exists);
      
      snprintf(cmd, MAX_PATH, "iptables -F %s", config->security_groups[i].chainname);    
      rc = se_add(&cmds, cmd, NULL, ignore_exit);
      
      // add jump rule
      snprintf(cmd, MAX_PATH, "iptables -A euca-ipsets-fwd -m set --set %s dst -j %s", config->security_groups[i].chainname, config->security_groups[i].chainname);    
      snprintf(clcmd, MAX_PATH, "iptables -F euca-ipsets-fwd");    
      rc = se_add(&cmds, cmd, clcmd, NULL);
      
      // populate forward chain
      // this one needs to be first
      snprintf(cmd, MAX_PATH, "iptables -I %s -m set --set %s src,dst -j ACCEPT", config->security_groups[i].chainname, config->security_groups[i].chainname);    
      snprintf(clcmd, MAX_PATH, "iptables -F %s", config->security_groups[i].chainname);
      rc = se_add(&cmds, cmd, clcmd, NULL);
      
      // then put all the group specific IPT rules (temporary one here)
      for (j=0; j<config->security_groups[i].max_grouprules; j++) {
	snprintf(cmd, MAX_PATH, "iptables -A %s %s -j ACCEPT", config->security_groups[i].chainname, config->security_groups[i].grouprules[j]);
	snprintf(clcmd, MAX_PATH, "iptables -D %s %s -j ACCEPT", config->security_groups[i].chainname, config->security_groups[i].grouprules[j]);
	rc = se_add(&cmds, cmd, clcmd, NULL);
      }

      snprintf(cmd, MAX_PATH, "iptables -A %s -m conntrack --ctstate ESTABLISHED -j ACCEPT", config->security_groups[i].chainname);    
      snprintf(clcmd, MAX_PATH, "iptables -D %s -m conntrack --ctstate ESTABLISHED -j ACCEPT", config->security_groups[i].chainname);    
      rc = se_add(&cmds, cmd, clcmd, NULL);
      
      // this ones needs to be last
      snprintf(cmd, MAX_PATH, "iptables -A %s -j DROP", config->security_groups[i].chainname);    
      snprintf(clcmd, MAX_PATH, "iptables -D %s -j DROP", config->security_groups[i].chainname);    
      rc = se_add(&cmds, cmd, clcmd, NULL);

      rc = se_execute(&cmds);
      if (rc) {
	LOGERROR("failed to execute command sequence\n");
	se_print(&cmds);
      } 
      se_free(&cmds);
    }
  }
  
  unlink(ips_file);
  return(ret);
}

int update_public_ips() {
  int slashnet, ret=0, rc, i;
  char cmd[MAX_PATH], clcmd[MAX_PATH];
  char *strptra=NULL, *strptrb=NULL;
  sequence_executor cmds;
  
  // install EL IP addrs and NAT rules
  // add addr/32 to pub interface
  rc = flush_euca_edge_chains();
  if (rc) {
    LOGERROR("failed to flush table euca-edge-nat\n");
    return(1);
  }
  
  slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - vnetconfig->networks[0].nm) + 1))));

  se_init(&cmds, 1);
  for (i=0; i<config->max_ips; i++) {
    strptra = hex2dot(config->public_ips[i]);
    strptrb = hex2dot(config->private_ips[i]);
    if ((config->public_ips[i] && config->private_ips[i]) && (config->public_ips[i] != config->private_ips[i])) {
      snprintf(cmd, MAX_PATH, "ip addr add %s/%d dev %s >/dev/null 2>&1", strptra, slashnet, vnetconfig->pubInterface);
      se_add(&cmds, cmd, NULL, ignore_exit2);
      
      snprintf(cmd, MAX_PATH, "iptables -t nat -I euca-edge-nat-pre -d %s/32 -j DNAT --to-destination %s", strptra, strptrb);
      se_add(&cmds, cmd, NULL, NULL);
      
      snprintf(cmd, MAX_PATH, "iptables -t nat -I euca-edge-nat-out -d %s/32 -j DNAT --to-destination %s", strptra, strptrb);
      se_add(&cmds, cmd, NULL, NULL);
      
      snprintf(cmd, MAX_PATH, "iptables -t nat -I euca-edge-nat-post -d %s/32 -j SNAT --to-source %s", strptra, strptrb);
      se_add(&cmds, cmd, NULL, NULL);
      
    } else if (config->public_ips[i] && !config->private_ips[0]) {
      snprintf(cmd, MAX_PATH, "ip addr del %s/%d dev %s >/dev/null 2>&1", strptra, slashnet, vnetconfig->pubInterface);
      se_add(&cmds, cmd, NULL, ignore_exit2);
    }
  }

  rc = se_execute(&cmds);
  if (rc) {
    LOGERROR("could not execute command sequence\n");
    se_print(&cmds);
    ret=1;
  }
  
  return(ret);
}


int update_private_ips() {
  int ret=0, rc, i;
  char mac[32];

  // populate vnetconfig with new info
  for (i=0; i<config->max_ips; i++) {
    if (config->private_ips[i]) {
      LOGINFO("adding ip: %s\n", hex2dot(config->private_ips[i]));
      rc = vnetAddPrivateIP(vnetconfig, hex2dot(config->private_ips[i]));
      if (rc) {
	LOGERROR("could not add private IP '%s'\n", hex2dot(config->private_ips[i]));
	ret=1;
      } else {
	rc = vnetGenerateNetworkParams(vnetconfig, "", 0, -1, mac, hex2dot(config->public_ips[i]), hex2dot(config->private_ips[i]));
	if (rc) {
	  LOGERROR("could not enable host '%s'\n", hex2dot(config->private_ips[i]));
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
  int ret=0;
  
  if (strcmp(config->last_network_topology_hash, config->curr_network_topology_hash)) {
    ret=1;
    LOGDEBUG("network topology hash has changed\n");
  } else if (strcmp(config->last_pubprivmap_hash, config->curr_pubprivmap_hash)) {
    LOGDEBUG("pub/priv mapping hash has changed\n");
    ret=1;
  }

  if (ret) {
    if (config->last_network_topology_hash) EUCA_FREE(config->last_network_topology_hash);
    if (config->last_pubprivmap_hash) EUCA_FREE(config->last_pubprivmap_hash);
    config->last_network_topology_hash = strdup(config->curr_network_topology_hash);
    config->last_pubprivmap_hash = strdup(config->curr_pubprivmap_hash);
  }

  return(ret);
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

  if (config->clcIp) EUCA_FREE(config->clcIp);
  config->clcIp = strdup(cvals[EUCANETD_CVAL_CLCIP]);
  config->cc_polling_frequency = atoi(cvals[EUCANETD_CVAL_CC_POLLING_FREQUENCY]);
  snprintf(config->network_topology_file, MAX_PATH, "%s/var/lib/eucalyptus/eucanetd_network_topology_file", cvals[EUCANETD_CVAL_EUCAHOME]);
  snprintf(config->pubprivmap_file, MAX_PATH, "%s/var/lib/eucalyptus/eucanetd_pubprivmap_file", cvals[EUCANETD_CVAL_EUCAHOME]);

  for (i=0; i<EUCANETD_CVAL_LAST; i++) {
    EUCA_FREE(cvals[i]);
  }
  unlink(config_ccfile);

  return(ret);
  
}

int flush_euca_edge_chains() {
  int rc, ret=0;

  sequence_executor cmds;
  
  se_init(&cmds, 1);
  rc = se_add(&cmds, "iptables -t nat -F euca-edge-nat-pre", NULL, ignore_exit);
  rc = se_add(&cmds, "iptables -t nat -F euca-edge-nat-post", NULL, ignore_exit);
  rc = se_add(&cmds, "iptables -t nat -F euca-edge-nat-out", NULL, ignore_exit);
  rc = se_execute(&cmds);
  if (rc) {
    LOGERROR("failed to execute command sequence\n");
    ret=1;
    se_print(&cmds);
  }
  se_free(&cmds);

  
  return(ret);
}

int create_euca_edge_chains() {
  int rc, ret=0;
  sequence_executor cmds;
  
  se_init(&cmds, 1);
  rc = se_add(&cmds, "iptables -N euca-ipsets-fwd", "iptables -X euca-ipsets-fwd", check_stderr_already_exists);
  rc = se_add(&cmds, "iptables -N euca-ipsets-in", "iptables -X euca-ipsets-in", check_stderr_already_exists);
  rc = se_add(&cmds, "iptables -N euca-ipsets-out", "iptables -X euca-ipsets-out", check_stderr_already_exists);
  rc = se_add(&cmds, "iptables -F euca-ipsets-in", NULL, NULL);
  rc = se_add(&cmds, "iptables -F euca-ipsets-fwd", NULL, NULL);
  rc = se_add(&cmds, "iptables -F euca-ipsets-out", NULL, NULL);
  rc = se_add(&cmds, "iptables -D INPUT -j euca-ipsets-in", NULL, ignore_exit);
  rc = se_add(&cmds, "iptables -A INPUT -j euca-ipsets-in", "iptables -D INPUT -j euca-ipsets-in", NULL);
  rc = se_add(&cmds, "iptables -D FORWARD -j euca-ipsets-fwd", NULL, ignore_exit);
  rc = se_add(&cmds, "iptables -A FORWARD -j euca-ipsets-fwd", "iptables -D FORWARD -j euca-ipsets-fwd", NULL);
  rc = se_add(&cmds, "iptables -D OUTPUT -j euca-ipsets-out", NULL, ignore_exit);
  rc = se_add(&cmds, "iptables -A OUTPUT -j euca-ipsets-out", "iptables -D OUTPUT -j euca-ipsets-out", NULL);
  rc = se_add(&cmds, "iptables -A FORWARD -m conntrack --ctstate ESTABLISHED -j ACCEPT", "iptables -D FORWARD -m conntrack --ctstate ESTABLISHED -j ACCEPT", NULL);
  rc = se_add(&cmds, "iptables -t nat -N euca-edge-nat-pre", "iptables -t nat -X euca-edge-nat-pre", check_stderr_already_exists); 
  rc = se_add(&cmds, "iptables -t nat -N euca-edge-nat-post", "iptables -t nat -X euca-edge-nat-post", check_stderr_already_exists); 
  rc = se_add(&cmds, "iptables -t nat -N euca-edge-nat-out", "iptables -t nat -X euca-edge-nat-out", check_stderr_already_exists);
  rc = se_add(&cmds, "iptables -t nat -F euca-edge-nat-pre", NULL, NULL);
  rc = se_add(&cmds, "iptables -t nat -F euca-edge-nat-post", NULL, NULL);
  rc = se_add(&cmds, "iptables -t nat -F euca-edge-nat-out", NULL, NULL);
  rc = se_add(&cmds, "iptables -t nat -D PREROUTING -j euca-edge-nat-pre", NULL, ignore_exit);
  rc = se_add(&cmds, "iptables -t nat -I PREROUTING 1 -j euca-edge-nat-pre", "iptables -t nat -D PREROUTING 1 -j euca-edge-nat-pre", NULL);
  rc = se_add(&cmds, "iptables -t nat -D POSTROUTING -j euca-edge-nat-post", NULL, ignore_exit);
  rc = se_add(&cmds, "iptables -t nat -I POSTROUTING 1 -j euca-edge-nat-post", "iptables -t nat -D POSTROUTING 1 -j euca-edge-nat-post", NULL);
  rc = se_add(&cmds, "iptables -t nat -D OUTPUT -j euca-edge-nat-out", NULL, ignore_exit);
  rc = se_add(&cmds, "iptables -t nat -I OUTPUT 1 -j euca-edge-nat-out", "iptables -t nat -D OUTPUT 1 -j euca-edge-nat-out", NULL);
  rc = se_execute(&cmds);
  if (rc) {
    LOGERROR("failed to execute command sequence\n");
    ret=1;
    se_print(&cmds);
  }
  se_free(&cmds);

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

int read_latest_network() {
  int rc, ret=0;

  rc = parse_network_topology(config->network_topology_file);
  if (rc) {
    LOGERROR("cannot parse network-topology file (%s)\n", config->network_topology_file);
    ret=1;
  }

  rc = parse_pubprivmap(config->pubprivmap_file);
  if (rc) {
    LOGERROR("cannot parse pubprivmap file (%s)\n", config->pubprivmap_file);
    ret=1;
  }
  return(ret);
}

int parse_pubprivmap(char *pubprivmap_file) {
  char buf[1024], priv[64], pub[64];
  int count=0, ret=0;
  FILE *FH = NULL;
  
  FH =fopen(pubprivmap_file, "r");  
  if (FH) {
    while (fgets(buf, 1024, FH)) {
      priv[0] = pub[0] = '\0';
      sscanf(buf, "%[0-9.]=%[0-9.]", pub, priv);
      if ( (strlen(priv) && strlen(pub)) && !(!strcmp(priv, "0.0.0.0") && !strcmp(pub, "0.0.0.0")) ) {
	config->private_ips[count] = dot2hex(priv);
	config->public_ips[count] = dot2hex(pub);
	count++;      
	config->max_ips = count;
      }
    }
    fclose(FH);
  } else {
    LOGERROR("could not open map file for read (%s)\n", pubprivmap_file);
    ret=1;
  }
  return(ret);
}


int fetch_latest_network(char *ccIp) {
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
  }
  
  if (config->curr_network_topology_hash) EUCA_FREE(config->curr_network_topology_hash);
  config->curr_network_topology_hash=file2md5str(network_topology_file);
  if (!config->curr_network_topology_hash) config->curr_network_topology_hash = strdup("UNSET");
  
  snprintf(url, MAX_PATH, "http://%s:8776/pubprivipmap", ccIp);
  rc = http_get_timeout(url, pubprivmap_file, 0, 0, 10, 15);
  if (rc) {
    LOGWARN("cannot get latest pubprivmap from CC\n");
    unlink(network_topology_file);
    unlink(pubprivmap_file);
    return (1);
  }
  if (config->curr_pubprivmap_hash) EUCA_FREE(config->curr_pubprivmap_hash);
  config->curr_pubprivmap_hash=file2md5str(pubprivmap_file);
  if (!config->curr_pubprivmap_hash) config->curr_pubprivmap_hash=strdup("UNSET");

  rc = rename(network_topology_file, config->network_topology_file);
  if (rc) {
    LOGERROR("could not rename downloaded file (%s) to local file (%s)\n", network_topology_file, config->network_topology_file);
    ret = 1;
  }
  rc = rename(pubprivmap_file, config->pubprivmap_file);
  if (rc) {
    LOGERROR("could not rename downloaded file (%s) to local file (%s)\n", pubprivmap_file, config->pubprivmap_file);
    ret = 1;
  }
  if (ret) {
    unlink(network_topology_file);
    unlink(pubprivmap_file);
  }
  
  return(ret);
}

int parse_network_topology(char *file) {
  int ret=0, rc, gidx, i;
  FILE *FH=NULL;
  char buf[MAX_PATH], rulebuf[2048], newrule[2048];
  char *toka=NULL, *ptra=NULL, *modetok=NULL, *grouptok=NULL, *chainname;
  sec_group *newgroups=NULL;
  int max_newgroups=0, curr_group=0;

  // do the GROUP pass first, then RULE pass
  FH=fopen(file, "r");
  if (!FH) {
    ret=1;
  } else {
    while (fgets(buf, MAX_PATH, FH)) {
      modetok = strtok_r(buf, " ", &ptra);
      grouptok = strtok_r(NULL, " ", &ptra);

      if (modetok && grouptok) {
	
	if (!strcmp(modetok, "GROUP")) {
	  curr_group = max_newgroups;
	  max_newgroups++;
	  newgroups = realloc(newgroups, sizeof(sec_group) * max_newgroups);
	  bzero(&(newgroups[curr_group]), sizeof(sec_group));
	  sscanf(grouptok, "%128[0-9]-%128s", newgroups[curr_group].accountId, newgroups[curr_group].name);
	  hash_b64enc_string(grouptok, &chainname);
	  if (chainname) {
	    snprintf(newgroups[curr_group].chainname, 32, "%s", chainname);
	    EUCA_FREE(chainname);
	  }

	  toka = strtok_r(NULL, " ", &ptra);
	  while(toka) {
	    newgroups[curr_group].member_ips[newgroups[curr_group].max_member_ips] = dot2hex(toka);
	    newgroups[curr_group].max_member_ips++;
	    toka = strtok_r(NULL, " ", &ptra);
	  }
	}
      }
    }
    fclose(FH);
  }

  if (ret == 0) {
    int i;
    if (config->security_groups) EUCA_FREE(config->security_groups);
    config->security_groups = newgroups;
    config->max_security_groups = max_newgroups;
  }  

  // now do RULE pass
  FH=fopen(file, "r");
  if (!FH) {
    ret=1;
  } else {
    while (fgets(buf, MAX_PATH, FH)) {
      modetok = strtok_r(buf, " ", &ptra);
      grouptok = strtok_r(NULL, " ", &ptra);
      rulebuf[0] = '\0';	      
      
      if (modetok && grouptok) {	
	if (!strcmp(modetok, "RULE")) {
	  hash_b64enc_string(grouptok, &chainname);
	  gidx=-1;
	  for (i=0; i<config->max_security_groups; i++) {
	    if (!strcmp(config->security_groups[i].chainname, chainname)) {
	      gidx=i;
	      break;
	    }
	  }
	  if (gidx >= 0) {
	    toka = strtok_r(NULL, " ", &ptra);
	    while(toka) {
	      strncat(rulebuf, toka, 2048);
	      strncat(rulebuf, " ", 2048);
	      toka = strtok_r(NULL, " ", &ptra);
	    }
	    rc = ruleconvert(rulebuf, newrule);
	    if (rc) {
	      LOGERROR("could not convert rule (%s)\n", SP(rulebuf));
	    } else {
	      config->security_groups[gidx].grouprules[config->security_groups[gidx].max_grouprules] = strdup(newrule);
	      config->security_groups[gidx].max_grouprules++;
	    }
	  }
	}
      }
    }
    fclose(FH);
  }
  
  print_sec_groups(config->security_groups, config->max_security_groups);
  
  return(ret);
}

int ruleconvert(char *rulebuf, char *outrule) {
  int ret=0;
  char proto[64], portrange[64], sourcecidr[64], icmptyperange[64], sourceowner[64], sourcegroup[64], newrule[2048], buf[2048];
  char *ptra=NULL, *toka=NULL, *idx=NULL;
  
  proto[0] = portrange[0] = sourcecidr[0] = icmptyperange[0] = newrule[0] = sourceowner[0] = sourcegroup[0] = '\0';
  
  if ( (idx=strchr(rulebuf, '\n')) ) {
    *idx = '\0';
  }

  toka = strtok_r(rulebuf, " ", &ptra);
  while(toka) {
    if (!strcmp(toka, "-P")) {
      toka = strtok_r(NULL, " ", &ptra);
      if (toka) snprintf(proto, 64, "%s", toka);
    } else if (!strcmp(toka, "-p")) {
      toka = strtok_r(NULL, " ", &ptra);
      if (toka) snprintf(portrange, 64, "%s", toka);
      if ( (idx = strchr(portrange, '-')) ) {
	*idx = ':';
      }
    } else if (!strcmp(toka, "-s")) {
      toka = strtok_r(NULL, " ", &ptra);
      if (toka) snprintf(sourcecidr, 64, "%s", toka);
    } else if (!strcmp(toka, "-t")) {
      toka = strtok_r(NULL, " ", &ptra);
      if (toka) snprintf(icmptyperange, 64, "any");
    } else if (!strcmp(toka, "-o")) {
      toka = strtok_r(NULL, " ", &ptra);
      if (toka) snprintf(sourcegroup, 64, toka);
    } else if (!strcmp(toka, "-u")) {
      toka = strtok_r(NULL, " ", &ptra);
      if (toka) snprintf(sourceowner, 64, toka);
    }
    toka = strtok_r(NULL, " ", &ptra);
  }

  LOGDEBUG("PROTO: %s PORTRANGE: %s SOURCECIDR: %s ICMPTYPERANGE: %s SOURCEOWNER: %s SOURCEGROUP: %s\n", proto, portrange, sourcecidr, icmptyperange, sourceowner, sourcegroup);

  // check if enough info is present to construct rule
  if ( (strlen(sourcecidr) || (strlen(sourceowner) && strlen(sourcegroup))) && 
	strlen(proto) && 
	(strlen(portrange) || strlen(icmptyperange))
	) {
	 
    if (strlen(sourcecidr)) {
      snprintf(buf, 2048, "-s %s ", sourcecidr);
      strncat(newrule, buf, 2048);
    }
    if (strlen(sourceowner) && strlen(sourcegroup)) {
      char ug[64], *chainname=NULL;
      snprintf(ug, 64, "%s-%s", sourceowner, sourcegroup);
      hash_b64enc_string(ug, &chainname);
      snprintf(buf, 2048, "-m set --set %s src ", chainname);
      strncat(newrule, buf, 2048);
    }
    if (strlen(proto)) {
      snprintf(buf, 2048, "-p %s -m %s ", proto, proto);
      strncat(newrule, buf, 2048);
    }
    if (strlen(portrange)) {
      snprintf(buf, 2048, "--dport %s ", portrange);
      strncat(newrule, buf, 2048);
    }
    if (strlen(icmptyperange)) {
      snprintf(buf, 2048, "--icmp-type %s ", icmptyperange);
      strncat(newrule, buf, 2048);
    }
  
    snprintf(outrule, 2048, "%s", newrule);
    LOGDEBUG("CONVERTED RULE: %s\n", outrule);
  } else {
    LOGWARN("not enough information in RULE to construct iptables rule\n");
    ret=1;
  }
  
  return(ret);
}

void print_sec_groups(sec_group *newgroups, int max_newgroups) {
  int i, j;
  for (i=0; i<max_newgroups; i++) {
    printf("GROUPNAME: %s GROUPACCOUNTID: %s GROUPCHAINNAME: %s\n", newgroups[i].name, newgroups[i].accountId, newgroups[i].chainname);
    for (j=0; j<newgroups[i].max_member_ips; j++) {
      printf("\tIP MEMBER: %s\n", hex2dot(newgroups[i].member_ips[j]));
    }
    for (j=0; j<newgroups[i].max_grouprules; j++) {
      printf("\tRULE: %s\n", newgroups[i].grouprules[j]);
    }
  }
}

int check_stderr_already_exists(int rc, char *o, char *e) {
  if (!rc) return(0);
  if (e && strstr(e, "already exists")) return(0);
  return(1);
}
