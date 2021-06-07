/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CreateOptionGroupType extends RdsMessage {

  @Nonnull
  private String engineName;

  @Nonnull
  private String majorEngineVersion;

  @Nonnull
  private String optionGroupDescription;

  @Nonnull
  private String optionGroupName;

  private TagList tags;

  public String getEngineName() {
    return engineName;
  }

  public void setEngineName(final String engineName) {
    this.engineName = engineName;
  }

  public String getMajorEngineVersion() {
    return majorEngineVersion;
  }

  public void setMajorEngineVersion(final String majorEngineVersion) {
    this.majorEngineVersion = majorEngineVersion;
  }

  public String getOptionGroupDescription() {
    return optionGroupDescription;
  }

  public void setOptionGroupDescription(final String optionGroupDescription) {
    this.optionGroupDescription = optionGroupDescription;
  }

  public String getOptionGroupName() {
    return optionGroupName;
  }

  public void setOptionGroupName(final String optionGroupName) {
    this.optionGroupName = optionGroupName;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

}
