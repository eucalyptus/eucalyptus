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
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.util;

import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

public class EucalyptusProperties {

  private static Logger LOG = Logger.getLogger( EucalyptusProperties.class );


  public static boolean disableNetworking = false;
  public static boolean disableBlockStorage = false;

  public static String NETWORK_DEFAULT_NAME = "default";
  public static String DEBUG_FSTRING = "[%12s] %s";

  public enum NETWORK_PROTOCOLS {

    tcp, udp, icmp
  }

  public static String NAME_SHORT = "euca2";
  public static String FSTRING = "::[ %-20s: %-50.50s ]::\n";
  public static String IMAGE_MACHINE = "machine";
  public static String IMAGE_KERNEL = "kernel";
  public static String IMAGE_RAMDISK = "ramdisk";
  public static String IMAGE_MACHINE_PREFIX = "emi";
  public static String IMAGE_KERNEL_PREFIX = "eki";
  public static String IMAGE_RAMDISK_PREFIX = "eri";

  public static String getDName( String name ) {
    return String.format( "CN=www.eucalyptus.com, OU=Eucalyptus, O=%s, L=Santa Barbara, ST=CA, C=US", name );
  }

  public static SystemConfiguration getSystemConfiguration() throws EucalyptusCloudException {
    EntityWrapper<SystemConfiguration> confDb = new EntityWrapper<SystemConfiguration>();
    SystemConfiguration conf = null;
    try {
      conf = confDb.getUnique( new SystemConfiguration() );
    }
    catch ( EucalyptusCloudException e ) {
      confDb.rollback();
      throw new EucalyptusCloudException( "Failed to load system configuration", e );
    }
    if( conf.getRegistrationId() == null ) {
      conf.setRegistrationId( UUID.randomUUID().toString() );
    }
    if( conf.getSystemReservedPublicAddresses() == null ) {
      conf.setSystemReservedPublicAddresses( 10 );
    }
    if( conf.getMaxUserPublicAddresses() == null ) {
      conf.setMaxUserPublicAddresses( 5 );
    }
    if( conf.isDoDynamicPublicAddresses() == null ) {
      conf.setDoDynamicPublicAddresses( true );
    }
    confDb.commit();
    String walrusUrl = null;
    try {
      walrusUrl = ( new URL( conf.getStorageUrl() + "/" ) ).toString();
    }
    catch ( MalformedURLException e ) {
      throw new EucalyptusCloudException( "System is misconfigured: cannot parse Walrus URL.", e );
    }

    return conf;
  }

  public enum TokenState {

    preallocate, returned, accepted, submitted, allocated, redeemed;
  }
}
