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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class StackEvent extends EucalyptusData {

  @FieldRange(min = 1, max = 128)
  private String clientRequestToken;

  @Nonnull
  private String eventId;

  private String logicalResourceId;

  private String physicalResourceId;

  private String resourceProperties;

  @FieldRegex(FieldRegexValue.ENUM_RESOURCESTATUS)
  private String resourceStatus;

  private String resourceStatusReason;

  @FieldRange(min = 1, max = 256)
  private String resourceType;

  @Nonnull
  private String stackId;

  @Nonnull
  private String stackName;

  @Nonnull
  private java.util.Date timestamp;

  public String getClientRequestToken() {
    return clientRequestToken;
  }

  public void setClientRequestToken(final String clientRequestToken) {
    this.clientRequestToken = clientRequestToken;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(final String eventId) {
    this.eventId = eventId;
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

  public String getResourceProperties() {
    return resourceProperties;
  }

  public void setResourceProperties(final String resourceProperties) {
    this.resourceProperties = resourceProperties;
  }

  public String getResourceStatus() {
    return resourceStatus;
  }

  public void setResourceStatus(final String resourceStatus) {
    this.resourceStatus = resourceStatus;
  }

  public String getResourceStatusReason() {
    return resourceStatusReason;
  }

  public void setResourceStatusReason(final String resourceStatusReason) {
    this.resourceStatusReason = resourceStatusReason;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(final String resourceType) {
    this.resourceType = resourceType;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(final String stackId) {
    this.stackId = stackId;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(final String stackName) {
    this.stackName = stackName;
  }

  public java.util.Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final java.util.Date timestamp) {
    this.timestamp = timestamp;
  }

}
