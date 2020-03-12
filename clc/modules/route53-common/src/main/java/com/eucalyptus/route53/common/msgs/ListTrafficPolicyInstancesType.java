/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import com.eucalyptus.binding.HttpNoContent;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpRequestMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/trafficpolicyinstances")
@HttpNoContent
public class ListTrafficPolicyInstancesType extends Route53Message {

  @HttpParameterMapping(parameter = "hostedzoneid")
  @FieldRange(max = 32)
  private String hostedZoneIdMarker;

  @HttpParameterMapping(parameter = "maxitems")
  private String maxItems;

  @HttpParameterMapping(parameter = "trafficpolicyinstancename")
  @FieldRange(max = 1024)
  private String trafficPolicyInstanceNameMarker;

  @HttpParameterMapping(parameter = "trafficpolicyinstancetype")
  @FieldRegex(FieldRegexValue.ENUM_RRTYPE)
  private String trafficPolicyInstanceTypeMarker;

  public String getHostedZoneIdMarker() {
    return hostedZoneIdMarker;
  }

  public void setHostedZoneIdMarker(final String hostedZoneIdMarker) {
    this.hostedZoneIdMarker = hostedZoneIdMarker;
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

}
