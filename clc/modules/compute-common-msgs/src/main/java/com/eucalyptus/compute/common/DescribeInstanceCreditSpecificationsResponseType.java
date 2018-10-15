/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeInstanceCreditSpecificationsResponseType extends ComputeMessage {


  private InstanceCreditSpecificationList instanceCreditSpecifications;
  private String nextToken;

  public InstanceCreditSpecificationList getInstanceCreditSpecifications( ) {
    return instanceCreditSpecifications;
  }

  public void setInstanceCreditSpecifications( final InstanceCreditSpecificationList instanceCreditSpecifications ) {
    this.instanceCreditSpecifications = instanceCreditSpecifications;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

}
