/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;


public class StartActivityStreamType extends RdsMessage {

  private Boolean applyImmediately;

  @Nonnull
  private String kmsKeyId;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_ACTIVITYSTREAMMODE)
  private String mode;

  @Nonnull
  private String resourceArn;

  public Boolean getApplyImmediately() {
    return applyImmediately;
  }

  public void setApplyImmediately(final Boolean applyImmediately) {
    this.applyImmediately = applyImmediately;
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

  public String getResourceArn() {
    return resourceArn;
  }

  public void setResourceArn(final String resourceArn) {
    this.resourceArn = resourceArn;
  }

}
