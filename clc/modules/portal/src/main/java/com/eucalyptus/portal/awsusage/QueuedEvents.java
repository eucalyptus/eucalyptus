/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
import java.util.function.Predicate;

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
    if ( event.getInstanceId() != null ) {
      q.setAny(event.getInstanceId());
    }
    q.setUsageValue(event.getActionInfo().getAction().toString());
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
  public static Function<InstanceUsageEvent, List<QueuedEvent>> FromPublicIpTransfer = (event) -> {
    if (!("NetworkInExternal".equals(event.getMetric()) || "NetworkOutExternal".equals(event.getMetric())))
      return Lists.newArrayList();
    if (event.getDimension() == null || !event.getDimension().equals("default"))
      return Lists.newArrayList();
    if (event.getInstanceId() == null)
      return Lists.newArrayList();

    Long newValue = null;
    final Map<String, Long> cache = "NetworkInExternal".equals(event.getMetric()) ? instancePublicTransferInLastMeter : instancePublicTransferOutLastMeter;
    try {
      final Long oldValue = cache.get(event.getInstanceId());
      if (oldValue != null) {
        newValue = event.getValue().longValue() - oldValue;
      }
      cache.put(event.getInstanceId(), event.getValue().longValue());
    } catch (final Exception ex) {
      return Lists.newArrayList();
    }

    if (newValue == null || newValue < 0) {
      return Lists.newArrayList();
    }

    final List<QueuedEvent> queueEvents = Lists.newArrayList();
    ReservationInfoType reservation = null;
    RunningInstancesItemType instance = null;
    // InstanceDataTransfer and InstancePublicIpTransfer
    try {
      final Optional<ReservationInfoType> optReservation =
              instanceCache.get(event.getInstanceId());
      if (!optReservation.isPresent()) {
        return Lists.newArrayList();
      }
      reservation = optReservation.get();
      final Optional<RunningInstancesItemType> optInstance = reservation.getInstancesSet().stream()
              .filter(i -> event.getInstanceId().equals(i.getInstanceId()))
              .findFirst();
      if (!optInstance.isPresent()) {
        return Lists.newArrayList();
      }
      instance = optInstance.get();
      final QueuedEvent idt = new QueuedEvent();
      if ("NetworkInExternal".equals(event.getMetric())) {
        idt.setEventType("InstanceDataTransfer-In");
      } else {
        idt.setEventType("InstanceDataTransfer-Out");
      }
      idt.setAccountId(reservation.getOwnerId());
      idt.setResourceId(event.getInstanceId());
      idt.setAvailabilityZone(instance.getPlacement());
      idt.setUsageValue(String.format("%d", newValue));
      idt.setTimestamp(new Date(System.currentTimeMillis()));
      final QueuedEvent ipt = new QueuedEvent(idt);
      if ("NetworkInExternal".equals(event.getMetric())) {
        ipt.setEventType("InstancePublicIpTransfer-In");
      } else {
        ipt.setEventType("InstancePublicIpTransfer-Out");
      }
      queueEvents.add(idt);
      queueEvents.add(ipt);
    } catch (final Exception ex) {
      ;
    }

    // LoadBalancing-DataTransfer
    final Predicate<ReservationInfoType> isLoadbalancer = (rsv) -> {
      try {
        if (!LoadBalancingAWSCredentialsProvider.LoadBalancingUserSupplier.INSTANCE.get()
                .getAccountNumber().equals(rsv.getOwnerId())) {
          return false;
        }
        final RunningInstancesItemType vm = rsv.getInstancesSet().stream()
                .filter(i -> event.getInstanceId().equals(i.getInstanceId()))
                .findFirst()
                .get();
        if (vm.getIamInstanceProfile() == null ||
                vm.getIamInstanceProfile().getArn() == null)
          return false;
        final String arn = vm.getIamInstanceProfile().getArn();
        // arn:aws:iam::000089838020:instance-profile/internal/loadbalancer/loadbalancer-vm-000495165767-httplb02
        if (!arn.startsWith(String.format("arn:aws:iam::%s:instance-profile/internal/loadbalancer", rsv.getOwnerId()))) {
          return false;
        }
        return true;
      } catch (final Exception ex) {
        return false;
      }
    };
    try {
      if (isLoadbalancer.test(reservation)) {
        final String arn = instance.getIamInstanceProfile().getArn();
        // arn:aws:iam::000089838020:instance-profile/internal/loadbalancer/loadbalancer-vm-000495165767-httplb02
        String[] tokens = arn.split("/");
        final String lbIdentifier = tokens[tokens.length-1].substring("loadbalancer-vm-".length());
        tokens = lbIdentifier.split("-");
        final String lbOwnerId = tokens[0];
        final String lbName = lbIdentifier.substring((lbOwnerId+"-").length());
        final QueuedEvent lbe = new QueuedEvent(queueEvents.get(0)); // copy constructor
        lbe.setAccountId(lbOwnerId);
        lbe.setResourceId(lbName);
        if ("NetworkInExternal".equals(event.getMetric())) {
          lbe.setEventType("LoadBalancing-DataTransfer-In");
        } else {
          lbe.setEventType("LoadBalancing-DataTransfer-Out");
        }
        queueEvents.add(lbe);
      }
    } catch (final Exception ex) {
      ;
    }
    return queueEvents;
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
