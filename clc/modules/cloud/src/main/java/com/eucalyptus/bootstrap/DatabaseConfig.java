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
package com.eucalyptus.bootstrap;

public class DatabaseConfig {
  public static final String EUCA_DB_PORT = "euca.db.port";
  public static final String EUCA_DB_PASSWORD = "euca.db.password";
  public static final String EUCA_DB_HOST = "euca.db.host";
  static {
    if( !System.getProperties( ).contains( EUCA_DB_HOST ) ) System.setProperty( EUCA_DB_HOST, "127.0.0.1" );
    if( !System.getProperties( ).contains( EUCA_DB_PORT ) ) System.setProperty( EUCA_DB_PORT, "9001" );
    if( !System.getProperties( ).contains( EUCA_DB_PASSWORD ) )System.setProperty( EUCA_DB_PASSWORD, "" );
  }

  private static String DEFAULT = "CREATE SCHEMA PUBLIC AUTHORIZATION DBA\n" + 
  		"CREATE USER SA PASSWORD \"eucalyptus\"\n" + 
  		"GRANT DBA TO SA\n" + 
  		"SET WRITE_DELAY 10 MILLIS";
  private String name;
  private String fileName;
  //TODO: handle persistence.xml issues here
}
