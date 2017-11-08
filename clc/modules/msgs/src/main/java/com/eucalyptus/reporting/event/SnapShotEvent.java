/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
