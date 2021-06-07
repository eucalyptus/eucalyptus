/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.event.Event;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;

public class LoadBalancerEvent implements Event {
  private static final long serialVersionUID = 1L;

  public enum LoadBalancerAction {
    LOADBALANCER_USAGE
  }

  private final EventActionInfo<LoadBalancerEvent.LoadBalancerAction> actionInfo;
  private final String loadbalancerName;
  private final String userId;
  private final String userName;
  private final String accountNumber;

  public static EventActionInfo<LoadBalancerEvent.LoadBalancerAction> forLoadBalancerUsage() {
    return new EventActionInfo<LoadBalancerEvent.LoadBalancerAction>( LoadBalancerAction.LOADBALANCER_USAGE );
  }


  public static LoadBalancerEvent with( final EventActionInfo<LoadBalancerEvent.LoadBalancerAction> actionInfo,
                                  final OwnerFullName ownerFullName,
                                  final String loadbalancerName ) {
    return new LoadBalancerEvent( actionInfo, ownerFullName, loadbalancerName );
  }

  private LoadBalancerEvent( final EventActionInfo<LoadBalancerEvent.LoadBalancerAction> actionInfo,
                       final OwnerFullName ownerFullName,
                       final String loadBalancerName ) {
    checkParam( actionInfo, notNullValue() );
    checkParam( ownerFullName.getUserId(), not( isEmptyOrNullString() ) );
    checkParam( ownerFullName.getAccountNumber(), not( isEmptyOrNullString() ) );
    checkParam( ownerFullName.getUserName(), not( isEmptyOrNullString() ) );
    checkParam( loadBalancerName, not( isEmptyOrNullString() ));
    this.userId = ownerFullName.getUserId( );
    this.userName = ownerFullName.getUserName( );
    this.accountNumber = ownerFullName.getAccountNumber( );
    this.actionInfo = actionInfo;
    this.loadbalancerName = loadBalancerName;
  }

  public String getUserId() {
    return this.userId;
  }

  public String getUserName() {
    return this.userName;
  }

  public String getAccountNumber() {
    return this.accountNumber;
  }

  public EventActionInfo<LoadBalancerEvent.LoadBalancerAction> getActionInfo() {
    return this.actionInfo;
  }

  public String getLoadbalancerName() {
    return this.loadbalancerName;
  }

  @Override
  public String toString() {
    return "LoadBalancerEvent [actionInfo=" + actionInfo + ", accountId=" + accountNumber
            + ", userId=" + userId + ", loadbalancerName="
            + loadbalancerName + "]";
  }
}
