/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.autoscaling.instances;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingInstanceMetadata;
import com.eucalyptus.auth.principal.OwnerFullName;

/**
 * Immutable core view of an auto scaling instance.
 */
public class AutoScalingInstanceCoreView implements AutoScalingInstanceMetadata {

  private final AutoScalingInstance instance;

  public AutoScalingInstanceCoreView( final AutoScalingInstance instance ) {
    this.instance = instance;
  }

  public String getInstanceId() {
    return instance.getInstanceId();
  }

  @Override
  public String getDisplayName() {
    return getInstanceId();
  }

  public ConfigurationState getConfigurationState() {
    return instance.getConfigurationState();
  }

  public LifecycleState getLifecycleState() {
    return instance.getLifecycleState();
  }

  public String getOwnerAccountNumber() {
    return instance.getOwnerAccountNumber();
  }

  public String getAvailabilityZone() {
    return instance.getAvailabilityZone();
  }

  public Integer getRegistrationAttempts() {
    return instance.getRegistrationAttempts();
  }

  public HealthStatus getHealthStatus() {
    return instance.getHealthStatus();
  }

  public String getLaunchConfigurationName() {
    return instance.getLaunchConfigurationName();
  }

  @Override
  public OwnerFullName getOwner() {
    return instance.getOwner();
  }

  public long getCreationTimestamp() {
    return instance.getCreationTimestamp().getTime();
  }
}
