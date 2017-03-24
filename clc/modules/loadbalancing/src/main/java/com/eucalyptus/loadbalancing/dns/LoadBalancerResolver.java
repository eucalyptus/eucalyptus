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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.dns.DnsResolvers;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
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
public class LoadBalancerResolver extends DnsResolvers.DnsResolver {

  private static final Logger logger = Logger.getLogger( LoadBalancerResolver.class );
  private static final int QUERY_ANSWER_EXPIRE_AFTER_SEC = 5;
  private static final LoadingCache<Name, List<String>> cachedAnswers =   CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(QUERY_ANSWER_EXPIRE_AFTER_SEC, TimeUnit.SECONDS)
      .build(new CacheLoader<Name, List<String>> () {
        @Override
        public List<String> load(final Name name) throws Exception {
          return resolveName(name);
        }
      });
  
  /// the LoadingCache is used to set the hard limit on memory usage
  private static final LoadingCache<Name, IpPermutation> nameToIpPermutations = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterAccess(1, TimeUnit.HOURS)
      .build(new CacheLoader<Name, IpPermutation> () {
        @Override
        public IpPermutation load(Name name) throws Exception {
          final List<String> ips = cachedAnswers.get(name);
          return new IpPermutation(ips);
        }
      });
  
  @ConfigurableField( description = "Enable the load balancing DNS resolver.  Note: dns.enable must also be 'true'", initial = "true" )
  public static Boolean dns_resolver_enabled = Boolean.TRUE;

  @Override
  public boolean checkAccepts( final DnsRequest request ) {
    final Record query = request.getQuery( );
    if ( !Bootstrap.isOperational( ) || !dns_resolver_enabled ) {
      return false;
    } else if ( query.getName( ).subdomain( LoadBalancerDomainName.getLoadBalancerSubdomain() ) ) {
      return true;
    }
    return false;
  }

  private static List<String> resolveName(final Name name) {
    final Name hostName = name.relativize( LoadBalancerDomainName.getLoadBalancerSubdomain( ) );
    final Optional<LoadBalancerDomainName> domainName = LoadBalancerDomainName.findMatching( hostName );
    final Set<String> ips = Sets.newTreeSet( );
    if ( domainName.isPresent( ) ) {
      final Pair<String,String> accountNamePair = domainName.get( ).toScopedLoadBalancerName( hostName );
      try ( final TransactionResource tx = Entities.transactionFor( LoadBalancer.class ) ) {
        final LoadBalancer loadBalancer =
            LoadBalancers.getLoadbalancerCaseInsensitive( accountNamePair.getLeft( ), accountNamePair.getRight( ) );
        final Predicate<LoadBalancerServoInstanceCoreView> canResolve = 
            new Predicate<LoadBalancerServoInstanceCoreView>(){
          @Override
          public boolean apply(LoadBalancerServoInstanceCoreView arg0) {
            return arg0.canResolveDns();
          }
        };

        final List<LoadBalancerServoInstanceCoreView> servos = Lists.newArrayList();
        for(final LoadBalancerAutoScalingGroupCoreView group : loadBalancer.getAutoScaleGroups()) {
          servos.addAll(INSTANCE.apply( group ).getServos());
        }
        final Function<LoadBalancerServoInstanceCoreView,String> ipExtractor =
            loadBalancer.getScheme( ) == LoadBalancer.Scheme.Internal ?
                LoadBalancerServoInstanceCoreView.privateIp( ) :
                  LoadBalancerServoInstanceCoreView.address( );
                Iterables.addAll( ips, Iterables.transform(
                    Collections2.filter(
                        servos,
                        canResolve),
                        ipExtractor ) );
      }
    }

    List<String> ipList = Lists.newArrayList(ips);
    Collections.sort(ipList);
    return ipList;
  }
  
  private static class IpPermutation {
    private List<String> ips = null;
    private List<List<String>> ipPermutations = null;
    private int idx = 0;
    private IpPermutation(final List<String> ips) {
      this.ips = ips;
      ipPermutations = Lists.newArrayList(Collections2.permutations(ips));
      Collections.shuffle(ipPermutations);
    }
   
    public synchronized List<String> getNext() {
      try{
        final List<String> next = ipPermutations.get(idx);
        if (++idx >= ipPermutations.size())
          idx = 0;
        return next;
      }catch(final Exception ex) {
        return Lists.newArrayList();
      }
    }
    
    public boolean isPermuatationFrom(final List<String> ips) {
      return this.ips.equals(ips);
    }
  }
  
  private static List<String> getIps (final Name name) throws ExecutionException{
    final List<String> ips = cachedAnswers.get(name);
    final IpPermutation old = nameToIpPermutations.get(name);
    if(!old.isPermuatationFrom(ips))
      nameToIpPermutations.invalidate(name);
    return nameToIpPermutations.get(name).getNext();
  }
  
  @Override
  public DnsResponse lookupRecords( final DnsRequest request ) {
    final Record query = request.getQuery( );
    try {
      final Name name = query.getName( );
      final List<String> ips = getIps(name);
      final List<Record> records = Lists.newArrayList( );
      for ( String ip : ips ) {
        final InetAddress inetAddress = InetAddresses.forString( ip );
        records.add( DomainNameRecords.addressRecord(
            name,
            inetAddress,
            LoadBalancerDnsRecord.getLoadbalancerTTL( ) ) );
      }
      if(DnsResolvers.RequestType.A.apply( query ))
        return DnsResponse.forName( name ).answer( records );
      else
        return DnsResponse.forName( name ).answer(Lists.<Record>newArrayList());
    } catch ( Exception ex ) {
      
      logger.debug( ex );
    }
    return DnsResponse.forName( query.getName( ) ).nxdomain( );
  }
}
