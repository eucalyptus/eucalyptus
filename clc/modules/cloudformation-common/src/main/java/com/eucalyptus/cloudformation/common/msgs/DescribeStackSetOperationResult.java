/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeStackSetOperationResult extends EucalyptusData {

  private StackSetOperation stackSetOperation;

  public StackSetOperation getStackSetOperation() {
    return stackSetOperation;
  }

  public void setStackSetOperation(final StackSetOperation stackSetOperation) {
    this.stackSetOperation = stackSetOperation;
  }

}
