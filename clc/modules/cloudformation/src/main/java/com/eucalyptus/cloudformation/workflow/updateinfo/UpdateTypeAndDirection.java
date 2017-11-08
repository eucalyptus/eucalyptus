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
package com.eucalyptus.cloudformation.workflow.updateinfo;

/**
* Created by ethomas on 1/18/16.
*/
public enum UpdateTypeAndDirection {
  UPDATE_NO_INTERRUPTION(UpdateType.NO_INTERRUPTION, UpdateDirection.FORWARD),
  UPDATE_SOME_INTERRUPTION(UpdateType.SOME_INTERRUPTION, UpdateDirection.FORWARD),
  UPDATE_WITH_REPLACEMENT(UpdateType.NEEDS_REPLACEMENT, UpdateDirection.FORWARD),
  UPDATE_ROLLBACK_NO_INTERRUPTION(UpdateType.NO_INTERRUPTION, UpdateDirection.ROLLBACK),
  UPDATE_ROLLBACK_SOME_INTERRUPTION(UpdateType.NO_INTERRUPTION, UpdateDirection.ROLLBACK),
  UPDATE_ROLLBACK_WITH_REPLACEMENT(UpdateType.NO_INTERRUPTION, UpdateDirection.ROLLBACK);

  private UpdateType updateType;
  private UpdateDirection updateDirection;

  UpdateTypeAndDirection(UpdateType updateType, UpdateDirection updateDirection) {
    this.updateType = updateType;
    this.updateDirection = updateDirection;
  }
}
