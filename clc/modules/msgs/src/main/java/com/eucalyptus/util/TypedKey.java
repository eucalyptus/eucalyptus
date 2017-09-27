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
package com.eucalyptus.util;

import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 *
 */
public final class TypedKey<T> {

  private final String description;
  private final Supplier<? extends T> initialValue;

  private TypedKey( final String description, final Supplier<? extends T> initialValue ) {
    this.description = description;
    this.initialValue = initialValue;
  }

  public static <T> TypedKey<T> create( String description ) {
    return new TypedKey<T>( description, Suppliers.ofInstance( null ) );
  }

  public static <T, V extends T> TypedKey<T> create( String description, V initialValue ) {
    return new TypedKey<T>( description, Suppliers.ofInstance( (T) initialValue ) );
  }

  public static <T, V extends T> TypedKey<T> create( String description, Supplier<V> initialValue ) {
    return new TypedKey<T>( description, initialValue );
  }

  public String getDescription( ) {
    return description;
  }

  public T initialValue( ) {
    return initialValue.get( );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "description", getDescription( ) )
        .toString( );
  }
}
