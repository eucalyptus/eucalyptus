/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
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

package com.eucalyptus.vm.dns;

import static com.eucalyptus.util.dns.DnsResolvers.DnsRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Subnets;
import com.eucalyptus.util.Subnets.SystemSubnetPredicate;
import com.eucalyptus.util.dns.DnsResolvers;
import com.eucalyptus.util.dns.DnsResolvers.DnsResolver;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DnsResolvers.RequestType;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.net.InetAddresses;

@ConfigurableClass( root = "dns.split_horizon",
                    description = "Options controlling Split-Horizon DNS resolution." )
public abstract class SplitHorizonResolver implements DnsResolver {
  private static final Logger LOG     = Logger.getLogger( SplitHorizonResolver.class );
  @ConfigurableField( description = "Enable the split-horizon DNS resolution for internal instance public DNS name queries.  "
                                    + "Note: dns.enable must also be 'true'" )
  public static Boolean       enabled = Boolean.TRUE;
  
  /**
   * Test whether the address is one which belongs to an instance or is external.
   * 
   * The checks are as follows:
   * 1. If the system has not yet fully bootstrapped, return false.
   * 2. If the address is a registered public address, return true.
   * 3. If the address falls w/in a subnet owned by a registered cluster, return true.
   * 4. Otherwise, return false.
   */
  @SystemSubnetPredicate
  public static class SystemInternalSubnet implements Predicate<InetAddress> {
    
    /**
     * @see com.google.common.base.Predicate#apply(Object)
     * @return true if the address is internal
     */
    @Override
    public boolean apply( final InetAddress input ) {
      if ( !Bootstrap.isOperational( ) ) {
        return false;
      } else if ( Addresses.getInstance( ).contains( input.getHostAddress( ) ) ) {
        return true;
      } else {
        try {
          VmInstances.lookupByPublicIp( input.getHostAddress( ) );
          return true;
        } catch ( NoSuchElementException ex1 ) {
          for ( final ServiceConfiguration clusterService : ServiceConfigurations.list( ClusterController.class ) ) {
            final ClusterConfiguration cluster = ( ClusterConfiguration ) clusterService;
            try {
              if ( Subnets.internalPredicate( cluster.getVnetSubnet( ), cluster.getVnetNetmask( ) ).apply( input ) ) {
                return true;
              }
            } catch ( final UnknownHostException ex ) {
              LOG.trace( ex );
            }
          }
          return false;
        }
      }
    }
    
  }
  
  /**
   * Do the split-horizon DNS lookup. The request here is necessarily from an internal instance
   * because {@link SplitHorizonResolver#checkAccepts(DnsRequest)} only allows for
   * source addresses which are system internal.
   * 
   * The procedure is to:
   * 1. Check we can parse the subdomain; otherwise fail w/ UNKNOWN
   * 2. Parse out the ip address; otherwise fail w/ NXDOMAIN
   * 3. Verify the existence of an instance for the indicate ip; otherwise fail w/ NXDOMAIN
   * 4. Construct the response record accordingly; otherwise fail w/ NXDOMAIN
   * 
   * @see DnsResolvers#findRecords(org.xbill.DNS.Message, DnsResolvers.DnsRequest)
   */
  @Override
  public DnsResponse lookupRecords( DnsRequest request ) {
    final Record query = request.getQuery( );
    if ( RequestType.PTR.apply( query ) ) {
      final InetAddress ip = DomainNameRecords.inAddrArpaToInetAddress( query.getName( ) );
      if ( InstanceDomainNames.isInstance( ip ) ) {
        final String hostAddress = ip.getHostAddress( );
        if ( Addresses.getInstance( ).contains( hostAddress ) ) {
          VmInstances.lookupByPublicIp( hostAddress );//existence check
          final Name dnsName = InstanceDomainNames.fromInetAddress( InstanceDomainNames.EXTERNAL, ip );
          return DnsResponse.forName( query.getName( ) ).answer( DomainNameRecords.ptrRecord( dnsName, ip ) );
        } else {
          VmInstances.lookupByPrivateIp( hostAddress );//existence check
          final Name dnsName = InstanceDomainNames.fromInetAddress( InstanceDomainNames.INTERNAL, ip );
          return DnsResponse.forName( query.getName( ) ).answer( DomainNameRecords.ptrRecord( dnsName, ip ) );
        }
      }
    }
    return DnsResponse.forName( query.getName( ) ).nxdomain( );
  }
  
  public static class InternalARecordResolver extends SplitHorizonResolver implements DnsResolver {
    
    @Override
    public DnsResponse lookupRecords( DnsRequest request ) {
      final Record query = request.getQuery( );
      if ( RequestType.A.apply( query ) ) {
        try {
          final Name name = query.getName( );
          final Name instanceDomain = InstanceDomainNames.lookupInstanceDomain( name );
          final InetAddress ip = InstanceDomainNames.toInetAddress( name.relativize( instanceDomain ) );
          VmInstances.lookupByPrivateIp( ip.getHostAddress( ) );//GRZE: existence check
          final Record aRecord = DomainNameRecords.addressRecord( name, ip );
          return DnsResponse.forName( name ).answer( aRecord );
        } catch ( Exception ex ) {
          LOG.debug( ex );
        }
      }
      return super.lookupRecords( request );
    }
    
    @Override
    public boolean checkAccepts( DnsRequest request ) {
      final Record query = request.getQuery( );
      return RequestType.PTR.apply( query ) ?
        super.checkAccepts( request ) :
        super.checkAccepts( request )
            && ( InstanceDomainNames.isInstanceSubdomain( query.getName( ) )
            && !query.getName( ).subdomain( InstanceDomainNames.EXTERNAL.get( ) ) );
    }
    
  }
  
  public static class HorizonARecordResolver extends SplitHorizonResolver implements DnsResolver {
    
    @Override
    public boolean checkAccepts( DnsRequest request ) {
      final Record query = request.getQuery( );
      final InetAddress source = request.getRemoteAddress( );
      return RequestType.PTR.apply( query ) ?
        super.checkAccepts( request ) :
        super.checkAccepts( request )
            && Subnets.isSystemManagedAddress( source )
            && query.getName( ).subdomain( InstanceDomainNames.EXTERNAL.get( ) );
    }
    
    @Override
    public DnsResponse lookupRecords( DnsRequest request ) {
      final Record query = request.getQuery( );
      if ( RequestType.A.apply( query ) ) {
        try {
          final Name name = query.getName( );
          final InetAddress requestIp = InstanceDomainNames.toInetAddress( name.relativize( InstanceDomainNames.EXTERNAL.get( ) ) );
          //GRZE: here it is not necessary to lookup the instance -- they public address assignment must have the needed information
          final VmInstance vm = VmInstances.lookupByPublicIp( requestIp.getHostAddress( ) );
          final InetAddress instanceAddress = InetAddresses.forString( vm.getPrivateAddress( ) );
          final Record instanceARecord = DomainNameRecords.addressRecord( name, instanceAddress );
          return DnsResponse.forName( name ).answer( instanceARecord );
        } catch ( Exception ex ) {
          LOG.debug( ex );
        }
      }
      return super.lookupRecords( request );
    }
    
  }
  
  public static class ExternalARecordResolver extends SplitHorizonResolver implements DnsResolver {
    @Override
    public boolean checkAccepts( DnsRequest request ) {
      final Record query = request.getQuery( );
      final InetAddress source = request.getRemoteAddress( );
      return RequestType.PTR.apply( query ) ?
        super.checkAccepts( request ) :
        super.checkAccepts( request )
            && !Subnets.isSystemManagedAddress( source )
            && query.getName( ).subdomain( InstanceDomainNames.EXTERNAL.get( ) );
    }
    
    @Override
    public DnsResponse lookupRecords( DnsRequest request ) {
      final Record query = request.getQuery( );
      if ( RequestType.A.apply( query ) ) {
        try {
          final Name name = query.getName( );
          final InetAddress requestIp = InstanceDomainNames.toInetAddress( name.relativize( InstanceDomainNames.EXTERNAL.get( ) ) );
          VmInstances.lookupByPublicIp( requestIp.getHostAddress( ) ); // Ensure used by instance
          final Record instanceARecord = DomainNameRecords.addressRecord( name, requestIp );
          return DnsResponse.forName( name ).answer( instanceARecord );
        } catch ( Exception ex ) {
          LOG.debug( ex );
        }
      }
      return super.lookupRecords( request );
    }
  }
  
  /**
   * Enforces that this resolver is only used under the following conditions:
   * 1. The system is currently operational (e.g., database access is safe)
   * 2. This resolver is enabled
   * 3. The source ip address is system controlled; either a public address or in a vnet subnet
   * 4. The request name is a subdomain request for the subdomains the system should respond
   *
   * @see com.eucalyptus.util.dns.DnsResolvers#findRecords(org.xbill.DNS.Message, DnsRequest)
   */
  @Override
  public boolean checkAccepts( final DnsRequest request ) {
    final Record query = request.getQuery( );
    if ( !Bootstrap.isOperational( ) || !enabled ) {
      return false;
    } else if ( RequestType.A.apply( query ) && InstanceDomainNames.isInstanceDomainName( query.getName( ) ) ) {
      return true;
    } else if ( RequestType.PTR.apply( query )
                && Subnets.isSystemManagedAddress( DomainNameRecords.inAddrArpaToInetAddress( query.getName( ) ) ) ) {
      return true;
    }
    return false;
  }
  
  @Override
  public String toString( ) {
    return this.getClass( ).getSimpleName( );
  }
  
  @SuppressWarnings( "EqualsWhichDoesntCheckParameterClass" )
  @Override
  public boolean equals( Object obj ) {
    return this.getClass( ).equals( Objects.firstNonNull( Classes.typeOf( obj ), Object.class ) );
  }
  
}
