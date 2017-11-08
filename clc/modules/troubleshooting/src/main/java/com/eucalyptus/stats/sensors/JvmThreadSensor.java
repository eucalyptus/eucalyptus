/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.stats.sensors;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Static enums for sensors on JVM Memory
 */
public enum JvmThreadSensor implements Callable<Map<String, Object>> {

    INSTANCE {
        @Override
        public Map<String, Object> call() throws Exception {
            Map<String, Object> result = Maps.newHashMap();
            try {
                final ThreadMXBean managerBean = ManagementFactory.getThreadMXBean();
                if (managerBean != null) {
                    long[] lockedThreadList = null;
                    long[] monLockedThreadList = null;

                    if (managerBean.isObjectMonitorUsageSupported()) {
                        if (managerBean.isSynchronizerUsageSupported()) {
                            //monitor and owned-synchronizer detection
                            lockedThreadList = managerBean.findDeadlockedThreads();
                        } else {
                            //monitor-only detection
                            monLockedThreadList = managerBean.findMonitorDeadlockedThreads();

                        }
                    }
                    result.put("DeadlockedThreadCount", lockedThreadList == null ? "" : lockedThreadList.length);
                    result.put("DeadlockedThreads", lockedThreadList == null ? "" : Joiner.on(',').join(Lists.newArrayList(lockedThreadList)));
                    result.put("MonitorDeadlockedThreadCount", monLockedThreadList == null ? "" : monLockedThreadList.length);
                    result.put("MonitorDeadlockedThreadCount", monLockedThreadList == null ? "" : Joiner.on(',').join(Lists.newArrayList(lockedThreadList)));

                    result.put("ThreadCount", managerBean.getThreadCount());
                    result.put("TotalStartedThreadCount", managerBean.getTotalStartedThreadCount());
                    result.put("PeakThreadCount", managerBean.getPeakThreadCount());
                    result.put("DaemonThreadCount", managerBean.getDaemonThreadCount());
                }
            } catch (Throwable f) {
                LOG.warn("Failed reading heap/non-heap memory usage via mxbean. Continuing.", f);
            }
            return result;
        }
    };

    private static final Logger LOG = Logger.getLogger(JvmThreadSensor.class);
}
