/**
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class AWSEC2LaunchTemplateProperties implements ResourceProperties {

  @Property
  private String name;

  @Property
  private LaunchTemplateData launchTemplateData;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public LaunchTemplateData getLaunchTemplateData() {
    return launchTemplateData;
  }

  public void setLaunchTemplateData(final LaunchTemplateData launchTemplateData) {
    this.launchTemplateData = launchTemplateData;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("launchTemplateData", launchTemplateData)
        .toString();
  }
}