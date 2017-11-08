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

import com.eucalyptus.stats.SensorEntry;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Helper functions for getting the standard collections of sensors
 */
public class Sensors {
    private static final Logger LOG = Logger.getLogger(Sensors.class);
    private static final String COMPONENT_NAME_PREFIX = "euca.components";
    private static final String CONTEXT_SENSOR_NAME = COMPONENT_NAME_PREFIX + ".message_contexts";
    private static final String DB_POOL_SENSOR_NAME = "euca.db.connection_pools";
    private static final String MEMORY_NAME_PREFIX = "euca.jvm.memory";
    private static final String THREAD_SENSOR_NAME = "euca.jvm.threads.state";
    private static final String MEMORY_GENERAL_SENSOR_NAME = MEMORY_NAME_PREFIX + ".general";
    private static final String MEMORY_POOL_SENSOR_NAME = MEMORY_NAME_PREFIX + ".pools";
    private static final String MEMORY_GC_SENSOR_NAME = MEMORY_NAME_PREFIX + ".gc";
    private static final List<String> DEFAULT_MEM_POOL_TAGS = Lists.newArrayList("memory", "jvm");
    private static final List<String> DEFAULT_GC_TAGS = Lists.newArrayList("memory", "jvm", "gc");
    private static final List<String> DEFAULT_MEM_HEAP_TAGS = Lists.newArrayList("memory", "jvm", "heap", "non-heap");
    private static final List<String> DEFAULT_THREAD_TAGS = Lists.newArrayList("threads", "jvm");
    private static final List<String> DEFAULT_DB_TAGS = Lists.newArrayList("db", "connection_count");

    public static String pollingIntervalTag(long intervalSec) {
        return "polling_interval: " + String.valueOf(intervalSec) + " sec";
    }
    /**
     * @param pollingInterval
     * @return
     */
    public static List<SensorEntry> JvmMemorySensors(final long pollingInterval, final long ttl) {
        List<SensorEntry> memorySensors = Lists.newArrayList();
        LOG.info("Building JVM Memory Pool sensors with ttl " + ttl + "sec and polling interval " + pollingInterval + "sec");
        List<String> tags = Lists.newArrayList();
        tags.addAll(DEFAULT_MEM_POOL_TAGS);
        tags.add(pollingIntervalTag(pollingInterval));
        try {
            memorySensors.add(new SensorEntry(BaseStatsSensor.buildSensor(MEMORY_POOL_SENSOR_NAME,
                    "JVM Memory usage for all pools in use",
                    tags,
                    ttl,
                    JvmMemorySensor.POOL),
                    pollingInterval));
        } catch (Exception e) {
            LOG.error("Error loading memory pool sensor.");
            throw Exceptions.toUndeclared(e);
        }

        LOG.info("Building JVM Heap/Non-Heap Memory sensors with ttl " + ttl + "sec and polling interval " + pollingInterval + "sec");
        tags = Lists.newArrayList();
        tags.addAll(DEFAULT_MEM_HEAP_TAGS);
        tags.add(pollingIntervalTag(pollingInterval));
        try {
            memorySensors.add(new SensorEntry(BaseStatsSensor.buildSensor(MEMORY_GENERAL_SENSOR_NAME,
                    "JVM Memory usage for heap/non-heap in use",
                    tags,
                    ttl,
                    JvmMemorySensor.HEAP),
                    pollingInterval));
        } catch (Exception e) {
            LOG.error("Error loading memory pool sensor.");
            throw Exceptions.toUndeclared(e);
        }

        LOG.info("Building JVM GC sensors with ttl " + ttl + "sec and polling interval " + pollingInterval + "sec");
        tags = Lists.newArrayList();
        tags.addAll(DEFAULT_GC_TAGS);
        tags.add(pollingIntervalTag(pollingInterval));
        try {
            memorySensors.add(new SensorEntry(BaseStatsSensor.buildSensor(MEMORY_GC_SENSOR_NAME,
                    "JVM GC stats for all GCs in use",
                    tags,
                    ttl,
                    JvmMemorySensor.GC),
                    pollingInterval));
        } catch (Exception e) {
            LOG.error("Error loading GC sensor.");
            throw Exceptions.toUndeclared(e);
        }

        return memorySensors;
    }


    public static List<SensorEntry> JvmThreadSensors(final long pollingInterval, final long ttl) {
        List<SensorEntry> threadSensors = Lists.newArrayList();
        LOG.info("Building JVM Threading sensors with ttl " + ttl + "sec and polling interval " + pollingInterval + "sec");
        List<String> tags = Lists.newArrayList();
        tags.addAll(DEFAULT_THREAD_TAGS);
        tags.add(pollingIntervalTag(pollingInterval));
        try {
            threadSensors.add(new SensorEntry(BaseStatsSensor.buildSensor(THREAD_SENSOR_NAME,
                    "JVM Memory usage for all pools in use",
                    tags,
                    ttl,
                    JvmThreadSensor.INSTANCE),
                    pollingInterval));
        } catch (Exception e) {
            LOG.error("Error loading threading sensor.");
            throw Exceptions.toUndeclared(e);
        }

        return threadSensors;
    }

    public static List<SensorEntry> DbConnectionPoolSensors(final long pollingInterval, final long ttl) {
        List<SensorEntry> poolSensors = Lists.newArrayList();
        LOG.info("Building Db conneciton pool sensors with ttl " + ttl + "sec and polling interval " + pollingInterval + "sec");
        List<String> tags = Lists.newArrayList();
        tags.addAll(DEFAULT_DB_TAGS);
        tags.add(pollingIntervalTag(pollingInterval));
        try {
            poolSensors.add(new SensorEntry(BaseStatsSensor.buildSensor(DB_POOL_SENSOR_NAME,
                    "Db Connection Pool info for all pools in JVM",
                    tags,
                    ttl,
                    DbPoolSensor.INSTANCE),
                    pollingInterval));
        } catch (Exception e) {
            LOG.error("Error loading db pool sensor.");
            throw Exceptions.toUndeclared(e);
        }

        return poolSensors;
    }

    /**
     * Returns a single sensor the runs check on each local service and emits
     * a system metric for that state.
     *
     * @return
     */
    public static List<SensorEntry> ComponentsSensor(final int pollingInterval, final long ttl) {
        //Single sensor that outputs all component states
        List<SensorEntry> sensors = Lists.newArrayList();
        List<String> tags = Lists.newArrayList();
        tags.add(pollingIntervalTag(pollingInterval));
        try {
            ComponentsSensor sensor = new ComponentsSensor();
            sensor.init(COMPONENT_NAME_PREFIX, "Component state and health status", tags, ttl);
            sensors.add(new SensorEntry(sensor, pollingInterval));
            return sensors;
        } catch (Throwable f) {
            LOG.error("Failed to build service sensors", f);
            throw Exceptions.toUndeclared(f);
        }
    }

    /**
     * Sensor for overall context count
     * @param pollingInterval
     * @param ttl
     * @return
     */
    public static List<SensorEntry> ContextSensor(final int pollingInterval, final long ttl) {
        //Single sensor that outputs all component states
        List<SensorEntry> sensors = Lists.newArrayList();
        List<String> tags = Lists.newArrayList();
        tags.add(pollingIntervalTag(pollingInterval));
        try {
            sensors.add(new SensorEntry(BaseStatsSensor.buildSensor(CONTEXT_SENSOR_NAME,
                    "Count of current message contexts",
                    tags,
                    ttl,
                    ContextsSensor.COUNT),
                    pollingInterval));
        } catch (Exception e) {
            LOG.error("Error loading db pool sensor.");
            throw Exceptions.toUndeclared(e);
        }
        return sensors;
    }
}
