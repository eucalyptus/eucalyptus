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


public class LaunchTemplateSet extends EucalyptusData {

  private ArrayList<LaunchTemplate> member = new ArrayList<LaunchTemplate>();

  public ArrayList<LaunchTemplate> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LaunchTemplate> member ) {
    this.member = member;
  }
}
