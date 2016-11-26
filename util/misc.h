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

#ifndef _INCLUDE_MISC_H_
#define _INCLUDE_MISC_H_

//!
//! @file util/misc.h
//! Defines a variety of utility tools
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdarg.h>
#include <unistd.h>                    // ssize_t
#include <sys/types.h>                 // mode_t
#include <linux/limits.h>
#include <stdint.h>                    // uint32_t
#include <pthread.h>

#include <eucalyptus.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name Definition of the boolean values TRUE and FALSE

#undef TRUE
#define TRUE                                     1  //!< Defines the "TRUE" boolean value
#undef FALSE
#define FALSE                                    0  //!< Defines the "FALSE" boolean value
//! @}

#define NANOSECONDS_IN_SECOND           1000000000  //!< constant for conversion

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef unsigned char boolean;         //!< @todo move this somewhere more global?

#include "log.h"                       // so everyone picks up the logging functions
#include "euca_file.h"

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
int killwait(pid_t pid);
int check_process(pid_t pid, char *search);
char *system_output(char *shell_command);
char *getConfString(char configFiles[][EUCA_MAX_PATH], int numFiles, char *key);
int get_conf_var(const char *path, const char *name, char **value);
void free_char_list(char **value);
char **from_var_to_char_list(const char *v);
int hash_code(const char *s);
int hash_code_bin(const char *buf, int buf_size);
char *get_string_stats(const char *s);
int daemonmaintain(char *cmd, char *procname, char *pidfile, int force, char *rootwrap);
int daemonrun(char *incmd, char *pidfile);
int vrun(const char *fmt, ...) _attribute_format_(1, 2);
int uint32compar(const void *ina, const void *inb);
int safekillfile(const char *pidfile, const char *procname, int sig, const char *rootwrap);
int safekill(pid_t pid, const char *procname, int sig, const char *rootwrap);
int maxint(int a, int b);
int minint(int a, int b);
char *xpath_content(const char *xml, const char *xpath);
int construct_uri(char *uri, char *uriType, char *host, int port, char *path);
int tokenize_uri(char *uri, char *uriType, char *host, int *port, char *path);
long long time_usec(void);
long long time_ms(void);
int get_blkid(const char *dev_path, char *uuid, unsigned int uuid_size);
char parse_boolean(const char *s);
int drop_privs(void);
int timeshell_nb(char *command, int timeout, boolean logerr);
int timeshell(char *command, char *stdout_str, char *stderr_str, int max_size, int timeout);
int get_remoteDevForNC(const char *the_iqn, const char *remoteDev, char *remoteDevForNC, int remoteDevForNCLen);
int check_for_string_in_list(char *string, char **list, int count);
char **build_argv(const char *first, va_list va);
int euca_execvp_fd(pid_t * ppid, int *stdin_fd, int *stdout_fd, int *stderr_fd, char **argv);
int euca_execvp_fds(pid_t * ppid, int stdin_fd_in, int *stdin_fd_out, int stdout_fd_in, int *stdout_fd_out, int stderr_fd_in, int *stderr_fd_out, char **argv);
int euca_waitpid(pid_t pid, int *pStatus);
int euca_execlp_fd(pid_t * ppid, int *stdin_fd, int *stdout_fd, int *stderr_fd, const char *file, ...);
int euca_execvp_fds(pid_t * ppid, int stdin_fd_in, int *stdin_fd_out, int stdout_fd_in, int *stdout_fd_out, int stderr_fd_in, int *stderr_fd_out, char **argv);
int euca_execlp(int *pStatus, const char *file, ...);
int euca_execlp_redirect(int *pStatus, const char *stdin_path, const char *stdout_path, boolean stdout_append, const char *stderr_path, boolean stderr_append, const char *file, ...);
int euca_run_workflow_parser(const char *line, void *data);
int euca_execlp_log(int *pStatus, int (*custom_parser) (const char *line, void *data), void *parser_data, const char *file, ...);
char *get_username(void);
int euca_nanosleep(unsigned long long nsec);
void euca_srand(void);

//! global variable and functions for setting correlation id
//!
typedef struct threadCorrelationId_t {
    char correlation_id[128];
    pid_t pid;
    pthread_t tid;
    boolean pthread;
    struct threadCorrelationId_t *next;
} threadCorrelationId;
char *create_corrid(const char *);
threadCorrelationId *set_corrid(const char *corr_id);
threadCorrelationId *set_corrid_pthread(const char *corr_id, pthread_t);
threadCorrelationId *set_corrid_fork(const char *corr_id, pid_t);
void unset_corrid(threadCorrelationId *);
threadCorrelationId *get_corrid();

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

//! Macro to print a string that might be NULL. If NULL, the string "UNSET" is returned.
#define SP(_a)                                   (((_a) != NULL) ? (_a) : "UNSET")

//! Macro to print a string that might be NULL. If NULL, the string "" is returned.
#define NP(_a)                                   (((_a) != NULL) ? (_a) : "")

//! Macro to generate a randum alphanumeric number.
#define RANDALPHANUM()                           ((rand() % 2) ? (rand() % 26 + 97) : ((rand() % 2) ? (rand() % 26 + 65) : (rand() % 10 + 48)))

//! @{
//! @name MIN and MAX macros

#undef MIN
#undef MAX
#define MIN(_a, _b)                              (((_a) < (_b)) ? (_a) : (_b))
#define MAX(_a, _b)                              (((_a) > (_b)) ? (_a) : (_b))
#if 0
// Faster min/max macros
#define MIN(_a, _b)                              ((_b) + (((_a) - (_b)) & -((_a) < (_b))))
#define MAX(_a, _b)                              ((_a) - (((_a) - (_b)) & -((_a) < (_b))))
#endif /* 0 */

//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_MISC_H_ */
