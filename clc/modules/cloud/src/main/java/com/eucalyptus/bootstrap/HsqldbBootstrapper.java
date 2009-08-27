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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
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
    LOG.info( "-> database host: " + System.getProperty("euca.db.host") );
    LOG.info( "-> database port: " + System.getProperty("euca.db.port") );
    HsqlProperties props = new HsqlProperties( );
    props.setProperty( ServerConstants.SC_KEY_NO_SYSTEM_EXIT, true );
    String dbPort = System.getProperty( DatabaseConfig.EUCA_DB_PORT );
    props.setProperty( ServerConstants.SC_KEY_PORT, Integer.parseInt( dbPort ) );
    props.setProperty( ServerConstants.SC_KEY_REMOTE_OPEN_DB, true );
    props.setProperty( ServerConstants.SC_KEY_DATABASE + ".0", SubDirectory.DB.toString( ) + File.separator + Component.eucalyptus.name() );
    props.setProperty( ServerConstants.SC_KEY_DBNAME + ".0", Component.eucalyptus.name() );
    String vol = "_volumes";
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
