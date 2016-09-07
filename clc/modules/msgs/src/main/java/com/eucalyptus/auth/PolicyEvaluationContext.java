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
