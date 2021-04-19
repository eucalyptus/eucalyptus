/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.dns.resolvers;

import static com.eucalyptus.util.dns.DnsResolvers.DnsRequest;

import java.util.List;
import java.util.NavigableSet;
import java.util.function.Predicate;

import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.util.dns.DnsResolvers.DnsResolver;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DnsResolvers.RequestType;
import com.eucalyptus.util.dns.NameserverResolver;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 * 
 * The query types addressed by this resolver:
 *  -- SOA
 *
 */
public class GenericDnsResolver extends DnsResolver {
  @Override
  public boolean checkAccepts( DnsRequest request ) {
    final Record query = request.getQuery( );
    final Name name = query.getName( );
    // zone apex
    if (DomainNames.externalSubdomain().equals(name)) {
      if ( RequestType.ANY.apply(query) || RequestType.SOA.apply( query ) )
        return true;
      else
        return false;
    } else
      return false;
  }

  @Override
  public DnsResponse lookupRecords( DnsRequest request ) {
    final Record query = request.getQuery( );
    final Name name = query.getName( );
    if( RequestType.SOA.apply(query)){
      if(DomainNames.externalSubdomain().equals(name)){
        final Record soaRec = DomainNameRecords.sourceOfAuthority(name);
        return DnsResponse.forName( name ).answer( soaRec );
      }
    }else if (RequestType.ANY.apply(query)){
      if(DomainNames.externalSubdomain().equals(name)){
        final List<Record> answers = Lists.newArrayList();
        answers.add(DomainNameRecords.sourceOfAuthority(name));
        final Predicate<ServiceConfiguration> nsServerUsable = DomainNameRecords.activeNameserverPredicate( );
        NavigableSet<ServiceConfiguration> nsServers = Components.lookup( Dns.class ).services( );
        List<Record> additional = Lists.newArrayList( );
        Name domain = DomainNames.isInternalSubdomain( name ) ? DomainNames.internalSubdomain( ) : DomainNames.externalSubdomain( );
        int idx = 1;
        for ( ServiceConfiguration conf : nsServers ) {
          final int offset = idx++;
          if ( nsServerUsable.test( conf ) ) {
            additional.add( DomainNameRecords.addressRecord(
                Name.fromConstantString( "ns" + offset + "." + domain ),
                NameserverResolver.maphost( request.getLocalAddress( ), conf.getInetAddress( ) ) ) );
          }
        }
        answers.addAll(DomainNameRecords.nameservers( name ));
        return DnsResponse.forName( name )
                          .withAdditional( additional )
                          .answer(answers );
      }
    }
    return null;
  }
}
