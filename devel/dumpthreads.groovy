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
 * Groovy script to dump thread stack traces from a running JVM.
 *
 * This script can be run via:
 *
 *  euctl euca=@dumpthreads.groovy
 *
 * The stack traces will be written to the (systemd) /tmp directory.
 */
import static com.eucalyptus.crypto.util.Timestamps.formatIso8601Timestamp
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.lang.management.ThreadMXBean
import java.nio.charset.StandardCharsets

Date time = new Date()
String dumpfile = "/tmp/dump-${time.format('yyyyMMddHHmmssSSS')}.log"
ThreadMXBean mxBean = ManagementFactory.threadMXBean
ThreadInfo[] threadInfos = mxBean.getThreadInfo( mxBean.allThreadIds, 0)
Map<Long, ThreadInfo> threadInfoMap = [:]
threadInfos.each { threadInfo ->
  threadInfoMap.put( threadInfo.threadId, threadInfo )
}
new File( dumpfile ).withWriter( 'UTF-8' ) { writer ->
  Map<Thread, StackTraceElement[]> stacks = Thread.allStackTraces
  writer.write( "Thread dump generated ${formatIso8601Timestamp(time)}\n\n" )
  stacks.entrySet( ).each { entry ->
    Thread thread = entry.key
    StackTraceElement[] elements = entry.value
    thread.with {
      writer.write("\"${name}\" prio=${priority} tid=${id} ")
      writer.write("${state} ${daemon?'deamon':'worker'}\n")
    }
    threadInfoMap.get( thread.id )?.with{
      writer.write("    native=${inNative}, suspended=${suspended}, " )
      writer.write(    "block=${blockedCount}, wait=${waitedCount}\n" )
      writer.write("    lock=${lockName} owner ${lockOwnerName} (${lockOwnerId}), ")
      writer.write(    "cpu=${mxBean.getThreadCpuTime(threadId) / 1000000L}, ")
      writer.write(    "user=${mxBean.getThreadUserTime(threadId) / 1000000L}\n" )
    }
    elements.each { element ->
      writer.write( '        ' )
      writer.write( element.toString( ) )
      writer.write( '\n' )
    }
    writer.write( '\n' )
  }
}
dumpfile
