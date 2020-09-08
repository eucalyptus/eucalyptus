/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeLaunchTemplatesResponseType extends ComputeMessage {


  private LaunchTemplateSet launchTemplates = new LaunchTemplateSet();
  private String nextToken;

  public LaunchTemplateSet getLaunchTemplates( ) {
    return launchTemplates;
  }

  public void setLaunchTemplates( final LaunchTemplateSet launchTemplates ) {
    this.launchTemplates = launchTemplates;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

}
