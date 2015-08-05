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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.blockstorage;

import java.util.EnumSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesType;
import com.eucalyptus.blockstorage.msgs.StorageVolume;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.internal.blockstorage.State;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.VolumeEvent;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
*
*/
public class VolumeUpdateEventListener implements EventListener<ClockTick>, Callable<Boolean> {
  private static final Logger LOG                 = Logger.getLogger( VolumeUpdateEventListener.class );
  private static final long VOLUME_STATE_TIMEOUT  = 2 * 60 * 60 * 1000L;
  private static final long VOLUME_DELETE_TIMEOUT = 30 * 60 * 1000L;
  private static final AtomicBoolean ready = new AtomicBoolean( true );

  public static void register( ) {
    Listeners.register( ClockTick.class, new VolumeUpdateEventListener() );
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    if ( Topology.isEnabledLocally( Eucalyptus.class ) && ready.compareAndSet( true, false ) ) {
      try {
        Threads.enqueue( Eucalyptus.class, Volumes.class, this );
      } catch ( final Exception ex ) {
        ready.set( true );
      }
    }
  }

  @Override
  public Boolean call( ) throws Exception {
    try {
      VolumeUpdateEventListener.update();
    } finally {
      ready.set( true );
    }
    return true;
  }

  static void update( ) {
    final Multimap<String, String> partitionVolumeMap = HashMultimap.create();
    final EntityTransaction db = Entities.get( Volume.class );
    try {
      for ( final Volume v : Entities.query( Volume.named( null, null ) ) ) {
        partitionVolumeMap.put( v.getPartition( ), v.getDisplayName( ) );
      }
      db.rollback( );
    } catch ( final Exception ex ) {
      Logs.extreme().error( ex, ex );
      db.rollback( );
    }
    Logs.extreme( ).debug( "Volume state update: " + Joiner.on( "\n" ).join( partitionVolumeMap.entries( ) ) );
    for ( final String partition : partitionVolumeMap.keySet( ) ) {
    try {
        final Map<String, StorageVolume> idStorageVolumeMap = updateVolumesInPartition( partition );//TODO:GRZE: restoring volume state
        for ( final String v : partitionVolumeMap.get( partition ) ) {
          try {
            final StorageVolume storageVolume = idStorageVolumeMap.get( v );
            volumeStateUpdate( v, storageVolume );
          } catch ( final Exception ex ) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
          }
        }
      } catch ( final Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
    }
  }

  static void volumeStateUpdate( final String volumeId, final StorageVolume storageVolume ) {
    final Function<String, Volume> updateVolume = new Function<String, Volume>( ) {
      @Override
      public Volume apply( final String input ) {
        final StringBuilder buf = new StringBuilder( );
        try {
          final Volume v = Entities.uniqueResult( Volume.named( null, input ) );
          VmVolumeAttachment vmAttachedVol = null;
          boolean maybeBusy = false;
          String vmId = null;
          try {
            vmAttachedVol = VmInstances.lookupVolumeAttachment( v.getDisplayName( ) );
            maybeBusy = true;
            vmId = vmAttachedVol.getVmInstance( ).getInstanceId( );
          } catch ( final NoSuchElementException ex ) {
          }

        State initialState = v.getState( );
        if ( !State.ANNIHILATING.equals( initialState ) && !State.ANNIHILATED.equals( initialState ) && maybeBusy ) {
          initialState = State.BUSY;
        }
        buf.append( "VolumeStateUpdate: Current Volume Info: [" )
           .append( "Partition: ").append(v.getPartition( ) ).append( " " )
           .append( "Name: ").append(v.getDisplayName( ) ).append( " " )
           .append( "CurrentState: ").append(v.getState( ) ).append( " " )
           .append( "Created: ").append(v.getCreationTimestamp( ) ).append(" ]");
        if ( vmAttachedVol != null ) {
          buf.append( " Attachment: [ " )
             .append( "InstanceId: ").append( vmId ).append( " " )
             .append( "AttachmentState: ").append(vmAttachedVol.getAttachmentState( ) ).append(" ]");
        }

        String status = null;
        Integer size = 0;
        String actualDeviceName = "unknown";
        State volumeState = initialState;
        if ( storageVolume != null ) {
          status = storageVolume.getStatus( );
          size = Integer.parseInt( storageVolume.getSize( ) );
          actualDeviceName = storageVolume.getActualDeviceName( );
          //NOTE: removed this conditional check for initial state and actual device name. An empty actualDeviceName or 'invalid'
          //is legitimate if the volume is not exported/attached. Only on attachment request will device name be populated
          //if ( State.EXTANT.equals( initialState )
          //       && ( ( actualDeviceName == null ) || "invalid".equals( actualDeviceName ) || "unknown".equals( actualDeviceName ) ) ) {
            //
            //volumeState = State.GENERATING;
          //} else if ( State.ANNIHILATING.equals( initialState ) && State.ANNIHILATED.equals( Volumes.transformStorageState( v.getState( ), status ) ) ) {
          if ( State.ANNIHILATING.equals( initialState ) && State.ANNIHILATED.equals( Volumes.transformStorageState( v.getState( ), status ) ) ) {
            volumeState = State.ANNIHILATED;
          } else {
            volumeState = Volumes.transformStorageState( v.getState( ), status );
          }
          buf.append( " Incoming state update: [" )
               .append("State: ").append( storageVolume.getStatus( ) ).append( "=>" ).append( volumeState ).append( " " )
               .append("Size: ").append( storageVolume.getSize( ) ).append( "GB " )
               .append("SourceSnapshotId: ").append( storageVolume.getSnapshotId( ) ).append( " " )
               .append("CreationTime: ").append( storageVolume.getCreateTime( ) ).append( " " )
               .append("DeviceName: ").append( storageVolume.getActualDeviceName( ) ).append(" ] ");
        } else if ( State.ANNIHILATING.equals( v.getState( ) ) ) {
          volumeState = State.ANNIHILATED;
        } else if ( State.GENERATING.equals( v.getState( ) ) && v.lastUpdateMillis( ) > VOLUME_STATE_TIMEOUT ) {
          volumeState = State.FAIL;
        } else if ( State.EXTANT.equals( v.getState( ) ) && v.lastUpdateMillis( ) > VOLUME_STATE_TIMEOUT ) {
            //volume is available but the SC does not know about it.
            //This is based on a guarantee that the SC will never send partial information
            //If the SC subsequently reports it as available, it will be recovered
            volumeState = State.ERROR;
        }
        v.setState( volumeState );
        try {
          if ( v.getSize( ) <= 0 ) {
            v.setSize( size );
            if ( EnumSet.of( State.GENERATING, State.EXTANT, State.BUSY ).contains( volumeState ) ) {
              Volumes.fireUsageEvent( v, VolumeEvent.forVolumeCreate() );
            }
          }
        } catch ( final Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
        }
        //TODO:GRZE: expire deleted/failed volumes in the future.
        //            if ( State.ANNIHILATED.equals( v.getState( ) ) && State.ANNIHILATED.equals( v.getState( ) ) && v.lastUpdateMillis( ) > VOLUME_DELETE_TIMEOUT ) {
        //              Entities.delete( v );
        //            }
        buf.append( " Resulting new-state: [" ).append( v.getState( ) ).append("]");
        LOG.debug( buf.toString( ) );
        return v;
      } catch ( final TransactionException ex ) {
        LOG.error( buf.toString( ) + " failed because of " + ex.getMessage( ) );
        Logs.extreme( ).error( buf.toString( ) + " failed because of " + ex.getMessage( ), ex );
        throw Exceptions.toUndeclared( ex );
      } catch ( final NoSuchElementException ex ) {
        LOG.error( buf.toString( ) + " failed because of " + ex.getMessage( ) );
        Logs.extreme( ).error( buf.toString( ) + " failed because of " + ex.getMessage( ), ex );
        throw ex;
      }
    }
    };
    Entities.asTransaction( Volume.class, updateVolume ).apply( volumeId );
  }

  static Map<String, StorageVolume> updateVolumesInPartition( final String partition ) {
    final Map<String, StorageVolume> idStorageVolumeMap = Maps.newHashMap();
    final ServiceConfiguration scConfig = Topology.lookup( Storage.class, Partitions.lookupByName( partition ) );
    try {
      final DescribeStorageVolumesResponseType volState = AsyncRequests.sendSync( scConfig, new DescribeStorageVolumesType() );
      for ( final StorageVolume vol : volState.getVolumeSet( ) ) {
        LOG.trace( "Volume states: " + vol.getVolumeId( ) + " " + vol.getStatus( ) + " " + vol.getActualDeviceName( ) );
        idStorageVolumeMap.put( vol.getVolumeId( ), vol );
      }
    } catch ( final Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
    return idStorageVolumeMap;
  }

}
