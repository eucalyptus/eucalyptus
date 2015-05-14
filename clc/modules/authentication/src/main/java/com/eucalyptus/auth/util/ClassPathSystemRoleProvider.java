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
package com.eucalyptus.auth.util;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.EuareRole;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * System role provider that loads policies from classpath resources.
 *
 * E.g. ProviderName ->
 *        provider-name-assume-role-policy.json
 *        provider-name-policy.json
 */
public abstract class ClassPathSystemRoleProvider implements SystemRoleProvider {

  private static Logger LOG = Logger.getLogger(ClassPathSystemRoleProvider.class);

  @Override
  public String getAssumeRolePolicy( ) {
    return loadPolicy( getResourceName( "AssumeRolePolicy" ) );
  }

  @Override
  public String getPolicy( ) {
    return loadPolicy( getResourceName( "Policy" ) );
  }

  private String getResourceName( String type ) {
    return CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_HYPHEN, getName( ) + type ) + ".json";
  }

  private String loadPolicy( final String resourceName ) {
    try {
      return Resources.toString( Resources.getResource( getClass( ), resourceName ), Charsets.UTF_8 );
    } catch ( final IOException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  @Override
  public String toString() {
    return String.format("For account: %s, class name: %s, path: %s", getAccountName(), getName(),
        getPath());
  }

  public void ensureAccountAndRoleExists() throws EucalyptusCloudException {
    Account systemAccount = null;
    EuareRole role = null;
    // Lookup account account. It could have been setup by the database bootstrapper. If not set it up here
    try {
      systemAccount = Accounts.lookupAccountByName(getAccountName());
    } catch (Exception e) {
      LOG.warn("Could not find account " + getAccountName() + ". Account may not exist, trying to create it");
      try {
        systemAccount = Accounts.addSystemAccountWithAdmin(getAccountName());
      } catch (Exception e1) {
        LOG.warn("Failed to create account " + getAccountName());
        throw new EucalyptusCloudException("Failed to create account " + getAccountName());
      }
    }

    // Lookup role of the account. Add the role if necessary.
    try {
      role = systemAccount.lookupRoleByName(getName());
    } catch (Exception e) {
      LOG.warn("Could not find " + getName() + " role for " + getAccountName()
          + " account. The role may not exist, trying to add role to the account");
      try {
        role = systemAccount.addRole(getName(), getPath(), getAssumeRolePolicy());
        role.addPolicy(getName(), getPolicy());
      } catch (Exception e1) {
        LOG.warn("Failed to create role " + getName());
        throw new EucalyptusCloudException("Failed to create role " + getName(), e1);
      }
    }
    // check that role has default policy
    boolean foundPolicy = false;
    try {
      List<Policy> policies = role.getPolicies();
      for (Policy policy : policies) {
        if ( getName().equals(policy.getName()) ){
          foundPolicy = true;
          break;
        }
      }
    } catch (Exception e) {
      throw new EucalyptusCloudException("Failed to list policies ", e);
    }

    if( !foundPolicy ){
      try {
        role.addPolicy(getName(), getPolicy());
      } catch (Exception e) {
        throw new EucalyptusCloudException("Failed add policy ", e);
      }
    }
  }
}
