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
