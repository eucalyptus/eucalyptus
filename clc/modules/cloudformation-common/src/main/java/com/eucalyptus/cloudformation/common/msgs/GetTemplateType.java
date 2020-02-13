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


public class GetTemplateType extends CloudFormationMessage {

  @FieldRange(min = 1, max = 1600)
  private String changeSetName;

  private String stackName;

  @FieldRegex(FieldRegexValue.ENUM_TEMPLATESTAGE)
  private String templateStage;

  public String getChangeSetName() {
    return changeSetName;
  }

  public void setChangeSetName(final String changeSetName) {
    this.changeSetName = changeSetName;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
  }

  public String getTemplateStage() {
    return templateStage;
  }

  public void setTemplateStage(final String templateStage) {
    this.templateStage = templateStage;
  }

}
