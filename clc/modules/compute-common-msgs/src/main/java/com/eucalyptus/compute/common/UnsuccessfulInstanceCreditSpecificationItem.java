/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class UnsuccessfulInstanceCreditSpecificationItem extends EucalyptusData {

  private UnsuccessfulInstanceCreditSpecificationItemError error;
  private String instanceId;

  public UnsuccessfulInstanceCreditSpecificationItemError getError( ) {
    return error;
  }

  public void setError( final UnsuccessfulInstanceCreditSpecificationItemError error ) {
    this.error = error;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

}
