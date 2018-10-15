/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
