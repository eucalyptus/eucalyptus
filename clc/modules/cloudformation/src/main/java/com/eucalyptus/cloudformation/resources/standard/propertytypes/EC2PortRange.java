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
import com.google.common.base.MoreObjects;

public class EC2PortRange {

  @Property
  private Integer from;

  @Property
  private Integer to;

  public Integer getFrom( ) {
    return from;
  }

  public void setFrom( Integer from ) {
    this.from = from;
  }

  public Integer getTo( ) {
    return to;
  }

  public void setTo( Integer to ) {
    this.to = to;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final EC2PortRange that = (EC2PortRange) o;
    return Objects.equals( getFrom( ), that.getFrom( ) ) &&
        Objects.equals( getTo( ), that.getTo( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getFrom( ), getTo( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "from", from )
        .add( "to", to )
        .toString( );
  }
}
