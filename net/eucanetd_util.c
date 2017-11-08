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
#include <sys/time.h>
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
#include <execinfo.h>

#include <euca_file.h>

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ebt_handler.h"
#include "dev_handler.h"
#include "euca_gni.h"
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

static struct timeval gtv;

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
 * Stop the local DHCP server. It is assumed that dhcpd was started by eucanetd
 * and that it pid file reflects the real dhcpd system state.
 *
 * @param config [in] pointer to system-wide eucanetdConfig data structure 
 * @return 0 on success or 1 if a failure occurred
 *
 * @pre
 *     The DHCP server daemon must be present on the system and the 'config->dhcpDaemon'
 *     path must be properly set.
 *
 * @post
 *     on success, the DHCP server has been stopped.
 */
int eucanetd_stop_dhcpd_server(eucanetdConfig *config) {
    int rc = 0;

    char dhcpdunit[EUCA_MAX_PATH] = "";
    snprintf(dhcpdunit, EUCA_MAX_PATH, EUCANETD_DHCPD_UNIT, config->bridgeDev);
    char cmd[EUCA_MAX_PATH] = "";
    snprintf(cmd, EUCA_MAX_PATH, "%s %s stop %s", config->cmdprefix,
            config->systemctl, dhcpdunit);
    rc = timeshell_nb(cmd, 10, FALSE);
    if (rc != 0) {
        LOGWARN("failed to stop eucanetd-dhcpd\n");
    }

    return (rc);
}

/**
 * Restart or simply start the local DHCP server so it can pick up the new
 * configuration.
 *
 * @param config [in] pointer to system-wide eucanetdConfig data structure 
 * @return 0 on success or 1 if a failure occurred
 *
 * @pre
 *     The DHCP server daemon must be present on the system and the 'config->dhcpDaemon'
 *     path must be properly set.
 *
 * @post
 *     on success, the DHCP server has been restarted. If the configuration file does not
 *     contain any data, the DHCP server is stopped.
 */
int eucanetd_kick_dhcpd_server(eucanetdConfig *config) {
    int ret = 0;
    int rc = 0;
    char *psConfig = NULL;
    char sPidFileName[EUCA_MAX_PATH] = "";
    char sConfigFileName[EUCA_MAX_PATH] = "";
    char sLeaseFileName[EUCA_MAX_PATH] = "";
    char sTraceFileName[EUCA_MAX_PATH] = "";
    struct stat mystat = { 0 };

    // Setup the path to the various files involved
    snprintf(sPidFileName, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.pid", config->eucahome);
    snprintf(sLeaseFileName, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.leases", config->eucahome);
    snprintf(sTraceFileName, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.trace", config->eucahome);
    snprintf(sConfigFileName, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.conf", config->eucahome);

    eucanetd_stop_dhcpd_server(config);

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
            char dhcpdunit[EUCA_MAX_PATH] = "";
            snprintf(dhcpdunit, EUCA_MAX_PATH, EUCANETD_DHCPD_UNIT, config->bridgeDev);
            char cmd[EUCA_MAX_PATH] = "";
            snprintf(cmd, EUCA_MAX_PATH, "%s %s start %s", config->cmdprefix,
                    config->systemctl, dhcpdunit);
            rc = timeshell_nb(cmd, 10, FALSE);

            if (rc != 0) {
                LOGERROR("failed to start eucanetd-dhcpd\n");
                ret = 1;
            }
        }
        EUCA_FREE(psConfig);
    }

    return (ret);
}

/**
 * Run a daemonized program and maintain its state. If a PID file is given, it will check if the
 * process is currently running and if the running process matches the given program. If not, the
 * current process will be terminated and restarted with the new program. If the process is running
 * and matche our program name, it will be left alone. If the process is not currently running,
 * it will be started.
 *
 * @param psPidFilePath [in] a constant string pointer to the PID file path
 * @param psRootWrap [in] a constant string pointer to the rootwrap program location
 * @param force [in] set to TRUE if we want to kill the process regardless of its state and restart it. Otherwise set to FALSE.
 * @param psProgram [in] a constant string pointer to the pathname of a program which is to be executed
 * @param ... [in] the list of string arguments to pass to the program
 *
 * @return 0 on success or 1 on failure
 *
 * @pre
 *     - psProgram should not be NULL
 *     - There more be more than 1 variable argument provided
 *
 * @post
 *     On success, the program is executed and its PID is recorded in the psPidFilePath location if provided. If
 *     the process is already running, nothing will change. On failure, depending of where it occurred, the system
 *     is left into a non-deterministic state from the caller's perspective.
 *
 */
int eucanetd_run_program(const char *psPidFilePath, const char *psRootWrap, boolean force, const char *psProgram, ...) {
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

/**
 * Safely terminate a program executed by eucanetd_run_program().
 *
 * @param pid [in] the PID of the program to kill
 * @param psProgramName [in] a constant string pointer to the program name matching the pid
 * @param psRootwrap [in] a constant string pointer to the rootwrap program location
 *
 * @return 0 on success or 1 on failure
 *
 * @pre
 *     - psProgramName should not be NULL
 *     - The program should be running
 *
 * @post
 *     On success, the program is terminated. On failure, we can't tell what happened for sure.
 *
 */
int eucanetd_kill_program(pid_t pid, const char *psProgramName, const char *psRootwrap) {
    int status = 0;
    FILE *FH = NULL;
    char sPid[16] = "";
    char sSignal[16] = "";
    char cmdstr[EUCA_MAX_PATH] = "";
    char file[EUCA_MAX_PATH] = "";

    if (pid < 2) {
        return (EUCA_INVALID_ERROR);
    }

    if (psProgramName) {
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
    }

    // found running process
    if (!psProgramName || strstr(cmdstr, psProgramName)) {
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

/**
 * Removes a handler file on disk.
 *
 * @param Filename [in] of file to remove.
 *
 * @return 0 on success or 1 if any failure occurred
 *
 * @pre
 *    - Filename should not be NULL, but function can handle it. 
 */
int unlink_handler_file(char *filename) {
    if (!filename || !strlen(filename)) {
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
        return (errno);
    }
    return (0);
}

/**
 * Truncates a file on disk to 0 bytes, or creates the file if it doesn't exist
 *
 * @param Filename [in] of file to truncate or create
 *
 * @return 0 on success or 1 if any failure occurred
 *
 * @pre
 *    - filename should not be NULL
 *
 * @post
 *    - Filedescriptor associated with file is closed and not returned to the caller.
 */
int truncate_file(char *filename) {
    int fd = 0;
    
    if (!filename || !strlen(filename)) {
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

/**
 * Splits an input CIDR into network address and network mask (slashnet) parts.
 * If netmask is not found, /32 is assumed. Input is not validated (whether it
 * is a CIDR).
 *
 * @param ipname [in] a CIDR string (assumed to be in network address/slashnet)
 * @param ippart [out] Network address of the given CIDR.
 * @param nmpart [out] Slashnet of the given CIDR
 *
 * @return 0 on success, 1 otherwise.
 */
int cidrsplit(char *ipname, char **ippart, int *nmpart) {
    char *idx = NULL;
    char *ipname_dup = NULL;
    if (!ipname || !ippart || !nmpart) {
        LOGERROR("invalid input\n");
        return (1);
    }

    *ippart = NULL;
    *nmpart = 0;
    ipname_dup = strdup(ipname);
    idx = strchr(ipname_dup, '/');
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
        // nm part is not present, use /32
        *nmpart = 32;
        *ippart = strdup(ipname);
    }
    EUCA_FREE(ipname_dup);
    return (0);
}

/**
 * Retrieves a given device information (assigned IPs and NMS).
 *
 * @param dev [in] name of the device of interest
 * @param outips [out] list of found IP addresses
 * @param outnms [out] list of found subnet masks
 * @param len [out] number of entries in both lists (both lists have the same
 * number of entries).
 *
 * @return EUCA_OK on success and the out fields will be set properly. On failure the
 *         following error codes are returned:
 *         - EUCA_ERROR: if we fail to retrieve the interfaces addresses.
 *         - EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
 *
 * @pre dev, outips, outnms and len must not be NULL.
 *
 * @note
 * @todo replace with a better version.
 */
int getdevinfo(char *dev, u32 **outips, u32 **outnms, int *len) {
    int rc = 0;
    int count = 0;
    char host[NI_MAXHOST] = "";
    char buf[32] = "";
    void *tmpAddrPtr = NULL;
    struct ifaddrs *ifaddr = NULL;
    struct ifaddrs *ifa = NULL;
    struct sockaddr_in *ifs = NULL;

    if ((dev == NULL) || (outips == NULL) || (outnms == NULL) || (len == NULL))
        return (EUCA_INVALID_ERROR);

    if ((rc = getifaddrs(&ifaddr)) != 0) {
        return (EUCA_ERROR);
    }

    *outips = *outnms = NULL;
    *len = 0;

    count = 0;
    for (ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next) {
        if (!strcmp(dev, "all") || !strcmp(ifa->ifa_name, dev)) {
            if (ifa->ifa_addr && ifa->ifa_addr->sa_family == AF_INET) {
                if ((rc = getnameinfo(ifa->ifa_addr, sizeof(struct sockaddr_in), host, NI_MAXHOST, NULL, 0, NI_NUMERICHOST)) == 0) {
                    count++;

                    *outips = EUCA_REALLOC_C(*outips, count, sizeof(u32));
                    *outnms = EUCA_REALLOC_C(*outnms, count, sizeof(u32));

                    (*outips)[count - 1] = dot2hex(host);

                    ifs = ((struct sockaddr_in *)ifa->ifa_netmask);
                    tmpAddrPtr = &ifs->sin_addr;
                    if (inet_ntop(AF_INET, tmpAddrPtr, buf, 32)) {
                        (*outnms)[count - 1] = dot2hex(buf);
                    }
                }
            }
        }
    }

    freeifaddrs(ifaddr);
    *len = count;
    return (EUCA_OK);
}

/**
 * Retrieves all local IP addresses. This function is based on getifaddrs() call.
 * @param if_ips [i/o] pointer to an array of integers (each of which represents
 * an IP address
 * @param max_if_ips [i/o] number of entries in the array
 * @return 0 on success. 1 on failure. Upon success, an array of integers (each
 * integer represents an IPv4 address) is created in if_ips, and max_if_ips reflects
 * the number of entries in the created array.
 */
int euca_getifaddrs(u32 **if_ips, int *max_if_ips) {
    struct ifaddrs *ifas = NULL;
    struct ifaddrs *elem = NULL;
    u32 *res = NULL;
    int rc = 0;

    if (!if_ips || !max_if_ips) {
        LOGWARN("invalid argument: cannot fill NULL with if_ips\n");
        return (1);
    }
    if (*if_ips) {
        EUCA_FREE(*if_ips);
    }
    *max_if_ips = 0;

    rc = getifaddrs(&ifas);
    if (rc) {
        LOGERROR("unable to retrieve system IPv4 addresses.\n");
        freeifaddrs(ifas);
        return (1);
    }

    elem = ifas;
    while (elem) {
        if (elem->ifa_addr && elem->ifa_addr->sa_family == AF_INET) {
            struct sockaddr_in *saddr = (struct sockaddr_in *) elem->ifa_addr;
            u32 addr = ntohl(saddr->sin_addr.s_addr);
            // Skip link local addresses 169.254.0.0/16 and localhost addresses 127.0.0.0/8
            if (!((addr & 0xa9fe0000) == 0xa9fe0000) && !((addr & 0x7f000000) == 0x7f000000)) {
                res = EUCA_REALLOC_C(res, *max_if_ips + 1, sizeof (u32));
                res[*max_if_ips] = ntohl(saddr->sin_addr.s_addr);
                (*max_if_ips)++;
            }
        }
        elem = elem->ifa_next;
    }
    freeifaddrs(ifas);
    *if_ips = res;
    return (0);
}

/**
 * Computes the time difference between the time values in the argument (te - ts).
 *
 * @param ts [in] - start time.
 * @param te [in] - end time.
 *
 * @return time difference in milliseconds. If input argument is invalid, return 0.
 */
long int timer_get_interval_millis(struct timeval *ts, struct timeval *te) {
    if ((!ts) || (!te)) {
        return 0;
    }
    return (((te->tv_sec - ts->tv_sec) * 1000) + ((te->tv_usec - ts->tv_usec) / 1000));
}

/**
 * Computes the time difference between the time values in the argument (te - ts).
 *
 * @param ts [in] - start time.
 * @param te [in] - end time.
 *
 * @return time difference in microseconds. If input argument is invalid, return 0.
 */
long int timer_get_interval_usec(struct timeval *ts, struct timeval *te) {
    if ((!ts) || (!te)) {
        return 0;
    }
    return (((te->tv_sec - ts->tv_sec) * 1000000) + (te->tv_usec - ts->tv_usec));
}

/**
 * Reads the time since Epoch, and compute the difference with the time in
 * the argument. The timeval structure in the argument is updated for future
 * or subsequent calls.
 *
 * @param t [in] - the time since Epoch (start time). If NULL, the value 
 * stored in a static/global variable is used.
 * Be careful when using this - can lead to wrong results.
 * t will be updated with the current time.
 *
 * @return time difference in milliseconds.
 *
 * @pre start time must have been set with a call to start_timer().
 */
long int eucanetd_timer(struct timeval *t) {
    struct timeval cur;
    gettimeofday(&cur, NULL);
    long int ret = 0;
    if (!t) {
        ret = timer_get_interval_millis(&gtv, &cur);
        gtv.tv_sec = cur.tv_sec;
        gtv.tv_usec = cur.tv_usec;
    } else {
        ret = timer_get_interval_millis(t, &cur);
        t->tv_sec = cur.tv_sec;
        t->tv_usec = cur.tv_usec;
    }
    return ret;
}

/**
 * Reads the time since Epoch, and compute the difference with the time in
 * the argument. The timeval structure in the argument is updated for future
 * or subsequent calls.
 *
 * @param t [in] - the time since Epoch (start time). If NULL, the value 
 * stored in a static/global variable is used.
 * Be careful when using this - can lead to wrong results.
 * t will be updated with the current time.
 *
 * @return time difference in microseconds.
 *
 * @pre start time must have been set with a call to start_timer().
 */
long int eucanetd_timer_usec(struct timeval *t) {
    struct timeval cur;
    gettimeofday(&cur, NULL);
    long int ret = 0;
    if (!t) {
        ret = timer_get_interval_usec(&gtv, &cur);
        gtv.tv_sec = cur.tv_sec;
        gtv.tv_usec = cur.tv_usec;
    } else {
        ret = timer_get_interval_usec(t, &cur);
        t->tv_sec = cur.tv_sec;
        t->tv_usec = cur.tv_usec;
    }
    return ret;
}

/**
 * Returns the current timestamp based on gettimeofday();
 * @return current timestamp
 */
long int eucanetd_get_timestamp() {
    long int res = 0;
    struct timeval cur;
    gettimeofday(&cur, NULL);
    res = (long int) (cur.tv_sec * 1000) + (long int) (cur.tv_usec / 1000);
    return (res);
}

/**
 * Splits a separator separated list of words and creates an array of pointer to strings.
 * Memory is allocated for the array and each string pointed in the array.
 * If result has memory pre-allocated, it will be reallocated and/or overwritten.
 * Caller is responsible to release all memory (use free_ptrarr).
 * @param string [in] string to be split
 * @param result [out] pointer to an array of pointers to string
 * @param nmemb [out] pointer to integer with the number of elements in result
 * @param separator [in] separator character to be used
 * @return 0 on success. 1 on failure.
 */
int euca_split_string(char *string, char ***result, int *nmemb, char separator) {
    if (!string || !result || !nmemb) {
        LOGWARN("Invalid argument: cannot split NULL string\n");
        return (1);
    }
    char delim[2] = { 0 };
    char *input = strdup(string);
    char *saveptr = NULL;
    char **words = *result;

    snprintf(delim, 2, "%c", separator);
    char *token = strtok_r(input, delim, &saveptr);
    while (token) {
        words = EUCA_REALLOC_C(words, *nmemb + 1, sizeof (char *));
        words[*nmemb] = strdup(token);
        (*nmemb)++;
        token = strtok_r(NULL, delim, &saveptr);
    }
    
    *result = words;
    EUCA_FREE(input);
    return (0);
}

/**
 * Splits a space separated list of words and turn into an array of strings
 * @param str [in] list of space separated words
 * @return array of single words. The caller is responsible to release the memory.
 */
static char **strsplit_on_space(const char *str) {
    const char* delim = " ";
    size_t tokens_alloc = 1;
    size_t tokens_used = 0;
    char **tokens = calloc(tokens_alloc, sizeof(char*));
    char *token, *strtok_ctx;
    char *s = strdup(str);
    for (token = strtok_r(s, delim, &strtok_ctx); token != NULL; token = strtok_r(NULL, delim, &strtok_ctx)) {
        if (tokens_used == tokens_alloc) {
            tokens_alloc *= 2;
            tokens = realloc(tokens, tokens_alloc * sizeof(char*));
        }
        tokens[tokens_used++] = strdup(token);
    }
    free(s);
    if (tokens_used == 0) {
        free(tokens);
        tokens = NULL;
    } else {
        tokens = realloc(tokens, (tokens_used + 1) * sizeof(char*));
        tokens[tokens_used] = (char *)NULL;
    }

    return tokens;
}

/**
 * Forks a process to execute a command specified in the variable argument section.
 * Waits up to timeout_sec for the child process to exit.
 * Try to terminate/kill the child_process upon time out.
 * @param timeout_sec [in] time to wait for the child process executing the command of interest.
 * @param prefix [in] optional prefix of the command of interest - typically euca_rootwrap.
 * @param first [in] first string of the command of interest.
 * @param ... variable argument section.
 * @return EUCA_OK on success, EUCA_TIMEOUT_ERROR if the child process created to
 * execute the command does not exit within timeout_sec, EUCA_INVALID_ERROR if a
 * malformed command is specified.
 */
int euca_exec_wait(int timeout_sec, const char *prefix, const char *first, ...) {
    int result = 0;
    char **argv = NULL;

    va_list va;
    va_start(va, first);
    argv = build_argv(first, va);
    va_end(va);
    if (argv == NULL) {
        return EUCA_INVALID_ERROR;
    }

    pid_t pid;
    int status = 0;
    if (prefix) {
        int j = 0;
        for (; argv[j]; j++);
        argv = EUCA_REALLOC_C(argv, j + 2, sizeof (char *));
        memmove(&(argv[1]), &(argv[0]), (j + 1) * sizeof (char *));
        argv[0] = strdup(prefix);
    }
    result = euca_execvp_fd(&pid, NULL, NULL, NULL, argv);
    free_char_list(argv);
    
    if (timeout_sec > 0) {
        if (timewait(pid, &status, timeout_sec) == 0) {
            eucanetd_kill_program(pid, NULL, prefix);
            result = EUCA_TIMEOUT_ERROR;
        }
    }
            
    return result;
}

/**
 * Invokes euca_execvp_fd with passed command
 * @param command that should be executed with parameters. Parameters must be separated by one space.
 * @return exit code from executed script
 */
int euca_exec(const char *command) {
    int result = 0;
    pid_t pid;
    int pStatus = -1;
    char **args = strsplit_on_space(command);
    result = euca_execvp_fd(&pid, NULL, NULL, NULL, args);
    if (result == EUCA_OK) {
        result = euca_waitpid(pid, &pStatus);
    } else {
        LOGERROR("Failed to run %s\n", command);
    }
    free_char_list(args);
    return result;
}

/**
 * Inserts the given integer (value) to the integer array (set) in the argument.
 * @param set [i/o] pointer to an array of pointers to integers. This array should
 * not contain duplicates (i.e., a set data structure).
 * @param max_set [i/o] pointer to integer representing the number of entries in the set
 * @param value [in] integer to be inserted
 * @return 0 if the integer is inserted. -1 if the integer is already in the set. 1 on failure.
 */
int euca_u32_set_insert(u32 **set, int *max_set, u32 value) {
    u32 *result = NULL;
    if (!set || !max_set) {
        LOGWARN("Invalid argument: cannot process NULL set.\n");
        return (1);
    }
    result = *set;
    for (int i = 0; i < (*max_set); i++) {
        if (result[i] == value) {
            return (-1);
        }
    }
    result = EUCA_REALLOC_C(result, *max_set + 1, sizeof(u32));
    result[*max_set] = value;
    (*max_set)++;
    *set = result;
    return (0);
}

/**
 * Inserts the given string (value) to the string list (set) in the argument.
 * @param set [i/o] pointer to an array of pointers to strings. This array should
 * not contain duplicates (i.e., a set data structure).
 * @param max_set [i/o] pointer to integer representing the number of entries in the set
 * @param value [in] string to be inserted
 * @return 0 if the string is inserted. -1 if the string is already in the set. 1 on failure.
 */
int euca_string_set_insert(char ***set, int *max_set, char *value) {
    char **result = NULL;
    if (!set || !max_set || !value) {
        LOGWARN("Invalid argument: cannot process NULL string value or set.\n");
        return (1);
    }
    result = *set;
    for (int i = 0; i < (*max_set); i++) {
        if (!strcmp(result[i], value)) {
            return (-1);
        }
    }
    result = EUCA_REALLOC_C(result, *max_set + 1, sizeof(char *));
    result[*max_set] = strdup(value);
    (*max_set)++;
    *set = result;
    return (0);
}

/**
 * Searches the given string (value) to the string list (set) in the argument.
 * @param set [in] an array of pointers to strings. This array should
 * not contain duplicates (i.e., a set data structure).
 * @param max_set [in] number of entries in the set
 * @param value [in] string of interest
 * @return the string in the set if found. NULL otherwise.
 */
char *euca_string_set_get(char **set, int max_set, char *value) {
    if (!set || !value) {
        LOGWARN("Invalid argument: cannot process NULL string value or set.\n");
        return (NULL);
    }
    for (int i = 0; i < max_set; i++) {
        if (!strcmp(set[i], value)) {
            return (set[i]);
        }
    }
    return (NULL);
}

/**
 * Invokes calloc() and perform error checking.
 * @param nmemb [in] see calloc() man pages.
 * @param size [in] see calloc() man pages.
 * @return pointer to the allocated memory.
 */
void *zalloc_check(size_t nmemb, size_t size) {
    void *ret = calloc(nmemb, size);
    if ((ret == NULL) && ((nmemb * size) != 0)) {
        LOGFATAL("out of memory - alloc nmemb %zd, size %zd\n", nmemb, size);
        LOGFATAL("Shutting down eucanetd.\n");
        get_stack_trace();
        exit(1);
    }
    return (ret);
}

/**
 * Invokes realloc() and perform error checking.
 * @param nmemb [in] see calloc() man pages.
 * @param size [in] see calloc() man pages.
 * @return pointer to the allocated memory.
 */
void *realloc_check(void *ptr, size_t nmemb, size_t size) {
    void *ret = realloc(ptr, nmemb * size);
    if ((ret == NULL) && ((nmemb * size) != 0)) {
        LOGFATAL("out of memory - realloc nmemb %zd, size %zd.\n", nmemb, size);
        LOGFATAL("Shutting down eucanetd.\n");
        get_stack_trace();
        exit(1);
    }
    return (ret);
}

/**
 * Appends pointer ptr to the end of the given pointer array arr. The array should
 * have been malloc'd. The allocation is adjusted as needed.
 * @param arr [i/o] arr pointer to an array of pointers
 * @param max_arr [i/o] max_arr the number of array entries.
 * @param ptr (in] pointer to be appended to the array.
 * @return pointer to the re-allocated array.
 */
void *append_ptrarr(void *arr, int *max_arr, void *ptr) {
    arr = EUCA_REALLOC(arr, *max_arr + 1, sizeof (void *));
    if (arr == NULL) {
        LOGFATAL("out of memory: failed to (re)allocate array of pointers\n");
        LOGFATAL("Shutting down eucanetd.\n");
        get_stack_trace();
        exit (1);
    }
    void **parr = arr;
    parr[*max_arr] = ptr;
    (*max_arr)++;
    return (arr);    
}

/**
 * Compacts the given pointer array arr removing NULL pointers. The array should
 * have been malloc'd.
 * @param arr [i/o] arr pointer to an array of pointers
 * @param max_arr [i/o] max_arr the number of array entries (including NULL pointers).
 * @return pointer to the re-allocated array.
 */
void *compact_ptrarr(void *arr, int *max_arr) {
    void **parr = arr;
    void **res = NULL;
    int max_res = 0;
    for (int i = 0; i < *max_arr; i++) {
        if (!parr[i]) continue;
        res = EUCA_APPEND_PTRARR(res, &max_res, parr[i]);
    }
    EUCA_FREE(arr);
    *max_arr = max_res;
    return (res);
}

/**
 * Releases memory allocated for an array of pointers. Each element of the array
 * is assumed to have also allocated memory, which will also be released.
 * @param arr [in] array of pointers of interest
 * @param nmemb [in] number of elements in the array
 * @return 0 on success. 1 on any error.
 */
int free_ptrarr(void *arr, int nmemb) {
    if (!arr) {
        return (1);
    }
    if (!nmemb) {
        return (0);
    }
    void **a = arr;
    for (int i = 0; i < nmemb; i++) {
        free(a[i]);
    }
    free(a);
    return (0);
}

/**
 * Attempts to log the backtrace.
 * @param out [out] an array of strings that represents the backtrace.
 */
void get_stack_trace (void) {
    void *traces[20] = {0};
    size_t traces_len = 0;
    char **traces_str = NULL;

    traces_len = backtrace(traces, 20);
    traces_str = backtrace_symbols(traces, traces_len);
    if (traces_str) {
        LOGINFO("stack trace:\n");
        for (int i = 1; (i < traces_len) && traces_str; i++) {
            LOGINFO("\t%s\n", traces_str[i]);
        }
    }
    EUCA_FREE(traces_str);
}

/**
 * Converts Eucalyptus version in the form of Major.Minor.Patch.Hotfix (e.g., 4.3.0.1)
 * to binary 32-bit format. Missing optional numbers are filled with 0 (e.g, 4 becomes
 * 4.0.0.0). After 4th digit, additional digits are discarded (e.g., 4.3.0.1.0 is
 * considered 4.3.0.1). Each digit must be positive and less than 256.
 * @param ver [in] Eucalyptus version in String format.
 * @return binary representation of an Eucalyptus version. NULL input returns 0.
 */
u32 euca_version_dot2hex(const char *ver) {
    int a = 0;
    int b = 0;
    int c = 0;
    int d = 0;
    int rc = 0;

    if (ver == NULL) {
        return (0);
    }

    rc = sscanf(ver, "%d.%d.%d.%d", &a, &b, &c, &d);
    if ((rc == EOF) || (rc == 0) || ((a < 0) || (a > 255)) || ((b < 0) || (b > 255)) ||
            ((c < 0) || (c > 255)) || ((d < 0) || (d > 255))) {
        a = 0;
        b = 0;
        c = 0;
        d = 0;
    }

    a = a << 24;
    b = b << 16;
    c = c << 8;

    return (a | b | c | d);
}

/**
 * Produces output string according to format (printf() format) and copies to buf.
 * At most (buf_len - 1) characters of the produced output are copied. The pointer
 * buf is advanced to the end of the string, and buf_len adjusted accordingly.
 * @param buf [i/o] pointer to a string where output will be stored. This pointer
 * is updated to point to the end of the string.
 * @param buf_len [i/o] Pointer to length of buffer, which should be free to use.
 * This length is updated depending on the number of characters written to buf.
 * @param format [in] format string
 * @param ... [in] the variable argument part of the format
 * @return the number of characters written to buf. Negative value on error.
 */
int euca_buffer_snprintf(char **buf, int *buf_len, const char *format, ...) {
    int rc = 0;
    va_list ap = { {0} };

    if (!buf || !(*buf) || !buf_len || !format) {
        return (0);
    }

    if (*buf_len > 0) {
        va_start(ap, format);

        rc = vsnprintf(*buf, *buf_len, format, ap);
        if (rc > 0) {
            *buf_len -= rc;
            *buf = *buf + rc;
        }

        va_end(ap);
    }

    return (rc);
}

