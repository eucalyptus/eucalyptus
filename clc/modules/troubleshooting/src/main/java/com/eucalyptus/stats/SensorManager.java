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

package com.eucalyptus.stats;

import java.util.List;

/**
 * Interface for the directory and updates for sensor metrics
 * The SensorManager tracks registered sensors and is responsible
 * for executing them on their specified schedule.
 */
public interface SensorManager {
    /**
     * Is the manager ready and can it function
     */
    public void check();

    /**
     * Force update of all metrics synchronously. May interfere with
     * other running updates, so sensors should be thread-safe.
     * <p/>
     * Results are emitted in the normal fasion via the emitter service
     *
     * @throws Exception
     */
    public void pollAll();

    /**
     * Get metrics for each registered sensor without emitting them
     *
     * @return list of resulting metrics
     * @throws Exception
     */
    public List<SystemMetric> getMetrics();

    public void init(EventEmitterService emitter);

    public EventEmitterService getEventEmitterService();

    public void start();

    public void stop();
}
