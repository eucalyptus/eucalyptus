/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.compute.upgrade;

import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.upgrade.Upgrades.PreUpgrade;
import groovy.sql.Sql;

/**
 *
 */
@PreUpgrade( value = Compute.class, since = Upgrades.Version.v4_4_0 )
public class Compute440PreUpgrade implements Callable<Boolean> {
  private static final Logger logger = Logger.getLogger( Compute440PreUpgrade.class );

  @Override
  public Boolean call( ) throws Exception {
    Sql sql = null;
    try {
      sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );
      sql.execute( "alter table metadata_instances drop column if exists metadata_vm_network_index" );
      sql.execute( "drop table if exists metadata_network_indices" );
      sql.execute( "drop table if exists metadata_extant_network" );
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