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

package com.eucalyptus.reporting.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import com.eucalyptus.event.Event;
import com.eucalyptus.util.OwnerFullName;

@SuppressWarnings("serial")
public class SnapShotEvent implements Event {

  public enum SnapShotAction {
    SNAPSHOTCREATE,
    SNAPSHOTDELETE
  }

  public static class ActionInfo {
    private final SnapShotAction action;

    private ActionInfo(final SnapShotAction action) {
      assertThat(action, notNullValue());
      this.action = action;
    }

    public SnapShotAction getAction() {
      return action;
    }

    public String toString() {
      return String.format("[action:%s]", getAction());
    }
  }

  private final ActionInfo actionInfo;
  private final Long sizeGB;
  private final OwnerFullName ownerFullName;
  private final String snapshotId;
  private final String uuid;

  public static ActionInfo forSnapShotCreate() {
    return new ActionInfo(SnapShotAction.SNAPSHOTCREATE);
  }

  public static ActionInfo forSnapShotDelete() {
    return new ActionInfo(SnapShotAction.SNAPSHOTDELETE);
  }

  public static SnapShotEvent with( final ActionInfo actionInfo,
                                    final String snapShotUUID,
                                    final String snapshotId,
                                    final OwnerFullName ownerFullName,
                                    final long sizeGB ) {

    return new SnapShotEvent(actionInfo, snapShotUUID, snapshotId, ownerFullName, sizeGB);
  }

  private SnapShotEvent( final ActionInfo actionInfo,
                         final String uuid,
                         final String snapshotId,
                         final OwnerFullName ownerFullName,
                         final long sizeGB) {
    assertThat(actionInfo, notNullValue());
    assertThat(uuid, notNullValue());
    assertThat(sizeGB, notNullValue());
    assertThat(ownerFullName.getUserId(), notNullValue());
    assertThat(snapshotId, notNullValue());
    this.actionInfo = actionInfo;
    this.sizeGB = sizeGB;
    this.ownerFullName = ownerFullName;
    this.snapshotId = snapshotId;
    this.uuid = uuid;
  }

  public String getSnapshotId() {
    return snapshotId;
  }

  public Long getSizeGB() {
    return sizeGB;
  }

  public OwnerFullName getOwnerFullName() {
    return ownerFullName;
  }

  public ActionInfo getActionInfo() {
    return actionInfo;
  }

  public String getUUID() {
    return uuid;
  }

  @Override
  public String toString() {
    return "SnapShotEvent [actionInfo=" + actionInfo + ", sizeGB=" + sizeGB
        + ", userName=" + ownerFullName.getUserName() + ", snapshotId="
        + snapshotId + ", uuid=" + uuid + "]";
  }
}
