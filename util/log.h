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

#ifndef _INCLUDE_LOG_H_
#define _INCLUDE_LOG_H_

//!
//! @file util/log.h
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include "ipc.h"                       // sem

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

//! Defines the various Eucalyptus logging levels available
typedef enum log_level_e {
    EUCA_LOG_ALL = 0,
    EUCA_LOG_EXTREME,
    EUCA_LOG_TRACE,
    EUCA_LOG_DEBUG,
    EUCA_LOG_INFO,
    EUCA_LOG_WARN,
    EUCA_LOG_ERROR,
    EUCA_LOG_FATAL,
    EUCA_LOG_OFF,
} log_level_e;

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

//! To convert the various log level IDs to a readable format
extern const char *log_level_names[];

//!
//! prefix format
//! %T = timestamp
//! %L = loglevel
//! %p = PID
//! %t = thread id (same as PID in CC)
//! %m = method
//! %F = file:line_no
//! %s = max rss size, in MB
//!
//! p,t,m,F may be followed by (-)NNN,
//!         '-' means left-justified
//!         and NNN is max field size
//!
extern const char *log_level_prefix[];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int log_level_int(const char *level);
void log_params_set(int log_level_in, int log_roll_number_in, long log_max_size_bytes_in);
int log_level_get(void);
void log_params_get(int *log_level_out, int *log_roll_number_out, long *log_max_size_bytes_out);
int log_fp_set(FILE * fp);
int log_file_set(const char *file);
int log_prefix_set(const char *log_spec);
int log_facility_set(const char *facility, const char *component_name);
int log_sem_set(sem * s);
int logfile(const char *file, int log_level_in, int log_roll_number_in);
int logprintf(const char *format, ...) _attribute_format_(1, 2);
int logprintfl(const char *func, const char *file, int line, log_level_e level, const char *format, ...) _attribute_format_(5, 6);
int logcat(int debug_level, const char *file_path);

void eventlog(char *hostTag, char *userTag, char *cid, char *eventTag, char *other);
void log_dump_trace(char *buf, int buf_size);

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifdef DEBUG
#define PRINTF(a, args...)                       logprintf(a, ## args)
#else /* DEBUG */
#define PRINTF(a, args...)
#endif /* DEBUG */

#ifdef DEBUG1
#define PRINTF1(a, args...)                      logprintf(a, ## args)
#else /* DEBUG1 */
#define PRINTF1(a, args...)
#endif /* DEBUG1 */

#ifdef DEBUGXML
#define PRINTF_XML(a, args...)                   logprintf(a, ## args)
#else /* DEBUGXML */
#define PRINTF_XML(a, args...)
#endif /* DEBUGXML */

//! @{
//! @name Various log level logging macros

#define EUCALOG(_level, _format, args...)                                          \
{                                                                                  \
    if ((_level) >= log_level_get()) {                                             \
        logprintfl(__FUNCTION__, __FILE__, __LINE__, (_level), _format, ## args);  \
    }                                                                              \
}

#define LOGEXTREME(_format, args...)             EUCALOG(EUCA_LOG_EXTREME, _format, ## args)
#define LOGTRACE(_format, args...)               EUCALOG(EUCA_LOG_TRACE, _format, ## args)
#define LOGDEBUG(_format, args...)               EUCALOG(EUCA_LOG_DEBUG, _format, ## args)
#define LOGINFO(_format, args...)                EUCALOG(EUCA_LOG_INFO, _format, ## args)
#define LOGWARN(_format, args...)                EUCALOG(EUCA_LOG_WARN, _format, ## args)
#define LOGERROR(_format, args...)               EUCALOG(EUCA_LOG_ERROR, _format, ## args)
#define LOGFATAL(_format, args...)               EUCALOG(EUCA_LOG_FATAL, _format, ## args)

//! @}

//! @{
//! @name Various log level logging macros that adds a new line character

#define EUCALOGNL(_level, _format, args...)      EUCALOG((_level), _format "\n", ## args);
#define LOGEXTREMENL(_format, args...)           EUCALOGNL(EUCA_LOG_EXTREME, _format, ## args)
#define LOGTRACENL(_format, args...)             EUCALOGNL(EUCA_LOG_TRACE, _format, ## args)
#define LOGDEBUGNL(_format, args...)             EUCALOGNL(EUCA_LOG_DEBUG, _format, ## args)
#define LOGINFONL(_format, args...)              EUCALOGNL(EUCA_LOG_INFO, _format, ## args)
#define LOGWARNNL(_format, args...)              EUCALOGNL(EUCA_LOG_WARN, _format, ## args)
#define LOGERRORNL(_format, args...)             EUCALOGNL(EUCA_LOG_ERROR, _format, ## args)
#define LOGFATALNL(_format, args...)             EUCALOGNL(EUCA_LOG_FATAL, _format, ## args)

//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_LOG_H_ */
