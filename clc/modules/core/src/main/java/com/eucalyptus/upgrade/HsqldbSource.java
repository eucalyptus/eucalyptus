/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.upgrade;

import groovy.sql.Sql;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.crypto.Hmacs;

public class HsqldbSource implements DatabaseSource {
  private static Logger     LOG      = Logger.getLogger( HsqldbSource.class );
  private File              oldDbDir = new File( System.getProperty( "euca.upgrade.old.dir" ) + "/var/lib/eucalyptus/db/" );
  private static Driver     driver;
  private static Properties props;
  static {
    try {
      Class driverClass = ClassLoader.getSystemClassLoader( ).loadClass( "org.hsqldb.jdbcDriver" );
      driver = ( Driver ) driverClass.newInstance( );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      System.exit( -1 );
    }
  }
  
  public HsqldbSource( ) {}
  
  /**
   * @see com.eucalyptus.upgrade.DatabaseSource#getSqlSession(java.lang.String)
   * @param persistenceContext
   * @return
   * @throws SQLException
   */
  public Sql getSqlSession( String persistenceContext ) throws SQLException {
    String url = "jdbc:hsqldb:file:" + oldDbDir.getAbsolutePath( ) + File.separator + persistenceContext;
    if ( props == null ) {
      synchronized ( HsqldbSource.class ) {
    	if ( props == null ) {
          props = new Properties( );
          props.setProperty( "user", "sa" );
          props.setProperty( "password", SystemIds.databasePassword( ) );
          // NB: readonly is HSQLDB-specific
          props.setProperty( "readonly", "true" );
    	}
      }
    }
    try {
        Connection conn = driver.connect( url, props );
        return new Sql( conn );
    } catch ( SQLException e) {
        return null;
    }
  }
}
