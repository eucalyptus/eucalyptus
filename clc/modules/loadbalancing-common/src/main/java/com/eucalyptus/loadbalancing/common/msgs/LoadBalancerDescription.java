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

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class LoadBalancerDescription extends EucalyptusData {

  private static final long serialVersionUID = 1L;
  private String loadBalancerName;
  private String dnsName;
  private String canonicalHostedZoneName;
  private String canonicalHostedZoneNameID;
  private ListenerDescriptions listenerDescriptions;
  private Policies policies;
  private BackendServerDescriptions backendServerDescriptions;
  private AvailabilityZones availabilityZones;
  private Subnets subnets;
  private String vpcId;
  private Instances instances;
  private HealthCheck healthCheck;
  private SourceSecurityGroup sourceSecurityGroup;
  private SecurityGroups securityGroups;
  private Date createdTime;
  private String scheme;

  public String getLoadBalancerName( ) {
    return loadBalancerName;
  }

  public void setLoadBalancerName( String loadBalancerName ) {
    this.loadBalancerName = loadBalancerName;
  }

  public String getDnsName( ) {
    return dnsName;
  }

  public void setDnsName( String dnsName ) {
    this.dnsName = dnsName;
  }

  public String getCanonicalHostedZoneName( ) {
    return canonicalHostedZoneName;
  }

  public void setCanonicalHostedZoneName( String canonicalHostedZoneName ) {
    this.canonicalHostedZoneName = canonicalHostedZoneName;
  }

  public String getCanonicalHostedZoneNameID( ) {
    return canonicalHostedZoneNameID;
  }

  public void setCanonicalHostedZoneNameID( String canonicalHostedZoneNameID ) {
    this.canonicalHostedZoneNameID = canonicalHostedZoneNameID;
  }

  public ListenerDescriptions getListenerDescriptions( ) {
    return listenerDescriptions;
  }

  public void setListenerDescriptions( ListenerDescriptions listenerDescriptions ) {
    this.listenerDescriptions = listenerDescriptions;
  }

  public Policies getPolicies( ) {
    return policies;
  }

  public void setPolicies( Policies policies ) {
    this.policies = policies;
  }

  public BackendServerDescriptions getBackendServerDescriptions( ) {
    return backendServerDescriptions;
  }

  public void setBackendServerDescriptions( BackendServerDescriptions backendServerDescriptions ) {
    this.backendServerDescriptions = backendServerDescriptions;
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

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  public Instances getInstances( ) {
    return instances;
  }

  public void setInstances( Instances instances ) {
    this.instances = instances;
  }

  public HealthCheck getHealthCheck( ) {
    return healthCheck;
  }

  public void setHealthCheck( HealthCheck healthCheck ) {
    this.healthCheck = healthCheck;
  }

  public SourceSecurityGroup getSourceSecurityGroup( ) {
    return sourceSecurityGroup;
  }

  public void setSourceSecurityGroup( SourceSecurityGroup sourceSecurityGroup ) {
    this.sourceSecurityGroup = sourceSecurityGroup;
  }

  public SecurityGroups getSecurityGroups( ) {
    return securityGroups;
  }

  public void setSecurityGroups( SecurityGroups securityGroups ) {
    this.securityGroups = securityGroups;
  }

  public Date getCreatedTime( ) {
    return createdTime;
  }

  public void setCreatedTime( Date createdTime ) {
    this.createdTime = createdTime;
  }

  public String getScheme( ) {
    return scheme;
  }

  public void setScheme( String scheme ) {
    this.scheme = scheme;
  }
}
