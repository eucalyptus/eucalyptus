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

package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.AbstractNamedRegistry;

import java.security.MessageDigest;
import java.util.zip.Adler32;

import com.eucalyptus.auth.Hashes;

public class VmInstances extends AbstractNamedRegistry<VmInstance> {

  private static VmInstances singleton = getInstance();

  public static VmInstances getInstance() {
    synchronized ( VmInstances.class ) {
      if ( singleton == null )
        singleton = new VmInstances();
    }
    return singleton;
  }

  public static String getId( Long rsvId, int launchIndex ) {
    String vmId = null;
    do {
      MessageDigest digest = Hashes.Digest.MD5.get();
      digest.reset();
      digest.update( Long.toString( rsvId + launchIndex + System.currentTimeMillis() ).getBytes() );

      Adler32 hash = new Adler32();
      hash.reset();
      hash.update( digest.digest() );
      vmId = String.format( "i-%08X", hash.getValue() );
    } while ( VmInstances.getInstance().contains( vmId ) );
    return vmId;
  }

  public static String getAsMAC( String instanceId ) {
    String mac = String.format( "d0:0d:%s:%s:%s:%s",
                                instanceId.substring( 2, 4 ),
                                instanceId.substring( 4, 6 ),
                                instanceId.substring( 6, 8 ),
                                instanceId.substring( 8, 10 ) );
    return mac;
  }

}
