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


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/hostedzone/{Id}/authorizevpcassociation")
@HttpNoContent
public class ListVPCAssociationAuthorizationsType extends Route53Message {

  @Nonnull
  @HttpUriMapping(uri = "Id")
  @FieldRange(max = 32)
  private String hostedZoneId;

  @HttpParameterMapping(parameter = "maxresults")
  private String maxResults;

  @HttpParameterMapping(parameter = "nexttoken")
  @FieldRange(max = 256)
  private String nextToken;

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public String getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(final String maxResults) {
    this.maxResults = maxResults;
  }

  public String getNextToken() {
    return nextToken;
  }

  public void setNextToken(final String nextToken) {
    this.nextToken = nextToken;
  }

}
