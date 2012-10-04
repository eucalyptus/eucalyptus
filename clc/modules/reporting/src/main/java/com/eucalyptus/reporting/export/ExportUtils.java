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
package com.eucalyptus.reporting.export;

import java.util.List;
import java.util.Map;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.event_store.EventFactory;
import com.eucalyptus.reporting.event_store.ReportingElasticIpAttachEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDetachEvent;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceUsageEvent;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeAttachEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeDetachEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotDeleteEvent;
import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

/**
 *
 */
public class ExportUtils {

  private static final List<Class<?>> supportClasses = ImmutableList.<Class<?>>of(
      ReportingUser.class,
      ReportingAccount.class
  );

  private static final List<Class<? extends ReportingEventSupport>> eventClasses = ImmutableList.of(
      ReportingElasticIpCreateEvent.class,
      ReportingElasticIpAttachEvent.class,
      ReportingElasticIpDetachEvent.class,
      ReportingElasticIpDeleteEvent.class,
      ReportingInstanceCreateEvent.class,
      ReportingS3ObjectCreateEvent.class,
      ReportingS3ObjectDeleteEvent.class,
      ReportingVolumeCreateEvent.class,
      ReportingVolumeAttachEvent.class,
      ReportingVolumeDetachEvent.class,
      ReportingVolumeDeleteEvent.class,
      ReportingVolumeSnapshotCreateEvent.class,
      ReportingVolumeSnapshotDeleteEvent.class
  );

  private static final List<Class<? extends ReportingEventSupport>> usageClasses = ImmutableList.<Class<? extends ReportingEventSupport>>of(
      ReportingInstanceUsageEvent.class
  );

  private static final BiMap<TypeAndAction,Class<? extends ReportingEventSupport>> typeActionToClass =
      ImmutableBiMap.<TypeAndAction,Class<? extends ReportingEventSupport>>builder()
          .put( typeAndAction( "ec2.ip", "Allocate" ), ReportingElasticIpCreateEvent.class )
          .put( typeAndAction( "ec2.ip", "Associate" ), ReportingElasticIpAttachEvent.class )
          .put( typeAndAction( "ec2.ip", "Disassociate" ), ReportingElasticIpDetachEvent.class )
          .put( typeAndAction( "ec2.ip", "Release" ), ReportingElasticIpDeleteEvent.class )
          .put( typeAndAction( "ec2.instance", "Create" ), ReportingInstanceCreateEvent.class )
          .put( typeAndAction( "ec2.instance", "Usage" ), ReportingInstanceUsageEvent.class )
          .put( typeAndAction( "s3.object", "Create" ), ReportingS3ObjectCreateEvent.class )
          .put( typeAndAction( "s3.object", "Delete" ), ReportingS3ObjectDeleteEvent.class )
          .put( typeAndAction( "ec2.volume", "Create" ), ReportingVolumeCreateEvent.class )
          .put( typeAndAction( "ec2.volume", "Attach" ), ReportingVolumeAttachEvent.class )
          .put( typeAndAction( "ec2.volume", "Detach" ), ReportingVolumeDetachEvent.class )
          .put( typeAndAction( "ec2.volume", "Delete" ), ReportingVolumeDeleteEvent.class )
          .put( typeAndAction( "ec2.snapshot", "Create" ), ReportingVolumeSnapshotCreateEvent.class )
          .put( typeAndAction( "ec2.snapshot", "Delete" ), ReportingVolumeSnapshotDeleteEvent.class )
          .build();

  private static final Map<Class<? extends ReportingEventSupport>,Function<ReportedAction,ReportingEventSupport>> fromActionFunctions =
      ImmutableMap.<Class<? extends ReportingEventSupport>,Function<ReportedAction,ReportingEventSupport>>builder()
          .put( ReportingElasticIpCreateEvent.class, FromEventFunctions.IP_CREATE )
          .put( ReportingElasticIpAttachEvent.class, FromEventFunctions.IP_ATTACH )
          .put( ReportingElasticIpDetachEvent.class, FromEventFunctions.IP_DETACH )
          .put( ReportingElasticIpDeleteEvent.class, FromEventFunctions.IP_DELETE )
          .put( ReportingInstanceCreateEvent.class, FromEventFunctions.INSTANCE_CREATE )
          .put( ReportingS3ObjectCreateEvent.class, FromEventFunctions.OBJECT_CREATE )
          .put( ReportingS3ObjectDeleteEvent.class, FromEventFunctions.OBJECT_DELETE )
          .put( ReportingVolumeCreateEvent.class, FromEventFunctions.VOLUME_CREATE )
          .put( ReportingVolumeAttachEvent.class, FromEventFunctions.VOLUME_ATTACH )
          .put( ReportingVolumeDetachEvent.class, FromEventFunctions.VOLUME_DETACH )
          .put( ReportingVolumeDeleteEvent.class, FromEventFunctions.VOLUME_DELETE )
          .put( ReportingVolumeSnapshotCreateEvent.class, FromEventFunctions.SNAPSHOT_CREATE )
          .put( ReportingVolumeSnapshotDeleteEvent.class, FromEventFunctions.SNAPSHOT_DELETE )
          .build();

  private static final Map<Class<? extends ReportingEventSupport>,Function<ReportingEventSupport,ReportedAction>> toActionFunctions =
      ImmutableMap.<Class<? extends ReportingEventSupport>,Function<ReportingEventSupport,ReportedAction>>builder()
          .put( ReportingElasticIpCreateEvent.class, ToEventFunctions.IP_CREATE )
          .put( ReportingElasticIpAttachEvent.class, ToEventFunctions.IP_ATTACH )
          .put( ReportingElasticIpDetachEvent.class, ToEventFunctions.IP_DETACH )
          .put( ReportingElasticIpDeleteEvent.class, ToEventFunctions.IP_DELETE )
          .put( ReportingInstanceCreateEvent.class, ToEventFunctions.INSTANCE_CREATE )
          .put( ReportingS3ObjectCreateEvent.class, ToEventFunctions.OBJECT_CREATE )
          .put( ReportingS3ObjectDeleteEvent.class, ToEventFunctions.OBJECT_DELETE )
          .put( ReportingVolumeCreateEvent.class, ToEventFunctions.VOLUME_CREATE )
          .put( ReportingVolumeAttachEvent.class, ToEventFunctions.VOLUME_ATTACH )
          .put( ReportingVolumeDetachEvent.class, ToEventFunctions.VOLUME_DETACH )
          .put( ReportingVolumeDeleteEvent.class, ToEventFunctions.VOLUME_DELETE )
          .put( ReportingVolumeSnapshotCreateEvent.class, ToEventFunctions.SNAPSHOT_CREATE )
          .put( ReportingVolumeSnapshotDeleteEvent.class, ToEventFunctions.SNAPSHOT_DELETE )
          .build();

  private static final Map<Class<? extends ReportingEventSupport>,Function<ReportedUsage,ReportingEventSupport>> fromUsageFunctions =
      ImmutableMap.<Class<? extends ReportingEventSupport>,Function<ReportedUsage,ReportingEventSupport>>builder()
          .put( ReportingInstanceUsageEvent.class, FromUsageBuilders.INSTANCE )
          .build();

  private static final Map<Class<? extends ReportingEventSupport>,Function<ReportingEventSupport,ReportedUsage>> toUsageFunctions =
      ImmutableMap.<Class<? extends ReportingEventSupport>,Function<ReportingEventSupport,ReportedUsage>>builder()
          .put( ReportingInstanceUsageEvent.class, ToUsageBuilders.INSTANCE )
          .build();

  public static Class<?> getTemplateClass() {
    return eventClasses.get( 0 );
  }

  public static Iterable<Class<? extends ReportingEventSupport>> getEventClasses() {
    return eventClasses;
  }

  public static Iterable<Class<? extends ReportingEventSupport>> getUsageClasses() {
    return usageClasses;
  }

  public static Iterable<Class<?>> getSupportClasses() {
    return supportClasses;
  }

  public static Iterable<Class<?>> getTimestampedClasses() {
    return Iterables.<Class<?>>concat(
        ExportUtils.eventClasses,
        ExportUtils.usageClasses );
  }

  public static Iterable<Class<?>> getPersistentClasses() {
    return Iterables.<Class<?>>concat(
        ExportUtils.eventClasses,
        ExportUtils.usageClasses,
        ExportUtils.supportClasses );
  }

  public static Function<ReportedAction,ReportingEventSupport> fromExportAction() {
    return FromExportAction.INSTANCE;
  }

  public static Function<ReportingEventSupport,ReportedAction> toExportAction() {
    return ToExportAction.INSTANCE;
  }

  public static Function<ReportedUsage,ReportingEventSupport> fromExportUsage() {
    return FromExportUsage.INSTANCE;
  }

  public static Function<ReportingEventSupport,ReportedUsage> toExportUsage(
      final List<? super ReportingEventSupport> actions ) {
    return new Function<ReportingEventSupport,ReportedUsage>(){
      @Override
      public ReportedUsage apply( final ReportingEventSupport reportingEventSupport ) {
        if ( eventClasses.contains( reportingEventSupport.getClass() ) ) {
          if (actions != null) actions.add( reportingEventSupport );
          return null;
        } else {
          final Function<ReportingEventSupport,ReportedUsage> builder = toUsageFunctions.get( reportingEventSupport.getClass() );
          return builder.apply( reportingEventSupport );
        }
      }
    };
  }

  private static TypeAndAction typeAndAction( final String type, final String action ) {
    return new TypeAndAction( type, action );
  }

  private enum FromExportAction implements Function<ReportedAction,ReportingEventSupport>{
    INSTANCE;

    @Override
    public ReportingEventSupport apply( final ReportedAction reportedAction ) {
      final Class<? extends ReportingEventSupport> eventClass =
          typeActionToClass.get( typeAndAction( reportedAction.getType(), reportedAction.getAction()) );
      final Function<ReportedAction,ReportingEventSupport> builder = fromActionFunctions.get(eventClass);
      return builder.apply( reportedAction );
    }
  }

  private enum ToExportAction implements Function<ReportingEventSupport,ReportedAction>{
    INSTANCE;

    @Override
    public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
      final Function<ReportingEventSupport,ReportedAction> builder = toActionFunctions.get( reportingEventSupport.getClass() );
      return builder.apply( reportingEventSupport );
    }
  }

  private enum FromExportUsage implements Function<ReportedUsage,ReportingEventSupport>{
    INSTANCE;

    @Override
    public ReportingEventSupport apply( final ReportedUsage reportedUsage ) {
      final Class<? extends ReportingEventSupport> eventClass =
          typeActionToClass.get( typeAndAction( reportedUsage.getType(), "Usage" ) );
      final Function<ReportedUsage,ReportingEventSupport> builder = fromUsageFunctions.get(eventClass );
      return builder.apply( reportedUsage );
    }
  }

  private enum FromEventFunctions implements Function<ReportedAction,ReportingEventSupport> {
    IP_CREATE {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newIpCreate(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getUuid(),
            reportedAction.getUserId(),
            reportedAction.getId() );
      }
    },
    IP_ATTACH {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newIpAttach(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getUuid(),
            reportedAction.getInstanceUuid() );
      }
    },
    IP_DETACH {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newIpDetach(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getUuid(),
            reportedAction.getInstanceUuid() );
      }
    },
    IP_DELETE {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newIpDelete(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getUuid() );
      }
    },
    INSTANCE_CREATE {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newInstanceCreate(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getUuid(),
            reportedAction.getId(),
            reportedAction.getSubType(),
            reportedAction.getUserId(),
            reportedAction.getScope() );
      }
    },
    OBJECT_CREATE {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newS3ObjectCreate(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getScope(),
            reportedAction.getId(),
            reportedAction.getVersion(),
            reportedAction.getSize(),
            reportedAction.getUserId() );
      }
    },
    OBJECT_DELETE {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newS3ObjectDelete(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getScope(),
            reportedAction.getId(),
            reportedAction.getVersion() );
      }
    },
    VOLUME_CREATE {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newVolumeCreate(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getUuid(),
            reportedAction.getId(),
            reportedAction.getUserId(),
            reportedAction.getScope(),
            reportedAction.getSize() );
      }
    },
    VOLUME_ATTACH {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newVolumeAttach(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getUuid(),
            reportedAction.getInstanceUuid(),
            reportedAction.getSize() );
      }
    },
    VOLUME_DETACH {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newVolumeDetach(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getUuid(),
            reportedAction.getInstanceUuid() );
      }
    },
    VOLUME_DELETE {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newVolumeDelete(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getUuid() );
      }
    },
    SNAPSHOT_CREATE {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newSnapshotCreate(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getUuid(),
            reportedAction.getId(),
            reportedAction.getVolumeUuid(),
            reportedAction.getUserId(),
            reportedAction.getSize() );
      }
    },
    SNAPSHOT_DELETE {
      @Override
      public ReportingEventSupport apply( final ReportedAction reportedAction ) {
        return EventFactory.newSnapshotDelete(
            reportedAction.getEventUuid(),
            reportedAction.getCreated(),
            reportedAction.getOccurred(),
            reportedAction.getUuid() );
      }
    },
  }

  private enum ToEventFunctions implements Function<ReportingEventSupport,ReportedAction> {
    IP_CREATE {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingElasticIpCreateEvent createEvent =
            (ReportingElasticIpCreateEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "ec2.ip" );
        action.setAction( "Allocate" );
        action.setUuid( createEvent.getUuid() );
        action.setId( createEvent.getIp() );
        action.setUserId( createEvent.getUserId() );
        return action;
      }
    },
    IP_ATTACH {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingElasticIpAttachEvent attachEvent =
            (ReportingElasticIpAttachEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "ec2.ip" );
        action.setAction( "Associate" );
        action.setUuid( attachEvent.getIpUuid() );
        action.setInstanceUuid( attachEvent.getInstanceUuid() );
        return action;
      }
    },
    IP_DETACH {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingElasticIpDetachEvent detachEvent =
            (ReportingElasticIpDetachEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "ec2.ip" );
        action.setAction( "Disassociate" );
        action.setUuid( detachEvent.getIpUuid() );
        action.setInstanceUuid( detachEvent.getInstanceUuid() );
        return action;
      }
    },
    IP_DELETE {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingElasticIpDeleteEvent deleteEvent =
            (ReportingElasticIpDeleteEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "ec2.ip" );
        action.setAction( "Release" );
        action.setUuid( deleteEvent.getUuid() );
        return action;
      }
    },
    INSTANCE_CREATE {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingInstanceCreateEvent createEvent =
            (ReportingInstanceCreateEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "ec2.instance" );
        action.setAction( "Create" );
        action.setUuid( createEvent.getUuid() );
        action.setId( createEvent.getInstanceId() );
        action.setSubType( createEvent.getInstanceType() );
        action.setScope( createEvent.getAvailabilityZone() );
        action.setUserId( createEvent.getUserId() );
        return action;
      }
    },
    OBJECT_CREATE {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingS3ObjectCreateEvent createEvent =
            (ReportingS3ObjectCreateEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "s3.object" );
        action.setAction( "Create" );
        action.setScope( createEvent.getS3BucketName() );
        action.setId( createEvent.getS3ObjectKey() );
        action.setVersion( createEvent.getObjectVersion() );
        action.setSize( createEvent.getSizeGB() );
        action.setUserId( createEvent.getUserId() );
        return action;
      }
    },
    OBJECT_DELETE {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingS3ObjectDeleteEvent deleteEvent =
            (ReportingS3ObjectDeleteEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "s3.object" );
        action.setAction( "Delete" );
        action.setScope( deleteEvent.getS3BucketName() );
        action.setId( deleteEvent.getS3ObjectKey() );
        action.setVersion( deleteEvent.getObjectVersion() );
        return action;
      }
    },
    VOLUME_CREATE {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingVolumeCreateEvent createEvent =
            (ReportingVolumeCreateEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "ec2.volume" );
        action.setAction( "Create" );
        action.setUuid( createEvent.getUuid() );
        action.setId( createEvent.getVolumeId() );
        action.setUserId( createEvent.getUserId() );
        action.setScope( createEvent.getAvailabilityZone() );
        action.setSize( createEvent.getSizeGB() );
        return action;
      }
    },
    VOLUME_ATTACH {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingVolumeAttachEvent attachEvent =
            (ReportingVolumeAttachEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "ec2.volume" );
        action.setAction( "Attach" );
        action.setUuid( attachEvent.getVolumeUuid() );
        action.setInstanceUuid( attachEvent.getInstanceUuid() );
        action.setSize( attachEvent.getSizeGB() );
        return action;
      }
    },
    VOLUME_DETACH {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingVolumeDetachEvent detachEvent =
            (ReportingVolumeDetachEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "ec2.volume" );
        action.setAction( "Detach" );
        action.setUuid( detachEvent.getVolumeUuid() );
        action.setInstanceUuid( detachEvent.getInstanceUuid() );
        return action;
      }
    },
    VOLUME_DELETE {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingVolumeDeleteEvent deleteEvent =
            (ReportingVolumeDeleteEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "ec2.volume" );
        action.setAction( "Delete" );
        action.setUuid( deleteEvent.getUuid() );
        return action;
      }
    },
    SNAPSHOT_CREATE {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingVolumeSnapshotCreateEvent createEvent =
            (ReportingVolumeSnapshotCreateEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "ec2.snapshot" );
        action.setAction( "Create" );
        action.setUuid( createEvent.getUuid() );
        action.setId( createEvent.getVolumeSnapshotId() );
        action.setVolumeUuid( createEvent.getVolumeUuid() );
        action.setUserId( createEvent.getUserId() );
        action.setSize( createEvent.getSizeGB() );
        return action;
      }
    },
    SNAPSHOT_DELETE {
      @Override
      public ReportedAction apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingVolumeSnapshotDeleteEvent deleteEvent =
            (ReportingVolumeSnapshotDeleteEvent) reportingEventSupport;
        final ReportedAction action = new ReportedAction( reportingEventSupport );
        action.setType( "ec2.snapshot" );
        action.setAction( "Delete" );
        action.setUuid( deleteEvent.getUuid() );
        return action;
      }
    },
  }

  private enum ToUsageBuilders implements Function<ReportingEventSupport,ReportedUsage> {
    INSTANCE {
      @Override
      public ReportedUsage apply( final ReportingEventSupport reportingEventSupport ) {
        final ReportingInstanceUsageEvent usageEvent =
            (ReportingInstanceUsageEvent) reportingEventSupport;
        final ReportedUsage usage = new ReportedUsage( reportingEventSupport );
        usage.setId( usageEvent.getUuid() );
        usage.setMetric( usageEvent.getMetric() );
        usage.setDimension( usageEvent.getDimension() );
        usage.setSequence( usageEvent.getSequenceNum() );
        usage.setValue( usageEvent.getValue() );
        usage.setType( "ec2.instance" );
        return usage;
      }
    }
  }

  private enum FromUsageBuilders implements Function<ReportedUsage,ReportingEventSupport> {
    INSTANCE {
      @Override
      public ReportingEventSupport apply( final ReportedUsage reportedUsage ) {
        return EventFactory.newInstanceUsage(
            reportedUsage.getEventUuid(),
            reportedUsage.getCreated(),
            reportedUsage.getOccurred(),
            reportedUsage.getId(),
            reportedUsage.getMetric(),
            reportedUsage.getDimension(),
            reportedUsage.getSequence(),
            reportedUsage.getValue() );
      }
    }
  }

  private static final class TypeAndAction {
    private final String type;
    private final String action;

    private TypeAndAction( final String type, final String action ) {
      this.type = type;
      this.action = action;
    }

    public String getType() {
      return type;
    }

    public String getAction() {
      return action;
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final TypeAndAction that = (TypeAndAction) o;

      if ( !action.equals( that.action ) ) return false;
      if ( !type.equals( that.type ) ) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = type.hashCode();
      result = 31 * result + action.hashCode();
      return result;
    }
  }
}
