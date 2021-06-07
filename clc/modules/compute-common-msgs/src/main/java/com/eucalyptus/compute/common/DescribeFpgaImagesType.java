/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;


public class DescribeFpgaImagesType extends ComputeMessage {

  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );
  @HttpEmbedded
  private FpgaImageIdList fpgaImageIds;
  private Integer maxResults;
  private String nextToken;
  @HttpEmbedded
  private OwnerStringList owners;

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
  }

  public FpgaImageIdList getFpgaImageIds( ) {
    return fpgaImageIds;
  }

  public void setFpgaImageIds( final FpgaImageIdList fpgaImageIds ) {
    this.fpgaImageIds = fpgaImageIds;
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

  public OwnerStringList getOwners( ) {
    return owners;
  }

  public void setOwners( final OwnerStringList owners ) {
    this.owners = owners;
  }

}
