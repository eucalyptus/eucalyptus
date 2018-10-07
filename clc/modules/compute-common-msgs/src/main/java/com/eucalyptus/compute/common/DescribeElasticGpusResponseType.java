/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeElasticGpusResponseType extends ComputeMessage {


  private ElasticGpuSet elasticGpuSet;
  private Integer maxResults;
  private String nextToken;

  public ElasticGpuSet getElasticGpuSet( ) {
    return elasticGpuSet;
  }

  public void setElasticGpuSet( final ElasticGpuSet elasticGpuSet ) {
    this.elasticGpuSet = elasticGpuSet;
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
