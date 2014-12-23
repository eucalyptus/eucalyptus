/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.stats.configuration;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.Configurables;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.stats.emitters.EventEmitter;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.WebServices;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;

import java.lang.annotation.Annotation;

/**
 * Basic config for sensor system. Must be host-local.
 * Overriding values is done via system properties (e.g. CLOUD_OPTS in eucalyptus.conf)
 */
@ConfigurableClass(root = "stats", description = "Configuration values for eucalyptus status and statistics on system processes", deferred = true)
public class StatsConfiguration {
    private static final Logger LOG = Logger.getLogger(StatsConfiguration.class);
    public static SubDirectory sensorCodeDirectory = SubDirectory.SCRIPTS; //Place to scan for groovy files to ingress sensors

    private static final String EMITTER_PROPERTY_NAME = "euca.stats.emitter";
    private static final String EMITTER_PROPERTY_DEFAULT = "com.eucalyptus.stats.emitters.FileSystemEmitter";
    private static final String CONFIG_SCRIPT_PROPERTY_NAME = "euca.stats.sensor_config_file";
    private static final String CONFIG_SCRIPT_PROPERTY_DEFAULT = "stats_sensors.groovy";
    private static final String THREAD_POOL_SIZE_PROPERTY_NAME = "euca.stats.sensor_thread_pool_size";
    private static final String THREAD_POOL_SIZE_PROPERTY_DEFAULT = "2";
    private static final String CONFIG_SCRIPT_CHECK_PROPERTY_NAME = "euca.stats.sensor_config_file_check_interval";
    private static final String CONFIG_SCRIPT_CHECK_PROPERTY_DEFAULT = "60";
    private static final String ENABLE_STATS_PROPERTY_NAME = "euca.enable_stats";
    private static final String ENABLE_STATS_PROPERTY_DEFAULT = "false"; //Set to "true" when feature should be on by default

    @ConfigurableField(displayName = "enable_stats", description = "Enable Eucalyptus internal monitoring stats", initial = "false", changeListener = PropertyChangeListeners.IsBoolean.class)
    public static Boolean enable_stats = Boolean.valueOf(ENABLE_STATS_PROPERTY_DEFAULT);

    @ConfigurableField(displayName = "event_emitter", description = "Internal stats emitter FQ classname used to send metrics to monitoring system", initial = "com.eucalyptus.stats.emitters.FileSystemEmitter")
    public static String event_emitter = EMITTER_PROPERTY_DEFAULT;

    @ConfigurableField(displayName = "config_update_check_interval_seconds", description = "Interval, in seconds, at which the sensor configuration is checked for changes", initial = "60", changeListener = PropertyChangeListeners.IsPositiveInteger.class)
    public static String config_update_check_interval_seconds = CONFIG_SCRIPT_CHECK_PROPERTY_DEFAULT;

    public static String getSensorConfigScript() {
        return System.getProperty(CONFIG_SCRIPT_PROPERTY_NAME, CONFIG_SCRIPT_PROPERTY_DEFAULT);
    }

    public static Integer getMonitoringThreadPoolSize() {
        return Integer.valueOf(System.getProperty(THREAD_POOL_SIZE_PROPERTY_NAME, THREAD_POOL_SIZE_PROPERTY_DEFAULT));
    }

    public static Boolean isStatsReportingEnabled() {
        String s = System.getProperty(ENABLE_STATS_PROPERTY_NAME, enable_stats.toString());
        if(Strings.isNullOrEmpty(s)) {
            LOG.error("Found empty or null config for enable_stats");
            return false;
        }
        return Boolean.valueOf(s);
    }

    public static Class getEmitterClass() throws ClassNotFoundException, IllegalArgumentException {
        //Always use local explicit if present, default is the global value
        Class candidate = Class.forName(System.getProperty(EMITTER_PROPERTY_NAME, event_emitter));
        if(!EventEmitter.class.isAssignableFrom(candidate)) {
            throw new IllegalArgumentException("Specified event emitter class " + candidate.getName() + " is not a valid event emitter");
        }
        return candidate;
    }

    public static int getConfigCheckInterval() throws Exception {
        String s = System.getProperty(CONFIG_SCRIPT_CHECK_PROPERTY_NAME, config_update_check_interval_seconds);
        if (Strings.isNullOrEmpty(s)) {
            LOG.error("Found empty or null config for enable_stats");
            throw Exceptions.toUndeclared("Unable to find any config value for the sensor configuration check interval");
        }
        return Integer.valueOf(s);
    }
}
