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

import org.apache.log4j.Logger;
import org.hsqldb.Server;
import org.hsqldb.ServerConstants;
import org.hsqldb.persist.HsqlProperties;

import com.eucalyptus.util.SubDirectory;

@Provides( resource = Resource.Database )
@Depends( resources = Resource.SystemCredentials, local = Component.eucalyptus )
public class HsqldbBootstrapper extends Bootstrapper implements Runnable {
  private static Logger             LOG = Logger.getLogger( HsqldbBootstrapper.class );
  private static HsqldbBootstrapper singleton;

  public static HsqldbBootstrapper getInstance( ) {
    synchronized ( HsqldbBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new HsqldbBootstrapper( );
      }
    }
    return singleton;
  }

  private Server db;
  private String fileName;

  private HsqldbBootstrapper( ) {
  }

  @Override
  public boolean check( ) {
    return false;
  }

  @Override
  public boolean destroy( ) throws Exception {
    return false;
  }

  @Override
  public boolean load( Resource current ) throws Exception {
    this.db = new Server( );
    Component.db.markLocal( );
    Component.db.setHostAddress( "127.0.0.1" );
    System.setProperty( "euca.db.password", "" );
    System.setProperty( "euca.db.url", Component.db.getUri( ).toASCIIString( ) );
    HsqlProperties props = new HsqlProperties( );
    props.setProperty( ServerConstants.SC_KEY_NO_SYSTEM_EXIT, true );
    int dbPort = 9001;
    props.setProperty( ServerConstants.SC_KEY_PORT, 9001 );
    props.setProperty( ServerConstants.SC_KEY_REMOTE_OPEN_DB, true );
    String general= "_general";
    props.setProperty( ServerConstants.SC_KEY_DATABASE + ".0", SubDirectory.DB.toString( ) + File.separator + Component.eucalyptus.name() + general );
    props.setProperty( ServerConstants.SC_KEY_DBNAME + ".0", Component.eucalyptus.name() + general );
    String vol = "_images";
    props.setProperty( ServerConstants.SC_KEY_DATABASE + ".1", SubDirectory.DB.toString( ) + File.separator + Component.eucalyptus.name() + vol );
    props.setProperty( ServerConstants.SC_KEY_DBNAME + ".1", Component.eucalyptus.name() + vol );
    String auth = "_auth";
    props.setProperty( ServerConstants.SC_KEY_DATABASE + ".2", SubDirectory.DB.toString( ) + File.separator + Component.eucalyptus.name() + auth );
    props.setProperty( ServerConstants.SC_KEY_DBNAME + ".2", Component.eucalyptus.name() + auth );
    String config = "_config";
    props.setProperty( ServerConstants.SC_KEY_DATABASE + ".3", SubDirectory.DB.toString( ) + File.separator + Component.eucalyptus.name() + config );
    props.setProperty( ServerConstants.SC_KEY_DBNAME + ".3", Component.eucalyptus.name() + config );
    this.db.setProperties( props );
    this.db.start( );
    if( !Component.walrus.isLocal( ) || !Component.storage.isLocal( ) ) {
      
    }
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    while( this.db.getState( ) != 1 ) {
      Throwable t = this.db.getServerError( );
      if( t != null ) {
        LOG.error( t, t );
        throw new RuntimeException(t);
      }
      LOG.info( "Waiting for database to start..." );
    }
    return true;
  }

  public void run( ) {
    this.db.start( );
  }

  @Override
  public boolean stop( ) {
    return false;
  }

  public String getFileName( ) {
    return fileName;
  }

  public void setFileName( String fileName ) {
    LOG.info( "Setting hsqldb filename=" + fileName );
    this.fileName = fileName;
  }

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
   * 110 *
   * +-----------------+-------------+----------+------------------------------+
   * 111 * | OPTION | TYPE | DEFAULT | DESCRIPTION |
   * 112 *
   * +-----------------+-------------+----------+------------------------------|
   * 113 * | --help | | | prints this message |
   * 114 * | --address | name|number | any | server inet address |
   * 115 * | --port | number | 9001/544 | port at which server listens |
   * 116 * | --database.i | [type]spec | 0=test | path of database i |
   * 117 * | --dbname.i | alias | | url alias for database i |
   * 118 * | --silent | true|false | true | false => display all queries |
   * 119 * | --trace | true|false | false | display JDBC trace messages |
   * 120 * | --tls | true|false | false | TLS/SSL (secure) sockets |
   * 121 * | --no_system_exit| true|false | false | do not issue System.exit() |
   * 122 * | --remote_open | true|false | false | can open databases remotely |
   * 123 *
   * +-----------------+-------------+----------+------------------------------+
   */
}
