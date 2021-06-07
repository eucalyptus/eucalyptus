/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class DeleteLaunchTemplateVersionsType extends ComputeMessage {

  private String launchTemplateId;
  private String launchTemplateName;
  @Nonnull
  private VersionStringList versions;

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

  public VersionStringList getVersions( ) {
    return versions;
  }

  public void setVersions( final VersionStringList versions ) {
    this.versions = versions;
  }

}
