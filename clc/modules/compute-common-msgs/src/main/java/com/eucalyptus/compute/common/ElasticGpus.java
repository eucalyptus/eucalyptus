/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ElasticGpus extends EucalyptusData {

  private String availabilityZone;
  private ElasticGpuHealth elasticGpuHealth;
  private String elasticGpuId;
  private String elasticGpuState;
  private String elasticGpuType;
  private String instanceId;

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( final String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public ElasticGpuHealth getElasticGpuHealth( ) {
    return elasticGpuHealth;
  }

  public void setElasticGpuHealth( final ElasticGpuHealth elasticGpuHealth ) {
    this.elasticGpuHealth = elasticGpuHealth;
  }

  public String getElasticGpuId( ) {
    return elasticGpuId;
  }

  public void setElasticGpuId( final String elasticGpuId ) {
    this.elasticGpuId = elasticGpuId;
  }

  public String getElasticGpuState( ) {
    return elasticGpuState;
  }

  public void setElasticGpuState( final String elasticGpuState ) {
    this.elasticGpuState = elasticGpuState;
  }

  public String getElasticGpuType( ) {
    return elasticGpuType;
  }

  public void setElasticGpuType( final String elasticGpuType ) {
    this.elasticGpuType = elasticGpuType;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

}
