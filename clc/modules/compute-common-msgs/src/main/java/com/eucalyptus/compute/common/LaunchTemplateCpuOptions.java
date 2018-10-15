/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LaunchTemplateCpuOptions extends EucalyptusData {

  private Integer coreCount;
  private Integer threadsPerCore;

  public Integer getCoreCount( ) {
    return coreCount;
  }

  public void setCoreCount( final Integer coreCount ) {
    this.coreCount = coreCount;
  }

  public Integer getThreadsPerCore( ) {
    return threadsPerCore;
  }

  public void setThreadsPerCore( final Integer threadsPerCore ) {
    this.threadsPerCore = threadsPerCore;
  }

}
