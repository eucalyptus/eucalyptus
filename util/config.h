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

#ifndef CONFIG_H
#define CONFIG_H

#include "misc.h"

typedef struct configEntry_t {
  char *key;
  char *defaultValue;
} configEntry;

void configInitValues (configEntry newConfigKeysRestart[], configEntry newConfigKeysNoRestart[]);
char *configFileValue(const char *key);
int configFileValueLong(const char *key, long *val);
int isConfigModified (char configFiles[][MAX_PATH], int numFiles);
int readConfigFile(char configFiles[][MAX_PATH], int numFiles);
void configReadLogParams(int *log_level_out, int *log_roll_number_out, long *log_max_size_bytes_out);

#endif /* CONFIG_H */
