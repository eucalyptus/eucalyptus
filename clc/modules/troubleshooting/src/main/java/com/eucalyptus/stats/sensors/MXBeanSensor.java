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
