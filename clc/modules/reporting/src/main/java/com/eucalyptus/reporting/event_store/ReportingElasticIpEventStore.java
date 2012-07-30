package com.eucalyptus.reporting.event_store;

import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.eucalyptus.entities.Entities;
import com.google.common.base.Preconditions;

/**
 * @author tom.werges
 */
public class ReportingElasticIpEventStore {
  private static final Logger LOG = Logger.getLogger( ReportingElasticIpEventStore.class );

  private static final ReportingElasticIpEventStore elasticIpCrud = new ReportingElasticIpEventStore();

  public static ReportingElasticIpEventStore getElasticIp() {
    return elasticIpCrud;
  }

  private ReportingElasticIpEventStore() {
  }

  public void insertCreateEvent( @Nonnull final String uuid,
                                          final long timestampMs,
                                 @Nonnull final String userId,
                                 @Nonnull final String ip ) {
    Preconditions.checkNotNull( uuid, "Uuid is required" );
    Preconditions.checkNotNull( userId, "UserId is required" );
    Preconditions.checkNotNull( ip, "IP is required" );

    persist( new ReportingElasticIpCreateEvent(uuid, timestampMs, userId, ip) );
  }

  public void insertDeleteEvent( @Nonnull final String uuid,
                                 final long timestampMs )
  {
    Preconditions.checkNotNull( uuid, "Uuid is required" );

    persist( new ReportingElasticIpDeleteEvent(uuid, timestampMs) );
  }

  public void insertAttachEvent( @Nonnull final String uuid,
                                 @Nonnull final String instanceUuid,
                                          final long timestampMs ) {
    Preconditions.checkNotNull( uuid, "Uuid is required" );
    Preconditions.checkNotNull( uuid, "InstanceUuid is required" );

    persist( new ReportingElasticIpAttachEvent( uuid, instanceUuid, timestampMs) );
  }

  public void insertDetachEvent( @Nonnull final String uuid,
                                 @Nonnull final String instanceUuid,
                                          final long timestampMs ) {
    Preconditions.checkNotNull( uuid, "Uuid is required" );
    Preconditions.checkNotNull( uuid, "InstanceUuid is required" );

    persist( new ReportingElasticIpDetachEvent( uuid, instanceUuid, timestampMs ) );
  }

  private void persist( final Object event ) {
    Entities.persist( event );
  }

}

