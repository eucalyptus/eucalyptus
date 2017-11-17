/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
