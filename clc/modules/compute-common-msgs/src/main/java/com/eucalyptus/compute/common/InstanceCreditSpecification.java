/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
