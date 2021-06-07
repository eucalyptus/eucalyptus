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
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;


public class SignalResourceType extends CloudFormationMessage {

  @Nonnull
  private String logicalResourceId;

  @Nonnull
  @FieldRange(min = 1)
  private String stackName;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_RESOURCESIGNALSTATUS)
  private String status;

  @Nonnull
  @FieldRange(min = 1, max = 64)
  private String uniqueId;

  public String getLogicalResourceId() {
    return logicalResourceId;
  }

  public void setLogicalResourceId(final String logicalResourceId) {
    this.logicalResourceId = logicalResourceId;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(final String uniqueId) {
    this.uniqueId = uniqueId;
  }

}
