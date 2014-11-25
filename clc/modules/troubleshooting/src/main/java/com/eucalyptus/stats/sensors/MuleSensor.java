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

import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.mule.module.management.mbean.MuleConfigurationService;
import org.mule.module.management.mbean.MuleConfigurationServiceMBean;
import org.mule.module.management.mbean.ServiceService;
import org.mule.module.management.mbean.ServiceServiceMBean;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Outputs JVM-wide mule message stats from the "Application Totals" property of the MXBean used
 * by Mule.
 */
public enum MuleSensor implements Callable<Map<String, Object>> {
    TOTAL {
        @Override
        public Map<String, Object> call() {
            try {
                ObjectName muleApplicationStatsName = ObjectName.getInstance("*:type=Application,name=\"application totals\"");
                MBeanAttributeInfo[] attrs = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(muleApplicationStatsName).getAttributes();
                String[] attrNames = new String[attrs.length];
                Map<String, Object> resultMap = Maps.newTreeMap();
                int i = 0;
                for (MBeanAttributeInfo info : attrs) {
                    attrNames[i++] = info.getName();
                }

                AttributeList resultAttrs = ManagementFactory.getPlatformMBeanServer().getAttributes(muleApplicationStatsName, attrNames);
                Attribute tmpAtt;
                for (Object att : resultAttrs) {
                    tmpAtt = (Attribute) att;
                    resultMap.put(tmpAtt.getName(), tmpAtt.getValue());
                }

                //reset the stats.
                resetMuleTotalStats();
                return resultMap;
            } catch (Throwable f) {
                LOG.error("Mule application stats sensor call failed. Cannot gather latest data", f);
                throw Exceptions.toUndeclared(f);
            }
        }

        private String muleDomainString = null;
        private String getMuleDomain() {
            if(muleDomainString == null) {
                for (String domain : ManagementFactory.getPlatformMBeanServer().getDomains()) {
                    if (domain.startsWith("Mule")) {
                        muleDomainString = domain;
                        break;
                    }
                }
            }
            return muleDomainString;
        }

        private void resetMuleTotalStats() {
            /*
            Invoke the necessary stats call on each service to reset message count/perf stats for next
            sensor invocation
             */

        }
    },
    SERVICES {
        @Override
        public Map<String, Object> call() {
            try {
                ObjectName muleServiceStatsMatchName = ObjectName.getInstance("*:type=org.mule.statistics,service=*");
                MBeanAttributeInfo[] attrs = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(muleServiceStatsMatchName).getAttributes();
                String[] attrNames = new String[attrs.length];
                Map<String, Object> resultMap = Maps.newTreeMap();
                int i = 0;
                for (MBeanAttributeInfo info : attrs) {
                    attrNames[i++] = info.getName();
                }

                AttributeList resultAttrs = ManagementFactory.getPlatformMBeanServer().getAttributes(muleServiceStatsMatchName, attrNames);
                Attribute tmpAtt;
                for (Object att : resultAttrs) {
                    tmpAtt = (Attribute) att;
                    resultMap.put(tmpAtt.getName(), tmpAtt.getValue());
                }
                return resultMap;
            } catch (Throwable f) {
                LOG.error("Mule service stats sensor call failed. Cannot gather latest data", f);
                throw Exceptions.toUndeclared(f);
            }
        }

        private String muleDomainString;

        private String getMuleDomain() {
            if(muleDomainString == null) {
                for (String domain : ManagementFactory.getPlatformMBeanServer().getDomains()) {
                    if (domain.startsWith("Mule")) {
                        muleDomainString = domain;
                        break;
                    }
                }
            }
            return muleDomainString;
        }
    };
    private static final Logger LOG = Logger.getLogger(MuleSensor.class);
}
