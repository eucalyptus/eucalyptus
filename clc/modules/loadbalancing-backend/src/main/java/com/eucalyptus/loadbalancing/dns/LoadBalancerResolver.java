/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.dns;

import static com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupEntityTransform.*;
import static com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import static com.eucalyptus.util.dns.DnsResolvers.DnsRequest;
import static com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.dns.DnsResolvers;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;

/**
 *
 */
@ConfigurableClass(
    root = "services.loadbalancing",
    description = "Parameters controlling loadbalancing"
)
public class LoadBalancerResolver implements DnsResolvers.DnsResolver {

  private static final Logger logger = Logger.getLogger( LoadBalancerResolver.class );

  @ConfigurableField( description = "Enable the load balancing DNS resolver.  Note: dns.enable must also be 'true'" )
  public static Boolean dns_resolver_enabled = Boolean.TRUE;

  @Override
  public boolean checkAccepts( final DnsRequest request ) {
    final Record query = request.getQuery( );
    if ( !Bootstrap.isOperational( ) || !dns_resolver_enabled ) {
      return false;
    } else if (
        DnsResolvers.RequestType.A.apply( query ) &&
        query.getName( ).subdomain( LoadBalancerDomainName.getLoadBalancerSubdomain() ) ) {
      return true;
    }
    return false;
  }

  @Override
  public DnsResponse lookupRecords( final DnsRequest request ) {
    final Record query = request.getQuery( );
    if ( DnsResolvers.RequestType.A.apply( query ) ) {
      try {
        final Name name = query.getName( );
        final Name hostName = name.relativize( LoadBalancerDomainName.getLoadBalancerSubdomain( ) );
        final Optional<LoadBalancerDomainName> domainName = LoadBalancerDomainName.findMatching( hostName );
        if ( domainName.isPresent( ) ) {
          final Pair<String,String> accountNamePair = domainName.get( ).toScopedLoadBalancerName( hostName );
          final Set<String> ips = Sets.newTreeSet( );
          try ( final TransactionResource tx = Entities.transactionFor( LoadBalancer.class ) ) {
            final LoadBalancer loadBalancer =
                LoadBalancers.getLoadbalancer( accountNamePair.getLeft( ), accountNamePair.getRight( ) );
            final Function<LoadBalancerServoInstanceCoreView,String> ipExtractor =
                loadBalancer.getScheme( ) == LoadBalancer.Scheme.Internal ?
                    LoadBalancerServoInstanceCoreView.privateIp( ) :
                    LoadBalancerServoInstanceCoreView.address( );
            Iterables.addAll( ips, Iterables.transform(
                INSTANCE.apply( loadBalancer.getAutoScaleGroup() ).getServos(),
                ipExtractor ) );
          }
          final List<Record> records = Lists.newArrayList( );
          for ( String ip : ips ) {
            final InetAddress inetAddress = InetAddresses.forString( ip );
            records.add( DomainNameRecords.addressRecord(
                name,
                inetAddress,
                LoadBalancerDnsRecord.getLoadbalancerTTL( ) ) );
          }
          return DnsResponse.forName( name ).answer( records );
        }
      } catch ( Exception ex ) {
        logger.debug( ex );
      }
    }
    return DnsResponse.forName( query.getName( ) ).nxdomain( );
  }
}
