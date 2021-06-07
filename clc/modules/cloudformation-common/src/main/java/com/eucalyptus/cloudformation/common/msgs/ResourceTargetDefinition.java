/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ResourceTargetDefinition extends EucalyptusData {

  @FieldRegex(FieldRegexValue.ENUM_RESOURCEATTRIBUTE)
  private String attribute;

  private String name;

  @FieldRegex(FieldRegexValue.ENUM_REQUIRESRECREATION)
  private String requiresRecreation;

  public String getAttribute() {
    return attribute;
  }

  public void setAttribute(final String attribute) {
    this.attribute = attribute;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getRequiresRecreation() {
    return requiresRecreation;
  }

  public void setRequiresRecreation(final String requiresRecreation) {
    this.requiresRecreation = requiresRecreation;
  }

}
