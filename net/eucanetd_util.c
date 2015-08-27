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
//! @file net/eucanetd_util.c
//! Implementation of various generic system utility specific APIs not found
//! under the util module. Every function exposed here must start with the
//! "eucanetd_" string.
//!
//! Coding Standard:
//! Every function that has multiple words must follow the word1_word2_word3() naming
//! convention and variables must follow the 'word1Word2Word3()' convention were no
//! underscore is used and every word, except for the first one, starts with a capitalized
//! letter. Whenever possible (not mendatory but strongly encouraged), prefixing a variable
//! name with one or more of the following qualifier would help reading code:
//!     - p - indicates a variable is a pointer (example: int *pAnIntegerPointer)
//!     - s - indicates a string variable (examples: char sThisString[10], char *psAnotherString). When 's' is used on its own, this mean a static string.
//!     - a - indicates an array of objects (example: int aAnArrayOfInteger[10])
//!     - g - indicates a variable with global scope to the file or application (example: static eucanetdConfig gConfig)
//!
//! Any other function implemented must have its name start with "eucanetd" followed by an underscore
//! and the rest of the function name with every words separated with an underscore character. For
//! example: eucanetd_this_is_a_good_function_name().
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pwd.h>
#include <dirent.h>
#include <errno.h>
#include <ctype.h>
#include <math.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <pthread.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdarg.h>
#include <ifaddrs.h>
#include <math.h>
#include <sys/socket.h>
#include <netdb.h>
#include <arpa/inet.h>

#include <sys/ioctl.h>
#include <net/if.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <log.h>
#include <hash.h>
#include <math.h>
#include <http.h>
#include <config.h>
#include <sequence_executor.h>
#include <atomic_file.h>

#include <euca_file.h>

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ebt_handler.h"
#include "dev_handler.h"
#include "eucanetd_config.h"
#include "euca_gni.h"
#include "euca_lni.h"
#include "eucanetd.h"
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

//!
//! Restart or simply start the local DHCP server so it can pick up the new
//! configuration.
//!
//! @return 0 on success or 1 if a failure occured
//!
//! @see
//!
//! @pre
//!     The DHCP server daemon must be present on the system and the 'config->dhcpDaemon'
//!     path must be properly set.
//!
//! @post
//!     on success, the DHCP server has been restarted. If the configuration file does not
//!     contain any data, the DHCP server is stopped.
//!
//! @note
//!
int eucanetd_kick_dhcpd_server(void)
{
    int ret = 0;
    int rc = 0;
    int pid = 0;
    int status = 0;
    char *psPid = NULL;
    char *psConfig = NULL;
    char sPidFileName[EUCA_MAX_PATH] = "";
    char sConfigFileName[EUCA_MAX_PATH] = "";
    char sLeaseFileName[EUCA_MAX_PATH] = "";
    char sTraceFileName[EUCA_MAX_PATH] = "";
    struct stat mystat = { 0 };

    // Do we have a valid path?
    if (stat(config->dhcpDaemon, &mystat) != 0) {
        LOGERROR("Unable to find DHCP daemon binaries: '%s'\n", config->dhcpDaemon);
        return (1);
    }
    // Setup the path to the various files involved
    snprintf(sPidFileName, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.pid", config->eucahome);
    snprintf(sLeaseFileName, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.leases", config->eucahome);
    snprintf(sTraceFileName, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.trace", config->eucahome);
    snprintf(sConfigFileName, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.conf", config->eucahome);

    // Retrieve the PID of the current DHCP server process if running
    if (stat(sPidFileName, &mystat) == 0) {
        psPid = file2str(sPidFileName);
        pid = atoi(psPid);
        EUCA_FREE(psPid);

        // If the PID value is valid, kill the server
        if (pid > 1) {
            LOGDEBUG("attempting to kill old dhcp daemon (pid=%d)\n", pid);
            if ((rc = safekillfile(sPidFileName, config->dhcpDaemon, 9, config->cmdprefix)) != 0) {
                LOGWARN("failed to kill previous dhcp daemon\n");
            }
        }
    }
    // Check to make sure the lease file is present
    if (stat(sLeaseFileName, &mystat) != 0) {
        // nope, just create an empty one
        LOGDEBUG("creating stub lease file (%s)\n", sLeaseFileName);
        if ((rc = touch(sLeaseFileName)) != 0) {
            LOGWARN("cannot create empty leasefile\n");
        }
    }
    // We should be able to load the configuration file
    if ((psConfig = file2str(sConfigFileName)) != NULL) {
        // Do we have any "node-" statement
        if (strstr(psConfig, "node-")) {
            // Run the DHCP command
            rc = euca_execlp(&status, config->cmdprefix, config->dhcpDaemon, "-cf", sConfigFileName, "-lf", sLeaseFileName, "-pf", sPidFileName, "-tf", sTraceFileName, NULL);
            if (rc != EUCA_OK) {
                LOGERROR("Fail to restart DHCP server. exitcode='%d'\n", status);
                LOGDEBUG("DHCPD Server Command='%s %s %s %s %s %s %s %s %s %s'\n", config->cmdprefix, config->dhcpDaemon, "-cf", sConfigFileName, "-lf", sLeaseFileName, "-pf",
                         sPidFileName, "-tf", sTraceFileName)
                    ret = 1;
            } else {
                LOGDEBUG("DHCP server restarted successfully\n");
            }
        }
        EUCA_FREE(psConfig);
    }

    return (ret);
}

//!
//! Run a daemonized program and maintain its state. If a PID file is given, it will check if the
//! process is currently running and if the running process matches the given program. If not, the
//! current process will be terminated and restarted with the new program. If the process is running
//! and matche our program name, it will be left alone. If the process is not currently running,
//! it will be started.
//!
//! @param[in] psPidFilePath a constant string pointer to the PID file path
//! @param[in] psRootWrap a constant string pointer to the rootwrap program location
//! @param[in] force set to TRUE if we want to kill the process regardless of its state and restart it. Otherwise set to FALSE.
//! @param[in] psProgram a constant string pointer to the pathname of a program which is to be executed
//! @param[in] ... the list of string arguments to pass to the program
//!
//! @return 0 on success or 1 on failure
//!
//! @pre
//!     - psProgram should not be NULL
//!     - There more be more than 1 variable argument provided
//!
//! @post
//!     On success, the program is executed and its PID is recorded in the psPidFilePath location if provided. If
//!     the process is already running, nothing will change. On failure, depending of where it occured, the system
//!     is left into a non-deterministic state from the caller's perspective.
//!
//! @note
//!
//! @todo
//!     We should move this to something more global under util/euca_system.[ch]
//!
int eucanetd_run_program(const char *psPidFilePath, const char *psRootWrap, boolean force, const char *psProgram, ...)
{
#define PID_STRING_LEN       32

    int i = 0;
    int rc = 0;
    char *psPidId = NULL;
    char *pString = NULL;
    char **argv = NULL;
    char sPid[PID_STRING_LEN] = "";
    char sFilePath[EUCA_MAX_PATH] = "";
    char sCommand[EUCA_MAX_PATH] = "";
    const char *psProgramName = psProgram;
    FILE *pFh = NULL;
    pid_t pid = 0;
    boolean found = FALSE;
    va_list va = { {0} };

    // Make sure we know what app we are running
    if (!psProgram) {
        return (1);
    }
    // turn variable arguments into a array of strings for the euca_execvp_fd()
    va_start(va, psProgram);
    {
        argv = build_argv(psProgram, va);
    }
    va_end(va);

    // Make sure we have a valid arg list
    if (argv == NULL)
        return (1);

    //
    // Set the psProgramName properly. If its currently the rootwrap program, then move to the
    // next argument in the list
    //
    if (!strcmp(psProgram, psRootWrap)) {
        // We should have another argument or I don't see how we can run rootwrap without something else?!?!?
        if (argv[1] == NULL) {
            free_char_list(argv);
            return (1);
        }
        // We're good, use the next argument
        psProgramName = argv[1];
    }
    // Do we need to check if we have the exact same program running?
    if (psPidFilePath) {
        found = FALSE;

        // Does the PID file exists?
        if ((rc = check_file(psPidFilePath)) == 0) {
            //
            // read and make sure the command matches. If it does not match, we will need to restart.
            //
            if ((psPidId = file2str(psPidFilePath)) != NULL) {
                snprintf(sFilePath, EUCA_MAX_PATH, "/proc/%s/cmdline", psPidId);
                // Check if the process is running
                if (check_file(sFilePath) == 0) {
                    // read the old command and make sure we have the same command running
                    if ((pFh = fopen(sFilePath, "r")) != NULL) {
                        if (fgets(sCommand, EUCA_MAX_PATH, pFh)) {
                            if (strstr(sCommand, psProgramName)) {
                                // process is running, and is indeed psProgram
                                found = TRUE;
                            }
                        }
                        fclose(pFh);
                    }
                }

                EUCA_FREE(psPidId);
            }

            if (found) {
                // pidfile passed in and process is already running
                if (force) {
                    // kill process and remove pidfile
                    LOGTRACE("Stopping '%s'\n", psProgramName);
                    rc = safekillfile(psPidFilePath, psProgramName, 9, psRootWrap);
                } else {
                    // nothing to do
                    LOGTRACE("Program '%s' running properly. Nothing to do.\n", psProgramName);
                    free_char_list(argv);
                    return (0);
                }
            } else {
                // pidfile passed in but process is not running
                unlink(psPidFilePath);
            }
        }

    }
    // Build the command string for debugging purpose
    for (i = 0, pString = sCommand; argv[i] != NULL; i++) {
        pString += snprintf(pString, (EUCA_MAX_PATH - (pString - sCommand)), "%s ", argv[i]);
    }

    rc = euca_execvp_fd(&pid, NULL, NULL, NULL, argv);
    LOGTRACE("Executed '%s'. PID=%d, RC=%d\n", sCommand, pid, rc);
    free_char_list(argv);

    if (psPidFilePath) {
        snprintf(sPid, PID_STRING_LEN, "%d", pid);
        rc = write2file(psPidFilePath, sPid);
    }
    return (rc);

#undef PID_STRING_LEN
}

//!
//! Safely terminate a program executed by eucanetd_run_program().
//!
//! @param[in] pid the PID of the program to kill
//! @param[in] psProgramName a constant string pointer to the program name matching the pid
//! @param[in] psRootwrap a constant string pointer to the rootwrap program location
//!
//! @return 0 on success or 1 on failure
//!
//! @pre
//!     - psProgramName should not be NULL
//!     - The program should be running
//!
//! @post
//!     On success, the program is terminated. On failure, we can't tell what happened for sure.
//!
//! @note
//!
//! @todo
//!     We should move this to something more global under util/euca_system.[ch]
//!
int eucanetd_kill_program(pid_t pid, const char *psProgramName, const char *psRootwrap)
{
    int status = 0;
    FILE *FH = NULL;
    char sPid[16] = "";
    char sSignal[16] = "";
    char cmdstr[EUCA_MAX_PATH] = "";
    char file[EUCA_MAX_PATH] = "";

    if ((pid < 2) || !psProgramName) {
        return (EUCA_INVALID_ERROR);
    }

    snprintf(file, EUCA_MAX_PATH, "/proc/%d/cmdline", pid);
    if (check_file(file)) {
        return (EUCA_PERMISSION_ERROR);
    }

    if ((FH = fopen(file, "r")) != NULL) {
        if (!fgets(cmdstr, EUCA_MAX_PATH, FH)) {
            fclose(FH);
            return (EUCA_ACCESS_ERROR);
        }
        fclose(FH);
    } else {
        return (EUCA_ACCESS_ERROR);
    }

    // found running process
    if (strstr(cmdstr, psProgramName)) {
        // passed in cmd matches running cmd
        if (psRootwrap) {
            snprintf(sPid, 16, "%d", pid);
            snprintf(sSignal, 16, "-%d", SIGTERM);
            euca_execlp(NULL, psRootwrap, "kill", sSignal, sPid, NULL);
            if (timewait(pid, &status, 1) == 0) {
                LOGERROR("child process {%u} failed to terminate. Attempting SIGKILL.\n", pid);
                snprintf(sSignal, 16, "-%d", SIGKILL);
                euca_execlp(NULL, psRootwrap, "kill", sSignal, sPid, NULL);
                if (timewait(pid, &status, 1) == 0) {
                    LOGERROR("child process {%u} failed to KILL. Attempting SIGKILL again.\n", pid);
                    euca_execlp(NULL, psRootwrap, "kill", sSignal, sPid, NULL);
                    if (timewait(pid, &status, 1) == 0) {
                        return (1);
                    }
                }
            }
        } else {
            kill(pid, SIGTERM);
            if (timewait(pid, &status, 1) == 0) {
                LOGERROR("child process {%u} failed to terminate. Attempting SIGKILL.\n", pid);
                kill(pid, SIGKILL);
                if (timewait(pid, &status, 1) == 0) {
                    LOGERROR("child process {%u} failed to KILL. Attempting SIGKILL again.\n", pid);
                    kill(pid, SIGKILL);
                    if (timewait(pid, &status, 1) == 0) {
                        return (1);
                    }
                }
            }
        }
    }

    return (0);
}

//!
//! Removes a handler file on disk.
//!
//! @param[in] Filename of file to remove.
//!
//! @return 0 on success or 1 if any failure occured
//!
//! @see
//!
//! @pre
//!    - Filename should not be NULL, but function can handle it. 
//!
//! @post
//!
//! @note
//!
int unlink_handler_file(char *filename)
{
    if (filename == NULL) {
        LOGTRACE("NULL filename passed\n");
        return (0);
    }

    if (access(filename, F_OK) == 0) {
        if (unlink(filename) < 0) {
            LOGERROR("Unable to unlink handler file: %s errno: %d\n", filename, errno);
            return (1);
        } else {
            LOGTRACE("File %s removed successfully\n",filename);
        }
    } else {
        LOGWARN("File: %s doesn't exist or is not accessible by user. errno: %d\n",filename, errno);
    }
    return (0);
}

//!
//! Truncates a file on disk to 0 bytes, or creates the file if it doesn't exist
//!
//! @param[in] Filename of file to truncate or create
//!
//! @return 0 on success or 1 if any failure occured
//!
//! @see
//!
//! @pre
//!    - filename should not be NULL
//!
//! @post
//!    - Filedescriptor associated with file is closed and not returned to the caller.
//!
//! @note 
//!
int truncate_file(char *filename) {
    int fd = 0;
    
    if (filename == NULL) {
        LOGTRACE("NULL filename passed\n");
        return (0);
    }
    if ((fd = open(filename, O_CREAT|O_TRUNC|O_WRONLY, S_IRUSR|S_IWUSR)) < 0) {
        LOGERROR("Unable to truncate file: %s errno: %d\n", filename, errno);
        return (1);
    } else {
        LOGTRACE("File %s truncated successfully\n",filename);
    }
    close(fd);
    return (0);
}
