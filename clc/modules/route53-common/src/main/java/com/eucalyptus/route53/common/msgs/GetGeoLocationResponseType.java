/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;


public class GetGeoLocationResponseType extends Route53Message {


  @Nonnull
  private GeoLocationDetails geoLocationDetails;

  public GeoLocationDetails getGeoLocationDetails() {
    return geoLocationDetails;
  }

  public void setGeoLocationDetails(final GeoLocationDetails geoLocationDetails) {
    this.geoLocationDetails = geoLocationDetails;
  }

}
