/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
   * <p>The role or user based identifier used when authenticating. The
   * identity associated with the principals permissions.</p>
   *
   * <p>For a role this will be the roleid:sessionName, for a user the
   * userid</p>
   *
   * @return The identifier
   * @see com.eucalyptus.auth.Accounts#isRoleIdentifier
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
