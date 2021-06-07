/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class StatusReport extends EucalyptusData {

  private java.util.Date checkedTime;

  private String status;

  public java.util.Date getCheckedTime() {
    return checkedTime;
  }

  public void setCheckedTime(final java.util.Date checkedTime) {
    this.checkedTime = checkedTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
