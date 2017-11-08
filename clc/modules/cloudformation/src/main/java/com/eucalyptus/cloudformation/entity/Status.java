/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
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
