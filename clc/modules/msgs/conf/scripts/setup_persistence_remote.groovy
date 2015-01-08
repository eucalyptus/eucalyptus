/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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


import com.eucalyptus.component.annotation.DatabaseNamingStrategy
import org.apache.log4j.Logger
import org.hibernate.ejb.Ejb3Configuration
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.bootstrap.SystemIds
import com.eucalyptus.entities.PersistenceContexts
import com.eucalyptus.util.EucalyptusCloudException

Logger LOG = Logger.getLogger( 'com.eucalyptus.scripts.setup_persistence_remote' );

default_hiber_config = [
      'hibernate.archive.autodetection': 'jar, class, hbm',
      'hibernate.ejb.interceptor.session_scoped': 'com.eucalyptus.entities.DelegatingInterceptor',
      'hibernate.show_sql': 'false',
      'hibernate.format_sql': 'false',
      'hibernate.connection.autocommit': 'false',
      'hibernate.connection.release_mode': 'after_transaction',
      'hibernate.hbm2ddl.auto': 'update',
      'hibernate.generate_statistics': 'false',
      'hibernate.bytecode.use_reflection_optimizer': 'true',
      'hibernate.default_batch_fetch_size': '50',
]

PersistenceContexts.listRemotable().each { String context_name ->
  
  LOG.info("attempting to deregister persistence context: " + context_name);
  try {
    PersistenceContexts.deregisterPersistenceContext( context_name);
  }catch( final Exception ex){
    LOG.error("failed to deregister persistence context: "+context_name, ex);
    throw new EucalyptusCloudException("failed to deregister persistence context: "+context_name, ex);
  }
  LOG.info("deregistered persistence context: "+context_name);
  
  
    // Configure the hibernate connection
  hibernate_config = [:]
  hibernate_config.putAll((Map<?,?>)default_hiber_config)
  hibernate_config.putAll( [
        /** jdbc driver **/
        'hibernate.dialect': Databases.getHibernateDialect( ),
        /** db pools **/
        'hibernate.connection.provider_class': 'org.hibernate.service.jdbc.connections.internal.ProxoolConnectionProvider',
        'hibernate.proxool.pool_alias': PersistenceContexts.toRemoteDatabaseName(context_name), // remote database
        'hibernate.proxool.existing_pool': 'true',
        /** transactions **/
        'hibernate.transaction.auto_close_session': 'false',
        'hibernate.transaction.flush_before_completion': 'false',
        'hibernate.transaction.jta.platform': 'org.hibernate.service.jta.platform.internal.BitronixJtaPlatform',
        /** l2 cache **/
        'hibernate.cache.use_second_level_cache': 'false',
        'hibernate.cache.use_query_cache': 'false'
      ] )
  
  String schemaName = PersistenceContexts.toRemoteSchemaName( context_name );
  if ( schemaName ) {
    hibernate_config.put( 'hibernate.default_schema', schemaName )
  }

    // Register the properties with the config
  config = new Ejb3Configuration();
  LOG.info( "${context_name} Setting up persistence:        done." )
  hibernate_config.each { k , v ->
    LOG.trace( "${k} = ${v}" )
    config.setProperty( k, v )
  }
  
  // Register the system-discovered entity list
  LOG.info( "${context_name} Registered entities:           " +
      PersistenceContexts.listEntities( context_name ).collect{ Class<?> ent ->
        config.addAnnotatedClass( ent )
        ent.simpleName
      }
  )
  // Register the context
  try {
    PersistenceContexts.registerPersistenceContext(context_name, config)
  } catch( Exception t ) {
    t.printStackTrace();
  }
}
