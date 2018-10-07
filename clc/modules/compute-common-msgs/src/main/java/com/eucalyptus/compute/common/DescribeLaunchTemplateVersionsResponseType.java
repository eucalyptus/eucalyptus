/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
