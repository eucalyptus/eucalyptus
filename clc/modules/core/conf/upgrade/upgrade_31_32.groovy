import com.eucalyptus.upgrade.AbstractUpgradeScript
import com.eucalyptus.upgrade.StandalonePersistence
import com.eucalyptus.entities.PersistenceContexts
import org.hibernate.ejb.Ejb3Configuration
import com.eucalyptus.bootstrap.ServiceJarDiscovery
import com.eucalyptus.entities.PersistenceContextDiscovery
import org.logicalcobwebs.proxool.configuration.PropertyConfigurator
import com.eucalyptus.bootstrap.SystemIds
import com.eucalyptus.reporting.domain.ReportingUser
import javax.persistence.EntityManagerFactory
import com.eucalyptus.system.Ats
import javax.persistence.PersistenceContext
import javax.persistence.EntityManager
import javax.persistence.EntityTransaction
import com.eucalyptus.reporting.domain.ReportingAccount
import com.eucalyptus.reporting.ReportingDataVerifier

class upgrade_31_32 extends AbstractUpgradeScript {
  private boolean PG_USE_SSL = Boolean.valueOf( System.getProperty("euca.db.ssl", "true") )
  private Map<String,String> hibernateProperties = [
      // Hibernate basic properties
      'hibernate.show_sql': 'false',
      'hibernate.format_sql': 'false',
      'hibernate.connection.autocommit': 'false',
      'hibernate.connection.release_mode': 'after_statement',
      'hibernate.hbm2ddl.auto': 'update',
      'hibernate.generate_statistics': 'false',
      'hibernate.bytecode.use_reflection_optimizer': 'true',
      'hibernate.cglib.use_reflection_optimizer': 'true',
      'hibernate.dialect': 'org.hibernate.dialect.PostgreSQLDialect',

      // Hibernate connection pool properties
      'hibernate.connection.provider_class': 'org.hibernate.connection.ProxoolConnectionProvider',
      'hibernate.proxool.existing_pool': 'true',
      'hibernate.proxool.pool_alias': '%s',

      // Proxool properties
      'jdbc-%s.user': 'eucalyptus',
      'jdbc-%s.password': 'PASSWORD HERE',
      'jdbc-%s.proxool.driver-url': 'jdbc:postgresql://localhost:8777/%s/' + (PG_USE_SSL ? '?ssl=true&sslfactory=com.eucalyptus.postgresql.PostgreSQLSSLSocketFactory' : ''),
      'jdbc-%s.proxool.alias': '%s',
      'jdbc-%s.proxool.driver-class': 'org.postgresql.Driver',
      'jdbc-%s.proxool.minimum-connection-count': '4',
      'jdbc-%s.proxool.maximum-connection-count': '16',
      'jdbc-%s.proxool.simultaneous-build-throttle': '4',
      'jdbc-%s.proxool.prototype-count': '2',
      'jdbc-%s.proxool.house-keeping-test-sql': 'SELECT 1=1',
      'jdbc-%s.proxool.house-keeping-sleep-time': '5000',
      'jdbc-%s.proxool.test-before-use': 'true',
      'jdbc-%s.proxool.test-after-use': 'false',
      'jdbc-%s.proxool.proxool.trace': 'false',
  ]

  upgrade_31_32() {
    super(1, ["3.1.0", "3.1.1", "3.1.2"], ["3.2.0"])
  }

  @Override
  void upgrade( final File oldEucaHome,
                final File newEucaHome ) {
    setupPersistenceContexts()

    upgradeReporting()
  }

  private void setupPersistenceContexts() {
    ServiceJarDiscovery.runDiscovery( new PersistenceContextDiscovery() )

    PersistenceContexts.list( ).each { String contextName ->
      Properties properties = new Properties()
      hibernateProperties.entrySet().each { entry ->
        String key = String.format( entry.key, contextName )
        String value = String.format( entry.key, contextName )
        if ( value.equals('PASSWORD HERE') ) {
          value = SystemIds.databasePassword();
        }
        properties.setProperty( key, value )
      }

      // proxool
      PropertyConfigurator.configure( properties )

      // hibernate
      Ejb3Configuration config = new Ejb3Configuration();
      config.setProperties( properties );

      PersistenceContexts.listEntities( contextName ).each{ entity ->
        config.addAnnotatedClass( entity )
      }

      PersistenceContexts.registerPersistenceContext( contextName, config )
    }
  }

  private void transactionalForContext( final Object exampleEntity, final Closure callback ) {
    String context = Ats.inClassHierarchy( exampleEntity ).get( PersistenceContext.class ).name()
    EntityManagerFactory entityManagerFactory = PersistenceContexts.getEntityManagerFactory( context );
    EntityManager entityManager = entityManagerFactory.createEntityManager( );
    EntityTransaction transaction = entityManager.getTransaction( );
    transaction.begin()
    boolean success = false
    try {
      callback.call( entityManager )
      success = true
    } finally {
      if ( success && !transaction.rollbackOnly ) {
        transaction.commit()
      } else {
        transaction.rollback()
      }
    }
  }

  private void upgradeReporting() {
    transactionalForContext( ReportingUser.class ) {
      EntityManager entityManager ->
      // delete old tables
      [ 'storage_usage_snapshot', 'instance_usage_snapshot', 's3_usage_snapshot', 'reporting_instance' ].each { table ->
        entityManager.createNativeQuery( 'drop table ' + table ).executeUpdate()
      }

      // purge old data from re-used tables
      [ ReportingUser.class, ReportingAccount.class ].each { entityClass ->
        entityManager.createQuery( 'delete from ' + entityClass.getName() ).executeUpdate()
      }
    }

    // create events for existing entities
    ReportingDataVerifier.closeGap()
  }
}