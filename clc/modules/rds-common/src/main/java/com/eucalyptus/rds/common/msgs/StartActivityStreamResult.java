/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class StartActivityStreamResult extends EucalyptusData {

  private Boolean applyImmediately;

  private String kinesisStreamName;

  private String kmsKeyId;

  @FieldRegex(FieldRegexValue.ENUM_ACTIVITYSTREAMMODE)
  private String mode;

  @FieldRegex(FieldRegexValue.ENUM_ACTIVITYSTREAMSTATUS)
  private String status;

  public Boolean getApplyImmediately() {
    return applyImmediately;
  }

  public void setApplyImmediately(final Boolean applyImmediately) {
    this.applyImmediately = applyImmediately;
  }

  public String getKinesisStreamName() {
    return kinesisStreamName;
  }

  public void setKinesisStreamName(final String kinesisStreamName) {
    this.kinesisStreamName = kinesisStreamName;
  }

  public String getKmsKeyId() {
    return kmsKeyId;
  }

  public void setKmsKeyId(final String kmsKeyId) {
    this.kmsKeyId = kmsKeyId;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(final String mode) {
    this.mode = mode;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
