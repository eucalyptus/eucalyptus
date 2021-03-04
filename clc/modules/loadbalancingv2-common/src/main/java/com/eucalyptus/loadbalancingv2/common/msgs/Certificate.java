/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Certificate extends EucalyptusData {

  private String certificateArn;

  private Boolean isDefault;

  public String getCertificateArn() {
    return certificateArn;
  }

  public void setCertificateArn(final String certificateArn) {
    this.certificateArn = certificateArn;
  }

  public Boolean getIsDefault() {
    return isDefault;
  }

  public void setIsDefault(final Boolean isDefault) {
    this.isDefault = isDefault;
  }

}
