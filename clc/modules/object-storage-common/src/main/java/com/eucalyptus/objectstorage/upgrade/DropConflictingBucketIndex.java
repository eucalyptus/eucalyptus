/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
