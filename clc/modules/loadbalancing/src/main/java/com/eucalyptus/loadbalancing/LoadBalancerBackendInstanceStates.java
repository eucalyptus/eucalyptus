/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.loadbalancing;

import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.STATE;

public enum LoadBalancerBackendInstanceStates {
  HealthCheckSuccess(STATE.InService, "", ""),
  InitialRegistration(STATE.OutOfService, "ELB", "Instance registration is still in progress."),
  HealthCheckFailure(STATE.OutOfService, "Instance" , "Instance has failed at least the UnhealthyThreshold number of health checks consecutively."),
  AvailabilityZoneDisabled(STATE.OutOfService, "ELB", "Zone disabled" ),
  UnrechableLoadBalancer(STATE.Error, "ELB", "Internal error: ELB VMs are not reachable. Contact administrator if problem continues"),
  InstanceStopped(STATE.OutOfService, "Instance", "Instance is in stopped state."),
  InstanceInvalidState(STATE.Error, "Instance", "Instance is in invalid state.");
  private STATE state = STATE.Unknown;
  private String reasonCode = null;
  private String description = null;

  LoadBalancerBackendInstanceStates(final STATE state, final String reasonCode, final String description) {
    this.state = state;
    this.reasonCode = reasonCode;
    this.description = description;
  }

  public STATE getState() {
    return this.state;
  }

  public String getReasonCode() {
    return this.reasonCode;
  }

  public String getDescription() {
    return this.description;
  }

  public boolean isInstanceState(final LoadBalancerBackendInstance instance) {
    return this.isInstanceState(instance.getState(), instance.getReasonCode(), instance.getDescription());
  }

  public boolean isInstanceState(final LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreView instance) {
    return this.isInstanceState(instance.getBackendState(), instance.getReasonCode(), instance.getDescription());
  }

  private boolean isInstanceState(final STATE state, final String reasonCode, final String description) {
    if (!this.state.equals(state))
      return false;

    if (this.reasonCode == null && reasonCode != null)
      return false;
    else if (this.reasonCode!=null && !this.reasonCode.equals(reasonCode))
      return false;

    if (this.description == null && description != null)
      return false;
    else if (this.description != null && !this.description.equals(description))
      return false;

    return true;
  }
}
