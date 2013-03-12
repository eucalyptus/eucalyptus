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
//! @file util/utf8.c
//! Implementation of UTF-8 charset encoding (RFC3629).
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <arpa/inet.h>          /* for htonl() */
#include <sys/types.h>

#include <wchar.h>

#include "utf8.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _NXT                                     0x80
#define _SEQ2                                    0xc0
#define _SEQ3                                    0xe0
#define _SEQ4                                    0xf0
#define _SEQ5                                    0xf8
#define _SEQ6                                    0xfc

#define _BOM                                     0xfeff

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

size_t utf8_to_wchar(const char *in, size_t insize, wchar_t * out, size_t outsize, int flags);
size_t wchar_to_utf8(const wchar_t * in, size_t insize, char *out, size_t outsize, int flags);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int __wchar_forbitten(wchar_t sym);
static int __utf8_forbitten(u_char octet);

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
//! Checks wether or not the given signed character is valid
//!
//! @param[in] sym the character to validate
//!
//! @return 0 if the signed character value is valid otherwise -1 is returned
//!
static int __wchar_forbitten(wchar_t sym)
{
    /* Surrogate pairs */
    if ((sym >= 0xd800) && (sym <= 0xdfff))
        return (-1);
    return (0);
}

//!
//! Checks wether or not the given octed is valid
//!
//! @param[in] octet the value to validate
//!
//! @return 0 if the octet value is valid otherwise -1 is returned
//!
static int __utf8_forbitten(u_char octet)
{
    switch (octet) {
    case 0xc0:
    case 0xc1:
    case 0xf5:
    case 0xff:
        return (-1);
    }

    return (0);
}

//!
//! This function translates UTF-8 string into UCS-4 string (all symbols
//! will be in local machine byte order).
//!
//! @param[in]  in input UTF-8 string. It can be null-terminated.
//! @param[in]  insize size of input string in bytes.
//! @param[out] out result buffer for UCS-4 string. If out is NULL, function returns size of result buffer.
//! @param[in]  outsize size of out buffer in wide characters.
//! @param[in]  flags
//!
//! @return The function returns size of result buffer (in wide characters). Zero is returned in case of error.
//!
//! @pre \li The in and out fields must not be NULL
//!      \li The insize and outsize must not be 0.
//!
//! @post The out field is set appropriately if non-NULL. If the out field is NULL, function returns size of result buffer.
//!
//! @note 1. If UTF-8 string contains zero symbols, they will be translated as regular symbols.
//!       2. If UTF8_IGNORE_ERROR or UTF8_SKIP_BOM flag is set, sizes may vary when `out' is NULL
//!          and not NULL. It's because of special UTF-8 sequences which may result in forbitten
//!          (by RFC3629) UNICODE characters.  So, the caller must check return value every time
//!          and not prepare buffer in advance (null terminate) but after calling this function.
//!
size_t utf8_to_wchar(const char *in, size_t insize, wchar_t * out, size_t outsize, int flags)
{
    u_char *p = NULL;
    u_char *lim = NULL;
    wchar_t *wlim = NULL;
    wchar_t high = 0;
    size_t n = 0;
    size_t total = 0;
    size_t i = 0;
    size_t n_bits = 0;

    if ((in == NULL) || (insize == 0) || ((outsize == 0) && (out != NULL)))
        return (0);

    total = 0;
    p = ((u_char *) in);
    lim = p + insize;
    wlim = out + outsize;

    for (; p < lim; p += n) {
        if ((__utf8_forbitten(*p) != 0) && ((flags & UTF8_IGNORE_ERROR) == 0))
            return (0);

        // Get number of bytes for one wide character.
        n = 1;                  // default: 1 byte. Used when skipping bytes.
        if ((*p & 0x80) == 0)
            high = ((wchar_t) * p);
        else if ((*p & 0xe0) == _SEQ2) {
            n = 2;
            high = ((wchar_t) (*p & 0x1f));
        } else if ((*p & 0xf0) == _SEQ3) {
            n = 3;
            high = ((wchar_t) (*p & 0x0f));
        } else if ((*p & 0xf8) == _SEQ4) {
            n = 4;
            high = ((wchar_t) (*p & 0x07));
        } else if ((*p & 0xfc) == _SEQ5) {
            n = 5;
            high = ((wchar_t) (*p & 0x03));
        } else if ((*p & 0xfe) == _SEQ6) {
            n = 6;
            high = ((wchar_t) (*p & 0x01));
        } else {
            if ((flags & UTF8_IGNORE_ERROR) == 0)
                return (0);
            continue;
        }

        // does the sequence header tell us truth about length?
        if (((signed)(lim - p)) <= ((signed)(n - 1))) {
            if ((flags & UTF8_IGNORE_ERROR) == 0)
                return (0);
            n = 1;
            continue;           // skip
        }
        // Validate sequence. All symbols must have higher bits set to 10xxxxxx
        if (n > 1) {
            for (i = 1; i < n; i++) {
                if ((p[i] & 0xc0) != _NXT)
                    break;
            }

            if (i != n) {
                if ((flags & UTF8_IGNORE_ERROR) == 0)
                    return (0);
                n = 1;
                continue;       // skip
            }
        }

        total++;

        if (out == NULL)
            continue;

        if (out >= wlim)
            return (0);         // no space left

        *out = 0;
        n_bits = 0;
        for (i = 1; i < n; i++) {
            *out |= ((wchar_t) (p[(n - i)] & 0x3f)) << n_bits;
            n_bits += 6;        // 6 low bits in every byte
        }
        *out |= (high << n_bits);

        if (__wchar_forbitten(*out) != 0) {
            if ((flags & UTF8_IGNORE_ERROR) == 0)
                return (0);     // forbitten character
            else {
                total--;
                out--;
            }
        } else if ((*out == _BOM) && ((flags & UTF8_SKIP_BOM) != 0)) {
            total--;
            out--;
        }

        out++;
    }

    return (total);
}

//!
//! This function translates UCS-4 symbols (given in local machine byte order) into UTF-8 string.
//!
//! @param[in]  in input unicode string. It can be null-terminated.
//! @param[in]  insize size of input string in wide characters.
//! @param[out] out result buffer for utf8 string. If out is NULL, function returns size of result buffer.
//! @param[in]  outsize size of result buffer.
//! @param[in]  flags
//!
//! @return The function returns size of result buffer (in bytes). Zero is returned in case of error.
//!
//! @pre \li The in and out fields must not be NULL
//!      \li The insize and outsize must not be 0.
//!
//! @post The out field is set appropriately if non-NULL. If the out field is NULL, function returns size of result buffer.
//!
//! @note If UCS-4 string contains zero symbols, they will be translated as regular symbols.
//!
size_t wchar_to_utf8(const wchar_t * in, size_t insize, char *out, size_t outsize, int flags)
{
    wchar_t *w = NULL;
    wchar_t *wlim = NULL;
    wchar_t ch = 0;
    u_char *p = NULL;
    u_char *lim = NULL;
    u_char *oc = NULL;
    size_t total = 0;
    size_t n = 0;

    if ((in == NULL) || (insize == 0) || ((outsize == 0) && (out != NULL)))
        return (0);

    w = ((wchar_t *) in);
    wlim = w + insize;
    p = ((u_char *) out);
    lim = p + outsize;
    total = 0;

    for (; w < wlim; w++) {
        if (__wchar_forbitten(*w) != 0) {
            if ((flags & UTF8_IGNORE_ERROR) == 0)
                return (0);
            continue;
        }

        if ((*w == _BOM) && ((flags & UTF8_SKIP_BOM) != 0))
            continue;

        if (*w < 0) {
            if ((flags & UTF8_IGNORE_ERROR) == 0)
                return (0);
            continue;
        } else if (*w <= 0x0000007f)
            n = 1;
        else if (*w <= 0x000007ff)
            n = 2;
        else if (*w <= 0x0000ffff)
            n = 3;
        else if (*w <= 0x001fffff)
            n = 4;
        else if (*w <= 0x03ffffff)
            n = 5;
        else                    // if (*w <= 0x7fffffff)
            n = 6;

        total += n;

        if (out == NULL)
            continue;

        if (((signed)(lim - p)) <= ((signed)(n - 1)))
            return (0);         // no space left

        /* make it work under different endians */
        ch = htonl(*w);
        oc = ((u_char *) & ch);
        switch (n) {
        case 1:
            *p = oc[3];
            break;

        case 2:
            p[1] = _NXT | (oc[3] & 0x3f);
            p[0] = _SEQ2 | (oc[3] >> 6) | ((oc[2] & 0x07) << 2);
            break;

        case 3:
            p[2] = _NXT | (oc[3] & 0x3f);
            p[1] = _NXT | (oc[3] >> 6) | ((oc[2] & 0x0f) << 2);
            p[0] = _SEQ3 | ((oc[2] & 0xf0) >> 4);
            break;

        case 4:
            p[3] = _NXT | (oc[3] & 0x3f);
            p[2] = _NXT | (oc[3] >> 6) | ((oc[2] & 0x0f) << 2);
            p[1] = _NXT | ((oc[2] & 0xf0) >> 4) | ((oc[1] & 0x03) << 4);
            p[0] = _SEQ4 | ((oc[1] & 0x1f) >> 2);
            break;

        case 5:
            p[4] = _NXT | (oc[3] & 0x3f);
            p[3] = _NXT | (oc[3] >> 6) | ((oc[2] & 0x0f) << 2);
            p[2] = _NXT | ((oc[2] & 0xf0) >> 4) | ((oc[1] & 0x03) << 4);
            p[1] = _NXT | (oc[1] >> 2);
            p[0] = _SEQ5 | (oc[0] & 0x03);
            break;

        case 6:
            p[5] = _NXT | (oc[3] & 0x3f);
            p[4] = _NXT | (oc[3] >> 6) | ((oc[2] & 0x0f) << 2);
            p[3] = _NXT | (oc[2] >> 4) | ((oc[1] & 0x03) << 4);
            p[2] = _NXT | (oc[1] >> 2);
            p[1] = _NXT | (oc[0] & 0x3f);
            p[0] = _SEQ6 | ((oc[0] & 0x40) >> 6);
            break;
        }

        // NOTE: do not check here for forbitten UTF-8 characters.
        //       They cannot appear here because we do proper convertion.
        p += n;
    }

    return (total);
}
