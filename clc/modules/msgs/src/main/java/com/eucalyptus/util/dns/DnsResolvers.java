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

package com.eucalyptus.util.dns;

import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.core.OrderComparator;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SetResponse;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.Ordered;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MutableClassToInstanceMap;

@ConfigurableClass( root = "dns",
                    description = "Configuration options controlling the behaviour of DNS features." )
public class DnsResolvers extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( DnsResolvers.class );
  @ConfigurableField( description = "Enable pluggable DNS resolvers.  "
                                    + "Note: This must be 'true' for any pluggable resolver to work.  "
                                    + "Also, each resolver may need to be separately enabled."
                                    + "See 'euca-describe-properties dns'.", initial = "true" )
  public static Boolean enabled = Boolean.TRUE;
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
    
    private static final Supplier<Map<Integer, RequestType>> backingMap = new Supplier( ) {
      
      @Override
      public Map<Integer, RequestType> get( ) {
        return new HashMap( ) {
          {
            for ( RequestType t : RequestType.values( ) ) {
              this.put( t.getType( ), t );
            }
          }
        };
      }
    };
    private static final Supplier<Map<Integer, RequestType>> typeMap = Suppliers.memoize( backingMap );
    private final int type;
    
    private RequestType( int type ) {
      this.type = type;
    }
    
    @Override
    public boolean apply( Record input ) {
      return input.getType( ) == this.type;
    }
    
    public static RequestType typeOf( int type ) {
      if ( !typeMap.get( ).containsKey( type ) ) {
        throw new IllegalArgumentException( "No RequestType with type=" + type );
      } else {
        return typeMap.get( ).get( type );
      }
    }
    
    public int getType( ) {
      return this.type;
    }
    
  }
  
  public enum ResponseSection {
    QUESTION,
    ANSWER,
    AUTHORITY,
    ADDITIONAL,
    ZONE {
      
      @Override
      public int section( ) {
        return 0;
      }
    },
    PREREQ {
      
      @Override
      public int section( ) {
        return 1;
      }
      
    },
    UPDATE {
      
      @Override
      public int section( ) {
        return 2;
      }
      
    };
    public int section( ) {
      return this.ordinal( );
    }
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
  
  private static RRset createRRset( Record... records ) {
    RRset rrset = new RRset( );
    for ( Record r : records ) {
      rrset.addRR( r );
    }
    return rrset;
  }
  
  @SuppressWarnings( "unchecked" )
  private static void addRRset( Name name,
                                final Message response,
                                Record[] records,
                                final int section ) {
    final Multimap<RequestType, Record> rrsets = LinkedHashMultimap.create();
    for ( Record r : records ) {
      RequestType type = RequestType.typeOf( r.getType( ) );
      rrsets.get( type ).addAll( Collections2.filter( Arrays.asList( records ), type ) );
    }
    Predicate<Record> checkNewRecord = new Predicate<Record>( ) {
      
      @Override
      public boolean apply( Record input ) {
        for ( int s = 1; s <= section; s++ ) {
          if ( response.findRecord( input, s ) ) {
            return false;
          }
        }
        return true;
      }
    };
    if ( rrsets.containsKey( RequestType.CNAME ) ) {
      for ( Record cnames : Iterables.filter( rrsets.removeAll( RequestType.CNAME ), checkNewRecord ) ) {
        response.addRecord( cnames, section );
      }
    }
    for ( Record sectionRecord : Iterables.filter( rrsets.values( ), checkNewRecord ) ) {
      response.addRecord( sectionRecord, section );
    }
  }
  
  public static class DnsResponse {
    private final Multimap<ResponseSection, Record> sections = LinkedHashMultimap.create( );
    private final Name                name;
    private boolean                   recursive = false;
    private boolean                   nxdomain  = false;
    private boolean                   refused = false;
    public static class Builder {
      private final DnsResponse response;
      
      Builder( Name name ) {
        this.response = new DnsResponse( name );
      }
      
      public Builder withAuthority( List<Record> records ) {
        if ( records != null ) {
          this.response.sections.get( ResponseSection.AUTHORITY ).addAll( records );
        }
        return this;
      }
      
      public Builder withAuthority( Record... records ) {
        if ( records != null ) {
          return withAuthority( Arrays.asList( records ) );
        } else {
          return this;
        }
      }
      
      public Builder withAdditional( List<Record> records ) {
        if ( records != null ) {
          this.response.sections.get( ResponseSection.ADDITIONAL ).addAll( records );
        }
        return this;
      }
      
      public Builder withAdditional( Record... records ) {
        if ( records != null ) {
          return withAdditional( Arrays.asList( records ) );
        } else {
          return this;
        }
      }
      
      public Builder recursive( ) {
        this.response.recursive = true;
        return this;
      }
      
      public DnsResponse nxdomain( ) {
        this.response.nxdomain = true;
        return this.response;
      }
      
      public DnsResponse refused( ) {
        this.response.refused = true;
        return this.response;
      }
      
      public DnsResponse answer( List<? extends Record> records ) {
        if ( records != null ) {
          this.response.sections.get( ResponseSection.ANSWER ).addAll( records );
        }
        return this.response;
      }
      
      public DnsResponse answer( Record... records ) {
        if ( records != null ) {
          return answer( Arrays.asList( records ) );
        } else {
          return this.response;
        }
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
    
    public Record[] section( ResponseSection s ) {
      if ( this.sections.containsKey( s ) ) {
        return this.sections.get( s ).toArray( new Record[] {} );
      } else {
        return null;
      }
    }
    
    public boolean isAuthoritative( ) {
      return !this.recursive;
    }
    
    public boolean isNxdomain( ) {
      return this.nxdomain;
    }
    
    public boolean isRecursive( ) {
      return this.recursive;
    }
    
    public boolean isRefused( ) {
      return this.refused;
    }
  }

  public interface DnsRequest {
    Record getQuery();

    InetAddress getRemoteAddress( );

    InetAddress getLocalAddress( );
  }

  public static abstract class DnsResolver implements Ordered {
    public abstract boolean checkAccepts( DnsRequest request );
    
    public abstract DnsResponse lookupRecords( DnsRequest request );
    
    protected static final int DEFAULT_ORDER = 0;
    @Override
    public int getOrder( ) {
      return DEFAULT_ORDER;
    }
  }
  
  /**
   * Returns the list of resolvers which accept the name from the given source address.
   */
  private static Iterable<DnsResolver> resolversFor( final DnsRequest request ) {
    final List<DnsResolver> acceptingResolvers = Lists.newArrayList(Collections2.filter( resolvers.values( ), new Predicate<DnsResolver>( ) {
      @Override
      public boolean apply( final DnsResolver input ) {
        try {
          return input.checkAccepts( request );
        } catch ( final Exception ex ) {
          return false;
        }
      }
    } ));
    Collections.sort(acceptingResolvers, new OrderComparator( ) );
    return acceptingResolvers;
  }
  
  private static SetResponse lookupRecords( final Message response,
                                            final DnsRequest request ) {
    final Record query = request.getQuery( );
    final InetAddress source = request.getRemoteAddress( );
    final Name name = query.getName( );
    final int type = query.getType( );
    response.getHeader( ).setFlag( Flags.RA );// always mark the response w/ the recursion available
// bit
    LOG.debug( "DnsResolver: " + RequestType.typeOf( type ) + " " + name );
    for ( final DnsResolver r : DnsResolvers.resolversFor( request ) ) {
      try {
        final DnsResponse reply = r.lookupRecords( request );
        if ( reply == null ) {
          LOG.debug( "DnsResolver: returned null " + name + " using " + r );
          continue;
        }
        if ( reply.isAuthoritative( ) ) {// mark
          response.getHeader( ).setFlag( Flags.AA );
        }
        if ( reply.isNxdomain( ) ) {
          try{
            addRRset( name, response, new Record[] { DomainNameRecords.sourceOfAuthority( name ) }, type );
          }catch(final Exception ex){
            ;
          }
          response.getHeader( ).setRcode( Rcode.NXDOMAIN );
          return SetResponses.ofType( SetResponses.SetResponseType.nxdomain );
        } else if (reply.isRefused()) {
          response.getHeader().setRcode( Rcode.REFUSED );
          return SetResponses.ofType( SetResponses.SetResponseType.unknown );
        } else if ( reply.hasAnswer( ) ) {
          for ( ResponseSection s : ResponseSection.values( ) ) {
            Record[] records = reply.section( s );
            if ( records != null ) {
              addRRset( name, response, records, s.section( ) );
            }
          }
          return SetResponses.ofType( SetResponses.SetResponseType.successful );
        } else {
          return SetResponses.ofType( SetResponses.SetResponseType.successful );
        }
      } catch ( final Exception ex ) {
        LOG.debug( "DnsResolver: failed for " + name + " using " + r + " because of: " + ex.getMessage( ), ex );
      }
    }
    return SetResponses.ofType( SetResponses.SetResponseType.unknown );// no dice, return unknown
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public boolean processClass( final Class candidate ) throws Exception {
    if ( DnsResolver.class.isAssignableFrom( candidate )
         && !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
      try {
        final DnsResolver resolver = ( DnsResolver ) candidate.newInstance( );
        resolvers.putInstance( candidate, resolver );
        return true;
      } catch ( final Exception ex ) {
        LOG.error( "Failed to create instance of DnsResolver: "
                   + candidate
                   + " because of: "
                   + ex.getMessage( ) );
      }
    }
    return false;
  }
  
  @Override
  public Double getPriority( ) {
    return 0.5d;
  }
  
  public static SetResponse findRecords( final Message response,
                                         final DnsRequest request ) {
    try {
      if ( !enabled || !Bootstrap.isOperational( ) ) {
        return SetResponses.ofType( SetResponses.SetResponseType.unknown );
      } else {
        final Iterable<DnsResolver> resolverList = DnsResolvers.resolversFor( request );
        if ( Iterables.isEmpty( resolverList ) ) {
          return SetResponses.ofType( SetResponses.SetResponseType.nxdomain );
        } else {
          return DnsResolvers.lookupRecords( response, request );
        }
      }
    } catch ( final Exception ex ) {
      LOG.error( ex );
      LOG.trace( ex, ex );
    }
    return SetResponses.ofType( SetResponses.SetResponseType.unknown );
  }
}
