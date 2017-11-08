/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import com.eucalyptus.bootstrap.BootstrapArgs
import com.eucalyptus.bootstrap.Host
import com.eucalyptus.bootstrap.Hosts
import com.eucalyptus.component.annotation.DatabaseNamingStrategy
import org.apache.log4j.Logger
import org.logicalcobwebs.proxool.ProxoolFacade
import org.logicalcobwebs.proxool.StateListenerIF
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.component.ServiceUris
import com.eucalyptus.component.id.Database


Logger LOG = Logger.getLogger( 'com.eucalyptus.scripts.setup_dbpool' );

ClassLoader.getSystemClassLoader().loadClass('org.logicalcobwebs.proxool.ProxoolDriver');

String pool_db_driver = Databases.driverName;
String db_user = Databases.userName
String db_pass = Databases.password

default_pool_props = [
      'proxool.simultaneous-build-throttle': '1000000',
      'proxool.minimum-connection-count': '8',
      'proxool.maximum-connection-count': '512',
      'proxool.prototype-count': '8',
      'proxool.house-keeping-test-sql': 'SELECT 1=1;',
      'proxool.house-keeping-sleep-time': '5000',
      'proxool.recently-started-threshold': '10000',
      'proxool.test-before-use': 'false',
      'proxool.test-after-use': 'false',
      'proxool.trace': 'false',
      'user': db_user,
      'password': db_pass,
]

def setupDbPool = { String db_name ->
  // Setup proxool
  proxool_config = new Properties();
  proxool_config.putAll(default_pool_props);
  if ( DatabaseNamingStrategy.SHARED_DATABASE_NAME == db_name ) {
    // properties for database pool shared between contexts
    proxool_config.setProperty( 'proxool.minimum-connection-count', '16' )
    proxool_config.setProperty( 'proxool.maximum-connection-count', '1024' )
  } else if ( 'database_events' == db_name ) {
    proxool_config.setProperty( 'proxool.minimum-connection-count', '0' )
    proxool_config.setProperty( 'proxool.maximum-connection-count', '8' )
    proxool_config.setProperty( 'proxool.prototype-count', '1' )
  }
  Host host = BootstrapArgs.cloudController ? 
      Hosts.localHost( ) : 
      Hosts.listActiveDatabases( ).get( 0 )
  String url = "proxool.${db_name}:${pool_db_driver}:jdbc:${ServiceUris.remote(Database.class,host.isLocalHost()?InetAddress.getByName('127.0.0.1'):host.getBindAddress( ), db_name ).toASCIIString( )}";
  LOG.info( "${db_name} Preparing connection pool:     ${url}" )

  // Register proxool
  LOG.trace( proxool_config )
  ProxoolFacade.registerConnectionPool(url, proxool_config);
  if ( 'database_events' != db_name ) ProxoolFacade.addStateListener(
      db_name, 
      { Integer state -> Databases.setVolatile( 
          state == StateListenerIF.STATE_DOWN ||
          state == StateListenerIF.STATE_OVERLOADED ) 
      } as StateListenerIF )
  ProxoolFacade.disableShutdownHook();
}

Databases.databases( ).each{ String database ->
  setupDbPool( database )
}
setupDbPool('database_events')
