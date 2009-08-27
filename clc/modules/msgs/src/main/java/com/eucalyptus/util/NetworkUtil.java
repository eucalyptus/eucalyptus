/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
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
