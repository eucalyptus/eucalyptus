/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class StackDriftInformationSummary extends EucalyptusData {

  private java.util.Date lastCheckTimestamp;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_STACKDRIFTSTATUS)
  private String stackDriftStatus;

  public java.util.Date getLastCheckTimestamp() {
    return lastCheckTimestamp;
  }

  public void setLastCheckTimestamp(final java.util.Date lastCheckTimestamp) {
    this.lastCheckTimestamp = lastCheckTimestamp;
  }

  public String getStackDriftStatus() {
    return stackDriftStatus;
  }

  public void setStackDriftStatus(final String stackDriftStatus) {
    this.stackDriftStatus = stackDriftStatus;
  }

}
