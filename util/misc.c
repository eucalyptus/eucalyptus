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
//! @file util/misc.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64    // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <string.h>             // strlen, strcpy
#include <ctype.h>              // isspace
#include <assert.h>
#include <stdarg.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/vfs.h>
#include <unistd.h>
#include <time.h>
#include <math.h>               // powf
#include <fcntl.h>              // open
#include <utime.h>              // utime
#include <sys/wait.h>
#include <pwd.h>
#include <dirent.h>             // opendir, etc
#include <sys/errno.h>          // errno
#include <sys/time.h>           // gettimeofday
#include <limits.h>
#include <sys/mman.h>           // mmap
#include <pthread.h>

#include "misc.h"
#include "euca_auth.h"
#include "diskutil.h"
#include "vnetwork.h"
#include "log.h"

#include "eucalyptus.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define OUTSIZE                                    50
#define BUFSIZE                                  1024

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
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int verify_helpers(char **helpers, char **helpers_path, int num_helpers);
int timeread(int fd, void *buf, size_t bytes, int timeout);
int add_euca_to_path(const char *euca_home_supplied);
pid_t timewait(pid_t pid, int *status, int timeout_sec);
int param_check(char *func, ...);
int check_process(pid_t pid, char *search);
int check_directory(const char *dir);
int check_file_newer_than(char *file, time_t mtime);
int check_block(const char *file);
int check_file(const char *file);
int check_path(const char *path);
int statfs_path(const char *path, unsigned long long *fs_bytes_size, unsigned long long *fs_bytes_available, int *fs_id);
char *replace_string(char **stringp, char *source, char *destination);
boolean sscanf_lines(char *lines, char *format, void *varp);
char *fp2str(FILE * fp);
char *system_output(char *shell_command);
char *getConfString(char configFiles[][MAX_PATH], int numFiles, char *key);
int get_conf_var(const char *path, const char *name, char **value);
void free_char_list(char **value);
char **from_var_to_char_list(const char *v);
int hash_code(const char *s);
int hash_code_bin(const char *buf, int buf_size);
char *get_string_stats(const char *s);
int daemonmaintain(char *cmd, char *procname, char *pidfile, int force, char *rootwrap);
int daemonrun(char *incmd, char *pidfile);
int vrun(const char *fmt, ...);
int cat(const char *file_name);
int touch(const char *path);
int diff(const char *path1, const char *path2);
long long dir_size(const char *path);
int write2file(const char *path, char *str);
char *file2strn(const char *path, const ssize_t limit);
char *file2str(const char *path);
char *file2str_seek(char *file, size_t size, int mode);
char *str2str(const char *str, const char *begin, const char *end);
long long str2longlong(const char *str, const char *begin, const char *end);
int uint32compar(const void *ina, const void *inb);
int safekillfile(char *pidfile, char *procname, int sig, char *rootwrap);
int safekill(pid_t pid, char *procname, int sig, char *rootwrap);
int maxint(int a, int b);
int minint(int a, int b);
int copy_file(const char *src, const char *dst);
long long file_size(const char *file_path);
char *strduplc(const char *s);
char *xpath_content(const char *xml, const char *xpath);
int construct_uri(char *uri, char *uriType, char *host, int port, char *path);
int tokenize_uri(char *uri, char *uriType, char *host, int *port, char *path);
char *strdupcat(char *original, char *new);
int ensure_directories_exist(const char *path, int is_file_path, const char *user, const char *group, mode_t mode);
long long time_usec(void);
long long time_ms(void);
char *safe_mkdtemp(char *template);
int safe_mkstemp(char *template);
char *safe_strncpy(char *s1, const char *s2, size_t len);
int get_blkid(const char *dev_path, char *uuid, unsigned int uuid_size);
char parse_boolean(const char *s);
int drop_privs(void);
int timeshell(char *command, char *stdout_str, char *stderr_str, int max_size, int timeout);

#ifdef _UNIT_TEST
int main(int argc, char **argv);
#endif /* _UNIT_TEST */

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
    int missing_helpers = 0;
    char **tmp_helpers_path = helpers_path;

    if (helpers_path == NULL)
        tmp_helpers_path = (char **)EUCA_ZALLOC(num_helpers, sizeof(char *));

    for (int i = 0; i < num_helpers; i++) {
        struct stat statbuf;
        int done = 0;

        // full path was given, so it just needs to be verified
        if (helpers_path != NULL && helpers_path[i] != NULL) {
            int rc = stat(helpers_path[i], &statbuf);
            if (!rc && S_ISREG(statbuf.st_mode)) {
                done++;
            }

        } else {                // no full path was given, so search $PATH
            char *tok = NULL, *toka = NULL, *path = NULL, *helper = NULL, *save = NULL, *savea = NULL;

            tok = getenv("PATH");
            if (!tok) {
                missing_helpers = -1;
                goto cleanup;
            }

            path = strdup(tok);
            if (!path) {
                missing_helpers = -1;
                goto cleanup;
            }

            {                   // append some Eucalyptus-specific locations to $PATH
                char *euca = getenv(EUCALYPTUS_ENV_VAR_NAME);
                if (euca == NULL) {
                    euca = "";
                }
                char *locations[] = {
                    ":" EUCALYPTUS_LIBEXEC_DIR,
                    ":" EUCALYPTUS_HELPER_DIR,
                    ":" EUCALYPTUS_SBIN_DIR,
                    NULL
                };
                for (int i = 0; locations[i]; i++) {
                    char lpath[MAX_PATH];
                    snprintf(lpath, sizeof(lpath), locations[i], euca);
                    char *newpath = strdupcat(path, lpath);
                    if (newpath == NULL) {
                        missing_helpers = -1;
                        goto cleanup;
                    }
                    path = newpath;
                }
            }

            tok = strtok_r(path, ":", &save);
            done = 0;
            while (tok && !done) {
                helper = strdup(helpers[i]);
                toka = strtok_r(helper, ",", &savea);
                while (toka && !done) {
                    char file[MAX_PATH];

                    snprintf(file, MAX_PATH, "%s/%s", tok, toka);
                    int rc = stat(file, &statbuf);
                    if (!rc) {
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
            logprintfl(EUCATRACE, "did not find '%s' in path\n", helpers[i]);
        } else {
            logprintfl(EUCATRACE, "found '%s' at '%s'\n", helpers[i], tmp_helpers_path[i]);
        }
    }

cleanup:

    if (helpers_path == NULL) {
        for (int i = 0; i < num_helpers; i++)
            EUCA_FREE(tmp_helpers_path[i]);
        EUCA_FREE(tmp_helpers_path);
    }

    return missing_helpers;
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
    int rc;
    fd_set rfds;
    struct timeval tv;

    if (timeout <= 0)
        timeout = 1;

    FD_ZERO(&rfds);
    FD_SET(fd, &rfds);

    tv.tv_sec = timeout;
    tv.tv_usec = 0;

    rc = select(fd + 1, &rfds, NULL, NULL, &tv);
    if (rc <= 0) {
        // timeout
        logprintfl(EUCAERROR, "select() timed out for read: timeout=%d\n", timeout);
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
    const char *euca_home;
    if (euca_home_supplied && strlen(euca_home_supplied)) {
        euca_home = euca_home_supplied;
    } else if (getenv(EUCALYPTUS_ENV_VAR_NAME) && strlen(getenv(EUCALYPTUS_ENV_VAR_NAME))) {
        euca_home = getenv(EUCALYPTUS_ENV_VAR_NAME);
    } else {                    // we'll assume root
        euca_home = "";
    }

    char *old_path = getenv("PATH");
    if (old_path == NULL)
        old_path = "";

    char new_path[4098];
    snprintf(new_path, sizeof(new_path), EUCALYPTUS_DATA_DIR ":"    // (connect|disconnect iscsi, get_xen_info, getstats, get_sys_info)
             EUCALYPTUS_SBIN_DIR ":"    // (eucalyptus-cloud, euca_conf, euca_sync_key, euca-* admin commands)
             EUCALYPTUS_LIBEXEC_DIR ":" // (rootwrap, mountwrap)
             "%s", euca_home, euca_home, euca_home, old_path);
    return setenv("PATH", new_path, TRUE);
}

//!
//! Wrapper around waitpid() that retries waiting until
//! a timeout, specified in seconds, occurs. Return value
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
    if (timeout_sec < 0)        // do not allow negative timeouts
        timeout_sec = 0;

    *status = 1;                //! @todo remove this once we know that no callers rely on status to detect timeout

    int rc = waitpid(pid, status, WNOHANG);
    time_t elapsed_usec = 0;
    while (rc == 0 && elapsed_usec < (timeout_sec * 1000000)) {
        usleep(10000);
        elapsed_usec += 10000;
        rc = waitpid(pid, status, WNOHANG);
    }
    if (rc == 0) {
        logprintfl(EUCAERROR, "waitpid() timed out: pid=%d\n", pid);
    }
    return (rc);
}

//!
//!
//!
//! @param[in] func
//! @param[in] ...
//!
//! @return 0 if the parameter is valid or 1 if not
//!
//! @todo check to see if boolean logic would be more appropriate
//!
int param_check(char *func, ...)
{
    int fail;
    va_list al;

    if (!func) {
        return (1);
    }

    va_start(al, func);
    fail = 0;
    if (!strcmp(func, "vnetGenerateDHCP") || !strcmp(func, "vnetKickDHCP")) {
        vnetConfig *a = va_arg(al, vnetConfig *);
        if (!a) {
            fail = 1;
        }
    } else if (!strcmp(func, "vnetAddPublicIP") || !strcmp(func, "vnetAddDev")) {
        vnetConfig *a = va_arg(al, vnetConfig *);
        char *b = va_arg(al, char *);
        if (!a || !b) {
            fail = 1;
        }
    } else if (!strcmp(func, "vnetAddHost")) {
        vnetConfig *a = NULL;
        char *b = NULL;
        char *c = NULL;
        int d = 0;

        a = va_arg(al, vnetConfig *);
        b = va_arg(al, char *);
        c = va_arg(al, char *);
        d = va_arg(al, int);
        if (!a || !b || (d < 0) || (d > NUMBER_OF_VLANS - 1)) {
            fail = 1;
        }
    } else if (!strcmp(func, "vnetGetNextHost")) {
        vnetConfig *a = va_arg(al, vnetConfig *);
        char *b = va_arg(al, char *);
        char *c = va_arg(al, char *);
        int d = va_arg(al, int);
        if (!a || !b || !c || d < 0 || d > NUMBER_OF_VLANS - 1) {
            fail = 1;
        }
    } else if (!strcmp(func, "vnetDelHost") || !strcmp(func, "vnetEnableHost") || !strcmp(func, "vnetDisableHost")) {
        vnetConfig *a = va_arg(al, vnetConfig *);
        char *b = va_arg(al, char *);
        char *c = va_arg(al, char *);
        int d = va_arg(al, int);
        if (!a || (!b && !c) || d < 0 || d > NUMBER_OF_VLANS - 1) {
            fail = 1;
        }
    } else if (!strcmp(func, "vnetDeleteChain") || !strcmp(func, "vnetCreateChain")) {
        vnetConfig *a = va_arg(al, vnetConfig *);
        char *b = va_arg(al, char *);
        char *c = va_arg(al, char *);
        if (!a || !b || !c) {
            fail = 1;
        }
    } else if (!strcmp(func, "vnetTableRule")) {
        vnetConfig *a = va_arg(al, vnetConfig *);
        char *b = va_arg(al, char *);
        char *c = va_arg(al, char *);
        char *d = va_arg(al, char *);
        char *e = va_arg(al, char *);
        char *f = va_arg(al, char *);
        char *g = va_arg(al, char *);
        if (!a || !b || !c || !d || (!e && !f && !g)) {
            fail = 1;
        }
    } else if (!strcmp(func, "vnetSetVlan")) {
        vnetConfig *a = va_arg(al, vnetConfig *);
        int b = va_arg(al, int);
        char *c = va_arg(al, char *);
        char *d = va_arg(al, char *);
        if (!a || b < 0 || b >= NUMBER_OF_VLANS || !c || !d) {
            fail = 1;
        }
    } else if (!strcmp(func, "vnetDelVlan")) {
        vnetConfig *a = va_arg(al, vnetConfig *);
        int b = va_arg(al, int);
        if (!a || b < 0 || b >= NUMBER_OF_VLANS) {
            fail = 1;
        }
    } else if (!strcmp(func, "vnetInit")) {
        vnetConfig *a = NULL;
        char *b = NULL;
        char *c = NULL;
        char *d = NULL;
        int e = 0;

        a = va_arg(al, vnetConfig *);
        b = va_arg(al, char *);
        c = va_arg(al, char *);
        d = va_arg(al, char *);
        e = va_arg(al, int);
        if (!a || !b || !c || d < 0) {
            fail = 1;
        }
    }

    va_end(al);

    if (fail) {
        logprintfl(EUCAERROR, "incorrect input parameters to function %s\n", func);
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
//! @return 0 if the PID exists or 1 if not
//!
int check_process(pid_t pid, char *search)
{
    char file[MAX_PATH], buf[1024];
    FILE *FH = NULL;
    int rc, ret = 0;

    snprintf(file, MAX_PATH, "/proc/%d/cmdline", pid);
    rc = check_file(file);
    if (!rc) {
        // cmdline exists
        ret = 1;
        if (search) {
            // check if cmdline contains 'search'
            FH = fopen(file, "r");
            if (FH) {
                bzero(buf, 1024);
                while (fgets(buf, 1024, FH)) {
                    char *p;
                    while ((p = memchr(buf, '\0', 1024))) {
                        *p = 'X';
                    }
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
    } else {
        ret = 1;
    }
    return (ret);
}

//!
//! make sure 'dir' is a directory or a soft-link to one
//! and that it is readable by the current user (1 on error)
//!
//! @param[in] dir
//!
//! @return 0 if dir is a directory or 1 if not
//!
int check_directory(const char *dir)
{
    if (!dir) {
        return (1);
    }

    char checked_dir[MAX_PATH];
    snprintf(checked_dir, sizeof(checked_dir), "%s", dir);

    struct stat mystat;
    int rc = lstat(checked_dir, &mystat);
    if (rc < 0)
        return 1;

    // if a soft link, append '/' and try lstat() again
    if (!S_ISDIR(mystat.st_mode) && S_ISLNK(mystat.st_mode)) {
        snprintf(checked_dir, sizeof(checked_dir), "%s/", dir);
        rc = lstat(checked_dir, &mystat);
        if (rc < 0)
            return 1;
    }

    if (!S_ISDIR(mystat.st_mode))
        return 1;

    DIR *d = opendir(checked_dir);
    if (d == NULL)
        return 1;

    closedir(d);
    return 0;
}

//!
//! Check if a file is newer than the given timestamp
//!
//! @param[in] file the file name string
//! @param[in] mtime the timestamp to compare with
//!
//! @return 0 if the file is newer than timestamp or 1 if not
//!
//! @todo use boolean instead
//!
int check_file_newer_than(char *file, time_t mtime)
{
    struct stat mystat;
    int rc;

    if (!file) {
        return (1);
    } else if (mtime <= 0) {
        return (0);
    }

    bzero(&mystat, sizeof(struct stat));
    rc = stat(file, &mystat);
    if (rc) {
        return (1);
    }
    if (mystat.st_mtime > mtime) {
        return (0);
    }
    return (1);
}

//!
//!
//!
//! @param[in] file
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int check_block(const char *file)
{
    int rc;
    struct stat mystat;
    char *rpath;

    if (!file) {
        return (EUCA_ERROR);
    }
    rpath = realpath(file, NULL);
    if (!rpath) {
        return (EUCA_ERROR);
    }
    rc = lstat(rpath, &mystat);
    EUCA_FREE(rpath);
    if (rc < 0 || !S_ISBLK(mystat.st_mode)) {
        return (EUCA_ERROR);
    }
    return (EUCA_OK);
}

//!
//! Checks if a file is a readable regular file.
//!
//! @param[in] file
//!
//! @return EUCA_OK if the file is readable, EUCA_ERROR otherwise
//!
int check_file(const char *file)
{
    int rc;
    struct stat mystat;

    if (!file) {
        return (EUCA_ERROR);
    }

    rc = lstat(file, &mystat);
    if (rc < 0 || !S_ISREG(mystat.st_mode)) {
        return (EUCA_ERROR);
    }
    return (EUCA_OK);
}

//!
//! Check if a path exists
//!
//! @param[in] path
//!
//! @return EUCA_OK if the path exists, othewise EUCA_ERROR is returned.
//!
int check_path(const char *path)
{
    int rc;
    struct stat mystat;

    if (!path) {
        return (EUCA_ERROR);
    }

    rc = lstat(path, &mystat);
    if (rc < 0) {
        return (EUCA_ERROR);
    }
    return (EUCA_OK);
}

//!
//! obtains size & available bytes of a file system that 'path' resides on
//! path may be a symbolic link, which will get resolved.
//!
//! @param[in]  path
//! @param[out] fs_bytes_size
//! @param[out] fs_bytes_available
//! @param[out] fs_id
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR
//!         and EUCA_IO_ERROR.
//!
int statfs_path(const char *path, unsigned long long *fs_bytes_size, unsigned long long *fs_bytes_available, int *fs_id)
{
    if (path == NULL)
        return (EUCA_ERROR);

    char cpath[PATH_MAX];
    errno = 0;
    if (realpath(path, cpath) == NULL) {    // will convert a path with symbolic links and '..' into canonical form
        logprintfl(EUCAERROR, "failed to resolve %s (%s)\n", path, strerror(errno));
        return (EUCA_IO_ERROR);
    }

    struct statfs fs;
    if (statfs(cpath, &fs) == -1) { // obtain the size and ID info from the file system of the canonical path
        logprintfl(EUCAERROR, "failed to stat %s (%s)\n", cpath, strerror(errno));
        return (EUCA_IO_ERROR);
    }
    *fs_id = hash_code_bin((char *)&fs.f_fsid, sizeof(fsid_t));
    *fs_bytes_size = (long long)fs.f_bsize * (long long)(fs.f_blocks);
    *fs_bytes_available = (long long)fs.f_bsize * (long long)(fs.f_bavail);

    logprintfl(EUCADEBUG, "path '%s' resolved\n", path);
    logprintfl(EUCADEBUG, "  to '%s' with ID %0x\n", cpath, *fs_id);
    logprintfl(EUCADEBUG, "  of size %llu bytes with available %llu bytes\n", *fs_bytes_size, *fs_bytes_available);

    return (EUCA_OK);
}

//!
//! given string *stringp, replace occurences of source with destination
//! and return the new string in stringp
//!
//! @param[in] stringp
//! @param[in] source
//! @param[in] destination
//!
//! @return the newly formatted string
//!
//! @note caller is reponsible to free the result
//!
char *replace_string(char **stringp, char *source, char *destination)
{
    char *start = NULL, *substart = NULL, *tok = NULL, *new_string = NULL;
    if (source == NULL || destination == NULL)
        return NULL;
    char *buf;
    int maxlen = 32768 * 2;

    buf = EUCA_ALLOC(maxlen, sizeof(char));
    new_string = EUCA_ZALLOC(maxlen, sizeof(char)); //! @todo this has to be dynamic
    if (!buf || !new_string) {
        fprintf(stderr, "replace_string: out of memory\n");
        EUCA_FREE(buf);
        EUCA_FREE(new_string);
        return NULL;
    }

    start = *stringp;
    substart = start;
    tok = strstr(start, source);
    while (tok != NULL) {
        *tok = '\0';
        snprintf(buf, maxlen, "%s%s%s", new_string, substart, destination);
        strncpy(new_string, buf, maxlen);
        tok += strlen(source);
        substart = tok;
        tok = strstr(substart, source);
    }
    snprintf(buf, maxlen, "%s%s", new_string, substart);
    strncpy(new_string, buf, maxlen);
    EUCA_FREE(buf);
    EUCA_FREE(*stringp);
    *stringp = new_string;

    return new_string;
}

//!
//! do sscanf() on each line in lines[], returning upon first match
//! returns TRUE if there was match and FALSE otherwise
//!
//! @param[in] lines
//! @param[in] format
//! @param[in] varp
//!
//! @return TRUE if we have a match, FALSE otherwise
//!
boolean sscanf_lines(char *lines, char *format, void *varp)
{
    char *copy;
    char *start, *end;
    boolean found = FALSE;

    if (!lines)
        return found;
    copy = strdup(lines);
    if (!copy)
        return found;

    for (start = copy; start && *start != '\0' && !found; start = end + 1) {
        int newline = 0;

        for (end = start + 1; *end != '\n' && *end != '\0'; end++) ;
        if (*end == '\n') {
            *end = '\0';
            newline = 1;
        }

        if (sscanf(start, format, varp) == 1)
            found = 1;

        if (!newline) {
            end--;              /* so that start=='\0' */
        }
    }
    EUCA_FREE(copy);

    return found;
}

//!
//!
//!
//! @param[in] fp
//!
//! @return a pointer to the file output string
//!
//! @note caller is responsible to free the returned memory
//!
char *fp2str(FILE * fp)
{
#   define INCREMENT 512
    int buf_max = INCREMENT;
    int buf_current = 0;
    char *last_read;
    char *buf = NULL;

    if (fp == NULL)
        return NULL;
    do {
        // create/enlarge the buffer
        void *new_buf;
        if ((new_buf = EUCA_REALLOC(buf, buf_max, sizeof(char))) == NULL) {
            EUCA_FREE(buf);     // free partial buffer
            return NULL;
        }
        memset(new_buf + buf_current, 0, INCREMENT * sizeof(char));

        buf = new_buf;
        logprintfl(EUCAEXTREME, "enlarged buf to %d\n", buf_max);

        do {                    // read in until EOF or buffer is full
            last_read = fgets(buf + buf_current, buf_max - buf_current, fp);
            if (last_read != NULL) {
                buf_current = strlen(buf);
            } else {
                if (!feof(fp)) {
                    logprintfl(EUCAERROR, "failed while reading from file handle\n");
                    EUCA_FREE(buf);
                    return NULL;
                }
            }
            logprintfl(EUCAEXTREME, "read %d characters so far (max=%d, last=%s)\n", buf_current, buf_max, last_read ? "no" : "yes");
        } while (last_read && buf_max > buf_current + 1);   // +1 is needed for fgets() to put \0

        buf_max += INCREMENT;   // in case it is full
    } while (last_read);

    return buf;
}

//!
//! execute system(shell_command) and return stdout in new string pointed to by *stringp
//!
//! @param[in] shell_command
//!
//! @return a pointer to the stdout output
//!
//! @note the caller is responsible to free the returned memory
//!
char *system_output(char *shell_command)
{
    char *buf = NULL;
    FILE *fp;

    /* forks off command (this doesn't fail if command doesn't exist */
    logprintfl(EUCATRACE, "[%s]\n", shell_command);
    if ((fp = popen(shell_command, "r")) == NULL)
        return NULL;            /* caller can check errno */
    buf = fp2str(fp);
    pclose(fp);

    return buf;
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
char *getConfString(char configFiles[][MAX_PATH], int numFiles, char *key)
{
    int rc, i, done;
    char *tmpstr = NULL;

    done = 0;
    for (i = 0; i < numFiles && !done; i++) {
        rc = get_conf_var(configFiles[i], key, &tmpstr);
        if (rc == 1) {
            done++;
        }
    }
    if (tmpstr && strlen(tmpstr)) {
        char *tmpptr;
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
//! @param[in] path
//! @param[in] name
//! @param[in] value
//!
//! @return 1 on success, 0 on variable not found and -1 on error (parse or file not found)
//!
//! @note caller is responsible to free the memory allocated for 'value'
//!
int get_conf_var(const char *path, const char *name, char **value)
{
    FILE *f;
    char *buf, *ptr, *ret;
    int len;

    /* sanity check */
    if (path == NULL || path[0] == '\0' || name == NULL || name[0] == '\0' || value == NULL) {
        return -1;
    }
    *value = NULL;

    f = fopen(path, "r");
    if (f == NULL) {
        return -1;
    }

    len = strlen(name);
    buf = EUCA_ALLOC(32768, sizeof(char));
    while (fgets(buf, 32768, f)) {
        /* the process here is fairly simple: spaces are not
         * considered (unless between "") so we remove them
         * before every step. We look for the variable *name*
         * first, then for an = then for the value */
        for (ptr = buf; (*ptr != '\0') && isspace((int)*ptr); ptr++) ;
        if (strncmp(ptr, name, len) != 0) {
            continue;
        }
        for (ptr += len; (*ptr != '\0') && isspace((int)*ptr); ptr++) ;
        if (*ptr != '=') {
            continue;
        }
        /* we are in business */
        for (ptr++; (*ptr != '\0') && isspace((int)*ptr); ptr++) ;
        if (*ptr == '"') {
            /* we have a quote, we need the companion */
            ret = ++ptr;
            while ((*ptr != '"')) {
                if (*ptr == '\0') {
                    /* something wrong happened */
                    fclose(f);
                    EUCA_FREE(buf);
                    return -1;
                }
                ptr++;
            }
        } else {
            /* well we get the single word right after the = */
            ret = ptr;
            while (!isspace((int)*ptr) && *ptr != '#' && *ptr != '\0') {
                ptr++;
            }
        }
        *ptr = '\0';
        *value = strdup(ret);
        if (*value == NULL) {
            fclose(f);
            EUCA_FREE(buf);
            return -1;
        }
        fclose(f);
        EUCA_FREE(buf);
        return 1;
    }
    fclose(f);
    EUCA_FREE(buf);
    return 0;
}

//!
//!
//!
//! @param[in] value
//!
void free_char_list(char **value)
{
    int i;

    if (value == NULL || *value == NULL) {
        return;
    }

    for (i = 0; value[i] != NULL; i++) {
        EUCA_FREE(value[i]);
    }
    EUCA_FREE(value);
}

//!
//!
//!
//! @param[in] v
//!
//! @return a pointer to a char list or NULL if any error occured
//!
char **from_var_to_char_list(const char *v)
{
    char *value, **tmp, *ptr, *w, a;
    int i;

    /* sanity check */
    if (v == NULL || v[0] == '\0') {
        return NULL;
    }
    tmp = EUCA_ZALLOC(1, sizeof(char *));
    if (tmp == NULL) {
        return NULL;
    }
    value = strdup(v);
    if (value == NULL) {
        EUCA_FREE(tmp);
        return NULL;
    }
    tmp[0] = NULL;

    i = 0;
    ptr = value;
    for (i = 0, ptr = value; *ptr != '\0'; ptr++) {
        /* let's look for the beginning of the word */
        for (; *ptr != '\0' && isspace((int)*ptr); ptr++) ;
        if (*ptr == '\0') {
            /* end of string with no starting word: we are
             * done here */
            break;
        }
        w = ptr;                /* beginning of word */
        for (ptr++; *ptr != '\0' && !isspace((int)*ptr); ptr++) ;

        /* found the end of word */
        a = *ptr;
        *ptr = '\0';

        tmp = EUCA_REALLOC(tmp, (i + 2), sizeof(char *));
        if (tmp == NULL) {
            EUCA_FREE(value);
            return NULL;
        }
        tmp[i] = strdup(w);
        if (tmp[i] == NULL) {
            free_char_list(tmp);
            EUCA_FREE(value);
            return NULL;
        }
        tmp[++i] = NULL;

        /* now we need to check if we were at the end of the
         * string */
        if (a == '\0') {
            break;
        }
    }
    EUCA_FREE(value);

    return tmp;
}

//!
//! implements Java's String.hashCode() on a string
//!
//! @param[in] s
//!
//! @return the computed hash code. If 's' is NULL, 0 will be returned
//!
int hash_code(const char *s)
{
    int code = 0;
    int i = 0;

    if (!s)
        return code;

    int len = strlen((char *)s);
    for (i = 0; i < len; i++) {
        code = 31 * code + (unsigned char)s[i];
    }

    return code;
}

//!
//! implements Java's String.hashCode() on a buffer of some size
//!
//! @param[in] buf
//! @param[in] buf_size
//!
//! @return the result of hash_code()
//!
//! @see hash_code()
//!
int hash_code_bin(const char *buf, int buf_size)
{
    char *buf_str = EUCA_ALLOC(((2 * buf_size) + 1), sizeof(char));
    if (buf_str == NULL)
        return -1;

    for (int i = 0; i < buf_size; i++) {
        snprintf(buf_str + (i * 2), 2, "%0x", *(buf + i));
    }
    buf_str[2 * buf_size] = '\0';

    int code = hash_code(buf_str);
    EUCA_FREE(buf_str);

    return code;
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
    static char out[OUTSIZE];   //! @todo malloc this?
    int len = (int)strlen(s);
    snprintf(out, OUTSIZE, "length=%d buf[n-1]=%i hash=%d", len, (int)((signed char)s[len - 1]), hash_code(s));
    return out;
}

//!
//! daemonize and store pid in pidfile. if pidfile exists and contained
//! pid is daemon already running, do nothing.  force option will first
//! kill and then re-daemonize */
//!
//! @param[in] cmd
//! @param[in] procname
//! @param[in] pidfile
//! @param[in] force
//! @param[in] rootwrap
//!
//! @return EUCA_OK on success. If an error occured, EUCA_ERROR is returned or
//!         the result of the daemonrun().
//!
//! @see daemonrun()
//!
int daemonmaintain(char *cmd, char *procname, char *pidfile, int force, char *rootwrap)
{
    int rc, found, ret;
    char cmdstr[MAX_PATH], file[MAX_PATH];
    FILE *FH;

    if (!cmd || !procname) {
        return (EUCA_ERROR);
    }

    if (pidfile) {
        found = 0;
        rc = check_file(pidfile);
        if (!rc) {
            char *pidstr = NULL;
            // pidfile exists
            pidstr = file2str(pidfile);
            if (pidstr) {
                snprintf(file, MAX_PATH, "/proc/%s/cmdline", pidstr);
                if (!check_file(file)) {
                    FH = fopen(file, "r");
                    if (FH) {
                        if (fgets(cmdstr, MAX_PATH, FH)) {
                            if (strstr(cmdstr, procname)) {
                                // process is running, and is indeed procname
                                found = 1;
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

    if ((ret = daemonrun(cmd, pidfile)) == 0) {
        // success
        //    if (pidfile) {
        //      char pidstr[32];
        //      snprintf(pidstr, 32, "%d", *dpid);
        //      rc = write2file(pidfile, pidstr);
        //    }
    }

    return (ret);
}

//!
//! daemonize and run 'cmd', returning pid of the daemonized process
//!
//! @param[in] incmd
//! @param[in] pidfile
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR
//!         and EUCA_THREAD_ERROR
//!
int daemonrun(char *incmd, char *pidfile)
{
    int pid, sid, i, status, rc;
    char **argv = NULL, *cmd;

    if (!incmd) {
        return (EUCA_ERROR);
    }

    pid = fork();
    if (pid < 0) {
        return (EUCA_THREAD_ERROR);
    }
    if (!pid) {
        char *tok = NULL, *ptr = NULL;
        int idx, rc;
        struct sigaction newsigact = { {NULL} };

        newsigact.sa_handler = SIG_DFL;
        newsigact.sa_flags = 0;
        sigemptyset(&newsigact.sa_mask);
        sigprocmask(SIG_SETMASK, &newsigact.sa_mask, NULL);
        sigaction(SIGTERM, &newsigact, NULL);

        rc = daemon(0, 0);
        // become parent of session
        sid = setsid();

        // construct argv
        cmd = strdup(incmd);
        idx = 0;
        argv = EUCA_ZALLOC(1, sizeof(char *));
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
            char pidstr[32];
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
//! given printf-style arguments, run the resulting string in the shell
//!
//! @param[in] fmt
//! @param[in] ...
//!
//! @return the result of the system() call.
//!
int vrun(const char *fmt, ...)
{
    char buf[MAX_PATH];
    int e;

    va_list ap;
    va_start(ap, fmt);
    vsnprintf(buf, MAX_PATH, fmt, ap);
    va_end(ap);

    logprintfl(EUCAINFO, "[%s]\n", buf);
    if ((e = system(buf)) != 0) {
        logprintfl(EUCAERROR, "system(%s) failed with %d\n", buf, e);   //! @todo remove?
    }
    return e;
}

//!
//! given a file path, prints it to stdout
//!
//! @param[in] file_name
//!
//! @return the number of bytes written to stdout
//!
int cat(const char *file_name)
{
    int got = 0;
    int put = 0;
    char buf[BUFSIZE];

    int fd = open(file_name, O_RDONLY);
    if (fd == -1) {
        // we should print some error
        return put;
    }
    while ((got = read(fd, buf, BUFSIZE)) > 0) {
        put += write(1, buf, got);
    }
    close(fd);
    return put;
}

//!
//! "touch" a file, creating if necessary
//!
//! @param[in] path
//!
//! @return EUCA_OK on success or EUCA_IO_ERROR on failure
//!
int touch(const char *path)
{
    int ret = EUCA_OK;
    int fd = -1;

    if ((fd = open(path, O_WRONLY | O_CREAT | O_NONBLOCK, 0644)) >= 0) {
        close(fd);
        if (utime(path, NULL) != 0) {
            logprintfl(EUCAERROR, "failed to adjust time for %s (%s)\n", path, strerror(errno));
            ret = EUCA_IO_ERROR;
        }
    } else {
        logprintfl(EUCAERROR, "failed to create/open file %s (%s)\n", path, strerror(errno));
        ret = EUCA_IO_ERROR;
    }
    return ret;
}

//!
//! diffs two files: 0=same, -N=different, N=error
//!
//! @param[in] path1
//! @param[in] path2
//!
//! @return 0=same, -N=different, N=error
//!
int diff(const char *path1, const char *path2)
{
    int fd1, fd2;
    char buf1[BUFSIZE], buf2[BUFSIZE];

    if ((fd1 = open(path1, O_RDONLY)) < 0) {
        logprintfl(EUCAERROR, "failed to open %s\n", path1);
    } else if ((fd2 = open(path2, O_RDONLY)) < 0) {
        logprintfl(EUCAERROR, "failed to open %s\n", path2);
        close(fd1);
    } else {
        int read1, read2;
        do {
            read1 = read(fd1, buf1, BUFSIZE);
            read2 = read(fd2, buf2, BUFSIZE);
            if (read1 != read2)
                break;
            if (read1 && memcmp(buf1, buf2, read1))
                break;
        } while (read1);
        close(fd1);
        close(fd2);
        return (-(read1 + read2));  /* both should be 0s if files are equal */
    }
    return EUCA_ERROR;
}

//!
//! sums up sizes of files in the directory, as well as the size of the
//! directory itself; no subdirectories are allowed - if there are any, this
//! returns -1
//!
//! @param[in] path
//!
//! @return the sommation of all file size in a directory
//!
long long dir_size(const char *path)
{
    struct stat mystat;
    DIR *dir;
    long long size = 0;

    if ((dir = opendir(path)) == NULL) {
        logprintfl(EUCAWARN, "unopeneable directory %s\n", path);
        return -1;
    }
    if (stat(path, &mystat) < 0) {
        logprintfl(EUCAWARN, "could not stat %s\n", path);
        closedir(dir);
        return -1;
    }
    size += (long long)mystat.st_size;

    struct dirent *dir_entry;
    while ((dir_entry = readdir(dir)) != NULL) {
        char *name = dir_entry->d_name;
        unsigned char type = dir_entry->d_type;

        if (!strcmp(".", name) || !strcmp("..", name))
            continue;

        if (DT_REG != type) {
            logprintfl(EUCAWARN, "non-regular (type=%d) file %s/%s\n", type, path, name);
            size = -1;
            break;
        }

        char filepath[MAX_PATH];
        snprintf(filepath, MAX_PATH, "%s/%s", path, name);
        if (stat(filepath, &mystat) < 0) {
            logprintfl(EUCAWARN, "could not stat file %s\n", filepath);
            size = -1;
            break;
        }

        size += (long long)mystat.st_size;
    }

    closedir(dir);
    return size;
}

//!
//!
//!
//! @param[in] path
//! @param[in] str
//!
//! @return EUCA_OK on success or EUCA_IO_ERROR on failure
//!
int write2file(const char *path, char *str)
{
    FILE *FH = NULL;

    FH = fopen(path, "w");
    if (FH) {
        fprintf(FH, "%s", str);
        fclose(FH);
    } else {
        return (EUCA_IO_ERROR);
    }
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] path
//! @param[in] limit
//!
//! @return the result of teh file2str() call
//!
//! @see file2str()
//!
//! @note the caller must free the memory when done.
//!
char *file2strn(const char *path, const ssize_t limit)
{
    struct stat mystat;
    if (stat(path, &mystat) < 0) {
        logprintfl(EUCAERROR, "could not stat file %s\n", path);
        return NULL;
    }
    if (mystat.st_size > limit) {
        logprintfl(EUCAERROR, "file %s exceeds the limit (%d) in file2strn()\n", path, limit);
        return NULL;
    }
    return file2str(path);
}

//!
//! read file 'path' into a new string
//!
//! @param[in] path
//!
//! @return the string content of the given file
//!
//! @note the caller must free the memory when done.
//!
char *file2str(const char *path)
{
    char *content = NULL;
    int file_size;

    struct stat mystat;
    if (stat(path, &mystat) < 0) {
        logprintfl(EUCAERROR, "could not stat file %s\n", path);
        return content;
    }
    file_size = mystat.st_size;

    if ((content = EUCA_ALLOC((file_size + 1), sizeof(char))) == NULL) {
        logprintfl(EUCAERROR, "out of memory reading file %s\n", path);
        return content;
    }

    int fp;
    if ((fp = open(path, O_RDONLY)) < 0) {
        logprintfl(EUCAERROR, "failed to open file %s\n", path);
        EUCA_FREE(content);
        content = NULL;
        return content;
    }

    int bytes;
    int bytes_total = 0;
    int to_read = ((SSIZE_MAX) < file_size) ? (SSIZE_MAX) : file_size;
    char *p = content;
    while ((bytes = read(fp, p, to_read)) > 0) {
        bytes_total += bytes;
        p += bytes;
        if (to_read > (file_size - bytes_total)) {
            to_read = file_size - bytes_total;
        }
    }
    close(fp);

    if (bytes < 0) {
        logprintfl(EUCAERROR, "failed to read file %s\n", path);
        EUCA_FREE(content);
        content = NULL;
        return content;
    }

    *p = '\0';
    return content;
}

//!
//!
//!
//! @param[in] file
//! @param[in] size
//! @param[in] mode
//!
//! @return the newly allocated string or NULL if any error occured
//!
//! @note caller is responsible to free the allocated memory
//!
char *file2str_seek(char *file, size_t size, int mode)
{
    int rc, fd;
    struct stat statbuf;
    char *ret = NULL;

    if (!file || size <= 0) {
        logprintfl(EUCAERROR, "bad input parameters\n");
        return (NULL);
    }

    ret = EUCA_ZALLOC(size, sizeof(char));
    if (!ret) {
        logprintfl(EUCAERROR, "out of memory!\n");
        return (NULL);
    }

    rc = stat(file, &statbuf);
    if (rc >= 0) {
        fd = open(file, O_RDONLY);
        if (fd >= 0) {
            if (mode == 1) {
                rc = lseek(fd, (off_t) (-1 * size), SEEK_END);
                if (rc < 0) {
                    rc = lseek(fd, (off_t) 0, SEEK_SET);
                    if (rc < 0) {
                        logprintfl(EUCAERROR, "cannot seek\n");
                        EUCA_FREE(ret);
                        close(fd);
                        return (NULL);
                    }
                }
            }

            rc = read(fd, ret, (size) - 1);
            close(fd);
        } else {
            logprintfl(EUCAERROR, "cannot open '%s' read-only\n", file);
            EUCA_FREE(ret);
            return (NULL);
        }
    } else {
        logprintfl(EUCAERROR, "cannot stat console_output file '%s'\n", file);
        EUCA_FREE(ret);
        return (NULL);
    }

    return (ret);
}

//!
//! extract string from str bound by 'begin' and 'end'
//!
//! @param[in] str
//! @param[in] begin
//! @param[in] end
//!
//! @return the newly constructed string or NULL if any error occured.
//!
//! @note caller is responsible to free the returned memory
//!
char *str2str(const char *str, const char *begin, const char *end)
{
    char *buf = NULL;

    if (str == NULL || begin == NULL || end == NULL || strlen(str) < 3 || strlen(begin) < 1 || strlen(end) < 1) {
        logprintfl(EUCAERROR, "called with bad parameters\n");
        return buf;
    }

    char *b = strstr(str, begin);
    if (b == NULL) {
        logprintfl(EUCAERROR, "beginning string '%s' not found\n", begin);
        return buf;
    }

    char *e = strstr(str, end);
    if (e == NULL) {
        logprintfl(EUCAERROR, "end string '%s' not found\n", end);
        return buf;
    }

    b += strlen(begin);         // b now points at the supposed content
    int len = e - b;
    if (len < 0) {
        logprintfl(EUCAERROR, "there is nothing between '%s' and '%s'\n", begin, end);
        return buf;
    }

    if (len > BUFSIZE - 1) {
        logprintfl(EUCAERROR, "string between '%s' and '%s' is too long\n", begin, end);
        return buf;
    }

    buf = EUCA_ALLOC(len + 1, sizeof(char));
    if (buf != NULL) {
        strncpy(buf, b, len);
        buf[len] = '\0';
    }

    return buf;
}

//!
//! extract integer from str bound by 'begin' and 'end'
//!
//! @param[in] str
//! @param[in] begin
//! @param[in] end
//!
//! @return the integer value from the string and its parameters
//!
long long str2longlong(const char *str, const char *begin, const char *end)
{
    long long val = -1L;
    char *buf = str2str(str, begin, end);
    if (buf != NULL) {
        val = atoll(buf);
        EUCA_FREE(buf);
    }

    return val;
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
    uint32_t a, b;
    a = *(uint32_t *) ina;
    b = *(uint32_t *) inb;
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
    int rc;
    char *pidstr;

    if (!pidfile || !procname || sig < 0 || check_file(pidfile)) {
        return (1);
    }

    rc = 1;
    pidstr = file2str(pidfile);
    if (pidstr) {
        logprintfl(EUCADEBUG, "calling safekill with pid %d\n", atoi(pidstr));
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
//! @return
//!
int safekill(pid_t pid, char *procname, int sig, char *rootwrap)
{
    char cmdstr[MAX_PATH], file[MAX_PATH], cmd[MAX_PATH];
    FILE *FH;
    int ret;

    if (pid < 2 || !procname) {
        return (1);
    }

    snprintf(file, MAX_PATH, "/proc/%d/cmdline", pid);
    if (check_file(file)) {
        return (1);
    }

    FH = fopen(file, "r");
    if (FH) {
        if (!fgets(cmdstr, MAX_PATH, FH)) {
            fclose(FH);
            return (1);
        }
        fclose(FH);
    } else {
        return (1);
    }

    ret = 1;

    // found running process
    if (strstr(cmdstr, procname)) {
        // passed in cmd matches running cmd
        if (rootwrap) {
            snprintf(cmd, MAX_PATH, "%s kill -%d %d", rootwrap, sig, pid);
            ret = system(cmd) >> 8;
        } else {
            ret = kill(pid, sig);
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
    return (a > b ? a : b);
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
    return (a < b ? a : b);
}

//!
//! copies contents of src to dst, possibly overwriting whatever is in dst
//!
//! @param[in] src
//! @param[in] dst
//!
//! @return EUCA_OK on success or EUCA_IO_ERROR on failure
//!
int copy_file(const char *src, const char *dst)
{
    struct stat mystat;

    if (stat(src, &mystat) < 0) {
        logprintfl(EUCAERROR, "cannot stat '%s'\n", src);
        return (EUCA_IO_ERROR);
    }

    int ifp = open(src, O_RDONLY);
    if (ifp < 0) {
        logprintfl(EUCAERROR, "failed to open the input file '%s'\n", src);
        return (EUCA_IO_ERROR);
    }

    int ofp = open(dst, O_WRONLY | O_CREAT, 0600);
    if (ofp < 0) {
        logprintfl(EUCAERROR, "failed to create the ouput file '%s'\n", dst);
        close(ifp);
        return (EUCA_IO_ERROR);
    }
#   define _BUFSIZE 16384
    char buf[_BUFSIZE];
    ssize_t bytes;
    int ret = EUCA_OK;

    while ((bytes = read(ifp, buf, _BUFSIZE)) > 0) {
        if (write(ofp, buf, bytes) < 1) {
            logprintfl(EUCAERROR, "failed while writing to '%s'\n", dst);
            ret = EUCA_IO_ERROR;
            break;
        }
    }
    if (bytes < 0) {
        logprintfl(EUCAERROR, "failed while writing to '%s'\n", dst);
        ret = EUCA_IO_ERROR;
    }

    close(ifp);
    close(ofp);

    return ret;
}

//!
//!
//!
//! @param[in] file_path
//!
//! @return the size of the file
//!
long long file_size(const char *file_path)
{
    struct stat mystat;
    int err = stat(file_path, &mystat);
    if (err < 0)
        return (long long)err;
    return (long long)(mystat.st_size);
}

//!
//! tolower for strings
//!
//! @param[in] s the string to convert to lower case
//!
//! @return a pointer to the lower care result
//!
//! @note result must be freed by the caller
//!
char *strduplc(const char *s)
{
    char *lc = strdup(s);
    for (int i = 0; i < strlen(s); i++) {
        lc[i] = tolower(lc[i]);
    }
    return lc;
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
    char *ret = NULL;

    int name_start = -1;
    int name_end = -1;
    int tag_start = -1;

    for (const char *p = xml; *p; p++) {

        if (*p == '<') {        // found a new tag
            tag_start = (p - xml);  // record the char so its offset can be returned

            *closing = 0;
            if (*(p + 1) == '/' || *(p + 1) == '?') {
                if (*(p + 1) == '/')    // if followed by '/' then it is a "closing" tag
                    *closing = 1;
                name_start = (p - xml + 2);
                p++;
            } else {
                name_start = (p - xml + 1);
            }
            continue;
        }

        if (*p == ' ' && name_start != -1 && name_end == -1) {  // a name may be terminated by a space
            name_end = (p - 1 - xml);
            continue;
        }

        if (*p == '>') {
            if (name_start == -1)   // never saw '<', error
                break;
            if (p < xml + 2)    // tag is too short, error
                break;
            const char *last_ch = p - 1;
            if (*last_ch == '/' || *last_ch == '?') {
                *single = 1;    // preceded by '/' then it is a "single" tag
                last_ch--;
            } else {
                *single = 0;
            }

            if (name_start != -1 && name_end == -1) {   // a name may be terminated by '/' or '>' or '?'
                name_end = (last_ch - xml);
            }

            if ((name_end - name_start) >= 0) { // we have a name rather than '<>'
                ret = EUCA_ZALLOC(name_end - name_start + 2, sizeof(char));
                if (ret == NULL)
                    break;
                strncpy(ret, xml + name_start, name_end - name_start + 1);
                *start = tag_start;
                *end = p - xml;
            }
            break;
        }
    }

    return ret;
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
    char *ret = NULL;

    int tag_start;
    int tag_end = -1;
    int single;
    int closing = 0;
    char *name;
#define _STK_SIZE 64
    char *n_stk[_STK_SIZE];
    const char *c_stk[_STK_SIZE];
    int stk_p = -1;

    // iterate over tags until the matching xpath is reached or
    // until no more tags are found in the 'xml'
    bzero(n_stk, sizeof(char *) * _STK_SIZE);

    for (int xml_offset = 0; (name = next_tag(xml + xml_offset, &tag_start, &tag_end, &single, &closing)) != NULL; xml_offset += tag_end + 1) {
        if (single) {
            // not interested in singles because we are looking for content

        } else if (!closing) {  // opening a tag
            // put name and pointer to content onto the stack
            stk_p++;
            if (stk_p == _STK_SIZE) // exceeding stack size, error
                goto cleanup;
            n_stk[stk_p] = strduplc(name);  // put a lower-case-only copy onto stack
            c_stk[stk_p] = xml + xml_offset + tag_end + 1;

        } else {                // closing tag
            // get the name in all lower-case, for consistency with xpath
            char *name_lc = strduplc(name);
            EUCA_FREE(name);
            name = name_lc;

            // name doesn't match last seen opening tag, error
            if (stk_p >= 0) {
                if (!n_stk[stk_p] || (strcmp(n_stk[stk_p], name) != 0)) {
                    goto cleanup;
                }
            }
            // construct the xpath of the closing tag based on stack contents
            char xpath_cur[MAX_PATH] = "";
            for (int i = 0; i <= stk_p; i++) {
                if (i > 0)
                    strncat(xpath_cur, "/", MAX_PATH);
                strncat(xpath_cur, n_stk[i], MAX_PATH);
            }

            // pop the stack whether we have a match or not
            if (stk_p < 0)      // past the bottom of the stack, error
                goto cleanup;
            const char *contp = c_stk[stk_p];
            int cont_len = xml + xml_offset + tag_start - contp;
            EUCA_FREE(n_stk[stk_p]);
            stk_p--;

            // see if current xpath matches the requested one
            if (strcmp(xpath, xpath_cur) == 0) {
                char *cont = EUCA_ZALLOC(cont_len + 1, sizeof(char));
                if (cont == NULL)
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
    EUCA_FREE(name);            // for exceptions
    for (int i = 0; i <= stk_p; i++)
        EUCA_FREE(n_stk[i]);    // free everything on the stack
    return ret;
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

    if (xml == NULL || xpath == NULL)
        return NULL;
    char *xpath_l = strduplc(xpath);    // lower-case copy of requested xpath
    if (xpath_l != NULL) {
        ret = find_cont(xml, xpath_l);
        EUCA_FREE(xpath_l);
    }

    return ret;
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
    if (!uri || !uriType || !host || !strlen(uriType) || !strlen(host)) {
        return (EUCA_ERROR);
    }

    uri[0] = '\0';
    strncat(uri, uriType, strlen(uriType));
    strncat(uri, "://", 3);

    strncat(uri, host, strlen(host));

    if (port > 0) {
        char tmp[32];
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
    char *tok, *start;

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
    //  printf("HERE: %s %s\n", start, tok);

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
//! returns a new string in which 'new' is appended to 'original'
//! and frees 'original'
//!
//! @param[in] original
//! @param[in] new
//!
//! @return a pointer to the newly allocated string
//!
char *strdupcat(char *original, char *new)
{
    int len = 0;
    int olen = 0;

    if (original) {
        olen = strlen(original);
        len += olen;
    }

    if (new) {
        len += strlen(new);
    }

    char *ret = EUCA_ZALLOC(len + 1, sizeof(char));
    if (ret) {
        if (original) {
            strncat(ret, original, len);
            EUCA_FREE(original);
        }

        if (new) {
            strncat(ret, new, len - olen);
        }
    }

    return ret;
}

//!
//! given path=A/B/C and only A existing, create A/B and, unless
//! is_file_path==1, also create A/B/C directory
//!
//! @param[in] path
//! @param[in] is_file_path
//! @param[in] user
//! @param[in] group
//! @param[in] mode
//!
//! @return 0 = path already existed, 1 = created OK, -1 = error
//!
int ensure_directories_exist(const char *path, int is_file_path, const char *user, const char *group, mode_t mode)
{
    int len = strlen(path);
    char *path_copy = NULL;
    int ret = 0;
    int i;

    if (len > 0)
        path_copy = strdup(path);

    if (path_copy == NULL)
        return -1;

    for (i = 0; i < len; i++) {
        struct stat buf;
        int try_dir = 0;

        if (path[i] == '/' && i > 0) {  // dir path, not root
            path_copy[i] = '\0';
            try_dir = 1;

        } else if (path[i] != '/' && i + 1 == len) {    // last one
            if (!is_file_path)
                try_dir = 1;
        }

        if (try_dir) {
            if (stat(path_copy, &buf) == -1) {
                logprintfl(EUCAINFO, "creating path %s\n", path_copy);

                if (mkdir(path_copy, mode) == -1) {
                    logprintfl(EUCAERROR, "failed to create path %s: %s\n", path_copy, strerror(errno));

                    EUCA_FREE(path_copy);
                    return -1;
                }
                ret = 1;        // we created a directory

                if (diskutil_ch(path_copy, user, group, mode) != EUCA_OK) {
                    logprintfl(EUCAERROR, "failed to change perms on path %s\n", path_copy);
                    EUCA_FREE(path_copy);
                    return -1;
                }
                //                chmod (path_copy, mode); // ensure perms are right despite mask
            }
            path_copy[i] = '/'; // restore the slash
        }
    }

    EUCA_FREE(path_copy);
    return ret;
}

//!
//! time since 1970 in microseconds
//!
//! @return the time since 1970 in microseconds
//!
long long time_usec(void)
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (long long)tv.tv_sec * 1000000 + tv.tv_usec;
}

//!
//! time since 1970 in milliseconds
//!
//! @return the time since 1970 in milliseconds
//!
long long time_ms(void)
{
    return time_usec() / 1000;
}

//!
//! ensure the temp file is only readable by the user
//!
//! @param[in] template
//!
//! @return -1 on failure or the corresponding files descriptor for the temp file
//!
char *safe_mkdtemp(char *template)
{
    mode_t u;
    char *ret = NULL;
    u = umask(0077);
    ret = mkdtemp(template);
    umask(u);
    return (ret);
}

//!
//! ensure the temp file is only readable by the user
//!
//! @param[in] template
//!
//! @return -1 on failure or the corresponding files descriptor for the temp file
//!
int safe_mkstemp(char *template)
{
    mode_t u;
    int ret;
    u = umask(0077);
    ret = mkstemp(template);
    umask(u);
    return (ret);
}

//!
//! ensure the string is aways 0-terminated
//!
//! @param[in] s1
//! @param[in] s2
//! @param[in] len
//!
//! @return the number of character copied in s1
//!
char *safe_strncpy(char *s1, const char *s2, size_t len)
{
    char *ret = strncpy(s1, s2, len);
    s1[len - 1] = '\0';
    return (ret);
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
    char cmd[1024];
    snprintf(cmd, sizeof(cmd), "blkid %s", dev_path);   // option '-u filesystem' did not exist on Centos
    char *blkid_output = system_output(cmd);
    if (blkid_output == NULL)
        return (EUCA_ERROR);
    int ret = EUCA_ERROR;
    char *first_char = strstr(blkid_output, "UUID=\"");
    if (first_char) {
        first_char += 6;
        char *last_char = strchr(first_char, '"');
        if (last_char && ((last_char - first_char) > 0)) {
            *last_char = '\0';
            safe_strncpy(uuid, first_char, uuid_size);
            assert(0 == strcmp(uuid, first_char));
            ret = EUCA_OK;
        }
    }
    EUCA_FREE(blkid_output);

    return ret;
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
    char *lc = strduplc(s);
    char val = 0;

    if (strcmp(lc, "y") == 0 || strcmp(lc, "yes") == 0 || strcmp(lc, "t") == 0 || strcmp(lc, "true") == 0) {
        val = 1;
    } else if (strcmp(lc, "n") == 0 || strcmp(lc, "no") == 0 || strcmp(lc, "f") == 0 || strcmp(lc, "false") == 0) {
        val = 0;
    } else
        logprintfl(EUCAERROR, "failed to parse value '%s' as boolean", lc);
    EUCA_FREE(lc);

    return val;
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
    char buf[16384] = { 0 };    // man-page said this is enough

    s = getpwnam_r(EUCALYPTUS_ADMIN, &pwd, buf, sizeof(buf), &result);
    if (result == NULL)
        return (EUCA_ERROR);    // not found if s==0, check errno otherwise

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

    // force nonempty on all arguments to simplify the logic
    assert(command);
    assert(stdout_str);
    assert(stderr_str);

    if (pipe(stdoutfds) < 0) {
        logprintfl(EUCAERROR, "error: failed to create pipe for stdout: %s\n", strerror_r(errno, errorBuf, 256));
        return -1;
    }
    if (pipe(stderrfds) < 0) {
        logprintfl(EUCAERROR, "error: failed to create pipe for stderr: %s\n", strerror_r(errno, errorBuf, 256));
        return -1;
    }

    child_pid = fork();
    if (child_pid == 0) {
        close(stdoutfds[0]);
        if (dup2(stdoutfds[1], STDOUT_FILENO) < 0) {
            logprintfl(EUCAERROR, "error: failed to dup2 stdout: %s\n", strerror_r(errno, errorBuf, 256));
            exit(1);
        }
        close(stdoutfds[1]);

        close(stderrfds[0]);
        if (dup2(stderrfds[1], STDERR_FILENO) < 0) {
            logprintfl(EUCAERROR, "error: failed to dup2 stderr: %s\n", strerror_r(errno, errorBuf, 256));
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
        return -1;
    }

    memset(stdout_str, 0, max_size);
    memset(stderr_str, 0, max_size);
    stdout_toread = stderr_toread = max_size - 1;

    maxfd = stdoutfds[0] > stderrfds[0] ? stdoutfds[0] : stderrfds[0];

    start_time = time(NULL);
    for (;;) {
        fd_set rfds;
        struct timeval tv;
        int retval;
        int readn;

        FD_ZERO(&rfds);
        FD_SET(stdoutfds[0], &rfds);
        FD_SET(stderrfds[0], &rfds);

        tv.tv_sec = 1;
        tv.tv_usec = 0;

        retval = select(maxfd + 1, &rfds, (fd_set *) 0, (fd_set *) 0, &tv);
        if (retval > 0) {
            if (FD_ISSET(stdoutfds[0], &rfds) && stdout_toread > 0) {
                readn = read(stdoutfds[0], stdout_str, stdout_toread);
                if (readn > 0) {
                    stdout_toread -= readn;
                    stdout_str += readn;
                } else {
                    break;
                }
            }
            if (FD_ISSET(stderrfds[0], &rfds) && stderr_toread > 0) {
                readn = read(stderrfds[0], stderr_str, stderr_toread);
                if (readn > 0) {
                    stderr_toread -= readn;
                    stderr_str += readn;
                } else {
                    break;
                }
            }
        } else if (retval < 0) {
            logprintfl(EUCAWARN, "warning: select error on pipe read: %s\n", strerror_r(errno, errorBuf, 256));
            break;
        }
        if (time(NULL) - start_time > timeout) {
            logprintfl(EUCAWARN, "warning: read timeout\n");
            break;
        }
    }
    close(stdoutfds[0]);
    close(stderrfds[0]);

    remaining_time = timeout - (time(NULL) - start_time);
    rc = timewait(child_pid, &status, remaining_time);
    if (rc) {
        rc = WEXITSTATUS(status);
    } else {
        kill(child_pid, SIGKILL);
        logprintfl(EUCAERROR, "warning: shell execution timeout\n");
        return -1;
    }
    return rc;
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
    int errors = 0;
    char cwd[1024] = { 0 };
    char *s = NULL;
    FILE *fp = NULL;
    char **d = NULL;
    char uuid[64] = "";
    char dev_path[32] = { 0 };
    char *devs[] = { "hda", "hdb", "hdc", "hdd", "sda", "sdb", "sdc", "sdd", NULL };

    getcwd(cwd, sizeof(cwd));
    srandom(time(NULL));

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
