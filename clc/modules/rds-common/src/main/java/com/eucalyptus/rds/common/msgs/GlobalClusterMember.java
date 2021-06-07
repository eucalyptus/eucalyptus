/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class GlobalClusterMember extends EucalyptusData {

  private String dBClusterArn;

  private Boolean isWriter;

  private ReadersArnList readers;

  public String getDBClusterArn() {
    return dBClusterArn;
  }

  public void setDBClusterArn(final String dBClusterArn) {
    this.dBClusterArn = dBClusterArn;
  }

  public Boolean getIsWriter() {
    return isWriter;
  }

  public void setIsWriter(final Boolean isWriter) {
    this.isWriter = isWriter;
  }

  public ReadersArnList getReaders() {
    return readers;
  }

  public void setReaders(final ReadersArnList readers) {
    this.readers = readers;
  }

}
