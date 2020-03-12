/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpRequestMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


@HttpRequestMapping(method = "POST", uri = "/2013-04-01/trafficpolicyinstance")
public class CreateTrafficPolicyInstanceType extends Route53Message {

  @Nonnull
  @FieldRange(max = 32)
  private String hostedZoneId;

  @Nonnull
  @FieldRange(max = 1024)
  private String name;

  @Nonnull
  @FieldRange(max = 2147483647)
  private Long tTL;

  @Nonnull
  @FieldRange(min = 1, max = 36)
  private String trafficPolicyId;

  @Nonnull
  @FieldRange(min = 1, max = 1000)
  private Integer trafficPolicyVersion;

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Long getTTL() {
    return tTL;
  }

  public void setTTL(final Long tTL) {
    this.tTL = tTL;
  }

  public String getTrafficPolicyId() {
    return trafficPolicyId;
  }

  public void setTrafficPolicyId(final String trafficPolicyId) {
    this.trafficPolicyId = trafficPolicyId;
  }

  public Integer getTrafficPolicyVersion() {
    return trafficPolicyVersion;
  }

  public void setTrafficPolicyVersion(final Integer trafficPolicyVersion) {
    this.trafficPolicyVersion = trafficPolicyVersion;
  }

}
