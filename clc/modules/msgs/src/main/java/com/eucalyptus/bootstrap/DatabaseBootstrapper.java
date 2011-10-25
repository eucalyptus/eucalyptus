package com.eucalyptus.bootstrap;

public interface DatabaseBootstrapper {
  public abstract void init( ) throws Exception;
  
  public abstract boolean load( ) throws Exception;
  
  public abstract boolean start( ) throws Exception;
  
  public abstract boolean stop( ) throws Exception;
  
  public abstract void destroy( ) throws Exception;
  
  public abstract boolean isRunning( );
  
  public abstract void hup( );
  
  public abstract String getDriverName( );
  
  public abstract String getJdbcDialect( );
  
  public abstract String getHibernateDialect( );
  
  public abstract String getJdbcScheme( );
  
  public abstract String getServicePath( String... pathParts );
}
