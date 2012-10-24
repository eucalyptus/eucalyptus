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

import static com.eucalyptus.upgrade.Upgrades.PostUpgrade;
import static com.eucalyptus.upgrade.Upgrades.Version.v3_2_0;
import java.util.concurrent.Callable;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.reporting.ReportingDataVerifier;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.google.common.base.Function;

/**
 *  Reporting upgrade for 3.2, generate reporting events for existing items.
 */
@PostUpgrade( value = Reporting.class, since = v3_2_0 )
public class PostUpgrade32 implements Callable<Boolean> {

  private static final Logger logger = Logger.getLogger( Upgrade32.class );

  @Override
  public Boolean call() throws Exception {
    return UpgradeUtils.transactionalForEntity(
        ReportingAccount.class,
        PostUpgrade32Work.INSTANCE
    );
  }

  private enum PostUpgrade32Work implements Function<EntityManager,Boolean> {
    INSTANCE;

    @Override
    public Boolean apply( final EntityManager entityManager ) {
      // create events for existing entities
      final String desc = ReportingDataVerifier.addMissingReportingEvents();
      logger.info( "Reporting event status / changes:\n " + desc );
      return true;
    }
  }
}
