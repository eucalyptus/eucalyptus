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


import com.eucalyptus.component.annotation.DatabaseNamingStrategy
import org.apache.log4j.Logger
import org.logicalcobwebs.proxool.ProxoolException
import org.logicalcobwebs.proxool.ProxoolFacade
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.bootstrap.DatabaseInfo;
import com.eucalyptus.bootstrap.Host
import com.eucalyptus.bootstrap.Hosts
import com.eucalyptus.component.ServiceUris
import com.eucalyptus.component.id.Database
import com.eucalyptus.system.SubDirectory
import com.eucalyptus.util.LogUtil

Logger LOG = Logger.getLogger( 'com.eucalyptus.scripts.setup_dbpool_remote' );

ClassLoader.getSystemClassLoader().loadClass('org.logicalcobwebs.proxool.ProxoolDriver');
ClassLoader.getSystemClassLoader().loadClass('com.eucalyptus.database.activities.VmDatabaseSSLSocketFactory');
String pool_db_driver = 'org.postgresql.Driver';
final DatabaseInfo dbInfo = DatabaseInfo.getDatabaseInfo();
String db_host = dbInfo.getAppendOnlyHost();
String db_port = dbInfo.getAppendOnlyPort();
String pool_db_url = String.format("jdbc:postgresql://%s:%s", db_host, db_port );


String db_user = null; 
String db_pass = null;
if ("localhost".equals(db_host)) {
  db_user = Databases.userName;
  db_pass = Databases.password;
}else {
  db_user = dbInfo.getAppendOnlyUser();
  db_pass = dbInfo.getAppendOnlyPassword();
}

default_pool_props = [
      'proxool.simultaneous-build-throttle': '32',
      'proxool.minimum-connection-count': '8',
      'proxool.maximum-connection-count': '512',
      'proxool.prototype-count': '8',
      'proxool.house-keeping-test-sql': 'SELECT 1=1;',
      'proxool.house-keeping-sleep-time': '5000',
      'proxool.test-before-use': 'false',
      'proxool.test-after-use': 'false',
      'proxool.trace': 'false',
      'user': db_user,
      'password': db_pass,
    ]

def cleanDbPool = { String db_name ->
  ProxoolFacade.removeConnectionPool(db_name);
  LOG.info( "${db_name} Cleaned up remote jdbc pool");
}

def setupDbPool = { String db_name ->
  LOG.info( "${db_name} Preparing remote jdbc pool" );
    // Setup proxool
  proxool_config = new Properties();
  proxool_config.putAll(default_pool_props);
  String sslParam = "ssl=true&sslfactory=com.eucalyptus.database.activities.VmDatabaseSSLSocketFactory"
  String timeout = "connectTimeout=7&socketTimeout=7&loginTimeout=7" 
  /* default 30 sec timeout causes shutdown hanging when remote db is disconnected
   * When shutting down proxool pools, proxool's lock wait until the existing connection pools are given socket timeout exception. 
   * The worst case wait time is { # of remote DBs x timeout }, which is roughly 14 seconds. 
   */
  String url = "proxool.${db_name}:${pool_db_driver}:${pool_db_url}/${db_name}?${sslParam}&${timeout}";
  LOG.info( "${db_name} Preparing connection pool:     ${url}" )
  
  // Register proxool
  LOG.trace( proxool_config )
  ProxoolFacade.registerConnectionPool(url, proxool_config);
}

if ("localhost".equals(db_host)){
  LOG.info("The exising local db pool will be used for append-only data");
  return
}

if ((db_user == null || db_user.length() <= 0) ||
(db_host == null || db_host.length() <= 0) ||
(db_pass == null || db_pass.length() <= 0)) {
  LOG.info("Cannot setup remote db pool due to missing parameters");
  return;
}

Databases.remoteDatabases().each { db_name ->
  try{
    cleanDbPool( db_name );
  }catch(final ProxoolException ex){
    ;
  }
  setupDbPool( db_name );
}