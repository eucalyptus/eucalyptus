/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LaunchTemplate extends EucalyptusData {

  private Date createTime;
  private String createdBy;
  private Long defaultVersionNumber;
  private Long latestVersionNumber;
  private String launchTemplateId;
  private String launchTemplateName;
  private ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>( );


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

  public Long getDefaultVersionNumber( ) {
    return defaultVersionNumber;
  }

  public void setDefaultVersionNumber( final Long defaultVersionNumber ) {
    this.defaultVersionNumber = defaultVersionNumber;
  }

  public Long getLatestVersionNumber( ) {
    return latestVersionNumber;
  }

  public void setLatestVersionNumber( final Long latestVersionNumber ) {
    this.latestVersionNumber = latestVersionNumber;
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

  public ArrayList<ResourceTag> getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ArrayList<ResourceTag> tagSet ) {
    this.tagSet = tagSet;
  }

}
