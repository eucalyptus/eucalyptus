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
package com.eucalyptus.blockstorage.upgrade;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_1_0;
import groovy.sql.Sql;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.upgrade.Upgrades.PreUpgrade;
import com.google.common.collect.ImmutableList;

/**
 * Delete tables removed in 3.3.0.
 *
 * <p>
 * Note that although the tables were removed in 3.3.0 the upgrade task was added in 3.4.0, so must run for that version.
 * </p>
 */
@PreUpgrade(value = Storage.class, since = v4_1_0)
// originally v3_4_0
public class PreUpgrade34DeleteTables implements Callable<Boolean> {

  private static final Logger logger = Logger.getLogger(PreUpgrade34DeleteTables.class);

  private static final List<String> DROP_TABLES = ImmutableList.of("aoemetainfo", "aoevolumeinfo", "storage_stats_info");

  @Override
  public Boolean call() throws Exception {
    Sql sql = null;
    try {
      sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection("eucalyptus_storage");
      for (final String table : DROP_TABLES) {
        sql.execute(String.format("drop table if exists %s", table));
      }
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
