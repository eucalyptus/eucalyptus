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
