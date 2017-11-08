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
