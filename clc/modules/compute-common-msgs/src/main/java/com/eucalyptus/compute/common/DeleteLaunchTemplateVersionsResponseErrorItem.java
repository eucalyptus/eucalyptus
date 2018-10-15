/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DeleteLaunchTemplateVersionsResponseErrorItem extends EucalyptusData {

  private String launchTemplateId;
  private String launchTemplateName;
  private ResponseError responseError;
  private Long versionNumber;

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

  public ResponseError getResponseError( ) {
    return responseError;
  }

  public void setResponseError( final ResponseError responseError ) {
    this.responseError = responseError;
  }

  public Long getVersionNumber( ) {
    return versionNumber;
  }

  public void setVersionNumber( final Long versionNumber ) {
    this.versionNumber = versionNumber;
  }

}
