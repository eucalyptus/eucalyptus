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


public class GetTemplateSummaryResult extends EucalyptusData {

  private Capabilities capabilities;

  private String capabilitiesReason;

  private TransformsList declaredTransforms;

  @FieldRange(min = 1, max = 1024)
  private String description;

  private String metadata;

  private ParameterDeclarations parameters;

  private ResourceIdentifierSummaries resourceIdentifierSummaries;

  private ResourceTypes resourceTypes;

  private String version;

  public Capabilities getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(final Capabilities capabilities) {
    this.capabilities = capabilities;
  }

  public String getCapabilitiesReason() {
    return capabilitiesReason;
  }

  public void setCapabilitiesReason(final String capabilitiesReason) {
    this.capabilitiesReason = capabilitiesReason;
  }

  public TransformsList getDeclaredTransforms() {
    return declaredTransforms;
  }

  public void setDeclaredTransforms(final TransformsList declaredTransforms) {
    this.declaredTransforms = declaredTransforms;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(final String metadata) {
    this.metadata = metadata;
  }

  public ParameterDeclarations getParameters() {
    return parameters;
  }

  public void setParameters(final ParameterDeclarations parameters) {
    this.parameters = parameters;
  }

  public ResourceIdentifierSummaries getResourceIdentifierSummaries() {
    return resourceIdentifierSummaries;
  }

  public void setResourceIdentifierSummaries(final ResourceIdentifierSummaries resourceIdentifierSummaries) {
    this.resourceIdentifierSummaries = resourceIdentifierSummaries;
  }

  public ResourceTypes getResourceTypes() {
    return resourceTypes;
  }

  public void setResourceTypes(final ResourceTypes resourceTypes) {
    this.resourceTypes = resourceTypes;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

}
