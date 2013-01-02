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
//! @file util/euca_string.c
//! Implementation of various string utilities.
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64    // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <string.h>             // strlen, strcpy
#include <ctype.h>              // isspace
#include <assert.h>
#include <stdarg.h>
#include <unistd.h>
#include <sys/errno.h>          // errno
#include <limits.h>
#include <wchar.h>

#include "eucalyptus.h"
#include "misc.h"
#include "euca_string.h"

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
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

char *euca_strreplace(char **haystack, char *source, char *value);
int euca_lscanf(const char *haystack, const char *format, void *value);
char *euca_strestr(const char *haystack, const char *begin, const char *end);
long long euca_strtoll(const char *string, const char *begin, const char *end);
char *euca_strduptolower(const char *restrict string);
char *euca_strdupcat(char *restrict s1, const char *restrict s2);
char *euca_strncpy(char *restrict to, const char *restrict from, size_t size);

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
//! Given string haystack, replace occurences of source with the given value
//! and return the new string in haystack
//!
//! @param[in,out] haystack pointer to the original string to search from
//! @param[in]     search the string we are looking to replace
//! @param[in]     value the replacement value
//!
//! @return the newly formatted string
//!
//! @pre \li The haystack, source and value pointers must not be NULL.
//!      \li The source string must not be 0 length.
//!
//! @post On success the current string pointed by haystack is freed and replaced
//!       by the new string.
//!
//! @note caller is reponsible to free the result
//!
char *euca_strreplace(char **haystack, char *search, char *value)
{
#define MAX_BUFFER_SIZE       (32768 * 2)

    char *buf = NULL;
    char *start = NULL;
    char *substart = NULL;
    char *tok = NULL;
    char *new_string = NULL;

    // Make sure all our parameters are valid
    if ((haystack == NULL) || (*haystack == NULL) || (search == NULL) || (search[0] == '\0') || (value == NULL))
        return (NULL);

    // Allocate some memory for processing
    if ((buf = EUCA_ALLOC(MAX_BUFFER_SIZE, sizeof(char))) == NULL)
        return (NULL);

    // Allocate memory for the result
    if ((new_string = EUCA_ZALLOC(MAX_BUFFER_SIZE, sizeof(char))) == NULL) {
        EUCA_FREE(buf);
        return (NULL);
    }
    // Scan our haystack for the string to replace
    start = *haystack;
    substart = start;
    tok = strstr(start, search);
    while (tok != NULL) {
        *tok = '\0';

        snprintf(buf, MAX_BUFFER_SIZE, "%s%s%s", new_string, substart, value);
        strncpy(new_string, buf, MAX_BUFFER_SIZE);

        tok += strlen(search);
        substart = tok;
        tok = strstr(substart, search);
    }

    // Build the final result
    snprintf(buf, MAX_BUFFER_SIZE, "%s%s", new_string, substart);
    strncpy(new_string, buf, MAX_BUFFER_SIZE);

    // Free our memory
    EUCA_FREE(buf);
    EUCA_FREE(*haystack);

    // Set the return value and we're done.
    *haystack = new_string;
    return (new_string);

#undef MAX_BUFFER_SIZE
}

//!
//! Do sscanf() on each line in lines[], returning upon first match
//! returns TRUE if there was match and FALSE otherwise
//!
//! @param[in]  haystack The text for which we search for the format
//! @param[in]  format The format string printf style
//! @param[out] value the variable to set in return.
//!
//! @return 1 if we have a match, 0 otherwise
//!
//! @pre The haystack, format and value fields must not be NULL
//!
//! @post If we have a matching format, the value field is being updated
//!
int euca_lscanf(const char *haystack, const char *format, void *value)
{
    int found = 0;
    char *copy = NULL;
    char *start = NULL;
    char *end = NULL;
    boolean newline = FALSE;

    // Make sure our given parameters are valid
    if (!haystack || !format || !value)
        return (0);

    // Duplicate the haystack so we can do some modifications as we go
    if ((copy = strdup(haystack)) == NULL)
        return (0);

    // Scan each lines
    for (start = copy, found = 0; (start && (*start != '\0') && !found); start = end + 1) {
        newline = FALSE;

        // Scan from start to find a '\n' or '\0'
        for (end = start + 1; ((*end != '\n') && (*end != '\0')); end++) ;

        // If we have a new line characters, replace with a NULL-Termination
        if (*end == '\n') {
            *end = '\0';
            newline = TRUE;
        }
        // Scan this substring for our format
        if (sscanf(start, format, value) == 1) {
            // Got it!!!
            found = 1;
        } else if (!newline) {
            // so that start == '\0'
            end--;
        }
    }

    // Make sure we free our allocated copy before we return.
    EUCA_FREE(copy);
    return (found);
}

//!
//! Extract string from str bound by 'begin' and 'end'
//!
//! @param[in] haystack the string containing the begining and ending values
//! @param[in] begin the begining string marker
//! @param[in] end the ending string marker
//!
//! @return the newly constructed string or NULL if any error occured.
//!
//! @pre \li The haystack, begin and end fields must not be NULL.
//!      \li The haystack field must be at least 3 characters long.
//!      \li The begin and end fields must be at least 1 character long.
//!      \li The begin and end field must be found withing the haystack field.
//!      \li The begin field must be located before the end field.
//!
//! @post A new string is allocated containing all characters between the begin
//!       and end needles and NULL-Terminated.
//!
//! @note Caller is responsible to free the returned memory
//!
char *euca_strestr(const char *haystack, const char *begin, const char *end)
{
    int len = 0;
    char *b = NULL;
    char *e = NULL;
    char *buf = NULL;

    // Make sure our parameters are valid
    if (!haystack || !begin || !end || (strlen(haystack) < 3) || (strlen(begin) < 1) || (strlen(end) < 1)) {
        return (NULL);
    }
    // Find the begining needle
    if ((b = strstr(haystack, begin)) == NULL) {
        return (NULL);
    }
    // Find the ending needle
    if ((e = strstr(haystack, end)) == NULL) {
        return (NULL);
    }
    // Move 'b' at the end of the begining needle (we don't want
    // begin in resulting string).
    b += strlen(begin);

    // Make sure begin is located before end.
    if ((len = e - b) < 0) {
        return (NULL);
    }
    // Allocate memory for our resulting string
    if ((buf = EUCA_ALLOC((len + 1), sizeof(char))) != NULL) {
        strncpy(buf, b, len);
        buf[len] = '\0';
    }

    return (buf);
}

//!
//! Extract a long integer from str bound by 'begin' and 'end'
//!
//! @param[in] string the string containing the long integer
//! @param[in] begin pointer to the begining of the long integer in 'str'
//! @param[in] end pointer to the end of the long integer in 'str'
//!
//! @return the integer value from the string and its parameters
//!
//! @pre \li The string, begin and end fields must not be NULL.
//!      \li The string field must be at least 3 characters long.
//!      \li The begin and end fields must be at least 1 character long.
//!      \li The begin and end field must be found withing the string field.
//!      \li The begin field must be located before the end field.
//!
long long euca_strtoll(const char *string, const char *begin, const char *end)
{
    char *buf = NULL;
    long long val = -1L;

    // Retrieve the long integer value from the string and convert
    if ((buf = euca_strestr(string, begin, end)) != NULL) {
        val = atoll(buf);
        EUCA_FREE(buf);
    }

    return (val);
}

//!
//! Duplicate and convert a string to lower characters
//!
//! @param[in] string the string to duplicate and convert to lower case
//!
//! @return a pointer to the lower care result
//!
//! @pre The string field must not be NULL.
//!
//! @post On success, str will be duplicated and each character of str will be converted to lower case.
//!
//! @note result must be freed by the caller
//!
char *euca_strduptolower(const char *restrict string)
{
    char *lc = NULL;
    register size_t i = 0;
    register size_t len = 0;

    // Was str provided?
    if (string) {
        if ((lc = strdup(string)) != NULL) {
            len = strlen(lc);
            for (i = 0; i < len; i++) {
                lc[i] = tolower(lc[i]);
            }
        }
    }

    return (lc);
}

//!
//! Returns a new string in which 's2' is appended to 's1' and frees 's1'. A NULL
//! character is appended to the resulting string.
//!
//! @param[in] s1 the optional string to append to and free
//! @param[in] s2 the optional string to append
//!
//! @return a pointer to the newly allocated string or NULL if any error occured.
//!
//! @pre Both s1 and s2 fields should be provided.
//!
//! @post If s1 was provided, the string will be freed uppon completion of the operation.
//!
char *euca_strdupcat(char *restrict s1, const char *restrict s2)
{
    int len = 0;
    int s1len = 0;
    char *ret = NULL;

    // Was s1 provided?
    if (s1) {
        s1len = strlen(s1);
        len += s1len;
    }
    // Was s2 provided?
    if (s2) {
        len += strlen(s2);
    }
    // Allocate the memory we need plus the NULL terminating character and concatenate
    // the two strings
    if ((ret = EUCA_ZALLOC((len + 1), sizeof(char))) != NULL) {
        if (s1) {
            strncat(ret, s1, len);
            EUCA_FREE(s1);
        }

        if (s2) {
            strncat(ret, s2, (len - s1len));
        }
    }

    return (ret);
}

//!
//! Copy 'from' into 'to' for up to 'size' - 1 characters and ensure the string is aways
//! NULL-terminated.
//!
//! @param[in] to the string to copy into
//! @param[in] from the string to copy
//! @param[in] size the maximum number of characters to copy
//!
//! @return A string pointer to the resulting string.
//!
//! @pre \li The to field must not be NULL
//!      \li The to and from fields must never points to the same address (restrict)
//!
//! @post Up to size - 1 characters will be copied from 'from' to 'to' and a NULL character
//!       will be set at the end of the resulting string.
//!
//! @note If from is longer or equals to size, size - 1 caracters will be copied
//!       into the resulting string to ensure a NULL-Terminated string. If the given
//!       size field is 0 or if 'from' is NULL, the function will set the first
//!       character of 'to' to '\0' and return a pointer to 'to'.
//!
char *euca_strncpy(char *restrict to, const char *restrict from, size_t size)
{
    char *ret = NULL;

    // Make sure we have a string to write to
    if (to != NULL) {
        // If 'from' is NULL, return 'to' and make sure its 0-length
        if ((from == NULL) || (size == 0)) {
            (*to) = '\0';
            return (to);
        }
        // Copy from into to
        ret = strncpy(to, from, size);

        // Make sure we're NULL-Terminated.
        ret[size - 1] = '\0';
    }
    return (ret);
}
