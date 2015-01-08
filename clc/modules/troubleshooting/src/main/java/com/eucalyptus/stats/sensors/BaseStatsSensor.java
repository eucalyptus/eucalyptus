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
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Base common code for any sensor.
 */
public class BaseStatsSensor implements EucalyptusStatsSensor {
    protected static final Logger LOG = Logger.getLogger(MXBeanSensor.class);

    protected String sensorName;
    protected List<String> defaultTagsToApply;
    protected String defaultDescription;
    protected long defaultTtl;
    protected Callable<Map<String, Object>> valuesProvider;

    /**
     * Builds a sensor. A convenience function
     *
     * @param name
     * @param description
     * @param defaultTags
     * @param defaultTtl
     * @param sensorCallable
     * @return
     * @throws Exception
     */
    public static BaseStatsSensor buildSensor(String name, String description, List<String> defaultTags, long defaultTtl, Callable<Map<String, Object>> sensorCallable) throws Exception {
        BaseStatsSensor sensor = new BaseStatsSensor();
        sensor.init(name, description, defaultTags, defaultTtl, sensorCallable);
        return sensor;
    }

    @Override
    public String getName() {
        return sensorName;
    }

    public void setName(String sensorName) {
        this.sensorName = sensorName;
    }

    public List<String> getDefaultTagsToApply() {
        return defaultTagsToApply;
    }

    public void setDefaultTagsToApply(List<String> defaultTagsToApply) {
        this.defaultTagsToApply = defaultTagsToApply;
    }

    @Override
    public String getDescription() {
        return defaultDescription;
    }

    public void setDescription(String defaultDescription) {
        this.defaultDescription = defaultDescription;
    }

    public long getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(long defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    /**
     * Invokes the sensor call and passes the result through the result mapper to get a SystemMetric result
     * and returns it. If no mapper is configured by the sensor callable returns SystemMetric directly, then
     * the mapper is skipped.
     * <p/>
     * If there is no mapper and the sensor callable does not return SystemMetric an exception is thrown.
     *
     * @return
     * @throws Exception
     */
    @Override
    public List<SystemMetric> poll() throws Exception {
        try {
            Map<String, Object> values = this.valuesProvider == null ? null : this.valuesProvider.call();
            SystemMetric m = new SystemMetric(this.getName(), this.getDefaultTagsToApply(), this.getDescription(), null, this.getDefaultTtl());
            m.setValues(values);
            return Lists.newArrayList(m);
        } catch (Exception e) {
            LOG.warn("Exception caught invoking sensor. ", e);
            throw e;
        }
    }

    @Override
    public void init(String name, String description, List<String> defaultTags, long defaultTtl) throws Exception {
        this.sensorName = name;
        this.defaultTagsToApply = defaultTags;
        this.defaultDescription = description;
        this.defaultTtl = defaultTtl;
    }

    public void init(String name, String description, List<String> defaultTags, long defaultTtl, Callable<Map<String, Object>> sensorCallable) throws Exception {
        this.init(name, description, defaultTags, defaultTtl);
        this.valuesProvider = sensorCallable;
    }
}
