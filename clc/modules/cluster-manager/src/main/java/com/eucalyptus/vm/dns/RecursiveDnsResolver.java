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

package com.eucalyptus.vm.dns;

import static com.eucalyptus.util.dns.DnsResolvers.DnsRequest;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.Type;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.Subnets;
import com.eucalyptus.util.dns.DnsResolvers.DnsResolver;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DnsResolvers.RequestType;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;

/**
 * Implementation of a recursive resolver. The resolver works by taking whatever the QNAME and QTYPE
 * are and performing a lookup. There are two response cases:
 *
 * 1. The response type is the same as QTYPE: the returned response is passed back.
 * 2. The response type is CNAME: the target canonical name is queried starting over with
 * QNAME=CNAME and QTYPE=A. This happens repeatedly until the CNAME chain is fully resolved to the
 * root A record. The ordered set of CNAMEs followed by any A records are returned in the answer
 * section.
 *
 * All responses include corresponding NS records in the authority section and their A records in
 * the additional section.
 *
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
@ConfigurableClass( root = "dns.recursive",
                    description = "Options controlling recursive DNS resolution and caching." )
public class RecursiveDnsResolver extends DnsResolver {
  private static Logger LOG = Logger.getLogger( RecursiveDnsResolver.class );
  @ConfigurableField( description = "Enable the recursive DNS resolver.  Note: dns.enable must also be 'true'",
      initial = "true" )
  public static Boolean enabled = Boolean.TRUE;

  private static List<Name> subdomainsForName( Name name ) {
    final List<Name> names = Lists.newArrayList( name );
    final String sub = parentDomainForName( name );
    if ( sub.equals( "" ) || sub.equals( "." ) ) {
      names.add( Name.fromConstantString( "." ) );
    } else {
      names.addAll( subdomainsForName( Name.fromConstantString( sub ) ) );
    }
    return names;
  }

  private static String parentDomainForName( Name name ) {
    return name.toString( ).replaceAll( "\\A[^\\.]+\\.", "" );
  }

  private static List<Record> lookupNSRecords( Name name, Cache cache ) {
    List<Name> subdomains = subdomainsForName( name );
    for ( Name sub : subdomains ) {
      Lookup aLookup = new Lookup( sub, Type.NS );
      aLookup.setCache( cache );
      Record[] answers = aLookup.run( );
      if ( answers != null && answers.length != 0 ) {
        return Arrays.asList( answers );
      }
    }
    return Lists.newArrayList( );
  }

  @Override
  public DnsResponse lookupRecords( final DnsRequest request ) {
    final Record query = request.getQuery( );
    final Name name = query.getName( );
    final int type = query.getType( );
    final InetAddress source = request.getRemoteAddress( );
    if (! enabled || !Subnets.isSystemManagedAddress( source ))
      return DnsResponse.forName( query.getName( ) )
      .recursive( )
      .refused();

    final Cache cache = new NonExpiringCache( );
    Lookup aLookup = new Lookup( name, type );
    aLookup.setCache( cache );
    Record[] found = aLookup.run( );
    List<Record> queriedrrs = Arrays.asList( found != null
      ? found : new Record[] {} );
    List<Name> cnames = ( List<Name> ) ( aLookup.getAliases( ).length > 0
      ? Arrays.asList( aLookup.getAliases( ) ) : Lists.newArrayList( ) );
    final Set<Record> answer = Sets.newLinkedHashSet( );
    final Set<Record> authority = Sets.newLinkedHashSet( );
    final Set<Record> additional = Sets.newLinkedHashSet( );

    boolean iamAuthority = false;
    for ( Record aRec : queriedrrs ) {
      List<Record> nsRecs = lookupNSRecords( aRec.getName( ), cache );
      for ( Record nsRec : nsRecs ) {
        if(nsRec.getName().equals(DomainNames.externalSubdomain()))
          iamAuthority = true;
        authority.add( nsRec );
        Lookup nsLookup = new Lookup( ( ( NSRecord ) nsRec ).getTarget( ), type );
        nsLookup.setCache( cache );
        Record[] nsAnswers = nsLookup.run( );
        if ( nsAnswers != null ) {
          additional.addAll( Arrays.asList( nsAnswers ) );
        }
      }
    }

    for ( Name cnameRec : cnames ) {
      SetResponse sr = cache.lookupRecords( cnameRec, Type.CNAME, Credibility.ANY );
      if ( sr != null && sr.isSuccessful( ) && sr.answers( ) != null ) {
        for ( RRset result : sr.answers( ) ) {
          Iterator rrs = result.rrs( false );
          if ( rrs != null ) {
            for ( Object record : ImmutableSet.copyOf( rrs ) ) {
              answer.add( ( Record ) record );
            }
          }
        }
      }
    }

    for ( Record record : ImmutableSet.copyOf( queriedrrs ) ) {
        if ( iamAuthority && DomainNames.isExternalSubdomain( record.getName() )){
            final Name resolvedName = record.getName();
            try {
              final Name instanceDomain = InstanceDomainNames.lookupInstanceDomain( resolvedName );
              final InetAddress publicIp = InstanceDomainNames.toInetAddress( resolvedName.relativize( instanceDomain ) );
              final VmInstance vm = VmInstances.lookupByPublicIp( publicIp.getHostAddress( ) );
              final InetAddress instanceAddress = InetAddresses.forString( vm.getPrivateAddress( ) );
              final Record privateARecord = DomainNameRecords.addressRecord( resolvedName, instanceAddress );
              answer.add(privateARecord);
            } catch(final Exception ex) {
              answer.add( record );
              continue;
            }
        } else {
            answer.add( record );
        }
    }

    if((aLookup.getResult() == Lookup.SUCCESSFUL
        || aLookup.getResult() == Lookup.TYPE_NOT_FOUND )
        && queriedrrs.size()==0){
      List<Record> nsRecs = lookupNSRecords( name, cache );
      for ( Record nsRec : nsRecs ) {
        authority.add( nsRec );
      }
    }

    DnsResponse response = DnsResponse.forName( query.getName( ) )
        .recursive( )
        .withAuthority( Lists.newArrayList( authority ) )
        .withAdditional( Lists.newArrayList( additional ) )
        .answer( Lists.newArrayList( answer ) );

    if(aLookup.getResult() == Lookup.HOST_NOT_FOUND && queriedrrs.size()==0){
        response = DnsResponse.forName( query.getName( ) )
          .recursive( )
          .withAuthority( Lists.newArrayList( authority ) )
          .nxdomain();
    }
    return response;
  }

  /**
   * This resolver works when it is:
   * 1. Enabled
   * 2. The query is absolute
   * 3. The name/address is not in a Eucalyptus controlled subdomain
   *
   * @see com.eucalyptus.util.dns.DnsResolvers.DnsResolver#checkAccepts(DnsRequest)
   */
  @Override
  public boolean checkAccepts( final DnsRequest request ) {
    final Record query = request.getQuery( );
    final InetAddress source = request.getRemoteAddress( );
    if ( !Bootstrap.isOperational( ) ) {
      return false;
    } else if ( ( RequestType.A.apply( query ) || RequestType.AAAA.apply( query ) || RequestType.MX.apply(query))
                && query.getName( ).isAbsolute( )
                && !DomainNames.isSystemSubdomain( query.getName( ) ) ) {
      return true;
    } else if ( RequestType.PTR.apply( query )
                && !Subnets.isSystemManagedAddress( DomainNameRecords.inAddrArpaToInetAddress( query.getName( ) ) ) ) {
      return true;
    }
    return false;
  }

  @Override
  public int getOrder( ) {
    return DEFAULT_ORDER + 1;
  }

  @Override
  public String toString( ) {
    return this.getClass( ).getSimpleName( );
  }
}
