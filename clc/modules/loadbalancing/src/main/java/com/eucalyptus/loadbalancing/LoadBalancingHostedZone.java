/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.loadbalancing;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.component.Topology;
import com.eucalyptus.loadbalancing.dns.LoadBalancerDomainName;
import com.eucalyptus.route53.common.Route53;
import com.eucalyptus.route53.common.Route53Api;
import com.eucalyptus.route53.common.msgs.CreateHostedZoneResponseType;
import com.eucalyptus.route53.common.msgs.CreateHostedZoneType;
import com.eucalyptus.route53.common.msgs.HostedZone;
import com.eucalyptus.route53.common.msgs.ListHostedZonesByNameResponseType;
import com.eucalyptus.route53.common.msgs.ListHostedZonesByNameType;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncProxy;
import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.primitives.Longs;
import io.vavr.control.Option;

/**
 *
 */
public class LoadBalancingHostedZone {

  private static final Logger LOG = Logger.getLogger(LoadBalancingHostedZone.class);

  private static final AtomicReference<Pair<String,String>> zoneNameAndId = new AtomicReference<>();
  private static final long zoneCheckInterval = MoreObjects.firstNonNull(
      Longs.tryParse( System.getProperty( "com.eucalyptus.loadbalancing.hostedZoneCheckInterval", "" ) ),
      15L * 60L );
  private static final CompatFunction<String, Supplier<Pair<String,String>>> zoneSupplier =
      FUtils.memoizeLast( zone -> Suppliers.memoizeWithExpiration(
          () -> getOrCreateHostedZone(zone), zoneCheckInterval, TimeUnit.SECONDS ));

  public static Option<Pair<String,String>> getHostedZoneNameAndId() {
    return Option.of(zoneNameAndId.get());
  }

  static boolean check() {
    if (Topology.isEnabled(Route53.class)) {
      final String zoneName = LoadBalancerDomainName.getLoadBalancerSubdomain().relativize(Name.root).toString();
      final Pair<String,String> currentZoneAndId = zoneNameAndId.get();
      final Pair<String,String> newZoneAndId = zoneSupplier.apply(zoneName).get();
      if (newZoneAndId != null &&
          !newZoneAndId.equals(currentZoneAndId) &&
          zoneNameAndId.compareAndSet(currentZoneAndId, newZoneAndId)) {
        LOG.info("Using route53 hosted zone " + newZoneAndId.getLeft() + "/" + newZoneAndId.getRight());
      }
    }
    return true;
  }

  private static Pair<String,String> getOrCreateHostedZone(final String zoneName) {
    final Route53Api route53 = AsyncProxy.privilegedClient(Route53Api.class, AccountIdentifiers.ELB_SYSTEM_ACCOUNT);
    for (int n=0; n<3; n++) {
      final ListHostedZonesByNameResponseType zones = route53.listHostedZonesByName(listZonesRequest(zoneName));
      if (zones.getHostedZones() != null && !zones.getHostedZones().getMember().isEmpty()) {
        return pair(zones.getHostedZones().getMember().get(0), zoneName);
      } else {
        try {
          final CreateHostedZoneResponseType createResponse = route53.createHostedZone(createZoneRequest(zoneName));
          final Pair<String,String> newZoneAndId = pair(createResponse.getHostedZone(), zoneName);
          LOG.info("Created route53 hosted zone " + newZoneAndId.getLeft() + "/" + newZoneAndId.getRight());
          return newZoneAndId;
        } catch (final Exception e) {
          if (AsyncExceptions.isWebServiceErrorCode(e, "HostedZoneAlreadyExists")) {
            continue;
          }
        }
        break;
      }
    }
    return null;
  }

  private static ListHostedZonesByNameType listZonesRequest(final String zoneName) {
    final ListHostedZonesByNameType listZones = new ListHostedZonesByNameType();
    listZones.setDNSName(zoneName);
    return listZones;
  }

  private static CreateHostedZoneType createZoneRequest(final String zoneName) {
    final CreateHostedZoneType createZone = new CreateHostedZoneType();
    createZone.setName(zoneName);
    createZone.setCallerReference(UUID.randomUUID().toString());
    return createZone;
  }

  private static Pair<String,String> pair(final HostedZone zone, final String name) {
    return Pair.of(name, Strings.trimPrefix("/hostedzone/", zone.getId()));
  }
}