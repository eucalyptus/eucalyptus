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
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;

public class DescribeReservedInstancesOfferingsType extends ReservedInstanceMessage {

  private ArrayList<String> reservedInstancesOfferingId = new ArrayList<String>( );
  private String availabilityZone;
  private String instanceTenancy;
  private String instanceType;
  private String offeringType;
  private String productDescription;
  private Integer maxInstanceCount;
  private Boolean includeMarketplace;
  private Integer maxResults;
  private String nextToken;
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );

  public ArrayList<String> getReservedInstancesOfferingId( ) {
    return reservedInstancesOfferingId;
  }

  public void setReservedInstancesOfferingId( ArrayList<String> reservedInstancesOfferingId ) {
    this.reservedInstancesOfferingId = reservedInstancesOfferingId;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getInstanceTenancy( ) {
    return instanceTenancy;
  }

  public void setInstanceTenancy( String instanceTenancy ) {
    this.instanceTenancy = instanceTenancy;
  }

  public String getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( String instanceType ) {
    this.instanceType = instanceType;
  }

  public String getOfferingType( ) {
    return offeringType;
  }

  public void setOfferingType( String offeringType ) {
    this.offeringType = offeringType;
  }

  public String getProductDescription( ) {
    return productDescription;
  }

  public void setProductDescription( String productDescription ) {
    this.productDescription = productDescription;
  }

  public Integer getMaxInstanceCount( ) {
    return maxInstanceCount;
  }

  public void setMaxInstanceCount( Integer maxInstanceCount ) {
    this.maxInstanceCount = maxInstanceCount;
  }

  public Boolean getIncludeMarketplace( ) {
    return includeMarketplace;
  }

  public void setIncludeMarketplace( Boolean includeMarketplace ) {
    this.includeMarketplace = includeMarketplace;
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
