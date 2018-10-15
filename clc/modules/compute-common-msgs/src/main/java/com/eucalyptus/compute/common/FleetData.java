/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class FleetData extends EucalyptusData {

  private String activityStatus;
  private String clientToken;
  private Date createTime;
  private String excessCapacityTerminationPolicy;
  private String fleetId;
  private String fleetState;
  private Double fulfilledCapacity;
  private Double fulfilledOnDemandCapacity;
  private FleetLaunchTemplateConfigList launchTemplateConfigs;
  private OnDemandOptions onDemandOptions;
  private Boolean replaceUnhealthyInstances;
  private SpotOptions spotOptions;
  private ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>( );
  private TargetCapacitySpecification targetCapacitySpecification;
  private Boolean terminateInstancesWithExpiration;
  private String type;
  private Date validFrom;
  private Date validUntil;

  public String getActivityStatus( ) {
    return activityStatus;
  }

  public void setActivityStatus( final String activityStatus ) {
    this.activityStatus = activityStatus;
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public Date getCreateTime( ) {
    return createTime;
  }

  public void setCreateTime( final Date createTime ) {
    this.createTime = createTime;
  }

  public String getExcessCapacityTerminationPolicy( ) {
    return excessCapacityTerminationPolicy;
  }

  public void setExcessCapacityTerminationPolicy( final String excessCapacityTerminationPolicy ) {
    this.excessCapacityTerminationPolicy = excessCapacityTerminationPolicy;
  }

  public String getFleetId( ) {
    return fleetId;
  }

  public void setFleetId( final String fleetId ) {
    this.fleetId = fleetId;
  }

  public String getFleetState( ) {
    return fleetState;
  }

  public void setFleetState( final String fleetState ) {
    this.fleetState = fleetState;
  }

  public Double getFulfilledCapacity( ) {
    return fulfilledCapacity;
  }

  public void setFulfilledCapacity( final Double fulfilledCapacity ) {
    this.fulfilledCapacity = fulfilledCapacity;
  }

  public Double getFulfilledOnDemandCapacity( ) {
    return fulfilledOnDemandCapacity;
  }

  public void setFulfilledOnDemandCapacity( final Double fulfilledOnDemandCapacity ) {
    this.fulfilledOnDemandCapacity = fulfilledOnDemandCapacity;
  }

  public FleetLaunchTemplateConfigList getLaunchTemplateConfigs( ) {
    return launchTemplateConfigs;
  }

  public void setLaunchTemplateConfigs( final FleetLaunchTemplateConfigList launchTemplateConfigs ) {
    this.launchTemplateConfigs = launchTemplateConfigs;
  }

  public OnDemandOptions getOnDemandOptions( ) {
    return onDemandOptions;
  }

  public void setOnDemandOptions( final OnDemandOptions onDemandOptions ) {
    this.onDemandOptions = onDemandOptions;
  }

  public Boolean getReplaceUnhealthyInstances( ) {
    return replaceUnhealthyInstances;
  }

  public void setReplaceUnhealthyInstances( final Boolean replaceUnhealthyInstances ) {
    this.replaceUnhealthyInstances = replaceUnhealthyInstances;
  }

  public SpotOptions getSpotOptions( ) {
    return spotOptions;
  }

  public void setSpotOptions( final SpotOptions spotOptions ) {
    this.spotOptions = spotOptions;
  }

  public ArrayList<ResourceTag> getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ArrayList<ResourceTag> tagSet ) {
    this.tagSet = tagSet;
  }

  public TargetCapacitySpecification getTargetCapacitySpecification( ) {
    return targetCapacitySpecification;
  }

  public void setTargetCapacitySpecification( final TargetCapacitySpecification targetCapacitySpecification ) {
    this.targetCapacitySpecification = targetCapacitySpecification;
  }

  public Boolean getTerminateInstancesWithExpiration( ) {
    return terminateInstancesWithExpiration;
  }

  public void setTerminateInstancesWithExpiration( final Boolean terminateInstancesWithExpiration ) {
    this.terminateInstancesWithExpiration = terminateInstancesWithExpiration;
  }

  public String getType( ) {
    return type;
  }

  public void setType( final String type ) {
    this.type = type;
  }

  public Date getValidFrom( ) {
    return validFrom;
  }

  public void setValidFrom( final Date validFrom ) {
    this.validFrom = validFrom;
  }

  public Date getValidUntil( ) {
    return validUntil;
  }

  public void setValidUntil( final Date validUntil ) {
    this.validUntil = validUntil;
  }

}
