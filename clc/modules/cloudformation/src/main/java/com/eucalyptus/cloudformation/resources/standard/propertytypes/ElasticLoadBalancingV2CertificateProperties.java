/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;

public class ElasticLoadBalancingV2CertificateProperties {

  @Property
  private String certificateArn;

  public String getCertificateArn() {
    return certificateArn;
  }

  public void setCertificateArn(String certificateArn) {
    this.certificateArn = certificateArn;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("certificateArn", certificateArn)
        .toString();
  }
}
