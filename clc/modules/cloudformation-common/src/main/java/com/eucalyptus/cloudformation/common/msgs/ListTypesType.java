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


public class ListTypesType extends CloudFormationMessage {

  @FieldRegex(FieldRegexValue.ENUM_DEPRECATEDSTATUS)
  private String deprecatedStatus;

  @FieldRange(min = 1, max = 100)
  private Integer maxResults;

  @FieldRange(min = 1, max = 1024)
  private String nextToken;

  @FieldRegex(FieldRegexValue.ENUM_PROVISIONINGTYPE)
  private String provisioningType;

  @FieldRegex(FieldRegexValue.ENUM_VISIBILITY)
  private String visibility;

  public String getDeprecatedStatus() {
    return deprecatedStatus;
  }

  public void setDeprecatedStatus(final String deprecatedStatus) {
    this.deprecatedStatus = deprecatedStatus;
  }

  public Integer getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(final Integer maxResults) {
    this.maxResults = maxResults;
  }

  public String getNextToken() {
    return nextToken;
  }

  public void setNextToken(final String nextToken) {
    this.nextToken = nextToken;
  }

  public String getProvisioningType() {
    return provisioningType;
  }

  public void setProvisioningType(final String provisioningType) {
    this.provisioningType = provisioningType;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(final String visibility) {
    this.visibility = visibility;
  }

}
