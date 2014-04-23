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
 ************************************************************************ 
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.compute.metadata;

import static com.eucalyptus.util.dns.DnsResolvers.DnsRequest;
import java.net.InetAddress;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.Subnets;
import com.eucalyptus.util.dns.DnsResolvers.DnsResolver;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DnsResolvers.RequestType;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.vm.dns.InstanceDomainNames;

@ConfigurableClass( root = "dns.instancedata",
                    description = "Options controlling DNS name resolution for the instance metadata service." )
public class InstanceDataDnsResolver implements DnsResolver {
  @ConfigurableField( description = "Enable the instance-data resolver.  Note: dns.enable must also be 'true'" )
  public static Boolean            enabled                = Boolean.TRUE;
  private static final Name        INSTANCE_DATA          = Name.fromConstantString( "instance-data." );
  private static final Name        RELATIVE_INSTANCE_DATA = INSTANCE_DATA.relativize( Name.fromConstantString( "." ) );
  private static final Name        INSTANCE_PTR           = Name.fromConstantString( "254.169.254.169.in-addr.arpa." );
  private static final InetAddress METADATA_ADDR          = DomainNameRecords.inAddrArpaToInetAddress( INSTANCE_PTR );
  private static final Record      PTR_RECORD             = DomainNameRecords.ptrRecord( INSTANCE_DATA, METADATA_ADDR );
  private static final Record      ADDRESS_RECORD         = DomainNameRecords.addressRecord( INSTANCE_DATA, METADATA_ADDR );
  
  @Override
  public boolean checkAccepts( DnsRequest request ) {
    final Record query = request.getQuery( );
    final InetAddress source = request.getRemoteAddress( );
    final Name name = query.getName( );
    if ( !Bootstrap.isOperational( ) || !enabled || !Subnets.isSystemManagedAddress( source ) ) {
      return false;
    } else if ( RequestType.A.apply( query ) ) {
      if ( INSTANCE_DATA.equals( name ) ) {
        return true;
      } else if ( name.subdomain( DomainNames.internalSubdomain( ) )
                  && RELATIVE_INSTANCE_DATA.equals( DomainNames.relativize( name, DomainNames.internalSubdomain( ) ) ) ) {
        return true;
      } else if ( name.subdomain( DomainNames.internalSubdomain( Eucalyptus.class ) )
                  && RELATIVE_INSTANCE_DATA.equals( DomainNames.relativize( name, DomainNames.internalSubdomain( Eucalyptus.class ) ) ) ) {
        return true;
      } else if ( InstanceDomainNames.isInstanceSubdomain( name )
                  && RELATIVE_INSTANCE_DATA.equals( DomainNames.relativize( name, InstanceDomainNames.lookupInstanceDomain( name ) ) ) ) {
        return true;
      }
    } else if ( RequestType.PTR.apply( query ) ) {
      return name.equals( INSTANCE_PTR );
    }
    return false;
  }
  
  @Override
  public DnsResponse lookupRecords( DnsRequest request ) {
    final Record query = request.getQuery( );
    final Name name = query.getName( );
    if ( RequestType.A.apply( query ) ) {
      final String label0 = name.getLabelString( 0 );
      if ( INSTANCE_DATA.getLabelString( 0 ).equals( label0 ) ) {
        return DnsResponse.forName( name )
                          .answer( ADDRESS_RECORD );
      }
    } else if ( RequestType.PTR.apply( query ) && INSTANCE_PTR.equals( query.getName( ) ) ) {
      return DnsResponse.forName( name )
                        .answer( PTR_RECORD );
    }
    return null;
  }
  
  @Override
  public String toString( ) {
    return this.getClass( ).getSimpleName( );
  }
}
