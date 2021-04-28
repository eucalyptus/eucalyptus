/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.dns;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.compute.common.network.NetworkLookup;
import com.eucalyptus.entities.EntityCache;
import com.eucalyptus.route53.common.Route53;
import com.eucalyptus.route53.common.dns.Route53AliasResolver.ResolvedAlias;
import com.eucalyptus.route53.service.persist.HostedZones;
import com.eucalyptus.route53.service.persist.entities.HostedZone;
import com.eucalyptus.route53.service.persist.entities.ResourceRecordSet.Type;
import com.eucalyptus.route53.service.persist.views.HostedZoneComposite;
import com.eucalyptus.route53.service.persist.views.HostedZoneView;
import com.eucalyptus.route53.service.persist.views.ImmutableResourceRecordSetView;
import com.eucalyptus.route53.service.persist.views.ResourceRecordSetView;
import com.eucalyptus.util.Subnets;
import com.eucalyptus.util.WildcardNameMatcher;
import com.eucalyptus.util.dns.DnsResolvers;
import com.eucalyptus.util.dns.DnsResolvers.DnsRequest;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.util.dns.NameserverResolver;
import com.eucalyptus.util.techpreview.TechPreviews;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 *
 */

@SuppressWarnings("StaticPseudoFunctionalStyleMethod")
public class Route53DnsResolver extends DnsResolvers.DnsResolver {
  private static final Logger logger = Logger.getLogger( Route53DnsResolver.class );

  private static final EntityCache<HostedZone, HostedZoneComposite> cache =
      new EntityCache<>(HostedZone.exampleWithOwner(null), HostedZones.COMPOSITE_FULL);

  private static final Supplier<Iterable<HostedZoneComposite>> cacheSupplier =
      Suppliers.memoizeWithExpiration(cache::get, 30, TimeUnit.SECONDS);

  private static final WildcardNameMatcher publicZoneMatcher = new WildcardNameMatcher( );

  private static final String ESCAPED_QUOTE_TEXT = "\\\"";
  private static final String QUOTE_TEXT = "\"";

  @Override
  public int getOrder( ) {
    return DEFAULT_ORDER + 200;
  }

  @Override
  public boolean checkAccepts(final DnsRequest request) {
    final Record query = request.getQuery();
    final Name name = query.getName( );
    if (!Bootstrap.isOperational() ||
        !Route53DnsProperties.isResolverEnabled() ||
        TechPreviews.isTechPreview(Route53.class) ||
        name.equals(DomainNames.externalSubdomain())
    ) {
      return false;
    } else if (!Route53DnsProperties.isUserResolverEnabled() &&
        !DomainNames.isExternalSubdomain(name)) {
      return false;
    }

    final Set<String> hostedZones = getHostedZoneNames(cacheSupplier.get());
    return hostedZones.removeAll(getCandidateZoneNames(name));
  }

  @Override
  public DnsResponse lookupRecords(final DnsRequest request) {
    final Record query = request.getQuery( );
    final Name name = query.getName( );
    final String nameString = name.toString();
    final Set<String> zoneNames = getCandidateZoneNames(name);
    final Iterable<HostedZoneComposite> zones = cacheSupplier.get();
    Record authority = null;

    // public
    for (final HostedZoneComposite zoneAndRecords : zones) {
      final HostedZoneView zone = zoneAndRecords.getHostedZone();
      if (!zone.getPrivateZone() &&
          zoneNames.contains(zone.getZoneName())) {
        if (Subnets.isSystemManagedAddress(request.getRemoteAddress())) {
          // For system hosts, only return public zones if they are whitelisted or
          // a subdomain of the cloud (so under administrative control)
          // Other public zones will can be resolved by the recursive resolver if
          // delegated to.
          if (!publicZoneMatcher.matches(Route53DnsProperties.getPublicZoneWhitelist(), zone.getZoneName()) &&
              !DomainNames.isExternalSubdomain(name)) {
            continue;
          }
        }
        final DnsResponse response = getZoneResponse(request, name, query.getType(), nameString, zoneAndRecords);
        if ( response != null ) {
          return response;
        } else if (authority == null){
          authority = getAuthority(zoneAndRecords);
        }
      }
    }

    // private
    if ( Subnets.isSystemManagedAddress( request.getRemoteAddress( ) ) ) {
      final Option<String> vpcId = NetworkLookup.lookupByIp(request.getRemoteAddress()).getVpcId();
      if (vpcId.isDefined()) {
        for (final HostedZoneComposite zoneAndRecords : zones) {
          final HostedZoneView zone = zoneAndRecords.getHostedZone();
          if (zone.getPrivateZone() &&
              zone.getVpcIds().contains(vpcId.get()) &&
              zoneNames.contains(zone.getZoneName())) {
            final DnsResponse response = getZoneResponse(request, name, query.getType(), nameString, zoneAndRecords);
            if ( response != null ) {
              return response;
            }
          }
        }
      }
    }

    return authority!=null ?
        DnsResponse.forName( name ).withAuthority(authority).nxdomain() :
        null;
  }

  private DnsResponse getZoneResponse(
      final DnsRequest request,
      final Name queryName,
      final int queryType,
      final String lookupName,
      final HostedZoneComposite zoneAndRecords) {
    boolean sawIp = false;
    for (final ResourceRecordSetView rrSet : zoneAndRecords.getResourceRecordSets()) {
      if (rrSet.getName().equalsIgnoreCase(lookupName)) {
        if (matchType(rrSet.getType(), queryType)) {
          return response(request, queryName, rrSet, zoneAndRecords);
        }
        if (rrSet.getType() == Type.A || rrSet.getType() == Type.AAAA) {
          sawIp = true;
        }
      }
    }
    if (sawIp && (queryType == Type.AAAA.code() || queryType == Type.A.code())) {
       return DnsResponse.forName(queryName).answer();
    }
    return null;
  }

  private boolean matchType(final Type type, final int typeCode) {
    return type.code() == typeCode || type == Type.CNAME;
  }

  private Set<String> getHostedZoneNames(final Iterable<HostedZoneComposite> zones) {
    final Set<String> hostedZoneNames = Sets.newHashSet();
    hostedZoneNames.addAll(Lists.newArrayList(Iterables.transform(
        zones,
        hzc -> hzc.getHostedZone().getZoneName() )) );
    return hostedZoneNames;
  }

  public Set<String> getCandidateZoneNames(final Name name) {
    final Set<String> zoneNames = Sets.newHashSet();
    for (Name zoneName = name; zoneName.labels() > 1; zoneName = new Name(zoneName, 1)) {
      zoneNames.add(zoneName.toString().toLowerCase());
    }
    return zoneNames;
  }

  private DnsResponse response(
      final DnsRequest request,
      final Name name,
      final ResourceRecordSetView matchedRRSet,
      final HostedZoneComposite zone
  ) {
    DnsResponse response = null;
    final ResourceRecordSetView rrSet;
    if (matchedRRSet.getAliasDnsName() != null) {
      if ( zone.getHostedZone().getDisplayName().equals(matchedRRSet.getAliasHostedZoneId()) ) {
        return getZoneResponse(request, name, matchedRRSet.getType().code(), matchedRRSet.getAliasDnsName(), zone);
      } else {
        final ResolvedAlias resolvedAlias =
            Route53AliasResolvers.resolve(matchedRRSet.getAliasHostedZoneId(), matchedRRSet.getAliasDnsName());
        if (resolvedAlias == null) {
          return null;
        }

        rrSet = ImmutableResourceRecordSetView.builder()
            .from(matchedRRSet)
            .type(Type.valueOf(resolvedAlias.getType().name()))
            .ttl(resolvedAlias.getTtl())
            .values(resolvedAlias.getValues())
            .build();
      }
    } else {
      rrSet = matchedRRSet;
    }

    switch (rrSet.getType()) {
      case A:
        final List<Record> arecords = Lists.newArrayList();
        for (final String ip : rrSet.getValues()) {
          final InetAddress inetAddress = InetAddresses.forString(ip);
          if (inetAddress instanceof Inet4Address) {
            arecords.add(new ARecord(name, DClass.IN, rrSet.getTtl(), inetAddress));
          }
        }
        response = DnsResponse.forName(name).answer(arecords);
        break;
      case AAAA:
        final List<Record> aaaarecords = Lists.newArrayList();
        for (final String ip : rrSet.getValues()) {
          final InetAddress inetAddress = InetAddresses.forString(ip);
          if (inetAddress instanceof Inet6Address) {
            aaaarecords.add(new AAAARecord(name, DClass.IN, rrSet.getTtl(), inetAddress));
          }
        }
        response = DnsResponse.forName(name).answer(aaaarecords);
        break;
      case CNAME:
        if (!rrSet.getValues().isEmpty()) {
          response = DnsResponse.forName(name)
              .answer(new CNAMERecord(name, DClass.IN, rrSet.getTtl(),
                  DomainNames.absolute(Name.fromConstantString(rrSet.getValues().get(0)))));
        }
        break;
      case NS:
        final Tuple2<List<Record>,List<Record>> nsEtc =
            getNameserverRecordsAndAdditionals(request, name, rrSet,
                getMatchingRRSets(Type.A, rrSet.getValues(), zone.getResourceRecordSets()));
        final List<Record> nsrecords = nsEtc._1();
        final List<Record> additionalRecords = nsEtc._2();
        response = DnsResponse.forName(name)
            .withAdditional(additionalRecords)
            .answer(nsrecords);
        break;
      case SOA:
        if (!rrSet.getValues().isEmpty()) {
          try {
            final Tuple2<List<Record>,List<Record>> nsAuthEtc =
                getNameserverRecordsAndAdditionals(request, name, zone);
            final List<Record> authority = nsAuthEtc._1();
            final List<Record> additional = nsAuthEtc._2();
            response = DnsResponse.forName(name)
                .withAuthority(authority)
                .withAdditional(additional)
                .answer( Record.fromString(name, Type.SOA.code(), DClass.IN, rrSet.getTtl(),
                    rrSet.getValues().get(0), Name.fromConstantString(zone.getHostedZone().getZoneName())));
          } catch (IOException e) {
            // invalid SOA record
          }
        }
        break;
      case TXT:
        response = DnsResponse.forName(name)
            .answer(new TXTRecord(name, DClass.IN, rrSet.getTtl(), unquote(rrSet.getValues())));
        break;
    }
    return response;
  }

  private static List<String> unquote(final List<String> values) {
    return Stream.ofAll(values)
        .flatMap(Route53DnsResolver::unquote)
        .toJavaList();
  }

  private static Option<String> unquote(final String value) {
    if (value.length() < 2 || !value.startsWith(QUOTE_TEXT) || !value.endsWith(QUOTE_TEXT)) {
      return Option.none();
    }
    final String trimmed = value.substring(1, value.length()-1);
    return Option.some(trimmed.replace(ESCAPED_QUOTE_TEXT, QUOTE_TEXT));
  }

  @Nullable
  private Record getAuthority(final HostedZoneComposite zoneAndRecords) {
    final Record authority;
    final HostedZoneView zone = zoneAndRecords.getHostedZone();
    final List<ResourceRecordSetView> soaRRSets =
        getMatchingRRSets(Type.SOA, Collections.singleton(zone.getZoneName()),
            zoneAndRecords.getResourceRecordSets());
    authority = soaRRSets.stream().findFirst().map( soaRRSet -> {
      try {
        return Record.fromString(
            Name.fromConstantString(zone.getZoneName()),
            Type.SOA.code(),
            DClass.IN,
            soaRRSet.getTtl().longValue(),
            soaRRSet.getValues().get(0),
            Name.fromConstantString(zone.getZoneName()));
      } catch (IOException e) {
        return null;
      }
    }).orElse(null);
    return authority;
  }

  private List<ResourceRecordSetView> getMatchingRRSets(
      final Type type,
      final Collection<String> names,
      final List<ResourceRecordSetView> candidateRRSets
  ) {
    final List<ResourceRecordSetView> rrSets = Lists.newArrayList();
    final Set<String> canonicalNames = names.stream()
        .map(Route53DnsHelper::absoluteName).collect(Collectors.toSet());
    for (final ResourceRecordSetView candidate : candidateRRSets) {
      if (candidate.getType()==type && canonicalNames.contains(candidate.getName())) {
        rrSets.add(candidate);
      }
    }
    return rrSets;
  }

  private Tuple2<List<Record>,List<Record>> getNameserverRecordsAndAdditionals(
      final DnsRequest request,
      final Name name,
      final HostedZoneComposite zoneAndRecords
  ) {
    final String lookupName = name.toString();
    for (final ResourceRecordSetView rrSet : zoneAndRecords.getResourceRecordSets()) {
      if (rrSet.getName().equalsIgnoreCase(lookupName) && rrSet.getType()==Type.NS) {
        return getNameserverRecordsAndAdditionals(request, name, rrSet,
            getMatchingRRSets(Type.A, rrSet.getValues(), zoneAndRecords.getResourceRecordSets()));
      }
    }
    return Tuple.of(null, null);
  }

  private Tuple2<List<Record>,List<Record>> getNameserverRecordsAndAdditionals(
      final DnsRequest request,
      final Name name,
      final ResourceRecordSetView rrSet,
      final List<ResourceRecordSetView> rrSetAdditionals
  ) {
    final List<Record> nsrecords = Lists.newArrayList();
    for (final String value : rrSet.getValues()) {
      final Name nsName = DomainNames.absolute(Name.fromConstantString(value));
      nsrecords.add(new NSRecord(name, DClass.IN, rrSet.getTtl(), nsName));
    }
    List<Record> additionalRecords = null;
    if (rrSetAdditionals != null && !rrSetAdditionals.isEmpty()) {
      additionalRecords = Lists.newArrayList();
      for (final ResourceRecordSetView additionalRRSet : rrSetAdditionals) try {
        additionalRecords.add(DomainNameRecords.addressRecord(
            Name.fromConstantString( additionalRRSet.getName() ) ,
            NameserverResolver.maphost(
                request.getLocalAddress( ),
                InetAddresses.forString(additionalRRSet.getValues().get(0))),
            additionalRRSet.getTtl()));
      } catch (IllegalArgumentException e ){
        // ignore record with invalid ip
      }
    }
    return Tuple.of(nsrecords, additionalRecords);
  }
}
