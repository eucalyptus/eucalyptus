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
package com.eucalyptus.auth.principal;

import java.util.Objects;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.principal.Principal.PrincipalType;
import com.eucalyptus.util.Parameters;

/**
 * Principal type and type specific name
 */
public final class TypedPrincipal {

  @Nonnull private final PrincipalType type;
  @Nonnull private final String name;

  private TypedPrincipal(
      @Nonnull final PrincipalType type,
      @Nonnull final String name
  ) {
    this.type = Parameters.checkParamNotNull( "type", type );
    this.name = Parameters.checkParamNotNullOrEmpty( "name", name );
  }

  public static TypedPrincipal of(
      @Nonnull final PrincipalType type,
      @Nonnull final String name
  ) {
    return new TypedPrincipal( type, name );
  }

  @Nonnull
  public PrincipalType getType( ) {
    return type;
  }

  @Nonnull
  public String getName( ) {
    return name;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final TypedPrincipal that = (TypedPrincipal) o;
    return type == that.type &&
        Objects.equals( name, that.name );
  }

  @Override
  public int hashCode() {
    return Objects.hash( type, name );
  }
}
