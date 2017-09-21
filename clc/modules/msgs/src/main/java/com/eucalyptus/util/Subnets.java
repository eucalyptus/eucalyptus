/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

package com.eucalyptus.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;
import com.google.common.base.Predicate;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.net.InetAddresses;

public class Subnets extends ServiceJarDiscovery {
  private static Logger                                           LOG            = Logger.getLogger( Subnets.class );
  private static final ClassToInstanceMap<Predicate<InetAddress>> subnetCheckers = MutableClassToInstanceMap.create( );

  /**
   * A Predicate<InetAddress> which returns true if the system is responsible for the source
   * address.
   */
  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface SystemSubnetPredicate {}

  /**
   * Determines if {@code addr} is from a subnet under the system's control (or possibly a public
   * address under the systems control).
   *
   * @param addr
   * @return
   */
  public static boolean isSystemManagedAddress( InetAddress addr ) {
    for ( Predicate<InetAddress> p : subnetCheckers.values( ) ) {
      try {
        if ( p.apply( addr ) ) {
          return true;
        }
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
    }
    return false;
  }


  /**
   * Determines if {@code addr} is a host under the system's control (or possibly a public
   * address under the systems control).
   *
   * @param addr
   * @return
   */
  public static boolean isSystemHostAddress( final InetAddress addr ) {
    Predicate<Host> filter = new Predicate<Host>() {

      /**
       * @see com.google.common.base.Predicate#apply(java.lang.Object)
       */
      @Override
      public boolean apply( Host arg0 ) {
        return arg0.getHostAddresses( ).contains( addr );
      }

    };
    return !Hosts.list( filter ).isEmpty( );
  }



  /**
   * Constructs a predicate which can test for address membership in the subnet defined by the given
   * {@code subnet} address and {@code netmask}.
   *
   * @param subnet
   * @param netmask
   * @return
   * @throws UnknownHostException
   */
  public static Predicate<InetAddress> internalPredicate( final String subnet, final String netmask ) throws UnknownHostException {
    return Subnets.create( subnet, netmask ).getPredicate( );
  }

  public static Subnet create( String subnet, String netmask ) throws UnknownHostException {
    return new Subnet( InetAddress.getByName( subnet ), InetAddress.getByName( netmask ) );
  }

  public static Cidr cidr( String subnet, String netmask ) throws UnknownHostException {
    return new Subnet( InetAddress.getByName( subnet ), InetAddress.getByName( netmask ) ).toCidr( );
  }

  private static class Subnet {
    private final InetAddress            subnet;
    private final int                    networkId;
    private final int                    subnetMask;
    private final int                    prefix;
    private final Predicate<InetAddress> predicate = new Predicate<InetAddress>( ) {

                                                     @Override
                                                     public boolean apply( InetAddress arg0 ) {
                                                       return Subnet.this.inSubnet( arg0 );
                                                     }
                                                   };

    Subnet( InetAddress address, InetAddress netmask ) {
      this.subnetMask = InetAddresses.coerceToInteger( netmask );
      this.networkId = InetAddresses.coerceToInteger( address ) & this.subnetMask;
      this.subnet = InetAddresses.fromInteger( networkId );
      this.prefix = ( int ) Math.round( Math.log( Integer.lowestOneBit( this.subnetMask ) ) / Math.log( 2 ) );
    }

    Subnet( InetAddress subnet, int prefix ) throws UnknownHostException {
      this( subnet, InetAddresses.fromInteger( ( int ) -1 << ( 32 - prefix ) ) );
    }

    public boolean inSubnet( InetAddress address ) {
      return ( InetAddresses.coerceToInteger( address ) & this.subnetMask ) == this.networkId;
    }

    @Override
    public String toString( ) {
      return this.subnet.getHostAddress( ) + "/" + this.prefix;
    }

    @Override
    public boolean equals( Object obj ) {
      if ( !( obj instanceof Subnet ) ) {
        return false;
      } else {
        return ( ( Subnet ) obj ).networkId == this.networkId;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash( getNetworkId( ) );
    }

    public InetAddress getSubnet( ) {
      return this.subnet;
    }

    public InetAddress getNetworkId( ) {
      return InetAddresses.fromInteger( this.networkId );
    }

    public int getSubnetMask( ) {
      return this.subnetMask;
    }

    public int getPrefix( ) {
      return this.prefix;
    }

    public Predicate<InetAddress> getPredicate( ) {
      return this.predicate;
    }

    public Cidr toCidr( ) {
      return Cidr.of( networkId, prefix );
    }
  }

  @SuppressWarnings( "unchecked" )
  @Override
  public boolean processClass( Class candidate ) throws Exception {
    if ( Ats.from( candidate ).has( SystemSubnetPredicate.class )
         && Predicate.class.isAssignableFrom( candidate )
         && !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
      try {
        Predicate<InetAddress> resolver = ( Predicate<InetAddress> ) candidate.newInstance( );
        subnetCheckers.putInstance( candidate, resolver );
        return true;
      } catch ( Exception ex ) {
        LOG.error( "Failed to create instance of SystemSubnetPredicate: " + candidate + " because of: " + ex.getMessage( ) );
      }
    }
    return false;
  }

  @Override
  public Double getPriority( ) {
    return 0.5d;
  }
}
