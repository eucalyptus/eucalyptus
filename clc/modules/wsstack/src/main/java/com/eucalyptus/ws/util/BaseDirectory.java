package com.eucalyptus.ws.util;

import java.io.File;

import org.apache.log4j.Logger;

public enum BaseDirectory {
  HOME( "euca.home" ), VAR( "euca.var.dir" ), CONF( "euca.conf.dir" ), LOG( "euca.log.dir" );
  private static Logger LOGG = Logger.getLogger( BaseDirectory.class );

  private String        key;

  BaseDirectory( final String key ) {
    this.key = key;
  }

  public boolean check( ) {
    if ( System.getProperty( this.key ) == null ) {
      BaseDirectory.LOGG.fatal( "System property '" + this.key + "' must be set." );
      return false;
    }
    return true;
  }

  @Override
  public String toString( ) {
    return System.getProperty( this.key );
  }

  public void create( ) {
    final File dir = new File( this.toString( ) );
    if ( dir.exists( ) ) { return; }
    dir.mkdirs( );
  }
}
