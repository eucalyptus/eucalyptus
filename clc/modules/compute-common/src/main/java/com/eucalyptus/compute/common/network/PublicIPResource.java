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
package com.eucalyptus.compute.common.network;

import java.util.Objects;

public class PublicIPResource extends NetworkResource {
  private static final long serialVersionUID = 1L;

  public PublicIPResource( ) {
  }

  public PublicIPResource( final String value ) {
    super( null, value );
  }

  public PublicIPResource( final String ownerId, final String value ) {
    super( ownerId, value );
  }

  @Override
  public String getType( ) {
    return "public-ip";
  }

  @Override
  public boolean equals( final Object o ) {
    return this == o || !( o == null || getClass( ) != o.getClass( ) ) && super.equals( o );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( super.hashCode( ) );
  }

  @Override
  public String toString( ) {
    return toStringHelper( this )
        .toString( );
  }
}
