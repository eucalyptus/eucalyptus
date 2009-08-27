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

import java.io.File;

import org.apache.log4j.Logger;

public enum BaseDirectory {
  HOME( "euca.home" ),
  VAR( "euca.var.dir" ),
  CONF( "euca.conf.dir" ),
  LIB( "euca.lib.dir" ),
  LOG( "euca.log.dir" );
  private static Logger LOGG = Logger.getLogger( BaseDirectory.class );

  private String        key;

  BaseDirectory( final String key ) {
    this.key = key;
  }

  public boolean check( ) {
    if ( System.getProperty( this.key ) == null ) {
      BaseDirectory.LOGG.fatal( "System property '" + this.key + "' must be set." );
      return false;
    }
    return true;
  }

  @Override
  public String toString( ) {
    return System.getProperty( this.key );
  }

  public void create( ) {
    final File dir = new File( this.toString( ) );
    if ( dir.exists( ) ) { return; }
    dir.mkdirs( );
  }
}
