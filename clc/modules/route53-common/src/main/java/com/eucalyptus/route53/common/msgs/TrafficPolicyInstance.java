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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TrafficPolicyInstance extends EucalyptusData {

  @Nonnull
  @FieldRange(max = 32)
  private String hostedZoneId;

  @Nonnull
  @FieldRange(min = 1, max = 36)
  private String id;

  @Nonnull
  @FieldRange(max = 1024)
  private String message;

  @Nonnull
  @FieldRange(max = 1024)
  private String name;

  @Nonnull
  private String state;

  @Nonnull
  @FieldRange(max = 2147483647)
  private Long tTL;

  @Nonnull
  @FieldRange(min = 1, max = 36)
  private String trafficPolicyId;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_RRTYPE)
  private String trafficPolicyType;

  @Nonnull
  @FieldRange(min = 1, max = 1000)
  private Integer trafficPolicyVersion;

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getState() {
    return state;
  }

  public void setState(final String state) {
    this.state = state;
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

  public String getTrafficPolicyType() {
    return trafficPolicyType;
  }

  public void setTrafficPolicyType(final String trafficPolicyType) {
    this.trafficPolicyType = trafficPolicyType;
  }

  public Integer getTrafficPolicyVersion() {
    return trafficPolicyVersion;
  }

  public void setTrafficPolicyVersion(final Integer trafficPolicyVersion) {
    this.trafficPolicyVersion = trafficPolicyVersion;
  }

}
