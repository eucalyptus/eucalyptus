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
 ************************************************************************/

#ifndef INCLUDE_WINDOWS_BUNDLE_H
#define INCLUDE_WINDOWS_BUNDLE_H

#include <eucalyptus.h>
#include <data.h>

#define MAXBUNDLES MAXINSTANCES

typedef struct bundleTask_t {
  char instanceId[CHAR_BUFFER_SIZE];
  char state[CHAR_BUFFER_SIZE];
} bundleTask;

int allocate_bundleTask(bundleTask *out, char *instanceId, char *state);

#endif
