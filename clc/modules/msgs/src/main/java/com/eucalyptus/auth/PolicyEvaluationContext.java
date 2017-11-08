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
package com.eucalyptus.auth;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.util.TypedContext;
import com.eucalyptus.util.TypedKey;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 *
 */
public class PolicyEvaluationContext {

  private final TypedContext attributes;
  private static final PolicyEvaluationContext empty = new PolicyEvaluationContext( Collections.emptyMap( ) );
  private static final ThreadLocal<PolicyEvaluationContext> threadContext = new ThreadLocal<>( );

  private PolicyEvaluationContext( final Map<TypedKey<?>,Object> attrMap ) {
    attributes = TypedContext.newTypedContext( ImmutableMap.copyOf( attrMap ) );
  }

  public <T> T getAttribute( final TypedKey<T> key ) {
    return attributes.get( key );
  }

  public boolean hasAttribute( final TypedKey<?> key ) {
    return attributes.containsKey( key );
  }

  public <R> R doWithContext( final Callable<R> callable ) throws Exception {
    final PolicyEvaluationContext previous = threadContext.get( );
    threadContext.set( this );
    try {
      return callable.call( );
    } finally {
      threadContext.set( previous );
    }
  }

  @Nonnull
  public static PolicyEvaluationContext get( ) {
    return MoreObjects.firstNonNull( threadContext.get( ), empty );
  }

  public static Builder builder( ) {
    return new Builder( );
  }

  public static final class Builder {
    private final Map<TypedKey<?>,Object> attributes = Maps.newHashMap( );

    public <T> Builder attr(
                 final PolicyEvaluationWriteContextKey<T> key,
        @Nonnull final T value ) {
      //noinspection ConstantConditions
      if ( value == null ) throw new NullPointerException( );
      attributes.put( key.getKey( ), value );
      return this;
    }

    public <T> Builder attrIfNotNull(
                  final PolicyEvaluationWriteContextKey<T> key,
        @Nullable final T value ) {
      if ( value != null ) {
        attributes.put( key.getKey( ), value );
      }
      return this;
    }

    public <T extends CharSequence> Builder attrIfNotNullOrEmpty(
                  final PolicyEvaluationWriteContextKey<T> key,
        @Nullable final T value ) {
      if ( value != null && value.length( ) > 0 ) {
        attributes.put( key.getKey( ), value );
      }
      return this;
    }

    public PolicyEvaluationContext build( ) {
      return new PolicyEvaluationContext( attributes );
    }
  }
}
