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

import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.TypedPrincipal;
import com.eucalyptus.auth.principal.User;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * Context for a policy evaluation.
 *
 * <p>The context can cache information between evaluations. A new context
 * should be created for each evaluation if caching is not desired.</p>
 */
@SuppressWarnings( { "StaticPseudoFunctionalStyleMethod", "Guava" } )
public interface AuthEvaluationContext {
  String getResourceType();
  String getAction();
  User getRequestUser();
  Map<String,String> getEvaluatedKeys();
  @Nullable
  Set<TypedPrincipal> getPrincipals( );
  @Nullable
  default Set<TypedPrincipal> getPrincipals( final Predicate<TypedPrincipal> filter ) {
    final Set<TypedPrincipal> principals = getPrincipals( );
    return principals == null ?
        null :
        ImmutableSet.copyOf( Iterables.filter( principals, filter ) );
  }
  String describe( String resourceAccountNumber, String resourceName );
  String describe( String resourceName, Long quantity );
}
