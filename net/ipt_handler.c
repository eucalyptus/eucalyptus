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

//!
//! @file net/ipt_handler.c
//! This file needs a description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <limits.h>

#include <eucalyptus.h>
#include <log.h>
#include <vnetwork.h>
#include <euca_string.h>

#include "ipt_handler.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] cmdprefix
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_handler_init(ipt_handler * ipth, char *cmdprefix)
{
    int fd;
    char cmd[EUCA_MAX_PATH];

    if (!ipth) {
        return (1);
    }
    bzero(ipth, sizeof(ipt_handler));

    snprintf(ipth->ipt_file, EUCA_MAX_PATH, "/tmp/ipt_file-XXXXXX");
    fd = safe_mkstemp(ipth->ipt_file);
    if (fd < 0) {
        LOGERROR("cannot create tmpfile '%s': check permissions\n", ipth->ipt_file);
        return (1);
    }
    chmod(ipth->ipt_file, 0600);
    close(fd);

    if (cmdprefix) {
        snprintf(ipth->cmdprefix, EUCA_MAX_PATH, "%s", cmdprefix);
    } else {
        ipth->cmdprefix[0] = '\0';
    }

    // test required shell-outs
    snprintf(cmd, EUCA_MAX_PATH, "%s iptables-save >/dev/null 2>&1", ipth->cmdprefix);
    if (system(cmd)) {
        LOGERROR("could not execute required shell out '%s': check command/permissions\n", cmd);
        return (1);
    }

    ipth->init = 1;
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_system_save(ipt_handler * ipth)
{
    int rc = 0;
    char cmd[EUCA_MAX_PATH] = "";

    snprintf(cmd, EUCA_MAX_PATH, "%s iptables-save -c > %s", ipth->cmdprefix, ipth->ipt_file);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc) {
        LOGERROR("iptables-save failed '%s'\n", cmd);
    }
    return (rc);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_system_restore(ipt_handler * ipth)
{
    int rc;
    char cmd[EUCA_MAX_PATH];

    snprintf(cmd, EUCA_MAX_PATH, "%s iptables-restore -c < %s", ipth->cmdprefix, ipth->ipt_file);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc) {
        copy_file(ipth->ipt_file, "/tmp/euca_ipt_file_failed");
        LOGERROR("iptables-restore failed '%s': copying failed input file to '/tmp/euca_ipt_file_failed' for manual retry.\n", cmd);
    }
    unlink(ipth->ipt_file);
    return (rc);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_handler_deploy(ipt_handler * ipth)
{
    int i, j, k;
    FILE *FH = NULL;
    if (!ipth || !ipth->init) {
        return (1);
    }

    ipt_handler_update_refcounts(ipth);

    FH = fopen(ipth->ipt_file, "w");
    if (!FH) {
        LOGERROR("could not open file for write '%s': check permissions\n", ipth->ipt_file);
        return (1);
    }
    for (i = 0; i < ipth->max_tables; i++) {
        fprintf(FH, "*%s\n", ipth->tables[i].name);
        for (j = 0; j < ipth->tables[i].max_chains; j++) {
            if (!ipth->tables[i].chains[j].flushed && ipth->tables[i].chains[j].ref_count) {
                fprintf(FH, ":%s %s %s\n", ipth->tables[i].chains[j].name, ipth->tables[i].chains[j].policyname, ipth->tables[i].chains[j].counters);
            }
        }
        for (j = 0; j < ipth->tables[i].max_chains; j++) {
            if (!ipth->tables[i].chains[j].flushed && ipth->tables[i].chains[j].ref_count) {
                // qsort!
                qsort(ipth->tables[i].chains[j].rules, ipth->tables[i].chains[j].max_rules, sizeof(ipt_rule), ipt_ruleordercmp);
                for (k = 0; k < ipth->tables[i].chains[j].max_rules; k++) {
                    if (!ipth->tables[i].chains[j].rules[k].flushed) {
                        fprintf(FH, "%s %s\n", ipth->tables[i].chains[j].rules[k].counterstr, ipth->tables[i].chains[j].rules[k].iptrule);
                    }
                }
            }
        }
        fprintf(FH, "COMMIT\n");
    }
    fclose(FH);

    return (ipt_system_restore(ipth));
}

//!
//! Function description.
//!
//! @param[in] p1
//! @param[in] p2
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_ruleordercmp(const void *p1, const void *p2)
{
    ipt_rule *a = NULL, *b = NULL;
    a = (ipt_rule *) p1;
    b = (ipt_rule *) p2;
    if (a->order == b->order) {
        return (0);
    } else if (a->order > b->order) {
        return (1);
    } else if (a->order < b->order) {
        return (-1);
    }
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_handler_repopulate(ipt_handler * ipth)
{
    int rc = 0;
    FILE *FH = NULL;
    char buf[1024] = "";
    char tmpbuf[1024] = "";
    char *strptr = NULL;
    char newrule[1024] = "";
    char tablename[64] = "";
    char chainname[64] = "";
    char policyname[64] = "";
    char counters[64] = "";
    char counterstr[256] = "";
    //  long long int countersa, countersb;

    if (!ipth || !ipth->init) {
        return (1);
    }

    rc = ipt_handler_free(ipth);
    if (rc) {
        return (1);
    }

    rc = ipt_system_save(ipth);
    if (rc) {
        LOGERROR("could not save current IPT rules to file, exiting re-populate\n");
        return (1);
    }

    FH = fopen(ipth->ipt_file, "r");
    if (!FH) {
        LOGERROR("could not open file for read '%s': check permissions\n", ipth->ipt_file);
        return (1);
    }

    while (fgets(buf, 1024, FH)) {
        if ((strptr = strchr(buf, '\n'))) {
            *strptr = '\0';
        }

        if (strlen(buf) < 1) {
            continue;
        }

        while (buf[strlen(buf) - 1] == ' ') {
            buf[strlen(buf) - 1] = '\0';
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
        } else if (buf[0] == '[' && strstr(buf, "-A")) {
            //      sscanf(buf, "[%lld:%lld] %[-A] %s", &countersa, &countersb, tmpbuf, chainname);
            sscanf(buf, "%s %[-A] %s", counterstr, tmpbuf, chainname);
            snprintf(newrule, 1024, "%s", strstr(buf, "-A"));
            //      ipt_chain_insert_rule(ipth, tablename, chainname, newrule, countersa, countersb, IPT_NO_ORDER);
            ipt_chain_insert_rule(ipth, tablename, chainname, newrule, counterstr, IPT_NO_ORDER);
        } else {
            LOGWARN("unknown IPT rule on ingress, will be thrown out: (%s)\n", buf);
        }
    }
    fclose(FH);

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] tablename
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_handler_add_table(ipt_handler * ipth, char *tablename)
{
    ipt_table *table = NULL;
    if (!ipth || !tablename || !ipth->init) {
        return (1);
    }

    table = ipt_handler_find_table(ipth, tablename);
    if (!table) {
        ipth->tables = realloc(ipth->tables, sizeof(ipt_table) * (ipth->max_tables + 1));
        if (!ipth->tables) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }
        bzero(&(ipth->tables[ipth->max_tables]), sizeof(ipt_table));
        snprintf(ipth->tables[ipth->max_tables].name, 64, tablename);
        ipth->max_tables++;
    }

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] tablename
//! @param[in] chainname
//! @param[in] policyname
//! @param[in] counters
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_table_add_chain(ipt_handler * ipth, char *tablename, char *chainname, char *policyname, char *counters)
{
    ipt_table *table = NULL;
    ipt_chain *chain = NULL;
    if (!ipth || !tablename || !chainname || !counters || !ipth->init) {
        return (1);
    }

    table = ipt_handler_find_table(ipth, tablename);
    if (!table) {
        return (1);
    }

    chain = ipt_table_find_chain(ipth, tablename, chainname);
    if (!chain) {
        table->chains = realloc(table->chains, sizeof(ipt_chain) * (table->max_chains + 1));
        if (!table->chains) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }
        bzero(&(table->chains[table->max_chains]), sizeof(ipt_chain));
        snprintf(table->chains[table->max_chains].name, 64, "%s", chainname);
        snprintf(table->chains[table->max_chains].policyname, 64, "%s", policyname);
        snprintf(table->chains[table->max_chains].counters, 64, "%s", counters);
        if (!strcmp(table->chains[table->max_chains].name, "INPUT") ||
            !strcmp(table->chains[table->max_chains].name, "FORWARD") ||
            !strcmp(table->chains[table->max_chains].name, "OUTPUT") ||
            !strcmp(table->chains[table->max_chains].name, "PREROUTING") || !strcmp(table->chains[table->max_chains].name, "POSTROUTING")) {
            table->chains[table->max_chains].ref_count = 1;
        }
        chain = &(table->chains[table->max_chains]);
        table->max_chains++;
    }
    chain->flushed = 0;

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] tablename
//! @param[in] chainname
//! @param[in] newrule
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_chain_add_rule(ipt_handler * ipth, char *tablename, char *chainname, char *newrule)
{
    return (ipt_chain_add_rule_with_counters(ipth, tablename, chainname, newrule, NULL));
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] tablename
//! @param[in] chainname
//! @param[in] newrule
//! @param[in] counterstr
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_chain_add_rule_with_counters(ipt_handler * ipth, char *tablename, char *chainname, char *newrule, char *counterstr)
{
    return (ipt_chain_insert_rule(ipth, tablename, chainname, newrule, counterstr, IPT_ORDER));
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] tablename
//! @param[in] chainname
//! @param[in] newrule
//! @param[in] counterstr
//! @param[in] order
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_chain_insert_rule(ipt_handler * ipth, char *tablename, char *chainname, char *newrule, char *counterstr, int order)
{
    int ret = 0;
    ipt_table *table = NULL;
    ipt_chain *chain = NULL;
    ipt_rule *rule = NULL;

    if (!ipth || !tablename || !chainname || !newrule || !ipth->init) {
        return (1);
    }

    table = ipt_handler_find_table(ipth, tablename);
    if (!table) {
        return (1);
    }

    chain = ipt_table_find_chain(ipth, tablename, chainname);
    if (!chain) {
        return (1);
    }

    rule = ipt_chain_find_rule(ipth, tablename, chainname, newrule);
    if (!rule) {
        chain->rules = realloc(chain->rules, sizeof(ipt_rule) * (chain->max_rules + 1));
        if (!chain->rules) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }
        rule = &(chain->rules[chain->max_rules]);
        bzero(rule, sizeof(ipt_rule));
        snprintf(rule->iptrule, 1024, "%s", newrule);
        snprintf(rule->counterstr, 256, "[0:0]");
        chain->max_rules++;
    }
    if (counterstr && strlen(counterstr)) {
        snprintf(rule->counterstr, 256, "%s", counterstr);
    }
    chain->ruleorder++;
    if (order == IPT_ORDER) {
        rule->order = chain->ruleorder;
    } else if (order == IPT_NO_ORDER) {
        rule->order = INT_MAX;
    } else {
        LOGERROR("BUG: invalid ordering mode passed to routine\n");
    }

    rule->flushed = 0;
    return (ret);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_handler_update_refcounts(ipt_handler * ipth)
{
    char *jumpptr = NULL, jumpchain[64], tmp[64];
    int i, j, k;
    ipt_table *table = NULL;
    ipt_chain *chain = NULL, *refchain = NULL;
    ipt_rule *rule = NULL;

    for (i = 0; i < ipth->max_tables; i++) {
        table = &(ipth->tables[i]);
        for (j = 0; j < table->max_chains; j++) {
            chain = &(table->chains[j]);
            for (k = 0; k < chain->max_rules; k++) {
                rule = &(chain->rules[k]);

                jumpptr = strstr(rule->iptrule, "-j");
                if (jumpptr) {
                    jumpchain[0] = '\0';
                    sscanf(jumpptr, "%[-j] %s", tmp, jumpchain);
                    if (strlen(jumpchain)) {
                        refchain = ipt_table_find_chain(ipth, table->name, jumpchain);
                        if (refchain) {
                            LOGDEBUG("FOUND REF TO CHAIN (name=%s sourcechain=%s jumpchain=%s currref=%d) (rule=%s\n", refchain->name, chain->name, jumpchain, refchain->ref_count,
                                     rule->iptrule);
                            refchain->ref_count++;
                        }
                    }
                }
            }
        }
    }
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] findtable
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
ipt_table *ipt_handler_find_table(ipt_handler * ipth, char *findtable)
{
    int i, tableidx = 0, found = 0;
    if (!ipth || !findtable || !ipth->init) {
        return (NULL);
    }

    found = 0;
    for (i = 0; i < ipth->max_tables && !found; i++) {
        tableidx = i;
        if (!strcmp(ipth->tables[i].name, findtable))
            found++;
    }
    if (!found) {
        return (NULL);
    }
    return (&(ipth->tables[tableidx]));
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] tablename
//! @param[in] findchain
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
ipt_chain *ipt_table_find_chain(ipt_handler * ipth, char *tablename, char *findchain)
{
    int i, found = 0, chainidx = 0;
    ipt_table *table = NULL;

    if (!ipth || !tablename || !findchain || !ipth->init) {
        return (NULL);
    }

    table = ipt_handler_find_table(ipth, tablename);
    if (!table) {
        return (NULL);
    }

    found = 0;
    for (i = 0; i < table->max_chains && !found; i++) {
        chainidx = i;
        if (!strcmp(table->chains[i].name, findchain))
            found++;
    }

    if (!found) {
        return (NULL);
    }

    return (&(table->chains[chainidx]));
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] tablename
//! @param[in] chainname
//! @param[in] findrule
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
ipt_rule *ipt_chain_find_rule(ipt_handler * ipth, char *tablename, char *chainname, char *findrule)
{
    int i, found = 0, ruleidx = 0;
    ipt_chain *chain;

    if (!ipth || !tablename || !chainname || !findrule || !ipth->init) {
        return (NULL);
    }

    chain = ipt_table_find_chain(ipth, tablename, chainname);
    if (!chain) {
        return (NULL);
    }

    for (i = 0; i < chain->max_rules && !found; i++) {
        ruleidx = i;
        if (!strcmp(chain->rules[i].iptrule, findrule))
            found++;
    }
    if (!found) {
        return (NULL);
    }
    return (&(chain->rules[ruleidx]));
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] tablename
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_table_deletechainempty(ipt_handler * ipth, char *tablename)
{
    int i, found = 0;
    ipt_table *table = NULL;

    if (!ipth || !tablename || !ipth->init) {
        return (1);
    }

    table = ipt_handler_find_table(ipth, tablename);
    if (!table) {
        return (1);
    }

    found = 0;
    for (i = 0; i < table->max_chains && !found; i++) {
        if (table->chains[i].max_rules == 0) {
            ipt_table_deletechainmatch(ipth, tablename, table->chains[i].name);
            found++;
        }
    }
    if (!found) {
        return (1);
    }
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] tablename
//! @param[in] chainmatch
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_table_deletechainmatch(ipt_handler * ipth, char *tablename, char *chainmatch)
{
    int i, found = 0;
    ipt_table *table = NULL;
    ipt_chain *chain = NULL;

    if (!ipth || !tablename || !chainmatch || !ipth->init) {
        return (1);
    }

    table = ipt_handler_find_table(ipth, tablename);
    if (!table) {
        return (1);
    }

    found = 0;
    for (i = 0; i < table->max_chains && !found; i++) {
        if (strstr(table->chains[i].name, chainmatch)) {
            chain = &(table->chains[i]);
            ipt_chain_flush(ipth, tablename, chain->name);
            chain->flushed = 1;
        }
    }

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//! @param[in] tablename
//! @param[in] chainname
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_chain_flush(ipt_handler * ipth, char *tablename, char *chainname)
{
    int i;
    ipt_table *table = NULL;
    ipt_chain *chain = NULL;

    if (!ipth || !tablename || !chainname || !ipth->init) {
        return (1);
    }

    table = ipt_handler_find_table(ipth, tablename);
    if (!table) {
        return (1);
    }
    chain = ipt_table_find_chain(ipth, tablename, chainname);
    if (!chain) {
        return (1);
    }

    for (i = 0; i < chain->max_rules; i++) {
        chain->rules[i].flushed = 1;
        chain->rules[i].order = 0;
    }
    chain->ruleorder = 0;

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_handler_free(ipt_handler * ipth)
{
    int i = 0;
    int j = 0;
    char saved_cmdprefix[EUCA_MAX_PATH] = "";

    if (!ipth || !ipth->init) {
        return (1);
    }
    snprintf(saved_cmdprefix, EUCA_MAX_PATH, "%s", ipth->cmdprefix);

    for (i = 0; i < ipth->max_tables; i++) {
        for (j = 0; j < ipth->tables[i].max_chains; j++) {
            EUCA_FREE(ipth->tables[i].chains[j].rules);
        }
        EUCA_FREE(ipth->tables[i].chains);
    }
    EUCA_FREE(ipth->tables);
    unlink(ipth->ipt_file);

    return (ipt_handler_init(ipth, saved_cmdprefix));
}

//!
//! Function description.
//!
//! @param[in] ipth pointer to the IP table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ipt_handler_print(ipt_handler * ipth)
{
    int i = 0, j = 0, k = 0, count = 0;
    if (!ipth || !ipth->init) {
        return (1);
    }

    if (log_level_get() == EUCA_LOG_TRACE) {
        for (i = 0; i < ipth->max_tables; i++) {
            LOGTRACE("TABLE (%d of %d): %s\n", i, ipth->max_tables, ipth->tables[i].name);
            for (j = 0; j < ipth->tables[i].max_chains; j++) {
                LOGTRACE("\tCHAIN: (%d of %d, flushed=%d, refcount=%d): %s %s %s\n", j, ipth->tables[i].max_chains, ipth->tables[i].chains[j].flushed,
                         ipth->tables[i].chains[j].ref_count, ipth->tables[i].chains[j].name, ipth->tables[i].chains[j].policyname, ipth->tables[i].chains[j].counters);
                count = 1;
                for (k = 0; k < ipth->tables[i].chains[j].max_rules; k++) {
                    LOGTRACE("\t\tRULE (%d of %d, idx=%d,flushed=%d,ruleorder=%d): %s (%s)\n", count, ipth->tables[i].chains[j].max_rules, k,
                             ipth->tables[i].chains[j].rules[k].flushed, ipth->tables[i].chains[j].rules[k].order, ipth->tables[i].chains[j].rules[k].iptrule,
                             ipth->tables[i].chains[j].rules[k].counterstr);
                    count++;
                }
            }
        }
    }

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//! @param[in] cmdprefix
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_handler_init(ips_handler * ipsh, char *cmdprefix)
{
    int fd;
    char cmd[EUCA_MAX_PATH];

    if (!ipsh) {
        LOGERROR("invalid input\n");
        return (1);
    }
    bzero(ipsh, sizeof(ips_handler));

    snprintf(ipsh->ips_file, EUCA_MAX_PATH, "/tmp/ips_file-XXXXXX");
    fd = safe_mkstemp(ipsh->ips_file);
    if (fd < 0) {
        LOGERROR("cannot create tmpfile '%s': check permissions\n", ipsh->ips_file);
        return (1);
    }
    chmod(ipsh->ips_file, 0600);
    close(fd);

    if (cmdprefix) {
        snprintf(ipsh->cmdprefix, EUCA_MAX_PATH, "%s", cmdprefix);
    } else {
        ipsh->cmdprefix[0] = '\0';
    }

    // test required shell-outs
    snprintf(cmd, EUCA_MAX_PATH, "%s ipset -L >/dev/null 2>&1", ipsh->cmdprefix);
    if (system(cmd)) {
        LOGERROR("could not execute required shell out '%s': check command/permissions\n", cmd);
        return (1);
    }

    ipsh->init = 1;
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_system_save(ips_handler * ipsh)
{
    int rc = 0;
    char cmd[EUCA_MAX_PATH] = "";

    snprintf(cmd, EUCA_MAX_PATH, "%s ipset save > %s", ipsh->cmdprefix, ipsh->ips_file);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc) {
        LOGERROR("ipset save failed '%s'\n", cmd);
    }
    return (rc);
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_system_restore(ips_handler * ipsh)
{
    int rc;
    char cmd[EUCA_MAX_PATH];

    snprintf(cmd, EUCA_MAX_PATH, "%s ipset -! restore < %s", ipsh->cmdprefix, ipsh->ips_file);
    rc = system(cmd);
    rc = rc >> 8;
    LOGDEBUG("RESTORE CMD: %s\n", cmd);
    if (rc) {
        copy_file(ipsh->ips_file, "/tmp/euca_ips_file_failed");
        LOGERROR("ipset restore failed '%s': copying failed input file to '/tmp/euca_ips_file_failed' for manual retry.\n", cmd);
    }
    unlink(ipsh->ips_file);
    return (rc);
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_handler_repopulate(ips_handler * ipsh)
{
    int rc = 0, nm = 0;
    FILE *FH = NULL;
    char buf[1024] = "";
    char *strptr = NULL;
    char setname[64] = "";
    char ipname[64] = "", *ip = NULL;

    if (!ipsh || !ipsh->init) {
        return (1);
    }

    rc = ips_handler_free(ipsh);
    if (rc) {
        return (1);
    }

    rc = ips_system_save(ipsh);
    if (rc) {
        LOGERROR("could not save current IPS rules to file, exiting re-populate\n");
        return (1);
    }

    FH = fopen(ipsh->ips_file, "r");
    if (!FH) {
        LOGERROR("could not open file for read '%s': check permissions\n", ipsh->ips_file);
        return (1);
    }

    while (fgets(buf, 1024, FH)) {
        if ((strptr = strchr(buf, '\n'))) {
            *strptr = '\0';
        }

        if (strlen(buf) < 1) {
            continue;
        }

        while (buf[strlen(buf) - 1] == ' ') {
            buf[strlen(buf) - 1] = '\0';
        }

        if (strstr(buf, "create")) {
            setname[0] = '\0';
            sscanf(buf, "create %s", setname);
            if (strlen(setname)) {
                ips_handler_add_set(ipsh, setname);
            }
        } else if (strstr(buf, "add")) {
            ipname[0] = '\0';
            sscanf(buf, "add %s %[0-9./]", setname, ipname);
            if (strlen(setname) && strlen(ipname)) {
                rc = cidrsplit(ipname, &ip, &nm);
                if (ip && strlen(ip) && nm >= 0 && nm <= 32) {
                    LOGDEBUG("reading in from ipset: adding ip/nm %s/%d to ipset %s\n", SP(ip), nm, SP(setname));
                    ips_set_add_net(ipsh, setname, ip, nm);
                    EUCA_FREE(ip);
                }
            }
        } else {
            LOGWARN("unknown IPS rule on ingress, rule will be thrown out: (%s)\n", buf);
        }
    }
    fclose(FH);

    return (0);
}

int cidrsplit(char *ipname, char **ippart, int *nmpart)
{
    char *idx = NULL;
    if (!ipname || !ippart || !nmpart) {
        LOGERROR("invalid input\n");
        return (1);
    }

    *ippart = NULL;
    *nmpart = 0;

    idx = strchr(ipname, '/');
    if (idx) {
        //nm part is present
        *idx = '\0';
        idx++;
        *nmpart = atoi(idx);
        if (*nmpart < 0 || *nmpart > 32) {
            LOGERROR("invalid netmask specified from input '%s': setting netmask to '/32'\n", ipname);
            *nmpart = 32;
        }
        *ippart = strdup(ipname);
    } else {
        // nm part is not present, use \32
        *nmpart = 32;
        *ippart = strdup(ipname);
    }
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//! @param[in] dodelete
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_handler_deploy(ips_handler * ipsh, int dodelete)
{
    int i = 0;
    int j = 0;
    FILE *FH = NULL;
    char *strptra = NULL;

    if (!ipsh || !ipsh->init) {
        return (1);
    }

    FH = fopen(ipsh->ips_file, "w");
    if (!FH) {
        LOGERROR("could not open file for write '%s': check permissions\n", ipsh->ips_file);
        return (1);
    }
    for (i = 0; i < ipsh->max_sets; i++) {
        if (ipsh->sets[i].ref_count) {
            fprintf(FH, "create %s hash:net family inet hashsize 2048 maxelem 65536\n", ipsh->sets[i].name);
            fprintf(FH, "flush %s\n", ipsh->sets[i].name);
            for (j = 0; j < ipsh->sets[i].max_member_ips; j++) {
                strptra = hex2dot(ipsh->sets[i].member_ips[j]);
                LOGDEBUG("adding ip/nm %s/%d to ipset %s\n", strptra, ipsh->sets[i].member_nms[j], ipsh->sets[i].name);
                fprintf(FH, "add %s %s/%d\n", ipsh->sets[i].name, strptra, ipsh->sets[i].member_nms[j]);
                EUCA_FREE(strptra);
            }
        } else if ((ipsh->sets[i].ref_count == 0) && dodelete) {
            fprintf(FH, "flush %s\n", ipsh->sets[i].name);
            fprintf(FH, "destroy %s\n", ipsh->sets[i].name);
        }
    }
    fclose(FH);

    return (ips_system_restore(ipsh));
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//! @param[in] setname
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_handler_add_set(ips_handler * ipsh, char *setname)
{
    ips_set *set = NULL;

    if (!ipsh || !setname || !ipsh->init) {
        return (1);
    }

    set = ips_handler_find_set(ipsh, setname);
    if (!set) {
        ipsh->sets = realloc(ipsh->sets, sizeof(ips_set) * (ipsh->max_sets + 1));
        if (!ipsh->sets) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }
        bzero(&(ipsh->sets[ipsh->max_sets]), sizeof(ips_set));
        snprintf(ipsh->sets[ipsh->max_sets].name, 64, setname);
        ipsh->sets[ipsh->max_sets].ref_count = 1;
        ipsh->max_sets++;
    }
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//! @param[in] findset
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
ips_set *ips_handler_find_set(ips_handler * ipsh, char *findset)
{
    int i, setidx = 0, found = 0;
    if (!ipsh || !findset || !ipsh->init) {
        return (NULL);
    }

    found = 0;
    for (i = 0; i < ipsh->max_sets && !found; i++) {
        setidx = i;
        if (!strcmp(ipsh->sets[i].name, findset))
            found++;
    }
    if (!found) {
        return (NULL);
    }
    return (&(ipsh->sets[setidx]));
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//! @param[in] setname
//! @param[in] ipname
//! @param[in] nmname
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_set_add_ip(ips_handler * ipsh, char *setname, char *ipname)
{
    return (ips_set_add_net(ipsh, setname, ipname, 32));
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//! @param[in] setname
//! @param[in] ipname
//! @param[in] nmname
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_set_add_net(ips_handler * ipsh, char *setname, char *ipname, int nmname)
{
    ips_set *set = NULL;
    u32 *ip = NULL;
    if (!ipsh || !setname || !ipname || !ipsh->init) {
        return (1);
    }

    set = ips_handler_find_set(ipsh, setname);
    if (!set) {
        return (1);
    }

    ip = ips_set_find_net(ipsh, setname, ipname, nmname);
    if (!ip) {
        set->member_ips = realloc(set->member_ips, sizeof(u32) * (set->max_member_ips + 1));
        if (!set->member_ips) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }
        set->member_nms = realloc(set->member_nms, sizeof(int) * (set->max_member_ips + 1));
        if (!set->member_nms) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }

        bzero(&(set->member_ips[set->max_member_ips]), sizeof(u32));
        bzero(&(set->member_nms[set->max_member_ips]), sizeof(int));
        set->member_ips[set->max_member_ips] = dot2hex(ipname);
        set->member_nms[set->max_member_ips] = nmname;
        set->max_member_ips++;
        set->ref_count++;
    }
    return (0);
}

u32 *ips_set_find_ip(ips_handler * ipsh, char *setname, char *findipstr)
{
    return (ips_set_find_net(ipsh, setname, findipstr, 32));
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//! @param[in] setname
//! @param[in] findipstr
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
u32 *ips_set_find_net(ips_handler * ipsh, char *setname, char *findipstr, int findnm)
{
    int i, found = 0, ipidx = 0;
    ips_set *set = NULL;
    u32 findip;

    if (!ipsh || !setname || !findipstr || !ipsh->init) {
        return (NULL);
    }

    set = ips_handler_find_set(ipsh, setname);
    if (!set) {
        return (NULL);
    }

    findip = dot2hex(findipstr);
    found = 0;
    for (i = 0; i < set->max_member_ips && !found; i++) {
        ipidx = i;
        if (set->member_ips[i] == findip && set->member_nms[i] == findnm)
            found++;
    }

    if (!found) {
        return (NULL);
    }

    return (&(set->member_ips[ipidx]));
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//! @param[in] setname
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_set_flush(ips_handler * ipsh, char *setname)
{
    ips_set *set = NULL;

    if (!ipsh || !setname || !ipsh->init) {
        return (1);
    }

    set = ips_handler_find_set(ipsh, setname);
    if (!set) {
        return (1);
    }

    EUCA_FREE(set->member_ips);
    EUCA_FREE(set->member_nms);
    set->max_member_ips = set->ref_count = 0;

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//! @param[in] setmatch
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_handler_deletesetmatch(ips_handler * ipsh, char *setmatch)
{
    int i = 0;
    int found = 0;

    if (!ipsh || !setmatch || !ipsh->init) {
        return (1);
    }

    found = 0;
    for (i = 0; i < ipsh->max_sets && !found; i++) {
        if (strstr(ipsh->sets[i].name, setmatch)) {
            EUCA_FREE(ipsh->sets[i].member_ips);
            EUCA_FREE(ipsh->sets[i].member_nms);
            ipsh->sets[i].max_member_ips = 0;
            ipsh->sets[i].ref_count = 0;
        }
    }

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_handler_free(ips_handler * ipsh)
{
    int i = 0;
    char saved_cmdprefix[EUCA_MAX_PATH] = "";

    if (!ipsh || !ipsh->init) {
        return (1);
    }
    snprintf(saved_cmdprefix, EUCA_MAX_PATH, "%s", ipsh->cmdprefix);

    for (i = 0; i < ipsh->max_sets; i++) {
        EUCA_FREE(ipsh->sets[i].member_ips);
        EUCA_FREE(ipsh->sets[i].member_nms);
    }
    EUCA_FREE(ipsh->sets);

    unlink(ipsh->ips_file);

    return (ips_handler_init(ipsh, saved_cmdprefix));
}

//!
//! Function description.
//!
//! @param[in] ipsh pointer to the IP set handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ips_handler_print(ips_handler * ipsh)
{
    int i, j;
    char *strptra = NULL;

    if (!ipsh) {
        return (1);
    }

    if (log_level_get() == EUCA_LOG_TRACE) {
        for (i = 0; i < ipsh->max_sets; i++) {
            LOGTRACE("IPSET NAME: %s\n", ipsh->sets[i].name);
            for (j = 0; j < ipsh->sets[i].max_member_ips; j++) {
                strptra = hex2dot(ipsh->sets[i].member_ips[j]);
                LOGTRACE("\t MEMBER IP: %s/%d\n", strptra, ipsh->sets[i].member_nms[j]);
                EUCA_FREE(strptra);
            }
        }
    }
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//! @param[in] cmdprefix
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_handler_init(ebt_handler * ebth, char *cmdprefix)
{
    int fd;
    char cmd[EUCA_MAX_PATH];

    if (!ebth) {
        return (1);
    }
    bzero(ebth, sizeof(ebt_handler));

    snprintf(ebth->ebt_filter_file, EUCA_MAX_PATH, "/tmp/ebt_filter_file-XXXXXX");
    fd = safe_mkstemp(ebth->ebt_filter_file);
    if (fd < 0) {
        LOGERROR("cannot create tmpfile '%s': check permissions\n", ebth->ebt_filter_file);
        return (1);
    }
    chmod(ebth->ebt_filter_file, 0600);
    close(fd);

    snprintf(ebth->ebt_nat_file, EUCA_MAX_PATH, "/tmp/ebt_nat_file-XXXXXX");
    fd = safe_mkstemp(ebth->ebt_nat_file);
    if (fd < 0) {
        LOGERROR("cannot create tmpfile '%s': check permissions\n", ebth->ebt_nat_file);
        return (1);
    }
    chmod(ebth->ebt_nat_file, 0600);
    close(fd);

    snprintf(ebth->ebt_asc_file, EUCA_MAX_PATH, "/tmp/ebt_asc_file-XXXXXX");
    fd = safe_mkstemp(ebth->ebt_asc_file);
    if (fd < 0) {
        LOGERROR("cannot create tmpfile '%s': check permissions\n", ebth->ebt_asc_file);
        unlink(ebth->ebt_filter_file);
        unlink(ebth->ebt_nat_file);
        return (1);
    }
    chmod(ebth->ebt_asc_file, 0600);
    close(fd);

    if (cmdprefix) {
        snprintf(ebth->cmdprefix, EUCA_MAX_PATH, "%s", cmdprefix);
    } else {
        ebth->cmdprefix[0] = '\0';
    }

    // test required shell-outs
    snprintf(cmd, EUCA_MAX_PATH, "%s ebtables -L >/dev/null 2>&1", ebth->cmdprefix);
    if (system(cmd)) {
        LOGERROR("could not execute required shell out '%s': check command/permissions\n", cmd);
        unlink(ebth->ebt_filter_file);
        unlink(ebth->ebt_nat_file);
        unlink(ebth->ebt_asc_file);
        return (1);
    }

    ebth->init = 1;
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_system_save(ebt_handler * ebth)
{
    int rc = 0;
    int ret = 0;
    char cmd[EUCA_MAX_PATH] = "";

    ret = 0;

    snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t filter --atomic-save", ebth->cmdprefix, ebth->ebt_filter_file);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc) {
        LOGERROR("ebtables-save failed '%s'\n", cmd);
        ret = 1;
    }

    snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t nat --atomic-save", ebth->cmdprefix, ebth->ebt_nat_file);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc) {
        LOGERROR("ebtables-save failed '%s'\n", cmd);
        ret = 1;
    }

    snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t filter -L > %s", ebth->cmdprefix, ebth->ebt_filter_file, ebth->ebt_asc_file);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc) {
        LOGERROR("ebtables-list failed '%s'\n", cmd);
        ret = 1;
    }

    snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t nat -L >> %s", ebth->cmdprefix, ebth->ebt_nat_file, ebth->ebt_asc_file);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc) {
        LOGERROR("ebtables-list failed '%s'\n", cmd);
        ret = 1;
    }

    return (ret);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_system_restore(ebt_handler * ebth)
{
    int rc;
    char cmd[EUCA_MAX_PATH];

    snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t filter --atomic-commit", ebth->cmdprefix, ebth->ebt_filter_file);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc) {
        copy_file(ebth->ebt_filter_file, "/tmp/euca_ebt_filter_file_failed");
        LOGERROR("ebtables-restore failed '%s': copying failed input file to '/tmp/euca_ebt_filter_file_failed' for manual retry.\n", cmd);
    }
    unlink(ebth->ebt_filter_file);

    snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t nat --atomic-commit", ebth->cmdprefix, ebth->ebt_nat_file);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc) {
        copy_file(ebth->ebt_nat_file, "/tmp/euca_ebt_nat_file_failed");
        LOGERROR("ebtables-restore failed '%s': copying failed input file to '/tmp/euca_ebt_nat_file_failed' for manual retry.\n", cmd);
    }
    unlink(ebth->ebt_nat_file);

    unlink(ebth->ebt_asc_file);

    return (rc);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_handler_deploy(ebt_handler * ebth)
{
    int i = 0;
    int j = 0;
    int k = 0;
    int rc = 0;
    char cmd[EUCA_MAX_PATH] = "";

    if (!ebth || !ebth->init) {
        return (1);
    }

    ebt_handler_update_refcounts(ebth);

    snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t filter --atomic-init", ebth->cmdprefix, ebth->ebt_filter_file);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc) {
        LOGERROR("ebtables-save failed '%s'\n", cmd);
        return (1);
    }

    snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t nat --atomic-init", ebth->cmdprefix, ebth->ebt_nat_file);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc) {
        LOGERROR("ebtables-save failed '%s'\n", cmd);
        return (1);
    }

    for (i = 0; i < ebth->max_tables; i++) {
        for (j = 0; j < ebth->tables[i].max_chains; j++) {
            if (strcmp(ebth->tables[i].chains[j].name, "EMPTY") && ebth->tables[i].chains[j].ref_count) {
                if (strcmp(ebth->tables[i].chains[j].name, "INPUT") && strcmp(ebth->tables[i].chains[j].name, "OUTPUT") && strcmp(ebth->tables[i].chains[j].name, "FORWARD")
                    && strcmp(ebth->tables[i].chains[j].name, "PREROUTING") && strcmp(ebth->tables[i].chains[j].name, "POSTROUTING")) {
                    if (!strcmp(ebth->tables[i].name, "filter")) {
                        snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t %s -N %s", ebth->cmdprefix, ebth->ebt_filter_file, ebth->tables[i].name,
                                 ebth->tables[i].chains[j].name);
                    } else if (!strcmp(ebth->tables[i].name, "nat")) {
                        snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t %s -N %s", ebth->cmdprefix, ebth->ebt_nat_file, ebth->tables[i].name,
                                 ebth->tables[i].chains[j].name);
                    }
                    rc = system(cmd);
                    rc = rc >> 8;
                    LOGTRACE("executed command (exit=%d): %s\n", rc, cmd);
                    if (rc)
                        LOGERROR("command failed: exitcode=%d command=%s\n", rc, cmd);
                }
            }
        }
        for (j = 0; j < ebth->tables[i].max_chains; j++) {
            if (strcmp(ebth->tables[i].chains[j].name, "EMPTY") && ebth->tables[i].chains[j].ref_count) {
                for (k = 0; k < ebth->tables[i].chains[j].max_rules; k++) {
                    if (!strcmp(ebth->tables[i].name, "filter")) {
                        snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t %s -A %s %s", ebth->cmdprefix, ebth->ebt_filter_file, ebth->tables[i].name,
                                 ebth->tables[i].chains[j].name, ebth->tables[i].chains[j].rules[k].ebtrule);
                    } else if (!strcmp(ebth->tables[i].name, "nat")) {
                        snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t %s -A %s %s", ebth->cmdprefix, ebth->ebt_nat_file, ebth->tables[i].name,
                                 ebth->tables[i].chains[j].name, ebth->tables[i].chains[j].rules[k].ebtrule);
                    }
                    rc = system(cmd);
                    rc = rc >> 8;
                    LOGTRACE("executed command (exit=%d): %s\n", rc, cmd);
                    if (rc)
                        LOGERROR("command failed: exitcode=%d command=%s\n", rc, cmd);
                }
            }
        }
    }
    return (ebt_system_restore(ebth));
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_handler_repopulate(ebt_handler * ebth)
{
    int rc = 0;
    FILE *FH = NULL;
    char buf[1024] = "";
    char tmpbuf[1024] = "";
    char *strptr = NULL;
    char tablename[64] = "";
    char chainname[64] = "";
    char policyname[64] = "";

    if (!ebth || !ebth->init) {
        return (1);
    }

    rc = ebt_handler_free(ebth);
    if (rc) {
        return (1);
    }

    rc = ebt_system_save(ebth);
    if (rc) {
        LOGERROR("could not save current EBT rules to file, exiting re-populate\n");
        return (1);
    }

    FH = fopen(ebth->ebt_asc_file, "r");
    if (!FH) {
        LOGERROR("could not open file for read '%s': check permissions\n", ebth->ebt_asc_file);
        return (1);
    }

    while (fgets(buf, 1024, FH)) {
        if ((strptr = strchr(buf, '\n'))) {
            *strptr = '\0';
        }

        if (strlen(buf) < 1) {
            continue;
        }

        while (buf[strlen(buf) - 1] == ' ') {
            buf[strlen(buf) - 1] = '\0';
        }

        if (strstr(buf, "Bridge table:")) {
            tablename[0] = '\0';
            sscanf(buf, "Bridge table: %s", tablename);
            if (strlen(tablename)) {
                ebt_handler_add_table(ebth, tablename);
            }
        } else if (strstr(buf, "Bridge chain: ")) {
            chainname[0] = '\0';
            sscanf(buf, "Bridge chain: %[^,]%s %s %s %s %s", chainname, tmpbuf, tmpbuf, tmpbuf, tmpbuf, policyname);
            if (strlen(chainname)) {
                ebt_table_add_chain(ebth, tablename, chainname, policyname, "");
            }
        } else if (buf[0] == '#') {
        } else if (buf[0] == '-') {
            ebt_chain_add_rule(ebth, tablename, chainname, buf);
        } else {
            LOGWARN("unknown EBT rule on ingress, will be thrown out: (%s)\n", buf);
        }
    }
    fclose(FH);

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//! @param[in] tablename
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_handler_add_table(ebt_handler * ebth, char *tablename)
{
    ebt_table *table = NULL;
    if (!ebth || !tablename || !ebth->init) {
        return (1);
    }

    LOGDEBUG("adding table %s\n", tablename);
    table = ebt_handler_find_table(ebth, tablename);
    if (!table) {
        ebth->tables = realloc(ebth->tables, sizeof(ebt_table) * (ebth->max_tables + 1));
        if (!ebth->tables) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }
        bzero(&(ebth->tables[ebth->max_tables]), sizeof(ebt_table));
        snprintf(ebth->tables[ebth->max_tables].name, 64, tablename);
        ebth->max_tables++;
    }

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//! @param[in] tablename
//! @param[in] chainname
//! @param[in] policyname
//! @param[in] counters
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_table_add_chain(ebt_handler * ebth, char *tablename, char *chainname, char *policyname, char *counters)
{
    ebt_table *table = NULL;
    ebt_chain *chain = NULL;
    if (!ebth || !tablename || !chainname || !counters || !ebth->init) {
        return (1);
    }
    LOGDEBUG("adding chain %s to table %s\n", chainname, tablename);
    table = ebt_handler_find_table(ebth, tablename);
    if (!table) {
        return (1);
    }

    chain = ebt_table_find_chain(ebth, tablename, chainname);
    if (!chain) {
        table->chains = realloc(table->chains, sizeof(ebt_chain) * (table->max_chains + 1));
        if (!table->chains) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }
        bzero(&(table->chains[table->max_chains]), sizeof(ebt_chain));
        snprintf(table->chains[table->max_chains].name, 64, "%s", chainname);
        snprintf(table->chains[table->max_chains].policyname, 64, "%s", policyname);
        snprintf(table->chains[table->max_chains].counters, 64, "%s", counters);
        if (!strcmp(table->chains[table->max_chains].name, "INPUT") ||
            !strcmp(table->chains[table->max_chains].name, "FORWARD") ||
            !strcmp(table->chains[table->max_chains].name, "OUTPUT") ||
            !strcmp(table->chains[table->max_chains].name, "PREROUTING") || !strcmp(table->chains[table->max_chains].name, "POSTROUTING")) {
            table->chains[table->max_chains].ref_count = 1;
        }

        table->max_chains++;

    }

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//! @param[in] tablename
//! @param[in] chainname
//! @param[in] newrule
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_chain_add_rule(ebt_handler * ebth, char *tablename, char *chainname, char *newrule)
{
    ebt_table *table = NULL;
    ebt_chain *chain = NULL;
    ebt_rule *rule = NULL;

    LOGDEBUG("adding rules (%s) to chain %s to table %s\n", newrule, chainname, tablename);
    if (!ebth || !tablename || !chainname || !newrule || !ebth->init) {
        return (1);
    }

    table = ebt_handler_find_table(ebth, tablename);
    if (!table) {
        return (1);
    }

    chain = ebt_table_find_chain(ebth, tablename, chainname);
    if (!chain) {
        return (1);
    }

    rule = ebt_chain_find_rule(ebth, tablename, chainname, newrule);
    if (!rule) {
        chain->rules = realloc(chain->rules, sizeof(ebt_rule) * (chain->max_rules + 1));
        if (!chain->rules) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }
        bzero(&(chain->rules[chain->max_rules]), sizeof(ebt_rule));
        snprintf(chain->rules[chain->max_rules].ebtrule, 1024, "%s", newrule);
        chain->max_rules++;
    }
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_handler_update_refcounts(ebt_handler * ebth)
{
    char *jumpptr = NULL, jumpchain[64], tmp[64];
    int i, j, k;
    ebt_table *table = NULL;
    ebt_chain *chain = NULL, *refchain = NULL;
    ebt_rule *rule = NULL;

    for (i = 0; i < ebth->max_tables; i++) {
        table = &(ebth->tables[i]);
        for (j = 0; j < table->max_chains; j++) {
            chain = &(table->chains[j]);
            for (k = 0; k < chain->max_rules; k++) {
                rule = &(chain->rules[k]);

                jumpptr = strstr(rule->ebtrule, "-j");
                if (jumpptr) {
                    jumpchain[0] = '\0';
                    sscanf(jumpptr, "%[-j] %s", tmp, jumpchain);
                    if (strlen(jumpchain)) {
                        refchain = ebt_table_find_chain(ebth, table->name, jumpchain);
                        if (refchain) {
                            LOGDEBUG("FOUND REF TO CHAIN (name=%s sourcechain=%s jumpchain=%s currref=%d) (rule=%s\n", refchain->name, chain->name, jumpchain, refchain->ref_count,
                                     rule->ebtrule);
                            refchain->ref_count++;
                        }
                    }
                }
            }
        }
    }
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//! @param[in] findtable
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
ebt_table *ebt_handler_find_table(ebt_handler * ebth, char *findtable)
{
    int i, tableidx = 0, found = 0;
    if (!ebth || !findtable || !ebth->init) {
        return (NULL);
    }

    found = 0;
    for (i = 0; i < ebth->max_tables && !found; i++) {
        tableidx = i;
        if (!strcmp(ebth->tables[i].name, findtable))
            found++;
    }
    if (!found) {
        return (NULL);
    }
    return (&(ebth->tables[tableidx]));
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//! @param[in] tablename
//! @param[in] findchain
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
ebt_chain *ebt_table_find_chain(ebt_handler * ebth, char *tablename, char *findchain)
{
    int i, found = 0, chainidx = 0;
    ebt_table *table = NULL;

    if (!ebth || !tablename || !findchain || !ebth->init) {
        return (NULL);
    }

    table = ebt_handler_find_table(ebth, tablename);
    if (!table) {
        return (NULL);
    }

    found = 0;
    for (i = 0; i < table->max_chains && !found; i++) {
        chainidx = i;
        if (!strcmp(table->chains[i].name, findchain))
            found++;
    }

    if (!found) {
        return (NULL);
    }

    return (&(table->chains[chainidx]));
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//! @param[in] tablename
//! @param[in] chainname
//! @param[in] findrule
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
ebt_rule *ebt_chain_find_rule(ebt_handler * ebth, char *tablename, char *chainname, char *findrule)
{
    int i, found = 0, ruleidx = 0;
    ebt_chain *chain;

    if (!ebth || !tablename || !chainname || !findrule || !ebth->init) {
        return (NULL);
    }

    chain = ebt_table_find_chain(ebth, tablename, chainname);
    if (!chain) {
        return (NULL);
    }

    for (i = 0; i < chain->max_rules; i++) {
        ruleidx = i;
        if (!strcmp(chain->rules[i].ebtrule, findrule))
            found++;
    }
    if (!found) {
        return (NULL);
    }
    return (&(chain->rules[i]));
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//! @param[in] tablename
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_table_deletechainempty(ebt_handler * ebth, char *tablename)
{
    int i, found = 0;
    ebt_table *table = NULL;

    if (!ebth || !tablename || !ebth->init) {
        return (1);
    }

    table = ebt_handler_find_table(ebth, tablename);
    if (!table) {
        return (1);
    }

    found = 0;
    for (i = 0; i < table->max_chains && !found; i++) {
        if (table->chains[i].max_rules == 0) {
            ebt_table_deletechainmatch(ebth, tablename, table->chains[i].name);
            found++;
        }
    }
    if (!found) {
        return (1);
    }
    return (0);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//! @param[in] tablename
//! @param[in] chainmatch
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_table_deletechainmatch(ebt_handler * ebth, char *tablename, char *chainmatch)
{
    int i, found = 0;
    ebt_table *table = NULL;

    if (!ebth || !tablename || !chainmatch || !ebth->init) {
        return (1);
    }

    table = ebt_handler_find_table(ebth, tablename);
    if (!table) {
        return (1);
    }

    found = 0;
    for (i = 0; i < table->max_chains && !found; i++) {
        if (strstr(table->chains[i].name, chainmatch)) {
            EUCA_FREE(table->chains[i].rules);
            bzero(&(table->chains[i]), sizeof(ebt_chain));
            snprintf(table->chains[i].name, 64, "EMPTY");
        }
    }

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//! @param[in] tablename
//! @param[in] chainname
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_chain_flush(ebt_handler * ebth, char *tablename, char *chainname)
{
    ebt_table *table = NULL;
    ebt_chain *chain = NULL;

    if (!ebth || !tablename || !chainname || !ebth->init) {
        return (1);
    }

    table = ebt_handler_find_table(ebth, tablename);
    if (!table) {
        return (1);
    }
    chain = ebt_table_find_chain(ebth, tablename, chainname);
    if (!chain) {
        return (1);
    }

    EUCA_FREE(chain->rules);
    chain->max_rules = 0;
    chain->counters[0] = '\0';

    return (0);
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_handler_free(ebt_handler * ebth)
{
    int i = 0;
    int j = 0;
    char saved_cmdprefix[EUCA_MAX_PATH] = "";
    if (!ebth || !ebth->init) {
        return (1);
    }
    snprintf(saved_cmdprefix, EUCA_MAX_PATH, "%s", ebth->cmdprefix);

    for (i = 0; i < ebth->max_tables; i++) {
        for (j = 0; j < ebth->tables[i].max_chains; j++) {
            EUCA_FREE(ebth->tables[i].chains[j].rules);
        }
        EUCA_FREE(ebth->tables[i].chains);
    }
    EUCA_FREE(ebth->tables);

    unlink(ebth->ebt_filter_file);
    unlink(ebth->ebt_nat_file);
    unlink(ebth->ebt_asc_file);

    return (ebt_handler_init(ebth, saved_cmdprefix));
}

//!
//! Function description.
//!
//! @param[in] ebth pointer to the EB table handler structure
//!
//! @return
//!
//! @see
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note
//!
int ebt_handler_print(ebt_handler * ebth)
{
    int i, j, k;
    if (!ebth || !ebth->init) {
        return (1);
    }

    if (log_level_get() == EUCA_LOG_TRACE) {
        for (i = 0; i < ebth->max_tables; i++) {
            LOGTRACE("TABLE (%d of %d): %s\n", i, ebth->max_tables, ebth->tables[i].name);
            for (j = 0; j < ebth->tables[i].max_chains; j++) {
                LOGTRACE("\tCHAIN: (%d of %d, refcount=%d): %s policy=%s counters=%s\n", j, ebth->tables[i].max_chains, ebth->tables[i].chains[j].ref_count,
                         ebth->tables[i].chains[j].name, ebth->tables[i].chains[j].policyname, ebth->tables[i].chains[j].counters);
                for (k = 0; k < ebth->tables[i].chains[j].max_rules; k++) {
                    LOGTRACE("\t\tRULE (%d of %d): %s\n", k, ebth->tables[i].chains[j].max_rules, ebth->tables[i].chains[j].rules[k].ebtrule);
                }
            }
        }
    }

    return (0);
}
