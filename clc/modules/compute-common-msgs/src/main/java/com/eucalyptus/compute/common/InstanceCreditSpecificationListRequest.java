/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class InstanceCreditSpecificationListRequest extends EucalyptusData {

  private ArrayList<InstanceCreditSpecificationRequest> member = new ArrayList<InstanceCreditSpecificationRequest>();

  public ArrayList<InstanceCreditSpecificationRequest> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<InstanceCreditSpecificationRequest> member ) {
    this.member = member;
  }
}
