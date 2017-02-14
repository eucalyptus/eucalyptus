/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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

import com.eucalyptus.component.annotation.ComponentPart;

import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.portal.AwsUsageReportData;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.resources.client.Ec2Client;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ComponentPart(Portal.class)
public class BillingActivitiesImpl implements BillingActivities {
  private static Logger LOG     =
          Logger.getLogger(  BillingActivitiesImpl.class );

  private static String lookupAccount(final QueuedEvent event) {
    if (event.getAccountId()!=null)
      return event.getAccountId();

    if ("InstanceUsage".equals(event.getEventType())) {
      final String instanceId = event.getResourceId();
      try {
        final Optional<ReservationInfoType> instance =
                Ec2Client.getInstance().describeInstanceReservations(null, Lists.newArrayList(instanceId))
                        .stream().findFirst();
        if (instance.isPresent()) {
          return instance.get().getOwnerId();
        }
      } catch (final Exception ex) {
        LOG.error("Failed to lookup owner if instance: " + instanceId);
      }
    }

    return null;
  }

  @Override
  public Map<String, String> createAccountQueues(final String globalQueue) throws BillingActivityException{
    final Map<String, String> accountQueues =  Maps.newHashMap();
    final SimpleQueueClientManager sqClient = SimpleQueueClientManager.getInstance();
    final List<QueuedEvent> events = Lists.newArrayList();
    try {
      events.addAll(sqClient.receiveAllMessages(globalQueue, true).stream()
              .map( m -> QueuedEvents.MessageToEvent.apply(m.getBody()) )
              .filter( e -> e != null )
              .collect(Collectors.toList())
      );
    }catch (final Exception ex) {
      throw new BillingActivityException("Failed to receive queue messages", ex);
    }

    final Map<String, String> resourceOwnerMap = Maps.newHashMap();
    final Function<QueuedEvent, String> cachedAccountLookup = queuedEvent -> {
      final String resourceId = queuedEvent.getResourceId();
      if (resourceOwnerMap.containsKey( resourceId ))
        return resourceOwnerMap.get( resourceId );
      else {
        final String accountId = lookupAccount(queuedEvent);
        if (accountId!=null) {
          resourceOwnerMap.put( resourceId, accountId );
          return accountId;
        }
      }
      return null;
    };

    final List<String> uniqueAccounts = events.stream()
            .map( cachedAccountLookup )
            .distinct()
            .filter( a -> a!=null )
            .collect(Collectors.toList());

    for (final String accountId : uniqueAccounts ) {
      try{
        final String queueName = String.format("%s-%s", accountId,
                UUID.randomUUID().toString().substring(0, 13) );
        sqClient.createQueue(
                queueName,
                Maps.newHashMap(
                        ImmutableMap.of(
                                "MessageRetentionPeriod", "120",
                                "MaximumMessageSize", "4096",
                                "VisibilityTimeout", "10")
                ) );
        accountQueues.put(accountId, queueName);
      } catch (final Exception ex) {
        LOG.error("Failed to create SQS queue", ex);
      }
    }

    for (final QueuedEvent e : events) {
      final String accountId = cachedAccountLookup.apply(e);
      if (accountId != null) {
        final String queueName = accountQueues.get(accountId);
        if (queueName != null) {
          try {
            sqClient.sendMessage(queueName, QueuedEvents.EventToMessage.apply(e));
          } catch (final Exception ex) {
            ;
          }
        }
      }
    }
    return accountQueues;
  }

  @Override
  public List<AwsUsageRecord> getAwsReportUsageRecord(final String accountId, final String queue, final String recordType) throws BillingActivityException{
    final List<AwsUsageReportData> records = Lists.newArrayList();
    final SimpleQueueClientManager sqClient = SimpleQueueClientManager.getInstance();
    final List<QueuedEvent> events = Lists.newArrayList();
    try {
      events.addAll(sqClient.receiveAllMessages(queue, false).stream()
              .map( m -> QueuedEvents.MessageToEvent.apply(m.getBody()) )
              .filter( e -> e != null )
              .collect(Collectors.toList())
      );
    }catch (final Exception ex) {
      throw new BillingActivityException("Failed to receive queue messages", ex);
    }

    final AwsUsageRecordType type =
            AwsUsageRecordType.forValue(recordType);
    return type.read(accountId, events);
  }

  @Override
  public void writeAwsReportHourlyUsage(final String accountId, final List<AwsUsageRecord> records) throws BillingActivityException{
    try {
      AwsUsageRecords.getInstance().append(records);
    } catch (final Exception ex) {
      throw new BillingActivityException("Failed to append aws usage records", ex);
    }
  }

  @Override
  public void deleteAccountQueues(final List<String> queues) throws BillingActivityException {
    for (final String queue : queues) {
      try {
        SimpleQueueClientManager.getInstance().deleteQueue(queue);
      } catch (final Exception ex) {
        LOG.error("Failed to delete the temporary queue (" + queue +")", ex);
      }
    }
  }
}
