/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

public enum SubDirectory {
  SYSFAULTS( BaseDirectory.LIB, "faults", false ),
  CUSTOMFAULTS( BaseDirectory.ETC, "faults", false  ),
  DB( BaseDirectory.STATE, "db" ) {
    @Override
    protected void assertPermissions( ) {
      super.assertPermissions( );
      try {
        Directories.execPermissions( "chmod -R 700 " + this.toString( ) );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
  },
  BACKUPS( BaseDirectory.STATE, "backups" ) {
    @Override
    protected void assertPermissions( ) {
      super.assertPermissions( );
      try {
        Directories.execPermissions( "chmod -R 700 " + this.toString( ) );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
  },
  TX( BaseDirectory.RUN, "tx" ) {
    @Override
    protected void assertPermissions( ) {
      super.assertPermissions( );
      try {
        Directories.execPermissions( "chmod -R 700 " + this.toString( ) );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
  },
  CLASSCACHE( BaseDirectory.RUN, "classcache" ),
  KEYS( BaseDirectory.STATE, "keys" ) {
    @Override
    protected void assertPermissions( ) {
      super.assertPermissions( );
      try {
        Directories.execPermissions( "chmod -R 700 " + this.toString( ) );
        Directories.execPermissions( "chgrp -R -L " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
        Directories.execPermissions( "chown -R -L " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
  },
  SCRIPTS( BaseDirectory.ETC, "scripts", false ),
  MANAGEMENT( BaseDirectory.ETC, "jmx", false ),
  STATUS( BaseDirectory.RUN, "status") {
    @Override
    protected void assertPermissions () {
      try {
        // set 750 permission only to directories
        Directories.execPermissions( "chmod 750 $(find " + this.toString( ) + " -type d)");
        Directories.execPermissions( "chgrp -R -L " + System.getProperty( "euca.status_group", "eucalyptus-status" ) + " " + this.toString( ) );
        Directories.execPermissions( "chown -R -L " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
  },
  UPGRADE( BaseDirectory.VAR, "upgrade" ),
  QUEUE( BaseDirectory.VAR, "queue" ),
  LIB( BaseDirectory.LIB, "", false ),
  RUNDB( BaseDirectory.RUN, "/db" ) {
    @Override
    protected void assertPermissions( ) {
      super.assertPermissions( );
      try {
        Directories.execPermissions( "chmod -R -L +rwX " + this.toString( ) );
        Directories.execPermissions( "chgrp -R -L " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
  };
  
  private static Logger LOG = Logger.getLogger( SubDirectory.class );
  private final BaseDirectory         parent;
  private final String                dir;
  private final boolean               assertPermissions;

  SubDirectory( final BaseDirectory parent, final String dir ) {
    this( parent, dir, true );
  }

  SubDirectory( final BaseDirectory parent, final String dir, final boolean assertPermissions ) {
    this.parent = parent;
    this.dir = dir;
    this.assertPermissions = assertPermissions;
  }
  
  @Override
  public String toString( ) {
    return this.parent.toString( ) + File.separator + this.dir;
  }
  
  public File getFile( ) {
    return new File( this.toString( ) );
  }

  public void check( ) {
    final File dir = new File( this.toString( ) );
    if ( dir.exists( ) ) {
      if ( assertPermissions ) {
        this.assertPermissions( );
      }
    } else {
      EventRecord.here( SubDirectory.class, EventType.SYSTEM_DIR_CREATE, this.name( ), this.toString( ) ).info( );
      if ( dir.mkdirs( ) ) {
        this.assertPermissions( ); // always assert permissions when creating
      }
    }
  }
  
  public File getChildFile( String... path ) {
    return new File( getChildPath( path ) );
  }
  
  public boolean hasChild( String... path ) {
    return getChildFile( path ).exists( );
  }
  
  public String getChildPath( String... path ) {
    return Directories.getChildPath( this.toString( ), path );
  }
  
  protected void assertPermissions( ) {
    try {
      Directories.execPermissions( "chown -R  " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
      Directories.execPermissions( "chgrp -R  " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
      Directories.execPermissions( "chmod -R  +rwX " + this.toString( ) );
    } catch ( ScriptExecutionFailedException ex ) {
      LOG.error( ex, ex );
    }
  }
}
