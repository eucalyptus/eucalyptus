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
package com.eucalyptus.portal.awsusage;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.component.annotation.ComponentPart;

import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.compute.common.internal.address.AddressState;
import com.eucalyptus.compute.common.internal.address.AllocatedAddressEntity;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshot;
import com.eucalyptus.compute.common.internal.blockstorage.State;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.portal.SimpleQueueClientManager;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.portal.workflow.AwsUsageRecord;
import com.eucalyptus.portal.workflow.AwsUsageActivities;
import com.eucalyptus.portal.workflow.BillingActivityException;
import com.eucalyptus.reporting.event.AddressEvent;
import com.eucalyptus.reporting.event.LoadBalancerEvent;
import com.eucalyptus.reporting.event.S3ObjectEvent;
import com.eucalyptus.reporting.event.SnapShotEvent;
import com.eucalyptus.reporting.event.VolumeEvent;
import com.eucalyptus.resources.client.Ec2Client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@ComponentPart(Portal.class)
public class AwsUsageActivitiesImpl implements AwsUsageActivities {
  private static Logger LOG     =
          Logger.getLogger(  AwsUsageActivitiesImpl.class );

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

  private final static String QUEUE_NAME_PREFIX = "awsusagework";

  @Override
  public Map<String, String> createAccountQueues(final String globalQueue) throws BillingActivityException {
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
        } else {
          resourceOwnerMap.put( resourceId, "000000000000");
          return "000000000000";
        }
      }
    };

    final List<String> uniqueAccounts = events.stream()
            .map( cachedAccountLookup )
            .distinct()
            .filter( a -> a!=null )
            .collect(Collectors.toList());

    for ( final String accountId : uniqueAccounts ) {
      try{
        final String queueName = String.format("%s-%s-%s",
                QUEUE_NAME_PREFIX,
                accountId,
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
        try { // clean up
          for (final String queueName : accountQueues.values()) {
            sqClient.deleteQueue(queueName);
          }
        } catch (final Exception ex2) {
          ;
        }
        throw new BillingActivityException("Failed to create SQS queue", ex);
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
  public List<AwsUsageRecord> getAwsReportHourlyUsageRecord(final String accountId, final String queue) throws BillingActivityException {
    final SimpleQueueClientManager sqClient = SimpleQueueClientManager.getInstance();
    final List<QueuedEvent> events = Lists.newArrayList();
    try {
      events.addAll(sqClient.receiveAllMessages(queue, false).stream()
              .map(m -> QueuedEvents.MessageToEvent.apply(m.getBody()))
              .filter(e -> e != null)
              .collect(Collectors.toList())
      );
    } catch (final Exception ex) {
      throw new BillingActivityException("Failed to receive queue messages", ex);
    }

    final List<AwsUsageRecord> result = Lists.newArrayList();
    for (final AwsUsageRecordType type : AwsUsageRecordType.values()) {
      if (AwsUsageRecordType.UNKNOWN.equals(type) || !AggregateGranularity.HOURLY.equals(type.getGranularity()))
        continue;

      result.addAll(type.read(accountId, events));
    }

    return result;
  }

  @Override
  public List<AwsUsageRecord> getAwsReportDailyUsageRecord(final String accountId, final String queue) throws BillingActivityException {
    final SimpleQueueClientManager sqClient = SimpleQueueClientManager.getInstance();
    final List<QueuedEvent> events = Lists.newArrayList();
    try {
      events.addAll(sqClient.receiveAllMessages(queue, false).stream()
              .map(m -> QueuedEvents.MessageToEvent.apply(m.getBody()))
              .filter(e -> e != null)
              .collect(Collectors.toList())
      );
    } catch (final Exception ex) {
      throw new BillingActivityException("Failed to receive queue messages", ex);
    }

    final List<AwsUsageRecord> result = Lists.newArrayList();
    for (final AwsUsageRecordType type : AwsUsageRecordType.values()) {
      if (AwsUsageRecordType.UNKNOWN.equals(type)) // daily aggregate processes both hourly and daily type records
        continue;

      result.addAll(type.read(accountId, events));
    }
    return result;
  }

  @Override
  public void writeAwsReportUsage(final List<AwsUsageRecord> records) throws BillingActivityException{
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
        // deleteQueue is idempotent operation
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

  @Override
  public void fireVolumeUsage() throws BillingActivityException {
    final List<Volume> volumes = Lists.newArrayList();
    try ( final TransactionResource db = Entities.transactionFor( Volume.class ) ) {
      final Volume sample = Volume.named(null, null);
      volumes.addAll(Entities.query(sample));
    }

    final Function<Volume, VolumeEvent> toEvent = (volume) -> VolumeEvent.with(
            VolumeEvent.forVolumeUsage(),
            volume.getNaturalId(),
            volume.getDisplayName(),
            volume.getSize(),
            volume.getOwner(),
            volume.getPartition());

    try {
      volumes.stream()
              .filter (v -> State.EXTANT.equals(v.getState()))
              .map( toEvent )
              .forEach( fire );
    } catch ( final Exception ex) {
      throw new BillingActivityException("Failed to fire volume usage events", ex);
    }
  }

  @Override
  public void fireSnapshotUsage() throws BillingActivityException {
    final List<Snapshot> snapshots = Lists.newArrayList();
    try ( final TransactionResource db = Entities.transactionFor( Snapshot.class ) ) {
      final Snapshot sample = Snapshot.named(null, null);
      snapshots.addAll(Entities.query(sample));
    }

    final Function<Snapshot, SnapShotEvent> toEvent = (snapshot) -> SnapShotEvent.with(
            SnapShotEvent.forSnapShotUsage(),
            snapshot.getNaturalId(),
            snapshot.getDisplayName(),
            snapshot.getOwnerUserId(),
            snapshot.getOwnerUserName(),
            snapshot.getOwnerAccountNumber(),
            snapshot.getVolumeSize()
    );

    try {
      snapshots.stream()
              .filter (snap -> State.EXTANT.equals(snap.getState()))
              .map( toEvent )
              .forEach( fire );
    } catch ( final Exception ex) {
      throw new BillingActivityException("Failed to fire snapshot usage events", ex);
    }
  }

  @Override
  public void fireAddressUsage() throws BillingActivityException {
    final List<AllocatedAddressEntity> addresses = Lists.newArrayList();
    try ( final TransactionResource db = Entities.transactionFor( AllocatedAddressEntity.class ) ) {
      final AllocatedAddressEntity sample = AllocatedAddressEntity.example();
      addresses.addAll(Entities.query(sample));
    }

    final Function< AllocatedAddressEntity, String > accountName = (addr) -> {
      try {
        return Accounts.lookupAccountAliasById( addr.getOwner().getAccountNumber( ) );
      } catch (final AuthException ex) {
        return "eucalyptus";
      }
    };

    final Function<AllocatedAddressEntity, AddressEvent> toEvent = (addr) -> AddressState.assigned.equals(addr.getState()) ?
            AddressEvent.with(
                    addr.getAddress(),
                    addr.getOwner(),
                    accountName.apply(addr),
                    addr.getInstanceId(),
                    AddressEvent.forUsageAssociate()) :
            AddressEvent.with(
                    addr.getAddress(),
                    addr.getOwner(),
                    accountName.apply(addr),
                    AddressEvent.forUsageAllocate());

    // EUCA-13348
    // Elastic IP is charged when 1) IP is NOT associated with a running instance,
    // or 2) For any additional IPs associated with a running instance
    try {
      addresses.stream()
              .filter( addr -> !"000000000000".equals(addr.getOwnerAccountNumber()))
              .filter (addr -> AddressState.allocated.equals(addr.getState())
                      || AddressState.assigned.equals(addr.getState()))
              .map( toEvent )
              .forEach( fire );
    } catch ( final Exception ex) {
      throw new BillingActivityException("Failed to fire address usage events", ex);
    }
  }

  @Override
  public void fireS3ObjectUsage() throws BillingActivityException {
    final List<ObjectEntity> objects = Lists.newArrayList();
    try ( final TransactionResource db = Entities.transactionFor( ObjectEntity.class ) ) {
      final ObjectEntity sample = new ObjectEntity();
      objects.addAll(Entities.query(sample));
    }

    final Map<String, String> accountNumberCache = Maps.newHashMap();
    final Function<String, String> lookupAccountNumber = (alias) -> {
      if(accountNumberCache.keySet().contains(alias))
        return accountNumberCache.get(alias);

      try {
        final String accountNumber = Accounts.lookupAccountIdByAlias(alias);
        accountNumberCache.put(alias, accountNumber);
        return accountNumber;
      }catch(final AuthException ex) {
        ;
      }
      return "000000000000";
    };

    final Function<ObjectEntity, S3ObjectEvent> toEvent = (obj) -> S3ObjectEvent.with(
            S3ObjectEvent.S3ObjectAction.OBJECTUSAGE,
            obj.getBucket().getBucketName(),
            obj.getObjectKey(),
            obj.getVersionId(),
            obj.getOwnerIamUserId(),
            obj.getOwnerIamUserDisplayName(),
            lookupAccountNumber.apply(obj.getOwnerDisplayName()),
            obj.getSize());

    try{
      objects.stream()
              .filter( o -> ObjectState.extant.equals(o.getState()) )
              .map ( toEvent )
              .forEach ( fire );
    } catch( final Exception ex) {
      throw new BillingActivityException("Failed to fire s3 object usage events", ex);
    }
  }

  @Override
  public void fireLoadBalancerUsage() throws BillingActivityException {
    final List<LoadBalancer> loadbalancers = LoadBalancers.listLoadbalancers();
    final Function<LoadBalancer, LoadBalancerEvent> toEvent = (lb) -> LoadBalancerEvent.with(
            LoadBalancerEvent.forLoadBalancerUsage(),
            lb.getOwner(),
            lb.getDisplayName());
    try{
      loadbalancers.stream()
              .map ( toEvent )
              .forEach( fire );
    } catch( final Exception ex) {
      throw new BillingActivityException("Failed to fire loadbalancer usage events", ex);
    }
  }

  private static Consumer<Event> fire = (event) -> {
    try {
      ListenerRegistry.getInstance().fireEvent(event);
    } catch (final EventFailedException ex) {
      ;
    }
  };
}
