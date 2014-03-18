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

#define _FILE_OFFSET_BITS 64           // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <string.h>                    // strlen, strcpy
#include <ctype.h>                     // isspace
#include <assert.h>
#include <stdarg.h>
#include <unistd.h>
#include <sys/errno.h>                 // errno
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
//! Given string \p haystack, replace occurences of \p search with the given \p value
//! and return the new string in \p haystack
//!
//! @param[in,out] haystack pointer to the original string to search from
//! @param[in]     search the string we are looking to replace
//! @param[in]     value the replacement value
//!
//! @return the newly formatted string
//!
//! @pre \li The \p haystack, \p search and \p value pointers must not be NULL.
//!      \li The \p search string must not be 0 length.
//!
//! @post On success the current string pointed by \p haystack is freed and replaced
//!       by the new string.
//!
//! @note caller is reponsible to free the result
//!
char *euca_strreplace(char **haystack, char *search, char *value)
{
#define MAX_BUFFER_SIZE       (32768 * 2)

    char *sBuffer = NULL;
    char *pStart = NULL;
    char *pSubStart = NULL;
    char *sToken = NULL;
    char *sNewString = NULL;

    // Make sure all our parameters are valid
    if ((haystack == NULL) || (*haystack == NULL) || (search == NULL) || (search[0] == '\0') || (value == NULL))
        return (NULL);

    // Allocate some memory for processing
    if ((sBuffer = EUCA_ALLOC(MAX_BUFFER_SIZE, sizeof(char))) == NULL)
        return (NULL);

    // Allocate memory for the result
    if ((sNewString = EUCA_ZALLOC(MAX_BUFFER_SIZE, sizeof(char))) == NULL) {
        EUCA_FREE(sBuffer);
        return (NULL);
    }
    // Scan our haystack for the string to replace
    pStart = *haystack;
    pSubStart = pStart;
    sToken = strstr(pStart, search);
    while (sToken != NULL) {
        *sToken = '\0';

        snprintf(sBuffer, MAX_BUFFER_SIZE, "%s%s%s", sNewString, pSubStart, value);
        strncpy(sNewString, sBuffer, MAX_BUFFER_SIZE);

        sToken += strlen(search);
        pSubStart = sToken;
        sToken = strstr(pSubStart, search);
    }

    // Build the final result
    snprintf(sBuffer, MAX_BUFFER_SIZE, "%s%s", sNewString, pSubStart);
    strncpy(sNewString, sBuffer, MAX_BUFFER_SIZE);

    // Free our memory
    EUCA_FREE(sBuffer);
    EUCA_FREE(*haystack);

    // Set the return value and we're done.
    *haystack = sNewString;
    return (sNewString);

#undef MAX_BUFFER_SIZE
}

//!
//! Do sscanf() on each line in \p haystack, returning upon first match
//! returns TRUE if there was match and FALSE otherwise
//!
//! @param[in]  haystack The text for which we search for the format
//! @param[in]  format The format string printf style
//! @param[out] value the variable to set in return.
//!
//! @return TRUE if we have a match, FALSE otherwise
//!
//! @pre The \p haystack, \p format and \p value fields must not be NULL
//!
//! @post If we have a matching \p format, the \p value field is being updated
//!
boolean euca_lscanf(const char *haystack, const char *format, void *value)
{
    char *sCopy = NULL;
    char *pStart = NULL;
    char *pEnd = NULL;
    boolean found = FALSE;
    boolean newline = FALSE;

    // Make sure our given parameters are valid
    if (!haystack || !format || !value)
        return (0);

    // Duplicate the haystack so we can do some modifications as we go
    if ((sCopy = strdup(haystack)) == NULL)
        return (0);

    // Scan each lines
    for (pStart = sCopy, found = FALSE; (pStart && (*pStart != '\0') && !found); pStart = pEnd + 1) {
        newline = FALSE;

        // Scan from start to find a '\n' or '\0'
        for (pEnd = pStart + 1; ((*pEnd != '\n') && (*pEnd != '\0')); pEnd++) ;

        // If we have a new line characters, replace with a NULL-Termination
        if (*pEnd == '\n') {
            *pEnd = '\0';
            newline = TRUE;
        }
        // Scan this substring for our format
        if (sscanf(pStart, format, value) == 1) {
            // Got it!!!
            found = TRUE;
        } else if (!newline) {
            // so that start == '\0'
            pEnd--;
        }
    }

    // Make sure we free our allocated copy before we return.
    EUCA_FREE(sCopy);
    return (found);
}

//!
//! Extract string from \p haystack bound by \p begin and \p end
//!
//! @param[in] haystack the string containing the begining and ending values
//! @param[in] begin the begining string marker
//! @param[in] end the ending string marker
//!
//! @return the newly constructed string or NULL if any error occured.
//!
//! @pre \li The \p haystack, \p begin and \p end fields must not be NULL.
//!      \li The \p haystack field must be at least 3 characters long.
//!      \li The \p begin and \p end fields must be at least 1 character long.
//!      \li The \p begin and \p end field must be found withing the \p haystack field.
//!      \li The \p begin field must be located before the \p end field.
//!
//! @post A new string is allocated containing all characters between the \p begin
//!       and \p end needles and NULL-Terminated.
//!
//! @note Caller is responsible to free the returned memory
//!
char *euca_strestr(const char *haystack, const char *begin, const char *end)
{
    char *pB = NULL;
    char *pE = NULL;
    char *sBuffer = NULL;
    ssize_t len = 0;

    // Make sure our parameters are valid
    if (!haystack || !begin || !end || (strlen(haystack) < 3) || (strlen(begin) < 1) || (strlen(end) < 1)) {
        return (NULL);
    }
    // Find the begining needle
    if ((pB = strstr(haystack, begin)) == NULL) {
        return (NULL);
    }
    // Find the ending needle
    if ((pE = strstr(haystack, end)) == NULL) {
        return (NULL);
    }
    // Move 'b' at the end of the begining needle (we don't want
    // begin in resulting string).
    pB += strlen(begin);

    // Make sure begin is located before end.
    if ((len = (pE - pB)) < 0) {
        return (NULL);
    }
    // Allocate memory for our resulting string
    if ((sBuffer = EUCA_ALLOC((len + 1), sizeof(char))) != NULL) {
        strncpy(sBuffer, pB, len);
        sBuffer[len] = '\0';
    }

    return (sBuffer);
}

//!
//! Extract a long integer from \p string bound by \p begin and \p end
//!
//! @param[in] string the string containing the long integer
//! @param[in] begin pointer to the begining of the long integer in \p string
//! @param[in] end pointer to the end of the long integer in \p string
//!
//! @return the integer value from the string and its parameters
//!
//! @pre \li The \p string, \p begin and \p end fields must not be NULL.
//!      \li The \p string field must be at least 3 characters long.
//!      \li The \p begin and \p end fields must be at least 1 character long.
//!      \li The \p begin and \p end field must be found withing the \p string field.
//!      \li The \p begin field must be located before the \p end field.
//!
long long euca_strtoll(const char *string, const char *begin, const char *end)
{
    char *sBuffer = NULL;
    long long val = -1L;

    // Retrieve the long integer value from the string and convert
    if ((sBuffer = euca_strestr(string, begin, end)) != NULL) {
        val = atoll(sBuffer);
        EUCA_FREE(sBuffer);
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
//! @pre The \p string field must not be NULL.
//!
//! @post On success, \p string will be duplicated and each character of \p string will be converted to lower case.
//!
//! @note result must be freed by the caller
//!
char *euca_strduptolower(const char *restrict string)
{
    char *sLower = NULL;
    register size_t i = 0;
    register size_t len = 0;

    // Was str provided?
    if (string) {
        if ((sLower = strdup(string)) != NULL) {
            len = strlen(sLower);
            for (i = 0; i < len; i++) {
                sLower[i] = tolower(sLower[i]);
            }
        }
    }

    return (sLower);
}

//!
//! Works in exactly the same way strdup() does but we allocate the memory and sanitize the string. This
//! is mainly when we want to duplicate untrusted strings
//!
//! @param[in] s1 the string to be duplicated and sanitized
//!
//! @return a pointer to the newly allocated string or NULL if any error occured
//!
//! @pre \p s1 fields must be provided
//!
//! @post a new string is allocated and sanitized on success
//!
char *euca_strdup(const char *s1)
{
    char *sRet = NULL;
    size_t len = 0;

    // Validate s1
    if (s1) {
        // make sure we allocate at least 1 character for empty strings
        len = (((len = strlen(s1)) > 0) ? len : 1);

        // Allocate the memory
        if ((sRet = EUCA_ALLOC(len, sizeof(char))) != NULL) {
            // now copy s1 in sRet using sprintf()
            sprintf(sRet, "%s", s1);
        }
    }

    return (sRet);
}

//!
//! Returns a new string in which \p s2 is appended to \p s1 and frees \p s1. A NULL
//! character is appended to the resulting string.
//!
//! @param[in] s1 the optional string to append to and free
//! @param[in] s2 the optional string to append
//!
//! @return a pointer to the newly allocated string or NULL if any error occured.
//!
//! @pre Both \p s1 and \p s2 fields should be provided.
//!
//! @post If \p s1 was provided, the string will be freed uppon completion of the operation.
//!
char *euca_strdupcat(char *restrict s1, const char *restrict s2)
{
    int s1len = 0;
    char *sRet = NULL;
    size_t len = 0;

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
    if ((sRet = EUCA_ZALLOC((len + 1), sizeof(char))) != NULL) {
        if (s1) {
            strncat(sRet, s1, len);
            EUCA_FREE(s1);
        }

        if (s2) {
            strncat(sRet, s2, (len - s1len));
        }
    }

    return (sRet);
}

//!
//! Appends \p src to \p dest and makes sure \p dest isn't going to be greater than \p size Characters. If \p dest
//! reaches \p size characters, a NULL character will be added for the n-th character
//!
//! @param[in] dest the optional string to append to and free
//! @param[in] src the optional string to append
//! @param[in] size the number of character not to exceed in S1
//!
//! @return a pointer to the destination string
//!
//! @pre Both \p dest and \p src fields should be provided.
//!
//! @post \p dest will contain all or most character of \p src at the end of \p dest
//!
char *euca_strncat(char *restrict dest, const char *restrict src, size_t size)
{
    char *sTo = NULL;
    size_t len = 0;

    // Was dest provided?
    if (dest) {
        len = strlen(dest);
        sTo = dest + len;
        euca_strncpy(sTo, src, (size - len));
    }

    return (dest);
}

//!
//! Copy \p from into \p to for up to \p size - 1 characters and ensure the string is aways
//! NULL-terminated.
//!
//! @param[in] to the string to copy into
//! @param[in] from the string to copy
//! @param[in] size the maximum number of characters to copy
//!
//! @return A string pointer to the resulting string.
//!
//! @pre \li The \p to field must not be NULL
//!      \li The \p to and \p from fields must never points to the same address (restrict)
//!
//! @post Up to \p size - 1 characters will be copied from \p from to \p to and a NULL character
//!       will be set at the end of the resulting string.
//!
//! @note If \p from is longer or equals to \p size, \p size - 1 caracters will be copied
//!       into the resulting string to ensure a NULL-Terminated string. If the given
//!       \p size field is 0 or if \p from is NULL, the function will set the first
//!       character of \p to to null and return a pointer to \p to.
//!
char *euca_strncpy(char *restrict to, const char *restrict from, size_t size)
{
    char *sRet = NULL;

    // Make sure we have a string to write to
    if (to != NULL) {
        // If 'from' is NULL, return 'to' and make sure its 0-length
        if ((from == NULL) || (size == 0)) {
            (*to) = '\0';
            return (to);
        }
        // Copy from into to
        sRet = strncpy(to, from, size);

        // Make sure we're NULL-Terminated.
        sRet[size - 1] = '\0';
    }
    return (sRet);
}

//!
//! Converts a human readable IP address to its binary counter part
//!
//! @param[in] psDot the human readable IP address to convert
//!
//! @return The binary representation of an IP address. If \p psDotIn is a NULL pointer or if any corresponding
//!         octets are invalid, then a binary IP value corresponding to "127.0.0.1" will be returned.
//!
//! @pre The \p psDotIn field must not be NULL and must be a valid IP address value in dot notation.
//!
u32 euca_dot2hex(char *psDot)
{
    int a = 127;
    int b = 0;
    int c = 0;
    int d = 1;
    int rc = 0;

    if (psDot != NULL) {
        rc = sscanf(psDot, "%d.%d.%d.%d", &a, &b, &c, &d);
        if ((rc != 4) || ((a < 0) || (a > 255)) || ((b < 0) || (b > 255)) || ((c < 0) || (c > 255)) || ((d < 0) || (d > 255))) {
            a = 127;
            b = 0;
            c = 0;
            d = 1;
        }
    }

    a = a << 24;
    b = b << 16;
    c = c << 8;

    return (a | b | c | d);
}

//!
//! Convert a binary IP address value to a human readable value.
//!
//! @param[in] hex the IP address value
//!
//! @return A human readable representation of the binary IP address value or NULL if we ran out of memory.
//!
//! @note The caller is responsible to free the allocated memory for the returned value
//!
char *euca_hex2dot(u32 hex)
{
    char sDot[16] = "";

    snprintf(sDot, 16, "%u.%u.%u.%u", ((hex & 0xFF000000) >> 24), ((hex & 0x00FF0000) >> 16), ((hex & 0x0000FF00) >> 8), (hex & 0x000000FF));
    return (strdup(sDot));
}

//!
//! Converts a human readable MAC address value to its corresponding binary array value
//!
//! @param[in]  psMacIn the human readable MAC address value
//! @param[out] aHexOut the output array that will contain the correct hexadecimal values.
//!
//! @return A pointer to the 'aHexOut' parameter if successful or NULL on failure. A failure occurs if
//!         the 'psMacIn' parameter is NULL or if the format of the readable MAC address under 'psMacIn'
//!         is invalid.
//!
//! @pre \p in must be a non NULL pointer and must be a valid MAC address format.
//!
//! @post On success, the bytes are extracted from 'psMacIn' and put into 'aHexOut'. On failure, the 'aHexOut' value
//!       remains unchanged.
//!
//! @note if \p psMacIn is NULL or if its of an invalid format, \p aHexOut will remain unchanged
//!
u8 *euca_mac2hex(char *psMacIn, u8 aHexOut[6])
{
    int rc = 0;
    u32 aTmp[6] = { 0 };

    if (psMacIn != NULL) {
        rc = sscanf(psMacIn, "%X:%X:%X:%X:%X:%X", ((u32 *) & aTmp[0]), ((u32 *) & aTmp[1]), ((u32 *) & aTmp[2]), ((u32 *) & aTmp[3]), ((u32 *) & aTmp[4]), ((u32 *) & aTmp[5]));
        if (rc == 6) {
            aHexOut[0] = ((u8) aTmp[0]);
            aHexOut[1] = ((u8) aTmp[1]);
            aHexOut[2] = ((u8) aTmp[2]);
            aHexOut[3] = ((u8) aTmp[3]);
            aHexOut[4] = ((u8) aTmp[4]);
            aHexOut[5] = ((u8) aTmp[5]);
            return (aHexOut);
        }
    }
    return (NULL);
}

//!
//! Convert a binary MAC address value to human readable.
//!
//! @param[in]  aHexIn the array of hexadecimal value making the MAC address.
//! @param[out] ppsMacOut the returned readable value corresponding to \p in
//!
//! @pre \p ppsMacOut must not be NULL.
//!
//! @note The \p (*ppsMacOut) field should be NULL.
//!
void euca_hex2mac(u8 aHexIn[6], char **ppsMacOut)
{
    if (ppsMacOut != NULL) {
        if ((*ppsMacOut = EUCA_ALLOC(24, sizeof(char))) != NULL) {
            snprintf(*ppsMacOut, 24, "%02X:%02X:%02X:%02X:%02X:%02X", aHexIn[0], aHexIn[1], aHexIn[2], aHexIn[3], aHexIn[4], aHexIn[5]);
        }
    }
}

//!
//! Compares a given binary MAC address value to an ALL 0s MAC address
//!
//! @param[in] aMac the array containing the 6 MAC address byte values.
//!
//! @return 0 if the given MAC address is ALL 0s or any other values if its not ALL 0s.
//!
int euca_maczero(u8 aMac[6])
{
    u8 aZeroMac[6] = { 0 };
    bzero(aZeroMac, (6 * sizeof(u8)));
    return (memcmp(aMac, aZeroMac, (6 * sizeof(u8))));
}

//!
//! Compares a binary MAC address value with a human readable MAC address value.
//!
//! @param[in] psMac a human readable MAC address value to compare
//! @param[in] aMac a binary MAC address value to compare with
//!
//! @return an integer less than, equal to, or greater than zero if the first n bytes of \p psMac
//!         is found, respectively, to be less than, to match, or be greater than the first n
//!         bytes of the converted aMac value.
//!
//! @see mac2hex();
//!
//! @pre \p psMac should not be a NULL value and should represent a valid human readable MAC address.
//!
//! @note if psMac is NULL, this will result in comparing aMac with "00:00:00:00:00:00".
//!
int euca_machexcmp(char *psMac, u8 aMac[6])
{
    u8 aMacConv[6] = { 0 };
    if (mac2hex(psMac, aMacConv) != NULL)
        return (memcmp(aMacConv, aMac, (6 * sizeof(u8))));
    return (-1);
}

//!
//! Take a given string and split it into a list of up to nbTokens tokens based on a given delimiter
//!
//! @param[in] list the string containing the list of tokens delimited by \p delim
//! @param[in] delim the delimiter string to use to split the list
//! @param[in,out] tokens an array of pointers to string that will end up containing the tokens
//! @param[in] nbTokens the size of the tokens array
//!
//! @return -1 on failure or the number of tokens it was able to find
//!
//! @pre \p list, \p delim and \p tokens MUST not be null
//!
//! @post The list of tokens is filled with allocated strings and the original \p list isn't modified.
//!
//! @note the caller is responsible to free all tokens.
//!
int euca_tokenizer(char *list, char *delim, char *tokens[], int nbTokens)
{
    int count = 0;
    char *ptr = NULL;
    char *token = NULL;
    char *dupList = NULL;
    char *savePtr = NULL;

    // Validate our parameters
    if ((list == NULL) || (delim == NULL) || (tokens == NULL))
        return (-1);

    // Duplicate the list to avoid modifying it
    if ((dupList = strdup(list)) == NULL) {
        return (-1);
    }
    // retrieve our tokens
    ptr = dupList;
    while (((token = strtok_r(ptr, delim, &savePtr)) != NULL) && (count < nbTokens)) {
        tokens[count++] = strdup(token);
        ptr = savePtr;
    }

    // Free out list
    EUCA_FREE(dupList);
    return (count);
}
