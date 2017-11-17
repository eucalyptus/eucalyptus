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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSElasticLoadBalancingLoadBalancerProperties implements ResourceProperties {

  @Property
  private ElasticLoadBalancingAccessLoggingPolicy accessLoggingPolicy;

  @Property
  private ArrayList<ElasticLoadBalancingAppCookieStickinessPolicy> appCookieStickinessPolicy = Lists.newArrayList( );

  @Property
  private ArrayList<String> availabilityZones = Lists.newArrayList( );

  @Property
  private ElasticLoadBalancingConnectionDrainingPolicy connectionDrainingPolicy;

  @Property
  private ElasticLoadBalancingConnectionSettings connectionSettings;

  @Property
  private Boolean crossZone;

  @Property
  private ElasticLoadBalancingHealthCheckType healthCheck;

  @Property
  private ArrayList<String> instances = Lists.newArrayList( );

  @Property( name = "LBCookieStickinessPolicy" )
  private ArrayList<ElasticLoadBalancingLBCookieStickinessPolicyType> lbCookieStickinessPolicy = Lists.newArrayList( );

  @Property
  private String loadBalancerName;

  @Required
  @Property
  private ArrayList<ElasticLoadBalancingListener> listeners = Lists.newArrayList( );

  @Property
  private ArrayList<ElasticLoadBalancingPolicyType> policies = Lists.newArrayList( );

  @Property
  private String scheme;

  @Property
  private ArrayList<String> securityGroups = Lists.newArrayList( );

  @Property
  private ArrayList<String> subnets = Lists.newArrayList( );

  @Property
  private ArrayList<CloudFormationResourceTag> tags = Lists.newArrayList( );

  public ElasticLoadBalancingAccessLoggingPolicy getAccessLoggingPolicy( ) {
    return accessLoggingPolicy;
  }

  public void setAccessLoggingPolicy( ElasticLoadBalancingAccessLoggingPolicy accessLoggingPolicy ) {
    this.accessLoggingPolicy = accessLoggingPolicy;
  }

  public ArrayList<ElasticLoadBalancingAppCookieStickinessPolicy> getAppCookieStickinessPolicy( ) {
    return appCookieStickinessPolicy;
  }

  public void setAppCookieStickinessPolicy( ArrayList<ElasticLoadBalancingAppCookieStickinessPolicy> appCookieStickinessPolicy ) {
    this.appCookieStickinessPolicy = appCookieStickinessPolicy;
  }

  public ArrayList<String> getAvailabilityZones( ) {
    return availabilityZones;
  }

  public void setAvailabilityZones( ArrayList<String> availabilityZones ) {
    this.availabilityZones = availabilityZones;
  }

  public ElasticLoadBalancingConnectionDrainingPolicy getConnectionDrainingPolicy( ) {
    return connectionDrainingPolicy;
  }

  public void setConnectionDrainingPolicy( ElasticLoadBalancingConnectionDrainingPolicy connectionDrainingPolicy ) {
    this.connectionDrainingPolicy = connectionDrainingPolicy;
  }

  public ElasticLoadBalancingConnectionSettings getConnectionSettings( ) {
    return connectionSettings;
  }

  public void setConnectionSettings( ElasticLoadBalancingConnectionSettings connectionSettings ) {
    this.connectionSettings = connectionSettings;
  }

  public Boolean getCrossZone( ) {
    return crossZone;
  }

  public void setCrossZone( Boolean crossZone ) {
    this.crossZone = crossZone;
  }

  public ElasticLoadBalancingHealthCheckType getHealthCheck( ) {
    return healthCheck;
  }

  public void setHealthCheck( ElasticLoadBalancingHealthCheckType healthCheck ) {
    this.healthCheck = healthCheck;
  }

  public ArrayList<String> getInstances( ) {
    return instances;
  }

  public void setInstances( ArrayList<String> instances ) {
    this.instances = instances;
  }

  public ArrayList<ElasticLoadBalancingLBCookieStickinessPolicyType> getLbCookieStickinessPolicy( ) {
    return lbCookieStickinessPolicy;
  }

  public void setLbCookieStickinessPolicy( ArrayList<ElasticLoadBalancingLBCookieStickinessPolicyType> lbCookieStickinessPolicy ) {
    this.lbCookieStickinessPolicy = lbCookieStickinessPolicy;
  }

  public ArrayList<ElasticLoadBalancingListener> getListeners( ) {
    return listeners;
  }

  public void setListeners( ArrayList<ElasticLoadBalancingListener> listeners ) {
    this.listeners = listeners;
  }

  public String getLoadBalancerName( ) {
    return loadBalancerName;
  }

  public void setLoadBalancerName( String loadBalancerName ) {
    this.loadBalancerName = loadBalancerName;
  }

  public ArrayList<ElasticLoadBalancingPolicyType> getPolicies( ) {
    return policies;
  }

  public void setPolicies( ArrayList<ElasticLoadBalancingPolicyType> policies ) {
    this.policies = policies;
  }

  public String getScheme( ) {
    return scheme;
  }

  public void setScheme( String scheme ) {
    this.scheme = scheme;
  }

  public ArrayList<String> getSecurityGroups( ) {
    return securityGroups;
  }

  public void setSecurityGroups( ArrayList<String> securityGroups ) {
    this.securityGroups = securityGroups;
  }

  public ArrayList<String> getSubnets( ) {
    return subnets;
  }

  public void setSubnets( ArrayList<String> subnets ) {
    this.subnets = subnets;
  }

  public ArrayList<CloudFormationResourceTag> getTags( ) {
    return tags;
  }

  public void setTags( ArrayList<CloudFormationResourceTag> tags ) {
    this.tags = tags;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "accessLoggingPolicy", accessLoggingPolicy )
        .add( "appCookieStickinessPolicy", appCookieStickinessPolicy )
        .add( "availabilityZones", availabilityZones )
        .add( "connectionDrainingPolicy", connectionDrainingPolicy )
        .add( "connectionSettings", connectionSettings )
        .add( "crossZone", crossZone )
        .add( "healthCheck", healthCheck )
        .add( "instances", instances )
        .add( "lbCookieStickinessPolicy", lbCookieStickinessPolicy )
        .add( "loadBalancerName", loadBalancerName )
        .add( "listeners", listeners )
        .add( "policies", policies )
        .add( "scheme", scheme )
        .add( "securityGroups", securityGroups )
        .add( "subnets", subnets )
        .add( "tags", tags )
        .toString( );
  }
}
