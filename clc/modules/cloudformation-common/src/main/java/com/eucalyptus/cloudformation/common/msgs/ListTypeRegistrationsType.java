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


public class ListTypeRegistrationsType extends CloudFormationMessage {

  @FieldRange(min = 1, max = 100)
  private Integer maxResults;

  @FieldRange(min = 1, max = 1024)
  private String nextToken;

  @FieldRegex(FieldRegexValue.ENUM_REGISTRATIONSTATUS)
  private String registrationStatusFilter;

  @FieldRegex(FieldRegexValue.ENUM_REGISTRYTYPE)
  private String type;

  @FieldRange(max = 1024)
  private String typeArn;

  @FieldRange(min = 10, max = 196)
  private String typeName;

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

  public String getRegistrationStatusFilter() {
    return registrationStatusFilter;
  }

  public void setRegistrationStatusFilter(final String registrationStatusFilter) {
    this.registrationStatusFilter = registrationStatusFilter;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getTypeArn() {
    return typeArn;
  }

  public void setTypeArn(final String typeArn) {
    this.typeArn = typeArn;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(final String typeName) {
    this.typeName = typeName;
  }

}
