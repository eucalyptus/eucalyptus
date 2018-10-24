/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
