/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
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
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.bootstrap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hsqldb.ServerConstants;

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.SubDirectory;

public class DatabaseConfig {
  private static String DEFAULT = 
    "CREATE SCHEMA PUBLIC AUTHORIZATION DBA\n" + 
    "CREATE USER SA PASSWORD \"%s\"\n" + 
    "GRANT DBA TO SA\n" + 
    "SET WRITE_DELAY 100 MILLIS\n" + 
    "SET SCHEMA PUBLIC\n";
  static {
    if( !System.getProperties( ).contains( PropertyKey.HOST ) ) System.setProperty( PropertyKey.HOST.toString( ), "127.0.0.1" );
    if( !System.getProperties( ).contains( PropertyKey.PORT ) ) System.setProperty( PropertyKey.PORT.toString( ), "9001" );
    if( !System.getProperties( ).contains( PropertyKey.PASSWORD ) )System.setProperty( PropertyKey.PASSWORD.toString( ), "" );
  }
  private static DatabaseConfig singleton = new DatabaseConfig();
  private static Logger LOG = Logger.getLogger( DatabaseConfig.Internal.class );
  public static DatabaseConfig getInstance() {
    return singleton;
  }
  public static void setInstance( DatabaseConfig dbConfig ) {
    singleton = dbConfig;
  }
  
  enum Internal {
    general,images,auth,config,walrus,storage,dns;
    public void prepareDatabase( ) throws IOException {
      File dbFile = new File( SubDirectory.DB.toString( ) + File.separator + this.getDatabaseName( ) + ".script" );
      if ( !dbFile.exists( ) ) {
        FileWriter dbOut = new FileWriter( dbFile );
        dbOut.write( String.format( DEFAULT, System.getProperty( "euca.db.password" ) ) );
        dbOut.flush( );
        dbOut.close( );
      }
    }
    public String getDatabaseName() {
      return Component.eucalyptus.name( ) + "_" + this.name();
    }
    public Properties getProperties() {
      Properties props = new Properties( );
      props.setProperty( ServerConstants.SC_KEY_DATABASE + "." + this.ordinal( ), SubDirectory.DB.toString( ) + File.separator + this.getDatabaseName( ) );
      props.setProperty( ServerConstants.SC_KEY_DBNAME + "."+ this.ordinal( ), this.getDatabaseName( ) );
      return props;
    }
  }
  
  public static void initialize() throws IOException {
    Component.db.markLocal( );
    Component.db.markEnabled( );
    Component.db.setHostAddress( "127.0.0.1" );
    System.setProperty( "euca.db.url", Component.db.getUri( ).toASCIIString( ) );
    LOG.info( LogUtil.header( "Setting up database: " ) );
    System.setProperty( "euca.db.password", Hashes.getHexSignature( ) );
    for( Internal dbName : Internal.values( ) ) {
      LOG.info( dbName.getProperties( ) );
      dbName.prepareDatabase( );
    }
  }
  
  public static Properties getProperties() {
    Properties props = new Properties( );
    props.setProperty( ServerConstants.SC_KEY_NO_SYSTEM_EXIT, Boolean.TRUE.toString( ) );
    props.setProperty( ServerConstants.SC_KEY_PORT, "9001" );
    props.setProperty( ServerConstants.SC_KEY_REMOTE_OPEN_DB, Boolean.TRUE.toString( ) );
//    props.setProperty( ServerConstants.SC_KEY_TLS, Boolean.TRUE.toString( ) );
    for ( DatabaseConfig.Internal i : DatabaseConfig.Internal.values( ) ) {
      props.putAll( i.getProperties( ) );
    }
    return props;
  }
  
  enum PropertyKey {
    HOST("euca.db.host"),URL("euca.db.url"),PORT("euca.db.port"),PASSWORD("euca.db.password");
    private String property;

    private PropertyKey( String property ) {
      this.property = property;
    }
    @Override
    public String toString() {
      return this.property;
    }    
  }
  

  //TODO: handle persistence.xml issues here
  /*
   * hsqldb.script_format=0
   * runtime.gc_interval=0
   * sql.enforce_strict_size=false
   * hsqldb.cache_size_scale=8
   * readonly=false
   * hsqldb.nio_data_file=true
   * hsqldb.cache_scale=14
   * version=1.8.0
   * hsqldb.default_table_type=memory
   * hsqldb.cache_file_scale=1
   * hsqldb.log_size=200
   * modified=yes
   * hsqldb.cache_version=1.7.0
   * hsqldb.original_version=1.8.0
   * hsqldb.compatible_version=1.8.0
   */
}
