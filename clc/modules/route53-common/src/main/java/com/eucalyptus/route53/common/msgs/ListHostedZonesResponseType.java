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


public class ListHostedZonesResponseType extends Route53Message {


  @Nonnull
  private HostedZones hostedZones;

  @Nonnull
  private Boolean isTruncated;

  @Nonnull
  @FieldRange(max = 64)
  private String marker;

  @Nonnull
  private String maxItems;

  @FieldRange(max = 64)
  private String nextMarker;

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

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public String getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(final String maxItems) {
    this.maxItems = maxItems;
  }

  public String getNextMarker() {
    return nextMarker;
  }

  public void setNextMarker(final String nextMarker) {
    this.nextMarker = nextMarker;
  }

}
