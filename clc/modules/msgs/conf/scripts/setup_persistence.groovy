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
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

import com.eucalyptus.system.SubDirectory;

import org.apache.log4j.Logger
import org.hibernate.ejb.Ejb3Configuration
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.bootstrap.SystemIds
import com.eucalyptus.entities.PersistenceContexts

Logger LOG = Logger.getLogger( "setup_persistence" );

String real_jdbc_driver = Databases.getDriverName( );;
String pool_db_driver = 'net.sf.hajdbc.sql.Driver';
String pool_db_url = 'jdbc:ha-jdbc:eucalyptus';
String db_pass = SystemIds.databasePassword( );

default_hiber_config = [
      'hibernate.archive.autodetection': 'jar, class, hbm',
      'hibernate.ejb.interceptor.session_scoped': 'com.eucalyptus.entities.DelegatingInterceptor',
      'hibernate.show_sql': 'false',
      'hibernate.format_sql': 'false',
      'hibernate.connection.autocommit': 'false',
      'hibernate.connection.release_mode': 'after_statement',
      'hibernate.hbm2ddl.auto': 'update',
      'hibernate.generate_statistics': 'true',
      'hibernate.bytecode.use_reflection_optimizer': 'true',
      'hibernate.cglib.use_reflection_optimizer': 'true',
    ]

ClassLoader.getSystemClassLoader().loadClass('com.eucalyptus.empyrean.EmpyreanTransactionManager');

PersistenceContexts.list( ).each { String ctx_simplename ->
  
  String context_name = ctx_simplename.replaceAll("eucalyptus_","")
  
  // Configure the hibernate connection
  hibernate_config = [:]
  hibernate_config.putAll(default_hiber_config)
  hibernate_config.putAll( [
        /** jdbc driver **/
        'hibernate.dialect': Databases.getHibernateDialect( ),
        /** db pools **/
        'hibernate.connection.provider_class': 'org.hibernate.connection.ProxoolConnectionProvider',
        'hibernate.proxool.pool_alias': "eucalyptus_${context_name}",
        'hibernate.proxool.existing_pool': 'true',
        /** transactions **/
        'hibernate.current_session_context_class': 'jta',
        'hibernate.jndi.class': 'bitronix.tm.jndi.BitronixInitialContextFactory',
        'hibernate.transaction.flush_before_completion':'false',
        'hibernate.transaction.auto_close_session':'false',
        'hibernate.transaction.manager_lookup_class': 'com.eucalyptus.empyrean.EmpyreanTransactionManager',
        /** l2 cache **/
        'hibernate.cache.use_second_level_cache': 'true',
        'hibernate.cache.use_query_cache': 'false',//GRZE: make it false!
        'hibernate.cache.jbc.query.localonly': 'true',
        'hibernate.cache.default_cache_concurrency_strategy': 'transactional',
        'hibernate.cache.region.factory_class': 'com.eucalyptus.bootstrap.CacheRegionFactory',
        'hibernate.cache.region.jbc2.cfg.shared': 'eucalyptus_jboss_cache.xml',
        'hibernate.cache.region.jbc2.cfg.multiplexer.stacks': 'eucalyptus_cache_jgroups.xml',
        'hibernate.cache.jbc.cfg.jgroups.stacks': 'eucalyptus_cache_jgroups.xml',
        'hibernate.cache.region_prefix': "eucalyptus_${context_name}_cache",
        'hibernate.cache.use_minimal_puts': 'true',
        'hibernate.cache.use_structured_entries': 'true',
      ] )
  
  // Register the properties with the config
  config = new Ejb3Configuration();
  LOG.info( "${ctx_simplename} Setting up persistence:        done." )
  hibernate_config.each { k , v ->
    LOG.trace( "${k} = ${v}" )
    config.setProperty( k, v )
  }
  
  // Register the system-discovered entity list
  LOG.info( "${ctx_simplename} Registered entities:           " +
      PersistenceContexts.listEntities( ctx_simplename ).collect{ ent ->
        config.addAnnotatedClass( ent )
        ent.getSimpleName()
      }
      )
  
  // Register the context
  try {
    PersistenceContexts.registerPersistenceContext(ctx_simplename, config)
  } catch( Exception t ) {
    t.printStackTrace();
  }
}


