// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
#include <sys/types.h>
#include <fcntl.h>
#include <limits.h>

#include <eucalyptus.h>
#include <log.h>
#include <euca_string.h>

#include "ebt_handler.h"
#include "eucanetd_util.h"
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

/**
 * Initialize the Ebtables handler structure.
 *
 * @param ebth [in] pointer to the EB table handler structure
 * @param cmdprefix [in] a string pointer to the prefix to use for each system commands
 *
 * @return 0 on success. 1 on failure.
 *
 * @see ipt_handler_init() 
 *
 * @pre
 *     - The ebth pointer should not be NULL
 *     - We should be able to create temporary files on the system
 *     - We should be able to execute ebtables commands.
 *
 * @post
 *     - Temporary files on disk: /tmp/ebt_filter_file-XXXXXX, /tmp/ebt_nat_file-XXXXXX
 *       and /tmp/ebt_asc_file-XXXXXX.
 *     - If cmdprefix was provided, the table's cmdprefix field will be set with it
 *
 * @note
 *     - Once temporary files are initialized the filename will be reused throughout the process
 *       lifetime. The files will be truncated/created on each successive calls to the *_handler_init()
 *       method. 
 */
int ebt_handler_init(ebt_handler *ebth, const char *cmdprefix) {
    int fd;
    char sTempFilterFile[EUCA_MAX_PATH] = "";
    char sTempNatFile[EUCA_MAX_PATH] = "";
    char sTempAscFile[EUCA_MAX_PATH] = "";
    
    if (!ebth) {
        return (1);
    }

    if (ebth->init) {
        snprintf(sTempFilterFile, EUCA_MAX_PATH, "%s", ebth->ebt_filter_file);
        snprintf(sTempNatFile, EUCA_MAX_PATH, "%s", ebth->ebt_nat_file);
        snprintf(sTempAscFile, EUCA_MAX_PATH, "%s", ebth->ebt_asc_file);

        if (truncate_file(sTempFilterFile)) {            
            return (1);
        }

        if (truncate_file(sTempNatFile)) {
            unlink(sTempFilterFile);
            return (1);
        }

        if (truncate_file(sTempAscFile)) {
            unlink(sTempFilterFile);
            unlink(sTempNatFile);
            return (1);
        }
    } else {
        snprintf(sTempFilterFile, EUCA_MAX_PATH, "/tmp/ebt_filter_file-XXXXXX");
        if ((fd = safe_mkstemp(sTempFilterFile))< 0) {
            LOGERROR("cannot create tmpfile '%s': check permissions\n", sTempFilterFile);
            return (1);
        }
        if (chmod(sTempFilterFile, 0600)) {
            LOGWARN("chmod failed: was able to create tmpfile '%s', but could not change file permissions\n", sTempFilterFile);
        }
        close(fd);
        
        snprintf(sTempNatFile, EUCA_MAX_PATH, "/tmp/ebt_nat_file-XXXXXX");
        if ((fd = safe_mkstemp(sTempNatFile)) < 0) {
            LOGERROR("cannot create tmpfile '%s': check permissions\n", sTempNatFile);
            unlink(sTempFilterFile);
            return (1);
        }
        if (chmod(sTempNatFile, 0600)) {
            LOGWARN("chmod failed: was able to create tmpfile '%s', but could not change file permissions\n", sTempNatFile);
        }
        close(fd);
        
        snprintf(sTempAscFile, EUCA_MAX_PATH, "/tmp/ebt_asc_file-XXXXXX");
        if ((fd = safe_mkstemp(sTempAscFile)) < 0) {
            LOGERROR("cannot create tmpfile '%s': check permissions\n", sTempAscFile);
            unlink(sTempFilterFile);
            unlink(sTempNatFile);
            return (1);
        }
        if (chmod(sTempAscFile, 0600)) {
            LOGWARN("chmod failed: was able to create tmpfile '%s', but could not change file permissions\n", sTempAscFile);
        }
        close(fd);
    }
    
    bzero(ebth, sizeof(ebt_handler));

    // Copy names back into handler
    snprintf(ebth->ebt_filter_file, EUCA_MAX_PATH, "%s", sTempFilterFile);
    snprintf(ebth->ebt_nat_file, EUCA_MAX_PATH, "%s", sTempNatFile);
    snprintf(ebth->ebt_asc_file, EUCA_MAX_PATH, "%s", sTempAscFile);

    if (cmdprefix) {
        snprintf(ebth->cmdprefix, EUCA_MAX_PATH, "%s", cmdprefix);
    } else {
        ebth->cmdprefix[0] = '\0';
    }

    // test required shell-outs
    if (euca_execlp_redirect(NULL, NULL, "/dev/null", FALSE, "/dev/null", FALSE, ebth->cmdprefix, "ebtables", "-L", NULL) != EUCA_OK) {
        LOGERROR("could not execute ebtables -L. check command/permissions\n");
        unlink(ebth->ebt_filter_file);
        unlink(ebth->ebt_nat_file);
        unlink(ebth->ebt_asc_file);
        return (1);
    }

    ebth->init = 1;
    return (0);
}

/**
 * Saves the current ebtables state to files.
 *
 * @param ebth [in] pointer to the EB table handler structure
 *
 * @return 0 on success. 1 on failure.
 */
int ebt_system_save(ebt_handler *ebth) {
    int ret = 0;

    if (euca_execlp(NULL, ebth->cmdprefix, "ebtables", "--atomic-file", ebth->ebt_filter_file, "-t", "filter", "--atomic-save", NULL) != EUCA_OK) {
        LOGERROR("ebtables-save -t filter failed\n");
        ret = 1;
    }
    if (euca_execlp(NULL, ebth->cmdprefix, "ebtables", "--atomic-file", ebth->ebt_nat_file, "-t", "nat", "--atomic-save", NULL) != EUCA_OK) {
        LOGERROR("ebtables-save -t nat failed\n");
        ret = 1;
    }
    if (euca_execlp_redirect(NULL, NULL, ebth->ebt_asc_file, FALSE, NULL, FALSE, ebth->cmdprefix, "ebtables", "--atomic-file", ebth->ebt_filter_file, "-t", "filter", "-L", NULL) != EUCA_OK) {
        LOGERROR("ebtables-list -t filter failed\n");
        ret = 1;
    }
    if (euca_execlp_redirect(NULL, NULL, ebth->ebt_asc_file, TRUE, NULL, FALSE, ebth->cmdprefix, "ebtables", "--atomic-file", ebth->ebt_nat_file, "-t", "nat", "-L", NULL) != EUCA_OK) {
        LOGERROR("ebtables-list -t filter failed\n");
        ret = 1;
    }
    return (ret);
}

/**
 * Restores ebtables state from files.
 *
 * @param ebth [in] pointer to the EB table handler structure
 *
 * @return 0 on success. 1 on failure.
 */
int ebt_system_restore(ebt_handler *ebth) {
    int ret = EUCA_OK;
    if (euca_execlp(NULL, ebth->cmdprefix, "ebtables", "--atomic-file", ebth->ebt_filter_file, "-t", "filter", "--atomic-commit", NULL) != EUCA_OK) {
        copy_file(ebth->ebt_filter_file, "/tmp/euca_ebt_filter_file_failed");
        LOGERROR("ebtables-restore failed. copying failed input file to '/tmp/euca_ebt_filter_file_failed' for manual retry.\n");
        ret = 1;
    }

    if (euca_execlp(NULL, ebth->cmdprefix, "ebtables", "--atomic-file", ebth->ebt_nat_file, "-t", "nat", "--atomic-commit", NULL) != EUCA_OK) {
        copy_file(ebth->ebt_nat_file, "/tmp/euca_ebt_nat_file_failed");
        LOGERROR("ebtables-restore failed. copying failed input file to '/tmp/euca_ebt_nat_file_failed' for manual retry.\n");
        ret = 1;
    }

    unlink_handler_file(ebth->ebt_filter_file);
    unlink_handler_file(ebth->ebt_nat_file);
    unlink_handler_file(ebth->ebt_asc_file);

    return (ret);
}

/**
 * Dumps ebtables hander state to files and restore this ebtables state into system.
 *
 * @param ebth [in] pointer to the EB table handler structure
 *
 * @return 0 on success. 1 on failure.
 */
int ebt_handler_deploy(ebt_handler *ebth) {
    int i = 0;
    int j = 0;
    int k = 0;
    char cmd[EUCA_MAX_PATH] = "";

    if (!ebth || !ebth->init) {
        return (1);
    }

    // Create tmp files as non-root
    char *strptr = strdup(ebth->cmdprefix);
    ebt_table *tablesbak = ebth->tables;
    int maxtablesbak = ebth->max_tables;
    ebt_handler_init(ebth, strptr);
    ebth->tables = tablesbak;
    ebth->max_tables = maxtablesbak;
    EUCA_FREE(strptr);

    ebt_handler_update_refcounts(ebth);

    if (euca_execlp(NULL, ebth->cmdprefix, "ebtables", "--atomic-file", ebth->ebt_filter_file, "-t", "filter", "--atomic-init", NULL) != EUCA_OK) {
        LOGERROR("ebtables-save failed\n");
        return (1);
    }

    if (euca_execlp(NULL, ebth->cmdprefix, "ebtables", "--atomic-file", ebth->ebt_nat_file, "-t", "nat", "--atomic-init", NULL) != EUCA_OK) {
        LOGERROR("ebtables-save failed\n");
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
                        if (euca_exec(cmd) != EUCA_OK) {
                            LOGERROR("command failed: command=%s\n", cmd);
                        }
                    } else if (!strcmp(ebth->tables[i].name, "nat")) {
                        snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t %s -N %s", ebth->cmdprefix, ebth->ebt_nat_file, ebth->tables[i].name,
                                 ebth->tables[i].chains[j].name);
                        if (euca_exec(cmd) != EUCA_OK) {
                            LOGERROR("command failed: command=%s\n", cmd);
                        }
                    }
                }
            }
        }
        for (j = 0; j < ebth->tables[i].max_chains; j++) {
            if (strcmp(ebth->tables[i].chains[j].name, "EMPTY") && ebth->tables[i].chains[j].ref_count) {
                for (k = 0; k < ebth->tables[i].chains[j].max_rules; k++) {
                    if (!strcmp(ebth->tables[i].name, "filter")) {
                        snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t %s -A %s %s", ebth->cmdprefix, ebth->ebt_filter_file, ebth->tables[i].name,
                                 ebth->tables[i].chains[j].name, ebth->tables[i].chains[j].rules[k].ebtrule);
                        if (euca_exec(cmd) != EUCA_OK) {
                            LOGERROR("command failed: command=%s\n", cmd);
                        }
                    } else if (!strcmp(ebth->tables[i].name, "nat")) {
                        snprintf(cmd, EUCA_MAX_PATH, "%s ebtables --atomic-file %s -t %s -A %s %s", ebth->cmdprefix, ebth->ebt_nat_file, ebth->tables[i].name,
                                 ebth->tables[i].chains[j].name, ebth->tables[i].chains[j].rules[k].ebtrule);
                        if (euca_exec(cmd) != EUCA_OK) {
                            LOGERROR("command failed: command=%s\n", cmd);
                        }
                    }
                }
            }
        }
    }
    return (ebt_system_restore(ebth));
}

/**
 * Retrieve ebtables system state to this handler data structure.
 *
 * @param ebth [in] pointer to the EB table handler structure
 *
 * @return 0 on success. 1 on failure.
 */
int ebt_handler_repopulate(ebt_handler *ebth) {
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

    unlink_handler_file(ebth->ebt_filter_file);
    unlink_handler_file(ebth->ebt_nat_file);
    unlink_handler_file(ebth->ebt_asc_file);

    return (0);
}

/**
 * Adds tablename table to this handler. No-op if the table is already present.
 *
 * @param ebth [in] pointer to the EB table handler structure
 * @param tablename [in] a string pointer to the table name
 *
 * @return 0 on success. 1 on failure.
 */
int ebt_handler_add_table(ebt_handler *ebth, char *tablename) {
    ebt_table *table = NULL;
    if (!ebth || !tablename || !ebth->init) {
        return (1);
    }

    LOGTRACE("adding table %s\n", tablename);
    table = ebt_handler_find_table(ebth, tablename);
    if (!table) {
        ebth->tables = realloc(ebth->tables, sizeof(ebt_table) * (ebth->max_tables + 1));
        if (!ebth->tables) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }
        bzero(&(ebth->tables[ebth->max_tables]), sizeof(ebt_table));
        snprintf(ebth->tables[ebth->max_tables].name, 64, "%s", tablename);
        ebth->max_tables++;
    }

    return (0);
}

/**
 * Adds chain chainname to table tablename with policy policyname and counter counters.
 * No-op if chainname is already present.
 *
 * @param ebth [in] pointer to the EB table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 * @param policyname [in] a string pointer to the default policy name to use (e.g. "DROP", "ACCEPT")
 * @param counters [in] a string pointer to the counter
 *
 * @return 0 on success. 1 on failure.
 */
int ebt_table_add_chain(ebt_handler *ebth, char *tablename, char *chainname, char *policyname, char *counters) {
    ebt_table *table = NULL;
    ebt_chain *chain = NULL;
    if (!ebth || !tablename || !chainname || !counters || !ebth->init) {
        return (1);
    }
    LOGTRACE("adding chain %s to table %s\n", chainname, tablename);
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

/**
 * Adds newrule to chain chainname in table tablename.
 *
 * @param ebth [in] pointer to the EB table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 * @param newrule [in] a string pointer to the new rule
 *
 * @return 0 on success. 1 on failure.
 */
int ebt_chain_add_rule(ebt_handler *ebth, char *tablename, char *chainname, char *newrule) {
    ebt_table *table = NULL;
    ebt_chain *chain = NULL;
    ebt_rule *rule = NULL;

    LOGTRACE("adding rules (%s) to chain %s to table %s\n", newrule, chainname, tablename);
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

/**
 * Update the chain reference counters.
 *
 * @param ebth [in] pointer to the EB table handler structure
 *
 * @return always 0.
 */
int ebt_handler_update_refcounts(ebt_handler *ebth) {
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
                            LOGTRACE("FOUND REF TO CHAIN (name=%s sourcechain=%s jumpchain=%s currref=%d) (rule=%s\n", refchain->name, chain->name, jumpchain, refchain->ref_count,
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

/**
 * Searches for table findtable.
 *
 * @param ebth [in] pointer to the EB table handler structure
 * @param findtable [in] a string pointer to the table name we're looking for
 *
 * @return pointer to ebt_table structure of interest if found. NULL otherwise.
 */
ebt_table *ebt_handler_find_table(ebt_handler *ebth, char *findtable) {
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

/**
 * Searches for chain findchain in table tablename.
 *
 * @param ebth [in] pointer to the EB table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param findchain [in] a string pointer to the chain name we're looking for
 *
 * @return pointer to ebt_chain structure of interest if found. NULL otherwise.
 */
ebt_chain *ebt_table_find_chain(ebt_handler *ebth, char *tablename, char *findchain) {
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

/**
 * Searches for rule findrule in chain chainname of table tablename.
 *
 * @param ebth [in] pointer to the EB table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 * @param findrule [in] a string pointer to the name of the rule we're looking for
 *
 * @return pointer to ebt_rule structure if found. NULL otherwise.
 */
ebt_rule *ebt_chain_find_rule(ebt_handler *ebth, char *tablename, char *chainname, char *findrule) {
    int i, found = 0;
    ebt_chain *chain;

    if (!ebth || !tablename || !chainname || !findrule || !ebth->init) {
        return (NULL);
    }

    chain = ebt_table_find_chain(ebth, tablename, chainname);
    if (!chain) {
        return (NULL);
    }

    for (i = 0; i < chain->max_rules; i++) {
        if (!strcmp(chain->rules[i].ebtrule, findrule))
            found++;
    }
    if (!found) {
        return (NULL);
    }
    return (&(chain->rules[i]));
}

/**
 * Remove all empty chains from table tablename.
 *
 * @param ebth [in] pointer to the EB table handler structure
 * @param tablename [in] a string pointer to the table name
 *
 * @return 0 on success. 1 on failure.
 */
int ebt_table_deletechainempty(ebt_handler *ebth, char *tablename) {
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

/**
 * Delete chains with partially matches chainmatch from table tablename.
 *
 * @param ebth [in] pointer to the EB table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainmatch [in] a string pointer to the list of characters to match
 *
 * @return 0 on success. 1 on failure.
 */
int ebt_table_deletechainmatch(ebt_handler *ebth, char *tablename, char *chainmatch) {
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

/**
 * Remove all rules from chain chainname in table tablename.
 *
 * @param ebth [in] pointer to the EB table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 *
 * @return 0 on success. 1 on failure.
 */
int ebt_chain_flush(ebt_handler *ebth, char *tablename, char *chainname) {
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

/**
 * Deletes a ebtables rule specified in the argument.
 *
 * @param ebth [in] pointer to the EB table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 * @param findrule [in] a string pointer to the rule to be deleted
 *
 * @return 0 if the rule given in the argument is successfully deleted. 1 otherwise.
 */
int ebt_chain_flush_rule(ebt_handler *ebth, char *tablename, char *chainname, char *findrule) {
    ebt_table *table = NULL;
    ebt_chain *chain = NULL;
    ebt_rule *rule = NULL;
    ebt_rule *newrules = NULL;
    int i;
    int nridx;

    if (!ebth || !tablename || !chainname || !findrule || !ebth->init) {
        return (EUCA_INVALID_ERROR);
    }

    table = ebt_handler_find_table(ebth, tablename);
    if (!table) {
        return (EUCA_INVALID_ERROR);
    }

    chain = ebt_table_find_chain(ebth, tablename, chainname);
    if (!chain) {
        return (EUCA_INVALID_ERROR);
    }

    rule = ebt_chain_find_rule(ebth, tablename, chainname, findrule);
    if (rule) {
        if (chain->max_rules > 1) {
            newrules = realloc(newrules, sizeof (ebt_rule) * (chain->max_rules - 1));
            if (!newrules) {
                LOGFATAL("out of memory!\n");
                exit(1);
            }

            bzero(newrules, sizeof (ebt_rule) * (chain->max_rules - 1));
            nridx = 0;
            for (i = 0; i < chain->max_rules; i++) {
                if (strcmp(chain->rules[i].ebtrule, findrule)) {
                    snprintf(newrules[nridx].ebtrule, 1024, "%s", chain->rules[i].ebtrule);
                    nridx++;
                }
            }
            EUCA_FREE(chain->rules);
            chain->rules = newrules;
            chain->max_rules = nridx;
        } else {
            EUCA_FREE(chain->rules);
            chain->max_rules = 0;
            chain->counters[0] = '\0';
        }
    } else {
        LOGDEBUG("Could not find (%s) from chain %s at table %s\n", findrule, chainname, tablename);
        return (2);
    }
    return (0);
}

/**
 * Releases resources allocated to this handler and re-initializes this handler.
 *
 * @param ebth [in] pointer to the EB table handler structure
 *
 * @return 0 on success. 1 on failure.
 */
int ebt_handler_free(ebt_handler *ebth) {
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

    return (ebt_handler_init(ebth, saved_cmdprefix));
}

/**
 * Releases all resources of the given ebt_handler.
 *
 * @param ebth [in] pointer to the EB table handler structure
 *
 * @return 0 on success. 1 otherwise.
 */
int ebt_handler_close(ebt_handler *ebth) {
    int i = 0;
    int j = 0;
    if (!ebth || !ebth->init) {
        LOGTRACE("Invalid argument. NULL or uninitialized ebt_handler.\n");
        return (1);
    }

    for (i = 0; i < ebth->max_tables; i++) {
        for (j = 0; j < ebth->tables[i].max_chains; j++) {
            EUCA_FREE(ebth->tables[i].chains[j].rules);
        }
        EUCA_FREE(ebth->tables[i].chains);
    }
    EUCA_FREE(ebth->tables);

    unlink_handler_file(ebth->ebt_filter_file);
    unlink_handler_file(ebth->ebt_nat_file);
    unlink_handler_file(ebth->ebt_asc_file);
    ebth->init = 0;

    return (0);
}

/**
 * Logs the current ebtables state in this handler.
 *
 * @param ebth [in] pointer to the EB table handler structure
 *
 * @return 0 on success or 1 if any failure occurred
 */
int ebt_handler_print(ebt_handler *ebth)
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
