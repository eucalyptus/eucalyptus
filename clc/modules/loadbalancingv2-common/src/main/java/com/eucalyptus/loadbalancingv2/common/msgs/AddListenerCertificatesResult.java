/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class AddListenerCertificatesResult extends EucalyptusData {

  private CertificateList certificates;

  public CertificateList getCertificates() {
    return certificates;
  }

  public void setCertificates(final CertificateList certificates) {
    this.certificates = certificates;
  }

}
