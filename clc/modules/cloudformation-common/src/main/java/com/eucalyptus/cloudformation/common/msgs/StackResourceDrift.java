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


public class StackResourceDrift extends EucalyptusData {

  private String actualProperties;

  private String expectedProperties;

  @Nonnull
  private String logicalResourceId;

  private String physicalResourceId;

  @FieldRange(max = 5)
  private PhysicalResourceIdContext physicalResourceIdContext;

  private PropertyDifferences propertyDifferences;

  @Nonnull
  @FieldRange(min = 1, max = 256)
  private String resourceType;

  @Nonnull
  private String stackId;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_STACKRESOURCEDRIFTSTATUS)
  private String stackResourceDriftStatus;

  @Nonnull
  private java.util.Date timestamp;

  public String getActualProperties() {
    return actualProperties;
  }

  public void setActualProperties(final String actualProperties) {
    this.actualProperties = actualProperties;
  }

  public String getExpectedProperties() {
    return expectedProperties;
  }

  public void setExpectedProperties(final String expectedProperties) {
    this.expectedProperties = expectedProperties;
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

  public PhysicalResourceIdContext getPhysicalResourceIdContext() {
    return physicalResourceIdContext;
  }

  public void setPhysicalResourceIdContext(final PhysicalResourceIdContext physicalResourceIdContext) {
    this.physicalResourceIdContext = physicalResourceIdContext;
  }

  public PropertyDifferences getPropertyDifferences() {
    return propertyDifferences;
  }

  public void setPropertyDifferences(final PropertyDifferences propertyDifferences) {
    this.propertyDifferences = propertyDifferences;
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

  public String getStackResourceDriftStatus() {
    return stackResourceDriftStatus;
  }

  public void setStackResourceDriftStatus(final String stackResourceDriftStatus) {
    this.stackResourceDriftStatus = stackResourceDriftStatus;
  }

  public java.util.Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final java.util.Date timestamp) {
    this.timestamp = timestamp;
  }

}
