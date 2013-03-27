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
#include <sys/syscall.h> // to get thread id
#include <sys/resource.h> // rusage
#include <execinfo.h> // backtrace
#include <errno.h>
#define SYSLOG_NAMES // we want facilities as strings
#include <syslog.h>

#include "eucalyptus.h"
#include "log.h"
#include "misc.h" // TRUE/FALSE
#include "ipc.h" // semaphores

#define LOGLINEBUF 101024 // enough for a log line, hopefully
#define MAX_FIELD_LENGTH 100 // any prefix value beyond this will be truncated, even if in spec
#define DEFAULT_LOG_LEVEL 4 // log level if none is specified (4==INFO)
#define LOGFH_DEFAULT stdout // without a file, this is where log output goes
#define USE_STANDARD_PREFIX "(standard)" // a special string that means no custom prefix

// the log file stream that will remain open:
// - unless do_close_fd==TRUE
// - unless log file is moved or removed
// - unless log file becomes too big and rolls over
static FILE * LOGFH = NULL;
static ino_t log_ino = -1; // the current inode

// parameters, for now unmodifiable
static const boolean timelog = FALSE; // change to TRUE for 'TIMELOG' entries
static const boolean do_close_fd = FALSE; // whether to close log fd after each message
static const boolean do_stat_log = TRUE; // whether to monitor file for changes
static char log_name [32] = "euca"; // name of the log, such as "euca-nc" or "euca-cc" for syslog
static const int syslog_options = 0; // flags to be passed to openlog(), such as LOG_PID

// these can be modified through setters
static int log_level=DEFAULT_LOG_LEVEL;
static int log_roll_number=4;
static long log_max_size_bytes=MAXLOGFILESIZE;
static char log_file_path [EUCA_MAX_PATH] = "";
static char log_custom_prefix [34] = USE_STANDARD_PREFIX; // any other string means use it as custom prefix
static sem * log_sem = NULL; // if set, the semaphore will be used when logging & rotating logs
static int syslog_facility = -1; // if not -1 then we are logging to a syslog facility

// these are set by _EUCA_CONTEXT_SETTER, which is included in log-level macros, such as EUCAWARN
__thread const char * _log_curr_method = "";
__thread const char * _log_curr_file   = "";
__thread int          _log_curr_line   = 0;

// returns log level as integer given the name or
// -1 if the name is not valid
// (used for parsing the setting in the config file)
int log_level_int (const char * level)
{
    for (int l=0; l<=EUCAOFF; l++) {
        if (!strcmp (level, log_level_names[l])) {
            return l;
        }
    }
    return -1;
}

// Log FILE pointer getter, which implements log rotation logic
// and tries to recover from log file being moved away, perhaps
// by an external log rotation tool.
//
// The log file gets re-opened if it is currently closed or if
// reopening is explicitly requested (do_reopen==TRUE). In case
// of failure, returns NULL. 
//
// To avoid unpredictable behavior due to concurrency, 
// this function should be called while holding a lock.
static FILE * get_file (boolean do_reopen)
{
    // if max size is 0, there will be no logging except syslog, if configured
    if (log_max_size_bytes == 0)
        return NULL;

    // no log file has been set
    if (strlen (log_file_path) == 0)
        return LOGFH_DEFAULT;

    int fd = -1;
    if (LOGFH != NULL) { // apparently the stream is still open
        boolean file_changed = FALSE;
        if (! do_reopen && do_stat_log) { // we are not reopening for every write
            struct stat statbuf;
            int err = stat (log_file_path, &statbuf);
            if (err == -1) { // probably file does not exist, perhaps because it was renamed
                file_changed = TRUE;
            } else if (statbuf.st_size < 1) { // truncated externally, reopen
                file_changed = TRUE;
            } else if (log_ino != statbuf.st_ino) { // inode change, reopen just in case
                file_changed = TRUE;
            }
        }
        fd = fileno (LOGFH); // try to get the file descriptor
        if (file_changed || do_reopen || fd < 0) {
            fclose (LOGFH);
            LOGFH = NULL;
        }
    }
    
 retry:

    // open unless it is already is open
    if (LOGFH == NULL) {
        LOGFH = fopen (log_file_path, "a+");
        if (LOGFH == NULL) {
            return NULL;
        }
        fd = fileno (LOGFH);
        if (fd < 0) {
            fclose (LOGFH);
            LOGFH = NULL;
            return NULL;
        }
    }
    
    // see if it is time to rotate the log
    struct stat statbuf;
    int rc = fstat (fd, &statbuf);
    if (!rc) {
        log_ino = statbuf.st_ino; // record the inode number of the currently opened log

        if (((long)statbuf.st_size > log_max_size_bytes) && (log_roll_number > 0)) {
            char oldFile[EUCA_MAX_PATH], newFile[EUCA_MAX_PATH];
            for (int i=log_roll_number-1; i>0; i--) {
                snprintf(oldFile, EUCA_MAX_PATH, "%s.%d", log_file_path, i-1);
                snprintf(newFile, EUCA_MAX_PATH, "%s.%d", log_file_path, i);
                rename(oldFile, newFile);
            }
            snprintf(oldFile, EUCA_MAX_PATH, "%s", log_file_path);
            snprintf(newFile, EUCA_MAX_PATH, "%s.%d", log_file_path, 0);
            rename(oldFile, newFile);
            fclose (LOGFH);
            LOGFH = NULL;
            goto retry;
        }
    }

    return LOGFH;
}

// Log FILE pointer release. Should be called with a lock
// held in multi-threaded context.
static void release_file (void)
{
    if (do_close_fd && LOGFH != NULL) {
        fclose (LOGFH);
        LOGFH = NULL;
    }
}

// setter for logging parameters except file path
void log_params_set(int log_level_in, int log_roll_number_in, long log_max_size_bytes_in)
{
    // update the log level
    if (log_level_in >= EUCAALL && log_level_in <= EUCAOFF) {
        log_level = log_level_in;
    } else {
        log_level = DEFAULT_LOG_LEVEL;
    }

    // update the roll number limit
    if (log_roll_number_in >= 0 &&   // sanity check
        log_roll_number_in < 1000 && // sanity check
        log_roll_number != log_roll_number_in) {

        log_roll_number = log_roll_number_in;
    }
    
    // update the max size for any file
    if (log_max_size_bytes_in >= 0 &&
        log_max_size_bytes != log_max_size_bytes_in) {

        log_max_size_bytes = log_max_size_bytes_in;
        if (get_file(FALSE)) // that will rotate log files if needed
            release_file();
    }
}

// getter for logging parameters except file path
void log_params_get(int *log_level_out, int *log_roll_number_out, long *log_max_size_bytes_out)
{
    * log_level_out = log_level;
    * log_roll_number_out = log_roll_number;
    * log_max_size_bytes_out = log_max_size_bytes;
}

int log_file_set(const char * file)
{
    if (file==NULL) { // NULL means standard output
        log_file_path [0] = '\0';
        return 0;
    }

    if (strcmp (log_file_path, file) == 0) // hasn't changed
        return 0;

    strncpy (log_file_path, file, EUCA_MAX_PATH);
    if (get_file (TRUE) == NULL) {
        return 1;
    }
    release_file();
    return 0;
}

int log_prefix_set (const char * log_spec)
{
    if (log_spec==NULL || strlen (log_spec)==0) // TODO: eventually, enable empty prefix
        strncpy (log_custom_prefix, USE_STANDARD_PREFIX, sizeof (log_custom_prefix));
    else
        strncpy (log_custom_prefix, log_spec, sizeof (log_custom_prefix));
    return 0;
}

int log_facility_set (const char * facility, const char * component_name)
{
    int facility_int = -1;

    if (facility && strlen (facility) > 0) {
        boolean matched = FALSE;
        for (CODE * c = facilitynames; c->c_name != NULL; c++) {
            if (strcmp (c->c_name, facility) == 0) {
                facility_int = c->c_val;
                matched = TRUE;
                break;
            }
        }
        if (! matched) {
            logprintfl (EUCAERROR, "unrecognized log facility '%s' requested, ignoring\n", facility);
            return -1;
        }
    }

    if (facility_int != syslog_facility) {
        syslog_facility = facility_int;
        if (component_name)
            snprintf (log_name, sizeof (log_name) - 1, "euca-%s", component_name);
        closelog (); // in case it was open
        if (syslog_facility != -1) {
            logprintfl (EUCAINFO, "opening syslog '%s' in facility '%s'\n", log_name, facility);
            openlog (log_name, syslog_options, syslog_facility);
        }
    }

    return 0;
}

int log_sem_set (sem * s)
{
    if (s==NULL)
        return 1;
    
    if (log_sem!=NULL) {
        sem * old_log_sem = log_sem;
        sem_p (old_log_sem);
        if (log_sem != s) {
            log_sem = s;
        }
        sem_v (old_log_sem);
    } else {
        log_sem = s;
    }
    
    return 0;
}

int logfile(char *file, int log_level_in, int log_roll_number_in) // TODO: legacy function, to be removed when no longer in use
{
    log_params_set (log_level_in, log_roll_number_in, MAXLOGFILESIZE);
    return log_file_set (file);
}

// Print timestamp in YYYY-MM-DD HH:MM:SS format.
// Returns number of characters that it took up or
// 0 on error.
static int fill_timestamp (char * buf, int buf_size)
{
    time_t t = time (NULL);
    struct tm tm;
    localtime_r(&t, &tm);
    return strftime (buf, buf_size, "%F %T", &tm);
}

// This is the function that ultimately dumps a buffer into a log.
static int log_line (const char * line)
{
    int rc = 1;

    if (log_sem)
        sem_prolaag (log_sem, FALSE);
    
    FILE * file = get_file (FALSE);
    if (file != NULL) {
        fprintf(file, "%s", line);
        fflush(file);
        release_file();
        rc = 0;
    }
    
    if (log_sem)
        sem_verhogen (log_sem, FALSE);

    return rc;
}

// Log-printing function without a specific log level.
// It is essentially printf() that will go verbatim, 
// with just timestamp as prefix and at any log level, 
// into the current log or stdout, if no log was open.
int logprintf (const char *format, ...)
{
    char buf [LOGLINEBUF];

    // start with current timestamp
    int offset = fill_timestamp (buf, sizeof (buf));

    // append the log message passed via va_list
    va_list ap;
    va_start(ap, format);
    int rc = vsnprintf (buf + offset, sizeof (buf) - offset - 1, format, ap);
    va_end(ap);
    if (rc<0)
        return rc;
        
    return log_line (buf);
}

static int print_field_truncated (char ** log_spec, char * buf, int left, const char * field)
{
    boolean left_justify = FALSE;
    int in_field_len = strlen (field);
    int out_field_len = MAX_FIELD_LENGTH;
    if (in_field_len < out_field_len) {
        out_field_len = in_field_len; // unless specified, we'll use length of the field or max
    }

    // first, look ahead down s[] to see if we have length 
    // and alignment specified (leading '-' means left-justified)
    char * nstart = (* log_spec) + 1;
    if (* nstart == '-') { // a leading zero
        left_justify = TRUE;
        nstart++;
    }
    char * nend;
    int i = (int) strtoll (nstart, &nend, 10);
    if (nstart != nend) { // we have some digits
        * log_spec = nend - 1; // move the pointer ahead so caller will skip digits
        if (i > 1 && i <100) { // sanity check
            out_field_len = i;
        }
    }

    // create a format string that would truncate the field
    // to len and then print the field into 's'
    if (left < (out_field_len + 1)) { // not enough room left
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
    char format [10];
    snprintf (format, sizeof (format), "%%%s%ds", (left_justify) ? "-" : "", out_field_len);
    if (snprintf (buf, (out_field_len + 1), format, field + offset) < out_field_len)
        return -1; // error in snprintf
    
    return out_field_len;
}

// Main log-printing function, which will dump a line into
// a log, with a prefix appropriate for the log level, given
// that the log level is above the threshold.
int logprintfl (int level, const char *format, ...)
{
    // return if level is invalid or below the threshold
    if (level < log_level) {
        return 0;
    } else if (level < 0 || level > EUCAOFF) {
        return -1; // unexpected log level
    }

    char buf [LOGLINEBUF];
    int offset = 0;
    boolean custom_spec;
    char * prefix_spec;
    if (strcmp(log_custom_prefix, USE_STANDARD_PREFIX) == 0) {
        prefix_spec = log_level_prefix [log_level];
        custom_spec = FALSE;
    } else { 
        prefix_spec = log_custom_prefix;
        custom_spec = TRUE;
    }

    // go over prefix format for the log level (defined in log.h or custom)
    for ( ; // prefix_spec is initialized above
         * prefix_spec != '\0'; 
         prefix_spec++) {
        
        char * s = buf + offset;
        int left = sizeof (buf) - offset - 1;
        if (left < 1) {
            return -1; // not enough room in internal buffer for a prefix
        }

        // see if we have a formatting character or a regular one
        char c  = prefix_spec [0];
        char cn = prefix_spec [1];
        if (c != '%' // not a special formatting char
            || (c == '%' && cn == '%') // formatting char, escaped
            || (c == '%' && cn == '\0')) { // formatting char at the end
            s [0] = c;
            s [1] = '\0';
            offset++;
            if (c == '%' && cn == '%')
                prefix_spec++; // swallow the one extra '%' in input
            continue;
        }
        prefix_spec++; // move past the '%' to the formatting char

        int size = 0;
        switch (* prefix_spec) {
        case 'T': // timestamp
            size = fill_timestamp (s, left);
            break;

        case 'L': { // log-level
            char l [6];
            safe_strncpy (l, log_level_names [level], 6); // we want hard truncation
            size = snprintf (s, left, "%5s", l);
            break;
        }
        case 'p': { // process ID
            char p [11];
            snprintf (p, sizeof (p), "%010d", getpid()); // 10 chars is enough for max 32-bit unsigned integer
            size = print_field_truncated (&prefix_spec, s, left, p);
            break;
        }
        case 't': { // thread ID
            char t [21];
            snprintf (t, sizeof (t), "%020d", (pid_t) syscall (SYS_gettid)); // 20 chars is enough for max 64-bit unsigned integer
            size = print_field_truncated (&prefix_spec, s, left, t);
            break;
        }
        case 'm': // method
            size = print_field_truncated (&prefix_spec, s, left, _log_curr_method);
            break;

        case 'F': { // file-and-line
            char file_and_line [64];
            snprintf (file_and_line, sizeof (file_and_line), "%s:%d", _log_curr_file, _log_curr_line);
            size = print_field_truncated (&prefix_spec, s, left, file_and_line);
            break;
        }

        case 's': { // max RSS of the process
            struct rusage u;
            bzero (&u, sizeof (struct rusage));
            getrusage (RUSAGE_SELF, &u);
            
            // unfortunately, many fields in 'struct rusage' aren't supported on Linux (notably: ru_ixrss, ru_idrss, ru_isrss)
            char size_str [64];
            snprintf (size_str, sizeof (size_str), "%05ld", u.ru_maxrss/1024);
            size = print_field_truncated (&prefix_spec, s, left, size_str);
            break;
        }
        case '?':
            s [0] = '?'; // not supported currently
            s [1] = '\0';
            size = 1;
            break;

        default:
            s [0] = * prefix_spec;
            s [1] = '\0';
            size = 1;
        }
        
        if (size < 0) {
            logprintf ("error in prefix construction in logprintfl()\n");
            return -1; // something went wrong in the snprintf()s above
        }
        offset += size;
    }

    // add a space between the prefix and the message proper
    if (offset > 0 && ((sizeof (buf) - offset - 1) > 0)) {
        buf [offset++] = ' ';
        buf [offset] = '\0';
    }
    
    // append the log message passed via va_list
    va_list ap;
    va_start(ap, format);
    int rc = vsnprintf (buf + offset, sizeof (buf) - offset - 1, format, ap);
    va_end(ap);
    if (rc<0)
        return rc;

    if (syslog_facility != -1) {
        // log to syslog, at the appropriate level
        int l = LOG_DEBUG; // euca DEBUG, TRACE, and EXTREME use syslog's DEBUG
        if (level==EUCAERROR)     l = LOG_ERR;
        else if (level==EUCAWARN) l = LOG_WARNING;
        else if (level==EUCAINFO) l = LOG_INFO;
        if (custom_spec)
            syslog (l, buf);
        else 
            syslog (l, buf + offset);
    }

    return log_line (buf);
}

// prints contents of an arbitrary file (at file_path)
// using logprintfl, thus dumping its contents into a log
int logcat (int debug_level, const char * file_path)
{
	int got = 0;
	char buf [LOGLINEBUF];

	FILE *fp = fopen (file_path, "r");
	if (!fp) return got;
    while ( fgets (buf, LOGLINEBUF, fp) ) {
        int l = strlen (buf);
        if ( l<0 )
            break;
        if ( l+1<LOGLINEBUF && buf[l-1]!='\n' ) {
            buf [l++] = '\n';
            buf [l] = '\0';
        }
        logprintfl (debug_level, buf);
        got += l;
	}
    fclose (fp);
    
	return got;
}

// eventlog() was used for some timing measurements, almost exclusively from
// server-marshal.c, where SOAP requests are getting unmarshalled and mashalled.
// May be considered a legacy function, given no current need for the measurements.
void eventlog(char *hostTag, char *userTag, char *cid, char *eventTag, char *other)
{
  double ts;
  struct timeval tv;
  char hostTagFull[256];
  char hostName [256];
  FILE *PH;

  if (!timelog) return;

  hostTagFull[0] = '\0';
  PH = popen("hostname", "r");
  if(PH) {
      fscanf(PH, "%256s", hostName);
      pclose(PH);

      snprintf (hostTagFull, 256, "%s/%s", hostName, hostTag);

      gettimeofday(&tv, NULL);
      ts = (double)tv.tv_sec + ((double)tv.tv_usec / 1000000.0);

      logprintf("TIMELOG %s:%s:%s:%s:%f:%s\n", hostTagFull, userTag, cid, eventTag, ts, other);
  }
}

void log_dump_trace (char * buf, int buf_size)
{
    void *array[64];
    size_t size;
    char **strings;
    size_t i;
    
    size = backtrace (array, sizeof(array)/sizeof(void *));
    strings = backtrace_symbols (array, size);
    
    buf [0] = '\0';
    for (i = 0; i < size; i++) {
        int left = buf_size - 1 - strlen (buf);
        if (left < 0) break;
        char line [512];
        snprintf (line, sizeof(line), "\t%s\n", strings [i]);
        strncat (buf, line, left);
    }

    free (strings);
}
