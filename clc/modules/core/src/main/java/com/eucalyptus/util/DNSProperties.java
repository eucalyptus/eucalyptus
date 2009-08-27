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
/*
 *
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.util;

import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.msgs.UpdateStorageConfigurationType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;
import java.util.UUID;
import java.util.List;
import java.util.Collections;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.Inet6Address;

public class DNSProperties {

    private static Logger LOG = Logger.getLogger( DNSProperties.class );
    public static String ADDRESS = "0.0.0.0";
    public static int PORT = 53;
    public static int MAX_MESSAGE_SIZE = 1024;
    public static String DNS_REF = "vm://DNSControlInternal";
    public static String DOMAIN = "localhost";
    public static String NS_HOST = "nshost." + DOMAIN;
    public static String NS_IP = "127.0.0.1";

    static {
        updateHost();
    }

    public static void update() {
        try {
            SystemConfiguration systemConfiguration = EucalyptusProperties.getSystemConfiguration();
            DOMAIN = systemConfiguration.getDnsDomain();
            NS_HOST = systemConfiguration.getNameserver();
            NS_IP = systemConfiguration.getNameserverAddress();
        } catch(Exception ex) {
            LOG.warn(ex.getMessage());
        }
    }

    private static void updateHost () {
        InetAddress ipAddr = null;
        String localAddr = "127.0.0.1";

        List<NetworkInterface> ifaces = null;
        try {
            ifaces = Collections.list( NetworkInterface.getNetworkInterfaces() );
        }
        catch ( SocketException e1 ) {}

        for ( NetworkInterface iface : ifaces )
            try {
                if ( !iface.isLoopback() && !iface.isVirtual() && iface.isUp() ) {
                    for ( InetAddress iaddr : Collections.list( iface.getInetAddresses() ) ) {
                        if ( !iaddr.isSiteLocalAddress() && !( iaddr instanceof Inet6Address) ) {
                            ipAddr = iaddr;
                        } else if ( iaddr.isSiteLocalAddress() && !( iaddr instanceof Inet6Address ) ) {
                            ipAddr = iaddr;
                        }
                    }
                }
            }
            catch ( SocketException e1 ) {}

        if(ipAddr != null) {
            DNSProperties.NS_IP = ipAddr.getHostAddress();
            DNSProperties.NS_HOST = ipAddr.getCanonicalHostName();
        }
    }

}
