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
package com.eucalyptus.reporting.service;

import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.reporting.event_store.*;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 *
 */
public class ReportingDataExportService {

  private static final Logger log = Logger.getLogger( ReportingDataExportService.class );

  public ExportDataResponseType exportData( final ExportDataType request ) throws EucalyptusCloudException {
    final ExportDataResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    final Context ctx = Contexts.lookup();
    final User requestUser = ctx.getUser( );

    if ( !requestUser.isSystemAdmin() ) {
      throw new ReportingException( HttpResponseStatus.UNUATHORIZED, ReportingException.NOT_AUTHORIZED, "Not authorized");
    }

    //TODO:STEVE: Export from DB
    //TODO:STEVE: User Iterator (Iterable) for collection mapping to allow streaming (iter-method="iterator")
    List<Class<? extends AbstractPersistent>> eventClasses = ImmutableList.of(
        ReportingElasticIpAttachEvent.class,
        ReportingElasticIpCreateEvent.class,
        ReportingElasticIpDeleteEvent.class,
        ReportingElasticIpDetachEvent.class,
        ReportingInstanceAttributeEvent.class,
        ReportingInstanceCreateEvent.class,
        ReportingInstanceUsageEvent.class,
        ReportingS3BucketCreateEvent.class,
        ReportingS3BucketDeleteEvent.class,
        ReportingS3ObjectCreateEvent.class,
        ReportingS3ObjectDeleteEvent.class,
        ReportingS3ObjectUsageEvent.class,
        ReportingVolumeAttachEvent.class,
        ReportingVolumeCreateEvent.class,
        ReportingVolumeDeleteEvent.class,
        ReportingVolumeDetachEvent.class,
        ReportingVolumeSnapshotCreateEvent.class,
        ReportingVolumeSnapshotDeleteEvent.class,
        ReportingVolumeUsageEvent.class
    );

    final List<Object> exportData = Lists.newArrayList();
    for ( final Class<? extends AbstractPersistent> eventClass : eventClasses ) {
      try {
        exportData.addAll( Entities.query( eventClass.newInstance(), true ) );
      } catch (InstantiationException e) {
        log.error( e, e );
      } catch (IllegalAccessException e) {
        log.error(e, e);
      }
    }

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final XMLEncoder encoder = new XMLEncoder( out );
    encoder.writeObject( exportData );
    encoder.flush();
    encoder.close();
    reply.setResult( new ExportDataResultType( out.toString() ) );

    return reply;
  }

}
