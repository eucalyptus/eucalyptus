/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.principal;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 */
public interface UserPrincipal extends User {

  /**
   * The username.
   */
  @Nonnull
  @Override
  String getName( );

  /**
   * The users path.
   */
  @Nonnull
  @Override
  String getPath( );

  @Nonnull
  @Override
  String getUserId( );

  /**
   * The authenticated IAM identifier.
   *
   * <p>The role or user identifier used when authenticating. The identity
   * associated with the principals permissions.</p>
   *
   * @return The identifier
   */
  @Nonnull
  String getAuthenticatedId( );

  /**
   * The alias for the account, or the account number if an alias is not set.
   *
   * @return The alias
   */
  @Nonnull
  String getAccountAlias( );

  @Nonnull
  String getAccountNumber( );

  /**
   * The accounts canonical identifier.
   */
  @Nonnull
  String getCanonicalId( );

  /**
   * User enabled flag.
   *
   * <p>Disabled users are not authorized to perform any action. If policy evaluation
   * is not being performed then this flag must be explicitly checked.</p>
   */
  @Override
  boolean isEnabled( );

  @Override
  boolean isAccountAdmin( );

  @Override
  boolean isSystemAdmin( );

  @Override
  boolean isSystemUser( );

  @Nullable
  String getToken( );

  /**
   * Get the users crypt password (if any)
   */
  @Nullable
  String getPassword( );

  /**
   * Get the expiration timestamp for the password (if any)
   */
  @Nullable
  Long getPasswordExpires( );

  @Nonnull
  List<AccessKey> getKeys( );

  @Nonnull
  List<Certificate> getCertificates( );

  @Nonnull
  List<PolicyVersion> getPrincipalPolicies( );

  @Nullable
  String getPTag( );
}
