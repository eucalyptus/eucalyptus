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

import com.eucalyptus.stats.sensors.EucalyptusStatsSensor;

import javax.management.MalformedObjectNameException;

/**
 * An entry in the sensor directory that specifies what and when to execute.
 */
public class SensorEntry {
    private EucalyptusStatsSensor sensor;
    private long queryInterval;

    public SensorEntry(EucalyptusStatsSensor s, long interval) throws MalformedObjectNameException {
        this.queryInterval = interval;
        this.sensor = s;
    }

    public long getQueryInterval() {
        return this.queryInterval;
    }

    public EucalyptusStatsSensor getSensor() {
        return this.sensor;
    }
}
