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


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/trafficpolicies")
@HttpNoContent
public class ListTrafficPoliciesType extends Route53Message {

  @HttpParameterMapping(parameter = "maxitems")
  private String maxItems;

  @HttpParameterMapping(parameter = "trafficpolicyid")
  @FieldRange(min = 1, max = 36)
  private String trafficPolicyIdMarker;

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

}
