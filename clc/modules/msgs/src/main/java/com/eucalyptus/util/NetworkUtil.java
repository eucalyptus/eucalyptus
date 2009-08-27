package com.eucalyptus.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import com.google.common.collect.Lists;

public class NetworkUtil {
  
  public static List<String> getAllAddresses() throws SocketException  {
    List<String> addrs = Lists.newArrayList( );
    Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces( );
    while( ifaces.hasMoreElements( ) ) {
      NetworkInterface iface = ifaces.nextElement( );
      for( InterfaceAddress iaddr : iface.getInterfaceAddresses( ) ) {
        InetAddress addr = iaddr.getAddress( );
        if( addr instanceof Inet4Address ) {
          if( !addr.isMulticastAddress( ) && !addr.isLoopbackAddress( ) && !addr.isLinkLocalAddress( ) ) {
            addrs.add( addr.getHostAddress( ) );
          }
        }
      }
    }
    return addrs;
  }

  public static boolean testReachability( String addr ) throws Exception {
    InetAddress inetAddr = Inet4Address.getByName( addr );
    return inetAddr.isReachable( 1000 );
  }

  public static boolean testLocal( String address ) throws Exception {
    List<String> addrs = Lists.newArrayList( );
    Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces( );
    while( ifaces.hasMoreElements( ) ) {
      NetworkInterface iface = ifaces.nextElement( );
      for( InterfaceAddress iaddr : iface.getInterfaceAddresses( ) ) {
        InetAddress addr = iaddr.getAddress( );
        if( addr instanceof Inet4Address ) {
          addrs.add( addr.getHostAddress( ) );
        }
      }
    }
    return addrs.contains( address );
  }

  
  public static void main( String[] args) throws Exception {
    for( String addr : NetworkUtil.getAllAddresses( ) ) {
      System.out.println( addr );
    }
    System.out.println("Testing if 192.168.7.8 is reachable: " + NetworkUtil.testReachability( "192.168.7.8" ) );
  }

}
