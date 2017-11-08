/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.euare;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.UserPrincipal;

/**
 *
 */
public class DelegatingUserPrincipal implements UserPrincipal {

  private final UserPrincipal delegate;

  public DelegatingUserPrincipal( final UserPrincipal delegate ) {
    this.delegate = delegate;
  }

  @Nonnull
  @Override
  public String getName() {
    return delegate.getName( );
  }

  @Nonnull
  @Override
  public String getPath() {
    return delegate.getPath( );
  }

  @Nonnull
  @Override
  public String getUserId() {
    return delegate.getUserId( );
  }

  @Override
  @Nonnull
  public String getAuthenticatedId() {
    return delegate.getAuthenticatedId( );
  }

  @Override
  @Nonnull
  public String getAccountAlias() {
    return delegate.getAccountAlias( );
  }

  @Override
  @Nonnull
  public String getAccountNumber() {
    return delegate.getAccountNumber( );
  }

  @Override
  @Nonnull
  public String getCanonicalId() {
    return delegate.getCanonicalId( );
  }

  @Override
  public boolean isEnabled() {
    return delegate.isEnabled( );
  }

  @Override
  public boolean isAccountAdmin() {
    return delegate.isAccountAdmin( );
  }

  @Override
  public boolean isSystemAdmin() {
    return delegate.isSystemAdmin( );
  }

  @Override
  public boolean isSystemUser() {
    return delegate.isSystemUser( );
  }

  @Override
  @Nullable
  public String getToken() {
    return delegate.getToken( );
  }

  @Override
  @Nullable
  public String getPassword() {
    return delegate.getPassword( );
  }

  @Override
  @Nullable
  public Long getPasswordExpires() {
    return delegate.getPasswordExpires( );
  }

  @Override
  @Nonnull
  public List<AccessKey> getKeys() {
    return delegate.getKeys( );
  }

  @Override
  @Nonnull
  public List<Certificate> getCertificates() {
    return delegate.getCertificates( );
  }

  @Override
  @Nonnull
  public List<PolicyVersion> getPrincipalPolicies() {
    return delegate.getPrincipalPolicies( );
  }

  @Override
  @Nullable
  public String getPTag() {
    return delegate.getPTag( );
  }
}
