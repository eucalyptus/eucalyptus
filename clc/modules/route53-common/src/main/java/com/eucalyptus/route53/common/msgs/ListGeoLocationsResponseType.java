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


public class ListGeoLocationsResponseType extends Route53Message {


  @Nonnull
  private GeoLocationDetailsList geoLocationDetailsList;

  @Nonnull
  private Boolean isTruncated;

  @Nonnull
  private String maxItems;

  @FieldRange(min = 2, max = 2)
  private String nextContinentCode;

  @FieldRange(min = 1, max = 2)
  private String nextCountryCode;

  @FieldRange(min = 1, max = 3)
  private String nextSubdivisionCode;

  public GeoLocationDetailsList getGeoLocationDetailsList() {
    return geoLocationDetailsList;
  }

  public void setGeoLocationDetailsList(final GeoLocationDetailsList geoLocationDetailsList) {
    this.geoLocationDetailsList = geoLocationDetailsList;
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

  public String getNextContinentCode() {
    return nextContinentCode;
  }

  public void setNextContinentCode(final String nextContinentCode) {
    this.nextContinentCode = nextContinentCode;
  }

  public String getNextCountryCode() {
    return nextCountryCode;
  }

  public void setNextCountryCode(final String nextCountryCode) {
    this.nextCountryCode = nextCountryCode;
  }

  public String getNextSubdivisionCode() {
    return nextSubdivisionCode;
  }

  public void setNextSubdivisionCode(final String nextSubdivisionCode) {
    this.nextSubdivisionCode = nextSubdivisionCode;
  }

}
