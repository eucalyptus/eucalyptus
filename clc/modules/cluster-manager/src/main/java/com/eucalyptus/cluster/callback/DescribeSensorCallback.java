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

package com.eucalyptus.cluster.callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;

import edu.ucsb.eucalyptus.msgs.DescribeSensorsResponse;
import edu.ucsb.eucalyptus.msgs.DescribeSensorsType;
import edu.ucsb.eucalyptus.msgs.MetricCounterType;
import edu.ucsb.eucalyptus.msgs.MetricDimensionsType;
import edu.ucsb.eucalyptus.msgs.MetricsResourceType;
import edu.ucsb.eucalyptus.msgs.SensorsResourceType;
import edu.ucsb.eucalyptus.msgs.MetricDimensionsValuesType;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.BroadcastCallback;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class DescribeSensorCallback extends
    BroadcastCallback<DescribeSensorsType, DescribeSensorsResponse> {

  private static final Logger LOG = Logger.getLogger(DescribeSensorCallback.class);
  private static final String RESOURCE_TYPE_INSTANCE = "instance";
  private final int historySize;
  private final int collectionIntervalTimeMs;
  private final ArrayList<String> instanceIds;
  private final ListenerRegistry listener = ListenerRegistry.getInstance();

  public DescribeSensorCallback(final int historySize,
                                final int collectionIntervalTimeMS, final ArrayList<String> instanceIds) {
    this.historySize = historySize;
    this.collectionIntervalTimeMs = collectionIntervalTimeMS;
    this.instanceIds = instanceIds;

    final DescribeSensorsType msg =
        new DescribeSensorsType(this.historySize, this.collectionIntervalTimeMs, this.instanceIds);

    try {
      msg.setUser(Accounts.lookupSystemAdmin());
    } catch (AuthException e) {
      LOG.error("Unable to find the system user", e);
    }

    this.setRequest(msg);
  }

  @Override
  public void initialize(final DescribeSensorsType msg) {
  }

  @Override
  public BroadcastCallback<DescribeSensorsType, DescribeSensorsResponse> newInstance() {
    return new DescribeSensorCallback(this.historySize, this.collectionIntervalTimeMs, this.instanceIds);
  }

  @Override
  public void fireException(Throwable e) {
    LOG.debug("Request failed: "
        + LogUtil.subheader(this.getRequest().toString(
        "eucalyptus_ucsb_edu")));
    Logs.extreme().error(e, e);
  }



  @Override
  public void fire(final DescribeSensorsResponse msg) {
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

  private void processCloudWatchStats(final DescribeSensorsResponse msg) throws Exception {
    CloudWatchHelper cloudWatchHelper = new CloudWatchHelper(new CloudWatchHelper.DefaultInstanceInfoProvider());
    List<PutMetricDataType> putMetricDataList = cloudWatchHelper.collectMetricData(msg);
    ServiceConfiguration serviceConfiguration = CloudWatchHelper.createServiceConfiguration();
    for (PutMetricDataType putMetricData: putMetricDataList) {
      cloudWatchHelper.sendSystemMetric(serviceConfiguration, putMetricData);
    }
  }


  private void processReportingStats(final DescribeSensorsResponse msg) throws Exception {
    final Iterable<String> uuidList =
        Iterables.transform(VmInstances.list(VmState.RUNNING), VmInstances.toInstanceUuid());
    for (final SensorsResourceType sensorData : msg.getSensorsResources()) {
      if (!RESOURCE_TYPE_INSTANCE.equals(sensorData.getResourceType()) ||
          !Iterables.contains(uuidList, sensorData.getResourceUuid()))
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
    InstanceUsageEvent event = null;
    event = instanceUsageEventSupplier.get();
    try {
      listener.fireEvent(event);
    } catch (EventFailedException e) {
      LOG.debug("Failed to fire instance usage event"
          + (event != null ? event : ""), e);
    }
  }

  enum GetTimestamp implements Function<MetricDimensionsValuesType, Date> {
    INSTANCE;

    @Override
    public Date apply(final MetricDimensionsValuesType metricDimensionsValuesType) {
      return metricDimensionsValuesType.getTimestamp();
    }
  }

}
