/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TargetDescription extends EucalyptusData {

  private String availabilityZone;

  @Nonnull
  private String id;

  @FieldRange(min = 1, max = 65535)
  private Integer port;

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  public void setAvailabilityZone(final String availabilityZone) {
    this.availabilityZone = availabilityZone;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

}
