/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.dns;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.rds.service.persist.DBInstances;
import com.eucalyptus.rds.service.persist.RdsMetadataNotFoundException;
import com.eucalyptus.rds.service.persist.entities.DBInstance;
import com.eucalyptus.rds.service.persist.entities.PersistenceDBInstances;
import com.eucalyptus.util.dns.DnsResolvers;
import com.eucalyptus.util.dns.DnsResolvers.DnsRequest;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.eucalyptus.util.dns.DomainNames;
import com.google.common.base.Predicates;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import io.vavr.Tuple2;
import io.vavr.control.Option;

/**
 *
 */
public class RdsDnsResolver  extends DnsResolvers.DnsResolver {

  private static final Logger logger = Logger.getLogger( RdsDnsResolver.class );
  private static final int QUERY_ANSWER_EXPIRE_AFTER_SEC = 15;
  private static final LoadingCache<Name, List<String>> cachedAnswers = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(QUERY_ANSWER_EXPIRE_AFTER_SEC, TimeUnit.SECONDS)
      .build(new CacheLoader<Name, List<String>>() {
        @Override
        public List<String> load(final Name name) throws Exception {
          return resolveName(name);
        }
      });
  private static final DBInstances dbInstances = new PersistenceDBInstances();

  @Override
  public boolean checkAccepts( final DnsRequest request ) {
    final Record query = request.getQuery( );
    if ( !Bootstrap.isOperational( ) || !RdsDnsProperties.isResolverEnabled() ) {
      return false;
    }

    final Name rdsDomain =  getRdsSubdomain( );
    return query.getName().subdomain(rdsDomain) && !query.getName().equals(rdsDomain);
  }

  @Override
  public DnsResponse lookupRecords( final DnsRequest request ) {
    final Record query = request.getQuery( );
    try {
      final Name name = query.getName( );
      final List<String> ips = cachedAnswers.get(name);
      final List<Record> records = Lists.newArrayList( );
      for ( final String ip : ips ) {
        final InetAddress inetAddress = InetAddresses.forString( ip );
        records.add( DomainNameRecords.addressRecord(
            name,
            inetAddress,
            RdsDnsProperties.getRdsTtl( ) ) );
      }
      if(DnsResolvers.RequestType.A.apply( query )) {
        return DnsResponse.forName(name).answer(records);
      } else {
        return DnsResponse.forName(name).answer(Lists.<Record>newArrayList());
      }
    } catch ( Exception ex ) {
      logger.debug( ex );
    }
    return DnsResponse.forName( query.getName( ) ).nxdomain( );
  }

  private static Name getRdsSubdomain( ) {
    return DomainNames.absolute(
        Name.fromConstantString( RdsDnsProperties.getRdsSubdomain( ) ),
        DomainNames.externalSubdomain( ) );
  }

  private static List<String> resolveName(final Name name) {
    final Name qualifiedName = name.relativize( getRdsSubdomain( ) );
    final Option<Tuple2<String,String>> nameOption =
        RdsDnsHelper.getAccountAndIdentifierFromName(qualifiedName);
    final Set<String> ips = Sets.newTreeSet( );
    if ( nameOption.isDefined( ) ) try {
      final String accountNumber = nameOption.get()._1();
      final String dbIdentifer = nameOption.get()._2();
      final AccountFullName ownerName = AccountFullName.getInstance(accountNumber);
      final String ip = dbInstances.lookupByExample(
          DBInstance.exampleWithName(ownerName, dbIdentifer),
          ownerName,
          dbIdentifer,
          Predicates.alwaysTrue(),
          instance ->
              instance.getPubliclyAccessible() && instance.getDbInstanceRuntime().getPublicIp()!=null ?
              instance.getDbInstanceRuntime().getPublicIp() :
              instance.getDbInstanceRuntime().getPrivateIp());
      if ( ip != null ) {
        ips.add( ip );
      }
    } catch ( final RdsMetadataNotFoundException e ) {
      // OK
    } catch ( final Exception e ) {
      logger.debug(e);
    }
    return Lists.newArrayList(ips);
  }
}
