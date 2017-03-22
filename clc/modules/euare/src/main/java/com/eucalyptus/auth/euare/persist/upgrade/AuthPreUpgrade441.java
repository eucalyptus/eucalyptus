/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.auth.euare.persist.upgrade;

import com.eucalyptus.component.id.Euare;
import com.eucalyptus.upgrade.Upgrades;
import groovy.sql.Sql;
import org.apache.log4j.Logger;

import java.util.concurrent.Callable;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_4_1;

/**
 *
 */
@Upgrades.PreUpgrade( value = Euare.class, since = v4_4_1 )
public class AuthPreUpgrade441 implements Callable<Boolean> {

  private static final Logger logger = Logger.getLogger( AuthPreUpgrade441.class );

  @Override
  public Boolean call() throws Exception {
    Sql sql = null;
    try {
      sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_auth" );
      // new policy will be uploaded at startup
      sql.execute("delete from auth_policy where auth_policy_name = 'InfrastructureAdministrator'");
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