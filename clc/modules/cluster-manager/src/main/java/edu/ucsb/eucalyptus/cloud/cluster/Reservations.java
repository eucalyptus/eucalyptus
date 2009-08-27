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

public class Reservations extends AbstractNamedRegistry<Reservation> {

  private static Reservations singleton = getInstance();

  public static Reservations getInstance()
  {
    synchronized ( Reservations.class )
    {
      if ( singleton == null )
        singleton = new Reservations();
    }
    return singleton;
  }

  public static String makeReservationId( Long idNumber )
  {
    MessageDigest digest = Hashes.Digest.MD2.get();
    digest.reset();
    digest.update( idNumber.toString().getBytes(  ));

    Adler32 hash = new Adler32();
    hash.reset();
    hash.update( digest.digest() );

    return String.format( "r-%08X", hash.getValue() );
  }

  public static String getMac( int mac )
  {
    return String.format( "%02X:%02X:%02X:%02X", ( mac >> 24 ) & 0xff, ( mac >> 16 ) & 0xff, ( mac >> 8 ) & 0xff, mac & 0xff );
  }
}
