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


public class ResourceChange extends EucalyptusData {

  @FieldRegex(FieldRegexValue.ENUM_CHANGEACTION)
  private String action;

  private ResourceChangeDetails details;

  private String logicalResourceId;

  private String physicalResourceId;

  @FieldRegex(FieldRegexValue.ENUM_REPLACEMENT)
  private String replacement;

  @FieldRange(min = 1, max = 256)
  private String resourceType;

  private Scope scope;

  public String getAction() {
    return action;
  }

  public void setAction(final String action) {
    this.action = action;
  }

  public ResourceChangeDetails getDetails() {
    return details;
  }

  public void setDetails(final ResourceChangeDetails details) {
    this.details = details;
  }

  public String getLogicalResourceId() {
    return logicalResourceId;
  }

  public void setLogicalResourceId(final String logicalResourceId) {
    this.logicalResourceId = logicalResourceId;
  }

  public String getPhysicalResourceId() {
    return physicalResourceId;
  }

  public void setPhysicalResourceId(final String physicalResourceId) {
    this.physicalResourceId = physicalResourceId;
  }

  public String getReplacement() {
    return replacement;
  }

  public void setReplacement(final String replacement) {
    this.replacement = replacement;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(final String resourceType) {
    this.resourceType = resourceType;
  }

  public Scope getScope() {
    return scope;
  }

  public void setScope(final Scope scope) {
    this.scope = scope;
  }

}
