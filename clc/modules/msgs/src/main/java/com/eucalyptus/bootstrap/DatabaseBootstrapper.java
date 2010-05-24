package com.eucalyptus.bootstrap;

import com.eucalyptus.bootstrap.Bootstrap.Stage;

public interface DatabaseBootstrapper {
  public abstract boolean load( Stage current ) throws Exception;
  
  public abstract boolean start( ) throws Exception;
  
  public boolean isRunning( );
  
  public void hup( );
}
