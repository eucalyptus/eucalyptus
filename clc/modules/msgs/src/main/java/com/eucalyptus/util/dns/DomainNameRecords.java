/*************************************************************************
 * Copyright 2008 Regents of the University of California
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

package com.eucalyptus.util.dns;

import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.SOARecord;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.google.common.net.InetAddresses;

/**
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
@ConfigurableClass( root = "dns",
                    description = "Configuration options controlling the behaviour of DNS features." )
public class DomainNameRecords {
  @ConfigurableField( description = "Time-to-live for all authoritative records" )
  private final static long TTL = 60L;
  @ConfigurableField( description = "Time-to-live for negative caching on authoritative records" )
  private final static long NEGATIVE_TTL = 5L;
  
  public static long ttl( ) {
    return TTL;
  }
  
  public static long negativeTtl( ) {
    return NEGATIVE_TTL;
  }

  public static Predicate<ServiceConfiguration> activeNameserverPredicate( ) {
    final Collection<ServiceConfiguration> enabledNsServers = Topology.enabledServices( Dns.class );
    return conf -> enabledNsServers.contains( conf ) || conf.isHostLocal( );
  }

  public static List<? extends Record> nameservers( Name subdomain ) {
    return DomainNames.nameServerRecords( subdomain );
  }
  
  public static SOARecord sourceOfAuthority( Name name ) {
    final Name soa = DomainNames.sourceOfAuthority( name );
    final Name ns = DomainNames.nameServerRecords( name ).get( 0 ).getTarget( );
    SOARecord soaRecord = new SOARecord( name, DClass.IN, TTL, ns,
                                         Name.fromConstantString( "root." + soa ),
                                         DomainNameRecords.serial( ), 1200L, 180L, 2419200L, NEGATIVE_TTL );
    return soaRecord;
  }
  
  public static SOARecord sourceOfAuthorityStaticSerial( Name name ) {
    final Name soa = DomainNames.sourceOfAuthority( name );
    final Name ns = DomainNames.nameServerRecords( name ).get( 0 ).getTarget( );
    final SOARecord soaRecord = new SOARecord( name, DClass.IN, TTL, ns,
        Name.fromConstantString( "root." + soa ),
        1, 3600L, 600L, 86400L, 3600L);
    return soaRecord;
  }
  
  public static Record canonicalName( Name aliasName, Name canonicalName ) {
    return new CNAMERecord( aliasName, DClass.IN, TTL, canonicalName );
  }
  
  public static InetAddress inAddrArpaToInetAddress( Name name ) {
    final String ipString = new StringBuffer( ).append( name.getLabelString( 3 ) )
                                                     .append( "." )
                                                     .append( name.getLabelString( 2 ) )
                                                     .append( "." )
                                                     .append( name.getLabelString( 1 ) )
                                                     .append( "." )
                                                     .append( name.getLabelString( 0 ) )
                                                     .toString( );
    return InetAddresses.forString( ipString );
  }
  
  public static Record ptrRecord( Name name, Name inAddrArpa ) {
    return ptrRecord( name, inAddrArpaToInetAddress( inAddrArpa ) );
  }

  public static Record ptrRecord( Name name, InetAddress ip ) {
    return new PTRRecord( ReverseMap.fromAddress( ip ), DClass.IN, TTL, name );
  }
  
  public static Record addressRecord( Name name, InetAddress ip ) {
    return addressRecord( name, ip, TTL );
  }

  public static Record addressRecord( Name name, InetAddress ip, long ttl ) {
    return new ARecord( name, DClass.IN, ttl, ip );
  }

  static long serial( ) {
    return Long.parseLong( DomainNameRecords.SERIALFORMATTER.format( new Date( ) ) );
  }
  
  static final DateFormat SERIALFORMATTER = new SimpleDateFormat( "yyMMddHHmm" );
  
}
