/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
