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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pwd.h>

#include <eucalyptus.h>
#include <vnetwork.h>
#include <log.h>
#include <hash.h>
#include <math.h>
#include <http.h>
#include <config.h>
#include <sequence_executor.h>
#include <ipt_handler.h>

#include "eucanetd.h"
#include "config-eucanetd.h"

vnetConfig *vnetconfig = NULL;
eucanetdConfig *config = NULL;

int main (int argc, char **argv) {
  int rc=0;

  // initialize the logfile and config
  init_log();
  eucanetdInit();

  // parse commandline arguments  
  if (argv[1]) {
    config->ccIp = strdup(argv[1]);
    config->cc_cmdline_override = 1;
  }
  
  // initialize vnetconfig from local eucalyptus.conf and remote (CC) dynamic config; spin looking for config from CC until one is available
  vnetconfig = malloc(sizeof(vnetConfig));
  bzero(vnetconfig, sizeof(vnetConfig));  

  rc = 1;
  while(rc) {
    rc = read_config_cc();
    if (rc) {
      LOGWARN("cannot fetch latest initial config from CC (%s), waiting for config to become available\n", config->ccIp);
      sleep(1);
    }
  }

  rc = daemonize(1);
  if (rc) {
      fprintf(stderr, "failed to daemonize eucanetd, exiting\n");
      exit(1);
  }
  
  // initialize the nat chains
  //  rc = create_euca_edge_chains();
  //  if (rc) {
  //    LOGERROR("could not create euca chains\n");
  //  }

  // enter main loop
  int counter=0;
  while(counter<1000) {
    //  while(1) {
    int update_localnet = 0, update_networktopo = 0, update_cc_config = 0, update_clcip = 0, i;
    int update_localnet_failed = 0, update_networktopo_failed = 0, update_cc_config_failed = 0, update_clcip_failed = 0;

    counter++;

    rc = get_latest_ccIp(&(config->nc_localnetfile));
    if (rc) {
      LOGWARN("cannot get latest CCIP from NC generated ccIp_file(%s)\n", config->nc_localnetfile.dest);
    }

    update_localnet = update_networktopo = update_cc_config = update_clcip = 0;

    // attempt to fetch latest VM network information
    rc = fetch_latest_network(&update_networktopo, &update_cc_config, &update_localnet);
    if (rc) {
        LOGWARN("fetch_latest_network from CC failed\n");
    }
    // if the last update op failed, regardless of new info, try to apply again
    if (update_networktopo_failed) update_networktopo = 1;
    if (update_localnet_failed) update_localnet = 1;
    
    // regardless of fetch, read latest view of network info
    rc = read_latest_network();
    if (rc) {
        LOGWARN("read_latest_network failed, skipping update\n");
        update_localnet = update_networktopo = update_cc_config = update_clcip = 0;
    }
    
    // if an update is required, implement changes
    // TODO: implement CLC update check
    update_clcip = 1;
    if (update_clcip_failed) update_clcip = 1;
    
    // now, preform any updates that are required

    //temporary to force updates on each iteration
    update_networktopo = update_localnet = update_cc_config = update_clcip = 1;
    
    if (update_clcip) {
      // update metadata redirect rule
      update_clcip_failed = 0;
      rc = update_metadata_redirect();
      if (rc) {
        LOGERROR("could not update metadata redirect rule\n");
        update_clcip_failed = 1;
      }
    }

    if (update_networktopo) {
      // install iptables FW rules, using IPsets for sec. group 
      update_networktopo_failed = 0;
      rc = update_sec_groups();
      if (rc) {
        LOGERROR("could not complete update of security groups\n");
        update_networktopo_failed = 1;
      }
    }

    if (update_localnet) {
      LOGINFO("new networking state (pubprivmap): updating system\n");
      update_localnet_failed = 0;

      // update list of private IPs, handle DHCP daemon re-configure and restart
      rc = update_private_ips();
      if (rc) {
        LOGERROR("could not complete update of private IPs\n");
        update_localnet_failed = 1;
      }
      // update public IP assignment and NAT table entries
      rc = update_public_ips();
      if (rc) {
        LOGERROR("could not complete update of public IPs\n");
        update_localnet_failed = 1;
      }

      // install ebtables rules for isolation
      rc = update_isolation_rules();
      if (rc) {
        LOGERROR("could not complete update of VM network isolation rules\n");
        update_localnet_failed = 1;
      }
    }
    
    // temporary exit after one iteration
    //    exit(0);
    // do it all over again...
    sleep (config->cc_polling_frequency);
  }
  
  exit(0);
}

int daemonize(int foreground) {
    int pid, sid;
    struct passwd *pwent=NULL;
    char pidfile[MAX_PATH];
    FILE *FH=NULL;

    if (!foreground) {
        pid = fork();
        if (pid) {
            exit(0);
        }
    
        sid = setsid();
        if (sid < 0) {
            fprintf(stderr, "could not establish a new session id\n");
            perror("daemonize(): ");
            exit(1);
        }
    }    

    pid = getpid();
    if (pid > 1) {
        snprintf(pidfile, MAX_PATH, "%s/var/run/eucalyptus/eucalyptus-eucanetd.pid", config->eucahome);
        FH = fopen(pidfile, "w");
        if (FH) {
            fprintf(FH, "%d\n", pid);
            fclose(FH);
        } else {
            fprintf(stderr, "could not open pidfile for write (%s)\n", pidfile);
            exit(1);
        }
    }

    pwent = getpwnam(config->eucauser);
    if (!pwent) {
        fprintf(stderr, "could not find UID of configured user '%s'\n", SP(config->eucauser));
        perror("daemonize(): ");
        exit(1);
    }

    if ( setgid(pwent->pw_gid) || setuid(pwent->pw_uid) ) {
        fprintf(stderr, "could not switch daemon process to UID/GID '%d/%d'\n", pwent->pw_uid, pwent->pw_gid);
        perror("daemonize(): ");
        exit(1);
    }
    
    return(0);
}

int update_isolation_rules() {
  int rc, ret=0, i, fd, j, doit;
  char cmd[MAX_PATH];
  char *strptra=NULL, *strptrb=NULL;
  sec_group *group=NULL;

  rc = ebt_handler_repopulate(config->ebt);

  rc = ebt_table_add_chain(config->ebt, "filter", "euca-ebt-fwd", "ACCEPT", "");
  rc = ebt_chain_add_rule(config->ebt, "filter", "FORWARD", "-j euca-ebt-fwd");
  rc = ebt_chain_flush(config->ebt, "filter", "euca-ebt-fwd");

  // add these for DHCP to pass
  rc = ebt_chain_add_rule(config->ebt, "filter", "euca-ebt-fwd", "-p IPv4 -d Broadcast --ip-proto udp --ip-dport 67:68 -j ACCEPT");
  rc = ebt_chain_add_rule(config->ebt, "filter", "euca-ebt-fwd", "-p IPv4 -d Broadcast --ip-proto udp --ip-sport 67:68 -j ACCEPT");

  for (i=0; i<config->max_security_groups; i++) {
    group = &(config->security_groups[i]);
    for (j=0; j<group->max_member_ips; j++) {
        if (group->member_ips[j] && maczero(group->member_macs[j])) {
            strptra = strptrb = NULL;
            strptra = hex2dot(group->member_ips[j]);
            hex2mac(group->member_macs[j], &strptrb);
            snprintf(cmd, MAX_PATH, "-p IPv4 -s %s -i ! br0 --ip-src ! %s -j DROP", strptrb, strptra);
            rc = ebt_chain_add_rule(config->ebt, "filter", "euca-ebt-fwd", cmd);
            snprintf(cmd, MAX_PATH, "-p IPv4 -s ! %s -i ! br0 --ip-src %s -j DROP", strptrb, strptra);
            rc = ebt_chain_add_rule(config->ebt, "filter", "euca-ebt-fwd", cmd);
            EUCA_FREE(strptra);
            EUCA_FREE(strptrb);
            doit++;
        }
    }
  }

  rc = ebt_handler_print(config->ebt);
  rc = ebt_handler_deploy(config->ebt);
  if (rc) {
      LOGERROR("could not install ebtables rules\n");
      ret=1;
  }
  
  return(ret);  
}

int update_metadata_redirect() {
  int ret=0, rc;
  char rule[1024];
  
  if (ipt_handler_repopulate(config->ipt)) return(1);
  
  snprintf(rule, 1024, "-A PREROUTING -d 169.254.169.254/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", config->clcIp);
  if (ipt_chain_add_rule(config->ipt, "nat", "PREROUTING", rule)) return(1);
  
  rc = ipt_handler_deploy(config->ipt);
  if (rc) {
    LOGERROR("could not apply new rule (%s)\n", rule);
    ret=1;
  }
  
  return(ret);
}

int eucanetdInit() {
  int rc;
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
  char ips_file[MAX_PATH], *strptra=NULL;
  char cmd[MAX_PATH], clcmd[MAX_PATH], rule[1024];
  FILE *FH=NULL;
  sequence_executor cmds;
  sec_group *group=NULL;

  ret=0;

  rc = ipt_handler_repopulate(config->ipt); 
  if (rc) {
      LOGERROR("cannot read current IPT rules\n");
      return(1);
  }
  
  rc = ips_handler_repopulate(config->ips);
  if (rc) {
      LOGERROR("cannot read current IPS sets\n");
      return(1);
  }
  
  // make sure euca chains are in place
  ipt_table_add_chain(config->ipt, "filter", "euca-ipsets-fwd", "-", "[0:0]");
  if (ipt_chain_add_rule(config->ipt, "filter", "FORWARD", "-A FORWARD -j euca-ipsets-fwd")) return(1);
  if (ipt_chain_add_rule(config->ipt, "filter", "FORWARD", "-A FORWARD -m conntrack --ctstate ESTABLISHED -j ACCEPT")) return(1);

  // clear all chains that we're about to (re)populate with latest network metadata
  rc = ipt_table_deletechainmatch(config->ipt, "filter", "eu-");

  rc = ipt_chain_flush(config->ipt, "filter", "euca-ipsets-fwd");
  
  rc = ips_handler_deletesetmatch(config->ips, "eu-");

  // add chains/rules
  for (i=0; i<config->max_security_groups; i++) {
    group = &(config->security_groups[i]);
    rule[0] = '\0';

    ips_handler_add_set(config->ips, group->chainname);
    ips_set_flush(config->ips, group->chainname);

    strptra = hex2dot(config->defaultgw);
    ips_set_add_ip(config->ips, group->chainname, strptra);
    EUCA_FREE(strptra);
    for (j=0; j<group->max_member_ips; j++) {
        if (group->member_ips[j]) {
            strptra = hex2dot(group->member_ips[j]);
            ips_set_add_ip(config->ips, group->chainname, strptra);
            EUCA_FREE(strptra);
        }
      }

    // add forward chain
    ipt_table_add_chain(config->ipt, "filter", group->chainname, "-", "[0:0]");
    ipt_chain_flush(config->ipt, "filter", group->chainname);
    
    // add jump rule
    snprintf(rule, 1024, "-A euca-ipsets-fwd -m set --match-set %s dst -j %s", group->chainname, group->chainname);
    ipt_chain_add_rule(config->ipt, "filter", "euca-ipsets-fwd", rule);
    
    // populate forward chain
    // this one needs to be first
    snprintf(rule, 1024, "-A %s -m set --match-set %s src,dst -j ACCEPT", group->chainname, group->chainname);
    ipt_chain_add_rule(config->ipt, "filter", group->chainname, rule);
        
    // then put all the group specific IPT rules (temporary one here)
    if (group->max_grouprules) {
      for (j=0; j<group->max_grouprules; j++) {
        snprintf(rule, 1024, "-A %s %s -j ACCEPT", group->chainname, group->grouprules[j]);
        ipt_chain_add_rule(config->ipt, "filter", group->chainname, rule);
      }
    }
    
    snprintf(rule, 1024, "-A %s -m conntrack --ctstate ESTABLISHED -j ACCEPT", group->chainname);
    ipt_chain_add_rule(config->ipt, "filter", group->chainname, rule);    
    
    // this ones needs to be last
    snprintf(rule, 1024, "-A %s -j DROP", group->chainname);
    ipt_chain_add_rule(config->ipt, "filter", group->chainname, rule);
    
  }
  
  if (1 || !ret) {
      ips_handler_print(config->ips);
      rc = ips_handler_deploy(config->ips, 0);
      if (rc) {
          LOGERROR("could not apply ipsets\n");
          ret = 1;
      }
  }

  if (1 || !ret) {
      ipt_handler_print(config->ipt);
      rc = ipt_handler_deploy(config->ipt);
      if (rc) {
          LOGERROR("could not apply new rules\n");
          ret=1;
      }
  }

  if (1 || !ret) {
      ips_handler_print(config->ips);
      rc = ips_handler_deploy(config->ips, 1);
      if (rc) {
          LOGERROR("could not apply ipsets\n");
          ret = 1;
      }
  }
  return(ret);
}

sec_group *find_sec_group_bypriv(sec_group *groups, int max_groups, u32 privip, int *outfoundidx) {
    int i, j, rc=0, found=0, foundgidx=0, foundipidx=0;
    
    if (!groups || max_groups <= 0 || !privip) {
        return(NULL);
    }
    
    for (i=0; i<max_groups && !found; i++) {
        for (j=0; j<groups[i].max_member_ips && !found; j++) {
            if (groups[i].member_ips[j] == privip) {
                foundgidx = i;
                foundipidx = j;
                found++;
            }
        }
    }
    if (found) {
        if (outfoundidx) {
            *outfoundidx = foundipidx;
        }
        return(&(groups[foundgidx]));
    }
    return(NULL);
}

sec_group *find_sec_group_bypub(sec_group *groups, int max_groups, u32 pubip, int *outfoundidx) {
    int i, j, rc=0, found=0, foundgidx=0, foundipidx=0;
    
    if (!groups || max_groups <= 0 || !pubip) {
        return(NULL);
    }
    
    for (i=0; i<max_groups && !found; i++) {
        for (j=0; j<groups[i].max_member_ips && !found; j++) {
            if (groups[i].member_public_ips[j] == pubip) {
                foundgidx = i;
                foundipidx = j;
                found++;
            }
        }
    }
    if (found) {
        if (outfoundidx) {
            *outfoundidx = foundipidx;
        }
        return(&(groups[foundgidx]));
    }
    return(NULL);
}

int update_public_ips() {
    int slashnet, ret=0, rc, i, j, foundidx, doit;
  char cmd[MAX_PATH], clcmd[MAX_PATH], rule[1024];
  char *strptra=NULL, *strptrb=NULL;
  sequence_executor cmds;
  sec_group *group;
  

  // install EL IP addrs and NAT rules
  rc = ipt_handler_repopulate(config->ipt);
  ipt_table_add_chain(config->ipt, "nat", "euca-edge-nat-pre", "-", "[0:0]");
  ipt_table_add_chain(config->ipt, "nat", "euca-edge-nat-post", "-", "[0:0]");
  ipt_table_add_chain(config->ipt, "nat", "euca-edge-nat-out", "-", "[0:0]");

  ipt_chain_add_rule(config->ipt, "nat", "PREROUTING", "-A PREROUTING -j euca-edge-nat-pre");
  ipt_chain_add_rule(config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j euca-edge-nat-post");
  ipt_chain_add_rule(config->ipt, "nat", "OUTPUT", "-A OUTPUT -j euca-edge-nat-out");
  
  ipt_chain_flush(config->ipt, "nat", "euca-edge-nat-pre");
  ipt_chain_flush(config->ipt, "nat", "euca-edge-nat-post");
  ipt_chain_flush(config->ipt, "nat", "euca-edge-nat-out");

  rc = ipt_handler_deploy(config->ipt);
  if (rc) {
      LOGERROR("could not add euca net chains\n");
      ret=1;
  }

  /*
  rc = flush_euca_edge_chains();
  if (rc) {
    LOGERROR("failed to flush table euca-edge-nat\n");
    return(1);
  }
  */
  
  slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - vnetconfig->networks[0].nm) + 1))));
  
  for (i=0; i<config->max_security_groups; i++) {
      group = &(config->security_groups[i]);
      for (j=0; j<group->max_member_ips; j++) {
          doit = 0;

          rc = ipt_handler_repopulate(config->ipt);
          rc = se_init(&cmds, config->cmdprefix, 2, 1);

          strptra = hex2dot(group->member_public_ips[j]);
          strptrb = hex2dot(group->member_ips[j]);
          if ((group->member_public_ips[j] && group->member_ips[j]) && (group->member_public_ips[j] != group->member_ips[j])) {
              snprintf(cmd, MAX_PATH, "ip addr add %s/%d dev %s >/dev/null 2>&1", strptra, slashnet, vnetconfig->pubInterface);
              rc = se_add(&cmds, cmd, NULL, ignore_exit2);
              
              snprintf(rule, 1024, "-A euca-edge-nat-pre -d %s/32 -j DNAT --to-destination %s", strptra, strptrb);
              rc = ipt_chain_add_rule(config->ipt, "nat", "euca-edge-nat-pre", rule);
              
              snprintf(rule, 1024, "-A euca-edge-nat-out -d %s/32 -j DNAT --to-destination %s", strptra, strptrb);
              rc = ipt_chain_add_rule(config->ipt, "nat", "euca-edge-nat-out", rule);
      
              snprintf(rule, 1024, "-A euca-edge-nat-post -s %s/32 -m set ! --match-set %s dst -j SNAT --to-source %s", strptrb, group->chainname, strptra);
              rc = ipt_chain_add_rule(config->ipt, "nat", "euca-edge-nat-post", rule);      

              // actually added some stuff to do
              doit++;
          }

          if (doit) {
              se_print(&cmds);
              rc = se_execute(&cmds);
              if (rc) {
                  LOGERROR("could not execute command sequence 1\n");
                  se_print(&cmds);
                  ret=1;
              }
              se_free(&cmds);
              
              rc = ipt_handler_deploy(config->ipt);
              if (rc) {
                  LOGERROR("could not apply new rules\n");
                  ret=1;
              }
          }
    
          EUCA_FREE(strptra);
          EUCA_FREE(strptrb);
      }
  }

  // if all has gone well, now clear any public IPs that have not been mapped to private IPs
  if (!ret) {
      se_init(&cmds, config->cmdprefix, 2, 1);
      for (i=0; i<config->max_all_public_ips; i++) {
          group = find_sec_group_bypub(config->security_groups, config->max_security_groups, config->all_public_ips[i], &foundidx);
          if (!group) {
              strptra = hex2dot(config->all_public_ips[i]);
              snprintf(cmd, MAX_PATH, "ip addr del %s/%d dev %s >/dev/null 2>&1", strptra, slashnet, vnetconfig->pubInterface);
              rc = se_add(&cmds, cmd, NULL, ignore_exit2);
              EUCA_FREE(strptra);
          }
      }
      se_print(&cmds);
      rc = se_execute(&cmds);
      if (rc) {
          LOGERROR("could not execute command sequence 2\n");
          ret = 1;
      }
      se_free(&cmds);
  }
  return(ret);
}

int update_private_ips() {
  int ret=0, rc, i, j;
  char mac[32], *strptra=NULL, *strptrb=NULL;
  sec_group *group=NULL;

  bzero(mac, 32);
  // populate vnetconfig with new info
  for (i=0; i<config->max_security_groups; i++) {
      group = &(config->security_groups[i]);
      for (j=0; j<group->max_member_ips; j++) {
          strptra = hex2dot(group->member_public_ips[j]);
          strptrb = hex2dot(group->member_ips[j]);
          if (group->member_ips[j]) {
              LOGINFO("adding ip: %s\n", strptrb);
              rc = vnetAddPrivateIP(vnetconfig, strptrb);
              if (rc) {
                  LOGERROR("could not add private IP '%s'\n", strptrb);
                  ret=1;
              } else {
                  vnetGenerateNetworkParams(vnetconfig, "", 0, -1, mac, strptra, strptrb);
              }
          }
          EUCA_FREE(strptra);
          EUCA_FREE(strptrb);
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

int get_latest_ccIp(atomic_file *file) {
  int ret, done;
  char ccIp[64], buf[1024];
  FILE *FH=NULL;
  
  ret=done=0;
  ccIp[0] = '\0';    

  if (config->cc_cmdline_override) {
    return(0);
  }

  FH =fopen(file->dest, "r");  
  if (FH) {
    while (fgets(buf, 1024, FH) && !done) {
      LOGTRACE("line: %s\n", SP(buf));
      if (strstr(buf, "CCIP=")) {
        sscanf(buf, "CCIP=%[0-9.]", ccIp);
        LOGDEBUG("parsed line from file(%s): ccIp=%s\n", SP(file->dest), ccIp);
        if (strlen(ccIp) && strcmp(ccIp, "0.0.0.0")) {
          done++;
        } else {
          LOGWARN("malformed ccIp entry in file, skipping: %s\n", SP(buf));
        }
      }
    }
    if (!strlen(ccIp)) {
      LOGERROR("could not find valid CCIP=<ccip> in file(%s)\n", SP(file->dest));
      ret=1;
    } else {
      if (config->ccIp) EUCA_FREE(config->ccIp);
      config->ccIp = strdup(ccIp);
    }
    fclose(FH);
  } else {
    LOGERROR("could not open file(%s)\n", SP(file->dest));
    ret=1;
  }
  return(ret);
}

int read_config_cc() {
  char configFiles[2][MAX_PATH];
  char *tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME), home[MAX_PATH], url[MAX_PATH], netPath[MAX_PATH], destfile[MAX_PATH], sourceuri[MAX_PATH];
  char *cvals[EUCANETD_CVAL_LAST];
  int fd, rc, ret, i, to_update=0;
  
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

  snprintf(netPath, MAX_PATH, CC_NET_PATH_DEFAULT, home);

  snprintf(destfile, MAX_PATH, "%s/var/lib/eucalyptus/eucanetd_nc_local_net_file", home);
  snprintf(sourceuri, MAX_PATH, "file://" EUCALYPTUS_LOG_DIR "/local-net", home);
  atomic_file_init(&(config->nc_localnetfile), sourceuri, destfile);
  
  rc = atomic_file_get(&(config->nc_localnetfile), &to_update);
  if (rc) {
      LOGWARN("cannot get latest CCIP from NC generated file(%s)\n", config->nc_localnetfile.dest);
      for (i=0; i<EUCANETD_CVAL_LAST; i++) {
          EUCA_FREE(cvals[i]);
      }
      return(1);
  }
  
  rc = get_latest_ccIp(&(config->nc_localnetfile));
  if (rc) {
    LOGWARN("cannot get latest CCIP from NC generated file(%s)\n", config->nc_localnetfile.dest);
    for (i=0; i<EUCANETD_CVAL_LAST; i++) {
      EUCA_FREE(cvals[i]);
    }
    return(1);
  }

  snprintf(destfile, MAX_PATH, "%s/var/lib/eucalyptus/eucanetd_cc_config_file", home);
  snprintf(sourceuri, MAX_PATH, "http://%s:8776/config-cc", SP(config->ccIp));
  atomic_file_init(&(config->cc_configfile), sourceuri, destfile);
  
  rc = atomic_file_get(&(config->cc_configfile), &to_update);
  if (rc) {
      LOGWARN("cannot fetch config file from CC(%s)\n", config->ccIp);
      for (i=0; i<EUCANETD_CVAL_LAST; i++) {
          EUCA_FREE(cvals[i]);
      }
      return(1);
  }

  snprintf(destfile, MAX_PATH, "%s/var/lib/eucalyptus/eucanetd_cc_network_topology_file", home);
  snprintf(sourceuri, MAX_PATH, "http://%s:8776/network-topology", SP(config->ccIp));
  atomic_file_init(&(config->cc_networktopofile), sourceuri, destfile);
  
  snprintf(configFiles[0], MAX_PATH, EUCALYPTUS_CONF_LOCATION, home);
  snprintf(configFiles[1], MAX_PATH, "%s", config->cc_configfile.dest);

  configInitValues(configKeysRestartEUCANETD, configKeysNoRestartEUCANETD);   // initialize config subsystem
  readConfigFile(configFiles, 2);
  
  // thing to read from the NC config file
  cvals[EUCANETD_CVAL_PUBINTERFACE] = configFileValue("VNET_PUBINTERFACE");
  cvals[EUCANETD_CVAL_PRIVINTERFACE] = configFileValue("VNET_PRIVINTERFACE");
  cvals[EUCANETD_CVAL_BRIDGE] = configFileValue("VNET_BRIDGE");
  cvals[EUCANETD_CVAL_EUCAHOME] = configFileValue("EUCALYPTUS");
  cvals[EUCANETD_CVAL_MODE] = configFileValue("VNET_MODE");
  cvals[EUCANETD_CVAL_EUCA_USER] = configFileValue("EUCA_USER");
  
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
  config->eucahome = strdup(cvals[EUCANETD_CVAL_EUCAHOME]);
  config->eucauser = strdup(cvals[EUCANETD_CVAL_EUCA_USER]);
  snprintf(config->cmdprefix, MAX_PATH, EUCALYPTUS_ROOTWRAP, config->eucahome);
  config->cc_polling_frequency = atoi(cvals[EUCANETD_CVAL_CC_POLLING_FREQUENCY]);
  config->defaultgw = dot2hex(cvals[EUCANETD_CVAL_ROUTER]);

  config->ipt = malloc(sizeof(ipt_handler));
  rc = ipt_handler_init(config->ipt, config->cmdprefix);
  if (rc) {
      LOGFATAL("could not initialize ipt_handler\n");
      ret=1;
  }

  config->ips = malloc(sizeof(ips_handler));
  rc = ips_handler_init(config->ips, config->cmdprefix);
  if (rc) {
      LOGFATAL("could not initialize ips_handler\n");
      ret=1;
  }

  config->ebt = malloc(sizeof(ebt_handler));
  rc = ebt_handler_init(config->ebt, config->cmdprefix);
  if (rc) {
      LOGFATAL("could not initialize ebt_handler\n");
      ret=1;
  }
  
  for (i=0; i<EUCANETD_CVAL_LAST; i++) {
    EUCA_FREE(cvals[i]);
  }

  return(ret);
  
}

int flush_euca_edge_chains() {
  int rc, ret=0;
  
  if (ipt_handler_repopulate(config->ipt)) return(1);
  
  if (ipt_chain_flush(config->ipt, "nat", "euca-edge-nat-pre")) return(1);
  if (ipt_chain_flush(config->ipt, "nat", "euca-edge-nat-post")) return(1);
  if (ipt_chain_flush(config->ipt, "nat", "euca-edge-nat-out")) return(1);
  
  rc = ipt_handler_deploy(config->ipt);
  if (rc) {
    LOGERROR("could not apply new rules\n");
    ret=1;
  }
  
  return(ret);
}

int create_euca_edge_chains() {
  int rc, ret=0, fd;
  char cmd[MAX_PATH];
  FILE *FH=NULL;
  
  if (ipt_handler_repopulate(config->ipt)) return(1);
  if (ipt_table_add_chain(config->ipt, "filter", "euca-ipsets-in", "-", "[0:0]")) return(1);
  if (ipt_table_add_chain(config->ipt, "filter", "euca-ipsets-fwd", "-", "[0:0]")) return(1);
  if (ipt_table_add_chain(config->ipt, "filter", "euca-ipsets-out", "-", "[0:0]")) return(1);
  if (ipt_table_add_chain(config->ipt, "nat", "euca-edge-nat-pre", "-", "[0:0]")) return(1);
  if (ipt_table_add_chain(config->ipt, "nat", "euca-edge-nat-post", "-", "[0:0]")) return(1);
  if (ipt_table_add_chain(config->ipt, "nat", "euca-edge-nat-out", "-", "[0:0]")) return(1);
  if (ipt_chain_add_rule(config->ipt, "filter", "INPUT", "-A INPUT -j euca-ipsets-in")) return(1);
  if (ipt_chain_add_rule(config->ipt, "filter", "FORWARD", "-A FORWARD -j euca-ipsets-fwd")) return(1);
  if (ipt_chain_add_rule(config->ipt, "filter", "OUTPUT", "-A OUTPUT -j euca-ipsets-out")) return(1);
  if (ipt_chain_add_rule(config->ipt, "filter", "FORWARD", "-A FORWARD -m conntrack --ctstate ESTABLISHED -j ACCEPT")) return(1);
  if (ipt_chain_add_rule(config->ipt, "nat", "PREROUTING", "-A PREROUTING -j euca-edge-nat-pre")) return(1);
  if (ipt_chain_add_rule(config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j euca-edge-nat-post")) return(1);
  if (ipt_chain_add_rule(config->ipt, "nat", "OUTPUT", "-A OUTPUT -j euca-edge-nat-out")) return(1);
  
  ipt_handler_print(config->ipt);

  rc = ipt_handler_deploy(config->ipt);
  if (rc) {
    LOGERROR("could not apply new rules\n");
    ret=1;
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

int read_latest_network() {
  int rc, ret=0;

  rc = parse_network_topology(config->cc_networktopofile.dest);
  if (rc) {
    LOGERROR("cannot parse network-topology file (%s)\n", config->cc_networktopofile.dest);
    ret=1;
  }

  rc = parse_pubprivmap(config->nc_localnetfile.dest);
  if (rc) {
    LOGERROR("cannot parse pubprivmap file (%s)\n", config->nc_localnetfile.dest);
    ret=1;
  }

  print_sec_groups(config->security_groups, config->max_security_groups);

  rc = parse_ccpubprivmap(config->cc_configfile.dest);
  if (rc) {
    LOGERROR("cannot parse pubprivmap file (%s)\n", config->cc_configfile.dest);
    ret=1;
  }

  return(ret);
}

int parse_ccpubprivmap(char *cc_configfile) {
    int ret=0;
    char pub[64], priv[64], buf[1024];
    FILE *FH;
    
    config->max_all_public_ips = 0;
    
    FH=fopen(cc_configfile, "r");
    if (FH) {
        while(fgets(buf, 1024, FH)) {
            pub[0] = priv[0] = '\0';
            if (strstr(buf, "IPMAP=")) {
                sscanf(buf, "IPMAP=%[0-9.] %[0-9.]", pub, priv);
                if (strlen(pub)) {
                    config->all_public_ips[config->max_all_public_ips] = dot2hex(pub);
                    config->max_all_public_ips++;
                }
            }
        }
        fclose(FH);
    } else {
        LOGERROR("could not open file (%s)\n", cc_configfile);
        ret=1;
    }
    
    return(ret);
}

int parse_pubprivmap(char *pubprivmap_file) {
  char buf[1024], priv[64], pub[64], mac[64], instid[64], bridgedev[64], tmp[64], vlan[64], ccIp[64];
  int count=0, ret=0, foundidx=0;
  FILE *FH = NULL;
  sec_group *group=NULL;
  
  FH =fopen(pubprivmap_file, "r");  
  if (FH) {
    while (fgets(buf, 1024, FH)) {
      priv[0] = pub[0] = ccIp[0] = '\0';
      
      if (strstr(buf, "CCIP=")) {
      } else {
        sscanf(buf, "%s %s %s %s %s %[0-9.] %[0-9.]", instid, bridgedev, tmp, vlan, mac, pub, priv);
        LOGDEBUG("parsed line from local pubprivmapfile: instId=%s bridgedev=%s NA=%s vlan=%s mac=%s pub=%s priv=%s\n", instid, bridgedev, tmp, vlan, mac, pub, priv);
        if ( (strlen(priv) && strlen(pub)) && !(!strcmp(priv, "0.0.0.0") && !strcmp(pub, "0.0.0.0")) ) {
            group = find_sec_group_bypriv(config->security_groups, config->max_security_groups, dot2hex(priv), &foundidx);
            if (!group) {
                LOGDEBUG("group is null\n");
            }
            if (group && (foundidx >= 0)) {
                group->member_public_ips[foundidx] = dot2hex(pub);
                mac2hex(mac, group->member_macs[foundidx]);
            }
        }
      }
    }
    fclose(FH);
  } else {
    LOGERROR("could not open map file for read (%s)\n", pubprivmap_file);
    ret=1;
  }
  print_sec_groups(config->security_groups, config->max_security_groups);
  return(ret);
}

#if 0
int parse_pubprivmap_cc(char *pubprivmap_file) {
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
#endif

int fetch_latest_network(int *update_networktopo, int *update_cc_config, int *update_localnet) {
    int rc=0, ret=0;

    rc = atomic_file_get(&(config->cc_networktopofile), update_networktopo);
    if (rc) {
        LOGWARN("could not fetch latest network topology from CC\n");
        ret = 1;
    } 

    rc = atomic_file_get(&(config->cc_configfile), update_cc_config);
    if (rc) {
        LOGWARN("could not fetch latest configuration from CC\n");
        ret = 1;
    } 
    
    rc = atomic_file_get(&(config->nc_localnetfile), update_localnet);
    if (rc) {
        LOGWARN("could not fetch latest local network info from NC\n");
        ret = 1;
    } 
    
    return(ret);
}

int parse_network_topology(char *file) {
  int ret=0, rc, gidx, i;
  FILE *FH=NULL;
  char buf[MAX_PATH], rulebuf[2048], newrule[2048];
  char *toka=NULL, *ptra=NULL, *modetok=NULL, *grouptok=NULL, chainname[32], *chainhash;
  sec_group *newgroups=NULL, *group=NULL;
  int max_newgroups=0, curr_group=0;
  u32 newip=0;

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
          hash_b64enc_string(grouptok, &chainhash);
          if (chainhash) {
            snprintf(newgroups[curr_group].chainname, 32, "eu-%s", chainhash);
            EUCA_FREE(chainhash);
          }

          toka = strtok_r(NULL, " ", &ptra);
          while(toka) {
            newip = dot2hex(toka);
            if (newip) {
                newgroups[curr_group].member_ips[newgroups[curr_group].max_member_ips] = dot2hex(toka);
                newgroups[curr_group].member_public_ips[newgroups[curr_group].max_member_ips] = 0;
                newgroups[curr_group].max_member_ips++;
            }
            toka = strtok_r(NULL, " ", &ptra);
          }
        }
      }
    }
    fclose(FH);
  }
  
  if (ret == 0) {
    int i, j;
    for (i=0; i<config->max_security_groups; i++) {
      group = &(config->security_groups[i]);
      for (j=0; j<group->max_grouprules; j++) {
        if (group->grouprules[j]) EUCA_FREE(group->grouprules[j]);
      }
    }
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
            gidx=-1;
            hash_b64enc_string(grouptok, &chainhash);
            if (chainhash) {
              snprintf(chainname, 32, "eu-%s", chainhash);
              for (i=0; i<config->max_security_groups; i++) {
                group = &(config->security_groups[i]);
                if (!strcmp(group->chainname, chainname)) {
                  gidx=i;
                  break;
                }
              }
              EUCA_FREE(chainhash);
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
        char minport[64], maxport[64];
        sscanf(portrange, "%[0-9]-%[0-9]", minport, maxport);
        if (!strcmp(minport, maxport)) {
          snprintf(portrange, 64, "%s", minport);
        } else {
          *idx = ':';
        }
      }
    } else if (!strcmp(toka, "-s")) {
      toka = strtok_r(NULL, " ", &ptra);
      if (toka) snprintf(sourcecidr, 64, "%s", toka);
      if (!strcmp(sourcecidr, "0.0.0.0/0")) {
        sourcecidr[0] = '\0';
      }
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
  if ( strlen(proto) && (strlen(portrange) || strlen(icmptyperange)) ) {	 
    if (strlen(sourcecidr)) {
      snprintf(buf, 2048, "-s %s ", sourcecidr);
      strncat(newrule, buf, 2048);
    }
    if (strlen(sourceowner) && strlen(sourcegroup)) {
      char ug[64], *chainhash=NULL;
      snprintf(ug, 64, "%s-%s", sourceowner, sourcegroup);
      hash_b64enc_string(ug, &chainhash);
      if (chainhash) {
        snprintf(buf, 2048, "-m set --set eu-%s src ", chainhash);
        strncat(newrule, buf, 2048);
        EUCA_FREE(chainhash);
      }
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

    while(newrule[strlen(newrule)-1] == ' ') {
      newrule[strlen(newrule)-1] = '\0';
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
  char *strptra=NULL, *strptrb=NULL;

  for (i=0; i<max_newgroups; i++) {
    LOGDEBUG("GROUPNAME: %s GROUPACCOUNTID: %s GROUPCHAINNAME: %s\n", newgroups[i].name, newgroups[i].accountId, newgroups[i].chainname);
    for (j=0; j<newgroups[i].max_member_ips; j++) {
      strptra = hex2dot(newgroups[i].member_ips[j]);
      strptrb = hex2dot(newgroups[i].member_public_ips[j]);
      LOGDEBUG("\tIP MEMBER: %s (%s)\n", strptra, strptrb);
      EUCA_FREE(strptra);
      EUCA_FREE(strptrb);
    }
    for (j=0; j<newgroups[i].max_grouprules; j++) {
      LOGDEBUG("\tRULE: %s\n", newgroups[i].grouprules[j]);
    }
  }
}

int check_stderr_already_exists(int rc, char *o, char *e) {
  if (!rc) return(0);
  if (e && strstr(e, "already exists")) return(0);
  return(1);
}

int atomic_file_init(atomic_file *file, char *source, char *dest) {
    if (!file) {
        return(1);
    }

    atomic_file_free(file);
    
    snprintf(file->dest, MAX_PATH, "%s", dest);
    snprintf(file->source, MAX_PATH, "%s", source);
    snprintf(file->tmpfilebase, MAX_PATH, "%s-XXXXXX", dest);
    snprintf(file->tmpfile, MAX_PATH, "%s-XXXXXX", dest);
    file->lasthash = strdup("UNSET");
    file->currhash = strdup("UNSET");
    return(0);
}

int atomic_file_get(atomic_file *file, int *file_updated) {
    char type[32], hostname[512], path[MAX_PATH], tmpsource[MAX_PATH], tmppath[MAX_PATH];
    int port, fd, ret, rc;
    
    if (!file || !file_updated) {
        return(1);
    }

    ret=0;
    *file_updated = 0;

    snprintf(file->tmpfile, MAX_PATH, "%s", file->tmpfilebase);
    fd = safe_mkstemp(file->tmpfile);
    if (fd < 0) {
        LOGERROR("cannot open tmpfile '%s'\n", file->tmpfile);
        return (1);
    }
    chmod(file->tmpfile, 0644);
    close(fd);

    snprintf(tmpsource, MAX_PATH, "%s", file->source);
    type[0] = tmppath[0] = path[0] = hostname[0] = '\0';
    port = 0;
    
    tokenize_uri(tmpsource, type, hostname, &port, tmppath);
    snprintf(path, MAX_PATH, "/%s", tmppath);

    if (!strcmp(type, "http")) {
        rc = http_get_timeout(file->source, file->tmpfile, 0, 0, 10, 15);
        if (rc) {
            LOGERROR("http client failed to fetch file URL=%s\n", file->source);
            ret=1;
        }
    } else if (!strcmp(type, "file")) {
        if (!strlen(path) || copy_file(path, file->tmpfile)) {
            LOGERROR("could not rename source file (%s) to dest file (%s)\n", path, file->tmpfile);
            ret=1;
        }
    } else {
        LOGWARN("incompatible URI type (only support http, file): (%s)\n", type);
        ret=1;
    }
    
    if (!ret) {
        char *hash=NULL;
        // do checksum - only copy if file has changed
        hash = file2md5str(file->tmpfile);
        if (!hash) {
            LOGERROR("could not compute hash of tmpfile (%s)\n", file->tmpfile);
            ret = 1;
        } else {
            if (file->currhash) EUCA_FREE(file->currhash);
            file->currhash = hash;
            if (strcmp(file->currhash, file->lasthash)) {
                // hashes are different, put new file in place
                LOGDEBUG("renaming file %s -> %s\n", file->tmpfile, file->dest);
                if (rename(file->tmpfile, file->dest)) {
                    LOGERROR("could not rename local copy to dest (%s -> %s)\n", file->tmpfile, file->dest);
                    ret=1;
                } else {
                    EUCA_FREE(file->lasthash);
                    file->lasthash = strdup(file->currhash);
                    *file_updated = 1;
                }
            }
        }
    }
    
    unlink(file->tmpfile);
    return(ret);
}

int atomic_file_free(atomic_file *file) {
    if (!file) return(1);
    if (file->lasthash) EUCA_FREE(file->lasthash);
    if (file->currhash) EUCA_FREE(file->currhash);
    bzero(file, sizeof(atomic_file));
    return(0);
}
