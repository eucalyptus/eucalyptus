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
//! @file util/sequence_executor.c
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

#include <misc.h>

#include "sequence_executor.h"

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
//! @param[in] se
//! @param[in] cmdprefix
//! @param[in] default_timeout
//! @param[in] clean_only_on_fail
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
int se_init(sequence_executor * se, char *cmdprefix, int default_timeout, int clean_only_on_fail)
{
    if (!se) {
        return (1);
    }

    if (!memset(se, 0, sizeof(sequence_executor))) {
        return (1);
    }

    se->init = 1;
    if (default_timeout > 0) {
        se->default_timeout = default_timeout;
    } else {
        se->default_timeout = 1;
    }

    se->clean_only_on_fail = clean_only_on_fail;
    if (cmdprefix) {
        snprintf(se->cmdprefix, EUCA_MAX_PATH, "%s", cmdprefix);
    } else {
        se->cmdprefix[0] = '\0';
    }

    return (0);
}

//!
//! Function description.
//!
//! @param[in] se
//! @param[in] command
//! @param[in] cleanup_command
//! @param[in] checker
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
int se_add(sequence_executor * se, char *command, char *cleanup_command, void *checker)
{
    char cmd[EUCA_MAX_PATH] = "";

    if (!se || !se->init) {
        return (1);
    }

    if (command) {
        snprintf(cmd, EUCA_MAX_PATH, "%s %s", se->cmdprefix, command);
        se->commands[se->max_commands] = strdup(cmd);
    } else {
        se->commands[se->max_commands] = NULL;
    }

    if (cleanup_command) {
        snprintf(cmd, EUCA_MAX_PATH, "%s %s", se->cmdprefix, cleanup_command);
        se->cleanup_commands[se->max_commands] = strdup(cmd);
    } else {
        se->cleanup_commands[se->max_commands] = NULL;
    }

    if (checker) {
        se->checkers[se->max_commands] = checker;
    } else {
        se->checkers[se->max_commands] = NULL;
    }

    se->max_commands++;
    return (0);
}

//!
//! Function description.
//!
//! @param[in] se
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
int se_print(sequence_executor * se)
{
    int i = 0;

    if (!se || !se->init) {
        return (1);
    }

    for (i = 0; i < se->max_commands; i++) {
        LOGDEBUG("COMMAND SEQUENCE PRINT: idx='%d' command='%s' cleanup_command='%s'\n", i, se->commands[i], se->cleanup_commands[i]);
    }
    return (0);
}

//!
//! Function description.
//!
//! @param[in] se
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
int se_execute(sequence_executor * se)
{
    int i = 0;
    int rc = 0;
    int failed = 0;
    int lastran = 0;
    int ret = 0;
    char out[1024] = "";
    char err[1024] = "";

    if (!se || !se->init) {
        return (1);
    }

    ret = 0;
    failed = 0;

    for (i = 0; i < se->max_commands; i++) {
        LOGDEBUG("RUNNING COMMAND: command='%s'\n", se->commands[i]);
        rc = timeshell(se->commands[i], out, err, 1024, se->commands_timers[i] ? se->commands_timers[i] : se->default_timeout);
        lastran = i;

        if (se->checkers[i]) {
            rc = se->checkers[i] (rc, out, err);
        }

        if (rc) {
            LOGERROR("COMMAND FAILED: exitcode='%d' command='%s' stdout='%s' stderr='%s'\n", rc, se->commands[i], out, err);
            failed = 1;
            break;
        } else {
            LOGDEBUG("COMMAND SUCCESS: command='%s'\n", se->commands[i]);
        }
    }

    if (se->clean_only_on_fail && failed) {
        for (i = lastran; i >= 0; i--) {
            if (se->cleanup_commands[i]) {
                LOGDEBUG("RUNNING CLEANUP_COMMAND: command='%s'\n", se->cleanup_commands[i]);
                rc = system(se->cleanup_commands[i]);
                rc = rc >> 8;
            }
        }
    }

    if (failed) {
        ret = 1;
    }
    return (ret);
}

//!
//! Function description.
//!
//! @param[in] se
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
int se_free(sequence_executor * se)
{
    int i = 0;

    if (!se || !se->init) {
        return (1);
    }

    for (i = 0; i < se->max_commands; i++) {
        if (se->commands[i])
            free(se->commands[i]);

        if (se->cleanup_commands[i])
            free(se->cleanup_commands[i]);
    }
    return (se_init(se, se->cmdprefix, se->default_timeout, se->clean_only_on_fail));
}

//!
//! Function description.
//!
//! @param[in] rc
//! @param[in] stdoutbuf
//! @param[in] stderrbuf
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
int ignore_exit(int rc, char *stdoutbuf, char *stderrbuf)
{
    return (0);
}

//!
//! Function description.
//!
//! @param[in] rc
//! @param[in] stdoutbuf
//! @param[in] stderrbuf
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
int ignore_exit2(int rc, char *stdoutbuf, char *stderrbuf)
{
    if (rc && (rc != 2)) {
        return (rc);
    }
    return (0);
}
