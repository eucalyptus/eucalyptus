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

import java.util.ArrayList;
import java.util.Objects;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public abstract class NetworkResource extends EucalyptusData {

  private String ownerId;
  private String value;
  private ArrayList<NetworkResource> resources = Lists.newArrayList( );

  public abstract String getType( );

  public String getOwnerId( ) {
    return ownerId;
  }

  public void setOwnerId( String ownerId ) {
    this.ownerId = ownerId;
  }

  public String getValue( ) {
    return value;
  }

  public void setValue( String value ) {
    this.value = value;
  }

  public ArrayList<NetworkResource> getResources( ) {
    return resources;
  }

  public void setResources( ArrayList<NetworkResource> resources ) {
    this.resources = resources;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof NetworkResource ) ) return false;
    final NetworkResource that = (NetworkResource) o;
    return Objects.equals( getOwnerId( ), that.getOwnerId( ) ) &&
        Objects.equals( getValue( ), that.getValue( ) ) &&
        Objects.equals( getResources( ), that.getResources( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getOwnerId( ), getValue( ), getResources( ) );
  }

  protected MoreObjects.ToStringHelper toStringHelper( Object self ) {
    return MoreObjects.toStringHelper( self )
        .add( "type", getType( ) )
        .add( "value", getValue( ) )
        .add( "ownerId", getOwnerId( ) )
        .add( "resources", getResources( ) );
  }
}
