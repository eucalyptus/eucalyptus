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

public class EC2ICMP {

  @Property
  private Integer code;

  @Property
  private Integer type;

  public Integer getCode( ) {
    return code;
  }

  public void setCode( Integer code ) {
    this.code = code;
  }

  public Integer getType( ) {
    return type;
  }

  public void setType( Integer type ) {
    this.type = type;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final EC2ICMP ec2ICMP = (EC2ICMP) o;
    return Objects.equals( getCode( ), ec2ICMP.getCode( ) ) &&
        Objects.equals( getType( ), ec2ICMP.getType( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getCode( ), getType( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "code", code )
        .add( "type", type )
        .toString( );
  }
}
