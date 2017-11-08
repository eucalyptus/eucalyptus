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
