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

#ifndef _INCLUDE_EUCANETD_UTIL_H_
#define _INCLUDE_EUCANETD_UTIL_H_

//!
//! @file net/eucanetd_util.h
//! Definition of various generic system utility specific APIs not found
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

#include <netinet/in.h>
#include <euca_network.h>
#include "eucanetd.h"

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
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! common API to restart the DHCP server
int eucanetd_stop_dhcpd_server(eucanetdConfig *config);
int eucanetd_kick_dhcpd_server(eucanetdConfig *config);

//! API to run a program and make sure only one copy of the program is running
int eucanetd_run_program(const char *psPidFilePath, const char *psRootWrap, boolean force, const char *psProgram, ...);

//! Safely terminate a program executed by eucanetd_run_program()
int eucanetd_kill_program(pid_t pid, const char *psProgramName, const char *psRootwrap);

int unlink_handler_file(char *filename);
int truncate_file(char *filename);

int cidrsplit(char *ipname, char **ippart, int *nmpart);

int getdevinfo(char *dev, u32 ** outips, u32 ** outnms, int *len);
int euca_getifaddrs(u32 **if_ips, int *max_if_ips);

long int timer_get_interval_millis(struct timeval *ts, struct timeval *te);
long int timer_get_interval_usec(struct timeval *ts, struct timeval *te);
long int eucanetd_timer(struct timeval *t);
long int eucanetd_timer_usec(struct timeval *t);
long int eucanetd_get_timestamp();

int euca_exec(const char *command);
int euca_exec_wait(int timeout_sec, const char *prefix, const char *first, ...);

int euca_split_string(char *string, char ***result, int *nmemb, char separator);

int euca_u32_set_insert(u32 **set, int *max_set, u32 value);
int euca_string_set_insert(char ***set, int *max_set, char *value);
char *euca_string_set_get(char **set, int max_set, char *value);

void *zalloc_check(size_t nmemb, size_t size);
void *realloc_check(void *ptr, size_t nmemb, size_t size);
void *append_ptrarr(void *arr, int *max_arr, void *ptr);
void *compact_ptrarr(void *arr, int *max_arr);

int free_ptrarr(void *arr, int nmemb);

void get_stack_trace ();

u32 euca_version_dot2hex(const char *ver);

int euca_buffer_snprintf(char **buf, int *buf_len, const char *format, ...);

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

#ifndef EUCA_ZALLOC_C
#define EUCA_ZALLOC_C(_nmemb, _size)             zalloc_check((_nmemb), (_size))
#define EUCA_REALLOC_C(_ptr, _nmemb, _size)      realloc_check((_ptr), (_nmemb), (_size))
#endif /* ! EUCA_ZALLOC_C */

#ifndef EUCA_APPEND_PTRARR
#define EUCA_APPEND_PTRARR(_arr, _nmemb, _ptr)   append_ptrarr((_arr), (_nmemb), (_ptr))
#endif /* ! EUCA_APPEND_PTRARR */

#ifndef EUCA_GET_STACK_TRACE
#define EUCA_GET_STACK_TRACE()                   get_stack_trace()
#endif /* ! EUCA_GET_STACK_TRACE */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_EUCANETD_UTIL_H_ */
