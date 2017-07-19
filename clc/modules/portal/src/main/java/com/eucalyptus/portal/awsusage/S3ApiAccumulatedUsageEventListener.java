/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.portal.awsusage;

import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.portal.BillingProperties;
import com.eucalyptus.reporting.event.S3ApiAccumulatedUsageEvent;

/**
 *
 */
public class S3ApiAccumulatedUsageEventListener extends SensorQueueEventListener<S3ApiAccumulatedUsageEvent> {
  private static final Logger LOG = Logger.getLogger( S3ApiAccumulatedUsageEventListener.class );

  public static void register( ) {
    Listeners.register( S3ApiAccumulatedUsageEvent.class, new S3ApiAccumulatedUsageEventListener( ) );
  }

  @Override
  public void fireEvent( @Nonnull final S3ApiAccumulatedUsageEvent event ) {
    if ( !Bootstrap.isOperational( ) || !BillingProperties.ENABLED ) {
      return;
    }

    if (event.getValueType() == S3ApiAccumulatedUsageEvent.ValueType.Counts) {
      transformAndQueue( LOG, event, QueuedEvents.FromS3ApiAccumulatedCountsEvent );
    } else if (event.getValueType() == S3ApiAccumulatedUsageEvent.ValueType.Bytes) {
      transformAndQueue( LOG, event, QueuedEvents.FromS3ApiAccumulatedBytesEvent );
    }
  }
}
