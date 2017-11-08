/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.ResourceState;
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
      for ( final Cluster cluster : Clusters.getInstance().listValues() ) {
        availabilities.put( Core, new AvailabilityAccumulator( VmType.SizeProperties.Cpu ) );
        availabilities.put( Disk, new AvailabilityAccumulator( VmType.SizeProperties.Disk ) );
        availabilities.put( Memory, new AvailabilityAccumulator( VmType.SizeProperties.Memory ) );

        for ( final VmType vmType : vmTypes ) {
          final ResourceState.VmTypeAvailability va = cluster.getNodeState().getAvailability( vmType.getName() );

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
