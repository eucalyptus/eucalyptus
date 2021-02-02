/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.loadbalancing.dns;

import static com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancer.Scheme;
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
