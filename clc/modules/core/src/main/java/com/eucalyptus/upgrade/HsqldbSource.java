package com.eucalyptus.upgrade;

import groovy.sql.Sql;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class HsqldbSource implements DatabaseSource {
  private File oldDbDir = new File( System.getProperty( "euca.upgrade.old.dir" ) + "/var/lib/eucalyptus/db/" );
  
  public HsqldbSource( ) {}
  
  /**
   * @see com.eucalyptus.upgrade.DatabaseSource#getSqlSession(java.lang.String)
   * @param persistenceContext
   * @return
   * @throws SQLException
   */
  public Sql getSqlSession( String persistenceContext ) throws SQLException {
    Connection conn = DriverManager.getConnection( "jdbc:hsqldb:file:" + oldDbDir.getAbsolutePath( ) + File.separator + persistenceContext, "eucalyptus",
                                                   System.getProperty( "euca.db.password" ) );
    return new Sql( conn );
  }
}
