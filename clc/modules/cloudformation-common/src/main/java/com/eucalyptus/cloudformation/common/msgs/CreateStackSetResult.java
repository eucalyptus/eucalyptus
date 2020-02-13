/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CreateStackSetResult extends EucalyptusData {

  private String stackSetId;

  public String getStackSetId() {
    return stackSetId;
  }

  public void setStackSetId(final String stackSetId) {
    this.stackSetId = stackSetId;
  }

}
