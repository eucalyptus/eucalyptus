/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import com.eucalyptus.binding.HttpNoContent;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpRequestMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/geolocations")
@HttpNoContent
public class ListGeoLocationsType extends Route53Message {

  @HttpParameterMapping(parameter = "maxitems")
  private String maxItems;

  @HttpParameterMapping(parameter = "startcontinentcode")
  @FieldRange(min = 2, max = 2)
  private String startContinentCode;

  @HttpParameterMapping(parameter = "startcountrycode")
  @FieldRange(min = 1, max = 2)
  private String startCountryCode;

  @HttpParameterMapping(parameter = "startsubdivisioncode")
  @FieldRange(min = 1, max = 3)
  private String startSubdivisionCode;

  public String getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(final String maxItems) {
    this.maxItems = maxItems;
  }

  public String getStartContinentCode() {
    return startContinentCode;
  }

  public void setStartContinentCode(final String startContinentCode) {
    this.startContinentCode = startContinentCode;
  }

  public String getStartCountryCode() {
    return startCountryCode;
  }

  public void setStartCountryCode(final String startCountryCode) {
    this.startCountryCode = startCountryCode;
  }

  public String getStartSubdivisionCode() {
    return startSubdivisionCode;
  }

  public void setStartSubdivisionCode(final String startSubdivisionCode) {
    this.startSubdivisionCode = startSubdivisionCode;
  }

}
