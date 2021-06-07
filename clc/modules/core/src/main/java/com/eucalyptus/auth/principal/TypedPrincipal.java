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
