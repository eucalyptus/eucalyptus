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
//! @file util/wc.c
//! Implements the library that handles variable substitution within a string.
//!
//! Within a string, variables begins with '${' and terminates with '}'. For
//! example, the following string "This is the ${varname} variable" has '${varname}'
//! as a variable that can be substitured.
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <ctype.h>                     // isspace
#include <assert.h>
#include <stdarg.h>
#include <errno.h>                     // errno
#include <locale.h>                    // setlocale
#include <string.h>

#include "eucalyptus.h"
#include "misc.h"                      // boolean
#include "wc.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define VAR_PREFIX                               L"${"  //!< this sequence of chars indicates beginning of a variable name
#define VAR_SUFFIX                               L"}"   //!< this sequence of chars, when following VAR_PREFIX, means the end

#define C_VAR_PREFIX                             "${"   //!< this sequence of chars indicates beginning of a variable name
#define C_VAR_SUFFIX                             "}"    //!< this sequence of chars, when following VAR_PREFIX, means the end

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

#ifdef _UNIT_TEST
static const wchar_t *s1 = L"The quick ${color} ${subject} jümps øver the låzy ${øbject}";
static const wchar_t *s2 = L"${}A m${}alformed ${color} string${}${}";
static const wchar_t *s3 = L"An undefined ${variable}";
static wchar_map **m = NULL;

static const char *c_s1 = "The quick ${color} ${subject} jumps over the lazy ${object}";
static const char *c_s2 = "${}A m${}alformed ${color} string${}${}";
static const char *c_s3 = "An undefined ${variable}";
static char_map **c_m = NULL;
#endif /* _UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

wchar_t *varsub(const wchar_t * s, const wchar_map * vars[]);
wchar_map **varmap_alloc(wchar_map ** map, const wchar_t * key, const wchar_t * val);
void varmap_free(wchar_map ** map);
char *c_varsub(const char *s, const char_map * vars[]);
char_map **c_varmap_alloc(char_map ** map, const char *key, const char *val);
void c_varmap_free(char_map ** map);

#ifdef _UNIT_TEST
int main(int argc, char **argv);
#endif /* _UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static wchar_t *find_valn(const wchar_map * vars[], const wchar_t * name, size_t name_len);
static char *c_find_valn(const char_map * vars[], const char *name, size_t name_len);
static wchar_t *wcappendn(wchar_t * dst, const wchar_t * src, size_t src_limit);
static char *c_wcappendn(char *dst, const char *src, size_t src_limit);

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
//! searches the 'vars' map (NULL-terminated array of wchar_map * pointers)
//! for an entry with 'key' equaling 'name' with only the first 'name_len'
//! characters considered; returns pointer to the 'val' in the map.
//!
//! @param[in] vars NULL-terminated array of wchar_map pointers
//! @param[in] name the key we're looking for
//! @param[in] name_len the length of the name variable
//!
//! @return the looked up value if found otherwise NULL is returned
//!
//! @pre The name field must not be NULL.
//!
static wchar_t *find_valn(const wchar_map * vars[], const wchar_t * name, size_t name_len)
{
    int i = 0;
    const wchar_map *v = NULL;

    if (name != NULL) {
        for (i = 0; vars[i] != NULL; i++) {
            v = vars[i];
            if (wcsncmp(v->key, name, name_len) == 0)
                return (v->val);
        }
    }
    return (NULL);
}

//!
//! searches the 'vars' map (NULL-terminated array of char_map * pointers)
//! for an entry with 'key' equaling 'name' with only the first 'name_len'
//! characters considered; returns pointer to the 'val' in the map.
//!
//! @param[in] vars NULL-terminated array of char_map pointers
//! @param[in] name the key we're looking for
//! @param[in] name_len the length of the name variable
//!
//! @return the looked up value if found otherwise NULL is returned
//!
//! @pre The name field must not be NULL.
//!
static char *c_find_valn(const char_map * vars[], const char *name, size_t name_len)
{
    int i = 0;
    const char_map *v = NULL;

    if (name != NULL) {
        for (i = 0; vars[i] != NULL; i++) {
            v = vars[i];
            if (strncmp(v->key, name, name_len) == 0)
                return (v->val);
        }
    }
    return (NULL);
}

//!
//! appends string 'src' to 'dst', up to 'src_len' characters (unless set
//! to 0, in which case to the end of 'src'), enlarging the 'dst' as necessary
//! returns the concatenated string or NULL if memory could not be allocated
//! if 'src' is an empty string, 'dst' is returned.
//!
//! @param[in] dst the destination string to append to
//! @param[in] src the source string to add to 'dst'
//! @param[in] src_limit the number of character to append from 'src'
//!
//! @return the concatenated string or NULL if memory could not be allocated
//!         if 'src' is an empty string, 'dst' is returned.
//!
static wchar_t *wcappendn(wchar_t * dst, const wchar_t * src, size_t src_limit)
{
    size_t src_len = 0;
    size_t dst_len = 0;

    // Make sure we have a valid source
    if (src == NULL)
        return (dst);

    // Should not be empty
    if ((src_len = wcslen(src)) < 1)
        return (dst);

    // Estimate the proper length
    if ((src_len > src_limit) && (src_limit > 0))
        src_len = src_limit;

    if (dst != NULL) {
        dst_len = wcslen(dst);
        if ((dst = (wchar_t *) EUCA_REALLOC(dst, (dst_len + src_len + 1), sizeof(wchar_t))) == NULL) {
            return (NULL);
        }
    } else {
        if ((dst = (wchar_t *) EUCA_ALLOC((dst_len + src_len + 1), sizeof(wchar_t))) == NULL) {
            return (NULL);
        }
        *dst = L'\0';
    }

    return (wcsncat(dst, src, src_len));
}

//!
//! appends string 'src' to 'dst', up to 'src_len' characters (unless set
//! to 0, in which case to the end of 'src'), enlarging the 'dst' as necessary
//! returns the concatenated string or NULL if memory could not be allocated
//! if 'src' is an empty string, 'dst' is returned.
//!
//! @param[in] dst the destination string to append to
//! @param[in] src the source string to add to 'dst'
//! @param[in] src_limit the number of character to append from 'src'
//!
//! @return the concatenated string or NULL if memory could not be allocated
//!         if 'src' is an empty string, 'dst' is returned.
//!
static char *c_wcappendn(char *dst, const char *src, size_t src_limit)
{
    size_t src_len = 0;
    size_t dst_len = 0;

    // Make sure we have a valid source
    if (src == NULL)
        return (dst);

    // Should not be empty
    if ((src_len = strlen(src)) < 1)
        return (dst);

    // Estimate the proper length
    if ((src_len > src_limit) && (src_limit > 0))
        src_len = src_limit;

    if (dst != NULL) {
        dst_len = strlen(dst);
        if ((dst = (char *)EUCA_REALLOC(dst, (dst_len + src_len + 1), sizeof(char))) == NULL) {
            return (NULL);
        }
    } else {
        if ((dst = (char *)EUCA_ALLOC((dst_len + src_len + 1), sizeof(char))) == NULL) {
            return (NULL);
        }
        *dst = '\0';
    }

    return (strncat(dst, src, src_len));
}

//!
//! substitutes in 's' all occurence of variables '${var}' based on the 'vars' map (NULL-terminated
//! array of wchar_map * pointers) returns a new string with all variables substituted or returns NULL
//! (and logs an error with logprintfl()) if some variables were not found in the map or if the map is
//! empty.
//!
//! @param[in] s the string containing variables
//! @param[in] vars the list of variables
//!
//! @return s string containing the substitution or NULL if any error occured
//!
//! @pre The s field must not be NULL
//!
//! @note caller is responsible to free the returned string
//!
//! @todo This currently will not sub any variables if it can't sub *all* variables. This is unfriendly:
// !      it should sub what it can.
//!
wchar_t *varsub(const wchar_t * s, const wchar_map * vars[])
{
    boolean malformed = FALSE;
    wchar_t *result = NULL;
    const wchar_t *remainder = s;
    wchar_t *var_start = NULL;
    wchar_t *var_end = NULL;
    wchar_t *val = NULL;
    size_t var_len = 0;
    size_t pref_len = wcslen(VAR_PREFIX);
    size_t suff_len = wcslen(VAR_SUFFIX);

    if (s == NULL) {
        return (NULL);
    }

    if (vars == NULL) {
        return ((wchar_t *) wcsdup(s));
    }

    while ((var_start = wcsstr(remainder, VAR_PREFIX)) != NULL) {
        // we have a beginning of a variable
        if (wcslen(var_start) <= (pref_len + suff_len)) {
            // nothing past the prefix
            malformed = TRUE;
            break;
        }

        if ((var_end = wcsstr(var_start + pref_len, VAR_SUFFIX)) == NULL) {
            // not suffix after prefix
            malformed = TRUE;
            break;
        }
        // calculate length of the variable
        var_len = var_end - var_start - pref_len;
        if (var_len < 1) {
            // empty var name
            remainder = var_end + suff_len; // move the pointer past the empty variable (skip it)
            malformed = TRUE;
            continue;
        }

        if ((val = find_valn(vars, var_start + pref_len, var_len)) == NULL) {
            //! @todo print variable name
            LOGWARN("failed to substitute variable\n");
            EUCA_FREE(result);
            return (NULL);
        }

        if (var_start > remainder) {
            // there is text prior to the variable
            if ((result = wcappendn(result, remainder, var_start - remainder)) == NULL) {
                //! @todo more specific error
                LOGERROR("failed to append during variable substitution");
                break;
            }
        }

        result = wcappendn(result, val, 0);
        remainder = var_end + suff_len;
    }

    result = wcappendn(result, remainder, 0);

    if (malformed) {
        //! @todo print the string
        LOGWARN("malformed string used for substitution\n");
    }

    return (result);
}

//!
//! Allocate a wide char variable map entry
//!
//! @param[in,out] map the array of variables
//! @param[in]     key the variable key
//! @param[in]     val the variable value
//!
//! @return the variable map.
//!
wchar_map **varmap_alloc(wchar_map ** map, const wchar_t * key, const wchar_t * val)
{
    int i = 0;

    if (map == NULL) {
        PRINTF1(("malloc(): %d\n", sizeof(wchar_map *) + sizeof(wchar_t *)));
        map = EUCA_ALLOC(1, (sizeof(wchar_map *) + sizeof(wchar_t *)));
    } else {
        while (map[i]) {
            i++;
        }
        PRINTF1(("relloc(): %d\n", (i + 1) * sizeof(wchar_map *) + sizeof(wchar_t *)));
        map = EUCA_REALLOC(map, (i + 1), (sizeof(wchar_map *) + sizeof(wchar_t *)));
    }

    if (map != NULL) {
        if ((map[i] = EUCA_ALLOC(1, sizeof(wchar_map))) != NULL) {
            map[i]->key = wcsdup(key);
            map[i]->val = wcsdup(val);
            map[i + 1] = NULL;         // NULL terminator
        }
    }

    return (map);
}

//!
//! Frees a wide char variable map
//!
//! @param[in] map the map to free
//!
void varmap_free(wchar_map ** map)
{
    int i = 0;

    if (map == NULL) {
        PRINTF(("called on NULL map.\n"));
    } else {
        while (map[i]) {
            EUCA_FREE(map[i]->key);
            EUCA_FREE(map[i]->val);
            EUCA_FREE(map[i]);
            i++;
        }

        EUCA_FREE(map[i]);             // NULL terminator
        EUCA_FREE(map);
    }
}

//!
//! substitutes in 's' all occurence of variables '${var}'
//! based on the 'vars' map (NULL-terminated array of wchar_map * pointers)
//! returns a new string with all variables substituted or returns NULL
//! (and logs an error with logprintfl()) if some variables were not
//! found in the map or if the map is empty
//!
//! @param[in] s the string containing variables
//! @param[in] vars the list of variables
//!
//! @return a string containing the substitution
//!
//! @pre The s field must not be NULL
//!
//! @note caller is responsible to free the returned string
//!
char *c_varsub(const char *s, const char_map * vars[])
{
    boolean malformed = FALSE;
    char *result = NULL;
    const char *remainder = s;
    char *var_start = NULL;
    char *var_end = NULL;
    char *val = NULL;
    char *missed_var = NULL;
    char *vartok = NULL;
    size_t var_len = 0;
    size_t pref_len = strlen(C_VAR_PREFIX);
    size_t suff_len = strlen(C_VAR_SUFFIX);

    if (s == NULL) {
        return (NULL);
    }

    if (vars == NULL) {
        return ((char *)strdup(s));
    }

    while ((var_start = strstr(remainder, C_VAR_PREFIX)) != NULL) {
        // we have a beginning of a variable
        if (strlen(var_start) <= (pref_len + suff_len)) {
            // nothing past the prefix
            malformed = TRUE;
            break;
        }

        if ((var_end = strstr(var_start + pref_len, C_VAR_SUFFIX)) == NULL) {
            // not suffix after prefix
            malformed = TRUE;
            break;
        }
        // length of the variable
        var_len = var_end - var_start - pref_len;
        if (var_len < 1) {
            // empty var name
            remainder = var_end + suff_len; // move the pointer past the empty variable (skip it)
            malformed = TRUE;
            continue;
        }

        if ((val = c_find_valn(vars, var_start + pref_len, var_len)) == NULL) {
            if ((missed_var = strndup(var_start + pref_len, var_len)) == NULL) {
                LOGWARN("failed to substitute variable\n");
                continue;
            } else {
                LOGWARN("substituted variable: %s%s%s\n", C_VAR_PREFIX, missed_var, C_VAR_SUFFIX);
            }

            if ((vartok = (char *)EUCA_ALLOC(1, (strlen(C_VAR_PREFIX) + strlen(C_VAR_SUFFIX) + strlen(missed_var) + 1))) == NULL) {
                EUCA_FREE(result);
                EUCA_FREE(missed_var);
                return (NULL);
            }

            sprintf(vartok, "%s%s%s", C_VAR_PREFIX, missed_var, C_VAR_SUFFIX);
            if (var_start > remainder) {    // there is text prior to the variable
                if ((result = c_wcappendn(result, remainder, var_start - remainder)) == NULL) {
                    //! @todo more specific error
                    LOGERROR("failed to append during variable substitution");
                    EUCA_FREE(vartok);
                    EUCA_FREE(missed_var);
                    break;
                }
            }

            result = c_wcappendn(result, vartok, 0);
            remainder = var_end + suff_len; // move the pointer past the empty variable (skip it)
            EUCA_FREE(missed_var);
            EUCA_FREE(vartok);
            continue;
        }

        if (var_start > remainder) {
            // there is text prior to the variable
            if ((result = c_wcappendn(result, remainder, var_start - remainder)) == NULL) {
                //! @todo more specific error
                LOGERROR("failed to append during variable substitution");
                break;
            }
        }

        result = c_wcappendn(result, val, 0);
        remainder = var_end + suff_len;
    }
    result = c_wcappendn(result, remainder, 0);

    if (malformed) {
        //! @todo print the string
        LOGWARN("malformed string used for substitution\n");
    }

    return (result);
}

//!
//! Allocate a char variable map entry
//!
//! @param[in,out] map the array of variables
//! @param[in]     key the variable key
//! @param[in]     val the variable value
//!
//! @return the variable map.
//!
char_map **c_varmap_alloc(char_map ** map, const char *key, const char *val)
{
    int i = 0;

    if (map == NULL) {
        PRINTF1(("malloc(): %d\n", sizeof(char_map *) + sizeof(char *)));
        map = EUCA_ALLOC(1, (sizeof(char_map *) + sizeof(char *)));
    } else {
        while (map[i]) {
            i++;
        }
        PRINTF1(("relloc(): %d\n", (i + 1) * sizeof(char_map *) + sizeof(char *)));
        map = EUCA_REALLOC(map, (i + 1), sizeof(char_map *) + sizeof(char *));
    }

    if (map != NULL) {
        if ((map[i] = EUCA_ALLOC(1, sizeof(char_map))) != NULL) {
            map[i]->key = strdup(key);
            map[i]->val = strdup(val);
            map[i + 1] = NULL;         // NULL terminator
        }
    }

    return (map);
}

//!
//! Frees a wide char variable map
//!
//! @param[in,out] map the map to free
//!
void c_varmap_free(char_map ** map)
{
    int i = 0;

    if (map == NULL) {
        PRINTF(("called on NULL map.\n"));
    } else {
        while (map[i]) {
            EUCA_FREE(map[i]->key);
            EUCA_FREE(map[i]->val);
            EUCA_FREE(map[i]);
            i++;
        }

        EUCA_FREE(map[i]);             // NULL terminator
        EUCA_FREE(map);
    }
}

#ifdef _UNIT_TEST
//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char **argv)
{
    char *c_s1_sub = NULL;
    char *c_s2_sub = NULL;
    char *c_s3_sub = NULL;
    wchar_t *s1_sub = NULL;
    wchar_t *s2_sub = NULL;
    wchar_t *s3_sub = NULL;

    setlocale(LC_ALL, "en_US.UTF-8");

    m = varmap_alloc(NULL, L"color", L"brown");
    m = varmap_alloc(m, L"subject", L"føx");
    m = varmap_alloc(m, L"øbject", L"dog");

    printf("       nice string: %ls\n", s1);
    s1_sub = varsub(s1, (const wchar_map **)m);
    assert(s1_sub != NULL);
    printf("nice string subbed: %ls\n", s1_sub);
    EUCA_FREE(s1_sub);
    printf("       ugly string: %ls\n", s2);
    s2_sub = varsub(s2, (const wchar_map **)m);
    assert(s2_sub != NULL);
    printf("ugly string subbed: %ls\n", s2_sub);
    EUCA_FREE(s2_sub);
    printf(" unsubbable string: %ls\n", s3);
    assert(varsub(s3, (const wchar_map **)m) == NULL);

    varmap_free(m);

    printf("  sending null map: %ls\n", s3);    // Reuse s3
    s3_sub = varsub(s3, NULL);
    printf("returned from null: %ls\n", s3_sub);
    EUCA_FREE(s3_sub);

    // Now do it again, this time non-widechar
    c_m = c_varmap_alloc(NULL, "colorxxxxxx", "brown"); //! @todo This matches
    c_m = c_varmap_alloc(c_m, "subject", "fox");
    c_m = c_varmap_alloc(c_m, "object", "dog");

    printf("       nice string: %s\n", c_s1);
    c_s1_sub = c_varsub(c_s1, (const char_map **)c_m);
    printf("nice string subbed: %s\n", c_s1_sub);

    assert(c_s1_sub != NULL);
    EUCA_FREE(c_s1_sub);
    printf("       ugly string: %s\n", c_s2);
    c_s2_sub = c_varsub(c_s2, (const char_map **)c_m);
    assert(c_s2_sub != NULL);
    printf("ugly string subbed: %s\n", c_s2_sub);
    EUCA_FREE(c_s2_sub);

    printf(" unsubbable string: %s\n", c_s3);
    c_s3_sub = c_varsub(c_s3, (const char_map **)c_m);
    printf("   unsubbed string: %s\n", c_s3_sub);
    assert(!strcmp(c_s3, c_s3_sub));
    EUCA_FREE(c_s3_sub);

    c_varmap_free(c_m);

    printf("  sending null map: %s\n", c_s3);   // Reuse s3
    c_s3_sub = c_varsub(c_s3, NULL);
    printf("returned from null: %s\n", c_s3_sub);
    assert(!strcmp(c_s3, c_s3_sub));
    EUCA_FREE(c_s3_sub);
}
#endif /* _UNIT_TEST */
