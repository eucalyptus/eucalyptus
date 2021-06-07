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


public class ListTrafficPoliciesResponseType extends Route53Message {


  @Nonnull
  private Boolean isTruncated;

  @Nonnull
  private String maxItems;

  @Nonnull
  @FieldRange(min = 1, max = 36)
  private String trafficPolicyIdMarker;

  @Nonnull
  private TrafficPolicySummaries trafficPolicySummaries;

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

  public String getTrafficPolicyIdMarker() {
    return trafficPolicyIdMarker;
  }

  public void setTrafficPolicyIdMarker(final String trafficPolicyIdMarker) {
    this.trafficPolicyIdMarker = trafficPolicyIdMarker;
  }

  public TrafficPolicySummaries getTrafficPolicySummaries() {
    return trafficPolicySummaries;
  }

  public void setTrafficPolicySummaries(final TrafficPolicySummaries trafficPolicySummaries) {
    this.trafficPolicySummaries = trafficPolicySummaries;
  }

}
