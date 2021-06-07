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
import static com.eucalyptus.reporting.event.EventActionInfo.InstanceEventActionInfo;

import com.eucalyptus.event.Event;
import com.eucalyptus.auth.principal.OwnerFullName;

public class VolumeEvent implements Event {
  private static final long serialVersionUID = 1L;

  public enum VolumeAction {
    VOLUMECREATE, VOLUMEDELETE, VOLUMEATTACH, VOLUMEDETACH, VOLUMEUSAGE
  }

  private final EventActionInfo<VolumeAction> actionInfo;
  private final String uuid;
  private final String volumeId;
  private final int sizeGB;
  private final String userId;
  private final String userName;
  private final String accountNumber;
  private final String availabilityZone;

  public static EventActionInfo<VolumeAction> forVolumeCreate() {
    return new EventActionInfo<VolumeAction>( VolumeAction.VOLUMECREATE );
  }

  public static EventActionInfo<VolumeAction> forVolumeDelete() {
    return new EventActionInfo<VolumeAction>( VolumeAction.VOLUMEDELETE );
  }

  public static InstanceEventActionInfo<VolumeAction> forVolumeAttach( final String instanceUuid,
                                                                       final String instanceId ) {
    return new InstanceEventActionInfo<VolumeAction>( VolumeAction.VOLUMEATTACH, instanceUuid, instanceId );
  }

  public static InstanceEventActionInfo<VolumeAction> forVolumeDetach( final String instanceUuid,
                                                                       final String instanceId ) {
    return new InstanceEventActionInfo<VolumeAction>( VolumeAction.VOLUMEDETACH, instanceUuid, instanceId );
  }

  public static EventActionInfo<VolumeAction> forVolumeUsage() {
    return new EventActionInfo<>( VolumeAction.VOLUMEUSAGE );
  }

  public static VolumeEvent with( final EventActionInfo<VolumeAction> actionInfo,
                                  final String uuid,
                                  final String volumeId,
                                  final int sizeGB,
                                  final OwnerFullName ownerFullName,
                                  final String availabilityZone ) {
    return new VolumeEvent( actionInfo, uuid, volumeId, sizeGB, ownerFullName, availabilityZone );
  }

  private VolumeEvent( final EventActionInfo<VolumeAction> actionInfo,
                       final String uuid,
                       final String volumeId,
                       final int sizeGB,
                       final OwnerFullName ownerFullName,
                       final String availabilityZone ) {
    checkParam( actionInfo, notNullValue() );
    checkParam( uuid, not( isEmptyOrNullString() ) );
    checkParam( sizeGB, greaterThan( -1 ) );
    checkParam( volumeId, not( isEmptyOrNullString() ) );
    checkParam( availabilityZone, notNullValue() );
    checkParam( ownerFullName.getUserId(), not( isEmptyOrNullString() ) );
    checkParam( ownerFullName.getAccountNumber(), not( isEmptyOrNullString() ) );
    checkParam( ownerFullName.getUserName(), not( isEmptyOrNullString() ) );
    this.userId = ownerFullName.getUserId( );
    this.userName = ownerFullName.getUserName( );
    this.accountNumber = ownerFullName.getAccountNumber( );
    this.actionInfo = actionInfo;
    this.uuid = uuid;
    this.sizeGB = sizeGB;
    this.volumeId = volumeId;
    this.availabilityZone = availabilityZone;
  }

  public String getVolumeId() {
    return volumeId;
  }

  public long getSizeGB() {
    return sizeGB;
  }

  public String getUserId() {
    return userId;
  }

  public String getUserName() {
    return userName;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public EventActionInfo<VolumeAction> getActionInfo() {
    return actionInfo;
  }

  public String getUuid() {
    return uuid;
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  @Override
  public String toString() {
    return "VolumeEvent [actionInfo=" + actionInfo + ", uuid=" + uuid
        + ", sizeGB=" + sizeGB
        + ", userId=" + userId + ", volumeId="
        + volumeId + ", availabilityZone=" + availabilityZone + "]";
  }

}
