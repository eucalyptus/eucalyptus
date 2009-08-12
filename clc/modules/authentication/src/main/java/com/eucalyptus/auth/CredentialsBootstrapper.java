package com.eucalyptus.auth;

import com.eucalyptus.bootstrap.Bootstrapper;

public class CredentialsBootstrapper implements Bootstrapper {

  @Override
  public boolean check( ) throws Exception {
    return true;
  }

  @Override
  public boolean destroy( ) throws Exception {
    return true;
  }

  @Override
  public String getVersion( ) {
    return "";
  }

  @Override
  public boolean load( ) throws Exception {
    Credentials.init( );
    //TODO: first time start up check
    return false;
  }

  @Override
  public boolean start( ) throws Exception {
    //TODO: this depends on the DB
    Credentials.check( );
    return true;
  }

  @Override
  public boolean stop( ) throws Exception {
    return true;
  }

}
