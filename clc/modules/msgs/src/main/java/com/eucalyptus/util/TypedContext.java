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
package com.eucalyptus.util;

import java.util.Map;
import com.google.common.collect.Maps;

/**
 *
 */
public class TypedContext {
  private final Map<TypedKey<?>, Object> delegate;

  private TypedContext( final Map<TypedKey<?>, Object> wrapped ) {
    this.delegate = wrapped;
  }

  public static TypedContext newTypedContext( ) {
    return newTypedContext( Maps.newHashMap( ) );
  }

  public static TypedContext newTypedContext( final Map<TypedKey<?>, Object> wrapped ) {
    return new TypedContext( wrapped );
  }

  @SuppressWarnings( "unchecked" )
  public <T> T get( TypedKey<T> key ) {
    T value = (T) delegate.get( key );
    if ( value == null && ( value = key.initialValue( ) ) != null ) {
      delegate.put( key, value );
    }

    return value;
  }

  @SuppressWarnings( "unchecked" )
  public <T> T put( TypedKey<T> key, T value ) {
    return (T) delegate.put( key, value );
  }

  @SuppressWarnings( "unchecked" )
  public <T> T remove( TypedKey<T> key ) {
    return (T) delegate.remove( key );
  }

  public <T> boolean containsKey( TypedKey<T> key ) {
    return delegate.containsKey( key );
  }

  public String toString( ) {
    return "TypedContext:" + String.valueOf( delegate );
  }
}
