/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.policy;

import com.eucalyptus.auth.policy.ern.ResourceNameSupport;
import com.google.common.base.Strings;

/**
 *
 */
public class RdsResourceName extends ResourceNameSupport {

  public RdsResourceName(
      final String region,
      final String account,
      final String resourceType,
      final String resourceName
  ) {
    super( RdsPolicySpec.VENDOR_RDS, region, account, resourceType, resourceName );
  }

  @Override
  public String toString( ) {
    return new StringBuilder( )
        .append( ARN_PREFIX )
        .append( getService( ) ).append( ':' )
        .append( Strings.nullToEmpty( getRegion( ) ) ).append( ':' )
        .append( Strings.nullToEmpty( getAccount( ) ) ).append( ':' )
        .append( getType( ) ).append( ':' )
        .append( getResourceName( ) ).toString( );
  }
}
