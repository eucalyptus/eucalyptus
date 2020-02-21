/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpNoContent;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpRequestMapping;
import com.eucalyptus.binding.HttpUriMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/trafficpolicies/{Id}/versions")
@HttpNoContent
public class ListTrafficPolicyVersionsType extends Route53Message {

  @Nonnull
  @HttpUriMapping(uri = "Id")
  @FieldRange(min = 1, max = 36)
  private String id;

  @HttpParameterMapping(parameter = "maxitems")
  private String maxItems;

  @HttpParameterMapping(parameter = "trafficpolicyversion")
  @FieldRange(max = 4)
  private String trafficPolicyVersionMarker;

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(final String maxItems) {
    this.maxItems = maxItems;
  }

  public String getTrafficPolicyVersionMarker() {
    return trafficPolicyVersionMarker;
  }

  public void setTrafficPolicyVersionMarker(final String trafficPolicyVersionMarker) {
    this.trafficPolicyVersionMarker = trafficPolicyVersionMarker;
  }

}
