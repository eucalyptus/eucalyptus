import java.lang.management.ManagementFactory
import javax.management.JMX
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import com.eucalyptus.util.Exceptions
import com.sun.management.HotSpotDiagnosticMXBean

String url = "service:jmx:rmi:///jndi/rmi://127.0.0.1:1099/eucalyptus";
JMXServiceURL jmxURL = new JMXServiceURL(url);
JMXConnector connector = JMXConnectorFactory.connect(jmxURL);
ObjectName name = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
String fname = "/tmp/${System.currentTimeMillis()}.hprof";
MBeanServer server = ManagementFactory.getPlatformMBeanServer( );
try {
  f = new File( fname );
  JMX.newMBeanProxy( server, name, HotSpotDiagnosticMXBean.class ).dumpHeap( fname, false );
  return "SUCCESS: Dumped heap to: ${fname} ${f.length()/(1024.0d*1024.0d)}MB";
} catch ( Exception ex ) {
  return Exceptions.string( ex );
}
