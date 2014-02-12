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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.cloudwatch.common.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.msgs.Dimensions;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataResponseType;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;
import com.eucalyptus.cluster.callback.DescribeSensorCallback.GetTimestamp;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstanceTag;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmInstance.VmState;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.DescribeSensorsResponse;
import edu.ucsb.eucalyptus.msgs.MetricCounterType;
import edu.ucsb.eucalyptus.msgs.MetricDimensionsType;
import edu.ucsb.eucalyptus.msgs.MetricDimensionsValuesType;
import edu.ucsb.eucalyptus.msgs.MetricsResourceType;
import edu.ucsb.eucalyptus.msgs.SensorsResourceType;

public class CloudWatchHelper {

  private InstanceInfoProvider instanceInfoProvider;
  public CloudWatchHelper(InstanceInfoProvider instanceInfoProvider) {
    this.instanceInfoProvider = instanceInfoProvider;

  }
  private static final Logger LOG = Logger.getLogger(CloudWatchHelper.class);
  private static final String RESOURCE_TYPE_INSTANCE = "instance";
  private static class DiskReadWriteMetricTypeCache {
    
    private Map<String, MetricDimensionsValuesType> eventMap = Maps.newConcurrentMap();
    private String mapKey(SensorsResourceType sensorData,
        MetricDimensionsType dimensionType, MetricDimensionsValuesType value) {

      String SEPARATOR = "|";
      // sensor data should include resource Uuid and resource name
      String resourceUUID = (sensorData != null) ? sensorData.getResourceUuid() : null;
      String resourceName = (sensorData != null) ? sensorData.getResourceName() : null;
      // dimension type should include dimension name
      String dimensionName = (dimensionType != null) ? dimensionType.getDimensionName() : null;
      // value should include timestamp
      String valueTimestampStr = (value != null && value.getTimestamp() != null) ? value.getTimestamp().toString() : null;
      return resourceUUID + SEPARATOR + resourceName + SEPARATOR + dimensionName + SEPARATOR + valueTimestampStr;
      
      
    }
    public void putEventInCache(SensorsResourceType sensorData,
        MetricDimensionsType dimensionType, MetricDimensionsValuesType value) {
      eventMap.put(mapKey(sensorData, dimensionType, value), value);
    }

    public MetricDimensionsValuesType getEventFromCache(
        SensorsResourceType sensorData, MetricDimensionsType dimensionType,
        MetricDimensionsValuesType value) {
      return eventMap.get(mapKey(sensorData, dimensionType, value));
    }
  }

  private static class EC2DiskMetricCacheKey {
    private String resourceUuid;
    private String resourceName;
    private Long currentTimeStamp;
    private String metricName;
    private EC2DiskMetricCacheKey(String resourceUuid, String resourceName,
        Long currentTimeStamp, String metricName) {
      super();
      this.resourceUuid = resourceUuid;
      this.resourceName = resourceName;
      this.currentTimeStamp = currentTimeStamp;
      this.metricName = metricName;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + ((currentTimeStamp == null) ? 0 : currentTimeStamp.hashCode());
      result = prime * result
          + ((metricName == null) ? 0 : metricName.hashCode());
      result = prime * result
          + ((resourceName == null) ? 0 : resourceName.hashCode());
      result = prime * result
          + ((resourceUuid == null) ? 0 : resourceUuid.hashCode());
      return result;
    }
    public String getResourceUuid() {
      return resourceUuid;
    }
    public String getResourceName() {
      return resourceName;
    }
    public Long getCurrentTimeStamp() {
      return currentTimeStamp;
    }
    public String getMetricName() {
      return metricName;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      EC2DiskMetricCacheKey other = (EC2DiskMetricCacheKey) obj;
      if (currentTimeStamp == null) {
        if (other.currentTimeStamp != null)
          return false;
      } else if (!currentTimeStamp.equals(other.currentTimeStamp))
        return false;
      if (metricName == null) {
        if (other.metricName != null)
          return false;
      } else if (!metricName.equals(other.metricName))
        return false;
      if (resourceName == null) {
        if (other.resourceName != null)
          return false;
      } else if (!resourceName.equals(other.resourceName))
        return false;
      if (resourceUuid == null) {
        if (other.resourceUuid != null)
          return false;
      } else if (!resourceUuid.equals(other.resourceUuid))
        return false;
      return true;
    }
    
  }
  private static class EC2DiskMetricCacheValue {
    private EC2DiskMetricCacheKey key;
    private Double value;
    public EC2DiskMetricCacheValue(EC2DiskMetricCacheKey key, Double value) {
      this.key = key;
      this.value = value;
    }
    public void addValue(Double currentValue) {
      this.value += currentValue;
    }
    public String getMetricName() {
      return key.getMetricName();
    }
    public Double getValue() {
      return value;
    }
    public Long getTimeStamp() {
      return key.getCurrentTimeStamp();
    }
    public String getResourceName() {
      return key.getResourceName();
    }
    public String getResourceUuid() {
      return key.getResourceUuid();
    }
    
  }
  private static class EC2DiskMetricCache {
    private ConcurrentMap<EC2DiskMetricCacheKey, EC2DiskMetricCacheValue> cacheMap = Maps.newConcurrentMap();

    public void addToMetric(String resourceUuid, String resourceName,
        String metricName, Double currentValue, Long currentTimeStamp) {
      EC2DiskMetricCacheKey key = new EC2DiskMetricCacheKey(resourceUuid, resourceName, currentTimeStamp, metricName);
      EC2DiskMetricCacheValue value = cacheMap.get(key);
      if (value == null) {
        cacheMap.put(key, new EC2DiskMetricCacheValue(key, currentValue));
      } else {
        value.addValue(currentValue);
      }
    }

    public void initializeMetrics(String resourceUuid,
        String resourceName, Long currentTimeStamp) {
      for (String metricName: EC2_DISK_METRICS) {
        addToMetric(resourceUuid, resourceName, metricName, 0.0, currentTimeStamp);
      }
    }

    public Collection<Supplier<InstanceUsageEvent>> getMetrics() {
      ArrayList<Supplier<InstanceUsageEvent>> suppliers = Lists.newArrayList();
      for (final EC2DiskMetricCacheValue value: cacheMap.values()) {
        suppliers.add(new Supplier<InstanceUsageEvent>() {
          @Override
          public InstanceUsageEvent get() {
            return new InstanceUsageEvent(
              value.getResourceUuid(),
              value.getResourceName(),
              value.getMetricName(),
              0L, // TODO: deal with sequence numbers?
              "Ephemeral",
              value.getValue(),
              value.getTimeStamp());
          }
        });
      }
      return suppliers;
    }
    
  }

  private Supplier<InstanceUsageEvent> combineReadWriteDiskMetric(String readMetricName, String writeMetricName,
      ConcurrentMap<String, DiskReadWriteMetricTypeCache> metricCacheMap,
      String combinedMetricName, MetricsResourceType metricType,
      SensorsResourceType sensorData, MetricDimensionsType dimensionType,
      MetricDimensionsValuesType thisValueType) throws Exception {
    metricCacheMap.putIfAbsent(readMetricName, new DiskReadWriteMetricTypeCache());
    metricCacheMap.putIfAbsent(writeMetricName, new DiskReadWriteMetricTypeCache());
    
    String matchingMetricName = null;
    String otherMetricName = null;
    if (metricType.getMetricName().equals(readMetricName)) {
      matchingMetricName = readMetricName;
      otherMetricName = writeMetricName;
    } else if (metricType.getMetricName().equals(writeMetricName)) {
      matchingMetricName = writeMetricName;
      otherMetricName = readMetricName;
    }
    if (matchingMetricName != null && otherMetricName != null) {
      metricCacheMap.get(matchingMetricName).putEventInCache(sensorData, dimensionType, thisValueType);
      MetricDimensionsValuesType otherValueType = metricCacheMap.get(otherMetricName).getEventFromCache(sensorData, dimensionType, thisValueType);
      if (otherValueType != null) {
        return createDiskOpsCacheSupplier(
          sensorData, 
        combinedMetricName, 
        dimensionType,
        thisValueType.getValue() + otherValueType.getValue(),
        thisValueType.getTimestamp().getTime());
      }
    }
    return null;
  }

  private Supplier<InstanceUsageEvent> createDiskOpsCacheSupplier(
      final SensorsResourceType sensorData, final String combinedMetricName,
      final MetricDimensionsType dimensionType, final Double value,
      final Long usageTimeStamp) {
    
    return new Supplier<InstanceUsageEvent>(){
      @Override
      public InstanceUsageEvent get() {
        return new InstanceUsageEvent(
            sensorData.getResourceUuid(),
            sensorData.getResourceName(),
            combinedMetricName,
            dimensionType.getSequenceNum(),
            dimensionType.getDimensionName(),
            value,
            usageTimeStamp);
      }
    };
  }

  private static final Set<String> EC2_DISK_METRICS = ImmutableSet.of(
      "DiskReadOps",
      "DiskWriteOps",
      "DiskReadBytes",
      "DiskWriteBytes"
  );

  private static final Set<String> UNSUPPORTED_EC2_METRICS = ImmutableSet.of(
      "NetworkInExternal", 
      "NetworkOutExternal", 
      "VolumeQueueLength", 
      "VolumeTotalReadTime", 
      "VolumeTotalWriteTime", 
      "VolumeTotalReadWriteTime", 
      "VolumeConsumedReadWriteOps",
      "DiskTotalReadTime", 
      "DiskTotalWriteTime", 
      "DiskConsumedReadWriteOps");

  private static final Map<String, String> ABSOLUTE_METRICS = 
      new ImmutableMap.Builder<String, String>()
      .put("CPUUtilization", "CPUUtilizationMSAbsolute") // this is actually the data in milliseconds, not percentage
      .put("VolumeReadOps", "VolumeReadOpsAbsolute") // this is actually the total volume read Ops since volume creation, not for the period
      .put("VolumeWriteOps", "VolumeWriteOpsAbsolute") // this is actually the total volume write Ops since volume creation, not for the period
      .put("VolumeConsumedReadWriteOps", "VolumeConsumedReadWriteOpsAbsolute") // this is actually the total volume consumed read write Ops since volume creation, not for the period
      .put("VolumeReadBytes", "VolumeReadBytesAbsolute") // this is actually the total volume read bytes since volume creation, not for the period
      .put("VolumeWriteBytes", "VolumeWriteBytesAbsolute") // this is actually the total volume write bytes since volume creation, not for the period
      .put("VolumeTotalReadTime", "VolumeTotalReadTimeAbsolute") // this is actually the total volume read time since volume creation, not for the period
      .put("VolumeTotalWriteTime", "VolumeTotalWriteTimeAbsolute") // this is actually the total volume read and write time since volume creation, not for the period
      .put("VolumeTotalReadWriteTime", "VolumeTotalReadWriteTimeAbsolute") // this is actually the total volume read and write time since volume creation, not for the period
      .put("DiskReadOps", "DiskReadOpsAbsolute") // this is actually the total disk read Ops since instance creation, not for the period
      .put("DiskWriteOps", "DiskWriteOpsAbsolute") // this is actually the total disk write Ops since instance creation, not for the period
      .put("DiskReadBytes", "DiskReadBytesAbsolute") // this is actually the total disk read bytes since instance creation, not for the period
      .put("DiskWriteBytes", "DiskWriteBytesAbsolute") // this is actually the total disk write bytes since instance creation, not for the period
      .put("NetworkIn", "NetworkInAbsolute") // this is actually the total network in bytes since instance creation, not for the period
      .put("NetworkOut", "NetworkOutAbsolute") // this is actually the total network out bytes since instance creation, not for the period
      .build();

  private enum Bytes {
    VolumeReadBytes,
    VolumeWriteBytes,
    DiskReadBytes,
    DiskWriteBytes,
    NetworkIn,
    NetworkOut,
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
    VolumeTotalReadWriteTime,
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

  public static ServiceConfiguration createServiceConfiguration() {
    return Topology.lookup( CloudWatch.class );
  }

  public void sendSystemMetric(PutMetricDataType putMetricData) throws Exception {
    sendSystemMetric(createServiceConfiguration(), putMetricData);
  }
  public void sendSystemMetric(ServiceConfiguration serviceConfiguration, PutMetricDataType putMetricData) throws Exception {
    BaseMessage reply = (BaseMessage) AsyncRequests.dispatch(serviceConfiguration, putMetricData).get();
    if (!(reply instanceof PutMetricDataResponseType)) {
      throw new EucalyptusCloudException("Unable to send put metric data to cloud watch");
    }
  }

  public interface InstanceInfoProvider {
    public Iterable<String> getRunningInstanceUUIDList();
    public String getAutoscalingGroupName(String instanceId);
    public String getInstanceId(String instanceId);
    public String getImageId(String instanceId);
    public String getVmTypeDisplayName(String instanceId);
    public String getEffectiveUserId(String instanceId) throws Exception;
    public boolean getMonitoring(String instanceId);
  }

  public static class DefaultInstanceInfoProvider implements InstanceInfoProvider {
    Map<String, VmInstance> cachedInstances = new HashMap<String, VmInstance>();
    @Override
    public Iterable<String> getRunningInstanceUUIDList() {
      return Iterables.transform(VmInstances.list(VmState.RUNNING), VmInstances.toInstanceUuid());
    }
    private VmInstance lookupInstance(String instanceId) {
      if (cachedInstances.containsKey(instanceId)) {
        return cachedInstances.get(instanceId);
      } else {
        VmInstance instance = VmInstances.lookup(instanceId);
        cachedInstances.put(instanceId, instance);
        return instance;
      }
    }
    
    @Override
    public String getAutoscalingGroupName(String instanceId) {
      VmInstance instance = lookupInstance(instanceId);
      try {
        return Transactions.find(VmInstanceTag.named(instance, instance.getOwner(), "aws:autoscaling:groupName")).getValue();
      } catch (Exception ex) {
        return null;
      }
    }

    @Override
    public String getInstanceId(String instanceId) {
      VmInstance instance = lookupInstance(instanceId);
      return instance.getInstanceId();
    }

    @Override
    public String getImageId(String instanceId) {
      VmInstance instance = lookupInstance(instanceId);
      return instance.getImageId();
    }

    @Override
    public String getVmTypeDisplayName(String instanceId) {
      VmInstance instance = lookupInstance(instanceId);
      return instance.getVmType().getDisplayName();
    }

    @Override
    public String getEffectiveUserId(String instanceId) throws Exception {
      VmInstance instance = lookupInstance(instanceId);
      Account account = Accounts.getAccountProvider().lookupAccountById(instance.getOwnerAccountNumber());
      User user = account.lookupUserByName(User.ACCOUNT_ADMIN);
      return user.getUserId();
    }

    @Override
    public boolean getMonitoring(String instanceId) {
      VmInstance instance = lookupInstance(instanceId);
      return instance.getMonitoring();
    }
    
  }
  public List<PutMetricDataType> collectMetricData(DescribeSensorsResponse msg) throws Exception {
    ArrayList<PutMetricDataType> putMetricDataList = new ArrayList<PutMetricDataType>();
    final Iterable<String> uuidList = instanceInfoProvider.getRunningInstanceUUIDList();

    // cloudwatch metric caches
    final ConcurrentMap<String, DiskReadWriteMetricTypeCache> metricCacheMap = Maps.newConcurrentMap();
    
    final EC2DiskMetricCache ec2DiskMetricCache = new EC2DiskMetricCache();

    for (final SensorsResourceType sensorData : msg.getSensorsResources()) {
      if (!RESOURCE_TYPE_INSTANCE.equals(sensorData.getResourceType()) ||
          !Iterables.contains(uuidList, sensorData.getResourceUuid()))
        continue;
      
      for (final MetricsResourceType metricType : sensorData.getMetrics()) {
        for (final MetricCounterType counterType : metricType.getCounters()) {
          for (final MetricDimensionsType dimensionType : counterType.getDimensions()) {
            // find and fire most recent value for metric/dimension
            final List<MetricDimensionsValuesType> values =
                Lists.newArrayList(stripMilliseconds(dimensionType.getValues()));;

            //CloudWatch use case of metric data
            // best to enter older data first...
            Collections.sort(values, Ordering.natural().onResultOf(GetTimestamp.INSTANCE));
            if(!values.isEmpty()) {

              for (MetricDimensionsValuesType value : values) {
                LOG.trace("ResourceUUID: " + sensorData.getResourceUuid());
                LOG.trace("ResourceName: " + sensorData.getResourceName());
                LOG.trace("Metric: " + metricType.getMetricName());
                LOG.trace("Dimension: " + dimensionType.getDimensionName());
                LOG.trace("Timestamp: " + value.getTimestamp());
                LOG.trace("Value: " + value.getValue());
                final Long currentTimeStamp = value.getTimestamp().getTime();
                final Double currentValue = value.getValue();
                if (currentValue == null) {
                  LOG.debug("Event received with null 'value', skipping for cloudwatch");
                  continue;
                }
                boolean hasEc2DiskMetricName = EC2_DISK_METRICS.contains(metricType.getMetricName().replace("Volume", "Disk"));
                // Let's try only creating "zero" points for timestamps from disks
                if (hasEc2DiskMetricName) {
                  ec2DiskMetricCache.initializeMetrics(sensorData.getResourceUuid(), sensorData.getResourceName(), currentTimeStamp); // Put a place holder in in case we don't have any non-EBS volumes
                }
                boolean isEbsMetric = dimensionType.getDimensionName().startsWith("vol-");
                boolean isEc2DiskMetric = !isEbsMetric && hasEc2DiskMetricName;
                
                if (isEbsMetric || !isEc2DiskMetric) {
                  addToPutMetricDataList(putMetricDataList, 
                    new Supplier<InstanceUsageEvent>() {
                    @Override
                    public InstanceUsageEvent get() {
                      return new InstanceUsageEvent(
                        sensorData.getResourceUuid(),
                        sensorData.getResourceName(),
                        metricType.getMetricName(),
                        dimensionType.getSequenceNum(),
                        dimensionType.getDimensionName(),
                        currentValue,
                        currentTimeStamp);
                    }
                  });

                  if (isEbsMetric) {
                    // special case to calculate VolumeConsumedReadWriteOps
                    // As it is (VolumeThroughputPercentage / 100) * (VolumeReadOps + VolumeWriteOps), and we are hard coding
                    // VolumeThroughputPercentage as 100%, we will just use VolumeReadOps + VolumeWriteOps
                
                    // And just in case VolumeReadOps is called DiskReadOps we do both cases...
                    addToPutMetricDataList(putMetricDataList, combineReadWriteDiskMetric("DiskReadOps", "DiskWriteOps", metricCacheMap, "DiskConsumedReadWriteOps", metricType, sensorData, dimensionType, value));
                    addToPutMetricDataList(putMetricDataList, combineReadWriteDiskMetric("VolumeReadOps", "VolumeWriteOps", metricCacheMap, "VolumeConsumedReadWriteOps", metricType, sensorData, dimensionType, value));

                    // Also need VolumeTotalReadWriteTime to compute VolumeIdleTime
                    addToPutMetricDataList(putMetricDataList, combineReadWriteDiskMetric("VolumeTotalReadTime", "VolumeTotalWriteTime", metricCacheMap, "VolumeTotalReadWriteTime", metricType, sensorData, dimensionType, value));
                  }
                } else {
                  // see if it is a volume metric
                  String metricName = metricType.getMetricName().replace("Volume", "Disk"); 
                    ec2DiskMetricCache.addToMetric(sensorData.getResourceUuid(), sensorData.getResourceName(), metricName, currentValue, currentTimeStamp);
                }
              }
            }
          }
        }
      }
    }
    Collection<Supplier<InstanceUsageEvent>> ec2DiskMetrics = ec2DiskMetricCache.getMetrics();
    List<Supplier<InstanceUsageEvent>> ec2DiskMetricsSorted = Lists.newArrayList(ec2DiskMetrics);
    Collections.sort(ec2DiskMetricsSorted, Ordering.natural().onResultOf(new Function<Supplier<InstanceUsageEvent>, Long>() {
      @Override
      @Nullable
      public Long apply(@Nullable Supplier<InstanceUsageEvent> supplier) {
        return supplier.get().getValueTimestamp();
      }}));
    for (Supplier<InstanceUsageEvent> ec2DiskMetric: ec2DiskMetricsSorted) {
      try {
        addToPutMetricDataList(putMetricDataList, ec2DiskMetric);
      } catch (Exception ex) {
        LOG.debug("Unable to add system metric " +ec2DiskMetric, ex);
      }
    }
    return putMetricDataList;
  }  

  private List<MetricDimensionsValuesType> stripMilliseconds(ArrayList<MetricDimensionsValuesType> values) {
    List<MetricDimensionsValuesType> newValues = new ArrayList<MetricDimensionsValuesType>();
    for (MetricDimensionsValuesType value: values) {
      MetricDimensionsValuesType newValue = new MetricDimensionsValuesType();
      // round down to the lowest second
      newValue.setTimestamp(value.getTimestamp() != null ? new Date((value.getTimestamp().getTime() / 1000L) * 1000L) : null);
      newValue.setValue(value.getValue());
      newValues.add(newValue);
    }
    return newValues;
  }

  private static class UserIdAndNamespace {
    private String userId;
    private String namespace;
    public String getUserId() {
      return userId;
    }
    public String getNamespace() {
      return namespace;
    }
    private UserIdAndNamespace(String userId, String namespace) {
      super();
      this.userId = userId;
      this.namespace = namespace;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + ((namespace == null) ? 0 : namespace.hashCode());
      result = prime * result + ((userId == null) ? 0 : userId.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      UserIdAndNamespace other = (UserIdAndNamespace) obj;
      if (namespace == null) {
        if (other.namespace != null)
          return false;
      } else if (!namespace.equals(other.namespace))
        return false;
      if (userId == null) {
        if (other.userId != null)
          return false;
      } else if (!userId.equals(other.userId))
        return false;
      return true;
    }
    
  }
  public static List<PutMetricDataType> consolidatePutMetricDataList(
      List<PutMetricDataType> putMetricDataList) {
    int MAX_PUT_METRIC_DATA_ITEMS = 20;
    LinkedHashMap<UserIdAndNamespace, List<MetricDatum>> metricDataMap = new LinkedHashMap<UserIdAndNamespace, List<MetricDatum>>();
    for (PutMetricDataType putMetricData: putMetricDataList) {
      UserIdAndNamespace userIdAndNamespace = new UserIdAndNamespace(putMetricData.getEffectiveUserId(), putMetricData.getNamespace());
      if (!metricDataMap.containsKey(userIdAndNamespace)) {
        metricDataMap.put(userIdAndNamespace, new ArrayList<MetricDatum>());
      }
      metricDataMap.get(userIdAndNamespace).addAll(putMetricData.getMetricData().getMember());
    }
    ArrayList<PutMetricDataType> retVal = new ArrayList<PutMetricDataType>();
    for (Map.Entry<UserIdAndNamespace, List<MetricDatum>> metricDataEntry: metricDataMap.entrySet()) {
      for (int i = 0; i < metricDataEntry.getValue().size(); i += MAX_PUT_METRIC_DATA_ITEMS) {
        PutMetricDataType putMetricData = new PutMetricDataType();
        putMetricData.setEffectiveUserId(metricDataEntry.getKey().getUserId());
        putMetricData.setNamespace(metricDataEntry.getKey().getNamespace());
        MetricData metricData = new MetricData();
        ArrayList<MetricDatum> metricDatumList = new ArrayList();
        metricDatumList.addAll(metricDataEntry.getValue().subList(i, Math.min(i + MAX_PUT_METRIC_DATA_ITEMS, metricDataEntry.getValue().size())));
        metricData.setMember(metricDatumList);
        putMetricData.setMetricData(metricData);
        retVal.add(putMetricData);
      }
    }
    return retVal;
  }

  private void addToPutMetricDataList(List<PutMetricDataType> putMetricDataList, Supplier<InstanceUsageEvent> cloudWatchSupplier) throws Exception {
    if (cloudWatchSupplier == null) return;
    InstanceUsageEvent event = null;
    event = cloudWatchSupplier.get();
    LOG.trace(event);

    if (!instanceInfoProvider.getInstanceId(event.getInstanceId()).equals(event.getInstanceId())
        || !instanceInfoProvider.getMonitoring(event.getInstanceId())) {
      LOG.trace("Instance : " + event.getInstanceId() + " monitoring is not enabled");
      return;
    }

    if (instanceInfoProvider.getInstanceId(event.getInstanceId()).equals(event.getInstanceId())
        && instanceInfoProvider.getMonitoring(event.getInstanceId())) {

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
          // get autoscaling group name if it exists
          try {
            String autoscalingGroupName = instanceInfoProvider.getAutoscalingGroupName(event.getInstanceId());
            if (autoscalingGroupName != null) {
              Dimension autoscalingGroupNameDim = new Dimension();
              autoscalingGroupNameDim.setName("AutoScalingGroupName");
              autoscalingGroupNameDim.setValue(autoscalingGroupName);
              dimArray.add(autoscalingGroupNameDim);
            }
          } catch (Exception ex) {
            ; // no autoscaling group, don't bother adding
          }
          Dimension instanceIdDim = new Dimension();
          instanceIdDim.setName("InstanceId");
          instanceIdDim.setValue(instanceInfoProvider.getInstanceId(event.getInstanceId()));
          dimArray.add(instanceIdDim);

          Dimension imageIdDim = new Dimension();
          imageIdDim.setName("ImageId");
          imageIdDim.setValue(instanceInfoProvider.getImageId(event.getInstanceId()));
          dimArray.add(imageIdDim);

          Dimension instanceTypeDim = new Dimension();
          instanceTypeDim.setName("InstanceType");
          instanceTypeDim.setValue(instanceInfoProvider.getVmTypeDisplayName(event.getInstanceId()));
          dimArray.add(instanceTypeDim);
          // convert ephemeral disks metrics
          if (UNSUPPORTED_EC2_METRICS.contains(event.getMetric())) {
            return;
          } else {
            metricDatum.setMetricName(event.getMetric());
          }
        }
      } else {
        LOG.debug("Event does not contain a dimension");
        return;
      }

      Dimensions dims = new Dimensions();
      dims.setMember(dimArray);

      MetricData metricData = new MetricData();

      metricDatum.setTimestamp(new Date(event.getValueTimestamp()));
      metricDatum.setDimensions(dims);
      metricDatum.setValue(event.getValue());

      final String unitType = containsUnitType(metricDatum.getMetricName());
      metricDatum.setUnit(unitType);

      if (ABSOLUTE_METRICS.containsKey(metricDatum.getMetricName())) {
        metricDatum.setMetricName(ABSOLUTE_METRICS.get(metricDatum.getMetricName()));
      }
      
      metricData.setMember(Lists.newArrayList(metricDatum));
      putMetricData.setMetricData(metricData);
      putMetricData.setUserId(instanceInfoProvider.getEffectiveUserId(event.getInstanceId()));
      putMetricData.markPrivileged();
      putMetricDataList.add(putMetricData);
    }
  }


}
