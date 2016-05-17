/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.objectstorage.upgrade;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_3_0;
import groovy.sql.Sql;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.upgrade.Upgrades.PreUpgrade;
import com.google.common.collect.ImmutableList;

/**
 * EUCA-11440: OSG schema uses duplicate index names
 *
 * <p>
 * BucketTags.java and LifecycleRule.java both created the same index name "idx_bucket_uuid"
 * for their tables. Index names must be unique so one of these indexes would fail to create.
 * 
 * In 4.3.0 we created a unique index name for each table, so we need to drop the old
 * index we no longer use.
 * </p>
 */
@PreUpgrade(value = ObjectStorage.class, since = v4_3_0)
public class DropConflictingBucketIndex implements Callable<Boolean> {

  private static final Logger logger = Logger.getLogger(DropConflictingBucketIndex.class);

  private static final String INDEX_TO_DROP = "idx_bucket_uuid";

  @Override
  public Boolean call() throws Exception {
    Sql sql = null;
    try {
      sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection("eucalyptus_osg");
      sql.execute(String.format("drop index if exists %s", INDEX_TO_DROP));
      return true;
    } catch (Exception ex) {
      logger.error(ex, ex);
      return false;
    } finally {
      if (sql != null) {
        sql.close();
      }
    }

  }
}
