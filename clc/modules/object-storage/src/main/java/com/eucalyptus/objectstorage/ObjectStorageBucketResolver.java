/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.objectstorage;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.util.dns.DnsResolvers.DnsRequest;
import com.eucalyptus.util.dns.DnsResolvers.DnsResolver;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DnsResolvers.RequestType;
import com.google.common.collect.Lists;

@SuppressWarnings( "unused" )
public class ObjectStorageBucketResolver extends DnsResolver {

  @Override
  public boolean checkAccepts(DnsRequest request) {
    if ( !Bootstrap.isOperational( ) ) {
      return false;
    }
    final Record query = request.getQuery( );
    return DomainNames.systemDomainFor( ObjectStorage.class, query.getName( ) ).isPresent( );
  }

  @Override
  public DnsResponse lookupRecords(DnsRequest request) {
    final Record query = request.getQuery( );
    final Name name = query.getName( );
    if ( !DomainNames.isSystemSubdomain( query.getName( ) ) ) {
      throw new NoSuchElementException( "Failed to lookup name: " + name );
    }
    if ( !RequestType.A.apply( query ) ) {
      return DnsResponse.forName( query.getName( ) ).answer( Collections.emptyList( ) );
    }
    try {
      final List<InetAddress> osgIps = ObjectStorageAddresses.getObjectStorageAddress( );
      final List<Record> records = Lists.newArrayList( );
      records.addAll( osgIps.stream( )
          .map( osgIp -> DomainNameRecords.addressRecord( name, maphost( request.getLocalAddress( ), osgIp ) ) )
          .collect( Collectors.toList( ) ) );
      return DnsResponse.forName( query.getName( ) ).answer( records );
    } catch( final Exception ex ) {
      return DnsResponse.forName( query.getName( ) ).nxdomain( );
    }
  }
  
  private static class ObjectStorageAddresses {
    static List<InetAddress> getObjectStorageAddress( ) throws EucalyptusCloudException {
      if ( Topology.isEnabled( ObjectStorage.class ) ) {
        final Iterable<ServiceConfiguration> osgs = Topology.lookupMany( ObjectStorage.class );
        final List<InetAddress> addresses = Lists.newArrayList( );
        for ( final ServiceConfiguration configuration : osgs ) {
          addresses.add( configuration.getInetAddress( ) );
        }
        Collections.shuffle( addresses );
        return addresses;
      } else {
        throw new EucalyptusCloudException("ObjectStorage not ENABLED");
      }
    }
  }

  private static InetAddress maphost( final InetAddress listenerAddress,
                                      final InetAddress hostAddress ) {
    return Hosts.maphost(
        hostAddress,
        listenerAddress,
        Internets.getInterfaceCidr( listenerAddress ).or( Cidr.of( 0, 0 ) ) );
  }
}
