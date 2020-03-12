/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class GeoLocationDetails extends EucalyptusData {

  @FieldRange(min = 2, max = 2)
  private String continentCode;

  @FieldRange(min = 1, max = 32)
  private String continentName;

  @FieldRange(min = 1, max = 2)
  private String countryCode;

  @FieldRange(min = 1, max = 64)
  private String countryName;

  @FieldRange(min = 1, max = 3)
  private String subdivisionCode;

  @FieldRange(min = 1, max = 64)
  private String subdivisionName;

  public String getContinentCode() {
    return continentCode;
  }

  public void setContinentCode(final String continentCode) {
    this.continentCode = continentCode;
  }

  public String getContinentName() {
    return continentName;
  }

  public void setContinentName(final String continentName) {
    this.continentName = continentName;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(final String countryCode) {
    this.countryCode = countryCode;
  }

  public String getCountryName() {
    return countryName;
  }

  public void setCountryName(final String countryName) {
    this.countryName = countryName;
  }

  public String getSubdivisionCode() {
    return subdivisionCode;
  }

  public void setSubdivisionCode(final String subdivisionCode) {
    this.subdivisionCode = subdivisionCode;
  }

  public String getSubdivisionName() {
    return subdivisionName;
  }

  public void setSubdivisionName(final String subdivisionName) {
    this.subdivisionName = subdivisionName;
  }

}
