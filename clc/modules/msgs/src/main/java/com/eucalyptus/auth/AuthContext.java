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
package com.eucalyptus.auth;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.google.common.collect.Maps;

/**
 * Context for authorization.
 */
public class AuthContext {
  private final String userId;
  private final String accountNumber;
  private final boolean accountAdmin;
  private final boolean systemAdmin;
  private final boolean systemUser;
  private final User user; // NOTE, do not invoke any methods on this without caching the result
  private final Map<String,String> evaluatedKeys;
  private Map<AuthEvaluationContextKey,AuthEvaluationContext> contexts = Maps.newHashMap();

  AuthContext( final User requestUser, final Map<String, String> evaluatedKeys ) throws AuthException {
    final Account account = requestUser.getAccount( );
    this.userId = requestUser.getUserId( );
    this.accountNumber = account.getAccountNumber( );
    this.systemAdmin = requestUser.isSystemAdmin( );
    this.systemUser = requestUser.isSystemUser( );
    this.accountAdmin = requestUser.isAccountAdmin( );
    this.user = requestUser;
    this.evaluatedKeys = evaluatedKeys;
  }

  public String getUserId() {
    return userId;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public boolean isAccountAdmin() {
    return accountAdmin;
  }

  public boolean isSystemAdmin() {
    return systemAdmin;
  }

  public boolean isSystemUser() {
    return systemUser;
  }

  /**
   * Create an evaluation context for this context.
   *
   * @param vendor The vendor.
   * @param resource The resource type (should not be null for authorizations)
   * @param action The action.
   * @return A matching evaluation context
   */
  public AuthEvaluationContext evaluationContext(
      @Nonnull final String vendor,
      @Nullable final String resource,
      @Nonnull  final String action
  ) {
    final AuthEvaluationContextKey key = new AuthEvaluationContextKey( vendor, resource, action );
    AuthEvaluationContext context = contexts.get( key );
    if ( context == null ) {
      context = Permissions.createEvaluationContext( vendor, resource, action, user, evaluatedKeys );
      contexts.put( key, context );
    }
    return context;
  }

  @SuppressWarnings( "RedundantIfStatement" )
  private static final class AuthEvaluationContextKey {
    @Nonnull  private final String vendor;
    @Nullable private final String resource;
    @Nonnull  private final String action;

    private AuthEvaluationContextKey(
        @Nonnull final String vendor,
        @Nullable final String resource,
        @Nonnull final String action
    ) {
      this.vendor = vendor;
      this.resource = resource;
      this.action = action;
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final AuthEvaluationContextKey that = (AuthEvaluationContextKey) o;

      if ( !action.equals( that.action ) ) return false;
      if ( resource != null ? !resource.equals( that.resource ) : that.resource != null ) return false;
      if ( !vendor.equals( that.vendor ) ) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = vendor.hashCode();
      result = 31 * result + ( resource != null ? resource.hashCode() : 0 );
      result = 31 * result + action.hashCode();
      return result;
    }
  }
}
