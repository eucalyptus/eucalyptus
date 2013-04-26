/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util.dns;

import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.SetResponse;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse.Builder;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.net.InetAddresses;
import edu.emory.mathcs.backport.java.util.Arrays;

@ConfigurableClass( root = "system.dns.resolvers",
                    description = "Configuration options controlling the behaviour of DNS features." )
public class DnsResolvers extends ServiceJarDiscovery {
  private static Logger                                LOG       = Logger.getLogger( DnsResolvers.class );
  @ConfigurableField( description = "Enable pluggable DNS resolvers.  "
                                    + "Note: This must be 'true' for any pluggable resolver to work.  "
                                    + "Also, each resolver may need to be separately enabled." )
  public static Boolean                                enabled   = Boolean.TRUE;
  private static final ClassToInstanceMap<DnsResolver> resolvers = MutableClassToInstanceMap.create( );
  
  public enum RequestType implements Predicate<Record> {
    A( 1 ),
    NS( 2 ),
    MD( 3 ),
    MF( 4 ),
    CNAME( 5 ),
    SOA( 6 ),
    MB( 7 ),
    MG( 8 ),
    MR( 9 ),
    NULL( 10 ),
    WKS( 11 ),
    PTR( 12 ),
    HINFO( 13 ),
    MINFO( 14 ),
    MX( 15 ),
    TXT( 16 ),
    RP( 17 ),
    AFSDB( 18 ),
    X25( 19 ),
    ISDN( 20 ),
    RT( 21 ),
    NSAP( 22 ),
    NSAP_PTR( 23 ),
    SIG( 24 ),
    KEY( 25 ),
    PX( 26 ),
    GPOS( 27 ),
    AAAA( 28 ),
    LOC( 29 ),
    NXT( 30 ),
    EID( 31 ),
    NIMLOC( 32 ),
    SRV( 33 ),
    ATMA( 34 ),
    NAPTR( 35 ),
    KX( 36 ),
    CERT( 37 ),
    A6( 38 ),
    DNAME( 39 ),
    OPT( 41 ),
    APL( 42 ),
    DS( 43 ),
    SSHFP( 44 ),
    IPSECKEY( 45 ),
    RRSIG( 46 ),
    NSEC( 47 ),
    DNSKEY( 48 ),
    DHCID( 49 ),
    NSEC3( 50 ),
    NSEC3PARAM( 51 ),
    TLSA( 52 ),
    SPF( 99 ),
    TKEY( 249 ),
    TSIG( 250 ),
    IXFR( 251 ),
    AXFR( 252 ),
    MAILB( 253 ),
    MAILA( 254 ),
    ANY( 255 ),
    DLV( 32769 );
    private final int type;
    
    private RequestType( int type ) {
      this.type = type;
    }
    
    @Override
    public boolean apply( Record input ) {
      return input.getType( ) == this.type;
    }
    
  }
  
  public enum ResponseSection {
    QUESTION,
    ANSWER,
    AUTHORITY,
    ADDITIONAL,
    ZONE,
    PREREQ,
    UPDATE;
  }
  
  public enum ResponseType {
    SUCCESSFUL,
    CNAME,
    DELEGATION,
    DNAME,
    NXDOMAIN,
    NXRRSET,
    UNKNOWN;
    
    public static ResponseType lookup( SetResponse sr ) {
      if ( sr.isCNAME( ) ) {
        return CNAME;
      } else if ( sr.isDelegation( ) ) {
        return DELEGATION;
      } else if ( sr.isDNAME( ) ) {
        return DNAME;
      } else if ( sr.isNXDOMAIN( ) ) {
        return NXDOMAIN;
      } else if ( sr.isNXRRSET( ) ) {
        return NXRRSET;
      } else if ( sr.isSuccessful( ) ) {
        return SUCCESSFUL;
      } else {
        return UNKNOWN;
      }
    }
  }
  
  @SuppressWarnings( "unchecked" )
  private static void addRRset( Name name, Message response, RRset rrset, int section ) {
    for ( int s = 1; s <= section; s++ )
      if ( response.findRRset( name, rrset.getType( ), s ) )
        return;
    for ( Record r : Lists.newArrayList( ( Iterator<Record> ) rrset.rrs( ) ) ) {
      response.addRecord( r, section );
    }
  }
  
  public static class DnsResponse {
    Multimap<ResponseSection, Record> sections = ArrayListMultimap.create( );
    private final Name                name;
    
    public static class Builder {
      private final DnsResponse response;
      
      Builder( Name name ) {
        this.response = new DnsResponse( name );
      }
      
      public Builder withAuthority( Record... records ) {
        if ( records != null ) {
          this.response.sections.get( ResponseSection.AUTHORITY ).addAll( Arrays.asList( records ) );
        }
        return this;
      }
      
      public Builder withAdditional( Record... records ) {
        if ( records != null ) {
          this.response.sections.get( ResponseSection.ADDITIONAL ).addAll( Arrays.asList( records ) );
        }
        return this;
      }
      
      public DnsResponse answer( Record... records ) {
        if ( records != null ) {
          this.response.sections.get( ResponseSection.ANSWER ).addAll( Arrays.asList( records ) );
        }
        return this.response;
      }

      private static RRset createRRset( Record... records ) {
        RRset rrset = new RRset( );
        for ( Record r : records ) {
          rrset.addRR( r );
        }
        return rrset;
      }
    }
    
    private DnsResponse( Name name ) {
      this.name = name;
    }
    
    public static Builder forName( Name name ) {
      return new Builder( name );
    }
    
    public boolean hasAnswer( ) {
      return !this.sections.isEmpty( );
    }
    
    public RRset section( ResponseSection s ) {
      if ( this.sections.containsKey( s ) ) {
        return Builder.createRRset( this.sections.get( s ).toArray( new Record[] {} ) );
      } else {
        return null;
      }
    }
  }
  
  public abstract interface DnsResolver {
    public abstract boolean checkAccepts( Record query, InetAddress source );
    
    public abstract DnsResponse lookupRecords( Record query );
    
  }
  
  /**
   * Returns the list of resolvers which accept the name from the given source address.
   * 
   * @param name
   * @param source
   * @return
   */
  private static Iterable<DnsResolver> resolversFor( final Record query, final InetAddress source ) {
    return Iterables.filter( resolvers.values( ), new Predicate<DnsResolver>( ) {
      
      @Override
      public boolean apply( final DnsResolver input ) {
        try {
          return input.checkAccepts( query, source );
        } catch ( final Exception ex ) {
          return false;
        }
      }
    } );
  }
  
  private static SetResponse lookupRecords( final Message response, final Record query, final InetAddress source ) {
    final Name name = query.getName( );
    final int type = query.getType( );
    for ( final DnsResolver r : DnsResolvers.resolversFor( query, source ) ) {
      try {
        LOG.debug( "DnsResolver: " + r.getClass( ).getSimpleName( ) + " for name " + name );
        final DnsResponse reply = r.lookupRecords( query );
        if ( reply.hasAnswer( ) ) {
          for ( ResponseSection s : ResponseSection.values( ) ) {
            RRset rrset = reply.section( s );
            if ( rrset != null ) {
              addRRset( name, response, rrset, type );
            }
          }
          return SetResponse.ofType( SetResponse.SUCCESSFUL );
        }
      } catch ( final Exception ex ) {
        LOG.error( ex );
        LOG.debug( ex, ex );
      }
    }
    return SetResponse.ofType( SetResponse.UNKNOWN );//no dice, return unknown
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public boolean processClass( final Class candidate ) throws Exception {
    if ( DnsResolver.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
      try {
        final DnsResolver resolver = ( DnsResolver ) candidate.newInstance( );
        resolvers.putInstance( candidate, resolver );
        return true;
      } catch ( final Exception ex ) {
        LOG.error( "Failed to create instance of DnsResolver: " + candidate + " because of: " + ex.getMessage( ) );
      }
    }
    return false;
  }
  
  @Override
  public Double getPriority( ) {
    return 0.5d;
  }
  
  public static SetResponse findRecords( final Message response, final Record queryRecord, final InetAddress source ) {
    final Name name = queryRecord.getName( );
    final int type = queryRecord.getType( );
    try {
      if ( !enabled || !Bootstrap.isOperational( ) ) {
        return SetResponse.ofType( SetResponse.UNKNOWN );
      } else {
        final Iterable<DnsResolver> resolverList = DnsResolvers.resolversFor( queryRecord, source );
        LOG.debug( "DnsResolvers.findRecords(): resolvers for " + name + " are: " + resolverList );
        if ( Iterables.isEmpty( resolverList ) ) {
          return SetResponse.ofType( SetResponse.UNKNOWN );
        } else {
          return DnsResolvers.lookupRecords( response, queryRecord, source );
        }
      }
    } catch ( final Exception ex ) {
      LOG.error( ex );
    }
    return SetResponse.ofType( SetResponse.UNKNOWN );
  }
}
