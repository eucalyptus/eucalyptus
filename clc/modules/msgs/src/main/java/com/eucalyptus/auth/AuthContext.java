/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.TypedPrincipal;
import com.eucalyptus.auth.principal.User;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
  private final Set<TypedPrincipal> principals;
  private final Map<String,String> evaluatedKeys;
  private final Collection<PolicyVersion> policies;
  private Map<AuthEvaluationContextKey,AuthEvaluationContext> contexts = Maps.newHashMap();

  AuthContext(
      final User requestUser,
      final Set<TypedPrincipal> principals,
      final Iterable<PolicyVersion> policies,
      final Map<String, String> evaluatedKeys
  ) throws AuthException {
    this.userId = requestUser.getUserId( );
    this.systemAdmin = requestUser.isSystemAdmin( );
    this.systemUser = requestUser.isSystemUser( );
    this.accountAdmin = requestUser.isAccountAdmin( );
    this.accountNumber = requestUser.getAccountNumber( );
    this.user = requestUser;
    this.principals = ImmutableSet.copyOf( principals );
    this.evaluatedKeys = evaluatedKeys;
    this.policies = ImmutableList.copyOf( policies );
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

  public Iterable<PolicyVersion> getPolicies( ) {
    return policies;
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
  ) throws AuthException {
    final AuthEvaluationContextKey key = new AuthEvaluationContextKey( vendor, resource, action );
    AuthEvaluationContext context = contexts.get( key );
    if ( context == null ) {
      context = Permissions.createEvaluationContext( vendor, resource, action, user, policies, evaluatedKeys, principals );
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
