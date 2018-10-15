/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DeleteLaunchTemplateVersionsResponseType extends ComputeMessage {


  private DeleteLaunchTemplateVersionsResponseSuccessSet successfullyDeletedLaunchTemplateVersions;
  private DeleteLaunchTemplateVersionsResponseErrorSet unsuccessfullyDeletedLaunchTemplateVersions;

  public DeleteLaunchTemplateVersionsResponseSuccessSet getSuccessfullyDeletedLaunchTemplateVersions( ) {
    return successfullyDeletedLaunchTemplateVersions;
  }

  public void setSuccessfullyDeletedLaunchTemplateVersions( final DeleteLaunchTemplateVersionsResponseSuccessSet successfullyDeletedLaunchTemplateVersions ) {
    this.successfullyDeletedLaunchTemplateVersions = successfullyDeletedLaunchTemplateVersions;
  }

  public DeleteLaunchTemplateVersionsResponseErrorSet getUnsuccessfullyDeletedLaunchTemplateVersions( ) {
    return unsuccessfullyDeletedLaunchTemplateVersions;
  }

  public void setUnsuccessfullyDeletedLaunchTemplateVersions( final DeleteLaunchTemplateVersionsResponseErrorSet unsuccessfullyDeletedLaunchTemplateVersions ) {
    this.unsuccessfullyDeletedLaunchTemplateVersions = unsuccessfullyDeletedLaunchTemplateVersions;
  }

}
