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
    snprintf(cmd, MAX_PATH, "cat %s", ipth->ipt_file);
    system(cmd);
  }
  return(rc);
}

int ipt_handler_deploy(ipt_handler *ipth) {
  int i, j, k;
  FILE *FH=NULL;
  if (!ipth || !ipth->init) {
    return(1);
  }
  
  ipt_handler_update_refcounts(ipth);

  FH=fopen(ipth->ipt_file, "w");
  if (!FH) {
    LOGERROR("could not open file for write(%s)\n", ipth->ipt_file);
    return(1);
  }
  for (i=0; i<ipth->max_tables; i++) {
    fprintf(FH, "*%s\n", ipth->tables[i].name);
    for (j=0; j<ipth->tables[i].max_chains; j++) {
      if (strcmp(ipth->tables[i].chains[j].name, "EMPTY") && ipth->tables[i].chains[j].ref_count) {
	fprintf(FH, ":%s %s %s\n", ipth->tables[i].chains[j].name, ipth->tables[i].chains[j].policyname, ipth->tables[i].chains[j].counters);
      }
    }
    for (j=0; j<ipth->tables[i].max_chains; j++) {
      if (strcmp(ipth->tables[i].chains[j].name, "EMPTY") && ipth->tables[i].chains[j].ref_count) {
	for (k=0; k<ipth->tables[i].chains[j].max_rules; k++) {
	  fprintf(FH, "%s\n", ipth->tables[i].chains[j].rules[k].iptrule);
	}
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
  ipt_table *table=NULL;
  if (!ipth || !tablename || !ipth->init) {
    return(1);
  }
  
  table = ipt_handler_find_table(ipth, tablename);
  if (!table) {
    ipth->tables = realloc(ipth->tables, sizeof(ipt_table) * (ipth->max_tables+1));
    bzero(&(ipth->tables[ipth->max_tables]), sizeof(ipt_table));
    snprintf(ipth->tables[ipth->max_tables].name, 64, tablename);
    ipth->max_tables++;
  }
  
  return(0);
}
int ipt_table_add_chain(ipt_handler *ipth, char *tablename, char *chainname, char *policyname, char *counters) {
  ipt_table *table=NULL;
  ipt_chain *chain=NULL;
  if (!ipth || !tablename || !chainname || !counters || !ipth->init) {
    return(1);
  }
  
  table = ipt_handler_find_table(ipth, tablename);
  if (!table) {
    return(1);
  }
  
  chain = ipt_table_find_chain(ipth, tablename, chainname);
  if (!chain) {
    table->chains = realloc(table->chains, sizeof(ipt_chain) * (table->max_chains+1));
    bzero(&(table->chains[table->max_chains]), sizeof(ipt_chain));
    snprintf(table->chains[table->max_chains].name, 64, "%s", chainname);
    snprintf(table->chains[table->max_chains].policyname, 64, "%s", policyname);
    snprintf(table->chains[table->max_chains].counters, 64, "%s", counters);
    if (!strcmp(table->chains[table->max_chains].name, "INPUT") ||
	!strcmp(table->chains[table->max_chains].name, "FORWARD") ||
	!strcmp(table->chains[table->max_chains].name, "OUTPUT") ||
	!strcmp(table->chains[table->max_chains].name, "PREROUTING") ||
	!strcmp(table->chains[table->max_chains].name, "POSTROUTING")) {
      table->chains[table->max_chains].ref_count=1;
    }
	
    table->max_chains++;

  }

  return(0);
}

int ipt_chain_add_rule(ipt_handler *ipth, char *tablename, char *chainname, char *newrule) {
  ipt_table *table=NULL;
  ipt_chain *chain=NULL;
  ipt_rule *rule=NULL;

  if (!ipth || !tablename || !chainname || !newrule || !ipth->init) {
    return(1);
  }
  
  table = ipt_handler_find_table(ipth, tablename);
  if (!table) {
    return(1);
  }

  chain = ipt_table_find_chain(ipth, tablename, chainname);
  if (!chain) {
    return(1);
  }
  
  rule = ipt_chain_find_rule(ipth, tablename, chainname, newrule);
  if (!rule) {
    chain->rules = realloc(chain->rules, sizeof(ipt_rule) * (chain->max_rules+1));
    bzero(&(chain->rules[chain->max_rules]), sizeof(ipt_rule));
    snprintf(chain->rules[chain->max_rules].iptrule, 1024, "%s", newrule);
    chain->max_rules++;
  }
  return(0);
}

int ipt_handler_update_refcounts(ipt_handler *ipth) {
    char *jumpptr=NULL, jumpchain[64], tmp[64];
    int i, j, k;
    ipt_table *table=NULL;
    ipt_chain *chain=NULL, *refchain=NULL;
    ipt_rule *rule=NULL;
    
    for (i=0; i<ipth->max_tables; i++) {
        table = &(ipth->tables[i]);
        for (j=0; j<table->max_chains; j++) {
            chain = &(table->chains[j]);
            for (k=0; k<chain->max_rules; k++) {
                rule = &(chain->rules[k]);

                jumpptr = strstr(rule->iptrule, "-j");
                if (jumpptr) {
                    jumpchain[0] = '\0';
                    sscanf(jumpptr, "%[-j] %s", tmp, jumpchain);
                    if (strlen(jumpchain)) {
                        refchain = ipt_table_find_chain(ipth, table->name, jumpchain);
                        if (refchain) {
                            LOGDEBUG("FOUND REF TO CHAIN (name=%s jumpchain=%s currref=%d)\n", refchain->name, jumpchain, refchain->ref_count);
                            refchain->ref_count++;
                        }
                    }
                }
            }
        }
    }
    return(0);
}

ipt_table *ipt_handler_find_table(ipt_handler *ipth, char *findtable) {
  int i, tableidx=0, found=0;
  if (!ipth || !findtable || !ipth->init) {
    return(NULL);
  }
  
  found=0;
  for (i=0; i<ipth->max_tables && !found; i++) {
    tableidx=i;
    if (!strcmp(ipth->tables[i].name, findtable)) found++;
  }
  if (!found) {
    return(NULL);
  }
  return(&(ipth->tables[tableidx]));  
}

ipt_chain *ipt_table_find_chain(ipt_handler *ipth, char *tablename, char *findchain) {
  int i, found=0, chainidx=0;
  ipt_table *table=NULL;

  if (!ipth || !tablename || !findchain || !ipth->init) {
    return(NULL);
  }
  
  table = ipt_handler_find_table(ipth, tablename);
  if (!table) {
    return(NULL);
  }
  
  found=0;
  for (i=0; i<table->max_chains && !found; i++) {
    chainidx=i;
    if (!strcmp(table->chains[i].name, findchain)) found++;
  }
  
  if (!found) {
    return(NULL);
  }
  
  return(&(table->chains[chainidx]));
}

ipt_rule *ipt_chain_find_rule(ipt_handler *ipth, char *tablename, char *chainname, char *findrule) {
  int i, found=0, ruleidx=0;
  ipt_chain *chain;

  if (!ipth || !tablename || !chainname || !findrule || !ipth->init) {
    return(NULL);
  }

  chain = ipt_table_find_chain(ipth, tablename, chainname);
  if (!chain) {
    return(NULL);
  }
  
  for (i=0; i<chain->max_rules; i++) {
    ruleidx=i;
    if (!strcmp(chain->rules[i].iptrule, findrule)) found++;
  }
  if (!found) {
    return(NULL);
  }
  return(&(chain->rules[i]));
}

int ipt_chain_deleteempty(ipt_handler *ipth, char *tablename) {
  int i, found=0;
  ipt_table *table=NULL;

  if (!ipth || !tablename || !ipth->init) {
    return(1);
  }
  
  table = ipt_handler_find_table(ipth, tablename);
  if (!table) {
    return(1);
  }
  
  found=0;
  for (i=0; i<table->max_chains && !found; i++) {
    if (table->chains[i].max_rules == 0) {
      ipt_chain_deletematch(ipth, tablename, table->chains[i].name);
      found++;
    }
  }
  if (!found) {
    return(1);
  }
  return(0);
}

int ipt_chain_deletematch(ipt_handler *ipth, char *tablename, char *chainmatch) {
  int i, found=0;
  ipt_table *table=NULL;

  if (!ipth || !tablename || !chainmatch || !ipth->init) {
    return(1);
  }
  
  table = ipt_handler_find_table(ipth, tablename);
  if (!table) {
    return(1);
  }
  
  found=0;
  for (i=0; i<table->max_chains && !found; i++) {
    if (strstr(table->chains[i].name, chainmatch)) {
      EUCA_FREE(table->chains[i].rules);
      bzero(&(table->chains[i]), sizeof(ipt_chain));
      snprintf(table->chains[i].name, 64, "EMPTY");
      //      found++;
    }
  }
  if (!found) {
    return(1);
  }
  

  return(0);

}

int ipt_chain_flush(ipt_handler *ipth, char *tablename, char *chainname) {
  ipt_table *table=NULL;
  ipt_chain *chain=NULL;

  if (!ipth || !tablename || !chainname || !ipth->init) {
    return(1);
  }
  
  table = ipt_handler_find_table(ipth, tablename);
  if (!table) {
    return(1);
  }
  chain = ipt_table_find_chain(ipth, tablename, chainname);
  if (!chain) {
    return(1);
  }
  
  EUCA_FREE(chain->rules);
  chain->max_rules = 0;
  snprintf(chain->counters, 64, "[0:0]");

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
      LOGDEBUG("\tCHAIN: (%d of %d, refcount=%d): %s %s %s\n", j, ipth->tables[i].max_chains, ipth->tables[i].chains[j].ref_count, ipth->tables[i].chains[j].name, ipth->tables[i].chains[j].policyname, ipth->tables[i].chains[j].counters);
      for (k=0; k<ipth->tables[i].chains[j].max_rules; k++) {
	LOGDEBUG("\t\tRULE (%d of %d): %s\n", k, ipth->tables[i].chains[j].max_rules, ipth->tables[i].chains[j].rules[k].iptrule);
      }
    }
  }

  return(0);
}
