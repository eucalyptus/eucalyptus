/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LaunchTemplateVersion extends EucalyptusData {

  private Date createTime;
  private String createdBy;
  private Boolean defaultVersion;
  private ResponseLaunchTemplateData launchTemplateData;
  private String launchTemplateId;
  private String launchTemplateName;
  private String versionDescription;
  private Long versionNumber;

  public Date getCreateTime( ) {
    return createTime;
  }

  public void setCreateTime( final Date createTime ) {
    this.createTime = createTime;
  }

  public String getCreatedBy( ) {
    return createdBy;
  }

  public void setCreatedBy( final String createdBy ) {
    this.createdBy = createdBy;
  }

  public Boolean getDefaultVersion( ) {
    return defaultVersion;
  }

  public void setDefaultVersion( final Boolean defaultVersion ) {
    this.defaultVersion = defaultVersion;
  }

  public ResponseLaunchTemplateData getLaunchTemplateData( ) {
    return launchTemplateData;
  }

  public void setLaunchTemplateData( final ResponseLaunchTemplateData launchTemplateData ) {
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

  public String getVersionDescription( ) {
    return versionDescription;
  }

  public void setVersionDescription( final String versionDescription ) {
    this.versionDescription = versionDescription;
  }

  public Long getVersionNumber( ) {
    return versionNumber;
  }

  public void setVersionNumber( final Long versionNumber ) {
    this.versionNumber = versionNumber;
  }

}
