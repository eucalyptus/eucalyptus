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


public class IdFormatList extends EucalyptusData {

  private ArrayList<IdFormatItemType> member = new ArrayList<IdFormatItemType>();

  public ArrayList<IdFormatItemType> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<IdFormatItemType> member ) {
    this.member = member;
  }
}
