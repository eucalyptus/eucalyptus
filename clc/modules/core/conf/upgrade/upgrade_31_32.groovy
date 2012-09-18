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

  upgrade_31_32() {
    super(1, ["3.1.0", "3.1.1", "3.1.2"], ["3.2.0"])
  }

  @Override
  void upgrade( final File oldEucaHome,
                final File newEucaHome ) {
    upgradeReporting()
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
        entityManager.createNativeQuery( 'drop table if exists ' + table ).executeUpdate()
      }

      // purge old data from re-used tables
      [ ReportingUser.class, ReportingAccount.class ].each { entityClass ->
        entityManager.createQuery( 'delete from ' + entityClass.getName() ).executeUpdate()
      }
    }

    // create events for existing entities
    ReportingDataVerifier.addMissingReportingEvents()
  }
}