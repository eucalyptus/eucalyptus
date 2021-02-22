/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class AWSRDSDBSubnetGroupResourceInfo extends ResourceInfo {

  public AWSRDSDBSubnetGroupResourceInfo( ) {
    setType( "AWS::RDS::DBSubnetGroup" );
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