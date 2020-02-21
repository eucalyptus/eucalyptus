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


public class ResourceRecordSet extends EucalyptusData {

  private AliasTarget aliasTarget;

  @FieldRegex(FieldRegexValue.ENUM_RESOURCERECORDSETFAILOVER)
  private String failover;

  private GeoLocation geoLocation;

  @FieldRange(max = 64)
  private String healthCheckId;

  private Boolean multiValueAnswer;

  @Nonnull
  @FieldRange(max = 1024)
  private String name;

  @FieldRange(min = 1, max = 64)
  private String region;

  @FieldRange(min = 1)
  private ResourceRecords resourceRecords;

  @FieldRange(min = 1, max = 128)
  private String setIdentifier;

  @FieldRange(max = 2147483647)
  private Long tTL;

  @FieldRange(min = 1, max = 36)
  private String trafficPolicyInstanceId;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_RRTYPE)
  private String type;

  @FieldRange(max = 255)
  private Long weight;

  public AliasTarget getAliasTarget() {
    return aliasTarget;
  }

  public void setAliasTarget(final AliasTarget aliasTarget) {
    this.aliasTarget = aliasTarget;
  }

  public String getFailover() {
    return failover;
  }

  public void setFailover(final String failover) {
    this.failover = failover;
  }

  public GeoLocation getGeoLocation() {
    return geoLocation;
  }

  public void setGeoLocation(final GeoLocation geoLocation) {
    this.geoLocation = geoLocation;
  }

  public String getHealthCheckId() {
    return healthCheckId;
  }

  public void setHealthCheckId(final String healthCheckId) {
    this.healthCheckId = healthCheckId;
  }

  public Boolean getMultiValueAnswer() {
    return multiValueAnswer;
  }

  public void setMultiValueAnswer(final Boolean multiValueAnswer) {
    this.multiValueAnswer = multiValueAnswer;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(final String region) {
    this.region = region;
  }

  public ResourceRecords getResourceRecords() {
    return resourceRecords;
  }

  public void setResourceRecords(final ResourceRecords resourceRecords) {
    this.resourceRecords = resourceRecords;
  }

  public String getSetIdentifier() {
    return setIdentifier;
  }

  public void setSetIdentifier(final String setIdentifier) {
    this.setIdentifier = setIdentifier;
  }

  public Long getTTL() {
    return tTL;
  }

  public void setTTL(final Long tTL) {
    this.tTL = tTL;
  }

  public String getTrafficPolicyInstanceId() {
    return trafficPolicyInstanceId;
  }

  public void setTrafficPolicyInstanceId(final String trafficPolicyInstanceId) {
    this.trafficPolicyInstanceId = trafficPolicyInstanceId;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public Long getWeight() {
    return weight;
  }

  public void setWeight(final Long weight) {
    this.weight = weight;
  }

}
