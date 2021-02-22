/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Certificate extends EucalyptusData {

  private String certificateArn;

  private String certificateIdentifier;

  private String certificateType;

  private Boolean customerOverride;

  private java.util.Date customerOverrideValidTill;

  private String thumbprint;

  private java.util.Date validFrom;

  private java.util.Date validTill;

  public String getCertificateArn() {
    return certificateArn;
  }

  public void setCertificateArn(final String certificateArn) {
    this.certificateArn = certificateArn;
  }

  public String getCertificateIdentifier() {
    return certificateIdentifier;
  }

  public void setCertificateIdentifier(final String certificateIdentifier) {
    this.certificateIdentifier = certificateIdentifier;
  }

  public String getCertificateType() {
    return certificateType;
  }

  public void setCertificateType(final String certificateType) {
    this.certificateType = certificateType;
  }

  public Boolean getCustomerOverride() {
    return customerOverride;
  }

  public void setCustomerOverride(final Boolean customerOverride) {
    this.customerOverride = customerOverride;
  }

  public java.util.Date getCustomerOverrideValidTill() {
    return customerOverrideValidTill;
  }

  public void setCustomerOverrideValidTill(final java.util.Date customerOverrideValidTill) {
    this.customerOverrideValidTill = customerOverrideValidTill;
  }

  public String getThumbprint() {
    return thumbprint;
  }

  public void setThumbprint(final String thumbprint) {
    this.thumbprint = thumbprint;
  }

  public java.util.Date getValidFrom() {
    return validFrom;
  }

  public void setValidFrom(final java.util.Date validFrom) {
    this.validFrom = validFrom;
  }

  public java.util.Date getValidTill() {
    return validTill;
  }

  public void setValidTill(final java.util.Date validTill) {
    this.validTill = validTill;
  }

}
