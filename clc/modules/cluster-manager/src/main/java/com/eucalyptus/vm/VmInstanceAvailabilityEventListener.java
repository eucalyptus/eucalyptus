/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.vm;

import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType.Core;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType.Disk;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType.Instance;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType.Memory;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.common.internal.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.common.internal.ResourceState;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 */
public class VmInstanceAvailabilityEventListener implements EventListener<ClockTick> {

  private static final Logger logger = Logger.getLogger( VmInstanceAvailabilityEventListener.class );

  private static final class AvailabilityAccumulator {
    private long total;
    private long available;
    private final Function<VmType,Integer> valueExtractor;
    private final List<ResourceAvailabilityEvent.Availability> availabilities = Lists.newArrayList();

    private AvailabilityAccumulator( final Function<VmType,Integer> valueExtractor ) {
      this.valueExtractor = valueExtractor;
    }

    private void rollUp( final Iterable<ResourceAvailabilityEvent.Tag> tags ) {
      availabilities.add( new ResourceAvailabilityEvent.Availability( total, available, tags ) );
      total = 0;
      available = 0;
    }
  }

  public static void register( ) {
    Listeners.register( ClockTick.class, new VmInstanceAvailabilityEventListener() );
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    if ( Bootstrap.isOperational() && Hosts.isCoordinator() ) {

      final List<ResourceAvailabilityEvent> resourceAvailabilityEvents = Lists.newArrayList();
      final Map<ResourceAvailabilityEvent.ResourceType,AvailabilityAccumulator> availabilities = Maps.newEnumMap( ResourceAvailabilityEvent.ResourceType.class );
      final Iterable<VmType> vmTypes = Lists.newArrayList( VmTypes.list());
      for ( final Cluster cluster : Clusters.list( ) ) {
        availabilities.put( Core, new AvailabilityAccumulator( VmType.SizeProperties.Cpu ) );
        availabilities.put( Disk, new AvailabilityAccumulator( VmType.SizeProperties.Disk ) );
        availabilities.put( Memory, new AvailabilityAccumulator( VmType.SizeProperties.Memory ) );

        for ( final VmType vmType : vmTypes ) {
          final ResourceState.VmTypeAvailability va = cluster.getNodeState().getAvailability( vmType );

          resourceAvailabilityEvents.add( new ResourceAvailabilityEvent( Instance, new ResourceAvailabilityEvent.Availability( va.getMax(), va.getAvailable(), Lists.<ResourceAvailabilityEvent.Tag>newArrayList(
              new ResourceAvailabilityEvent.Dimension( "availabilityZone", cluster.getPartition() ),
              new ResourceAvailabilityEvent.Dimension( "cluster", cluster.getName() ),
              new ResourceAvailabilityEvent.Type( "vm-type", vmType.getName() )
              ) ) ) );

          for ( final AvailabilityAccumulator availability : availabilities.values() ) {
            availability.total = Math.max( availability.total, va.getMax() * availability.valueExtractor.apply(vmType) );
            availability.available = Math.max( availability.available, va.getAvailable() * availability.valueExtractor.apply(vmType) );
          }
        }

        for ( final AvailabilityAccumulator availability : availabilities.values() ) {
          availability.rollUp(  Lists.<ResourceAvailabilityEvent.Tag>newArrayList(
              new ResourceAvailabilityEvent.Dimension( "availabilityZone", cluster.getPartition() ),
              new ResourceAvailabilityEvent.Dimension( "cluster", cluster.getName() )
          ) );
        }
      }

      for ( final Map.Entry<ResourceAvailabilityEvent.ResourceType,AvailabilityAccumulator> entry : availabilities.entrySet() )  {
        resourceAvailabilityEvents.add( new ResourceAvailabilityEvent( entry.getKey(), entry.getValue().availabilities ) );
      }

      for ( final ResourceAvailabilityEvent resourceAvailabilityEvent : resourceAvailabilityEvents  ) try {
        ListenerRegistry.getInstance().fireEvent( resourceAvailabilityEvent );
      } catch ( Exception ex ) {
        logger.error( ex, ex );
      }

    }
  }
}
