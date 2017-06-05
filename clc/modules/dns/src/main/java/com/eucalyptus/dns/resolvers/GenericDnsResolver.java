/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
        final Record soaRec = DomainNameRecords.sourceOfAuthorityStaticSerial(name);
        return DnsResponse.forName( name ).answer( soaRec );
      }
    }else if (RequestType.ANY.apply(query)){
      if(DomainNames.externalSubdomain().equals(name)){
        final List<Record> answers = Lists.newArrayList();
        answers.add(DomainNameRecords.sourceOfAuthorityStaticSerial(name));
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
