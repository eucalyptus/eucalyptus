/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
