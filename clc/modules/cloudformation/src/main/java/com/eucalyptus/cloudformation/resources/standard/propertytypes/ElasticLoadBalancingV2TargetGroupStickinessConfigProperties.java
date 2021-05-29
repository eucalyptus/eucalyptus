/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;

public class ElasticLoadBalancingV2TargetGroupStickinessConfigProperties {

  @Property
  private Integer durationSeconds;

  @Property
  private Boolean enabled;

  public Integer getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds(Integer durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("durationSeconds", durationSeconds)
        .add("enabled", enabled)
        .toString();
  }
}
