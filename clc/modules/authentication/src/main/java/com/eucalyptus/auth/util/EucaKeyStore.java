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
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.auth.util;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.SubDirectory;

public class EucaKeyStore extends AbstractKeyStore {
  public static String            FORMAT         = "pkcs12";
  private static String           KEY_STORE_PASS = Component.eucalyptus.name();
  private static String           FILENAME       = "euca.p12";
  private static Logger           LOG            = Logger.getLogger( EucaKeyStore.class );

  private static AbstractKeyStore singleton      = EucaKeyStore.getInstance( );

  public static AbstractKeyStore getInstance( ) {
    synchronized ( EucaKeyStore.class ) {
      if ( EucaKeyStore.singleton == null ) {
        try {
          singleton = new EucaKeyStore( );
        } catch ( final Exception e ) {
          LOG.error( e, e );
        }
      }
    }
    return EucaKeyStore.singleton;
  }

  public static AbstractKeyStore getCleanInstance( ) throws Exception {
    synchronized ( EucaKeyStore.class ) {
      singleton = new EucaKeyStore( );      
    }
    return singleton;
  }

  
  private EucaKeyStore( ) throws GeneralSecurityException, IOException {
    super( SubDirectory.KEYS.toString( ) + File.separator + EucaKeyStore.FILENAME, EucaKeyStore.KEY_STORE_PASS, EucaKeyStore.FORMAT );
  }

  @Override
  public boolean check( ) throws GeneralSecurityException {
    return (this.getCertificate( Component.jetty.name() )!= null)&&(this.getCertificate( Component.eucalyptus.name() )!=null);
  }
}
