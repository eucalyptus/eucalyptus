// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
Copyright (c) 2012  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/

#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <ctype.h> // isspace
#include <assert.h>
#include <stdarg.h>
#include <errno.h> // errno
#include <locale.h> // setlocale

#include "misc.h" // boolean
#include "wc.h"

#define VAR_PREFIX L"${" // this sequence of chars indicates beginning of a variable name
#define VAR_SUFFIX L"}"  // this sequence of chars, when following VAR_PREFIX, means the end

// searches the 'vars' map (NULL-terminated array of wchar_map * pointers)
// for an entry with 'key' equaling 'name' with only the first 'name_len'
// characters considered; returns pointer to the 'val' in the map
static wchar_t *
find_valn (const wchar_map * vars [], const wchar_t * name, size_t name_len) 
{
    for (int i = 0; vars[i]!=NULL; i++) {
        const wchar_map * v = vars[i];
        if (wcsncmp (v->key, name, name_len) == 0)
            return v->val;
    }
    return NULL;
}

// appends string 'src' to 'dst', up to 'src_len' characters (unless set
// to 0, in which case to the end of 'src'), enlarging the 'dst' as necessary 
// returns the concatenated string or NULL if memory could not be allocated
// if 'src' is an empty string, 'dst' is returned.
static wchar_t * 
wcappendn (wchar_t * dst, const wchar_t * src, size_t src_limit)
{
    size_t src_len = wcslen (src);
    if (src_len < 1) 
        return dst;
    if (src_len > src_limit && src_limit > 0)
        src_len = src_limit;
    size_t dst_len = 0;
    if (dst)
        dst_len = wcslen (dst);
    dst = (wchar_t *) realloc (dst, (dst_len + src_len + 1) * sizeof (wchar_t));
    if (dst == NULL)
        return dst;
    return wcsncat (dst, src, src_len);
}

// substitutes in 's' all occurence of variables '${var}' 
// based on the 'vars' map (NULL-terminated array of wchar_map * pointers)
// returns a new string with all variables substituted or returns NULL
// (and logs an error with logprintfl()) if some variables were not
// found in the map or if the map is empty
wchar_t *
varsub (const wchar_t * s, const wchar_map * vars []) 
{
    assert (s!=NULL);
    assert (vars!=NULL);
    size_t pref_len = wcslen (VAR_PREFIX);
    size_t suff_len = wcslen (VAR_SUFFIX);

    int vars_subbed = 0;
    boolean malformed = FALSE;
    wchar_t * result = NULL;
    const wchar_t * remainder = s;

    wchar_t * var_start;
    while ((var_start = wcsstr (remainder, VAR_PREFIX)) != NULL ) { // we have a beginning of a variable
        if (wcslen (var_start) <= (pref_len + suff_len)) { // nothing past the prefix
            malformed = TRUE;
            break;
        }
        wchar_t * var_end = wcsstr (var_start + pref_len, VAR_SUFFIX);
        if (var_end == NULL) { // not suffix after prefix
            malformed = TRUE;
            break;
        }
        size_t var_len = var_end - var_start - pref_len; // length of the variable
        if (var_len < 1) { // empty var name
            remainder = var_end + suff_len; // move the pointer past the empty variable (skip it)
            malformed = TRUE;
            continue;
        }
        wchar_t * val = find_valn (vars, var_start + pref_len, var_len);
        if (val == NULL) {
            logprintfl (EUCAERROR, "failed to substitute variable\n"); // TODO: print variable name
            if (result != NULL) 
                free (result);
            return NULL;
        }
        if (var_start > remainder) { // there is text prior to the variable
            result = wcappendn (result, remainder, var_start - remainder);
            if (result == NULL) {
                logprintfl (EUCAERROR, "failed to append during variable substitution"); // TODO: more specific error
                break;
            }
        }
        result = wcappendn (result, val, 0);
        remainder = var_end + suff_len;
    }
    result = wcappendn (result, remainder, 0);
    
    if (malformed) {
        logprintfl (EUCAWARN, "malformed string used for substitution\n"); // TODO: print the string
    }

    return result;
}

wchar_map **
varmap_alloc (wchar_map **map, const wchar_t *key, const wchar_t *val)
{
    int i = 0;

    if (map == NULL) {
        map = malloc (2 * sizeof (wchar_map *));
    } else {
        while (map[i]) {
            i++;
        }
        map = realloc (map, (i + 2) * sizeof (wchar_map *));
    }
    map[i] = malloc (sizeof (wchar_map));
    map[i]->key = wcsdup (key);
    map[i]->val = wcsdup (val);
    map[i+1] = NULL;            /* NULL terminator */
        
    return map;    
}
    
void
varmap_free (wchar_map **map)
{
    int i = 0;

    if (map == NULL) {
        logprintfl (EUCAWARN, "varmap_free() called on NULL map.\n");
        return;
    }
    while (map[i]) {
        free (map[i]->key);
        free (map[i]->val);
        free (map[i]);
        i++;
    }
    free (map[i]);              /* NULL terminator */
    free (map);
}

/////////////////////////////////////////////// unit testing code ///////////////////////////////////////////////////

#ifdef _UNIT_TEST

const wchar_t * s1 = L"The quick ${color} ${subject} jümps øver the låzy ${øbject}";
const wchar_t * s2 = L"${}A m${}alformed ${color} string${}${}";
const wchar_t * s3 = L"An undefined ${variable}";
wchar_map **m = NULL;

int
main (int argc, char ** argv)
{
    setlocale(LC_ALL, "en_US.UTF-8");

    m = varmap_alloc (NULL, L"color", L"brown");
    m = varmap_alloc (m, L"subject", L"føx");
    m = varmap_alloc (m, L"øbject", L"dog");

    printf ("       nice string: %ls\n", s1);
    wchar_t * s1_sub = varsub (s1, (const wchar_map **)m);
    assert (s1_sub != NULL);
    printf ("nice string subbed: %ls\n", s1_sub);

    printf ("       ugly string: %ls\n", s2);
    wchar_t * s2_sub = varsub (s2, (const wchar_map **)m);
    assert (s2_sub != NULL);
    printf ("ugly string subbed: %ls\n", s2_sub);
    assert (varsub (s3, (const wchar_map **)m) == NULL);

    varmap_free(m);
}

#endif
