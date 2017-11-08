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
package com.eucalyptus.autoscaling.common.internal.configurations;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.LaunchConfigurationMetadata;
import com.eucalyptus.auth.principal.OwnerFullName;

/**
 * Immutable minimum view of a launch configuration.
 */
public class LaunchConfigurationMinimumView implements LaunchConfigurationMetadata {

  private final LaunchConfiguration launchConfiguration;

  public LaunchConfigurationMinimumView( final LaunchConfiguration launchConfiguration ) {
    this.launchConfiguration = launchConfiguration;
  }

  public String getLaunchConfigurationName() {
    return launchConfiguration.getLaunchConfigurationName();
  }

  @Override
  public String getDisplayName() {
    return getLaunchConfigurationName();
  }

  @Override
  public String getArn() {
    return launchConfiguration.getArn();
  }

  public String getIamInstanceProfile() {
    return launchConfiguration.getIamInstanceProfile();
  }

  public String getImageId() {
    return launchConfiguration.getImageId();
  }

  public Boolean getAssociatePublicIpAddress() {
    return launchConfiguration.getAssociatePublicIpAddress();
  }

  public Boolean getInstanceMonitoring() {
    return launchConfiguration.getInstanceMonitoring();
  }

  public String getInstanceType() {
    return launchConfiguration.getInstanceType();
  }

  public String getKernelId() {
    return launchConfiguration.getKernelId();
  }

  public String getKeyName() {
    return launchConfiguration.getKeyName();
  }

  public String getRamdiskId() {
    return launchConfiguration.getRamdiskId();
  }

  public String getUserData() {
    return launchConfiguration.getUserData();
  }

  @Override
  public OwnerFullName getOwner() {
    return launchConfiguration.getOwner();
  }

  public String getOwnerAccountNumber() {
    return launchConfiguration.getOwnerAccountNumber();
  }
}
