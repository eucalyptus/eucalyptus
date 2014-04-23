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

import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import com.eucalyptus.util.dns.DomainNameRecords;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.util.dns.DnsResolvers.DnsResolver;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DnsResolvers.RequestType;

/**
 * @author Sang-Min Park
 * 
 * The query types addressed by this resolver:
 *  -- SOA
 *
 */
public class GenericDnsResolver implements DnsResolver {
  @Override
  public boolean checkAccepts( DnsRequest request ) {
    final Record query = request.getQuery( );
    final Name name = query.getName( );
    if ( RequestType.SOA.apply( query ) ) {
      if (DomainNames.externalSubdomain().equals(name)) // zone apex
        return true;
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
    }
    return null;
  }
}
