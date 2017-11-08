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

public class AutoScalingTag {

  @Property
  @Required
  private String key;

  @Property
  @Required
  private String value;

  @Property
  @Required
  private Boolean propagateAtLaunch;

  public String getKey( ) {
    return key;
  }

  public void setKey( String key ) {
    this.key = key;
  }

  public Boolean getPropagateAtLaunch( ) {
    return propagateAtLaunch;
  }

  public void setPropagateAtLaunch( Boolean propagateAtLaunch ) {
    this.propagateAtLaunch = propagateAtLaunch;
  }

  public String getValue( ) {
    return value;
  }

  public void setValue( String value ) {
    this.value = value;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final AutoScalingTag that = (AutoScalingTag) o;
    return Objects.equals( getKey( ), that.getKey( ) ) &&
        Objects.equals( getValue( ), that.getValue( ) ) &&
        Objects.equals( getPropagateAtLaunch( ), that.getPropagateAtLaunch( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getKey( ), getValue( ), getPropagateAtLaunch( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "key", key )
        .add( "value", value )
        .add( "propagateAtLaunch", propagateAtLaunch )
        .toString( );
  }
}
