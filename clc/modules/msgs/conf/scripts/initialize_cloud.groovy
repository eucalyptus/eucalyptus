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

import java.util.Properties
import org.apache.log4j.Logger
import org.hibernate.ejb.Ejb3Configuration
import com.eucalyptus.bootstrap.Bootstrap
import com.eucalyptus.bootstrap.BootstrapArgs
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.bootstrap.ServiceJarDiscovery
import com.eucalyptus.component.ComponentDiscovery
import com.eucalyptus.component.ServiceBuilder
import com.eucalyptus.component.ServiceBuilders
import com.eucalyptus.component.ServiceConfiguration
import com.eucalyptus.component.ServiceConfigurations
import com.eucalyptus.component.ServiceUris
import com.eucalyptus.component.ServiceBuilders.ServiceBuilderDiscovery
import com.eucalyptus.component.auth.SystemCredentials
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.component.id.Eucalyptus.Database
import com.eucalyptus.entities.PersistenceContextDiscovery
import com.eucalyptus.entities.PersistenceContexts
import com.eucalyptus.system.DirectoryBootstrapper
import com.eucalyptus.util.Internets

Logger LOG = Logger.getLogger( Bootstrap.class );

if( BootstrapArgs.isInitializeSystem( ) ) {
  new DirectoryBootstrapper( ).load( );
  ServiceJarDiscovery.doSingleDiscovery(  new ComponentDiscovery( ) );
  [
    new ServiceBuilderDiscovery( ),
    new PersistenceContextDiscovery( )
  ].each{
    ServiceJarDiscovery.runDiscovery( it );
  }
  SystemCredentials.initialize( );
}
try {
  Databases.initialize( );
  try {
    props = [
          "hibernate.archive.autodetection": "jar, class, hbm",
          "hibernate.show_sql": "false",
          "hibernate.format_sql": "false",
          "hibernate.connection.autocommit": "true",
          "hibernate.hbm2ddl.auto": "create",
          "hibernate.generate_statistics": "true",
          "hibernate.connection.driver_class": Databases.getDriverName( ),
          "hibernate.connection.username": "eucalyptus",
          "hibernate.connection.password": Databases.getPassword( ),
          "hibernate.bytecode.use_reflection_optimizer": "true",
          "hibernate.cglib.use_reflection_optimizer": "true",
          "hibernate.dialect": Databases.getHibernateDialect( ),
          "hibernate.current_session_context_class": "jta",
          "hibernate.jndi.class": "bitronix.tm.jndi.BitronixInitialContextFactory",
          "hibernate.transaction.flush_before_completion":"false",
          "hibernate.transaction.auto_close_session":"false",
          "hibernate.transaction.manager_lookup_class": "com.eucalyptus.empyrean.EmpyreanTransactionManager",
          "hibernate.cache.use_second_level_cache": "true",
          "hibernate.cache.use_query_cache": "false",
          "hibernate.cache.default_cache_concurrency_strategy": "transactional",
          "hibernate.cache.region.factory_class": "com.eucalyptus.bootstrap.CacheRegionFactory",
          "hibernate.cache.infinispan.cfg": "eucalyptus_cache_infinispan.xml",
          "hibernate.cache.use_minimal_puts": "true",
          "hibernate.cache.use_structured_entries": "true",
    ]
    for ( String ctx : PersistenceContexts.list( ) ) {
      Properties p = new Properties( );
      p.putAll( props );
      String ctxUrl = "jdbc:${ServiceUris.remote(Database.class,Internets.loopback( ),ctx)}";
      p.put( "hibernate.connection.url", ctxUrl );
      p.put("hibernate.cache.region_prefix", "eucalyptus_" + ctx + "_cache" );
      Ejb3Configuration config = new Ejb3Configuration( );
      config.setProperties( p );
      for ( Class c : PersistenceContexts.listEntities( ctx ) ) {
        config.addAnnotatedClass( c );
      }
      PersistenceContexts.registerPersistenceContext( ctx, config );
    }
    if( BootstrapArgs.isInitializeSystem( ) ) {
      ServiceBuilder sb = ServiceBuilders.lookup( Eucalyptus.class );
      final ServiceConfiguration newComponent = sb.newInstance( Eucalyptus.INSTANCE.name( ), Internets.localHostAddress( ), Internets.localHostAddress( ), 8773 );
      ServiceConfigurations.store( newComponent );
      LOG.info( "Added registration for this cloud controller: " + newComponent.toString() );
    }
    Databases.getBootstrapper( ).destroy( );
  } catch( Exception ex ) {
    Databases.getBootstrapper( ).destroy( );
    LOG.error( ex, ex );
    System.exit( 123 );
  }
} catch( Exception ex ) {
  Databases.getBootstrapper( ).destroy( );
  LOG.fatal( "", ex );
  System.exit( 37 );
}
