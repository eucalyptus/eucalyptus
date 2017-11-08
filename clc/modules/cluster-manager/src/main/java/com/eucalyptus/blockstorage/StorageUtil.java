/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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

import java.util.EnumSet;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.compute.common.internal.blockstorage.Snapshot;
import com.eucalyptus.compute.common.internal.blockstorage.State;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 *
 */
public class StorageUtil {
  private static Logger LOG = Logger.getLogger( StorageUtil.class );

  /**
   * Get the total size used for block storage.
   *
   * @param partition Optional partition qualifier
   * @return The total size used for block storage (optionally by partition)
   */
  public static long getBlockStorageTotalSize( final String partition ) {
    return
        getBlockStorageTotalVolumeSize( partition ) +
        getBlockStorageTotalSnapshotSize( partition );
  }

  /**
   * Get the total size used for volumes.
   *
   * @param partition Optional partition qualifier
   * @return The total size used for volumes (optionally by partition)
   */
  public static long getBlockStorageTotalVolumeSize( final String partition ) {
    return getBlockStorageTotalSize( partition, "partition", "size", Volume.class );
  }

  /**
   * Get the total size used for snapshots.
   *
   * @param partition Optional partition qualifier
   * @return The total size used for snapshots (optionally by partition)
   */
  public static long getBlockStorageTotalSnapshotSize( final String partition ) {
    return getBlockStorageTotalSize( partition, "volumePartition", "volumeSize", Snapshot.class );
  }

  private static long getBlockStorageTotalSize( final String partition,
                                                final String partitionProperty,
                                                final String sizeProperty,
                                                final Class<? extends UserMetadata<State>> sizedType ) {
    long size = -1;
    final EntityTransaction db = Entities.get(sizedType);
    try {
      size = Objects.firstNonNull((Number) Entities.createCriteria(sizedType)
          .add(Restrictions.in("state", EnumSet.of(State.EXTANT, State.BUSY)))
          .add(partition == null ?
              Restrictions.isNotNull(partitionProperty) : // Get size for all partitions.
              Restrictions.eq(partitionProperty, partition))
          .setProjection(Projections.sum(sizeProperty))
          .setReadOnly(true)
          .uniqueResult(), 0).longValue();
      db.commit();
    } catch ( Exception e ) {
      LOG.error(e);
      db.rollback();
    }
    return size;
  }

  static Optional<State> mapState( final String state ) {
    Optional<State> mappedState = Optional.absent( );
    if ( StorageProperties.Status.creating.toString( ).equals( state ) ) {
      mappedState = Optional.of( State.GENERATING );
    } else if ( StorageProperties.Status.pending.toString( ).equals( state ) ) {
      mappedState = Optional.of( State.GENERATING );
    } else if ( StorageProperties.Status.available.toString( ).equals( state ) ) {
      mappedState = Optional.of( State.EXTANT );
    } else if ( StorageProperties.Status.failed.toString( ).equals( state ) ) {
      mappedState = Optional.of( State.FAIL );
    }
    return mappedState;
  }

  static void setMappedState( final Snapshot snapshot, final String state ) {
    final Optional<State> mapped = mapState( state );
    if ( mapped.isPresent( ) ) {
      snapshot.setState( mapped.get( ) );
    }
  }
}
