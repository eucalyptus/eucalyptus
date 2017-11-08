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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class ElasticLoadBalancingConnectionSettings {

  @Required
  @Property
  private Integer idleTimeout;

  public Integer getIdleTimeout( ) {
    return idleTimeout;
  }

  public void setIdleTimeout( Integer idleTimeout ) {
    this.idleTimeout = idleTimeout;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final ElasticLoadBalancingConnectionSettings that = (ElasticLoadBalancingConnectionSettings) o;
    return Objects.equals( getIdleTimeout( ), that.getIdleTimeout( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getIdleTimeout( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "idleTimeout", idleTimeout )
        .toString( );
  }
}
