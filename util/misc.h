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

#ifndef _INCLUDE_MISC_H_
#define _INCLUDE_MISC_H_

//!
//! @file util/misc.h
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdarg.h>
#include <unistd.h>             // ssize_t
#include <sys/types.h>          // mode_t
#include <linux/limits.h>
#include <stdint.h>             // uint32_t

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define TRUE                                     1
#define FALSE                                    0

#ifndef MAX_PATH
#define MAX_PATH                                 4096
#endif /* ! MAX_PATH */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef unsigned char boolean;  //! @todo move this somewhere more global?

#include "log.h"                // so everyone picks up the logging functions

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
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name These must be defined by each euca component
extern const char *euca_client_component_name;
extern const char *euca_this_component_name;
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int verify_helpers(char **helpers, char **helpers_path, int num_helpers);
int timeread(int fd, void *buf, size_t bytes, int timeout);
int add_euca_to_path(const char *euca_home_supplied);
pid_t timewait(pid_t pid, int *status, int timeout_sec);
int param_check(const char *func, ...);
int check_process(pid_t pid, char *search);
int check_directory(const char *dir);
int check_file_newer_than(const char *file, time_t mtime);
int check_block(const char *file);
int check_file(const char *file);
int check_path(const char *path);
int statfs_path(const char *path, unsigned long long *fs_bytes_size, unsigned long long *fs_bytes_available, int *fs_id);
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
int vrun(const char *fmt, ...) __attribute__ ((__format__ (__printf__, 1, 2)));
int cat(const char *file_name);
int touch(const char *path);
int diff(const char *path1, const char *path2);
long long dir_size(const char *path);
int write2file(const char *path, char *str);
char *file2strn(const char *path, const ssize_t limit);
char *file2str(const char *path);
char *file2str_seek(char *file, size_t size, int mode);
int uint32compar(const void *ina, const void *inb);
int safekillfile(char *pidfile, char *procname, int sig, char *rootwrap);
int safekill(pid_t pid, char *procname, int sig, char *rootwrap);
int maxint(int a, int b);
int minint(int a, int b);
int copy_file(const char *src, const char *dst);
long long file_size(const char *file_path);
char *xpath_content(const char *xml, const char *xpath);
int construct_uri(char *uri, char *uriType, char *host, int port, char *path);
int tokenize_uri(char *uri, char *uriType, char *host, int *port, char *path);
int ensure_directories_exist(const char *path, int is_file_path, const char *user, const char *group, mode_t mode);
long long time_usec(void);
long long time_ms(void);
char *safe_mkdtemp(char *template);
int safe_mkstemp(char *template);
int get_blkid(const char *dev_path, char *uuid, unsigned int uuid_size);
char parse_boolean(const char *s);
int drop_privs(void);
int timeshell(char *command, char *stdout_str, char *stderr_str, int max_size, int timeout);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define SP(_a)                                   (((_a) != NULL) ? (_a) : "UNSET")
#define RANDALPHANUM()                           ((rand() % 2) ? (rand() % 26 + 97) : ((rand() % 2) ? (rand() % 26 + 65) : (rand() % 10 + 48)))

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_MISC_H_ */
