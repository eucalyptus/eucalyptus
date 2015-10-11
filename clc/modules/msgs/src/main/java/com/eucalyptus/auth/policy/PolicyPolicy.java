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
package com.eucalyptus.auth.policy;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.Authorization;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 *
 */
public class PolicyPolicy {

  private final String policyVersion;

  private final List<Authorization> authorizations;

  public PolicyPolicy( @Nullable final String policyVersion,
                       @Nonnull  final List<PolicyAuthorization> authorizations ) {
    this.policyVersion = PolicyUtils.intern( policyVersion );
    this.authorizations = ImmutableList.copyOf( Iterables.transform( authorizations, PolicyUtils.internAuthorization( ) ) );
  }

  public String getPolicyVersion() {
    return policyVersion;
  }

  public List<Authorization> getAuthorizations( ) {
    return authorizations;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    final PolicyPolicy that = (PolicyPolicy) o;

    if ( !authorizations.equals( that.authorizations ) ) return false;
    if ( policyVersion != null ? !policyVersion.equals( that.policyVersion ) : that.policyVersion != null )
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = policyVersion != null ? policyVersion.hashCode() : 0;
    result = 31 * result + authorizations.hashCode();
    return result;
  }
}
