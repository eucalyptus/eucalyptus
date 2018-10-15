/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class InstanceCreditSpecification extends EucalyptusData {

  private String cpuCredits;
  private String instanceId;

  public String getCpuCredits( ) {
    return cpuCredits;
  }

  public void setCpuCredits( final String cpuCredits ) {
    this.cpuCredits = cpuCredits;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

}
