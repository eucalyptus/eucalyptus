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
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;


public class ListTrafficPolicyInstancesResponseType extends Route53Message {


  @FieldRange(max = 32)
  private String hostedZoneIdMarker;

  @Nonnull
  private Boolean isTruncated;

  @Nonnull
  private String maxItems;

  @FieldRange(max = 1024)
  private String trafficPolicyInstanceNameMarker;

  @FieldRegex(FieldRegexValue.ENUM_RRTYPE)
  private String trafficPolicyInstanceTypeMarker;

  @Nonnull
  private TrafficPolicyInstances trafficPolicyInstances;

  public String getHostedZoneIdMarker() {
    return hostedZoneIdMarker;
  }

  public void setHostedZoneIdMarker(final String hostedZoneIdMarker) {
    this.hostedZoneIdMarker = hostedZoneIdMarker;
  }

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

  public String getTrafficPolicyInstanceNameMarker() {
    return trafficPolicyInstanceNameMarker;
  }

  public void setTrafficPolicyInstanceNameMarker(final String trafficPolicyInstanceNameMarker) {
    this.trafficPolicyInstanceNameMarker = trafficPolicyInstanceNameMarker;
  }

  public String getTrafficPolicyInstanceTypeMarker() {
    return trafficPolicyInstanceTypeMarker;
  }

  public void setTrafficPolicyInstanceTypeMarker(final String trafficPolicyInstanceTypeMarker) {
    this.trafficPolicyInstanceTypeMarker = trafficPolicyInstanceTypeMarker;
  }

  public TrafficPolicyInstances getTrafficPolicyInstances() {
    return trafficPolicyInstances;
  }

  public void setTrafficPolicyInstances(final TrafficPolicyInstances trafficPolicyInstances) {
    this.trafficPolicyInstances = trafficPolicyInstances;
  }

}
