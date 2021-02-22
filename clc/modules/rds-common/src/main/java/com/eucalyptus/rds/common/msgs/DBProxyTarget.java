/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBProxyTarget extends EucalyptusData {

  private String endpoint;

  private Integer port;

  private String rdsResourceId;

  private String targetArn;

  private String trackedClusterId;

  @FieldRegex(FieldRegexValue.ENUM_TARGETTYPE)
  private String type;

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(final String endpoint) {
    this.endpoint = endpoint;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

  public String getRdsResourceId() {
    return rdsResourceId;
  }

  public void setRdsResourceId(final String rdsResourceId) {
    this.rdsResourceId = rdsResourceId;
  }

  public String getTargetArn() {
    return targetArn;
  }

  public void setTargetArn(final String targetArn) {
    this.targetArn = targetArn;
  }

  public String getTrackedClusterId() {
    return trackedClusterId;
  }

  public void setTrackedClusterId(final String trackedClusterId) {
    this.trackedClusterId = trackedClusterId;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

}
