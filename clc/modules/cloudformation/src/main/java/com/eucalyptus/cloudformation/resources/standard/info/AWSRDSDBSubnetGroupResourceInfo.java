/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class AWSRDSDBSubnetGroupResourceInfo extends ResourceInfo {

  @AttributeJson
  private String arn;

  public AWSRDSDBSubnetGroupResourceInfo( ) {
    setType( "AWS::RDS::DBSubnetGroup" );
  }

  public String getArn() {
    return arn;
  }

  public void setArn(String arn) {
    this.arn = arn;
  }

  @Override
  public boolean supportsTags( ) {
    return true;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "arn", arn )
        .toString( );
  }
}