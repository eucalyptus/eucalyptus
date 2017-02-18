/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;

import com.eucalyptus.event.Event;

public class SnapShotEvent implements Event {
  private static final long serialVersionUID = 1L;

  public enum SnapShotAction {
    SNAPSHOTCREATE,
    SNAPSHOTDELETE,
    SNAPSHOTUSAGE
  }

  public static class CreateActionInfo extends EventActionInfo<SnapShotAction> {
    private static final long serialVersionUID = 1L;
    private final Integer size;
    private final String volumeId;
    private final String volumeUuid;

    private CreateActionInfo( final Integer size,
                              final String volumeUuid,
                              final String volumeId ) {
      super( SnapShotAction.SNAPSHOTCREATE );
      this.size = size;
      this.volumeUuid = volumeUuid;
      this.volumeId = volumeId;
    }

    /**
     * Get the size in GiB
     */
    public Integer getSize() {
      return size;
    }

    /**
     * Get the uuid for the parent volume
     */
    public String getVolumeUuid() {
      return volumeUuid;
    }

    /**
     * Get the id for the parent volume
     */
    public String getVolumeId() {
      return volumeId;
    }

    public String toString() {
      return String.format("[action:%s,size:%s]", getAction(), getSize());
    }
  }

  private final EventActionInfo<SnapShotAction> actionInfo;
  private final String userId;
  private final String userName;
  private final String accountNumber;
  private final String snapshotId;
  private final String uuid;
  private final int volumeSizeGB;;

  /**
   * Action for snapshot creation.
   *
   * @param size The snapshot size in GiB
   * @return The action info
   */
  public static EventActionInfo<SnapShotAction> forSnapShotCreate( final Integer size,
                                                                   final String volumeUuid,
                                                                   final String volumeId ) {
    checkParam( size, greaterThan( -1 ) );
    checkParam( volumeUuid, not( isEmptyOrNullString() ) );
    checkParam( volumeId, not( isEmptyOrNullString() ) );

    return new CreateActionInfo( size, volumeUuid, volumeId );
  }

  public static EventActionInfo<SnapShotAction> forSnapShotDelete() {
    return new EventActionInfo<SnapShotAction>(SnapShotAction.SNAPSHOTDELETE);
  }

  public static EventActionInfo<SnapShotAction> forSnapShotUsage() {
    return new EventActionInfo<>(SnapShotAction.SNAPSHOTUSAGE);
  }

  public static SnapShotEvent with( final EventActionInfo<SnapShotAction> actionInfo,
                                    final String snapShotUUID,
                                    final String snapshotId,
                                    final String userId,
                                    final String userName,
                                    final String accountNumber ) {

    return new SnapShotEvent( actionInfo, snapShotUUID, snapshotId, userId, userName, accountNumber, -1);
  }

  public static SnapShotEvent with( final EventActionInfo<SnapShotAction> actionInfo,
                                    final String snapShotUUID,
                                    final String snapshotId,
                                    final String userId,
                                    final String userName,
                                    final String accountNumber,
                                    final int volumeSizeGb) {
    return new SnapShotEvent( actionInfo, snapShotUUID, snapshotId, userId, userName, accountNumber, volumeSizeGb);
  }

  private SnapShotEvent( final EventActionInfo<SnapShotAction> actionInfo,
                         final String uuid,
                         final String snapshotId,
                         final String userId,
                         final String userName,
                         final String accountNumber,
                         final int volumeSizeGb) {
    checkParam( actionInfo, notNullValue() );
    checkParam( uuid, not( isEmptyOrNullString() ) );
    checkParam( userId, not( isEmptyOrNullString() ) );
    checkParam( userName, not( isEmptyOrNullString() ) );
    checkParam( accountNumber, not( isEmptyOrNullString() ) );
    checkParam( snapshotId, not( isEmptyOrNullString() ) );
    this.actionInfo = actionInfo;
    this.userId = userId;
    this.userName = userName;
    this.accountNumber = accountNumber;
    this.snapshotId = snapshotId;
    this.uuid = uuid;
    this.volumeSizeGB = volumeSizeGb;
  }



  public String getSnapshotId() {
    return snapshotId;
  }

  public String getUserId() {
    return userId;
  }

  public String getUserName() { return userName; }

  public String getAccountNumber() {
    return accountNumber;
  }

  public EventActionInfo<SnapShotAction> getActionInfo() {
    return actionInfo;
  }

  public String getUuid() {
    return uuid;
  }

  public int getVolumeSizeGB() { return this.volumeSizeGB; }

  @Override
  public String toString() {
    return "SnapShotEvent [actionInfo=" + actionInfo
        + ", userId=" + userId + ", snapshotId="
        + snapshotId + ", uuid=" + uuid + "]";
  }
}
