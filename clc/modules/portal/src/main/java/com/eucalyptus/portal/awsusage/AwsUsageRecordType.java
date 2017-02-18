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

import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.resources.client.Ec2Client;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum AwsUsageRecordType implements AwsUsageRecordTypeReader {
  UNKNOWN(null, null, null) {
    @Override
    public List<AwsUsageRecord> read(final String accountId, final List<QueuedEvent> events) {
      return Lists.newArrayList();
    }
  },

  EC2_RUNINSTANCE_BOX_USAGE("AmazonEC2", "RunInstances", "BoxUsage") {
    @Override
    public List<AwsUsageRecord> read(final String accountId, final List<QueuedEvent> events) {
      // generate BoxUsage per instance types
      final List<QueuedEvent> instanceEvents = events.stream()
              .filter(e -> "InstanceUsage".equals(e.getEventType()))
              .collect(Collectors.toList());
      if (instanceEvents.size() <= 0)
        return Lists.newArrayList();

      final Map<String, String> instanceTypeMap = Maps.newHashMap();
      for (final String instanceId : instanceEvents.stream()
              .map(e -> e.getResourceId() )
              .distinct()
              .collect(Collectors.toList())) {
        try {
          final Optional<RunningInstancesItemType> instance =
                  Ec2Client.getInstance().describeInstances(null, Lists.newArrayList(instanceId)).stream()
                  .findFirst();
          if (instance.isPresent()) {
            instanceTypeMap.put(instance.get().getInstanceId(), instance.get().getInstanceType());
          }
        } catch (final Exception ex) {
          ;
        }
      }

      final Map<String, Integer> usagePerInstanceType = Maps.newHashMap();
      for (final String instanceId : instanceEvents.stream()
              .map(e -> e.getResourceId() )
              .distinct()
              .collect(Collectors.toList())) {
        if (instanceTypeMap.containsKey(instanceId)) {
          final String instanceType = instanceTypeMap.get(instanceId);
          if (!usagePerInstanceType.containsKey(instanceType)) {
            usagePerInstanceType.put(instanceType, 0);
          }
          usagePerInstanceType.put(instanceType,
                  usagePerInstanceType.get(instanceType) + 1);
        }
      }

      final Date earliestRecord = AwsUsageRecordType.getEarliest(instanceEvents);
      final List<AwsUsageRecord> records = Lists.newArrayList();

      final Date endTime = getNextHour(earliestRecord);
      final Date startTime = getPreviousHour(endTime);
      for (final String instanceType : usagePerInstanceType.keySet()) {
        final Integer usageValue = usagePerInstanceType.get(instanceType);
        final AwsUsageRecord data = AwsUsageRecords.getInstance().newRecord(accountId)
                .withService("AmazonEC2")
                .withOperation("RunInstances")
                .withResource(null)
                .withUsageType(String.format("BoxUsage:%s", instanceType))
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withUsageValue(usageValue.toString())
                .build();
        records.add(data);
      }
      return records;
    }
  },
  EC2_CREATEVOLUME_VOLUME_USAGE("AmazonEC2", "CreateVolume", "VolumeUsage") {
    @Override
    public List<AwsUsageRecord> read ( final String accountId, final List<QueuedEvent> events){
      List<QueuedEvent> volumeEvents = events.stream()
              .filter(e -> "VolumeUsage".equals(e.getEventType()))
              .collect(Collectors.toList());
      if (volumeEvents.size() <= 0)
        return Lists.newArrayList();

      volumeEvents = AwsUsageRecordType.distinctByResourceIds(volumeEvents);
      // AmazonEC2,CreateVolume,USW2-EBS:VolumeUsage,,11/01/16 04:00:00,11/01/16 05:00:00,8589934592
      final List<AwsUsageRecord> records = Lists.newArrayList();
      final Date earliestRecord = AwsUsageRecordType.getEarliest(volumeEvents);
      final Date endTime = getNextHour(earliestRecord);
      final Date startTime = getPreviousHour(endTime);

      final Optional<Long> value = volumeEvents.stream()
              .map( e -> Long.parseLong(e.getUsageValue()) )
              .reduce( (l1, l2) -> l1+l2 );
      if (value.isPresent()) {
        final AwsUsageRecord data = AwsUsageRecords.getInstance().newRecord(accountId)
                .withService("AmazonEC2")
                .withOperation("CreateVolume")
                .withResource(null)
                .withUsageType("EBS:VolumeUsage")
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withUsageValue(value.get().toString())
                .build();
        records.add(data);
      }
      return records;
    }
  },
  EC2_CREATESNAPSHOT_SNAPSHOT_USAGE("AmazonEC2", "CreateSnapshot", "SnapshotUsage") {
    @Override
    public List<AwsUsageRecord> read(String accountId, List<QueuedEvent> events) {
      List<QueuedEvent> snapshotEvents = events.stream()
              .filter(e -> "SnapshotUsage".equals(e.getEventType()))
              .collect(Collectors.toList());
      if (snapshotEvents.size() <= 0)
        return Lists.newArrayList();

      snapshotEvents = AwsUsageRecordType.distinctByResourceIds(snapshotEvents);
     // AmazonEC2,CreateSnapshot,EBS:SnapshotUsage,,11/04/16 22:00:00,11/04/16 23:00:00,9423844728
      final List<AwsUsageRecord> records = Lists.newArrayList();
      final Date earliestRecord = AwsUsageRecordType.getEarliest(snapshotEvents);
      final Date endTime = getNextHour(earliestRecord);
      final Date startTime = getPreviousHour(endTime);

      final Optional<Long> value = snapshotEvents.stream()
              .map( e -> Long.parseLong(e.getUsageValue()) )
              .reduce( (l1, l2) -> l1+l2 );
      if (value.isPresent()) {
        final AwsUsageRecord data = AwsUsageRecords.getInstance().newRecord(accountId)
                .withService("AmazonEC2")
                .withOperation("CreateSnapshot")
                .withResource(null)
                .withUsageType("EBS:SnapshotUsage")
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withUsageValue(value.get().toString())
                .build();
        records.add(data);
      }
      return records;
    }
  },
  EC2_ASSOCIATEADDRESS_ELASTIC_IP("AmazonEC2", "AssociateAddress", "ElasticIP") {
    @Override
    public List<AwsUsageRecord> read(String accountId, List<QueuedEvent> events) {
      // AmazonEC2,AssociateAddress,USW2-ElasticIP:IdleAddress,,11/15/16 14:00:00,11/15/16 15:00:00,1
      List<QueuedEvent> addressEvents = events.stream()
              .filter(e -> "AddressUsage".equals(e.getEventType()))
              .collect(Collectors.toList());
      if (addressEvents.size() <= 0)
        return Lists.newArrayList();

      addressEvents = AwsUsageRecordType.distinctByResourceIds(addressEvents);
      final List<AwsUsageRecord> records = Lists.newArrayList();
      final Date earliestRecord = AwsUsageRecordType.getEarliest(addressEvents);
      final Date endTime = getNextHour(earliestRecord);
      final Date startTime = getPreviousHour(endTime);
      final long countAddresses = addressEvents.stream()
              .count();
      if (countAddresses > 0) {
        /// TODO: Group by IdleAddress and AssociateAddress?
        final AwsUsageRecord data = AwsUsageRecords.getInstance().newRecord(accountId)
                .withService("AmazonEC2")
                .withOperation("AssociateAddress")
                .withResource(null)
                .withUsageType("ElasticIP:IdleAddress")
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withUsageValue(String.format("%d", countAddresses))
                .build();
        records.add(data);
      }
      return records;
    }
  };

  private String service = null;
  private String operation = null;
  private String usageType = null;
  AwsUsageRecordType(final String service, final String operation, final String usageType) {
    this.service = service;
    this.operation = operation;
    this.usageType = usageType;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    if (service !=null)
      sb.append(String.format("%s:", service));
    else
      sb.append(":");
    if (operation !=null)
      sb.append(String.format("%s:", operation));
    else
      sb.append(":");
    if (usageType != null)
      sb.append(String.format("%s", usageType));
    return sb.toString();
  }

  public static AwsUsageRecordType forValue(final String value) {
    final String[] tokens = value.split(":");
    if (tokens==null || tokens.length != 3) {
      return UNKNOWN;
    }
    final String vService = tokens[0];
    final String vOperation = tokens[1];
    final String vUsageType = tokens[2];
    for (final AwsUsageRecordType type : AwsUsageRecordType.values()) {
      if (type.service == null) {
        if (vService!=null)
          continue;
      } else if (!type.service.equals(vService)) {
          continue;
      }

      if(type.operation == null) {
        if (vOperation != null)
          continue;
      } else if(!type.operation.equals(vOperation)) {
          continue;
      }

      if(type.usageType == null) {
        if(vUsageType != null)
          continue;
      } else if(!type.usageType.equals(vUsageType)) {
        continue;
      }

      return type;
    }

    return UNKNOWN;
  }

  private static List<QueuedEvent> distinctByResourceIds(final List<QueuedEvent> events) {
    final Map<String, QueuedEvent> uniqueEvents = Maps.newHashMap();
    events.stream().forEach( evt -> uniqueEvents.put(evt.getResourceId(), evt) );
    return Lists.newArrayList(uniqueEvents.values());
  }

  private static Date getEarliest(final  List<QueuedEvent> events) {
    final Date earliestRecord = events.stream()
            .map( e -> e.getTimestamp())
            .min((a, b) -> a.before(b) ? -1 : 1)
            .get();
    return earliestRecord;
  }
  private static Date getNextHour(final Date time) {
    final Calendar c = Calendar.getInstance();
    c.setTime(time);
    c.set(Calendar.HOUR, c.get(Calendar.HOUR) + 1);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  private static Date getPreviousHour(final Date time) {
    final Calendar c = Calendar.getInstance();
    c.setTime(time);
    c.set(Calendar.HOUR, c.get(Calendar.HOUR) - 1);
    return c.getTime();
  }
}
