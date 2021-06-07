/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.dns;

import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;

/**
 *
 */
@ConfigurableClass(root = "services.route53.dns", description = "Parameters controlling route53")
public class Route53DnsProperties {
  private static Logger LOG = Logger.getLogger(Route53DnsProperties.class);

  @ConfigurableField(
      description = "Enable the route53 dns resolver.  Note: dns.enabled must also be 'true'",
      initial = "true" )
  public static Boolean RESOLVER_ENABLED = Boolean.TRUE;

  @ConfigurableField(
      description = "Enable the route53 dns resolver for users hosted zones.  Must also be enabled.",
      initial = "true" )
  public static Boolean USER_RESOLVER_ENABLED = Boolean.TRUE;


  @ConfigurableField(
      description = "List of patterns identifying trusted hosted zones for internal dns.",
      initial = "" )
  public static String PUBLIC_ZONE_WHITELIST = "";

  public static boolean isResolverEnabled() {
    return Boolean.TRUE.equals(RESOLVER_ENABLED);
  }

  public static boolean isUserResolverEnabled() {
    return Boolean.TRUE.equals(USER_RESOLVER_ENABLED);
  }

  public static String getPublicZoneWhitelist() {
    return PUBLIC_ZONE_WHITELIST;
  }
}
