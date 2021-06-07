/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.dns;

import java.lang.reflect.Modifier;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.route53.common.dns.Route53AliasResolver;
import com.eucalyptus.route53.common.dns.Route53AliasResolver.ResolvedAlias;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

/**
 *.
 */
@SuppressWarnings("unchecked")
public class Route53AliasResolvers extends ServiceJarDiscovery {
  private static final Logger logger = Logger.getLogger( Route53AliasResolvers.class );
  private static final ClassToInstanceMap<Route53AliasResolver> resolvers =
      MutableClassToInstanceMap.create( );

  @Nullable
  static ResolvedAlias resolve(
      final String hostedZoneId,
      final String dnsName
  ) {
    for (final Route53AliasResolver resolver : resolvers.values()) {
      final ResolvedAlias resolvedAlias = resolver.resolve( hostedZoneId, dnsName);
      if (resolvedAlias != null) {
        return resolvedAlias;
      }
    }
    return null;
  }

  @Override
  public boolean processClass(final Class candidate) throws Exception {
    if ( Route53AliasResolver.class.isAssignableFrom( candidate )
        && !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
      try {
        final Route53AliasResolver resolver = (Route53AliasResolver) candidate.newInstance( );
        resolvers.putInstance( candidate, resolver );
        return true;
      } catch ( final Exception ex ) {
        logger.error( "Failed to create instance of Route53AliasResolver: "
            + candidate
            + " because of: "
            + ex.getMessage( ) );
      }
    }
    return false;
  }

  @Override
  public Double getPriority( ) {
    return 0.5d;
  }
}
