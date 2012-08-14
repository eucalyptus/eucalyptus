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

#include "eucalyptus.h"
#include "log.h"

#define BUFSIZE 1024

static int timelog=0; /* change to 1 for TIMELOG entries */
static int logging=0;
static FILE *LOGFH=NULL;
static char logFile[EUCA_MAX_PATH];

#define DEFAULT_LOG_LEVEL EUCADEBUG
static int log_level=DEFAULT_LOG_LEVEL;
static int log_roll_number=4;
static long log_max_size_bytes=MAXLOGFILESIZE;

int log_level_int (const char * level)
{
    for (int l=0; l<=EUCAOFF; l++) {
        if (!strcmp (level, log_level_names[l])) {
            return l;
        }
    }
    return -1;
}

// log getter, which implements log rotation logic
FILE * get_file (void)
{
    FILE *file = LOGFH;
    int fd = fileno(file);
    if (fd < 1)
        return file;

    struct stat statbuf;
    int rc = fstat(fd, &statbuf);
    if (!rc && ((int)statbuf.st_size > log_max_size_bytes)) {
        int i;
        char oldFile[EUCA_MAX_PATH], newFile[EUCA_MAX_PATH];

        rc = stat(logFile, &statbuf);
        if (!rc && ((int)statbuf.st_size > log_max_size_bytes)) {
            for (i=log_roll_number-1; i>=0; i--) {
                snprintf(oldFile, EUCA_MAX_PATH, "%s.%d", logFile, i);
                snprintf(newFile, EUCA_MAX_PATH, "%s.%d", logFile, i+1);
                rename(oldFile, newFile);
            }
            snprintf(oldFile, EUCA_MAX_PATH, "%s", logFile);
            snprintf(newFile, EUCA_MAX_PATH, "%s.%d", logFile, 0);
            rename(oldFile, newFile);
        }
        fclose(LOGFH);
        LOGFH = fopen(logFile, "a");
        if (LOGFH) {
            file = LOGFH;
        } else {
            file = stdout;
        }
    }

    return file;
}

// setter for logging parameters except file path
void log_params_set(int log_level_in, int log_roll_number_in, long log_max_size_bytes_in)
{
    if (log_level_in >= EUCAALL && log_level_in <= EUCAOFF) {
        log_level = log_level_in;
    } else {
        log_level = DEFAULT_LOG_LEVEL;
    }

    if (log_roll_number_in > 0 &&
        log_roll_number_in < 100 &&
        log_roll_number != log_roll_number_in) {

        log_roll_number = log_roll_number_in;
    }

    if (log_max_size_bytes_in > 0 &&
        log_max_size_bytes != log_max_size_bytes_in) {

        log_max_size_bytes = log_max_size_bytes_in;
        get_file(); // that will rotate log files if needed
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
    logging = 0;

    if (LOGFH != NULL) {
        fclose(LOGFH);
    }

    if (file == NULL) {
        LOGFH = NULL;
    } else {
        snprintf(logFile, EUCA_MAX_PATH, "%s", file);
        LOGFH = fopen(file, "a");
        if (LOGFH) {
            logging=1;
        }
    }

    return (1-logging);
}

int logfile(char *file, int log_level_in, int log_roll_number_in) // TODO: legacy function, to be removed when no longer in use
{
    log_params_set (log_level_in, log_roll_number_in, 0);
    return log_file_set (file);
}

// print timestamp in YYYY-MM-DD HH:MM:SS format
static void print_timestamp (FILE * file)
{
    time_t t = time (NULL);
    struct tm tm;
    gmtime_r(&t, &tm);
    char buf[27];
    if (strftime (buf, sizeof(buf), "%F %T", &tm)) {
        fprintf(file, "%s ", buf);
    }
}

int logprintf(const char *format, ...)
{
    va_list ap;
    int rc;
    char buf[27], *eol;
    time_t t;
    FILE *file;

    rc = 1;
    va_start(ap, format);

    if (logging) {
        file = LOGFH;
    } else {
        file = stdout;
    }

    print_timestamp (file);
    rc = vfprintf(file, format, ap);
    fflush(file);

    va_end(ap);
    return(rc);
}

int logprintfl(int level, const char *format, ...)
{
    va_list ap;
    int rc, fd;
    FILE *file;

    if (level < log_level) {
        return (0);
    }

    rc = 1;
    va_start(ap, format);

    if (logging) {
        file = get_file();
    } else {
        file = stderr;
    }

    print_timestamp (file);

    // log level, a 5-char field, indented to the right
    if (level == EUCATRACE)       { fprintf (file, "%s", "TRACE");}
    else if (level == EUCADEBUG3) { fprintf (file, "%s", "DBUG3");}
    else if (level == EUCADEBUG2) { fprintf (file, "%s", "DBUG2");}
    else if (level == EUCADEBUG)  { fprintf (file, "%s", "DEBUG");}
    else if (level == EUCAINFO)   { fprintf (file, "%s", " INFO");}
    else if (level == EUCAWARN)   { fprintf (file, "%s", " WARN");}
    else if (level == EUCAERROR)  { fprintf (file, "%s", "ERROR");}
    else if (level == EUCAFATAL)  { fprintf (file, "%s", "FATAL");}
    else                          { fprintf (file, "%s", "?????");}

    // the PID and thread ID
    fprintf (file, " %06d:%06d", getpid(), (pid_t) syscall (SYS_gettid));

    // last thing - the separator from free-form part of the log message
    fprintf (file, " | ");

    rc = vfprintf(file, format, ap);
    fflush(file);

    va_end(ap);
    return(rc);
}

/* prints contents of a file with logprintf */
int logcat (int debug_level, const char * file_name)
{
	int got = 0;
	char buf [BUFSIZE];

	FILE *fp = fopen (file_name, "r");
	if (!fp) return got;
    while ( fgets (buf, BUFSIZE, fp) ) {
        int l = strlen (buf);
        if ( l<0 )
            break;
        if ( l+1<BUFSIZE && buf[l-1]!='\n' ) {
            buf [l++] = '\n';
            buf [l] = '\0';
        }
        logprintfl (debug_level, buf);
        got += l;
	}
    fclose (fp);
	return got;
}

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
