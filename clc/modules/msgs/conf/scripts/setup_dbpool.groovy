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

import groovy.xml.MarkupBuilder
import org.apache.log4j.Logger
import org.logicalcobwebs.proxool.ProxoolFacade
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.bootstrap.Host
import com.eucalyptus.bootstrap.Hosts
import com.eucalyptus.component.ServiceUris
import com.eucalyptus.component.id.Database
import com.eucalyptus.entities.PersistenceContexts
import com.eucalyptus.system.SubDirectory
import com.eucalyptus.util.LogUtil


Logger LOG = Logger.getLogger( "com.eucalyptus.scripts.setup_dbpool" );

ClassLoader.getSystemClassLoader().loadClass('org.logicalcobwebs.proxool.ProxoolDriver');
ClassLoader.getSystemClassLoader().loadClass('net.sf.hajdbc.state.simple.SimpleStateManager');

String real_jdbc_driver = Databases.getDriverName( );
String pool_db_driver = 'net.sf.hajdbc.sql.Driver';
String pool_db_url = 'jdbc:ha-jdbc';
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

def setupDbPool = { String ctx_simplename ->
  String context_name = ctx_simplename.replaceAll("eucalyptus_","")
  String context_pool_alias = ctx_simplename;
  String ha_jdbc_config_file_name = SubDirectory.TX.toString( ) + "/ha_jdbc_${context_name}.xml";
  LogUtil.logHeader( "${ctx_simplename} Setting up database connection pool -> ${ha_jdbc_config_file_name}" )
  
  
  LOG.info( "${ctx_simplename} Preparing jdbc cluster:        ${ha_jdbc_config_file_name}" )
  new File( ha_jdbc_config_file_name ).withWriter{ writer ->
    def xml = new MarkupBuilder(writer);
    xml.'ha-jdbc'(xmlns: 'urn:ha-jdbc:cluster:2.1') {
      sync(id:'full') {
        'property'(name:'fetchSize', '1000')
        'property'(name:'maxBatchSize', '1000')
      }
      sync(id:'passive');
      state(id:'simple');
      cluster(
//          'auto-activate-schedule':'0 * * ? * *',
          balancer:'simple', //(simple|random|round-robin|load)
          'default-sync': 'passive',
          dialect:Databases.getJdbcDialect( ),
          durability:'none',//(none|coarse|fine)
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
                  weight:(Hosts.isCoordinator(host)?100:1),
                  location:("jdbc:${ServiceUris.remote(Database.class,host.getBindAddress( ), context_pool_alias ).toASCIIString( )}")
                  ) {
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
  String url = "proxool.${context_pool_alias}:${pool_db_driver}:${pool_db_url}:${ctx_simplename}";
  LOG.info( "${ctx_simplename} Preparing connection pool:     ${url}" )
  
  // Register proxool
  LOG.trace( proxool_config )
  ProxoolFacade.registerConnectionPool(url, proxool_config);
  ProxoolFacade.disableShutdownHook();
}

PersistenceContexts.list( ).each{ setupDbPool(it) }
setupDbPool("database_events");
