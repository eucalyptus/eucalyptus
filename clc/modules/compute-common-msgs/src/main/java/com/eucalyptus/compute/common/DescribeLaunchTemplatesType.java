/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;


public class DescribeLaunchTemplatesType extends ComputeMessage {

  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );
  @HttpParameterMapping( parameter = "LaunchTemplateId" )
  private ValueStringList launchTemplateIds;
  @HttpEmbedded
  private LaunchTemplateNameStringList launchTemplateNames;
  private Integer maxResults;
  private String nextToken;

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
  }

  public ValueStringList getLaunchTemplateIds( ) {
    return launchTemplateIds;
  }

  public void setLaunchTemplateIds( final ValueStringList launchTemplateIds ) {
    this.launchTemplateIds = launchTemplateIds;
  }

  public LaunchTemplateNameStringList getLaunchTemplateNames( ) {
    return launchTemplateNames;
  }

  public void setLaunchTemplateNames( final LaunchTemplateNameStringList launchTemplateNames ) {
    this.launchTemplateNames = launchTemplateNames;
  }

  public Integer getMaxResults( ) {
    return maxResults;
  }

  public void setMaxResults( final Integer maxResults ) {
    this.maxResults = maxResults;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

}
