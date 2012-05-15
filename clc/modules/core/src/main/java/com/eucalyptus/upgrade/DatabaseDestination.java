package com.eucalyptus.upgrade;

import com.eucalyptus.bootstrap.DatabaseBootstrapper;

public interface DatabaseDestination {
  public abstract void initialize( ) throws Exception;
  public DatabaseBootstrapper getDb() throws Exception;
}
