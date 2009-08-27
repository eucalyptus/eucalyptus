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
package com.eucalyptus.auth;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;

@Provides( resource = Resource.SystemCredentials )
@Depends( remote = Component.eucalyptus )
public class RemoteComponentCredentialBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( RemoteComponentCredentialBootstrapper.class );

  @Override
  public boolean load( Resource current ) throws Exception {
    Credentials.init( );
    while ( true ) {
      if ( this.checkAllKeys( ) ) {
        for ( Component c : Component.values( ) ) {
          LOG.info( "Initializing system credentials for " + c.name( ) );
          SystemCredentialProvider.init( c );
          c.markHasKeys( );
        }
        break;
      } else {
        LOG.fatal( "Waiting for system credentials before proceeding with startup..." );
        try {
          Thread.sleep( 2000 );
        } catch ( Exception e ) {
        }
      }
    }
    return true;
  }

  private boolean checkAllKeys( ) {
    for ( Component c : Component.values( ) ) {
      if ( c.isEnabled( ) ) {
        try {
          if( !EucaKeyStore.getCleanInstance( ).containsEntry( c.name( ) ) ) {
            return false;
          }
        } catch ( Exception e ) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }

}
