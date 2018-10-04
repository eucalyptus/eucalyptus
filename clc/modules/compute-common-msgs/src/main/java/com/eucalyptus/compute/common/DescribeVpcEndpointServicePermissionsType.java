/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;


public class DescribeVpcEndpointServicePermissionsType extends VpcMessage {

  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );
  private Integer maxResults;
  private String nextToken;
  @Nonnull
  private String serviceId;

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
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

  public String getServiceId( ) {
    return serviceId;
  }

  public void setServiceId( final String serviceId ) {
    this.serviceId = serviceId;
  }

}
