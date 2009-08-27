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
 */

/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.util;

import edu.ucsb.eucalyptus.cloud.entities.NetworkRulesGroup;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Hashes;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.SubDirectory;

import java.security.NoSuchAlgorithmException;
import com.eucalyptus.util.WalrusProperties;

public class UserManagement {

  private static Logger LOG     = Logger.getLogger( UserManagement.class );
  private static String keyPath = SubDirectory.KEYS.toString( );

  public static UserInfo generateAdmin( ) {
    UserInfo admin = new UserInfo( "admin" );
    admin.setUserName( admin.getUserName( ) );
    admin.setEmail( "" );
    admin.setRealName( "" );
    admin.setTelephoneNumber( "" );

    admin.setAffiliation( "" );
    admin.setProjectDescription( "" );
    admin.setProjectPIName( "" );

    admin.setPasswordExpires( 0L ); /* must be changed upon login */
    try {
      admin.setBCryptedPassword( Hashes.hashPassword( admin.getUserName( ) ) );
    } catch ( NoSuchAlgorithmException e ) {
    }

    admin.setConfirmationCode( UserManagement.generateConfirmationCode( admin.getUserName( ) ) );
    admin.setCertificateCode( UserManagement.generateCertificateCode( admin.getUserName( ) ) );

    admin.setSecretKey( UserManagement.generateSecretKey( admin.getUserName( ) ) );
    admin.setQueryId( UserManagement.generateQueryId( admin.getUserName( ) ) );

    admin.setReservationId( 0l );

    admin.setIsApproved( true );
    admin.setIsConfirmed( true );
    admin.setIsEnabled( true );
    admin.setIsAdministrator( true );

    admin.getNetworkRulesGroup( ).add( NetworkRulesGroup.getDefaultGroup( ) );

    return admin;
  }

  public static String generateConfirmationCode( String userName ) {
    return Hashes.getDigestBase64( userName, Hashes.Digest.SHA512, true ).replaceAll("\\p{Punct}", "" );
  }

  public static String generateCertificateCode( String userName ) {
    return Hashes.getDigestBase64( userName, Hashes.Digest.SHA512, true ).replaceAll("\\p{Punct}", "" );
  }

  public static String generateSecretKey( String userName ) {
    return Hashes.getDigestBase64( userName, Hashes.Digest.SHA224, true ).replaceAll("\\p{Punct}", "" );
  }

  public static String generateQueryId( String userName ) {
    return Hashes.getDigestBase64( userName, Hashes.Digest.MD5, false ).replaceAll("\\p{Punct}", "" );
  }

  public static boolean isAdministrator( String userId ) {
    if ( Component.eucalyptus.name( ).equals( userId ) || WalrusProperties.ADMIN.equals( userId ) ) return true;
    return false;
  }

  public static String getUserName( String queryId ) {
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>( );
    UserInfo userInfo = new UserInfo( );
    userInfo.setQueryId( queryId );
    try {
      UserInfo foundUserInfo = db.getUnique( userInfo );
      return foundUserInfo.getUserName( );
    } catch ( EucalyptusCloudException ex ) {
      LOG.warn( ex, ex );
    }
    return null;
  }
}
