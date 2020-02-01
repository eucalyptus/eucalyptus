/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class AccountGateResult extends EucalyptusData {

  @FieldRegex(FieldRegexValue.ENUM_ACCOUNTGATESTATUS)
  private String status;

  private String statusReason;

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public String getStatusReason() {
    return statusReason;
  }

  public void setStatusReason(final String statusReason) {
    this.statusReason = statusReason;
  }

}
