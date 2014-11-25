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

import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Generic sensor that queries an MXBean to get the result.
 * Wraps a single bean and marshalls output for SystemMetric.
 */
public class MXBeanSensor extends BaseStatsSensor {
    protected static final Logger LOG = Logger.getLogger(MXBeanSensor.class);

    protected ObjectName wrappedBeanName;
    protected String queryAttribute;
    protected static final long DEFAULT_TTL = 120; //2 minutes

    public static MXBeanSensor makeSensorFromPlatformMBean(String sensorName, String sensorDescription, MBeanServer server, ObjectName beanName, String attributeNameToQuery) throws Exception {
        MXBeanSensor sensor = new MXBeanSensor();
        sensor.init(sensorName, sensorDescription, null, DEFAULT_TTL, queryPlatformMbeanServer(server, beanName, attributeNameToQuery));
        sensor.setWrappedBeanName(beanName);
        sensor.setQueryAttribute(attributeNameToQuery);
        return sensor;
    }

    public static MXBeanSensor makeSensorFromPlatformMBean(String sensorName, String sensorDescription, String beanName, String attributeNameToQuery) throws Exception {
        return makeSensorFromPlatformMBean(sensorName, sensorDescription, ManagementFactory.getPlatformMBeanServer(), new ObjectName(beanName), attributeNameToQuery);
    }

    public static Callable<Map<String, Object>> queryPlatformMbeanServer(final MBeanServer server, final ObjectName beanName, final String attributeName) {
        return new Callable<Map<String, Object>>() {
            public Map<String, Object> call() throws Exception {
                Map<String, Object> result = Maps.newHashMap();
                if (server == null) {
                    result.put(attributeName, ManagementFactory.getPlatformMBeanServer().getAttribute(beanName, attributeName).toString());
                } else {
                    result.put(attributeName, server.getAttribute(beanName, attributeName).toString());
                }
                return result;
            }
        };
    }

    public ObjectName getWrappedBeanName() {
        return wrappedBeanName;
    }

    public void setWrappedBeanName(ObjectName wrappedBeanName) {
        this.wrappedBeanName = wrappedBeanName;
    }

    public String getQueryAttribute() {
        return queryAttribute;
    }

    public void setQueryAttribute(String queryAttribute) {
        this.queryAttribute = queryAttribute;
    }
}
