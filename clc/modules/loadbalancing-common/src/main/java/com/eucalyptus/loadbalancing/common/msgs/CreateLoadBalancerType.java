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
package com.eucalyptus.loadbalancing.common.msgs;

import javax.annotation.Nonnull;

public class CreateLoadBalancerType extends LoadBalancingMessage {

  @Nonnull
  @LoadBalancingMessageValidation.FieldRegex( LoadBalancingMessageValidation.FieldRegexValue.LOAD_BALANCER_NAME )
  private String loadBalancerName;
  @Nonnull
  private Listeners listeners;
  private AvailabilityZones availabilityZones;
  private Subnets subnets;
  private SecurityGroups securityGroups;
  @LoadBalancingMessageValidation.FieldRegex( LoadBalancingMessageValidation.FieldRegexValue.LOAD_BALANCER_SCHEME )
  private String scheme;
  private TagList tags;

  public CreateLoadBalancerType( ) {
  }

  public String getLoadBalancerName( ) {
    return loadBalancerName;
  }

  public void setLoadBalancerName( String loadBalancerName ) {
    this.loadBalancerName = loadBalancerName;
  }

  public Listeners getListeners( ) {
    return listeners;
  }

  public void setListeners( Listeners listeners ) {
    this.listeners = listeners;
  }

  public AvailabilityZones getAvailabilityZones( ) {
    return availabilityZones;
  }

  public void setAvailabilityZones( AvailabilityZones availabilityZones ) {
    this.availabilityZones = availabilityZones;
  }

  public Subnets getSubnets( ) {
    return subnets;
  }

  public void setSubnets( Subnets subnets ) {
    this.subnets = subnets;
  }

  public SecurityGroups getSecurityGroups( ) {
    return securityGroups;
  }

  public void setSecurityGroups( SecurityGroups securityGroups ) {
    this.securityGroups = securityGroups;
  }

  public String getScheme( ) {
    return scheme;
  }

  public void setScheme( String scheme ) {
    this.scheme = scheme;
  }

  public TagList getTags( ) {
    return tags;
  }

  public void setTags( TagList tags ) {
    this.tags = tags;
  }
}
