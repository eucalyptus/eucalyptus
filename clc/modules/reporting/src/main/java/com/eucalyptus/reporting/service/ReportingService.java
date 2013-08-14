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

import static com.eucalyptus.reporting.ReportGenerationFacade.ReportGenerationArgumentException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.ReportGenerationFacade;
import com.eucalyptus.reporting.export.Export;
import com.eucalyptus.reporting.export.Import;
import com.eucalyptus.reporting.export.ReportingExport;
import com.eucalyptus.reporting.units.SizeUnit;
import com.eucalyptus.reporting.units.TimeUnit;
import com.eucalyptus.reporting.units.Units;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;
import com.google.common.base.Objects;

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
      throw new ReportingException( HttpResponseStatus.UNAUTHORIZED, ReportingException.NOT_AUTHORIZED, "Not authorized");
    }

    Date startDate = null;
    Date endDate = null;
    if ( request.getStartDate() != null ) {
      startDate = new Date( parseDate( request.getStartDate() ) );
    }
    if ( request.getEndDate() != null ) {
      endDate = new Date( parseDate( request.getEndDate() ) );
    }
    if ( endDate != null && startDate != null && endDate.getTime() <= startDate.getTime() ) {
      throw new ReportingException( HttpResponseStatus.BAD_REQUEST, ReportingException.BAD_REQUEST, "Bad request: Invalid start or end date");
    }

    final ReportingExport export = Export.export(
        startDate,
        endDate,
        request.isDependencies() );
    reply.setResult( new ExportDataResultType(export ) );

    logger.info( "Exporting report data from " +
        Objects.firstNonNull( request.getStartDate(), "-" ) + " to " +
        Objects.firstNonNull( request.getEndDate(), "-" ) );

    try {
      RestfulMarshallingHandler.streamResponse( reply );
    } catch ( final Exception e ) {
      logger.error( e, e );
      throw new ReportingException( HttpResponseStatus.INTERNAL_SERVER_ERROR, ReportingException.INTERNAL_SERVER_ERROR, "Error exporting data");
    }

    return null;
  }

  public DeleteReportDataResponseType deleteData( final DeleteReportDataType request ) throws EucalyptusCloudException {
    final DeleteReportDataResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    final Context ctx = Contexts.lookup();
    final User requestUser = ctx.getUser( );

    if ( !requestUser.isSystemAdmin() ) {
      throw new ReportingException( HttpResponseStatus.UNAUTHORIZED, ReportingException.NOT_AUTHORIZED, "Not authorized");
    }

    Date endDate;
    if ( request.getEndDate() != null ) {
      endDate = new Date( parseDate( request.getEndDate() ) );
    } else {
      throw new ReportingException( HttpResponseStatus.BAD_REQUEST, ReportingException.BAD_REQUEST, "Bad request: End date is required");
    }

    logger.info( "Deleting report data up to " + request.getEndDate() );

    try {
      reply.setResult( new DeleteDataResultType( Import.deleteAll( endDate ) ) );
    } catch ( final Exception e ) {
      logger.error( e, e );
      throw new ReportingException( HttpResponseStatus.INTERNAL_SERVER_ERROR, ReportingException.INTERNAL_SERVER_ERROR, "Error deleting report data");
    }

    return reply;
  }

  public GenerateReportResponseType generateReport( final GenerateReportType request ) throws EucalyptusCloudException {
    final GenerateReportResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    final Context ctx = Contexts.lookup();
    final User requestUser = ctx.getUser( );

    if ( !requestUser.isSystemAdmin() ) {
      throw new ReportingException( HttpResponseStatus.UNAUTHORIZED, ReportingException.NOT_AUTHORIZED, "Not authorized");
    }

    final Period period = Period.defaultPeriod();
    long startTime = period.getBeginningMs();
    long endTime = period.getEndingMs();
    if ( request.getStartDate() != null ) {
      startTime = parseDate( request.getStartDate() );
    }
    if ( request.getEndDate() != null ) {
      endTime = parseDate( request.getEndDate() );
    }

    final String reportData;
    try {
      reportData = ReportGenerationFacade.generateReport(
          Objects.firstNonNull( request.getType(), "instance" ),
          Objects.firstNonNull( request.getFormat(), "html" ),
          units(request),
          startTime,
          endTime );
    } catch ( final ReportGenerationArgumentException e ) {
      throw new ReportingException( HttpResponseStatus.BAD_REQUEST, ReportingException.BAD_REQUEST, "Bad request: Invalid start or end date");
    } catch ( final Exception e ) {
      logger.error( e, e );
      throw new ReportingException( HttpResponseStatus.INTERNAL_SERVER_ERROR, ReportingException.INTERNAL_SERVER_ERROR, "Error generating report");
    }

    reply.setResult( new GenerateReportResultType( reportData ) );

    return reply;
  }

  private static Units units( final GenerateReportType request ) throws ReportGenerationArgumentException {
    return new Units(
      TimeUnit.fromString( request.getTimeUnit(), TimeUnit.DAYS ),
      SizeUnit.fromString( request.getSizeUnit(), SizeUnit.GB ),
      TimeUnit.fromString( firstNonNullOrNull( request.getSizeTimeTimeUnit(), request.getTimeUnit() ), TimeUnit.DAYS ),
      SizeUnit.fromString( firstNonNullOrNull( request.getSizeTimeSizeUnit(), request.getSizeUnit() ), SizeUnit.GB )
    );
  }

  private static <T> T firstNonNullOrNull( final T... items ) {
    T value = null;
    for ( final T item : items ) if ( item != null ) {
      value = item;
      break;
    }
    return value;
  }

  private static long parseDate( final String date ) throws ReportingException {
    try {
      return getDateFormat( date.endsWith("Z") ).parse( date ).getTime();
    } catch (ParseException e) {
      throw new ReportingException( HttpResponseStatus.BAD_REQUEST, ReportingException.BAD_REQUEST, "Bad request: Invalid start or end date");
    }
  }

  private static DateFormat getDateFormat( final boolean utc ) {
    final String format = "yyyy-MM-dd'T'HH:mm:ss";
    final SimpleDateFormat dateFormat = new SimpleDateFormat( format + ( utc ? "'Z'" : "")  );
    if (utc) {
      dateFormat.setTimeZone( TimeZone.getTimeZone("UTC") );
    }
    return dateFormat;
  }
}
