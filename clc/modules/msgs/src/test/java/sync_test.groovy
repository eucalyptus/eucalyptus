import java.util.concurrent.TimeUnit
import javax.persistence.EntityTransaction
import org.apache.log4j.Logger
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.component.Faults
import com.eucalyptus.component.ServiceConfiguration
import com.eucalyptus.component.Topology
import com.eucalyptus.component.Faults.CheckException
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.entities.Entities


Logger LOG = Logger.getLogger( "hi" );
10.times {
  def t = new Thread().start {
    long startTime = System.currentMillis( );
    long sleepTime = 1000L * Math.random( );
    TimeUnit.MILLISECONDS.sleep( stime );
    ServiceConfiguration conf = Topology.lookup( Eucalyptus.class );
    String msg = "${new Date( startTime )} Slept for ${sleepTime}: hosts=${Databases.ActiveHostSet.HOSTS.get()} dbs=${Databases.ActiveHostSet.ACTIVATED.get( )} volatile=${Databases.isVolatile( )}";
    CheckException fault = Faults.advisory( conf, new Exception( msg ) );
    final EntityTransaction db = Entities.get( CheckException.class );
    try {
      Entities.persist( fault );
      db.commit( );
    } catch ( final Exception ex ) {
      LOG.error( "Failed to persist error information for: " + fault, ex );
      db.rollback( );
    }
  }
}
