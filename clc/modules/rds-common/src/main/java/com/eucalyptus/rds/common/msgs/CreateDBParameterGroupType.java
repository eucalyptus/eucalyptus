/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;
import javax.annotation.Nonnull;


public class CreateDBParameterGroupType extends RdsMessage {

  @Nonnull
  @FieldRegex(FieldRegexValue.STRING_64)
  private String dBParameterGroupFamily;

  @Nonnull
  @FieldRegex(FieldRegexValue.RDS_DB_PARAMETER_GROUP_NAME)
  private String dBParameterGroupName;

  @Nonnull
  @FieldRegex(FieldRegexValue.ESTRING_1024)
  private String description;

  private TagList tags;

  public String getDBParameterGroupFamily() {
    return dBParameterGroupFamily;
  }

  public void setDBParameterGroupFamily(final String dBParameterGroupFamily) {
    this.dBParameterGroupFamily = dBParameterGroupFamily;
  }

  public String getDBParameterGroupName() {
    return dBParameterGroupName;
  }

  public void setDBParameterGroupName(final String dBParameterGroupName) {
    this.dBParameterGroupName = dBParameterGroupName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

}
