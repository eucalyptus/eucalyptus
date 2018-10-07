/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DeleteFleetSuccessSet extends EucalyptusData {

  private ArrayList<DeleteFleetSuccessItem> member = new ArrayList<DeleteFleetSuccessItem>();

  public ArrayList<DeleteFleetSuccessItem> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<DeleteFleetSuccessItem> member ) {
    this.member = member;
  }
}
