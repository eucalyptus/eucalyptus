/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ResourceIdentifierSummary extends EucalyptusData {

  @FieldRange(min = 1, max = 200)
  private LogicalResourceIds logicalResourceIds;

  private ResourceIdentifiers resourceIdentifiers;

  @FieldRange(min = 1, max = 256)
  private String resourceType;

  public LogicalResourceIds getLogicalResourceIds() {
    return logicalResourceIds;
  }

  public void setLogicalResourceIds(final LogicalResourceIds logicalResourceIds) {
    this.logicalResourceIds = logicalResourceIds;
  }

  public ResourceIdentifiers getResourceIdentifiers() {
    return resourceIdentifiers;
  }

  public void setResourceIdentifiers(final ResourceIdentifiers resourceIdentifiers) {
    this.resourceIdentifiers = resourceIdentifiers;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(final String resourceType) {
    this.resourceType = resourceType;
  }

}
