/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 *
 */
public class AWSRoute53RecordSetProperties implements ResourceProperties {

  @Property
  @Required
  private String name;

  @Property
  @Required
  private String type;

  @Property
  private Route53AliasTarget aliasTarget;

  @Property
  private String comment;

  @Property
  private String failover;

  @Property
  private String healthCheckId;

  @Property
  private String hostedZoneId;

  @Property
  private String hostedZoneName;

  @Property
  private Boolean multiValueAnswer;

  @Property
  private String region;

  @Property
  private ArrayList<String> resourceRecords = Lists.newArrayList();

  @Property
  private String setIdentifier;

  @Property(name = "TTL")
  private String ttl;

  @Property
  private Integer weight;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public Route53AliasTarget getAliasTarget() {
    return aliasTarget;
  }

  public void setAliasTarget(final Route53AliasTarget aliasTarget) {
    this.aliasTarget = aliasTarget;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(final String comment) {
    this.comment = comment;
  }

  public String getFailover() {
    return failover;
  }

  public void setFailover(final String failover) {
    this.failover = failover;
  }

  public String getHealthCheckId() {
    return healthCheckId;
  }

  public void setHealthCheckId(final String healthCheckId) {
    this.healthCheckId = healthCheckId;
  }

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public String getHostedZoneName() {
    return hostedZoneName;
  }

  public void setHostedZoneName(final String hostedZoneName) {
    this.hostedZoneName = hostedZoneName;
  }

  public Boolean getMultiValueAnswer() {
    return multiValueAnswer;
  }

  public void setMultiValueAnswer(final Boolean multiValueAnswer) {
    this.multiValueAnswer = multiValueAnswer;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(final String region) {
    this.region = region;
  }

  public ArrayList<String> getResourceRecords() {
    return resourceRecords;
  }

  public void setResourceRecords(final ArrayList<String> resourceRecords) {
    this.resourceRecords = resourceRecords;
  }

  public String getSetIdentifier() {
    return setIdentifier;
  }

  public void setSetIdentifier(final String setIdentifier) {
    this.setIdentifier = setIdentifier;
  }

  public String getTtl() {
    return ttl;
  }

  public void setTtl(final String ttl) {
    this.ttl = ttl;
  }

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(final Integer weight) {
    this.weight = weight;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("type", type)
        .add("aliasTarget", aliasTarget)
        .add("comment", comment)
        .add("failover", failover)
        .add("healthCheckId", healthCheckId)
        .add("hostedZoneId", hostedZoneId)
        .add("hostedZoneName", hostedZoneName)
        .add("multiValueAnswer", multiValueAnswer)
        .add("region", region)
        .add("resourceRecords", resourceRecords)
        .add("setIdentifier", setIdentifier)
        .add("ttl", ttl)
        .add("weight", weight)
        .toString();
  }
}
