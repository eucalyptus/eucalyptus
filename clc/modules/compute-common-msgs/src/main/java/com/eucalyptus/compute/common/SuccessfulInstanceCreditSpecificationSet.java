/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class SuccessfulInstanceCreditSpecificationSet extends EucalyptusData {

  private ArrayList<SuccessfulInstanceCreditSpecificationItem> member = new ArrayList<SuccessfulInstanceCreditSpecificationItem>();

  public ArrayList<SuccessfulInstanceCreditSpecificationItem> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<SuccessfulInstanceCreditSpecificationItem> member ) {
    this.member = member;
  }
}
