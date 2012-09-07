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
package edu.ucsb.eucalyptus.cloud.ws;

import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Availability;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType.StorageEBS;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Tag;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.entities.StorageInfo;

/**
 * Event listener that fires resource availability events for block storage.
 */
public class BlockStorageAvailabilityEventListener implements EventListener<ClockTick> {
  private static Logger logger = Logger.getLogger( WalrusAvailabilityEventListener.class );

  public static void register( ) {
    Listeners.register( ClockTick.class, new BlockStorageAvailabilityEventListener() );
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    if ( Bootstrap.isFinished() && Hosts.isCoordinator() ) {
      final List<Availability> resourceAvailability = Lists.newArrayList();
      for ( final Cluster cluster : Clusters.getInstance().listValues() ) {
        long total = 0;

        try {
          total = Transactions.find( new StorageInfo( cluster.getPartition() ) ).getMaxTotalVolumeSizeInGb();
        } catch ( TransactionException e ) {
          logger.debug( "Error finding capacity for " + cluster.getPartition(), e );
        }

        resourceAvailability.add( new Availability( total, Math.max( total - StorageUtil.getBlockStorageTotalSize(cluster.getPartition()), 0), Lists.<Tag>newArrayList(
            new ResourceAvailabilityEvent.Dimension( "availabilityZone", cluster.getPartition() ),
            new ResourceAvailabilityEvent.Dimension( "cluster", cluster.getName() )
        ) ) );
      }

      try {
        ListenerRegistry.getInstance().fireEvent(
            new ResourceAvailabilityEvent( StorageEBS, resourceAvailability )
        );
      } catch ( Exception ex ) {
        logger.error( ex, ex );
      }
    }
  }
}
