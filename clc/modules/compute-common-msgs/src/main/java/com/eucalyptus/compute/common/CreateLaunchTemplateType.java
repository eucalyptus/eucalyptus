/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class CreateLaunchTemplateType extends VmControlMessage {

  private String clientToken;
  @Nonnull
  private RequestLaunchTemplateData launchTemplateData;
  @Nonnull
  private String launchTemplateName;
  private String versionDescription;

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public RequestLaunchTemplateData getLaunchTemplateData( ) {
    return launchTemplateData;
  }

  public void setLaunchTemplateData( final RequestLaunchTemplateData launchTemplateData ) {
    this.launchTemplateData = launchTemplateData;
  }

  public String getLaunchTemplateName( ) {
    return launchTemplateName;
  }

  public void setLaunchTemplateName( final String launchTemplateName ) {
    this.launchTemplateName = launchTemplateName;
  }

  public String getVersionDescription( ) {
    return versionDescription;
  }

  public void setVersionDescription( final String versionDescription ) {
    this.versionDescription = versionDescription;
  }

}
