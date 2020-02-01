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


public class ValidateTemplateResult extends EucalyptusData {

  private Capabilities capabilities;

  private String capabilitiesReason;

  private TransformsList declaredTransforms;

  @FieldRange(min = 1, max = 1024)
  private String description;

  private TemplateParameters parameters;

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

  public TemplateParameters getParameters() {
    return parameters;
  }

  public void setParameters(final TemplateParameters parameters) {
    this.parameters = parameters;
  }

}
