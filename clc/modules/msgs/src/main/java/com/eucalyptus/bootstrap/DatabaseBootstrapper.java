package com.eucalyptus.bootstrap;

public interface DatabaseBootstrapper {
  public abstract void init( );

  public abstract boolean load( ) throws Exception;
  
  public abstract boolean start( ) throws Exception;
  
  public abstract boolean stop( ) throws Exception;
  
  public abstract void destroy( ) throws Exception;
  
  public abstract boolean isRunning( );
  
  public abstract void hup( );
  
  public abstract String getDriverName( );

  public abstract String getJdbcDialect( );

  public abstract String getHibernateDialect( );


  /**
   * Format string pattern which has two parameters:
   * 1. %s - host string
   * 2. %d - port number 
   * @return
   */
  public abstract String getUriPattern( );
}
