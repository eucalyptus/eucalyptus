/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.portal.instanceusage;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.portal.awsusage.QueuedEvent;
import com.eucalyptus.resources.client.Ec2Client;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import java.util.AbstractMap.SimpleEntry;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public enum InstanceLogReaders implements InstanceLogReader {
  INSTANCE_TYPE {
    @Override
    public InstanceLogs.InstanceLogBuilder read(final QueuedEvent event) {
      final Optional<RunningInstancesItemType> instance = getInstance(event);
      if (instance.isPresent()) {
        return prototype(event)
                .withInstanceType( instance.get().getInstanceType() );
      } else {
        return empty(event);
      }
    }
  },
  PLATFORM {
    @Override
    public InstanceLogs.InstanceLogBuilder read(final QueuedEvent event) {
      final Optional<RunningInstancesItemType> optInstance = getInstance(event);
      if (optInstance.isPresent()) {
        final RunningInstancesItemType instance = optInstance.get();
        String platform = "linux";
        if (instance.getPlatform()!=null && !instance.getPlatform().isEmpty()) {
          platform = instance.getPlatform().toLowerCase();
        }
        return prototype(event)
                .withPlatform(platform);
      } else {
        return empty(event);
      }
    }
  },
  AVAILABILITY_ZONE {
    @Override
    public InstanceLogs.InstanceLogBuilder read(final QueuedEvent event) {
      final Optional<RunningInstancesItemType> instance = getInstance(event);
      if (instance.isPresent()) {
        return prototype(event)
                .withAvailabilityZone(instance.get().getPlacement());
      } else {
        return empty(event);
      }
    }
  },
  TAG {
    @Override
    public InstanceLogs.InstanceLogBuilder read(final QueuedEvent event) {
      final Optional<RunningInstancesItemType> instance = getInstance(event);
      if (instance.isPresent()) {
        InstanceLogs.InstanceLogBuilder builder = prototype(event);
        for (final ResourceTag tag : instance.get().getTagSet()) {
          builder = builder.addTag(tag.getKey(), tag.getValue());
        }
        return builder;
      } else {
        return empty(event);
      }
    }
  };

  private static Optional<RunningInstancesItemType> getInstance(final QueuedEvent event) {
    final SimpleEntry<String,String> key = new SimpleEntry<>(event.getAccountId(), event.getResourceId());
    try {
      return instanceCache.get(key);
    } catch(final ExecutionException ex) {
      return Optional.empty();
    }
  }

  private static final LoadingCache<SimpleEntry<String, String>, Optional<RunningInstancesItemType>> instanceCache = CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(3, TimeUnit.MINUTES)
          .build( new CacheLoader<SimpleEntry<String, String>, Optional<RunningInstancesItemType>>() {
            @Override
            public Optional<RunningInstancesItemType> load(final SimpleEntry<String, String> accountInstanceIdPair) throws Exception {
              try {
                final String userId =
                        Accounts.lookupPrincipalByAccountNumberAndUsername(accountInstanceIdPair.getKey(), "admin").getUserId();
                final List<RunningInstancesItemType> results
                        = Ec2Client.getInstance().describeInstances(userId, Lists.newArrayList(accountInstanceIdPair.getValue()));
                return results.stream().findAny();
              } catch (final Exception ex) {
                ;
              }
              return Optional.empty();
            }
          });

  private static InstanceLogs.InstanceLogBuilder prototype(final QueuedEvent evt) {
    return InstanceLogs.getInstance().newRecord(evt.getAccountId())
            .withInstanceId(evt.getResourceId())
            .withLogTime(
                    getHour(new Date(System.currentTimeMillis()))
            );
  }

  private static Date getHour(final Date time) {
    final Calendar c = Calendar.getInstance();
    c.setTime(time);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  private static InstanceLogs.InstanceLogBuilder empty(final QueuedEvent evt) {
    return InstanceLogs.getInstance().newRecord(evt.getAccountId()).empty();
  }

  public static List<InstanceLogs.InstanceLogBuilder> readLogs(final List<QueuedEvent> events) {
    final List<QueuedEvent> uniqueEvents = events.stream()
            .collect( groupingBy(
                            e -> e.getResourceId() )
            ).entrySet().stream()
            .map(kv -> kv.getValue().get(0))
            .collect(Collectors.toList()); // distinct event for instance ID

    return uniqueEvents.stream()
            .map ( e ->
                    INSTANCE_TYPE.read(e)
                    .merge(PLATFORM.read(e))
                    .merge(AVAILABILITY_ZONE.read(e))
                    .merge(TAG.read(e))
            ).collect(Collectors.toList());
  }
}
