/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import com.eucalyptus.compute.common.IdFormatList;


public class PrincipalIdFormat extends EucalyptusData {

  private String arn;
  private IdFormatList statuses;

  public String getArn( ) {
    return arn;
  }

  public void setArn( final String arn ) {
    this.arn = arn;
  }

  public IdFormatList getStatuses( ) {
    return statuses;
  }

  public void setStatuses( final IdFormatList statuses ) {
    this.statuses = statuses;
  }

}
