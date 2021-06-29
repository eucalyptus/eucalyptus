/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.policy;

import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.ServiceErnBuilder;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONException;

public class RdsErnBuilder extends ServiceErnBuilder {

  public static final Pattern RESOURCE_PATTERN = Pattern.compile( "([a-z0-9_-]+):(\\S+)" );

  public static final int ARN_PATTERNGROUP_RDS_TYPE = 1;
  public static final int ARN_PATTERNGROUP_RDS_ID = 2;

  public RdsErnBuilder( ) {
    super(Collections.singleton(RdsPolicySpec.VENDOR_RDS));
  }

  @Override
  public Ern build(final String ern,
      final String service,
      final String region,
      final String account,
      final String resource ) throws JSONException {
    final Matcher matcher = RESOURCE_PATTERN.matcher( resource );
    if ( matcher.matches( ) ) {
      String type = matcher.group( ARN_PATTERNGROUP_RDS_TYPE ).toLowerCase( );
      String id = matcher.group( ARN_PATTERNGROUP_RDS_ID );
      return new RdsResourceName(region, account, type, id);
    }
    throw new JSONException( "'" + ern + "' is not a valid ARN" );
  }
}
