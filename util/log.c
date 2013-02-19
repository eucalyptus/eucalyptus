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
//! @file util/log.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU
#include <string.h>
#include <stdarg.h>
#include <time.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/syscall.h>               // to get thread id
#include <sys/resource.h>              // rusage
#include <execinfo.h>                  // backtrace
#include <errno.h>
#define SYSLOG_NAMES                   // we want facilities as strings
#include <syslog.h>

#include "eucalyptus.h"
#include "log.h"
#include "misc.h"                      // TRUE/FALSE
#include "ipc.h"                       // semaphores
#include "euca_string.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define LOGLINEBUF                               101024 //!< enough for a log line, hopefully
#define MAX_FIELD_LENGTH                            100 //!< any prefix value beyond this will be truncated, even if in spec
#define DEFAULT_LOG_LEVEL                             4 //!< log level if none is specified (4==INFO)
#define LOGFH_DEFAULT                            stdout //!< without a file, this is where log output goes
#define USE_STANDARD_PREFIX                      "(standard)"   //!< a special string that means no custom prefix

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

//! To convert the various log level IDs to a readable format
const char *log_level_names[] = {
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
const char *log_level_prefix[] = {
    "",
    "%T %L %t9 %m-24 %F-33 |",         // EXTREME
    "%T %L %t9 %m-24 |",               // TRACE
    "%T %L %t9 %m-24 |",               // DEBUG
    "%T %L |",                         // INFO
    "%T %L |",                         // WARN
    "%T %L |",                         // ERROR
    "%T %L |",                         // FATAL
    ""
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! the log file stream that will remain open:
//! - unless do_close_fd==TRUE
//! - unless log file is moved or removed
//! - unless log file becomes too big and rolls over
static FILE *gLogFh = NULL;

//!< the current inode
static ino_t log_ino = -1;

//! @{
//! @name parameters, for now unmodifiable
static const boolean timelog = FALSE;  //!< change to TRUE for 'TIMELOG' entries
static const boolean do_close_fd = FALSE;   //!< whether to close log fd after each message
static const boolean do_stat_log = TRUE;    //!< whether to monitor file for changes
static char log_name[32] = "euca";     //!< name of the log, such as "euca-nc" or "euca-cc" for syslog
static const int syslog_options = 0;   //!< flags to be passed to openlog(), such as LOG_PID
//! @}

//! @{
//! @name these can be modified through setters
static int log_level = DEFAULT_LOG_LEVEL;
static int log_roll_number = 10;
static long log_max_size_bytes = MAXLOGFILESIZE;
static char log_file_path[EUCA_MAX_PATH] = "";
static char log_custom_prefix[34] = USE_STANDARD_PREFIX;    //!< any other string means use it as custom prefix
static sem *log_sem = NULL;            //!< if set, the semaphore will be used when logging & rotating logs
static int syslog_facility = -1;       //!< if not -1 then we are logging to a syslog facility
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int log_level_int(const char *level);
void log_params_set(int log_level_in, int log_roll_number_in, long log_max_size_bytes_in);
int log_level_get(void);
void log_params_get(int *log_level_out, int *log_roll_number_out, long *log_max_size_bytes_out);
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
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static FILE *get_file(boolean do_reopen);
static void release_file(void);
static int fill_timestamp(char *buf, int buf_size);
static int log_line(const char *line);
static int print_field_truncated(const char **log_spec, char *buf, int left, const char *field);

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
//! returns log level as integer given the name or -1 if the name is not valid
//! (used for parsing the setting in the config file)
//!
//! @param[in] level
//!
//! @return the log level index
//!
int log_level_int(const char *level)
{
    int l = 0;
    for (l = 0; l <= EUCA_LOG_OFF; l++) {
        if (!strcmp(level, log_level_names[l])) {
            return (l);
        }
    }
    return (-1);
}

//!
//! Log FILE pointer getter, which implements log rotation logic and tries to recover from log
//! file being moved away, perhaps by an external log rotation tool.
//!
//! The log file gets re-opened if it is currently closed or if reopening is explicitly requested
//! (do_reopen==TRUE). In case of failure, returns NULL.
//!
//! To avoid unpredictable behavior due to concurrency,  this function should be called while
//! holding a lock.
//!
//! @param[in] do_reopen
//!
//! @return a pointer to the lof file or NULL if any error occured
//!
static FILE *get_file(boolean do_reopen)
{
    // if max size is 0, there will be no logging except syslog, if configured
    if (log_max_size_bytes == 0)
        return NULL;

    // no log file has been set
    if (strlen(log_file_path) == 0)
        return LOGFH_DEFAULT;

    int fd = -1;
    if (gLogFh != NULL) {              // apparently the stream is still open
        boolean file_changed = FALSE;
        if (!do_reopen && do_stat_log) {    // we are not reopening for every write
            struct stat statbuf;
            int err = stat(log_file_path, &statbuf);
            if (err == -1) {           // probably file does not exist, perhaps because it was renamed
                file_changed = TRUE;
            } else if (statbuf.st_size < 1) {   // truncated externally, reopen
                file_changed = TRUE;
            } else if (log_ino != statbuf.st_ino) { // inode change, reopen just in case
                file_changed = TRUE;
            }
        }
        fd = fileno(gLogFh);           // try to get the file descriptor
        if (file_changed || do_reopen || fd < 0) {
            fclose(gLogFh);
            gLogFh = NULL;
        }
    }

retry:
    // open unless it is already is open
    if (gLogFh == NULL) {
        gLogFh = fopen(log_file_path, "a+");
        if (gLogFh == NULL) {
            return NULL;
        }
        fd = fileno(gLogFh);
        if (fd < 0) {
            fclose(gLogFh);
            gLogFh = NULL;
            return NULL;
        }
    }
    // see if it is time to rotate the log
    struct stat statbuf;
    int rc = fstat(fd, &statbuf);
    if (!rc) {
        log_ino = statbuf.st_ino;      // record the inode number of the currently opened log

        if (((long)statbuf.st_size > log_max_size_bytes) && (log_roll_number > 0)) {
            char oldFile[EUCA_MAX_PATH], newFile[EUCA_MAX_PATH];
            for (int i = log_roll_number - 1; i > 0; i--) {
                snprintf(oldFile, EUCA_MAX_PATH, "%s.%d", log_file_path, i - 1);
                snprintf(newFile, EUCA_MAX_PATH, "%s.%d", log_file_path, i);
                rename(oldFile, newFile);
            }
            snprintf(oldFile, EUCA_MAX_PATH, "%s", log_file_path);
            snprintf(newFile, EUCA_MAX_PATH, "%s.%d", log_file_path, 0);
            rename(oldFile, newFile);
            fclose(gLogFh);
            gLogFh = NULL;
            goto retry;
        }
    }

    return gLogFh;
}

//!
//! Log FILE pointer release. Should be called with a lock held in multi-threaded context.
//!
static void release_file(void)
{
    if (do_close_fd && gLogFh != NULL) {
        fclose(gLogFh);
        gLogFh = NULL;
    }
}

//!
//! setter for logging parameters except file path
//!
//! @param[in] log_level_in the log level to set
//! @param[in] log_roll_number_in the log roll number to set
//! @param[in] log_max_size_bytes_in the maximum file size in bytes to set
//!
void log_params_set(int log_level_in, int log_roll_number_in, long log_max_size_bytes_in)
{
    // update the log level
    if (log_level_in >= EUCA_LOG_ALL && log_level_in <= EUCA_LOG_OFF) {
        log_level = log_level_in;
    } else {
        log_level = DEFAULT_LOG_LEVEL;
    }

    // update the roll number limit
    if (log_roll_number_in >= 0 &&     // sanity check
        log_roll_number_in < 1000 &&   // sanity check
        log_roll_number != log_roll_number_in) {

        log_roll_number = log_roll_number_in;
    }
    // update the max size for any file
    if (log_max_size_bytes_in >= 0 && log_max_size_bytes != log_max_size_bytes_in) {
        log_max_size_bytes = log_max_size_bytes_in;
        if (get_file(FALSE))           // that will rotate log files if needed
            release_file();
    }
}

//!
//! Getter to retrieve the currently configured log level
//!
//! @return the currently configured log level
int log_level_get(void)
{
    return (log_level);
}

//!
//! getter for logging parameters except file path
//!
//! @param[out] log_level_out holder for the log level
//! @param[out] log_roll_number_out holder for the log roll number
//! @param[out] log_max_size_bytes_out holder for the maximum file size in bytes
//!
void log_params_get(int *log_level_out, int *log_roll_number_out, long *log_max_size_bytes_out)
{
    *log_level_out = log_level;
    *log_roll_number_out = log_roll_number;
    *log_max_size_bytes_out = log_max_size_bytes;
}

//!
//! Sets and opens the log file
//!
//! @param[in] file the file name of the log file. A NULL value unset the file
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int log_file_set(const char *file)
{
    if (file == NULL) {
        // NULL means standard output
        log_file_path[0] = '\0';
        return (EUCA_OK);
    }

    if (strcmp(log_file_path, file) == 0) {
        // hasn't changed
        return (EUCA_OK);
    }

    euca_strncpy(log_file_path, file, EUCA_MAX_PATH);
    if (get_file(TRUE) == NULL) {
        return (EUCA_ERROR);
    }
    release_file();
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] log_spec
//!
//! @return Always return EUCA_OK
//!
int log_prefix_set(const char *log_spec)
{
    // @todo eventually, enable empty prefix
    if ((log_spec == NULL) || (strlen(log_spec) == 0))
        euca_strncpy(log_custom_prefix, USE_STANDARD_PREFIX, sizeof(log_custom_prefix));
    else
        euca_strncpy(log_custom_prefix, log_spec, sizeof(log_custom_prefix));
    return (EUCA_OK);
}

//!
//! Sets the logging facility
//!
//! @param[in] facility the logging facility name string
//! @param[in] component_name the component name
//!
//! @return 0 on success or -1 on failure.
//!
int log_facility_set(const char *facility, const char *component_name)
{
    int facility_int = -1;

    if (facility && strlen(facility) > 0) {
        boolean matched = FALSE;
        for (CODE * c = facilitynames; c->c_name != NULL; c++) {
            if (strcmp(c->c_name, facility) == 0) {
                facility_int = c->c_val;
                matched = TRUE;
                break;
            }
        }
        if (!matched) {
            LOGERROR("unrecognized log facility '%s' requested, ignoring\n", facility);
            return -1;
        }
    }

    if (facility_int != syslog_facility) {
        syslog_facility = facility_int;
        if (component_name)
            snprintf(log_name, sizeof(log_name) - 1, "euca-%s", component_name);
        closelog();                    // in case it was open
        if (syslog_facility != -1) {
            LOGINFO("opening syslog '%s' in facility '%s'\n", log_name, facility);
            openlog(log_name, syslog_options, syslog_facility);
        }
    }

    return 0;
}

//!
//! Sets the logging semaphore
//!
//! @param[in] s a pointer to the logging semaphore
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int log_sem_set(sem * s)
{
    if (s == NULL)
        return (EUCA_ERROR);

    if (log_sem != NULL) {
        sem *old_log_sem = log_sem;
        sem_p(old_log_sem);
        if (log_sem != s) {
            log_sem = s;
        }
        sem_v(old_log_sem);
    } else {
        log_sem = s;
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] file the log file name string
//! @param[in] log_level_in the log level to set
//! @param[in] log_roll_number_in the log rolling number to set
//!
//! @return the result of the log_file_set() call.
//!
//! @see log_file_set()
//!
//! @todo legacy function, to be removed when no longer in use
//!
int logfile(const char *file, int log_level_in, int log_roll_number_in)
{
    log_params_set(log_level_in, log_roll_number_in, MAXLOGFILESIZE);
    return log_file_set(file);
}

//!
//! Print timestamp in YYYY-MM-DD HH:MM:SS format.
//!
//! @param[in,out] buf the string buffer to contain the formatted timestamp
//! @param[in]     buf_size the size of the given buffer
//!
//! @return the number of characters that it took up or 0 on error.
//!
static int fill_timestamp(char *buf, int buf_size)
{
    time_t t = time(NULL);
    struct tm tm;
    localtime_r(&t, &tm);
    return strftime(buf, buf_size, "%F %T", &tm);
}

//!
//! This is the function that ultimately dumps a buffer into a log.
//!
//! @param[in] line the string buffer to log
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
static int log_line(const char *line)
{
    int rc = EUCA_ERROR;

    if (log_sem)
        sem_prolaag(log_sem, FALSE);

    FILE *file = get_file(FALSE);
    if (file != NULL) {
        fprintf(file, "%s", line);
        fflush(file);
        release_file();
        rc = EUCA_OK;
    }

    if (log_sem)
        sem_verhogen(log_sem, FALSE);

    return rc;
}

//!
//! Log-printing function without a specific log level. It is essentially printf() that will go verbatim,
//! with just timestamp as prefix and at any log level, into the current log or stdout, if no log was open.
//!
//! @param[in] format the format of the log message
//! @param[in] ... the variable argument part filling the format
//!
//! @return 0 on success. If negative, error with vsnprintf() or if 1 means error with log_line()
//!
//! @todo Chuck to evaluate if we cannot standardize the error code returned.
//!
//! @see log_line()
//!
int logprintf(const char *format, ...)
{
    char buf[LOGLINEBUF];

    // start with current timestamp
    int offset = fill_timestamp(buf, sizeof(buf));

    // append the log message passed via va_list
    va_list ap;
    va_start(ap, format);
    int rc = vsnprintf(buf + offset, sizeof(buf) - offset - 1, format, ap);
    va_end(ap);
    if (rc < 0)
        return rc;

    return log_line(buf);
}

//!
//!
//!
//! @param[in] log_spec
//! @param[in] buf
//! @param[in] left
//! @param[in] field
//!
//! @return the number of bytes written in our string buffer 'buf'
//!
static int print_field_truncated(const char **log_spec, char *buf, int left, const char *field)
{
    boolean left_justify = FALSE;
    int in_field_len = strlen(field);
    int out_field_len = MAX_FIELD_LENGTH;
    if (in_field_len < out_field_len) {
        out_field_len = in_field_len;  // unless specified, we'll use length of the field or max
    }
    // first, look ahead down s[] to see if we have length 
    // and alignment specified (leading '-' means left-justified)
    const char *nstart = (*log_spec) + 1;
    if (*nstart == '-') {              // a leading zero
        left_justify = TRUE;
        nstart++;
    }
    char *nend;
    int i = (int)strtoll(nstart, &nend, 10);
    if (nstart != nend) {              // we have some digits
        *log_spec = nend - 1;          // move the pointer ahead so caller will skip digits
        if (i > 1 && i < 100) {        // sanity check
            out_field_len = i;
        }
    }
    // create a format string that would truncate the field
    // to len and then print the field into 's'
    if (left < (out_field_len + 1)) {  // not enough room left
        return -1;
    }
    // when right-justifying, we want to truncate the field on the left
    // (when left-justifying the snprintf below will truncate on the right)
    int offset = 0;
    if (left_justify == FALSE) {
        offset = in_field_len - out_field_len;
        if (offset < 0)
            offset = 0;
    }
    char format[10];
    snprintf(format, sizeof(format), "%%%s%ds", (left_justify) ? "-" : "", out_field_len);
    if (snprintf(buf, (out_field_len + 1), format, field + offset) < out_field_len)
        return -1;                     // error in snprintf

    return out_field_len;
}

//!
//! Main log-printing function, which will dump a line into a log, with a prefix appropriate for
//! the log level, given that the log level is above the threshold.
//!
//! @param[in] func the caller function name (i.e. __FUNCTION__)
//! @param[in] file the file in which the caller function reside (i.e. __FILE__)
//! @param[in] line the line at which this function was called (i.e. __LINE__)
//! @param[in] level the log level for this message
//! @param[in] format the format string of the message
//! @param[in] ... the variable argument part of the format
//!
//! @return 0 on success, -1 on failure with this function or 1 if log_line() failed.
//!
//! @see log_line()
//!
//! @todo Chuck to evaluate if we cannot standardize the error code returned.
//!
int logprintfl(const char *func, const char *file, int line, log_level_e level, const char *format, ...)
{
    int offset = 0;
    boolean custom_spec = FALSE;
    char buf[LOGLINEBUF] = "";
    const char *prefix_spec = NULL;

    // return if level is invalid or below the threshold
    if (level < log_level) {
        return 0;
    } else if (level < 0 || level > EUCA_LOG_OFF) {
        return -1;                     // unexpected log level
    }

    if (strcmp(log_custom_prefix, USE_STANDARD_PREFIX) == 0) {
        prefix_spec = log_level_prefix[log_level];
        custom_spec = FALSE;
    } else {
        prefix_spec = log_custom_prefix;
        custom_spec = TRUE;
    }

    // go over prefix format for the log level (defined in log.h or custom)
    for (;                             // prefix_spec is initialized above
         *prefix_spec != '\0'; prefix_spec++) {

        char *s = buf + offset;
        int left = sizeof(buf) - offset - 1;
        if (left < 1) {
            return -1;                 // not enough room in internal buffer for a prefix
        }
        // see if we have a formatting character or a regular one
        char c = prefix_spec[0];
        char cn = prefix_spec[1];
        if (c != '%'                   // not a special formatting char
            || (c == '%' && cn == '%') // formatting char, escaped
            || (c == '%' && cn == '\0')) {  // formatting char at the end
            s[0] = c;
            s[1] = '\0';
            offset++;
            if (c == '%' && cn == '%')
                prefix_spec++;         // swallow the one extra '%' in input
            continue;
        }
        prefix_spec++;                 // move past the '%' to the formatting char

        int size = 0;
        switch (*prefix_spec) {
        case 'T':                     // timestamp
            size = fill_timestamp(s, left);
            break;

        case 'L':{                    // log-level
                char l[6];
                euca_strncpy(l, log_level_names[level], 6); // we want hard truncation
                size = snprintf(s, left, "%5s", l);
                break;
            }
        case 'p':{                    // process ID
                char p[11];
                snprintf(p, sizeof(p), "%010d", getpid());  // 10 chars is enough for max 32-bit unsigned integer
                size = print_field_truncated(&prefix_spec, s, left, p);
                break;
            }
        case 't':{                    // thread ID
                char t[21];
                snprintf(t, sizeof(t), "%020d", (pid_t) syscall(SYS_gettid));   // 20 chars is enough for max 64-bit unsigned integer
                size = print_field_truncated(&prefix_spec, s, left, t);
                break;
            }
        case 'm':                     // method
            size = print_field_truncated(&prefix_spec, s, left, func);
            break;

        case 'F':{                    // file-and-line
                char file_and_line[64];
                snprintf(file_and_line, sizeof(file_and_line), "%s:%d", file, line);
                size = print_field_truncated(&prefix_spec, s, left, file_and_line);
                break;
            }

        case 's':{                    // max RSS of the process
                struct rusage u;
                bzero(&u, sizeof(struct rusage));
                getrusage(RUSAGE_SELF, &u);

                // unfortunately, many fields in 'struct rusage' aren't supported on Linux (notably: ru_ixrss, ru_idrss, ru_isrss)
                char size_str[64];
                snprintf(size_str, sizeof(size_str), "%05ld", u.ru_maxrss / 1024);
                size = print_field_truncated(&prefix_spec, s, left, size_str);
                break;
            }
        case '?':
            s[0] = '?';                // not supported currently
            s[1] = '\0';
            size = 1;
            break;

        default:
            s[0] = *prefix_spec;
            s[1] = '\0';
            size = 1;
            break;
        }

        if (size < 0) {
            logprintf("error in prefix construction in logprintfl()\n");
            return -1;                 // something went wrong in the snprintf()s above
        }
        offset += size;
    }

    // add a space between the prefix and the message proper
    if (offset > 0 && ((sizeof(buf) - offset - 1) > 0)) {
        buf[offset++] = ' ';
        buf[offset] = '\0';
    }
    // append the log message passed via va_list
    va_list ap;
    va_start(ap, format);
    int rc = vsnprintf(buf + offset, sizeof(buf) - offset - 1, format, ap);
    va_end(ap);
    if (rc < 0)
        return rc;

    if (syslog_facility != -1) {
        // log to syslog, at the appropriate level
        int l = LOG_DEBUG;             // euca DEBUG, TRACE, and EXTREME use syslog's DEBUG
        if (level == EUCA_LOG_ERROR)
            l = LOG_ERR;
        else if (level == EUCA_LOG_WARN)
            l = LOG_WARNING;
        else if (level == EUCA_LOG_INFO)
            l = LOG_INFO;
        if (custom_spec)
            syslog(l, buf);
        else
            syslog(l, buf + offset);
    }

    return log_line(buf);
}

//!
//! prints contents of an arbitrary file (at file_path) using logprintfl, thus dumping
//! its contents into a log
//!
//! @param[in] debug_level the log level
//! @param[in] file_path a path to the file to dump info into
//!
//! @return the number of lines written into the file
//!
int logcat(int debug_level, const char *file_path)
{
    int got = 0;
    char buf[LOGLINEBUF];

    FILE *fp = fopen(file_path, "r");
    if (!fp)
        return got;
    while (fgets(buf, LOGLINEBUF, fp)) {
        int l = strlen(buf);
        if (l < 0)
            break;
        if (l + 1 < LOGLINEBUF && buf[l - 1] != '\n') {
            buf[l++] = '\n';
            buf[l] = '\0';
        }
        EUCALOG(debug_level, buf);
        got += l;
    }
    fclose(fp);

    return got;
}

//!
//! eventlog() was used for some timing measurements, almost exclusively from
//! server-marshal.c, where SOAP requests are getting unmarshalled and mashalled.
//! May be considered a legacy function, given no current need for the measurements.
//!
//! @param[in] hostTag
//! @param[in] userTag
//! @param[in] cid
//! @param[in] eventTag
//! @param[in] other
//!
void eventlog(char *hostTag, char *userTag, char *cid, char *eventTag, char *other)
{
    double ts = 0.0;
    struct timeval tv = { 0 };
    char hostTagFull[256] = "";
    char hostName[256] = "";
    FILE *PH = NULL;

    if (timelog) {
        hostTagFull[0] = '\0';
        PH = popen("hostname", "r");
        if (PH) {
            if (fscanf(PH, "%256s", hostName) == 1) {
                snprintf(hostTagFull, 256, "%s/%s", hostName, hostTag);
            } else {
                snprintf(hostTagFull, 256, "%s", hostTag);
            }

            pclose(PH);

            snprintf(hostTagFull, 256, "%s/%s", hostName, hostTag);

            gettimeofday(&tv, NULL);
            ts = (double)tv.tv_sec + ((double)tv.tv_usec / 1000000.0);

            logprintf("TIMELOG %s:%s:%s:%s:%f:%s\n", hostTagFull, userTag, cid, eventTag, ts, other);
        }
    }
}

//!
//!
//!
//! @param[in] buf
//! @param[in] buf_size
//!
void log_dump_trace(char *buf, int buf_size)
{
    void *array[64];
    size_t size;
    char **strings;
    size_t i;

    size = backtrace(array, sizeof(array) / sizeof(void *));
    strings = backtrace_symbols(array, size);

    buf[0] = '\0';
    for (i = 0; i < size; i++) {
        int left = buf_size - 1 - strlen(buf);
        if (left < 0)
            break;
        char line[512];
        snprintf(line, sizeof(line), "\t%s\n", strings[i]);
        strncat(buf, line, left);
    }

    EUCA_FREE(strings);
}
