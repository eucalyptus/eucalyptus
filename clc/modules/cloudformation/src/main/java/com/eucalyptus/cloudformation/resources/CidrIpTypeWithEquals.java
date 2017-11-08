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
package com.eucalyptus.cloudformation.resources;

import java.util.Objects;
import com.eucalyptus.compute.common.CidrIpType;
import com.google.common.base.MoreObjects;

public class CidrIpTypeWithEquals {

  private String cidrIp;

  public CidrIpTypeWithEquals( CidrIpType cidrIpType ) {
    this.cidrIp = cidrIpType.getCidrIp( );
  }

  public CidrIpType getCidrIpType( ) {
    CidrIpType cidrIpType = new CidrIpType( );
    cidrIpType.setCidrIp( cidrIp );
    return cidrIpType;
  }

  public String getCidrIp( ) {
    return cidrIp;
  }

  public void setCidrIp( String cidrIp ) {
    this.cidrIp = cidrIp;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final CidrIpTypeWithEquals that = (CidrIpTypeWithEquals) o;
    return Objects.equals( getCidrIp( ), that.getCidrIp( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getCidrIp( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "cidrIp", cidrIp )
        .toString( );
  }
}
