import java.util.concurrent.TimeUnit
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.component.Faults
import com.eucalyptus.component.ServiceConfiguration
import com.eucalyptus.component.Topology
import com.eucalyptus.component.Faults.CheckException
import com.eucalyptus.component.id.Eucalyptus



10.times {
  def t = new Thread().start {
    long startTime = System.currentMillis( );
    long sleepTime = 1000L * Math.random( );
    TimeUnit.MILLISECONDS.sleep( stime );
    ServiceConfiguration conf = Topology.lookup( Eucalyptus.class );
    String msg = "${new Date( startTime )} Slept for ${sleepTime}: hosts=${Databases.ActiveHostSet.HOSTS.get()} dbs=${Databases.ActiveHostSet.ACTIVATED.get( )} volatile=${Databases.isVolatile( )}";
    CheckException fault = Faults.advisory( conf, new Exception( msg ) );
    Faults.persist( fault );
  }
}
