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
