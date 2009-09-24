package com.eucalyptus.bootstrap;

public interface DatabaseBootstrapper {
  public boolean isRunning();

  public void hup( );
}
