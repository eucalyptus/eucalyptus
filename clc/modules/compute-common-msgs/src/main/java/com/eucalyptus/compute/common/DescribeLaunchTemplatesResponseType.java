/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeLaunchTemplatesResponseType extends ComputeMessage {


  private LaunchTemplateSet launchTemplates;
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
