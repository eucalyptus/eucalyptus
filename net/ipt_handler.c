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

#include "ipt_handler.h"
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
 * Initialize an IP table handler structure
 *
 * @param pIpt [in] pointer to the IP table handler structure
 * @param psCmdPrefix [in] a constant pointer to a string containing the prefix for EUCA commands
 * @param psPreloadPath [in] a constant pointer to a string containing the path to the IP table preload file
 *
 * @return 0 on success or 1 if any failure occurred
 *
 * @see ipt_handler_free()
 *
 * @pre
 *     - The pIpt pointer should not be NULL
 *     - We should be able to create temporary files on the system
 *     - We should be able to execute the iptables commands
 *
 * @post
 *     On success, the IP table structure will be initialized with the following:
 *     - The ipt_file will point to a temporary file under /tmp/ipt_file-XXXXXX
 *     - If psCmdPrefix was provided, the table's cmdprefix field will be set with it
 *     - If psPreloadPath was provided, the structure's preloadPath will be set with it
 *     -
 *
 * @note
 *     - Once temporary file is initialized the filename will be reused throughout the process
 *       lifetime. The file will be truncated/created on each successive calls to the *_handler_init()
 *       method. 
 */
int ipt_handler_init(ipt_handler *pIpt, const char *psCmdPrefix, const char *psPreloadPath) {
    int fd = 0;
    char sTempFileName[EUCA_MAX_PATH] = "";  // Used to temporarily hold name while we zero out the struct

    // Make sure our pointers are valid
    if (!pIpt) {
        return (1);
    }

    //
    // Initialize the temporary file *ONCE* per process execution
    // This handler init function is called many times, but the temporary file
    // will always be the same for the associated handler struct.
    //
    if (pIpt->init) {
        //
        // Copy filename out of the current ipt_handler struct.
        //
        snprintf(sTempFileName, EUCA_MAX_PATH, "%s", pIpt->ipt_file);
        
        //  Truncate the file to ensure we're dealing with a clean slate.
        if (truncate_file(sTempFileName)){
           return (1);
        }
        LOGDEBUG("Using already allocated temporary filename: %s\n", sTempFileName);
        
    } else {
        //
        // Initialize a new temporaty file name
        //
        snprintf(sTempFileName, EUCA_MAX_PATH, "/tmp/ipt_file-XXXXXX");
        if ((fd = safe_mkstemp(sTempFileName)) < 0) {
            LOGERROR("cannot create tmpfile '%s': check permissions\n", sTempFileName);
            return (1);
        }

        // Check to see if we can set the permissions to 0600
        if (chmod(sTempFileName, 0600) < 0) {
            LOGWARN("chmod failed: ipt_file '%s' errno: %d\n", sTempFileName, errno);
        }

        LOGDEBUG("Using newly created temporary filename: %s\n", sTempFileName);
        close(fd);
    }

    // Empty this structure
    bzero(pIpt, sizeof(ipt_handler));

    // Populate the temporary filename
    snprintf(pIpt->ipt_file, EUCA_MAX_PATH, "%s", sTempFileName);
    
    // If we have a command prefix (like euca_rootwrap) set it
    pIpt->cmdprefix[0] = '\0';
    if (psCmdPrefix) {
        snprintf(pIpt->cmdprefix, EUCA_MAX_PATH, "%s", psCmdPrefix);
    }
    // If we have a preload file path, set it.
    pIpt->preloadPath[0] = '\0';
    if (psPreloadPath) {
        snprintf(pIpt->preloadPath, EUCA_MAX_PATH, "%s", psPreloadPath);
    }
    // test required shell-outs
    if (euca_execlp_redirect(NULL, NULL, "/dev/null", FALSE, "/dev/null", FALSE, pIpt->cmdprefix, "iptables-save", NULL) != EUCA_OK) {
        LOGERROR("could not execute iptables-save. check command/permissions\n");
        return (1);
    }

    pIpt->init = 1;
    return (0);
}

/**
 * Runs iptables-save and store the content in our configured IP table file
 *
 * @param pIpt [in] pointer to the IP table handler structure
 *
 * @return 0 on success or any other values if any failure occurred
 *
 * @see ipt_system_restore()
 *
 * @pre
 *     - pIpt MUST not be NULL
 *     - We should be able to write to the configured IP table structure temporary file
 *
 * @post
 *     On success, the content from iptables-save is stored in ipth->ipt_file. On failure,
 *     the destination file should remain unchanged.
 *
 * @note
 */
int ipt_system_save(ipt_handler *pIpt) {
    if (euca_execlp_redirect(NULL, NULL, pIpt->ipt_file, FALSE, NULL, FALSE, pIpt->cmdprefix, "iptables-save", "-c", NULL) != EUCA_OK) {
        LOGERROR("iptables-save failed\n");
        return EUCA_ERROR;
    }
    return EUCA_OK;
}

/**
 * Runs the iptables-restore program provided with our IP table configured file.
 *
 * @param pIpt [in] pointer to the IP table handler structure
 *
 * @return 0 on success or any other value if any failure occurred
 *
 * @see ipt_system_save()
 *
 * @pre
 *     - pIpt MUST not be NULL
 *     - The IP table structure temporary file must exists on the system
 *
 * @post
 *     On success, the system IP tables have been restored with the content from our
 *     configured file. On failure, the system IP tables should remain unchanged and
 *     the content of the file saved in /tmp/euca_ipt_file_failed.
 *
 * @note
 */
int ipt_system_restore(ipt_handler *pIpt) {
    int rc = EUCA_OK;
    if (euca_execlp_redirect(NULL, pIpt->ipt_file, NULL, FALSE, NULL, FALSE, pIpt->cmdprefix, "iptables-restore", "-c", NULL) != EUCA_OK) {
        copy_file(pIpt->ipt_file, "/tmp/euca_ipt_file_failed");
        LOGERROR("iptables-restore failed. copying failed input file to '/tmp/euca_ipt_file_failed' for manual retry.\n");
        rc = EUCA_ERROR;
    }
    unlink_handler_file(pIpt->ipt_file);
    return (rc);
}

/**
 * Takes our latest IP table virtual content and puts it into a file in IP tables format that
 * will be passed to ip_system_restore(). Once completed, the system IP tables should contain
 * the latest changes we made.
 *
 * @param pIpt [in] pointer to the IP table handler structure
 *
 * @return 0 on success or any other value if any failure occurred
 *
 * @see ipt_system_restore()
 *
 * @pre
 *     - Our given pointers must not be NULL
 *     - The IP table structure must have been intialized
 *     - The system must allow us to write to the file configured in the IP table structure
 *
 * @post
 *     On success, the system IP tables will contain what we put in our structure. On failure, the
 *     system IP tables should remain unchanged.
 *
 * @note
 */
int ipt_handler_deploy(ipt_handler * pIpt) {
    int i = 0;
    int j = 0;
    int k = 0;
    char *psPreload = NULL;
    FILE *pFh = NULL;

    if (!pIpt || !pIpt->init) {
        return (1);
    }

    ipt_handler_update_refcounts(pIpt);

    if ((pFh = fopen(pIpt->ipt_file, "w")) == NULL) {
        LOGERROR("could not open file for write '%s': check permissions\n", pIpt->ipt_file);
        return (1);
    }
    // do the preload stuff first if needed
    if (strlen(pIpt->preloadPath)) {
        if ((psPreload = file2str(pIpt->preloadPath)) == NULL) {
            LOGTRACE("Fail to load IP table preload content from '%s'.\n", pIpt->preloadPath);
        } else {
            fprintf(pFh, "%s\n", psPreload);
            EUCA_FREE(psPreload);
        }
    }

    for (i = 0; i < pIpt->max_tables; i++) {
        fprintf(pFh, "*%s\n", pIpt->tables[i].name);
        for (j = 0; j < pIpt->tables[i].max_chains; j++) {
            if (!pIpt->tables[i].chains[j].flushed && pIpt->tables[i].chains[j].ref_count) {
                fprintf(pFh, ":%s %s %s\n", pIpt->tables[i].chains[j].name, pIpt->tables[i].chains[j].policyname, pIpt->tables[i].chains[j].counters);
            }
        }
        for (j = 0; j < pIpt->tables[i].max_chains; j++) {
            if (!pIpt->tables[i].chains[j].flushed && pIpt->tables[i].chains[j].ref_count) {
                // qsort!
                qsort(pIpt->tables[i].chains[j].rules, pIpt->tables[i].chains[j].max_rules, sizeof(ipt_rule), ipt_ruleordercmp);
                for (k = 0; k < pIpt->tables[i].chains[j].max_rules; k++) {
                    if (!pIpt->tables[i].chains[j].rules[k].flushed) {
                        fprintf(pFh, "%s %s\n", pIpt->tables[i].chains[j].rules[k].counterstr, pIpt->tables[i].chains[j].rules[k].iptrule);
                    }
                }
            }
        }
        fprintf(pFh, "COMMIT\n");
    }
    fclose(pFh);
    return (ipt_system_restore(pIpt));
}

/**
 * Compares two given rules to see which comes first
 *
 * @param p1 [in] a pointer to the left hand side IP table rule
 * @param p2 [in] a pointer to the right hand side IP table rule
 *
 * @return 0 if p1 an p2 are of the same order, -1 if p1 comes before p2 and 1 if p2 comes before p1.
 */
int ipt_ruleordercmp(const void *p1, const void *p2) {
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

/**
 * Retrieves the current iptables system state.
 *
 * @param ipth [in] pointer to the IP table handler structure
 *
 * @return 0 on success or 1 if any failure occurred
 */
int ipt_handler_repopulate(ipt_handler *ipth) {
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
    struct timeval tv = { 0 };
    //  long long int countersa, countersb;

    eucanetd_timer_usec(&tv);
    if (!ipth || !ipth->init) {
        LOGERROR("Invalid argument: cannot reinitialize NULL ipt handler.\n");
        return (1);
    }

    rc = ipt_handler_free(ipth);
    if (rc) {
        LOGERROR("could not reinitialize ipt handler.\n");
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

    unlink_handler_file(ipth->ipt_file);
    LOGDEBUG("ipt populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (0);
}

/**
 * Adds table tablename. No-op if tablename is already present.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param tablename [in] a string pointer to the table name
 *
 * @return 0 on success or 1 if any failure occurred
 *
 */
int ipt_handler_add_table(ipt_handler *ipth, char *tablename) {
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
        snprintf(ipth->tables[ipth->max_tables].name, 64, "%s", tablename);
        ipth->max_tables++;
    }

    return (0);
}

/**
 * Add chain chainname to table tablename with policy policyname and counters.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 * @param policyname [in] a string pointer to the policy to apply
 * @param counters [in] a string pointer to the counters
 *
 * @return 0 on success or 1 if any failure occurred
 */
int ipt_table_add_chain(ipt_handler *ipth, char *tablename, char *chainname, char *policyname, char *counters) {
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
                !strcmp(table->chains[table->max_chains].name, "PREROUTING") ||
                !strcmp(table->chains[table->max_chains].name, "POSTROUTING")) {
            table->chains[table->max_chains].ref_count = 1;
        }
        chain = &(table->chains[table->max_chains]);
        table->max_chains++;
    }
    chain->flushed = 0;

    return (0);
}

/**
 * Add rule newrule to chain chainname in table tablename.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 * @param newrule [in] a string pointer to the rule to add
 *
 * @return 0 on success or 1 if any failure occurred
 */
int ipt_chain_add_rule(ipt_handler *ipth, char *tablename, char *chainname, char *newrule) {
    return (ipt_chain_add_rule_with_counters(ipth, tablename, chainname, newrule, NULL));
}

/**
 * Add rule newrule with counter counterstr to chain chainname in table tablename.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 * @param newrule [in] a string pointer to the rule to add
 * @param counterstr [in] a string pointer to the counters
 *
 * @return 0 on success or 1 if any failure occurred
 */
int ipt_chain_add_rule_with_counters(ipt_handler *ipth, char *tablename, char *chainname, char *newrule, char *counterstr) {
    return (ipt_chain_insert_rule(ipth, tablename, chainname, newrule, counterstr, IPT_ORDER));
}

/**
 * Inserts rule newrule to chain chainname in table tablename with counter counterstr
 * in the position dictated by order.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 * @param newrule [in] a string pointer to the new rule
 * @param counterstr [in] a string pointer to the counters
 * @param order [in] the order in which to insert this rule
 *
 * @return 0 on success or 1 if any failure occurred
 */
int ipt_chain_insert_rule(ipt_handler *ipth, char *tablename, char *chainname, char *newrule, char *counterstr, int order) {
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

/**
 * Update reference conts.
 *
 * @param ipth [in] pointer to the IP table handler structure
 *
 * @return Always returns 0
 */
int ipt_handler_update_refcounts(ipt_handler *ipth) {
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

                if (0 == rule->flushed) {
                    jumpptr = strstr(rule->iptrule, "-j");
                    if (jumpptr) {
                        jumpchain[0] = '\0';
                        sscanf(jumpptr, "%[-j] %s", tmp, jumpchain);
                        if (strlen(jumpchain)) {
                            refchain = ipt_table_find_chain(ipth, table->name, jumpchain);
                            if (refchain) {
                                LOGDEBUG("FOUND REF TO CHAIN (name=%s sourcechain=%s jumpchain=%s currref=%d) (rule=%s)\n",
                                        refchain->name, chain->name, jumpchain, refchain->ref_count, rule->iptrule);
                                refchain->ref_count++;
                            }
                        }
                    }
                }
            }
        }
    }
    return (0);
}

/**
 * Searches for table named findtable.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param findtable [in] a string pointer to the name of the table we're looking for
 *
 * @return a pointer to the IP table structure if found. Otherwise, NULL is returned
 */
ipt_table *ipt_handler_find_table(ipt_handler *ipth, const char *findtable)
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

/**
 * Searches for chain named findchain in table tablename.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param findchain [in] a string pointer to the chain name we're looking for
 *
 * @return a pointer to the IP table chain structure if found. Otherwise, NULL is returned
 */
ipt_chain *ipt_table_find_chain(ipt_handler *ipth, const char *tablename, const char *findchain) {
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

/**
 * Finds a given IPT chain in a given IPT and set its default policy.
 *
 * @param pIpth [in] pointer to the IP table handler structure
 * @param tablename [in] a constant string pointer to the name of the table (e.g. 'filter', 'nat', 'mangle', etc.)
 * @param chainname [in] a constant string pointer to the name of the chain (e.g. 'INPUT', 'FORWARD', etc.)
 * @param policyname [in] a constant string pointer to the policy name (e.g. 'ACCEPT', 'DROP')
 *
 * @return 0 on success or 1 if any failure occurred
 *
 * @pre \li All pointers and strings must not be NULL
 *      \li The referred table and chain must exists
 *
 * @post On success the chain default policy has been changed. On failure, the original policy
 *       will remain.
 */
int ipt_table_set_chain_policy(ipt_handler * pIpth, const char *tablename, const char *chainname, const char *policyname)
{
    ipt_table *pTable = NULL;
    ipt_chain *pChain = NULL;

    if (!pIpth || !tablename || !chainname || !pIpth->init) {
        return (1);
    }

    if ((pTable = ipt_handler_find_table(pIpth, tablename)) == NULL) {
        return (1);
    }

    if ((pChain = ipt_table_find_chain(pIpth, tablename, chainname)) != NULL) {
        snprintf(pChain->policyname, 64, "%s", policyname);
    }

    return (0);
}

/**
 * Searches for rule findrule in chain chainname in table tablename.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 * @param findrule [in] a string pointer to the rule we're looking for
 *
 * @return a pointer to the IP table rule structure if found. Otherwise, NULL is returned
 */
ipt_rule *ipt_chain_find_rule(ipt_handler *ipth, char *tablename, char *chainname, char *findrule) {
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

/**
 * Remove all empty chains from table tablename.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param tablename [in] a string pointer to the table name
 *
 * @return 0 on success or 1 if any failure occurred
 *
 * @see
 *
 * @pre
 *
 * @post
 *
 * @note
 */
int ipt_table_deletechainempty(ipt_handler *ipth, char *tablename) {
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

/**
 * Remove all chains that partially match chainmatch from table tablename.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainmatch [in] a string pointer to the list of characters to match
 *
 * @return 0 on success or 1 if any failure occurred
 */
int ipt_table_deletechainmatch(ipt_handler *ipth, char *tablename, char *chainmatch) {
    int i = 0;
    ipt_table *table = NULL;
    ipt_chain *chain = NULL;

    if (!ipth || !tablename || !chainmatch || !ipth->init) {
        return (1);
    }

    table = ipt_handler_find_table(ipth, tablename);
    if (!table) {
        return (1);
    }

    for (i = 0; i < table->max_chains; i++) {
        if (strstr(table->chains[i].name, chainmatch)) {
            chain = &(table->chains[i]);
            ipt_chain_flush(ipth, tablename, chain->name);
            chain->flushed = 1;
        }
    }

    return (0);
}

/**
 * Mark all rules in the chain chain name of table tablename as flushed.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 *
 * @return 0 on success or 1 if any failure occurred
 */
int ipt_chain_flush(ipt_handler *ipth, char *tablename, char *chainname) {
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

/**
 * Flushes/removes the rule of interest from a chain.
 *
 * @param ipth [in] pointer to the IP table handler structure
 * @param tablename [in] a string pointer to the table name
 * @param chainname [in] a string pointer to the chain name
 * @param findrule [in] a string pointer to the rule we're looking for
 *
 * @return 0 on success or 1 if any failure occurred
 */
int ipt_chain_flush_rule(ipt_handler *ipth, char *tablename, char *chainname, char *findrule) {
    int i, found = 0;
    ipt_chain *chain;

    if (!ipth || !tablename || !chainname || !findrule || !ipth->init) {
        return (EUCA_INVALID_ERROR);
    }

    chain = ipt_table_find_chain(ipth, tablename, chainname);
    if (!chain) {
        return (EUCA_INVALID_ERROR);
    }

    for (i = 0; i < chain->max_rules && !found; i++) {
        if (!strcmp(chain->rules[i].iptrule, findrule)) {
            found++;
            chain->rules[i].flushed = 1;
            chain->rules[i].order = 0;
        }
    }
    if (!found) {
        return (EUCA_NOT_FOUND_ERROR);
    }
    return (EUCA_OK);
}

/**
 * Releases resources allocated for ipth and re-initializes ipth.
 *
 * @param ipth [in] pointer to the IP table handler structure
 *
 * @return 0 on success or 1 if any failure occurred
 */
int ipt_handler_free(ipt_handler *ipth) {
    int i = 0;
    int j = 0;
    char saved_cmdprefix[EUCA_MAX_PATH] = "";
    char saved_preloadPath[EUCA_MAX_PATH] = "";

    if (!ipth || !ipth->init) {
        return (1);
    }
    snprintf(saved_cmdprefix, EUCA_MAX_PATH, "%s", ipth->cmdprefix);
    snprintf(saved_preloadPath, EUCA_MAX_PATH, "%s", ipth->preloadPath);

    for (i = 0; i < ipth->max_tables; i++) {
        for (j = 0; j < ipth->tables[i].max_chains; j++) {
            EUCA_FREE(ipth->tables[i].chains[j].rules);
        }
        EUCA_FREE(ipth->tables[i].chains);
    }
    EUCA_FREE(ipth->tables);

    return (ipt_handler_init(ipth, saved_cmdprefix, saved_preloadPath));
}

/**
 * Release all resources associated with the given ipt_handler.
 *
 * @param ipth [in] pointer to the IP table handler structure
 *
 * @return 0 on success or 1 if any failure occurred
 */
int ipt_handler_close(ipt_handler *ipth) {
    int i = 0;
    int j = 0;

    if (!ipth || !ipth->init) {
        LOGDEBUG("Invalid argument. NULL or uninitialized ipt_handler.\n");
        return (1);
    }

    for (i = 0; i < ipth->max_tables; i++) {
        for (j = 0; j < ipth->tables[i].max_chains; j++) {
            EUCA_FREE(ipth->tables[i].chains[j].rules);
        }
        EUCA_FREE(ipth->tables[i].chains);
    }
    EUCA_FREE(ipth->tables);
    unlink_handler_file(ipth->ipt_file);
    ipth->init = 0;
    return (0);
}

/**
 * Logs the contents of ipth.
 *
 * @param ipth [in] pointer to the IP table handler structure
 *
 * @return 0 on success. 1 on failure.
 */
int ipt_handler_print(ipt_handler *ipth) {
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

