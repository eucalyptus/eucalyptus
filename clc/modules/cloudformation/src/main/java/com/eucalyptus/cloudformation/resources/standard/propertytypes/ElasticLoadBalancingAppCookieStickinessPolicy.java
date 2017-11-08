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

public class ElasticLoadBalancingAppCookieStickinessPolicy {

  @Required
  @Property
  private String cookieName;

  @Required
  @Property
  private String policyName;

  public String getCookieName( ) {
    return cookieName;
  }

  public void setCookieName( String cookieName ) {
    this.cookieName = cookieName;
  }

  public String getPolicyName( ) {
    return policyName;
  }

  public void setPolicyName( String policyName ) {
    this.policyName = policyName;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final ElasticLoadBalancingAppCookieStickinessPolicy that = (ElasticLoadBalancingAppCookieStickinessPolicy) o;
    return Objects.equals( getCookieName( ), that.getCookieName( ) ) &&
        Objects.equals( getPolicyName( ), that.getPolicyName( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getCookieName( ), getPolicyName( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "cookieName", cookieName )
        .add( "policyName", policyName )
        .toString( );
  }
}
