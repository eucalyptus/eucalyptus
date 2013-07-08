/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.reporting;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.DatabaseAuthProvider;
import com.eucalyptus.auth.api.AccountProvider;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.blockstorage.Snapshot;
import com.eucalyptus.blockstorage.State;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.entities.ObjectInfo;
import com.eucalyptus.objectstorage.util.WalrusProperties;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;
import com.eucalyptus.reporting.domain.ReportingUserCrud;
import com.eucalyptus.reporting.event_store.ReportingElasticIpAttachEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDetachEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpEventStore;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectEventStore;
import com.eucalyptus.reporting.event_store.ReportingVolumeAttachEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeDetachEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeEventStore;
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotEventStore;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasNaturalId;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Class to verify / create reporting data.
 *
 * Create a view of the current cloud status from reporting data or from
 * cloud metadata. Views can be compared and synchronized.
 */
public final class ReportingDataVerifier {

  // Accounts.getAccountProvider() will be null during a DB upgrade
  private static Supplier<AccountProvider> accountProviderSupplier =
      Suppliers.memoize( new Supplier<AccountProvider>() {
    @Override
    public AccountProvider get() {
      return Accounts.getAccountProvider() != null ?
          Accounts.getAccountProvider() :
          new DatabaseAuthProvider();
    }
  } );

  /**
   * Update the reporting database to ensure events are present for existing items.
   *
   * @return A description of the changes made
   */
  public static String addMissingReportingEvents() {
    final View liveView = getLiveView();
    final View reportingView = getReportingView();
    final EventDescriptionBag eventDescriptions = getEventDifferences( liveView, reportingView );
    addReportingEvents( eventDescriptions );
    return "Live:\n" + liveView + "\nReporting:\n" + reportingView + "\nGap:\n" + eventDescriptions ;
  }

  /**
   * Get the "event" view of the cloud metadata.
   *
   * @return The "live" view
   */
  public static View getLiveView() {
    return new LiveViewBuilder().buildView();
  }

  /**
   * Get the view of the reporting database / event store
   *
   * @return The reporting view
   */
  public static View getReportingView() {
    return new ReportingViewBuilder().buildView();
  }

  /**
   * Get the events neccessary to align the current view with the target view.
   *
   * @param target The desired "event" state
   * @param current The current state
   * @return The bag describing the necessary events
   */
  public static EventDescriptionBag getEventDifferences( final View target, final View current ) {
    final EventDescriptionBag description = new EventDescriptionBag();

    final Set<Class<?>> types = Sets.newHashSet( Iterables.transform( Iterables.concat( target.typedResourceHolders, current.typedResourceHolders ), type() ) );
    for ( final Class<?> type : types ) {
      final TypedResourceHolder targetHolder = target.getHolderOrNull( type );
      final TypedResourceHolder currentHolder = current.getHolderOrNull( type );

      if ( targetHolder == null ) {
        description.delete.add( currentHolder );
      } else if ( currentHolder == null ) {
        description.create.add( targetHolder );
      } else { // determine difference
        final TypedResourceHolder add = new TypedResourceHolder( type );
        final TypedResourceHolder del = new TypedResourceHolder( type );
        final TypedResourceHolder att = new TypedResourceHolder( type );
        final TypedResourceHolder det = new TypedResourceHolder( type );

        // create copies containing only items that differ
        List<ResourceWithRelation<?>> targetCopy = Lists.newArrayList( targetHolder.resources );
        List<ResourceWithRelation<?>> currentCopy = Lists.newArrayList( currentHolder.resources );
        targetCopy.removeAll( currentHolder.resources );
        currentCopy.removeAll( targetHolder.resources );

        final Set<ResourceKey> keys = Sets.newHashSet( Iterables.transform( Iterables.concat( targetCopy, currentCopy ), key() ) );
        for ( final ResourceKey key : keys ) {
          final List<ResourceWithRelation<?>> findIn;
          final TypedResourceHolder addTo;
          if ( !Iterables.contains( Iterables.transform( targetCopy, key() ), key ) ) {
            findIn = currentCopy;
            addTo = del;
          } else if ( !Iterables.contains( Iterables.transform( currentCopy, key() ), key ) ) {
            findIn = targetCopy;
            addTo = add;
          } else if ( Iterables.find( targetCopy, withKeyMatching( key ) ).relationId == null ) {
            findIn = currentCopy;
            addTo = det;
          } else {
            findIn = targetCopy;
            addTo = att;
          }
          addTo.resources.add( Iterables.find( findIn, withKeyMatching( key ) ) );
        }

        if ( !add.resources.isEmpty() ) description.create.add( add );
        if ( !del.resources.isEmpty() ) description.delete.add( del );
        if ( !att.resources.isEmpty() ) description.attach.add( att );
        if ( !det.resources.isEmpty() ) description.detach.add( det );
      }
    }

    return description;
  }

  /**
   * Add events as described in the given bag
   *
   * @param eventDescriptions The description of the events to add.
   */
  public static void addReportingEvents( final EventDescriptionBag eventDescriptions ) {
    final long timestamp = System.currentTimeMillis();
    final Set<String> verifiedUserIds = Sets.newHashSet();
    addCreateEvents( verifiedUserIds, eventDescriptions.create );
    addDeleteEvents( timestamp, eventDescriptions.delete );
    addAttachmentStateEvents( timestamp, verifiedUserIds, eventDescriptions.attach, true );
    addAttachmentStateEvents( timestamp, verifiedUserIds, eventDescriptions.detach, false );
  }

  private static Predicate<ResourceWithRelation<?>> withKeyMatching( final ResourceKey key ) {
    return Predicates.compose( Predicates.equalTo( key ), key() );
  }

  private static void addCreateEvents( final Set<String> verifiedUserIds,
                                       final List<TypedResourceHolder> holders ) {
    final Map<String,User> accountNumberToAccountAdminMap = Maps.newHashMap();
    for ( TypedResourceHolder holder : holders ) {
      for ( ResourceWithRelation resource : holder.resources ) {
        if ( Address.class.equals( holder.type ) ) {
          final ReportingElasticIpEventStore store = ReportingElasticIpEventStore.getInstance();
          final Address address = findAddress( resource.resourceKey.toString() );
          if ( address != null && ensureUserAndAccount( verifiedUserIds, address.getUserId() ) ) {
            store.insertCreateEvent( address.getCreationTimestamp().getTime(), address.getUserId(), address.getDisplayName() );
          }
        } else if ( ObjectInfo.class.equals( holder.type ) ) {
          final ReportingS3ObjectEventStore store = ReportingS3ObjectEventStore.getInstance();
          final S3ObjectKey key = (S3ObjectKey) resource.resourceKey;
          final ObjectInfo objectInfo = findObjectInfo( key );
          final User user = objectInfo==null ? null : getAccountAdmin( accountNumberToAccountAdminMap, objectInfo.getOwnerId() );
          if ( objectInfo != null && user != null && ensureUserAndAccount( verifiedUserIds, user.getUserId() ) ) {
            store.insertS3ObjectCreateEvent( objectInfo.getBucketName(), objectInfo.getObjectKey(), objectInfo.getVersionId(), objectInfo.getSize(), objectInfo.getCreationTimestamp().getTime(), user.getUserId() );
          }
        } else if ( Snapshot.class.equals( holder.type ) ) {
          final ReportingVolumeSnapshotEventStore store = ReportingVolumeSnapshotEventStore.getInstance();
          final Snapshot snapshot = findSnapshot( resource.resourceKey.toString() );
          final Volume volume = snapshot==null ? null : findVolumeById( snapshot.getParentVolume() );
          if ( snapshot != null && volume != null && ensureUserAndAccount( verifiedUserIds, snapshot.getOwnerUserId() ) ) {
            store.insertCreateEvent( snapshot.getNaturalId(), volume.getNaturalId(), snapshot.getDisplayName(), snapshot.getCreationTimestamp().getTime(), snapshot.getOwnerUserId(), snapshot.getVolumeSize() );
          }
        } else if ( Volume.class.equals( holder.type ) ) {
          final ReportingVolumeEventStore store = ReportingVolumeEventStore.getInstance();
          final Volume volume = findVolume( resource.resourceKey.toString() );
          if ( volume != null && ensureUserAndAccount( verifiedUserIds, volume.getOwnerUserId() ) ) {
            store.insertCreateEvent( volume.getNaturalId(), volume.getDisplayName(), volume.getCreationTimestamp().getTime(), volume.getOwnerUserId(), volume.getPartition(), volume.getSize() );
          }
        }
      }
    }
  }

  private static void addDeleteEvents( final long timestamp,
                                       final List<TypedResourceHolder> holders ) {
    for ( TypedResourceHolder holder : holders ) {
      for ( ResourceWithRelation resource : holder.resources ) {
        if ( Address.class.equals( holder.type ) ) {
          final ReportingElasticIpEventStore store = ReportingElasticIpEventStore.getInstance();
          store.insertDeleteEvent( resource.resourceKey.toString(), timestamp );
        } else if ( ObjectInfo.class.equals( holder.type ) ) {
          final ReportingS3ObjectEventStore store = ReportingS3ObjectEventStore.getInstance();
          final S3ObjectKey key = (S3ObjectKey) resource.resourceKey;
          store.insertS3ObjectDeleteEvent( key.bucketName, key.objectKey, key.objectVersion, timestamp );
        } else if ( Snapshot.class.equals( holder.type ) ) {
          final ReportingVolumeSnapshotEventStore store = ReportingVolumeSnapshotEventStore.getInstance();
          store.insertDeleteEvent( resource.resourceKey.toString(), timestamp );
        } else if ( Volume.class.equals( holder.type ) ) {
          final ReportingVolumeEventStore store = ReportingVolumeEventStore.getInstance();
          store.insertDeleteEvent( resource.resourceKey.toString(), timestamp );
        }
      }
    }
  }

  private static void addAttachmentStateEvents( final long timestamp,
                                                final Set<String> verifiedUserIds,
                                                final List<TypedResourceHolder> holders,
                                                final boolean attach ) {
    for ( TypedResourceHolder holder : holders ) {
      for ( ResourceWithRelation resource : holder.resources ) {
        if ( Address.class.equals( holder.type ) ) {
          final ReportingElasticIpEventStore store = ReportingElasticIpEventStore.getInstance();
          if ( attach ) {
            store.insertAttachEvent( resource.resourceKey.toString(), resource.relationId, timestamp );
          } else {
            store.insertDetachEvent( resource.resourceKey.toString(), resource.relationId, timestamp );
          }
        } else if ( Volume.class.equals( holder.type ) ) {
          final Volume volume = findVolume( resource.resourceKey.toString() );
          if ( volume != null && ensureUserAndAccount( verifiedUserIds, volume.getOwnerUserId() ) ) {
            final ReportingVolumeEventStore store = ReportingVolumeEventStore.getInstance();
            if ( attach ) {
              store.insertAttachEvent( volume.getNaturalId(), resource.relationId, volume.getSize(), timestamp );
            } else {
              store.insertDetachEvent( volume.getNaturalId(), resource.relationId, timestamp );
            }
          }
        }
      }
    }
  }

  private static ResourceWithRelation<SimpleResourceKey> resource( final String resourceId,
                                                                   final String relationId ) {
    return new ResourceWithRelation<SimpleResourceKey>( new SimpleResourceKey( resourceId ), relationId );
  }

  private static ResourceWithRelation<S3ObjectKey> s3ObjectResource( final String bucketName,
                                                                     final String objectKey,
                                                                     final String objectVersion ) {
    return s3ObjectResource( new S3ObjectKey( bucketName, objectKey, objectVersion ) );
  }

  private static ResourceWithRelation<S3ObjectKey> s3ObjectResource( final S3ObjectKey key ) {
    return new ResourceWithRelation<S3ObjectKey>( key, null );
  }

  private static void append( final StringBuilder builder, final List<TypedResourceHolder> typedResourceHolders ) {
    for ( final TypedResourceHolder typedResourceHolder : typedResourceHolders ) {
      builder.append( "Type: " ).append( typedResourceHolder.type.getSimpleName() ).append("\n");
      for ( ResourceWithRelation<?> resource : typedResourceHolder.resources ) {
        builder.append( resource ).append("\n");
      }
    }
  }

  private static boolean ensureUserAndAccount( final Set<String> userIds, final String userId ) {
    boolean verified;

    if ( userIds.contains( userId ) ) {
      verified = true;
    } else {
      try {
        final User user = getAccountProvider().lookupUserById( userId );
        final Account account = user.getAccount();
        ReportingAccountCrud.getInstance().createOrUpdateAccount( account.getAccountNumber(), account.getName() );
        ReportingUserCrud.getInstance().createOrUpdateUser( user.getUserId(), account.getAccountNumber(), user.getName() );
        verified = true;
      } catch ( AuthException e ) {
        verified = false;
      }
    }

    return verified;
  }

  private static User getAccountAdmin( final Map<String, User> accountNumberToAccountAdminMap, final String accountNumber ) {
    User user = accountNumberToAccountAdminMap.get( accountNumber );

    if ( user==null && !accountNumberToAccountAdminMap.containsKey( accountNumber ) ) {
      try {
        user = getAccountProvider().lookupAccountById( accountNumber ).lookupAdmin();
        accountNumberToAccountAdminMap.put( accountNumber, user );
      } catch ( AuthException e ) {
        accountNumberToAccountAdminMap.put( accountNumber, null );
      }
    }

    return user;
  }

  private static AccountProvider getAccountProvider() {
    return accountProviderSupplier.get();
  }

  private static Address findAddress( final String uuid ) {
    return Iterables.getFirst( Iterables.filter(
        Iterables.concat( Addresses.getInstance().listValues(), Addresses.getInstance().listDisabledValues() ),
        Predicates.compose( Predicates.equalTo( uuid ), naturalId() ) ), null );
  }

  private static ObjectInfo findObjectInfo( final S3ObjectKey key ) {
    try {
      final ObjectInfo objectInfo = new ObjectInfo();
      objectInfo.setBucketName( key.bucketName );
      objectInfo.setObjectKey( key.objectKey );
      objectInfo.setVersionId( key.objectVersion==null ? WalrusProperties.NULL_VERSION_ID : key.objectVersion );
      final List<ObjectInfo> infos = Transactions.findAll( objectInfo );
      if ( infos.isEmpty() ) {
        return null;
      }
      ObjectInfo result = infos.get( 0 );
      for ( final ObjectInfo current : infos ) {
        if ( current.getCreationTimestamp().after( result.getCreationTimestamp() ) ) {
          result = current;
        }
      }
      return result;
    } catch ( TransactionException e ) {
      return null;
    }
  }

  private static Snapshot findSnapshot( final String uuid  ) {
    return findOrNull( Snapshot.naturalId( uuid ) );
  }

  private static Volume findVolume( final String uuid  ) {
    return findOrNull( Volume.naturalId( uuid ) );
  }

  private static Volume findVolumeById( final String id ) {
    return findOrNull( Volume.named( null, id ) );
  }

  private static <T> T findOrNull( final T example ) {
    try {
      return Transactions.find( example );
    } catch ( TransactionException e ) {
      return null;
    }
  }

  private static Function<HasNaturalId, String> naturalId() {
    return new Function<HasNaturalId, String>() {
      @Override
      public String apply( final HasNaturalId hasNaturalId ) {
        return hasNaturalId.getNaturalId();
      }
    };
  }


  private static Function<TypedResourceHolder, Class<?>> type() {
    return new Function<TypedResourceHolder, Class<?>>() {
      @Override
      public Class<?> apply( final TypedResourceHolder typedResourceHolder ) {
        return typedResourceHolder.type;
      }
    };
  }

  private static Function<ResourceWithRelation<?>, ResourceKey> key() {
    return new Function<ResourceWithRelation<?>, ResourceKey>() {
      @Override
      public ResourceKey apply( final ResourceWithRelation<?> resource ) {
        return resource.resourceKey;
      }
    };
  }

  public static class EventDescriptionBag {
    private final List<TypedResourceHolder> create = Lists.newArrayList();
    private final List<TypedResourceHolder> delete = Lists.newArrayList();
    private final List<TypedResourceHolder> attach = Lists.newArrayList();
    private final List<TypedResourceHolder> detach = Lists.newArrayList();

    public String toString() {
      final StringBuilder builder = new StringBuilder();

      builder.append("Creation required:\n");
      append( builder, create );
      builder.append("Deletion required:\n");
      append( builder, delete );
      builder.append( "Attach required:\n" );
      append( builder, attach );
      builder.append( "Detach required:\n" );
      append( builder, detach );

      return builder.toString();
    }
  }

  /**
   * A view of current status.
   *
   * We care about:
   *  - Allocated Elastic IPs and their association target
   *  - Existing S3 objects
   *  - Existing volumes and their attachment target
   *  - Existing snapshots
   */
  public static class View {
    private final List<TypedResourceHolder> typedResourceHolders = Lists.newArrayList();

    private <CM> void add( @Nonnull  final Class<CM> type,
                           @Nonnull  final String resourceId,
                           @Nullable final String relationId ) {
      add(type, resource( resourceId, relationId ));
    }

    private <CM> void add( @Nonnull  final Class<CM> type,
                           @Nonnull  final ResourceWithRelation<?> resource ) {
      final TypedResourceHolder holder = getHolder(type);
      holder.resources.add( resource );
    }

    @Nonnull
    private <CM> TypedResourceHolder getHolder( @Nonnull final Class<CM> type ) {
      TypedResourceHolder holder = getHolderOrNull( type );

      if ( holder == null ) {
        holder = new TypedResourceHolder( type );
        typedResourceHolders.add( holder );
      }

      return holder;
    }

    private <CM> TypedResourceHolder getHolderOrNull( final Class<CM> type ) {
      return Iterables.get( Iterables.filter( typedResourceHolders, new Predicate<TypedResourceHolder>() {
        @Override
        public boolean apply( final TypedResourceHolder typedResourceHolder ) {
          return type.equals( typedResourceHolder.type );
        }
      } ), 0, null );
    }

    public String toString() {
      final StringBuilder builder = new StringBuilder();
      append( builder, typedResourceHolders );
      return builder.toString();
    }
  }

  private static abstract class ViewBuilder {
    abstract View buildView();
  }

  private static final class LiveViewBuilder extends ViewBuilder {
    @Override
    View buildView() {
      try {
        final View view = new View();

        // Addresses
        for ( final Address address : Iterables.concat( Addresses.getInstance().listValues(), Addresses.getInstance().listDisabledValues() ) ) {
          if ( address.isAllocated() && !address.isSystemOwned() ) {
            final String id = address.getNaturalId();
            String relatedId = null;
            if ( address.isAssigned() ) {
              relatedId = address.getInstanceUuid();
            }
            view.add( Address.class, id, relatedId );
          }
        }

        // S3 Objects
        for ( final ObjectInfo objectInfo : Transactions.findAll( new ObjectInfo() ) ) {
          if ( Boolean.FALSE.equals(objectInfo.getDeleted()) ) {
            view.add( ObjectInfo.class, s3ObjectResource(
                objectInfo.getBucketName(),
                objectInfo.getObjectKey(),
                objectInfo.getVersionId() ) );
          }
        }

        // Volumes
        for ( final Volume volume : Transactions.findAll( Volume.named( null, null ) )) {
          if ( volume.isReady() ) {
            final String id = volume.getNaturalId();
            String relatedId = null;
            try {
              relatedId = VmInstances.lookupVolumeAttachment( volume.getDisplayName() ).getVmInstance().getInstanceId();
            } catch ( NoSuchElementException ex1 ) {
              /** no attachment **/
            }
            view.add( Volume.class, id, relatedId );
          }
        }

        // Snapshots
        for ( final Snapshot snapshot : Transactions.findAll( Snapshot.named( null, null ) )) {
          if (EnumSet.of(State.EXTANT,State.BUSY).contains( snapshot.getState() ) ) {
            view.add( Snapshot.class, snapshot.getNaturalId(), null );
          }
        }

        return view;
      } catch ( TransactionException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }

  }

  private static final class ReportingViewBuilder extends ViewBuilder {
    private static final class RelationTimestamp {
      private final String relationId;
      private final long timestamp;

      private RelationTimestamp( final String relationId, final long timestamp ) {
        this.relationId = relationId;
        this.timestamp = timestamp;
      }
    }

    @Override
    View buildView() {
      // Build address info, map key denotes existence
      final Map<String,RelationTimestamp> addressRelationMap = Maps.newHashMap();
      foreach( ReportingElasticIpCreateEvent.class, new Callback<ReportingElasticIpCreateEvent>() {
        @Override
        public void fire( final ReportingElasticIpCreateEvent input ) {
          RelationTimestamp rt = addressRelationMap.get(input.getIp());
          if ( rt == null || rt.timestamp < input.getTimestampMs() ) {
            addressRelationMap.put( input.getIp(), new RelationTimestamp( null, input.getTimestampMs() ) );
          }
        }
      } );
      foreach(ReportingElasticIpDeleteEvent.class, new Callback<ReportingElasticIpDeleteEvent>() {
        @Override
        public void fire( final ReportingElasticIpDeleteEvent input ) {
          RelationTimestamp rt = addressRelationMap.get(input.getIp());
          if ( rt == null || rt.timestamp < input.getTimestampMs() ) {
            addressRelationMap.remove(input.getIp());
          }
        }
      } );
      foreach(ReportingElasticIpAttachEvent.class, new Callback<ReportingElasticIpAttachEvent>() {
        @Override
        public void fire( final ReportingElasticIpAttachEvent input ) {
          if ( addressRelationMap.containsKey( input.getIp() ) ) {
            RelationTimestamp rt = addressRelationMap.get(input.getIp());
            if ( rt == null || rt.relationId == null || rt.timestamp < input.getTimestampMs() ) {
              addressRelationMap.put( input.getIp(), new RelationTimestamp( input.getInstanceUuid(), input.getTimestampMs() ) );
            }
          }
        }
      } );
      foreach(ReportingElasticIpDetachEvent.class, new Callback<ReportingElasticIpDetachEvent>() {
        @Override
        public void fire( final ReportingElasticIpDetachEvent input ) {
          if ( addressRelationMap.containsKey( input.getIp() ) ) {
            RelationTimestamp rt = addressRelationMap.get(input.getIp());
            if ( rt != null && rt.relationId != null && rt.relationId.equals( input.getInstanceUuid() ) && rt.timestamp < input.getTimestampMs() ) {
              addressRelationMap.put( input.getIp(), null );
            }
          }
        }
      } );

      // Build S3 info
      final List<S3ObjectKey> s3ObjectList = Lists.newLinkedList();
      foreach(ReportingS3ObjectCreateEvent.class, new Callback<ReportingS3ObjectCreateEvent>() {
        @Override
        public void fire( final ReportingS3ObjectCreateEvent input ) {
          s3ObjectList.add( new S3ObjectKey( input.getS3BucketName(), input.getS3ObjectKey(), input.getObjectVersion() ) );
        }
      } );
      foreach(ReportingS3ObjectDeleteEvent.class, new Callback<ReportingS3ObjectDeleteEvent>() {
        @Override
        public void fire( final ReportingS3ObjectDeleteEvent input ) {
          s3ObjectList.remove( new S3ObjectKey(input.getS3BucketName(), input.getS3ObjectKey(), input.getObjectVersion() ) );
        }
      } );

      // Build volume info
      final Map<String,RelationTimestamp> volumeRelationMap = Maps.newHashMap();
      foreach( ReportingVolumeCreateEvent.class, new Callback<ReportingVolumeCreateEvent>() {
        @Override
        public void fire( final ReportingVolumeCreateEvent input ) {
          volumeRelationMap.put( input.getUuid(), null );
        }
      } );
      foreach(ReportingVolumeDeleteEvent.class, new Callback<ReportingVolumeDeleteEvent>() {
        @Override
        public void fire( final ReportingVolumeDeleteEvent input ) {
          volumeRelationMap.remove(input.getUuid());
        }
      } );
      foreach(ReportingVolumeAttachEvent.class, new Callback<ReportingVolumeAttachEvent>() {
        @Override
        public void fire( final ReportingVolumeAttachEvent input ) {
          if ( volumeRelationMap.containsKey( input.getVolumeUuid() ) ) {
            RelationTimestamp rt = volumeRelationMap.get(input.getVolumeUuid());
            if ( rt == null || rt.timestamp < input.getTimestampMs() ) {
              volumeRelationMap.put( input.getVolumeUuid(), new RelationTimestamp( input.getInstanceUuid(), input.getTimestampMs() ) );
            }
          }
        }
      } );
      foreach(ReportingVolumeDetachEvent.class, new Callback<ReportingVolumeDetachEvent>() {
        @Override
        public void fire( final ReportingVolumeDetachEvent input ) {
          if ( volumeRelationMap.containsKey( input.getVolumeUuid() ) ) {
            RelationTimestamp rt = volumeRelationMap.get(input.getVolumeUuid());
            if ( rt != null && rt.relationId.equals( input.getInstanceUuid() ) && rt.timestamp < input.getTimestampMs() ) {
              volumeRelationMap.put( input.getVolumeUuid(), null );
            }
          }
        }
      } );

      // Build snapshot info
      final List<String> snapshotList = Lists.newLinkedList();
      foreach(ReportingVolumeSnapshotCreateEvent.class, new Callback<ReportingVolumeSnapshotCreateEvent>() {
        @Override
        public void fire( final ReportingVolumeSnapshotCreateEvent input ) {
          snapshotList.add(input.getUuid());
        }
      } );
      foreach(ReportingVolumeSnapshotDeleteEvent.class, new Callback<ReportingVolumeSnapshotDeleteEvent>() {
        @Override
        public void fire( final ReportingVolumeSnapshotDeleteEvent input ) {
          snapshotList.remove(input.getUuid());
        }
      } );

      // Build view for extant resources
      final View view = new View();

      for ( final Map.Entry<String,RelationTimestamp> addressEntry : addressRelationMap.entrySet() ) {
        view.add( Address.class, addressEntry.getKey(), addressEntry.getValue()==null ? null : addressEntry.getValue().relationId );
      }

      for ( final S3ObjectKey s3ObjectKey : s3ObjectList ) {
        view.add( ObjectInfo.class, ReportingDataVerifier.s3ObjectResource(s3ObjectKey) );
      }

      for ( final Map.Entry<String,RelationTimestamp> volumeEntry : volumeRelationMap.entrySet() ) {
        view.add( Volume.class, volumeEntry.getKey(), volumeEntry.getValue()==null ? null : volumeEntry.getValue().relationId );
      }

      for ( final String snapshotUuid : snapshotList ) {
        view.add( Snapshot.class, snapshotUuid, null );
      }

      return view;
    }

    private <RET extends ReportingEventSupport> void foreach( final Class<RET> type,
                                                              final Callback<RET> callback ) {
      final EntityTransaction transaction = Entities.get( type );
      ScrollableResults results = null;
      try {
        results = Entities.createCriteria( type )
            .setCacheable( false )
            .setReadOnly( true )
            .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
            .scroll( ScrollMode.FORWARD_ONLY );

        while ( results.next() ) {
          final Object result = results.get( 0 );
          if ( type.isInstance( result ) ) {
            callback.fire( type.cast( result ) );
          }
        }
      } finally {
        if (results != null) try {
          results.close();
        } finally {
          transaction.rollback();
        } else {
          transaction.rollback();
        }
      }
    }
  }

  private static final class TypedResourceHolder {
    private final Class<?> type;
    private final List<ResourceWithRelation<?>> resources;

    private TypedResourceHolder( final Class<?> type ) {
      this.type = type;
      this.resources = Lists.newArrayList();
    }
  }

  private static abstract class ResourceKey {
  }

  private static final class SimpleResourceKey extends ResourceKey {
    private final String key;

    private SimpleResourceKey(final String key) {
      this.key = key;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final SimpleResourceKey that = (SimpleResourceKey) o;

      return key.equals(that.key);
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }

    public String toString() {
      return key;
    }
  }

  private static final class S3ObjectKey extends ResourceKey {
    @Nonnull  private final String bucketName;
    @Nonnull  private final String objectKey;
    @Nullable private final String objectVersion;

    private S3ObjectKey( @Nonnull  final String bucketName,
                         @Nonnull  final String objectKey,
                         @Nullable final String objectVersion  ) {
      this.bucketName = bucketName;
      this.objectKey = objectKey;
      this.objectVersion = objectVersion;
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final S3ObjectKey that = (S3ObjectKey) o;

      if ( !bucketName.equals( that.bucketName ) ) return false;
      if ( !objectKey.equals( that.objectKey ) ) return false;
      if ( objectVersion != null ? !objectVersion.equals( that.objectVersion ) : that.objectVersion != null )
        return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = bucketName.hashCode();
      result = 31 * result + objectKey.hashCode();
      result = 31 * result + (objectVersion != null ? objectVersion.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "ObjectKey[" +
          "bucket:" + bucketName +
          "; object:" + objectKey +
          "; version:" + objectVersion +
          ']';
    }
  }

  private static class ResourceWithRelation<KT extends ResourceKey> {
    @Nonnull  private final KT resourceKey;
    @Nullable private final String relationId;

    private ResourceWithRelation( @Nonnull final KT resourceKey,
                                  @Nullable final String relationId ) {
      this.resourceKey = resourceKey;
      this.relationId = relationId;
    }

    public String toString() {
      final StringBuilder builder = new StringBuilder();

      builder.append( "key:" ).append( resourceKey );
      if ( relationId != null ) {
        builder.append( "; relation:" ).append( relationId );
      }

      return builder.toString();
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final ResourceWithRelation that = (ResourceWithRelation) o;

      if ( relationId != null ? !relationId.equals( that.relationId ) : that.relationId != null ) return false;
      if ( !resourceKey.equals( that.resourceKey ) ) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = resourceKey.hashCode();
      result = 31 * result + (relationId != null ? relationId.hashCode() : 0);
      return result;
    }
  }

}
