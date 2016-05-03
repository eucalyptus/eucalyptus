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

#ifdef USE_IP_ROUTE_HANDLER
//!
//! @file net/ipr_handler.h
//! Implements the IP Rule Handler API.
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
#include <euca_string.h>
#include <sequence_executor.h>
#include <atomic_file.h>

#include "ipt_handler.h"
#include "ipr_handler.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name IP Rule Operation Bitmask values

#define IPR_OPS_POPULATED                        0x00000001  //!< Indicates this rule was populated
#define IPR_OPS_ADD                              0x00000002  //!< Indicates we need to add this rule
#define IPR_OPS_KEEP                             IPR_OPS_ADD //!< Indicates we need to keep this rule
#define IPR_OPS_FLUSH                           ~IPR_OPS_ADD //!< Mask use to clear the ADD/KEEP bit for deletion
#define IPR_OPS_MASK                             0xFFFFFFFF  //!< Entire field mask

//! @}

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

static int ipr_handler_add_rule_static(ipr_handler * pIprh, const char *psRule, boolean fromPopulate);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name IP Rule Operation Macros

#define POPULATE_RULE(_pRule)                    (_pRule)->operation |= IPR_OPS_POPULATED
#define ADD_RULE(_pRule)                         (_pRule)->operation |= IPR_OPS_ADD
#define KEEP_RULE(_pRule)                        (_pRule)->operation |= IPR_OPS_KEEP
#define FLUSH_RULE(_pRule)                       (_pRule)->operation &= IPR_OPS_FLUSH

#define NEED_ADD(_pRule)                         (((_pRule)->operation & (IPR_OPS_POPULATED | IPR_OPS_ADD)) == IPR_OPS_ADD)
#define NEED_FLUSH(_pRule)                       (((_pRule)->operation & IPR_OPS_ADD) == 0)

//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Initialize the given IP Rule handler structure.
//!
//! @param[in] pIprh pointer to the IP Rule handler structure
//! @param[in] psCmdPrefix a string pointer to the prefix to use to run commands (usually path to rootwrap)
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!     - The pIprh parameter MUST not be NULL
//!     - We should be able to create a temporary file under /tmp/
//!     - We should be able to change the permissions of that temporary file
//!     - We should be able to execute the "ip rule" command
//!
//! @post
//!     - On success, a temporary file is created and permissions are set to 0600
//!     - On success ONLY, the structure is initialized and the 'initialized' field is set to true
//!
//! @note
//!
int ipr_handler_init(ipr_handler * pIprh, const char *psCmdPrefix)
{
    int fd = 0;

    // Make sure we got the pointer correctly
    if (!pIprh) {
        LOGERROR("invalid input\n");
        return (1);
    }
    // if we're initialized, free
    if (pIprh->initialized) {
        return (ipr_handler_free(pIprh));
    }
    // Zero out our structure properly
    bzero(pIprh, sizeof(ipr_handler));

    // Create our temporary file to dump the rules when we work them
    snprintf(pIprh->sIpRuleFile, EUCA_MAX_PATH, "/tmp/ipr_file-XXXXXX");
    if ((fd = safe_mkstemp(pIprh->sIpRuleFile)) < 0) {
        LOGERROR("cannot create tmpfile '%s': check permissions\n", pIprh->sIpRuleFile);
        return (1);
    }
    // Change the permissions on that temporary file
    if (chmod(pIprh->sIpRuleFile, 0600)) {
        LOGWARN("chmod failed: was able to create tmpfile '%s', but could not change file permissions\n", pIprh->sIpRuleFile);
    }
    // Now close this file descriptor
    close(fd);

    // If we were given a prefix for the commands, set it here (usually path to rootwrap
    if (psCmdPrefix) {
        snprintf(pIprh->sCmdPrefix, EUCA_MAX_PATH, "%s", psCmdPrefix);
    }
    // test required shell-outs
    if (euca_execlp(NULL, pIprh->sCmdPrefix, "ip", "rule", "list", NULL) != EUCA_OK) {
        LOGERROR("could not execute ip rule list. check command/permissions\n");

        // Lets clean up the temporary file and empty the file name
        unlink(pIprh->sIpRuleFile);
        pIprh->sIpRuleFile[0] = '\0';
        return (1);
    }
    // We're good to go
    pIprh->initialized = TRUE;
    return (0);
}

//!
//! Populates the IP Rule Structure with what's on the system
//!
//! @param[in] pIprh pointer to the IP Rule handler structure
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!     - The pIprh pointer must not be NULL
//!     - The IP Rule structure must be initialized
//!     - We should be able to list the IP rules
//!     - We should be able to read the temporary file content
//!
//! @post
//!     - On success, the IP rule structure is populated
//!     - The temporary file remains
//!
//! @note
//!
int ipr_handler_repopulate(ipr_handler * pIprh)
{
#define LINE_LEN    1024

    int i = 0;
    int rc = 0;
    char *pString = NULL;
    char sCommand[EUCA_MAX_PATH] = "";
    char sLineBuffer[LINE_LEN] = "";
    FILE *pFileHandler = NULL;

    // Make sure our pointer is valid and that we're initialized
    if (!pIprh || !pIprh->initialized) {
        return (1);
    }
    // Reset our structure
    if ((rc = ipr_handler_free(pIprh)) != 0) {
        return (1);
    }
    // Save our IP rules
    if (euca_execlp_redirect(NULL, NULL, pIprh->sIpRuleFile, FALSE, NULL, FALSE, pIprh->sCmdPrefix, "ip", "rule", "list", NULL) != EUCA_OK) {
        LOGERROR("ip rule listing failed\n");
        return (1);
    }
    // Now open our file to be processed
    if ((pFileHandler = fopen(pIprh->sIpRuleFile, "r")) == NULL) {
        LOGERROR("Could not open file for read '%s': check permissions\n", pIprh->sIpRuleFile);
        return (1);
    }
    // Go line by line
    while (fgets(sLineBuffer, LINE_LEN, pFileHandler)) {
        // Replace the new line with the null character
        if ((pString = strchr(sLineBuffer, '\n'))) {
            (*pString) = '\0';
        }
        // Trim the spaces at the end of the line
        for (i = strlen(sLineBuffer) - 1; ((i >= 0) && (sLineBuffer[i] == ' ')); i--) {
            sLineBuffer[i] = '\0';
        }

        // Make sure we have at least 1 character (perhaps it was a new empty line)
        if (strlen(sLineBuffer) < 1) {
            continue;
        }
        // We only care if the rules are about our private or public table
        if ((strstr(sLineBuffer, "euca_private") == NULL) && (strstr(sLineBuffer, "euca_public") == NULL)) {
            continue;
        }
        // Skip the priority
        if ((pString = strchr(sLineBuffer, ':')) != NULL) {
            // Trim at the front
            for (pString++, i = strlen(pString); ((((*pString) == ' ') || ((*pString) == '\t')) && (i >= 0)); i--) {
                pString++;
            }

            // Make sure it wasn't just blank space
            if (strlen(pString) > 1) {
                // Now add this rule to our list of rule
                if (ipr_handler_add_rule_static(pIprh, pString, TRUE)) {
                    LOGWARN("Fail to populate rule: (%s)\n", pString);
                }
            }
        }
    }

    fclose(pFileHandler);
    return (0);

#undef LINE_LEN
}

//!
//! Flush the IP Rule found in the structure
//!
//! @param[in] pIprh pointer to the IP Rule handler structure
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!     - The pIprh pointer must not be NULL
//!     - The IP Rule structure must be initialized
//!
//! @post
//!     - On success all rules are marked for deletion on deploy
//!
//! @note
//!
int ipr_handler_flush(ipr_handler * pIprh)
{
    int i = 0;

    // Make sure our pointer is valid and that we're initialized
    if (!pIprh || !pIprh->initialized) {
        return (1);
    }
    // Go through all our rules and mark them for deletion
    for (i = 0; i < pIprh->nbRules; i++) {
        FLUSH_RULE(&pIprh->pRuleList[i]);
    }

    return (0);
}

//!
//! Deploy the list of IP rules
//!
//! @param[in] pIprh pointer to the IP Rule handler structure
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!     - The pIprh pointer must not be NULL
//!     - The IP Rule structure must be initialized
//!
//! @post
//!     - On success all rules are being deployed
//!     - If the parameters are valid and regardless of the deploy result, the temporary file is unlinked
//!     - On any deployment failure the temporary file is copied to /tmp/euca_ipr_file_failed
//!
//! @note
//!
int ipr_handler_deploy(ipr_handler * pIprh)
{
    int i = 0;
    int rc = 0;
    int ret = 0;
    char sCommand[EUCA_MAX_PATH] = "";
    ipr_rule *pRule = NULL;
    sequence_executor iprExecutor = { {0} };

    // Make sure our pointer is valid and that we're initialized
    if (!pIprh || !pIprh->initialized) {
        return (1);
    }
    // Initialize the sequence executor
    if ((rc = se_init(&iprExecutor, pIprh->sCmdPrefix, 2, 1)) != 0) {
        LOGERROR("Failed to initialize our sequence executor!\n");
        return (1);
    }
    // Go through all our rules and mark them for deletion
    for (i = 0, pRule = pIprh->pRuleList; i < pIprh->nbRules; i++, pRule++) {
        // What are we doing with this rule?
        if (NEED_ADD(pRule)) {
            snprintf(sCommand, EUCA_MAX_PATH, "ip rule add %s", pIprh->pRuleList[i].name);
        } else if (NEED_FLUSH(pRule)) {
            snprintf(sCommand, EUCA_MAX_PATH, "ip rule del %s", pIprh->pRuleList[i].name);
        } else {
            // Nothing to do
            continue;
        }

        // could we add it?
        if ((rc = se_add(&iprExecutor, sCommand, NULL, ignore_exit2)) != 0) {
            LOGWARN("Fail to deploy ip rule '%s'. rc=%d\n", sCommand, rc);
            ret = 1;
        }
    }

    // Now try to push what we have
    se_print(&iprExecutor);
    if ((rc = se_execute(&iprExecutor)) != 0) {
        LOGERROR("could not execute command sequence (check above log errors for details): ip rules.\n");
        ret = 1;
    }
    se_free(&iprExecutor);

    // Did we have any failure
    if (ret) {
        LOGERROR("some ip rule failed: copying failed input file to '/tmp/euca_ipr_file_failed'.\n");
        if ((rc = copy_file(pIprh->sIpRuleFile, "/tmp/euca_ipr_file_failed")) != 0) {
            LOGERROR("could not copy file %s to %s. rc=%d\n", pIprh->sIpRuleFile, "/tmp/euca_ipr_file_failed", rc);
        }
    }

    unlink(pIprh->sIpRuleFile);
    return (ret);
}

//!
//! Add a new IP rule
//!
//! @param[in] pIprh pointer to the IP Rule handler structure
//! @param[in] psRule constant string pointer to the rule to add
//!
//! @return 0 on success or 1 on failure
//!
//! @see ipr_handler_add_rule_static()
//!
//! @pre
//!     - The pIprh pointer must not be NULL
//!     - The IP Rule structure must be initialized
//!
//! @post
//!     - If the rule exists we make sure its not marked for deletion
//!     - If the rule does not exists, we add it to our list
//!
//! @note
//!
int ipr_handler_add_rule(ipr_handler * pIprh, const char *psRule)
{
    return (ipr_handler_add_rule_static(pIprh, psRule, FALSE));
}

//!
//! Add a new IP rule
//!
//! @param[in] pIprh pointer to the IP Rule handler structure
//! @param[in] psRule constant string pointer to the rule to add
//! @param[in] fromPopulate set to TRUE if this is call from ipr_handler_repopulate()
//!
//! @return 0 on success or 1 on failure
//!
//! @see ipr_handler_repopulate(), ipr_handler_add_rule()
//!
//! @pre
//!     - The pIprh pointer must not be NULL
//!     - The IP Rule structure must be initialized
//!
//! @post
//!     - If the rule exists we make sure its not marked for deletion
//!     - If the rule does not exists, we add it to our list
//!     - The proper operation flag is set
//!
//! @note
//!
static int ipr_handler_add_rule_static(ipr_handler * pIprh, const char *psRule, boolean fromPopulate)
{
    ipr_rule *pRule = NULL;

    // Make sure our pointer is valid and that we're initialized
    if (!pIprh || !pIprh->initialized) {
        return (1);
    }
    // If the rule already exists, we'll keep it
    if ((pRule = ipr_handler_find_rule(pIprh, psRule)) != NULL) {
        KEEP_RULE(pRule);
        return (0);
    }
    // Re-allocate our memory
    if ((pIprh->pRuleList = EUCA_REALLOC(pIprh->pRuleList, (pIprh->nbRules + 1), sizeof(ipr_rule))) == NULL) {
        LOGFATAL("out of memory!\n");
        exit(1);
    }

    // Set our rule pointer to make things simple
    pRule = &(pIprh->pRuleList[pIprh->nbRules]);

    // Clear the structure
    bzero(pRule, sizeof(ipr_rule));

    // Copy the rule content
    snprintf(pRule->name, EUCA_MAX_PATH, psRule);

    // Mark it as addition or populated based on the fromPopulate flag
    if (fromPopulate) {
        POPULATE_RULE(pRule);
    } else {
        ADD_RULE(pRule);
    }

    // Now we have one more rule in our list
    pIprh->nbRules++;
    return (0);
}

//!
//! Removes a given IP rule from our list
//!
//! @param[in] pIprh pointer to the IP Rule handler structure
//! @param[in] psRule constant string pointer to the rule to remove
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!     - The pIprh pointer must not be NULL
//!     - The IP Rule structure must be initialized
//!
//! @post
//!     - If the rule exists we will remove it
//!     - If the rule does not exists, its a no-op and we'll return success
//!
//! @note
//!
int ipr_handler_del_rule(ipr_handler * pIprh, const char *psRule)
{
    ipr_rule *pRule = NULL;

    // Make sure our pointer is valid and that we're initialized
    if (!pIprh || !pIprh->initialized) {
        return (1);
    }
    // Do we have this rule?
    if ((pRule = ipr_handler_find_rule(pIprh, psRule)) == NULL) {
        return (0);
    }
    // Yes, flush it
    FLUSH_RULE(pRule);
    return (0);
}

//!
//! Finds a given rule in our list of ip rules
//!
//! @param[in] pIprh pointer to the IP Rule handler structure
//! @param[in] psRule constant string pointer to the rule we're looking for
//!
//! @return A pointer to the rule structure if found or NULL otherwise
//!
//! @see
//!
//! @pre
//!     - The pIprh and psRule pointers must not be NULL
//!     - The IP Rule structure must be initialized
//!
//! @post
//!
//! @note
//!
ipr_rule *ipr_handler_find_rule(ipr_handler * pIprh, const char *psRule)
{
    int i = 0;
    ipr_rule *pRule = NULL;

    // Make sure our pointer is valid and that we're initialized
    if (!pIprh || !psRule || !pIprh->initialized) {
        return (NULL);
    }
    // Go through all our rules and mark them for deletion
    for (i = 0, pRule = pIprh->pRuleList; i < pIprh->nbRules; i++, pRule++) {
        // Do we have a match?
        if (!strcmp(pRule->name, psRule)) {
            return (pRule);
        }
    }

    // Yikes!!! Not found
    return (NULL);
}

//!
//! Frees the content of the given IP Rule Handler structure
//!
//! @param[in] pIprh pointer to the IP Rule handler structure
//!
//! @return 1 on failure or the result of ipr_handler_init() API
//!
//! @see ipr_handler_init()
//!
//! @pre
//!     - The pIprh pointer must not be NULL
//!     - The IP Rule structure must be initialized
//!
//! @post
//!     - The structure content is deallocated and the structure is re-initialized
//!     - If the given pointer is valid and any failure occured, the initialized field will remain FALSE.
//!
//! @note
//!
int ipr_handler_free(ipr_handler * pIprh)
{
    char sCmdPrefix[EUCA_MAX_PATH] = "";

    // Make sure we have a valid pointer
    if (!pIprh || !pIprh->initialized) {
        return (1);
    }
    // No longer initialized
    pIprh->initialized = FALSE;

    // Save our command prefix for the re-init
    snprintf(sCmdPrefix, EUCA_MAX_PATH, "%s", pIprh->sCmdPrefix);
    pIprh->sCmdPrefix[0] = '\0';

    // Get rid of our list
    EUCA_FREE(pIprh->pRuleList);
    pIprh->nbRules = 0;

    // Unlink our temporary file in case it hasn't been freed already
    unlink(pIprh->sIpRuleFile);
    pIprh->sIpRuleFile[0] = '\0';

    // Re-initialize our structure
    return (ipr_handler_init(pIprh, sCmdPrefix));
}

//!
//! Logs the content of the IP rule list.
//!
//! @param[in] pIprh pointer to the IP Rule handler structure
//!
//! @return 0 on success or 1 on failure
//!
//! @see
//!
//! @pre
//!     - The pIprh pointer must not be NULL
//!     - The IP Rule structure must be initialized
//!
//! @post
//!     - Nothing has changed
//!
//! @note
//!
int ipr_handler_print(ipr_handler * pIprh)
{
    int i = 0;

    // Make sure we have a valid pointer
    if (!pIprh || !pIprh->initialized) {
        LOGINFO("Invalid IP Rule handler structure provided.\n");
        return (1);
    }

    if (log_level_get() == EUCA_LOG_TRACE) {
        for (i = 0; i < pIprh->nbRules; i++) {
            LOGTRACE("IPRULE NAME: %s\n", pIprh->pRuleList[i].name);
        }
    }
    return (0);
}
#endif /* USE_IP_ROUTE_HANDLER */
