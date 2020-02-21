/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpHeaderMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


public class CreateTrafficPolicyInstanceResponseType extends Route53Message {


  @Nonnull
  @HttpHeaderMapping(header = "Location")
  @FieldRange(max = 1024)
  private String location;

  @Nonnull
  private TrafficPolicyInstance trafficPolicyInstance;

  public String getLocation() {
    return location;
  }

  public void setLocation(final String location) {
    this.location = location;
  }

  public TrafficPolicyInstance getTrafficPolicyInstance() {
    return trafficPolicyInstance;
  }

  public void setTrafficPolicyInstance(final TrafficPolicyInstance trafficPolicyInstance) {
    this.trafficPolicyInstance = trafficPolicyInstance;
  }

}
