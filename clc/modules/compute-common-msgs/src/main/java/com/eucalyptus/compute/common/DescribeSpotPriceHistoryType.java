/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import java.util.Date;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;

public class DescribeSpotPriceHistoryType extends SpotInstanceMessage {

  private Date startTime;
  private Date endTime;
  private String availabilityZone;
  private ArrayList<String> instanceType;
  private ArrayList<String> productDescription;
  private Integer maxResults;
  private String nextToken;
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );

  public Date getStartTime( ) {
    return startTime;
  }

  public void setStartTime( Date startTime ) {
    this.startTime = startTime;
  }

  public Date getEndTime( ) {
    return endTime;
  }

  public void setEndTime( Date endTime ) {
    this.endTime = endTime;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public ArrayList<String> getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( ArrayList<String> instanceType ) {
    this.instanceType = instanceType;
  }

  public ArrayList<String> getProductDescription( ) {
    return productDescription;
  }

  public void setProductDescription( ArrayList<String> productDescription ) {
    this.productDescription = productDescription;
  }

  public Integer getMaxResults( ) {
    return maxResults;
  }

  public void setMaxResults( Integer maxResults ) {
    this.maxResults = maxResults;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( String nextToken ) {
    this.nextToken = nextToken;
  }

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
  }
}
