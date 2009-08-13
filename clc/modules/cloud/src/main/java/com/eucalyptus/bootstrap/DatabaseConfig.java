package com.eucalyptus.bootstrap;

public class DatabaseConfig {
  private static String DEFAULT = "CREATE SCHEMA PUBLIC AUTHORIZATION DBA\n" + 
  		"CREATE USER SA PASSWORD \"eucalyptus\"\n" + 
  		"GRANT DBA TO SA\n" + 
  		"SET WRITE_DELAY 10 MILLIS";
  private String name;
  private String fileName;
  //TODO: handle persistence.xml issues here
}
