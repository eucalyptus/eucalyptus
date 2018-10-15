/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ServiceConfiguration extends EucalyptusData {

  private Boolean acceptanceRequired;
  private ValueStringList availabilityZones;
  private ValueStringList baseEndpointDnsNames;
  private ValueStringList networkLoadBalancerArns;
  private String privateDnsName;
  private String serviceId;
  private String serviceName;
  private String serviceState;
  private ServiceTypeDetailSet serviceType;

  public Boolean getAcceptanceRequired( ) {
    return acceptanceRequired;
  }

  public void setAcceptanceRequired( final Boolean acceptanceRequired ) {
    this.acceptanceRequired = acceptanceRequired;
  }

  public ValueStringList getAvailabilityZones( ) {
    return availabilityZones;
  }

  public void setAvailabilityZones( final ValueStringList availabilityZones ) {
    this.availabilityZones = availabilityZones;
  }

  public ValueStringList getBaseEndpointDnsNames( ) {
    return baseEndpointDnsNames;
  }

  public void setBaseEndpointDnsNames( final ValueStringList baseEndpointDnsNames ) {
    this.baseEndpointDnsNames = baseEndpointDnsNames;
  }

  public ValueStringList getNetworkLoadBalancerArns( ) {
    return networkLoadBalancerArns;
  }

  public void setNetworkLoadBalancerArns( final ValueStringList networkLoadBalancerArns ) {
    this.networkLoadBalancerArns = networkLoadBalancerArns;
  }

  public String getPrivateDnsName( ) {
    return privateDnsName;
  }

  public void setPrivateDnsName( final String privateDnsName ) {
    this.privateDnsName = privateDnsName;
  }

  public String getServiceId( ) {
    return serviceId;
  }

  public void setServiceId( final String serviceId ) {
    this.serviceId = serviceId;
  }

  public String getServiceName( ) {
    return serviceName;
  }

  public void setServiceName( final String serviceName ) {
    this.serviceName = serviceName;
  }

  public String getServiceState( ) {
    return serviceState;
  }

  public void setServiceState( final String serviceState ) {
    this.serviceState = serviceState;
  }

  public ServiceTypeDetailSet getServiceType( ) {
    return serviceType;
  }

  public void setServiceType( final ServiceTypeDetailSet serviceType ) {
    this.serviceType = serviceType;
  }

}
