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
package com.eucalyptus.reporting.dw.commands;

import static com.eucalyptus.reporting.ReportGenerationFacade.ReportGenerationException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.ReportGenerationFacade;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Data warehouse report generation command, invoked from Python wrapper.
 */
public class ReportCommand extends CommandSupport {

  public ReportCommand(final String[] args) {
    super(argumentsBuilder()
        .withArg("t", "type", "The type for the generated report", false)
        .withArg("f", "format", "The format for the generated report", false)
        .withArg("s", "start", "The inclusive start time for the report period (e.g. 2012-08-19T00:00:00)", false)
        .withArg("e", "end", "The exclusive end time for the report period (e.g. 2012-08-26T00:00:00)", false)
        .withArg( "r", "report", "File for generated report", false )
        .forArgs(args));
  }

  @Override
  protected void runCommand( final Arguments arguments ) {
    final Period defaultPeriod = Period.defaultPeriod();
    final String type = arguments.getArgument( "type", "instance" );
    final String format = arguments.getArgument( "format", "html" );
    final String start = arguments.getArgument( "start", formatDate(defaultPeriod.getBeginningMs()) );
    final String end = arguments.getArgument( "end", formatDate(defaultPeriod.getEndingMs()) );
    final String reportFilename = arguments.getArgument( "file", null );

    long startTime = parseDate( start );
    long endTime = parseDate( end );
    if ( endTime <= startTime ) {
      throw new IllegalArgumentException( "Invalid start or end time");
    }

    final String reportData;
    try {
      reportData = ReportGenerationFacade.generateReport( type, format, startTime, endTime );
    } catch ( ReportGenerationException e ) {
      throw Exceptions.toUndeclared( e );
    }

    if ( reportFilename != null ) {
      try {
        Files.write( reportData, new File(reportFilename), Charsets.UTF_8);
      } catch ( IOException  e) {
        throw Exceptions.toUndeclared( e );
      }
    } else {
      System.out.println( reportData );
    }
  }

  private String formatDate( final long time ) {
    return getDateFormat(true).format(new Date(time));
  }

  private long parseDate( final String date ) {
    try {
      return getDateFormat( date.endsWith("Z") ).parse( date ).getTime();
    } catch (ParseException e) {
      throw Exceptions.toUndeclared( e );
    }
  }

  private DateFormat getDateFormat( final boolean utc ) {
    final String format = "yyyy-MM-dd'T'HH:mm:ss";
    final SimpleDateFormat dateFormat = new SimpleDateFormat( format + ( utc ? "'Z'" : "")  );
    if (utc) {
      dateFormat.setTimeZone( TimeZone.getTimeZone("UTC") );
    }
    return dateFormat;
  }

  public static void main( final String[] args ) {
    new ReportCommand( args ).run();
  }
}
