import com.eucalyptus.entities.PersistenceContexts;
import org.hibernate.ejb.*
import com.eucalyptus.util.*
import edu.ucsb.eucalyptus.cloud.ws.*;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.component.Components;
import org.logicalcobwebs.proxool.ProxoolFacade;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.system.SubDirectory;
import org.apache.log4j.Logger;

Logger LOG = Logger.getLogger( "after_database" );

//Base config for hibernate and proxool
String dbDriver = 'org.hsqldb.jdbcDriver';
String db_pass = Hmacs.generateSystemSignature( );
String db_url = "jdbc:hsqldb:" + Components.lookup("db").getUri( ).toASCIIString( );
ClassLoader.getSystemClassLoader().loadClass('org.logicalcobwebs.proxool.ProxoolDriver');
default_hiber_config = [
  'hibernate.archive.autodetection': 'jar, class, hbm',
  'hibernate.show_sql': 'false',
  'hibernate.format_sql': 'false',
  'hibernate.connection.autocommit': 'true',
  'hibernate.hbm2ddl.auto': 'update',
  'hibernate.generate_statistics': 'true',
]
default_pool_props = [
    'proxool.simultaneous-build-throttle': '16',
    'proxool.minimum-connection-count': '8',
    'proxool.maximum-connection-count': '256',
    'proxool.house-keeping-test-sql': 'SELECT 1=1;',
    'proxool.house-keeping-sleep-time': '300000',
    'user': 'sa',
    'password': db_pass,
]

// Build each persistence context
PersistenceContexts.list( ).each { String ctxName ->
  String context_name = ctxName.replaceAll("eucalyptus_","")
  LogUtil.logHeader( "Setting up persistence context: ${ctxName} (log level TRACE for details)" )
  
  // Setup proxool
  proxool_config = new Properties();
  proxool_config.putAll(default_pool_props)
  String url = "proxool.eucalyptus_${context_name}:${dbDriver}:${db_url}_${context_name}";
  LOG.info( "${ctxName} Preparing connection pool:     ${url}" )
  
  // Register proxool
  LOG.trace( proxool_config )
  ProxoolFacade.registerConnectionPool(url, proxool_config);
  LOG.info( "${ctxName} Registered connection pool:    done." )
  
  // Configure the hibernate connection
  hibernate_config = [:]
  hibernate_config.putAll(default_hiber_config)
  hibernate_config.putAll( [
    'hibernate.bytecode.use_reflection_optimizer': 'true',
    'hibernate.cglib.use_reflection_optimizer': 'true',
    'hibernate.dialect': 'org.hibernate.dialect.HSQLDialect',
    'hibernate.connection.provider_class': 'org.hibernate.connection.ProxoolConnectionProvider',
    'hibernate.proxool.pool_alias': "eucalyptus_${context_name}",
    'hibernate.proxool.existing_pool': 'true',
    'hibernate.cache.provider_class': 'net.sf.ehcache.hibernate.SingletonEhCacheProvider',
    'hibernate.cache.region_prefix': "eucalyptus_${context_name}_cache",
    'hibernate.cache.use_second_level_cache': 'true',
    'hibernate.cache.use_query_cache': 'true',
    'hibernate.cache.use_structured_entries': 'true',
  ] )

  // Register the properties with the config
  config = new Ejb3Configuration();
  LOG.info( "${ctxName} Setting up persistence:        done." )
  hibernate_config.each { k , v ->
    LOG.trace( "${k} = ${v}" )
    config.setProperty( k, v )
  }
  
  // Register the system-discovered entity list
  LOG.info( "${ctxName} Registered entities:           " +
    PersistenceContexts.listEntities( ctxName ).collect{ ent ->
      config.addAnnotatedClass( ent )
      ent.getSimpleName()
    }
  )
  
  // Register the context
  try {
    PersistenceContexts.registerPersistenceContext("${ctxName}", config)
  } catch( Throwable t ) {
    t.printStackTrace();
    System.exit(1)
  }
}
