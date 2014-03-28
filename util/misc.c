// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
//! @file util/misc.c
//! Implements a variety of utility tools
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64           // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <string.h>                    // strlen, strcpy
#include <ctype.h>                     // isspace
#include <assert.h>
#include <stdarg.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/vfs.h>
#include <unistd.h>
#include <time.h>
#include <math.h>                      // powf
#include <fcntl.h>                     // open
#include <utime.h>                     // utime
#include <sys/wait.h>
#include <pwd.h>
#include <dirent.h>                    // opendir, etc
#include <sys/errno.h>                 // errno
#include <sys/time.h>                  // gettimeofday
#include <limits.h>
#include <sys/mman.h>                  // mmap
#include <pthread.h>

#include "eucalyptus.h"

#include <diskutil.h>
#include <vnetwork.h>

#include "misc.h"
#include "euca_auth.h"
#include "log.h"
#include "euca_string.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define OUTSIZE                                    50

#define DEV_STR_DELIMITER                         ","
#define DEV_STR_IQNS_DELIMITER                    "|"
#define DEV_STR_KEY_VAL_DELIMITER                 "="

#ifdef _UNIT_TEST
#define _STR                                     "a lovely string"
#endif /* _UNIT_TEST */

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

static char *next_tag(const char *xml, int *start, int *end, int *single, int *closing);
static char *find_cont(const char *xml, char *xpath);

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
//! Given an array of pointers to command names (e.g., "ls", "dd", etc.),
//! as well as either an array of NULL pointers or pointers to full paths,
//! either the full paths are verified or system $PATH is searched for
//! the names and full paths are pointed to by helpers_path[] entries.
//!
//! @param[in] helpers
//! @param[in] helpers_path
//! @param[in] num_helpers
//!
//! @return Number of missing entries is returned or -1 for error.
//!
int verify_helpers(char **helpers, char **helpers_path, int num_helpers)
{
    int i = 0;
    int j = 0;
    int rc = 0;
    int done = 0;
    int missing_helpers = 0;
    char *tok = NULL;
    char *toka = NULL;
    char *path = NULL;
    char *helper = NULL;
    char *save = NULL;
    char *savea = NULL;
    char *euca = NULL;
    char *newpath = NULL;
    char file[EUCA_MAX_PATH] = "";
    char lpath[EUCA_MAX_PATH] = "";
    char **tmp_helpers_path = helpers_path;
    struct stat statbuf = { 0 };
    char *locations[] = {
        ":" EUCALYPTUS_LIBEXEC_DIR,
        ":" EUCALYPTUS_HELPER_DIR,
        ":" EUCALYPTUS_SBIN_DIR,
        NULL
    };

    if (helpers_path == NULL)
        tmp_helpers_path = (char **)EUCA_ZALLOC(num_helpers, sizeof(char *));

    for (i = 0; i < num_helpers; i++) {
        // full path was given, so it just needs to be verified
        if (helpers_path != NULL && helpers_path[i] != NULL) {
            rc = stat(helpers_path[i], &statbuf);
            if (!rc && S_ISREG(statbuf.st_mode)) {
                done++;
            }
        } else {                       // no full path was given, so search $PATH
            if ((tok = getenv("PATH")) == NULL) {
                missing_helpers = -1;
                goto cleanup;
            }

            if ((path = strdup(tok)) == NULL) {
                missing_helpers = -1;
                goto cleanup;
            }
            // append some Eucalyptus-specific locations to $PATH
            if ((euca = getenv(EUCALYPTUS_ENV_VAR_NAME)) == NULL) {
                euca = "";
            }

            for (j = 0; (locations[j] != NULL); j++) {
                snprintf(lpath, sizeof(lpath), locations[j], euca);
                if ((newpath = euca_strdupcat(path, lpath)) == NULL) {
                    missing_helpers = -1;
                    goto cleanup;
                }
                path = newpath;
            }

            done = 0;
            tok = strtok_r(path, ":", &save);
            while (tok && !done) {
                helper = strdup(helpers[i]);
                toka = strtok_r(helper, ",", &savea);
                while (toka && !done) {
                    snprintf(file, EUCA_MAX_PATH, "%s/%s", tok, toka);
                    if ((rc = stat(file, &statbuf)) == 0) {
                        if (S_ISREG(statbuf.st_mode)) {
                            tmp_helpers_path[i] = strdup(file);
                            done++;
                        }
                    }
                    toka = strtok_r(NULL, ":", &savea);
                }
                tok = strtok_r(NULL, ":", &save);
                EUCA_FREE(helper);
            }
            EUCA_FREE(path);
        }

        if (!done) {
            missing_helpers++;
            LOGTRACE("did not find '%s' in path\n", helpers[i]);
        } else {
            LOGTRACE("found '%s' at '%s'\n", helpers[i], tmp_helpers_path[i]);
        }
    }

cleanup:
    if (helpers_path == NULL) {
        for (i = 0; i < num_helpers; i++)
            EUCA_FREE(tmp_helpers_path[i]);
        EUCA_FREE(tmp_helpers_path);
    }

    return (missing_helpers);
}

//!
//!
//!
//! @param[in]  fd
//! @param[out] buf
//! @param[in]  bytes
//! @param[in]  timeout
//!
//! @return -1 on failure or the number of bytes read.
//!
int timeread(int fd, void *buf, size_t bytes, int timeout)
{
    int rc = 0;
    fd_set rfds = { {0} };
    struct timeval tv = { 0 };

    if (timeout <= 0)
        timeout = 1;

    FD_ZERO(&rfds);
    FD_SET(fd, &rfds);

    tv.tv_sec = timeout;
    tv.tv_usec = 0;

    if ((rc = select(fd + 1, &rfds, NULL, NULL, &tv)) <= 0) {
        // timeout
        LOGERROR("select() timed out for read: timeout=%d\n", timeout);
        return (-1);
    }

    rc = read(fd, buf, bytes);
    return (rc);
}

//!
//!
//!
//! @param[in] euca_home_supplied
//!
//! @return the result of the setenv() system call
//!
int add_euca_to_path(const char *euca_home_supplied)
{
    char *old_path = NULL;
    char *euca_home = NULL;
    char new_path[4098] = "";

    if (euca_home_supplied && strlen(euca_home_supplied)) {
        if (euca_sanitize_path(euca_home_supplied) != EUCA_OK) {
            euca_home = strdup("");
        } else {
            euca_home = strdup(euca_home_supplied);
        }
    } else if (getenv(EUCALYPTUS_ENV_VAR_NAME) && strlen(getenv(EUCALYPTUS_ENV_VAR_NAME))) {
        if (euca_sanitize_path(getenv(EUCALYPTUS_ENV_VAR_NAME)) != EUCA_OK) {
            euca_home = strdup("");
        } else {
            euca_home = strdup(getenv(EUCALYPTUS_ENV_VAR_NAME));
        }
    } else {
        // we'll assume root
        euca_home = strdup("");
    }

    if (euca_sanitize_path(getenv("PATH")) != EUCA_OK) {
        old_path = strdup("");
    } else {
        old_path = strdup(getenv("PATH"));
    }

    snprintf(new_path, sizeof(new_path), EUCALYPTUS_DATA_DIR ":"    // (connect|disconnect iscsi, get_xen_info, getstats, get_sys_info)
             EUCALYPTUS_SBIN_DIR ":"   // (eucalyptus-cloud, euca_conf, euca-* admin commands)
             EUCALYPTUS_LIBEXEC_DIR ":" // (rootwrap, mountwrap)
             "%s", euca_home, euca_home, euca_home, old_path);

    EUCA_FREE(euca_home);
    EUCA_FREE(old_path);
    return (setenv("PATH", new_path, TRUE));
}

//!
//! Wrapper around waitpid() that retries waiting until a timeout, specified in seconds, occurs. Return value
//! is that of waitpid():
//!
//!  -1 means there was an error
//!   0 means there was a timeout
//!   N is the PID of the process
//!
//! When a positive value is returned, status is set
//! to the exit status of the process
//!
//! @param[in] pid
//! @param[in] status
//! @param[in] timeout_sec
//!
//! @return -1 means there was an error; 0 means there was a timeout; N is the PID of the process
//!
pid_t timewait(pid_t pid, int *status, int timeout_sec)
{
    int rc = 0;
    time_t elapsed_usec = 0;

    // do not allow negative timeouts
    if (timeout_sec < 0)
        timeout_sec = 0;

    //! @todo remove this once we know that no callers rely on status to detect timeout
    *status = 1;

    rc = waitpid(pid, status, WNOHANG);
    while ((rc == 0) && (elapsed_usec < (timeout_sec * 1000000))) {
        usleep(10000);
        elapsed_usec += 10000;
        rc = waitpid(pid, status, WNOHANG);
    }

    if (rc == 0) {
        LOGERROR("waitpid() timed out: pid=%d\n", pid);
    }

    return (rc);
}

//!
//! Attempt to kill a process. This could wait up to 3 seconds to confirm/infirm the
//! success of the kill operations.
//!
//! @param pid the process identifier for the process we're terminating
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int killwait(pid_t pid)
{
    int status = 0;

    kill(pid, SIGTERM);                // should be able to do
    if (timewait(pid, &status, 1) == 0) {
        LOGERROR("child process {%u} failed to terminate. Attempting SIGKILL.\n", pid);
        kill(pid, SIGKILL);
        if (timewait(pid, &status, 1) == 0) {
            LOGERROR("child process {%u} failed to KILL. Attempting SIGKILL again.\n", pid);
            kill(pid, SIGKILL);
            if (timewait(pid, &status, 1) == 0) {
                return (EUCA_ERROR);
            }
        }
    }
    return (EUCA_OK);
}

//!
//! Validates a list of parameters for a given vnet function.
//!
//! @param[in] func the function name that parameters will be validated against
//! @param[in] ... the list of parameters
//!
//! @return 0 if the parameter list is valid or 1 if not
//!
int param_check(const char *func, ...)
{
    int i1 = 0;
    char *str1 = NULL;
    char *str2 = NULL;
    char *str3 = NULL;
    char *str4 = NULL;
    char *str5 = NULL;
    char *str6 = NULL;
    va_list al = { {0} };
    boolean fail = FALSE;
    vnetConfig *vnet1 = NULL;

    if (!func) {
        return (1);
    }

    va_start(al, func);
    {
        if (!strcmp(func, "vnetGenerateDHCP") || !strcmp(func, "vnetKickDHCP")) {
            if ((vnet1 = va_arg(al, vnetConfig *)) == NULL) {
                fail = TRUE;
            }
        } else if (!strcmp(func, "vnetAddPublicIP") || !strcmp(func, "vnetAddDev")) {
            vnet1 = va_arg(al, vnetConfig *);
            str1 = va_arg(al, char *);
            if (!vnet1 || !str1) {
                fail = TRUE;
            }
        } else if (!strcmp(func, "vnetAddHost")) {
            vnet1 = va_arg(al, vnetConfig *);
            str1 = va_arg(al, char *);
            str2 = va_arg(al, char *);
            i1 = va_arg(al, int);
            if (!vnet1 || !str1 || (i1 < 0) || (i1 > (NUMBER_OF_VLANS - 1))) {
                fail = TRUE;
            }
        } else if (!strcmp(func, "vnetGetNextHost")) {
            vnet1 = va_arg(al, vnetConfig *);
            str1 = va_arg(al, char *);
            str2 = va_arg(al, char *);
            i1 = va_arg(al, int);
            if (!vnet1 || !str1 || !str2 || (i1 < 0) || (i1 > (NUMBER_OF_VLANS - 1))) {
                fail = TRUE;
            }
        } else if (!strcmp(func, "vnetDelHost") || !strcmp(func, "vnetEnableHost") || !strcmp(func, "vnetDisableHost")) {
            vnet1 = va_arg(al, vnetConfig *);
            str1 = va_arg(al, char *);
            str2 = va_arg(al, char *);
            i1 = va_arg(al, int);
            if (!vnet1 || (!str1 && !str2) || (i1 < 0) || (i1 > (NUMBER_OF_VLANS - 1))) {
                fail = TRUE;
            }
        } else if (!strcmp(func, "vnetDeleteChain") || !strcmp(func, "vnetCreateChain")) {
            vnet1 = va_arg(al, vnetConfig *);
            str1 = va_arg(al, char *);
            str2 = va_arg(al, char *);
            if (!vnet1 || !str1 || !str2) {
                fail = TRUE;
            }
        } else if (!strcmp(func, "vnetTableRule")) {
            vnet1 = va_arg(al, vnetConfig *);
            str1 = va_arg(al, char *);
            str2 = va_arg(al, char *);
            str3 = va_arg(al, char *);
            str4 = va_arg(al, char *);
            str5 = va_arg(al, char *);
            str6 = va_arg(al, char *);
            if (!vnet1 || !str1 || !str2 || !str3 || (!str4 && !str5 && !str6)) {
                fail = TRUE;
            }
        } else if (!strcmp(func, "vnetSetVlan")) {
            vnet1 = va_arg(al, vnetConfig *);
            i1 = va_arg(al, int);
            str1 = va_arg(al, char *);
            str2 = va_arg(al, char *);
            if (!vnet1 || (i1 < 0) || (i1 >= NUMBER_OF_VLANS) || !str1 || !str2) {
                fail = TRUE;
            }
        } else if (!strcmp(func, "vnetDelVlan")) {
            vnet1 = va_arg(al, vnetConfig *);
            i1 = va_arg(al, int);
            if (!vnet1 || (i1 < 0) || (i1 >= NUMBER_OF_VLANS)) {
                fail = TRUE;
            }
        } else if (!strcmp(func, "vnetInit")) {
            vnet1 = va_arg(al, vnetConfig *);
            str1 = va_arg(al, char *);
            str2 = va_arg(al, char *);
            str3 = va_arg(al, char *);
            i1 = va_arg(al, int);
            if (!vnet1 || !str1 || !str2 || (i1 < 0)) {
                fail = TRUE;
            }
        }
    }
    va_end(al);

    if (fail) {
        LOGERROR("incorrect input parameters to function %s\n", func);
        return (1);
    }
    return (0);
}

//!
//!
//!
//! @param[in] pid
//! @param[in] search
//!
//! @return 0 on success or 1 if not valid
//!
int check_process(pid_t pid, char *search)
{
    int rc = 0;
    int ret = 1;
    FILE *FH = NULL;
    char *p = NULL;
    char file[EUCA_MAX_PATH] = "";
    char buf[1024] = "";

    snprintf(file, EUCA_MAX_PATH, "/proc/%d/cmdline", pid);
    if ((rc = check_file(file)) == 0) {
        // cmdline exists
        if (search) {
            // check if cmdline contains 'search'
            if ((FH = fopen(file, "r")) != NULL) {
                bzero(buf, 1024);
                while (fgets(buf, 1024, FH)) {
                    while ((p = memchr(buf, '\0', 1024))) {
                        *p = 'X';
                    }

                    // safety
                    buf[1023] = '\0';
                    if (strstr(buf, search)) {
                        ret = 0;
                    }
                }
                fclose(FH);
            }
        } else {
            ret = 0;
        }
    }
    return (ret);
}

//!
//! execute system(shell_command) and return stdout in new string pointed to by *stringp
//!
//! @param[in] shell_command
//!
//! @return a pointer to the stdout output
//!
//! @pre The shell_command field must not be NULL.
//!
//! @note the caller is responsible to free the returned memory
//!
char *system_output(char *shell_command)
{
    char *buf = NULL;
    FILE *fp = NULL;

    if (!shell_command)
        return (NULL);

    // forks off command (this doesn't fail if command doesn't exist
    LOGTRACE("[%s]\n", shell_command);
    if ((fp = popen(shell_command, "r")) == NULL) {
        // caller can check errno
        return (NULL);
    }

    buf = fp2str(fp);
    pclose(fp);
    return (buf);
}

//!
//!
//!
//! @param[in] configFiles
//! @param[in] numFiles
//! @param[in] key
//!
//! @return a pointer to the configuration string
//!
//! @note caller is responsible to free memory allocated
//!
char *getConfString(char configFiles[][EUCA_MAX_PATH], int numFiles, char *key)
{
    int rc = 0;
    int i = 0;
    int done = 0;
    char *tmpstr = NULL;
    char *tmpptr = NULL;

    done = 0;
    for (i = 0; ((i < numFiles) && !done); i++) {
        if ((rc = get_conf_var(configFiles[i], key, &tmpstr)) == 1) {
            done++;
        }
    }

    if (tmpstr && strlen(tmpstr)) {
        tmpptr = tmpstr + (strlen(tmpstr) - 1);
        while (*tmpptr == ' ') {
            *tmpptr = '\0';
            tmpptr = tmpstr + (strlen(tmpstr) - 1);
        }
    }

    return (tmpstr);
}

//!
//! search for variable 'name' in file 'path' and return whatever is after
//! = in value (which will need to be freed).
//!
//! Example of what we are able to parse:
//! TEST="test"
//! TEST=test
//!    TEST   = test
//!
//! @param[in]     path
//! @param[in]     name
//! @param[in,out] value
//!
//! @pre \li The path, name and value fields must not be NULL.
//!      \li The path field must be a valid path name.
//!      \li The name field must not be 0 length.
//!      \li The value field should be set to NULL or potential memory leak will occur.
//!
//! @post On success, the value field is updated.
//!
//! @return 1 on success, 0 on variable not found and -1 on error (parse or file not found)
//!
//! @note caller is responsible to free the memory allocated for 'value'
//!
int get_conf_var(const char *path, const char *name, char **value)
{
    FILE *f = NULL;
    char *buf = NULL;
    char *ptr = NULL;
    char *ret = NULL;
    int len = 0;

    // sanity check
    if ((path == NULL) || (path[0] == '\0') || (name == NULL) || (name[0] == '\0') || (value == NULL)) {
        return (-1);
    }

    *value = NULL;

    if ((f = fopen(path, "r")) == NULL) {
        return (-1);
    }

    len = strlen(name);
    buf = EUCA_ALLOC(32768, sizeof(char));
    while (fgets(buf, 32768, f)) {
        // the process here is fairly simple: spaces are not considered (unless between "")
        // so we remove them before every step. We look for the variable *name* first, then
        // for an = then for the value
        for (ptr = buf; ((*ptr != '\0') && isspace((int)*ptr)); ptr++) ;

        if (strncmp(ptr, name, len) != 0) {
            continue;
        }

        for (ptr += len; (*ptr != '\0') && isspace((int)*ptr); ptr++) ;

        if (*ptr != '=') {
            continue;
        }
        // we are in business
        for (ptr++; (*ptr != '\0') && isspace((int)*ptr); ptr++) ;

        if (*ptr == '"') {
            // we have a quote, we need the companion
            ret = ++ptr;
            while ((*ptr != '"')) {
                if (*ptr == '\0') {
                    // something wrong happened
                    fclose(f);
                    EUCA_FREE(buf);
                    return (-1);
                }
                ptr++;
            }
        } else {
            // well we get the single word right after the =
            ret = ptr;
            while (!isspace((int)*ptr) && (*ptr != '#') && (*ptr != '\0')) {
                ptr++;
            }
        }

        *ptr = '\0';
        if ((*value = strdup(ret)) == NULL) {
            fclose(f);
            EUCA_FREE(buf);
            return (-1);
        }

        fclose(f);
        EUCA_FREE(buf);
        return (1);
    }

    fclose(f);
    EUCA_FREE(buf);
    return (0);
}

//!
//!
//!
//! @param[in] value
//!
void free_char_list(char **value)
{
    int i = 0;

    if ((value != NULL) && (*value != NULL)) {
        for (i = 0; value[i] != NULL; i++) {
            EUCA_FREE(value[i]);
        }
        EUCA_FREE(value);
    }
}

//!
//!
//!
//! @param[in] v
//!
//! @pre The v field must not be NULL and must not be of 0 length.
//!
//! @return a pointer to a char list or NULL if any error occured
//!
char **from_var_to_char_list(const char *v)
{
    int i = 0;
    char a = '\0';
    char *w = NULL;
    char *ptr = NULL;
    char *value = NULL;
    char **tmp = NULL;

    // sanity check
    if ((v == NULL) || (v[0] == '\0')) {
        return (NULL);
    }

    if ((tmp = EUCA_ZALLOC(1, sizeof(char *))) == NULL) {
        return (NULL);
    }

    if ((value = strdup(v)) == NULL) {
        EUCA_FREE(tmp);
        return (NULL);
    }

    tmp[0] = NULL;
    i = 0;
    ptr = value;
    for (i = 0, ptr = value; *ptr != '\0'; ptr++) {
        // let's look for the beginning of the word
        for (; *ptr != '\0' && isspace((int)*ptr); ptr++) ;

        if (*ptr == '\0') {
            // end of string with no starting word: we are done here
            break;
        }
        // beginning of word
        w = ptr;
        for (ptr++; *ptr != '\0' && !isspace((int)*ptr); ptr++) ;

        // found the end of word
        a = *ptr;
        *ptr = '\0';

        if ((tmp = EUCA_REALLOC(tmp, (i + 2), sizeof(char *))) == NULL) {
            EUCA_FREE(value);
            return (NULL);
        }

        if ((tmp[i] = strdup(w)) == NULL) {
            free_char_list(tmp);
            EUCA_FREE(value);
            return (NULL);
        }
        tmp[++i] = NULL;

        // now we need to check if we were at the end of the string
        if (a == '\0') {
            break;
        }
    }

    EUCA_FREE(value);
    return (tmp);
}

//!
//! implements Java's String.hashCode() on a string
//!
//! @param[in] s
//!
//! @pre The s field must not be NULL.
//!
//! @return the computed hash code. If 's' is NULL, 0 will be returned
//!
int hash_code(const char *s)
{
    int i = 0;
    int len = 0;
    int code = 0;

    if (s) {
        len = strlen(s);
        for (i = 0; i < len; i++) {
            code = 31 * code + ((unsigned char)s[i]);
        }
    }
    return (0);
}

//!
//! implements Java's String.hashCode() on a buffer of some size
//!
//! @param[in] buf
//! @param[in] buf_size
//!
//! @return the result of hash_code() or -1 on failure.
//!
//! @pre \li The buf field must not be NULL.
//!      \li The buf_size field must not be 0.
//!
//! @see hash_code()
//!
int hash_code_bin(const char *buf, int buf_size)
{
    int i = 0;
    int code = 0;
    char *buf_str = NULL;

    if ((buf == NULL) || (buf_size == 0))
        return (-1);

    if ((buf_str = EUCA_ALLOC(((2 * buf_size) + 1), sizeof(char))) == NULL)
        return (-1);

    for (i = 0; i < buf_size; i++) {
        snprintf(buf_str + (i * 2), 2, "%0x", *(buf + i));
    }
    buf_str[2 * buf_size] = '\0';

    code = hash_code(buf_str);
    EUCA_FREE(buf_str);
    return (code);
}

//!
//! given a string, returns 3 relevant statistics as a static string
//!
//! @param[in] s
//!
//! @return a pointer to the static string containing teh stats
//!
char *get_string_stats(const char *s)
{
    size_t len = 0;
    static char out[OUTSIZE] = "";     //! @todo malloc this?

    out[0] = '\0';
    if ((s != NULL) && (s[0] != '\0')) {
        len = strlen(s);
        snprintf(out, OUTSIZE, "length=%ld buf[n-1]=%i hash=%d", len, (int)((signed char)s[len - 1]), hash_code(s));
    }
    return (out);
}

//!
//! daemonize and store pid in pidfile. if pidfile exists and contained
//! pid is daemon already running, do nothing.  force option will first
//! kill and then re-daemonize
//!
//! @param[in] cmd
//! @param[in] procname
//! @param[in] pidfile
//! @param[in] force
//! @param[in] rootwrap
//!
//! @return EUCA_OK on success. If an error occured, the following error
//!         codes are returned or the result of the daemonrun():
//!         \li EUCA_INVALID_ERROR if any parameter does not meet our pre-conditions.
//!
//! @see daemonrun()
//!
//! @pre The cmd and procname pointers must not be NULL
//!
int daemonmaintain(char *cmd, char *procname, char *pidfile, int force, char *rootwrap)
{
    int rc = 0;
    char cmdstr[EUCA_MAX_PATH] = "";
    char file[EUCA_MAX_PATH] = "";
    char *pidstr = NULL;
    FILE *FH = NULL;
    boolean found = FALSE;

    if (!cmd || !procname) {
        return (EUCA_INVALID_ERROR);
    }

    if (pidfile) {
        found = FALSE;
        if ((rc = check_file(pidfile)) == 0) {
            // pidfile exists
            if ((pidstr = file2str(pidfile)) != NULL) {
                snprintf(file, EUCA_MAX_PATH, "/proc/%s/cmdline", pidstr);
                if (!check_file(file)) {
                    if ((FH = fopen(file, "r")) != NULL) {
                        if (fgets(cmdstr, EUCA_MAX_PATH, FH)) {
                            if (strstr(cmdstr, procname)) {
                                // process is running, and is indeed procname
                                found = TRUE;
                            }
                        }
                        fclose(FH);
                    }
                }

                EUCA_FREE(pidstr);
            }
        }

        if (found) {
            // pidfile passed in and process is already running
            if (force) {
                // kill process and remove pidfile
                rc = safekillfile(pidfile, procname, 9, rootwrap);
            } else {
                // nothing to do
                return (EUCA_OK);
            }
        } else {
            // pidfile passed in but process is not running
            if (!check_file(pidfile)) {
                unlink(pidfile);
            }
        }
    }

    return (daemonrun(cmd, pidfile));
}

//!
//!
//!
//! @param[in,out] list pointer to the list of strings to free
//! @param[in]     nmemb number of members in the list
//!
//! @pre The list must not be NULL.
//!
void free_string_list(char ***list, int nmemb)
{
    int i = 0;
    char **tmpList = NULL;

    if (list != NULL) {
        tmpList = (*list);
        for (i = 0; i < nmemb; i++)
            EUCA_FREE(tmpList[i]);
        EUCA_FREE(tmpList);
        (*list) = NULL;
    }
}

//!
//! daemonize and run 'incmd', returning pid of the daemonized process
//!
//! @param[in] incmd
//! @param[in] pidfile
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR
//!         and EUCA_THREAD_ERROR
//!
int daemonrun(char *incmd, char *pidfile)
{
    int i = 0;
    int rc = 0;
    int pid = 0;
    int sid = 0;
    int idx = 0;
    int status = 0;
    char *tok = NULL;
    char *ptr = NULL;
    char *cmd = NULL;
    char **argv = NULL;
    char pidstr[32] = "";
    struct sigaction newsigact = { {NULL} };

    if (!incmd) {
        return (EUCA_INVALID_ERROR);
    }

    if ((pid = fork()) < 0) {
        return (EUCA_THREAD_ERROR);
    }

    if (pid == 0) {
        newsigact.sa_handler = SIG_DFL;
        newsigact.sa_flags = 0;
        sigemptyset(&newsigact.sa_mask);
        sigprocmask(SIG_SETMASK, &newsigact.sa_mask, NULL);
        sigaction(SIGTERM, &newsigact, NULL);

        rc = daemon(0, 0);

        // become parent of session
        sid = setsid();

        if ((cmd = strdup(incmd)) == NULL)
            exit(-1);

        // construct argv
        if ((argv = EUCA_ZALLOC(1, sizeof(char *))) == NULL) {
            EUCA_FREE(cmd)
                exit(-1);
        }

        tok = strtok_r(cmd, " ", &ptr);
        while (tok) {
            fflush(stdout);
            argv[idx] = strdup(tok);

            idx++;
            tok = strtok_r(NULL, " ", &ptr);
            argv = EUCA_REALLOC(argv, (idx + 1), sizeof(char *));
        }

        argv[idx] = NULL;
        EUCA_FREE(cmd);

        // close all fds
        for (i = 0; i < sysconf(_SC_OPEN_MAX); i++) {
            close(i);
        }

        if (pidfile) {
            snprintf(pidstr, 32, "%d", getpid());
            rc = write2file(pidfile, pidstr);
        }
        // run
        exit(execvp(*argv, argv));
    } else {
        rc = waitpid(pid, &status, 0);
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] ina
//! @param[in] inb
//!
//! @return 0 if ina and inb are the same, -1 if ina is less than inb and 1 if inb is less than ina
//!
int uint32compar(const void *ina, const void *inb)
{
    u32 a = 0;
    u32 b = 0;

    a = (*(u32 *) ina);
    b = (*(u32 *) inb);

    if (a < b) {
        return (-1);
    } else if (a > b) {
        return (1);
    }

    return (0);
}

//!
//!
//!
//! @param[in] pidfile
//! @param[in] procname
//! @param[in] sig
//! @param[in] rootwrap
//!
//! @return
//!
//! @see safekill()
//!
int safekillfile(char *pidfile, char *procname, int sig, char *rootwrap)
{
    int rc = 0;
    char *pidstr = NULL;

    if (!pidfile || !procname || (sig < 0) || check_file(pidfile)) {
        return (1);
    }

    rc = 1;
    if ((pidstr = file2str(pidfile)) != NULL) {
        LOGDEBUG("calling safekill with pid %d\n", atoi(pidstr));
        rc = safekill(atoi(pidstr), procname, sig, rootwrap);
        EUCA_FREE(pidstr);
    }
    unlink(pidfile);

    return (rc);
}

//!
//!
//!
//! @param[in] pid
//! @param[in] procname
//! @param[in] sig
//! @param[in] rootwrap
//!
//! @return EUCA_OK on success or any of the following error code:
//!         \li EUCA_ERROR: if we fail to send kill to the process
//!         \li EUCA_INVALID_ERROR: if the process name is NULL or the pid parameter is less than 2
//!         \li EUCA_PERMISSION_ERROR: if the file is not readable
//!         \li EUCA_ACCESS_ERROR: if we fail to open or read the /proc/{pid}/cmdline file.
//!
int safekill(pid_t pid, char *procname, int sig, char *rootwrap)
{
    int ret = 0;
    FILE *FH = NULL;
    char sPid[16] = "";
    char sSignal[16] = "";
    char cmdstr[EUCA_MAX_PATH] = "";
    char file[EUCA_MAX_PATH] = "";

    if ((pid < 2) || !procname) {
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

    ret = 1;

    // found running process
    if (strstr(cmdstr, procname)) {
        // passed in cmd matches running cmd
        if (rootwrap) {
            snprintf(sPid, 16, "%d", pid);
            snprintf(sSignal, 16, "-%d", sig);
            ret = euca_execlp(NULL, rootwrap, "kill", sSignal, sPid, NULL);
        } else {
            ret = ((kill(pid, sig) == -1) ? EUCA_ERROR : EUCA_OK);
        }
    }

    return (ret);
}

//!
//!
//!
//! @param[in] a
//! @param[in] b
//!
//! @return the maximum of the two given values
//!
int maxint(int a, int b)
{
    return ((a > b) ? a : b);
}

//!
//!
//!
//! @param[in] a
//! @param[in] b
//!
//! @return the minimum of the two given values
//!
int minint(int a, int b)
{
    return ((a < b) ? a : b);
}

//!
//! given a null-terminated string in 'xml', finds the next complete <tag...>
//! and returns its name in a newly allocated string (which must be freed by
//! the caller) or returns NULL if no tag can be found or if the XML is not
//! well formed; when not NULL, the following parameters are also returned:
//!
//!     start   = index of the '<' character
//!     end     = index of the '>' character
//!     single  = 1 if this is a <..../> tag
//!     closing = 1 if this is a </....> tag
//!
//! @param[in]  xml
//! @param[out] start
//! @param[out] end
//! @param[out] single
//! @param[out] closing
//!
//! @return the name of the next complete xml tag
//!
//! @note the caller is responsible to free the returned memory
//!
static char *next_tag(const char *xml, int *start, int *end, int *single, int *closing)
{
    int name_start = -1;
    int name_end = -1;
    int tag_start = -1;
    char *ret = NULL;
    const char *p = NULL;
    const char *last_ch = NULL;

    for (p = xml; *p; p++) {
        if (*p == '<') {               // found a new tag
            tag_start = (p - xml);     // record the char so its offset can be returned

            *closing = 0;
            if ((*(p + 1) == '/') || (*(p + 1) == '?')) {
                if (*(p + 1) == '/')   // if followed by '/' then it is a "closing" tag
                    *closing = 1;
                name_start = (p - xml + 2);
                p++;
            } else {
                name_start = (p - xml + 1);
            }
            continue;
        }

        if ((*p == ' ') && (name_start != -1) && (name_end == -1)) {    // a name may be terminated by a space
            name_end = (p - 1 - xml);
            continue;
        }

        if (*p == '>') {
            if (name_start == -1)      // never saw '<', error
                break;

            if (p < (xml + 2))         // tag is too short, error
                break;

            last_ch = p - 1;
            if ((*last_ch == '/') || (*last_ch == '?')) {
                *single = 1;           // preceded by '/' then it is a "single" tag
                last_ch--;
            } else {
                *single = 0;
            }

            if ((name_start != -1) && (name_end == -1)) {   // a name may be terminated by '/' or '>' or '?'
                name_end = (last_ch - xml);
            }

            if ((name_end - name_start) >= 0) { // we have a name rather than '<>'
                if ((ret = EUCA_ZALLOC(name_end - name_start + 2, sizeof(char))) == NULL)
                    break;
                strncpy(ret, xml + name_start, name_end - name_start + 1);
                *start = tag_start;
                *end = p - xml;
            }
            break;
        }
    }

    return (ret);
}

//!
//! given a null-terminated string in 'xml' and an 'xpath' of an XML
//! element, returns the "content" of that element in a newly allocated
//! string; for example, with XML:
//!
//!   "<a><b>foo</b><c><d>bar</d><e>baz</e></c></a>"
//!
//! the content returned for xpath "a/c/e" is "baz"
//!
//! @param[in] xml
//! @param[in] xpath
//!
//! @return a pointer to the content we're looking for
//!
//! @note the caller is responsible to free the returned memory
//!
static char *find_cont(const char *xml, char *xpath)
{
#define _STK_SIZE            64

    int i = 0;
    int xpathLen = 0;
    int xml_offset = 0;
    int tag_start = 1;
    int tag_end = -1;
    int single = 0;
    int closing = 0;
    int stk_p = -1;
    int cont_len = 0;
    char *ret = NULL;
    char *cont = NULL;
    char *name = NULL;
    char *name_lc = NULL;
    char *n_stk[_STK_SIZE] = { NULL };
    char xpath_cur[EUCA_MAX_PATH] = "";
    const char *contp = NULL;
    const char *c_stk[_STK_SIZE] = { NULL };

    // iterate over tags until the matching xpath is reached or until no more tags are found in the 'xml'
    bzero(n_stk, sizeof(char *) * _STK_SIZE);

    for (xml_offset = 0; (name = next_tag(xml + xml_offset, &tag_start, &tag_end, &single, &closing)) != NULL; xml_offset += tag_end + 1) {
        if (single) {
            // not interested in singles because we are looking for content
        } else if (!closing) {         // opening a tag
            // put name and pointer to content onto the stack
            stk_p++;
            if (stk_p == _STK_SIZE)    // exceeding stack size, error
                goto cleanup;
            n_stk[stk_p] = euca_strduptolower(name);    // put a lower-case-only copy onto stack
            c_stk[stk_p] = xml + xml_offset + tag_end + 1;
        } else {                       // closing tag
            // get the name in all lower-case, for consistency with xpath
            name_lc = euca_strduptolower(name);
            EUCA_FREE(name);
            name = name_lc;

            // name doesn't match last seen opening tag, error
            if (stk_p >= 0) {
                if (!n_stk[stk_p] || (strcmp(n_stk[stk_p], name) != 0)) {
                    goto cleanup;
                }
            }
            // construct the xpath of the closing tag based on stack contents
            xpath_cur[0] = '\0';
            for (i = 0, xpathLen = EUCA_MAX_PATH - 1; i <= stk_p; i++) {
                if (i > 0) {
                    strncat(xpath_cur, "/", (xpathLen - strlen(xpath_cur) - 1));
                }
                strncat(xpath_cur, n_stk[i], (xpathLen - strlen(xpath_cur) - 1));
            }

            // pop the stack whether we have a match or not
            if (stk_p < 0)             // past the bottom of the stack, error
                goto cleanup;

            contp = c_stk[stk_p];
            cont_len = xml + xml_offset + tag_start - contp;
            EUCA_FREE(n_stk[stk_p]);
            stk_p--;

            // see if current xpath matches the requested one
            if (strcmp(xpath, xpath_cur) == 0) {
                if ((cont = EUCA_ZALLOC(cont_len + 1, sizeof(char))) == NULL)
                    goto cleanup;
                strncpy(cont, contp, cont_len);
                ret = cont;
                break;
            }
        }
        EUCA_FREE(name);
        name = NULL;
    }

cleanup:
    EUCA_FREE(name);                   // for exceptions
    for (i = 0; i <= stk_p; i++)
        EUCA_FREE(n_stk[i]);           // free everything on the stack
    return (ret);

#undef _STK_SIZE
}

//!
//! given a null-terminated string in 'xml' and an 'xpath' of an XML
//! element, returns the "content" of that element in a newly allocated
//! string; for example, with XML:
//!
//!  "<a><b>foo</b><c><d>bar</d><e>baz</e></c></a>"
//!
//! the content returned for xpath "a/c/e" is "baz"
//!
//! @param[in] xml
//! @param[in] xpath
//!
//! @return a pointer to the content we're looking for
//!
//! @see find_cont()
//!
//! @note the caller is responsible to free the returned memory
//!
char *xpath_content(const char *xml, const char *xpath)
{
    char *ret = NULL;
    char *xpath_l = NULL;

    if ((xml == NULL) || (xpath == NULL))
        return (NULL);

    xpath_l = euca_strduptolower(xpath);    // lower-case copy of requested xpath
    if (xpath_l != NULL) {
        ret = find_cont(xml, xpath_l);
        EUCA_FREE(xpath_l);
    }

    return (ret);
}

//!
//!
//!
//! @param[in] uri
//! @param[in] uriType
//! @param[in] host
//! @param[in] port
//! @param[in] path
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int construct_uri(char *uri, char *uriType, char *host, int port, char *path)
{
    char tmp[32] = "";

    if (!uri || !uriType || !host || !strlen(uriType) || !strlen(host)) {
        return (EUCA_ERROR);
    }

    uri[0] = '\0';
    strncat(uri, uriType, strlen(uriType));
    strncat(uri, "://", 3);

    strncat(uri, host, strlen(host));

    if (port > 0) {
        snprintf(tmp, 32, ":%d", port);
        strncat(uri, tmp, strlen(tmp));
    }
    strncat(uri, "/", 1);

    if (path && strlen(path)) {
        strncat(uri, path, strlen(path));
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] uri
//! @param[in] uriType
//! @param[in] host
//! @param[in] port
//! @param[in] path
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int tokenize_uri(char *uri, char *uriType, char *host, int *port, char *path)
{
    char *tok = NULL;
    char *start = NULL;

    uriType[0] = host[0] = path[0] = '\0';
    *port = 0;

    start = uri;

    // must have a type
    tok = strsep(&start, "://");
    if (!start) {
        return (EUCA_ERROR);
    }

    snprintf(uriType, strlen(tok) + 1, "%s", tok);
    start += 2;

    tok = strsep(&start, ":");
    if (!start) {
        // no port
        start = tok;
        tok = strsep(&start, "/");
        if (!start) {
            // no path
            if (tok) {
                snprintf(host, strlen(tok) + 1, "%s", tok);
            } else {
                // no host
                // must have a host
                return (EUCA_ERROR);
            }
        } else {
            // path present
            snprintf(host, strlen(tok) + 1, "%s", tok);
            snprintf(path, strlen(start) + 1, "%s", start);
        }
    } else {
        // port present
        snprintf(host, strlen(tok) + 1, "%s", tok);
        tok = strsep(&start, "/");
        if (!start) {
            // no path present
            if (tok) {
                *port = atoi(tok);
            }
        } else {
            // path present
            *port = atoi(tok);
            snprintf(path, strlen(start) + 1, "%s", start);
        }
    }

    return (EUCA_OK);
}

//!
//! time since 1970 in microseconds
//!
//! @return the time since 1970 in microseconds
//!
long long time_usec(void)
{
    struct timeval tv = { 0 };

    gettimeofday(&tv, NULL);
    return ((long long)tv.tv_sec * 1000000 + tv.tv_usec);
}

//!
//! time since 1970 in milliseconds
//!
//! @return the time since 1970 in milliseconds
//!
long long time_ms(void)
{
    return (time_usec() / 1000);
}

//!
//! try to get UUID of the block device
//!
//! @param[in] dev_path
//! @param[in] uuid
//! @param[in] uuid_size
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @todo use blkidlib maybe
//!
int get_blkid(const char *dev_path, char *uuid, unsigned int uuid_size)
{
    int ret = EUCA_ERROR;
    char cmd[1024] = "";
    char *first_char = NULL;
    char *last_char = NULL;
    char blkid_output[1024];
    char blkid_stderr[1024];

    int status;
    snprintf(cmd, sizeof(cmd), "blkid %s", dev_path);   // option '-u filesystem' did not exist on Centos
    for (int i = 0; i < 3; i++) {      // we will retry these invocations because sometimes blkid hangs
        status = timeshell(cmd, blkid_output, blkid_stderr, sizeof(blkid_output), 5);
        if (status < 0) {
            LOGWARN("invocation '%s' failed with %d (attempt %d)\n", cmd, status, i + 1);
        } else {
            break;
        }
    }
    if (status < 0)
        return (EUCA_ERROR);

    if ((first_char = strstr(blkid_output, "UUID=\"")) != NULL) {
        first_char += 6;
        last_char = strchr(first_char, '"');
        if (last_char && ((last_char - first_char) > 0)) {
            *last_char = '\0';
            euca_strncpy(uuid, first_char, uuid_size);
            assert(0 == strcmp(uuid, first_char));
            ret = EUCA_OK;
        }
    }

    return (ret);
}

//!
//! turn a string into a boolean (returned as a char)
//!
//! @param[in] s the string to parse
//!
//! @return 1 if 's' is "y", "yes", "t" or "true". Otherwise 0 is returned
//!
char parse_boolean(const char *s)
{
    char val = '\0';
    char *lc = euca_strduptolower(s);

    if (!strcmp(lc, "y") || !strcmp(lc, "yes") || !strcmp(lc, "t") || !strcmp(lc, "true")) {
        val = 1;
    } else if (!strcmp(lc, "n") || !strcmp(lc, "no") || !strcmp(lc, "f") || !strcmp(lc, "false")) {
        val = 0;
    } else {
        LOGERROR("failed to parse value '%s' as boolean", lc);
    }

    EUCA_FREE(lc);
    return (val);
}

//!
//! become user 'eucalyptus'
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int drop_privs(void)
{
    int s = 0;
    struct passwd pwd = { 0 };
    struct passwd *result = NULL;
    char buf[16384] = { 0 };           // man-page said this is enough

    s = getpwnam_r(EUCALYPTUS_ADMIN, &pwd, buf, sizeof(buf), &result);
    if (result == NULL)
        return (EUCA_ERROR);           // not found if s==0, check errno otherwise

    if (setgid(pwd.pw_gid) != 0)
        return (EUCA_ERROR);

    if (setuid(pwd.pw_uid) != 0)
        return (EUCA_ERROR);

    return (EUCA_OK);
}

//!
//! run shell command with timeout and get back: return value, stdout and stderr all in one
//!
//! @param[in] command
//! @param[in] stdout_str
//! @param[in] stderr_str
//! @param[in] max_size
//! @param[in] timeout
//!
//! @return -1 for any failures, 0 if a timeout occured or a positive value is success.
//!
int timeshell(char *command, char *stdout_str, char *stderr_str, int max_size, int timeout)
{
    int retval;
    int readn;
    int stdoutfds[2] = { 0, 0 };
    int stderrfds[2] = { 0, 0 };
    int child_pid = 0;
    int maxfd = 0;
    int rc = 0;
    int status = 0;
    int stdout_toread = 0;
    int stderr_toread = 0;
    char errorBuf[256] = { 0 };
    time_t start_time = 0;
    time_t remaining_time = 0;
    fd_set rfds = { {0} };
    struct timeval tv = { 0 };

    // force nonempty on all arguments to simplify the logic
    assert(command);
    assert(stdout_str);
    assert(stderr_str);

    if (pipe(stdoutfds) < 0) {
        strerror_r(errno, errorBuf, 256);
        LOGERROR("error: failed to create pipe for stdout: %s\n", errorBuf);
        return (-1);
    }
    if (pipe(stderrfds) < 0) {
        strerror_r(errno, errorBuf, 256);
        LOGERROR("error: failed to create pipe for stderr: %s\n", errorBuf);
        return (-1);
    }

    if ((child_pid = fork()) == 0) {
        close(stdoutfds[0]);
        if (dup2(stdoutfds[1], STDOUT_FILENO) < 0) {
            strerror_r(errno, errorBuf, 256);
            LOGERROR("error: failed to dup2 stdout: %s\n", errorBuf);
            exit(1);
        }

        close(stdoutfds[1]);
        close(stderrfds[0]);

        if (dup2(stderrfds[1], STDERR_FILENO) < 0) {
            strerror_r(errno, errorBuf, 256);
            LOGERROR("error: failed to dup2 stderr: %s\n", errorBuf);
            exit(1);
        };

        close(stderrfds[1]);

        execl("/bin/sh", "sh", "-c", command, (char *)0);

        exit(127);
    }

    close(stdoutfds[1]);
    close(stderrfds[1]);

    if (child_pid < 0) {
        close(stdoutfds[0]);
        close(stderrfds[0]);
        return (-1);
    }

    memset(stdout_str, 0, max_size);
    memset(stderr_str, 0, max_size);
    stdout_toread = stderr_toread = max_size - 1;

    maxfd = ((stdoutfds[0] > stderrfds[0]) ? stdoutfds[0] : stderrfds[0]);

    start_time = time(NULL);
    for (;;) {
        FD_ZERO(&rfds);
        FD_SET(stdoutfds[0], &rfds);
        FD_SET(stderrfds[0], &rfds);

        tv.tv_sec = 1;
        tv.tv_usec = 0;

        if ((retval = select(maxfd + 1, &rfds, (fd_set *) 0, (fd_set *) 0, &tv)) > 0) {
            if (FD_ISSET(stdoutfds[0], &rfds) && stdout_toread > 0) {
                if ((readn = read(stdoutfds[0], stdout_str, stdout_toread)) > 0) {
                    stdout_toread -= readn;
                    stdout_str += readn;
                } else {
                    break;
                }
            }

            if (FD_ISSET(stderrfds[0], &rfds) && stderr_toread > 0) {
                if ((readn = read(stderrfds[0], stderr_str, stderr_toread)) > 0) {
                    stderr_toread -= readn;
                    stderr_str += readn;
                } else {
                    break;
                }
            }
        } else if (retval < 0) {
            strerror_r(errno, errorBuf, 256);
            LOGWARN("warning: select error on pipe read: %s\n", errorBuf);
            break;
        }

        if ((time(NULL) - start_time) > timeout) {
            LOGWARN("warning: read timeout\n");
            break;
        }
    }

    close(stdoutfds[0]);
    close(stderrfds[0]);

    remaining_time = timeout - (time(NULL) - start_time);
    if ((rc = timewait(child_pid, &status, remaining_time)) != 0) {
        if (WIFEXITED(status))
            rc = WEXITSTATUS(status);
        else
            rc = -1;
    } else {
        killwait(child_pid);
        LOGERROR("warning: shell execution timeout\n");
        return (-1);
    }

    return (rc);
}

//!
//!
//!
//! @param the_iqn
//! @param remoteDev
//! @param remoteDevForNC
//! @param remoteDevForNCLen
//!
//! @return
//!
//! @pre
//!
//! @post
//!
//! @note
int get_remoteDevForNC(const char *the_iqn, const char *remoteDev, char *remoteDevForNC, int remoteDevForNCLen)
{
    assert(remoteDevForNC != NULL);
    assert(remoteDevForNCLen > 0);
    remoteDevForNC[0] = '\0';          // clear out the destination string

    char *remoteDevCopy = strdup(remoteDev);
    if (remoteDevCopy == NULL) {
        LOGERROR("out of memory\n");
        return (1);
    }

    int ret = 1;
    char *toka;
    char *ptra = remoteDevCopy;
    for (int i = 0; (toka = strsep(&ptra, DEV_STR_DELIMITER)); i++) {
        if (i == 2) {                  // IQN strings are in the 3rd field
            if (strstr(toka, DEV_STR_KEY_VAL_DELIMITER) == NULL) {  // old format, just the LUN, don't munge it
                ret = 0;
            } else {
                char *ptrb;
                char *tokb = strtok_r(toka, DEV_STR_IQNS_DELIMITER, &ptrb);
                while (tokb) {
                    char *ptrc;
                    char *an_iqn = strtok_r(tokb, DEV_STR_KEY_VAL_DELIMITER, &ptrc);
                    char *lun = strtok_r(NULL, DEV_STR_KEY_VAL_DELIMITER, &ptrc);
                    if (an_iqn && lun) {
                        if (strcmp(an_iqn, the_iqn) == 0) {
                            toka = lun;
                            ret = 0;
                            break;
                        }
                    }
                    tokb = strtok_r(NULL, DEV_STR_IQNS_DELIMITER, &ptrb);
                }
            }
        }

        strncat(remoteDevForNC, toka, remoteDevForNCLen);
        if (ptra != NULL) {            // there are more fields to come
            strncat(remoteDevForNC, DEV_STR_DELIMITER, remoteDevForNCLen);
        }
    }
    EUCA_FREE(remoteDevCopy);

    return (ret);
}

//!
//! @param[in] string string
//! @param[in] list   list of strings
//! @param[in] count  number of entries in list
//!
//!
//! @return FALSE if string not in list of strings, TRUE if string is in list.
//!

int check_for_string_in_list(char *string, char **list, int count)
{
    if (!string || !count || !list || !(*list)) {
        return (FALSE);
    }

    for (int i = 0; i < count; i++) {
        if (!list[i]) {
            return (FALSE);
        }
        if (!strcmp(string, list[i])) {
            return (TRUE);
        }
    }
    return (FALSE);
}

//!
//! Eucalyptus wrapper function around execlp
//!
//! @param[in] pStatus a pointer to the status field to return (status from waitpid()) if not NULL
//! @param[in] file a constant pointer to the pathname of a file which is to be executed
//! @param[in] ... the list of string arguments to pass to the program
//!
//! @return EUCA_OK on success or the following error codes on failure:
//!         \li EUCA_ERROR if the execution terminated but failed
//!         \li EUCA_INVALID_ERROR if the provided argument does not meet the pre-requirements
//!         \li EUCA_THREAD_ERROR if we fail to execute the program within its own thread
//!         \li EUCA_TIMEOUT_ERROR if the execution of the program timed out
//!
//! @pre The file parameter must not be NULL
//!
//! @post
//!
//! @note
//!
int euca_execlp(int *pStatus, const char *file, ...)
{
    int i = 0;
    int j = 0;
    int args = 0;
    int status = 0;
    int result = 0;
    char *str = NULL;
    char **argv = NULL;
    pid_t pid = 0;
    va_list va = { {0} };

    // Default the returned status to -1
    if (pStatus != NULL)
        (*pStatus) = -1;

    // Make sure our given parameter isn't NULL
    if (file == NULL) {
        LOGDEBUG("failed to execute invalid command\n");
        return (EUCA_INVALID_ERROR);
    }
    // Fork the work
    if ((pid = fork()) == -1) {
        LOGDEBUG("failed to create a child process\n");
        return (EUCA_THREAD_ERROR);
    }
    // child?
    if (pid == 0) {
        // Count the number of arguments provided
        va_start(va, file);
        {
            while ((str = va_arg(va, char *)) != NULL) {
                args++;
            }
        }
        va_end(va);

        // Allocate memory for our list of arguments including the last NULL and the pathname in position 0
        if ((argv = EUCA_ZALLOC((args + 2), sizeof(char *))) != NULL) {
            // The first argument should be the pathname again per exec() family convention
            argv[0] = strdup(file);

            // Set our variable array
            va_start(va, file);
            {
                for (i = 0, j = 1; i < args; i++, j++) {
                    argv[j] = strdup(va_arg(va, char *));
                }
            }
            va_end(va);

            // Run the command. This should never return unless a failure occured
            result = execvp(file, argv);

            // Free the allocated memory
            for (i = 0; i < (args + 1); i++)
                EUCA_FREE(argv[i]);
            EUCA_FREE(argv);
            exit(result);
        }
        // out of memory
        exit(ENOMEM);
    }
    // We got a timeout value, see if we can complete successfully within the given time
    if (waitpid(pid, &status, 0) != -1) {
        // Return the status to our caller if requested
        if (pStatus)
            (*pStatus) = status;

        if (WIFEXITED(status))
            return ((WEXITSTATUS(status) == 0) ? EUCA_OK : EUCA_ERROR);
        else if (WIFSIGNALED(status)) {
            LOGDEBUG("Child ended because of an uncaught signal. sig=%d\n", WTERMSIG(status));
        } else if (WIFSTOPPED(status)) {
            LOGDEBUG("Child stopped because of an uncaught signal. sig=%d\n", WSTOPSIG(status));
            killwait(pid);
        }

        LOGDEBUG("child process did not terminate normally. status=%d\n", status);
        return (EUCA_THREAD_ERROR);
    }
    // Return the status to our caller if requested
    if (pStatus)
        (*pStatus) = status;

    killwait(pid);
    LOGERROR("failed to wait for child process. errno=%s(%d)\n", strerror(errno), errno);
    return (EUCA_TIMEOUT_ERROR);
}

#ifdef _UNIT_TEST
//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char **argv)
{
    int p = 0;
    int ret = 0;
    char cwd[1024] = { 0 };
    char *s = NULL;
    FILE *fp = NULL;
    char **d = NULL;
    char uuid[64] = "";
    char path[EUCA_MAX_PATH] = "";
    char dev_path[32] = "";
    char *devs[] = { "hda", "hdb", "hdc", "hdd", "sda", "sdb", "sdc", "sdd", NULL };
    struct stat estat = { 0 };

    if (getcwd(cwd, sizeof(cwd)) == NULL) {
        printf("Failed to retrieve the current working directory information.\n");
        return (1);
    }
    // a nice big buffer with random chars
    char buf[1048576];
    bzero(buf, sizeof(buf));
    for (int i = 0; i < sizeof(buf) - 1; i++) {
        buf[i] = '!' + rand() % ('~' - '!');
        if (i % 79 == 0)
            buf[i] = '\n';
    }

    // We're testing the euca_execlp() API.
    printf("Testing euca_execlp() in misc.c\n");

    // First test is to copy /var/log/messages to /tmp/eucaexec.txt using cat and redirect.
    if ((ret = euca_execlp(NULL, "/bin/cp", "./misc.c", "/tmp/eucaexec.txt", NULL)) != EUCA_OK) {
        printf("\teuca_execlp(): Failed to copy file to /tmp/eucaexec.txt. ret=%d.\n", ret);
    } else {
        // Make sure the file exist
        if (stat("/tmp/eucaexec.txt", &estat) == 0) {
            // Make sure its no 0 length
            if (estat.st_size > 0) {
                printf("\teuca_execlp(): Successfully copied file to /tmp/eucaexec.txt. File size=%lu bytes.\n", estat.st_size);

                // Now execute an invalide command
                if ((ret = euca_execlp(NULL, "/bin/rim", "/tmp/eucaexec.tmp", NULL)) != EUCA_OK) {
                    // Now delete our file properly
                    if ((ret = euca_execlp(NULL, "/bin/rm", "/tmp/eucaexec.txt", NULL)) == EUCA_OK) {
                        // Make sure its deleted
                        if (stat("/tmp/eucaexec.txt", &estat) == 0) {
                            printf("\teuca_execlp(): Test Failed.\n");
                        } else {
                            printf("\teuca_execlp(): Test passed.\n");
                        }
                    } else {
                        printf("\teuca_execlp(): Test failure. Failed to delete temporary file /tmp/eucaexec.tmp.\n");
                    }
                } else {
                    printf("\teuca_execlp(): Test failure. Invalid command passed succeeded.\n");
                }
            } else {
                printf("\teuca_execlp(): Failed to copy file to /tmp/eucaexec.txt. Empty file.\n");
            }
        } else {
            printf("\teuca_execlp(): Failed to copy file to /tmp/eucaexec.txt. errno=%s(%d)\n", strerror(errno), errno);
        }
    }

    printf("testing str2file in misc.c\n");

    strcpy(path, "/tmp/euca-misc-test-XXXXXX");
    assert(str2file(buf, path, 0, 0644, TRUE) == EUCA_OK);  // normal case
    assert(unlink(path) == 0);

    strcpy(path, "/tmp/euca-misc-test-XXXXXX");
    assert(str2file(NULL, path, 0, 0644, TRUE) == EUCA_OK); // empty tmp file
    assert(unlink(path) == 0);

    strcpy(path, "/tmp/euca-misc-test-XXXXXX");
    assert(str2file("", path, 0, 0644, TRUE) == EUCA_OK);   // empty tmp file
    assert(unlink(path) == 0);

    assert(str2file("xyz", NULL, 0, 0644, TRUE) != EUCA_OK);    // empty path
    assert(str2file("xyz", NULL, O_CREAT | O_TRUNC | O_WRONLY, 0644, FALSE) != EUCA_OK);    // empty path

    strcpy(path, "/tmp/euca-misc-test-XYZ123");
    assert(str2file(buf, path, O_CREAT | O_TRUNC | O_WRONLY, 0644, FALSE) == EUCA_OK);  // normal case non tmp file
    assert(unlink(path) == 0);

    srandom(time(NULL));

    {
        printf("testing get_remoteDevForNC\n");
        char *remoteDev = "a,b,c=1|d=2|e=3,f,g,h";
        char *the_iqn = "d";
        char remoteDevForNC[4096] = "foobar";
        assert(get_remoteDevForNC(the_iqn, remoteDev, remoteDevForNC, sizeof(remoteDevForNC)) == 0);
        assert(strcmp(remoteDevForNC, "a,b,2,f,g,h") == 0);

        remoteDev = "a,b,d=2,f,g,h";
        assert(get_remoteDevForNC(the_iqn, remoteDev, remoteDevForNC, sizeof(remoteDevForNC)) == 0);
        assert(strcmp(remoteDevForNC, "a,b,2,f,g,h") == 0);

        remoteDev = "a,b,2,f,g,h";
        assert(get_remoteDevForNC(the_iqn, remoteDev, remoteDevForNC, sizeof(remoteDevForNC)) == 0);
        assert(strcmp(remoteDevForNC, "a,b,2,f,g,h") == 0);

        char *remoteDevForNCGood = "b483-1000,,1,kyF3TR2zPQ/t01+U6irzECGiVdrVbOPGPjVDJqmYwhWDaWAd5P98YkGzUmhrr/C3K1+M5qO//dXtFOyU90uxL0OuBdumb3zPJ3Tpfx7O0cQ8x+2XufKJl47G8Ca3vk"
            "ravOXqyRV7hmFrvGsSZXk0eqzBN7liYBzkUdpj3zhe0PMwxft+e1WyQSAvNNB/Ea41jkrG8T0X2amYE9gflqmOZlWLUiJLZV6GgJ7rV3Xb3uKtEaLqHISuaGsK1FGT0oZzpNdd4DPTe"
            "o8mo+XfphlMq0NAIZl/+VdUfCRbGhU977koY4nPX3W7xwg+ZP5S3qGF+b9R7mrUD8s4izRkqSEZjg==,,192.168.25.182,iqn.1992-04.com.emc:cx.apm00121200804.a6";
        remoteDev = "b483-1000,,iqn.1994-05.com.redhat:d0d578d4d530=1|iqn.1994-05.com.redhat:e4a4c74e2470=1,kyF3TR2zPQ/t01+U6irzECGiVdrVbOPGPjVDJqmYwhWDaWAd5P98YkGzUmhrr/C3K1+"
            "M5qO//dXtFOyU90uxL0OuBdumb3zPJ3Tpfx7O0cQ8x+2XufKJl47G8Ca3vkravOXqyRV7hmFrvGsSZXk0eqzBN7liYBzkUdpj3zhe0PMwxft+e1WyQSAvNNB/Ea41jkrG8T0X2amYE9gflqmOZlWLUiJLZ"
            "V6GgJ7rV3Xb3uKtEaLqHISuaGsK1FGT0oZzpNdd4DPTeo8mo+XfphlMq0NAIZl/+VdUfCRbGhU977koY4nPX3W7xwg+ZP5S3qGF+b9R7mrUD8s4izRkqSEZjg==,,192.168.25.182,iqn.1992-04.co"
            "m.emc:cx.apm00121200804.a6";
        the_iqn = "iqn.1994-05.com.redhat:d0d578d4d530";
        assert(get_remoteDevForNC(the_iqn, remoteDev, remoteDevForNC, sizeof(remoteDevForNC)) == 0);
        assert(strcmp(remoteDevForNC, remoteDevForNCGood) == 0);
    }

    printf("testing system_output() in misc.c\n");
    s = system_output("echo Hello");
    assert(s);
    assert(strlen(s) != 0);
    printf("echo Hello == |%s|\n", s);
    EUCA_FREE(s);

    printf("testing fp2str in misc.c\n");
    fp = tmpfile();
    assert(fp);
    s = fp2str(fp);
    assert(s);
    assert(strlen(s) == 0);
    EUCA_FREE(s);
    rewind(fp);
    fputs(_STR, fp);
    rewind(fp);
    s = fp2str(fp);
    assert(s);
    assert(strlen(s) == strlen(_STR));
    EUCA_FREE(s);
    fclose(fp);

    printf("testing get_blkid in misc.c\n");
    for (d = devs; *d != NULL; d++) {
        for (p = 1; p < 4; p++) {
            snprintf(dev_path, sizeof(dev_path), "/dev/%s%d", *d, p);
            ret = get_blkid(dev_path, uuid, sizeof(uuid));
            printf("\t%s: %s\n", dev_path, ((ret == 0) ? uuid : "UUID not found"));
        }
    }

    return (0);
}
#endif // _UNIT_TEST
