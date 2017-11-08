/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
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
import com.eucalyptus.objectstorage.util.OSGUtil;
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
    return OSGUtil.isBucketName( query.getName( ), false );
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
