/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class AccountLimitList extends EucalyptusData {

  private ArrayList<AccountLimit> member = new ArrayList<>();

  public ArrayList<AccountLimit> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<AccountLimit> member ) {
    this.member = member;
  }
}
