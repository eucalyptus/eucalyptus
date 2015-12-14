/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.workflow

/**
 * Created by ethomas on 12/11/15.
 */
public enum UpdateType {
  NONE,
  NO_INTERRUPTION,
  SOME_INTERRUPTION,
  NEEDS_REPLACEMENT;

  public static UpdateType max(UpdateType u1, UpdateType u2) {
    if (u1 == NEEDS_REPLACEMENT || u2 == NEEDS_REPLACEMENT) return NEEDS_REPLACEMENT;
    if (u1 == SOME_INTERRUPTION || u2 == SOME_INTERRUPTION) return SOME_INTERRUPTION;
    if (u1 == NO_INTERRUPTION || u2 == NO_INTERRUPTION) return NO_INTERRUPTION;
    return NONE;
  }
}