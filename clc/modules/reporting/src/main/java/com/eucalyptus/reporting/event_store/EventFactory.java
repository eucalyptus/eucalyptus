/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
                                                           final String ip,
                                                           final String instanceUuid ) {
    final ReportingElasticIpAttachEvent event =
        new ReportingElasticIpAttachEvent( ip, instanceUuid, occurred.getTime() );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingElasticIpCreateEvent newIpCreate( final String eventId,
                                                           final Date created,
                                                           final Date occurred,
                                                           final String userId,
                                                           final String ip ) {
    final ReportingElasticIpCreateEvent event =
        new ReportingElasticIpCreateEvent( occurred.getTime(), ip, userId );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingElasticIpDetachEvent newIpDetach( final String eventId,
                                                           final Date created,
                                                           final Date occurred,
                                                           final String ip,
                                                           final String instanceUuid ) {
    final ReportingElasticIpDetachEvent event =
        new ReportingElasticIpDetachEvent( ip, instanceUuid, occurred.getTime() );
    event.initialize( eventId, created );
    return event;
  }

  public static ReportingElasticIpDeleteEvent newIpDelete( final String eventId,
                                                           final Date created,
                                                           final Date occurred,
                                                           final String ip ) {
    final ReportingElasticIpDeleteEvent event =
        new ReportingElasticIpDeleteEvent( ip, occurred.getTime() );
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
