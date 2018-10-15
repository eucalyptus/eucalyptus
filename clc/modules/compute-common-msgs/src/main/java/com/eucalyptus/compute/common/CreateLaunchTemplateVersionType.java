/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class CreateLaunchTemplateVersionType extends ComputeMessage {

  private String clientToken;
  @Nonnull
  private RequestLaunchTemplateData launchTemplateData;
  private String launchTemplateId;
  private String launchTemplateName;
  private String sourceVersion;
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

  public String getLaunchTemplateId( ) {
    return launchTemplateId;
  }

  public void setLaunchTemplateId( final String launchTemplateId ) {
    this.launchTemplateId = launchTemplateId;
  }

  public String getLaunchTemplateName( ) {
    return launchTemplateName;
  }

  public void setLaunchTemplateName( final String launchTemplateName ) {
    this.launchTemplateName = launchTemplateName;
  }

  public String getSourceVersion( ) {
    return sourceVersion;
  }

  public void setSourceVersion( final String sourceVersion ) {
    this.sourceVersion = sourceVersion;
  }

  public String getVersionDescription( ) {
    return versionDescription;
  }

  public void setVersionDescription( final String versionDescription ) {
    this.versionDescription = versionDescription;
  }

}
