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


public class TrafficPolicySummary extends EucalyptusData {

  @Nonnull
  @FieldRange(min = 1, max = 36)
  private String id;

  @Nonnull
  @FieldRange(min = 1, max = 1000)
  private Integer latestVersion;

  @Nonnull
  @FieldRange(max = 512)
  private String name;

  @Nonnull
  @FieldRange(min = 1, max = 1000)
  private Integer trafficPolicyCount;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_RRTYPE)
  private String type;

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public Integer getLatestVersion() {
    return latestVersion;
  }

  public void setLatestVersion(final Integer latestVersion) {
    this.latestVersion = latestVersion;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Integer getTrafficPolicyCount() {
    return trafficPolicyCount;
  }

  public void setTrafficPolicyCount(final Integer trafficPolicyCount) {
    this.trafficPolicyCount = trafficPolicyCount;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

}
