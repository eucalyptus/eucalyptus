/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TargetGroupStickinessConfig extends EucalyptusData {

  @FieldRange(min = 1, max = 604800)
  private Integer durationSeconds;

  private Boolean enabled;

  public Integer getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds(final Integer durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(final Boolean enabled) {
    this.enabled = enabled;
  }

}
