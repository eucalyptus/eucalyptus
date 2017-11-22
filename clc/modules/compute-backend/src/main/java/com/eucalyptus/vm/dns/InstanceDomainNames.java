/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

import static com.eucalyptus.vm.VmInstances.INSTANCE_SUBDOMAIN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xbill.DNS.Name;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.dns.DomainNames;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.net.InetAddresses;

public enum InstanceDomainNames implements Function<Name, InetAddress> {
  EXTERNAL {
    final Supplier<Name> externalInstanceSubdomain = Suppliers.memoizeWithExpiration(
                                                                                      new Supplier<Name>( ) {
                                                                                        
                                                                                        @Override
                                                                                        public Name get( ) {
                                                                                          return DomainNames.absolute(
                                                                                                                       lookupInstanceSubdomainProperty( ),
                                                                                                                       DomainNames.externalSubdomain( ) );
                                                                                        }
                                                                                      },
                                                                                      INSTANCE_DOMAIN_REFRESH,
                                                                                      TimeUnit.SECONDS );
    
    @Override
    public Name get( ) {
      return this.externalInstanceSubdomain.get( );
    }
  },
  INTERNAL {
    final Supplier<Name> internalInstanceSubdomain = Suppliers.memoizeWithExpiration(
                                                                                      new Supplier<Name>( ) {
                                                                                        
                                                                                        @Override
                                                                                        public Name get( ) {
                                                                                          return DomainNames.absolute( lookupInstanceSubdomainProperty( ),
                                                                                                                       DomainNames.internalSubdomain( ) );
                                                                                        }
                                                                                      },
                                                                                      INSTANCE_DOMAIN_REFRESH,
                                                                                      TimeUnit.SECONDS );
    
    @Override
    public Name get( ) {
      return this.internalInstanceSubdomain.get( );
    }
  },
  CONFIGURABLEINTERNAL {
    final Supplier<Name> configurableSubdomain = Suppliers.memoizeWithExpiration(
                                                                                  new Supplier<Name>( ) {
                                                                                    
                                                                                    @Override
                                                                                    public Name get( ) {
                                                                                      return DomainNames.absolute( lookupInstanceSubdomainProperty( ) );
                                                                                    }
                                                                                  },
                                                                                  INSTANCE_DOMAIN_REFRESH,
                                                                                  TimeUnit.SECONDS );
    
    @Override
    public Name get( ) {
      return this.configurableSubdomain.get( );
    }
  },
  DEFAULTINTERNAL {
    final Supplier<Name> defaultInternalSubdomain = Suppliers.memoizeWithExpiration(
                                                                                     new Supplier<Name>( ) {
                                                                                       
                                                                                       @Override
                                                                                       public Name get( ) {
                                                                                         return DomainNames.internalSubdomain( Eucalyptus.class );
                                                                                       }
                                                                                     },
                                                                                     INSTANCE_DOMAIN_REFRESH,
                                                                                     TimeUnit.SECONDS );
    
    @Override
    public Name get( ) {
      return this.defaultInternalSubdomain.get( );
    }
  };
  private static final int        INSTANCE_DOMAIN_REFRESH = 30;
  private static final String     DNS_TO_IP_REGEX         = "$1.$2.$3.$4";
  private static final String     INSTANCE_DNS_REGEX      = "euca-(.+{3})-(.+{3})-(.+{3})-(.+{3})";
  private static final Pattern    PATTERN                 = Pattern.compile( INSTANCE_DNS_REGEX + ".*" );
  private final Supplier<Pattern> instancePattern         = new Supplier<Pattern>( ) {
                                                            
                                                            @Override
                                                            public Pattern get( ) {
                                                              final String DNS_SUFFIX = InstanceDomainNames.this.get( )
                                                                                                                .toString( )
                                                                                                                .replace( ".", "\\." );
                                                              return Pattern.compile( INSTANCE_DNS_REGEX + "\\." + DNS_SUFFIX );
                                                            }
                                                          };
  
  private boolean matches( Name name ) {
    return this.instancePattern.get( ).matcher( name.toString( ) ).matches( );
  }
  
  static Name fromInetAddress( InstanceDomainNames instanceDomain, InetAddress ip ) {
    final String instancePart = "euca-" + ip.getHostAddress( ).replace( '.', '-' );
    return DomainNames.absolute( Name.fromConstantString( instancePart ), instanceDomain.get( ) );
  }
  
  public static InetAddress toInetAddress( Name name ) {
    return InetAddresses.forString( PATTERN.matcher( name.toString( ) ).replaceAll( DNS_TO_IP_REGEX ) );
  }
  
  @Override
  public InetAddress apply( Name input ) {
    try {
      final Matcher matcher = PATTERN.matcher( input.toString( ) );
      String parsedIp = matcher.replaceAll( DNS_TO_IP_REGEX );
      return InetAddress.getByName( parsedIp );
    } catch ( UnknownHostException ex ) {
      return Internets.loopback( );
    }
  }
  
  public abstract Name get( );
  
  private static Name lookupInstanceSubdomainProperty( ) {
    return Name.fromConstantString( INSTANCE_SUBDOMAIN.replaceFirst( "^\\.", "" ) );
  }
  
  /**
   * Get the instance domain name for the given name if it is valid -- throw an exception otherwise.
   * 
   * @throws NoSuchElementException
   */
  public static Name lookupInstanceDomain( Name name ) throws NoSuchElementException {
    if ( PATTERN.matcher( name.toString( ) ).matches( ) ) {
      for ( InstanceDomainNames dom : InstanceDomainNames.values( ) ) {
        if ( name.subdomain( dom.get( ) ) ) {
          return dom.get( );
        }
      }
    }
    throw new NoSuchElementException( "Failed to lookup instance domain name matching " + name );
  }
  
  /**
   * Test the given name to determine whether it is a valid instance DNS name in some instance
   * domain.
   */
  public static boolean isInstanceDomainName( Name name ) {
    if ( PATTERN.matcher( name.toString( ) ).matches( ) ) {
      for ( InstanceDomainNames dom : InstanceDomainNames.values( ) ) {
        if ( dom.matches( name ) ) {
          return true;
        }
      }
    }
    return false;
  }
  
  /**
   * Test the given name to determine whether it is in some instance domain.
   */
  public static boolean isInstanceSubdomain( Name name ) {
    if ( PATTERN.matcher( name.toString( ) ).matches( ) ) {
      for ( InstanceDomainNames dom : InstanceDomainNames.values( ) ) {
        if ( name.subdomain( dom.get( ) ) ) {
          return true;
        }
      }
    }
    return false;
  }
}
