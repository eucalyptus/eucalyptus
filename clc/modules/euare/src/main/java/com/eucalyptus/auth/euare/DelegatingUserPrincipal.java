/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
  private static final long serialVersionUID = 1L;

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
