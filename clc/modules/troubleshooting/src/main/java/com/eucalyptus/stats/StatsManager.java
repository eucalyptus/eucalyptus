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

package com.eucalyptus.stats;

import com.eucalyptus.bootstrap.OrderedShutdown;
import com.eucalyptus.stats.configuration.StatsConfiguration;
import com.eucalyptus.stats.sensors.SensorManagers;
import org.apache.log4j.Logger;

/**
 * Primary interface for other components to interact with the monitoring subsystem
 * and manage its lifecycle.
 * <p/>
 * NOTE: this is for monitoring of the eucalyptus processes themselves, NOT CloudWatch.
 */
public class StatsManager {
    private static final Logger LOG = Logger.getLogger(StatsManager.class);

    @SuppressWarnings("unchecked")
    public static void init() throws Exception {
        LOG.info("Initializing monitoring system");

        if (!StatsConfiguration.isStatsReportingEnabled()) {
            LOG.warn("Skipping monitoring configuration because it is explicitly disabled");
            return;
        }

        try {
            EventEmitterService.getInstance().init();
            SensorManagers.getInstance().init(EventEmitterService.getInstance());
            OrderedShutdown.registerPreShutdownHook(monitoringShutdownHook);
        } catch (Exception e) {
            LOG.error("Error initializing monitoring service.", e);
            throw new RuntimeException(e);
        }
    }

    protected static final Runnable monitoringShutdownHook = new Runnable() {
        @Override
        public void run() {
            int retryCount = 3;
            for (int i = 0; i < retryCount; i++) {
                try {
                    StatsManager.stop();
                    break;
                } catch (Throwable f) {
                    LOG.warn("Error shutting down monitoring system", f);
                }
            }
        }
    };

    public static SensorManager getSensorManager() {
        return SensorManagers.getInstance();
    }

    public static void start() throws Exception {
        if (!StatsConfiguration.isStatsReportingEnabled()) {
            LOG.warn("Skipping monitoring startup because it is explicitly disabled");
        } else {
            LOG.info("Starting monitoring system");
            EventEmitterService.getInstance().start();
            SensorManagers.getInstance().start();
        }
    }

    public static void stop() throws Exception {
        SensorManagers.getInstance().stop();
        EventEmitterService.getInstance().start();
    }

    /**
     * Verifies that the stats system is ready and able to operate.
     * Does not guarantee that events will be delivered, just that configuration
     * meets the prerequisites to start the service
     * @throws Exception
     */
    public static void check() throws Exception {
        try {
            SensorManagers.getInstance().check();
            EventEmitterService.getInstance().check();
        } catch(Exception e) {
            LOG.error("Stats subsystem failed check() call");
            throw e;
        }
    }
}
