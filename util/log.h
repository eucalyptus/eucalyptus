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

#ifndef LOG_H
#define LOG_H

#include "ipc.h" // sem

extern __thread const char * _log_curr_method;
extern __thread const char * _log_curr_file;
extern __thread int          _log_curr_line;

#define _EUCA_CONTEXT_SETTER (_log_curr_method=__FUNCTION__,\
                              _log_curr_file=__FILE__,\
                              _log_curr_line=__LINE__)

#define EUCAALL     0
#define EUCAEXTREME (_EUCA_CONTEXT_SETTER, 1)
#define EUCATRACE   (_EUCA_CONTEXT_SETTER, 2)
#define EUCADEBUG   (_EUCA_CONTEXT_SETTER, 3)
#define EUCAINFO    (_EUCA_CONTEXT_SETTER, 4)
#define EUCAWARN    (_EUCA_CONTEXT_SETTER, 5)
#define EUCAERROR   (_EUCA_CONTEXT_SETTER, 6)
#define EUCAFATAL   (_EUCA_CONTEXT_SETTER, 7)
#define EUCAOFF     8

static char * log_level_names [] = {
    "ALL",
    "EXTREME",
    "TRACE",
    "DEBUG",
    "INFO",
    "WARN",
    "ERROR",
    "FATAL",
    "OFF"
};

/////////////////////// prefix format
// T = timestamp
// L = loglevel
// p = PID
// t = thread id (same as PID in CC)
// m = method
// F = file:line_no
//
// p,t,m,F may be followed by (-)NNN,
//         '-' means left-justified
//         and NNN is max field size
/////////////////////////////////////
static char * log_level_prefix [] = {
    "",
    "T L t9 m-24 F-33 | ", // EXTREME
    "T L t9 m-24 | ",      // TRACE
    "T L t9 m-24 | ",      // DEBUG
    "T L | ",              // INFO
    "T L | ",              // WARN
    "T L | ",              // ERROR
    "T L | ",              // FATAL
    ""
};

#ifdef DEBUG
#define PRINTF(a) logprintf a
#else
#define PRINTF(a)
#endif

#ifdef DEBUG1
#define PRINTF1(a) logprintf a
#else
#define PRINTF1(a)
#endif

#ifdef DEBUGXML
#define PRINTF_XML(a) logprintf a
#else
#define PRINTF_XML(a)
#endif

int log_level_int(const char *level);
void log_params_set(int log_level_in, int log_roll_number_in, long log_max_size_bytes_in);
void log_params_get(int *log_level_out, int *log_roll_number_out, long *log_max_size_bytes_out);
int log_file_set(const char * file);
int log_prefix_set (const char * log_spec);
int log_sem_set (sem * s);
int logfile(char *file, int in_loglevel, int in_logrollnumber);
int logprintf(const char *format, ...);
int logprintfl(int level, const char *format, ...);
int logcat (int debug_level, const char * file_name);

void eventlog(char *hostTag, char *userTag, char *cid, char *eventTag, char *other);

#endif
