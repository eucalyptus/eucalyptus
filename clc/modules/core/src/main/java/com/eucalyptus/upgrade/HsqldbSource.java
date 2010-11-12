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
import com.eucalyptus.auth.crypto.Hmacs;

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
          props.setProperty( "password", Hmacs.generateSystemSignature( ) );
        }
      }
    }
    Connection conn = driver.connect( url, props );
    return new Sql( conn );
  }
}
