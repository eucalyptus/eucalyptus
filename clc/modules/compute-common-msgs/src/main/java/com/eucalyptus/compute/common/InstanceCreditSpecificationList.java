/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class InstanceCreditSpecificationList extends EucalyptusData {

  private ArrayList<InstanceCreditSpecification> member = new ArrayList<InstanceCreditSpecification>();

  public ArrayList<InstanceCreditSpecification> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<InstanceCreditSpecification> member ) {
    this.member = member;
  }
}
