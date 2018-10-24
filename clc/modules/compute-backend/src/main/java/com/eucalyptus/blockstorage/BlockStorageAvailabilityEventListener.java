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
package com.eucalyptus.blockstorage;

import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Availability;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType.StorageEBS;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Tag;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent;
import com.eucalyptus.util.HasFullName;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Event listener that fires resource availability events for block storage.
 */
public class BlockStorageAvailabilityEventListener implements EventListener<ClockTick> {
  private static Logger logger = Logger.getLogger( BlockStorageAvailabilityEventListener.class );

  public static void register( ) {
    Listeners.register( ClockTick.class, new BlockStorageAvailabilityEventListener() );
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    if ( BootstrapArgs.isCloudController( ) && Bootstrap.isOperational() ) {
      final List<Availability> resourceAvailability = Lists.newArrayList();
      final Set<String> partitions =
          Sets.newHashSet( Clusters.stream( ).map( HasFullName.GET_PARTITION ) );
      for ( final String partition : partitions ) {
        long total = 0;

        try {
          total = Transactions.find( new StorageInfo( partition ) ).getMaxTotalVolumeSizeInGb();
        } catch ( TransactionException e ) {
          logger.debug( "Error finding capacity for " + partition, e );
        } catch ( NoSuchElementException e ) {
          continue;
        }

        resourceAvailability.add( new Availability( total, Math.max( total - StorageUtil.getBlockStorageTotalSize(partition), 0), Lists.<Tag>newArrayList(
            new ResourceAvailabilityEvent.Dimension( "AvailabilityZone", partition )
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
