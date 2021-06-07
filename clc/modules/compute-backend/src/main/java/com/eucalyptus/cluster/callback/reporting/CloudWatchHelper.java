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
package com.eucalyptus.cluster.callback.reporting;

import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet.TORNDOWN;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;

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
import com.eucalyptus.compute.common.internal.vm.VmRuntimeState;
import com.eucalyptus.entities.EntityCache;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstanceTag;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import com.eucalyptus.cluster.common.msgs.DescribeSensorsResponseType;
import com.eucalyptus.cluster.common.msgs.MetricCounterType;
import com.eucalyptus.cluster.common.msgs.MetricDimensionsType;
import com.eucalyptus.cluster.common.msgs.MetricDimensionsValuesType;
import com.eucalyptus.cluster.common.msgs.MetricsResourceType;
import com.eucalyptus.cluster.common.msgs.SensorsResourceType;

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

  private static final Map<String,String> metricsToUnitTypes = new ImmutableMap.Builder<String, String>()
      .putAll( metricsToUnitType( Bytes.class ) )
      .putAll( metricsToUnitType( Count.class ) )
      .putAll( metricsToUnitType( Seconds.class ) )
      .putAll( metricsToUnitType( Percent.class ) )
      .build();

  private static <E extends Enum<E>> Map<String,String> metricsToUnitType( final Class<E> unitEnum ) {
    return CollectionUtils.putAll(
        EnumSet.allOf( unitEnum ),
        Maps.<String,String>newHashMap( ),
        Functions.toStringFunction( ),
        Functions.constant( unitEnum.getSimpleName( ) ) );
  }

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
    StatusCheckFailed_Instance,
    StatusCheckFailed_System,
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

  private String containsUnitType( final String metricType ) {
    final String unitType = metricsToUnitTypes.get( metricType );
    if ( unitType == null ) {
      throw new NoSuchElementException(
          "Unknown system unit type : " + metricType);
    }
    return unitType;
  }

  public static ServiceConfiguration createServiceConfiguration() {
    return Topology.lookup( CloudWatch.class );
  }

  public void sendSystemMetric(ServiceConfiguration serviceConfiguration, PutMetricDataType putMetricData) throws Exception {
    BaseMessage reply = AsyncRequests.dispatch(serviceConfiguration, putMetricData).get();
    if (!(reply instanceof PutMetricDataResponseType)) {
      throw new EucalyptusCloudException("Unable to send put metric data to cloud watch");
    }
  }

  public interface InstanceInfoProvider {
    String getAutoscalingGroupName(String instanceId);
    String getInstanceId(String instanceId);
    String getImageId(String instanceId);
    String getVmTypeDisplayName(String instanceId);
    String getAccountNumber(String instanceId);
    Integer getStatusCheckFailed(String instanceId);
    Integer getInstanceStatusCheckFailed(String instanceId);
    Integer getSystemStatusCheckFailed(String instanceId);
    boolean getMonitoring(String instanceId);
  }

  public static class DefaultInstanceInfoProvider implements InstanceInfoProvider {

    private static final EntityCache<VmInstance,CloudWatchInstanceInfo> instanceInfoCache =
        new EntityCache<>(
            VmInstance.named(null),
            Restrictions.not( VmInstance.criterion( TORNDOWN.array( ) ) ),
            Sets.newHashSet( "bootRecord.vmType" ),
            Sets.newHashSet( "networkGroups","bootRecord.machineImage" ),
            TypeMappers.lookup( VmInstance.class, CloudWatchInstanceInfo.class ) );

    private static final EntityCache<VmInstanceTag,CloudWatchInstanceGroupInfo> instanceGroupInfoCache =
        new EntityCache<>(
            VmInstanceTag.key( "aws:autoscaling:groupName" ),
            TypeMappers.lookup( VmInstanceTag.class, CloudWatchInstanceGroupInfo.class ) );


    private static final AtomicReference<Map<String,CloudWatchInstanceInfo>> instances =
        new AtomicReference<>( Collections.<String,CloudWatchInstanceInfo>emptyMap( ) );

    private static final AtomicReference<Map<String,CloudWatchInstanceGroupInfo>> instanceGroups =
        new AtomicReference<>( Collections.<String,CloudWatchInstanceGroupInfo>emptyMap( ) );

    @TypeMapper
    public enum VmInstanceToCloudWatchInstanceInfo implements Function<VmInstance,CloudWatchInstanceInfo> {
      INSTANCE;

      @Override
      public CloudWatchInstanceInfo apply( final VmInstance instance ) {
        return new CloudWatchInstanceInfo(
            instance.getInstanceId( ),
            instance.getBootRecord( ).getDisplayMachineImageId( ),
            instance.getBootRecord( ).getVmType( ).getDisplayName( ),
            instance.getOwnerAccountNumber( ),
            instance.getRuntimeState( ).getInstanceStatus( ) == VmRuntimeState.InstanceStatus.Ok ? 0 : 1,
            MoreObjects.firstNonNull( instance.getMonitoring( ), Boolean.FALSE )
        );
      }
    }

    @TypeMapper
    public enum VmInstanceTagToCloudWatchInstanceGroupInfo implements Function<VmInstanceTag,CloudWatchInstanceGroupInfo> {
      INSTANCE;

      @Override
      public CloudWatchInstanceGroupInfo apply( final VmInstanceTag instanceTag ) {
        return new CloudWatchInstanceGroupInfo(
            instanceTag.getResourceId( ),
            instanceTag.getValue( )
        );
      }
    }

    private static class CloudWatchInstanceInfo implements HasName<CloudWatchInstanceInfo> {
      private final String instanceId;
      private final String imageId;
      private final String vmTypeDisplayName;
      private final String accountId;
      private final Integer systemStatusCheckFailed; //
      private final boolean monitoring;

      public CloudWatchInstanceInfo(
          final String instanceId,
          final String imageId,
          final String vmTypeDisplayName,
          final String accountId,
          final Integer systemStatusCheckFailed,
          final boolean monitoring
      ) {
        this.instanceId = instanceId;
        this.imageId = imageId;
        this.vmTypeDisplayName = vmTypeDisplayName;
        this.accountId = accountId;
        this.systemStatusCheckFailed = systemStatusCheckFailed;
        this.monitoring = monitoring;
      }

      public String getInstanceId( ) {
        return instanceId;
      }

      public String getImageId( ) {
        return imageId;
      }

      public String getVmTypeDisplayName( ) {
        return vmTypeDisplayName;
      }

      public String getAccountId( ) {
        return accountId;
      }

      public Integer getSystemStatusCheckFailed( ) {
        return systemStatusCheckFailed;
      }

      public boolean getMonitoring( ) {
        return monitoring;
      }

      @Override
      public String getName() {
        return getInstanceId( );
      }

      @Override
      public int compareTo( final CloudWatchInstanceInfo o ) {
        return getInstanceId( ).compareTo( o.getInstanceId( ) );
      }
    }

    private static class CloudWatchInstanceGroupInfo implements HasName<CloudWatchInstanceGroupInfo> {
      private final String instanceId;
      private final String autoScalingGroup;

      public CloudWatchInstanceGroupInfo(
          final String instanceId,
          final String autoScalingGroup
      ) {
        this.instanceId = instanceId;
        this.autoScalingGroup = autoScalingGroup;
      }

      public String getAutoScalingGroup( ) {
        return autoScalingGroup;
      }

      public String getInstanceId( ) {
        return instanceId;
      }

      @Override
      public String getName() {
        return getInstanceId( );
      }

      @Override
      public int compareTo( final CloudWatchInstanceGroupInfo o ) {
        return getInstanceId( ).compareTo( o.getInstanceId( ) );
      }
    }

    public static synchronized void refresh( ) {
      instances.set( ImmutableMap.copyOf( CollectionUtils.putAll(
          instanceInfoCache.get( ),
          Maps.<String,CloudWatchInstanceInfo>newHashMap( ),
          HasName.GET_NAME,
          Functions.<CloudWatchInstanceInfo>identity( ) ) ) );
      instanceGroups.set( ImmutableMap.copyOf( CollectionUtils.putAll(
          instanceGroupInfoCache.get( ),
          Maps.<String,CloudWatchInstanceGroupInfo>newHashMap( ),
          HasName.GET_NAME,
          Functions.<CloudWatchInstanceGroupInfo>identity( ) ) ) );
    }

    private CloudWatchInstanceInfo lookupInstance(String instanceId) {
      final CloudWatchInstanceInfo instance = instances.get( ).get( instanceId );
      if ( instance == null ) {
        throw new NoSuchElementException( instanceId );
      }
      return instance;
    }

    private CloudWatchInstanceGroupInfo lookupInstanceGroup(String instanceId) {
      final CloudWatchInstanceGroupInfo instanceGroupInfo = instanceGroups.get( ).get( instanceId );
      if ( instanceGroupInfo == null ) {
        throw new NoSuchElementException( instanceId );
      }
      return instanceGroupInfo;
    }

    @Override
    public String getAutoscalingGroupName( String instanceId ) {
      return lookupInstanceGroup( instanceId ).getAutoScalingGroup( );
    }

    @Override
    public String getInstanceId(String instanceId) {
      return lookupInstance( instanceId ).getInstanceId( );
    }

    @Override
    public String getImageId( String instanceId ) {
      return lookupInstance( instanceId ).getImageId( );
    }

    @Override
    public String getVmTypeDisplayName(String instanceId) {
      return lookupInstance( instanceId ).getVmTypeDisplayName( );
    }

    @Override
    public String getAccountNumber(String instanceId) {
      return lookupInstance( instanceId ).getAccountId( );
    }

    @Override
    public Integer getStatusCheckFailed( final String instanceId ) {
      return getSystemStatusCheckFailed( instanceId );
    }

    @Override
    public Integer getInstanceStatusCheckFailed( final String instanceId ) {
      return 0;
    }

    @Override
    public Integer getSystemStatusCheckFailed(String instanceId) {
      return lookupInstance( instanceId ).getSystemStatusCheckFailed( );
    }

    @Override
    public boolean getMonitoring(String instanceId) {
      return lookupInstance( instanceId ).getMonitoring( );
    }
  }

  public List<AbsoluteMetricQueueItem> collectMetricData(
      final Collection<String> expectedInstanceIds,
      final DescribeSensorsResponseType msg
  ) throws Exception {
    ArrayList<AbsoluteMetricQueueItem> absoluteMetricQueueItems = new ArrayList<>();

    // cloudwatch metric caches
    final ConcurrentMap<String, DiskReadWriteMetricTypeCache> metricCacheMap = Maps.newConcurrentMap();
    
    final EC2DiskMetricCache ec2DiskMetricCache = new EC2DiskMetricCache();

    for (final SensorsResourceType sensorData : msg.getSensorsResources()) {
      if (!RESOURCE_TYPE_INSTANCE.equals(sensorData.getResourceType()) ||
          !expectedInstanceIds.contains( sensorData.getResourceName()) ||
          sensorData.getResourceUuid() == null || 
          sensorData.getResourceUuid().isEmpty())
        continue;
      
      for (final MetricsResourceType metricType : sensorData.getMetrics()) {
        for (final MetricCounterType counterType : metricType.getCounters()) {
          for (final MetricDimensionsType dimensionType : counterType.getDimensions()) {
            // find and fire most recent value for metric/dimension
            final List<MetricDimensionsValuesType> values =
                Lists.newArrayList(stripMilliseconds(dimensionType.getValues()));

            //CloudWatch use case of metric data
            // best to enter older data first...
            Collections.sort(values, Ordering.natural().onResultOf(GetTimestamp.INSTANCE));
            for ( final MetricDimensionsValuesType value : values ) {
              if ( LOG.isTraceEnabled( ) ) {
                LOG.trace("ResourceUUID: " + sensorData.getResourceUuid());
                LOG.trace("ResourceName: " + sensorData.getResourceName());
                LOG.trace("Metric: " + metricType.getMetricName());
                LOG.trace("Dimension: " + dimensionType.getDimensionName());
                LOG.trace("Timestamp: " + value.getTimestamp());
                LOG.trace("Value: " + value.getValue());
              }
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
                addToQueueItems(absoluteMetricQueueItems,
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
                  addToQueueItems(absoluteMetricQueueItems, combineReadWriteDiskMetric("DiskReadOps", "DiskWriteOps", metricCacheMap, "DiskConsumedReadWriteOps", metricType, sensorData, dimensionType, value));
                  addToQueueItems(absoluteMetricQueueItems, combineReadWriteDiskMetric("VolumeReadOps", "VolumeWriteOps", metricCacheMap, "VolumeConsumedReadWriteOps", metricType, sensorData, dimensionType, value));

                  // Also need VolumeTotalReadWriteTime to compute VolumeIdleTime
                  addToQueueItems(absoluteMetricQueueItems, combineReadWriteDiskMetric("VolumeTotalReadTime", "VolumeTotalWriteTime", metricCacheMap, "VolumeTotalReadWriteTime", metricType, sensorData, dimensionType, value));
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

      if ( Iterables.tryFind( absoluteMetricQueueItems, withMetric( "AWS/EC2", null, "InstanceId", sensorData.getResourceName( ) ) ).isPresent( ) &&
          !Iterables.tryFind( absoluteMetricQueueItems, withMetric( "AWS/EC2", Count.StatusCheckFailed.name( ), "InstanceId", sensorData.getResourceName( ) ) ).isPresent( ) ) {
                absoluteMetricQueueItems.addAll(buildInstanceStatusPut(sensorData.getResourceName()));
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
        addToQueueItems(absoluteMetricQueueItems, ec2DiskMetric);
      } catch (Exception ex) {
        LOG.debug("Unable to add system metric " +ec2DiskMetric, ex);
      }
    }
    return absoluteMetricQueueItems;
  }  

  private List<MetricDimensionsValuesType> stripMilliseconds(ArrayList<MetricDimensionsValuesType> values) {
    List<MetricDimensionsValuesType> newValues = new ArrayList<>();
    for (MetricDimensionsValuesType value: values) {
      MetricDimensionsValuesType newValue = new MetricDimensionsValuesType();
      // round down to the lowest second
      newValue.setTimestamp(value.getTimestamp() != null ? new Date((value.getTimestamp().getTime() / 1000L) * 1000L) : null);
      newValue.setValue(value.getValue());
      newValues.add(newValue);
    }
    return newValues;
  }

  public static List<PutMetricDataType> consolidatePutMetricDataList( final List<PutMetricDataType> putMetricDataList ) {
    final int MAX_PUT_METRIC_DATA_ITEMS = 20;
    final LinkedHashMap<Pair<String,String>, List<MetricDatum>> metricDataMap = new LinkedHashMap<>();
    for ( final PutMetricDataType putMetricData : putMetricDataList) {
      final Pair<String,String> userIdAndNamespacePair =
          Pair.pair( putMetricData.getUserId( ), putMetricData.getNamespace( ) );
      if ( !metricDataMap.containsKey( userIdAndNamespacePair ) ) {
        metricDataMap.put( userIdAndNamespacePair, new ArrayList<MetricDatum>( ) );
      }
      metricDataMap.get( userIdAndNamespacePair ).addAll( putMetricData.getMetricData( ).getMember( )) ;
    }
    final ArrayList<PutMetricDataType> retVal = new ArrayList<>();
    for ( final Map.Entry<Pair<String,String>, List<MetricDatum>> metricDataEntry : metricDataMap.entrySet( ) ) {
      for ( final List<MetricDatum> datums : Iterables.partition( metricDataEntry.getValue( ), MAX_PUT_METRIC_DATA_ITEMS ) ) {
        final MetricData metricData = new MetricData( );
        metricData.setMember( Lists.newArrayList( datums ) );
        final PutMetricDataType putMetricData = new PutMetricDataType( );
        putMetricData.setUserId( metricDataEntry.getKey( ).getLeft( ) );
        putMetricData.markPrivileged( );
        putMetricData.setNamespace(metricDataEntry.getKey( ).getRight( ) );
        putMetricData.setMetricData(metricData);
        retVal.add(putMetricData);
      }
    }
    return retVal;
  }

  private void addToQueueItems(List<AbsoluteMetricQueueItem> queueItems, Supplier<InstanceUsageEvent> cloudWatchSupplier) throws Exception {
    if (cloudWatchSupplier == null) return;
    final InstanceUsageEvent event = cloudWatchSupplier.get();
    LOG.trace(event);

    if (!instanceInfoProvider.getInstanceId(event.getInstanceId()).equals(event.getInstanceId())
        || !instanceInfoProvider.getMonitoring(event.getInstanceId())) {
      LOG.trace("Instance : " + event.getInstanceId() + " monitoring is not enabled");
      return;
    }

    if (instanceInfoProvider.getInstanceId(event.getInstanceId()).equals(event.getInstanceId())
        && instanceInfoProvider.getMonitoring(event.getInstanceId())) {

      AbsoluteMetricQueueItem newQueueItem = new AbsoluteMetricQueueItem();
      MetricDatum metricDatum = new MetricDatum();
      ArrayList<Dimension> dimArray = Lists.newArrayList();

      if (event.getDimension() != null && event.getValue() != null) {

        if (event.getDimension().startsWith("vol-")) {
          newQueueItem.setNamespace("AWS/EBS");
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
          newQueueItem.setNamespace("AWS/EC2");
          populateInstanceDimensions( event.getInstanceId( ), dimArray );

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

      metricDatum.setTimestamp(new Date(event.getValueTimestamp()));
      metricDatum.setDimensions(dims);
      metricDatum.setValue(event.getValue());

      final String unitType = containsUnitType(metricDatum.getMetricName());
      metricDatum.setUnit(unitType);

      if (ABSOLUTE_METRICS.containsKey(metricDatum.getMetricName())) {
        metricDatum.setMetricName(ABSOLUTE_METRICS.get(metricDatum.getMetricName()));
      }
      
      newQueueItem.setMetricDatum(metricDatum);
      newQueueItem.setAccountId(instanceInfoProvider.getAccountNumber( event.getInstanceId( ) ));
      queueItems.add(newQueueItem);
    }
  }

  private void populateInstanceDimensions( final String instanceId, final ArrayList<Dimension> dimArray ) {
    // get autoscaling group name if it exists
    try {
      String autoscalingGroupName = instanceInfoProvider.getAutoscalingGroupName( instanceId );
      if (autoscalingGroupName != null) {
        Dimension autoscalingGroupNameDim = new Dimension();
        autoscalingGroupNameDim.setName("AutoScalingGroupName");
        autoscalingGroupNameDim.setValue(autoscalingGroupName);
        dimArray.add(autoscalingGroupNameDim);
      }
    } catch (Exception ex) {
      // no autoscaling group, don't bother adding
    }
    Dimension instanceIdDim = new Dimension();
    instanceIdDim.setName("InstanceId");
    instanceIdDim.setValue(instanceInfoProvider.getInstanceId( instanceId ));
    dimArray.add(instanceIdDim);

    Dimension imageIdDim = new Dimension();
    imageIdDim.setName("ImageId");
    imageIdDim.setValue(instanceInfoProvider.getImageId( instanceId ));
    dimArray.add(imageIdDim);

    Dimension instanceTypeDim = new Dimension();
    instanceTypeDim.setName("InstanceType");
    instanceTypeDim.setValue(instanceInfoProvider.getVmTypeDisplayName( instanceId ));
    dimArray.add(instanceTypeDim);
  }

  private List<AbsoluteMetricQueueItem> buildInstanceStatusPut( final String instanceId ) throws Exception {
    final List<Pair<String,Double>> instanceStatusDatums = ImmutableList.<Pair<String,Double>>builder()
        .add( Pair.pair(
            Count.StatusCheckFailed.name(),
            instanceInfoProvider.getStatusCheckFailed( instanceId ).doubleValue() ) )
        .add( Pair.pair(
            Count.StatusCheckFailed_Instance.name(),
            instanceInfoProvider.getInstanceStatusCheckFailed( instanceId ).doubleValue() ) )
        .add( Pair.pair(
            Count.StatusCheckFailed_System.name(),
            instanceInfoProvider.getSystemStatusCheckFailed( instanceId ).doubleValue() ) )
        .build( );

    final ArrayList<Dimension> dimArray = Lists.newArrayList( );
    populateInstanceDimensions( instanceId, dimArray );
    final Dimensions dimensions = new Dimensions();
    dimensions.setMember( dimArray );

    final List<AbsoluteMetricQueueItem> queueItems = Lists.newArrayList();
    for ( final Pair<String,Double> datum : instanceStatusDatums ) {
      final MetricDatum metricDatum = new MetricDatum( );
      metricDatum.setMetricName(datum.getLeft());
      metricDatum.setDimensions(dimensions);
      metricDatum.setTimestamp(new Date());
      metricDatum.setValue(datum.getRight());
      metricDatum.setUnit( Count.class.getSimpleName() );
      final AbsoluteMetricQueueItem queueItem = new AbsoluteMetricQueueItem( );
      queueItem.setNamespace("AWS/EC2");
      queueItem.setMetricDatum(metricDatum);
      queueItem.setAccountId(instanceInfoProvider.getAccountNumber(instanceId));
      queueItems.add(queueItem );
    }
    return queueItems;
  }

  private static Predicate<AbsoluteMetricQueueItem> withMetric( final String namespace,
                                                                final String name,
                                                                final String dimensionName,
                                                                final String dimensionValue ) {
    return new Predicate<AbsoluteMetricQueueItem>( ) {
      private final Predicate<MetricDatum> metricDatumPredicate = Predicates.and(
        name == null ?
          Predicates.<MetricDatum>alwaysTrue( ) :
          withMetric( name ),
        withMetricDimension( dimensionName, dimensionValue )
      );

      @Override
      public boolean apply( @Nullable final AbsoluteMetricQueueItem queueItem ) {
        return queueItem != null &&
          namespace.equals( queueItem.getNamespace( ) ) &&
          queueItem.getMetricDatum() != null &&
          metricDatumPredicate.apply( queueItem.getMetricDatum());
      }
    };
  }

  private static Predicate<MetricDatum> withMetric( final String name ) {
    return new Predicate<MetricDatum>( ) {
      @Override
      public boolean apply( @Nullable final MetricDatum metricDatum ) {
        return metricDatum != null &&
            name.equals( metricDatum.getMetricName( ) );
      }
    };
  }

  private static Predicate<MetricDatum> withMetricDimension( final String dimensionName,
                                                             final String dimensionValue ) {
    return new Predicate<MetricDatum>( ) {
      private final Predicate<Dimension> dimensionPredicate = withDimension( dimensionName, dimensionValue );

      @Override
      public boolean apply( @Nullable final MetricDatum metricDatum ) {
        return metricDatum != null &&
            metricDatum.getDimensions( ) != null &&
            metricDatum.getDimensions( ).getMember( ) != null &&
            Iterables.tryFind( metricDatum.getDimensions( ).getMember( ), dimensionPredicate ).isPresent( );
      }
    };
  }


  private static Predicate<Dimension> withDimension( final String dimensionName,
                                                     final String dimensionValue ) {
    return new Predicate<Dimension>( ) {
      @Override
      public boolean apply( @Nullable final Dimension dimension ) {
        return dimension != null &&
            dimensionName.equals( dimension.getName( ) ) &&
            dimensionValue.equals( dimension.getValue( ) );
      }
    };
  }
}
