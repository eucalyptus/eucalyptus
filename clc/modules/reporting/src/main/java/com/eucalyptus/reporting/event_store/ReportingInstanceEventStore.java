package com.eucalyptus.reporting.event_store;

import javax.annotation.Nonnull;
import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.google.common.base.Preconditions;

/**
 * @author tom.werges
 */
public class ReportingInstanceEventStore {
  private static final Logger LOG = Logger.getLogger( ReportingInstanceEventStore.class );

  private static final ReportingInstanceEventStore instance = new ReportingInstanceEventStore( );

  public static ReportingInstanceEventStore getInstance( ) {
    return instance;
  }

  private ReportingInstanceEventStore( ) { }

  public void insertCreateEvent( @Nonnull final String uuid,
                                          final long timestampMs,
                                 @Nonnull final String instanceId,
                                 @Nonnull final String instanceType,
                                 @Nonnull final String userId,
                                 @Nonnull final String clusterName,
                                 @Nonnull final String availabilityZone ) {
    Preconditions.checkNotNull( uuid, "Uuid is required" );
    Preconditions.checkNotNull( instanceId, "InstanceId is required" );
    Preconditions.checkNotNull( instanceType, "InstanceType is required" );
    Preconditions.checkNotNull( userId, "UserId is required" );
    Preconditions.checkNotNull( clusterName, "ClusterName is required" );
    Preconditions.checkNotNull( availabilityZone, "AvailabilityZone is required" );

    persist(
        new ReportingInstanceCreateEvent(
            uuid,
            timestampMs,
            instanceId,
            instanceType,
            userId,
            clusterName,
            availabilityZone ) );
  }

  public void insertUsageEvent( @Nonnull final String uuid,
                                         final long timestampMs,
                                @Nonnull final Long cumulativeNetIoMegs,
                                @Nonnull final Long cumulativeDiskIoMegs,
                                @Nonnull final Integer cpuUtilizationPercent ) {
    Preconditions.checkNotNull( uuid, "Uuid is required" );
    Preconditions.checkNotNull( cumulativeNetIoMegs, "CumulativeNetIoMegs is required" );
    Preconditions.checkNotNull( cumulativeDiskIoMegs, "CumulativeDiskIoMegs is required" );
    Preconditions.checkNotNull( cpuUtilizationPercent, "CpuUtilizationPercent is required" );

    persist(
        new ReportingInstanceUsageEvent(
            uuid,
            timestampMs,
            cumulativeNetIoMegs,
            cumulativeDiskIoMegs,
            cpuUtilizationPercent ) );
  }

  private void persist( final Object event ) {
    Entities.persist( event );
  }
}
