/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal.awsusage;

import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.loadbalancing.workflow.LoadBalancingAWSCredentialsProvider;
import com.eucalyptus.reporting.event.AddressEvent;
import com.eucalyptus.reporting.event.CloudWatchApiUsageEvent;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.reporting.event.LoadBalancerEvent;
import com.eucalyptus.reporting.event.S3ObjectEvent;
import com.eucalyptus.reporting.event.SnapShotEvent;
import com.eucalyptus.reporting.event.VolumeEvent;
import com.eucalyptus.resources.client.Ec2Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class QueuedEvents {
  private static final Logger LOG = Logger
          .getLogger(QueuedEvents.class);

  private static final LoadingCache<String, Optional<ReservationInfoType>> instanceCache = CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(3, TimeUnit.MINUTES)
          .build( new CacheLoader<String, Optional<ReservationInfoType>>() {
            @Override
            public Optional<ReservationInfoType> load(String instanceId) throws Exception {
              try {
                final List<ReservationInfoType> results
                        = Ec2Client.getInstance().describeInstanceReservations(null, Lists.newArrayList(instanceId));
                return results.stream().findAny();
              } catch (final Exception ex) {
                ;
              }
              return Optional.empty();
            }
          });

  public static Function<QueuedEvent, String> EventToMessage = (event) -> {
    final ObjectMapper mapper = new ObjectMapper();
    try {
      final String jsonObj = mapper.writeValueAsString(event);
      return new String(Base64.getEncoder().encode(jsonObj.getBytes()));
    }catch (final IOException ex) {
      LOG.debug("Failed to serialize QueuedEvent", ex);
      return null;
    }
  };

  public static Function<String, QueuedEvent> MessageToEvent = (message) -> {
    final ObjectMapper mapper = new ObjectMapper();
    try {
      final String jsonObj = new String(Base64.getDecoder().decode(message.getBytes()));
      final QueuedEvent event =
              mapper.readValue(jsonObj, QueuedEvent.class);
      return event;
    }catch (final IOException ex) {
      LOG.debug("Failed to deserialize QueuedEvent", ex);
      return null;
    }
  };

  public static Function<InstanceUsageEvent, Optional<QueuedEvent>> FromInstanceUsageEvent = (event) -> {
    if ("CPUUtilization".equals(event.getMetric())) {
      final QueuedEvent q = new QueuedEvent();
      q.setEventType("InstanceUsage");
      q.setResourceId(event.getInstanceId());
      try {
        final Optional<ReservationInfoType> reservation =
                instanceCache.get(event.getInstanceId());
        if (reservation.isPresent()) {
          q.setAccountId(reservation.get().getOwnerId());
        }
      } catch (final Exception ex) {
        ;
      }
      q.setUserId(null);
      q.setTimestamp(new Date(System.currentTimeMillis()));
      return Optional.of(q);
    } else {
      return Optional.empty();
    }
  };

  private static final long GigaByte = 1073741824;
  public static Function<VolumeEvent, QueuedEvent> FromVolumeUsageEvent = (event) -> {
    final QueuedEvent q = new QueuedEvent();
    q.setEventType("VolumeUsage");
    q.setResourceId(event.getVolumeId());
    q.setAccountId(event.getAccountNumber());
    q.setUserId(event.getUserId());
    q.setAvailabilityZone(event.getAvailabilityZone());
    q.setUsageValue(String.format("%d", event.getSizeGB() * GigaByte));
    q.setTimestamp( new Date(System.currentTimeMillis()));
    return q;
  };

  public static Function<SnapShotEvent, QueuedEvent> FromSnapshotUsageEvent = (event) -> {
    final QueuedEvent q = new QueuedEvent();
    q.setEventType("SnapshotUsage");
    q.setResourceId(event.getSnapshotId());
    q.setAccountId(event.getAccountNumber());
    q.setUserId(event.getUserId());
    q.setUsageValue(String.format("%d", event.getVolumeSizeGB() * GigaByte));
    q.setTimestamp(new Date(System.currentTimeMillis()));
    return q;
  };

  public static Function<AddressEvent, QueuedEvent> FromAddressUsageEvent = (event) -> {
    final QueuedEvent q = new QueuedEvent();
    q.setEventType("AddressUsage");
    q.setResourceId(event.getAddress());
    q.setAccountId(event.getAccountId());
    q.setUserId(event.getUserId());
    q.setUsageValue(event.getActionInfo().getAction().toString()); // ALLOCATE or ASSOCIATE
    q.setTimestamp(new Date(System.currentTimeMillis()));
    return q;
  };

  public static Function<S3ObjectEvent, QueuedEvent> FromS3ObjectUsageEvent = (event) -> {
    final QueuedEvent q = new QueuedEvent();
    q.setEventType("S3ObjectUsage");
    q.setResourceId(String.format("%s/%s", event.getBucketName(), event.getObjectKey()));
    q.setAccountId(event.getAccountNumber());
    q.setUserId(event.getUserId());
    q.setUsageValue(String.format("%d", event.getSize()));
    q.setTimestamp(new Date(System.currentTimeMillis()));
    return q;
  };

  private static Map<String, Long> instancePublicTransferInLastMeter = Maps.newConcurrentMap();
  private static Map<String, Long> instancePublicTransferOutLastMeter = Maps.newConcurrentMap();
  public static Function<InstanceUsageEvent, Optional<QueuedEvent>> FromPublicIpTransfer = (event) -> {
    if ( !("NetworkInExternal".equals(event.getMetric()) || "NetworkOutExternal".equals(event.getMetric())) )
      return Optional.empty();
    if (event.getDimension() == null || !event.getDimension().equals("default"))
      return Optional.empty();
    if (event.getInstanceId() == null )
      return Optional.empty();

    try{
      final QueuedEvent q = new QueuedEvent();
      final Optional<ReservationInfoType> reservation =
              instanceCache.get(event.getInstanceId());
      if (reservation.isPresent()) {
        q.setAccountId(reservation.get().getOwnerId());
      } else {
        return Optional.empty();
      }
      final RunningInstancesItemType instance = reservation.get().getInstancesSet().stream()
              .filter( i -> event.getInstanceId().equals(i.getInstanceId()))
              .findFirst()
              .get();
      q.setResourceId(event.getInstanceId());
      q.setAvailabilityZone(instance.getPlacement());

      if ("NetworkInExternal".equals(event.getMetric())) {
        final Long oldValue = instancePublicTransferInLastMeter.get(event.getInstanceId());
        if (oldValue == null || oldValue > event.getValue().longValue()) {
          instancePublicTransferInLastMeter.put(event.getInstanceId(),  event.getValue().longValue());
          return Optional.empty();
        } else {
          q.setEventType("InstancePublicIpTransfer-In");
          q.setUsageValue(String.format("%d", event.getValue().longValue() - oldValue));
        }
      } else {
        final Long oldValue = instancePublicTransferOutLastMeter.get(event.getInstanceId());
        if (oldValue == null || oldValue > event.getValue().longValue()) {
          instancePublicTransferOutLastMeter.put(event.getInstanceId(),  event.getValue().longValue());
          return Optional.empty();
        } else {
          q.setEventType("InstancePublicIpTransfer-Out");
          q.setUsageValue(String.format("%d", event.getValue().longValue() - oldValue));
        }
      }
      q.setTimestamp(new Date(System.currentTimeMillis()));
      return Optional.of(q);
    }catch (final Exception ex) {
      return Optional.empty();
    }
  };

  private static Map<String, Long> instanceDataInLastMeter = Maps.newConcurrentMap();
  private static Map<String, Long> instanceDataOutLastMeter = Maps.newConcurrentMap();
  public static Function<InstanceUsageEvent, Optional<QueuedEvent>> FromInstanceDataTransfer = (event) -> {
    if ( !("NetworkIn".equals(event.getMetric()) || "NetworkOut".equals(event.getMetric())) )
      return Optional.empty();
    if (event.getDimension() == null || !event.getDimension().equals("total"))
      return Optional.empty();
    if (event.getInstanceId() == null )
      return Optional.empty();

    try{
      final QueuedEvent q = new QueuedEvent();
      final Optional<ReservationInfoType> reservation =
              instanceCache.get(event.getInstanceId());
      if (reservation.isPresent()) {
        q.setAccountId(reservation.get().getOwnerId());
      } else {
        return Optional.empty();
      }
      final RunningInstancesItemType instance = reservation.get().getInstancesSet().stream()
              .filter( i -> event.getInstanceId().equals(i.getInstanceId()))
              .findFirst()
              .get();
      q.setResourceId(event.getInstanceId());
      q.setAvailabilityZone(instance.getPlacement());

      if ("NetworkIn".equals(event.getMetric())) {
        final Long oldValue = instanceDataInLastMeter.get(event.getInstanceId());
        if (oldValue == null || oldValue > event.getValue().longValue()) {
          instanceDataInLastMeter.put(event.getInstanceId(),  event.getValue().longValue());
          return Optional.empty();
        } else {
          q.setEventType("InstanceDataTransfer-In");
          q.setUsageValue(String.format("%d", event.getValue().longValue() - oldValue));
        }
      } else {
        final Long oldValue = instanceDataOutLastMeter.get(event.getInstanceId());
        if (oldValue == null || oldValue > event.getValue().longValue()) {
          instanceDataOutLastMeter.put(event.getInstanceId(),  event.getValue().longValue());
          return Optional.empty();
        } else {
          q.setEventType("InstanceDataTransfer-Out");
          q.setUsageValue(String.format("%d", event.getValue().longValue() - oldValue));
        }
      }
      q.setTimestamp(new Date(System.currentTimeMillis()));
      return Optional.of(q);
    }catch (final Exception ex) {
      return Optional.empty();
    }
  };

  private static Map<String, Long> volumeReadOpsLastMeter = Maps.newConcurrentMap();
  private static Map<String, Long> volumeWriteOpsLastMeter = Maps.newConcurrentMap();
  public static Function<InstanceUsageEvent, Optional<QueuedEvent>> FromVolumeIoUsage = (event) -> {
    if ( !("DiskWriteOps".equals(event.getMetric()) || "DiskReadOps".equals(event.getMetric())) )
      return Optional.empty();
    if (event.getDimension() == null || !event.getDimension().startsWith("vol-"))
      return Optional.empty();
    if (event.getInstanceId() == null )
      return Optional.empty();

    try {
      final QueuedEvent q = new QueuedEvent();
      final Optional<ReservationInfoType> reservation =
              instanceCache.get(event.getInstanceId());
      if (reservation.isPresent()) {
        q.setAccountId(reservation.get().getOwnerId());
      } else {
        return Optional.empty();
      }

      final RunningInstancesItemType instance = reservation.get().getInstancesSet().stream()
              .filter( i -> event.getInstanceId().equals(i.getInstanceId()))
              .findFirst()
              .get();

      final Optional<String> volumeId = instance.getBlockDevices().stream()
              .filter(dev -> dev.getEbs()!=null && event.getDimension().equals(dev.getEbs().getVolumeId()) )
              .map ( dev -> dev.getEbs().getVolumeId())
              .findAny();
      if (volumeId.isPresent()) {
        q.setResourceId(volumeId.get());
      } else {
        return Optional.empty();
      }

      if ("DiskWriteOps".equals(event.getMetric())) {
        final Long oldValue =
                volumeWriteOpsLastMeter.replace(volumeId.get(), event.getValue().longValue());
        if (oldValue  == null || oldValue > event.getValue().longValue()) {
          volumeWriteOpsLastMeter.put(volumeId.get(), event.getValue().longValue());
          return Optional.empty();
        } else {
          q.setEventType("EBS:VolumeIOUsage-Write");
          q.setUsageValue(String.format("%d", event.getValue().longValue() - oldValue));
        }
      } else {
        final Long oldValue =
                volumeReadOpsLastMeter.replace(volumeId.get(), event.getValue().longValue());
        if (oldValue  == null || oldValue > event.getValue().longValue()) {
          volumeReadOpsLastMeter.put(volumeId.get(), event.getValue().longValue());
          return Optional.empty();
        } else {
          q.setEventType("EBS:VolumeIOUsage-Read");
          q.setUsageValue(String.format("%d", event.getValue().longValue() - oldValue));
        }
      }
      q.setTimestamp(new Date(System.currentTimeMillis()));
      return Optional.of(q);
    }catch (final Exception ex) {
      return Optional.empty();
    }
  };

  // key: instanceId of loadbalancer VM, value: latest network usage bytes
  private static Map<String, Long> loadbalancerDataInLastMeter = Maps.newConcurrentMap();
  private static Map<String, Long> loadbalancerDataOutLastMeter = Maps.newConcurrentMap();
  public static Function<InstanceUsageEvent, Optional<QueuedEvent>> FromLoadBalancerDataTransfer = (event) -> {
    if ( !("NetworkIn".equals(event.getMetric()) || "NetworkOut".equals(event.getMetric())) )
      return Optional.empty();
    if (event.getDimension() == null || !event.getDimension().equals("total"))
      return Optional.empty();
    if (event.getInstanceId() == null )
      return Optional.empty();

    try{
      final QueuedEvent q = new QueuedEvent();
      final Optional<ReservationInfoType> reservation =
              instanceCache.get(event.getInstanceId());
      if (!reservation.isPresent()) {
        return Optional.empty();
      }

      final String instanceOwnerId = reservation.get().getOwnerId();
      if (! LoadBalancingAWSCredentialsProvider.LoadBalancingUserSupplier.INSTANCE.get().getAccountNumber().equals(instanceOwnerId)) {
        return Optional.empty();
      }

      final RunningInstancesItemType instance = reservation.get().getInstancesSet().stream()
              .filter( i -> event.getInstanceId().equals(i.getInstanceId()))
              .findFirst()
              .get();

      if(instance.getIamInstanceProfile() == null ||
              instance.getIamInstanceProfile().getArn() == null )
        return Optional.empty();
      final String arn = instance.getIamInstanceProfile().getArn();
      // arn:aws:iam::000089838020:instance-profile/internal/loadbalancer/loadbalancer-vm-000495165767-httplb02
      if (!arn.startsWith(String.format("arn:aws:iam::%s:instance-profile/internal/loadbalancer", instanceOwnerId)))
        return Optional.empty();
      String[] tokens = arn.split("/");
      final String lbIdentifier = tokens[tokens.length-1].substring("loadbalancer-vm-".length());
      tokens = lbIdentifier.split("-");
      final String lbOwnerId = tokens[0];
      final String lbName = lbIdentifier.substring((lbOwnerId+"-").length());
      q.setAccountId(lbOwnerId);
      q.setResourceId(lbName);

      if ("NetworkIn".equals(event.getMetric())) {
        final Long oldValue = loadbalancerDataInLastMeter.get(event.getInstanceId());
        if (oldValue == null || oldValue > event.getValue().longValue()) {
          loadbalancerDataInLastMeter.put(event.getInstanceId(),  event.getValue().longValue());
          return Optional.empty();
        } else {
          q.setEventType("LoadBalancing-DataTransfer-In");
          q.setUsageValue(String.format("%d", event.getValue().longValue() - oldValue));
        }
      } else {
        final Long oldValue = loadbalancerDataOutLastMeter.get(event.getInstanceId());
        if (oldValue == null || oldValue > event.getValue().longValue()) {
          loadbalancerDataOutLastMeter.put(event.getInstanceId(),  event.getValue().longValue());
          return Optional.empty();
        } else {
          q.setEventType("LoadBalancing-DataTransfer-Out");
          q.setUsageValue(String.format("%d", event.getValue().longValue() - oldValue));
        }
      }
      q.setTimestamp(new Date(System.currentTimeMillis()));
      return Optional.of(q);
    }catch (final Exception ex) {
      return Optional.empty();
    }
  };

  public static Function<LoadBalancerEvent, QueuedEvent> fromLoadBalancerEvent = (event) -> {
    final QueuedEvent q = new QueuedEvent();
    q.setEventType("LoadBalancerUsage");
    q.setResourceId(event.getLoadbalancerName());
    q.setAccountId(event.getAccountNumber());
    q.setUserId(event.getUserId());
    q.setUsageValue("1");
    q.setTimestamp(new Date(System.currentTimeMillis()));
    return q;
  };

  public static Function<CloudWatchApiUsageEvent, QueuedEvent> FromCloudWatchApiUsageEvent = (event) -> {
    final QueuedEvent q = new QueuedEvent();
    q.setEventType("CW:Requests");
    q.setResourceId(event.getOperation());
    q.setAccountId(event.getAccountId());
    q.setUsageValue(String.valueOf(event.getRequestCount()));
    q.setTimestamp(new Date(event.getEndTime()));
    return q;
  };
}
