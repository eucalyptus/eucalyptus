/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
/*
 * Groovy script to dump the heap from a running JVM.
 *
 * This script can be run via:
 *
 *  euctl euca=@dumpheap.groovy
 *
 * The heap will be dumped to the (systemd) /tmp directory.
 */
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import com.sun.management.HotSpotDiagnosticMXBean;

Date time = new Date()
String dumpfile = "/tmp/dump-${time.format('yyyyMMddHHmmssSSS')}.hprof"

MBeanServer server = ManagementFactory.getPlatformMBeanServer();
HotSpotDiagnosticMXBean bean = ManagementFactory.newPlatformMXBeanProxy(
    server,
    "com.sun.management:type=HotSpotDiagnostic",
    HotSpotDiagnosticMXBean.class);
bean.dumpHeap( dumpfile, false )
dumpfile
