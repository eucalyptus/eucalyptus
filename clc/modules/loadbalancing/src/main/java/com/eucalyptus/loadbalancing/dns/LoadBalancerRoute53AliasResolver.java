/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.dns;

import java.util.List;
import javax.annotation.Nullable;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord;
import com.eucalyptus.loadbalancing.LoadBalancingHostedZoneManager;
import com.eucalyptus.route53.common.dns.Route53AliasResolver;
import com.eucalyptus.util.Pair;

/**
 *
 */
public class LoadBalancerRoute53AliasResolver implements Route53AliasResolver {

  @Override
  public ResolvedAlias resolve(final String hostedZoneId, final String dnsName) {
    @Nullable final String loadBalancerHostedZoneId =
        LoadBalancingHostedZoneManager.getHostedZoneNameAndId().map(Pair::getRight).getOrNull();
    @Nullable final List<String> names =
        loadBalancerHostedZoneId!=null && loadBalancerHostedZoneId.equals(hostedZoneId) ?
            LoadBalancerResolver.getIps(dnsName) :
            null;
    return names == null ?
        null :
        Route53AliasResolver.resolved(Type.A, LoadBalancerDnsRecord.getLoadbalancerTTL( ), names);
  }
}
