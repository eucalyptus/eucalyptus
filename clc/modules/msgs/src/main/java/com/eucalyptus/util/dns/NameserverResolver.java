/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util.dns;

import static com.eucalyptus.util.dns.DnsResolvers.DnsRequest;

import java.net.InetAddress;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Subnets;
import com.eucalyptus.util.dns.DnsResolvers.DnsResolver;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DnsResolvers.RequestType;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

@ConfigurableClass( root = "dns.ns",
                    description = "Options controlling DNS name resolution for the system's nameservers." )
public class NameserverResolver extends DnsResolver {
  @ConfigurableField( description = "Enable the NS resolver.  Note: dns.enable must also be 'true'", initial = "true" )
  public static Boolean enabled = Boolean.TRUE;

  private static final Function<InetAddress,Cidr> cidrLookup = CacheBuilder.newBuilder( )
      .maximumSize( 64 )
      .expireAfterWrite( 1, TimeUnit.MINUTES )
      .build( CacheLoader.from( Functions.compose(
          CollectionUtils.optionalOr( Cidr.of( 0, 0 ) ),
          Internets.interfaceCidr( ) ) ) );

  @Override
  public boolean checkAccepts( final DnsRequest request ) {
    final Record query = request.getQuery( );
    final Name name = query.getName( );
    if ( !Bootstrap.isOperational( ) || !enabled || !DomainNames.isSystemSubdomain( name ) ) {
      return false;
    } else if ( RequestType.A.apply( query ) ) {
      return name.getLabelString( 0 ).matches( "ns(?:[1-9][0-9]*)?" );
    } else if ( RequestType.NS.apply( query ) ) {
      return true;
    } else {
      return false;
    }
  }
  
  @Override
  public DnsResponse lookupRecords( DnsRequest request ) {
    final Record query = request.getQuery( );
    Name name = query.getName( );
    if ( RequestType.A.apply( query ) && DomainNames.isSystemSubdomain( name ) ) {
      String label0 = name.getLabelString( 0 );
      if ( name.equals( Name.fromConstantString( label0 + "." + DomainNames.internalSubdomain( ) ) )
           || name.equals( Name.fromConstantString( label0 + "." + DomainNames.externalSubdomain( ) ) ) ) {
        NavigableSet<ServiceConfiguration> nsServers = Components.lookup( Dns.class ).services( );
        Integer index = MoreObjects.firstNonNull( Ints.tryParse( label0.substring( 2 ) ), 1 );
        if ( nsServers.size( ) >= index ) {
          ServiceConfiguration conf = nsServers.toArray( new ServiceConfiguration[] {} )[index-1];
          final Record addressRecord = DomainNameRecords.addressRecord(
              query.getName( ),
              maphost( request.getLocalAddress( ), conf.getInetAddress( ) ) );
          return DnsResponse.forName( name ).answer( addressRecord );
        }
      }
    } else if ( RequestType.NS.apply( query ) ) {
      final NavigableSet<ServiceConfiguration> nsServers = Components.lookup( Dns.class ).services( );
      final Predicate<ServiceConfiguration> nsServerUsable = DomainNameRecords.activeNameserverPredicate( );
      final Name domain = DomainNames.isInternalSubdomain( name ) ? DomainNames.internalSubdomain( ) : DomainNames.externalSubdomain( );
      final List<Record> aRecs = Lists.newArrayList( );
      int idx = 1;
      for ( final ServiceConfiguration conf : nsServers ) {
        final int offset = idx++;
        if ( nsServerUsable.test( conf ) ) {
          aRecs.add( DomainNameRecords.addressRecord(
              Name.fromConstantString( "ns" + offset + "." + domain ) ,
              maphost( request.getLocalAddress( ), conf.getInetAddress( ) ) ) );
        }
      }
      return DnsResponse.forName( name )
                        .withAdditional( aRecs )
                        .answer( DomainNameRecords.nameservers( name ) );
    }
    return null;
  }
  
  @Override
  public String toString( ) {
    return this.getClass( ).getSimpleName( );
  }


  @SuppressWarnings( "unused" )
  public static class NameserverReverseResolver extends DnsResolver {
    
    @Override
    public boolean checkAccepts( DnsRequest request ) {
      final Record query = request.getQuery( );
      return RequestType.PTR.apply( query ) && Subnets.isSystemHostAddress( DomainNameRecords.inAddrArpaToInetAddress( query.getName( ) ) );
    }
    
    @Override
    public DnsResponse lookupRecords( DnsRequest request ) {
      final Record query = request.getQuery( );
      final InetAddress hostAddr = DomainNameRecords.inAddrArpaToInetAddress( query.getName( ) );
      final Host host = Hosts.lookup( hostAddr );
      if ( host != null ) {
        final String hostAddress = host.getBindAddress( ).getHostAddress( );
        final NavigableSet<ServiceConfiguration> nsServers = Components.lookup( Dns.class ).services( );
        final Option<ServiceConfiguration> hostConfiguration =
            Stream.ofAll( nsServers ).find( configuration -> hostAddress.equals( configuration.getHostName( ) ) );
        if ( hostConfiguration.isDefined( ) ) {
          int index = nsServers.headSet( hostConfiguration.get( ) ).size( );
          final Name nsName = Name.fromConstantString( "ns" + (index+1) + "." + DomainNames.externalSubdomain( ) );
          final Record ptrRecord = DomainNameRecords.ptrRecord( nsName, hostAddr );
          return DnsResponse.forName( query.getName( ) ).answer( ptrRecord );
        }
      }
      // EUCA-10245: return zero answer so that the next reverse resolver would answer
      return DnsResponse.forName( query.getName( ) ).answer();
    }
  }

  @SuppressWarnings( "ConstantConditions" )
  public static InetAddress maphost( final InetAddress listenerAddress,
                                     final InetAddress hostAddress ) {
    InetAddress result = hostAddress;
    final Cidr cidr = cidrLookup.apply( listenerAddress );
    if ( !cidr.apply( result ) ) {
      final Host host = Hosts.lookup( hostAddress );
      if ( host != null ) {
        result = Iterables.tryFind( host.getHostAddresses( ), cidr ).or( result );
      }
    }
    return result;
  }
}
