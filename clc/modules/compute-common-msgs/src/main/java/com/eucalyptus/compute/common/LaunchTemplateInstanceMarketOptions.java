/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LaunchTemplateInstanceMarketOptions extends EucalyptusData {

  private String marketType;
  private LaunchTemplateSpotMarketOptions spotOptions;

  public String getMarketType( ) {
    return marketType;
  }

  public void setMarketType( final String marketType ) {
    this.marketType = marketType;
  }

  public LaunchTemplateSpotMarketOptions getSpotOptions( ) {
    return spotOptions;
  }

  public void setSpotOptions( final LaunchTemplateSpotMarketOptions spotOptions ) {
    this.spotOptions = spotOptions;
  }

}
