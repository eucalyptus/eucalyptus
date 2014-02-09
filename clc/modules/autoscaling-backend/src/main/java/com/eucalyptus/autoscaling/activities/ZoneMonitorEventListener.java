/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.autoscaling.activities;

import org.apache.log4j.Logger;
import com.eucalyptus.autoscaling.common.AutoScalingBackend;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;

/**
 *
 */
public class ZoneMonitorEventListener implements EventListener<ClockTick> {
  private static Logger logger = Logger.getLogger( ZoneMonitorEventListener.class );

  public static void register( ) {
    Listeners.register( ClockTick.class, new ZoneMonitorEventListener() );
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    if ( Topology.isEnabledLocally( AutoScalingBackend.class ) && Bootstrap.isOperational() ) {
      try {
        ZoneMonitor.checkZones();
      } catch ( Exception ex ) {
        logger.error( ex, ex );
      }
    }
  }
}
