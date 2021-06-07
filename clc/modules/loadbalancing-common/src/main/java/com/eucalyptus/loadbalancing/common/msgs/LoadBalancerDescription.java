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
