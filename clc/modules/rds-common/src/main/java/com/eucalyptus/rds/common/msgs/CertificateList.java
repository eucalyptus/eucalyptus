/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CertificateList extends EucalyptusData {

  @HttpEmbedded(multiple = true)
  @HttpParameterMapping(parameter = "Certificate")
  private ArrayList<Certificate> member = new ArrayList<>();

  public ArrayList<Certificate> getMember() {
    return member;
  }

  public void setMember(final ArrayList<Certificate> member) {
    this.member = member;
  }
}
