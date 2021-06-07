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


public class StackResourceSummary extends EucalyptusData {

  private StackResourceDriftInformationSummary driftInformation;

  @Nonnull
  private java.util.Date lastUpdatedTimestamp;

  @Nonnull
  private String logicalResourceId;

  private String physicalResourceId;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_RESOURCESTATUS)
  private String resourceStatus;

  private String resourceStatusReason;

  @Nonnull
  @FieldRange(min = 1, max = 256)
  private String resourceType;

  public StackResourceDriftInformationSummary getDriftInformation() {
    return driftInformation;
  }

  public void setDriftInformation(final StackResourceDriftInformationSummary driftInformation) {
    this.driftInformation = driftInformation;
  }

  public java.util.Date getLastUpdatedTimestamp() {
    return lastUpdatedTimestamp;
  }

  public void setLastUpdatedTimestamp(final java.util.Date lastUpdatedTimestamp) {
    this.lastUpdatedTimestamp = lastUpdatedTimestamp;
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

}
