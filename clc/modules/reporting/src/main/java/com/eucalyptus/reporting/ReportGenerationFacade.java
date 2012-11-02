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
package com.eucalyptus.reporting;

import java.io.ByteArrayOutputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.reporting.units.Units;
import com.google.common.base.Charsets;

/**
 *
 */
public class ReportGenerationFacade {

  public static String generateReport( @Nonnull  final String type,
                                       @Nonnull  final String format,
                                       final long start,
                                       final long end ) throws ReportGenerationException {
    return generateReport( type, format, null, start, end );
  }

  public static String generateReport( @Nonnull  final String type,
                                         @Nonnull  final String format,
                                         @Nullable final Units units,
                                                   final long start,
                                                   final long end ) throws ReportGenerationException {
    final long maxEndTime = System.currentTimeMillis();
    final long adjustedEndTime = end > maxEndTime ? maxEndTime : end;
    if ( start >= adjustedEndTime ) {
      throw new ReportGenerationArgumentException( "Invalid report period" );
    }

    final ReportGenerator generator = ReportGenerator.getInstance();
    final ByteArrayOutputStream reportOutput = new ByteArrayOutputStream(10240);
    try {
      generator.generateReport(
          new Period( start, adjustedEndTime ),
          ReportFormat.valueOf(format.toUpperCase()),
          ReportType.valueOf(type.toUpperCase().replace('-','_')),
          units,
          reportOutput );
    } catch ( final Exception e ) {
      throw new ReportGenerationException( "Error generating report", e );
    }

    return new String( reportOutput.toByteArray(), Charsets.UTF_8 );
  }

  public static class ReportGenerationException extends Exception {
    private static final long serialVersionUID = 1L;

    public ReportGenerationException( final String message ) {
      super(message);
    }

    public ReportGenerationException( final String message,
                                      final Throwable cause ) {
      super(message, cause);
    }
  }

  public static final class ReportGenerationArgumentException extends ReportGenerationException {
    private static final long serialVersionUID = 1L;

    public ReportGenerationArgumentException( final String message ) {
      super(message);
    }
  }
}
