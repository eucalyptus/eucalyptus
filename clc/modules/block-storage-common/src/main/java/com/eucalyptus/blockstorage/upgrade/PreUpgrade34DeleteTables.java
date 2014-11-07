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
package com.eucalyptus.blockstorage.upgrade;

import static com.eucalyptus.upgrade.Upgrades.PreUpgrade;
import static com.eucalyptus.upgrade.Upgrades.Version.v4_1_0;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.upgrade.Upgrades;
import com.google.common.collect.ImmutableList;
import groovy.sql.Sql;

/**
 * Delete tables removed in 3.3.0.
 *
 * <p>Note that although the tables were removed in 3.3.0 the upgrade task was
 * added in 3.4.0, so must run for that version.</p>
 */
@PreUpgrade( value = Storage.class, since = v4_1_0 )  // originally v3_4_0
public class PreUpgrade34DeleteTables implements Callable<Boolean> {

  private static final Logger logger = Logger.getLogger( PreUpgrade34DeleteTables.class );

  private static final List<String> DROP_TABLES = ImmutableList.of(
      "aoemetainfo",
      "aoevolumeinfo",
      "storage_stats_info" );

  @Override
  public Boolean call() throws Exception {
    Sql sql = null;
    try {
      sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_storage" );
      for ( final String table : DROP_TABLES ) {
        sql.execute( String.format( "drop table if exists %s", table ) );
      }
      return true;
    } catch ( Exception ex ) {
      logger.error( ex, ex );
      return false;
    } finally {
      if ( sql != null ) {
        sql.close( );
      }
    }

  }
}
