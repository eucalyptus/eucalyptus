/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyCertificatesType extends RdsMessage {

  private String certificateIdentifier;

  private Boolean removeCustomerOverride;

  public String getCertificateIdentifier() {
    return certificateIdentifier;
  }

  public void setCertificateIdentifier(final String certificateIdentifier) {
    this.certificateIdentifier = certificateIdentifier;
  }

  public Boolean getRemoveCustomerOverride() {
    return removeCustomerOverride;
  }

  public void setRemoveCustomerOverride(final Boolean removeCustomerOverride) {
    this.removeCustomerOverride = removeCustomerOverride;
  }

}
