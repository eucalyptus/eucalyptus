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


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/geolocation")
@HttpNoContent
public class GetGeoLocationType extends Route53Message {

  @HttpParameterMapping(parameter = "continentcode")
  @FieldRange(min = 2, max = 2)
  private String continentCode;

  @HttpParameterMapping(parameter = "countrycode")
  @FieldRange(min = 1, max = 2)
  private String countryCode;

  @HttpParameterMapping(parameter = "subdivisioncode")
  @FieldRange(min = 1, max = 3)
  private String subdivisionCode;

  public String getContinentCode() {
    return continentCode;
  }

  public void setContinentCode(final String continentCode) {
    this.continentCode = continentCode;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(final String countryCode) {
    this.countryCode = countryCode;
  }

  public String getSubdivisionCode() {
    return subdivisionCode;
  }

  public void setSubdivisionCode(final String subdivisionCode) {
    this.subdivisionCode = subdivisionCode;
  }

}
