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
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceUsageEvent;
import com.eucalyptus.reporting.export.Export;
import com.eucalyptus.reporting.export.ReportingExport;
import com.eucalyptus.ws.util.SerializationUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

/**
 *
 */
public class ReportGenerationFacade {

    public static String generateReport( @Nonnull  final String type,
                                                   final long start,
                                                   final long end,
                                         @Nullable final String accountId ) throws ReportGenerationException {
      final String report;
      if ( !"raw".equals(type) ) {
        final ReportGenerator generator = ReportGenerator.getInstance();
        final ByteArrayOutputStream reportOutput = new ByteArrayOutputStream(10240);
        try {
          generator.generateReport( new Period( start, end ), ReportFormat.HTML, ReportType.valueOf(type.toUpperCase()), null, reportOutput, null );
        } catch ( final Exception e ) {
          throw new ReportGenerationException( "Error generating report", e );
        }

        report = new String( reportOutput.toByteArray(), Charsets.UTF_8 );
      } else {
        //TODO:STEVE: remove temporary "raw" report type
        final ReportingExport export = Export.export(new Date(start), new Date(end));
        final StringBuilder builder = new StringBuilder(10240);

        final Map<String,String> uuidToInstanceIdMap = Maps.newHashMap();
        for ( final Serializable item : export ) {
          if ( item instanceof ReportingInstanceCreateEvent ) {
            final ReportingInstanceCreateEvent event = (ReportingInstanceCreateEvent) item;
            uuidToInstanceIdMap.put( event.getUuid(), event.getInstanceId() );
          }
          if ( item instanceof ReportingInstanceUsageEvent) {
            final ReportingInstanceUsageEvent event = (ReportingInstanceUsageEvent) item;
            builder.append( SerializationUtils.serializeDateTime( new Date( event.getTimestampMs() ) ) ).append(", ");
            builder.append( event.getUuid() ).append( ", ");
            builder.append( uuidToInstanceIdMap.get(event.getUuid()) ).append( ", ");
            builder.append( event.getCpuUtilizationPercent() ).append( ", ");
            builder.append( event.getCumulativeDiskIoMegs() ).append( ", ");
            builder.append( event.getCumulativeNetIncomingMegsPublic() ).append( ", ");
            builder.append( event.getCumulativeNetIncomingMegsBetweenZones() ).append(", ");
            builder.append( event.getCumulativeNetIncomingMegsWithinZone() ).append( ", ");
            builder.append( event.getCumulativeNetOutgoingMegsPublic() ).append( ", ");
            builder.append( event.getCumulativeNetOutgoingMegsBetweenZones() ).append(", ");
            builder.append( event.getCumulativeNetOutgoingMegsWithinZone() ).append("\n");
          }
        }

        report = builder.toString();
      }

      return report;
    }


    public static final class ReportGenerationException extends Exception {
      private static final long serialVersionUID = 1L;

      public ReportGenerationException( final String message ) {
        super(message);
      }

      public ReportGenerationException( final String message,
                                        final Throwable cause ) {
        super(message, cause);
      }
    }
}
