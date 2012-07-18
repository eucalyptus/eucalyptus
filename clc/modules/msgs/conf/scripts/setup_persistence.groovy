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

import org.apache.log4j.Logger
import org.hibernate.ejb.Ejb3Configuration
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.entities.PersistenceContexts

Logger LOG = Logger.getLogger( "com.eucalyptus.scripts.setup_persistence" );

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
