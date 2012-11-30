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
//! @file storage/map.c
//! Implementation of a dumb and dangerous map library (uses a linked list, allocates
//! and frees memory for keys, but leaves memory management of values to the user)
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <malloc.h>
#include <string.h>
#include "map.h"
#include "eucalyptus.h"

#ifdef _TEST_MAP
#include <assert.h>
#endif /* _TEST_MAP */

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

inline static void set(map * m, const char *key, void *val);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

map *map_create(int size);
void map_set(map * m, const char *key, void *val);
void *map_get(map * m, char *key);

#ifdef _TEST_MAP
int main(int argc, char *argv[]);
#endif /* _TEST_MAP */

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
//! Allocate a new MAP entry
//!
//! @param[in] size unused parameter
//!
//! @return a pointer to the new map entry structure or NULL if any error occured
//!
map *map_create(int size)
{
    return ((map *) EUCA_ZALLOC(1, sizeof(map)));
}

 //!
 //! Initialize a given map entry with a key and value
 //!
 //! @param[in] m a pointer to the map entry structure
 //! @param[in] key the unique key string
 //! @param[in] val a transparent pointer to the value
 //!
inline static void set(map * m, const char *key, void *val)
{
    m->key = strdup(key);
    m->val = val;
    m->next = NULL;
}

//!
//! Initialize a given map entry with a key and value
//!
//! @param[in] m a pointer to the map entry structure
//! @param[in] key the unique key string
//! @param[in] val a transparent pointer to the value
//!
void map_set(map * m, const char *key, void *val)
{
    map *mp = NULL;

    if (m->key == NULL) {
        // empty map
        set(m, key, val);
    } else {
        // map has stuff
        for (mp = m; mp != NULL; mp = mp->next) {
            if (strcmp(key, mp->key) == 0) {
                // key already exists, overwrite it
                mp->val = val;
                return;
            } else {
                // we are at the end, so add a new entry
                if (mp->next == NULL) {
                    mp->next = map_create(1);
                    mp = mp->next;
                    if (mp != NULL) //! @todo need to return an error if calloc failed
                        set(mp, key, val);
                    return;
                }
            }
        }
    }
}

//!
//! Retrieves a map entry value matching the given key
//!
//! @param[in] m pointer to the head of the MAP
//! @param[in] key the key we're looking for
//!
//! @return a transparent pointer to the matching value or NULL if any error occured
//!
void *map_get(map * m, char *key)
{
    map *mp = NULL;

    for (mp = m; mp != NULL; mp = mp->next) {
        if ((mp->key != NULL) && (strcmp(key, mp->key) == 0)) {
            return (mp->val);
        }
    }
    return (NULL);
}

#ifdef _TEST_MAP
//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @note little unit test: compile with gcc -g -D_TEST_MAP map.c
//!
int main(int argc, char *argv[])
{
    char *s1 = "string 1";
    char *s2 = "string 2";
    map *m = map_create(10);

    assert(map_get(m, "foo") == NULL);
    map_set(m, "k1", s1);
    assert(map_get(m, "k1") == s1);
    map_set(m, "k2", s2);
    assert(map_get(m, "k2") == s2);
    map_set(m, "k2", s1);
    assert(map_get(m, "k2") == s1);

    return (EUCA_OK);
}
#endif /* _TEST_MAP */
