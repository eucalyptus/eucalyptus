/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.system;

import java.io.File;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.scripting.ScriptExecutionFailedException;

public enum BaseDirectory {
  HOME( "euca.home" ),
  VAR( "euca.var.dir", "var", "lib", "eucalyptus" ),
  ETC( "euca.etc.dir", "etc", "eucalyptus", "cloud.d" ),
  CONF( "euca.conf.dir", "etc", "eucalyptus"  ),
  LIB( "euca.lib.dir", "usr", "share", "eucalyptus" ),
  LOG( "euca.log.dir", "var", "log", "eucalyptus" ),
  RUN( "euca.run.dir", "var", "run", "eucalyptus"  ),
  LIBEXEC( "euca.libexec.dir", "usr", "lib", "eucalyptus" ),
  STATE( "euca.state.dir",  "var", "lib", "eucalyptus" ),
  ;

  private static Logger LOGG = Logger.getLogger( BaseDirectory.class );
  
  private String        key;
  private String[]      defaultPath; // based on HOME

  BaseDirectory( final String key ) {
    this.key = key;
    this.defaultPath = null;
  }

  BaseDirectory( final String key, final String... defaultPath ) {
    this.key = key;
    this.defaultPath = defaultPath;
  }

  public boolean check( ) {
    if ( System.getProperty( this.key ) == null && this.defaultPath == null ) {
      LOGG.fatal( "System property '" + this.key + "' must be set." );
      return false;
    }
    this.create( );
    return true;
  }
  
  @Override
  public String toString( ) {
    return System.getProperty( this.key, this.defaultPath==null ? null : HOME.getChildPath( this.defaultPath ) );
  }
  
  public File getFile( ) {
    return new File( this.toString( ) );
  }
  
  public void create( ) {
    final File dir = new File( this.toString( ) );
    if ( !dir.exists( ) ) {
      EventRecord.here( SubDirectory.class, EventType.SYSTEM_DIR_CREATE, this.name( ), this.toString( ) ).info( );
      if( dir.mkdirs( ) ) {
        this.assertPermissions( );
      }
    }
  }
  
  public File getChildFile( String... path ) {
    return new File( getChildPath( path ) );
  }
  
  public String getChildPath( String... args ) {
    return Directories.getChildPath( this.toString( ), args );
  }

  private void assertPermissions( ) {
    try {
      Directories.execPermissions( "chown " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
    } catch ( ScriptExecutionFailedException ex ) {
      LOGG.error( ex, ex );
    }
    try {
      Directories.execPermissions( "chmod og-rwX " + this.toString( ) );
    } catch ( ScriptExecutionFailedException ex ) {
      LOGG.error( ex, ex );
    }
  }
}
