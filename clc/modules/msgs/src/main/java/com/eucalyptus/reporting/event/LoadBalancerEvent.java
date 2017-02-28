/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
