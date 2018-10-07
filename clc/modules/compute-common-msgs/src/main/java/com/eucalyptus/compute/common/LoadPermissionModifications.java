/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LoadPermissionModifications extends EucalyptusData {

  private LoadPermissionListRequest add;
  private LoadPermissionListRequest remove;

  public LoadPermissionListRequest getAdd( ) {
    return add;
  }

  public void setAdd( final LoadPermissionListRequest add ) {
    this.add = add;
  }

  public LoadPermissionListRequest getRemove( ) {
    return remove;
  }

  public void setRemove( final LoadPermissionListRequest remove ) {
    this.remove = remove;
  }

}
