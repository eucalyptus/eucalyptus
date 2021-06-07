/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class FleetLaunchTemplateOverridesRequest extends EucalyptusData {

  private String availabilityZone;
  private String instanceType;
  private String maxPrice;
  private Double priority;
  private String subnetId;
  private Double weightedCapacity;

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( final String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( final String instanceType ) {
    this.instanceType = instanceType;
  }

  public String getMaxPrice( ) {
    return maxPrice;
  }

  public void setMaxPrice( final String maxPrice ) {
    this.maxPrice = maxPrice;
  }

  public Double getPriority( ) {
    return priority;
  }

  public void setPriority( final Double priority ) {
    this.priority = priority;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( final String subnetId ) {
    this.subnetId = subnetId;
  }

  public Double getWeightedCapacity( ) {
    return weightedCapacity;
  }

  public void setWeightedCapacity( final Double weightedCapacity ) {
    this.weightedCapacity = weightedCapacity;
  }

}
