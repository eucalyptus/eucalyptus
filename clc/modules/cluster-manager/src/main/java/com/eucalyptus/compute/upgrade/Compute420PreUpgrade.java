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
package com.eucalyptus.compute.upgrade;

import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.upgrade.Upgrades;
import groovy.sql.Sql;

/**
 *
 */
@Upgrades.PreUpgrade( value = Compute.class, since = Upgrades.Version.v4_2_0 )
public class Compute420PreUpgrade implements Callable<Boolean> {
  private static final Logger logger = Logger.getLogger( Compute420PreUpgrade.class );

  @Override
  public Boolean call( ) throws Exception {
    Sql sql = null;
    try {
      sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );
      sql.execute( "drop table if exists cloud_address_configuration" );
      return true;
    } catch ( Exception ex ) {
      logger.error( "Error updating cloud schema", ex );
      return false;
    } finally {
      if ( sql != null ) {
        sql.close( );
      }
    }
  }
}