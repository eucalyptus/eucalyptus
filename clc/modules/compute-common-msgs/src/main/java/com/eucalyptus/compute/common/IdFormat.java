/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class IdFormat extends EucalyptusData {

  private java.util.Date deadline;
  private String resource;
  private Boolean useLongIds;

  public java.util.Date getDeadline( ) {
    return deadline;
  }

  public void setDeadline( final java.util.Date deadline ) {
    this.deadline = deadline;
  }

  public String getResource( ) {
    return resource;
  }

  public void setResource( final String resource ) {
    this.resource = resource;
  }

  public Boolean getUseLongIds( ) {
    return useLongIds;
  }

  public void setUseLongIds( final Boolean useLongIds ) {
    this.useLongIds = useLongIds;
  }

}
