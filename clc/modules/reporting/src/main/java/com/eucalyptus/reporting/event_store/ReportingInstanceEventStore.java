/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.reporting.event_store;

import java.util.*;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import java.util.concurrent.TimeUnit;

public class ReportingInstanceEventStore extends EventStoreSupport {
  private static final ReportingInstanceEventStore instance = new ReportingInstanceEventStore();

  public static ReportingInstanceEventStore getInstance() {
    return instance;
  }

  protected ReportingInstanceEventStore() {
  }

  public void insertCreateEvent(
      @Nonnull final String uuid,
      @Nonnull final String instanceId,
      @Nonnull final Long timestampMs,
      @Nonnull final String instanceType,
      @Nonnull final String userId,
      @Nonnull final String availabilityZone ) {
    Preconditions.checkNotNull( uuid, "Uuid is required" );
    Preconditions.checkNotNull( instanceId, "InstanceId is required" );
    Preconditions.checkNotNull( timestampMs, "TimestampMs is required" );
    Preconditions.checkNotNull( instanceType, "InstanceType is required" );
    Preconditions.checkNotNull( userId, "UserId is required" );
    Preconditions.checkNotNull( availabilityZone,
        "AvailabilityZone is required" );

    persist( new ReportingInstanceCreateEvent(
        uuid,
        instanceId,
        timestampMs,
        instanceType,
        userId,
        availabilityZone ) );
  }

  private final Map<InstanceMetricDimensionKey,MetricInfo> metricInfoMap
  		= new HashMap<InstanceMetricDimensionKey,MetricInfo>();
  
  public void insertUsageEvent( @Nonnull final String uuid,
                                @Nonnull final Long timestamp,
                                @Nonnull final String metric,
                                @Nonnull final Long sequenceNum,
                                @Nonnull final String dimension,
                                @Nonnull final Double value ) {

    Preconditions.checkNotNull( uuid, "Uuid is required" );
    Preconditions.checkNotNull( timestamp, "Timestamp is required" );
    Preconditions.checkNotNull( metric, "Metric is required" );
    Preconditions.checkNotNull( sequenceNum, "SequenceNum is required" );
    Preconditions.checkNotNull( dimension, "Dimension is required" );
    Preconditions.checkNotNull( value, "value is required" );

    /* insertUsageEvent corrects sensor resets before inserting data into the database. Sensors
     * occasionally reset when an nc reboots or for some other reason. In which case, the sequence
     * number resets to 0 and the value resets to 0. We correct this by increasing the value
     * by whatever the last value before reset was (values are cumulative) for every subsequent
     * event for this metric. To do this, we must retain the last value for each metric value, the
     * last sequence number, and the offsets; the offsets are increased every time there is a sensor
     * reset.
     * 
     * In some circumstances, the CLC will failover and this data will be lost. This will not
     * affect this algorithm. Only if a sensor has a sequence number lower than a prior remembered
     * one, will we detect a sequence reset.
     * 
     * On rare occasions, the sensor can reset at the same moment that the CLC fails over. In this
     * rare case, instance usage could go backwards in the database. Programs which read the database
     * should be aware of this rare possibility.
     * 
     * We must not retain prior data forever for all instances, so the data is periodically
     * purged.
     */
    InstanceMetricDimensionKey key = new InstanceMetricDimensionKey(uuid, metric, dimension);
    if (!metricInfoMap.containsKey(key)) {
    	metricInfoMap.put(key, new MetricInfo());
    }
    MetricInfo info = metricInfoMap.get(key);
    if (sequenceNum < info.lastSequenceNum) {
    	/* RESET HAS OCCURRED */
    	info.sequenceOffset += info.lastSequenceNum;
    	info.valueOffset += info.lastValue;
    }
    info.lastSequenceNum = sequenceNum;
    info.lastValue = value;
    info.lastEventArrived = System.currentTimeMillis();
    
    if (eventCnt++ % PURGE_EVERY_NUM_EVENTS==0) purgeMetricInfo();

    persist( new ReportingInstanceUsageEvent( uuid, metric, sequenceNum+info.sequenceOffset,
    		dimension, value+info.valueOffset, timestamp ) );
  }

  private static final long PURGE_EVERY_NUM_EVENTS = 50000;
  private static final long PURGE_OLDER_THAN_MS = TimeUnit.DAYS.toMillis(2);
  private long eventCnt = 0;

  private void purgeMetricInfo()
  {
  	for (InstanceMetricDimensionKey key: metricInfoMap.keySet()) {
		MetricInfo info = metricInfoMap.get(key);
		if (info.lastEventArrived < System.currentTimeMillis() - PURGE_OLDER_THAN_MS) {
			metricInfoMap.remove(key);
		}
	}
  }
  
  private static class InstanceMetricDimensionKey
  {
	  private final String uuid;
	  private final String metric;
	  private final String dim;
	  
	  InstanceMetricDimensionKey(String uuid, String metric, String dim)
	  {
		  this.uuid = uuid;
		  this.metric = metric;
		  this.dim = dim;
	  }

	  @Override
	  public int hashCode() {
		  final int prime = 31;
		  int result = 1;
		  result = prime * result + ((dim == null) ? 0 : dim.hashCode());
		  result = prime * result + ((metric == null) ? 0 : metric.hashCode());
		  result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		  return result;
	  }

	  @Override
	  public boolean equals(Object obj) {
		  if (this == obj) return true;
		  if (obj == null) return false;
		  if (getClass() != obj.getClass()) return false;
		  InstanceMetricDimensionKey other = (InstanceMetricDimensionKey) obj;
		  if (dim == null) {
			  if (other.dim != null) return false;
		  } else if (!dim.equals(other.dim)) return false;
		  if (metric == null) {
			  if (other.metric != null) return false;
		  } else if (!metric.equals(other.metric)) return false;
		  if (uuid == null) {
			  if (other.uuid != null)	return false;
		  } else if (!uuid.equals(other.uuid)) return false;
		  return true;
	  }
	  
	  
  }
  
  private static class MetricInfo
  {
	  private long lastSequenceNum = 0l;
	  private long sequenceOffset = 0l;
	  private double lastValue = 0d;
	  private double valueOffset = 0d;
	  private long lastEventArrived = 0l;

	  MetricInfo()
	  {
	  }
  }
  

}
