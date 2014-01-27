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
