/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeListenerCertificatesResult extends EucalyptusData {

  private CertificateList certificates;

  private String nextMarker;

  public CertificateList getCertificates() {
    return certificates;
  }

  public void setCertificates(final CertificateList certificates) {
    this.certificates = certificates;
  }

  public String getNextMarker() {
    return nextMarker;
  }

  public void setNextMarker(final String nextMarker) {
    this.nextMarker = nextMarker;
  }

}
