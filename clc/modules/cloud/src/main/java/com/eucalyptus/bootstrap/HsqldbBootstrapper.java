package com.eucalyptus.bootstrap;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.hsqldb.Server;
import org.hsqldb.ServerConstants;
import org.hsqldb.persist.HsqlProperties;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.SubDirectory;
@Provides(resource=Resource.Database)
@Depends(resources={Resource.SystemCredentials})
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
  private HsqldbBootstrapper( ) {}

  @Override
  public boolean check( ) {
    return false;
  }

  @Override
  public boolean destroy( ) throws Exception {
    return false;
  }

  @Override
  public boolean load( ) throws Exception {
    db = new Server( );
    HsqlProperties props = new HsqlProperties( );
    props.setProperty( ServerConstants.SC_KEY_NO_SYSTEM_EXIT, true );
    props.setProperty( ServerConstants.SC_KEY_PORT, 9001 );
    props.setProperty( ServerConstants.SC_KEY_REMOTE_OPEN_DB, true );
    props.setProperty( ServerConstants.SC_KEY_DATABASE+".0", SubDirectory.DB.toString( ) + File.separator + EucalyptusProperties.NAME );
    props.setProperty( ServerConstants.SC_KEY_DBNAME+".0", EucalyptusProperties.NAME );
    String vol = "_volumes";
    props.setProperty( ServerConstants.SC_KEY_DATABASE+".1", SubDirectory.DB.toString( ) + File.separator + EucalyptusProperties.NAME + vol );
    props.setProperty( ServerConstants.SC_KEY_DBNAME+".1", EucalyptusProperties.NAME + vol );
    String auth = "_auth";
    props.setProperty( ServerConstants.SC_KEY_DATABASE+".2", SubDirectory.DB.toString( ) + File.separator + EucalyptusProperties.NAME + auth );
    props.setProperty( ServerConstants.SC_KEY_DBNAME+".2", EucalyptusProperties.NAME + auth );
    db.setProperties( props );
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    (new Thread(this)).start( );
    return true;
  }
  
  public void run() {
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
   * 110 * +-----------------+-------------+----------+------------------------------+
   * 111 * | OPTION | TYPE | DEFAULT | DESCRIPTION |
   * 112 * +-----------------+-------------+----------+------------------------------|
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
   * 123 * +-----------------+-------------+----------+------------------------------+
   */
}
