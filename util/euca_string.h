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

#ifndef _INCLUDE_EUCA_STRING_H_
#define _INCLUDE_EUCA_STRING_H_

//!
//! @file util/euca_string.h
//! Definition of various string utility functions
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/
#include <string.h>
#include "data.h"

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

char *euca_strreplace(char **haystack, char *source, char *value);
boolean euca_lscanf(const char *haystack, const char *format, void *value);
char *euca_strestr(const char *haystack, const char *begin, const char *end);
long long euca_strtoll(const char *string, const char *begin, const char *end);
char *euca_strduptolower(const char *restrict string);
char *euca_strtolower(char *restrict string);
char *euca_strdup(const char *s1);
char *euca_strdupcat(char *restrict s1, const char *restrict s2);
char *euca_strncat(char *restrict dest, const char *restrict src, size_t size);
char *euca_strncpy(char *restrict to, const char *restrict from, size_t size);

char *euca_truncate_id(const char *id, const char *prefix, size_t idlen);
char *euca_truncate_interfaceid(const char *id);

//! @{
//! @name IP conversion APIs
u32 euca_dot2hex(const char *psDot);
char *euca_hex2dot(u32 hex);
char *euca_hex2dot_s(u32 hex);
//! @}

//! @{
//! @name MAC conversion APIs
u8 *euca_mac2hex(const char *psMacIn, u8 aHexOut[6]);
void euca_hex2mac(u8 aHexIn[6], char **ppsMacOut, boolean trimVersion);
int euca_maczero(u8 aMac[6]);
int euca_machexcmp(const char *psMac, u8 aMac[6]);
//! @}

int euca_tokenizer(char *list, char *delim, char *tokens[], int nbTokens);
char *euca_gettok(char *haystack, char *needle);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int inline euca_strcmp(const char *str1, const char *str2);
int inline euca_strstrcmp(const char *str1, const char *str2);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name IP Conversion macros

#define dot2hex(_dot)                            euca_dot2hex((_dot))
#define hex2dot(_hex)                            euca_hex2dot((_hex))
#define hex2dot_s(_hex)                          euca_hex2dot_s((_hex))

//! @}

//! @{
//! @name MAC address macros

#define mac2hex(_macIn, _hexOut)                 euca_mac2hex((_macIn), (_hexOut))
#define hex2mac(_hexIn, _macOut)                 euca_hex2mac((_hexIn), (_macOut), FALSE)
#define maczero(_mac)                            euca_maczero((_mac))
#define machexcmp(_sMac, _aMac)                  euca_machexcmp((_sMac), (_aMac))

//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/**
 * Compares strings str1 and str2. Returns an integer less than, equal to, or
 * greater than zero if str1 is found, respectively to be less than, to match,
 * or to be greater than str2. NULL pointer is considered greater than non NULL
 * string pointer.
 * @param str1 [in] string of interest
 * @param str2 [in] string of interest
 * @return 0 iff str1 == str2. -1 iff str1 < str2. 1 iff str1 > str2.
 */
int inline euca_strcmp(const char *str1, const char *str2) {
    if (str1 == str2) {
        return (0);
    }
    if (str1 == NULL) {
        return (1);
    }
    if (str2 == NULL) {
        return (-1);
    }
    return (strcmp(str1, str2));
}

/**
 * Compares strings str1 and str2. Returns an integer less than, equal to, or
 * greater than zero if str1 is found, respectively to be less than, to match,
 * or to be greater than str2. NULL pointer is considered greater than non NULL
 * string pointer.
 * If one of the strings is a substring of the other, starting at the beginning,
 * the comparison will be considered an exact match (returns 0).
 * @param str1 [in] string of interest
 * @param str2 [in] string of interest
 * @return 0 iff str1 == str2. -1 iff str1 < str2. 1 iff str1 > str2.
 */
int inline euca_strstrcmp(const char *str1, const char *str2) {
    if (str1 == str2) {
        return (0);
    }
    if (str1 == NULL) {
        return (1);
    }
    if (str2 == NULL) {
        return (-1);
    }
    if (strlen(str1) > strlen(str2)) {
        if (strstr(str1, str2) == str1) {
            return (0);
        }
    }
    if (strlen(str1) < strlen(str2)) {
        if (strstr(str2, str1) == str2) {
            return (0);
        }
    }
    return (strcmp(str1, str2));
}

#endif /* ! _INCLUDE_EUCA_STRING_H_ */
