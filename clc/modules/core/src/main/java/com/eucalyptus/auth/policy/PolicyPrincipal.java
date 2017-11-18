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

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.auth.policy.PolicyUtils.checkParam;
import java.util.Set;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.principal.Principal;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 *
 */
public class PolicyPrincipal implements Principal {

  private final Principal.PrincipalType type;

  private final Set<String> values;

  private final boolean notPrincipal;

  public PolicyPrincipal(
               final boolean notPrincipal,
      @Nonnull final PrincipalType type,
      @Nonnull final Set<String> values
  ) {
    checkParam( "type", type, notNullValue() );
    checkParam( "values", values, notNullValue() );
    this.type = type;
    this.values = ImmutableSet.copyOf( Iterables.transform( values, PolicyUtils.internString( ) ) );
    this.notPrincipal = notPrincipal;
  }

  @Override
  public PrincipalType getType() {
    return type;
  }

  @Override
  public Set<String> getValues() {
    return values;
  }

  @Override
  public boolean isNotPrincipal() {
    return notPrincipal;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    final PolicyPrincipal that = (PolicyPrincipal) o;

    if ( notPrincipal != that.notPrincipal ) return false;
    if ( type != that.type ) return false;
    if ( !values.equals( that.values ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + values.hashCode();
    result = 31 * result + ( notPrincipal ? 1 : 0 );
    return result;
  }
}
