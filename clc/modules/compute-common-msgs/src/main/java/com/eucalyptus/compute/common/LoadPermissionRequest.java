/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LoadPermissionRequest extends EucalyptusData {

  private String group;
  private String userId;

  public String getGroup( ) {
    return group;
  }

  public void setGroup( final String group ) {
    this.group = group;
  }

  public String getUserId( ) {
    return userId;
  }

  public void setUserId( final String userId ) {
    this.userId = userId;
  }

}
