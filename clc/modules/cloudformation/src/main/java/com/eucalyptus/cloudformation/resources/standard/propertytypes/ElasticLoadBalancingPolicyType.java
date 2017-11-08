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

import java.util.ArrayList;
import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class ElasticLoadBalancingPolicyType {

  @Property
  private ArrayList<ElasticLoadBalancingPolicyTypeAttribute> attributes = Lists.newArrayList( );

  @Property
  private ArrayList<Integer> instancePorts = Lists.newArrayList( );

  @Property
  private ArrayList<Integer> loadBalancerPorts = Lists.newArrayList( );

  @Required
  @Property
  private String policyName;

  @Required
  @Property
  private String policyType;

  public ArrayList<ElasticLoadBalancingPolicyTypeAttribute> getAttributes( ) {
    return attributes;
  }

  public void setAttributes( ArrayList<ElasticLoadBalancingPolicyTypeAttribute> attributes ) {
    this.attributes = attributes;
  }

  public ArrayList<Integer> getInstancePorts( ) {
    return instancePorts;
  }

  public void setInstancePorts( ArrayList<Integer> instancePorts ) {
    this.instancePorts = instancePorts;
  }

  public ArrayList<Integer> getLoadBalancerPorts( ) {
    return loadBalancerPorts;
  }

  public void setLoadBalancerPorts( ArrayList<Integer> loadBalancerPorts ) {
    this.loadBalancerPorts = loadBalancerPorts;
  }

  public String getPolicyName( ) {
    return policyName;
  }

  public void setPolicyName( String policyName ) {
    this.policyName = policyName;
  }

  public String getPolicyType( ) {
    return policyType;
  }

  public void setPolicyType( String policyType ) {
    this.policyType = policyType;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final ElasticLoadBalancingPolicyType that = (ElasticLoadBalancingPolicyType) o;
    return Objects.equals( getAttributes( ), that.getAttributes( ) ) &&
        Objects.equals( getInstancePorts( ), that.getInstancePorts( ) ) &&
        Objects.equals( getLoadBalancerPorts( ), that.getLoadBalancerPorts( ) ) &&
        Objects.equals( getPolicyName( ), that.getPolicyName( ) ) &&
        Objects.equals( getPolicyType( ), that.getPolicyType( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getAttributes( ), getInstancePorts( ), getLoadBalancerPorts( ), getPolicyName( ), getPolicyType( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "attributes", attributes )
        .add( "instancePorts", instancePorts )
        .add( "loadBalancerPorts", loadBalancerPorts )
        .add( "policyName", policyName )
        .add( "policyType", policyType )
        .toString( );
  }
}
