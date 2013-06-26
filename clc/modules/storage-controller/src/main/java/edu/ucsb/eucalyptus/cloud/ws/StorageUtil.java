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

import java.util.EnumSet;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.blockstorage.Snapshot;
import com.eucalyptus.blockstorage.State;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.UserMetadata;
import com.google.common.base.Objects;

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
}
