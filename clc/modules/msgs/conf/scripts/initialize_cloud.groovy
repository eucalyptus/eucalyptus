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
          "hibernate.hbm2ddl.auto": "update",
          "hibernate.generate_statistics": "true",
          "hibernate.connection.driver_class": Databases.getDriverName( ),
          "hibernate.connection.username": "eucalyptus",
          "hibernate.connection.password": Databases.getPassword( ),
          "hibernate.bytecode.use_reflection_optimizer": "true",
          "hibernate.cglib.use_reflection_optimizer": "true",
          "hibernate.dialect": Databases.getHibernateDialect( ),
          "hibernate.cache.provider_class": "org.hibernate.cache.TreeCache",
          "hibernate.cache.region.factory_class": "org.hibernate.cache.jbc2.SharedJBossCacheRegionFactory",
          "hibernate.cache.region.jbc2.cfg.shared": "eucalyptus_jboss_cache.xml",
          "hibernate.cache.use_second_level_cache": "true",
          "hibernate.cache.use_query_cache": "true",
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
  LOG.error( ex, ex );
}
