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
        this.tags = Lists.newArrayList(tags);
        Collections.sort(this.tags);
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
