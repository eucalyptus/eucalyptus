/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
