/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 *
 */
public class Route53RecordSet {

  @Property
  @Required
  private String name;

  @Property
  @Required
  private String type;

  @Property
  private Route53AliasTarget aliasTarget;

  @Property
  private String failover;

  @Property
  private String healthCheckId;

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
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Route53RecordSet that = (Route53RecordSet) o;
    return Objects.equals(getName(), that.getName()) &&
        Objects.equals(getType(), that.getType()) &&
        Objects.equals(getAliasTarget(), that.getAliasTarget()) &&
        Objects.equals(getFailover(), that.getFailover()) &&
        Objects.equals(getHealthCheckId(), that.getHealthCheckId()) &&
        Objects.equals(getMultiValueAnswer(), that.getMultiValueAnswer()) &&
        Objects.equals(getRegion(), that.getRegion()) &&
        Objects.equals(getResourceRecords(), that.getResourceRecords()) &&
        Objects.equals(getSetIdentifier(), that.getSetIdentifier()) &&
        Objects.equals(getTtl(), that.getTtl()) &&
        Objects.equals(getWeight(), that.getWeight());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getType(), getAliasTarget(), getFailover(), getHealthCheckId(),
        getMultiValueAnswer(), getRegion(), getResourceRecords(), getSetIdentifier(), getTtl(), getWeight());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("type", type)
        .add("aliasTarget", aliasTarget)
        .add("failover", failover)
        .add("healthCheckId", healthCheckId)
        .add("multiValueAnswer", multiValueAnswer)
        .add("region", region)
        .add("resourceRecords", resourceRecords)
        .add("setIdentifier", setIdentifier)
        .add("ttl", ttl)
        .add("weight", weight)
        .toString();
  }
}
