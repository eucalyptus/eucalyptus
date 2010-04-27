import com.eucalyptus.entities.PersistenceContexts;
import org.hibernate.ejb.*
import com.eucalyptus.util.*
import edu.ucsb.eucalyptus.cloud.ws.*;
hiber_config = [
  'hibernate.archive.autodetection': 'jar, class, hbm',
  'hibernate.show_sql': 'false',
  'hibernate.format_sql': 'false',
  'hibernate.connection.autocommit': 'true',
  'hibernate.hbm2ddl.auto': 'update',
  'hibernate.generate_statistics': 'true',
]
contexts = PersistenceContexts.list( );
contexts.each { String ctxName ->
  String it = ctxName.replaceAll("eucalyptus_","")
  pool_config = new pools(new Binding([context_name:it])).run()
  cache_config  = new caches(new Binding([context_name:it])).run()
  config = new Ejb3Configuration();
  LogUtil.logHeader( "Hibernate for ${ctxName}" ).log(hiber_config.inspect())
  hiber_config.each { k, v -> config.setProperty(k, v) }
  LogUtil.logHeader( "Pool for ${ctxName}").log( pool_config.inspect() )
  pool_config.each { k, v -> config.setProperty(k, v) }
  LogUtil.logHeader( "Cache for ${ctxName}").log( cache_config )
  cache_config.each { k, v -> config.setProperty(k, v) }
  entity_list = PersistenceContexts.listEntities( ctxName )
  LogUtil.logHeader("Entities for ${ctxName}")
  entity_list.each{ ent ->
    LogUtil.log( ent.toString() )
    config.addAnnotatedClass( ent )
  }
  try {
    PersistenceContexts.registerPersistenceContext("${ctxName}", config)
  } catch( Throwable t ) {
    t.printStackTrace();
    System.exit(1)
  }
}


