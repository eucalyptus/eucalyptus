/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


public class ListTrafficPolicyVersionsResponseType extends Route53Message {


  @Nonnull
  private Boolean isTruncated;

  @Nonnull
  private String maxItems;

  @Nonnull
  private TrafficPolicies trafficPolicies;

  @Nonnull
  @FieldRange(max = 4)
  private String trafficPolicyVersionMarker;

  public Boolean getIsTruncated() {
    return isTruncated;
  }

  public void setIsTruncated(final Boolean isTruncated) {
    this.isTruncated = isTruncated;
  }

  public String getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(final String maxItems) {
    this.maxItems = maxItems;
  }

  public TrafficPolicies getTrafficPolicies() {
    return trafficPolicies;
  }

  public void setTrafficPolicies(final TrafficPolicies trafficPolicies) {
    this.trafficPolicies = trafficPolicies;
  }

  public String getTrafficPolicyVersionMarker() {
    return trafficPolicyVersionMarker;
  }

  public void setTrafficPolicyVersionMarker(final String trafficPolicyVersionMarker) {
    this.trafficPolicyVersionMarker = trafficPolicyVersionMarker;
  }

}
