package com.eucalyptus.ws.util;

import java.io.File;

public enum SubDirectory {
  DB( BaseDirectory.VAR, "db" ), MODULES( BaseDirectory.VAR, "modules" ), SERVICES( BaseDirectory.VAR, "services" ), WWW( BaseDirectory.CONF, "www" ), WEBAPPS( BaseDirectory.VAR, "webapps" ), KEYS( BaseDirectory.VAR, "keys" );
  BaseDirectory parent;
  String        dir;

  SubDirectory( final BaseDirectory parent, final String dir ) {
    this.parent = parent;
    this.dir = dir;
  }

  @Override
  public String toString( ) {
    return this.parent.toString( ) + File.separator + this.dir;
  }

  public void create( ) {
    final File dir = new File( this.toString( ) );
    if ( dir.exists( ) ) { return; }
    dir.mkdirs( );
  }
}
