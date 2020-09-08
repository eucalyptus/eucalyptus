/**
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class AWSEC2LaunchTemplateResourceInfo extends ResourceInfo {

  public AWSEC2LaunchTemplateResourceInfo( ) {
    setType( "AWS::EC2::LaunchTemplate" );
  }

  @Override
  public boolean supportsTags( ) {
    return true;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .toString( );
  }
}
