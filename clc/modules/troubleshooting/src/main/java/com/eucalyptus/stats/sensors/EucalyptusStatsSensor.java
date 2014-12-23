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

package com.eucalyptus.stats.sensors;

import com.eucalyptus.stats.SystemMetric;

import java.util.List;

/**
 * All sensors must implement this interface
 * The stateCallable is a function that maps the metric output to the a state string (may be no-op or constant)
 * The metricCallable is a call to get the metric value in the SystemMetric result of poll().
 */
public interface EucalyptusStatsSensor {
    public List<SystemMetric> poll() throws Exception;

    public void init(String name, String description, List<String> defaultTags, long defaultTtl) throws Exception;

    //public void init(String name, String description, List<String> defaultTags, long defaultTtl, Callable<Map<String, Object>> metricCallable) throws Exception;
    public String getName();

    public String getDescription();
}
