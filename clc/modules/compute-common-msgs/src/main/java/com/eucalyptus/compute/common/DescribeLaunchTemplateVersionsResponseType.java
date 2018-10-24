/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

public class DescribeLaunchTemplateVersionsResponseType extends ComputeMessage {

  private LaunchTemplateVersionSet launchTemplateVersions;
  private String nextToken;

  public LaunchTemplateVersionSet getLaunchTemplateVersions( ) {
    return launchTemplateVersions;
  }

  public void setLaunchTemplateVersions( final LaunchTemplateVersionSet launchTemplateVersions ) {
    this.launchTemplateVersions = launchTemplateVersions;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

}
