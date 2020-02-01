/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;


public class GetTemplateSummaryType extends CloudFormationMessage {

  @FieldRange(min = 1)
  private String stackName;

  private String stackSetName;

  @FieldRange(min = 1)
  private String templateBody;

  @FieldRange(min = 1, max = 1024)
  private String templateURL;

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
  }

  public String getStackSetName() {
    return stackSetName;
  }

  public void setStackSetName(final String stackSetName) {
    this.stackSetName = stackSetName;
  }

  public String getTemplateBody() {
    return templateBody;
  }

  public void setTemplateBody(final String templateBody) {
    this.templateBody = templateBody;
  }

  public String getTemplateURL() {
    return templateURL;
  }

  public void setTemplateURL(final String templateURL) {
    this.templateURL = templateURL;
  }

}
