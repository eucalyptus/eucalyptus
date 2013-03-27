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

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static com.eucalyptus.reporting.event.EventActionInfo.InstanceEventActionInfo;

import com.eucalyptus.event.Event;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Objects;

/**
 * AddressEvent is an event indicating address resource activity in the CLC.
 *
 * <p>The address will be in a canonical format to allow use as a natural
 * identifier for the resource.</p>
 *
 * <p>Events will be fired when an address is allocated, released, associated
 * or disassociated and is triggered (possibly indirectly) by user
 * activity.</p>
 */
public class AddressEvent implements Event {
  private static final long serialVersionUID = 1L;

  public enum AddressAction { ALLOCATE, RELEASE, ASSOCIATE, DISASSOCIATE }

  private final String address;
  private final String userId;
  private final String userName;
  private final String accountId;
  private final String accountName;
  private final EventActionInfo<AddressAction> actionInfo;

  public static EventActionInfo<AddressAction> forAllocate() {
    return new EventActionInfo<AddressAction>( AddressAction.ALLOCATE );
  }

  public static EventActionInfo<AddressAction> forRelease() {
    return new EventActionInfo<AddressAction>( AddressAction.RELEASE );
  }

  public static InstanceEventActionInfo<AddressAction> forAssociate( final String instanceUuid,
                                                                     final String instanceId ) {
    return new InstanceEventActionInfo<AddressAction>( AddressAction.ASSOCIATE, instanceUuid, instanceId );
  }

  public static InstanceEventActionInfo<AddressAction> forDisassociate( final String instanceUuid,
                                                                        final String instanceId ) {
    return new InstanceEventActionInfo<AddressAction>( AddressAction.DISASSOCIATE, instanceUuid, instanceId );
  }

  public static AddressEvent with( final String address,
                                   final OwnerFullName owner,
                                   final String accountName,
                                   final EventActionInfo<AddressAction> action ) {
    return new AddressEvent(
        address,
        owner.getUserId(),
        owner.getUserName(),
        owner.getAccountNumber(),
        Objects.firstNonNull( owner.getAccountName(), accountName ),
        action
    );
  }

  private AddressEvent( final String address,
                        final String userId,
                        final String userName,
                        final String accountId,
                        final String accountName,
                        final EventActionInfo<AddressAction> actionInfo) {
    checkParam( address, not(isEmptyOrNullString()) );
    checkParam( userId, not(isEmptyOrNullString()) );
    checkParam( userName, not(isEmptyOrNullString()) );
    checkParam( accountId, not(isEmptyOrNullString()) );
    checkParam( accountName, not(isEmptyOrNullString()) );
    checkParam( actionInfo, notNullValue() );

    this.address = address;
    this.userId = userId;
    this.userName = userName;
    this.accountId = accountId;
    this.accountName = accountName;
    this.actionInfo = actionInfo;
  }

  public String getAddress() {
    return address;
  }

  public String getUserId() {
    return userId;
  }

  public String getUserName() {
    return userName;
  }

  public String getAccountId() {
    return accountId;
  }

  public String getAccountName() {
    return accountName;
  }

  public EventActionInfo<AddressAction> getActionInfo() {
    return actionInfo;
  }

  public String toString() {
    return String.format(
        "[address:%s,userId:%s,accountId:%s,actionInfo:%s]",
        address, userId, accountId, actionInfo);
  }
}
