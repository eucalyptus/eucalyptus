/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.policy;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.ServiceErnBuilder;
import net.sf.json.JSONException;

/**
 *
 */
public class Route53ErnBuilder extends ServiceErnBuilder {

  public static final Pattern RESOURCE_PATTERN = Pattern.compile( "([a-z0-9_-]+)/(\\S+)" );

  public static final int ARN_PATTERNGROUP_ROUTE53_TYPE = 1;
  public static final int ARN_PATTERNGROUP_ROUTE53_ID = 2;

  public Route53ErnBuilder( ) {
    super(Collections.singleton(Route53PolicySpec.VENDOR_ROUTE53));
  }

  @Override
  public Ern build(final String ern,
                   final String service,
                   final String region,
                   final String account,
                   final String resource ) throws JSONException {
    final Matcher matcher = RESOURCE_PATTERN.matcher( resource );
    if ( matcher.matches( ) ) {
      String type = matcher.group( ARN_PATTERNGROUP_ROUTE53_TYPE ).toLowerCase( );
      String id = matcher.group( ARN_PATTERNGROUP_ROUTE53_ID );
      return new Route53ResourceName(region, account, type, id);
    }
    throw new JSONException( "'" + ern + "' is not a valid ARN" );
  }
}
