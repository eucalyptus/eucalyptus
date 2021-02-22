/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBInstanceStatusInfo extends EucalyptusData {

  private String message;

  private Boolean normal;

  private String status;

  private String statusType;

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public Boolean getNormal() {
    return normal;
  }

  public void setNormal(final Boolean normal) {
    this.normal = normal;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public String getStatusType() {
    return statusType;
  }

  public void setStatusType(final String statusType) {
    this.statusType = statusType;
  }

}
