#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

#include <eucalyptus.h>
#include <log.h>

#include "ipt_handler.h"

int ipt_handler_init(ipt_handler *ipth) {
  int fd;
  if (!ipth) {
    return(1);
  }
  bzero(ipth, sizeof(ipt_handler));
  
  snprintf(ipth->ipt_file, MAX_PATH, "/tmp/ipt_file-XXXXXX");
  fd = safe_mkstemp(ipth->ipt_file);
  if (fd < 0) {
    LOGERROR("cannot open ipt_file '%s'\n", ipth->ipt_file);
    return (1);
  }
  chmod(ipth->ipt_file, 0644);
  close(fd);
  
  ipth->init = 1;
  return(0);
}

int ipt_system_save(ipt_handler *ipth) {
  int rc, fd;
  char cmd[MAX_PATH];
  
  snprintf(cmd, MAX_PATH, "iptables-save > %s", ipth->ipt_file);
  rc = system(cmd);
  rc = rc>>8;
  if (rc) {
    LOGERROR("failed to execute iptables-save\n");
  }
  return(rc);
}

int ipt_system_restore(ipt_handler *ipth) {
  int rc;
  char cmd[MAX_PATH];
    
  snprintf(cmd, MAX_PATH, "iptables-restore -c < %s", ipth->ipt_file);
  rc = system(cmd);
  rc = rc>>8;
  if (rc) {
    LOGERROR("failed to execute iptables-restore\n");
  }
  return(rc);
}

int ipt_handler_deploy(ipt_handler *ipth) {
  int i, j, k;
  FILE *FH=NULL;
  if (!ipth || !ipth->init) {
    return(1);
  }
  
  FH=fopen(ipth->ipt_file, "w");
  if (!FH) {
    LOGERROR("could not open file for write(%s)\n", ipth->ipt_file);
    return(1);
  }
  for (i=0; i<ipth->max_tables; i++) {
    fprintf(FH, "*%s\n", ipth->tables[i].name);
    for (j=0; j<ipth->tables[i].max_chains; j++) {
      fprintf(FH, ":%s %s %s\n", ipth->tables[i].chains[j].name, ipth->tables[i].chains[j].policyname, ipth->tables[i].chains[j].counters);
    }
    for (j=0; j<ipth->tables[i].max_chains; j++) {
      for (k=0; k<ipth->tables[i].chains[j].max_rules; k++) {
	fprintf(FH, "%s\n", ipth->tables[i].chains[j].rules[k].iptrule);
      }
    }
    fprintf(FH, "COMMIT\n");
  }
  fclose(FH);
  
  return(ipt_system_restore(ipth));
}

int ipt_handler_repopulate(ipt_handler *ipth) {
  int i, rc;
  FILE *FH=NULL;
  char buf[1024], tmpbuf[1024], *strptr=NULL;
  char tablename[64], chainname[64], policyname[64], counters[64];

  if (!ipth || !ipth->init) {
    return(1);
  }
      
  rc = ipt_handler_free(ipth);
  if (rc) {
    return(1);
  }

  rc = ipt_system_save(ipth);
  if (rc) {
    LOGERROR("could not save current IPT rules to file, skipping re-populate\n");
    return(1);
  }
  
  FH=fopen(ipth->ipt_file, "r");
  if (!FH) {
    LOGERROR("could not open file for read(%s)\n", ipth->ipt_file);
    return(1);
  }
    
  while (fgets(buf, 1024, FH)) {
    if ( (strptr = strchr(buf, '\n')) ) {
      *strptr = '\0';
    }

    if (strlen(buf) < 1) {
      continue;
    }
    
    while(buf[strlen(buf)-1] == ' ') {
      buf[strlen(buf)-1] = '\0';
    }

    if (buf[0] == '*') {
      tablename[0] = '\0';
      sscanf(buf, "%[*]%s", tmpbuf, tablename);
      if (strlen(tablename)) {
	ipt_handler_add_table(ipth, tablename);
      }
    } else if (buf[0] == ':') {
      chainname[0] = '\0';
      sscanf(buf, "%[:]%s %s %s", tmpbuf, chainname, policyname, counters);
      if (strlen(chainname)) {
	ipt_table_add_chain(ipth, tablename, chainname, policyname, counters);
      }
    } else if (strstr(buf, "COMMIT")) {
    } else if (buf[0] == '#') {
    } else if (buf[0] == '-' && buf[1] == 'A') {
      sscanf(buf, "%[-A] %s", tmpbuf, chainname);
      ipt_chain_add_rule(ipth, tablename, chainname, buf);
    } else {
      LOGWARN("unknown IPT rule on ingress, will be thrown out: (%s)\n", buf);
    }
  }
  fclose(FH);
  
  return(0);
}
int ipt_handler_add_table(ipt_handler *ipth, char *tablename) {
  int found=0, tableidx=0, i;
  if (!ipth || !tablename || !ipth->init) {
    return(1);
  }
  
  found=0;
  for (i=0; i<ipth->max_tables && !found; i++) {
    tableidx=i;
    if (!strcmp(ipth->tables[i].name, tablename)) found++;
  }

  if (!found) {
    ipth->tables = realloc(ipth->tables, sizeof(ipt_table) * (ipth->max_tables+1));
    bzero(&(ipth->tables[ipth->max_tables]), sizeof(ipt_table));
    snprintf(ipth->tables[ipth->max_tables].name, 64, tablename);
    ipth->max_tables++;
  }
  
  return(0);
}
int ipt_table_add_chain(ipt_handler *ipth, char *tablename, char *chainname, char *policyname, char *counters) {
  int i, j, found=0, tableidx=0;
  if (!ipth || !tablename || !chainname || !counters || !ipth->init) {
    return(1);
  }
  
  found=0;
  for (i=0; i<ipth->max_tables && !found; i++) {
    tableidx=i;
    if (!strcmp(ipth->tables[i].name, tablename)) found++;
  }
  if (!found) {
    return(1);
  }
  
  found=0;
  for (i=0; i<ipth->tables[tableidx].max_chains && !found; i++) {
    if (!strcmp(ipth->tables[tableidx].chains[i].name, chainname)) found++;
  }

  if (!found) {
    ipth->tables[tableidx].chains = realloc(ipth->tables[tableidx].chains, sizeof(ipt_chain) * (ipth->tables[tableidx].max_chains+1));
    bzero(&(ipth->tables[tableidx].chains[ipth->tables[tableidx].max_chains]), sizeof(ipt_chain));
    snprintf(ipth->tables[tableidx].chains[ipth->tables[tableidx].max_chains].name, 64, "%s", chainname);
    snprintf(ipth->tables[tableidx].chains[ipth->tables[tableidx].max_chains].policyname, 64, "%s", policyname);
    snprintf(ipth->tables[tableidx].chains[ipth->tables[tableidx].max_chains].counters, 64, "%s", counters);
    ipth->tables[tableidx].max_chains++;
  }
  return(0);
}
int ipt_chain_add_rule(ipt_handler *ipth, char *tablename, char *chainname, char *newrule) {
  int i, j, tableidx=0, chainidx=0, found=0;
  if (!ipth || !tablename || !chainname || !newrule || !ipth->init) {
    return(1);
  }

  found=0;
  for (i=0; i<ipth->max_tables && !found; i++) {
    tableidx=i;
    if (!strcmp(ipth->tables[i].name, tablename)) found++;
  }
  if (!found) {
    return(1);
  }

  found=0;
  for (i=0; i<ipth->tables[tableidx].max_chains && !found; i++) {
    chainidx=i;
    if (!strcmp(ipth->tables[tableidx].chains[i].name, chainname)) found++;
  }
  if (!found) {
    return(1);
  }

  found=0;
  for (i=0; i<ipth->tables[tableidx].chains[chainidx].max_rules && !found; i++) {
    if (!strcmp(ipth->tables[tableidx].chains[chainidx].rules[i].iptrule, newrule)) found++;
  }

  if (!found) {
    ipth->tables[tableidx].chains[chainidx].rules = realloc(ipth->tables[tableidx].chains[chainidx].rules, sizeof(ipt_rule) * (ipth->tables[tableidx].chains[chainidx].max_rules+1));
    bzero(&(ipth->tables[tableidx].chains[chainidx].rules[ipth->tables[tableidx].chains[chainidx].max_rules]), sizeof(ipt_rule));
    snprintf(ipth->tables[tableidx].chains[chainidx].rules[ipth->tables[tableidx].chains[chainidx].max_rules].iptrule, 1024, "%s", newrule);
    ipth->tables[tableidx].chains[chainidx].max_rules++;
  }
  
  return(0);
}

int ipt_chain_flush(ipt_handler *ipth, char *tablename, char *chainname) {
  int i, j, tableidx=0, chainidx=0, found=0;
  if (!ipth || !tablename || !chainname || !ipth->init) {
    return(1);
  }
  
  found=0;
  for (i=0; i<ipth->max_tables && !found; i++) {
    tableidx=i;
    if (!strcmp(ipth->tables[i].name, tablename)) found++;
  }
  if (!found) {
    return(1);
  }
  
  found=0;
  for (i=0; i<ipth->tables[tableidx].max_chains && !found; i++) {
    chainidx=i;
    if (!strcmp(ipth->tables[tableidx].chains[i].name, chainname)) found++;
  }
  if (!found) {
    return(1);
  }

  EUCA_FREE(ipth->tables[tableidx].chains[chainidx].rules);
  ipth->tables[tableidx].chains[chainidx].max_rules = 0;
  snprintf(ipth->tables[tableidx].chains[chainidx].counters, 64, "[0:0]");

  return(0);
}

int ipt_handler_free(ipt_handler *ipth) {
  int i, j, k;
  if (!ipth || !ipth->init) {
    return(1);
  }
  
  for (i=0; i<ipth->max_tables; i++) {
    for (j=0; j<ipth->tables[i].max_chains; j++) {
      EUCA_FREE(ipth->tables[i].chains[j].rules);
    }
    EUCA_FREE(ipth->tables[i].chains);
  }
  EUCA_FREE(ipth->tables);
  unlink(ipth->ipt_file);
  return(ipt_handler_init(ipth));
}

int ipt_handler_print(ipt_handler *ipth) {
  int i, j, k;
  if (!ipth || !ipth->init) {
    return(1);
  }
  
  for (i=0; i<ipth->max_tables; i++) {
    LOGDEBUG("TABLE (%d of %d): %s\n", i, ipth->max_tables, ipth->tables[i].name);
    for (j=0; j<ipth->tables[i].max_chains; j++) {
      LOGDEBUG("\tCHAIN: (%d of %d): %s %s %s\n", j, ipth->tables[i].max_chains, ipth->tables[i].chains[j].name, ipth->tables[i].chains[j].policyname, ipth->tables[i].chains[j].counters);
      for (k=0; k<ipth->tables[i].chains[j].max_rules; k++) {
	LOGDEBUG("\t\tRULE (%d of %d): %s\n", k, ipth->tables[i].chains[j].max_rules, ipth->tables[i].chains[j].rules[k].iptrule);
      }
    }
  }

  return(0);
}
