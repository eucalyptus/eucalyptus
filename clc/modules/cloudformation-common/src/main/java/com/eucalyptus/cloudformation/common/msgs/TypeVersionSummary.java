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


public class TypeVersionSummary extends EucalyptusData {

  @FieldRange(max = 1024)
  private String arn;

  @FieldRange(min = 1, max = 1024)
  private String description;

  private java.util.Date timeCreated;

  @FieldRegex(FieldRegexValue.ENUM_REGISTRYTYPE)
  private String type;

  @FieldRange(min = 10, max = 196)
  private String typeName;

  @FieldRange(min = 1, max = 128)
  private String versionId;

  public String getArn() {
    return arn;
  }

  public void setArn(final String arn) {
    this.arn = arn;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public java.util.Date getTimeCreated() {
    return timeCreated;
  }

  public void setTimeCreated(final java.util.Date timeCreated) {
    this.timeCreated = timeCreated;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(final String typeName) {
    this.typeName = typeName;
  }

  public String getVersionId() {
    return versionId;
  }

  public void setVersionId(final String versionId) {
    this.versionId = versionId;
  }

}
