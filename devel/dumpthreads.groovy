/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
