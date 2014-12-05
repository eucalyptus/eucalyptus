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

import com.eucalyptus.stats.StatsOutputValues;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.logicalcobwebs.proxool.ProxoolFacade;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Check for db pools
 */
public enum DbPoolSensor implements Callable<Map<String, Object>> {
    INSTANCE {
        public Map<String, Object> call() {
            Map<String, Object> results = Maps.newTreeMap();
            try {
                long max = 0;
                long active = 0;
                for (String alias : ProxoolFacade.getAliases()) {
                    max = ProxoolFacade.getConnectionPoolDefinition(alias).getMaximumConnectionCount();
                    active = ProxoolFacade.getSnapshot(alias, true).getActiveConnectionCount();
                    results.put(alias + ".MaxConnectionCount", max);
                    results.put(alias + ".ActiveConnectionCount", active);
                    results.put(alias + ".CapacityCheck", active < max ? StatsOutputValues.CHECK_OK : StatsOutputValues.CHECK_FAILED);
                }
            } catch (Throwable f) {
                LOG.warn("Error getting proxool db connection information. Continuing.", f);
            }
            return results;
        }
    };
    private static final Logger LOG = Logger.getLogger(DbPoolSensor.class);
}
