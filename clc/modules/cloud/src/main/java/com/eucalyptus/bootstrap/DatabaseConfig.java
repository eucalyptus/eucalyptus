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
