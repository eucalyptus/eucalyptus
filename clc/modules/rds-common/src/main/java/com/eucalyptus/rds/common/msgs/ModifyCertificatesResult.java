/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ModifyCertificatesResult extends EucalyptusData {

  private Certificate certificate;

  public Certificate getCertificate() {
    return certificate;
  }

  public void setCertificate(final Certificate certificate) {
    this.certificate = certificate;
  }

}
