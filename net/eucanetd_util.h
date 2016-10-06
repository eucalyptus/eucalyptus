// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
int euca_exec_wait(int timeout_sec, const char *file, ...);

int euca_u32_set_insert(u32 **set, int *max_set, u32 value);
int euca_string_set_insert(char ***set, int *max_set, char *value);

void *zalloc_check(size_t nmemb, size_t size);
void *realloc_check(void *ptr, size_t nmemb, size_t size);
void *append_ptrarr(void *arr, int *max_arr, void *ptr);
void get_stack_trace ();

u32 euca_version_dot2hex(const char *ver);

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
