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

public class SnapShotEvent implements Event {
  private static final long serialVersionUID = 1L;

  public enum SnapShotAction {
    SNAPSHOTCREATE,
    SNAPSHOTDELETE
  }

  public static class CreateActionInfo extends EventActionInfo<SnapShotAction> {
    private static final long serialVersionUID = 1L;
    private final Long size;

    private CreateActionInfo( final Long size ) {
      super( SnapShotAction.SNAPSHOTCREATE );
      this.size = size;
    }

    /**
     * Get the size in GiB
     */
    public Long getSize() {
      return size;
    }

    public String toString() {
      return String.format("[action:%s,size:%s]", getAction(), getSize());
    }
  }

  private final EventActionInfo<SnapShotAction> actionInfo;
  private final String userName;
  private final String snapshotId;
  private final String uuid;

  /**
   * Action for snapshot creation.
   *
   * @param size The snapshot size in GiB
   * @return The action info
   */
  public static EventActionInfo<SnapShotAction> forSnapShotCreate( final Long size ) {
    assertThat(size, notNullValue());

    return new CreateActionInfo( size );
  }

  public static EventActionInfo<SnapShotAction> forSnapShotDelete() {
    return new EventActionInfo<SnapShotAction>(SnapShotAction.SNAPSHOTDELETE);
  }

  public static SnapShotEvent with( final EventActionInfo<SnapShotAction> actionInfo,
                                    final String snapShotUUID,
                                    final String snapshotId,
                                    final String userName ) {

    return new SnapShotEvent( actionInfo, snapShotUUID, snapshotId, userName );
  }

  private SnapShotEvent( final EventActionInfo<SnapShotAction> actionInfo,
                         final String uuid,
                         final String snapshotId,
                         final String userName ) {
    assertThat(actionInfo, notNullValue());
    assertThat(uuid, notNullValue());
    assertThat(userName, notNullValue());
    assertThat(snapshotId, notNullValue());
    this.actionInfo = actionInfo;
    this.userName = userName;
    this.snapshotId = snapshotId;
    this.uuid = uuid;
  }

  public String getSnapshotId() {
    return snapshotId;
  }

  public String getOwner() {
    return userName;
  }

  public EventActionInfo<SnapShotAction> getActionInfo() {
    return actionInfo;
  }

  public String getUuid() {
    return uuid;
  }

  @Override
  public String toString() {
    return "SnapShotEvent [actionInfo=" + actionInfo
        + ", userName=" + userName + ", snapshotId="
        + snapshotId + ", uuid=" + uuid + "]";
  }
}
