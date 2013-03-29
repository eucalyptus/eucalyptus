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

package com.eucalyptus.cluster.callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import com.eucalyptus.cloudwatch.CloudWatch;
import com.eucalyptus.cloudwatch.Dimension;
import com.eucalyptus.cloudwatch.Dimensions;
import com.eucalyptus.cloudwatch.MetricData;
import com.eucalyptus.cloudwatch.MetricDatum;
import com.eucalyptus.cloudwatch.PutMetricDataType;
import com.eucalyptus.cloudwatch.PutMetricDataResponseType;

import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
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
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.BroadcastCallback;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstance;
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
    try {
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

              //CloudWatch use case of metric data

              if(!values.isEmpty()) {

                for (MetricDimensionsValuesType value : values) {
                  final Double currentValue = value.getValue();
                  final Long currentTimeStamp = value.getTimestamp().getTime();
                  sendSystemMetric(new Supplier<InstanceUsageEvent>() {
                    @Override
                    public InstanceUsageEvent get() {
                      return new InstanceUsageEvent(
                          sensorData.getResourceUuid(),
                          sensorData.getResourceName(),
                          metricType.getMetricName(),
                          counterType.getSequenceNum(),
                          dimensionType.getDimensionName(),
                          currentValue,
                          currentTimeStamp);
                    }
                  });
                }
              }

              //Reporting use case of metric data from the cc

              Collections.sort(values, Ordering.natural().onResultOf(GetTimestamp.INSTANCE));

              if (!values.isEmpty()) {
                final MetricDimensionsValuesType latestValue = Iterables.getLast(values);
                final Double usageValue = latestValue.getValue();
                final Long usageTimestamp = latestValue.getTimestamp().getTime();
                final long sequenceNumber = counterType.getSequenceNum() + (values.size() - 1);
                fireUsageEvent(new Supplier<InstanceUsageEvent>() {
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
    } catch (Exception ex) {
      LOG.debug("Unable to fire describe sensors call back", ex);
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

  private enum GetTimestamp implements Function<MetricDimensionsValuesType, Date> {
    INSTANCE;

    @Override
    public Date apply(final MetricDimensionsValuesType metricDimensionsValuesType) {
      return metricDimensionsValuesType.getTimestamp();
    }
  }

  private void sendSystemMetric(Supplier<InstanceUsageEvent> cloudWatchSupplier) throws Exception {
    InstanceUsageEvent event = null;
    event = cloudWatchSupplier.get();

    final VmInstance instance = VmInstances.lookup(event.getInstanceId());

    if (!instance.getInstanceId().equals(event.getInstanceId())
        || !instance.getBootRecord().isMonitoring()) {
      throw new NoSuchElementException("Instance : " + event.getInstanceId() + " monitoring is not enabled");
    }

    if (instance.getInstanceId().equals(event.getInstanceId())
        && instance.getBootRecord().isMonitoring()) {

      PutMetricDataType putMetricData = new PutMetricDataType();
      MetricDatum metricDatum = new MetricDatum();
      ArrayList<Dimension> dimArray = Lists.newArrayList();

      if (event.getDimension() != null && event.getValue() != null) {

        if (event.getDimension().startsWith("vol-")) {
          putMetricData.setNamespace("AWS/EBS");
          Dimension volDim = new Dimension();
          volDim.setName("VolumeId");
          volDim.setValue(event.getDimension());
          dimArray.add(volDim);
          // Need to replace metric name
          if (event.getMetric().startsWith("Disk")) {
            final String convertedEBSMetricName = event.getMetric()
                .replace("Disk", "Volume");
            metricDatum.setMetricName(convertedEBSMetricName);
          } else {
            metricDatum.setMetricName(event.getMetric());
          }
        } else {
          putMetricData.setNamespace("AWS/EC2");

          Dimension instanceIdDim = new Dimension();
          instanceIdDim.setName("InstanceId");
          instanceIdDim.setValue(instance.getInstanceId());
          dimArray.add(instanceIdDim);

          Dimension imageIdDim = new Dimension();
          imageIdDim.setName("ImageId");
          imageIdDim.setValue(instance.getImageId());
          dimArray.add(imageIdDim);

          Dimension instanceTypeDim = new Dimension();
          instanceTypeDim.setName("InstanceType");
          instanceTypeDim.setValue(instance.getVmType()
              .getDisplayName());
          dimArray.add(instanceTypeDim);

          // convert ephemeral disks metrics
          if (event.getMetric().equals("VolumeTotalReadTime")) {
            metricDatum.setMetricName("DiskReadBytes");
          } else if (event.getMetric().endsWith("External")) {
            final String convertedEC2NetworkMetricName = event
                .getMetric().replace("External", "");
            metricDatum
                .setMetricName(convertedEC2NetworkMetricName);
          } else if (event.getMetric().equals("VolumeTotalWriteTime")) {
            metricDatum.setMetricName("DiskWriteBytes");
          } else {
            metricDatum.setMetricName(event.getMetric());
          }
        }
      } else {
        LOG.debug("Event does not contain a dimension");
        throw new Exception();
      }

      Dimensions dims = new Dimensions();
      dims.setMember(dimArray);

      MetricData metricData = new MetricData();

      metricDatum.setTimestamp(new Date(event.getValueTimestamp()));
      metricDatum.setDimensions(dims);
      metricDatum.setValue(event.getValue());

      final String unitType = containsUnitType(metricDatum.getMetricName());
      metricDatum.setUnit(unitType);


      if (event.getMetric().equals("CPUUtilization")) {
        metricDatum.setMetricName("CPUUtilizationMS"); // this is actually the data in milliseconds, not percentage
      }
      if (event.getMetric().equals("VolumeReadOps")) {
        metricDatum.setMetricName("VolumeReadOpsTotal"); // this is actually the total volume read Ops since volume creation, not for the period
      }
      if (event.getMetric().equals("VolumeWriteOps")) {
        metricDatum.setMetricName("VolumeWriteOpsTotal"); // this is actually the total volume write Ops since volume creation, not for the period
      }
      if (event.getMetric().equals("VolumeReadBytes")) {
        metricDatum.setMetricName("VolumeReadBytesTotal"); // this is actually the total volume read bytes since volume creation, not for the period
      }
      if (event.getMetric().equals("VolumeWriteBytes")) {
        metricDatum.setMetricName("VolumeWriteBytesTotal"); // this is actually the total volume write bytes since volume creation, not for the period
      }
      if (event.getMetric().equals("VolumeReadTime")) {
        metricDatum.setMetricName("VolumeReadTimeTotal"); // this is actually the total volume read time since volume creation, not for the period
      }
      if (event.getMetric().equals("VolumeWriteTime")) {
        metricDatum.setMetricName("VolumeWriteTimeTotal"); // this is actually the total volume write time since volume creation, not for the period
      }
      
      metricData.setMember(Lists.newArrayList(metricDatum));
      putMetricData.setMetricData(metricData);

      Account account = Accounts.getAccountProvider().lookupAccountById(
          instance.getOwnerAccountNumber());

      User user = account.lookupUserByName(User.ACCOUNT_ADMIN);
      putMetricData.setEffectiveUserId(user.getUserId());

      ServiceConfiguration serviceConfiguration = ServiceConfigurations
          .createEphemeral(ComponentIds.lookup(CloudWatch.class));
      BaseMessage reply = (BaseMessage) AsyncRequests.dispatch(serviceConfiguration, putMetricData);
      if (!(reply instanceof PutMetricDataResponseType)) {
        throw new EucalyptusCloudException("Unable to send put metric data to cloud watch");
      }

    }
  }

  private enum Bytes {
    VolumeReadBytes,
    VolumeWriteBytes,
    DiskReadBytes,
    DiskWriteBytes,
    NetworkIn,
    NetworkOut
  }

  private enum Count {
    VolumeWriteOps,
    VolumeQueueLength,
    VolumeConsumedReadWriteOps,
    DiskReadOps,
    DiskWriteOps,
    StatusCheckFailed,
    VolumeReadOps
  }

  private enum Seconds {
    VolumeTotalReadTime,
    VolumeTotalWriteTime,
    VolumeIdleTime
  }

  private enum Percent {
    VolumeThroughputPercentage,
    CPUUtilization
  }

  private String containsUnitType(final String metricType) {
    //TODO:KEN find cleaner method of finding the metric type
    try {
      Enum.valueOf(Bytes.class, metricType);
      return "Bytes";
    } catch (IllegalArgumentException ex1) {
      try {
        Enum.valueOf(Count.class, metricType);
        return "Count";
      } catch (IllegalArgumentException ex2) {
        try {
          Enum.valueOf(Seconds.class, metricType);
          return "Seconds";
        } catch (IllegalArgumentException ex4) {
          try {
            Enum.valueOf(Percent.class, metricType);
            return "Percent";
          } catch (IllegalArgumentException ex5) {
            throw new NoSuchElementException(
                "Unknown system unit type : " + metricType);
          }
        }
      }
    }
  }

}
