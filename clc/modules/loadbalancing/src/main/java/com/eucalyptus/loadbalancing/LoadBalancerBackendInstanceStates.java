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
package com.eucalyptus.loadbalancing;

import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerBackendInstance;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerBackendInstance.STATE;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerBackendInstanceView;

public enum LoadBalancerBackendInstanceStates {
  HealthCheckSuccess(STATE.InService, "", ""),
  InitialRegistration(STATE.OutOfService, "ELB", "Instance registration is still in progress."),
  HealthCheckFailure(STATE.OutOfService, "Instance",
      "Instance has failed at least the UnhealthyThreshold number of health checks consecutively."),
  AvailabilityZoneDisabled(STATE.OutOfService, "ELB", "Zone disabled"),
  UnrechableLoadBalancer(STATE.Error, "ELB",
      "Internal error: ELB VMs are not reachable. Contact administrator if problem continues"),
  InstanceStopped(STATE.OutOfService, "Instance", "Instance is in stopped state."),
  InstanceInvalidState(STATE.Error, "Instance", "Instance is in invalid state.");
  private STATE state = STATE.Unknown;
  private String reasonCode = null;
  private String description = null;

  LoadBalancerBackendInstanceStates(final STATE state, final String reasonCode,
      final String description) {
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
    return this.isInstanceState(instance.getState(), instance.getReasonCode(),
        instance.getDescription());
  }

  public boolean isInstanceState(final LoadBalancerBackendInstanceView instance) {
    return this.isInstanceState(instance.getBackendState(), instance.getReasonCode(),
        instance.getDescription());
  }

  private boolean isInstanceState(final STATE state, final String reasonCode,
      final String description) {
    if (!this.state.equals(state)) {
      return false;
    }

    if (this.reasonCode == null && reasonCode != null) {
      return false;
    } else if (this.reasonCode != null && !this.reasonCode.equals(reasonCode)) {
      return false;
    }

    if (this.description == null && description != null) {
      return false;
    } else if (this.description != null && !this.description.equals(description)) {
      return false;
    }

    return true;
  }
}
