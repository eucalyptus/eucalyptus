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
package com.eucalyptus.reporting.upgrade;

import static com.eucalyptus.reporting.upgrade.UpgradeUtils.transactionalForEntity;
import static com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import static com.eucalyptus.upgrade.Upgrades.Version.v3_2_0;
import javax.persistence.EntityManager;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Reporting upgrade for 3.2, remove existing user and account data.
 */
@EntityUpgrade(
    value = Reporting.class,
    since = v3_2_0,
    entities = { ReportingUser.class, ReportingAccount.class } )
public class Upgrade32 implements Predicate<Class> {
  @Override
  public boolean apply( final Class entityClass ) {
    return transactionalForEntity( entityClass, new Function<EntityManager, Boolean>() {
      @Override
      public Boolean apply( final EntityManager entityManager ) {
        // purge old data from re-used tables
        entityManager.createQuery( "delete from " + entityClass.getName() ).executeUpdate();
        return true;
      }
    } );
  }
}
