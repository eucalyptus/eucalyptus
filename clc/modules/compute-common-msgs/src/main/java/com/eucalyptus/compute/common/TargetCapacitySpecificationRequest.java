/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import javax.annotation.Nonnull;


public class TargetCapacitySpecificationRequest extends EucalyptusData {

  private String defaultTargetCapacityType;
  private Integer onDemandTargetCapacity;
  private Integer spotTargetCapacity;
  @Nonnull
  private Integer totalTargetCapacity;

  public String getDefaultTargetCapacityType( ) {
    return defaultTargetCapacityType;
  }

  public void setDefaultTargetCapacityType( final String defaultTargetCapacityType ) {
    this.defaultTargetCapacityType = defaultTargetCapacityType;
  }

  public Integer getOnDemandTargetCapacity( ) {
    return onDemandTargetCapacity;
  }

  public void setOnDemandTargetCapacity( final Integer onDemandTargetCapacity ) {
    this.onDemandTargetCapacity = onDemandTargetCapacity;
  }

  public Integer getSpotTargetCapacity( ) {
    return spotTargetCapacity;
  }

  public void setSpotTargetCapacity( final Integer spotTargetCapacity ) {
    this.spotTargetCapacity = spotTargetCapacity;
  }

  public Integer getTotalTargetCapacity( ) {
    return totalTargetCapacity;
  }

  public void setTotalTargetCapacity( final Integer totalTargetCapacity ) {
    this.totalTargetCapacity = totalTargetCapacity;
  }

}
