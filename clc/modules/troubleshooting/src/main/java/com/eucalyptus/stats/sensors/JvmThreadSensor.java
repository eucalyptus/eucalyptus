/*
 * Copyright 2009-$year Eucalyptus Systems, Inc.
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
 */

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
