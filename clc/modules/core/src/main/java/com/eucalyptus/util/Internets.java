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

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

public class Internets {
  private static Logger                                   LOG               = Logger.getLogger( Internets.class );
  private static final ConcurrentMap<String, InetAddress> localHostAddrList = new ConcurrentHashMap<String, InetAddress>( );
  private static final InetAddress                        localHostAddr     = determineLocalAddress( );
  private static final String                             localId           = localHostIdentifier( );

  
  private static InetAddress determineLocalAddress( ) {
    InetAddress laddr = null;
    LOG.info( "Trying to determine local bind address based on cli (--bind-addr)... " );
    if ( !BootstrapArgs.bindAddresses( ).isEmpty( ) ) {
      laddr = lookupBindAddresses( );
    }
    if ( laddr == null ) {
      LOG.info( "Trying to determine local bind address based on the default route... " );
      laddr = lookupDefaultRoute( );
    }
    if ( laddr == null ) {
      LOG.info( "Trying to determine local bind address based on a netmask and scope maximizing heuristic... " );
      laddr = Internets.getAllInetAddresses( ).get( 0 );
    }
    LOG.info( "==> Decided to use local bind address: " + laddr );
    System.setProperty( "bind_addr", laddr.getHostAddress( ) );
    System.setProperty( "bind.address", laddr.getHostAddress( ) );
    System.setProperty( "jgroups.bind_addr", laddr.getHostAddress( ) );
    System.setProperty( "jgroups.udp.bind_addr", laddr.getHostAddress( ) );
    
    return laddr;
  }
  
  private static InetAddress lookupDefaultRoute( ) {
    InetAddress laddr = null;
    try {
      String localAddr = ( String ) Groovyness.eval( "hi=\"/sbin/ip -o route get 4.2.2.1\".execute();hi.waitFor();hi.text" );
      String[] parts = localAddr.replaceAll( ".*src *", "" ).split( " " );
      if ( parts.length >= 1 ) {
        laddr = InetAddresses.forString( parts[0] );
      }
    } catch ( ScriptExecutionFailedException ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
    return laddr;
  }
  
  private static InetAddress lookupBindAddresses( ) {
    InetAddress laddr = null;
    List<InetAddress> locallyBoundAddrs = Internets.getAllInetAddresses( );
    boolean err = false;
    for ( String addrStr : BootstrapArgs.bindAddresses( ) ) {
      try {
        InetAddress next = InetAddress.getByName( addrStr );
        laddr = ( laddr == null )
          ? next
          : laddr;
        NetworkInterface iface = NetworkInterface.getByInetAddress( next );
        if ( locallyBoundAddrs.contains( InetAddress.getByName( addrStr ) ) ) {
          localHostAddrList.put( next.getHostAddress( ), next );
          LOG.info( "Identified local bind address: " + addrStr + " on interface " + iface.toString( ) );
        } else {
          LOG.error( "Failed to find specified --bind-addr=" + addrStr + " as it is not bound to a local interface.\n  Known addresses are: "
                     + Joiner.on( ", " ).join( locallyBoundAddrs ) );
        }
      } catch ( UnknownHostException ex ) {
        LOG.fatal( "Invalid argument given for --bind-addr=" + addrStr + " " + ex.getMessage( ) );
        LOG.error( ex, ex );
        err = true;
      } catch ( SocketException ex ) {
        LOG.fatal( "Invalid argument given for --bind-addr=" + addrStr + " " + ex.getMessage( ) );
        LOG.error( ex, ex );
        err = true;
      }
      if ( err ) {
        System.exit( 1 );//GRZE: special case, failed to determine bind address
      }
    }
    return laddr;
  }
  
  public static InetAddress loopback( ) {
    try {
      return InetAddress.getByName( "127.0.0.1" );
    } catch ( UnknownHostException ex ) {
      for ( InetAddress i : getAllInetAddresses( ) ) {
        if ( i.isLoopbackAddress( ) ) {
          return i;
        }
      }
      return localHostInetAddress( );
    }
  }

  public static InetAddress any( ) {
    return InetAddresses.fromInteger( 0 );
  }

  public static InetAddress localHostInetAddress( ) {
    return localHostAddr;
  }
  
  public static String localHostAddress( ) {
    return localHostInetAddress( ).getHostAddress( );
  }
  
  public static String localHostIdentifier( ) {
    return localId != null
      ? localId
      : localHostInetAddress( ).getHostAddress( );
  }
  
  public static List<NetworkInterface> getNetworkInterfaces( ) {
    try {
      List<NetworkInterface> ifaces = Collections.list( NetworkInterface.getNetworkInterfaces( ) );
      ifaces = Lists.newArrayList( Iterables.filter( ifaces, new Predicate<NetworkInterface>( ) {
        
        @Override
        public boolean apply( NetworkInterface input ) {
          return !input.getName( ).contains( "virbr0" ) && !input.getDisplayName( ).contains( "virbr0" );
        }
      } ) );
      ifaces.sort( new Comparator<NetworkInterface>( ) {

        @Override
        public int compare( NetworkInterface o1, NetworkInterface o2 ) {
          int min1 = 0;
          int min2 = 0;
          for ( InterfaceAddress ifaceAddr : o1.getInterfaceAddresses( ) ) {
            min1 = ( min1 > ifaceAddr.getNetworkPrefixLength( )
                ? ifaceAddr.getNetworkPrefixLength( )
                : min1 );
          }
          for ( InterfaceAddress ifaceAddr : o2.getInterfaceAddresses( ) ) {
            min2 = ( min2 > ifaceAddr.getNetworkPrefixLength( )
                ? ifaceAddr.getNetworkPrefixLength( )
                : min2 );
          }
          return min2 - min1;//return a positive int when min1 has a shorter routing prefix
        }
      } );
      return ifaces;
    } catch ( SocketException ex ) {
      LOG.error( ex, ex );
      throw new RuntimeException( "Getting list of network interfaces failed because of " + ex.getMessage( ), ex );
    }
  }
  
  public static List<InetAddress> getAllInetAddresses( ) {
    List<InetAddress> addrs = Lists.newArrayList( );
    for ( NetworkInterface iface : Internets.getNetworkInterfaces( ) ) {
      try {
        if ( iface.isPointToPoint( ) ) {
          continue;
        }
      } catch ( SocketException ex ) {
        LOG.error( ex, ex );
      }
      for ( InterfaceAddress iaddr : iface.getInterfaceAddresses( ) ) {
        InetAddress addr = iaddr.getAddress( );
        if ( addr instanceof Inet4Address ) {
          if ( !addr.isMulticastAddress( )
               && !addr.isLoopbackAddress( )
               && !addr.isLinkLocalAddress( )
               && !addr.isSiteLocalAddress( )
               && !addr.getHostAddress( ).contains( "192.168.122." ) ) {
            addrs.add( addr );
          }
        }
      }
      for ( InterfaceAddress iaddr : iface.getInterfaceAddresses( ) ) {
        InetAddress addr = iaddr.getAddress( );
        if ( addr instanceof Inet4Address ) {
          if ( !addr.isMulticastAddress( )
               && !addr.isLoopbackAddress( )
               && !addr.isLinkLocalAddress( )
               && !addrs.contains( addr.getHostAddress( ) )
               && !addr.getHostAddress( ).contains( "192.168.122." ) ) {
            addrs.add( addr );
          }
        }
      }
    }
    return addrs;
  }
  
  public static List<String> getAllAddresses( ) {
    return Lists.transform( Internets.getAllInetAddresses( ), new Function<InetAddress, String>( ) {
      @Override
      public String apply( InetAddress arg0 ) {
        return arg0.getHostAddress( );
      }
    } );
  }
  
  public static class Inet4AddressComparator implements Comparator<InetAddress>, Serializable {
    private static final long serialVersionUID = 1L;
    
    @Override
    public int compare( InetAddress o1, InetAddress o2 ) {
      return o1.getHostAddress( ).compareTo( o2.getHostAddress( ) );
    }
  }
  
  public static final Comparator<InetAddress> INET_ADDRESS_COMPARATOR = new Inet4AddressComparator( );
  
  public static InetAddress toAddress( URI uri ) {
    checkParam( "BUG: uri is null.", uri, notNullValue() );
    try {
      return InetAddress.getByName( uri.getHost( ) );
    } catch ( UnknownHostException e ) {
      throw Exceptions.toUndeclared( "Failed to resolve address for host: " + uri.getHost( ), e );
    }
  }
  
  public static InetAddress toAddress( String maybeUrlMaybeHostname ) {
    checkParam( "BUG: maybeUrlMaybeHostname is null.", maybeUrlMaybeHostname, notNullValue() );
    if ( maybeUrlMaybeHostname.startsWith( "vm:" ) ) {
      maybeUrlMaybeHostname = "localhost";
    }
    URI uri;
    String hostAddress;
    try {
      uri = new URI( maybeUrlMaybeHostname );
      hostAddress = uri.getHost( );
    } catch ( URISyntaxException e ) {
      hostAddress = maybeUrlMaybeHostname;
    }
    InetAddress ret = null;
    try {
      ret = InetAddress.getByName( hostAddress );
    } catch ( UnknownHostException e1 ) {
      Exceptions.error( "Failed to resolve address for host: " + maybeUrlMaybeHostname, e1 );
    }
    return ret;
  }
  
  public static boolean testLocal( final InetAddress addr ) {
    if ( addr == null ) return true;
    try {
      Boolean result = addr.isAnyLocalAddress( );
      result |= Iterables.any( Internets.getNetworkInterfaces( ), new Predicate<NetworkInterface>( ) {
        @Override
        public boolean apply( NetworkInterface arg0 ) {
          return Iterables.any( arg0.getInterfaceAddresses( ), new Predicate<InterfaceAddress>( ) {
            @Override
            public boolean apply( InterfaceAddress arg0 ) {
              return arg0.getAddress( ).equals( addr );
            }
          } );
        }
      } );
      return result;
    } catch ( Exception e ) {
//      Exceptions.eat( e.getMessage( ), e );
      return false;
    }
  }
  
  public static boolean testLocal( String address ) {
    if ( address == null ) return true;
    InetAddress addr;
    try {
      addr = InetAddress.getByName( address );
      return testLocal( addr );
    } catch ( UnknownHostException e ) {
      LOG.error( e.getMessage( ) );
      return address.endsWith( "Internal" );
    }
  }
  
  public static boolean testGoodAddress( String address ) throws Exception {
    InetAddress addr = InetAddress.getByName( address );
    LOG.debug( addr + " site=" + addr.isSiteLocalAddress( ) );
    LOG.debug( addr + " any=" + addr.isAnyLocalAddress( ) );
    LOG.debug( addr + " loop=" + addr.isLoopbackAddress( ) );
    LOG.debug( addr + " link=" + addr.isLinkLocalAddress( ) );
    LOG.debug( addr + " multi=" + addr.isMulticastAddress( ) );
    return addr.isSiteLocalAddress( )
           || ( !addr.isAnyLocalAddress( ) && !addr.isLoopbackAddress( ) && !addr.isLinkLocalAddress( ) && !addr.isMulticastAddress( ) );
  }
  
  private static void addAddress(Set<String> out, InetAddress addr) {
    if (addr instanceof Inet4Address &&
        !addr.isMulticastAddress() &&
        !addr.isLinkLocalAddress() &&
        !addr.isLoopbackAddress()) {
      out.add(addr.getCanonicalHostName());
      out.add(addr.getHostName());
      out.add(addr.getHostAddress());
    }
  }

  private static void addName(Set<String> out, String name) {
    try {
      for (InetAddress addr : InetAddress.getAllByName(name)) {
        addAddress(out, addr);
      }
    } catch (UnknownHostException uhe) {
      LOG.error("Failed to get addresses for name " + name + ": " + uhe, uhe);
    }
  }

  public static Set<String> getAllLocalHostNamesIps() {
    Set<String> results = new HashSet<String>();
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      addAddress(results, localHost);
      addName(results, localHost.getCanonicalHostName());
      addName(results, localHost.getHostName());
    } catch (UnknownHostException uhe) {
      LOG.error("Failed to get localhost: " + uhe, uhe);
    }
    try {
      Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      if (ifaces != null) {
        while (ifaces.hasMoreElements()) {
          NetworkInterface iface = ifaces.nextElement();
          Enumeration<InetAddress> addrs = iface.getInetAddresses();
          if (addrs != null) {
            while (addrs.hasMoreElements()) {
              addAddress(results, addrs.nextElement());
            }
          }
        }
      }
    } catch (SocketException se) {
      LOG.error("Failed to get all network interfaces: " + se);
    }
    return results;
  }

  public static Optional<Cidr> getInterfaceCidr( final InetAddress address ) {
    try {
      final NetworkInterface networkInterface = NetworkInterface.getByInetAddress( address );
      if(networkInterface==null)
        return Optional.absent();
      for ( final InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses( ) ) {
        if ( address.equals( interfaceAddress.getAddress( ) ) ) {
          final int prefix = interfaceAddress.getNetworkPrefixLength( );
          return Optional.of( Cidr.fromAddress( address, prefix ) );
        }
      }
    } catch ( SocketException e ) {
      LOG.debug("Error finding interface CIDR for address '"+address+"'", e );
    } 
    return Optional.absent( );
  }

  public static Function<InetAddress,Optional<Cidr>> interfaceCidr( ) {
    return InetAddressToCidr.INSTANCE;
  }

  private enum InetAddressToCidr implements Function<InetAddress,Optional<Cidr>>  {
    INSTANCE;

    @Override
    public Optional<Cidr> apply( final InetAddress address ) {
      return getInterfaceCidr( address );
    }
  }
}
