package com.eucalyptus.bootstrap;

import java.util.Map;

public interface DatabaseBootstrapper {

  /**
   * Recommended / default database username
   */
  String DB_USERNAME = "eucalyptus";

  void init( ) throws Exception;
  
  boolean load( ) throws Exception;
  
  boolean start( ) throws Exception;
  
  boolean stop( ) throws Exception;
  
  void destroy( ) throws Exception;
  
  boolean isRunning( );
  
  void hup( );
  String getDriverName( );
  
  String getJdbcDialect( );
  
  String getHibernateDialect( );
  
  String getJdbcScheme( );
  
  String getServicePath( String... pathParts );

  Map<String,String> getJdbcUrlQueryParameters();

  String getUserName();

  String getPassword();

}
