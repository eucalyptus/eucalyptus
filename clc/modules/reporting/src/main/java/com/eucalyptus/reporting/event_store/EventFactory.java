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
package com.eucalyptus.reporting.event_store;

import java.util.Date;

/**
 * Event factory used with import
 */
public class EventFactory {

  public static ReportingInstanceCreateEvent newInstanceCreate( final String eventId,
                                                                final Date created,
                                                                final Date occurred,
                                                                final String uuid,
                                                                final String instanceId,
                                                                final String instanceType,
                                                                final String userId,
                                                                final String availabilityZone ) {
    final ReportingInstanceCreateEvent event =
        new ReportingInstanceCreateEvent( uuid, instanceId, occurred.getTime(), instanceType, userId, availabilityZone );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingInstanceUsageEvent newInstanceUsage( final String eventId,
                                                              final Date created,
                                                              final Date occurred,
                                                              final String uuid,
                                                              final String metric,
                                                              final String dimension,
                                                              final Long sequence,
                                                              final Double value ) {
    final ReportingInstanceUsageEvent event =
        new ReportingInstanceUsageEvent( uuid, metric, sequence, dimension, value, occurred.getTime() );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingElasticIpAttachEvent newIpAttach( final String eventId,
                                                           final Date created,
                                                           final Date occurred,
                                                           final String uuid,
                                                           final String instanceUuid ) {
    final ReportingElasticIpAttachEvent event =
        new ReportingElasticIpAttachEvent( uuid, instanceUuid, occurred.getTime() );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingElasticIpCreateEvent newIpCreate( final String eventId,
                                                           final Date created,
                                                           final Date occurred,
                                                           final String uuid,
                                                           final String userId,
                                                           final String ip ) {
    final ReportingElasticIpCreateEvent event =
        new ReportingElasticIpCreateEvent( uuid, occurred.getTime(), ip, userId );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingElasticIpDetachEvent newIpDetach( final String eventId,
                                                           final Date created,
                                                           final Date occurred,
                                                           final String uuid,
                                                           final String instanceUuid ) {
    final ReportingElasticIpDetachEvent event =
        new ReportingElasticIpDetachEvent( uuid, instanceUuid, occurred.getTime() );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingElasticIpDeleteEvent newIpDelete( final String eventId,
                                                           final Date created,
                                                           final Date occurred,
                                                           final String uuid ) {
    final ReportingElasticIpDeleteEvent event =
        new ReportingElasticIpDeleteEvent( uuid, occurred.getTime() );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingS3ObjectCreateEvent newS3ObjectCreate( final String eventId,
                                                                final Date created,
                                                                final Date occurred,
                                                                final String bucketName,
                                                                final String objectKey,
                                                                final String objectVersion,
                                                                final Long size,
                                                                final String userId ) {
    final ReportingS3ObjectCreateEvent event =
        new ReportingS3ObjectCreateEvent( bucketName, objectKey, objectVersion, size, occurred.getTime(), userId );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingS3ObjectDeleteEvent newS3ObjectDelete( final String eventId,
                                                                final Date created,
                                                                final Date occurred,
                                                                final String bucketName,
                                                                final String objectKey,
                                                                final String objectVersion ) {
    final ReportingS3ObjectDeleteEvent event =
        new ReportingS3ObjectDeleteEvent( bucketName, objectKey, objectVersion, occurred.getTime() );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingVolumeCreateEvent newVolumeCreate( final String eventId,
                                                            final Date created,
                                                            final Date occurred,
                                                            final String uuid,
                                                            final String id,
                                                            final String userId,
                                                            final String availabilityZone,
                                                            final Long size ) {
    final ReportingVolumeCreateEvent event =
        new ReportingVolumeCreateEvent( uuid, id, occurred.getTime(), userId, availabilityZone, size );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingVolumeAttachEvent newVolumeAttach( final String eventId,
                                                            final Date created,
                                                            final Date occurred,
                                                            final String uuid,
                                                            final String instanceUuid,
                                                            final Long size ) {
    final ReportingVolumeAttachEvent event =
        new ReportingVolumeAttachEvent( uuid, instanceUuid, size, occurred.getTime() );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingVolumeDetachEvent newVolumeDetach( final String eventId,
                                                           final Date created,
                                                           final Date occurred,
                                                           final String uuid,
                                                           final String instanceUuid ) {
    final ReportingVolumeDetachEvent event =
        new ReportingVolumeDetachEvent(  uuid, instanceUuid, occurred.getTime()  );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingVolumeDeleteEvent newVolumeDelete( final String eventId,
                                                            final Date created,
                                                            final Date occurred,
                                                            final String uuid ) {
    final ReportingVolumeDeleteEvent event =
        new ReportingVolumeDeleteEvent( uuid, occurred.getTime() );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingVolumeSnapshotCreateEvent newSnapshotCreate( final String eventId,
                                                                      final Date created,
                                                                      final Date occurred,
                                                                      final String uuid,
                                                                      final String id,
                                                                      final String volumeUuid,
                                                                      final String userId,
                                                                      final Long size ) {
    final ReportingVolumeSnapshotCreateEvent event =
        new ReportingVolumeSnapshotCreateEvent( uuid, volumeUuid, id, occurred.getTime(), userId, size );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingVolumeSnapshotDeleteEvent newSnapshotDelete( final String eventId,
                                                                      final Date created,
                                                                      final Date occurred,
                                                                      final String uuid ) {
    final ReportingVolumeSnapshotDeleteEvent event =
        new ReportingVolumeSnapshotDeleteEvent( uuid, occurred.getTime() );
    event.initialize( eventId, created );
    return event;
  }

}
