package com.eucalyptus.bootstrap;


public interface DatabaseBootstrapper {
  public abstract boolean load( ) throws Exception;
  
  public abstract boolean start( ) throws Exception;
  
  public boolean isRunning( );
  
  public void hup( );
}
