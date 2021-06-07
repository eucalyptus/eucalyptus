/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LoggingConfig extends EucalyptusData {

  @Nonnull
  @FieldRange(min = 1, max = 512)
  private String logGroupName;

  @Nonnull
  @FieldRange(min = 1, max = 256)
  private String logRoleArn;

  public String getLogGroupName() {
    return logGroupName;
  }

  public void setLogGroupName(final String logGroupName) {
    this.logGroupName = logGroupName;
  }

  public String getLogRoleArn() {
    return logRoleArn;
  }

  public void setLogRoleArn(final String logRoleArn) {
    this.logRoleArn = logRoleArn;
  }

}
