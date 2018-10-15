/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


import com.eucalyptus.binding.HttpEmbedded;

public class DescribePrincipalIdFormatType extends ComputeMessage {

  private Integer maxResults;
  private String nextToken;
  @HttpEmbedded
  private ResourceList resources;

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

  public ResourceList getResources( ) {
    return resources;
  }

  public void setResources( final ResourceList resources ) {
    this.resources = resources;
  }

}
