/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CreditSpecification extends EucalyptusData {

  private String cpuCredits;

  public String getCpuCredits( ) {
    return cpuCredits;
  }

  public void setCpuCredits( final String cpuCredits ) {
    this.cpuCredits = cpuCredits;
  }

}
