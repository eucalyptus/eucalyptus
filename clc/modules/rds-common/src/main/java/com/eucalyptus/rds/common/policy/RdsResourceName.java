/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.policy;

import com.eucalyptus.auth.policy.ern.Ern;
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

  public static RdsResourceName parse(final String ern) throws RdsResourceNameException {
    final Ern resourceName;
    try {
      resourceName = Ern.parse(ern);
    } catch (final Exception e) {
      throw new RdsResourceNameException(e.getMessage(), e);
    }
    if (resourceName instanceof RdsResourceName) {
      return (RdsResourceName) resourceName;
    } else {
      throw new RdsResourceNameException( "'" + ern + "' is not a valid rds ARN" );
    }
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
