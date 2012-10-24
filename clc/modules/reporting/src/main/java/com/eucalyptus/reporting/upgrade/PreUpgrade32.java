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

import static com.eucalyptus.upgrade.Upgrades.PreUpgrade;
import static com.eucalyptus.upgrade.Upgrades.Version.v3_2_0;
import java.util.List;
import java.util.concurrent.Callable;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.google.common.collect.ImmutableList;

/**
 * Reporting upgrade for 3.2, delete obsolete tables.
 */
@PreUpgrade( value = Reporting.class, since = v3_2_0 )
public class PreUpgrade32 implements Callable<Boolean> {

  private static final List<String> DROP_TABLES = ImmutableList.of(
      "storage_usage_snapshot",
      "instance_usage_snapshot",
      "s3_usage_snapshot",
      "reporting_instance" );

  @Override
  public Boolean call() throws Exception {
    return UpgradeUtils.transactionalForEntity(
        ReportingAccount.class,
        UpgradeUtils.dropTables( DROP_TABLES )
    );
  }
}
