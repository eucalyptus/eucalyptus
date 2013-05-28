/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.vm.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.Type;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Subnets;
import com.eucalyptus.util.Subnets.SystemSubnetPredicate;
import com.eucalyptus.util.dns.DnsResolvers;
import com.eucalyptus.util.dns.DnsResolvers.DnsResolver;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DnsResolvers.RequestType;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.net.InetAddresses;

@ConfigurableClass( root = "experimental.dns.split_horizon",
                    description = "Options controlling Split-Horizon DNS resolution." )
public abstract class SplitHorizonResolver implements DnsResolver {
  private static final Logger LOG              = Logger.getLogger( SplitHorizonResolver.class );
  @ConfigurableField( description = "Enable the split-horizon DNS resolution for internal instance public DNS name queries.  "
                                    + "Note: experimental.dns.enable must also be 'true'" )
  public static Boolean       enabled          = Boolean.TRUE;
  
  enum InstanceDomainName implements Function<Name, InetAddress> {
    INSTANCE;
    private static final String         DNS_TO_IP_REGEX       = "$1.$2.$3.$4";
    private static final String         DNS_REGEX             = "euca-(.+{3})-(.+{3})-(.+{3})-(.+{3}).*";
    private static final Pattern        PATTERN               = Pattern.compile( DNS_REGEX );
    private static final Supplier<Name> realInstanceSubdomain = new Supplier<Name>( ) {
                                                                
                                                                @Override
                                                                public Name get( ) {
                                                                  return DomainNames.absolute(
                                                                    Name.fromConstantString( VmInstances.INSTANCE_SUBDOMAIN.replaceFirst( "^\\.", "" ) ),
                                                                    DomainNames.externalSubdomain( ) );
                                                                }
                                                              };
    private static final Supplier<Name> instanceSubdomain     = Suppliers.memoizeWithExpiration( realInstanceSubdomain, 30, TimeUnit.SECONDS );
    
    static Matcher matcher( Name name ) {
      return PATTERN.matcher( name.toString( ) );
    }
    
    static InetAddress toInetAddress( Name name ) {
      return InetAddresses.forString( matcher( name ).replaceAll( DNS_TO_IP_REGEX ) );
    }
    
    @Override
    public InetAddress apply( Name input ) {
      try {
        final Matcher matcher = PATTERN.matcher( input.toString( ) );
        String parsedIp = matcher.replaceAll( DNS_TO_IP_REGEX );
        return InetAddress.getByName( parsedIp );
      } catch ( UnknownHostException ex ) {
        return Internets.loopback( );
      }
    }
    
    public Name get( ) {
      return instanceSubdomain.get( );
    }
  }
  
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
     * @see com.google.common.base.Pedicate#apply(java.lang.Object)
     * @return true if the address is internal
     */
    @Override
    public boolean apply( final InetAddress input ) {
      if ( !Bootstrap.isOperational( ) ) {
        return false;
      } else if ( Addresses.getInstance( ).contains( input.getHostAddress( ) ) ) {
        return true;
      } else {
        for ( final ServiceConfiguration clusterService : Components.lookup( ClusterController.class ).services( ) ) {
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
  
  /**
   * Do the split-horizon DNS lookup. The request here is necessarily from an internal instance
   * because {@link SplitHorizonResolver#checkAccepts(Record, Name, InetAddress)} only allows for
   * source addresses which are system internal.
   * 
   * The procedure is to:
   * 1. Check we can parse the subdomain; otherwise fail w/ UNKNOWN
   * 2. Parse out the ip address; otherwise fail w/ NXDOMAIN
   * 3. Verify the existence of an instance for the indicate ip; otherwise fail w/ NXDOMAIN
   * 4. Construct the response record accordingly; otherwise fail w/ NXDOMAIN
   * 
   * @see com.eucalyptus.util.dns.DnsResolvers#findRecords(Name, int, int, InetAddress)
   */
  @Override
  public abstract DnsResponse lookupRecords( Record query );
  
  public static class InternalARecordResolver extends SplitHorizonResolver implements DnsResolver {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkAccepts( Record query, InetAddress source ) {
      return super.checkAccepts( query, source ) 
             && RequestType.A.apply( query )
             && DomainNames.isInternalSubdomain( query.getName( ) )
             && InstanceDomainName.matcher( query.getName( ) ).matches( )
             && query.getName( ).subdomain( DomainNames.internalSubdomain( Eucalyptus.class ) );
    }
    
    @Override
    public DnsResponse lookupRecords( Record query ) {
      final Name name = query.getName( );
      final Name origin = DomainNames.internalSubdomain( Eucalyptus.class );
      InetAddress ip = InstanceDomainName.toInetAddress( name.relativize( origin ) );
      VmInstances.lookupByPrivateIp( ip.getHostAddress( ) );//this is an existance check and not an attempt to access the state
      return DnsResponse.forName( name )
                        .answer( DomainNameRecords.addressRecord( name, ip ) );
    }
    
  }
  
  public static class ExternalARecordResolver extends SplitHorizonResolver implements DnsResolver {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkAccepts( Record query, InetAddress source ) {
      return super.checkAccepts( query, source )
             && RequestType.A.apply( query )
             && DomainNames.isExternalSubdomain( query.getName( ) )
             && InstanceDomainName.matcher( query.getName( ) ).matches( )
             && query.getName( ).subdomain( InstanceDomainName.INSTANCE.get( ) );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see com.eucalyptus.vm.dns.SplitHorizonResolver#lookupRecords(org.xbill.DNS.Record)
     */
    @Override
    public DnsResponse lookupRecords( Record query ) {
      final Name name = query.getName( );
      InetAddress requestIp = InstanceDomainName.toInetAddress( name.relativize( InstanceDomainName.INSTANCE.get( ) ) );
      //GRZE: here it is not necessary to lookup the instance -- they public address assignment must have the needed information
      Address addr = Addresses.getInstance( ).lookup( requestIp.getHostAddress( ) );
      String instancePrivAddr = addr.getInstanceAddress( );
      //GRZE: in the case that there are different DNS views across accounts/security groups it may be necessary to look the instance up...
      //          String instanceId = addr.getInstanceId( );
      if ( addr.isAssigned( ) ) {
        return DnsResponse.forName( name )
                          .answer( DomainNameRecords.addressRecord( name, InetAddresses.forString( instancePrivAddr ) ) );
      } else {
        return DnsResponse.forName( name )
                          .answer( DomainNameRecords.addressRecord( name, requestIp ) );
      }
    }

  }
  
  /**
   * Enforces that this resolver is only used under the following conditions:
   * 1. The system is currently operational (e.g., database access is safe)
   * 2. This resolver is enabled
   * 3. The source ip address is system controlled; either a public address or in a vnet subnet
   * 4. The request name is a subdomain request for the subdomains the system should respond
   * 
   * @see com.eucalyptus.util.dns.DnsResolvers#findRecords(Name, int, int, InetAddress)
   */
  @Override
  public boolean checkAccepts( final Record query, final InetAddress source ) {
    return Bootstrap.isOperational( )
           && enabled
           && Subnets.isSystemSourceAddress( source );
  }

  @Override
  public String toString( ) {
    return this.getClass( ).getSimpleName( );
  }

  @Override
  public boolean equals( Object obj ) {
    return this.getClass( ).equals( Objects.firstNonNull( obj, Object.class ) );
  }

}
