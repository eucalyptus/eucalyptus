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


import com.eucalyptus.upgrade.Upgrades
import org.apache.log4j.Logger
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
import com.eucalyptus.component.id.Database
import com.eucalyptus.entities.PersistenceContextConfiguration
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
    Map<String,String> props = [
          "hibernate.show_sql": "false",
          "hibernate.format_sql": "false",
          "hibernate.connection.autocommit": "true",
          "hibernate.hbm2ddl.auto": "create",
          "hibernate.generate_statistics": "false",
          "hibernate.connection.driver_class": Databases.getDriverName( ),
          "hibernate.connection.username": Databases.getUserName( ),
          "hibernate.connection.password": Databases.getPassword( ),
          "hibernate.bytecode.use_reflection_optimizer": "true",
          "hibernate.cglib.use_reflection_optimizer": "true",
          "hibernate.dialect": Databases.getHibernateDialect( ),
          "hibernate.transaction.auto_close_session":"false",
          "hibernate.transaction.flush_before_completion":"false",
          "hibernate.cache.use_second_level_cache": "false",
          "hibernate.cache.use_query_cache": "false",
          "hibernate.discriminator.ignore_explicit_for_joined": "true", // HHH-6911
    ]
    for ( String ctx : PersistenceContexts.list( ) ) {
      final String databaseName = PersistenceContexts.toDatabaseName( ).apply( ctx )
      final String schemaName = PersistenceContexts.toSchemaName( ).apply( ctx )
      final String ctxUrl = "jdbc:${ServiceUris.remote(Database.class, InetAddress.getByName('127.0.0.1'), databaseName)}";
      props.put( "hibernate.connection.url", ctxUrl );
      if ( schemaName != null ) props.put( 'hibernate.default_schema', schemaName )
      final PersistenceContextConfiguration config = new PersistenceContextConfiguration(
          ctx,
          PersistenceContexts.listEntities( ctx ),
          PersistenceContexts.listAuxiliaryDatabaseObjects( ctx ),
          props
      );
      PersistenceContexts.registerPersistenceContext( config );
    }
    if( BootstrapArgs.isInitializeSystem( ) ) {
      Upgrades.init( )
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
