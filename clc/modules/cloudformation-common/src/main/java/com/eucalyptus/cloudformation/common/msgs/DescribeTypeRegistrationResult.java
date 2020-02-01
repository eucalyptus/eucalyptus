/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeTypeRegistrationResult extends EucalyptusData {

  @FieldRange(min = 1, max = 1024)
  private String description;

  @FieldRegex(FieldRegexValue.ENUM_REGISTRATIONSTATUS)
  private String progressStatus;

  @FieldRange(max = 1024)
  private String typeArn;

  @FieldRange(max = 1024)
  private String typeVersionArn;

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getProgressStatus() {
    return progressStatus;
  }

  public void setProgressStatus(final String progressStatus) {
    this.progressStatus = progressStatus;
  }

  public String getTypeArn() {
    return typeArn;
  }

  public void setTypeArn(final String typeArn) {
    this.typeArn = typeArn;
  }

  public String getTypeVersionArn() {
    return typeVersionArn;
  }

  public void setTypeVersionArn(final String typeVersionArn) {
    this.typeVersionArn = typeVersionArn;
  }

}
