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


public class ListHostedZonesByNameResponseType extends Route53Message {


  @FieldRange(max = 1024)
  private String dNSName;

  @FieldRange(max = 32)
  private String hostedZoneId;

  @Nonnull
  private HostedZones hostedZones;

  @Nonnull
  private Boolean isTruncated;

  @Nonnull
  private String maxItems;

  @FieldRange(max = 1024)
  private String nextDNSName;

  @FieldRange(max = 32)
  private String nextHostedZoneId;

  public String getDNSName() {
    return dNSName;
  }

  public void setDNSName(final String dNSName) {
    this.dNSName = dNSName;
  }

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public HostedZones getHostedZones() {
    return hostedZones;
  }

  public void setHostedZones(final HostedZones hostedZones) {
    this.hostedZones = hostedZones;
  }

  public Boolean getIsTruncated() {
    return isTruncated;
  }

  public void setIsTruncated(final Boolean isTruncated) {
    this.isTruncated = isTruncated;
  }

  public String getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(final String maxItems) {
    this.maxItems = maxItems;
  }

  public String getNextDNSName() {
    return nextDNSName;
  }

  public void setNextDNSName(final String nextDNSName) {
    this.nextDNSName = nextDNSName;
  }

  public String getNextHostedZoneId() {
    return nextHostedZoneId;
  }

  public void setNextHostedZoneId(final String nextHostedZoneId) {
    this.nextHostedZoneId = nextHostedZoneId;
  }

}
