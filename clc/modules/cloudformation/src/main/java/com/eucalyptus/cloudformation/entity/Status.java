/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.entity;

/**
* Created by ethomas on 12/8/15.
*/
public enum Status {

  // There are some common status values between Stacks and StackResources.
  // Unlike the documentation, StackEvents can have all values.  To avoid equality checks against different
  // Status enums, one was used.  Role represents whether or not the value is applicable to Stacks, Resources, or
  // Both.

  NOT_STARTED(Role.RESOURCE), // not an AWS type
  CREATE_IN_PROGRESS(Role.BOTH),
  CREATE_FAILED(Role.BOTH),
  CREATE_COMPLETE(Role.BOTH),
  ROLLBACK_IN_PROGRESS(Role.STACK),
  ROLLBACK_FAILED(Role.STACK),
  ROLLBACK_COMPLETE(Role.STACK),
  DELETE_IN_PROGRESS(Role.BOTH),
  DELETE_FAILED(Role.BOTH),
  DELETE_SKIPPED(Role.RESOURCE),
  DELETE_COMPLETE(Role.BOTH),
  UPDATE_IN_PROGRESS(Role.BOTH),
  UPDATE_FAILED(Role.RESOURCE),
  UPDATE_COMPLETE_CLEANUP_IN_PROGRESS(Role.STACK),
  UPDATE_COMPLETE(Role.BOTH),
  UPDATE_ROLLBACK_IN_PROGRESS(Role.STACK),
  UPDATE_ROLLBACK_FAILED(Role.STACK),
  UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS(Role.STACK),
  UPDATE_ROLLBACK_COMPLETE(Role.STACK);

  private Role role;
  Status(Role role) {
    this.role = role;
  }

  private enum Role {
    STACK,
    RESOURCE,
    BOTH
  };
}
