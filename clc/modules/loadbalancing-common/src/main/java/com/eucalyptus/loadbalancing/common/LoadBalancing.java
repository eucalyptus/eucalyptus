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
package com.eucalyptus.loadbalancing.common;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.AwsServiceName;
import com.eucalyptus.component.annotation.PublicComponentAccounts;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.component.annotation.PublicService;

/**
 *
 */
@PublicService
@AwsServiceName( "elasticloadbalancing" )
@PolicyVendor( "elasticloadbalancing" )
@Partition( value = LoadBalancing.class, manyToOne = true )
@Description( "ELB API service" )
@PublicComponentAccounts(AccountIdentifiers.ELB_SYSTEM_ACCOUNT)
public class LoadBalancing extends ComponentId {
  private static final long serialVersionUID = 1L;

  @Override
  public Optional<? extends Iterable<UserPrincipal>> getPublicComponentAccounts() {
    try {
      return Optional.of(Lists.newArrayList(
          Accounts.lookupSystemAccountByAlias(AccountIdentifiers.ELB_SYSTEM_ACCOUNT)));
    } catch(Exception e) {
      return Optional.absent();
    }
  }
}
