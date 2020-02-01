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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ResourceToImport extends EucalyptusData {

  @Nonnull
  private String logicalResourceId;

  @Nonnull
  @FieldRange(min = 1, max = 256)
  private ResourceIdentifierProperties resourceIdentifier;

  @Nonnull
  @FieldRange(min = 1, max = 256)
  private String resourceType;

  public String getLogicalResourceId() {
    return logicalResourceId;
  }

  public void setLogicalResourceId(final String logicalResourceId) {
    this.logicalResourceId = logicalResourceId;
  }

  public ResourceIdentifierProperties getResourceIdentifier() {
    return resourceIdentifier;
  }

  public void setResourceIdentifier(final ResourceIdentifierProperties resourceIdentifier) {
    this.resourceIdentifier = resourceIdentifier;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(final String resourceType) {
    this.resourceType = resourceType;
  }

}
