/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

import com.eucalyptus.component.id.Database
import com.eucalyptus.entities.PersistenceContextConfiguration
import com.eucalyptus.system.Threads
import org.apache.log4j.Logger
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.bootstrap.SystemIds
import com.eucalyptus.entities.PersistenceContexts

import java.util.concurrent.Callable

Logger LOG = Logger.getLogger( 'com.eucalyptus.scripts.setup_persistence' );

def default_hiber_config = [
      'hibernate.ejb.interceptor.session_scoped'          : 'com.eucalyptus.entities.DelegatingInterceptor',
      'hibernate.show_sql'                                : 'false',
      'hibernate.format_sql'                              : 'false',
      'hibernate.connection.autocommit'                   : 'false',
      'hibernate.connection.release_mode'                 : 'after_transaction',
      'hibernate.generate_statistics'                     : 'false',
      'hibernate.bytecode.use_reflection_optimizer'       : 'true',
      'hibernate.default_batch_fetch_size'                : '50',
      'hibernate.discriminator.ignore_explicit_for_joined': 'true', // HHH-6911
]

// Set system properties
System.setProperty('com.eucalyptus.cache.cluster', SystemIds.cacheName())

PersistenceContexts.list( ).collect{ String context_name ->
  Threads.<Void>enqueue( Database, PersistenceContexts, {
    // Configure the hibernate connection
    def hibernate_config = [:]
    hibernate_config.putAll((Map<?, ?>) default_hiber_config)
    hibernate_config.putAll([
        /** jdbc driver **/
        'hibernate.dialect'                            : Databases.getHibernateDialect(),
        /** db pools **/
        'hibernate.connection.provider_class'          : 'org.hibernate.proxool.internal.ProxoolConnectionProvider',
        'hibernate.proxool.pool_alias'                 : PersistenceContexts.toDatabaseName().apply(context_name),
        'hibernate.proxool.existing_pool'              : 'true',
        /** transactions **/
        'hibernate.transaction.auto_close_session'     : 'false',
        'hibernate.transaction.flush_before_completion': 'false',
        /** l2 cache **/
        'hibernate.cache.use_second_level_cache'       : 'false',
        'hibernate.cache.use_query_cache'              : 'false',
    ])

    LOG.info("Context name: = '" + context_name + "'");
    if (context_name in ["eucalyptus_cloudwatch", "eucalyptus_cloudwatch_backend", "eucalyptus_cloud"]) {
      LOG.info("Using batching for context ${context_name}");
      hibernate_config.putAll([
          /** batch **/
          'hibernate.jdbc.batch_size'          : '50',
          'hibernate.order_inserts'            : 'true',
          'hibernate.order_updates'            : 'true',
          'hibernate.jdbc.batch_versioned_data': 'true'
      ])
    } else {
      LOG.debug("Not using batching for context ${context_name}");
    }

    String schemaName = PersistenceContexts.toSchemaName().apply(context_name)
    if (schemaName) {
      hibernate_config.put( 'hibernate.default_schema', schemaName )
    }

    // Register the properties with the config
    PersistenceContextConfiguration config = new PersistenceContextConfiguration(
        context_name,
        PersistenceContexts.listEntities(context_name),
        hibernate_config
    );

    // Log the system-discovered entity list
    LOG.info("${context_name} Registered entities:           " +
        config.entityClasses.collect { Class<?> ent ->
          ent.simpleName
        }
    )

    // Register the context
    try {
      PersistenceContexts.registerPersistenceContext(config)
    } catch (Exception t) {
      LOG.error( "Error registering persistence context ${context_name}", t )
    }

    void
  } as Callable<Void> )
}*.get( )
