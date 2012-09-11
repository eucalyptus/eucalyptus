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

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.reporting.ReportGenerator;
import com.eucalyptus.reporting.export.Export;
import com.eucalyptus.reporting.export.ReportingExport;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Charsets;

/**
 *
 */
public class ReportingService {

  private static final Logger logger = Logger.getLogger( ReportingService.class );

  public ExportReportDataResponseType exportData( final ExportReportDataType request ) throws EucalyptusCloudException {
    final ExportReportDataResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    final Context ctx = Contexts.lookup();
    final User requestUser = ctx.getUser( );

    if ( !requestUser.isSystemAdmin() ) {
      throw new ReportingException( HttpResponseStatus.UNUATHORIZED, ReportingException.NOT_AUTHORIZED, "Not authorized");
    }

    final ReportingExport export = Export.export( request.getStartDate(), request.getEndDate() );
    reply.setResult( new ExportDataResultType(export ) );

    return reply;
  }

  public GenerateReportResponseType generateReport( final GenerateReportType request ) throws EucalyptusCloudException {
    final GenerateReportResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    final Context ctx = Contexts.lookup();
    final User requestUser = ctx.getUser( );

    if ( !requestUser.isSystemAdmin() ) {
      throw new ReportingException( HttpResponseStatus.UNUATHORIZED, ReportingException.NOT_AUTHORIZED, "Not authorized");
    }

    long startTime = yesterday();
    long endTime = startTime + TimeUnit.DAYS.toMillis( 1L );
    if ( request.getStartDate() != null ) {
      startTime = request.getStartDate().getTime();
    }
    if ( request.getEndDate() != null ) {
      endTime = request.getStartDate().getTime();
    }
    if ( endTime <= startTime ) {
      throw new ReportingException( HttpResponseStatus.BAD_REQUEST, ReportingException.BAD_REQUEST, "Bad request: Invalid start or end date");
    }

    final ReportGenerator generator = ReportGenerator.getInstance();
    final ByteArrayOutputStream reportOutput = new ByteArrayOutputStream(10240);
    try {
      //TODO:STEVE: generate report
      //generator.generateReport( ... );
      reportOutput.write( ("REPORT CONTENT HERE\n type:" + request.getTypes() + ", start=" + startTime + ", end=" + endTime).getBytes(Charsets.UTF_8) );
    } catch ( final Exception e ) {
      logger.error( e, e );
      throw new ReportingException( HttpResponseStatus.INTERNAL_SERVER_ERROR, ReportingException.INTERNAL_SERVER_ERROR, "Error generating report");
    }

    reply.setResult( new GenerateReportResultType( new String( reportOutput.toByteArray(), Charsets.UTF_8 ) ) );

    return reply;
  }

  private static long yesterday() {
    final Calendar calendar = Calendar.getInstance();
    calendar.add( Calendar.DAY_OF_MONTH, -1 );
    calendar.set( Calendar.HOUR_OF_DAY, 0 );
    calendar.set( Calendar.MINUTE, 0 );
    calendar.set( Calendar.SECOND, 0 );
    calendar.set( Calendar.MILLISECOND, 0 );
    return calendar.getTimeInMillis();
  }
}
