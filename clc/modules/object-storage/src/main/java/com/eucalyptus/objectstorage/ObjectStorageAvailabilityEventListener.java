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
package com.eucalyptus.objectstorage;

import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Availability;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType.StorageWalrus;

import com.eucalyptus.objectstorage.entities.ObjectStorageGlobalConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.objectstorage.policy.ObjectStorageQuotaUtil;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent;


/**
 *  Event listener that fires ResourceAvailabilityEvents for the ObjectStorage.
 */
public class ObjectStorageAvailabilityEventListener implements EventListener<ClockTick> {
  private static Logger logger = Logger.getLogger( ObjectStorageAvailabilityEventListener.class );

  public static void register( ) {
    Listeners.register(ClockTick.class, new ObjectStorageAvailabilityEventListener());
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    if ( BootstrapArgs.isCloudController() && Bootstrap.isOperational() ) {
      try {
      	long capacity = 0;      	
      	capacity = ObjectStorageGlobalConfiguration.max_total_reporting_capacity_gb;

      	ListenerRegistry.getInstance().fireEvent(
      			new ResourceAvailabilityEvent( StorageWalrus, new Availability(
      					capacity,
      					Math.max( 0, capacity - (long) Math.ceil( (double) ObjectStorageQuotaUtil.getTotalObjectSize() / FileUtils.ONE_GB ) )
      					) )      			
      			);
      } catch ( Exception ex ) {
        logger.error( ex, ex );
      }
    }
  }
}
