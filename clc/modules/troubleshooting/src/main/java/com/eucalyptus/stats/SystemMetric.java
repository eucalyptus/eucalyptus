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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.json.JSONObject;
import org.apache.commons.collections.list.TreeList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A single metric value. The base unit of sensor reporting.
 */
public class SystemMetric {
    private TreeMap<String, Object> values; //The measured values, in sorted order when retrieved
    private long timestamp; //Time of this event occurred (not delivered), in unix epoch seconds
    private long ttl; //Time this event is "valid" in seconds, after which it should be dropped or ignored. <0 indicates forever.
    private String sensor; //Name to identify the metric. e.g. StorageControllerServiceState
    private List<String> tags; //Arbitrary tag set for use outside the system
    private String description; //Free-form description

    public SystemMetric(String name,
                        List<String> tagsToUse,
                        String desc,
                        Map<String, Object> sensorValues,
                        long eventTimestamp,
                        long eventTtl) {
        setSensor(name);
        setTags(tagsToUse);
        setDescription(desc);
        setValues(sensorValues);
        setTimestamp(eventTimestamp);
        setTtl(eventTtl);
    }

    public SystemMetric(String serv,
                        List<String> tagsToUse,
                        String desc,
                        Map<String, Object> metricValues,
                        long eventTtl) {
        this(serv, tagsToUse, desc, metricValues, System.currentTimeMillis() / 1000l, eventTtl);
    }

    @Override
    public String toString() {
        JSONObject obj = new JSONObject();
        obj.accumulate("timestamp", this.timestamp);
        obj.accumulate("sensor", this.sensor);
        obj.accumulate("description", this.description);
        obj.accumulate("tags", this.tags);
        obj.accumulate("values", this.values);
        obj.accumulate("ttl", this.ttl);
        return obj.toString(4);
    }

    public String getSensor() {
        return sensor;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getTtl() {
        return ttl;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getDescription() {
        return description;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }

    /**
     * Sets the tag. The list will be sorted lexicographically when set but
     * the argument list is unchanged
     * @param tags
     */
    public void setTags(List<String> tags) {
        if(tags != null) {
            this.tags = Lists.newArrayList(tags);
            Collections.sort(this.tags);
        } else {
            this.tags = Lists.newArrayList();
        }

    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> sensorValues) {
        //Use sorted structure
        this.values = Maps.newTreeMap();
        if(sensorValues != null) {
            this.values.putAll(sensorValues);
        }
    }
}
