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
      if (events.size() <= 0)
        return Lists.newArrayList();

      final Map<String, String> instanceTypeMap = Maps.newHashMap();
      for (final String instanceId : events.stream()
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
      for (final String instanceId : events.stream()
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

      final Date latestRecord = events.stream()
              .map(e -> e.getTimestamp())
              .max((a, b) -> a.before(b) ? -1 : 1)
              .get();
      final List<AwsUsageRecord> records = Lists.newArrayList();

      final Date endTime = getNextHour(latestRecord);
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
