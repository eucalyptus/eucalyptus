/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;


public class GetTrafficPolicyResponseType extends Route53Message {


  @Nonnull
  private TrafficPolicy trafficPolicy;

  public TrafficPolicy getTrafficPolicy() {
    return trafficPolicy;
  }

  public void setTrafficPolicy(final TrafficPolicy trafficPolicy) {
    this.trafficPolicy = trafficPolicy;
  }

}
