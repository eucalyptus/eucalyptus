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
 ************************************************************************/

import groovy.xml.MarkupBuilder
import org.apache.log4j.Logger
import org.logicalcobwebs.proxool.ProxoolFacade
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.bootstrap.Host
import com.eucalyptus.bootstrap.Hosts
import com.eucalyptus.bootstrap.SystemIds
import com.eucalyptus.component.ServiceUris
import com.eucalyptus.component.id.Eucalyptus.Database
import com.eucalyptus.entities.PersistenceContexts
import com.eucalyptus.system.SubDirectory
import com.eucalyptus.util.LogUtil


Logger LOG = Logger.getLogger( "com.eucalyptus.scripts.setup_dbpool" );

ClassLoader.getSystemClassLoader().loadClass('org.logicalcobwebs.proxool.ProxoolDriver');
ClassLoader.getSystemClassLoader().loadClass('net.sf.hajdbc.local.LocalStateManager');

String real_jdbc_driver = Databases.getDriverName( );
String pool_db_driver = 'net.sf.hajdbc.sql.Driver';
String pool_db_url = 'jdbc:ha-jdbc:eucalyptus';
String db_pass = Databases.getPassword();

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
      'user': 'eucalyptus',
      'password': db_pass,
    ]

PersistenceContexts.list( ).each { String ctx_simplename ->
  String context_name = ctx_simplename.replaceAll("eucalyptus_","")
  String context_pool_alias = "eucalyptus_${context_name}";
  String ha_jdbc_config_file_name = SubDirectory.TX.toString( ) + "/ha_jdbc_${context_name}.xml";
  LogUtil.logHeader( "${ctx_simplename} Setting up database connection pool -> ${ha_jdbc_config_file_name}" )
  
  
  LOG.info( "${ctx_simplename} Preparing jdbc cluster:        ${ha_jdbc_config_file_name}" )
  new File( ha_jdbc_config_file_name ).withWriter{ writer ->
    def xml = new MarkupBuilder(writer);
    xml.'ha-jdbc'() {
      sync('class':'com.eucalyptus.bootstrap.Databases\$FullSynchronizationStrategy', id:'full') {
        'property'(name:'fetchSize', '1000')
        'property'(name:'maxBatchSize', '1000')
      }
      sync('class':'com.eucalyptus.bootstrap.Databases\$PassiveSynchronizationStrategy', id:'passive');
      cluster(id:context_pool_alias,
//          'auto-activate-schedule':'0 * * ? * *',
          balancer:'simple', //(simple|random|round-robin|load)
          'default-sync': 'passive',
          dialect:Databases.getJdbcDialect( ),
          'failure-detect-schedule':'0/15 * * ? * *',
          'meta-data-cache':'none',//(none|lazy|eager)
          'transaction-mode':'serial',//(parallel|serial)
          'detect-sequences':'false',
          'detect-identity-columns':'false',
          'eval-current-date':'true',
          'eval-current-time':'true',
          'eval-current-timestamp':'true',
          'eval-rand':'true'
          ) {
            Hosts.listActiveDatabases( ).each{ Host host ->
              database(id:host.getBindAddress().getHostAddress( ),
                  local:host.isLocalHost( ),
                  weight:(host.equals(Hosts.getCoordinator())?100:1)
                  ) {
                    driver(real_jdbc_driver)
                    url("jdbc:${ServiceUris.remote(Database.class,host.getBindAddress( ), context_pool_alias ).toASCIIString( )}")
                    user('eucalyptus')
                    password(db_pass)
                  }
            }
          }
    }
  }
  
  
  
  // Setup proxool
  proxool_config = new Properties();
  proxool_config.putAll(default_pool_props);
  proxool_config.put('config',"file://"+ha_jdbc_config_file_name);
  String url = "proxool.${context_pool_alias}:${pool_db_driver}:${pool_db_url}_${context_name}";
  LOG.info( "${ctx_simplename} Preparing connection pool:     ${url}" )
  
  // Register proxool
  LOG.trace( proxool_config )
  ProxoolFacade.registerConnectionPool(url, proxool_config);
  ProxoolFacade.disableShutdownHook();
}
