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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static com.eucalyptus.reporting.event.EventActionInfo.InstanceEventActionInfo;

import com.eucalyptus.event.Event;
import com.eucalyptus.auth.principal.OwnerFullName;

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

  public enum AddressAction { ALLOCATE, RELEASE, ASSOCIATE, DISASSOCIATE, USAGE_ALLOCATE, USAGE_ASSOCIATE }

  private final String address;
  private final String userId;
  private final String userName;
  private final String accountId;
  private final String accountName;
  private final String instanceId;
  private final EventActionInfo<AddressAction> actionInfo;

  public static EventActionInfo<AddressAction> forAllocate() {
    return new EventActionInfo<AddressAction>( AddressAction.ALLOCATE );
  }

  public static EventActionInfo<AddressAction> forRelease() {
    return new EventActionInfo<AddressAction>( AddressAction.RELEASE );
  }

  public static EventActionInfo<AddressAction> forUsageAllocate() {
    return new EventActionInfo<AddressAction>( AddressAction.USAGE_ALLOCATE );
  }

  public static EventActionInfo<AddressAction> forUsageAssociate() {
    return new EventActionInfo<AddressAction>( AddressAction.USAGE_ASSOCIATE );
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
        accountName,
        action
    );
  }

  public static AddressEvent with( final String address,
                                   final OwnerFullName owner,
                                   final String accountName,
                                   final String instanceId,
                                   final EventActionInfo<AddressAction> action ) {
    return new AddressEvent(
            address,
            owner.getUserId(),
            owner.getUserName(),
            owner.getAccountNumber(),
            accountName,
            instanceId,
            action
    );
  }

  private AddressEvent( final String address,
                        final String userId,
                        final String userName,
                        final String accountId,
                        final String accountName,
                        final EventActionInfo<AddressAction> actionInfo) {
    this(address, userId, userName, accountId, accountName, null, actionInfo);
  }

  private AddressEvent( final String address,
                        final String userId,
                        final String userName,
                        final String accountId,
                        final String accountName,
                        final String instanceId,
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
    this.instanceId = instanceId;
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

  public String getInstanceId() { return instanceId; }

  public EventActionInfo<AddressAction> getActionInfo() {
    return actionInfo;
  }

  public String toString() {
    return String.format(
        "[address:%s,userId:%s,accountId:%s,actionInfo:%s]",
        address, userId, accountId, actionInfo);
  }
}
