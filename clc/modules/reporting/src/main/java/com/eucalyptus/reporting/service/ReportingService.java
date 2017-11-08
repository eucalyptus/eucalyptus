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
package com.eucalyptus.reporting.service;

import com.eucalyptus.auth.AuthContextSupplier;
import static com.eucalyptus.component.id.Reporting.VENDOR_REPORTING;
import static com.eucalyptus.reporting.ReportGenerationFacade.ReportGenerationArgumentException;
import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.eucalyptus.component.Faults;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.Permissions;
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
@ConfigurableClass(root="reporting", description = "Reporting only parameters")
@ComponentNamed
public class ReportingService {

  private static final Logger logger = Logger.getLogger( ReportingService.class );
  @ConfigurableField(initial = "false", description = "Set this to false to stop reporting from populating new data")
  public static Boolean DATA_COLLECTION_ENABLED = false;
  private static final int DISABLED_SERVICE_FAULT_ID = 1501;
  private static boolean alreadyFaulted = false;
  public synchronized static void faultDisableReportingServiceIfNecessary() {
    if (!alreadyFaulted) {
      Faults.forComponent(Reporting.class).havingId(DISABLED_SERVICE_FAULT_ID).withVar("component", "reporting").log();
      alreadyFaulted = true;
    }
  }

  public ExportReportDataResponseType exportData( final ExportReportDataType request ) throws EucalyptusCloudException {
    final ExportReportDataResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );

    checkAuthorized();

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

    checkAuthorized();

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

    checkAuthorized();

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

  private static void checkAuthorized() throws ReportingException {
    final Context ctx = Contexts.lookup();
    final User requestUser = ctx.getUser( );
    final AuthContextSupplier requestUserSupplier = ctx.getAuthContext( );

    if ( !requestUser.isSystemUser() ||
        !Permissions.isAuthorized( VENDOR_REPORTING, "", "", null, getIamActionByMessageType(), requestUserSupplier ) ) {
      throw new ReportingException( HttpResponseStatus.UNAUTHORIZED, ReportingException.NOT_AUTHORIZED, "Not authorized");
    }
  }
}
