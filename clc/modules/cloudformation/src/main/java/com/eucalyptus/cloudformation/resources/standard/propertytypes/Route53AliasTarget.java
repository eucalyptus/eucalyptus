/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class Route53AliasTarget {

  @Property(name = "DNSName")
  @Required
  private String dnsName;

  @Property
  private Boolean evaluateTargetHealth;

  @Property
  @Required
  private String hostedZoneId;

  public String getDnsName() {
    return dnsName;
  }

  public void setDnsName(final String dnsName) {
    this.dnsName = dnsName;
  }

  public Boolean getEvaluateTargetHealth() {
    return evaluateTargetHealth;
  }

  public void setEvaluateTargetHealth(final Boolean evaluateTargetHealth) {
    this.evaluateTargetHealth = evaluateTargetHealth;
  }

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Route53AliasTarget that = (Route53AliasTarget) o;
    return Objects.equals(getDnsName(), that.getDnsName()) &&
        Objects.equals(getEvaluateTargetHealth(), that.getEvaluateTargetHealth()) &&
        Objects.equals(getHostedZoneId(), that.getHostedZoneId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDnsName(), getEvaluateTargetHealth(), getHostedZoneId());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("dnsName", dnsName)
        .add("evaluateTargetHealth", evaluateTargetHealth)
        .add("hostedZoneId", hostedZoneId)
        .toString();
  }
}
