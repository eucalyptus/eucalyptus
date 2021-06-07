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


public class TypeSummary extends EucalyptusData {

  @FieldRange(min = 1, max = 128)
  private String defaultVersionId;

  @FieldRange(min = 1, max = 1024)
  private String description;

  private java.util.Date lastUpdated;

  @FieldRegex(FieldRegexValue.ENUM_REGISTRYTYPE)
  private String type;

  @FieldRange(max = 1024)
  private String typeArn;

  @FieldRange(min = 10, max = 196)
  private String typeName;

  public String getDefaultVersionId() {
    return defaultVersionId;
  }

  public void setDefaultVersionId(final String defaultVersionId) {
    this.defaultVersionId = defaultVersionId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public java.util.Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(final java.util.Date lastUpdated) {
    this.lastUpdated = lastUpdated;
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
