// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2012 Eucalyptus Systems, Inc.
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
 ************************************************************************/

#ifndef INCLUDE_WC_H
#define INCLUDE_WC_H

#define _GNU_SOURCE
#include <wchar.h>

typedef struct wchar_map_struct {
    wchar_t *key;
    wchar_t *val;
} wchar_map;

typedef struct char_map_struct {
    char *key;
    char *val;
} char_map;

extern wchar_t *varsub(const wchar_t *, const wchar_map **);
extern wchar_map **varmap_alloc(wchar_map **, const wchar_t *, const wchar_t *);
extern void varmap_free(wchar_map **);

extern char *c_varsub(const char *, const char_map **);
extern char_map **c_varmap_alloc(char_map **, const char *, const char *);
extern void c_varmap_free(char_map **);

#endif // INCLUDE_WC_H
