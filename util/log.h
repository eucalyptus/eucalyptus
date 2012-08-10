// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2012 Eucalyptus Systems, Inc.
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
 ************************************************************************/

#ifndef LOG_H
#define LOG_H

enum {
    EUCAALL=0, 
    EUCATRACE, 
    EUCADEBUG3, 
    EUCADEBUG2, 
    EUCADEBUG, 
    EUCAINFO, 
    EUCAWARN, 
    EUCAERROR, 
    EUCAFATAL, 
    EUCAOFF
};

static char * log_level_names [] = {
    "ALL",
    "TRACE",
    "DEBUG3",
    "DEBUG2",
    "DEBUG",
    "INFO",
    "WARN",
    "ERROR",
    "FATAL",
    "OFF"
};

int log_level_int(const char *level);
void log_params_set(int log_level_in, int log_roll_number_in, long log_max_size_bytes_in);
void log_params_get(int *log_level_out, int *log_roll_number_out, long *log_max_size_bytes_out);
int log_file_set(const char * file);
int logfile(char *file, int in_loglevel, int in_logrollnumber);
int logprintf(const char *format, ...);
int logprintfl(int level, const char *format, ...);
int logcat (int debug_level, const char * file_name);

void eventlog(char *hostTag, char *userTag, char *cid, char *eventTag, char *other);

#endif

