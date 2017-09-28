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

import java.util.List;
import java.util.Objects;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class Service {

  private String type;
  private List<String> endpoints;

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  public List<String> getEndpoints( ) {
    return endpoints;
  }

  public void setEndpoints( List<String> endpoints ) {
    this.endpoints = endpoints;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final Service service = (Service) o;
    return Objects.equals( getType( ), service.getType( ) ) &&
        Objects.equals( getEndpoints( ), service.getEndpoints( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getType( ), getEndpoints( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "type", type )
        .add( "endpoints", endpoints )
        .toString( );
  }
}
