/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.bootstrap;

import org.apache.log4j.Logger;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.scripting.ScriptExecutionFailedException;

public class Databases {
  private static final ScriptedDbBootstrapper singleton   = new ScriptedDbBootstrapper( );
  private static Logger                       LOG         = Logger.getLogger( Databases.class );
  private static final String                 DB_NAME     = "eucalyptus";
  public static final String                  DB_USERNAME = DB_NAME;

  public static String getUserName( ) {
    return DB_USERNAME;
  }
  
  public static String getDatabaseName( ) { 
    return DB_NAME;
  }
  
  public static String getPassword( ) {
    return SystemIds.databasePassword( );
  }
  
  public static String getDriverName( ) {
    return singleton.getDriverName( );
  }
  
  public static String getJdbcDialect( ) {
    return singleton.getJdbcDialect( );
  }
  
  public static String getHibernateDialect( ) {
    return singleton.getHibernateDialect( );
  }
  
  public static DatabaseBootstrapper getBootstrapper( ) {
    return singleton;
  }
  
  public static void initialize( ) {
    singleton.init( );
  }
  
  @RunDuring( Bootstrap.Stage.DatabaseInit )
  @Provides( Empyrean.class )
  @DependsLocal( Eucalyptus.class )
  public static class ScriptedDbBootstrapper implements DatabaseBootstrapper {
    DatabaseBootstrapper db;
    
    public ScriptedDbBootstrapper( ) {
      super( );
      try {
        this.db = Groovyness.newInstance( "setup_db" );
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    }
    
    public boolean load( ) throws Exception {
      return this.db.load( );
    }
    
    public boolean start( ) throws Exception {
      return this.db.start( );
    }
    
    public boolean stop( ) throws Exception {
      return this.db.stop( );
    }
    
    public void destroy( ) throws Exception {
      this.db.destroy( );
    }
    
    public boolean isRunning( ) {
      return this.db.isRunning( );
    }
    
    public void hup( ) {
      this.db.hup( );
    }
    
    public String getDriverName( ) {
      return this.db.getDriverName( );
    }
    
    @Override
    public String getJdbcDialect( ) {
      return this.db.getJdbcDialect( );
    }
    
    @Override
    public String getHibernateDialect( ) {
      return this.db.getHibernateDialect( );
    }
    
    @Override
    public void init( ) {
      this.db.init( );
    }
    
    public static DatabaseBootstrapper getInstance( ) {
      return singleton;
    }
    
    @Override
    public String getUriPattern( ) {
      return this.db.getUriPattern( );
    }
  }
  
  public static String getUriPattern( ) {
    return singleton.getUriPattern( );
  }
  
}
