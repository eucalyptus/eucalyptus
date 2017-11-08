/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
