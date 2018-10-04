/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class ModifyVpcTenancyType extends ComputeMessage {

  @Nonnull
  private String instanceTenancy;
  @Nonnull
  private String vpcId;

  public String getInstanceTenancy( ) {
    return instanceTenancy;
  }

  public void setInstanceTenancy( final String instanceTenancy ) {
    this.instanceTenancy = instanceTenancy;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( final String vpcId ) {
    this.vpcId = vpcId;
  }

}
