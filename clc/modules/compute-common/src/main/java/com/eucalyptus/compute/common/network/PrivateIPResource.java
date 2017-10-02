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

public class PrivateIPResource extends NetworkResource {

  private static final long serialVersionUID = 1L;

  private String mac;

  public PrivateIPResource( ) {
  }

  public PrivateIPResource( final String ownerId, final String value, final String mac ) {
    super( ownerId, value );
    this.mac = mac;
  }

  @Override
  public String getType( ) {
    return "private-ip";
  }

  public String getMac( ) {
    return mac;
  }

  public void setMac( String mac ) {
    this.mac = mac;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    if ( !super.equals( o ) ) return false;
    final PrivateIPResource that = (PrivateIPResource) o;
    return Objects.equals( getMac( ), that.getMac( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( super.hashCode( ), getMac( ) );
  }

  @Override
  public String toString( ) {
    return toStringHelper( this )
        .add( "mac", mac )
        .toString( );
  }
}
