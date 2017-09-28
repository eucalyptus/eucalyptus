/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.auth.euare.identity.region;

import java.util.function.Supplier;


/**
 *
 */
class NamedProperty<T> {

  private final String method;
  private final Supplier<T> methodSupplier;

  NamedProperty( final String method, final Supplier<T> methodSupplier ) {
    this.method = method;
    this.methodSupplier = methodSupplier;
  }

  static <T> NamedProperty<T> of( final String method, final Supplier<T> methodSupplier ) {
    return new NamedProperty<T>( method, methodSupplier );
  }

  public T get( ) {
    return methodSupplier.get( );
  }

  public final String getMethod( ) {
    return method;
  }
}
