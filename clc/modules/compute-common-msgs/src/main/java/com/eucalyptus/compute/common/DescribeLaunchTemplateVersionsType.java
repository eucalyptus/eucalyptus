/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;


public class DescribeLaunchTemplateVersionsType extends ComputeMessage {

  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );
  private String launchTemplateId;
  private String launchTemplateName;
  private Integer maxResults;
  private String maxVersion;
  private String minVersion;
  private String nextToken;
  @HttpEmbedded
  private VersionStringList versions;

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
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

  public Integer getMaxResults( ) {
    return maxResults;
  }

  public void setMaxResults( final Integer maxResults ) {
    this.maxResults = maxResults;
  }

  public String getMaxVersion( ) {
    return maxVersion;
  }

  public void setMaxVersion( final String maxVersion ) {
    this.maxVersion = maxVersion;
  }

  public String getMinVersion( ) {
    return minVersion;
  }

  public void setMinVersion( final String minVersion ) {
    this.minVersion = minVersion;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

  public VersionStringList getVersions( ) {
    return versions;
  }

  public void setVersions( final VersionStringList versions ) {
    this.versions = versions;
  }

}
