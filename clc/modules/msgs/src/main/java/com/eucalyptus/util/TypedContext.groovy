/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.util

import com.google.common.collect.Maps
import groovy.transform.CompileStatic

/**
 *
 */
@CompileStatic
class TypedContext {
  private final Map<TypedKey<?>,Object> delegate;

  private TypedContext( final Map<TypedKey<?>,Object> wrapped ) {
    this.delegate = wrapped
  }

  static TypedContext newTypedContext( ) {
    newTypedContext( Maps.newHashMap( ) )
  }

  static TypedContext newTypedContext( final Map<TypedKey<?>,Object> wrapped ) {
    new TypedContext( wrapped )
  }

  def <T> T get( TypedKey<T> key ) {
    T value = (T) delegate.get( key )
    if ( value == null && ( value = key.initialValue( ) ) != null ) {
      delegate.put( key, value )
    }
    value
  }

  def <T> T put( TypedKey<T> key, T value ) {
    (T) delegate.put( key, value )
  }

  def <T> T remove( TypedKey<T> key ) {
    (T) delegate.remove( key )
  }

  def <T> boolean containsKey( TypedKey<T> key ) {
    delegate.containsKey( key )
  }

  String toString( ) {
    "TypedContext:${String.valueOf( delegate )}"
  }
}
