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

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.SimpleQueueClientManager;
import com.eucalyptus.portal.awsusage.QueuedEvent;
import com.eucalyptus.portal.awsusage.QueuedEvents;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.portal.workflow.BillingActivityException;
import com.eucalyptus.portal.workflow.InstanceLog;
import com.eucalyptus.portal.workflow.InstanceLogActivities;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@ComponentPart(Portal.class)
public class InstanceLogActivitiesImpl implements InstanceLogActivities {
  private static Logger LOG     =
          Logger.getLogger(  InstanceLogActivitiesImpl.class );

  private final static String QUEUE_NAME_PREFIX = "instancehourwork";

  // return key: account_id, value: SQS queue)
  @Override
  public Map<String, String> distributeEvents(String globalQueue) throws BillingActivityException {
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

    final Map<String, List<QueuedEvent>> accountEvents =
            events.stream()
                    .filter(e -> "InstanceUsage".equals(e.getEventType()))
                    .collect( groupingBy( e -> e.getAccountId() ,
                            Collectors.mapping( Function.identity() ,
                                    Collectors.toList())) );

    final Map<String, String> queueMap = Maps.newHashMap();
    for (final String accountId : accountEvents.keySet() ) {
      try {
        final String queueName = String.format("%s-%s-%s",
                QUEUE_NAME_PREFIX,
                accountId,
                UUID.randomUUID().toString().substring(0, 13));
        // create a new temporary queue
        sqClient.createQueue(
                queueName,
                Maps.newHashMap(
                        ImmutableMap.of(
                                "MessageRetentionPeriod", "120",
                                "MaximumMessageSize", "4096",
                                "VisibilityTimeout", "10")
                ));
        accountEvents.get(accountId).stream()
                .forEach(
                        e -> {
                          try {
                            sqClient.sendMessage(queueName, QueuedEvents.EventToMessage.apply(e));
                          } catch (final Exception ex) {
                            ;
                          }
                        }
                );
        queueMap.put(accountId, queueName);
      } catch (final Exception ex) {
        try { // clean up
          for (final String queueName : queueMap.values()) {
            sqClient.deleteQueue(queueName);
          }
        } catch (final Exception ex2) {
          ;
        }
        throw new BillingActivityException("Failed to copy SQS message into a new queue", ex);
      }
    }
    return queueMap;
  }

  @Override
  public void persist(final String accountId, final String queueName) throws BillingActivityException {
    final SimpleQueueClientManager sqClient = SimpleQueueClientManager.getInstance();
    final List<QueuedEvent> events = Lists.newArrayList();
    try {
      events.addAll(sqClient.receiveAllMessages(queueName, false).stream()
              .map(m -> QueuedEvents.MessageToEvent.apply(m.getBody()))
              .filter(e -> e != null)
              .collect(Collectors.toList())
      );
    } catch (final Exception ex) {
      throw new BillingActivityException("Failed to receive queue messages", ex);
    }

    try {
      final List<InstanceLog> logs = InstanceLogReaders.readLogs(events).stream()
              .map( e -> e.build() )
              .filter( e -> e.isPresent() )
              .map( e -> e.get() )
              .collect(Collectors.toList());
      InstanceLogs.getInstance().append(logs);
    } catch (final Exception ex) {
      LOG.error("Failed to persist instance hour record", ex);
    }
  }

  @Override
  public void deleteQueues(List<String> queues) throws BillingActivityException {
    for (final String queue : queues) {
      try {
        SimpleQueueClientManager.getInstance().deleteQueue(queue);
      } catch (final Exception ex) {
        LOG.error("Failed to delete temporary queue (" + queue +")", ex);
      }
    }
  }

  @Override
  public void cleanupQueues() {
    final SimpleQueueClientManager sqClient = SimpleQueueClientManager.getInstance();
    try {
      for (final String queueUrl : sqClient.listQueues(QUEUE_NAME_PREFIX)) {
        sqClient.deleteQueue(queueUrl);
      }
    } catch(final Exception ex) {
      ;
    }
  }
}
