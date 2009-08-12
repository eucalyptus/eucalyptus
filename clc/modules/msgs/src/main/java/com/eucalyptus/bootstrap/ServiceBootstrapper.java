package com.eucalyptus.bootstrap;

import java.util.List;

public class ServiceBootstrapper extends Bootstrapper {

  @Override
  public boolean check( ) {
    return false;
  }

  @Override
  public boolean destroy( ) {
    return false;
  }

  public boolean load( ) {
    return false;
  }

  @Override
  public boolean start( ) {
    return false;
  }

  @Override
  public boolean stop( ) {
    return false;
  }

}
