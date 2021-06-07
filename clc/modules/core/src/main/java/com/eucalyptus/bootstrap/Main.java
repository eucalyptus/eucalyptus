package com.eucalyptus.bootstrap;

/**
 * Entry point for eucalyptus cloud startup
 */
public class Main {
  public static void main( String[] args ) {
    final SystemBootstrapper systemBootstrapper = SystemBootstrapper.getInstance( );
    try {
      systemBootstrapper.init( );
      systemBootstrapper.load( );
      systemBootstrapper.start( );
    } catch ( Throwable t ) {
      t.printStackTrace();
    }
  }
}