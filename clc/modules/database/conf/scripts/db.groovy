import org.hibernate.ejb.*
import com.eucalyptus.util.*

hiber_config = [
  'hibernate.dialect': 'org.hibernate.dialect.HSQLDialect',
  'hibernate.archive.autodetection': 'jar, class, hbm',
  'hibernate.show_sql': 'false',
  'hibernate.format_sql': 'false',
  'hibernate.connection.autocommit': 'true',
  'hibernate.hbm2ddl.auto': 'update',
  'hibernate.cache.use_structured_entries': 'true',
  'hibernate.generate_statistics': 'true',
]
contexts = ['general','images','auth','config','walrus','storage','dns']
contexts.each {  
  pool_config = new pools(new Binding([context_name:it])).run()
  cache_config  = new caches(new Binding([context_name:it])).run()
  config = new Ejb3Configuration();
  hiber_config.each { k, v -> config.setProperty(k, v) }
  pool_config.each { k, v -> config.setProperty(k, v) }
  cache_config.each { k, v -> config.setProperty(k, v) }
  entity_list = new entities(new Binding([context_name:it])).run()
  LogUtil.dumpObject(entity_list)
  entity_list.each{ 
    config.addAnnotatedClass( it )
  }
  try {
    DatabaseUtil.registerPersistenceContext("eucalyptus_${it}", config)
  } catch( Throwable t ) {
    t.printStackTrace();
    System.exit(1)
  }
}
