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
package com.eucalyptus.auth.euare.persist.upgrade;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_2_0;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.upgrade.Upgrades;
import com.google.common.collect.ImmutableList;
import groovy.sql.Sql;

/**
 *
 */
@Upgrades.PreUpgrade( value = Euare.class, since = v4_2_0 )
public class AuthPreUpgrade420 implements Callable<Boolean> {

  private static final Logger logger = Logger.getLogger( AuthPreUpgrade420.class );

  private static final List<String> DROP_TABLES = ImmutableList.of(
      "auth_auth",
      "auth_auth_action_list",
      "auth_auth_resource_list",
      "auth_condition",
      "auth_condition_value_list",
      "auth_principal",
      "auth_principal_value_list",
      "auth_statement"
  );

  @Override
  public Boolean call() throws Exception {
    Sql sql = null;
    try {
      sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection("eucalyptus_auth");
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
