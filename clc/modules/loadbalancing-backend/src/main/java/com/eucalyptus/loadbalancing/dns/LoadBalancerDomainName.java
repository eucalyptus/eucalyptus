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

import static com.eucalyptus.loadbalancing.LoadBalancer.Scheme;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.xbill.DNS.Name;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.dns.DomainNames;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

/**
 *
 */
public enum LoadBalancerDomainName implements Predicate<Name> {

  INTERNAL( "internal-([a-zA-Z0-9-]{1,255})-([0-9]{12})", "internal-%s-%s" ), // most specific first for matching
  EXTERNAL( "([a-zA-Z0-9-]{1,255})-([0-9]{12})", "%s-%s" ),
  ;

  private final Pattern hostPattern;
  private final String hostFormat;

  private LoadBalancerDomainName( final String pattern, final String format ) {
    hostPattern = Pattern.compile( pattern );
    hostFormat = format;
  }

  public static Name getLoadBalancerSubdomain( ) {
    return DomainNames.absolute(
        lookupLoadBalancerSubdomainProperty( ),
        DomainNames.externalSubdomain( ) );
  }

  public static LoadBalancerDomainName forScheme( @Nullable final Scheme scheme ) {
    if ( Scheme.Internal == scheme ) {
      return INTERNAL;
    }
    return EXTERNAL;
  }

  public static Optional<LoadBalancerDomainName> findMatching( final Name name ) {
    for ( final LoadBalancerDomainName domainName : values( ) ) {
      if ( domainName.apply( name ) ) {
        return Optional.of( domainName );
      }
    }
    return Optional.absent( );
  }

  public String generate( String loadBalancerName, String accountNumber ) {
    final String dnsPrefix = String.format( hostFormat, loadBalancerName, accountNumber );
    return Joiner.on('.').join( dnsPrefix, getLoadBalancerSubdomain( ).relativize( Name.root ) );
  }

  private Matcher matcher( final Name name ) {
    return hostPattern.matcher( name.toString( ) );
  }

  /**
   * Extract info from a known good name.
   *
   * @param name The DNS name
   * @return A pair of account number and load balancer name
   */
  public Pair<String,String> toScopedLoadBalancerName( final Name name ) {
    final Matcher nameMatcher = matcher( name );
    nameMatcher.matches( );
    return Pair.pair( nameMatcher.group( 2 ), nameMatcher.group( 1 ) );
  }

  private static Name lookupLoadBalancerSubdomainProperty( ) {
    return Name.fromConstantString( LoadBalancerDnsRecord.DNS_SUBDOMAIN );
  }

  @Override
  public boolean apply( @Nullable final Name name ) {
    return name != null && matcher( name ).matches( );
  }
}
