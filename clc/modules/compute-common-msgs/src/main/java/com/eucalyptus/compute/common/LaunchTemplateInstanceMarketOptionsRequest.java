/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LaunchTemplateInstanceMarketOptionsRequest extends EucalyptusData {

  private String marketType;
  private LaunchTemplateSpotMarketOptionsRequest spotOptions;

  public String getMarketType( ) {
    return marketType;
  }

  public void setMarketType( final String marketType ) {
    this.marketType = marketType;
  }

  public LaunchTemplateSpotMarketOptionsRequest getSpotOptions( ) {
    return spotOptions;
  }

  public void setSpotOptions( final LaunchTemplateSpotMarketOptionsRequest spotOptions ) {
    this.spotOptions = spotOptions;
  }

}
