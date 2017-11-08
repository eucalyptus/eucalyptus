// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

//!
//! @file util/euca_system.c
//! Implements the API available for system interaction
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64           // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU
#include <string.h>                    // strlen, strcpy
#include <ctype.h>                     // isspace
#include <assert.h>
#include <stdarg.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/vfs.h>
#include <unistd.h>
#include <time.h>
#include <math.h>                      // powf
#include <fcntl.h>                     // open
#include <utime.h>                     // utime
#include <sys/wait.h>
#include <pwd.h>
#include <dirent.h>                    // opendir, etc
#include <sys/errno.h>                 // errno
#include <sys/time.h>                  // gettimeofday
#include <limits.h>
#include <sys/mman.h>                  // mmap
#include <pthread.h>
#include <sys/select.h>                // pselect

#include "eucalyptus.h"

#include "misc.h"
#include "euca_auth.h"
#include "log.h"
#include "euca_string.h"
#include "euca_system.h"
#include "ipc.h"

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
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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
//! Checks whether or not a process identified by the given PID and optional command line
//! is actively running on this system.
//!
//! @param[in] pid the Process Identifier (PID)
//! @param[in] psSearch an optional string pointer to the string to search in the process' command line string
//!
//! @return TRUE if the process if found and running or FALSE otherwise
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
boolean euca_is_running(pid_t pid, const char *psSearch)
{
#define BUFFER_LEN         1024

    int rc = 0;
    char *pChar = NULL;
    FILE *pFh = NULL;
    static char sFile[EUCA_MAX_PATH] = "";
    static char sBuffer[BUFFER_LEN] = "";

    // Build the path to the process' cmdline file and check if it is present
    snprintf(sFile, EUCA_MAX_PATH, "/proc/%d/cmdline", pid);
    if ((rc = check_file(sFile)) == 0) {
        if (psSearch) {
            // check if cmdline contains 'psSearch'. The cmdline proc file contains the list of
            // arguments given on the command line. Each arguments are separated with a NULL '\0'
            // character.
            if ((pFh = fopen(sFile, "r")) != NULL) {
                sBuffer[0] = '\0';
                while (fgets(sBuffer, BUFFER_LEN, pFh)) {
                    while ((pChar = memchr(sBuffer, '\0', 1024))) {
                        (*pChar) = 'X';
                    }

                    // safety to ensure strstr() does not go beyond the buffer's end
                    sBuffer[BUFFER_LEN - 1] = '\0';
                    if (strstr(sBuffer, psSearch)) {
                        fclose(pFh);
                        return (TRUE);
                    }
                }
                fclose(pFh);
            }
        } else {
            return (TRUE);
        }
    }

    return (FALSE);

#undef BUFFER_LEN
}
