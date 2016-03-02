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
package com.eucalyptus.cloudformation.workflow.updateinfo;

import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateDirection;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;

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
