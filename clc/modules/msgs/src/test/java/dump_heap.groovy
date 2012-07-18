/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
