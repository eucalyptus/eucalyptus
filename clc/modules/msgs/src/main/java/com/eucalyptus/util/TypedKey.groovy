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
package com.eucalyptus.util

import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
 *
 */
@CompileStatic
@ToString( includes='description' )
final class TypedKey<T> {
  final String description;
  private final Supplier<? extends T> initialValue;

  private TypedKey( final String description,
                    final Supplier<? extends T> initialValue ) {
    this.description = description
    this.initialValue = initialValue
  }

  static <T> TypedKey<T> create( String description ) {
    new TypedKey<T>( description, Suppliers.ofInstance( null ) )
  }

  static <T, V extends T> TypedKey<T> create( String description, V initialValue ) {
    new TypedKey<T>( description, Suppliers.ofInstance( (T)initialValue ) )
  }

  static <T, V extends T> TypedKey<T> create( String description, Supplier<V> initialValue ) {
    new TypedKey<T>( description, initialValue )
  }

  T initialValue( ) {
    initialValue.get( )
  }
}
