/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.dns;

import java.util.Collection;
import java.util.List;
import org.immutables.value.Value.Immutable;

/**
 * Interface implemented by Route53 alias targets
 */
public interface Route53AliasResolver {

  /**
   * The DNS record type
   */
  enum Type {
    A,
    CNAME
  }

  /**
   * The result of the resolution.
   *
   * @see #resolved(Type, int, Collection)
   */
  @Immutable
  interface ResolvedAlias {
    Type getType();
    int getTtl();
    List<String> getValues();
  }

  /**
   * Construct an immutable result.
   */
  static ResolvedAlias resolved(
      final Type type,
      final int ttl,
      final Collection<String> values
  ) {
    return ImmutableResolvedAlias.builder()
        .type(type)
        .ttl(ttl)
        .values(values)
        .build();
  }

  /**
   * Resolve the given name.
   *
   * @param hostedZoneId The hosted zone identifier
   * @param dnsName The canonical name to resolve (with trailing dot)
   * @return The result or null if no result found
   */
  ResolvedAlias resolve(String hostedZoneId, String dnsName);
}
