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

import static org.hamcrest.Matchers.notNullValue;
import java.util.Set;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.principal.Principal;
import com.eucalyptus.util.Parameters;
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
    Parameters.checkParam( "type", type, notNullValue( ) );
    Parameters.checkParam( "values", values, notNullValue( ) );
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
