/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.google.common.base.MoreObjects;

public class AWSEC2InstanceTypeResourceInfo extends ResourceInfo {

  public AWSEC2InstanceTypeResourceInfo( ) {
    setType( "AWS::EC2::InstanceType" );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .toString( );
  }
}
