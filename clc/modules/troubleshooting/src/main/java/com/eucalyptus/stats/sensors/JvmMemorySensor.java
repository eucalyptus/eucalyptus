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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Static enums for sensors on JVM Memory
 */
public enum JvmMemorySensor implements Callable<Map<String, Object>> {
    /**
     * Single callable that returns stats for all memory pools in the result map.
     */
    POOL {
        @Override
        public Map<String, Object> call() throws Exception {
            Map<String, Object> result = Maps.newHashMap();
            String poolName;
            //Memory pool sensors
            for (MemoryPoolMXBean managerBean : ManagementFactory.getMemoryPoolMXBeans()) {
                try {
                    poolName = managerBean.getName();
                    if (Strings.isNullOrEmpty(poolName)) {
                        poolName = "unknown";
                    }
                    //Basic usage threshold and counts
                    long used = managerBean.getUsage().getUsed();
                    long max = managerBean.getUsage().getMax();
                    if (managerBean.isUsageThresholdSupported()) {
                        result.put(poolName + ".UsageThresholdBytes", managerBean.getUsageThreshold());
                        result.put(poolName + ".UsageThresholdCount", managerBean.getUsageThresholdCount());
                    }

                    result.put(poolName + ".UsedByes", max);
                    result.put(poolName + ".InitBytes", managerBean.getUsage().getInit());
                    result.put(poolName + ".CommittedBytes", managerBean.getUsage().getCommitted());
                    result.put(poolName + ".MaxBytes", max);
                    result.put(poolName + ".PercentOfMaxUsed", ((double) used / (double) max * 100.0d));

                    if (managerBean.isCollectionUsageThresholdSupported() && managerBean.getCollectionUsage() != null) {
                        result.put(poolName + ".CollectionUsageUsedBytes", managerBean.getCollectionUsage().getUsed());
                        result.put(poolName + ".CollectionUsageInitBytes", managerBean.getCollectionUsage().getInit());
                        result.put(poolName + ".CollectionUsageCommittedBytes", managerBean.getCollectionUsage().getCommitted());
                        result.put(poolName + ".CollectionUsageMaxBytes", managerBean.getCollectionUsage().getMax());
                        result.put(poolName + ".CollectionUsageThresholdBytes", managerBean.getCollectionUsageThreshold());
                        result.put(poolName + ".CollectionUsageThresholdCount", managerBean.getCollectionUsageThresholdCount());
                    }
                    //Peak usage since JVM was started or peak was reset
                    if (managerBean.getPeakUsage() != null) {
                        long peakUsed = managerBean.getPeakUsage().getUsed();
                        result.put(poolName + ".PeakUsedBytes", peakUsed);
                        result.put(poolName + ".PeakCommittedBytes", managerBean.getPeakUsage().getCommitted());
                        result.put(poolName + ".PeakMaxBytes", managerBean.getPeakUsage().getMax());
                        result.put(poolName + ".PercentOfPeakUsed", (double) used / (double) peakUsed * 100.0d);
                    }
                } catch (Throwable f) {
                    LOG.warn("Failed invoking memory bean sensor. Continuing.", f);
                }
            }

            return result;
        }
    },

    /**
     * Returns map of useful values from all GC MX beans
     */
    GC {
        @Override
        public Map<String, Object> call() throws Exception {
            //GC Sensors
            Map<String, Object> result = Maps.newHashMap();
            try {
                List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
                if (null != beans) {
                    for (GarbageCollectorMXBean gcBean : beans) {
                        String name = gcBean.getName();
                        result.put(name + ".CollectionCount", gcBean.getCollectionCount());
                        result.put(name + ".CollectionTimeMSec", gcBean.getCollectionTime());
                    }
                }
            } catch (Throwable f) {
                LOG.warn("Error polling GC bean via mxbean. Continuing.", f);
            }
            return result;
        }
    },

    /**
     * Heap/Non-Heap general usage
     */
    HEAP {
        @Override
        public Map<String, Object> call() throws Exception {
            Map<String, Object> result = Maps.newHashMap();
            try {
                final MemoryMXBean managerBean = ManagementFactory.getMemoryMXBean();
                if (managerBean != null) {
                    result.put("HeapUsageBytes", managerBean.getHeapMemoryUsage().getUsed());
                    result.put("NonHeapUsageBytes", managerBean.getNonHeapMemoryUsage().getUsed());
                }
            } catch (Throwable f) {
                LOG.warn("Failed reading heap/non-heap memory usage via mxbean. Continuing.", f);
            }
            return result;
        }
    };

    private static final Logger LOG = Logger.getLogger(JvmMemorySensor.class);
}
