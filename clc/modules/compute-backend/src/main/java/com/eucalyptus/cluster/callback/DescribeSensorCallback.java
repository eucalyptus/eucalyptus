/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

package com.eucalyptus.cluster.callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.eucalyptus.cluster.callback.reporting.AbsoluteMetricQueue;
import com.eucalyptus.cluster.callback.reporting.AbsoluteMetricQueueItem;
import com.eucalyptus.cluster.callback.reporting.CloudWatchHelper;
import com.eucalyptus.cluster.common.msgs.DescribeSensorsResponseType;
import com.eucalyptus.cluster.common.msgs.DescribeSensorsType;
import com.eucalyptus.cluster.common.msgs.MetricCounterType;
import com.eucalyptus.cluster.common.msgs.MetricDimensionsType;
import com.eucalyptus.cluster.common.msgs.MetricsResourceType;
import com.eucalyptus.cluster.common.msgs.SensorsResourceType;
import com.eucalyptus.cluster.common.msgs.MetricDimensionsValuesType;

import org.apache.log4j.Logger;

import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.util.LogUtil;

import com.eucalyptus.util.async.MessageCallback;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class DescribeSensorCallback extends
    MessageCallback<DescribeSensorsType, DescribeSensorsResponseType> {

  private static final Logger LOG = Logger.getLogger(DescribeSensorCallback.class);
  private static final String RESOURCE_TYPE_INSTANCE = "instance";
  private final ArrayList<String> instanceIds;
  private final ListenerRegistry listener = ListenerRegistry.getInstance();

  public DescribeSensorCallback(
      final int historySize,
      final int collectionIntervalTimeMS,
      final ArrayList<String> instanceIds
  ) {
    this.instanceIds = instanceIds;

    final DescribeSensorsType msg =
        new DescribeSensorsType( historySize, collectionIntervalTimeMS, this.instanceIds );

    this.setRequest(msg);
  }

  @Override
  public void initialize(final DescribeSensorsType msg) {
  }

  @Override
  public void fireException(Throwable e) {
    LOG.debug("Request failed: "
        + LogUtil.subheader(this.getRequest().toString(
        "eucalyptus_ucsb_edu")));
    Logs.extreme().error(e, e);
  }

  @Override
  public void fire(final DescribeSensorsResponseType msg) {
    LOG.trace("DescribeSensorCallback (fire) called at " + new Date());
    try {
      processCloudWatchStats(msg);
    } catch (Exception ex) {
      LOG.debug("Unable to fire describe sensors call back (cloudwatch)", ex);
    }
    try {
      processReportingStats(msg);
    } catch (Exception ex) {
      LOG.debug("Unable to fire describe sensors call back (reporting)", ex);
    }
  }

  private void processCloudWatchStats(final DescribeSensorsResponseType msg) throws Exception {
    CloudWatchHelper cloudWatchHelper = new CloudWatchHelper(new CloudWatchHelper.DefaultInstanceInfoProvider());
    List<AbsoluteMetricQueueItem> queueItems = cloudWatchHelper.collectMetricData(instanceIds, msg);
    AbsoluteMetricQueue.getInstance().addQueueItems(queueItems);
  }


  private void processReportingStats(final DescribeSensorsResponseType msg) throws Exception {
    for (final SensorsResourceType sensorData : msg.getSensorsResources()) {
      if (!RESOURCE_TYPE_INSTANCE.equals(sensorData.getResourceType()) ||
          !instanceIds.contains( sensorData.getResourceName( )) ||
          sensorData.getResourceUuid( ) == null ||
          sensorData.getResourceUuid( ).isEmpty() )
        continue;
      
      for (final MetricsResourceType metricType : sensorData.getMetrics()) {
        for (final MetricCounterType counterType : metricType.getCounters()) {
          for (final MetricDimensionsType dimensionType : counterType.getDimensions()) {
            // find and fire most recent value for metric/dimension
            final List<MetricDimensionsValuesType> values =
                Lists.newArrayList(dimensionType.getValues());

            //Reporting use case of metric data from the cc
            Collections.sort(values, Ordering.natural().onResultOf(GetTimestamp.INSTANCE));

            if (!values.isEmpty()) {
              final MetricDimensionsValuesType latestValue = Iterables.getLast(values);
              final Double usageValue = latestValue.getValue();
              if (usageValue == null) {
                LOG.debug("Event received with null 'value', skipping for reporting");
                continue;
              }
              final Long usageTimestamp = latestValue.getTimestamp().getTime();
              final long sequenceNumber = dimensionType.getSequenceNum() + (values.size() - 1);
              fireUsageEvent( new Supplier<InstanceUsageEvent>(){
                @Override
                public InstanceUsageEvent get() {
                  return new InstanceUsageEvent(
                      sensorData.getResourceUuid(),
                      sensorData.getResourceName(),
                      metricType.getMetricName(),
                      sequenceNumber,
                      dimensionType.getDimensionName(),
                      usageValue,
                      usageTimestamp);
                }
              });
            }
          }
        }
      }
    }
  }  

  private void fireUsageEvent(Supplier<InstanceUsageEvent> instanceUsageEventSupplier) {
    InstanceUsageEvent event = instanceUsageEventSupplier.get();
    try {
      listener.fireEvent(event);
    } catch (EventFailedException e) {
      LOG.debug("Failed to fire instance usage event"
          + (event != null ? event : ""), e);
    }
  }

  public enum GetTimestamp implements Function<MetricDimensionsValuesType, Date> {
    INSTANCE;

    @Override
    public Date apply(final MetricDimensionsValuesType metricDimensionsValuesType) {
      return metricDimensionsValuesType.getTimestamp();
    }
  }

}
