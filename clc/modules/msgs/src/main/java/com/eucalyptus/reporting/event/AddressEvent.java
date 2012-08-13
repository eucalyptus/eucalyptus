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

  public enum AddressAction { ALLOCATE, RELEASE, ASSOCIATE, DISASSOCIATE }

  public static class ActionInfo {
    private final AddressAction action;

    private ActionInfo( final AddressAction action ) {
      assertThat( action, notNullValue() );
      this.action = action;
    }

    public AddressAction getAction() {
      return action;
    }

    public String toString() {
      return String.format( "[action:%s]", getAction() );
    }
  }

  public static class InstanceActionInfo extends ActionInfo {
    private final String instanceUuid;
    private final String instanceId;

    private InstanceActionInfo( final AddressAction action,
                                final String instanceUuid,
                                final String instanceId ) {
      super( action );
      assertThat( instanceUuid, notNullValue() );
      assertThat(instanceId, notNullValue());
      this.instanceUuid = instanceUuid;
      this.instanceId = instanceId;
    }

    public String getInstanceUuid() {
      return instanceUuid;
    }

    public String getInstanceId() {
      return instanceId;
    }

    public String toString() {
      return String.format( "[action:%s,instanceUuid:%s,instanceId:%s]",
          getAction(),
          getInstanceUuid(),
          getInstanceId() );
    }
  }

  private final String uuid;
  private final String address;
  private final String userId;
  private final String userName;
  private final String accountId;
  private final String accountName;
  private final ActionInfo actionInfo;

  public static ActionInfo forAllocate() {
    return new ActionInfo( AddressAction.ALLOCATE );
  }

  public static ActionInfo forRelease() {
    return new ActionInfo( AddressAction.RELEASE );
  }

  public static InstanceActionInfo forAssociate( final String instanceUuid,
                                                 final String instanceId ) {
    return new InstanceActionInfo( AddressAction.ASSOCIATE, instanceUuid, instanceId );
  }

  public static InstanceActionInfo forDisassociate( final String instanceUuid,
                                                    final String instanceId ) {
    return new InstanceActionInfo( AddressAction.DISASSOCIATE, instanceUuid, instanceId );
  }

  public static AddressEvent with( final String uuid,
                                   final String address,
                                   final OwnerFullName owner,
                                   final String accountName,
                                   final ActionInfo action ) {
    return new AddressEvent(
        uuid,
        address,
        owner.getUserId(),
        owner.getUserName(),
        owner.getAccountNumber(),
        Objects.firstNonNull( owner.getAccountName(), accountName ),
        action
    );
  }

  private AddressEvent( final String uuid,
                        final String address,
                        final String userId,
                        final String userName,
                        final String accountId,
                        final String accountName,
                        final ActionInfo actionInfo) {
    assertThat( uuid, notNullValue() );
    assertThat( address, notNullValue() );
    assertThat( userId, notNullValue() );
    assertThat( userName, notNullValue() );
    assertThat( accountId, notNullValue() );
    assertThat( accountName, notNullValue() );
    assertThat(actionInfo, notNullValue() );

    this.uuid = uuid;
    this.address = address;
    this.userId = userId;
    this.userName = userName;
    this.accountId = accountId;
    this.accountName = accountName;
    this.actionInfo = actionInfo;
  }

  public String getUuid() {
    return uuid;
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

  public ActionInfo getActionInfo() {
    return actionInfo;
  }

  public String toString() {
    return String.format(
        "[address:%s,userId:%s,accountId:%s,actionInfo:%s]",
        address, userId, accountId, actionInfo);
  }
}
