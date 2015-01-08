/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.system;

import java.io.File;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.scripting.ScriptExecutionFailedException;

public enum SubDirectory {
  SYSFAULTS( BaseDirectory.LIB, "faults" ),
  CUSTOMFAULTS( BaseDirectory.HOME, "/etc/eucalyptus/faults" ),
  
  DB( BaseDirectory.STATE, "db" ) {
    @Override
    protected void assertPermissions( ) {
      
      super.assertPermissions( );
      try {
        Groovyness.exec( "chmod -R 700 " + this.toString( ) );
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
        Groovyness.exec( "chmod -R 700 " + this.toString( ) );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
  },
  TX( BaseDirectory.RUN, "/tx" ) {
    @Override
    protected void assertPermissions( ) {
      
      super.assertPermissions( );
      try {
        Groovyness.exec( "chmod -R 700 " + this.toString( ) );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
  },
  CLASSCACHE( BaseDirectory.RUN, "/classcache" ),
  WWW( BaseDirectory.CONF, "www" ),
  WEBAPPS( BaseDirectory.STATE, "webapps" ),
  KEYS( BaseDirectory.STATE, "keys" ) {
    @Override
    protected void assertPermissions( ) {
      
      super.assertPermissions( );
      try {
        Groovyness.exec( "chmod -R 700 " + this.toString( ) );
        Groovyness.exec( "chgrp -R -L " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
        Groovyness.exec( "chown -R -L " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
  },
  SCRIPTS( BaseDirectory.CONF, "scripts" ),
  MANAGEMENT( BaseDirectory.CONF, "jmx" ),
  STATUS( BaseDirectory.RUN, "status") {
    @Override
    protected void assertPermissions () {
      super.assertPermissions();
      try {
        Groovyness.exec( "chmod -R 760 " + this.toString( ) );
        //Same as loaded in com.eucalyptus.monitoring.MonitoringConfiguration
        Groovyness.exec( "chgrp -R -L " + System.getProperty( "euca.status_group" ) + " " + this.toString( ) );
        Groovyness.exec( "chown -R -L " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
  },
  UPGRADE( BaseDirectory.VAR, "upgrade" ),
  REPORTS( BaseDirectory.CONF, "reports" ),
  CONF( BaseDirectory.CONF, "conf" ),
  QUEUE( BaseDirectory.VAR, "queue" ),
  LIB( BaseDirectory.LIB, "" ),
  RUNDB( BaseDirectory.RUN, "/db" ) {
    
    @Override
    protected void assertPermissions( ) {
      
      super.assertPermissions( );
      try {
        Groovyness.exec( "chmod -R -L +rwX " + this.toString( ) );
        Groovyness.exec( "chgrp -R -L " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
    
  };
  
  private static Logger LOG = Logger.getLogger( SubDirectory.class );
  BaseDirectory         parent;
  String                dir;
  
  SubDirectory( final BaseDirectory parent, final String dir ) {
    this.parent = parent;
    this.dir = dir;
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
      this.assertPermissions( );
    } else {
      EventRecord.here( SubDirectory.class, EventType.SYSTEM_DIR_CREATE, this.name( ), this.toString( ) ).info( );
      if ( dir.mkdirs( ) ) {
        this.assertPermissions( );
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
    String ret = this.toString( );
    for ( String s : path ) {
      ret += File.separator + s;
    }
    return ret;
  }
  
  protected void assertPermissions( ) {
    try {
      Groovyness.exec( "chown -R  " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
      Groovyness.exec( "chgrp -R  " + System.getProperty( "euca.user" ) + " " + this.toString( ) );
      Groovyness.exec( "chmod -R  +rwX " + this.toString( ) );
    } catch ( ScriptExecutionFailedException ex ) {
      LOG.error( ex, ex );
    }
  }
}
