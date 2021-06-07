/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class CreateLaunchTemplateVersionResponseType extends ComputeMessage {


  private LaunchTemplateVersion launchTemplateVersion;

  public LaunchTemplateVersion getLaunchTemplateVersion( ) {
    return launchTemplateVersion;
  }

  public void setLaunchTemplateVersion( final LaunchTemplateVersion launchTemplateVersion ) {
    this.launchTemplateVersion = launchTemplateVersion;
  }

}
