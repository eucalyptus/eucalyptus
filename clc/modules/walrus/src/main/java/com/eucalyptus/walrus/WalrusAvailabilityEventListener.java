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
package com.eucalyptus.walrus;

import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Availability;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType.StorageWalrus;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent;
import com.eucalyptus.walrus.entities.WalrusInfo;
import com.eucalyptus.walrus.policy.WalrusQuotaUtil;


/**
 *  Event listener that fires ResourceAvailabilityEvents for the Walrus.
 */
public class WalrusAvailabilityEventListener implements EventListener<ClockTick> {
  private static Logger logger = Logger.getLogger( WalrusAvailabilityEventListener.class );

  public static void register( ) {
    Listeners.register(ClockTick.class, new WalrusAvailabilityEventListener());
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    if ( BootstrapArgs.isCloudController() && Bootstrap.isOperational() ) {
      try {

      	WalrusInfo wInfo = WalrusInfo.getWalrusInfo();
      	long capacity = 0;
      	if(wInfo != null) capacity = wInfo.getStorageMaxTotalCapacity();

      	ListenerRegistry.getInstance().fireEvent(
      			new ResourceAvailabilityEvent( StorageWalrus, new Availability(
      					capacity,
      					Math.max( 0, capacity - (long) Math.ceil( (double) WalrusQuotaUtil.countTotalObjectSize() / FileUtils.ONE_GB ) )
      					) )      			
      			);
      } catch ( Exception ex ) {
        logger.error( ex, ex );
      }
    }
  }
}
